package io.stashapp.android.core.network.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.apollographql.apollo.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.stashapp.android.core.network.StashAuthInterceptor
import io.stashapp.android.core.network.StashEndpointProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        endpointProvider: StashEndpointProvider,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // Network (not application) interceptor so the key is re-evaluated on
                // every redirect hop and never forwarded to another origin.
                .addNetworkInterceptor(StashAuthInterceptor(endpointProvider))
        // Log request method + URL only in debug builds so release builds don't
        // leak URL query strings (scene IDs, search terms) into logcat.
        if ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideApolloClient(
        okHttpClient: OkHttpClient,
        endpointProvider: StashEndpointProvider,
    ): ApolloClient =
        ApolloClient
            .Builder()
            // Placeholder URL — per-request URL is overridden by the interceptor below
            // to support switching endpoints at runtime without rebuilding the client.
            .serverUrl("http://localhost/graphql")
            .okHttpClient(okHttpClient)
            .addHttpInterceptor(StashEndpointInterceptor(endpointProvider))
            .build()
}

/**
 * Rewrites every GraphQL request to hit the currently-configured Stash endpoint.
 * The API key header is added at the OkHttp network layer by [StashAuthInterceptor].
 */
private class StashEndpointInterceptor(
    private val endpointProvider: StashEndpointProvider,
) : HttpInterceptor {
    override suspend fun intercept(
        request: HttpRequest,
        chain: HttpInterceptorChain,
    ): HttpResponse {
        val endpoint =
            endpointProvider.current()
                ?: error("No Stash endpoint configured. Connect to a server first.")

        // Apollo 4 Builder sets URL at construction via newBuilder(method, url).
        return chain.proceed(request.newBuilder(request.method, endpoint.graphqlUrl).build())
    }
}
