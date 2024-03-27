package com.example.snitchsms.roomdatabase

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.snitchsms.contacts.model.SmsSaveModel

@Dao
interface SmsDao {

    @Insert
    fun insertSms(model: SmsSaveModel)

    @Query("SELECT * FROM sms_save")
    fun getAllSms(): LiveData<List<SmsSaveModel>>

    @Query("DELETE FROM sms_save")
    fun deleteAll()

    @Delete
     fun deleteItems(items: List<SmsSaveModel>)

    @Query("UPDATE sms_save SET isBlocked = 1 WHERE receiptName = :contactName")
     fun blockContact(contactName: String)

    @Query("UPDATE sms_save SET isBlocked = 0 WHERE receiptName = :contactName")
     fun unblockContact(contactName: String)

    @Query("SELECT isBlocked FROM sms_save WHERE receiptName = :contactName LIMIT 1")
     fun isContactBlocked(contactName: String): Boolean

    @Query("DELETE FROM sms_save WHERE receiptName = :contactName")
      fun deleteMessagesForContact(contactName: String)

    @Delete
    fun deleteItem(item: SmsSaveModel)
}