package com.example.xingtuclone.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var start by remember { mutableStateOf(false) }

    val transition = updateTransition(targetState = start, label = "splash")
    val circleSize by transition.animateDp(label = "size", transitionSpec = { tween(durationMillis = 1200, easing = FastOutSlowInEasing) }) { s -> if (s) 180.dp else 40.dp }
    val circleAlpha by transition.animateFloat(label = "alpha", transitionSpec = { tween(1200) }) { s -> if (s) 0.8f else 0.0f }
    val titleAlpha by transition.animateFloat(label = "title", transitionSpec = { tween(1000) }) { s -> if (s) 1f else 0f }

    LaunchedEffect(Unit) {
        start = true
        delay(1400)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 绿色光圈
        Box(
            modifier = Modifier
                .size(circleSize)
                .align(Alignment.Center)
                .alpha(circleAlpha)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFCCFF00), Color(0x55CCFF00), Color.Transparent),
                        radius = 300f
                    )
                )
        )

        // 底部品牌字样
        Text(
            text = "醒图",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(titleAlpha)
        )
    }
}

