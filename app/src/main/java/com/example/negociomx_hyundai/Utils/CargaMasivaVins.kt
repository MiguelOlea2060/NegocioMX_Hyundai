package com.example.negociomx_hyundai.Utils

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import com.example.negociomx_hyundai.BE.Hyundai.VehiculoA
import com.example.negociomx_hyundai.DAL.DALVehiculo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CargaMasivaVins(private val context: Context) {

    companion object {
        private const val TAG = "CargaMasivaVINs"
    }

    suspend fun descargarFotosVehiculo(
        registros: MutableList<VehiculoA>,
        maxPaquete:Int,
        onProgress: (String, String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {

        try {
            Log.d(TAG, "🚀 Iniciando carga masiva de VINs : ${registros.count()}")

            var vinesGuardados = 0
            var vinesRepetidos = 0
            var dalVeh=DALVehiculo()

            var contador=0
            var salir=false
            var cantidadPaquete=0
            var totalRegistros=registros.count()
            do {
                var pedazo=registros.take(maxPaquete).toMutableList()
                if(pedazo==null || pedazo.count()==0)
                {
                    salir=true
                }
                else {
                    cantidadPaquete=pedazo.count()
                    val vinsCad=""
                    withContext(Dispatchers.Main) {
                        onProgress(
                            "Procesando ${(cantidadPaquete+contador)} de ${totalRegistros} registro(s) -> Vins ${vinsCad}",
                            ""
                        )
                    }
                    try {
                        var respuesta = dalVeh.addVehiculoCargaMasiva(pedazo)
                        if (respuesta != null && !respuesta.TieneError) {
                            if (respuesta.Data.isNotEmpty()) {
                                val cantidadGuardados=respuesta.Data.toInt()
                                vinesGuardados+=cantidadGuardados
                                Log.d(TAG, "✅ ${pedazo.count()} VINs procesados correctamente")
                            } else
                                vinesRepetidos++
                        } else {
                            Log.e(TAG, "❌ Error al guardar ${pedazo.count()} VINs")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "💥 Excepción descargando foto $contador: ${e.message}")
                    }

                    contador+=cantidadPaquete
                    var contadorAux=0
                    while (contadorAux<maxPaquete) {
                        if(registros.count()>0)
                            registros.removeAt(0)
                        contadorAux++
                    }
                }
            }while(!salir)

            val mensaje = buildString {
                append("📊 RESUMEN:\n")
                append("📥 VINs Guardados : $vinesGuardados\n")
                append("VINs Repetidos : $vinesRepetidos\n")
                append("💡 Los VINs se han guardado en el sistema\n")
            }

            withContext(Dispatchers.Main) {
                onComplete(true, mensaje)
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error general en descarga: ${e.message}")
            withContext(Dispatchers.Main) {
                onComplete(false, "Error general: ${e.message}")
            }
        }
    }

    private suspend fun descargarArchivo(url: String, archivoDestino: File): Boolean {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream: InputStream = connection.inputStream
                val outputStream = FileOutputStream(archivoDestino)

                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()
                connection.disconnect()

                true
            } else {
                Log.e(TAG, "❌ Error HTTP: ${connection.responseCode} para URL: $url")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error descargando archivo: ${e.message}")
            false
        }
    }

}