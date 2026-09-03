package com.duggustore.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.model.DeliveryTracking
import com.duggustore.app.ui.theme.*

/**
 * Where the rider is, on the customer's order.
 *
 * There is no map drawn in the app: an embedded map means the Google Maps SDK
 * and an API key, which is a decision about billing rather than about layout.
 * What is here works with neither — how far away the rider is, how fresh the
 * fix is, and a handoff to whatever map app the phone already has.
 */
@Composable
fun RiderLocationCard(
    tracking: DeliveryTracking?,
    /** Straight-line metres to the delivery address, when it could be resolved. */
    distanceMetres: Float?,
    ageMinutes: Long?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (tracking?.hasFix() == true) TealSurface else SurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tracking?.hasFix() == true) Icons.Default.LocalShipping
                                      else Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = if (tracking?.hasFix() == true) Teal else TextLight,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your rider",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = describe(tracking, distanceMetres, ageMinutes),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }

            if (tracking?.hasFix() == true) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Teal)
                        .clickable { openInMaps(context, tracking) }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "See on map",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun describe(
    tracking: DeliveryTracking?,
    distanceMetres: Float?,
    ageMinutes: Long?
): String {
    if (tracking == null || !tracking.hasFix()) {
        return "Not sharing their location right now"
    }

    val distance = when {
        distanceMetres == null -> null
        distanceMetres < 1000f -> "${distanceMetres.toInt()} m away"
        else -> "%.1f km away".format(distanceMetres / 1000f)
    }
    val freshness = when {
        ageMinutes == null -> null
        ageMinutes <= 0L -> "just now"
        ageMinutes == 1L -> "1 minute ago"
        ageMinutes < 60L -> "$ageMinutes minutes ago"
        else -> "over an hour ago"
    }

    return listOfNotNull(distance, freshness?.let { "updated $it" })
        .joinToString(" · ")
        .ifBlank { "Location shared" }
}

private fun openInMaps(context: android.content.Context, tracking: DeliveryTracking) {
    // geo: is handled by any map app on the device, so this needs no key and no
    // assumption about which one is installed.
    val uri = Uri.parse(
        "geo:${tracking.latitude},${tracking.longitude}" +
            "?q=${tracking.latitude},${tracking.longitude}(Your rider)"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        // No map app; the distance line above is still the useful part.
    }
}
