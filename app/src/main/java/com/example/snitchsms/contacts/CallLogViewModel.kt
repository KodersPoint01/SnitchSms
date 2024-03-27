package com.example.snitchsms.contacts

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.snitchsms.contacts.model.ContactModel

class CallLogViewModel : ViewModel(){

  /*  @SuppressLint("Range")
     suspend fun fetchCallLog(context: Context):ArrayList<CallLogModel> {
        // reading all data in descending order according to DATE
        val sortOrder = CallLog.Calls.DATE + " DESC"
        val cursor: Cursor? = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            sortOrder
        )

        //clearing the arraylist
       val arrayList =ArrayList<CallLogModel>()
        //looping through the cursor to add data into arraylist
        while (cursor!!.moveToNext()){
            val callerName = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME))
            val callerNumber = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER))
            val  callerNameSave = if (callerName == null || callerName.equals("")) callerNumber else callerName
            var callerType=cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE))
            var callerDuration=cursor.getString(cursor.getColumnIndex(CallLog.Calls.DURATION))
            val callerFullDate=cursor.getString(cursor.getColumnIndex(CallLog.Calls.DATE))

            val dateFormatter = SimpleDateFormat(
                "dd MMM yyyy"
            )

            val callerDate = dateFormatter.format(Date(callerFullDate.toLong()))

            val timeFormatter = SimpleDateFormat(
                "HH:mm:ss"
            )
            var callTime = timeFormatter.format(Date(callerFullDate.toLong()))

            callTime=HelperCallLog.getFormatedDateTime(callTime, "HH:mm:ss", "hh:mm a")

            callerDuration =HelperCallLog.durationFormat(callerDuration)

            val callLogModel =  CallLogModel(callerNameSave,callerNumber,callTime,callerDate,callerType,callerDuration)
            arrayList.add(callLogModel)
        }
        cursor.close()
        return arrayList
    }*/


    @SuppressLint("Range", "NotifyDataSetChanged")
    suspend fun getContacts(context: Context): ArrayList<ContactModel> {
        val arrayList = ArrayList<ContactModel>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val contactName =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val contactNumber =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                Log.d("CallLogViewModel", "getContacts: $contactName ,$contactNumber ")
                val contactModel = ContactModel(contactName ?: "", contactNumber ?: "")
                contactModel.let {
                    arrayList.add(it)
                }
            }
            cursor.close()
        } else {
            // Handle the case where the cursor is null
            Log.e("CallLogViewModel", "Cursor is null")
        }

        return arrayList
    }

}