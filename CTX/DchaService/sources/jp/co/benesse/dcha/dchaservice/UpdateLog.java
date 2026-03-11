package jp.co.benesse.dcha.dchaservice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import jp.co.benesse.dcha.dchaservice.util.Log;

public class UpdateLog {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss.SSS", Locale.JAPAN);

    public static synchronized void write() {
        String str;
        String str2;
        String str3;
        FileWriter fileWriter;
        Log.d("UpdateLog", "write 0001");
        synchronized (DATE_FORMAT) {
            str = DATE_FORMAT.format(new Date());
        }
        File file = new File("/factory/log/update.log");
        FileWriter fileWriter2 = null;
        try {
            try {
                fileWriter = new FileWriter(file, false);
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            fileWriter.write(str);
            Log.d("UpdateLog", "write 0002");
            Log.d("UpdateLog", "write update.log");
            Log.d("UpdateLog", "write 0004");
            try {
                Log.d("UpdateLog", "write 0005");
                fileWriter.close();
            } catch (IOException e2) {
                e = e2;
                Log.d("UpdateLog", "write 0006");
                str2 = "UpdateLog";
                str3 = "write";
                Log.e(str2, str3, e);
            }
        } catch (Exception e3) {
            e = e3;
            fileWriter2 = fileWriter;
            Log.d("UpdateLog", "write 0003");
            Log.e("UpdateLog", "write", e);
            Log.d("UpdateLog", "write 0004");
            if (fileWriter2 != null) {
                try {
                    Log.d("UpdateLog", "write 0005");
                    fileWriter2.close();
                } catch (IOException e4) {
                    e = e4;
                    Log.d("UpdateLog", "write 0006");
                    str2 = "UpdateLog";
                    str3 = "write";
                    Log.e(str2, str3, e);
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fileWriter2 = fileWriter;
            Log.d("UpdateLog", "write 0004");
            if (fileWriter2 != null) {
                try {
                    Log.d("UpdateLog", "write 0005");
                    fileWriter2.close();
                } catch (IOException e5) {
                    Log.d("UpdateLog", "write 0006");
                    Log.e("UpdateLog", "write", e5);
                }
            }
            throw th;
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
        zExists = new File("/factory/log/update.log").exists();
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
