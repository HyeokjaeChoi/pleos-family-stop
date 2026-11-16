package ai.familystop.impl.platform

import ai.familystop.core.GeoPoint
import ai.familystop.core.LocationError
import ai.familystop.core.LocationSource
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper

/** Picks the provider: FUSED on API 31+, else GPS; null = none enabled (ADR-0001). */
internal fun pickProvider(sdkInt: Int, isEnabled: (String) -> Boolean): String? = listOfNotNull(
    LocationManager.FUSED_PROVIDER.takeIf { sdkInt >= Build.VERSION_CODES.S },
    LocationManager.GPS_PROVIDER
).firstOrNull(isEnabled)

/** GPS needs FINE; FUSED (API 31+) also works with approximate location. */
internal fun permitted(provider: String, fine: Boolean, coarse: Boolean) =
    fine || (provider != LocationManager.GPS_PROVIDER && coarse)

/** [LocationSource] on Android's LocationManager; swapped for Pleos FusedLocation when it opens. */
class PlatformLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)
    private var listener: LocationListener? = null

    private fun granted(permission: String) =
        appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    override fun start(onUpdate: (GeoPoint) -> Unit, onUnavailable: (LocationError) -> Unit) {
        stop()
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val provider = pickProvider(Build.VERSION.SDK_INT) { manager.isProviderEnabled(it) }
            ?: return onUnavailable(LocationError.UNAVAILABLE)
        if (!permitted(provider, fine, granted(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            return onUnavailable(LocationError.NO_PERMISSION)
        }
        // Explicit no-op overrides: onStatusChanged/onProviderEnabled are abstract below API 30.
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (listener ===
                    this
                ) {
                    onUpdate(GeoPoint(location.latitude, location.longitude, location.time))
                }
            }

            override fun onProviderDisabled(provider: String) {
                if (listener === this) onUnavailable(LocationError.OFF)
            }

            override fun onProviderEnabled(provider: String) = Unit

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        try {
            listener = l
            manager.requestLocationUpdates(
                provider,
                UPDATE_INTERVAL_MS,
                0f,
                l,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            listener = null
            onUnavailable(LocationError.NO_PERMISSION)
        }
    }

    override fun stop() {
        listener?.let { manager.removeUpdates(it) }
        listener = null
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 1_000L
    }
}
