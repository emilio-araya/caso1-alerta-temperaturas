package com.example.caso1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.caso1.ui.theme.AlertaTemperaturasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlertaTemperaturasTheme {
                com.example.caso1.ui.navigation.AppNavigation()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    AlertaTemperaturasTheme {
        Text("Preview")
    }
}
