package com.example.negociomx_hyundai

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PasoPosicionado_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPasoPosicionadoBinding
    private val dalPasoLog = DALPasoLogVehiculo()
    private val dalCliente = DALCliente()
    private val dalEmp = DALEmpleadoSQL()
    private var vehiculoActual: VehiculoPasoLog? = null
    private var statusActual: PasoLogVehiculoDet? = null
    private var empleados = listOf<Empleado>()
    var bllBlo: BLLBloque? = null
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    var fechaActual: String = ""
    private lateinit var bloques: List<Bloque>
    private var posiciones = listOf<PosicionBloque>()
    val gson = Gson()
    private val seleccionPosicionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val posicionSeleccionada = result.data?.getSerializableExtra("posicion_seleccionada")
                    as? PosicionBloque
            if (posicionSeleccionada != null) {
                // Actualizar spinner con la posición seleccionada
                actualizarSpinnerConPosicionSeleccionada(posicionSeleccionada)
            }
        }
    }

    private var posicionSeleccionadaManual: PosicionBloque? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasoPosicionadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainPosicionado)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bllBlo = BLLBloque()


        inicializarComponentes()
        configurarEventos()
        cargarDatosIniciales()
    }

    private fun inicializarComponentes() {
        if (ParametrosSistema.CfgGloSql != null &&
            ParametrosSistema.CfgGloSql?.ManejaSeleccionBloquePosXTablero == true
        )leeBloquesSistema()
        binding.tvEmpleadoReceptorPosicionado.text = "Empleado receptor: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"
        inicializarHoraDinamica()
    }

    private fun configurarEventos() {
        binding.btnGuardarPosicionado.setOnClickListener {
            guardarStatusPosicionado()
        }
        binding.btnSeleccionaPosicionPosicionado.setOnClickListener {
            abrirPantallaPosicionGrafica()
        }
        binding.btnRegresarPosicionado.setOnClickListener {
            finish()
        }
    }



    private fun cargarDatosIniciales() {
        //obtenerDatosVehiculo()
        mostrarCarga("Cargando datos iniciales...", "Consultando empleados y tipos de movimiento")

        lifecycleScope.launch {
            try {
                obtenerDatosVehiculo()

                empleados = dalEmp.consultarEmpleados(105,null)
                // Cargar personal
                cargarPersonal()

                if(ParametrosSistema.CfgGloSql!=null &&
                    ParametrosSistema?.CfgGloSql!!.ManejaSeleccionBloquePosXTablero==true) {
                    binding.llSeleccionaPosicionTableroPosicionado.visibility = View.VISIBLE
                    binding.llSeleccionaPosicionSpinnerPosicionado.visibility=View.GONE
                    binding.llSeleccionBloquesPosicionado.visibility=View.GONE
                }
                else{
                    binding.llSeleccionaPosicionTableroPosicionado.visibility = View.GONE
                    binding.llSeleccionaPosicionSpinnerPosicionado.visibility=View.VISIBLE
                    binding.llSeleccionBloquesPosicionado.visibility=View.VISIBLE
                    cargarBloques()
                }

                ocultarCarga()
                mostrarFormularios()
                Log.d("PasoPosicionado_Activity", "✅ Datos iniciales cargados correctamente")

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error cargando datos iniciales: ${e.message}")
                Log.e("PasoPosicionado_Activity", "Error cargando datos: ${e.message}")
            }
        }
    }



    private fun obtenerDatosVehiculo() {
        try {
            val jsonVeh = intent.getStringExtra("vehiculo") ?: ""

            if (jsonVeh.isNotEmpty()) {
                vehiculoActual = gson.fromJson(jsonVeh,VehiculoPasoLog::class.java)
                mostrarInfoVehiculo()
                Log.d("PasoPosicionado_Activity", "✅ Datos del vehículo obtenidos: VIN=${vehiculoActual?.VIN}")
            } else {
                mostrarError("No se recibieron datos válidos del vehículo")
            }
        } catch (e: Exception) {
            mostrarError("Error obteniendo datos del vehículo: ${e.message}")
            Log.e("PasoPosicionado_Activity", "Error obteniendo datos: ${e.message}")
        }
    }

    private fun mostrarInfoVehiculo() {
        vehiculoActual?.let { vehiculo ->
            // <CHANGE> Optimizado usando apply para reducir accesos al binding
            binding.apply {
                tvVinVehiculoPosicionado.text = "VIN: ${vehiculo.VIN}"
                tvBlVehiculoPosicionado.text = "BL: ${vehiculo.BL}"
                tvMarcaModeloAnnioPosicionado.text = "${vehiculo.Especificaciones}   Año: ${vehiculo.Anio}"
                tvColorExteriorPosicionado.text = "Color Ext: ${vehiculo.ColorExterior}"
                tvColorInteriorVehiculoPosicionado.text = "Color Int: ${vehiculo.ColorInterior}"
                layoutInfoVehiculoPosicionado.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarFormularios() {
        // <CHANGE> Mostrar información del vehículo cuando se muestran los formularios
        binding.layoutFormularioPosicionado.visibility = View.VISIBLE
        binding.layoutErrorPosicionado.visibility = View.GONE
        Toast.makeText(this, "✅ Vehículo válido para status->Posicionado", Toast.LENGTH_SHORT).show()
    }


    private fun inicializarFormulario() {
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
            tvVinVehiculoPosicionado.text = "VIN: ${vehiculo.VIN}"
            tvBlVehiculoPosicionado.text = "BL: ${vehiculo.BL}"
            tvMarcaModeloAnnioPosicionado.text = "${vehiculo.Especificaciones}, Año: ${vehiculo.Anio}"
            tvColorExteriorPosicionado.text = "Color Ext.: ${vehiculo.ColorExterior}"
            tvColorInteriorVehiculoPosicionado.text = "Color Int.: ${vehiculo.ColorInterior}"
            layoutInfoVehiculoPosicionado.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularioPosicionado() {
        binding.layoutFormularioPosicionado.visibility = View.VISIBLE
        binding.layoutErrorPosicionado.visibility = View.GONE
        Toast.makeText(this, "✅ Vehículo válido para posicionado", Toast.LENGTH_SHORT).show()
    }

    private fun guardarStatusPosicionado() {
        if (!validarFormularioPosicionado()) {
            return
        }

        mostrarCargaGuardado()
        lifecycleScope.launch {
            try {
                Log.d("PasoPosicionado", "💾 Guardando Status->Posicionado del vehículo")
                var idBloque:Short = 0
                var nombreBloque:String=""

                if(ParametrosSistema.CfgGloSql!=null &&
                    ParametrosSistema.CfgGloSql?.ManejaSeleccionBloquePosXTablero==false) {
                    val posicionBloque = binding.spinnerBloquePosicionado.selectedItemPosition
                    val bloque = bloques[posicionBloque - 1]
                    idBloque = bloque.IdBloque
                    nombreBloque=bloque.Nombre
                }
                else
                {
                    idBloque=posicionSeleccionadaManual?.IdBloque!!
                    val bloque=bloques.filter { it.IdBloque==posicionSeleccionadaManual?.IdBloque }.first()
                    nombreBloque=bloque.Nombre
                }

                val posicionEmpleado=binding.spinnerPersonalPosicionado.selectedItemPosition
                val empleado= empleados[posicionEmpleado-1]
                val idEmpleadoPosiciono=empleado.IdEmpleado
                val nombrePersonalMovimiento = empleado.NombreCompleto

                // Usar la posición seleccionada manualmente
                if (posicionSeleccionadaManual == null) {
                    Toast.makeText(this@PasoPosicionado_Activity, "Debe seleccionar una posición", Toast.LENGTH_SHORT).show()
                    ocultarCargaGuardado()
                    return@launch
                }

                val posicionSeleccionada = posicionSeleccionadaManual!!
                val fila = posicionSeleccionada.Fila
                val columna = posicionSeleccionada.Columna

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
                    FechaMovimiento = fechaActual,
                    NumeroEconomico = null,
                    Bloque = nombreBloque,
                    Placa = "",
                    PersonaQueHaraMovimiento = nombrePersonalMovimiento,
                    IdVehiculo = vehiculoActual?.Id?.toInt()
                )
                val exito = dalPasoLog.insertaStatusNuevoPasoLogVehiculo(paso)
                ocultarCargaGuardado()

                if (exito) {
                    Toast.makeText(this@PasoPosicionado_Activity, "✅ Vehículo posicionado exitosamente", Toast.LENGTH_SHORT).show()

                    val data = Intent()
                    data.putExtra("Refrescar", true);
                    data.putExtra("Vin", vehiculoActual?.VIN);
                    setResult(Activity.RESULT_OK,data)
                    finish() // Cerrar la actividad actual
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
        if (ParametrosSistema.CfgGloSql?.ManejaSeleccionBloquePosXTablero==false &&
            binding.spinnerBloquePosicionado.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el bloque", Toast.LENGTH_SHORT).show()
            return false
        }
        if (posicionSeleccionadaManual == null) {
            Toast.makeText(this, "Seleccione la posición", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.spinnerPersonalPosicionado.selectedItemPosition == 0) {
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
                fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val fechaActualAux = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                binding.tvFechaMovimientoPosicionado.text = "Fecha actual: $fechaActualAux"
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
       // lifecycleScope.launch {
            try {


                val nombresPersonal = mutableListOf("Seleccionar personal...")
                nombresPersonal.addAll(empleados.map { it.NombreCompleto ?: "Sin nombre" })

                val adapter = ArrayAdapter(
                    this@PasoPosicionado_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresPersonal
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerPersonalPosicionado.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando personal: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando personal", Toast.LENGTH_SHORT).show()
            }
      //  }
    }

    private fun mostrarCargaGuardado() {
        binding.apply {
            loadingContainerPosicionado.visibility = View.VISIBLE
            btnGuardarPosicionado.isEnabled = false
            btnGuardarPosicionado.alpha = 0.5f
            tvLoadingTextPosicionado.text = "Guardando posicionado..."
            tvLoadingSubtextPosicionado.text = "Actualizando status del vehículo"
        }

    }

    private fun ocultarCargaGuardado() {
        binding.apply {
            loadingContainerPosicionado.visibility = View.GONE
            btnGuardarPosicionado.isEnabled = true
            btnGuardarPosicionado.alpha = 1.0f
        }
    }

    private fun leeBloquesSistema() {
        lifecycleScope.launch {
            try {
                    bloques = dalPasoLog.consultarBloques()
                } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando bloques: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando bloques", Toast.LENGTH_SHORT).show()
            }
        }
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
                binding.spinnerBloquePosicionado.adapter = adapter

                binding.spinnerBloquePosicionado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0) {
                            // Abrir pantalla gráfica de posiciones
                            if(ParametrosSistema.CfgGloSql?.ManejaSeleccionBloquePosXTablero==true)
                                abrirPantallaPosicionGrafica()
                            else
                                cargarPosiciones(position )
                        } else {
                            binding.spinnerPosicionPosicionado.adapter = null
                            posicionSeleccionadaManual = null
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
                var bloque:Bloque=bloques[posicion-1]
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
                binding.spinnerPosicionPosicionado.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando posiciones: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando posiciones", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun mostrarCarga(mensaje: String, submensaje: String = "") {
        binding.apply {
            loadingContainerPosicionado.visibility = View.VISIBLE
            tvLoadingTextPosicionado.text = mensaje
            tvLoadingSubtextPosicionado.text = submensaje
            tvLoadingSubtextPosicionado.visibility = if(submensaje.isNotEmpty()) View.VISIBLE else View.GONE
            btnGuardarPosicionado.isEnabled = false
            btnGuardarPosicionado.alpha = 0.5f
        }
    }

    private fun ocultarCarga() {
        //Optimizado usando apply para reducir accesos al binding
        binding.apply {
            loadingContainerPosicionado.visibility = View.GONE
            btnGuardarPosicionado.isEnabled = true
            btnGuardarPosicionado.alpha = 1.0f
        }
    }

    private fun mostrarError(mensaje: String) {
        binding.apply {
            tvMensajeErrorPosicionado.text = mensaje
            layoutErrorPosicionado.visibility = View.VISIBLE
        }
    }
    private fun limpiarFormulario() {
        binding.apply {
            binding.spinnerBloquePosicionado.setSelection(0)
            spinnerPosicionPosicionado.setSelection(0)
            spinnerPersonalPosicionado.setSelection(0)
            layoutInfoVehiculoPosicionado.visibility = View.GONE
            layoutFormularioPosicionado.visibility = View.GONE
            layoutErrorPosicionado.visibility = View.GONE
        }
        vehiculoActual = null
        statusActual = null
    }

    private fun abrirPantallaPosicionGrafica() {
        try {
            val gson=Gson()
            val jsonBloques=gson.toJson(bloques)
            val intent = Intent(this, PosicionGrafica_Activity::class.java)
            intent.putExtra("bloques", jsonBloques)
            seleccionPosicionLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("PasoPosicionado", "Error abriendo pantalla gráfica: ${e.message}")
            Toast.makeText(this, "Error abriendo selección de posiciones", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarSpinnerConPosicionSeleccionada(posicion: PosicionBloque) {
        val bloque=bloques.filter { it.IdBloque==posicion.IdBloque }.firstOrNull()
        if(bloque!=null) {
            posicionSeleccionadaManual = posicion
            var nombrePosicion: String = "Bloque: ${bloque.Nombre}, " +
                    "Col.: ${posicion.Columna} -> Fila: ${posicion.Fila}"
            binding.lblBloquePosicionSeleccionadaPosicionado.setText(nombrePosicion)
            Toast.makeText(this, "Posición seleccionada: $nombrePosicion", Toast.LENGTH_SHORT)
                .show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        detenerHoraDinamica()
    }
}








