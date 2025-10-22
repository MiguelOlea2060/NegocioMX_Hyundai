package com.example.negociomx_hyundai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.negociomx_hyundai.canvas.MyCanvas
import com.example.negociomx_hyundai.databinding.ActivityInventariovehiculosBinding

class inventariovehiculos_activity : AppCompatActivity() {
    lateinit var binding:ActivityInventariovehiculosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val canvas=MyCanvas(this)
        setContentView(canvas)
        /*binding=ActivityInventariovehiculosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresarInventario.setOnClickListener {
            finish()
        }*/
    }
}