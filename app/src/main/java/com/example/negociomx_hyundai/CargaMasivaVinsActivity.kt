package com.example.negociomx_hyundai

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.lifecycle.lifecycleScope
import com.example.negociomx_hyundai.BE.Hyundai.VehiculoA
import com.example.negociomx_hyundai.databinding.ActivityCargaMasivaVinsBinding
import kotlinx.coroutines.launch
import org.apache.commons.compress.archivers.dump.InvalidFormatException
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class CargaMasivaVinsActivity : AppCompatActivity() {
    lateinit var binding:ActivityCargaMasivaVinsBinding

    var lista:MutableList<VehiculoA>?=null

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
    }

    private fun readExcelFile(uri: Uri) {
        try {
            val inputStream=contentResolver.openInputStream(uri)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet: Sheet = workbook.getSheetAt(0) // Assuming you want the first sheet

            // Iterate through rows and cells
            val rowIterator: Iterator<Row> = sheet.iterator()
            while (rowIterator.hasNext()) {
                val row: Row = rowIterator.next()
                val cellIterator: Iterator<Cell> = row.cellIterator()
                while (cellIterator.hasNext()) {
                    var celda=cellIterator.next()
                    if(celda.cellType.equals("STRING"))
                    {

                    }
                }
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
    @SuppressLint("MissingSuperCall")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode != Activity.RESULT_OK) return
        when(requestCode) {
            111 -> {
                data?.data.let {
                    uri->
                    lifecycleScope.launch {
                        readExcelFile(uri!!)
                    }
                }
            }
            // Other result codes
            else -> {}
        }
    }

}