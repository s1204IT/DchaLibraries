package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HSConnectionCapabilityElement extends ANQPElement {
    private final List<ProtocolPortTuple> mStatusList;

    @VisibleForTesting
    public HSConnectionCapabilityElement(List<ProtocolPortTuple> list) {
        super(Constants.ANQPElementType.HSConnCapability);
        this.mStatusList = list;
    }

    public static HSConnectionCapabilityElement parse(ByteBuffer byteBuffer) {
        ArrayList arrayList = new ArrayList();
        while (byteBuffer.hasRemaining()) {
            arrayList.add(ProtocolPortTuple.parse(byteBuffer));
        }
        return new HSConnectionCapabilityElement(arrayList);
    }

    public List<ProtocolPortTuple> getStatusList() {
        return Collections.unmodifiableList(this.mStatusList);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HSConnectionCapabilityElement)) {
            return false;
        }
        return this.mStatusList.equals(((HSConnectionCapabilityElement) obj).mStatusList);
    }

    public int hashCode() {
        return this.mStatusList.hashCode();
    }

    public String toString() {
        return "HSConnectionCapability{mStatusList=" + this.mStatusList + '}';
    }
}
