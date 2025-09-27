package com.example.negociomx_hyundai.BE

data class VehiculoPasoLog(
    var Id: String = "",
    var VIN: String = "",
    var Marca: String = "",
    var Modelo: String = "",
    var Anio: Int = 0,
    var ColorExterior: String = "",
    var ColorInterior: String = "",
    var Placa: String = "",
    var NumeroSerie: String = "",
    var IdEmpresa: String = "",
    var Activo: Boolean = true,
    var FechaCreacion: String = "",
    var FechaModificacion: String = "",
    var TipoCombustible:String="",
    var TipoVehiculo:String="",
    var BL:String="",

    // ✅ CAMPOS SOC (State of Charge)
    var IdPasoLogVehiculo:Int=0,
    var IdStatusActual:Int?=null,
    var Especificaciones:String=""
)
