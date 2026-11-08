package ai.familystop.core.designsystem

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/** DESIGN.md 11절 모션 토큰. 상태 변화 알림용만, bounce·blur·shake·무한 반복 금지. */
object AppMotion {
    const val PRESS_MS = 100
    const val PRESS_RELEASE_MS = 150
    const val FADE_MS = 150
    const val ENTER_MS = 200
    const val EXIT_MS = 160
    const val MOVE_MS = 250
    const val PRESS_SCALE = 0.97f
    const val PULSE_SCALE = 1.02f
    const val RISE_DP = 12

    val EaseEnter = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseExit = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val EaseMove = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** 시스템 애니메이션 끔(`ANIMATOR_DURATION_SCALE == 0`)이면 true. 값은 AppTheme이 제공. */
    val reduce: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current

    /** reduce-motion이면 즉시 교체(snap), 아니면 [ms] tween. */
    fun <T> spec(reduce: Boolean, ms: Int, easing: Easing = EaseEnter): FiniteAnimationSpec<T> =
        if (reduce) snap() else tween(ms, easing = easing)
}

val LocalReduceMotion = compositionLocalOf { false }

@Composable
internal fun rememberSystemReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    fun read() =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reduce by remember(resolver) { mutableStateOf(read()) }
    // 실행 중 시스템 설정이 바뀌어도 따라간다.
    DisposableEffect(resolver) {
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduce = read()
            }
        }
        resolver.registerContentObserver(uri, false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduce
}

/** 누름 시 0.97배. graphicsLayer라 레이아웃·터치 대상 크기는 그대로다. */
@Composable
fun Modifier.pressScale(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) AppMotion.PRESS_SCALE else 1f,
        AppMotion.spec(
            AppMotion.reduce,
            if (pressed) AppMotion.PRESS_MS else AppMotion.PRESS_RELEASE_MS
        ),
        label = "press"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
