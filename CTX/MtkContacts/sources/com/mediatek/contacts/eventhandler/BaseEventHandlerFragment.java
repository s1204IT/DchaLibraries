package com.mediatek.contacts.eventhandler;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import com.mediatek.contacts.eventhandler.GeneralEventHandler;
import com.mediatek.contacts.util.Log;

public class BaseEventHandlerFragment extends Fragment implements GeneralEventHandler.Listener {
    private static final int DEFAULT_NO_USE_SUBID = -1;
    private static String TAG = "BaseEventHanleFragment";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.i(TAG, "[onCreate]");
        GeneralEventHandler.getInstance(getContext()).register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "[onDestroy]");
        GeneralEventHandler.getInstance(getContext()).unRegister(this);
    }

    public void onReceiveEvent(String str, Intent intent) {
        int subId = getSubId();
        int intExtra = intent.getIntExtra("subscription", -1000);
        Log.i(TAG, "[onReceiveEvent] eventType: " + str + ", extraData: " + intent.toString() + ",subId: " + subId + ",stateChangeSubId: " + intExtra);
        if ("PhbChangeEvent".equals(str) && SubscriptionManager.isValidSubscriptionId(subId) && SubscriptionManager.isValidSubscriptionId(intExtra) && subId == intExtra) {
            Log.i(TAG, "[onReceiveEvent] phb state change,default action: getActivity finish!");
            getActivity().finish();
        }
    }

    protected int getSubId() {
        return -1;
    }
}
