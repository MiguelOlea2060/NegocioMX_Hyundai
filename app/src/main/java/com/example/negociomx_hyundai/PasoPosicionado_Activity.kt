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

    // Variables para overlay de carga
    private lateinit var loadingContainer: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var tvLoadingSubtext: TextView

   // var IdVehiculo: Int? = null
    var bllBlo: BLLBloque? = null

    // Variables para hora dinámica
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    var fechaActual: String = ""
    private lateinit var bloques: List<Bloque>
    private var posiciones = listOf<PosicionBloque>()

    val gson = Gson()

    // Variable para manejar resultado de selección de posición
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bllBlo = BLLBloque()
           if(intent?.extras!=null)
        {
        //    val jsonVeh=intent.extras?.getString("vehiculo","")
        //    vehiculoActual=gson.fromJson(jsonVeh,VehiculoPasoLog::class.java)
         //   IdVehiculo= vehiculoActual?.Id!!.toInt()
        }

        inicializarComponentes()
        configurarEventos()
        cargarDatosIniciales()
       // inicializarFormulario()
        inicializarHoraDinamica()
    }

    private fun inicializarComponentes() {
        if (ParametrosSistema.CfgGloSql != null &&
            ParametrosSistema.CfgGloSql?.ManejaSeleccionBloquePosXTablero == true
        )
            leeBloquesSistema()

        loadingContainer = findViewById(R.id.loadingContainer)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvLoadingSubtext = findViewById(R.id.tvLoadingSubtext)

        binding.tvEmpleadoReceptor.text = "Empleado receptor: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"



    }

    private fun configurarEventos() {
        // Botón guardar posicionado
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
                // Obtener datos del vehículo por Intent
                obtenerDatosVehiculo()


                empleados = dalEmp.consultarEmpleados(105)
                // Cargar personal
                cargarPersonal()


                if(ParametrosSistema.CfgGloSql!=null &&
                    ParametrosSistema?.CfgGloSql!!.ManejaSeleccionBloquePosXTablero==true) {
                    binding.llSeleccionaPosicionTablero.visibility = View.VISIBLE
                    binding.llSeleccionaPosicionSpinner.visibility=View.GONE
                    binding.llSeleccionBloquesPosicionado.visibility=View.GONE
                }
                else{
                    binding.llSeleccionaPosicionTablero.visibility = View.GONE
                    binding.llSeleccionaPosicionSpinner.visibility=View.VISIBLE
                    binding.llSeleccionBloquesPosicionado.visibility=View.VISIBLE
                    // <CHANGE> Cargar datos para spinners
                    cargarBloques()
                }


                ocultarCarga()
                mostrarFormularios()
                Log.d("PasoSalida_Activity", "✅ Datos iniciales cargados correctamente")

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error cargando datos iniciales: ${e.message}")
                Log.e("PasoSalida_Activity", "Error cargando datos: ${e.message}")
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
                tvVinVehiculo.text = "VIN: ${vehiculo.VIN}"
                tvBlVehiculo.text = "BL: ${vehiculo.BL}"
                tvMarcaModeloAnnio.text = "${vehiculo.Especificaciones}   Año: ${vehiculo.Anio}"
                tvColorExterior.text = "Color Ext: ${vehiculo.ColorExterior}"
                tvColorInteriorVehiculo.text = "Color Int: ${vehiculo.ColorInterior}"
                layoutInfoVehiculo.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarFormularios() {
        // <CHANGE> Mostrar información del vehículo cuando se muestran los formularios
        binding.layoutFormularioPosicionado.visibility = View.VISIBLE
        binding.layoutError.visibility = View.GONE
        Toast.makeText(this, "✅ Vehículo válido para status->Posicionado", Toast.LENGTH_SHORT).show()
    }


    private fun inicializarFormulario() {


     /*   if(vehiculoActual!=null && vehiculoActual?.Id?.toInt()!!>0) {
            binding.tvVinVehiculo.setText(vehiculoActual?.VIN)
        }*/


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
            tvBlVehiculo.text = "BL: ${vehiculo.BL}"
            tvMarcaModeloAnnio.text = "${vehiculo.Especificaciones}, Año: ${vehiculo.Anio}"
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
                    val posicionBloque = binding.spinnerBloque.selectedItemPosition
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

                val posicionEmpleado=binding.spinnerPersonal.selectedItemPosition
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
                    NumeroEconomico = "",
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
            binding.spinnerBloque.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el bloque", Toast.LENGTH_SHORT).show()
            return false
        }
        if (posicionSeleccionadaManual == null) {
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
                fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val fechaActualAux = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                binding.tvFechaMovimiento.text = "Fecha actual: $fechaActualAux"
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
                binding.spinnerPersonal.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando personal: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando personal", Toast.LENGTH_SHORT).show()
            }
      //  }
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
                binding.spinnerBloque.adapter = adapter

                binding.spinnerBloque.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position > 0) {
                            // Abrir pantalla gráfica de posiciones
                            if(ParametrosSistema.CfgGloSql?.ManejaSeleccionBloquePosXTablero==true)
                                abrirPantallaPosicionGrafica()
                            else
                                cargarPosiciones(position )
                        } else {
                            binding.spinnerPosicion.adapter = null
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
                binding.spinnerPosicion.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoPosicionado", "Error cargando posiciones: ${e.message}")
                Toast.makeText(this@PasoPosicionado_Activity, "Error cargando posiciones", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun mostrarCarga(mensaje: String, submensaje: String = "") {
        // <CHANGE> Optimizado usando apply para reducir accesos al binding
        binding.apply {
            loadingContainer.visibility = View.VISIBLE
            tvLoadingText.text = mensaje
            tvLoadingSubtext.text = submensaje
            tvLoadingSubtext.visibility = if(submensaje.isNotEmpty()) View.VISIBLE else View.GONE
            btnGuardarPosicionado.isEnabled = false
            btnGuardarPosicionado.alpha = 0.5f
        }
    }

    private fun ocultarCarga() {
        // <CHANGE> Optimizado usando apply para reducir accesos al binding
        binding.apply {
            loadingContainer.visibility = View.GONE
            btnGuardarPosicionado.isEnabled = true
            btnGuardarPosicionado.alpha = 1.0f
        }
    }

    private fun mostrarError(mensaje: String) {
        // <CHANGE> Optimizado usando apply para reducir accesos al binding
        binding.apply {
            tvMensajeError.text = mensaje
            layoutError.visibility = View.VISIBLE
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
        // Guardar la posición seleccionada
        val bloque=bloques.filter { it.IdBloque==posicion.IdBloque }.firstOrNull()
        if(bloque!=null) {
            posicionSeleccionadaManual = posicion
            var nombrePosicion: String = "Bloque: ${bloque.Nombre}, " +
                    "Col.: ${posicion.Columna} -> Fila: ${posicion.Fila}"

            binding.lblBloquePosicionSeleccionada.setText(nombrePosicion)

            // Crear adapter con la posición seleccionada
            Toast.makeText(this, "Posición seleccionada: $nombrePosicion", Toast.LENGTH_SHORT)
                .show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        detenerHoraDinamica()
    }
}








