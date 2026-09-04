package com.duggustore.app.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.R
import com.duggustore.app.ui.theme.*

/**
 * Shown while the stored session is being restored, so the app does not flash the
 * login screen before it knows whether someone is already signed in.
 */
@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "Duggu Store",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(200.dp)
                .aspectRatio(980f / 1023f)
                .scale(scale)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Groceries delivered fast",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.alpha(glow + 0.4f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Teal,
            strokeWidth = 2.5.dp
        )
    }
}
