package com.example.snitchsms.recieptdata

import android.Manifest.permission.READ_PHONE_NUMBERS
import android.Manifest.permission.READ_PHONE_STATE
import android.Manifest.permission.READ_SMS
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snitchsms.AppData
import com.example.snitchsms.R
import com.example.snitchsms.contacts.CallLogViewModel
import com.example.snitchsms.contacts.adapter.SmsAdapter
import com.example.snitchsms.contacts.callbacks.SmsDelete
import com.example.snitchsms.contacts.model.ContactModel
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.databinding.ActivityMessageListBinding
import com.example.snitchsms.firebase.MessageModel
import com.example.snitchsms.roomdatabase.SmsRepository
import com.example.snitchsms.roomdatabase.SmsViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale


class MessageListActivity : AppCompatActivity() {

    lateinit var binding: ActivityMessageListBinding

    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    var phoneNumberRecept = "recipient_phone_number"
    var message = "Your SMS message here"
    var operator1 = ""
    var operator2 = ""
    private var madapter: SmsAdapter? = null
    private val PHONE_STATE_SMS_PERMISSION_REQUEST = 101
    companion object {
        lateinit var smsListLiveData: LiveData<List<SmsSaveModel>>
        var arrayList: MutableList<ContactModel> = mutableListOf()
        var viewModelFactory: SmsViewModel.Factory? = null
        var smsViewModel: SmsViewModel? = null
        val dateList: MutableList<String> = mutableListOf()
        var datePosition: MutableList<Int> = mutableListOf()
        var isActivityInForeground = false

    }

    private val mViewModel: CallLogViewModel by viewModels()
    private lateinit var contactPhoto: Bitmap
    var contactName = ""
    var phoneNumber = ""
    var operatorValue = ""
    var forwardMessage = ""

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor("#389FD6")
        arrayList.clear()
        CoroutineScope(Dispatchers.IO).launch {
            arrayList = mViewModel.getContacts(this@MessageListActivity)
        }
        doSomethingWithSelectedValue()
        /*  phoneNumber = intent.getStringExtra("phoneNumber")!!
          contactName = intent.getStringExtra("contactName")!!
          operatorValue = intent.getStringExtra("operatorValue")!!*/
        /*  if (intent.hasExtra("phoneNumber")) {
              phoneNumber = intent.getStringExtra("phoneNumber")!!
              Log.d("TAG", "onCreate:phoneNumberphoneNumber ${phoneNumber} ")
  //            contactName = findRecipientName(phoneNumber)!!
          }
          phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
  //        contactName = intent.getStringExtra("contactName") ?: ""
          operatorValue = intent.getStringExtra("operatorValue") ?: ""
  //        contactName = findRecipientName(phoneNumber)!!
          Log.d("TAG", "onCreate: Main phoneNumber $phoneNumber")
          Log.d("TAG", "onCreate: Main operatorValue $operatorValue")
  //        contactName = findRecipientName(phoneNumber)!!
          Log.d("TAG", "onCreate: MaincontactNamecontactName ${findRecipientName(phoneNumber)}")
          Log.d("TAG", "onCreate: Main contactName $contactName")*/
        if (intent.hasExtra("phoneNumber")) {
            phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
            Log.d("TAG", "onCreate:phoneNumberphoneNumber $phoneNumber")
            contactName = findRecipientName(phoneNumber) ?: "DefaultContactName"
        }
//        operatorValue = intent.getStringExtra("operatorValue") ?: ""
        message = intent.getStringExtra("message") ?: ""
        Log.d("TAG", "onCreate: Main message $message")
        Log.d("TAG", "onCreate: Main phoneNumber $phoneNumber")
//        Log.d("TAG", "onCreate: Main operatorValue $operatorValue")
        contactName = getContactNameFromNumber(this, phoneNumber) ?: "DefaultContactName"
        Log.d("TAG", "onCreate: Main contactName $contactName")
        phoneNumberRecept = phoneNumber
        binding.name.setText(contactName)
//        myListData = ArrayList()

        if (message != "") {
            binding.etsendmsg.setText(message)
        } else if (message == "") {
            binding.etsendmsg.setText("")
        }
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference
//        getMessageDataFromFirebase()


        GetNumber()

        viewModelFactory = SmsViewModel.Factory(SmsRepository(this))
        smsViewModel = ViewModelProvider(this, viewModelFactory!!).get(SmsViewModel::class.java)

        contactPhoto = retrieveContactPhoto(this, phoneNumber)
        binding.mImg.setImageBitmap(contactPhoto)

        smsListLiveData = smsViewModel!!.allItems
        smsListLiveData.observe(this) { itemList ->
//        smsViewModel!!.allItems.observe(this) { itemList ->
            Log.d("TAG", "onCreate: isblocked ${itemList[0].isBlocked}")
            if (phoneNumber != null && contactName != null) {
                phoneNumberRecept = phoneNumber
                binding.name.setText(contactName)

                val filteredMessages = filterMessagesForPhoneNumber(contactName, itemList)
                Log.d("TAG", "Main: filteredMessages  ${filteredMessages}")

                setSmsAdapter(filteredMessages)
            }

        }
        CoroutineScope(Dispatchers.IO).launch {
            if (!smsViewModel!!.isContactBlocked(contactName)) {
//                binding.cvSend.visibility = View.VISIBLE
                binding.txtBlocStatus.visibility = View.GONE

                // Allow sending the message
                // ... your existing code to send the message
            } else {
                // Display a message to the user that the contact is blocked
//                Toast.makeText(this@MessageListActivity, "Contact is blocked", Toast.LENGTH_SHORT).show()
                Log.d("TAG", "onCreate:blocked Contact is blocked")
//                binding.cvSend.visibility = View.GONE
                binding.txtBlocStatus.visibility = View.VISIBLE
            }
        }
        getAllOperator()
        clickListener()
        Log.d("MessageListSize", "MLActivity oncreate: ${AppData.myListData.size}")
//        removeAllDataFromFirebase()

    }

    private fun getSelectedValueFromPreferences(): String {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("selected_value", "") ?: ""
    }
    private fun removeAllDataFromFirebase() {
        val databaseReference = FirebaseDatabase.getInstance().reference
        val uploadBackupReference = databaseReference.child("UploadBackUp")

        Log.d("TAG", "removeAllDataFromFirebase: Before removing data")

        // Remove all data in a single call
        uploadBackupReference.removeValue()
            .addOnSuccessListener {
                Log.d("TAG", "removeAllDataFromFirebase: Data removed successfully")
            }
            .addOnFailureListener { error ->
                Log.e("TAG", "removeAllDataFromFirebase: Failed to remove data", error)
            }
    }




    // Use this method wherever you need the selected value
    private fun doSomethingWithSelectedValue() {
        val selectedValue = getSelectedValueFromPreferences()

        when (selectedValue) {
            "op1" -> {
                operatorValue = "op1"
            }

            "op2" -> {
                operatorValue = "op2"
            }

            "wifi" -> {
                operatorValue = "wifi"
            }

            else -> {
                operatorValue = "wifi"
            }

        }
        Log.d("TAG", "onCreate: Main prefrences operatorValue $operatorValue")

    }

    private fun setStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)
        }
    }

    private fun filterMessagesForPhoneNumber(
        phoneNumber: String,
        allMessages: List<SmsSaveModel>
    ): ArrayList<SmsSaveModel> {
        val filteredMessages = ArrayList<SmsSaveModel>()
        for (message in allMessages) {
            if (message.receiptName == phoneNumber) {
                filteredMessages.add(message)
            }
        }
        return filteredMessages
    }


    private fun setSmsAdapter(mySmsList: ArrayList<SmsSaveModel>) {
        runOnUiThread {

            madapter = SmsAdapter(mySmsList, this, object : SmsDelete {
                override fun itemDelete(pos: Int) {
                    val deletedItem = mySmsList[pos]
                    madapter?.item?.removeAt(pos)
                    madapter?.notifyItemRemoved(pos)
                    smsViewModel!!.deleteItem(deletedItem)
                }

                override fun itemLongClick(pos: Int) {
                    /*val dialog = SmsDetailDialog(this@MainActivity)
                    dialog.show()*/
                }


            })

            binding.recyclerSaveSms.apply {
                layoutManager = GridLayoutManager(this@MessageListActivity, 1)
                adapter = madapter
                scrollToPosition(mySmsList.size - 1)
            }
        }
    }

    @SuppressLint("Range")
    fun getContactNameFromNumber(context: Context, phoneNumber: String): String? {
        val contentResolver: ContentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val contactName =
                    it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                Log.d("ContactLookup", "Contact Name for $phoneNumber: $contactName")
                return contactName
            }
        }

        Log.d("ContactLookup", "No contact name found for $phoneNumber")
        return null
    }

    private fun clickListener() {

        binding.backImg.setOnClickListener {
            onBackPressed()
        }
        binding.menuImg.setOnClickListener {
            showCustomPopupMenu(binding.menuImg)
        }
        binding.txtBlocStatus.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            val customLayout = layoutInflater.inflate(R.layout.custom_alert_dialog_un_block, null)
            builder.setView(customLayout)

            val dialogTitle = customLayout.findViewById<TextView>(R.id.dialogTitle)
            val dialogMessage = customLayout.findViewById<TextView>(R.id.dialogMessage)
            /*   val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
               val isBlocked = sharedPreferences.getBoolean(contactName, false)
               if (isBlocked) {
                   dialogTitle.text = "Unblock $contactName?"
                   binding.txtBlocStatus.visibility = View.VISIBLE
               } else {
                   dialogTitle.text = "Block $contactName?"
                   binding.txtBlocStatus.visibility = View.GONE
               }*/
//            dialogTitle.text = "Block $contactName?"
            dialogTitle.text =
                "Unblock this contact to send message"
            val btnPositive = customLayout.findViewById<Button>(R.id.btnPositive)
            val btnNegative = customLayout.findViewById<Button>(R.id.btnNegative)

            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnPositive.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val isBlocked = smsViewModel!!.isContactBlocked(contactName)
                    Log.d("TAG", "showCustomPopupMenu: isBlocked $isBlocked")
                    withContext(Dispatchers.Main) {
                        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        smsViewModel!!.unblockContact(contactName)
                        editor.putBoolean(contactName, false)
                        binding.txtBlocStatus.visibility = View.GONE
                        editor.apply()
                    }
                }
                dialog.dismiss()
            }

            btnNegative.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
        binding.imgSend.setOnClickListener {
            /*  binding.cvOperator1.visibility = View.VISIBLE
              binding.cvOperator2.visibility = View.VISIBLE*/
            if (operatorValue == "op2") {
                message = binding.etsendmsg.text.toString()
                sendSMS(operator2, phoneNumberRecept, message)

                val currentTimeMillis = System.currentTimeMillis()
                val currentDate =
                    SimpleDateFormat(
                        "yyyy-MM-dd hh:mm a",
                        Locale.getDefault()
                    ).format(currentTimeMillis)
                val currentTime =
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentTimeMillis)

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedTime = timeFormat.format(currentTimeMillis)

                val recipientName = findRecipientName(phoneNumberRecept)


                AppData.myListData?.add(
                    SmsSaveModel(
                        0,
                        recipientName!!,
                        phoneNumberRecept,
                        message,
                        currentDate,
                        formattedTime, "Left"
                    )
                )
                CoroutineScope(Dispatchers.IO).launch() {

                    smsViewModel!!.insertItem(
                        SmsSaveModel(
                            0,
                            recipientName!!,
                            phoneNumberRecept,
                            message,
                            currentDate,
                            formattedTime, "Left"
                        )
                    )
//                setSmsAdapter(myListData!!)
                }
                Log.d("MessageListSize", "MLActivity: ${AppData.myListData.size}")
            } else if (operatorValue == "op1")
            {
                message = binding.etsendmsg.text.toString()
                sendSMS(operator1, phoneNumberRecept, message)

                val currentTimeMillis = System.currentTimeMillis()
                val currentDate =
                    SimpleDateFormat(
                        "yyyy-MM-dd hh:mm a",
                        Locale.getDefault()
                    ).format(currentTimeMillis)
                val currentTime =
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentTimeMillis)

                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedTime = timeFormat.format(currentTimeMillis)

                val recipientName = findRecipientName(phoneNumberRecept)


                AppData.myListData?.add(
                    SmsSaveModel(
                        0,
                        recipientName!!,
                        phoneNumberRecept,
                        message,
                        currentDate,
                        formattedTime, "Left"
                    )
                )
                CoroutineScope(Dispatchers.IO).launch() {

                    smsViewModel!!.insertItem(
                        SmsSaveModel(
                            0,
                            recipientName!!,
                            phoneNumberRecept,
                            message,
                            currentDate,
                            formattedTime, "Left"
                        )
                    )
                }
            } else {

                Toast.makeText(this, "send message using wifi", Toast.LENGTH_SHORT).show()
                message = binding.etsendmsg.text.toString()
//                sendSMS(operator1, phoneNumberRecept, message)

                val currentTimeMillis = System.currentTimeMillis()
                val currentDate =
                    SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(
                        currentTimeMillis
                    )
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedTime = timeFormat.format(currentTimeMillis)

                val recipientName = findRecipientName(phoneNumberRecept)
                Log.d("messagee", "phoneNumberRecept: $phoneNumberRecept")
                Log.d("messagee", "recipientName: $recipientName")

                if (recipientName != null) {
                    addMessageToDatabase(
                        recipientName!!, phoneNumberRecept,
                        message, currentDate, formattedTime
                    )
                }
                AppData.myListData?.add(
                    SmsSaveModel(
                        0,
                        recipientName!!,
                        phoneNumberRecept,
                        message,
                        currentDate,
                        formattedTime, "Left"
                    )
                )
                CoroutineScope(Dispatchers.IO).launch() {

                    smsViewModel!!.insertItem(
                        SmsSaveModel(
                            0,
                            recipientName!!,
                            phoneNumberRecept,
                            message,
                            currentDate,
                            formattedTime, "Left"
                        )
                    )
                }

            }
        }

        /*binding.cvOperator1.setOnClickListener {
            message = binding.etsendmsg.text.toString()
            Log.d(
                "TAG",
                "clickListener: operator1 $operator1 phoneNumberRecept $phoneNumberRecept message $message"
            )
            sendSMS(operator1, phoneNumberRecept, message)
            myListData?.add(
                SmsSaveModel(
                    0,
                    phoneNumberRecept,
                    message,
                    "6112023",
                    "1200", "Right"
                )
            )
            CoroutineScope(Dispatchers.Main).launch() {
                smsViewModel!!.insertItem(
                    SmsSaveModel(
                        0,
                        phoneNumberRecept,
                        message,
                        "6112023",
                        "1200", "Right"
                    )
                )
                setSmsAdapter(myListData!!)
            }
        }*/

    }

    private fun addMessageToDatabase(
        recieptName: String, recieptPhoneNumber: String,
        message: String, date: String, time: String
    ) {
        val messageId = databaseReference.child("messages").push().key

        if (messageId != null) {
            val messageModel = MessageModel(
                messageId, recieptName, recieptPhoneNumber,
                message, date, time
            )
            databaseReference.child("messages").child(messageId).setValue(messageModel)
            binding.etsendmsg.setText("")
            Toast.makeText(this, "Message sent successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMessageDataFromFirebase() {
        Log.d("TAG", "getMessageDataFromFirebase: in ")
        databaseReference.child("messages").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("TAG", "getMessageDataFromFirebase:snapshot in ")
                val messages = mutableListOf<MessageModel>()
                Log.d("TAG", "getMessageDataFromFirebase: Before loop")
                for (messageSnapshot in snapshot.children) {
                    Log.d("TAG", "getMessageDataFromFirebase: Inside loop")
                    val message = messageSnapshot.getValue(MessageModel::class.java)
                    Log.d("TAG", "getMessageDataFromFirebase: ${message}")
                    message?.let {
                        messages.add(it)
                        Log.d("TAG", "getMessageDataFromFirebase: Added message to list")
                    }
                }

                val sharedPreferences =
                    getSharedPreferences(this@MessageListActivity.packageName, Context.MODE_PRIVATE)
                val storedPhone = sharedPreferences.getString("phone", "")
                Log.d("TAG", "onDataChange: storedPhone $storedPhone")
                val filteredMessages = messages.filter {
                    it.receiptPhoneNumber.contains(storedPhone.orEmpty())
                }
                Log.d("TAG", "getMessageDataFromFirebase: After loop")
                addFilteredMessagesToLocalDatabase(filteredMessages)

            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("getMessageDataFromFirebase", "onCancelled: ${error.details}")
            }
        })
    }
    private fun addFilteredMessagesToLocalDatabase(filteredMessages: List<MessageModel>) {
        for (message in filteredMessages) {
            CoroutineScope(Dispatchers.IO).launch {
                val smsSaveModel = SmsSaveModel(
                    0,
                    message.receiptName,
                    message.receiptPhoneNumber,
                    message.message,
                    message.date,
                    message.time,
                    "Right"
                )
                smsViewModel?.insertItem(smsSaveModel)
            }
        }
    }
    private fun getAllOperator() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED

        ) {
            // Request permissions for READ_PHONE_STATE and SEND_SMS
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.RECEIVE_SMS
                ),
                PHONE_STATE_SMS_PERMISSION_REQUEST
            )
        } else {


            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            operator1 = telephonyManager.simOperatorName
            val subscriptionManager =
                getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            if (subscriptionManager.activeSubscriptionInfoCountMax > 1) {
                val infoList = subscriptionManager.activeSubscriptionInfoList
                operator2 =
                    telephonyManager.createForSubscriptionId(infoList[1].subscriptionId).simOperatorName
                binding.tvOp1.text = operator1
                binding.tvOp2.text = operator2
                Log.d("TAG", "Operator 1: $operator1")
                Log.d("TAG", "Operator 2: $operator2")
            } else {
                Log.d("TAG", "Operator 1: $operator1")
            }
        }
    }

    private fun findRecipientName(phoneNumber: String): String? {
        val recipient = arrayList.find { it.number == phoneNumber }
        return recipient?.contactName
    }

    fun sendSMS(operator: String, phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        val subscriptionManager =
            getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
        for (info in subscriptionInfoList) {
            if (info.displayName == operator) {
                val subscriptionId = info.subscriptionId
                val smsManagerForSubscription =
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                smsManagerForSubscription.sendTextMessage(phoneNumber, null, message, null, null)
                binding.etsendmsg.setText("")
                Toast.makeText(this, "Message sent successfully", Toast.LENGTH_SHORT).show()
                break
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PHONE_STATE_SMS_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[2] == PackageManager.PERMISSION_GRANTED
                ) {
                    getAllOperator()
                } else {
                    getAllOperator()
                }
            }

            100 -> {
                val telephonyManager = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                if (ActivityCompat.checkSelfPermission(this, READ_SMS) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        READ_PHONE_NUMBERS
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        READ_PHONE_STATE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                val phoneNumber = telephonyManager.line1Number
                Log.d("TAG", "onRequestPermissionsResult: phoneNumber $phoneNumber")
            }
        }
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
        }

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

                return getDefaultContactPhoto(context)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return photo ?: getDefaultContactPhoto(context)
    }

    private fun getDefaultContactPhoto(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.icon_profile)
    }


    private fun showCustomPopupMenu(anchorView: View) {
        val inflater =
            anchorView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_main_menu_three_dot, null)

        val txtblockContact = popupView.findViewById<TextView>(R.id.txtblockContact)
        val txtdetail = popupView.findViewById<TextView>(R.id.txtdetail)
        val txtAllDelete = popupView.findViewById<TextView>(R.id.txtDeleteM)
        val mUserImg = popupView.findViewById<CircleImageView>(R.id.mUserImg)
        val userName = popupView.findViewById<TextView>(R.id.userName)


        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )


        val xOff = anchorView.width
        val yOff = -anchorView.height + 20

        popupWindow.showAsDropDown(anchorView, xOff, yOff)

        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val isBlocked = sharedPreferences.getBoolean(contactName, false)
        if (isBlocked) {
            txtblockContact.text = "Unblock contact"
            binding.txtBlocStatus.visibility = View.VISIBLE
        } else {
            txtblockContact.text = "Block contact"
            binding.txtBlocStatus.visibility = View.GONE
        }

        mUserImg.setImageBitmap(contactPhoto)
        userName.text = contactName

        txtblockContact.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val customLayout = layoutInflater.inflate(R.layout.custom_alert_dialog, null)
            builder.setView(customLayout)

            val dialogTitle = customLayout.findViewById<TextView>(R.id.dialogTitle)
            val dialogMessage = customLayout.findViewById<TextView>(R.id.dialogMessage)
            val isBlocked = sharedPreferences.getBoolean(contactName, false)
            if (isBlocked) {
                dialogTitle.text = "Unblock $contactName?"
                binding.txtBlocStatus.visibility = View.VISIBLE
            } else {
                dialogTitle.text = "Block $contactName?"
                binding.txtBlocStatus.visibility = View.GONE
            }
//            dialogTitle.text = "Block $contactName?"
            dialogMessage.text =
                "Blocked contact cannot call or send you messages. This contact will not be notified."
            val btnPositive = customLayout.findViewById<Button>(R.id.btnPositive)
            val btnNegative = customLayout.findViewById<Button>(R.id.btnNegative)

            val dialog = builder.create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnPositive.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val isBlocked = smsViewModel!!.isContactBlocked(contactName)
                    Log.d("TAG", "showCustomPopupMenu: isBlocked $isBlocked")
                    withContext(Dispatchers.Main) {
                        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()

                        if (isBlocked) {
                            Log.d("TAG", "showCustomPopupMenu: Unblock $isBlocked")
                            txtblockContact.text = "Unblock"
                            // binding.cvSend.visibility = View.VISIBLE
                            binding.txtBlocStatus.visibility = View.GONE
                            smsViewModel!!.unblockContact(contactName)
                            editor.putBoolean(contactName, false)
                        } else {
                            Log.d("TAG", "showCustomPopupMenu: Block $isBlocked")
                            txtblockContact.text = "Block"
                            // binding.cvSend.visibility = View.GONE
                            binding.txtBlocStatus.visibility = View.VISIBLE
                            smsViewModel!!.blockContact(contactName)
                            editor.putBoolean(contactName, true)
                        }

                        editor.apply()
                    }
                }
                dialog.dismiss()
                popupWindow.dismiss()
                dialog.dismiss()
                popupWindow.dismiss()
            }

            btnNegative.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        /* txtAllDelete.setOnClickListener {
             CoroutineScope(Dispatchers.IO).launch() {
                 smsViewModel!!.deleteAllMessagesOfChat(contactName)
                *//* message = binding.etsendmsg.text.toString()
                val currentTimeMillis = System.currentTimeMillis()
                val currentDate =
                    SimpleDateFormat(
                        "yyyy-MM-dd hh:mm a",
                        Locale.getDefault()
                    ).format(currentTimeMillis)
                val currentTime =
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(currentTimeMillis)
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedTime = timeFormat.format(currentTimeMillis)
                val recipientName = findRecipientName(phoneNumberRecept)
                Log.d("messagee", "phoneNumberRecept: $phoneNumberRecept")
                Log.d("messagee", "recipientName: $recipientName")

                AppData.myListData?.add(
                    SmsSaveModel(
                        0,
                        recipientName!!,
                        phoneNumberRecept,
                        message,
                        currentDate,
                        formattedTime, "Left"
                    )
                )
                CoroutineScope(Dispatchers.IO).launch() {

                    smsViewModel!!.insertItem(
                        SmsSaveModel(
                            0,
                            recipientName!!,
                            phoneNumberRecept,
                            message,
                            currentDate,
                            formattedTime, "Left"
                        )
                    )
                }*//*
            }
            popupWindow.dismiss()
        }*/
        txtAllDelete.setOnClickListener {
            showConfirmationDialog()
            popupWindow.dismiss()
        }


        /*  txtblockContact.setOnClickListener {
              CoroutineScope(Dispatchers.IO).launch {
                  val isBlocked = smsViewModel!!.isContactBlocked(contactName)
                  Log.d("TAG", "showCustomPopupMenu: isBlocked $isBlocked")
                  withContext(Dispatchers.Main) {
                      val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
                      val editor = sharedPreferences.edit()

                      if (isBlocked) {
                          Log.d("TAG", "showCustomPopupMenu: Unblock $isBlocked")
                          txtblockContact.text = "Unblock"
  //                        binding.cvSend.visibility = View.VISIBLE
                          binding.txtBlocStatus.visibility = View.GONE
                          smsViewModel!!.unblockContact(contactName)
                          editor.putBoolean(contactName, false)
                      } else {
                          Log.d("TAG", "showCustomPopupMenu: Block $isBlocked")
                          txtblockContact.text = "Block"
  //                        binding.cvSend.visibility = View.GONE
                          binding.txtBlocStatus.visibility = View.VISIBLE
                          smsViewModel!!.blockContact(contactName)
                          editor.putBoolean(contactName, true)
                      }

                      editor.apply()
                  }
              }
              popupWindow.dismiss()
          }*/

        /*  txtdetail.setOnClickListener {
              popupWindow.dismiss()
              showCustomPopupDetail(anchorView)
          }*/
    }

    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            smsViewModel!!.allItems.observe(this@MessageListActivity) { itemList ->
                if (phoneNumberRecept != null && contactName != null) {
                    val filteredMessages = filterMessagesForPhoneNumber(contactName, itemList)
                    Log.d("TAG", "Main: filteredMessages  ${filteredMessages}")

                    setSmsAdapter(filteredMessages)
                }
            }
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Messages")
        builder.setMessage("Are you sure you want to delete all messages?")
        builder.setPositiveButton("Delete") { dialog, which ->
            // User clicked on the "Delete" button
            CoroutineScope(Dispatchers.IO).launch {
                smsViewModel!!.deleteAllMessagesOfChat(contactName)
            }
            dialog.dismiss()
            finish()
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            // User clicked on the "Cancel" button
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        isActivityInForeground = true
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(messageReceiver, IntentFilter("new_message"))

        smsListLiveData.observe(this) { itemList ->

            if (phoneNumberRecept != null && contactName != null) {
                val filteredMessages = filterMessagesForPhoneNumber(contactName, itemList)
                Log.d("TAG", "Main: filteredMessages  ${filteredMessages}")
                setSmsAdapter(filteredMessages)
            }
        }

    }

    override fun onPause() {
        isActivityInForeground = false
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver)
        smsListLiveData.removeObservers(this)

        super.onPause()
    }

    fun GetNumber() {
        if (ActivityCompat.checkSelfPermission(
                this,
                READ_SMS
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                READ_PHONE_NUMBERS
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission check

            // Create obj of TelephonyManager and ask for current telephone service
            val telephonyManager = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            val phoneNumber = telephonyManager.line1Number
            Log.d("TAG", "GetNumber: phoneNumber $phoneNumber ")
            return
        } else {
            // Ask for permission
            requestPermission()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf<String>(READ_SMS, READ_PHONE_NUMBERS, READ_PHONE_STATE), 100)
        }
    }


}