package com.example.medicine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class ActiveOrdersAdapter(
    private val orders: List<Order>,
    private val onStartDelivery: (Order) -> Unit,
    private val onDelivered: (Order) -> Unit,
    private val onOpenDetail: (Order) -> Unit
) : RecyclerView.Adapter<ActiveOrdersAdapter.ActiveOrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_active_order, parent, false)
        return ActiveOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActiveOrderViewHolder, position: Int) {
        holder.bind(orders[position], onStartDelivery, onDelivered, onOpenDetail)
    }

    override fun getItemCount(): Int = orders.size

    class ActiveOrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvItems: TextView = itemView.findViewById(R.id.tvItems)
        private val tvTotal: TextView = itemView.findViewById(R.id.tvTotal)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnStart: MaterialButton = itemView.findViewById(R.id.btnAcceptOrder)
        private val btnDelivered: MaterialButton = itemView.findViewById(R.id.btnMarkDelivered)

        fun bind(
            order: Order,
            onStartDelivery: (Order) -> Unit,
            onDelivered: (Order) -> Unit,
            onOpenDetail: (Order) -> Unit
        ) {
            itemView.setOnClickListener { onOpenDetail(order) }

            tvCustomerName.text = order.customerName ?: "Customer"
            tvAddress.text = "Address: ${order.address ?: "-"}"
            tvItems.text = "Items: ${order.items ?: "-"}"
            tvTotal.text = "Total: ₹${order.totalPrice ?: 0.0}"

            val status = order.status?.trim()?.ifEmpty { "Pending" } ?: "Pending"
            tvStatus.text = "Status: $status"

            val terminal = status.equals("Delivered", true) ||
                status.equals("Rejected", true) ||
                status.equals("Cancelled", true)

            when {
                terminal -> {
                    btnStart.visibility = View.GONE
                    btnStart.setOnClickListener(null)
                    btnDelivered.visibility = View.GONE
                    btnDelivered.setOnClickListener(null)
                }
                status.equals("Accepted", true) -> {
                    btnStart.visibility = View.VISIBLE
                    btnStart.text = "Start delivery"
                    btnStart.setOnClickListener { onStartDelivery(order) }
                    btnDelivered.visibility = View.VISIBLE
                    btnDelivered.text = "Mark delivered"
                    btnDelivered.setOnClickListener { onDelivered(order) }
                }
                status.equals("Out for delivery", true) -> {
                    btnStart.visibility = View.GONE
                    btnDelivered.visibility = View.VISIBLE
                    btnDelivered.text = "Mark delivered"
                    btnDelivered.setOnClickListener { onDelivered(order) }
                }
                else -> {
                    btnStart.visibility = View.GONE
                    btnDelivered.visibility = View.VISIBLE
                    btnDelivered.text = "Mark delivered"
                    btnDelivered.setOnClickListener { onDelivered(order) }
                }
            }
        }
    }
}
