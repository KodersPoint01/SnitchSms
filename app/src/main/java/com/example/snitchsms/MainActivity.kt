package com.example.snitchsms

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snitchsms.contacts.CallLogViewModel
import com.example.snitchsms.contacts.ContactsActivity
import com.example.snitchsms.contacts.adapter.SmsAdapter
import com.example.snitchsms.contacts.callbacks.SmsDelete
import com.example.snitchsms.contacts.model.ContactModel
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.databinding.ActivityMainBinding
import com.example.snitchsms.roomdatabase.SmsRepository
import com.example.snitchsms.roomdatabase.SmsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private val PHONE_STATE_SMS_PERMISSION_REQUEST = 101

    var phoneNumberRecept = "recipient_phone_number"
    var message = "Your SMS message here"
    var operator1 = ""
    var operator2 = ""

    private var madapter: SmsAdapter? = null
    private var myListData: ArrayList<SmsSaveModel>? = null
    private var arrayList: MutableList<ContactModel> = mutableListOf()
    private val mViewModel: CallLogViewModel by viewModels()


    private var viewModelFactory: SmsViewModel.Factory? = null
    private lateinit var smsViewModel: SmsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor("#389FD6")

        val phoneNumber = intent.getStringExtra("phoneNumber")
        val contactName = intent.getStringExtra("contactName")
        Log.d("TAG", "onCreate: Main phoneNumber $phoneNumber")
        ContactsActivity.arrayList.clear()
        CoroutineScope(Dispatchers.IO).launch {
            arrayList = mViewModel.getContacts(this@MainActivity)
        }
        viewModelFactory = SmsViewModel.Factory(SmsRepository(this))
        smsViewModel = ViewModelProvider(this, viewModelFactory!!).get(SmsViewModel::class.java)
        myListData = ArrayList()

        smsViewModel.allItems.observe(this) { itemList ->
            Log.d("TAG", "onCreate:setMicSmsAdapter observer  ${itemList.size}")
            if (phoneNumber != null && contactName != null) {
                binding.addNum.visibility = View.GONE
//                binding.clTop1.visibility=View.GONE
                phoneNumberRecept = phoneNumber
                binding.etSearch.setText(phoneNumber)

                val filteredMessages = filterMessagesForPhoneNumber(phoneNumber, itemList)
                Log.d("TAG", "Main: filteredMessages  ${filteredMessages}")

                setSmsAdapter(filteredMessages)
            }
//            setSmsAdapter(itemList as java.util.ArrayList<SmsSaveModel>)
        }

        val num = intent.getStringExtra("num")
        if (num != null) {
            binding.etSearch.setText(num.toString())
            phoneNumberRecept = num
        }

        getAllOperator()
        clickListener()


    }

    private fun setStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)
        }
    }

    /*   private fun filterMessagesForPhoneNumber(
           phoneNumber: String,
           allMessages: List<SmsSaveModel>
       ): List<SmsSaveModel> {
           return allMessages.filter { it.phoneNumber == phoneNumber }
       }*/
    private fun filterMessagesForPhoneNumber(
        phoneNumber: String,
        allMessages: List<SmsSaveModel>
    ): ArrayList<SmsSaveModel> {
        val filteredMessages = ArrayList<SmsSaveModel>()
        for (message in allMessages) {
            if (message.phoneNumber == phoneNumber) {
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
                    smsViewModel.deleteItem(deletedItem)
                }

                override fun itemLongClick(pos: Int) {
                    /*val dialog = SmsDetailDialog(this@MainActivity)
                    dialog.show()*/
                }


            })

            binding.recyclerSaveSms.apply {
                layoutManager = GridLayoutManager(this@MainActivity, 1)
                adapter = madapter
                scrollToPosition(mySmsList.size - 1)
            }
        }
    }

    private fun clickListener() {

        binding.imgSend.setOnClickListener {
            binding.cvOperator1.visibility = View.VISIBLE
            binding.cvOperator2.visibility = View.VISIBLE

        }

        binding.addNum.setOnClickListener {
            startActivity(Intent(this, ContactsActivity::class.java))
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
                smsViewModel.insertItem(
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
        binding.cvOperator2.setOnClickListener {
            message = binding.etsendmsg.text.toString()
            Log.d(
                "TAG",
                "clickListener: operator1 $operator2 phoneNumberRecept $phoneNumberRecept message $message"
            )
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


            myListData?.add(
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


                smsViewModel.insertItem(
                    SmsSaveModel(
                        0,
                        recipientName!!,
                        phoneNumberRecept,
                        message,
                        currentDate,
                        formattedTime, "Left"
                    )
                )
                setSmsAdapter(myListData!!)
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
        }
    }
}