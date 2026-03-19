package org.gsma.joyn.vsh;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Arrays;
import org.gsma.joyn.H264Config;

public class VideoCodec implements Parcelable {
    public static final Parcelable.Creator<VideoCodec> CREATOR = new Parcelable.Creator<VideoCodec>() {
        @Override
        public VideoCodec createFromParcel(Parcel source) {
            return new VideoCodec(source);
        }

        @Override
        public VideoCodec[] newArray(int size) {
            return new VideoCodec[size];
        }
    };
    private int bitRate;
    private int clockRate;
    private String encoding;
    private int frameRate;
    private int height;
    private String parameters;
    private int payload;
    private int width;

    public VideoCodec(String encoding, int payload, int clockRate, int frameRate, int bitRate, int width, int height, String parameters) {
        this.encoding = encoding;
        this.payload = payload;
        this.clockRate = clockRate;
        this.frameRate = frameRate;
        this.bitRate = bitRate;
        this.width = width;
        this.height = height;
        this.parameters = parameters;
    }

    public VideoCodec(Parcel source) {
        this.encoding = source.readString();
        this.payload = source.readInt();
        this.clockRate = source.readInt();
        this.frameRate = source.readInt();
        this.bitRate = source.readInt();
        this.width = source.readInt();
        this.height = source.readInt();
        this.parameters = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.encoding);
        dest.writeInt(this.payload);
        dest.writeInt(this.clockRate);
        dest.writeInt(this.frameRate);
        dest.writeInt(this.bitRate);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeString(this.parameters);
    }

    public String getEncoding() {
        return this.encoding;
    }

    public int getPayloadType() {
        return this.payload;
    }

    public int getClockRate() {
        return this.clockRate;
    }

    public int getFrameRate() {
        return this.frameRate;
    }

    public int getBitRate() {
        return this.bitRate;
    }

    public int getVideoWidth() {
        return this.width;
    }

    public int getVideoHeight() {
        return this.height;
    }

    public String getParameters() {
        return this.parameters;
    }

    public String getParameter(String key) {
        String value = null;
        String[] parameters = getParameters().split(",");
        ArrayList<String> codecparams = new ArrayList<>(Arrays.asList(parameters));
        for (int i = 0; i < codecparams.size(); i++) {
            if (codecparams.get(i).startsWith(key)) {
                value = codecparams.get(i).substring(key.length() + 1);
            }
        }
        return value;
    }

    public boolean compare(VideoCodec codec) {
        if (!getEncoding().equalsIgnoreCase(codec.getEncoding())) {
            return false;
        }
        if (getVideoWidth() != codec.getVideoWidth() && getVideoWidth() != 0 && codec.getVideoWidth() != 0) {
            return false;
        }
        if (getVideoHeight() != codec.getVideoHeight() && getVideoHeight() != 0 && codec.getVideoHeight() != 0) {
            return false;
        }
        if (getEncoding().equalsIgnoreCase(H264Config.CODEC_NAME)) {
            if (H264Config.getCodecProfileLevelId(getParameters()).compareToIgnoreCase(H264Config.getCodecProfileLevelId(codec.getParameters())) != 0) {
                return false;
            }
            return true;
        }
        if (!getParameters().equalsIgnoreCase(codec.getParameters())) {
            return false;
        }
        return true;
    }
}
