package ai.familystop

import android.app.Application

class FamilyStopApp : Application() {
    val graph by lazy { AppGraph(this) }
}
