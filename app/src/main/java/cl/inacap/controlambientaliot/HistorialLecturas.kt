package cl.inacap.controlambientaliot


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun HistorialLecturas(
    temperatura: Float?,
    luminosidad: Float?
) {
    val context = LocalContext.current

    val db = remember {
        LecturasDB(context.applicationContext)
    }

    val temperaturaActual by rememberUpdatedState(temperatura)
    val luzActual by rememberUpdatedState(luminosidad)

    var registros by remember {
        mutableStateOf<List<LecturaAmbiental>>(emptyList())
    }

    var mensaje by remember {
        mutableStateOf("Esperando mediciones...")
    }

    var mostrarHistorial by remember {
        mutableStateOf(false)
    }

    DisposableEffect(db) {
        onDispose {
            db.close()
        }
    }

    LaunchedEffect(db) {
        while (true) {
            val temp = temperaturaActual
            val luz = luzActual

            try {
                val resultado = withContext(Dispatchers.IO) {
                    val guardado = if (
                        temp != null || luz != null
                    ) {
                        db.guardarLectura(temp, luz)
                    } else {
                        false
                    }

                    Pair(guardado, db.obtenerUltimas(10))
                }

                registros = resultado.second

                mensaje = if (resultado.first) {
                    "Medición guardada correctamente"
                } else if (temp == null && luz == null) {
                    "Esperando datos de los sensores"
                } else {
                    "No se pudo guardar la medición"
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mensaje = "Error al consultar el historial"
            }

            delay(30000)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider()

        Text(
            "HISTORIAL DE MEDICIONES",
            style = MaterialTheme.typography.titleLarge
        )

        Text("Registro automático cada 30 segundos")

        Text(mensaje)

        Button(
            onClick = {
                mostrarHistorial = !mostrarHistorial
            }
        ) {
            Text(
                if (mostrarHistorial) {
                    "Ocultar historial"
                } else {
                    "Ver historial"
                }
            )
        }

        if (mostrarHistorial) {
            if (registros.isEmpty()) {
                Text("Todavía no hay mediciones guardadas")
            }

            registros.forEach { lectura ->
                val fecha = DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,
                    DateFormat.MEDIUM
                ).format(Date(lectura.fecha))

                Text("Fecha: $fecha")

                Text(
                    "Temperatura: " +
                            (lectura.temperatura?.let {
                                "%.1f °C".format(it)
                            } ?: "Sin datos")
                )

                Text(
                    "Luminosidad: " +
                            (lectura.luminosidad?.let {
                                "%.1f lux".format(it)
                            } ?: "Sin datos")
                )

                HorizontalDivider()
            }
        }
    }
}
