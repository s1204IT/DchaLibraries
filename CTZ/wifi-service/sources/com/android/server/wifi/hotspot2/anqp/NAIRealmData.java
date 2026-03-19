package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.eap.EAPMethod;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NAIRealmData {

    @VisibleForTesting
    public static final int NAI_ENCODING_UTF8_MASK = 1;

    @VisibleForTesting
    public static final String NAI_REALM_STRING_SEPARATOR = ";";
    private final List<EAPMethod> mEAPMethods;
    private final List<String> mRealms;

    @VisibleForTesting
    public NAIRealmData(List<String> list, List<EAPMethod> list2) {
        this.mRealms = list;
        this.mEAPMethods = list2;
    }

    public static NAIRealmData parse(ByteBuffer byteBuffer) throws ProtocolException {
        int integer = ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK;
        if (integer > byteBuffer.remaining()) {
            throw new ProtocolException("Invalid data length: " + integer);
        }
        List listAsList = Arrays.asList(ByteBufferReader.readStringWithByteLength(byteBuffer, (byteBuffer.get() & 1) != 0 ? StandardCharsets.UTF_8 : StandardCharsets.US_ASCII).split(NAI_REALM_STRING_SEPARATOR));
        ArrayList arrayList = new ArrayList();
        for (int i = byteBuffer.get() & 255; i > 0; i--) {
            arrayList.add(EAPMethod.parse(byteBuffer));
        }
        return new NAIRealmData(listAsList, arrayList);
    }

    public List<String> getRealms() {
        return Collections.unmodifiableList(this.mRealms);
    }

    public List<EAPMethod> getEAPMethods() {
        return Collections.unmodifiableList(this.mEAPMethods);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NAIRealmData)) {
            return false;
        }
        NAIRealmData nAIRealmData = (NAIRealmData) obj;
        return this.mRealms.equals(nAIRealmData.mRealms) && this.mEAPMethods.equals(nAIRealmData.mEAPMethods);
    }

    public int hashCode() {
        return (this.mRealms.hashCode() * 31) + this.mEAPMethods.hashCode();
    }

    public String toString() {
        return "NAIRealmElement{mRealms=" + this.mRealms + " mEAPMethods=" + this.mEAPMethods + "}";
    }
}
