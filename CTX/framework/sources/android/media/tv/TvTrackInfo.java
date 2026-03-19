package android.media.tv;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public final class TvTrackInfo implements Parcelable {
    public static final Parcelable.Creator<TvTrackInfo> CREATOR = new Parcelable.Creator<TvTrackInfo>() {
        @Override
        public TvTrackInfo createFromParcel(Parcel parcel) {
            return new TvTrackInfo(parcel);
        }

        @Override
        public TvTrackInfo[] newArray(int i) {
            return new TvTrackInfo[i];
        }
    };
    public static final int TYPE_AUDIO = 0;
    public static final int TYPE_SUBTITLE = 2;
    public static final int TYPE_VIDEO = 1;
    private final int mAudioChannelCount;
    private final int mAudioSampleRate;
    private final CharSequence mDescription;
    private final Bundle mExtra;
    private final String mId;
    private final String mLanguage;
    private final int mType;
    private final byte mVideoActiveFormatDescription;
    private final float mVideoFrameRate;
    private final int mVideoHeight;
    private final float mVideoPixelAspectRatio;
    private final int mVideoWidth;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    private TvTrackInfo(int i, String str, String str2, CharSequence charSequence, int i2, int i3, int i4, int i5, float f, float f2, byte b, Bundle bundle) {
        this.mType = i;
        this.mId = str;
        this.mLanguage = str2;
        this.mDescription = charSequence;
        this.mAudioChannelCount = i2;
        this.mAudioSampleRate = i3;
        this.mVideoWidth = i4;
        this.mVideoHeight = i5;
        this.mVideoFrameRate = f;
        this.mVideoPixelAspectRatio = f2;
        this.mVideoActiveFormatDescription = b;
        this.mExtra = bundle;
    }

    private TvTrackInfo(Parcel parcel) {
        this.mType = parcel.readInt();
        this.mId = parcel.readString();
        this.mLanguage = parcel.readString();
        this.mDescription = parcel.readString();
        this.mAudioChannelCount = parcel.readInt();
        this.mAudioSampleRate = parcel.readInt();
        this.mVideoWidth = parcel.readInt();
        this.mVideoHeight = parcel.readInt();
        this.mVideoFrameRate = parcel.readFloat();
        this.mVideoPixelAspectRatio = parcel.readFloat();
        this.mVideoActiveFormatDescription = parcel.readByte();
        this.mExtra = parcel.readBundle();
    }

    public final int getType() {
        return this.mType;
    }

    public final String getId() {
        return this.mId;
    }

    public final String getLanguage() {
        return this.mLanguage;
    }

    public final CharSequence getDescription() {
        return this.mDescription;
    }

    public final int getAudioChannelCount() {
        if (this.mType != 0) {
            throw new IllegalStateException("Not an audio track");
        }
        return this.mAudioChannelCount;
    }

    public final int getAudioSampleRate() {
        if (this.mType != 0) {
            throw new IllegalStateException("Not an audio track");
        }
        return this.mAudioSampleRate;
    }

    public final int getVideoWidth() {
        if (this.mType != 1) {
            throw new IllegalStateException("Not a video track");
        }
        return this.mVideoWidth;
    }

    public final int getVideoHeight() {
        if (this.mType != 1) {
            throw new IllegalStateException("Not a video track");
        }
        return this.mVideoHeight;
    }

    public final float getVideoFrameRate() {
        if (this.mType != 1) {
            throw new IllegalStateException("Not a video track");
        }
        return this.mVideoFrameRate;
    }

    public final float getVideoPixelAspectRatio() {
        if (this.mType != 1) {
            throw new IllegalStateException("Not a video track");
        }
        return this.mVideoPixelAspectRatio;
    }

    public final byte getVideoActiveFormatDescription() {
        if (this.mType != 1) {
            throw new IllegalStateException("Not a video track");
        }
        return this.mVideoActiveFormatDescription;
    }

    public final Bundle getExtra() {
        return this.mExtra;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mType);
        parcel.writeString(this.mId);
        parcel.writeString(this.mLanguage);
        parcel.writeString(this.mDescription != null ? this.mDescription.toString() : null);
        parcel.writeInt(this.mAudioChannelCount);
        parcel.writeInt(this.mAudioSampleRate);
        parcel.writeInt(this.mVideoWidth);
        parcel.writeInt(this.mVideoHeight);
        parcel.writeFloat(this.mVideoFrameRate);
        parcel.writeFloat(this.mVideoPixelAspectRatio);
        parcel.writeByte(this.mVideoActiveFormatDescription);
        parcel.writeBundle(this.mExtra);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TvTrackInfo)) {
            return false;
        }
        TvTrackInfo tvTrackInfo = (TvTrackInfo) obj;
        if (TextUtils.equals(this.mId, tvTrackInfo.mId) && this.mType == tvTrackInfo.mType && TextUtils.equals(this.mLanguage, tvTrackInfo.mLanguage) && TextUtils.equals(this.mDescription, tvTrackInfo.mDescription) && Objects.equals(this.mExtra, tvTrackInfo.mExtra)) {
            if (this.mType == 0) {
                if (this.mAudioChannelCount == tvTrackInfo.mAudioChannelCount && this.mAudioSampleRate == tvTrackInfo.mAudioSampleRate) {
                    return true;
                }
            } else {
                if (this.mType != 1) {
                    return true;
                }
                if (this.mVideoWidth == tvTrackInfo.mVideoWidth && this.mVideoHeight == tvTrackInfo.mVideoHeight && this.mVideoFrameRate == tvTrackInfo.mVideoFrameRate && this.mVideoPixelAspectRatio == tvTrackInfo.mVideoPixelAspectRatio) {
                    return true;
                }
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hashCode(this.mId);
    }

    public static final class Builder {
        private int mAudioChannelCount;
        private int mAudioSampleRate;
        private CharSequence mDescription;
        private Bundle mExtra;
        private final String mId;
        private String mLanguage;
        private final int mType;
        private byte mVideoActiveFormatDescription;
        private float mVideoFrameRate;
        private int mVideoHeight;
        private float mVideoPixelAspectRatio = 1.0f;
        private int mVideoWidth;

        public Builder(int i, String str) {
            if (i != 0 && i != 1 && i != 2) {
                throw new IllegalArgumentException("Unknown type: " + i);
            }
            Preconditions.checkNotNull(str);
            this.mType = i;
            this.mId = str;
        }

        public final Builder setLanguage(String str) {
            this.mLanguage = str;
            return this;
        }

        public final Builder setDescription(CharSequence charSequence) {
            this.mDescription = charSequence;
            return this;
        }

        public final Builder setAudioChannelCount(int i) {
            if (this.mType != 0) {
                throw new IllegalStateException("Not an audio track");
            }
            this.mAudioChannelCount = i;
            return this;
        }

        public final Builder setAudioSampleRate(int i) {
            if (this.mType != 0) {
                throw new IllegalStateException("Not an audio track");
            }
            this.mAudioSampleRate = i;
            return this;
        }

        public final Builder setVideoWidth(int i) {
            if (this.mType != 1) {
                throw new IllegalStateException("Not a video track");
            }
            this.mVideoWidth = i;
            return this;
        }

        public final Builder setVideoHeight(int i) {
            if (this.mType != 1) {
                throw new IllegalStateException("Not a video track");
            }
            this.mVideoHeight = i;
            return this;
        }

        public final Builder setVideoFrameRate(float f) {
            if (this.mType != 1) {
                throw new IllegalStateException("Not a video track");
            }
            this.mVideoFrameRate = f;
            return this;
        }

        public final Builder setVideoPixelAspectRatio(float f) {
            if (this.mType != 1) {
                throw new IllegalStateException("Not a video track");
            }
            this.mVideoPixelAspectRatio = f;
            return this;
        }

        public final Builder setVideoActiveFormatDescription(byte b) {
            if (this.mType != 1) {
                throw new IllegalStateException("Not a video track");
            }
            this.mVideoActiveFormatDescription = b;
            return this;
        }

        public final Builder setExtra(Bundle bundle) {
            this.mExtra = new Bundle(bundle);
            return this;
        }

        public TvTrackInfo build() {
            return new TvTrackInfo(this.mType, this.mId, this.mLanguage, this.mDescription, this.mAudioChannelCount, this.mAudioSampleRate, this.mVideoWidth, this.mVideoHeight, this.mVideoFrameRate, this.mVideoPixelAspectRatio, this.mVideoActiveFormatDescription, this.mExtra);
        }
    }
}
