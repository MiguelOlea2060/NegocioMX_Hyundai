package com.example.negociomx_hyundai

import android.content.Intent
import android.database.DatabaseUtils
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.Usuario
import com.example.negociomx_hyundai.DAL.DALEmpresa
import com.example.negociomx_hyundai.DAL.DALUsuarioSQL
import com.example.negociomx_hyundai.adapters.SpinnerAdapter
import com.example.negociomx_hyundai.databinding.ActivityUsuarioNuevoBinding
import com.example.negociomx_hyundai.room.BLL.BLLUtil
import com.example.negociomx_hyundai.room.entities.Admins.Rol
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class usuario_nuevo_activity : AppCompatActivity() {

    lateinit var binding: ActivityUsuarioNuevoBinding
    lateinit var firebaseRef: DatabaseReference
    lateinit var firebaseUtil: DatabaseUtils

    lateinit var dalUsu: DALUsuarioSQL
    lateinit var dalEmp: DALEmpresa

    lateinit var listaUsuarios: List<Usuario>

    lateinit var bllUtil: BLLUtil

    var idEmpresaParaUsuario:Int?=null

    private var timeoutHandler: Handler? = null
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuarioNuevoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val idEmpresaAux:Int?= intent.extras?.getInt("IdEmpresaParaUsuario",0)
        if(idEmpresaAux!=null)
            idEmpresaParaUsuario=idEmpresaAux

        Log.d("UsuarioNuevo", "🎬 === VERSIÓN DEFINITIVA ===")

        dalUsu = DALUsuarioSQL()
        dalEmp = DALEmpresa()
        bllUtil = BLLUtil()

        firebaseRef = FirebaseDatabase.getInstance().getReference("Usuario")

        var listaRoles: List<Rol>
        listaRoles = arrayListOf()

        listaRoles.add(Rol(IdRol = 0, Nombre = "Seleccione..."))
        listaRoles.add(Rol(IdRol = 2, Nombre = "Administrador"))
        listaRoles.add(Rol(IdRol = 3, Nombre = "Supervisor"))
        listaRoles.add(Rol(IdRol = 4, Nombre = "Empleado"))
        listaRoles.add(Rol(IdRol = 5, Nombre = "Usuario Cliente"))

        var adapter: SpinnerAdapter
        adapter = bllUtil.convertListRolToListSpinner(this, listaRoles)

        // Configurar validación en tiempo real
        setupRealTimeValidation()

        binding.apply {
            btnRegresarNuevo.setOnClickListener {
                cierraPantalla()
            }
            btnEnviarSolicitudUsuarioNuevo.setOnClickListener {
                if (!isProcessing) {
                    registrarUsuarioDefinitivo()
                }
            }
        }
    }

    private fun setupRealTimeValidation() {
        // Validación de email en tiempo real
        binding.txtEmailUsuarioNuevo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.txtEmailUsuarioNuevo.error = "Email no válido"
                }
            }
        })

        // Validación de contraseña en tiempo real
        binding.txtContrasenaUsuarioNuevo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isNotEmpty() && password.length < 6) {
                    binding.txtContrasenaUsuarioNuevo.error = "Mínimo 6 caracteres"
                }
                validatePasswordMatch()
            }
        })

        // Validación de confirmación de contraseña
        binding.txtRepetirContrasenaUsuarioNuevo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswordMatch()
            }
        })
    }

    private fun validatePasswordMatch() {
        val password = binding.txtContrasenaUsuarioNuevo.text.toString()
        val confirmPassword = binding.txtRepetirContrasenaUsuarioNuevo.text.toString()

        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            binding.txtRepetirContrasenaUsuarioNuevo.error = "Las contraseñas no coinciden"
        }
    }

    private fun registrarUsuarioDefinitivo() {
        val nombreCompleto = binding.txtNombreCompletoUsuarioNuevo.text.toString().trim()
        val contrasena = binding.txtContrasenaUsuarioNuevo.text.toString()
        val contrasena1 = binding.txtRepetirContrasenaUsuarioNuevo.text.toString()
        val email = binding.txtEmailUsuarioNuevo.text.toString().trim().lowercase()

        Log.d("UsuarioNuevo", "🚀 === REGISTRO DEFINITIVO ===")
        Log.d("UsuarioNuevo", "📧 Email: '$email'")
        Log.d("UsuarioNuevo", "👤 Nombre: '$nombreCompleto'")

        // Validaciones básicas
        when {
            nombreCompleto.isEmpty() -> {
                binding.txtNombreCompletoUsuarioNuevo.error = "Debe suministrar el nombre completo"
                return
            }
            email.isEmpty() -> {
                binding.txtEmailUsuarioNuevo.error = "Debe suministrar un email"
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.txtEmailUsuarioNuevo.error = "Email no válido"
                return
            }
            contrasena.isEmpty() || contrasena1.isEmpty() -> {
                binding.txtContrasenaUsuarioNuevo.error = "Las contraseñas no deben estar vacías"
                return
            }
            contrasena != contrasena1 -> {
                binding.txtContrasenaUsuarioNuevo.error = "Las contraseñas no coinciden"
                return
            }
            contrasena.length <= 5 -> {
                binding.txtContrasenaUsuarioNuevo.error = "La contraseña debe ser mínimo de 6 caracteres"
                return
            }
            else -> {
                lifecycleScope.launch {
                    binding.btnEnviarSolicitudUsuarioNuevo.isEnabled=false
                    iniciarProcesamiento()

                    configurarTimeoutLargo("verificación")
                    var find= dalUsu.getUsuarioByEmail(email)

                    cancelarTimeout()
                    if (find != null) {
                        finalizarProcesamiento()

                        bllUtil.MessageShow(
                            this@usuario_nuevo_activity,
                            "El correo ya existe en el sistema",
                            "Email Existente"
                        ) {}
                        binding.txtEmailUsuarioNuevo.requestFocus()
                    } else {
                        //Log.d("UsuarioNuevo", "✅ Email disponible - Procediendo con registro...")
                        procederConRegistroDefinitivo(nombreCompleto, email, contrasena)
                    }

                    binding.btnEnviarSolicitudUsuarioNuevo.isEnabled=true
                }
            }
        }
    }

    private fun procederConRegistroDefinitivo(nombreCompleto: String, email: String, contrasena: String) {
        val usuario = Usuario(
            IdEmpresa = idEmpresaParaUsuario,
            NombreCompleto = nombreCompleto,
            IdRol = 2,
            Email = email,
            CuentaVerificada = false,
            Contrasena = contrasena,
            Activo = true,
            Bloqueado = false,
            NombreUsuario = "",
            FechaCuentaVerificada = "",
            Domicilio = ""
        )

        // Timeout de 20 segundos para insert
        configurarTimeoutLargo("registro")
        lifecycleScope.launch {
            var idUsuario:Int?=dalUsu.addUsuario(usuario)

            cancelarTimeout()
            finalizarProcesamiento()

            if (idUsuario!=null && idUsuario>0) {
                bllUtil.MessageShow(
                    this@usuario_nuevo_activity,
                    "¡Usuario registrado exitosamente!\n\nSu cuenta será verificada por un administrador.",
                    "¡Éxito!"
                ) {
                    navegarALogin()
                }
            } else {
                bllUtil.MessageShow(
                    this@usuario_nuevo_activity,
                    "Error al registrar usuario.\n\nPor favor, verifique su conexión a internet e intente nuevamente.",
                    "Error de Registro"
                ) {}
            }
        }
    }

    private fun iniciarProcesamiento() {
        isProcessing = true
        binding.btnEnviarSolicitudUsuarioNuevo.isEnabled = false
        binding.btnEnviarSolicitudUsuarioNuevo.text = "PROCESANDO..."
        Log.d("UsuarioNuevo", "⏳ Procesamiento iniciado")
    }

    private fun finalizarProcesamiento() {
        isProcessing = false
        binding.btnEnviarSolicitudUsuarioNuevo.isEnabled = true
        binding.btnEnviarSolicitudUsuarioNuevo.text = "REGISTRARSE"
        Log.d("UsuarioNuevo", "✅ Procesamiento finalizado")
    }

    private fun configurarTimeoutLargo(operacion: String) {
        timeoutHandler = Handler(Looper.getMainLooper())
        timeoutHandler?.postDelayed({
            Log.w("UsuarioNuevo", "⏰ Timeout de $operacion (15 segundos)")

            if (isProcessing) {
                finalizarProcesamiento()
                bllUtil.MessageShow(
                    this@usuario_nuevo_activity,
                    "La operación está tardando más de lo esperado.\n\nPor favor, verifique su conexión a internet e intente nuevamente.",
                    "Conexión Lenta"
                ) {}
            }
        }, 15000) // 15 segundos
    }

    private fun cancelarTimeout() {
        timeoutHandler?.removeCallbacksAndMessages(null)
        timeoutHandler = null
        Log.d("UsuarioNuevo", "⏰ Timeout cancelado")
    }

    private fun cierraPantalla() {
        cancelarTimeout()
        finish()
    }

    private fun navegarALogin() {
        try {
            val intent = Intent(this, acceso_activity::class.java) //me dio erro y tuve que agregar r
            // Limpiar el stack de actividades para que no pueda regresar

            var email=binding.txtEmailUsuarioNuevo.text

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("LimpiarCampos",true)
            intent.putExtra("EmailNuevo",email)
            startActivity(intent)
            finish() // Cerrar la actividad actual
        } catch (e: Exception) {
            Log.e("UsuarioNuevo", "Error navegando a login: ${e.message}")
            // Si falla la navegación, cerrar la pantalla actual
            cierraPantalla()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelarTimeout()
    }
}
