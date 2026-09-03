package com.duggustore.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.components.DugguTextField
import com.duggustore.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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
    var expandedRoleMenu by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val roles = listOf(
        "customer" to "Customer",
        "seller" to "Seller",
        "delivery" to "Delivery Partner"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Join Duggu Store today",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                (error ?: localError)?.let { err ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = AccentRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = err,
                            modifier = Modifier.padding(12.dp),
                            color = AccentRed,
                            fontSize = 13.sp
                        )
                    }
                }

                DugguTextField(
                    value = fullName,
                    onValueChange = { fullName = it; localError = null; onClearError() },
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person
                )

                DugguTextField(
                    value = email,
                    onValueChange = { email = it; localError = null; onClearError() },
                    label = "Email",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                )

                DugguTextField(
                    value = phone,
                    onValueChange = { phone = it; localError = null; onClearError() },
                    label = "Phone Number",
                    leadingIcon = Icons.Default.Phone,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                )

                ExposedDropdownMenuBox(
                    expanded = expandedRoleMenu,
                    onExpandedChange = { expandedRoleMenu = it }
                ) {
                    OutlinedTextField(
                        value = roles.find { it.first == selectedRole }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("I want to join as") },
                        leadingIcon = { Icon(Icons.Default.Badge, "Role", tint = TextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = BorderGray
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRoleMenu,
                        onDismissRequest = { expandedRoleMenu = false }
                    ) {
                        roles.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedRole = value
                                    expandedRoleMenu = false
                                }
                            )
                        }
                    }
                }

                DugguTextField(
                    value = password,
                    onValueChange = { password = it; localError = null; onClearError() },
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )

                DugguTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm Password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )

                DugguButton(
                    text = "Create Account",
                    onClick = {
                        localError = null
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Already have an account? ", fontSize = 14.sp, color = TextSecondary)
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Sign In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }
    }
}
