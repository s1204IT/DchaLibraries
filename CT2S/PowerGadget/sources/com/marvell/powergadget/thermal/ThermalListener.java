package com.marvell.powergadget.thermal;

import android.app.NotificationManager;
import android.content.Context;
import android.os.SystemProperties;
import android.powerhint.PowerHintManager;
import android.util.Log;

public class ThermalListener {
    public static String TAG = "ThermalService";
    public Context mContext;
    public NotificationManager mNM = null;
    private String mPath;
    public String mTag;
    int mTripPoint0;
    int mTripPoint1;
    int mTripPoint2;
    int mTripPoint3;

    public ThermalListener(Context context, String path, String name) {
        this.mContext = context;
        this.mPath = path;
        this.mTag = name;
        getTripTemp();
    }

    private void getTripTemp() {
        int ret = Utils.readInfoInt(this.mPath + "/trip_point_0_temp");
        if (ret <= 0) {
            ret = 200000;
        }
        this.mTripPoint0 = ret;
        int ret2 = Utils.readInfoInt(this.mPath + "/trip_point_1_temp");
        if (ret2 <= 0) {
            ret2 = 200000;
        }
        this.mTripPoint1 = ret2;
        int ret3 = Utils.readInfoInt(this.mPath + "/trip_point_2_temp");
        if (ret3 <= 0) {
            ret3 = 200000;
        }
        this.mTripPoint2 = ret3;
        int ret4 = Utils.readInfoInt(this.mPath + "/trip_point_3_temp");
        if (ret4 <= 0) {
            ret4 = 200000;
        }
        this.mTripPoint3 = ret4;
        Log.d(TAG, this.mTag + ": Trip point 0-3: " + this.mTripPoint0 + "," + this.mTripPoint1 + "," + this.mTripPoint2 + "," + this.mTripPoint3);
    }

    public void tempChanged(int temp) {
        int currentState;
        String debug = SystemProperties.get("powerhint.thermal.debug");
        if (debug.equals("1")) {
            getTripTemp();
        }
        if (temp >= this.mTripPoint3) {
            tp3Handler();
            currentState = 3;
        } else if (temp >= this.mTripPoint2) {
            tp2Handler();
            currentState = 2;
        } else if (temp >= this.mTripPoint1) {
            tp1Handler();
            currentState = 1;
        } else if (temp >= this.mTripPoint0) {
            tp0Handler();
            currentState = 0;
        } else {
            safeHandler();
            currentState = -1;
        }
        String data = this.mTag + ">" + Integer.toString(currentState);
        Log.i(TAG, this.mTag + ": Send powerhint data: " + data);
        PowerHintManager phm = new PowerHintManager();
        phm.sendPowerHint("thermal", data);
    }

    public void safeHandler() {
        Log.d(TAG, this.mTag + ": Safe handler is triggered !");
    }

    public void tp0Handler() {
        Log.d(TAG, this.mTag + ": Trip0 handler is triggered !");
    }

    public void tp1Handler() {
        Log.d(TAG, this.mTag + ": Trip1 handler is triggered !");
    }

    public void tp2Handler() {
        Log.d(TAG, this.mTag + ": Trip2 handler is triggered !");
    }

    public void tp3Handler() {
        Log.d(TAG, this.mTag + ": Trip3 handler is triggered !");
    }
}
