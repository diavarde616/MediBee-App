package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*

class login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        Toast.makeText(this, "Google sign-in successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity4::class.java))
                        finishAffinity()
                    } else {
                        Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        findViewById<android.view.View>(android.R.id.content).applySystemBarInsets()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("DeliveryBoys")

        val etEmail = findViewById<EditText>(R.id.et6)
        val etPassword = findViewById<EditText>(R.id.et7)
        val btnLogin = findViewById<Button>(R.id.b2)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)
        val reg = findViewById<TextView>(R.id.tv8)

        // ✅ FIX: Removed finish()
        reg.setOnClickListener {
            startActivity(Intent(this, registeration::class.java))
        }

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ ADMIN LOGIN
            if (email == "diavarde123@gmail.com" && password == "123456") {
                getSharedPreferences("session", MODE_PRIVATE)
                    .edit()
                    .remove("deliveryBoyId")
                    .apply()
                startActivity(Intent(this, MainActivity5::class.java))
                finishAffinity()
                return@setOnClickListener
            }

            checkDeliveryBoy(email, password)
        }

        btnGoogleSignIn.setOnClickListener {
            startGoogleSignIn()
        }
    }

    private fun startGoogleSignIn() {
        val webClientId = getStringResourceByName("default_web_client_id")
        if (webClientId.isBlank()) {
            Toast.makeText(this, "Missing Firebase web client ID", Toast.LENGTH_LONG).show()
            return
        }

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(this, options)
        googleLauncher.launch(client.signInIntent)
    }

    private fun getStringResourceByName(name: String): String {
        val resId = resources.getIdentifier(name, "string", packageName)
        return if (resId != 0) getString(resId) else ""
    }

    private fun checkDeliveryBoy(email: String, password: String) {

        database.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                var isDeliveryBoy = false

                for (data in snapshot.children) {

                    val dbEmail = data.child("email").getValue(String::class.java)
                    val dbPassword = data.child("password").getValue(String::class.java)

                    if (email == dbEmail && password == dbPassword) {

                        isDeliveryBoy = true
                        val deliveryBoyId = data.key
                        getSharedPreferences("session", MODE_PRIVATE)
                            .edit()
                            .putString("deliveryBoyId", deliveryBoyId)
                            .apply()

                        Toast.makeText(this@login, "Delivery Boy Login", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this@login, MainActivity3::class.java))
                        finishAffinity()
                        break
                    }
                }

                if (!isDeliveryBoy) {
                    loginUser(email, password)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@login, "DB Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loginUser(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    getSharedPreferences("session", MODE_PRIVATE)
                        .edit()
                        .remove("deliveryBoyId")
                        .apply()

                    Toast.makeText(this, "User Login Successful", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(this, MainActivity4::class.java))
                    finishAffinity()

                } else {

                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
    }
}