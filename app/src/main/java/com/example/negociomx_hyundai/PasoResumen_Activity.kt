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
import com.example.negociomx_hyundai.BE.MovimientoCompleto
import com.example.negociomx_hyundai.BE.MovimientoTracking
import com.example.negociomx_hyundai.BE.ResumenCompletoConQuery
import com.example.negociomx_hyundai.BE.Vehiculo
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
import com.example.negociomx_hyundai.Utils.Utils
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

    val util=Utils()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPasoResumenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainResumen) { v, insets ->
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

        // Botón consultar
        binding.btnConsultarResumen.setOnClickListener {
            val vin = binding.etVinResumen.text.toString().trim()
            if (vin.isEmpty()) {
                Toast.makeText(this, "Ingrese un VIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            consultarVehiculo(vin)
        }
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
                val resumen = dalPasoLog.consultarResumenCompletoConQuery(vin)

                if (resumen == null || resumen.Movimientos.isEmpty()) {
                    ocultarCarga()
                    mostrarError("No hay registros para este vehículo")
                    return@launch
                }
                mostrarDatosVehiculoNuevo(resumen)
                mostrarStatusResumenNuevo(resumen)
                mostrarTrackingNuevo(resumen.Movimientos)
                ocultarCarga()

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error consultando vehículo: ${e.message}")
                Log.e("PasoResumen", "Error: ${e.message}")
            }
        }
    }

    private fun mostrarDatosVehiculoNuevo(resumen: ResumenCompletoConQuery) {
        binding.apply {
            //  Datos del vehículo desde
            tvBlResumen.text = "BL: ${resumen.BL}"
            tvMarcaModeloResumen.text = "${resumen.Marca} ${resumen.Modelo}"
            tvColorExteriorResumen.text = "Color Ext.: ${resumen.ColorExterior}"
            tvColorInteriorResumen.text = "Color Int.: ${resumen.ColorInterior}"

            // Calculo de días de estadía desde el primer movimiento
            val diasEstadia = calcularDiasEstadia(resumen.Movimientos)
            tvDiasEstadiaResumen.text = formatearDiasEstadia(diasEstadia)

            layoutInfoVehiculoResumen.visibility = View.VISIBLE
        }
    }

    private fun mostrarStatusResumenNuevo(resumen: ResumenCompletoConQuery) {
        binding.apply {
            // status actual
            val ultimoMovimiento = resumen.Movimientos.lastOrNull()

            if (ultimoMovimiento != null) {
                // Status actual con posición
                val statusTexto = if (ultimoMovimiento.NombreBloque.isNotEmpty()) {
                    "${ultimoMovimiento.NombreStatusMovimiento}, Col.: ${ultimoMovimiento.Columna ?: "—"}, Fila: ${ultimoMovimiento.Fila ?: "—"}"
                } else {
                    ultimoMovimiento.NombreStatusMovimiento
                }
                tvStatusActualResumen.text = statusTexto

                // Fecha del último movimiento
                tvFechaMovimientoResumen.text =util.formatearFecha(ultimoMovimiento.FechaMovimiento)
            }

            val primerMovimiento = resumen.Movimientos.firstOrNull()
            tvFechaEntradaResumen.text = if (primerMovimiento != null) {
                util.formatearFecha(primerMovimiento.FechaMovimiento)
            } else "—"


            val movimientoSalida = resumen.Movimientos.find {
                it.NombreStatusMovimiento.contains("Salida", ignoreCase = true)
            }
            tvFechaSalidaResumen.text = if (movimientoSalida != null) {
                util.formatearFecha(movimientoSalida.FechaMovimiento)
            } else "—"


            // Total de movimientos
            val totalMovimientos=resumen.Movimientos.filter { it.IdStatusMovimiento!=168
                    && it.IdStatusMovimiento!=169}.count()
            tvTotalMovimientosResumen.text = totalMovimientos.toString()
            layoutStatusResumen.visibility = View.VISIBLE
        }
    }
    private fun mostrarTrackingNuevo(movimientos: List<MovimientoCompleto>) {
        if (movimientos.isEmpty()) {
            binding.layoutTrackingResumen.visibility = View.GONE
            return
        }
        // Tracking con nombres
        val movimientosTracking = movimientos.map { mov ->
            MovimientoTracking(
                fecha = mov.FechaMovimiento,
                status = mov.NombreStatusMovimiento,
                detalle = buildString {
                    if(mov.IdStatusMovimiento==168) {
                        append("Ingresado por: ${mov.NombreTransporte}")
                    }
                    else if(mov.IdStatusMovimiento==169) {
                        append("Retirado por: ${mov.NombreTransporte}")
                    }
                    else if(mov.IdStatusMovimiento==170) {//STATUS -> POSICIONADO
                        if (mov.NombreBloque.isNotEmpty()) {
                            append("${mov.NombreBloque} Col. ${mov.Columna}, Fila ${mov.Fila}")
                        }
                    }
                    else if(mov.IdStatusMovimiento==171) {//// EN TALLER
                        if (mov.NombreParteDanno.isNotEmpty()) {
                            if (isNotEmpty()) append(" - ")
                            append("Parte: ${mov.NombreParteDanno}")
                        }
                    }
                    else if(mov.IdStatusMovimiento==172) {///MOVIMIENTO LOCAL
                        if (mov.NombreTipoMovimiento.isNotEmpty()) {
                            append("${mov.NombreTipoMovimiento}")
                        }
                    }
                },
            )
        }

        trackingAdapter.actualizarMovimientos(movimientosTracking)
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

    private fun calcularDiasEstadia(movimientos: List<MovimientoCompleto>): Int {
        if (movimientos.isEmpty()) return 0

        return try {
            val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val primerMovimiento = movimientos.firstOrNull() ?: return 0
            val fechaEntrada = formatoFecha.parse(primerMovimiento.FechaMovimiento) ?: return 0

            val movimientoSalida = movimientos.find {
                it.NombreStatusMovimiento.contains("Salida", ignoreCase = true)
            }

            // resta
            val fechaSalidaMillis = if (movimientoSalida != null) {
                val fechaSalidaDate = formatoFecha.parse(movimientoSalida.FechaMovimiento)
                fechaSalidaDate?.time ?: System.currentTimeMillis()
            } else {
                System.currentTimeMillis()
            }

            val diferencia = fechaSalidaMillis - fechaEntrada.time

            TimeUnit.MILLISECONDS.toDays(diferencia).toInt()
        } catch (e: Exception) {
            Log.e("PasoResumen", "Error calculando días: ${e.message}")
            0
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