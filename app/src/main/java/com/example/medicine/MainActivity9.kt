package com.example.medicine

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity9 : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var database: DatabaseReference

    private val products: MutableList<Product> = mutableListOf()
    private val productKeys: MutableList<String> = mutableListOf()

    private lateinit var adapter: ArrayAdapter<Product>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main9)

        listView = findViewById(R.id.listViewProducts)

        database = FirebaseDatabase.getInstance().getReference("products")

        setupAdapter()
        loadProducts()
    }

    private fun loadProducts() {

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                products.clear()
                productKeys.clear()

                for (snap in snapshot.children) {

                    try {

                        val product = snap.getValue(Product::class.java)

                        if (product != null) {
                            products.add(product)
                            productKeys.add(snap.key!!)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity9, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupAdapter() {

        adapter = object : ArrayAdapter<Product>(this, R.layout.edit, products) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                val view = convertView ?: layoutInflater.inflate(R.layout.edit, parent, false)

                val ivImage = view.findViewById<ImageView>(R.id.ivProduct)
                val tvName = view.findViewById<TextView>(R.id.tvProductName)
                val tvPrice = view.findViewById<TextView>(R.id.tvProductPrice)
                val btnDelete = view.findViewById<Button>(R.id.delete)
                val btnEdit = view.findViewById<Button>(R.id.edit)

                val product = getItem(position)

                tvName.text = product?.name ?: ""
                tvPrice.text = "₹${product?.price ?: ""}"

                // Decode Image
                product?.image?.let {
                    try {
                        val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        ivImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        ivImage.setImageDrawable(null)
                    }
                }

                btnDelete.setOnClickListener {

                    AlertDialog.Builder(this@MainActivity9)
                        .setTitle("Delete Product")
                        .setMessage("Delete ${product?.name}?")
                        .setPositiveButton("Yes") { _, _ ->
                            database.child(productKeys[position]).removeValue()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }

                btnEdit.setOnClickListener {

                    val productKey = productKeys[position]

                    val intent = Intent(this@MainActivity9, MainActivity8::class.java)
                    intent.putExtra("productId", productKey)

                    startActivity(intent)
                }

                return view
            }
        }

        listView.adapter = adapter
    }
}