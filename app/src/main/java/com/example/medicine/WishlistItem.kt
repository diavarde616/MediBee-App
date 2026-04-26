package com.example.medicine

data class WishlistItem(
    var productId: String? = null,
    var name: String? = null,
    var price: Double? = null,
    var image: String? = null,
    var category: String? = null,
    var stock: Int? = null,
    var description: String? = null,
    var uid: String? = null
)
