package com.vestis.wardrobe.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationUtil {

    /**
     * Requests a single fresh location fix and returns the hemisphere.
     * Requires ACCESS_COARSE_LOCATION permission to be granted before calling.
     * Returns SOUTH as a safe default if location is unavailable.
     */
    @SuppressLint("MissingPermission")
    suspend fun detectHemisphere(context: Context): Hemisphere =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    val h = if (location != null)
                        SeasonUtil.hemisphereFromLatitude(location.latitude)
                    else
                        Hemisphere.SOUTH
                    if (cont.isActive) cont.resume(h)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(Hemisphere.SOUTH)
                }

            cont.invokeOnCancellation { cts.cancel() }
        }
}
