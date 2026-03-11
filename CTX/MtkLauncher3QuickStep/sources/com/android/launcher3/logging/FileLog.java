package com.android.launcher3.logging;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.Utilities;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class FileLog {
    private static final String FILE_NAME_PREFIX = "log-";
    private static final long MAX_LOG_FILE_SIZE = 4194304;
    protected static final boolean ENABLED = Utilities.IS_DEBUG_DEVICE;
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(3, 3);
    private static Handler sHandler = null;
    private static File sLogsDirectory = null;

    public static void setDir(File file) {
        if (ENABLED) {
            synchronized (DATE_FORMAT) {
                if (sHandler != null && !file.equals(sLogsDirectory)) {
                    ((HandlerThread) sHandler.getLooper().getThread()).quit();
                    sHandler = null;
                }
            }
        }
        sLogsDirectory = file;
    }

    public static void d(String str, String str2, Exception exc) {
        Log.d(str, str2, exc);
        print(str, str2, exc);
    }

    public static void d(String str, String str2) {
        Log.d(str, str2);
        print(str, str2);
    }

    public static void e(String str, String str2, Exception exc) {
        Log.e(str, str2, exc);
        print(str, str2, exc);
    }

    public static void e(String str, String str2) {
        Log.e(str, str2);
        print(str, str2);
    }

    public static void print(String str, String str2) {
        print(str, str2, null);
    }

    public static void print(String str, String str2, Exception exc) {
        if (!ENABLED) {
            return;
        }
        String str3 = String.format("%s %s %s", DATE_FORMAT.format(new Date()), str, str2);
        if (exc != null) {
            str3 = str3 + "\n" + Log.getStackTraceString(exc);
        }
        Message.obtain(getHandler(), 1, str3).sendToTarget();
    }

    private static Handler getHandler() {
        synchronized (DATE_FORMAT) {
            if (sHandler == null) {
                HandlerThread handlerThread = new HandlerThread("file-logger");
                handlerThread.start();
                sHandler = new Handler(handlerThread.getLooper(), new LogWriterCallback());
            }
        }
        return sHandler;
    }

    public static void flushAll(PrintWriter printWriter) throws InterruptedException {
        if (!ENABLED) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Message.obtain(getHandler(), 3, Pair.create(printWriter, countDownLatch)).sendToTarget();
        countDownLatch.await(2L, TimeUnit.SECONDS);
    }

    private static class LogWriterCallback implements Handler.Callback {
        private static final long CLOSE_DELAY = 5000;
        private static final int MSG_CLOSE = 2;
        private static final int MSG_FLUSH = 3;
        private static final int MSG_WRITE = 1;
        private String mCurrentFileName;
        private PrintWriter mCurrentWriter;

        private LogWriterCallback() {
            this.mCurrentFileName = null;
            this.mCurrentWriter = null;
        }

        private void closeWriter() {
            Utilities.closeSilently(this.mCurrentWriter);
            this.mCurrentWriter = null;
        }

        @Override
        public boolean handleMessage(Message message) throws Throwable {
            if (FileLog.sLogsDirectory == null || !FileLog.ENABLED) {
                return true;
            }
            switch (message.what) {
                case 1:
                    Calendar calendar = Calendar.getInstance();
                    String str = FileLog.FILE_NAME_PREFIX + (calendar.get(6) & 1);
                    if (!str.equals(this.mCurrentFileName)) {
                        closeWriter();
                    }
                    try {
                        if (this.mCurrentWriter == null) {
                            this.mCurrentFileName = str;
                            File file = new File(FileLog.sLogsDirectory, str);
                            boolean z = false;
                            if (file.exists()) {
                                Calendar calendar2 = Calendar.getInstance();
                                calendar2.setTimeInMillis(file.lastModified());
                                calendar2.add(10, 36);
                                if (calendar.before(calendar2) && file.length() < FileLog.MAX_LOG_FILE_SIZE) {
                                    z = true;
                                }
                            }
                            this.mCurrentWriter = new PrintWriter(new FileWriter(file, z));
                        }
                        this.mCurrentWriter.println((String) message.obj);
                        this.mCurrentWriter.flush();
                        FileLog.sHandler.removeMessages(2);
                        FileLog.sHandler.sendEmptyMessageDelayed(2, CLOSE_DELAY);
                    } catch (Exception e) {
                        Log.e("FileLog", "Error writing logs to file", e);
                        closeWriter();
                    }
                    break;
                case 2:
                    closeWriter();
                    break;
                case 3:
                    closeWriter();
                    Pair pair = (Pair) message.obj;
                    if (pair.first != null) {
                        FileLog.dumpFile((PrintWriter) pair.first, "log-0");
                        FileLog.dumpFile((PrintWriter) pair.first, "log-1");
                    }
                    ((CountDownLatch) pair.second).countDown();
                    break;
            }
            return true;
            return true;
        }
    }

    public static void dumpFile(PrintWriter printWriter, String str) throws Throwable {
        BufferedReader bufferedReader;
        File file = new File(sLogsDirectory, str);
        if (file.exists()) {
            try {
                bufferedReader = new BufferedReader(new FileReader(file));
                try {
                    printWriter.println();
                    printWriter.println("--- logfile: " + str + " ---");
                    while (true) {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        } else {
                            printWriter.println(line);
                        }
                    }
                } catch (Exception e) {
                } catch (Throwable th) {
                    th = th;
                    Utilities.closeSilently(bufferedReader);
                    throw th;
                }
            } catch (Exception e2) {
                bufferedReader = null;
            } catch (Throwable th2) {
                th = th2;
                bufferedReader = null;
            }
            Utilities.closeSilently(bufferedReader);
        }
    }
}
