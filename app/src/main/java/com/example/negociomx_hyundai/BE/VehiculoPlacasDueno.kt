package com.example.negociomx_hyundai.BE

data class VehiculoPlacasDueno(
    var IdVehiculoPlacasDueno:Int=0,
    var IdVehiculoPlacas:Int?=null,
    var Placas:String="",
    var NumeroEconomico:String=""
)
