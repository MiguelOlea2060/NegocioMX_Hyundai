package com.example.negociomx_hyundai

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.*
import com.example.negociomx_hyundai.BLL.BLLEmpleado
import com.example.negociomx_hyundai.DAL.DALClienteSQL
import com.example.negociomx_hyundai.DAL.DALEmpleadoSQL
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALTaller
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityPasoTallerBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PasoTaller_Activity : AppCompatActivity() {
    private lateinit var binding:ActivityPasoTallerBinding
    // Variables de datos
    private var vehiculoActual: VehiculoPasoLog? = null
    private var empleados = listOf<Empleado>()
    private var empleadosConductores = listOf<Empleado>()
    private var empleadosTaller = listOf<Empleado>()

    private var empresasTaller = listOf<Cliente>()
    private var partesDanadas = listOf<ParteDanno>()
    private var parteSeleccionada: ParteDanno? = null
    var fechaActual:String=""
    private val dalPasoLog = DALPasoLogVehiculo()

    private var paginaActual = 0
    private val partesPorPagina = 2
    private var totalPaginas = 0
    private val dalTaller = DALTaller()
    private var dalEmpresaTaller=DALClienteSQL()
    private val dalEmpleado = DALEmpleadoSQL()
    var bllEmp= BLLEmpleado()
    val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPasoTallerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainTaller)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        inicializarComponentes()
        configurarEventos()
        cargarDatosIniciales()
    }

    private fun inicializarComponentes() {
        inicializarFechaActual()
    }

    private fun configurarEventos() {
        // Radio buttons para tipo de reparación
        binding.radioGroupTipoReparacionTaller.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioEnSitioTaller -> {
                    binding.layoutEmpresaExternaTaller.visibility = View.GONE
                    binding.layoutEmpresaInternaTaller.visibility = View.VISIBLE
                }
                R.id.radioForaneaTaller -> {
                    binding.layoutEmpresaExternaTaller.visibility = View.VISIBLE
                    binding.layoutEmpresaInternaTaller.visibility = View.GONE
                }
            }
        }

        binding.btnRegresarPasoTaller.setOnClickListener {
            finish()
        }

        // Selector de fecha
        binding.btnSeleccionarFechaTaller.setOnClickListener {
            mostrarSelectorFechaHora()
        }

        // Navegación de páginas
        binding.btnPaginaAnteriorTaller.setOnClickListener {
            if (paginaActual > 0) {
                paginaActual--
                mostrarPartesActuales()
                actualizarBotonesPaginacion()
            }
        }

        binding.btnPaginaSiguienteTaller.setOnClickListener {
            if (paginaActual < totalPaginas - 1) {
                paginaActual++
                mostrarPartesActuales()
                actualizarBotonesPaginacion()
            }
        }

        // Botones de acción
        binding.btnLimpiarTaller.setOnClickListener {
            limpiarFormulario()
        }

        binding.btnGuardarTaller.setOnClickListener {
            guardarRegistroTaller()
        }
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
            Log.e("PasoTaller_Activity", "Error obteniendo datos: ${e.message}")
        }
    }

    private fun mostrarInfoVehiculo() {
        vehiculoActual?.let { vehiculo ->
            binding.tvVinVehiculoTaller.text = "VIN: ${vehiculo.VIN}"
            binding.tvBlVehiculoTaller.text = "BL: ${vehiculo.BL}"
            binding.tvMarcaModeloAnnioTaller.text = "${vehiculo.Especificaciones}  Año: ${vehiculo.Anio}"
            binding.tvColorExteriorTaller.text = "Color Ext: ${vehiculo.ColorExterior}"
            binding.tvColorInteriorTaller.text = "Color Int: ${vehiculo.ColorInterior}"
            binding.layoutInfoVehiculoTaller.visibility = View.VISIBLE
        }
    }

    private fun cargarDatosIniciales() {
        mostrarCarga("Cargando datos iniciales...")

        lifecycleScope.launch {
            try {
                obtenerDatosVehiculo()

                // Cargar empleados
                empleados = dalEmpleado.consultarEmpleados(105,106)

                empleadosConductores=bllEmp.getEmpleadosPorTipo(105,empleados)
                empleadosTaller=bllEmp.getEmpleadosPorTipo(106,empleados)

                configurarSpinnerConductorYTaller()

                // Cargar empresas de taller
                empresasTaller = dalEmpresaTaller.consultarEmpresasTaller()
                configurarSpinnersEmpresas()

                // Cargar partes dañadas
                partesDanadas = dalTaller.consultarPartesDanno()
                configurarPartesDanadas()

                ocultarCarga()
                mostrarFormularios()

                Log.d("PasoTaller_Activity", "✅ Datos iniciales cargados correctamente")

            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error cargando datos: ${e.message}")
                Log.e("PasoTaller_Activity", "Error cargando datos: ${e.message}")
            }
        }
    }

   /* private fun configurarSpinnerConductorYTaller() {
        val nombresEmpleadosConductores = empleadosConductores.map { " ${it.NombreCompleto} " }
        val adapterCon = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresEmpleadosConductores)
        adapterCon.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPersonalMovimientoTaller.adapter = adapterCon

        val nombresEmpleadosTaller = empleadosTaller.map { " ${it.NombreCompleto} " }
        val adapterTall = ArrayAdapter(this, android.R.layout.simple_spinner_item,
            nombresEmpleadosTaller)
        adapterTall.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPersonaReparacionTaller.adapter = adapterTall
    }*/
   private fun configurarSpinnerConductorYTaller() {
       // <CHANGE> Agregar "Seleccionar empleado..." como primer elemento
       val nombresEmpleadosConductores = mutableListOf("Seleccionar empleado...")
       nombresEmpleadosConductores.addAll(empleadosConductores.map { " ${it.NombreCompleto} " })

       val adapterCon = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresEmpleadosConductores)
       adapterCon.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
       binding.spinnerPersonalMovimientoTaller.adapter = adapterCon

       // <CHANGE> Agregar "Seleccionar empleado..." como primer elemento
       val nombresEmpleadosTaller = mutableListOf("Seleccionar empleado...")
       nombresEmpleadosTaller.addAll(empleadosTaller.map { " ${it.NombreCompleto} " })

       val adapterTall = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresEmpleadosTaller)
       adapterTall.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
       binding.spinnerPersonaReparacionTaller.adapter = adapterTall
   }

  /*  private fun configurarSpinnersEmpresas() {
        // Spinner empresas externas
        val empresasExternas = empresasTaller.filter { it.Tipo == "EXTERNA" }
        val nombresExternas = empresasExternas.map { it.Nombre }
        val adapterExternas = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresExternas)
        adapterExternas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEmpresaExternaTaller.adapter = adapterExternas

        // Spinner empresas internas
        val empresasInternas = empresasTaller.filter { it.Tipo == "INTERNA" }
        val nombresInternas = empresasInternas.map { it.Nombre }
        val adapterInternas = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresInternas)
        adapterInternas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEmpresaInternaTaller.adapter = adapterInternas
    }*/
  private fun configurarSpinnersEmpresas() {
      // <CHANGE> Agregar "Seleccionar empresa..." como primer elemento para empresas externas
      val empresasExternas = empresasTaller.filter { it.Tipo == "EXTERNA" }
      val nombresExternas = mutableListOf("Seleccionar empresa...")
      nombresExternas.addAll(empresasExternas.map { it.Nombre ?: "Sin nombre" })

      val adapterExternas = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresExternas)
      adapterExternas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      binding.spinnerEmpresaExternaTaller.adapter = adapterExternas

      // <CHANGE> Agregar "Seleccionar empresa..." como primer elemento para empresas internas
      val empresasInternas = empresasTaller.filter { it.Tipo == "INTERNA" }
      val nombresInternas = mutableListOf("Seleccionar empresa...")
      nombresInternas.addAll(empresasInternas.map { it.Nombre ?: "Sin nombre" })

      val adapterInternas = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresInternas)
      adapterInternas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      binding.spinnerEmpresaInternaTaller.adapter = adapterInternas
  }

    private fun configurarPartesDanadas() {
        if (partesDanadas.isEmpty()) return

        totalPaginas = (partesDanadas.size + partesPorPagina - 1) / partesPorPagina
        paginaActual = 0

        mostrarPartesActuales()
        actualizarBotonesPaginacion()
    }

    private fun mostrarPartesActuales() {
        binding.containerPartesTaller.removeAllViews()

        val inicio = paginaActual * partesPorPagina
        val fin = minOf(inicio + partesPorPagina, partesDanadas.size)

        for (i in inicio until fin) {
            val parte = partesDanadas[i]
            val itemView = crearItemParte(parte)
            binding.containerPartesTaller.addView(itemView)
        }

        binding.tvPaginaActualTaller.text = "Página ${paginaActual + 1} de $totalPaginas"
    }

    private fun crearItemParte(parte: ParteDanno): View {
        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.item_parte_danada, null)

        val ivParteDanada = itemView.findViewById<ImageView>(R.id.ivParteDanada)
        val tvNombreParte = itemView.findViewById<TextView>(R.id.tvNombreParte)
        val cbSeleccionarParte = itemView.findViewById<CheckBox>(R.id.cbSeleccionarParte)

        // Configurar imagen según la parte
        val imagenResource = obtenerImagenParte(parte.IdParteDanno)
        ivParteDanada.setImageResource(imagenResource)

        tvNombreParte.text = parte.Nombre
        cbSeleccionarParte.isChecked = parte.Seleccionada

        cbSeleccionarParte.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Deseleccionar otras partes
                partesDanadas.forEach { it.Seleccionada = false }
                parte.Seleccionada = true
                parteSeleccionada = parte

                // Actualizar vista
                mostrarPartesActuales()
            } else {
                parte.Seleccionada = false
                if (parteSeleccionada == parte) {
                    parteSeleccionada = null
                }
            }
        }

        return itemView
    }

    private fun obtenerImagenParte(idParte:Short): Int {
        var id=R.drawable.ic_car_part_placeholder
        if(idParte.toInt()==11)id= R.drawable.veh_cofre_200px
        else if(idParte.toInt()==1)id= R.drawable.veh_cajuela_200px
        else if(idParte.toInt()==2)id= R.drawable.veh_pd_200px
        else if (idParte.toInt()==16)id= R.drawable.veh_pt_200px
        else if (idParte.toInt()==10)id= R.drawable.veh_toldo_200px
        else if (idParte.toInt()==7)id= R.drawable.veh_trasero_200px
        return id
    }


    //Optimizado l
    private fun actualizarBotonesPaginacion() {
        binding.apply {
            btnPaginaAnteriorTaller.isEnabled = paginaActual > 0
            btnPaginaSiguienteTaller.isEnabled = paginaActual < totalPaginas - 1
            btnPaginaAnteriorTaller.alpha = if (paginaActual > 0) 1.0f else 0.5f
            btnPaginaSiguienteTaller.alpha = if (paginaActual < totalPaginas - 1) 1.0f else 0.5f
        }
    }


    private fun mostrarSelectorFechaHora() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val fechaHora = String.format("%04d-%02d-%02d %02d:%02d",
                            year, month + 1, dayOfMonth, hourOfDay, minute)
                        binding.tvFechaInicioTaller.text = fechaHora
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }


    //Optimizado
    private fun mostrarFormularios() {
        binding.apply {
            layoutFormularioTaller.visibility = View.VISIBLE
            layoutPartesDanadasTaller.visibility = View.VISIBLE
            layoutDescripcionTaller.visibility = View.VISIBLE
            layoutBotonesTaller.visibility = View.VISIBLE
        }
    }


   /* private fun guardarRegistroTaller() {
        if (!validarFormulario()) return
        mostrarCarga("Guardando registro de taller...")
        lifecycleScope.launch {
            try {
                var idUsuario=ParametrosSistema.usuarioLogueado.Id?.toInt()
                var empleado = empleados[binding.spinnerPersonalMovimientoTaller.selectedItemPosition]
                var PersonaMoviento= empleado.NombreCompleto
                var tipoReparacion = 1
                if(binding.radioForaneaTaller.isSelected)  tipoReparacion = 2
                var PersonaReparacion = binding.spinnerPersonaReparacionTaller.selectedItemPosition
                var idParteDanno = parteSeleccionada?.IdParteDanno ?: 0
                var descripcion = binding.etDescripcionTaller.text.toString()

                val paso=PasoLogVehiculoDet(
                    IdVehiculo = vehiculoActual?.Id!!.toInt(),
                    IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo,
                    PersonaQueHaraMovimiento = PersonaMoviento,
                    IdTipoEntradaSalida = tipoReparacion,
                    IdParteDanno = idParteDanno.toInt(),
                    Observacion = descripcion,
                    IdStatus = 171,
                    IdUsuarioMovimiento = idUsuario,
                    IdEmpleadoPosiciono = PersonaReparacion,
                    FechaMovimiento = fechaActual,
                    Placa = ""
                )
                val exito = dalTaller.crearRegistroTaller(paso)
                ocultarCarga()
                if (exito) {
                    Toast.makeText(this@PasoTaller_Activity, "✅ Registro de taller guardado exitosamente", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    mostrarError("Error al guardar el registro de taller")
                }
            } catch (e: Exception) {
                ocultarCarga()
                mostrarError("Error de conexión: ${e.message}")
                Log.e("PasoTaller_Activity", "Error guardando: ${e.message}")
            }
        }
    }*/
 /*  private fun guardarRegistroTaller() {
       if (!validarFormulario()) {
           return
       }

       mostrarCarga("Guardando registro de taller...")

       lifecycleScope.launch {
           try {
               var idUsuario = ParametrosSistema.usuarioLogueado.Id?.toInt()

               // <CHANGE> Restar 1 porque el índice 0 es el placeholder
               var empleado = empleados[binding.spinnerPersonalMovimientoTaller.selectedItemPosition - 1]
               var PersonaMoviento = empleado.NombreCompleto

               var tipoReparacion = 1
               if(binding.radioForaneaTaller.isSelected) tipoReparacion = 2

               // <CHANGE> Restar 1 porque el índice 0 es el placeholder
               var PersonaReparacion = binding.spinnerPersonaReparacionTaller.selectedItemPosition - 1
               var emp= empleadosTaller[PersonaReparacion]
               var IdEmpleadoTaller = emp.IdEmpleado

               var idParteDanno = parteSeleccionada?.IdParteDanno ?: 0
               var descripcion = binding.etDescripcionTaller.text.toString()

               val paso = PasoLogVehiculoDet(
                   IdVehiculo = vehiculoActual?.Id!!.toInt(),
                   IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo,
                   PersonaQueHaraMovimiento = PersonaMoviento,
                   IdTipoEntradaSalida = tipoReparacion,
                   IdParteDanno = idParteDanno.toInt(),
                   Observacion = descripcion,
                   IdStatus = 171,
                   IdUsuarioMovimiento = idUsuario,
                   IdEmpleadoPosiciono = IdEmpleadoTaller,
                   FechaMovimiento = fechaActual,
                   Placa = ""
               )

               val exito = dalTaller.crearRegistroTaller(paso)

               ocultarCarga()

               if (exito) {
                   Toast.makeText(this@PasoTaller_Activity, "✅ Registro de taller guardado exitosamente", Toast.LENGTH_LONG).show()
                   finish()
               } else {
                   mostrarError("Error al guardar el registro de taller")
               }
           } catch (e: Exception) {
               ocultarCarga()
               mostrarError("Error de conexión: ${e.message}")
               Log.e("PasoTaller_Activity", "Error guardando: ${e.message}")
           }
       }
   }*/
   private fun guardarRegistroTaller() {
       if (!validarFormulario()) {
           return
       }

       mostrarCarga("Guardando registro de taller...")

       lifecycleScope.launch {
           try {
               var idUsuario = ParametrosSistema.usuarioLogueado.Id?.toInt()



               var empleado = empleados[binding.spinnerPersonalMovimientoTaller.selectedItemPosition - 1]
               var PersonaMoviento = empleado.NombreCompleto


               var tipoReparacion = 1 // 1 = En Sitio, 2 = Foranea
               var IdEmpleadoTaller: Int? = null
               var IdEmpresaExterna: Int? = null


               if (binding.radioForaneaTaller.isChecked) {

                   var EmpresaReparacion = binding.spinnerEmpresaExternaTaller.selectedItemPosition - 1
                   var empExt = empresasTaller[EmpresaReparacion]
                   IdEmpresaExterna = empExt.IdCliente


                   Log.d("PasoTaller", "Guardando con tipo: FORANEA")

                   tipoReparacion = 2
                   IdEmpleadoTaller = null

               } else if (binding.radioEnSitioTaller.isChecked) {

                   Log.d("PasoTaller", "Guardando con tipo: EN SITIO")

                   tipoReparacion = 1
                   var PersonaReparacion = binding.spinnerPersonaReparacionTaller.selectedItemPosition - 1
                   var emp = empleadosTaller[PersonaReparacion]
                   IdEmpleadoTaller = emp.IdEmpleado
                   IdEmpresaExterna = null
               }

               var idParteDanno = parteSeleccionada?.IdParteDanno ?: 0
               var descripcion = binding.etDescripcionTaller.text.toString()

               val paso = PasoLogVehiculoDet(
                   IdVehiculo = vehiculoActual?.Id!!.toInt(),
                   IdPasoLogVehiculo = vehiculoActual?.IdPasoLogVehiculo,
                   PersonaQueHaraMovimiento = PersonaMoviento,
                   IdTipoEntradaSalida = tipoReparacion, // 1 = En Sitio, 2 = Foranea
                   IdParteDanno = idParteDanno.toInt(),
                   Observacion = descripcion,
                   IdStatus = 171,
                   IdUsuarioMovimiento = idUsuario,
                   IdEmpleadoPosiciono = IdEmpleadoTaller, // null para Foranea, IdEmpleado para En Sitio
                   FechaMovimiento = fechaActual,
                   Placa = "" ,
                   IdTransporte = IdEmpresaExterna
               )

               val exito = dalPasoLog.insertaStatusNuevoPasoLogVehiculo(paso)

               ocultarCarga()

               if (exito) {
                   Toast.makeText(this@PasoTaller_Activity, "Registro de taller guardado exitosamente", Toast.LENGTH_LONG).show()
                   val data = Intent()
                   data.putExtra("Refrescar", true)
                   data.putExtra("Vin", vehiculoActual?.VIN)
                   setResult(Activity.RESULT_OK, data)
                   finish()
               } else {
                   mostrarError("Error al guardar el registro de taller")
               }
           } catch (e: Exception) {
               ocultarCarga()
               mostrarError("Error de conexión: ${e.message}")
               Log.e("PasoTaller_Activity", "Error guardando: ${e.message}")
           }
       }
   }


    /*private fun validarFormulario(): Boolean {
        // Validar personal general
        if (binding.spinnerPersonalMovimientoTaller.selectedItemPosition < 0) {
            mostrarError("Seleccione el personal que hará el movimiento")
            return false
        }

        // Validar tipo de reparación específico
        if (binding.radioForaneaTaller.isChecked && binding.spinnerEmpresaExternaTaller.selectedItemPosition < 0) {
            mostrarError("Seleccione la empresa externa")
            return false
        }

        if (binding.radioEnSitioTaller.isChecked) {
            if (binding.spinnerEmpresaInternaTaller.selectedItemPosition < 0) {
                mostrarError("Seleccione la empresa que realizará la reparación")
                return false
            }
            if (binding.spinnerPersonaReparacionTaller.selectedItemPosition < 0) {
                mostrarError("Seleccione la persona que realizará el trabajo")
                return false
            }
        }

        // Validar parte seleccionada
        if (parteSeleccionada == null) {
            mostrarError("Seleccione la parte a reparar")
            return false
        }
        // Validar fecha
        if (binding.tvFechaInicioTaller.text.toString().isEmpty()) {
            mostrarError("Seleccione la fecha de inicio")
            return false
        }
        return true
    }*/
    private fun validarFormulario(): Boolean {

        if (binding.spinnerPersonalMovimientoTaller.selectedItemPosition == 0) {
            Toast.makeText(this, "Seleccione el personal que hará el movimiento", Toast.LENGTH_SHORT).show()
            return false
        }


        if (binding.radioEnSitioTaller.isChecked) {


            if (binding.spinnerEmpresaInternaTaller.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione la empresa que realizará la reparacióna", Toast.LENGTH_SHORT).show()
                return false
            }

            if (binding.spinnerPersonaReparacionTaller.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione la persona que realizará el trabajo", Toast.LENGTH_SHORT).show()
                return false
            }

        }

        if (binding.radioForaneaTaller.isChecked) {

            if (binding.spinnerEmpresaExternaTaller.selectedItemPosition == 0) {
                Toast.makeText(this, "Seleccione la empresa que realizará la reparacióna", Toast.LENGTH_SHORT).show()
                return false
            }

        }


        if (parteSeleccionada == null) {
            Toast.makeText(this, "Seleccione la parte a reparar", Toast.LENGTH_SHORT).show()
            return false
        }


        if (binding.tvFechaInicioTaller.text.toString().isEmpty()) {
            Toast.makeText(this, "Seleccione la fecha de inicio", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }


    //Optimizado
   /* private fun limpiarFormulario() {
        binding.apply {
            spinnerPersonalMovimientoTaller.setSelection(0)
            radioEnSitioTaller.isChecked = true
            spinnerEmpresaExternaTaller.setSelection(0)
            spinnerEmpresaInternaTaller.setSelection(0)
            spinnerPersonaReparacionTaller.setSelection(0)
            etDescripcionTaller.setText("")
        }

        inicializarFechaActual()


        partesDanadas.forEach { it.Seleccionada = false }
        parteSeleccionada = null
        paginaActual = 0

        mostrarPartesActuales()
        actualizarBotonesPaginacion()
        ocultarError()
    }*/

    private fun limpiarFormulario() {
        binding.apply {
            // <CHANGE> Ahora la posición 0 es el placeholder
            spinnerPersonalMovimientoTaller.setSelection(0)
            radioEnSitioTaller.isChecked = true
            spinnerEmpresaExternaTaller.setSelection(0)
            spinnerEmpresaInternaTaller.setSelection(0)
            spinnerPersonaReparacionTaller.setSelection(0)
            etDescripcionTaller.setText("")
        }

        inicializarFechaActual()

        partesDanadas.forEach { it.Seleccionada = false }
        parteSeleccionada = null
        paginaActual = 0

        mostrarPartesActuales()
        actualizarBotonesPaginacion()
        ocultarError()
    }


    //Optimizado
    private fun mostrarCarga(mensaje: String) {
        binding.apply {
            tvLoadingTextTaller.text = mensaje
            loadingContainerTaller.visibility = View.VISIBLE
            btnGuardarTaller.isEnabled = false
            btnGuardarTaller.alpha = 0.5f
        }
    }


    //Optimizado
    private fun ocultarCarga() {
        binding.apply {
            loadingContainerTaller.visibility = View.GONE
            btnGuardarTaller.isEnabled = true
            btnGuardarTaller.alpha = 1.0f
        }

    }


    //Optimizado
    private fun mostrarError(mensaje: String) {
        binding.apply {
            tvErrorTaller.text = mensaje
            layoutErrorTaller.visibility = View.VISIBLE
        }

    }


    private fun inicializarFechaActual() {
        val formatoFechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        binding.tvFechaInicioTaller.text = formatoFechaHora.format(Date())
    }


    private fun ocultarError() {
        binding.layoutErrorTaller.visibility = View.GONE
    }











}