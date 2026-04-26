package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class notifdeliveryFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var btnAdd: Button
    private lateinit var database: DatabaseReference

    private lateinit var deliveryList: ArrayList<DeliveryBoy>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_notifdelivery, container, false)

        listView = view.findViewById(R.id.listDeliveryBoys)
        btnAdd = view.findViewById(R.id.btnAddDeliveryBoy)

        deliveryList = ArrayList()
        database = FirebaseDatabase.getInstance().getReference("DeliveryBoys")

        // 👉 Open Create Account Activity
        btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), DboyAccActivity::class.java))
        }

        // 👉 Fetch Data from Firebase
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                deliveryList.clear()

                for (data in snapshot.children) {
                    val deliveryBoy = data.getValue(DeliveryBoy::class.java)
                    if (deliveryBoy != null) {
                        deliveryList.add(deliveryBoy)
                    }
                }

                val adapter = object : ArrayAdapter<DeliveryBoy>(
                    requireContext(),
                    R.layout.delivery,
                    deliveryList
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                        val view = layoutInflater.inflate(R.layout.delivery, null)

                        val item = deliveryList[position]

                        view.findViewById<TextView>(R.id.tvName).text = item.name
                        view.findViewById<TextView>(R.id.tvPhone).text = "Phone: ${item.phone}"
                        view.findViewById<TextView>(R.id.tvEmail).text = "Email: ${item.email}"
                        view.findViewById<TextView>(R.id.tvAddress).text = "Address: ${item.address}"
                        view.findViewById<TextView>(R.id.tvStatus).text = "Status: ${item.status}"

                        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
                        val btnEdit = view.findViewById<Button>(R.id.btnEdit)

                        // 👉 Delete
                        btnDelete.setOnClickListener {
                            val id = item.id
                            if (id != null) {
                                database.child(id).removeValue()
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            }
                        }

                        // 👉 Edit (you can implement later)
                        btnEdit.setOnClickListener {
                            val id = item.id
                            if (id.isNullOrEmpty()) {
                                Toast.makeText(context, "Invalid delivery profile", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                            startActivity(
                                Intent(requireContext(), DboyAccActivity::class.java)
                                    .putExtra("isEditMode", true)
                                    .putExtra("deliveryBoyId", id)
                            )
                        }

                        return view
                    }
                }

                listView.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        return view
    }
}