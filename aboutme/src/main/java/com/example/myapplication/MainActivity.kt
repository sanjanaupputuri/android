package com.example.myapplication


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var profileImage: CircleImageView
    private lateinit var editPhotoButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_me)

        profileImage = findViewById(R.id.profileImage)
        editPhotoButton = findViewById(R.id.editPhotoButton)

        editPhotoButton.setOnClickListener {
            showPhotoOptionsDialog()
        }

        profileImage.setOnClickListener {
            showPhotoOptionsDialog()
        }

        findViewById<Button>(R.id.button3).setOnClickListener {
            openUrl("https://leetcode.com/u/upputurisanjana/")
        }

        findViewById<Button>(R.id.button1).setOnClickListener {
            openUrl("https://www.linkedin.com/in/sanjana-upputuri-73b47b349/")
        }

        findViewById<Button>(R.id.button2).setOnClickListener {
            openUrl("https://github.com/24WH1A05Z3")
        }

        findViewById<Button>(R.id.downloadCVButton).setOnClickListener {
            if (checkPermission()) {
                saveResumeToDownloads()
            } else {
                requestPermission()
            }
        }
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Change Photo", "Remove Photo", "Cancel")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Profile Photo")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openGalleryForImage()
                1 -> removeProfilePhoto()
            }
        }
        builder.show()
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun removeProfilePhoto() {
        profileImage.setImageResource(R.drawable.default_profile)
        Toast.makeText(this, "Profile photo removed", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri? = data.data
            profileImage.setImageURI(imageUri)
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveResumeToDownloads()
            } else {
                Toast.makeText(this, "Permission denied to write to storage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveResumeToDownloads() {
        try {
            val inputStream = resources.openRawResource(R.raw.resumeapp)

            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outFile = File(downloadsFolder, "My_Resume.pdf")
            val outputStream = FileOutputStream(outFile)

            val buffer = ByteArray(1024)
            var length = inputStream.read(buffer)
            while (length > 0) {
                outputStream.write(buffer, 0, length)
                length = inputStream.read(buffer)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Toast.makeText(this, "Resume downloaded to Downloads folder", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to download resume", Toast.LENGTH_SHORT).show()
        }
    }
}
