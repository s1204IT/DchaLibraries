package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import com.android.internal.telephony.SmsApplication;
import java.util.List;

public class DefaultRCSSettings implements IRCSSettings {
    private static final String TAG = "DefaultRCSSettings";

    @Override
    public void addRCSPreference(Activity activity, PreferenceScreen screen) {
        Log.d("@M_DefaultRCSSettings", TAG);
    }

    @Override
    public boolean isNeedAskFirstItemForSms() {
        Log.d("@M_DefaultRCSSettings", "isNeedAskFirstItemForSms");
        return true;
    }

    @Override
    public int getDefaultSmsClickContentExt(List<SubscriptionInfo> subInfoList, int value, int subId) {
        Log.d("@M_DefaultRCSSettings", "getDefaultSmsClickContent");
        return subId;
    }

    @Override
    public void setDefaultSmsApplication(String packageName, Context context) {
        SmsApplication.setDefaultApplication(packageName, context);
    }
}
