package com.example.mikardex

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Utilidades ----------
@Composable
fun Campo(
    valor: String,
    onChange: (String) -> Unit,
    etiqueta: String,
    numerico: Boolean = false,
    editable: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(etiqueta) },
        singleLine = true,
        readOnly = !editable,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numerico) KeyboardType.Decimal else KeyboardType.Text
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

fun txt(v: Double?): String =
    v?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""

// ---------- Pantalla principal (Shared Preferences) ----------
@Composable
fun PantallaInicio(onRegistrar: () -> Unit, onHistorial: () -> Unit, onPromedios: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("alumno", Context.MODE_PRIVATE) }

    var nombre by remember { mutableStateOf(prefs.getString("nombre", "") ?: "") }
    var control by remember { mutableStateOf(prefs.getString("control", "") ?: "") }
    // Si ya hay datos guardados, los campos quedan bloqueados
    var guardado by remember {
        mutableStateOf(
            !prefs.getString("nombre", "").isNullOrBlank() &&
                    !prefs.getString("control", "").isNullOrBlank()
        )
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Mi Cardex", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Campo(nombre, { nombre = it }, "Nombre del alumno", editable = !guardado)
        Campo(control, { control = it }, "Número de control", editable = !guardado)

        if (!guardado) {
            Button(
                onClick = {
                    if (nombre.isBlank() || control.isBlank()) {
                        Toast.makeText(context, "Completa nombre y número de control", Toast.LENGTH_SHORT).show()
                    } else {
                        prefs.edit()
                            .putString("nombre", nombre.trim())
                            .putString("control", control.trim())
                            .commit()
                        nombre = nombre.trim()
                        control = control.trim()
                        guardado = true
                        Toast.makeText(context, "Datos guardados", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar mis datos") }
        } else {
            Text("Datos guardados (no editables)", modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onRegistrar, modifier = Modifier.fillMaxWidth()) { Text("Registrar materia") }
        Button(onClick = onHistorial, modifier = Modifier.fillMaxWidth()) { Text("Ver historial") }
        Button(onClick = onPromedios, modifier = Modifier.fillMaxWidth()) { Text("Promedios") }
    }
}

// ---------- Registrar materia ----------
@Composable
fun PantallaRegistrar(onVolver: () -> Unit) {
    val context = LocalContext.current
    var materia by remember { mutableStateOf("") }
    var semestre by remember { mutableStateOf("") }
    var normal by remember { mutableStateOf("") }
    var rep by remember { mutableStateOf("") }
    var esp by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Registrar materia", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Campo(materia, { materia = it }, "Nombre de la materia")
        Campo(semestre, { semestre = it }, "Semestre", numerico = true)
        Campo(normal, { normal = it }, "Calificación Normal", numerico = true)
        Campo(rep, { rep = it }, "Calificación Repetición", numerico = true)
        Campo(esp, { esp = it }, "Calificación Especial", numerico = true)

        Button(
            onClick = {
                val sem = semestre.toIntOrNull()
                val n = normal.toDoubleOrNull()
                val r = rep.toDoubleOrNull()
                val e = esp.toDoubleOrNull()

                if (materia.isBlank() || sem == null) {
                    Toast.makeText(context, "Nombre y semestre son obligatorios", Toast.LENGTH_SHORT).show()
                } else if (listOf(n, r, e).any { it != null && (it < 0 || it > 100) }) {
                    Toast.makeText(context, "Las calificaciones van de 0 a 100", Toast.LENGTH_SHORT).show()
                } else {
                    AppDatabase.get(context).materiaDao()
                        .insertar(Materia(0, materia.trim(), sem, n, r, e))
                    Toast.makeText(context, "Materia guardada", Toast.LENGTH_SHORT).show()
                    // El semestre se conserva para capturar varias materias seguidas
                    materia = ""; normal = ""; rep = ""; esp = ""
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("Guardar") }
    }
}

// ---------- Historial (cuadros por semestre) ----------
fun nota(v: Double?, naSiNulo: Boolean): String = when {
    v != null -> txt(v)
    naSiNulo -> "N/A"
    else -> "-"
}

fun tipoEvaluacion(m: Materia): String = when {
    m.especial != null -> "Curso Especial"
    m.repeticion != null -> "Repetición"
    m.normal != null -> "Evaluación Ordinaria"
    else -> "Sin acreditar"
}

@Composable
fun RowScope.Celda(texto: String, peso: Float, negrita: Boolean = false, izquierda: Boolean = false) {
    Text(
        texto,
        modifier = Modifier.weight(peso),
        fontSize = 12.sp,
        fontWeight = if (negrita) FontWeight.Bold else FontWeight.Normal,
        textAlign = if (izquierda) TextAlign.Start else TextAlign.Center
    )
}

// ---------- Ventana para editar o eliminar ----------
@Composable
fun DialogEditar(
    m: Materia,
    onCerrar: () -> Unit,
    onGuardar: (Materia) -> Unit,
    onEliminar: (Materia) -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(m.nombre) }
    var semestre by remember { mutableStateOf(m.semestre.toString()) }
    var normal by remember { mutableStateOf(txt(m.normal)) }
    var rep by remember { mutableStateOf(txt(m.repeticion)) }
    var esp by remember { mutableStateOf(txt(m.especial)) }
    var confirmar by remember { mutableStateOf(false) }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Eliminar materia") },
            text = { Text("¿Eliminar \"${m.nombre}\"? Esta acción no se puede deshacer.") },
            confirmButton = { TextButton(onClick = { onEliminar(m) }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    } else {
        AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("Editar materia") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Campo(nombre, { nombre = it }, "Nombre de la materia")
                    Campo(semestre, { semestre = it }, "Semestre", numerico = true)
                    Campo(normal, { normal = it }, "Calificación Normal", numerico = true)
                    Campo(rep, { rep = it }, "Calificación Repetición", numerico = true)
                    Campo(esp, { esp = it }, "Calificación Especial", numerico = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sem = semestre.toIntOrNull()
                    val n = normal.toDoubleOrNull()
                    val r = rep.toDoubleOrNull()
                    val e = esp.toDoubleOrNull()
                    if (nombre.isBlank() || sem == null) {
                        Toast.makeText(context, "Nombre y semestre son obligatorios", Toast.LENGTH_SHORT).show()
                    } else if (listOf(n, r, e).any { it != null && (it < 0 || it > 100) }) {
                        Toast.makeText(context, "Las calificaciones van de 0 a 100", Toast.LENGTH_SHORT).show()
                    } else {
                        onGuardar(Materia(m.id, nombre.trim(), sem, n, r, e))
                    }
                }) { Text("Guardar cambios") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmar = true }) { Text("Eliminar") }
                    TextButton(onClick = onCerrar) { Text("Cancelar") }
                }
            }
        )
    }
}

@Composable
fun PantallaHistorial(onVolver: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).materiaDao() }
    var recarga by remember { mutableStateOf(0) }
    var seleccionada by remember { mutableStateOf<Materia?>(null) }

    val porSemestre = remember(recarga) {
        dao.obtenerTodas()
            .groupBy { it.semestre }
            .toSortedMap()
            .toList()
    }

    seleccionada?.let { m ->
        DialogEditar(
            m = m,
            onCerrar = { seleccionada = null },
            onGuardar = { nueva ->
                dao.actualizar(nueva)
                seleccionada = null
                recarga++
                Toast.makeText(context, "Materia actualizada", Toast.LENGTH_SHORT).show()
            },
            onEliminar = { vieja ->
                dao.eliminar(vieja)
                seleccionada = null
                recarga++
                Toast.makeText(context, "Materia eliminada", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Historial", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Toca una materia para editarla o eliminarla", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))

        if (porSemestre.isEmpty()) {
            Text("Sin materias registradas")
        } else {
            LazyColumn {
                items(porSemestre) { (sem, lista) ->
                    val materias = lista.sortedBy { it.id }
                    val finales = materias.mapNotNull { it.calFinal() }
                    val promedio = if (finales.isEmpty()) "N/A"
                    else String.format("%.2f", finales.average())

                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Semestre $sem", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))

                            Row(Modifier.fillMaxWidth()) {
                                Celda("Materia", 2.6f, negrita = true, izquierda = true)
                                Celda("Normal", 1f, negrita = true)
                                Celda("Rep.", 1f, negrita = true)
                                Celda("Esp.", 1f, negrita = true)
                                Celda("Final", 1f, negrita = true)
                            }
                            Spacer(Modifier.height(4.dp))

                            materias.forEachIndexed { i, m ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable { seleccionada = m }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(Modifier.weight(2.6f)) {
                                        Text("${i + 1}. ${m.nombre}", fontSize = 12.sp)
                                        Text(
                                            tipoEvaluacion(m),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Celda(nota(m.normal, naSiNulo = true), 1f)
                                    Celda(nota(m.repeticion, naSiNulo = m.especial != null), 1f)
                                    Celda(nota(m.especial, naSiNulo = false), 1f)
                                    Celda(nota(m.calFinal(), naSiNulo = true), 1f, negrita = true)
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("Promedio semestral: $promedio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Materias: ${materias.size}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------- Promedios ----------
@Composable
fun PantallaPromedios(onVolver: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).materiaDao() }
    val general = remember { dao.promedioGeneral() }
    var semestre by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text(
            if (general == null) "Promedio general: sin materias"
            else "Promedio general: " + String.format("%.2f", general),
            fontSize = 20.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        Campo(semestre, { semestre = it }, "Semestre a consultar", numerico = true)
        Button(
            onClick = {
                val sem = semestre.toIntOrNull()
                resultado = if (sem == null) {
                    "Escribe un semestre válido"
                } else {
                    val p = dao.promedioSemestre(sem)
                    if (p == null) "No hay materias en el semestre $sem"
                    else "Promedio del semestre $sem: " + String.format("%.2f", p)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ver promedio del semestre") }
        Spacer(Modifier.height(16.dp))
        Text(resultado, fontSize = 18.sp)
    }
}