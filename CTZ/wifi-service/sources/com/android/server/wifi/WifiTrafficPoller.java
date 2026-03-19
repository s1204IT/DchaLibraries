package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiTrafficPoller {
    private static final int ADD_CLIENT = 3;
    private static final boolean DBG = false;
    private static final int ENABLE_TRAFFIC_STATS_POLL = 1;
    private static final int POLL_TRAFFIC_STATS_INTERVAL_MSECS = 1000;
    private static final int REMOVE_CLIENT = 4;
    private static final String TAG = "WifiTrafficPoller";
    private static final int TRAFFIC_STATS_POLL = 2;
    private int mDataActivity;
    private NetworkInfo mNetworkInfo;
    private long mRxPkts;
    private final TrafficHandler mTrafficHandler;
    private long mTxPkts;
    private final WifiNative mWifiNative;
    private boolean mEnableTrafficStatsPoll = DBG;
    private int mTrafficStatsPollToken = 0;
    private final List<Messenger> mClients = new ArrayList();
    private AtomicBoolean mScreenOn = new AtomicBoolean(true);
    private boolean mVerboseLoggingEnabled = DBG;

    static int access$508(WifiTrafficPoller wifiTrafficPoller) {
        int i = wifiTrafficPoller.mTrafficStatsPollToken;
        wifiTrafficPoller.mTrafficStatsPollToken = i + 1;
        return i;
    }

    WifiTrafficPoller(Context context, Looper looper, WifiNative wifiNative) {
        this.mTrafficHandler = new TrafficHandler(looper);
        this.mWifiNative = wifiNative;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent == null) {
                    return;
                }
                if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    WifiTrafficPoller.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                } else if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                    WifiTrafficPoller.this.mScreenOn.set(WifiTrafficPoller.DBG);
                } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                    WifiTrafficPoller.this.mScreenOn.set(true);
                }
                WifiTrafficPoller.this.evaluateTrafficStatsPolling();
            }
        }, intentFilter);
    }

    public void addClient(Messenger messenger) {
        Message.obtain(this.mTrafficHandler, 3, messenger).sendToTarget();
    }

    public void removeClient(Messenger messenger) {
        Message.obtain(this.mTrafficHandler, 4, messenger).sendToTarget();
    }

    void enableVerboseLogging(int i) {
        if (i > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = DBG;
        }
    }

    private class TrafficHandler extends Handler {
        public TrafficHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    WifiTrafficPoller.this.mEnableTrafficStatsPoll = message.arg1 == 1;
                    if (WifiTrafficPoller.this.mVerboseLoggingEnabled) {
                        Log.d(WifiTrafficPoller.TAG, "ENABLE_TRAFFIC_STATS_POLL " + WifiTrafficPoller.this.mEnableTrafficStatsPoll + " Token " + Integer.toString(WifiTrafficPoller.this.mTrafficStatsPollToken));
                    }
                    WifiTrafficPoller.access$508(WifiTrafficPoller.this);
                    String clientInterfaceName = WifiTrafficPoller.this.mWifiNative.getClientInterfaceName();
                    if (WifiTrafficPoller.this.mEnableTrafficStatsPoll && !TextUtils.isEmpty(clientInterfaceName)) {
                        WifiTrafficPoller.this.notifyOnDataActivity(clientInterfaceName);
                        sendMessageDelayed(Message.obtain(this, 2, WifiTrafficPoller.this.mTrafficStatsPollToken, 0), 1000L);
                        break;
                    }
                    break;
                case 2:
                    if (message.arg1 == WifiTrafficPoller.this.mTrafficStatsPollToken) {
                        String clientInterfaceName2 = WifiTrafficPoller.this.mWifiNative.getClientInterfaceName();
                        if (!TextUtils.isEmpty(clientInterfaceName2)) {
                            WifiTrafficPoller.this.notifyOnDataActivity(clientInterfaceName2);
                            sendMessageDelayed(Message.obtain(this, 2, WifiTrafficPoller.this.mTrafficStatsPollToken, 0), 1000L);
                        }
                    }
                    break;
                case 3:
                    WifiTrafficPoller.this.mClients.add((Messenger) message.obj);
                    if (WifiTrafficPoller.this.mVerboseLoggingEnabled) {
                        Log.d(WifiTrafficPoller.TAG, "ADD_CLIENT: " + Integer.toString(WifiTrafficPoller.this.mClients.size()));
                    }
                    break;
                case 4:
                    WifiTrafficPoller.this.mClients.remove(message.obj);
                    break;
            }
        }
    }

    private void evaluateTrafficStatsPolling() {
        Message messageObtain;
        if (this.mNetworkInfo == null) {
            return;
        }
        if (this.mNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED && this.mScreenOn.get()) {
            messageObtain = Message.obtain(this.mTrafficHandler, 1, 1, 0);
        } else {
            messageObtain = Message.obtain(this.mTrafficHandler, 1, 0, 0);
        }
        messageObtain.sendToTarget();
    }

    private void notifyOnDataActivity(String str) {
        int i;
        long j = this.mTxPkts;
        long j2 = this.mRxPkts;
        this.mTxPkts = this.mWifiNative.getTxPackets(str);
        this.mRxPkts = this.mWifiNative.getRxPackets(str);
        if (j > 0 || j2 > 0) {
            long j3 = this.mTxPkts - j;
            long j4 = this.mRxPkts - j2;
            if (j3 > 0) {
                i = 2;
            } else {
                i = 0;
            }
            if (j4 > 0) {
                i |= 1;
            }
            if (i != this.mDataActivity && this.mScreenOn.get()) {
                this.mDataActivity = i;
                if (this.mVerboseLoggingEnabled) {
                    Log.e(TAG, "notifying of data activity " + Integer.toString(this.mDataActivity));
                }
                for (Messenger messenger : this.mClients) {
                    Message messageObtain = Message.obtain();
                    messageObtain.what = 1;
                    messageObtain.arg1 = this.mDataActivity;
                    try {
                        messenger.send(messageObtain);
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("mEnableTrafficStatsPoll " + this.mEnableTrafficStatsPoll);
        printWriter.println("mTrafficStatsPollToken " + this.mTrafficStatsPollToken);
        printWriter.println("mTxPkts " + this.mTxPkts);
        printWriter.println("mRxPkts " + this.mRxPkts);
        printWriter.println("mDataActivity " + this.mDataActivity);
    }
}
