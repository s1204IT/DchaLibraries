package com.android.server.wifi.hotspot2.anqp;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class HSWanMetricsElement extends ANQPElement {

    @VisibleForTesting
    public static final int AT_CAPACITY_MASK = 8;

    @VisibleForTesting
    public static final int EXPECTED_BUFFER_SIZE = 13;
    public static final int LINK_STATUS_DOWN = 2;

    @VisibleForTesting
    public static final int LINK_STATUS_MASK = 3;
    public static final int LINK_STATUS_RESERVED = 0;
    public static final int LINK_STATUS_TEST = 3;
    public static final int LINK_STATUS_UP = 1;
    private static final int MAX_LOAD = 256;

    @VisibleForTesting
    public static final int SYMMETRIC_LINK_MASK = 4;
    private final boolean mCapped;
    private final int mDownlinkLoad;
    private final long mDownlinkSpeed;
    private final int mLMD;
    private final int mStatus;
    private final boolean mSymmetric;
    private final int mUplinkLoad;
    private final long mUplinkSpeed;

    @VisibleForTesting
    public HSWanMetricsElement(int i, boolean z, boolean z2, long j, long j2, int i2, int i3, int i4) {
        super(Constants.ANQPElementType.HSWANMetrics);
        this.mStatus = i;
        this.mSymmetric = z;
        this.mCapped = z2;
        this.mDownlinkSpeed = j;
        this.mUplinkSpeed = j2;
        this.mDownlinkLoad = i2;
        this.mUplinkLoad = i3;
        this.mLMD = i4;
    }

    public static HSWanMetricsElement parse(ByteBuffer byteBuffer) throws ProtocolException {
        if (byteBuffer.remaining() != 13) {
            throw new ProtocolException("Unexpected buffer size: " + byteBuffer.remaining());
        }
        int i = byteBuffer.get() & 255;
        return new HSWanMetricsElement(i & 3, (i & 4) != 0, (i & 8) != 0, ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 4) & Constants.INT_MASK, Constants.INT_MASK & ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 4), byteBuffer.get() & 255, byteBuffer.get() & 255, ((int) ByteBufferReader.readInteger(byteBuffer, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK);
    }

    public int getStatus() {
        return this.mStatus;
    }

    public boolean isSymmetric() {
        return this.mSymmetric;
    }

    public boolean isCapped() {
        return this.mCapped;
    }

    public long getDownlinkSpeed() {
        return this.mDownlinkSpeed;
    }

    public long getUplinkSpeed() {
        return this.mUplinkSpeed;
    }

    public int getDownlinkLoad() {
        return this.mDownlinkLoad;
    }

    public int getUplinkLoad() {
        return this.mUplinkLoad;
    }

    public int getLMD() {
        return this.mLMD;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HSWanMetricsElement)) {
            return false;
        }
        HSWanMetricsElement hSWanMetricsElement = (HSWanMetricsElement) obj;
        return this.mStatus == hSWanMetricsElement.mStatus && this.mSymmetric == hSWanMetricsElement.mSymmetric && this.mCapped == hSWanMetricsElement.mCapped && this.mDownlinkSpeed == hSWanMetricsElement.mDownlinkSpeed && this.mUplinkSpeed == hSWanMetricsElement.mUplinkSpeed && this.mDownlinkLoad == hSWanMetricsElement.mDownlinkLoad && this.mUplinkLoad == hSWanMetricsElement.mUplinkLoad && this.mLMD == hSWanMetricsElement.mLMD;
    }

    public int hashCode() {
        return (int) (((long) this.mStatus) + this.mDownlinkSpeed + this.mUplinkSpeed + ((long) this.mDownlinkLoad) + ((long) this.mUplinkLoad) + ((long) this.mLMD));
    }

    public String toString() {
        return String.format("HSWanMetrics{mStatus=%s, mSymmetric=%s, mCapped=%s, mDlSpeed=%d, mUlSpeed=%d, mDlLoad=%f, mUlLoad=%f, mLMD=%d}", Integer.valueOf(this.mStatus), Boolean.valueOf(this.mSymmetric), Boolean.valueOf(this.mCapped), Long.valueOf(this.mDownlinkSpeed), Long.valueOf(this.mUplinkSpeed), Double.valueOf((((double) this.mDownlinkLoad) * 100.0d) / 256.0d), Double.valueOf((((double) this.mUplinkLoad) * 100.0d) / 256.0d), Integer.valueOf(this.mLMD));
    }
}
