package com.example.negociomx_hyundai.Utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class BLLUtils {

    companion object {

        /**
         * Convierte un archivo de imagen a Base64
         * @param archivo Archivo de imagen a convertir
         * @return String en formato Base64 o null si hay error
         */
        fun convertirImagenABase64(archivo: File): String? {
            return try {
                val bytes = archivo.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e("BLLUtils", "Error convirtiendo imagen a Base64: ${e.message}")
                null
            }
        }

        /**
         * Convierte un Bitmap a Base64
         * @param bitmap Bitmap a convertir
         * @return String en formato Base64 o null si hay error
         */
        fun convertirBitmapABase64(bitmap: Bitmap): String? {
            return try {
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val bytes = outputStream.toByteArray()
                android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e("BLLUtils", "Error convirtiendo Bitmap a Base64: ${e.message}")
                null
            }
        }

        /**
         * Guarda un Bitmap en la galería del dispositivo
         * @param context Contexto de la aplicación
         * @param bitmap Bitmap a guardar
         * @param nombreCarpeta Nombre de la carpeta donde se guardará
         * @param nombreArchivo Nombre del archivo
         * @return Uri del archivo guardado o null si hay error
         */
        fun saveBitmapToFile(
            context: Context,
            bitmap: Bitmap,
            nombreCarpeta: String,
            nombreArchivo: String
        ): Uri? {
            var imgUri: Uri? = null

            try {
                imgUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/$nombreCarpeta")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val mkDir = File("${Environment.DIRECTORY_PICTURES}/$nombreCarpeta")
                if (!mkDir.exists()) {
                    mkDir.mkdirs()
                }

                imgUri = context.contentResolver.insert(imgUri, contentValues)

                imgUri?.let { uri ->
                    context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(imgUri!!, contentValues, null, null)
                }

            } catch (ex: Exception) {
                Log.e("BLLUtils", "Error guardando bitmap: ${ex.message}")
            }

            return imgUri
        }

        /**
         * Comprime una imagen y la guarda en un archivo temporal
         * @param context Contexto de la aplicación
         * @param archivoOriginal Archivo original a comprimir
         * @return Archivo comprimido
         */
        fun comprimirImagen(context: Context, archivoOriginal: File): File {
            return try {
                val bitmap = BitmapFactory.decodeFile(archivoOriginal.absolutePath)

                val maxSize = 3072
                val ratio: Float = if (bitmap.width > bitmap.height) {
                    maxSize.toFloat() / bitmap.width
                } else {
                    maxSize.toFloat() / bitmap.height
                }

                val newWidth = (bitmap.width * ratio).toInt()
                val newHeight = (bitmap.height * ratio).toInt()

                val bitmapRedimensionado = Bitmap.createScaledBitmap(
                    bitmap, newWidth, newHeight, true
                )

                val timeStamp = java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                val archivoComprimido = File(
                    context.getExternalFilesDir(null),
                    "compressed_$timeStamp.jpg"
                )

                val outputStream = FileOutputStream(archivoComprimido)
                bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.close()

                bitmap.recycle()
                bitmapRedimensionado.recycle()

                Log.d("BLLUtils", "Imagen comprimida: ${archivoComprimido.length()} bytes")
                archivoComprimido

            } catch (e: Exception) {
                Log.e("BLLUtils", "Error comprimiendo imagen: ${e.message}")
                archivoOriginal
            }
        }
    }
}