
package cl.inacap.controlambientaliot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuariosDB(context: Context) :
    SQLiteOpenHelper(context, "usuarios.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                usuario TEXT NOT NULL UNIQUE,
                sal BLOB NOT NULL,
                clave_hash BLOB NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Versión inicial
    }

    private fun crearHash(
        clave: String,
        sal: ByteArray
    ): ByteArray {
        val especificacion = PBEKeySpec(
            clave.toCharArray(),
            sal,
            600000,
            256
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(especificacion)
                .encoded
        } finally {
            especificacion.clearPassword()
        }
    }

    fun registrar(
        usuario: String,
        clave: String
    ): Boolean {
        val nombre = usuario.trim()

        if (nombre.length < 3 || clave.length < 8) {
            return false
        }

        val sal = ByteArray(16)
        SecureRandom().nextBytes(sal)

        val valores = ContentValues().apply {
            put("usuario", nombre)
            put("sal", sal)
            put("clave_hash", crearHash(clave, sal))
        }

        val resultado = writableDatabase.insertWithOnConflict(
            "usuarios",
            null,
            valores,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        return resultado != -1L
    }

    fun iniciarSesion(
        usuario: String,
        clave: String
    ): Boolean {
        readableDatabase.query(
            "usuarios",
            arrayOf("sal", "clave_hash"),
            "usuario = ?",
            arrayOf(usuario.trim()),
            null,
            null,
            null
        ).use { cursor ->
            if (!cursor.moveToFirst()) {
                return false
            }

            val sal = cursor.getBlob(0)
            val hashGuardado = cursor.getBlob(1)
            val hashIngresado = crearHash(clave, sal)

            return MessageDigest.isEqual(
                hashGuardado,
                hashIngresado
            )
        }
    }
}
