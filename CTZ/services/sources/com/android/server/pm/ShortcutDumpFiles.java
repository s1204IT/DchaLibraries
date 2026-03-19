package com.android.server.pm;

import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;

public class ShortcutDumpFiles {
    private static final boolean DEBUG = false;
    private static final String TAG = "ShortcutService";
    private final ShortcutService mService;

    public ShortcutDumpFiles(ShortcutService shortcutService) {
        this.mService = shortcutService;
    }

    public boolean save(String str, Consumer<PrintWriter> consumer) {
        try {
            File dumpPath = this.mService.getDumpPath();
            dumpPath.mkdirs();
            if (!dumpPath.exists()) {
                Slog.e(TAG, "Failed to create directory: " + dumpPath);
                return false;
            }
            PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(new File(dumpPath, str))));
            try {
                consumer.accept(printWriter);
                return true;
            } finally {
                $closeResource(null, printWriter);
            }
        } catch (IOException | RuntimeException e) {
            Slog.w(TAG, "Failed to create dump file: " + str, e);
            return false;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public boolean save(String str, final byte[] bArr) {
        return save(str, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((PrintWriter) obj).println(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bArr)).toString());
            }
        });
    }

    public void dumpAll(PrintWriter printWriter) {
        try {
            File dumpPath = this.mService.getDumpPath();
            File[] fileArrListFiles = dumpPath.listFiles(new FileFilter() {
                @Override
                public final boolean accept(File file) {
                    return file.isFile();
                }
            });
            if (dumpPath.exists() && !ArrayUtils.isEmpty(fileArrListFiles)) {
                Arrays.sort(fileArrListFiles, Comparator.comparing(new Function() {
                    @Override
                    public final Object apply(Object obj) {
                        return ((File) obj).getName();
                    }
                }));
                for (File file : fileArrListFiles) {
                    printWriter.print("*** Dumping: ");
                    printWriter.println(file.getName());
                    printWriter.print("mtime: ");
                    printWriter.println(ShortcutService.formatTime(file.lastModified()));
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    Throwable th = null;
                    while (true) {
                        try {
                            try {
                                String line = bufferedReader.readLine();
                                if (line == null) {
                                    break;
                                } else {
                                    printWriter.println(line);
                                }
                            } finally {
                                $closeResource(th, bufferedReader);
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            throw th;
                        }
                    }
                }
                return;
            }
            printWriter.print("  No dump files found.");
        } catch (IOException | RuntimeException e) {
            Slog.w(TAG, "Failed to print dump files", e);
        }
    }
}
