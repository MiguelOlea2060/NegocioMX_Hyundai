package com.example.negociomx_hyundai.BE

data class PasoLogVehiculo(
    var IdPasoLogVehiculo: Int? = null,
    var IdVehiculo: Int? = null,
    var IdStatusActual: Int? = null,
    var FechaAlta: String? = null,
    var IdUsuarioAlta: Int? = null
)