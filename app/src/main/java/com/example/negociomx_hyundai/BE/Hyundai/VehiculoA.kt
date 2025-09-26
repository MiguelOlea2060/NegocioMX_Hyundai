package com.example.negociomx_hyundai.BE.Hyundai

import androidx.camera.core.processing.SurfaceProcessorNode.In

data class VehiculoA(
    var IdVehiculo: Int = 0,
    var VIN: String = "",
    var IdMarca:Int=-1,
    var Marca: String = "",
    var IdModelo:Int=-1,
    var Modelo: String = "",
    var Anio: Int = 0,
    var Placa: String = "",
    var IdEmpresa: String = "",
    var FechaCreacion: String = "",
    var BL:String="",

    // ✅ CAMPOS SOC (State of Charge)
    var Especificaciones: String = "",
)
