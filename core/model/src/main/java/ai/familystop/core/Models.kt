package ai.familystop.core

data class GeoPoint(val lat: Double, val lon: Double, val timeMs: Long)

data class StopPlace(val name: String, val lat: Double, val lon: Double, val poiId: String? = null)
