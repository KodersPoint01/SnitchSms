package com.example.snitchsms.roomdatabase

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.snitchsms.contacts.model.SmsSaveModel

class SmsRepository(context: Context) {


    companion object {
        @Volatile
        private var instance: SmsRepository? = null
        init {
            Log.d("smsRepository", "Database initialized")
        }
        fun getInstance(context: Context): SmsRepository {
            return instance ?: synchronized(this) {
                instance ?: SmsRepository(context).also { instance = it }
            }
        }
    }

    private var db: SmsDao = SmsDatabase.getDatabase(context).jsonModelDao()

    fun getAll(): LiveData<List<SmsSaveModel>> {
        return db.getAllSms()
    }

     fun insert(model: SmsSaveModel) {
        db.insertSms(model)
    }
    suspend fun deleteSelectedItems(selectedItems: List<SmsSaveModel>) {
        db.deleteItems(selectedItems)
    }
    suspend fun deleteMessagesForContact(contactName: String) {
        db.deleteMessagesForContact(contactName)
    }
    suspend fun blockContact(phoneNumber: String) {
        // Update the database to mark the contact as blocked
        db.blockContact(phoneNumber)
    }
    suspend fun unblockContact(contactName: String) {
        db.unblockContact(contactName)
    }
    suspend fun isContactBlocked(contactName: String): Boolean {
        return db.isContactBlocked(contactName)
    }
    suspend fun delete() {
        db.deleteAll()
    }

    suspend fun deleteItem(item: SmsSaveModel) {
        db.deleteItem(item)
    }

}