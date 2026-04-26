package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private lateinit var autoComplete: AutoCompleteTextView
    private lateinit var tvGreeting: TextView
    private lateinit var rvTrending: RecyclerView
    private lateinit var productsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference

    private val productNames = mutableListOf<String>()
    private val productKeys = mutableListOf<String>()
    private val trending = mutableListOf<TrendingProduct>()
    private lateinit var trendingAdapter: HomeTrendingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        autoComplete = view.findViewById(R.id.autoComplete)
        rvTrending = view.findViewById(R.id.rvTrending)

        rvTrending.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        trendingAdapter = HomeTrendingAdapter(trending) { item ->
            startActivity(
                Intent(requireContext(), MainActivity7::class.java)
                    .putExtra("productId", item.key)
            )
        }
        rvTrending.adapter = trendingAdapter

        productsRef = FirebaseDatabase.getInstance().getReference("products")
        usersRef = FirebaseDatabase.getInstance().getReference("Users")

        loadProductNames()
        loadTrending()

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedKey = productKeys[position]
            startActivity(
                Intent(requireContext(), MainActivity7::class.java)
                    .putExtra("productId", selectedKey)
            )
        }

        view.findViewById<TextView>(R.id.tvViewAllCategories).setOnClickListener {
            (activity as? MainActivity4)?.selectTab(1)
        }

        view.findViewById<TextView>(R.id.tvViewAllTrending).setOnClickListener {
            startActivity(Intent(requireContext(), MainActivity10::class.java))
        }

        bindCategory(
            view.findViewById(R.id.catInjection),
            R.drawable.injection,
            "Injection",
            "Injection"
        )
        bindCategory(
            view.findViewById(R.id.catCapsule),
            R.drawable.capsule,
            "Capsule",
            "Capsule"
        )
        bindCategory(
            view.findViewById(R.id.catSyrup),
            R.drawable.syrup,
            "Syrup",
            "Syrup"
        )
        bindCategory(
            view.findViewById(R.id.catTube),
            R.drawable.tube,
            "Tube",
            "Tube"
        )

        return view
    }

    override fun onStart() {
        super.onStart()
        loadUserName()
    }

    private fun bindCategory(root: View, imageRes: Int, label: String, category: String) {
        root.findViewById<ImageView>(R.id.ivCat).setImageResource(imageRes)
        root.findViewById<TextView>(R.id.tvCatLabel).text = label
        root.setOnClickListener {
            startActivity(
                Intent(requireContext(), MainActivity10::class.java)
                    .putExtra("category", category)
            )
        }
    }

    private fun loadUserName() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        usersRef.child(uid).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.getValue(String::class.java)?.trim()?.takeIf { it.isNotEmpty() }
                    tvGreeting.text = if (name != null) "Hello, $name!" else "Hello!"
                }

                override fun onCancelled(error: DatabaseError) {
                    tvGreeting.text = "Hello!"
                }
            })
    }

    private fun loadProductNames() {
        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productNames.clear()
                productKeys.clear()
                for (snap in snapshot.children) {
                    val name = snap.child("name").getValue(String::class.java)
                    if (name != null) {
                        productNames.add(name)
                        productKeys.add(snap.key!!)
                    }
                }
                if (isAdded) {
                    autoComplete.setAdapter(
                        ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            productNames
                        )
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadTrending() {
        productsRef.limitToFirst(12)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trending.clear()
                    for (snap in snapshot.children) {
                        val p = snap.getValue(Product::class.java) ?: continue
                        trending.add(TrendingProduct(snap.key!!, p))
                    }
                    if (isAdded) trendingAdapter.submit(trending)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
