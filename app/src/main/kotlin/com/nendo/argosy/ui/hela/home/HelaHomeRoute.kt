package com.nendo.argosy.ui.hela.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.nendo.argosy.ui.common.settingsSection
import com.nendo.argosy.ui.navigation.Screen

@Composable
fun HelaHomeRoute(
    navController: NavHostController,
    onNavigateToDefault: () -> Unit,
    onDrawerToggle: () -> Unit,
    onPlayMedia: (itemId: String, startOver: Boolean) -> Unit
) {
    HelaHomeScreen(
        onGameSelect = { gameId ->
            navController.navigate(Screen.GameDetail.createRoute(gameId))
        },
        onNavigateToLibrary = { platformId, sourceFilter, filters ->
            navController.navigate(Screen.Library.createRoute(platformId, sourceFilter, filters))
        },
        onNavigateToCollections = { collectionId ->
            navController.navigate(
                if (collectionId > 0) {
                    Screen.CollectionDetail.createRoute(collectionId)
                } else {
                    Screen.Collections.route
                }
            )
        },
        onNavigateToDefault = onNavigateToDefault,
        onDrawerToggle = onDrawerToggle,
        onChangelogAction = { action ->
            navController.navigate(
                Screen.Settings.createRoute(action.settingsSection.name, action.actionKey)
            )
        },
        onNavigateToSettings = { section ->
            navController.navigate(Screen.Settings.createRoute(section))
        },
        onOpenSettings = {
            navController.navigate(Screen.Settings.createRoute())
        },
        onNavigateToApps = {
            navController.navigate(Screen.Apps.route) { launchSingleTop = true }
        },
        onPlayMedia = onPlayMedia,
        onMediaSelect = { itemId ->
            navController.navigate(Screen.MediaDetail.createRoute(itemId))
        }
    )
}
