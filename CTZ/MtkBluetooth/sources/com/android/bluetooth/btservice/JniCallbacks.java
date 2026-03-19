package com.android.bluetooth.btservice;

final class JniCallbacks {
    private AdapterProperties mAdapterProperties;
    private AdapterService mAdapterService;
    private BondStateMachine mBondStateMachine;
    private RemoteDevices mRemoteDevices;

    JniCallbacks(AdapterService adapterService, AdapterProperties adapterProperties) {
        this.mAdapterService = adapterService;
        this.mAdapterProperties = adapterProperties;
    }

    void init(BondStateMachine bondStateMachine, RemoteDevices remoteDevices) {
        this.mRemoteDevices = remoteDevices;
        this.mBondStateMachine = bondStateMachine;
    }

    void cleanup() {
        this.mRemoteDevices = null;
        this.mAdapterProperties = null;
        this.mAdapterService = null;
        this.mBondStateMachine = null;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    void sspRequestCallback(byte[] bArr, byte[] bArr2, int i, int i2, int i3) {
        this.mBondStateMachine.sspRequestCallback(bArr, bArr2, i, i2, i3);
    }

    void devicePropertyChangedCallback(byte[] bArr, int[] iArr, byte[][] bArr2) {
        this.mRemoteDevices.devicePropertyChangedCallback(bArr, iArr, bArr2);
    }

    void deviceFoundCallback(byte[] bArr) {
        this.mRemoteDevices.deviceFoundCallback(bArr);
    }

    void pinRequestCallback(byte[] bArr, byte[] bArr2, int i, boolean z) {
        this.mBondStateMachine.pinRequestCallback(bArr, bArr2, i, z);
    }

    void bondStateChangeCallback(int i, byte[] bArr, int i2) {
        this.mBondStateMachine.bondStateChangeCallback(i, bArr, i2);
    }

    void aclStateChangeCallback(int i, byte[] bArr, int i2) {
        this.mRemoteDevices.aclStateChangeCallback(i, bArr, i2);
    }

    void stateChangeCallback(int i) {
        this.mAdapterService.stateChangeCallback(i);
    }

    void discoveryStateChangeCallback(int i) {
        this.mAdapterProperties.discoveryStateChangeCallback(i);
    }

    void adapterPropertyChangedCallback(int[] iArr, byte[][] bArr) {
        this.mAdapterProperties.adapterPropertyChangedCallback(iArr, bArr);
    }
}
