package com.mediatek.settings.ext;

import android.app.Activity;
import android.content.Context;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import java.util.List;

/* loaded from: classes.dex */
public interface IRCSSettings {
    void addRCSPreference(Activity activity, PreferenceScreen preferenceScreen);

    int getDefaultSmsClickContentExt(List<SubscriptionInfo> list, int i, int i2);

    boolean isNeedAskFirstItemForSms();

    void setDefaultSmsApplication(String str, Context context);
}
