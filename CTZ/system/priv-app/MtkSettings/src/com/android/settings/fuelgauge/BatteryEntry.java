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
import com.android.settingslib.wifi.AccessPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/* loaded from: classes.dex */
public class BatteryEntry {
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
    static Locale sCurrentLocale = null;

    private static class NameAndIconLoader extends Thread {
        private boolean mAbort;

        public NameAndIconLoader() {
            super("BatteryUsage Icon Loader");
            this.mAbort = false;
        }

        public void abort() {
            this.mAbort = true;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            BatteryEntry batteryEntryRemove;
            while (true) {
                synchronized (BatteryEntry.mRequestQueue) {
                    if (BatteryEntry.mRequestQueue.isEmpty() || this.mAbort) {
                        break;
                    } else {
                        batteryEntryRemove = BatteryEntry.mRequestQueue.remove(0);
                    }
                }
                batteryEntryRemove.loadNameAndIcon();
            }
            if (BatteryEntry.sHandler != null) {
                BatteryEntry.sHandler.sendEmptyMessage(2);
            }
            BatteryEntry.mRequestQueue.clear();
        }
    }

    public static void startRequestQueue() {
        if (sHandler != null) {
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

    public BatteryEntry(Context context, Handler handler, UserManager userManager, BatterySipper batterySipper) {
        sHandler = handler;
        this.context = context;
        this.sipper = batterySipper;
        switch (AnonymousClass1.$SwitchMap$com$android$internal$os$BatterySipper$DrainType[batterySipper.drainType.ordinal()]) {
            case 1:
                this.name = context.getResources().getString(R.string.power_idle);
                this.iconId = R.drawable.ic_settings_phone_idle;
                break;
            case 2:
                this.name = context.getResources().getString(R.string.power_cell);
                this.iconId = R.drawable.ic_settings_cell_standby;
                break;
            case 3:
                this.name = context.getResources().getString(R.string.power_phone);
                this.iconId = R.drawable.ic_settings_voice_calls;
                break;
            case 4:
                this.name = context.getResources().getString(R.string.power_wifi);
                this.iconId = R.drawable.ic_settings_wireless;
                break;
            case 5:
                this.name = context.getResources().getString(R.string.power_bluetooth);
                this.iconId = R.drawable.ic_settings_bluetooth;
                break;
            case 6:
                this.name = context.getResources().getString(R.string.power_screen);
                this.iconId = R.drawable.ic_settings_display;
                break;
            case 7:
                this.name = context.getResources().getString(R.string.power_flashlight);
                this.iconId = R.drawable.ic_settings_display;
                break;
            case 8:
                PackageManager packageManager = context.getPackageManager();
                batterySipper.mPackages = packageManager.getPackagesForUid(batterySipper.uidObj.getUid());
                if (batterySipper.mPackages == null || batterySipper.mPackages.length != 1) {
                    this.name = batterySipper.packageWithHighestDrain;
                    break;
                } else {
                    this.defaultPackageName = packageManager.getPackagesForUid(batterySipper.uidObj.getUid())[0];
                    try {
                        this.name = packageManager.getApplicationLabel(packageManager.getApplicationInfo(this.defaultPackageName, 0)).toString();
                        break;
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.d("BatteryEntry", "PackageManager failed to retrieve ApplicationInfo for: " + this.defaultPackageName);
                        this.name = this.defaultPackageName;
                        break;
                    }
                }
            case 9:
                UserInfo userInfo = userManager.getUserInfo(batterySipper.userId);
                if (userInfo != null) {
                    this.icon = Utils.getUserIcon(context, userManager, userInfo);
                    this.name = Utils.getUserLabel(context, userInfo);
                    break;
                } else {
                    this.icon = null;
                    this.name = context.getResources().getString(R.string.running_process_item_removed_user_label);
                    break;
                }
            case AccessPoint.Speed.MODERATE /* 10 */:
                this.name = context.getResources().getString(R.string.power_unaccounted);
                this.iconId = R.drawable.ic_power_system;
                break;
            case 11:
                this.name = context.getResources().getString(R.string.power_overcounted);
                this.iconId = R.drawable.ic_power_system;
                break;
            case 12:
                this.name = context.getResources().getString(R.string.power_camera);
                this.iconId = R.drawable.ic_settings_camera;
                break;
            case 13:
                this.name = context.getResources().getString(R.string.ambient_display_screen_title);
                this.iconId = R.drawable.ic_settings_aod;
                break;
        }
        if (this.iconId > 0) {
            this.icon = context.getDrawable(this.iconId);
        }
        if ((this.name == null || this.iconId == 0) && this.sipper.uidObj != null) {
            getQuickNameIconForUid(this.sipper.uidObj.getUid());
        }
    }

    /* renamed from: com.android.settings.fuelgauge.BatteryEntry$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$os$BatterySipper$DrainType = new int[BatterySipper.DrainType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.CELL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.PHONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.WIFI.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.BLUETOOTH.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.SCREEN.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.FLASHLIGHT.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.APP.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.USER.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.UNACCOUNTED.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.OVERCOUNTED.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.CAMERA.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[BatterySipper.DrainType.AMBIENT_DISPLAY.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    public Drawable getIcon() {
        return this.icon;
    }

    public String getLabel() {
        return this.name;
    }

    void getQuickNameIconForUid(int i) {
        Locale locale = Locale.getDefault();
        if (sCurrentLocale != locale) {
            clearUidCache();
            sCurrentLocale = locale;
        }
        String string = Integer.toString(i);
        if (sUidCache.containsKey(string)) {
            UidToDetail uidToDetail = sUidCache.get(string);
            this.defaultPackageName = uidToDetail.packageName;
            this.name = uidToDetail.name;
            this.icon = uidToDetail.icon;
            return;
        }
        PackageManager packageManager = this.context.getPackageManager();
        this.icon = packageManager.getDefaultActivityIcon();
        if (packageManager.getPackagesForUid(i) == null) {
            if (i == 0) {
                this.name = this.context.getResources().getString(R.string.process_kernel_label);
            } else if ("mediaserver".equals(this.name)) {
                this.name = this.context.getResources().getString(R.string.process_mediaserver_label);
            } else if ("dex2oat".equals(this.name)) {
                this.name = this.context.getResources().getString(R.string.process_dex2oat_label);
            }
            this.iconId = R.drawable.ic_power_system;
            this.icon = this.context.getDrawable(this.iconId);
        }
        if (sHandler != null) {
            synchronized (mRequestQueue) {
                mRequestQueue.add(this);
            }
        }
    }

    public void loadNameAndIcon() {
        CharSequence text;
        if (this.sipper.uidObj == null) {
            return;
        }
        PackageManager packageManager = this.context.getPackageManager();
        int uid = this.sipper.uidObj.getUid();
        if (this.sipper.mPackages == null) {
            this.sipper.mPackages = packageManager.getPackagesForUid(uid);
        }
        String[] strArrExtractPackagesFromSipper = extractPackagesFromSipper(this.sipper);
        if (strArrExtractPackagesFromSipper != null) {
            String[] strArr = new String[strArrExtractPackagesFromSipper.length];
            System.arraycopy(strArrExtractPackagesFromSipper, 0, strArr, 0, strArrExtractPackagesFromSipper.length);
            IPackageManager packageManager2 = AppGlobals.getPackageManager();
            int userId = UserHandle.getUserId(uid);
            for (int i = 0; i < strArr.length; i++) {
                try {
                    ApplicationInfo applicationInfo = packageManager2.getApplicationInfo(strArr[i], 0, userId);
                    if (applicationInfo == null) {
                        Log.d("BatteryEntry", "Retrieving null app info for package " + strArr[i] + ", user " + userId);
                    } else {
                        CharSequence charSequenceLoadLabel = applicationInfo.loadLabel(packageManager);
                        if (charSequenceLoadLabel != null) {
                            strArr[i] = charSequenceLoadLabel.toString();
                        }
                        if (applicationInfo.icon != 0) {
                            this.defaultPackageName = strArrExtractPackagesFromSipper[i];
                            this.icon = applicationInfo.loadIcon(packageManager);
                            break;
                        }
                        continue;
                    }
                } catch (RemoteException e) {
                    Log.d("BatteryEntry", "Error while retrieving app info for package " + strArr[i] + ", user " + userId, e);
                }
            }
            if (strArr.length == 1) {
                this.name = strArr[0];
            } else {
                for (String str : strArrExtractPackagesFromSipper) {
                    try {
                        PackageInfo packageInfo = packageManager2.getPackageInfo(str, 0, userId);
                        if (packageInfo == null) {
                            Log.d("BatteryEntry", "Retrieving null package info for package " + str + ", user " + userId);
                        } else if (packageInfo.sharedUserLabel != 0 && (text = packageManager.getText(str, packageInfo.sharedUserLabel, packageInfo.applicationInfo)) != null) {
                            this.name = text.toString();
                            if (packageInfo.applicationInfo.icon == 0) {
                                break;
                            }
                            this.defaultPackageName = str;
                            this.icon = packageInfo.applicationInfo.loadIcon(packageManager);
                            break;
                        }
                    } catch (RemoteException e2) {
                        Log.d("BatteryEntry", "Error while retrieving package info for package " + str + ", user " + userId, e2);
                    }
                }
            }
        }
        String string = Integer.toString(uid);
        if (this.name == null) {
            this.name = string;
        }
        if (this.icon == null) {
            this.icon = packageManager.getDefaultActivityIcon();
        }
        UidToDetail uidToDetail = new UidToDetail();
        uidToDetail.name = this.name;
        uidToDetail.icon = this.icon;
        uidToDetail.packageName = this.defaultPackageName;
        sUidCache.put(string, uidToDetail);
        if (sHandler != null) {
            sHandler.sendMessage(sHandler.obtainMessage(1, this));
        }
    }

    String[] extractPackagesFromSipper(BatterySipper batterySipper) {
        if (batterySipper.getUid() == 1000) {
            return new String[]{"android"};
        }
        return batterySipper.mPackages;
    }
}
