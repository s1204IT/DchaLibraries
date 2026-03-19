package com.mediatek.net.tethering;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.LinkProperties;
import android.net.NetworkState;
import android.net.RouteInfo;
import android.net.util.InterfaceSet;
import android.net.wifi.WifiManager;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.connectivity.Tethering;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mediatek.net.wifi.HotspotClient;
import mediatek.net.wifi.WifiHotspotManager;
import vendor.mediatek.hardware.netdagent.V1_0.INetdagent;

public class MtkTethering {
    private static final boolean DBG = false;
    static final int EVENT_BOOTUP = 3;
    static final int EVENT_HOTSPOT_STATUS = 1;
    static final int EVENT_LOCALE_CHANGED = 2;
    static final int EVENT_TETHER_STATUS = 0;
    private static final String TAG = "MtkTethering";
    private static Tethering sTethering;
    private final Context mContext;
    private final InternalHandler mHandler;
    protected final HandlerThread mHandlerThread;
    private int mLastNotificationId;
    private final BroadcastReceiver mStateReceiver;
    private Notification.Builder mTetheredNotificationBuilder;
    private String[] mWifiRegexs;

    public MtkTethering(Context context, Tethering tethering) {
        this.mContext = context;
        sTethering = tethering;
        this.mHandlerThread = new HandlerThread("TetheringInternalHandler");
        this.mHandlerThread.start();
        this.mHandler = new InternalHandler(this.mHandlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.TETHER_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        this.mStateReceiver = new StateReceiver();
        this.mContext.registerReceiver(this.mStateReceiver, intentFilter, null, this.mHandler);
        this.mHandler.sendEmptyMessage(EVENT_BOOTUP);
    }

    private class InternalHandler extends Handler {
        public InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    MtkTethering.this.showTetheredNotification(((Boolean) message.obj).booleanValue());
                    break;
                case 1:
                    MtkTethering.this.updateTetheredNotification();
                    break;
                case 2:
                    MtkTethering.this.updateAospNotificatin();
                    MtkTethering.this.updateTetheredNotification();
                    break;
                case MtkTethering.EVENT_BOOTUP:
                    MtkTethering.this.checkEmSetting();
                    break;
            }
        }
    }

    private void checkEmSetting() {
        if (SystemProperties.getBoolean("persist.vendor.radio.bgdata.disabled", DBG)) {
            try {
                INetdagent service = INetdagent.getService();
                if (service == null) {
                    Log.e(TAG, "netagent is null");
                } else {
                    Log.d(TAG, "setIotFirewall");
                    service.dispatchNetdagentCmd("netdagent firewall set_nsiot_firewall");
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception:" + e);
            }
        }
    }

    private void updateAospNotificatin() {
        try {
            Method declaredMethod = sTethering.getClass().getDeclaredMethod("clearTetheredNotification", (Class[]) null);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(sTethering, new Object[0]);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        try {
            Method declaredMethod2 = sTethering.getClass().getDeclaredMethod("sendTetherStateChangedBroadcast", (Class[]) null);
            declaredMethod2.setAccessible(true);
            declaredMethod2.invoke(sTethering, new Object[0]);
        } catch (ReflectiveOperationException e2) {
            e2.printStackTrace();
        }
    }

    private class StateReceiver extends BroadcastReceiver {
        private StateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            Log.i(MtkTethering.TAG, "Intent:" + intent);
            if (action.equals("android.net.conn.TETHER_STATE_CHANGED")) {
                MtkTethering.this.handleTetherStatus(intent);
            } else if (action.equals("android.intent.action.LOCALE_CHANGED")) {
                MtkTethering.this.mHandler.sendEmptyMessage(2);
            } else if (action.equals("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED")) {
                MtkTethering.this.mHandler.sendEmptyMessage(1);
            }
        }
    }

    private void handleTetherStatus(Intent intent) {
        boolean z;
        String[] tetherableWifiRegexs = sTethering.getTetherableWifiRegexs();
        ArrayList<String> stringArrayListExtra = intent.getStringArrayListExtra("tetherArray");
        if (stringArrayListExtra != null) {
            z = false;
            for (String str : stringArrayListExtra) {
                boolean z2 = z;
                for (String str2 : tetherableWifiRegexs) {
                    if (str.matches(str2)) {
                        z2 = true;
                    }
                }
                z = z2;
            }
        } else {
            z = false;
        }
        Message.obtain(this.mHandler, 0, 0, 0, Boolean.valueOf(z)).sendToTarget();
    }

    private void showTetheredNotification(boolean z) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager == null) {
            return;
        }
        Log.i(TAG, "showTetheredNotification:" + z + ":" + this.mLastNotificationId);
        if (!z && this.mLastNotificationId != 0) {
            notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
            this.mLastNotificationId = 0;
            Log.i(TAG, "Disable notification");
            return;
        }
        if (!z) {
            Log.i(TAG, "Ignore");
            return;
        }
        if (this.mLastNotificationId != 0) {
            if (this.mLastNotificationId == 17303513) {
                return;
            }
            notificationManager.cancelAsUser(null, this.mLastNotificationId, UserHandle.ALL);
            this.mLastNotificationId = 0;
        }
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setFlags(1073741824);
        PendingIntent activityAsUser = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
        if (BenesseExtension.getDchaState() != 0) {
            activityAsUser = null;
        }
        Resources system = Resources.getSystem();
        CharSequence text = system.getText(R.string.mediasize_na_quarto);
        String hotspotClientInfo = getHotspotClientInfo(system);
        if (this.mTetheredNotificationBuilder == null) {
            this.mTetheredNotificationBuilder = new Notification.Builder(this.mContext, SystemNotificationChannels.NETWORK_STATUS);
            this.mTetheredNotificationBuilder.setWhen(0L).setOngoing(true).setColor(this.mContext.getColor(R.color.car_colorPrimary)).setVisibility(1).setCategory("status");
        }
        this.mTetheredNotificationBuilder.setSmallIcon(R.drawable.pointer_vertical_text_large).setContentTitle(text).setContentText(hotspotClientInfo).setContentIntent(activityAsUser);
        this.mLastNotificationId = 14;
        notificationManager.notifyAsUser(null, this.mLastNotificationId, this.mTetheredNotificationBuilder.build(), UserHandle.ALL);
    }

    private void updateTetheredNotification() {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        if (notificationManager == null || this.mTetheredNotificationBuilder == null || this.mLastNotificationId == 0) {
            return;
        }
        Resources system = Resources.getSystem();
        CharSequence text = system.getText(R.string.mediasize_na_quarto);
        this.mTetheredNotificationBuilder.setContentTitle(text).setContentText(getHotspotClientInfo(system));
        notificationManager.notifyAsUser(null, this.mLastNotificationId, this.mTetheredNotificationBuilder.build(), UserHandle.ALL);
    }

    private String getHotspotClientInfo(Resources resources) {
        String string;
        int size;
        int i;
        try {
            string = resources.getString(134545594, 0, 0);
        } catch (Exception e) {
            e = e;
            string = "";
        }
        try {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            if (wifiManager == null) {
                return string;
            }
            WifiHotspotManager wifiHotspotManager = wifiManager.getWifiHotspotManager();
            if (wifiManager == null) {
                return string;
            }
            List hotspotClients = wifiHotspotManager.getHotspotClients();
            if (hotspotClients != null) {
                Iterator it = hotspotClients.iterator();
                i = 0;
                while (it.hasNext()) {
                    if (((HotspotClient) it.next()).isBlocked) {
                        i++;
                    }
                }
                size = hotspotClients.size() - i;
            } else {
                size = 0;
                i = 0;
            }
            Log.i(TAG, "getHotspotClientInfo:" + size + ":" + i);
            return resources.getString(134545594, Integer.valueOf(size), Integer.valueOf(i));
        } catch (Exception e2) {
            e = e2;
            e.printStackTrace();
            return string;
        }
    }

    public InterfaceSet getTetheringInterfaces(NetworkState networkState) {
        if (networkState == null) {
            return null;
        }
        String interfaceForDestination = getInterfaceForDestination(networkState.linkProperties, Inet4Address.ANY);
        String iPv6Interface = getIPv6Interface(networkState);
        Log.d(TAG, "getTetheringInterfaces if4: " + interfaceForDestination + " if6: " + iPv6Interface);
        if (interfaceForDestination == null && iPv6Interface == null) {
            return null;
        }
        return new InterfaceSet(new String[]{interfaceForDestination, iPv6Interface});
    }

    public String getIPv6Interface(NetworkState networkState) {
        boolean z = DBG;
        if (networkState != null && networkState.network != null && networkState.linkProperties != null && networkState.networkCapabilities != null && hasIPv6GlobalAddress(networkState.linkProperties) && networkState.networkCapabilities.hasTransport(0)) {
            z = true;
        }
        if (z) {
            return getInterfaceForDestination(networkState.linkProperties, Inet6Address.ANY);
        }
        return null;
    }

    private String getInterfaceForDestination(LinkProperties linkProperties, InetAddress inetAddress) {
        RouteInfo routeInfoSelectBestRoute;
        if (linkProperties != null) {
            routeInfoSelectBestRoute = RouteInfo.selectBestRoute(linkProperties.getAllRoutes(), inetAddress);
        } else {
            routeInfoSelectBestRoute = null;
        }
        if (routeInfoSelectBestRoute != null) {
            return routeInfoSelectBestRoute.getInterface();
        }
        return null;
    }

    private boolean hasIPv6GlobalAddress(LinkProperties linkProperties) {
        for (InetAddress inetAddress : linkProperties.getAllAddresses()) {
            if ((inetAddress instanceof Inet6Address) && !inetAddress.isAnyLocalAddress() && !inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && !inetAddress.isSiteLocalAddress() && !inetAddress.isMulticastAddress()) {
                return true;
            }
        }
        return DBG;
    }
}
