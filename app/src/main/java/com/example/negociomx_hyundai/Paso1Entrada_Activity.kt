package com.example.negociomx_hyundai

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
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.Vehiculo
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityPaso1EntradaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// <CHANGE> Agregar imports para Handler y Looper
import android.os.Handler
import android.widget.LinearLayout
import android.widget.TextView
import com.example.negociomx_hyundai.BE.VehiculoPasoLog

class Paso1Entrada_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityPaso1EntradaBinding
    private val dalVehiculo = DALVehiculo()
    private val dalPasoLog = DALPasoLogVehiculo()
    private var vehiculoActual: VehiculoPasoLog? = null
    private var statusActual: PasoLogVehiculoDet? = null
    // <CHANGE> Agregar variables para overlay de carga
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView
    private var loadingHandler: Handler? = null
    private var loadingRunnable: Runnable? = null

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
            // TODO: Navegar a Activity de Posicionado
            Toast.makeText(this, "Navegar a Posicionado", Toast.LENGTH_SHORT).show()
        }

        binding.btnMovimientoLocal.setOnClickListener {
            // TODO: Navegar a Activity de Movimiento Local
            Toast.makeText(this, "Navegar a Movimiento Local", Toast.LENGTH_SHORT).show()
        }

        binding.btnEnTaller.setOnClickListener {
            // TODO: Navegar a Activity de En Taller
            Toast.makeText(this, "Navegar a En Taller", Toast.LENGTH_SHORT).show()
        }

        binding.btnSalida.setOnClickListener {
            // TODO: Navegar a Activity de Salida
            Toast.makeText(this, "Navegar a Salida", Toast.LENGTH_SHORT).show()
        }

        // <CHANGE> Botón para registrar VIN nuevo
        binding.btnRegistrarVIN.setOnClickListener {
            registrarNuevoVIN()
        }
    }

    private fun inicializarFormulario() {
        // <CHANGE> Inicializar datos del formulario
        val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        binding.tvFechaEntrada.text = "Fecha de entrada: $fechaActual"
        binding.tvEmpleadoReceptor.text = "Empleado receptor: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"
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

                    if (vehiculoActual?.IdPasoLogVehiculo == 0) {
                        mostrarFormularioEntrada()
                    } else {
                        mostrarOpcionesTransicion()
                    }

                } else {
                    // <CHANGE> Mostrar opción para registrar VIN en lugar de solo mensaje
                    mostrarOpcionRegistrarVIN()
                }

            } catch (e: Exception) {
                // <CHANGE> Ocultar carga en caso de error
                ocultarCargaConsulta()
                Log.e("Paso1Entrada", "💥 Error consultando vehículo: ${e.message}")
                Toast.makeText(this@Paso1Entrada_Activity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarInformacionVehiculo(vehiculo: VehiculoPasoLog) {
        // <CHANGE> Mostrar información del vehículo (copiado de Paso1SOC)
        binding.apply {
            tvBlVehiculo.text = "MBL: ${vehiculo.BL}"
            tvMarcaModeloAnnio.text = "${vehiculo.Marca} - ${vehiculo.Modelo}, ${vehiculo.Anio}"
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

        val idStatusActual = statusActual?.IdStatus ?: 0
        binding.tvStatusActual.text = "Status actual: ${obtenerNombreStatus(idStatusActual)}"

        // Mostrar botones según reglas de transición
        configurarBotonesTransicion(idStatusActual)

        Toast.makeText(this, "✅ Vehículo ya registrado. Seleccione acción", Toast.LENGTH_SHORT).show()
    }

    private fun configurarBotonesTransicion(statusActual: Int) {
        // <CHANGE> Configurar botones según reglas de negocio
        // Ocultar todos primero
        binding.btnPosicionado.visibility = View.GONE
        binding.btnMovimientoLocal.visibility = View.GONE
        binding.btnEnTaller.visibility = View.GONE
        binding.btnSalida.visibility = View.GONE

        when (statusActual) {
            1 -> { // Entrada
                binding.btnPosicionado.visibility = View.VISIBLE
                binding.btnMovimientoLocal.visibility = View.VISIBLE
                binding.btnEnTaller.visibility = View.VISIBLE
            }
            2 -> { // Posicionado
                binding.btnMovimientoLocal.visibility = View.VISIBLE
                binding.btnEnTaller.visibility = View.VISIBLE
                binding.btnSalida.visibility = View.VISIBLE
            }
            3 -> { // En Taller
                binding.btnPosicionado.visibility = View.VISIBLE
                binding.btnMovimientoLocal.visibility = View.VISIBLE
            }
            4 -> { // Movimiento Local
                binding.btnPosicionado.visibility = View.VISIBLE
                binding.btnEnTaller.visibility = View.VISIBLE
            }
        }
    }

    private fun obtenerNombreStatus(idStatus: Int): String {
        return when (idStatus) {
            1 -> "Entrada"
            2 -> "Posicionado"
            3 -> "En Taller"
            4 -> "Movimiento Local"
            5 -> "Salida"
            else -> "Desconocido"
        }
    }

    private fun guardarEntrada() {
        if (!validarFormularioEntrada()) {
            return
        }

        lifecycleScope.launch {
            try {
                Log.d("Paso1Entrada", "💾 Guardando entrada del vehículo")

                val tipoEntrada = if (binding.rbRodando.isChecked) 1 else 2
                val placa = if (tipoEntrada == 1)
                    binding.etEmpresaRodando.text.toString()
                else binding.etPlacaTransporte.text.toString()

                val numeroEconomico = if (tipoEntrada == 2)
                    binding.etNumeroEconomico.text.toString()
                else null

                val exito = dalPasoLog.crearRegistroEntrada(
                    idVehiculo = vehiculoActual!!.Id!!.toInt(),
                    idUsuario = ParametrosSistema.usuarioLogueado.Id!!.toInt(),
                    tipoEntrada = tipoEntrada,
                    placa = placa,
                    numeroEconomico = numeroEconomico
                )

                if (exito) {
                    Toast.makeText(this@Paso1Entrada_Activity, "✅ Entrada registrada exitosamente", Toast.LENGTH_SHORT).show()
                    limpiarFormulario()
                    mostrarOpcionesTransicion()
                } else {
                    Toast.makeText(this@Paso1Entrada_Activity, "❌ Error guardando entrada", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("Paso1Entrada", "💥 Error guardando entrada: ${e.message}")
                Toast.makeText(this@Paso1Entrada_Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validarFormularioEntrada(): Boolean {
        // <CHANGE> Validar campos según tipo de entrada
        if (binding.rbRodando.isChecked) {
            if (binding.etEmpresaRodando.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Ingrese la empresa que trajo el vehículo", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            if (binding.etPlacaTransporte.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Ingrese la placa del transporte", Toast.LENGTH_SHORT).show()
                return false
            }
            if (binding.etNombreConductor.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre del conductor", Toast.LENGTH_SHORT).show()
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

    // <CHANGE> Métodos para manejar VIN no encontrado
    private fun mostrarOpcionRegistrarVIN() {
        binding.layoutRegistrarVIN.visibility = View.VISIBLE
        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutOpcionesTransicion.visibility = View.GONE

        Toast.makeText(this, "❌ Vehículo no encontrado", Toast.LENGTH_SHORT).show()
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



    private fun limpiarFormulario() {
        // <CHANGE> Limpiar todos los campos
        binding.etVIN.setText("")
        binding.etEmpresaRodando.setText("")
        binding.etPlacaTransporte.setText("")
        binding.etNumeroEconomico.setText("")
        binding.etNombreConductor.setText("")
        binding.rbRodando.isChecked = true

        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutOpcionesTransicion.visibility = View.GONE

        vehiculoActual = null
        statusActual = null

        // <CHANGE> Ocultar también la sección de registrar VIN
        binding.layoutRegistrarVIN.visibility = View.GONE
        binding.etVIN.requestFocus()
    }

    private fun limpiarFormularioCompleto() {
        // Limpiar TODOS los campos incluyendo VIN
        binding.etVIN.setText("")
        binding.etEmpresaRodando.setText("")
        binding.etPlacaTransporte.setText("")
        binding.etNumeroEconomico.setText("")
        binding.etNombreConductor.setText("")
        binding.rbRodando.isChecked = true

        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutOpcionesTransicion.visibility = View.GONE
        binding.layoutRegistrarVIN.visibility = View.GONE

        vehiculoActual = null
        statusActual = null

        binding.etVIN.requestFocus()
    }
}