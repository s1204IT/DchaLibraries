package com.android.settings.wifi;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import com.android.internal.util.Preconditions;

/* loaded from: classes.dex */
final class CaptivePortalNetworkCallback extends ConnectivityManager.NetworkCallback {
    private final ConnectedAccessPointPreference mConnectedApPreference;
    private boolean mIsCaptivePortal;
    private final Network mNetwork;

    CaptivePortalNetworkCallback(Network network, ConnectedAccessPointPreference connectedAccessPointPreference) {
        this.mNetwork = (Network) Preconditions.checkNotNull(network);
        this.mConnectedApPreference = (ConnectedAccessPointPreference) Preconditions.checkNotNull(connectedAccessPointPreference);
    }

    @Override // android.net.ConnectivityManager.NetworkCallback
    public void onLost(Network network) {
        if (this.mNetwork.equals(network)) {
            this.mIsCaptivePortal = false;
        }
    }

    @Override // android.net.ConnectivityManager.NetworkCallback
    public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        if (this.mNetwork.equals(network)) {
            this.mIsCaptivePortal = WifiUtils.canSignIntoNetwork(networkCapabilities);
            this.mConnectedApPreference.setCaptivePortal(this.mIsCaptivePortal);
        }
    }

    public boolean isSameNetworkAndPreference(Network network, ConnectedAccessPointPreference connectedAccessPointPreference) {
        return this.mNetwork.equals(network) && this.mConnectedApPreference == connectedAccessPointPreference;
    }

    public boolean isCaptivePortal() {
        return this.mIsCaptivePortal;
    }

    public Network getNetwork() {
        return this.mNetwork;
    }
}
