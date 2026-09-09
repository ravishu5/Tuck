package com.tuck.app.ui

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.tuck.app.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    /**
     * Whether the saved theme is known yet.
     *
     * Settings live in DataStore and arrive asynchronously, so the first composition used to
     * run against the defaults — meaning anyone not on Linen watched the whole app draw in the
     * wrong palette and then flip to theirs. That flash is what "it renders when you open it"
     * looks like.
     */
    @Volatile
    private var themeReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        holdFirstFrameUntilThemeIsKnown()

        lifecycleScope.launch {
            // Bounded: a read that never returns must not leave a permanently blank window.
            // The defaults are a worse first frame than the real theme, but they are a far
            // better one than nothing at all.
            withTimeoutOrNull(THEME_WAIT_MS) { settingsRepository.getSettings().first() }
            themeReady = true
        }

        val openItemId = intent.getLongExtra("open_item_id", -1L).takeIf { it > 0 }

        setContent {
            TuckApp(initialOpenItemId = openItemId)
        }
    }

    /**
     * Keeps the launch window up until [themeReady].
     *
     * The system is already showing the app's launch background, so this shows no more than a
     * splash would — it just avoids drawing a frame that is about to be replaced.
     */
    private fun holdFirstFrameUntilThemeIsKnown() {
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!themeReady) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            }
        )
    }

    private companion object {
        const val THEME_WAIT_MS = 700L
    }
}
