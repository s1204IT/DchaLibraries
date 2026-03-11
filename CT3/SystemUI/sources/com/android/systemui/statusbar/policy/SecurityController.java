package com.android.systemui.statusbar.policy;

public interface SecurityController {

    public interface SecurityControllerCallback {
        void onStateChanged();
    }

    void addCallback(SecurityControllerCallback securityControllerCallback);

    String getDeviceOwnerName();

    String getPrimaryVpnName();

    String getProfileOwnerName();

    String getProfileVpnName();

    boolean hasProfileOwner();

    boolean isDeviceManaged();

    boolean isVpnEnabled();

    boolean isVpnRestricted();

    void removeCallback(SecurityControllerCallback securityControllerCallback);
}
