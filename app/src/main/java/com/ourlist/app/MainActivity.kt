package com.ourlist.app

import android.Manifest
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ourlist.app.ui.navigation.AppNavGraph
import com.ourlist.app.ui.theme.OurListTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val requestedItem = MutableStateFlow<String?>(null)
    private fun handleItemIntent(intent: Intent?) {
        requestedItem.value = intent?.getStringExtra("navigate_to_item_id")?.takeIf { it.length <= 128 && it.matches(Regex("[A-Za-z0-9_-]+")) }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleItemIntent(intent)
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        val app = application as OurListApp
        handleItemIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    app.syncEngine.sync()
                    delay(10_000)
                }
            }
        }
        setContent {
            OurListTheme {
                val itemId by requestedItem.collectAsState()
                AppNavGraph(app = app, openItemId = itemId, onOpenItemHandled = { requestedItem.value = null })
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
