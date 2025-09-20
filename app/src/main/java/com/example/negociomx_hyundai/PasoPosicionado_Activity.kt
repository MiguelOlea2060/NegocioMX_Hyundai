package com.example.negociomx_hyundai

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import com.example.negociomx_hyundai.BE.Bloque
import com.example.negociomx_hyundai.BE.Empleado
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.PosicionBloque
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.BLL.BLLBloque
import com.example.negociomx_hyundai.DAL.DALCliente
import com.example.negociomx_hyundai.DAL.DALEmpleadoSQL
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
    private val dalEmp=DALEmpleadoSQL()
    private var vehiculoActual: VehiculoPasoLog? = null
    private var statusActual: PasoLogVehiculoDet? = null
    private var empleados = listOf<Empleado>()

    // Variables para overlay de carga
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView

    var IdVehiculo:Int?=null
    var bllBlo:BLLBloque?=null
    // Variables para hora dinámica
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    private lateinit var bloques : List<Bloque>
    private var posiciones = listOf<PosicionBloque>()

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

        bllBlo=BLLBloque()
        if(intent?.extras!=null)
        {
            val idPasoLogVehiculo= intent.extras?.getInt("IdPasoLogVehiculo",0)?:0
            IdVehiculo= intent.extras?.getInt("IdVehiculo",0)?:0
            val marca= intent.extras?.getString("Marca","")?:""
            val modelo= intent.extras?.getString("Modelo","")?:""
            val bl= intent.extras?.getString("Bl","")?:""
            val vin= intent.extras?.getString("Vin","")?:""
            val colorExterior= intent.extras?.getString("ColorExterior","")?:""
            val colorInterior= intent.extras?.getString("ColorInterior","")?:""
            vehiculoActual=VehiculoPasoLog(
                Id =IdVehiculo.toString(),
                Marca = marca,
                Modelo = modelo,
                BL =bl,
                VIN = vin,
                ColorExterior = colorExterior,
                ColorInterior = colorInterior,
                IdPasoLogVehiculo = idPasoLogVehiculo,
            )
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
        // Botón guardar posicionado
        binding.btnGuardarPosicionado.setOnClickListener {
            guardarPosicionado()
        }

        binding.btnRegresarPosicionado.setOnClickListener {
            finish()
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

        if(vehiculoActual!=null && vehiculoActual?.Id?.toInt()!!>0) {
            binding.tvVinVehiculo.setText(vehiculoActual?.VIN)
        }
        mostrarInformacionVehiculo(vehiculoActual!!)
        mostrarFormularioPosicionado()
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }
    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun mostrarInformacionVehiculo(vehiculo: VehiculoPasoLog) {
        binding.apply {
            tvVinVehiculo.text = "VIN: ${vehiculo.VIN}"
            tvBlVehiculo.text = "MBL: ${vehiculo.BL}"
            tvMarcaModeloAnnio.text = "${vehiculo.Marca} - ${vehiculo.Modelo}, ${vehiculo.Anio}"
            tvColorExterior.text = "Color Ext.: ${vehiculo.ColorExterior}"
            tvColorInteriorVehiculo.text = "Color Int.: ${vehiculo.ColorInterior}"

            layoutInfoVehiculo.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularioPosicionado() {
        binding.layoutFormularioPosicionado.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        Toast.makeText(this, "✅ Vehículo válido para posicionado", Toast.LENGTH_SHORT).show()
    }

    private fun guardarPosicionado() {
        if (!validarFormularioPosicionado()) {
            return
        }

        mostrarCargaGuardado()
        lifecycleScope.launch {
            try {
                Log.d("PasoPosicionado", "💾 Guardando posicionado del vehículo")

                val posicionBloque = binding.spinnerBloque.selectedItemPosition
                val bloque=bloques[posicionBloque-1]
                val idBloque:Short = bloque.IdBloque

                val posicionEmpleado=binding.spinnerPersonal.selectedItemPosition
                val empleado= empleados[posicionEmpleado-1]
                val idEmpleadoPosiciono=empleado.IdEmpleado
                val nombrePersonalMovimiento = empleado.NombreCompleto

                val posicionPosicion = binding.spinnerPosicion.selectedItemPosition
                val posicionSeleccionada = posiciones[posicionPosicion-1]
                val fila =  posicionSeleccionada.Fila
                val columna =  posicionSeleccionada.Columna

                var idUsuario=ParametrosSistema.usuarioLogueado.Id?.toInt()

                val paso=PasoLogVehiculoDet(
                    IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo,
                    IdEmpleadoTransporte =null,
                    IdEmpleadoPosiciono = idEmpleadoPosiciono,
                    Fila = fila,
                    Columna = columna,
                    IdBloque = idBloque,
                    IdStatus = 170,
                    IdTransporte = null,
                    IdTipoMovimiento = null,
                    IdUsuarioMovimiento = idUsuario,
                    IdPasoLogVehiculoDet = 0,
                    IdParteDanno = null,
                    IdTipoEntradaSalida = null,
                    EnviadoAInterface = null,
                    FechaEnviado = null,
                    Observacion = null,
                    FechaMovimiento = "",
                    NumeroEconomico = "",
                    Bloque = bloque.Nombre,
                    Placa = "",
                    PersonaQueHaraMovimiento = nombrePersonalMovimiento,
                    IdVehiculo = IdVehiculo
                )
                val exito = dalPasoLog.insertaStatusNuevoPasoLogVehiculo(paso)
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
                empleados = dalEmp.consultarEmpleados(105)

                val nombresPersonal = mutableListOf("Seleccionar personal...")
                nombresPersonal.addAll(empleados.map { it.NombreCompleto ?: "Sin nombre" })

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
                bloques.forEach {
                    nombresBloques.add(it.Nombre)
                }

                val adapter = ArrayAdapter(
                    this@PasoPosicionado_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresBloques
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerBloque.adapter = adapter

                binding.spinnerBloque.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0) {
                            cargarPosiciones(position )
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

    private fun cargarPosiciones(posicion: Int) {
        lifecycleScope.launch {
            try {
                var bloque:Bloque=bloques[posicion]
                posiciones = bllBlo?.getPosicionesDisponiblesDeBloque(bloque)!!

                val nombresPosiciones = mutableListOf("Seleccionar posición...")
                posiciones.forEach {
                    nombresPosiciones.add(it.Nombre)
                }

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

    private fun limpiarFormulario() {
        binding.spinnerBloque.setSelection(0)
        binding.spinnerPosicion.setSelection(0)
        binding.spinnerPersonal.setSelection(0)

        binding.layoutInfoVehiculo.visibility = View.GONE
        binding.layoutFormularioPosicionado.visibility = View.GONE
        binding.layoutError.visibility = View.GONE

        vehiculoActual = null
        statusActual = null
    }
    override fun onDestroy() {
        super.onDestroy()
        detenerHoraDinamica()
    }
}








