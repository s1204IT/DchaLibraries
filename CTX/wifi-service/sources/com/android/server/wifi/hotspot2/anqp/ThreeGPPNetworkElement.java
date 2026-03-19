package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreeGPPNetworkElement extends ANQPElement {

    @VisibleForTesting
    public static final int GUD_VERSION_1 = 0;
    private final List<CellularNetwork> mNetworks;

    @VisibleForTesting
    public ThreeGPPNetworkElement(List<CellularNetwork> list) {
        super(Constants.ANQPElementType.ANQP3GPPNetwork);
        this.mNetworks = list;
    }

    public static ThreeGPPNetworkElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        int i = byteBuffer.get() & 255;
        if (i != 0) {
            throw new ProtocolException("Unsupported GUD version: " + i);
        }
        int i2 = byteBuffer.get() & 255;
        if (i2 != byteBuffer.remaining()) {
            throw new ProtocolException("Mismatch length and buffer size: length=" + i2 + " bufferSize=" + byteBuffer.remaining());
        }
        ArrayList arrayList = new ArrayList();
        while (byteBuffer.hasRemaining()) {
            CellularNetwork cellularNetwork = CellularNetwork.parse(byteBuffer);
            if (cellularNetwork != null) {
                arrayList.add(cellularNetwork);
            }
        }
        return new ThreeGPPNetworkElement(arrayList);
    }

    public List<CellularNetwork> getNetworks() {
        return Collections.unmodifiableList(this.mNetworks);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ThreeGPPNetworkElement)) {
            return false;
        }
        return this.mNetworks.equals(((ThreeGPPNetworkElement) obj).mNetworks);
    }

    public int hashCode() {
        return this.mNetworks.hashCode();
    }

    public String toString() {
        return "ThreeGPPNetwork{mNetworks=" + this.mNetworks + "}";
    }
}
