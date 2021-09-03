package com.lj.duplicateremover

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    var pdf : Uri?= null
    var export : PDDocument = PDDocument()
    var set = mutableSetOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()
        CoroutineScope(Dispatchers.Default).launch {
            PDFBoxResourceLoader.init(applicationContext)
        }
        button.setOnClickListener {
            accessFiles()
        }
    }

    private fun accessFiles(){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "application/pdf"
        startActivityForResult(Intent.createChooser(intent, "Select file"), 1);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)


        if(requestCode == 1 && resultCode == Activity.RESULT_OK && data != null ){
            pdf = data.data
            pdf?.let{
                    beginExtraction(it)
            }
        }
    }

    private fun beginExtraction(it: Uri) = CoroutineScope(Dispatchers.IO).launch{
        val inputStream: InputStream = contentResolver.openInputStream(it)!!
        Log.d("Finall", "load111")
        CoroutineScope(Dispatchers.Default).launch {
            val document = PDDocument.load(inputStream);
            Log.d("Finall", "load")

            extractText(document)
        }

    }


    fun extractText(document: PDDocument){

        progressBar.max = document.numberOfPages
        CoroutineScope(Dispatchers.Default).launch{
            val pdfStripper = PDFTextStripper()
            for (i in 0 until document.numberOfPages){
                pdfStripper.startPage = i
                pdfStripper.endPage = i + 1
                progressBar.progress = i
                val parsedText = pdfStripper.getText(document)
                if(!set.contains(parsedText)){
                    set.add(parsedText)
                    try {
                        Log.d("Finall", i.toString())
                        export.importPage(document.getPage(i))

                    }catch (e: Exception){
                        Log.d("Finall", "Add " + e.toString())
                    }
                }
            }
            try {
                val filename: String = "pissed.pdf"
                var directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

                val file: File = File(directory, "/$filename")

                export!!.save(file)
                withContext(Dispatchers.Main){
                    progressBar.visibility = View.INVISIBLE
                    Toast.makeText(this@MainActivity,"Success",Toast.LENGTH_SHORT).show()
                }
                Log.d("Finall", "Success  -" + directory)


            }catch (e: Exception){
                withContext(Dispatchers.Main){
                    progressBar.visibility = View.INVISIBLE
                    Snackbar.make(cl, "An unknown error occured", Snackbar.LENGTH_LONG)
                            .setAction("Try again") {
                                pdf?.let { beginExtraction(pdf!!) }
                            }
                            .show()
                }
                Log.d("Finall", "Export  " + e.toString())
            }

        }
    }



    var permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    private fun checkPermissions(): Boolean {
        var result: Int
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (p in permissions) {
            result = ContextCompat.checkSelfPermission(this, p)
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p)
            }
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 100)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
            return
        }
    }

}

