package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class WishlistFragment : Fragment() {

    private lateinit var rvWishlist: RecyclerView
    private lateinit var tvEmpty: TextView
    private val items = mutableListOf<WishlistItem>()
    private lateinit var adapter: WishlistAdapter
    private val wishlistViewModel: WishlistViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_wishlist, container, false)
        rvWishlist = view.findViewById(R.id.rvWishlist)
        tvEmpty = view.findViewById(R.id.tvWishlistEmpty)

        adapter = WishlistAdapter(
            items = items,
            onItemClick = { item ->
                val productId = item.productId ?: return@WishlistAdapter
                startActivity(Intent(requireContext(), MainActivity7::class.java).putExtra("productId", productId))
            },
            onRemoveClick = { item ->
                val productId = item.productId ?: return@WishlistAdapter
                wishlistViewModel.remove(productId)
            }
        )

        rvWishlist.layoutManager = LinearLayoutManager(requireContext())
        rvWishlist.adapter = adapter
        return view
    }

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }
        wishlistViewModel.start(uid)
        wishlistViewModel.wishlistItems.observe(viewLifecycleOwner) { updated ->
            adapter.submit(updated)
            val isEmpty = updated.isEmpty()
            tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            rvWishlist.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        wishlistViewModel.stop()
    }
}