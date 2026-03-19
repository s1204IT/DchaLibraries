package android.app;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.AndroidException;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import com.android.internal.os.IResultReceiver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class PendingIntent implements Parcelable {
    public static final int FLAG_CANCEL_CURRENT = 268435456;
    public static final int FLAG_IMMUTABLE = 67108864;
    public static final int FLAG_NO_CREATE = 536870912;
    public static final int FLAG_ONE_SHOT = 1073741824;
    public static final int FLAG_UPDATE_CURRENT = 134217728;
    private ArraySet<CancelListener> mCancelListeners;
    private IResultReceiver mCancelReceiver;
    private final IIntentSender mTarget;
    private IBinder mWhitelistToken;
    private static final ThreadLocal<OnMarshaledListener> sOnMarshaledListener = new ThreadLocal<>();
    public static final Parcelable.Creator<PendingIntent> CREATOR = new Parcelable.Creator<PendingIntent>() {
        @Override
        public PendingIntent createFromParcel(Parcel parcel) {
            IBinder strongBinder = parcel.readStrongBinder();
            if (strongBinder != null) {
                return new PendingIntent(strongBinder, parcel.getClassCookie(PendingIntent.class));
            }
            return null;
        }

        @Override
        public PendingIntent[] newArray(int i) {
            return new PendingIntent[i];
        }
    };

    public interface CancelListener {
        void onCancelled(PendingIntent pendingIntent);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    public interface OnFinished {
        void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle);
    }

    public interface OnMarshaledListener {
        void onMarshaled(PendingIntent pendingIntent, Parcel parcel, int i);
    }

    public static class CanceledException extends AndroidException {
        public CanceledException() {
        }

        public CanceledException(String str) {
            super(str);
        }

        public CanceledException(Exception exc) {
            super(exc);
        }
    }

    private static class FinishedDispatcher extends IIntentReceiver.Stub implements Runnable {
        private static Handler sDefaultSystemHandler;
        private final Handler mHandler;
        private Intent mIntent;
        private final PendingIntent mPendingIntent;
        private int mResultCode;
        private String mResultData;
        private Bundle mResultExtras;
        private final OnFinished mWho;

        FinishedDispatcher(PendingIntent pendingIntent, OnFinished onFinished, Handler handler) {
            this.mPendingIntent = pendingIntent;
            this.mWho = onFinished;
            if (handler == null && ActivityThread.isSystem()) {
                if (sDefaultSystemHandler == null) {
                    sDefaultSystemHandler = new Handler(Looper.getMainLooper());
                }
                this.mHandler = sDefaultSystemHandler;
                return;
            }
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
            this.mWho.onSendFinished(this.mPendingIntent, this.mIntent, this.mResultCode, this.mResultData, this.mResultExtras);
        }
    }

    public static void setOnMarshaledListener(OnMarshaledListener onMarshaledListener) {
        sOnMarshaledListener.set(onMarshaledListener);
    }

    public static PendingIntent getActivity(Context context, int i, Intent intent, int i2) {
        return getActivity(context, i, intent, i2, null);
    }

    public static PendingIntent getActivity(Context context, int i, Intent intent, int i2, Bundle bundle) {
        String strResolveTypeIfNeeded;
        String packageName = context.getPackageName();
        if (intent != null) {
            strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            strResolveTypeIfNeeded = null;
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(context);
            IIntentSender intentSender = ActivityManager.getService().getIntentSender(2, packageName, null, null, i, new Intent[]{intent}, strResolveTypeIfNeeded != null ? new String[]{strResolveTypeIfNeeded} : null, i2, bundle, context.getUserId());
            if (intentSender != null) {
                return new PendingIntent(intentSender);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getActivityAsUser(Context context, int i, Intent intent, int i2, Bundle bundle, UserHandle userHandle) {
        String strResolveTypeIfNeeded;
        String packageName = context.getPackageName();
        if (intent != null) {
            strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            strResolveTypeIfNeeded = null;
        }
        try {
            intent.migrateExtraStreamToClipData();
            intent.prepareToLeaveProcess(context);
            IIntentSender intentSender = ActivityManager.getService().getIntentSender(2, packageName, null, null, i, new Intent[]{intent}, strResolveTypeIfNeeded != null ? new String[]{strResolveTypeIfNeeded} : null, i2, bundle, userHandle.getIdentifier());
            if (intentSender != null) {
                return new PendingIntent(intentSender);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getActivities(Context context, int i, Intent[] intentArr, int i2) {
        return getActivities(context, i, intentArr, i2, null);
    }

    public static PendingIntent getActivities(Context context, int i, Intent[] intentArr, int i2, Bundle bundle) {
        String packageName = context.getPackageName();
        String[] strArr = new String[intentArr.length];
        for (int i3 = 0; i3 < intentArr.length; i3++) {
            intentArr[i3].migrateExtraStreamToClipData();
            intentArr[i3].prepareToLeaveProcess(context);
            strArr[i3] = intentArr[i3].resolveTypeIfNeeded(context.getContentResolver());
        }
        try {
            IIntentSender intentSender = ActivityManager.getService().getIntentSender(2, packageName, null, null, i, intentArr, strArr, i2, bundle, context.getUserId());
            if (intentSender != null) {
                return new PendingIntent(intentSender);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getActivitiesAsUser(Context context, int i, Intent[] intentArr, int i2, Bundle bundle, UserHandle userHandle) {
        String packageName = context.getPackageName();
        String[] strArr = new String[intentArr.length];
        for (int i3 = 0; i3 < intentArr.length; i3++) {
            intentArr[i3].migrateExtraStreamToClipData();
            intentArr[i3].prepareToLeaveProcess(context);
            strArr[i3] = intentArr[i3].resolveTypeIfNeeded(context.getContentResolver());
        }
        try {
            IIntentSender intentSender = ActivityManager.getService().getIntentSender(2, packageName, null, null, i, intentArr, strArr, i2, bundle, userHandle.getIdentifier());
            if (intentSender != null) {
                return new PendingIntent(intentSender);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getBroadcast(Context context, int i, Intent intent, int i2) {
        return getBroadcastAsUser(context, i, intent, i2, context.getUser());
    }

    public static PendingIntent getBroadcastAsUser(Context context, int i, Intent intent, int i2, UserHandle userHandle) {
        String strResolveTypeIfNeeded;
        String packageName = context.getPackageName();
        if (intent != null) {
            strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            strResolveTypeIfNeeded = null;
        }
        try {
            intent.prepareToLeaveProcess(context);
            IIntentSender intentSender = ActivityManager.getService().getIntentSender(1, packageName, null, null, i, new Intent[]{intent}, strResolveTypeIfNeeded != null ? new String[]{strResolveTypeIfNeeded} : null, i2, null, userHandle.getIdentifier());
            if (intentSender != null) {
                return new PendingIntent(intentSender);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static PendingIntent getService(Context context, int i, Intent intent, int i2) {
        return buildServicePendingIntent(context, i, intent, i2, 4);
    }

    public static PendingIntent getForegroundService(Context context, int i, Intent intent, int i2) {
        return buildServicePendingIntent(context, i, intent, i2, 5);
    }

    private static PendingIntent buildServicePendingIntent(Context context, int i, Intent intent, int i2, int i3) {
        String strResolveTypeIfNeeded;
        String packageName = context.getPackageName();
        if (intent != null) {
            strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(context.getContentResolver());
        } else {
            strResolveTypeIfNeeded = null;
        }
        try {
            intent.prepareToLeaveProcess(context);
            IIntentSender intentSender = ActivityManager.getService().getIntentSender(i3, packageName, null, null, i, new Intent[]{intent}, strResolveTypeIfNeeded != null ? new String[]{strResolveTypeIfNeeded} : null, i2, null, context.getUserId());
            if (intentSender != null) {
                return new PendingIntent(intentSender);
            }
            return null;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IntentSender getIntentSender() {
        return new IntentSender(this.mTarget, this.mWhitelistToken);
    }

    public void cancel() {
        try {
            ActivityManager.getService().cancelIntentSender(this.mTarget);
        } catch (RemoteException e) {
        }
    }

    public void send() throws CanceledException {
        send(null, 0, null, null, null, null, null);
    }

    public void send(int i) throws CanceledException {
        send(null, i, null, null, null, null, null);
    }

    public void send(Context context, int i, Intent intent) throws CanceledException {
        send(context, i, intent, null, null, null, null);
    }

    public void send(int i, OnFinished onFinished, Handler handler) throws CanceledException {
        send(null, i, null, onFinished, handler, null, null);
    }

    public void send(Context context, int i, Intent intent, OnFinished onFinished, Handler handler) throws CanceledException {
        send(context, i, intent, onFinished, handler, null, null);
    }

    public void send(Context context, int i, Intent intent, OnFinished onFinished, Handler handler, String str) throws CanceledException {
        send(context, i, intent, onFinished, handler, str, null);
    }

    public void send(Context context, int i, Intent intent, OnFinished onFinished, Handler handler, String str, Bundle bundle) throws CanceledException {
        if (sendAndReturnResult(context, i, intent, onFinished, handler, str, bundle) < 0) {
            throw new CanceledException();
        }
    }

    public int sendAndReturnResult(Context context, int i, Intent intent, OnFinished onFinished, Handler handler, String str, Bundle bundle) throws CanceledException {
        String strResolveTypeIfNeeded;
        FinishedDispatcher finishedDispatcher = null;
        if (intent != null) {
            try {
                strResolveTypeIfNeeded = intent.resolveTypeIfNeeded(context.getContentResolver());
            } catch (RemoteException e) {
                throw new CanceledException(e);
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
        return service.sendIntentSender(iIntentSender, iBinder, i, intent, strResolveTypeIfNeeded, finishedDispatcher, str, bundle);
    }

    @Deprecated
    public String getTargetPackage() {
        try {
            return ActivityManager.getService().getPackageForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getCreatorPackage() {
        try {
            return ActivityManager.getService().getPackageForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getCreatorUid() {
        try {
            return ActivityManager.getService().getUidForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerCancelListener(CancelListener cancelListener) {
        synchronized (this) {
            if (this.mCancelReceiver == null) {
                this.mCancelReceiver = new IResultReceiver.Stub() {
                    @Override
                    public void send(int i, Bundle bundle) throws RemoteException {
                        PendingIntent.this.notifyCancelListeners();
                    }
                };
            }
            if (this.mCancelListeners == null) {
                this.mCancelListeners = new ArraySet<>();
            }
            boolean zIsEmpty = this.mCancelListeners.isEmpty();
            this.mCancelListeners.add(cancelListener);
            if (zIsEmpty) {
                try {
                    ActivityManager.getService().registerIntentSenderCancelListener(this.mTarget, this.mCancelReceiver);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
        }
    }

    private void notifyCancelListeners() {
        ArraySet arraySet;
        synchronized (this) {
            arraySet = new ArraySet((ArraySet) this.mCancelListeners);
        }
        int size = arraySet.size();
        for (int i = 0; i < size; i++) {
            ((CancelListener) arraySet.valueAt(i)).onCancelled(this);
        }
    }

    public void unregisterCancelListener(CancelListener cancelListener) {
        synchronized (this) {
            if (this.mCancelListeners == null) {
                return;
            }
            boolean zIsEmpty = this.mCancelListeners.isEmpty();
            this.mCancelListeners.remove(cancelListener);
            if (this.mCancelListeners.isEmpty() && !zIsEmpty) {
                try {
                    ActivityManager.getService().unregisterIntentSenderCancelListener(this.mTarget, this.mCancelReceiver);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
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
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isTargetedToPackage() {
        try {
            return ActivityManager.getService().isIntentSenderTargetedToPackage(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isActivity() {
        try {
            return ActivityManager.getService().isIntentSenderAnActivity(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isForegroundService() {
        try {
            return ActivityManager.getService().isIntentSenderAForegroundService(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Intent getIntent() {
        try {
            return ActivityManager.getService().getIntentForIntentSender(this.mTarget);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getTag(String str) {
        try {
            return ActivityManager.getService().getTagForIntentSender(this.mTarget, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof PendingIntent) {
            return this.mTarget.asBinder().equals(((PendingIntent) obj).mTarget.asBinder());
        }
        return false;
    }

    public int hashCode() {
        return this.mTarget.asBinder().hashCode();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("PendingIntent{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(": ");
        sb.append(this.mTarget != null ? this.mTarget.asBinder() : null);
        sb.append('}');
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        if (this.mTarget != null) {
            protoOutputStream.write(1138166333441L, this.mTarget.asBinder().toString());
        }
        protoOutputStream.end(jStart);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(this.mTarget.asBinder());
        OnMarshaledListener onMarshaledListener = sOnMarshaledListener.get();
        if (onMarshaledListener != null) {
            onMarshaledListener.onMarshaled(this, parcel, i);
        }
    }

    public static void writePendingIntentOrNullToParcel(PendingIntent pendingIntent, Parcel parcel) {
        OnMarshaledListener onMarshaledListener;
        parcel.writeStrongBinder(pendingIntent != null ? pendingIntent.mTarget.asBinder() : null);
        if (pendingIntent != null && (onMarshaledListener = sOnMarshaledListener.get()) != null) {
            onMarshaledListener.onMarshaled(pendingIntent, parcel, 0);
        }
    }

    public static PendingIntent readPendingIntentOrNullFromParcel(Parcel parcel) {
        IBinder strongBinder = parcel.readStrongBinder();
        if (strongBinder != null) {
            return new PendingIntent(strongBinder, parcel.getClassCookie(PendingIntent.class));
        }
        return null;
    }

    PendingIntent(IIntentSender iIntentSender) {
        this.mTarget = iIntentSender;
    }

    PendingIntent(IBinder iBinder, Object obj) {
        this.mTarget = IIntentSender.Stub.asInterface(iBinder);
        if (obj != null) {
            this.mWhitelistToken = (IBinder) obj;
        }
    }

    public IIntentSender getTarget() {
        return this.mTarget;
    }

    public IBinder getWhitelistToken() {
        return this.mWhitelistToken;
    }
}
