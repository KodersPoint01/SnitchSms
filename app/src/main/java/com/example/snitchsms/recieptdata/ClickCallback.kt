package com.example.snitchsms.recieptdata

import java.text.FieldPosition

interface ClickCallback {
    fun itemClick(position: Int)
    fun resetUnreadCounter(contactName: String)
    fun updateDeleteButtonVisibility()

}