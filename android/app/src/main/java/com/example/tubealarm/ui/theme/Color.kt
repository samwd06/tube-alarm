package com.example.tubealarm.ui.theme

import androidx.compose.ui.graphics.Color

// The Underground's own blue and red, as on the roundel
val TubeBlue = Color(0xFF0019A8)
val TubeRed = Color(0xFFDC241F)
val TubeBlueLight = Color(0xFFAEB9FF)
val TubeRedLight = Color(0xFFFFB4AB)

// TfL's colour for each line. The Northern line is black, which disappears on a dark screen,
// so it turns light grey there
fun lineColor(line: String?, dark: Boolean = false): Color = when (line) {
    "bakerloo" -> Color(0xFFB36305)
    "central" -> Color(0xFFE32017)
    "circle" -> Color(0xFFFFD300)
    "district" -> Color(0xFF00782A)
    "hammersmith-city" -> Color(0xFFF3A9BB)
    "jubilee" -> Color(0xFFA0A5A9)
    "metropolitan" -> Color(0xFF9B0056)
    "northern" -> if (dark) Color(0xFFD0D0D0) else Color(0xFF000000)
    "piccadilly" -> Color(0xFF003688)
    "victoria" -> Color(0xFF0098D4)
    "waterloo-city" -> Color(0xFF95CDBA)
    else -> Color(0xFF8A8A8A)
}
