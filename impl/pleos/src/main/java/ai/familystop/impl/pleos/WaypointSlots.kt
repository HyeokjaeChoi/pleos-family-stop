package ai.familystop.impl.pleos

import ai.familystop.core.StopPlace

/** Tracks the NaviHelper waypoint slots (max 2, ADR-0003). Pure logic, no SDK types. */
class WaypointSlots(private val size: Int = 2) {
    private val slots = arrayOfNulls<StopPlace>(size)

    /** Reserves the first free slot for [place]; null when all slots are taken or [place] is already in one. */
    fun reserve(place: StopPlace): Int? {
        if (slots.contains(place)) return null
        val i = slots.indexOfFirst { it == null }
        if (i >= 0) slots[i] = place
        return i.takeIf { it >= 0 }
    }

    /** Slot index holding [place], or null. */
    fun indexOf(place: StopPlace): Int? = slots.indexOf(place).takeIf { it >= 0 }

    fun release(index: Int) {
        slots[index] = null
    }
}
