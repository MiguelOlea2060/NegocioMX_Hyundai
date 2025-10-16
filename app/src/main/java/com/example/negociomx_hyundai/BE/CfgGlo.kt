package com.example.negociomx_hyundai.BE

data class CfgGlo(
    var IdCfgApp:Short?=null,
    var IdConfiguracion:Int=0,
    var ManejaSeleccionBloquePosXTablero:Boolean=false,
    var IdEmpresa:Int?=null,
    var RfcEmpresa:String="",

    ////Esto se manejara para guardar los archivos fotos o lo que sea dentro de BD
    var ManejaGuardadoArchivosEnBD:Boolean?=null,
    var ManejaSeleccionObsMovimientoLocal:Boolean?=null,
    var ManejaSeleccionObsEnTaller:Boolean?=null,
    var FormatoCarpetaArchivos:String="",
    var urlGuardadoArchivos:String="",
    var ReglasNotificaciones :String="",

    var ManejaStatusInspeccionEntrada:Boolean?=null,
    var ManejaStatusInspeccionSalida:Boolean?=null,
    var ManejaFotosEnInspeccionEntrada:Boolean?=null,
    var ManejaFotosEnInspeccionSalida:Boolean?=null,
    var ManejaNotificaciones:Boolean?=null,
    var ManejaNotififacionesEnWeb:Boolean?=null
)
