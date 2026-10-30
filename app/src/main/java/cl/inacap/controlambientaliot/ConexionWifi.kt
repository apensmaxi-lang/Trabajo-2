package cl.inacap.controlambientaliot


import java.net.ServerSocket
import java.net.Socket
import java.net.InetSocketAddress
import java.net.SocketException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ConexionWifi {

    private var servidor: ServerSocket? = null

    companion object {
        const val PUERTO = 5050
    }

    suspend fun enviar(
        ip: String,
        temperatura: Float?,
        luminosidad: Float?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val datos = JSONObject().apply {
                put(
                    "temperatura",
                    temperatura?.toDouble() ?: JSONObject.NULL
                )
                put(
                    "luminosidad",
                    luminosidad?.toDouble() ?: JSONObject.NULL
                )
            }

            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(ip.trim(), PUERTO),
                    4000
                )

                socket.getOutputStream()
                    .bufferedWriter(Charsets.UTF_8)
                    .use { escritor ->
                        escritor.write(datos.toString())
                        escritor.newLine()
                        escritor.flush()
                    }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun recibir(
        alRecibir: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            ServerSocket(PUERTO).use { escucha ->
                servidor = escucha

                while (true) {
                    escucha.accept().use { cliente ->
                        cliente.soTimeout = 5000

                        val mensaje = cliente
                            .getInputStream()
                            .bufferedReader(Charsets.UTF_8)
                            .readLine()

                        if (!mensaje.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                alRecibir(mensaje)
                            }
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            if (servidor != null) throw e
        } finally {
            servidor = null
        }
    }

    fun detenerRecepcion() {
        val actual = servidor
        servidor = null
        actual?.close()
    }
}
