package com.example.negociomx_hyundai.DAL

import android.util.Log
import com.example.negociomx_hyundai.BE.Cliente
import com.example.negociomx_hyundai.BE.ClienteEmpleado
import com.example.negociomx_hyundai.BE.VehiculoPlacasDueno
import com.example.negociomx_hyundai.Utils.ConexionSQLServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class DALCliente {

    // CONSULTAR TRANSPORTISTAS (EMPRESAS)
    suspend fun consultarTransportistas(conEmpleados:Boolean): List<Cliente> = withContext(Dispatchers.IO) {
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

            var query = "SELECT c.idcliente, Nombre, Rfc FROM dbo.Cliente c WHERE substring(c.Tabla,5,1)='1'"
            if(conEmpleados)
            {
                query = "SELECT c.idcliente, Nombre, Rfc, ce.IdClienteEmpleado, ce.NombreCompleto, ce.Celular  \n" +
                        "FROM dbo.Cliente c left join dbo.ClienteEmpleado ce on c.IdCliente=ce.IdCliente WHERE substring(c.Tabla,5,1)='1'"
            }
            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                var ce:ClienteEmpleado?=null
                var cliente = Cliente(
                    IdCliente = resultSet.getInt("idcliente"),
                    Nombre = resultSet.getString("Nombre") ?: "",
                    Rfc = resultSet.getString("Rfc") ?: ""
                )
                if(conEmpleados)
                {
                    ce=ClienteEmpleado(
                        IdClienteEmpleado = resultSet.getInt("IdClienteEmpleado")?:0,
                        NombreCompleto = resultSet.getString("NombreCompleto")?:"",
                        Celular = resultSet.getString("Celular")?:"")
                }
                var existe=false
                transportistas.forEach{
                    if(it.IdCliente==cliente.IdCliente) {
                        existe = true
                        cliente=it
                    }
                }
                if(!existe) {
                    if(conEmpleados && ce!=null) {
                        cliente.Empleados= mutableListOf<ClienteEmpleado>()
                        cliente.Empleados?.add(ce)
                    }
                    transportistas.add(cliente)
                }
                else
                {
                    if(ce!=null) {
                        if (cliente?.Empleados == null) cliente?.Empleados =
                            mutableListOf<ClienteEmpleado>()
                        cliente?.Empleados!!.add(ce)
                    }
                }
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

    suspend fun consultarTransportistasConPlacasNumEco(conEmpleados:Boolean): List<Cliente> = withContext(Dispatchers.IO) {
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

            var query = "SELECT c.idcliente, Nombre, Rfc FROM dbo.Cliente c WHERE substring(c.Tabla,5,1)='1'"
            if(conEmpleados)
            {

                query = "SSELECT c.idcliente, Nombre, Rfc, ce.IdClienteEmpleado, ce.NombreCompleto, ce.Celular, vpd.idvehiculoplacasdueno, vp.IdVehiculoPlacas\n" +
                        ", vp.Placas, vp.NumeroEconomico " +
                        "FROM dbo.Cliente c left join dbo.ClienteEmpleado ce on c.IdCliente=ce.IdCliente " +
                        "left join dbo.VehiculoPlacasDueno vpd on vpd.idtipopersonadueno=1 and c.IdCliente=vpd.idpersona " +
                        "left join dbo.VehiculoPlacas vp on vpd.idvehiculoplacas=vp.IdVehiculoPlacas " +
                        "WHERE substring(c.Tabla,5,1)='1'"
            }
            statement = conexion.prepareStatement(query)
            resultSet = statement.executeQuery()

            while (resultSet.next()) {
                var ce:ClienteEmpleado?=null
                var vp:VehiculoPlacasDueno?=null
                var cliente = Cliente(
                    IdCliente = resultSet.getInt("idcliente"),
                    Nombre = resultSet.getString("Nombre") ?: "",
                    Rfc = resultSet.getString("Rfc") ?: ""
                )
                if(conEmpleados)
                {
                    ce=ClienteEmpleado(
                        IdClienteEmpleado = resultSet.getInt("IdClienteEmpleado")?:0,
                        NombreCompleto = resultSet.getString("NombreCompleto")?:"",
                        Celular = resultSet.getString("Celular")?:"")
                }
                var idVehiculoPlacasDueno:Int=resultSet.getInt("IdVehiculoPlacasDueno")?:0
                if(idVehiculoPlacasDueno>0) {
                    vp = VehiculoPlacasDueno(
                        IdVehiculoPlacasDueno = idVehiculoPlacasDueno,
                        IdVehiculoPlacas = resultSet.getInt("IdVehiculoPlacas") ?: 0,
                        Placas = resultSet.getString("Placas") ?: "",
                        NumeroEconomico = resultSet.getString("NumeroEconomico") ?: ""
                    )
                }

                var existe=false
                transportistas.forEach{
                    if(it.IdCliente==cliente.IdCliente) {
                        existe = true
                        cliente=it
                    }
                }
                if(!existe) {
                    if(conEmpleados && ce!=null) {
                        cliente.Empleados= mutableListOf<ClienteEmpleado>()
                        cliente.Empleados?.add(ce)
                    }
                    transportistas.add(cliente)
                    if(vp!=null)
                    {
                        cliente.Placas= mutableListOf()
                    }
                }
                else
                {
                    if(ce!=null) {
                        if (cliente?.Empleados == null) cliente?.Empleados =
                            mutableListOf<ClienteEmpleado>()
                        cliente?.Empleados!!.add(ce)
                    }
                    if(vp!=null)
                    {
                        cliente.Placas?.add(vp)
                    }
                }
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

            val query = "SELECT CE.IdClienteEmpleado, ce.NombreCompleto, ce.Celular " +
                        "FROM dbo.ClienteEmpleado ce WHERE CE.IdCliente=?"
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