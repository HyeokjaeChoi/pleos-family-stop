package ai.familystop.impl.pleos

import ai.familystop.core.StopPlace

/** Tracks the NaviHelper waypoint slots (max 2, ADR-0003). Pure logic, no SDK types. */
class WaypointSlots(private val size: Int = 2) {
    private val slots = arrayOfNulls<StopPlace>(size)

    /** Reserves the first free slot for [place]; null when all slots are taken or [place] is already in one. */
    fun reserve(place: StopPlace): Int? {
        if (indexOf(place) != null) return null
        val i = slots.indexOfFirst { it == null }
        if (i >= 0) slots[i] = place
        return i.takeIf { it >= 0 }
    }

    /** Slot index holding [place], or null. */
    fun indexOf(place: StopPlace): Int? {
        val i = slots.indexOfFirst { it != null && same(it, place) }
        return if (i >= 0) i else null
    }

    // Navi echoes waypoints with lat/lon 0.0 (emulator), so identity is the poiId, else the name.
    private fun same(a: StopPlace, b: StopPlace) =
        if (a.poiId != null && b.poiId != null) a.poiId == b.poiId else a.name == b.name

    /** Replaces all slots with what Navi reports (in slot order), e.g. waypoints left from an earlier app run. */
    fun sync(places: List<StopPlace>) {
        slots.fill(null)
        places.take(size).forEachIndexed { i, p -> slots[i] = p }
    }

    fun release(index: Int) {
        slots[index] = null
    }
}
