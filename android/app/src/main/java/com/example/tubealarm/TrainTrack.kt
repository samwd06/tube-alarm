package com.example.tubealarm

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tubealarm.ui.theme.TubeBlue
import com.example.tubealarm.ui.theme.TubeRed
import com.example.tubealarm.ui.theme.lineColor

// Where the passenger is along the route, counting every station once, or null if they're not on it
fun trainIndex(legs: List<Leg>, current: String): Int? =
    routeStops(legs).indexOfFirst { it.name.equals(current, ignoreCase = true) }.takeIf { it >= 0 }

@Composable
fun TrainTrack(progress: Progress) {
    val stops = routeStops(progress.legs)
    val here = trainIndex(progress.legs, progress.current) ?: return
    val getOff = stops.indexOfFirst { it.name.equals(progress.nextStopOff, ignoreCase = true) }
    val dark = isSystemInDarkTheme()
    val ring = MaterialTheme.colorScheme.onSurface
    val fill = MaterialTheme.colorScheme.surfaceContainer

    // The train glides to each new station rather than jumping there
    val position by animateFloatAsState(
        targetValue = here.toFloat(),
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "train",
    )
    val motion = rememberInfiniteTransition(label = "motion")
    // A gentle bob, like a carriage on the rails
    val bob by motion.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bob",
    )
    // The stop to get off at pulses when it's time to wake up
    val pulse by motion.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart), label = "pulse",
    )

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(64.dp)) {
            val pad = 18.dp.toPx()
            val y = size.height - 18.dp.toPx()
            val gap = if (stops.size > 1) (size.width - 2 * pad) / (stops.size - 1) else 0f
            fun x(i: Float) = pad + i * gap

            // The rails, each stretch in its line's colour, faded once it's behind the train
            for (i in 0 until stops.size - 1) {
                val line = stops[i].lineBelow
                val colour = lineColor(line, dark)
                drawLine(
                    if (i < here) colour.copy(alpha = 0.3f) else colour,
                    Offset(x(i.toFloat()), y), Offset(x(i + 1f), y),
                    strokeWidth = if (line == "walk") 3.dp.toPx() else 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            stops.forEachIndexed { i, stop ->
                val at = Offset(x(i.toFloat()), y)
                val big = stop.lineAbove == null || stop.lineBelow == null || stop.lineAbove != stop.lineBelow
                if (i == getOff) {
                    if (progress.wakeUp) {
                        drawCircle(TubeRed.copy(alpha = 0.5f * (1 - pulse)), radius = (8 + 14 * pulse).dp.toPx(), center = at)
                    }
                    drawCircle(fill, radius = 7.dp.toPx(), center = at)
                    drawCircle(TubeRed, radius = 7.dp.toPx(), center = at, style = Stroke(3.dp.toPx()))
                } else if (big) {
                    drawCircle(fill, radius = 6.dp.toPx(), center = at)
                    drawCircle(ring, radius = 6.dp.toPx(), center = at, style = Stroke(2.5.dp.toPx()))
                } else {
                    drawCircle(fill, radius = 2.dp.toPx(), center = at)
                }
            }

            // Lifted just enough for the wheels to sit on the rail
            train(Offset(x(position), y - 7.dp.toPx() - bob * 1.5.dp.toPx()))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                stops.first().name, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Text(
                stops.last().name, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
        }
    }
}

// A little Tube train facing right, its bottom centre at the given point
private fun DrawScope.train(bottom: Offset) {
    val w = 38.dp.toPx()
    val h = 18.dp.toPx()
    val left = bottom.x - w / 2
    val top = bottom.y - h

    // Body with a sloped nose, red on top and a blue skirt along the bottom
    val body = Path().apply {
        moveTo(left + 4.dp.toPx(), top)
        lineTo(left + w - 8.dp.toPx(), top)
        quadraticTo(left + w, top, left + w, top + 9.dp.toPx())
        lineTo(left + w, bottom.y)
        lineTo(left, bottom.y)
        lineTo(left, top + 4.dp.toPx())
        quadraticTo(left, top, left + 4.dp.toPx(), top)
        close()
    }
    drawPath(body, TubeRed)
    drawRect(TubeBlue, Offset(left, bottom.y - 4.dp.toPx()), Size(w, 4.dp.toPx()))

    // Windows, and a bigger one at the front for the driver
    for (i in 0..2) {
        drawRoundRect(
            Color.White, Offset(left + 4.dp.toPx() + i * 8.dp.toPx(), top + 3.dp.toPx()),
            Size(6.dp.toPx(), 6.dp.toPx()), CornerRadius(1.5.dp.toPx()),
        )
    }
    drawRoundRect(
        Color.White, Offset(left + w - 10.dp.toPx(), top + 3.dp.toPx()),
        Size(7.dp.toPx(), 6.dp.toPx()), CornerRadius(2.dp.toPx()),
    )

    // Wheels sit on the rails
    for (x in listOf(left + 8.dp.toPx(), left + w - 8.dp.toPx())) {
        drawCircle(Color(0xFF2B2B2B), radius = 3.dp.toPx(), center = Offset(x, bottom.y + 1.dp.toPx()))
    }
}
