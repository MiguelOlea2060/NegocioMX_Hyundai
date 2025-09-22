package com.example.negociomx_hyundai

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.negociomx_hyundai.BE.UsuarioNube
import com.example.negociomx_hyundai.DAL.DALDispotivioAcceso
import com.example.negociomx_hyundai.DAL.DALUsuario
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.Utils.negociomx_posApplication.Companion.prefs
import com.example.negociomx_hyundai.databinding.ActivityAccesoBinding
import com.example.negociomx_hyundai.room.BLL.BLLUtil
import com.example.negociomx_hyundai.room.db.POSDatabase
import com.google.firebase.auth.FirebaseAuth

// sql server login
import com.example.negociomx_hyundai.DAL.DALUsuarioSQL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class acceso_activity : AppCompatActivity() {
    lateinit var binding: ActivityAccesoBinding
    lateinit var dal:DALDispotivioAcceso
    lateinit var dalUsuSQL:DALUsuarioSQL

    lateinit var base: POSDatabase
    lateinit var bllUtil: BLLUtil

    private var loginInProgress = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private  val  startForResult=
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {resul->
            if(resul.resultCode== Activity.RESULT_OK)
            {
                var intent=resul.data
                var cerrarSesion= intent?.getBooleanExtra("cerrarSesion",false)!!
                if(cerrarSesion) {
                    finishAffinity()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityAccesoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("AccesoActivity", "🚀 INICIANDO ACTIVIDAD DE ACCESO")

        base = POSDatabase.getDatabase(applicationContext)
        dalUsuSQL=DALUsuarioSQL()
        bllUtil=BLLUtil()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_PHONE_STATE)) {
            } else { ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_STATE), 2) } }

        dal=DALDispotivioAcceso()

        var recordarAcceso=prefs.getRecordarAcceso()
        var usernameGuardado:String= prefs.getUsername()
        var pwdGuardado= prefs.getPassword()

        if(recordarAcceso)
        {
            binding.chkRecordarAcceso.isChecked=recordarAcceso
            binding.txtUsuarioEmailAcceso.setText(usernameGuardado)
            binding.txtContrasenaAcceso.setText(pwdGuardado)
        }

        apply {
            binding.lblRegistrarUsuarioAcceso.setOnClickListener{
                registrarUsuarioNuevo()
            }
            binding.btnIngresarAcceso.setOnClickListener {
                if (loginInProgress) {
                    Log.w("AccesoActivity", "⚠️ Login ya en progreso, ignorando clic")
                    return@setOnClickListener
                }

                Log.d("AccesoActivity", "🔘 BOTÓN INGRESAR PRESIONADO")

                var nombreUsuarioEmail=binding.txtUsuarioEmailAcceso.text.toString()
                var pwd =binding. txtContrasenaAcceso.text.toString()

                Log.d("AccesoActivity", "📧 Email ingresado: '$nombreUsuarioEmail'")
                Log.d("AccesoActivity", "🔐 Password ingresado: '${if(pwd.isNotEmpty()) "***" else "VACÍO"}'")

                if (nombreUsuarioEmail.isEmpty() == true) {
                    Log.w("AccesoActivity", "⚠️ Email vacío")
                    binding. txtUsuarioEmailAcceso.error="Es necesario suministrar el nombre de Usuario o Email"
                } else if (pwd.isEmpty() == true) {
                    Log.w("AccesoActivity", "⚠️ Password vacío")
                    binding. txtContrasenaAcceso.error="Es necesario suministrar la contraseña"
                } else {
                    Log.d("AccesoActivity", "✅ Datos válidos, iniciando proceso de login")

                    // MARCAR LOGIN EN PROGRESO
                    loginInProgress = true

                    // DESHABILITAR BOTÓN PARA EVITAR MÚLTIPLES CLICS
                    binding.btnIngresarAcceso.isEnabled = false
                    binding.btnIngresarAcceso.text = "Ingresando..."

                    // ✅ TIMEOUT GLOBAL EXTENDIDO A 60 SEGUNDOS
                    val loginTimeoutRunnable = Runnable {
                        Log.e("AccesoActivity", "⏰ TIMEOUT GLOBAL: Login tardó más de 60 segundos")
                        resetLoginUI()
                        bllUtil.MessageShow(this, "Tiempo de espera agotado. Verifique su conexión a internet y que Firebase esté configurado correctamente.", "Error") { res -> }
                    }

                    mainHandler.postDelayed(loginTimeoutRunnable, 60000) // 60 segundos

                    loguearUsuario(nombreUsuarioEmail, pwd) { usuarioLogueado ->
                        // Cancelar timeout global
                        mainHandler.removeCallbacks(loginTimeoutRunnable)

                        Log.d("AccesoActivity", "🏁 Resultado login: $usuarioLogueado")

                        if (usuarioLogueado == true) {
                            Log.d("AccesoActivity", "🎉 LOGIN EXITOSO")
                            prefs.saveRecordarAcceso(binding.chkRecordarAcceso.isChecked)
                            prefs.saveUsername(nombreUsuarioEmail)
                            prefs.savePassword(pwd)

                            mainHandler.post {
                                val intent = Intent(applicationContext, menu_principal_activity::class.java)
                                startForResult.launch(intent)
                                resetLoginUI()
                            }
                        } else {
                            Log.e("AccesoActivity", "❌ LOGIN FALLIDO")
                            mainHandler.post {
                                resetLoginUI()
                                bllUtil.MessageShow(this, "El usuario o contraseña son incorrectas, o hay un problema de conectividad con Firebase.", "Aviso") { res -> }
                            }
                        }
                    }
                }
            }

            var limpiarControles:Boolean = false
            var emailNuevo:String=""
            if (intent?.extras != null) {
                limpiarControles = intent.extras?.getBoolean("LimpiarCampos", false)?:false
                emailNuevo= intent.extras?.getString("EmailNuevo", "") ?: ""
            }

            if(limpiarControles==true && emailNuevo.isNotEmpty())
            {
                binding.chkRecordarAcceso.isChecked=false
                binding.txtContrasenaAcceso.setText("")
                binding.txtUsuarioEmailAcceso.setText(emailNuevo)

                binding.txtContrasenaAcceso.requestFocus()
            }
        }
    }

    private fun resetLoginUI() {
        loginInProgress = false
        binding.btnIngresarAcceso.isEnabled = true
        binding.btnIngresarAcceso.text = "Ingresar"
    }

    private fun registrarUsuarioNuevo() {
        val intent= Intent(this,usuario_nuevo_activity::class.java)
        intent.putExtra("IdEmpresaParaUsuario",12)// Para Patio Zacatula
        startActivity(intent)
    }

    // REEMPLAZAR toda la función loguearUsuario() por esta:
    fun loguearUsuario(email:String, pwd:String, onLoginFinish: (Boolean) -> Unit) {
        Log.d("AccesoActivity", "🔐 INICIANDO LOGIN CON SQL SERVER")
        Log.d("AccesoActivity", "📧 Email: '$email'")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val usuario = dalUsuSQL.getUsuarioByEmailAndPassword(email, pwd)

                if (usuario != null) {
                    Log.d("AccesoActivity", "✅ Usuario encontrado en SQL Server")
                    Log.d("AccesoActivity", "👤 Usuario: ${usuario.Email}")
                    Log.d("AccesoActivity", "🟢 Activo: ${usuario.Activo}")
                    Log.d("AccesoActivity", "✅ Verificado: ${usuario.CuentaVerificada}")

                    // Validar estado del usuario
                    if (usuario.CuentaVerificada != true) {
                        Log.w("AccesoActivity", "⚠️ Cuenta no verificada")
                        bllUtil.MessageShow(this@acceso_activity, "La cuenta no se encuentra verificada. Comunicarse con el Administrador", "Aviso") { res -> }
                        onLoginFinish(false)
                        return@launch
                    }

                    if (usuario.Activo != true) {
                        Log.w("AccesoActivity", "⚠️ Cuenta no activa")
                        bllUtil.MessageShow(this@acceso_activity, "La cuenta no se encuentra Activa. Comunicarse con el Administrador", "Aviso") { res -> }
                        onLoginFinish(false)
                        return@launch
                    }

                    // Configurar usuario logueado
                    ParametrosSistema.usuarioLogueado = usuario
                    Log.d("AccesoActivity", "✅ Login exitoso")
                    onLoginFinish(true)

                } else {
                    Log.e("AccesoActivity", "❌ Credenciales incorrectas")
                    bllUtil.MessageShow(this@acceso_activity, "El usuario o contraseña son incorrectas", "Aviso") { res -> }
                    onLoginFinish(false)
                }

            } catch (e: Exception) {
                Log.e("AccesoActivity", "💥 Error en login: ${e.message}")
                bllUtil.MessageShow(this@acceso_activity, "Error de conexión: ${e.message}", "Error") { res -> }
                onLoginFinish(false)
            }
        }
    }
}
