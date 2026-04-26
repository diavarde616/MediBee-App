package com.example.medicine

import android.util.Patterns

object InputValidators {
    fun isValidEmail(email: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    fun isValidIndianPhone(phone: String): Boolean = phone.length == 10 && phone.all { it.isDigit() }
    fun isValidUpiId(upiId: String): Boolean = upiId.contains("@") && upiId.length >= 6
    fun isValidCardNumber(card: String): Boolean = card.filter { it.isDigit() }.length in 12..19
}
