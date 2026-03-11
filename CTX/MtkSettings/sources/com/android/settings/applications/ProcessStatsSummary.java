package com.android.settings.applications;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.format.Formatter;
import com.android.settings.R;
import com.android.settings.SummaryPreference;
import com.android.settings.Utils;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.SummaryLoader;

public class ProcessStatsSummary extends ProcessStatsBase implements Preference.OnPreferenceClickListener {
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    private Preference mAppListPreference;
    private Preference mAverageUsed;
    private Preference mFree;
    private Preference mPerformance;
    private SummaryPreference mSummaryPref;
    private Preference mTotalMemory;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.process_stats_summary);
        this.mSummaryPref = (SummaryPreference) findPreference("status_header");
        this.mPerformance = findPreference("performance");
        this.mTotalMemory = findPreference("total_memory");
        this.mAverageUsed = findPreference("average_used");
        this.mFree = findPreference("free");
        this.mAppListPreference = findPreference("apps_list");
        this.mAppListPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void refreshUi() {
        CharSequence charSequence;
        Context context = getContext();
        ProcStatsData.MemInfo memInfo = this.mStatsManager.getMemInfo();
        double d = memInfo.realUsedRam;
        double d2 = memInfo.realTotalRam;
        double d3 = memInfo.realFreeRam;
        long j = (long) d;
        Formatter.BytesResult bytes = Formatter.formatBytes(context.getResources(), j, 1);
        long j2 = (long) d2;
        String shortFileSize = Formatter.formatShortFileSize(context, j2);
        String shortFileSize2 = Formatter.formatShortFileSize(context, (long) d3);
        CharSequence[] textArray = getResources().getTextArray(R.array.ram_states);
        int memState = this.mStatsManager.getMemState();
        if (memState >= 0 && memState < textArray.length - 1) {
            charSequence = textArray[memState];
        } else {
            charSequence = textArray[textArray.length - 1];
        }
        this.mSummaryPref.setAmount(bytes.value);
        this.mSummaryPref.setUnits(bytes.units);
        float f = (float) (d / (d3 + d));
        this.mSummaryPref.setRatios(f, 0.0f, 1.0f - f);
        this.mPerformance.setSummary(charSequence);
        this.mTotalMemory.setSummary(shortFileSize);
        this.mAverageUsed.setSummary(Utils.formatPercentage(j, j2));
        this.mFree.setSummary(shortFileSize2);
        String string = getString(sDurationLabels[this.mDurationIndex]);
        int size = this.mStatsManager.getEntries().size();
        this.mAppListPreference.setSummary(getResources().getQuantityString(R.plurals.memory_usage_apps_summary, size, Integer.valueOf(size), string));
    }

    @Override
    public int getMetricsCategory() {
        return 202;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_process_stats_summary;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mAppListPreference) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("transfer_stats", true);
            bundle.putInt("duration_index", this.mDurationIndex);
            this.mStatsManager.xferStats();
            new SubSettingLauncher(getContext()).setDestination(ProcessStatsUi.class.getName()).setTitle(R.string.memory_usage_apps).setArguments(bundle).setSourceMetricsCategory(getMetricsCategory()).launch();
            return true;
        }
        return false;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean z) {
            if (z) {
                ProcStatsData procStatsData = new ProcStatsData(this.mContext, false);
                procStatsData.setDuration(ProcessStatsBase.sDurations[0]);
                ProcStatsData.MemInfo memInfo = procStatsData.getMemInfo();
                this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.memory_summary, Formatter.formatShortFileSize(this.mContext, (long) memInfo.realUsedRam), Formatter.formatShortFileSize(this.mContext, (long) memInfo.realTotalRam)));
            }
        }
    }
}
