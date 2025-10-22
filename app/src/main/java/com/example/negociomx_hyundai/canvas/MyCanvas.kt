package com.example.negociomx_hyundai.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.negociomx_hyundai.R


class MyCanvas(context:Context) :View(context) {
    private val backColor=ResourcesCompat.getColor(resources, R.color.black, null)
    private lateinit var canvas1:Canvas
    private lateinit var bmp:Bitmap

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        //bmp=Bitmap.createBitmap(R.drawable.fotodanno_frente_original,w,h,Bitmap.Config.ARGB_8888)
    }

}