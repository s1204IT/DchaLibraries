package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class ApduList implements Parcelable {
    public static final Parcelable.Creator<ApduList> CREATOR = new Parcelable.Creator<ApduList>() {
        @Override
        public ApduList createFromParcel(Parcel in) {
            return new ApduList(in);
        }

        @Override
        public ApduList[] newArray(int size) {
            return new ApduList[size];
        }
    };
    private ArrayList<byte[]> commands;

    public ApduList() {
        this.commands = new ArrayList<>();
    }

    public void add(byte[] command) {
        this.commands.add(command);
    }

    public List<byte[]> get() {
        return this.commands;
    }

    private ApduList(Parcel in) {
        this.commands = new ArrayList<>();
        int count = in.readInt();
        for (int i = 0; i < count; i++) {
            int length = in.readInt();
            byte[] cmd = new byte[length];
            in.readByteArray(cmd);
            this.commands.add(cmd);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.commands.size());
        for (byte[] cmd : this.commands) {
            dest.writeInt(cmd.length);
            dest.writeByteArray(cmd);
        }
    }
}
