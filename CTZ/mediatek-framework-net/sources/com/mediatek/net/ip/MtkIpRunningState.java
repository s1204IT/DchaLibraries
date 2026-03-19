package com.mediatek.net.ip;

import android.content.Context;
import android.net.StaticIpConfiguration;
import android.net.ip.IpClient;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.util.State;
import com.android.server.net.NetlinkTracker;
import com.mediatek.net.dhcp.MtkDhcp6Client;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

public class MtkIpRunningState extends State {
    private static final boolean DBG = false;
    private static final int EVENT_PRE_DHCP_ACTION_COMPLETE = 4;
    private static final String TAG = "MtkIpRunningState";
    private static final boolean VDBG = false;
    private static final String WLAN_INTERFACE = "wlan0";
    private static final boolean sMtkDhcpv6cWifi = SystemProperties.get("ro.vendor.mtk_dhcpv6c_wifi").equals("1");
    private final Context mContext;
    private MtkDhcp6Client mDhcp6Client;
    private ArrayList<InetAddress> mDnsV6Servers;
    private final String mIfaceName;
    private final IpClient mIpClient;
    private State mIpRunningState;
    private final NetlinkTracker mNetlinkTracker;
    private StaticIpConfiguration mStaticIpConfig;

    public MtkIpRunningState(Context context, IpClient ipClient, String str, NetlinkTracker netlinkTracker, State state) {
        Log.d(TAG, "Initialize MtkIpRunningState");
        this.mContext = context;
        this.mIpClient = ipClient;
        this.mIfaceName = str;
        this.mNetlinkTracker = netlinkTracker;
        this.mIpRunningState = state;
    }

    public void enter() {
        this.mIpRunningState.enter();
        this.mStaticIpConfig = getIpConfiguration();
        if (isDhcp6Support() && this.mStaticIpConfig == null) {
            this.mDhcp6Client = MtkDhcp6Client.makeDhcp6Client(this.mContext, this.mIpClient, this.mIfaceName);
            this.mDhcp6Client.registerForPreDhcpNotification();
            this.mDhcp6Client.sendMessage(MtkDhcp6Client.CMD_START_DHCP);
        }
        Log.d(TAG, "enter");
    }

    public void exit() {
        this.mIpRunningState.exit();
        if (isDhcp6Support(this.mDhcp6Client)) {
            this.mDhcp6Client.sendMessage(MtkDhcp6Client.CMD_STOP_DHCP);
            this.mDhcp6Client.doQuit();
        }
        Log.d(TAG, "exit");
    }

    public boolean processMessage(Message message) {
        int i = message.what;
        if (i != EVENT_PRE_DHCP_ACTION_COMPLETE) {
            if (i != 196615) {
                if (i == 196813) {
                    Log.e(TAG, "Unexpected v6 CMD_ON_QUIT.");
                    this.mDhcp6Client = null;
                    this.mDnsV6Servers = null;
                    return true;
                }
                if (i == 196816) {
                    this.mDnsV6Servers = (ArrayList) message.obj;
                    String[] strArr = new String[this.mDnsV6Servers.size()];
                    for (int i2 = 0; i2 < this.mDnsV6Servers.size(); i2++) {
                        strArr[i2] = this.mDnsV6Servers.get(i2).getHostAddress();
                    }
                    this.mNetlinkTracker.interfaceDnsServerInfo(this.mIfaceName, 3600L, strArr);
                    try {
                        Method declaredMethod = this.mIpClient.getClass().getDeclaredMethod("handleLinkPropertiesUpdate", Boolean.TYPE);
                        declaredMethod.setAccessible(true);
                        declaredMethod.invoke(this.mIpClient, true);
                    } catch (ReflectiveOperationException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            } else if (this.mStaticIpConfig != null) {
                Log.d(TAG, "static Ip is configured, ignore clearIPv4Address");
                return true;
            }
        } else if (isDhcp6Support(this.mDhcp6Client)) {
            this.mDhcp6Client.sendMessage(MtkDhcp6Client.CMD_PRE_DHCP_ACTION_COMPLETE);
        }
        return this.mIpRunningState.processMessage(message);
    }

    private boolean isDhcp6Support() {
        if (sMtkDhcpv6cWifi) {
            return WLAN_INTERFACE.equals(this.mIfaceName);
        }
        return false;
    }

    private boolean isDhcp6Support(MtkDhcp6Client mtkDhcp6Client) {
        if (sMtkDhcpv6cWifi && mtkDhcp6Client != null) {
            return WLAN_INTERFACE.equals(this.mIfaceName);
        }
        return false;
    }

    private StaticIpConfiguration getIpConfiguration() {
        StaticIpConfiguration staticIpConfiguration;
        StaticIpConfiguration staticIpConfiguration2 = null;
        try {
            Field declaredField = this.mIpClient.getClass().getDeclaredField("mConfiguration");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(this.mIpClient);
            Field declaredField2 = obj.getClass().getDeclaredField("mStaticIpConfig");
            declaredField2.setAccessible(true);
            staticIpConfiguration = (StaticIpConfiguration) declaredField2.get(obj);
        } catch (Exception e) {
            e = e;
        }
        try {
            Log.d(TAG, "getIpConfiguration:" + staticIpConfiguration);
            return staticIpConfiguration;
        } catch (Exception e2) {
            e = e2;
            staticIpConfiguration2 = staticIpConfiguration;
            e.printStackTrace();
            return staticIpConfiguration2;
        }
    }
}
