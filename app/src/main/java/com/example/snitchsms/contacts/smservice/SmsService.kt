package com.example.snitchsms.contacts.smservice


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.snitchsms.R
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.roomdatabase.SmsDao
import com.example.snitchsms.roomdatabase.SmsRepository
import com.example.snitchsms.roomdatabase.SmsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale


class SmsService : Service() {

    companion object {
        val CHANNEL_ID = "SmsServiceChannel"
    }
    private lateinit var smsRepository: SmsRepository

//     lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModelFactory: SmsViewModel.Factory
    private lateinit var smsViewModel: SmsViewModel


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        handleReceivedSms(intent)
        return START_STICKY
    }


    override fun onCreate() {
        super.onCreate()
        Log.d("smsRepository", "onCreate")
        smsRepository = SmsRepository.getInstance(applicationContext)
    }

    private fun handleReceivedSms(intent: Intent?) {
        val sender = intent?.getStringExtra("sender")
        val message = intent?.getStringExtra("message")
        val name = intent?.getStringExtra("name")

        if (sender != null && message != null && name != null) {
            val smsViewModel = SmsRepository.getInstance(this)
            val currentTimeMillis = System.currentTimeMillis()
            val currentDate = SimpleDateFormat(
                "yyyy-MM-dd hh:mm a",
                Locale.getDefault()
            ).format(currentTimeMillis)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(currentTimeMillis)
            CoroutineScope(Dispatchers.IO).launch() {
                smsViewModel.insert(
                    SmsSaveModel(
                        0,
                        name,
                        sender,
                        message,
                        currentDate,
                        formattedTime,
                        "Right"
                    )
                )
            }
        }

        startForegroundService()
    }

    /*    private fun startForegroundService() {
            val notificationIntent = Intent(this, MessageListActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE)

            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SmsService")
                .setContentText("Service is running in the background.")
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentIntent(pendingIntent)
                .build()

            startForeground(1, notification)
        }*/
    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = SmsService.CHANNEL_ID
            val channel = NotificationChannel(
                channelId,
                "SmsService Channel",
                NotificationManager.IMPORTANCE_LOW
            )

            // Create a custom layout for the notification
            val customLayout = RemoteViews(packageName, R.layout.custom_notif)

            // Set the visibility to secret to make it less noticeable
            customLayout.setInt(R.id.custom_layout, "setVisibility", View.GONE)

            val notification = NotificationCompat.Builder(this, channelId)
                .setCustomContentView(customLayout)
                .setSmallIcon(R.drawable.dot_no_img)
                .setPriority(NotificationCompat.PRIORITY_LOW) // Set low priority
                .build()

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            startForeground(1, notification)
        }
    }


}
