package ai.familystop.core.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val WHEEL_ROWS = 5
private const val WHEEL_CENTER = WHEEL_ROWS / 2
private const val REPEAT_DELAY_MS = 400L
private const val REPEAT_EVERY_MS = 120L

/**
 * 시각 휠: 오전/오후(탭 토글) · 시(1-12) · 분(0-59, 1분 단위) 3열. 열마다 스냅 스크롤, 행 76dp x 5행,
 * 가운데 행이 `bg/raised` 선택 막대 위에 놓인다. 이웃 행을 탭하면 그 행으로 이동한다.
 * [hour]는 0-23, 값은 스크롤이 멈춘 뒤에만 [onChange]로 올라간다. 선택은 막대 위치(면 단계)로 드러낸다.
 */
@Composable
fun TimeWheel(
    hour: Int,
    minute: Int,
    onChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pm = hour >= 12
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    val rowH = App.touchMin
    val update by rememberUpdatedState(onChange)
    Box(modifier.height(rowH * WHEEL_ROWS), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(rowH)
                .clip(RoundedCornerShape(App.radius.md))
                .background(App.colors.bgRaised)
        )
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(App.space.md)) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(role = Role.Button) { update((hour + 12) % 24, minute) }
                    // 현재 쪽 행이 가운데(3번째) 선택 막대 위에 오도록 내린다.
                    .padding(top = if (pm) rowH else rowH * 2),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 오전·오후를 가운데 막대 위아래로 쌓아 현재 쪽만 막대 위에 둔다.
                listOf("오전" to !pm, "오후" to pm).forEach { (text, selected) ->
                    Box(Modifier.height(rowH).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        WheelLabel(text, selected)
                    }
                }
            }
            WheelColumn(
                count = 12,
                index = h12 - 1,
                label = { (it + 1).toString() },
                onIndex = { update((it + 1) % 12 + if (pm) 12 else 0, minute) },
                columnName = "시",
                modifier = Modifier.weight(1f)
            )
            WheelColumn(
                count = 60,
                index = minute,
                label = { "%02d".format(it) },
                onIndex = { update(hour, it) },
                columnName = "분",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WheelLabel(
    text: String,
    selected: Boolean,
    alpha: Float = if (selected) 1f else 0.55f
) {
    Text(
        text,
        style = if (selected) App.type.driveTitle else App.type.driveBody,
        color = App.colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(alpha)
    )
}

// 유한 목록 + 위아래 2행 패딩. 가운데 행 index = firstVisibleItemIndex(스냅이 시작 위치에 맞춘다).
// ponytail: 유한 목록(무한 루프 아님). 59→0 연속 스크롤이 필요하면 가상 index로 바꾼다.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    count: Int,
    index: Int,
    label: (Int) -> String,
    onIndex: (Int) -> Unit,
    columnName: String,
    modifier: Modifier = Modifier
) {
    val rowH = App.touchMin
    val state = rememberLazyListState(initialFirstVisibleItemIndex = index)
    val scope = rememberCoroutineScope()
    val update by rememberUpdatedState(onIndex)
    val center by remember {
        derivedStateOf {
            val half = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.div(2) ?: 0
            val raw = state.firstVisibleItemIndex +
                if (state.firstVisibleItemScrollOffset > half) 1 else 0
            raw.coerceIn(0, count - 1)
        }
    }
    LaunchedEffect(index) {
        if (!state.isScrollInProgress && center != index) state.scrollToItem(index)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress to center }.collect { (moving, c) ->
            if (!moving && c != index) update(c)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().semantics {
            // 스크롤 제스처 없이도 TalkBack으로 값을 바꿀 수 있게 현재 값과 증감 동작을 알린다.
            contentDescription = "$columnName ${label(index)}"
            customActions = listOf(
                CustomAccessibilityAction("$columnName 늘리기") {
                    if (index < count - 1) update(index + 1)
                    true
                },
                CustomAccessibilityAction("$columnName 줄이기") {
                    if (index > 0) update(index - 1)
                    true
                }
            )
        },
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        contentPadding = PaddingValues(vertical = rowH * WHEEL_CENTER)
    ) {
        items(count) { i ->
            val a = when (abs(i - center)) {
                0 -> 1f
                1 -> 0.55f
                else -> 0.25f
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(rowH)
                    .clickable { scope.launch { state.animateScrollToItem(i) } },
                contentAlignment = Alignment.Center
            ) {
                WheelLabel(label(i), selected = i == center, alpha = a)
            }
        }
    }
}

/**
 * 값 양옆 −/+ 버튼(76dp 정사각, `bg/raised`). 버튼을 400ms 누르고 있으면 120ms마다 반복 호출한다.
 * 아이콘만 있는 버튼이므로 [minusLabel]/[plusLabel]이 접근성 이름이다.
 */
@Composable
fun Stepper(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusLabel: String = "줄이기",
    plusLabel: String = "늘리기"
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepButton(AppIcons.Remove, minusLabel, onMinus)
        Text(
            value,
            style = App.type.driveTitle,
            color = App.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = App.space.lg)
        )
        StepButton(Icons.Rounded.Add, plusLabel, onPlus)
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, onStep: () -> Unit) {
    val step by rememberUpdatedState(onStep)
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(App.touchMin)
            .clip(RoundedCornerShape(App.radius.md))
            .background(App.colors.bgRaised)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                onClick {
                    step()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    var repeating = false
                    val job = scope.launch {
                        delay(REPEAT_DELAY_MS)
                        repeating = true
                        while (true) {
                            step()
                            delay(REPEAT_EVERY_MS)
                        }
                    }
                    val released = tryAwaitRelease()
                    job.cancel()
                    if (released && !repeating) step()
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = App.colors.textPrimary,
            modifier = Modifier.size(App.icon.secondary)
        )
    }
}

/**
 * 체크 행(>=96dp, `bg/raised`). 완료는 왼쪽 48dp 원(accent 채움 + 체크)과 취소선 + 보조색 글자로 표시한다.
 * 문구로 상태를 쓰지 않는다. 접근성은 [Role.Checkbox] toggleable.
 */
@Composable
fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = App.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(App.radius.md))
            .background(c.bgRaised)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = App.space.lg, vertical = App.space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.lg)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (checked) c.accentPrimary else c.bgSurface),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Rounded.Check,
                    null,
                    tint = c.accentOn,
                    modifier = Modifier.size(App.icon.tertiary)
                )
            }
        }
        Text(
            label,
            style = App.type.stopTitle,
            color = if (checked) c.textSecondary else c.textPrimary,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
    }
}

/** 진행 막대: 16dp 높이, 반경 8dp, 트랙 `bg/raised`, 채움 accent. 값은 0..1로 자른다. */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val f = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(App.radius.sm))
            .background(App.colors.bgRaised)
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .fillMaxSize()
                .background(App.colors.accentPrimary)
        )
    }
}

/**
 * 선택 행(>=96dp): 제목 + 선택적 부제. 선택되면 accent 옅은 면 + 오른쪽 check_circle 아이콘(문구 아님).
 * 접근성은 [Role.RadioButton] selectable.
 */
@Composable
fun SelectRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = App.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(App.radius.md))
            .background(if (selected) c.accentPrimary.copy(alpha = 0.16f) else c.bgRaised)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = App.space.lg, vertical = App.space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(App.space.lg)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = App.type.stopTitle, color = c.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = App.type.stopBody, color = c.textSecondary)
            }
        }
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                null,
                tint = c.accentPrimary,
                modifier = Modifier.size(App.icon.primary)
            )
        }
    }
}
