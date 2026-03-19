package com.android.server.utils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public final class PriorityDump {
    public static final String PRIORITY_ARG = "--dump-priority";
    public static final String PRIORITY_ARG_CRITICAL = "CRITICAL";
    public static final String PRIORITY_ARG_HIGH = "HIGH";
    public static final String PRIORITY_ARG_NORMAL = "NORMAL";
    private static final int PRIORITY_TYPE_CRITICAL = 1;
    private static final int PRIORITY_TYPE_HIGH = 2;
    private static final int PRIORITY_TYPE_INVALID = 0;
    private static final int PRIORITY_TYPE_NORMAL = 3;
    public static final String PROTO_ARG = "--proto";

    @Retention(RetentionPolicy.SOURCE)
    private @interface PriorityType {
    }

    private PriorityDump() {
        throw new UnsupportedOperationException();
    }

    public static void dump(PriorityDumper priorityDumper, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        int i = 0;
        if (strArr == null) {
            priorityDumper.dump(fileDescriptor, printWriter, strArr, false);
        }
        String[] strArr2 = new String[strArr.length];
        int i2 = 0;
        int priorityType = 0;
        boolean z = false;
        while (i < strArr.length) {
            if (strArr[i].equals(PROTO_ARG)) {
                z = true;
            } else if (strArr[i].equals(PRIORITY_ARG)) {
                int i3 = i + 1;
                if (i3 < strArr.length) {
                    priorityType = getPriorityType(strArr[i3]);
                    i = i3;
                }
            } else {
                strArr2[i2] = strArr[i];
                i2++;
            }
            i++;
        }
        if (i2 < strArr.length) {
            strArr2 = (String[]) Arrays.copyOf(strArr2, i2);
        }
        switch (priorityType) {
            case 1:
                priorityDumper.dumpCritical(fileDescriptor, printWriter, strArr2, z);
                break;
            case 2:
                priorityDumper.dumpHigh(fileDescriptor, printWriter, strArr2, z);
                break;
            case 3:
                priorityDumper.dumpNormal(fileDescriptor, printWriter, strArr2, z);
                break;
            default:
                priorityDumper.dump(fileDescriptor, printWriter, strArr2, z);
                break;
        }
    }

    private static int getPriorityType(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != -1986416409) {
            if (iHashCode != -1560189025) {
                b = (iHashCode == 2217378 && str.equals(PRIORITY_ARG_HIGH)) ? (byte) 1 : (byte) -1;
            } else if (str.equals(PRIORITY_ARG_CRITICAL)) {
                b = 0;
            }
        } else if (str.equals(PRIORITY_ARG_NORMAL)) {
            b = 2;
        }
        switch (b) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 0;
        }
    }

    public interface PriorityDumper {
        default void dumpCritical(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
        }

        default void dumpHigh(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
        }

        default void dumpNormal(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
        }

        default void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, boolean z) {
            dumpCritical(fileDescriptor, printWriter, strArr, z);
            dumpHigh(fileDescriptor, printWriter, strArr, z);
            dumpNormal(fileDescriptor, printWriter, strArr, z);
        }
    }
}
