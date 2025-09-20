package com.example.negociomx_hyundai.BLL

import com.example.negociomx_hyundai.BE.Bloque
import com.example.negociomx_hyundai.BE.PosicionBloque

class BLLBloque {

    public fun getPosicionesDisponiblesDeBloque(bloque:Bloque):List<PosicionBloque>?
    {
        var res:MutableList<PosicionBloque>?=null

        res= mutableListOf()
        var columna: Short = 1
        var fila: Short = 1
        var nombre: String = ""
        while (columna <= bloque.NumColumnas) {
            fila = 1
            while (fila <= bloque.NumFilas) {
                var existe = false
                if (bloque.Ocupadas != null && bloque.Ocupadas?.count()!! > 0)
                    existe = bloque.Ocupadas?.filter { it ->
                        it.NumFila == fila && it.NumColumna == columna
                                && it.IdBloque ==bloque.IdBloque
                    }?.count()!! > 0

                if (!existe) {
                    nombre = "Col -> ${columna} - Fila -> ${fila}"
                    res.add(
                        PosicionBloque(
                            IdBloque =bloque.IdBloque,
                            Nombre = nombre,
                            Fila = fila,
                            Columna = columna
                        )
                    )
                }
                fila++
            }
            columna++
        }
        return  res
    }
}