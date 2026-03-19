package com.android.storagemanager.deletionhelper;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.DateUtils;
import com.android.storagemanager.R;
import com.android.storagemanager.deletionhelper.AppsAsyncLoader;
import java.util.concurrent.TimeUnit;

public class AppDeletionPreference extends NestedDeletionPreference {
    private AppsAsyncLoader.PackageInfo mApp;
    private Context mContext;

    public AppDeletionPreference(Context context, AppsAsyncLoader.PackageInfo packageInfo) {
        super(context);
        this.mApp = packageInfo;
        this.mContext = context;
        setIcon(packageInfo.icon);
        setTitle(packageInfo.label);
        setItemSize(this.mApp.size);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        preferenceViewHolder.setDividerAllowedAbove(false);
    }

    public String getPackageName() {
        return this.mApp.packageName;
    }

    public void updateSummary() {
        if (this.mApp == null) {
            return;
        }
        if (this.mApp.daysSinceLastUse == Long.MAX_VALUE) {
            setSummary(this.mContext.getString(R.string.deletion_helper_app_summary_never_used));
            return;
        }
        if (this.mApp.daysSinceLastUse == -1) {
            setSummary(this.mContext.getString(R.string.deletion_helper_app_summary_unknown_used));
        } else if (this.mApp.daysSinceLastUse <= 1) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            setSummary(DateUtils.getRelativeTimeSpanString(jCurrentTimeMillis - TimeUnit.DAYS.toMillis(this.mApp.daysSinceLastUse), jCurrentTimeMillis, 86400000L, 0));
        } else {
            setSummary(this.mContext.getString(R.string.deletion_helper_app_summary, Long.valueOf(this.mApp.daysSinceLastUse)));
        }
    }
}
