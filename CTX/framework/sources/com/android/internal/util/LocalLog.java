package com.android.internal.util;

import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

public class LocalLog {
    private final String mTag;
    private final int mMaxLines = 20;
    private final ArrayList<String> mLines = new ArrayList<>(20);

    public LocalLog(String str) {
        this.mTag = str;
    }

    public void w(String str) {
        synchronized (this.mLines) {
            Slog.w(this.mTag, str);
            if (this.mLines.size() >= 20) {
                this.mLines.remove(0);
            }
            this.mLines.add(str);
        }
    }

    public boolean dump(PrintWriter printWriter, String str, String str2) {
        synchronized (this.mLines) {
            if (this.mLines.size() <= 0) {
                return false;
            }
            if (str != null) {
                printWriter.println(str);
            }
            for (int i = 0; i < this.mLines.size(); i++) {
                if (str2 != null) {
                    printWriter.print(str2);
                }
                printWriter.println(this.mLines.get(i));
            }
            return true;
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        synchronized (this.mLines) {
            for (int i = 0; i < this.mLines.size(); i++) {
                protoOutputStream.write(2237677961217L, this.mLines.get(i));
            }
        }
        protoOutputStream.end(jStart);
    }
}
