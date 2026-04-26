package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class registeration : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnVerifyPhoneOtp: Button
    private lateinit var tvPhoneVerificationStatus: TextView

    private lateinit var auth: FirebaseAuth
    private var verifiedPhoneNumber: String? = null
    private val otpVerificationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val phone = result.data?.getStringExtra(OtpVerificationActivity.EXTRA_VERIFIED_PHONE)
                if (!phone.isNullOrBlank()) {
                    verifiedPhoneNumber = phone
                    tvPhoneVerificationStatus.text = "Phone verified: $phone"
                    tvPhoneVerificationStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registeration)
        findViewById<android.view.View>(android.R.id.content).applySystemBarInsets()

        auth = FirebaseAuth.getInstance()

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnVerifyPhoneOtp = findViewById(R.id.btnVerifyPhoneOtp)
        tvPhoneVerificationStatus = findViewById(R.id.tvPhoneVerificationStatus)

        btnVerifyPhoneOtp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (!InputValidators.isValidIndianPhone(phone)) {
                etPhone.error = "Enter valid phone"
                return@setOnClickListener
            }
            otpVerificationLauncher.launch(
                Intent(this, OtpVerificationActivity::class.java)
                    .putExtra(OtpVerificationActivity.EXTRA_PHONE_NUMBER, phone)
            )
        }

        btnRegister.setOnClickListener {

            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (name.isEmpty()) {
                etName.error = "Enter Name"
                return@setOnClickListener
            }

            if (!InputValidators.isValidIndianPhone(phone)) {
                etPhone.error = "Enter valid phone"
                return@setOnClickListener
            }

            if (verifiedPhoneNumber != phone) {
                Toast.makeText(this, "Verify mobile number via OTP first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!InputValidators.isValidEmail(email)) {
                etEmail.error = "Enter valid email"
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "Min 6 characters"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Password not match"
                return@setOnClickListener
            }

            btnRegister.isEnabled = false  // 🔥 prevent double click

            registerUser(name, verifiedPhoneNumber ?: phone, email, password)
        }
    }

    private fun registerUser(name: String, phone: String, email: String, password: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                val uid = auth.currentUser?.uid

                if (uid == null) {
                    Toast.makeText(this, "User creation failed", Toast.LENGTH_SHORT).show()
                    btnRegister.isEnabled = true
                    return@addOnSuccessListener
                }

                val user = User(
                    uid = uid,
                    name = name,
                    address = "",
                    email = email,
                    phone = phone,
                    password = ""   // 🔥 DO NOT STORE PASSWORD
                )

                FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(uid)
                    .setValue(user)
                    .addOnSuccessListener {

                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, login::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        btnRegister.isEnabled = true
                        Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->

                btnRegister.isEnabled = true

                if (e is FirebaseAuthUserCollisionException) {

                    Toast.makeText(
                        this,
                        "Email already registered, please login",
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(Intent(this, login::class.java))
                    finish()

                } else {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            }
    }
}