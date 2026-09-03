package com.duggustore.app.ui.screens.auth
import androidx.compose.ui.draw.scale

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.theme.*

private const val CODE_LENGTH = 6

@Composable
fun VerifyEmailScreen(
    email: String,
    onVerifyCode: (String) -> Unit,
    onResendEmail: () -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    verificationResent: Boolean = false,
    onClearError: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val infiniteTransition2 = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition2.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Animated envelope icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(pulseScale)
                .offset(y = floatOffset.dp)
                .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = "Email",
                modifier = Modifier.size(56.dp),
                tint = PrimaryGreen
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verify Your Email",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We've sent a 6-digit code to",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = email,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryGreen,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the code from your email to activate your account.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { input ->
                // Digits only, capped at the code length, so the field cannot hold
                // something the verify call would reject anyway.
                val digits = input.filter { it.isDigit() }.take(CODE_LENGTH)
                if (digits != code) {
                    code = digits
                    onClearError()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            label = { Text("6-digit code") },
            placeholder = { Text("000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = BorderGray,
                cursorColor = PrimaryGreen
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        DugguButton(
            text = "Verify",
            onClick = {
                onClearError()
                onVerifyCode(code)
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading,
            enabled = !isLoading && code.length == CODE_LENGTH
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (verificationResent) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = DeliveredGreen.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "New code sent! Check your inbox.",
                    modifier = Modifier.padding(12.dp),
                    color = DeliveredGreen,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        error?.let { err ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = AccentRed.copy(alpha = 0.1f)
            ) {
                Text(
                    text = err,
                    modifier = Modifier.padding(12.dp),
                    color = AccentRed,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Resend button
        OutlinedButton(
            onClick = {
                onClearError()
                onResendEmail()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryGreen
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Resend Code",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Back to login
        TextButton(
            onClick = {
                onClearError()
                onBackToLogin()
            }
        ) {
            Text(
                text = "Back to Sign In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
