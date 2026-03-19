package com.mediatek.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiMonitor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;

public class MtkWifiApmDelegate {
    public static final boolean MDMI_SUPPORT;
    private static final String TAG = "MtkWifiApmDelegate";
    private static MtkWifiApmDelegate sApmDelegate = null;
    private Calendar mLastStartScanTime;
    private String mLastSsid = null;
    private final String mInterfaceName = WifiInjector.getInstance().getWifiNative().getClientInterfaceName();
    private Handler mHandler = new Handler(WifiInjector.getInstance().getWifiStateMachineHandlerThread().getLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            Log.d(MtkWifiApmDelegate.TAG, "handleMessage --> " + MtkWifiApmDelegate.this.msgToString(message));
            int i = message.what;
            if (i == 147460) {
                MtkWifiApmDelegate.this.broadcastNetworkDisconnect((String) message.obj, message.arg2);
                return true;
            }
            if (i == 147499) {
                MtkWifiApmDelegate.this.broadcastAssociationReject((String) message.obj);
                return true;
            }
            return true;
        }
    });

    static {
        MDMI_SUPPORT = SystemProperties.getInt("persist.vendor.mdmi_support", 0) == 1;
    }

    private MtkWifiApmDelegate() {
    }

    public void init() {
        if (MDMI_SUPPORT) {
            registerMessage(WifiMonitor.NETWORK_DISCONNECTION_EVENT);
            registerMessage(WifiMonitor.ASSOCIATION_REJECTION_EVENT);
        }
    }

    private String msgToString(Message message) {
        int i = message.what;
        if (i == 147460) {
            return "NETWORK_DISCONNECTION_EVENT";
        }
        if (i != 147499) {
            return "";
        }
        return "ASSOCIATION_REJECTION_EVENT";
    }

    private void registerMessage(int i) {
        WifiInjector.getInstance().getWifiMonitor().registerHandler(this.mInterfaceName, i, this.mHandler);
    }

    public static MtkWifiApmDelegate getInstance() {
        if (sApmDelegate == null) {
            synchronized (TAG) {
                sApmDelegate = new MtkWifiApmDelegate();
            }
            Log.d(TAG, "MDMI suuport: " + MDMI_SUPPORT);
        }
        return sApmDelegate;
    }

    public void notifyStartScanTime() {
        if (MDMI_SUPPORT) {
            this.mLastStartScanTime = Calendar.getInstance();
        }
    }

    public void fillExtraInfo(Intent intent) {
        if (MDMI_SUPPORT) {
            Bundle bundle = new Bundle();
            bundle.putSerializable("scan_start", this.mLastStartScanTime);
            intent.putExtras(bundle);
            intent.putIntegerArrayListExtra("scan_channels", getScanChannels());
        }
    }

    public ArrayList<Integer> getScanChannels() {
        int[] channelsForBand = WifiInjector.getInstance().getWifiNative().getChannelsForBand(1);
        if (channelsForBand == null) {
            channelsForBand = new int[0];
        }
        int[] channelsForBand2 = WifiInjector.getInstance().getWifiNative().getChannelsForBand(2);
        if (channelsForBand2 == null) {
            channelsForBand2 = new int[0];
        }
        int[] channelsForBand3 = WifiInjector.getInstance().getWifiNative().getChannelsForBand(4);
        if (channelsForBand3 == null) {
            channelsForBand3 = new int[0];
        }
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i : channelsForBand) {
            if (!arrayList.contains(Integer.valueOf(i))) {
                arrayList.add(Integer.valueOf(i));
            }
        }
        for (int i2 : channelsForBand2) {
            if (!arrayList.contains(Integer.valueOf(i2))) {
                arrayList.add(Integer.valueOf(i2));
            }
        }
        for (int i3 : channelsForBand3) {
            if (!arrayList.contains(Integer.valueOf(i3))) {
                arrayList.add(Integer.valueOf(i3));
            }
        }
        return arrayList;
    }

    private static Context getContext() {
        WifiInjector wifiInjector = WifiInjector.getInstance();
        try {
            Field declaredField = wifiInjector.getClass().getDeclaredField("mContext");
            declaredField.setAccessible(true);
            return (Context) declaredField.get(wifiInjector);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void broadcastAssociationReject(String str) {
        if (MDMI_SUPPORT) {
            Intent intent = new Intent("mediatek.intent.action.WIFI_ASSOCIATION_REJECT");
            intent.putExtra("bssid", str);
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void broadcastNetworkDisconnect(String str, int i) {
        if (MDMI_SUPPORT) {
            Intent intent = new Intent("mediatek.intent.action.WIFI_NETWORK_DISCONNECT");
            intent.putExtra("bssid", str);
            intent.putExtra("reason", i);
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public void broadcastPowerSaveModeChanged(boolean z) {
        if (MDMI_SUPPORT) {
            Intent intent = new Intent("mediatek.intent.action.WIFI_PS_CHANGED");
            intent.putExtra("ps_mode", z);
            getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    public void broadcastProvisionFail() {
        if (MDMI_SUPPORT) {
            getContext().sendBroadcastAsUser(new Intent("mediatek.intent.action.WIFI_PROVISION_FAIL"), UserHandle.ALL);
        }
    }
}
