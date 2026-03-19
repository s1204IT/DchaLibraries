package android.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KeymasterCertificateChain implements Parcelable {
    public static final Parcelable.Creator<KeymasterCertificateChain> CREATOR = new Parcelable.Creator<KeymasterCertificateChain>() {
        @Override
        public KeymasterCertificateChain createFromParcel(Parcel parcel) {
            return new KeymasterCertificateChain(parcel);
        }

        @Override
        public KeymasterCertificateChain[] newArray(int i) {
            return new KeymasterCertificateChain[i];
        }
    };
    private List<byte[]> mCertificates;

    public KeymasterCertificateChain() {
        this.mCertificates = null;
    }

    public KeymasterCertificateChain(List<byte[]> list) {
        this.mCertificates = list;
    }

    private KeymasterCertificateChain(Parcel parcel) {
        readFromParcel(parcel);
    }

    public List<byte[]> getCertificates() {
        return this.mCertificates;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mCertificates == null) {
            parcel.writeInt(0);
            return;
        }
        parcel.writeInt(this.mCertificates.size());
        Iterator<byte[]> it = this.mCertificates.iterator();
        while (it.hasNext()) {
            parcel.writeByteArray(it.next());
        }
    }

    public void readFromParcel(Parcel parcel) {
        int i = parcel.readInt();
        this.mCertificates = new ArrayList(i);
        for (int i2 = 0; i2 < i; i2++) {
            this.mCertificates.add(parcel.createByteArray());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
