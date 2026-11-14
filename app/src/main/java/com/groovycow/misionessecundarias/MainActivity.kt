package com.groovycow.misionessecundarias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.groovycow.misionessecundarias.ui.navigation.AppNavigation
import com.groovycow.misionessecundarias.ui.theme.MisionesSecundariasTheme

/** Actividad única de la aplicación; delega toda la navegación en [AppNavigation]. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MisionesSecundariasTheme {
                AppNavigation()
            }
        }
    }
}
