package com.example.negociomx_hyundai.Utils

import java.text.SimpleDateFormat
import java.util.Locale

class Utils {
    public fun formatearFecha(fecha: String): String {
        return formatearFecha(fecha,"")
    }

    public fun formatearFecha(fecha: String, formato:String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            var formatoSalida = SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault())
            if(formato.isNotEmpty())
                formatoSalida = SimpleDateFormat(formato, Locale.getDefault())

            val date = formatoEntrada.parse(fecha)
            formatoSalida.format(date!!)
        } catch (e: Exception) {
            fecha
        }
    }

}