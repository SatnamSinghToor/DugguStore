package com.duggustore.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.duggustore.app.R
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Satellite map via osmdroid, tiled from Esri's free World Imagery service
 * rather than a plain street map — no API key or billing account either
 * way, unlike the Google Maps SDK. Draws an origin marker (the rider, when
 * a fix is available) as a plain dot, a destination pin, and either the
 * real road route (when [routePoints] came back from OSRM) or a straight
 * line between the two as a fallback — the route is drawn with a white
 * halo underneath its own colour so it still reads clearly over whatever
 * terrain or rooftops sit beneath it.
 */
@Composable
fun OsmMapView(
    destination: GeoPoint,
    destinationLabel: String,
    modifier: Modifier = Modifier,
    origin: GeoPoint? = null,
    routePoints: List<GeoPoint>? = null,
    routeColor: Int = android.graphics.Color.parseColor("#2BB3AC")
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(EsriSatelliteTileSource)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { view ->
            view.overlays.clear()

            val destinationMarker = Marker(view).apply {
                position = destination
                icon = ContextCompat.getDrawable(context, R.drawable.marker_pin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = destinationLabel
                // The bottom info card already carries this; the default
                // osmdroid tooltip bubble is plain and unbranded, so it's
                // left off rather than tapping into it.
                infoWindow = null
            }
            view.overlays.add(destinationMarker)

            if (origin != null) {
                val linePoints = routePoints?.takeIf { it.size >= 2 } ?: listOf(origin, destination)

                // A wider white line underneath the teal one, so the route
                // stays legible over light-coloured roofs and roads in the
                // satellite imagery instead of blending into them.
                val haloLine = Polyline(view).apply {
                    setPoints(linePoints)
                    outlinePaint.color = android.graphics.Color.WHITE
                    outlinePaint.strokeWidth = 14f
                }
                view.overlays.add(haloLine)

                val line = Polyline(view).apply {
                    setPoints(linePoints)
                    outlinePaint.color = routeColor
                    outlinePaint.strokeWidth = 8f
                }
                view.overlays.add(line)

                val originMarker = Marker(view).apply {
                    position = origin
                    icon = ContextCompat.getDrawable(context, R.drawable.marker_you)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "You"
                    infoWindow = null
                }
                view.overlays.add(originMarker)

                // Posted rather than called directly: the view needs a
                // measured size first, which it doesn't have on the same
                // pass it was created.
                view.post {
                    view.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(listOf(origin, destination)),
                        true,
                        140
                    )
                }
            } else {
                view.controller.setCenter(destination)
            }

            view.invalidate()
        }
    )
}
