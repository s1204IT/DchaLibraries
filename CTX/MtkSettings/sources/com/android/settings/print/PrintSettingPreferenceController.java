package com.android.settings.print;

import android.content.Context;
import android.content.pm.PackageManager;
import android.print.PrintJob;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import java.util.Iterator;
import java.util.List;

public class PrintSettingPreferenceController extends BasePreferenceController implements PrintManager.PrintJobStateChangeListener, LifecycleObserver, OnStart, OnStop {
    private static final String KEY_PRINTING_SETTINGS = "connected_device_printing";
    private final PackageManager mPackageManager;
    private Preference mPreference;
    private final PrintManager mPrintManager;

    public PrintSettingPreferenceController(Context context) {
        super(context, KEY_PRINTING_SETTINGS);
        this.mPackageManager = context.getPackageManager();
        this.mPrintManager = ((PrintManager) context.getSystemService("print")).getGlobalPrintManagerForUser(context.getUserId());
    }

    @Override
    public int getAvailabilityStatus() {
        return this.mPackageManager.hasSystemFeature("android.software.print") ? 0 : 2;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        this.mPrintManager.addPrintJobStateChangeListener(this);
    }

    @Override
    public void onStop() {
        this.mPrintManager.removePrintJobStateChangeListener(this);
    }

    public void onPrintJobStateChanged(PrintJobId printJobId) {
        updateState(this.mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        ((RestrictedPreference) preference).checkRestrictionAndSetDisabled("no_printing");
    }

    @Override
    public CharSequence getSummary() {
        int i;
        List<PrintJob> printJobs = this.mPrintManager.getPrintJobs();
        if (printJobs != null) {
            Iterator<PrintJob> it = printJobs.iterator();
            i = 0;
            while (it.hasNext()) {
                if (shouldShowToUser(it.next().getInfo())) {
                    i++;
                }
            }
        } else {
            i = 0;
        }
        if (i > 0) {
            return this.mContext.getResources().getQuantityString(R.plurals.print_jobs_summary, i, Integer.valueOf(i));
        }
        List printServices = this.mPrintManager.getPrintServices(1);
        if (printServices == null || printServices.isEmpty()) {
            return this.mContext.getText(R.string.print_settings_summary_no_service);
        }
        int size = printServices.size();
        return this.mContext.getResources().getQuantityString(R.plurals.print_settings_summary, size, Integer.valueOf(size));
    }

    static boolean shouldShowToUser(PrintJobInfo printJobInfo) {
        int state = printJobInfo.getState();
        if (state != 6) {
            switch (state) {
                case 2:
                case 3:
                case 4:
                    return true;
                default:
                    return false;
            }
        }
        return true;
    }
}
