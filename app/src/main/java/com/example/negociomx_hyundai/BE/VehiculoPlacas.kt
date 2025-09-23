package com.example.negociomx_hyundai.BE

data class VehiculoPlacas(
    var IdVehiculoPlacas:Int?=null,
    var IdVehiculo:Int?=null,
    var Placas:String="",
    var NumeroEconomico:String="",
    var Activo:Boolean?=null,
    var Annio:Short?=null,
    var IdTipoPersonaDueno:Short=0,
    var IdPersona:Int=0
)
