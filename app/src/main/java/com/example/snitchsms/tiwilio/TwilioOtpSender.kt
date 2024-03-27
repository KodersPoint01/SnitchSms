//package com.example.snitchsms.tiwilio
//
//import android.util.Log
//import com.twilio.Twilio
//import com.twilio.rest.api.v2010.account.Message
//import com.twilio.type.PhoneNumber
//
//class TwilioOtpSender {
//    // Your Twilio Account SID and Auth Token
//    private val accountSid = "AC4039f1744027c2f6ce9c0ea9591761ad"
//    private val authToken = "449053f9df29e9fc355d8be5394139b7"
//
//    init {
//        Twilio.init(accountSid, authToken)
//    }
//
//    fun sendOtp(destinationNumber: String, otp: String) {
//        val from = PhoneNumber("+923078126912")
//        val to = PhoneNumber(destinationNumber)
//        val messageBody = "Your OTP is: $otp"
//        Log.d("TwilioOtpSender", "sendOtp: $otp")
//        val message = Message.creator(to, from, messageBody).create()
//        Log.d("TwilioOtpSender", "sendOtp:  ${message.sid}")
//
//        println("Message SID: ${message.sid}")
//    }
//}
