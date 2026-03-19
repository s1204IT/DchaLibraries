package com.android.server.wifi.hotspot2.anqp;

import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class HSIconFileElement extends ANQPElement {
    public static final int STATUS_CODE_FILE_NOT_FOUND = 1;
    public static final int STATUS_CODE_SUCCESS = 0;
    public static final int STATUS_CODE_UNSPECIFIED_ERROR = 2;
    private static final String TAG = "HSIconFileElement";
    private final byte[] mIconData;
    private final String mIconType;
    private final int mStatusCode;

    @VisibleForTesting
    public HSIconFileElement(int i, String str, byte[] bArr) {
        super(Constants.ANQPElementType.HSIconFile);
        this.mStatusCode = i;
        this.mIconType = str;
        this.mIconData = bArr;
    }

    public static HSIconFileElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        int i = byteBuffer.get() & 255;
        if (i != 0) {
            Log.e(TAG, "Icon file download failed: " + i);
            return new HSIconFileElement(i, null, null);
        }
        String stringWithByteLength = ByteBufferReader.readStringWithByteLength(byteBuffer, StandardCharsets.US_ASCII);
        byte[] bArr = new byte[((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK];
        byteBuffer.get(bArr);
        return new HSIconFileElement(i, stringWithByteLength, bArr);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HSIconFileElement)) {
            return false;
        }
        HSIconFileElement hSIconFileElement = (HSIconFileElement) obj;
        return this.mStatusCode == hSIconFileElement.mStatusCode && TextUtils.equals(this.mIconType, hSIconFileElement.mIconType) && Arrays.equals(this.mIconData, hSIconFileElement.mIconData);
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mStatusCode), this.mIconType, Integer.valueOf(Arrays.hashCode(this.mIconData)));
    }

    public String toString() {
        return "HSIconFileElement{mStatusCode=" + this.mStatusCode + "mIconType=" + this.mIconType + "}";
    }
}
