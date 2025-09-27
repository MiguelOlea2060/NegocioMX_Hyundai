package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.CfgGlo
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALCfgSQL {
    suspend fun getConfigGlo(idEmpresa:Int?): CfgGlo? = withContext(Dispatchers.IO) {
        val cfg :CfgGlo?=null
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALCfgGlo", "🔍 Consultando configuracion global de empresa")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALTaller", "❌ No se pudo obtener conexión")
                return@withContext cfg
            }

            // Consultar Configuracion del sistema por IdEmpresa
            var query = """
                SELECT IdConfiguracion, IdEmpresa, ManejaSeleccionBloquePosXTablero
                FROM dbo.Configuracion
            """.trimIndent()
            if(idEmpresa!=null)
                query+=" WHERE IDEMPRESA = ?"


            statement = conexion.prepareStatement(query)
            if(idEmpresa!=null)
                statement.setInt(1,idEmpresa!!)
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val empresa = CfgGlo(
                    IdConfiguracion = resultSet.getInt("IdConfiguracion"),
                    IdEmpresa = resultSet.getInt("IdEmpresa")?:0,
                    ManejaSeleccionBloquePosXTablero = resultSet.getBoolean("Nombre") ?: false,
                )
            }

            Log.d("DALCfgGlo", "✅ Se consulto la configuracion de la empresa")

        } catch (e: Exception) {
            Log.e("DALCfgGlo", "💥 Error al leer la configuracion de la empresa")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALCfgGlo", "Error cerrando recursos: ${e.message}")
            }
        }

        return@withContext cfg
    }
}