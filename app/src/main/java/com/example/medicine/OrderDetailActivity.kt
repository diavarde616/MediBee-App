package com.example.medicine

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.database.*

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var orderRef: DatabaseReference
    private var listener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_detail)

        findViewById<MaterialToolbar>(R.id.toolbarOrderDetail).setNavigationOnClickListener { finish() }

        val orderId = intent.getStringExtra("orderId") ?: run {
            finish()
            return
        }

        val tvId = findViewById<TextView>(R.id.tvOrderId)
        val tvWhen = findViewById<TextView>(R.id.tvPlacedOn)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvPayment = findViewById<TextView>(R.id.tvPayment)
        val tvAddress = findViewById<TextView>(R.id.tvAddress)
        val tvItems = findViewById<TextView>(R.id.tvItems)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)

        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId)

        listener = orderRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val o = snapshot.getValue(Order::class.java) ?: return
                tvId.text = o.orderId ?: orderId
                val t = o.createdAt ?: 0L
                tvWhen.text = if (t > 0) {
                    java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                        .format(java.util.Date(t))
                } else "-"
                tvStatus.text = o.status ?: "Pending"
                tvPayment.text = o.paymentMethod ?: "-"
                tvAddress.text = o.address ?: "-"
                tvItems.text = o.items ?: "-"
                tvTotal.text = "₹${o.totalPrice ?: 0.0}"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.let { orderRef.removeEventListener(it) }
    }
}
