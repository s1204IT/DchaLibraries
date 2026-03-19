package com.android.server.am;

import android.content.IntentFilter;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;

final class BroadcastFilter extends IntentFilter {
    final boolean instantApp;
    final int owningUid;
    final int owningUserId;
    final String packageName;
    final ReceiverList receiverList;
    final String requiredPermission;
    final boolean visibleToInstantApp;

    BroadcastFilter(IntentFilter intentFilter, ReceiverList receiverList, String str, String str2, int i, int i2, boolean z, boolean z2) {
        super(intentFilter);
        this.receiverList = receiverList;
        this.packageName = str;
        this.requiredPermission = str2;
        this.owningUid = i;
        this.owningUserId = i2;
        this.instantApp = z;
        this.visibleToInstantApp = z2;
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        super.writeToProto(protoOutputStream, 1146756268033L);
        if (this.requiredPermission != null) {
            protoOutputStream.write(1138166333442L, this.requiredPermission);
        }
        protoOutputStream.write(1138166333443L, Integer.toHexString(System.identityHashCode(this)));
        protoOutputStream.write(1120986464260L, this.owningUserId);
        protoOutputStream.end(jStart);
    }

    public void dump(PrintWriter printWriter, String str) {
        dumpInReceiverList(printWriter, new PrintWriterPrinter(printWriter), str);
        this.receiverList.dumpLocal(printWriter, str);
    }

    public void dumpBrief(PrintWriter printWriter, String str) {
        dumpBroadcastFilterState(printWriter, str);
    }

    public void dumpInReceiverList(PrintWriter printWriter, Printer printer, String str) {
        super.dump(printer, str);
        dumpBroadcastFilterState(printWriter, str);
    }

    void dumpBroadcastFilterState(PrintWriter printWriter, String str) {
        if (this.requiredPermission != null) {
            printWriter.print(str);
            printWriter.print("requiredPermission=");
            printWriter.println(this.requiredPermission);
        }
    }

    public String toString() {
        return "BroadcastFilter{" + Integer.toHexString(System.identityHashCode(this)) + " u" + this.owningUserId + ' ' + this.receiverList + '}';
    }
}
