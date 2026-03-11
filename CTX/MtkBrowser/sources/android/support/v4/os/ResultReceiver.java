package android.support.v4.os;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.os.IResultReceiver;

public class ResultReceiver implements Parcelable {
    public static final Parcelable.Creator<ResultReceiver> CREATOR = new Parcelable.Creator<ResultReceiver>() {
        @Override
        public ResultReceiver createFromParcel(Parcel parcel) {
            return new ResultReceiver(parcel);
        }

        @Override
        public ResultReceiver[] newArray(int i) {
            return new ResultReceiver[i];
        }
    };
    IResultReceiver mReceiver;
    final boolean mLocal = false;
    final Handler mHandler = null;

    class MyResultReceiver extends IResultReceiver.Stub {
        final ResultReceiver this$0;

        MyResultReceiver(ResultReceiver resultReceiver) {
            this.this$0 = resultReceiver;
        }

        @Override
        public void send(int i, Bundle bundle) {
            if (this.this$0.mHandler != null) {
                this.this$0.mHandler.post(new MyRunnable(this.this$0, i, bundle));
            } else {
                this.this$0.onReceiveResult(i, bundle);
            }
        }
    }

    class MyRunnable implements Runnable {
        final int mResultCode;
        final Bundle mResultData;
        final ResultReceiver this$0;

        MyRunnable(ResultReceiver resultReceiver, int i, Bundle bundle) {
            this.this$0 = resultReceiver;
            this.mResultCode = i;
            this.mResultData = bundle;
        }

        @Override
        public void run() {
            this.this$0.onReceiveResult(this.mResultCode, this.mResultData);
        }
    }

    ResultReceiver(Parcel parcel) {
        this.mReceiver = IResultReceiver.Stub.asInterface(parcel.readStrongBinder());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected void onReceiveResult(int i, Bundle bundle) {
    }

    public void send(int i, Bundle bundle) {
        if (this.mLocal) {
            if (this.mHandler != null) {
                this.mHandler.post(new MyRunnable(this, i, bundle));
                return;
            } else {
                onReceiveResult(i, bundle);
                return;
            }
        }
        if (this.mReceiver != null) {
            try {
                this.mReceiver.send(i, bundle);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        synchronized (this) {
            if (this.mReceiver == null) {
                this.mReceiver = new MyResultReceiver(this);
            }
            parcel.writeStrongBinder(this.mReceiver.asBinder());
        }
    }
}
