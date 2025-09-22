package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.Cliente
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALClienteSQL {
    // CONSULTAR EMPRESAS DE TALLER (INTERNAS Y EXTERNAS)
    suspend fun consultarEmpresasTaller(): List<Cliente> = withContext(Dispatchers.IO) {
        val empresas = mutableListOf<Cliente>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALTaller", "🔍 Consultando empresas de taller...")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALTaller", "❌ No se pudo obtener conexión")
                return@withContext empresas
            }

            // Consultar empresas externas (clientes con tabla específica)
            val queryExternas = """
                SELECT IdCliente, Nombre, 'EXTERNA' as Tipo
                FROM dbo.Cliente 
                WHERE substring(Tabla,6,1)='1'
                ORDER BY Nombre
            """.trimIndent()

            statement = conexion.prepareStatement(queryExternas)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val empresa = Cliente(
                    IdCliente = resultSet.getInt("IdCliente"),
                    Nombre = resultSet.getString("Nombre") ?: "",
                    Tipo = "EXTERNA",
                    Activo = true
                )
                empresas.add(empresa)
            }

            // Agregar empresa interna (la propia empresa)
            val empresaInterna = Cliente(
                IdCliente = 0,
                Nombre = "EMPRESA INTERNA",
                Tipo = "INTERNA",
                Activo = true
            )
            empresas.add(0, empresaInterna)

            Log.d("DALTaller", "✅ Se obtuvieron ${empresas.size} empresas de taller")

        } catch (e: Exception) {
            Log.e("DALTaller", "💥 Error consultando empresas: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALTaller", "Error cerrando recursos: ${e.message}")
            }
        }

        return@withContext empresas
    }
}