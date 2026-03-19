package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NAIRealmElement extends ANQPElement {
    private final List<NAIRealmData> mRealmDataList;

    @VisibleForTesting
    public NAIRealmElement(List<NAIRealmData> list) {
        super(Constants.ANQPElementType.ANQPNAIRealm);
        this.mRealmDataList = list;
    }

    public static NAIRealmElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        ArrayList arrayList = new ArrayList();
        if (byteBuffer.hasRemaining()) {
            for (int integer = ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK; integer > 0; integer--) {
                arrayList.add(NAIRealmData.parse(byteBuffer));
            }
        }
        return new NAIRealmElement(arrayList);
    }

    public List<NAIRealmData> getRealmDataList() {
        return Collections.unmodifiableList(this.mRealmDataList);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NAIRealmElement)) {
            return false;
        }
        return this.mRealmDataList.equals(((NAIRealmElement) obj).mRealmDataList);
    }

    public int hashCode() {
        return this.mRealmDataList.hashCode();
    }

    public String toString() {
        return "NAIRealmElement{mRealmDataList=" + this.mRealmDataList + "}";
    }
}
