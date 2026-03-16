package com.android.deskclock;

public class Log {
    public static void i(String logMe) {
        android.util.Log.i("AlarmClock", logMe);
    }

    public static void e(String logMe) {
        android.util.Log.e("AlarmClock", logMe);
    }
}
