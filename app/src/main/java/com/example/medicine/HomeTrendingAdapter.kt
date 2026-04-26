package com.example.medicine

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class TrendingProduct(
    val key: String,
    val product: Product
)

class HomeTrendingAdapter(
    private var items: List<TrendingProduct>,
    private val onClick: (TrendingProduct) -> Unit
) : RecyclerView.Adapter<HomeTrendingAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_home_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<TrendingProduct>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iv: ImageView = itemView.findViewById(R.id.ivProduct)
        private val name: TextView = itemView.findViewById(R.id.tvProductName)
        private val price: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val cat: TextView = itemView.findViewById(R.id.tvProductCategory)

        fun bind(item: TrendingProduct, onClick: (TrendingProduct) -> Unit) {
            val p = item.product
            name.text = p.name ?: ""
            price.text = "₹${p.price ?: 0.0}"
            cat.text = p.category ?: "Medicine"

            p.image?.let { enc ->
                try {
                    val bytes = Base64.decode(enc, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    iv.setImageBitmap(bmp)
                } catch (_: Exception) {
                    iv.setImageDrawable(null)
                }
            } ?: iv.setImageDrawable(null)

            itemView.setOnClickListener { onClick(item) }
        }
    }
}
