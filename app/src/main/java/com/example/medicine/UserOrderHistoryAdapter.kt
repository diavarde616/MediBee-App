package com.example.medicine

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserOrderHistoryAdapter(
    private val context: Activity,
    private val orders: List<Order>
) : ArrayAdapter<Order>(context, R.layout.item_user_order_history, orders) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_user_order_history, parent, false)

        val item = orders[position]
        view.findViewById<TextView>(R.id.tvOrderDate).text =
            "Date: ${dateFormat.format(Date(item.createdAt ?: System.currentTimeMillis()))}"
        view.findViewById<TextView>(R.id.tvOrderTotal).text = "Total: ₹${item.totalPrice ?: 0.0}"
        view.findViewById<TextView>(R.id.tvOrderItems).text = "Items: ${item.items ?: "-"}"
        view.findViewById<TextView>(R.id.tvOrderStatus).text = item.status ?: "Pending"
        view.findViewById<TextView>(R.id.tvDeliveryStatus).text =
            if (item.deliveryBoyId.isNullOrBlank()) "Waiting for delivery partner" else "Assigned"

        return view
    }
}
