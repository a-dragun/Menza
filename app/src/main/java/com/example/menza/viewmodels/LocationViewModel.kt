@file:Suppress("DEPRECATION")

package com.example.menza.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import com.example.menza.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationViewModel : ViewModel() {
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation

    fun updateLocation(lat: Double, lon: Double) {
        Log.d("LocationViewModel", "Updating location to: $lat, $lon")
        _userLocation.value = lat to lon
    }
}
fun isLocationServicesEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationTracker(locationViewModel: LocationViewModel) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    var askedToEnableLocation by rememberSaveable { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text(stringResource(R.string.location_disabled_title)) },
            text = { Text(stringResource(R.string.location_disabled_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    askedToEnableLocation = true
                    context.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }) {
                    Text(stringResource(R.string.enable))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLocationDialog = false
                    askedToEnableLocation = true
                }) {
                    Text(stringResource(R.string.not_now))
                }
            }
        )
    }

    LaunchedEffect(permissionState.status) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
        while (!permissionState.status.isGranted) {
            delay(200)
        }
        if (!isLocationServicesEnabled(context) && !askedToEnableLocation) {
            showLocationDialog = true
            return@LaunchedEffect
        }

        if (!isLocationServicesEnabled(context)) {
            Log.d("LocationTracker", "Location services disabled, but already asked once.")
            return@LaunchedEffect
        }
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                loc?.let {
                    Log.d("LocationTracker", "Immediate location received: ${it.latitude}, ${it.longitude}")
                    locationViewModel.updateLocation(it.latitude, it.longitude)
                } ?: run {
                    Log.d("LocationTracker", "Immediate location is null, waiting for updates...")
                }
            }
            .addOnFailureListener { e ->
                Log.e("LocationTracker", "Error getting current location", e)
            }
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    Log.d("LocationTracker", "Location update received: ${loc.latitude}, ${loc.longitude}")
                    locationViewModel.updateLocation(loc.latitude, loc.longitude)
                    fusedLocationClient.removeLocationUpdates(this)
                } else {
                    Log.d("LocationTracker", "Location update received but null")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
}


