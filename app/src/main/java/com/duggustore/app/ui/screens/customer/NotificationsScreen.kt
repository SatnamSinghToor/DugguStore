package com.duggustore.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.StoreNotification
import com.duggustore.app.ui.components.DashboardEmpty
import com.duggustore.app.R
import com.duggustore.app.ui.theme.*

@Composable
fun NotificationsScreen(
    notifications: List<StoreNotification>,
    onNotificationClick: (StoreNotification) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Surface(color = Teal.copy(alpha = 0.92f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, stringResource(R.string.common_back), tint = Color.White)
                }
                Column {
                    Text(
                        stringResource(R.string.notifications_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (notifications.size == 1) "1 update" else "${notifications.size} updates",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            DashboardEmpty(
                icon = Icons.Default.NotificationsNone,
                title = stringResource(R.string.notifications_empty_title),
                subtitle = stringResource(R.string.notifications_empty_sub)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: StoreNotification, onClick: () -> Unit) {
    val (icon, tint) = iconFor(notification.kind)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = notification.body,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
                if (notification.timestamp.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notification.timestamp.take(10),
                        fontSize = 11.sp,
                        color = TextLight
                    )
                }
            }
        }
    }
}

private fun iconFor(kind: StoreNotification.Kind): Pair<ImageVector, Color> = when (kind) {
    StoreNotification.Kind.Placed -> Icons.Default.Receipt to Orange
    StoreNotification.Kind.Confirmed -> Icons.Default.CheckCircle to Teal
    StoreNotification.Kind.Preparing -> Icons.Default.Inventory to Orange
    StoreNotification.Kind.ReadyForPickup -> Icons.Default.ShoppingBag to Orange
    StoreNotification.Kind.OutForDelivery -> Icons.Default.LocalShipping to Teal
    StoreNotification.Kind.Delivered -> Icons.Default.CheckCircle to SuccessGreen
    StoreNotification.Kind.Cancelled -> Icons.Default.Cancel to Coral
}
