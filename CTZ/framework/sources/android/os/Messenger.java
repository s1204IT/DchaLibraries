package android.os;

import android.os.IMessenger;
import android.os.Parcelable;

public final class Messenger implements Parcelable {
    public static final Parcelable.Creator<Messenger> CREATOR = new Parcelable.Creator<Messenger>() {
        @Override
        public Messenger createFromParcel(Parcel parcel) {
            IBinder strongBinder = parcel.readStrongBinder();
            if (strongBinder != null) {
                return new Messenger(strongBinder);
            }
            return null;
        }

        @Override
        public Messenger[] newArray(int i) {
            return new Messenger[i];
        }
    };
    private final IMessenger mTarget;

    public Messenger(Handler handler) {
        this.mTarget = handler.getIMessenger();
    }

    public void send(Message message) throws RemoteException {
        this.mTarget.send(message);
    }

    public IBinder getBinder() {
        return this.mTarget.asBinder();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        try {
            return this.mTarget.asBinder().equals(((Messenger) obj).mTarget.asBinder());
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.mTarget.asBinder());
    }

    public static void writeMessengerOrNullToParcel(Messenger messenger, Parcel parcel) {
        parcel.writeStrongBinder(messenger != null ? messenger.mTarget.asBinder() : null);
    }

    public static Messenger readMessengerOrNullFromParcel(Parcel parcel) {
        IBinder strongBinder = parcel.readStrongBinder();
        if (strongBinder != null) {
            return new Messenger(strongBinder);
        }
        return null;
    }

    public Messenger(IBinder iBinder) {
        this.mTarget = IMessenger.Stub.asInterface(iBinder);
    }
}
