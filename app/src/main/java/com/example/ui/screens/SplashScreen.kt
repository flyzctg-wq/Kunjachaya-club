package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Full-screen splash screen shown on first launch.
 *
 * Behaviour:
 * - If `res/raw/splash_video.mp4` exists → plays the video via ExoPlayer then calls [onFinished].
 * - Otherwise → shows an animated Compose logo splash for [animatedDurationMs] ms then calls [onFinished].
 *
 * To use the video path: drop a file named `splash_video.mp4` into
 * `app/src/main/res/raw/` and rebuild. The app will switch automatically.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val hasVideo = remember { rawVideoExists(context, "splash_video") }

    if (hasVideo) {
        VideoSplash(onFinished = onFinished)
    } else {
        AnimatedLogoSplash(onFinished = onFinished)
    }
}

// ---------------------------------------------------------------------------
// ExoPlayer video splash
// ---------------------------------------------------------------------------

@Composable
private fun VideoSplash(onFinished: () -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val rawUri = Uri.parse(
                "android.resource://${context.packageName}/raw/splash_video"
            )
            setMediaItem(MediaItem.fromUri(rawUri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onFinished()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Safety timeout: if video is longer than 8 s, move on anyway.
    LaunchedEffect(Unit) {
        delay(8_000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ---------------------------------------------------------------------------
// Animated logo splash (used when no MP4 is present)
// ---------------------------------------------------------------------------

@Composable
private fun AnimatedLogoSplash(
    animatedDurationMs: Long = 3_000,
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(animatedDurationMs)
        onFinished()
    }

    // Pulse animation on the logo circle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Fade-in for text
    val textAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, delayMillis = 400),
        label = "text_alpha"
    )

    // Shimmer for the tagline
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1628),   // deep navy
                        Color(0xFF1B2D4F),   // mid-navy
                        Color(0xFF0D1F3C),   // dark blue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo circle with ambient glow
            Box(
                modifier = Modifier
                    .scale(pulse)
                    .size(130.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4FC3F7),   // sky blue center
                                Color(0xFF0288D1),   // deep sky
                                Color(0xFF01579B),   // navy edge
                            )
                        ),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "কুঞ্জ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Club name
            Text(
                text = "Kunjachaya Club",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE3F2FD),
                modifier = Modifier.alpha(textAlpha),
                letterSpacing = 1.5.sp
            )

            // Tagline
            Text(
                text = "কুঞ্জছায়া আবাসিক ক্লাব",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF90CAF9).copy(alpha = 0.85f + 0.15f * shimmer),
                modifier = Modifier.alpha(textAlpha),
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading dots
            LoadingDots()
        }
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotAlphas = (0..2).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = i * 200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_$i"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotAlphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha.value)
                    .background(
                        Color(0xFF4FC3F7),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Utility
// ---------------------------------------------------------------------------

private fun rawVideoExists(context: Context, resourceName: String): Boolean {
    return try {
        val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        resId != 0
    } catch (e: Exception) {
        false
    }
}
