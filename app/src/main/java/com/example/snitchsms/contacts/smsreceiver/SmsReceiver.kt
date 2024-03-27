package com.example.snitchsms.contacts.smsreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.snitchsms.AppData
import com.example.snitchsms.R
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.contacts.smservice.SmsService
import com.example.snitchsms.recieptdata.MessageListActivity
import com.example.snitchsms.recieptdata.MessageListActivity.Companion.smsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {
    /*    companion object{
            val lastReceivedMessages = mutableMapOf<String, String?>()
            fun getLastReceivedMessage(contactName: String): String? {
                return lastReceivedMessages[contactName]
            }
        }*/
    companion object {

        val lastReceivedMessages = mutableMapOf<String, String?>()

         const val LAST_RECEIVED_MESSAGE_PREF = "last_received_message_pref"

        fun saveLastReceivedMessage(context: Context, contactName: String, message: String?) {
            val sharedPreferences =
                context.getSharedPreferences(LAST_RECEIVED_MESSAGE_PREF, Context.MODE_PRIVATE)
            sharedPreferences.edit().putString(contactName, message).apply()
        }

        fun getLastReceivedMessage(context: Context, contactName: String): String? {
            val sharedPreferences =
                context.getSharedPreferences(LAST_RECEIVED_MESSAGE_PREF, Context.MODE_PRIVATE)
            return sharedPreferences.getString(contactName, null)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {


            val bundle = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as Array<Any>
                val messages = arrayOfNulls<SmsMessage>(pdus.size)
                for (i in messages.indices) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val format = bundle.getString("format")
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray, format)
                    } else {
                        messages[i] = SmsMessage.createFromPdu(pdus[i] as ByteArray)
                    }


                    val sender = messages[i]?.originatingAddress
                    val message = messages[i]?.messageBody
                    val timestamp = messages[i]?.timestampMillis
                    Toast.makeText(context, "message recieved from $sender", Toast.LENGTH_SHORT)
                        .show()

                    val recipientName = findRecipientName(sender!!)

                    if (!MessageListActivity.isActivityInForeground) {
                        Log.d("updateUnreadMessageCounter", "onReceive: andr aya")
                        updateUnreadMessageCounter(context, recipientName ?: sender)
                    }
                    Log.d("updateUnreadMessageCounter", "onReceive: andr ni aya")


                    CoroutineScope(Dispatchers.IO).launch {
                        lastReceivedMessages[recipientName ?: sender!!] = message
                        saveLastReceivedMessage(context!!, recipientName ?: sender!!, message)

                        if (sender != "Secure"){
                            if (!smsViewModel!!.isContactBlocked(recipientName!!)) {
                                Log.d("TAG", "onReceive: andr aya")
                                val serviceIntent = Intent(context, SmsService::class.java)
                                serviceIntent.putExtra("sender", sender)
                                serviceIntent.putExtra("message", message)
                                serviceIntent.putExtra("name", recipientName)

                                // Start the service
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context?.startForegroundService(serviceIntent)
                                } else {
                                    context?.startService(serviceIntent)
                                }

                                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    .format(timestamp)

                                Log.d("SmsReceiver", "Sender: $sender, Message: $message, Date: $date")

                                // Check if a database entry already exists for the given phone number
                                val existingRecipient =
                                    MessageListActivity.arrayList.find { it.number == sender }

                                if (existingRecipient != null) {

                                    Log.d("SmsReceiver", "SexistingRecipient!= null ")

                                    // Database entry already exists, update the existing entry with the new message
                                    val currentTimeMillis = System.currentTimeMillis()
                                    val currentDate =
                                        SimpleDateFormat(
                                            "yyyy-MM-dd hh:mm a",
                                            Locale.getDefault()
                                        ).format(currentTimeMillis)

                                    val timeFormat =
                                        SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    val formattedTime = timeFormat.format(currentTimeMillis)

                                    existingRecipient.contactName?.let { recipientName ->
                                        AppData.myListData?.add(
                                            SmsSaveModel(
                                                0,
                                                recipientName,
                                                sender!!,
                                                message ?: "",
                                                currentDate,
                                                formattedTime, "Right"
                                            )
                                        )
                                        /* CoroutineScope(Dispatchers.IO).launch() {
                                    AllReceiptActivity.smsViewModel.insertItem(
                                        SmsSaveModel(
                                            0,
                                            recipientName,
                                            sender!!,
                                            message ?: "",
                                            currentDate,
                                            formattedTime, "Right"
                                        )
                                    )
                                }*/
                                    }

                                    val broadcastIntent = Intent("new_message")
                                    LocalBroadcastManager.getInstance(context!!)
                                        .sendBroadcast(broadcastIntent)

                                    Log.d("MessageListSize", "Reciever : ${AppData.myListData.size}")

                                } else {
                                    Log.d("SmsReceiver", " else {")

                                    // Database entry doesn't exist, create a new entry
                                    val currentTimeMillis = System.currentTimeMillis()
                                    val currentDate =
                                        SimpleDateFormat(
                                            "yyyy-MM-dd hh:mm a",
                                            Locale.getDefault()
                                        ).format(currentTimeMillis)

                                    val timeFormat =
                                        SimpleDateFormat("hh:mm a", Locale.getDefault())
                                    val formattedTime = timeFormat.format(currentTimeMillis)

                                    val recipientName = findRecipientName(sender!!)

                                    AppData.myListData?.add(
                                        SmsSaveModel(
                                            0,
                                            recipientName ?: sender,
                                            sender,
                                            message ?: "",
                                            currentDate,
                                            formattedTime, "Right"
                                        )
                                    )
                                    CoroutineScope(Dispatchers.IO).launch() {
                                        smsViewModel!!.insertItem(
                                            SmsSaveModel(
                                                0,
                                                recipientName ?: sender,
                                                sender,
                                                message ?: "",
                                                currentDate,
                                                formattedTime, "Right"
                                            )
                                        )


                                    }
                                }
                            }
                        }else{
                            if (sender == "Secure")
                            {
                                val otp = extractSixDigitOtp(message)
                                // Broadcast the updated counter value
                                val broadcastIntent = Intent("otp")
                                broadcastIntent.putExtra("otp", otp)
                                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent)
                            }
                        }

                    }
                }
            }
        }

    }

    private fun extractSixDigitOtp(message: String?): String {
        // Use regex to extract a 6-digit number from the message
        val regex = Regex("\\b\\d{6}\\b")
        val matchResult = regex.find(message ?: "")
        return matchResult?.value ?: ""
    }



    private fun findRecipientName(phoneNumber: String): String? {
        val recipient = MessageListActivity.arrayList.find { it.number == phoneNumber }
        return recipient?.contactName
    }

    private fun updateUnreadMessageCounter(context: Context?, name: String) {
        val sharedPreferences =
            context?.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
        var unreadCounter = sharedPreferences?.getInt(name, 0) ?: 0
        Log.d("unreadCounter", "put:get unreadCounter $unreadCounter")
        val updatedCounter = ++unreadCounter

        // Save the updated counter value in SharedPreferences
        sharedPreferences?.edit()?.putInt(name, updatedCounter)?.apply()
        Log.d("unreadCounter", "put: unreadCounter $unreadCounter")

        // Broadcast the updated counter value
        val broadcastIntent = Intent("unread_message_count")
        broadcastIntent.putExtra("name", name)
        broadcastIntent.putExtra("unreadCount", updatedCounter)
        LocalBroadcastManager.getInstance(context!!).sendBroadcast(broadcastIntent)
    }

}

