package com.example.negociomx_hyundai.BE

data class EmpresaTaller(
    var IdEmpresa: Int = 0,
    var Nombre: String = "",
    var Tipo: String = "", // "INTERNA" o "EXTERNA"
    var Activa: Boolean = false,
    var Empleados: MutableList<Empleado>? = null
)