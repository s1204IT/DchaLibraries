package com.mediatek.phone.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.util.Log;
import android.view.View;
import com.mediatek.phone.ext.ICallForwardExt;

public class DefaultCallForwardExt implements ICallForwardExt {
    private static final String LOG_TAG = "DefaultCallForwardExt";

    @Override
    public void onCreate(Activity activity, int i) {
        log("default onCreate()");
    }

    @Override
    public void onBindDialogView(EditTextPreference editTextPreference, View view) {
        log("default onBindDialogView()");
    }

    @Override
    public boolean onDialogClosed(EditTextPreference editTextPreference, int i) {
        log("default onDialogClosed()");
        return false;
    }

    @Override
    public boolean onCallForwardActivityResult(int i, int i2, Intent intent) {
        log("default onCallForwardActivityResult()");
        return false;
    }

    @Override
    public boolean getCallForwardInTimeSlot(EditTextPreference editTextPreference, Message message, Handler handler) {
        log("default getCallForwardInTimeSlot()");
        return false;
    }

    @Override
    public boolean setCallForwardInTimeSlot(EditTextPreference editTextPreference, int i, String str, int i2, Handler handler) {
        log("default setCallForwardInTimeSlot()");
        return false;
    }

    @Override
    public boolean handleGetCFInTimeSlotResponse(EditTextPreference editTextPreference, Message message) {
        log("default handleGetCFTimeSlotResponse()");
        return false;
    }

    @Override
    public void updateSummaryTimeSlotText(EditTextPreference editTextPreference, String[] strArr) {
        log("default updateSummaryTimeSlotText()");
    }

    @Override
    public void updateCfiIconEx(int i, Context context, ICallForwardExt.ICfiAction iCfiAction, boolean z) {
        log("default updateCfiIconEx()");
    }

    @Override
    public boolean isEnableCFUInTimeSlot(int i) {
        log("default isEnableCFUInTimeSlot()");
        return false;
    }

    void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
