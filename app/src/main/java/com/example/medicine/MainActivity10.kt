package com.example.medicine

import android.graphics.BitmapFactory
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity10 : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var database: DatabaseReference

    private val products: MutableList<Product> = mutableListOf()
    private val productKeys: MutableList<String> = mutableListOf()
    private lateinit var adapter: ArrayAdapter<Product>
    private var selectedCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main10)

        listView = findViewById(R.id.listViewProducts)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        selectedCategory = intent.getStringExtra("category")?.trim()?.takeIf { it.isNotEmpty() }
        if (selectedCategory != null) {
            tvTitle.text = "${selectedCategory} Medicines"
        }

        database = FirebaseDatabase.getInstance().getReference("products")

        setupAdapter()
        loadProducts()
    }

    private fun loadProducts() {

        database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                products.clear()
                productKeys.clear()
                val normalizedSelected = selectedCategory?.lowercase()

                for (snap in snapshot.children) {

                    try {
                        val product = snap.getValue(Product::class.java)

                        if (product != null) {
                            val productCategory = product.category?.trim()?.lowercase()
                            val shouldInclude = normalizedSelected == null || normalizedSelected == productCategory

                            Log.d(
                                "CATEGORY_FILTER",
                                "selected=$normalizedSelected productCategory=$productCategory name=${product.name} include=$shouldInclude"
                            )

                            if (shouldInclude) {
                                products.add(product)
                                productKeys.add(snap.key ?: "")
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("CATEGORY_FILTER", "Product parse error: ${e.message}", e)
                    }
                }

                Log.d("CATEGORY_FILTER", "finalCount=${products.size} selected=$normalizedSelected")
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity10, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupAdapter() {

        adapter = object : ArrayAdapter<Product>(this, R.layout.product, products) {

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                val view = convertView ?: layoutInflater.inflate(R.layout.product, parent, false)

                val ivImage = view.findViewById<ImageView>(R.id.ivProduct)
                val tvName = view.findViewById<TextView>(R.id.tvProductName)
                val tvPrice = view.findViewById<TextView>(R.id.tvProductPrice)
                val tvQty = view.findViewById<TextView>(R.id.tvProductQuantity)

                val product = getItem(position)

                tvName.text = product?.name ?: ""
                tvPrice.text = "₹${product?.price ?: ""}"
                tvQty.text = "Stock: ${product?.stock ?: 0}"

                product?.image?.let {
                    try {
                        val decoded = Base64.decode(it, Base64.DEFAULT)
                        val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        ivImage.setImageBitmap(bmp)
                    } catch (e: Exception) {
                        ivImage.setImageDrawable(null)
                    }
                }

                return view
            }
        }

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val key = productKeys.getOrNull(position).orEmpty()
            if (key.isNotEmpty()) {
                startActivity(
                    Intent(this@MainActivity10, MainActivity7::class.java)
                        .putExtra("productId", key)
                )
            }
        }
    }
}