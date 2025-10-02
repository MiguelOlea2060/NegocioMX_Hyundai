package com.example.negociomx_hyundai

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.example.negociomx_hyundai.DAL.DALCliente
import com.example.negociomx_hyundai.DAL.DALClienteSQL
import com.example.negociomx_hyundai.DAL.DALEmpleadoSQL
import com.example.negociomx_hyundai.DAL.DALTaller
import com.example.negociomx_hyundai.databinding.ActivityPasoTallerBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PasoTaller_Activity : AppCompatActivity() {



    private lateinit var binding:ActivityPasoTallerBinding

    // Variables de datos
    private var vehiculoActual: VehiculoPasoLog? = null
    private var empleados = listOf<Empleado>()
    private var empresasTaller = listOf<Cliente>()
    private var partesDanadas = listOf<ParteDanno>()
    private var parteSeleccionada: ParteDanno? = null

    // Variables de paginación
    private var paginaActual = 0
    private val partesPorPagina = 2
    private var totalPaginas = 0

    // DALs
    private val dalTaller = DALTaller()
    private var dalEmpresaTaller=DALClienteSQL()
    private val dalEmpleado = DALEmpleadoSQL()


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
            val idVehiculo = intent.getIntExtra("IdVehiculo", 0)
            val vin = intent.getStringExtra("Vin") ?: ""
            val bl = intent.getStringExtra("Bl") ?: ""
            val marca = intent.getStringExtra("Marca") ?: ""
            val modelo = intent.getStringExtra("Modelo") ?: ""
            val annioAux = intent.getStringExtra("Annio") ?: ""
            var annio: Int = 0
            if (annioAux.isNotEmpty())
                annio=annioAux.toInt()
            val colorExterior = intent.getStringExtra("ColorExterior") ?: ""
            val colorInterior = intent.getStringExtra("ColorInterior") ?: ""
            val especificaciones = intent.getStringExtra("Especificaciones") ?: ""


            if (idVehiculo > 0 && vin.isNotEmpty()) {
                vehiculoActual = VehiculoPasoLog(
                    Id = idVehiculo.toString(),
                    VIN = vin,
                    BL = bl,
                    Marca = marca,
                    Modelo = modelo,
                    Anio = annio,
                    ColorExterior = colorExterior,
                    ColorInterior = colorInterior,
                    Especificaciones = especificaciones
                )

                mostrarInfoVehiculo()

                Log.d("PasoTaller_Activity", "✅ Datos del vehículo obtenidos: VIN=$vin")
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
                empleados = dalEmpleado.consultarEmpleados(105)
                configurarSpinnerConductor()

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

    private fun configurarSpinnerConductor() {
        val nombresEmpleados = empleados.map { "${it.NombreCompleto} (ID: ${it.IdEmpleado})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresEmpleados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPersonalMovimientoTaller.adapter = adapter
    }

    private fun configurarSpinnersEmpresas() {
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

        // Spinner personas de reparación (inicialmente vacío)
        val adapterPersonas = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf<String>())
        adapterPersonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPersonaReparacionTaller.adapter = adapterPersonas
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

    private fun actualizarBotonesPaginacion() {
        binding.btnPaginaAnteriorTaller.isEnabled = paginaActual > 0
        binding.btnPaginaSiguienteTaller.isEnabled = paginaActual < totalPaginas - 1

        binding.btnPaginaAnteriorTaller.alpha = if (paginaActual > 0) 1.0f else 0.5f
        binding.btnPaginaSiguienteTaller.alpha = if (paginaActual < totalPaginas - 1) 1.0f else 0.5f
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


    private fun mostrarFormularios() {
        binding.layoutFormularioTaller.visibility = View.VISIBLE
        binding.layoutPartesDanadasTaller.visibility = View.VISIBLE
        binding.layoutDescripcionTaller.visibility = View.VISIBLE
        binding.layoutBotonesTaller.visibility = View.VISIBLE
    }


    private fun guardarRegistroTaller() {
        if (!validarFormulario()) return

        mostrarCarga("Guardando registro de taller...")

        lifecycleScope.launch {
            try {
                val idVehiculo = vehiculoActual?.Id?.toIntOrNull() ?: 0
                val idUsuario = 1 // Obtener del usuario actual
                val idPersonalMovimiento = empleados[binding.spinnerPersonalMovimientoTaller.selectedItemPosition].IdEmpleado
                val tipoReparacion = if (binding.radioEnSitioTaller.isChecked) "EN_SITIO" else "FORANEA"
                val idParteDanno = parteSeleccionada?.IdParteDanno ?: 0
                val descripcion = binding.etDescripcionTaller.text.toString()
                val fechaInicio = binding.tvFechaInicioTaller.text.toString()

                val resultado = dalTaller.crearRegistroTaller(
                    idVehiculo = idVehiculo,
                    idUsuario = idUsuario,
                    idPersonalMovimiento = idPersonalMovimiento,
                    tipoReparacion = tipoReparacion,
                    idParteDanno = idParteDanno,
                    descripcion = descripcion,
                    fechaInicio = fechaInicio
                )

                ocultarCarga()

                if (resultado) {
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
    }


    private fun validarFormulario(): Boolean {
        // Validar personal
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
    }


    private fun limpiarFormulario() {
        binding.spinnerPersonalMovimientoTaller.setSelection(0)
        binding.radioEnSitioTaller.isChecked = true
        binding.spinnerEmpresaExternaTaller.setSelection(0)
        binding.spinnerEmpresaInternaTaller.setSelection(0)
        binding.spinnerPersonaReparacionTaller.setSelection(0)
        binding.etDescripcionTaller.setText("")
        inicializarFechaActual()

        // Limpiar selección de partes
        partesDanadas.forEach { it.Seleccionada = false }
        parteSeleccionada = null
        paginaActual = 0
        mostrarPartesActuales()
        actualizarBotonesPaginacion()

        ocultarError()
    }


    // MÉTODOS DE UI
    private fun mostrarCarga(mensaje: String) {
        binding.tvLoadingTextTaller.text = mensaje
        binding.loadingContainerTaller.visibility = View.VISIBLE
        binding.btnGuardarTaller.isEnabled = false
        binding.btnGuardarTaller.alpha = 0.5f
    }


    private fun ocultarCarga() {
        binding.loadingContainerTaller.visibility = View.GONE
        binding.btnGuardarTaller.isEnabled = true
        binding.btnGuardarTaller.alpha = 1.0f
    }


    private fun mostrarError(mensaje: String) {
        binding.tvErrorTaller.text = mensaje
        binding.layoutErrorTaller.visibility = View.VISIBLE
    }

    private fun inicializarFechaActual() {
        val formatoFechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        binding.tvFechaInicioTaller.text = formatoFechaHora.format(Date())
    }

    private fun ocultarError() {
        binding.layoutErrorTaller.visibility = View.GONE
    }











}