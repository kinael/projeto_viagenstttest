package com.example.projeto_viagens.ui.screens

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Mapa OpenStreetMap (OSMDroid) centrado em [latitude]/[longitude] com um
 * marcador na posição atual. Não requer API key.
 */
@Composable
fun TripMapView(
    latitude: Double,
    longitude: Double,
    markerTitle: String = "",
    modifier: Modifier = Modifier,
    height: Dp = 220.dp
) {
    val context = LocalContext.current

    // Configuração obrigatória do OSMDroid (user-agent + cache)
    Configuration.getInstance().load(
        context,
        PreferenceManager.getDefaultSharedPreferences(context)
    )
    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember(context) {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
        }
    }

    DisposableEffect(latitude, longitude) {
        val point = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(point)
        mapView.controller.setZoom(14.0)

        mapView.overlays.clear()
        val marker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = markerTitle
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
        mapView.onResume()

        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    )
}
