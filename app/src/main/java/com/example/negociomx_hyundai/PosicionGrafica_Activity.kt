package com.example.negociomx_hyundai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.Bloque
import com.example.negociomx_hyundai.BE.PosicionBloque
import com.example.negociomx_hyundai.BLL.BLLBloque
import com.example.negociomx_hyundai.databinding.ActivityPosicionGraficaBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class PosicionGrafica_Activity : AppCompatActivity() {

    lateinit var binding:ActivityPosicionGraficaBinding

    private lateinit var gridLayout: GridLayout
    private lateinit var tvInfo: TextView
    private var bloqueSeleccionado: Bloque? = null
    private var listaBloques:MutableList<Bloque>?=null
    private var posicionesDisponibles: List<PosicionBloque> = listOf()
    private val bllBloque = BLLBloque()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityPosicionGraficaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarComponentes()
        obtenerDatosIntent()
        //generarMatrizPosiciones()
    }

    private fun inicializarComponentes() {
        gridLayout = findViewById(R.id.gridLayoutPosiciones)
        //binding.tvTituloBloque = findViewById(R.id.tvTituloBloque)
        tvInfo = findViewById(R.id.tvInfoPosiciones)

        findViewById<ImageView>(R.id.btnRegresar).setOnClickListener {
            finish()
        }
    }

    private fun obtenerDatosIntent() {
        val gson=Gson()

        val jsonLista=intent.getStringExtra("bloques")?:""
        val listTutorialType = object : TypeToken<MutableList<Bloque>>() {}.type
        listaBloques=gson.fromJson(jsonLista,listTutorialType)

        if (listaBloques == null || listaBloques?.count()==0) {
            Toast.makeText(this, "Error: No se recibió información de los bloques", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarBloques()
    }

    private fun cargarBloques() {
        val nombresBloques = mutableListOf("Seleccionar bloque...")
        listaBloques?.forEach {
            nombresBloques.add(it.Nombre)
        }

        val adapter = ArrayAdapter(
            this@PosicionGrafica_Activity,
            android.R.layout.simple_spinner_item,
            nombresBloques
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBloqueGrafica.adapter = adapter

        binding.spinnerBloqueGrafica.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    bloqueSeleccionado=listaBloques!![position-1]
                    tvInfo.text = "Columnas: ${bloqueSeleccionado!!.NumColumnas} | Filas: ${bloqueSeleccionado!!.NumFilas}"

                    generarMatrizPosiciones()
                } else {
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }

    private fun generarMatrizPosiciones() {
        lifecycleScope.launch {
            try {
                // Obtener posiciones disponibles
                posicionesDisponibles = bllBloque.getPosicionesDisponiblesDeBloque(bloqueSeleccionado!!) ?: listOf()

                // Configurar grid
                gridLayout.removeAllViews()
                gridLayout.columnCount = bloqueSeleccionado!!.NumColumnas.toInt()

                // Generar matriz
                for (fila in 1..bloqueSeleccionado!!.NumFilas) {
                    for (columna in 1..bloqueSeleccionado!!.NumColumnas) {
                        val posicionView = crearVistaposicion(fila.toShort(), columna.toShort())
                        gridLayout.addView(posicionView)
                    }
                }

            } catch (e: Exception) {
                Log.e("PosicionGrafica", "Error generando matriz: ${e.message}")
                Toast.makeText(this@PosicionGrafica_Activity, "Error generando posiciones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun crearVistaposicion(fila: Short, columna: Short): View {
        val view = layoutInflater.inflate(R.layout.item_posicion_grafica, null)
        val imageView = view.findViewById<ImageView>(R.id.imgPosicion)
        val textView = view.findViewById<TextView>(R.id.tvPosicion)

        // Verificar si la posición está disponible
        val posicionDisponible = posicionesDisponibles.find {
            it.Fila == fila && it.Columna == columna
        }

        val textoposicion = "C$columna-F$fila"
        textView.text = textoposicion

        if (posicionDisponible != null) {
            // Posición libre
            imageView.setImageResource(R.drawable.posicionlibre_32px)
            view.setOnClickListener {
                seleccionarPosicion(posicionDisponible, textoposicion)
            }
            view.alpha = 1.0f
        } else {
            // Posición ocupada
            imageView.setImageResource(R.drawable.posicionocupada_32px)
            view.setOnClickListener(null)
            view.alpha = 0.6f
        }

        return view
    }

    private fun seleccionarPosicion(posicion: PosicionBloque, nombrePosicion: String) {
        val intent = Intent()
        intent.putExtra("posicion_seleccionada", posicion)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}