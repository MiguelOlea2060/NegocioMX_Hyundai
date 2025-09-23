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
import com.example.negociomx_hyundai.databinding.ActivityPasoMovimientoLocalBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PasoMovimientoLocal_Activity : AppCompatActivity() {

    lateinit var binding:ActivityPasoMovimientoLocalBinding
    // Variables de UI
    private lateinit var layoutInfoVehiculo: LinearLayout
    private lateinit var layoutFormularioMovimiento: LinearLayout
    private lateinit var layoutBotones: LinearLayout
    private lateinit var layoutError: LinearLayout
    private lateinit var loadingContainer: LinearLayout

    private lateinit var tvBlVehiculo: TextView
    private lateinit var tvMarcaModeloAnnio: TextView
    private lateinit var tvColorExterior: TextView
    private lateinit var tvColorInterior: TextView
    private lateinit var spinnerPersonalMovimiento: Spinner
    private lateinit var spinnerTipoMovimiento: Spinner
    private lateinit var etObservacion: EditText
    private lateinit var tvFechaMovimiento: TextView
    private lateinit var tvEmpleadoRegistra: TextView
    private lateinit var btnLimpiar: Button
    private lateinit var btnGuardarMovimiento: Button
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView
    private lateinit var tvError: TextView

    // Variables de datos
    private var vehiculoActual: VehiculoPasoLog? = null
    private var empleados = listOf<Empleado>()
    private var tiposMovimiento = listOf<TipoMovimiento>()
    private var usuarioActual = "Sistema" // En implementación real, obtener del usuario logueado

    // DALs
    private val dalVehiculo = DALVehiculo()
    private val dalEmpleado = DALEmpleadoSQL()
    private val dalPasoLogVehiculo = DALPasoLogVehiculo()
    var fechaActual:String=""
    // Handler para actualizar fecha en tiempo real
    private lateinit var handlerFecha: Handler
    private lateinit var runnableFecha: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPasoMovimientoLocalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarComponentes()
        configurarEventos()
        cargarDatosIniciales()
        iniciarActualizacionFecha()
    }

    private fun inicializarComponentes() {
        // Layouts
        layoutInfoVehiculo = findViewById(R.id.layoutInfoVehiculo)
        layoutFormularioMovimiento = findViewById(R.id.layoutFormularioMovimiento)

        layoutBotones = findViewById(R.id.layoutBotones)
        layoutError = findViewById(R.id.layoutError)
        loadingContainer = findViewById(R.id.loadingContainer)

        // Información del vehículo
        tvBlVehiculo = findViewById(R.id.tvBlVehiculo)
        tvMarcaModeloAnnio = findViewById(R.id.tvMarcaModeloAnnio)
        tvColorExterior = findViewById(R.id.tvColorExterior)
        tvColorInterior = findViewById(R.id.tvColorInterior)

        // Formulario
        spinnerPersonalMovimiento = findViewById(R.id.spinnerPersonalMovimiento)
        spinnerTipoMovimiento = findViewById(R.id.spinnerTipoMovimiento)
        etObservacion = findViewById(R.id.etObservacion)
        tvFechaMovimiento = findViewById(R.id.tvFechaMovimiento)
        tvEmpleadoRegistra = findViewById(R.id.tvEmpleadoRegistra)

        // Botones de acción
        btnLimpiar = findViewById(R.id.btnLimpiar)
        btnGuardarMovimiento = findViewById(R.id.btnGuardarMovimiento)

        // Loading y error
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvLoadingSubtext = findViewById(R.id.tvLoadingSubtext)
        tvError = findViewById(R.id.tvError)

        // Configurar empleado que registra
        tvEmpleadoRegistra.text = "Empleado que registra: $usuarioActual"
    }

    private fun configurarEventos() {
        binding.btnRegresarPaso1Entrada.setOnClickListener {
            finish()
        }

        btnLimpiar.setOnClickListener {
            limpiarFormulario()
        }

        btnGuardarMovimiento.setOnClickListener {
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
                empleados = dalEmpleado.consultarEmpleados(105)
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
        nombresEmpleados.addAll(empleados.map { "${it.NombreCompleto} (ID: ${it.IdEmpleado})" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresEmpleados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPersonalMovimiento.adapter = adapter
    }

    private fun configurarSpinnerTiposMovimiento() {
        val nombresTipos = mutableListOf("Seleccione tipo de movimiento...")
        nombresTipos.addAll(tiposMovimiento.map { it.Nombre })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresTipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoMovimiento.adapter = adapter
    }


    private fun obtenerDatosVehiculo() {
        try {
            val idVehiculo = intent.getIntExtra("IdVehiculo", 0)
            val vin = intent.getStringExtra("Vin") ?: ""
            val bl = intent.getStringExtra("Bl") ?: ""
            val marca = intent.getStringExtra("Marca") ?: ""
            val modelo = intent.getStringExtra("Modelo") ?: ""
            val annioAux = intent.getStringExtra("Annio") ?: ""
            var annio: Int = 0
            if (annioAux.isNotEmpty())
                annio=annioAux.toInt()
            val colorExterior = intent.getStringExtra("ColorExterior") ?: ""
            val colorInterior = intent.getStringExtra("ColorInterior") ?: ""


            if (idVehiculo > 0 && vin.isNotEmpty()) {
                vehiculoActual = VehiculoPasoLog(
                    Id = idVehiculo.toString(),
                    VIN = vin,
                    BL = bl,
                    Marca = marca,
                    Modelo = modelo,
                    Anio = annio,
                    ColorExterior = colorExterior,
                    ColorInterior = colorInterior
                )

                mostrarInfoVehiculo()
                Log.d("PasoMovimientoLocal_Activity", "✅ Datos del vehículo obtenidos: VIN=$vin")
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
            binding.tvVinVehiculoMovimiento.text="VIN: ${vehiculo.VIN}"
            tvBlVehiculo.text = "BL: ${vehiculo.BL}"
            tvMarcaModeloAnnio.text = "Marca: ${vehiculo.Marca}     Modelo: ${vehiculo.Modelo}    Año: ${vehiculo.Anio}"
            tvColorExterior.text = "Color Ext: ${vehiculo.ColorExterior}"
            tvColorInterior.text = "Color Int: ${vehiculo.ColorInterior}"

            layoutInfoVehiculo.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularios() {
        // <CHANGE> Mostrar información del vehículo cuando se muestran los formularios

        layoutFormularioMovimiento.visibility = View.VISIBLE
        layoutBotones.visibility = View.VISIBLE
    }

    private fun ocultarFormularios() {
        layoutInfoVehiculo.visibility = View.GONE
        layoutFormularioMovimiento.visibility = View.GONE
        layoutBotones.visibility = View.GONE
    }

    private fun iniciarActualizacionFecha() {
        handlerFecha = Handler(Looper.getMainLooper())
        runnableFecha = object : Runnable {
            override fun run() {
                fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val fechaActualAux = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                tvFechaMovimiento.text = fechaActualAux.format(Date())
                handlerFecha.postDelayed(this, 1000) // Actualizar cada segundo
            }
        }
        handlerFecha.post(runnableFecha)
    }

    private fun guardarStatusMovimientoLocal() {
        if (!validarFormulario()) return

        mostrarCarga("Guardando status->Movimiento local...", "Registrando en base de datos")

        lifecycleScope.launch {
            try {
                val idVehiculo = vehiculoActual?.Id?.toIntOrNull() ?: 0
                val idUsuario = 1 // En implementación real, obtener del usuario logueado
                val idPersonalMovimiento = empleados[spinnerPersonalMovimiento.selectedItemPosition - 1].IdEmpleado
                val idTipoMovimiento = tiposMovimiento[spinnerTipoMovimiento.selectedItemPosition - 1].IdTipoMovimiento
                val observacion = etObservacion.text.toString().trim()
                val placa=""

                val resultado = dalPasoLogVehiculo.crearRegistroMovimientoLocal(
                    idVehiculo = idVehiculo,
                    idUsuario = idUsuario,
                    idPersonalMovimiento = idPersonalMovimiento,
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
        if (spinnerPersonalMovimiento.selectedItemPosition <= 0) {
            mostrarError("Seleccione el personal que hace el movimiento")
            return false
        }

        // Validar tipo de movimiento
        if (spinnerTipoMovimiento.selectedItemPosition <= 0) {
            mostrarError("Seleccione el tipo de movimiento")
            return false
        }

        return true
    }

    private fun limpiarFormulario() {

        vehiculoActual = null
        spinnerPersonalMovimiento.setSelection(0)
        spinnerTipoMovimiento.setSelection(0)
        etObservacion.setText("")

     //   ocultarFormularios() // No ocultar formularios, solo limpiar campos
        ocultarError()


    }

    // MÉTODOS DE UI
    private fun mostrarCarga(mensaje: String, submensaje: String = "") {
        tvLoadingText.text = mensaje
        tvLoadingSubtext.text = submensaje
        tvLoadingSubtext.visibility = if (submensaje.isNotEmpty()) View.VISIBLE else View.GONE
        loadingContainer.visibility = View.VISIBLE

        btnGuardarMovimiento.isEnabled = false

        btnGuardarMovimiento.alpha = 0.5f
    }

    private fun ocultarCarga() {
        loadingContainer.visibility = View.GONE


        btnGuardarMovimiento.isEnabled = true

        btnGuardarMovimiento.alpha = 1.0f
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        layoutError.visibility = View.VISIBLE
    }

    private fun ocultarError() {
        layoutError.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener actualización de fecha
        if (::handlerFecha.isInitialized) {
            handlerFecha.removeCallbacks(runnableFecha)
        }
    }
}
