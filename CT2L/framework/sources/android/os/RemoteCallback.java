package android.os;

import android.os.IRemoteCallback;
import android.os.Parcelable;

public abstract class RemoteCallback implements Parcelable {
    public static final Parcelable.Creator<RemoteCallback> CREATOR = new Parcelable.Creator<RemoteCallback>() {
        @Override
        public RemoteCallback createFromParcel(Parcel in) {
            IBinder target = in.readStrongBinder();
            if (target != null) {
                return new RemoteCallbackProxy(IRemoteCallback.Stub.asInterface(target));
            }
            return null;
        }

        @Override
        public RemoteCallback[] newArray(int size) {
            return new RemoteCallback[size];
        }
    };
    final Handler mHandler;
    final IRemoteCallback mTarget;

    protected abstract void onResult(Bundle bundle);

    class DeliverResult implements Runnable {
        final Bundle mResult;

        DeliverResult(Bundle result) {
            this.mResult = result;
        }

        @Override
        public void run() {
            RemoteCallback.this.onResult(this.mResult);
        }
    }

    class LocalCallback extends IRemoteCallback.Stub {
        LocalCallback() {
        }

        @Override
        public void sendResult(Bundle bundle) {
            RemoteCallback.this.mHandler.post(RemoteCallback.this.new DeliverResult(bundle));
        }
    }

    static class RemoteCallbackProxy extends RemoteCallback {
        RemoteCallbackProxy(IRemoteCallback target) {
            super(target);
        }

        @Override
        protected void onResult(Bundle bundle) {
        }
    }

    public RemoteCallback(Handler handler) {
        this.mHandler = handler;
        this.mTarget = new LocalCallback();
    }

    RemoteCallback(IRemoteCallback target) {
        this.mHandler = null;
        this.mTarget = target;
    }

    public void sendResult(Bundle bundle) throws RemoteException {
        this.mTarget.sendResult(bundle);
    }

    public boolean equals(Object otherObj) {
        if (otherObj == null) {
            return false;
        }
        try {
            return this.mTarget.asBinder().equals(((RemoteCallback) otherObj).mTarget.asBinder());
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        return this.mTarget.asBinder().hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(this.mTarget.asBinder());
    }
}
