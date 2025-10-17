package com.example.negociomx_hyundai.BE

data class PasoLogVehiculoPDI(
    var IdPasoLogVehiculoPDI: Int = 0,
    var IdPasoLogVehiculo: Int = 0,
    var IdTipoEvidencia: Short = 1, // 1 = Inspección de entrada
    var IdParteDanno: Short? = null,
    var IdGradoSeveridad: Short? = null,
    var Consecutivo: Short = 0, // Número de foto (1-5)
    var IdDescEvidencia: Int? = null,
    var IdTransporte: Int = 0,
    var IdEmpleadoTransporte: Int? = null,
    var NombreFotoEvidencia: String = "",

    // Campos adicionales para manejo en la app (no se guardan en BD)
    var FotoBase64: String? = null, // Para almacenar temporalmente la foto
    var RutaArchivoLocal: String? = null // Ruta del archivo guardado localmente
)