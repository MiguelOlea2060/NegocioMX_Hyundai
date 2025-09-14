package com.example.negociomx_hyundai.room.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.negociomx_hyundai.room.daos.Admins.ConfigDAO
import com.example.negociomx_hyundai.room.daos.Admins.CfgNVDAO
import com.example.negociomx_hyundai.room.daos.Admins.EmpresaDAO
import com.example.negociomx_hyundai.room.daos.ArticuloDAO
import com.example.negociomx_hyundai.room.daos.CategoriaDAO
import com.example.negociomx_hyundai.room.daos.ClienteDAO
import com.example.negociomx_hyundai.room.daos.DocumentoDAO
import com.example.negociomx_hyundai.room.daos.ImpuestoDAO
import com.example.negociomx_hyundai.room.daos.MarcaDAO
import com.example.negociomx_hyundai.room.daos.TipoPagoDAO
import com.example.negociomx_hyundai.room.daos.UnidadMedidaDAO
import com.example.negociomx_hyundai.room.entities.Admins.CfgNV
import com.example.negociomx_hyundai.room.entities.Admins.Config
import com.example.negociomx_hyundai.room.entities.Admins.Empresa
import com.example.negociomx_hyundai.room.entities.Articulo
import com.example.negociomx_hyundai.room.entities.Categoria
import com.example.negociomx_hyundai.room.entities.Cliente
import com.example.negociomx_hyundai.room.entities.Documento
import com.example.negociomx_hyundai.room.entities.DocumentoDetalle
import com.example.negociomx_hyundai.room.entities.Impuesto
import com.example.negociomx_hyundai.room.entities.Marca
import com.example.negociomx_hyundai.room.entities.PagoDocumento
import com.example.negociomx_hyundai.room.entities.TipoPago
import com.example.negociomx_hyundai.room.entities.UnidadMedida

@Database(entities = [Empresa::class, UnidadMedida::class, Categoria::class, Articulo::class, Cliente::class, Documento::class,
    DocumentoDetalle::class, Impuesto::class, TipoPago::class, Config::class,CfgNV::class, PagoDocumento::class, Marca::class],
    version = 1)
abstract class POSDatabase:RoomDatabase() {
    abstract fun unidadMedidaDAO():UnidadMedidaDAO
    abstract fun categoriaDAO():CategoriaDAO
    abstract fun articuloDAO():ArticuloDAO
    abstract fun clienteDAO():ClienteDAO
    abstract fun documentoDAO():DocumentoDAO
    abstract fun impuestoDAO():ImpuestoDAO
    abstract fun tipoPagoDAO():TipoPagoDAO
    abstract fun configDAO():ConfigDAO
    abstract fun cfgNVDAO():CfgNVDAO
    abstract fun empresaDAO():EmpresaDAO
    abstract fun marcaDAO(): MarcaDAO

    companion object{
        fun getDatabase(ctx:Context):POSDatabase
        {
            val db = Room.databaseBuilder(ctx, POSDatabase::class.java, "NegocioMX").build()
            return db
        }
    }
}