package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var database: DatabaseReference
    private lateinit var orderList: ArrayList<Order>
    private var listener: ValueEventListener? = null

    private val deliveryBoyId: String?
        get() = requireContext().getSharedPreferences("session", 0).getString("deliveryBoyId", null)
            ?: FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_order, container, false)
        view.applySystemBarInsets()

        listView = view.findViewById(R.id.listOrders)
        database = FirebaseDatabase.getInstance().getReference("Orders")

        orderList = ArrayList()

        fetchOrders()

        return view
    }

    private fun fetchOrders() {
        listener = database.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                orderList.clear()

                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        val order = data.getValue(Order::class.java)
                        if (order != null) {
                            if (order.orderId.isNullOrEmpty()) order.orderId = data.key
                            val st = order.status?.trim() ?: ""
                            val rider = order.deliveryBoyId?.trim().orEmpty()
                            val isPending = st.equals("Pending", true) || st.isEmpty()
                            val unassigned = rider.isEmpty()
                            if (isPending && unassigned) {
                                orderList.add(order)
                            }
                        }
                    }
                }

                val adapter = object : ArrayAdapter<Order>(
                    requireContext(),
                    R.layout.order,
                    orderList
                ) {

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                        val row = layoutInflater.inflate(R.layout.order, null)

                        val item = orderList[position]

                        val tvName = row.findViewById<TextView>(R.id.tvCustomerName)
                        val tvAddress = row.findViewById<TextView>(R.id.tvAddress)
                        val tvItems = row.findViewById<TextView>(R.id.tvItems)
                        val tvTotal = row.findViewById<TextView>(R.id.tvTotal)
                        val tvStatus = row.findViewById<TextView>(R.id.tvStatus)

                        val btnAccept = row.findViewById<Button>(R.id.btnAccept)
                        val btnReject = row.findViewById<Button>(R.id.btnReject)
                        val btnDelivered = row.findViewById<Button>(R.id.btnDelivered)

                        tvName.text = item.customerName
                        tvAddress.text = "Address: ${item.address}"
                        tvItems.text = "Items: ${item.items}"
                        tvTotal.text = "Total: ₹${item.totalPrice}"
                        tvStatus.text = "Status: ${item.status}"

                        val orderId = item.orderId
                        val myId = deliveryBoyId

                        row.setOnClickListener {
                            val id = orderId?.trim().orEmpty()
                            if (id.isNotEmpty()) {
                                startActivity(
                                    Intent(context, OrderDetailActivity::class.java)
                                        .putExtra("orderId", id)
                                )
                            }
                        }

                        btnAccept.setOnClickListener {
                            if (orderId != null && myId != null) {
                                database.child(orderId).child("deliveryBoyId").setValue(myId)
                                database.child(orderId).child("status").setValue("Accepted")
                                Toast.makeText(context, "Order accepted — check Active tab", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Cannot assign rider", Toast.LENGTH_SHORT).show()
                            }
                        }

                        btnReject.setOnClickListener {
                            if (orderId != null) {
                                database.child(orderId).child("status").setValue("Rejected")
                                Toast.makeText(context, "Order Rejected", Toast.LENGTH_SHORT).show()
                            }
                        }

                        btnDelivered.visibility = View.GONE

                        return row
                    }
                }

                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.let { database.removeEventListener(it) }
        listener = null
    }
}
