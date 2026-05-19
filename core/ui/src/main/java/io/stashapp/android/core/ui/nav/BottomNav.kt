package io.stashapp.android.core.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewModule
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
 * Bottom navigation bar. [visibleIds] controls which items show as tabs;
 * everything else (plus Settings / Disconnect) spills into the More sheet.
 */
@Composable
fun MainBottomBar(
    currentRoute: String?,
    visibleIds: List<String> = MainNavItems.DefaultVisibleIds,
    onNavigate: (String) -> Unit,
    onOpenMore: () -> Unit,
) {
    val visibleItems = visibleIds.mapNotNull { id -> MainNavItems.All.find { it.id == id } }

    NavigationBar(
        containerColor = SpineColors.Surface,
        contentColor = SpineColors.OnSurface,
        tonalElevation = 0.dp,
    ) {
        visibleItems.forEach { item ->
            val selected =
                currentRoute == item.route ||
                    (
                        currentRoute?.startsWith(item.route.substringBefore("?")) == true &&
                            currentRoute.substringBefore("?") == item.route.substringBefore("?")
                    )
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        if (selected) item.iconFilled else item.iconOutlined,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = SpineColors.AccentPrimary,
                        selectedTextColor = SpineColors.AccentPrimary,
                        indicatorColor = SpineColors.AccentPrimary.copy(alpha = 0.18f),
                        unselectedIconColor = SpineColors.OnSurfaceVariant,
                        unselectedTextColor = SpineColors.OnSurfaceVariant,
                    ),
            )
        }
        // Always-visible More tab — opens the overflow sheet
        NavigationBarItem(
            selected = false,
            onClick = onOpenMore,
            icon = { Icon(Icons.Filled.Tune, contentDescription = "More") },
            label = { Text("More", style = MaterialTheme.typography.labelSmall) },
            colors =
                NavigationBarItemDefaults.colors(
                    unselectedIconColor = SpineColors.OnSurfaceVariant,
                    unselectedTextColor = SpineColors.OnSurfaceVariant,
                ),
        )
    }
}

/**
 * Overflow sheet shown when the user taps "More". Surfaces navigation items
 * that aren't in the bottom-bar's visible set, plus always-present shortcuts
 * to Settings and Disconnect.
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
    val hiddenItems = MainNavItems.All.filter { it.id !in visibleIds }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpineColors.Surface,
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                "Browse",
                style = MaterialTheme.typography.labelMedium,
                color = SpineColors.OnSurfaceMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            hiddenItems.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.label) },
                    leadingContent = { Icon(item.iconOutlined, contentDescription = null) },
                    modifier =
                        Modifier.clickable {
                            onNavigate(item.route)
                            onDismiss()
                        },
                    colors = ListItemDefaults.colors(containerColor = SpineColors.Surface),
                )
            }
            HorizontalDivider(color = SpineColors.Border)
            Text(
                "App",
                style = MaterialTheme.typography.labelMedium,
                color = SpineColors.OnSurfaceMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            ListItem(
                headlineContent = { Text("Settings") },
                leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null) },
                modifier =
                    Modifier.clickable {
                        onOpenSettings()
                        onDismiss()
                    },
                colors = ListItemDefaults.colors(containerColor = SpineColors.Surface),
            )
            ListItem(
                headlineContent = { Text("Customize nav bar") },
                supportingContent = {
                    Text("Choose which items appear at the bottom", style = MaterialTheme.typography.labelSmall)
                },
                leadingContent = { Icon(Icons.Filled.ViewModule, contentDescription = null) },
                modifier =
                    Modifier.clickable {
                        onCustomize()
                        onDismiss()
                    },
                colors = ListItemDefaults.colors(containerColor = SpineColors.Surface),
            )
        }
    }
}
