package com.example.negociomx_hyundai

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.negociomx_hyundai.BE.MovimientoTracking
import com.example.negociomx_hyundai.BE.Vehiculo
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
import com.example.negociomx_hyundai.adapters.TrackingAdapter
import com.example.negociomx_hyundai.databinding.ActivityPasoResumenBinding
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class PasoResumen_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPasoResumenBinding
    private var vehiculoActual: Vehiculo? = null
    private lateinit var trackingAdapter: TrackingAdapter
    private val dalVehiculo = DALVehiculo()
    private val dalPasoLog = DALPasoLogVehiculo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPasoResumenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarUI()
        configurarRecyclerView()

        // Verificar si viene VIN por Intent
        val vinIntent = intent.getStringExtra("VIN")
        if (!vinIntent.isNullOrEmpty()) {
            binding.etVinResumen.setText(vinIntent)
            consultarVehiculo(vinIntent)
        }
    }

    private fun configurarUI() {
        // Botón escanear QR
        binding.btnEscanearQRResumen.setOnClickListener {
            iniciarEscaneoQR()
        }

        // Botón consultar
        binding.btnConsultarResumen.setOnClickListener {
            val vin = binding.etVinResumen.text.toString().trim()
            if (vin.isEmpty()) {
                Toast.makeText(this, "Ingrese un VIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            consultarVehiculo(vin)
        }

        // Botón regresar
        binding.btnRegresarResumen.setOnClickListener {
            finish()
        }
    }

    private fun configurarRecyclerView() {
        trackingAdapter = TrackingAdapter(emptyList())
        binding.rvTrackingMovimientos.apply {
            layoutManager = LinearLayoutManager(this@PasoResumen_Activity)
            adapter = trackingAdapter
        }
    }

    private fun iniciarEscaneoQR() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Escanea el código QR del vehículo")
        integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(false)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result.contents != null) {
            binding.etVinResumen.setText(result.contents)
            consultarVehiculo(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun consultarVehiculo(vin: String) {
        mostrarCarga()

        lifecycleScope.launch {
            try {
                // Consultar vehículo
                val vehiculo = dalVehiculo.consultarVehiculoPorVIN(vin)

                if (vehiculo == null || vehiculo.Id.toInt() <= 0) {
                    ocultarCarga()
                    mostrarError("Vehículo no registrado")
                    return@launch
                }

                vehiculoActual = vehiculo

                // Consultar resumen completo
                val resumen = dalPasoLog.consultarResumenCompleto(vin)

                if (resumen.isEmpty()) {
                    ocultarCarga()
                    mostrarError("No hay registros para este vehículo")
                    return@launch
                }

                // Consultar tracking
                val tracking = dalPasoLog.consultarTrackingMovimientos(vin)

                // Mostrar datos
                mostrarDatosVehiculo(vehiculo, resumen)
                mostrarStatusResumen(resumen)
                mostrarTracking(tracking)

                ocultarCarga()

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error consultando vehículo: ${e.message}")
                Log.e("PasoResumen", "Error: ${e.message}")
            }
        }
    }

    private fun mostrarDatosVehiculo(vehiculo: Vehiculo, resumen: Map<String, Any?>) {
        binding.apply {
            tvVinResumen.text = "VIN: ${vehiculo.VIN}"
            tvBlResumen.text = "BL: ${vehiculo.BL}"
            tvMarcaModeloResumen.text = "${vehiculo.Marca} ${vehiculo.Modelo} - ${vehiculo.Anio}"
            tvColorExteriorResumen.text = "Color Ext: ${vehiculo.ColorExterior}"
            tvColorInteriorResumen.text = "Color Int: ${vehiculo.ColorInterior}"


            // Calcular y mostrar días de estadía
            val diasEstadia = resumen["DiasEstadia"] as? Int ?: 0
            tvDiasEstadiaResumen.text = "Estadía: ${formatearDiasEstadia(diasEstadia)}"

            layoutInfoVehiculoResumen.visibility = View.VISIBLE
        }
    }

    private fun mostrarStatusResumen(resumen: Map<String, Any?>) {
        binding.apply {
            tvStatusActualResumen.text = resumen["StatusActual"] as? String ?: "—"

            // Fecha entrada
            val fechaEntrada = resumen["FechaEntrada"] as? String
            tvFechaEntradaResumen.text = if (fechaEntrada != null) {
                formatearFecha(fechaEntrada)
            } else "—"

            // Fecha salida
            val fechaSalida = resumen["FechaSalida"] as? String
            tvFechaSalidaResumen.text = if (fechaSalida != null) {
                formatearFecha(fechaSalida)
            } else "—"

            // Total movimientos
            val totalMovimientos = resumen["TotalMovimientos"] as? Int ?: 0
            tvTotalMovimientosResumen.text = totalMovimientos.toString()

            layoutStatusResumen.visibility = View.VISIBLE
        }
    }

    private fun mostrarTracking(movimientos: List<MovimientoTracking>) {
        if (movimientos.isEmpty()) {
            binding.layoutTrackingResumen.visibility = View.GONE
            return
        }

        trackingAdapter.actualizarMovimientos(movimientos)
        binding.layoutTrackingResumen.visibility = View.VISIBLE
    }

    private fun formatearDiasEstadia(dias: Int): String {
        val anos = dias / 365
        val meses = (dias % 365) / 30
        val semanas = ((dias % 365) % 30) / 7
        val diasRestantes = ((dias % 365) % 30) % 7

        return buildString {
            if (anos > 0) append("$anos año${if (anos > 1) "s" else ""}")
            if (meses > 0) {
                if (isNotEmpty()) append(", ")
                append("$meses mes${if (meses > 1) "es" else ""}")
            }
            if (semanas > 0) {
                if (isNotEmpty()) append(", ")
                append("$semanas semana${if (semanas > 1) "s" else ""}")
            }
            if (diasRestantes > 0) {
                if (isNotEmpty()) append(", ")
                append("$diasRestantes día${if (diasRestantes > 1) "s" else ""}")
            }
            if (isEmpty()) append("0 días")
        }
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault())
            val date = formatoEntrada.parse(fecha)
            formatoSalida.format(date!!)
        } catch (e: Exception) {
            fecha
        }
    }

    private fun mostrarCarga() {
        binding.apply {
            progressOverlayResumen.visibility = View.VISIBLE
            tvLoadingTextResumen.text = "Consultando vehículo..."
        }
    }

    private fun ocultarCarga() {
        binding.progressOverlayResumen.visibility = View.GONE
    }

    private fun mostrarError(mensaje: String) {
        binding.apply {
            tvMensajeErrorResumen.text = mensaje
            layoutErrorResumen.visibility = View.VISIBLE
            layoutInfoVehiculoResumen.visibility = View.GONE
            layoutStatusResumen.visibility = View.GONE
            layoutTrackingResumen.visibility = View.GONE
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }
}