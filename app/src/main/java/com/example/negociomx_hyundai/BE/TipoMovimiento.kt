package com.example.negociomx_hyundai.BE

data class TipoMovimiento(
    var IdTipoMovimiento: Int = 0,
    var Nombre: String = "",
    var Activo: Boolean = false,
    var Tabla: String = ""
)