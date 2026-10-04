package com.lifeyourchoice.app.ui.cinema

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import com.lifeyourchoice.app.ui.art.SceneCanvas
import com.lifeyourchoice.app.ui.art.drawFigure
import com.lifeyourchoice.app.ui.art.drawFigureShadow
import com.lifeyourchoice.app.ui.art.paintTimeOfDay

private const val FIGURE_UNITS = 222f
private const val FLOOR_Y = 0.92f
private const val FIGURE_HEIGHT = 0.60f

/**
 * The movie screen: environment and actors drawn together, then moved by the camera
 * (zoom + pan) with a graphics layer, so cuts, push-ins and follows are cheap and crisp.
 */
@Composable
fun StageView(rt: StageRuntime, modifier: Modifier = Modifier) {
    Box(modifier.clipToBounds().background(Color.Black)) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                val z = rt.camZoom
                scaleX = z; scaleY = z
                transformOrigin = TransformOrigin(0f, 0f)
                translationX = (0.5f - rt.camX * z) * size.width
                translationY = (0.5f - rt.camY * z) * size.height
            }
        ) {
            SceneCanvas(rt.env, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                rt.frame // redraw every frame
                drawActors(rt)
                paintTimeOfDay(rt.time)
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val a = maxOf(rt.blackout, rt.cutFlash)
            if (a > 0.001f) drawRect(Color.Black.copy(alpha = a.coerceIn(0f, 1f)))
            // letterbox-style vignette keeps focus on the action
            drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0x55000000)),
                center = Offset(size.width / 2f, size.height * 0.5f), radius = size.maxDimension * 0.75f))
        }
    }
}

private fun DrawScope.drawActors(rt: StageRuntime) {
    val ordered = rt.figures.values.filter { it.appeared > 0.02f }.sortedBy { it.x }
    for (f in ordered) {
        val info = rt.cast[f.id] ?: continue
        val s = size.height * FIGURE_HEIGHT * info.scale / FIGURE_UNITS
        val feetX = f.x * size.width
        val feetY = size.height * FLOOR_Y
        drawFigureShadow(feetX, feetY, 80f * s)
        translate(feetX, feetY) {
            scale(s, s, pivot = Offset.Zero) {
                drawFigure(info.look, info.gender, info.age, f.pose, rt.faceOf(f), f.prop, f.facing, f.x * 3f)
            }
        }
    }
}
