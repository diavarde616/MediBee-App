package com.example.medicine

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val usersRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("Users")
    }
    private var userListener: ValueEventListener? = null
    private var ivProfile: ImageView? = null
    private var currentUid: String? = null

    private val pickGalleryImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                uploadProfileBitmap(bitmap)
            }
        }

    private val captureCameraImage =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                uploadProfileBitmap(bitmap)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        ivProfile = view.findViewById(R.id.ivProfile)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvPhone = view.findViewById<TextView>(R.id.tvPhone)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val loading = view.findViewById<CircularProgressIndicator>(R.id.profileLoading)
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnMyOrders = view.findViewById<MaterialButton>(R.id.btnMyOrders)
        val btnSavedAddresses = view.findViewById<MaterialButton>(R.id.btnSavedAddresses)
        val btnSupport = view.findViewById<MaterialButton>(R.id.btnSupport)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnLogout)

        loading.visibility = View.VISIBLE
        currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            loading.visibility = View.GONE
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), login::class.java))
            requireActivity().finishAffinity()
            return view
        }

        ivProfile?.setOnClickListener { showImagePickOptions() }

        userListener = usersRef.child(currentUid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                loading.visibility = View.GONE
                if (!snapshot.exists()) {
                    tvName.text = "Welcome"
                    tvPhone.text = "Phone not available"
                    tvEmail.text = auth.currentUser?.email ?: "Email not available"
                    return
                }

                val user = snapshot.getValue(User::class.java)
                tvName.text = user?.name?.ifBlank { "Welcome" } ?: "Welcome"
                tvPhone.text = user?.phone?.ifBlank { "Phone not available" } ?: "Phone not available"
                tvEmail.text = user?.email?.ifBlank { "Email not available" } ?: "Email not available"
                user?.profileImage?.let { encoded ->
                    ImageCodec.base64ToBitmap(encoded)?.let { bitmap ->
                        ivProfile?.setImageBitmap(bitmap)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                loading.visibility = View.GONE
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        })

        btnEditProfile.setOnClickListener {
            showImagePickOptions()
        }
        btnMyOrders.setOnClickListener {
            startActivity(Intent(requireContext(), UserOrderHistoryActivity::class.java))
        }
        btnSavedAddresses.setOnClickListener {
            Toast.makeText(requireContext(), "Saved address management coming next", Toast.LENGTH_SHORT).show()
        }
        btnSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Call support: +91-9876543210", Toast.LENGTH_SHORT).show()
        }
        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), login::class.java))
            requireActivity().finishAffinity()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (currentUid != null && userListener != null) {
            usersRef.child(currentUid!!).removeEventListener(userListener!!)
        }
        userListener = null
        ivProfile = null
    }

    private fun showImagePickOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Update Profile Picture")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> captureCameraImage.launch(null)
                    1 -> pickGalleryImage.launch("image/*")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadProfileBitmap(bitmap: Bitmap) {
        val uid = currentUid ?: return
        val encoded = ImageCodec.bitmapToBase64(bitmap, 65)
        usersRef.child(uid).child("profileImage").setValue(encoded)
            .addOnSuccessListener {
                ivProfile?.setImageBitmap(bitmap)
                Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }
}