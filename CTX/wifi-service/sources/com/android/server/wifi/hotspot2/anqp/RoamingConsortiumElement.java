package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.Utils;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoamingConsortiumElement extends ANQPElement {

    @VisibleForTesting
    public static final int MAXIMUM_OI_LENGTH = 8;

    @VisibleForTesting
    public static final int MINIMUM_OI_LENGTH = 1;
    private final List<Long> mOIs;

    @VisibleForTesting
    public RoamingConsortiumElement(List<Long> list) {
        super(Constants.ANQPElementType.ANQPRoamingConsortium);
        this.mOIs = list;
    }

    public static RoamingConsortiumElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        ArrayList arrayList = new ArrayList();
        while (byteBuffer.hasRemaining()) {
            int i = byteBuffer.get() & 255;
            if (i < 1 || i > 8) {
                throw new ProtocolException("Bad OI length: " + i);
            }
            arrayList.add(Long.valueOf(ByteBufferReader.readInteger(byteBuffer, ByteOrder.BIG_ENDIAN, i)));
        }
        return new RoamingConsortiumElement(arrayList);
    }

    public List<Long> getOIs() {
        return Collections.unmodifiableList(this.mOIs);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RoamingConsortiumElement)) {
            return false;
        }
        return this.mOIs.equals(((RoamingConsortiumElement) obj).mOIs);
    }

    public int hashCode() {
        return this.mOIs.hashCode();
    }

    public String toString() {
        return "RoamingConsortium{mOis=[" + Utils.roamingConsortiumsToString(this.mOIs) + "]}";
    }
}
