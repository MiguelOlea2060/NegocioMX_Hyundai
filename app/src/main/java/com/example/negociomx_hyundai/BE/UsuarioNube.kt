package com.example.negociomx_hyundai.BE

data class UsuarioNube(
    var Id: String? = null,
    var IdLocal: String? = null,
    var NombreCompleto: String? = null,
    var Email: String? = null,
    var Password: String? = null,
    var IdRol: String? = null,
    var IdEmpresa: String? = null,
    var Activo: Boolean? = null,
    var CuentaVerificada: Boolean? = null,
    var NombreCuentaVerificada: String? = null,

    var RazonSocialEmpresa:String="",
    var NombreComercialEmpresa:String="",
    var RfcEmpresa:String=""
)
//DATA CLASE CREADA POR MIGUEL