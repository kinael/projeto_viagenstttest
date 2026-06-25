package com.example.projeto_viagens.data.location

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationRepository(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5_000L
    ).apply {
        setMinUpdateDistanceMeters(10f)
        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        setWaitForAccurateLocation(true)
    }.build()

    /** Geocoding reverso: retorna cidade, estado e país a partir de coordenadas. */
    private suspend fun getAddressFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Triple<String?, String?, String?> = withContext(Dispatchers.IO) {

        val geocoder = Geocoder(context, Locale.getDefault())

        fun extractFrom(address: Address?): Triple<String?, String?, String?> {
            if (address == null) return Triple(null, null, null)
            val city = address.locality ?: address.subAdminArea ?: address.adminArea
            val state = address.adminArea
            val country = address.countryName
            return Triple(city, state, country)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            var result = Triple<String?, String?, String?>(null, null, null)
            val latch = java.util.concurrent.CountDownLatch(1)
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                result = extractFrom(addresses.firstOrNull())
                latch.countDown()
            }
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
            result
        } else {
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                extractFrom(addresses?.firstOrNull())
            } catch (e: Exception) {
                Triple(null, null, null)
            }
        }
    }

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ])
    fun locationWithCityFlow(): Flow<LocationInfo> =
        locationFlow()
            .map { location ->
                val (city, state, country) = getAddressFromCoordinates(
                    location.latitude,
                    location.longitude
                )
                LocationInfo(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    city = city,
                    state = state,
                    country = country
                )
            }
            .distinctUntilChangedBy { it.city }

    @RequiresPermission(anyOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ])
    fun locationFlow(): Flow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }.flowOn(Dispatchers.IO)
}