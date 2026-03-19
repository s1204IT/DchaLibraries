package com.android.traceur;

import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class AtraceUtils {
    private static final Runtime RUNTIME = Runtime.getRuntime();

    public static boolean atraceStart(String str, int i, boolean z) {
        String str2 = "atrace --async_start -c -b " + i + " " + (z ? "-a '*' " : "") + str;
        Log.v("Traceur", "Starting async atrace: " + str2);
        try {
            Process processExec = exec(str2);
            if (processExec.waitFor() != 0) {
                Log.e("Traceur", "atraceStart failed with: " + processExec.exitValue());
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void atraceStop() {
        Log.v("Traceur", "Stopping async atrace: atrace --async_stop > /dev/null");
        try {
            Process processExec = exec("atrace --async_stop > /dev/null");
            if (processExec.waitFor() != 0) {
                Log.e("Traceur", "atraceStop failed with: " + processExec.exitValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean atraceDump(File file) {
        String str = "atrace --async_stop -z -c -o " + file;
        Log.v("Traceur", "Dumping async atrace: " + str);
        try {
            Process processExec = exec(str);
            if (processExec.waitFor() != 0) {
                Log.e("Traceur", "atraceDump failed with: " + processExec.exitValue());
                return false;
            }
            Process processExec2 = exec("ps -AT");
            new Streamer("atraceDump:ps:stdout", processExec2.getInputStream(), new FileOutputStream(file, true));
            if (processExec2.waitFor() != 0) {
                Log.e("Traceur", "atraceDump:ps failed with: " + processExec2.exitValue());
                return false;
            }
            file.setReadable(true, false);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TreeMap<String, String> atraceListCategories() {
        Log.v("Traceur", "Listing tags: atrace --list_categories");
        try {
            Process processExec = exec("atrace --list_categories");
            new Logger("atraceListCat:stderr", processExec.getErrorStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processExec.getInputStream()));
            if (processExec.waitFor() != 0) {
                Log.e("Traceur", "atraceListCategories failed with: " + processExec.exitValue());
            }
            TreeMap<String, String> treeMap = new TreeMap<>();
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    String[] strArrSplit = line.trim().split(" - ", 2);
                    if (strArrSplit.length == 2) {
                        treeMap.put(strArrSplit[0], strArrSplit[1]);
                    }
                } else {
                    return treeMap;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearSavedTraces() {
        Log.v("Traceur", "Clearing trace directory: rm -f /data/local/traces/trace-*.ctrace");
        try {
            Process processExec = exec("rm -f /data/local/traces/trace-*.ctrace");
            if (processExec.waitFor() != 0) {
                Log.e("Traceur", "clearSavedTraces failed with: " + processExec.exitValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Process exec(String str) throws IOException {
        String[] strArr = {"sh", "-c", str};
        Log.v("Traceur", "exec: " + Arrays.toString(strArr));
        return RUNTIME.exec(strArr);
    }

    public static boolean isTracingOn() {
        List<String> allLines;
        boolean zEquals = "1".equals(SystemProperties.get("debug.atrace.user_initiated", ""));
        if (!zEquals) {
            return false;
        }
        try {
            Path path = Paths.get("/sys/kernel/debug/tracing/tracing_on", new String[0]);
            Path path2 = Paths.get("/sys/kernel/tracing/tracing_on", new String[0]);
            if (Files.isReadable(path)) {
                allLines = Files.readAllLines(path);
            } else {
                if (!Files.isReadable(path2)) {
                    return false;
                }
                allLines = Files.readAllLines(path2);
            }
            boolean z = !allLines.get(0).equals("0");
            if (!zEquals || !z) {
                return false;
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getOutputFilename() {
        return String.format("trace-%s-%s-%s.ctrace", Build.BOARD, Build.ID, new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(new Date()));
    }

    public static File getOutputFile(String str) {
        return new File("/data/local/traces/", str);
    }

    private static class Streamer {
        private boolean mDone;

        Streamer(final String str, final InputStream inputStream, final OutputStream outputStream) {
            new Thread(str) {
                @Override
                public void run() {
                    byte[] bArr = new byte[2048];
                    while (true) {
                        try {
                            try {
                                int i = inputStream.read(bArr);
                                if (i != -1) {
                                    outputStream.write(bArr, 0, i);
                                } else {
                                    try {
                                        break;
                                    } catch (IOException e) {
                                    }
                                }
                            } catch (IOException e2) {
                                Log.e("Traceur", "Error while streaming " + str);
                                try {
                                    outputStream.close();
                                } catch (IOException e3) {
                                }
                                synchronized (Streamer.this) {
                                    Streamer.this.mDone = true;
                                    Streamer.this.notify();
                                }
                            }
                        } catch (Throwable th) {
                            try {
                                outputStream.close();
                            } catch (IOException e4) {
                            }
                            synchronized (Streamer.this) {
                                Streamer.this.mDone = true;
                                Streamer.this.notify();
                                throw th;
                            }
                        }
                    }
                    outputStream.close();
                    synchronized (Streamer.this) {
                        Streamer.this.mDone = true;
                        Streamer.this.notify();
                    }
                }
            }.start();
        }
    }

    private static class Logger {
        Logger(final String str, final InputStream inputStream) {
            new Thread(str) {
                @Override
                public void run() {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    while (true) {
                        try {
                            try {
                                try {
                                    String line = bufferedReader.readLine();
                                    if (line == null) {
                                        break;
                                    }
                                    Log.e("Traceur", str + ": " + line);
                                } catch (IOException e) {
                                    Log.e("Traceur", "Error while streaming " + str);
                                    bufferedReader.close();
                                }
                            } catch (Throwable th) {
                                try {
                                    bufferedReader.close();
                                } catch (IOException e2) {
                                }
                                throw th;
                            }
                        } catch (IOException e3) {
                            return;
                        }
                    }
                    bufferedReader.close();
                }
            }.start();
        }
    }
}
