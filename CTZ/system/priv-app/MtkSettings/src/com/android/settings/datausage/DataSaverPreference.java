package com.android.settings.datausage;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import com.android.settings.R;
import com.android.settings.datausage.DataSaverBackend;

/* loaded from: classes.dex */
public class DataSaverPreference extends Preference implements DataSaverBackend.Listener {
    private final DataSaverBackend mDataSaverBackend;

    public DataSaverPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDataSaverBackend = new DataSaverBackend(context);
    }

    @Override // android.support.v7.preference.Preference
    public void onAttached() {
        super.onAttached();
        this.mDataSaverBackend.addListener(this);
    }

    @Override // android.support.v7.preference.Preference
    public void onDetached() {
        super.onDetached();
        this.mDataSaverBackend.remListener(this);
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onDataSaverChanged(boolean z) {
        setSummary(z ? R.string.data_saver_on : R.string.data_saver_off);
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onWhitelistStatusChanged(int i, boolean z) {
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onBlacklistStatusChanged(int i, boolean z) {
    }
}
