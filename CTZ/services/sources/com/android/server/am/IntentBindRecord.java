package com.android.server.am;

import android.content.Intent;
import android.os.IBinder;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;

final class IntentBindRecord {
    final ArrayMap<ProcessRecord, AppBindRecord> apps = new ArrayMap<>();
    IBinder binder;
    boolean doRebind;
    boolean hasBound;
    final Intent.FilterComparison intent;
    boolean received;
    boolean requested;
    final ServiceRecord service;
    String stringName;

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("service=");
        printWriter.println(this.service);
        dumpInService(printWriter, str);
    }

    void dumpInService(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("intent={");
        printWriter.print(this.intent.getIntent().toShortString(false, true, false, false));
        printWriter.println('}');
        printWriter.print(str);
        printWriter.print("binder=");
        printWriter.println(this.binder);
        printWriter.print(str);
        printWriter.print("requested=");
        printWriter.print(this.requested);
        printWriter.print(" received=");
        printWriter.print(this.received);
        printWriter.print(" hasBound=");
        printWriter.print(this.hasBound);
        printWriter.print(" doRebind=");
        printWriter.println(this.doRebind);
        for (int i = 0; i < this.apps.size(); i++) {
            AppBindRecord appBindRecordValueAt = this.apps.valueAt(i);
            printWriter.print(str);
            printWriter.print("* Client AppBindRecord{");
            printWriter.print(Integer.toHexString(System.identityHashCode(appBindRecordValueAt)));
            printWriter.print(' ');
            printWriter.print(appBindRecordValueAt.client);
            printWriter.println('}');
            appBindRecordValueAt.dumpInIntentBind(printWriter, str + "  ");
        }
    }

    IntentBindRecord(ServiceRecord serviceRecord, Intent.FilterComparison filterComparison) {
        this.service = serviceRecord;
        this.intent = filterComparison;
    }

    int collectFlags() {
        int i = 0;
        for (int size = this.apps.size() - 1; size >= 0; size--) {
            ArraySet<ConnectionRecord> arraySet = this.apps.valueAt(size).connections;
            for (int size2 = arraySet.size() - 1; size2 >= 0; size2--) {
                i |= arraySet.valueAt(size2).flags;
            }
        }
        return i;
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("IntentBindRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        if ((collectFlags() & 1) != 0) {
            sb.append("CR ");
        }
        sb.append(this.service.shortName);
        sb.append(':');
        if (this.intent != null) {
            this.intent.getIntent().toShortString(sb, false, false, false, false);
        }
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        if (this.intent != null) {
            this.intent.getIntent().writeToProto(protoOutputStream, 1146756268033L, false, true, false, false);
        }
        if (this.binder != null) {
            protoOutputStream.write(1138166333442L, this.binder.toString());
        }
        protoOutputStream.write(1133871366147L, (collectFlags() & 1) != 0);
        protoOutputStream.write(1133871366148L, this.requested);
        protoOutputStream.write(1133871366149L, this.received);
        protoOutputStream.write(1133871366150L, this.hasBound);
        protoOutputStream.write(1133871366151L, this.doRebind);
        int size = this.apps.size();
        for (int i = 0; i < size; i++) {
            AppBindRecord appBindRecordValueAt = this.apps.valueAt(i);
            if (appBindRecordValueAt != null) {
                appBindRecordValueAt.writeToProto(protoOutputStream, 2246267895816L);
            }
        }
        protoOutputStream.end(jStart);
    }
}
