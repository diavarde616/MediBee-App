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
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileDlvryFragment : Fragment() {

    private val deliveryRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("DeliveryBoys")
    }
    private val ordersRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("Orders")
    }
    private var deliveryListener: ValueEventListener? = null
    private var orderListener: ValueEventListener? = null
    private var deliveryBoyId: String? = null
    private var ivDeliveryProfile: ImageView? = null

    private val pickGalleryImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                uploadDeliveryImage(bitmap)
            }
        }

    private val captureCameraImage =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                uploadDeliveryImage(bitmap)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile_dlvry, container, false)
        val progress = view.findViewById<CircularProgressIndicator>(R.id.progressDeliveryProfile)
        val tvName = view.findViewById<TextView>(R.id.tvDeliveryName)
        val tvPhone = view.findViewById<TextView>(R.id.tvDeliveryPhone)
        val tvVehicle = view.findViewById<TextView>(R.id.tvVehicle)
        val tvCompletedCount = view.findViewById<TextView>(R.id.tvCompletedCount)
        val tvEarnings = view.findViewById<TextView>(R.id.tvEarnings)
        val switchAvailability = view.findViewById<MaterialSwitch>(R.id.switchAvailability)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnDeliveryLogout)

        val authUid = FirebaseAuth.getInstance().currentUser?.uid
        val cachedDeliveryBoyId = requireContext().getSharedPreferences("session", 0)
            .getString("deliveryBoyId", null)
        deliveryBoyId = cachedDeliveryBoyId ?: authUid

        if (deliveryBoyId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Please login again", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), login::class.java))
            requireActivity().finishAffinity()
            return view
        }

        ivDeliveryProfile = view.findViewById(R.id.ivDeliveryProfile)
        ivDeliveryProfile?.setOnClickListener { showImagePickOptions() }

        progress.visibility = View.VISIBLE
        deliveryListener = deliveryRef.child(deliveryBoyId!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                progress.visibility = View.GONE
                val profile = snapshot.getValue(DeliveryBoy::class.java)
                tvName.text = profile?.name ?: "Delivery Partner"
                tvPhone.text = profile?.phone ?: "Phone not available"
                tvVehicle.text = "Vehicle: ${profile?.vehicle?.ifBlank { "Not added" } ?: "Not added"}"
                switchAvailability.isChecked = (profile?.status ?: "Active").equals("Active", true)
                profile?.profileImage?.let { encoded ->
                    ImageCodec.base64ToBitmap(encoded)?.let { bitmap ->
                        ivDeliveryProfile?.setImageBitmap(bitmap)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        })

        orderListener = ordersRef.orderByChild("deliveryBoyId").equalTo(deliveryBoyId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var completed = 0
                    var earnings = 0.0
                    for (child in snapshot.children) {
                        val order = child.getValue(Order::class.java)
                        if (order?.status.equals("Delivered", true)) {
                            completed++
                            earnings += order?.totalPrice ?: 0.0
                        }
                    }
                    tvCompletedCount.text = completed.toString()
                    tvEarnings.text = "₹%.0f".format(earnings)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            val value = if (isChecked) "Active" else "Offline"
            deliveryRef.child(deliveryBoyId!!).child("status").setValue(value)
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            requireContext().getSharedPreferences("session", 0).edit().remove("deliveryBoyId").apply()
            startActivity(Intent(requireContext(), login::class.java))
            requireActivity().finishAffinity()
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!deliveryBoyId.isNullOrEmpty() && deliveryListener != null) {
            deliveryRef.child(deliveryBoyId!!).removeEventListener(deliveryListener!!)
        }
        orderListener?.let { ordersRef.removeEventListener(it) }
        ivDeliveryProfile = null
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

    private fun uploadDeliveryImage(bitmap: Bitmap) {
        val id = deliveryBoyId ?: return
        val encoded = ImageCodec.bitmapToBase64(bitmap, 65)
        deliveryRef.child(id).child("profileImage").setValue(encoded)
            .addOnSuccessListener { ivDeliveryProfile?.setImageBitmap(bitmap) }
    }
}