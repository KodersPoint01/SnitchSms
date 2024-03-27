package com.example.snitchsms.incomingnotification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.snitchsms.AppData
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.contacts.smservice.SmsService
import com.example.snitchsms.firebase.MessageModel
import com.example.snitchsms.recieptdata.MessageListActivity
import com.example.snitchsms.roomdatabase.SmsDao
import com.example.snitchsms.roomdatabase.SmsRepository
import com.example.snitchsms.roomdatabase.SmsViewModel
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MyNotificationListenerService() : NotificationListenerService() {

    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    private lateinit var smsRepository: SmsRepository

    override fun onCreate() {
        super.onCreate()
        smsRepository = SmsRepository.getInstance(this)
    }


    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference
        if (sbn != null) {
            val notification = sbn.notification
            val title = notification.extras.getString(Notification.EXTRA_TITLE)
            val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)

            Log.d("TAG", "onNotificationPosted:title $title  text $text ")

//            Toast.makeText(this, "Recieved $title", Toast.LENGTH_SHORT).show()
            if (title == "File Deleted") {
                Toast.makeText(this, "$text", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.Main).launch {
                val storedPhone = getStoredPhoneNumber()

                val allMessages = AppData.database?.jsonModelDao()?.getAllSms()
                val data = allMessages?.value
                if (AppData.database != null) {
                    val allMessages: Unit? =
                        (AppData.database?.jsonModelDao()?.getAllSms()?.observeForever {
                            Log.d("TAG", "uploadAllMessagesToFirebase: allMessages 11 $it")

                            if (!storedPhone.isNullOrBlank()) {
                                uploadAllMessagesToFirebase(it, storedPhone)
                            }

                        })
                }

            }
        }
        }
    }

    private fun uploadAllMessagesToFirebase(smsSaveModels: List<SmsSaveModel>, storedPhone: String) {
        CoroutineScope(Dispatchers.IO).launch {
            for (message in smsSaveModels) {
                val messageId = databaseReference.child("UploadBackUp").child(storedPhone).push().key
                if (messageId != null) {
                    val messageModel = MessageModel(
                        messageId,
                        message.receiptName,
                        message.phoneNumber,
                        message.message,
                        message.date,
                        message.time
                    )
                    databaseReference.child("UploadBackUp").child(storedPhone).child(messageId).setValue(messageModel)
                }
            }
        }
    }

    /*  private fun uploadMessageToFirebase(message: SmsSaveModel) {
          val messageId = databaseReference.child("UploadBackUp").push().key

          if (messageId != null) {
              val messageModel = MessageModel(
                  messageId,
                  message.receiptName,
                  message.phoneNumber,
                  message.message,
                  message.date,
                  message.time
              )
              databaseReference.child("UploadBackUp").child(messageId).setValue(messageModel)
          }
      }*/


    private fun getStoredPhoneNumber(): String? {
        val sharedPreferences =
            getSharedPreferences(
                this@MyNotificationListenerService.packageName,
                Context.MODE_PRIVATE
            )
        return sharedPreferences.getString("phone", "")
    }



}
