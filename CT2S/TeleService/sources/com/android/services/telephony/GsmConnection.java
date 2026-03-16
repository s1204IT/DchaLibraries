package com.android.services.telephony;

import com.android.internal.telephony.Connection;

final class GsmConnection extends TelephonyConnection {
    GsmConnection(Connection connection) {
        super(connection);
    }

    @Override
    public TelephonyConnection cloneConnection() {
        GsmConnection gsmConnection = new GsmConnection(getOriginalConnection());
        return gsmConnection;
    }

    @Override
    public void onPlayDtmfTone(char digit) {
        if (getPhone() != null) {
            getPhone().startDtmf(digit);
        }
    }

    @Override
    public void onStopDtmfTone() {
        if (getPhone() != null) {
            getPhone().stopDtmf();
        }
    }

    @Override
    protected int buildConnectionCapabilities() {
        int capabilities = super.buildConnectionCapabilities() | 64 | 2;
        if (getState() == 4 || getState() == 5) {
            return capabilities | 1;
        }
        return capabilities;
    }
}
