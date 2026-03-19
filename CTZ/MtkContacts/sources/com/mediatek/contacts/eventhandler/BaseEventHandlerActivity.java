package com.mediatek.contacts.eventhandler;

import android.content.Intent;
import android.os.Bundle;
import com.android.contacts.activities.TransactionSafeActivity;
import com.mediatek.contacts.eventhandler.GeneralEventHandler;
import com.mediatek.contacts.util.Log;

public class BaseEventHandlerActivity extends TransactionSafeActivity implements GeneralEventHandler.Listener {
    private static String TAG = "BaseEventHandleActivity";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i(TAG, "[onCreate]");
        GeneralEventHandler.getInstance(this).register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "[onDestroy]");
        GeneralEventHandler.getInstance(this).unRegister(this);
    }

    public void onReceiveEvent(String str, Intent intent) {
        Log.d(TAG, "[onReceiveEvent] eventType: " + str + ", extraData: " + intent.toString());
        if (("PhbChangeEvent".equals(str) || "SdStateChangeEvenet".equals(str)) && !isFinishing()) {
            Log.i(TAG, "[onReceiveEvent] Phb and sd event,default action is finish!");
            finish();
        }
    }
}
