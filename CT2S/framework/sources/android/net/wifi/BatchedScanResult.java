package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

public class BatchedScanResult implements Parcelable {
    public static final Parcelable.Creator<BatchedScanResult> CREATOR = new Parcelable.Creator<BatchedScanResult>() {
        @Override
        public BatchedScanResult createFromParcel(Parcel in) {
            BatchedScanResult result = new BatchedScanResult();
            result.truncated = in.readInt() == 1;
            int count = in.readInt();
            while (true) {
                int count2 = count;
                count = count2 - 1;
                if (count2 > 0) {
                    result.scanResults.add(ScanResult.CREATOR.createFromParcel(in));
                } else {
                    return result;
                }
            }
        }

        @Override
        public BatchedScanResult[] newArray(int size) {
            return new BatchedScanResult[size];
        }
    };
    private static final String TAG = "BatchedScanResult";
    public final List<ScanResult> scanResults = new ArrayList();
    public boolean truncated;

    public BatchedScanResult() {
    }

    public BatchedScanResult(BatchedScanResult source) {
        this.truncated = source.truncated;
        for (ScanResult s : source.scanResults) {
            this.scanResults.add(new ScanResult(s));
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("BatchedScanResult: ").append("truncated: ").append(String.valueOf(this.truncated)).append("scanResults: [");
        for (ScanResult s : this.scanResults) {
            sb.append(" <").append(s.toString()).append("> ");
        }
        sb.append(" ]");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.truncated ? 1 : 0);
        dest.writeInt(this.scanResults.size());
        for (ScanResult s : this.scanResults) {
            s.writeToParcel(dest, flags);
        }
    }
}
