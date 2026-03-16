package com.android.server.wifi;

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
import android.net.wifi.WifiSsid;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiNative;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class WifiScanningServiceImpl extends IWifiScanner.Stub {
    private static final int BASE = 160000;
    private static final int CMD_DRIVER_LOADED = 160006;
    private static final int CMD_DRIVER_UNLOADED = 160007;
    private static final int CMD_FULL_SCAN_RESULTS = 160001;
    private static final int CMD_HOTLIST_AP_FOUND = 160002;
    private static final int CMD_HOTLIST_AP_LOST = 160003;
    private static final int CMD_SCAN_PAUSED = 160008;
    private static final int CMD_SCAN_RESTARTED = 160009;
    private static final int CMD_SCAN_RESULTS_AVAILABLE = 160000;
    private static final int CMD_WIFI_CHANGES_STABILIZED = 160005;
    private static final int CMD_WIFI_CHANGE_DETECTED = 160004;
    private static final boolean DBG = true;
    private static final int INVALID_KEY = 0;
    private static final int MIN_PERIOD_PER_CHANNEL_MS = 200;
    private static final String TAG = "WifiScanningService";
    private ClientHandler mClientHandler;
    HashMap<Messenger, ClientInfo> mClients = new HashMap<>();
    private Context mContext;
    private WifiScanningStateMachine mStateMachine;
    WifiChangeStateMachine mWifiChangeStateMachine;

    public Messenger getMessenger() {
        return new Messenger(this.mClientHandler);
    }

    public Bundle getAvailableChannels(int band) {
        WifiScanner.ChannelSpec[] channelSpecs = getChannelsForBand(band);
        ArrayList<Integer> list = new ArrayList<>(channelSpecs.length);
        for (WifiScanner.ChannelSpec channelSpec : channelSpecs) {
            list.add(Integer.valueOf(channelSpec.frequency));
        }
        Bundle b = new Bundle();
        b.putIntegerArrayList("Channels", list);
        return b;
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", "WifiScanningServiceImpl");
    }

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(WifiScanningServiceImpl.TAG, "ClientHandler got" + msg);
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        AsyncChannel c = (AsyncChannel) msg.obj;
                        Slog.d(WifiScanningServiceImpl.TAG, "New client listening to asynchronous messages: " + msg.replyTo);
                        ClientInfo cInfo = WifiScanningServiceImpl.this.new ClientInfo(c, msg.replyTo);
                        WifiScanningServiceImpl.this.mClients.put(msg.replyTo, cInfo);
                    } else {
                        Slog.e(WifiScanningServiceImpl.TAG, "Client connection failure, error=" + msg.arg1);
                    }
                    break;
                case 69633:
                    AsyncChannel ac = new AsyncChannel();
                    ac.connect(WifiScanningServiceImpl.this.mContext, this, msg.replyTo);
                    break;
                case 69634:
                case 69635:
                default:
                    if (WifiScanningServiceImpl.this.mClients.get(msg.replyTo) != null) {
                        try {
                            WifiScanningServiceImpl.this.enforceConnectivityInternalPermission();
                            int[] validCommands = {159744, 159746, 159747, 159750, 159751, 159757, 159755, 159756};
                            for (int cmd : validCommands) {
                                if (cmd == msg.what) {
                                    WifiScanningServiceImpl.this.mStateMachine.sendMessage(Message.obtain(msg));
                                }
                                break;
                            }
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "Invalid request");
                        } catch (SecurityException e) {
                            WifiScanningServiceImpl.this.replyFailed(msg, -4, "Not authorized");
                            return;
                        }
                    } else {
                        Slog.e(WifiScanningServiceImpl.TAG, "Could not find client info for message " + msg.replyTo);
                        WifiScanningServiceImpl.this.replyFailed(msg, -2, "Could not find listener");
                    }
                    break;
                case 69636:
                    if (msg.arg1 == 2) {
                        Slog.e(WifiScanningServiceImpl.TAG, "Send failed, client connection lost");
                    } else {
                        Slog.d(WifiScanningServiceImpl.TAG, "Client connection lost with reason: " + msg.arg1);
                    }
                    Slog.d(WifiScanningServiceImpl.TAG, "closing client " + msg.replyTo);
                    ClientInfo ci = WifiScanningServiceImpl.this.mClients.remove(msg.replyTo);
                    if (ci != null) {
                        ci.cleanup();
                    }
                    break;
            }
        }
    }

    WifiScanningServiceImpl() {
    }

    WifiScanningServiceImpl(Context context) {
        this.mContext = context;
    }

    public void startService(Context context) {
        this.mContext = context;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mClientHandler = new ClientHandler(thread.getLooper());
        this.mStateMachine = new WifiScanningStateMachine(thread.getLooper());
        this.mWifiChangeStateMachine = new WifiChangeStateMachine(thread.getLooper());
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                int state = intent.getIntExtra("scan_enabled", 1);
                Log.d(WifiScanningServiceImpl.TAG, "SCAN_AVAILABLE : " + state);
                if (state == 3) {
                    WifiScanningServiceImpl.this.mStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_LOADED);
                } else if (state == 1) {
                    WifiScanningServiceImpl.this.mStateMachine.sendMessage(WifiScanningServiceImpl.CMD_DRIVER_UNLOADED);
                }
            }
        }, new IntentFilter("wifi_scan_available"));
        this.mStateMachine.start();
        this.mWifiChangeStateMachine.start();
    }

    class WifiScanningStateMachine extends StateMachine implements WifiNative.ScanEventHandler, WifiNative.HotlistEventHandler, WifiNative.SignificantWifiChangeEventHandler {
        private final DefaultState mDefaultState;
        private final PausedState mPausedState;
        private final StartedState mStartedState;

        public WifiScanningStateMachine(Looper looper) {
            super(WifiScanningServiceImpl.TAG, looper);
            this.mDefaultState = new DefaultState();
            this.mStartedState = new StartedState();
            this.mPausedState = new PausedState();
            setLogRecSize(512);
            setLogOnlyTransitions(false);
            addState(this.mDefaultState);
            addState(this.mStartedState, this.mDefaultState);
            addState(this.mPausedState, this.mDefaultState);
            setInitialState(this.mDefaultState);
        }

        @Override
        public void onScanResultsAvailable() {
            Log.d(WifiScanningServiceImpl.TAG, "onScanResultAvailable event received");
            sendMessage(160000);
        }

        @Override
        public void onSingleScanComplete() {
            Log.d(WifiScanningServiceImpl.TAG, "onSingleScanComplete event received");
            sendMessage(160000);
        }

        @Override
        public void onFullScanResult(ScanResult fullScanResult) {
            Log.d(WifiScanningServiceImpl.TAG, "Full scanresult received");
            sendMessage(WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS, 0, 0, fullScanResult);
        }

        @Override
        public void onScanPaused() {
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_PAUSED);
        }

        @Override
        public void onScanRestarted() {
            sendMessage(WifiScanningServiceImpl.CMD_SCAN_RESTARTED);
        }

        @Override
        public void onHotlistApFound(ScanResult[] results) {
            Log.d(WifiScanningServiceImpl.TAG, "HotlistApFound event received");
            sendMessage(WifiScanningServiceImpl.CMD_HOTLIST_AP_FOUND, 0, 0, results);
        }

        @Override
        public void onChangesFound(ScanResult[] results) {
            Log.d(WifiScanningServiceImpl.TAG, "onWifiChangesFound event received");
            sendMessage(WifiScanningServiceImpl.CMD_WIFI_CHANGE_DETECTED, 0, 0, results);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                Log.d(WifiScanningServiceImpl.TAG, "DefaultState");
            }

            public boolean processMessage(Message msg) {
                Log.d(WifiScanningServiceImpl.TAG, "DefaultState got" + msg);
                WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159744:
                    case 159746:
                    case 159747:
                    case 159750:
                    case 159751:
                    case 159755:
                    case 159756:
                    case 159757:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "not available");
                        break;
                    case 160000:
                        WifiScanningStateMachine.this.log("ignored scan results available event");
                        break;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        WifiScanningStateMachine.this.log("ignored full scan result event");
                        break;
                    case WifiScanningServiceImpl.CMD_DRIVER_LOADED:
                        if (!WifiNative.startHal() || WifiNative.getInterfaces() == 0) {
                            WifiScanningStateMachine.this.loge("could not start HAL");
                        } else {
                            WifiNative.ScanCapabilities capabilities = new WifiNative.ScanCapabilities();
                            if (WifiNative.getScanCapabilities(capabilities)) {
                                WifiScanningStateMachine.this.transitionTo(WifiScanningStateMachine.this.mStartedState);
                            } else {
                                WifiScanningStateMachine.this.loge("could not get scan capabilities");
                            }
                        }
                        break;
                }
                return true;
            }
        }

        class StartedState extends State {
            StartedState() {
            }

            public void enter() {
                Log.d(WifiScanningServiceImpl.TAG, "StartedState");
            }

            public boolean processMessage(Message msg) {
                Log.d(WifiScanningServiceImpl.TAG, "StartedState got" + msg);
                ClientInfo ci = WifiScanningServiceImpl.this.mClients.get(msg.replyTo);
                switch (msg.what) {
                    case 159744:
                        WifiScanningServiceImpl.this.replyFailed(msg, -1, "not implemented");
                        return true;
                    case 159746:
                        if (WifiScanningServiceImpl.this.addScanRequest(ci, msg.arg2, (WifiScanner.ScanSettings) msg.obj)) {
                            WifiScanningServiceImpl.this.replySucceeded(msg, null);
                        } else {
                            WifiScanningServiceImpl.this.replyFailed(msg, -3, "bad request");
                        }
                        return true;
                    case 159747:
                        WifiScanningServiceImpl.this.removeScanRequest(ci, msg.arg2);
                        return true;
                    case 159748:
                        WifiScanningServiceImpl.this.replySucceeded(msg, WifiScanningServiceImpl.this.getScanResults(ci));
                        return true;
                    case 159750:
                        WifiScanningServiceImpl.this.setHotlist(ci, msg.arg2, (WifiScanner.HotlistSettings) msg.obj);
                        WifiScanningServiceImpl.this.replySucceeded(msg, null);
                        return true;
                    case 159751:
                        WifiScanningServiceImpl.this.resetHotlist(ci, msg.arg2);
                        return true;
                    case 159755:
                        WifiScanningServiceImpl.this.trackWifiChanges(ci, msg.arg2);
                        WifiScanningServiceImpl.this.replySucceeded(msg, null);
                        return true;
                    case 159756:
                        WifiScanningServiceImpl.this.untrackWifiChanges(ci, msg.arg2);
                        return true;
                    case 159757:
                        WifiScanningServiceImpl.this.configureWifiChange((WifiScanner.WifiChangeSettings) msg.obj);
                        return true;
                    case 160000:
                        ScanResult[] results = WifiNative.getScanResults();
                        Collection<ClientInfo> clients = WifiScanningServiceImpl.this.mClients.values();
                        for (ClientInfo ci2 : clients) {
                            ci2.reportScanResults(results);
                        }
                        return true;
                    case WifiScanningServiceImpl.CMD_FULL_SCAN_RESULTS:
                        ScanResult result = (ScanResult) msg.obj;
                        Log.d(WifiScanningServiceImpl.TAG, "reporting fullscan result for " + result.SSID);
                        Collection<ClientInfo> clients2 = WifiScanningServiceImpl.this.mClients.values();
                        for (ClientInfo ci22 : clients2) {
                            ci22.reportFullScanResult(result);
                        }
                        return true;
                    case WifiScanningServiceImpl.CMD_HOTLIST_AP_FOUND:
                        ScanResult[] results2 = (ScanResult[]) msg.obj;
                        Log.d(WifiScanningServiceImpl.TAG, "Found " + results2.length + " results");
                        Collection<ClientInfo> clients3 = WifiScanningServiceImpl.this.mClients.values();
                        for (ClientInfo ci23 : clients3) {
                            ci23.reportHotlistResults(results2);
                        }
                        return true;
                    case WifiScanningServiceImpl.CMD_WIFI_CHANGE_DETECTED:
                        ScanResult[] results3 = (ScanResult[]) msg.obj;
                        WifiScanningServiceImpl.this.reportWifiChanged(results3);
                        return true;
                    case WifiScanningServiceImpl.CMD_WIFI_CHANGES_STABILIZED:
                        ScanResult[] results4 = (ScanResult[]) msg.obj;
                        WifiScanningServiceImpl.this.reportWifiStabilized(results4);
                        return true;
                    case WifiScanningServiceImpl.CMD_DRIVER_UNLOADED:
                        WifiScanningStateMachine.this.transitionTo(WifiScanningStateMachine.this.mDefaultState);
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
                Log.d(WifiScanningServiceImpl.TAG, "PausedState");
            }

            public boolean processMessage(Message msg) {
                Log.d(WifiScanningServiceImpl.TAG, "PausedState got" + msg);
                switch (msg.what) {
                    case WifiScanningServiceImpl.CMD_SCAN_RESTARTED:
                        WifiScanningStateMachine.this.transitionTo(WifiScanningStateMachine.this.mStartedState);
                        break;
                    default:
                        WifiScanningStateMachine.this.deferMessage(msg);
                        break;
                }
                return true;
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            super.dump(fd, pw, args);
            pw.println("number of clients : " + WifiScanningServiceImpl.this.mClients.size());
            pw.println();
        }
    }

    private class ClientInfo {
        private static final int MAX_LIMIT = 16;
        private final AsyncChannel mChannel;
        private final Messenger mMessenger;
        HashMap<Integer, WifiScanner.ScanSettings> mScanSettings = new HashMap<>(4);
        HashMap<Integer, Integer> mScanPeriods = new HashMap<>(4);
        HashMap<Integer, WifiScanner.HotlistSettings> mHotlistSettings = new HashMap<>();
        HashSet<Integer> mSignificantWifiHandlers = new HashSet<>();

        ClientInfo(AsyncChannel c, Messenger m) {
            this.mChannel = c;
            this.mMessenger = m;
            Slog.d(WifiScanningServiceImpl.TAG, "New client, channel: " + c + " messenger: " + m);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("mChannel ").append(this.mChannel).append("\n");
            sb.append("mMessenger ").append(this.mMessenger).append("\n");
            for (Map.Entry<Integer, WifiScanner.ScanSettings> entry : this.mScanSettings.entrySet()) {
                sb.append("[ScanId ").append(entry.getKey()).append("\n");
                sb.append("ScanSettings ").append(entry.getValue()).append("\n");
                sb.append("]");
            }
            return sb.toString();
        }

        void addScanRequest(WifiScanner.ScanSettings settings, int id) {
            this.mScanSettings.put(Integer.valueOf(id), settings);
        }

        void removeScanRequest(int id) {
            this.mScanSettings.remove(Integer.valueOf(id));
        }

        Iterator<Map.Entry<Integer, WifiScanner.ScanSettings>> getScans() {
            return this.mScanSettings.entrySet().iterator();
        }

        Collection<WifiScanner.ScanSettings> getScanSettings() {
            return this.mScanSettings.values();
        }

        void reportScanResults(ScanResult[] results) {
            Iterator<Integer> it = this.mScanSettings.keySet().iterator();
            while (it.hasNext()) {
                int handler = it.next().intValue();
                reportScanResults(results, handler);
            }
        }

        void reportScanResults(ScanResult[] results, int handler) {
            WifiScanner.ScanSettings settings = this.mScanSettings.get(Integer.valueOf(handler));
            WifiScanner.ChannelSpec[] desiredChannels = settings.channels;
            if (settings.band != 0 || desiredChannels == null || desiredChannels.length == 0) {
                desiredChannels = WifiScanningServiceImpl.getChannelsForBand(settings.band);
            }
            int num_results = 0;
            int length = results.length;
            int i$ = 0;
            while (true) {
                int i$2 = i$;
                if (i$2 >= length) {
                    break;
                }
                ScanResult result = results[i$2];
                WifiScanner.ChannelSpec[] arr$ = desiredChannels;
                int len$ = arr$.length;
                int i$3 = 0;
                while (true) {
                    if (i$3 < len$) {
                        WifiScanner.ChannelSpec channelSpec = arr$[i$3];
                        if (channelSpec.frequency != result.frequency) {
                            i$3++;
                        } else {
                            num_results++;
                            break;
                        }
                    }
                }
                i$ = i$2 + 1;
            }
            if (num_results != 0) {
                ScanResult[] results2 = new ScanResult[num_results];
                int index = 0;
                int length2 = results.length;
                int i$4 = 0;
                while (true) {
                    int i$5 = i$4;
                    if (i$5 < length2) {
                        ScanResult result2 = results[i$5];
                        WifiScanner.ChannelSpec[] arr$2 = desiredChannels;
                        int len$2 = arr$2.length;
                        int i$6 = 0;
                        while (true) {
                            if (i$6 < len$2) {
                                WifiScanner.ChannelSpec channelSpec2 = arr$2[i$6];
                                if (channelSpec2.frequency != result2.frequency) {
                                    i$6++;
                                } else {
                                    WifiSsid wifiSsid = WifiSsid.createFromAsciiEncoded(result2.SSID);
                                    ScanResult newResult = new ScanResult(wifiSsid, result2.BSSID, "", result2.level, result2.frequency, result2.timestamp);
                                    results2[index] = newResult;
                                    index++;
                                    break;
                                }
                            }
                        }
                        i$4 = i$5 + 1;
                    } else {
                        deliverScanResults(handler, results2);
                        return;
                    }
                }
            }
        }

        void deliverScanResults(int handler, ScanResult[] results) {
            WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results);
            this.mChannel.sendMessage(159749, 0, handler, parcelableScanResults);
        }

        void reportFullScanResult(ScanResult result) {
            Iterator<Integer> it = this.mScanSettings.keySet().iterator();
            while (it.hasNext()) {
                int handler = it.next().intValue();
                WifiScanner.ScanSettings settings = this.mScanSettings.get(Integer.valueOf(handler));
                WifiScanner.ChannelSpec[] desiredChannels = settings.channels;
                if (settings.band != 0 || desiredChannels == null || desiredChannels.length == 0) {
                    desiredChannels = WifiScanningServiceImpl.getChannelsForBand(settings.band);
                }
                WifiScanner.ChannelSpec[] arr$ = desiredChannels;
                for (WifiScanner.ChannelSpec channelSpec : arr$) {
                    if (channelSpec.frequency == result.frequency) {
                        WifiSsid wifiSsid = WifiSsid.createFromAsciiEncoded(result.SSID);
                        ScanResult newResult = new ScanResult(wifiSsid, result.BSSID, "", result.level, result.frequency, result.timestamp);
                        Log.d(WifiScanningServiceImpl.TAG, "sending it to " + handler);
                        newResult.informationElements = (ScanResult.InformationElement[]) result.informationElements.clone();
                        this.mChannel.sendMessage(159764, 0, handler, newResult);
                    }
                }
            }
        }

        void reportPeriodChanged(int handler, WifiScanner.ScanSettings settings, int newPeriodInMs) {
            Integer prevPeriodObject = this.mScanPeriods.get(Integer.valueOf(handler));
            int prevPeriodInMs = settings.periodInMs;
            if (prevPeriodObject != null) {
                prevPeriodInMs = prevPeriodObject.intValue();
            }
            if (prevPeriodInMs != newPeriodInMs) {
                this.mChannel.sendMessage(159763, newPeriodInMs, handler);
            }
        }

        void addHostlistSettings(WifiScanner.HotlistSettings settings, int handler) {
            this.mHotlistSettings.put(Integer.valueOf(handler), settings);
        }

        void removeHostlistSettings(int handler) {
            this.mHotlistSettings.remove(Integer.valueOf(handler));
        }

        Collection<WifiScanner.HotlistSettings> getHotlistSettings() {
            return this.mHotlistSettings.values();
        }

        void reportHotlistResults(ScanResult[] results) {
            for (Map.Entry<Integer, WifiScanner.HotlistSettings> entry : this.mHotlistSettings.entrySet()) {
                int handler = entry.getKey().intValue();
                WifiScanner.HotlistSettings settings = entry.getValue();
                int num_results = 0;
                for (ScanResult result : results) {
                    WifiScanner.BssidInfo[] arr$ = settings.bssidInfos;
                    int len$ = arr$.length;
                    int i$ = 0;
                    while (true) {
                        if (i$ < len$) {
                            WifiScanner.BssidInfo BssidInfo = arr$[i$];
                            if (!result.BSSID.equalsIgnoreCase(BssidInfo.bssid)) {
                                i$++;
                            } else {
                                num_results++;
                                break;
                            }
                        }
                    }
                }
                if (num_results != 0) {
                    ScanResult[] results2 = new ScanResult[num_results];
                    int index = 0;
                    for (ScanResult result2 : results) {
                        WifiScanner.BssidInfo[] arr$2 = settings.bssidInfos;
                        for (WifiScanner.BssidInfo BssidInfo2 : arr$2) {
                            if (result2.BSSID.equalsIgnoreCase(BssidInfo2.bssid)) {
                                results2[index] = result2;
                                index++;
                            }
                        }
                    }
                    WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results2);
                    this.mChannel.sendMessage(159753, 0, handler, parcelableScanResults);
                } else {
                    return;
                }
            }
        }

        void addSignificantWifiChange(int handler) {
            this.mSignificantWifiHandlers.add(Integer.valueOf(handler));
        }

        void removeSignificantWifiChange(int handler) {
            this.mSignificantWifiHandlers.remove(Integer.valueOf(handler));
        }

        Collection<Integer> getWifiChangeHandlers() {
            return this.mSignificantWifiHandlers;
        }

        void reportWifiChanged(ScanResult[] results) {
            WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results);
            Iterator<Integer> it = this.mSignificantWifiHandlers.iterator();
            while (it.hasNext()) {
                int handler = it.next().intValue();
                this.mChannel.sendMessage(159759, 0, handler, parcelableScanResults);
            }
        }

        void reportWifiStabilized(ScanResult[] results) {
            WifiScanner.ParcelableScanResults parcelableScanResults = new WifiScanner.ParcelableScanResults(results);
            Iterator<Integer> it = this.mSignificantWifiHandlers.iterator();
            while (it.hasNext()) {
                int handler = it.next().intValue();
                this.mChannel.sendMessage(159760, 0, handler, parcelableScanResults);
            }
        }

        void cleanup() {
            this.mScanSettings.clear();
            WifiScanningServiceImpl.this.resetBuckets();
            this.mHotlistSettings.clear();
            WifiScanningServiceImpl.this.resetHotlist();
            for (Integer handler : this.mSignificantWifiHandlers) {
                WifiScanningServiceImpl.this.untrackWifiChanges(this, handler.intValue());
            }
            this.mSignificantWifiHandlers.clear();
            Log.d(WifiScanningServiceImpl.TAG, "Successfully stopped all requests for client " + this);
        }
    }

    void replySucceeded(Message msg, Object obj) {
        if (msg.replyTo != null) {
            Message reply = Message.obtain();
            reply.what = 159761;
            reply.arg2 = msg.arg2;
            reply.obj = obj;
            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
            }
        }
    }

    void replyFailed(Message msg, int reason, String description) {
        if (msg.replyTo != null) {
            Message reply = Message.obtain();
            reply.what = 159762;
            reply.arg2 = msg.arg2;
            reply.obj = new WifiScanner.OperationResult(reason, description);
            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
            }
        }
    }

    private static class SettingsComputer {
        private static final int DEFAULT_BASE_PERIOD_MS = 5000;
        private static final int DEFAULT_MAX_AP_PER_SCAN = 10;
        private static final int DEFAULT_REPORT_THRESHOLD = 10;
        private static final int MAX_BUCKETS = 8;
        private static final int MAX_CHANNELS = 8;
        private static final TimeBucket[] mTimeBuckets = {new TimeBucket(1, 0, 5), new TimeBucket(5, 5, 10), new TimeBucket(10, 10, 25), new TimeBucket(30, 25, 55), new TimeBucket(60, 55, 100), new TimeBucket(300, 240, 500), new TimeBucket(600, 500, 1500), new TimeBucket(1800, 1500, 1024000)};
        HashMap<Integer, Integer> mChannelToBucketMap;
        private WifiNative.ScanSettings mSettings;

        private SettingsComputer() {
            this.mSettings = new WifiNative.ScanSettings();
            this.mSettings.max_ap_per_scan = 10;
            this.mSettings.base_period_ms = DEFAULT_BASE_PERIOD_MS;
            this.mSettings.report_threshold = 10;
            this.mSettings.buckets = new WifiNative.BucketSettings[8];
            for (int i = 0; i < this.mSettings.buckets.length; i++) {
                WifiNative.BucketSettings bucketSettings = new WifiNative.BucketSettings();
                bucketSettings.bucket = i;
                bucketSettings.report_events = 0;
                bucketSettings.channels = new WifiNative.ChannelSettings[8];
                bucketSettings.num_channels = 0;
                for (int j = 0; j < bucketSettings.channels.length; j++) {
                    WifiNative.ChannelSettings channelSettings = new WifiNative.ChannelSettings();
                    bucketSettings.channels[j] = channelSettings;
                }
                this.mSettings.buckets[i] = bucketSettings;
            }
            this.mChannelToBucketMap = new HashMap<>();
        }

        private static class TimeBucket {
            int periodInSecond;
            int periodMaxInSecond;
            int periodMinInSecond;

            TimeBucket(int p, int min, int max) {
                this.periodInSecond = p;
                this.periodMinInSecond = min;
                this.periodMaxInSecond = max;
            }
        }

        private int getBestBucket(WifiScanner.ScanSettings settings) {
            int bucket;
            WifiScanner.ChannelSpec[] channels = settings.channels;
            if (channels == null) {
                channels = WifiScanningServiceImpl.getChannelsForBand(settings.band);
            }
            if (channels == null) {
                Log.e(WifiScanningServiceImpl.TAG, "No channels to scan!!");
                return -1;
            }
            int mostFrequentBucketIndex = mTimeBuckets.length;
            WifiScanner.ChannelSpec[] arr$ = channels;
            for (WifiScanner.ChannelSpec desiredChannelSpec : arr$) {
                if (this.mChannelToBucketMap.containsKey(Integer.valueOf(desiredChannelSpec.frequency)) && (bucket = this.mChannelToBucketMap.get(Integer.valueOf(desiredChannelSpec.frequency)).intValue()) < mostFrequentBucketIndex) {
                    mostFrequentBucketIndex = bucket;
                }
            }
            int bestBucketIndex = -1;
            int i = 0;
            while (true) {
                if (i >= mTimeBuckets.length) {
                    break;
                }
                TimeBucket bucket2 = mTimeBuckets[i];
                if (bucket2.periodMinInSecond * 1000 > settings.periodInMs || settings.periodInMs >= bucket2.periodMaxInSecond * 1000) {
                    i++;
                } else {
                    bestBucketIndex = i;
                    break;
                }
            }
            if (mostFrequentBucketIndex < bestBucketIndex) {
                WifiScanner.ChannelSpec[] arr$2 = channels;
                for (WifiScanner.ChannelSpec desiredChannelSpec2 : arr$2) {
                    this.mChannelToBucketMap.put(Integer.valueOf(desiredChannelSpec2.frequency), Integer.valueOf(mostFrequentBucketIndex));
                }
                Log.d(WifiScanningServiceImpl.TAG, "returning mf bucket number " + mostFrequentBucketIndex);
                return mostFrequentBucketIndex;
            }
            if (bestBucketIndex != -1) {
                WifiScanner.ChannelSpec[] arr$3 = channels;
                for (WifiScanner.ChannelSpec desiredChannelSpec3 : arr$3) {
                    this.mChannelToBucketMap.put(Integer.valueOf(desiredChannelSpec3.frequency), Integer.valueOf(bestBucketIndex));
                }
                Log.d(WifiScanningServiceImpl.TAG, "returning best bucket number " + bestBucketIndex);
                int mostFrequentBucketIndex2 = bestBucketIndex;
                return mostFrequentBucketIndex2;
            }
            Log.e(WifiScanningServiceImpl.TAG, "Could not find suitable bucket for period " + settings.periodInMs);
            return -1;
        }

        void prepChannelMap(WifiScanner.ScanSettings settings) {
            getBestBucket(settings);
        }

        int addScanRequestToBucket(WifiScanner.ScanSettings settings) {
            int bucketIndex = getBestBucket(settings);
            if (bucketIndex == -1) {
                Log.e(WifiScanningServiceImpl.TAG, "Ignoring invalid settings");
                return -1;
            }
            WifiScanner.ChannelSpec[] desiredChannels = settings.channels;
            if ((settings.band != 0 || desiredChannels == null || desiredChannels.length == 0) && (desiredChannels = WifiScanningServiceImpl.getChannelsForBand(settings.band)) == null) {
                Log.e(WifiScanningServiceImpl.TAG, "No channels to scan!!");
                return -1;
            }
            Log.d(WifiScanningServiceImpl.TAG, "merging " + desiredChannels.length + " channels  for period " + settings.periodInMs);
            WifiNative.BucketSettings bucket = this.mSettings.buckets[bucketIndex];
            boolean added = bucket.num_channels == 0 && bucket.band == 0;
            Log.d(WifiScanningServiceImpl.TAG, "existing " + bucket.num_channels + " channels ");
            HashSet<WifiScanner.ChannelSpec> newChannels = new HashSet<>();
            WifiScanner.ChannelSpec[] arr$ = desiredChannels;
            for (WifiScanner.ChannelSpec desiredChannelSpec : arr$) {
                Log.d(WifiScanningServiceImpl.TAG, "desired channel " + desiredChannelSpec.frequency);
                boolean found = false;
                WifiNative.ChannelSettings[] arr$2 = bucket.channels;
                int len$ = arr$2.length;
                int i$ = 0;
                while (true) {
                    if (i$ >= len$) {
                        break;
                    }
                    WifiNative.ChannelSettings existingChannelSpec = arr$2[i$];
                    if (desiredChannelSpec.frequency != existingChannelSpec.frequency) {
                        i$++;
                    } else {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    newChannels.add(desiredChannelSpec);
                } else {
                    Log.d(WifiScanningServiceImpl.TAG, "Already scanning channel " + desiredChannelSpec.frequency);
                }
            }
            if (settings.band != 0 || bucket.num_channels + newChannels.size() > bucket.channels.length) {
                bucket.num_channels = 0;
                bucket.band = WifiScanningServiceImpl.getBandFromChannels(bucket.channels) | WifiScanningServiceImpl.getBandFromChannels(desiredChannels);
                bucket.channels = new WifiNative.ChannelSettings[0];
                Log.d(WifiScanningServiceImpl.TAG, "switching to using band " + bucket.band);
            } else {
                for (WifiScanner.ChannelSpec desiredChannelSpec2 : newChannels) {
                    Log.d(WifiScanningServiceImpl.TAG, "adding new channel spec " + desiredChannelSpec2.frequency);
                    WifiNative.ChannelSettings channelSettings = bucket.channels[bucket.num_channels];
                    channelSettings.frequency = desiredChannelSpec2.frequency;
                    bucket.num_channels++;
                    this.mChannelToBucketMap.put(Integer.valueOf(bucketIndex), Integer.valueOf(channelSettings.frequency));
                }
            }
            if (bucket.report_events < settings.reportEvents) {
                Log.d(WifiScanningServiceImpl.TAG, "setting report_events to " + settings.reportEvents);
                bucket.report_events = settings.reportEvents;
            } else {
                Log.d(WifiScanningServiceImpl.TAG, "report_events is " + settings.reportEvents);
            }
            if (added) {
                bucket.period_ms = mTimeBuckets[bucketIndex].periodInSecond * 1000;
                this.mSettings.num_buckets++;
            }
            if (this.mSettings.max_ap_per_scan < settings.numBssidsPerScan) {
                this.mSettings.max_ap_per_scan = settings.numBssidsPerScan;
            }
            return bucket.period_ms;
        }

        public WifiNative.ScanSettings getComputedSettings() {
            return this.mSettings;
        }

        public void compressBuckets() {
            int num_buckets = 0;
            for (int i = 0; i < this.mSettings.buckets.length; i++) {
                if (this.mSettings.buckets[i].num_channels != 0 || this.mSettings.buckets[i].band != 0) {
                    this.mSettings.buckets[num_buckets] = this.mSettings.buckets[i];
                    num_buckets++;
                }
            }
            for (int i2 = num_buckets; i2 < this.mSettings.buckets.length; i2++) {
                this.mSettings.buckets[i2] = null;
            }
            this.mSettings.num_buckets = num_buckets;
            if (num_buckets != 0) {
                this.mSettings.base_period_ms = this.mSettings.buckets[0].period_ms;
            }
        }
    }

    boolean resetBuckets() {
        SettingsComputer c = new SettingsComputer();
        Collection<ClientInfo> clients = this.mClients.values();
        Iterator<ClientInfo> it = clients.iterator();
        while (it.hasNext()) {
            Collection<WifiScanner.ScanSettings> settings = it.next().getScanSettings();
            Iterator<WifiScanner.ScanSettings> it2 = settings.iterator();
            while (it2.hasNext()) {
                c.prepChannelMap(it2.next());
            }
        }
        for (ClientInfo ci : clients) {
            Iterator<Map.Entry<Integer, WifiScanner.ScanSettings>> scans = ci.getScans();
            while (scans.hasNext()) {
                Map.Entry<Integer, WifiScanner.ScanSettings> entry = scans.next();
                int id = entry.getKey().intValue();
                WifiScanner.ScanSettings s = entry.getValue();
                int newPeriodInMs = c.addScanRequestToBucket(s);
                if (newPeriodInMs == -1) {
                    Log.d(TAG, "could not find a good bucket");
                    return false;
                }
                if (newPeriodInMs != s.periodInMs) {
                    ci.reportPeriodChanged(id, s, newPeriodInMs);
                }
            }
        }
        c.compressBuckets();
        WifiNative.ScanSettings s2 = c.getComputedSettings();
        if (s2.num_buckets == 0) {
            Log.d(TAG, "Stopping scan because there are no buckets");
            WifiNative.stopScan();
            return true;
        }
        if (WifiNative.startScan(s2, this.mStateMachine)) {
            Log.d(TAG, "Successfully started scan of " + s2.num_buckets + " buckets attime = " + (SystemClock.elapsedRealtimeNanos() / 1000));
            return true;
        }
        Log.d(TAG, "Failed to start scan of " + s2.num_buckets + " buckets");
        return false;
    }

    boolean addScanRequest(ClientInfo ci, int handler, WifiScanner.ScanSettings settings) {
        if (settings.periodInMs < 1000) {
            Log.d(TAG, "Failing scan request because periodInMs is " + settings.periodInMs);
            return false;
        }
        int minSupportedPeriodMs = 0;
        if (settings.channels != null) {
            minSupportedPeriodMs = settings.channels.length * MIN_PERIOD_PER_CHANNEL_MS;
        } else {
            if ((settings.band & 1) == 0) {
                minSupportedPeriodMs = 0 + 1000;
            }
            if ((settings.band & 2) == 0) {
                minSupportedPeriodMs += 1000;
            }
            if ((settings.band & 4) == 0) {
                minSupportedPeriodMs += 2000;
            }
        }
        if (settings.periodInMs < minSupportedPeriodMs) {
            Log.d(TAG, "Failing scan request because minSupportedPeriodMs is " + minSupportedPeriodMs + " but the request wants " + settings.periodInMs);
            return false;
        }
        ci.addScanRequest(settings, handler);
        if (resetBuckets()) {
            return true;
        }
        ci.removeScanRequest(handler);
        Log.d(TAG, "Failing scan request because failed to reset scan");
        return false;
    }

    void removeScanRequest(ClientInfo ci, int handler) {
        ci.removeScanRequest(handler);
        resetBuckets();
    }

    ScanResult[] getScanResults(ClientInfo ci) {
        ScanResult[] results = WifiNative.getScanResults();
        ci.reportScanResults(results);
        return results;
    }

    void resetHotlist() {
        Collection<ClientInfo> clients = this.mClients.values();
        int num_hotlist_ap = 0;
        for (ClientInfo ci : clients) {
            Collection<WifiScanner.HotlistSettings> c = ci.getHotlistSettings();
            Iterator<WifiScanner.HotlistSettings> it = c.iterator();
            while (it.hasNext()) {
                num_hotlist_ap += it.next().bssidInfos.length;
            }
        }
        if (num_hotlist_ap == 0) {
            WifiNative.resetHotlist();
            return;
        }
        WifiScanner.BssidInfo[] bssidInfos = new WifiScanner.BssidInfo[num_hotlist_ap];
        int index = 0;
        for (ClientInfo ci2 : clients) {
            for (WifiScanner.HotlistSettings s : ci2.getHotlistSettings()) {
                int i = 0;
                while (i < s.bssidInfos.length) {
                    bssidInfos[index] = s.bssidInfos[i];
                    i++;
                    index++;
                }
            }
        }
        WifiScanner.HotlistSettings settings = new WifiScanner.HotlistSettings();
        settings.bssidInfos = bssidInfos;
        settings.apLostThreshold = 3;
        WifiNative.setHotlist(settings, this.mStateMachine);
    }

    void setHotlist(ClientInfo ci, int handler, WifiScanner.HotlistSettings settings) {
        ci.addHostlistSettings(settings, handler);
        resetHotlist();
    }

    void resetHotlist(ClientInfo ci, int handler) {
        ci.removeHostlistSettings(handler);
        resetHotlist();
    }

    void trackWifiChanges(ClientInfo ci, int handler) {
        this.mWifiChangeStateMachine.enable();
        ci.addSignificantWifiChange(handler);
    }

    void untrackWifiChanges(ClientInfo ci, int handler) {
        ci.removeSignificantWifiChange(handler);
        Collection<ClientInfo> clients = this.mClients.values();
        for (ClientInfo ci2 : clients) {
            if (ci2.getWifiChangeHandlers().size() != 0) {
                return;
            }
        }
        this.mWifiChangeStateMachine.disable();
    }

    void configureWifiChange(WifiScanner.WifiChangeSettings settings) {
        this.mWifiChangeStateMachine.configure(settings);
    }

    void reportWifiChanged(ScanResult[] results) {
        Collection<ClientInfo> clients = this.mClients.values();
        for (ClientInfo ci : clients) {
            ci.reportWifiChanged(results);
        }
    }

    void reportWifiStabilized(ScanResult[] results) {
        Collection<ClientInfo> clients = this.mClients.values();
        for (ClientInfo ci : clients) {
            ci.reportWifiStabilized(results);
        }
    }

    class WifiChangeStateMachine extends StateMachine implements WifiNative.SignificantWifiChangeEventHandler {
        private static final String ACTION_TIMEOUT = "com.android.server.WifiScanningServiceImpl.action.TIMEOUT";
        private static final int MAX_APS_TO_TRACK = 3;
        private static final int MOVING_SCAN_PERIOD_MS = 10000;
        private static final int MOVING_STATE_TIMEOUT_MS = 30000;
        private static final int SCAN_COMMAND_ID = 1;
        private static final int STATIONARY_SCAN_PERIOD_MS = 5000;
        private static final String TAG = "WifiChangeStateMachine";
        private static final int WIFI_CHANGE_CMD_CHANGE_DETECTED = 1;
        private static final int WIFI_CHANGE_CMD_CHANGE_TIMEOUT = 2;
        private static final int WIFI_CHANGE_CMD_CONFIGURE = 5;
        private static final int WIFI_CHANGE_CMD_DISABLE = 4;
        private static final int WIFI_CHANGE_CMD_ENABLE = 3;
        private static final int WIFI_CHANGE_CMD_NEW_SCAN_RESULTS = 0;
        AlarmManager mAlarmManager;
        ClientInfo mClientInfo;
        ScanResult[] mCurrentBssids;
        State mDefaultState;
        State mMovingState;
        State mStationaryState;
        PendingIntent mTimeoutIntent;

        WifiChangeStateMachine(Looper looper) {
            super("SignificantChangeStateMachine", looper);
            this.mDefaultState = new DefaultState();
            this.mStationaryState = new StationaryState();
            this.mMovingState = new MovingState();
            this.mClientInfo = new ClientInfoLocal();
            WifiScanningServiceImpl.this.mClients.put(null, this.mClientInfo);
            addState(this.mDefaultState);
            addState(this.mStationaryState, this.mDefaultState);
            addState(this.mMovingState, this.mDefaultState);
            setInitialState(this.mDefaultState);
        }

        public void enable() {
            if (this.mAlarmManager == null) {
                this.mAlarmManager = (AlarmManager) WifiScanningServiceImpl.this.mContext.getSystemService("alarm");
            }
            if (this.mTimeoutIntent == null) {
                Intent intent = new Intent(ACTION_TIMEOUT, (Uri) null);
                this.mTimeoutIntent = PendingIntent.getBroadcast(WifiScanningServiceImpl.this.mContext, 0, intent, 0);
                WifiScanningServiceImpl.this.mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent2) {
                        WifiChangeStateMachine.this.sendMessage(2);
                    }
                }, new IntentFilter(ACTION_TIMEOUT));
            }
            sendMessage(3);
        }

        public void disable() {
            sendMessage(4);
        }

        public void configure(WifiScanner.WifiChangeSettings settings) {
            sendMessage(5, settings);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public void enter() {
                Log.d(WifiChangeStateMachine.TAG, "Entering IdleState");
            }

            public boolean processMessage(Message msg) {
                Log.d(WifiChangeStateMachine.TAG, "DefaultState state got " + msg);
                switch (msg.what) {
                    case 0:
                    case 4:
                        return true;
                    case 1:
                    case 2:
                    default:
                        return false;
                    case 3:
                        WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mMovingState);
                        return true;
                    case 5:
                        WifiChangeStateMachine.this.deferMessage(msg);
                        return true;
                }
            }
        }

        class StationaryState extends State {
            StationaryState() {
            }

            public void enter() {
                Log.d(WifiChangeStateMachine.TAG, "Entering StationaryState");
                WifiScanningServiceImpl.this.reportWifiStabilized(WifiChangeStateMachine.this.mCurrentBssids);
            }

            public boolean processMessage(Message msg) {
                Log.d(WifiChangeStateMachine.TAG, "Stationary state got " + msg);
                switch (msg.what) {
                    case 1:
                        Log.d(WifiChangeStateMachine.TAG, "Got wifi change detected");
                        WifiScanningServiceImpl.this.reportWifiChanged((ScanResult[]) msg.obj);
                        WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mMovingState);
                        return true;
                    case 2:
                    default:
                        return false;
                    case 3:
                        return true;
                    case 4:
                        Log.d(WifiChangeStateMachine.TAG, "Got Disable Wifi Change");
                        WifiChangeStateMachine.this.mCurrentBssids = null;
                        WifiChangeStateMachine.this.removeScanRequest();
                        WifiChangeStateMachine.this.untrackSignificantWifiChange();
                        WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mDefaultState);
                        return true;
                    case 5:
                        WifiChangeStateMachine.this.deferMessage(msg);
                        return true;
                }
            }
        }

        class MovingState extends State {
            boolean mWifiChangeDetected = false;
            boolean mScanResultsPending = false;

            MovingState() {
            }

            public void enter() {
                Log.d(WifiChangeStateMachine.TAG, "Entering MovingState");
                issueFullScan();
            }

            public boolean processMessage(Message msg) {
                Log.d(WifiChangeStateMachine.TAG, "MovingState state got " + msg);
                switch (msg.what) {
                    case 0:
                        Log.d(WifiChangeStateMachine.TAG, "Got scan results");
                        if (this.mScanResultsPending) {
                            Log.d(WifiChangeStateMachine.TAG, "reconfiguring scan");
                            WifiChangeStateMachine.this.reconfigureScan((ScanResult[]) msg.obj, WifiChangeStateMachine.STATIONARY_SCAN_PERIOD_MS);
                            this.mWifiChangeDetected = false;
                            WifiChangeStateMachine.this.mAlarmManager.setExact(0, System.currentTimeMillis() + 30000, WifiChangeStateMachine.this.mTimeoutIntent);
                            this.mScanResultsPending = false;
                        }
                        break;
                    case 1:
                        Log.d(WifiChangeStateMachine.TAG, "Change detected");
                        WifiChangeStateMachine.this.mAlarmManager.cancel(WifiChangeStateMachine.this.mTimeoutIntent);
                        WifiScanningServiceImpl.this.reportWifiChanged((ScanResult[]) msg.obj);
                        this.mWifiChangeDetected = true;
                        issueFullScan();
                        break;
                    case 2:
                        Log.d(WifiChangeStateMachine.TAG, "Got timeout event");
                        if (!this.mWifiChangeDetected) {
                            WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mStationaryState);
                        }
                        break;
                    case 3:
                        break;
                    case 4:
                        Log.d(WifiChangeStateMachine.TAG, "Got Disable Wifi Change");
                        WifiChangeStateMachine.this.mCurrentBssids = null;
                        WifiChangeStateMachine.this.removeScanRequest();
                        WifiChangeStateMachine.this.untrackSignificantWifiChange();
                        WifiChangeStateMachine.this.transitionTo(WifiChangeStateMachine.this.mDefaultState);
                        break;
                    case 5:
                        Log.d(WifiChangeStateMachine.TAG, "Got configuration from app");
                        WifiScanner.WifiChangeSettings settings = (WifiScanner.WifiChangeSettings) msg.obj;
                        WifiChangeStateMachine.this.reconfigureScan(settings);
                        this.mWifiChangeDetected = false;
                        long unchangedDelay = settings.unchangedSampleSize * settings.periodInMs;
                        WifiChangeStateMachine.this.mAlarmManager.cancel(WifiChangeStateMachine.this.mTimeoutIntent);
                        WifiChangeStateMachine.this.mAlarmManager.setExact(0, System.currentTimeMillis() + unchangedDelay, WifiChangeStateMachine.this.mTimeoutIntent);
                        break;
                    default:
                        return false;
                }
                return true;
            }

            public void exit() {
                WifiChangeStateMachine.this.mAlarmManager.cancel(WifiChangeStateMachine.this.mTimeoutIntent);
            }

            void issueFullScan() {
                Log.d(WifiChangeStateMachine.TAG, "Issuing full scan");
                WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
                settings.band = 3;
                settings.periodInMs = WifiChangeStateMachine.MOVING_SCAN_PERIOD_MS;
                settings.reportEvents = 1;
                WifiChangeStateMachine.this.addScanRequest(settings);
                this.mScanResultsPending = true;
            }
        }

        void reconfigureScan(ScanResult[] results, int period) {
            if (results.length < 3) {
                Log.d(TAG, "too few APs (" + results.length + ") available to track wifi change");
                return;
            }
            removeScanRequest();
            HashMap<String, ScanResult> bssidToScanResult = new HashMap<>();
            for (ScanResult result : results) {
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
            Log.d(TAG, "Found " + channels.size() + " channels");
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
                Log.d(TAG, "Setting bssid=" + BssidInfo.bssid + ", low=" + BssidInfo.low + ", high=" + BssidInfo.high);
            }
            trackSignificantWifiChange(settings2);
            this.mCurrentBssids = brightest;
        }

        void reconfigureScan(WifiScanner.WifiChangeSettings settings) {
            if (settings.bssidInfos.length < 3) {
                Log.d(TAG, "too few APs (" + settings.bssidInfos.length + ") available to track wifi change");
                return;
            }
            Log.d(TAG, "Setting configuration specified by app");
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

        class ClientInfoLocal extends ClientInfo {
            ClientInfoLocal() {
                super(null, null);
            }

            @Override
            void deliverScanResults(int handler, ScanResult[] results) {
                Log.d(WifiChangeStateMachine.TAG, "Delivering messages directly");
                WifiChangeStateMachine.this.sendMessage(0, 0, 0, results);
            }

            @Override
            void reportPeriodChanged(int handler, WifiScanner.ScanSettings settings, int newPeriodInMs) {
            }
        }

        @Override
        public void onChangesFound(ScanResult[] results) {
            sendMessage(1, 0, 0, results);
        }

        void addScanRequest(WifiScanner.ScanSettings settings) {
            Log.d(TAG, "Starting scans");
            Message msg = Message.obtain();
            msg.what = 159746;
            msg.arg2 = 1;
            msg.obj = settings;
            WifiScanningServiceImpl.this.mClientHandler.sendMessage(msg);
        }

        void removeScanRequest() {
            Log.d(TAG, "Stopping scans");
            Message msg = Message.obtain();
            msg.what = 159747;
            msg.arg2 = 1;
            WifiScanningServiceImpl.this.mClientHandler.sendMessage(msg);
        }

        void trackSignificantWifiChange(WifiScanner.WifiChangeSettings settings) {
            WifiNative.untrackSignificantWifiChange();
            WifiNative.trackSignificantWifiChange(settings, this);
        }

        void untrackSignificantWifiChange() {
            WifiNative.untrackSignificantWifiChange();
        }
    }

    private static WifiScanner.ChannelSpec[] getChannelsForBand(int band) {
        int[] channels = WifiNative.getChannelsForBand(band);
        if (channels != null) {
            WifiScanner.ChannelSpec[] channelSpecs = new WifiScanner.ChannelSpec[channels.length];
            for (int i = 0; i < channels.length; i++) {
                channelSpecs[i] = new WifiScanner.ChannelSpec(channels[i]);
            }
            return channelSpecs;
        }
        return new WifiScanner.ChannelSpec[0];
    }

    private static int getBandFromChannels(WifiScanner.ChannelSpec[] channels) {
        int band = 0;
        for (WifiScanner.ChannelSpec channel : channels) {
            if (2400 <= channel.frequency && channel.frequency < 2500) {
                band |= 1;
            } else if (5100 <= channel.frequency && channel.frequency < 6000) {
                band |= 2;
            }
        }
        return band;
    }

    private static int getBandFromChannels(WifiNative.ChannelSettings[] channels) {
        int band = 0;
        for (WifiNative.ChannelSettings channel : channels) {
            if (2400 <= channel.frequency && channel.frequency < 2500) {
                band |= 1;
            } else if (5100 <= channel.frequency && channel.frequency < 6000) {
                band |= 2;
            }
        }
        return band;
    }
}
