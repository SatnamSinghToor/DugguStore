package com.duggustore.app.ui.screens.delivery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.DELIVERY_DOC_TYPES
import com.duggustore.app.data.model.DeliveryPartner
import com.duggustore.app.data.model.DeliveryPartnerDocument
import com.duggustore.app.data.model.VEHICLE_TYPES
import com.duggustore.app.data.model.docTypeLabel
import com.duggustore.app.ui.components.DocumentUploadRow
import com.duggustore.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A rider's application — licence, ID and vehicle details plus the documents backing them, same shape as the seller one. */
@Composable
fun DeliveryOnboardingScreen(
    userId: String,
    prefillEmail: String,
    prefillPhone: String,
    existing: DeliveryPartner?,
    documents: List<DeliveryPartnerDocument>,
    isSaving: Boolean,
    isSubmitting: Boolean,
    uploadingDocType: String?,
    error: String?,
    onUploadDocument: (docType: String, bytes: ByteArray, contentType: String) -> Unit,
    onSave: (DeliveryPartner) -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember(existing) { mutableStateOf(existing?.fullName.orEmpty()) }
    var phone by remember(existing) { mutableStateOf(existing?.phone ?: prefillPhone) }
    var licence by remember(existing) { mutableStateOf(existing?.licenceNumber.orEmpty()) }
    var aadhaar by remember(existing) { mutableStateOf(existing?.aadhaarNumber.orEmpty()) }
    var pan by remember(existing) { mutableStateOf(existing?.panNumber.orEmpty()) }
    var vehicleType by remember(existing) { mutableStateOf(existing?.vehicleType ?: VEHICLE_TYPES.first()) }
    var vehicleNumber by remember(existing) { mutableStateOf(existing?.vehicleNumber.orEmpty()) }
    var bankAccount by remember(existing) { mutableStateOf(existing?.bankAccountNumber.orEmpty()) }
    var bankIfsc by remember(existing) { mutableStateOf(existing?.bankIfsc.orEmpty()) }
    var upiId by remember(existing) { mutableStateOf(existing?.upiId.orEmpty()) }
    var city by remember(existing) { mutableStateOf(existing?.city.orEmpty()) }
    var address by remember(existing) { mutableStateOf(existing?.address.orEmpty()) }
    var emergencyName by remember(existing) { mutableStateOf(existing?.emergencyContactName.orEmpty()) }
    var emergencyPhone by remember(existing) { mutableStateOf(existing?.emergencyContactPhone.orEmpty()) }
    var localError by remember { mutableStateOf<String?>(null) }
    var pendingDocType by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val docType = pendingDocType
        pendingDocType = null
        if (uri == null || docType == null) return@rememberLauncherForActivityResult
        scope.launch {
            val read = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val mimeType = resolver.getType(uri) ?: "image/jpeg"
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Could not read the selected file")
                    bytes to mimeType
                }
            }.getOrNull()
            if (read != null) onUploadDocument(docType, read.first, read.second)
        }
    }

    val documentsByType = remember(documents) { documents.associateBy { it.docType } }
    val allDocsUploaded = remember(documents) { DELIVERY_DOC_TYPES.all { documentsByType.containsKey(it) } }
    val requiredFilled = fullName.isNotBlank() && phone.isNotBlank() && licence.isNotBlank() &&
        aadhaar.isNotBlank() && vehicleNumber.isNotBlank() && bankAccount.isNotBlank() &&
        bankIfsc.isNotBlank() && address.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Surface(color = Teal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Become a delivery partner", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Fill this in once — an admin reviews it before you can go online",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onSignOut) {
                    Icon(Icons.Default.Logout, "Sign out", tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (existing?.rejectionReason?.isNotBlank() == true) {
                OnboardingErrorBanner("Your last application was rejected: ${existing.rejectionReason}")
                Spacer(Modifier.height(16.dp))
            }
            val shownError = localError ?: error
            if (shownError != null) {
                OnboardingErrorBanner(shownError, onDismiss = { localError = null; onClearError() })
                Spacer(Modifier.height(16.dp))
            }

            SectionLabel("Step 1 · Your details")
            Spacer(Modifier.height(10.dp))
            OnboardingField(fullName, { fullName = it }, "Full name")
            Spacer(Modifier.height(10.dp))
            OnboardingField(phone, { phone = it }, "Phone number", keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(10.dp))
            OnboardingField(city, { city = it }, "City")
            Spacer(Modifier.height(10.dp))
            OnboardingField(address, { address = it }, "Home address")
            Spacer(Modifier.height(10.dp))
            OnboardingField(emergencyName, { emergencyName = it }, "Emergency contact name (optional)")
            Spacer(Modifier.height(10.dp))
            OnboardingField(emergencyPhone, { emergencyPhone = it }, "Emergency contact phone (optional)", keyboardType = KeyboardType.Phone)

            Spacer(Modifier.height(22.dp))
            SectionLabel("Step 2 · ID, vehicle & bank details")
            Spacer(Modifier.height(10.dp))
            OnboardingField(licence, { licence = it.uppercase() }, "Driving licence number")
            Spacer(Modifier.height(10.dp))
            OnboardingField(aadhaar, { aadhaar = it }, "Aadhaar number", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            OnboardingField(pan, { pan = it.uppercase() }, "PAN number (optional)")
            Spacer(Modifier.height(10.dp))
            Text("Vehicle type", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VEHICLE_TYPES.forEach { type ->
                    VehicleTypeChip(
                        label = type.lowercase().replaceFirstChar { it.uppercase() },
                        selected = vehicleType == type,
                        onClick = { vehicleType = type }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            OnboardingField(vehicleNumber, { vehicleNumber = it.uppercase() }, "Vehicle number")
            Spacer(Modifier.height(10.dp))
            OnboardingField(bankAccount, { bankAccount = it }, "Bank account number", keyboardType = KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            OnboardingField(bankIfsc, { bankIfsc = it.uppercase() }, "Bank IFSC code")
            Spacer(Modifier.height(10.dp))
            OnboardingField(upiId, { upiId = it }, "UPI ID (optional)")

            Spacer(Modifier.height(22.dp))
            SectionLabel("Step 3 · Documents")
            Spacer(Modifier.height(6.dp))
            Text(
                "A clear photo of each is fine — save first, then upload.",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))

            if (existing == null) {
                Text(
                    "Save your details first to unlock document uploads.",
                    fontSize = 13.sp,
                    color = TextLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                DELIVERY_DOC_TYPES.forEach { docType ->
                    DocumentUploadRow(
                        label = docTypeLabel(docType),
                        docStatus = documentsByType[docType]?.status,
                        isUploading = uploadingDocType == docType,
                        onPick = {
                            pendingDocType = docType
                            pickDocument.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!requiredFilled) {
                        localError = "Please fill in every required field"
                        return@Button
                    }
                    onSave(
                        DeliveryPartner(
                            id = userId,
                            fullName = fullName.trim(),
                            email = prefillEmail,
                            phone = phone.trim(),
                            licenceNumber = licence.trim(),
                            aadhaarNumber = aadhaar.trim(),
                            panNumber = pan.trim().ifBlank { null },
                            vehicleType = vehicleType,
                            vehicleNumber = vehicleNumber.trim(),
                            bankAccountNumber = bankAccount.trim(),
                            bankIfsc = bankIfsc.trim(),
                            upiId = upiId.trim().ifBlank { null },
                            city = city.trim().ifBlank { null },
                            address = address.trim(),
                            emergencyContactName = emergencyName.trim().ifBlank { null },
                            emergencyContactPhone = emergencyPhone.trim().ifBlank { null }
                        )
                    )
                    if (existing != null && allDocsUploaded) onSubmit()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
                enabled = !isSaving && !isSubmitting
            ) {
                if (isSaving || isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (existing == null) "Save details" else if (allDocsUploaded) "Save & submit for review" else "Save details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            if (existing != null && !allDocsUploaded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Upload every document above to submit for review.",
                    fontSize = 12.sp,
                    color = TextLight
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VehicleTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Teal else SurfaceWhite,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else TextSecondary
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
}

@Composable
private fun OnboardingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceWhite,
            unfocusedContainerColor = SurfaceWhite,
            focusedBorderColor = Teal,
            unfocusedBorderColor = BorderGray
        )
    )
}

@Composable
private fun OnboardingErrorBanner(message: String, onDismiss: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CoralSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, modifier = Modifier.weight(1f), color = CoralDark, fontSize = 13.sp)
            if (onDismiss != null) {
                Text(
                    "Dismiss",
                    color = CoralDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onDismiss() }
                )
            }
        }
    }
}
