package com.example.tubealarm

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.tubealarm.ui.theme.TubeAlarmTheme

// Android 17 (API 37) and later block apps from the local network unless the user allows it,
// and the emulator reaches the backend on the development machine through the local network
private const val LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

class MainActivity : ComponentActivity() {
    private val askForLocalNetwork =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED) {
            askForLocalNetwork.launch(LOCAL_NETWORK)
        }
        enableEdgeToEdge()
        setContent {
            TubeAlarmTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    JourneyScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
