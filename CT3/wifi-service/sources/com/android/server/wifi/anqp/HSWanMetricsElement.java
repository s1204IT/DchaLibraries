package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;
import java.net.ProtocolException;
import java.nio.ByteBuffer;

public class HSWanMetricsElement extends ANQPElement {
    private final boolean mCapped;
    private final int mDlLoad;
    private final long mDlSpeed;
    private final int mLMD;
    private final LinkStatus mStatus;
    private final boolean mSymmetric;
    private final int mUlLoad;
    private final long mUlSpeed;

    public enum LinkStatus {
        Reserved,
        Up,
        Down,
        Test;

        public static LinkStatus[] valuesCustom() {
            return values();
        }
    }

    public HSWanMetricsElement(Constants.ANQPElementType infoID, ByteBuffer payload) throws ProtocolException {
        super(infoID);
        if (payload.remaining() != 13) {
            throw new ProtocolException("Bad WAN metrics length: " + payload.remaining());
        }
        int status = payload.get() & 255;
        this.mStatus = LinkStatus.valuesCustom()[status & 3];
        this.mSymmetric = (status & 4) != 0;
        this.mCapped = (status & 8) != 0;
        this.mDlSpeed = ((long) payload.getInt()) & Constants.INT_MASK;
        this.mUlSpeed = ((long) payload.getInt()) & Constants.INT_MASK;
        this.mDlLoad = payload.get() & 255;
        this.mUlLoad = payload.get() & 255;
        this.mLMD = payload.getShort() & 65535;
    }

    public LinkStatus getStatus() {
        return this.mStatus;
    }

    public boolean isSymmetric() {
        return this.mSymmetric;
    }

    public boolean isCapped() {
        return this.mCapped;
    }

    public long getDlSpeed() {
        return this.mDlSpeed;
    }

    public long getUlSpeed() {
        return this.mUlSpeed;
    }

    public int getDlLoad() {
        return this.mDlLoad;
    }

    public int getUlLoad() {
        return this.mUlLoad;
    }

    public int getLMD() {
        return this.mLMD;
    }

    public String toString() {
        return String.format("HSWanMetrics{mStatus=%s, mSymmetric=%s, mCapped=%s, mDlSpeed=%d, mUlSpeed=%d, mDlLoad=%f, mUlLoad=%f, mLMD=%d}", this.mStatus, Boolean.valueOf(this.mSymmetric), Boolean.valueOf(this.mCapped), Long.valueOf(this.mDlSpeed), Long.valueOf(this.mUlSpeed), Double.valueOf((((double) this.mDlLoad) * 100.0d) / 256.0d), Double.valueOf((((double) this.mUlLoad) * 100.0d) / 256.0d), Integer.valueOf(this.mLMD));
    }
}
