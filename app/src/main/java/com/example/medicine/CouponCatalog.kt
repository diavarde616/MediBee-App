package com.example.medicine

import java.util.Locale

enum class PromoCouponType {
    PERCENTAGE,
    FLAT,
    FREE_DELIVERY
}

data class PromoCoupon(
    val code: String,
    val type: PromoCouponType,
    val value: Double,
    val minOrder: Double,
    val maxDiscount: Double? = null,
    val firstOrderOnly: Boolean = false,
    val title: String,
    val description: String
)

/**
 * Line items in Firebase store [CartItem.price] as the full line total (unit × qty), not unit price.
 * All subtotals here use that convention.
 */
data class AppliedPromoResult(
    val success: Boolean,
    val message: String,
    /** Total rupees off (product + delivery waiver), capped so payable never goes negative incorrectly */
    val totalDiscount: Double = 0.0
)

object CouponCatalog {

    val allCoupons: List<PromoCoupon> = listOf(
        PromoCoupon(
            "MEDI15", PromoCouponType.PERCENTAGE, 15.0, 299.0, 150.0, false,
            "15% off", "Up to ₹150 off on orders above ₹299"
        ),
        PromoCoupon(
            "SAVE20", PromoCouponType.PERCENTAGE, 20.0, 499.0, 250.0, false,
            "20% saver", "Up to ₹250 off on orders above ₹499"
        ),
        PromoCoupon(
            "HEALTH10", PromoCouponType.PERCENTAGE, 10.0, 199.0, 80.0, false,
            "10% wellness", "Up to ₹80 off on orders above ₹199"
        ),
        PromoCoupon(
            "FLAT50", PromoCouponType.FLAT, 50.0, 399.0, null, false,
            "₹50 flat", "Flat ₹50 off on orders above ₹399"
        ),
        PromoCoupon(
            "FLAT100", PromoCouponType.FLAT, 100.0, 799.0, null, false,
            "₹100 flat", "Flat ₹100 off on orders above ₹799"
        ),
        PromoCoupon(
            "FLAT200", PromoCouponType.FLAT, 200.0, 1499.0, null, false,
            "₹200 flat", "Flat ₹200 off on orders above ₹1499"
        ),
        PromoCoupon(
            "FREESHIP", PromoCouponType.FREE_DELIVERY, 0.0, 249.0, null, false,
            "Free delivery", "Waive delivery fee on orders above ₹249"
        ),
        PromoCoupon(
            "SHIP0", PromoCouponType.FREE_DELIVERY, 0.0, 99.0, null, false,
            "Zero shipping", "Free delivery on orders above ₹99"
        ),
        PromoCoupon(
            "FIRST60", PromoCouponType.FLAT, 60.0, 349.0, null, true,
            "First order ₹60", "₹60 off your first order above ₹349"
        ),
        PromoCoupon(
            "WELCOME25", PromoCouponType.PERCENTAGE, 25.0, 0.0, 100.0, true,
            "Welcome 25%", "New users: 25% off (max ₹100)"
        ),
        PromoCoupon(
            "NIGHT12", PromoCouponType.PERCENTAGE, 12.0, 599.0, 120.0, false,
            "Night owl 12%", "12% off late orders above ₹599"
        ),
        PromoCoupon(
            "EXTRA5", PromoCouponType.PERCENTAGE, 5.0, 149.0, 40.0, false,
            "Extra 5%", "5% off on orders above ₹149"
        ),
        PromoCoupon(
            "BULK75", PromoCouponType.FLAT, 75.0, 999.0, null, false,
            "Bulk ₹75", "₹75 off large carts above ₹999"
        ),
        PromoCoupon(
            "CARE30", PromoCouponType.FLAT, 30.0, 449.0, null, false,
            "Care pack", "₹30 off on orders above ₹449"
        )
    )

    private val byCode: Map<String, PromoCoupon> =
        allCoupons.associateBy { it.code.uppercase(Locale.getDefault()) }

    fun find(code: String?): PromoCoupon? {
        if (code.isNullOrBlank()) return null
        return byCode[code.trim().uppercase(Locale.getDefault())]
    }

    fun apply(
        code: String?,
        subtotalLineItems: Double,
        deliveryCharge: Double,
        isFirstOrder: Boolean
    ): AppliedPromoResult {
        if (code.isNullOrBlank()) {
            return AppliedPromoResult(true, "", 0.0)
        }
        val coupon = find(code)
            ?: return AppliedPromoResult(false, "Invalid coupon code", 0.0)
        if (subtotalLineItems <= 0.0) {
            return AppliedPromoResult(false, "Add items to cart before applying a coupon", 0.0)
        }
        if (subtotalLineItems < coupon.minOrder) {
            return AppliedPromoResult(
                false,
                "Minimum order for ${coupon.code} is ₹${formatMoney(coupon.minOrder)}",
                0.0
            )
        }
        if (coupon.firstOrderOnly && !isFirstOrder) {
            return AppliedPromoResult(false, "${coupon.code} is only for your first order", 0.0)
        }

        val deliveryDiscount = when (coupon.type) {
            PromoCouponType.FREE_DELIVERY -> deliveryCharge
            else -> 0.0
        }
        val productDiscount = when (coupon.type) {
            PromoCouponType.PERCENTAGE -> {
                val raw = (subtotalLineItems * coupon.value) / 100.0
                minOf(raw, coupon.maxDiscount ?: raw)
            }
            PromoCouponType.FLAT -> coupon.value
            PromoCouponType.FREE_DELIVERY -> 0.0
        }
        val subtotalAfterProduct = maxOf(subtotalLineItems - productDiscount, 0.0)
        val totalDiscount = minOf(
            productDiscount + deliveryDiscount,
            subtotalAfterProduct + deliveryCharge
        )
        return AppliedPromoResult(
            success = true,
            message = "Applied ${coupon.code}",
            totalDiscount = totalDiscount
        )
    }

    fun payableAmount(
        subtotalLineItems: Double,
        deliveryCharge: Double,
        totalDiscount: Double
    ): Double =
        maxOf(subtotalLineItems + deliveryCharge - totalDiscount, 0.0)

    fun formatMoney(value: Double): String =
        String.format(Locale.getDefault(), "%.2f", value)
}
