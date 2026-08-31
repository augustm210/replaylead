package com.replaylead.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.replaylead.app.ui.ReplayLeadApp
import com.replaylead.app.ui.theme.ReplayLeadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReplayLeadTheme {
                ReplayLeadApp()
            }
        }
    }
}
