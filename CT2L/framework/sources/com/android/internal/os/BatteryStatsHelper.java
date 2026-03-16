package com.android.internal.os;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
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
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.BatterySipper;
import com.android.internal.telephony.PhoneConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class BatteryStatsHelper {
    private static final boolean DEBUG = false;
    private static Intent sBatteryBroadcastXfer;
    private static BatteryStats sStatsXfer;
    private long mAppMobileActive;
    private long mAppWifiRunning;
    private Intent mBatteryBroadcast;
    private IBatteryStats mBatteryInfo;
    long mBatteryRealtime;
    long mBatteryTimeRemaining;
    long mBatteryUptime;
    private double mBluetoothPower;
    private final List<BatterySipper> mBluetoothSippers;
    long mChargeTimeRemaining;
    private final boolean mCollectBatteryBroadcast;
    private double mComputedPower;
    private final Context mContext;
    private double mMaxDrainedPower;
    private double mMaxPower;
    private double mMaxRealPower;
    private double mMinDrainedPower;
    private final List<BatterySipper> mMobilemsppList;
    private PowerProfile mPowerProfile;
    long mRawRealtime;
    long mRawUptime;
    private BatteryStats mStats;
    private long mStatsPeriod;
    private int mStatsType;
    private double mTotalPower;
    long mTypeBatteryRealtime;
    long mTypeBatteryUptime;
    private final List<BatterySipper> mUsageList;
    private final SparseArray<Double> mUserPower;
    private final SparseArray<List<BatterySipper>> mUserSippers;
    private final boolean mWifiOnly;
    private double mWifiPower;
    private final List<BatterySipper> mWifiSippers;
    private static final String TAG = BatteryStatsHelper.class.getSimpleName();
    private static ArrayMap<File, BatteryStats> sFileXfer = new ArrayMap<>();

    public BatteryStatsHelper(Context context) {
        this(context, true);
    }

    public BatteryStatsHelper(Context context, boolean collectBatteryBroadcast) {
        this.mUsageList = new ArrayList();
        this.mWifiSippers = new ArrayList();
        this.mBluetoothSippers = new ArrayList();
        this.mUserSippers = new SparseArray<>();
        this.mUserPower = new SparseArray<>();
        this.mMobilemsppList = new ArrayList();
        this.mStatsType = 0;
        this.mStatsPeriod = 0L;
        this.mMaxPower = 1.0d;
        this.mMaxRealPower = 1.0d;
        this.mContext = context;
        this.mCollectBatteryBroadcast = collectBatteryBroadcast;
        this.mWifiOnly = checkWifiOnly(context);
    }

    public BatteryStatsHelper(Context context, boolean collectBatteryBroadcast, boolean wifiOnly) {
        this.mUsageList = new ArrayList();
        this.mWifiSippers = new ArrayList();
        this.mBluetoothSippers = new ArrayList();
        this.mUserSippers = new SparseArray<>();
        this.mUserPower = new SparseArray<>();
        this.mMobilemsppList = new ArrayList();
        this.mStatsType = 0;
        this.mStatsPeriod = 0L;
        this.mMaxPower = 1.0d;
        this.mMaxRealPower = 1.0d;
        this.mContext = context;
        this.mCollectBatteryBroadcast = collectBatteryBroadcast;
        this.mWifiOnly = wifiOnly;
    }

    public static boolean checkWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return !cm.isNetworkSupported(0);
    }

    public void storeStatsHistoryInFile(String fname) {
        FileOutputStream fout;
        synchronized (sFileXfer) {
            File path = makeFilePath(this.mContext, fname);
            sFileXfer.put(path, getStats());
            FileOutputStream fout2 = null;
            try {
                try {
                    fout = new FileOutputStream(path);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                Parcel hist = Parcel.obtain();
                getStats().writeToParcelWithoutUids(hist, 0);
                byte[] histData = hist.marshall();
                fout.write(histData);
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException e3) {
                e = e3;
                fout2 = fout;
                Log.w(TAG, "Unable to write history to file", e);
                if (fout2 != null) {
                    try {
                        fout2.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                fout2 = fout;
                if (fout2 != null) {
                    try {
                        fout2.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        }
    }

    public static BatteryStats statsFromFile(Context context, String fname) {
        FileInputStream fin;
        synchronized (sFileXfer) {
            File path = makeFilePath(context, fname);
            BatteryStats stats = sFileXfer.get(path);
            if (stats != null) {
                return stats;
            }
            FileInputStream fin2 = null;
            try {
                try {
                    fin = new FileInputStream(path);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                byte[] data = readFully(fin);
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(data, 0, data.length);
                parcel.setDataPosition(0);
                BatteryStats stats2 = BatteryStatsImpl.CREATOR.createFromParcel(parcel);
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e2) {
                    }
                }
                return stats2;
            } catch (IOException e3) {
                e = e3;
                fin2 = fin;
                Log.w(TAG, "Unable to read history to file", e);
                if (fin2 != null) {
                    try {
                        fin2.close();
                    } catch (IOException e4) {
                    }
                }
                return getStats(IBatteryStats.Stub.asInterface(ServiceManager.getService(BatteryStats.SERVICE_NAME)));
            } catch (Throwable th2) {
                th = th2;
                fin2 = fin;
                if (fin2 != null) {
                    try {
                        fin2.close();
                    } catch (IOException e5) {
                    }
                }
                throw th;
            }
        }
    }

    public static void dropFile(Context context, String fname) {
        makeFilePath(context, fname).delete();
    }

    private static File makeFilePath(Context context, String fname) {
        return new File(context.getFilesDir(), fname);
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

    public void create(BatteryStats stats) {
        this.mPowerProfile = new PowerProfile(this.mContext);
        this.mStats = stats;
    }

    public void create(Bundle icicle) {
        if (icicle != null) {
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

    public static String makemAh(double power) {
        return power < 1.0E-5d ? String.format("%.8f", Double.valueOf(power)) : power < 1.0E-4d ? String.format("%.7f", Double.valueOf(power)) : power < 0.001d ? String.format("%.6f", Double.valueOf(power)) : power < 0.01d ? String.format("%.5f", Double.valueOf(power)) : power < 0.1d ? String.format("%.4f", Double.valueOf(power)) : power < 1.0d ? String.format("%.3f", Double.valueOf(power)) : power < 10.0d ? String.format("%.2f", Double.valueOf(power)) : power < 100.0d ? String.format("%.1f", Double.valueOf(power)) : String.format("%.0f", Double.valueOf(power));
    }

    public void refreshStats(int statsType, int asUser) {
        SparseArray<UserHandle> users = new SparseArray<>(1);
        users.put(asUser, new UserHandle(asUser));
        refreshStats(statsType, users);
    }

    public void refreshStats(int statsType, List<UserHandle> asUsers) {
        int n = asUsers.size();
        SparseArray<UserHandle> users = new SparseArray<>(n);
        for (int i = 0; i < n; i++) {
            UserHandle userHandle = asUsers.get(i);
            users.put(userHandle.getIdentifier(), userHandle);
        }
        refreshStats(statsType, users);
    }

    public void refreshStats(int statsType, SparseArray<UserHandle> asUsers) {
        refreshStats(statsType, asUsers, SystemClock.elapsedRealtime() * 1000, SystemClock.uptimeMillis() * 1000);
    }

    public void refreshStats(int statsType, SparseArray<UserHandle> asUsers, long rawRealtimeUs, long rawUptimeUs) {
        getStats();
        this.mMaxPower = 0.0d;
        this.mMaxRealPower = 0.0d;
        this.mComputedPower = 0.0d;
        this.mTotalPower = 0.0d;
        this.mWifiPower = 0.0d;
        this.mBluetoothPower = 0.0d;
        this.mAppMobileActive = 0L;
        this.mAppWifiRunning = 0L;
        this.mUsageList.clear();
        this.mWifiSippers.clear();
        this.mBluetoothSippers.clear();
        this.mUserSippers.clear();
        this.mUserPower.clear();
        this.mMobilemsppList.clear();
        if (this.mStats != null) {
            this.mStatsType = statsType;
            this.mRawUptime = rawUptimeUs;
            this.mRawRealtime = rawRealtimeUs;
            this.mBatteryUptime = this.mStats.getBatteryUptime(rawUptimeUs);
            this.mBatteryRealtime = this.mStats.getBatteryRealtime(rawRealtimeUs);
            this.mTypeBatteryUptime = this.mStats.computeBatteryUptime(rawUptimeUs, this.mStatsType);
            this.mTypeBatteryRealtime = this.mStats.computeBatteryRealtime(rawRealtimeUs, this.mStatsType);
            this.mBatteryTimeRemaining = this.mStats.computeBatteryTimeRemaining(rawRealtimeUs);
            this.mChargeTimeRemaining = this.mStats.computeChargeTimeRemaining(rawRealtimeUs);
            this.mMinDrainedPower = (((double) this.mStats.getLowDischargeAmountSinceCharge()) * this.mPowerProfile.getBatteryCapacity()) / 100.0d;
            this.mMaxDrainedPower = (((double) this.mStats.getHighDischargeAmountSinceCharge()) * this.mPowerProfile.getBatteryCapacity()) / 100.0d;
            processAppUsage(asUsers);
            for (int i = 0; i < this.mUsageList.size(); i++) {
                BatterySipper bs = this.mUsageList.get(i);
                bs.computeMobilemspp();
                if (bs.mobilemspp != 0.0d) {
                    this.mMobilemsppList.add(bs);
                }
            }
            for (int i2 = 0; i2 < this.mUserSippers.size(); i2++) {
                List<BatterySipper> user = this.mUserSippers.valueAt(i2);
                for (int j = 0; j < user.size(); j++) {
                    BatterySipper bs2 = user.get(j);
                    bs2.computeMobilemspp();
                    if (bs2.mobilemspp != 0.0d) {
                        this.mMobilemsppList.add(bs2);
                    }
                }
            }
            Collections.sort(this.mMobilemsppList, new Comparator<BatterySipper>() {
                @Override
                public int compare(BatterySipper lhs, BatterySipper rhs) {
                    if (lhs.mobilemspp < rhs.mobilemspp) {
                        return 1;
                    }
                    if (lhs.mobilemspp > rhs.mobilemspp) {
                        return -1;
                    }
                    return 0;
                }
            });
            processMiscUsage();
            this.mTotalPower = this.mComputedPower;
            if (this.mStats.getLowDischargeAmountSinceCharge() > 1) {
                if (this.mMinDrainedPower > this.mComputedPower) {
                    double amount = this.mMinDrainedPower - this.mComputedPower;
                    this.mTotalPower = this.mMinDrainedPower;
                    addEntryNoTotal(BatterySipper.DrainType.UNACCOUNTED, 0L, amount);
                } else if (this.mMaxDrainedPower < this.mComputedPower) {
                    double amount2 = this.mComputedPower - this.mMaxDrainedPower;
                    addEntryNoTotal(BatterySipper.DrainType.OVERCOUNTED, 0L, amount2);
                }
            }
            Collections.sort(this.mUsageList);
        }
    }

    private void processAppUsage(SparseArray<UserHandle> asUsers) {
        double p;
        Double userPower;
        boolean forAllUsers = asUsers.get(-1) != null;
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService(Context.SENSOR_SERVICE);
        int which = this.mStatsType;
        int speedSteps = this.mPowerProfile.getNumSpeedSteps();
        double[] powerCpuNormal = new double[speedSteps];
        long[] cpuSpeedStepTimes = new long[speedSteps];
        for (int p2 = 0; p2 < speedSteps; p2++) {
            powerCpuNormal[p2] = this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_ACTIVE, p2);
        }
        double mobilePowerPerPacket = getMobilePowerPerPacket();
        double mobilePowerPerMs = getMobilePowerPerMs();
        double wifiPowerPerPacket = getWifiPowerPerPacket();
        long appWakelockTimeUs = 0;
        BatterySipper osApp = null;
        this.mStatsPeriod = this.mTypeBatteryRealtime;
        SparseArray<? extends BatteryStats.Uid> uidStats = this.mStats.getUidStats();
        int NU = uidStats.size();
        for (int iu = 0; iu < NU; iu++) {
            BatteryStats.Uid u = uidStats.valueAt(iu);
            double power = 0.0d;
            double highestDrain = 0.0d;
            String packageWithHighestDrain = null;
            Map<String, ? extends BatteryStats.Uid.Proc> processStats = u.getProcessStats();
            long cpuTime = 0;
            long cpuFgTime = 0;
            long wakelockTime = 0;
            long gpsTime = 0;
            if (processStats.size() > 0) {
                for (Map.Entry<String, ? extends BatteryStats.Uid.Proc> ent : processStats.entrySet()) {
                    BatteryStats.Uid.Proc ps = ent.getValue();
                    long userTime = ps.getUserTime(which);
                    long systemTime = ps.getSystemTime(which);
                    long foregroundTime = ps.getForegroundTime(which);
                    cpuFgTime += 10 * foregroundTime;
                    long tmpCpuTime = (userTime + systemTime) * 10;
                    int totalTimeAtSpeeds = 0;
                    for (int step = 0; step < speedSteps; step++) {
                        cpuSpeedStepTimes[step] = ps.getTimeAtCpuSpeedStep(step, which);
                        totalTimeAtSpeeds = (int) (((long) totalTimeAtSpeeds) + cpuSpeedStepTimes[step]);
                    }
                    if (totalTimeAtSpeeds == 0) {
                        totalTimeAtSpeeds = 1;
                    }
                    double processPower = 0.0d;
                    for (int step2 = 0; step2 < speedSteps; step2++) {
                        double ratio = cpuSpeedStepTimes[step2] / ((double) totalTimeAtSpeeds);
                        processPower += tmpCpuTime * ratio * powerCpuNormal[step2];
                    }
                    cpuTime += tmpCpuTime;
                    power += processPower;
                    if (packageWithHighestDrain == null || packageWithHighestDrain.startsWith(PhoneConstants.APN_TYPE_ALL)) {
                        highestDrain = processPower;
                        packageWithHighestDrain = ent.getKey();
                    } else if (highestDrain < processPower && !ent.getKey().startsWith(PhoneConstants.APN_TYPE_ALL)) {
                        highestDrain = processPower;
                        packageWithHighestDrain = ent.getKey();
                    }
                }
            }
            if (cpuFgTime > cpuTime) {
                cpuTime = cpuFgTime;
            }
            double power2 = power / 3600000.0d;
            Map<String, ? extends BatteryStats.Uid.Wakelock> wakelockStats = u.getWakelockStats();
            for (Map.Entry<String, ? extends BatteryStats.Uid.Wakelock> wakelockEntry : wakelockStats.entrySet()) {
                BatteryStats.Uid.Wakelock wakelock = wakelockEntry.getValue();
                BatteryStats.Timer timer = wakelock.getWakeTime(0);
                if (timer != null) {
                    wakelockTime += timer.getTotalTimeLocked(this.mRawRealtime, which);
                }
            }
            appWakelockTimeUs += wakelockTime;
            long wakelockTime2 = wakelockTime / 1000;
            double p3 = (wakelockTime2 * this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE)) / 3600000.0d;
            double power3 = power2 + p3;
            long mobileRx = u.getNetworkActivityPackets(0, this.mStatsType);
            long mobileTx = u.getNetworkActivityPackets(1, this.mStatsType);
            long mobileRxB = u.getNetworkActivityBytes(0, this.mStatsType);
            long mobileTxB = u.getNetworkActivityBytes(1, this.mStatsType);
            long mobileActive = u.getMobileRadioActiveTime(this.mStatsType);
            if (mobileActive > 0) {
                this.mAppMobileActive += mobileActive;
                p = (mobileActive * mobilePowerPerMs) / 1000.0d;
            } else {
                p = (mobileRx + mobileTx) * mobilePowerPerPacket;
            }
            double power4 = power3 + p;
            long wifiRx = u.getNetworkActivityPackets(2, this.mStatsType);
            long wifiTx = u.getNetworkActivityPackets(3, this.mStatsType);
            long wifiRxB = u.getNetworkActivityBytes(2, this.mStatsType);
            long wifiTxB = u.getNetworkActivityBytes(3, this.mStatsType);
            double p4 = (wifiRx + wifiTx) * wifiPowerPerPacket;
            double power5 = power4 + p4;
            long wifiRunningTimeMs = u.getWifiRunningTime(this.mRawRealtime, which) / 1000;
            this.mAppWifiRunning += wifiRunningTimeMs;
            double p5 = (wifiRunningTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)) / 3600000.0d;
            double power6 = power5 + p5;
            long wifiScanTimeMs = u.getWifiScanTime(this.mRawRealtime, which) / 1000;
            double p6 = (wifiScanTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_SCAN)) / 3600000.0d;
            double power7 = power6 + p6;
            for (int bin = 0; bin < 5; bin++) {
                long batchScanTimeMs = u.getWifiBatchedScanTime(bin, this.mRawRealtime, which) / 1000;
                double p7 = (batchScanTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_BATCHED_SCAN, bin)) / 3600000.0d;
                power7 += p7;
            }
            SparseArray<? extends BatteryStats.Uid.Sensor> sensorStats = u.getSensorStats();
            int NSE = sensorStats.size();
            for (int ise = 0; ise < NSE; ise++) {
                BatteryStats.Uid.Sensor sensor = sensorStats.valueAt(ise);
                int sensorHandle = sensorStats.keyAt(ise);
                long sensorTime = sensor.getSensorTime().getTotalTimeLocked(this.mRawRealtime, which) / 1000;
                double multiplier = 0.0d;
                switch (sensorHandle) {
                    case -10000:
                        multiplier = this.mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_ON);
                        gpsTime = sensorTime;
                        break;
                    default:
                        List<Sensor> sensorList = sensorManager.getSensorList(-1);
                        Iterator<Sensor> it = sensorList.iterator();
                        while (true) {
                            if (it.hasNext()) {
                                Sensor s = it.next();
                                if (s.getHandle() == sensorHandle) {
                                    multiplier = s.getPower();
                                    break;
                                }
                            }
                        }
                        break;
                }
                double p8 = (sensorTime * multiplier) / 3600000.0d;
                power7 += p8;
            }
            int userId = UserHandle.getUserId(u.getUid());
            if (power7 != 0.0d || u.getUid() == 0) {
                BatterySipper app = new BatterySipper(BatterySipper.DrainType.APP, u, new double[]{power7});
                app.cpuTime = cpuTime;
                app.gpsTime = gpsTime;
                app.wifiRunningTime = wifiRunningTimeMs;
                app.cpuFgTime = cpuFgTime;
                app.wakeLockTime = wakelockTime2;
                app.mobileRxPackets = mobileRx;
                app.mobileTxPackets = mobileTx;
                app.mobileActive = mobileActive / 1000;
                app.mobileActiveCount = u.getMobileRadioActiveCount(this.mStatsType);
                app.wifiRxPackets = wifiRx;
                app.wifiTxPackets = wifiTx;
                app.mobileRxBytes = mobileRxB;
                app.mobileTxBytes = mobileTxB;
                app.wifiRxBytes = wifiRxB;
                app.wifiTxBytes = wifiTxB;
                app.packageWithHighestDrain = packageWithHighestDrain;
                if (u.getUid() == 1010) {
                    this.mWifiSippers.add(app);
                    this.mWifiPower += power7;
                } else if (u.getUid() == 1002) {
                    this.mBluetoothSippers.add(app);
                    this.mBluetoothPower += power7;
                } else if (!forAllUsers && asUsers.get(userId) == null && UserHandle.getAppId(u.getUid()) >= 10000) {
                    List<BatterySipper> list = this.mUserSippers.get(userId);
                    if (list == null) {
                        list = new ArrayList<>();
                        this.mUserSippers.put(userId, list);
                    }
                    list.add(app);
                    if (power7 != 0.0d) {
                        Double userPower2 = this.mUserPower.get(userId);
                        if (userPower2 == null) {
                            userPower = Double.valueOf(power7);
                        } else {
                            userPower = Double.valueOf(userPower2.doubleValue() + power7);
                        }
                        this.mUserPower.put(userId, userPower);
                    }
                } else {
                    this.mUsageList.add(app);
                    if (power7 > this.mMaxPower) {
                        this.mMaxPower = power7;
                    }
                    if (power7 > this.mMaxRealPower) {
                        this.mMaxRealPower = power7;
                    }
                    this.mComputedPower += power7;
                }
                if (u.getUid() == 0) {
                    osApp = app;
                }
            }
        }
        if (osApp != null) {
            long wakeTimeMillis = (this.mBatteryUptime / 1000) - ((appWakelockTimeUs / 1000) + (this.mStats.getScreenOnTime(this.mRawRealtime, which) / 1000));
            if (wakeTimeMillis > 0) {
                double power8 = (wakeTimeMillis * this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_AWAKE)) / 3600000.0d;
                osApp.wakeLockTime += wakeTimeMillis;
                osApp.value += power8;
                double[] dArr = osApp.values;
                dArr[0] = dArr[0] + power8;
                if (osApp.value > this.mMaxPower) {
                    this.mMaxPower = osApp.value;
                }
                if (osApp.value > this.mMaxRealPower) {
                    this.mMaxRealPower = osApp.value;
                }
                this.mComputedPower += power8;
            }
        }
    }

    private void addPhoneUsage() {
        long phoneOnTimeMs = this.mStats.getPhoneOnTime(this.mRawRealtime, this.mStatsType) / 1000;
        double phoneOnPower = (this.mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) * phoneOnTimeMs) / 3600000.0d;
        if (phoneOnPower != 0.0d) {
            addEntry(BatterySipper.DrainType.PHONE, phoneOnTimeMs, phoneOnPower);
        }
    }

    private void addScreenUsage() {
        long screenOnTimeMs = this.mStats.getScreenOnTime(this.mRawRealtime, this.mStatsType) / 1000;
        double power = 0.0d + (screenOnTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_ON));
        double screenFullPower = this.mPowerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
        for (int i = 0; i < 5; i++) {
            double screenBinPower = (((double) (i + 0.5f)) * screenFullPower) / 5.0d;
            long brightnessTime = this.mStats.getScreenBrightnessTime(i, this.mRawRealtime, this.mStatsType) / 1000;
            double p = screenBinPower * brightnessTime;
            power += p;
        }
        double power2 = power / 3600000.0d;
        if (power2 != 0.0d) {
            addEntry(BatterySipper.DrainType.SCREEN, screenOnTimeMs, power2);
        }
    }

    private void addRadioUsage() {
        double power = 0.0d;
        long signalTimeMs = 0;
        long noCoverageTimeMs = 0;
        for (int i = 0; i < 5; i++) {
            long strengthTimeMs = this.mStats.getPhoneSignalStrengthTime(i, this.mRawRealtime, this.mStatsType) / 1000;
            double p = (strengthTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ON, i)) / 3600000.0d;
            power += p;
            signalTimeMs += strengthTimeMs;
            if (i == 0) {
                noCoverageTimeMs = strengthTimeMs;
            }
        }
        long scanningTimeMs = this.mStats.getPhoneSignalScanningTime(this.mRawRealtime, this.mStatsType) / 1000;
        double p2 = (scanningTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_SCANNING)) / 3600000.0d;
        double power2 = power + p2;
        long radioActiveTimeUs = this.mStats.getMobileRadioActiveTime(this.mRawRealtime, this.mStatsType);
        long remainingActiveTime = (radioActiveTimeUs - this.mAppMobileActive) / 1000;
        if (remainingActiveTime > 0) {
            power2 += getMobilePowerPerMs() * remainingActiveTime;
        }
        if (power2 != 0.0d) {
            BatterySipper bs = addEntry(BatterySipper.DrainType.CELL, signalTimeMs, power2);
            if (signalTimeMs != 0) {
                bs.noCoveragePercent = (noCoverageTimeMs * 100.0d) / signalTimeMs;
            }
            bs.mobileActive = remainingActiveTime;
            bs.mobileActiveCount = this.mStats.getMobileRadioActiveUnknownCount(this.mStatsType);
        }
    }

    private void aggregateSippers(BatterySipper bs, List<BatterySipper> from, String tag) {
        for (int i = 0; i < from.size(); i++) {
            BatterySipper wbs = from.get(i);
            bs.cpuTime += wbs.cpuTime;
            bs.gpsTime += wbs.gpsTime;
            bs.wifiRunningTime += wbs.wifiRunningTime;
            bs.cpuFgTime += wbs.cpuFgTime;
            bs.wakeLockTime += wbs.wakeLockTime;
            bs.mobileRxPackets += wbs.mobileRxPackets;
            bs.mobileTxPackets += wbs.mobileTxPackets;
            bs.mobileActive += wbs.mobileActive;
            bs.mobileActiveCount += wbs.mobileActiveCount;
            bs.wifiRxPackets += wbs.wifiRxPackets;
            bs.wifiTxPackets += wbs.wifiTxPackets;
            bs.mobileRxBytes += wbs.mobileRxBytes;
            bs.mobileTxBytes += wbs.mobileTxBytes;
            bs.wifiRxBytes += wbs.wifiRxBytes;
            bs.wifiTxBytes += wbs.wifiTxBytes;
        }
        bs.computeMobilemspp();
    }

    private void addWiFiUsage() {
        long onTimeMs = this.mStats.getWifiOnTime(this.mRawRealtime, this.mStatsType) / 1000;
        long runningTimeMs = (this.mStats.getGlobalWifiRunningTime(this.mRawRealtime, this.mStatsType) / 1000) - this.mAppWifiRunning;
        if (runningTimeMs < 0) {
            runningTimeMs = 0;
        }
        double wifiPower = (((0 * onTimeMs) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON)) + (runningTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ON))) / 3600000.0d;
        if (this.mWifiPower + wifiPower != 0.0d) {
            BatterySipper bs = addEntry(BatterySipper.DrainType.WIFI, runningTimeMs, this.mWifiPower + wifiPower);
            aggregateSippers(bs, this.mWifiSippers, "WIFI");
        }
    }

    private void addIdleUsage() {
        long idleTimeMs = (this.mTypeBatteryRealtime - this.mStats.getScreenOnTime(this.mRawRealtime, this.mStatsType)) / 1000;
        double idlePower = (idleTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_CPU_IDLE)) / 3600000.0d;
        if (idlePower != 0.0d) {
            addEntry(BatterySipper.DrainType.IDLE, idleTimeMs, idlePower);
        }
    }

    private void addBluetoothUsage() {
        long btOnTimeMs = this.mStats.getBluetoothOnTime(this.mRawRealtime, this.mStatsType) / 1000;
        double btPower = (btOnTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_ON)) / 3600000.0d;
        int btPingCount = this.mStats.getBluetoothPingCount();
        double pingPower = (((double) btPingCount) * this.mPowerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_AT_CMD)) / 3600000.0d;
        double btPower2 = btPower + pingPower;
        if (this.mBluetoothPower + btPower2 != 0.0d) {
            BatterySipper bs = addEntry(BatterySipper.DrainType.BLUETOOTH, btOnTimeMs, this.mBluetoothPower + btPower2);
            aggregateSippers(bs, this.mBluetoothSippers, "Bluetooth");
        }
    }

    private void addFlashlightUsage() {
        long flashlightOnTimeMs = this.mStats.getFlashlightOnTime(this.mRawRealtime, this.mStatsType) / 1000;
        double flashlightPower = (flashlightOnTimeMs * this.mPowerProfile.getAveragePower(PowerProfile.POWER_FLASHLIGHT)) / 3600000.0d;
        if (flashlightPower != 0.0d) {
            addEntry(BatterySipper.DrainType.FLASHLIGHT, flashlightOnTimeMs, flashlightPower);
        }
    }

    private void addUserUsage() {
        for (int i = 0; i < this.mUserSippers.size(); i++) {
            int userId = this.mUserSippers.keyAt(i);
            List<BatterySipper> sippers = this.mUserSippers.valueAt(i);
            Double userPower = this.mUserPower.get(userId);
            double power = userPower != null ? userPower.doubleValue() : 0.0d;
            BatterySipper bs = addEntry(BatterySipper.DrainType.USER, 0L, power);
            bs.userId = userId;
            aggregateSippers(bs, sippers, "User");
        }
    }

    private double getMobilePowerPerPacket() {
        double MOBILE_POWER = this.mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) / 3600.0d;
        long mobileRx = this.mStats.getNetworkActivityPackets(0, this.mStatsType);
        long mobileTx = this.mStats.getNetworkActivityPackets(1, this.mStatsType);
        long mobileData = mobileRx + mobileTx;
        long radioDataUptimeMs = this.mStats.getMobileRadioActiveTime(this.mRawRealtime, this.mStatsType) / 1000;
        double mobilePps = (mobileData == 0 || radioDataUptimeMs == 0) ? 12.20703125d : mobileData / radioDataUptimeMs;
        return (MOBILE_POWER / mobilePps) / 3600.0d;
    }

    private double getMobilePowerPerMs() {
        return this.mPowerProfile.getAveragePower(PowerProfile.POWER_RADIO_ACTIVE) / 3600000.0d;
    }

    private double getWifiPowerPerPacket() {
        double WIFI_POWER = this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_ACTIVE) / 3600.0d;
        return (WIFI_POWER / 61.03515625d) / 3600.0d;
    }

    private void processMiscUsage() {
        addUserUsage();
        addPhoneUsage();
        addScreenUsage();
        addFlashlightUsage();
        addWiFiUsage();
        addBluetoothUsage();
        addIdleUsage();
        if (!this.mWifiOnly) {
            addRadioUsage();
        }
    }

    private BatterySipper addEntry(BatterySipper.DrainType drainType, long time, double power) {
        this.mComputedPower += power;
        if (power > this.mMaxRealPower) {
            this.mMaxRealPower = power;
        }
        return addEntryNoTotal(drainType, time, power);
    }

    private BatterySipper addEntryNoTotal(BatterySipper.DrainType drainType, long time, double power) {
        if (power > this.mMaxPower) {
            this.mMaxPower = power;
        }
        BatterySipper bs = new BatterySipper(drainType, null, new double[]{power});
        bs.usageTime = time;
        this.mUsageList.add(bs);
        return bs;
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

    public long getBatteryTimeRemaining() {
        return this.mBatteryTimeRemaining;
    }

    public long getChargeTimeRemaining() {
        return this.mChargeTimeRemaining;
    }

    public static byte[] readFully(FileInputStream stream) throws IOException {
        return readFully(stream, stream.available());
    }

    public static byte[] readFully(FileInputStream stream, int avail) throws IOException {
        int pos = 0;
        byte[] data = new byte[avail];
        while (true) {
            int amt = stream.read(data, pos, data.length - pos);
            if (amt <= 0) {
                return data;
            }
            pos += amt;
            int avail2 = stream.available();
            if (avail2 > data.length - pos) {
                byte[] newData = new byte[pos + avail2];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
            }
        }
    }

    private void load() {
        if (this.mBatteryInfo != null) {
            this.mStats = getStats(this.mBatteryInfo);
            if (this.mCollectBatteryBroadcast) {
                this.mBatteryBroadcast = this.mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }
        }
    }

    private static BatteryStatsImpl getStats(IBatteryStats service) {
        try {
            ParcelFileDescriptor pfd = service.getStatisticsStream();
            if (pfd != null) {
                FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                try {
                    byte[] data = readFully(fis, MemoryFile.getSize(pfd.getFileDescriptor()));
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(data, 0, data.length);
                    parcel.setDataPosition(0);
                    BatteryStatsImpl stats = BatteryStatsImpl.CREATOR.createFromParcel(parcel);
                    stats.distributeWorkLocked(0);
                    return stats;
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
