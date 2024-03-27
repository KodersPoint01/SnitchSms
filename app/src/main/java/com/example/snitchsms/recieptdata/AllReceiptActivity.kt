package com.example.snitchsms.recieptdata

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snitchsms.AppData
import com.example.snitchsms.R
import com.example.snitchsms.contacts.ContactsActivity
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.databinding.ActivityAllReceiptBinding
import com.example.snitchsms.firebase.MessageModel
import com.example.snitchsms.incomingnotification.MyNotificationListenerService
import com.example.snitchsms.roomdatabase.SmsRepository
import com.example.snitchsms.roomdatabase.SmsViewModel
import com.example.snitchsms.utils.Utilities
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class AllReceiptActivity : AppCompatActivity() {
    lateinit var binding: ActivityAllReceiptBinding
    private val PHONE_STATE_SMS_PERMISSION_REQUEST = 101

    private var madapter: ReceiptAdapter? = null
    var operatorValue = ""

    private var phoneNumber = ""
    private var contactName = ""
    var operator1 = ""
    var operator2 = ""
    var isOp1 = false
    var isOp2 = false
    var isWifi = false
    var tvOp1: TextView? = null
    var tvOp2: TextView? = null
    private var isDeleteModeActive = false
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    private val WIFI_SWITCH_STATE = "wifi_switch_state"
    private val selectedItems = mutableListOf<SmsSaveModel>()

    companion object {

        //        var myListData: ArrayList<ReceiptItem>? = null
        var viewModelFactory: SmsViewModel.Factory? = null
        lateinit var smsViewModel: SmsViewModel
    }

    var contactPhoto: Bitmap? = null
    val unreadCounts: MutableMap<String, Int> = mutableMapOf()

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }
    private var isDataRetrieved: Boolean
        get() = sharedPreferences.getBoolean("isDataRetrieved", false)
        set(value) = sharedPreferences.edit().putBoolean("isDataRetrieved", value).apply()


    private val unreadMessagesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "unread_message_count") {
                val name = intent.getStringExtra("name")
                val newUnreadCount = intent.getIntExtra("unreadCount", 0)
                unreadCounts[name!!] = newUnreadCount

                madapter?.updateUnreadCount(contactName, newUnreadCount)

                Log.d(
                    "TAG",
                    "unreadMessagesReceiver: name ${name}  + unreadCount ${newUnreadCount}"
                )
                Log.d(
                    "TAG",
                    "unreadMessagesReceiver: unreadCounts name ${unreadCounts}  "
                )

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference

        requestPermission()
        getAllOperator()
        if (!isNotificationListenerEnabled()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
        if (!isDataRetrieved) {

            retrieveMessagesForPhoneNumber()
        }
        val sharedPreferences = this.getSharedPreferences(this.packageName, Context.MODE_PRIVATE)
        val phoneNumber1 = sharedPreferences.getString("phone", null)

        if (phoneNumber1 != null) {
            Log.d("TAG", "onCreate: phoneNumbersharedPreferences $phoneNumber1")
        } else {
            Log.d("TAG", "onCreate: phoneNumber empty")

        }
        val unreadCounter = sharedPreferences?.getInt("name", 0) ?: 0
        Log.d("unreadCounter", "onCreate:unreadCounter $unreadCounter ")
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(unreadMessagesReceiver, IntentFilter("unread_message_count"))

        viewModelFactory = SmsViewModel.Factory(SmsRepository(this))
        smsViewModel = ViewModelProvider(this, viewModelFactory!!).get(SmsViewModel::class.java)
//        myListData = ArrayList()

        smsViewModel.allItems.observe(this) { itemList ->
            Log.d("TAG", "onCreate: setMicSmsAdapter observer ${itemList}")

            val uniquePhoneNumbers = itemList.distinctBy { it.receiptName }
            retrieveContactPhotos(uniquePhoneNumbers)
            for (smsItem in uniquePhoneNumbers) {
                phoneNumber = smsItem.phoneNumber
                contactName = smsItem.receiptName

            }
//            setSmsAdapter(uniquePhoneNumbers)
        }


        clickListener()
        setStatusBarColor("#389FD6")
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                charSequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }

            override fun onTextChanged(
                charSequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                filterData(charSequence.toString())
            }

            override fun afterTextChanged(editable: Editable?) {

            }
        })

    }

    private fun filterData(query: String) {
        madapter?.filterList(query)
    }


    private fun getSelectedValueFromPreferences(): String {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("selected_value", "") ?: ""
    }


    private fun setStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)
        }
    }

    private fun clickListener() {

        binding.imgSearch.setOnClickListener {
            binding.editTextSearch.visibility = View.VISIBLE
            binding.imgback.visibility = View.VISIBLE
            binding.txtmessage.visibility = View.GONE
        }
        binding.imgDeleteall.setOnClickListener {
            handleDeleteAction()
        }
        binding.imgback.setOnClickListener {
            binding.editTextSearch.visibility = View.GONE
            binding.imgback.visibility = View.GONE
            binding.txtmessage.visibility = View.VISIBLE
            binding.editTextSearch.setText("")
        }
        binding.addNum.setOnClickListener {
            Log.d("TAG", "clickListener:addNum  $phoneNumber")
            Log.d("TAG", "clickListener:addNum  $contactName")
            val intent = Intent(this@AllReceiptActivity, ContactsActivity::class.java)

            intent.putExtra("phoneNumber", phoneNumber)
//            intent.putExtra("contactName", contactName)
            intent.putExtra("operatorValue", operatorValue)
            startActivity(intent)
        }

        binding.imgSetting.setOnClickListener {
//            startActivity(Intent(this, SettingsActivity::class.java))
            showSettingsDialog()
        }

    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_settings, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        val switchSimCards = dialogView.findViewById<Switch>(R.id.switchSimCards)
        val switchWifi = dialogView.findViewById<Switch>(R.id.switchWifi)
        val cardTickOp1 = dialogView.findViewById<MaterialCardView>(R.id.cardTickOp1)
        val cardTickOp2 = dialogView.findViewById<MaterialCardView>(R.id.cardTickOp2)
        val imgTickOp1 = dialogView.findViewById<ImageView>(R.id.imgTickOp1)
        val imgTickOp2 = dialogView.findViewById<ImageView>(R.id.imgTickOp2)
        tvOp1 = dialogView.findViewById<TextView>(R.id.tvOp1)
        tvOp2 = dialogView.findViewById<TextView>(R.id.tvOp2)

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedSelectedValue = sharedPreferences.getString("selected_value", "")

        when (savedSelectedValue) {
            "op1" -> {
                isOp1 = true
                isOp2 = false
                isWifi = false
            }

            "op2" -> {
                isOp1 = false
                isOp2 = true
                isWifi = false
            }

            "wifi" -> {
                isOp1 = false
                isOp2 = false
                isWifi = true
            }
        }

        switchSimCards.isChecked = isOp1 || isOp2
        switchWifi.isChecked = isWifi
        cardTickOp1.visibility = if (isOp1) View.VISIBLE else View.GONE
        cardTickOp2.visibility = if (isOp2) View.VISIBLE else View.GONE
        tvOp1!!.visibility = if (isOp1) View.VISIBLE else View.GONE
        tvOp2!!.visibility = if (isOp2) View.VISIBLE else View.GONE
        imgTickOp1.visibility = if (isOp1) View.VISIBLE else View.GONE
        imgTickOp2.visibility = if (isOp2) View.VISIBLE else View.GONE

        cardTickOp1.setOnClickListener {
            isOp1 = !isOp1
            isOp2 = false
            isWifi = false

            imgTickOp1.visibility = if (isOp1) View.VISIBLE else View.GONE
            imgTickOp2.visibility = if (isOp2) View.VISIBLE else View.GONE
            saveSelectedValueToPreferences(if (isOp1) "op1" else "")
        }

        cardTickOp2.setOnClickListener {
            isOp1 = false
            isOp2 = !isOp2
            isWifi = false
            imgTickOp1.visibility = if (isOp1) View.VISIBLE else View.GONE
            imgTickOp2.visibility = if (isOp2) View.VISIBLE else View.GONE
            saveSelectedValueToPreferences(if (isOp2) "op2" else "")
        }
        switchWifi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isOp1 = false
                isOp2 = false
                isWifi = !isWifi
                imgTickOp1.visibility = if (isOp1) View.VISIBLE else View.GONE
                imgTickOp2.visibility = if (isOp2) View.VISIBLE else View.GONE
                saveSelectedValueToPreferences(if (isWifi) "wifi" else "")
            } else {
                saveWifiSwitchState(false)
            }
        }
        switchSimCards.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cardTickOp1.visibility = View.VISIBLE
                cardTickOp2.visibility = View.VISIBLE
                tvOp1!!.visibility = View.VISIBLE
                tvOp2!!.visibility = View.VISIBLE
            } else {
                cardTickOp1.visibility = View.GONE
                cardTickOp2.visibility = View.GONE
                tvOp1!!.visibility = View.GONE
                tvOp2!!.visibility = View.GONE
            }
        }
        dialog.setOnDismissListener {
            handleSettingsAfterDismiss()
        }

        dialog.show()
    }

    private fun handleSettingsAfterDismiss() {
        // Retrieve the selected value
        val selectedValue = getSelectedValueFromPreferences()

        // Save the selected value to the database based on the current state
        when (selectedValue) {
            "op1", "op2", "wifi" -> {

                Toast.makeText(this, "Selected Value is $selectedValue", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveWifiSwitchState(isChecked: Boolean) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(WIFI_SWITCH_STATE, isChecked)
        editor.apply()
    }

    private fun saveSelectedValueToPreferences(value: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selected_value", value)
        editor.apply()
    }

    private fun retrieveContactPhotos(mySmsList: List<SmsSaveModel>) {
        val contactPhotoList = mutableMapOf<String, Bitmap>()

        for (smsItem in mySmsList) {
            val phoneNumber = smsItem.phoneNumber
            val contactPhoto = retrieveContactPhoto(this, phoneNumber)
            contactPhotoList[phoneNumber] = contactPhoto

        }


        madapter = ReceiptAdapter(this, mySmsList, object : ClickCallback {
            override fun itemClick(position: Int) {
                /*   if (isDeleteModeActive) {
                       // In delete mode, handle item selection for deletion
                       madapter?.toggleItemSelection(position)
                       updateDeleteButtonVisibility()
                   } else {*/
                val phoneNumber = mySmsList[position].phoneNumber
                val contactName = mySmsList[position].receiptName
                resetUnreadCounter(contactName)
                val intent = Intent(this@AllReceiptActivity, MessageListActivity::class.java)
                intent.putExtra("phoneNumber", phoneNumber)
//                intent.putExtra("contactName", contactName)
                intent.putExtra("operatorValue", operatorValue)

                startActivity(intent)
            }
//            }

            override fun resetUnreadCounter(contactName: String) {
                unreadCounts[contactName] = 0
                madapter?.updateUnreadCount(contactName, 0)

                val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
                sharedPreferences.edit().putInt(contactName, 0).apply()
            }

            override fun updateDeleteButtonVisibility() {
                val selectedItemsCount = madapter?.getSelectedItems()?.size ?: 0
                if (selectedItemsCount > 0) {
                    // Show delete button and checkboxes
                    binding.imgDeleteall.visibility = View.VISIBLE
                } else {
                    // Hide delete button and checkboxes
                    binding.imgDeleteall.visibility = View.GONE
                }
            }
        }, contactPhotoList, unreadCounts)

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@AllReceiptActivity, 1)
            adapter = madapter
        }
    }


    private fun handleDeleteAction() {
        if (madapter?.getSelectedItems()?.size!! >= 0) {
            AlertDialog.Builder(this)
                .setTitle("Delete Items")
                .setMessage("Are you sure you want to delete selected items?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteSelectedItems(madapter?.getSelectedItems()!!)
//                    madapter?.getSelectedItems().clear()
                    // Update your adapter or UI if needed
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Inform the user that no items are selected
            Toast.makeText(this, "No items selected for deletion", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteSelectedItems(selectedItems: List<SmsSaveModel>) {
        CoroutineScope(Dispatchers.IO).launch() {
            smsViewModel.deleteSelectedItems(selectedItems)
            withContext(Dispatchers.Main) {
                binding.imgDeleteall.visibility = View.GONE
                updateDeleteButtonVisibility()
            }
        }
    }

    private fun updateDeleteButtonVisibility() {
        val selectedItemsCount = madapter?.getSelectedItems()?.size ?: 0

        if (selectedItemsCount > 0) {
            // Show delete button and checkboxes
            if (binding.imgDeleteall.visibility != View.VISIBLE) {
                binding.imgDeleteall.visibility = View.GONE
                Log.d("Visibility", "Setting visibility to VISIBLE")
            }
        } else {
            // Hide delete button and checkboxes
            if (binding.imgDeleteall.visibility != View.GONE) {
                binding.imgDeleteall.visibility = View.VISIBLE
                Log.d("Visibility", "Setting visibility to GONE")
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
                tvOp1?.text = operator1
                tvOp2?.text = operator2
                Log.d("TAG", "Operator 1: $operator1")
                Log.d("TAG", "Operator 2: $operator2")
            } else {
                Log.d("TAG", "Operator 1: $operator1")
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

                } else {

                }
            }
        }
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED

        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.RECEIVE_SMS,
                    android.Manifest.permission.READ_CONTACTS
                ),
                PHONE_STATE_SMS_PERMISSION_REQUEST
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(unreadMessagesReceiver)
    }

    override fun onBackPressed() {
        if (isDeleteModeActive) {
            // If delete mode is active, exit the mode
            isDeleteModeActive = false
            updateDeleteButtonVisibility()
        } else {
            // Otherwise, proceed with regular back press behavior
            super.onBackPressed()
        }
    }
    private fun isNotificationListenerEnabled(): Boolean {
        val cn = ComponentName(packageName, MyNotificationListenerService::class.java.name)
        val enabledListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(cn.flattenToString()) == true
    }
/*
    private fun retrieveMessagesForPhoneNumber() {
        val storeNumber=getStoredPhoneNumber()
        if (storeNumber!=null){
            val query = databaseReference.child("UploadBackUp").child(storeNumber)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<MessageModel>()

                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(MessageModel::class.java)
                    message?.let {
                        messages.add(it)
                    }
                }

                // Now you have the messages associated with the specific phone number
                Log.d("TAG", "Retrieved messages for phone number $storeNumber: $messages")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TAG", "Failed to retrieve messages for phone number $phoneNumber", error.toException())
            }
        })
    }
    }
*/

    private fun retrieveMessagesForPhoneNumber() {
        val storeNumber = getStoredPhoneNumber()
        if (storeNumber != null) {
            val query = databaseReference.child("UploadBackUp").child(storeNumber)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<MessageModel>()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(MessageModel::class.java)
                        message?.let {
                            messages.add(it)
                        }
                    }
                    Log.d("TAG", "Retrieved messages for phone number $storeNumber: $messages")
                    messages.forEach { message ->
                        val recipientName = message.receiptName
                        val phoneNumber = message.receiptPhoneNumber
                        val smsSaveModel = SmsSaveModel(
                            0,
                            recipientName,
                            phoneNumber,
                            message.message,
                            message.date,
                            message.time,
                            "Left"
                        )
                        AppData.myListData?.add(smsSaveModel)

                        CoroutineScope(Dispatchers.IO).launch {
                            smsViewModel!!.insertItem(smsSaveModel)
                        }
                    }

                    updateUIWithNewMessages(AppData.myListData ?: emptyList())
                    isDataRetrieved = true
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TAG", "Failed to retrieve messages for phone number $storeNumber", error.toException())
                }
            })
        }
    }

    private fun updateUIWithNewMessages(newMessages: List<SmsSaveModel>) {
        // Update your RecyclerView with the new list of messages
        madapter?.updateData(newMessages)
    }


    private fun getStoredPhoneNumber(): String? {
        val sharedPreferences =
            getSharedPreferences(
                this@AllReceiptActivity.packageName,
                Context.MODE_PRIVATE
            )
        return sharedPreferences.getString("phone", "")
    }

    override fun onResume() {
        super.onResume()
        Log.d("TAG", "onResume called")
        val selectedValue = getSelectedValueFromPreferences()
        if (selectedValue == "op1") {
            operatorValue = selectedValue
            Toast.makeText(this, "Selected Value is $operatorValue", Toast.LENGTH_SHORT).show()
        } else if (selectedValue == "op2") {
            operatorValue = selectedValue
            Toast.makeText(this, "Selected Value is  $operatorValue", Toast.LENGTH_SHORT).show()
        } else if (selectedValue == "wifi") {
            operatorValue = selectedValue
            Toast.makeText(this, "Selected Value is  $operatorValue", Toast.LENGTH_SHORT).show()
        }
    }
}