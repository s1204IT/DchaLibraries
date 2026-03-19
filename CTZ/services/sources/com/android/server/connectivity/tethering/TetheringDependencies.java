package com.android.server.connectivity.tethering;

import android.content.Context;
import android.net.INetd;
import android.net.ip.RouterAdvertisementDaemon;
import android.net.util.InterfaceParams;
import android.net.util.NetdService;
import android.net.util.SharedLog;
import android.os.Handler;
import com.android.internal.util.StateMachine;
import java.util.ArrayList;

public class TetheringDependencies {
    public OffloadHardwareInterface getOffloadHardwareInterface(Handler handler, SharedLog sharedLog) {
        return new OffloadHardwareInterface(handler, sharedLog);
    }

    public UpstreamNetworkMonitor getUpstreamNetworkMonitor(Context context, StateMachine stateMachine, SharedLog sharedLog, int i) {
        return new UpstreamNetworkMonitor(context, stateMachine, sharedLog, i);
    }

    public IPv6TetheringCoordinator getIPv6TetheringCoordinator(ArrayList<TetherInterfaceStateMachine> arrayList, SharedLog sharedLog) {
        return new IPv6TetheringCoordinator(arrayList, sharedLog);
    }

    public RouterAdvertisementDaemon getRouterAdvertisementDaemon(InterfaceParams interfaceParams) {
        return new RouterAdvertisementDaemon(interfaceParams);
    }

    public InterfaceParams getInterfaceParams(String str) {
        return InterfaceParams.getByName(str);
    }

    public INetd getNetdService() {
        return NetdService.getInstance();
    }

    public boolean isTetheringSupported() {
        return true;
    }
}
