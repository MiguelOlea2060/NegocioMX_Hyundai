package com.example.negociomx_hyundai

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.negociomx_hyundai.BE.ActividadDiariaItem
import com.example.negociomx_hyundai.DAL.DALConsultaActividades
import com.example.negociomx_hyundai.adapters.ActividadDiariaAdapter
import com.example.negociomx_hyundai.databinding.ActivityConsultaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class Consulta_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityConsultaBinding
    private val dalConsulta = DALConsultaActividades()
    private lateinit var adapter: ActividadDiariaAdapter
    private var actividades = listOf<ActividadDiariaItem>()

    // Variables para overlay de carga
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView

    // Fecha seleccionada
    private var fechaSeleccionada = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConsultaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarComponentes()
        configurarEventos()
        inicializarFechaActual()
    }

    private fun inicializarComponentes() {
        loadingContainer = findViewById(R.id.loadingContainer)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvLoadingSubtext = findViewById(R.id.tvLoadingSubtext)

        // Configurar RecyclerView
        adapter = ActividadDiariaAdapter(actividades) { actividad ->
            mostrarDetalleActividad(actividad)
        }
        binding.recyclerViewActividades.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewActividades.adapter = adapter
    }

    private fun configurarEventos() {
        binding.btnSeleccionarFecha.setOnClickListener {
            mostrarSelectorFecha()
        }

        binding.btnConsultar.setOnClickListener {
            consultarActividades()
        }
    }

    private fun inicializarFechaActual() {
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fechaSeleccionada = formatoFecha.format(Date())
        binding.tvFechaSeleccionada.text = fechaSeleccionada
    }

    private fun mostrarSelectorFecha() {
        val calendar = Calendar.getInstance()

        // Si hay fecha seleccionada, usarla como inicial
        if (fechaSeleccionada.isNotEmpty()) {
            try {
                val formatoFecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fecha = formatoFecha.parse(fechaSeleccionada)
                calendar.time = fecha!!
            } catch (e: Exception) {
                Log.e("Consulta_Activity", "Error parseando fecha: ${e.message}")
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val fechaFormateada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                fechaSeleccionada = fechaFormateada
                binding.tvFechaSeleccionada.text = fechaSeleccionada
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun consultarActividades() {
        if (fechaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Seleccione una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCarga()

        lifecycleScope.launch {
            try {
                Log.d("Consulta_Activity", "🔍 Consultando actividades para fecha: $fechaSeleccionada")

                actividades = dalConsulta.consultarActividadesPorFecha(fechaSeleccionada)

                ocultarCarga()

                if (actividades.isNotEmpty()) {
                    mostrarResultados()
                    actualizarEstadisticas()
                } else {
                    mostrarSinDatos()
                }

            } catch (e: Exception) {
                ocultarCarga()
                Log.e("Consulta_Activity", "💥 Error consultando actividades: ${e.message}")
                Toast.makeText(this@Consulta_Activity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarResultados() {
        binding.layoutEstadisticas.visibility = View.VISIBLE
        binding.layoutListaActividades.visibility = View.VISIBLE
        binding.layoutSinDatos.visibility = View.GONE

        adapter.actualizarActividades(actividades)
        Toast.makeText(this, "✅ Se encontraron ${actividades.size} actividades", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarSinDatos() {
        binding.layoutEstadisticas.visibility = View.GONE
        binding.layoutListaActividades.visibility = View.GONE
        binding.layoutSinDatos.visibility = View.VISIBLE
    }

    private fun actualizarEstadisticas() {
        val total = actividades.size
        val entradas = actividades.count { it.IdStatus == 168 }
        val salidas = actividades.count { it.IdStatus == 169 }
        val posicionados = actividades.count { it.IdStatus == 170 }

        binding.tvTotalActividades.text = "Total: $total"
        binding.tvEntradas.text = "Entradas: $entradas"
        binding.tvSalidas.text = "Salidas: $salidas"
        binding.tvPosicionados.text = "Posicionados: $posicionados"
    }

    private fun mostrarDetalleActividad(actividad: ActividadDiariaItem) {
        val mensaje = buildString {
            append("VIN: ${actividad.VIN}\n")
            append("Vehículo: ${actividad.Marca} ${actividad.Modelo} ${actividad.Anio}\n")
            append("Status: ${actividad.NombreStatus}\n")
            append("Fecha: ${actividad.FechaMovimiento}\n")
            append("Usuario: ${actividad.UsuarioMovimiento}\n")

            if (!actividad.Bloque.isNullOrEmpty()) {
                append("Ubicación: ${actividad.Bloque} ${actividad.Fila}-${actividad.Columna}\n")
            }

            if (!actividad.PersonaQueHaraMovimiento.isNullOrEmpty()) {
                append("Personal: ${actividad.PersonaQueHaraMovimiento}\n")
            }

            if (!actividad.PlacaTransporte.isNullOrEmpty()) {
                append("Transporte: ${actividad.PlacaTransporte}\n")
            }

            if (!actividad.Observacion.isNullOrEmpty()) {
                append("Observación: ${actividad.Observacion}")
            }
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Detalle de Actividad")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    // MÉTODOS PARA OVERLAY DE CARGA
    private fun mostrarCarga() {
        loadingContainer.visibility = View.VISIBLE
        binding.btnConsultar.isEnabled = false
        binding.btnConsultar.alpha = 0.5f

        binding.layoutEstadisticas.visibility = View.GONE
        binding.layoutListaActividades.visibility = View.GONE
        binding.layoutSinDatos.visibility = View.GONE

        tvLoadingText.text = "Consultando actividades..."
        tvLoadingSubtext.text = "Obteniendo datos de la base de datos"
    }

    private fun ocultarCarga() {
        loadingContainer.visibility = View.GONE
        binding.btnConsultar.isEnabled = true
        binding.btnConsultar.alpha = 1.0f
    }
}