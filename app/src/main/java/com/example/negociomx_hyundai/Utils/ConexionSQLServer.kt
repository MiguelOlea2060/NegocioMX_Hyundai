package com.example.negociomx_hyundai.Utils

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager

object ConexionSQLServer {
/*
    //servidornube
    private const val SERVER = "dato"
    private const val DATABASE = "dato"
    private const val USERNAME = "dato"
    private const val PASSWORD = "dato
    private const val PORT = "1433"*/

    //Servidor Nube

    private const val SERVER = "P3NWPLSK12SQL-v13.shr.prod.phx3.secureserver.net"
    private const val DATABASE = "NegocioMX_Concentradora"
    private const val USERNAME = "NegocioMX_Concentradora"
    private const val PASSWORD = "NITr11ziu7#"
    private const val PORT = "1433"


    fun obtenerConexion(): Connection? {
        return try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver")

            val connectionString = "jdbc:jtds:sqlserver://$SERVER:$PORT/$DATABASE;instance=SQLEXPRESS;user=$USERNAME;password=$PASSWORD"

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


