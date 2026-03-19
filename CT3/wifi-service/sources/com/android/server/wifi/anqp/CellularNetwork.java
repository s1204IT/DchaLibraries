package com.android.server.wifi.anqp;

import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CellularNetwork implements Iterable<String> {
    private static final int PLMNListType = 0;
    private final List<String> mMccMnc;

    private CellularNetwork(int plmnCount, ByteBuffer payload) throws ProtocolException {
        String mccMnc;
        this.mMccMnc = new ArrayList(plmnCount);
        while (plmnCount > 0) {
            if (payload.remaining() < 3) {
                throw new ProtocolException("Truncated PLMN info");
            }
            byte[] plmn = new byte[3];
            payload.get(plmn);
            int mcc = ((plmn[0] << 8) & 3840) | (plmn[0] & 240) | (plmn[1] & 15);
            int mnc = ((plmn[2] << 4) & 240) | ((plmn[2] >> 4) & 15);
            int n2 = (plmn[1] >> 4) & 15;
            if (n2 != 15) {
                mccMnc = String.format("%03x%03x", Integer.valueOf(mcc), Integer.valueOf((mnc << 4) | n2));
            } else {
                mccMnc = String.format("%03x%02x", Integer.valueOf(mcc), Integer.valueOf(mnc));
            }
            this.mMccMnc.add(mccMnc);
            plmnCount--;
        }
    }

    public static CellularNetwork buildCellularNetwork(ByteBuffer payload) throws ProtocolException {
        int iei = payload.get() & 255;
        int plmnLen = payload.get() & 127;
        if (iei != 0) {
            payload.position(payload.position() + plmnLen);
            return null;
        }
        int plmnCount = payload.get() & 255;
        return new CellularNetwork(plmnCount, payload);
    }

    @Override
    public Iterator<String> iterator() {
        return this.mMccMnc.iterator();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PLMN:");
        for (String mccMnc : this.mMccMnc) {
            sb.append(' ').append(mccMnc);
        }
        return sb.toString();
    }
}
