package com.android.bluetooth.map;

import android.util.Log;
import com.android.bluetooth.DeviceWorkArounds;
import com.android.bluetooth.map.BluetoothMapSmsPdu;
import com.android.bluetooth.map.BluetoothMapUtils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class BluetoothMapbMessageSms extends BluetoothMapbMessage {
    private ArrayList<BluetoothMapSmsPdu.SmsPdu> mSmsBodyPdus = null;
    private String mSmsBody = null;

    public void setSmsBodyPdus(ArrayList<BluetoothMapSmsPdu.SmsPdu> arrayList) {
        this.mSmsBodyPdus = arrayList;
        this.mCharset = null;
        if (arrayList.size() > 0) {
            this.mEncoding = arrayList.get(0).getEncodingString();
        }
    }

    public String getSmsBody() {
        return this.mSmsBody;
    }

    public void setSmsBody(String str) {
        this.mSmsBody = str;
        this.mCharset = "UTF-8";
        this.mEncoding = null;
    }

    @Override
    public void parseMsgPart(String str) {
        if (this.mAppParamCharset == 0) {
            if (D) {
                Log.d("BluetoothMapbMessage", "Decoding \"" + str + "\" as native PDU");
            }
            byte[] bArrDecodeBinary = decodeBinary(str);
            if (bArrDecodeBinary.length > 0 && bArrDecodeBinary[0] < bArrDecodeBinary.length - 1 && (bArrDecodeBinary[bArrDecodeBinary[0] + 1] & 3) != 1) {
                if (D) {
                    Log.d("BluetoothMapbMessage", "Only submit PDUs are supported");
                }
                throw new IllegalArgumentException("Only submit PDUs are supported");
            }
            StringBuilder sb = new StringBuilder();
            sb.append(this.mSmsBody);
            sb.append(BluetoothMapSmsPdu.decodePdu(bArrDecodeBinary, this.mType == BluetoothMapUtils.TYPE.SMS_CDMA ? 2 : 1));
            this.mSmsBody = sb.toString();
            return;
        }
        this.mSmsBody += str;
    }

    @Override
    public void parseMsgInit() {
        this.mSmsBody = "";
    }

    @Override
    public byte[] encode() throws UnsupportedEncodingException {
        ArrayList<byte[]> arrayList = new ArrayList<>();
        if (this.mSmsBody != null) {
            String strReplaceAll = this.mSmsBody.replaceAll("END:MSG", "/END\\:MSG");
            String address = BluetoothMapService.getRemoteDevice().getAddress();
            if (DeviceWorkArounds.addressStartsWith(address, DeviceWorkArounds.PCM_CARKIT)) {
                strReplaceAll = strReplaceAll.replaceAll("\r", "");
            } else if (DeviceWorkArounds.addressStartsWith(address, DeviceWorkArounds.FORD_SYNC_CARKIT)) {
                strReplaceAll = strReplaceAll.replaceAll("\n", "");
            } else if (DeviceWorkArounds.addressStartsWith(address, DeviceWorkArounds.SYNC_CARKIT) && strReplaceAll.length() > 0) {
                int i = 0;
                while (strReplaceAll.charAt((strReplaceAll.length() - i) - 1) == '\n') {
                    i++;
                }
                strReplaceAll = strReplaceAll.substring(0, strReplaceAll.length() - i);
            }
            arrayList.add(strReplaceAll.getBytes("UTF-8"));
        } else if (this.mSmsBodyPdus != null && this.mSmsBodyPdus.size() > 0) {
            for (BluetoothMapSmsPdu.SmsPdu smsPdu : this.mSmsBodyPdus) {
                arrayList.add(encodeBinary(smsPdu.getData(), smsPdu.getScAddress()).getBytes("UTF-8"));
            }
        } else {
            arrayList.add(new byte[0]);
        }
        return encodeGeneric(arrayList);
    }
}
