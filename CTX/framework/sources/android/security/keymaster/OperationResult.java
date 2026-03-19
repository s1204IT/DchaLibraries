package android.security.keymaster;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class OperationResult implements Parcelable {
    public static final Parcelable.Creator<OperationResult> CREATOR = new Parcelable.Creator<OperationResult>() {
        @Override
        public OperationResult createFromParcel(Parcel parcel) {
            return new OperationResult(parcel);
        }

        @Override
        public OperationResult[] newArray(int i) {
            return new OperationResult[i];
        }
    };
    public final int inputConsumed;
    public final long operationHandle;
    public final KeymasterArguments outParams;
    public final byte[] output;
    public final int resultCode;
    public final IBinder token;

    public OperationResult(int i, IBinder iBinder, long j, int i2, byte[] bArr, KeymasterArguments keymasterArguments) {
        this.resultCode = i;
        this.token = iBinder;
        this.operationHandle = j;
        this.inputConsumed = i2;
        this.output = bArr;
        this.outParams = keymasterArguments;
    }

    protected OperationResult(Parcel parcel) {
        this.resultCode = parcel.readInt();
        this.token = parcel.readStrongBinder();
        this.operationHandle = parcel.readLong();
        this.inputConsumed = parcel.readInt();
        this.output = parcel.createByteArray();
        this.outParams = KeymasterArguments.CREATOR.createFromParcel(parcel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.resultCode);
        parcel.writeStrongBinder(this.token);
        parcel.writeLong(this.operationHandle);
        parcel.writeInt(this.inputConsumed);
        parcel.writeByteArray(this.output);
        this.outParams.writeToParcel(parcel, i);
    }
}
