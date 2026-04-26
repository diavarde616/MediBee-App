package com.example.medicine

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class MainActivity6 : AppCompatActivity() {

    lateinit var etName: EditText
    lateinit var etPrice: EditText
    lateinit var etDescription: EditText
    lateinit var etQuantity: EditText
    lateinit var etCategory: EditText
    lateinit var imageView: ImageView
    lateinit var btnSelectImage: Button
    lateinit var btnSave: Button

    lateinit var imageUri: Uri
    val PICK_IMAGE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main6)

        etName = findViewById(R.id.et9)
        etPrice = findViewById(R.id.et10)
        etDescription = findViewById(R.id.et11)
        etQuantity = findViewById(R.id.et12)
        etCategory = findViewById(R.id.et13)

        imageView = findViewById(R.id.iv3)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnSave = findViewById(R.id.b6)

        btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        btnSave.setOnClickListener {

            if (!::imageUri.isInitialized) {
                Toast.makeText(this, "Please Select Image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = etName.text.toString().trim()
            val price = etPrice.text.toString().toDoubleOrNull()
            val description = etDescription.text.toString().trim()
            val quantity = etQuantity.text.toString().toIntOrNull()
            val category = etCategory.text.toString().trim()

            if (name.isEmpty() || price == null || description.isEmpty()
                || quantity == null || category.isEmpty()
            ) {
                Toast.makeText(this, "Fill all fields correctly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val encodedImage = encodeImage(imageUri)

            val dbRef = FirebaseDatabase.getInstance().getReference("products")
            val productId = dbRef.push().key!!

            val product = Product(
                name = name,
                category = category,
                price = price,
                stock = quantity,
                description = description,
                image = encodedImage
            )

            dbRef.child(productId).setValue(product)
                .addOnSuccessListener {
                    Toast.makeText(this, "Medicine Saved", Toast.LENGTH_SHORT).show()
                    clearFields()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Firebase Error: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data!!
            imageView.setImageURI(imageUri)
        }
    }

    private fun encodeImage(uri: Uri): String {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

        val stream = ByteArrayOutputStream()

        // ✅ Compress smaller → avoids huge DB size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream)

        val bytes = stream.toByteArray()

        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun clearFields() {
        etName.text.clear()
        etPrice.text.clear()
        etDescription.text.clear()
        etQuantity.text.clear()
        etCategory.text.clear()
        imageView.setImageResource(0)
    }
}
