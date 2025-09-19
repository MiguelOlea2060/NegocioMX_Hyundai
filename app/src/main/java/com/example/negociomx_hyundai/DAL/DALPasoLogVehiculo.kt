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
                        tv.Nombre TipoVehiculo, bl, pl.IdStatusActual, pl.IdPasoLogVehiculo 
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
                    IdPasoLogVehiculo = resultSet.getInt("IdPasoLogVehiculo")?:0,
                    IdStatusActual = resultSet.getInt("IdStatusActual")?:0
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
            // 1. Insertar registro principal
            val queryPrincipal = """
                INSERT INTO PasoLogVehiculo (IdVehiculo, IdStatusActual, FechaAlta, IdUsuarioAlta)
                VALUES (?, 168, GETDATE(), ?)
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
                ) VALUES (?, ?, ?, ?, ?, ?, 168, GETDATE(), ?)
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

    // CONSULTAR STATUS ACTUAL DE UN VEHÍCULO
    suspend fun consultarStatusActual(idVehiculo: Int): PasoLogVehiculoDet? = withContext(Dispatchers.IO) {
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
                SELECT TOP 1 d.*, s.Nombre as NombreStatus
                FROM PasoLogVehiculoDet d
                INNER JOIN PasoLogVehiculo p ON d.IdPasoLogVehiculo = p.IdPasoLogVehiculo
                INNER JOIN Status s ON d.IdStatus = s.IdStatus
                WHERE p.IdVehiculo = ?
                ORDER BY d.FechaMovimiento DESC
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            statement.setInt(1, idVehiculo)
            resultSet = statement.executeQuery()

            if (resultSet.next()) {
                statusActual = PasoLogVehiculoDet(
                    IdPasoLogVehiculoDet = resultSet.getInt("IdPasoLogVehiculoDet"),
                    IdPasoLogVehiculo = resultSet.getInt("IdPasoLogVehiculo"),
                    IdTransporte = resultSet.getObject("IdTransporte") as? Int,
                    Placa = resultSet.getString("Placa"),
                    NumeroEconomico = resultSet.getString("NumeroEconomico"),
                    IdEmpleadoTransporte = resultSet.getObject("IdEmpleadoTransporte") as? Int,
                    IdTipoEntradaSalida = resultSet.getObject("IdTipoEntradaSalida") as? Int,
                    IdStatus = resultSet.getInt("IdStatus"),
                    FechaMovimiento = resultSet.getString("FechaMovimiento"),
                    IdUsuarioMovimiento = resultSet.getInt("IdUsuarioMovimiento")
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


    // CREAR REGISTRO DE POSICIONADO
    suspend fun crearRegistroPosicionado(
        idVehiculo: Int,
        idUsuario: Int,
        bloque: String,
        fila: Int,
        columna: Int,
        idTipoMovimiento: Int,
        nombrePersonalMovimiento: String,
        idEmpleadoPersonal: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var conexion: Connection? = null
        var statementPrincipal: PreparedStatement? = null
        var statementDetalle: PreparedStatement? = null

        try {
            Log.d("DALPasoLogVehiculo", "💾 Creando registro de posicionado para vehículo ID: $idVehiculo")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext false
            }

            conexion.autoCommit = false

            // 1. Actualizar status actual en PasoLogVehiculo
            val queryPrincipal = """
                UPDATE PasoLogVehiculo 
                SET IdStatusActual = 170 
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

            // 3. Insertar detalle de posicionado
            // <CHANGE> Agregar campos faltantes: IdTipoMovimiento y PersonaQueHaraMovimiento
            val queryDetalle = """
                INSERT INTO PasoLogVehiculoDet (
                    IdPasoLogVehiculo, Bloque, Fila, Columna, 
                    IdTipoMovimiento, PersonaQueHaraMovimiento, 
                    IdStatus, FechaMovimiento, IdUsuarioMovimiento
                ) VALUES (?, ?, ?, ?, ?, ?, 170, GETDATE(), ?)
            """.trimIndent()

            statementDetalle = conexion.prepareStatement(queryDetalle)
            statementDetalle.setInt(1, idPasoLogVehiculo)
            statementDetalle.setString(2, bloque)
            statementDetalle.setInt(3, fila)
            statementDetalle.setInt(4, columna)
            statementDetalle.setInt(5, idTipoMovimiento)
            statementDetalle.setString(6, nombrePersonalMovimiento)
            statementDetalle.setInt(7, idUsuario)

            statementDetalle.executeUpdate()

            conexion.commit()
            Log.d("DALPasoLogVehiculo", "✅ Registro de posicionado creado exitosamente")
            return@withContext true

        } catch (e: Exception) {
            Log.e("DALPasoLogVehiculo", "💥 Error creando registro de posicionado: ${e.message}")
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





    // CONSULTAR BLOQUES DISPONIBLES
    suspend fun consultarBloques(): List<String> = withContext(Dispatchers.IO) {
        val bloques = mutableListOf<String>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALPasoLogVehiculo", "🔍 Consultando bloques disponibles...")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext bloques
            }

            // <CHANGE> Consultar bloques únicos de la tabla de configuración o usar valores predefinidos
            val query = """
                SELECT DISTINCT Bloque 
                FROM PasoLogVehiculoDet 
                WHERE Bloque IS NOT NULL AND Bloque != ''
                UNION
                SELECT 'A' as Bloque
                UNION SELECT 'B' as Bloque
                UNION SELECT 'C' as Bloque
                UNION SELECT 'D' as Bloque
                UNION SELECT 'E' as Bloque
                ORDER BY Bloque
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val bloque = resultSet.getString("Bloque")
                if (!bloque.isNullOrEmpty()) {
                    bloques.add(bloque)
                }
            }

            Log.d("DALPasoLogVehiculo", "✅ Se obtuvieron ${bloques.size} bloques")

        } catch (e: Exception) {
            Log.e("DALPasoLogVehiculo", "💥 Error consultando bloques: ${e.message}")
            // <CHANGE> Valores por defecto si falla la consulta
            bloques.addAll(listOf("A", "B", "C", "D", "E"))
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALPasoLogVehiculo", "Error cerrando conexión: ${e.message}")
            }
        }

        return@withContext bloques
    }

    // CONSULTAR POSICIONES DISPONIBLES PARA UN BLOQUE
    suspend fun consultarPosicionesPorBloque(bloque: String): List<String> = withContext(Dispatchers.IO) {
        val posiciones = mutableListOf<String>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALPasoLogVehiculo", "🔍 Consultando posiciones para bloque: $bloque")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext posiciones
            }

            // <CHANGE> Generar posiciones disponibles (1-1 a 20-10 como ejemplo)
            for (fila in 1..20) {
                for (columna in 1..10) {
                    posiciones.add("$fila-$columna")
                }
            }

            Log.d("DALPasoLogVehiculo", "✅ Se generaron ${posiciones.size} posiciones")

        } catch (e: Exception) {
            Log.e("DALPasoLogVehiculo", "💥 Error consultando posiciones: ${e.message}")
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALPasoLogVehiculo", "Error cerrando conexión: ${e.message}")
            }
        }

        return@withContext posiciones
    }

    // CONSULTAR TIPOS DE MOVIMIENTO
    suspend fun consultarTiposMovimiento(): List<Pair<Int, String>> = withContext(Dispatchers.IO) {
        val tiposMovimiento = mutableListOf<Pair<Int, String>>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALPasoLogVehiculo", "🔍 Consultando tipos de movimiento...")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext tiposMovimiento
            }

            val query = "SELECT IdTipoMovimiento, Nombre FROM TipoMovimiento WHERE Activo = 1 ORDER BY Nombre"
            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val id = resultSet.getInt("IdTipoMovimiento")
                val nombre = resultSet.getString("Nombre") ?: ""
                tiposMovimiento.add(Pair(id, nombre))
            }

            Log.d("DALPasoLogVehiculo", "✅ Se obtuvieron ${tiposMovimiento.size} tipos de movimiento")

        } catch (e: Exception) {
            Log.e("DALPasoLogVehiculo", "💥 Error consultando tipos de movimiento: ${e.message}")
            // <CHANGE> Valores por defecto si falla la consulta
            tiposMovimiento.addAll(listOf(
                Pair(1, "POSICIONADO MANUAL"),
                Pair(2, "POSICIONADO AUTOMÁTICO")
            ))
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALPasoLogVehiculo", "Error cerrando conexión: ${e.message}")
            }
        }

        return@withContext tiposMovimiento
    }







}