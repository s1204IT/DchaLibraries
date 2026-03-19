package android.hardware.radio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.util.Set;

public final class RadioMetadata implements Parcelable {
    public static final Parcelable.Creator<RadioMetadata> CREATOR;
    private static final ArrayMap<String, Integer> METADATA_KEYS_TYPE = new ArrayMap<>();
    public static final String METADATA_KEY_ALBUM = "android.hardware.radio.metadata.ALBUM";
    public static final String METADATA_KEY_ART = "android.hardware.radio.metadata.ART";
    public static final String METADATA_KEY_ARTIST = "android.hardware.radio.metadata.ARTIST";
    public static final String METADATA_KEY_CLOCK = "android.hardware.radio.metadata.CLOCK";
    public static final String METADATA_KEY_GENRE = "android.hardware.radio.metadata.GENRE";
    public static final String METADATA_KEY_ICON = "android.hardware.radio.metadata.ICON";
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
    private static final String TAG = "RadioMetadata";
    private final Bundle mBundle;

    RadioMetadata(Bundle bundle, RadioMetadata radioMetadata) {
        this(bundle);
    }

    RadioMetadata(Parcel in, RadioMetadata radioMetadata) {
        this(in);
    }

    static {
        METADATA_KEYS_TYPE.put(METADATA_KEY_RDS_PI, 1);
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
            public RadioMetadata createFromParcel(Parcel in) {
                return new RadioMetadata(in, (RadioMetadata) null);
            }

            @Override
            public RadioMetadata[] newArray(int size) {
                return new RadioMetadata[size];
            }
        };
    }

    public static final class Clock implements Parcelable {
        public static final Parcelable.Creator<Clock> CREATOR = new Parcelable.Creator<Clock>() {
            @Override
            public Clock createFromParcel(Parcel in) {
                return new Clock(in, (Clock) null);
            }

            @Override
            public Clock[] newArray(int size) {
                return new Clock[size];
            }
        };
        private final int mTimezoneOffsetMinutes;
        private final long mUtcEpochSeconds;

        Clock(Parcel in, Clock clock) {
            this(in);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeLong(this.mUtcEpochSeconds);
            out.writeInt(this.mTimezoneOffsetMinutes);
        }

        public Clock(long utcEpochSeconds, int timezoneOffsetMinutes) {
            this.mUtcEpochSeconds = utcEpochSeconds;
            this.mTimezoneOffsetMinutes = timezoneOffsetMinutes;
        }

        private Clock(Parcel in) {
            this.mUtcEpochSeconds = in.readLong();
            this.mTimezoneOffsetMinutes = in.readInt();
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

    private RadioMetadata(Parcel in) {
        this.mBundle = in.readBundle();
    }

    public boolean containsKey(String key) {
        return this.mBundle.containsKey(key);
    }

    public String getString(String key) {
        return this.mBundle.getString(key);
    }

    public int getInt(String key) {
        return this.mBundle.getInt(key, 0);
    }

    public Bitmap getBitmap(String key) {
        try {
            Bitmap bmp = (Bitmap) this.mBundle.getParcelable(key);
            return bmp;
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve a key as Bitmap.", e);
            return null;
        }
    }

    public Clock getClock(String key) {
        try {
            Clock clock = (Clock) this.mBundle.getParcelable(key);
            return clock;
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(this.mBundle);
    }

    public int size() {
        return this.mBundle.size();
    }

    public Set<String> keySet() {
        return this.mBundle.keySet();
    }

    public static String getKeyFromNativeKey(int nativeKey) {
        return NATIVE_KEY_MAPPING.get(nativeKey, null);
    }

    public static final class Builder {
        private final Bundle mBundle;

        public Builder() {
            this.mBundle = new Bundle();
        }

        public Builder(RadioMetadata source) {
            this.mBundle = new Bundle(source.mBundle);
        }

        public Builder(RadioMetadata source, int maxBitmapSize) {
            this(source);
            for (String key : this.mBundle.keySet()) {
                ?? r3 = this.mBundle.get(key);
                if (r3 != 0 && (r3 instanceof Bitmap) && (r3.getHeight() > maxBitmapSize || r3.getWidth() > maxBitmapSize)) {
                    putBitmap(key, scaleBitmap(r3, maxBitmapSize));
                }
            }
        }

        public Builder putString(String key, String value) {
            if (!RadioMetadata.METADATA_KEYS_TYPE.containsKey(key) || ((Integer) RadioMetadata.METADATA_KEYS_TYPE.get(key)).intValue() != 1) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a String");
            }
            this.mBundle.putString(key, value);
            return this;
        }

        public Builder putInt(String key, int value) {
            if (!RadioMetadata.METADATA_KEYS_TYPE.containsKey(key) || ((Integer) RadioMetadata.METADATA_KEYS_TYPE.get(key)).intValue() != 0) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a long");
            }
            this.mBundle.putInt(key, value);
            return this;
        }

        public Builder putBitmap(String key, Bitmap value) {
            if (!RadioMetadata.METADATA_KEYS_TYPE.containsKey(key) || ((Integer) RadioMetadata.METADATA_KEYS_TYPE.get(key)).intValue() != 2) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a Bitmap");
            }
            this.mBundle.putParcelable(key, value);
            return this;
        }

        public Builder putClock(String key, long utcSecondsSinceEpoch, int timezoneOffsetMinutes) {
            if (!RadioMetadata.METADATA_KEYS_TYPE.containsKey(key) || ((Integer) RadioMetadata.METADATA_KEYS_TYPE.get(key)).intValue() != 3) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a RadioMetadata.Clock.");
            }
            this.mBundle.putParcelable(key, new Clock(utcSecondsSinceEpoch, timezoneOffsetMinutes));
            return this;
        }

        public RadioMetadata build() {
            return new RadioMetadata(this.mBundle, (RadioMetadata) null);
        }

        private Bitmap scaleBitmap(Bitmap bmp, int maxSize) {
            float maxSizeF = maxSize;
            float widthScale = maxSizeF / bmp.getWidth();
            float heightScale = maxSizeF / bmp.getHeight();
            float scale = Math.min(widthScale, heightScale);
            int height = (int) (bmp.getHeight() * scale);
            int width = (int) (bmp.getWidth() * scale);
            return Bitmap.createScaledBitmap(bmp, width, height, true);
        }
    }

    int putIntFromNative(int nativeKey, int value) {
        String key = getKeyFromNativeKey(nativeKey);
        if (!METADATA_KEYS_TYPE.containsKey(key) || METADATA_KEYS_TYPE.get(key).intValue() != 0) {
            return -1;
        }
        this.mBundle.putInt(key, value);
        return 0;
    }

    int putStringFromNative(int nativeKey, String value) {
        String key = getKeyFromNativeKey(nativeKey);
        if (!METADATA_KEYS_TYPE.containsKey(key) || METADATA_KEYS_TYPE.get(key).intValue() != 1) {
            return -1;
        }
        this.mBundle.putString(key, value);
        return 0;
    }

    int putBitmapFromNative(int nativeKey, byte[] value) {
        String key = getKeyFromNativeKey(nativeKey);
        if (!METADATA_KEYS_TYPE.containsKey(key) || METADATA_KEYS_TYPE.get(key).intValue() != 2) {
            return -1;
        }
        try {
            Bitmap bmp = BitmapFactory.decodeByteArray(value, 0, value.length);
            if (bmp == null) {
                return -1;
            }
            this.mBundle.putParcelable(key, bmp);
            return 0;
        } catch (Exception e) {
            return -1;
        } catch (Throwable th) {
            return -1;
        }
    }

    int putClockFromNative(int nativeKey, long utcEpochSeconds, int timezoneOffsetInMinutes) {
        Log.d(TAG, "putClockFromNative()");
        String key = getKeyFromNativeKey(nativeKey);
        if (!METADATA_KEYS_TYPE.containsKey(key) || METADATA_KEYS_TYPE.get(key).intValue() != 3) {
            return -1;
        }
        this.mBundle.putParcelable(key, new Clock(utcEpochSeconds, timezoneOffsetInMinutes));
        return 0;
    }
}
