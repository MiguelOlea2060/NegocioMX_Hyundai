package com.example.negociomx_hyundai

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.Hyundai.VehiculoA
import com.example.negociomx_hyundai.Utils.CargaMasivaVins
import com.example.negociomx_hyundai.Utils.ParametrosSistema
import com.example.negociomx_hyundai.databinding.ActivityCargaMasivaVinsBinding
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.dump.InvalidFormatException
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.IOException


class CargaMasivaVinsActivity : AppCompatActivity() {
    lateinit var binding:ActivityCargaMasivaVinsBinding

    var lista:MutableList<VehiculoA>?=null

    private var loadingHandler: Handler? = null
    private var loadingRunnable: Runnable? = null

    private lateinit var cargaMasiva: CargaMasivaVins
    private var dialogoProgreso: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityCargaMasivaVinsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnExaminarArchivosXls.setOnClickListener{
            val mimeTypes =
                arrayOf(
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",  // .xls & .xlsx
                )

            var mimeTypesStr = ""
            for (mimeType in mimeTypes) {
                mimeTypesStr += "$mimeType|"
            }
            val intent=Intent().setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent,"selecciona el excel"),
                111)
        }
        binding.btnGuardarCargaMasiva.setOnClickListener {
            if(lista!=null && lista!!.count()>0)
                iniciarDescargaFotos(lista!!)
            else
            {

            }
        }
        binding.btnRegresarCargaMasiva.setOnClickListener {
            finish()
        }

        cargaMasiva = CargaMasivaVins(this)
    }

    private fun ocultarDialogoProgreso() {
        dialogoProgreso?.let { dialogo ->
            if (dialogo.isShowing) {
                dialogo.dismiss()
            }
        }
        dialogoProgreso = null
    }

    private fun actualizarDialogoProgreso(titulo: String, subtitulo: String) {
        dialogoProgreso?.let { dialogo ->
            if (dialogo.isShowing) {
                dialogo.findViewById<TextView>(R.id.tvTituloProgreso)?.text = titulo
                dialogo.findViewById<TextView>(R.id.tvSubtituloProgreso)?.text = subtitulo
            }
        }
    }

    private fun mostrarDialogoProgreso(titulo: String, subtitulo: String) {
        if (dialogoProgreso?.isShowing == true) {
            dialogoProgreso?.dismiss()
        }

        dialogoProgreso = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_progreso_descarga)
            setCancelable(false)

            findViewById<TextView>(R.id.tvTituloProgreso).text = titulo
            findViewById<TextView>(R.id.tvSubtituloProgreso).text = subtitulo

            show()
        }
    }

    private fun iniciarDescargaFotos(registros: MutableList<VehiculoA>) {
        var maxPaquete=0
        if(binding.etNumRegistrosBloqueCarga.text.toString().isNotEmpty())
            maxPaquete=binding.etNumRegistrosBloqueCarga.text.toString().toInt()
        if (registros==null || registros.count() == 0) {
            Toast.makeText(this, "No existen Vins para la Carga masiva", Toast.LENGTH_SHORT).show()
            return
        }
        else if (maxPaquete<=0) {
            Toast.makeText(this, "Debe suministrar # registros x Bloque validos.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("CargaMasivaVINs", "🚀 Iniciando carga masiva de INs: ${registros.count()}")
        lifecycleScope.launch {
            mostrarDialogoProgreso("Preparando descarga...", "Iniciando proceso")

            cargaMasiva.descargarFotosVehiculo(registros,maxPaquete,
                onProgress = { titulo, subtitulo ->
                    runOnUiThread {
                        actualizarDialogoProgreso(titulo, subtitulo)
                    }
                },
                onComplete = { exito, mensaje ->
                    runOnUiThread {
                        ocultarDialogoProgreso()

                        if (exito) {
                            Toast.makeText(this@CargaMasivaVinsActivity, "✅ Carga masiva completada", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CargaMasivaVinsActivity, "❌ Error en la carga masiva de VINs", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    private fun readExcelFile(uri: Uri) {
        try {
            val inputStream=contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet: Sheet = workbook.getSheetAt(0) // Assuming you want the first sheet

            val numColVin=binding.etCeldaVinCarga.text.toString().toInt()
            val numColModelo=binding.etCeldaModeloCarga.text.toString().toInt()
            val numColEspec=binding.etCeldaEspecificacionesCarga.text.toString().toInt()

            lista= mutableListOf()
            var posicion=binding.etPosInicialCarga.text.toString().toInt()
            var vin:String=""
            var modelo:String=""
            var especificaciones:String=""

            var idMarcaAuto:Int=ParametrosSistema.marcaAutoPre.IdMarcaAuto
            var columna:Int=0
            var fila:Int=0
            // Iterate through rows and cells
            val rowIterator: Iterator<Row> = sheet.iterator()
            while (rowIterator.hasNext()) {
                val row: Row = rowIterator.next()
                val cellIterator: Iterator<Cell> = row.cellIterator()

                vin=""
                modelo=""
                especificaciones=""
                columna=0
                while (cellIterator.hasNext()) {
                    var celda=cellIterator.next()
                    if(columna==numColVin-1)
                    {
                        vin=celda.toString()
                    }
                    else if(columna==numColModelo-1)
                    {
                        modelo=celda.toString()
                    }
                    else if(columna==numColEspec-1)
                    {
                        especificaciones=celda.toString()
                    }
                    columna++
                }
                if(vin.isNotEmpty() && vin.length>15)
                {
                    val v=VehiculoA(VIN = vin,
                        Especificaciones = especificaciones,
                        IdMarca = idMarcaAuto,
                        IdModelo = 0,
                        Anio = 0,
                        IdVehiculo = -1,
                        Modelo = modelo)
                    lista?.add(v)
                }
                fila++
            }
            workbook.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InvalidFormatException) {
            e.printStackTrace()
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mostrarCargaConsulta() {
        binding.loadingContainerCarga.visibility = View.VISIBLE
        binding.btnExaminarArchivosXls.isEnabled = false
        binding.btnExaminarArchivosXls.alpha = 0.5f

        binding.tvLoadingTextCarga.text = "Leyendo registros de archivo..."
        binding.tvLoadingSubtextCarga.text = "Verificando información de VINs\""
    }

    private fun ocultarCargaConsulta() {
        binding. loadingContainerCarga.visibility = View.GONE
        binding.btnExaminarArchivosXls.isEnabled = true
        binding.btnExaminarArchivosXls.alpha = 1.0f

        // Limpiar handlers
        loadingHandler?.removeCallbacks(loadingRunnable!!)
        loadingHandler = null
        loadingRunnable = null
    }

    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode != Activity.RESULT_OK) return
        when(requestCode) {
            111 -> {
                data?.data.let {
                    uri->
                    mostrarCargaConsulta()

                    lifecycleScope.launch {
                        var totalRegistros = 0
                        try {
                            readExcelFile(uri!!)

                            ocultarCargaConsulta()
                            if (lista != null && lista?.count()!! > 0) totalRegistros =
                                lista!!.count()
                        }
                        catch (ex:Exception)
                        {
                            var cadena=ex.message.toString()
                            cadena+=""
                        }
                        binding.lblDetallesArchivoCarga.setText("Registros leidos de archivo: ${totalRegistros}")
                        binding.btnGuardarCargaMasiva.isEnabled=lista!=null && lista!!.count()>0
                    }
                }
            }
            // Other result codes
            else -> {}
        }
    }

}