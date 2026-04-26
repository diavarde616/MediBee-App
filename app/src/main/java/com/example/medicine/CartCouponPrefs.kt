package com.example.medicine

import android.content.Context

/**
 * Persists the applied coupon code for the current user so Cart and Payment stay in sync.
 */
object CartCouponPrefs {

    private const val PREFS = "medibee_cart_coupon"

    private fun keyCode(uid: String) = "coupon_code_$uid"

    fun getAppliedCode(context: Context, uid: String?): String? {
        if (uid.isNullOrBlank()) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(keyCode(uid), null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun setAppliedCode(context: Context, uid: String?, code: String?) {
        if (uid.isNullOrBlank()) return
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (code.isNullOrBlank()) e.remove(keyCode(uid))
        else e.putString(keyCode(uid), code.trim().uppercase())
        e.apply()
    }
}
