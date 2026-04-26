package com.example.medicine

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WishlistAdapter(
    private val items: MutableList<WishlistItem>,
    private val onItemClick: (WishlistItem) -> Unit,
    private val onRemoveClick: (WishlistItem) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.WishlistVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wishlist, parent, false)
        return WishlistVH(view)
    }

    override fun onBindViewHolder(holder: WishlistVH, position: Int) {
        holder.bind(items[position], onItemClick, onRemoveClick)
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<WishlistItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class WishlistVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivWishlistProduct)
        private val tvName: TextView = itemView.findViewById(R.id.tvWishlistName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvWishlistPrice)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveWishlist)

        fun bind(
            item: WishlistItem,
            onItemClick: (WishlistItem) -> Unit,
            onRemoveClick: (WishlistItem) -> Unit
        ) {
            tvName.text = item.name ?: "Medicine"
            tvPrice.text = "₹%.2f".format(item.price ?: 0.0)

            item.image?.let {
                try {
                    val bytes = Base64.decode(it, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ivImage.setImageBitmap(bmp)
                } catch (_: Exception) {
                    ivImage.setImageDrawable(null)
                }
            } ?: ivImage.setImageDrawable(null)

            itemView.setOnClickListener { onItemClick(item) }
            btnRemove.setOnClickListener { onRemoveClick(item) }
        }
    }
}
