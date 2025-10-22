package com.example.negociomx_hyundai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.negociomx_hyundai.databinding.ActivityMenucatalogosBinding

class menucatalogos_activity : AppCompatActivity() {
    lateinit var binding:ActivityMenucatalogosBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMenucatalogosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegresarCatalogos.setOnClickListener {
            finish()
        }
    }
}