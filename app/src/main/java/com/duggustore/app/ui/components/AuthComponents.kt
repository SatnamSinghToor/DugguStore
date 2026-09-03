package com.duggustore.app.ui.components

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
 * Shared frame for the auth screens: a teal band carrying the wordmark and the
 * screen's title, with the form in a white sheet whose rounded top overlaps it.
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Teal)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp, bottom = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🛒", fontSize = 28.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-26).dp),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = SurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp, bottom = 28.dp),
                content = content
            )
        }
    }
}

/**
 * Filled field with the label above it, rather than the floating label the
 * pre-redesign screens used, so the forms read like the reference.
 */
@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder.isBlank()) null else {
                { Text(placeholder, color = TextLight, fontSize = 14.sp) }
            },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp)) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation =
                if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceMuted,
                unfocusedContainerColor = SurfaceMuted,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = Teal
            )
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
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
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
