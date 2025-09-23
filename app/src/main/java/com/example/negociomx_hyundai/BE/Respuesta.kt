package com.example.negociomx_hyundai.BE

import com.google.android.gms.common.internal.Objects

data class Respuesta(
    var TieneError:Boolean=false,
    var Mensaje:String="",
    var Data:String=""
)
