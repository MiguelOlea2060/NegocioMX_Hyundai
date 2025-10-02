package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.Empleado
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALEmpleadoSQL {
    // CONSULTAR EMPLEADOS INTERNOS DE LA EMPRESA POR TIPO DE EMPLEADO
    suspend fun consultarEmpleados(idTipoEmpleadoConductor:Int?, idTipoEmpleadoTaller :Int?):
            List<Empleado> = withContext(
        Dispatchers.IO) {
        val empleados = mutableListOf<Empleado>()
        var conexion: Connection? = null
        var statement: PreparedStatement? = null
        var resultSet: ResultSet? = null

        try {
            Log.d("DALEMPLEADO", "🔍 Consultando los empleados por tipoempleado: $idTipoEmpleadoConductor")

            conexion = ConexionSQLServer.obtenerConexion()
            if (conexion == null) {
                Log.e("DALCliente", "❌ No se pudo obtener conexión")
                return@withContext empleados
            }

            var query = "select IdEmpleado, nombres, apellidopaterno, apellidomaterno, IdTipoEmpleado\n" +
                    "from dbo.Empleado where substring(tabla, 2,1)='1' \n" +
                    "and IdStatus=1"
            if(idTipoEmpleadoConductor!=null)
                query += " and (IdTipoEmpleado =? "
            if(idTipoEmpleadoTaller!=null)
                query += " or idtipoempleado= ?)"
            else
                query += " )"


            statement = conexion.prepareStatement(query)
            if (idTipoEmpleadoConductor!=null)
                statement.setInt(1, idTipoEmpleadoConductor)
            if (idTipoEmpleadoTaller!=null)
                statement.setInt(2, idTipoEmpleadoTaller)

            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                var empleado = Empleado(
                    IdEmpleado = resultSet.getInt("IdEmpleado"),
                    Nombres = resultSet.getString("Nombres") ?: "",
                    ApellidoPaterno = resultSet.getString("ApellidoPaterno") ?: "",
                    ApellidoMaterno = resultSet.getString("ApellidoPaterno") ?: "",
                    IdTipoEmpleado = resultSet.getInt("IdTipoEmpleado")?:0
                )
                empleado.NombreCompleto=empleado.Nombres
                if(empleado.ApellidoPaterno.isNotEmpty())
                    empleado.NombreCompleto+=" "+empleado.ApellidoPaterno
                if(empleado.ApellidoMaterno.isNotEmpty())
                    empleado.NombreCompleto+=" "+empleado.ApellidoMaterno

                empleados.add(empleado)
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