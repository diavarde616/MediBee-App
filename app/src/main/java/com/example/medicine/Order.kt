package com.example.medicine

data class Order(

    var orderId: String? = null,
    var customerName: String? = null,
    var address: String? = null,
    var items: String? = null,
    var totalPrice: Double? = null,
    var userId: String? = null,
    var deliveryBoyId: String? = null,
    var status: String? = null,
    var createdAt: Long? = null,
    var paymentMethod: String? = null,
    var subtotalPrice: Double? = null,
    var deliveryCharge: Double? = null,
    var couponCode: String? = null,
    var couponDiscount: Double? = null
)
