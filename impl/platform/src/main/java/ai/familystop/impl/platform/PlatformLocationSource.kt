package ai.familystop.impl.platform

import ai.familystop.core.GeoPoint
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

/** [LocationSource] on Android's LocationManager; swapped for Pleos FusedLocation when it opens. */
class PlatformLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)
    private var listener: LocationListener? = null

    private fun granted(permission: String) =
        appContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    override fun start(onUpdate: (GeoPoint) -> Unit, onUnavailable: (String) -> Unit) {
        stop()
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val provider = pickProvider(Build.VERSION.SDK_INT) { manager.isProviderEnabled(it) }
            ?: return onUnavailable("위치를 사용할 수 없습니다")
        // GPS needs FINE; FUSED (API 31+) also works with approximate location.
        if (!fine &&
            (
                provider == LocationManager.GPS_PROVIDER ||
                    !granted(Manifest.permission.ACCESS_COARSE_LOCATION)
                )
        ) {
            return onUnavailable("위치 권한이 없습니다")
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
                if (listener === this) onUnavailable("위치가 꺼졌습니다")
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
            onUnavailable("위치 권한이 없습니다")
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
