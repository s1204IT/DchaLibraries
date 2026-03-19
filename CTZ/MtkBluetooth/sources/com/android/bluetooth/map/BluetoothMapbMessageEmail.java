package com.android.bluetooth.map;

import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class BluetoothMapbMessageEmail extends BluetoothMapbMessage {
    private String mEmailBody = null;

    public void setEmailBody(String str) {
        this.mEmailBody = str;
        this.mCharset = "UTF-8";
        this.mEncoding = "8bit";
    }

    public String getEmailBody() {
        return this.mEmailBody;
    }

    @Override
    public void parseMsgPart(String str) {
        if (this.mEmailBody == null) {
            this.mEmailBody = str;
            return;
        }
        this.mEmailBody += str;
    }

    @Override
    public void parseMsgInit() {
    }

    @Override
    public byte[] encode() throws UnsupportedEncodingException {
        ArrayList<byte[]> arrayList = new ArrayList<>();
        if (this.mEmailBody != null) {
            arrayList.add(this.mEmailBody.replaceAll("END:MSG", "/END\\:MSG").getBytes("UTF-8"));
        } else {
            Log.e("BluetoothMapbMessage", "Email has no body - this should not be possible");
            arrayList.add(new byte[0]);
        }
        return encodeGeneric(arrayList);
    }
}
