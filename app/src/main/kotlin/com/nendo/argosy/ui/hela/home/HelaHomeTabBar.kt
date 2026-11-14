package com.nendo.argosy.ui.hela.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nendo.argosy.R
import com.nendo.argosy.ui.primitives.FocusIndicators
import com.nendo.argosy.ui.primitives.argosyFocusIndicators
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import com.nendo.argosy.ui.util.clickableNoFocus

@Composable
fun HelaHomeTabBar(
    focusedTab: HelaHomeTab?,
    onTab: (HelaHomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(Dimens.radiusPill)
    Row(
        modifier = modifier
            .height(ComponentDefaults.HelaHome.tabBarHeightDp.dp)
            .clip(barShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = ComponentDefaults.HelaHome.labelScrimAlpha))
            .padding(horizontal = Dimens.spacingXs),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HelaHomeTab.entries.forEach { tab ->
            HelaHomeTabPill(
                tab = tab,
                isFocused = tab == focusedTab,
                onClick = { onTab(tab) }
            )
        }
    }
}

@Composable
private fun HelaHomeTabPill(tab: HelaHomeTab, isFocused: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.radiusPill)
    val contentColor = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = Dimens.spacingXs)
            .argosyFocusIndicators(focused = isFocused, indicators = FocusIndicators.Pill, shape = shape)
            .clip(shape)
            .clickableNoFocus(onClick = onClick)
            .padding(horizontal = Dimens.spacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = stringResource(tab.labelRes).takeIf { !isFocused },
            tint = contentColor,
            modifier = Modifier.size(ComponentDefaults.HelaHome.tabIconDp.dp)
        )
        if (isFocused) {
            Spacer(modifier = Modifier.width(Dimens.spacingSm))
            Text(
                text = stringResource(tab.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@get:StringRes
val HelaHomeTab.labelRes: Int
    get() = when (this) {
        HelaHomeTab.LIBRARY -> R.string.hela_home_tab_library
        HelaHomeTab.COLLECTIONS -> R.string.hela_home_tab_collections
        HelaHomeTab.APPS -> R.string.hela_home_tab_apps
        HelaHomeTab.SETTINGS -> R.string.hela_home_tab_settings
    }

val HelaHomeTab.icon: ImageVector
    get() = when (this) {
        HelaHomeTab.LIBRARY -> Icons.Default.VideoLibrary
        HelaHomeTab.COLLECTIONS -> Icons.Default.CollectionsBookmark
        HelaHomeTab.APPS -> Icons.Default.Apps
        HelaHomeTab.SETTINGS -> Icons.Default.Settings
    }
