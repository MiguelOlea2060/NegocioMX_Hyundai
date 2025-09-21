package com.example.negociomx_hyundai

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.Empleado
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.BLL.BLLBloque
import com.example.negociomx_hyundai.DAL.DALEmpleadoSQL
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityPasoSalidaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PasoSalida_Activity : AppCompatActivity() {

    private lateinit var binding:ActivityPasoSalidaBinding
    private var empleados = listOf<Empleado>()
    private val dalEmp= DALEmpleadoSQL()
    private val dalPasoLog = DALPasoLogVehiculo()

    // Variables para overlay de carga
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView

    private var vehiculoActual: VehiculoPasoLog? = null
    private var statusActual: PasoLogVehiculoDet? = null

    // Variables para hora dinámica
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    var IdVehiculo:Int?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPasoSalidaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

    private fun inicializarFormulario() {
        // Inicializar empleado receptor
        binding.tvEmpleadoReceptorSalida.text = "Empleado Responsable: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"

        // Inicializar hora dinámica
        inicializarHoraDinamica()
        // Cargar personal
        cargarPersonal()

        if(vehiculoActual!=null && vehiculoActual?.Id?.toInt()!!>0) {
            binding.tvVinVehiculoSalida.setText(vehiculoActual?.VIN)
        }
        mostrarInformacionVehiculo(vehiculoActual!!)
        mostrarFormularioPosicionado()
    }

    private fun mostrarInformacionVehiculo(vehiculo: VehiculoPasoLog) {
        binding.apply {
            tvVinVehiculoSalida.text = "VIN: ${vehiculo.VIN}"
            tvBlVehiculoSalida.text = "MBL: ${vehiculo.BL}"
            tvMarcaModeloAnnioSalida.text = "${vehiculo.Marca} - ${vehiculo.Modelo}, ${vehiculo.Anio}"
            tvColorExteriorSalida.text = "Color Ext.: ${vehiculo.ColorExterior}"
            tvColorInteriorVehiculoSalida.text = "Color Int.: ${vehiculo.ColorInterior}"

            layoutInfoVehiculoSalida.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularioPosicionado() {
        binding.layoutFormularioSalida.visibility = View.VISIBLE
        binding.layoutErrorSalida.visibility = View.GONE
        Toast.makeText(this, "✅ Vehículo válido para posicionado", Toast.LENGTH_SHORT).show()
    }

    // MÉTODOS PARA CARGAR PERSONAL
    private fun cargarPersonal() {
        lifecycleScope.launch {
            try {
                empleados = dalEmp.consultarEmpleados(105)

                val nombresPersonal = mutableListOf("Seleccionar personal...")
                nombresPersonal.addAll(empleados.map { it.NombreCompleto ?: "Sin nombre" })

                val adapter = ArrayAdapter(
                    this@PasoSalida_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresPersonal
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerPersonalSalida.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando personal: ${e.message}")
                Toast.makeText(this@PasoSalida_Activity, "Error cargando personal", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }
    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun configurarEventos() {
        // Botón guardar posicionado
        binding.btnGuardarSalida.setOnClickListener {
            guardarPosicionado()
        }

        binding.btnRegresarSalida.setOnClickListener {
            finish()
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

                val posicionEmpleado=binding.spinnerPersonalSalida.selectedItemPosition
                val empleado= empleados[posicionEmpleado-1]
                val idEmpleadoPosiciono=empleado.IdEmpleado
                val nombrePersonalMovimiento = empleado.NombreCompleto

                val idBloque:Short? =  null
                val fila:Short? =  null
                val columna:Short? =  null

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
                    Bloque = "",
                    Placa = "",
                    PersonaQueHaraMovimiento = nombrePersonalMovimiento,
                    IdVehiculo = IdVehiculo
                )
                val exito = dalPasoLog.insertaStatusNuevoPasoLogVehiculo(paso)
                ocultarCargaGuardado()

                if (exito) {
                    Toast.makeText(this@PasoSalida_Activity, "✅ Vehículo posicionado exitosamente", Toast.LENGTH_SHORT).show()
                    limpiarFormulario()
                } else {
                    Toast.makeText(this@PasoSalida_Activity, "❌ Error guardando posicionado", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                ocultarCargaGuardado()
                Log.e("PasoPosicionado", "💥 Error guardando posicionado: ${e.message}")
                Toast.makeText(this@PasoSalida_Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ocultarCargaGuardado() {
        loadingContainer.visibility = View.GONE
        binding.btnGuardarSalida.isEnabled = true
        binding.btnGuardarSalida.alpha = 1.0f
    }

    private fun validarFormularioPosicionado(): Boolean {
        if (binding.spinnerPersonalSalida.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el personal que lo manejará", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun mostrarCargaGuardado() {
        loadingContainer.visibility = View.VISIBLE
        binding.btnGuardarSalida.isEnabled = false
        binding.btnGuardarSalida.alpha = 0.5f

        tvLoadingText.text = "Guardando posicionado..."
        tvLoadingSubtext.text = "Actualizando status del vehículo"
    }


    private fun limpiarFormulario() {
        binding.spinnerPersonalSalida.setSelection(0)

        binding.layoutInfoVehiculoSalida.visibility = View.GONE
        binding.layoutFormularioSalida.visibility = View.GONE
        binding.layoutErrorSalida.visibility = View.GONE

        vehiculoActual = null
        statusActual = null
    }

    // MÉTODOS PARA HORA DINÁMICA
    private fun inicializarHoraDinamica() {
        timerHandler = Handler(Looper.getMainLooper())
        timerRunnable = object : Runnable {
            override fun run() {
                val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(
                    Date()
                )
                binding.tvFechaMovimientoSalida.text = "Fecha de movimiento: $fechaActual"
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

    override fun onDestroy() {
        super.onDestroy()
        detenerHoraDinamica()
    }

}