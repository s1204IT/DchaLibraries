package android.media;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class AudioAttributes implements Parcelable {
    private static final int ALL_PARCEL_FLAGS = 1;
    private static final int ATTR_PARCEL_IS_NULL_BUNDLE = -1977;
    private static final int ATTR_PARCEL_IS_VALID_BUNDLE = 1980;
    public static final int CONTENT_TYPE_MOVIE = 3;
    public static final int CONTENT_TYPE_MUSIC = 2;
    public static final int CONTENT_TYPE_SONIFICATION = 4;
    public static final int CONTENT_TYPE_SPEECH = 1;
    public static final int CONTENT_TYPE_UNKNOWN = 0;
    public static final Parcelable.Creator<AudioAttributes> CREATOR;
    private static final int FLAG_ALL = 1023;
    private static final int FLAG_ALL_PUBLIC = 273;
    public static final int FLAG_AUDIBILITY_ENFORCED = 1;

    @SystemApi
    public static final int FLAG_BEACON = 8;

    @SystemApi
    public static final int FLAG_BYPASS_INTERRUPTION_POLICY = 64;

    @SystemApi
    public static final int FLAG_BYPASS_MUTE = 128;
    public static final int FLAG_DEEP_BUFFER = 512;
    public static final int FLAG_HW_AV_SYNC = 16;

    @SystemApi
    public static final int FLAG_HW_HOTWORD = 32;
    public static final int FLAG_LOW_LATENCY = 256;
    public static final int FLAG_SCO = 4;
    public static final int FLAG_SECURE = 2;
    public static final int FLATTEN_TAGS = 1;
    public static final int[] SDK_USAGES;
    public static final int SUPPRESSIBLE_ALARM = 4;
    public static final int SUPPRESSIBLE_CALL = 2;
    public static final int SUPPRESSIBLE_MEDIA = 5;
    public static final int SUPPRESSIBLE_NEVER = 3;
    public static final int SUPPRESSIBLE_NOTIFICATION = 1;
    public static final int SUPPRESSIBLE_SYSTEM = 6;
    public static final SparseIntArray SUPPRESSIBLE_USAGES = new SparseIntArray();
    private static final String TAG = "AudioAttributes";
    public static final int USAGE_ALARM = 4;
    public static final int USAGE_ASSISTANCE_ACCESSIBILITY = 11;
    public static final int USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12;
    public static final int USAGE_ASSISTANCE_SONIFICATION = 13;
    public static final int USAGE_ASSISTANT = 16;
    public static final int USAGE_GAME = 14;
    public static final int USAGE_MEDIA = 1;
    public static final int USAGE_NOTIFICATION = 5;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_DELAYED = 9;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_INSTANT = 8;
    public static final int USAGE_NOTIFICATION_COMMUNICATION_REQUEST = 7;
    public static final int USAGE_NOTIFICATION_EVENT = 10;
    public static final int USAGE_NOTIFICATION_RINGTONE = 6;
    public static final int USAGE_UNKNOWN = 0;
    public static final int USAGE_VIRTUAL_SOURCE = 15;
    public static final int USAGE_VOICE_COMMUNICATION = 2;
    public static final int USAGE_VOICE_COMMUNICATION_SIGNALLING = 3;
    private Bundle mBundle;
    private int mContentType;
    private int mFlags;
    private String mFormattedTags;
    private int mSource;
    private HashSet<String> mTags;
    private int mUsage;

    @Retention(RetentionPolicy.SOURCE)
    public @interface AttributeContentType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AttributeUsage {
    }

    static {
        SUPPRESSIBLE_USAGES.put(5, 1);
        SUPPRESSIBLE_USAGES.put(6, 2);
        SUPPRESSIBLE_USAGES.put(7, 2);
        SUPPRESSIBLE_USAGES.put(8, 1);
        SUPPRESSIBLE_USAGES.put(9, 1);
        SUPPRESSIBLE_USAGES.put(10, 1);
        SUPPRESSIBLE_USAGES.put(11, 3);
        SUPPRESSIBLE_USAGES.put(2, 3);
        SUPPRESSIBLE_USAGES.put(4, 4);
        SUPPRESSIBLE_USAGES.put(1, 5);
        SUPPRESSIBLE_USAGES.put(12, 5);
        SUPPRESSIBLE_USAGES.put(14, 5);
        SUPPRESSIBLE_USAGES.put(16, 5);
        SUPPRESSIBLE_USAGES.put(0, 5);
        SUPPRESSIBLE_USAGES.put(3, 6);
        SUPPRESSIBLE_USAGES.put(13, 6);
        SDK_USAGES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16};
        CREATOR = new Parcelable.Creator<AudioAttributes>() {
            @Override
            public AudioAttributes createFromParcel(Parcel parcel) {
                return new AudioAttributes(parcel);
            }

            @Override
            public AudioAttributes[] newArray(int i) {
                return new AudioAttributes[i];
            }
        };
    }

    private AudioAttributes() {
        this.mUsage = 0;
        this.mContentType = 0;
        this.mSource = -1;
        this.mFlags = 0;
    }

    public int getContentType() {
        return this.mContentType;
    }

    public int getUsage() {
        return this.mUsage;
    }

    @SystemApi
    public int getCapturePreset() {
        return this.mSource;
    }

    public int getFlags() {
        return this.mFlags & 273;
    }

    @SystemApi
    public int getAllFlags() {
        return this.mFlags & 1023;
    }

    @SystemApi
    public Bundle getBundle() {
        if (this.mBundle == null) {
            return this.mBundle;
        }
        return new Bundle(this.mBundle);
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(this.mTags);
    }

    public static class Builder {
        private Bundle mBundle;
        private int mContentType;
        private int mFlags;
        private int mSource;
        private HashSet<String> mTags;
        private int mUsage;

        public Builder() {
            this.mUsage = 0;
            this.mContentType = 0;
            this.mSource = -1;
            this.mFlags = 0;
            this.mTags = new HashSet<>();
        }

        public Builder(AudioAttributes audioAttributes) {
            this.mUsage = 0;
            this.mContentType = 0;
            this.mSource = -1;
            this.mFlags = 0;
            this.mTags = new HashSet<>();
            this.mUsage = audioAttributes.mUsage;
            this.mContentType = audioAttributes.mContentType;
            this.mFlags = audioAttributes.mFlags;
            this.mTags = (HashSet) audioAttributes.mTags.clone();
        }

        public AudioAttributes build() {
            AudioAttributes audioAttributes = new AudioAttributes();
            audioAttributes.mContentType = this.mContentType;
            audioAttributes.mUsage = this.mUsage;
            audioAttributes.mSource = this.mSource;
            audioAttributes.mFlags = this.mFlags;
            audioAttributes.mTags = (HashSet) this.mTags.clone();
            audioAttributes.mFormattedTags = TextUtils.join(";", this.mTags);
            if (this.mBundle != null) {
                audioAttributes.mBundle = new Bundle(this.mBundle);
            }
            return audioAttributes;
        }

        public Builder setUsage(int i) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    this.mUsage = i;
                    return this;
                default:
                    this.mUsage = 0;
                    return this;
            }
        }

        public Builder setContentType(int i) {
            switch (i) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    this.mContentType = i;
                    return this;
                default:
                    this.mUsage = 0;
                    return this;
            }
        }

        public Builder setFlags(int i) {
            this.mFlags = (i & 1023) | this.mFlags;
            return this;
        }

        public Builder replaceFlags(int i) {
            this.mFlags = i & 1023;
            return this;
        }

        @SystemApi
        public Builder addBundle(Bundle bundle) {
            if (bundle == null) {
                throw new IllegalArgumentException("Illegal null bundle");
            }
            if (this.mBundle == null) {
                this.mBundle = new Bundle(bundle);
            } else {
                this.mBundle.putAll(bundle);
            }
            return this;
        }

        public Builder addTag(String str) {
            this.mTags.add(str);
            return this;
        }

        public Builder setLegacyStreamType(int i) {
            if (i == 10) {
                throw new IllegalArgumentException("STREAM_ACCESSIBILITY is not a legacy stream type that was used for audio playback");
            }
            return setInternalLegacyStreamType(i);
        }

        public Builder setInternalLegacyStreamType(int i) {
            switch (i) {
                case 0:
                    this.mContentType = 1;
                    break;
                case 1:
                    this.mContentType = 4;
                    break;
                case 2:
                    this.mContentType = 4;
                    break;
                case 3:
                    this.mContentType = 2;
                    break;
                case 4:
                    this.mContentType = 4;
                    break;
                case 5:
                    this.mContentType = 4;
                    break;
                case 6:
                    this.mContentType = 1;
                    this.mFlags |= 4;
                    break;
                case 7:
                    this.mFlags = 1 | this.mFlags;
                    this.mContentType = 4;
                    break;
                case 8:
                    this.mContentType = 4;
                    break;
                case 9:
                    this.mContentType = 4;
                    break;
                case 10:
                    this.mContentType = 1;
                    break;
                default:
                    Log.e(AudioAttributes.TAG, "Invalid stream type " + i + " for AudioAttributes");
                    break;
            }
            this.mUsage = AudioAttributes.usageForStreamType(i);
            return this;
        }

        @SystemApi
        public Builder setCapturePreset(int i) {
            switch (i) {
                case 0:
                case 1:
                case 5:
                case 6:
                case 7:
                case 9:
                    this.mSource = i;
                    return this;
                case 2:
                case 3:
                case 4:
                case 8:
                default:
                    Log.e(AudioAttributes.TAG, "Invalid capture preset " + i + " for AudioAttributes");
                    return this;
            }
        }

        @SystemApi
        public Builder setInternalCapturePreset(int i) {
            if (i == 1999 || i == 8 || i == 1998 || i == 3 || i == 2 || i == 4) {
                this.mSource = i;
            } else {
                setCapturePreset(i);
            }
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mUsage);
        parcel.writeInt(this.mContentType);
        parcel.writeInt(this.mSource);
        parcel.writeInt(this.mFlags);
        int i2 = i & 1;
        parcel.writeInt(i2);
        if (i2 != 0) {
            if (i2 == 1) {
                parcel.writeString(this.mFormattedTags);
            }
        } else {
            String[] strArr = new String[this.mTags.size()];
            this.mTags.toArray(strArr);
            parcel.writeStringArray(strArr);
        }
        if (this.mBundle == null) {
            parcel.writeInt(ATTR_PARCEL_IS_NULL_BUNDLE);
        } else {
            parcel.writeInt(ATTR_PARCEL_IS_VALID_BUNDLE);
            parcel.writeBundle(this.mBundle);
        }
    }

    private AudioAttributes(Parcel parcel) {
        this.mUsage = 0;
        this.mContentType = 0;
        this.mSource = -1;
        this.mFlags = 0;
        this.mUsage = parcel.readInt();
        this.mContentType = parcel.readInt();
        this.mSource = parcel.readInt();
        this.mFlags = parcel.readInt();
        boolean z = (parcel.readInt() & 1) == 1;
        this.mTags = new HashSet<>();
        if (z) {
            this.mFormattedTags = new String(parcel.readString());
            this.mTags.add(this.mFormattedTags);
        } else {
            String[] stringArray = parcel.readStringArray();
            for (int length = stringArray.length - 1; length >= 0; length--) {
                this.mTags.add(stringArray[length]);
            }
            this.mFormattedTags = TextUtils.join(";", this.mTags);
        }
        int i = parcel.readInt();
        if (i == ATTR_PARCEL_IS_NULL_BUNDLE) {
            this.mBundle = null;
        } else if (i == ATTR_PARCEL_IS_VALID_BUNDLE) {
            this.mBundle = new Bundle(parcel.readBundle());
        } else {
            Log.e(TAG, "Illegal value unmarshalling AudioAttributes, can't initialize bundle");
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AudioAttributes audioAttributes = (AudioAttributes) obj;
        if (this.mContentType == audioAttributes.mContentType && this.mFlags == audioAttributes.mFlags && this.mSource == audioAttributes.mSource && this.mUsage == audioAttributes.mUsage && this.mFormattedTags.equals(audioAttributes.mFormattedTags)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mContentType), Integer.valueOf(this.mFlags), Integer.valueOf(this.mSource), Integer.valueOf(this.mUsage), this.mFormattedTags, this.mBundle);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AudioAttributes: usage=");
        sb.append(usageToString());
        sb.append(" content=");
        sb.append(contentTypeToString());
        sb.append(" flags=0x");
        sb.append(Integer.toHexString(this.mFlags).toUpperCase());
        sb.append(" tags=");
        sb.append(this.mFormattedTags);
        sb.append(" bundle=");
        sb.append(this.mBundle == null ? "null" : this.mBundle.toString());
        return new String(sb.toString());
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1159641169921L, this.mUsage);
        protoOutputStream.write(1159641169922L, this.mContentType);
        protoOutputStream.write(1120986464259L, this.mFlags);
        for (String str : this.mFormattedTags.split(";")) {
            String strTrim = str.trim();
            if (strTrim != "") {
                protoOutputStream.write(2237677961220L, strTrim);
            }
        }
        protoOutputStream.end(jStart);
    }

    public String usageToString() {
        return usageToString(this.mUsage);
    }

    public static String usageToString(int i) {
        switch (i) {
            case 0:
                return new String("USAGE_UNKNOWN");
            case 1:
                return new String("USAGE_MEDIA");
            case 2:
                return new String("USAGE_VOICE_COMMUNICATION");
            case 3:
                return new String("USAGE_VOICE_COMMUNICATION_SIGNALLING");
            case 4:
                return new String("USAGE_ALARM");
            case 5:
                return new String("USAGE_NOTIFICATION");
            case 6:
                return new String("USAGE_NOTIFICATION_RINGTONE");
            case 7:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_REQUEST");
            case 8:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_INSTANT");
            case 9:
                return new String("USAGE_NOTIFICATION_COMMUNICATION_DELAYED");
            case 10:
                return new String("USAGE_NOTIFICATION_EVENT");
            case 11:
                return new String("USAGE_ASSISTANCE_ACCESSIBILITY");
            case 12:
                return new String("USAGE_ASSISTANCE_NAVIGATION_GUIDANCE");
            case 13:
                return new String("USAGE_ASSISTANCE_SONIFICATION");
            case 14:
                return new String("USAGE_GAME");
            case 15:
            default:
                return new String("unknown usage " + i);
            case 16:
                return new String("USAGE_ASSISTANT");
        }
    }

    public String contentTypeToString() {
        switch (this.mContentType) {
            case 0:
                return new String("CONTENT_TYPE_UNKNOWN");
            case 1:
                return new String("CONTENT_TYPE_SPEECH");
            case 2:
                return new String("CONTENT_TYPE_MUSIC");
            case 3:
                return new String("CONTENT_TYPE_MOVIE");
            case 4:
                return new String("CONTENT_TYPE_SONIFICATION");
            default:
                return new String("unknown content type " + this.mContentType);
        }
    }

    private static int usageForStreamType(int i) {
        switch (i) {
        }
        return 2;
    }

    public int getVolumeControlStream() {
        return toVolumeStreamType(true, this);
    }

    public static int toLegacyStreamType(AudioAttributes audioAttributes) {
        return toVolumeStreamType(false, audioAttributes);
    }

    private static int toVolumeStreamType(boolean z, AudioAttributes audioAttributes) {
        if ((audioAttributes.getFlags() & 1) == 1) {
            return z ? 1 : 7;
        }
        if ((audioAttributes.getFlags() & 4) == 4) {
            return z ? 0 : 6;
        }
        switch (audioAttributes.getUsage()) {
            case 0:
                return 3;
            case 1:
            case 12:
            case 14:
            case 16:
                return 3;
            case 2:
                return 0;
            case 3:
                return z ? 0 : 8;
            case 4:
                return 4;
            case 5:
            case 7:
            case 8:
            case 9:
            case 10:
                return 5;
            case 6:
                return 2;
            case 11:
                return 10;
            case 13:
                return 1;
            case 15:
            default:
                if (!z) {
                    return 3;
                }
                throw new IllegalArgumentException("Unknown usage value " + audioAttributes.getUsage() + " in audio attributes");
        }
    }
}
