package com.example.negociomx_hyundai.BE

data class Cliente(
    var IdCliente: Int? = null,
    var Nombre: String? = null,
    var Rfc: String? = null,
    var Activo: Boolean = false,
    var Tipo: String = "", // "INTERNA" o "EXTERNA",

    var Empleados:MutableList<ClienteEmpleado>?= null,
    var Placas:MutableList<VehiculoPlacasDueno>?=null
)