package com.example.negociomx_hyundai

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.Orientation
import com.example.negociomx_hyundai.BE.Bloque
import com.example.negociomx_hyundai.BE.PosicionBloque
import com.example.negociomx_hyundai.DAL.DALPasoLogVehiculo
import com.example.negociomx_hyundai.DAL.DALVehiculo
import com.example.negociomx_hyundai.databinding.ActivityUbicacionVehiculosBinding
import kotlinx.coroutines.launch

class UbicacionVehiculosActivity : AppCompatActivity() {
    lateinit var binding:ActivityUbicacionVehiculosBinding

    var listaBloques:List<Bloque>?=null

    var dalBloque:DALPasoLogVehiculo?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityUbicacionVehiculosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dalBloque= DALPasoLogVehiculo()

        leeBloquesYPosiciones()
    }

    private fun leeBloquesYPosiciones() {
        lifecycleScope.launch {
            listaBloques = dalBloque?.consultarBloques()

            if(listaBloques!=null)
            {
                var salir=false
                var columna:Short=0
                var fila:Short=0
                var bloque= listaBloques!![0]

                var altoLinea:Int=140
                var altoAnchoBtn:Int=100
                var xInicial:Float=10F
                var x:Float=xInicial
                var y:Float=10F
                var anchoX:Float=55F
                var altoY:Float=50F
                var incrementoX:Float=5F
                var incrementoY:Float=5F

                do {
                    var linea:LinearLayout=LinearLayout(this@UbicacionVehiculosActivity)
                    linea.orientation=LinearLayout.HORIZONTAL
                    linea.y=y
                    linea.layoutParams=LinearLayout.LayoutParams(900,120,)
                    binding.mainLayout.addView(linea)
                    do{
                        var ocupada=bloque.Ocupadas?.filter { it.NumColumna==columna && it.NumFila==fila }!!.firstOrNull()!=null

                        val imgPosicion=ImageView(this@UbicacionVehiculosActivity)
                        imgPosicion.layoutParams=LinearLayout.LayoutParams(altoAnchoBtn,altoAnchoBtn)
                        imgPosicion.x=x
                        imgPosicion.y=y
                        if(ocupada)
                        {
                            imgPosicion.setImageResource(R.drawable.posicionocupada_32px)
                        }
                        else
                        {
                            imgPosicion.setImageResource(R.drawable.posicionlibre_32px)

                        }

                        linea.addView(imgPosicion)
                        x+=anchoX+incrementoX

                        fila++
                        linea.addView(imgPosicion)
                    }while (fila<bloque.NumFilas)
                    columna++
                    fila=0

                    x=xInicial
                    y+=altoLinea
                }while (columna<bloque.NumColumnas)

            }
        }
    }
}