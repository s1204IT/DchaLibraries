package com.android.server.wifi.scanner;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.IWifiScanner;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Binder;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.Clock;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiConnectivityManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.hotspot2.anqp.NAIRealmData;
import com.android.server.wifi.scanner.ChannelHelper;
import com.android.server.wifi.scanner.WifiScannerImpl;
import com.android.server.wifi.scanner.WifiScanningServiceImpl;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.WifiAsyncChannel;
import com.android.server.wifi.util.WifiHandler;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public class WifiScanningServiceImpl extends IWifiScanner.Stub {
    private static final int BASE = 160000;
    private static final int CMD_DRIVER_LOADED = 160006;
    private static final int CMD_DRIVER_UNLOADED = 160007;
    private static final int CMD_FULL_SCAN_RESULTS = 160001;
    private static final int CMD_PNO_NETWORK_FOUND = 160011;
    private static final int CMD_PNO_SCAN_FAILED = 160012;
    private static final int CMD_SCAN_FAILED = 160010;
    private static final int CMD_SCAN_PAUSED = 160008;
    private static final int CMD_SCAN_RESTARTED = 160009;
    private static final int CMD_SCAN_RESULTS_AVAILABLE = 160000;
    private static final boolean DBG = false;
    private static final String TAG = "WifiScanningService";
    private static final int UNKNOWN_PID = -1;
    private final AlarmManager mAlarmManager;
    private WifiBackgroundScanStateMachine mBackgroundScanStateMachine;
    private BackgroundScanScheduler mBackgroundScheduler;
    private final IBatteryStats mBatteryStats;
    private ChannelHelper mChannelHelper;
    private ClientHandler mClientHandler;
    private final Clock mClock;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private WifiLog mLog;
    private final Looper mLooper;
    private WifiPnoScanStateMachine mPnoScanStateMachine;
    private WifiScannerImpl mScannerImpl;
    private final WifiScannerImpl.WifiScannerImplFactory mScannerImplFactory;
    private WifiSingleScanStateMachine mSingleScanStateMachine;
    private final WifiMetrics mWifiMetrics;
    private final LocalLog mLocalLog = new LocalLog(512);
    private final RequestList<Void> mSingleScanListeners = new RequestList<>();
    private final ArrayMap<Messenger, ClientInfo> mClients = new ArrayMap<>();
    private WifiNative.ScanSettings mPreviousSchedule = null;

    private void localLog(String str) {
        this.mLocalLog.log(str);
        Log.d(TAG, str);
    }

    private void logw(String str) {
        Log.w(TAG, str);
        this.mLocalLog.log(str);
    }

    private void loge(String str) {
        Log.e(TAG, str);
        this.mLocalLog.log(str);
    }

    public Messenger getMessenger() {
        if (this.mClientHandler != null) {
            this.mLog.trace("getMessenger() uid=%").c(Binder.getCallingUid()).flush();
            return new Messenger(this.mClientHandler);
        }
        loge("WifiScanningServiceImpl trying to get messenger w/o initialization");
        return null;
    }

    public Bundle getAvailableChannels(int i) {
        this.mChannelHelper.updateChannels();
        WifiScanner.ChannelSpec[] availableScanChannels = this.mChannelHelper.getAvailableScanChannels(i);
        ArrayList<Integer> arrayList = new ArrayList<>(availableScanChannels.length);
        for (WifiScanner.ChannelSpec channelSpec : availableScanChannels) {
            arrayList.add(Integer.valueOf(channelSpec.frequency));
        }
        Bundle bundle = new Bundle();
        bundle.putIntegerArrayList("Channels", arrayList);
        this.mLog.trace("getAvailableChannels uid=%").c(Binder.getCallingUid()).flush();
        return bundle;
    }

    private void enforceLocationHardwarePermission(int i) {
        this.mContext.enforcePermission("android.permission.LOCATION_HARDWARE", -1, i, "LocationHardware");
    }

    private class ClientHandler extends WifiHandler {
        ClientHandler(String str, Looper looper) {
            super(str, looper);
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            switch (message.what) {
                case 69633:
                    if (message.replyTo == null) {
                        WifiScanningServiceImpl.this.logw("msg.replyTo is null");
                    } else {
                        ExternalClientInfo externalClientInfo = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(message.replyTo);
                        if (externalClientInfo == null) {
                            WifiAsyncChannel wifiAsyncChannelMakeWifiAsyncChannel = WifiScanningServiceImpl.this.mFrameworkFacade.makeWifiAsyncChannel(WifiScanningServiceImpl.TAG);
                            wifiAsyncChannelMakeWifiAsyncChannel.connected(WifiScanningServiceImpl.this.mContext, this, message.replyTo);
                            ExternalClientInfo externalClientInfo2 = WifiScanningServiceImpl.this.new ExternalClientInfo(message.sendingUid, message.replyTo, wifiAsyncChannelMakeWifiAsyncChannel);
                            externalClientInfo2.register();
                            wifiAsyncChannelMakeWifiAsyncChannel.replyToMessage(message, 69634, 0);
                            WifiScanningServiceImpl.this.localLog("client connected: " + externalClientInfo2);
                        } else {
                            WifiScanningServiceImpl.this.logw("duplicate client connection: " + message.sendingUid + ", messenger=" + message.replyTo);
                            externalClientInfo.mChannel.replyToMessage(message, 69634, 3);
                        }
                    }
                    break;
                case 69634:
                default:
                    try {
                        WifiScanningServiceImpl.this.enforceLocationHardwarePermission(message.sendingUid);
                        if (message.what == 159748) {
                            WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(Message.obtain(message));
                            break;
                        } else if (message.what == 159773) {
                            WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(Message.obtain(message));
                            break;
                        } else {
                            ClientInfo clientInfo = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(message.replyTo);
                            if (clientInfo != null) {
                                switch (message.what) {
                                    case 159746:
                                    case 159747:
                                        WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(Message.obtain(message));
                                        break;
                                    case 159765:
                                    case 159766:
                                        WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(Message.obtain(message));
                                        break;
                                    case 159768:
                                    case 159769:
                                        WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(Message.obtain(message));
                                        break;
                                    case 159771:
                                        WifiScanningServiceImpl.this.logScanRequest("registerScanListener", clientInfo, message.arg2, null, null, null);
                                        WifiScanningServiceImpl.this.mSingleScanListeners.addRequest(clientInfo, message.arg2, null, null);
                                        WifiScanningServiceImpl.this.replySucceeded(message);
                                        break;
                                    case 159772:
                                        WifiScanningServiceImpl.this.logScanRequest("deregisterScanListener", clientInfo, message.arg2, null, null, null);
                                        WifiScanningServiceImpl.this.mSingleScanListeners.removeRequest(clientInfo, message.arg2);
                                        break;
                                    default:
                                        WifiScanningServiceImpl.this.replyFailed(message, -3, "Invalid request");
                                        break;
                                }
                            } else {
                                WifiScanningServiceImpl.this.loge("Could not find client info for message " + message.replyTo + ", msg=" + message);
                                WifiScanningServiceImpl.this.replyFailed(message, -2, "Could not find listener");
                                break;
                            }
                        }
                    } catch (SecurityException e) {
                        WifiScanningServiceImpl.this.localLog("failed to authorize app: " + e);
                        WifiScanningServiceImpl.this.replyFailed(message, -4, "Not authorized");
                        return;
                    }
                    break;
                case 69635:
                    ExternalClientInfo externalClientInfo3 = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(message.replyTo);
                    if (externalClientInfo3 != null) {
                        externalClientInfo3.mChannel.disconnect();
                    }
                    break;
                case 69636:
                    ExternalClientInfo externalClientInfo4 = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(message.replyTo);
                    if (externalClientInfo4 != null && message.arg1 != 2 && message.arg1 != 3) {
                        WifiScanningServiceImpl.this.localLog("client disconnected: " + externalClientInfo4 + ", reason: " + message.arg1);
                        externalClientInfo4.cleanup();
                        break;
                    }
                    break;
            }
        }
    }

    WifiScanningServiceImpl(Context context, Looper looper, WifiScannerImpl.WifiScannerImplFactory wifiScannerImplFactory, IBatteryStats iBatteryStats, WifiInjector wifiInjector) {
        this.mContext = context;
        this.mLooper = looper;
        this.mScannerImplFactory = wifiScannerImplFactory;
        this.mBatteryStats = iBatteryStats;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWifiMetrics = wifiInjector.getWifiMetrics();
        this.mClock = wifiInjector.getClock();
        this.mLog = wifiInjector.makeLog(TAG);
        this.mFrameworkFacade = wifiInjector.getFrameworkFacade();
    }

    public void startService() {
        this.mBackgroundScanStateMachine = new WifiBackgroundScanStateMachine(this.mLooper);
        this.mSingleScanStateMachine = new WifiSingleScanStateMachine(this.mLooper);
        this.mPnoScanStateMachine = new WifiPnoScanStateMachine(this.mLooper);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int intExtra = intent.getIntExtra("scan_enabled", 1);
                if (intExtra == 3) {
                    WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                    WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                } else if (intExtra == 1) {
                    WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                    WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                }
            }
        }, new IntentFilter("wifi_scan_available"));
        this.mBackgroundScanStateMachine.start();
        this.mSingleScanStateMachine.start();
        this.mPnoScanStateMachine.start();
        this.mClientHandler = new ClientHandler(TAG, this.mLooper);
    }

    @VisibleForTesting
    public void setWifiHandlerLogForTest(WifiLog wifiLog) {
        this.mClientHandler.setWifiLog(wifiLog);
    }

    private WorkSource computeWorkSource(ClientInfo clientInfo, WorkSource workSource) {
        if (workSource != null) {
            workSource.clearNames();
            if (!workSource.isEmpty()) {
                return workSource;
            }
        }
        if (clientInfo.getUid() > 0) {
            return new WorkSource(clientInfo.getUid());
        }
        loge("Unable to compute workSource for client: " + clientInfo + ", requested: " + workSource);
        return new WorkSource();
    }

    private class RequestInfo<T> {
        final ClientInfo clientInfo;
        final int handlerId;
        final T settings;
        final WorkSource workSource;

        RequestInfo(ClientInfo clientInfo, int i, WorkSource workSource, T t) {
            this.clientInfo = clientInfo;
            this.handlerId = i;
            this.settings = t;
            this.workSource = WifiScanningServiceImpl.this.computeWorkSource(clientInfo, workSource);
        }

        void reportEvent(int i, int i2, Object obj) {
            this.clientInfo.reportEvent(i, i2, this.handlerId, obj);
        }
    }

    private class RequestList<T> extends ArrayList<RequestInfo<T>> {
        private RequestList() {
        }

        void addRequest(ClientInfo clientInfo, int i, WorkSource workSource, T t) {
            add(WifiScanningServiceImpl.this.new RequestInfo(clientInfo, i, workSource, t));
        }

        T removeRequest(ClientInfo clientInfo, int i) {
            Iterator<RequestInfo<T>> it = iterator();
            T t = null;
            while (it.hasNext()) {
                RequestInfo<T> next = it.next();
                if (next.clientInfo == clientInfo && next.handlerId == i) {
                    t = next.settings;
                    it.remove();
                }
            }
            return t;
        }

        Collection<T> getAllSettings() {
            ArrayList arrayList = new ArrayList();
            Iterator<RequestInfo<T>> it = iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().settings);
            }
            return arrayList;
        }

        Collection<T> getAllSettingsForClient(ClientInfo clientInfo) {
            ArrayList arrayList = new ArrayList();
            for (RequestInfo<T> requestInfo : this) {
                if (requestInfo.clientInfo == clientInfo) {
                    arrayList.add(requestInfo.settings);
                }
            }
            return arrayList;
        }

        void removeAllForClient(ClientInfo clientInfo) {
            Iterator<RequestInfo<T>> it = iterator();
            while (it.hasNext()) {
                if (it.next().clientInfo == clientInfo) {
                    it.remove();
                }
            }
        }

        WorkSource createMergedWorkSource() {
            WorkSource workSource = new WorkSource();
            Iterator<RequestInfo<T>> it = iterator();
            while (it.hasNext()) {
                workSource.add(it.next().workSource);
            }
            return workSource;
        }
    }

    class WifiSingleScanStateMachine extends StateMachine implements WifiNative.ScanEventHandler {

        @VisibleForTesting
        public static final int CACHED_SCAN_RESULTS_MAX_AGE_IN_MILLIS = 180000;
        private WifiNative.ScanSettings mActiveScanSettings;
        private RequestList<WifiScanner.ScanSettings> mActiveScans;
        private final List<ScanResult> mCachedScanResults;
        private final DefaultState mDefaultState;
        private final DriverStartedState mDriverStartedState;
        private final IdleState mIdleState;
        private RequestList<WifiScanner.ScanSettings> mPendingScans;
        private final ScanningState mScanningState;

        WifiSingleScanStateMachine(Looper looper) {
            super("WifiSingleScanStateMachine", looper);
            this.mDefaultState = new DefaultState();
            this.mDriverStartedState = new DriverStartedState();
            this.mIdleState = new IdleState();
            this.mScanningState = new ScanningState();
            this.mActiveScanSettings = null;
            this.mActiveScans = new RequestList<>();
            this.mPendingScans = new RequestList<>();
            this.mCachedScanResults = new ArrayList();
            setLogRecSize(128);
            setLogOnlyTransitions(WifiScanningServiceImpl.DBG);
            addState(this.mDefaultState);
            addState(this.mDriverStartedState, this.mDefaultState);
            addState(this.mIdleState, this.mDriverStartedState);
            addState(this.mScanningState, this.mDriverStartedState);
            setInitialState(this.mDefaultState);
        }

        @Override
        public void onScanStatus(int i) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                    sendMessage(WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS);
                    break;
                case 3:
                    sendMessage(WifiScanningServiceImpl.CMD_SCAN_FAILED);
                    break;
                default:
                    Log.e(WifiScanningServiceImpl.TAG, "Unknown scan status event: " + i);
                    break;
            }
        }

        @Override
        public void onFullScanResult(ScanResult scanResult, int i) {
            sendMessage(WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS, 0, i, scanResult);
        }

        @Override
        public void onScanPaused(WifiScanner.ScanData[] scanDataArr) {
            Log.e(WifiScanningServiceImpl.TAG, "Got scan paused for single scan");
        }

        @Override
        public void onScanRestarted() {
            Log.e(WifiScanningServiceImpl.TAG, "Got scan restarted for single scan");
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                WifiSingleScanStateMachine.this.mActiveScans.clear();
                WifiSingleScanStateMachine.this.mPendingScans.clear();
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 159765:
                    case 159766:
                        WifiScanningServiceImpl.this.replyFailed(message, -1, "not available");
                        break;
                    case 159773:
                        message.obj = new WifiScanner.ParcelableScanResults(filterCachedScanResultsByAge());
                        WifiScanningServiceImpl.this.replySucceeded(message);
                        break;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                        break;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        if (WifiScanningServiceImpl.this.mScannerImpl == null) {
                            WifiSingleScanStateMachine.this.loge("Failed to start single scan state machine because scanner impl is null");
                        } else {
                            WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                        }
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mDefaultState);
                        break;
                }
                return true;
            }

            private ScanResult[] filterCachedScanResultsByAge() {
                final long elapsedSinceBootMillis = WifiScanningServiceImpl.this.mClock.getElapsedSinceBootMillis();
                return (ScanResult[]) WifiSingleScanStateMachine.this.mCachedScanResults.stream().filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return WifiScanningServiceImpl.WifiSingleScanStateMachine.DefaultState.lambda$filterCachedScanResultsByAge$0(elapsedSinceBootMillis, (ScanResult) obj);
                    }
                }).toArray(new IntFunction() {
                    @Override
                    public final Object apply(int i) {
                        return WifiScanningServiceImpl.WifiSingleScanStateMachine.DefaultState.lambda$filterCachedScanResultsByAge$1(i);
                    }
                });
            }

            static boolean lambda$filterCachedScanResultsByAge$0(long j, ScanResult scanResult) {
                if (j - (scanResult.timestamp / 1000) < 180000) {
                    return true;
                }
                return WifiScanningServiceImpl.DBG;
            }

            static ScanResult[] lambda$filterCachedScanResultsByAge$1(int i) {
                return new ScanResult[i];
            }
        }

        class DriverStartedState extends State {
            DriverStartedState() {
            }

            public void exit() {
                WifiSingleScanStateMachine.this.mCachedScanResults.clear();
                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(2, WifiSingleScanStateMachine.this.mPendingScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mPendingScans, -1, "Scan was interrupted");
            }

            public boolean processMessage(Message message) {
                ClientInfo clientInfo = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(message.replyTo);
                int i = message.what;
                if (i == WifiScanningServiceImpl.CMD_DRIVER_LOADED) {
                    return true;
                }
                switch (i) {
                    case 159765:
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementOneshotScanCount();
                        int i2 = message.arg2;
                        Bundle bundle = (Bundle) message.obj;
                        if (bundle == null) {
                            WifiScanningServiceImpl.this.logCallback("singleScanInvalidRequest", clientInfo, i2, "null params");
                            WifiScanningServiceImpl.this.replyFailed(message, -3, "params null");
                        } else {
                            bundle.setDefusable(true);
                            WifiScanner.ScanSettings parcelable = bundle.getParcelable("ScanSettings");
                            WorkSource workSource = (WorkSource) bundle.getParcelable("WorkSource");
                            if (WifiSingleScanStateMachine.this.validateScanRequest(clientInfo, i2, parcelable)) {
                                WifiScanningServiceImpl.this.logScanRequest("addSingleScanRequest", clientInfo, i2, workSource, parcelable, null);
                                WifiScanningServiceImpl.this.replySucceeded(message);
                                if (WifiSingleScanStateMachine.this.getCurrentState() != WifiSingleScanStateMachine.this.mScanningState) {
                                    WifiSingleScanStateMachine.this.mPendingScans.addRequest(clientInfo, i2, workSource, parcelable);
                                    WifiSingleScanStateMachine.this.tryToStartNewScan();
                                } else if (WifiSingleScanStateMachine.this.activeScanSatisfies(parcelable)) {
                                    WifiSingleScanStateMachine.this.mActiveScans.addRequest(clientInfo, i2, workSource, parcelable);
                                } else {
                                    WifiSingleScanStateMachine.this.mPendingScans.addRequest(clientInfo, i2, workSource, parcelable);
                                }
                            } else {
                                WifiScanningServiceImpl.this.logCallback("singleScanInvalidRequest", clientInfo, i2, "bad request");
                                WifiScanningServiceImpl.this.replyFailed(message, -3, "bad request");
                                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(3, 1);
                            }
                        }
                        break;
                    case 159766:
                        WifiSingleScanStateMachine.this.removeSingleScanRequest(clientInfo, message.arg2);
                        break;
                }
                return true;
            }
        }

        class IdleState extends State {
            IdleState() {
            }

            public void enter() {
                WifiSingleScanStateMachine.this.tryToStartNewScan();
            }

            public boolean processMessage(Message message) {
                return WifiScanningServiceImpl.DBG;
            }
        }

        class ScanningState extends State {
            private WorkSource mScanWorkSource;

            ScanningState() {
            }

            public void enter() {
                this.mScanWorkSource = WifiSingleScanStateMachine.this.mActiveScans.createMergedWorkSource();
                try {
                    WifiScanningServiceImpl.this.mBatteryStats.noteWifiScanStartedFromSource(this.mScanWorkSource);
                } catch (RemoteException e) {
                    WifiSingleScanStateMachine.this.loge(e.toString());
                }
            }

            public void exit() {
                WifiSingleScanStateMachine.this.mActiveScanSettings = null;
                try {
                    WifiScanningServiceImpl.this.mBatteryStats.noteWifiScanStoppedFromSource(this.mScanWorkSource);
                } catch (RemoteException e) {
                    WifiSingleScanStateMachine.this.loge(e.toString());
                }
                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(0, WifiSingleScanStateMachine.this.mActiveScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mActiveScans, -1, "Scan was interrupted");
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != WifiScanningServiceImpl.CMD_SCAN_FAILED) {
                    switch (i) {
                        case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                            WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(1, WifiSingleScanStateMachine.this.mActiveScans.size());
                            WifiSingleScanStateMachine.this.reportScanResults(WifiScanningServiceImpl.this.mScannerImpl.getLatestSingleScanResults());
                            WifiSingleScanStateMachine.this.mActiveScans.clear();
                            WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                            return true;
                        case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                            WifiSingleScanStateMachine.this.reportFullScanResult((ScanResult) message.obj, message.arg2);
                            return true;
                        default:
                            return WifiScanningServiceImpl.DBG;
                    }
                }
                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(0, WifiSingleScanStateMachine.this.mActiveScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mActiveScans, -1, "Scan failed");
                WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                return true;
            }
        }

        boolean validateScanType(int i) {
            if (i == 0 || i == 1 || i == 2) {
                return true;
            }
            return WifiScanningServiceImpl.DBG;
        }

        boolean validateScanRequest(ClientInfo clientInfo, int i, WifiScanner.ScanSettings scanSettings) {
            if (clientInfo == null) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing single scan request ClientInfo not found " + i);
                return WifiScanningServiceImpl.DBG;
            }
            if (scanSettings.band == 0 && (scanSettings.channels == null || scanSettings.channels.length == 0)) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing single scan because channel list was empty");
                return WifiScanningServiceImpl.DBG;
            }
            if (!validateScanType(scanSettings.type)) {
                Log.e(WifiScanningServiceImpl.TAG, "Invalid scan type " + scanSettings.type);
                return WifiScanningServiceImpl.DBG;
            }
            if (WifiScanningServiceImpl.this.mContext.checkPermission("android.permission.NETWORK_STACK", -1, clientInfo.getUid()) == -1) {
                if (!ArrayUtils.isEmpty(scanSettings.hiddenNetworks)) {
                    Log.e(WifiScanningServiceImpl.TAG, "Failing single scan because app " + clientInfo.getUid() + " does not have permission to set hidden networks");
                    return WifiScanningServiceImpl.DBG;
                }
                if (scanSettings.type != 0) {
                    Log.e(WifiScanningServiceImpl.TAG, "Failing single scan because app " + clientInfo.getUid() + " does not have permission to set type");
                    return WifiScanningServiceImpl.DBG;
                }
                return true;
            }
            return true;
        }

        int getNativeScanType(int i) {
            switch (i) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                default:
                    throw new IllegalArgumentException("Invalid scan type " + i);
            }
        }

        boolean activeScanTypeSatisfies(int i) {
            switch (this.mActiveScanSettings.scanType) {
                case 0:
                case 1:
                    if (i != 2) {
                        return true;
                    }
                    return WifiScanningServiceImpl.DBG;
                case 2:
                    return true;
                default:
                    throw new IllegalArgumentException("Invalid scan type " + this.mActiveScanSettings.scanType);
            }
        }

        int mergeScanTypes(int i, int i2) {
            switch (i) {
                case 0:
                case 1:
                    return i2;
                case 2:
                    return i;
                default:
                    throw new IllegalArgumentException("Invalid scan type " + i);
            }
        }

        boolean activeScanSatisfies(WifiScanner.ScanSettings scanSettings) {
            if (this.mActiveScanSettings == null || !activeScanTypeSatisfies(getNativeScanType(scanSettings.type))) {
                return WifiScanningServiceImpl.DBG;
            }
            WifiNative.BucketSettings bucketSettings = this.mActiveScanSettings.buckets[0];
            ChannelHelper.ChannelCollection channelCollectionCreateChannelCollection = WifiScanningServiceImpl.this.mChannelHelper.createChannelCollection();
            channelCollectionCreateChannelCollection.addChannels(bucketSettings);
            if (!channelCollectionCreateChannelCollection.containsSettings(scanSettings)) {
                return WifiScanningServiceImpl.DBG;
            }
            if ((scanSettings.reportEvents & 2) != 0 && (bucketSettings.report_events & 2) == 0) {
                return WifiScanningServiceImpl.DBG;
            }
            if (!ArrayUtils.isEmpty(scanSettings.hiddenNetworks)) {
                if (ArrayUtils.isEmpty(this.mActiveScanSettings.hiddenNetworks)) {
                    return WifiScanningServiceImpl.DBG;
                }
                ArrayList arrayList = new ArrayList();
                for (WifiNative.HiddenNetwork hiddenNetwork : this.mActiveScanSettings.hiddenNetworks) {
                    arrayList.add(hiddenNetwork);
                }
                for (WifiScanner.ScanSettings.HiddenNetwork hiddenNetwork2 : scanSettings.hiddenNetworks) {
                    WifiNative.HiddenNetwork hiddenNetwork3 = new WifiNative.HiddenNetwork();
                    hiddenNetwork3.ssid = hiddenNetwork2.ssid;
                    if (!arrayList.contains(hiddenNetwork3)) {
                        return WifiScanningServiceImpl.DBG;
                    }
                }
                return true;
            }
            return true;
        }

        void removeSingleScanRequest(ClientInfo clientInfo, int i) {
            if (clientInfo != null) {
                WifiScanningServiceImpl.this.logScanRequest("removeSingleScanRequest", clientInfo, i, null, null, null);
                this.mPendingScans.removeRequest(clientInfo, i);
                this.mActiveScans.removeRequest(clientInfo, i);
            }
        }

        void removeSingleScanRequests(ClientInfo clientInfo) {
            if (clientInfo != null) {
                WifiScanningServiceImpl.this.logScanRequest("removeSingleScanRequests", clientInfo, -1, null, null, null);
                this.mPendingScans.removeAllForClient(clientInfo);
                this.mActiveScans.removeAllForClient(clientInfo);
            }
        }

        void tryToStartNewScan() {
            if (this.mPendingScans.size() != 0) {
                WifiScanningServiceImpl.this.mChannelHelper.updateChannels();
                WifiNative.ScanSettings scanSettings = new WifiNative.ScanSettings();
                scanSettings.num_buckets = 1;
                WifiNative.BucketSettings bucketSettings = new WifiNative.BucketSettings();
                bucketSettings.bucket = 0;
                bucketSettings.period_ms = 0;
                bucketSettings.report_events = 1;
                ChannelHelper.ChannelCollection channelCollectionCreateChannelCollection = WifiScanningServiceImpl.this.mChannelHelper.createChannelCollection();
                ArrayList arrayList = new ArrayList();
                Iterator<RequestInfo<T>> it = this.mPendingScans.iterator();
                while (it.hasNext()) {
                    RequestInfo requestInfo = (RequestInfo) it.next();
                    scanSettings.scanType = mergeScanTypes(scanSettings.scanType, getNativeScanType(((WifiScanner.ScanSettings) requestInfo.settings).type));
                    channelCollectionCreateChannelCollection.addChannels((WifiScanner.ScanSettings) requestInfo.settings);
                    if (((WifiScanner.ScanSettings) requestInfo.settings).hiddenNetworks != null) {
                        for (int i = 0; i < ((WifiScanner.ScanSettings) requestInfo.settings).hiddenNetworks.length; i++) {
                            WifiNative.HiddenNetwork hiddenNetwork = new WifiNative.HiddenNetwork();
                            hiddenNetwork.ssid = ((WifiScanner.ScanSettings) requestInfo.settings).hiddenNetworks[i].ssid;
                            arrayList.add(hiddenNetwork);
                        }
                    }
                    if ((((WifiScanner.ScanSettings) requestInfo.settings).reportEvents & 2) != 0) {
                        bucketSettings.report_events |= 2;
                    }
                }
                if (arrayList.size() > 0) {
                    scanSettings.hiddenNetworks = new WifiNative.HiddenNetwork[arrayList.size()];
                    Iterator it2 = arrayList.iterator();
                    int i2 = 0;
                    while (it2.hasNext()) {
                        scanSettings.hiddenNetworks[i2] = (WifiNative.HiddenNetwork) it2.next();
                        i2++;
                    }
                }
                channelCollectionCreateChannelCollection.fillBucketSettings(bucketSettings, ScoringParams.Values.MAX_EXPID);
                scanSettings.buckets = new WifiNative.BucketSettings[]{bucketSettings};
                if (WifiScanningServiceImpl.this.mScannerImpl.startSingleScan(scanSettings, this)) {
                    this.mActiveScanSettings = scanSettings;
                    RequestList<WifiScanner.ScanSettings> requestList = this.mActiveScans;
                    this.mActiveScans = this.mPendingScans;
                    this.mPendingScans = requestList;
                    this.mPendingScans.clear();
                    transitionTo(this.mScanningState);
                    return;
                }
                WifiScanningServiceImpl.this.mWifiMetrics.incrementScanReturnEntry(0, this.mPendingScans.size());
                sendOpFailedToAllAndClear(this.mPendingScans, -1, "Failed to start single scan");
            }
        }

        void sendOpFailedToAllAndClear(RequestList<?> requestList, int i, String str) {
            Iterator<RequestInfo<T>> it = requestList.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                WifiScanningServiceImpl.this.logCallback("singleScanFailed", requestInfo.clientInfo, requestInfo.handlerId, "reason=" + i + ", " + str);
                requestInfo.reportEvent(159762, 0, new WifiScanner.OperationResult(i, str));
            }
            requestList.clear();
        }

        void reportFullScanResult(ScanResult scanResult, int i) {
            Iterator<RequestInfo<T>> it = this.mActiveScans.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                if (ScanScheduleUtil.shouldReportFullScanResultForSettings(WifiScanningServiceImpl.this.mChannelHelper, scanResult, i, (WifiScanner.ScanSettings) requestInfo.settings, -1)) {
                    requestInfo.reportEvent(159764, 0, scanResult);
                }
            }
            Iterator<RequestInfo<T>> it2 = WifiScanningServiceImpl.this.mSingleScanListeners.iterator();
            while (it2.hasNext()) {
                ((RequestInfo) it2.next()).reportEvent(159764, 0, scanResult);
            }
        }

        void reportScanResults(WifiScanner.ScanData scanData) {
            if (scanData != null && scanData.getResults() != null) {
                if (scanData.getResults().length > 0) {
                    WifiScanningServiceImpl.this.mWifiMetrics.incrementNonEmptyScanResultCount();
                } else {
                    WifiScanningServiceImpl.this.mWifiMetrics.incrementEmptyScanResultCount();
                }
            }
            WifiScanner.ScanData[] scanDataArr = {scanData};
            Iterator<RequestInfo<T>> it = this.mActiveScans.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                WifiScanner.ScanData[] scanDataArrFilterResultsForSettings = ScanScheduleUtil.filterResultsForSettings(WifiScanningServiceImpl.this.mChannelHelper, scanDataArr, (WifiScanner.ScanSettings) requestInfo.settings, -1);
                WifiScanner.ParcelableScanData parcelableScanData = new WifiScanner.ParcelableScanData(scanDataArrFilterResultsForSettings);
                WifiScanningServiceImpl.this.logCallback("singleScanResults", requestInfo.clientInfo, requestInfo.handlerId, WifiScanningServiceImpl.describeForLog(scanDataArrFilterResultsForSettings));
                requestInfo.reportEvent(159749, 0, parcelableScanData);
                requestInfo.reportEvent(159767, 0, null);
            }
            WifiScanner.ParcelableScanData parcelableScanData2 = new WifiScanner.ParcelableScanData(scanDataArr);
            for (RequestInfo requestInfo2 : WifiScanningServiceImpl.this.mSingleScanListeners) {
                WifiScanningServiceImpl.this.logCallback("singleScanResults", requestInfo2.clientInfo, requestInfo2.handlerId, WifiScanningServiceImpl.describeForLog(scanDataArr));
                requestInfo2.reportEvent(159749, 0, parcelableScanData2);
            }
            if (scanData.isAllChannelsScanned()) {
                this.mCachedScanResults.clear();
                this.mCachedScanResults.addAll(Arrays.asList(scanData.getResults()));
            }
        }

        List<ScanResult> getCachedScanResultsAsList() {
            return this.mCachedScanResults;
        }
    }

    class WifiBackgroundScanStateMachine extends StateMachine implements WifiNative.ScanEventHandler {
        private final RequestList<WifiScanner.ScanSettings> mActiveBackgroundScans;
        private final DefaultState mDefaultState;
        private final PausedState mPausedState;
        private final StartedState mStartedState;

        WifiBackgroundScanStateMachine(Looper looper) {
            super("WifiBackgroundScanStateMachine", looper);
            this.mDefaultState = new DefaultState();
            this.mStartedState = new StartedState();
            this.mPausedState = new PausedState();
            this.mActiveBackgroundScans = new RequestList<>();
            setLogRecSize(512);
            setLogOnlyTransitions(WifiScanningServiceImpl.DBG);
            addState(this.mDefaultState);
            addState(this.mStartedState, this.mDefaultState);
            addState(this.mPausedState, this.mDefaultState);
            setInitialState(this.mDefaultState);
        }

        public Collection<WifiScanner.ScanSettings> getBackgroundScanSettings(ClientInfo clientInfo) {
            return this.mActiveBackgroundScans.getAllSettingsForClient(clientInfo);
        }

        public void removeBackgroundScanSettings(ClientInfo clientInfo) {
            this.mActiveBackgroundScans.removeAllForClient(clientInfo);
            updateSchedule();
        }

        @Override
        public void onScanStatus(int i) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                    sendMessage(WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS);
                    break;
                case 3:
                    sendMessage(WifiScanningServiceImpl.CMD_SCAN_FAILED);
                    break;
                default:
                    Log.e(WifiScanningServiceImpl.TAG, "Unknown scan status event: " + i);
                    break;
            }
        }

        @Override
        public void onFullScanResult(ScanResult scanResult, int i) {
            sendMessage(WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS, 0, i, scanResult);
        }

        @Override
        public void onScanPaused(WifiScanner.ScanData[] scanDataArr) {
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_PAUSED, scanDataArr);
        }

        @Override
        public void onScanRestarted() {
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_RESTARTED);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                WifiBackgroundScanStateMachine.this.mActiveBackgroundScans.clear();
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 159746:
                    case 159747:
                    case 159748:
                    case 159765:
                    case 159766:
                        WifiScanningServiceImpl.this.replyFailed(message, -1, "not available");
                        return true;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                    default:
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        WifiScanningServiceImpl.this.mScannerImpl = WifiScanningServiceImpl.this.mScannerImplFactory.create(WifiScanningServiceImpl.this.mContext, WifiScanningServiceImpl.this.mLooper, WifiScanningServiceImpl.this.mClock);
                        if (WifiScanningServiceImpl.this.mScannerImpl == null) {
                            WifiBackgroundScanStateMachine.this.loge("Failed to start bgscan scan state machine because scanner impl is null");
                            return true;
                        }
                        WifiScanningServiceImpl.this.mChannelHelper = WifiScanningServiceImpl.this.mScannerImpl.getChannelHelper();
                        WifiScanningServiceImpl.this.mBackgroundScheduler = new BackgroundScanScheduler(WifiScanningServiceImpl.this.mChannelHelper);
                        WifiNative.ScanCapabilities scanCapabilities = new WifiNative.ScanCapabilities();
                        if (!WifiScanningServiceImpl.this.mScannerImpl.getScanCapabilities(scanCapabilities)) {
                            WifiBackgroundScanStateMachine.this.loge("could not get scan capabilities");
                            return true;
                        }
                        if (scanCapabilities.max_scan_buckets > 0) {
                            WifiScanningServiceImpl.this.mBackgroundScheduler.setMaxBuckets(scanCapabilities.max_scan_buckets);
                            WifiScanningServiceImpl.this.mBackgroundScheduler.setMaxApPerScan(scanCapabilities.max_ap_cache_per_scan);
                            Log.i(WifiScanningServiceImpl.TAG, "wifi driver loaded with scan capabilities: max buckets=" + scanCapabilities.max_scan_buckets);
                            WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mStartedState);
                            return true;
                        }
                        WifiBackgroundScanStateMachine.this.loge("invalid max buckets in scan capabilities " + scanCapabilities.max_scan_buckets);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        Log.i(WifiScanningServiceImpl.TAG, "wifi driver unloaded");
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mDefaultState);
                        return true;
                }
            }
        }

        class StartedState extends State {
            StartedState() {
            }

            public void enter() {
            }

            public void exit() {
                WifiBackgroundScanStateMachine.this.sendBackgroundScanFailedToAllAndClear(-1, "Scan was interrupted");
                if (WifiScanningServiceImpl.this.mScannerImpl != null) {
                    WifiScanningServiceImpl.this.mScannerImpl.cleanup();
                }
            }

            public boolean processMessage(Message message) {
                ClientInfo clientInfo = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(message.replyTo);
                switch (message.what) {
                    case 159746:
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementBackgroundScanCount();
                        Bundle bundle = (Bundle) message.obj;
                        if (bundle == null) {
                            WifiScanningServiceImpl.this.replyFailed(message, -3, "params null");
                            return true;
                        }
                        bundle.setDefusable(true);
                        if (!WifiBackgroundScanStateMachine.this.addBackgroundScanRequest(clientInfo, message.arg2, bundle.getParcelable("ScanSettings"), (WorkSource) bundle.getParcelable("WorkSource"))) {
                            WifiScanningServiceImpl.this.replyFailed(message, -3, "bad request");
                        } else {
                            WifiScanningServiceImpl.this.replySucceeded(message);
                        }
                        return true;
                    case 159747:
                        WifiBackgroundScanStateMachine.this.removeBackgroundScanRequest(clientInfo, message.arg2);
                        return true;
                    case 159748:
                        WifiBackgroundScanStateMachine.this.reportScanResults(WifiScanningServiceImpl.this.mScannerImpl.getLatestBatchedScanResults(true));
                        WifiScanningServiceImpl.this.replySucceeded(message);
                        return true;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                        WifiBackgroundScanStateMachine.this.reportScanResults(WifiScanningServiceImpl.this.mScannerImpl.getLatestBatchedScanResults(true));
                        return true;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        WifiBackgroundScanStateMachine.this.reportFullScanResult((ScanResult) message.obj, message.arg2);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        Log.e(WifiScanningServiceImpl.TAG, "wifi driver loaded received while already loaded");
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        return WifiScanningServiceImpl.DBG;
                    case WifiScanningServiceImpl.CMD_SCAN_PAUSED:
                        WifiBackgroundScanStateMachine.this.reportScanResults((WifiScanner.ScanData[]) message.obj);
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mPausedState);
                        return true;
                    case WifiScanningServiceImpl.CMD_SCAN_FAILED:
                        Log.e(WifiScanningServiceImpl.TAG, "WifiScanner background scan gave CMD_SCAN_FAILED");
                        WifiBackgroundScanStateMachine.this.sendBackgroundScanFailedToAllAndClear(-1, "Background Scan failed");
                        return true;
                    default:
                        return WifiScanningServiceImpl.DBG;
                }
            }
        }

        class PausedState extends State {
            PausedState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message message) {
                if (message.what == WifiScanningServiceImpl.CMD_SCAN_RESTARTED) {
                    WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mStartedState);
                    return true;
                }
                WifiBackgroundScanStateMachine.this.deferMessage(message);
                return true;
            }
        }

        private boolean addBackgroundScanRequest(ClientInfo clientInfo, int i, WifiScanner.ScanSettings scanSettings, WorkSource workSource) {
            if (clientInfo == null) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing scan request ClientInfo not found " + i);
                return WifiScanningServiceImpl.DBG;
            }
            if (scanSettings.periodInMs < 1000) {
                loge("Failing scan request because periodInMs is " + scanSettings.periodInMs + ", min scan period is: 1000");
                return WifiScanningServiceImpl.DBG;
            }
            if (scanSettings.band == 0 && scanSettings.channels == null) {
                loge("Channels was null with unspecified band");
                return WifiScanningServiceImpl.DBG;
            }
            if (scanSettings.band != 0 || scanSettings.channels.length != 0) {
                int iEstimateScanDuration = WifiScanningServiceImpl.this.mChannelHelper.estimateScanDuration(scanSettings);
                if (scanSettings.periodInMs < iEstimateScanDuration) {
                    loge("Failing scan request because minSupportedPeriodMs is " + iEstimateScanDuration + " but the request wants " + scanSettings.periodInMs);
                    return WifiScanningServiceImpl.DBG;
                }
                if (scanSettings.maxPeriodInMs != 0 && scanSettings.maxPeriodInMs != scanSettings.periodInMs) {
                    if (scanSettings.maxPeriodInMs < scanSettings.periodInMs) {
                        loge("Failing scan request because maxPeriodInMs is " + scanSettings.maxPeriodInMs + " but less than periodInMs " + scanSettings.periodInMs);
                        return WifiScanningServiceImpl.DBG;
                    }
                    if (scanSettings.maxPeriodInMs > 1024000) {
                        loge("Failing scan request because maxSupportedPeriodMs is 1024000 but the request wants " + scanSettings.maxPeriodInMs);
                        return WifiScanningServiceImpl.DBG;
                    }
                    if (scanSettings.stepCount < 1) {
                        loge("Failing scan request because stepCount is " + scanSettings.stepCount + " which is less than 1");
                        return WifiScanningServiceImpl.DBG;
                    }
                }
                WifiScanningServiceImpl.this.logScanRequest("addBackgroundScanRequest", clientInfo, i, null, scanSettings, null);
                this.mActiveBackgroundScans.addRequest(clientInfo, i, workSource, scanSettings);
                if (updateSchedule()) {
                    return true;
                }
                this.mActiveBackgroundScans.removeRequest(clientInfo, i);
                WifiScanningServiceImpl.this.localLog("Failing scan request because failed to reset scan");
                return WifiScanningServiceImpl.DBG;
            }
            loge("No channels specified");
            return WifiScanningServiceImpl.DBG;
        }

        private boolean updateSchedule() {
            if (WifiScanningServiceImpl.this.mChannelHelper != null && WifiScanningServiceImpl.this.mBackgroundScheduler != null && WifiScanningServiceImpl.this.mScannerImpl != null) {
                WifiScanningServiceImpl.this.mChannelHelper.updateChannels();
                WifiScanningServiceImpl.this.mBackgroundScheduler.updateSchedule(this.mActiveBackgroundScans.getAllSettings());
                WifiNative.ScanSettings schedule = WifiScanningServiceImpl.this.mBackgroundScheduler.getSchedule();
                if (ScanScheduleUtil.scheduleEquals(WifiScanningServiceImpl.this.mPreviousSchedule, schedule)) {
                    return true;
                }
                WifiScanningServiceImpl.this.mPreviousSchedule = schedule;
                if (schedule.num_buckets == 0) {
                    WifiScanningServiceImpl.this.mScannerImpl.stopBatchedScan();
                    return true;
                }
                WifiScanningServiceImpl.this.localLog("starting scan: base period=" + schedule.base_period_ms + ", max ap per scan=" + schedule.max_ap_per_scan + ", batched scans=" + schedule.report_threshold_num_scans);
                for (int i = 0; i < schedule.num_buckets; i++) {
                    WifiNative.BucketSettings bucketSettings = schedule.buckets[i];
                    WifiScanningServiceImpl.this.localLog("bucket " + bucketSettings.bucket + " (" + bucketSettings.period_ms + "ms)[" + bucketSettings.report_events + "]: " + ChannelHelper.toString(bucketSettings));
                }
                if (WifiScanningServiceImpl.this.mScannerImpl.startBatchedScan(schedule, this)) {
                    return true;
                }
                WifiScanningServiceImpl.this.mPreviousSchedule = null;
                loge("error starting scan: base period=" + schedule.base_period_ms + ", max ap per scan=" + schedule.max_ap_per_scan + ", batched scans=" + schedule.report_threshold_num_scans);
                for (int i2 = 0; i2 < schedule.num_buckets; i2++) {
                    WifiNative.BucketSettings bucketSettings2 = schedule.buckets[i2];
                    loge("bucket " + bucketSettings2.bucket + " (" + bucketSettings2.period_ms + "ms)[" + bucketSettings2.report_events + "]: " + ChannelHelper.toString(bucketSettings2));
                }
                return WifiScanningServiceImpl.DBG;
            }
            loge("Failed to update schedule because WifiScanningService is not initialized");
            return WifiScanningServiceImpl.DBG;
        }

        private void removeBackgroundScanRequest(ClientInfo clientInfo, int i) {
            if (clientInfo != null) {
                WifiScanningServiceImpl.this.logScanRequest("removeBackgroundScanRequest", clientInfo, i, null, this.mActiveBackgroundScans.removeRequest(clientInfo, i), null);
                updateSchedule();
            }
        }

        private void reportFullScanResult(ScanResult scanResult, int i) {
            Iterator<RequestInfo<T>> it = this.mActiveBackgroundScans.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                ClientInfo clientInfo = requestInfo.clientInfo;
                int i2 = requestInfo.handlerId;
                if (WifiScanningServiceImpl.this.mBackgroundScheduler.shouldReportFullScanResultForSettings(scanResult, i, (WifiScanner.ScanSettings) requestInfo.settings)) {
                    ScanResult scanResult2 = new ScanResult(scanResult);
                    if (scanResult.informationElements != null) {
                        scanResult2.informationElements = (ScanResult.InformationElement[]) scanResult.informationElements.clone();
                    } else {
                        scanResult2.informationElements = null;
                    }
                    clientInfo.reportEvent(159764, 0, i2, scanResult2);
                }
            }
        }

        private void reportScanResults(WifiScanner.ScanData[] scanDataArr) {
            if (scanDataArr == null) {
                Log.d(WifiScanningServiceImpl.TAG, "The results is null, nothing to report.");
                return;
            }
            for (WifiScanner.ScanData scanData : scanDataArr) {
                if (scanData != null && scanData.getResults() != null) {
                    if (scanData.getResults().length > 0) {
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementNonEmptyScanResultCount();
                    } else {
                        WifiScanningServiceImpl.this.mWifiMetrics.incrementEmptyScanResultCount();
                    }
                }
            }
            Iterator<RequestInfo<T>> it = this.mActiveBackgroundScans.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                ClientInfo clientInfo = requestInfo.clientInfo;
                int i = requestInfo.handlerId;
                WifiScanner.ScanData[] scanDataArrFilterResultsForSettings = WifiScanningServiceImpl.this.mBackgroundScheduler.filterResultsForSettings(scanDataArr, (WifiScanner.ScanSettings) requestInfo.settings);
                if (scanDataArrFilterResultsForSettings != null) {
                    WifiScanningServiceImpl.this.logCallback("backgroundScanResults", clientInfo, i, WifiScanningServiceImpl.describeForLog(scanDataArrFilterResultsForSettings));
                    clientInfo.reportEvent(159749, 0, i, new WifiScanner.ParcelableScanData(scanDataArrFilterResultsForSettings));
                }
            }
        }

        private void sendBackgroundScanFailedToAllAndClear(int i, String str) {
            Iterator<RequestInfo<T>> it = this.mActiveBackgroundScans.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                requestInfo.clientInfo.reportEvent(159762, 0, requestInfo.handlerId, new WifiScanner.OperationResult(i, str));
            }
            this.mActiveBackgroundScans.clear();
        }
    }

    class WifiPnoScanStateMachine extends StateMachine implements WifiNative.PnoEventHandler {
        private final RequestList<Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings>> mActivePnoScans;
        private final DefaultState mDefaultState;
        private final HwPnoScanState mHwPnoScanState;
        private InternalClientInfo mInternalClientInfo;
        private final SingleScanState mSingleScanState;
        private final StartedState mStartedState;

        WifiPnoScanStateMachine(Looper looper) {
            super("WifiPnoScanStateMachine", looper);
            this.mDefaultState = new DefaultState();
            this.mStartedState = new StartedState();
            this.mHwPnoScanState = new HwPnoScanState();
            this.mSingleScanState = new SingleScanState();
            this.mActivePnoScans = new RequestList<>();
            setLogRecSize(256);
            setLogOnlyTransitions(WifiScanningServiceImpl.DBG);
            addState(this.mDefaultState);
            addState(this.mStartedState, this.mDefaultState);
            addState(this.mHwPnoScanState, this.mStartedState);
            addState(this.mSingleScanState, this.mHwPnoScanState);
            setInitialState(this.mDefaultState);
        }

        public void removePnoSettings(ClientInfo clientInfo) {
            this.mActivePnoScans.removeAllForClient(clientInfo);
            transitionTo(this.mStartedState);
        }

        @Override
        public void onPnoNetworkFound(ScanResult[] scanResultArr) {
            sendMessage(WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND, 0, 0, scanResultArr);
        }

        @Override
        public void onPnoScanFailed() {
            sendMessage(WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED, 0, 0, null);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 159749:
                    case 159762:
                    case WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND:
                    case WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED:
                        WifiPnoScanStateMachine.this.loge("Unexpected message " + message.what);
                        return true;
                    case 159768:
                    case 159769:
                        WifiScanningServiceImpl.this.replyFailed(message, -1, "not available");
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        if (WifiScanningServiceImpl.this.mScannerImpl == null) {
                            WifiPnoScanStateMachine.this.loge("Failed to start pno scan state machine because scanner impl is null");
                            return true;
                        }
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mDefaultState);
                        return true;
                    default:
                        return WifiScanningServiceImpl.DBG;
                }
            }
        }

        class StartedState extends State {
            StartedState() {
            }

            public void enter() {
            }

            public void exit() {
                WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "Scan was interrupted");
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i == WifiScanningServiceImpl.CMD_DRIVER_LOADED) {
                    return true;
                }
                switch (i) {
                    case 159768:
                        Bundle bundle = (Bundle) message.obj;
                        if (bundle == null) {
                            WifiScanningServiceImpl.this.replyFailed(message, -3, "params null");
                            return true;
                        }
                        bundle.setDefusable(true);
                        if (!WifiScanningServiceImpl.this.mScannerImpl.isHwPnoSupported(bundle.getParcelable("PnoSettings").isConnected)) {
                            WifiScanningServiceImpl.this.replyFailed(message, -3, "not supported");
                        } else {
                            WifiPnoScanStateMachine.this.deferMessage(message);
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mHwPnoScanState);
                        }
                        return true;
                    case 159769:
                        WifiScanningServiceImpl.this.replyFailed(message, -1, "no scan running");
                        return true;
                    default:
                        return WifiScanningServiceImpl.DBG;
                }
            }
        }

        class HwPnoScanState extends State {
            HwPnoScanState() {
            }

            public void enter() {
            }

            public void exit() {
                WifiScanningServiceImpl.this.mScannerImpl.resetHwPnoList();
                WifiPnoScanStateMachine.this.removeInternalClient();
            }

            public boolean processMessage(Message message) {
                ClientInfo clientInfo = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(message.replyTo);
                switch (message.what) {
                    case 159768:
                        Bundle bundle = (Bundle) message.obj;
                        if (bundle == null) {
                            WifiScanningServiceImpl.this.replyFailed(message, -3, "params null");
                            return true;
                        }
                        bundle.setDefusable(true);
                        WifiScanner.PnoSettings parcelable = bundle.getParcelable("PnoSettings");
                        if (!WifiPnoScanStateMachine.this.addHwPnoScanRequest(clientInfo, message.arg2, bundle.getParcelable("ScanSettings"), parcelable)) {
                            WifiScanningServiceImpl.this.replyFailed(message, -3, "bad request");
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        } else {
                            WifiScanningServiceImpl.this.replySucceeded(message);
                        }
                        return true;
                    case 159769:
                        WifiPnoScanStateMachine.this.removeHwPnoScanRequest(clientInfo, message.arg2);
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    case WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND:
                        if (WifiPnoScanStateMachine.this.isSingleScanNeeded((ScanResult[]) message.obj)) {
                            WifiScanner.ScanSettings scanSettings = WifiPnoScanStateMachine.this.getScanSettings();
                            if (scanSettings == null) {
                                WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "couldn't retrieve setting");
                                WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                            } else {
                                WifiPnoScanStateMachine.this.addSingleScanRequest(scanSettings);
                                WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mSingleScanState);
                            }
                        } else {
                            WifiPnoScanStateMachine.this.reportPnoNetworkFound((ScanResult[]) message.obj);
                        }
                        return true;
                    case WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED:
                        WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "pno scan failed");
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    default:
                        return WifiScanningServiceImpl.DBG;
                }
            }
        }

        class SingleScanState extends State {
            SingleScanState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i == 159749) {
                    WifiScanner.ScanData[] results = ((WifiScanner.ParcelableScanData) message.obj).getResults();
                    WifiPnoScanStateMachine.this.reportPnoNetworkFound(results[results.length - 1].getResults());
                    WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mHwPnoScanState);
                } else if (i == 159762) {
                    WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "single scan failed");
                    WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                } else {
                    return WifiScanningServiceImpl.DBG;
                }
                return true;
            }
        }

        private WifiNative.PnoSettings convertSettingsToPnoNative(WifiScanner.ScanSettings scanSettings, WifiScanner.PnoSettings pnoSettings) {
            WifiNative.PnoSettings pnoSettings2 = new WifiNative.PnoSettings();
            pnoSettings2.periodInMs = scanSettings.periodInMs;
            pnoSettings2.min5GHzRssi = pnoSettings.min5GHzRssi;
            pnoSettings2.min24GHzRssi = pnoSettings.min24GHzRssi;
            pnoSettings2.initialScoreMax = pnoSettings.initialScoreMax;
            pnoSettings2.currentConnectionBonus = pnoSettings.currentConnectionBonus;
            pnoSettings2.sameNetworkBonus = pnoSettings.sameNetworkBonus;
            pnoSettings2.secureBonus = pnoSettings.secureBonus;
            pnoSettings2.band5GHzBonus = pnoSettings.band5GHzBonus;
            pnoSettings2.isConnected = pnoSettings.isConnected;
            pnoSettings2.networkList = new WifiNative.PnoNetwork[pnoSettings.networkList.length];
            for (int i = 0; i < pnoSettings.networkList.length; i++) {
                pnoSettings2.networkList[i] = new WifiNative.PnoNetwork();
                pnoSettings2.networkList[i].ssid = pnoSettings.networkList[i].ssid;
                pnoSettings2.networkList[i].flags = pnoSettings.networkList[i].flags;
                pnoSettings2.networkList[i].auth_bit_field = pnoSettings.networkList[i].authBitField;
            }
            return pnoSettings2;
        }

        private WifiScanner.ScanSettings getScanSettings() {
            Iterator<Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings>> it = this.mActivePnoScans.getAllSettings().iterator();
            if (it.hasNext()) {
                return (WifiScanner.ScanSettings) it.next().second;
            }
            return null;
        }

        private void removeInternalClient() {
            if (this.mInternalClientInfo != null) {
                this.mInternalClientInfo.cleanup();
                this.mInternalClientInfo = null;
            } else {
                Log.w(WifiScanningServiceImpl.TAG, "No Internal client for PNO");
            }
        }

        private void addInternalClient(ClientInfo clientInfo) {
            if (this.mInternalClientInfo == null) {
                this.mInternalClientInfo = WifiScanningServiceImpl.this.new InternalClientInfo(clientInfo.getUid(), new Messenger(getHandler()));
                this.mInternalClientInfo.register();
            } else {
                Log.w(WifiScanningServiceImpl.TAG, "Internal client for PNO already exists");
            }
        }

        private void addPnoScanRequest(ClientInfo clientInfo, int i, WifiScanner.ScanSettings scanSettings, WifiScanner.PnoSettings pnoSettings) {
            this.mActivePnoScans.addRequest(clientInfo, i, WifiStateMachine.WIFI_WORK_SOURCE, Pair.create(pnoSettings, scanSettings));
            addInternalClient(clientInfo);
        }

        private Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings> removePnoScanRequest(ClientInfo clientInfo, int i) {
            return this.mActivePnoScans.removeRequest(clientInfo, i);
        }

        private boolean addHwPnoScanRequest(ClientInfo clientInfo, int i, WifiScanner.ScanSettings scanSettings, WifiScanner.PnoSettings pnoSettings) {
            if (clientInfo == null) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing scan request ClientInfo not found " + i);
                return WifiScanningServiceImpl.DBG;
            }
            if (!this.mActivePnoScans.isEmpty()) {
                loge("Failing scan request because there is already an active scan");
                return WifiScanningServiceImpl.DBG;
            }
            if (!WifiScanningServiceImpl.this.mScannerImpl.setHwPnoList(convertSettingsToPnoNative(scanSettings, pnoSettings), WifiScanningServiceImpl.this.mPnoScanStateMachine)) {
                return WifiScanningServiceImpl.DBG;
            }
            WifiScanningServiceImpl.this.logScanRequest("addHwPnoScanRequest", clientInfo, i, null, scanSettings, pnoSettings);
            addPnoScanRequest(clientInfo, i, scanSettings, pnoSettings);
            return true;
        }

        private void removeHwPnoScanRequest(ClientInfo clientInfo, int i) {
            if (clientInfo != null) {
                Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings> pairRemovePnoScanRequest = removePnoScanRequest(clientInfo, i);
                WifiScanningServiceImpl.this.logScanRequest("removeHwPnoScanRequest", clientInfo, i, null, (WifiScanner.ScanSettings) pairRemovePnoScanRequest.second, (WifiScanner.PnoSettings) pairRemovePnoScanRequest.first);
            }
        }

        private void reportPnoNetworkFound(ScanResult[] scanResultArr) {
            WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(scanResultArr);
            Iterator<RequestInfo<T>> it = this.mActivePnoScans.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                ClientInfo clientInfo = requestInfo.clientInfo;
                int i = requestInfo.handlerId;
                WifiScanningServiceImpl.this.logCallback("pnoNetworkFound", clientInfo, i, WifiScanningServiceImpl.describeForLog(scanResultArr));
                clientInfo.reportEvent(159770, 0, i, parcelableScanResults);
            }
        }

        private void sendPnoScanFailedToAllAndClear(int i, String str) {
            Iterator<RequestInfo<T>> it = this.mActivePnoScans.iterator();
            while (it.hasNext()) {
                RequestInfo requestInfo = (RequestInfo) it.next();
                requestInfo.clientInfo.reportEvent(159762, 0, requestInfo.handlerId, new WifiScanner.OperationResult(i, str));
            }
            this.mActivePnoScans.clear();
        }

        private void addSingleScanRequest(WifiScanner.ScanSettings scanSettings) {
            if (this.mInternalClientInfo != null) {
                this.mInternalClientInfo.sendRequestToClientHandler(159765, scanSettings, WifiStateMachine.WIFI_WORK_SOURCE);
            }
        }

        private boolean isSingleScanNeeded(ScanResult[] scanResultArr) {
            for (ScanResult scanResult : scanResultArr) {
                if (scanResult.informationElements != null && scanResult.informationElements.length > 0) {
                    return WifiScanningServiceImpl.DBG;
                }
            }
            return true;
        }
    }

    private abstract class ClientInfo {
        protected final Messenger mMessenger;
        private boolean mScanWorkReported = WifiScanningServiceImpl.DBG;
        private final int mUid;
        private final WorkSource mWorkSource;

        public abstract void reportEvent(int i, int i2, int i3, Object obj);

        ClientInfo(int i, Messenger messenger) {
            this.mUid = i;
            this.mMessenger = messenger;
            this.mWorkSource = new WorkSource(i);
        }

        public void register() {
            WifiScanningServiceImpl.this.mClients.put(this.mMessenger, this);
        }

        private void unregister() {
            WifiScanningServiceImpl.this.mClients.remove(this.mMessenger);
        }

        public void cleanup() {
            WifiScanningServiceImpl.this.mSingleScanListeners.removeAllForClient(this);
            WifiScanningServiceImpl.this.mSingleScanStateMachine.removeSingleScanRequests(this);
            WifiScanningServiceImpl.this.mBackgroundScanStateMachine.removeBackgroundScanSettings(this);
            unregister();
            WifiScanningServiceImpl.this.localLog("Successfully stopped all requests for client " + this);
        }

        public int getUid() {
            return this.mUid;
        }

        public void reportEvent(int i, int i2, int i3) {
            reportEvent(i, i2, i3, null);
        }

        private void reportBatchedScanStart() {
            if (this.mUid == 0) {
                return;
            }
            try {
                WifiScanningServiceImpl.this.mBatteryStats.noteWifiBatchedScanStartedFromSource(this.mWorkSource, getCsph());
            } catch (RemoteException e) {
                WifiScanningServiceImpl.this.logw("failed to report scan work: " + e.toString());
            }
        }

        private void reportBatchedScanStop() {
            if (this.mUid != 0) {
                try {
                    WifiScanningServiceImpl.this.mBatteryStats.noteWifiBatchedScanStoppedFromSource(this.mWorkSource);
                } catch (RemoteException e) {
                    WifiScanningServiceImpl.this.logw("failed to cleanup scan work: " + e.toString());
                }
            }
        }

        private int getCsph() {
            int iEstimateScanDuration = 0;
            for (WifiScanner.ScanSettings scanSettings : WifiScanningServiceImpl.this.mBackgroundScanStateMachine.getBackgroundScanSettings(this)) {
                iEstimateScanDuration += WifiScanningServiceImpl.this.mChannelHelper.estimateScanDuration(scanSettings) * (scanSettings.periodInMs == 0 ? 1 : 3600000 / scanSettings.periodInMs);
            }
            return iEstimateScanDuration / ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS;
        }

        private void reportScanWorkUpdate() {
            if (this.mScanWorkReported) {
                reportBatchedScanStop();
                this.mScanWorkReported = WifiScanningServiceImpl.DBG;
            }
            if (WifiScanningServiceImpl.this.mBackgroundScanStateMachine.getBackgroundScanSettings(this).isEmpty()) {
                reportBatchedScanStart();
                this.mScanWorkReported = true;
            }
        }

        public String toString() {
            return "ClientInfo[uid=" + this.mUid + "," + this.mMessenger + "]";
        }
    }

    private class ExternalClientInfo extends ClientInfo {
        private final AsyncChannel mChannel;
        private boolean mDisconnected;

        ExternalClientInfo(int i, Messenger messenger, AsyncChannel asyncChannel) {
            super(i, messenger);
            this.mDisconnected = WifiScanningServiceImpl.DBG;
            this.mChannel = asyncChannel;
        }

        @Override
        public void reportEvent(int i, int i2, int i3, Object obj) {
            if (!this.mDisconnected) {
                this.mChannel.sendMessage(i, i2, i3, obj);
            }
        }

        @Override
        public void cleanup() {
            this.mDisconnected = true;
            WifiScanningServiceImpl.this.mPnoScanStateMachine.removePnoSettings(this);
            super.cleanup();
        }
    }

    private class InternalClientInfo extends ClientInfo {
        private static final int INTERNAL_CLIENT_HANDLER = 0;

        InternalClientInfo(int i, Messenger messenger) {
            super(i, messenger);
        }

        @Override
        public void reportEvent(int i, int i2, int i3, Object obj) {
            Message messageObtain = Message.obtain();
            messageObtain.what = i;
            messageObtain.arg1 = i2;
            messageObtain.arg2 = i3;
            messageObtain.obj = obj;
            try {
                this.mMessenger.send(messageObtain);
            } catch (RemoteException e) {
                WifiScanningServiceImpl.this.loge("Failed to send message: " + i);
            }
        }

        public void sendRequestToClientHandler(int i, WifiScanner.ScanSettings scanSettings, WorkSource workSource) {
            Message messageObtain = Message.obtain();
            messageObtain.what = i;
            messageObtain.arg2 = 0;
            if (scanSettings != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("ScanSettings", scanSettings);
                bundle.putParcelable("WorkSource", workSource);
                messageObtain.obj = bundle;
            }
            messageObtain.replyTo = this.mMessenger;
            messageObtain.sendingUid = getUid();
            WifiScanningServiceImpl.this.mClientHandler.sendMessage(messageObtain);
        }

        public void sendRequestToClientHandler(int i) {
            sendRequestToClientHandler(i, null, null);
        }

        @Override
        public String toString() {
            return "InternalClientInfo[]";
        }
    }

    void replySucceeded(Message message) {
        if (message.replyTo != null) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 159761;
            messageObtain.arg2 = message.arg2;
            if (message.obj != null) {
                messageObtain.obj = message.obj;
            }
            try {
                message.replyTo.send(messageObtain);
                this.mLog.trace("replySucceeded recvdMessage=%").c(message.what).flush();
            } catch (RemoteException e) {
            }
        }
    }

    void replyFailed(Message message, int i, String str) {
        if (message.replyTo != null) {
            Message messageObtain = Message.obtain();
            messageObtain.what = 159762;
            messageObtain.arg2 = message.arg2;
            messageObtain.obj = new WifiScanner.OperationResult(i, str);
            try {
                message.replyTo.send(messageObtain);
                this.mLog.trace("replyFailed recvdMessage=% reason=%").c(message.what).c(i).flush();
            } catch (RemoteException e) {
            }
        }
    }

    private static String toString(int i, WifiScanner.ScanSettings scanSettings) {
        StringBuilder sb = new StringBuilder();
        sb.append("ScanSettings[uid=");
        sb.append(i);
        sb.append(", period=");
        sb.append(scanSettings.periodInMs);
        sb.append(", report=");
        sb.append(scanSettings.reportEvents);
        if (scanSettings.reportEvents == 0 && scanSettings.numBssidsPerScan > 0 && scanSettings.maxScansToCache > 1) {
            sb.append(", batch=");
            sb.append(scanSettings.maxScansToCache);
            sb.append(", numAP=");
            sb.append(scanSettings.numBssidsPerScan);
        }
        sb.append(", ");
        sb.append(ChannelHelper.toString(scanSettings));
        sb.append("]");
        return sb.toString();
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        WifiNative.ScanSettings schedule;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump WifiScanner from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        printWriter.println("WifiScanningService - Log Begin ----");
        this.mLocalLog.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("WifiScanningService - Log End ----");
        printWriter.println();
        printWriter.println("clients:");
        Iterator<ClientInfo> it = this.mClients.values().iterator();
        while (it.hasNext()) {
            printWriter.println("  " + it.next());
        }
        printWriter.println("listeners:");
        for (ClientInfo clientInfo : this.mClients.values()) {
            Iterator<WifiScanner.ScanSettings> it2 = this.mBackgroundScanStateMachine.getBackgroundScanSettings(clientInfo).iterator();
            while (it2.hasNext()) {
                printWriter.println("  " + toString(clientInfo.mUid, it2.next()));
            }
        }
        if (this.mBackgroundScheduler != null && (schedule = this.mBackgroundScheduler.getSchedule()) != null) {
            printWriter.println("schedule:");
            printWriter.println("  base period: " + schedule.base_period_ms);
            printWriter.println("  max ap per scan: " + schedule.max_ap_per_scan);
            printWriter.println("  batched scans: " + schedule.report_threshold_num_scans);
            printWriter.println("  buckets:");
            for (int i = 0; i < schedule.num_buckets; i++) {
                WifiNative.BucketSettings bucketSettings = schedule.buckets[i];
                printWriter.println("    bucket " + bucketSettings.bucket + " (" + bucketSettings.period_ms + "ms)[" + bucketSettings.report_events + "]: " + ChannelHelper.toString(bucketSettings));
            }
        }
        if (this.mPnoScanStateMachine != null) {
            this.mPnoScanStateMachine.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println();
        if (this.mSingleScanStateMachine != null) {
            this.mSingleScanStateMachine.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
            printWriter.println("Latest scan results:");
            ScanResultUtil.dumpScanResults(printWriter, this.mSingleScanStateMachine.getCachedScanResultsAsList(), this.mClock.getElapsedSinceBootMillis());
            printWriter.println();
        }
        if (this.mScannerImpl != null) {
            this.mScannerImpl.dump(fileDescriptor, printWriter, strArr);
        }
    }

    void logScanRequest(String str, ClientInfo clientInfo, int i, WorkSource workSource, WifiScanner.ScanSettings scanSettings, WifiScanner.PnoSettings pnoSettings) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(": ");
        sb.append(clientInfo == null ? "ClientInfo[unknown]" : clientInfo.toString());
        sb.append(",Id=");
        sb.append(i);
        if (workSource != null) {
            sb.append(",");
            sb.append(workSource);
        }
        if (scanSettings != null) {
            sb.append(", ");
            describeTo(sb, scanSettings);
        }
        if (pnoSettings != null) {
            sb.append(", ");
            describeTo(sb, pnoSettings);
        }
        localLog(sb.toString());
    }

    void logCallback(String str, ClientInfo clientInfo, int i, String str2) {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        sb.append(": ");
        sb.append(clientInfo == null ? "ClientInfo[unknown]" : clientInfo.toString());
        sb.append(",Id=");
        sb.append(i);
        if (str2 != null) {
            sb.append(",");
            sb.append(str2);
        }
        localLog(sb.toString());
    }

    static String describeForLog(WifiScanner.ScanData[] scanDataArr) {
        StringBuilder sb = new StringBuilder();
        sb.append("results=");
        for (int i = 0; i < scanDataArr.length; i++) {
            if (i > 0) {
                sb.append(NAIRealmData.NAI_REALM_STRING_SEPARATOR);
            }
            sb.append(scanDataArr[i].getResults().length);
        }
        return sb.toString();
    }

    static String describeForLog(ScanResult[] scanResultArr) {
        return "results=" + scanResultArr.length;
    }

    static String getScanTypeString(int i) {
        switch (i) {
            case 0:
                return "LOW LATENCY";
            case 1:
                return "LOW POWER";
            case 2:
                return "HIGH ACCURACY";
            default:
                throw new IllegalArgumentException("Invalid scan type " + i);
        }
    }

    static String describeTo(StringBuilder sb, WifiScanner.ScanSettings scanSettings) {
        sb.append("ScanSettings { ");
        sb.append(" type:");
        sb.append(getScanTypeString(scanSettings.type));
        sb.append(" band:");
        sb.append(ChannelHelper.bandToString(scanSettings.band));
        sb.append(" period:");
        sb.append(scanSettings.periodInMs);
        sb.append(" reportEvents:");
        sb.append(scanSettings.reportEvents);
        sb.append(" numBssidsPerScan:");
        sb.append(scanSettings.numBssidsPerScan);
        sb.append(" maxScansToCache:");
        sb.append(scanSettings.maxScansToCache);
        sb.append(" channels:[ ");
        if (scanSettings.channels != null) {
            for (int i = 0; i < scanSettings.channels.length; i++) {
                sb.append(scanSettings.channels[i].frequency);
                sb.append(" ");
            }
        }
        sb.append(" ] ");
        sb.append(" hiddenNetworks:[ ");
        if (scanSettings.hiddenNetworks != null) {
            for (int i2 = 0; i2 < scanSettings.hiddenNetworks.length; i2++) {
                sb.append(scanSettings.hiddenNetworks[i2].ssid);
                sb.append(" ");
            }
        }
        sb.append(" ] ");
        sb.append(" } ");
        return sb.toString();
    }

    static String describeTo(StringBuilder sb, WifiScanner.PnoSettings pnoSettings) {
        sb.append("PnoSettings { ");
        sb.append(" min5GhzRssi:");
        sb.append(pnoSettings.min5GHzRssi);
        sb.append(" min24GhzRssi:");
        sb.append(pnoSettings.min24GHzRssi);
        sb.append(" initialScoreMax:");
        sb.append(pnoSettings.initialScoreMax);
        sb.append(" currentConnectionBonus:");
        sb.append(pnoSettings.currentConnectionBonus);
        sb.append(" sameNetworkBonus:");
        sb.append(pnoSettings.sameNetworkBonus);
        sb.append(" secureBonus:");
        sb.append(pnoSettings.secureBonus);
        sb.append(" band5GhzBonus:");
        sb.append(pnoSettings.band5GHzBonus);
        sb.append(" isConnected:");
        sb.append(pnoSettings.isConnected);
        sb.append(" networks:[ ");
        if (pnoSettings.networkList != null) {
            for (int i = 0; i < pnoSettings.networkList.length; i++) {
                sb.append(pnoSettings.networkList[i].ssid);
                sb.append(",");
            }
        }
        sb.append(" ] ");
        sb.append(" } ");
        return sb.toString();
    }
}
