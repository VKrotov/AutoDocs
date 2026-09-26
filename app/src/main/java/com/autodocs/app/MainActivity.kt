package com.autodocs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.autodocs.app.ui.navigation.AutoDocsNavHost
import com.autodocs.app.ui.theme.AutoDocsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Застосунок лише темний: іконки статус-бару завжди світлі, незалежно від теми телефона.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AutoDocsTheme {
                AutoDocsNavHost()
            }
        }
    }
}
