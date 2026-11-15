package com.nendo.argosy.ui.hela.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nendo.argosy.R
import com.nendo.argosy.data.hela.theme.CocoonSmartFolders
import com.nendo.argosy.data.model.SortOption
import com.nendo.argosy.data.model.hela.PackFolderArt
import com.nendo.argosy.data.model.hela.PackSmartFolders
import com.nendo.argosy.ui.common.rememberFileImageModel
import com.nendo.argosy.ui.dualscreen.CompanionDetail
import com.nendo.argosy.ui.hela.theme.rememberHelaThemeStore
import com.nendo.argosy.ui.primitives.FocusIndicators
import com.nendo.argosy.ui.primitives.argosyFocusIndicators
import com.nendo.argosy.ui.screens.home.HomeRow
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import com.nendo.argosy.ui.util.clickableNoFocus
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun rememberHelaFolders(): PackSmartFolders? {
    val store = rememberHelaThemeStore()
    val flow = remember(store) { store?.folders ?: MutableStateFlow(null) }
    val folders by flow.collectAsState()
    return folders
}

fun HelaFolderTarget.art(folders: PackSmartFolders?): PackFolderArt? = when (this) {
    is HelaFolderTarget.Special -> folders?.special?.get(artKey)
    is HelaFolderTarget.Sorted -> folders?.special?.get(artKey)
    is HelaFolderTarget.Platform -> folders?.let { CocoonSmartFolders.resolve(it, platform.slug) }
}

@Composable
fun HelaFolderTarget.label(): String = when (this) {
    is HelaFolderTarget.Platform -> platform.displayName
    is HelaFolderTarget.Special ->
        if (row == HomeRow.Favorites) stringResource(R.string.hela_home_folder_favorites)
        else stringResource(R.string.hela_home_folder_recent)
    is HelaFolderTarget.Sorted ->
        if (sort == SortOption.PLAY_TIME) stringResource(R.string.hela_home_folder_most_played)
        else stringResource(R.string.hela_home_folder_newly_added)
}

fun helaFolderDetail(label: String, art: PackFolderArt?): CompanionDetail = CompanionDetail(
    title = label,
    platformSlug = HELA_FOLDER_SLUG,
    backdropUrl = art?.heroPath,
    logoUrl = art?.logoPath
)

internal fun helaGridColumns(maxWidth: Dp, gap: Dp): Int = maxOf(
    ComponentDefaults.HelaHome.minColumns,
    ((maxWidth + gap) / (ComponentDefaults.HelaHome.tileSizeDp.dp + gap)).toInt()
)

@Composable
fun HelaFoldersGrid(
    targets: List<HelaFolderTarget>,
    folders: PackSmartFolders?,
    focusedIndex: Int?,
    onColumns: (Int) -> Unit,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val gap = Dimens.spacingMd
        val columns = helaGridColumns(maxWidth, gap)
        val tileHeight = (maxWidth - gap * (columns - 1)) / columns
        LaunchedEffect(columns) { onColumns(columns) }

        val gridState = rememberLazyGridState()
        LaunchedEffect(focusedIndex, targets.size) {
            if (focusedIndex != null && focusedIndex in targets.indices) {
                gridState.animateScrollToItem(focusedIndex)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(vertical = Dimens.spacingLg)
        ) {
            itemsIndexed(targets, key = { _, target -> target.stableKey() }) { index, target ->
                HelaFolderTile(
                    label = target.label(),
                    iconPath = target.art(folders)?.iconPath,
                    symbol = target.symbol(),
                    isFocused = index == focusedIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tileHeight)
                        .clickableNoFocus { onTap(index) }
                )
            }
        }
    }
}

private fun HelaFolderTarget.stableKey(): String = when (this) {
    is HelaFolderTarget.Special -> "special-$artKey"
    is HelaFolderTarget.Sorted -> "sorted-$artKey"
    is HelaFolderTarget.Platform -> "platform-${platform.id}"
}

@Composable
private fun HelaFolderTarget.symbol(): ImageVector? = when (this) {
    is HelaFolderTarget.Platform -> null
    is HelaFolderTarget.Special -> if (row == HomeRow.Favorites) Icons.Default.Favorite else Icons.Default.History
    is HelaFolderTarget.Sorted -> if (sort == SortOption.PLAY_TIME) Icons.Default.Whatshot else Icons.Default.NewReleases
}

@Composable
private fun HelaFolderTile(
    label: String,
    iconPath: String?,
    symbol: ImageVector?,
    isFocused: Boolean,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(Dimens.radiusLg)
    Box(
        modifier = modifier
            .argosyFocusIndicators(focused = isFocused, indicators = FocusIndicators.Tile, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (iconPath != null) {
            AsyncImage(
                model = rememberFileImageModel(iconPath),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Dimens.spacingSm)
            ) {
                if (symbol != null) {
                    Icon(
                        imageVector = symbol,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(bottom = Dimens.spacingSm)
                            .size(Dimens.iconXl)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
