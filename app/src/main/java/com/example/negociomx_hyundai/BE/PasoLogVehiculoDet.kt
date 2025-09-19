package com.example.negociomx_hyundai.BE

data class PasoLogVehiculoDet(
    var IdPasoLogVehiculoDet: Int? = null,
    var IdPasoLogVehiculo: Int? = null,
    var IdTransporte: Int? = null,
    var Placa: String? = null,
    var NumeroEconomico: String? = null,
    var Bloque: String? = null,
    var Fila: Int? = null,
    var Columna: Int? = null,
    var IdTipoMovimiento: Int? = null,
    var PersonaQueHaraMovimiento: String? = null,
    var IdParteDanno: Int? = null,
    var IdEmpleadoTransporte: Int? = null,
    var IdTipoEntradaSalida: Int? = null, // 1=Rodando, 2=En Madrina
    var IdStatus: Int? = null,
    var FechaMovimiento: String? = null,
    var IdUsuarioMovimiento: Int? = null,
    var Observacion: String? = null,
    var EnviadoAInterface: Boolean? = null,
    var FechaEnviado: String? = null,
    var IdBloque:Short?=null
)