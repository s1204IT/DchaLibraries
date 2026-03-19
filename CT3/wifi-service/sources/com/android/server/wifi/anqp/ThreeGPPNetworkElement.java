package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreeGPPNetworkElement extends ANQPElement {
    private final List<CellularNetwork> mPlmns;
    private final int mUserData;

    public ThreeGPPNetworkElement(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        super(infoID);
        this.mPlmns = new ArrayList();
        this.mUserData = payload.get() & 255;
        int length = payload.get() & 255;
        if (length > payload.remaining()) {
            throw new ProtocolException("Runt payload");
        }
        while (payload.hasRemaining()) {
            CellularNetwork network = CellularNetwork.buildCellularNetwork(payload);
            if (network != null) {
                this.mPlmns.add(network);
            }
        }
    }

    public int getUserData() {
        return this.mUserData;
    }

    public List<CellularNetwork> getPlmns() {
        return Collections.unmodifiableList(this.mPlmns);
    }

    public String toString() {
        return "ThreeGPPNetwork{mUserData=" + this.mUserData + ", mPlmns=" + this.mPlmns + '}';
    }
}
