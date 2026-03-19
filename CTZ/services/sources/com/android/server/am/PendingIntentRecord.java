package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.os.IResultReceiver;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;

final class PendingIntentRecord extends IIntentSender.Stub {
    private static final String TAG = "ActivityManager";
    final Key key;
    String lastTag;
    String lastTagPrefix;
    private RemoteCallbackList<IResultReceiver> mCancelCallbacks;
    final ActivityManagerService owner;
    String stringName;
    final int uid;
    private ArrayMap<IBinder, Long> whitelistDuration;
    boolean sent = false;
    boolean canceled = false;
    final WeakReference<PendingIntentRecord> ref = new WeakReference<>(this);

    static final class Key {
        private static final int ODD_PRIME_NUMBER = 37;
        final ActivityRecord activity;
        Intent[] allIntents;
        String[] allResolvedTypes;
        final int flags;
        final int hashCode;
        final SafeActivityOptions options;
        final String packageName;
        final int requestCode;
        final Intent requestIntent;
        final String requestResolvedType;
        final int type;
        final int userId;
        final String who;

        Key(int i, String str, ActivityRecord activityRecord, String str2, int i2, Intent[] intentArr, String[] strArr, int i3, SafeActivityOptions safeActivityOptions, int i4) {
            this.type = i;
            this.packageName = str;
            this.activity = activityRecord;
            this.who = str2;
            this.requestCode = i2;
            this.requestIntent = intentArr != null ? intentArr[intentArr.length - 1] : null;
            this.requestResolvedType = strArr != null ? strArr[strArr.length - 1] : null;
            this.allIntents = intentArr;
            this.allResolvedTypes = strArr;
            this.flags = i3;
            this.options = safeActivityOptions;
            this.userId = i4;
            int iHashCode = ((((851 + i3) * 37) + i2) * 37) + i4;
            iHashCode = str2 != null ? (iHashCode * 37) + str2.hashCode() : iHashCode;
            iHashCode = activityRecord != null ? (iHashCode * 37) + activityRecord.hashCode() : iHashCode;
            iHashCode = this.requestIntent != null ? (iHashCode * 37) + this.requestIntent.filterHashCode() : iHashCode;
            this.hashCode = (37 * (((this.requestResolvedType != null ? (iHashCode * 37) + this.requestResolvedType.hashCode() : iHashCode) * 37) + (str != null ? str.hashCode() : 0))) + i;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            try {
                Key key = (Key) obj;
                if (this.type != key.type || this.userId != key.userId || !Objects.equals(this.packageName, key.packageName) || this.activity != key.activity || !Objects.equals(this.who, key.who) || this.requestCode != key.requestCode) {
                    return false;
                }
                if (this.requestIntent != key.requestIntent) {
                    if (this.requestIntent != null) {
                        if (!this.requestIntent.filterEquals(key.requestIntent)) {
                            return false;
                        }
                    } else if (key.requestIntent != null) {
                        return false;
                    }
                }
                if (!Objects.equals(this.requestResolvedType, key.requestResolvedType)) {
                    return false;
                }
                if (this.flags != key.flags) {
                    return false;
                }
                return true;
            } catch (ClassCastException e) {
                return false;
            }
        }

        public int hashCode() {
            return this.hashCode;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Key{");
            sb.append(typeName());
            sb.append(" pkg=");
            sb.append(this.packageName);
            sb.append(" intent=");
            sb.append(this.requestIntent != null ? this.requestIntent.toShortString(false, true, false, false) : "<null>");
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(this.flags));
            sb.append(" u=");
            sb.append(this.userId);
            sb.append("}");
            return sb.toString();
        }

        String typeName() {
            switch (this.type) {
                case 1:
                    return "broadcastIntent";
                case 2:
                    return "startActivity";
                case 3:
                    return "activityResult";
                case 4:
                    return "startService";
                case 5:
                    return "startForegroundService";
                default:
                    return Integer.toString(this.type);
            }
        }
    }

    PendingIntentRecord(ActivityManagerService activityManagerService, Key key, int i) {
        this.owner = activityManagerService;
        this.key = key;
        this.uid = i;
    }

    void setWhitelistDurationLocked(IBinder iBinder, long j) {
        if (j > 0) {
            if (this.whitelistDuration == null) {
                this.whitelistDuration = new ArrayMap<>();
            }
            this.whitelistDuration.put(iBinder, Long.valueOf(j));
        } else if (this.whitelistDuration != null) {
            this.whitelistDuration.remove(iBinder);
            if (this.whitelistDuration.size() <= 0) {
                this.whitelistDuration = null;
            }
        }
        this.stringName = null;
    }

    public void registerCancelListenerLocked(IResultReceiver iResultReceiver) {
        if (this.mCancelCallbacks == null) {
            this.mCancelCallbacks = new RemoteCallbackList<>();
        }
        this.mCancelCallbacks.register(iResultReceiver);
    }

    public void unregisterCancelListenerLocked(IResultReceiver iResultReceiver) {
        if (this.mCancelCallbacks == null) {
            return;
        }
        this.mCancelCallbacks.unregister(iResultReceiver);
        if (this.mCancelCallbacks.getRegisteredCallbackCount() <= 0) {
            this.mCancelCallbacks = null;
        }
    }

    public RemoteCallbackList<IResultReceiver> detachCancelListenersLocked() {
        RemoteCallbackList<IResultReceiver> remoteCallbackList = this.mCancelCallbacks;
        this.mCancelCallbacks = null;
        return remoteCallbackList;
    }

    public void send(int i, Intent intent, String str, IBinder iBinder, IIntentReceiver iIntentReceiver, String str2, Bundle bundle) throws Throwable {
        sendInner(i, intent, str, iBinder, iIntentReceiver, str2, null, null, 0, 0, 0, bundle);
    }

    public int sendWithResult(int i, Intent intent, String str, IBinder iBinder, IIntentReceiver iIntentReceiver, String str2, Bundle bundle) {
        return sendInner(i, intent, str, iBinder, iIntentReceiver, str2, null, null, 0, 0, 0, bundle);
    }

    int sendInner(int i, Intent intent, String str, IBinder iBinder, IIntentReceiver iIntentReceiver, String str2, IBinder iBinder2, String str3, int i2, int i3, int i4, Bundle bundle) throws Throwable {
        String str4;
        long j;
        int i5;
        Intent intent2;
        ActivityManagerService activityManagerService;
        int iStartActivityInPackage;
        Long l;
        if (intent != null) {
            intent.setDefusable(true);
        }
        if (bundle != null) {
            bundle.setDefusable(true);
        }
        ActivityManagerService activityManagerService2 = this.owner;
        synchronized (activityManagerService2) {
            try {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    int i6 = -96;
                    if (this.canceled) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return -96;
                    }
                    this.sent = true;
                    if ((this.key.flags & 1073741824) != 0) {
                        this.owner.cancelIntentSenderLocked(this, true);
                    }
                    Intent intent3 = this.key.requestIntent != null ? new Intent(this.key.requestIntent) : new Intent();
                    if ((this.key.flags & 67108864) != 0) {
                        str4 = this.key.requestResolvedType;
                    } else {
                        str4 = intent != null ? (intent3.fillIn(intent, this.key.flags) & 2) == 0 ? this.key.requestResolvedType : str : this.key.requestResolvedType;
                        int i7 = i3 & (-196);
                        intent3.setFlags(((~i7) & intent3.getFlags()) | (i4 & i7));
                    }
                    String str5 = str4;
                    int callingUid = Binder.getCallingUid();
                    int callingPid = Binder.getCallingPid();
                    SafeActivityOptions safeActivityOptionsFromBundle = this.key.options;
                    if (safeActivityOptionsFromBundle == null) {
                        safeActivityOptionsFromBundle = SafeActivityOptions.fromBundle(bundle);
                    } else {
                        safeActivityOptionsFromBundle.setCallerOptions(ActivityOptions.fromBundle(bundle));
                    }
                    SafeActivityOptions safeActivityOptions = safeActivityOptionsFromBundle;
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    if (this.whitelistDuration == null || (l = this.whitelistDuration.get(iBinder)) == null) {
                        j = jClearCallingIdentity;
                    } else {
                        int uidState = this.owner.getUidState(callingUid);
                        if (ActivityManager.isProcStateBackground(uidState)) {
                            j = jClearCallingIdentity;
                            Slog.w(TAG, "Not doing whitelist " + this + ": caller state=" + uidState);
                        } else {
                            StringBuilder sb = new StringBuilder(64);
                            sb.append("pendingintent:");
                            UserHandle.formatUid(sb, callingUid);
                            sb.append(":");
                            if (intent3.getAction() != null) {
                                sb.append(intent3.getAction());
                            } else if (intent3.getComponent() != null) {
                                intent3.getComponent().appendShortString(sb);
                            } else if (intent3.getData() != null) {
                                sb.append(intent3.getData());
                            }
                            j = jClearCallingIdentity;
                            this.owner.tempWhitelistForPendingIntentLocked(callingPid, callingUid, this.uid, l.longValue(), sb.toString());
                        }
                    }
                    boolean z = iIntentReceiver != null;
                    int currentOrTargetUserId = this.key.userId;
                    if (currentOrTargetUserId == -2) {
                        currentOrTargetUserId = this.owner.mUserController.getCurrentOrTargetUserId();
                    }
                    int i8 = currentOrTargetUserId;
                    switch (this.key.type) {
                        case 1:
                            try {
                                i5 = 0;
                                intent2 = intent3;
                                activityManagerService = activityManagerService2;
                                try {
                                    if (this.owner.broadcastIntentInPackage(this.key.packageName, this.uid, intent3, str5, iIntentReceiver, i, null, null, str2, bundle, iIntentReceiver != null, false, i8) == 0) {
                                        z = false;
                                    }
                                } catch (RuntimeException e) {
                                    e = e;
                                    Slog.w(TAG, "Unable to send startActivity intent", e);
                                }
                            } catch (RuntimeException e2) {
                                e = e2;
                                i5 = 0;
                                intent2 = intent3;
                                activityManagerService = activityManagerService2;
                            }
                            i6 = i5;
                            if (z && i6 != -96) {
                                try {
                                    iIntentReceiver.performReceive(new Intent(intent2), 0, (String) null, (Bundle) null, false, false, this.key.userId);
                                    break;
                                } catch (RemoteException e3) {
                                }
                            }
                            Binder.restoreCallingIdentity(j);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return i6;
                        case 2:
                            try {
                                if (this.key.allIntents == null || this.key.allIntents.length <= 1) {
                                    iStartActivityInPackage = this.owner.getActivityStartController().startActivityInPackage(this.uid, callingPid, callingUid, this.key.packageName, intent3, str5, iBinder2, str3, i2, 0, safeActivityOptions, i8, null, "PendingIntentRecord", false);
                                } else {
                                    Intent[] intentArr = new Intent[this.key.allIntents.length];
                                    String[] strArr = new String[this.key.allIntents.length];
                                    System.arraycopy(this.key.allIntents, 0, intentArr, 0, this.key.allIntents.length);
                                    if (this.key.allResolvedTypes != null) {
                                        System.arraycopy(this.key.allResolvedTypes, 0, strArr, 0, this.key.allResolvedTypes.length);
                                    }
                                    intentArr[intentArr.length - 1] = intent3;
                                    strArr[strArr.length - 1] = str5;
                                    iStartActivityInPackage = this.owner.getActivityStartController().startActivitiesInPackage(this.uid, callingPid, callingUid, this.key.packageName, intentArr, strArr, iBinder2, safeActivityOptions, i8, false);
                                }
                                i6 = iStartActivityInPackage;
                                intent2 = intent3;
                                activityManagerService = activityManagerService2;
                            } catch (RuntimeException e4) {
                                Slog.w(TAG, "Unable to send startActivity intent", e4);
                                i5 = 0;
                                intent2 = intent3;
                                activityManagerService = activityManagerService2;
                                i6 = i5;
                            }
                            if (z) {
                                iIntentReceiver.performReceive(new Intent(intent2), 0, (String) null, (Bundle) null, false, false, this.key.userId);
                                break;
                            }
                            Binder.restoreCallingIdentity(j);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return i6;
                        case 3:
                            ActivityStack stack = this.key.activity.getStack();
                            if (stack != null) {
                                stack.sendActivityResultLocked(-1, this.key.activity, this.key.who, this.key.requestCode, i, intent3);
                            }
                            i5 = 0;
                            intent2 = intent3;
                            activityManagerService = activityManagerService2;
                            i6 = i5;
                            if (z) {
                            }
                            Binder.restoreCallingIdentity(j);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return i6;
                        case 4:
                        case 5:
                            try {
                                this.owner.startServiceInPackage(this.uid, intent3, str5, this.key.type == 5, this.key.packageName, i8);
                                break;
                            } catch (TransactionTooLargeException e5) {
                                break;
                            } catch (RuntimeException e6) {
                                Slog.w(TAG, "Unable to send startService intent", e6);
                            }
                            i5 = 0;
                            intent2 = intent3;
                            activityManagerService = activityManagerService2;
                            i6 = i5;
                            if (z) {
                            }
                            Binder.restoreCallingIdentity(j);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return i6;
                        default:
                            i5 = 0;
                            intent2 = intent3;
                            activityManagerService = activityManagerService2;
                            i6 = i5;
                            if (z) {
                            }
                            Binder.restoreCallingIdentity(j);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return i6;
                    }
                } catch (Throwable th) {
                    th = th;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.canceled) {
                this.owner.mHandler.sendMessage(this.owner.mHandler.obtainMessage(23, this));
            }
        } finally {
            super/*java.lang.Object*/.finalize();
        }
    }

    public void completeFinalize() {
        synchronized (this.owner) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.owner.mIntentSenderRecords.get(this.key) == this.ref) {
                    this.owner.mIntentSenderRecords.remove(this.key);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("uid=");
        printWriter.print(this.uid);
        printWriter.print(" packageName=");
        printWriter.print(this.key.packageName);
        printWriter.print(" type=");
        printWriter.print(this.key.typeName());
        printWriter.print(" flags=0x");
        printWriter.println(Integer.toHexString(this.key.flags));
        if (this.key.activity != null || this.key.who != null) {
            printWriter.print(str);
            printWriter.print("activity=");
            printWriter.print(this.key.activity);
            printWriter.print(" who=");
            printWriter.println(this.key.who);
        }
        if (this.key.requestCode != 0 || this.key.requestResolvedType != null) {
            printWriter.print(str);
            printWriter.print("requestCode=");
            printWriter.print(this.key.requestCode);
            printWriter.print(" requestResolvedType=");
            printWriter.println(this.key.requestResolvedType);
        }
        if (this.key.requestIntent != null) {
            printWriter.print(str);
            printWriter.print("requestIntent=");
            printWriter.println(this.key.requestIntent.toShortString(false, true, true, true));
        }
        if (this.sent || this.canceled) {
            printWriter.print(str);
            printWriter.print("sent=");
            printWriter.print(this.sent);
            printWriter.print(" canceled=");
            printWriter.println(this.canceled);
        }
        if (this.whitelistDuration != null) {
            printWriter.print(str);
            printWriter.print("whitelistDuration=");
            for (int i = 0; i < this.whitelistDuration.size(); i++) {
                if (i != 0) {
                    printWriter.print(", ");
                }
                printWriter.print(Integer.toHexString(System.identityHashCode(this.whitelistDuration.keyAt(i))));
                printWriter.print(":");
                TimeUtils.formatDuration(this.whitelistDuration.valueAt(i).longValue(), printWriter);
            }
            printWriter.println();
        }
        if (this.mCancelCallbacks != null) {
            printWriter.print(str);
            printWriter.println("mCancelCallbacks:");
            for (int i2 = 0; i2 < this.mCancelCallbacks.getRegisteredCallbackCount(); i2++) {
                printWriter.print(str);
                printWriter.print("  #");
                printWriter.print(i2);
                printWriter.print(": ");
                printWriter.println(this.mCancelCallbacks.getRegisteredCallbackItem(i2));
            }
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("PendingIntentRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.key.packageName);
        sb.append(' ');
        sb.append(this.key.typeName());
        if (this.whitelistDuration != null) {
            sb.append(" (whitelist: ");
            for (int i = 0; i < this.whitelistDuration.size(); i++) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(Integer.toHexString(System.identityHashCode(this.whitelistDuration.keyAt(i))));
                sb.append(":");
                TimeUtils.formatDuration(this.whitelistDuration.valueAt(i).longValue(), sb);
            }
            sb.append(")");
        }
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }
}
