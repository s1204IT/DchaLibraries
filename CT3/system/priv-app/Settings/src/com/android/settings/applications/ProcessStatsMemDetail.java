package com.android.settings.applications;

import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Utils;
/* loaded from: classes.dex */
public class ProcessStatsMemDetail extends InstrumentedFragment {
    double mMemCachedWeight;
    double mMemFreeWeight;
    double mMemKernelWeight;
    double mMemNativeWeight;
    private ViewGroup mMemStateParent;
    double[] mMemStateWeights;
    long[] mMemTimes;
    double mMemTotalWeight;
    private ViewGroup mMemUseParent;
    double mMemZRamWeight;
    private View mRootView;
    long mTotalTime;
    boolean mUseUss;

    @Override // com.android.settings.InstrumentedFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle args = getArguments();
        this.mMemTimes = args.getLongArray("mem_times");
        this.mMemStateWeights = args.getDoubleArray("mem_state_weights");
        this.mMemCachedWeight = args.getDouble("mem_cached_weight");
        this.mMemFreeWeight = args.getDouble("mem_free_weight");
        this.mMemZRamWeight = args.getDouble("mem_zram_weight");
        this.mMemKernelWeight = args.getDouble("mem_kernel_weight");
        this.mMemNativeWeight = args.getDouble("mem_native_weight");
        this.mMemTotalWeight = args.getDouble("mem_total_weight");
        this.mUseUss = args.getBoolean("use_uss");
        this.mTotalTime = args.getLong("total_time");
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.process_stats_mem_details, container, false);
        Utils.prepareCustomPreferencesList(container, view, view, false);
        this.mRootView = view;
        createDetails();
        return view;
    }

    @Override // com.android.settings.InstrumentedFragment
    protected int getMetricsCategory() {
        return 22;
    }

    @Override // com.android.settings.InstrumentedFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
    }

    private void createDetails() {
        this.mMemStateParent = (ViewGroup) this.mRootView.findViewById(R.id.mem_state);
        this.mMemUseParent = (ViewGroup) this.mRootView.findViewById(R.id.mem_use);
        fillMemStateSection();
        fillMemUseSection();
    }

    private void addDetailsItem(ViewGroup parent, CharSequence title, float level, CharSequence value) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup item = (ViewGroup) inflater.inflate(R.layout.app_item, (ViewGroup) null);
        inflater.inflate(R.layout.widget_progress_bar, (ViewGroup) item.findViewById(16908312));
        parent.addView(item);
        item.findViewById(16908294).setVisibility(8);
        TextView titleView = (TextView) item.findViewById(16908310);
        TextView valueView = (TextView) item.findViewById(16908308);
        titleView.setText(title);
        valueView.setText(value);
        ProgressBar progress = (ProgressBar) item.findViewById(16908301);
        progress.setProgress(Math.round(100.0f * level));
    }

    private void fillMemStateSection() {
        CharSequence[] labels = getResources().getTextArray(R.array.proc_stats_memory_states);
        for (int i = 0; i < 4; i++) {
            if (this.mMemTimes[i] > 0) {
                float level = ((float) this.mMemTimes[i]) / ((float) this.mTotalTime);
                addDetailsItem(this.mMemStateParent, labels[i], level, Formatter.formatShortElapsedTime(getActivity(), this.mMemTimes[i]));
            }
        }
    }

    private void addMemUseDetailsItem(ViewGroup parent, CharSequence title, double weight) {
        if (weight <= 0.0d) {
            return;
        }
        float level = (float) (weight / this.mMemTotalWeight);
        String value = Formatter.formatShortFileSize(getActivity(), (long) ((1024.0d * weight) / this.mTotalTime));
        addDetailsItem(parent, title, level, value);
    }

    private void fillMemUseSection() {
        CharSequence[] labels = getResources().getTextArray(R.array.proc_stats_process_states);
        addMemUseDetailsItem(this.mMemUseParent, getResources().getText(R.string.mem_use_kernel_type), this.mMemKernelWeight);
        addMemUseDetailsItem(this.mMemUseParent, getResources().getText(R.string.mem_use_zram_type), this.mMemZRamWeight);
        addMemUseDetailsItem(this.mMemUseParent, getResources().getText(R.string.mem_use_native_type), this.mMemNativeWeight);
        for (int i = 0; i < 14; i++) {
            addMemUseDetailsItem(this.mMemUseParent, labels[i], this.mMemStateWeights[i]);
        }
        addMemUseDetailsItem(this.mMemUseParent, getResources().getText(R.string.mem_use_kernel_cache_type), this.mMemCachedWeight);
        addMemUseDetailsItem(this.mMemUseParent, getResources().getText(R.string.mem_use_free_type), this.mMemFreeWeight);
        addMemUseDetailsItem(this.mMemUseParent, getResources().getText(R.string.mem_use_total), this.mMemTotalWeight);
    }
}
