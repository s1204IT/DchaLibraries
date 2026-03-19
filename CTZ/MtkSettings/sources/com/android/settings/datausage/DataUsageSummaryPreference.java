package com.android.settings.datausage;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.Utils;
import com.android.settingslib.utils.StringUtil;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DataUsageSummaryPreference extends Preference {
    private CharSequence mCarrierName;
    private boolean mChartEnabled;
    private long mCycleEndTimeMs;
    private long mDataplanSize;
    private long mDataplanUse;
    private CharSequence mEndLabel;
    private boolean mHasMobileData;
    private Intent mLaunchIntent;
    private String mLimitInfoText;
    private int mNumPlans;
    private float mProgress;
    private long mSnapshotTimeMs;
    private CharSequence mStartLabel;
    private String mUsagePeriod;
    private boolean mWifiMode;
    private static final long MILLIS_IN_A_DAY = TimeUnit.DAYS.toMillis(1);
    private static final long WARNING_AGE = TimeUnit.HOURS.toMillis(6);

    @VisibleForTesting
    static final Typeface SANS_SERIF_MEDIUM = Typeface.create("sans-serif-medium", 0);

    public DataUsageSummaryPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mChartEnabled = true;
        setLayoutResource(R.layout.data_usage_summary_preference);
    }

    public void setLimitInfo(String str) {
        if (!Objects.equals(str, this.mLimitInfoText)) {
            this.mLimitInfoText = str;
            notifyChanged();
        }
    }

    public void setProgress(float f) {
        this.mProgress = f;
        notifyChanged();
    }

    public void setUsageInfo(long j, long j2, CharSequence charSequence, int i, Intent intent) {
        this.mCycleEndTimeMs = j;
        this.mSnapshotTimeMs = j2;
        this.mCarrierName = charSequence;
        this.mNumPlans = i;
        this.mLaunchIntent = intent;
        notifyChanged();
    }

    public void setChartEnabled(boolean z) {
        if (this.mChartEnabled != z) {
            this.mChartEnabled = z;
            notifyChanged();
        }
    }

    public void setLabels(CharSequence charSequence, CharSequence charSequence2) {
        this.mStartLabel = charSequence;
        this.mEndLabel = charSequence2;
        notifyChanged();
    }

    void setUsageNumbers(long j, long j2, boolean z) {
        this.mDataplanUse = j;
        this.mDataplanSize = j2;
        this.mHasMobileData = z;
        notifyChanged();
    }

    void setWifiMode(boolean z, String str) {
        this.mWifiMode = z;
        this.mUsagePeriod = str;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        ProgressBar progressBar = (ProgressBar) preferenceViewHolder.findViewById(R.id.determinateBar);
        if (this.mChartEnabled && (!TextUtils.isEmpty(this.mStartLabel) || !TextUtils.isEmpty(this.mEndLabel))) {
            progressBar.setVisibility(0);
            preferenceViewHolder.findViewById(R.id.label_bar).setVisibility(0);
            progressBar.setProgress((int) (this.mProgress * 100.0f));
            ((TextView) preferenceViewHolder.findViewById(android.R.id.text1)).setText(this.mStartLabel);
            ((TextView) preferenceViewHolder.findViewById(android.R.id.text2)).setText(this.mEndLabel);
        } else {
            progressBar.setVisibility(8);
            preferenceViewHolder.findViewById(R.id.label_bar).setVisibility(8);
        }
        updateDataUsageLabels(preferenceViewHolder);
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.usage_title);
        TextView textView2 = (TextView) preferenceViewHolder.findViewById(R.id.carrier_and_update);
        Button button = (Button) preferenceViewHolder.findViewById(R.id.launch_mdp_app_button);
        TextView textView3 = (TextView) preferenceViewHolder.findViewById(R.id.data_limits);
        if (this.mWifiMode) {
            textView.setText(R.string.data_usage_wifi_title);
            textView.setVisibility(0);
            ((TextView) preferenceViewHolder.findViewById(R.id.cycle_left_time)).setText(this.mUsagePeriod);
            textView2.setVisibility(8);
            textView3.setVisibility(8);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    DataUsageSummaryPreference.launchWifiDataUsage(this.f$0.getContext());
                }
            });
            button.setText(R.string.launch_wifi_text);
            button.setVisibility(0);
            return;
        }
        textView.setVisibility(this.mNumPlans > 1 ? 0 : 8);
        updateCycleTimeText(preferenceViewHolder);
        updateCarrierInfo(textView2);
        if (this.mLaunchIntent != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    DataUsageSummaryPreference dataUsageSummaryPreference = this.f$0;
                    dataUsageSummaryPreference.getContext().startActivity(dataUsageSummaryPreference.mLaunchIntent);
                }
            });
            button.setVisibility(0);
        } else {
            button.setVisibility(8);
        }
        textView3.setVisibility(TextUtils.isEmpty(this.mLimitInfoText) ? 8 : 0);
        textView3.setText(this.mLimitInfoText);
    }

    private static void launchWifiDataUsage(Context context) {
        Bundle bundle = new Bundle(1);
        bundle.putParcelable("network_template", NetworkTemplate.buildTemplateWifiWildcard());
        SubSettingLauncher sourceMetricsCategory = new SubSettingLauncher(context).setArguments(bundle).setDestination(DataUsageList.class.getName()).setSourceMetricsCategory(0);
        sourceMetricsCategory.setTitle(context.getString(R.string.wifi_data_usage));
        sourceMetricsCategory.launch();
    }

    private void updateDataUsageLabels(PreferenceViewHolder preferenceViewHolder) {
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.data_usage_view);
        Formatter.BytesResult bytes = Formatter.formatBytes(getContext().getResources(), this.mDataplanUse, 10);
        SpannableString spannableString = new SpannableString(bytes.value);
        spannableString.setSpan(new AbsoluteSizeSpan(getContext().getResources().getDimensionPixelSize(R.dimen.usage_number_text_size)), 0, spannableString.length(), 33);
        textView.setText(TextUtils.expandTemplate(getContext().getText(R.string.data_used_formatted), spannableString, bytes.units));
        MeasurableLinearLayout measurableLinearLayout = (MeasurableLinearLayout) preferenceViewHolder.findViewById(R.id.usage_layout);
        if (this.mHasMobileData && this.mNumPlans >= 0 && this.mDataplanSize > 0) {
            TextView textView2 = (TextView) preferenceViewHolder.findViewById(R.id.data_remaining_view);
            long j = this.mDataplanSize - this.mDataplanUse;
            if (j >= 0) {
                textView2.setText(TextUtils.expandTemplate(getContext().getText(R.string.data_remaining), DataUsageUtils.formatDataUsage(getContext(), j)));
                textView2.setTextColor(Utils.getColorAttr(getContext(), android.R.attr.colorAccent));
            } else {
                textView2.setText(TextUtils.expandTemplate(getContext().getText(R.string.data_overusage), DataUsageUtils.formatDataUsage(getContext(), -j)));
                textView2.setTextColor(Utils.getColorAttr(getContext(), android.R.attr.colorError));
            }
            measurableLinearLayout.setChildren(textView, textView2);
            return;
        }
        measurableLinearLayout.setChildren(textView, null);
    }

    private void updateCycleTimeText(PreferenceViewHolder preferenceViewHolder) {
        String quantityString;
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.cycle_left_time);
        long jCurrentTimeMillis = this.mCycleEndTimeMs - System.currentTimeMillis();
        if (jCurrentTimeMillis <= 0) {
            textView.setText(getContext().getString(R.string.billing_cycle_none_left));
            return;
        }
        int i = (int) (jCurrentTimeMillis / MILLIS_IN_A_DAY);
        if (i < 1) {
            quantityString = getContext().getString(R.string.billing_cycle_less_than_one_day_left);
        } else {
            quantityString = getContext().getResources().getQuantityString(R.plurals.billing_cycle_days_left, i, Integer.valueOf(i));
        }
        textView.setText(quantityString);
    }

    private void updateCarrierInfo(TextView textView) {
        int i;
        int i2;
        if (this.mNumPlans > 0 && this.mSnapshotTimeMs >= 0) {
            textView.setVisibility(0);
            long jCalculateTruncatedUpdateAge = calculateTruncatedUpdateAge();
            CharSequence elapsedTime = null;
            if (jCalculateTruncatedUpdateAge == 0) {
                if (this.mCarrierName != null) {
                    i2 = R.string.carrier_and_update_now_text;
                } else {
                    i2 = R.string.no_carrier_update_now_text;
                }
            } else {
                if (this.mCarrierName != null) {
                    i = R.string.carrier_and_update_text;
                } else {
                    i = R.string.no_carrier_update_text;
                }
                i2 = i;
                elapsedTime = StringUtil.formatElapsedTime(getContext(), jCalculateTruncatedUpdateAge, false);
            }
            textView.setText(TextUtils.expandTemplate(getContext().getText(i2), this.mCarrierName, elapsedTime));
            if (jCalculateTruncatedUpdateAge <= WARNING_AGE) {
                setCarrierInfoTextStyle(textView, android.R.attr.textColorSecondary, Typeface.SANS_SERIF);
                return;
            } else {
                setCarrierInfoTextStyle(textView, android.R.attr.colorError, SANS_SERIF_MEDIUM);
                return;
            }
        }
        textView.setVisibility(8);
    }

    private long calculateTruncatedUpdateAge() {
        long jCurrentTimeMillis = System.currentTimeMillis() - this.mSnapshotTimeMs;
        if (jCurrentTimeMillis >= TimeUnit.DAYS.toMillis(1L)) {
            return (jCurrentTimeMillis / TimeUnit.DAYS.toMillis(1L)) * TimeUnit.DAYS.toMillis(1L);
        }
        if (jCurrentTimeMillis >= TimeUnit.HOURS.toMillis(1L)) {
            return (jCurrentTimeMillis / TimeUnit.HOURS.toMillis(1L)) * TimeUnit.HOURS.toMillis(1L);
        }
        if (jCurrentTimeMillis >= TimeUnit.MINUTES.toMillis(1L)) {
            return (jCurrentTimeMillis / TimeUnit.MINUTES.toMillis(1L)) * TimeUnit.MINUTES.toMillis(1L);
        }
        return 0L;
    }

    private void setCarrierInfoTextStyle(TextView textView, int i, Typeface typeface) {
        textView.setTextColor(Utils.getColorAttr(getContext(), i));
        textView.setTypeface(typeface);
    }
}
