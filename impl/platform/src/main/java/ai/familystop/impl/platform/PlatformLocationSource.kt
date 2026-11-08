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
import android.os.Looper

/** Picks the provider: FUSED on API 31+, else GPS; null = none enabled (ADR-0001). */
internal fun pickProvider(sdkInt: Int, isEnabled: (String) -> Boolean): String? = listOf(
    LocationManager.FUSED_PROVIDER.takeIf { sdkInt >= Build.VERSION_CODES.S },
    LocationManager.GPS_PROVIDER
).filterNotNull().firstOrNull(isEnabled)

/** [LocationSource] on Android's LocationManager; swapped for Pleos FusedLocation when it opens. */
class PlatformLocationSource(context: Context) : LocationSource {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
    private var listener: LocationListener? = null

    override fun start(onUpdate: (GeoPoint) -> Unit, onUnavailable: (String) -> Unit) {
        stop()
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onUnavailable("위치 권한이 없습니다")
            return
        }
        val lm = manager
        val provider = lm?.let { m ->
            pickProvider(Build.VERSION.SDK_INT) { m.isProviderEnabled(it) }
        }
        if (lm == null || provider == null) {
            onUnavailable("위치를 사용할 수 없습니다")
            return
        }
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) =
                onUpdate(GeoPoint(location.latitude, location.longitude, location.time))

            override fun onProviderDisabled(provider: String) = onUnavailable("위치가 꺼졌습니다")
        }
        try {
            lm.requestLocationUpdates(provider, UPDATE_INTERVAL_MS, 0f, l, Looper.getMainLooper())
            listener = l
        } catch (e: SecurityException) {
            onUnavailable("위치 권한이 없습니다")
        }
    }

    override fun stop() {
        listener?.let { manager?.removeUpdates(it) }
        listener = null
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 1_000L
    }
}
