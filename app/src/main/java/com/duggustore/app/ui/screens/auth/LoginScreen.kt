package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit = {},
    isLoading: Boolean = false,
    error: String? = null,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthScaffold(
        title = "Welcome back",
        subtitle = "Sign in to keep shopping"
    ) {
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

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = password,
            onValueChange = { password = it; onClearError() },
            label = "Password",
            placeholder = "Your password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Forgot password?",
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onForgotPassword() },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Teal
        )

        Spacer(Modifier.height(24.dp))

        AuthPrimaryButton(
            text = "Sign in",
            onClick = { onLogin(email, password) },
            isLoading = isLoading,
            // Nothing to send until both fields carry something.
            enabled = email.isNotBlank() && password.isNotBlank()
        )

        Spacer(Modifier.height(20.dp))

        AuthSwitchRow(
            question = "New to Duggu Store?",
            action = "Create an account",
            onClick = onNavigateToRegister
        )
    }
}
