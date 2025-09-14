package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.UsuarioNube
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALUsuarioSQL {

    suspend fun getUsuarioByEmailAndPassword(email: String, password: String): UsuarioNube? = withContext(Dispatchers.IO) {
        Log.d("DALUsuarioSQL", "🔍 Buscando usuario: $email")

        var usuario: UsuarioNube? = null
        var connection = ConexionSQLServer.obtenerConexion()

        try {
            if (connection != null) {
                val query = """
                    SELECT IdUsuario, NombreCompleto, Email, IdRol, IdEmpresa, 
                           Activo, CuentaVerificada, Contrasena
                    FROM Usuario 
                    WHERE Email = ? AND Contrasena = ? AND Activo = 1
                """

                val statement: PreparedStatement = connection.prepareStatement(query)
                statement.setString(1, email)
                statement.setString(2, password) // En producción, usar hash

                val resultSet: ResultSet = statement.executeQuery()

                if (resultSet.next()) {
                    usuario = UsuarioNube().apply {
                        Id = resultSet.getInt("IdUsuario").toString()
                        NombreCompleto = resultSet.getString("NombreCompleto")
                        Email = resultSet.getString("Email")
                        IdRol = resultSet.getInt("IdRol").toString()
                        IdEmpresa = resultSet.getInt("IdEmpresa").toString()
                        Activo = resultSet.getBoolean("Activo")
                        CuentaVerificada = resultSet.getBoolean("CuentaVerificada")
                        Password = resultSet.getString("Contrasena")
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

        return@withContext usuario
    }
}