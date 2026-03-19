package com.android.internal.app;

import android.app.ActionBar;
import android.view.View;
import android.widget.AdapterView;

class NavItemSelectedListener implements AdapterView.OnItemSelectedListener {
    private final ActionBar.OnNavigationListener mListener;

    public NavItemSelectedListener(ActionBar.OnNavigationListener onNavigationListener) {
        this.mListener = onNavigationListener;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (this.mListener != null) {
            this.mListener.onNavigationItemSelected(i, j);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }
}
