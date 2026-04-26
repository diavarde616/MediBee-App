package com.example.medicine

import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ActiveFragment : Fragment() {

    private lateinit var adapter: ActiveOrdersAdapter
    private val orders = mutableListOf<Order>()
    private val ordersRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("Orders")
    }
    private var listener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_active, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.rvActiveOrders)
        val progress = view.findViewById<ProgressBar>(R.id.activeLoading)
        val empty = view.findViewById<TextView>(R.id.tvEmptyActive)

        adapter = ActiveOrdersAdapter(
            orders = orders,
            onStartDelivery = { order -> updateOrderStatus(order, "Out for delivery") },
            onDelivered = { order -> updateOrderStatus(order, "Delivered") },
            onOpenDetail = { order ->
                val id = order.orderId?.trim().orEmpty()
                if (id.isEmpty()) {
                    Toast.makeText(requireContext(), "Invalid order id", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(
                        Intent(requireContext(), OrderDetailActivity::class.java)
                            .putExtra("orderId", id)
                    )
                }
            }
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        fetchActiveOrders(progress, empty)
        return view
    }

    private fun fetchActiveOrders(progress: ProgressBar, empty: TextView) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val fallbackDeliveryId = requireContext()
            .getSharedPreferences("session", 0)
            .getString("deliveryBoyId", null)

        val resolvedId = fallbackDeliveryId ?: uid
        if (resolvedId.isNullOrEmpty()) {
            progress.visibility = View.GONE
            empty.visibility = View.VISIBLE
            empty.text = "Login required"
            return
        }

        Log.d("ACTIVE_ORDERS", "resolvedDeliveryBoyId=$resolvedId uid=$uid fallback=$fallbackDeliveryId")

        listener = ordersRef.orderByChild("deliveryBoyId").equalTo(resolvedId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()
                    for (child in snapshot.children) {
                        val order = child.getValue(Order::class.java)
                        if (order != null) {
                            if (order.orderId.isNullOrEmpty()) {
                                order.orderId = child.key
                            }
                            val status = order.status?.trim()?.lowercase().orEmpty()
                            val terminal = status == "delivered" ||
                                status == "rejected" ||
                                status == "cancelled"
                            if (!terminal) {
                                orders.add(order)
                            }
                        }
                    }

                    Log.d("ACTIVE_ORDERS", "fetched=${orders.size} for deliveryBoyId=$resolvedId")
                    adapter.notifyDataSetChanged()
                    progress.visibility = View.GONE
                    empty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    progress.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateOrderStatus(order: Order, status: String) {
        val orderId = order.orderId
        if (orderId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Invalid order id", Toast.LENGTH_SHORT).show()
            return
        }

        ordersRef.child(orderId).child("status").setValue(status)
            .addOnSuccessListener {
                Log.d("ACTIVE_ORDERS", "status updated orderId=$orderId status=$status")
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Status update failed", Toast.LENGTH_SHORT).show()
                Log.e("ACTIVE_ORDERS", "status update failed: ${it.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.let { ordersRef.removeEventListener(it) }
        listener = null
    }
}