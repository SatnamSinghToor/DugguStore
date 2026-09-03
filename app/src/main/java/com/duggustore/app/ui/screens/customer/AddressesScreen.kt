package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.Address
import com.duggustore.app.ui.components.DugguButton
import com.duggustore.app.ui.components.DugguTextField
import com.duggustore.app.ui.components.DugguTopBar
import com.duggustore.app.ui.components.EmptyState
import com.duggustore.app.ui.theme.*

@Composable
fun AddressesScreen(
    addresses: List<Address>,
    isLoading: Boolean,
    onSaveAddress: (label: String, fullAddress: String, isDefault: Boolean, existingId: String) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onSetDefault: (String) -> Unit,
    onBack: () -> Unit,
    /** When set, picking an address returns it to the caller instead of just managing the list. */
    onSelectAddress: ((Address) -> Unit)? = null
) {
    var editing by remember { mutableStateOf<Address?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        DugguTopBar(
            title = if (onSelectAddress != null) "Choose Address" else "My Addresses",
            onBackClick = onBack
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading && addresses.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }
                addresses.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.LocationOff,
                        title = "No addresses yet",
                        subtitle = "Add a delivery address to place an order"
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(addresses, key = { it.id }) { address ->
                            AddressCard(
                                address = address,
                                onClick = { onSelectAddress?.invoke(address) ?: onSetDefault(address.id) },
                                onEdit = { editing = address; showSheet = true },
                                onDelete = { onDeleteAddress(address.id) }
                            )
                        }
                    }
                }
            }
        }

        Surface(color = Color.White, shadowElevation = 12.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                DugguButton(
                    text = "Add New Address",
                    onClick = { editing = null; showSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showSheet) {
        AddressDialog(
            existing = editing,
            onDismiss = { showSheet = false; editing = null },
            onSave = { label, full, isDefault ->
                onSaveAddress(label, full, isDefault, editing?.id ?: "")
                showSheet = false
                editing = null
            }
        )
    }
}

@Composable
private fun AddressCard(
    address: Address,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (address.isDefault) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (address.isDefault) "Default address" else "Set as default",
                tint = if (address.isDefault) PrimaryGreen else TextLight,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = address.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = PrimaryGreen.copy(alpha = 0.12f)) {
                            Text(
                                text = "DEFAULT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.fullAddress,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = AccentRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddressDialog(
    existing: Address?,
    onDismiss: () -> Unit,
    onSave: (label: String, fullAddress: String, isDefault: Boolean) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "Home") }
    var fullAddress by remember { mutableStateOf(existing?.fullAddress ?: "") }
    var isDefault by remember { mutableStateOf(existing?.isDefault ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Address" else "Edit Address") },
        text = {
            Column {
                DugguTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = "Label (Home, Work…)"
                )
                Spacer(modifier = Modifier.height(12.dp))
                DugguTextField(
                    value = fullAddress,
                    onValueChange = { fullAddress = it },
                    label = "Full address",
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                    )
                    Text("Set as default", fontSize = 14.sp, color = TextPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label, fullAddress, isDefault) },
                enabled = fullAddress.isNotBlank()
            ) {
                Text("Save", color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
