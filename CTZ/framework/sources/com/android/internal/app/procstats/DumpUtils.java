package com.android.internal.app.procstats;

import android.accounts.GrantCredentialsPermissionActivity;
import android.app.backup.FullBackup;
import android.content.Context;
import android.hardware.Camera;
import android.media.TtmlUtils;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.UserHandle;
import android.provider.CalendarContract;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.DumpHeapActivity;
import com.android.internal.content.NativeLibraryHelper;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class DumpUtils {
    public static final String[] ADJ_MEM_NAMES_CSV;
    static final int[] ADJ_MEM_PROTO_ENUMS;
    static final String[] ADJ_MEM_TAGS;
    public static final String[] ADJ_SCREEN_NAMES_CSV;
    static final int[] ADJ_SCREEN_PROTO_ENUMS;
    static final String[] ADJ_SCREEN_TAGS;
    static final String CSV_SEP = "\t";
    public static final String[] STATE_NAMES = new String[14];
    public static final String[] STATE_NAMES_CSV;
    static final int[] STATE_PROTO_ENUMS;
    static final String[] STATE_TAGS;

    static {
        STATE_NAMES[0] = "Persist";
        STATE_NAMES[1] = "Top";
        STATE_NAMES[2] = "ImpFg";
        STATE_NAMES[3] = "ImpBg";
        STATE_NAMES[4] = "Backup";
        STATE_NAMES[5] = "Service";
        STATE_NAMES[6] = "ServRst";
        STATE_NAMES[7] = "Receivr";
        STATE_NAMES[8] = "HeavyWt";
        STATE_NAMES[9] = "Home";
        STATE_NAMES[10] = "LastAct";
        STATE_NAMES[11] = "CchAct";
        STATE_NAMES[12] = "CchCAct";
        STATE_NAMES[13] = "CchEmty";
        STATE_NAMES_CSV = new String[14];
        STATE_NAMES_CSV[0] = "pers";
        STATE_NAMES_CSV[1] = "top";
        STATE_NAMES_CSV[2] = "impfg";
        STATE_NAMES_CSV[3] = "impbg";
        STATE_NAMES_CSV[4] = Context.BACKUP_SERVICE;
        STATE_NAMES_CSV[5] = "service";
        STATE_NAMES_CSV[6] = "service-rs";
        STATE_NAMES_CSV[7] = "receiver";
        STATE_NAMES_CSV[8] = "heavy";
        STATE_NAMES_CSV[9] = CalendarContract.CalendarCache.TIMEZONE_TYPE_HOME;
        STATE_NAMES_CSV[10] = "lastact";
        STATE_NAMES_CSV[11] = "cch-activity";
        STATE_NAMES_CSV[12] = "cch-aclient";
        STATE_NAMES_CSV[13] = "cch-empty";
        STATE_TAGS = new String[14];
        STATE_TAGS[0] = TtmlUtils.TAG_P;
        STATE_TAGS[1] = "t";
        STATE_TAGS[2] = FullBackup.FILES_TREE_TOKEN;
        STATE_TAGS[3] = "b";
        STATE_TAGS[4] = "u";
        STATE_TAGS[5] = "s";
        STATE_TAGS[6] = "x";
        STATE_TAGS[7] = FullBackup.ROOT_TREE_TOKEN;
        STATE_TAGS[8] = "w";
        STATE_TAGS[9] = "h";
        STATE_TAGS[10] = "l";
        STATE_TAGS[11] = FullBackup.APK_TREE_TOKEN;
        STATE_TAGS[12] = FullBackup.CACHE_TREE_TOKEN;
        STATE_TAGS[13] = "e";
        STATE_PROTO_ENUMS = new int[14];
        STATE_PROTO_ENUMS[0] = 1;
        STATE_PROTO_ENUMS[1] = 2;
        STATE_PROTO_ENUMS[2] = 3;
        STATE_PROTO_ENUMS[3] = 4;
        STATE_PROTO_ENUMS[4] = 5;
        STATE_PROTO_ENUMS[5] = 6;
        STATE_PROTO_ENUMS[6] = 7;
        STATE_PROTO_ENUMS[7] = 8;
        STATE_PROTO_ENUMS[8] = 9;
        STATE_PROTO_ENUMS[9] = 10;
        STATE_PROTO_ENUMS[10] = 11;
        STATE_PROTO_ENUMS[11] = 12;
        STATE_PROTO_ENUMS[12] = 13;
        STATE_PROTO_ENUMS[13] = 14;
        ADJ_SCREEN_NAMES_CSV = new String[]{"off", Camera.Parameters.FLASH_MODE_ON};
        ADJ_MEM_NAMES_CSV = new String[]{"norm", "mod", "low", "crit"};
        ADJ_SCREEN_TAGS = new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, WifiEnterpriseConfig.ENGINE_ENABLE};
        ADJ_SCREEN_PROTO_ENUMS = new int[]{1, 2};
        ADJ_MEM_TAGS = new String[]{"n", "m", "l", FullBackup.CACHE_TREE_TOKEN};
        ADJ_MEM_PROTO_ENUMS = new int[]{1, 2, 3, 4};
    }

    private DumpUtils() {
    }

    public static void printScreenLabel(PrintWriter printWriter, int i) {
        if (i != 4) {
            switch (i) {
                case -1:
                    printWriter.print("     ");
                    break;
                case 0:
                    printWriter.print("SOff/");
                    break;
                default:
                    printWriter.print("????/");
                    break;
            }
        }
        printWriter.print("SOn /");
    }

    public static void printScreenLabelCsv(PrintWriter printWriter, int i) {
        if (i != 4) {
            switch (i) {
                case -1:
                    break;
                case 0:
                    printWriter.print(ADJ_SCREEN_NAMES_CSV[0]);
                    break;
                default:
                    printWriter.print("???");
                    break;
            }
        }
        printWriter.print(ADJ_SCREEN_NAMES_CSV[1]);
    }

    public static void printMemLabel(PrintWriter printWriter, int i, char c) {
        switch (i) {
            case -1:
                printWriter.print("    ");
                if (c != 0) {
                    printWriter.print(' ');
                }
                break;
            case 0:
                printWriter.print("Norm");
                if (c != 0) {
                    printWriter.print(c);
                }
                break;
            case 1:
                printWriter.print("Mod ");
                if (c != 0) {
                    printWriter.print(c);
                }
                break;
            case 2:
                printWriter.print("Low ");
                if (c != 0) {
                    printWriter.print(c);
                }
                break;
            case 3:
                printWriter.print("Crit");
                if (c != 0) {
                    printWriter.print(c);
                }
                break;
            default:
                printWriter.print("????");
                if (c != 0) {
                    printWriter.print(c);
                }
                break;
        }
    }

    public static void printMemLabelCsv(PrintWriter printWriter, int i) {
        if (i >= 0) {
            if (i <= 3) {
                printWriter.print(ADJ_MEM_NAMES_CSV[i]);
            } else {
                printWriter.print("???");
            }
        }
    }

    public static void printPercent(PrintWriter printWriter, double d) {
        double d2 = d * 100.0d;
        if (d2 < 1.0d) {
            printWriter.print(String.format("%.2f", Double.valueOf(d2)));
        } else if (d2 < 10.0d) {
            printWriter.print(String.format("%.1f", Double.valueOf(d2)));
        } else {
            printWriter.print(String.format("%.0f", Double.valueOf(d2)));
        }
        printWriter.print("%");
    }

    public static void printProcStateTag(PrintWriter printWriter, int i) {
        printArrayEntry(printWriter, STATE_TAGS, printArrayEntry(printWriter, ADJ_MEM_TAGS, printArrayEntry(printWriter, ADJ_SCREEN_TAGS, i, 56), 14), 1);
    }

    public static void printProcStateTagProto(ProtoOutputStream protoOutputStream, long j, long j2, long j3, int i) {
        printProto(protoOutputStream, j3, STATE_PROTO_ENUMS, printProto(protoOutputStream, j2, ADJ_MEM_PROTO_ENUMS, printProto(protoOutputStream, j, ADJ_SCREEN_PROTO_ENUMS, i, 56), 14), 1);
    }

    public static void printAdjTag(PrintWriter printWriter, int i) {
        printArrayEntry(printWriter, ADJ_MEM_TAGS, printArrayEntry(printWriter, ADJ_SCREEN_TAGS, i, 4), 1);
    }

    public static void printProcStateTagAndValue(PrintWriter printWriter, int i, long j) {
        printWriter.print(',');
        printProcStateTag(printWriter, i);
        printWriter.print(':');
        printWriter.print(j);
    }

    public static void printAdjTagAndValue(PrintWriter printWriter, int i, long j) {
        printWriter.print(',');
        printAdjTag(printWriter, i);
        printWriter.print(':');
        printWriter.print(j);
    }

    public static long dumpSingleTime(PrintWriter printWriter, String str, long[] jArr, int i, long j, long j2) {
        int i2 = 0;
        long j3 = 0;
        int i3 = -1;
        while (i2 < 8) {
            int i4 = i3;
            int i5 = -1;
            long j4 = j3;
            int i6 = 0;
            while (i6 < 4) {
                int i7 = i6 + i2;
                long j5 = jArr[i7];
                String str2 = "";
                if (i == i7) {
                    j5 += j2 - j;
                    if (printWriter != null) {
                        str2 = " (running)";
                    }
                }
                if (j5 != 0) {
                    if (printWriter != null) {
                        printWriter.print(str);
                        printScreenLabel(printWriter, i4 != i2 ? i2 : -1);
                        printMemLabel(printWriter, i5 != i6 ? i6 : -1, (char) 0);
                        printWriter.print(": ");
                        TimeUtils.formatDuration(j5, printWriter);
                        printWriter.println(str2);
                        i4 = i2;
                        i5 = i6;
                    }
                    j4 += j5;
                }
                i6++;
            }
            i2 += 4;
            j3 = j4;
            i3 = i4;
        }
        if (j3 != 0 && printWriter != null) {
            printWriter.print(str);
            printWriter.print("    TOTAL: ");
            TimeUtils.formatDuration(j3, printWriter);
            printWriter.println();
        }
        return j3;
    }

    public static void dumpAdjTimesCheckin(PrintWriter printWriter, String str, long[] jArr, int i, long j, long j2) {
        for (int i2 = 0; i2 < 8; i2 += 4) {
            for (int i3 = 0; i3 < 4; i3++) {
                int i4 = i3 + i2;
                long j3 = jArr[i4];
                if (i == i4) {
                    j3 += j2 - j;
                }
                if (j3 != 0) {
                    printAdjTagAndValue(printWriter, i4, j3);
                }
            }
        }
    }

    private static void dumpStateHeadersCsv(PrintWriter printWriter, String str, int[] iArr, int[] iArr2, int[] iArr3) {
        boolean z;
        int length = iArr != null ? iArr.length : 1;
        int length2 = iArr2 != null ? iArr2.length : 1;
        int length3 = iArr3 != null ? iArr3.length : 1;
        for (int i = 0; i < length; i++) {
            for (int i2 = 0; i2 < length2; i2++) {
                for (int i3 = 0; i3 < length3; i3++) {
                    printWriter.print(str);
                    if (iArr == null || iArr.length <= 1) {
                        z = false;
                    } else {
                        printScreenLabelCsv(printWriter, iArr[i]);
                        z = true;
                    }
                    if (iArr2 != null && iArr2.length > 1) {
                        if (z) {
                            printWriter.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                        }
                        printMemLabelCsv(printWriter, iArr2[i2]);
                        z = true;
                    }
                    if (iArr3 != null && iArr3.length > 1) {
                        if (z) {
                            printWriter.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                        }
                        printWriter.print(STATE_NAMES_CSV[iArr3[i3]]);
                    }
                }
            }
        }
    }

    public static void dumpProcessSummaryLocked(PrintWriter printWriter, String str, ArrayList<ProcessState> arrayList, int[] iArr, int[] iArr2, int[] iArr3, long j, long j2) {
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            arrayList.get(size).dumpSummary(printWriter, str, iArr, iArr2, iArr3, j, j2);
        }
    }

    public static void dumpProcessListCsv(PrintWriter printWriter, ArrayList<ProcessState> arrayList, boolean z, int[] iArr, boolean z2, int[] iArr2, boolean z3, int[] iArr3, long j) {
        printWriter.print(DumpHeapActivity.KEY_PROCESS);
        printWriter.print(CSV_SEP);
        printWriter.print(GrantCredentialsPermissionActivity.EXTRAS_REQUESTING_UID);
        printWriter.print(CSV_SEP);
        printWriter.print("vers");
        dumpStateHeadersCsv(printWriter, CSV_SEP, z ? iArr : null, z2 ? iArr2 : null, z3 ? iArr3 : null);
        printWriter.println();
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            ProcessState processState = arrayList.get(size);
            printWriter.print(processState.getName());
            printWriter.print(CSV_SEP);
            UserHandle.formatUid(printWriter, processState.getUid());
            printWriter.print(CSV_SEP);
            printWriter.print(processState.getVersion());
            processState.dumpCsv(printWriter, z, iArr, z2, iArr2, z3, iArr3, j);
            printWriter.println();
        }
    }

    public static int printArrayEntry(PrintWriter printWriter, String[] strArr, int i, int i2) {
        int i3 = i / i2;
        if (i3 >= 0 && i3 < strArr.length) {
            printWriter.print(strArr[i3]);
        } else {
            printWriter.print('?');
        }
        return i - (i3 * i2);
    }

    public static int printProto(ProtoOutputStream protoOutputStream, long j, int[] iArr, int i, int i2) {
        int i3 = i / i2;
        if (i3 >= 0 && i3 < iArr.length) {
            protoOutputStream.write(j, iArr[i3]);
        }
        return i - (i3 * i2);
    }

    public static String collapseString(String str, String str2) {
        if (str2.startsWith(str)) {
            int length = str2.length();
            int length2 = str.length();
            if (length == length2) {
                return "";
            }
            if (length >= length2 && str2.charAt(length2) == '.') {
                return str2.substring(length2);
            }
        }
        return str2;
    }
}
