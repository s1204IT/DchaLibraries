package android.os;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;
import android.util.MathUtils;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Arrays;

public abstract class VibrationEffect implements Parcelable {
    public static final int DEFAULT_AMPLITUDE = -1;
    public static final int EFFECT_CLICK = 0;
    public static final int EFFECT_DOUBLE_CLICK = 1;
    public static final int EFFECT_HEAVY_CLICK = 5;
    public static final int EFFECT_POP = 4;
    public static final int EFFECT_THUD = 3;
    public static final int EFFECT_TICK = 2;
    public static final int MAX_AMPLITUDE = 255;
    private static final int PARCEL_TOKEN_EFFECT = 3;
    private static final int PARCEL_TOKEN_ONE_SHOT = 1;
    private static final int PARCEL_TOKEN_WAVEFORM = 2;

    @VisibleForTesting
    public static final int[] RINGTONES = {6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    public static final Parcelable.Creator<VibrationEffect> CREATOR = new Parcelable.Creator<VibrationEffect>() {
        @Override
        public VibrationEffect createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            if (i == 1) {
                return new OneShot(parcel);
            }
            if (i == 2) {
                return new Waveform(parcel);
            }
            if (i == 3) {
                return new Prebaked(parcel);
            }
            throw new IllegalStateException("Unexpected vibration event type token in parcel.");
        }

        @Override
        public VibrationEffect[] newArray(int i) {
            return new VibrationEffect[i];
        }
    };

    public abstract long getDuration();

    public abstract void validate();

    public static VibrationEffect createOneShot(long j, int i) {
        OneShot oneShot = new OneShot(j, i);
        oneShot.validate();
        return oneShot;
    }

    public static VibrationEffect createWaveform(long[] jArr, int i) {
        int[] iArr = new int[jArr.length];
        for (int i2 = 0; i2 < jArr.length / 2; i2++) {
            iArr[(i2 * 2) + 1] = -1;
        }
        return createWaveform(jArr, iArr, i);
    }

    public static VibrationEffect createWaveform(long[] jArr, int[] iArr, int i) {
        Waveform waveform = new Waveform(jArr, iArr, i);
        waveform.validate();
        return waveform;
    }

    public static VibrationEffect get(int i) {
        return get(i, true);
    }

    public static VibrationEffect get(int i, boolean z) {
        Prebaked prebaked = new Prebaked(i, z);
        prebaked.validate();
        return prebaked;
    }

    public static VibrationEffect get(Uri uri, Context context) {
        Uri uriUncanonicalize;
        String[] stringArray = context.getResources().getStringArray(R.array.config_ringtoneEffectUris);
        for (int i = 0; i < stringArray.length && i < RINGTONES.length; i++) {
            if (stringArray[i] != null && (uriUncanonicalize = context.getContentResolver().uncanonicalize(Uri.parse(stringArray[i]))) != null && uriUncanonicalize.equals(uri)) {
                return get(RINGTONES[i]);
            }
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected static int scale(int i, float f, int i2) {
        return (int) (MathUtils.pow(i / 255.0f, f) * i2);
    }

    public static class OneShot extends VibrationEffect implements Parcelable {
        public static final Parcelable.Creator<OneShot> CREATOR = new Parcelable.Creator<OneShot>() {
            @Override
            public OneShot createFromParcel(Parcel parcel) {
                parcel.readInt();
                return new OneShot(parcel);
            }

            @Override
            public OneShot[] newArray(int i) {
                return new OneShot[i];
            }
        };
        private final int mAmplitude;
        private final long mDuration;

        public OneShot(Parcel parcel) {
            this.mDuration = parcel.readLong();
            this.mAmplitude = parcel.readInt();
        }

        public OneShot(long j, int i) {
            this.mDuration = j;
            this.mAmplitude = i;
        }

        @Override
        public long getDuration() {
            return this.mDuration;
        }

        public int getAmplitude() {
            return this.mAmplitude;
        }

        public VibrationEffect scale(float f, int i) {
            return new OneShot(this.mDuration, scale(this.mAmplitude, f, i));
        }

        public OneShot resolve(int i) {
            if (i > 255 || i < 0) {
                throw new IllegalArgumentException("Amplitude is negative or greater than MAX_AMPLITUDE");
            }
            if (this.mAmplitude == -1) {
                return new OneShot(this.mDuration, i);
            }
            return this;
        }

        @Override
        public void validate() {
            if (this.mAmplitude < -1 || this.mAmplitude == 0 || this.mAmplitude > 255) {
                throw new IllegalArgumentException("amplitude must either be DEFAULT_AMPLITUDE, or between 1 and 255 inclusive (amplitude=" + this.mAmplitude + ")");
            }
            if (this.mDuration <= 0) {
                throw new IllegalArgumentException("duration must be positive (duration=" + this.mDuration + ")");
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof OneShot)) {
                return false;
            }
            OneShot oneShot = (OneShot) obj;
            return oneShot.mDuration == this.mDuration && oneShot.mAmplitude == this.mAmplitude;
        }

        public int hashCode() {
            return 17 + (((int) this.mDuration) * 37) + (37 * this.mAmplitude);
        }

        public String toString() {
            return "OneShot{mDuration=" + this.mDuration + ", mAmplitude=" + this.mAmplitude + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(1);
            parcel.writeLong(this.mDuration);
            parcel.writeInt(this.mAmplitude);
        }
    }

    public static class Waveform extends VibrationEffect implements Parcelable {
        public static final Parcelable.Creator<Waveform> CREATOR = new Parcelable.Creator<Waveform>() {
            @Override
            public Waveform createFromParcel(Parcel parcel) {
                parcel.readInt();
                return new Waveform(parcel);
            }

            @Override
            public Waveform[] newArray(int i) {
                return new Waveform[i];
            }
        };
        private final int[] mAmplitudes;
        private final int mRepeat;
        private final long[] mTimings;

        public Waveform(Parcel parcel) {
            this(parcel.createLongArray(), parcel.createIntArray(), parcel.readInt());
        }

        public Waveform(long[] jArr, int[] iArr, int i) {
            this.mTimings = new long[jArr.length];
            System.arraycopy(jArr, 0, this.mTimings, 0, jArr.length);
            this.mAmplitudes = new int[iArr.length];
            System.arraycopy(iArr, 0, this.mAmplitudes, 0, iArr.length);
            this.mRepeat = i;
        }

        public long[] getTimings() {
            return this.mTimings;
        }

        public int[] getAmplitudes() {
            return this.mAmplitudes;
        }

        public int getRepeatIndex() {
            return this.mRepeat;
        }

        @Override
        public long getDuration() {
            if (this.mRepeat >= 0) {
                return Long.MAX_VALUE;
            }
            long j = 0;
            for (long j2 : this.mTimings) {
                j += j2;
            }
            return j;
        }

        public VibrationEffect scale(float f, int i) {
            if (f == 1.0f && i == 255) {
                return new Waveform(this.mTimings, this.mAmplitudes, this.mRepeat);
            }
            int[] iArrCopyOf = Arrays.copyOf(this.mAmplitudes, this.mAmplitudes.length);
            for (int i2 = 0; i2 < iArrCopyOf.length; i2++) {
                iArrCopyOf[i2] = scale(iArrCopyOf[i2], f, i);
            }
            return new Waveform(this.mTimings, iArrCopyOf, this.mRepeat);
        }

        public Waveform resolve(int i) {
            if (i > 255 || i < 0) {
                throw new IllegalArgumentException("Amplitude is negative or greater than MAX_AMPLITUDE");
            }
            int[] iArrCopyOf = Arrays.copyOf(this.mAmplitudes, this.mAmplitudes.length);
            for (int i2 = 0; i2 < iArrCopyOf.length; i2++) {
                if (iArrCopyOf[i2] == -1) {
                    iArrCopyOf[i2] = i;
                }
            }
            return new Waveform(this.mTimings, iArrCopyOf, this.mRepeat);
        }

        @Override
        public void validate() {
            if (this.mTimings.length != this.mAmplitudes.length) {
                throw new IllegalArgumentException("timing and amplitude arrays must be of equal length (timings.length=" + this.mTimings.length + ", amplitudes.length=" + this.mAmplitudes.length + ")");
            }
            if (!hasNonZeroEntry(this.mTimings)) {
                throw new IllegalArgumentException("at least one timing must be non-zero (timings=" + Arrays.toString(this.mTimings) + ")");
            }
            for (long j : this.mTimings) {
                if (j < 0) {
                    throw new IllegalArgumentException("timings must all be >= 0 (timings=" + Arrays.toString(this.mTimings) + ")");
                }
            }
            for (int i : this.mAmplitudes) {
                if (i < -1 || i > 255) {
                    throw new IllegalArgumentException("amplitudes must all be DEFAULT_AMPLITUDE or between 0 and 255 (amplitudes=" + Arrays.toString(this.mAmplitudes) + ")");
                }
            }
            if (this.mRepeat < -1 || this.mRepeat >= this.mTimings.length) {
                throw new IllegalArgumentException("repeat index must be within the bounds of the timings array (timings.length=" + this.mTimings.length + ", index=" + this.mRepeat + ")");
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Waveform)) {
                return false;
            }
            Waveform waveform = (Waveform) obj;
            return Arrays.equals(this.mTimings, waveform.mTimings) && Arrays.equals(this.mAmplitudes, waveform.mAmplitudes) && this.mRepeat == waveform.mRepeat;
        }

        public int hashCode() {
            return 17 + (Arrays.hashCode(this.mTimings) * 37) + (Arrays.hashCode(this.mAmplitudes) * 37) + (37 * this.mRepeat);
        }

        public String toString() {
            return "Waveform{mTimings=" + Arrays.toString(this.mTimings) + ", mAmplitudes=" + Arrays.toString(this.mAmplitudes) + ", mRepeat=" + this.mRepeat + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(2);
            parcel.writeLongArray(this.mTimings);
            parcel.writeIntArray(this.mAmplitudes);
            parcel.writeInt(this.mRepeat);
        }

        private static boolean hasNonZeroEntry(long[] jArr) {
            for (long j : jArr) {
                if (j != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class Prebaked extends VibrationEffect implements Parcelable {
        public static final Parcelable.Creator<Prebaked> CREATOR = new Parcelable.Creator<Prebaked>() {
            @Override
            public Prebaked createFromParcel(Parcel parcel) {
                parcel.readInt();
                return new Prebaked(parcel);
            }

            @Override
            public Prebaked[] newArray(int i) {
                return new Prebaked[i];
            }
        };
        private final int mEffectId;
        private int mEffectStrength;
        private final boolean mFallback;

        public Prebaked(Parcel parcel) {
            this(parcel.readInt(), parcel.readByte() != 0);
            this.mEffectStrength = parcel.readInt();
        }

        public Prebaked(int i, boolean z) {
            this.mEffectId = i;
            this.mFallback = z;
            this.mEffectStrength = 1;
        }

        public int getId() {
            return this.mEffectId;
        }

        public boolean shouldFallback() {
            return this.mFallback;
        }

        @Override
        public long getDuration() {
            return -1L;
        }

        public void setEffectStrength(int i) {
            if (!isValidEffectStrength(i)) {
                throw new IllegalArgumentException("Invalid effect strength: " + i);
            }
            this.mEffectStrength = i;
        }

        public int getEffectStrength() {
            return this.mEffectStrength;
        }

        private static boolean isValidEffectStrength(int i) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void validate() {
            switch (this.mEffectId) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    break;
                default:
                    if (this.mEffectId < RINGTONES[0] || this.mEffectId > RINGTONES[RINGTONES.length - 1]) {
                        throw new IllegalArgumentException("Unknown prebaked effect type (value=" + this.mEffectId + ")");
                    }
                    break;
            }
            if (!isValidEffectStrength(this.mEffectStrength)) {
                throw new IllegalArgumentException("Unknown prebaked effect strength (value=" + this.mEffectStrength + ")");
            }
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Prebaked)) {
                return false;
            }
            Prebaked prebaked = (Prebaked) obj;
            return this.mEffectId == prebaked.mEffectId && this.mFallback == prebaked.mFallback && this.mEffectStrength == prebaked.mEffectStrength;
        }

        public int hashCode() {
            return 17 + (this.mEffectId * 37) + (37 * this.mEffectStrength);
        }

        public String toString() {
            return "Prebaked{mEffectId=" + this.mEffectId + ", mEffectStrength=" + this.mEffectStrength + ", mFallback=" + this.mFallback + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(3);
            parcel.writeInt(this.mEffectId);
            parcel.writeByte(this.mFallback ? (byte) 1 : (byte) 0);
            parcel.writeInt(this.mEffectStrength);
        }
    }
}
