package com.example.negociomx_hyundai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.*
import com.example.negociomx_hyundai.DAL.DALEmpleadoSQL
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
import com.example.negociomx_hyundai.BE.TipoMovimiento
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityPasoMovimientoLocalBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PasoMovimientoLocal_Activity : AppCompatActivity() {
    lateinit var binding:ActivityPasoMovimientoLocalBinding
    private var vehiculoActual: VehiculoPasoLog? = null
    private var empleados = listOf<Empleado>()
    private var tiposMovimiento = listOf<TipoMovimiento>()
    private val dalVehiculo = DALVehiculo()
    private val dalEmpleado = DALEmpleadoSQL()
    private val dalPasoLogVehiculo = DALPasoLogVehiculo()
    var fechaActual:String=""
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    val gson= Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPasoMovimientoLocalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainMovLoc)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        inicializarComponentes()
        configurarEventos()
        cargarDatosIniciales()

    }

    private fun inicializarComponentes() {
        binding.tvEmpleadoRegistraMovLoc.text = "Empleado receptor: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"
        inicializarHoraDinamica()
    }

    private fun configurarEventos() {
        binding.btnRegresarPaso1EntradaMovLoc.setOnClickListener {
            finish()
        }

        binding.btnLimpiarMovLoc.setOnClickListener {
            limpiarFormulario()
        }

        binding.btnGuardarMovLoc.setOnClickListener {
            guardarStatusMovimientoLocal()
        }
    }

    private fun cargarDatosIniciales() {
        mostrarCarga("Cargando datos iniciales...", "Consultando empleados y tipos de movimiento")

        lifecycleScope.launch {
            try {
                // Obtener datos del vehículo por Intent
               obtenerDatosVehiculo()

                // Cargar tipos de movimiento
                tiposMovimiento = dalPasoLogVehiculo.consultarTiposMovimientoLocal()
                configurarSpinnerTiposMovimiento()

                // Cargar empleados
                empleados = dalEmpleado.consultarEmpleados(105,null)
                configurarSpinnerPersonal()


                ocultarCarga()
                mostrarFormularios()
                Log.d("PasoMovimientoLocal_Activity", "✅ Datos iniciales cargados correctamente")

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error cargando datos iniciales: ${e.message}")
                Log.e("PasoMovimientoLocal_Activity", "Error cargando datos: ${e.message}")
            }
        }
    }

    private fun configurarSpinnerPersonal() {
        val nombresEmpleados = mutableListOf("Seleccione personal...")
        nombresEmpleados.addAll(empleados.map { " ${it.NombreCompleto} " })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresEmpleados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPersonalMovLoc.adapter = adapter
    }

    private fun configurarSpinnerTiposMovimiento() {
        val nombresTipos = mutableListOf("Seleccione tipo de movimiento...")
        nombresTipos.addAll(tiposMovimiento.map { it.Nombre })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTipoMovimientoMovLoc.adapter = adapter
    }


    private fun obtenerDatosVehiculo() {
        try {
            val jsonVeh = intent.getStringExtra("vehiculo") ?: ""

            if (jsonVeh.isNotEmpty()) {
                vehiculoActual = gson.fromJson(jsonVeh,VehiculoPasoLog::class.java)
                mostrarInfoVehiculo()
                Log.d("PasoMovimientoLocal_Activity", "✅ Datos del vehículo obtenidos: VIN=${vehiculoActual?.VIN}")
            } else {
                mostrarError("No se recibieron datos válidos del vehículo")
            }
        } catch (e: Exception) {
            mostrarError("Error obteniendo datos del vehículo: ${e.message}")
            Log.e("PasoMovimientoLocal_Activity", "Error obteniendo datos: ${e.message}")
        }
    }

    private fun mostrarInfoVehiculo() {
        vehiculoActual?.let { vehiculo ->
            binding.tvVinVehiculoMovLoc.text="VIN: ${vehiculo.VIN}"
            binding.tvBlVehiculoMovLoc.text = "BL: ${vehiculo.BL}"
            binding.tvMarcaModeloAnnioMovLoc.text = "${vehiculo.Especificaciones}   Año: ${vehiculo.Anio}"
            binding.tvColorExteriorMovLoc.text = "Color Ext: ${vehiculo.ColorExterior}"
            binding.tvColorInteriorMovLoc.text = "Color Int: ${vehiculo.ColorInterior}"
            binding.layoutInfoVehiculoMovLoc.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularios() {
        binding.apply {
            layoutFormularioMovLoc.visibility = View.VISIBLE
            layoutBotonesMovLoc.visibility = View.VISIBLE
        }

    }

    private fun ocultarFormularios() {
        binding.apply {
            layoutInfoVehiculoMovLoc.visibility = View.GONE
            layoutFormularioMovLoc.visibility = View.GONE
            layoutBotonesMovLoc.visibility = View.GONE
        }

    }

   /* private fun iniciarActualizacionFecha() {
        handlerFecha = Handler(Looper.getMainLooper())
        runnableFecha = object : Runnable {
            override fun run() {
                fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val fechaActualAux = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                binding.tvFechaMovLoc.text = fechaActualAux.format(Date())
                handlerFecha.postDelayed(this, 1000) // Actualizar cada segundo
            }
        }
        handlerFecha.post(runnableFecha)
    }*/

    private fun guardarStatusMovimientoLocal() {
        if (!validarFormulario()) return
        mostrarCarga("Guardando status->Movimiento local...", "Registrando en base de datos")
        lifecycleScope.launch {
            try {
                val idVehiculo = vehiculoActual?.Id?.toIntOrNull() ?: 0
                val idUsuario = ParametrosSistema.usuarioLogueado.Id?.toInt() // En implementación real, obtener del usuario logueado ///checar con martin
           //     val idPersonalMovimiento = empleados[binding.spinnerPersonalMovLoc.selectedItemPosition - 1].IdEmpleado

                val posicionEmpleado=binding.spinnerPersonalMovLoc.selectedItemPosition
                val empleado= empleados[posicionEmpleado-1]
                val idEmpleadoPosiciono=empleado.IdEmpleado
                val nombrePersonalMovimiento = empleado.NombreCompleto

                val idTipoMovimiento = tiposMovimiento[binding.spinnerTipoMovimientoMovLoc.selectedItemPosition - 1].IdTipoMovimiento
                val observacion = binding.etObservacionMovLoc.text.toString().trim()
                val placa=""

                val resultado = dalPasoLogVehiculo.crearRegistroMovimientoLocal(
                    idVehiculo = idVehiculo,
                    idUsuario = idUsuario!!,
                    PersonaHaraMovimiento = nombrePersonalMovimiento,
                    idTipoMovimiento = idTipoMovimiento,
                    observacion = observacion,
                    fechaMovimiento = fechaActual,
                    placa = placa
                )

                ocultarCarga()

                if (resultado) {
                    Toast.makeText(this@PasoMovimientoLocal_Activity, "Movimiento local registrado exitosamente", Toast.LENGTH_LONG).show()

                    val data = Intent()
                    data.putExtra("Refrescar", true);
                    data.putExtra("Vin", vehiculoActual?.VIN);
                    setResult(Activity.RESULT_OK,data)
                    finish()
                } else {
                    mostrarError("Error al guardar el movimiento local")
                }

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error de conexión: ${e.message}")
                Log.e("PasoMovimientoLocal_Activity", "Error guardando: ${e.message}")
            }
        }
    }

    private fun validarFormulario(): Boolean {
        // Validar vehículo seleccionado
        if (vehiculoActual == null) {
            mostrarError("Primero debe consultar un vehículo")
            return false
        }

        // Validar personal
        if (binding.spinnerPersonalMovLoc.selectedItemPosition <= 0) {
            mostrarError("Seleccione el personal que hace el movimiento")
            return false
        }

        // Validar tipo de movimiento
        if (binding.spinnerTipoMovimientoMovLoc.selectedItemPosition <= 0) {
            mostrarError("Seleccione el tipo de movimiento")
            return false
        }

        return true
    }

    private fun limpiarFormulario() {
        vehiculoActual = null
        binding.apply {
            spinnerPersonalMovLoc.setSelection(0)
            spinnerTipoMovimientoMovLoc.setSelection(0)
            etObservacionMovLoc.setText("")
        }
        ocultarError()
    }

    // MÉTODOS DE UI
    private fun mostrarCarga(mensaje: String, submensaje: String = "") {
        binding.apply {
            tvLoadingTextMovLoc.text = mensaje
            tvLoadingSubtextMovLoc.text = submensaje
            tvLoadingSubtextMovLoc.visibility = if (submensaje.isNotEmpty()) View.VISIBLE else View.GONE
            loadingContainerMovLoc.visibility = View.VISIBLE
            btnGuardarMovLoc.isEnabled = false
            btnGuardarMovLoc.alpha = 0.5f
        }

    }

    private fun ocultarCarga() {
        binding.apply {
            loadingContainerMovLoc.visibility = View.GONE
            btnGuardarMovLoc.isEnabled = true
            btnGuardarMovLoc.alpha = 1.0f
        }

    }

    private fun mostrarError(mensaje: String) {
        binding.apply {
            tvErrorMovLoc.text = mensaje
            layoutErrorMovLoc.visibility = View.VISIBLE
        }

    }


    private fun inicializarHoraDinamica() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                var fechaActualAux = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                binding.tvFechaMovimientoLocal.text = "Fecha de movimiento: $fechaActualAux"
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun detenerHoraDinamica() {
        if (::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(timerRunnable)
        }
    }
    private fun ocultarError() {
        binding.layoutErrorMovLoc.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerHoraDinamica()
    }
}
