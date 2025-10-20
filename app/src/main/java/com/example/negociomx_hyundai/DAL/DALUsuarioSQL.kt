package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.CfgGlo
import com.example.negociomx_hyundai.BE.Usuario
import com.example.negociomx_hyundai.BE.UsuarioNube
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALUsuarioSQL {

    suspend fun getUsuarioByEmailAndPassword(email: String): Pair<UsuarioNube?,CfgGlo?>
    = withContext(Dispatchers.IO) {
        Log.d("DALUsuarioSQL", "🔍 Buscando usuario: $email")

        var usuario: UsuarioNube? = null
        var cfg: CfgGlo? = null
        var connection = ConexionSQLServer.obtenerConexion()

        try {
            if (connection != null) {
                val query = """
                exec getUsuarioAndCfgApp ?
                """

                val statement: PreparedStatement = connection.prepareStatement(query)
                statement.setString(1, email)
                //statement.setString(2, password) // En producción, usar hash

                val resultSet: ResultSet = statement.executeQuery()

                if (resultSet.next()) {
                    val idEmpresa=resultSet.getInt("IdEmpresa")
                    val rfcEmpresa=resultSet.getString("Rfc")?:""
                    usuario = UsuarioNube().apply {
                        Id = resultSet.getInt("IdUsuario").toString()
                        NombreCompleto = resultSet.getString("NombreCompleto")
                        Email = resultSet.getString("Email")
                        IdRol = resultSet.getInt("IdRol").toString()
                        IdEmpresa = idEmpresa.toString()
                        Activo = resultSet.getBoolean("Activo")
                        CuentaVerificada = resultSet.getBoolean("CuentaVerificada")
                        Password = resultSet.getString("Contrasena")

                        NombreComercialEmpresa=resultSet.getString("NombreComercial")?:""
                        RazonSocialEmpresa=resultSet.getString("RazonSocial")?:""
                        RfcEmpresa=rfcEmpresa
                    }
                    val idCfgApp=resultSet.getShort("IdCfgApp")?:0
                    if(idCfgApp>0) {
                        cfg = CfgGlo(
                            IdCfgApp = idCfgApp,
                            IdConfiguracion = resultSet.getInt("IdCfgApp")?:0,
                            IdEmpresa = idEmpresa,
                            ManejaSeleccionBloquePosXTablero = resultSet.getBoolean("ManejaSeleccionBloquePosXTablero")?:false,
                            urlGuardadoArchivos = resultSet.getString("urlguardadoarchivos")?:"",
                            UrlAPIControllerGuardadoArchivos=resultSet.getString("urlapicontrollerguardadoarchivos")?:"",
                            ManejaGuardadoArchivosEnBD = resultSet.getBoolean("ManejaGuardadoArchivosEnBD")?:false,
                            FormatoCarpetaArchivos = resultSet.getString("FormatoCarpetaArchivos")?:"",
                            ReglasNotificaciones = resultSet.getString("reglasnotificaciones")?:"",
                            RfcEmpresa = rfcEmpresa,
                            ManejaSeleccionObsMovimientoLocal = resultSet.getBoolean("ManejaSeleccionObsMovimientoLocal")?:false,
                            ManejaSeleccionObsEnTaller = resultSet.getBoolean("ManejaSeleccionObsEnTaller")?:false,
                            ManejaFotosEnInspeccionEntrada = resultSet.getBoolean("ManejaStatusInspeccionEntrada")?:false,
                            ManejaFotosEnInspeccionSalida = resultSet.getBoolean("ManejaStatusInspeccionSalida")?:false,
                            ManejaStatusInspeccionSalida = resultSet.getBoolean("ManejaStatusInspeccionSalida")?:false,
                            ManejaStatusInspeccionEntrada = resultSet.getBoolean("ManejaStatusInspeccionEntrada")?:false,
                            ManejaNotificaciones = resultSet.getBoolean("ManejaNotificaciones")?:false
                        )
                    }
                    Log.d("DALUsuarioSQL", "✅ Usuario encontrado: ${usuario.NombreCompleto}")
                }

                resultSet.close()
                statement.close()
            }
        } catch (e: Exception) {
            Log.e("DALUsuarioSQL", "❌ Error: ${e.message}")
        } finally {
            connection?.close()
        }

        return@withContext Pair(usuario,cfg)
    }

    suspend fun getUsuarioByEmail(email: String): Usuario? = withContext(Dispatchers.IO) {
        Log.d("DALUsuarioSQL", "🔍 Buscando usuario: $email")

        var usuario: Usuario? = null
        var connection = ConexionSQLServer.obtenerConexion()

        try {
            if (connection != null) {
                val query = """
                    SELECT IdUsuario, NombreCompleto, Email, IdRol, IdEmpresa,  Activo, CuentaVerificada
                    FROM Usuario 
                    WHERE Email = ?
                """

                val statement: PreparedStatement = connection.prepareStatement(query)
                statement.setString(1, email)

                val resultSet: ResultSet = statement.executeQuery()

                if (resultSet.next()) {
                    usuario = Usuario().apply {
                        IdUsuario = resultSet.getInt("IdUsuario")
                        NombreCompleto = resultSet.getString("NombreCompleto")
                        Email = resultSet.getString("Email")
                        IdRol = resultSet.getInt("IdRol")
                        IdEmpresa = resultSet.getInt("IdEmpresa")?:0
                        Activo = resultSet.getBoolean("Activo")
                        CuentaVerificada = resultSet.getBoolean("CuentaVerificada")
                    }
                    //Log.d("DALUsuarioSQL", "✅ Usuario encontrado: ${usuario.NombreCompleto}")
                }

                resultSet.close()
                statement.close()
            }
        } catch (e: Exception) {
            Log.e("DALUsuarioSQL", "❌ Error: ${e.message}")
        } finally {
            connection?.close()
        }

        return@withContext usuario
    }

    suspend fun getUsuariosByEmpresa(idEmpresa: Int?): List<Usuario>? = withContext(Dispatchers.IO) {
        var lista: MutableList<Usuario>? = null
        var connection = ConexionSQLServer.obtenerConexion()

        try {
            if (connection != null) {
                var query = """
                SELECT u.IdUsuario, u.NombreCompleto, u.Email, u.IdRol, u.IdEmpresa, u.Activo, u.CuentaVerificada, u.Contrasena
                    ,e.razonsocial, e.nombrecomercial, e.Rfc, u.fechacuentaverificada
                FROM Usuario u with (nolock) left join dbo.empresa e on u.idempresa=e.idempresa 
                """
                var where=""
                if(idEmpresa!=null)
                    where=" WHERE  u.idempresa= ?"

                query+=where

                val statement: PreparedStatement = connection.prepareStatement(query)
                if(idEmpresa!=null)
                    statement.setInt(1, idEmpresa)

                val resultSet: ResultSet = statement.executeQuery()

                lista= mutableListOf()
                while(resultSet.next())
                {
                    var usuario = Usuario().apply {
                        IdUsuario = resultSet.getInt("IdUsuario")
                        NombreCompleto = resultSet.getString("NombreCompleto")
                        Email = resultSet.getString("Email")
                        IdRol = resultSet.getInt("IdRol")
                        IdEmpresa = resultSet.getInt("IdEmpresa")?:0
                        Activo = resultSet.getBoolean("Activo")

                        CuentaVerificada = resultSet.getBoolean("CuentaVerificada")?:false
                        FechaCuentaVerificada=resultSet.getString("FechaCuentaVerificada")?:""
                    }

                    lista.add(usuario)
                }

                resultSet.close()
                statement.close()
            }
        } catch (e: Exception) {
            Log.e("DALUsuarioSQL", "❌ Error: ${e.message}")
        } finally {
            connection?.close()
        }

        return@withContext lista
    }

    suspend fun updateUsuario(usuario: Usuario): Boolean = withContext(Dispatchers.IO) {
        var res:Boolean=false
        var connection = ConexionSQLServer.obtenerConexion()

        try {
            if (connection != null) {

                connection.autoCommit = false

                var query="UPDATE Usuario set Activo = ? ,CuentaVerificada = ? WHERE IDUSUARIO = ?"
                val statement: PreparedStatement = connection.prepareStatement(query,
                    PreparedStatement.RETURN_GENERATED_KEYS)
                statement.setBoolean(1, usuario?.Activo!!)
                statement.setBoolean(2, usuario?.CuentaVerificada!!)
                statement.setInt(3, usuario?.IdUsuario!!)

                val filasAfectadas = statement.executeUpdate()
                if (filasAfectadas == 0) {
                    throw Exception("No se pudo insertar el Usuario al sistema")
                }
                res=filasAfectadas>0

                connection.commit()
                Log.d("DALUsuarioSQL", "✅ Se genero el IdUsuario: ${res}")
            }
        } catch (e: Exception) {
            Log.e("DALUsuarioSQL", "❌ Error: ${e.message}")
        } finally {
            connection?.close()
        }

        return@withContext res
    }

    suspend fun addUsuario(usuario: Usuario): Int? = withContext(Dispatchers.IO) {
        Log.d("DALUsuarioSQL", "🔍 Dando de alta a Usuario ${usuario.Email}")

        var IdUsuario:Int=0
        var connection = ConexionSQLServer.obtenerConexion()

        try {
            if (connection != null) {

                connection.autoCommit = false

                var campos="INSERT INTO Usuario (NombreCompleto, Email, Contrasena, Domicilio, IdRol, Bloqueado, Activo, " +
                        "NombreUsuario, UsuarioConcentrador, CuentaVerificada, Predeterminado"
                var values=" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                var query = ""
                if (usuario.IdEmpresa!=null) {
                    campos+=", IdEmpresa"
                    values += ", ?"
                }
                campos+=")"
                values += ")"

                query+=campos+values

                val statement: PreparedStatement = connection.prepareStatement(query,
                    PreparedStatement.RETURN_GENERATED_KEYS)
                statement.setString(1, usuario?.NombreCompleto)
                statement.setString(2, usuario?.Email)
                statement.setString(3, usuario?.Contrasena)
                statement.setString(4, usuario?.Domicilio)
                statement.setInt(5, usuario?.IdRol!!)
                statement.setBoolean(6, usuario?.Bloqueado!!)
                statement.setBoolean(7, usuario?.Activo!!)
                statement.setString(8, usuario?.NombreUsuario)
                statement.setBoolean(9, false)//UsuarioConsentrador
                statement.setBoolean(10, false)
                statement.setBoolean(11, false)
                if(usuario.IdEmpresa!=null)
                    statement.setInt(12, usuario.IdEmpresa!!)

                val filasAfectadas = statement.executeUpdate()
                if (filasAfectadas == 0) {
                    throw Exception("No se pudo insertar el Usuario al sistema")
                }

                val rs = statement.generatedKeys
                if (rs.next()) {
                    IdUsuario = rs.getInt(1)
                }
                connection.commit()
                Log.d("DALUsuarioSQL", "✅ Se genero el IdUsuario: ${IdUsuario}")
            }
        } catch (e: Exception) {
            Log.e("DALUsuarioSQL", "❌ Error: ${e.message}")
        } finally {
            connection?.close()
        }

        return@withContext IdUsuario
    }
}