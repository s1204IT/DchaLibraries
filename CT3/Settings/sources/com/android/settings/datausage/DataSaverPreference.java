package com.android.settings.datausage;

import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.datausage.DataSaverBackend;

public class DataSaverPreference extends Preference implements DataSaverBackend.Listener {
    private final DataSaverBackend mDataSaverBackend;

    @Override
    public void onAttached() {
        super.onAttached();
        this.mDataSaverBackend.addListener(this);
    }

    @Override
    public void onDetached() {
        super.onDetached();
        this.mDataSaverBackend.addListener(this);
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        setSummary(isDataSaving ? R.string.data_saver_on : R.string.data_saver_off);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
    }
}
