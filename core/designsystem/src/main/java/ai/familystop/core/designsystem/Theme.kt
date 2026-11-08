package ai.familystop.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** DESIGN.md 2절 색 토큰. 외곽선 토큰은 없다(border-less): 구분은 면 단계와 간격으로만 한다. */
class AppColors(
    val bgBase: Color,
    val bgSurface: Color,
    val bgRaised: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentOn: Color,
    val statusOk: Color,
    val statusWarn: Color,
    val statusDanger: Color,
    val statusUnknown: Color,
    val focusRing: Color,
    val night: Boolean
)

internal val NightColors = AppColors(
    bgBase = Color(0xFF080A0C),
    bgSurface = Color(0xFF1B2126),
    bgRaised = Color(0xFF272E34),
    textPrimary = Color(0xFFF2F5F7),
    textSecondary = Color(0xFFB4BEC6),
    accentPrimary = Color(0xFF5CB8FF),
    accentOn = Color(0xFF06121C),
    statusOk = Color(0xFF5BD38A),
    statusWarn = Color(0xFFFFC247),
    statusDanger = Color(0xFFFF6B6B),
    statusUnknown = Color(0xFF9AA7B0),
    focusRing = Color(0xFFFFFFFF),
    night = true
)

internal val DayColors = AppColors(
    bgBase = Color(0xFFDDE3E8),
    bgSurface = Color(0xFFFFFFFF),
    bgRaised = Color(0xFFEEF1F4),
    textPrimary = Color(0xFF11181D),
    textSecondary = Color(0xFF46535C),
    accentPrimary = Color(0xFF0A62BA),
    accentOn = Color(0xFFFFFFFF),
    statusOk = Color(0xFF13673A),
    statusWarn = Color(0xFF7A4F00),
    statusDanger = Color(0xFFB71C1C),
    statusUnknown = Color(0xFF4F5E68),
    focusRing = Color(0xFF11181D),
    night = false
)

/** DESIGN.md 3절 텍스트 스타일 4개. 공식 최소(주요 32 · 보조 24) 미만은 두지 않는다. */
class AppType(
    val driveTitle: TextStyle =
        TextStyle(
            fontSize = 40.sp,
            lineHeight = 52.sp,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum"
        ),
    val driveBody: TextStyle =
        TextStyle(
            fontSize = 32.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Medium,
            fontFeatureSettings = "tnum"
        ),
    val stopTitle: TextStyle =
        TextStyle(
            fontSize = 32.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum"
        ),
    val stopBody: TextStyle =
        TextStyle(
            fontSize = 24.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Normal,
            fontFeatureSettings = "tnum"
        )
)

/** DESIGN.md 4절 간격. 터치 대상 사이는 [lg] 이상. */
object AppSpace {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object AppRadius {
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
}

object AppIcon {
    val primary = 44.dp
    val secondary = 36.dp
    val tertiary = 24.dp
}

private val LocalAppColors = staticCompositionLocalOf { NightColors }
private val LocalAppType = staticCompositionLocalOf { AppType() }

/** 화면 코드가 토큰을 읽는 유일한 진입점: `App.colors.bgSurface`, `App.type.driveBody`, `App.space.lg`. */
object App {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current
    val type: AppType
        @Composable @ReadOnlyComposable
        get() = LocalAppType.current
    val space = AppSpace
    val radius = AppRadius
    val icon = AppIcon

    /** 터치 대상 최소 크기(DESIGN.md 4절, 공식 76×76dp). */
    val touchMin = 76.dp

    /** 상태 줄(UndoBar·실패 안내) 고정 높이: 표시·사라짐에도 카드가 움직이지 않게 예약한다. */
    val statusSlot = 108.dp
}

/**
 * 앱 전체 테마. 낮/밤은 시스템 상태를 따른다(DESIGN.md 5절): 앱 안에 테마 토글을 두지 않는다.
 * 배경 면(`bg/base`)과 기본 글자색·스타일(stop/body)을 깔아 주므로 화면은 색을 직접 정하지 않는다.
 */
@Composable
fun AppTheme(
    night: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = rememberSystemReduceMotion(),
    content: @Composable () -> Unit
) {
    val c = if (night) NightColors else DayColors
    val t = remember { AppType() }
    val scheme = if (night) {
        darkColorScheme(
            primary = c.accentPrimary,
            onPrimary = c.accentOn,
            background = c.bgBase,
            onBackground = c.textPrimary,
            surface = c.bgSurface,
            onSurface = c.textPrimary,
            surfaceVariant = c.bgRaised,
            onSurfaceVariant = c.textSecondary,
            error = c.statusDanger
        )
    } else {
        lightColorScheme(
            primary = c.accentPrimary,
            onPrimary = c.accentOn,
            background = c.bgBase,
            onBackground = c.textPrimary,
            surface = c.bgSurface,
            onSurface = c.textPrimary,
            surfaceVariant = c.bgRaised,
            onSurfaceVariant = c.textSecondary,
            error = c.statusDanger
        )
    }
    // M3 기본 컴포넌트가 12~14sp를 쓰지 않도록 보조 이하 슬롯을 모두 stop/body로 맞춘다.
    val typography = Typography(
        titleLarge = t.stopTitle,
        titleMedium = t.stopTitle,
        titleSmall = t.stopBody,
        bodyLarge = t.stopBody,
        bodyMedium = t.stopBody,
        bodySmall = t.stopBody,
        labelLarge = t.stopBody,
        labelMedium = t.stopBody,
        labelSmall = t.stopBody
    )
    CompositionLocalProvider(
        LocalAppColors provides c,
        LocalAppType provides t,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            Surface(Modifier.fillMaxSize(), color = c.bgBase, contentColor = c.textPrimary) {
                ProvideTextStyle(t.stopBody, content)
            }
        }
    }
}
