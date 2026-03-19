package com.android.bluetooth.mapclient;

import android.util.Log;
import com.android.bluetooth.mapclient.MasClient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import javax.obex.ClientSession;

final class RequestGetMessage extends Request {
    private static final String TAG = "RequestGetMessage";
    private static final String TYPE = "x-bt/message";
    private Bmessage mBmessage;

    RequestGetMessage(String str, MasClient.CharsetType charsetType, boolean z) {
        this.mHeaderSet.setHeader(1, str);
        this.mHeaderSet.setHeader(66, TYPE);
        ObexAppParameters obexAppParameters = new ObexAppParameters();
        obexAppParameters.add((byte) 20, MasClient.CharsetType.UTF_8.equals(charsetType) ? (byte) 1 : (byte) 0);
        obexAppParameters.add((byte) 10, z ? (byte) 1 : (byte) 0);
        obexAppParameters.addToHeaderSet(this.mHeaderSet);
    }

    @Override
    protected void readResponse(InputStream inputStream) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[1024];
        while (true) {
            try {
                int i = inputStream.read(bArr);
                if (i != -1) {
                    byteArrayOutputStream.write(bArr, 0, i);
                }
            } catch (IOException e) {
                Log.e(TAG, "I/O exception while reading response", e);
            }
            try {
                break;
            } catch (UnsupportedEncodingException e2) {
                Log.e(TAG, "Coudn't decode the bmessage with UTF-8. Something must be really messed up.");
                return;
            }
        }
        this.mBmessage = BmessageParser.createBmessage(byteArrayOutputStream.toString(StandardCharsets.UTF_8.name()));
        if (this.mBmessage == null) {
            this.mResponseCode = 208;
        }
    }

    public Bmessage getMessage() {
        return this.mBmessage;
    }

    public String getHandle() {
        try {
            return (String) this.mHeaderSet.getHeader(1);
        } catch (IOException e) {
            Log.e(TAG, "Unexpected exception while reading handle!", e);
            return null;
        }
    }

    @Override
    public void execute(ClientSession clientSession) throws IOException {
        executeGet(clientSession);
    }
}
