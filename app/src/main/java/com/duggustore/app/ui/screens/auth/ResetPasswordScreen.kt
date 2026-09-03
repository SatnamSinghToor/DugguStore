package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.components.DugguTextField
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = PrimaryGreen
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Set a New Password",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Choose a new password for your account. You'll be signed in straight after.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        DugguTextField(
            value = password,
            onValueChange = { password = it; onClearError() },
            label = "New password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        DugguTextField(
            value = confirm,
            onValueChange = { confirm = it; onClearError() },
            label = "Confirm new password",
            isPassword = true
        )

        error?.let { err ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = AccentRed.copy(alpha = 0.1f)
            ) {
                Text(err, modifier = Modifier.padding(12.dp), color = AccentRed, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        DugguButton(
            text = "Save Password",
            onClick = {
                onClearError()
                onSubmit(password, confirm)
            },
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading,
            enabled = !isLoading && password.isNotBlank() && confirm.isNotBlank()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { onClearError(); onCancel() }) {
            Text(
                text = "Cancel",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }
    }
}
