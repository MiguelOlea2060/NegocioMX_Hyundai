package com.example.negociomx_hyundai

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.Cliente
import com.example.negociomx_hyundai.BE.Empleado
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.BE.VehiculoPlacas
import com.example.negociomx_hyundai.BE.VehiculoPlacasDueno
import com.example.negociomx_hyundai.DAL.DALCliente
import com.example.negociomx_hyundai.DAL.DALEmpleadoSQL
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
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
    var transportistaSeleccionado :Cliente?=null

    private var transportistas = listOf<Cliente>()
    private var vehiculoActual: VehiculoPasoLog? = null
    private var statusActual: PasoLogVehiculoDet? = null
    private val dalCliente = DALCliente()

    private lateinit var adapterTransportistasRodando: ArrayAdapter<String>
    private lateinit var adapterTransportistasMadrina: ArrayAdapter<String>
    private lateinit var adapterConductores: ArrayAdapter<String>
    private lateinit var adapterPlacas: ArrayAdapter<String>

    var dalVeh:DALVehiculo?=null
    // Variables para hora dinámica
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable
    var fechaActual:String=""
    var IdVehiculo:Int?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPasoSalidaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dalVeh=DALVehiculo()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if(intent?.extras!=null)
        {
            val idPasoLogVehiculo= intent.extras?.getInt("IdPasoLogVehiculo",0)?:0
            val annioCad=intent.extras?.getString("Annio","")
            var annio:Short=0
            if(annioCad.toString().isNotEmpty())
                annio=annioCad.toString().toShort()
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
                Anio = annio.toInt()
            )
        }

        configurarEventos()
        inicializarFormulario()
    }

    private fun inicializarFormulario() {
        // Inicializar empleado receptor
        binding.tvEmpleadoReceptorSalida.text = "Empleado Responsable: ${ParametrosSistema.usuarioLogueado.NombreCompleto}"

        // Inicializar hora dinámica
        inicializarHoraDinamica()
        // Cargar transportistas
        cargarTransportistas()

        // Cargar personal
        cargaConductores()

        if(vehiculoActual!=null && vehiculoActual?.Id?.toInt()!!>0) {
            binding.tvVinVehiculoSalida.setText(vehiculoActual?.VIN)
        }
        mostrarInformacionVehiculo(vehiculoActual!!)
        mostrarFormularioStatusSalida()
    }

    // MÉTODOS PARA CARGAR SPINNERS
    private fun cargarTransportistas() {
        lifecycleScope.launch {
            try {
                transportistas = dalCliente.consultarTransportistasConPlacasNumEco(true)

                val nombresTransportistas = mutableListOf("Seleccionar empresa...")
                nombresTransportistas.addAll(transportistas.map { it.Nombre ?: "Sin nombre" })

                // Configurar adapter para Rodando
                adapterTransportistasRodando = ArrayAdapter(
                    this@PasoSalida_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresTransportistas
                )
                adapterTransportistasRodando.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerEmpresaRodandoSalida.adapter = adapterTransportistasRodando

                // Configurar adapter para Madrina
                adapterTransportistasMadrina = ArrayAdapter(
                    this@PasoSalida_Activity,
                    android.R.layout.simple_spinner_item,
                    nombresTransportistas
                )
                adapterTransportistasMadrina.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerEmpresaMadrinaSalida.adapter = adapterTransportistasMadrina

            } catch (e: Exception) {
                Log.e("Paso1Entrada", "Error cargando transportistas: ${e.message}")
                Toast.makeText(this@PasoSalida_Activity, "Error cargando empresas", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun mostrarFormularioStatusSalida() {
        binding.layoutFormularioSalida.visibility = View.VISIBLE
        binding.layoutErrorSalida.visibility = View.GONE
        Toast.makeText(this, "✅ Vehículo válido para status->Salida", Toast.LENGTH_SHORT).show()
    }

    // MÉTODOS PARA CARGAR PERSONAL
    private fun cargaConductores() {
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
                binding.spinnerConductorSalida.adapter = adapter

            } catch (e: Exception) {
                Log.e("PasoSalida", "Error cargando empleado: ${e.message}")
                Toast.makeText(this@PasoSalida_Activity, "Error cargando empleado", Toast.LENGTH_SHORT).show()
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
        // Botón guardar Guardar status->Salida
        binding.btnGuardarSalida.setOnClickListener {
            guardarStatusSalida()
        }

        binding.btnRegresarSalida.setOnClickListener {
            finish()
        }

        binding.rgTipoSalida.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbRodandoSalida -> {
                    binding.layoutRodandoSalida.visibility = View.VISIBLE
                    binding.layoutEnMadrinaSalida.visibility = View.GONE
                }
                R.id.rbEnMadrinaSalida -> {
                    binding.layoutRodandoSalida.visibility = View.GONE
                    binding.layoutEnMadrinaSalida.visibility = View.VISIBLE
                }
            }
        }

        configurarEventosSpinners()
    }

    private fun configurarEventosSpinners() {
        // Evento para spinner de empresa en Madrina
        binding.spinnerEmpresaMadrinaSalida.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // No es "Seleccionar empresa..."
                    transportistaSeleccionado = transportistas[position - 1]
                    cargarConductores(transportistaSeleccionado?.IdCliente!!)
                    cargarPlacasYNumeros(transportistaSeleccionado?.IdCliente!!,null)
                } else {
                    transportistaSeleccionado=null
                    limpiarSpinnerConductores()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerPlacaTransporteSalida.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(transportistaSeleccionado==null)
                {
                    limpiarSpinnerPlacas()
                }
                else
                {
                    val placasTransportista=transportistaSeleccionado?.Placas
                    if((placasTransportista==null && position==1) ||
                        (placasTransportista!=null && position==(placasTransportista?.count()!!+1))) {
                        val dialog= Dialog(this@PasoSalida_Activity)
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                        dialog.setContentView(R.layout.item_placa_numeco)

                        val btnGuardar=dialog.findViewById<ImageView>(R.id.btnGuardarItemPlacaEco)
                        val btnRegresar=dialog.findViewById<ImageView>(R.id.btnRegresarItemPlacaEco)
                        val txtPlaca = dialog.findViewById<EditText>(R.id.txtItemPlaca)
                        val txtNumEco = dialog.findViewById<EditText>(R.id.txtItemNumeroEconomico)

                        // Configurar diálogo
                        btnGuardar.setOnClickListener {
                            var cadena:String=""
                            var numeroEconomico=txtNumEco.text.toString()
                            var placas=txtPlaca.text.toString()

                            var posicion=binding.spinnerEmpresaMadrinaSalida.selectedItemPosition
                            var transportista= transportistas[posicion-1]

                            var vp=VehiculoPlacas(IdVehiculo=vehiculoActual?.Id!!.toInt(),
                                IdVehiculoPlacas = 0,
                                NumeroEconomico = numeroEconomico,
                                Placas = placas,
                                Annio = vehiculoActual?.Anio!!.toShort(),
                                Activo = true,
                                IdPersona = transportista.IdCliente!!
                                )

                            btnRegresar.isEnabled=false
                            btnGuardar.isEnabled=false

                            lifecycleScope.launch {
                                val respuesta = dalVeh?.insertarVehiculoPlacas(vp)
                                if (respuesta == null) {
                                    Toast.makeText(
                                        this@PasoSalida_Activity,
                                        "Ocurrio un error en el Sistema",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    dialog.dismiss()
                                } else if (respuesta?.TieneError == true) {
                                    Log.e("Paso1Entrada", respuesta.Mensaje)
                                    Toast.makeText(
                                        this@PasoSalida_Activity, "${respuesta?.Mensaje}",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    if (transportistaSeleccionado != null && transportistaSeleccionado?.Placas != null) {
                                        var posicion = -1
                                        var contador = 0
                                        transportistaSeleccionado?.Placas!!.forEach {
                                            if (it.Placas.equals(placas))
                                                posicion = contador
                                            contador++
                                        }
                                        if (posicion >= 0)
                                            binding.spinnerPlacaTransporteSalida.setSelection(posicion + 1)
                                        else
                                            binding.spinnerPlacaTransporteSalida.setSelection(0)
                                        binding.etNumeroEconomico.setText(numeroEconomico)
                                    }

                                    dialog.dismiss()
                                }
                                else {

                                    val valor = respuesta?.Data?.split("|")
                                    var idVehiculoPlacas = 0
                                    var idVehiculoPlacasDueno = 0
                                    if (valor != null) {
                                        idVehiculoPlacas = valor[0].toInt()
                                        idVehiculoPlacasDueno = valor[1].toInt()
                                    }
                                    cadena = id.toString()

                                    btnRegresar.isEnabled = true
                                    btnGuardar.isEnabled = true

                                    if (id!! > 0) {
                                        if (transportistaSeleccionado?.Placas == null) transportista.Placas =
                                            mutableListOf()

                                        val vpd = VehiculoPlacasDueno(
                                            IdVehiculoPlacas = idVehiculoPlacas,
                                            IdVehiculoPlacasDueno = idVehiculoPlacasDueno,
                                            IdPersona = transportista.IdCliente!!,
                                            Placas = placas,
                                            NumeroEconomico = numeroEconomico,
                                            IdTipoPersonaDueno = 1
                                        )
                                        transportistaSeleccionado?.Placas!!.add(vpd)

                                        dialog.dismiss()

                                        var posicionNueva =0
                                        var contador=0
                                        var lista=transportista.Placas?.sortedBy {a -> a.Placas } as MutableList<VehiculoPlacasDueno>
                                        transportistaSeleccionado?.Placas= mutableListOf()
                                        lista.forEach {
                                            transportistaSeleccionado?.Placas!!.add(it)
                                            if(it.Placas.equals(placas))
                                                posicionNueva=contador
                                            contador++
                                        }
                                        binding.etNumeroEconomico.setText(numeroEconomico)

                                        cargarPlacasYNumeros(transportista.IdCliente!!,null)

                                        binding.spinnerPlacaTransporteSalida.setSelection(posicionNueva+1)
                                    }
                                }
                            }
                        }
                        btnRegresar.setOnClickListener {
                            if(position==binding.spinnerPlacaTransporteSalida.count-1)
                                binding.spinnerPlacaTransporteSalida.setSelection(0)
                            else
                                binding.spinnerPlacaTransporteSalida.setSelection(position)
                            dialog.dismiss()
                        }

                        dialog.show()
                        txtPlaca.requestFocus()

                        // Ajustar tamaño del diálogo
                        val window = dialog.window
                        window?.setLayout(
                            (resources.displayMetrics.widthPixels * 0.9).toInt(),
                            (resources.displayMetrics.heightPixels * 0.7).toInt()
                        )

                    }
                    else
                    {
                        if(position>0) {
                            val placa = placasTransportista!![position-1]
                            if (placa != null && placa.NumeroEconomico.isNotEmpty()) {
                                binding.etNumeroEconomico.setText(placa.NumeroEconomico)
                                binding.etNumeroEconomico.requestFocus()
                            } else {
                                binding.etNumeroEconomico.setText("")
                            }
                        }
                        else
                            binding.etNumeroEconomico.setText("")
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun limpiarSpinnerConductores() {
        val conductoresVacio = listOf("Seleccionar conductor...")
        adapterConductores = ArrayAdapter(
            this@PasoSalida_Activity,
            android.R.layout.simple_spinner_item,
            conductoresVacio
        )
        adapterConductores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerConductorSalida.adapter = adapterConductores
    }

    private fun limpiarSpinnerPlacas() {
        val placaVacia = listOf("Seleccionar...")
        adapterPlacas = ArrayAdapter(
            this@PasoSalida_Activity,
            android.R.layout.simple_spinner_item,
            placaVacia
        )
        adapterPlacas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlacaTransporteSalida.adapter = adapterPlacas
    }

    private fun cargarConductores(idCliente: Int) {
        try {
            var empleadosTransportista= transportistaSeleccionado?.Empleados
            val nombresConductores = mutableListOf("Seleccionar conductor...")
            nombresConductores.addAll(empleadosTransportista!!.map { it.NombreCompleto ?: "Sin nombre" })

            adapterConductores = ArrayAdapter(
                this@PasoSalida_Activity,
                android.R.layout.simple_spinner_item,
                nombresConductores
            )
            adapterConductores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerConductorSalida.adapter = adapterConductores

        } catch (e: Exception) {
            Log.e("Paso1Entrada", "Error cargando conductores: ${e.message}")
            Toast.makeText(this@PasoSalida_Activity, "Error cargando conductores", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarPlacasYNumeros(idCliente: Int,posicionSeleccionada:Int?) {
        try {
            val transporte= transportistas.filter {it.IdCliente==idCliente }.firstOrNull()
            var placasTransportista:MutableList<VehiculoPlacasDueno>?=null
            if(transporte!=null && transporte.Placas!=null) placasTransportista=transporte?.Placas!!

            var placas = mutableListOf("Seleccionar...")
            if(placasTransportista!=null) {
                placasTransportista?.forEach {
                    if(it.IdVehiculoPlacasDueno>0)
                        placas.add(it.Placas)
                }
            }
            placas.add("Nueva Placa")
            adapterPlacas = ArrayAdapter(
                this@PasoSalida_Activity,
                android.R.layout.simple_spinner_item,
                placas
            )
            adapterPlacas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPlacaTransporteSalida.adapter = adapterPlacas
        } catch (e: Exception) {
            Log.e("Paso1Entrada", "Error cargando conductores: ${e.message}")
            Toast.makeText(this@PasoSalida_Activity, "Error cargando conductores", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarStatusSalida() {
        if (!validarFormularioStatusSalida()) {
            return
        }

        mostrarCargaGuardado()
        lifecycleScope.launch {
            try {
                Log.d("Status->Salida de Vehiculo", "💾 Guardando Status->Salida del vehículo")

                val posicionTransporte=binding.spinnerEmpresaMadrinaSalida.selectedItemPosition
                val transporte= transportistas[posicionTransporte-1]
                val idTransporteSalida=transporte.IdCliente

                val posicionEmpleado=binding.spinnerConductorSalida.selectedItemPosition
                val empleado= empleados[posicionEmpleado-1]
                val idEmpleadoTransporteSalida=empleado.IdEmpleado

                val posicionPlaca=binding.spinnerPlacaTransporteSalida.selectedItemPosition
                val placa= transportistaSeleccionado?.Placas!![posicionPlaca-1]
                val idVehiculoPlacas=placa.IdVehiculoPlacas
                var placas=placa.Placas

                val idBloque:Short? =  null
                val fila:Short? =  null
                val columna:Short? =  null

                var idUsuario=ParametrosSistema.usuarioLogueado.Id?.toInt()

                val paso=PasoLogVehiculoDet(
                    IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo,
                    IdEmpleadoTransporte =idEmpleadoTransporteSalida,
                    IdEmpleadoPosiciono = null,
                    Fila = fila,
                    Columna = columna,
                    IdBloque = idBloque,
                    IdStatus = 169,
                    IdTransporte = idTransporteSalida,
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
                    Bloque = "",
                    Placa = placas,
                    PersonaQueHaraMovimiento = "",
                    IdVehiculo = IdVehiculo,
                    IdVehiculoPlacas = idVehiculoPlacas,
                )
                val exito = dalPasoLog.insertaStatusNuevoPasoLogVehiculo(paso)
                ocultarCargaGuardado()

                if (exito) {
                    Toast.makeText(this@PasoSalida_Activity, "✅ Vehículo con Status->Salida exitosamente", Toast.LENGTH_SHORT).show()

                    val intentA = Intent(this@PasoSalida_Activity, Paso1Entrada_Activity::class.java) //me dio erro y tuve que agregar r
                    intentA.putExtra("RefrescarVin",true)
                    intentA.putExtra("Vin",vehiculoActual?.VIN)
                    startActivity(intentA)
                    finish() // Cerrar la actividad actual
                } else {
                    Toast.makeText(this@PasoSalida_Activity, "❌ Error guardando status -> Salida", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                ocultarCargaGuardado()
                Log.e("PasoSalida", "💥 Error guardando status->Salida: ${e.message}")
                Toast.makeText(this@PasoSalida_Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun ocultarCargaGuardado() {
        binding.loadingContainerSalida.visibility = View.GONE
        binding.btnGuardarSalida.isEnabled = true
        binding.btnGuardarSalida.alpha = 1.0f
    }

    private fun validarFormularioStatusSalida(): Boolean {
        if (binding.spinnerEmpresaMadrinaSalida.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el el transporte que se llevara el Vehiculo", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (binding.spinnerConductorSalida.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el conductor que manejará el Transporte", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (binding.spinnerPlacaTransporteSalida.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione las placas del Transporte", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (binding.etNumeroEconomico.text.isEmpty()) {
            Toast.makeText(this, "Suministre el numero economico del Transporte", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun mostrarCargaGuardado() {
        binding.loadingContainerSalida.visibility = View.VISIBLE
        binding.btnGuardarSalida.isEnabled = false
        binding.btnGuardarSalida.alpha = 0.5f

        binding.tvLoadingTextSalida.text = "Guardando status->Salida..."
       binding.tvLoadingSubtextSalida.text = "Actualizando status del vehículo"
    }

    private fun limpiarFormulario() {
        binding.spinnerConductorSalida.setSelection(0)

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
                fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                var fechaActualAux = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                binding.tvFechaMovimientoSalida.text = "Fecha de movimiento: $fechaActualAux"
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