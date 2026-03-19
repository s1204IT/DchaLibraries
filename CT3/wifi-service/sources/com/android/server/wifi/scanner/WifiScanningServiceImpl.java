package com.android.server.wifi.scanner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.IWifiScanner;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.Clock;
import com.android.server.wifi.WifiConnectivityManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.scanner.ChannelHelper;
import com.android.server.wifi.scanner.WifiScannerImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class WifiScanningServiceImpl extends IWifiScanner.Stub {
    private static final int BASE = 160000;
    private static final int CMD_DRIVER_LOADED = 160006;
    private static final int CMD_DRIVER_UNLOADED = 160007;
    private static final int CMD_FULL_SCAN_RESULTS = 160001;
    private static final int CMD_HOTLIST_AP_FOUND = 160002;
    private static final int CMD_HOTLIST_AP_LOST = 160003;
    private static final int CMD_PNO_NETWORK_FOUND = 160011;
    private static final int CMD_PNO_SCAN_FAILED = 160012;
    private static final int CMD_SCAN_FAILED = 160010;
    private static final int CMD_SCAN_PAUSED = 160008;
    private static final int CMD_SCAN_RESTARTED = 160009;
    private static final int CMD_SCAN_RESULTS_AVAILABLE = 160000;
    private static final int CMD_WIFI_CHANGE_DETECTED = 160004;
    private static final int CMD_WIFI_CHANGE_TIMEOUT = 160005;
    private static final boolean DBG = true;
    private static final int MIN_PERIOD_PER_CHANNEL_MS = 200;
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
    private final Looper mLooper;
    private WifiPnoScanStateMachine mPnoScanStateMachine;
    private WifiScannerImpl mScannerImpl;
    private final WifiScannerImpl.WifiScannerImplFactory mScannerImplFactory;
    private WifiSingleScanStateMachine mSingleScanStateMachine;
    private WifiChangeStateMachine mWifiChangeStateMachine;
    private final WifiMetrics mWifiMetrics;
    private final LocalLog mLocalLog = new LocalLog(1024);
    private final ArrayMap<Messenger, ClientInfo> mClients = new ArrayMap<>();
    private WifiNative.ScanSettings mPreviousSchedule = null;

    private void localLog(String message) {
        this.mLocalLog.log(message);
        Log.d(TAG, message);
    }

    private void logw(String message) {
        Log.w(TAG, message);
        this.mLocalLog.log(message);
    }

    private void loge(String message) {
        Log.e(TAG, message);
        this.mLocalLog.log(message);
    }

    public Messenger getMessenger() {
        if (this.mClientHandler != null) {
            return new Messenger(this.mClientHandler);
        }
        loge("WifiScanningServiceImpl trying to get messenger w/o initialization");
        return null;
    }

    public Bundle getAvailableChannels(int band) {
        this.mChannelHelper.updateChannels();
        WifiScanner.ChannelSpec[] channelSpecs = this.mChannelHelper.getAvailableScanChannels(band);
        ArrayList<Integer> list = new ArrayList<>(channelSpecs.length);
        for (WifiScanner.ChannelSpec channelSpec : channelSpecs) {
            list.add(Integer.valueOf(channelSpec.frequency));
        }
        Bundle b = new Bundle();
        b.putIntegerArrayList("Channels", list);
        return b;
    }

    private void enforceLocationHardwarePermission(int uid) {
        this.mContext.enforcePermission("android.permission.LOCATION_HARDWARE", -1, uid, "LocationHardware");
    }

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 69633:
                    ExternalClientInfo client = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                    if (client != null) {
                        WifiScanningServiceImpl.this.logw("duplicate client connection: " + msg.sendingUid);
                        client.mChannel.replyToMessage(msg, 69634, 3);
                    } else {
                        AsyncChannel ac = new AsyncChannel();
                        ac.connected(WifiScanningServiceImpl.this.mContext, this, msg.replyTo);
                        ExternalClientInfo client2 = WifiScanningServiceImpl.this.new ExternalClientInfo(msg.sendingUid, msg.replyTo, ac);
                        client2.register();
                        ac.replyToMessage(msg, 69634, 0);
                        Log.d(WifiScanningServiceImpl.TAG, "client connected: " + client2);
                    }
                    break;
                case 69634:
                default:
                    try {
                        WifiScanningServiceImpl.this.enforceLocationHardwarePermission(msg.sendingUid);
                        if (msg.what == 159748) {
                            WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(Message.obtain(msg));
                            break;
                        } else {
                            ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                            if (ci == null) {
                                WifiScanningServiceImpl.this.loge("Could not find client info for message " + msg.replyTo);
                                WifiScanningServiceImpl.this.replyFailed(msg, -2, "Could not find listener");
                                break;
                            } else {
                                switch (msg.what) {
                                    case 159746:
                                    case 159747:
                                    case 159750:
                                    case 159751:
                                        WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(Message.obtain(msg));
                                        break;
                                    case 159748:
                                    case 159749:
                                    case 159752:
                                    case 159753:
                                    case 159754:
                                    case 159758:
                                    case 159759:
                                    case 159760:
                                    case 159761:
                                    case 159762:
                                    case 159763:
                                    case 159764:
                                    case 159767:
                                    default:
                                        WifiScanningServiceImpl.this.replyFailed(msg, -3, "Invalid request");
                                        break;
                                    case 159755:
                                    case 159756:
                                    case 159757:
                                        WifiScanningServiceImpl.this.mWifiChangeStateMachine.sendMessage(Message.obtain(msg));
                                        break;
                                    case 159765:
                                    case 159766:
                                        WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(Message.obtain(msg));
                                        break;
                                    case 159768:
                                    case 159769:
                                        WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(Message.obtain(msg));
                                        break;
                                }
                            }
                        }
                    } catch (SecurityException e) {
                        WifiScanningServiceImpl.this.localLog("failed to authorize app: " + e);
                        WifiScanningServiceImpl.this.replyFailed(msg, -4, "Not authorized");
                        return;
                    }
                    break;
                case 69635:
                    ExternalClientInfo client3 = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                    if (client3 != null) {
                        client3.mChannel.disconnect();
                    }
                    break;
                case 69636:
                    ExternalClientInfo client4 = (ExternalClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                    if (client4 != null) {
                        Log.d(WifiScanningServiceImpl.TAG, "client disconnected: " + client4 + ", reason: " + msg.arg1);
                        client4.cleanup();
                    }
                    break;
            }
        }
    }

    WifiScanningServiceImpl(Context context, Looper looper, WifiScannerImpl.WifiScannerImplFactory scannerImplFactory, IBatteryStats batteryStats, WifiInjector wifiInjector) {
        this.mContext = context;
        this.mLooper = looper;
        this.mScannerImplFactory = scannerImplFactory;
        this.mBatteryStats = batteryStats;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mWifiMetrics = wifiInjector.getWifiMetrics();
        this.mClock = wifiInjector.getClock();
    }

    public void startService() {
        this.mClientHandler = new ClientHandler(this.mLooper);
        this.mBackgroundScanStateMachine = new WifiBackgroundScanStateMachine(this, this.mLooper);
        this.mWifiChangeStateMachine = new WifiChangeStateMachine(this.mLooper);
        this.mSingleScanStateMachine = new WifiSingleScanStateMachine(this, this.mLooper);
        this.mPnoScanStateMachine = new WifiPnoScanStateMachine(this.mLooper);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra("scan_enabled", 1);
                WifiScanningServiceImpl.this.localLog("SCAN_AVAILABLE : " + state);
                if (state == 3) {
                    WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                    WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                } else {
                    if (state != 1) {
                        return;
                    }
                    WifiScanningServiceImpl.this.mBackgroundScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                    WifiScanningServiceImpl.this.mSingleScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                    WifiScanningServiceImpl.this.mPnoScanStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                }
            }
        }, new IntentFilter("wifi_scan_available"));
        this.mBackgroundScanStateMachine.start();
        this.mWifiChangeStateMachine.start();
        this.mSingleScanStateMachine.start();
        this.mPnoScanStateMachine.start();
    }

    private static boolean isWorkSourceValid(WorkSource workSource) {
        return workSource != null && workSource.size() > 0 && workSource.get(0) >= 0;
    }

    private WorkSource computeWorkSource(ClientInfo ci, WorkSource requestedWorkSource) {
        if (requestedWorkSource != null) {
            if (isWorkSourceValid(requestedWorkSource)) {
                requestedWorkSource.clearNames();
                return requestedWorkSource;
            }
            loge("Got invalid work source request: " + requestedWorkSource.toString() + " from " + ci);
        }
        WorkSource callingWorkSource = new WorkSource(ci.getUid());
        if (isWorkSourceValid(callingWorkSource)) {
            return callingWorkSource;
        }
        loge("Client has invalid work source: " + callingWorkSource);
        return new WorkSource();
    }

    private class RequestInfo<T> {
        final ClientInfo clientInfo;
        final int handlerId;
        final T settings;
        final WorkSource workSource;

        RequestInfo(ClientInfo clientInfo, int handlerId, WorkSource requestedWorkSource, T settings) {
            this.clientInfo = clientInfo;
            this.handlerId = handlerId;
            this.settings = settings;
            this.workSource = WifiScanningServiceImpl.this.computeWorkSource(clientInfo, requestedWorkSource);
        }

        void reportEvent(int what, int arg1, Object obj) {
            this.clientInfo.reportEvent(what, arg1, this.handlerId, obj);
        }
    }

    private class RequestList<T> extends ArrayList<RequestInfo<T>> {
        RequestList(WifiScanningServiceImpl this$0, RequestList requestList) {
            this();
        }

        private RequestList() {
        }

        void addRequest(ClientInfo ci, int handler, WorkSource reqworkSource, T settings) {
            add(WifiScanningServiceImpl.this.new RequestInfo(ci, handler, reqworkSource, settings));
        }

        T removeRequest(ClientInfo ci, int handlerId) {
            T removed = null;
            Iterator<RequestInfo<T>> iter = iterator();
            while (iter.hasNext()) {
                RequestInfo<T> entry = iter.next();
                if (entry.clientInfo == ci && entry.handlerId == handlerId) {
                    removed = entry.settings;
                    iter.remove();
                }
            }
            return removed;
        }

        Collection<T> getAllSettings() {
            ArrayList<T> settingsList = new ArrayList<>();
            for (RequestInfo<T> entry : this) {
                settingsList.add(entry.settings);
            }
            return settingsList;
        }

        Collection<T> getAllSettingsForClient(ClientInfo ci) {
            ArrayList<T> settingsList = new ArrayList<>();
            for (RequestInfo<T> entry : this) {
                if (entry.clientInfo == ci) {
                    settingsList.add(entry.settings);
                }
            }
            return settingsList;
        }

        void removeAllForClient(ClientInfo ci) {
            Iterator<RequestInfo<T>> iter = iterator();
            while (iter.hasNext()) {
                RequestInfo<T> entry = iter.next();
                if (entry.clientInfo == ci) {
                    iter.remove();
                }
            }
        }

        WorkSource createMergedWorkSource() {
            WorkSource workSource = new WorkSource();
            Iterator<T> it = iterator();
            while (it.hasNext()) {
                workSource.add(((RequestInfo) it.next()).workSource);
            }
            return workSource;
        }
    }

    class WifiSingleScanStateMachine extends StateMachine implements WifiNative.ScanEventHandler {
        private RequestList<WifiScanner.ScanSettings> mActiveScans;
        private final DefaultState mDefaultState;
        private final DriverStartedState mDriverStartedState;
        private final IdleState mIdleState;
        private RequestList<WifiScanner.ScanSettings> mPendingScans;
        private final ScanningState mScanningState;
        final WifiScanningServiceImpl this$0;

        WifiSingleScanStateMachine(WifiScanningServiceImpl this$0, Looper looper) {
            super("WifiSingleScanStateMachine", looper);
            RequestList requestList = null;
            this.this$0 = this$0;
            this.mDefaultState = new DefaultState();
            this.mDriverStartedState = new DriverStartedState();
            this.mIdleState = new IdleState();
            this.mScanningState = new ScanningState();
            this.mActiveScans = new RequestList<>(this.this$0, requestList);
            this.mPendingScans = new RequestList<>(this.this$0, requestList);
            setLogRecSize(128);
            setLogOnlyTransitions(false);
            addState(this.mDefaultState);
            addState(this.mDriverStartedState, this.mDefaultState);
            addState(this.mIdleState, this.mDriverStartedState);
            addState(this.mScanningState, this.mDriverStartedState);
            setInitialState(this.mDefaultState);
        }

        @Override
        public void onScanStatus(int event) {
            this.this$0.localLog("onScanStatus event received, event=" + event);
            switch (event) {
                case 0:
                case 1:
                case 2:
                    sendMessage(WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS);
                    break;
                case 3:
                    sendMessage(WifiScanningServiceImpl.CMD_SCAN_FAILED);
                    break;
                default:
                    Log.e(WifiScanningServiceImpl.TAG, "Unknown scan status event: " + event);
                    break;
            }
        }

        @Override
        public void onFullScanResult(ScanResult fullScanResult, int bucketsScanned) {
            this.this$0.localLog("WifiSingleScanStateMachine onFullScanResult received bucketsScanned =" + bucketsScanned + "fullScanResult" + fullScanResult);
            sendMessage(WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS, 0, bucketsScanned, fullScanResult);
        }

        @Override
        public void onScanPaused(WifiScanner.ScanData[] scanData) {
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

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159765:
                    case 159766:
                        WifiSingleScanStateMachine.this.this$0.replyFailed(msg, -1, "not available");
                        break;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                        WifiSingleScanStateMachine.this.this$0.localLog("ignored scan results available event");
                        break;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        WifiSingleScanStateMachine.this.this$0.localLog("ignored full scan result event");
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mDefaultState);
                        break;
                }
                return true;
            }
        }

        class DriverStartedState extends State {
            DriverStartedState() {
            }

            public void exit() {
                WifiSingleScanStateMachine.this.this$0.mWifiMetrics.incrementScanReturnEntry(2, WifiSingleScanStateMachine.this.mPendingScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mPendingScans, -1, "Scan was interrupted");
            }

            public boolean processMessage(Message msg) {
                ClientInfo ci = (ClientInfo) WifiSingleScanStateMachine.this.this$0.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159765:
                        WifiSingleScanStateMachine.this.this$0.mWifiMetrics.incrementOneshotScanCount();
                        int handler = msg.arg2;
                        Bundle scanParams = (Bundle) msg.obj;
                        if (scanParams == null) {
                            WifiSingleScanStateMachine.this.this$0.logCallback("singleScanInvalidRequest", ci, handler, "null params");
                            WifiSingleScanStateMachine.this.this$0.replyFailed(msg, -3, "params null");
                        } else {
                            scanParams.setDefusable(true);
                            WifiScanner.ScanSettings scanSettings = scanParams.getParcelable("ScanSettings");
                            WorkSource workSource = (WorkSource) scanParams.getParcelable("WorkSource");
                            if (WifiSingleScanStateMachine.this.validateAndAddToScanQueue(ci, handler, scanSettings, workSource)) {
                                WifiSingleScanStateMachine.this.this$0.replySucceeded(msg);
                                if (WifiSingleScanStateMachine.this.getCurrentState() != WifiSingleScanStateMachine.this.mScanningState) {
                                    WifiSingleScanStateMachine.this.tryToStartNewScan();
                                }
                            } else {
                                WifiSingleScanStateMachine.this.this$0.logCallback("singleScanInvalidRequest", ci, handler, "bad request");
                                WifiSingleScanStateMachine.this.this$0.replyFailed(msg, -3, "bad request");
                                WifiSingleScanStateMachine.this.this$0.mWifiMetrics.incrementScanReturnEntry(3, 1);
                            }
                        }
                        break;
                    case 159766:
                        WifiSingleScanStateMachine.this.removeSingleScanRequest(ci, msg.arg2);
                        break;
                }
                return true;
            }
        }

        class IdleState extends State {
            IdleState() {
            }

            public void enter() {
                Log.e(WifiScanningServiceImpl.TAG, "WifiSingleScanStateMachine IdleState enter");
                WifiSingleScanStateMachine.this.tryToStartNewScan();
            }

            public boolean processMessage(Message msg) {
                return false;
            }
        }

        class ScanningState extends State {
            private WorkSource mScanWorkSource;

            ScanningState() {
            }

            public void enter() {
                this.mScanWorkSource = WifiSingleScanStateMachine.this.mActiveScans.createMergedWorkSource();
                try {
                    WifiSingleScanStateMachine.this.this$0.mBatteryStats.noteWifiScanStartedFromSource(this.mScanWorkSource);
                } catch (RemoteException e) {
                    WifiSingleScanStateMachine.this.loge(e.toString());
                }
            }

            public void exit() {
                try {
                    WifiSingleScanStateMachine.this.this$0.mBatteryStats.noteWifiScanStoppedFromSource(this.mScanWorkSource);
                } catch (RemoteException e) {
                    WifiSingleScanStateMachine.this.loge(e.toString());
                }
                WifiSingleScanStateMachine.this.this$0.mWifiMetrics.incrementScanReturnEntry(0, WifiSingleScanStateMachine.this.mActiveScans.size());
                WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mActiveScans, -1, "Scan was interrupted");
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                        WifiSingleScanStateMachine.this.this$0.mWifiMetrics.incrementScanReturnEntry(1, WifiSingleScanStateMachine.this.mActiveScans.size());
                        WifiSingleScanStateMachine.this.reportScanResults(WifiSingleScanStateMachine.this.this$0.mScannerImpl.getLatestSingleScanResults());
                        WifiSingleScanStateMachine.this.mActiveScans.clear();
                        WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                        return true;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        WifiSingleScanStateMachine.this.reportFullScanResult((ScanResult) msg.obj, msg.arg2);
                        return true;
                    case WifiScanningServiceImpl.CMD_SCAN_FAILED:
                        WifiSingleScanStateMachine.this.this$0.mWifiMetrics.incrementScanReturnEntry(0, WifiSingleScanStateMachine.this.mActiveScans.size());
                        WifiSingleScanStateMachine.this.sendOpFailedToAllAndClear(WifiSingleScanStateMachine.this.mActiveScans, -1, "Scan failed");
                        WifiSingleScanStateMachine.this.transitionTo(WifiSingleScanStateMachine.this.mIdleState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        boolean validateAndAddToScanQueue(ClientInfo ci, int handler, WifiScanner.ScanSettings settings, WorkSource workSource) {
            if (ci == null) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing single scan request ClientInfo not found " + handler);
                return false;
            }
            if (settings.band == 0 && (settings.channels == null || settings.channels.length == 0)) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing single scan because channel list was empty");
                return false;
            }
            this.this$0.logScanRequest("addSingleScanRequest", ci, handler, workSource, settings, null);
            this.mPendingScans.addRequest(ci, handler, workSource, settings);
            return true;
        }

        void removeSingleScanRequest(ClientInfo ci, int handler) {
            if (ci == null) {
                return;
            }
            this.this$0.logScanRequest("removeSingleScanRequest", ci, handler, null, null, null);
            this.mPendingScans.removeRequest(ci, handler);
            this.mActiveScans.removeRequest(ci, handler);
        }

        void removeSingleScanRequests(ClientInfo ci) {
            if (ci == null) {
                return;
            }
            this.this$0.logScanRequest("removeSingleScanRequests", ci, -1, null, null, null);
            this.mPendingScans.removeAllForClient(ci);
            this.mActiveScans.removeAllForClient(ci);
        }

        void tryToStartNewScan() {
            if (this.mPendingScans.size() == 0) {
                return;
            }
            this.this$0.mChannelHelper.updateChannels();
            WifiNative.ScanSettings settings = new WifiNative.ScanSettings();
            settings.num_buckets = 1;
            WifiNative.BucketSettings bucketSettings = new WifiNative.BucketSettings();
            bucketSettings.bucket = 0;
            bucketSettings.period_ms = 0;
            bucketSettings.report_events = 1;
            ChannelHelper.ChannelCollection channels = this.this$0.mChannelHelper.createChannelCollection();
            HashSet<Integer> hiddenNetworkIdSet = new HashSet<>();
            for (RequestInfo<WifiScanner.ScanSettings> entry : this.mPendingScans) {
                channels.addChannels(entry.settings);
                if (entry.settings.hiddenNetworkIds != null) {
                    for (int i = 0; i < entry.settings.hiddenNetworkIds.length; i++) {
                        hiddenNetworkIdSet.add(Integer.valueOf(entry.settings.hiddenNetworkIds[i]));
                    }
                }
                if ((entry.settings.reportEvents & 2) != 0) {
                    bucketSettings.report_events |= 2;
                }
            }
            if (hiddenNetworkIdSet.size() > 0) {
                settings.hiddenNetworkIds = new int[hiddenNetworkIdSet.size()];
                int numHiddenNetworks = 0;
                for (Integer hiddenNetworkId : hiddenNetworkIdSet) {
                    settings.hiddenNetworkIds[numHiddenNetworks] = hiddenNetworkId.intValue();
                    numHiddenNetworks++;
                }
            }
            channels.fillBucketSettings(bucketSettings, Integer.MAX_VALUE);
            settings.buckets = new WifiNative.BucketSettings[]{bucketSettings};
            this.this$0.localLog("tryToStartNewScan startSingleScan: base_period_ms" + settings.base_period_ms + " num_buckets= " + settings.num_buckets);
            if (this.this$0.mScannerImpl.startSingleScan(settings, this)) {
                RequestList<WifiScanner.ScanSettings> tmp = this.mActiveScans;
                this.mActiveScans = this.mPendingScans;
                this.mPendingScans = tmp;
                this.mPendingScans.clear();
                transitionTo(this.mScanningState);
                return;
            }
            this.this$0.mWifiMetrics.incrementScanReturnEntry(0, this.mPendingScans.size());
            sendOpFailedToAllAndClear(this.mPendingScans, -1, "Failed to start single scan");
        }

        void sendOpFailedToAllAndClear(RequestList<?> clientHandlers, int reason, String description) {
            Iterator<?> it = clientHandlers.iterator();
            while (it.hasNext()) {
                RequestInfo<?> entry = (RequestInfo) it.next();
                this.this$0.logCallback("singleScanFailed", entry.clientInfo, entry.handlerId, "reason=" + reason + ", " + description);
                entry.reportEvent(159762, 0, new WifiScanner.OperationResult(reason, description));
            }
            clientHandlers.clear();
        }

        void reportFullScanResult(ScanResult result, int bucketsScanned) {
            for (RequestInfo<WifiScanner.ScanSettings> entry : this.mActiveScans) {
                if (ScanScheduleUtil.shouldReportFullScanResultForSettings(this.this$0.mChannelHelper, result, bucketsScanned, entry.settings, -1)) {
                    entry.reportEvent(159764, 0, result);
                }
            }
        }

        void reportScanResults(WifiScanner.ScanData results) {
            if (results != null && results.getResults() != null) {
                if (results.getResults().length > 0) {
                    this.this$0.mWifiMetrics.incrementNonEmptyScanResultCount();
                } else {
                    this.this$0.mWifiMetrics.incrementEmptyScanResultCount();
                }
            }
            for (RequestInfo<WifiScanner.ScanSettings> entry : this.mActiveScans) {
                WifiScanner.ScanData[] resultsArray = {results};
                WifiScanner.ScanData[] resultsToDeliver = ScanScheduleUtil.filterResultsForSettings(this.this$0.mChannelHelper, resultsArray, entry.settings, -1);
                WifiScanner.ParcelableScanData parcelableScanData = new WifiScanner.ParcelableScanData(resultsToDeliver);
                this.this$0.logCallback("singleScanResults", entry.clientInfo, entry.handlerId, WifiScanningServiceImpl.describeForLog(resultsToDeliver));
                entry.reportEvent(159749, 0, parcelableScanData);
                entry.reportEvent(159767, 0, null);
            }
        }
    }

    class WifiBackgroundScanStateMachine extends StateMachine implements WifiNative.ScanEventHandler, WifiNative.HotlistEventHandler {
        private final RequestList<WifiScanner.ScanSettings> mActiveBackgroundScans;
        private final RequestList<WifiScanner.HotlistSettings> mActiveHotlistSettings;
        private final DefaultState mDefaultState;
        private final PausedState mPausedState;
        private final StartedState mStartedState;
        final WifiScanningServiceImpl this$0;

        WifiBackgroundScanStateMachine(WifiScanningServiceImpl this$0, Looper looper) {
            super("WifiBackgroundScanStateMachine", looper);
            RequestList requestList = null;
            this.this$0 = this$0;
            this.mDefaultState = new DefaultState();
            this.mStartedState = new StartedState();
            this.mPausedState = new PausedState();
            this.mActiveBackgroundScans = new RequestList<>(this.this$0, requestList);
            this.mActiveHotlistSettings = new RequestList<>(this.this$0, requestList);
            setLogRecSize(512);
            setLogOnlyTransitions(false);
            addState(this.mDefaultState);
            addState(this.mStartedState, this.mDefaultState);
            addState(this.mPausedState, this.mDefaultState);
            setInitialState(this.mDefaultState);
        }

        public Collection<WifiScanner.ScanSettings> getBackgroundScanSettings(ClientInfo ci) {
            return this.mActiveBackgroundScans.getAllSettingsForClient(ci);
        }

        public void removeBackgroundScanSettings(ClientInfo ci) {
            this.mActiveBackgroundScans.removeAllForClient(ci);
            updateSchedule();
        }

        public void removeHotlistSettings(ClientInfo ci) {
            this.mActiveHotlistSettings.removeAllForClient(ci);
            resetHotlist();
        }

        @Override
        public void onScanStatus(int event) {
            this.this$0.localLog("onScanStatus event received, event=" + event);
            switch (event) {
                case 0:
                case 1:
                case 2:
                    sendMessage(WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS);
                    break;
                case 3:
                    sendMessage(WifiScanningServiceImpl.CMD_SCAN_FAILED);
                    break;
                default:
                    Log.e(WifiScanningServiceImpl.TAG, "Unknown scan status event: " + event);
                    break;
            }
        }

        @Override
        public void onFullScanResult(ScanResult fullScanResult, int bucketsScanned) {
            this.this$0.localLog("WifiBackgroundScanStateMachine onFullScanResult received " + fullScanResult);
            sendMessage(WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS, 0, bucketsScanned, fullScanResult);
        }

        @Override
        public void onScanPaused(WifiScanner.ScanData[] scanData) {
            this.this$0.localLog("onScanPaused received");
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_PAUSED, scanData);
        }

        @Override
        public void onScanRestarted() {
            this.this$0.localLog("onScanRestarted received");
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_RESTARTED);
        }

        @Override
        public void onHotlistApFound(ScanResult[] results) {
            this.this$0.localLog("onHotlistApFound event received");
            sendMessage(WifiScanningServiceImpl.CMD_HOTLIST_AP_FOUND, 0, 0, results);
        }

        @Override
        public void onHotlistApLost(ScanResult[] results) {
            this.this$0.localLog("onHotlistApLost event received");
            sendMessage(WifiScanningServiceImpl.CMD_HOTLIST_AP_LOST, 0, 0, results);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                WifiBackgroundScanStateMachine.this.this$0.localLog("DefaultState");
                WifiBackgroundScanStateMachine.this.mActiveBackgroundScans.clear();
                WifiBackgroundScanStateMachine.this.mActiveHotlistSettings.clear();
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159746:
                    case 159747:
                    case 159748:
                    case 159750:
                    case 159751:
                    case 159765:
                    case 159766:
                        WifiBackgroundScanStateMachine.this.this$0.replyFailed(msg, -1, "not available");
                        return true;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                        WifiBackgroundScanStateMachine.this.this$0.localLog("ignored scan results available event");
                        return true;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        WifiBackgroundScanStateMachine.this.this$0.localLog("ignored full scan result event");
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        if (WifiBackgroundScanStateMachine.this.this$0.mScannerImpl == null) {
                            WifiBackgroundScanStateMachine.this.this$0.mScannerImpl = WifiBackgroundScanStateMachine.this.this$0.mScannerImplFactory.create(WifiBackgroundScanStateMachine.this.this$0.mContext, WifiBackgroundScanStateMachine.this.this$0.mLooper, WifiBackgroundScanStateMachine.this.this$0.mClock);
                            WifiBackgroundScanStateMachine.this.this$0.mChannelHelper = WifiBackgroundScanStateMachine.this.this$0.mScannerImpl.getChannelHelper();
                        }
                        WifiBackgroundScanStateMachine.this.this$0.mBackgroundScheduler = new BackgroundScanScheduler(WifiBackgroundScanStateMachine.this.this$0.mChannelHelper);
                        WifiNative.ScanCapabilities capabilities = new WifiNative.ScanCapabilities();
                        if (!WifiBackgroundScanStateMachine.this.this$0.mScannerImpl.getScanCapabilities(capabilities)) {
                            WifiBackgroundScanStateMachine.this.loge("could not get scan capabilities");
                            return true;
                        }
                        WifiBackgroundScanStateMachine.this.this$0.mBackgroundScheduler.setMaxBuckets(capabilities.max_scan_buckets);
                        WifiBackgroundScanStateMachine.this.this$0.mBackgroundScheduler.setMaxApPerScan(capabilities.max_ap_cache_per_scan);
                        Log.i(WifiScanningServiceImpl.TAG, "wifi driver loaded with scan capabilities: max buckets=" + capabilities.max_scan_buckets);
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mStartedState);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        Log.i(WifiScanningServiceImpl.TAG, "wifi driver unloaded");
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mDefaultState);
                        return true;
                    default:
                        return true;
                }
            }
        }

        class StartedState extends State {
            StartedState() {
            }

            public void enter() {
                WifiBackgroundScanStateMachine.this.this$0.localLog("StartedState");
            }

            public void exit() {
                WifiBackgroundScanStateMachine.this.sendBackgroundScanFailedToAllAndClear(-1, "Scan was interrupted");
                WifiBackgroundScanStateMachine.this.sendHotlistFailedToAllAndClear(-1, "Scan was interrupted");
                WifiBackgroundScanStateMachine.this.this$0.mScannerImpl.cleanup();
            }

            public boolean processMessage(Message msg) {
                ClientInfo ci = (ClientInfo) WifiBackgroundScanStateMachine.this.this$0.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159746:
                        WifiBackgroundScanStateMachine.this.this$0.mWifiMetrics.incrementBackgroundScanCount();
                        Bundle scanParams = (Bundle) msg.obj;
                        if (scanParams == null) {
                            WifiBackgroundScanStateMachine.this.this$0.replyFailed(msg, -3, "params null");
                            return true;
                        }
                        scanParams.setDefusable(true);
                        WifiScanner.ScanSettings scanSettings = scanParams.getParcelable("ScanSettings");
                        WorkSource workSource = (WorkSource) scanParams.getParcelable("WorkSource");
                        if (WifiBackgroundScanStateMachine.this.addBackgroundScanRequest(ci, msg.arg2, scanSettings, workSource)) {
                            WifiBackgroundScanStateMachine.this.this$0.replySucceeded(msg);
                        } else {
                            WifiBackgroundScanStateMachine.this.this$0.replyFailed(msg, -3, "bad request");
                        }
                        return true;
                    case 159747:
                        WifiBackgroundScanStateMachine.this.removeBackgroundScanRequest(ci, msg.arg2);
                        return true;
                    case 159748:
                        WifiBackgroundScanStateMachine.this.reportScanResults(WifiBackgroundScanStateMachine.this.this$0.mScannerImpl.getLatestBatchedScanResults(true));
                        WifiBackgroundScanStateMachine.this.this$0.replySucceeded(msg);
                        return true;
                    case 159750:
                        WifiBackgroundScanStateMachine.this.addHotlist(ci, msg.arg2, (WifiScanner.HotlistSettings) msg.obj);
                        WifiBackgroundScanStateMachine.this.this$0.replySucceeded(msg);
                        return true;
                    case 159751:
                        WifiBackgroundScanStateMachine.this.removeHotlist(ci, msg.arg2);
                        return true;
                    case WifiConnectivityManager.MAX_PERIODIC_SCAN_INTERVAL_MS:
                        WifiBackgroundScanStateMachine.this.reportScanResults(WifiBackgroundScanStateMachine.this.this$0.mScannerImpl.getLatestBatchedScanResults(true));
                        return true;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        WifiBackgroundScanStateMachine.this.reportFullScanResult((ScanResult) msg.obj, msg.arg2);
                        return true;
                    case WifiScanningServiceImpl.CMD_HOTLIST_AP_FOUND:
                        WifiBackgroundScanStateMachine.this.reportHotlistResults(159753, (ScanResult[]) msg.obj);
                        return true;
                    case WifiScanningServiceImpl.CMD_HOTLIST_AP_LOST:
                        WifiBackgroundScanStateMachine.this.reportHotlistResults(159754, (ScanResult[]) msg.obj);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        return false;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        return false;
                    case WifiScanningServiceImpl.CMD_SCAN_PAUSED:
                        WifiBackgroundScanStateMachine.this.reportScanResults((WifiScanner.ScanData[]) msg.obj);
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mPausedState);
                        return true;
                    case WifiScanningServiceImpl.CMD_SCAN_FAILED:
                        Log.e(WifiScanningServiceImpl.TAG, "WifiScanner background scan gave CMD_SCAN_FAILED");
                        WifiBackgroundScanStateMachine.this.sendBackgroundScanFailedToAllAndClear(-1, "Background Scan failed");
                        return true;
                    default:
                        return false;
                }
            }
        }

        class PausedState extends State {
            PausedState() {
            }

            public void enter() {
                WifiBackgroundScanStateMachine.this.this$0.localLog("PausedState");
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case WifiScanningServiceImpl.CMD_SCAN_RESTARTED:
                        WifiBackgroundScanStateMachine.this.transitionTo(WifiBackgroundScanStateMachine.this.mStartedState);
                        break;
                    default:
                        WifiBackgroundScanStateMachine.this.deferMessage(msg);
                        break;
                }
                return true;
            }
        }

        private boolean addBackgroundScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings settings, WorkSource workSource) {
            if (ci == null) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing scan request ClientInfo not found " + handler);
                return false;
            }
            if (settings.periodInMs < 1000) {
                loge("Failing scan request because periodInMs is " + settings.periodInMs + ", min scan period is: 1000");
                return false;
            }
            if (settings.band == 0 && settings.channels == null) {
                loge("Channels was null with unspecified band");
                return false;
            }
            if (settings.band == 0 && settings.channels.length == 0) {
                loge("No channels specified");
                return false;
            }
            int minSupportedPeriodMs = this.this$0.mChannelHelper.estimateScanDuration(settings);
            if (settings.periodInMs < minSupportedPeriodMs) {
                loge("Failing scan request because minSupportedPeriodMs is " + minSupportedPeriodMs + " but the request wants " + settings.periodInMs);
                return false;
            }
            if (settings.maxPeriodInMs != 0 && settings.maxPeriodInMs != settings.periodInMs) {
                if (settings.maxPeriodInMs < settings.periodInMs) {
                    loge("Failing scan request because maxPeriodInMs is " + settings.maxPeriodInMs + " but less than periodInMs " + settings.periodInMs);
                    return false;
                }
                if (settings.maxPeriodInMs > 1024000) {
                    loge("Failing scan request because maxSupportedPeriodMs is 1024000 but the request wants " + settings.maxPeriodInMs);
                    return false;
                }
                if (settings.stepCount < 1) {
                    loge("Failing scan request because stepCount is " + settings.stepCount + " which is less than 1");
                    return false;
                }
            }
            this.this$0.logScanRequest("addBackgroundScanRequest", ci, handler, null, settings, null);
            this.mActiveBackgroundScans.addRequest(ci, handler, workSource, settings);
            if (updateSchedule()) {
                return true;
            }
            this.mActiveBackgroundScans.removeRequest(ci, handler);
            this.this$0.localLog("Failing scan request because failed to reset scan");
            return false;
        }

        private boolean updateSchedule() {
            if (this.this$0.mChannelHelper == null || this.this$0.mBackgroundScheduler == null || this.this$0.mScannerImpl == null) {
                loge("Failed to update schedule because WifiScanningService is not initialized");
                return false;
            }
            this.this$0.mChannelHelper.updateChannels();
            Collection<WifiScanner.ScanSettings> settings = this.mActiveBackgroundScans.getAllSettings();
            this.this$0.mBackgroundScheduler.updateSchedule(settings);
            WifiNative.ScanSettings schedule = this.this$0.mBackgroundScheduler.getSchedule();
            if (ScanScheduleUtil.scheduleEquals(this.this$0.mPreviousSchedule, schedule)) {
                Log.d(WifiScanningServiceImpl.TAG, "schedule updated with no change");
                return true;
            }
            this.this$0.mPreviousSchedule = schedule;
            if (schedule.num_buckets == 0) {
                this.this$0.mScannerImpl.stopBatchedScan();
                Log.d(WifiScanningServiceImpl.TAG, "scan stopped");
                return true;
            }
            Log.d(WifiScanningServiceImpl.TAG, "starting scan: base period=" + schedule.base_period_ms + ", max ap per scan=" + schedule.max_ap_per_scan + ", batched scans=" + schedule.report_threshold_num_scans);
            for (int b = 0; b < schedule.num_buckets; b++) {
                WifiNative.BucketSettings bucket = schedule.buckets[b];
                Log.d(WifiScanningServiceImpl.TAG, "bucket " + bucket.bucket + " (" + bucket.period_ms + "ms)[" + bucket.report_events + "]: " + ChannelHelper.toString(bucket) + "(MAX" + bucket.max_period_ms + ") ");
            }
            if (this.this$0.mScannerImpl.startBatchedScan(schedule, this)) {
                Log.d(WifiScanningServiceImpl.TAG, "scan restarted with " + schedule.num_buckets + " bucket(s) and base period: " + schedule.base_period_ms);
                return true;
            }
            this.this$0.mPreviousSchedule = null;
            loge("error starting scan: base period=" + schedule.base_period_ms + ", max ap per scan=" + schedule.max_ap_per_scan + ", batched scans=" + schedule.report_threshold_num_scans);
            for (int b2 = 0; b2 < schedule.num_buckets; b2++) {
                WifiNative.BucketSettings bucket2 = schedule.buckets[b2];
                loge("bucket " + bucket2.bucket + " (" + bucket2.period_ms + "ms)[" + bucket2.report_events + "]: " + ChannelHelper.toString(bucket2));
            }
            return false;
        }

        private void removeBackgroundScanRequest(ClientInfo ci, int handler) {
            if (ci == null) {
                return;
            }
            WifiScanner.ScanSettings settings = this.mActiveBackgroundScans.removeRequest(ci, handler);
            this.this$0.logScanRequest("removeBackgroundScanRequest", ci, handler, null, settings, null);
            updateSchedule();
        }

        private void reportFullScanResult(ScanResult result, int bucketsScanned) {
            for (RequestInfo<WifiScanner.ScanSettings> entry : this.mActiveBackgroundScans) {
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                WifiScanner.ScanSettings settings = entry.settings;
                if (this.this$0.mBackgroundScheduler.shouldReportFullScanResultForSettings(result, bucketsScanned, settings)) {
                    ScanResult newResult = new ScanResult(result);
                    if (result.informationElements != null) {
                        newResult.informationElements = (ScanResult.InformationElement[]) result.informationElements.clone();
                    } else {
                        newResult.informationElements = null;
                    }
                    ci.reportEvent(159764, 0, handler, newResult);
                }
            }
        }

        private void reportScanResults(WifiScanner.ScanData[] results) {
            for (WifiScanner.ScanData result : results) {
                if (result != null && result.getResults() != null) {
                    if (result.getResults().length > 0) {
                        this.this$0.mWifiMetrics.incrementNonEmptyScanResultCount();
                    } else {
                        this.this$0.mWifiMetrics.incrementEmptyScanResultCount();
                    }
                }
            }
            for (RequestInfo<WifiScanner.ScanSettings> entry : this.mActiveBackgroundScans) {
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                WifiScanner.ScanSettings settings = entry.settings;
                WifiScanner.ScanData[] resultsToDeliver = this.this$0.mBackgroundScheduler.filterResultsForSettings(results, settings);
                if (resultsToDeliver != null) {
                    this.this$0.logCallback("backgroundScanResults", ci, handler, WifiScanningServiceImpl.describeForLog(resultsToDeliver));
                    WifiScanner.ParcelableScanData parcelableScanData = new WifiScanner.ParcelableScanData(resultsToDeliver);
                    ci.reportEvent(159749, 0, handler, parcelableScanData);
                }
            }
        }

        private void sendBackgroundScanFailedToAllAndClear(int reason, String description) {
            for (RequestInfo<WifiScanner.ScanSettings> entry : this.mActiveBackgroundScans) {
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                ci.reportEvent(159762, 0, handler, new WifiScanner.OperationResult(reason, description));
            }
            this.mActiveBackgroundScans.clear();
            this.this$0.mPreviousSchedule = null;
        }

        private void addHotlist(ClientInfo ci, int handler, WifiScanner.HotlistSettings settings) {
            this.mActiveHotlistSettings.addRequest(ci, handler, null, settings);
            resetHotlist();
        }

        private void removeHotlist(ClientInfo ci, int handler) {
            this.mActiveHotlistSettings.removeRequest(ci, handler);
            resetHotlist();
        }

        private void resetHotlist() {
            if (this.this$0.mScannerImpl == null) {
                loge("Failed to update hotlist because WifiScanningService is not initialized");
                return;
            }
            Collection<WifiScanner.HotlistSettings> settings = this.mActiveHotlistSettings.getAllSettings();
            int num_hotlist_ap = 0;
            Iterator s$iterator = settings.iterator();
            while (s$iterator.hasNext()) {
                num_hotlist_ap += ((WifiScanner.HotlistSettings) s$iterator.next()).bssidInfos.length;
            }
            if (num_hotlist_ap == 0) {
                this.this$0.mScannerImpl.resetHotlist();
                return;
            }
            WifiScanner.BssidInfo[] bssidInfos = new WifiScanner.BssidInfo[num_hotlist_ap];
            int apLostThreshold = Integer.MAX_VALUE;
            int index = 0;
            for (WifiScanner.HotlistSettings s : settings) {
                int i = 0;
                while (i < s.bssidInfos.length) {
                    bssidInfos[index] = s.bssidInfos[i];
                    i++;
                    index++;
                }
                if (s.apLostThreshold < apLostThreshold) {
                    apLostThreshold = s.apLostThreshold;
                }
            }
            WifiScanner.HotlistSettings mergedSettings = new WifiScanner.HotlistSettings();
            mergedSettings.bssidInfos = bssidInfos;
            mergedSettings.apLostThreshold = apLostThreshold;
            this.this$0.mScannerImpl.setHotlist(mergedSettings, this);
        }

        private void reportHotlistResults(int what, ScanResult[] results) {
            this.this$0.localLog("reportHotlistResults " + what + " results " + results.length);
            for (RequestInfo<WifiScanner.HotlistSettings> entry : this.mActiveHotlistSettings) {
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                WifiScanner.HotlistSettings settings = entry.settings;
                int num_results = 0;
                for (ScanResult result : results) {
                    WifiScanner.BssidInfo[] bssidInfoArr = settings.bssidInfos;
                    int i = 0;
                    int length = bssidInfoArr.length;
                    while (true) {
                        if (i < length) {
                            WifiScanner.BssidInfo BssidInfo = bssidInfoArr[i];
                            if (!result.BSSID.equalsIgnoreCase(BssidInfo.bssid)) {
                                i++;
                            } else {
                                num_results++;
                                break;
                            }
                        }
                    }
                }
                if (num_results == 0) {
                    return;
                }
                ScanResult[] results2 = new ScanResult[num_results];
                int index = 0;
                for (ScanResult result2 : results) {
                    for (WifiScanner.BssidInfo BssidInfo2 : settings.bssidInfos) {
                        if (result2.BSSID.equalsIgnoreCase(BssidInfo2.bssid)) {
                            results2[index] = result2;
                            index++;
                        }
                    }
                }
                WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results2);
                ci.reportEvent(what, 0, handler, parcelableScanResults);
            }
        }

        private void sendHotlistFailedToAllAndClear(int reason, String description) {
            for (RequestInfo<WifiScanner.HotlistSettings> entry : this.mActiveHotlistSettings) {
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                ci.reportEvent(159762, 0, handler, new WifiScanner.OperationResult(reason, description));
            }
            this.mActiveHotlistSettings.clear();
        }
    }

    class WifiPnoScanStateMachine extends StateMachine implements WifiNative.PnoEventHandler {
        private final RequestList<Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings>> mActivePnoScans;
        private final DefaultState mDefaultState;
        private final HwPnoScanState mHwPnoScanState;
        private InternalClientInfo mInternalClientInfo;
        private final SingleScanState mSingleScanState;
        private final StartedState mStartedState;
        private final SwPnoScanState mSwPnoScanState;

        WifiPnoScanStateMachine(Looper looper) {
            super("WifiPnoScanStateMachine", looper);
            this.mDefaultState = new DefaultState();
            this.mStartedState = new StartedState();
            this.mHwPnoScanState = new HwPnoScanState();
            this.mSwPnoScanState = new SwPnoScanState();
            this.mSingleScanState = new SingleScanState();
            this.mActivePnoScans = new RequestList<>(WifiScanningServiceImpl.this, null);
            setLogRecSize(512);
            setLogOnlyTransitions(false);
            addState(this.mDefaultState);
            addState(this.mStartedState, this.mDefaultState);
            addState(this.mHwPnoScanState, this.mStartedState);
            addState(this.mSingleScanState, this.mHwPnoScanState);
            addState(this.mSwPnoScanState, this.mStartedState);
            setInitialState(this.mDefaultState);
        }

        public void removePnoSettings(ClientInfo ci) {
            this.mActivePnoScans.removeAllForClient(ci);
            transitionTo(this.mStartedState);
        }

        @Override
        public void onPnoNetworkFound(ScanResult[] results) {
            WifiScanningServiceImpl.this.localLog("onWifiPnoNetworkFound event received");
            sendMessage(WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND, 0, 0, results);
        }

        @Override
        public void onPnoScanFailed() {
            WifiScanningServiceImpl.this.localLog("onWifiPnoScanFailed event received");
            sendMessage(WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED, 0, 0, null);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("DefaultState");
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159749:
                    case 159762:
                    case WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND:
                    case WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED:
                        WifiPnoScanStateMachine.this.loge("Unexpected message " + msg.what);
                        return true;
                    case 159768:
                    case 159769:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "not available");
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mDefaultState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class StartedState extends State {
            StartedState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("StartedState");
            }

            public void exit() {
                WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "Scan was interrupted");
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159768:
                        Bundle pnoParams = (Bundle) msg.obj;
                        if (pnoParams == null) {
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "params null");
                            return true;
                        }
                        pnoParams.setDefusable(true);
                        WifiScanner.PnoSettings pnoSettings = pnoParams.getParcelable("PnoSettings");
                        WifiPnoScanStateMachine.this.deferMessage(msg);
                        if (WifiScanningServiceImpl.this.mScannerImpl.isHwPnoSupported(pnoSettings.isConnected)) {
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mHwPnoScanState);
                        } else {
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mSwPnoScanState);
                        }
                        return true;
                    case 159769:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "no scan running");
                        return true;
                    default:
                        return false;
                }
            }
        }

        class HwPnoScanState extends State {
            HwPnoScanState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("HwPnoScanState");
            }

            public void exit() {
                WifiScanningServiceImpl.this.mScannerImpl.resetHwPnoList();
                WifiPnoScanStateMachine.this.removeInternalClient();
            }

            public boolean processMessage(Message msg) {
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159768:
                        Bundle pnoParams = (Bundle) msg.obj;
                        if (pnoParams == null) {
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "params null");
                            return true;
                        }
                        pnoParams.setDefusable(true);
                        WifiScanner.PnoSettings pnoSettings = pnoParams.getParcelable("PnoSettings");
                        WifiScanner.ScanSettings scanSettings = pnoParams.getParcelable("ScanSettings");
                        if (WifiPnoScanStateMachine.this.addHwPnoScanRequest(ci, msg.arg2, scanSettings, pnoSettings)) {
                            WifiScanningServiceImpl.this.replySucceeded(msg);
                        } else {
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "bad request");
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        }
                        return true;
                    case 159769:
                        WifiPnoScanStateMachine.this.removeHwPnoScanRequest(ci, msg.arg2);
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    case WifiScanningServiceImpl.CMD_PNO_NETWORK_FOUND:
                        ScanResult[] scanResults = (ScanResult[]) msg.obj;
                        if (WifiPnoScanStateMachine.this.isSingleScanNeeded(scanResults)) {
                            WifiScanner.ScanSettings activeScanSettings = WifiPnoScanStateMachine.this.getScanSettings();
                            if (activeScanSettings == null) {
                                WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "couldn't retrieve setting");
                                WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                            } else {
                                WifiPnoScanStateMachine.this.addSingleScanRequest(activeScanSettings);
                                WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mSingleScanState);
                            }
                        } else {
                            WifiPnoScanStateMachine.this.reportPnoNetworkFound((ScanResult[]) msg.obj);
                        }
                        return true;
                    case WifiScanningServiceImpl.CMD_PNO_SCAN_FAILED:
                        WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "pno scan failed");
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class SingleScanState extends State {
            SingleScanState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("SingleScanState");
            }

            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case 159749:
                        WifiScanner.ParcelableScanData parcelableScanData = (WifiScanner.ParcelableScanData) msg.obj;
                        WifiScanner.ScanData[] scanDatas = parcelableScanData.getResults();
                        WifiScanner.ScanData lastScanData = scanDatas[scanDatas.length - 1];
                        WifiPnoScanStateMachine.this.reportPnoNetworkFound(lastScanData.getResults());
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mHwPnoScanState);
                        return true;
                    case 159762:
                        WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "single scan failed");
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class SwPnoScanState extends State {
            private final ArrayList<ScanResult> mSwPnoFullScanResults = new ArrayList<>();

            SwPnoScanState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("SwPnoScanState");
                this.mSwPnoFullScanResults.clear();
            }

            public void exit() {
                WifiPnoScanStateMachine.this.removeInternalClient();
            }

            public boolean processMessage(Message msg) {
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159749:
                        ScanResult[] scanResults = (ScanResult[]) this.mSwPnoFullScanResults.toArray(new ScanResult[this.mSwPnoFullScanResults.size()]);
                        WifiPnoScanStateMachine.this.reportPnoNetworkFound(scanResults);
                        this.mSwPnoFullScanResults.clear();
                        return true;
                    case 159762:
                        WifiPnoScanStateMachine.this.sendPnoScanFailedToAllAndClear(-1, "background scan failed");
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    case 159764:
                        this.mSwPnoFullScanResults.add((ScanResult) msg.obj);
                        return true;
                    case 159768:
                        Bundle pnoParams = (Bundle) msg.obj;
                        if (pnoParams == null) {
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "params null");
                            return true;
                        }
                        pnoParams.setDefusable(true);
                        WifiScanner.PnoSettings pnoSettings = pnoParams.getParcelable("PnoSettings");
                        WifiScanner.ScanSettings scanSettings = pnoParams.getParcelable("ScanSettings");
                        if (WifiPnoScanStateMachine.this.addSwPnoScanRequest(ci, msg.arg2, scanSettings, pnoSettings)) {
                            WifiScanningServiceImpl.this.replySucceeded(msg);
                        } else {
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "bad request");
                            WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        }
                        return true;
                    case 159769:
                        WifiPnoScanStateMachine.this.removeSwPnoScanRequest(ci, msg.arg2);
                        WifiPnoScanStateMachine.this.transitionTo(WifiPnoScanStateMachine.this.mStartedState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        private WifiNative.PnoSettings convertPnoSettingsToNative(WifiScanner.PnoSettings pnoSettings) {
            WifiNative.PnoSettings nativePnoSetting = new WifiNative.PnoSettings();
            nativePnoSetting.min5GHzRssi = pnoSettings.min5GHzRssi;
            nativePnoSetting.min24GHzRssi = pnoSettings.min24GHzRssi;
            nativePnoSetting.initialScoreMax = pnoSettings.initialScoreMax;
            nativePnoSetting.currentConnectionBonus = pnoSettings.currentConnectionBonus;
            nativePnoSetting.sameNetworkBonus = pnoSettings.sameNetworkBonus;
            nativePnoSetting.secureBonus = pnoSettings.secureBonus;
            nativePnoSetting.band5GHzBonus = pnoSettings.band5GHzBonus;
            nativePnoSetting.isConnected = pnoSettings.isConnected;
            nativePnoSetting.networkList = new WifiNative.PnoNetwork[pnoSettings.networkList.length];
            for (int i = 0; i < pnoSettings.networkList.length; i++) {
                nativePnoSetting.networkList[i] = new WifiNative.PnoNetwork();
                nativePnoSetting.networkList[i].ssid = pnoSettings.networkList[i].ssid;
                nativePnoSetting.networkList[i].networkId = pnoSettings.networkList[i].networkId;
                nativePnoSetting.networkList[i].priority = pnoSettings.networkList[i].priority;
                nativePnoSetting.networkList[i].flags = pnoSettings.networkList[i].flags;
                nativePnoSetting.networkList[i].auth_bit_field = pnoSettings.networkList[i].authBitField;
            }
            return nativePnoSetting;
        }

        private WifiScanner.ScanSettings getScanSettings() {
            Iterator settingsPair$iterator = this.mActivePnoScans.getAllSettings().iterator();
            if (settingsPair$iterator.hasNext()) {
                Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings> settingsPair = (Pair) settingsPair$iterator.next();
                return (WifiScanner.ScanSettings) settingsPair.second;
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

        private void addInternalClient(ClientInfo ci) {
            if (this.mInternalClientInfo == null) {
                this.mInternalClientInfo = WifiScanningServiceImpl.this.new InternalClientInfo(ci.getUid(), new Messenger(getHandler()));
                this.mInternalClientInfo.register();
            } else {
                Log.w(WifiScanningServiceImpl.TAG, "Internal client for PNO already exists");
            }
        }

        private void addPnoScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings scanSettings, WifiScanner.PnoSettings pnoSettings) {
            this.mActivePnoScans.addRequest(ci, handler, WifiStateMachine.WIFI_WORK_SOURCE, Pair.create(pnoSettings, scanSettings));
            addInternalClient(ci);
        }

        private Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings> removePnoScanRequest(ClientInfo ci, int handler) {
            Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings> settings = this.mActivePnoScans.removeRequest(ci, handler);
            return settings;
        }

        private boolean addHwPnoScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings scanSettings, WifiScanner.PnoSettings pnoSettings) {
            if (ci == null) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing scan request ClientInfo not found " + handler);
                return false;
            }
            if (!this.mActivePnoScans.isEmpty()) {
                loge("Failing scan request because there is already an active scan");
                return false;
            }
            WifiNative.PnoSettings nativePnoSettings = convertPnoSettingsToNative(pnoSettings);
            if (!WifiScanningServiceImpl.this.mScannerImpl.setHwPnoList(nativePnoSettings, WifiScanningServiceImpl.this.mPnoScanStateMachine)) {
                return false;
            }
            WifiScanningServiceImpl.this.logScanRequest("addHwPnoScanRequest", ci, handler, null, scanSettings, pnoSettings);
            addPnoScanRequest(ci, handler, scanSettings, pnoSettings);
            if (WifiScanningServiceImpl.this.mScannerImpl.shouldScheduleBackgroundScanForHwPno()) {
                addBackgroundScanRequest(scanSettings);
                return true;
            }
            return true;
        }

        private void removeHwPnoScanRequest(ClientInfo ci, int handler) {
            if (ci == null) {
                return;
            }
            Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings> settings = removePnoScanRequest(ci, handler);
            WifiScanningServiceImpl.this.logScanRequest("removeHwPnoScanRequest", ci, handler, null, (WifiScanner.ScanSettings) settings.second, (WifiScanner.PnoSettings) settings.first);
        }

        private boolean addSwPnoScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings scanSettings, WifiScanner.PnoSettings pnoSettings) {
            if (ci == null) {
                Log.d(WifiScanningServiceImpl.TAG, "Failing scan request ClientInfo not found " + handler);
                return false;
            }
            if (!this.mActivePnoScans.isEmpty()) {
                loge("Failing scan request because there is already an active scan");
                return false;
            }
            WifiScanningServiceImpl.this.logScanRequest("addSwPnoScanRequest", ci, handler, null, scanSettings, pnoSettings);
            addPnoScanRequest(ci, handler, scanSettings, pnoSettings);
            scanSettings.reportEvents = 3;
            addBackgroundScanRequest(scanSettings);
            return true;
        }

        private void removeSwPnoScanRequest(ClientInfo ci, int handler) {
            if (ci == null) {
                return;
            }
            Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings> settings = removePnoScanRequest(ci, handler);
            WifiScanningServiceImpl.this.logScanRequest("removeSwPnoScanRequest", ci, handler, null, (WifiScanner.ScanSettings) settings.second, (WifiScanner.PnoSettings) settings.first);
        }

        private void reportPnoNetworkFound(ScanResult[] results) {
            WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results);
            Iterator<Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings>> it = this.mActivePnoScans.iterator();
            while (it.hasNext()) {
                RequestInfo<Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings>> entry = (RequestInfo) it.next();
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                WifiScanningServiceImpl.this.logCallback("pnoNetworkFound", ci, handler, WifiScanningServiceImpl.describeForLog(results));
                ci.reportEvent(159770, 0, handler, parcelableScanResults);
            }
        }

        private void sendPnoScanFailedToAllAndClear(int reason, String description) {
            Iterator<Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings>> it = this.mActivePnoScans.iterator();
            while (it.hasNext()) {
                RequestInfo<Pair<WifiScanner.PnoSettings, WifiScanner.ScanSettings>> entry = (RequestInfo) it.next();
                ClientInfo ci = entry.clientInfo;
                int handler = entry.handlerId;
                ci.reportEvent(159762, 0, handler, new WifiScanner.OperationResult(reason, description));
            }
            this.mActivePnoScans.clear();
        }

        private void addBackgroundScanRequest(WifiScanner.ScanSettings settings) {
            WifiScanningServiceImpl.this.localLog("Starting background scan");
            if (this.mInternalClientInfo == null) {
                return;
            }
            this.mInternalClientInfo.sendRequestToClientHandler(159746, settings, WifiStateMachine.WIFI_WORK_SOURCE);
        }

        private void addSingleScanRequest(WifiScanner.ScanSettings settings) {
            WifiScanningServiceImpl.this.localLog("Starting single scan");
            if (this.mInternalClientInfo == null) {
                return;
            }
            this.mInternalClientInfo.sendRequestToClientHandler(159765, settings, WifiStateMachine.WIFI_WORK_SOURCE);
        }

        private boolean isSingleScanNeeded(ScanResult[] scanResults) {
            for (ScanResult scanResult : scanResults) {
                if (scanResult.informationElements != null && scanResult.informationElements.length > 0) {
                    return false;
                }
            }
            return true;
        }
    }

    private abstract class ClientInfo {
        protected final Messenger mMessenger;
        private boolean mScanWorkReported = false;
        private final int mUid;
        private final WorkSource mWorkSource;

        public abstract void reportEvent(int i, int i2, int i3, Object obj);

        ClientInfo(int uid, Messenger messenger) {
            this.mUid = uid;
            this.mMessenger = messenger;
            this.mWorkSource = new WorkSource(uid);
        }

        public void register() {
            WifiScanningServiceImpl.this.mClients.put(this.mMessenger, this);
        }

        private void unregister() {
            WifiScanningServiceImpl.this.mClients.remove(this.mMessenger);
        }

        public void cleanup() {
            WifiScanningServiceImpl.this.mSingleScanStateMachine.removeSingleScanRequests(this);
            WifiScanningServiceImpl.this.mBackgroundScanStateMachine.removeBackgroundScanSettings(this);
            WifiScanningServiceImpl.this.mBackgroundScanStateMachine.removeHotlistSettings(this);
            unregister();
            WifiScanningServiceImpl.this.localLog("Successfully stopped all requests for client " + this);
        }

        public int getUid() {
            return this.mUid;
        }

        public void reportEvent(int what, int arg1, int arg2) {
            reportEvent(what, arg1, arg2, null);
        }

        private void reportBatchedScanStart() {
            if (this.mUid == 0) {
                return;
            }
            int csph = getCsph();
            try {
                WifiScanningServiceImpl.this.mBatteryStats.noteWifiBatchedScanStartedFromSource(this.mWorkSource, csph);
            } catch (RemoteException e) {
                WifiScanningServiceImpl.this.logw("failed to report scan work: " + e.toString());
            }
        }

        private void reportBatchedScanStop() {
            if (this.mUid == 0) {
                return;
            }
            try {
                WifiScanningServiceImpl.this.mBatteryStats.noteWifiBatchedScanStoppedFromSource(this.mWorkSource);
            } catch (RemoteException e) {
                WifiScanningServiceImpl.this.logw("failed to cleanup scan work: " + e.toString());
            }
        }

        private int getCsph() {
            int totalScanDurationPerHour = 0;
            Collection<WifiScanner.ScanSettings> settingsList = WifiScanningServiceImpl.this.mBackgroundScanStateMachine.getBackgroundScanSettings(this);
            for (WifiScanner.ScanSettings settings : settingsList) {
                int scanDurationMs = WifiScanningServiceImpl.this.mChannelHelper.estimateScanDuration(settings);
                int scans_per_Hour = settings.periodInMs == 0 ? 1 : 3600000 / settings.periodInMs;
                totalScanDurationPerHour += scanDurationMs * scans_per_Hour;
            }
            return totalScanDurationPerHour / 200;
        }

        public void reportScanWorkUpdate() {
            if (this.mScanWorkReported) {
                reportBatchedScanStop();
                this.mScanWorkReported = false;
            }
            if (!WifiScanningServiceImpl.this.mBackgroundScanStateMachine.getBackgroundScanSettings(this).isEmpty()) {
                return;
            }
            reportBatchedScanStart();
            this.mScanWorkReported = true;
        }

        public String toString() {
            return "ClientInfo[uid=" + this.mUid + "]";
        }
    }

    private class ExternalClientInfo extends ClientInfo {
        private final AsyncChannel mChannel;
        private boolean mDisconnected;

        ExternalClientInfo(int uid, Messenger messenger, AsyncChannel c) {
            super(uid, messenger);
            this.mDisconnected = false;
            this.mChannel = c;
            WifiScanningServiceImpl.this.localLog("New client, channel: " + c);
        }

        @Override
        public void reportEvent(int what, int arg1, int arg2, Object obj) {
            if (this.mDisconnected) {
                return;
            }
            this.mChannel.sendMessage(what, arg1, arg2, obj);
        }

        @Override
        public void cleanup() {
            this.mDisconnected = true;
            WifiScanningServiceImpl.this.mWifiChangeStateMachine.removeWifiChangeHandler(this);
            WifiScanningServiceImpl.this.mPnoScanStateMachine.removePnoSettings(this);
            super.cleanup();
        }
    }

    private class InternalClientInfo extends ClientInfo {
        private static final int INTERNAL_CLIENT_HANDLER = 0;

        InternalClientInfo(int requesterUid, Messenger messenger) {
            super(requesterUid, messenger);
        }

        @Override
        public void reportEvent(int what, int arg1, int arg2, Object obj) {
            Message message = Message.obtain();
            message.what = what;
            message.arg1 = arg1;
            message.arg2 = arg2;
            message.obj = obj;
            try {
                this.mMessenger.send(message);
            } catch (RemoteException e) {
                WifiScanningServiceImpl.this.loge("Failed to send message: " + what);
            }
        }

        public void sendRequestToClientHandler(int what, WifiScanner.ScanSettings settings, WorkSource workSource) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.arg2 = 0;
            if (settings != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("ScanSettings", settings);
                bundle.putParcelable("WorkSource", workSource);
                msg.obj = bundle;
            }
            msg.replyTo = this.mMessenger;
            msg.sendingUid = getUid();
            WifiScanningServiceImpl.this.mClientHandler.sendMessage(msg);
        }

        public void sendRequestToClientHandler(int what) {
            sendRequestToClientHandler(what, null, null);
        }
    }

    void replySucceeded(Message msg) {
        if (msg.replyTo == null) {
            return;
        }
        Message reply = Message.obtain();
        reply.what = 159761;
        reply.arg2 = msg.arg2;
        try {
            msg.replyTo.send(reply);
        } catch (RemoteException e) {
        }
    }

    void replyFailed(Message msg, int reason, String description) {
        if (msg.replyTo == null) {
            return;
        }
        Message reply = Message.obtain();
        reply.what = 159762;
        reply.arg2 = msg.arg2;
        reply.obj = new WifiScanner.OperationResult(reason, description);
        try {
            msg.replyTo.send(reply);
        } catch (RemoteException e) {
        }
    }

    class WifiChangeStateMachine extends StateMachine implements WifiNative.SignificantWifiChangeEventHandler {
        private static final String ACTION_TIMEOUT = "com.android.server.WifiScanningServiceImpl.action.TIMEOUT";
        private static final int MAX_APS_TO_TRACK = 3;
        private static final int MOVING_SCAN_PERIOD_MS = 10000;
        private static final int MOVING_STATE_TIMEOUT_MS = 30000;
        private static final int STATIONARY_SCAN_PERIOD_MS = 5000;
        private final Set<Pair<ClientInfo, Integer>> mActiveWifiChangeHandlers;
        private ScanResult[] mCurrentBssids;
        State mDefaultState;
        private InternalClientInfo mInternalClientInfo;
        State mMovingState;
        State mStationaryState;
        private PendingIntent mTimeoutIntent;

        WifiChangeStateMachine(Looper looper) {
            super("SignificantChangeStateMachine", looper);
            this.mDefaultState = new DefaultState();
            this.mStationaryState = new StationaryState();
            this.mMovingState = new MovingState();
            this.mActiveWifiChangeHandlers = new HashSet();
            addState(this.mDefaultState);
            addState(this.mStationaryState, this.mDefaultState);
            addState(this.mMovingState, this.mDefaultState);
            setInitialState(this.mDefaultState);
        }

        public void removeWifiChangeHandler(ClientInfo ci) {
            Iterator<Pair<ClientInfo, Integer>> iter = this.mActiveWifiChangeHandlers.iterator();
            while (iter.hasNext()) {
                Pair<ClientInfo, Integer> entry = iter.next();
                if (entry.first == ci) {
                    iter.remove();
                }
            }
            untrackSignificantWifiChangeOnEmpty();
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("Entering IdleState");
            }

            public boolean processMessage(Message msg) {
                WifiScanningServiceImpl.this.localLog("DefaultState state got " + msg);
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159749:
                    case 159756:
                        return true;
                    case 159750:
                    case 159751:
                    case 159752:
                    case 159753:
                    case 159754:
                    default:
                        return false;
                    case 159755:
                        WifiChangeStateMachine.this.addWifiChangeHandler(ci, msg.arg2);
                        WifiScanningServiceImpl.this.replySucceeded(msg);
                        WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mMovingState);
                        return true;
                    case 159757:
                        WifiChangeStateMachine.this.deferMessage(msg);
                        return true;
                }
            }
        }

        class StationaryState extends State {
            StationaryState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("Entering StationaryState");
                WifiChangeStateMachine.this.reportWifiStabilized(WifiChangeStateMachine.this.mCurrentBssids);
            }

            public boolean processMessage(Message msg) {
                WifiScanningServiceImpl.this.localLog("Stationary state got " + msg);
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159749:
                        return true;
                    case 159755:
                        WifiChangeStateMachine.this.addWifiChangeHandler(ci, msg.arg2);
                        WifiScanningServiceImpl.this.replySucceeded(msg);
                        return true;
                    case 159756:
                        WifiChangeStateMachine.this.removeWifiChangeHandler(ci, msg.arg2);
                        return true;
                    case 159757:
                        WifiChangeStateMachine.this.deferMessage(msg);
                        return true;
                    case WifiScanningServiceImpl.CMD_WIFI_CHANGE_DETECTED:
                        WifiScanningServiceImpl.this.localLog("Got wifi change detected");
                        WifiChangeStateMachine.this.reportWifiChanged((ScanResult[]) msg.obj);
                        WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mMovingState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class MovingState extends State {
            boolean mWifiChangeDetected = false;
            boolean mScanResultsPending = false;

            MovingState() {
            }

            public void enter() {
                WifiScanningServiceImpl.this.localLog("Entering MovingState");
                if (WifiChangeStateMachine.this.mTimeoutIntent == null) {
                    Intent intent = new Intent(WifiChangeStateMachine.ACTION_TIMEOUT, (Uri) null);
                    WifiChangeStateMachine.this.mTimeoutIntent = PendingIntent.getBroadcast(WifiScanningServiceImpl.this.mContext, 0, intent, 0);
                    WifiScanningServiceImpl.this.mContext.registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent2) {
                            WifiChangeStateMachine.this.sendMessage(WifiScanningServiceImpl.CMD_WIFI_CHANGE_TIMEOUT);
                        }
                    }, new IntentFilter(WifiChangeStateMachine.ACTION_TIMEOUT));
                }
                issueFullScan();
            }

            public boolean processMessage(Message msg) {
                WifiScanningServiceImpl.this.localLog("MovingState state got " + msg);
                ClientInfo ci = (ClientInfo) WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159749:
                        WifiScanningServiceImpl.this.localLog("Got scan results");
                        if (this.mScanResultsPending) {
                            WifiScanningServiceImpl.this.localLog("reconfiguring scan");
                            WifiScanner.ParcelableScanData parcelableScanData = (WifiScanner.ParcelableScanData) msg.obj;
                            WifiScanner.ScanData[] scanDatas = parcelableScanData.getResults();
                            WifiChangeStateMachine.this.reconfigureScan(scanDatas, WifiChangeStateMachine.STATIONARY_SCAN_PERIOD_MS);
                            this.mWifiChangeDetected = false;
                            WifiScanningServiceImpl.this.mAlarmManager.setExact(2, WifiScanningServiceImpl.this.mClock.elapsedRealtime() + 30000, WifiChangeStateMachine.this.mTimeoutIntent);
                            this.mScanResultsPending = false;
                            return true;
                        }
                        return true;
                    case 159755:
                        WifiChangeStateMachine.this.addWifiChangeHandler(ci, msg.arg2);
                        WifiScanningServiceImpl.this.replySucceeded(msg);
                        return true;
                    case 159756:
                        WifiChangeStateMachine.this.removeWifiChangeHandler(ci, msg.arg2);
                        return true;
                    case 159757:
                        WifiScanningServiceImpl.this.localLog("Got configuration from app");
                        WifiScanner.WifiChangeSettings settings = (WifiScanner.WifiChangeSettings) msg.obj;
                        WifiChangeStateMachine.this.reconfigureScan(settings);
                        this.mWifiChangeDetected = false;
                        long unchangedDelay = settings.unchangedSampleSize * settings.periodInMs;
                        WifiScanningServiceImpl.this.mAlarmManager.cancel(WifiChangeStateMachine.this.mTimeoutIntent);
                        WifiScanningServiceImpl.this.mAlarmManager.setExact(2, WifiScanningServiceImpl.this.mClock.elapsedRealtime() + unchangedDelay, WifiChangeStateMachine.this.mTimeoutIntent);
                        return true;
                    case WifiScanningServiceImpl.CMD_WIFI_CHANGE_DETECTED:
                        WifiScanningServiceImpl.this.localLog("Change detected");
                        WifiScanningServiceImpl.this.mAlarmManager.cancel(WifiChangeStateMachine.this.mTimeoutIntent);
                        WifiChangeStateMachine.this.reportWifiChanged((ScanResult[]) msg.obj);
                        this.mWifiChangeDetected = true;
                        issueFullScan();
                        return true;
                    case WifiScanningServiceImpl.CMD_WIFI_CHANGE_TIMEOUT:
                        WifiScanningServiceImpl.this.localLog("Got timeout event");
                        if (!this.mWifiChangeDetected) {
                            WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mStationaryState);
                            return true;
                        }
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                WifiScanningServiceImpl.this.mAlarmManager.cancel(WifiChangeStateMachine.this.mTimeoutIntent);
            }

            void issueFullScan() {
                WifiScanningServiceImpl.this.localLog("Issuing full scan");
                WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
                settings.band = 3;
                settings.periodInMs = WifiChangeStateMachine.MOVING_SCAN_PERIOD_MS;
                settings.reportEvents = 1;
                WifiChangeStateMachine.this.addScanRequest(settings);
                this.mScanResultsPending = true;
            }
        }

        private void reconfigureScan(WifiScanner.ScanData[] results, int period) {
            if (results.length < 3) {
                WifiScanningServiceImpl.this.localLog("too few APs (" + results.length + ") available to track wifi change");
                return;
            }
            removeScanRequest();
            HashMap<String, ScanResult> bssidToScanResult = new HashMap<>();
            for (ScanResult result : results[0].getResults()) {
                ScanResult saved = bssidToScanResult.get(result.BSSID);
                if (saved == null) {
                    bssidToScanResult.put(result.BSSID, result);
                } else if (saved.level > result.level) {
                    bssidToScanResult.put(result.BSSID, result);
                }
            }
            ScanResult[] brightest = new ScanResult[3];
            Collection<ScanResult> results2 = bssidToScanResult.values();
            for (ScanResult result2 : results2) {
                for (int j = 0; j < brightest.length; j++) {
                    if (brightest[j] == null || brightest[j].level < result2.level) {
                        for (int k = brightest.length; k > j + 1; k--) {
                            brightest[k - 1] = brightest[k - 2];
                        }
                        brightest[j] = result2;
                    }
                }
            }
            ArrayList<Integer> channels = new ArrayList<>();
            for (int i = 0; i < brightest.length; i++) {
                boolean found = false;
                for (int j2 = i + 1; j2 < brightest.length; j2++) {
                    if (brightest[j2].frequency == brightest[i].frequency) {
                        found = true;
                    }
                }
                if (!found) {
                    channels.add(Integer.valueOf(brightest[i].frequency));
                }
            }
            WifiScanningServiceImpl.this.localLog("Found " + channels.size() + " channels");
            WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
            settings.band = 0;
            settings.channels = new WifiScanner.ChannelSpec[channels.size()];
            for (int i2 = 0; i2 < channels.size(); i2++) {
                settings.channels[i2] = new WifiScanner.ChannelSpec(channels.get(i2).intValue());
            }
            settings.periodInMs = period;
            addScanRequest(settings);
            WifiScanner.WifiChangeSettings settings2 = new WifiScanner.WifiChangeSettings();
            settings2.rssiSampleSize = 3;
            settings2.lostApSampleSize = 3;
            settings2.unchangedSampleSize = 3;
            settings2.minApsBreachingThreshold = 2;
            settings2.bssidInfos = new WifiScanner.BssidInfo[brightest.length];
            for (int i3 = 0; i3 < brightest.length; i3++) {
                WifiScanner.BssidInfo BssidInfo = new WifiScanner.BssidInfo();
                BssidInfo.bssid = brightest[i3].BSSID;
                int threshold = ((brightest[i3].level + 100) / 32) + 2;
                BssidInfo.low = brightest[i3].level - threshold;
                BssidInfo.high = brightest[i3].level + threshold;
                settings2.bssidInfos[i3] = BssidInfo;
                WifiScanningServiceImpl.this.localLog("Setting bssid=" + BssidInfo.bssid + ", low=" + BssidInfo.low + ", high=" + BssidInfo.high);
            }
            trackSignificantWifiChange(settings2);
            this.mCurrentBssids = brightest;
        }

        private void reconfigureScan(WifiScanner.WifiChangeSettings settings) {
            if (settings.bssidInfos.length < 3) {
                WifiScanningServiceImpl.this.localLog("too few APs (" + settings.bssidInfos.length + ") available to track wifi change");
                return;
            }
            WifiScanningServiceImpl.this.localLog("Setting configuration specified by app");
            this.mCurrentBssids = new ScanResult[settings.bssidInfos.length];
            HashSet<Integer> channels = new HashSet<>();
            for (int i = 0; i < settings.bssidInfos.length; i++) {
                ScanResult result = new ScanResult();
                result.BSSID = settings.bssidInfos[i].bssid;
                this.mCurrentBssids[i] = result;
                channels.add(Integer.valueOf(settings.bssidInfos[i].frequencyHint));
            }
            removeScanRequest();
            WifiScanner.ScanSettings settings2 = new WifiScanner.ScanSettings();
            settings2.band = 0;
            settings2.channels = new WifiScanner.ChannelSpec[channels.size()];
            int i2 = 0;
            for (Integer channel : channels) {
                settings2.channels[i2] = new WifiScanner.ChannelSpec(channel.intValue());
                i2++;
            }
            settings2.periodInMs = settings.periodInMs;
            addScanRequest(settings2);
            trackSignificantWifiChange(settings);
        }

        @Override
        public void onChangesFound(ScanResult[] results) {
            sendMessage(WifiScanningServiceImpl.CMD_WIFI_CHANGE_DETECTED, 0, 0, results);
        }

        private void addScanRequest(WifiScanner.ScanSettings settings) {
            WifiScanningServiceImpl.this.localLog("Starting scans");
            if (this.mInternalClientInfo == null) {
                return;
            }
            this.mInternalClientInfo.sendRequestToClientHandler(159746, settings, null);
        }

        private void removeScanRequest() {
            WifiScanningServiceImpl.this.localLog("Stopping scans");
            if (this.mInternalClientInfo == null) {
                return;
            }
            this.mInternalClientInfo.sendRequestToClientHandler(159747);
        }

        private void trackSignificantWifiChange(WifiScanner.WifiChangeSettings settings) {
            if (WifiScanningServiceImpl.this.mScannerImpl == null) {
                return;
            }
            WifiScanningServiceImpl.this.mScannerImpl.untrackSignificantWifiChange();
            WifiScanningServiceImpl.this.mScannerImpl.trackSignificantWifiChange(settings, this);
        }

        private void untrackSignificantWifiChange() {
            if (WifiScanningServiceImpl.this.mScannerImpl == null) {
                return;
            }
            WifiScanningServiceImpl.this.mScannerImpl.untrackSignificantWifiChange();
        }

        private void addWifiChangeHandler(ClientInfo ci, int handler) {
            this.mActiveWifiChangeHandlers.add(Pair.create(ci, Integer.valueOf(handler)));
            if (this.mInternalClientInfo != null) {
                return;
            }
            this.mInternalClientInfo = WifiScanningServiceImpl.this.new InternalClientInfo(ci.getUid(), new Messenger(getHandler()));
            this.mInternalClientInfo.register();
        }

        private void removeWifiChangeHandler(ClientInfo ci, int handler) {
            this.mActiveWifiChangeHandlers.remove(Pair.create(ci, Integer.valueOf(handler)));
            untrackSignificantWifiChangeOnEmpty();
        }

        private void untrackSignificantWifiChangeOnEmpty() {
            if (!this.mActiveWifiChangeHandlers.isEmpty()) {
                return;
            }
            WifiScanningServiceImpl.this.localLog("Got Disable Wifi Change");
            this.mCurrentBssids = null;
            untrackSignificantWifiChange();
            if (this.mInternalClientInfo != null) {
                this.mInternalClientInfo.cleanup();
                this.mInternalClientInfo = null;
            }
            transitionTo(this.mDefaultState);
        }

        private void reportWifiChanged(ScanResult[] results) {
            WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results);
            for (Pair<ClientInfo, Integer> entry : this.mActiveWifiChangeHandlers) {
                ClientInfo ci = (ClientInfo) entry.first;
                int handler = ((Integer) entry.second).intValue();
                ci.reportEvent(159759, 0, handler, parcelableScanResults);
            }
        }

        private void reportWifiStabilized(ScanResult[] results) {
            WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results);
            for (Pair<ClientInfo, Integer> entry : this.mActiveWifiChangeHandlers) {
                ClientInfo ci = (ClientInfo) entry.first;
                int handler = ((Integer) entry.second).intValue();
                ci.reportEvent(159760, 0, handler, parcelableScanResults);
            }
        }
    }

    private static String toString(int uid, WifiScanner.ScanSettings settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("ScanSettings[uid=").append(uid);
        sb.append(", period=").append(settings.periodInMs);
        sb.append(", report=").append(settings.reportEvents);
        if (settings.reportEvents == 0 && settings.numBssidsPerScan > 0 && settings.maxScansToCache > 1) {
            sb.append(", batch=").append(settings.maxScansToCache);
            sb.append(", numAP=").append(settings.numBssidsPerScan);
        }
        sb.append(", ").append(ChannelHelper.toString(settings));
        sb.append("]");
        return sb.toString();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        WifiNative.ScanSettings schedule;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiScanner from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        pw.println("WifiScanningService - Log Begin ----");
        this.mLocalLog.dump(fd, pw, args);
        pw.println("WifiScanningService - Log End ----");
        pw.println();
        pw.println("clients:");
        Iterator client$iterator = this.mClients.values().iterator();
        while (client$iterator.hasNext()) {
            pw.println("  " + ((ClientInfo) client$iterator.next()));
        }
        pw.println("listeners:");
        for (ClientInfo client : this.mClients.values()) {
            Collection<WifiScanner.ScanSettings> settingsList = this.mBackgroundScanStateMachine.getBackgroundScanSettings(client);
            for (WifiScanner.ScanSettings settings : settingsList) {
                pw.println("  " + toString(client.mUid, settings));
            }
        }
        if (this.mBackgroundScheduler != null && (schedule = this.mBackgroundScheduler.getSchedule()) != null) {
            pw.println("schedule:");
            pw.println("  base period: " + schedule.base_period_ms);
            pw.println("  max ap per scan: " + schedule.max_ap_per_scan);
            pw.println("  batched scans: " + schedule.report_threshold_num_scans);
            pw.println("  buckets:");
            for (int b = 0; b < schedule.num_buckets; b++) {
                WifiNative.BucketSettings bucket = schedule.buckets[b];
                pw.println("    bucket " + bucket.bucket + " (" + bucket.period_ms + "ms)[" + bucket.report_events + "]: " + ChannelHelper.toString(bucket));
            }
        }
        if (this.mPnoScanStateMachine != null) {
            this.mPnoScanStateMachine.dump(fd, pw, args);
        }
    }

    void logScanRequest(String request, ClientInfo ci, int id, WorkSource workSource, WifiScanner.ScanSettings settings, WifiScanner.PnoSettings pnoSettings) {
        StringBuilder sb = new StringBuilder();
        sb.append(request).append(": ").append(ci.toString()).append(",Id=").append(id);
        if (workSource != null) {
            sb.append(",").append(workSource);
        }
        if (settings != null) {
            sb.append(", ");
            describeTo(sb, settings);
        }
        if (pnoSettings != null) {
            sb.append(", ");
            describeTo(sb, pnoSettings);
        }
        localLog(sb.toString());
    }

    void logCallback(String callback, ClientInfo ci, int id, String extra) {
        StringBuilder sb = new StringBuilder();
        sb.append(callback).append(": ");
        if (ci != null) {
            sb.append(ci.toString());
        }
        sb.append(",Id=").append(id);
        if (extra != null) {
            sb.append(",").append(extra);
        }
        localLog(sb.toString());
    }

    static String describeForLog(WifiScanner.ScanData[] results) {
        StringBuilder sb = new StringBuilder();
        sb.append("results=");
        for (int i = 0; i < results.length; i++) {
            if (i > 0) {
                sb.append(";");
            }
            sb.append(results[i].getResults().length);
        }
        return sb.toString();
    }

    static String describeForLog(ScanResult[] results) {
        return "results=" + results.length;
    }

    static String describeTo(StringBuilder sb, WifiScanner.ScanSettings scanSettings) {
        sb.append("ScanSettings { ").append(" band:").append(scanSettings.band).append(" period:").append(scanSettings.periodInMs).append(" reportEvents:").append(scanSettings.reportEvents).append(" numBssidsPerScan:").append(scanSettings.numBssidsPerScan).append(" maxScansToCache:").append(scanSettings.maxScansToCache).append(" channels:[ ");
        if (scanSettings.channels != null) {
            for (int i = 0; i < scanSettings.channels.length; i++) {
                sb.append(scanSettings.channels[i].frequency).append(" ");
            }
        }
        sb.append(" ] ");
        sb.append(" maxPeriodInMs:").append(scanSettings.maxPeriodInMs).append(" stepCount:").append(scanSettings.stepCount).append(" } ");
        return sb.toString();
    }

    static String describeTo(StringBuilder sb, WifiScanner.PnoSettings pnoSettings) {
        sb.append("PnoSettings { ").append(" min5GhzRssi:").append(pnoSettings.min5GHzRssi).append(" min24GhzRssi:").append(pnoSettings.min24GHzRssi).append(" initialScoreMax:").append(pnoSettings.initialScoreMax).append(" currentConnectionBonus:").append(pnoSettings.currentConnectionBonus).append(" sameNetworkBonus:").append(pnoSettings.sameNetworkBonus).append(" secureBonus:").append(pnoSettings.secureBonus).append(" band5GhzBonus:").append(pnoSettings.band5GHzBonus).append(" isConnected:").append(pnoSettings.isConnected).append(" networks:[ ");
        if (pnoSettings.networkList != null) {
            for (int i = 0; i < pnoSettings.networkList.length; i++) {
                sb.append(pnoSettings.networkList[i].ssid).append(",").append(pnoSettings.networkList[i].networkId).append(" ");
            }
        }
        sb.append(" ] ").append(" } ");
        return sb.toString();
    }
}
