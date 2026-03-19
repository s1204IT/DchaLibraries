package org.gsma.joyn.ipcall;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Arrays;
import org.gsma.joyn.Logger;

public class AudioCodec implements Parcelable {
    public static final Parcelable.Creator<AudioCodec> CREATOR = new Parcelable.Creator<AudioCodec>() {
        @Override
        public AudioCodec createFromParcel(Parcel source) {
            return new AudioCodec(source);
        }

        @Override
        public AudioCodec[] newArray(int size) {
            return new AudioCodec[size];
        }
    };
    public static final String TAG = "AudioCodec";
    private String encoding;
    private String parameters;
    private int payload;
    private int sampleRate;

    public AudioCodec(String encoding, int payload, int sampleRate, String parameters) {
        Logger.i(TAG, "AudioCodec entry encoding-" + encoding + "payload-sampleRate-" + sampleRate + "parameters-" + parameters);
        this.encoding = encoding;
        this.payload = payload;
        this.sampleRate = sampleRate;
        this.parameters = parameters;
    }

    public AudioCodec(Parcel source) {
        this.encoding = source.readString();
        this.payload = source.readInt();
        this.sampleRate = source.readInt();
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
        dest.writeInt(this.sampleRate);
        dest.writeString(this.parameters);
    }

    public String getEncoding() {
        Logger.i(TAG, "getEncoding value " + this.encoding);
        return this.encoding;
    }

    public int getPayloadType() {
        Logger.i(TAG, "getPayloadType value " + this.payload);
        return this.payload;
    }

    public int getSampleRate() {
        Logger.i(TAG, "getSampleRate value " + this.sampleRate);
        return this.sampleRate;
    }

    public String getParameters() {
        Logger.i(TAG, "getParameters value " + this.parameters);
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
        Logger.i(TAG, "getParameter key - " + key + " value -" + value);
        return value;
    }

    public boolean compare(AudioCodec codec) {
        boolean ret = false;
        if (getEncoding().equalsIgnoreCase(codec.getEncoding())) {
            ret = true;
        }
        Logger.i(TAG, "compare value " + ret);
        return ret;
    }
}
