package com.example.snitchsms.dialog

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.snitchsms.R
import com.example.snitchsms.contacts.adapter.ForwardReceiptAdapter
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.recieptdata.ClickCallback
import com.example.snitchsms.recieptdata.MessageListActivity
import com.example.snitchsms.roomdatabase.SmsRepository
import com.example.snitchsms.roomdatabase.SmsViewModel
import java.io.IOException

//class SmsForwardDialog(context: Context) : Dialog(context, R.style.FullScreenDialogStyle) {
class SmsForwardDialog(
    context: Context, private val owner: ViewModelStoreOwner,
    override val lifecycle: Lifecycle,
    val message: String?
) : Dialog(context, R.style.FullScreenDialogStyle), LifecycleOwner {


    private var madapter: ForwardReceiptAdapter? = null
    private var recyclerView: RecyclerView? = null
    val unreadCounts: MutableMap<String, Int> = mutableMapOf()
    var viewModelFactory: SmsViewModel.Factory? = null
    lateinit var smsViewModel: SmsViewModel
    private val unreadMessagesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "unread_message_count") {
                val name = intent.getStringExtra("name")
                val newUnreadCount = intent.getIntExtra("unreadCount", 0)
                unreadCounts[name!!] = newUnreadCount

                madapter?.notifyDataSetChanged()

                Log.d(
                    "TAG",
                    "unreadMessagesReceiver: name ${name}  + unreadCount ${newUnreadCount}"
                )

            }
        }
    }
    private var imgClose: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val params = window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        window?.attributes = params
        setContentView(R.layout.layout_dialog_detail_sms)
        recyclerView = findViewById(R.id.recyclerView)
        imgClose = findViewById(R.id.imgClose)


        viewModelFactory = SmsViewModel.Factory(SmsRepository(context))
//        smsViewModel = ViewModelProvider(context, viewModelFactory!!).get(SmsViewModel::class.java)
        smsViewModel = ViewModelProvider(owner, viewModelFactory!!).get(SmsViewModel::class.java)

        smsViewModel.allItems.observe(this@SmsForwardDialog) { itemList ->
            Log.d("TAG", "onCreate: setMicSmsAdapter observer ${itemList}")

            val uniquePhoneNumbers = itemList.distinctBy { it.receiptName }
            setUpAdapter(uniquePhoneNumbers)
        }

        ClickListenr()
    }

    fun dismissIt() {
        try {
            dismiss()
        } catch (e: Exception) {
            // Handle exceptions if needed
        }
    }

    private fun ClickListenr() {
        imgClose?.setOnClickListener {
            dismissIt()
        }
    }

    private fun setUpAdapter(mySmsList: List<SmsSaveModel>) {
        val contactPhotoList = mutableMapOf<String, Bitmap>()

        for (smsItem in mySmsList) {
            val phoneNumber = smsItem.phoneNumber
            val contactPhoto = retrieveContactPhoto(context, phoneNumber)
            contactPhotoList[phoneNumber] = contactPhoto

        }


        madapter = ForwardReceiptAdapter(mySmsList, object : ClickCallback {
            override fun itemClick(position: Int) {
                val phoneNumber = mySmsList[position].phoneNumber
                val contactName = mySmsList[position].receiptName
                Log.d("ForwardReceiptAdapter", "phoneNumber: ${phoneNumber} ")
                Log.d("ForwardReceiptAdapter", "contactName: ${contactName} ")
                Log.d("ForwardReceiptAdapter", "Message: ${message} ")
                val intent = Intent(context, MessageListActivity::class.java)
                intent.putExtra("phoneNumber", phoneNumber)
                intent.putExtra("message", message)
                context.startActivity(intent)
                dismissIt()

            }

            override fun resetUnreadCounter(contactName: String) {
            }

            override fun updateDeleteButtonVisibility() {
            }
        }, contactPhotoList, unreadCounts)


        recyclerView?.layoutManager = GridLayoutManager(context, 1)
        recyclerView?.adapter = madapter

    }

    fun retrieveContactPhoto(context: Context, phoneNumber: String?): Bitmap {
        val contentResolver = context.contentResolver
        var contactId: String? = null

        // Check if phoneNumber is null and return a default image or null
        if (phoneNumber == null) {
            return getDefaultContactPhoto(context)
        }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection =
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID)

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    contactId =
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                }
                cursor.close()
            }
        } catch (e: SecurityException) {
            // Handle the security exception if needed
        }

        // Check if contactId is null and return a default image or null
        if (contactId == null) {
            return getDefaultContactPhoto(context)
        }

        var photo: Bitmap? = null
        try {
            val inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI,
                    contactId.toLong()
                )
            )

            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
            } else {
                // Handle the case where the contact has no photo
                // You can return a default image or null here
                return getDefaultContactPhoto(context)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return photo ?: getDefaultContactPhoto(context)
    }

    // Helper function to return a default contact photo
    private fun getDefaultContactPhoto(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.icon_profile)
    }

}
