package com.android.bluetooth.map;

import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class BluetoothMapbMessageEmail extends BluetoothMapbMessage {
    private String mEmailBody = null;

    public void setEmailBody(String emailBody) {
        this.mEmailBody = emailBody;
        this.mCharset = "UTF-8";
        this.mEncoding = "8bit";
    }

    public String getEmailBody() {
        return this.mEmailBody;
    }

    @Override
    public void parseMsgPart(String msgPart) {
        if (this.mEmailBody == null) {
            this.mEmailBody = msgPart;
        } else {
            this.mEmailBody += msgPart;
        }
    }

    @Override
    public void parseMsgInit() {
    }

    @Override
    public byte[] encode() throws UnsupportedEncodingException {
        ArrayList<byte[]> bodyFragments = new ArrayList<>();
        if (this.mEmailBody != null) {
            String tmpBody = this.mEmailBody.replaceAll("END:MSG", "/END\\:MSG");
            bodyFragments.add(tmpBody.getBytes("UTF-8"));
        } else {
            Log.e(TAG, "Email has no body - this should not be possible");
            bodyFragments.add(new byte[0]);
        }
        return encodeGeneric(bodyFragments);
    }
}
