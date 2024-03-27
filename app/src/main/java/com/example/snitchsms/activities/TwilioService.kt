package com.example.snitchsms.activities

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import kotlin.random.Random

class TwilioService {
    private val ACCOUNT_SID = "your_account_sid"
    private val AUTH_TOKEN = "your_auth_token"
    private val TWILIO_PHONE_NUMBER = "your_twilio_phone_number"

    init {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN)
    }

    fun sendVerificationCode(phoneNumber: String, verificationCode: String) {
        val message = Message.creator(
            PhoneNumber(phoneNumber),
            PhoneNumber(TWILIO_PHONE_NUMBER),
            "Your verification code is: $verificationCode"
        ).create()

        // Handle the response as needed
        // Optionally, you can log or check the message SID or status
    }
}