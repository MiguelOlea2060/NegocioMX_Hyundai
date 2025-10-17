package com.example.negociomx_hyundai.Utils

import com.example.negociomx_hyundai.BE.CfgGlo
import com.example.negociomx_hyundai.BE.CfgNVNube
import com.example.negociomx_hyundai.BE.CfgNube
import com.example.negociomx_hyundai.BE.EmpresaNube
import com.example.negociomx_hyundai.BE.MarcaAuto
import com.example.negociomx_hyundai.BE.UsuarioNube
import com.example.negociomx_hyundai.room.entities.Admins.CfgNV
import com.example.negociomx_hyundai.room.entities.Admins.Config
import com.example.negociomx_hyundai.room.entities.Admins.Empresa
import com.example.negociomx_hyundai.room.enums.TipoUsoSistemaEnum
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ParametrosSistema {
    companion object{

        lateinit var usuarioLogueado:UsuarioNube

        lateinit var empresaNube:EmpresaNube
        var empresaLocal:Empresa?=null
        lateinit var cfg:CfgNube
        var cfgLocal:Config?=null
        var CfgGloSql:CfgGlo?=null
        var TipoUsoSistema=TipoUsoSistemaEnum.OnLine
        lateinit var cfgNV:CfgNVNube
        var cfgNVLocal:CfgNV?=null
        val NombreBD:String="NEGOCIOMX-FB"
        var marcaAutoPre=MarcaAuto(IdMarcaAuto = 35, Nombre = "Hyundai")

    }
}