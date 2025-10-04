package com.example.negociomx_hyundai.BE

data class ResumenCompletoConQuery(
    // Datos del vehículo (se toman de la primera fila)
    var VIN: String = "",
    var IdVehiculo: Int = 0,
    var IdMarca: Int = 0,
    var Marca: String = "",
    var IdModelo: Int = 0,
    var Modelo: String = "",
    var BL: String = "",
    var IdColorExterior: Int = 0,
    var ColorExterior: String = "",
    var IdColorInterior: Int = 0,
    var ColorInterior: String = "",
    var IdStatusActual: Int = 0,
    var NombreStatusActual: String = "",

    // Lista de movimientos (una entrada por cada fila del query)
    var Movimientos: MutableList<MovimientoCompleto> = mutableListOf()
)

data class MovimientoCompleto(
    var IdTransporte: Int? = null,
    var NombreTransporte: String = "",
    var IdStatusMovimiento: Int = 0,
    var NombreStatusMovimiento: String = "",
    var FechaMovimiento: String = "",
    var IdBloque: Int? = null,
    var NombreBloque: String = "",
    var Columna: Short? = null,
    var Fila: Short? = null,
    var IdParteDanno: Int? = null,
    var NombreParteDanno: String = "",
    var IdTipoMovimiento: Int? = null,
    var NombreTipoMovimiento: String = ""
)