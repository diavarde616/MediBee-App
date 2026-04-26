package com.example.medicine

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LowStockAdapter(private val items: MutableList<Pair<String, Product>>) :
    RecyclerView.Adapter<LowStockAdapter.LowStockVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LowStockVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_low_stock, parent, false)
        return LowStockVH(view)
    }

    override fun onBindViewHolder(holder: LowStockVH, position: Int) {
        holder.bind(items[position].second)
    }

    override fun getItemCount(): Int = items.size

    fun submit(updated: List<Pair<String, Product>>) {
        items.clear()
        items.addAll(updated)
        notifyDataSetChanged()
    }

    class LowStockVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivLowStockImage)
        private val tvName: TextView = itemView.findViewById(R.id.tvLowStockName)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvLowStockMeta)
        private val tvCount: TextView = itemView.findViewById(R.id.tvLowStockCount)

        fun bind(product: Product) {
            tvName.text = product.name ?: "Medicine"
            tvMeta.text = "${product.category ?: "General"} • ₹%.2f".format(product.price ?: 0.0)
            val stockCount = product.stock ?: 0
            tvCount.text = if (stockCount <= 0) "Out of stock" else "Only $stockCount left"

            product.image?.let {
                try {
                    val bytes = Base64.decode(it, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ivImage.setImageBitmap(bmp)
                } catch (_: Exception) {
                    ivImage.setImageDrawable(null)
                }
            } ?: ivImage.setImageDrawable(null)
        }
    }
}
