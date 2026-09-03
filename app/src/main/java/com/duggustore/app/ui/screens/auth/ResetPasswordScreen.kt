package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun ResetPasswordScreen(
    onSubmit: (newPassword: String, confirmPassword: String) -> Unit,
    onCancel: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    onClearError: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AuthScaffold(
        title = "New password",
        subtitle = "You'll be signed in straight after"
    ) {
        if (error != null) {
            AuthErrorBanner(message = error, onDismiss = onClearError)
            Spacer(Modifier.height(18.dp))
        }

        AuthField(
            value = password,
            onValueChange = { password = it; onClearError() },
            label = "New password",
            placeholder = "At least 6 characters",
            leadingIcon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = confirm,
            onValueChange = { confirm = it; onClearError() },
            label = "Confirm new password",
            placeholder = "Repeat the password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(Modifier.height(24.dp))

        AuthPrimaryButton(
            text = "Save password",
            onClick = { onClearError(); onSubmit(password, confirm) },
            isLoading = isLoading,
            enabled = password.isNotBlank() && confirm.isNotBlank()
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Cancel",
            modifier = Modifier
                .fillMaxWidth()
                .clickableText { onClearError(); onCancel() },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
