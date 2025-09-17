package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.Cliente
import com.example.negociomx_hyundai.BE.ClienteEmpleado
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALCliente {

    // CONSULTAR TRANSPORTISTAS (EMPRESAS)
    suspend fun consultarTransportistas(): List<Cliente> = withContext(Dispatchers.IO) {
        val transportistas = mutableListOf<Cliente>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALCliente", "🔍 Consultando transportistas...")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALCliente", "❌ No se pudo obtener conexión")
                return@withContext transportistas
            }

            val query = "SELECT c.idcliente, Nombre, Rfc FROM dbo.Cliente c WHERE substring(c.Tabla,5,1)='1'"
            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val cliente = Cliente(
                    IdCliente = resultSet.getInt("idcliente"),
                    Nombre = resultSet.getString("Nombre") ?: "",
                    Rfc = resultSet.getString("Rfc") ?: ""
                )
                transportistas.add(cliente)
            }

            Log.d("DALCliente", "✅ Se obtuvieron ${transportistas.size} transportistas")

        } catch (e: Exception) {
            Log.e("DALCliente", "💥 Error consultando transportistas: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALCliente", "Error cerrando recursos: ${e.message}")
            }
        }

        return@withContext transportistas
    }

    // CONSULTAR EMPLEADOS DE UN TRANSPORTISTA
    suspend fun consultarEmpleadosTransportista(idCliente: Int): List<ClienteEmpleado> = withContext(Dispatchers.IO) {
        val empleados = mutableListOf<ClienteEmpleado>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALCliente", "🔍 Consultando empleados del cliente: $idCliente")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALCliente", "❌ No se pudo obtener conexión")
                return@withContext empleados
            }

            val query = "SELECT CE.IdClienteEmpleado, ce.NombreCompleto, ce.Celular FROM dbo.ClienteEmpleado ce WHERE CE.IdCliente=?"
            statement = conexion.prepareStatement(query)
            statement.setInt(1, idCliente)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                val empleado = ClienteEmpleado(
                    IdClienteEmpleado = resultSet.getInt("IdClienteEmpleado"),
                    NombreCompleto = resultSet.getString("NombreCompleto") ?: "",
                    Celular = resultSet.getString("Celular") ?: "",
                    IdCliente = idCliente
                )
                empleados.add(empleado)
            }

            Log.d("DALCliente", "✅ Se obtuvieron ${empleados.size} empleados")

        } catch (e: Exception) {
            Log.e("DALCliente", "💥 Error consultando empleados: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALCliente", "Error cerrando recursos: ${e.message}")
            }
        }

        return@withContext empleados
    }
}