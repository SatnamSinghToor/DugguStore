package com.duggustore.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * OpenStreetMap tiles via osmdroid — free, no API key or billing account,
 * unlike the Google Maps SDK. Draws an origin marker (the rider, when a fix
 * is available), a destination marker, and either the real road route (when
 * [routePoints] came back from OSRM) or a straight line between the two as
 * a fallback.
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
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
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
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = destinationLabel
            }
            view.overlays.add(destinationMarker)

            if (origin != null) {
                val originMarker = Marker(view).apply {
                    position = origin
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "You"
                }
                view.overlays.add(originMarker)

                val linePoints = routePoints?.takeIf { it.size >= 2 } ?: listOf(origin, destination)
                val line = Polyline(view).apply {
                    setPoints(linePoints)
                    outlinePaint.color = routeColor
                    outlinePaint.strokeWidth = 10f
                }
                view.overlays.add(line)

                // Posted rather than called directly: the view needs a
                // measured size first, which it doesn't have on the same
                // pass it was created.
                view.post {
                    view.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(listOf(origin, destination)),
                        true,
                        120
                    )
                }
            } else {
                view.controller.setCenter(destination)
            }

            view.invalidate()
        }
    )
}
