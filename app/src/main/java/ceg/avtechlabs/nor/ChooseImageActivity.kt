package ceg.avtechlabs.nor

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.system.ErrnoException
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_choose_image.*

import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.ArrayList

class ChooseImageActivity : AppCompatActivity() {
    private var CropImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_image)
    }

    fun onLoadImageClick(view: View) {
        startActivityForResult(pickImageChooserIntent, 200)
    }

    /**
     * Crop the image and set it back to the  cropping view.
     */
    fun onCropImageClick(view: View) {
        val cropped = CropImageView.getCroppedImage(500, 500)
        if (cropped != null) {
            CropImageView.setImageBitmap(cropped)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(IMAGE_URI, cropped)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Unknown error occured. Try again.", Toast.LENGTH_LONG).show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val imageUri = getPickImageResultUri(data)

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            var requirePermissions = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    isUriRequiresPermissions(imageUri)) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true
                CropImageUri = imageUri
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
            }
            Toast.makeText(this, imageUri.toString(), Toast.LENGTH_LONG).show()

            if (!requirePermissions) {
                CropImageView.setImageUriAsync(imageUri)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (CropImageUri != null && grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            CropImageView.setImageUriAsync(CropImageUri)
        } else {
            Toast.makeText(this, "Required permissions are not granted", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Create a chooser intent to select the  source to get image from.<br></br>
     * The source can be camera's  (ACTION_IMAGE_CAPTURE) or gallery's (ACTION_GET_CONTENT).<br></br>
     * All possible sources are added to the  intent chooser.
     */
    // Determine Uri of camera image to  save.
    // collect all camera intents
    // collect all gallery intents
    // the main intent is the last in the  list (fucking android) so pickup the useless one
    // Create a chooser from the main  intent
    // Add all other intents
    val pickImageChooserIntent: Intent
        get() {
            val outputFileUri = captureImageOutputUri

            val allIntents = ArrayList<Intent>()
            val packageManager = packageManager
            val captureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            val listCam = packageManager.queryIntentActivities(captureIntent, 0)
            for (res in listCam) {
                val intent = Intent(captureIntent)
                intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                intent.`package` = res.activityInfo.packageName
                if (outputFileUri != null) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
                }
                allIntents.add(intent)
            }
            val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
            galleryIntent.type = "image/*"
            val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
            for (res in listGallery) {
                val intent = Intent(galleryIntent)
                intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
                intent.`package` = res.activityInfo.packageName
                allIntents.add(intent)
            }
            var mainIntent = allIntents[allIntents.size - 1]
            for (intent in allIntents) {
                if (intent.component.className == "com.android.documentsui.DocumentsActivity") {
                    mainIntent = intent
                    break
                }
            }
            allIntents.remove(mainIntent)
            val chooserIntent = Intent.createChooser(mainIntent, "Select source")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>())

            return chooserIntent
        }

    /**
     * Get URI to image received from capture  by camera.
     */
    private val captureImageOutputUri: Uri?
        get() {
            var outputFileUri: Uri? = null
            val getImage = externalCacheDir
            if (getImage != null) {
                outputFileUri = Uri.fromFile(File(getImage.path, "pickImageResult.jpeg"))
            }
            return outputFileUri
        }

    /**
     * Get the URI of the selected image from  [.getPickImageChooserIntent].<br></br>
     * Will return the correct URI for camera  and gallery image.

     * @param data the returned data of the  activity result
     */
    fun getPickImageResultUri(data: Intent?): Uri {
        var isCamera = true
        if (data != null && data.data != null) {
            val action = data.action
            isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
        }
        return if (isCamera) captureImageOutputUri!! else data!!.data
    }

    /**
     * Test if we can open the given Android URI to test if permission required error is thrown.<br></br>
     */
    fun isUriRequiresPermissions(uri: Uri): Boolean {
        try {
            val resolver = contentResolver
            val stream = resolver.openInputStream(uri)
            stream!!.close()
            return false
        } catch (e: FileNotFoundException) {
            @TargetApi(21)
            if (e.cause is ErrnoException) {
                return true
            }
        } catch (e: Exception) {
        }

        return false
    }

    companion object {
        val IMAGE_URI = "imageuri"
    }
}
