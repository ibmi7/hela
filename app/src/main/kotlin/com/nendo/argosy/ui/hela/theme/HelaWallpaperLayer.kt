package com.nendo.argosy.ui.hela.theme

import android.graphics.Matrix
import android.net.Uri
import android.view.Display
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.nendo.argosy.data.hela.theme.HelaThemeStore
import com.nendo.argosy.data.model.hela.PackWallpaper
import com.nendo.argosy.data.model.hela.WallpaperKind
import com.nendo.argosy.ui.theme.LocalLauncherTheme
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlin.math.max

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HelaThemeEntryPoint {
    fun helaThemeStore(): HelaThemeStore
}

data class HelaScreenWallpaper(
    val wallpaper: PackWallpaper,
    val animate: Boolean
)

/**
 * Pack wallpaper of the display this composition renders on: the main slot on the default
 * display, the external slot on any other. Null when no pack supplies that slot.
 */
@Composable
fun rememberHelaWallpaper(): HelaScreenWallpaper? {
    val context = LocalContext.current
    val store = remember(context) {
        runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, HelaThemeEntryPoint::class.java)
                .helaThemeStore()
        }.getOrNull()
    } ?: return null
    val wallpapers by store.wallpapers.collectAsState()
    val onDefaultDisplay =
        ContextCompat.getDisplayOrDefault(context).displayId == Display.DEFAULT_DISPLAY
    val wallpaper = (if (onDefaultDisplay) wallpapers.main else wallpapers.external) ?: return null
    return remember(wallpaper, wallpapers.animate) {
        HelaScreenWallpaper(wallpaper, wallpapers.animate)
    }
}

@Composable
fun HelaWallpaperLayer(
    wallpaper: HelaScreenWallpaper,
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    scrim: Boolean = false
) {
    val media = wallpaper.wallpaper
    Box(modifier = modifier) {
        val imagePath = when (media.kind) {
            WallpaperKind.IMAGE -> media.path
            WallpaperKind.VIDEO -> media.stillPath
        }
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (media.kind == WallpaperKind.VIDEO && wallpaper.animate) {
            key(media.path) {
                WallpaperVideo(path = media.path, paused = paused, modifier = Modifier.fillMaxSize())
            }
        }
        if (scrim) {
            Box(modifier = Modifier.fillMaxSize().background(LocalLauncherTheme.current.overlayLight))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun WallpaperVideo(path: String, paused: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                prepare()
            }
        }.getOrNull()
    } ?: return

    var started by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> started = true
                Lifecycle.Event.ON_STOP -> started = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    LaunchedEffect(player, started, paused) {
        player.playWhenReady = started && !paused
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            TextureView(viewContext).also { view ->
                view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    view.centerCrop(player.videoSize)
                }
                player.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        view.centerCrop(videoSize)
                    }
                })
                player.setVideoTextureView(view)
            }
        }
    )
}

private fun TextureView.centerCrop(videoSize: VideoSize) {
    if (width == 0 || height == 0 || videoSize.width == 0 || videoSize.height == 0) return
    val viewRatio = width.toFloat() / height
    val videoRatio = videoSize.width.toFloat() / videoSize.height
    val matrix = Matrix()
    matrix.setScale(
        max(1f, videoRatio / viewRatio),
        max(1f, viewRatio / videoRatio),
        width / 2f,
        height / 2f
    )
    setTransform(matrix)
}
