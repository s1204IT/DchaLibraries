package android.content;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AndroidException;

public class IntentSender implements Parcelable {
    public static final Parcelable.Creator<IntentSender> CREATOR = new Parcelable.Creator<IntentSender>() {
        @Override
        public IntentSender createFromParcel(Parcel parcel) {
            IBinder strongBinder = parcel.readStrongBinder();
            if (strongBinder != null) {
                return new IntentSender(strongBinder);
            }
            return null;
        }

        @Override
        public IntentSender[] newArray(int i) {
            return new IntentSender[i];
        }
    };
    private final IIntentSender mTarget;
    IBinder mWhitelistToken;

    public interface OnFinished {
        void onSendFinished(IntentSender intentSender, Intent intent, int i, String str, Bundle bundle);
    }

    public static class SendIntentException extends AndroidException {
        public SendIntentException() {
        }

        public SendIntentException(String str) {
            super(str);
        }

        public SendIntentException(Exception exc) {
            super(exc);
        }
    }

    private static class FinishedDispatcher extends IIntentReceiver.Stub implements Runnable {
        private final Handler mHandler;
        private Intent mIntent;
        private final IntentSender mIntentSender;
        private int mResultCode;
        private String mResultData;
        private Bundle mResultExtras;
        private final OnFinished mWho;

        FinishedDispatcher(IntentSender intentSender, OnFinished onFinished, Handler handler) {
            this.mIntentSender = intentSender;
            this.mWho = onFinished;
            this.mHandler = handler;
        }

        @Override
        public void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
            this.mIntent = intent;
            this.mResultCode = i;
            this.mResultData = str;
            this.mResultExtras = bundle;
            if (this.mHandler == null) {
                run();
            } else {
                this.mHandler.post(this);
            }
        }

        @Override
        public void run() {
            this.mWho.onSendFinished(this.mIntentSender, this.mIntent, this.mResultCode, this.mResultData, this.mResultExtras);
        }
    }

    public void sendIntent(Context context, int i, Intent intent, OnFinished onFinished, Handler handler) throws SendIntentException {
        sendIntent(context, i, intent, onFinished, handler, null);
    }

    public void sendIntent(Context context, int i, Intent intent, OnFinished onFinished, Handler handler, String str) throws SendIntentException {
        String strResolveTypeIfNeeded;
        FinishedDispatcher finishedDispatcher = null;
        if (intent != null) {
            try {
                strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(context.getContentResolver());
            } catch (RemoteException e) {
                throw new SendIntentException();
            }
        } else {
            strResolveTypeIfNeeded = null;
        }
        IActivityManager service = ActivityManager.getService();
        IIntentSender iIntentSender = this.mTarget;
        IBinder iBinder = this.mWhitelistToken;
        if (onFinished != null) {
            finishedDispatcher = new FinishedDispatcher(this, onFinished, handler);
        }
        if (service.sendIntentSender(iIntentSender, iBinder, i, intent, strResolveTypeIfNeeded, finishedDispatcher, str, null) < 0) {
            throw new SendIntentException();
        }
    }

    @Deprecated
    public String getTargetPackage() {
        try {
            return ActivityManager.getService().getPackageForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getCreatorPackage() {
        try {
            return ActivityManager.getService().getPackageForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            return null;
        }
    }

    public int getCreatorUid() {
        try {
            return ActivityManager.getService().getUidForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public UserHandle getCreatorUserHandle() {
        try {
            int uidForIntentSender = ActivityManager.getService().getUidForIntentSender(this.mTarget);
            if (uidForIntentSender > 0) {
                return new UserHandle(UserHandle.getUserId(uidForIntentSender));
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof IntentSender) {
            return this.mTarget.asBinder().equals(((IntentSender) obj).mTarget.asBinder());
        }
        return false;
    }

    public int hashCode() {
        return this.mTarget.asBinder().hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("IntentSender{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(": ");
        sb.append(this.mTarget != null ? this.mTarget.asBinder() : null);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.mTarget.asBinder());
    }

    public static void writeIntentSenderOrNullToParcel(IntentSender intentSender, Parcel parcel) {
        parcel.writeStrongBinder(intentSender != null ? intentSender.mTarget.asBinder() : null);
    }

    public static IntentSender readIntentSenderOrNullFromParcel(Parcel parcel) {
        IBinder strongBinder = parcel.readStrongBinder();
        if (strongBinder != null) {
            return new IntentSender(strongBinder);
        }
        return null;
    }

    public IIntentSender getTarget() {
        return this.mTarget;
    }

    public IBinder getWhitelistToken() {
        return this.mWhitelistToken;
    }

    public IntentSender(IIntentSender iIntentSender) {
        this.mTarget = iIntentSender;
    }

    public IntentSender(IIntentSender iIntentSender, IBinder iBinder) {
        this.mTarget = iIntentSender;
        this.mWhitelistToken = iBinder;
    }

    public IntentSender(IBinder iBinder) {
        this.mTarget = IIntentSender.Stub.asInterface(iBinder);
    }
}
