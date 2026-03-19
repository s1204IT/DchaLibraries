package android.media;

import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.Set;

public final class MediaMetadata implements Parcelable {
    public static final Parcelable.Creator<MediaMetadata> CREATOR;
    private static final SparseArray<String> EDITOR_KEY_MAPPING;
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
    private static final int METADATA_TYPE_BITMAP = 2;
    private static final int METADATA_TYPE_INVALID = -1;
    private static final int METADATA_TYPE_LONG = 0;
    private static final int METADATA_TYPE_RATING = 3;
    private static final int METADATA_TYPE_TEXT = 1;
    private static final String TAG = "MediaMetadata";
    private final Bundle mBundle;
    private MediaDescription mDescription;
    private static final String[] PREFERRED_DESCRIPTION_ORDER = {"android.media.metadata.TITLE", "android.media.metadata.ARTIST", "android.media.metadata.ALBUM", "android.media.metadata.ALBUM_ARTIST", "android.media.metadata.WRITER", "android.media.metadata.AUTHOR", "android.media.metadata.COMPOSER"};
    private static final String[] PREFERRED_BITMAP_ORDER = {"android.media.metadata.DISPLAY_ICON", "android.media.metadata.ART", "android.media.metadata.ALBUM_ART"};
    private static final String[] PREFERRED_URI_ORDER = {"android.media.metadata.DISPLAY_ICON_URI", "android.media.metadata.ART_URI", "android.media.metadata.ALBUM_ART_URI"};
    private static final ArrayMap<String, Integer> METADATA_KEYS_TYPE = new ArrayMap<>();

    @Retention(RetentionPolicy.SOURCE)
    public @interface BitmapKey {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface LongKey {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RatingKey {
    }

    @Retention(RetentionPolicy.SOURCE)
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
        METADATA_KEYS_TYPE.put("android.media.metadata.BT_FOLDER_TYPE", 0);
        METADATA_KEYS_TYPE.put("android.media.metadata.MEDIA_ID", 1);
        METADATA_KEYS_TYPE.put("android.media.metadata.MEDIA_URI", 1);
        EDITOR_KEY_MAPPING = new SparseArray<>();
        EDITOR_KEY_MAPPING.put(100, "android.media.metadata.ART");
        EDITOR_KEY_MAPPING.put(101, "android.media.metadata.RATING");
        EDITOR_KEY_MAPPING.put(MediaMetadataEditor.RATING_KEY_BY_USER, "android.media.metadata.USER_RATING");
        EDITOR_KEY_MAPPING.put(1, "android.media.metadata.ALBUM");
        EDITOR_KEY_MAPPING.put(13, "android.media.metadata.ALBUM_ARTIST");
        EDITOR_KEY_MAPPING.put(2, "android.media.metadata.ARTIST");
        EDITOR_KEY_MAPPING.put(3, "android.media.metadata.AUTHOR");
        EDITOR_KEY_MAPPING.put(0, "android.media.metadata.TRACK_NUMBER");
        EDITOR_KEY_MAPPING.put(4, "android.media.metadata.COMPOSER");
        EDITOR_KEY_MAPPING.put(15, "android.media.metadata.COMPILATION");
        EDITOR_KEY_MAPPING.put(5, "android.media.metadata.DATE");
        EDITOR_KEY_MAPPING.put(14, "android.media.metadata.DISC_NUMBER");
        EDITOR_KEY_MAPPING.put(9, "android.media.metadata.DURATION");
        EDITOR_KEY_MAPPING.put(6, "android.media.metadata.GENRE");
        EDITOR_KEY_MAPPING.put(10, "android.media.metadata.NUM_TRACKS");
        EDITOR_KEY_MAPPING.put(7, "android.media.metadata.TITLE");
        EDITOR_KEY_MAPPING.put(11, "android.media.metadata.WRITER");
        EDITOR_KEY_MAPPING.put(8, "android.media.metadata.YEAR");
        CREATOR = new Parcelable.Creator<MediaMetadata>() {
            @Override
            public MediaMetadata createFromParcel(Parcel parcel) {
                return new MediaMetadata(parcel);
            }

            @Override
            public MediaMetadata[] newArray(int i) {
                return new MediaMetadata[i];
            }
        };
    }

    private MediaMetadata(Bundle bundle) {
        this.mBundle = new Bundle(bundle);
    }

    private MediaMetadata(Parcel parcel) {
        this.mBundle = Bundle.setDefusable(parcel.readBundle(), true);
    }

    public boolean containsKey(String str) {
        return this.mBundle.containsKey(str);
    }

    public CharSequence getText(String str) {
        return this.mBundle.getCharSequence(str);
    }

    public String getString(String str) {
        CharSequence text = getText(str);
        if (text != null) {
            return text.toString();
        }
        return null;
    }

    public long getLong(String str) {
        return this.mBundle.getLong(str, 0L);
    }

    public Rating getRating(String str) {
        try {
            return (Rating) this.mBundle.getParcelable(str);
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve a key as Rating.", e);
            return null;
        }
    }

    public Bitmap getBitmap(String str) {
        try {
            return (Bitmap) this.mBundle.getParcelable(str);
        } catch (Exception e) {
            Log.w(TAG, "Failed to retrieve a key as Bitmap.", e);
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

    public MediaDescription getDescription() {
        Bitmap bitmap;
        Uri uri;
        if (this.mDescription != null) {
            return this.mDescription;
        }
        String string = getString("android.media.metadata.MEDIA_ID");
        CharSequence[] charSequenceArr = new CharSequence[3];
        CharSequence text = getText("android.media.metadata.DISPLAY_TITLE");
        if (!TextUtils.isEmpty(text)) {
            charSequenceArr[0] = text;
            charSequenceArr[1] = getText("android.media.metadata.DISPLAY_SUBTITLE");
            charSequenceArr[2] = getText("android.media.metadata.DISPLAY_DESCRIPTION");
        } else {
            int i = 0;
            int i2 = 0;
            while (i < charSequenceArr.length && i2 < PREFERRED_DESCRIPTION_ORDER.length) {
                int i3 = i2 + 1;
                CharSequence text2 = getText(PREFERRED_DESCRIPTION_ORDER[i2]);
                if (!TextUtils.isEmpty(text2)) {
                    charSequenceArr[i] = text2;
                    i++;
                }
                i2 = i3;
            }
        }
        int i4 = 0;
        while (true) {
            if (i4 < PREFERRED_BITMAP_ORDER.length) {
                bitmap = getBitmap(PREFERRED_BITMAP_ORDER[i4]);
                if (bitmap != null) {
                    break;
                }
                i4++;
            } else {
                bitmap = null;
                break;
            }
        }
        int i5 = 0;
        while (true) {
            if (i5 < PREFERRED_URI_ORDER.length) {
                String string2 = getString(PREFERRED_URI_ORDER[i5]);
                if (TextUtils.isEmpty(string2)) {
                    i5++;
                } else {
                    uri = Uri.parse(string2);
                    break;
                }
            } else {
                uri = null;
                break;
            }
        }
        String string3 = getString("android.media.metadata.MEDIA_URI");
        Uri uri2 = TextUtils.isEmpty(string3) ? null : Uri.parse(string3);
        MediaDescription.Builder builder = new MediaDescription.Builder();
        builder.setMediaId(string);
        builder.setTitle(charSequenceArr[0]);
        builder.setSubtitle(charSequenceArr[1]);
        builder.setDescription(charSequenceArr[2]);
        builder.setIconBitmap(bitmap);
        builder.setIconUri(uri);
        builder.setMediaUri(uri2);
        if (this.mBundle.containsKey("android.media.metadata.BT_FOLDER_TYPE")) {
            Bundle bundle = new Bundle();
            bundle.putLong(MediaDescription.EXTRA_BT_FOLDER_TYPE, getLong("android.media.metadata.BT_FOLDER_TYPE"));
            builder.setExtras(bundle);
        }
        this.mDescription = builder.build();
        return this.mDescription;
    }

    public static String getKeyFromMetadataEditorKey(int i) {
        return EDITOR_KEY_MAPPING.get(i, null);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MediaMetadata)) {
            return false;
        }
        MediaMetadata mediaMetadata = (MediaMetadata) obj;
        for (int i = 0; i < METADATA_KEYS_TYPE.size(); i++) {
            String strKeyAt = METADATA_KEYS_TYPE.keyAt(i);
            switch (METADATA_KEYS_TYPE.valueAt(i).intValue()) {
                case 0:
                    if (getLong(strKeyAt) != mediaMetadata.getLong(strKeyAt)) {
                        return false;
                    }
                    break;
                    break;
                case 1:
                    if (!Objects.equals(getString(strKeyAt), mediaMetadata.getString(strKeyAt))) {
                        return false;
                    }
                    break;
                    break;
            }
        }
        return true;
    }

    public int hashCode() {
        int iHashCode;
        int i = 17;
        for (int i2 = 0; i2 < METADATA_KEYS_TYPE.size(); i2++) {
            String strKeyAt = METADATA_KEYS_TYPE.keyAt(i2);
            switch (METADATA_KEYS_TYPE.valueAt(i2).intValue()) {
                case 0:
                    iHashCode = (31 * i) + Long.hashCode(getLong(strKeyAt));
                    i = iHashCode;
                    break;
                case 1:
                    iHashCode = (31 * i) + Objects.hash(getString(strKeyAt));
                    i = iHashCode;
                    break;
            }
        }
        return i;
    }

    public static final class Builder {
        private final Bundle mBundle;

        public Builder() {
            this.mBundle = new Bundle();
        }

        public Builder(MediaMetadata mediaMetadata) {
            this.mBundle = new Bundle(mediaMetadata.mBundle);
        }

        public Builder(MediaMetadata mediaMetadata, int i) {
            this(mediaMetadata);
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

        public Builder putText(String str, CharSequence charSequence) {
            if (MediaMetadata.METADATA_KEYS_TYPE.containsKey(str) && ((Integer) MediaMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 1) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a CharSequence");
            }
            this.mBundle.putCharSequence(str, charSequence);
            return this;
        }

        public Builder putString(String str, String str2) {
            if (MediaMetadata.METADATA_KEYS_TYPE.containsKey(str) && ((Integer) MediaMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 1) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a String");
            }
            this.mBundle.putCharSequence(str, str2);
            return this;
        }

        public Builder putLong(String str, long j) {
            if (MediaMetadata.METADATA_KEYS_TYPE.containsKey(str) && ((Integer) MediaMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 0) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a long");
            }
            this.mBundle.putLong(str, j);
            return this;
        }

        public Builder putRating(String str, Rating rating) {
            if (MediaMetadata.METADATA_KEYS_TYPE.containsKey(str) && ((Integer) MediaMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 3) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a Rating");
            }
            this.mBundle.putParcelable(str, rating);
            return this;
        }

        public Builder putBitmap(String str, Bitmap bitmap) {
            if (MediaMetadata.METADATA_KEYS_TYPE.containsKey(str) && ((Integer) MediaMetadata.METADATA_KEYS_TYPE.get(str)).intValue() != 2) {
                throw new IllegalArgumentException("The " + str + " key cannot be used to put a Bitmap");
            }
            this.mBundle.putParcelable(str, bitmap);
            return this;
        }

        public MediaMetadata build() {
            return new MediaMetadata(this.mBundle);
        }

        private Bitmap scaleBitmap(Bitmap bitmap, int i) {
            float f = i;
            float fMin = Math.min(f / bitmap.getWidth(), f / bitmap.getHeight());
            return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * fMin), (int) (bitmap.getHeight() * fMin), true);
        }
    }
}
