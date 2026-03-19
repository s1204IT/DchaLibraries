package com.android.server.wifi.rtt;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.wifi.V1_0.RttResult;
import android.location.LocationManager;
import android.net.MacAddress;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.IWifiAwareManager;
import android.net.wifi.rtt.IRttCallback;
import android.net.wifi.rtt.IWifiRttManager;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.ResponderConfig;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.Clock;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.rtt.RttServiceImpl;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class RttServiceImpl extends IWifiRttManager.Stub {
    private static final int CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_DEFAULT = 0;
    private static final String CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME = "override_assume_no_privilege";
    private static final int CONVERSION_US_TO_MS = 1000;
    private static final long DEFAULT_BACKGROUND_PROCESS_EXEC_GAP_MS = 1800000;
    private static final long HAL_RANGING_TIMEOUT_MS = 5000;
    static final String HAL_RANGING_TIMEOUT_TAG = "RttServiceImpl HAL Ranging Timeout";
    static final int MAX_QUEUED_PER_UID = 20;
    private static final String TAG = "RttServiceImpl";
    private static final boolean VDBG = false;
    private ActivityManager mActivityManager;
    private IWifiAwareManager mAwareBinder;
    private long mBackgroundProcessExecGapMs;
    private Clock mClock;
    private final Context mContext;
    private FrameworkFacade mFrameworkFacade;
    private LocationManager mLocationManager;
    private PowerManager mPowerManager;
    private RttMetrics mRttMetrics;
    private RttNative mRttNative;
    private RttServiceSynchronized mRttServiceSynchronized;
    private WifiPermissionsUtil mWifiPermissionsUtil;
    private boolean mDbg = false;
    private final RttShellCommand mShellCommand = new RttShellCommand();

    public RttServiceImpl(Context context) {
        this.mContext = context;
        this.mShellCommand.reset();
    }

    private class RttShellCommand extends ShellCommand {
        private Map<String, Integer> mControlParams;

        private RttShellCommand() {
            this.mControlParams = new HashMap();
        }

        public int onCommand(String str) {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 0) {
                throw new SecurityException("Uid " + callingUid + " does not have access to wifirtt commands");
            }
            PrintWriter errPrintWriter = getErrPrintWriter();
            try {
                if ("reset".equals(str)) {
                    reset();
                    return 0;
                }
                if ("get".equals(str)) {
                    String nextArgRequired = getNextArgRequired();
                    if (!this.mControlParams.containsKey(nextArgRequired)) {
                        errPrintWriter.println("Unknown parameter name -- '" + nextArgRequired + "'");
                        return -1;
                    }
                    getOutPrintWriter().println(this.mControlParams.get(nextArgRequired));
                    return 0;
                }
                if ("set".equals(str)) {
                    String nextArgRequired2 = getNextArgRequired();
                    String nextArgRequired3 = getNextArgRequired();
                    if (!this.mControlParams.containsKey(nextArgRequired2)) {
                        errPrintWriter.println("Unknown parameter name -- '" + nextArgRequired2 + "'");
                        return -1;
                    }
                    try {
                        this.mControlParams.put(nextArgRequired2, Integer.valueOf(nextArgRequired3));
                        return 0;
                    } catch (NumberFormatException e) {
                        errPrintWriter.println("Can't convert value to integer -- '" + nextArgRequired3 + "'");
                        return -1;
                    }
                }
                handleDefaultCommands(str);
            } catch (Exception e2) {
                errPrintWriter.println("Exception: " + e2);
            }
            return -1;
        }

        public void onHelp() {
            PrintWriter outPrintWriter = getOutPrintWriter();
            outPrintWriter.println("Wi-Fi RTT (wifirt) commands:");
            outPrintWriter.println("  help");
            outPrintWriter.println("    Print this help text.");
            outPrintWriter.println("  reset");
            outPrintWriter.println("    Reset parameters to default values.");
            outPrintWriter.println("  get <name>");
            outPrintWriter.println("    Get the value of the control parameter.");
            outPrintWriter.println("  set <name> <value>");
            outPrintWriter.println("    Set the value of the control parameter.");
            outPrintWriter.println("  Control parameters:");
            Iterator<String> it = this.mControlParams.keySet().iterator();
            while (it.hasNext()) {
                outPrintWriter.println("    " + it.next());
            }
            outPrintWriter.println();
        }

        public int getControlParam(String str) {
            if (this.mControlParams.containsKey(str)) {
                return this.mControlParams.get(str).intValue();
            }
            Log.wtf(RttServiceImpl.TAG, "getControlParam for unknown variable: " + str);
            return 0;
        }

        public void reset() {
            this.mControlParams.put(RttServiceImpl.CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME, 0);
        }
    }

    public void start(Looper looper, Clock clock, IWifiAwareManager iWifiAwareManager, final RttNative rttNative, RttMetrics rttMetrics, WifiPermissionsUtil wifiPermissionsUtil, final FrameworkFacade frameworkFacade) {
        this.mClock = clock;
        this.mAwareBinder = iWifiAwareManager;
        this.mRttNative = rttNative;
        this.mRttMetrics = rttMetrics;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mFrameworkFacade = frameworkFacade;
        this.mRttServiceSynchronized = new RttServiceSynchronized(looper, rttNative);
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (RttServiceImpl.this.mDbg) {
                    Log.v(RttServiceImpl.TAG, "BroadcastReceiver: action=" + action);
                }
                if ("android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action)) {
                    if (RttServiceImpl.this.mPowerManager.isDeviceIdleMode()) {
                        RttServiceImpl.this.disable();
                    } else {
                        RttServiceImpl.this.enableIfPossible();
                    }
                }
            }
        }, intentFilter);
        frameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_verbose_logging_enabled"), true, new ContentObserver(this.mRttServiceSynchronized.mHandler) {
            @Override
            public void onChange(boolean z) {
                RttServiceImpl.this.enableVerboseLogging(frameworkFacade.getIntegerSetting(RttServiceImpl.this.mContext, "wifi_verbose_logging_enabled", 0));
            }
        });
        enableVerboseLogging(frameworkFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0));
        frameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_rtt_background_exec_gap_ms"), true, new ContentObserver(this.mRttServiceSynchronized.mHandler) {
            @Override
            public void onChange(boolean z) {
                RttServiceImpl.this.updateBackgroundThrottlingInterval(frameworkFacade);
            }
        });
        updateBackgroundThrottlingInterval(frameworkFacade);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (RttServiceImpl.this.mDbg) {
                    Log.v(RttServiceImpl.TAG, "onReceive: MODE_CHANGED_ACTION: intent=" + intent);
                }
                if (RttServiceImpl.this.mLocationManager.isLocationEnabled()) {
                    RttServiceImpl.this.enableIfPossible();
                } else {
                    RttServiceImpl.this.disable();
                }
            }
        }, intentFilter2);
        this.mRttServiceSynchronized.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                rttNative.start(this.f$0.mRttServiceSynchronized.mHandler);
            }
        });
    }

    private void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mDbg = true;
        } else {
            this.mDbg = false;
        }
        this.mRttNative.mDbg = this.mDbg;
        this.mRttMetrics.mDbg = this.mDbg;
    }

    private void updateBackgroundThrottlingInterval(FrameworkFacade frameworkFacade) {
        this.mBackgroundProcessExecGapMs = frameworkFacade.getLongSetting(this.mContext, "wifi_rtt_background_exec_gap_ms", DEFAULT_BACKGROUND_PROCESS_EXEC_GAP_MS);
    }

    public int getMockableCallingUid() {
        return getCallingUid();
    }

    public void enableIfPossible() {
        if (!isAvailable()) {
            return;
        }
        sendRttStateChangedBroadcast(true);
        this.mRttServiceSynchronized.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mRttServiceSynchronized.executeNextRangingRequestIfPossible(false);
            }
        });
    }

    public void disable() {
        sendRttStateChangedBroadcast(false);
        this.mRttServiceSynchronized.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mRttServiceSynchronized.cleanUpOnDisable();
            }
        });
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        this.mShellCommand.exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    public boolean isAvailable() {
        return this.mRttNative.isReady() && !this.mPowerManager.isDeviceIdleMode() && this.mLocationManager.isLocationEnabled();
    }

    public void startRanging(final IBinder iBinder, final String str, final WorkSource workSource, final RangingRequest rangingRequest, final IRttCallback iRttCallback) throws RemoteException {
        if (iBinder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        }
        if (rangingRequest == null || rangingRequest.mRttPeers == null || rangingRequest.mRttPeers.size() == 0) {
            throw new IllegalArgumentException("Request must not be null or empty");
        }
        Iterator it = rangingRequest.mRttPeers.iterator();
        while (it.hasNext()) {
            if (((ResponderConfig) it.next()) == null) {
                throw new IllegalArgumentException("Request must not contain null Responders");
            }
        }
        if (iRttCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        rangingRequest.enforceValidity(this.mAwareBinder != null);
        if (!isAvailable()) {
            try {
                this.mRttMetrics.recordOverallStatus(3);
                iRttCallback.onRangingFailure(2);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "startRanging: disabled, callback failed -- " + e);
                return;
            }
        }
        final int mockableCallingUid = getMockableCallingUid();
        enforceAccessPermission();
        enforceChangePermission();
        this.mWifiPermissionsUtil.enforceFineLocationPermission(str, mockableCallingUid);
        if (workSource != null) {
            enforceLocationHardware();
            workSource.clearNames();
        }
        final boolean z = checkLocationHardware() && this.mShellCommand.getControlParam(CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME) == 0;
        final AnonymousClass5 anonymousClass5 = new AnonymousClass5(mockableCallingUid, iBinder);
        try {
            iBinder.linkToDeath(anonymousClass5, 0);
            this.mRttServiceSynchronized.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    RttServiceImpl.lambda$startRanging$3(this.f$0, workSource, mockableCallingUid, iBinder, anonymousClass5, str, rangingRequest, iRttCallback, z);
                }
            });
        } catch (RemoteException e2) {
            Log.e(TAG, "Error on linkToDeath - " + e2);
        }
    }

    class AnonymousClass5 implements IBinder.DeathRecipient {
        final IBinder val$binder;
        final int val$uid;

        AnonymousClass5(int i, IBinder iBinder) {
            this.val$uid = i;
            this.val$binder = iBinder;
        }

        @Override
        public void binderDied() {
            if (RttServiceImpl.this.mDbg) {
                Log.v(RttServiceImpl.TAG, "binderDied: uid=" + this.val$uid);
            }
            this.val$binder.unlinkToDeath(this, 0);
            Handler handler = RttServiceImpl.this.mRttServiceSynchronized.mHandler;
            final int i = this.val$uid;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    RttServiceImpl.this.mRttServiceSynchronized.cleanUpClientRequests(i, null);
                }
            });
        }
    }

    public static void lambda$startRanging$3(RttServiceImpl rttServiceImpl, WorkSource workSource, int i, IBinder iBinder, IBinder.DeathRecipient deathRecipient, String str, RangingRequest rangingRequest, IRttCallback iRttCallback, boolean z) {
        int i2;
        WorkSource workSource2;
        if (workSource == null || workSource.isEmpty()) {
            i2 = i;
            workSource2 = new WorkSource(i2);
        } else {
            workSource2 = workSource;
            i2 = i;
        }
        rttServiceImpl.mRttServiceSynchronized.queueRangingRequest(i2, workSource2, iBinder, deathRecipient, str, rangingRequest, iRttCallback, z);
    }

    public void cancelRanging(final WorkSource workSource) throws RemoteException {
        enforceLocationHardware();
        if (workSource != null) {
            workSource.clearNames();
        }
        if (workSource == null || workSource.isEmpty()) {
            Log.e(TAG, "cancelRanging: invalid work-source -- " + workSource);
            return;
        }
        this.mRttServiceSynchronized.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mRttServiceSynchronized.cleanUpClientRequests(0, workSource);
            }
        });
    }

    public void onRangingResults(final int i, final List<RttResult> list) {
        this.mRttServiceSynchronized.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mRttServiceSynchronized.onRangingResults(i, list);
            }
        });
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceLocationHardware() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", TAG);
    }

    private boolean checkLocationHardware() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.LOCATION_HARDWARE") == 0;
    }

    private void sendRttStateChangedBroadcast(boolean z) {
        Intent intent = new Intent("android.net.wifi.rtt.action.WIFI_RTT_STATE_CHANGED");
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump RttService from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        printWriter.println("Wi-Fi RTT Service");
        this.mRttServiceSynchronized.dump(fileDescriptor, printWriter, strArr);
    }

    private class RttServiceSynchronized {
        public Handler mHandler;
        private WakeupMessage mRangingTimeoutMessage;
        private RttNative mRttNative;
        private int mNextCommandId = RttServiceImpl.CONVERSION_US_TO_MS;
        private Map<Integer, RttRequesterInfo> mRttRequesterInfo = new HashMap();
        private List<RttRequestInfo> mRttRequestQueue = new LinkedList();

        RttServiceSynchronized(Looper looper, RttNative rttNative) {
            this.mRangingTimeoutMessage = null;
            this.mRttNative = rttNative;
            this.mHandler = new Handler(looper);
            this.mRangingTimeoutMessage = new WakeupMessage(RttServiceImpl.this.mContext, this.mHandler, RttServiceImpl.HAL_RANGING_TIMEOUT_TAG, new Runnable() {
                @Override
                public final void run() {
                    this.f$0.timeoutRangingRequest();
                }
            });
        }

        private void cancelRanging(RttRequestInfo rttRequestInfo) {
            ArrayList<byte[]> arrayList = new ArrayList<>();
            Iterator it = rttRequestInfo.request.mRttPeers.iterator();
            while (it.hasNext()) {
                arrayList.add(((ResponderConfig) it.next()).macAddress.toByteArray());
            }
            this.mRttNative.rangeCancel(rttRequestInfo.cmdId, arrayList);
        }

        private void cleanUpOnDisable() {
            for (RttRequestInfo rttRequestInfo : this.mRttRequestQueue) {
                try {
                    if (rttRequestInfo.dispatchedToNative) {
                        cancelRanging(rttRequestInfo);
                    }
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(3);
                    rttRequestInfo.callback.onRangingFailure(2);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: disabled, callback failed -- " + e);
                }
                rttRequestInfo.binder.unlinkToDeath(rttRequestInfo.dr, 0);
            }
            this.mRttRequestQueue.clear();
            this.mRangingTimeoutMessage.cancel();
        }

        private void cleanUpClientRequests(int i, WorkSource workSource) {
            ListIterator<RttRequestInfo> listIterator = this.mRttRequestQueue.listIterator();
            boolean z = false;
            while (listIterator.hasNext()) {
                RttRequestInfo next = listIterator.next();
                boolean z2 = next.uid == i;
                if (next.workSource != null && workSource != null) {
                    next.workSource.remove(workSource);
                    if (next.workSource.isEmpty()) {
                        z2 = true;
                    }
                }
                if (z2) {
                    if (!next.dispatchedToNative) {
                        listIterator.remove();
                        next.binder.unlinkToDeath(next.dr, 0);
                    } else {
                        Log.d(RttServiceImpl.TAG, "Client death - cancelling RTT operation in progress: cmdId=" + next.cmdId);
                        this.mRangingTimeoutMessage.cancel();
                        cancelRanging(next);
                        z = true;
                    }
                }
            }
            if (z) {
                executeNextRangingRequestIfPossible(true);
            }
        }

        private void timeoutRangingRequest() {
            if (this.mRttRequestQueue.size() == 0) {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: but nothing in queue!?");
                return;
            }
            RttRequestInfo rttRequestInfo = this.mRttRequestQueue.get(0);
            if (!rttRequestInfo.dispatchedToNative) {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: command not dispatched to native!?");
                return;
            }
            cancelRanging(rttRequestInfo);
            try {
                RttServiceImpl.this.mRttMetrics.recordOverallStatus(4);
                rttRequestInfo.callback.onRangingFailure(1);
            } catch (RemoteException e) {
                Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: callback failed: " + e);
            }
            executeNextRangingRequestIfPossible(true);
        }

        private void queueRangingRequest(int i, WorkSource workSource, IBinder iBinder, IBinder.DeathRecipient deathRecipient, String str, RangingRequest rangingRequest, IRttCallback iRttCallback, boolean z) {
            RttServiceImpl.this.mRttMetrics.recordRequest(workSource, rangingRequest);
            if (isRequestorSpamming(workSource)) {
                Log.w(RttServiceImpl.TAG, "Work source " + workSource + " is spamming, dropping request: " + rangingRequest);
                iBinder.unlinkToDeath(deathRecipient, 0);
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(5);
                    iRttCallback.onRangingFailure(1);
                    return;
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.queueRangingRequest: spamming, callback failed -- " + e);
                    return;
                }
            }
            RttRequestInfo rttRequestInfo = new RttRequestInfo();
            rttRequestInfo.uid = i;
            rttRequestInfo.workSource = workSource;
            rttRequestInfo.binder = iBinder;
            rttRequestInfo.dr = deathRecipient;
            rttRequestInfo.callingPackage = str;
            rttRequestInfo.request = rangingRequest;
            rttRequestInfo.callback = iRttCallback;
            rttRequestInfo.isCalledFromPrivilegedContext = z;
            this.mRttRequestQueue.add(rttRequestInfo);
            executeNextRangingRequestIfPossible(false);
        }

        private boolean isRequestorSpamming(WorkSource workSource) {
            SparseIntArray sparseIntArray = new SparseIntArray();
            Iterator<RttRequestInfo> it = this.mRttRequestQueue.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                RttRequestInfo next = it.next();
                for (int i = 0; i < next.workSource.size(); i++) {
                    int i2 = next.workSource.get(i);
                    sparseIntArray.put(i2, sparseIntArray.get(i2) + 1);
                }
                ArrayList workChains = next.workSource.getWorkChains();
                if (workChains != null) {
                    for (int i3 = 0; i3 < workChains.size(); i3++) {
                        int attributionUid = ((WorkSource.WorkChain) workChains.get(i3)).getAttributionUid();
                        sparseIntArray.put(attributionUid, sparseIntArray.get(attributionUid) + 1);
                    }
                }
            }
            for (int i4 = 0; i4 < workSource.size(); i4++) {
                if (sparseIntArray.get(workSource.get(i4)) < 20) {
                    return false;
                }
            }
            ArrayList workChains2 = workSource.getWorkChains();
            if (workChains2 != null) {
                for (int i5 = 0; i5 < workChains2.size(); i5++) {
                    if (sparseIntArray.get(((WorkSource.WorkChain) workChains2.get(i5)).getAttributionUid()) < 20) {
                        return false;
                    }
                }
            }
            if (RttServiceImpl.this.mDbg) {
                Log.v(RttServiceImpl.TAG, "isRequestorSpamming: ws=" + workSource + ", someone is spamming: " + sparseIntArray);
            }
            return true;
        }

        private void executeNextRangingRequestIfPossible(boolean z) {
            if (z) {
                if (this.mRttRequestQueue.size() != 0) {
                    RttRequestInfo rttRequestInfoRemove = this.mRttRequestQueue.remove(0);
                    rttRequestInfoRemove.binder.unlinkToDeath(rttRequestInfoRemove.dr, 0);
                } else {
                    Log.w(RttServiceImpl.TAG, "executeNextRangingRequestIfPossible: pop requested - but empty queue!? Ignoring pop.");
                }
            }
            if (this.mRttRequestQueue.size() != 0) {
                RttRequestInfo rttRequestInfo = this.mRttRequestQueue.get(0);
                if (rttRequestInfo.peerHandlesTranslated || rttRequestInfo.dispatchedToNative) {
                    return;
                }
                startRanging(rttRequestInfo);
            }
        }

        private void startRanging(RttRequestInfo rttRequestInfo) {
            if (!RttServiceImpl.this.isAvailable()) {
                Log.d(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: disabled");
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(3);
                    rttRequestInfo.callback.onRangingFailure(2);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: disabled, callback failed -- " + e);
                    executeNextRangingRequestIfPossible(true);
                    return;
                }
            }
            if (processAwarePeerHandles(rttRequestInfo)) {
                return;
            }
            if (!preExecThrottleCheck(rttRequestInfo.workSource)) {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: execution throttled - nextRequest=" + rttRequestInfo + ", mRttRequesterInfo=" + this.mRttRequesterInfo);
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(5);
                    rttRequestInfo.callback.onRangingFailure(1);
                } catch (RemoteException e2) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: throttled, callback failed -- " + e2);
                }
                executeNextRangingRequestIfPossible(true);
                return;
            }
            int i = this.mNextCommandId;
            this.mNextCommandId = i + 1;
            rttRequestInfo.cmdId = i;
            if (this.mRttNative.rangeRequest(rttRequestInfo.cmdId, rttRequestInfo.request, rttRequestInfo.isCalledFromPrivilegedContext)) {
                this.mRangingTimeoutMessage.schedule(RttServiceImpl.this.mClock.getElapsedSinceBootMillis() + RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
            } else {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: native rangeRequest call failed");
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(6);
                    rttRequestInfo.callback.onRangingFailure(1);
                } catch (RemoteException e3) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: HAL request failed, callback failed -- " + e3);
                }
                executeNextRangingRequestIfPossible(true);
            }
            rttRequestInfo.dispatchedToNative = true;
        }

        private boolean preExecThrottleCheck(WorkSource workSource) {
            boolean z;
            boolean z2;
            boolean z3;
            int i = 0;
            while (true) {
                z = true;
                if (i < workSource.size()) {
                    if (RttServiceImpl.this.mActivityManager.getUidImportance(workSource.get(i)) > 125) {
                        i++;
                    } else {
                        z2 = false;
                        break;
                    }
                } else {
                    z2 = true;
                    break;
                }
            }
            ArrayList workChains = workSource.getWorkChains();
            if (z2 && workChains != null) {
                int i2 = 0;
                while (true) {
                    if (i2 >= workChains.size()) {
                        break;
                    }
                    if (RttServiceImpl.this.mActivityManager.getUidImportance(((WorkSource.WorkChain) workChains.get(i2)).getAttributionUid()) > 125) {
                        i2++;
                    } else {
                        z2 = false;
                        break;
                    }
                }
            }
            long elapsedSinceBootMillis = RttServiceImpl.this.mClock.getElapsedSinceBootMillis() - RttServiceImpl.this.mBackgroundProcessExecGapMs;
            if (z2) {
                for (int i3 = 0; i3 < workSource.size(); i3++) {
                    RttRequesterInfo rttRequesterInfo = this.mRttRequesterInfo.get(Integer.valueOf(workSource.get(i3)));
                    if (rttRequesterInfo == null || rttRequesterInfo.lastRangingExecuted < elapsedSinceBootMillis) {
                        z3 = true;
                        break;
                    }
                }
                z3 = false;
                if ((workChains != null) & (!z3)) {
                    for (int i4 = 0; i4 < workChains.size(); i4++) {
                        RttRequesterInfo rttRequesterInfo2 = this.mRttRequesterInfo.get(Integer.valueOf(((WorkSource.WorkChain) workChains.get(i4)).getAttributionUid()));
                        if (rttRequesterInfo2 == null || rttRequesterInfo2.lastRangingExecuted < elapsedSinceBootMillis) {
                            break;
                        }
                    }
                    z = z3;
                } else {
                    z = z3;
                }
            }
            if (z) {
                int i5 = 0;
                while (true) {
                    if (i5 >= workSource.size()) {
                        break;
                    }
                    RttRequesterInfo rttRequesterInfo3 = this.mRttRequesterInfo.get(Integer.valueOf(workSource.get(i5)));
                    if (rttRequesterInfo3 == null) {
                        rttRequesterInfo3 = new RttRequesterInfo();
                        this.mRttRequesterInfo.put(Integer.valueOf(workSource.get(i5)), rttRequesterInfo3);
                    }
                    rttRequesterInfo3.lastRangingExecuted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis();
                    i5++;
                }
                if (workChains != null) {
                    for (int i6 = 0; i6 < workChains.size(); i6++) {
                        WorkSource.WorkChain workChain = (WorkSource.WorkChain) workChains.get(i6);
                        RttRequesterInfo rttRequesterInfo4 = this.mRttRequesterInfo.get(Integer.valueOf(workChain.getAttributionUid()));
                        if (rttRequesterInfo4 == null) {
                            rttRequesterInfo4 = new RttRequesterInfo();
                            this.mRttRequesterInfo.put(Integer.valueOf(workChain.getAttributionUid()), rttRequesterInfo4);
                        }
                        rttRequesterInfo4.lastRangingExecuted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis();
                    }
                }
            }
            return z;
        }

        private boolean processAwarePeerHandles(RttRequestInfo rttRequestInfo) {
            ArrayList arrayList = new ArrayList();
            for (ResponderConfig responderConfig : rttRequestInfo.request.mRttPeers) {
                if (responderConfig.peerHandle != null && responderConfig.macAddress == null) {
                    arrayList.add(Integer.valueOf(responderConfig.peerHandle.peerId));
                }
            }
            if (arrayList.size() == 0) {
                return false;
            }
            if (rttRequestInfo.peerHandlesTranslated) {
                Log.w(RttServiceImpl.TAG, "processAwarePeerHandles: request=" + rttRequestInfo + ": PeerHandles translated - but information still missing!?");
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(7);
                    rttRequestInfo.callback.onRangingFailure(1);
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "processAwarePeerHandles: onRangingResults failure -- " + e);
                }
                executeNextRangingRequestIfPossible(true);
                return true;
            }
            rttRequestInfo.peerHandlesTranslated = true;
            try {
                RttServiceImpl.this.mAwareBinder.requestMacAddresses(rttRequestInfo.uid, arrayList, new AnonymousClass1(rttRequestInfo));
                return true;
            } catch (RemoteException e2) {
                Log.e(RttServiceImpl.TAG, "processAwarePeerHandles: exception while calling requestMacAddresses -- " + e2 + ", aborting request=" + rttRequestInfo);
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(7);
                    rttRequestInfo.callback.onRangingFailure(1);
                } catch (RemoteException e3) {
                    Log.e(RttServiceImpl.TAG, "processAwarePeerHandles: onRangingResults failure -- " + e3);
                }
                executeNextRangingRequestIfPossible(true);
                return true;
            }
        }

        class AnonymousClass1 extends IWifiAwareMacAddressProvider.Stub {
            final RttRequestInfo val$request;

            AnonymousClass1(RttRequestInfo rttRequestInfo) {
                this.val$request = rttRequestInfo;
            }

            public void macAddress(final Map map) {
                Handler handler = RttServiceSynchronized.this.mHandler;
                final RttRequestInfo rttRequestInfo = this.val$request;
                handler.post(new Runnable() {
                    @Override
                    public final void run() {
                        RttServiceImpl.RttServiceSynchronized.this.processReceivedAwarePeerMacAddresses(rttRequestInfo, map);
                    }
                });
            }
        }

        private void processReceivedAwarePeerMacAddresses(RttRequestInfo rttRequestInfo, Map<Integer, byte[]> map) {
            RangingRequest.Builder builder = new RangingRequest.Builder();
            for (ResponderConfig responderConfig : rttRequestInfo.request.mRttPeers) {
                if (responderConfig.peerHandle == null || responderConfig.macAddress != null) {
                    builder.addResponder(responderConfig);
                } else {
                    builder.addResponder(new ResponderConfig(MacAddress.fromBytes(map.get(Integer.valueOf(responderConfig.peerHandle.peerId))), responderConfig.peerHandle, responderConfig.responderType, responderConfig.supports80211mc, responderConfig.channelWidth, responderConfig.frequency, responderConfig.centerFreq0, responderConfig.centerFreq1, responderConfig.preamble));
                }
            }
            rttRequestInfo.request = builder.build();
            startRanging(rttRequestInfo);
        }

        private void onRangingResults(int i, List<RttResult> list) {
            if (this.mRttRequestQueue.size() == 0) {
                Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: no current RTT request pending!?");
                return;
            }
            this.mRangingTimeoutMessage.cancel();
            boolean z = false;
            RttRequestInfo rttRequestInfo = this.mRttRequestQueue.get(0);
            if (rttRequestInfo.cmdId == i) {
                if (RttServiceImpl.this.mWifiPermissionsUtil.checkCallersLocationPermission(rttRequestInfo.callingPackage, rttRequestInfo.uid) && RttServiceImpl.this.mLocationManager.isLocationEnabled()) {
                    z = true;
                }
                try {
                    if (z) {
                        List<RangingResult> listPostProcessResults = postProcessResults(rttRequestInfo.request, list, rttRequestInfo.isCalledFromPrivilegedContext);
                        RttServiceImpl.this.mRttMetrics.recordOverallStatus(1);
                        RttServiceImpl.this.mRttMetrics.recordResult(rttRequestInfo.request, list);
                        rttRequestInfo.callback.onRangingResults(listPostProcessResults);
                    } else {
                        Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: location permission revoked - not forwarding results");
                        RttServiceImpl.this.mRttMetrics.recordOverallStatus(8);
                        rttRequestInfo.callback.onRangingFailure(1);
                    }
                } catch (RemoteException e) {
                    Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: callback exception -- " + e);
                }
                executeNextRangingRequestIfPossible(true);
                return;
            }
            Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: cmdId=" + i + ", does not match pending RTT request cmdId=" + rttRequestInfo.cmdId);
        }

        private List<RangingResult> postProcessResults(RangingRequest rangingRequest, List<RttResult> list, boolean z) {
            byte[] bArrByteArrayFromArrayList;
            byte[] bArrByteArrayFromArrayList2;
            Iterator it;
            RttServiceSynchronized rttServiceSynchronized = this;
            HashMap map = new HashMap();
            for (RttResult rttResult : list) {
                map.put(MacAddress.fromBytes(rttResult.addr), rttResult);
            }
            ArrayList arrayList = new ArrayList(rangingRequest.mRttPeers.size());
            Iterator it2 = rangingRequest.mRttPeers.iterator();
            while (it2.hasNext()) {
                ResponderConfig responderConfig = (ResponderConfig) it2.next();
                RttResult rttResult2 = (RttResult) map.get(responderConfig.macAddress);
                int i = 1;
                if (rttResult2 == null) {
                    if (RttServiceImpl.this.mDbg) {
                        Log.v(RttServiceImpl.TAG, "postProcessResults: missing=" + responderConfig.macAddress);
                    }
                    if (!z && !responderConfig.supports80211mc) {
                        i = 2;
                    }
                    int i2 = i;
                    if (responderConfig.peerHandle == null) {
                        arrayList.add(new RangingResult(i2, responderConfig.macAddress, 0, 0, 0, 0, 0, null, null, 0L));
                    } else {
                        arrayList.add(new RangingResult(i2, responderConfig.peerHandle, 0, 0, 0, 0, 0, null, null, 0L));
                    }
                    it = it2;
                } else {
                    int i3 = rttResult2.status == 0 ? 0 : 1;
                    if (!z) {
                        bArrByteArrayFromArrayList = null;
                        bArrByteArrayFromArrayList2 = null;
                    } else {
                        bArrByteArrayFromArrayList = NativeUtil.byteArrayFromArrayList(rttResult2.lci.data);
                        bArrByteArrayFromArrayList2 = NativeUtil.byteArrayFromArrayList(rttResult2.lcr.data);
                    }
                    if (rttResult2.successNumber <= 1 && rttResult2.distanceSdInMm != 0) {
                        if (RttServiceImpl.this.mDbg) {
                            Log.w(RttServiceImpl.TAG, "postProcessResults: non-zero distance stdev with 0||1 num samples!? result=" + rttResult2);
                        }
                        rttResult2.distanceSdInMm = 0;
                    }
                    if (responderConfig.peerHandle == null) {
                        it = it2;
                        arrayList.add(new RangingResult(i3, responderConfig.macAddress, rttResult2.distanceInMm, rttResult2.distanceSdInMm, rttResult2.rssi / (-2), rttResult2.numberPerBurstPeer, rttResult2.successNumber, bArrByteArrayFromArrayList, bArrByteArrayFromArrayList2, rttResult2.timeStampInUs / 1000));
                    } else {
                        it = it2;
                        arrayList.add(new RangingResult(i3, responderConfig.peerHandle, rttResult2.distanceInMm, rttResult2.distanceSdInMm, rttResult2.rssi / (-2), rttResult2.numberPerBurstPeer, rttResult2.successNumber, bArrByteArrayFromArrayList, bArrByteArrayFromArrayList2, rttResult2.timeStampInUs / 1000));
                    }
                }
                it2 = it;
                rttServiceSynchronized = this;
            }
            return arrayList;
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.println("  mNextCommandId: " + this.mNextCommandId);
            printWriter.println("  mRttRequesterInfo: " + this.mRttRequesterInfo);
            printWriter.println("  mRttRequestQueue: " + this.mRttRequestQueue);
            printWriter.println("  mRangingTimeoutMessage: " + this.mRangingTimeoutMessage);
            RttServiceImpl.this.mRttMetrics.dump(fileDescriptor, printWriter, strArr);
            this.mRttNative.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private static class RttRequestInfo {
        public IBinder binder;
        public IRttCallback callback;
        public String callingPackage;
        public int cmdId;
        public boolean dispatchedToNative;
        public IBinder.DeathRecipient dr;
        public boolean isCalledFromPrivilegedContext;
        public boolean peerHandlesTranslated;
        public RangingRequest request;
        public int uid;
        public WorkSource workSource;

        private RttRequestInfo() {
            this.cmdId = 0;
            this.dispatchedToNative = false;
            this.peerHandlesTranslated = false;
        }

        public String toString() {
            return "RttRequestInfo: uid=" + this.uid + ", workSource=" + this.workSource + ", binder=" + this.binder + ", dr=" + this.dr + ", callingPackage=" + this.callingPackage + ", request=" + this.request.toString() + ", callback=" + this.callback + ", cmdId=" + this.cmdId + ", peerHandlesTranslated=" + this.peerHandlesTranslated + ", isCalledFromPrivilegedContext=" + this.isCalledFromPrivilegedContext;
        }
    }

    private static class RttRequesterInfo {
        public long lastRangingExecuted;

        private RttRequesterInfo() {
        }

        public String toString() {
            return "RttRequesterInfo: lastRangingExecuted=" + this.lastRangingExecuted;
        }
    }
}
