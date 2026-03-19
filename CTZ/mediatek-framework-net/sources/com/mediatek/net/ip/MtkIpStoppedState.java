package com.mediatek.net.ip;

import android.content.Context;
import android.net.LinkProperties;
import android.net.ip.IpClient;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.State;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MtkIpStoppedState extends State {
    private static final int CMD_CONFIRM = 3;
    private static final int CMD_START = 2;
    private static final int CMD_STOP = 1;
    private static final boolean DBG = false;
    private static final String TAG = "MtkIpStoppedState";
    private static final boolean VDBG = false;
    private static final String WLAN_INTERFACE = "wlan0";
    private final Context mContext;
    private final String mIfaceName;
    private final IpClient mIpClient;
    private State mIpStoppedState;
    private boolean mIsWifiSMStarted = false;

    public MtkIpStoppedState(Context context, IpClient ipClient, String str, State state) {
        Log.d(TAG, "Initialize MtkIpStoppedState");
        this.mContext = context;
        this.mIpClient = ipClient;
        this.mIfaceName = str;
        this.mIpStoppedState = state;
    }

    public void enter() {
        Log.d(TAG, "enter");
        this.mIpStoppedState.enter();
        if (this.mIsWifiSMStarted) {
            Log.d(TAG, "resetLinkProperties");
            updateLinkPropertiesChange();
        }
    }

    public void exit() {
        Log.d(TAG, "exit");
        this.mIpStoppedState.exit();
    }

    public boolean processMessage(Message message) {
        if (message.what == 2) {
            this.mIsWifiSMStarted = true;
        }
        return this.mIpStoppedState.processMessage(message);
    }

    private void updateLinkPropertiesChange() {
        try {
            Field declaredField = this.mIpClient.getClass().getDeclaredField("mCallback");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(this.mIpClient);
            Field declaredField2 = this.mIpClient.getClass().getDeclaredField("mLinkProperties");
            declaredField2.setAccessible(true);
            LinkProperties linkProperties = (LinkProperties) declaredField2.get(this.mIpClient);
            Method declaredMethod = obj.getClass().getDeclaredMethod("onLinkPropertiesChange", LinkProperties.class);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(obj, linkProperties);
            Log.d(TAG, "updateLinkPropertiesChange for static IP");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
