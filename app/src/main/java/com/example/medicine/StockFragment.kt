package com.example.medicine

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class StockFragment : Fragment() {

    private lateinit var tvTotalProducts: TextView
    private lateinit var tvOutOfStock: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvInventoryValue: TextView
    private lateinit var tvStockEmpty: TextView
    private lateinit var rvLowStock: RecyclerView
    private lateinit var adapter: LowStockAdapter
    private lateinit var productRef: DatabaseReference
    private var listener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stock, container, false)
        tvTotalProducts = view.findViewById(R.id.tvTotalProducts)
        tvOutOfStock = view.findViewById(R.id.tvOutOfStock)
        tvLowStock = view.findViewById(R.id.tvLowStock)
        tvInventoryValue = view.findViewById(R.id.tvInventoryValue)
        tvStockEmpty = view.findViewById(R.id.tvStockEmpty)
        rvLowStock = view.findViewById(R.id.rvLowStock)

        adapter = LowStockAdapter(mutableListOf())
        rvLowStock.layoutManager = LinearLayoutManager(requireContext())
        rvLowStock.adapter = adapter
        productRef = FirebaseDatabase.getInstance().getReference("products")
        return view
    }

    override fun onStart() {
        super.onStart()
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalProducts = 0
                var outOfStock = 0
                var lowStock = 0
                var inventoryValue = 0.0
                val lowStockItems = mutableListOf<Pair<String, Product>>()

                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val product = child.getValue(Product::class.java) ?: continue
                    totalProducts++
                    val stock = product.stock ?: 0
                    val price = product.price ?: 0.0
                    inventoryValue += (stock * price)
                    if (stock <= 0) outOfStock++
                    if (stock <= 5) {
                        lowStock++
                        lowStockItems.add(key to product)
                    }
                }

                tvTotalProducts.text = totalProducts.toString()
                tvOutOfStock.text = outOfStock.toString()
                tvLowStock.text = lowStock.toString()
                tvInventoryValue.text = "₹%.0f".format(inventoryValue)
                adapter.submit(lowStockItems)

                val isEmpty = lowStockItems.isEmpty()
                tvStockEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                rvLowStock.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                tvStockEmpty.text = error.message
                tvStockEmpty.visibility = View.VISIBLE
                rvLowStock.visibility = View.GONE
            }
        }
        productRef.addValueEventListener(listener!!)
    }

    override fun onStop() {
        super.onStop()
        if (::productRef.isInitialized && listener != null) {
            productRef.removeEventListener(listener!!)
        }
    }
}