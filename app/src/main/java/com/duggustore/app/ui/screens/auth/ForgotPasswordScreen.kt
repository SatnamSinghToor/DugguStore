package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.*
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

    AuthScaffold(
        title = if (resetSent) "Check your email" else "Reset password",
        subtitle = if (resetSent) "The link is on its way"
                   else "We'll email you a link to set a new one"
    ) {
        if (resetSent) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(TealSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MarkEmailRead,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "If an account exists for that address, we've sent it a link " +
                       "to set a new password. Open it on this phone and the app will " +
                       "take you to the next step.",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
        } else {
            if (error != null) {
                AuthErrorBanner(message = error, onDismiss = onClearError)
                Spacer(Modifier.height(18.dp))
            }

            AuthField(
                value = email,
                onValueChange = { email = it; onClearError() },
                label = "Email",
                placeholder = "you@example.com",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(24.dp))

            AuthPrimaryButton(
                text = "Send reset link",
                onClick = { onClearError(); onSendReset(email) },
                isLoading = isLoading,
                enabled = email.isNotBlank()
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Back to sign in",
            modifier = Modifier
                .fillMaxWidth()
                .clickableText { onClearError(); onBackToLogin() },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Teal,
            textAlign = TextAlign.Center
        )
    }
}
