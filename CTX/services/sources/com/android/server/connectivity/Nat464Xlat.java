package com.android.server.connectivity;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.net.BaseNetworkObserver;
import java.net.Inet4Address;
import java.util.Objects;

public class Nat464Xlat extends BaseNetworkObserver {
    private static final String CLAT_PREFIX = "v4-";
    private String mBaseIface;
    private String mIface;
    private final INetworkManagementService mNMService;
    private final NetworkAgentInfo mNetwork;
    private State mState = State.IDLE;
    private static final String TAG = Nat464Xlat.class.getSimpleName();
    private static final int[] NETWORK_TYPES = {0, 1, 9};
    private static final NetworkInfo.State[] NETWORK_STATES = {NetworkInfo.State.CONNECTED, NetworkInfo.State.SUSPENDED};

    private enum State {
        IDLE,
        STARTING,
        RUNNING,
        STOPPING
    }

    public Nat464Xlat(INetworkManagementService iNetworkManagementService, NetworkAgentInfo networkAgentInfo) {
        this.mNMService = iNetworkManagementService;
        this.mNetwork = networkAgentInfo;
    }

    public static boolean requiresClat(NetworkAgentInfo networkAgentInfo) {
        return ArrayUtils.contains(NETWORK_TYPES, networkAgentInfo.networkInfo.getType()) && ArrayUtils.contains(NETWORK_STATES, networkAgentInfo.networkInfo.getState()) && !(networkAgentInfo.linkProperties != null && networkAgentInfo.linkProperties.hasIPv4Address());
    }

    public boolean isStarted() {
        return this.mState != State.IDLE;
    }

    public boolean isStarting() {
        return this.mState == State.STARTING;
    }

    public boolean isRunning() {
        return this.mState == State.RUNNING;
    }

    public boolean isStopping() {
        return this.mState == State.STOPPING;
    }

    private void enterStartingState(String str) {
        try {
            this.mNMService.registerObserver(this);
            try {
                this.mNMService.startClatd(str);
            } catch (RemoteException | IllegalStateException e) {
                Slog.e(TAG, "Error starting clatd on " + str, e);
            }
            this.mIface = CLAT_PREFIX + str;
            this.mBaseIface = str;
            this.mState = State.STARTING;
        } catch (RemoteException e2) {
            Slog.e(TAG, "startClat: Can't register interface observer for clat on " + this.mNetwork.name());
        }
    }

    private void enterRunningState() {
        this.mState = State.RUNNING;
    }

    private void enterStoppingState() {
        try {
            this.mNMService.stopClatd(this.mBaseIface);
        } catch (RemoteException | IllegalStateException e) {
            Slog.e(TAG, "Error stopping clatd on " + this.mBaseIface, e);
        }
        this.mState = State.STOPPING;
    }

    private void enterIdleState() {
        try {
            this.mNMService.unregisterObserver(this);
        } catch (RemoteException | IllegalStateException e) {
            Slog.e(TAG, "Error unregistering clatd observer on " + this.mBaseIface, e);
        }
        this.mIface = null;
        this.mBaseIface = null;
        this.mState = State.IDLE;
    }

    public void start() {
        if (isStarted()) {
            Slog.e(TAG, "startClat: already started");
            return;
        }
        if (this.mNetwork.linkProperties == null) {
            Slog.e(TAG, "startClat: Can't start clat with null LinkProperties");
            return;
        }
        String interfaceName = this.mNetwork.linkProperties.getInterfaceName();
        if (interfaceName == null) {
            Slog.e(TAG, "startClat: Can't start clat on null interface");
            return;
        }
        Slog.i(TAG, "Starting clatd on " + interfaceName);
        enterStartingState(interfaceName);
    }

    public void stop() {
        if (!isStarted()) {
            return;
        }
        Slog.i(TAG, "Stopping clatd on " + this.mBaseIface);
        boolean zIsStarting = isStarting();
        enterStoppingState();
        if (zIsStarting) {
            enterIdleState();
        }
    }

    public void fixupLinkProperties(LinkProperties linkProperties, LinkProperties linkProperties2) {
        if (!isRunning() || linkProperties2 == null || linkProperties2.getAllInterfaceNames().contains(this.mIface)) {
            return;
        }
        Slog.d(TAG, "clatd running, updating NAI for " + this.mIface);
        for (LinkProperties linkProperties3 : linkProperties.getStackedLinks()) {
            if (Objects.equals(this.mIface, linkProperties3.getInterfaceName())) {
                linkProperties2.addStackedLink(linkProperties3);
                return;
            }
        }
    }

    private LinkProperties makeLinkProperties(LinkAddress linkAddress) {
        LinkProperties linkProperties = new LinkProperties();
        linkProperties.setInterfaceName(this.mIface);
        linkProperties.addRoute(new RouteInfo(new LinkAddress(Inet4Address.ANY, 0), linkAddress.getAddress(), this.mIface));
        linkProperties.addLinkAddress(linkAddress);
        return linkProperties;
    }

    private LinkAddress getLinkAddress(String str) {
        try {
            return this.mNMService.getInterfaceConfig(str).getLinkAddress();
        } catch (RemoteException | IllegalStateException e) {
            Slog.e(TAG, "Error getting link properties: " + e);
            return null;
        }
    }

    private void handleInterfaceLinkStateChanged(String str, boolean z) {
        if (!isStarting() || !z || !Objects.equals(this.mIface, str)) {
            return;
        }
        LinkAddress linkAddress = getLinkAddress(str);
        if (linkAddress == null) {
            Slog.e(TAG, "clatAddress was null for stacked iface " + str);
            return;
        }
        Slog.i(TAG, String.format("interface %s is up, adding stacked link %s on top of %s", this.mIface, this.mIface, this.mBaseIface));
        enterRunningState();
        LinkProperties linkProperties = new LinkProperties(this.mNetwork.linkProperties);
        linkProperties.addStackedLink(makeLinkProperties(linkAddress));
        this.mNetwork.connService().handleUpdateLinkProperties(this.mNetwork, linkProperties);
    }

    private void handleInterfaceRemoved(String str) {
        if (!Objects.equals(this.mIface, str)) {
            return;
        }
        if (!isRunning() && !isStopping()) {
            return;
        }
        Slog.i(TAG, "interface " + str + " removed");
        if (!isStopping()) {
            enterStoppingState();
        }
        enterIdleState();
        LinkProperties linkProperties = new LinkProperties(this.mNetwork.linkProperties);
        linkProperties.removeStackedLink(str);
        this.mNetwork.connService().handleUpdateLinkProperties(this.mNetwork, linkProperties);
    }

    public void interfaceLinkStateChanged(final String str, final boolean z) {
        this.mNetwork.handler().post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.handleInterfaceLinkStateChanged(str, z);
            }
        });
    }

    public void interfaceRemoved(final String str) {
        this.mNetwork.handler().post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.handleInterfaceRemoved(str);
            }
        });
    }

    public String toString() {
        return "mBaseIface: " + this.mBaseIface + ", mIface: " + this.mIface + ", mState: " + this.mState;
    }
}
