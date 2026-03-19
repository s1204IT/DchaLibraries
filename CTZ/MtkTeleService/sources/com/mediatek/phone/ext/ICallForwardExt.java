package com.mediatek.phone.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.view.View;

public interface ICallForwardExt {

    public interface ICfiAction {
        void updateCfiEx(int i, boolean z);
    }

    boolean getCallForwardInTimeSlot(EditTextPreference editTextPreference, Message message, Handler handler);

    boolean handleGetCFInTimeSlotResponse(EditTextPreference editTextPreference, Message message);

    boolean isEnableCFUInTimeSlot(int i);

    void onBindDialogView(EditTextPreference editTextPreference, View view);

    boolean onCallForwardActivityResult(int i, int i2, Intent intent);

    void onCreate(Activity activity, int i);

    boolean onDialogClosed(EditTextPreference editTextPreference, int i);

    boolean setCallForwardInTimeSlot(EditTextPreference editTextPreference, int i, String str, int i2, Handler handler);

    void updateCfiIconEx(int i, Context context, ICfiAction iCfiAction, boolean z);

    void updateSummaryTimeSlotText(EditTextPreference editTextPreference, String[] strArr);
}
