package com.qme.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qme.mobile.data.api.RetrofitClient
import com.qme.mobile.navigation.AppNavigation
import com.qme.mobile.ui.theme.QMeTheme
import com.qme.mobile.util.SessionManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val session = SessionManager(applicationContext)
        val api     = RetrofitClient.create(session)

        setContent {
            QMeTheme {
                AppNavigation(api = api, session = session)
            }
        }
    }
}
