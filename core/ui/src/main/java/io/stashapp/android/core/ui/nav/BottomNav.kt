package io.stashapp.android.core.ui.nav

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.stashapp.android.core.designsystem.theme.MetaMono
import io.stashapp.android.core.designsystem.theme.SpaceGrotesk
import io.stashapp.android.core.designsystem.theme.SpineColors

/**
 * A single selectable destination surfaced by the bottom navigation bar.
 *
 * @param id stable key used to persist visible-set ordering
 * @param route the navigation route this item jumps to
 * @param label human-readable label
 */
data class MainNavItem(
    val id: String,
    val route: String,
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
)

object MainNavItems {
    val Home =
        MainNavItem(
            id = "home",
            route = Routes.Home,
            label = "Home",
            iconFilled = Icons.Filled.ViewModule,
            iconOutlined = Icons.Outlined.Home,
        )
    val Scenes =
        MainNavItem(
            id = "scenes",
            route = Routes.Library,
            label = "Scenes",
            iconFilled = Icons.Filled.Movie,
            iconOutlined = Icons.Outlined.Movie,
        )
    val Studios =
        MainNavItem(
            id = "studios",
            route = Routes.browse("studios"),
            label = "Studios",
            iconFilled = Icons.Filled.Storefront,
            iconOutlined = Icons.Outlined.Storefront,
        )
    val Performers =
        MainNavItem(
            id = "performers",
            route = Routes.browse("performers"),
            label = "People",
            iconFilled = Icons.Filled.Person,
            iconOutlined = Icons.Outlined.Person,
        )
    val Tags =
        MainNavItem(
            id = "tags",
            route = Routes.browse("tags"),
            label = "Tags",
            iconFilled = Icons.Filled.Label,
            iconOutlined = Icons.Outlined.Label,
        )

    /** All items available to the user. The first four are shown by default,
     *  everything beyond lives in the More sheet. */
    val All = listOf(Home, Scenes, Studios, Performers, Tags)

    val DefaultVisibleIds = listOf(Home.id, Scenes.id, Studios.id, Performers.id)
}

/**
 * Spine floating pill bottom navigation bar.
 *
 * Renders a centered Row with a frosted-glass pill container. Active tab shows
 * AccentPrimary background with label; inactive tabs are icon-only.
 * [onOpenMore] is kept for backward compatibility (the Browse screen triggers it).
 */
@Composable
fun MainBottomBar(
    currentRoute: String?,
    visibleIds: List<String> = MainNavItems.DefaultVisibleIds,
    onNavigate: (String) -> Unit,
    onOpenMore: () -> Unit,
) {
    val visibleItems = visibleIds.mapNotNull { id -> MainNavItems.All.find { it.id == id } }

    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            renderEffect = BlurEffect(20f, 20f, TileMode.Clamp)
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = blurModifier
                .background(
                    color = SpineColors.Surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                )
                .border(1.dp, SpineColors.Border, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleItems.forEach { item ->
                val selected = currentRoute?.startsWith(item.route) == true
                val tabBg = if (selected) {
                    Modifier.background(SpineColors.AccentPrimary, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
                Row(
                    modifier = tabBg
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (selected) item.iconFilled else item.iconOutlined,
                        contentDescription = item.label,
                        tint = if (selected) SpineColors.AccentOnPrimary else SpineColors.OnSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    if (selected) {
                        Text(
                            text = item.label,
                            style = TextStyle(
                                fontFamily = SpaceGrotesk,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.1).sp,
                            ),
                            color = SpineColors.AccentOnPrimary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Overflow sheet — Spine styled with fixed Browse group (Tags/Markers/History)
 * and App group (Settings/Customize nav/Cast).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreSheet(
    sheetState: SheetState,
    visibleIds: List<String>,
    onNavigate: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCustomize: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpineColors.SurfaceHigh,
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(Modifier.padding(bottom = 16.dp)) {
            // Browse group — fixed Spine items
            Text(
                "Browse",
                style = MetaMono,
                color = SpineColors.OnSurfaceMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            MoreSheetItem(
                label = "Tags",
                icon = Icons.Filled.Label,
                onClick = {
                    onNavigate(Routes.browse("tags"))
                    onDismiss()
                },
            )
            MoreSheetItem(
                label = "Markers",
                icon = Icons.Outlined.Bookmarks,
                onClick = {
                    onNavigate(Routes.browse("markers"))
                    onDismiss()
                },
            )
            MoreSheetItem(
                label = "History",
                icon = Icons.Filled.History,
                onClick = {
                    onNavigate(Routes.browse("history"))
                    onDismiss()
                },
            )

            HorizontalDivider(color = SpineColors.Border)

            // App group
            Text(
                "App",
                style = MetaMono,
                color = SpineColors.OnSurfaceMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            MoreSheetItem(
                label = "Settings",
                icon = Icons.Filled.Tune,
                onClick = {
                    onOpenSettings()
                    onDismiss()
                },
            )
            MoreSheetItem(
                label = "Customize nav bar",
                icon = Icons.Filled.ViewModule,
                onClick = {
                    onCustomize()
                    onDismiss()
                },
            )
            MoreSheetItem(
                label = "Cast",
                icon = Icons.Filled.Cast,
                onClick = { onDismiss() },
            )
        }
    }
}

@Composable
private fun MoreSheetItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.titleSmall)
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SpineColors.AccentPrimary,
                modifier = Modifier.size(22.dp),
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = SpineColors.SurfaceHigh),
    )
}
