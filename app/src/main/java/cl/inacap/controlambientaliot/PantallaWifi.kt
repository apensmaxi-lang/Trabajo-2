
package cl.inacap.controlambientaliot

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun PantallaWifi(
    temperatura: Float?,
    luminosidad: Float?
) {
    val context = LocalContext.current
    val conexion = remember { ConexionWifi() }
    val scope = rememberCoroutineScope()

    var ipDestino by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("Sin conexión") }
    var datosRecibidos by remember {
        mutableStateOf("Todavía no se reciben datos")
    }
    var escuchando by remember { mutableStateOf(false) }

    val permiso = "android.permission.ACCESS_LOCAL_NETWORK"

    var permisoRed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 37 ||
                    ContextCompat.checkSelfPermission(
                        context,
                        permiso
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoRed = concedido
        if (!concedido) {
            estado = "Permiso de red local denegado"
        }
    }

    DisposableEffect(conexion) {
        onDispose {
            conexion.detenerRecepcion()
        }
    }

    LaunchedEffect(escuchando, permisoRed) {
        if (escuchando && permisoRed) {
            try {
                estado = "Esperando datos en puerto 5050"

                conexion.recibir { mensaje ->
                    datosRecibidos = mensaje
                    estado = "Datos recibidos correctamente"
                }
            } catch (e: Exception) {
                if (escuchando) {
                    estado = "Error de recepción"
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider()

        Text(
            "CONEXIÓN WI-FI",
            style = MaterialTheme.typography.titleLarge
        )

        if (!permisoRed) {
            Button(
                onClick = {
                    solicitarPermiso.launch(permiso)
                }
            ) {
                Text("Permitir acceso a red local")
            }
        }

        Text("RECEPCIÓN DE DATOS")

        Button(
            enabled = permisoRed,
            onClick = {
                if (escuchando) {
                    conexion.detenerRecepcion()
                    escuchando = false
                    estado = "Recepción detenida"
                } else {
                    escuchando = true
                }
            }
        ) {
            Text(
                if (escuchando) {
                    "Detener recepción"
                } else {
                    "Comenzar a recibir"
                }
            )
        }

        Text("Último mensaje recibido:")
        Text(datosRecibidos)

        HorizontalDivider()

        Text("ENVÍO DE DATOS")

        OutlinedTextField(
            value = ipDestino,
            onValueChange = { ipDestino = it },
            label = { Text("IP del otro celular") },
            placeholder = { Text("192.168.1.100") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            enabled = permisoRed && ipDestino.isNotBlank(),
            onClick = {
                scope.launch {
                    estado = "Enviando datos..."

                    val enviado = conexion.enviar(
                        ipDestino,
                        temperatura,
                        luminosidad
                    )

                    estado = if (enviado) {
                        "Datos enviados correctamente"
                    } else {
                        "Error: revisa la IP y la conexión Wi-Fi"
                    }
                }
            }
        ) {
            Text("Enviar mediciones")
        }

        Text("Estado: $estado")
    }
}
