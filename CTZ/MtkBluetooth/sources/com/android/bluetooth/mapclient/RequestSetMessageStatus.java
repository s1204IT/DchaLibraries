package com.android.bluetooth.mapclient;

import java.io.IOException;
import javax.obex.ClientSession;

final class RequestSetMessageStatus extends Request {
    private static final String TYPE = "x-bt/messageStatus";

    public enum StatusIndicator {
        READ,
        DELETED
    }

    public RequestSetMessageStatus(String str, StatusIndicator statusIndicator) {
        this.mHeaderSet.setHeader(66, TYPE);
        this.mHeaderSet.setHeader(1, str);
        ObexAppParameters obexAppParameters = new ObexAppParameters();
        obexAppParameters.add((byte) 23, statusIndicator == StatusIndicator.READ ? (byte) 0 : (byte) 1);
        obexAppParameters.add((byte) 24, (byte) 1);
        obexAppParameters.addToHeaderSet(this.mHeaderSet);
    }

    @Override
    public void execute(ClientSession clientSession) throws IOException {
        executePut(clientSession, FILLER_BYTE);
    }
}
