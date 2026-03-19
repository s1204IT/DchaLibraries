package android.media;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
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
    public static final Parcelable.Creator<AudioAttributes> CREATOR = new Parcelable.Creator<AudioAttributes>() {
        @Override
        public AudioAttributes createFromParcel(Parcel p) {
            return new AudioAttributes(p, null);
        }

        @Override
        public AudioAttributes[] newArray(int size) {
            return new AudioAttributes[size];
        }
    };
    private static final int FLAG_ALL = 511;
    private static final int FLAG_ALL_PUBLIC = 273;
    public static final int FLAG_AUDIBILITY_ENFORCED = 1;
    public static final int FLAG_BEACON = 8;
    public static final int FLAG_BYPASS_INTERRUPTION_POLICY = 64;
    public static final int FLAG_BYPASS_MUTE = 128;
    public static final int FLAG_HW_AV_SYNC = 16;
    public static final int FLAG_HW_HOTWORD = 32;
    public static final int FLAG_LOW_LATENCY = 256;
    public static final int FLAG_SCO = 4;
    public static final int FLAG_SECURE = 2;
    public static final int FLATTEN_TAGS = 1;
    private static final String TAG = "AudioAttributes";
    public static final int USAGE_ALARM = 4;
    public static final int USAGE_ASSISTANCE_ACCESSIBILITY = 11;
    public static final int USAGE_ASSISTANCE_NAVIGATION_GUIDANCE = 12;
    public static final int USAGE_ASSISTANCE_SONIFICATION = 13;
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

    AudioAttributes(AudioAttributes audioAttributes) {
        this();
    }

    AudioAttributes(Parcel in, AudioAttributes audioAttributes) {
        this(in);
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

    public int getCapturePreset() {
        return this.mSource;
    }

    public int getFlags() {
        return this.mFlags & FLAG_ALL_PUBLIC;
    }

    public int getAllFlags() {
        return this.mFlags & 511;
    }

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

        public Builder(AudioAttributes aa) {
            this.mUsage = 0;
            this.mContentType = 0;
            this.mSource = -1;
            this.mFlags = 0;
            this.mTags = new HashSet<>();
            this.mUsage = aa.mUsage;
            this.mContentType = aa.mContentType;
            this.mFlags = aa.mFlags;
            this.mTags = (HashSet) aa.mTags.clone();
        }

        public AudioAttributes build() {
            AudioAttributes aa = new AudioAttributes((AudioAttributes) null);
            aa.mContentType = this.mContentType;
            aa.mUsage = this.mUsage;
            aa.mSource = this.mSource;
            aa.mFlags = this.mFlags;
            aa.mTags = (HashSet) this.mTags.clone();
            aa.mFormattedTags = TextUtils.join(";", this.mTags);
            if (this.mBundle != null) {
                aa.mBundle = new Bundle(this.mBundle);
            }
            return aa;
        }

        public Builder setUsage(int usage) {
            switch (usage) {
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
                    this.mUsage = usage;
                    return this;
                default:
                    this.mUsage = 0;
                    return this;
            }
        }

        public Builder setContentType(int contentType) {
            switch (contentType) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    this.mContentType = contentType;
                    return this;
                default:
                    this.mUsage = 0;
                    return this;
            }
        }

        public Builder setFlags(int flags) {
            this.mFlags |= flags & 511;
            return this;
        }

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

        public Builder addTag(String tag) {
            this.mTags.add(tag);
            return this;
        }

        public Builder setLegacyStreamType(int streamType) {
            return setInternalLegacyStreamType(streamType);
        }

        public Builder setInternalLegacyStreamType(int streamType) {
            switch (streamType) {
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
                    this.mFlags |= 1;
                    this.mContentType = 4;
                    break;
                case 8:
                    this.mContentType = 4;
                    break;
                case 9:
                    this.mContentType = 1;
                    break;
                default:
                    Log.e(AudioAttributes.TAG, "Invalid stream type " + streamType + " for AudioAttributes");
                    break;
            }
            this.mUsage = AudioAttributes.usageForLegacyStreamType(streamType);
            return this;
        }

        public Builder setCapturePreset(int preset) {
            switch (preset) {
                case 0:
                case 1:
                case 5:
                case 6:
                case 7:
                case 9:
                    this.mSource = preset;
                    return this;
                case 2:
                case 3:
                case 4:
                case 8:
                default:
                    Log.e(AudioAttributes.TAG, "Invalid capture preset " + preset + " for AudioAttributes");
                    return this;
            }
        }

        public Builder setInternalCapturePreset(int preset) {
            if (preset == 1999 || preset == 8 || preset == 1998) {
                this.mSource = preset;
            } else {
                setCapturePreset(preset);
            }
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mUsage);
        dest.writeInt(this.mContentType);
        dest.writeInt(this.mSource);
        dest.writeInt(this.mFlags);
        dest.writeInt(flags & 1);
        if ((flags & 1) == 0) {
            String[] tagsArray = new String[this.mTags.size()];
            this.mTags.toArray(tagsArray);
            dest.writeStringArray(tagsArray);
        } else if ((flags & 1) == 1) {
            dest.writeString(this.mFormattedTags);
        }
        if (this.mBundle == null) {
            dest.writeInt(ATTR_PARCEL_IS_NULL_BUNDLE);
        } else {
            dest.writeInt(ATTR_PARCEL_IS_VALID_BUNDLE);
            dest.writeBundle(this.mBundle);
        }
    }

    private AudioAttributes(Parcel in) {
        this.mUsage = 0;
        this.mContentType = 0;
        this.mSource = -1;
        this.mFlags = 0;
        this.mUsage = in.readInt();
        this.mContentType = in.readInt();
        this.mSource = in.readInt();
        this.mFlags = in.readInt();
        boolean hasFlattenedTags = (in.readInt() & 1) == 1;
        this.mTags = new HashSet<>();
        if (hasFlattenedTags) {
            this.mFormattedTags = new String(in.readString());
            this.mTags.add(this.mFormattedTags);
        } else {
            String[] tagsArray = in.readStringArray();
            for (int i = tagsArray.length - 1; i >= 0; i--) {
                this.mTags.add(tagsArray[i]);
            }
            this.mFormattedTags = TextUtils.join(";", this.mTags);
        }
        switch (in.readInt()) {
            case ATTR_PARCEL_IS_NULL_BUNDLE:
                this.mBundle = null;
                break;
            case ATTR_PARCEL_IS_VALID_BUNDLE:
                this.mBundle = new Bundle(in.readBundle());
                break;
            default:
                Log.e(TAG, "Illegal value unmarshalling AudioAttributes, can't initialize bundle");
                break;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudioAttributes that = (AudioAttributes) o;
        if (this.mContentType == that.mContentType && this.mFlags == that.mFlags && this.mSource == that.mSource && this.mUsage == that.mUsage) {
            return this.mFormattedTags.equals(that.mFormattedTags);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mContentType), Integer.valueOf(this.mFlags), Integer.valueOf(this.mSource), Integer.valueOf(this.mUsage), this.mFormattedTags, this.mBundle);
    }

    public String toString() {
        return new String("AudioAttributes: usage=" + this.mUsage + " content=" + this.mContentType + " flags=0x" + Integer.toHexString(this.mFlags).toUpperCase() + " tags=" + this.mFormattedTags + " bundle=" + (this.mBundle == null ? "null" : this.mBundle.toString()));
    }

    public String usageToString() {
        return usageToString(this.mUsage);
    }

    public static String usageToString(int usage) {
        switch (usage) {
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
            default:
                return new String("unknown usage " + usage);
        }
    }

    public static int usageForLegacyStreamType(int streamType) {
        switch (streamType) {
        }
        return 2;
    }

    public static int getVolumeControlStream(AudioAttributes aa) {
        if (aa == null) {
            throw new IllegalArgumentException("Invalid null audio attributes");
        }
        return toVolumeStreamType(true, aa);
    }

    public static int toLegacyStreamType(AudioAttributes aa) {
        return toVolumeStreamType(false, aa);
    }

    private static int toVolumeStreamType(boolean fromGetVolumeControlStream, AudioAttributes aa) {
        if ((aa.getFlags() & 1) == 1) {
            return fromGetVolumeControlStream ? 1 : 7;
        }
        if ((aa.getFlags() & 4) == 4) {
            return fromGetVolumeControlStream ? 0 : 6;
        }
        switch (aa.getUsage()) {
            case 0:
                return fromGetVolumeControlStream ? Integer.MIN_VALUE : 3;
            case 1:
            case 11:
            case 12:
            case 14:
                return 3;
            case 2:
                return 0;
            case 3:
                return fromGetVolumeControlStream ? 0 : 8;
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
            case 13:
                return 1;
            default:
                if (fromGetVolumeControlStream) {
                    throw new IllegalArgumentException("Unknown usage value " + aa.getUsage() + " in audio attributes");
                }
                return 3;
        }
    }
}
