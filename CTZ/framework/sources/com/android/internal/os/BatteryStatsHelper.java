package com.android.internal.os;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseLongArray;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatterySipper;
import com.android.internal.util.ArrayUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BatteryStatsHelper {
    static final boolean DEBUG = false;
    private static Intent sBatteryBroadcastXfer;
    private static BatteryStats sStatsXfer;
    private Intent mBatteryBroadcast;
    private IBatteryStats mBatteryInfo;
    long mBatteryRealtimeUs;
    long mBatteryTimeRemainingUs;
    long mBatteryUptimeUs;
    PowerCalculator mBluetoothPowerCalculator;
    private final List<BatterySipper> mBluetoothSippers;
    PowerCalculator mCameraPowerCalculator;
    long mChargeTimeRemainingUs;
    private final boolean mCollectBatteryBroadcast;
    private double mComputedPower;
    private final Context mContext;
    PowerCalculator mCpuPowerCalculator;
    PowerCalculator mFlashlightPowerCalculator;
    boolean mHasBluetoothPowerReporting;
    boolean mHasWifiPowerReporting;
    private double mMaxDrainedPower;
    private double mMaxPower;
    private double mMaxRealPower;
    PowerCalculator mMediaPowerCalculator;
    PowerCalculator mMemoryPowerCalculator;
    private double mMinDrainedPower;
    MobileRadioPowerCalculator mMobileRadioPowerCalculator;
    private final List<BatterySipper> mMobilemsppList;
    private PackageManager mPackageManager;
    private PowerProfile mPowerProfile;
    long mRawRealtimeUs;
    long mRawUptimeUs;
    PowerCalculator mSensorPowerCalculator;
    private String[] mServicepackageArray;
    private BatteryStats mStats;
    private long mStatsPeriod;
    private int mStatsType;
    private String[] mSystemPackageArray;
    private double mTotalPower;
    long mTypeBatteryRealtimeUs;
    long mTypeBatteryUptimeUs;
    private final List<BatterySipper> mUsageList;
    private final SparseArray<List<BatterySipper>> mUserSippers;
    PowerCalculator mWakelockPowerCalculator;
    private final boolean mWifiOnly;
    PowerCalculator mWifiPowerCalculator;
    private final List<BatterySipper> mWifiSippers;
    private static final String TAG = BatteryStatsHelper.class.getSimpleName();
    private static ArrayMap<File, BatteryStats> sFileXfer = new ArrayMap<>();

    public static boolean checkWifiOnly(Context context) {
        if (((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)) == null) {
            return false;
        }
        return !r1.isNetworkSupported(0);
    }

    public static boolean checkHasWifiPowerReporting(BatteryStats batteryStats, PowerProfile powerProfile) {
        return (!batteryStats.hasWifiActivityReporting() || powerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_IDLE) == 0.0d || powerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_RX) == 0.0d || powerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_TX) == 0.0d) ? false : true;
    }

    public static boolean checkHasBluetoothPowerReporting(BatteryStats batteryStats, PowerProfile powerProfile) {
        return (!batteryStats.hasBluetoothActivityReporting() || powerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_IDLE) == 0.0d || powerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_RX) == 0.0d || powerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_TX) == 0.0d) ? false : true;
    }

    public BatteryStatsHelper(Context context) {
        this(context, true);
    }

    public BatteryStatsHelper(Context context, boolean z) {
        this(context, z, checkWifiOnly(context));
    }

    public BatteryStatsHelper(Context context, boolean z, boolean z2) {
        this.mUsageList = new ArrayList();
        this.mWifiSippers = new ArrayList();
        this.mBluetoothSippers = new ArrayList();
        this.mUserSippers = new SparseArray<>();
        this.mMobilemsppList = new ArrayList();
        this.mStatsType = 0;
        this.mStatsPeriod = 0L;
        this.mMaxPower = 1.0d;
        this.mMaxRealPower = 1.0d;
        this.mHasWifiPowerReporting = false;
        this.mHasBluetoothPowerReporting = false;
        this.mContext = context;
        this.mCollectBatteryBroadcast = z;
        this.mWifiOnly = z2;
        this.mPackageManager = context.getPackageManager();
        Resources resources = context.getResources();
        this.mSystemPackageArray = resources.getStringArray(R.array.config_batteryPackageTypeSystem);
        this.mServicepackageArray = resources.getStringArray(R.array.config_batteryPackageTypeService);
    }

    public void storeStatsHistoryInFile(String str) {
        FileOutputStream fileOutputStream;
        synchronized (sFileXfer) {
            File fileMakeFilePath = makeFilePath(this.mContext, str);
            sFileXfer.put(fileMakeFilePath, getStats());
            FileOutputStream fileOutputStream2 = null;
            try {
                try {
                    try {
                        fileOutputStream = new FileOutputStream(fileMakeFilePath);
                    } catch (IOException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e2) {
            }
            try {
                Parcel parcelObtain = Parcel.obtain();
                getStats().writeToParcelWithoutUids(parcelObtain, 0);
                fileOutputStream.write(parcelObtain.marshall());
                fileOutputStream.close();
            } catch (IOException e3) {
                e = e3;
                fileOutputStream2 = fileOutputStream;
                Log.w(TAG, "Unable to write history to file", e);
                if (fileOutputStream2 != null) {
                    fileOutputStream2.close();
                }
            } catch (Throwable th2) {
                th = th2;
                fileOutputStream2 = fileOutputStream;
                if (fileOutputStream2 != null) {
                    try {
                        fileOutputStream2.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        }
    }

    public static BatteryStats statsFromFile(Context context, String str) {
        FileInputStream fileInputStream;
        synchronized (sFileXfer) {
            File fileMakeFilePath = makeFilePath(context, str);
            BatteryStats batteryStats = sFileXfer.get(fileMakeFilePath);
            if (batteryStats != null) {
                return batteryStats;
            }
            FileInputStream fileInputStream2 = null;
            try {
                try {
                    fileInputStream = new FileInputStream(fileMakeFilePath);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
                fileInputStream = fileInputStream2;
            }
            try {
                byte[] fully = readFully(fileInputStream);
                Parcel parcelObtain = Parcel.obtain();
                parcelObtain.unmarshall(fully, 0, fully.length);
                parcelObtain.setDataPosition(0);
                BatteryStatsImpl batteryStatsImplCreateFromParcel = BatteryStatsImpl.CREATOR.createFromParcel(parcelObtain);
                try {
                    fileInputStream.close();
                } catch (IOException e2) {
                }
                return batteryStatsImplCreateFromParcel;
            } catch (IOException e3) {
                e = e3;
                fileInputStream2 = fileInputStream;
                Log.w(TAG, "Unable to read history to file", e);
                if (fileInputStream2 != null) {
                    try {
                        fileInputStream2.close();
                    } catch (IOException e4) {
                    }
                }
                return getStats(IBatteryStats.Stub.asInterface(ServiceManager.getService(BatteryStats.SERVICE_NAME)));
            } catch (Throwable th2) {
                th = th2;
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        }
    }

    public static void dropFile(Context context, String str) {
        makeFilePath(context, str).delete();
    }

    private static File makeFilePath(Context context, String str) {
        return new File(context.getFilesDir(), str);
    }

    public void clearStats() {
        this.mStats = null;
    }

    public BatteryStats getStats() {
        if (this.mStats == null) {
            load();
        }
        return this.mStats;
    }

    public Intent getBatteryBroadcast() {
        if (this.mBatteryBroadcast == null && this.mCollectBatteryBroadcast) {
            load();
        }
        return this.mBatteryBroadcast;
    }

    public PowerProfile getPowerProfile() {
        return this.mPowerProfile;
    }

    public void create(BatteryStats batteryStats) {
        this.mPowerProfile = new PowerProfile(this.mContext);
        this.mStats = batteryStats;
    }

    public void create(Bundle bundle) {
        if (bundle != null) {
            this.mStats = sStatsXfer;
            this.mBatteryBroadcast = sBatteryBroadcastXfer;
        }
        this.mBatteryInfo = IBatteryStats.Stub.asInterface(ServiceManager.getService(BatteryStats.SERVICE_NAME));
        this.mPowerProfile = new PowerProfile(this.mContext);
    }

    public void storeState() {
        sStatsXfer = this.mStats;
        sBatteryBroadcastXfer = this.mBatteryBroadcast;
    }

    public static String makemAh(double d) {
        String str;
        if (d == 0.0d) {
            return WifiEnterpriseConfig.ENGINE_DISABLE;
        }
        if (d < 1.0E-5d) {
            str = "%.8f";
        } else if (d < 1.0E-4d) {
            str = "%.7f";
        } else if (d < 0.001d) {
            str = "%.6f";
        } else if (d < 0.01d) {
            str = "%.5f";
        } else if (d < 0.1d) {
            str = "%.4f";
        } else if (d < 1.0d) {
            str = "%.3f";
        } else if (d < 10.0d) {
            str = "%.2f";
        } else if (d < 100.0d) {
            str = "%.1f";
        } else {
            str = "%.0f";
        }
        return String.format(Locale.ENGLISH, str, Double.valueOf(d));
    }

    public void refreshStats(int i, int i2) {
        SparseArray<UserHandle> sparseArray = new SparseArray<>(1);
        sparseArray.put(i2, new UserHandle(i2));
        refreshStats(i, sparseArray);
    }

    public void refreshStats(int i, List<UserHandle> list) {
        int size = list.size();
        SparseArray<UserHandle> sparseArray = new SparseArray<>(size);
        for (int i2 = 0; i2 < size; i2++) {
            UserHandle userHandle = list.get(i2);
            sparseArray.put(userHandle.getIdentifier(), userHandle);
        }
        refreshStats(i, sparseArray);
    }

    public void refreshStats(int i, SparseArray<UserHandle> sparseArray) {
        refreshStats(i, sparseArray, SystemClock.elapsedRealtime() * 1000, SystemClock.uptimeMillis() * 1000);
    }

    public void refreshStats(int i, SparseArray<UserHandle> sparseArray, long j, long j2) {
        PowerCalculator wifiPowerEstimator;
        getStats();
        this.mMaxPower = 0.0d;
        this.mMaxRealPower = 0.0d;
        this.mComputedPower = 0.0d;
        this.mTotalPower = 0.0d;
        this.mUsageList.clear();
        this.mWifiSippers.clear();
        this.mBluetoothSippers.clear();
        this.mUserSippers.clear();
        this.mMobilemsppList.clear();
        if (this.mStats == null) {
            return;
        }
        if (this.mCpuPowerCalculator == null) {
            this.mCpuPowerCalculator = new CpuPowerCalculator(this.mPowerProfile);
        }
        this.mCpuPowerCalculator.reset();
        if (this.mMemoryPowerCalculator == null) {
            this.mMemoryPowerCalculator = new MemoryPowerCalculator(this.mPowerProfile);
        }
        this.mMemoryPowerCalculator.reset();
        if (this.mWakelockPowerCalculator == null) {
            this.mWakelockPowerCalculator = new WakelockPowerCalculator(this.mPowerProfile);
        }
        this.mWakelockPowerCalculator.reset();
        if (this.mMobileRadioPowerCalculator == null) {
            this.mMobileRadioPowerCalculator = new MobileRadioPowerCalculator(this.mPowerProfile, this.mStats);
        }
        this.mMobileRadioPowerCalculator.reset(this.mStats);
        boolean zCheckHasWifiPowerReporting = checkHasWifiPowerReporting(this.mStats, this.mPowerProfile);
        if (this.mWifiPowerCalculator == null || zCheckHasWifiPowerReporting != this.mHasWifiPowerReporting) {
            if (zCheckHasWifiPowerReporting) {
                wifiPowerEstimator = new WifiPowerCalculator(this.mPowerProfile);
            } else {
                wifiPowerEstimator = new WifiPowerEstimator(this.mPowerProfile);
            }
            this.mWifiPowerCalculator = wifiPowerEstimator;
            this.mHasWifiPowerReporting = zCheckHasWifiPowerReporting;
        }
        this.mWifiPowerCalculator.reset();
        boolean zCheckHasBluetoothPowerReporting = checkHasBluetoothPowerReporting(this.mStats, this.mPowerProfile);
        if (this.mBluetoothPowerCalculator == null || zCheckHasBluetoothPowerReporting != this.mHasBluetoothPowerReporting) {
            this.mBluetoothPowerCalculator = new BluetoothPowerCalculator(this.mPowerProfile);
            this.mHasBluetoothPowerReporting = zCheckHasBluetoothPowerReporting;
        }
        this.mBluetoothPowerCalculator.reset();
        this.mSensorPowerCalculator = new SensorPowerCalculator(this.mPowerProfile, (SensorManager) this.mContext.getSystemService(Context.SENSOR_SERVICE), this.mStats, j, i);
        this.mSensorPowerCalculator.reset();
        if (this.mCameraPowerCalculator == null) {
            this.mCameraPowerCalculator = new CameraPowerCalculator(this.mPowerProfile);
        }
        this.mCameraPowerCalculator.reset();
        if (this.mFlashlightPowerCalculator == null) {
            this.mFlashlightPowerCalculator = new FlashlightPowerCalculator(this.mPowerProfile);
        }
        this.mFlashlightPowerCalculator.reset();
        if (this.mMediaPowerCalculator == null) {
            this.mMediaPowerCalculator = new MediaPowerCalculator(this.mPowerProfile);
        }
        this.mMediaPowerCalculator.reset();
        this.mStatsType = i;
        this.mRawUptimeUs = j2;
        this.mRawRealtimeUs = j;
        this.mBatteryUptimeUs = this.mStats.getBatteryUptime(j2);
        this.mBatteryRealtimeUs = this.mStats.getBatteryRealtime(j);
        this.mTypeBatteryUptimeUs = this.mStats.computeBatteryUptime(j2, this.mStatsType);
        this.mTypeBatteryRealtimeUs = this.mStats.computeBatteryRealtime(j, this.mStatsType);
        this.mBatteryTimeRemainingUs = this.mStats.computeBatteryTimeRemaining(j);
        this.mChargeTimeRemainingUs = this.mStats.computeChargeTimeRemaining(j);
        this.mMinDrainedPower = (((double) this.mStats.getLowDischargeAmountSinceCharge()) * this.mPowerProfile.getBatteryCapacity()) / 100.0d;
        this.mMaxDrainedPower = (((double) this.mStats.getHighDischargeAmountSinceCharge()) * this.mPowerProfile.getBatteryCapacity()) / 100.0d;
        processAppUsage(sparseArray);
        for (int i2 = 0; i2 < this.mUsageList.size(); i2++) {
            BatterySipper batterySipper = this.mUsageList.get(i2);
            batterySipper.computeMobilemspp();
            if (batterySipper.mobilemspp != 0.0d) {
                this.mMobilemsppList.add(batterySipper);
            }
        }
        for (int i3 = 0; i3 < this.mUserSippers.size(); i3++) {
            List<BatterySipper> listValueAt = this.mUserSippers.valueAt(i3);
            for (int i4 = 0; i4 < listValueAt.size(); i4++) {
                BatterySipper batterySipper2 = listValueAt.get(i4);
                batterySipper2.computeMobilemspp();
                if (batterySipper2.mobilemspp != 0.0d) {
                    this.mMobilemsppList.add(batterySipper2);
                }
            }
        }
        Collections.sort(this.mMobilemsppList, new Comparator<BatterySipper>() {
            @Override
            public int compare(BatterySipper batterySipper3, BatterySipper batterySipper4) {
                return Double.compare(batterySipper4.mobilemspp, batterySipper3.mobilemspp);
            }
        });
        processMiscUsage();
        Collections.sort(this.mUsageList);
        if (!this.mUsageList.isEmpty()) {
            double d = this.mUsageList.get(0).totalPowerMah;
            this.mMaxPower = d;
            this.mMaxRealPower = d;
            int size = this.mUsageList.size();
            for (int i5 = 0; i5 < size; i5++) {
                this.mComputedPower += this.mUsageList.get(i5).totalPowerMah;
            }
        }
        this.mTotalPower = this.mComputedPower;
        if (this.mStats.getLowDischargeAmountSinceCharge() > 1) {
            if (this.mMinDrainedPower > this.mComputedPower) {
                double d2 = this.mMinDrainedPower - this.mComputedPower;
                this.mTotalPower = this.mMinDrainedPower;
                BatterySipper batterySipper3 = new BatterySipper(BatterySipper.DrainType.UNACCOUNTED, null, d2);
                int iBinarySearch = Collections.binarySearch(this.mUsageList, batterySipper3);
                if (iBinarySearch < 0) {
                    iBinarySearch = -(iBinarySearch + 1);
                }
                this.mUsageList.add(iBinarySearch, batterySipper3);
                this.mMaxPower = Math.max(this.mMaxPower, d2);
            } else if (this.mMaxDrainedPower < this.mComputedPower) {
                double d3 = this.mComputedPower - this.mMaxDrainedPower;
                BatterySipper batterySipper4 = new BatterySipper(BatterySipper.DrainType.OVERCOUNTED, null, d3);
                int iBinarySearch2 = Collections.binarySearch(this.mUsageList, batterySipper4);
                if (iBinarySearch2 < 0) {
                    iBinarySearch2 = -(iBinarySearch2 + 1);
                }
                this.mUsageList.add(iBinarySearch2, batterySipper4);
                this.mMaxPower = Math.max(this.mMaxPower, d3);
            }
        }
        double dRemoveHiddenBatterySippers = removeHiddenBatterySippers(this.mUsageList);
        double totalPower = getTotalPower() - dRemoveHiddenBatterySippers;
        if (Math.abs(totalPower) > 0.001d) {
            int size2 = this.mUsageList.size();
            for (int i6 = 0; i6 < size2; i6++) {
                BatterySipper batterySipper5 = this.mUsageList.get(i6);
                if (!batterySipper5.shouldHide) {
                    batterySipper5.proportionalSmearMah = ((batterySipper5.totalPowerMah + batterySipper5.screenPowerMah) / totalPower) * dRemoveHiddenBatterySippers;
                    batterySipper5.sumPower();
                }
            }
        }
    }

    private void processAppUsage(SparseArray<UserHandle> sparseArray) {
        int i = 0;
        boolean z = sparseArray.get(-1) != null;
        this.mStatsPeriod = this.mTypeBatteryRealtimeUs;
        BatterySipper batterySipper = null;
        SparseArray<? extends BatteryStats.Uid> uidStats = this.mStats.getUidStats();
        int size = uidStats.size();
        while (i < size) {
            BatteryStats.Uid uidValueAt = uidStats.valueAt(i);
            BatterySipper batterySipper2 = new BatterySipper(BatterySipper.DrainType.APP, uidValueAt, 0.0d);
            SparseArray<? extends BatteryStats.Uid> sparseArray2 = uidStats;
            this.mCpuPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mWakelockPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mMobileRadioPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mWifiPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mBluetoothPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mSensorPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mCameraPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mFlashlightPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            this.mMediaPowerCalculator.calculateApp(batterySipper2, uidValueAt, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            if (batterySipper2.sumPower() != 0.0d || uidValueAt.getUid() == 0) {
                int uid = batterySipper2.getUid();
                int userId = UserHandle.getUserId(uid);
                if (uid == 1010) {
                    this.mWifiSippers.add(batterySipper2);
                } else if (uid == 1002) {
                    this.mBluetoothSippers.add(batterySipper2);
                } else if (z || sparseArray.get(userId) != null || UserHandle.getAppId(uid) < 10000) {
                    this.mUsageList.add(batterySipper2);
                } else {
                    List<BatterySipper> arrayList = this.mUserSippers.get(userId);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                        this.mUserSippers.put(userId, arrayList);
                    }
                    arrayList.add(batterySipper2);
                }
                if (uid == 0) {
                    batterySipper = batterySipper2;
                }
            }
            i++;
            uidStats = sparseArray2;
        }
        if (batterySipper != null) {
            this.mWakelockPowerCalculator.calculateRemaining(batterySipper, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
            batterySipper.sumPower();
        }
    }

    private void addPhoneUsage() {
        long phoneOnTime = this.mStats.getPhoneOnTime(this.mRawRealtimeUs, this.mStatsType) / 1000;
        double averagePower = (this.mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) * phoneOnTime) / 3600000.0d;
        if (averagePower != 0.0d) {
            addEntry(BatterySipper.DrainType.PHONE, phoneOnTime, averagePower);
        }
    }

    private void addScreenUsage() {
        long screenOnTime = this.mStats.getScreenOnTime(this.mRawRealtimeUs, this.mStatsType) / 1000;
        double averagePower = (screenOnTime * this.mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON)) + 0.0d;
        double averagePower2 = this.mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        for (int i = 0; i < 5; i++) {
            averagePower += ((((double) (i + 0.5f)) * averagePower2) / 5.0d) * (this.mStats.getScreenBrightnessTime(i, this.mRawRealtimeUs, this.mStatsType) / 1000);
        }
        double d = averagePower / 3600000.0d;
        if (d != 0.0d) {
            addEntry(BatterySipper.DrainType.SCREEN, screenOnTime, d);
        }
    }

    private void addAmbientDisplayUsage() {
        long screenDozeTime = this.mStats.getScreenDozeTime(this.mRawRealtimeUs, this.mStatsType) / 1000;
        double averagePower = (this.mPowerProfile.getAveragePower(PowerProfile.POWER_AMBIENT_DISPLAY) * screenDozeTime) / 3600000.0d;
        if (averagePower > 0.0d) {
            addEntry(BatterySipper.DrainType.AMBIENT_DISPLAY, screenDozeTime, averagePower);
        }
    }

    private void addRadioUsage() {
        BatterySipper batterySipper = new BatterySipper(BatterySipper.DrainType.CELL, null, 0.0d);
        this.mMobileRadioPowerCalculator.calculateRemaining(batterySipper, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        batterySipper.sumPower();
        if (batterySipper.totalPowerMah > 0.0d) {
            this.mUsageList.add(batterySipper);
        }
    }

    private void aggregateSippers(BatterySipper batterySipper, List<BatterySipper> list, String str) {
        for (int i = 0; i < list.size(); i++) {
            batterySipper.add(list.get(i));
        }
        batterySipper.computeMobilemspp();
        batterySipper.sumPower();
    }

    private void addIdleUsage() {
        double averagePower = (((this.mTypeBatteryRealtimeUs / 1000) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_SUSPEND)) + ((this.mTypeBatteryUptimeUs / 1000) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE))) / 3600000.0d;
        if (averagePower != 0.0d) {
            addEntry(BatterySipper.DrainType.IDLE, this.mTypeBatteryRealtimeUs / 1000, averagePower);
        }
    }

    private void addWiFiUsage() {
        BatterySipper batterySipper = new BatterySipper(BatterySipper.DrainType.WIFI, null, 0.0d);
        this.mWifiPowerCalculator.calculateRemaining(batterySipper, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        aggregateSippers(batterySipper, this.mWifiSippers, "WIFI");
        if (batterySipper.totalPowerMah > 0.0d) {
            this.mUsageList.add(batterySipper);
        }
    }

    private void addBluetoothUsage() {
        BatterySipper batterySipper = new BatterySipper(BatterySipper.DrainType.BLUETOOTH, null, 0.0d);
        this.mBluetoothPowerCalculator.calculateRemaining(batterySipper, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        aggregateSippers(batterySipper, this.mBluetoothSippers, "Bluetooth");
        if (batterySipper.totalPowerMah > 0.0d) {
            this.mUsageList.add(batterySipper);
        }
    }

    private void addUserUsage() {
        for (int i = 0; i < this.mUserSippers.size(); i++) {
            int iKeyAt = this.mUserSippers.keyAt(i);
            BatterySipper batterySipper = new BatterySipper(BatterySipper.DrainType.USER, null, 0.0d);
            batterySipper.userId = iKeyAt;
            aggregateSippers(batterySipper, this.mUserSippers.valueAt(i), "User");
            this.mUsageList.add(batterySipper);
        }
    }

    private void addMemoryUsage() {
        BatterySipper batterySipper = new BatterySipper(BatterySipper.DrainType.MEMORY, null, 0.0d);
        this.mMemoryPowerCalculator.calculateRemaining(batterySipper, this.mStats, this.mRawRealtimeUs, this.mRawUptimeUs, this.mStatsType);
        batterySipper.sumPower();
        if (batterySipper.totalPowerMah > 0.0d) {
            this.mUsageList.add(batterySipper);
        }
    }

    private void processMiscUsage() {
        addUserUsage();
        addPhoneUsage();
        addScreenUsage();
        addAmbientDisplayUsage();
        addWiFiUsage();
        addBluetoothUsage();
        addMemoryUsage();
        addIdleUsage();
        if (!this.mWifiOnly) {
            addRadioUsage();
        }
    }

    private BatterySipper addEntry(BatterySipper.DrainType drainType, long j, double d) {
        BatterySipper batterySipper = new BatterySipper(drainType, null, 0.0d);
        batterySipper.usagePowerMah = d;
        batterySipper.usageTimeMs = j;
        batterySipper.sumPower();
        this.mUsageList.add(batterySipper);
        return batterySipper;
    }

    public List<BatterySipper> getUsageList() {
        return this.mUsageList;
    }

    public List<BatterySipper> getMobilemsppList() {
        return this.mMobilemsppList;
    }

    public long getStatsPeriod() {
        return this.mStatsPeriod;
    }

    public int getStatsType() {
        return this.mStatsType;
    }

    public double getMaxPower() {
        return this.mMaxPower;
    }

    public double getMaxRealPower() {
        return this.mMaxRealPower;
    }

    public double getTotalPower() {
        return this.mTotalPower;
    }

    public double getComputedPower() {
        return this.mComputedPower;
    }

    public double getMinDrainedPower() {
        return this.mMinDrainedPower;
    }

    public double getMaxDrainedPower() {
        return this.mMaxDrainedPower;
    }

    public static byte[] readFully(FileInputStream fileInputStream) throws IOException {
        return readFully(fileInputStream, fileInputStream.available());
    }

    public static byte[] readFully(FileInputStream fileInputStream, int i) throws IOException {
        byte[] bArr = new byte[i];
        int i2 = 0;
        while (true) {
            int i3 = fileInputStream.read(bArr, i2, bArr.length - i2);
            if (i3 <= 0) {
                return bArr;
            }
            i2 += i3;
            int iAvailable = fileInputStream.available();
            if (iAvailable > bArr.length - i2) {
                byte[] bArr2 = new byte[iAvailable + i2];
                System.arraycopy(bArr, 0, bArr2, 0, i2);
                bArr = bArr2;
            }
        }
    }

    public double removeHiddenBatterySippers(List<BatterySipper> list) {
        double d = 0.0d;
        BatterySipper batterySipper = null;
        for (int size = list.size() - 1; size >= 0; size--) {
            BatterySipper batterySipper2 = list.get(size);
            batterySipper2.shouldHide = shouldHideSipper(batterySipper2);
            if (batterySipper2.shouldHide && batterySipper2.drainType != BatterySipper.DrainType.OVERCOUNTED && batterySipper2.drainType != BatterySipper.DrainType.SCREEN && batterySipper2.drainType != BatterySipper.DrainType.AMBIENT_DISPLAY && batterySipper2.drainType != BatterySipper.DrainType.UNACCOUNTED && batterySipper2.drainType != BatterySipper.DrainType.BLUETOOTH && batterySipper2.drainType != BatterySipper.DrainType.WIFI && batterySipper2.drainType != BatterySipper.DrainType.IDLE) {
                d += batterySipper2.totalPowerMah;
            }
            if (batterySipper2.drainType == BatterySipper.DrainType.SCREEN) {
                batterySipper = batterySipper2;
            }
        }
        smearScreenBatterySipper(list, batterySipper);
        return d;
    }

    public void smearScreenBatterySipper(List<BatterySipper> list, BatterySipper batterySipper) {
        SparseLongArray sparseLongArray = new SparseLongArray();
        int size = list.size();
        long j = 0;
        for (int i = 0; i < size; i++) {
            BatteryStats.Uid uid = list.get(i).uidObj;
            if (uid != null) {
                long processForegroundTimeMs = getProcessForegroundTimeMs(uid, 0);
                sparseLongArray.put(uid.getUid(), processForegroundTimeMs);
                j += processForegroundTimeMs;
            }
        }
        if (batterySipper != null && j >= 600000) {
            double d = batterySipper.totalPowerMah;
            int size2 = list.size();
            for (int i2 = 0; i2 < size2; i2++) {
                list.get(i2).screenPowerMah = (sparseLongArray.get(r3.getUid(), 0L) * d) / j;
            }
        }
    }

    public boolean shouldHideSipper(BatterySipper batterySipper) {
        BatterySipper.DrainType drainType = batterySipper.drainType;
        return drainType == BatterySipper.DrainType.IDLE || drainType == BatterySipper.DrainType.CELL || drainType == BatterySipper.DrainType.SCREEN || drainType == BatterySipper.DrainType.AMBIENT_DISPLAY || drainType == BatterySipper.DrainType.UNACCOUNTED || drainType == BatterySipper.DrainType.OVERCOUNTED || isTypeService(batterySipper) || isTypeSystem(batterySipper);
    }

    public boolean isTypeService(BatterySipper batterySipper) {
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(batterySipper.getUid());
        if (packagesForUid == null) {
            return false;
        }
        for (String str : packagesForUid) {
            if (ArrayUtils.contains(this.mServicepackageArray, str)) {
                return true;
            }
        }
        return false;
    }

    public boolean isTypeSystem(BatterySipper batterySipper) {
        int uid = batterySipper.uidObj == null ? -1 : batterySipper.getUid();
        batterySipper.mPackages = this.mPackageManager.getPackagesForUid(uid);
        if (uid >= 0 && uid < 10000) {
            return true;
        }
        if (batterySipper.mPackages != null) {
            for (String str : batterySipper.mPackages) {
                if (ArrayUtils.contains(this.mSystemPackageArray, str)) {
                    return true;
                }
            }
        }
        return false;
    }

    public long convertUsToMs(long j) {
        return j / 1000;
    }

    public long convertMsToUs(long j) {
        return j * 1000;
    }

    @VisibleForTesting
    public long getForegroundActivityTotalTimeUs(BatteryStats.Uid uid, long j) {
        BatteryStats.Timer foregroundActivityTimer = uid.getForegroundActivityTimer();
        if (foregroundActivityTimer != null) {
            return foregroundActivityTimer.getTotalTimeLocked(j, 0);
        }
        return 0L;
    }

    @VisibleForTesting
    public long getProcessForegroundTimeMs(BatteryStats.Uid uid, int i) {
        long jConvertMsToUs = convertMsToUs(SystemClock.elapsedRealtime());
        long processStateTime = 0;
        for (int i2 : new int[]{0}) {
            processStateTime += uid.getProcessStateTime(i2, jConvertMsToUs, i);
        }
        return convertUsToMs(Math.min(processStateTime, getForegroundActivityTotalTimeUs(uid, jConvertMsToUs)));
    }

    @VisibleForTesting
    public void setPackageManager(PackageManager packageManager) {
        this.mPackageManager = packageManager;
    }

    @VisibleForTesting
    public void setSystemPackageArray(String[] strArr) {
        this.mSystemPackageArray = strArr;
    }

    @VisibleForTesting
    public void setServicePackageArray(String[] strArr) {
        this.mServicepackageArray = strArr;
    }

    private void load() {
        if (this.mBatteryInfo == null) {
            return;
        }
        this.mStats = getStats(this.mBatteryInfo);
        if (this.mCollectBatteryBroadcast) {
            this.mBatteryBroadcast = this.mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }
    }

    private static BatteryStatsImpl getStats(IBatteryStats iBatteryStats) {
        try {
            ParcelFileDescriptor statisticsStream = iBatteryStats.getStatisticsStream();
            if (statisticsStream != null) {
                try {
                    ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(statisticsStream);
                    Throwable th = null;
                    try {
                        byte[] fully = readFully(autoCloseInputStream, MemoryFile.getSize(statisticsStream.getFileDescriptor()));
                        Parcel parcelObtain = Parcel.obtain();
                        parcelObtain.unmarshall(fully, 0, fully.length);
                        parcelObtain.setDataPosition(0);
                        BatteryStatsImpl batteryStatsImplCreateFromParcel = BatteryStatsImpl.CREATOR.createFromParcel(parcelObtain);
                        autoCloseInputStream.close();
                        return batteryStatsImplCreateFromParcel;
                    } catch (Throwable th2) {
                        if (0 != 0) {
                            try {
                                autoCloseInputStream.close();
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                            }
                        } else {
                            autoCloseInputStream.close();
                        }
                        throw th2;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Unable to read statistics stream", e);
                }
            }
        } catch (RemoteException e2) {
            Log.w(TAG, "RemoteException:", e2);
        }
        return new BatteryStatsImpl();
    }
}
