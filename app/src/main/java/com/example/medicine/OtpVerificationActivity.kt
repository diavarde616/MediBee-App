package com.example.medicine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OtpVerificationActivity : AppCompatActivity() {
    private lateinit var etOtp: EditText
    private lateinit var btnVerifyOtp: Button
    private lateinit var tvOtpHint: TextView
    private var verificationId: String? = null
    private var phoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)
        findViewById<android.view.View>(android.R.id.content).applySystemBarInsets()

        etOtp = findViewById(R.id.etOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        tvOtpHint = findViewById(R.id.tvOtpHint)

        phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)?.trim().orEmpty()
        if (phoneNumber.length != 10) {
            Toast.makeText(this, "Invalid phone", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val e164Phone = "+91$phoneNumber"
        tvOtpHint.text = "OTP sent to $e164Phone"
        sendOtp(e164Phone)

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            val id = verificationId
            if (otp.length != 6 || id.isNullOrBlank()) {
                Toast.makeText(this, "Enter valid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyOtp(id, otp)
        }
    }

    private fun sendOtp(phone: String) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnSuccessListener { finishWithSuccess() }
                        .addOnFailureListener {
                            Toast.makeText(
                                this@OtpVerificationActivity,
                                "Auto verification failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        this@OtpVerificationActivity,
                        "OTP send failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@OtpVerificationActivity.verificationId = verificationId
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(id: String, otp: String) {
        val credential = PhoneAuthProvider.getCredential(id, otp)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { finishWithSuccess() }
            .addOnFailureListener {
                Toast.makeText(this, "OTP verification failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun finishWithSuccess() {
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(EXTRA_VERIFIED_PHONE, phoneNumber)
        )
        finish()
    }

    companion object {
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val EXTRA_VERIFIED_PHONE = "EXTRA_VERIFIED_PHONE"
    }
}
