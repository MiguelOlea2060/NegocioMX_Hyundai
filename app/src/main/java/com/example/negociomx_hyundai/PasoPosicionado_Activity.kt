package com.example.negociomx_hyundai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.ClienteEmpleado
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.DAL.DALCliente
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityPasoPosicionadoBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PasoPosicionado_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPasoPosicionadoBinding
    private val dalPasoLog = DALPasoLogVehiculo()
    private val dalCliente = DALCliente()
    private var vehiculoActual: VehiculoPasoLog? = null
    private var statusActual: PasoLogVehiculoDet? = null
    private var empleadosPersonal = listOf<ClienteEmpleado>()

    // Variables para overlay de carga
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView

    // Variables para hora dinámica
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable


    private var bloques = listOf<String>()
    private var posiciones = listOf<String>()
    private var tiposMovimiento = listOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPasoPosicionadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarComponentes()
        configurarEventos()
        inicializarFormulario()
    }

    private fun inicializarComponentes() {
        loadingContainer = findViewById(R.id.loadingContainer)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvLoadingSubtext = findViewById(R.id.tvLoadingSubtext)
    }

    private fun configurarEventos() {
        binding.etVIN.requestFocus()

        // Captura de Enter en el campo VIN
        binding.etVIN.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                consultarVehiculo()
                return@setOnKeyListener true
            }
            false
        }

        // Botón consultar vehículo
        binding.btnConsultarVehiculo.setOnClickListener {
            consultarVehiculo()
        }

        // Botón guardar posicionado
        binding.btnGuardarPosicionado.setOnClickListener {
            guardarPosicionado()
        }
    }

    private fun inicializarFormulario() {
        // Inicializar empleado receptor
        binding.tvEmpleadoReceptor.text = "Empleado receptor: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"

        // Inicializar hora dinámica
        inicializarHoraDinamica()

        // Cargar personal
        cargarPersonal()

        // <CHANGE> Cargar datos para spinners
        cargarBloques()
        cargarTiposMovimiento()

    }





    private fun consultarVehiculo() {
        val vin = binding.etVIN.text.toString().trim()
        if (vin.isEmpty() || vin.length < 17) {
            Toast.makeText(this, "Ingrese un VIN válido (17 caracteres)", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCargaConsulta()

        lifecycleScope.launch {
            try {
                Log.d("PasoPosicionado", "🔍 Consultando vehículo con VIN: $vin")

                vehiculoActual = dalPasoLog.consultaVehiculoPorVINParaPasoLogVehiculo(vin)

                ocultarCargaConsulta()

                if (vehiculoActual != null) {
                    // Consultar status actual
                    statusActual = dalPasoLog.consultarStatusActual(vehiculoActual!!.Id!!.toInt())

                    if (validarStatusParaPosicionado()) {
                        mostrarInformacionVehiculo(vehiculoActual!!)
                        mostrarFormularioPosicionado()
                    } else {
                        mostrarError("El vehículo no tiene un status válido para posicionado. Status actual: ${obtenerNombreStatus(statusActual?.IdStatus ?: 0)}")
                    }
                } else {
                    mostrarError("El VIN no existe en la base de datos.")
                }

            } catch (e: Exception) {
                ocultarCargaConsulta()
                Log.e("PasoPosicionado", "💥 Error consultando vehículo: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validarStatusParaPosicionado(): Boolean {
        val statusValidos = listOf(168, 171, 172) // ENTRADA, EN TALLER, MOVIMIENTO LOCAL
        return statusActual?.IdStatus in statusValidos
    }

    private fun mostrarInformacionVehiculo(vehiculo: VehiculoPasoLog) {
        binding.apply {
            tvBlVehiculo.text = "MBL: ${vehiculo.BL}"
            tvMarcaModeloAnnio.text = "${vehiculo.Marca} - ${vehiculo.Modelo}, ${vehiculo.Anio}"
            tvColorExterior.text = "Color Ext.: ${vehiculo.ColorExterior}"
            tvStatusActual.text = "Status actual: ${obtenerNombreStatus(statusActual?.IdStatus ?: 0)}"

            layoutInfoVehiculo.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularioPosicionado() {
        binding.layoutFormularioPosicionado.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        Toast.makeText(this, "✅ Vehículo válido para posicionado", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarError(mensaje: String) {
        binding.layoutError.visibility = View.VISIBLE
        binding.layoutFormularioPosicionado.visibility = View.GONE
        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.tvMensajeError.text = mensaje
    }

    private fun obtenerNombreStatus(idStatus: Int): String {
        return when (idStatus) {
            168 -> "ENTRADA"
            169 -> "SALIDA"
            170 -> "POSICIONADO"
            171 -> "EN TALLER"
            172 -> "MOVIMIENTO LOCAL"
            else -> "Desconocido"
        }
    }

    private fun guardarPosicionado() {
        if (!validarFormularioPosicionado()) {
            return
        }

        mostrarCargaGuardado()

        lifecycleScope.launch {
            try {
                Log.d("PasoPosicionado", "💾 Guardando posicionado del vehículo")

                // <CHANGE> Obtener datos de los spinners
                val posicionBloque = binding.spinnerBloque.selectedItemPosition
                val bloque = if (posicionBloque > 0) bloques[posicionBloque - 1] else ""

                val posicionPosicion = binding.spinnerPosicion.selectedItemPosition
                val posicionSeleccionada = if (posicionPosicion > 0) posiciones[posicionPosicion - 1] else ""
                val partesPos = posicionSeleccionada.split("-")
                val fila = partesPos.getOrNull(0)?.toIntOrNull() ?: 0
                val columna = partesPos.getOrNull(1)?.toIntOrNull() ?: 0

                val posicionTipoMov = binding.spinnerTipoMovimiento.selectedItemPosition
                val idTipoMovimiento = if (posicionTipoMov > 0) tiposMovimiento[posicionTipoMov - 1].first else 0

                val posicionPersonal = binding.spinnerPersonal.selectedItemPosition
                val idEmpleadoPersonal = if (posicionPersonal > 0) empleadosPersonal[posicionPersonal - 1].IdClienteEmpleado else null
                val nombrePersonalMovimiento = if (posicionPersonal > 0) empleadosPersonal[posicionPersonal - 1].NombreCompleto ?: "" else ""

                val exito = dalPasoLog.crearRegistroPosicionado(
                    idVehiculo = vehiculoActual!!.Id!!.toInt(),
                    idUsuario = ParametrosSistema.usuarioLogueado.Id!!.toInt(),
                    bloque = bloque,
                    fila = fila,
                    columna = columna,
                    idTipoMovimiento = idTipoMovimiento,
                    nombrePersonalMovimiento = nombrePersonalMovimiento,
                    idEmpleadoPersonal = idEmpleadoPersonal
                )

                ocultarCargaGuardado()

                if (exito) {
                    Toast.makeText(this@PasoPosicionado_Activity, "✅ Vehículo posicionado exitosamente", Toast.LENGTH_SHORT).show()
                    limpiarFormulario()
                } else {
                    Toast.makeText(this@PasoPosicionado_Activity, "❌ Error guardando posicionado", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                ocultarCargaGuardado()
                Log.e("PasoPosicionado", "💥 Error guardando posicionado: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validarFormularioPosicionado(): Boolean {
        if (binding.spinnerBloque.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el bloque", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.spinnerPosicion.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione la posición", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.spinnerTipoMovimiento.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el tipo de movimiento", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.spinnerPersonal.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el personal que lo manejará", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


    // MÉTODOS PARA HORA DINÁMICA
    private fun inicializarHoraDinamica() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                binding.tvFechaMovimiento.text = "Fecha de movimiento: $fechaActual"
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

    // MÉTODOS PARA CARGAR PERSONAL
    private fun cargarPersonal() {
        lifecycleScope.launch {
            try {
                val transportistas = dalCliente.consultarTransportistas(true)
                empleadosPersonal = mutableListOf()

                transportistas.forEach { transportista ->
                    transportista.Empleados?.let { empleados ->
                        (empleadosPersonal as MutableList).addAll(empleados)
                    }
                }

                val nombresPersonal = mutableListOf("Seleccionar personal...")
                nombresPersonal.addAll(empleadosPersonal.map { it.NombreCompleto ?: "Sin nombre" })

                val adapter = ArrayAdapter(
                    this@PasoPosicionado_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresPersonal
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerPersonal.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando personal: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando personal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // MÉTODOS PARA OVERLAY DE CARGA
    private fun mostrarCargaConsulta() {
        loadingContainer.visibility = View.VISIBLE
        binding.btnConsultarVehiculo.isEnabled = false
        binding.btnConsultarVehiculo.alpha = 0.5f

        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioPosicionado.visibility = View.GONE
        binding.layoutError.visibility = View.GONE

        tvLoadingText.text = "Consultando vehículo..."
        tvLoadingSubtext.text = "Verificando status para posicionado"
    }

    private fun ocultarCargaConsulta() {
        loadingContainer.visibility = View.GONE
        binding.btnConsultarVehiculo.isEnabled = true
        binding.btnConsultarVehiculo.alpha = 1.0f
    }

    private fun mostrarCargaGuardado() {
        loadingContainer.visibility = View.VISIBLE
        binding.btnGuardarPosicionado.isEnabled = false
        binding.btnGuardarPosicionado.alpha = 0.5f

        tvLoadingText.text = "Guardando posicionado..."
        tvLoadingSubtext.text = "Actualizando status del vehículo"
    }

    private fun ocultarCargaGuardado() {
        loadingContainer.visibility = View.GONE
        binding.btnGuardarPosicionado.isEnabled = true
        binding.btnGuardarPosicionado.alpha = 1.0f
    }



    // MÉTODOS PARA CARGAR DATOS DE SPINNERS
    private fun cargarBloques() {
        lifecycleScope.launch {
            try {
                bloques = dalPasoLog.consultarBloques()

                val nombresBloques = mutableListOf("Seleccionar bloque...")
                nombresBloques.addAll(bloques)

                val adapter = ArrayAdapter(
                    this@PasoPosicionado_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresBloques
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerBloque.adapter = adapter

                // <CHANGE> Configurar listener para cargar posiciones cuando cambie el bloque
                binding.spinnerBloque.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0) {
                            cargarPosiciones(bloques[position - 1])
                        } else {
                            binding.spinnerPosicion.adapter = null
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando bloques: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando bloques", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarPosiciones(bloque: String) {
        lifecycleScope.launch {
            try {
                posiciones = dalPasoLog.consultarPosicionesPorBloque(bloque)

                val nombresPosiciones = mutableListOf("Seleccionar posición...")
                nombresPosiciones.addAll(posiciones)

                val adapter = ArrayAdapter(
                    this@PasoPosicionado_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresPosiciones
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerPosicion.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando posiciones: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando posiciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarTiposMovimiento() {
        lifecycleScope.launch {
            try {
                tiposMovimiento = dalPasoLog.consultarTiposMovimiento()

                val nombresTipos = mutableListOf("Seleccionar tipo de movimiento...")
                nombresTipos.addAll(tiposMovimiento.map { it.second })

                val adapter = ArrayAdapter(
                    this@PasoPosicionado_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresTipos
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerTipoMovimiento.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando tipos de movimiento: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando tipos de movimiento", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun limpiarFormulario() {
        binding.etVIN.setText("")
        binding.spinnerBloque.setSelection(0)
        binding.spinnerPosicion.setSelection(0)
        binding.spinnerTipoMovimiento.setSelection(0)
        binding.spinnerPersonal.setSelection(0)

        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioPosicionado.visibility = View.GONE
        binding.layoutError.visibility = View.GONE

        vehiculoActual = null
        statusActual = null

        binding.etVIN.requestFocus()
    }
    override fun onDestroy() {
        super.onDestroy()
        detenerHoraDinamica()
    }
}








