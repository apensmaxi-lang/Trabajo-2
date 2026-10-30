
package cl.inacap.controlambientaliot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaLogin(
    onLoginExitoso: (String) -> Unit
) {
    val context = LocalContext.current

    val db = remember {
        UsuariosDB(context.applicationContext)
    }

    DisposableEffect(db) {
        onDispose {
            db.close()
        }
    }

    val scope = rememberCoroutineScope()

    var usuario by remember {
        mutableStateOf("")
    }

    var clave by remember {
        mutableStateOf("")
    }

    var confirmarClave by remember {
        mutableStateOf("")
    }

    var registrando by remember {
        mutableStateOf(false)
    }

    var procesando by remember {
        mutableStateOf(false)
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Control Ambiental IoT",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = if (registrando) {
                "Crear cuenta"
            } else {
                "Inicio de sesión"
            },
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            "Sistema de monitoreo y control ambiental"
        )

        HorizontalDivider()

        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            visualTransformation =
                PasswordVisualTransformation(),
            singleLine = true,
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth()
        )

        if (registrando) {
            OutlinedTextField(
                value = confirmarClave,
                onValueChange = {
                    confirmarClave = it
                },
                label = {
                    Text("Confirmar contraseña")
                },
                visualTransformation =
                    PasswordVisualTransformation(),
                singleLine = true,
                enabled = !procesando,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !procesando,
            onClick = {
                val nombre = usuario.trim()
                val password = clave

                mensaje = ""

                when {
                    nombre.length < 3 -> {
                        mensaje =
                            "El usuario debe tener al menos 3 caracteres"
                    }

                    password.length < 8 -> {
                        mensaje =
                            "La contraseña debe tener al menos 8 caracteres"
                    }

                    registrando &&
                            password != confirmarClave -> {
                        mensaje =
                            "Las contraseñas no coinciden"
                    }

                    else -> {
                        procesando = true

                        scope.launch {
                            try {
                                val resultado = withContext(
                                    Dispatchers.IO
                                ) {
                                    if (registrando) {
                                        db.registrar(
                                            nombre,
                                            password
                                        )
                                    } else {
                                        db.iniciarSesion(
                                            nombre,
                                            password
                                        )
                                    }
                                }

                                if (registrando) {
                                    if (resultado) {
                                        mensaje =
                                            "Cuenta creada correctamente"
                                        registrando = false
                                        clave = ""
                                        confirmarClave = ""
                                    } else {
                                        mensaje =
                                            "No se pudo registrar. El usuario podría existir"
                                    }
                                } else {
                                    if (resultado) {
                                        onLoginExitoso(nombre)
                                    } else {
                                        mensaje =
                                            "Usuario o contraseña incorrectos"
                                    }
                                }
                            } catch (e: Exception) {
                                mensaje =
                                    "Ocurrió un error al acceder a la base de datos"
                            } finally {
                                procesando = false
                            }
                        }
                    }
                }
            }
        ) {
            Text(
                if (registrando) {
                    "Crear cuenta"
                } else {
                    "Iniciar sesión"
                }
            )
        }

        if (procesando) {
            CircularProgressIndicator()
        }

        TextButton(
            enabled = !procesando,
            onClick = {
                registrando = !registrando
                clave = ""
                confirmarClave = ""
                mensaje = ""
            }
        ) {
            Text(
                if (registrando) {
                    "¿Ya tienes cuenta? Iniciar sesión"
                } else {
                    "¿No tienes cuenta? Registrarse"
                }
            )
        }

        if (mensaje.isNotEmpty()) {
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
