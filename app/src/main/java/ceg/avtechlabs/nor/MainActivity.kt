package ceg.avtechlabs.nor

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.media.ExifInterface
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast

import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {
    internal var image: Bitmap? = null
    private var tess: TessBaseAPI? = null
    internal var datapath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        image = intent.getParcelableExtra(ChooseImageActivity.IMAGE_URI)
        imageView.setImageBitmap(image)
        datapath = filesDir.absolutePath + "/tesseract"

        checkFile(File(datapath + "/tessdata/"))
        val lang = "eng"

        tess = TessBaseAPI()
        tess?.init(datapath, lang)
    }

    private fun checkFile(dir: File) {
        if(!dir.exists() && dir.mkdirs())
            copyFiles()

        if(dir.exists()) {
            val path = datapath + "/tessdata/eng.traineddata"
            val data = File(path)
            if(!data.exists()) {
                copyFiles()
            }
        }
    }

    private fun copyFiles() {
        try {
            val path = datapath + "/tessdata/eng.traineddata"
            val input = assets.open("tessdata/eng.traineddata");
            val output = FileOutputStream(path)

            val buffer = ByteArray(1024)
            var read = 0
            while(true) {
                read = input.read(buffer)
                if (read == -1)
                    break
                output.write(buffer, 0, read)
            }

            output.flush()
            output.close()
            input.close()

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun processImage(v: View) {
        //val progress = ProgressDialog(this)
        //progress.setMessage("Extracting text from image ..")
        //progress.show()

        fixImageRotation()

        //progress.dismiss()
    }

    fun fixImageRotation() {
        //val exif = ExifInterface(resources.)

        //val path = resources.getIdentifier("test_image_2", "drawable", packageName)
        val bos = ByteArrayOutputStream()
        //image!!.compress(Bitmap.CompressFormat.PNG, 0, bos)
        val data = bos.toByteArray()
        val exif = ExifInterface(ByteArrayInputStream(data))
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        var rotate = 0

        Toast.makeText(this, orientation.toString(), Toast.LENGTH_LONG).show()
        when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
        }

        if(rotate != 0) {
            val w = image!!.width
            val h = image!!.height

            val matrix = Matrix()
            matrix.preRotate(rotate.toFloat())

            image = Bitmap.createBitmap(image, 0, 0, w, h, matrix, false)
            image = image!!.copy(Bitmap.Config.ARGB_8888, true)
            imageView.setImageBitmap(image)
            extractText()
        } else {
            val w = image!!.width
            val h = image!!.height

            val matrix = Matrix()
            matrix.preRotate(90.toFloat())

            image = Bitmap.createBitmap(image, 0, 0, w, h, matrix, false)
            image = image!!.copy(Bitmap.Config.ARGB_8888, true)
            imageView.setImageBitmap(image)

            val alert = AlertDialog.Builder(this)
                    .setMessage("Does the image needs to be rotated?")
                    .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            fixImageRotation()
                        }
                    })
                    .setNegativeButton("No", object: DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            dialog!!.dismiss()
                            extractText()
                        }
                    })
            alert.show()
        }

        //Toast.makeText(this, path, Toast.LENGTH_LONG).show()
    }

    fun extractText() {
        var result = ""
        tess?.setImage(image)
        result = tess!!.utF8Text
        //OCRTextView.text = result
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Enter the newspaper name from which pic was taken")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        dialog.setView(input)
        dialog.setPositiveButton("Search google", object: DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val paper = input.text.toString()
                if(paper.length == 0) {
                    Toast.makeText(applicationContext, "Paper name cannot be empty", Toast.LENGTH_LONG).show()
                    extractText()
                } else {
                    val url = "https://www.google.co.in/search?q=" + URLEncoder.encode(result + " " + paper, "UTF-8")
                    val intent  = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(url))
                    startActivity(intent)
                }

            }
        })
        dialog.show()
    }
}
