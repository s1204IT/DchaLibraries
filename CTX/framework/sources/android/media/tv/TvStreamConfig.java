package android.media.tv;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

@SystemApi
public class TvStreamConfig implements Parcelable {
    public static final int STREAM_TYPE_BUFFER_PRODUCER = 2;
    public static final int STREAM_TYPE_INDEPENDENT_VIDEO_SOURCE = 1;
    private int mGeneration;
    private int mMaxHeight;
    private int mMaxWidth;
    private int mStreamId;
    private int mType;
    static final String TAG = TvStreamConfig.class.getSimpleName();
    public static final Parcelable.Creator<TvStreamConfig> CREATOR = new Parcelable.Creator<TvStreamConfig>() {
        @Override
        public TvStreamConfig createFromParcel(Parcel parcel) {
            try {
                return new Builder().streamId(parcel.readInt()).type(parcel.readInt()).maxWidth(parcel.readInt()).maxHeight(parcel.readInt()).generation(parcel.readInt()).build();
            } catch (Exception e) {
                Log.e(TvStreamConfig.TAG, "Exception creating TvStreamConfig from parcel", e);
                return null;
            }
        }

        @Override
        public TvStreamConfig[] newArray(int i) {
            return new TvStreamConfig[i];
        }
    };

    private TvStreamConfig() {
    }

    public int getStreamId() {
        return this.mStreamId;
    }

    public int getType() {
        return this.mType;
    }

    public int getMaxWidth() {
        return this.mMaxWidth;
    }

    public int getMaxHeight() {
        return this.mMaxHeight;
    }

    public int getGeneration() {
        return this.mGeneration;
    }

    public String toString() {
        return "TvStreamConfig {mStreamId=" + this.mStreamId + ";mType=" + this.mType + ";mGeneration=" + this.mGeneration + "}";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mStreamId);
        parcel.writeInt(this.mType);
        parcel.writeInt(this.mMaxWidth);
        parcel.writeInt(this.mMaxHeight);
        parcel.writeInt(this.mGeneration);
    }

    public static final class Builder {
        private Integer mGeneration;
        private Integer mMaxHeight;
        private Integer mMaxWidth;
        private Integer mStreamId;
        private Integer mType;

        public Builder streamId(int i) {
            this.mStreamId = Integer.valueOf(i);
            return this;
        }

        public Builder type(int i) {
            this.mType = Integer.valueOf(i);
            return this;
        }

        public Builder maxWidth(int i) {
            this.mMaxWidth = Integer.valueOf(i);
            return this;
        }

        public Builder maxHeight(int i) {
            this.mMaxHeight = Integer.valueOf(i);
            return this;
        }

        public Builder generation(int i) {
            this.mGeneration = Integer.valueOf(i);
            return this;
        }

        public TvStreamConfig build() {
            if (this.mStreamId == null || this.mType == null || this.mMaxWidth == null || this.mMaxHeight == null || this.mGeneration == null) {
                throw new UnsupportedOperationException();
            }
            TvStreamConfig tvStreamConfig = new TvStreamConfig();
            tvStreamConfig.mStreamId = this.mStreamId.intValue();
            tvStreamConfig.mType = this.mType.intValue();
            tvStreamConfig.mMaxWidth = this.mMaxWidth.intValue();
            tvStreamConfig.mMaxHeight = this.mMaxHeight.intValue();
            tvStreamConfig.mGeneration = this.mGeneration.intValue();
            return tvStreamConfig;
        }
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TvStreamConfig)) {
            return false;
        }
        TvStreamConfig tvStreamConfig = (TvStreamConfig) obj;
        return tvStreamConfig.mGeneration == this.mGeneration && tvStreamConfig.mStreamId == this.mStreamId && tvStreamConfig.mType == this.mType && tvStreamConfig.mMaxWidth == this.mMaxWidth && tvStreamConfig.mMaxHeight == this.mMaxHeight;
    }
}
