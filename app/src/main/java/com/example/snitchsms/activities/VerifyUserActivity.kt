package com.example.snitchsms.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.snitchsms.MainActivity
import com.example.snitchsms.R
import com.example.snitchsms.databinding.ActivityVerifyUserBinding
import com.example.snitchsms.recieptdata.AllReceiptActivity
import com.example.snitchsms.utils.SharedPrefHelper
import com.example.snitchsms.utils.Utilities
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class VerifyUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyUserBinding
    private lateinit var mAuth: FirebaseAuth

    private lateinit var edtPhone: EditText
    private lateinit var edtOTP: EditText
    private lateinit var verifyOTPBtn: Button
    private lateinit var generateOTPBtn: Button
    private var verificationId: String? = null
    var randomNumberOtp = ""
    lateinit var utilities: Utilities
    var sharedPreferences: SharedPreferences? = null
    var phone: String = ""
    private val PHONE_STATE_SMS_PERMISSION_REQUEST = 101

    private val unreadMessagesReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "otp") {
                Log.d("TAG", "unreadMessagesReceiver: action ")
                val name = intent.getStringExtra("otp")
                Log.d("TAG", "unreadMessagesReceiver: name $name ")

                if (name!!.isNotEmpty()) {
                    edtOTP.text = Editable.Factory.getInstance().newEditable(name)
                }
                Log.d(
                    "TAG",
                    "unreadMessagesReceiver: name ${name}  + unreadCount ${name}"
                )
                Log.d(
                    "TAG",
                    "unreadMessagesReceiver: unreadCounts name ${name}  "
                )

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        val twilioOtpSender = TwilioOtpSender()
        val savedCID: String? = SharedPrefHelper.getSavedPHONE(this)
        if (savedCID != null) {
            startActivity(Intent(this, AllReceiptActivity::class.java))
            finish()
            return
        }
        setStatusBarColor("#389FD6")

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions for READ_PHONE_STATE and SEND_SMS
            ActivityCompat.requestPermissions(
                this,
                arrayOf(

                    android.Manifest.permission.RECEIVE_SMS
                ),
                PHONE_STATE_SMS_PERMISSION_REQUEST
            )
        }

        sharedPreferences = getSharedPreferences(this.packageName, Context.MODE_PRIVATE)
        mAuth = FirebaseAuth.getInstance()
        edtPhone = findViewById(R.id.idEdtPhoneNumber)
        edtOTP = findViewById(R.id.idEdtOtp)
        verifyOTPBtn = findViewById(R.id.idBtnVerify)
        generateOTPBtn = findViewById(R.id.idBtnGetOtp)
        utilities = Utilities(this@VerifyUserActivity)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(unreadMessagesReceiver, IntentFilter("otp"))
        generateOTPBtn.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            if (TextUtils.isEmpty(edtPhone.text.toString()) || TextUtils.isEmpty(binding.countryCode.text.toString())) {

                Toast.makeText(
                    this@VerifyUserActivity,
                    "Please enter a valid phone number.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.d("VerifiyActivity123", "click done")

                phone = binding.countryCode.text.toString() + edtPhone.text.toString()

//                sendVerificationCode(phone)
//                binding.progressbar.visibility = View.VISIBLE
//                binding.idBtnGetOtp.visibility = View.GONE

                if (utilities.isConnectingToInternet(this@VerifyUserActivity)) {
                    try {
                        // Use Kotlin Coroutines to perform the network request on a background thread

                        val client = OkHttpClient()
                        randomNumberOtp = generateRandomOTP()
                        val mediaType = "application/x-www-form-urlencoded".toMediaTypeOrNull()
                        val body =
                            "From=+14403055828&Body=$randomNumberOtp&To=$phone".toRequestBody(
                                mediaType
                            )
                        val oldurl =
                            "https://api.twilio.com/2010-04-01/Accounts/AC9024dd4cd2f86095919000393aba6150/Messages.json"
                        val request = Request.Builder()
                            .url("https://api.twilio.com/2010-04-01/Accounts/AC4039f1744027c2f6ce9c0ea9591761ad/Messages.json")
                            .post(body)
                            .addHeader("Content-Type", "application/x-www-form-urlencoded")
                            .addHeader(
                                "Authorization",
                                "Basic QUM0MDM5ZjE3NDQwMjdjMmY2Y2U5YzBlYTk1OTE3NjFhZDo0NDkwNTNmOWRmMjllOWZjMzU1ZDhiZTUzOTQxMzliNw=="
                            )
                            .build()

                        // Use enqueue for an asynchronous request
                        client.newCall(request).enqueue(object : Callback {
                            override fun onResponse(call: Call, response: Response) {
                                Log.d(
                                    "VerifiyActivity123",
                                    "onResponse:${response.body.toString()}"
                                )
                                // Handle the response on the main thread if needed
                                runOnUiThread {
                                    try {
                                        val responseData = response.body?.string()
                                        Log.d("VerifiyActivity123", "responseData:$responseData")

                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(this@VerifyUserActivity, "${response.body?.string()}", Toast.LENGTH_SHORT).show()
//                                        generateOTPBtn.text = responseData
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }

                            override fun onFailure(call: Call, e: IOException) {
                                Log.d("VerifiyActivity123", "onFailure:${e.message}")
                                e.printStackTrace()
                            }
                        })


                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(
                        this@VerifyUserActivity,
                        "Please connect to internet",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }


        verifyOTPBtn.setOnClickListener {
            if (TextUtils.isEmpty(edtOTP.text.toString())) {
                Toast.makeText(this@VerifyUserActivity, "Please enter OTP", Toast.LENGTH_SHORT)
                    .show()
            } else {
//                verifyCode(edtOTP.text.toString())
                if (edtOTP.text.toString() == randomNumberOtp) {
                    sharedPreferences!!.edit().putString("phone", phone).apply()
                    SharedPrefHelper.savePHONE(this, phone)
                    val i = Intent(this@VerifyUserActivity, AllReceiptActivity::class.java)
                    startActivity(i)
                    finish()
                } else {
                    Toast.makeText(this@VerifyUserActivity, "Wrong Otp", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateRandomOTP(): String {
        // Generate a random 6-digit OTP
        return (100000..999999).random().toString()
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this@VerifyUserActivity, OnCompleteListener<AuthResult> { task ->
                Log.d("VerifiyActivity123", "onVerificationCompleted:signInWithCredential")

                if (task.isSuccessful) {
                    Log.d("VerifiyActivity123", "onVerificationCompleted:isSuccessful")

                    val i = Intent(this@VerifyUserActivity, AllReceiptActivity::class.java)
                    startActivity(i)
                    finish()
                } else {
                    Log.d("VerifiyActivity123", "onVerificationCompleted: not  isSuccessful")

                    Toast.makeText(
                        this@VerifyUserActivity,
                        task.exception?.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun sendVerificationCode(number: String) {
        Log.d("VerifiyActivity123", "onVerificationCompleted:sendVerificationCode")

        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(mCallBack)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val mCallBack = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onCodeSent(
            s: String,
            forceResendingToken: PhoneAuthProvider.ForceResendingToken
        ) {
            super.onCodeSent(s, forceResendingToken)
            verificationId = s
            Log.d("VerifiyActivity123", "onVerificationCompleted:verificationId $verificationId ")
        }

        override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {

            val code = phoneAuthCredential.smsCode
            Log.d("VerifiyActivity123", "onVerificationCompleted:code $code ")
            if (code != null) {
                edtOTP.setText(code)
                Log.d("VerifiyActivity123", "onVerificationCompleted:code is not null")

                verifyCode(code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // displaying an error message with firebase exception.
            Toast.makeText(this@VerifyUserActivity, e.message, Toast.LENGTH_LONG).show()
            binding.progressbar.visibility = View.GONE
            binding.idBtnGetOtp.visibility = View.VISIBLE
        }
    }

    // below method is used to verify code from Firebase.
    private fun verifyCode(code: String) {
        Log.d("VerifiyActivity123", "onVerificationCompleted:verifyCode")

        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithCredential(credential)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PHONE_STATE_SMS_PERMISSION_REQUEST) {
            // Check if the permission is granted
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now receive SMS
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    private fun setStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = android.graphics.Color.parseColor(color)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(unreadMessagesReceiver)

    }
}

