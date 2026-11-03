package com.example.mikardex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.mikardex.ui.theme.MiKardexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiKardexTheme {
                var pantalla by remember { mutableStateOf("inicio") }

                // El botón "atrás" del celular regresa al inicio
                BackHandler(enabled = pantalla != "inicio") { pantalla = "inicio" }

                Scaffold { innerPadding ->
                    Box(Modifier.padding(innerPadding)) {
                        when (pantalla) {
                            "registrar" -> PantallaRegistrar { pantalla = "inicio" }
                            "historial" -> PantallaHistorial { pantalla = "inicio" }
                            "promedios" -> PantallaPromedios { pantalla = "inicio" }
                            else -> PantallaInicio(
                                onRegistrar = { pantalla = "registrar" },
                                onHistorial = { pantalla = "historial" },
                                onPromedios = { pantalla = "promedios" }
                            )
                        }
                    }
                }
            }
        }
    }
}