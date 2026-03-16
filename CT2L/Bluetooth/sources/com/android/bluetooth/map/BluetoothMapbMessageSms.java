package com.android.bluetooth.map;

import android.util.Log;
import com.android.bluetooth.map.BluetoothMapSmsPdu;
import com.android.bluetooth.map.BluetoothMapUtils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class BluetoothMapbMessageSms extends BluetoothMapbMessage {
    private ArrayList<BluetoothMapSmsPdu.SmsPdu> mSmsBodyPdus = null;
    private String mSmsBody = null;

    public void setSmsBodyPdus(ArrayList<BluetoothMapSmsPdu.SmsPdu> smsBodyPdus) {
        this.mSmsBodyPdus = smsBodyPdus;
        this.mCharset = null;
        if (smsBodyPdus.size() > 0) {
            this.mEncoding = smsBodyPdus.get(0).getEncodingString();
        }
    }

    public String getSmsBody() {
        return this.mSmsBody;
    }

    public void setSmsBody(String smsBody) {
        this.mSmsBody = smsBody;
        this.mCharset = "UTF-8";
        this.mEncoding = null;
    }

    @Override
    public void parseMsgPart(String msgPart) {
        if (this.mAppParamCharset == 0) {
            Log.d(TAG, "Decoding \"" + msgPart + "\" as native PDU");
            byte[] msgBytes = decodeBinary(msgPart);
            if (msgBytes.length > 0 && msgBytes[0] < msgBytes.length - 1 && (msgBytes[msgBytes[0] + 1] & 3) != 1) {
                Log.d(TAG, "Only submit PDUs are supported");
                throw new IllegalArgumentException("Only submit PDUs are supported");
            }
            this.mSmsBody += BluetoothMapSmsPdu.decodePdu(msgBytes, this.mType == BluetoothMapUtils.TYPE.SMS_CDMA ? BluetoothMapSmsPdu.SMS_TYPE_CDMA : BluetoothMapSmsPdu.SMS_TYPE_GSM);
            return;
        }
        this.mSmsBody += msgPart;
    }

    @Override
    public void parseMsgInit() {
        this.mSmsBody = "";
    }

    @Override
    public byte[] encode() throws UnsupportedEncodingException {
        ArrayList<byte[]> bodyFragments = new ArrayList<>();
        if (this.mSmsBody != null) {
            String tmpBody = this.mSmsBody.replaceAll("END:MSG", "/END\\:MSG");
            bodyFragments.add(tmpBody.getBytes("UTF-8"));
        } else if (this.mSmsBodyPdus != null && this.mSmsBodyPdus.size() > 0) {
            for (BluetoothMapSmsPdu.SmsPdu pdu : this.mSmsBodyPdus) {
                bodyFragments.add(encodeBinary(pdu.getData(), pdu.getScAddress()).getBytes("UTF-8"));
            }
        } else {
            bodyFragments.add(new byte[0]);
        }
        return encodeGeneric(bodyFragments);
    }
}
