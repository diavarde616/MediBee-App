package com.example.medicine

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class CartLineAdapter(
    private val items: List<CartItem>,
    private val keys: List<String>,
    private val onPlus: (CartItem, String) -> Unit,
    private val onMinus: (CartItem, String) -> Unit,
    private val onRemove: (CartItem, String) -> Unit,
    private val onSaveLater: (CartItem, String) -> Unit
) : RecyclerView.Adapter<CartLineAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iv: ImageView = itemView.findViewById(R.id.ivProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvVariant: TextView = itemView.findViewById(R.id.tvVariantLine)
        val tvLinePrice: TextView = itemView.findViewById(R.id.tvLinePrice)
        val tvQty: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnMinus: MaterialButton = itemView.findViewById(R.id.btnMinus)
        val btnPlus: MaterialButton = itemView.findViewById(R.id.btnPlus)
        val btnRemove: MaterialButton = itemView.findViewById(R.id.btnRemove)
        val btnSaveLater: MaterialButton = itemView.findViewById(R.id.btnSaveLater)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cart_line, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val key = keys[position]
        val qty = item.quantity ?: 1
        val unit = item.unitPrice ?: ((item.price ?: 0.0) / qty.coerceAtLeast(1))
        val line = item.price ?: 0.0

        holder.tvName.text = item.name ?: "Item"
        holder.tvVariant.text =
            holder.itemView.context.getString(R.string.cart_line_meta, qty, CouponCatalog.formatMoney(unit))
        holder.tvLinePrice.text = "₹${CouponCatalog.formatMoney(line)}"
        holder.tvQty.text = qty.toString()

        item.image?.let { enc ->
            try {
                val bytes = Base64.decode(enc, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.iv.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.iv.setImageDrawable(null)
            }
        } ?: holder.iv.setImageDrawable(null)

        holder.btnPlus.setOnClickListener { onPlus(item, key) }
        holder.btnMinus.setOnClickListener { onMinus(item, key) }
        holder.btnRemove.setOnClickListener { onRemove(item, key) }
        holder.btnSaveLater.setOnClickListener { onSaveLater(item, key) }
    }

    override fun getItemCount(): Int = items.size
}
