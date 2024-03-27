package com.example.snitchsms.firebase

data class MessageModel(
    val id: String = "",
    val receiptName: String = "",
    val receiptPhoneNumber: String = "",
    val message: String = "",
    val date: String = "",
    val time: String = ""
) {
    // Add a no-argument constructor
    constructor() : this("", "", "", "", "", "")
}

