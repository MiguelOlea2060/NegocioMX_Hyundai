package com.example.negociomx_hyundai.DAL

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.Bloque
import com.example.negociomx_hyundai.BE.BloqueColumnaFilaUso
import com.example.negociomx_hyundai.BE.Paso1SOCItem
import com.example.negociomx_hyundai.BE.PasoLogVehiculo
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.PosicionBloque
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DALPasoLogVehiculo {

    suspend fun consultarPasosPorFecha(fecha: String): List<PasoLogVehiculo> = withContext(Dispatchers.IO) {
        val registros = mutableListOf<PasoLogVehiculo>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALConsultaPaso1SOC", "🔍 Consultando registros SOC para fecha: $fecha")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALConsultaPaso1SOC", "❌ No se pudo obtener conexión")
                return@withContext registros
            }

            // Parsear fecha para obtener año, mes y día
            val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fechaDate = formatoFecha.parse(fecha)
            val calendar = Calendar.getInstance()
            calendar.time = fechaDate!!

            val anio = calendar.get(Calendar.YEAR)
            val mes = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH es 0-based
            val dia = calendar.get(Calendar.DAY_OF_MONTH)

            val query = """
            select p.IdPasoLogVehiculo, p.IdVehiculo, p.IdStatusActual, s.Nombre NombreStatus, p.IdUsuarioAlta
                  , pd.IdPasoLogVehiculoDet, pd.IdBloque NombreBloque, pd.FechaMovimiento
                  , pd.IdEmpleadoPosiciono, pd.IdEmpleadoTransporte, ep.Nombres, ep.ApellidoPaterno
                  , ep.ApellidoMaterno, pd.IdEmpleadoTransporte, ce.NombreCompleto as NombreEmpleadoTransporte
                  , pd.IdTransporte, c.Nombre as NombreTransporte, pd.IdStatus IdStatusPD, s1.Nombre 
                  NombreStatusPD, pd.NumeroEconomico, pd.IdUsuarioMovimiento, pd.Fila, pd.Columna, v.Vin
                  , p.FechaAlta, pd.IdBloque
                from dbo.PasoLogVehiculo p inner join dbo.PasoLogVehiculoDet pd on p.IdPasoLogVehiculo=pd.IdPasoLogVehiculo
                left join dbo.Vehiculo v on p.IdVehiculo=v.IdVehiculo
                left join dbo.Status s on p.IdStatusActual=s.IdStatus
                left join dbo.Bloque b on pd.IdBloque=b.IdBloque
                left join dbo.Cliente c on pd.IdTransporte=c.IdCliente
                left join dbo.Empleado ep on pd.IdEmpleadoPosiciono=ep.IdEmpleado
                left join dbo.ClienteEmpleado ce on pd.IdEmpleadoTransporte=ce.IdClienteEmpleado
                left join dbo.Status s1 on pd.IdStatus=s1.IdStatus
            where DATEPART(year, pd.fechamovimiento)=?
                  and  DATEPART(MONTH, pd.fechamovimiento)=?
                  and  DATEPART(DAY, pd.fechamovimiento)=?
            order by pd.FechaMovimiento asc
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            statement.setInt(1, anio)
            statement.setInt(2, mes)
            statement.setInt(3, dia)

            resultSet = statement.executeQuery()

            while (resultSet.next()) {

                val registro = PasoLogVehiculo(
                    IdVehiculo = resultSet.getInt("IdVehiculo"),
                    Vin = resultSet.getString("Vin") ?: "",
                    FechaAlta = resultSet.getString("FechaAlta") ?: "",
                    IdStatusActual = resultSet.getInt("IdStatusActual"),
                    IdPasoLogVehiculo =resultSet.getInt("IdPasoLogVehiculo")?:0,
                    NombreStatus = resultSet.getString("NombreStatus")
                )
                var apellidoPaterno= resultSet.getString("ApellidoPaterno")?:""
                var apellidoMaterno= resultSet.getString("ApellidoMaterno")?:""

                var det=PasoLogVehiculoDet(
                    IdVehiculo = registro.IdVehiculo,
                    IdPasoLogVehiculoDet = resultSet.getInt("IdPasoLogVehiculoDet")?:0,
                    Bloque = resultSet.getString("NombreBloque")?:"",
                    IdBloque = resultSet.getShort("IdBloque")?:0,
                    FechaMovimiento = resultSet.getString("FechaMovimiento"),
                    IdEmpleadoPosiciono = resultSet.getInt("IdEmpleadoPosiciono"),
                    IdEmpleadoTransporte = resultSet.getInt("IdEmpleadoTransporte"),
                    IdTransporte =resultSet.getInt("IdTransporte")?:0,
                    NombreTransporte = resultSet.getString("NombreTransporte")?:"",
                    NombreEmpleadoTransporte = resultSet.getString("NombreEmpleadoTransporte")?:"",
                    NumeroEconomico = resultSet.getString("NumeroEconomico")?:"",
                    NombreEmpleadoPosiciono = resultSet.getString("Nombres")?:"",
                    IdPasoLogVehiculo = registro.IdPasoLogVehiculo,
                    IdUsuarioMovimiento = resultSet.getInt("IdUsuarioMovimiento")?:0,
                    IdStatus = resultSet.getInt("IdStatusPD")?:0,
                    NombreStatus = resultSet.getString("NombreStatusPD")?:"",
                    Fila =resultSet.getShort("Fila")?:0,
                    Columna = resultSet.getShort("Columna")?:0,
                )
                if (apellidoPaterno.isNotEmpty()) det.NombreEmpleadoPosiciono+=" "+apellidoPaterno
                if (apellidoMaterno.isNotEmpty()) det.NombreEmpleadoPosiciono+=" "+apellidoMaterno

                val find=registros.filter { it.IdPasoLogVehiculo==registro.IdPasoLogVehiculo }.firstOrNull()
                if(find==null) {
                    if(registro.Detalles==null)registro.Detalles= mutableListOf()
                    registro.Detalles?.add(det)

                    registros.add(registro)
                }
                else
                {
                    if(find.Detalles==null)find.Detalles= mutableListOf()

                    find.Detalles?.add(det)
                }
            }

            Log.d("DALConsultaPaso1SOC", "✅ Se obtuvieron ${registros.size} registros")

        } catch (e: Exception) {
            Log.e("DALConsultaPaso1SOC", "💥 Error consultando registros: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALConsultaPaso1SOC", "Error cerrando recursos: ${e.message}")
            }
        }

        return@withContext registros
    }

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
        IdBloque:Short?=null,
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

    suspend fun insertaStatusNuevoPasoLogVehiculo(paso:PasoLogVehiculoDet ): Boolean = withContext(Dispatchers.IO) {
        var conexion: Connection? = null
        var statementPrincipal: PreparedStatement? = null
        var statementDetalle: PreparedStatement? = null
        var statementBloque: PreparedStatement? = null

        try {
            Log.d("DALPasoLogVehiculo", "💾 Creando registro de posicionado para vehículo ID: " +
                    "${paso.IdPasoLogVehiculo}")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext false
            }

            conexion.autoCommit = false
            // 1. Actualizar status actual en PasoLogVehiculo
            var queryPrincipal = """
                UPDATE PasoLogVehiculo  SET IdStatusActual = ?  WHERE IdPasoLogVehiculo = ?
            """.trimIndent()

            statementPrincipal = conexion.prepareStatement(queryPrincipal)
            statementPrincipal.setInt(1, paso?.IdStatus!!)
            statementPrincipal.setInt(2, paso?.IdPasoLogVehiculo!!)
            statementPrincipal.executeUpdate()

            // 2. Insertar un registro en tabla dbo.BloqueColumnaFilaUso
            //    esta tanla define que posiciones se encuentra ocupadas
            queryPrincipal = """
                INSERT INTO dbo.BloqueColumnaFilaUso(IdBloque, NumColumna, NumFila, Nombre, IdVehiculo, Activa) 
                values (?, ?, ?, ?, ?, ?)
            """.trimIndent()

            statementBloque = conexion.prepareStatement(queryPrincipal)
            statementBloque.setShort(1, paso?.IdBloque!!)
            statementBloque.setShort(2, paso?.Columna!!)
            statementBloque.setShort(3, paso?.Fila!!)
            statementBloque.setString(4, "Col. -> ${paso?.Columna}  Fila-> ${paso?.Fila}")
            statementBloque.setInt(5, paso?.IdVehiculo!!)
            statementBloque.setBoolean(6, true)
            statementBloque.executeUpdate()


            // 2. Insertar detalle de posicionado
            // <CHANGE> Agregar campos faltantes: IdTipoMovimiento y PersonaQueHaraMovimiento
            var campos=""
            var valores=""
            var contadorparams:Int=1

            campos+="(IdPasoLogVehiculo, Bloque, Fila, Placa"
            valores+="values(?, ?, ?, ?"
            if(paso?.Columna!=null)
            {
                campos+=", Columna"
                valores+=", ?"
            }
            if(paso?.IdTipoMovimiento!=null) {
                campos+=", IdTipoMovimiento"
                valores+=", ?"
            }
            campos+=", PersonaQueHaraMovimiento, IdStatus, FechaMovimiento"
            valores+=", ?, ?, ?"
            if(paso?.IdUsuarioMovimiento!=null) {
                campos+=", IdUsuarioMovimiento"
                valores+=", ?"
            }
            if(paso?.IdEmpleadoPosiciono!=null) {
                campos+=", IdEmpleadoPosiciono"
                valores+=", ?"
            }
            campos+=", IdBloque"
            valores+=", ?"
            if(paso?.EnviadoAInterface!=null) {
                campos+=", EnviadoAInterface"
                valores+=", ?"
            }
            if (paso?.NumeroEconomico!=null)
            {
                campos+=", NumeroEconomico"
                valores+=", ?"
            }
            campos+=") "
            valores+=") "
            var queryDetalle = "INSERT INTO PasoLogVehiculoDet "
            queryDetalle+=campos + valores
            statementDetalle = conexion.prepareStatement(queryDetalle)

            statementDetalle.setInt(contadorparams, paso?.IdPasoLogVehiculo!!)
            contadorparams++
            statementDetalle.setString(contadorparams, paso?.Bloque)
            contadorparams++
            statementDetalle.setShort(contadorparams, paso?.Fila!!)
            contadorparams++
            statementDetalle.setString(contadorparams, paso?.Placa)
            contadorparams++

            if(paso?.Columna!=null) {
                statementDetalle.setShort(contadorparams, paso?.Columna!!)
                contadorparams++
            }
            if(paso?.IdTipoMovimiento!=null) {
                statementDetalle.setInt(contadorparams, paso?.IdTipoMovimiento!!)
                contadorparams++
            }
            statementDetalle.setString(contadorparams, paso.PersonaQueHaraMovimiento)
            contadorparams++
            statementDetalle.setInt(contadorparams, paso?.IdStatus!!)
            contadorparams++
            statementDetalle.setString(contadorparams, paso?.FechaMovimiento)
            contadorparams++

            if(paso?.IdUsuarioMovimiento!=null) {
                statementDetalle.setInt(contadorparams, paso?.IdUsuarioMovimiento!!)
                contadorparams++
            }
            if(paso?.IdEmpleadoPosiciono!=null) {
                statementDetalle.setInt(contadorparams, paso?.IdEmpleadoPosiciono!!)
                contadorparams++
            }
            statementDetalle.setShort(contadorparams, paso?.IdBloque!!)
            contadorparams++

            if(paso?.EnviadoAInterface!=null) {
                statementDetalle.setBoolean(contadorparams, paso?.EnviadoAInterface!!)
                contadorparams++
            }
            if (paso?.NumeroEconomico!=null)
            {
                statementDetalle.setString(contadorparams, paso?.NumeroEconomico)
                contadorparams++
            }

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
                statementBloque?.close()
                statementDetalle?.close()
                statementPrincipal?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALPasoLogVehiculo", "Error cerrando conexión: ${e.message}")
            }
        }
    }


    // CONSULTAR BLOQUES DISPONIBLES
    suspend fun consultarBloques(): List<Bloque> = withContext(Dispatchers.IO) {
        val bloques = mutableListOf<Bloque>()
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

            val query = """
                    select b.IdBloque, b.NumColumnas, NumFilas, b.Nombre, bd.IdBloqueColumnaFilaUso
                            , bd.NumColumna, bd.NumFila, bd.Activa
                    from dbo.Bloque b left join dbo.bloquecolumnafilauso bd on b.idbloque=bd.idbloque
                    where Activo=1  order by b.Nombre
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val bloque =Bloque(
                    IdBloque =  resultSet.getShort("IdBloque"),
                    NumColumnas = resultSet.getShort("NumColumnas"),
                    NumFilas = resultSet.getShort("NumFilas"),
                    Nombre = resultSet.getString("Nombre")
                )
                var find=bloques.filter { it.IdBloque==bloque.IdBloque }.firstOrNull()
                if (find==null)
                    bloques.add(bloque)
                else
                {
                    if(find.Ocupadas==null)find.Ocupadas= mutableListOf<BloqueColumnaFilaUso>()
                    var item=BloqueColumnaFilaUso(
                        IdBloque = bloque.IdBloque,
                        IdBloqueColumnaFilaUso = resultSet.getShort("IdBloqueColumnaFilaUso")?:0,
                        NumFila =  resultSet.getShort("NumFila")?:0,
                        NumColumna =  resultSet.getShort("NumColumna")?:0,
                        Activa = resultSet.getBoolean("Activa")?:false,
                    )
                    find.Ocupadas?.add(item)
                }
            }

            Log.d("DALPasoLogVehiculo", "✅ Se obtuvieron ${bloques.size} bloques")
        } catch (e: Exception) {
            Log.e("DALPasoLogVehiculo", "💥 Error consultando bloques: ${e.message}")
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
    suspend fun consultarPosicionesPorBloque(idBloque: Short,numColumnas:Short, numFilas:Short):
            List<PosicionBloque> = withContext(Dispatchers.IO) {
        val posiciones = mutableListOf<PosicionBloque>()
        var ocupadas= mutableListOf<BloqueColumnaFilaUso>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALPasoLogVehiculo", "🔍 Consultando posiciones para bloque: $idBloque")
            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALPasoLogVehiculo", "❌ No se pudo obtener conexión")
                return@withContext posiciones
            }

            val query = """
                    select IdBloque, NumColumna, NumFila, Nombre 
                    from dbo.BloqueColumnaFilaUso 
                    where IdBloque= ?
            """.trimIndent()

            statement = conexion.prepareStatement(query)
            statement.setShort(1, idBloque)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val ocupada =BloqueColumnaFilaUso(
                    IdBloque =  resultSet.getShort("IdBloque"),
                    Nombre = resultSet.getString("Nombre"),
                    NumFila = resultSet.getShort("NumFila"),
                    NumColumna = resultSet.getShort("NumColumna"),
                )
                ocupadas.add(ocupada)
            }

            var columna:Short=1
            var fila:Short=1
            var nombre:String=""
            while (columna<=numColumnas)
            {
                fila=1
                while (fila<=numFilas)
                {
                    var existe=false
                    if(ocupadas!=null && ocupadas.count()>0)
                      existe= ocupadas.filter { it->it.NumFila==fila && it.NumColumna==columna
                              && it.IdBloque==idBloque }.count()>0

                    if(!existe) {
                        nombre = "Col -> ${columna} - Fila -> ${fila}"
                        posiciones.add(
                            PosicionBloque(
                                IdBloque = idBloque,
                                Nombre = nombre,
                                Fila = fila,
                                Columna = columna)
                        )
                    }
                    fila++
                }
                columna++
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