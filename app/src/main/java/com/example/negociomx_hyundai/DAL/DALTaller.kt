package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.ParteDanno
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALTaller {

    // CONSULTAR PARTES DAÑADAS DISPONIBLES
    suspend fun consultarPartesDanno(): List<ParteDanno> = withContext(Dispatchers.IO) {
        val partes = mutableListOf<ParteDanno>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALTaller", "🔍 Consultando partes dañadas...")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALTaller", "❌ No se pudo obtener conexión")
                return@withContext partes
            }

            val query = """
                SELECT IdParteDanno, Nombre, Activa 
                FROM dbo.ParteDanno 
                WHERE Activa = 1 
                ORDER BY Nombre
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val parte = ParteDanno(
                    IdParteDanno = resultSet.getShort("IdParteDanno"),
                    Nombre = resultSet.getString("Nombre") ?: "",
                    Activa = resultSet.getBoolean("Activa"),
                )
                partes.add(parte)
            }
            Log.d("DALTaller", "✅ Se obtuvieron ${partes.size} partes dañadas")
        } catch (e: Exception) {
            Log.e("DALTaller", "💥 Error consultando partes dañadas: ${e.message}")
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

        return@withContext partes
    }


    // GUARDAR REGISTRO DE TALLER
    suspend fun crearRegistroTaller(
       det: PasoLogVehiculoDet
    ): Boolean = withContext(Dispatchers.IO) {
        var conexion: Connection? = null
        var statementPrincipal: PreparedStatement? = null
        var statementDetalle: PreparedStatement? = null

        try {
            Log.d("DALTaller", "💾 Creando registro de taller para vehículo ID: ${det.IdVehiculo}")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALTaller", "❌ No se pudo obtener conexión")
                return@withContext false
            }

            conexion.autoCommit = false

            // 1. Actualizar status actual en PasoLogVehiculo
            val queryPrincipal = """
                UPDATE PasoLogVehiculo 
                SET IdStatusActual = 171 
                WHERE IdVehiculo = ?
            """.trimIndent()

            statementPrincipal = conexion.prepareStatement(queryPrincipal)
            statementPrincipal.setInt(1, det.IdVehiculo!!)
            statementPrincipal.executeUpdate()

            // 3. Insertar detalle de taller
            val queryDetalle = """
                INSERT INTO PasoLogVehiculoDet (
                    IdPasoLogVehiculo, PersonaQueHaraMovimiento, IdParteDanno, IdtipoEntradaSalida,
                    Observacion, IdStatus, FechaMovimiento, IdUsuarioMovimiento, IdEmpleadoPosiciono, Placa
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            statementDetalle = conexion.prepareStatement(queryDetalle)
            statementDetalle.setInt(1, det.IdPasoLogVehiculo!!)
            statementDetalle.setString(2, det.PersonaQueHaraMovimiento )
            statementDetalle.setShort(3, det.IdParteDanno!!.toShort())
            statementDetalle.setInt(4, det.IdTipoEntradaSalida!!)
            statementDetalle.setString(5, det.Observacion)
            statementDetalle.setInt(6, det.IdStatus!!)
            statementDetalle.setString(7, det.FechaMovimiento)
            statementDetalle.setInt(8, det.IdUsuarioMovimiento!!)
            statementDetalle.setInt(9, det.IdEmpleadoPosiciono!!)
            statementDetalle.setString(10, det.Placa!!)

            statementDetalle.executeUpdate()

            conexion.commit()
            Log.d("DALTaller", "✅ Registro de taller creado exitosamente")
            return@withContext true

        } catch (e: Exception) {
            Log.e("DALTaller", "💥 Error creando registro de taller: ${e.message}")
            conexion?.rollback()
            return@withContext false
        } finally {
            try {
                statementDetalle?.close()
                statementPrincipal?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALTaller", "Error cerrando conexión: ${e.message}")
            }
        }
    }
}