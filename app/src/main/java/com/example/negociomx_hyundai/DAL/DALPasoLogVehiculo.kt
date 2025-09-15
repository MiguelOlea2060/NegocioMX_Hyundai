package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.PasoLogVehiculo
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

class DALPasoLogVehiculo {

    suspend fun consultarStatusActualVehiculo(idVehiculo: Int): PasoLogVehiculoDet? = withContext(Dispatchers.IO) {
        var statusActual: PasoLogVehiculoDet? = null
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALPasoLogVehiculo", "🔍 Consultando status actual del vehículo ID: $idVehiculo")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext null
            }

            val query = """
                SELECT TOP 1 pld.*, s.Nombre as NombreStatus
                FROM PasoLogVehiculoDet pld
                INNER JOIN PasoLogVehiculo pl ON pld.IdPasoLogVehiculo = pl.IdPasoLogVehiculo
                INNER JOIN Status s ON pld.IdStatus = s.IdStatus
                WHERE pl.IdVehiculo = ?
                ORDER BY pld.FechaMovimiento DESC
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            statement.setInt(1, idVehiculo)
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                statusActual = PasoLogVehiculoDet(
                    IdPasoLogVehiculoDet = resultSet.getInt("IdPasoLogVehiculoDet"),
                    IdPasoLogVehiculo = resultSet.getInt("IdPasoLogVehiculo"),
                    IdStatus = resultSet.getInt("IdStatus"),
                    IdTipoEntradaSalida = resultSet.getInt("IdTipoEntradaSalida"),
                    Placa = resultSet.getString("Placa"),
                    FechaMovimiento = resultSet.getString("FechaMovimiento")
                )
                Log.d("DALPasoLogVehiculo", "✅ Status actual encontrado: ${resultSet.getString("NombreStatus")}")
            }

        } catch (e: Exception) {
            Log.e("DALPasoLogVehiculo", "💥 Error consultando status: ${e.message}")
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALPasoLogVehiculo", "Error cerrando conexión: ${e.message}")
            }
        }

        return@withContext statusActual
    }

    suspend fun crearRegistroEntrada(
        idVehiculo: Int,
        idUsuario: Int,
        tipoEntrada: Int, // 1=Rodando, 2=En Madrina
        placa: String,
        numeroEconomico: String? = null,
        idTransporte: Int? = null,
        idEmpleadoTransporte: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var conexion: Connection? = null
        var statementPrincipal: PreparedStatement? = null
        var statementDetalle: PreparedStatement? = null

        try {
            Log.d("DALPasoLogVehiculo", "💾 Creando registro de entrada para vehículo ID: $idVehiculo")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext false
            }

            conexion.autoCommit = false

            // 1. Insertar registro principal
            val queryPrincipal = """
                INSERT INTO PasoLogVehiculo (IdVehiculo, IdStatusActual, FechaAlta, IdUsuarioAlta)
                VALUES (?, 1, GETDATE(), ?)
            """.trimIndent()

            statementPrincipal = conexion.prepareStatement(queryPrincipal, PreparedStatement.RETURN_GENERATED_KEYS)
            statementPrincipal.setInt(1, idVehiculo)
            statementPrincipal.setInt(2, idUsuario)

            val filasAfectadas = statementPrincipal.executeUpdate()
            if (filasAfectadas == 0) {
                throw Exception("No se pudo insertar el registro principal")
            }

            // Obtener ID generado
            val generatedKeys = statementPrincipal.generatedKeys
            var idPasoLogVehiculo = 0
            if (generatedKeys.next()) {
                idPasoLogVehiculo = generatedKeys.getInt(1)
            }

            // 2. Insertar detalle
            val queryDetalle = """
                INSERT INTO PasoLogVehiculoDet (
                    IdPasoLogVehiculo, IdTransporte, Placa, NumeroEconomico, 
                    IdEmpleadoTransporte, IdTipoEntradaSalida, IdStatus, 
                    FechaMovimiento, IdUsuarioMovimiento
                ) VALUES (?, ?, ?, ?, ?, ?, 1, GETDATE(), ?)
            """.trimIndent()

            statementDetalle = conexion.prepareStatement(queryDetalle)
            statementDetalle.setInt(1, idPasoLogVehiculo)
            statementDetalle.setObject(2, idTransporte)
            statementDetalle.setString(3, placa)
            statementDetalle.setString(4, numeroEconomico)
            statementDetalle.setObject(5, idEmpleadoTransporte)
            statementDetalle.setInt(6, tipoEntrada)
            statementDetalle.setInt(7, idUsuario)

            statementDetalle.executeUpdate()

            conexion.commit()
            Log.d("DALPasoLogVehiculo", "✅ Registro de entrada creado exitosamente")
            return@withContext true

        } catch (e: Exception) {
            Log.e("DALPasoLogVehiculo", "💥 Error creando registro de entrada: ${e.message}")
            conexion?.rollback()
            return@withContext false
        } finally {
            try {
                statementDetalle?.close()
                statementPrincipal?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALPasoLogVehiculo", "Error cerrando conexión: ${e.message}")
            }
        }
    }
}