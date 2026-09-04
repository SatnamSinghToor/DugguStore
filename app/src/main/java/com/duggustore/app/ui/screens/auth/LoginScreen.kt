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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.*
import com.duggustore.app.R
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
        title = stringResource(R.string.auth_welcome_back),
        subtitle = stringResource(R.string.auth_welcome_back_sub),
        footer = {
            AuthSwitchRow(
                question = stringResource(R.string.auth_new_here),
                action = stringResource(R.string.auth_create_account),
                onClick = onNavigateToRegister
            )
        }
    ) {
        AuthTabSwitcher(
            selected = AuthTab.LOG_IN,
            onSelect = { tab -> if (tab == AuthTab.SIGN_UP) onNavigateToRegister() }
        )
        Spacer(Modifier.height(18.dp))

        if (error != null) {
            AuthErrorBanner(message = error, onDismiss = onClearError)
            Spacer(Modifier.height(18.dp))
        }

        AuthField(
            value = email,
            onValueChange = { email = it; onClearError() },
            label = stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_hint),
            leadingIcon = Icons.Default.Email,
            leadingIconTint = Orange,
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = password,
            onValueChange = { password = it; onClearError() },
            label = stringResource(R.string.auth_password),
            placeholder = stringResource(R.string.auth_password_hint),
            leadingIcon = Icons.Default.Lock,
            leadingIconTint = Violet,
            isPassword = true
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.auth_forgot),
            modifier = Modifier
                .align(Alignment.End)
                .clickable { onForgotPassword() },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Teal
        )

        Spacer(Modifier.height(24.dp))

        AuthPrimaryButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = { onLogin(email, password) },
            isLoading = isLoading,
            // Nothing to send until both fields carry something.
            enabled = email.isNotBlank() && password.isNotBlank()
        )
    }
}
