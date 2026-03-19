package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DomainNameElement extends ANQPElement {
    private final List<String> mDomains;

    @VisibleForTesting
    public DomainNameElement(List<String> list) {
        super(Constants.ANQPElementType.ANQPDomName);
        this.mDomains = list;
    }

    public static DomainNameElement parse(ByteBuffer byteBuffer) {
        ArrayList arrayList = new ArrayList();
        while (byteBuffer.hasRemaining()) {
            arrayList.add(ByteBufferReader.readStringWithByteLength(byteBuffer, StandardCharsets.ISO_8859_1));
        }
        return new DomainNameElement(arrayList);
    }

    public List<String> getDomains() {
        return Collections.unmodifiableList(this.mDomains);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DomainNameElement)) {
            return false;
        }
        return this.mDomains.equals(((DomainNameElement) obj).mDomains);
    }

    public int hashCode() {
        return this.mDomains.hashCode();
    }

    public String toString() {
        return "DomainName{mDomains=" + this.mDomains + '}';
    }
}
