package com.android.server.am;

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
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

final class BroadcastRecord extends Binder {
    static final int APP_RECEIVE = 1;
    static final int CALL_DONE_RECEIVE = 3;
    static final int CALL_IN_RECEIVE = 2;
    static final int IDLE = 0;
    static final int WAITING_SERVICES = 4;
    int anrCount;
    final int appOp;
    final ProcessRecord callerApp;
    final String callerPackage;
    final int callingPid;
    final int callingUid;
    ProcessRecord curApp;
    ComponentName curComponent;
    BroadcastFilter curFilter;
    ActivityInfo curReceiver;
    long dispatchClockTime;
    long dispatchTime;
    long finishTime;
    final boolean initialSticky;
    final Intent intent;
    final boolean ordered;
    BroadcastQueue queue;
    IBinder receiver;
    long receiverTime;
    final List receivers;
    final String requiredPermission;
    final String resolvedType;
    boolean resultAbort;
    int resultCode;
    String resultData;
    Bundle resultExtras;
    IIntentReceiver resultTo;
    final boolean sticky;
    final ComponentName targetComp;
    final int userId;
    int nextReceiver = 0;
    int state = 0;

    void dump(PrintWriter pw, String prefix) {
        long now = SystemClock.uptimeMillis();
        pw.print(prefix);
        pw.print(this);
        pw.print(" to user ");
        pw.println(this.userId);
        pw.print(prefix);
        pw.println(this.intent.toInsecureString());
        if (this.targetComp != null && this.targetComp != this.intent.getComponent()) {
            pw.print(prefix);
            pw.print("  targetComp: ");
            pw.println(this.targetComp.toShortString());
        }
        Bundle bundle = this.intent.getExtras();
        if (bundle != null) {
            pw.print(prefix);
            pw.print("  extras: ");
            pw.println(bundle.toString());
        }
        pw.print(prefix);
        pw.print("caller=");
        pw.print(this.callerPackage);
        pw.print(" ");
        pw.print(this.callerApp != null ? this.callerApp.toShortString() : "null");
        pw.print(" pid=");
        pw.print(this.callingPid);
        pw.print(" uid=");
        pw.println(this.callingUid);
        if (this.requiredPermission != null || this.appOp != -1) {
            pw.print(prefix);
            pw.print("requiredPermission=");
            pw.print(this.requiredPermission);
            pw.print("  appOp=");
            pw.println(this.appOp);
        }
        pw.print(prefix);
        pw.print("dispatchClockTime=");
        pw.println(new Date(this.dispatchClockTime));
        pw.print(prefix);
        pw.print("dispatchTime=");
        TimeUtils.formatDuration(this.dispatchTime, now, pw);
        if (this.finishTime != 0) {
            pw.print(" finishTime=");
            TimeUtils.formatDuration(this.finishTime, now, pw);
        } else {
            pw.print(" receiverTime=");
            TimeUtils.formatDuration(this.receiverTime, now, pw);
        }
        pw.println("");
        if (this.anrCount != 0) {
            pw.print(prefix);
            pw.print("anrCount=");
            pw.println(this.anrCount);
        }
        if (this.resultTo != null || this.resultCode != -1 || this.resultData != null) {
            pw.print(prefix);
            pw.print("resultTo=");
            pw.print(this.resultTo);
            pw.print(" resultCode=");
            pw.print(this.resultCode);
            pw.print(" resultData=");
            pw.println(this.resultData);
        }
        if (this.resultExtras != null) {
            pw.print(prefix);
            pw.print("resultExtras=");
            pw.println(this.resultExtras);
        }
        if (this.resultAbort || this.ordered || this.sticky || this.initialSticky) {
            pw.print(prefix);
            pw.print("resultAbort=");
            pw.print(this.resultAbort);
            pw.print(" ordered=");
            pw.print(this.ordered);
            pw.print(" sticky=");
            pw.print(this.sticky);
            pw.print(" initialSticky=");
            pw.println(this.initialSticky);
        }
        if (this.nextReceiver != 0 || this.receiver != null) {
            pw.print(prefix);
            pw.print("nextReceiver=");
            pw.print(this.nextReceiver);
            pw.print(" receiver=");
            pw.println(this.receiver);
        }
        if (this.curFilter != null) {
            pw.print(prefix);
            pw.print("curFilter=");
            pw.println(this.curFilter);
        }
        if (this.curReceiver != null) {
            pw.print(prefix);
            pw.print("curReceiver=");
            pw.println(this.curReceiver);
        }
        if (this.curApp != null) {
            pw.print(prefix);
            pw.print("curApp=");
            pw.println(this.curApp);
            pw.print(prefix);
            pw.print("curComponent=");
            pw.println(this.curComponent != null ? this.curComponent.toShortString() : "--");
            if (this.curReceiver != null && this.curReceiver.applicationInfo != null) {
                pw.print(prefix);
                pw.print("curSourceDir=");
                pw.println(this.curReceiver.applicationInfo.sourceDir);
            }
        }
        if (this.state != 0) {
            String stateStr = " (?)";
            switch (this.state) {
                case 1:
                    stateStr = " (APP_RECEIVE)";
                    break;
                case 2:
                    stateStr = " (CALL_IN_RECEIVE)";
                    break;
                case 3:
                    stateStr = " (CALL_DONE_RECEIVE)";
                    break;
                case 4:
                    stateStr = " (WAITING_SERVICES)";
                    break;
            }
            pw.print(prefix);
            pw.print("state=");
            pw.print(this.state);
            pw.println(stateStr);
        }
        int N = this.receivers != null ? this.receivers.size() : 0;
        String p2 = prefix + "  ";
        PrintWriterPrinter printer = new PrintWriterPrinter(pw);
        for (int i = 0; i < N; i++) {
            Object o = this.receivers.get(i);
            pw.print(prefix);
            pw.print("Receiver #");
            pw.print(i);
            pw.print(": ");
            pw.println(o);
            if (o instanceof BroadcastFilter) {
                ((BroadcastFilter) o).dumpBrief(pw, p2);
            } else if (o instanceof ResolveInfo) {
                ((ResolveInfo) o).dump(printer, p2);
            }
        }
    }

    BroadcastRecord(BroadcastQueue _queue, Intent _intent, ProcessRecord _callerApp, String _callerPackage, int _callingPid, int _callingUid, String _resolvedType, String _requiredPermission, int _appOp, List _receivers, IIntentReceiver _resultTo, int _resultCode, String _resultData, Bundle _resultExtras, boolean _serialized, boolean _sticky, boolean _initialSticky, int _userId) {
        this.queue = _queue;
        this.intent = _intent;
        this.targetComp = _intent.getComponent();
        this.callerApp = _callerApp;
        this.callerPackage = _callerPackage;
        this.callingPid = _callingPid;
        this.callingUid = _callingUid;
        this.resolvedType = _resolvedType;
        this.requiredPermission = _requiredPermission;
        this.appOp = _appOp;
        this.receivers = _receivers;
        this.resultTo = _resultTo;
        this.resultCode = _resultCode;
        this.resultData = _resultData;
        this.resultExtras = _resultExtras;
        this.ordered = _serialized;
        this.sticky = _sticky;
        this.initialSticky = _initialSticky;
        this.userId = _userId;
    }

    public String toString() {
        return "BroadcastRecord{" + Integer.toHexString(System.identityHashCode(this)) + " u" + this.userId + " " + this.intent.getAction() + "}";
    }
}
