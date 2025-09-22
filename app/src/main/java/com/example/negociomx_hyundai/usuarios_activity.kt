package com.example.negociomx_hyundai

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.negociomx_hyundai.BE.Usuario
import com.example.negociomx_hyundai.DAL.DALEmpresa
import com.example.negociomx_hyundai.DAL.DALUsuarioSQL
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.adapters.SpinnerAdapter
import com.example.negociomx_hyundai.adapters.UsuarioAdapter
import com.example.negociomx_hyundai.databinding.ActivityUsuariosBinding
import com.example.negociomx_hyundai.room.BLL.BLLUtil
import com.example.negociomx_hyundai.room.entities.Admins.Rol
import com.example.negociomx_hyundai.room.entities.ItemSpinner
import kotlinx.coroutines.launch

class usuarios_activity : AppCompatActivity() {

    lateinit var binding: ActivityUsuariosBinding
    lateinit var dalUsu: DALUsuarioSQL
    lateinit var dalEmp: DALEmpresa

    var listaUsuarios: List<Usuario>?=null

    lateinit var bllUtil: BLLUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dalUsu = DALUsuarioSQL()
        dalEmp = DALEmpresa()

        bllUtil = BLLUtil()

/*        if (ParametrosSistema.usuarioLogueado.IdEmpresa==null ||
            ParametrosSistema.usuarioLogueado.IdEmpresa?.toInt()==0)
            muestraEmpresasNube()*/

        var listaRoles: List<Rol>
        listaRoles = arrayListOf()

        listaRoles.add(Rol(IdRol = 0, Nombre = "Seleccione..."))
        listaRoles.add(Rol(IdRol = 2, Nombre = "Admin"))
        listaRoles.add(Rol(IdRol = 3, Nombre = "Supervisor"))
        listaRoles.add(Rol(IdRol = 4, Nombre = "Empleado"))
        listaRoles.add(Rol(IdRol = 5, Nombre = "Usuario Cliente"))

        var adapter: SpinnerAdapter
        adapter = bllUtil.convertListRolToListSpinner(this, listaRoles)

        val visibleEmpresa=ParametrosSistema.usuarioLogueado.IdEmpresa==null
                && ParametrosSistema.usuarioLogueado.IdEmpresa!!.toInt()>0

        binding.apply {
            cmbRolUsuarioUsuarios.adapter = adapter

            cmbEmpresaUsuarioUsuarios.isVisible=visibleEmpresa
            lblEmpresaUsuarioUsuarios.isVisible=visibleEmpresa

            progressAltaUsuario.isVisible = false
            chkActivoEmpresaNube.isChecked = true
            chkActivoEmpresaNube.isEnabled = false
            btnNuevoUsuarioAlta.isVisible = false
            btnRegresarUsuarios.setOnClickListener {
                finish()
            }
            btnGuardarUsuarioAlta.setOnClickListener {
                var nombreCompleto = txtNombreCompletoUsuarioUsuarios.text.toString()
                var contrasena = txtContrasenaUsuarios.text.toString()
                var contrasena1 = txtRepetirContrasenaUsuarios.text.toString()
                var email = txtEmailUsuarioUsuarios.text.toString()
                var idEmpresa: String? = null
                var selEmp = cmbEmpresaUsuarioUsuarios.selectedItem as ItemSpinner
                if (selEmp.Valor != 0) idEmpresa = selEmp.Valor.toString()

                var idRol: String? = null
                var selRol = cmbRolUsuarioUsuarios.selectedItem as ItemSpinner
                if (selRol.Valor != 0) idRol = selRol.Valor.toString()

                if (nombreCompleto.isEmpty() == true)
                    txtEmailUsuarioUsuarios.error = "Debe suministrar el Nombre completo"
                else if (email.isEmpty() == true)
                    txtEmailUsuarioUsuarios.error = "Debe suministrar un Email"
                else if (contrasena.isEmpty() || contrasena1.isEmpty())
                    txtContrasenaUsuarios.error = "La contraseñas no deben estar vacias"
                else if (!contrasena.equals(contrasena1))
                    txtContrasenaUsuarios.error = "Las contraseñas no coinciden"
                else if (contrasena.length <= 5)
                    txtContrasenaUsuarios.error = "La debe ser minimo de 6 catacteres"
                else if (idRol == null || idRol.isEmpty() == true)
                    lblRolUsuario.error = "Es necesario seleccionar un Rol"
                else if (idEmpresa == null || idEmpresa.isEmpty() == true)
                    lblEmpresaUsuarioUsuarios.error = "Es necesario seleccionar una Empresa"
                else {
                    lifecycleScope.launch {
                        var find= dalUsu.getUsuarioByEmail(email)
                        if (find != null) {
                            bllUtil.MessageShow(
                                this@usuarios_activity, "El correo ya existe en el Sistema",
                                "Aviso"
                            ) {}
                            txtEmailUsuarioUsuarios.requestFocus()
                        } else {
                            btnGuardarUsuarioAlta.isVisible = false

                            progressAltaUsuario.isVisible = true

                            var activo = chkActivoEmpresaNube.isChecked

                            var usuario = Usuario(
                                IdEmpresa = idEmpresa.toInt(),
                                IdRol = idRol.toInt(),
                                NombreCompleto = nombreCompleto,
                                Email = email,
                                CuentaVerificada = false,
                                Contrasena = contrasena,
                                Activo = activo
                            )
                            val id=dalUsu.addUsuario(usuario)
                        }
                    }

                }
            }
        }

        muestraListaUsuarios()
    }

    private fun muestraListaUsuarios() {
        var idEmpresa:Int?=null
        lifecycleScope.launch {
            if (ParametrosSistema.usuarioLogueado.IdEmpresa != null)
                idEmpresa = ParametrosSistema.usuarioLogueado.IdEmpresa!!.toInt()

            listaUsuarios = dalUsu.getUsuariosByEmpresa(idEmpresa)
            val adaptador = UsuarioAdapter(listaUsuarios) { usuario,opcion -> onItemSelected(usuario,opcion ) }

            binding.rvUsuarios.layoutManager = LinearLayoutManager(applicationContext)
            binding.rvUsuarios.adapter = adaptador
        }
    }

    private fun onItemSelected(usuario: Usuario,opcion:Int) {
        if(opcion==1)
        {

        }
        else if(opcion==2)
        {
            if (usuario.IdUsuario.toInt()==ParametrosSistema.usuarioLogueado.Id?.toInt())
            {
                bllUtil.MessageShow(
                    this,
                    "Aviso",
                    "No es posible modificar el usuario Logueado. Favor de verificarlo"
                ) { res ->
                }

                return
            }
            var cuentaVerificada=usuario.CuentaVerificada
            var mensaje="Desea Verificar el usuario: ${usuario.NombreCompleto} con email: " +
                    "${usuario.Email} ?"
            var tipo="verificado"
            if(cuentaVerificada) {
                tipo = "desverificado"
                mensaje="Desea Desverificar el usuario: ${usuario.NombreCompleto} con email: " +
                        "${usuario.Email} ?"
            }

            bllUtil.MessageShow(
                this,
                "Pregunta",
                mensaje
            ) { res ->
                if (res == 1) {
                    usuario.CuentaVerificada = !cuentaVerificada
                    lifecycleScope.launch {
                        var res = dalUsu.updateUsuario(usuario)
                        if (res) {
                            Toast.makeText(
                                this@usuarios_activity,
                                "El usuario: ${usuario.NombreCompleto} se ha ${tipo} correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        muestraListaUsuarios()
                    }
                }
            }
        }
        else if(opcion==3)
        {
            if (usuario.IdUsuario.toInt()==ParametrosSistema.usuarioLogueado.Id?.toInt())
            {
                bllUtil.MessageShow(
                    this,
                    "Aceptar",
                    "",
                    "No es posible modificar el usuario Logueado. Favor de verificarlo",
                    "Aviso",
                ) { res ->
                }

                return
            }

            var activado=usuario.Activo
            var mensaje="Desea Activar el usuario: ${usuario.NombreCompleto} con email: " +
                    "${usuario.Email} ?"
            var tipo="activado"
            if(activado) {
                tipo = "desactivdo"
                mensaje="Desea Desactivar el usuario: ${usuario.NombreCompleto} con email: " +
                        "${usuario.Email} ?"
            }

            bllUtil.MessageShow(
                this,
                "Pregunta",
                mensaje
            ) { res ->
                if (res == 1) {
                    usuario.Activo = !activado
                    lifecycleScope.launch {
                        var res = dalUsu.updateUsuario(usuario)
                        if (res) {
                            Toast.makeText(
                                this@usuarios_activity,
                                "El usuario: ${usuario.NombreCompleto} se ha ${tipo} correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        muestraListaUsuarios()
                    }
                }
            }

        }
    }

    private fun limpiaControles() {
        binding.apply {
            progressAltaUsuario.isVisible = false

            btnGuardarUsuarioAlta.isVisible = true
            txtNombreCompletoUsuarioUsuarios.text?.clear()
            txtEmailUsuarioUsuarios.text?.clear()
            txtNombreCompletoUsuarioUsuarios.text?.clear()
            txtNombreCompletoUsuarioUsuarios.text?.clear()
            txtContrasenaUsuarios.text?.clear()
            txtRepetirContrasenaUsuarios.text?.clear()

            if (cmbRolUsuarioUsuarios.count > 0) cmbRolUsuarioUsuarios.setSelection(0)
            if (cmbEmpresaUsuarioUsuarios.count > 0) cmbEmpresaUsuarioUsuarios.setSelection(0)

            txtNombreCompletoUsuarioUsuarios.requestFocus()
        }
    }

    private fun muestraEmpresasNube() {
        dalEmp.getByFilters(null) { lista ->
            runOnUiThread {
                var adapter = bllUtil.convertListEmpresaToListSpinner(this, lista!!)
                binding.cmbEmpresaUsuarioUsuarios.adapter = adapter
            }
        }
    }
}
