package com.example.medicine

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

class productFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val t1: TextView = view.findViewById(R.id.t1)
        val b3: Button = view.findViewById(R.id.b3)
        val b4: Button = view.findViewById(R.id.b4)
        val b5: Button = view.findViewById(R.id.b5)

        b3.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity6::class.java)
            startActivity(intent)
        }

        b4.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity9::class.java)
            startActivity(intent)
        }

        b5.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity10    ::class.java)
            startActivity(intent)
        }
    }
}
