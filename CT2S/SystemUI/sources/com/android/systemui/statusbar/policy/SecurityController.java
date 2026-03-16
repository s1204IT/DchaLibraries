package com.android.systemui.statusbar.policy;

public interface SecurityController {

    public interface SecurityControllerCallback {
        void onStateChanged();
    }

    void addCallback(SecurityControllerCallback securityControllerCallback);

    void disconnectFromVpn();

    String getDeviceOwnerName();

    String getLegacyVpnName();

    String getProfileOwnerName();

    String getVpnApp();

    boolean hasDeviceOwner();

    boolean hasProfileOwner();

    boolean isLegacyVpn();

    boolean isVpnEnabled();

    void onUserSwitched(int i);

    void removeCallback(SecurityControllerCallback securityControllerCallback);
}
