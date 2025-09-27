package com.example.negociomx_hyundai.BE

import java.io.Serializable

data class BloqueColumnaFilaUso(
    var IdBloqueColumnaFilaUso: Short = 0,
    var IdBloque: Short = 0,
    var NumColumna: Short = 0,
    var NumFila: Short = 0,
    var Nombre: String = "",
    var IdVehiculo: Int? = null,
    var Activa: Boolean = false
) : Serializable