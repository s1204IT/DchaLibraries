package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.android.internal.telephony.CommandsInterface;

public class NetworkManager extends Handler {
    protected static final int EVENT_GET_AVAILABLE_NW = 1;
    static final String LOG_TAG = "GSM";
    private static NetworkManager sNetworkManager;
    private CommandsInterface[] mCi;
    private Context mContext;
    private int mPhoneCount;

    public static NetworkManager init(Context context, int phoneCount, CommandsInterface[] ci) {
        NetworkManager networkManager;
        synchronized (NetworkManager.class) {
            if (sNetworkManager == null) {
                sNetworkManager = new NetworkManager(context, phoneCount, ci);
            }
            networkManager = sNetworkManager;
        }
        return networkManager;
    }

    public static NetworkManager getInstance() {
        return sNetworkManager;
    }

    private NetworkManager(Context context, int phoneCount, CommandsInterface[] ci) {
        log("Initialize NetworkManager under airplane mode phoneCount= " + phoneCount);
        this.mContext = context;
        this.mCi = ci;
        this.mPhoneCount = phoneCount;
    }

    public void getAvailableNetworks(long subId, Message response) {
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                synchronized (this) {
                    break;
                }
                break;
            default:
                log("Unhandled message with number: " + msg.what);
                break;
        }
    }

    private static void log(String s) {
        Log.d(LOG_TAG, "[NetworkManager] " + s);
    }
}
