package com.android.server.am;

import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.PrintWriterPrinter;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

final class BroadcastRecord extends Binder {
    static final int APP_RECEIVE = 1;
    static final int CALL_DONE_RECEIVE = 3;
    static final int CALL_IN_RECEIVE = 2;
    static final int DELIVERY_DELIVERED = 1;
    static final int DELIVERY_PENDING = 0;
    static final int DELIVERY_SKIPPED = 2;
    static final int DELIVERY_TIMEOUT = 3;
    static final int IDLE = 0;
    static final int WAITING_SERVICES = 4;
    int anrCount;
    final int appOp;
    final ProcessRecord callerApp;
    final boolean callerInstantApp;
    final String callerPackage;
    final int callingPid;
    final int callingUid;
    ProcessRecord curApp;
    ComponentName curComponent;
    BroadcastFilter curFilter;
    ActivityInfo curReceiver;
    final int[] delivery;
    long dispatchClockTime;
    long dispatchTime;
    long enqueueClockTime;
    long finishTime;
    final boolean initialSticky;
    final Intent intent;
    int manifestCount;
    int manifestSkipCount;
    int nextReceiver;
    final BroadcastOptions options;
    final boolean ordered;
    BroadcastQueue queue;
    IBinder receiver;
    long receiverTime;
    final List receivers;
    final String[] requiredPermissions;
    final String resolvedType;
    boolean resultAbort;
    int resultCode;
    String resultData;
    Bundle resultExtras;
    IIntentReceiver resultTo;
    int state;
    final boolean sticky;
    final ComponentName targetComp;
    final int userId;

    void dump(PrintWriter printWriter, String str, SimpleDateFormat simpleDateFormat) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        printWriter.print(str);
        printWriter.print(this);
        printWriter.print(" to user ");
        printWriter.println(this.userId);
        printWriter.print(str);
        printWriter.println(this.intent.toInsecureString());
        if (this.targetComp != null && this.targetComp != this.intent.getComponent()) {
            printWriter.print(str);
            printWriter.print("  targetComp: ");
            printWriter.println(this.targetComp.toShortString());
        }
        Bundle extras = this.intent.getExtras();
        if (extras != null) {
            printWriter.print(str);
            printWriter.print("  extras: ");
            printWriter.println(extras.toString());
        }
        printWriter.print(str);
        printWriter.print("caller=");
        printWriter.print(this.callerPackage);
        printWriter.print(" ");
        printWriter.print(this.callerApp != null ? this.callerApp.toShortString() : "null");
        printWriter.print(" pid=");
        printWriter.print(this.callingPid);
        printWriter.print(" uid=");
        printWriter.println(this.callingUid);
        if ((this.requiredPermissions != null && this.requiredPermissions.length > 0) || this.appOp != -1) {
            printWriter.print(str);
            printWriter.print("requiredPermissions=");
            printWriter.print(Arrays.toString(this.requiredPermissions));
            printWriter.print("  appOp=");
            printWriter.println(this.appOp);
        }
        if (this.options != null) {
            printWriter.print(str);
            printWriter.print("options=");
            printWriter.println(this.options.toBundle());
        }
        printWriter.print(str);
        printWriter.print("enqueueClockTime=");
        printWriter.print(simpleDateFormat.format(new Date(this.enqueueClockTime)));
        printWriter.print(" dispatchClockTime=");
        printWriter.println(simpleDateFormat.format(new Date(this.dispatchClockTime)));
        printWriter.print(str);
        printWriter.print("dispatchTime=");
        TimeUtils.formatDuration(this.dispatchTime, jUptimeMillis, printWriter);
        printWriter.print(" (");
        TimeUtils.formatDuration(this.dispatchClockTime - this.enqueueClockTime, printWriter);
        printWriter.print(" since enq)");
        if (this.finishTime != 0) {
            printWriter.print(" finishTime=");
            TimeUtils.formatDuration(this.finishTime, jUptimeMillis, printWriter);
            printWriter.print(" (");
            TimeUtils.formatDuration(this.finishTime - this.dispatchTime, printWriter);
            printWriter.print(" since disp)");
        } else {
            printWriter.print(" receiverTime=");
            TimeUtils.formatDuration(this.receiverTime, jUptimeMillis, printWriter);
        }
        printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        if (this.anrCount != 0) {
            printWriter.print(str);
            printWriter.print("anrCount=");
            printWriter.println(this.anrCount);
        }
        if (this.resultTo != null || this.resultCode != -1 || this.resultData != null) {
            printWriter.print(str);
            printWriter.print("resultTo=");
            printWriter.print(this.resultTo);
            printWriter.print(" resultCode=");
            printWriter.print(this.resultCode);
            printWriter.print(" resultData=");
            printWriter.println(this.resultData);
        }
        if (this.resultExtras != null) {
            printWriter.print(str);
            printWriter.print("resultExtras=");
            printWriter.println(this.resultExtras);
        }
        if (this.resultAbort || this.ordered || this.sticky || this.initialSticky) {
            printWriter.print(str);
            printWriter.print("resultAbort=");
            printWriter.print(this.resultAbort);
            printWriter.print(" ordered=");
            printWriter.print(this.ordered);
            printWriter.print(" sticky=");
            printWriter.print(this.sticky);
            printWriter.print(" initialSticky=");
            printWriter.println(this.initialSticky);
        }
        if (this.nextReceiver != 0 || this.receiver != null) {
            printWriter.print(str);
            printWriter.print("nextReceiver=");
            printWriter.print(this.nextReceiver);
            printWriter.print(" receiver=");
            printWriter.println(this.receiver);
        }
        if (this.curFilter != null) {
            printWriter.print(str);
            printWriter.print("curFilter=");
            printWriter.println(this.curFilter);
        }
        if (this.curReceiver != null) {
            printWriter.print(str);
            printWriter.print("curReceiver=");
            printWriter.println(this.curReceiver);
        }
        if (this.curApp != null) {
            printWriter.print(str);
            printWriter.print("curApp=");
            printWriter.println(this.curApp);
            printWriter.print(str);
            printWriter.print("curComponent=");
            printWriter.println(this.curComponent != null ? this.curComponent.toShortString() : "--");
            if (this.curReceiver != null && this.curReceiver.applicationInfo != null) {
                printWriter.print(str);
                printWriter.print("curSourceDir=");
                printWriter.println(this.curReceiver.applicationInfo.sourceDir);
            }
        }
        if (this.state != 0) {
            String str2 = " (?)";
            switch (this.state) {
                case 1:
                    str2 = " (APP_RECEIVE)";
                    break;
                case 2:
                    str2 = " (CALL_IN_RECEIVE)";
                    break;
                case 3:
                    str2 = " (CALL_DONE_RECEIVE)";
                    break;
                case 4:
                    str2 = " (WAITING_SERVICES)";
                    break;
            }
            printWriter.print(str);
            printWriter.print("state=");
            printWriter.print(this.state);
            printWriter.println(str2);
        }
        int size = this.receivers != null ? this.receivers.size() : 0;
        String str3 = str + "  ";
        PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(printWriter);
        for (int i = 0; i < size; i++) {
            Object obj = this.receivers.get(i);
            printWriter.print(str);
            switch (this.delivery[i]) {
                case 0:
                    printWriter.print("Pending");
                    break;
                case 1:
                    printWriter.print("Deliver");
                    break;
                case 2:
                    printWriter.print("Skipped");
                    break;
                case 3:
                    printWriter.print("Timeout");
                    break;
                default:
                    printWriter.print("???????");
                    break;
            }
            printWriter.print(" #");
            printWriter.print(i);
            printWriter.print(": ");
            if (obj instanceof BroadcastFilter) {
                printWriter.println(obj);
                ((BroadcastFilter) obj).dumpBrief(printWriter, str3);
            } else if (obj instanceof ResolveInfo) {
                printWriter.println("(manifest)");
                ((ResolveInfo) obj).dump(printWriterPrinter, str3, 0);
            } else {
                printWriter.println(obj);
            }
        }
    }

    BroadcastRecord(BroadcastQueue broadcastQueue, Intent intent, ProcessRecord processRecord, String str, int i, int i2, boolean z, String str2, String[] strArr, int i3, BroadcastOptions broadcastOptions, List list, IIntentReceiver iIntentReceiver, int i4, String str3, Bundle bundle, boolean z2, boolean z3, boolean z4, int i5) {
        if (intent == null) {
            throw new NullPointerException("Can't construct with a null intent");
        }
        this.queue = broadcastQueue;
        this.intent = intent;
        this.targetComp = intent.getComponent();
        this.callerApp = processRecord;
        this.callerPackage = str;
        this.callingPid = i;
        this.callingUid = i2;
        this.callerInstantApp = z;
        this.resolvedType = str2;
        this.requiredPermissions = strArr;
        this.appOp = i3;
        this.options = broadcastOptions;
        this.receivers = list;
        this.delivery = new int[list != null ? list.size() : 0];
        this.resultTo = iIntentReceiver;
        this.resultCode = i4;
        this.resultData = str3;
        this.resultExtras = bundle;
        this.ordered = z2;
        this.sticky = z3;
        this.initialSticky = z4;
        this.userId = i5;
        this.nextReceiver = 0;
        this.state = 0;
    }

    private BroadcastRecord(BroadcastRecord broadcastRecord, Intent intent) {
        this.intent = intent;
        this.targetComp = intent.getComponent();
        this.callerApp = broadcastRecord.callerApp;
        this.callerPackage = broadcastRecord.callerPackage;
        this.callingPid = broadcastRecord.callingPid;
        this.callingUid = broadcastRecord.callingUid;
        this.callerInstantApp = broadcastRecord.callerInstantApp;
        this.ordered = broadcastRecord.ordered;
        this.sticky = broadcastRecord.sticky;
        this.initialSticky = broadcastRecord.initialSticky;
        this.userId = broadcastRecord.userId;
        this.resolvedType = broadcastRecord.resolvedType;
        this.requiredPermissions = broadcastRecord.requiredPermissions;
        this.appOp = broadcastRecord.appOp;
        this.options = broadcastRecord.options;
        this.receivers = broadcastRecord.receivers;
        this.delivery = broadcastRecord.delivery;
        this.resultTo = broadcastRecord.resultTo;
        this.enqueueClockTime = broadcastRecord.enqueueClockTime;
        this.dispatchTime = broadcastRecord.dispatchTime;
        this.dispatchClockTime = broadcastRecord.dispatchClockTime;
        this.receiverTime = broadcastRecord.receiverTime;
        this.finishTime = broadcastRecord.finishTime;
        this.resultCode = broadcastRecord.resultCode;
        this.resultData = broadcastRecord.resultData;
        this.resultExtras = broadcastRecord.resultExtras;
        this.resultAbort = broadcastRecord.resultAbort;
        this.nextReceiver = broadcastRecord.nextReceiver;
        this.receiver = broadcastRecord.receiver;
        this.state = broadcastRecord.state;
        this.anrCount = broadcastRecord.anrCount;
        this.manifestCount = broadcastRecord.manifestCount;
        this.manifestSkipCount = broadcastRecord.manifestSkipCount;
        this.queue = broadcastRecord.queue;
    }

    public BroadcastRecord maybeStripForHistory() {
        if (!this.intent.canStripForHistory()) {
            return this;
        }
        return new BroadcastRecord(this, this.intent.maybeStripForHistory());
    }

    boolean cleanupDisabledPackageReceiversLocked(String str, Set<String> set, int i, boolean z) {
        if ((i != -1 && this.userId != i) || this.receivers == null) {
            return false;
        }
        boolean z2 = false;
        for (int size = this.receivers.size() - 1; size >= 0; size--) {
            Object obj = this.receivers.get(size);
            if (obj instanceof ResolveInfo) {
                ActivityInfo activityInfo = ((ResolveInfo) obj).activityInfo;
                if (!(str == null || (activityInfo.applicationInfo.packageName.equals(str) && (set == null || set.contains(activityInfo.name))))) {
                    continue;
                } else {
                    if (!z) {
                        return true;
                    }
                    this.receivers.remove(size);
                    if (size < this.nextReceiver) {
                        this.nextReceiver--;
                    }
                    z2 = true;
                }
            }
        }
        this.nextReceiver = Math.min(this.nextReceiver, this.receivers.size());
        return z2;
    }

    public String toString() {
        return "BroadcastRecord{" + Integer.toHexString(System.identityHashCode(this)) + " u" + this.userId + " " + this.intent.getAction() + "}";
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.userId);
        protoOutputStream.write(1138166333442L, this.intent.getAction());
        protoOutputStream.end(jStart);
    }
}
