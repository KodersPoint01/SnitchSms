package com.example.snitchsms.activities

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.snitchsms.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val PHONE_STATE_SMS_PERMISSION_REQUEST = 101
    var operator1 = ""
    var operator2 = ""
    var isOp1 = false
    var isOp2 = false
    var isWifi = false

    private val STATE_SELECTED_VALUE = "selected_value"
    private val STATE_OP1_VISIBILITY = "op1_visibility"
    private val STATE_OP2_VISIBILITY = "op2_visibility"
    private val STATE_WIFI_VISIBILITY = "wifi_visibility"
    private val WIFI_SWITCH_STATE = "wifi_switch_state"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColor("#389FD6")
        clickListener()
        getAllOperator()

        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isWifiSwitchChecked = sharedPreferences.getBoolean(WIFI_SWITCH_STATE, false)
        binding.switchWifi.isChecked = isWifiSwitchChecked

        savedInstanceState?.let {
            isOp1 = it.getBoolean(STATE_OP1_VISIBILITY)
            isOp2 = it.getBoolean(STATE_OP2_VISIBILITY)
            isWifi = it.getBoolean(STATE_WIFI_VISIBILITY)
            updateSelectionViews()
            saveSelectedValueToPreferences(it.getString(STATE_SELECTED_VALUE, ""))
        }

        if (savedInstanceState != null) {
            Log.d("TAG", "onCreate:  savedInstanceState")
            isOp1 = savedInstanceState.getBoolean(STATE_OP1_VISIBILITY)
            isOp2 = savedInstanceState.getBoolean(STATE_OP2_VISIBILITY)
            isWifi = savedInstanceState.getBoolean(STATE_WIFI_VISIBILITY)
            updateSelectionViews()
            saveSelectedValueToPreferences(savedInstanceState.getString(STATE_SELECTED_VALUE, ""))
        } else {
            val selectedValue = getSelectedValueFromPreferences()
            when (selectedValue) {
                "op1" -> isOp1 = true
                "op2" -> isOp2 = true
                "wifi" -> isWifi = true
            }
            updateSelectionViews()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save the current state
        outState.putBoolean(STATE_OP1_VISIBILITY, isOp1)
        outState.putBoolean(STATE_OP2_VISIBILITY, isOp2)
        outState.putBoolean(STATE_WIFI_VISIBILITY, isWifi)
        outState.putString(STATE_SELECTED_VALUE, getSelectedValueFromPreferences())

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState)
    }
    private fun clickListener() {
       /* binding.ivBackArrow.setOnClickListener {
            onBackPressed()
        }*/

        binding.cardTickOp1.setOnClickListener {
            isOp1 = !isOp1
            isOp2 = false
            isWifi = false
            updateSelectionViews()
            saveSelectedValueToPreferences(if (isOp1) "op1" else "")
        }

        binding.cardTickOp2.setOnClickListener {
            isOp1 = false
            isOp2 = !isOp2
            isWifi = false
            updateSelectionViews()
            saveSelectedValueToPreferences(if (isOp2) "op2" else "")
        }
        binding.switchWifi.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                isOp1 = false
                isOp2 = false
                isWifi = !isWifi
                updateSelectionViews()
                saveSelectedValueToPreferences(if (isWifi) "wifi" else "")
            } else {
                // Save Wi-Fi switch state when user disables it
                saveWifiSwitchState(false)
            }
        }
        binding.switchSimCards.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
               binding.cardTickOp1.visibility=View.VISIBLE
               binding.cardTickOp2.visibility=View.VISIBLE
               binding.tvOp1.visibility=View.VISIBLE
               binding.tvOp2.visibility=View.VISIBLE
            } else {
                binding.cardTickOp1.visibility=View.GONE
                binding.cardTickOp2.visibility=View.GONE
                binding.tvOp1.visibility=View.GONE
                binding.tvOp2.visibility=View.GONE
            }
        }

    }

    private fun updateSelectionViews() {
        binding.imgTickOp1.visibility = if (isOp1) View.VISIBLE else View.GONE
        binding.imgTickOp2.visibility = if (isOp2) View.VISIBLE else View.GONE
    }
    private fun saveSelectedValueToPreferences(value: String) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("selected_value", value)
        editor.apply()
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
    private fun saveWifiSwitchState(isChecked: Boolean) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(WIFI_SWITCH_STATE, isChecked)
        editor.apply()
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
    override fun onPause() {
        super.onPause()

        // Save Wi-Fi switch state when the activity is paused
        saveWifiSwitchState(binding.switchWifi.isChecked)
    }
    override fun onResume() {
        super.onResume()

        // Restore Wi-Fi switch state when the activity is resumed
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isWifiSwitchChecked = sharedPreferences.getBoolean(WIFI_SWITCH_STATE, false)
        binding.switchWifi.isChecked = isWifiSwitchChecked
    }

}