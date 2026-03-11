package com.android.settings.fuelgauge;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.os.BatterySipper;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.HashMap;

public class BatteryEntry {

    private static final int[] f7comandroidinternalosBatterySipper$DrainTypeSwitchesValues = null;
    private static NameAndIconLoader mRequestThread;
    static Handler sHandler;
    public final Context context;
    public String defaultPackageName;
    public Drawable icon;
    public int iconId;
    public String name;
    public final BatterySipper sipper;
    static final HashMap<String, UidToDetail> sUidCache = new HashMap<>();
    static final ArrayList<BatteryEntry> mRequestQueue = new ArrayList<>();

    private static int[] m927xe0235176() {
        if (f7comandroidinternalosBatterySipper$DrainTypeSwitchesValues != null) {
            return f7comandroidinternalosBatterySipper$DrainTypeSwitchesValues;
        }
        int[] iArr = new int[BatterySipper.DrainType.values().length];
        try {
            iArr[BatterySipper.DrainType.APP.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[BatterySipper.DrainType.BLUETOOTH.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[BatterySipper.DrainType.CAMERA.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[BatterySipper.DrainType.CELL.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[BatterySipper.DrainType.FLASHLIGHT.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[BatterySipper.DrainType.IDLE.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[BatterySipper.DrainType.OVERCOUNTED.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[BatterySipper.DrainType.PHONE.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[BatterySipper.DrainType.SCREEN.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[BatterySipper.DrainType.UNACCOUNTED.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[BatterySipper.DrainType.USER.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[BatterySipper.DrainType.WIFI.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        f7comandroidinternalosBatterySipper$DrainTypeSwitchesValues = iArr;
        return iArr;
    }

    private static class NameAndIconLoader extends Thread {
        private boolean mAbort;

        public NameAndIconLoader() {
            super("BatteryUsage Icon Loader");
            this.mAbort = false;
        }

        public void abort() {
            this.mAbort = true;
        }

        @Override
        public void run() {
            BatteryEntry be;
            while (true) {
                synchronized (BatteryEntry.mRequestQueue) {
                    if (BatteryEntry.mRequestQueue.isEmpty() || this.mAbort) {
                        break;
                    } else {
                        be = BatteryEntry.mRequestQueue.remove(0);
                    }
                }
                be.loadNameAndIcon();
            }
            if (BatteryEntry.sHandler != null) {
                BatteryEntry.sHandler.sendEmptyMessage(2);
            }
            BatteryEntry.mRequestQueue.clear();
        }
    }

    public static void startRequestQueue() {
        if (sHandler == null) {
            return;
        }
        synchronized (mRequestQueue) {
            if (!mRequestQueue.isEmpty()) {
                if (mRequestThread != null) {
                    mRequestThread.abort();
                }
                mRequestThread = new NameAndIconLoader();
                mRequestThread.setPriority(1);
                mRequestThread.start();
                mRequestQueue.notify();
            }
        }
    }

    public static void stopRequestQueue() {
        synchronized (mRequestQueue) {
            if (mRequestThread != null) {
                mRequestThread.abort();
                mRequestThread = null;
                sHandler = null;
            }
        }
    }

    public static void clearUidCache() {
        sUidCache.clear();
    }

    static class UidToDetail {
        Drawable icon;
        String name;
        String packageName;

        UidToDetail() {
        }
    }

    public BatteryEntry(Context context, Handler handler, UserManager um, BatterySipper sipper) {
        sHandler = handler;
        this.context = context;
        this.sipper = sipper;
        switch (m927xe0235176()[sipper.drainType.ordinal()]) {
            case DefaultWfcSettingsExt.PAUSE:
                this.name = sipper.packageWithHighestDrain;
                break;
            case DefaultWfcSettingsExt.CREATE:
                this.name = context.getResources().getString(R.string.power_bluetooth);
                this.iconId = R.drawable.ic_settings_bluetooth;
                break;
            case DefaultWfcSettingsExt.DESTROY:
                this.name = context.getResources().getString(R.string.power_camera);
                this.iconId = R.drawable.ic_settings_camera;
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                this.name = context.getResources().getString(R.string.power_cell);
                this.iconId = R.drawable.ic_settings_cell_standby;
                break;
            case 5:
                this.name = context.getResources().getString(R.string.power_flashlight);
                this.iconId = R.drawable.ic_settings_display;
                break;
            case 6:
                this.name = context.getResources().getString(R.string.power_idle);
                this.iconId = R.drawable.ic_settings_phone_idle;
                break;
            case 7:
                this.name = context.getResources().getString(R.string.power_overcounted);
                this.iconId = R.drawable.ic_power_system;
                break;
            case 8:
                this.name = context.getResources().getString(R.string.power_phone);
                this.iconId = R.drawable.ic_settings_voice_calls;
                break;
            case 9:
                this.name = context.getResources().getString(R.string.power_screen);
                this.iconId = R.drawable.ic_settings_display;
                break;
            case 10:
                this.name = context.getResources().getString(R.string.power_unaccounted);
                this.iconId = R.drawable.ic_power_system;
                break;
            case 11:
                UserInfo info = um.getUserInfo(sipper.userId);
                if (info != null) {
                    this.icon = Utils.getUserIcon(context, um, info);
                    this.name = Utils.getUserLabel(context, info);
                } else {
                    this.icon = null;
                    this.name = context.getResources().getString(R.string.running_process_item_removed_user_label);
                }
                break;
            case 12:
                this.name = context.getResources().getString(R.string.power_wifi);
                this.iconId = R.drawable.ic_settings_wireless;
                break;
        }
        if (this.iconId > 0) {
            this.icon = context.getDrawable(this.iconId);
        }
        if ((this.name != null && this.iconId != 0) || this.sipper.uidObj == null) {
            return;
        }
        getQuickNameIconForUid(this.sipper.uidObj.getUid());
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public String getLabel() {
        return this.name;
    }

    void getQuickNameIconForUid(int uid) {
        String uidString = Integer.toString(uid);
        if (sUidCache.containsKey(uidString)) {
            UidToDetail utd = sUidCache.get(uidString);
            this.defaultPackageName = utd.packageName;
            this.name = utd.name;
            this.icon = utd.icon;
            return;
        }
        PackageManager pm = this.context.getPackageManager();
        this.icon = pm.getDefaultActivityIcon();
        if (pm.getPackagesForUid(uid) == null) {
            if (uid == 0) {
                this.name = this.context.getResources().getString(R.string.process_kernel_label);
            } else if ("mediaserver".equals(this.name)) {
                this.name = this.context.getResources().getString(R.string.process_mediaserver_label);
            } else if ("dex2oat".equals(this.name)) {
                this.name = this.context.getResources().getString(R.string.process_dex2oat_label);
            }
            this.iconId = R.drawable.ic_power_system;
            this.icon = this.context.getDrawable(this.iconId);
        }
        if (sHandler == null) {
            return;
        }
        synchronized (mRequestQueue) {
            mRequestQueue.add(this);
        }
    }

    public void loadNameAndIcon() {
        CharSequence nm;
        if (this.sipper.uidObj == null) {
            return;
        }
        PackageManager pm = this.context.getPackageManager();
        int uid = this.sipper.uidObj.getUid();
        this.sipper.mPackages = pm.getPackagesForUid(uid);
        if (this.sipper.mPackages != null) {
            String[] packageLabels = new String[this.sipper.mPackages.length];
            System.arraycopy(this.sipper.mPackages, 0, packageLabels, 0, this.sipper.mPackages.length);
            IPackageManager ipm = AppGlobals.getPackageManager();
            int userId = UserHandle.getUserId(uid);
            for (int i = 0; i < packageLabels.length; i++) {
                try {
                    ApplicationInfo ai = ipm.getApplicationInfo(packageLabels[i], 0, userId);
                    if (ai == null) {
                        Log.d("PowerUsageSummary", "Retrieving null app info for package " + packageLabels[i] + ", user " + userId);
                    } else {
                        CharSequence label = ai.loadLabel(pm);
                        if (label != null) {
                            packageLabels[i] = label.toString();
                        }
                        if (ai.icon != 0) {
                            this.defaultPackageName = this.sipper.mPackages[i];
                            this.icon = ai.loadIcon(pm);
                            break;
                        }
                        continue;
                    }
                } catch (RemoteException e) {
                    Log.d("PowerUsageSummary", "Error while retrieving app info for package " + packageLabels[i] + ", user " + userId, e);
                }
            }
            if (packageLabels.length == 1) {
                this.name = packageLabels[0];
            } else {
                for (String pkgName : this.sipper.mPackages) {
                    try {
                        PackageInfo pi = ipm.getPackageInfo(pkgName, 0, userId);
                        if (pi == null) {
                            Log.d("PowerUsageSummary", "Retrieving null package info for package " + pkgName + ", user " + userId);
                        } else if (pi.sharedUserLabel != 0 && (nm = pm.getText(pkgName, pi.sharedUserLabel, pi.applicationInfo)) != null) {
                            this.name = nm.toString();
                            if (pi.applicationInfo.icon == 0) {
                                break;
                            }
                            this.defaultPackageName = pkgName;
                            this.icon = pi.applicationInfo.loadIcon(pm);
                            break;
                        }
                    } catch (RemoteException e2) {
                        Log.d("PowerUsageSummary", "Error while retrieving package info for package " + pkgName + ", user " + userId, e2);
                    }
                }
            }
        }
        String uidString = Integer.toString(uid);
        if (this.name == null) {
            this.name = uidString;
        }
        if (this.icon == null) {
            this.icon = pm.getDefaultActivityIcon();
        }
        UidToDetail utd = new UidToDetail();
        utd.name = this.name;
        utd.icon = this.icon;
        utd.packageName = this.defaultPackageName;
        sUidCache.put(uidString, utd);
        if (sHandler == null) {
            return;
        }
        sHandler.sendMessage(sHandler.obtainMessage(1, this));
    }
}
