package com.nendo.argosy.ui.hela.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nendo.argosy.R
import com.nendo.argosy.ui.common.rememberFileImageModel
import com.nendo.argosy.ui.common.rememberResolvedArt
import com.nendo.argosy.ui.dualscreen.CompanionDetail
import com.nendo.argosy.ui.dualscreen.rememberShowcaseBackdrop
import com.nendo.argosy.ui.dualscreen.showcaseMotionDurations
import com.nendo.argosy.ui.dualscreen.withLiveArt
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.LocalMotionTier
import com.nendo.argosy.ui.theme.MotionTier
import com.nendo.argosy.ui.theme.generated.ColorTokens
import com.nendo.argosy.ui.theme.generated.ComponentDefaults

/**
 * The showcase screen's answer for a focused game while the Hela home drives the other screen:
 * the game's art across the whole screen, its logo in the middle, its cover as a card in the
 * bottom-left corner and its RetroAchievements progress in the bottom-right one.
 */
@Composable
fun HelaGameDetail(detail: CompanionDetail, bottomInset: Dp, modifier: Modifier = Modifier) {
    val art = detail.gameId?.let { rememberResolvedArt(it) }
    val live = remember(detail, art) { detail.withLiveArt(art) }
    val theme = LocalArgosyTheme.current
    val reduced = LocalMotionTier.current == MotionTier.Reduced

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(theme.surfaceBase)) {
        val backdropPath = live.backdropUrl ?: live.artUrl
        if (backdropPath != null) {
            val backdrop = rememberShowcaseBackdrop(
                path = backdropPath,
                crossfadeMs = showcaseMotionDurations(reduced).backdropCrossfadeMs,
                stackedScreenshot = live.backdropUrl != null && live.backdropIsScreenshotOrArt
            )
            AsyncImage(
                model = backdrop.request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = backdrop.onSuccess,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            theme.surfaceBase.copy(alpha = ComponentDefaults.HelaHome.detailScrimTopAlpha),
                            theme.surfaceBase.copy(alpha = ComponentDefaults.HelaHome.detailScrimBottomAlpha)
                        )
                    )
                )
        )

        val logoMaxWidth = maxWidth * ComponentDefaults.HelaHome.detailLogoWidthRatio
        val logoMaxHeight = maxHeight * ComponentDefaults.HelaHome.detailLogoHeightRatio
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.spacingXl)
                .padding(top = Dimens.spacingXxl, bottom = bottomInset + Dimens.spacingLg)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HelaDetailLogo(
                    logoPath = live.logoUrl,
                    title = live.title,
                    modifier = Modifier
                        .widthIn(max = logoMaxWidth)
                        .heightIn(max = logoMaxHeight)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val cover = live.artUrl
                if (cover != null) HelaDetailMediaCard(coverPath = cover, title = live.title) else Spacer(Modifier)
                val stats = live.stats
                if (stats != null && stats.achievementCount > 0) {
                    HelaDetailAchievements(earned = stats.earnedAchievementCount, total = stats.achievementCount)
                }
            }
        }
    }
}

@Composable
private fun HelaDetailLogo(logoPath: String?, title: String, modifier: Modifier) {
    var logoFailed by remember(logoPath) { mutableStateOf(false) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (logoPath != null && !logoFailed) {
            AsyncImage(
                model = rememberFileImageModel(logoPath),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                onError = { logoFailed = true }
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = LocalArgosyTheme.current.surfaceBase,
                        blurRadius = ComponentDefaults.HelaHome.detailTitleShadowBlurPx.toFloat()
                    )
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HelaDetailMediaCard(coverPath: String, title: String) {
    val shape = RoundedCornerShape(Dimens.radiusLg)
    val offset = ComponentDefaults.HelaHome.detailMediaStackOffsetDp.dp
    val layer = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .width(ComponentDefaults.HelaHome.detailMediaCardWidthDp.dp)
            .aspectRatio(ComponentDefaults.HelaHome.detailMediaCardAspectRatio)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = offset * 2, y = -offset * 2)
                .clip(shape)
                .background(layer.copy(alpha = ComponentDefaults.HelaHome.detailScrimBottomAlpha))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = offset, y = -offset)
                .clip(shape)
                .background(layer.copy(alpha = ComponentDefaults.HelaHome.labelScrimAlpha))
        )
        AsyncImage(
            model = rememberFileImageModel(coverPath),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(layer)
        )
    }
}

@Composable
private fun HelaDetailAchievements(earned: Int, total: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = ComponentDefaults.HelaHome.labelScrimAlpha))
            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = ColorTokens.Domain.trophyAmber,
            modifier = Modifier.size(ComponentDefaults.HelaHome.tabIconDp.dp)
        )
        Text(
            text = stringResource(R.string.hela_detail_achievements, earned, total),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
