package android.hardware.radio;

import android.annotation.SystemApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.Set;

@SystemApi
public final class RadioMetadata implements Parcelable {
    public static final Parcelable.Creator<RadioMetadata> CREATOR;
    private static final ArrayMap<String, Integer> METADATA_KEYS_TYPE = new ArrayMap<>();
    public static final String METADATA_KEY_ALBUM = "android.hardware.radio.metadata.ALBUM";
    public static final String METADATA_KEY_ART = "android.hardware.radio.metadata.ART";
    public static final String METADATA_KEY_ARTIST = "android.hardware.radio.metadata.ARTIST";
    public static final String METADATA_KEY_CLOCK = "android.hardware.radio.metadata.CLOCK";
    public static final String METADATA_KEY_DAB_COMPONENT_NAME = "android.hardware.radio.metadata.DAB_COMPONENT_NAME";
    public static final String METADATA_KEY_DAB_COMPONENT_NAME_SHORT = "android.hardware.radio.metadata.DAB_COMPONENT_NAME_SHORT";
    public static final String METADATA_KEY_DAB_ENSEMBLE_NAME = "android.hardware.radio.metadata.DAB_ENSEMBLE_NAME";
    public static final String METADATA_KEY_DAB_ENSEMBLE_NAME_SHORT = "android.hardware.radio.metadata.DAB_ENSEMBLE_NAME_SHORT";
    public static final String METADATA_KEY_DAB_SERVICE_NAME = "android.hardware.radio.metadata.DAB_SERVICE_NAME";
    public static final String METADATA_KEY_DAB_SERVICE_NAME_SHORT = "android.hardware.radio.metadata.DAB_SERVICE_NAME_SHORT";
    public static final String METADATA_KEY_GENRE = "android.hardware.radio.metadata.GENRE";
    public static final String METADATA_KEY_ICON = "android.hardware.radio.metadata.ICON";
    public static final String METADATA_KEY_PROGRAM_NAME = "android.hardware.radio.metadata.PROGRAM_NAME";
    public static final String METADATA_KEY_RBDS_PTY = "android.hardware.radio.metadata.RBDS_PTY";
    public static final String METADATA_KEY_RDS_PI = "android.hardware.radio.metadata.RDS_PI";
    public static final String METADATA_KEY_RDS_PS = "android.hardware.radio.metadata.RDS_PS";
    public static final String METADATA_KEY_RDS_PTY = "android.hardware.radio.metadata.RDS_PTY";
    public static final String METADATA_KEY_RDS_RT = "android.hardware.radio.metadata.RDS_RT";
    public static final String METADATA_KEY_TITLE = "android.hardware.radio.metadata.TITLE";
    private static final int METADATA_TYPE_BITMAP = 2;
    private static final int METADATA_TYPE_CLOCK = 3;
    private static final int METADATA_TYPE_INT = 0;
    private static final int METADATA_TYPE_INVALID = -1;
    private static final int METADATA_TYPE_TEXT = 1;
    private static final int NATIVE_KEY_ALBUM = 7;
    private static final int NATIVE_KEY_ART = 10;
    private static final int NATIVE_KEY_ARTIST = 6;
    private static final int NATIVE_KEY_CLOCK = 11;
    private static final int NATIVE_KEY_GENRE = 8;
    private static final int NATIVE_KEY_ICON = 9;
    private static final int NATIVE_KEY_INVALID = -1;
    private static final SparseArray<String> NATIVE_KEY_MAPPING;
    private static final int NATIVE_KEY_RBDS_PTY = 3;
    private static final int NATIVE_KEY_RDS_PI = 0;
    private static final int NATIVE_KEY_RDS_PS = 1;
    private static final int NATIVE_KEY_RDS_PTY = 2;
    private static final int NATIVE_KEY_RDS_RT = 4;
    private static final int NATIVE_KEY_TITLE = 5;
    private static final String TAG = "BroadcastRadio.metadata";
    private final Bundle mBundle;

    static {
        METADATA_KEYS_TYPE.put(METADATA_KEY_RDS_PI, 0);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RDS_PS, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RDS_PTY, 0);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RBDS_PTY, 0);
        METADATA_KEYS_TYPE.put(METADATA_KEY_RDS_RT, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_TITLE, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ARTIST, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ALBUM, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_GENRE, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ICON, 2);
        METADATA_KEYS_TYPE.put(METADATA_KEY_ART, 2);
        METADATA_KEYS_TYPE.put(METADATA_KEY_CLOCK, 3);
        METADATA_KEYS_TYPE.put(METADATA_KEY_PROGRAM_NAME, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DAB_ENSEMBLE_NAME, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DAB_ENSEMBLE_NAME_SHORT, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DAB_SERVICE_NAME, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DAB_SERVICE_NAME_SHORT, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DAB_COMPONENT_NAME, 1);
        METADATA_KEYS_TYPE.put(METADATA_KEY_DAB_COMPONENT_NAME_SHORT, 1);
        NATIVE_KEY_MAPPING = new SparseArray<>();
        NATIVE_KEY_MAPPING.put(0, METADATA_KEY_RDS_PI);
        NATIVE_KEY_MAPPING.put(1, METADATA_KEY_RDS_PS);
        NATIVE_KEY_MAPPING.put(2, METADATA_KEY_RDS_PTY);
        NATIVE_KEY_MAPPING.put(3, METADATA_KEY_RBDS_PTY);
        NATIVE_KEY_MAPPING.put(4, METADATA_KEY_RDS_RT);
        NATIVE_KEY_MAPPING.put(5, METADATA_KEY_TITLE);
        NATIVE_KEY_MAPPING.put(6, METADATA_KEY_ARTIST);
        NATIVE_KEY_MAPPING.put(7, METADATA_KEY_ALBUM);
        NATIVE_KEY_MAPPING.put(8, METADATA_KEY_GENRE);
        NATIVE_KEY_MAPPING.put(9, METADATA_KEY_ICON);
        NATIVE_KEY_MAPPING.put(10, METADATA_KEY_ART);
        NATIVE_KEY_MAPPING.put(11, METADATA_KEY_CLOCK);
        CREATOR = new Parcelable.Creator<RadioMetadata>() {
            @Override
            public RadioMetadata createFromParcel(Parcel parcel) {
                return new RadioMetadata(parcel);
            }

            @Override
            public RadioMetadata[] newArray(int i) {
                return new RadioMetadata[i];
            }
        };
    }

    @SystemApi
    public static final class Clock implements Parcelable {
        public static final Parcelable.Creator<Clock> CREATOR = new Parcelable.Creator<Clock>() {
            @Override
            public Clock createFromParcel(Parcel parcel) {
                return new Clock(parcel);
            }

            @Override
            public Clock[] newArray(int i) {
                return new Clock[i];
            }
        };
        private final int mTimezoneOffsetMinutes;
        private final long mUtcEpochSeconds;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeLong(this.mUtcEpochSeconds);
            parcel.writeInt(this.mTimezoneOffsetMinutes);
        }

        public Clock(long j, int i) {
            this.mUtcEpochSeconds = j;
            this.mTimezoneOffsetMinutes = i;
        }

        private Clock(Parcel parcel) {
            this.mUtcEpochSeconds = parcel.readLong();
            this.mTimezoneOffsetMinutes = parcel.readInt();
        }

        public long getUtcEpochSeconds() {
            return this.mUtcEpochSeconds;
        }

        public int getTimezoneOffsetMinutes() {
            return this.mTimezoneOffsetMinutes;
        }
    }

    RadioMetadata() {
        this.mBundle = new Bundle();
    }

    private RadioMetadata(Bundle bundle) {
        this.mBundle = new Bundle(bundle);
    }

    private RadioMetadata(Parcel parcel) {
        this.mBundle = parcel.readBundle();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RadioMetadata[");
        boolean z = true;
        for (String str : this.mBundle.keySet()) {
            if (z) {
                z = false;
            } else {
                sb.append(", ");
            }
            sb.append(str.startsWith("android.hardware.radio.metadata") ? str.substring("android.hardware.radio.metadata".length()) : str);
            sb.append('=');
            sb.append(this.mBundle.get(str));
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean containsKey(String str) {
        return this.mBundle.containsKey(str);
    }

    public String getString(String str) {
        return this.mBundle.getString(str);
    }

    private static void putInt(Bundle bundle, String str, int i) {
        int iIntValue = METADATA_KEYS_TYPE.getOrDefault(str, -1).intValue();
        if (iIntValue != 0 && iIntValue != 2) {
            throw new IllegalArgumentException("The " + str + " key cannot be used to put an int");
        }
        bundle.putInt(str, i);
    }

    public int getInt(String str) {
        return this.mBundle.getInt(str, 0);
    }

    @Deprecated
    public Bitmap getBitmap(String str) {
        try {
            return (Bitmap) this.mBundle.getParcelable(str);
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve a key as Bitmap.", e);
            return null;
        }
    }

    public int getBitmapId(String str) {
        if (METADATA_KEY_ICON.equals(str) || METADATA_KEY_ART.equals(str)) {
            return getInt(str);
        }
        return 0;
    }

    public Clock getClock(String str) {
        try {
            return (Clock) this.mBundle.getParcelable(str);
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve a key as Clock.", e);
            return null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeBundle(this.mBundle);
    }

    public int size() {
        return this.mBundle.size();
    }

    public Set<String> keySet() {
        return this.mBundle.keySet();
    }

    public static String getKeyFromNativeKey(int i) {
        return NATIVE_KEY_MAPPING.get(i, null);
    }

    public static final class Builder {
        private final Bundle mBundle;

        public Builder() {
            this.mBundle = new Bundle();
        }

        public Builder(RadioMetadata radioMetadata) {
            this.mBundle = new Bundle(radioMetadata.mBundle);
        }

        public Builder(RadioMetadata radioMetadata, int i) {
            this(radioMetadata);
            for (String str : this.mBundle.keySet()) {
                Object obj = this.mBundle.get(str);
                if (obj != null && (obj instanceof Bitmap)) {
                    Bitmap bitmap = (Bitmap) obj;
                    if (bitmap.getHeight() > i || bitmap.getWidth() > i) {
                        putBitmap(str, scaleBitmap(bitmap, i));
                    }
                }
            }
        }

        public Builder putString(String str, String str2) {
            if (!RadioMetadata.METADATA_KEYS_TYPE.containsKey(str) || ((Integer) RadioMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 1) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a String");
            }
            this.mBundle.putString(str, str2);
            return this;
        }

        public Builder putInt(String str, int i) {
            RadioMetadata.putInt(this.mBundle, str, i);
            return this;
        }

        public Builder putBitmap(String str, Bitmap bitmap) {
            if (!RadioMetadata.METADATA_KEYS_TYPE.containsKey(str) || ((Integer) RadioMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 2) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a Bitmap");
            }
            this.mBundle.putParcelable(str, bitmap);
            return this;
        }

        public Builder putClock(String str, long j, int i) {
            if (!RadioMetadata.METADATA_KEYS_TYPE.containsKey(str) || ((Integer) RadioMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 3) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a RadioMetadata.Clock.");
            }
            this.mBundle.putParcelable(str, new Clock(j, i));
            return this;
        }

        public RadioMetadata build() {
            return new RadioMetadata(this.mBundle);
        }

        private Bitmap scaleBitmap(Bitmap bitmap, int i) {
            float f = i;
            float fMin = Math.min(f / bitmap.getWidth(), f / bitmap.getHeight());
            return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * fMin), (int) (bitmap.getHeight() * fMin), true);
        }
    }

    int putIntFromNative(int i, int i2) {
        try {
            putInt(this.mBundle, getKeyFromNativeKey(i), i2);
            return 0;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

    int putStringFromNative(int i, String str) {
        String keyFromNativeKey = getKeyFromNativeKey(i);
        if (!METADATA_KEYS_TYPE.containsKey(keyFromNativeKey) || METADATA_KEYS_TYPE.get(keyFromNativeKey).intValue() != 1) {
            return -1;
        }
        this.mBundle.putString(keyFromNativeKey, str);
        return 0;
    }

    int putBitmapFromNative(int i, byte[] bArr) {
        String keyFromNativeKey = getKeyFromNativeKey(i);
        if (!METADATA_KEYS_TYPE.containsKey(keyFromNativeKey) || METADATA_KEYS_TYPE.get(keyFromNativeKey).intValue() != 2) {
            return -1;
        }
        try {
            Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length);
            if (bitmapDecodeByteArray != null) {
                this.mBundle.putParcelable(keyFromNativeKey, bitmapDecodeByteArray);
                return 0;
            }
        } catch (Exception e) {
        }
        return -1;
    }

    int putClockFromNative(int i, long j, int i2) {
        String keyFromNativeKey = getKeyFromNativeKey(i);
        if (!METADATA_KEYS_TYPE.containsKey(keyFromNativeKey) || METADATA_KEYS_TYPE.get(keyFromNativeKey).intValue() != 3) {
            return -1;
        }
        this.mBundle.putParcelable(keyFromNativeKey, new Clock(j, i2));
        return 0;
    }
}
