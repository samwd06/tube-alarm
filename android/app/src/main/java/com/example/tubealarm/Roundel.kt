package com.example.tubealarm

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tubealarm.ui.theme.TubeBlue
import com.example.tubealarm.ui.theme.TubeRed

// A small Underground roundel: a red ring with a blue bar across it
@Composable
fun Roundel(modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Box(modifier = modifier.size(width = size * 1.4f, height = size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val ring = this.size.minDimension * 0.18f
            drawCircle(TubeRed, radius = (this.size.minDimension - ring) / 2, style = Stroke(width = ring))
        }
        Box(
            modifier = Modifier
                .background(TubeBlue)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text("TUBE ALARM", color = Color.White, fontSize = (size.value / 6).sp, fontWeight = FontWeight.Bold)
        }
    }
}
