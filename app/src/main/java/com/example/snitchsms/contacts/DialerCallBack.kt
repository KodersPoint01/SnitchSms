package com.numbertracker.phonelocator.locationfinder.calllogs

import com.example.snitchsms.contacts.model.ContactModel


interface DialerCallBack {
    fun setIntent(model: ContactModel)
}