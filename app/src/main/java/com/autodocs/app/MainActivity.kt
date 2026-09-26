package com.autodocs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.autodocs.app.ui.navigation.AutoDocsNavHost
import com.autodocs.app.ui.theme.AutoDocsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutoDocsTheme {
                AutoDocsNavHost()
            }
        }
    }
}
