package com.nendo.argosy.ui.hela.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nendo.argosy.ui.screens.home.GameDownloadIndicator
import com.nendo.argosy.ui.screens.home.HomeRow
import com.nendo.argosy.ui.screens.home.HomeRowItem
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import com.nendo.argosy.ui.util.clickableNoFocus

@Composable
fun HelaHomeGrid(
    items: List<HomeRowItem>,
    page: HomeRow,
    focusedIndex: Int?,
    downloadIndicators: State<Map<Long, GameDownloadIndicator>>,
    onColumns: (Int) -> Unit,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val gap = Dimens.spacingMd
        val tile = ComponentDefaults.HelaHome.tileSizeDp.dp
        val columns = maxOf(
            ComponentDefaults.HelaHome.minColumns,
            ((maxWidth + gap) / (tile + gap)).toInt()
        )
        val tileHeight = (maxWidth - gap * (columns - 1)) / columns
        LaunchedEffect(columns) { onColumns(columns) }

        val wideFirst = page == HomeRow.Continue
        val cells = remember(items.size, columns, wideFirst) {
            HelaGridLayout.cells(items.size, columns, wideFirst)
        }
        val gridState = rememberLazyGridState()
        LaunchedEffect(page, focusedIndex, items.size) {
            if (focusedIndex != null && focusedIndex in items.indices) {
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
            itemsIndexed(
                items = items,
                key = { index, item -> item.helaKey(index) },
                span = { index, _ -> GridItemSpan(cells.getOrNull(index)?.span ?: 1) }
            ) { index, item ->
                val gameId = (item as? HomeRowItem.Game)?.game?.id
                HelaHomeTile(
                    item = item,
                    isFocused = index == focusedIndex,
                    wide = (cells.getOrNull(index)?.span ?: 1) > 1,
                    downloadIndicator = gameId?.let { downloadIndicators.value[it] }
                        ?: GameDownloadIndicator.NONE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tileHeight)
                        .clickableNoFocus(onClick = { onTap(index) }, onLongClick = { onLongPress(index) })
                )
            }
        }
    }
}

@Composable
fun HelaHomePageDots(
    pages: List<HomeRow>,
    current: HomeRow,
    onSelect: (HomeRow) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pages.isEmpty()) return
    Row(
        modifier = modifier.padding(vertical = Dimens.spacingSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        current.shortLabelRes?.let { label ->
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = Dimens.spacingSm)
            )
        }
        pages.forEach { page ->
            val color = if (page == current) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(ComponentDefaults.HelaHome.pageDotDp.dp * 2)
                    .clickableNoFocus { onSelect(page) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(ComponentDefaults.HelaHome.pageDotDp.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

private fun HomeRowItem.helaKey(index: Int): String = when (this) {
    is HomeRowItem.Game -> "game-${game.id}"
    is HomeRowItem.Media -> "media-${media.itemId}"
    is HomeRowItem.ViewAll -> "all-$index"
}
