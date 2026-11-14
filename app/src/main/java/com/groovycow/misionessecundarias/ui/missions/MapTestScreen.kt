package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.fillMaxSize

@Composable
fun MapTestScreen() {
    val caliLat = 3.4516
    val caliLon = -76.5320
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Mantenemos una referencia al MapView
    val mapView = remember { mutableStateOf<MapView?>(null) }

    // 2. Observamos el ciclo de vida de la pantalla para pausar/reanudar el mapa
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.value?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.value?.onPause()
                Lifecycle.Event.ON_STOP -> mapView.value?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.value?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView<MapView>(
        factory = { context ->
            MapView(context).apply {
                mapView.value = this
                onCreate(null) // OBLIGATORIO antes de usarlo

                getMapAsync { map ->
                    // Usamos un estilo público de prueba de MapLibre
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(caliLat, caliLon))
                            .zoom(12.0)
                            .build()
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}