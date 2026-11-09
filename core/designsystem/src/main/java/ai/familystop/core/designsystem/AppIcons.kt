package ai.familystop.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun glyph(name: String, d: String, fillType: PathFillType = PathFillType.NonZero) =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(addPathNodes(d), pathFillType = fillType, fill = SolidColor(Color.Black))
    }.build()

/** DESIGN.md 6절 아이콘 이름 매핑 중 `material-icons-core`에 없는 것(Material Symbols Fill 1). 색은 호출처 tint. */
object AppIcons {
    /** `add_location_alt`: 핀 + 더하기(더하기는 even-odd로 뚫음). */
    val AddLocationAlt: ImageVector by lazy {
        glyph(
            "AddLocationAlt",
            "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" +
                "M11 6h2v2.5h2.5v2H13V13h-2v-2.5H8.5v-2H11z",
            PathFillType.EvenOdd
        )
    }
    val EvStation: ImageVector by lazy {
        glyph(
            "EvStation",
            "M19.77 7.23l.01-.01-3.72-3.72L15 4.56l2.11 2.11c-.94.36-1.61 1.26-1.61 2.33 0 1." +
                "38 1.12 " +
                "2.5 2.5 2.5.36 0 .69-.08 1-.21v7.21c0 .55-.45 1-1 1s-1-.45-1-1V14c0-1.1-.9-2-2-2" +
                "h-1V5c0" +
                "-1.1-.9-2-2-2H6c-1.1 0-2 .9-2 2v16h10v-7.5h1.5v5c0 1.38 1.12 2.5 2.5 2.5s2.5-1.1" +
                "2 2.5-2.5" +
                "V9c0-.69-.28-1.32-.73-1.77zM18 10c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1" +
                "zM8 18v" +
                "-4.5H6L10 6v5h2l-4 7z"
        )
    }
    val Undo: ImageVector by lazy {
        glyph(
            "Undo",
            "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88" +
                " 3.54 0 " +
                "6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"
        )
    }
    val VolumeUp: ImageVector by lazy {
        glyph(
            "VolumeUp",
            "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2." +
                "5-4.02z" +
                "M14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.7" +
                "7s-2.99" +
                "-7.86-7-8.77z"
        )
    }
    val Remove: ImageVector by lazy { glyph("Remove", "M19 13H5v-2h14v2z") }
    val Navigation: ImageVector by lazy {
        glyph("Navigation", "M12 2L4.5 20.29l.71.71L12 18l6.79 3 .71-.71z")
    }
    val Bolt: ImageVector by lazy {
        glyph(
            "Bolt",
            "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12" +
                "C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51" +
                "l-.07.15C12.96 17.55 11 21 11 21z"
        )
    }
    val Place: ImageVector by lazy {
        glyph(
            "Place",
            "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5" +
                "c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"
        )
    }
}
