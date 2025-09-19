package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.ActividadDiariaItem
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

class DALConsultaActividades {

    suspend fun consultarActividadesPorFecha(fecha: String): List<ActividadDiariaItem> = withContext(Dispatchers.IO) {
        val actividades = mutableListOf<ActividadDiariaItem>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALConsultaActividades", "🔍 Consultando actividades para fecha: $fecha")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALConsultaActividades", "❌ No se pudo obtener conexión")
                return@withContext actividades
            }

            // Parsear fecha para obtener año, mes y día
            val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechaDate = formatoFecha.parse(fecha)
            val calendar = Calendar.getInstance()
            calendar.time = fechaDate!!

            val anio = calendar.get(Calendar.YEAR)
            val mes = calendar.get(Calendar.MONTH) + 1
            val dia = calendar.get(Calendar.DAY_OF_MONTH)

            val query = """
                SELECT v.IdVehiculo,
                       plv.IdPasoLogVehiculo,
                       v.Vin,
                       v.BL,
                       v.Marca,
                       v.Modelo,
                       v.Anio,
                       v.ColorExterior,
                       v.ColorInterior,
                       plvd.IdStatus,
                       s.Nombre AS NombreStatus,
                       plvd.FechaMovimiento,
                       u.NombreCompleto AS UsuarioMovimiento,
                       plvd.Bloque,
                       plvd.Fila,
                       plvd.Columna,
                       plvd.PersonaQueHaraMovimiento,
                       plvd.Observacion,
                       plvd.Placa AS PlacaTransporte,
                       plvd.NumeroEconomico
                FROM dbo.PasoLogVehiculoDet plvd
                INNER JOIN dbo.PasoLogVehiculo plv ON plvd.IdPasoLogVehiculo = plv.IdPasoLogVehiculo
                INNER JOIN dbo.Vehiculo v ON plv.IdVehiculo = v.IdVehiculo
                INNER JOIN dbo.Status s ON plvd.IdStatus = s.IdStatus
                INNER JOIN dbo.Usuario u ON plvd.IdUsuarioMovimiento = u.IdUsuario
                WHERE DATEPART(YEAR, plvd.FechaMovimiento) = ?
                  AND DATEPART(MONTH, plvd.FechaMovimiento) = ?
                  AND DATEPART(DAY, plvd.FechaMovimiento) = ?
                ORDER BY plvd.FechaMovimiento DESC, v.Vin
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            statement.setInt(1, anio)
            statement.setInt(2, mes)
            statement.setInt(3, dia)

            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val actividad = ActividadDiariaItem(
                    IdVehiculo = resultSet.getInt("IdVehiculo"),
                    IdPasoLogVehiculo = resultSet.getInt("IdPasoLogVehiculo"),
                    VIN = resultSet.getString("Vin") ?: "",
                    BL = resultSet.getString("BL") ?: "",
                    Marca = resultSet.getString("Marca") ?: "",
                    Modelo = resultSet.getString("Modelo") ?: "",
                    Anio = resultSet.getInt("Anio"),
                    ColorExterior = resultSet.getString("ColorExterior") ?: "",
                    ColorInterior = resultSet.getString("ColorInterior") ?: "",
                    IdStatus = resultSet.getInt("IdStatus"),
                    NombreStatus = resultSet.getString("NombreStatus") ?: "",
                    FechaMovimiento = resultSet.getString("FechaMovimiento") ?: "",
                    UsuarioMovimiento = resultSet.getString("UsuarioMovimiento") ?: "",
                    Bloque = resultSet.getString("Bloque") ?: "",
                    Fila = resultSet.getObject("Fila") as? Int,
                    Columna = resultSet.getObject("Columna") as? Int,
                    PersonaQueHaraMovimiento = resultSet.getString("PersonaQueHaraMovimiento") ?: "",
                    Observacion = resultSet.getString("Observacion") ?: "",
                    PlacaTransporte = resultSet.getString("PlacaTransporte") ?: "",
                    NumeroEconomico = resultSet.getString("NumeroEconomico") ?: ""
                )
                actividades.add(actividad)
            }

            Log.d("DALConsultaActividades", "✅ Se obtuvieron ${actividades.size} actividades")

        } catch (e: Exception) {
            Log.e("DALConsultaActividades", "💥 Error consultando actividades: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALConsultaActividades", "Error cerrando recursos: ${e.message}")
            }
        }

        return@withContext actividades
    }
}