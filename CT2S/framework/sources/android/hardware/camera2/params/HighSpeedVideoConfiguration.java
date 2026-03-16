package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Range;
import android.util.Size;
import com.android.internal.util.Preconditions;

public final class HighSpeedVideoConfiguration {
    private final int mFpsMax;
    private final int mFpsMin;
    private final Range<Integer> mFpsRange;
    private final int mHeight;
    private final Size mSize;
    private final int mWidth;

    public HighSpeedVideoConfiguration(int width, int height, int fpsMin, int fpsMax) {
        if (fpsMax < 60) {
            throw new IllegalArgumentException("fpsMax must be at least 60");
        }
        this.mFpsMax = fpsMax;
        this.mWidth = Preconditions.checkArgumentPositive(width, "width must be positive");
        this.mHeight = Preconditions.checkArgumentPositive(height, "height must be positive");
        this.mFpsMin = Preconditions.checkArgumentPositive(fpsMin, "fpsMin must be positive");
        this.mSize = new Size(this.mWidth, this.mHeight);
        this.mFpsRange = new Range<>(Integer.valueOf(this.mFpsMin), Integer.valueOf(this.mFpsMax));
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getFpsMin() {
        return this.mFpsMin;
    }

    public int getFpsMax() {
        return this.mFpsMax;
    }

    public Size getSize() {
        return this.mSize;
    }

    public Range<Integer> getFpsRange() {
        return this.mFpsRange;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HighSpeedVideoConfiguration)) {
            return false;
        }
        HighSpeedVideoConfiguration other = (HighSpeedVideoConfiguration) obj;
        return this.mWidth == other.mWidth && this.mHeight == other.mHeight && this.mFpsMin == other.mFpsMin && this.mFpsMax == other.mFpsMax;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCode(this.mWidth, this.mHeight, this.mFpsMin, this.mFpsMax);
    }
}
