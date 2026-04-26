package com.example.medicine

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class ProductAdapter(
    private val context: Activity,
    private val list: List<Product>
) : ArrayAdapter<Product>(context, R.layout.edit, list) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.edit, parent, false)

        val name = view.findViewById<TextView>(R.id.tvProductName)
        val price = view.findViewById<TextView>(R.id.tvProductPrice)

        val editBtn = view.findViewById<Button>(R.id.edit)
        val deleteBtn = view.findViewById<Button>(R.id.delete)

        val product = list[position]

        name.text = product.name
        price.text = "₹ " + product.price

        editBtn.setOnClickListener {
            Toast.makeText(context, "Edit " + product.name, Toast.LENGTH_SHORT).show()
        }

        deleteBtn.setOnClickListener {
            Toast.makeText(context, "Delete " + product.name, Toast.LENGTH_SHORT).show()
        }

        return view
    }
}