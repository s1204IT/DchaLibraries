package jp.co.benesse.dcha.dchaservice;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import jp.co.benesse.dcha.dchaservice.util.Log;

public class TimeSetLogSender {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss.SSS", Locale.JAPAN);

    public static synchronized void send(Context context) {
        String time;
        Log.d("TimeSetLogSender", "send 0001");
        synchronized (DATE_FORMAT) {
            time = DATE_FORMAT.format(new Date());
        }
        RandomAccessFile reader = null;
        FileLock lock = null;
        try {
            try {
                File file = new File("/data/data/jp.co.benesse.dcha.dchaservice/TimeSetLog.log");
                if (file.exists()) {
                    Log.d("TimeSetLogSender", "send 0002");
                    RandomAccessFile reader2 = new RandomAccessFile(file, "rw");
                    try {
                        FileChannel channel = reader2.getChannel();
                        lock = channel.tryLock();
                        if (lock != null) {
                            Log.d("TimeSetLogSender", "send 0003");
                            int mark = 0;
                            while (reader2.readLine() != null) {
                                mark++;
                            }
                            reader2.seek(0L);
                            int mark2 = 100 >= mark ? 0 : mark - 100;
                            int lines = 0;
                            StringBuffer sb = new StringBuffer();
                            while (true) {
                                String line = reader2.readLine();
                                if (line == null) {
                                    break;
                                }
                                lines++;
                                if (lines > mark2) {
                                    Log.d("TimeSetLogSender", "send 0004");
                                    sb.append(String.format("L%1$04d: ", Integer.valueOf(lines)));
                                    int length = line.length();
                                    if (255 < length) {
                                        Log.d("TimeSetLogSender", "send 0005");
                                        length = 255;
                                    }
                                    sb.append(line.substring(0, length));
                                    EmergencyLog.write(context, time, "ELK012", sb.toString(), false);
                                    sb.setLength(0);
                                }
                            }
                        }
                        reader = reader2;
                    } catch (Exception e) {
                        e = e;
                        reader = reader2;
                        Log.e("TimeSetLogSender", "send 0006", e);
                        Log.d("TimeSetLogSender", "send 0007");
                        if (lock != null) {
                            try {
                                Log.d("TimeSetLogSender", "send 0008");
                                lock.release();
                            } catch (IOException e2) {
                                Log.e("TimeSetLogSender", "send 0009", e2);
                            }
                        }
                        if (reader != null) {
                            try {
                                Log.d("TimeSetLogSender", "send 0010");
                                reader.close();
                            } catch (IOException e3) {
                                Log.e("TimeSetLogSender", "send 0011", e3);
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        reader = reader2;
                        Log.d("TimeSetLogSender", "send 0007");
                        if (lock != null) {
                            try {
                                Log.d("TimeSetLogSender", "send 0008");
                                lock.release();
                            } catch (IOException e4) {
                                Log.e("TimeSetLogSender", "send 0009", e4);
                            }
                            if (reader != null) {
                                throw th;
                            }
                            try {
                                Log.d("TimeSetLogSender", "send 0010");
                                reader.close();
                                throw th;
                            } catch (IOException e5) {
                                Log.e("TimeSetLogSender", "send 0011", e5);
                                throw th;
                            }
                        }
                        if (reader != null) {
                        }
                    }
                }
                Log.d("TimeSetLogSender", "send 0007");
                if (lock != null) {
                    try {
                        Log.d("TimeSetLogSender", "send 0008");
                        lock.release();
                    } catch (IOException e6) {
                        Log.e("TimeSetLogSender", "send 0009", e6);
                    }
                }
                if (reader != null) {
                    try {
                        Log.d("TimeSetLogSender", "send 0010");
                        reader.close();
                    } catch (IOException e7) {
                        Log.e("TimeSetLogSender", "send 0011", e7);
                    }
                }
            } catch (Exception e8) {
                e = e8;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }
}
