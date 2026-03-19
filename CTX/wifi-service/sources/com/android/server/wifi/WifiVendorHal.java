package com.android.server.wifi;

import android.hardware.wifi.V1_0.IWifiApIface;
import android.hardware.wifi.V1_0.IWifiChip;
import android.hardware.wifi.V1_0.IWifiChipEventCallback;
import android.hardware.wifi.V1_0.IWifiIface;
import android.hardware.wifi.V1_0.IWifiRttController;
import android.hardware.wifi.V1_0.IWifiRttControllerEventCallback;
import android.hardware.wifi.V1_0.IWifiStaIface;
import android.hardware.wifi.V1_0.IWifiStaIfaceEventCallback;
import android.hardware.wifi.V1_0.RttCapabilities;
import android.hardware.wifi.V1_0.RttConfig;
import android.hardware.wifi.V1_0.RttResponder;
import android.hardware.wifi.V1_0.RttResult;
import android.hardware.wifi.V1_0.StaApfPacketFilterCapabilities;
import android.hardware.wifi.V1_0.StaBackgroundScanBucketParameters;
import android.hardware.wifi.V1_0.StaBackgroundScanCapabilities;
import android.hardware.wifi.V1_0.StaBackgroundScanParameters;
import android.hardware.wifi.V1_0.StaLinkLayerRadioStats;
import android.hardware.wifi.V1_0.StaLinkLayerStats;
import android.hardware.wifi.V1_0.StaRoamingCapabilities;
import android.hardware.wifi.V1_0.StaRoamingConfig;
import android.hardware.wifi.V1_0.StaScanData;
import android.hardware.wifi.V1_0.StaScanResult;
import android.hardware.wifi.V1_0.WifiDebugHostWakeReasonStats;
import android.hardware.wifi.V1_0.WifiDebugRingBufferStatus;
import android.hardware.wifi.V1_0.WifiDebugRxPacketFateReport;
import android.hardware.wifi.V1_0.WifiDebugTxPacketFateReport;
import android.hardware.wifi.V1_0.WifiInformationElement;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.V1_2.IWifiChipEventCallback;
import android.hardware.wifi.V1_2.IWifiStaIface;
import android.net.MacAddress;
import android.net.apf.ApfCapabilities;
import android.net.wifi.RttManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiSsid;
import android.net.wifi.WifiWakeReasonAndCounts;
import android.os.Handler;
import android.os.IHwInterface;
import android.os.Looper;
import android.os.RemoteException;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableBoolean;
import android.util.MutableInt;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.HexDump;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.WifiLog;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiVendorHal;
import com.android.server.wifi.util.BitMask;
import com.android.server.wifi.util.NativeUtil;
import com.google.errorprone.annotations.CompileTimeConstant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WifiVendorHal {

    @VisibleForTesting
    static final int sRssiMonCmdId = 7551;
    private WifiNative.VendorHalDeathEventHandler mDeathEventHandler;
    private String mDriverDescription;
    private String mFirmwareDescription;
    private final HalDeviceManager mHalDeviceManager;
    private final Handler mHalEventHandler;
    private IWifiChip mIWifiChip;
    private final ChipEventCallback mIWifiChipEventCallback;
    private final ChipEventCallbackV12 mIWifiChipEventCallbackV12;
    private IWifiRttController mIWifiRttController;
    private final IWifiStaIfaceEventCallback mIWifiStaIfaceEventCallback;
    private int mLastScanCmdId;
    private final Looper mLooper;
    private WifiNative.VendorHalRadioModeChangeEventHandler mRadioModeChangeEventHandler;
    private int mRttCmdId;
    private final RttEventCallback mRttEventCallback;
    private WifiNative.RttEventHandler mRttEventHandler;
    private WifiNative.WifiRssiEventHandler mWifiRssiEventHandler;
    private static final WifiLog sNoLog = new FakeWifiLog();
    public static final Object sLock = new Object();
    private static final int[][] sChipFeatureCapabilityTranslation = {new int[]{67108864, 256}, new int[]{128, 512}, new int[]{256, 1024}};
    private static final int[][] sStaFeatureCapabilityTranslation = {new int[]{2, 128}, new int[]{4, 256}, new int[]{32, 2}, new int[]{1024, 512}, new int[]{4096, 1024}, new int[]{8192, 2048}, new int[]{65536, 4}, new int[]{524288, 8}, new int[]{1048576, 8192}, new int[]{2097152, 4096}, new int[]{8388608, 16}, new int[]{16777216, 32}, new int[]{33554432, 64}};
    private static final ApfCapabilities sNoApfCapabilities = new ApfCapabilities(0, 0, 0);

    @VisibleForTesting
    WifiLog mVerboseLog = sNoLog;

    @VisibleForTesting
    WifiLog mLog = new LogcatLog("WifiVendorHal");
    private HashMap<String, IWifiStaIface> mIWifiStaIfaces = new HashMap<>();
    private HashMap<String, IWifiApIface> mIWifiApIfaces = new HashMap<>();

    @VisibleForTesting
    CurrentBackgroundScan mScan = null;

    @VisibleForTesting
    boolean mLinkLayerStatsDebug = false;
    private int mRttCmdIdNext = 1;
    private int mRttResponderCmdId = 0;
    private WifiNative.WifiLoggerEventHandler mLogEventHandler = null;
    private final HalDeviceManagerStatusListener mHalDeviceManagerStatusCallbacks = new HalDeviceManagerStatusListener();

    public void enableVerboseLogging(boolean z) {
        synchronized (sLock) {
            try {
                if (z) {
                    this.mVerboseLog = this.mLog;
                    enter("verbose=true").flush();
                } else {
                    enter("verbose=false").flush();
                    this.mVerboseLog = sNoLog;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean ok(WifiStatus wifiStatus) {
        if (wifiStatus.code == 0) {
            return true;
        }
        this.mLog.err("% failed %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(wifiStatus.toString()).flush();
        return false;
    }

    private boolean boolResult(boolean z) {
        if (this.mVerboseLog == sNoLog) {
            return z;
        }
        this.mVerboseLog.err("% returns %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(z).flush();
        return z;
    }

    private String stringResult(String str) {
        if (this.mVerboseLog == sNoLog) {
            return str;
        }
        this.mVerboseLog.err("% returns %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(str).flush();
        return str;
    }

    private byte[] byteArrayResult(byte[] bArr) {
        if (this.mVerboseLog == sNoLog) {
            return bArr;
        }
        this.mVerboseLog.err("% returns %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(HexDump.dumpHexString(bArr)).flush();
        return bArr;
    }

    private WifiLog.LogMessage enter(@CompileTimeConstant String str) {
        return this.mVerboseLog == sNoLog ? sNoLog.info(str) : this.mVerboseLog.trace(str, 1);
    }

    private static String niceMethodName(StackTraceElement[] stackTraceElementArr, int i) {
        String fileName;
        if (i >= stackTraceElementArr.length) {
            return "";
        }
        StackTraceElement stackTraceElement = stackTraceElementArr[i];
        String methodName = stackTraceElement.getMethodName();
        if (methodName.contains("lambda$") && (fileName = stackTraceElement.getFileName()) != null) {
            while (true) {
                i++;
                if (i >= stackTraceElementArr.length) {
                    break;
                }
                if (fileName.equals(stackTraceElementArr[i].getFileName())) {
                    methodName = stackTraceElementArr[i].getMethodName();
                    break;
                }
            }
        }
        return methodName + "(l." + stackTraceElement.getLineNumber() + ")";
    }

    public WifiVendorHal(HalDeviceManager halDeviceManager, Looper looper) {
        this.mHalDeviceManager = halDeviceManager;
        this.mLooper = looper;
        this.mHalEventHandler = new Handler(looper);
        this.mIWifiStaIfaceEventCallback = new StaIfaceEventCallback();
        this.mIWifiChipEventCallback = new ChipEventCallback();
        this.mIWifiChipEventCallbackV12 = new ChipEventCallbackV12();
        this.mRttEventCallback = new RttEventCallback();
    }

    private void handleRemoteException(RemoteException remoteException) {
        this.mVerboseLog.err("% RemoteException in HIDL call %").c(niceMethodName(Thread.currentThread().getStackTrace(), 3)).c(remoteException.toString()).flush();
        clearState();
    }

    public boolean initialize(WifiNative.VendorHalDeathEventHandler vendorHalDeathEventHandler) {
        synchronized (sLock) {
            this.mHalDeviceManager.initialize();
            this.mHalDeviceManager.registerStatusListener(this.mHalDeviceManagerStatusCallbacks, null);
            this.mDeathEventHandler = vendorHalDeathEventHandler;
        }
        return true;
    }

    public void registerRadioModeChangeHandler(WifiNative.VendorHalRadioModeChangeEventHandler vendorHalRadioModeChangeEventHandler) {
        synchronized (sLock) {
            this.mRadioModeChangeEventHandler = vendorHalRadioModeChangeEventHandler;
        }
    }

    public boolean isVendorHalSupported() {
        boolean zIsSupported;
        synchronized (sLock) {
            zIsSupported = this.mHalDeviceManager.isSupported();
        }
        return zIsSupported;
    }

    public boolean startVendorHalAp() {
        synchronized (sLock) {
            if (!startVendorHal()) {
                return false;
            }
            if (TextUtils.isEmpty(createApIface(null))) {
                stopVendorHal();
                return false;
            }
            return true;
        }
    }

    public boolean startVendorHalSta() {
        synchronized (sLock) {
            if (!startVendorHal()) {
                return false;
            }
            if (TextUtils.isEmpty(createStaIface(false, null))) {
                stopVendorHal();
                return false;
            }
            return true;
        }
    }

    public boolean startVendorHal() {
        synchronized (sLock) {
            if (!this.mHalDeviceManager.start()) {
                this.mLog.err("Failed to start vendor HAL").flush();
                return false;
            }
            this.mLog.info("Vendor Hal started successfully").flush();
            return true;
        }
    }

    private IWifiStaIface getStaIface(String str) {
        IWifiStaIface iWifiStaIface;
        synchronized (sLock) {
            iWifiStaIface = this.mIWifiStaIfaces.get(str);
        }
        return iWifiStaIface;
    }

    private class StaInterfaceDestroyedListenerInternal implements HalDeviceManager.InterfaceDestroyedListener {
        private final HalDeviceManager.InterfaceDestroyedListener mExternalListener;

        StaInterfaceDestroyedListenerInternal(HalDeviceManager.InterfaceDestroyedListener interfaceDestroyedListener) {
            this.mExternalListener = interfaceDestroyedListener;
        }

        @Override
        public void onDestroyed(String str) {
            synchronized (WifiVendorHal.sLock) {
                WifiVendorHal.this.mIWifiStaIfaces.remove(str);
            }
            if (this.mExternalListener != null) {
                this.mExternalListener.onDestroyed(str);
            }
        }
    }

    public String createStaIface(boolean z, HalDeviceManager.InterfaceDestroyedListener interfaceDestroyedListener) {
        synchronized (sLock) {
            IWifiStaIface iWifiStaIfaceCreateStaIface = this.mHalDeviceManager.createStaIface(z, new StaInterfaceDestroyedListenerInternal(interfaceDestroyedListener), null);
            if (iWifiStaIfaceCreateStaIface == null) {
                this.mLog.err("Failed to create STA iface").flush();
                return stringResult(null);
            }
            HalDeviceManager halDeviceManager = this.mHalDeviceManager;
            String name = HalDeviceManager.getName(iWifiStaIfaceCreateStaIface);
            if (TextUtils.isEmpty(name)) {
                this.mLog.err("Failed to get iface name").flush();
                return stringResult(null);
            }
            if (!registerStaIfaceCallback(iWifiStaIfaceCreateStaIface)) {
                this.mLog.err("Failed to register STA iface callback").flush();
                return stringResult(null);
            }
            this.mIWifiRttController = this.mHalDeviceManager.createRttController();
            if (this.mIWifiRttController == null) {
                this.mLog.err("Failed to create RTT controller").flush();
                return stringResult(null);
            }
            if (!registerRttEventCallback()) {
                this.mLog.err("Failed to register RTT controller callback").flush();
                return stringResult(null);
            }
            if (!retrieveWifiChip(iWifiStaIfaceCreateStaIface)) {
                this.mLog.err("Failed to get wifi chip").flush();
                return stringResult(null);
            }
            enableLinkLayerStats(iWifiStaIfaceCreateStaIface);
            this.mIWifiStaIfaces.put(name, iWifiStaIfaceCreateStaIface);
            return name;
        }
    }

    public boolean removeStaIface(String str) {
        synchronized (sLock) {
            IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            if (!this.mHalDeviceManager.removeIface(staIface)) {
                this.mLog.err("Failed to remove STA iface").flush();
                return boolResult(false);
            }
            this.mIWifiStaIfaces.remove(str);
            return true;
        }
    }

    private IWifiApIface getApIface(String str) {
        IWifiApIface iWifiApIface;
        synchronized (sLock) {
            iWifiApIface = this.mIWifiApIfaces.get(str);
        }
        return iWifiApIface;
    }

    private class ApInterfaceDestroyedListenerInternal implements HalDeviceManager.InterfaceDestroyedListener {
        private final HalDeviceManager.InterfaceDestroyedListener mExternalListener;

        ApInterfaceDestroyedListenerInternal(HalDeviceManager.InterfaceDestroyedListener interfaceDestroyedListener) {
            this.mExternalListener = interfaceDestroyedListener;
        }

        @Override
        public void onDestroyed(String str) {
            synchronized (WifiVendorHal.sLock) {
                WifiVendorHal.this.mIWifiApIfaces.remove(str);
            }
            if (this.mExternalListener != null) {
                this.mExternalListener.onDestroyed(str);
            }
        }
    }

    public String createApIface(HalDeviceManager.InterfaceDestroyedListener interfaceDestroyedListener) {
        synchronized (sLock) {
            IWifiApIface iWifiApIfaceCreateApIface = this.mHalDeviceManager.createApIface(new ApInterfaceDestroyedListenerInternal(interfaceDestroyedListener), null);
            if (iWifiApIfaceCreateApIface == null) {
                this.mLog.err("Failed to create AP iface").flush();
                return stringResult(null);
            }
            HalDeviceManager halDeviceManager = this.mHalDeviceManager;
            String name = HalDeviceManager.getName(iWifiApIfaceCreateApIface);
            if (TextUtils.isEmpty(name)) {
                this.mLog.err("Failed to get iface name").flush();
                return stringResult(null);
            }
            if (!retrieveWifiChip(iWifiApIfaceCreateApIface)) {
                this.mLog.err("Failed to get wifi chip").flush();
                return stringResult(null);
            }
            this.mIWifiApIfaces.put(name, iWifiApIfaceCreateApIface);
            return name;
        }
    }

    public boolean removeApIface(String str) {
        synchronized (sLock) {
            IWifiApIface apIface = getApIface(str);
            if (apIface == null) {
                return boolResult(false);
            }
            if (!this.mHalDeviceManager.removeIface(apIface)) {
                this.mLog.err("Failed to remove AP iface").flush();
                return boolResult(false);
            }
            this.mIWifiApIfaces.remove(str);
            return true;
        }
    }

    private boolean retrieveWifiChip(IWifiIface iWifiIface) {
        synchronized (sLock) {
            boolean z = this.mIWifiChip == null;
            this.mIWifiChip = this.mHalDeviceManager.getChip(iWifiIface);
            if (this.mIWifiChip == null) {
                this.mLog.err("Failed to get the chip created for the Iface").flush();
                return false;
            }
            if (!z) {
                return true;
            }
            if (registerChipCallback()) {
                return true;
            }
            this.mLog.err("Failed to register chip callback").flush();
            return false;
        }
    }

    private boolean registerStaIfaceCallback(IWifiStaIface iWifiStaIface) {
        synchronized (sLock) {
            try {
                if (iWifiStaIface == null) {
                    return boolResult(false);
                }
                if (this.mIWifiStaIfaceEventCallback == null) {
                    return boolResult(false);
                }
                try {
                    return ok(iWifiStaIface.registerEventCallback(this.mIWifiStaIfaceEventCallback));
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private boolean registerChipCallback() {
        WifiStatus wifiStatusRegisterEventCallback;
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return boolResult(false);
            }
            try {
                android.hardware.wifi.V1_2.IWifiChip wifiChipForV1_2Mockable = getWifiChipForV1_2Mockable();
                if (wifiChipForV1_2Mockable != null) {
                    wifiStatusRegisterEventCallback = wifiChipForV1_2Mockable.registerEventCallback_1_2(this.mIWifiChipEventCallbackV12);
                } else {
                    wifiStatusRegisterEventCallback = this.mIWifiChip.registerEventCallback(this.mIWifiChipEventCallback);
                }
                return ok(wifiStatusRegisterEventCallback);
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    private boolean registerRttEventCallback() {
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return boolResult(false);
            }
            if (this.mRttEventCallback == null) {
                return boolResult(false);
            }
            try {
                return ok(this.mIWifiRttController.registerEventCallback(this.mRttEventCallback));
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public void stopVendorHal() {
        synchronized (sLock) {
            this.mHalDeviceManager.stop();
            clearState();
            this.mLog.info("Vendor Hal stopped").flush();
        }
    }

    private void clearState() {
        this.mIWifiChip = null;
        this.mIWifiStaIfaces.clear();
        this.mIWifiApIfaces.clear();
        this.mIWifiRttController = null;
        this.mDriverDescription = null;
        this.mFirmwareDescription = null;
    }

    public boolean isHalStarted() {
        boolean z;
        synchronized (sLock) {
            z = (this.mIWifiStaIfaces.isEmpty() && this.mIWifiApIfaces.isEmpty()) ? false : true;
        }
        return z;
    }

    public boolean getBgScanCapabilities(String str, final WifiNative.ScanCapabilities scanCapabilities) {
        synchronized (sLock) {
            IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                staIface.getBackgroundScanCapabilities(new IWifiStaIface.getBackgroundScanCapabilitiesCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, StaBackgroundScanCapabilities staBackgroundScanCapabilities) {
                        WifiVendorHal.lambda$getBgScanCapabilities$0(this.f$0, scanCapabilities, mutableBoolean, wifiStatus, staBackgroundScanCapabilities);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static void lambda$getBgScanCapabilities$0(WifiVendorHal wifiVendorHal, WifiNative.ScanCapabilities scanCapabilities, MutableBoolean mutableBoolean, WifiStatus wifiStatus, StaBackgroundScanCapabilities staBackgroundScanCapabilities) {
        if (wifiVendorHal.ok(wifiStatus)) {
            wifiVendorHal.mVerboseLog.info("scan capabilities %").c(staBackgroundScanCapabilities.toString()).flush();
            scanCapabilities.max_scan_cache_size = staBackgroundScanCapabilities.maxCacheSize;
            scanCapabilities.max_ap_cache_per_scan = staBackgroundScanCapabilities.maxApCachePerScan;
            scanCapabilities.max_scan_buckets = staBackgroundScanCapabilities.maxBuckets;
            scanCapabilities.max_rssi_sample_size = 0;
            scanCapabilities.max_scan_reporting_threshold = staBackgroundScanCapabilities.maxReportingThreshold;
            mutableBoolean.value = true;
        }
    }

    @VisibleForTesting
    class CurrentBackgroundScan {
        public int cmdId;
        public WifiNative.ScanEventHandler eventHandler = null;
        public boolean paused = false;
        public WifiScanner.ScanData[] latestScanResults = null;
        public StaBackgroundScanParameters param = new StaBackgroundScanParameters();

        CurrentBackgroundScan(int i, WifiNative.ScanSettings scanSettings) {
            this.cmdId = i;
            this.param.basePeriodInMs = scanSettings.base_period_ms;
            this.param.maxApPerScan = scanSettings.max_ap_per_scan;
            this.param.reportThresholdPercent = scanSettings.report_threshold_percent;
            this.param.reportThresholdNumScans = scanSettings.report_threshold_num_scans;
            if (scanSettings.buckets != null) {
                for (WifiNative.BucketSettings bucketSettings : scanSettings.buckets) {
                    this.param.buckets.add(WifiVendorHal.this.makeStaBackgroundScanBucketParametersFromBucketSettings(bucketSettings));
                }
            }
        }
    }

    private StaBackgroundScanBucketParameters makeStaBackgroundScanBucketParametersFromBucketSettings(WifiNative.BucketSettings bucketSettings) {
        StaBackgroundScanBucketParameters staBackgroundScanBucketParameters = new StaBackgroundScanBucketParameters();
        staBackgroundScanBucketParameters.bucketIdx = bucketSettings.bucket;
        staBackgroundScanBucketParameters.band = makeWifiBandFromFrameworkBand(bucketSettings.band);
        if (bucketSettings.channels != null) {
            for (WifiNative.ChannelSettings channelSettings : bucketSettings.channels) {
                staBackgroundScanBucketParameters.frequencies.add(Integer.valueOf(channelSettings.frequency));
            }
        }
        staBackgroundScanBucketParameters.periodInMs = bucketSettings.period_ms;
        staBackgroundScanBucketParameters.eventReportScheme = makeReportSchemeFromBucketSettingsReportEvents(bucketSettings.report_events);
        staBackgroundScanBucketParameters.exponentialMaxPeriodInMs = bucketSettings.max_period_ms;
        staBackgroundScanBucketParameters.exponentialBase = 2;
        staBackgroundScanBucketParameters.exponentialStepCount = bucketSettings.step_count;
        return staBackgroundScanBucketParameters;
    }

    private int makeWifiBandFromFrameworkBand(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
            default:
                throw new IllegalArgumentException("bad band " + i);
            case 6:
                return 6;
            case 7:
                return 7;
        }
    }

    private int makeReportSchemeFromBucketSettingsReportEvents(int i) {
        BitMask bitMask = new BitMask(i);
        int i2 = 1;
        if (!bitMask.testAndClear(1)) {
            i2 = 0;
        }
        if (bitMask.testAndClear(2)) {
            i2 |= 2;
        }
        if (bitMask.testAndClear(4)) {
            i2 |= 4;
        }
        if (bitMask.value != 0) {
            throw new IllegalArgumentException("bad " + i);
        }
        return i2;
    }

    public boolean startBgScan(String str, WifiNative.ScanSettings scanSettings, WifiNative.ScanEventHandler scanEventHandler) {
        if (scanEventHandler == null) {
            return boolResult(false);
        }
        synchronized (sLock) {
            IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                if (this.mScan != null && !this.mScan.paused) {
                    ok(staIface.stopBackgroundScan(this.mScan.cmdId));
                    this.mScan = null;
                }
                this.mLastScanCmdId = (this.mLastScanCmdId % 9) + 1;
                CurrentBackgroundScan currentBackgroundScan = new CurrentBackgroundScan(this.mLastScanCmdId, scanSettings);
                if (!ok(staIface.startBackgroundScan(currentBackgroundScan.cmdId, currentBackgroundScan.param))) {
                    return false;
                }
                currentBackgroundScan.eventHandler = scanEventHandler;
                this.mScan = currentBackgroundScan;
                return true;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public void stopBgScan(String str) {
        synchronized (sLock) {
            IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return;
            }
            try {
                if (this.mScan != null) {
                    ok(staIface.stopBackgroundScan(this.mScan.cmdId));
                    this.mScan = null;
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public void pauseBgScan(String str) {
        synchronized (sLock) {
            try {
                IWifiStaIface staIface = getStaIface(str);
                if (staIface == null) {
                    return;
                }
                if (this.mScan != null && !this.mScan.paused) {
                    if (!ok(staIface.stopBackgroundScan(this.mScan.cmdId))) {
                    } else {
                        this.mScan.paused = true;
                    }
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public void restartBgScan(String str) {
        synchronized (sLock) {
            IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return;
            }
            try {
                if (this.mScan != null && this.mScan.paused) {
                    if (!ok(staIface.startBackgroundScan(this.mScan.cmdId, this.mScan.param))) {
                    } else {
                        this.mScan.paused = false;
                    }
                }
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
        }
    }

    public WifiScanner.ScanData[] getBgScanResults(String str) {
        synchronized (sLock) {
            if (getStaIface(str) == null) {
                return null;
            }
            if (this.mScan == null) {
                return null;
            }
            return this.mScan.latestScanResults;
        }
    }

    class C1AnswerBox {
        public StaLinkLayerStats value = null;

        C1AnswerBox() {
        }
    }

    public WifiLinkLayerStats getWifiLinkLayerStats(String str) {
        final C1AnswerBox c1AnswerBox = new C1AnswerBox();
        synchronized (sLock) {
            try {
                IWifiStaIface staIface = getStaIface(str);
                if (staIface == null) {
                    return null;
                }
                staIface.getLinkLayerStats(new IWifiStaIface.getLinkLayerStatsCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, StaLinkLayerStats staLinkLayerStats) {
                        WifiVendorHal.lambda$getWifiLinkLayerStats$1(this.f$0, c1AnswerBox, wifiStatus, staLinkLayerStats);
                    }
                });
                return frameworkFromHalLinkLayerStats(c1AnswerBox.value);
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static void lambda$getWifiLinkLayerStats$1(WifiVendorHal wifiVendorHal, C1AnswerBox c1AnswerBox, WifiStatus wifiStatus, StaLinkLayerStats staLinkLayerStats) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c1AnswerBox.value = staLinkLayerStats;
        }
    }

    @VisibleForTesting
    static WifiLinkLayerStats frameworkFromHalLinkLayerStats(StaLinkLayerStats staLinkLayerStats) {
        if (staLinkLayerStats == null) {
            return null;
        }
        WifiLinkLayerStats wifiLinkLayerStats = new WifiLinkLayerStats();
        wifiLinkLayerStats.beacon_rx = staLinkLayerStats.iface.beaconRx;
        wifiLinkLayerStats.rssi_mgmt = staLinkLayerStats.iface.avgRssiMgmt;
        wifiLinkLayerStats.rxmpdu_be = staLinkLayerStats.iface.wmeBePktStats.rxMpdu;
        wifiLinkLayerStats.txmpdu_be = staLinkLayerStats.iface.wmeBePktStats.txMpdu;
        wifiLinkLayerStats.lostmpdu_be = staLinkLayerStats.iface.wmeBePktStats.lostMpdu;
        wifiLinkLayerStats.retries_be = staLinkLayerStats.iface.wmeBePktStats.retries;
        wifiLinkLayerStats.rxmpdu_bk = staLinkLayerStats.iface.wmeBkPktStats.rxMpdu;
        wifiLinkLayerStats.txmpdu_bk = staLinkLayerStats.iface.wmeBkPktStats.txMpdu;
        wifiLinkLayerStats.lostmpdu_bk = staLinkLayerStats.iface.wmeBkPktStats.lostMpdu;
        wifiLinkLayerStats.retries_bk = staLinkLayerStats.iface.wmeBkPktStats.retries;
        wifiLinkLayerStats.rxmpdu_vi = staLinkLayerStats.iface.wmeViPktStats.rxMpdu;
        wifiLinkLayerStats.txmpdu_vi = staLinkLayerStats.iface.wmeViPktStats.txMpdu;
        wifiLinkLayerStats.lostmpdu_vi = staLinkLayerStats.iface.wmeViPktStats.lostMpdu;
        wifiLinkLayerStats.retries_vi = staLinkLayerStats.iface.wmeViPktStats.retries;
        wifiLinkLayerStats.rxmpdu_vo = staLinkLayerStats.iface.wmeVoPktStats.rxMpdu;
        wifiLinkLayerStats.txmpdu_vo = staLinkLayerStats.iface.wmeVoPktStats.txMpdu;
        wifiLinkLayerStats.lostmpdu_vo = staLinkLayerStats.iface.wmeVoPktStats.lostMpdu;
        wifiLinkLayerStats.retries_vo = staLinkLayerStats.iface.wmeVoPktStats.retries;
        if (staLinkLayerStats.radios.size() > 0) {
            StaLinkLayerRadioStats staLinkLayerRadioStats = staLinkLayerStats.radios.get(0);
            wifiLinkLayerStats.on_time = staLinkLayerRadioStats.onTimeInMs;
            wifiLinkLayerStats.tx_time = staLinkLayerRadioStats.txTimeInMs;
            wifiLinkLayerStats.tx_time_per_level = new int[staLinkLayerRadioStats.txTimeInMsPerLevel.size()];
            for (int i = 0; i < wifiLinkLayerStats.tx_time_per_level.length; i++) {
                wifiLinkLayerStats.tx_time_per_level[i] = staLinkLayerRadioStats.txTimeInMsPerLevel.get(i).intValue();
            }
            wifiLinkLayerStats.rx_time = staLinkLayerRadioStats.rxTimeInMs;
            wifiLinkLayerStats.on_time_scan = staLinkLayerRadioStats.onTimeInMsForScan;
        }
        wifiLinkLayerStats.timeStampInMs = staLinkLayerStats.timeStampInMs;
        return wifiLinkLayerStats;
    }

    private void enableLinkLayerStats(IWifiStaIface iWifiStaIface) {
        synchronized (sLock) {
            try {
            } catch (RemoteException e) {
                handleRemoteException(e);
            }
            if (!ok(iWifiStaIface.enableLinkLayerStatsCollection(this.mLinkLayerStatsDebug))) {
                this.mLog.err("unable to enable link layer stats collection").flush();
            }
        }
    }

    @VisibleForTesting
    int wifiFeatureMaskFromChipCapabilities(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < sChipFeatureCapabilityTranslation.length; i3++) {
            if ((sChipFeatureCapabilityTranslation[i3][1] & i) != 0) {
                i2 |= sChipFeatureCapabilityTranslation[i3][0];
            }
        }
        return i2;
    }

    @VisibleForTesting
    int wifiFeatureMaskFromStaCapabilities(int i) {
        int i2 = 0;
        for (int i3 = 0; i3 < sStaFeatureCapabilityTranslation.length; i3++) {
            if ((sStaFeatureCapabilityTranslation[i3][1] & i) != 0) {
                i2 |= sStaFeatureCapabilityTranslation[i3][0];
            }
        }
        return i2;
    }

    public int getSupportedFeatureSet(String str) {
        if (!this.mHalDeviceManager.isStarted()) {
            return 0;
        }
        try {
            final MutableInt mutableInt = new MutableInt(0);
            synchronized (sLock) {
                if (this.mIWifiChip != null) {
                    this.mIWifiChip.getCapabilities(new IWifiChip.getCapabilitiesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, int i) {
                            WifiVendorHal.lambda$getSupportedFeatureSet$2(this.f$0, mutableInt, wifiStatus, i);
                        }
                    });
                }
                IWifiStaIface staIface = getStaIface(str);
                if (staIface != null) {
                    staIface.getCapabilities(new IWifiStaIface.getCapabilitiesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, int i) {
                            WifiVendorHal.lambda$getSupportedFeatureSet$3(this.f$0, mutableInt, wifiStatus, i);
                        }
                    });
                }
            }
            int i = mutableInt.value;
            Set<Integer> supportedIfaceTypes = this.mHalDeviceManager.getSupportedIfaceTypes();
            if (supportedIfaceTypes.contains(0)) {
                i |= 1;
            }
            if (supportedIfaceTypes.contains(1)) {
                i |= 16;
            }
            if (supportedIfaceTypes.contains(2)) {
                i |= 8;
            }
            if (supportedIfaceTypes.contains(3)) {
                return i | 64;
            }
            return i;
        } catch (RemoteException e) {
            handleRemoteException(e);
            return 0;
        }
    }

    public static void lambda$getSupportedFeatureSet$2(WifiVendorHal wifiVendorHal, MutableInt mutableInt, WifiStatus wifiStatus, int i) {
        if (wifiVendorHal.ok(wifiStatus)) {
            mutableInt.value = wifiVendorHal.wifiFeatureMaskFromChipCapabilities(i);
        }
    }

    public static void lambda$getSupportedFeatureSet$3(WifiVendorHal wifiVendorHal, MutableInt mutableInt, WifiStatus wifiStatus, int i) {
        if (wifiVendorHal.ok(wifiStatus)) {
            mutableInt.value |= wifiVendorHal.wifiFeatureMaskFromStaCapabilities(i);
        }
    }

    class C2AnswerBox {
        public RttManager.RttCapabilities value = null;

        C2AnswerBox() {
        }
    }

    public RttManager.RttCapabilities getRttCapabilities() {
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return null;
            }
            try {
                final C2AnswerBox c2AnswerBox = new C2AnswerBox();
                this.mIWifiRttController.getCapabilities(new IWifiRttController.getCapabilitiesCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, RttCapabilities rttCapabilities) {
                        WifiVendorHal.lambda$getRttCapabilities$4(this.f$0, c2AnswerBox, wifiStatus, rttCapabilities);
                    }
                });
                return c2AnswerBox.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static void lambda$getRttCapabilities$4(WifiVendorHal wifiVendorHal, C2AnswerBox c2AnswerBox, WifiStatus wifiStatus, RttCapabilities rttCapabilities) {
        if (wifiVendorHal.ok(wifiStatus)) {
            wifiVendorHal.mVerboseLog.info("rtt capabilites %").c(rttCapabilities.toString()).flush();
            RttManager.RttCapabilities rttCapabilities2 = new RttManager.RttCapabilities();
            rttCapabilities2.oneSidedRttSupported = rttCapabilities.rttOneSidedSupported;
            rttCapabilities2.twoSided11McRttSupported = rttCapabilities.rttFtmSupported;
            rttCapabilities2.lciSupported = rttCapabilities.lciSupported;
            rttCapabilities2.lcrSupported = rttCapabilities.lcrSupported;
            rttCapabilities2.preambleSupported = frameworkPreambleFromHalPreamble(rttCapabilities.preambleSupport);
            rttCapabilities2.bwSupported = frameworkBwFromHalBw(rttCapabilities.bwSupport);
            rttCapabilities2.responderSupported = rttCapabilities.responderSupported;
            rttCapabilities2.secureRttSupported = false;
            rttCapabilities2.mcVersion = rttCapabilities.mcVersion & 255;
            c2AnswerBox.value = rttCapabilities2;
        }
    }

    private class RttEventCallback extends IWifiRttControllerEventCallback.Stub {
        private RttEventCallback() {
        }

        @Override
        public void onResults(int i, ArrayList<RttResult> arrayList) {
            synchronized (WifiVendorHal.sLock) {
                if (i == WifiVendorHal.this.mRttCmdId && WifiVendorHal.this.mRttEventHandler != null) {
                    WifiNative.RttEventHandler rttEventHandler = WifiVendorHal.this.mRttEventHandler;
                    WifiVendorHal.this.mRttCmdId = 0;
                    RttManager.RttResult[] rttResultArr = new RttManager.RttResult[arrayList.size()];
                    for (int i2 = 0; i2 < rttResultArr.length; i2++) {
                        rttResultArr[i2] = WifiVendorHal.frameworkRttResultFromHalRttResult(arrayList.get(i2));
                    }
                    rttEventHandler.onRttResults(rttResultArr);
                }
            }
        }
    }

    @VisibleForTesting
    static RttManager.RttResult frameworkRttResultFromHalRttResult(RttResult rttResult) {
        RttManager.RttResult rttResult2 = new RttManager.RttResult();
        rttResult2.bssid = NativeUtil.macAddressFromByteArray(rttResult.addr);
        rttResult2.burstNumber = rttResult.burstNum;
        rttResult2.measurementFrameNumber = rttResult.measurementNumber;
        rttResult2.successMeasurementFrameNumber = rttResult.successNumber;
        rttResult2.frameNumberPerBurstPeer = rttResult.numberPerBurstPeer;
        rttResult2.status = rttResult.status;
        rttResult2.retryAfterDuration = rttResult.retryAfterDuration;
        rttResult2.measurementType = rttResult.type;
        rttResult2.rssi = rttResult.rssi;
        rttResult2.rssiSpread = rttResult.rssiSpread;
        rttResult2.txRate = rttResult.txRate.bitRateInKbps;
        rttResult2.rxRate = rttResult.rxRate.bitRateInKbps;
        rttResult2.rtt = rttResult.rtt;
        rttResult2.rttStandardDeviation = rttResult.rttSd;
        rttResult2.rttSpread = rttResult.rttSpread;
        rttResult2.distance = rttResult.distanceInMm / 10;
        rttResult2.distanceStandardDeviation = rttResult.distanceSdInMm / 10;
        rttResult2.distanceSpread = rttResult.distanceSpreadInMm / 10;
        rttResult2.ts = rttResult.timeStampInUs;
        rttResult2.burstDuration = rttResult.burstDurationInMs;
        rttResult2.negotiatedBurstNum = rttResult.negotiatedBurstNum;
        rttResult2.LCI = ieFromHal(rttResult.lci);
        rttResult2.LCR = ieFromHal(rttResult.lcr);
        rttResult2.secure = false;
        return rttResult2;
    }

    @VisibleForTesting
    static RttManager.WifiInformationElement ieFromHal(WifiInformationElement wifiInformationElement) {
        if (wifiInformationElement == null) {
            return null;
        }
        RttManager.WifiInformationElement wifiInformationElement2 = new RttManager.WifiInformationElement();
        wifiInformationElement2.id = wifiInformationElement.id;
        wifiInformationElement2.data = NativeUtil.byteArrayFromArrayList(wifiInformationElement.data);
        return wifiInformationElement2;
    }

    @VisibleForTesting
    static RttConfig halRttConfigFromFrameworkRttParams(RttManager.RttParams rttParams) {
        RttConfig rttConfig = new RttConfig();
        if (rttParams.bssid != null) {
            byte[] bArrMacAddressToByteArray = NativeUtil.macAddressToByteArray(rttParams.bssid);
            for (int i = 0; i < rttConfig.addr.length; i++) {
                rttConfig.addr[i] = bArrMacAddressToByteArray[i];
            }
        }
        rttConfig.type = halRttTypeFromFrameworkRttType(rttParams.requestType);
        rttConfig.peer = halPeerFromFrameworkPeer(rttParams.deviceType);
        rttConfig.channel.width = halChannelWidthFromFrameworkChannelWidth(rttParams.channelWidth);
        rttConfig.channel.centerFreq = rttParams.frequency;
        rttConfig.channel.centerFreq0 = rttParams.centerFreq0;
        rttConfig.channel.centerFreq1 = rttParams.centerFreq1;
        rttConfig.burstPeriod = rttParams.interval;
        rttConfig.numBurst = rttParams.numberBurst;
        rttConfig.numFramesPerBurst = rttParams.numSamplesPerBurst;
        rttConfig.numRetriesPerRttFrame = rttParams.numRetriesPerMeasurementFrame;
        rttConfig.numRetriesPerFtmr = rttParams.numRetriesPerFTMR;
        rttConfig.mustRequestLci = rttParams.LCIRequest;
        rttConfig.mustRequestLcr = rttParams.LCRRequest;
        rttConfig.burstDuration = rttParams.burstTimeout;
        rttConfig.preamble = halPreambleFromFrameworkPreamble(rttParams.preamble);
        rttConfig.bw = halBwFromFrameworkBw(rttParams.bandwidth);
        return rttConfig;
    }

    @VisibleForTesting
    static int halRttTypeFromFrameworkRttType(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    @VisibleForTesting
    static int frameworkRttTypeFromHalRttType(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    @VisibleForTesting
    static int halPeerFromFrameworkPeer(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    @VisibleForTesting
    static int frameworkPeerFromHalPeer(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    @VisibleForTesting
    static int halChannelWidthFromFrameworkChannelWidth(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    @VisibleForTesting
    static int frameworkChannelWidthFromHalChannelWidth(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    @VisibleForTesting
    static int halPreambleFromFrameworkPreamble(int i) {
        BitMask bitMask = new BitMask(i);
        int i2 = 1;
        if (!bitMask.testAndClear(1)) {
            i2 = 0;
        }
        if (bitMask.testAndClear(2)) {
            i2 |= 2;
        }
        if (bitMask.testAndClear(4)) {
            i2 |= 4;
        }
        if (bitMask.value != 0) {
            throw new IllegalArgumentException("bad " + i);
        }
        return i2;
    }

    @VisibleForTesting
    static int frameworkPreambleFromHalPreamble(int i) {
        BitMask bitMask = new BitMask(i);
        int i2 = 1;
        if (!bitMask.testAndClear(1)) {
            i2 = 0;
        }
        if (bitMask.testAndClear(2)) {
            i2 |= 2;
        }
        if (bitMask.testAndClear(4)) {
            i2 |= 4;
        }
        if (bitMask.value != 0) {
            throw new IllegalArgumentException("bad " + i);
        }
        return i2;
    }

    @VisibleForTesting
    static int halBwFromFrameworkBw(int i) {
        BitMask bitMask = new BitMask(i);
        int i2 = 1;
        if (!bitMask.testAndClear(1)) {
            i2 = 0;
        }
        if (bitMask.testAndClear(2)) {
            i2 |= 2;
        }
        if (bitMask.testAndClear(4)) {
            i2 |= 4;
        }
        if (bitMask.testAndClear(8)) {
            i2 |= 8;
        }
        if (bitMask.testAndClear(16)) {
            i2 |= 16;
        }
        if (bitMask.testAndClear(32)) {
            i2 |= 32;
        }
        if (bitMask.value != 0) {
            throw new IllegalArgumentException("bad " + i);
        }
        return i2;
    }

    @VisibleForTesting
    static int frameworkBwFromHalBw(int i) {
        BitMask bitMask = new BitMask(i);
        int i2 = 1;
        if (!bitMask.testAndClear(1)) {
            i2 = 0;
        }
        if (bitMask.testAndClear(2)) {
            i2 |= 2;
        }
        if (bitMask.testAndClear(4)) {
            i2 |= 4;
        }
        if (bitMask.testAndClear(8)) {
            i2 |= 8;
        }
        if (bitMask.testAndClear(16)) {
            i2 |= 16;
        }
        if (bitMask.testAndClear(32)) {
            i2 |= 32;
        }
        if (bitMask.value != 0) {
            throw new IllegalArgumentException("bad " + i);
        }
        return i2;
    }

    @VisibleForTesting
    static ArrayList<RttConfig> halRttConfigArrayFromFrameworkRttParamsArray(RttManager.RttParams[] rttParamsArr) {
        ArrayList<RttConfig> arrayList = new ArrayList<>(rttParamsArr.length);
        for (RttManager.RttParams rttParams : rttParamsArr) {
            RttConfig rttConfigHalRttConfigFromFrameworkRttParams = halRttConfigFromFrameworkRttParams(rttParams);
            if (rttConfigHalRttConfigFromFrameworkRttParams != null) {
                arrayList.add(rttConfigHalRttConfigFromFrameworkRttParams);
            }
        }
        return arrayList;
    }

    public boolean requestRtt(RttManager.RttParams[] rttParamsArr, WifiNative.RttEventHandler rttEventHandler) {
        try {
            ArrayList<RttConfig> arrayListHalRttConfigArrayFromFrameworkRttParamsArray = halRttConfigArrayFromFrameworkRttParamsArray(rttParamsArr);
            synchronized (sLock) {
                if (this.mIWifiRttController == null) {
                    return boolResult(false);
                }
                if (this.mRttCmdId != 0) {
                    return boolResult(false);
                }
                int i = this.mRttCmdIdNext;
                this.mRttCmdIdNext = i + 1;
                this.mRttCmdId = i;
                this.mRttEventHandler = rttEventHandler;
                if (this.mRttCmdIdNext <= 0) {
                    this.mRttCmdIdNext = 1;
                }
                try {
                    if (ok(this.mIWifiRttController.rangeRequest(this.mRttCmdId, arrayListHalRttConfigArrayFromFrameworkRttParamsArray))) {
                        return true;
                    }
                    this.mRttCmdId = 0;
                    return false;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            }
        } catch (IllegalArgumentException e2) {
            this.mLog.err("Illegal argument for RTT request").c(e2.toString()).flush();
            return false;
        }
    }

    public boolean cancelRtt(RttManager.RttParams[] rttParamsArr) {
        ArrayList<RttConfig> arrayListHalRttConfigArrayFromFrameworkRttParamsArray = halRttConfigArrayFromFrameworkRttParamsArray(rttParamsArr);
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return boolResult(false);
            }
            if (this.mRttCmdId == 0) {
                return boolResult(false);
            }
            ArrayList<byte[]> arrayList = new ArrayList<>(arrayListHalRttConfigArrayFromFrameworkRttParamsArray.size());
            Iterator<RttConfig> it = arrayListHalRttConfigArrayFromFrameworkRttParamsArray.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().addr);
            }
            try {
                WifiStatus wifiStatusRangeCancel = this.mIWifiRttController.rangeCancel(this.mRttCmdId, arrayList);
                this.mRttCmdId = 0;
                return ok(wifiStatusRangeCancel);
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    class C3AnswerBox {
        public RttResponder value = null;

        C3AnswerBox() {
        }
    }

    private RttResponder getRttResponder() {
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return null;
            }
            final C3AnswerBox c3AnswerBox = new C3AnswerBox();
            try {
                this.mIWifiRttController.getResponderInfo(new IWifiRttController.getResponderInfoCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, RttResponder rttResponder) {
                        WifiVendorHal.lambda$getRttResponder$5(this.f$0, c3AnswerBox, wifiStatus, rttResponder);
                    }
                });
                return c3AnswerBox.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static void lambda$getRttResponder$5(WifiVendorHal wifiVendorHal, C3AnswerBox c3AnswerBox, WifiStatus wifiStatus, RttResponder rttResponder) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c3AnswerBox.value = rttResponder;
        }
    }

    private RttManager.ResponderConfig frameworkResponderConfigFromHalRttResponder(RttResponder rttResponder) {
        RttManager.ResponderConfig responderConfig = new RttManager.ResponderConfig();
        responderConfig.frequency = rttResponder.channel.centerFreq;
        responderConfig.centerFreq0 = rttResponder.channel.centerFreq0;
        responderConfig.centerFreq1 = rttResponder.channel.centerFreq1;
        responderConfig.channelWidth = frameworkChannelWidthFromHalChannelWidth(rttResponder.channel.width);
        responderConfig.preamble = frameworkPreambleFromHalPreamble(rttResponder.preamble);
        return responderConfig;
    }

    public RttManager.ResponderConfig enableRttResponder(int i) {
        RttManager.ResponderConfig responderConfigFrameworkResponderConfigFromHalRttResponder;
        RttResponder rttResponder = getRttResponder();
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return null;
            }
            if (this.mRttResponderCmdId != 0) {
                this.mLog.err("responder mode already enabled - this shouldn't happen").flush();
                return null;
            }
            int i2 = this.mRttCmdIdNext;
            this.mRttCmdIdNext = i2 + 1;
            if (this.mRttCmdIdNext <= 0) {
                this.mRttCmdIdNext = 1;
            }
            try {
                if (ok(this.mIWifiRttController.enableResponder(i2, null, i, rttResponder))) {
                    this.mRttResponderCmdId = i2;
                    responderConfigFrameworkResponderConfigFromHalRttResponder = frameworkResponderConfigFromHalRttResponder(rttResponder);
                    this.mVerboseLog.i("enabling rtt " + this.mRttResponderCmdId);
                } else {
                    responderConfigFrameworkResponderConfigFromHalRttResponder = null;
                }
                return responderConfigFrameworkResponderConfigFromHalRttResponder;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public boolean disableRttResponder() {
        synchronized (sLock) {
            if (this.mIWifiRttController == null) {
                return boolResult(false);
            }
            if (this.mRttResponderCmdId == 0) {
                return boolResult(false);
            }
            try {
                WifiStatus wifiStatusDisableResponder = this.mIWifiRttController.disableResponder(this.mRttResponderCmdId);
                this.mRttResponderCmdId = 0;
                return ok(wifiStatusDisableResponder);
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public boolean setScanningMacOui(String str, byte[] bArr) {
        if (bArr != null && bArr.length == 3) {
            synchronized (sLock) {
                try {
                    try {
                        IWifiStaIface staIface = getStaIface(str);
                        if (staIface == null) {
                            return boolResult(false);
                        }
                        if (!ok(staIface.setScanningMacOui(bArr))) {
                            return false;
                        }
                        return true;
                    } catch (RemoteException e) {
                        handleRemoteException(e);
                        return false;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        return boolResult(false);
    }

    public boolean setMacAddress(String str, MacAddress macAddress) {
        byte[] byteArray = macAddress.toByteArray();
        synchronized (sLock) {
            try {
                try {
                    android.hardware.wifi.V1_2.IWifiStaIface wifiStaIfaceForV1_2Mockable = getWifiStaIfaceForV1_2Mockable(str);
                    if (wifiStaIfaceForV1_2Mockable == null) {
                        return boolResult(false);
                    }
                    if (!ok(wifiStaIfaceForV1_2Mockable.setMacAddress(byteArray))) {
                        return false;
                    }
                    return true;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    class C4AnswerBox {
        public ApfCapabilities value = WifiVendorHal.sNoApfCapabilities;

        C4AnswerBox() {
        }
    }

    public ApfCapabilities getApfCapabilities(String str) {
        synchronized (sLock) {
            try {
                try {
                    IWifiStaIface staIface = getStaIface(str);
                    if (staIface == null) {
                        return sNoApfCapabilities;
                    }
                    final C4AnswerBox c4AnswerBox = new C4AnswerBox();
                    staIface.getApfPacketFilterCapabilities(new IWifiStaIface.getApfPacketFilterCapabilitiesCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, StaApfPacketFilterCapabilities staApfPacketFilterCapabilities) {
                            WifiVendorHal.lambda$getApfCapabilities$6(this.f$0, c4AnswerBox, wifiStatus, staApfPacketFilterCapabilities);
                        }
                    });
                    return c4AnswerBox.value;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return sNoApfCapabilities;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static void lambda$getApfCapabilities$6(WifiVendorHal wifiVendorHal, C4AnswerBox c4AnswerBox, WifiStatus wifiStatus, StaApfPacketFilterCapabilities staApfPacketFilterCapabilities) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c4AnswerBox.value = new ApfCapabilities(staApfPacketFilterCapabilities.version, staApfPacketFilterCapabilities.maxLength, OsConstants.ARPHRD_ETHER);
        }
    }

    public boolean installPacketFilter(String str, byte[] bArr) {
        if (bArr == null) {
            return boolResult(false);
        }
        ArrayList<Byte> arrayListByteArrayToArrayList = NativeUtil.byteArrayToArrayList(bArr);
        enter("filter length %").c(bArr.length).flush();
        synchronized (sLock) {
            try {
                try {
                    IWifiStaIface staIface = getStaIface(str);
                    if (staIface == null) {
                        return boolResult(false);
                    }
                    if (!ok(staIface.installApfPacketFilter(0, arrayListByteArrayToArrayList))) {
                        return false;
                    }
                    return true;
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    class C5AnswerBox {
        public byte[] data = null;

        C5AnswerBox() {
        }
    }

    public byte[] readPacketFilter(String str) {
        final C5AnswerBox c5AnswerBox = new C5AnswerBox();
        enter("").flush();
        synchronized (sLock) {
            try {
                try {
                    android.hardware.wifi.V1_2.IWifiStaIface wifiStaIfaceForV1_2Mockable = getWifiStaIfaceForV1_2Mockable(str);
                    if (wifiStaIfaceForV1_2Mockable == null) {
                        return byteArrayResult(null);
                    }
                    wifiStaIfaceForV1_2Mockable.readApfPacketFilterData(new IWifiStaIface.readApfPacketFilterDataCallback() {
                        @Override
                        public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                            WifiVendorHal.lambda$readPacketFilter$7(this.f$0, c5AnswerBox, wifiStatus, arrayList);
                        }
                    });
                    return byteArrayResult(c5AnswerBox.data);
                } catch (RemoteException e) {
                    handleRemoteException(e);
                    return byteArrayResult(null);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public static void lambda$readPacketFilter$7(WifiVendorHal wifiVendorHal, C5AnswerBox c5AnswerBox, WifiStatus wifiStatus, ArrayList arrayList) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c5AnswerBox.data = NativeUtil.byteArrayFromArrayList(arrayList);
        }
    }

    public boolean setCountryCodeHal(String str, String str2) {
        if (str2 != null && str2.length() == 2) {
            try {
                byte[] bArrStringToByteArray = NativeUtil.stringToByteArray(str2);
                synchronized (sLock) {
                    try {
                        try {
                            IWifiApIface apIface = getApIface(str);
                            if (apIface == null) {
                                return boolResult(false);
                            }
                            if (!ok(apIface.setCountryCode(bArrStringToByteArray))) {
                                return false;
                            }
                            return true;
                        } catch (Throwable th) {
                            throw th;
                        }
                    } catch (RemoteException e) {
                        handleRemoteException(e);
                        return false;
                    }
                }
            } catch (IllegalArgumentException e2) {
                return boolResult(false);
            }
        }
        return boolResult(false);
    }

    public boolean setLoggingEventHandler(WifiNative.WifiLoggerEventHandler wifiLoggerEventHandler) {
        if (wifiLoggerEventHandler == null) {
            return boolResult(false);
        }
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return boolResult(false);
            }
            if (this.mLogEventHandler != null) {
                return boolResult(false);
            }
            try {
                if (!ok(this.mIWifiChip.enableDebugErrorAlerts(true))) {
                    return false;
                }
                this.mLogEventHandler = wifiLoggerEventHandler;
                return true;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public boolean resetLogHandler() {
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return boolResult(false);
            }
            if (this.mLogEventHandler == null) {
                return boolResult(false);
            }
            try {
                if (!ok(this.mIWifiChip.enableDebugErrorAlerts(false))) {
                    return false;
                }
                if (!ok(this.mIWifiChip.stopLoggingToDebugRingBuffer())) {
                    return false;
                }
                this.mLogEventHandler = null;
                return true;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public boolean startLoggingRingBuffer(int i, int i2, int i3, int i4, String str) {
        enter("verboseLevel=%, flags=%, maxIntervalInSec=%, minDataSizeInBytes=%, ringName=%").c(i).c(i2).c(i3).c(i4).c(str).flush();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return boolResult(false);
            }
            try {
                return ok(this.mIWifiChip.startLoggingToDebugRingBuffer(str, i, i3, i4));
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public int getSupportedLoggerFeatureSet() {
        return -1;
    }

    public String getDriverVersion() {
        String str;
        synchronized (sLock) {
            if (this.mDriverDescription == null) {
                requestChipDebugInfo();
            }
            str = this.mDriverDescription;
        }
        return str;
    }

    public String getFirmwareVersion() {
        String str;
        synchronized (sLock) {
            if (this.mFirmwareDescription == null) {
                requestChipDebugInfo();
            }
            str = this.mFirmwareDescription;
        }
        return str;
    }

    private void requestChipDebugInfo() {
        this.mDriverDescription = null;
        this.mFirmwareDescription = null;
        try {
            if (this.mIWifiChip == null) {
                return;
            }
            this.mIWifiChip.requestChipDebugInfo(new IWifiChip.requestChipDebugInfoCallback() {
                @Override
                public final void onValues(WifiStatus wifiStatus, IWifiChip.ChipDebugInfo chipDebugInfo) {
                    WifiVendorHal.lambda$requestChipDebugInfo$8(this.f$0, wifiStatus, chipDebugInfo);
                }
            });
            this.mLog.info("Driver: % Firmware: %").c(this.mDriverDescription).c(this.mFirmwareDescription).flush();
        } catch (RemoteException e) {
            handleRemoteException(e);
        }
    }

    public static void lambda$requestChipDebugInfo$8(WifiVendorHal wifiVendorHal, WifiStatus wifiStatus, IWifiChip.ChipDebugInfo chipDebugInfo) {
        if (wifiVendorHal.ok(wifiStatus)) {
            wifiVendorHal.mDriverDescription = chipDebugInfo.driverDescription;
            wifiVendorHal.mFirmwareDescription = chipDebugInfo.firmwareDescription;
        }
    }

    private static WifiNative.RingBufferStatus ringBufferStatus(WifiDebugRingBufferStatus wifiDebugRingBufferStatus) {
        WifiNative.RingBufferStatus ringBufferStatus = new WifiNative.RingBufferStatus();
        ringBufferStatus.name = wifiDebugRingBufferStatus.ringName;
        ringBufferStatus.flag = frameworkRingBufferFlagsFromHal(wifiDebugRingBufferStatus.flags);
        ringBufferStatus.ringBufferId = wifiDebugRingBufferStatus.ringId;
        ringBufferStatus.ringBufferByteSize = wifiDebugRingBufferStatus.sizeInBytes;
        ringBufferStatus.verboseLevel = wifiDebugRingBufferStatus.verboseLevel;
        return ringBufferStatus;
    }

    private static int frameworkRingBufferFlagsFromHal(int i) {
        BitMask bitMask = new BitMask(i);
        int i2 = 1;
        if (!bitMask.testAndClear(1)) {
            i2 = 0;
        }
        if (bitMask.testAndClear(2)) {
            i2 |= 2;
        }
        if (bitMask.testAndClear(4)) {
            i2 |= 4;
        }
        if (bitMask.value != 0) {
            throw new IllegalArgumentException("Unknown WifiDebugRingBufferFlag " + bitMask.value);
        }
        return i2;
    }

    private static WifiNative.RingBufferStatus[] makeRingBufferStatusArray(ArrayList<WifiDebugRingBufferStatus> arrayList) {
        WifiNative.RingBufferStatus[] ringBufferStatusArr = new WifiNative.RingBufferStatus[arrayList.size()];
        Iterator<WifiDebugRingBufferStatus> it = arrayList.iterator();
        int i = 0;
        while (it.hasNext()) {
            ringBufferStatusArr[i] = ringBufferStatus(it.next());
            i++;
        }
        return ringBufferStatusArr;
    }

    class C6AnswerBox {
        public WifiNative.RingBufferStatus[] value = null;

        C6AnswerBox() {
        }
    }

    public WifiNative.RingBufferStatus[] getRingBufferStatus() {
        final C6AnswerBox c6AnswerBox = new C6AnswerBox();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.getDebugRingBuffersStatus(new IWifiChip.getDebugRingBuffersStatusCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        WifiVendorHal.lambda$getRingBufferStatus$9(this.f$0, c6AnswerBox, wifiStatus, arrayList);
                    }
                });
                return c6AnswerBox.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static void lambda$getRingBufferStatus$9(WifiVendorHal wifiVendorHal, C6AnswerBox c6AnswerBox, WifiStatus wifiStatus, ArrayList arrayList) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c6AnswerBox.value = makeRingBufferStatusArray(arrayList);
        }
    }

    public boolean getRingBufferData(String str) {
        enter("ringName %").c(str).flush();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return boolResult(false);
            }
            try {
                return ok(this.mIWifiChip.forceDumpToDebugRingBuffer(str));
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    class C7AnswerBox {
        public byte[] value;

        C7AnswerBox() {
        }
    }

    public byte[] getFwMemoryDump() {
        final C7AnswerBox c7AnswerBox = new C7AnswerBox();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.requestFirmwareDebugDump(new IWifiChip.requestFirmwareDebugDumpCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        WifiVendorHal.lambda$getFwMemoryDump$10(this.f$0, c7AnswerBox, wifiStatus, arrayList);
                    }
                });
                return c7AnswerBox.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static void lambda$getFwMemoryDump$10(WifiVendorHal wifiVendorHal, C7AnswerBox c7AnswerBox, WifiStatus wifiStatus, ArrayList arrayList) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c7AnswerBox.value = NativeUtil.byteArrayFromArrayList(arrayList);
        }
    }

    class C8AnswerBox {
        public byte[] value;

        C8AnswerBox() {
        }
    }

    public byte[] getDriverStateDump() {
        final C8AnswerBox c8AnswerBox = new C8AnswerBox();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.requestDriverDebugDump(new IWifiChip.requestDriverDebugDumpCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        WifiVendorHal.lambda$getDriverStateDump$11(this.f$0, c8AnswerBox, wifiStatus, arrayList);
                    }
                });
                return c8AnswerBox.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static void lambda$getDriverStateDump$11(WifiVendorHal wifiVendorHal, C8AnswerBox c8AnswerBox, WifiStatus wifiStatus, ArrayList arrayList) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c8AnswerBox.value = NativeUtil.byteArrayFromArrayList(arrayList);
        }
    }

    public boolean startPktFateMonitoring(String str) {
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                return ok(staIface.startDebugPacketFateMonitoring());
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    private byte halToFrameworkPktFateFrameType(int i) {
        switch (i) {
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 1;
            case 2:
                return (byte) 2;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    private byte halToFrameworkRxPktFate(int i) {
        switch (i) {
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 1;
            case 2:
                return (byte) 2;
            case 3:
                return (byte) 3;
            case 4:
                return (byte) 4;
            case 5:
                return (byte) 5;
            case 6:
                return (byte) 6;
            case 7:
                return (byte) 7;
            case 8:
                return (byte) 8;
            case 9:
                return (byte) 9;
            case 10:
                return (byte) 10;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    private byte halToFrameworkTxPktFate(int i) {
        switch (i) {
            case 0:
                return (byte) 0;
            case 1:
                return (byte) 1;
            case 2:
                return (byte) 2;
            case 3:
                return (byte) 3;
            case 4:
                return (byte) 4;
            case 5:
                return (byte) 5;
            case 6:
                return (byte) 6;
            case 7:
                return (byte) 7;
            case 8:
                return (byte) 8;
            case 9:
                return (byte) 9;
            default:
                throw new IllegalArgumentException("bad " + i);
        }
    }

    public boolean getTxPktFates(String str, final WifiNative.TxFateReport[] txFateReportArr) {
        if (ArrayUtils.isEmpty(txFateReportArr)) {
            return boolResult(false);
        }
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                staIface.getDebugTxPacketFates(new IWifiStaIface.getDebugTxPacketFatesCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        WifiVendorHal.lambda$getTxPktFates$12(this.f$0, txFateReportArr, mutableBoolean, wifiStatus, arrayList);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static void lambda$getTxPktFates$12(WifiVendorHal wifiVendorHal, WifiNative.TxFateReport[] txFateReportArr, MutableBoolean mutableBoolean, WifiStatus wifiStatus, ArrayList arrayList) {
        if (wifiVendorHal.ok(wifiStatus)) {
            int i = 0;
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                WifiDebugTxPacketFateReport wifiDebugTxPacketFateReport = (WifiDebugTxPacketFateReport) it.next();
                if (i >= txFateReportArr.length) {
                    break;
                }
                txFateReportArr[i] = new WifiNative.TxFateReport(wifiVendorHal.halToFrameworkTxPktFate(wifiDebugTxPacketFateReport.fate), wifiDebugTxPacketFateReport.frameInfo.driverTimestampUsec, wifiVendorHal.halToFrameworkPktFateFrameType(wifiDebugTxPacketFateReport.frameInfo.frameType), NativeUtil.byteArrayFromArrayList(wifiDebugTxPacketFateReport.frameInfo.frameContent));
                i++;
            }
            mutableBoolean.value = true;
        }
    }

    public boolean getRxPktFates(String str, final WifiNative.RxFateReport[] rxFateReportArr) {
        if (ArrayUtils.isEmpty(rxFateReportArr)) {
            return boolResult(false);
        }
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                staIface.getDebugRxPacketFates(new IWifiStaIface.getDebugRxPacketFatesCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, ArrayList arrayList) {
                        WifiVendorHal.lambda$getRxPktFates$13(this.f$0, rxFateReportArr, mutableBoolean, wifiStatus, arrayList);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static void lambda$getRxPktFates$13(WifiVendorHal wifiVendorHal, WifiNative.RxFateReport[] rxFateReportArr, MutableBoolean mutableBoolean, WifiStatus wifiStatus, ArrayList arrayList) {
        if (wifiVendorHal.ok(wifiStatus)) {
            int i = 0;
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                WifiDebugRxPacketFateReport wifiDebugRxPacketFateReport = (WifiDebugRxPacketFateReport) it.next();
                if (i >= rxFateReportArr.length) {
                    break;
                }
                rxFateReportArr[i] = new WifiNative.RxFateReport(wifiVendorHal.halToFrameworkRxPktFate(wifiDebugRxPacketFateReport.fate), wifiDebugRxPacketFateReport.frameInfo.driverTimestampUsec, wifiVendorHal.halToFrameworkPktFateFrameType(wifiDebugRxPacketFateReport.frameInfo.frameType), NativeUtil.byteArrayFromArrayList(wifiDebugRxPacketFateReport.frameInfo.frameContent));
                i++;
            }
            mutableBoolean.value = true;
        }
    }

    public int startSendingOffloadedPacket(String str, int i, byte[] bArr, byte[] bArr2, byte[] bArr3, int i2, int i3) {
        enter("slot=% periodInMs=%").c(i).c(i3).flush();
        ArrayList<Byte> arrayListByteArrayToArrayList = NativeUtil.byteArrayToArrayList(bArr3);
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return -1;
            }
            try {
                if (!ok(staIface.startSendingKeepAlivePackets(i, arrayListByteArrayToArrayList, (short) i2, bArr, bArr2, i3))) {
                    return -1;
                }
                return 0;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return -1;
            }
        }
    }

    public int stopSendingOffloadedPacket(String str, int i) {
        enter("slot=%").c(i).flush();
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return -1;
            }
            try {
                if (!ok(staIface.stopSendingKeepAlivePackets(i))) {
                    return -1;
                }
                return 0;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return -1;
            }
        }
    }

    public int startRssiMonitoring(String str, byte b, byte b2, WifiNative.WifiRssiEventHandler wifiRssiEventHandler) {
        enter("maxRssi=% minRssi=%").c(b).c(b2).flush();
        if (b <= b2 || wifiRssiEventHandler == null) {
            return -1;
        }
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return -1;
            }
            try {
                staIface.stopRssiMonitoring(sRssiMonCmdId);
                if (!ok(staIface.startRssiMonitoring(sRssiMonCmdId, b, b2))) {
                    return -1;
                }
                this.mWifiRssiEventHandler = wifiRssiEventHandler;
                return 0;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return -1;
            }
        }
    }

    public int stopRssiMonitoring(String str) {
        synchronized (sLock) {
            this.mWifiRssiEventHandler = null;
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return -1;
            }
            try {
                if (!ok(staIface.stopRssiMonitoring(sRssiMonCmdId))) {
                    return -1;
                }
                return 0;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return -1;
            }
        }
    }

    private static int[] intsFromArrayList(ArrayList<Integer> arrayList) {
        if (arrayList == null) {
            return null;
        }
        int[] iArr = new int[arrayList.size()];
        int i = 0;
        Iterator<Integer> it = arrayList.iterator();
        while (it.hasNext()) {
            iArr[i] = it.next().intValue();
            i++;
        }
        return iArr;
    }

    private static WifiWakeReasonAndCounts halToFrameworkWakeReasons(WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats) {
        if (wifiDebugHostWakeReasonStats == null) {
            return null;
        }
        WifiWakeReasonAndCounts wifiWakeReasonAndCounts = new WifiWakeReasonAndCounts();
        wifiWakeReasonAndCounts.totalCmdEventWake = wifiDebugHostWakeReasonStats.totalCmdEventWakeCnt;
        wifiWakeReasonAndCounts.totalDriverFwLocalWake = wifiDebugHostWakeReasonStats.totalDriverFwLocalWakeCnt;
        wifiWakeReasonAndCounts.totalRxDataWake = wifiDebugHostWakeReasonStats.totalRxPacketWakeCnt;
        wifiWakeReasonAndCounts.rxUnicast = wifiDebugHostWakeReasonStats.rxPktWakeDetails.rxUnicastCnt;
        wifiWakeReasonAndCounts.rxMulticast = wifiDebugHostWakeReasonStats.rxPktWakeDetails.rxMulticastCnt;
        wifiWakeReasonAndCounts.rxBroadcast = wifiDebugHostWakeReasonStats.rxPktWakeDetails.rxBroadcastCnt;
        wifiWakeReasonAndCounts.icmp = wifiDebugHostWakeReasonStats.rxIcmpPkWakeDetails.icmpPkt;
        wifiWakeReasonAndCounts.icmp6 = wifiDebugHostWakeReasonStats.rxIcmpPkWakeDetails.icmp6Pkt;
        wifiWakeReasonAndCounts.icmp6Ra = wifiDebugHostWakeReasonStats.rxIcmpPkWakeDetails.icmp6Ra;
        wifiWakeReasonAndCounts.icmp6Na = wifiDebugHostWakeReasonStats.rxIcmpPkWakeDetails.icmp6Na;
        wifiWakeReasonAndCounts.icmp6Ns = wifiDebugHostWakeReasonStats.rxIcmpPkWakeDetails.icmp6Ns;
        wifiWakeReasonAndCounts.ipv4RxMulticast = wifiDebugHostWakeReasonStats.rxMulticastPkWakeDetails.ipv4RxMulticastAddrCnt;
        wifiWakeReasonAndCounts.ipv6Multicast = wifiDebugHostWakeReasonStats.rxMulticastPkWakeDetails.ipv6RxMulticastAddrCnt;
        wifiWakeReasonAndCounts.otherRxMulticast = wifiDebugHostWakeReasonStats.rxMulticastPkWakeDetails.otherRxMulticastAddrCnt;
        wifiWakeReasonAndCounts.cmdEventWakeCntArray = intsFromArrayList(wifiDebugHostWakeReasonStats.cmdEventWakeCntPerType);
        wifiWakeReasonAndCounts.driverFWLocalWakeCntArray = intsFromArrayList(wifiDebugHostWakeReasonStats.driverFwLocalWakeCntPerType);
        return wifiWakeReasonAndCounts;
    }

    class C9AnswerBox {
        public WifiDebugHostWakeReasonStats value = null;

        C9AnswerBox() {
        }
    }

    public WifiWakeReasonAndCounts getWlanWakeReasonCount() {
        final C9AnswerBox c9AnswerBox = new C9AnswerBox();
        synchronized (sLock) {
            if (this.mIWifiChip == null) {
                return null;
            }
            try {
                this.mIWifiChip.getDebugHostWakeReasonStats(new IWifiChip.getDebugHostWakeReasonStatsCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats) {
                        WifiVendorHal.lambda$getWlanWakeReasonCount$14(this.f$0, c9AnswerBox, wifiStatus, wifiDebugHostWakeReasonStats);
                    }
                });
                return halToFrameworkWakeReasons(c9AnswerBox.value);
            } catch (RemoteException e) {
                handleRemoteException(e);
                return null;
            }
        }
    }

    public static void lambda$getWlanWakeReasonCount$14(WifiVendorHal wifiVendorHal, C9AnswerBox c9AnswerBox, WifiStatus wifiStatus, WifiDebugHostWakeReasonStats wifiDebugHostWakeReasonStats) {
        if (wifiVendorHal.ok(wifiStatus)) {
            c9AnswerBox.value = wifiDebugHostWakeReasonStats;
        }
    }

    public boolean configureNeighborDiscoveryOffload(String str, boolean z) {
        enter("enabled=%").c(z).flush();
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                if (!ok(staIface.enableNdOffload(z))) {
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public boolean getRoamingCapabilities(String str, final WifiNative.RoamingCapabilities roamingCapabilities) {
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                final MutableBoolean mutableBoolean = new MutableBoolean(false);
                staIface.getRoamingCapabilities(new IWifiStaIface.getRoamingCapabilitiesCallback() {
                    @Override
                    public final void onValues(WifiStatus wifiStatus, StaRoamingCapabilities staRoamingCapabilities) {
                        WifiVendorHal.lambda$getRoamingCapabilities$15(this.f$0, roamingCapabilities, mutableBoolean, wifiStatus, staRoamingCapabilities);
                    }
                });
                return mutableBoolean.value;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            }
        }
    }

    public static void lambda$getRoamingCapabilities$15(WifiVendorHal wifiVendorHal, WifiNative.RoamingCapabilities roamingCapabilities, MutableBoolean mutableBoolean, WifiStatus wifiStatus, StaRoamingCapabilities staRoamingCapabilities) {
        if (wifiVendorHal.ok(wifiStatus)) {
            roamingCapabilities.maxBlacklistSize = staRoamingCapabilities.maxBlacklistSize;
            roamingCapabilities.maxWhitelistSize = staRoamingCapabilities.maxWhitelistSize;
            mutableBoolean.value = true;
        }
    }

    public int enableFirmwareRoaming(String str, int i) {
        byte b;
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return 6;
            }
            try {
                switch (i) {
                    case 0:
                        b = 0;
                        break;
                    case 1:
                        b = 1;
                        break;
                    default:
                        this.mLog.err("enableFirmwareRoaming invalid argument %").c(i).flush();
                        return 7;
                }
                WifiStatus roamingState = staIface.setRoamingState(b);
                this.mVerboseLog.d("setRoamingState returned " + roamingState.code);
                return roamingState.code;
            } catch (RemoteException e) {
                handleRemoteException(e);
                return 9;
            }
        }
    }

    public boolean configureRoaming(String str, WifiNative.RoamingConfig roamingConfig) {
        synchronized (sLock) {
            android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
            if (staIface == null) {
                return boolResult(false);
            }
            try {
                StaRoamingConfig staRoamingConfig = new StaRoamingConfig();
                if (roamingConfig.blacklistBssids != null) {
                    Iterator<String> it = roamingConfig.blacklistBssids.iterator();
                    while (it.hasNext()) {
                        staRoamingConfig.bssidBlacklist.add(NativeUtil.macAddressToByteArray(it.next()));
                    }
                }
                if (roamingConfig.whitelistSsids != null) {
                    Iterator<String> it2 = roamingConfig.whitelistSsids.iterator();
                    while (it2.hasNext()) {
                        String strRemoveDoubleQuotes = WifiInfo.removeDoubleQuotes(it2.next());
                        int length = strRemoveDoubleQuotes.length();
                        if (length > 32) {
                            this.mLog.err("configureRoaming: skip invalid SSID %").r(strRemoveDoubleQuotes).flush();
                        } else {
                            byte[] bArr = new byte[length];
                            for (int i = 0; i < length; i++) {
                                bArr[i] = (byte) strRemoveDoubleQuotes.charAt(i);
                            }
                            staRoamingConfig.ssidWhitelist.add(bArr);
                        }
                    }
                }
                return ok(staIface.configureRoaming(staRoamingConfig));
            } catch (RemoteException e) {
                handleRemoteException(e);
                return false;
            } catch (IllegalArgumentException e2) {
                this.mLog.err("Illegal argument for roaming configuration").c(e2.toString()).flush();
                return false;
            }
        }
    }

    protected android.hardware.wifi.V1_1.IWifiChip getWifiChipForV1_1Mockable() {
        if (this.mIWifiChip == null) {
            return null;
        }
        return android.hardware.wifi.V1_1.IWifiChip.castFrom((IHwInterface) this.mIWifiChip);
    }

    protected android.hardware.wifi.V1_2.IWifiChip getWifiChipForV1_2Mockable() {
        if (this.mIWifiChip == null) {
            return null;
        }
        return android.hardware.wifi.V1_2.IWifiChip.castFrom((IHwInterface) this.mIWifiChip);
    }

    protected android.hardware.wifi.V1_2.IWifiStaIface getWifiStaIfaceForV1_2Mockable(String str) {
        android.hardware.wifi.V1_0.IWifiStaIface staIface = getStaIface(str);
        if (staIface == null) {
            return null;
        }
        return android.hardware.wifi.V1_2.IWifiStaIface.castFrom((IHwInterface) staIface);
    }

    private int frameworkToHalTxPowerScenario(int i) {
        if (i == 1) {
            return 0;
        }
        throw new IllegalArgumentException("bad scenario: " + i);
    }

    public boolean selectTxPowerScenario(int i) {
        WifiStatus wifiStatusSelectTxPowerScenario;
        synchronized (sLock) {
            try {
                try {
                    android.hardware.wifi.V1_1.IWifiChip wifiChipForV1_1Mockable = getWifiChipForV1_1Mockable();
                    if (wifiChipForV1_1Mockable == null) {
                        return boolResult(false);
                    }
                    if (i != 0) {
                        try {
                            wifiStatusSelectTxPowerScenario = wifiChipForV1_1Mockable.selectTxPowerScenario(frameworkToHalTxPowerScenario(i));
                        } catch (IllegalArgumentException e) {
                            this.mLog.err("Illegal argument for select tx power scenario").c(e.toString()).flush();
                            return false;
                        }
                    } else {
                        wifiStatusSelectTxPowerScenario = wifiChipForV1_1Mockable.resetTxPowerScenario();
                    }
                    if (!ok(wifiStatusSelectTxPowerScenario)) {
                        return false;
                    }
                    return true;
                } catch (RemoteException e2) {
                    handleRemoteException(e2);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static byte[] hidlIeArrayToFrameworkIeBlob(ArrayList<WifiInformationElement> arrayList) {
        if (arrayList == null || arrayList.isEmpty()) {
            return new byte[0];
        }
        ArrayList arrayList2 = new ArrayList();
        for (WifiInformationElement wifiInformationElement : arrayList) {
            arrayList2.add(Byte.valueOf(wifiInformationElement.id));
            arrayList2.addAll(wifiInformationElement.data);
        }
        return NativeUtil.byteArrayFromArrayList(arrayList2);
    }

    private static ScanResult hidlToFrameworkScanResult(StaScanResult staScanResult) {
        if (staScanResult == null) {
            return null;
        }
        ScanResult scanResult = new ScanResult();
        scanResult.SSID = NativeUtil.encodeSsid(staScanResult.ssid);
        scanResult.wifiSsid = WifiSsid.createFromByteArray(NativeUtil.byteArrayFromArrayList(staScanResult.ssid));
        scanResult.BSSID = NativeUtil.macAddressFromByteArray(staScanResult.bssid);
        scanResult.level = staScanResult.rssi;
        scanResult.frequency = staScanResult.frequency;
        scanResult.timestamp = staScanResult.timeStampInUs;
        return scanResult;
    }

    private static ScanResult[] hidlToFrameworkScanResults(ArrayList<StaScanResult> arrayList) {
        int i = 0;
        if (arrayList == null || arrayList.isEmpty()) {
            return new ScanResult[0];
        }
        ScanResult[] scanResultArr = new ScanResult[arrayList.size()];
        Iterator<StaScanResult> it = arrayList.iterator();
        while (it.hasNext()) {
            scanResultArr[i] = hidlToFrameworkScanResult(it.next());
            i++;
        }
        return scanResultArr;
    }

    private static int hidlToFrameworkScanDataFlags(int i) {
        if (i == 1) {
            return 1;
        }
        return 0;
    }

    private static WifiScanner.ScanData[] hidlToFrameworkScanDatas(int i, ArrayList<StaScanData> arrayList) {
        int i2 = 0;
        if (arrayList == null || arrayList.isEmpty()) {
            return new WifiScanner.ScanData[0];
        }
        WifiScanner.ScanData[] scanDataArr = new WifiScanner.ScanData[arrayList.size()];
        for (StaScanData staScanData : arrayList) {
            scanDataArr[i2] = new WifiScanner.ScanData(i, hidlToFrameworkScanDataFlags(staScanData.flags), staScanData.bucketsScanned, false, hidlToFrameworkScanResults(staScanData.results));
            i2++;
        }
        return scanDataArr;
    }

    private class StaIfaceEventCallback extends IWifiStaIfaceEventCallback.Stub {
        private StaIfaceEventCallback() {
        }

        @Override
        public void onBackgroundScanFailure(int i) {
            WifiVendorHal.this.mVerboseLog.d("onBackgroundScanFailure " + i);
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mScan != null && i == WifiVendorHal.this.mScan.cmdId) {
                    WifiVendorHal.this.mScan.eventHandler.onScanStatus(3);
                }
            }
        }

        @Override
        public void onBackgroundFullScanResult(int i, int i2, StaScanResult staScanResult) {
            WifiVendorHal.this.mVerboseLog.d("onBackgroundFullScanResult " + i);
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mScan != null && i == WifiVendorHal.this.mScan.cmdId) {
                    WifiVendorHal.this.mScan.eventHandler.onFullScanResult(WifiVendorHal.hidlToFrameworkScanResult(staScanResult), i2);
                }
            }
        }

        @Override
        public void onBackgroundScanResults(int i, ArrayList<StaScanData> arrayList) {
            WifiVendorHal.this.mVerboseLog.d("onBackgroundScanResults " + i);
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mScan != null && i == WifiVendorHal.this.mScan.cmdId) {
                    WifiNative.ScanEventHandler scanEventHandler = WifiVendorHal.this.mScan.eventHandler;
                    WifiVendorHal.this.mScan.latestScanResults = WifiVendorHal.hidlToFrameworkScanDatas(i, arrayList);
                    scanEventHandler.onScanStatus(0);
                }
            }
        }

        @Override
        public void onRssiThresholdBreached(int i, byte[] bArr, int i2) {
            WifiVendorHal.this.mVerboseLog.d("onRssiThresholdBreached " + i + "currRssi " + i2);
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mWifiRssiEventHandler != null && i == WifiVendorHal.sRssiMonCmdId) {
                    WifiVendorHal.this.mWifiRssiEventHandler.onRssiThresholdBreached((byte) i2);
                }
            }
        }
    }

    private class ChipEventCallback extends IWifiChipEventCallback.Stub {
        private ChipEventCallback() {
        }

        @Override
        public void onChipReconfigured(int i) {
            WifiVendorHal.this.mVerboseLog.d("onChipReconfigured " + i);
        }

        @Override
        public void onChipReconfigureFailure(WifiStatus wifiStatus) {
            WifiVendorHal.this.mVerboseLog.d("onChipReconfigureFailure " + wifiStatus);
        }

        @Override
        public void onIfaceAdded(int i, String str) {
            WifiVendorHal.this.mVerboseLog.d("onIfaceAdded " + i + ", name: " + str);
        }

        @Override
        public void onIfaceRemoved(int i, String str) {
            WifiVendorHal.this.mVerboseLog.d("onIfaceRemoved " + i + ", name: " + str);
        }

        @Override
        public void onDebugRingBufferDataAvailable(final WifiDebugRingBufferStatus wifiDebugRingBufferStatus, final ArrayList<Byte> arrayList) {
            WifiVendorHal.this.mHalEventHandler.post(new Runnable() {
                @Override
                public final void run() {
                    WifiVendorHal.ChipEventCallback.lambda$onDebugRingBufferDataAvailable$0(this.f$0, wifiDebugRingBufferStatus, arrayList);
                }
            });
        }

        public static void lambda$onDebugRingBufferDataAvailable$0(ChipEventCallback chipEventCallback, WifiDebugRingBufferStatus wifiDebugRingBufferStatus, ArrayList arrayList) {
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mLogEventHandler != null && wifiDebugRingBufferStatus != null && arrayList != null) {
                    WifiNative.WifiLoggerEventHandler wifiLoggerEventHandler = WifiVendorHal.this.mLogEventHandler;
                    int size = arrayList.size();
                    boolean z = true;
                    try {
                        wifiLoggerEventHandler.onRingBufferData(WifiVendorHal.ringBufferStatus(wifiDebugRingBufferStatus), NativeUtil.byteArrayFromArrayList(arrayList));
                        if (arrayList.size() == size) {
                            z = false;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                    }
                    if (z) {
                        Log.wtf("WifiVendorHal", "Conversion failure detected in onDebugRingBufferDataAvailable. The input ArrayList |data| is potentially corrupted. Starting size=" + size + ", final size=" + arrayList.size());
                    }
                }
            }
        }

        @Override
        public void onDebugErrorAlert(final int i, final ArrayList<Byte> arrayList) {
            WifiVendorHal.this.mLog.w("onDebugErrorAlert " + i);
            WifiVendorHal.this.mHalEventHandler.post(new Runnable() {
                @Override
                public final void run() {
                    WifiVendorHal.ChipEventCallback.lambda$onDebugErrorAlert$1(this.f$0, arrayList, i);
                }
            });
        }

        public static void lambda$onDebugErrorAlert$1(ChipEventCallback chipEventCallback, ArrayList arrayList, int i) {
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mLogEventHandler != null && arrayList != null) {
                    WifiVendorHal.this.mLogEventHandler.onWifiAlert(i, NativeUtil.byteArrayFromArrayList(arrayList));
                }
            }
        }
    }

    private class ChipEventCallbackV12 extends IWifiChipEventCallback.Stub {
        private ChipEventCallbackV12() {
        }

        @Override
        public void onChipReconfigured(int i) {
            WifiVendorHal.this.mIWifiChipEventCallback.onChipReconfigured(i);
        }

        @Override
        public void onChipReconfigureFailure(WifiStatus wifiStatus) {
            WifiVendorHal.this.mIWifiChipEventCallback.onChipReconfigureFailure(wifiStatus);
        }

        @Override
        public void onIfaceAdded(int i, String str) {
            WifiVendorHal.this.mIWifiChipEventCallback.onIfaceAdded(i, str);
        }

        @Override
        public void onIfaceRemoved(int i, String str) {
            WifiVendorHal.this.mIWifiChipEventCallback.onIfaceRemoved(i, str);
        }

        @Override
        public void onDebugRingBufferDataAvailable(WifiDebugRingBufferStatus wifiDebugRingBufferStatus, ArrayList<Byte> arrayList) {
            WifiVendorHal.this.mIWifiChipEventCallback.onDebugRingBufferDataAvailable(wifiDebugRingBufferStatus, arrayList);
        }

        @Override
        public void onDebugErrorAlert(int i, ArrayList<Byte> arrayList) {
            WifiVendorHal.this.mIWifiChipEventCallback.onDebugErrorAlert(i, arrayList);
        }

        private boolean areSameIfaceNames(List<IWifiChipEventCallback.IfaceInfo> list, List<IWifiChipEventCallback.IfaceInfo> list2) {
            return ((List) list.stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((IWifiChipEventCallback.IfaceInfo) obj).name;
                }
            }).collect(Collectors.toList())).containsAll((List) list2.stream().map(new Function() {
                @Override
                public final Object apply(Object obj) {
                    return ((IWifiChipEventCallback.IfaceInfo) obj).name;
                }
            }).collect(Collectors.toList()));
        }

        private boolean areSameIfaces(List<IWifiChipEventCallback.IfaceInfo> list, List<IWifiChipEventCallback.IfaceInfo> list2) {
            return list.containsAll(list2);
        }

        @Override
        public void onRadioModeChange(ArrayList<IWifiChipEventCallback.RadioModeInfo> arrayList) {
            WifiVendorHal.this.mVerboseLog.d("onRadioModeChange " + arrayList);
            synchronized (WifiVendorHal.sLock) {
                if (WifiVendorHal.this.mRadioModeChangeEventHandler != null && arrayList != null) {
                    WifiNative.VendorHalRadioModeChangeEventHandler vendorHalRadioModeChangeEventHandler = WifiVendorHal.this.mRadioModeChangeEventHandler;
                    if (arrayList.size() == 0 || arrayList.size() > 2) {
                        WifiVendorHal.this.mLog.e("Unexpected number of radio info in list " + arrayList.size());
                        return;
                    }
                    IWifiChipEventCallback.RadioModeInfo radioModeInfo = arrayList.get(0);
                    IWifiChipEventCallback.RadioModeInfo radioModeInfo2 = arrayList.size() == 2 ? arrayList.get(1) : null;
                    if (radioModeInfo2 != null && radioModeInfo.ifaceInfos.size() != radioModeInfo2.ifaceInfos.size()) {
                        WifiVendorHal.this.mLog.e("Unexpected number of iface info in list " + radioModeInfo.ifaceInfos.size() + ", " + radioModeInfo2.ifaceInfos.size());
                        return;
                    }
                    int size = radioModeInfo.ifaceInfos.size();
                    if (size == 0 || size > 2) {
                        WifiVendorHal.this.mLog.e("Unexpected number of iface info in list " + size);
                        return;
                    }
                    if (arrayList.size() == 2 && size == 1) {
                        if (areSameIfaceNames(radioModeInfo.ifaceInfos, radioModeInfo2.ifaceInfos)) {
                            WifiVendorHal.this.mLog.e("Unexpected for both radio infos to have same iface");
                            return;
                        } else if (radioModeInfo.bandInfo != radioModeInfo2.bandInfo) {
                            vendorHalRadioModeChangeEventHandler.onDbs();
                            return;
                        } else {
                            vendorHalRadioModeChangeEventHandler.onSbs(radioModeInfo.bandInfo);
                            return;
                        }
                    }
                    if (arrayList.size() == 1 && size == 2) {
                        if (radioModeInfo.ifaceInfos.get(0).channel != radioModeInfo.ifaceInfos.get(1).channel) {
                            vendorHalRadioModeChangeEventHandler.onMcc(radioModeInfo.bandInfo);
                        } else {
                            vendorHalRadioModeChangeEventHandler.onScc(radioModeInfo.bandInfo);
                        }
                    }
                }
            }
        }
    }

    public class HalDeviceManagerStatusListener implements HalDeviceManager.ManagerStatusListener {
        public HalDeviceManagerStatusListener() {
        }

        @Override
        public void onStatusChanged() {
            WifiNative.VendorHalDeathEventHandler vendorHalDeathEventHandler;
            boolean zIsReady = WifiVendorHal.this.mHalDeviceManager.isReady();
            boolean zIsStarted = WifiVendorHal.this.mHalDeviceManager.isStarted();
            WifiVendorHal.this.mVerboseLog.i("Device Manager onStatusChanged. isReady(): " + zIsReady + ", isStarted(): " + zIsStarted);
            if (!zIsReady) {
                synchronized (WifiVendorHal.sLock) {
                    WifiVendorHal.this.clearState();
                    vendorHalDeathEventHandler = WifiVendorHal.this.mDeathEventHandler;
                }
                if (vendorHalDeathEventHandler != null) {
                    vendorHalDeathEventHandler.onDeath();
                }
            }
        }
    }
}
