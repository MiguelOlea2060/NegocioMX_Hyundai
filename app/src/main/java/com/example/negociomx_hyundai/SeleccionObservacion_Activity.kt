package com.example.negociomx_hyundai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.negociomx_hyundai.databinding.ActivitySeleccionObservacionBinding

class SeleccionObservacion_Activity : AppCompatActivity() {
    lateinit var binding:ActivitySeleccionObservacionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySeleccionObservacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}