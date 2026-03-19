package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;

public final class InputConfiguration {
    private final int mFormat;
    private final int mHeight;
    private final int mWidth;

    public InputConfiguration(int i, int i2, int i3) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mFormat = i3;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getFormat() {
        return this.mFormat;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InputConfiguration)) {
            return false;
        }
        InputConfiguration inputConfiguration = (InputConfiguration) obj;
        return inputConfiguration.getWidth() == this.mWidth && inputConfiguration.getHeight() == this.mHeight && inputConfiguration.getFormat() == this.mFormat;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCode(this.mWidth, this.mHeight, this.mFormat);
    }

    public String toString() {
        return String.format("InputConfiguration(w:%d, h:%d, format:%d)", Integer.valueOf(this.mWidth), Integer.valueOf(this.mHeight), Integer.valueOf(this.mFormat));
    }
}
