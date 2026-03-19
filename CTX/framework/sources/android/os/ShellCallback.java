package android.os;

import android.os.Parcelable;
import android.util.Log;
import com.android.internal.os.IShellCallback;

public class ShellCallback implements Parcelable {
    public static final Parcelable.Creator<ShellCallback> CREATOR = new Parcelable.Creator<ShellCallback>() {
        @Override
        public ShellCallback createFromParcel(Parcel parcel) {
            return new ShellCallback(parcel);
        }

        @Override
        public ShellCallback[] newArray(int i) {
            return new ShellCallback[i];
        }
    };
    static final boolean DEBUG = false;
    static final String TAG = "ShellCallback";
    final boolean mLocal = true;
    IShellCallback mShellCallback;

    class MyShellCallback extends IShellCallback.Stub {
        MyShellCallback() {
        }

        @Override
        public ParcelFileDescriptor openFile(String str, String str2, String str3) {
            return ShellCallback.this.onOpenFile(str, str2, str3);
        }
    }

    public ShellCallback() {
    }

    public ParcelFileDescriptor openFile(String str, String str2, String str3) {
        if (this.mLocal) {
            return onOpenFile(str, str2, str3);
        }
        if (this.mShellCallback != null) {
            try {
                return this.mShellCallback.openFile(str, str2, str3);
            } catch (RemoteException e) {
                Log.w(TAG, "Failure opening " + str, e);
                return null;
            }
        }
        return null;
    }

    public ParcelFileDescriptor onOpenFile(String str, String str2, String str3) {
        return null;
    }

    public static void writeToParcel(ShellCallback shellCallback, Parcel parcel) {
        if (shellCallback == null) {
            parcel.writeStrongBinder(null);
        } else {
            shellCallback.writeToParcel(parcel, 0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        synchronized (this) {
            if (this.mShellCallback == null) {
                this.mShellCallback = new MyShellCallback();
            }
            parcel.writeStrongBinder(this.mShellCallback.asBinder());
        }
    }

    ShellCallback(Parcel parcel) {
        this.mShellCallback = IShellCallback.Stub.asInterface(parcel.readStrongBinder());
        if (this.mShellCallback != null) {
            Binder.allowBlocking(this.mShellCallback.asBinder());
        }
    }
}
