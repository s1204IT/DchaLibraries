package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HSFriendlyNameElement extends ANQPElement {

    @VisibleForTesting
    public static final int MAXIMUM_OPERATOR_NAME_LENGTH = 252;
    private final List<I18Name> mNames;

    @VisibleForTesting
    public HSFriendlyNameElement(List<I18Name> list) {
        super(Constants.ANQPElementType.HSFriendlyName);
        this.mNames = list;
    }

    public static HSFriendlyNameElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        ArrayList arrayList = new ArrayList();
        while (byteBuffer.hasRemaining()) {
            I18Name i18Name = I18Name.parse(byteBuffer);
            int length = i18Name.getText().getBytes(StandardCharsets.UTF_8).length;
            if (length > 252) {
                throw new ProtocolException("Operator Name exceeds the maximum allowed " + length);
            }
            arrayList.add(i18Name);
        }
        return new HSFriendlyNameElement(arrayList);
    }

    public List<I18Name> getNames() {
        return Collections.unmodifiableList(this.mNames);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HSFriendlyNameElement)) {
            return false;
        }
        return this.mNames.equals(((HSFriendlyNameElement) obj).mNames);
    }

    public int hashCode() {
        return this.mNames.hashCode();
    }

    public String toString() {
        return "HSFriendlyName{mNames=" + this.mNames + '}';
    }
}
