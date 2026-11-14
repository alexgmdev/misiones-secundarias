package com.groovycow.misionessecundarias.ui.missions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.groovycow.misionessecundarias.domain.model.Mission
import com.groovycow.misionessecundarias.domain.util.parseWktPoint
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun MissionMapScreen(
    missions: List<Mission>,
    onMissionClick: (Mission) -> Unit,
    modifier: Modifier = Modifier,
) {
    val caliLat = 3.4516
    val caliLon = -76.5320
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Crear el MapView una sola vez y recordarlo
    val mapView = remember { mutableStateOf<MapView?>(null) }
    val mapLibreMap = remember { mutableStateOf<MapLibreMap?>(null) }

    // 2. Ciclo de vida COMPLETO, incluyendo onDestroy
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.value?.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.value?.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.value?.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.value?.onStop()
                    Lifecycle.Event.ON_DESTROY -> {
                        mapView.value?.onDestroy() // 🔥 CRÍTICO: libera el GLThread
                        mapView.value = null
                        mapLibreMap.value = null
                    }
                    else -> {}
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Asegurar la destrucción si el composable sale sin ON_DESTROY
            mapView.value?.onDestroy()
            mapView.value = null
            mapLibreMap.value = null
        }
    }

    // 3. Actualizar pines cuando cambian las misiones
    LaunchedEffect(missions, mapLibreMap.value) {
        val map = mapLibreMap.value ?: return@LaunchedEffect
        map.clear()

        val markerMissionMap = mutableMapOf<Marker, Mission>()
        missions.forEach { mission ->
            val point = parseWktPoint(mission.location)
            if (point != null) {
                val marker =
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(point.latitude, point.longitude))
                            .title(mission.title),
                    )
                markerMissionMap[marker] = mission
            }
        }

        map.setOnMarkerClickListener { clickedMarker ->
            markerMissionMap[clickedMarker]?.let {
                onMissionClick(it)
                true
            } ?: false
        }
    }

    // 4. AndroidView con factory que crea el MapView
    AndroidView<MapView>(
        factory = { context ->
            MapView(context).apply {
                mapView.value = this
                onCreate(null) // Inicializa el renderizador nativo
                getMapAsync { map ->
                    map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                        map.cameraPosition =
                            CameraPosition.Builder()
                                .target(LatLng(caliLat, caliLon))
                                .zoom(12.0)
                                .build()
                        mapLibreMap.value = map
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        // 5. onRelease se llama cuando el AndroidView se elimina
        onRelease = { view ->
            view.onDestroy()
            mapView.value = null
            mapLibreMap.value = null
        },
    )
}
