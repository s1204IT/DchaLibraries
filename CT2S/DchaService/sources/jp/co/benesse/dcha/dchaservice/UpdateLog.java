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
        String time;
        FileWriter fw;
        Log.d("UpdateLog", "write 0001");
        synchronized (DATE_FORMAT) {
            time = DATE_FORMAT.format(new Date());
        }
        File file = new File("/data/data/jp.co.benesse.dcha.dchaservice/update.log");
        FileWriter fw2 = null;
        try {
            try {
                fw = new FileWriter(file, false);
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            fw.write(time);
            Log.d("UpdateLog", "write 0002");
            Log.d("UpdateLog", "write update.log");
            Log.d("UpdateLog", "write 0004");
            if (fw != null) {
                try {
                    Log.d("UpdateLog", "write 0005");
                    fw.close();
                } catch (IOException e2) {
                    Log.d("UpdateLog", "write 0006");
                    Log.e("UpdateLog", "write", e2);
                }
            }
        } catch (Exception e3) {
            e = e3;
            fw2 = fw;
            Log.d("UpdateLog", "write 0003");
            Log.e("UpdateLog", "write", e);
            Log.d("UpdateLog", "write 0004");
            if (fw2 != null) {
                try {
                    Log.d("UpdateLog", "write 0005");
                    fw2.close();
                } catch (IOException e4) {
                    Log.d("UpdateLog", "write 0006");
                    Log.e("UpdateLog", "write", e4);
                }
            }
        } catch (Throwable th2) {
            th = th2;
            fw2 = fw;
            Log.d("UpdateLog", "write 0004");
            if (fw2 != null) {
                try {
                    Log.d("UpdateLog", "write 0005");
                    fw2.close();
                } catch (IOException e5) {
                    Log.d("UpdateLog", "write 0006");
                    Log.e("UpdateLog", "write", e5);
                }
            }
            throw th;
        }
        Log.d("UpdateLog", "write 0007");
    }

    public static synchronized boolean exists() {
        boolean fileExists;
        Log.d("UpdateLog", "exists 0001");
        File file = new File("/data/data/jp.co.benesse.dcha.dchaservice/update.log");
        fileExists = file.exists();
        if (fileExists) {
            Log.d("UpdateLog", "exists 0002");
            Log.d("UpdateLog", "exists true");
        } else {
            Log.d("UpdateLog", "exists 0003");
            Log.d("UpdateLog", "exists false");
        }
        Log.d("UpdateLog", "exists 0004");
        return fileExists;
    }
}
