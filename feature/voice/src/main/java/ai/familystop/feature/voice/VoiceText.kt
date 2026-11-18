package ai.familystop.feature.voice

import ai.familystop.core.StopNeed
import ai.familystop.core.designsystem.R as DsR
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/** Spoken sentence for [u] in [locale] (ADR-0007). Built outside Compose, so it reads resources directly. */
fun spokenText(context: Context, u: Utterance, locale: Locale): String {
    val res = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
    ).resources
    fun ordinal(n: Int) = res.getString(DsR.string.ds_unit_ordinal, n)
    return when (u) {
        is Utterance.Recommend -> res.getQuantityString(
            R.plurals.voice_say_recommend,
            u.names.size,
            u.names.size,
            u.names.mapIndexed { i, name ->
                res.getString(R.string.voice_say_item, ordinal(i + 1), name)
            }
                .joinToString(res.getString(R.string.voice_say_separator))
        )
        is Utterance.Next -> {
            val names = u.reason.needs.map {
                res.getString(
                    when (it) {
                        StopNeed.MEAL -> R.string.voice_need_meal
                        StopNeed.RESTROOM -> R.string.voice_need_restroom
                        StopNeed.KIDS_REST -> R.string.voice_need_kids_rest
                    }
                )
            }.joinToString(res.getString(R.string.voice_need_separator))
            val reason = res.getString(
                if (u.reason.coversAll) R.string.voice_reason_all else R.string.voice_reason_some,
                names
            )
            res.getString(R.string.voice_say_next, ordinal(u.ordinal), u.name, reason)
        }
        is Utterance.Result -> if (u.ok) {
            res.getString(R.string.voice_say_added, u.name)
        } else {
            res.getString(R.string.voice_say_add_failed)
        }
    }
}
