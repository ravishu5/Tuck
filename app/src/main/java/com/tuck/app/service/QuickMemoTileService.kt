package com.tuck.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tuck.app.R
import com.tuck.app.processing.QuickRecordController
import com.tuck.app.ui.quickrecord.QuickRecordActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Two taps from a locked phone to a running memo: swipe down, tap the tile, talk.
 *
 * The tile does not record anything itself. It opens [QuickRecordActivity], which is the one
 * entry point every quick-access route goes through — so a memo started here behaves exactly
 * like one started from the launcher shortcut or from inside the app.
 *
 * Its label follows the recorder's real state rather than a flag of its own, so a tile tapped
 * twice reads "Recording…" the second time instead of pretending to start a new memo.
 */
@AndroidEntryPoint
class QuickMemoTileService : TileService() {

    @Inject lateinit var controller: QuickRecordController

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        // Only while the tile is on screen: a collapsed shade is not worth a live collector.
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { active ->
            controller.state
                .onEach { render(it) }
                .launchIn(active)
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    private fun render(state: QuickRecordController.State) {
        val tile = qsTile ?: return
        val recording = state is QuickRecordController.State.Recording
        tile.state = if (recording) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(
            if (recording) R.string.tile_quick_memo_recording else R.string.tile_quick_memo
        )
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, QuickRecordActivity::class.java).apply {
            action = QuickRecordActivity.ACTION_QUICK_RECORD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        // Starting the activity is what begins the recording — the tile deliberately does not
        // start one itself, so there is no path where a memo is running without a screen that
        // can stop it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        super.onDestroy()
    }
}
