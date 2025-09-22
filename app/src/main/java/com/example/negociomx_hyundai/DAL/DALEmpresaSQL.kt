package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import com.example.negociomx_hyundai.room.entities.Admins.Empresa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALEmpresaSQL {
    suspend fun consultarEmppresas(idTipoEmpleado:Int?): List<Empresa> = withContext(
        Dispatchers.IO) {
        val empleados = mutableListOf<Empresa>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALEMPLEADO", "🔍 Consultando los empleados por tipoempleado: $idTipoEmpleado")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALCliente", "❌ No se pudo obtener conexión")
                return@withContext empleados
            }

            var query = "select IdEmpleado, nombres, apellidopaterno, apellidomaterno, IdTipoEmpleado\n" +
                    "from dbo.Empleado where substring(tabla, 2,1)='1' \n" +
                    "and IdStatus=1"
            if(idTipoEmpleado!=null)
                query += " and IdTipoEmpleado = ?"

            statement = conexion.prepareStatement(query)
            if (idTipoEmpleado!=null)
                statement.setInt(1, idTipoEmpleado)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                var emp = Empresa(
                    /*IdEmpleado = resultSet.getInt("IdEmpleado"),
                    Nombres = resultSet.getString("Nombres") ?: "",
                    ApellidoPaterno = resultSet.getString("ApellidoPaterno") ?: "",
                    ApellidoMaterno = resultSet.getString("ApellidoPaterno") ?: "",
                    IdTipoEmpleado = resultSet.getInt("IdTipoEmpleado")?:0*/
                )
                /*emp.NombreCompleto=emp.Nombres
                if(emp.ApellidoPaterno.isNotEmpty())
                    emp.NombreCompleto+=" "+emp.ApellidoPaterno
                if(emp.ApellidoMaterno.isNotEmpty())
                    emp.NombreCompleto+=" "+emp.ApellidoMaterno
*/
                empleados.add(emp)
            }

            Log.d("DALEMPLEADO", "✅ Se obtuvieron ${empleados.size} empleados")
        } catch (e: Exception) {
            Log.e("DALEMPLEADO", "💥 Error consultando empleados: ${e.message}")
            e.printStackTrace()
        } finally {
            try {
                resultSet?.close()
                statement?.close()
                conexion?.close()
            } catch (e: Exception) {
                Log.e("DALEMPLEADO", "Error cerrando recursos: ${e.message}")
            }
        }

        return@withContext empleados
    }

}