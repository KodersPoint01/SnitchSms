package com.example.snitchsms.contacts.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_save")
data class SmsSaveModel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val receiptName: String,
    val phoneNumber: String,
    val message: String,
    val date: String,
    val time: String,
    val side: String,
    val isBlocked: Boolean = false,
    var isSelected: Boolean = false


)