package android.hardware.display;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.Objects;

@SystemApi
public final class BrightnessConfiguration implements Parcelable {
    public static final Parcelable.Creator<BrightnessConfiguration> CREATOR = new Parcelable.Creator<BrightnessConfiguration>() {
        @Override
        public BrightnessConfiguration createFromParcel(Parcel parcel) {
            Builder builder = new Builder(parcel.createFloatArray(), parcel.createFloatArray());
            builder.setDescription(parcel.readString());
            return builder.build();
        }

        @Override
        public BrightnessConfiguration[] newArray(int i) {
            return new BrightnessConfiguration[i];
        }
    };
    private final String mDescription;
    private final float[] mLux;
    private final float[] mNits;

    private BrightnessConfiguration(float[] fArr, float[] fArr2, String str) {
        this.mLux = fArr;
        this.mNits = fArr2;
        this.mDescription = str;
    }

    public Pair<float[], float[]> getCurve() {
        return Pair.create(Arrays.copyOf(this.mLux, this.mLux.length), Arrays.copyOf(this.mNits, this.mNits.length));
    }

    public String getDescription() {
        return this.mDescription;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloatArray(this.mLux);
        parcel.writeFloatArray(this.mNits);
        parcel.writeString(this.mDescription);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("BrightnessConfiguration{[");
        int length = this.mLux.length;
        for (int i = 0; i < length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("(");
            sb.append(this.mLux[i]);
            sb.append(", ");
            sb.append(this.mNits[i]);
            sb.append(")");
        }
        sb.append("], '");
        if (this.mDescription != null) {
            sb.append(this.mDescription);
        }
        sb.append("'}");
        return sb.toString();
    }

    public int hashCode() {
        int iHashCode = ((Arrays.hashCode(this.mLux) + 31) * 31) + Arrays.hashCode(this.mNits);
        if (this.mDescription != null) {
            return (iHashCode * 31) + this.mDescription.hashCode();
        }
        return iHashCode;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BrightnessConfiguration)) {
            return false;
        }
        BrightnessConfiguration brightnessConfiguration = (BrightnessConfiguration) obj;
        return Arrays.equals(this.mLux, brightnessConfiguration.mLux) && Arrays.equals(this.mNits, brightnessConfiguration.mNits) && Objects.equals(this.mDescription, brightnessConfiguration.mDescription);
    }

    public static class Builder {
        private float[] mCurveLux;
        private float[] mCurveNits;
        private String mDescription;

        public Builder() {
        }

        public Builder(float[] fArr, float[] fArr2) {
            setCurve(fArr, fArr2);
        }

        public Builder setCurve(float[] fArr, float[] fArr2) {
            Preconditions.checkNotNull(fArr);
            Preconditions.checkNotNull(fArr2);
            if (fArr.length == 0 || fArr2.length == 0) {
                throw new IllegalArgumentException("Lux and nits arrays must not be empty");
            }
            if (fArr.length != fArr2.length) {
                throw new IllegalArgumentException("Lux and nits arrays must be the same length");
            }
            if (fArr[0] != 0.0f) {
                throw new IllegalArgumentException("Initial control point must be for 0 lux");
            }
            Preconditions.checkArrayElementsInRange(fArr, 0.0f, Float.MAX_VALUE, "lux");
            Preconditions.checkArrayElementsInRange(fArr2, 0.0f, Float.MAX_VALUE, "nits");
            checkMonotonic(fArr, true, "lux");
            checkMonotonic(fArr2, false, "nits");
            this.mCurveLux = fArr;
            this.mCurveNits = fArr2;
            return this;
        }

        public Builder setDescription(String str) {
            this.mDescription = str;
            return this;
        }

        public BrightnessConfiguration build() {
            if (this.mCurveLux == null || this.mCurveNits == null) {
                throw new IllegalStateException("A curve must be set!");
            }
            return new BrightnessConfiguration(this.mCurveLux, this.mCurveNits, this.mDescription);
        }

        private static void checkMonotonic(float[] fArr, boolean z, String str) {
            if (fArr.length <= 1) {
                return;
            }
            float f = fArr[0];
            for (int i = 1; i < fArr.length; i++) {
                if (f > fArr[i] || (f == fArr[i] && z)) {
                    throw new IllegalArgumentException(str + " values must be " + (z ? "strictly increasing" : "monotonic"));
                }
                f = fArr[i];
            }
        }
    }
}
