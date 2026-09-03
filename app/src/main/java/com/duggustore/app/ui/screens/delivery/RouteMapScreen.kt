package com.duggustore.app.ui.screens.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duggustore.app.data.repository.RoutingRepository
import com.duggustore.app.platform.LocationState
import com.duggustore.app.platform.rememberDeviceLocation
import com.duggustore.app.ui.components.OsmMapView
import com.duggustore.app.ui.theme.*
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

/**
 * In-app turn-by-turn view for a rider's pickup or drop stop: OpenStreetMap
 * tiles (osmdroid) with the real road route from OSRM's public routing
 * server drawn between the rider's live position and the destination —
 * free and accurate, with no external maps app hand-off and no API key.
 */
@Composable
fun RouteMapScreen(
    title: String,
    destinationLabel: String,
    destinationLat: Double,
    destinationLng: Double,
    onBack: () -> Unit
) {
    val detected = rememberDeviceLocation()
    val originState = detected.state as? LocationState.Found
    val origin = originState?.let { GeoPoint(it.latitude, it.longitude) }
    val destination = remember(destinationLat, destinationLng) { GeoPoint(destinationLat, destinationLng) }

    val routingRepo = remember { RoutingRepository() }
    var routePoints by remember { mutableStateOf<List<GeoPoint>?>(null) }
    var routeInfo by remember { mutableStateOf<Pair<Double, Double>?>(null) } // metres, seconds
    var routeFailed by remember { mutableStateOf(false) }

    LaunchedEffect(originState?.latitude, originState?.longitude) {
        val fix = originState ?: return@LaunchedEffect
        routeFailed = false
        routingRepo.getDrivingRoute(fix.latitude, fix.longitude, destinationLat, destinationLng)
            .onSuccess { result ->
                routePoints = result.points.map { GeoPoint(it.latitude, it.longitude) }
                routeInfo = result.distanceMetres to result.durationSeconds
            }
            .onFailure {
                // A straight line between the two points is still a usable
                // map even when the routing call itself couldn't be reached.
                routeFailed = true
                routePoints = null
                routeInfo = null
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Surface(color = Teal) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = destinationLabel,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            OsmMapView(
                destination = destination,
                destinationLabel = destinationLabel,
                origin = origin,
                routePoints = routePoints,
                modifier = Modifier.fillMaxSize()
            )

            if (origin == null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = OrangeSurface
                ) {
                    Text(
                        text = when (detected.state) {
                            LocationState.Locating -> "Finding your location…"
                            is LocationState.Unavailable -> "Turn on location to see your position and the route"
                            else -> "Waiting for your location…"
                        },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontSize = 12.sp,
                        color = OrangeDark
                    )
                }
            }
        }

        val (distance, duration) = routeInfo ?: (null to null)
        Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Coral, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            distance != null -> "%.1f km".format(distance / 1000)
                            routeFailed -> "Route unavailable"
                            else -> "Locating…"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                if (duration != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalShipping, null, tint = Teal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${(duration / 60).roundToInt()} min",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
