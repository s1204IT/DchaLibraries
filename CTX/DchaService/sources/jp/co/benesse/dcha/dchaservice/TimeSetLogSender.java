package jp.co.benesse.dcha.dchaservice;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import jp.co.benesse.dcha.dchaservice.util.Log;

public class TimeSetLogSender {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss.SSS", Locale.JAPAN);

    public static synchronized void send(Context context) {
        String str;
        FileLock fileLockTryLock;
        RandomAccessFile randomAccessFile;
        String str2;
        String str3;
        Log.d("TimeSetLogSender", "send 0001");
        synchronized (DATE_FORMAT) {
            str = DATE_FORMAT.format(new Date());
        }
        FileLock fileLock = null;
        try {
            File file = new File("/factory/log/TimeSetLog.log");
            if (file.exists()) {
                Log.d("TimeSetLogSender", "send 0002");
                randomAccessFile = new RandomAccessFile(file, "rw");
                try {
                    try {
                        fileLockTryLock = randomAccessFile.getChannel().tryLock();
                        if (fileLockTryLock != null) {
                            try {
                                Log.d("TimeSetLogSender", "send 0003");
                                int i = 0;
                                while (randomAccessFile.readLine() != null) {
                                    i++;
                                }
                                randomAccessFile.seek(0L);
                                int i2 = 100 >= i ? 0 : i - 100;
                                StringBuffer stringBuffer = new StringBuffer();
                                int i3 = 0;
                                while (true) {
                                    String line = randomAccessFile.readLine();
                                    if (line == null) {
                                        break;
                                    }
                                    i3++;
                                    if (i3 > i2) {
                                        Log.d("TimeSetLogSender", "send 0004");
                                        stringBuffer.append(String.format("L%1$04d: ", Integer.valueOf(i3)));
                                        int length = line.length();
                                        if (255 < length) {
                                            Log.d("TimeSetLogSender", "send 0005");
                                            length = 255;
                                        }
                                        stringBuffer.append(line.substring(0, length));
                                        EmergencyLog.write(context, str, "ELK012", stringBuffer.toString(), false);
                                        stringBuffer.setLength(0);
                                    }
                                }
                            } catch (Exception e) {
                                e = e;
                                fileLock = fileLockTryLock;
                                Log.e("TimeSetLogSender", "send 0006", e);
                                Log.d("TimeSetLogSender", "send 0007");
                                if (fileLock != null) {
                                    try {
                                        Log.d("TimeSetLogSender", "send 0008");
                                        fileLock.release();
                                    } catch (IOException e2) {
                                        Log.e("TimeSetLogSender", "send 0009", e2);
                                    }
                                }
                                if (randomAccessFile != null) {
                                    try {
                                        Log.d("TimeSetLogSender", "send 0010");
                                        randomAccessFile.close();
                                    } catch (IOException e3) {
                                        e = e3;
                                        str2 = "TimeSetLogSender";
                                        str3 = "send 0011";
                                        Log.e(str2, str3, e);
                                    }
                                }
                            } catch (Throwable th) {
                                th = th;
                                Log.d("TimeSetLogSender", "send 0007");
                                if (fileLockTryLock != null) {
                                    try {
                                        Log.d("TimeSetLogSender", "send 0008");
                                        fileLockTryLock.release();
                                    } catch (IOException e4) {
                                        Log.e("TimeSetLogSender", "send 0009", e4);
                                    }
                                    if (randomAccessFile != null) {
                                        throw th;
                                    }
                                    try {
                                        Log.d("TimeSetLogSender", "send 0010");
                                        randomAccessFile.close();
                                        throw th;
                                    } catch (IOException e5) {
                                        Log.e("TimeSetLogSender", "send 0011", e5);
                                        throw th;
                                    }
                                }
                                if (randomAccessFile != null) {
                                }
                            }
                        }
                        fileLock = fileLockTryLock;
                    } catch (Exception e6) {
                        e = e6;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    fileLockTryLock = fileLock;
                }
            } else {
                randomAccessFile = null;
            }
            Log.d("TimeSetLogSender", "send 0007");
            if (fileLock != null) {
                try {
                    Log.d("TimeSetLogSender", "send 0008");
                    fileLock.release();
                } catch (IOException e7) {
                    Log.e("TimeSetLogSender", "send 0009", e7);
                }
            }
            if (randomAccessFile != null) {
                try {
                    Log.d("TimeSetLogSender", "send 0010");
                    randomAccessFile.close();
                } catch (IOException e8) {
                    e = e8;
                    str2 = "TimeSetLogSender";
                    str3 = "send 0011";
                    Log.e(str2, str3, e);
                }
            }
        } catch (Exception e9) {
            e = e9;
            randomAccessFile = null;
        } catch (Throwable th3) {
            th = th3;
            fileLockTryLock = null;
            randomAccessFile = null;
        }
    }
}
