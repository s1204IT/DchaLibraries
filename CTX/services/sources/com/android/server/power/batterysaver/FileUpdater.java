package com.android.server.power.batterysaver;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.IoThread;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class FileUpdater {
    private static final boolean DEBUG = false;
    private static final String PROP_SKIP_WRITE = "debug.batterysaver.no_write_files";
    private static final String TAG = "BatterySaverController";
    private static final String TAG_DEFAULT_ROOT = "defaults";
    private final int MAX_RETRIES;
    private final long RETRY_INTERVAL_MS;
    private final Context mContext;

    @GuardedBy("mLock")
    private final ArrayMap<String, String> mDefaultValues;
    private Runnable mHandleWriteOnHandlerRunnable;
    private final Handler mHandler;
    private final Object mLock;

    @GuardedBy("mLock")
    private final ArrayMap<String, String> mPendingWrites;

    @GuardedBy("mLock")
    private int mRetries;

    public FileUpdater(Context context) {
        this(context, IoThread.get().getLooper(), 10, 5000);
    }

    @VisibleForTesting
    FileUpdater(Context context, Looper looper, int i, int i2) {
        this.mLock = new Object();
        this.mPendingWrites = new ArrayMap<>();
        this.mDefaultValues = new ArrayMap<>();
        this.mRetries = 0;
        this.mHandleWriteOnHandlerRunnable = new Runnable() {
            @Override
            public final void run() throws Exception {
                this.f$0.handleWriteOnHandler();
            }
        };
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.MAX_RETRIES = i;
        this.RETRY_INTERVAL_MS = i2;
    }

    public void systemReady(boolean z) {
        synchronized (this.mLock) {
            try {
                if (z) {
                    if (loadDefaultValuesLocked()) {
                        Slog.d(TAG, "Default values loaded after runtime restart; writing them...");
                        restoreDefault();
                    }
                } else {
                    injectDefaultValuesFilename().delete();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void writeFiles(ArrayMap<String, String> arrayMap) {
        synchronized (this.mLock) {
            for (int size = arrayMap.size() - 1; size >= 0; size--) {
                this.mPendingWrites.put(arrayMap.keyAt(size), arrayMap.valueAt(size));
            }
            this.mRetries = 0;
            this.mHandler.removeCallbacks(this.mHandleWriteOnHandlerRunnable);
            this.mHandler.post(this.mHandleWriteOnHandlerRunnable);
        }
    }

    public void restoreDefault() {
        synchronized (this.mLock) {
            this.mPendingWrites.clear();
            writeFiles(this.mDefaultValues);
        }
    }

    private String getKeysString(Map<String, String> map) {
        return new ArrayList(map.keySet()).toString();
    }

    private ArrayMap<String, String> cloneMap(ArrayMap<String, String> arrayMap) {
        return new ArrayMap<>(arrayMap);
    }

    private void handleWriteOnHandler() throws Exception {
        synchronized (this.mLock) {
            if (this.mPendingWrites.size() == 0) {
                return;
            }
            ArrayMap<String, String> arrayMapCloneMap = cloneMap(this.mPendingWrites);
            int size = arrayMapCloneMap.size();
            boolean z = false;
            for (int i = 0; i < size; i++) {
                String strKeyAt = arrayMapCloneMap.keyAt(i);
                String strValueAt = arrayMapCloneMap.valueAt(i);
                if (ensureDefaultLoaded(strKeyAt)) {
                    try {
                        injectWriteToFile(strKeyAt, strValueAt);
                        removePendingWrite(strKeyAt);
                    } catch (IOException e) {
                        z = true;
                    }
                }
            }
            if (z) {
                scheduleRetry();
            }
        }
    }

    private void removePendingWrite(String str) {
        synchronized (this.mLock) {
            this.mPendingWrites.remove(str);
        }
    }

    private void scheduleRetry() {
        synchronized (this.mLock) {
            if (this.mPendingWrites.size() == 0) {
                return;
            }
            this.mRetries++;
            if (this.mRetries > this.MAX_RETRIES) {
                doWtf("Gave up writing files: " + getKeysString(this.mPendingWrites));
                return;
            }
            this.mHandler.removeCallbacks(this.mHandleWriteOnHandlerRunnable);
            this.mHandler.postDelayed(this.mHandleWriteOnHandlerRunnable, this.RETRY_INTERVAL_MS);
        }
    }

    private boolean ensureDefaultLoaded(String str) {
        synchronized (this.mLock) {
            if (this.mDefaultValues.containsKey(str)) {
                return true;
            }
            try {
                String strInjectReadFromFileTrimmed = injectReadFromFileTrimmed(str);
                synchronized (this.mLock) {
                    this.mDefaultValues.put(str, strInjectReadFromFileTrimmed);
                    saveDefaultValuesLocked();
                }
                return true;
            } catch (IOException e) {
                injectWtf("Unable to read from file", e);
                removePendingWrite(str);
                return false;
            }
        }
    }

    @VisibleForTesting
    String injectReadFromFileTrimmed(String str) throws IOException {
        return IoUtils.readFileAsString(str).trim();
    }

    @VisibleForTesting
    void injectWriteToFile(String str, String str2) throws Exception {
        if (injectShouldSkipWrite()) {
            Slog.i(TAG, "Skipped writing to '" + str + "'");
            return;
        }
        try {
            FileWriter fileWriter = new FileWriter(str);
            try {
                fileWriter.write(str2);
            } finally {
                $closeResource(null, fileWriter);
            }
        } catch (IOException | RuntimeException e) {
            Slog.w(TAG, "Failed writing '" + str2 + "' to '" + str + "': " + e.getMessage());
            throw e;
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

    @GuardedBy("mLock")
    private void saveDefaultValuesLocked() {
        FileOutputStream fileOutputStreamStartWrite;
        Throwable e;
        AtomicFile atomicFile = new AtomicFile(injectDefaultValuesFilename());
        try {
            atomicFile.getBaseFile().getParentFile().mkdirs();
            fileOutputStreamStartWrite = atomicFile.startWrite();
        } catch (IOException | RuntimeException | XmlPullParserException e2) {
            fileOutputStreamStartWrite = null;
            e = e2;
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_DEFAULT_ROOT);
            XmlUtils.writeMapXml(this.mDefaultValues, fastXmlSerializer, (XmlUtils.WriteMapCallback) null);
            fastXmlSerializer.endTag(null, TAG_DEFAULT_ROOT);
            fastXmlSerializer.endDocument();
            atomicFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException | RuntimeException | XmlPullParserException e3) {
            e = e3;
            Slog.e(TAG, "Failed to write to file " + atomicFile.getBaseFile(), e);
            atomicFile.failWrite(fileOutputStreamStartWrite);
        }
    }

    @GuardedBy("mLock")
    @VisibleForTesting
    boolean loadDefaultValuesLocked() {
        ArrayMap thisArrayMapXml;
        Throwable e;
        FileInputStream fileInputStreamOpenRead;
        Throwable th;
        ArrayMap arrayMap;
        AtomicFile atomicFile = new AtomicFile(injectDefaultValuesFilename());
        try {
            try {
                fileInputStreamOpenRead = atomicFile.openRead();
            } catch (IOException | RuntimeException | XmlPullParserException e2) {
                thisArrayMapXml = null;
                e = e2;
            }
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                thisArrayMapXml = null;
                while (true) {
                    try {
                        int next = xmlPullParserNewPullParser.next();
                        try {
                            if (next != 1) {
                                if (next == 2) {
                                    int depth = xmlPullParserNewPullParser.getDepth();
                                    String name = xmlPullParserNewPullParser.getName();
                                    if (depth != 1) {
                                        thisArrayMapXml = XmlUtils.readThisArrayMapXml(xmlPullParserNewPullParser, TAG_DEFAULT_ROOT, new String[1], (XmlUtils.ReadMapCallback) null);
                                    } else if (!TAG_DEFAULT_ROOT.equals(name)) {
                                        break;
                                    }
                                }
                            } else if (fileInputStreamOpenRead != null) {
                                $closeResource(null, fileInputStreamOpenRead);
                            }
                        } catch (IOException | RuntimeException | XmlPullParserException e3) {
                            e = e3;
                            Slog.e(TAG, "Failed to read file " + atomicFile.getBaseFile(), e);
                            if (thisArrayMapXml != null) {
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        arrayMap = thisArrayMapXml;
                        th = null;
                        if (fileInputStreamOpenRead != null) {
                        }
                        e = e;
                        thisArrayMapXml = arrayMap;
                    }
                    Slog.e(TAG, "Failed to read file " + atomicFile.getBaseFile(), e);
                }
            } catch (Throwable th3) {
                th = th3;
                thisArrayMapXml = null;
            }
        } catch (FileNotFoundException e4) {
            thisArrayMapXml = null;
        }
    }

    private void doWtf(String str) {
        injectWtf(str, null);
    }

    @VisibleForTesting
    void injectWtf(String str, Throwable th) {
        Slog.wtf(TAG, str, th);
    }

    File injectDefaultValuesFilename() {
        File file = new File(Environment.getDataSystemDirectory(), "battery-saver");
        file.mkdirs();
        return new File(file, "default-values.xml");
    }

    @VisibleForTesting
    boolean injectShouldSkipWrite() {
        return SystemProperties.getBoolean(PROP_SKIP_WRITE, false);
    }

    @VisibleForTesting
    ArrayMap<String, String> getDefaultValuesForTest() {
        return this.mDefaultValues;
    }
}
