package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VenueNameElement extends ANQPElement {

    @VisibleForTesting
    public static final int MAXIMUM_VENUE_NAME_LENGTH = 252;

    @VisibleForTesting
    public static final int VENUE_INFO_LENGTH = 2;
    private final List<I18Name> mNames;

    @VisibleForTesting
    public VenueNameElement(List<I18Name> list) {
        super(Constants.ANQPElementType.ANQPVenueName);
        this.mNames = list;
    }

    public static VenueNameElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        for (int i = 0; i < 2; i++) {
            byteBuffer.get();
        }
        ArrayList arrayList = new ArrayList();
        while (byteBuffer.hasRemaining()) {
            I18Name i18Name = I18Name.parse(byteBuffer);
            int length = i18Name.getText().getBytes(StandardCharsets.UTF_8).length;
            if (length > 252) {
                throw new ProtocolException("Venue Name exceeds the maximum allowed " + length);
            }
            arrayList.add(i18Name);
        }
        return new VenueNameElement(arrayList);
    }

    public List<I18Name> getNames() {
        return Collections.unmodifiableList(this.mNames);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VenueNameElement)) {
            return false;
        }
        return this.mNames.equals(((VenueNameElement) obj).mNames);
    }

    public int hashCode() {
        return this.mNames.hashCode();
    }

    public String toString() {
        return "VenueName{ mNames=" + this.mNames + "}";
    }
}
