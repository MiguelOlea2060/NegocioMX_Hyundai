package com.example.negociomx_hyundai.BE

import java.io.Serializable

data class PosicionBloque(
    var IdBloque:Short=0,
    var Columna:Short=0,
    var Fila:Short=0,
    var Nombre:String=""
) : Serializable
