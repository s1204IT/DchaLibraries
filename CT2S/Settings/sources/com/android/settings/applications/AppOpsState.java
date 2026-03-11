package com.android.settings.applications;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.settings.R;
import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class AppOpsState {
    final AppOpsManager mAppOps;
    final Context mContext;
    final CharSequence[] mOpLabels;
    final CharSequence[] mOpSummaries;
    final PackageManager mPm;
    public static final OpsTemplate LOCATION_TEMPLATE = new OpsTemplate(new int[]{0, 1, 2, 10, 12, 41, 42}, new boolean[]{true, true, false, false, false, false, false});
    public static final OpsTemplate PERSONAL_TEMPLATE = new OpsTemplate(new int[]{4, 5, 6, 7, 8, 9, 29, 30}, new boolean[]{true, true, true, true, true, true, false, false});
    public static final OpsTemplate MESSAGING_TEMPLATE = new OpsTemplate(new int[]{14, 16, 17, 18, 19, 15, 20, 21, 22}, new boolean[]{true, true, true, true, true, true, true, true, true});
    public static final OpsTemplate MEDIA_TEMPLATE = new OpsTemplate(new int[]{3, 26, 27, 28, 31, 32, 33, 34, 35, 36, 37, 38, 39, 44}, new boolean[]{false, true, true, false, false, false, false, false, false, false, false, false, false});
    public static final OpsTemplate DEVICE_TEMPLATE = new OpsTemplate(new int[]{11, 25, 13, 23, 24, 40, 46, 47}, new boolean[]{false, true, true, true, true, true, false, false});
    public static final OpsTemplate[] ALL_TEMPLATES = {LOCATION_TEMPLATE, PERSONAL_TEMPLATE, MESSAGING_TEMPLATE, MEDIA_TEMPLATE, DEVICE_TEMPLATE};
    public static final Comparator<AppOpEntry> APP_OP_COMPARATOR = new Comparator<AppOpEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppOpEntry object1, AppOpEntry object2) {
            if (object1.getSwitchOrder() != object2.getSwitchOrder()) {
                return object1.getSwitchOrder() < object2.getSwitchOrder() ? -1 : 1;
            }
            if (object1.isRunning() != object2.isRunning()) {
                return !object1.isRunning() ? 1 : -1;
            }
            if (object1.getTime() != object2.getTime()) {
                return object1.getTime() <= object2.getTime() ? 1 : -1;
            }
            return this.sCollator.compare(object1.getAppEntry().getLabel(), object2.getAppEntry().getLabel());
        }
    };

    public AppOpsState(Context context) {
        this.mContext = context;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mPm = context.getPackageManager();
        this.mOpSummaries = context.getResources().getTextArray(R.array.app_ops_summaries);
        this.mOpLabels = context.getResources().getTextArray(R.array.app_ops_labels);
    }

    public static class OpsTemplate implements Parcelable {
        public static final Parcelable.Creator<OpsTemplate> CREATOR = new Parcelable.Creator<OpsTemplate>() {
            @Override
            public OpsTemplate createFromParcel(Parcel source) {
                return new OpsTemplate(source);
            }

            @Override
            public OpsTemplate[] newArray(int size) {
                return new OpsTemplate[size];
            }
        };
        public final int[] ops;
        public final boolean[] showPerms;

        public OpsTemplate(int[] _ops, boolean[] _showPerms) {
            this.ops = _ops;
            this.showPerms = _showPerms;
        }

        OpsTemplate(Parcel src) {
            this.ops = src.createIntArray();
            this.showPerms = src.createBooleanArray();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(this.ops);
            dest.writeBooleanArray(this.showPerms);
        }
    }

    public static class AppEntry {
        private final File mApkFile;
        private Drawable mIcon;
        private final ApplicationInfo mInfo;
        private String mLabel;
        private boolean mMounted;
        private final AppOpsState mState;
        private final SparseArray<AppOpsManager.OpEntry> mOps = new SparseArray<>();
        private final SparseArray<AppOpEntry> mOpSwitches = new SparseArray<>();

        public AppEntry(AppOpsState state, ApplicationInfo info) {
            this.mState = state;
            this.mInfo = info;
            this.mApkFile = new File(info.sourceDir);
        }

        public void addOp(AppOpEntry entry, AppOpsManager.OpEntry op) {
            this.mOps.put(op.getOp(), op);
            this.mOpSwitches.put(AppOpsManager.opToSwitch(op.getOp()), entry);
        }

        public boolean hasOp(int op) {
            return this.mOps.indexOfKey(op) >= 0;
        }

        public AppOpEntry getOpSwitch(int op) {
            return this.mOpSwitches.get(AppOpsManager.opToSwitch(op));
        }

        public ApplicationInfo getApplicationInfo() {
            return this.mInfo;
        }

        public String getLabel() {
            return this.mLabel;
        }

        public Drawable getIcon() {
            if (this.mIcon == null) {
                if (this.mApkFile.exists()) {
                    this.mIcon = this.mInfo.loadIcon(this.mState.mPm);
                    return this.mIcon;
                }
                this.mMounted = false;
            } else if (!this.mMounted) {
                if (this.mApkFile.exists()) {
                    this.mMounted = true;
                    this.mIcon = this.mInfo.loadIcon(this.mState.mPm);
                    return this.mIcon;
                }
            } else {
                return this.mIcon;
            }
            return this.mState.mContext.getDrawable(android.R.drawable.sym_def_app_icon);
        }

        public String toString() {
            return this.mLabel;
        }

        void loadLabel(Context context) {
            if (this.mLabel == null || !this.mMounted) {
                if (!this.mApkFile.exists()) {
                    this.mMounted = false;
                    this.mLabel = this.mInfo.packageName;
                } else {
                    this.mMounted = true;
                    CharSequence label = this.mInfo.loadLabel(context.getPackageManager());
                    this.mLabel = label != null ? label.toString() : this.mInfo.packageName;
                }
            }
        }
    }

    public static class AppOpEntry {
        private final AppEntry mApp;
        private final AppOpsManager.PackageOps mPkgOps;
        private final int mSwitchOrder;
        private final ArrayList<AppOpsManager.OpEntry> mOps = new ArrayList<>();
        private final ArrayList<AppOpsManager.OpEntry> mSwitchOps = new ArrayList<>();

        public AppOpEntry(AppOpsManager.PackageOps pkg, AppOpsManager.OpEntry op, AppEntry app, int switchOrder) {
            this.mPkgOps = pkg;
            this.mApp = app;
            this.mSwitchOrder = switchOrder;
            this.mApp.addOp(this, op);
            this.mOps.add(op);
            this.mSwitchOps.add(op);
        }

        private static void addOp(ArrayList<AppOpsManager.OpEntry> list, AppOpsManager.OpEntry op) {
            for (int i = 0; i < list.size(); i++) {
                AppOpsManager.OpEntry pos = list.get(i);
                if (pos.isRunning() != op.isRunning()) {
                    if (op.isRunning()) {
                        list.add(i, op);
                        return;
                    }
                } else if (pos.getTime() < op.getTime()) {
                    list.add(i, op);
                    return;
                }
            }
            list.add(op);
        }

        public void addOp(AppOpsManager.OpEntry op) {
            this.mApp.addOp(this, op);
            addOp(this.mOps, op);
            if (this.mApp.getOpSwitch(AppOpsManager.opToSwitch(op.getOp())) == null) {
                addOp(this.mSwitchOps, op);
            }
        }

        public AppEntry getAppEntry() {
            return this.mApp;
        }

        public int getSwitchOrder() {
            return this.mSwitchOrder;
        }

        public AppOpsManager.PackageOps getPackageOps() {
            return this.mPkgOps;
        }

        public AppOpsManager.OpEntry getOpEntry(int pos) {
            return this.mOps.get(pos);
        }

        private CharSequence getCombinedText(ArrayList<AppOpsManager.OpEntry> ops, CharSequence[] items) {
            if (ops.size() == 1) {
                return items[ops.get(0).getOp()];
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < ops.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(items[ops.get(i).getOp()]);
            }
            return builder.toString();
        }

        public CharSequence getSummaryText(AppOpsState state) {
            return getCombinedText(this.mOps, state.mOpSummaries);
        }

        public CharSequence getSwitchText(AppOpsState state) {
            return this.mSwitchOps.size() > 0 ? getCombinedText(this.mSwitchOps, state.mOpLabels) : getCombinedText(this.mOps, state.mOpLabels);
        }

        public CharSequence getTimeText(Resources res, boolean showEmptyText) {
            if (isRunning()) {
                return res.getText(R.string.app_ops_running);
            }
            if (getTime() > 0) {
                return DateUtils.getRelativeTimeSpanString(getTime(), System.currentTimeMillis(), 60000L, 262144);
            }
            return showEmptyText ? res.getText(R.string.app_ops_never_used) : "";
        }

        public boolean isRunning() {
            return this.mOps.get(0).isRunning();
        }

        public long getTime() {
            return this.mOps.get(0).getTime();
        }

        public String toString() {
            return this.mApp.getLabel();
        }
    }

    private void addOp(List<AppOpEntry> entries, AppOpsManager.PackageOps pkgOps, AppEntry appEntry, AppOpsManager.OpEntry opEntry, boolean allowMerge, int switchOrder) {
        if (allowMerge && entries.size() > 0) {
            AppOpEntry last = entries.get(entries.size() - 1);
            if (last.getAppEntry() == appEntry) {
                boolean lastExe = last.getTime() != 0;
                boolean entryExe = opEntry.getTime() != 0;
                if (lastExe == entryExe) {
                    last.addOp(opEntry);
                    return;
                }
            }
        }
        AppOpEntry entry = appEntry.getOpSwitch(opEntry.getOp());
        if (entry != null) {
            entry.addOp(opEntry);
        } else {
            entries.add(new AppOpEntry(pkgOps, opEntry, appEntry, switchOrder));
        }
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl) {
        return buildState(tpl, 0, null);
    }

    private AppEntry getAppEntry(Context context, HashMap<String, AppEntry> appEntries, String packageName, ApplicationInfo appInfo) {
        AppEntry appEntry = appEntries.get(packageName);
        if (appEntry == null) {
            if (appInfo == null) {
                try {
                    appInfo = this.mPm.getApplicationInfo(packageName, 8704);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("AppOpsState", "Unable to find info for package " + packageName);
                    return null;
                }
            }
            appEntry = new AppEntry(this, appInfo);
            appEntry.loadLabel(context);
            appEntries.put(packageName, appEntry);
        }
        return appEntry;
    }

    public List<AppOpEntry> buildState(OpsTemplate tpl, int uid, String packageName) {
        List<AppOpsManager.PackageOps> pkgs;
        List<PackageInfo> apps;
        String perm;
        Context context = this.mContext;
        HashMap<String, AppEntry> appEntries = new HashMap<>();
        List<AppOpEntry> entries = new ArrayList<>();
        ArrayList<String> perms = new ArrayList<>();
        ArrayList<Integer> permOps = new ArrayList<>();
        int[] opToOrder = new int[48];
        for (int i = 0; i < tpl.ops.length; i++) {
            if (tpl.showPerms[i] && (perm = AppOpsManager.opToPermission(tpl.ops[i])) != null && !perms.contains(perm)) {
                perms.add(perm);
                permOps.add(Integer.valueOf(tpl.ops[i]));
                opToOrder[tpl.ops[i]] = i;
            }
        }
        if (packageName != null) {
            pkgs = this.mAppOps.getOpsForPackage(uid, packageName, tpl.ops);
        } else {
            pkgs = this.mAppOps.getPackagesForOps(tpl.ops);
        }
        if (pkgs != null) {
            for (int i2 = 0; i2 < pkgs.size(); i2++) {
                AppOpsManager.PackageOps pkgOps = pkgs.get(i2);
                AppEntry appEntry = getAppEntry(context, appEntries, pkgOps.getPackageName(), null);
                if (appEntry != null) {
                    for (int j = 0; j < pkgOps.getOps().size(); j++) {
                        AppOpsManager.OpEntry opEntry = (AppOpsManager.OpEntry) pkgOps.getOps().get(j);
                        addOp(entries, pkgOps, appEntry, opEntry, packageName == null, packageName == null ? 0 : opToOrder[opEntry.getOp()]);
                    }
                }
            }
        }
        if (packageName != null) {
            apps = new ArrayList<>();
            try {
                PackageInfo pi = this.mPm.getPackageInfo(packageName, 4096);
                apps.add(pi);
            } catch (PackageManager.NameNotFoundException e) {
            }
        } else {
            String[] permsArray = new String[perms.size()];
            perms.toArray(permsArray);
            apps = this.mPm.getPackagesHoldingPermissions(permsArray, 0);
        }
        for (int i3 = 0; i3 < apps.size(); i3++) {
            PackageInfo appInfo = apps.get(i3);
            AppEntry appEntry2 = getAppEntry(context, appEntries, appInfo.packageName, appInfo.applicationInfo);
            if (appEntry2 != null) {
                List<AppOpsManager.OpEntry> dummyOps = null;
                AppOpsManager.PackageOps pkgOps2 = null;
                if (appInfo.requestedPermissions != null) {
                    for (int j2 = 0; j2 < appInfo.requestedPermissions.length; j2++) {
                        if (appInfo.requestedPermissionsFlags == null || (appInfo.requestedPermissionsFlags[j2] & 2) != 0) {
                            for (int k = 0; k < perms.size(); k++) {
                                if (perms.get(k).equals(appInfo.requestedPermissions[j2]) && !appEntry2.hasOp(permOps.get(k).intValue())) {
                                    if (dummyOps == null) {
                                        dummyOps = new ArrayList<>();
                                        pkgOps2 = new AppOpsManager.PackageOps(appInfo.packageName, appInfo.applicationInfo.uid, dummyOps);
                                    }
                                    AppOpsManager.OpEntry opEntry2 = new AppOpsManager.OpEntry(permOps.get(k).intValue(), 0, 0L, 0L, 0);
                                    dummyOps.add(opEntry2);
                                    addOp(entries, pkgOps2, appEntry2, opEntry2, packageName == null, packageName == null ? 0 : opToOrder[opEntry2.getOp()]);
                                }
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(entries, APP_OP_COMPARATOR);
        return entries;
    }
}
