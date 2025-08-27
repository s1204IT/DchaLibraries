package jp.co.benesse.dcha.dchaservice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import jp.co.benesse.dcha.dchaservice.util.Log;

/* loaded from: classes.dex */
public class UpdateLog {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss.SSS", Locale.JAPAN);

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [62=5] */
    public static synchronized void write() {
        String str;
        String str2;
        String str3;
        Log.d("UpdateLog", "write 0001");
        synchronized (DATE_FORMAT) {
            str = DATE_FORMAT.format(new Date());
        }
        File file = new File("/data/data/jp.co.benesse.dcha.dchaservice/update.log");
        FileWriter fileWriter = null;
        try {
            try {
                FileWriter fileWriter2 = new FileWriter(file, false);
                try {
                    fileWriter2.write(str);
                    Log.d("UpdateLog", "write 0002");
                    Log.d("UpdateLog", "write update.log");
                    Log.d("UpdateLog", "write 0004");
                } catch (Exception e) {
                    e = e;
                    fileWriter = fileWriter2;
                    Log.d("UpdateLog", "write 0003");
                    Log.e("UpdateLog", "write", e);
                    Log.d("UpdateLog", "write 0004");
                    if (fileWriter == null) {
                        file.setReadable(true, false);
                        Log.d("UpdateLog", "write 0008");
                    }
                    try {
                        Log.d("UpdateLog", "write 0005");
                        fileWriter.close();
                    } catch (IOException e2) {
                        e = e2;
                        Log.d("UpdateLog", "write 0006");
                        str2 = "UpdateLog";
                        str3 = "write";
                        Log.e(str2, str3, e);
                        file.setReadable(true, false);
                        Log.d("UpdateLog", "write 0008");
                    }
                    file.setReadable(true, false);
                    Log.d("UpdateLog", "write 0008");
                } catch (Throwable th) {
                    th = th;
                    fileWriter = fileWriter2;
                    Log.d("UpdateLog", "write 0004");
                    if (fileWriter != null) {
                        try {
                            Log.d("UpdateLog", "write 0005");
                            fileWriter.close();
                        } catch (IOException e3) {
                            Log.d("UpdateLog", "write 0006");
                            Log.e("UpdateLog", "write", e3);
                        }
                    }
                    throw th;
                }
                try {
                    Log.d("UpdateLog", "write 0005");
                    fileWriter2.close();
                } catch (IOException e4) {
                    e = e4;
                    Log.d("UpdateLog", "write 0006");
                    str2 = "UpdateLog";
                    str3 = "write";
                    Log.e(str2, str3, e);
                    file.setReadable(true, false);
                    Log.d("UpdateLog", "write 0008");
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (Exception e5) {
            e = e5;
        }
        try {
            file.setReadable(true, false);
        } catch (Exception e6) {
            Log.e("UpdateLog", "write 0007", e6);
        }
        Log.d("UpdateLog", "write 0008");
    }

    public static synchronized boolean exists() {
        boolean zExists;
        Log.d("UpdateLog", "exists 0001");
        zExists = new File("/data/data/jp.co.benesse.dcha.dchaservice/update.log").exists();
        if (zExists) {
            Log.d("UpdateLog", "exists 0002");
            Log.d("UpdateLog", "exists true");
        } else {
            Log.d("UpdateLog", "exists 0003");
            Log.d("UpdateLog", "exists false");
        }
        Log.d("UpdateLog", "exists 0004");
        return zExists;
    }
}
