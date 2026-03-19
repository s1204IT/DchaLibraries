package com.android.server.am;

import android.app.IActivityContainer;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.TransactionTooLargeException;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.am.ActivityStack;
import com.android.server.am.ActivityStackSupervisor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;

final class PendingIntentRecord extends IIntentSender.Stub {
    private static final String TAG = "ActivityManager";
    final Key key;
    String lastTag;
    String lastTagPrefix;
    final ActivityManagerService owner;
    String stringName;
    final int uid;
    boolean sent = false;
    boolean canceled = false;
    private long whitelistDuration = 0;
    final WeakReference<PendingIntentRecord> ref = new WeakReference<>(this);

    static final class Key {
        private static final int ODD_PRIME_NUMBER = 37;
        final ActivityRecord activity;
        Intent[] allIntents;
        String[] allResolvedTypes;
        final int flags;
        final int hashCode;
        final Bundle options;
        final String packageName;
        final int requestCode;
        final Intent requestIntent;
        final String requestResolvedType;
        final int type;
        final int userId;
        final String who;

        Key(int _t, String _p, ActivityRecord _a, String _w, int _r, Intent[] _i, String[] _it, int _f, Bundle _o, int _userId) {
            this.type = _t;
            this.packageName = _p;
            this.activity = _a;
            this.who = _w;
            this.requestCode = _r;
            this.requestIntent = _i != null ? _i[_i.length - 1] : null;
            this.requestResolvedType = _it != null ? _it[_it.length - 1] : null;
            this.allIntents = _i;
            this.allResolvedTypes = _it;
            this.flags = _f;
            this.options = _o;
            this.userId = _userId;
            int hash = ((((_f + 851) * 37) + _r) * 37) + _userId;
            hash = _w != null ? (hash * 37) + _w.hashCode() : hash;
            hash = _a != null ? (hash * 37) + _a.hashCode() : hash;
            hash = this.requestIntent != null ? (hash * 37) + this.requestIntent.filterHashCode() : hash;
            hash = this.requestResolvedType != null ? (hash * 37) + this.requestResolvedType.hashCode() : hash;
            this.hashCode = ((_p != null ? (hash * 37) + _p.hashCode() : hash) * 37) + _t;
            if (this.packageName == null) {
                Slog.d(PendingIntentRecord.TAG, "special case: packageName is null for " + this + " hashCode=0x" + Integer.toHexString(this.hashCode) + " ar=" + this.activity + " who=" + this.who + " r= " + this.requestCode + " rt= " + this.requestResolvedType);
            }
        }

        public boolean equals(Object otherObj) {
            if (otherObj == null) {
                return false;
            }
            try {
                Key other = (Key) otherObj;
                if (this.type != other.type || this.userId != other.userId) {
                    return false;
                }
                if (this.packageName != null) {
                    if (!this.packageName.equals(other.packageName)) {
                        return false;
                    }
                } else if (this.packageName == null && other.packageName != null) {
                    return false;
                }
                if (this.activity != other.activity) {
                    return false;
                }
                if (this.who != other.who) {
                    if (this.who != null) {
                        if (!this.who.equals(other.who)) {
                            return false;
                        }
                    } else if (other.who != null) {
                        return false;
                    }
                }
                if (this.requestCode != other.requestCode) {
                    return false;
                }
                if (this.requestIntent != other.requestIntent) {
                    if (this.requestIntent != null) {
                        if (!this.requestIntent.filterEquals(other.requestIntent)) {
                            return false;
                        }
                    } else if (other.requestIntent != null) {
                        return false;
                    }
                }
                if (this.requestResolvedType != other.requestResolvedType) {
                    if (this.requestResolvedType != null) {
                        if (!this.requestResolvedType.equals(other.requestResolvedType)) {
                            return false;
                        }
                    } else if (other.requestResolvedType != null) {
                        return false;
                    }
                }
                if (this.flags != other.flags) {
                    return false;
                }
                if (this.packageName == null) {
                    Slog.d(PendingIntentRecord.TAG, "special case: packageName is null for " + this + " hashCode=0x" + Integer.toHexString(this.hashCode));
                    return true;
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
            return "Key{" + typeName() + " pkg=" + this.packageName + " intent=" + (this.requestIntent != null ? this.requestIntent.toShortString(false, true, false, false) : "<null>") + " flags=0x" + Integer.toHexString(this.flags) + " u=" + this.userId + "}";
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
                default:
                    return Integer.toString(this.type);
            }
        }
    }

    PendingIntentRecord(ActivityManagerService _owner, Key _k, int _u) {
        this.owner = _owner;
        this.key = _k;
        this.uid = _u;
    }

    void setWhitelistDuration(long duration) {
        this.whitelistDuration = duration;
        this.stringName = null;
    }

    public void send(int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        sendInner(code, intent, resolvedType, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options, null);
    }

    public int sendWithResult(int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
        return sendInner(code, intent, resolvedType, finishedReceiver, requiredPermission, null, null, 0, 0, 0, options, null);
    }

    int sendInner(int code, Intent intent, String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, IBinder resultTo, String resultWho, int requestCode, int flagsMask, int flagsValues, Bundle options, IActivityContainer container) {
        if (intent != null) {
            intent.setDefusable(true);
        }
        if (options != null) {
            options.setDefusable(true);
        }
        if (this.whitelistDuration > 0 && !this.canceled) {
            this.owner.tempWhitelistAppForPowerSave(Binder.getCallingPid(), Binder.getCallingUid(), this.uid, this.whitelistDuration);
        }
        synchronized (this.owner) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ActivityStackSupervisor.ActivityContainer activityContainer = (ActivityStackSupervisor.ActivityContainer) container;
                if (activityContainer != null && activityContainer.mParentActivity != null && activityContainer.mParentActivity.state != ActivityStack.ActivityState.RESUMED) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return -6;
                }
                if (this.canceled) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return -6;
                }
                this.sent = true;
                if ((this.key.flags & 1073741824) != 0) {
                    this.owner.cancelIntentSenderLocked(this, true);
                    this.canceled = true;
                }
                Intent finalIntent = this.key.requestIntent != null ? new Intent(this.key.requestIntent) : new Intent();
                boolean immutable = (this.key.flags & 67108864) != 0;
                if (immutable) {
                    resolvedType = this.key.requestResolvedType;
                } else {
                    if (intent != null) {
                        int changes = finalIntent.fillIn(intent, this.key.flags);
                        if ((changes & 2) == 0) {
                            resolvedType = this.key.requestResolvedType;
                        }
                    } else {
                        resolvedType = this.key.requestResolvedType;
                    }
                    int flagsMask2 = flagsMask & (-196);
                    finalIntent.setFlags((finalIntent.getFlags() & (~flagsMask2)) | (flagsValues & flagsMask2));
                }
                long origId = Binder.clearCallingIdentity();
                boolean sendFinish = finishedReceiver != null;
                int userId = this.key.userId;
                if (userId == -2) {
                    userId = this.owner.mUserController.getCurrentOrTargetUserIdLocked();
                }
                int res = 0;
                switch (this.key.type) {
                    case 1:
                        try {
                            int sent = this.owner.broadcastIntentInPackage(this.key.packageName, this.uid, finalIntent, resolvedType, finishedReceiver, code, null, null, requiredPermission, options, finishedReceiver != null, false, userId);
                            if (sent == 0) {
                                sendFinish = false;
                            }
                        } catch (RuntimeException e) {
                            Slog.w(TAG, "Unable to send startActivity intent", e);
                        }
                        if (sendFinish && res != -6) {
                            try {
                                finishedReceiver.performReceive(new Intent(finalIntent), 0, (String) null, (Bundle) null, false, false, this.key.userId);
                                break;
                            } catch (RemoteException e2) {
                            }
                        }
                        Binder.restoreCallingIdentity(origId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return res;
                    case 2:
                        if (options == null) {
                            options = this.key.options;
                        } else if (this.key.options != null) {
                            Bundle opts = new Bundle(this.key.options);
                            opts.putAll(options);
                            options = opts;
                        }
                        try {
                            if (this.key.allIntents == null || this.key.allIntents.length <= 1) {
                                this.owner.startActivityInPackage(this.uid, this.key.packageName, finalIntent, resolvedType, resultTo, resultWho, requestCode, 0, options, userId, container, null);
                            } else {
                                Intent[] allIntents = new Intent[this.key.allIntents.length];
                                String[] allResolvedTypes = new String[this.key.allIntents.length];
                                System.arraycopy(this.key.allIntents, 0, allIntents, 0, this.key.allIntents.length);
                                if (this.key.allResolvedTypes != null) {
                                    System.arraycopy(this.key.allResolvedTypes, 0, allResolvedTypes, 0, this.key.allResolvedTypes.length);
                                }
                                allIntents[allIntents.length - 1] = finalIntent;
                                allResolvedTypes[allResolvedTypes.length - 1] = resolvedType;
                                this.owner.startActivitiesInPackage(this.uid, this.key.packageName, allIntents, allResolvedTypes, resultTo, options, userId);
                            }
                        } catch (RuntimeException e3) {
                            Slog.w(TAG, "Unable to send startActivity intent", e3);
                        }
                        if (sendFinish) {
                            finishedReceiver.performReceive(new Intent(finalIntent), 0, (String) null, (Bundle) null, false, false, this.key.userId);
                            break;
                        }
                        Binder.restoreCallingIdentity(origId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return res;
                    case 3:
                        if (this.key.activity.task.stack != null) {
                            this.key.activity.task.stack.sendActivityResultLocked(-1, this.key.activity, this.key.who, this.key.requestCode, code, finalIntent);
                        }
                        if (sendFinish) {
                        }
                        Binder.restoreCallingIdentity(origId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return res;
                    case 4:
                        try {
                            this.owner.startServiceInPackage(this.uid, finalIntent, resolvedType, this.key.packageName, userId);
                            break;
                        } catch (TransactionTooLargeException e4) {
                            res = -6;
                        } catch (RuntimeException e5) {
                            Slog.w(TAG, "Unable to send startService intent", e5);
                        }
                        if (sendFinish) {
                        }
                        Binder.restoreCallingIdentity(origId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return res;
                    default:
                        if (sendFinish) {
                        }
                        Binder.restoreCallingIdentity(origId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return res;
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.canceled) {
                this.owner.mHandler.sendMessage(this.owner.mHandler.obtainMessage(23, this));
            }
        } finally {
            super.finalize();
        }
    }

    public void completeFinalize() {
        synchronized (this.owner) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                WeakReference<PendingIntentRecord> current = this.owner.mIntentSenderRecords.get(this.key);
                if (current == this.ref) {
                    this.owner.mIntentSenderRecords.remove(this.key);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("uid=");
        pw.print(this.uid);
        pw.print(" packageName=");
        pw.print(this.key.packageName);
        pw.print(" type=");
        pw.print(this.key.typeName());
        pw.print(" flags=0x");
        pw.println(Integer.toHexString(this.key.flags));
        if (this.key.activity != null || this.key.who != null) {
            pw.print(prefix);
            pw.print("activity=");
            pw.print(this.key.activity);
            pw.print(" who=");
            pw.println(this.key.who);
        }
        if (this.key.requestCode != 0 || this.key.requestResolvedType != null) {
            pw.print(prefix);
            pw.print("requestCode=");
            pw.print(this.key.requestCode);
            pw.print(" requestResolvedType=");
            pw.println(this.key.requestResolvedType);
        }
        if (this.key.requestIntent != null) {
            pw.print(prefix);
            pw.print("requestIntent=");
            pw.println(this.key.requestIntent.toShortString(false, true, true, true));
        }
        if (this.sent || this.canceled) {
            pw.print(prefix);
            pw.print("sent=");
            pw.print(this.sent);
            pw.print(" canceled=");
            pw.println(this.canceled);
        }
        if (this.whitelistDuration == 0) {
            return;
        }
        pw.print(prefix);
        pw.print("whitelistDuration=");
        TimeUtils.formatDuration(this.whitelistDuration, pw);
        pw.println();
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
        if (this.whitelistDuration > 0) {
            sb.append(" (whitelist: ");
            TimeUtils.formatDuration(this.whitelistDuration, sb);
            sb.append(")");
        }
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }
}
