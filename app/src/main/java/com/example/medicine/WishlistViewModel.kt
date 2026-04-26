package com.example.medicine

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*

class WishlistViewModel : ViewModel() {
    private val _wishlistItems = MutableLiveData<List<WishlistItem>>(emptyList())
    val wishlistItems: LiveData<List<WishlistItem>> = _wishlistItems

    private var wishlistRef: DatabaseReference? = null
    private var listener: ValueEventListener? = null

    fun start(uid: String) {
        stop()
        wishlistRef = FirebaseDatabase.getInstance().getReference("wishlist").child(uid)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updated = mutableListOf<WishlistItem>()
                for (child in snapshot.children) {
                    val item = child.getValue(WishlistItem::class.java)
                    if (item != null) updated.add(item)
                }
                _wishlistItems.value = updated
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        wishlistRef?.addValueEventListener(listener!!)
    }

    fun remove(productId: String) {
        wishlistRef?.child(productId)?.removeValue()
    }

    fun stop() {
        val ref = wishlistRef
        val l = listener
        if (ref != null && l != null) {
            ref.removeEventListener(l)
        }
        listener = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
