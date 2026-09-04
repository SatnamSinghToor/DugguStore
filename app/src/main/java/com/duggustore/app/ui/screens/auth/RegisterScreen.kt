package com.duggustore.app.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.*
import com.duggustore.app.R
import com.duggustore.app.ui.theme.*

private const val STEP_NAME = 0
private const val STEP_EMAIL = 1
private const val STEP_PHONE = 2
private const val STEP_ROLE = 3
private const val STEP_PASSWORD = 4
private const val STEP_REFERRAL = 5
private const val STEP_COUNT = 6

/**
 * One question per screen with a slim progress bar and Back/Next, the way
 * Facebook's own sign-up guides you through — rather than one long form
 * that dumps every field on the person at once.
 */
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    successMessage: String? = null,
    onRegister: (String, String, String, String, String, String) -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(STEP_NAME) }
    var movingForward by remember { mutableStateOf(true) }

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var referralCode by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf("customer") }
    var localError by remember { mutableStateOf<String?>(null) }

    val roles = listOf(
        Triple("customer", stringResource(R.string.auth_role_shop), Icons.Default.ShoppingBag),
        Triple("seller", stringResource(R.string.auth_role_sell), Icons.Default.Storefront),
        Triple("delivery", stringResource(R.string.auth_role_deliver), Icons.Default.LocalShipping)
    )

    val shownError = localError ?: error

    // Resolved here rather than inside goNext(): that function is not a
    // composable scope and cannot call stringResource.
    val errNoName = stringResource(R.string.auth_err_name)
    val errNoEmail = stringResource(R.string.auth_err_email)
    val errNoPhone = stringResource(R.string.auth_err_phone)
    val errShortPassword = stringResource(R.string.auth_err_password_short)
    val errPasswordMatch = stringResource(R.string.auth_err_password_match)

    fun goNext() {
        localError = when (step) {
            STEP_NAME -> if (fullName.isBlank()) errNoName else null
            STEP_EMAIL -> if (email.isBlank() || !email.contains("@")) errNoEmail else null
            STEP_PHONE -> if (phone.isBlank()) errNoPhone else null
            STEP_PASSWORD -> when {
                password.length < 6 -> errShortPassword
                password != confirmPassword -> errPasswordMatch
                else -> null
            }
            else -> null
        }
        if (localError != null) return
        onClearError()

        if (step == STEP_REFERRAL) {
            onClearSuccess()
            onRegister(email, password, fullName, phone, selectedRole, referralCode)
        } else {
            movingForward = true
            step++
        }
    }

    fun goBack() {
        localError = null
        onClearError()
        movingForward = false
        step--
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > STEP_NAME) {
                IconButton(onClick = ::goBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
            Spacer(Modifier.width(4.dp))
            StepProgressBar(current = step, total = STEP_COUNT, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(52.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            if (step == STEP_NAME) {
                Spacer(Modifier.height(4.dp))
                AppLogo(size = 84)
                Spacer(Modifier.height(20.dp))
            } else {
                Spacer(Modifier.height(20.dp))
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (movingForward) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "signup-step"
            ) { s ->
                Column {
                    when (s) {
                        STEP_NAME -> StepName(fullName) { fullName = it; localError = null; onClearError() }
                        STEP_EMAIL -> StepEmail(email) { email = it; localError = null; onClearError() }
                        STEP_PHONE -> StepPhone(phone) { phone = it; localError = null; onClearError() }
                        STEP_ROLE -> StepRole(roles, selectedRole) { selectedRole = it }
                        STEP_PASSWORD -> StepPassword(
                            password = password,
                            confirmPassword = confirmPassword,
                            onPasswordChange = { password = it; localError = null; onClearError() },
                            onConfirmChange = { confirmPassword = it; localError = null }
                        )
                        else -> StepReferral(referralCode) { referralCode = it; localError = null; onClearError() }
                    }
                }
            }

            if (step == STEP_NAME) {
                Spacer(Modifier.height(24.dp))
                AuthTabSwitcher(
                    selected = AuthTab.SIGN_UP,
                    onSelect = { tab -> if (tab == AuthTab.LOG_IN) onNavigateToLogin() }
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            if (shownError != null) {
                AuthErrorBanner(message = shownError, onDismiss = { localError = null; onClearError() })
                Spacer(Modifier.height(12.dp))
            }
            if (successMessage != null) {
                AuthInfoBanner(message = successMessage)
                Spacer(Modifier.height(12.dp))
            }
            AuthPrimaryButton(
                text = if (step == STEP_REFERRAL) stringResource(R.string.auth_create_title) else "Next",
                onClick = ::goNext,
                isLoading = isLoading && step == STEP_REFERRAL
            )
        }
    }
}

@Composable
private fun StepProgressBar(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index <= current) Teal else BorderGray)
            )
        }
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String) {
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, fontSize = 14.sp, color = TextSecondary)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun StepName(value: String, onValueChange: (String) -> Unit) {
    StepHeading("What's your name?", "This is how you'll appear on Duggu Store.")
    AuthField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.auth_full_name),
        placeholder = stringResource(R.string.auth_full_name_hint),
        leadingIcon = Icons.Default.Person,
        leadingIconTint = Teal
    )
}

@Composable
private fun StepEmail(value: String, onValueChange: (String) -> Unit) {
    StepHeading("What's your email?", "We'll use this to sign you in.")
    AuthField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.auth_email),
        placeholder = stringResource(R.string.auth_email_hint),
        leadingIcon = Icons.Default.Email,
        leadingIconTint = Orange,
        keyboardType = KeyboardType.Email
    )
}

@Composable
private fun StepPhone(value: String, onValueChange: (String) -> Unit) {
    StepHeading("Your phone number?", "So a rider or seller can reach you about an order.")
    AuthField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.auth_phone),
        placeholder = stringResource(R.string.auth_phone_hint),
        leadingIcon = Icons.Default.Phone,
        leadingIconTint = Coral,
        keyboardType = KeyboardType.Phone
    )
}

@Composable
private fun StepRole(
    roles: List<Triple<String, String, ImageVector>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    StepHeading("What brings you here?", "You can change this later from your account.")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        roles.forEach { (value, label, icon) ->
            RoleCard(icon = icon, label = label, selected = value == selected, onClick = { onSelect(value) })
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) TealSurface else SurfaceWhite,
        border = BorderStroke(1.5.dp, if (selected) Teal else BorderGray)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background((if (selected) Teal else TextLight).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (selected) Teal else TextSecondary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Teal))
        }
    }
}

@Composable
private fun StepPassword(
    password: String,
    confirmPassword: String,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit
) {
    StepHeading("Create a password", "At least 6 characters — keep it somewhere safe.")
    AuthField(
        value = password,
        onValueChange = onPasswordChange,
        label = stringResource(R.string.auth_password),
        placeholder = stringResource(R.string.auth_password_min_hint),
        leadingIcon = Icons.Default.Lock,
        leadingIconTint = Teal,
        isPassword = true
    )
    Spacer(Modifier.height(16.dp))
    AuthField(
        value = confirmPassword,
        onValueChange = onConfirmChange,
        label = stringResource(R.string.auth_confirm_password),
        placeholder = stringResource(R.string.auth_confirm_password_hint),
        leadingIcon = Icons.Default.Lock,
        leadingIconTint = Orange,
        isPassword = true
    )
}

@Composable
private fun StepReferral(value: String, onValueChange: (String) -> Unit) {
    StepHeading("Got a referral code?", "Optional — enter one if a friend shared it with you, or just create your account.")
    AuthField(
        value = value,
        onValueChange = onValueChange,
        label = "Referral code (optional)",
        placeholder = "Got a code from a friend?",
        leadingIcon = Icons.Default.CardGiftcard,
        leadingIconTint = Violet
    )
}
