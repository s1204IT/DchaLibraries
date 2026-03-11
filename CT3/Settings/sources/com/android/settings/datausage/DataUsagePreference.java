package com.android.settings.datausage;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import android.util.AttributeSet;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.datausage.TemplatePreference;
import com.android.settingslib.net.DataUsageController;

public class DataUsagePreference extends Preference implements TemplatePreference {
    private int mSubId;
    private NetworkTemplate mTemplate;

    public DataUsagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setTemplate(NetworkTemplate template, int subId, TemplatePreference.NetworkServices services) {
        this.mTemplate = template;
        this.mSubId = subId;
        DataUsageController controller = new DataUsageController(getContext());
        DataUsageController.DataUsageInfo usageInfo = controller.getDataUsageInfo(this.mTemplate);
        setSummary(getContext().getString(R.string.data_usage_template, Formatter.formatFileSize(getContext(), usageInfo.usageLevel), usageInfo.period));
        setIntent(getIntent());
    }

    @Override
    public Intent getIntent() {
        Bundle args = new Bundle();
        args.putParcelable("network_template", this.mTemplate);
        args.putInt("sub_id", this.mSubId);
        return Utils.onBuildStartFragmentIntent(getContext(), DataUsageList.class.getName(), args, getContext().getPackageName(), 0, getTitle(), false);
    }
}
