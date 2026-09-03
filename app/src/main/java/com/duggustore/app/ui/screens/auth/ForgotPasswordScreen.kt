package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.components.DugguTextField
import com.duggustore.app.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    onSendReset: (String) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    resetSent: Boolean = false,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }

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
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (resetSent) Icons.Default.MarkEmailRead else Icons.Default.LockReset,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = PrimaryGreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (resetSent) "Check Your Email" else "Reset Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (resetSent) {
                "If an account exists for that address, we've sent it a link to set a new password."
            } else {
                "Enter your email and we'll send you a link to set a new password."
            },
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!resetSent) {
            DugguTextField(
                value = email,
                onValueChange = { email = it; onClearError() },
                label = "Email",
                keyboardType = KeyboardType.Email
            )

            error?.let { err ->
                Spacer(modifier = Modifier.height(12.dp))
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
            }

            Spacer(modifier = Modifier.height(20.dp))

            DugguButton(
                text = "Send Reset Link",
                onClick = {
                    onClearError()
                    onSendReset(email)
                },
                modifier = Modifier.fillMaxWidth(),
                isLoading = isLoading,
                enabled = !isLoading && email.isNotBlank()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { onClearError(); onBackToLogin() }) {
            Text(
                text = "Back to Sign In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}
