package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.*
import com.duggustore.app.ui.theme.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    successMessage: String? = null,
    onRegister: (String, String, String, String, String) -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("customer") }
    var localError by remember { mutableStateOf<String?>(null) }

    val roles = listOf(
        "customer" to "Shop",
        "seller" to "Sell",
        "delivery" to "Deliver"
    )

    val shownError = localError ?: error

    AuthScaffold(
        title = "Create account",
        subtitle = "Groceries at your door in minutes"
    ) {
        if (shownError != null) {
            AuthErrorBanner(
                message = shownError,
                onDismiss = { localError = null; onClearError() }
            )
            Spacer(Modifier.height(18.dp))
        }

        // The view model reports things like "check your inbox" here. The
        // screen used to accept this and never render it.
        if (successMessage != null) {
            AuthInfoBanner(message = successMessage)
            Spacer(Modifier.height(18.dp))
        }

        AuthField(
            value = fullName,
            onValueChange = { fullName = it; localError = null; onClearError() },
            label = "Full name",
            placeholder = "Your name",
            leadingIcon = Icons.Default.Person
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = email,
            onValueChange = { email = it; localError = null; onClearError() },
            label = "Email",
            placeholder = "you@example.com",
            leadingIcon = Icons.Default.Email,
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = phone,
            onValueChange = { phone = it; localError = null; onClearError() },
            label = "Phone number",
            placeholder = "10-digit number",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = "I want to",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            roles.forEach { (value, label) ->
                RoleChip(
                    label = label,
                    selected = selectedRole == value,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = value }
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        AuthField(
            value = password,
            onValueChange = { password = it; localError = null; onClearError() },
            label = "Password",
            placeholder = "At least 6 characters",
            leadingIcon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; localError = null },
            label = "Confirm password",
            placeholder = "Repeat the password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true
        )

        Spacer(Modifier.height(24.dp))

        AuthPrimaryButton(
            text = "Create account",
            onClick = {
                localError = null
                onClearSuccess()
                when {
                    fullName.isBlank() -> localError = "Please enter your name"
                    email.isBlank() -> localError = "Please enter your email"
                    phone.isBlank() -> localError = "Please enter your phone number"
                    password.length < 6 -> localError = "Password must be at least 6 characters"
                    password != confirmPassword -> localError = "Passwords don't match"
                    else -> onRegister(email, password, fullName, phone, selectedRole)
                }
            },
            isLoading = isLoading
        )

        Spacer(Modifier.height(20.dp))

        AuthSwitchRow(
            question = "Already have an account?",
            action = "Sign in",
            onClick = onNavigateToLogin
        )
    }
}

/** Replaces the role dropdown: all three choices are visible at once. */
@Composable
private fun RoleChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Teal else SurfaceMuted)
            .border(
                width = 1.dp,
                color = if (selected) Teal else BorderGray,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
