package com.example.medicine

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class MainActivity5 : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main5)

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.applyNavIcons(
            intArrayOf(
                R.drawable.ic_tab_inventory,
                R.drawable.ic_tab_notifications,
                R.drawable.ic_tab_shipping
            ),
            arrayOf("Products", "Stock", "Delivery")
        )

        if (savedInstanceState == null) {
            tabLayout.getTabAt(0)?.select()
            replaceFragment(productFragment())
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedFragment = when (tab?.position){
                    0 -> productFragment()
                    1 -> StockFragment()
                    2 -> notifdeliveryFragment()
                    else -> productFragment()
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
            .replace(R.id.frag, fragment)
            .commit()
    }
}