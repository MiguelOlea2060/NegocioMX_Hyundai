package com.example.negociomx_hyundai.BLL

import com.example.negociomx_hyundai.BE.Empleado

class BLLEmpleado {
    fun getEmpleadosPorTipo(idTipoEmpleado:Int,  empleados:List<Empleado>):List<Empleado>
    {
        var lista:MutableList<Empleado>
        lista = empleados.filter { it.IdTipoEmpleado==idTipoEmpleado }.toMutableList()

        return lista;
    }
}