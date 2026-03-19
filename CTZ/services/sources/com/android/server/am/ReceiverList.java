package com.android.server.am;

import android.content.IIntentReceiver;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.PrintWriterPrinter;
import android.util.proto.ProtoOutputStream;
import com.android.server.IntentResolver;
import java.io.PrintWriter;
import java.util.ArrayList;

final class ReceiverList extends ArrayList<BroadcastFilter> implements IBinder.DeathRecipient {
    public final ProcessRecord app;
    BroadcastRecord curBroadcast = null;
    boolean linkedToDeath = false;
    final ActivityManagerService owner;
    public final int pid;
    public final IIntentReceiver receiver;
    String stringName;
    public final int uid;
    public final int userId;

    ReceiverList(ActivityManagerService activityManagerService, ProcessRecord processRecord, int i, int i2, int i3, IIntentReceiver iIntentReceiver) {
        this.owner = activityManagerService;
        this.receiver = iIntentReceiver;
        this.app = processRecord;
        this.pid = i;
        this.uid = i2;
        this.userId = i3;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public void binderDied() {
        this.linkedToDeath = false;
        this.owner.unregisterReceiver(this.receiver);
    }

    public boolean containsFilter(IntentFilter intentFilter) {
        int size = size();
        for (int i = 0; i < size; i++) {
            if (IntentResolver.filterEquals(get(i), intentFilter)) {
                return true;
            }
        }
        return false;
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        this.app.writeToProto(protoOutputStream, 1146756268033L);
        protoOutputStream.write(1120986464258L, this.pid);
        protoOutputStream.write(1120986464259L, this.uid);
        protoOutputStream.write(1120986464260L, this.userId);
        if (this.curBroadcast != null) {
            this.curBroadcast.writeToProto(protoOutputStream, 1146756268037L);
        }
        protoOutputStream.write(1133871366150L, this.linkedToDeath);
        int size = size();
        for (int i = 0; i < size; i++) {
            get(i).writeToProto(protoOutputStream, 2246267895815L);
        }
        protoOutputStream.write(1138166333448L, Integer.toHexString(System.identityHashCode(this)));
        protoOutputStream.end(jStart);
    }

    void dumpLocal(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("app=");
        printWriter.print(this.app != null ? this.app.toShortString() : null);
        printWriter.print(" pid=");
        printWriter.print(this.pid);
        printWriter.print(" uid=");
        printWriter.print(this.uid);
        printWriter.print(" user=");
        printWriter.println(this.userId);
        if (this.curBroadcast != null || this.linkedToDeath) {
            printWriter.print(str);
            printWriter.print("curBroadcast=");
            printWriter.print(this.curBroadcast);
            printWriter.print(" linkedToDeath=");
            printWriter.println(this.linkedToDeath);
        }
    }

    void dump(PrintWriter printWriter, String str) {
        PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(printWriter);
        dumpLocal(printWriter, str);
        String str2 = str + "  ";
        int size = size();
        for (int i = 0; i < size; i++) {
            BroadcastFilter broadcastFilter = get(i);
            printWriter.print(str);
            printWriter.print("Filter #");
            printWriter.print(i);
            printWriter.print(": BroadcastFilter{");
            printWriter.print(Integer.toHexString(System.identityHashCode(broadcastFilter)));
            printWriter.println('}');
            broadcastFilter.dumpInReceiverList(printWriter, printWriterPrinter, str2);
        }
    }

    @Override
    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ReceiverList{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.pid);
        sb.append(' ');
        sb.append(this.app != null ? this.app.processName : "(unknown name)");
        sb.append('/');
        sb.append(this.uid);
        sb.append("/u");
        sb.append(this.userId);
        sb.append(this.receiver.asBinder() instanceof Binder ? " local:" : " remote:");
        sb.append(Integer.toHexString(System.identityHashCode(this.receiver.asBinder())));
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }
}
