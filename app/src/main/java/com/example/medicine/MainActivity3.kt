package com.example.medicine

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class MainActivity3 : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.applyNavIcons(
            intArrayOf(
                R.drawable.ic_tab_inbox,
                R.drawable.ic_tab_shipping,
                R.drawable.ic_tab_person
            ),
            arrayOf("New", "Active", "Profile")
        )

        if (savedInstanceState == null) {
            tabLayout.getTabAt(0)?.select()
            replaceFragment(OrderFragment())
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedFragment = when (tab?.position){
                    0 -> OrderFragment()
                    1 -> ActiveFragment()
                    2 -> ProfileDlvryFragment()
                    else -> OrderFragment()
                }
                replaceFragment(selectedFragment)
            }
            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }
            override fun onTabReselected(p0: TabLayout.Tab?) {
            }
        })

    }

    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragement, fragment)
            .commit()
    }
}