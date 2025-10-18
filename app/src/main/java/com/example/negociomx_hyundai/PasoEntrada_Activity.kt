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
import com.example.negociomx_hyundai.BE.CfgGlo
import com.example.negociomx_hyundai.BE.Cliente
import com.example.negociomx_hyundai.BE.ClienteEmpleado
import com.example.negociomx_hyundai.BE.Empleado
import com.example.negociomx_hyundai.BE.PasoLogVehiculoDet
import com.example.negociomx_hyundai.BE.PasoLogVehiculoPDI
import com.example.negociomx_hyundai.BE.VehiculoPasoLog
import com.example.negociomx_hyundai.BE.VehiculoPlacas
import com.example.negociomx_hyundai.BE.VehiculoPlacasDueno
import com.example.negociomx_hyundai.BLL.BLLEmpleado
import com.example.negociomx_hyundai.DAL.DALCliente
import com.example.negociomx_hyundai.DAL.DALEmpleadoSQL
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
import com.example.negociomx_hyundai.Utils.ApiUploadUtil
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityPasoEntradaBinding
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PasoEntrada_Activity : AppCompatActivity() {
    private lateinit var binding: ActivityPasoEntradaBinding
    private val dalPasoLog = DALPasoLogVehiculo()
    private val dalCliente = DALCliente()
    private val dalEmp= DALEmpleadoSQL()
    private var transportistas = listOf<Cliente>()
    private var empleados = listOf<Empleado>()
    private var empleadosTransportista = listOf<ClienteEmpleado>()
    var transportistaSeleccionado :Cliente?=null
    private var statusActual: PasoLogVehiculoDet? = null
    private var vehiculoActual: VehiculoPasoLog? = null
    var dalVeh:DALVehiculo?=null
    private lateinit var timerHandler: Handler
    private lateinit var timerRunnable: Runnable

    var fechaActual: String = ""
    private lateinit var adapterTransportistasRodando: ArrayAdapter<String>
    private lateinit var adapterTransportistasMadrina: ArrayAdapter<String>
    private lateinit var adapterConductores: ArrayAdapter<String>
    private lateinit var adapterPlacas: ArrayAdapter<String>
    private var fotosInspeccion: MutableList<PasoLogVehiculoPDI>? = null
    val gson = Gson()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPasoEntradaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dalVeh=DALVehiculo()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainEntrada)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
       /* if (ParametrosSistema.CfgGloSql == null) {
            ParametrosSistema.CfgGloSql = CfgGlo().apply {
                urlGuardadoArchivos = "https://softsystemmx.com/api/Upload/UploadFile"
                FormatoCarpetaArchivos = "~/imgs"
                ManejaGuardadoArchivosEnBD = false
            }
            Log.d("Paso1Entrada", "CfgGloSql inicializado")
            Log.d("Paso1Entrada", "  URL: ${ParametrosSistema.CfgGloSql?.urlGuardadoArchivos}")
            Log.d("Paso1Entrada", "  Carpeta: ${ParametrosSistema.CfgGloSql?.FormatoCarpetaArchivos}")
        }*/
        inicializarComponentes()
        configurarEventos()
        cargarDatosIniciales()

    }

    private fun inicializarComponentes() {
        binding.tvEmpleadoReceptorEntrada.text = "Empleado Responsable: " + "${ParametrosSistema.usuarioLogueado.NombreCompleto}"
        inicializarHoraDinamica()
    }
    private fun cargarDatosIniciales() {
        mostrarCarga("Cargando datos iniciales...")

        lifecycleScope.launch {
            try {
                obtenerDatosVehiculo()

                // Cargar transportistas
                transportistas = dalCliente.consultarTransportistasConPlacasNumEco(true)
                cargarTransportistas()

                // Cargar personal
                empleados = dalEmp.consultarEmpleados(105,106)
                cargaConductores()

                ocultarCarga()
                mostrarFormularios()

                Log.d("PasoEntrada_Activity", "✅ Datos iniciales cargados correctamente")

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error cargando datos: ${e.message}")
                Log.e("PasoEntrada_Activity", "Error cargando datos: ${e.message}")

            }
        }
    }
    private fun configurarEventos() {
        // Botón guardar Guardar status->Salida
        binding.btnGuardarEntrada.setOnClickListener {
            guardarStatusSalida()
        }

        binding.btnRegresarEntrada.setOnClickListener {
            finish()
        }

        binding.rgTipoEntrada.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbRodandoEntrada -> {
                    binding.layoutRodandoEntrada.visibility = View.VISIBLE
                    binding.layoutEnMadrinaEntrada.visibility = View.GONE
                }
                R.id.rbEnMadrinaEntrada -> {
                    binding.layoutRodandoEntrada.visibility = View.GONE
                    binding.layoutEnMadrinaEntrada.visibility = View.VISIBLE
                }
            }
        }

        binding.btnInspeccionar.setOnClickListener{
            val codigoRespuesta=101
            val jsonVeh=gson.toJson(vehiculoActual)
            val intent = Intent(this, PasoInspeccionar_Activity::class.java)
            intent.putExtra("vehiculo", jsonVeh)
            startActivityForResult(intent,codigoRespuesta)
        }

        configurarEventosSpinners()
    }


    private fun obtenerDatosVehiculo() {
        try {
            val jsonVeh = intent.getStringExtra("vehiculo") ?: ""
            if (jsonVeh.isNotEmpty()) {
                vehiculoActual = gson.fromJson(jsonVeh,VehiculoPasoLog::class.java)
                mostrarInfoVehiculo()
                Log.d("PasoTaller_Activity", "✅ Datos del vehículo obtenidos: " +
                        "VIN=${vehiculoActual!!.VIN}")
            } else {
                mostrarError("No se recibieron datos válidos del vehículo")
            }
        } catch (e: Exception) {
            mostrarError("Error obteniendo datos del vehículo: ${e.message}")
            Log.e("PasoEntrada_Activity", "Error obteniendo datos: ${e.message}")
        }
    }

    private fun mostrarInfoVehiculo() {
        vehiculoActual?.let { vehiculo ->
            binding.tvVinVehiculoEntrada.text = "VIN: ${vehiculo.VIN}"
            binding.tvBlVehiculoEntrada.text = "BL: ${vehiculo.BL}"
            binding.tvMarcaModeloAnnioEntrada.text = "${vehiculo.Especificaciones}  Año: ${vehiculo.Anio}"
            binding.tvColorExteriorEntrada.text = "Color Ext: ${vehiculo.ColorExterior}"
            binding.tvColorInteriorVehiculoEntrada.text = "Color Int: ${vehiculo.ColorInterior}"
            binding.layoutInfoVehiculoEntrada.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularios() {
        binding.apply {
            binding.apply {
                layoutFormularioEntrada.visibility = View.VISIBLE
                layoutErrorEntrada.visibility = View.GONE
            }

            Toast.makeText(this@PasoEntrada_Activity, "✅ Vehículo válido para status->Entrada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inicializarFormulario() {
        // Inicializar empleado receptor
        binding.tvEmpleadoReceptorEntrada.text = "Empleado Responsable: " + "${ParametrosSistema.usuarioLogueado.NombreCompleto}"

        // Inicializar hora dinámica
        inicializarHoraDinamica()
        // Cargar transportistas
        cargarTransportistas()

        // Cargar personal
        cargaConductores()

        if(vehiculoActual!=null && vehiculoActual?.Id?.toInt()!!>0) {
            binding.tvVinVehiculoEntrada.setText(vehiculoActual?.VIN)
        }
        mostrarInformacionVehiculo(vehiculoActual!!)
        mostrarFormularioStatusSalida()
    }

    private fun cargarTransportistas() {
        //  lifecycleScope.launch {
        try {

            val nombresTransportistas = mutableListOf("Seleccionar empresa...")
            nombresTransportistas.addAll(transportistas.map { it.Nombre ?: "Sin nombre" })

            // Configurar adapter para Rodando
            adapterTransportistasRodando = ArrayAdapter(
                this@PasoEntrada_Activity,
                android.R.layout.simple_spinner_item,
                nombresTransportistas
            )
            adapterTransportistasRodando.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerEmpresaRodandoEntrada.adapter = adapterTransportistasRodando

            // Configurar adapter para Madrina
            adapterTransportistasMadrina = ArrayAdapter(
                this@PasoEntrada_Activity,
                android.R.layout.simple_spinner_item,
                nombresTransportistas
            )
            adapterTransportistasMadrina.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerEmpresaMadrinaEntrada.adapter = adapterTransportistasMadrina

        } catch (e: Exception) {
            Log.e("Paso1Entrada", "Error cargando transportistas: ${e.message}")
            Toast.makeText(this, "Error cargando empresas", Toast.LENGTH_SHORT).show()
        }
        // }
    }

    private fun mostrarInformacionVehiculo(vehiculo: VehiculoPasoLog) {
        binding.apply {
            tvVinVehiculoEntrada.text = "VIN: ${vehiculo.VIN}"
            tvBlVehiculoEntrada.text = "MBL: ${vehiculo.BL}"
            tvMarcaModeloAnnioEntrada.text = "${vehiculo.Especificaciones}, Año: ${vehiculo.Anio}"
            tvColorExteriorEntrada.text = "Color Ext.: ${vehiculo.ColorExterior}"
            tvColorInteriorVehiculoEntrada.text = "Color Int.: ${vehiculo.ColorInterior}"
            layoutInfoVehiculoEntrada.visibility = View.VISIBLE
        }
    }

    private fun mostrarFormularioStatusSalida() {
        binding.apply {
            layoutFormularioEntrada.visibility = View.VISIBLE
            layoutErrorEntrada.visibility = View.GONE
        }

        Toast.makeText(this, "✅ Vehículo válido para status->Salida", Toast.LENGTH_SHORT).show()
    }

    // MÉTODOS PARA CARGAR PERSONAL
    private fun cargaConductores() {
        //lifecycleScope.launch {
        try {

            val nombresPersonal = mutableListOf("Seleccionar personal...")
            nombresPersonal.addAll(empleados.map { it.NombreCompleto ?: "Sin nombre" })

            val adapter = ArrayAdapter(
                this@PasoEntrada_Activity,
                android.R.layout.simple_spinner_item,
                nombresPersonal
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerConductorEntrada.adapter = adapter

        } catch (e: Exception) {
            Log.e("PasoSalida", "Error cargando empleado: ${e.message}")
            Toast.makeText(this@PasoEntrada_Activity, "Error cargando empleado", Toast.LENGTH_SHORT).show()
        }
        // }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }
    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // <CHANGE> Manejar resultado de PasoInspeccionar_Activity
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            val jsonFotos = data?.getStringExtra("fotos") ?: ""
            val fotosCapturadas = data?.getIntExtra("fotosCapturadas", 0) ?: 0

            if (jsonFotos.isNotEmpty()) {
                val listType = object : com.google.gson.reflect.TypeToken<MutableList<PasoLogVehiculoPDI>>() {}.type
                fotosInspeccion = gson.fromJson(jsonFotos, listType)

                // Actualizar etiqueta de fotos capturadas
                binding.tvFotosCapturadas.text = "Fotos de inspección: $fotosCapturadas/5"
                binding.tvFotosCapturadas.visibility = View.VISIBLE

                Log.d("PasoEntrada", "✅ Fotos recibidas: $fotosCapturadas/5")
            }
        }
    }

    private fun configurarEventosSpinners() {
        // Evento para spinner de empresa en Madrina
        binding.spinnerEmpresaMadrinaEntrada.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

        binding.spinnerPlacaTransporteEntrada.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
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
                        val dialog= Dialog(this@PasoEntrada_Activity)
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

                            var posicion=binding.spinnerEmpresaMadrinaEntrada.selectedItemPosition
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
                                        this@PasoEntrada_Activity,
                                        "Ocurrio un error en el Sistema",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    dialog.dismiss()
                                } else if (respuesta?.TieneError == true) {
                                    Log.e("Paso1Entrada", respuesta.Mensaje)
                                    Toast.makeText(
                                        this@PasoEntrada_Activity, "${respuesta?.Mensaje}",
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
                                            binding.spinnerPlacaTransporteEntrada.setSelection(posicion + 1)
                                        else
                                            binding.spinnerPlacaTransporteEntrada.setSelection(0)
                                        binding.etNumeroEconomicoEntrada.setText(numeroEconomico)
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
                                        binding.etNumeroEconomicoEntrada.setText(numeroEconomico)

                                        cargarPlacasYNumeros(transportista.IdCliente!!,null)

                                        binding.spinnerPlacaTransporteEntrada.setSelection(posicionNueva+1)
                                    }
                                }
                            }
                        }
                        btnRegresar.setOnClickListener {
                            if(position==binding.spinnerPlacaTransporteEntrada.count-1)
                                binding.spinnerPlacaTransporteEntrada.setSelection(0)
                            else
                                binding.spinnerPlacaTransporteEntrada.setSelection(position)
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
                                binding.etNumeroEconomicoEntrada.setText(placa.NumeroEconomico)
                                binding.etNumeroEconomicoEntrada.requestFocus()
                            } else {
                                binding.etNumeroEconomicoEntrada.setText("")
                            }
                        }
                        else
                            binding.etNumeroEconomicoEntrada.setText("")
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun limpiarSpinnerConductores() {
        val conductoresVacio = listOf("Seleccionar conductor...")
        adapterConductores = ArrayAdapter(
            this@PasoEntrada_Activity,
            android.R.layout.simple_spinner_item,
            conductoresVacio
        )
        adapterConductores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerConductorEntrada.adapter = adapterConductores
    }

    private fun limpiarSpinnerPlacas() {
        val placaVacia = listOf("Seleccionar...")
        adapterPlacas = ArrayAdapter(
            this@PasoEntrada_Activity,
            android.R.layout.simple_spinner_item,
            placaVacia
        )
        adapterPlacas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlacaTransporteEntrada.adapter = adapterPlacas
    }

    private fun cargarConductores(idCliente: Int) {
        try {
            var empleadosTransportista= transportistaSeleccionado?.Empleados
            val nombresConductores = mutableListOf("Seleccionar conductor...")
            nombresConductores.addAll(empleadosTransportista!!.map { it.NombreCompleto ?: "Sin nombre" })

            adapterConductores = ArrayAdapter(
                this@PasoEntrada_Activity,
                android.R.layout.simple_spinner_item,
                nombresConductores
            )
            adapterConductores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerConductorEntrada.adapter = adapterConductores

        } catch (e: Exception) {
            Log.e("Paso1Entrada", "Error cargando conductores: ${e.message}")
            Toast.makeText(this@PasoEntrada_Activity, "Error cargando conductores", Toast.LENGTH_SHORT).show()
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
                this@PasoEntrada_Activity,
                android.R.layout.simple_spinner_item,
                placas
            )
            adapterPlacas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPlacaTransporteEntrada.adapter = adapterPlacas
        } catch (e: Exception) {
            Log.e("Paso1Entrada", "Error cargando conductores: ${e.message}")
            Toast.makeText(this@PasoEntrada_Activity, "Error cargando conductores", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarStatusSalida() {
        if (!validarFormularioStatusSalida()) {
            return
        }

        mostrarCargaGuardado()
        lifecycleScope.launch {
            try {
                Log.d("Status->Entrada de Vehiculo", "💾 Guardando Status->Entrada del vehículo")

                val posicionTransporte=binding.spinnerEmpresaMadrinaEntrada.selectedItemPosition
                val transporte= transportistas[posicionTransporte-1]
                val idTransporteSalida=transporte.IdCliente

                val posicionEmpleado=binding.spinnerConductorEntrada.selectedItemPosition
                val empleado= empleados[posicionEmpleado-1]
                val idEmpleadoTransporteSalida=empleado.IdEmpleado

                val posicionPlaca=binding.spinnerPlacaTransporteEntrada.selectedItemPosition
                val placa= transportistaSeleccionado?.Placas!![posicionPlaca-1]
                val idVehiculoPlacas=placa.IdVehiculoPlacas
                var placas=placa.Placas
                var tipoEntradaSalida = 1
                if(binding.rbEnMadrinaEntrada.isSelected) tipoEntradaSalida=2
                val idBloque:Short? =  null
                val fila:Short? =  null
                val columna:Short? =  null
                var numeroEconomico = binding.etNumeroEconomicoEntrada.text.toString().trim()
                var idUsuario=ParametrosSistema.usuarioLogueado.Id?.toInt()

                val paso=PasoLogVehiculoDet(
                    IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo,
                    IdEmpleadoTransporte =idEmpleadoTransporteSalida,
                    IdEmpleadoPosiciono = null,
                    Fila = fila,
                    Columna = columna,
                    IdBloque = idBloque,
                    IdStatus = 168,
                    IdTransporte = idTransporteSalida,
                    IdTipoMovimiento = null,
                    IdUsuarioMovimiento = idUsuario,
                    IdPasoLogVehiculoDet = 0,
                    IdParteDanno = null,
                    IdTipoEntradaSalida =tipoEntradaSalida,
                    EnviadoAInterface = null,
                    FechaEnviado = null,
                    Observacion = null,
                    FechaMovimiento = fechaActual,
                    NumeroEconomico = numeroEconomico,
                    Bloque = null,
                    Placa = placas,
                    PersonaQueHaraMovimiento = null,
                    IdVehiculo = vehiculoActual!!.Id.toInt(),
                    IdVehiculoPlacas = idVehiculoPlacas,
                )
                val exito = dalPasoLog.insertaStatusNuevoPasoLogVehiculo(paso)

                ocultarCargaGuardado()


                if (exito) {
                    Log.d("PasoEntrada", "✅ Status guardado, procediendo a guardar fotos...")

                    // <CHANGE> Guardar fotos y esperar resultado
                    val exitoFotos = guardarFotosInspeccion()

                    Log.d("PasoEntrada", "🔍 Resultado guardado de fotos: $exitoFotos")

                    if (exitoFotos) {
                        Toast.makeText(
                            this@PasoEntrada_Activity,
                            "✅ Vehículo con Status->Entrada y fotos guardadas exitosamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else {
                        Toast.makeText(
                            this@PasoEntrada_Activity,
                            "⚠️ Vehículo guardado pero error guardando fotos",
                            Toast.LENGTH_LONG
                        ).show()
                    }


                    Toast.makeText(this@PasoEntrada_Activity, "✅ Vehículo con Status->Entrada exitosamente", Toast.LENGTH_SHORT).show()

                    val data = Intent()
                    data.putExtra("Refrescar", true);
                    data.putExtra("Vin", vehiculoActual?.VIN);
                    setResult(Activity.RESULT_OK,data)
                    finish() // Cerrar la actividad actual
                } else {
                    Toast.makeText(this@PasoEntrada_Activity, "❌ Error guardando status -> Entrada", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                ocultarCargaGuardado()
                Log.e("PasoSalida", "💥 Error guardando status->Entrada: ${e.message}")
                Toast.makeText(this@PasoEntrada_Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun guardarFotosInspeccion(): Boolean {
        return try {
            if (ParametrosSistema.CfgGloSql?.ManejaGuardadoArchivosEnBD == true) {
                // Guardar en base de datos
                guardarFotosEnBD()
            } else {
                // Guardar en WebAPI
                guardarFotosEnWebAPI()
            }
        } catch (e: Exception) {
            Log.e("PasoEntrada", "Error guardando fotos: ${e.message}")
            false
        }
    }
    private suspend fun guardarFotosEnWebAPI(): Boolean = withContext(Dispatchers.IO) {
      /*  ParametrosSistema.CfgGloSql = CfgGlo().apply {
            urlGuardadoArchivos = "https://softsystemmx.com/api/Upload/UploadFile"
            FormatoCarpetaArchivos = "~/imgs"
            ManejaGuardadoArchivosEnBD = false
        }*/
        return@withContext try {
            Log.d("PasoEntrada", "🔍 Iniciando guardado de fotos en WebAPI")
            Log.d("PasoEntrada", "🔍 IdPasoLogVehiculo: ${vehiculoActual?.IdPasoLogVehiculo}")
            Log.d("PasoEntrada", "🔍 Cantidad de fotos: ${fotosInspeccion?.size}")

            // <CHANGE> Validar que exista IdPasoLogVehiculo
            if (vehiculoActual?.IdPasoLogVehiculo == null ) {
                Log.e("PasoEntrada", "❌ Error: IdPasoLogVehiculo no disponible")
                return@withContext false
            }

            if (fotosInspeccion == null || fotosInspeccion?.isEmpty() == true) {
                Log.d("PasoEntrada", "⚠️ No hay fotos para guardar")
                return@withContext true
            }

            val urlBase = ParametrosSistema.CfgGloSql!!.urlGuardadoArchivos + '/' +
                    ParametrosSistema.CfgGloSql!!.UrlAPIControllerGuardadoArchivos
            Log.d("PasoEntrada", "🔍 URL Base: $urlBase")
            Log.d("PasoEntrada", "🔍 Carpeta: ${ParametrosSistema.CfgGloSql?.FormatoCarpetaArchivos}")

            if (urlBase.isNullOrEmpty()) {
                Log.e("PasoEntrada", "❌ URL de guardado no configurada")
                return@withContext false
            }

            // Verificar conexión primero
            val conectado = ApiUploadUtil.verificarConexion(urlBase)
            if (!conectado) {
                Log.e("PasoEntrada", "No hay conexión con el servidor")
                return@withContext false
            }

            var exitoTotal = true
            val fotosSubidas = mutableListOf<PasoLogVehiculoPDI>()

            fotosInspeccion!!.forEachIndexed { index, foto ->
                try {
                    // Construir nombre del archivo según formato configurado
                    val nombreArchivo = construirNombreArchivo(foto, index + 1)

                    // Decodificar Base64 a bytes y crear archivo temporal
                    val bytes = android.util.Base64.decode(foto.FotoBase64, android.util.Base64.DEFAULT)
                    val archivoTemp = File(cacheDir, nombreArchivo)
                    archivoTemp.writeBytes(bytes)

                    // Subir archivo a la API
                    val (exito, mensaje) = ApiUploadUtil.subirFoto(
                        urlBase = urlBase,
                        nombreArchivo = nombreArchivo,
                        file = archivoTemp,
                        vin = vehiculoActual?.VIN ?: "",
                        paso = 1, // Paso de entrada
                        numeroFoto = index + 1
                    )

                    if (exito) {
                        Log.d("PasoEntrada", "Foto ${index + 1} subida: $mensaje")

                        // <CHANGE> Preparar metadatos para guardar en BD SIN el Base64
                        val fotoConMetadatos = foto.copy(
                            IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo ?: 0,
                            IdTipoEvidencia = 1,
                            Consecutivo = (index + 1).toShort(),
                            NombreFotoEvidencia = nombreArchivo,
                            IdTransporte = vehiculoActual?.IdTransporte ?: 0,
                            IdEmpleadoTransporte = null,
                            FotoBase64 = null  // <-- AGREGAR ESTA LÍNEA (limpiar Base64 porque ya está en API)
                        )
                        fotosSubidas.add(fotoConMetadatos)

                    } else {
                        Log.e("PasoEntrada", "Error subiendo foto ${index + 1}: $mensaje")
                        exitoTotal = false
                    }

                    // Eliminar archivo temporal
                    archivoTemp.delete()

                } catch (e: Exception) {
                    Log.e("PasoEntrada", "Error procesando foto ${index + 1}: ${e.message}")
                    exitoTotal = false
                }
            }

            // <CHANGE> Si todas las fotos se subieron, guardar metadatos en BD
            if (exitoTotal && fotosSubidas.isNotEmpty()) {
                val exitoBD = dalPasoLog.insertarFotosInspeccion(fotosSubidas)
                if (!exitoBD) {
                    Log.e("PasoEntrada", "Fotos subidas pero error guardando metadatos en BD")
                    exitoTotal = false
                }
            }

            if (exitoTotal) {
                Log.d("PasoEntrada", "Todas las fotos subidas y metadatos guardados exitosamente")
            } else {
                Log.e("PasoEntrada", "Algunas fotos no se pudieron procesar")
            }

            exitoTotal

        } catch (e: Exception) {
            Log.e("PasoEntrada", "Error en guardarFotosEnWebAPI: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    private suspend fun guardarFotosEnBD(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PasoEntrada", "Guardando fotos en sistema de archivos local y metadatos en BD")

            if (fotosInspeccion == null || fotosInspeccion?.isEmpty() == true) {
                return@withContext true
            }

            // <CHANGE> Primero guardar las fotos en el sistema de archivos local
            val fotosGuardadas = mutableListOf<PasoLogVehiculoPDI>()

            fotosInspeccion!!.forEachIndexed { index, foto ->
                try {
                    // Construir nombre del archivo
                    val nombreArchivo = construirNombreArchivo(foto, index + 1)

                    // Decodificar Base64 a bytes
                    val bytes = android.util.Base64.decode(foto.FotoBase64, android.util.Base64.DEFAULT)

                    // Crear carpeta según configuración
                    val carpetaBase = ParametrosSistema.CfgGloSql?.FormatoCarpetaArchivos ?: "InspeccionVehiculos"
                    val carpeta = File(getExternalFilesDir(null), carpetaBase)
                    if (!carpeta.exists()) {
                        carpeta.mkdirs()
                    }

                    // Guardar archivo
                    val archivo = File(carpeta, nombreArchivo)
                    archivo.writeBytes(bytes)

                    Log.d("PasoEntrada", "Foto guardada localmente: ${archivo.absolutePath}")

                    // Preparar objeto con metadatos para BD
                    val fotoConMetadatos = foto.copy(
                        IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo ?: 0,
                        IdTipoEvidencia = 1, // Inspección de entrada
                        Consecutivo = (index + 1).toShort(),
                        NombreFotoEvidencia = nombreArchivo,
                        IdTransporte = vehiculoActual?.IdTransporte ?: 0,
                        IdEmpleadoTransporte = null,
                        RutaArchivoLocal = archivo.absolutePath
                    )

                    fotosGuardadas.add(fotoConMetadatos)

                } catch (e: Exception) {
                    Log.e("PasoEntrada", "Error guardando foto ${index + 1}: ${e.message}")
                    return@withContext false
                }
            }

            // <CHANGE> Luego guardar los metadatos en la base de datos
            val exito = dalPasoLog.insertarFotosInspeccion(fotosGuardadas)

            if (exito) {
                Log.d("PasoEntrada", "Fotos y metadatos guardados exitosamente")
            } else {
                Log.e("PasoEntrada", "Error guardando metadatos en BD")
            }

            exito

        } catch (e: Exception) {
            Log.e("PasoEntrada", "Error en guardarFotosEnBD: ${e.message}")
            e.printStackTrace()
            false
        }
    }


    private fun construirNombreArchivo(foto: PasoLogVehiculoPDI, numeroFoto: Int): String {
        return try {
            val formato = ParametrosSistema.CfgGloSql?.FormatoCarpetaArchivos ?: ""

            if (formato.isNotEmpty()) {
                // Reemplazar variables en el formato
                // Ejemplo: "{VIN}_{PASO}_{NUMERO}.jpg" -> "ABC123_ENTRADA_1.jpg"
                formato
                    .replace("{VIN}", vehiculoActual?.VIN ?: "SINVIN")
                    .replace("{PASO}", "ENTRADA")
                    .replace("{NUMERO}", numeroFoto.toString())
                    .replace("{FECHA}", SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()))
                    .replace("{HORA}", SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date()))
            } else {
                // Formato por defecto
                "${vehiculoActual?.VIN}_ENTRADA_$numeroFoto.jpg"
            }
        } catch (e: Exception) {
            Log.e("PasoEntrada", "Error construyendo nombre de archivo: ${e.message}")
            "${vehiculoActual?.VIN}_ENTRADA_$numeroFoto.jpg"
        }
    }

    private fun construirNombreArchivo(foto: PasoLogVehiculoPDI): String {
        val formato = ParametrosSistema.CfgGloSql?.FormatoCarpetaArchivos ?: ""
        // Implementar lógica según el formato configurado
        return "${foto.NombreFotoEvidencia}.jpg"
    }

    private fun ocultarCargaGuardado() {
        binding.loadingContainerEntrada.visibility = View.GONE
        binding.btnGuardarEntrada.isEnabled = true
        binding.btnGuardarEntrada.alpha = 1.0f
    }

    private fun validarFormularioStatusSalida(): Boolean {
        if (fotosInspeccion == null || fotosInspeccion?.size != 5) {
            Toast.makeText(this, "Debe capturar las 5 fotos de inspección antes de guardar", Toast.LENGTH_LONG).show()
            return false
        }
        if (binding.spinnerEmpresaMadrinaEntrada.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el el transporte que se llevara el Vehiculo", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (binding.spinnerConductorEntrada.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el conductor que manejará el Transporte", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (binding.spinnerPlacaTransporteEntrada.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione las placas del Transporte", Toast.LENGTH_SHORT).show()
            return false
        }
        else if (binding.etNumeroEconomicoEntrada.text.isEmpty()) {
            Toast.makeText(this, "Suministre el numero economico del Transporte", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun mostrarCargaGuardado() {
        binding.apply {
            loadingContainerEntrada.visibility = View.VISIBLE
            btnGuardarEntrada.isEnabled = false
            btnGuardarEntrada.alpha = 0.5f
            tvLoadingTextEntrada.text = "Guardando status->Salida..."
            tvLoadingSubtextEntrada.text = "Actualizando status del vehículo"
        }

    }
    //Optimizado por miguel
    private fun mostrarError(mensaje: String) {
        binding.apply {
            tvMensajeErrorEntrada.text = mensaje
            layoutErrorEntrada.visibility = View.VISIBLE
        }

    }
    private fun mostrarCarga(mensaje: String) {
        binding.apply {
            tvLoadingTextEntrada.text = mensaje
            loadingContainerEntrada.visibility = View.VISIBLE
            btnGuardarEntrada.isEnabled = false
            btnGuardarEntrada.alpha = 0.5f
        }
    }

    //Optimizado
    private fun ocultarCarga() {
        binding.apply {
            loadingContainerEntrada.visibility = View.GONE
            btnGuardarEntrada.isEnabled = true
            btnGuardarEntrada.alpha = 1.0f
        }

    }

    private fun limpiarFormulario() {
        binding.spinnerConductorEntrada.setSelection(0)
        binding.layoutInfoVehiculoEntrada.visibility = View.GONE
        binding.layoutFormularioEntrada.visibility = View.GONE
        binding.layoutErrorEntrada.visibility = View.GONE
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
                binding.tvFechaMovimientoEntrada.text = "Fecha de movimiento: $fechaActualAux"
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