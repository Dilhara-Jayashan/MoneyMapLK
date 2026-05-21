package com.example.moneymaplk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.moneymaplk.core.navigation.MoneyMapNavGraph
import com.example.moneymaplk.core.theme.MoneyMapLKTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyMapLKTheme {
                MoneyMapNavGraph()
            }
        }
    }
}
