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
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.process_stats_summary);
        this.mSummaryPref = (SummaryPreference) findPreference("status_header");
        int memColor = getContext().getColor(R.color.running_processes_apps_ram);
        this.mSummaryPref.setColors(memColor, memColor, getContext().getColor(R.color.running_processes_free_ram));
        this.mPerformance = findPreference("performance");
        this.mTotalMemory = findPreference("total_memory");
        this.mAverageUsed = findPreference("average_used");
        this.mFree = findPreference("free");
        this.mAppListPreference = findPreference("apps_list");
        this.mAppListPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void refreshUi() {
        CharSequence memString;
        Context context = getContext();
        ProcStatsData.MemInfo memInfo = this.mStatsManager.getMemInfo();
        double usedRam = memInfo.realUsedRam;
        double totalRam = memInfo.realTotalRam;
        double freeRam = memInfo.realFreeRam;
        Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(), (long) usedRam, 1);
        String totalString = Formatter.formatShortFileSize(context, (long) totalRam);
        String freeString = Formatter.formatShortFileSize(context, (long) freeRam);
        CharSequence[] memStatesStr = getResources().getTextArray(R.array.ram_states);
        int memState = this.mStatsManager.getMemState();
        if (memState >= 0 && memState < memStatesStr.length - 1) {
            memString = memStatesStr[memState];
        } else {
            memString = memStatesStr[memStatesStr.length - 1];
        }
        this.mSummaryPref.setAmount(usedResult.value);
        this.mSummaryPref.setUnits(usedResult.units);
        float usedRatio = (float) (usedRam / (freeRam + usedRam));
        this.mSummaryPref.setRatios(usedRatio, 0.0f, 1.0f - usedRatio);
        this.mPerformance.setSummary(memString);
        this.mTotalMemory.setSummary(totalString);
        this.mAverageUsed.setSummary(Utils.formatPercentage((long) usedRam, (long) totalRam));
        this.mFree.setSummary(freeString);
        String durationString = getString(sDurationLabels[this.mDurationIndex]);
        int numApps = this.mStatsManager.getEntries().size();
        this.mAppListPreference.setSummary(getResources().getQuantityString(R.plurals.memory_usage_apps_summary, numApps, Integer.valueOf(numApps), durationString));
    }

    @Override
    protected int getMetricsCategory() {
        return 202;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference != this.mAppListPreference) {
            return false;
        }
        Bundle args = new Bundle();
        args.putBoolean("transfer_stats", true);
        args.putInt("duration_index", this.mDurationIndex);
        this.mStatsManager.xferStats();
        startFragment(this, ProcessStatsUi.class.getName(), R.string.app_list_memory_use, 0, args);
        return true;
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            ProcStatsData statsManager = new ProcStatsData(this.mContext, false);
            statsManager.setDuration(ProcessStatsSummary.sDurations[0]);
            ProcStatsData.MemInfo memInfo = statsManager.getMemInfo();
            String usedResult = Formatter.formatShortFileSize(this.mContext, (long) memInfo.realUsedRam);
            String totalResult = Formatter.formatShortFileSize(this.mContext, (long) memInfo.realTotalRam);
            this.mSummaryLoader.setSummary(this, this.mContext.getString(R.string.memory_summary, usedResult, totalResult));
        }
    }
}
