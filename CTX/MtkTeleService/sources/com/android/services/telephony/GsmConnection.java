package com.android.services.telephony;

import com.android.internal.telephony.Connection;

final class GsmConnection extends TelephonyConnection {
    GsmConnection(Connection connection, String str, boolean z) {
        super(connection, str, z);
    }

    @Override
    public TelephonyConnection cloneConnection() {
        return new GsmConnection(getOriginalConnection(), getTelecomCallId(), this.mIsOutgoing);
    }

    @Override
    public void onPlayDtmfTone(char c) {
        if (getPhone() != null) {
            getPhone().startDtmf(c);
        }
    }

    @Override
    public void onStopDtmfTone() {
        if (getPhone() != null) {
            getPhone().stopDtmf();
        }
    }

    @Override
    protected int buildConnectionProperties() {
        int iBuildConnectionProperties = super.buildConnectionProperties();
        if ((getConnectionProperties() & 64) != 0) {
            return iBuildConnectionProperties | 64;
        }
        return iBuildConnectionProperties;
    }

    @Override
    protected int buildConnectionCapabilities() {
        int iBuildConnectionCapabilities = super.buildConnectionCapabilities() | 64;
        if (!shouldTreatAsEmergencyCall()) {
            int i = iBuildConnectionCapabilities | 2;
            if (!isHoldable()) {
                return i;
            }
            if (getState() == 4 || getState() == 5) {
                return i | 1;
            }
            return i;
        }
        return iBuildConnectionCapabilities;
    }

    @Override
    protected void onRemovedFromCallService() {
        super.onRemovedFromCallService();
    }
}
