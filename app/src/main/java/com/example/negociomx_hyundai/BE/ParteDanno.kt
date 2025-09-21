package com.example.negociomx_hyundai.BE

data class ParteDanno(
    var IdParteDanno: Int = 0,
    var Nombre: String = "",
    var Activa: Boolean = false,
    var IdTabla: Int? = null,
    var ImagenUrl: String = "", // Para las imágenes de las partes
    var Seleccionada: Boolean = false // Para el checkbox
)