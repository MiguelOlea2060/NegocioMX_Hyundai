package com.example.negociomx_hyundai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.negociomx_hyundai.BE.PasoLogVehiculoPDI
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.databinding.ActivityPasoInspeccionarBinding
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PasoInspeccionar_Activity : AppCompatActivity() {
    private val CAMERA_PERMISSION_CODE = 100

    private lateinit var binding: ActivityPasoInspeccionarBinding
    private var vehiculoActual: VehiculoPasoLog? = null
    private val gson = Gson()
    private val listaFotos = mutableListOf<PasoLogVehiculoPDI?>(null, null, null, null, null)
    private var fotoActualIndex = -1
    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null && fotoActualIndex >= 0) {
                guardarFotoEnLista(imageBitmap, fotoActualIndex)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // <CHANGE> Inicializar binding correctamente
        binding = ActivityPasoInspeccionarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // <CHANGE> Agregar inicialización de componentes
        obtenerDatosVehiculo()
        mostrarInfoVehiculo()
        configurarGridFotos()
        configurarEventos()
    }


    private fun configurarEventos() {
        binding.btnRegresarInspeccionarBottom.setOnClickListener {
            val fotosCapturadas = listaFotos.count { it != null }
            if (fotosCapturadas < 5) {
                Toast.makeText(
                    this,
                    "Debe capturar las 5 fotos antes de regresar. Faltan ${5 - fotosCapturadas} fotos.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                regresarConFotos()
            }
        }

    }
    private fun mostrarInfoVehiculo() {
        vehiculoActual?.let { vehiculo ->
            binding.tvVinVehiculoInspeccionar.text = "VIN: ${vehiculo.VIN}"
            binding.tvBlVehiculoInspeccionar.text = "BL: ${vehiculo.BL}"
            binding.tvMarcaModeloAnnioInspeccionar.text = "${vehiculo.Especificaciones}  Año: ${vehiculo.Anio}"
            binding.tvColorExteriorInspeccionar.text = "Color Ext: ${vehiculo.ColorExterior}"
            binding.tvColorInteriorInspeccionar.text = "Color Int: ${vehiculo.ColorInterior}"
            binding.layoutInfoVehiculoInspeccionar.visibility = View.VISIBLE
        }
    }

    private fun obtenerDatosVehiculo() {
        val jsonVeh = intent.getStringExtra("vehiculo") ?: ""
        if (jsonVeh.isNotEmpty()) {
            vehiculoActual = gson.fromJson(jsonVeh, VehiculoPasoLog::class.java)
            mostrarInfoVehiculo()
        }
    }
    private fun configurarGridFotos() {
        binding.gridFotosInspeccionar.removeAllViews()

        for (i in 0..4) {
            // <CHANGE> Inflar con el parent para obtener los parámetros correctos
            val itemView = layoutInflater.inflate(
                R.layout.item_foto_inspeccion,
                binding.gridFotosInspeccionar,
                false
            )

            // Configurar referencias a los elementos del item
            val tvNumeroFoto = itemView.findViewById<TextView>(R.id.tvNumeroFoto)
            val ivFotoCapturada = itemView.findViewById<ImageView>(R.id.ivFotoCapturada)
            val layoutPlaceholder = itemView.findViewById<LinearLayout>(R.id.layoutPlaceholder)
            val ivCheckCapturada = itemView.findViewById<ImageView>(R.id.ivCheckCapturada)
            val viewBordeEstado = itemView.findViewById<View>(R.id.viewBordeEstado)

            // Configurar número de foto
            tvNumeroFoto.text = "Foto ${i + 1}"

            // <CHANGE> Configurar parámetros del GridLayout para posicionar correctamente
            val params = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(i % 2, 1f) // 2 columnas
                rowSpec = android.widget.GridLayout.spec(i / 2) // Calcular fila automáticamente
                setMargins(8, 8, 8, 8)
            }
            itemView.layoutParams = params

            // Configurar click listener para abrir cámara
            itemView.setOnClickListener {
                abrirCamara(i)
            }

            // Guardar referencia del item para actualizarlo después
            itemView.tag = i

            binding.gridFotosInspeccionar.addView(itemView)
        }

        actualizarContadorFotos()
    }
    private fun abrirCamara(index: Int) {
        // <CHANGE> Verificar permiso de cámara antes de abrir
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permiso
            fotoActualIndex = index // Guardar el índice para usarlo después de conceder permiso
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            // Permiso ya concedido, abrir cámara
            fotoActualIndex = index
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                camaraLauncher.launch(intent)
            } else {
                Toast.makeText(this, "No se encontró aplicación de cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // <CHANGE> Manejar respuesta de solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, abrir cámara
                    if (fotoActualIndex >= 0) {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        if (intent.resolveActivity(packageManager) != null) {
                            camaraLauncher.launch(intent)
                        }
                    }
                } else {
                    // Permiso denegado
                    Toast.makeText(
                        this,
                        "Se necesita permiso de cámara para capturar fotos",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun guardarFotoEnLista(bitmap: Bitmap, index: Int) {
        if (index < 0 || index >= listaFotos.size) {
            Toast.makeText(this, "Error: índice de foto inválido", Toast.LENGTH_SHORT).show()
            return
        }
        val base64 = convertirBitmapABase64(bitmap)
        val foto = PasoLogVehiculoPDI(
            IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo ?: 0,
            IdTipoEvidencia = 1, // Tipo de evidencia para inspección de entrada
            Consecutivo = (index + 1).toShort(),
            NombreFotoEvidencia = "INSPECCION_ENTRADA_${vehiculoActual?.VIN}_${index + 1}",
            FotoBase64 = base64
        )
        listaFotos[index] = foto
        actualizarVistaFoto(index, bitmap)
        actualizarContadorFotos()
    }
    private fun actualizarVistaFoto(index: Int, bitmap: Bitmap) {
        // <CHANGE> Validar índice y existencia del item
        if (index < 0 || index >= binding.gridFotosInspeccionar.childCount) {
            Toast.makeText(this, "Error actualizando vista de foto", Toast.LENGTH_SHORT).show()
            return
        }

        val itemView = binding.gridFotosInspeccionar.getChildAt(index)

        if (itemView != null) {
            val ivFotoCapturada = itemView.findViewById<ImageView>(R.id.ivFotoCapturada)
            val layoutPlaceholder = itemView.findViewById<LinearLayout>(R.id.layoutPlaceholder)
            val ivCheckCapturada = itemView.findViewById<ImageView>(R.id.ivCheckCapturada)
            val viewBordeEstado = itemView.findViewById<View>(R.id.viewBordeEstado)

            // <CHANGE> Validar que todos los elementos existan
            if (ivFotoCapturada != null && layoutPlaceholder != null &&
                ivCheckCapturada != null && viewBordeEstado != null) {

                // Mostrar la foto capturada
                ivFotoCapturada.setImageBitmap(bitmap)
                ivFotoCapturada.visibility = View.VISIBLE

                // Ocultar placeholder
                layoutPlaceholder.visibility = View.GONE

                // Mostrar check verde
                ivCheckCapturada.visibility = View.VISIBLE

                // Cambiar borde a verde
                viewBordeEstado.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                Toast.makeText(this, "Error: elementos de vista no encontrados", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Error: vista de foto no encontrada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertirBitmapABase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    private fun actualizarContadorFotos() {
        val fotosCapturadas = listaFotos.count { it != null }
        binding.tvContadorFotosInspeccionar.text = "Fotos capturadas: $fotosCapturadas/5"
    }

    private fun regresarConFotos() {
        val intent = Intent()
        val jsonFotos = gson.toJson(listaFotos)
        intent.putExtra("fotos", jsonFotos)
        intent.putExtra("fotosCapturadas", listaFotos.count { it != null })
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}