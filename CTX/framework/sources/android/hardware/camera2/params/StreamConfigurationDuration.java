package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Size;
import com.android.internal.util.Preconditions;

public final class StreamConfigurationDuration {
    private final long mDurationNs;
    private final int mFormat;
    private final int mHeight;
    private final int mWidth;

    public StreamConfigurationDuration(int i, int i2, int i3, long j) {
        this.mFormat = StreamConfigurationMap.checkArgumentFormatInternal(i);
        this.mWidth = Preconditions.checkArgumentPositive(i2, "width must be positive");
        this.mHeight = Preconditions.checkArgumentPositive(i3, "height must be positive");
        this.mDurationNs = Preconditions.checkArgumentNonnegative(j, "durationNs must be non-negative");
    }

    public final int getFormat() {
        return this.mFormat;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public Size getSize() {
        return new Size(this.mWidth, this.mHeight);
    }

    public long getDuration() {
        return this.mDurationNs;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StreamConfigurationDuration)) {
            return false;
        }
        StreamConfigurationDuration streamConfigurationDuration = (StreamConfigurationDuration) obj;
        if (this.mFormat != streamConfigurationDuration.mFormat || this.mWidth != streamConfigurationDuration.mWidth || this.mHeight != streamConfigurationDuration.mHeight || this.mDurationNs != streamConfigurationDuration.mDurationNs) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCode(this.mFormat, this.mWidth, this.mHeight, (int) this.mDurationNs, (int) (this.mDurationNs >>> 32));
    }
}
