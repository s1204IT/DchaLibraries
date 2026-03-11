package com.android.settings.applications.appops;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;

public class BackgroundCheckSummary extends InstrumentedPreferenceFragment {
    private LayoutInflater mInflater;

    @Override
    public int getMetricsCategory() {
        return 258;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getActivity().setTitle(R.string.background_check_pref);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mInflater = layoutInflater;
        View viewInflate = this.mInflater.inflate(R.layout.background_check_summary, viewGroup, false);
        if (viewGroup instanceof PreferenceFrameLayout) {
            viewInflate.getLayoutParams().removeBorders = true;
        }
        FragmentTransaction fragmentTransactionBeginTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.add(R.id.appops_content, new AppOpsCategory(AppOpsState.RUN_IN_BACKGROUND_TEMPLATE), "appops");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        return viewInflate;
    }
}
