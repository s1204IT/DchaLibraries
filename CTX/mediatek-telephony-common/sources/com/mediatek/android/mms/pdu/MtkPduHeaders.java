package com.mediatek.android.mms.pdu;

import android.hardware.radio.V1_0.RadioCdmaSmsConst;
import android.util.Log;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu.PduHeaders;
import java.util.ArrayList;

public class MtkPduHeaders extends PduHeaders {
    public static final int DATE_SENT = 201;
    public static final int STATE_SKIP_RETRYING = 137;
    private static final String TAG = "MtkPduHeaders";

    protected void setOctet(int i, int i2) throws InvalidHeaderValueException {
        int i3 = 224;
        switch (i2) {
            case RadioCdmaSmsConst.UDH_VAR_PIC_SIZE:
            case 144:
            case 145:
            case 148:
            case 162:
            case MtkPduPart.P_TRANSFER_ENCODING:
            case 169:
            case 171:
            case 177:
            case 187:
            case 188:
                if (128 != i && 129 != i) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 140:
                if (i < 128 || i > 151) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 141:
                if (i < 16 || i > 19) {
                    i = 18;
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 143:
                if (i < 128 || i > 130) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case MtkPduPart.P_DATE:
                if (i <= 196 || i >= 224) {
                    if ((i <= 235 || i > 255) && i >= 128 && ((i <= 136 || i >= 192) && i <= 255)) {
                        i3 = i;
                    }
                } else {
                    i3 = 192;
                }
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 149:
                if (i < 128 || i > 137) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 153:
                if (i <= 194 || i >= 224) {
                    if ((i <= 227 || i > 255) && i >= 128 && ((i <= 128 || i >= 192) && i <= 255)) {
                    }
                }
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 155:
                if (128 != i && 129 != i) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 156:
                if (i < 128 || i > 131) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 163:
                if (i < 128 || i > 132) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 165:
                if (i <= 193 || i >= 224) {
                    if ((i <= 228 || i > 255) && i >= 128 && ((i <= 128 || i >= 192) && i <= 255)) {
                    }
                }
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 180:
                if (128 != i) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 186:
                if (i < 128 || i > 135) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            case 191:
                if (128 != i && 129 != i) {
                    throw new InvalidHeaderValueException("Invalid Octet value!");
                }
                i3 = i;
                this.mHeaderMap.put(Integer.valueOf(i2), Integer.valueOf(i3));
                return;
            default:
                throw new RuntimeException("Invalid header field!");
        }
    }

    public MtkEncodedStringValue[] getEncodedStringValuesEx(int i) {
        ArrayList arrayList = (ArrayList) this.mHeaderMap.get(Integer.valueOf(i));
        if (arrayList == null) {
            return null;
        }
        return (MtkEncodedStringValue[]) arrayList.toArray(new MtkEncodedStringValue[arrayList.size()]);
    }

    public MtkEncodedStringValue getEncodedStringValueEx(int i) {
        return (MtkEncodedStringValue) this.mHeaderMap.get(Integer.valueOf(i));
    }

    public void setLongInteger(long j, int i) {
        if (i == 201) {
            Log.d(TAG, "DATE_SENT");
            this.mHeaderMap.put(Integer.valueOf(i), Long.valueOf(j));
        } else {
            super.setLongInteger(j, i);
        }
    }
}
