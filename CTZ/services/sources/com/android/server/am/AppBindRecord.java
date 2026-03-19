package com.android.server.am;

import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;

public final class AppBindRecord {
    final ProcessRecord client;
    final ArraySet<ConnectionRecord> connections = new ArraySet<>();
    final IntentBindRecord intent;
    final ServiceRecord service;

    void dump(PrintWriter printWriter, String str) {
        printWriter.println(str + "service=" + this.service);
        printWriter.println(str + "client=" + this.client);
        dumpInIntentBind(printWriter, str);
    }

    void dumpInIntentBind(PrintWriter printWriter, String str) {
        int size = this.connections.size();
        if (size > 0) {
            printWriter.println(str + "Per-process Connections:");
            for (int i = 0; i < size; i++) {
                printWriter.println(str + "  " + this.connections.valueAt(i));
            }
        }
    }

    AppBindRecord(ServiceRecord serviceRecord, IntentBindRecord intentBindRecord, ProcessRecord processRecord) {
        this.service = serviceRecord;
        this.intent = intentBindRecord;
        this.client = processRecord;
    }

    public String toString() {
        return "AppBindRecord{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.service.shortName + ":" + this.client.processName + "}";
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.service.shortName);
        protoOutputStream.write(1138166333442L, this.client.processName);
        int size = this.connections.size();
        for (int i = 0; i < size; i++) {
            protoOutputStream.write(2237677961219L, Integer.toHexString(System.identityHashCode(this.connections.valueAt(i))));
        }
        protoOutputStream.end(jStart);
    }
}
