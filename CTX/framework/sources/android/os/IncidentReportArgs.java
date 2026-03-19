package android.os;

import android.annotation.SystemApi;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcelable;
import android.util.IntArray;
import java.util.ArrayList;

@SystemApi
public final class IncidentReportArgs implements Parcelable {
    public static final Parcelable.Creator<IncidentReportArgs> CREATOR = new Parcelable.Creator<IncidentReportArgs>() {
        @Override
        public IncidentReportArgs createFromParcel(Parcel parcel) {
            return new IncidentReportArgs(parcel);
        }

        @Override
        public IncidentReportArgs[] newArray(int i) {
            return new IncidentReportArgs[i];
        }
    };
    private static final int DEST_AUTO = 200;
    private static final int DEST_EXPLICIT = 100;
    private boolean mAll;
    private int mDest;
    private final ArrayList<byte[]> mHeaders;
    private final IntArray mSections;

    public IncidentReportArgs() {
        this.mSections = new IntArray();
        this.mHeaders = new ArrayList<>();
        this.mDest = 200;
    }

    public IncidentReportArgs(Parcel parcel) {
        this.mSections = new IntArray();
        this.mHeaders = new ArrayList<>();
        readFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAll ? 1 : 0);
        int size = this.mSections.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            parcel.writeInt(this.mSections.get(i2));
        }
        int size2 = this.mHeaders.size();
        parcel.writeInt(size2);
        for (int i3 = 0; i3 < size2; i3++) {
            parcel.writeByteArray(this.mHeaders.get(i3));
        }
        parcel.writeInt(this.mDest);
    }

    public void readFromParcel(Parcel parcel) {
        this.mAll = parcel.readInt() != 0;
        this.mSections.clear();
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            this.mSections.add(parcel.readInt());
        }
        this.mHeaders.clear();
        int i3 = parcel.readInt();
        for (int i4 = 0; i4 < i3; i4++) {
            this.mHeaders.add(parcel.createByteArray());
        }
        this.mDest = parcel.readInt();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Incident(");
        if (this.mAll) {
            sb.append("all");
        } else {
            int size = this.mSections.size();
            if (size > 0) {
                sb.append(this.mSections.get(0));
            }
            for (int i = 1; i < size; i++) {
                sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                sb.append(this.mSections.get(i));
            }
        }
        sb.append(", ");
        sb.append(this.mHeaders.size());
        sb.append(" headers), ");
        sb.append("Dest enum value: ");
        sb.append(this.mDest);
        return sb.toString();
    }

    public void setAll(boolean z) {
        this.mAll = z;
        if (z) {
            this.mSections.clear();
        }
    }

    public void setPrivacyPolicy(int i) {
        if (i == 100 || i == 200) {
            this.mDest = i;
        } else {
            this.mDest = 200;
        }
    }

    public void addSection(int i) {
        if (!this.mAll && i > 1) {
            this.mSections.add(i);
        }
    }

    public boolean isAll() {
        return this.mAll;
    }

    public boolean containsSection(int i) {
        return this.mAll || this.mSections.indexOf(i) >= 0;
    }

    public int sectionCount() {
        return this.mSections.size();
    }

    public void addHeader(byte[] bArr) {
        this.mHeaders.add(bArr);
    }
}
