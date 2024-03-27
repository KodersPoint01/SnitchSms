package com.example.snitchsms.callbacks

interface ClickCallbackCounter {
    fun itemClick(position: Int)
    fun resetUnreadCounter(contactName: String)
}