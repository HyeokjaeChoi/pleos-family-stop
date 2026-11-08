package ai.familystop

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue

/** Writes the node as `files/shots/<name>.png` in the app's private dir (pulled by scripts/capture-drive-states.sh). */
fun SemanticsNodeInteraction.saveShot(name: String) {
    val bmp = captureToImage().asAndroidBitmap()
    val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "shots")
        .apply { mkdirs() }
    val f = File(dir, "$name.png")
    f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    assertTrue(f.length() > 0)
}
