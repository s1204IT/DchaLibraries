package com.android.settings.applications;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import com.android.settings.widget.AppPreference;
/* loaded from: classes.dex */
public class ProcessStatsPreference extends AppPreference {
    private ProcStatsPackageEntry mEntry;

    public ProcessStatsPreference(Context context) {
        super(context, null);
    }

    public void init(ProcStatsPackageEntry procStatsPackageEntry, PackageManager packageManager, double d, double d2, double d3, boolean z) {
        double d4;
        this.mEntry = procStatsPackageEntry;
        String str = TextUtils.isEmpty(procStatsPackageEntry.mUiLabel) ? procStatsPackageEntry.mPackage : procStatsPackageEntry.mUiLabel;
        setTitle(str);
        if (TextUtils.isEmpty(str)) {
            Log.d("ProcessStatsPreference", "PackageEntry contained no package name or uiLabel");
        }
        if (procStatsPackageEntry.mUiTargetApp != null) {
            setIcon(procStatsPackageEntry.mUiTargetApp.loadIcon(packageManager));
        } else {
            setIcon(packageManager.getDefaultActivityIcon());
        }
        boolean z2 = procStatsPackageEntry.mRunWeight > procStatsPackageEntry.mBgWeight;
        if (z) {
            d4 = (z2 ? procStatsPackageEntry.mRunWeight : procStatsPackageEntry.mBgWeight) * d2;
        } else {
            d4 = (z2 ? procStatsPackageEntry.mMaxRunMem : procStatsPackageEntry.mMaxBgMem) * d3 * 1024.0d;
        }
        setSummary(Formatter.formatShortFileSize(getContext(), (long) d4));
        setProgress((int) ((100.0d * d4) / d));
    }

    public ProcStatsPackageEntry getEntry() {
        return this.mEntry;
    }
}
