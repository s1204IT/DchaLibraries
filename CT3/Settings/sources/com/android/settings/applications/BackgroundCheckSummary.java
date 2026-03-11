package com.android.settings.applications;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceFrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;

public class BackgroundCheckSummary extends InstrumentedFragment {
    private LayoutInflater mInflater;

    @Override
    protected int getMetricsCategory() {
        return 258;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mInflater = inflater;
        View rootView = this.mInflater.inflate(R.layout.background_check_summary, container, false);
        if (container instanceof PreferenceFrameLayout) {
            rootView.getLayoutParams().removeBorders = true;
        }
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.add(R.id.appops_content, new AppOpsCategory(AppOpsState.RUN_IN_BACKGROUND_TEMPLATE, true), "appops");
        ft.commitAllowingStateLoss();
        return rootView;
    }
}
