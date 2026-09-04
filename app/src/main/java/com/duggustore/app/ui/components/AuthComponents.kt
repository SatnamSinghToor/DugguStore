package com.duggustore.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.R
import com.duggustore.app.ui.theme.*

/**
 * Shared frame for the auth screens: logo, centred title, then the form
 * sitting directly on the page background — no card behind the fields,
 * which just doubled up on the outlined fields' own borders.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    footer: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppLogo()
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(22.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )

        Spacer(Modifier.height(20.dp))
        footer()
    }
}

/**
 * The app's mark — the Duggu Store logo, matching the launcher icon. Sign in
 * and sign up were the two screens with no branding on them at all once the
 * old teal header band was removed.
 */
@Composable
fun AppLogo(size: Int = 140) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = "Duggu Store",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(size.dp)
            .aspectRatio(985f / 1034f)
    )
}

enum class AuthTab { LOG_IN, SIGN_UP }

/**
 * Pill switcher at the top of the login/signup forms — tapping the side
 * you're not on navigates there. The active side's fill colour follows
 * which tab it is (orange for Log In, teal for Sign Up) rather than a
 * single accent, matching the two screens' own button colours.
 */
@Composable
fun AuthTabSwitcher(selected: AuthTab, onSelect: (AuthTab) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = SurfaceMuted
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            AuthTabSegment(
                text = "Log In",
                selected = selected == AuthTab.LOG_IN,
                activeColor = Orange,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(AuthTab.LOG_IN) }
            )
            AuthTabSegment(
                text = "Sign Up",
                selected = selected == AuthTab.SIGN_UP,
                activeColor = Teal,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(AuthTab.SIGN_UP) }
            )
        }
    }
}

@Composable
private fun AuthTabSegment(
    text: String,
    selected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = if (selected) activeColor else Color.Transparent
    ) {
        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else TextSecondary
            )
        }
    }
}

/**
 * Outlined field with a floating label, matching the compact pre-redesign
 * look the filled version replaced — the filled fields with a label stacked
 * above them took noticeably more vertical space per field for no real gain.
 * The leading icon takes its own tint rather than a fixed one: a whole form
 * of teal icons on a teal-bordered field reads as one flat colour rather
 * than as distinct fields.
 *
 * A password field carries its own show/hide toggle, and [helper] puts the
 * reason a value is wrong directly under the field it belongs to rather than
 * only in a banner at the top of the form.
 */
@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Teal,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    helper: String? = null,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {}
) {
    var revealed by remember { mutableStateOf(false) }
    val hideCharacters = isPassword && !revealed

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label, fontSize = 13.sp) },
            placeholder = if (placeholder.isBlank()) null else {
                { Text(placeholder, color = TextLight, fontSize = 14.sp) }
            },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = leadingIconTint, modifier = Modifier.size(20.dp)) }
            },
            trailingIcon = if (!isPassword) null else {
                {
                    Icon(
                        imageVector = if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (revealed) "Hide password" else "Show password",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { revealed = !revealed }
                    )
                }
            },
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
                onGo = { onImeAction() }
            ),
            visualTransformation =
                if (hideCharacters) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                focusedBorderColor = Teal,
                unfocusedBorderColor = BorderGray,
                errorBorderColor = Coral,
                focusedLabelColor = Teal,
                unfocusedLabelColor = TextSecondary,
                cursorColor = Teal
            )
        )
        if (helper != null) {
            Text(
                text = helper,
                modifier = Modifier.padding(start = 14.dp, top = 5.dp),
                fontSize = 12.sp,
                color = if (isError) CoralDark else TextSecondary
            )
        }
    }
}

/** A requirement the person can watch turn green as they type, rather than a rule they only meet by trial and error. */
@Composable
fun AuthRequirementRow(text: String, met: Boolean) {
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (met) SuccessGreen else TextLight,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (met) SuccessGreen else TextSecondary
        )
    }
}

/** Full-width teal action button used at the bottom of each auth form. */
@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Teal,
            disabledContainerColor = BorderGray
        ),
        enabled = enabled && !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/** Coral banner for a failed attempt, dismissable so it does not sit forever. */
@Composable
fun AuthErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CoralSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = CoralDark,
                fontSize = 13.sp
            )
            Icon(
                Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = CoralDark,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable { onDismiss() }
            )
        }
    }
}

/** Teal banner for a message worth reading that is not a failure. */
@Composable
fun AuthInfoBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = TealSurface
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            color = TealDark,
            fontSize = 13.sp
        )
    }
}

/** Footer line pairing a question with the link to the other auth screen. */
@Composable
fun AuthSwitchRow(question: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(question, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.width(4.dp))
        Text(
            text = action,
            modifier = Modifier.clickable { onClick() },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Teal
        )
    }
}

/**
 * Lets a plain Text act as a link. TextButton would work but brings its own
 * min-height and padding, which throws off the spacing in these forms.
 */
fun Modifier.clickableText(onClick: () -> Unit): Modifier = this.clickable { onClick() }
