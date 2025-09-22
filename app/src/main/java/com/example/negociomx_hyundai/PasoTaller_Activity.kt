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
    // Variables de UI
    private lateinit var layoutInfoVehiculo: LinearLayout
    private lateinit var layoutFormularioTaller: LinearLayout
    private lateinit var layoutPartesDanadas: LinearLayout
    private lateinit var layoutDescripcion: LinearLayout
    private lateinit var layoutBotones: LinearLayout
    private lateinit var layoutError: LinearLayout
    private lateinit var loadingContainer: LinearLayout

    private lateinit var tvInfoVehiculo: TextView
    private lateinit var spinnerPersonalMovimiento: Spinner
    private lateinit var radioGroupTipoReparacion: RadioGroup
    private lateinit var radioEnSitio: RadioButton
    private lateinit var radioForanea: RadioButton
    private lateinit var layoutEmpresaExterna: LinearLayout
    private lateinit var layoutEmpresaInterna: LinearLayout
    private lateinit var spinnerEmpresaExterna: Spinner
    private lateinit var spinnerEmpresaInterna: Spinner
    private lateinit var spinnerPersonaReparacion: Spinner
    private lateinit var tvFechaInicio: TextView
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var containerPartes: LinearLayout
    private lateinit var btnPaginaAnterior: Button
    private lateinit var btnPaginaSiguiente: Button
    private lateinit var tvPaginaActual: TextView
    private lateinit var etDescripcion: EditText
    private lateinit var btnLimpiar: Button
    private lateinit var btnGuardar: Button
    private lateinit var tvLoadingText: TextView
    private lateinit var tvError: TextView

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarComponentes()
        configurarEventos()
        obtenerDatosVehiculo()
        cargarDatosIniciales()
    }

    private fun inicializarComponentes() {
        // Layouts principales
        layoutInfoVehiculo = findViewById(R.id.layoutInfoVehiculo)
        layoutFormularioTaller = findViewById(R.id.layoutFormularioTaller)
        layoutPartesDanadas = findViewById(R.id.layoutPartesDanadas)
        layoutDescripcion = findViewById(R.id.layoutDescripcion)
        layoutBotones = findViewById(R.id.layoutBotones)
        layoutError = findViewById(R.id.layoutError)
        loadingContainer = findViewById(R.id.loadingContainer)

        // Componentes de UI
        tvInfoVehiculo = findViewById(R.id.tvInfoVehiculo)
        spinnerPersonalMovimiento = findViewById(R.id.spinnerPersonalMovimiento)
        radioGroupTipoReparacion = findViewById(R.id.radioGroupTipoReparacion)
        radioEnSitio = findViewById(R.id.radioEnSitio)
        radioForanea = findViewById(R.id.radioForanea)
        layoutEmpresaExterna = findViewById(R.id.layoutEmpresaExterna)
        layoutEmpresaInterna = findViewById(R.id.layoutEmpresaInterna)
        spinnerEmpresaExterna = findViewById(R.id.spinnerEmpresaExterna)
        spinnerEmpresaInterna = findViewById(R.id.spinnerEmpresaInterna)
        spinnerPersonaReparacion = findViewById(R.id.spinnerPersonaReparacion)
        tvFechaInicio = findViewById(R.id.tvFechaInicio)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        containerPartes = findViewById(R.id.containerPartes)
        btnPaginaAnterior = findViewById(R.id.btnPaginaAnterior)
        btnPaginaSiguiente = findViewById(R.id.btnPaginaSiguiente)
        tvPaginaActual = findViewById(R.id.tvPaginaActual)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnLimpiar = findViewById(R.id.btnLimpiar)
        btnGuardar = findViewById(R.id.btnGuardar)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvError = findViewById(R.id.tvError)

        // Inicializar fecha actual
        inicializarFechaActual()
    }

    private fun configurarEventos() {
        // Radio buttons para tipo de reparación
        radioGroupTipoReparacion.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioEnSitio -> {
                    layoutEmpresaExterna.visibility = View.GONE
                    layoutEmpresaInterna.visibility = View.VISIBLE
                }
                R.id.radioForanea -> {
                    layoutEmpresaExterna.visibility = View.VISIBLE
                    layoutEmpresaInterna.visibility = View.GONE
                }
            }
        }

        binding.btnRegresarPasoTaller.setOnClickListener {
            finish()
        }

        // Selector de fecha
        btnSeleccionarFecha.setOnClickListener {
            mostrarSelectorFechaHora()
        }

        // Navegación de páginas
        btnPaginaAnterior.setOnClickListener {
            if (paginaActual > 0) {
                paginaActual--
                mostrarPartesActuales()
                actualizarBotonesPaginacion()
            }
        }

        btnPaginaSiguiente.setOnClickListener {
            if (paginaActual < totalPaginas - 1) {
                paginaActual++
                mostrarPartesActuales()
                actualizarBotonesPaginacion()
            }
        }

        // Botones de acción
        btnLimpiar.setOnClickListener {
            limpiarFormulario()
        }

        btnGuardar.setOnClickListener {
            guardarRegistroTaller()
        }
    }

    private fun obtenerDatosVehiculo() {
        // Obtener datos del vehículo desde Intent
        val vinVehiculo = intent.getStringExtra("VIN") ?: ""
        val idVehiculo = intent.getIntExtra("ID_VEHICULO", 0)

        if (vinVehiculo.isNotEmpty() || idVehiculo > 0) {
            // Crear objeto vehiculo temporal (en implementación real, consultar BD)
            vehiculoActual = VehiculoPasoLog(
                Id = idVehiculo.toString(),
                VIN = vinVehiculo,
                Marca = intent.getStringExtra("MARCA") ?: "HYUNDAI",
                Modelo = intent.getStringExtra("MODELO") ?: "ACCENT",
                Anio = intent.getIntExtra("ANIO", 2024),
                ColorExterior = intent.getStringExtra("COLOR_EXTERIOR") ?: "BLANCO",
                ColorInterior = intent.getStringExtra("COLOR_INTERIOR") ?: "NEGRO"
            )

            mostrarInfoVehiculo()
        } else {
            mostrarError("No se recibieron datos del vehículo")
        }
    }

    private fun mostrarInfoVehiculo() {
        vehiculoActual?.let { vehiculo ->
            val info = buildString {
                append("VIN: ${vehiculo.VIN}\n")
                append("Marca: ${vehiculo.Marca} ${vehiculo.Modelo} ${vehiculo.Anio}\n")
                append("Color: ${vehiculo.ColorExterior} / ${vehiculo.ColorInterior}")
            }
            tvInfoVehiculo.text = info
            layoutInfoVehiculo.visibility = View.VISIBLE
        }
    }

    private fun cargarDatosIniciales() {
        mostrarCarga("Cargando datos iniciales...")

        lifecycleScope.launch {
            try {
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
        spinnerPersonalMovimiento.adapter = adapter
    }

    private fun configurarSpinnersEmpresas() {
        // Spinner empresas externas
        val empresasExternas = empresasTaller.filter { it.Tipo == "EXTERNA" }
        val nombresExternas = empresasExternas.map { it.Nombre }
        val adapterExternas = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresExternas)
        adapterExternas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmpresaExterna.adapter = adapterExternas

        // Spinner empresas internas
        val empresasInternas = empresasTaller.filter { it.Tipo == "INTERNA" }
        val nombresInternas = empresasInternas.map { it.Nombre }
        val adapterInternas = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresInternas)
        adapterInternas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmpresaInterna.adapter = adapterInternas

        // Spinner personas de reparación (inicialmente vacío)
        val adapterPersonas = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf<String>())
        adapterPersonas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPersonaReparacion.adapter = adapterPersonas
    }

    private fun configurarPartesDanadas() {
        if (partesDanadas.isEmpty()) return

        totalPaginas = (partesDanadas.size + partesPorPagina - 1) / partesPorPagina
        paginaActual = 0

        mostrarPartesActuales()
        actualizarBotonesPaginacion()
    }

    private fun mostrarPartesActuales() {
        containerPartes.removeAllViews()

        val inicio = paginaActual * partesPorPagina
        val fin = minOf(inicio + partesPorPagina, partesDanadas.size)

        for (i in inicio until fin) {
            val parte = partesDanadas[i]
            val itemView = crearItemParte(parte)
            containerPartes.addView(itemView)
        }

        tvPaginaActual.text = "Página ${paginaActual + 1} de $totalPaginas"
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
        btnPaginaAnterior.isEnabled = paginaActual > 0
        btnPaginaSiguiente.isEnabled = paginaActual < totalPaginas - 1

        btnPaginaAnterior.alpha = if (paginaActual > 0) 1.0f else 0.5f
        btnPaginaSiguiente.alpha = if (paginaActual < totalPaginas - 1) 1.0f else 0.5f
    }

    private fun inicializarFechaActual() {
        val formatoFechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        tvFechaInicio.text = formatoFechaHora.format(Date())
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
                        tvFechaInicio.text = fechaHora
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
        layoutFormularioTaller.visibility = View.VISIBLE
        layoutPartesDanadas.visibility = View.VISIBLE
        layoutDescripcion.visibility = View.VISIBLE
        layoutBotones.visibility = View.VISIBLE
    }

    private fun guardarRegistroTaller() {
        if (!validarFormulario()) return

        mostrarCarga("Guardando registro de taller...")

        lifecycleScope.launch {
            try {
                val idVehiculo = vehiculoActual?.Id?.toIntOrNull() ?: 0
                val idUsuario = 1 // Obtener del usuario actual
                val idPersonalMovimiento = empleados[spinnerPersonalMovimiento.selectedItemPosition].IdEmpleado
                val tipoReparacion = if (radioEnSitio.isChecked) "EN_SITIO" else "FORANEA"
                val idParteDanno = parteSeleccionada?.IdParteDanno ?: 0
                val descripcion = etDescripcion.text.toString()
                val fechaInicio = tvFechaInicio.text.toString()

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
        if (spinnerPersonalMovimiento.selectedItemPosition < 0) {
            mostrarError("Seleccione el personal que hará el movimiento")
            return false
        }

        // Validar tipo de reparación específico
        if (radioForanea.isChecked && spinnerEmpresaExterna.selectedItemPosition < 0) {
            mostrarError("Seleccione la empresa externa")
            return false
        }

        if (radioEnSitio.isChecked) {
            if (spinnerEmpresaInterna.selectedItemPosition < 0) {
                mostrarError("Seleccione la empresa que realizará la reparación")
                return false
            }
            if (spinnerPersonaReparacion.selectedItemPosition < 0) {
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
        if (tvFechaInicio.text.toString().isEmpty()) {
            mostrarError("Seleccione la fecha de inicio")
            return false
        }

        return true
    }

    private fun limpiarFormulario() {
        spinnerPersonalMovimiento.setSelection(0)
        radioEnSitio.isChecked = true
        spinnerEmpresaExterna.setSelection(0)
        spinnerEmpresaInterna.setSelection(0)
        spinnerPersonaReparacion.setSelection(0)
        etDescripcion.setText("")
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
        tvLoadingText.text = mensaje
        loadingContainer.visibility = View.VISIBLE
        btnGuardar.isEnabled = false
        btnGuardar.alpha = 0.5f
    }

    private fun ocultarCarga() {
        loadingContainer.visibility = View.GONE
        btnGuardar.isEnabled = true
        btnGuardar.alpha = 1.0f
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        layoutError.visibility = View.VISIBLE
    }

 /*   private fun ob(){


        // Ejemplo de cómo llamar la Activity de Taller
        val intent = Intent(this, PasoTaller_Activity::class.java)
        intent.putExtra("VIN", vehiculoSeleccionado.VIN)
        intent.putExtra("ID_VEHICULO", vehiculoSeleccionado.Id.toInt())
        intent.putExtra("MARCA", vehiculoSeleccionado.Marca)
        intent.putExtra("MODELO", vehiculoSeleccionado.Modelo)
        intent.putExtra("ANIO", vehiculoSeleccionado.Anio)
        intent.putExtra("COLOR_EXTERIOR", vehiculoSeleccionado.ColorExterior)
        intent.putExtra("COLOR_INTERIOR", vehiculoSeleccionado.ColorInterior)
        startActivity(intent)
    }*/




    private fun ocultarError() {
        layoutError.visibility = View.GONE
    }











}