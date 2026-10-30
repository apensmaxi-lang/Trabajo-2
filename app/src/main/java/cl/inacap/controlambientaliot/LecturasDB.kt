
package cl.inacap.controlambientaliot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class LecturaAmbiental(
    val id: Long,
    val fecha: Long,
    val temperatura: Float?,
    val luminosidad: Float?
)

class LecturasDB(context: Context) :
    SQLiteOpenHelper(
        context.applicationContext,
        "lecturas.db",
        null,
        1
    ) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE lecturas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha INTEGER NOT NULL,
                temperatura REAL,
                luminosidad REAL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Versión inicial de la base de datos.
    }

    fun guardarLectura(
        temperatura: Float?,
        luminosidad: Float?
    ): Boolean {

        if (temperatura == null && luminosidad == null) {
            return false
        }

        val valores = ContentValues().apply {
            put("fecha", System.currentTimeMillis())

            if (temperatura != null) {
                put("temperatura", temperatura)
            } else {
                putNull("temperatura")
            }

            if (luminosidad != null) {
                put("luminosidad", luminosidad)
            } else {
                putNull("luminosidad")
            }
        }

        val resultado = writableDatabase.insert(
            "lecturas",
            null,
            valores
        )

        return resultado != -1L
    }

    fun obtenerUltimas(
        limite: Int = 50
    ): List<LecturaAmbiental> {

        val lecturas = mutableListOf<LecturaAmbiental>()

        readableDatabase.query(
            "lecturas",
            arrayOf(
                "id",
                "fecha",
                "temperatura",
                "luminosidad"
            ),
            null,
            null,
            null,
            null,
            "fecha DESC, id DESC",
            limite.coerceIn(1, 500).toString()
        ).use { cursor ->

            while (cursor.moveToNext()) {

                val lectura = LecturaAmbiental(
                    id = cursor.getLong(0),
                    fecha = cursor.getLong(1),
                    temperatura = if (cursor.isNull(2)) {
                        null
                    } else {
                        cursor.getFloat(2)
                    },
                    luminosidad = if (cursor.isNull(3)) {
                        null
                    } else {
                        cursor.getFloat(3)
                    }
                )

                lecturas.add(lectura)
            }
        }

        return lecturas
    }
}
