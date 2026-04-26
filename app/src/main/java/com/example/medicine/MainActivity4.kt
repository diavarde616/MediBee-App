package com.example.medicine

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout

class MainActivity4 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main4)

        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.applyNavIcons(
            intArrayOf(
                R.drawable.ic_tab_home,
                R.drawable.ic_tab_favorite,
                R.drawable.ic_tab_person,
                R.drawable.ic_tab_cart
            ),
            arrayOf("Home", "Wishlist", "Profile", "Cart")
        )

        if (savedInstanceState == null) {
            tabLayout.getTabAt(0)?.select()
            replaceFragment(HomeFragment())
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val selectedFragment = when (tab?.position){
                    0 -> HomeFragment()
                    1 -> WishlistFragment()
                    2 -> ProfileFragment()
                    3 -> CartFragment()
                    else -> HomeFragment()
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

    fun selectTab(index: Int) {
        val tabLayout: TabLayout = findViewById(R.id.tabLayout)
        tabLayout.getTabAt(index)?.select()
    }
}