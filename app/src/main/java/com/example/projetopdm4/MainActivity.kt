package com.example.projetopdm4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.projetopdm4.ui.screens.FinanceApp
import com.example.projetopdm4.ui.theme.ProjetoPDM4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjetoPDM4Theme {
                FinanceApp()
            }
        }
    }
}
