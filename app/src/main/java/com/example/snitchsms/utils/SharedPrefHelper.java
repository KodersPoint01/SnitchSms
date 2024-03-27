package com.example.snitchsms.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefHelper {
    private static final String PREF_NAME = "MyPrefs";
    private static final String PHONE_KEY = "PHONE";

    public static void savePHONE(Context context, String Phone) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PHONE_KEY, Phone);
        editor.apply();
    }

    public static String getSavedPHONE(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(PHONE_KEY, null);
    }
}

