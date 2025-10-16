package com.example.negociomx_hyundai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.negociomx_hyundai.databinding.ActivityInventariovehiculosBinding

class inventariovehiculos_activity : AppCompatActivity() {
    lateinit var binding:ActivityInventariovehiculosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding=ActivityInventariovehiculosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresarInventario.setOnClickListener {
            finish()
        }
    }
}