package com.android.bluetooth.mapclient;

import java.io.IOException;
import javax.obex.ClientSession;

final class RequestSetNotificationRegistration extends Request {
    private static final String TYPE = "x-bt/MAP-NotificationRegistration";
    private final boolean mStatus;

    RequestSetNotificationRegistration(boolean z) {
        this.mStatus = z;
        this.mHeaderSet.setHeader(66, TYPE);
        ObexAppParameters obexAppParameters = new ObexAppParameters();
        obexAppParameters.add((byte) 14, z ? (byte) 1 : (byte) 0);
        obexAppParameters.addToHeaderSet(this.mHeaderSet);
    }

    @Override
    public void execute(ClientSession clientSession) throws IOException {
        executePut(clientSession, FILLER_BYTE);
    }

    public boolean getStatus() {
        return this.mStatus;
    }
}
