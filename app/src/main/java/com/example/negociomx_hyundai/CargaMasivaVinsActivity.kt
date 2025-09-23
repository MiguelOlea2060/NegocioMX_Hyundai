package com.example.negociomx_hyundai

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.negociomx_hyundai.databinding.ActivityCargaMasivaVinsBinding

class CargaMasivaVinsActivity : AppCompatActivity() {
    lateinit var binding:ActivityCargaMasivaVinsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityCargaMasivaVinsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}