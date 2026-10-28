package com.example.tubealarm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tubealarm.ui.theme.lineColor

// One station on the map, with the line coming in from above and going out below.
// A start or end has nothing on one side; a change has a different line on each
data class RouteStop(val name: String, val lineAbove: String?, val lineBelow: String?, val leg: Int, val middle: Boolean)

fun routeStops(legs: List<Leg>): List<RouteStop> {
    val stops = mutableListOf<RouteStop>()
    legs.forEachIndexed { i, leg ->
        leg.stops.forEachIndexed { j, name ->
            val first = j == 0
            val last = j == leg.stops.size - 1
            when {
                // A change station was already added as the end of the leg before
                first && i > 0 -> stops[stops.size - 1] = stops.last().copy(lineBelow = leg.line)
                else -> stops += RouteStop(
                    name = name,
                    lineAbove = if (first) null else leg.line,
                    lineBelow = if (last) null else leg.line,
                    leg = i,
                    middle = !first && !last,
                )
            }
        }
    }
    return stops
}

// Legs longer than this fold their middle stations away until tapped
private const val FOLD_AFTER = 4
private val ROW = 30.dp

@Composable
fun RouteMap(legs: List<Leg>) {
    val dark = isSystemInDarkTheme()
    val stops = routeStops(legs)
    val opened = remember(legs) { mutableStateListOf<Int>() }

    Column {
        stops.forEachIndexed { i, stop ->
            val middles = stops.count { it.leg == stop.leg && it.middle }
            val folded = stop.middle && middles > FOLD_AFTER && stop.leg !in opened
            if (folded) {
                // Only the first hidden station draws the "more stops" row
                if (stops.getOrNull(i - 1)?.middle != true) {
                    FoldedRow(stop.lineBelow, middles, dark) { opened += stop.leg }
                }
                return@forEachIndexed
            }
            // The line's name sits just under where it starts
            if (stop.lineBelow != null && stop.lineBelow != stop.lineAbove) {
                StationRow(stop, dark)
                LinePill(stop.lineBelow, dark)
            } else {
                StationRow(stop, dark)
            }
        }
    }
}

@Composable
private fun StationRow(stop: RouteStop, dark: Boolean) {
    val above = stop.lineAbove?.let { lineColor(it, dark) }
    val below = stop.lineBelow?.let { lineColor(it, dark) }
    val change = above != null && below != null && stop.lineAbove != stop.lineBelow
    val end = above == null || below == null
    val ring = MaterialTheme.colorScheme.onSurface
    val fill = MaterialTheme.colorScheme.surfaceContainer

    Row(modifier = Modifier.height(ROW), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(32.dp).fillMaxHeight()) {
            val x = size.width / 2
            val y = size.height / 2
            above?.let { track(it, stop.lineAbove, Offset(x, 0f), Offset(x, y)) }
            below?.let { track(it, stop.lineBelow, Offset(x, y), Offset(x, size.height)) }
            when {
                // Interchange and end stations get the Tube map's white circle with a dark ring
                change || end -> {
                    drawCircle(fill, radius = 8.dp.toPx())
                    drawCircle(ring, radius = 8.dp.toPx(), style = Stroke(width = 3.dp.toPx()))
                }
                else -> drawLine(
                    above ?: Color.Gray, Offset(x, y), Offset(x + 9.dp.toPx(), y), strokeWidth = 4.dp.toPx(),
                )
            }
        }
        Text(
            stop.name,
            style = if (change || end) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (change || end) FontWeight.SemiBold else FontWeight.Normal,
            color = if (change || end) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoldedRow(line: String?, count: Int, dark: Boolean, open: () -> Unit) {
    val colour = lineColor(line, dark)
    Row(
        modifier = Modifier.height(ROW).fillMaxWidth().clickable(onClick = open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.width(32.dp).fillMaxHeight()) {
            track(colour, line, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height))
        }
        Text(
            "${stopsText(count)} in between, tap to show",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LinePill(line: String, dark: Boolean) {
    val colour = lineColor(line, dark)
    Row(modifier = Modifier.height(ROW), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(32.dp).fillMaxHeight()) {
            track(colour, line, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height))
        }
        Text(
            lineName(line),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            // White on the darker lines, black on the light ones like the Circle line's yellow
            color = if (colour.luminance() > 0.5f) Color.Black else Color.White,
            modifier = Modifier
                .background(colour, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

// A walk is dotted and grey, like the walking links on the Tube map
private fun DrawScope.track(colour: Color, line: String?, from: Offset, to: Offset, width: Dp = 6.dp) {
    val walk = line == "walk"
    drawLine(
        colour,
        from,
        to,
        strokeWidth = if (walk) 3.dp.toPx() else width.toPx(),
        cap = StrokeCap.Butt,
        pathEffect = if (walk) PathEffect.dashPathEffect(floatArrayOf(6f, 8f)) else null,
    )
}
