package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Collection;

public class ScanSettings implements Parcelable {
    public static final Parcelable.Creator<ScanSettings> CREATOR = new Parcelable.Creator<ScanSettings>() {
        @Override
        public ScanSettings createFromParcel(Parcel in) {
            ScanSettings settings = new ScanSettings();
            int size = in.readInt();
            if (size > 0) {
                settings.channelSet = new ArrayList(size);
                while (true) {
                    int size2 = size;
                    size = size2 - 1;
                    if (size2 <= 0) {
                        break;
                    }
                    settings.channelSet.add(WifiChannel.CREATOR.createFromParcel(in));
                }
            }
            return settings;
        }

        @Override
        public ScanSettings[] newArray(int size) {
            return new ScanSettings[size];
        }
    };
    public Collection<WifiChannel> channelSet;

    public ScanSettings() {
    }

    public ScanSettings(ScanSettings source) {
        if (source.channelSet == null) {
            return;
        }
        this.channelSet = new ArrayList(source.channelSet);
    }

    public boolean isValid() {
        for (WifiChannel channel : this.channelSet) {
            if (!channel.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.channelSet == null ? 0 : this.channelSet.size());
        if (this.channelSet == null) {
            return;
        }
        for (WifiChannel channel : this.channelSet) {
            channel.writeToParcel(out, flags);
        }
    }
}
