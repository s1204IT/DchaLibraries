package jp.co.benesse.dcha.dchaservice;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import jp.co.benesse.dcha.dchaservice.util.Log;

/* loaded from: classes.dex */
public class EmergencyLog {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss.SSS", Locale.JAPAN);

    public static synchronized void write(Context context, String str, String str2) {
        String str3;
        Log.d("EmergencyLog", "write 0001");
        Log.d("EmergencyLog", "write without time");
        synchronized (DATE_FORMAT) {
            str3 = DATE_FORMAT.format(new Date());
        }
        Log.d("EmergencyLog", "write 0002");
        write(context, str3, str, str2, true);
    }

    public static synchronized void write(Context context, String str, String str2, String str3) {
        Log.d("EmergencyLog", "write 0003");
        Log.d("EmergencyLog", "write with time");
        write(context, str, str2, str3, true);
    }

    public static synchronized void write(Context context, String str, String str2, String str3, boolean z) {
        Log.d("EmergencyLog", "write 0004");
        Log.d("EmergencyLog", "write with time");
        StringBuffer stringBuffer = new StringBuffer(str);
        stringBuffer.append(" ");
        stringBuffer.append(str2);
        stringBuffer.append(" ");
        stringBuffer.append(str3);
        String string = stringBuffer.toString();
        Log.d("EmergencyLog", "write 0005");
        writeLog(string);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [149=5] */
    private static void writeLog(String str) throws Throwable {
        FileWriter fileWriter;
        Log.d("EmergencyLog", "writeLog 0001");
        File file = new File("/factory/log/jp.co.benesse.dcha.dchaservice_000.txt");
        File file2 = new File("/factory/log/jp.co.benesse.dcha.dchaservice_001.txt");
        if (file.exists()) {
            Log.d("EmergencyLog", "writeLog 0002");
            if (file.length() > 102400) {
                Log.d("EmergencyLog", "writeLog 0003");
                if (file2.exists()) {
                    Log.d("EmergencyLog", "writeLog 0004");
                    file2.delete();
                }
                Log.d("EmergencyLog", "writeLog 0005");
                Log.d("EmergencyLog", "change log");
                file.renameTo(file2);
            }
        }
        Log.d("EmergencyLog", "writeLog 0006");
        FileWriter fileWriter2 = null;
        try {
            try {
                try {
                    fileWriter = new FileWriter(file, true);
                    try {
                        fileWriter.write(str + System.getProperty("line.separator"));
                        Log.d("EmergencyLog", "end writeLog");
                        Log.d("EmergencyLog", "writeLog 0008");
                        Log.d("EmergencyLog", "writeLog 0009");
                        fileWriter.close();
                    } catch (Exception e) {
                        e = e;
                        fileWriter2 = fileWriter;
                        Log.e("EmergencyLog", "writeLog 0007", e);
                        Log.d("EmergencyLog", "writeLog 0008");
                        if (fileWriter2 != null) {
                            Log.d("EmergencyLog", "writeLog 0009");
                            fileWriter2.close();
                        }
                        new File("/factory/log/jp.co.benesse.dcha.dchaservice_000.txt").setReadable(true, false);
                        Log.d("EmergencyLog", "writeLog 0012");
                    } catch (Throwable th) {
                        th = th;
                        Log.d("EmergencyLog", "writeLog 0008");
                        if (fileWriter != null) {
                            try {
                                Log.d("EmergencyLog", "writeLog 0009");
                                fileWriter.close();
                            } catch (IOException e2) {
                                Log.e("EmergencyLog", "writeLog 0010", e2);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    fileWriter = fileWriter2;
                }
            } catch (Exception e3) {
                e = e3;
            }
        } catch (IOException e4) {
            Log.e("EmergencyLog", "writeLog 0010", e4);
        }
        try {
            new File("/factory/log/jp.co.benesse.dcha.dchaservice_000.txt").setReadable(true, false);
        } catch (Exception e5) {
            Log.e("EmergencyLog", "writeLog 0011", e5);
        }
        Log.d("EmergencyLog", "writeLog 0012");
    }
}
