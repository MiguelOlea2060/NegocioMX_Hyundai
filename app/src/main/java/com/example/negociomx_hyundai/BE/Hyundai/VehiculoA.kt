package com.example.negociomx_hyundai.BE.Hyundai

data class VehiculoA(
    var IdVehiculo: Int = 0,
    var VIN: String = "",
    var Marca: String = "",
    var Modelo: String = "",
    var Anio: Int = 0,
    var Placa: String = "",
    var IdEmpresa: String = "",
    var FechaCreacion: String = "",
    var BL:String="",

    // ✅ CAMPOS SOC (State of Charge)
    var Especificaciones: String = "",
)
