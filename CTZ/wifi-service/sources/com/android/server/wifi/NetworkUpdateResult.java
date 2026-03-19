package com.android.server.wifi;

public class NetworkUpdateResult {
    boolean credentialChanged;
    boolean ipChanged;
    boolean isNewNetwork;
    int netId;
    boolean proxyChanged;

    public NetworkUpdateResult(int i) {
        this.isNewNetwork = false;
        this.netId = i;
        this.ipChanged = false;
        this.proxyChanged = false;
        this.credentialChanged = false;
    }

    public NetworkUpdateResult(boolean z, boolean z2, boolean z3) {
        this.isNewNetwork = false;
        this.netId = -1;
        this.ipChanged = z;
        this.proxyChanged = z2;
        this.credentialChanged = z3;
    }

    public void setNetworkId(int i) {
        this.netId = i;
    }

    public int getNetworkId() {
        return this.netId;
    }

    public boolean hasIpChanged() {
        return this.ipChanged;
    }

    public boolean hasProxyChanged() {
        return this.proxyChanged;
    }

    public boolean hasCredentialChanged() {
        return this.credentialChanged;
    }

    public boolean isNewNetwork() {
        return this.isNewNetwork;
    }

    public void setIsNewNetwork(boolean z) {
        this.isNewNetwork = z;
    }

    public boolean isSuccess() {
        return this.netId != -1;
    }
}
