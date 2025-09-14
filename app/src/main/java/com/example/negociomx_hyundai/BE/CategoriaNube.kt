package com.example.negociomx_hyundai.BE

data class CategoriaNube(
    var Id:String?=null,
    var IdLocal:String?=null,
    var Nombre:String?="",
    var Activa:Boolean?=null,
    var Predeterminada:Boolean?=null,
    var IdEmpresaNube:String?=null,
    var Orden:Int?=null,
    var Tabla:String?=null,
)
