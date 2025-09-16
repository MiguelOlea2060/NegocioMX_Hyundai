package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import com.google.zxing.client.result.VINParsedResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALPasoLogVehiculo {

    suspend fun consultaVehiculoPorVINParaPasoLogVehiculo(vin: String): VehiculoPasoLog? = withContext(Dispatchers.IO) {
        var item: VehiculoPasoLog? = null
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALPasoLogVehiculo", "🔍 Consultando status actual del vehículo VIN: $vin")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext null
            }

            val query = """
                select  v.vin, v.idmarca, v.idmodelo, marcaauto.nombre Marca, modelo.nombre Modelo, v.Annio, Motor, 
                        v.idvehiculo, ce.Nombre ColorExterior, ci.Nombre ColorInterior, tc.Nombre TipoCombustible, 
                        tv.Nombre TipoVehiculo, bl
						,pl.IdPasoLogVehiculo 
                from vehiculo v left join dbo.PasoLogVehiculo pl on v.IdVehiculo=pl.IdVehiculo 
						inner join dbo.MarcaAuto on v.IdMarca=MarcaAuto.IdMarcaAuto
                        inner join dbo.Modelo on v.IdModelo=modelo.IdModelo
                        left join dbo.VehiculoColor vc on v.IdVehiculo=vc.IdVehiculo
                        left join dbo.Color ce on vc.IdColor=ce.IdColor
                        left join dbo.Color ci on vc.IdColorInterior=ci.IdColor
                        left join dbo.TipoCombustible tc on v.idtipocombustible=tc.idtipocombustible
                        left join dbo.tipovehiculo tv on v.idtipovehiculo=tv.idtipovehiculo
                        left join dbo.bl b on v.idbl=b.idbl
                where v.vin = ?
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            statement.setString(1, vin)
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                item = VehiculoPasoLog(
                    Id = resultSet.getInt("IdVehiculo").toString(),
                    VIN = resultSet.getString("Vin") ?: "",
                    Marca = resultSet.getString("Marca") ?: "",
                    Modelo = resultSet.getString("Modelo") ?: "",
                    Anio = resultSet.getInt("Annio"),
                    ColorExterior = resultSet.getString("ColorExterior") ?: "",
                    ColorInterior = resultSet.getString("ColorInterior") ?: "",
                    BL = resultSet.getString("ColorInterior") ?: "",
                    NumeroSerie = resultSet.getString("BL") ?: "",
                    TipoVehiculo = resultSet.getString("TipoVehiculo") ?: "",
                    TipoCombustible = resultSet.getString("TipoCombustible") ?: "",
                    IdEmpresa = "", // No existe en el esquema actual
                    Activo = true, // Asumimos que está activo si existe
                    FechaCreacion = "", // No existe en el esquema actual
                    // FechaModificacion = resultSet.getString("FechaModificacion") ?: "",
                    // CAMPOS SOC - Valores por defecto ya que no existen en la BD actual
                    Odometro = 0,
                    Bateria = 0,
                    ModoTransporte = false,
                    RequiereRecarga = false,
                    Evidencia1 = "",
                    Evidencia2 = "",
                    FechaActualizacion = "",
                    IdPasoLogVehiculo = resultSet.getInt("IdPasoLogVehiculo")?:0,
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

        return@withContext item
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