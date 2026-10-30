
package cl.inacap.controlambientaliot

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            MaterialTheme {
                var usuarioActual by remember {
                    mutableStateOf<String?>(null)
                }

                if (usuarioActual == null) {
                    PantallaLogin(
                        onLoginExitoso = { usuario ->
                            usuarioActual = usuario
                        }
                    )

                } else {
                    PantallaPrincipal(
                        onCerrarSesion = {
                            usuarioActual = null
                        }
                    )
                }

            }
        }

    }
}

@Composable

fun PantallaPrincipal(onCerrarSesion: () -> Unit) {

    val context = LocalContext.current

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE)
                as SensorManager
    }

    val sensorLuz = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    var luminosidad by remember {
        mutableStateOf<Float?>(null)
    }

    DisposableEffect(sensorManager, sensorLuz) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                luminosidad = event.values[0]
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int
            ) {}
        }

        if (sensorLuz != null) {
            sensorManager.registerListener(
                listener,
                sensorLuz,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "CONTROL AMBIENTAL IoT",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Sistema inteligente de monitoreo y control",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Sensores y actuadores en tiempo real",
                    style = MaterialTheme.typography.bodySmall
                )

                TextButton(
                    onClick = { onCerrarSesion() }
                ) {
                    Text("Cerrar sesión")
                }
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

        Text("MONITOREO AMBIENTAL")

        Spacer(modifier = Modifier.height(16.dp))

        Text("Sensor 1: Luminosidad")

        Text(
            when {
                sensorLuz == null ->
                    "Sensor no disponible"

                luminosidad == null ->
                    "Esperando lectura..."

                else ->
                    "Luz: ${luminosidad!!.toInt()} lux"
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Sensor 2: Temperatura de batería")


        var temperatura by remember {
            mutableStateOf<Float?>(null)
        }

        LaunchedEffect(context) {
            while (true) {
                val bateria = context.registerReceiver(
                    null,
                    android.content.IntentFilter(
                        android.content.Intent.ACTION_BATTERY_CHANGED
                    )
                )

                val valor = bateria?.getIntExtra(
                    android.os.BatteryManager.EXTRA_TEMPERATURE,
                    Int.MIN_VALUE
                ) ?: Int.MIN_VALUE

                temperatura = if (valor != Int.MIN_VALUE) {
                    valor / 10f
                } else {
                    null
                }

                kotlinx.coroutines.delay(2000)
            }
        }

        Text(
            text = temperatura?.let {
                "Temperatura: %.1f °C".format(it)
            } ?: "Temperatura no disponible"
        )

        Text("Actualización automática cada 2 segundos")


        Spacer(modifier = Modifier.height(24.dp))

        Text("ACTUADOR 1: ALARMA SONORA")

        var limite by remember {
            mutableFloatStateOf(36f)
        }

        var alarmaActivada by remember {
            mutableStateOf(true)
        }

        val sonido = remember {
            android.media.ToneGenerator(
                android.media.AudioManager.STREAM_MUSIC,
                90
            )
        }

        DisposableEffect(sonido) {
            onDispose {
                sonido.release()
            }
        }

        Text("Límite: %.1f °C".format(limite))

        Slider(
            value = limite,
            onValueChange = { limite = it },
            valueRange = 30f..45f
        )

        Row {
            Text("Alarma automática")
            Switch(
                checked = alarmaActivada,
                onCheckedChange = {
                    alarmaActivada = it
                }
            )
        }

        val temperaturaAlta = temperatura?.let {
            it >= limite
        } == true


        LaunchedEffect(temperaturaAlta, alarmaActivada) {
            try {
                while (temperaturaAlta && alarmaActivada) {
                    sonido.startTone(
                        android.media.ToneGenerator.TONE_PROP_BEEP,
                        600
                    )

                    kotlinx.coroutines.delay(800)
                }
            } finally {
                sonido.stopTone()
            }
        }


        Text(
            if (temperaturaAlta) {
                "Estado: Temperatura sobre el límite"
            } else {
                "Estado: Temperatura normal"
            }
        )

        Button(
            onClick = {
                sonido.startTone(
                    android.media.ToneGenerator.TONE_PROP_BEEP,
                    1000
                )
            }
        ) {
            Text("Probar alarma")
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Text(
            "ACTUADOR 2: LINTERNA AUTOMÁTICA",
            style = MaterialTheme.typography.titleMedium
        )

        val camaraManager = remember {
            context.getSystemService(Context.CAMERA_SERVICE)
                    as android.hardware.camera2.CameraManager
        }

        val idFlash = remember {
            runCatching {
                camaraManager.cameraIdList.firstOrNull { id ->
                    camaraManager.getCameraCharacteristics(id).get(
                        android.hardware.camera2.CameraCharacteristics
                            .FLASH_INFO_AVAILABLE
                    ) == true
                }
            }.getOrNull()
        }

        var linternaAuto by remember {
            mutableStateOf(false)
        }

        var permisoCamara by remember {
            mutableStateOf(
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )
        }

        val pedirPermiso = androidx.activity.compose
            .rememberLauncherForActivityResult(
                androidx.activity.result.contract
                    .ActivityResultContracts.RequestPermission()
            ) { permitido ->
                permisoCamara = permitido
                if (permitido) linternaAuto = true
            }

        var limiteLuz by remember {
            mutableFloatStateOf(30f)
        }

        var estadoLinterna by remember {
            mutableStateOf("Apagada")
        }

        val hayOscuridad = luminosidad?.let {
            it < limiteLuz
        } == true

        LaunchedEffect(
            hayOscuridad,
            linternaAuto,
            permisoCamara,
            idFlash
        ) {
            if (idFlash != null && permisoCamara) {
                val encender = linternaAuto && hayOscuridad

                runCatching {
                    camaraManager.setTorchMode(idFlash, encender)
                }.onSuccess {
                    estadoLinterna = if (encender) {
                        "Encendida"
                    } else {
                        "Apagada"
                    }
                }.onFailure {
                    estadoLinterna = "Error al controlar la linterna"
                }
            }
        }

        DisposableEffect(camaraManager, idFlash) {
            onDispose {
                if (idFlash != null) {
                    runCatching {
                        camaraManager.setTorchMode(idFlash, false)
                    }
                }
            }
        }

        Text("Límite de oscuridad: ${limiteLuz.toInt()} lux")

        Slider(
            value = limiteLuz,
            onValueChange = { limiteLuz = it },
            valueRange = 5f..200f
        )

        Row {
            Text("Linterna automática")

            Switch(
                checked = linternaAuto,
                onCheckedChange = { activar ->
                    if (activar && !permisoCamara) {
                        pedirPermiso.launch(
                            android.Manifest.permission.CAMERA
                        )
                    } else {
                        linternaAuto = activar
                    }
                }
            )
        }

        Text("Estado de linterna: $estadoLinterna")

        Text(
            if (hayOscuridad) {
                "Ambiente oscuro"
            } else {
                "Iluminación suficiente"
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        PantallaWifi(
            temperatura = temperatura,
            luminosidad = luminosidad
        )

        Spacer(modifier = Modifier.height(24.dp))

        HistorialLecturas(
            temperatura = temperatura,
            luminosidad = luminosidad
        )




    }
}
