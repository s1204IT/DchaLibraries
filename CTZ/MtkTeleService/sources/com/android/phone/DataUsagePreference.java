package com.android.phone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.os.BenesseExtension;
import android.os.Parcelable;
import android.preference.Preference;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.util.AttributeSet;
import com.android.settingslib.net.DataUsageController;

public class DataUsagePreference extends Preference {
    private int mSubId;
    private NetworkTemplate mTemplate;

    public DataUsagePreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void initialize(int i) {
        Activity activity = (Activity) getContext();
        this.mSubId = i;
        this.mTemplate = getNetworkTemplate(activity, i);
        DataUsageController.DataUsageInfo dataUsageInfo = new DataUsageController(activity).getDataUsageInfo(this.mTemplate);
        setSummary(activity.getString(R.string.data_usage_template, new Object[]{formatDataUsage(activity, dataUsageInfo.usageLevel), dataUsageInfo.period}));
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        setIntent(getIntent());
    }

    @Override
    public Intent getIntent() {
        Intent intent = new Intent("android.settings.MOBILE_DATA_USAGE");
        intent.putExtra("show_drawer_menu", true);
        intent.putExtra("network_template", (Parcelable) this.mTemplate);
        intent.putExtra("android.provider.extra.SUB_ID", this.mSubId);
        return intent;
    }

    private NetworkTemplate getNetworkTemplate(Activity activity, int i) {
        TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService("phone");
        return NetworkTemplate.normalize(NetworkTemplate.buildTemplateMobileAll(telephonyManager.getSubscriberId(i)), telephonyManager.getMergedSubscriberIds());
    }

    private CharSequence formatDataUsage(Context context, long j) {
        Formatter.BytesResult bytes = Formatter.formatBytes(context.getResources(), j, 8);
        return BidiFormatter.getInstance().unicodeWrap(context.getString(android.R.string.config_carrierAppInstallDialogComponent, bytes.value, bytes.units));
    }
}
