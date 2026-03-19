package com.android.bluetooth.mapclient;

import com.android.bluetooth.mapclient.MasClient;
import java.io.IOException;
import java.math.BigInteger;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;

final class RequestPushMessage extends Request {
    private static final String TYPE = "x-bt/message";
    private Bmessage mMsg;
    private String mMsgHandle;

    private RequestPushMessage(String str) {
        this.mHeaderSet.setHeader(66, TYPE);
        this.mHeaderSet.setHeader(1, str == null ? "" : str);
    }

    RequestPushMessage(String str, Bmessage bmessage, MasClient.CharsetType charsetType, boolean z, boolean z2) {
        this(str);
        this.mMsg = bmessage;
        ObexAppParameters obexAppParameters = new ObexAppParameters();
        obexAppParameters.add((byte) 11, z ? (byte) 1 : (byte) 0);
        obexAppParameters.add((byte) 12, z2 ? (byte) 1 : (byte) 0);
        obexAppParameters.add((byte) 20, charsetType == MasClient.CharsetType.NATIVE ? (byte) 0 : (byte) 1);
        obexAppParameters.addToHeaderSet(this.mHeaderSet);
    }

    @Override
    protected void readResponseHeaders(HeaderSet headerSet) {
        try {
            String str = (String) headerSet.getHeader(1);
            if (str != null) {
                new BigInteger(str, 16);
                this.mMsgHandle = str;
            }
        } catch (IOException e) {
            this.mResponseCode = 208;
        } catch (NumberFormatException e2) {
            this.mResponseCode = 208;
        }
    }

    public Bmessage getBMsg() {
        return this.mMsg;
    }

    public String getMsgHandle() {
        return this.mMsgHandle;
    }

    @Override
    public void execute(ClientSession clientSession) throws IOException {
        executePut(clientSession, BmessageBuilder.createBmessage(this.mMsg).getBytes());
    }
}
