package com.nendo.argosy.ui.hela.theme

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.nendo.argosy.data.hela.theme.CocoonIcons
import com.nendo.argosy.data.model.hela.PackIconOverlay
import com.nendo.argosy.ui.common.rememberFileImageModel
import com.nendo.argosy.ui.components.DownloadProgressBadge
import com.nendo.argosy.ui.screens.home.GameDownloadIndicator
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.theme.LocalBoxArtStyle
import com.nendo.argosy.ui.theme.Motion
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

private const val MASK_DECODE_PX = 512
private const val SQUARE = 1f
private const val GLOW_CORNER_RATIO = 0.18f

data class HelaIcons(val byPlatform: Map<String, PackIconOverlay>)

val LocalHelaIcons = staticCompositionLocalOf<HelaIcons?> { null }

/**
 * Applies the selected icon pack to the game tiles inside [content]: tiles become square and
 * [rememberHelaIcon] returns the pack overlay of a platform. Without a selected pack the
 * content renders as Argosy draws it.
 */
@Composable
fun HelaIconScope(content: @Composable () -> Unit) {
    val store = rememberHelaThemeStore()
    val iconsFlow = remember(store) { store?.icons ?: MutableStateFlow(null) }
    val byPlatform by iconsFlow.collectAsState()
    val icons = remember(byPlatform) { byPlatform?.let(::HelaIcons) }
    val style = LocalBoxArtStyle.current
    val tileStyle = remember(style, icons) {
        if (icons != null) style.copy(aspectRatio = SQUARE, nativeAspectRatio = false) else style
    }
    CompositionLocalProvider(
        LocalHelaIcons provides icons,
        LocalBoxArtStyle provides tileStyle,
        content = content
    )
}

@Composable
fun rememberHelaIcon(platformSlug: String): PackIconOverlay? {
    val icons = LocalHelaIcons.current ?: return null
    return remember(icons, platformSlug) { CocoonIcons.resolve(icons.byPlatform, platformSlug) }
}

@Composable
fun HelaIconTile(
    icon: PackIconOverlay,
    coverPath: String,
    title: String,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
    focusScale: Float = ComponentDefaults.Focus.scaleFocused,
    scalePivotY: Float = 0.5f,
    scaleOverride: Float? = null,
    alphaOverride: Float? = null,
    colorFilter: ColorFilter? = null,
    downloadIndicator: GameDownloadIndicator = GameDownloadIndicator.NONE
) {
    val scale by animateFloatAsState(
        targetValue = scaleOverride
            ?: if (isFocused) focusScale else ComponentDefaults.Focus.scaleDefault,
        animationSpec = Motion.focusSpring,
        label = "helaIconScale"
    )
    val alpha by animateFloatAsState(
        targetValue = alphaOverride
            ?: if (isFocused) ComponentDefaults.Focus.alphaFocused else ComponentDefaults.Focus.alphaUnfocused,
        animationSpec = Motion.focusSpring,
        label = "helaIconAlpha"
    )
    val glowColor = LocalArgosyTheme.current.focusAccent
    val boxArtStyle = LocalBoxArtStyle.current
    val glowRadius = boxArtStyle.outerEffectThicknessPx
    val glowAlpha = boxArtStyle.glowAlpha

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, scalePivotY)
                this.alpha = alpha
                clip = false
            }
            .then(
                if (isFocused && glowRadius > 0f) {
                    Modifier.drawBehind {
                        val corner = size.minDimension * GLOW_CORNER_RATIO
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                                color = glowColor.copy(alpha = glowAlpha).toArgb()
                            }
                            canvas.nativeCanvas.drawRoundRect(
                                0f, 0f, size.width, size.height, corner, corner, paint
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        key(icon.maskPath) {
            MaskedCover(
                maskPath = icon.maskPath,
                coverPath = coverPath,
                title = title,
                colorFilter = colorFilter
            )
        }
        if (icon.overlayPath != null) {
            AsyncImage(
                model = File(icon.overlayPath),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (downloadIndicator.isShown) {
            DownloadProgressBadge(
                progress = downloadIndicator.progress,
                badgeSize = Dimens.iconLg,
                modifier = Modifier.align(Alignment.Center),
                paused = downloadIndicator.isPaused
            )
        }
    }
}

@Composable
private fun MaskedCover(
    maskPath: String?,
    coverPath: String,
    title: String,
    colorFilter: ColorFilter?
) {
    val context = LocalContext.current
    val maskPainter = if (maskPath != null) {
        rememberAsyncImagePainter(
            model = remember(maskPath) {
                ImageRequest.Builder(context).data(File(maskPath)).size(MASK_DECODE_PX).build()
            }
        )
    } else {
        null
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                if (maskPainter != null && maskPainter.state is AsyncImagePainter.State.Success) {
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(
                            Rect(Offset.Zero, size),
                            Paint().apply { blendMode = BlendMode.DstIn }
                        )
                        with(maskPainter) { draw(size) }
                        canvas.restore()
                    }
                }
            }
    ) {
        if (coverPath.isNotEmpty()) {
            AsyncImage(
                model = rememberFileImageModel(coverPath),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}
