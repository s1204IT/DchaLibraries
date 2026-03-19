package android.os;

import android.annotation.SystemApi;
import android.os.IRemoteCallback;
import android.os.Parcelable;

@SystemApi
public final class RemoteCallback implements Parcelable {
    public static final Parcelable.Creator<RemoteCallback> CREATOR = new Parcelable.Creator<RemoteCallback>() {
        @Override
        public RemoteCallback createFromParcel(Parcel parcel) {
            return new RemoteCallback(parcel);
        }

        @Override
        public RemoteCallback[] newArray(int i) {
            return new RemoteCallback[i];
        }
    };
    private final IRemoteCallback mCallback;
    private final Handler mHandler;
    private final OnResultListener mListener;

    public interface OnResultListener {
        void onResult(Bundle bundle);
    }

    public RemoteCallback(OnResultListener onResultListener) {
        this(onResultListener, null);
    }

    public RemoteCallback(OnResultListener onResultListener, Handler handler) {
        if (onResultListener == null) {
            throw new NullPointerException("listener cannot be null");
        }
        this.mListener = onResultListener;
        this.mHandler = handler;
        this.mCallback = new IRemoteCallback.Stub() {
            @Override
            public void sendResult(Bundle bundle) {
                RemoteCallback.this.sendResult(bundle);
            }
        };
    }

    RemoteCallback(Parcel parcel) {
        this.mListener = null;
        this.mHandler = null;
        this.mCallback = IRemoteCallback.Stub.asInterface(parcel.readStrongBinder());
    }

    public void sendResult(final Bundle bundle) {
        if (this.mListener != null) {
            if (this.mHandler != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        RemoteCallback.this.mListener.onResult(bundle);
                    }
                });
                return;
            } else {
                this.mListener.onResult(bundle);
                return;
            }
        }
        try {
            this.mCallback.sendResult(bundle);
        } catch (RemoteException e) {
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.mCallback.asBinder());
    }
}
