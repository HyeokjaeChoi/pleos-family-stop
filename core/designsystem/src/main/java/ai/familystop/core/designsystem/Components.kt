package ai.familystop.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** 상태 4종. 아이콘 + 문구 + 색 3중으로 표시하고 색만으로 구분하지 않는다(DESIGN.md 6절). */
enum class Status { Ok, Warn, Danger, Unknown }

/** 상태 칩: `bg/raised` 면 + 상태색 아이콘 24dp + 본문색 문구. 아이콘만 단독으로 쓰지 않는다. */
@Composable
fun StatusChip(status: Status, text: String, modifier: Modifier = Modifier) {
    val c = App.colors
    val (icon, tint) = when (status) {
        Status.Ok -> Icons.Rounded.CheckCircle to c.statusOk
        Status.Warn -> Icons.Rounded.Warning to c.statusWarn
        Status.Danger -> CancelIcon to c.statusDanger
        Status.Unknown -> HelpIcon to c.statusUnknown
    }
    Row(
        modifier
            .clip(RoundedCornerShape(App.radius.md))
            .background(c.bgRaised)
            .padding(horizontal = App.space.md, vertical = App.space.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.sm)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(App.icon.tertiary)
        )
        Text(text, style = App.type.stopBody, color = c.textPrimary)
    }
}

/** 카드: 외곽선 없이 `bg/surface` 면. [onClick]이 있으면 카드 전체가 터치 대상. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(App.radius.lg))
            .background(App.colors.bgSurface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(App.space.lg),
        verticalArrangement = Arrangement.spacedBy(App.space.md),
        content = content
    )
}

/** 큰 버튼: 높이 ≥ 76dp. [large]는 주행 중 글자(drive/body), 아니면 stop/body. */
@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    large: Boolean = false
) {
    val c = App.colors
    val bg = if (primary && enabled) c.accentPrimary else c.bgRaised
    val fg = when {
        !enabled -> c.textSecondary
        primary -> c.accentOn
        else -> c.textPrimary
    }
    Box(
        modifier
            .heightIn(min = App.touchMin)
            .clip(RoundedCornerShape(App.radius.md))
            .background(bg)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = App.space.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = if (large) App.type.driveBody else App.type.stopBody, color = fg)
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
            .toggleable(selected, role = role, onValueChange = { onClick() })
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

private val HelpIcon: ImageVector by lazy {
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
