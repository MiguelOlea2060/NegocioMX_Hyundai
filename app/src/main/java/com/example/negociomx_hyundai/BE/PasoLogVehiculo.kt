package com.example.negociomx_hyundai.BE

data class PasoLogVehiculo(
    var IdPasoLogVehiculo: Int? = null,
    var IdVehiculo: Int? = null,
    var IdStatusActual: Int? = null,
    var FechaAlta: String? = null,
    var IdUsuarioAlta: Int? = null,

    var Vin:String="",
    var BL:String="",
    var Marca:String="",
    var Modelo:String="",
    var Anio:Short=0,
    var NombreStatus:String="",
    var NombreUsuarioAlta:String="",
    var CantidadStatus:Short=0,
    var FechaEntrada:String="",
    var FechaSalida:String="",

    var Detalles:MutableList<PasoLogVehiculoDet>?=null
)