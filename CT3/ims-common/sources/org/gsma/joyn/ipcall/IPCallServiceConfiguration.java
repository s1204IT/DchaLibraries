package org.gsma.joyn.ipcall;

import android.os.Parcel;
import android.os.Parcelable;
import org.gsma.joyn.Logger;

public class IPCallServiceConfiguration implements Parcelable {
    public static final Parcelable.Creator<IPCallServiceConfiguration> CREATOR = new Parcelable.Creator<IPCallServiceConfiguration>() {
        @Override
        public IPCallServiceConfiguration createFromParcel(Parcel source) {
            return new IPCallServiceConfiguration(source);
        }

        @Override
        public IPCallServiceConfiguration[] newArray(int size) {
            return new IPCallServiceConfiguration[size];
        }
    };
    public static final String TAG = "IPCallServiceConfiguration";
    private boolean voiceBreakout;

    public IPCallServiceConfiguration(boolean voiceBreakout) {
        Logger.i(TAG, "IPCallServiceConfiguration entry" + voiceBreakout);
        this.voiceBreakout = voiceBreakout;
    }

    public IPCallServiceConfiguration(Parcel source) {
        this.voiceBreakout = source.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.voiceBreakout ? 1 : 0);
    }

    public boolean isVoiceCallBreakout() {
        Logger.i(TAG, "isVoiceCallBreakout value " + this.voiceBreakout);
        return this.voiceBreakout;
    }
}
