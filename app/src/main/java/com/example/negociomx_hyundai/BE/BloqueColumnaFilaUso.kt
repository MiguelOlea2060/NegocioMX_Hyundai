package com.example.negociomx_hyundai.BE

data class BloqueColumnaFilaUso(
    var IdBloqueColumnaFilaUso:Short=0,
    var IdBloque:Short=0,
    var NumColumna:Short=0,
    var NumFila:Short=0,
    var Nombre:String="",
    var Activa:Boolean=false,
    var IdVehiculo:Int?=null,
)
