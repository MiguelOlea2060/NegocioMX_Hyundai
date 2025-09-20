package com.example.negociomx_hyundai.BE

data class Empleado(
    var IdEmpleado:Int=0,
    var Nombres:String="",
    var ApellidoPaterno:String="",
    var ApellidoMaterno:String="",
    var IdStatus:Int=0,
    var IdTipoEmpleado:Int=0,
    var NombreCompleto:String=""
)
