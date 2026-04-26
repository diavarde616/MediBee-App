package com.example.medicine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartFragment : Fragment() {

    private lateinit var rvCart: RecyclerView
    private lateinit var btnPay: MaterialButton
    private lateinit var btnApplyCoupon: MaterialButton
    private lateinit var database: DatabaseReference
    private lateinit var tvItemsSubtotal: TextView
    private lateinit var tvDiscountAmount: TextView
    private lateinit var tvDeliveryFee: TextView
    private lateinit var tvCartTotalSummary: TextView
    private lateinit var tvCartLineCount: TextView
    private lateinit var tvCouponBanner: TextView
    private lateinit var tvCartEmpty: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvCustomerAddress: TextView

    private val cartList = mutableListOf<CartItem>()
    private val cartKeys = mutableListOf<String>()
    private var cartListener: ValueEventListener? = null

    private var appliedCouponCode: String? = null
    private var isFirstOrder: Boolean = false
    private var firstOrderResolved: Boolean = false
    private var currentUid: String? = null

    private val couponLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val code = result.data?.getStringExtra(CouponActivity.EXTRA_COUPON_CODE)?.trim().orEmpty()
            val uid = currentUid ?: return@registerForActivityResult
            CartCouponPrefs.setAppliedCode(requireContext(), uid, code.takeIf { it.isNotEmpty() })
            appliedCouponCode = CartCouponPrefs.getAppliedCode(requireContext(), uid)
            refreshTotalsUi()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_cart, container, false)
        rvCart = view.findViewById(R.id.rvCart)
        btnPay = view.findViewById(R.id.btnProceedToPay)
        btnApplyCoupon = view.findViewById(R.id.btnApplyCoupon)
        tvItemsSubtotal = view.findViewById(R.id.tvItemsSubtotal)
        tvDiscountAmount = view.findViewById(R.id.tvDiscountAmount)
        tvDeliveryFee = view.findViewById(R.id.tvDeliveryFee)
        tvCartTotalSummary = view.findViewById(R.id.tvCartTotalSummary)
        tvCartLineCount = view.findViewById(R.id.tvCartLineCount)
        tvCouponBanner = view.findViewById(R.id.tvCouponBanner)
        tvCartEmpty = view.findViewById(R.id.tvCartEmpty)
        tvCustomerName = view.findViewById(R.id.tvCustomerName)
        tvCustomerPhone = view.findViewById(R.id.tvCustomerPhone)
        tvCustomerAddress = view.findViewById(R.id.tvCustomerAddress)

        view.findViewById<TextView>(R.id.btnEditCustomer).setOnClickListener {
            (activity as? MainActivity4)?.selectTab(2)
        }
        view.findViewById<ImageButton>(R.id.btnHeaderWishlist).setOnClickListener {
            (activity as? MainActivity4)?.selectTab(1)
        }
        view.findViewById<ImageButton>(R.id.btnHeaderCart).setOnClickListener {
            rvCart.smoothScrollToPosition(0)
        }

        rvCart.layoutManager = LinearLayoutManager(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnApplyCoupon.setOnClickListener {
            couponLauncher.launch(Intent(requireContext(), CouponActivity::class.java))
        }

        btnPay.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val itemsTotal = sumLineTotals()
            val delivery = deliveryChargeNow()
            val applied = CouponCatalog.apply(appliedCouponCode, itemsTotal, delivery, isFirstOrder)
            if (!applied.success) {
                Toast.makeText(requireContext(), applied.message, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val discount = applied.totalDiscount
            val payable = CouponCatalog.payableAmount(itemsTotal, delivery, discount)

            val itemsBuilder = StringBuilder()
            for (item in cartList) {
                val line = item.price ?: 0.0
                itemsBuilder.append("${item.name} x${item.quantity} (₹${CouponCatalog.formatMoney(line)}), ")
            }

            val intent = Intent(requireContext(), PaymentActivity::class.java)
            intent.putExtra(PaymentActivity.EXTRA_PAYABLE_AMOUNT, payable)
            intent.putExtra(PaymentActivity.EXTRA_ITEMS_SUBTOTAL, itemsTotal)
            intent.putExtra(PaymentActivity.EXTRA_DELIVERY_CHARGE, delivery)
            intent.putExtra(PaymentActivity.EXTRA_COUPON_DISCOUNT, discount)
            intent.putExtra(PaymentActivity.EXTRA_COUPON_CODE, appliedCouponCode.orEmpty())
            intent.putExtra("ITEMS", itemsBuilder.toString())
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        currentUid = uid
        appliedCouponCode = CartCouponPrefs.getAppliedCode(requireContext(), uid)

        database = FirebaseDatabase.getInstance()
            .getReference("cart")
            .child(uid)

        Log.d("CART_READ", "Reading from path: cart/$uid")

        loadCustomer(uid)
        checkFirstOrder(uid)
        loadCart()
    }

    private fun loadCustomer(uid: String) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return
                    val user = snapshot.getValue(User::class.java)
                    tvCustomerName.text = user?.name?.ifBlank { "—" } ?: "—"
                    tvCustomerPhone.text = user?.phone?.ifBlank { "—" } ?: "—"
                    val addr = user?.address?.trim().orEmpty()
                    tvCustomerAddress.text =
                        if (addr.isNotEmpty()) addr else "Add your address at checkout or in profile."
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkFirstOrder(uid: String) {
        FirebaseDatabase.getInstance().getReference("Orders")
            .orderByChild("userId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFirstOrder = !snapshot.exists()
                    firstOrderResolved = true
                    if (isAdded) refreshTotalsUi()
                }

                override fun onCancelled(error: DatabaseError) {
                    isFirstOrder = false
                    firstOrderResolved = true
                    if (isAdded) refreshTotalsUi()
                }
            })
    }

    private fun loadCart() {
        cartListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                cartList.clear()
                cartKeys.clear()

                for (snap in snapshot.children) {
                    val item = snap.getValue(CartItem::class.java)
                    if (item != null) {
                        cartList.add(item)
                        cartKeys.add(snap.key!!)
                    }
                }

                rvCart.post {
                    if (!isAdded) return@post
                    bindAdapter()
                    val n = cartList.size
                    tvCartLineCount.text = if (n == 0) "0 items" else "$n items"
                    tvCartEmpty.visibility = if (n == 0) View.VISIBLE else View.GONE
                    rvCart.visibility = if (n == 0) View.GONE else View.VISIBLE
                    refreshTotalsUi()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CART_READ", "Error: ${error.message}")
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        }

        database.addValueEventListener(cartListener!!)
    }

    private fun bindAdapter() {
        rvCart.adapter = CartLineAdapter(
            cartList,
            cartKeys,
            onPlus = { item, key -> handlePlus(item, key) },
            onMinus = { item, key -> handleMinus(item, key) },
            onRemove = { item, key -> handleRemove(item, key) },
            onSaveLater = { item, key -> handleSaveLater(item, key) }
        )
    }

    private fun sumLineTotals(): Double =
        cartList.sumOf { it.price ?: 0.0 }

    private fun deliveryChargeNow(): Double =
        if (!firstOrderResolved) PaymentActivity.DEFAULT_DELIVERY_CHARGE
        else if (isFirstOrder) 0.0
        else PaymentActivity.DEFAULT_DELIVERY_CHARGE

    private fun refreshTotalsUi() {
        val itemsTotal = sumLineTotals()
        val delivery = deliveryChargeNow()

        val first = CouponCatalog.apply(appliedCouponCode, itemsTotal, delivery, isFirstOrder)
        var invalidCouponMessage: String? = null
        if (!first.success && !appliedCouponCode.isNullOrBlank()) {
            invalidCouponMessage = first.message
            CartCouponPrefs.setAppliedCode(requireContext(), currentUid, null)
            appliedCouponCode = null
        }

        val finalResult = CouponCatalog.apply(appliedCouponCode, itemsTotal, delivery, isFirstOrder)
        val discount = if (finalResult.success) finalResult.totalDiscount else 0.0
        val payable = CouponCatalog.payableAmount(itemsTotal, delivery, discount)

        tvItemsSubtotal.text = "₹${CouponCatalog.formatMoney(itemsTotal)}"
        tvDiscountAmount.text = "-₹${CouponCatalog.formatMoney(discount)}"
        tvDeliveryFee.text = if (delivery == 0.0) "FREE" else "₹${CouponCatalog.formatMoney(delivery)}"
        tvCartTotalSummary.text = "₹${CouponCatalog.formatMoney(payable)}"

        tvCouponBanner.text = invalidCouponMessage ?: when {
            appliedCouponCode.isNullOrBlank() -> {
                if (firstOrderResolved && isFirstOrder) {
                    "First order: free delivery applied."
                } else {
                    "Add a coupon or proceed to checkout."
                }
            }
            finalResult.success -> "${finalResult.message} · You save ₹${CouponCatalog.formatMoney(discount)}"
            else -> ""
        }
    }

    private fun handlePlus(item: CartItem, key: String) {
        val currentQty = item.quantity ?: 1
        val unitPrice = item.unitPrice ?: 0.0
        val productId = item.productId ?: return

        val productRef = FirebaseDatabase.getInstance()
            .getReference("products").child(productId)

        productRef.child("stock")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stock = snapshot.getValue(Int::class.java) ?: 0
                    if (stock <= 0) {
                        Toast.makeText(requireContext(), "No more stock", Toast.LENGTH_SHORT).show()
                    } else {
                        val newQty = currentQty + 1
                        database.child(key).child("quantity").setValue(newQty)
                        database.child(key).child("price").setValue(unitPrice * newQty)
                        productRef.child("stock").setValue(stock - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handleMinus(item: CartItem, key: String) {
        val currentQty = item.quantity ?: 1
        if (currentQty <= 1) return

        val unitPrice = item.unitPrice ?: 0.0
        val productId = item.productId ?: return
        val newQty = currentQty - 1

        val productRef = FirebaseDatabase.getInstance()
            .getReference("products").child(productId)

        database.child(key).child("quantity").setValue(newQty)
        database.child(key).child("price").setValue(unitPrice * newQty)

        productRef.child("stock")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stock = snapshot.getValue(Int::class.java) ?: 0
                    productRef.child("stock").setValue(stock + 1)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun handleRemove(item: CartItem, key: String) {
        restoreStockAndRemove(item, key, showToast = true)
    }

    private fun handleSaveLater(item: CartItem, key: String) {
        val uid = currentUid ?: return
        val pid = item.productId ?: return
        val unit = item.unitPrice ?: ((item.price ?: 0.0) / (item.quantity ?: 1).coerceAtLeast(1))
        val wish = WishlistItem(
            productId = pid,
            name = item.name,
            price = unit,
            image = item.image,
            category = null,
            stock = null,
            description = null,
            uid = uid
        )
        FirebaseDatabase.getInstance().getReference("wishlist").child(uid).child(pid).setValue(wish)
        restoreStockAndRemove(item, key, showToast = false)
        Toast.makeText(requireContext(), "Saved to wishlist", Toast.LENGTH_SHORT).show()
    }

    private fun restoreStockAndRemove(item: CartItem, key: String, showToast: Boolean) {
        val productId = item.productId ?: return
        val itemQty = item.quantity ?: 1

        val productRef = FirebaseDatabase.getInstance()
            .getReference("products").child(productId)

        productRef.child("stock")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val stock = snapshot.getValue(Int::class.java) ?: 0
                    productRef.child("stock").setValue(stock + itemQty)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        database.child(key).removeValue()
        if (showToast) {
            Toast.makeText(requireContext(), "Removed from cart", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::database.isInitialized) {
            cartListener?.let { database.removeEventListener(it) }
        }
    }
}
