package com.example.medicine

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity8 : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var productKey: String

    private lateinit var ivProductImage: ImageView
    private lateinit var etName: EditText
    private lateinit var etCategory: EditText
    private lateinit var etPrice: EditText
    private lateinit var etQty: EditText
    private lateinit var etDesc: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var currentImageBase64: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main8)

        productKey = intent.getStringExtra("productId") ?: ""

        if (productKey.isEmpty()) {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance().getReference("products")

        initViews()
        fetchProductData()

        btnSave.setOnClickListener {
            updateProduct()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {

        ivProductImage = findViewById(R.id.ivProductImage)
        etName = findViewById(R.id.etName)
        etCategory = findViewById(R.id.etCategory)
        etPrice = findViewById(R.id.etPrice)
        etQty = findViewById(R.id.etQty)
        etDesc = findViewById(R.id.etDesc)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun fetchProductData() {

        database.child(productKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    try {

                        val product = snapshot.getValue(Product::class.java)

                        if (product != null) {

                            etName.setText(product.name)
                            etCategory.setText(product.category)
                            etPrice.setText(product.price?.toString())
                            etQty.setText(product.stock?.toString())
                            etDesc.setText(product.description)

                            currentImageBase64 = product.image

                            product.image?.let {

                                val decoded = Base64.decode(it, Base64.DEFAULT)
                                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                ivProductImage.setImageBitmap(bmp)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity8, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateProduct() {

        val name = etName.text.toString().trim()
        val category = etCategory.text.toString().trim()
        val price = etPrice.text.toString().toDoubleOrNull()
        val stock = etQty.text.toString().toIntOrNull()
        val description = etDesc.text.toString().trim()

        if (name.isEmpty() || category.isEmpty() || price == null || stock == null || description.isEmpty()) {

            Toast.makeText(this, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedProduct = Product(
            name = name,
            category = category,
            price = price,
            stock = stock,
            description = description,
            image = currentImageBase64
        )

        database.child(productKey)
            .setValue(updatedProduct)
            .addOnSuccessListener {

                Toast.makeText(this, "Product Updated Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {

                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }
}