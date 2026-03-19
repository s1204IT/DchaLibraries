package com.android.server.wifi.hotspot2.anqp;

import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CellularNetwork {

    @VisibleForTesting
    public static final int IEI_CONTENT_LENGTH_MASK = 127;

    @VisibleForTesting
    public static final int IEI_TYPE_PLMN_LIST = 0;
    private static final int MNC_2DIGIT_VALUE = 15;

    @VisibleForTesting
    public static final int PLMN_DATA_BYTES = 3;
    private static final String TAG = "CellularNetwork";
    private final List<String> mPlmnList;

    @VisibleForTesting
    public CellularNetwork(List<String> list) {
        this.mPlmnList = list;
    }

    public static CellularNetwork parse(ByteBuffer byteBuffer) throws ProtocolException {
        int i = byteBuffer.get() & 255;
        int i2 = byteBuffer.get() & 127;
        if (i != 0) {
            Log.e(TAG, "Ignore unsupported IEI Type: " + i);
            byteBuffer.position(byteBuffer.position() + i2);
            return null;
        }
        int i3 = byteBuffer.get() & 255;
        if (i2 != (i3 * 3) + 1) {
            throw new ProtocolException("IEI size and PLMN count mismatched: IEI Size=" + i2 + " PLMN Count=" + i3);
        }
        ArrayList arrayList = new ArrayList();
        while (i3 > 0) {
            arrayList.add(parsePlmn(byteBuffer));
            i3--;
        }
        return new CellularNetwork(arrayList);
    }

    public List<String> getPlmns() {
        return Collections.unmodifiableList(this.mPlmnList);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CellularNetwork)) {
            return false;
        }
        return this.mPlmnList.equals(((CellularNetwork) obj).mPlmnList);
    }

    public int hashCode() {
        return this.mPlmnList.hashCode();
    }

    public String toString() {
        return "CellularNetwork{mPlmnList=" + this.mPlmnList + "}";
    }

    private static String parsePlmn(ByteBuffer byteBuffer) {
        byte[] bArr = new byte[3];
        byteBuffer.get(bArr);
        int i = ((bArr[0] << 8) & 3840) | (bArr[0] & 240) | (bArr[1] & 15);
        int i2 = ((bArr[2] << 4) & 240) | ((bArr[2] >> 4) & 15);
        int i3 = (bArr[1] >> 4) & 15;
        if (i3 != 15) {
            return String.format("%03x%03x", Integer.valueOf(i), Integer.valueOf((i2 << 4) | i3));
        }
        return String.format("%03x%02x", Integer.valueOf(i), Integer.valueOf(i2));
    }
}
