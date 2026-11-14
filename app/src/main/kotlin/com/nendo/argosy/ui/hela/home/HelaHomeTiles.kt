package com.nendo.argosy.ui.hela.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nendo.argosy.R
import com.nendo.argosy.ui.common.rememberFileImageModel
import com.nendo.argosy.ui.common.rememberResolvedArt
import com.nendo.argosy.ui.hela.theme.HelaIconTile
import com.nendo.argosy.ui.hela.theme.rememberHelaIcon
import com.nendo.argosy.ui.primitives.FocusIndicators
import com.nendo.argosy.ui.primitives.argosyFocusIndicators
import com.nendo.argosy.ui.screens.home.GameDownloadIndicator
import com.nendo.argosy.ui.screens.home.HomeGameUi
import com.nendo.argosy.ui.screens.home.HomeRowItem
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.generated.ComponentDefaults

@Composable
fun HelaHomeTile(
    item: HomeRowItem,
    isFocused: Boolean,
    wide: Boolean,
    downloadIndicator: GameDownloadIndicator,
    modifier: Modifier = Modifier
) {
    when (item) {
        is HomeRowItem.Game -> if (wide) {
            HelaResumeTile(game = item.game, isFocused = isFocused, modifier = modifier)
        } else {
            HelaGameTile(
                game = item.game,
                isFocused = isFocused,
                downloadIndicator = downloadIndicator,
                modifier = modifier
            )
        }
        is HomeRowItem.Media -> HelaArtTile(
            model = item.media.posterUrl,
            title = item.media.title,
            isFocused = isFocused,
            modifier = modifier
        )
        is HomeRowItem.ViewAll -> HelaViewAllTile(isFocused = isFocused, modifier = modifier)
    }
}

@Composable
private fun HelaGameTile(
    game: HomeGameUi,
    isFocused: Boolean,
    downloadIndicator: GameDownloadIndicator,
    modifier: Modifier
) {
    val coverPath = rememberResolvedArt(game.id)?.coverPath ?: game.coverPath
    val icon = rememberHelaIcon(game.platformSlug)
    if (icon != null && coverPath != null) {
        HelaIconTile(
            icon = icon,
            coverPath = coverPath,
            title = game.title,
            isFocused = isFocused,
            modifier = modifier,
            downloadIndicator = downloadIndicator
        )
    } else {
        HelaArtTile(
            model = rememberFileImageModel(coverPath),
            title = game.title,
            isFocused = isFocused,
            modifier = modifier
        )
    }
}

@Composable
private fun HelaResumeTile(game: HomeGameUi, isFocused: Boolean, modifier: Modifier) {
    val art = rememberResolvedArt(game.id)
    val path = art?.backgroundPath ?: game.backgroundPath ?: art?.coverPath ?: game.coverPath
    HelaArtTile(
        model = rememberFileImageModel(path),
        title = game.title,
        isFocused = isFocused,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.hela_home_tile_resume),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimens.spacingSm)
                .clip(RoundedCornerShape(Dimens.radiusPill))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = ComponentDefaults.HelaHome.labelScrimAlpha))
                .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingXs)
        )
    }
}

@Composable
private fun HelaViewAllTile(isFocused: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(Dimens.radiusLg)
    Box(
        modifier = modifier
            .argosyFocusIndicators(focused = isFocused, indicators = FocusIndicators.Tile, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ComponentDefaults.HelaHome.tabIconDp.dp)
            )
            Text(
                text = stringResource(R.string.hela_home_tile_view_all),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spacingXs)
            )
        }
    }
}

@Composable
private fun HelaArtTile(
    model: Any?,
    title: String,
    isFocused: Boolean,
    modifier: Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(Dimens.radiusLg)
    Box(
        modifier = modifier
            .argosyFocusIndicators(focused = isFocused, indicators = FocusIndicators.Tile, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = model,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        overlay()
    }
}
