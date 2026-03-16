package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

final class WifiTrafficPoller {
    private static final int ADD_CLIENT = 3;
    private static final int BOOST_LOG_THRESHOLD = 524288;
    private static final boolean DBG_BOOST = true;
    private static final boolean ENABLE_FEATURE_WIFI_BOOST = true;
    private static final int ENABLE_TRAFFIC_STATS_POLL = 1;
    private static final int POLL_TRAFFIC_STATS_INTERVAL_MSECS = 1000;
    private static final int REMOVE_CLIENT = 4;
    private static final int TRAFFIC_STATS_POLL = 2;
    private static final int WIFI_BOOST_THRESHOLD = 2097152;
    private Context mContext;
    private int mDataActivity;
    private final String mInterface;
    private NetworkInfo mNetworkInfo;
    private long mRxBytes;
    private long mRxPkts;
    private long mTpThreshold;
    private long mTxBytes;
    private long mTxPkts;
    private WifiServiceImpl mWifiServiceImpl;
    private boolean DBG = false;
    private boolean VDBG = false;
    private final String TAG = "WifiTrafficPoller";
    private boolean mEnableTrafficStatsPoll = false;
    private int mTrafficStatsPollToken = 0;
    private final List<Messenger> mClients = new ArrayList();
    private AtomicBoolean mScreenOn = new AtomicBoolean(true);
    private boolean mLastWifiBoostFlag = false;
    private final TrafficHandler mTrafficHandler = new TrafficHandler();

    static int access$708(WifiTrafficPoller x0) {
        int i = x0.mTrafficStatsPollToken;
        x0.mTrafficStatsPollToken = i + 1;
        return i;
    }

    WifiTrafficPoller(Context context, WifiServiceImpl service, String iface) {
        this.mInterface = iface;
        this.mContext = context;
        this.mWifiServiceImpl = service;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        Log.d("WifiTrafficPoller", "enable wifi boost");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        this.mContext = context;
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                    WifiTrafficPoller.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                } else if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                    WifiTrafficPoller.this.mScreenOn.set(false);
                } else if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                    WifiTrafficPoller.this.mScreenOn.set(true);
                } else if (intent.getAction().equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                    Log.v("WifiTrafficPoller", "got WIFI_AP_STATE_CHANGED_ACTION, state = " + WifiTrafficPoller.this.mWifiServiceImpl.getWifiApEnabledState());
                }
                WifiTrafficPoller.this.evaluateTrafficStatsPolling();
            }
        }, filter);
    }

    void addClient(Messenger client) {
        Message.obtain(this.mTrafficHandler, 3, client).sendToTarget();
    }

    void removeClient(Messenger client) {
        Message.obtain(this.mTrafficHandler, 4, client).sendToTarget();
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.DBG = true;
        } else {
            this.DBG = false;
        }
    }

    private class TrafficHandler extends Handler {
        private TrafficHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WifiTrafficPoller.this.mEnableTrafficStatsPoll = msg.arg1 == 1;
                    if (WifiTrafficPoller.this.DBG) {
                        Log.e("WifiTrafficPoller", "ENABLE_TRAFFIC_STATS_POLL " + WifiTrafficPoller.this.mEnableTrafficStatsPoll + " Token " + Integer.toString(WifiTrafficPoller.this.mTrafficStatsPollToken));
                    }
                    WifiTrafficPoller.access$708(WifiTrafficPoller.this);
                    if (WifiTrafficPoller.this.mEnableTrafficStatsPoll) {
                        WifiTrafficPoller.this.notifyOnDataActivity();
                        WifiTrafficPoller.this.checkWifiBoost();
                        sendMessageDelayed(Message.obtain(this, 2, WifiTrafficPoller.this.mTrafficStatsPollToken, 0), 1000L);
                    }
                    break;
                case 2:
                    if (WifiTrafficPoller.this.VDBG) {
                        Log.e("WifiTrafficPoller", "TRAFFIC_STATS_POLL " + WifiTrafficPoller.this.mEnableTrafficStatsPoll + " Token " + Integer.toString(WifiTrafficPoller.this.mTrafficStatsPollToken) + " num clients " + WifiTrafficPoller.this.mClients.size());
                    }
                    if (msg.arg1 == WifiTrafficPoller.this.mTrafficStatsPollToken) {
                        WifiTrafficPoller.this.notifyOnDataActivity();
                        WifiTrafficPoller.this.checkWifiBoost();
                        sendMessageDelayed(Message.obtain(this, 2, WifiTrafficPoller.this.mTrafficStatsPollToken, 0), 1000L);
                    }
                    break;
                case 3:
                    WifiTrafficPoller.this.mClients.add((Messenger) msg.obj);
                    if (WifiTrafficPoller.this.DBG) {
                        Log.e("WifiTrafficPoller", "ADD_CLIENT: " + Integer.toString(WifiTrafficPoller.this.mClients.size()));
                    }
                    break;
                case 4:
                    WifiTrafficPoller.this.mClients.remove(msg.obj);
                    break;
            }
        }
    }

    private void evaluateTrafficStatsPolling() {
        Message msg;
        if (this.mWifiServiceImpl.getWifiApEnabledState() == 13) {
            Log.d("WifiTrafficPoller", "evaluateWifiBoostPolling, AP State is Enabled");
            Message msg2 = Message.obtain(this.mTrafficHandler, 1, 1, 0);
            msg2.sendToTarget();
        }
        if (this.mNetworkInfo != null) {
            if (this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && this.mScreenOn.get()) {
                msg = Message.obtain(this.mTrafficHandler, 1, 1, 0);
            } else {
                msg = Message.obtain(this.mTrafficHandler, 1, 0, 0);
            }
            msg.sendToTarget();
        }
    }

    private void checkWifiBoost() {
        int apEnabledState = this.mWifiServiceImpl.getWifiApEnabledState();
        if (apEnabledState == 13) {
            long preTxBytes = this.mTxBytes;
            long preRxBytes = this.mRxBytes;
            this.mTxBytes = TrafficStats.getTxBytes(this.mInterface);
            this.mRxBytes = TrafficStats.getRxBytes(this.mInterface);
            if (preTxBytes > 0 || preRxBytes > 0) {
                long sentBytes = this.mTxBytes - preTxBytes;
                long receivedBytes = this.mRxBytes - preRxBytes;
                this.mTpThreshold = Integer.parseInt(System.getProperty("persist.radio.wifi.boot.threshold", "0"));
                this.mTpThreshold = this.mTpThreshold > 2097152 ? this.mTpThreshold : 2097152L;
                if (sentBytes > this.mTpThreshold || receivedBytes > this.mTpThreshold) {
                    Log.v("WifiTrafficPoller", "enable wifi boost, sentBytes=" + sentBytes + ", receivedBytes=" + receivedBytes);
                    this.mLastWifiBoostFlag = true;
                    return;
                }
                if (this.mLastWifiBoostFlag || sentBytes > 524288 || receivedBytes > 524288) {
                    Log.v("WifiTrafficPoller", "disable wifi boost, sentBytes=" + sentBytes + ", receivedBytes=" + receivedBytes);
                }
                this.mLastWifiBoostFlag = false;
            }
        }
    }

    private void notifyOnDataActivity() {
        long preTxPkts = this.mTxPkts;
        long preRxPkts = this.mRxPkts;
        int dataActivity = 0;
        this.mTxPkts = TrafficStats.getTxPackets(this.mInterface);
        this.mRxPkts = TrafficStats.getRxPackets(this.mInterface);
        if (this.VDBG) {
            Log.e("WifiTrafficPoller", " packet count Tx=" + Long.toString(this.mTxPkts) + " Rx=" + Long.toString(this.mRxPkts));
        }
        if (preTxPkts > 0 || preRxPkts > 0) {
            long sent = this.mTxPkts - preTxPkts;
            long received = this.mRxPkts - preRxPkts;
            if (sent > 0) {
                dataActivity = 0 | 2;
            }
            if (received > 0) {
                dataActivity |= 1;
            }
            if (dataActivity != this.mDataActivity && this.mScreenOn.get()) {
                this.mDataActivity = dataActivity;
                if (this.DBG) {
                    Log.e("WifiTrafficPoller", "notifying of data activity " + Integer.toString(this.mDataActivity));
                }
                for (Messenger client : this.mClients) {
                    Message msg = Message.obtain();
                    msg.what = 1;
                    msg.arg1 = this.mDataActivity;
                    try {
                        client.send(msg);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("mEnableTrafficStatsPoll " + this.mEnableTrafficStatsPoll);
        pw.println("mTrafficStatsPollToken " + this.mTrafficStatsPollToken);
        pw.println("mTxPkts " + this.mTxPkts);
        pw.println("mRxPkts " + this.mRxPkts);
        pw.println("mDataActivity " + this.mDataActivity);
    }
}
