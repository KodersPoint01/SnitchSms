package com.example.snitchsms

import android.app.Application
import androidx.room.Room
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.roomdatabase.SmsDatabase
import com.example.snitchsms.roomdatabase.SmsViewModel


class AppData : Application() {


    companion object {
        var database: SmsDatabase? = null


        //        var myListData: ArrayList<SmsSaveModel>? = null
        lateinit var smsViewModel: SmsViewModel
        var viewModelFactory: SmsViewModel.Factory? = null

        var myListData: MutableList<SmsSaveModel> = mutableListOf()
    }

    override fun onCreate() {
        super.onCreate()

        myListData = ArrayList()
        database = Room.databaseBuilder(this, SmsDatabase::class.java, "database")
            .build()
        /*   viewModelFactory = SmsViewModel.Factory(SmsRepository(this))
           smsViewModel = ViewModelProvider(
               applicationContext,
               viewModelFactory!!
           ).get(SmsViewModel::class.java)*/
    }
}
