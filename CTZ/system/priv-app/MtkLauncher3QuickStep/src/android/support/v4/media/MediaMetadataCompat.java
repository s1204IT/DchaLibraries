package android.support.v4.media;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;
/* loaded from: classes.dex */
public final class MediaMetadataCompat implements Parcelable {
    public static final Parcelable.Creator<MediaMetadataCompat> CREATOR;
    static final ArrayMap<String, Integer> METADATA_KEYS_TYPE = new ArrayMap<>();
    public static final String METADATA_KEY_ADVERTISEMENT = "android.media.metadata.ADVERTISEMENT";
    public static final String METADATA_KEY_ALBUM = "android.media.metadata.ALBUM";
    public static final String METADATA_KEY_ALBUM_ART = "android.media.metadata.ALBUM_ART";
    public static final String METADATA_KEY_ALBUM_ARTIST = "android.media.metadata.ALBUM_ARTIST";
    public static final String METADATA_KEY_ALBUM_ART_URI = "android.media.metadata.ALBUM_ART_URI";
    public static final String METADATA_KEY_ART = "android.media.metadata.ART";
    public static final String METADATA_KEY_ARTIST = "android.media.metadata.ARTIST";
    public static final String METADATA_KEY_ART_URI = "android.media.metadata.ART_URI";
    public static final String METADATA_KEY_AUTHOR = "android.media.metadata.AUTHOR";
    public static final String METADATA_KEY_BT_FOLDER_TYPE = "android.media.metadata.BT_FOLDER_TYPE";
    public static final String METADATA_KEY_COMPILATION = "android.media.metadata.COMPILATION";
    public static final String METADATA_KEY_COMPOSER = "android.media.metadata.COMPOSER";
    public static final String METADATA_KEY_DATE = "android.media.metadata.DATE";
    public static final String METADATA_KEY_DISC_NUMBER = "android.media.metadata.DISC_NUMBER";
    public static final String METADATA_KEY_DISPLAY_DESCRIPTION = "android.media.metadata.DISPLAY_DESCRIPTION";
    public static final String METADATA_KEY_DISPLAY_ICON = "android.media.metadata.DISPLAY_ICON";
    public static final String METADATA_KEY_DISPLAY_ICON_URI = "android.media.metadata.DISPLAY_ICON_URI";
    public static final String METADATA_KEY_DISPLAY_SUBTITLE = "android.media.metadata.DISPLAY_SUBTITLE";
    public static final String METADATA_KEY_DISPLAY_TITLE = "android.media.metadata.DISPLAY_TITLE";
    public static final String METADATA_KEY_DOWNLOAD_STATUS = "android.media.metadata.DOWNLOAD_STATUS";
    public static final String METADATA_KEY_DURATION = "android.media.metadata.DURATION";
    public static final String METADATA_KEY_GENRE = "android.media.metadata.GENRE";
    public static final String METADATA_KEY_MEDIA_ID = "android.media.metadata.MEDIA_ID";
    public static final String METADATA_KEY_MEDIA_URI = "android.media.metadata.MEDIA_URI";
    public static final String METADATA_KEY_NUM_TRACKS = "android.media.metadata.NUM_TRACKS";
    public static final String METADATA_KEY_RATING = "android.media.metadata.RATING";
    public static final String METADATA_KEY_TITLE = "android.media.metadata.TITLE";
    public static final String METADATA_KEY_TRACK_NUMBER = "android.media.metadata.TRACK_NUMBER";
    public static final String METADATA_KEY_USER_RATING = "android.media.metadata.USER_RATING";
    public static final String METADATA_KEY_WRITER = "android.media.metadata.WRITER";
    public static final String METADATA_KEY_YEAR = "android.media.metadata.YEAR";
    static final int METADATA_TYPE_BITMAP = 2;
    static final int METADATA_TYPE_LONG = 0;
    static final int METADATA_TYPE_RATING = 3;
    static final int METADATA_TYPE_TEXT = 1;
    private static final String[] PREFERRED_BITMAP_ORDER;
    private static final String[] PREFERRED_DESCRIPTION_ORDER;
    private static final String[] PREFERRED_URI_ORDER;
    private static final String TAG = "MediaMetadata";
    final Bundle mBundle;
    private MediaDescriptionCompat mDescription;
    private Object mMetadataObj;

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    /* loaded from: classes.dex */
    public @interface BitmapKey {
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    /* loaded from: classes.dex */
    public @interface LongKey {
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    /* loaded from: classes.dex */
    public @interface RatingKey {
    }

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    /* loaded from: classes.dex */
    public @interface TextKey {
    }

    static {
        METADATA_KEYS_TYPE.put("android.media.metadata.TITLE", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.ARTIST", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.DURATION", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.ALBUM", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.AUTHOR", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.WRITER", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.COMPOSER", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.COMPILATION", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.DATE", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.YEAR", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.GENRE", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.TRACK_NUMBER", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.NUM_TRACKS", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.DISC_NUMBER", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.ALBUM_ARTIST", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.ART", 2);
        METADATA_KEYS_TYPE.put("android.media.metadata.ART_URI", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.ALBUM_ART", 2);
        METADATA_KEYS_TYPE.put("android.media.metadata.ALBUM_ART_URI", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.USER_RATING", 3);
        METADATA_KEYS_TYPE.put("android.media.metadata.RATING", 3);
        METADATA_KEYS_TYPE.put("android.media.metadata.DISPLAY_TITLE", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.DISPLAY_SUBTITLE", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.DISPLAY_DESCRIPTION", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.DISPLAY_ICON", 2);
        METADATA_KEYS_TYPE.put("android.media.metadata.DISPLAY_ICON_URI", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.MEDIA_ID", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.BT_FOLDER_TYPE", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.MEDIA_URI", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.ADVERTISEMENT", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.DOWNLOAD_STATUS", 0);
        PREFERRED_DESCRIPTION_ORDER = new String[]{"android.media.metadata.TITLE", "android.media.metadata.ARTIST", "android.media.metadata.ALBUM", "android.media.metadata.ALBUM_ARTIST", "android.media.metadata.WRITER", "android.media.metadata.AUTHOR", "android.media.metadata.COMPOSER"};
        PREFERRED_BITMAP_ORDER = new String[]{"android.media.metadata.DISPLAY_ICON", "android.media.metadata.ART", "android.media.metadata.ALBUM_ART"};
        PREFERRED_URI_ORDER = new String[]{"android.media.metadata.DISPLAY_ICON_URI", "android.media.metadata.ART_URI", "android.media.metadata.ALBUM_ART_URI"};
        CREATOR = new Parcelable.Creator<MediaMetadataCompat>() { // from class: android.support.v4.media.MediaMetadataCompat.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public MediaMetadataCompat createFromParcel(Parcel in) {
                return new MediaMetadataCompat(in);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public MediaMetadataCompat[] newArray(int size) {
                return new MediaMetadataCompat[size];
            }
        };
    }

    MediaMetadataCompat(Bundle bundle) {
        this.mBundle = new Bundle(bundle);
        this.mBundle.setClassLoader(MediaMetadataCompat.class.getClassLoader());
    }

    MediaMetadataCompat(Parcel in) {
        this.mBundle = in.readBundle();
        this.mBundle.setClassLoader(MediaMetadataCompat.class.getClassLoader());
    }

    public boolean containsKey(String key) {
        return this.mBundle.containsKey(key);
    }

    public CharSequence getText(String key) {
        return this.mBundle.getCharSequence(key);
    }

    public String getString(String key) {
        CharSequence text = this.mBundle.getCharSequence(key);
        if (text != null) {
            return text.toString();
        }
        return null;
    }

    public long getLong(String key) {
        return this.mBundle.getLong(key, 0L);
    }

    public RatingCompat getRating(String key) {
        RatingCompat rating = null;
        try {
            if (Build.VERSION.SDK_INT >= 19) {
                rating = RatingCompat.fromRating(this.mBundle.getParcelable(key));
            } else {
                rating = (RatingCompat) this.mBundle.getParcelable(key);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve a key as Rating.", e);
        }
        return rating;
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

    public MediaDescriptionCompat getDescription() {
        if (this.mDescription != null) {
            return this.mDescription;
        }
        String mediaId = getString("android.media.metadata.MEDIA_ID");
        CharSequence[] text = new CharSequence[3];
        Bitmap icon = null;
        Uri iconUri = null;
        CharSequence displayText = getText("android.media.metadata.DISPLAY_TITLE");
        if (!TextUtils.isEmpty(displayText)) {
            text[0] = displayText;
            text[1] = getText("android.media.metadata.DISPLAY_SUBTITLE");
            text[2] = getText("android.media.metadata.DISPLAY_DESCRIPTION");
        } else {
            int textIndex = 0;
            int textIndex2 = 0;
            while (textIndex < text.length && textIndex2 < PREFERRED_DESCRIPTION_ORDER.length) {
                int keyIndex = textIndex2 + 1;
                CharSequence next = getText(PREFERRED_DESCRIPTION_ORDER[textIndex2]);
                if (!TextUtils.isEmpty(next)) {
                    text[textIndex] = next;
                    textIndex++;
                }
                textIndex2 = keyIndex;
            }
        }
        int i = 0;
        while (true) {
            if (i >= PREFERRED_BITMAP_ORDER.length) {
                break;
            }
            Bitmap next2 = getBitmap(PREFERRED_BITMAP_ORDER[i]);
            if (next2 == null) {
                i++;
            } else {
                icon = next2;
                break;
            }
        }
        int i2 = 0;
        while (true) {
            if (i2 >= PREFERRED_URI_ORDER.length) {
                break;
            }
            String next3 = getString(PREFERRED_URI_ORDER[i2]);
            if (TextUtils.isEmpty(next3)) {
                i2++;
            } else {
                iconUri = Uri.parse(next3);
                break;
            }
        }
        Uri mediaUri = null;
        String mediaUriStr = getString("android.media.metadata.MEDIA_URI");
        if (!TextUtils.isEmpty(mediaUriStr)) {
            mediaUri = Uri.parse(mediaUriStr);
        }
        MediaDescriptionCompat.Builder bob = new MediaDescriptionCompat.Builder();
        bob.setMediaId(mediaId);
        bob.setTitle(text[0]);
        bob.setSubtitle(text[1]);
        bob.setDescription(text[2]);
        bob.setIconBitmap(icon);
        bob.setIconUri(iconUri);
        bob.setMediaUri(mediaUri);
        Bundle bundle = new Bundle();
        if (this.mBundle.containsKey("android.media.metadata.BT_FOLDER_TYPE")) {
            bundle.putLong(MediaDescriptionCompat.EXTRA_BT_FOLDER_TYPE, getLong("android.media.metadata.BT_FOLDER_TYPE"));
        }
        if (this.mBundle.containsKey("android.media.metadata.DOWNLOAD_STATUS")) {
            bundle.putLong(MediaDescriptionCompat.EXTRA_DOWNLOAD_STATUS, getLong("android.media.metadata.DOWNLOAD_STATUS"));
        }
        if (!bundle.isEmpty()) {
            bob.setExtras(bundle);
        }
        this.mDescription = bob.build();
        return this.mDescription;
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(this.mBundle);
    }

    public int size() {
        return this.mBundle.size();
    }

    public Set<String> keySet() {
        return this.mBundle.keySet();
    }

    public Bundle getBundle() {
        return this.mBundle;
    }

    public static MediaMetadataCompat fromMediaMetadata(Object metadataObj) {
        if (metadataObj != null && Build.VERSION.SDK_INT >= 21) {
            Parcel p = Parcel.obtain();
            MediaMetadataCompatApi21.writeToParcel(metadataObj, p, 0);
            p.setDataPosition(0);
            MediaMetadataCompat metadata = CREATOR.createFromParcel(p);
            p.recycle();
            metadata.mMetadataObj = metadataObj;
            return metadata;
        }
        return null;
    }

    public Object getMediaMetadata() {
        if (this.mMetadataObj == null && Build.VERSION.SDK_INT >= 21) {
            Parcel p = Parcel.obtain();
            writeToParcel(p, 0);
            p.setDataPosition(0);
            this.mMetadataObj = MediaMetadataCompatApi21.createFromParcel(p);
            p.recycle();
        }
        return this.mMetadataObj;
    }

    /* loaded from: classes.dex */
    public static final class Builder {
        private final Bundle mBundle;

        public Builder() {
            this.mBundle = new Bundle();
        }

        public Builder(MediaMetadataCompat source) {
            this.mBundle = new Bundle(source.mBundle);
        }

        @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
        public Builder(MediaMetadataCompat source, int maxBitmapSize) {
            this(source);
            for (String key : this.mBundle.keySet()) {
                Object value = this.mBundle.get(key);
                if (value instanceof Bitmap) {
                    Bitmap bmp = (Bitmap) value;
                    if (bmp.getHeight() > maxBitmapSize || bmp.getWidth() > maxBitmapSize) {
                        putBitmap(key, scaleBitmap(bmp, maxBitmapSize));
                    }
                }
            }
        }

        public Builder putText(String key, CharSequence value) {
            if (MediaMetadataCompat.METADATA_KEYS_TYPE.containsKey(key) && MediaMetadataCompat.METADATA_KEYS_TYPE.get(key).intValue() != 1) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a CharSequence");
            }
            this.mBundle.putCharSequence(key, value);
            return this;
        }

        public Builder putString(String key, String value) {
            if (MediaMetadataCompat.METADATA_KEYS_TYPE.containsKey(key) && MediaMetadataCompat.METADATA_KEYS_TYPE.get(key).intValue() != 1) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a String");
            }
            this.mBundle.putCharSequence(key, value);
            return this;
        }

        public Builder putLong(String key, long value) {
            if (MediaMetadataCompat.METADATA_KEYS_TYPE.containsKey(key) && MediaMetadataCompat.METADATA_KEYS_TYPE.get(key).intValue() != 0) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a long");
            }
            this.mBundle.putLong(key, value);
            return this;
        }

        public Builder putRating(String key, RatingCompat value) {
            if (MediaMetadataCompat.METADATA_KEYS_TYPE.containsKey(key) && MediaMetadataCompat.METADATA_KEYS_TYPE.get(key).intValue() != 3) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a Rating");
            }
            if (Build.VERSION.SDK_INT >= 19) {
                this.mBundle.putParcelable(key, (Parcelable) value.getRating());
            } else {
                this.mBundle.putParcelable(key, value);
            }
            return this;
        }

        public Builder putBitmap(String key, Bitmap value) {
            if (MediaMetadataCompat.METADATA_KEYS_TYPE.containsKey(key) && MediaMetadataCompat.METADATA_KEYS_TYPE.get(key).intValue() != 2) {
                throw new IllegalArgumentException("The " + key + " key cannot be used to put a Bitmap");
            }
            this.mBundle.putParcelable(key, value);
            return this;
        }

        public MediaMetadataCompat build() {
            return new MediaMetadataCompat(this.mBundle);
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
}
