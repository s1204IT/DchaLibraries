package com.android.phone;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class SubscriptionInfoHelper {
    private static Context mContext;
    private static int mSubId = -1;
    private static String mSubLabel;

    public SubscriptionInfoHelper(Context context, Intent intent) {
        mContext = context;
        mSubId = intent.getIntExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId", -1);
        mSubLabel = intent.getStringExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionLabel");
    }

    public Intent getIntent(Class newActivityClass) {
        Intent intent = new Intent(mContext, (Class<?>) newActivityClass);
        if (hasSubId()) {
            intent.putExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId", mSubId);
        }
        if (!TextUtils.isEmpty(mSubLabel)) {
            intent.putExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionLabel", mSubLabel);
        }
        return intent;
    }

    public static void addExtrasToIntent(Intent intent, SubscriptionInfo subscription) {
        if (subscription != null) {
            intent.putExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionId", subscription.getSubscriptionId());
            intent.putExtra("com.android.phone.settings.SubscriptionInfoHelper.SubscriptionLabel", subscription.getDisplayName().toString());
        }
    }

    public Phone getPhone() {
        return hasSubId() ? PhoneFactory.getPhone(SubscriptionManager.getPhoneId(mSubId)) : PhoneGlobals.getPhone();
    }

    public void setActionBarTitle(ActionBar actionBar, Resources res, int resId) {
        if (actionBar != null && !TextUtils.isEmpty(mSubLabel) && TelephonyManager.from(mContext).isMultiSimEnabled()) {
            String title = String.format(res.getString(resId), mSubLabel);
            actionBar.setTitle(title);
        }
    }

    public boolean hasSubId() {
        return mSubId != -1;
    }

    public int getSubId() {
        return mSubId;
    }
}
