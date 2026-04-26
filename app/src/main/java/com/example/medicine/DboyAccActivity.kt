package com.example.medicine

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class DboyAccActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var database: DatabaseReference
    private var deliveryBoyId: String? = null
    private var isEditMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dboy_acc)

        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etAddress = findViewById(R.id.etAddress)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegisterDeliveryBoy)
        database = FirebaseDatabase.getInstance().getReference("DeliveryBoys")
        isEditMode = intent.getBooleanExtra("isEditMode", false)
        deliveryBoyId = intent.getStringExtra("deliveryBoyId")

        if (isEditMode && !deliveryBoyId.isNullOrEmpty()) {
            title = "Edit Delivery Partner"
            btnRegister.text = "Update Delivery Boy"
            prefillDeliveryBoyData(deliveryBoyId!!)
        }

        btnRegister.setOnClickListener {
            if (isEditMode) {
                updateDeliveryBoy()
            } else {
                createDeliveryBoy()
            }
        }
    }

    private fun createDeliveryBoy() {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val id = database.push().key ?: return

        val deliveryBoy = DeliveryBoy(
            id = id,
            name = name,
            phone = phone,
            email = email,
            address = address,
            password = password,
            status = "Active"
        )

        database.child(id).setValue(deliveryBoy)
            .addOnSuccessListener {
                Toast.makeText(this, "Delivery Boy Added", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun prefillDeliveryBoyData(id: String) {
        database.child(id).get()
            .addOnSuccessListener { snapshot ->
                val deliveryBoy = snapshot.getValue(DeliveryBoy::class.java) ?: return@addOnSuccessListener
                etName.setText(deliveryBoy.name.orEmpty())
                etPhone.setText(deliveryBoy.phone.orEmpty())
                etEmail.setText(deliveryBoy.email.orEmpty())
                etAddress.setText(deliveryBoy.address.orEmpty())
                etPassword.setText(deliveryBoy.password.orEmpty())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDeliveryBoy() {
        val id = deliveryBoyId ?: return
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || address.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        database.child(id).get().addOnSuccessListener { snapshot ->
            val existing = snapshot.getValue(DeliveryBoy::class.java)
            val updated = DeliveryBoy(
                id = id,
                name = name,
                phone = phone,
                email = email,
                address = address,
                vehicle = existing?.vehicle,
                password = password,
                status = existing?.status ?: "Active",
                profileImage = existing?.profileImage
            )
            database.child(id).setValue(updated)
                .addOnSuccessListener {
                    Toast.makeText(this, "Delivery Boy Updated", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(this, "Unable to update data", Toast.LENGTH_SHORT).show()
        }
    }
}