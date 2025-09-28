package com.example.negociomx_hyundai

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityPaso1EntradaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// <CHANGE> Agregar imports para Handler y Looper
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.negociomx_hyundai.BE.VehiculoPasoLog


import android.widget.ArrayAdapter
import android.widget.AdapterView
import com.example.negociomx_hyundai.BE.Cliente
import com.example.negociomx_hyundai.BE.ClienteEmpleado
import com.example.negociomx_hyundai.DAL.DALCliente
import com.google.gson.Gson

class Paso1Entrada_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPaso1EntradaBinding
    private val dalVehiculo = DALVehiculo()
    private val dalPasoLog = DALPasoLogVehiculo()
    private var vehiculoActual: VehiculoPasoLog? = null
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView
    private var loadingHandler: Handler? = null
    private var loadingRunnable: Runnable? = null

    // Variables para Spinners
    private val dalCliente = DALCliente()
    private var transportistas = listOf<Cliente>()
    private var empleadosTransportista = listOf<ClienteEmpleado>()
    private lateinit var adapterTransportistasRodando: ArrayAdapter<String>
    private lateinit var adapterTransportistasMadrina: ArrayAdapter<String>
    private lateinit var adapterConductores: ArrayAdapter<String>

    // Variables para hora dinámica
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    var fechaActual:String=""

    val gson=Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPaso1EntradaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarEventos()
        // <CHANGE> Inicializar variables del overlay de carga
        loadingContainer = findViewById(R.id.loadingContainer)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvLoadingSubtext = findViewById(R.id.tvLoadingSubtext)
        inicializarFormulario()
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode != Activity.RESULT_OK) return
        when(requestCode) {
            101 -> {
                val refrescar=data?.getBooleanExtra("Refrescar",false)
                val vin= data?.getStringExtra("Vin")

                if(refrescar==true) {
                    binding.etVIN.setText(vin)

                    consultarVehiculo()
                }
            }
            // Other result codes
            else -> {}
        }
    }

    private fun configurarEventos() {
        // <CHANGE> Configurar eventos de la interfaz
        binding.etVIN.requestFocus()

        // Captura de Enter en el campo VIN
        binding.etVIN.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                consultarVehiculo()
                return@setOnKeyListener true
            }
            false
        }

        //Boton regresar para que regrese al menu principal
        binding.btnRegresarPaso1Entrada.setOnClickListener {
            finish()
        }

        // Botón consultar vehículo
        binding.btnConsultarVehiculo.setOnClickListener {
            consultarVehiculo()
        }

        // Radio buttons para tipo de entrada
        binding.rgTipoEntrada.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbRodando -> {
                    binding.layoutRodando.visibility = View.VISIBLE
                    binding.layoutEnMadrina.visibility = View.GONE
                }
                R.id.rbEnMadrina -> {
                    binding.layoutRodando.visibility = View.GONE
                    binding.layoutEnMadrina.visibility = View.VISIBLE
                }
            }
        }

        // Botón guardar entrada
        binding.btnGuardarEntrada.setOnClickListener {
            guardarEntrada()
        }

        // Botones de transición
        binding.btnPosicionado.setOnClickListener {
            val codigoRespuesta=101
            val intent = Intent(this, PasoPosicionado_Activity::class.java)

            val jsonVeh=gson.toJson(vehiculoActual)
            intent.putExtra("vehiculo",jsonVeh)
            startActivityForResult(intent,codigoRespuesta)
        }

        binding.btnMovimientoLocal.setOnClickListener {
            val codigoRespuesta=101
            val jsonVeh=gson.toJson(vehiculoActual)

            val intent = Intent(this, PasoMovimientoLocal_Activity::class.java)
            intent.putExtra("vehiculo", jsonVeh)
            startActivityForResult(intent,codigoRespuesta)
        }

        binding.btnEnTaller.setOnClickListener {
            val codigoRespuesta=101
            val intent = Intent(this, PasoTaller_Activity::class.java)
            intent.putExtra("IdVehiculo", vehiculoActual?.Id?.toInt())
            intent.putExtra("IdPasoLogVehiculo", vehiculoActual?.IdPasoLogVehiculo)
            intent.putExtra("Vin", vehiculoActual?.VIN)
            intent.putExtra("Bl", vehiculoActual?.BL)
            intent.putExtra("Marca", vehiculoActual?.Marca)
            intent.putExtra("Modelo", vehiculoActual?.Modelo)
            intent.putExtra("Annio", vehiculoActual?.Anio.toString())
            intent.putExtra("ColorExterior", vehiculoActual?.ColorExterior)
            intent.putExtra("ColorInterior", vehiculoActual?.ColorInterior)
            intent.putExtra("Especificaciones",vehiculoActual?.Especificaciones)
            startActivityForResult(intent,codigoRespuesta)
        }

        binding.btnSalida.setOnClickListener {
            Toast.makeText(this, "Navegar a Salida", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, PasoSalida_Activity::class.java)

            val codigoRespuesta=101
            val idVehiculo:Int?=vehiculoActual?.Id!!.toInt()
            intent.putExtra("IdVehiculo",idVehiculo)
            intent.putExtra("IdPasoLogVehiculo",vehiculoActual?.IdPasoLogVehiculo)
            intent.putExtra("Vin",vehiculoActual?.VIN)
            intent.putExtra("Bl",vehiculoActual?.BL)
            intent.putExtra("Marca",vehiculoActual?.Marca)
            intent.putExtra("Modelo",vehiculoActual?.Modelo)
            intent.putExtra("Annio", vehiculoActual?.Anio.toString())
            intent.putExtra("ColorExterior",vehiculoActual?.ColorExterior)
            intent.putExtra("ColorInterior",vehiculoActual?.ColorInterior)
            intent.putExtra("Especificaciones",vehiculoActual?.Especificaciones)
            startActivityForResult(intent,codigoRespuesta)
        }

        // <CHANGE> Botón para registrar VIN nuevo
        binding.btnRegistrarVIN.setOnClickListener {
            registrarNuevoVIN()
        }
    }

    private fun inicializarFormulario() {
        // Inicializar empleado receptor
        binding.tvEmpleadoReceptor.text = "Empleado receptor: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"

        // Inicializar hora dinámica
        inicializarHoraDinamica()

        // Cargar transportistas
        cargarTransportistas()

        if(intent.extras!=null)
        {
            val refrescar= intent?.extras!!.getBoolean("RefrescarVin")
            val vin= intent?.extras!!.getString("Vin")
            if(refrescar) {
                binding.etVIN.setText(vin)
                consultarVehiculo()
            }
        }
    }

    private fun consultarVehiculo() {
        val vin = binding.etVIN.text.toString().trim()
        if (vin.isEmpty() || vin.length < 17) {
            Toast.makeText(this, "Ingrese un VIN válido (17 caracteres)", Toast.LENGTH_SHORT).show()
            return
        }

        // <CHANGE> Mostrar overlay de carga antes de consultar
        mostrarCargaConsulta()

        lifecycleScope.launch {
            try {
                Log.d("Paso1Entrada", "🔍 Consultando vehículo con VIN: $vin")

                vehiculoActual = dalPasoLog.consultaVehiculoPorVINParaPasoLogVehiculo(vin)

                // <CHANGE> Ocultar carga antes de mostrar resultados
                ocultarCargaConsulta()

                if (vehiculoActual != null) {
                    mostrarInformacionVehiculo(vehiculoActual!!)

                    // <CHANGE> Consultar status actual para determinar el flujo
                    if (vehiculoActual?.IdPasoLogVehiculo == 0) {
                        // No tiene registros en PasoLogVehiculo
                        mostrarFormularioEntrada()
                    } else {
                        // Ya tiene registros, consultar status actual
                        mostrarOpcionesTransicion()
                    }
                    hideKeyboard()
                } else {
                }

            } catch (e: Exception) {
                // <CHANGE> Ocultar carga en caso de error
                ocultarCargaConsulta()
                Log.e("Paso1Entrada", "💥 Error consultando vehículo: ${e.message}")
                Toast.makeText(this@Paso1Entrada_Activity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }
    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun mostrarInformacionVehiculo(vehiculo: VehiculoPasoLog) {
        // <CHANGE> Mostrar información del vehículo (copiado de Paso1SOC)
        binding.apply {
            tvBlVehiculo.text = "BL: ${vehiculo.BL}"
            tvMarcaModeloAnnio.text = "${vehiculo.Especificaciones}  Año -> ${vehiculo.Anio}"
            tvColorExterior.text = "Color Ext.: ${vehiculo.ColorExterior}"
            tvColorInterior.text = "Color Int.: ${vehiculo.ColorInterior}"

            layoutInfoVehiculo.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularioEntrada() {
        // <CHANGE> Mostrar formulario para vehículos nuevos
        binding.layoutFormularioEntrada.visibility = View.VISIBLE
        binding.layoutOpcionesTransicion.visibility = View.GONE

        Toast.makeText(this, "✅ Vehículo encontrado. Registrar entrada", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarOpcionesTransicion() {
        // <CHANGE> Mostrar opciones según el status actual
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutOpcionesTransicion.visibility = View.VISIBLE

        var detalles:String=""
        if(vehiculoActual?.IdStatusActual==170)
        {
            detalles=" en ${vehiculoActual?.NombreBloque}, Col-> ${vehiculoActual?.Columna} " +
                    "- Fila-> ${vehiculoActual?.Fila}"
        }
        val idStatusActual = vehiculoActual?.IdStatusActual ?: 0
        binding.tvStatusActual.text = "Status actual: ${obtenerNombreStatus(idStatusActual)}${detalles}"

        // Mostrar botones según reglas de transición
        configurarBotonesTransicion(idStatusActual)

        Toast.makeText(this, "✅ Vehículo ya registrado. Seleccione acción", Toast.LENGTH_SHORT).show()
    }

    private fun configurarBotonesTransicion(statusActual: Int) {
        // Ocultar todos primero
        binding.btnPosicionado.visibility = View.GONE
        binding.btnMovimientoLocal.visibility = View.GONE
        binding.btnEnTaller.visibility = View.GONE
        binding.btnSalida.visibility = View.GONE

        when (statusActual) {
            168 -> { // ENTRADA
                binding.btnPosicionado.visibility = View.VISIBLE
                binding.btnMovimientoLocal.visibility = View.VISIBLE
                binding.btnEnTaller.visibility = View.VISIBLE
            }
            170 -> { // POSICIONADO
                binding.btnMovimientoLocal.visibility = View.VISIBLE
                binding.btnEnTaller.visibility = View.VISIBLE
                binding.btnSalida.visibility = View.VISIBLE
            }
            171 -> { // EN TALLER
                binding.btnPosicionado.visibility = View.VISIBLE
                binding.btnMovimientoLocal.visibility = View.VISIBLE
            }
            172 -> { // MOVIMIENTO LOCAL
                binding.btnPosicionado.visibility = View.VISIBLE
                binding.btnEnTaller.visibility = View.VISIBLE
            }
        }
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

    private fun mostrarCargaGuardado() {
        binding.loadingContainerEntrada.visibility = View.VISIBLE
        binding.btnGuardarEntrada.isEnabled = false
        binding.btnGuardarEntrada.alpha = 0.5f

        binding.tvLoadingTextEntrada.text = "Guardando status->Entrada..."
        binding.tvLoadingSubtextEntrada.text = "Actualizando status del vehículo"
    }

    private fun guardarEntrada() {
        if (!validarFormularioEntrada()) {
            return
        }

        mostrarCargaGuardado()
        lifecycleScope.launch {
            try {
                Log.d("Paso1Entrada", "💾 Guardando entrada del vehículo")

                val tipoEntrada = if (binding.rbRodando.isChecked) 1 else 2

                // <CHANGE> Obtener datos correctos según tipo de entrada
                val idTransporte: Int?
                val placa: String
                val numeroEconomico: String?
                val idEmpleadoTransporte: Int?
                val idStatus:Int=168

                if (tipoEntrada == 1) { // RODANDO
                    val posicionTransportista = binding.spinnerEmpresaRodando.selectedItemPosition
                    idTransporte = if (posicionTransportista > 0) transportistas[posicionTransportista - 1].IdCliente else null
                    placa = "" // Para rodando SE MANDA A VACIO
                    numeroEconomico = null
                    idEmpleadoTransporte = null
                } else { // EN MADRINA
                    val posicionTransportista = binding.spinnerEmpresaMadrina.selectedItemPosition
                    val posicionConductor = binding.spinnerConductor.selectedItemPosition

                    idTransporte = if (posicionTransportista > 0) transportistas[posicionTransportista - 1].IdCliente else null
                    idEmpleadoTransporte = if (posicionConductor > 0) empleadosTransportista[posicionConductor - 1].IdClienteEmpleado else null
                    placa = binding.etPlacaTransporte.text.toString().trim()
                    numeroEconomico = binding.etNumeroEconomico.text.toString().trim()
                }

                var annio=vehiculoActual?.Anio!!.toShort()

                Log.d("Paso1Entrada", "📋 Datos a guardar: idTransporte=$idTransporte, placa=$placa, numeroEconomico=$numeroEconomico, idEmpleadoTransporte=$idEmpleadoTransporte")
                val exito = dalPasoLog.crearRegistroEntrada(
                    idVehiculo = vehiculoActual!!.Id!!.toInt(),
                    idUsuario = ParametrosSistema.usuarioLogueado.Id!!.toInt(),
                    tipoEntrada = tipoEntrada,
                    placa = placa,
                    numeroEconomico = numeroEconomico,
                    idTransporte = idTransporte,
                    idEmpleadoTransporte = idEmpleadoTransporte,
                    idStatus = idStatus,
                    fechaMovimiento = fechaActual,
                    annio = annio,
                )
                ocultarCargaGuardado()

                if (exito) {
                    Toast.makeText(this@Paso1Entrada_Activity, "✅ Entrada registrada exitosamente", Toast.LENGTH_SHORT).show()
                    // <CHANGE> Consultar nuevamente para mostrar opciones de transición
                    consultarVehiculo()
                } else {
                    Toast.makeText(this@Paso1Entrada_Activity, "❌ Error guardando entrada", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("Paso1Entrada", "💥 Error guardando entrada: ${e.message}")
                Toast.makeText(this@Paso1Entrada_Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ocultarCargaGuardado() {
        binding.loadingContainerEntrada.visibility = View.GONE
        binding.btnGuardarEntrada.isEnabled = true
        binding.btnGuardarEntrada.alpha = 1.0f
    }

    private fun validarFormularioEntrada(): Boolean {
        if (binding.rbRodando.isChecked) {
            if (binding.spinnerEmpresaRodando.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione la empresa transportista", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            if (binding.spinnerEmpresaMadrina.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione la empresa transportista", Toast.LENGTH_SHORT).show()
                return false
            }
            if (binding.spinnerConductor.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione el conductor", Toast.LENGTH_SHORT).show()
                return false
            }
            if (binding.etPlacaTransporte.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Ingrese la placa del transporte", Toast.LENGTH_SHORT).show()
                return false
            }
            if (binding.etNumeroEconomico.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Ingrese el número económico", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    // <CHANGE> Métodos para manejar overlay de carga
    private fun mostrarCargaConsulta() {
        loadingContainer.visibility = View.VISIBLE
        binding.btnConsultarVehiculo.isEnabled = false
        binding.btnConsultarVehiculo.alpha = 0.5f

        // Ocultar otras secciones mientras carga
        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutOpcionesTransicion.visibility = View.GONE
        binding.layoutRegistrarVIN.visibility = View.GONE

        tvLoadingText.text = "Consultando vehículo..."
        tvLoadingSubtext.text = "Verificando información en base de datos"
    }

    private fun ocultarCargaConsulta() {
        loadingContainer.visibility = View.GONE
        binding.btnConsultarVehiculo.isEnabled = true
        binding.btnConsultarVehiculo.alpha = 1.0f

        // Limpiar handlers
        loadingHandler?.removeCallbacks(loadingRunnable!!)
        loadingHandler = null
        loadingRunnable = null
    }

    private fun mostrarCargaRegistro() {
        loadingContainer.visibility = View.VISIBLE
        binding.btnRegistrarVIN.isEnabled = false
        binding.btnRegistrarVIN.alpha = 0.5f

        tvLoadingText.text = "Registrando vehículo..."
        tvLoadingSubtext.text = "Creando registro en base de datos"
    }

    private fun ocultarCargaRegistro() {
        loadingContainer.visibility = View.GONE
        binding.btnRegistrarVIN.isEnabled = true
        binding.btnRegistrarVIN.alpha = 1.0f
    }

    private fun registrarNuevoVIN() {
        val vin = binding.etVIN.text.toString().trim()
        if (vin.isEmpty() || vin.length < 17) {
            Toast.makeText(this, "Ingrese un VIN válido (17 caracteres)", Toast.LENGTH_SHORT).show()
            return
        }
        mostrarCargaRegistro()
        lifecycleScope.launch {
            try {
                Log.d("Paso1Entrada", "📝 Registrando nuevo VIN: $vin")

                // USAR EL MÉTODO REAL PARA REGISTRAR VIN
                val exito = dalVehiculo.registrarVINBasico(vin)

                ocultarCargaRegistro()

                if (exito) {
                    Toast.makeText(this@Paso1Entrada_Activity, "✅ VIN registrado exitosamente", Toast.LENGTH_SHORT).show()

                    // Limpiar solo el campo VIN y consultar nuevamente
                    limpiarFormularioCompleto()
                    binding.etVIN.setText(vin) // Volver a poner el VIN
                    consultarVehiculo()
                } else {
                    Toast.makeText(this@Paso1Entrada_Activity, "❌ Error registrando VIN", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                ocultarCargaRegistro()
                Log.e("Paso1Entrada", "💥 Error registrando VIN: ${e.message}")
                Toast.makeText(this@Paso1Entrada_Activity, "Error registrando VIN: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // MÉTODOS PARA HORA DINÁMICA
    private fun inicializarHoraDinamica() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                var fechaActualAux = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                binding.tvFechaEntrada.text = "Fecha de entrada: $fechaActualAux"
                timerHandler.postDelayed(this, 1000) // Actualizar cada segundo
            }
        }
        timerHandler.post(timerRunnable)
    }

    private fun detenerHoraDinamica() {
        if (::timerHandler.isInitialized) {
            timerHandler.removeCallbacks(timerRunnable)
        }
    }

    // MÉTODOS PARA CARGAR SPINNERS
    private fun cargarTransportistas() {
        lifecycleScope.launch {
            try {
                transportistas = dalCliente.consultarTransportistas(true)

                val nombresTransportistas = mutableListOf("Seleccionar empresa...")
                nombresTransportistas.addAll(transportistas.map { it.Nombre ?: "Sin nombre" })

                // Configurar adapter para Rodando
                adapterTransportistasRodando = ArrayAdapter(
                    this@Paso1Entrada_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresTransportistas
                )
                adapterTransportistasRodando.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerEmpresaRodando.adapter = adapterTransportistasRodando

                // Configurar adapter para Madrina
                adapterTransportistasMadrina = ArrayAdapter(
                    this@Paso1Entrada_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresTransportistas
                )
                adapterTransportistasMadrina.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerEmpresaMadrina.adapter = adapterTransportistasMadrina

                configurarEventosSpinners()

            } catch (e: Exception) {
                Log.e("Paso1Entrada", "Error cargando transportistas: ${e.message}")
                Toast.makeText(this@Paso1Entrada_Activity, "Error cargando empresas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarEventosSpinners() {
        // Evento para spinner de empresa en Madrina
        binding.spinnerEmpresaMadrina.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // No es "Seleccionar empresa..."
                    val transportistaSeleccionado = transportistas[position - 1]
                    cargarConductores(transportistaSeleccionado.IdCliente!!)
                } else {
                    limpiarSpinnerConductores()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun cargarConductores(idCliente: Int) {
        lifecycleScope.launch {
            try {
                empleadosTransportista= listOf(ClienteEmpleado())
                transportistas.forEach {
                    if(it.IdCliente==idCliente )
                       empleadosTransportista= it.Empleados!!
                }
                val nombresConductores = mutableListOf("Seleccionar conductor...")
                nombresConductores.addAll(empleadosTransportista.map { it.NombreCompleto ?: "Sin nombre" })

                adapterConductores = ArrayAdapter(
                    this@Paso1Entrada_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresConductores
                )
                adapterConductores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerConductor.adapter = adapterConductores

            } catch (e: Exception) {
                Log.e("Paso1Entrada", "Error cargando conductores: ${e.message}")
                Toast.makeText(this@Paso1Entrada_Activity, "Error cargando conductores", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun limpiarSpinnerConductores() {
        val conductoresVacio = listOf("Seleccionar conductor...")
        adapterConductores = ArrayAdapter(
            this@Paso1Entrada_Activity,
            android.R.layout.simple_spinner_item,
            conductoresVacio
        )
        adapterConductores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerConductor.adapter = adapterConductores
    }

    private fun limpiarFormulario() {
        // <CHANGE> Limpiar todos los campos
        binding.etVIN.setText("")
        // Limpiar Spinners
        binding.spinnerEmpresaRodando.setSelection(0)
        binding.spinnerEmpresaMadrina.setSelection(0)
        binding.spinnerConductor.setSelection(0)

        // Limpiar EditText restantes
        binding.etPlacaTransporte.setText("")
        binding.etNumeroEconomico.setText("")
        binding.rbRodando.isChecked = true

        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutOpcionesTransicion.visibility = View.GONE

        vehiculoActual = null
        // <CHANGE> Ocultar también la sección de registrar VIN
        binding.layoutRegistrarVIN.visibility = View.GONE
        binding.etVIN.requestFocus()
    }

    private fun limpiarFormularioCompleto() {
        // Limpiar TODOS los campos incluyendo VIN
        binding.etVIN.setText("")
        // Limpiar Spinners
        binding.spinnerEmpresaRodando.setSelection(0)
        binding.spinnerEmpresaMadrina.setSelection(0)
        binding.spinnerConductor.setSelection(0)

        // Limpiar EditText restantes
        binding.etPlacaTransporte.setText("")
        binding.etNumeroEconomico.setText("")
        binding.rbRodando.isChecked = true

        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutOpcionesTransicion.visibility = View.GONE
        binding.layoutRegistrarVIN.visibility = View.GONE

        vehiculoActual = null

        binding.etVIN.requestFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        detenerHoraDinamica()
    }
}