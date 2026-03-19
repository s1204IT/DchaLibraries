package com.mediatek.settings;

import android.content.Context;
import android.preference.SwitchPreference;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;
import com.android.phone.R;

public class Enhanced4GLteSwitchPreference extends SwitchPreference {
    private int mPhoneId;
    private int mSubId;

    public Enhanced4GLteSwitchPreference(Context context) {
        super(context);
    }

    public Enhanced4GLteSwitchPreference(Context context, int i) {
        this(context);
        this.mSubId = i;
        this.mPhoneId = SubscriptionManager.getPhoneId(i);
    }

    @Override
    protected void onClick() {
        if (canNotSetAdvanced4GMode()) {
            log("[onClick] can't set Enhanced 4G mode.");
            showTips(R.string.can_not_switch_enhanced_4g_lte_mode_tips);
        } else {
            log("[onClick] can set Enhanced 4G mode.");
            super.onClick();
        }
    }

    private boolean canNotSetAdvanced4GMode() {
        return TelephonyUtils.isInCall(getContext()) || TelephonyUtils.isAirplaneModeOn(getContext());
    }

    public void setSubId(int i) {
        this.mSubId = i;
    }

    private void showTips(int i) {
        Toast.makeText(getContext(), i, 0).show();
    }

    private void log(String str) {
        Log.d("Enhanced4GLteSwitchPreference", str);
    }
}
