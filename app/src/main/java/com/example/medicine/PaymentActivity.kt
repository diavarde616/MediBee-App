package com.example.medicine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class PaymentActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var tvCustomerName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView

    private lateinit var spinnerAddress: Spinner
    private lateinit var layoutNewAddress: LinearLayout
    private lateinit var etLabel: EditText
    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var etPincode: EditText

    private lateinit var tvProductNames: TextView
    private lateinit var tvTotalQuantity: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvDeliveryCharge: TextView
    private lateinit var tvDiscount: TextView
    private lateinit var tvFinalAmount: TextView
    private lateinit var tvOrderNote: TextView

    private lateinit var paymentGroup: RadioGroup
    private lateinit var btnConfirm: Button

    private lateinit var etUPI: EditText
    private lateinit var etCardNumber: EditText
    private lateinit var etExpiry: EditText
    private lateinit var etCVV: EditText

    private lateinit var userRef: DatabaseReference
    private lateinit var addressesRef: DatabaseReference
    private lateinit var cartRef: DatabaseReference
    private lateinit var orderRef: DatabaseReference

    private var subtotalAmount = 0.0
    private var deliveryCharge = DEFAULT_DELIVERY_CHARGE
    private var discountAmount = 0.0
    private var payableAmount = 0.0
    private var totalQuantity = 0
    private var productList = ""
    private var appliedCouponCode: String? = null
    private var isFirstOrder = false

    private var userName = ""
    private var userPhone = ""
    private var userEmail = ""
    private var currentUid: String = ""
    private var pendingAddress: String = ""
    private var pendingPaymentMethod: String = "Cash on Delivery"
    private var pendingCardPayment = false

    private val savedAddresses = mutableListOf<SavedAddress>()
    private val spinnerLabels = mutableListOf<String>()

    private val upiPaymentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK || it.resultCode == RESULT_CANCELED) {
                placeOrder(pendingAddress, pendingPaymentMethod)
            } else {
                Toast.makeText(this, "UPI payment failed/cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
        findViewById<View>(android.R.id.content).applySystemBarInsets()
        Checkout.preload(applicationContext)

        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvPhone = findViewById(R.id.tvPhone)
        tvEmail = findViewById(R.id.tvEmail)

        spinnerAddress = findViewById(R.id.spinnerAddress)
        layoutNewAddress = findViewById(R.id.layoutNewAddress)
        etLabel = findViewById(R.id.etAddressLabel)
        etAddress = findViewById(R.id.etAddress)
        etCity = findViewById(R.id.etCity)
        etPincode = findViewById(R.id.etPincode)

        tvProductNames = findViewById(R.id.tvProductNames)
        tvTotalQuantity = findViewById(R.id.tvTotalQuantity)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvDeliveryCharge = findViewById(R.id.tvDeliveryCharge)
        tvDiscount = findViewById(R.id.tvDiscount)
        tvFinalAmount = findViewById(R.id.tvFinalAmount)
        tvOrderNote = findViewById(R.id.tvOrderNote)

        paymentGroup = findViewById(R.id.paymentMethodGroup)
        btnConfirm = findViewById(R.id.btnConfirmOrder)

        etUPI = findViewById(R.id.etUPI)
        etCardNumber = findViewById(R.id.etCardNumber)
        etExpiry = findViewById(R.id.etExpiry)
        etCVV = findViewById(R.id.etCVV)

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUid = uid

        appliedCouponCode = CartCouponPrefs.getAppliedCode(this, uid)
            ?: intent.getStringExtra(EXTRA_COUPON_CODE)?.trim()?.takeIf { it.isNotEmpty() }

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
        addressesRef = userRef.child("addresses")
        cartRef = FirebaseDatabase.getInstance().getReference("cart").child(uid)
        orderRef = FirebaseDatabase.getInstance().getReference("Orders")
        checkFirstOrderEligibility()

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@PaymentActivity, "User data not found", Toast.LENGTH_LONG).show()
                    return
                }

                val user = snapshot.getValue(User::class.java)

                if (user == null) {
                    Toast.makeText(this@PaymentActivity, "User error", Toast.LENGTH_SHORT).show()
                    return
                }

                userName = user.name ?: ""
                userPhone = user.phone ?: ""
                userEmail = user.email ?: ""

                tvCustomerName.text = userName
                tvPhone.text = userPhone
                tvEmail.text = userEmail
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PaymentActivity, "User load failed", Toast.LENGTH_SHORT).show()
            }
        })

        loadAddressesIntoSpinner()

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                subtotalAmount = 0.0
                totalQuantity = 0
                productList = ""

                for (item in snapshot.children) {
                    val cartItem = item.getValue(CartItem::class.java)
                    val name = cartItem?.name ?: ""
                    /** [CartItem.price] is already line total (unit × qty), same as CartFragment. */
                    val lineTotal = cartItem?.price ?: 0.0
                    val quantity = cartItem?.quantity ?: 0

                    subtotalAmount += lineTotal
                    totalQuantity += quantity

                    productList += "$name x$quantity, "
                }

                tvProductNames.text = "Products: $productList"
                tvTotalQuantity.text = "Total quantity: $totalQuantity"
                recalculatePayable()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PaymentActivity, "Cart load failed", Toast.LENGTH_SHORT).show()
            }
        })

        paymentGroup.setOnCheckedChangeListener { _, checkedId ->
            etUPI.visibility = View.GONE
            etCardNumber.visibility = View.GONE
            etExpiry.visibility = View.GONE
            etCVV.visibility = View.GONE

            when (checkedId) {
                R.id.rbUPI -> etUPI.visibility = View.VISIBLE
                R.id.rbCard -> {
                    etCardNumber.visibility = View.VISIBLE
                    etExpiry.visibility = View.VISIBLE
                    etCVV.visibility = View.VISIBLE
                }
            }
        }

        spinnerAddress.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val label = spinnerLabels.getOrNull(position) ?: return
                layoutNewAddress.visibility =
                    if (label == ADD_NEW_ADDRESS) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnConfirm.setOnClickListener {
            if (userName.isEmpty() || userPhone.isEmpty() || userEmail.isEmpty()) {
                Toast.makeText(this, "User data not loaded", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fullAddress = resolveDeliveryAddress()
            if (fullAddress == null) {
                Toast.makeText(this, "Select or enter a delivery address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedId = paymentGroup.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pendingAddress = fullAddress

            when (selectedId) {
                R.id.rbCOD -> {
                    pendingPaymentMethod = "Cash on Delivery"
                    placeOrder(fullAddress, pendingPaymentMethod)
                }
                R.id.rbCard -> {
                    if (!InputValidators.isValidCardNumber(etCardNumber.text.toString().trim())) {
                        Toast.makeText(this, "Enter valid card details", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    pendingPaymentMethod = "Card Payment"
                    pendingCardPayment = true
                    launchCardCheckout()
                }
                R.id.rbUPI -> {
                    pendingPaymentMethod = "UPI Payment"
                    launchUpiIntent()
                }
            }
        }
    }

    private fun checkFirstOrderEligibility() {
        orderRef.orderByChild("userId").equalTo(currentUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFirstOrder = !snapshot.exists()
                    deliveryCharge = if (isFirstOrder) 0.0 else DEFAULT_DELIVERY_CHARGE
                    recalculatePayable()
                }

                override fun onCancelled(error: DatabaseError) {
                    isFirstOrder = false
                    deliveryCharge = DEFAULT_DELIVERY_CHARGE
                    recalculatePayable()
                }
            })
    }

    private fun recalculatePayable() {
        var promo = CouponCatalog.apply(appliedCouponCode, subtotalAmount, deliveryCharge, isFirstOrder)
        var couponWarning: String? = null
        if (!promo.success && !appliedCouponCode.isNullOrBlank()) {
            couponWarning = promo.message
            appliedCouponCode = null
            CartCouponPrefs.setAppliedCode(this, currentUid, null)
            promo = CouponCatalog.apply(null, subtotalAmount, deliveryCharge, isFirstOrder)
        }

        discountAmount = if (promo.success) promo.totalDiscount else 0.0
        payableAmount = CouponCatalog.payableAmount(subtotalAmount, deliveryCharge, discountAmount)

        tvOrderNote.text = couponWarning ?: when {
            isFirstOrder -> "First order: free delivery is included."
            else -> "Totals use your cart subtotal and coupon from the cart screen."
        }

        tvTotalAmount.text = "Items total: ₹${formatAmount(subtotalAmount)}"
        tvDeliveryCharge.text = if (deliveryCharge == 0.0) {
            "Delivery: FREE"
        } else {
            "Delivery: ₹${formatAmount(deliveryCharge)}"
        }
        tvDiscount.text = "Coupon savings: -₹${formatAmount(discountAmount)}"
        tvFinalAmount.text = "Amount payable: ₹${formatAmount(payableAmount)}"
    }

    private fun loadAddressesIntoSpinner() {
        addressesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                savedAddresses.clear()
                spinnerLabels.clear()
                spinnerLabels.add(CHOOSE_SAVED)
                for (child in snapshot.children) {
                    val a = child.getValue(SavedAddress::class.java) ?: continue
                    val id = child.key
                    if (id != null) {
                        a.id = id
                        savedAddresses.add(a)
                        spinnerLabels.add(a.label?.ifBlank { "Saved" } + ": " + a.fullLine())
                    }
                }
                spinnerLabels.add(ADD_NEW_ADDRESS)

                val adapter = ArrayAdapter(
                    this@PaymentActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    spinnerLabels
                )
                spinnerAddress.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun resolveDeliveryAddress(): String? {
        val pos = spinnerAddress.selectedItemPosition
        val label = spinnerLabels.getOrNull(pos) ?: return null

        return when {
            pos == 0 || label == CHOOSE_SAVED -> null
            label == ADD_NEW_ADDRESS -> {
                val line = etAddress.text.toString().trim()
                val city = etCity.text.toString().trim()
                val pin = etPincode.text.toString().trim()
                val lbl = etLabel.text.toString().trim().ifBlank { "Home" }
                if (line.isEmpty() || city.isEmpty() || pin.isEmpty()) return null
                val full = "$line, $city, $pin"
                val key = addressesRef.push().key ?: return full
                val sa = SavedAddress(key, lbl, line, city, pin)
                addressesRef.child(key).setValue(sa)
                full
            }
            else -> savedAddresses.getOrNull(pos - 1)?.fullLine()
        }
    }

    private fun launchUpiIntent() {
        // Hard lock payee to requested destination so funds go to MediBee business account.
        val fixedPayeeUpi = FIXED_UPI_PAYEE
        val upiUri = Uri.parse(
            "upi://pay?pa=${encode(fixedPayeeUpi)}&pn=${encode("MediBee")}&tn=${encode("Medicine Order")}&am=${formatAmount(payableAmount)}&cu=INR"
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = upiUri
        }
        val chooser = Intent.createChooser(intent, "Pay with UPI app")
        if (intent.resolveActivity(packageManager) != null) {
            upiPaymentLauncher.launch(chooser)
        } else {
            Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCardCheckout() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_replace_with_live_key")
        val options = JSONObject().apply {
            put("name", "MediBee")
            put("description", "Medicine order payment")
            put("currency", "INR")
            put("amount", (payableAmount * 100).toInt())
            put("prefill", JSONObject().apply {
                put("email", userEmail)
                put("contact", userPhone)
            })
        }
        try {
            checkout.open(this, options)
        } catch (e: Exception) {
            pendingCardPayment = false
            Toast.makeText(this, "Card checkout failed to start", Toast.LENGTH_SHORT).show()
            Log.e("PaymentActivity", "Razorpay launch failed", e)
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        if (pendingCardPayment) {
            pendingCardPayment = false
            placeOrder(pendingAddress, "$pendingPaymentMethod [Txn:${razorpayPaymentId ?: "N/A"}]")
        }
    }

    override fun onPaymentError(code: Int, message: String?, data: PaymentData?) {
        pendingCardPayment = false
        Toast.makeText(this, "Card payment failed: ${message ?: code}", Toast.LENGTH_SHORT).show()
    }

    private fun placeOrder(fullAddress: String, paymentMethod: String) {
        val orderId = orderRef.push().key ?: return
        val couponLabel = appliedCouponCode?.takeIf { it.isNotBlank() }
        val order = Order(
            orderId = orderId,
            customerName = userName,
            address = fullAddress,
            items = productList,
            totalPrice = payableAmount,
            userId = currentUid,
            deliveryBoyId = "",
            status = "Pending",
            createdAt = System.currentTimeMillis(),
            paymentMethod = if (couponLabel.isNullOrBlank()) {
                paymentMethod
            } else {
                "$paymentMethod ($couponLabel)"
            },
            subtotalPrice = subtotalAmount,
            deliveryCharge = deliveryCharge,
            couponCode = couponLabel,
            couponDiscount = discountAmount
        )
        orderRef.child(orderId).setValue(order)
            .addOnSuccessListener {
                cartRef.removeValue()
                CartCouponPrefs.setAppliedCode(this, currentUid, null)
                lifecycleScope.launch {
                    OrderNotificationService.notifyOrderPlaced(
                        orderId = orderId,
                        phone = userPhone,
                        title = "Order Confirmed",
                        body = "Your MediBee order #$orderId for ₹${formatAmount(payableAmount)} is placed."
                    )
                }
                Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun formatAmount(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

    companion object {
        const val CHOOSE_SAVED = "Choose saved address"
        const val ADD_NEW_ADDRESS = "+ Add new address"
        const val DEFAULT_DELIVERY_CHARGE = 40.0

        const val EXTRA_PAYABLE_AMOUNT = "PAYABLE_AMOUNT"
        const val EXTRA_ITEMS_SUBTOTAL = "ITEMS_SUBTOTAL"
        const val EXTRA_DELIVERY_CHARGE = "DELIVERY_CHARGE"
        const val EXTRA_COUPON_DISCOUNT = "COUPON_DISCOUNT"
        const val EXTRA_COUPON_CODE = "COUPON_CODE"
        const val FIXED_UPI_PAYEE = "9824355859@paytm"
    }
}
