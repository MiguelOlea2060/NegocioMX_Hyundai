package com.example.negociomx_hyundai.BE

data class ActividadDiariaItem(
    var IdVehiculo: Int = 0,
    var IdPasoLogVehiculo: Int = 0,
    var VIN: String = "",
    var BL: String = "",
    var Marca: String = "",
    var Modelo: String = "",
    var Anio: Int = 0,
    var ColorExterior: String = "",
    var ColorInterior: String = "",

    // Datos de la actividad
    var IdStatus: Int = 0,
    var NombreStatus: String = "",
    var FechaMovimiento: String = "",
    var UsuarioMovimiento: String = "",
    var Bloque: String = "",
    var Fila: Int? = null,
    var Columna: Int? = null,
    var PersonaQueHaraMovimiento: String = "",
    var Observacion: String = "",
    var PlacaTransporte: String = "",
    var NumeroEconomico: String = ""
)