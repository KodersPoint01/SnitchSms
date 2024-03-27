package com.example.snitchsms.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snitchsms.MainActivity
import com.example.snitchsms.R
import com.example.snitchsms.contacts.adapter.ContactAdapter
import com.example.snitchsms.databinding.ActivityContactsBinding
import com.numbertracker.phonelocator.locationfinder.calllogs.DialerCallBack
import com.example.snitchsms.contacts.model.ContactModel
import com.example.snitchsms.recieptdata.MessageListActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactsActivity : AppCompatActivity(), DialerCallBack {
    lateinit var binding: ActivityContactsBinding
    private val mViewModel: CallLogViewModel by viewModels()

    var contactName = ""
    var phoneNumber = ""
    var operatorValue = ""
    companion object {
        var arrayList = ArrayList<ContactModel>()
    }

    private lateinit var contactAdapter: ContactAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
//        contactName = intent.getStringExtra("contactName") ?: ""
        operatorValue = intent.getStringExtra("operatorValue") ?: ""
        Log.d("TAG", "clickListener:addNum get $phoneNumber")
        Log.d("TAG", "clickListener:addNum  get $contactName")
        binding.tbContacts.ivBackArrow.setOnClickListener {
            onBackPressed()
        }


        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getContacts()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CONTACTS), 11
                )
            } else {
                getContacts()
            }
        }

        setStatusBarColor("#389FD6")
    }
    private fun setStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("TAG", "onRequestPermissionsResult: ")
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getContacts()
        } else {
            Toast.makeText(this, "Permission Declined by User", Toast.LENGTH_SHORT).show()
        }
    }

    /*@SuppressLint("Range", "NotifyDataSetChanged")
    private fun getContacts() {
        arrayList.clear()
        val cursor = this.contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,

                    ),null,null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
        while (cursor!!.moveToNext()){
            val contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            val contactModel =  ContactModel(contactName,contactNumber)
            arrayList.add(contactModel)
        }
        binding.apply {
            contactAdapter= ContactAdapter(arrayList,this@ContactsActivity)
            rvContacts.apply {
                layoutManager = LinearLayoutManager(this@ContactsActivity)
                adapter = contactAdapter
            }
        }
        contactAdapter.notifyDataSetChanged()
        cursor.close()
    }
    */
    @SuppressLint("Range", "NotifyDataSetChanged")
    private fun getContacts() {
        arrayList.clear()
        CoroutineScope(Dispatchers.IO).launch {
            arrayList = mViewModel.getContacts(this@ContactsActivity)

            withContext(Dispatchers.Main) {
                if (arrayList.size > 0) {
                    contactAdapter =
                        ContactAdapter(arrayList, this@ContactsActivity, this@ContactsActivity)
                    binding.rvContacts.apply {
                        layoutManager = LinearLayoutManager(this@ContactsActivity)
                        adapter = contactAdapter
                    }
                    contactAdapter.notifyDataSetChanged()
                    binding.tbContacts.etSearch.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            p0: CharSequence?,
                            p1: Int,
                            p2: Int,
                            p3: Int
                        ) {
                        }

                        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                        override fun afterTextChanged(p0: Editable?) {
                            filterContacts(p0.toString())
                        }
                    })
                } else {
                    binding.tvContactFound.visibility = View.VISIBLE
                    Toast.makeText(this@ContactsActivity, "No Contact Found", Toast.LENGTH_SHORT)
                        .show()
                }

            }
        }
    }

    private fun filterContacts(text: String) {

        val filteredList: ArrayList<ContactModel> = ArrayList()
        // running a for loop to compare elements.
        for (i in 0 until arrayList.size) {
            // checking if the entered string matched with any item of our recycler view.
            if (arrayList[i].contactName.toLowerCase().contains(text.toLowerCase())) {
                // if the item is matched we are
                // adding it to our filtered list.
                filteredList.add(arrayList[i])
            }
        }
        if (filteredList.isEmpty()) {
            // if no item is added in filtered list we are
            // displaying a toast message as no data found.
            Toast.makeText(this, "No Data Found..", Toast.LENGTH_SHORT).show()
        } else {
            // at last we are passing that filtered
            // list to our adapter class.
//            dieselAdapter.filterList(filteredList)
            contactAdapter.filterList(filteredList)
        }

    }


    override fun setIntent(model: ContactModel) {

        val intent = Intent(this, MessageListActivity::class.java)
        intent.putExtra("phoneNumber", model.number)
//        intent.putExtra("contactName", contactName)
        intent.putExtra("operatorValue", operatorValue)
        startActivity(intent)
    }


}