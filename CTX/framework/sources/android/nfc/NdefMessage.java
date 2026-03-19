package android.nfc;

import android.os.Parcel;
import android.os.Parcelable;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class NdefMessage implements Parcelable {
    public static final Parcelable.Creator<NdefMessage> CREATOR = new Parcelable.Creator<NdefMessage>() {
        @Override
        public NdefMessage createFromParcel(Parcel parcel) {
            NdefRecord[] ndefRecordArr = new NdefRecord[parcel.readInt()];
            parcel.readTypedArray(ndefRecordArr, NdefRecord.CREATOR);
            return new NdefMessage(ndefRecordArr);
        }

        @Override
        public NdefMessage[] newArray(int i) {
            return new NdefMessage[i];
        }
    };
    private final NdefRecord[] mRecords;

    public NdefMessage(byte[] bArr) throws FormatException {
        if (bArr == null) {
            throw new NullPointerException("data is null");
        }
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        this.mRecords = NdefRecord.parse(byteBufferWrap, false);
        if (byteBufferWrap.remaining() > 0) {
            throw new FormatException("trailing data");
        }
    }

    public NdefMessage(NdefRecord ndefRecord, NdefRecord... ndefRecordArr) {
        if (ndefRecord == null) {
            throw new NullPointerException("record cannot be null");
        }
        for (NdefRecord ndefRecord2 : ndefRecordArr) {
            if (ndefRecord2 == null) {
                throw new NullPointerException("record cannot be null");
            }
        }
        this.mRecords = new NdefRecord[ndefRecordArr.length + 1];
        this.mRecords[0] = ndefRecord;
        System.arraycopy(ndefRecordArr, 0, this.mRecords, 1, ndefRecordArr.length);
    }

    public NdefMessage(NdefRecord[] ndefRecordArr) {
        if (ndefRecordArr.length < 1) {
            throw new IllegalArgumentException("must have at least one record");
        }
        for (NdefRecord ndefRecord : ndefRecordArr) {
            if (ndefRecord == null) {
                throw new NullPointerException("records cannot contain null");
            }
        }
        this.mRecords = ndefRecordArr;
    }

    public NdefRecord[] getRecords() {
        return this.mRecords;
    }

    public int getByteArrayLength() {
        int byteLength = 0;
        for (NdefRecord ndefRecord : this.mRecords) {
            byteLength += ndefRecord.getByteLength();
        }
        return byteLength;
    }

    public byte[] toByteArray() {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(getByteArrayLength());
        int i = 0;
        while (i < this.mRecords.length) {
            boolean z = true;
            boolean z2 = i == 0;
            if (i != this.mRecords.length - 1) {
                z = false;
            }
            this.mRecords[i].writeToByteBuffer(byteBufferAllocate, z2, z);
            i++;
        }
        return byteBufferAllocate.array();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mRecords.length);
        parcel.writeTypedArray(this.mRecords, i);
    }

    public int hashCode() {
        return Arrays.hashCode(this.mRecords);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(this.mRecords, ((NdefMessage) obj).mRecords);
    }

    public String toString() {
        return "NdefMessage " + Arrays.toString(this.mRecords);
    }
}
