package com.example.medicine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment

class CategoryFragment : Fragment() {

    private lateinit var ivInjection: ImageView
    private lateinit var ivCapsule: ImageView
    private lateinit var ivSyrup: ImageView
    private lateinit var ivTube: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_category, container, false)

        ivInjection = view.findViewById(R.id.iv3)
        ivCapsule = view.findViewById(R.id.iv4)
        ivSyrup = view.findViewById(R.id.iv5)
        ivTube = view.findViewById(R.id.iv6)

        ivInjection.setOnClickListener {
            openCategory("Injection")
        }

        ivCapsule.setOnClickListener {
            openCategory("Capsule")
        }

        ivSyrup.setOnClickListener {
            openCategory("Syrup")
        }

        ivTube.setOnClickListener {
            openCategory("Tube")
        }

        return view
    }

    private fun openCategory(category: String) {

        val intent = Intent(requireContext(), MainActivity10::class.java)
        intent.putExtra("category", category)

        startActivity(intent)
    }
}