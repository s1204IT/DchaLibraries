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

public class EmergencyLog {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss.SSS", Locale.JAPAN);

    public static synchronized void write(Context context, String kind, String logMessages) {
        String time;
        Log.d("EmergencyLog", "write 0001");
        Log.d("EmergencyLog", "write without time");
        synchronized (DATE_FORMAT) {
            time = DATE_FORMAT.format(new Date());
        }
        Log.d("EmergencyLog", "write 0002");
        write(context, time, kind, logMessages, true);
    }

    public static synchronized void write(Context context, String time, String kind, String logMessages) {
        Log.d("EmergencyLog", "write 0003");
        Log.d("EmergencyLog", "write with time");
        write(context, time, kind, logMessages, true);
    }

    public static synchronized void write(Context context, String time, String kind, String logMessages, boolean factory) {
        Log.d("EmergencyLog", "write 0004");
        Log.d("EmergencyLog", "write with time");
        StringBuffer sb = new StringBuffer(time);
        sb.append(" ");
        sb.append(kind);
        sb.append(" ");
        sb.append(logMessages);
        String log = sb.toString();
        Log.d("EmergencyLog", "write 0005");
        writeLog(log);
    }

    private static void writeLog(String logMessages) throws Throwable {
        FileWriter fw;
        Log.d("EmergencyLog", "writeLog 0001");
        File fileA = new File("/data/data/jp.co.benesse.dcha.dchaservice/jp.co.benesse.dcha.dchaservice_000.txt");
        File fileB = new File("/data/data/jp.co.benesse.dcha.dchaservice/jp.co.benesse.dcha.dchaservice_001.txt");
        if (fileA.exists()) {
            Log.d("EmergencyLog", "writeLog 0002");
            if (fileA.length() > 102400) {
                Log.d("EmergencyLog", "writeLog 0003");
                if (fileB.exists()) {
                    Log.d("EmergencyLog", "writeLog 0004");
                    fileB.delete();
                }
                Log.d("EmergencyLog", "writeLog 0005");
                Log.d("EmergencyLog", "change log");
                fileA.renameTo(fileB);
            }
        }
        Log.d("EmergencyLog", "writeLog 0006");
        FileWriter fw2 = null;
        try {
            try {
                fw = new FileWriter(fileA, true);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Exception e) {
            e = e;
        }
        try {
            fw.write(logMessages + System.getProperty("line.separator"));
            Log.d("EmergencyLog", "end writeLog");
            Log.d("EmergencyLog", "writeLog 0008");
            if (fw != null) {
                try {
                    Log.d("EmergencyLog", "writeLog 0009");
                    fw.close();
                } catch (IOException e2) {
                    Log.e("EmergencyLog", "writeLog 0010", e2);
                }
            }
            fw2 = fw;
        } catch (Exception e3) {
            e = e3;
            fw2 = fw;
            Log.e("EmergencyLog", "writeLog 0007", e);
            Log.d("EmergencyLog", "writeLog 0008");
            if (fw2 != null) {
                try {
                    Log.d("EmergencyLog", "writeLog 0009");
                    fw2.close();
                } catch (IOException e4) {
                    Log.e("EmergencyLog", "writeLog 0010", e4);
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fw2 = fw;
            Log.d("EmergencyLog", "writeLog 0008");
            if (fw2 != null) {
                try {
                    Log.d("EmergencyLog", "writeLog 0009");
                    fw2.close();
                } catch (IOException e5) {
                    Log.e("EmergencyLog", "writeLog 0010", e5);
                }
            }
            throw th;
        }
        Log.d("EmergencyLog", "writeLog 0011");
    }
}
