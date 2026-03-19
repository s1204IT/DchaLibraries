package com.android.server;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.DropBoxManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.IDropBoxManagerService;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.ObjectUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import libcore.io.IoUtils;

public final class DropBoxManagerService extends SystemService {
    private static final int DEFAULT_AGE_SECONDS = 259200;
    private static final int DEFAULT_MAX_FILES = 1000;
    private static final int DEFAULT_MAX_FILES_LOWRAM = 300;
    private static final int DEFAULT_QUOTA_KB = 5120;
    private static final int DEFAULT_QUOTA_PERCENT = 10;
    private static final int DEFAULT_RESERVE_PERCENT = 10;
    private static final int MSG_SEND_BROADCAST = 1;
    private static final boolean PROFILE_DUMP = false;
    private static final int QUOTA_RESCAN_MILLIS = 5000;
    private static final String TAG = "DropBoxManagerService";
    private FileList mAllFiles;
    private int mBlockSize;
    private volatile boolean mBooted;
    private int mCachedQuotaBlocks;
    private long mCachedQuotaUptimeMillis;
    private final ContentResolver mContentResolver;
    private final File mDropBoxDir;
    private ArrayMap<String, FileList> mFilesByTag;
    private final Handler mHandler;
    private int mMaxFiles;
    private final BroadcastReceiver mReceiver;
    private StatFs mStatFs;
    private final IDropBoxManagerService.Stub mStub;

    public DropBoxManagerService(Context context) {
        this(context, new File("/data/system/dropbox"), FgThread.get().getLooper());
    }

    @VisibleForTesting
    public DropBoxManagerService(Context context, File file, Looper looper) {
        super(context);
        this.mAllFiles = null;
        this.mFilesByTag = null;
        this.mStatFs = null;
        this.mBlockSize = 0;
        this.mCachedQuotaBlocks = 0;
        this.mCachedQuotaUptimeMillis = 0L;
        this.mBooted = false;
        this.mMaxFiles = -1;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                DropBoxManagerService.this.mCachedQuotaUptimeMillis = 0L;
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            DropBoxManagerService.this.init();
                            DropBoxManagerService.this.trimToFit();
                        } catch (IOException e) {
                            Slog.e(DropBoxManagerService.TAG, "Can't init", e);
                        }
                    }
                }.start();
            }
        };
        this.mStub = new IDropBoxManagerService.Stub() {
            public void add(DropBoxManager.Entry entry) throws Throwable {
                DropBoxManagerService.this.add(entry);
            }

            public boolean isTagEnabled(String str) {
                return DropBoxManagerService.this.isTagEnabled(str);
            }

            public DropBoxManager.Entry getNextEntry(String str, long j) {
                return DropBoxManagerService.this.getNextEntry(str, j);
            }

            public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
                DropBoxManagerService.this.dump(fileDescriptor, printWriter, strArr);
            }
        };
        this.mDropBoxDir = file;
        this.mContentResolver = getContext().getContentResolver();
        this.mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    DropBoxManagerService.this.getContext().sendBroadcastAsUser((Intent) message.obj, UserHandle.SYSTEM, "android.permission.READ_LOGS");
                }
            }
        };
    }

    @Override
    public void onStart() {
        publishBinderService("dropbox", this.mStub);
    }

    @Override
    public void onBootPhase(int i) {
        if (i != 500) {
            if (i == 1000) {
                this.mBooted = true;
            }
        } else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
            getContext().registerReceiver(this.mReceiver, intentFilter);
            this.mContentResolver.registerContentObserver(Settings.Global.CONTENT_URI, true, new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean z) {
                    DropBoxManagerService.this.mReceiver.onReceive(DropBoxManagerService.this.getContext(), (Intent) null);
                }
            });
        }
    }

    public IDropBoxManagerService getServiceStub() {
        return this.mStub;
    }

    public void add(DropBoxManager.Entry entry) throws Throwable {
        InputStream inputStream;
        File file;
        int i;
        long jCreateEntry;
        String tag = entry.getTag();
        OutputStream gZIPOutputStream = null;
        try {
            try {
                int flags = entry.getFlags();
                if ((flags & 1) != 0) {
                    throw new IllegalArgumentException();
                }
                init();
                if (!isTagEnabled(tag)) {
                    IoUtils.closeQuietly((AutoCloseable) null);
                    IoUtils.closeQuietly((AutoCloseable) null);
                    entry.close();
                    return;
                }
                long jTrimToFit = trimToFit();
                long jCurrentTimeMillis = System.currentTimeMillis();
                byte[] bArr = new byte[this.mBlockSize];
                inputStream = entry.getInputStream();
                int i2 = 0;
                while (i2 < bArr.length) {
                    try {
                        try {
                            int i3 = inputStream.read(bArr, i2, bArr.length - i2);
                            if (i3 <= 0) {
                                break;
                            } else {
                                i2 += i3;
                            }
                        } catch (IOException e) {
                            e = e;
                            file = null;
                            Slog.e(TAG, "Can't write: " + tag, e);
                            IoUtils.closeQuietly(gZIPOutputStream);
                            IoUtils.closeQuietly(inputStream);
                            entry.close();
                            if (file != null) {
                            }
                        } catch (Throwable th) {
                            th = th;
                            file = null;
                            IoUtils.closeQuietly(gZIPOutputStream);
                            IoUtils.closeQuietly(inputStream);
                            entry.close();
                            if (file != null) {
                            }
                            throw th;
                        }
                    } catch (IOException e2) {
                        e = e2;
                        gZIPOutputStream = null;
                    } catch (Throwable th2) {
                        th = th2;
                        gZIPOutputStream = null;
                    }
                }
                file = new File(this.mDropBoxDir, "drop" + Thread.currentThread().getId() + ".tmp");
                try {
                    int i4 = this.mBlockSize;
                    if (i4 > 4096) {
                        i4 = 4096;
                    }
                    if (i4 < 512) {
                        i4 = 512;
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, i4);
                    try {
                        if (i2 == bArr.length && (flags & 4) == 0) {
                            gZIPOutputStream = new GZIPOutputStream(bufferedOutputStream);
                            i = flags | 4;
                        } else {
                            gZIPOutputStream = bufferedOutputStream;
                            i = flags;
                        }
                        while (true) {
                            try {
                                gZIPOutputStream.write(bArr, 0, i2);
                                long jCurrentTimeMillis2 = System.currentTimeMillis();
                                if (jCurrentTimeMillis2 - jCurrentTimeMillis > 30000) {
                                    jTrimToFit = trimToFit();
                                    jCurrentTimeMillis = jCurrentTimeMillis2;
                                }
                                i2 = inputStream.read(bArr);
                                if (i2 <= 0) {
                                    FileUtils.sync(fileOutputStream);
                                    gZIPOutputStream.close();
                                    gZIPOutputStream = null;
                                } else {
                                    gZIPOutputStream.flush();
                                }
                                if (file.length() > jTrimToFit) {
                                    Slog.w(TAG, "Dropping: " + tag + " (" + file.length() + " > " + jTrimToFit + " bytes)");
                                    file.delete();
                                    file = null;
                                    break;
                                }
                                if (i2 <= 0) {
                                    break;
                                }
                            } catch (IOException e3) {
                                e = e3;
                                Slog.e(TAG, "Can't write: " + tag, e);
                                IoUtils.closeQuietly(gZIPOutputStream);
                                IoUtils.closeQuietly(inputStream);
                                entry.close();
                                if (file != null) {
                                    file.delete();
                                    return;
                                }
                                return;
                            }
                        }
                        jCreateEntry = createEntry(file, tag, i);
                    } catch (IOException e4) {
                        e = e4;
                        gZIPOutputStream = bufferedOutputStream;
                    } catch (Throwable th3) {
                        th = th3;
                        gZIPOutputStream = bufferedOutputStream;
                        IoUtils.closeQuietly(gZIPOutputStream);
                        IoUtils.closeQuietly(inputStream);
                        entry.close();
                        if (file != null) {
                            file.delete();
                        }
                        throw th;
                    }
                } catch (IOException e5) {
                    e = e5;
                    gZIPOutputStream = null;
                } catch (Throwable th4) {
                    th = th4;
                    gZIPOutputStream = null;
                }
                try {
                    Intent intent = new Intent("android.intent.action.DROPBOX_ENTRY_ADDED");
                    intent.putExtra("tag", tag);
                    intent.putExtra("time", jCreateEntry);
                    if (!this.mBooted) {
                        intent.addFlags(1073741824);
                    }
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(1, intent));
                    IoUtils.closeQuietly(gZIPOutputStream);
                    IoUtils.closeQuietly(inputStream);
                    entry.close();
                } catch (IOException e6) {
                    e = e6;
                    file = null;
                    Slog.e(TAG, "Can't write: " + tag, e);
                    IoUtils.closeQuietly(gZIPOutputStream);
                    IoUtils.closeQuietly(inputStream);
                    entry.close();
                    if (file != null) {
                    }
                } catch (Throwable th5) {
                    th = th5;
                    file = null;
                    IoUtils.closeQuietly(gZIPOutputStream);
                    IoUtils.closeQuietly(inputStream);
                    entry.close();
                    if (file != null) {
                    }
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
            }
        } catch (IOException e7) {
            e = e7;
            gZIPOutputStream = null;
            inputStream = null;
        } catch (Throwable th7) {
            th = th7;
            gZIPOutputStream = null;
            inputStream = null;
        }
    }

    public boolean isTagEnabled(String str) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = this.mContentResolver;
            return !"disabled".equals(Settings.Global.getString(contentResolver, "dropbox:" + str));
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public synchronized DropBoxManager.Entry getNextEntry(String str, long j) {
        if (getContext().checkCallingOrSelfPermission("android.permission.READ_LOGS") != 0) {
            throw new SecurityException("READ_LOGS permission required");
        }
        try {
            init();
            FileList fileList = str == null ? this.mAllFiles : this.mFilesByTag.get(str);
            if (fileList == null) {
                return null;
            }
            for (EntryFile entryFile : fileList.contents.tailSet(new EntryFile(j + 1))) {
                if (entryFile.tag != null) {
                    if ((entryFile.flags & 1) != 0) {
                        return new DropBoxManager.Entry(entryFile.tag, entryFile.timestampMillis);
                    }
                    File file = entryFile.getFile(this.mDropBoxDir);
                    try {
                        return new DropBoxManager.Entry(entryFile.tag, entryFile.timestampMillis, file, entryFile.flags);
                    } catch (IOException e) {
                        Slog.wtf(TAG, "Can't read: " + file, e);
                    }
                }
            }
            return null;
        } catch (IOException e2) {
            Slog.e(TAG, "Can't init", e2);
            return null;
        }
    }

    public synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        ArrayList arrayList;
        int i;
        Throwable th;
        InputStreamReader inputStreamReader;
        DropBoxManager.Entry entry;
        DropBoxManager.Entry entry2;
        InputStreamReader inputStreamReader2;
        if (DumpUtils.checkDumpAndUsageStatsPermission(getContext(), TAG, printWriter)) {
            try {
                init();
                StringBuilder sb = new StringBuilder();
                ArrayList<String> arrayList2 = new ArrayList();
                boolean z = false;
                boolean z2 = false;
                for (int i2 = 0; strArr != null && i2 < strArr.length; i2++) {
                    if (strArr[i2].equals("-p") || strArr[i2].equals("--print")) {
                        z = true;
                    } else {
                        if (!strArr[i2].equals("-f") && !strArr[i2].equals("--file")) {
                            if (!strArr[i2].equals("-h") && !strArr[i2].equals("--help")) {
                                if (strArr[i2].startsWith("-")) {
                                    sb.append("Unknown argument: ");
                                    sb.append(strArr[i2]);
                                    sb.append("\n");
                                } else {
                                    arrayList2.add(strArr[i2]);
                                }
                            }
                            printWriter.println("Dropbox (dropbox) dump options:");
                            printWriter.println("  [-h|--help] [-p|--print] [-f|--file] [timestamp]");
                            printWriter.println("    -h|--help: print this help");
                            printWriter.println("    -p|--print: print full contents of each entry");
                            printWriter.println("    -f|--file: print path of each entry's file");
                            printWriter.println("  [timestamp] optionally filters to only those entries.");
                            return;
                        }
                        z2 = true;
                    }
                }
                sb.append("Drop box contents: ");
                sb.append(this.mAllFiles.contents.size());
                sb.append(" entries\n");
                sb.append("Max entries: ");
                sb.append(this.mMaxFiles);
                sb.append("\n");
                if (!arrayList2.isEmpty()) {
                    sb.append("Searching for:");
                    for (String str : arrayList2) {
                        sb.append(" ");
                        sb.append(str);
                    }
                    sb.append("\n");
                }
                int size = arrayList2.size();
                Time time = new Time();
                sb.append("\n");
                int i3 = 0;
                for (EntryFile entryFile : this.mAllFiles.contents) {
                    time.set(entryFile.timestampMillis);
                    String str2 = time.format("%Y-%m-%d %H:%M:%S");
                    boolean z3 = true;
                    for (int i4 = 0; i4 < size && z3; i4++) {
                        String str3 = (String) arrayList2.get(i4);
                        z3 = str2.contains(str3) || str3.equals(entryFile.tag);
                    }
                    if (z3) {
                        int i5 = i3 + 1;
                        if (z) {
                            sb.append("========================================\n");
                        }
                        sb.append(str2);
                        sb.append(" ");
                        sb.append(entryFile.tag == null ? "(no tag)" : entryFile.tag);
                        File file = entryFile.getFile(this.mDropBoxDir);
                        if (file == null) {
                            sb.append(" (no file)\n");
                        } else if ((entryFile.flags & 1) != 0) {
                            sb.append(" (contents lost)\n");
                        } else {
                            sb.append(" (");
                            if ((entryFile.flags & 4) != 0) {
                                sb.append("compressed ");
                            }
                            sb.append((entryFile.flags & 2) != 0 ? "text" : "data");
                            sb.append(", ");
                            sb.append(file.length());
                            sb.append(" bytes)\n");
                            if (z2 || (z && (entryFile.flags & 2) == 0)) {
                                if (!z) {
                                    sb.append("    ");
                                }
                                sb.append(file.getPath());
                                sb.append("\n");
                            }
                            if ((entryFile.flags & 2) == 0 || (!z && z2)) {
                                arrayList = arrayList2;
                            } else {
                                try {
                                    DropBoxManager.Entry entry3 = new DropBoxManager.Entry(entryFile.tag, entryFile.timestampMillis, file, entryFile.flags);
                                    if (z) {
                                        try {
                                            entry = entry3;
                                            try {
                                                try {
                                                    inputStreamReader2 = new InputStreamReader(entry.getInputStream());
                                                } catch (Throwable th2) {
                                                    th = th2;
                                                    th = th;
                                                    inputStreamReader = null;
                                                    if (entry != null) {
                                                        entry.close();
                                                    }
                                                    if (inputStreamReader == null) {
                                                        throw th;
                                                    }
                                                    try {
                                                        inputStreamReader.close();
                                                        throw th;
                                                    } catch (IOException e) {
                                                        throw th;
                                                    }
                                                }
                                                try {
                                                    try {
                                                        char[] cArr = new char[4096];
                                                        boolean z4 = false;
                                                        while (true) {
                                                            int i6 = inputStreamReader2.read(cArr);
                                                            if (i6 <= 0) {
                                                                break;
                                                            }
                                                            arrayList = arrayList2;
                                                            try {
                                                                sb.append(cArr, 0, i6);
                                                                z4 = cArr[i6 + (-1)] == '\n';
                                                                if (sb.length() > 65536) {
                                                                    printWriter.write(sb.toString());
                                                                    sb.setLength(0);
                                                                }
                                                                arrayList2 = arrayList;
                                                            } catch (IOException e2) {
                                                                e = e2;
                                                                inputStreamReader = inputStreamReader2;
                                                                entry2 = entry;
                                                                try {
                                                                    sb.append("*** ");
                                                                    sb.append(e.toString());
                                                                    sb.append("\n");
                                                                    StringBuilder sb2 = new StringBuilder();
                                                                    i = i5;
                                                                    sb2.append("Can't read: ");
                                                                    sb2.append(file);
                                                                    Slog.e(TAG, sb2.toString(), e);
                                                                    if (entry2 != null) {
                                                                        entry2.close();
                                                                    }
                                                                    if (inputStreamReader != null) {
                                                                        try {
                                                                            inputStreamReader.close();
                                                                        } catch (IOException e3) {
                                                                        }
                                                                    }
                                                                    if (z) {
                                                                    }
                                                                    arrayList2 = arrayList;
                                                                    i3 = i;
                                                                } catch (Throwable th3) {
                                                                    th = th3;
                                                                    entry = entry2;
                                                                    if (entry != null) {
                                                                    }
                                                                    if (inputStreamReader == null) {
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        if (!z4) {
                                                            sb.append("\n");
                                                        }
                                                        arrayList = arrayList2;
                                                        entry.close();
                                                        if (inputStreamReader2 != null) {
                                                            try {
                                                                inputStreamReader2.close();
                                                            } catch (IOException e4) {
                                                            }
                                                        }
                                                    } catch (IOException e5) {
                                                        e = e5;
                                                        arrayList = arrayList2;
                                                    }
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    inputStreamReader = inputStreamReader2;
                                                    if (entry != null) {
                                                    }
                                                    if (inputStreamReader == null) {
                                                    }
                                                }
                                            } catch (IOException e6) {
                                                e = e6;
                                                arrayList = arrayList2;
                                                entry2 = entry;
                                                inputStreamReader = null;
                                                sb.append("*** ");
                                                sb.append(e.toString());
                                                sb.append("\n");
                                                StringBuilder sb22 = new StringBuilder();
                                                i = i5;
                                                sb22.append("Can't read: ");
                                                sb22.append(file);
                                                Slog.e(TAG, sb22.toString(), e);
                                                if (entry2 != null) {
                                                }
                                                if (inputStreamReader != null) {
                                                }
                                                if (z) {
                                                }
                                                arrayList2 = arrayList;
                                                i3 = i;
                                            }
                                        } catch (IOException e7) {
                                            e = e7;
                                            arrayList = arrayList2;
                                            entry = entry3;
                                        } catch (Throwable th5) {
                                            th = th5;
                                            entry = entry3;
                                        }
                                    } else {
                                        arrayList = arrayList2;
                                        entry = entry3;
                                        try {
                                            String text = entry.getText(70);
                                            sb.append("    ");
                                            if (text == null) {
                                                sb.append("[null]");
                                            } else {
                                                boolean z5 = text.length() == 70;
                                                sb.append(text.trim().replace('\n', '/'));
                                                if (z5) {
                                                    sb.append(" ...");
                                                }
                                            }
                                            sb.append("\n");
                                            inputStreamReader2 = null;
                                            entry.close();
                                            if (inputStreamReader2 != null) {
                                            }
                                        } catch (IOException e8) {
                                            e = e8;
                                            entry2 = entry;
                                            inputStreamReader = null;
                                            sb.append("*** ");
                                            sb.append(e.toString());
                                            sb.append("\n");
                                            StringBuilder sb222 = new StringBuilder();
                                            i = i5;
                                            sb222.append("Can't read: ");
                                            sb222.append(file);
                                            Slog.e(TAG, sb222.toString(), e);
                                            if (entry2 != null) {
                                            }
                                            if (inputStreamReader != null) {
                                            }
                                        }
                                    }
                                } catch (IOException e9) {
                                    e = e9;
                                    arrayList = arrayList2;
                                    inputStreamReader = null;
                                    entry2 = null;
                                } catch (Throwable th6) {
                                    th = th6;
                                    inputStreamReader = null;
                                    entry = null;
                                }
                                arrayList2 = arrayList;
                                i3 = i;
                            }
                            i = i5;
                            if (z) {
                                sb.append("\n");
                            }
                            arrayList2 = arrayList;
                            i3 = i;
                        }
                        arrayList = arrayList2;
                        i = i5;
                        arrayList2 = arrayList;
                        i3 = i;
                    }
                }
                if (i3 == 0) {
                    sb.append("(No entries found.)\n");
                }
                if (strArr == null || strArr.length == 0) {
                    if (!z) {
                        sb.append("\n");
                    }
                    sb.append("Usage: dumpsys dropbox [--print|--file] [YYYY-mm-dd] [HH:MM:SS] [tag]\n");
                }
                printWriter.write(sb.toString());
            } catch (IOException e10) {
                printWriter.println("Can't initialize: " + e10);
                Slog.e(TAG, "Can't init", e10);
            }
        }
    }

    private static final class FileList implements Comparable<FileList> {
        public int blocks;
        public final TreeSet<EntryFile> contents;

        private FileList() {
            this.blocks = 0;
            this.contents = new TreeSet<>();
        }

        @Override
        public final int compareTo(FileList fileList) {
            if (this.blocks != fileList.blocks) {
                return fileList.blocks - this.blocks;
            }
            if (this == fileList) {
                return 0;
            }
            if (hashCode() < fileList.hashCode()) {
                return -1;
            }
            return hashCode() > fileList.hashCode() ? 1 : 0;
        }
    }

    @VisibleForTesting
    static final class EntryFile implements Comparable<EntryFile> {
        public final int blocks;
        public final int flags;
        public final String tag;
        public final long timestampMillis;

        @Override
        public final int compareTo(EntryFile entryFile) {
            int iCompare = Long.compare(this.timestampMillis, entryFile.timestampMillis);
            if (iCompare != 0) {
                return iCompare;
            }
            int iCompare2 = ObjectUtils.compare(this.tag, entryFile.tag);
            if (iCompare2 != 0) {
                return iCompare2;
            }
            int iCompare3 = Integer.compare(this.flags, entryFile.flags);
            return iCompare3 != 0 ? iCompare3 : Integer.compare(hashCode(), entryFile.hashCode());
        }

        public EntryFile(File file, File file2, String str, long j, int i, int i2) throws IOException {
            if ((i & 1) != 0) {
                throw new IllegalArgumentException();
            }
            this.tag = TextUtils.safeIntern(str);
            this.timestampMillis = j;
            this.flags = i;
            File file3 = getFile(file2);
            if (!file.renameTo(file3)) {
                throw new IOException("Can't rename " + file + " to " + file3);
            }
            long j2 = i2;
            this.blocks = (int) (((file3.length() + j2) - 1) / j2);
        }

        public EntryFile(File file, String str, long j) throws IOException {
            this.tag = TextUtils.safeIntern(str);
            this.timestampMillis = j;
            this.flags = 1;
            this.blocks = 0;
            new FileOutputStream(getFile(file)).close();
        }

        public EntryFile(File file, int i) {
            String strDecode;
            int i2;
            String str;
            boolean z;
            String strSubstring;
            long j;
            String name = file.getName();
            int iLastIndexOf = name.lastIndexOf(64);
            if (iLastIndexOf >= 0) {
                strDecode = Uri.decode(name.substring(0, iLastIndexOf));
                if (name.endsWith(PackageManagerService.COMPRESSED_EXTENSION)) {
                    name = name.substring(0, name.length() - 3);
                    i2 = 4;
                } else {
                    i2 = 0;
                }
                if (name.endsWith(".lost")) {
                    i2 |= 1;
                    strSubstring = name.substring(iLastIndexOf + 1, name.length() - 5);
                } else if (name.endsWith(".txt")) {
                    i2 |= 2;
                    strSubstring = name.substring(iLastIndexOf + 1, name.length() - 4);
                } else if (name.endsWith(".dat")) {
                    strSubstring = name.substring(iLastIndexOf + 1, name.length() - 4);
                } else {
                    str = name;
                    z = true;
                    if (!z) {
                        j = 0;
                    } else {
                        try {
                            j = Long.parseLong(str);
                        } catch (NumberFormatException e) {
                            j = 0;
                            z = true;
                        }
                    }
                    if (z) {
                        Slog.wtf(DropBoxManagerService.TAG, "Invalid filename: " + file);
                        file.delete();
                        this.tag = null;
                        this.flags = 1;
                        this.timestampMillis = 0L;
                        this.blocks = 0;
                        return;
                    }
                    long length = file.length();
                    long j2 = i;
                    this.blocks = (int) (((length + j2) - 1) / j2);
                    this.tag = TextUtils.safeIntern(strDecode);
                    this.flags = i2;
                    this.timestampMillis = j;
                    return;
                }
                str = strSubstring;
                z = false;
                if (!z) {
                }
                if (z) {
                }
            } else {
                j = 0;
                strDecode = null;
                i2 = 0;
            }
            z = true;
            if (z) {
            }
        }

        public EntryFile(long j) {
            this.tag = null;
            this.timestampMillis = j;
            this.flags = 1;
            this.blocks = 0;
        }

        public boolean hasFile() {
            return this.tag != null;
        }

        private String getExtension() {
            if ((this.flags & 1) != 0) {
                return ".lost";
            }
            StringBuilder sb = new StringBuilder();
            sb.append((this.flags & 2) != 0 ? ".txt" : ".dat");
            sb.append((this.flags & 4) != 0 ? PackageManagerService.COMPRESSED_EXTENSION : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            return sb.toString();
        }

        public String getFilename() {
            if (!hasFile()) {
                return null;
            }
            return Uri.encode(this.tag) + "@" + this.timestampMillis + getExtension();
        }

        public File getFile(File file) {
            if (hasFile()) {
                return new File(file, getFilename());
            }
            return null;
        }

        public void deleteFile(File file) {
            if (hasFile()) {
                getFile(file).delete();
            }
        }
    }

    private synchronized void init() throws IOException {
        if (this.mStatFs == null) {
            if (!this.mDropBoxDir.isDirectory() && !this.mDropBoxDir.mkdirs()) {
                throw new IOException("Can't mkdir: " + this.mDropBoxDir);
            }
            try {
                this.mStatFs = new StatFs(this.mDropBoxDir.getPath());
                this.mBlockSize = this.mStatFs.getBlockSize();
            } catch (IllegalArgumentException e) {
                throw new IOException("Can't statfs: " + this.mDropBoxDir);
            }
        }
        if (this.mAllFiles == null) {
            File[] fileArrListFiles = this.mDropBoxDir.listFiles();
            if (fileArrListFiles == null) {
                throw new IOException("Can't list files: " + this.mDropBoxDir);
            }
            this.mAllFiles = new FileList();
            this.mFilesByTag = new ArrayMap<>();
            for (File file : fileArrListFiles) {
                if (file.getName().endsWith(".tmp")) {
                    Slog.i(TAG, "Cleaning temp file: " + file);
                    file.delete();
                } else {
                    EntryFile entryFile = new EntryFile(file, this.mBlockSize);
                    if (entryFile.hasFile()) {
                        enrollEntry(entryFile);
                    }
                }
            }
        }
    }

    private synchronized void enrollEntry(EntryFile entryFile) {
        this.mAllFiles.contents.add(entryFile);
        this.mAllFiles.blocks += entryFile.blocks;
        if (entryFile.hasFile() && entryFile.blocks > 0) {
            FileList fileList = this.mFilesByTag.get(entryFile.tag);
            if (fileList == null) {
                fileList = new FileList();
                this.mFilesByTag.put(TextUtils.safeIntern(entryFile.tag), fileList);
            }
            fileList.contents.add(entryFile);
            fileList.blocks += entryFile.blocks;
        }
    }

    private synchronized long createEntry(File file, String str, int i) throws IOException {
        long j;
        long jCurrentTimeMillis = System.currentTimeMillis();
        SortedSet<EntryFile> sortedSetTailSet = this.mAllFiles.contents.tailSet(new EntryFile(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY + jCurrentTimeMillis));
        EntryFile[] entryFileArr = null;
        if (!sortedSetTailSet.isEmpty()) {
            entryFileArr = (EntryFile[]) sortedSetTailSet.toArray(new EntryFile[sortedSetTailSet.size()]);
            sortedSetTailSet.clear();
        }
        if (!this.mAllFiles.contents.isEmpty()) {
            jCurrentTimeMillis = Math.max(jCurrentTimeMillis, this.mAllFiles.contents.last().timestampMillis + 1);
        }
        if (entryFileArr != null) {
            j = jCurrentTimeMillis;
            for (EntryFile entryFile : entryFileArr) {
                this.mAllFiles.blocks -= entryFile.blocks;
                FileList fileList = this.mFilesByTag.get(entryFile.tag);
                if (fileList != null && fileList.contents.remove(entryFile)) {
                    fileList.blocks -= entryFile.blocks;
                }
                if ((entryFile.flags & 1) == 0) {
                    enrollEntry(new EntryFile(entryFile.getFile(this.mDropBoxDir), this.mDropBoxDir, entryFile.tag, j, entryFile.flags, this.mBlockSize));
                    j++;
                } else {
                    enrollEntry(new EntryFile(this.mDropBoxDir, entryFile.tag, j));
                    j++;
                }
            }
        } else {
            j = jCurrentTimeMillis;
        }
        if (file == null) {
            enrollEntry(new EntryFile(this.mDropBoxDir, str, j));
        } else {
            enrollEntry(new EntryFile(file, this.mDropBoxDir, str, j, i, this.mBlockSize));
        }
        return j;
    }

    private synchronized long trimToFit() throws IOException {
        int i;
        int i2 = Settings.Global.getInt(this.mContentResolver, "dropbox_age_seconds", DEFAULT_AGE_SECONDS);
        ContentResolver contentResolver = this.mContentResolver;
        if (!ActivityManager.isLowRamDeviceStatic()) {
            i = 1000;
        } else {
            i = 300;
        }
        this.mMaxFiles = Settings.Global.getInt(contentResolver, "dropbox_max_files", i);
        long jCurrentTimeMillis = System.currentTimeMillis() - ((long) (i2 * 1000));
        while (!this.mAllFiles.contents.isEmpty()) {
            EntryFile entryFileFirst = this.mAllFiles.contents.first();
            if (entryFileFirst.timestampMillis > jCurrentTimeMillis && this.mAllFiles.contents.size() < this.mMaxFiles) {
                break;
            }
            FileList fileList = this.mFilesByTag.get(entryFileFirst.tag);
            if (fileList != null && fileList.contents.remove(entryFileFirst)) {
                fileList.blocks -= entryFileFirst.blocks;
            }
            if (this.mAllFiles.contents.remove(entryFileFirst)) {
                this.mAllFiles.blocks -= entryFileFirst.blocks;
            }
            entryFileFirst.deleteFile(this.mDropBoxDir);
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        int i3 = 0;
        if (jUptimeMillis > this.mCachedQuotaUptimeMillis + 5000) {
            int i4 = Settings.Global.getInt(this.mContentResolver, "dropbox_quota_percent", 10);
            int i5 = Settings.Global.getInt(this.mContentResolver, "dropbox_reserve_percent", 10);
            int i6 = Settings.Global.getInt(this.mContentResolver, "dropbox_quota_kb", DEFAULT_QUOTA_KB);
            try {
                this.mStatFs.restat(this.mDropBoxDir.getPath());
                this.mCachedQuotaBlocks = Math.min((i6 * 1024) / this.mBlockSize, Math.max(0, ((this.mStatFs.getAvailableBlocks() - ((this.mStatFs.getBlockCount() * i5) / 100)) * i4) / 100));
                this.mCachedQuotaUptimeMillis = jUptimeMillis;
                if (this.mAllFiles.blocks > this.mCachedQuotaBlocks) {
                    int i7 = this.mAllFiles.blocks;
                    TreeSet<FileList> treeSet = new TreeSet(this.mFilesByTag.values());
                    for (FileList fileList2 : treeSet) {
                        if (i3 > 0 && fileList2.blocks <= (this.mCachedQuotaBlocks - i7) / i3) {
                            break;
                        }
                        i7 -= fileList2.blocks;
                        i3++;
                    }
                    int i8 = (this.mCachedQuotaBlocks - i7) / i3;
                    for (FileList fileList3 : treeSet) {
                        if (this.mAllFiles.blocks < this.mCachedQuotaBlocks) {
                            break;
                        }
                        while (fileList3.blocks > i8 && !fileList3.contents.isEmpty()) {
                            EntryFile entryFileFirst2 = fileList3.contents.first();
                            if (fileList3.contents.remove(entryFileFirst2)) {
                                fileList3.blocks -= entryFileFirst2.blocks;
                            }
                            if (this.mAllFiles.contents.remove(entryFileFirst2)) {
                                this.mAllFiles.blocks -= entryFileFirst2.blocks;
                            }
                            try {
                                entryFileFirst2.deleteFile(this.mDropBoxDir);
                                enrollEntry(new EntryFile(this.mDropBoxDir, entryFileFirst2.tag, entryFileFirst2.timestampMillis));
                            } catch (IOException e) {
                                Slog.e(TAG, "Can't write tombstone file", e);
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException e2) {
                throw new IOException("Can't restat: " + this.mDropBoxDir);
            }
        } else {
            if (this.mAllFiles.blocks > this.mCachedQuotaBlocks) {
            }
        }
        return this.mCachedQuotaBlocks * this.mBlockSize;
    }
}
