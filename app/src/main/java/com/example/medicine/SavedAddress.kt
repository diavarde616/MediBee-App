package com.example.medicine

data class SavedAddress(
    var id: String? = null,
    var label: String? = null,
    var line1: String? = null,
    var city: String? = null,
    var pincode: String? = null
) {
    fun fullLine(): String = listOfNotNull(line1?.takeIf { it.isNotBlank() }, city?.takeIf { it.isNotBlank() }, pincode?.takeIf { it.isNotBlank() })
        .joinToString(", ")
}
