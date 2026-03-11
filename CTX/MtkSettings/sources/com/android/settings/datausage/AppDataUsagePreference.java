package com.android.settings.datausage;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.PreferenceViewHolder;
import android.widget.ProgressBar;
import com.android.settings.R;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.AppItem;
import com.android.settingslib.net.UidDetail;
import com.android.settingslib.net.UidDetailProvider;
import com.android.settingslib.utils.ThreadUtils;

public class AppDataUsagePreference extends AppPreference {
    private UidDetail mDetail;
    private final AppItem mItem;
    private final int mPercent;

    public AppDataUsagePreference(Context context, AppItem appItem, int i, final UidDetailProvider uidDetailProvider) {
        super(context);
        this.mItem = appItem;
        this.mPercent = i;
        if (appItem.restricted && appItem.total <= 0) {
            setSummary(R.string.data_usage_app_restricted);
        } else {
            setSummary(DataUsageUtils.formatDataUsage(context, appItem.total));
        }
        this.mDetail = uidDetailProvider.getUidDetail(appItem.key, false);
        if (this.mDetail != null) {
            setAppInfo();
        } else {
            ThreadUtils.postOnBackgroundThread(new Runnable() {
                @Override
                public final void run() {
                    AppDataUsagePreference.lambda$new$1(this.f$0, uidDetailProvider);
                }
            });
        }
    }

    public static void lambda$new$1(final AppDataUsagePreference appDataUsagePreference, UidDetailProvider uidDetailProvider) {
        appDataUsagePreference.mDetail = uidDetailProvider.getUidDetail(appDataUsagePreference.mItem.key, true);
        ThreadUtils.postOnMainThread(new Runnable() {
            @Override
            public final void run() {
                this.f$0.setAppInfo();
            }
        });
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        ProgressBar progressBar = (ProgressBar) preferenceViewHolder.findViewById(android.R.id.progress);
        if (this.mItem.restricted && this.mItem.total <= 0) {
            progressBar.setVisibility(8);
        } else {
            progressBar.setVisibility(0);
        }
        progressBar.setProgress(this.mPercent);
    }

    public void setAppInfo() {
        if (this.mDetail != null) {
            setIcon(this.mDetail.icon);
            setTitle(this.mDetail.label);
        } else {
            setIcon((Drawable) null);
            setTitle((CharSequence) null);
        }
    }

    public AppItem getItem() {
        return this.mItem;
    }
}
