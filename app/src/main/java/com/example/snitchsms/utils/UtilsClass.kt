package com.example.snitchsms.utils

import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowManager

class UtilsClass {
    companion object{

        private fun setStatusBarColor(activity: Activity, color: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window: Window = activity.window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = android.graphics.Color.parseColor(color)
            }
        }
    }
}