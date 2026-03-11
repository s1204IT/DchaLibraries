package com.android.settings.applications;

import android.view.View;
import android.widget.AdapterView;
import com.android.settings.applications.ManageApplications;

interface AppClickListener {
    void onItemClick(ManageApplications.TabInfo tabInfo, AdapterView<?> adapterView, View view, int i, long j);
}
