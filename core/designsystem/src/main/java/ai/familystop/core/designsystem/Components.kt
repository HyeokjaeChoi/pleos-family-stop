package ai.familystop.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 상태 4종. 아이콘 + 문구 + 색 3중으로 표시하고 색만으로 구분하지 않는다(DESIGN.md 6절). */
enum class Status { Ok, Warn, Danger, Unknown }

/** 상태 칩: `bg/raised` 면 + 상태색 아이콘 24dp + 본문색 문구. 아이콘만 단독으로 쓰지 않는다. */
@Composable
fun StatusChip(status: Status, text: String, modifier: Modifier = Modifier) {
    val c = App.colors
    // 성공으로 바뀔 때만 1회 pulse(1.0→1.02→1.0). 처음 표시·reduce-motion에서는 생략.
    val pulse = remember { Animatable(1f) }
    val reduce = AppMotion.reduce
    val last = remember { arrayOf(status) } // 상태 아님: 쓰기가 재구성을 부르지 않게
    LaunchedEffect(status) {
        if (status == Status.Ok && last[0] != Status.Ok && !reduce) {
            pulse.animateTo(
                AppMotion.PULSE_SCALE,
                tween(AppMotion.ENTER_MS / 2, easing = AppMotion.EaseEnter)
            )
            pulse.animateTo(1f, tween(AppMotion.ENTER_MS / 2, easing = AppMotion.EaseMove))
        }
        last[0] = status
    }
    Row(
        modifier
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .clip(RoundedCornerShape(App.radius.md))
            .background(c.bgRaised)
            .padding(horizontal = App.space.md, vertical = App.space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.sm)
    ) {
        // 아이콘+문구를 한 덩어리로 150ms 크로스페이드(DESIGN.md 11절). 크기는 칩이 맞춘다.
        AnimatedContent(
            status to text,
            transitionSpec = {
                (
                    fadeIn(AppMotion.spec(reduce, AppMotion.FADE_MS)) togetherWith
                        fadeOut(AppMotion.spec(reduce, AppMotion.FADE_MS))
                    ).using(
                    SizeTransform(clip = false) { _, _ ->
                        AppMotion.spec(reduce, AppMotion.FADE_MS)
                    }
                )
            },
            label = "chip"
        ) { (s, label) ->
            val (icon, tint) = when (s) {
                Status.Ok -> Icons.Rounded.CheckCircle to c.statusOk
                Status.Warn -> Icons.Rounded.Warning to c.statusWarn
                Status.Danger -> CancelIcon to c.statusDanger
                Status.Unknown -> HelpIcon to c.statusUnknown
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(App.space.sm)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(App.icon.tertiary)
                )
                Text(label, style = App.type.stopBody, color = c.textPrimary)
            }
        }
    }
}

/** 카드: 외곽선 없이 `bg/surface` 면. [onClick]이 있으면 카드 전체가 터치 대상. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    spacing: Dp = App.space.md,
    content: @Composable ColumnScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .then(if (onClick != null && enabled) Modifier.pressScale(source) else Modifier)
            .clip(RoundedCornerShape(App.radius.lg))
            .background(App.colors.bgSurface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        source,
                        LocalIndication.current,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(App.space.lg),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/**
 * 큰 버튼: 높이 ≥ 76dp. [onClick]이 null이면 표시용(부모 카드가 탭을 받음, 포커스 대상 하나만 유지).
 * [large]는 주행 중 글자(drive/body), 아니면 stop/body. [icon]은 문구 앞 36dp, [background]는 면 색 덮어쓰기(raised 위 버튼 등).
 */
@Composable
fun AppButton(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    large: Boolean = false,
    icon: ImageVector? = null,
    background: Color? = null
) {
    val c = App.colors
    val reduce = AppMotion.reduce
    val bg by animateColorAsState(
        background ?: if (primary && enabled) c.accentPrimary else c.bgRaised,
        AppMotion.spec(reduce, AppMotion.MOVE_MS, AppMotion.EaseMove),
        label = "btnBg"
    )
    val fg = when {
        !enabled -> c.textSecondary
        primary -> c.accentOn
        else -> c.textPrimary
    }
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = App.touchMin)
            .then(if (onClick != null && enabled) Modifier.pressScale(source) else Modifier)
            .clip(RoundedCornerShape(App.radius.md))
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        source,
                        LocalIndication.current,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = App.space.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.sm, Alignment.CenterHorizontally)
    ) {
        if (icon != null && enabled) {
            Icon(
                icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(App.icon.secondary)
            )
        }
        Text(label, style = if (large) App.type.driveBody else App.type.stopBody, color = fg)
    }
}

/** 빈 상태(DESIGN.md 7절): 아이콘 + 제목 + 안내 한 줄. 가운데 정렬로 면을 채운다. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(App.space.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            HelpIcon,
            contentDescription = null,
            tint = App.colors.statusUnknown,
            modifier = Modifier.size(App.icon.primary)
        )
        Text(title, style = App.type.driveTitle, color = App.colors.textPrimary)
        Text(body, style = App.type.driveBody, color = App.colors.textSecondary)
    }
}

/** 선택 칩: 선택은 면 색 + 체크 아이콘으로 표시한다(색만으로 구분하지 않음). */
@Composable
fun AppToggleChip(
    selected: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    role: Role = Role.Checkbox,
    onClick: () -> Unit
) {
    val c = App.colors
    Row(
        modifier
            .heightIn(min = App.touchMin)
            .clip(RoundedCornerShape(App.radius.md))
            .background(if (selected) c.accentPrimary else c.bgRaised)
            .then(
                if (role == Role.RadioButton) {
                    Modifier.selectable(selected, role = role, onClick = onClick)
                } else {
                    Modifier.toggleable(selected, role = role, onValueChange = { onClick() })
                }
            )
            .padding(horizontal = App.space.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.sm)
    ) {
        val fg = if (selected) c.accentOn else c.textPrimary
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(App.icon.tertiary)
            )
        }
        Text(label, style = App.type.stopBody, color = fg)
    }
}

/** 필요 항목 타일(Figma Tile/선택·기본): 위 아이콘 + 아래 라벨. 선택은 면 색 + 체크 아이콘(색만으로 구분하지 않음). */
@Composable
fun AppToggleTile(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = App.colors
    val fg = if (selected) c.accentOn else c.textPrimary
    Column(
        modifier
            .clip(RoundedCornerShape(App.radius.md))
            .background(if (selected) c.accentPrimary else c.bgRaised)
            .toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = App.space.sm, vertical = App.space.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(App.space.sm)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(App.icon.primary))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(App.space.sm)
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(App.icon.tertiary)
                )
            }
            Text(label, style = App.type.stopBody, color = fg, maxLines = 1, softWrap = false)
        }
    }
}

/** 요약 칩(Figma Chip/cur·fill·empty): 현재 단계는 accent 면, 정한 값은 raised 면, 못 정한 값은 흐린 글자. 누르면 그 단계로. */
@Composable
fun AppStepChip(
    label: String,
    current: Boolean,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = App.colors
    Row(
        modifier
            .heightIn(min = App.touchMin)
            .clip(RoundedCornerShape(App.radius.md))
            .background(if (current) c.accentPrimary else c.bgRaised)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = App.space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = App.type.stopBody,
            color = when {
                current -> c.accentOn
                filled -> c.textPrimary
                else -> c.textSecondary
            },
            maxLines = 1
        )
    }
}

/** 입력 필드: 밑줄·외곽선 없이 `bg/raised` 면. 높이 ≥ 76dp. */
@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val c = App.colors
    val none = Color.Transparent
    TextField(
        value,
        onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(App.radius.md),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = c.bgRaised,
            unfocusedContainerColor = c.bgRaised,
            focusedIndicatorColor = none,
            unfocusedIndicatorColor = none,
            disabledIndicatorColor = none,
            errorIndicatorColor = none,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
            focusedLabelColor = c.textSecondary,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.accentPrimary
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = App.touchMin)
    )
}

// Material Symbols Fill 1 글리프. material-icons-core에 없는 cancel/help만 직접 둔다(의존성 크기).
private val CancelIcon: ImageVector by lazy {
    ImageVector.Builder("Cancel", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.47f, 2f, 2f, 6.47f, 2f, 12f)
            reflectiveCurveToRelative(4.47f, 10f, 10f, 10f)
            reflectiveCurveToRelative(10f, -4.47f, 10f, -10f)
            reflectiveCurveTo(17.53f, 2f, 12f, 2f)
            close()
            moveTo(17f, 15.59f)
            lineTo(15.59f, 17f)
            lineTo(12f, 13.41f)
            lineTo(8.41f, 17f)
            lineTo(7f, 15.59f)
            lineTo(10.59f, 12f)
            lineTo(7f, 8.41f)
            lineTo(8.41f, 7f)
            lineTo(12f, 10.59f)
            lineTo(15.59f, 7f)
            lineTo(17f, 8.41f)
            lineTo(13.41f, 12f)
            lineTo(17f, 15.59f)
            close()
        }
    }.build()
}

internal val HelpIcon: ImageVector by lazy {
    ImageVector.Builder("Help", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
            reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f)
            close()
            moveTo(13f, 19f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(2f)
            close()
            moveTo(15.07f, 11.25f)
            lineToRelative(-0.9f, 0.92f)
            curveTo(13.45f, 12.9f, 13f, 13.5f, 13f, 15f)
            horizontalLineToRelative(-2f)
            verticalLineToRelative(-0.5f)
            curveToRelative(0f, -1.1f, 0.45f, -2.1f, 1.17f, -2.83f)
            lineToRelative(1.24f, -1.26f)
            curveToRelative(0.37f, -0.36f, 0.59f, -0.86f, 0.59f, -1.41f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
            lineTo(8f, 9f)
            curveToRelative(0f, -2.21f, 1.79f, -4f, 4f, -4f)
            reflectiveCurveToRelative(4f, 1.79f, 4f, 4f)
            curveToRelative(0f, 0.88f, -0.36f, 1.68f, -0.93f, 2.25f)
            close()
        }
    }.build()
}

/**
 * 후보 비교 한 행(DESIGN.md 7절 CompareRow, 정차 중 전용). 열 폭을 weight로 고정해 행끼리 열이 맞는다.
 * 선택된 행은 accent 면 + 체크 원 아이콘으로 구분한다(색만으로 구분하지 않음, 문구 없음). 행 전체가 ≥76dp 터치 대상.
 */
@Composable
fun CompareRow(
    name: String,
    minutes: String,
    distance: String,
    status: Status,
    statusText: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = App.colors
    val fg = if (selected) c.accentOn else c.textPrimary
    val sub = if (selected) c.accentOn else c.textSecondary
    val reduce = AppMotion.reduce
    val rowBg by animateColorAsState(
        if (selected) c.accentPrimary else c.bgSurface,
        AppMotion.spec(reduce, AppMotion.MOVE_MS, AppMotion.EaseMove),
        label = "rowBg"
    )
    Row(
        modifier
            .heightIn(min = App.touchMin)
            .clip(RoundedCornerShape(App.radius.md))
            .background(rowBg)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = App.space.lg, vertical = App.space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.md)
    ) {
        Text(name, style = App.type.stopTitle, color = fg, modifier = Modifier.weight(COL_NAME))
        Text(minutes, style = App.type.stopBody, color = sub, modifier = Modifier.weight(COL_NUM))
        Text(distance, style = App.type.stopBody, color = sub, modifier = Modifier.weight(COL_NUM))
        Box(Modifier.weight(COL_CHIP)) { StatusChip(status, statusText) }
        Box(Modifier.weight(COL_PICK), contentAlignment = Alignment.CenterEnd) {
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(App.icon.secondary)
                )
            }
        }
    }
}

private const val COL_NAME = 2.2f
private const val COL_NUM = 1f
private const val COL_CHIP = 3f
private const val COL_PICK = 0.5f
