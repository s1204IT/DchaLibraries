package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HSConnectionCapabilityElement extends ANQPElement {
    private final List<ProtocolTuple> mStatusList;

    public enum ProtoStatus {
        Closed,
        Open,
        Unknown;

        public static ProtoStatus[] valuesCustom() {
            return values();
        }
    }

    public static class ProtocolTuple {
        private final int mPort;
        private final int mProtocol;
        private final ProtoStatus mStatus;

        ProtocolTuple(ByteBuffer payload, ProtocolTuple protocolTuple) {
            this(payload);
        }

        private ProtocolTuple(ByteBuffer payload) throws ProtocolException {
            ProtoStatus protoStatus;
            if (payload.remaining() < 4) {
                throw new ProtocolException("Runt protocol tuple: " + payload.remaining());
            }
            this.mProtocol = payload.get() & 255;
            this.mPort = payload.getShort() & 65535;
            int statusNumber = payload.get() & 255;
            if (statusNumber < ProtoStatus.valuesCustom().length) {
                protoStatus = ProtoStatus.valuesCustom()[statusNumber];
            } else {
                protoStatus = null;
            }
            this.mStatus = protoStatus;
        }

        public int getProtocol() {
            return this.mProtocol;
        }

        public int getPort() {
            return this.mPort;
        }

        public ProtoStatus getStatus() {
            return this.mStatus;
        }

        public String toString() {
            return "ProtocolTuple{mProtocol=" + this.mProtocol + ", mPort=" + this.mPort + ", mStatus=" + this.mStatus + '}';
        }
    }

    public HSConnectionCapabilityElement(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        super(infoID);
        this.mStatusList = new ArrayList();
        while (payload.hasRemaining()) {
            this.mStatusList.add(new ProtocolTuple(payload, null));
        }
    }

    public List<ProtocolTuple> getStatusList() {
        return Collections.unmodifiableList(this.mStatusList);
    }

    public String toString() {
        return "HSConnectionCapability{mStatusList=" + this.mStatusList + '}';
    }
}
