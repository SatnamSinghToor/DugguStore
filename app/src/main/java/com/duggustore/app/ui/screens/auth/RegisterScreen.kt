package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    successMessage: String? = null,
    onRegister: (String, String, String, String, String, String) -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("customer") }
    var localError by remember { mutableStateOf<String?>(null) }

    val roles = listOf(
        "customer" to stringResource(R.string.auth_role_shop),
        "seller" to stringResource(R.string.auth_role_sell),
        "delivery" to stringResource(R.string.auth_role_deliver)
    )

    val shownError = localError ?: error

    // Resolved here rather than in the click handler: that lambda is not a
    // composable scope and cannot call stringResource.
    val errNoName = stringResource(R.string.auth_err_name)
    val errNoEmail = stringResource(R.string.auth_err_email)
    val errNoPhone = stringResource(R.string.auth_err_phone)
    val errShortPassword = stringResource(R.string.auth_err_password_short)
    val errPasswordMatch = stringResource(R.string.auth_err_password_match)

    AuthScaffold(
        title = stringResource(R.string.auth_create_title),
        subtitle = stringResource(R.string.auth_create_sub),
        footer = {
            AuthSwitchRow(
                question = stringResource(R.string.auth_have_account),
                action = stringResource(R.string.auth_sign_in),
                onClick = onNavigateToLogin
            )
        }
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
            label = stringResource(R.string.auth_full_name),
            placeholder = stringResource(R.string.auth_full_name_hint),
            leadingIcon = Icons.Default.Person,
            leadingIconTint = Teal
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = email,
            onValueChange = { email = it; localError = null; onClearError() },
            label = stringResource(R.string.auth_email),
            placeholder = stringResource(R.string.auth_email_hint),
            leadingIcon = Icons.Default.Email,
            leadingIconTint = Teal,
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = phone,
            onValueChange = { phone = it; localError = null; onClearError() },
            label = stringResource(R.string.auth_phone),
            placeholder = stringResource(R.string.auth_phone_hint),
            leadingIcon = Icons.Default.Phone,
            leadingIconTint = Teal,
            keyboardType = KeyboardType.Phone
        )

        Spacer(Modifier.height(16.dp))

        RoleDropdownField(
            label = stringResource(R.string.auth_i_want_to),
            roles = roles,
            selected = selectedRole,
            onSelect = { selectedRole = it }
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = password,
            onValueChange = { password = it; localError = null; onClearError() },
            label = stringResource(R.string.auth_password),
            placeholder = stringResource(R.string.auth_password_min_hint),
            leadingIcon = Icons.Default.Lock,
            leadingIconTint = Teal,
            isPassword = true
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; localError = null },
            label = stringResource(R.string.auth_confirm_password),
            placeholder = stringResource(R.string.auth_confirm_password_hint),
            leadingIcon = Icons.Default.Lock,
            leadingIconTint = Teal,
            isPassword = true
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = referralCode,
            onValueChange = { referralCode = it; localError = null; onClearError() },
            label = "Referral code (optional)",
            placeholder = "Got a code from a friend?",
            leadingIcon = Icons.Default.CardGiftcard,
            leadingIconTint = Teal
        )

        Spacer(Modifier.height(24.dp))

        AuthPrimaryButton(
            text = stringResource(R.string.auth_create_title),
            onClick = {
                localError = null
                onClearSuccess()
                when {
                    fullName.isBlank() -> localError = errNoName
                    email.isBlank() -> localError = errNoEmail
                    phone.isBlank() -> localError = errNoPhone
                    password.length < 6 -> localError = errShortPassword
                    password != confirmPassword -> localError = errPasswordMatch
                    else -> onRegister(email, password, fullName, phone, selectedRole, referralCode)
                }
            },
            isLoading = isLoading
        )
    }
}

/**
 * Back to a dropdown rather than the three-way chip row it was briefly
 * replaced with — styled to match the outlined fields around it (border,
 * leading icon, floating label) instead of a plain unlabeled box.
 */
@Composable
private fun RoleDropdownField(
    label: String,
    roles: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = roles.firstOrNull { it.first == selected }?.second ?: ""

    Box {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            enabled = false,
            label = { Text(label, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Category, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Teal)
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = SurfaceWhite,
                disabledBorderColor = BorderGray,
                disabledLabelColor = TextSecondary,
                disabledTextColor = TextPrimary,
                disabledLeadingIconColor = Teal,
                disabledTrailingIconColor = TextSecondary
            )
        )

        // A transparent click target the full size of the field: the field
        // itself is disabled (so it can't be typed into) which would also
        // swallow the click if this weren't layered on top separately.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            roles.forEach { (value, roleLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = roleLabel,
                            fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (value == selected) Teal else TextPrimary
                        )
                    },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}
