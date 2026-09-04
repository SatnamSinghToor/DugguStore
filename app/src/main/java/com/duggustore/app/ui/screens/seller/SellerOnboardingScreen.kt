package com.duggustore.app.ui.screens.seller

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import com.duggustore.app.data.model.SELLER_DOC_TYPES
import com.duggustore.app.data.model.Seller
import com.duggustore.app.data.model.SellerDocument
import com.duggustore.app.data.model.docTypeLabel
import com.duggustore.app.ui.components.DocumentUploadRow
import com.duggustore.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A seller's application, matching what a real quick-commerce marketplace
 * asks for: business + bank details, PAN/GST/FSSAI, and the documents that
 * back them up — laid out as three sections on one scrolling form rather
 * than as separate wizard pages, so nothing is lost if the seller has to
 * come back to it later.
 */
@Composable
fun SellerOnboardingScreen(
    userId: String,
    prefillEmail: String,
    prefillPhone: String,
    existing: Seller?,
    documents: List<SellerDocument>,
    isSaving: Boolean,
    isSubmitting: Boolean,
    uploadingDocType: String?,
    error: String?,
    onUploadDocument: (docType: String, bytes: ByteArray, contentType: String) -> Unit,
    onSave: (Seller) -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit,
    onSignOut: () -> Unit
) {
    var businessName by remember(existing) { mutableStateOf(existing?.businessName.orEmpty()) }
    var ownerName by remember(existing) { mutableStateOf(existing?.ownerName.orEmpty()) }
    var phone by remember(existing) { mutableStateOf(existing?.phone ?: prefillPhone) }
    var pan by remember(existing) { mutableStateOf(existing?.panNumber.orEmpty()) }
    var gst by remember(existing) { mutableStateOf(existing?.gstNumber.orEmpty()) }
    var fssai by remember(existing) { mutableStateOf(existing?.fssaiNumber.orEmpty()) }
    var bankAccount by remember(existing) { mutableStateOf(existing?.bankAccountNumber.orEmpty()) }
    var bankIfsc by remember(existing) { mutableStateOf(existing?.bankIfsc.orEmpty()) }
    var upiId by remember(existing) { mutableStateOf(existing?.upiId.orEmpty()) }
    var businessAddress by remember(existing) { mutableStateOf(existing?.businessAddress.orEmpty()) }
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
    val allDocsUploaded = remember(documents) { SELLER_DOC_TYPES.all { documentsByType.containsKey(it) } }
    val requiredFilled = businessName.isNotBlank() && ownerName.isNotBlank() && phone.isNotBlank() &&
        pan.isNotBlank() && bankAccount.isNotBlank() && bankIfsc.isNotBlank() && businessAddress.isNotBlank()

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Surface(color = Teal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Become a seller", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Fill this in once — an admin reviews it before you can list products",
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

            SectionLabel("Step 1 · Business details")
            Spacer(Modifier.height(10.dp))
            OnboardingField(businessName, { businessName = it }, "Business / store name")
            Spacer(Modifier.height(10.dp))
            OnboardingField(ownerName, { ownerName = it }, "Owner's full name")
            Spacer(Modifier.height(10.dp))
            OnboardingField(phone, { phone = it }, "Phone number", keyboardType = KeyboardType.Phone)
            Spacer(Modifier.height(10.dp))
            OnboardingField(businessAddress, { businessAddress = it }, "Business / pickup address")

            Spacer(Modifier.height(22.dp))
            SectionLabel("Step 2 · Tax & bank details")
            Spacer(Modifier.height(10.dp))
            OnboardingField(pan, { pan = it.uppercase() }, "PAN number")
            Spacer(Modifier.height(10.dp))
            OnboardingField(gst, { gst = it.uppercase() }, "GST number (if you have one)")
            Spacer(Modifier.height(10.dp))
            OnboardingField(fssai, { fssai = it }, "FSSAI licence (for food & grocery)")
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
                    "Save your business details first to unlock document uploads.",
                    fontSize = 13.sp,
                    color = TextLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                SELLER_DOC_TYPES.forEach { docType ->
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
                        Seller(
                            id = userId,
                            businessName = businessName.trim(),
                            ownerName = ownerName.trim(),
                            email = prefillEmail,
                            phone = phone.trim(),
                            panNumber = pan.trim(),
                            gstNumber = gst.trim().ifBlank { null },
                            fssaiNumber = fssai.trim().ifBlank { null },
                            bankAccountNumber = bankAccount.trim(),
                            bankIfsc = bankIfsc.trim(),
                            upiId = upiId.trim().ifBlank { null },
                            businessAddress = businessAddress.trim()
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
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
