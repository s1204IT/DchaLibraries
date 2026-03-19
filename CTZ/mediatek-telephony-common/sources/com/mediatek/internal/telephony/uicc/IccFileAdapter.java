package com.mediatek.internal.telephony.uicc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.Rlog;
import com.android.internal.telephony.Phone;

public class IccFileAdapter {
    private static final String TAG = "IccFileAdapter";
    private static IccFileAdapter sInstance;
    private Context mContext;
    private Phone mPhone;
    private int mPhoneId;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    };

    public IccFileAdapter(Context context, Phone phone) {
        log("IccFileAdapter Creating!");
        this.mContext = context;
        this.mPhone = phone;
        this.mPhoneId = this.mPhone.getPhoneId();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.RADIO_TECHNOLOGY");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
    }

    protected void log(String str) {
        Rlog.d(TAG, str + " (phoneId " + this.mPhoneId + ")");
    }

    protected void loge(String str) {
        Rlog.e(TAG, str + " (phoneId " + this.mPhoneId + ")");
    }
}
