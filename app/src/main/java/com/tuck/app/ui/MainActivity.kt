package com.tuck.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openItemId = intent.getLongExtra("open_item_id", -1L).takeIf { it > 0 }

        setContent {
            TuckApp(initialOpenItemId = openItemId)
        }
    }
}
