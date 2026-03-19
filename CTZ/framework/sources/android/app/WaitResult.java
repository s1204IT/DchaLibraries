package android.app;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.PrintWriter;

public class WaitResult implements Parcelable {
    public static final Parcelable.Creator<WaitResult> CREATOR = new Parcelable.Creator<WaitResult>() {
        @Override
        public WaitResult createFromParcel(Parcel parcel) {
            return new WaitResult(parcel);
        }

        @Override
        public WaitResult[] newArray(int i) {
            return new WaitResult[i];
        }
    };
    public int result;
    public long thisTime;
    public boolean timeout;
    public long totalTime;
    public ComponentName who;

    public WaitResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.result);
        parcel.writeInt(this.timeout ? 1 : 0);
        ComponentName.writeToParcel(this.who, parcel);
        parcel.writeLong(this.thisTime);
        parcel.writeLong(this.totalTime);
    }

    private WaitResult(Parcel parcel) {
        this.result = parcel.readInt();
        this.timeout = parcel.readInt() != 0;
        this.who = ComponentName.readFromParcel(parcel);
        this.thisTime = parcel.readLong();
        this.totalTime = parcel.readLong();
    }

    public void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "WaitResult:");
        printWriter.println(str + "  result=" + this.result);
        printWriter.println(str + "  timeout=" + this.timeout);
        printWriter.println(str + "  who=" + this.who);
        printWriter.println(str + "  thisTime=" + this.thisTime);
        printWriter.println(str + "  totalTime=" + this.totalTime);
    }
}
