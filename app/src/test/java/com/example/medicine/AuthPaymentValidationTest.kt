package com.example.medicine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPaymentValidationTest {

    @Test
    fun `valid email passes`() {
        assertTrue(InputValidators.isValidEmail("user@medibee.in"))
    }

    @Test
    fun `invalid phone fails`() {
        assertFalse(InputValidators.isValidIndianPhone("12345"))
    }

    @Test
    fun `valid card length passes`() {
        assertTrue(InputValidators.isValidCardNumber("4111111111111111"))
    }

    @Test
    fun `invalid upi format fails`() {
        assertFalse(InputValidators.isValidUpiId("9824355859"))
    }
}
