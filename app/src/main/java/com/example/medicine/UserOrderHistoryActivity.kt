package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserOrderHistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var tvEmpty: TextView
    private lateinit var ordersRef: DatabaseReference
    private val orders = mutableListOf<Order>()
    private lateinit var adapter: UserOrderHistoryAdapter
    private var listener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_order_history)

        listView = findViewById(R.id.listUserOrders)
        tvEmpty = findViewById(R.id.tvEmptyOrders)
        ordersRef = FirebaseDatabase.getInstance().getReference("Orders")

        adapter = UserOrderHistoryAdapter(this, orders)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val id = orders.getOrNull(position)?.orderId ?: return@setOnItemClickListener
            startActivity(
                Intent(this, OrderDetailActivity::class.java)
                    .putExtra("orderId", id)
            )
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        listener = ordersRef.orderByChild("userId").equalTo(uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    orders.clear()
                    for (child in snapshot.children) {
                        val order = child.getValue(Order::class.java)
                        if (order != null) {
                            if (order.orderId.isNullOrEmpty()) order.orderId = child.key
                            orders.add(order)
                        }
                    }
                    orders.sortByDescending { it.createdAt ?: 0L }
                    adapter.notifyDataSetChanged()
                    tvEmpty.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserOrderHistoryActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.let { ordersRef.removeEventListener(it) }
    }
}
