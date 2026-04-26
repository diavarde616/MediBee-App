package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class CouponActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coupon)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationIcon(R.drawable.ic_arrow_back_24)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRemoveCoupon)
            .setOnClickListener {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_COUPON_CODE, ""))
                finish()
            }

        val rv = findViewById<RecyclerView>(R.id.rvCoupons)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = CouponAdapter(CouponCatalog.allCoupons) { coupon ->
            setResult(RESULT_OK, Intent().putExtra(EXTRA_COUPON_CODE, coupon.code))
            finish()
        }
    }

    private class CouponAdapter(
        private val items: List<PromoCoupon>,
        private val onPick: (PromoCoupon) -> Unit
    ) : RecyclerView.Adapter<CouponAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val code: TextView = view.findViewById(R.id.tvCouponCode)
            val minOrder: TextView = view.findViewById(R.id.tvMinOrder)
            val title: TextView = view.findViewById(R.id.tvCouponTitle)
            val desc: TextView = view.findViewById(R.id.tvCouponDesc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_coupon_row, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = items[position]
            holder.code.text = c.code
            holder.minOrder.text = "Min ₹${CouponCatalog.formatMoney(c.minOrder)}"
            holder.title.text = c.title
            val extra = if (c.firstOrderOnly) " • First order only" else ""
            holder.desc.text = c.description + extra
            holder.itemView.setOnClickListener { onPick(c) }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val EXTRA_COUPON_CODE = "COUPON_CODE"
    }
}
