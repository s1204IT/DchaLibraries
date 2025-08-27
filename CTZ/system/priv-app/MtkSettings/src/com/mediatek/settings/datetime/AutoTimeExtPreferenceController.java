package com.mediatek.settings.datetime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.datetime.AutoTimePreferenceController;
import com.android.settings.datetime.UpdateTimeAndDateCallback;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class AutoTimeExtPreferenceController extends AutoTimePreferenceController implements DialogInterface.OnClickListener {
    private RestrictedListPreference mAutoTimePref;
    private final GPSPreferenceHost mCallback;

    public interface GPSPreferenceHost extends UpdateTimeAndDateCallback {
        void showGPSConfirmDialog();
    }

    public AutoTimeExtPreferenceController(Context context, GPSPreferenceHost gPSPreferenceHost) {
        super(context, gPSPreferenceHost);
        this.mCallback = gPSPreferenceHost;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mAutoTimePref = (RestrictedListPreference) preferenceScreen.findPreference("auto_time");
    }

    @Override // com.android.settings.datetime.AutoTimePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        int i;
        if (!(preference instanceof RestrictedListPreference)) {
            return;
        }
        RestrictedListPreference restrictedListPreference = (RestrictedListPreference) preference;
        if (!restrictedListPreference.isDisabledByAdmin()) {
            restrictedListPreference.setDisabledByAdmin(getEnforcedAdminProperty());
        }
        if (isAutoTimeNetworkEnabled()) {
            i = 0;
        } else if (isAutoTimeGPSEnabled()) {
            i = 1;
        } else {
            i = 2;
        }
        updateSummaryAndValue(i);
    }

    @Override // com.android.settings.datetime.AutoTimePreferenceController, android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) throws NumberFormatException {
        int i = Integer.parseInt((String) obj);
        switch (i) {
            case 0:
                setAutoState("auto_time", 1);
                setAutoState("auto_time_gps", 0);
                break;
            case 1:
                if (!isAutoTimeGPSEnabled()) {
                    this.mCallback.showGPSConfirmDialog();
                    break;
                }
                break;
            case 2:
                setAutoState("auto_time", 0);
                setAutoState("auto_time_gps", 0);
                break;
        }
        updateSummaryAndValue(i);
        this.mCallback.updateTimeAndDateDisplay(this.mContext);
        return true;
    }

    public AlertDialog buildGPSConfirmDialog(Activity activity) {
        int i;
        if (isGpsEnabled()) {
            i = R.string.gps_time_sync_attention_gps_on;
        } else {
            i = R.string.gps_time_sync_attention_gps_off;
        }
        return new AlertDialog.Builder(activity).setMessage(activity.getResources().getString(i)).setTitle(R.string.proxy_error).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).create();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            Log.d("AutoTimeExtPreferenceContr", "Enable GPS time sync");
            if (!isGpsEnabled()) {
                enableGPS();
            }
            setAutoState("auto_time", 0);
            setAutoState("auto_time_gps", 1);
            this.mAutoTimePref.setValueIndex(1);
            this.mAutoTimePref.setSummary(this.mAutoTimePref.getEntries()[1]);
            this.mCallback.updateTimeAndDateDisplay(this.mContext);
            return;
        }
        if (i == -2) {
            Log.d("AutoTimeExtPreferenceContr", "DialogInterface.BUTTON_NEGATIVE");
            reSetAutoTimePref();
        }
    }

    public void reSetAutoTimePref() {
        int i;
        boolean zIsAutoTimeNetworkEnabled = isAutoTimeNetworkEnabled();
        boolean zIsAutoTimeGPSEnabled = isAutoTimeGPSEnabled();
        if (zIsAutoTimeNetworkEnabled) {
            i = 0;
        } else if (zIsAutoTimeGPSEnabled) {
            i = 1;
        } else {
            i = 2;
        }
        this.mAutoTimePref.setValueIndex(i);
        this.mAutoTimePref.setSummary(this.mAutoTimePref.getEntries()[i]);
        Log.d("AutoTimeExtPreferenceContr", "reset AutoTimePref as cancel the selection, index = " + i);
        this.mCallback.updateTimeAndDateDisplay(this.mContext);
    }

    @Override // com.android.settings.datetime.AutoTimePreferenceController
    public boolean isEnabled() {
        boolean zIsAutoTimeNetworkEnabled = isAutoTimeNetworkEnabled() | isAutoTimeGPSEnabled();
        Log.d("AutoTimeExtPreferenceContr", "network : " + isAutoTimeNetworkEnabled() + ", GPS :" + isAutoTimeGPSEnabled());
        return zIsAutoTimeNetworkEnabled;
    }

    private RestrictedLockUtils.EnforcedAdmin getEnforcedAdminProperty() {
        return RestrictedLockUtils.checkIfAutoTimeRequired(this.mContext);
    }

    private boolean isGpsEnabled() {
        return Settings.Secure.isLocationProviderEnabled(this.mContext.getContentResolver(), "gps");
    }

    private void enableGPS() {
        Log.d("AutoTimeExtPreferenceContr", "enable GPS");
        Settings.Secure.setLocationProviderEnabled(this.mContext.getContentResolver(), "gps", true);
    }

    private boolean isAutoTimeNetworkEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time", 0) > 0;
    }

    private boolean isAutoTimeGPSEnabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time_gps", 0) > 0;
    }

    private void setAutoState(String str, int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), str, i);
    }

    private void updateSummaryAndValue(int i) {
        this.mAutoTimePref.setValueIndex(i);
        this.mAutoTimePref.setSummary(this.mAutoTimePref.getEntries()[i]);
    }
}
