package io.stashapp.android.feature.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.stashapp.android.core.designsystem.theme.JetBrainsMono
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.ShapeSmall
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors

@Composable
fun ConnectionScreen(
    onConnected: () -> Unit,
    viewModel: ConnectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Cloud,
                contentDescription = null,
                tint = SpineColors.AccentPrimary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
            // Spine headline — SpaceGrotesk 26sp W600 -0.6sp
            Text(
                "Connect to Stash",
                style = TextStyle(
                    fontFamily = SpaceGrotesk,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp,
                ),
                color = SpineColors.OnSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Enter your server URL. An API key is required if you have auth enabled.",
                style = MetaMono,
                color = SpineColors.OnSurfaceMuted,
            )

            Spacer(Modifier.height(24.dp))

            // Input: Server URL
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::setUrl,
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.10:9999") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                shape = ShapeSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpineColors.AccentPrimary,
                    unfocusedBorderColor = SpineColors.Border,
                    focusedContainerColor = SpineColors.Surface,
                    unfocusedContainerColor = SpineColors.Surface,
                    focusedTextColor = SpineColors.OnSurface,
                    unfocusedTextColor = SpineColors.OnSurface,
                    cursorColor = SpineColors.AccentPrimary,
                ),
            )
            Spacer(Modifier.height(12.dp))

            // Input: API Key
            var apiKeyVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::setApiKey,
                label = { Text("API key (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                // Mask the key by default; user-toggleable reveal eye icon.
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                // Disable autocorrect + IME-learning so the key never lands in
                // the keyboard's personal dictionary or autofill service.
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrect = false,
                    ),
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (apiKeyVisible) "Hide key" else "Show key",
                            tint = SpineColors.OnSurfaceVariant,
                        )
                    }
                },
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                shape = ShapeSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpineColors.AccentPrimary,
                    unfocusedBorderColor = SpineColors.Border,
                    focusedContainerColor = SpineColors.Surface,
                    unfocusedContainerColor = SpineColors.Surface,
                    focusedTextColor = SpineColors.OnSurface,
                    unfocusedTextColor = SpineColors.OnSurface,
                    cursorColor = SpineColors.AccentPrimary,
                ),
            )
            Spacer(Modifier.height(12.dp))

            // Input: Display Name
            OutlinedTextField(
                value = state.displayName,
                onValueChange = viewModel::setName,
                label = { Text("Display name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp),
                shape = ShapeSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpineColors.AccentPrimary,
                    unfocusedBorderColor = SpineColors.Border,
                    focusedContainerColor = SpineColors.Surface,
                    unfocusedContainerColor = SpineColors.Surface,
                    focusedTextColor = SpineColors.OnSurface,
                    unfocusedTextColor = SpineColors.OnSurface,
                    cursorColor = SpineColors.AccentPrimary,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // Success card — Spine AccentPrimary tinted card
            state.serverInfo?.let { info ->
                Surface(
                    shape = ShapeSmall,
                    color = SpineColors.AccentPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SpineColors.AccentPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = SpineColors.AccentPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "Connected to Stash v${info.version}",
                                style = TextStyle(fontFamily = SpaceGrotesk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                color = SpineColors.OnSurface,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${info.sceneCount} scenes · ${info.performerCount} performers · " +
                                "${info.studioCount} studios · ${info.tagCount} tags",
                            style = MetaMono,
                            color = SpineColors.OnSurfaceMuted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Error card — Spine Error tinted card
            state.error?.let { err ->
                Surface(
                    shape = ShapeSmall,
                    color = SpineColors.Error.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SpineColors.Error.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = SpineColors.Error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            err,
                            style = MetaMono,
                            color = SpineColors.OnSurface,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Test button — outlined with Spine border
                OutlinedButton(
                    onClick = viewModel::test,
                    enabled = !state.testing && state.baseUrl.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SpineColors.Border),
                    shape = ShapeSmall,
                ) {
                    if (state.testing) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                            color = SpineColors.AccentPrimary,
                        )
                    } else {
                        Text("Test", color = SpineColors.OnSurface)
                    }
                }
                // Connect button — AccentPrimary filled
                Button(
                    onClick = { viewModel.connect(onConnected) },
                    enabled = state.serverInfo != null,
                    modifier = Modifier.weight(1f),
                    shape = ShapeSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpineColors.AccentPrimary,
                        contentColor = SpineColors.AccentOnPrimary,
                    ),
                ) {
                    Text("Connect", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = androidx.compose.foundation.layout.Row(
    modifier = modifier,
    horizontalArrangement = horizontalArrangement,
    verticalAlignment = verticalAlignment,
    content = content,
)
