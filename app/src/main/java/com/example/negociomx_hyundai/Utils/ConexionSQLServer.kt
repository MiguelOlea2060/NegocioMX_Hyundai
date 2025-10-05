package com.example.negociomx_hyundai.Utils

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager

object ConexionSQLServer {
    private const val SERVER = "179.61.12.164"
    private const val DATABASE = "softsyst_n3goc10_hyunda1"
    private const val USERNAME = "softsyst_samx02"
    private const val PASSWORD = "NITr11ziu7#.MXL1z34@"
    private const val PORT = "1433"

    fun obtenerConexion(): Connection? {
        return try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")

            val connectionString = "jdbc:jtds:sqlserver://$SERVER:$PORT/$DATABASE;instance=SQLEXPRESS;user=$USERNAME;" +
                    "password=$PASSWORD"

            Log.d("ConexionSQLServer", "🔗 Intentando conectar a: $SERVER")

            val connection = DriverManager.getConnection(connectionString)

            Log.d("ConexionSQLServer", "✅ Conexión exitosa")
            connection

        } catch (e: Exception) {
            Log.e("ConexionSQLServer", "❌ Error de conexión: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}


