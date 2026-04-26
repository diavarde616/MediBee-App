package com.example.medicine

import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.tabs.TabLayout

fun TabLayout.applyNavIcons(icons: IntArray, labels: Array<String>) {
    val n = minOf(tabCount, icons.size, labels.size)
    for (i in 0 until n) {
        val tab = getTabAt(i) ?: continue
        val v = LayoutInflater.from(context).inflate(R.layout.tab_nav_item, this, false)
        v.findViewById<ImageView>(R.id.tabIcon).setImageResource(icons[i])
        v.findViewById<TextView>(R.id.tabLabel).text = labels[i]
        tab.customView = v
    }
}
