package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.duggustore.app.ui.components.AuthField
import com.duggustore.app.ui.components.DashboardEmpty
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
        Surface(color = Teal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Column {
                    Text(
                        text = if (onSelectAddress != null) "Choose address" else "My addresses",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (addresses.size == 1) "1 saved" else "${addresses.size} saved",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading && addresses.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Teal)
                }

                addresses.isEmpty() -> DashboardEmpty(
                    icon = Icons.Default.LocationOff,
                    title = "No addresses yet",
                    subtitle = "Add a delivery address to place an order"
                )

                else -> LazyColumn(
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

        Surface(
            color = SurfaceWhite,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            shadowElevation = 18.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = { editing = null; showSheet = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Add new address",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
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
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (address.isDefault) TealSurface else SurfaceWhite,
        shadowElevation = if (address.isDefault) 0.dp else 2.dp,
        border = if (address.isDefault) BorderStroke(1.5.dp, Teal) else null
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (address.isDefault) Icons.Default.CheckCircle
                              else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (address.isDefault) "Default address" else "Set as default",
                tint = if (address.isDefault) Teal else TextLight,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = address.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (address.isDefault) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = Teal) {
                            Text(
                                text = "DEFAULT",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = address.fullAddress,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Edit,
                    "Edit ${address.label}",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    "Delete ${address.label}",
                    tint = Coral,
                    modifier = Modifier.size(18.dp)
                )
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
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (existing == null) "Add address" else "Edit address",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column {
                AuthField(
                    value = label,
                    onValueChange = { label = it },
                    label = "Label",
                    placeholder = "Home, Work…"
                )
                Spacer(Modifier.height(14.dp))
                AuthField(
                    value = fullAddress,
                    onValueChange = { fullAddress = it },
                    label = "Full address",
                    placeholder = "House, street, area, pincode"
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.clickable { isDefault = !isDefault },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = Teal)
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
                Text("Save", color = Teal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
