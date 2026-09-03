package com.duggustore.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.theme.*

/**
 * Shared frame for the auth screens: a centred title over a bordered card
 * holding the form, rather than the earlier full-width teal band — that band
 * pushed the whole form down a screen's worth before any field was visible.
 * This stays compact and leans on the app's own palette (teal border/accent
 * on a plain white ground) instead of a flat colour block.
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
            .appPatternOverlay()
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

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceMuted,
            border = BorderStroke(1.dp, BorderGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .padding(top = 20.dp, bottom = 20.dp),
                content = content
            )
        }

        Spacer(Modifier.height(20.dp))
        footer()
    }
}

/**
 * The app's mark: a rounded orange tile with a bold white "D", matching the
 * launcher icon. Sign in and sign up were the two screens with no branding
 * on them at all once the old teal header band was removed.
 */
@Composable
fun AppLogo(size: Int = 64) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size * 0.28).dp))
            .background(Orange),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "D",
            color = Color.White,
            fontSize = (size * 0.52).sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/**
 * Outlined field with a floating label, matching the compact pre-redesign
 * look the filled version replaced — the filled fields with a label stacked
 * above them took noticeably more vertical space per field for no real gain.
 * The leading icon takes its own tint rather than a fixed one: a whole form
 * of teal icons on a teal-bordered field reads as one flat colour rather
 * than as distinct fields.
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
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 13.sp) },
        placeholder = if (placeholder.isBlank()) null else {
            { Text(placeholder, color = TextLight, fontSize = 14.sp) }
        },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = leadingIconTint, modifier = Modifier.size(20.dp)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation =
            if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedBorderColor = Teal,
            unfocusedBorderColor = BorderGray,
            focusedLabelColor = Teal,
            unfocusedLabelColor = TextSecondary,
            cursorColor = Teal
        )
    )
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
