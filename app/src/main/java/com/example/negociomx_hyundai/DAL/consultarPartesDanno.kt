package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.Empleado
import com.example.negociomx_hyundai.BE.EmpresaTaller
import com.example.negociomx_hyundai.BE.ParteDanno
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
                SELECT IdParteDanno, Nombre, Activa, IdTabla 
                FROM dbo.ParteDanno 
                WHERE Activa = 1 
                ORDER BY Nombre
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val parte = ParteDanno(
                    IdParteDanno = resultSet.getInt("IdParteDanno"),
                    Nombre = resultSet.getString("Nombre") ?: "",
                    Activa = resultSet.getBoolean("Activa"),
                    IdTabla = resultSet.getObject("IdTabla") as? Int
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

    // CONSULTAR EMPRESAS DE TALLER (INTERNAS Y EXTERNAS)
    suspend fun consultarEmpresasTaller(): List<EmpresaTaller> = withContext(Dispatchers.IO) {
        val empresas = mutableListOf<EmpresaTaller>()
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
                val empresa = EmpresaTaller(
                    IdEmpresa = resultSet.getInt("IdCliente"),
                    Nombre = resultSet.getString("Nombre") ?: "",
                    Tipo = "EXTERNA",
                    Activa = true
                )
                empresas.add(empresa)
            }

            // Agregar empresa interna (la propia empresa)
            val empresaInterna = EmpresaTaller(
                IdEmpresa = 0,
                Nombre = "EMPRESA INTERNA",
                Tipo = "INTERNA",
                Activa = true
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

    // GUARDAR REGISTRO DE TALLER
    suspend fun crearRegistroTaller(
        idVehiculo: Int,
        idUsuario: Int,
        idPersonalMovimiento: Int,
        tipoReparacion: String, // "EN_SITIO" o "FORANEA"
        idEmpresaExterna: Int? = null,
        idEmpresaInterna: Int? = null,
        idPersonaReparacion: Int? = null,
        idParteDanno: Int,
        descripcion: String,
        fechaInicio: String
    ): Boolean = withContext(Dispatchers.IO) {
        var conexion: Connection? = null
        var statementPrincipal: PreparedStatement? = null
        var statementDetalle: PreparedStatement? = null

        try {
            Log.d("DALTaller", "💾 Creando registro de taller para vehículo ID: $idVehiculo")

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
            statementPrincipal.setInt(1, idVehiculo)
            statementPrincipal.executeUpdate()

            // 2. Obtener IdPasoLogVehiculo
            val queryObtenerID = "SELECT IdPasoLogVehiculo FROM PasoLogVehiculo WHERE IdVehiculo = ?"
            val statementObtenerID = conexion.prepareStatement(queryObtenerID)
            statementObtenerID.setInt(1, idVehiculo)
            val resultSet = statementObtenerID.executeQuery()

            var idPasoLogVehiculo = 0
            if (resultSet.next()) {
                idPasoLogVehiculo = resultSet.getInt("IdPasoLogVehiculo")
            }

            // 3. Insertar detalle de taller
            val queryDetalle = """
                INSERT INTO PasoLogVehiculoDet (
                    IdPasoLogVehiculo, PersonaQueHaraMovimiento, IdParteDanno,
                    Observacion, IdStatus, FechaMovimiento, IdUsuarioMovimiento
                ) VALUES (?, ?, ?, ?, 171, ?, ?)
            """.trimIndent()

            statementDetalle = conexion.prepareStatement(queryDetalle)
            statementDetalle.setInt(1, idPasoLogVehiculo)
            statementDetalle.setString(2, "ID:$idPersonalMovimiento - Tipo:$tipoReparacion")
            statementDetalle.setInt(3, idParteDanno)
            statementDetalle.setString(4, descripcion)
            statementDetalle.setString(5, fechaInicio)
            statementDetalle.setInt(6, idUsuario)

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