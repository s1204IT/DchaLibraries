package com.android.server;

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
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Slog;
import com.android.internal.os.IDropBoxManagerService;
import com.android.server.job.controllers.JobStatus;
import com.mediatek.datashaping.DataShapingUtils;
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
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import libcore.io.IoUtils;

public final class DropBoxManagerService extends SystemService {
    private static final int DEFAULT_AGE_SECONDS = 259200;
    private static final int DEFAULT_MAX_FILES = 1000;
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
    private HashMap<String, FileList> mFilesByTag;
    private final Handler mHandler;
    private final BroadcastReceiver mReceiver;
    private StatFs mStatFs;
    private final IDropBoxManagerService.Stub mStub;

    public DropBoxManagerService(Context context) {
        this(context, new File("/data/system/dropbox"));
    }

    public DropBoxManagerService(Context context, File path) {
        super(context);
        this.mAllFiles = null;
        this.mFilesByTag = null;
        this.mStatFs = null;
        this.mBlockSize = 0;
        this.mCachedQuotaBlocks = 0;
        this.mCachedQuotaUptimeMillis = 0L;
        this.mBooted = false;
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

            public boolean isTagEnabled(String tag) {
                return DropBoxManagerService.this.isTagEnabled(tag);
            }

            public DropBoxManager.Entry getNextEntry(String tag, long millis) {
                return DropBoxManagerService.this.getNextEntry(tag, millis);
            }

            public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                DropBoxManagerService.this.dump(fd, pw, args);
            }
        };
        this.mDropBoxDir = path;
        this.mContentResolver = getContext().getContentResolver();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    return;
                }
                DropBoxManagerService.this.getContext().sendBroadcastAsUser((Intent) msg.obj, UserHandle.SYSTEM, "android.permission.READ_LOGS");
            }
        };
    }

    @Override
    public void onStart() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.DEVICE_STORAGE_LOW");
        getContext().registerReceiver(this.mReceiver, filter);
        this.mContentResolver.registerContentObserver(Settings.Global.CONTENT_URI, true, new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                DropBoxManagerService.this.mReceiver.onReceive(DropBoxManagerService.this.getContext(), (Intent) null);
            }
        });
        publishBinderService("dropbox", this.mStub);
    }

    @Override
    public void onBootPhase(int phase) {
        switch (phase) {
            case 1000:
                this.mBooted = true;
                break;
        }
    }

    public IDropBoxManagerService getServiceStub() {
        return this.mStub;
    }

    public void add(DropBoxManager.Entry entry) throws Throwable {
        File file;
        File file2 = null;
        InputStream input = null;
        OutputStream output = null;
        String tag = entry.getTag();
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
                long max = trimToFit();
                long lastTrim = System.currentTimeMillis();
                byte[] buffer = new byte[this.mBlockSize];
                input = entry.getInputStream();
                int read = 0;
                while (read < buffer.length) {
                    int n = input.read(buffer, read, buffer.length - read);
                    if (n <= 0) {
                        break;
                    } else {
                        read += n;
                    }
                }
                File temp = new File(this.mDropBoxDir, "drop" + Thread.currentThread().getId() + ".tmp");
                try {
                    int bufferSize = this.mBlockSize;
                    if (bufferSize > 4096) {
                        bufferSize = 4096;
                    }
                    if (bufferSize < 512) {
                        bufferSize = 512;
                    }
                    FileOutputStream foutput = new FileOutputStream(temp);
                    OutputStream output2 = new BufferedOutputStream(foutput, bufferSize);
                    try {
                        if (read == buffer.length && (flags & 4) == 0) {
                            output = new GZIPOutputStream(output2);
                            flags |= 4;
                        } else {
                            output = output2;
                        }
                        while (true) {
                            output.write(buffer, 0, read);
                            long now = System.currentTimeMillis();
                            if (now - lastTrim > 30000) {
                                max = trimToFit();
                                lastTrim = now;
                            }
                            read = input.read(buffer);
                            if (read <= 0) {
                                FileUtils.sync(foutput);
                                output.close();
                                output = null;
                            } else {
                                output.flush();
                            }
                            long len = temp.length();
                            if (len > max) {
                                break;
                            } else if (read <= 0) {
                                file = temp;
                                break;
                            }
                        }
                        long time = createEntry(file, tag, flags);
                        Intent dropboxIntent = new Intent("android.intent.action.DROPBOX_ENTRY_ADDED");
                        dropboxIntent.putExtra("tag", tag);
                        dropboxIntent.putExtra("time", time);
                        if (!this.mBooted) {
                            dropboxIntent.addFlags(1073741824);
                        }
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, dropboxIntent));
                        IoUtils.closeQuietly(output);
                        IoUtils.closeQuietly(input);
                        entry.close();
                    } catch (IOException e) {
                        e = e;
                        output = output2;
                        file2 = temp;
                        Slog.e(TAG, "Can't write: " + tag, e);
                        IoUtils.closeQuietly(output);
                        IoUtils.closeQuietly(input);
                        entry.close();
                        if (file2 != null) {
                            file2.delete();
                        }
                    } catch (Throwable th) {
                        th = th;
                        output = output2;
                        file2 = temp;
                        IoUtils.closeQuietly(output);
                        IoUtils.closeQuietly(input);
                        entry.close();
                        if (file2 != null) {
                            file2.delete();
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    e = e2;
                    file2 = temp;
                } catch (Throwable th2) {
                    th = th2;
                    file2 = temp;
                }
            } catch (IOException e3) {
                e = e3;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    public boolean isTagEnabled(String tag) {
        long token = Binder.clearCallingIdentity();
        try {
            return !"disabled".equals(Settings.Global.getString(this.mContentResolver, new StringBuilder().append("dropbox:").append(tag).toString()));
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public synchronized DropBoxManager.Entry getNextEntry(String tag, long millis) {
        if (getContext().checkCallingOrSelfPermission("android.permission.READ_LOGS") != 0) {
            throw new SecurityException("READ_LOGS permission required");
        }
        try {
            init();
            FileList list = tag == null ? this.mAllFiles : this.mFilesByTag.get(tag);
            if (list == null) {
                return null;
            }
            for (EntryFile entry : list.contents.tailSet(new EntryFile(1 + millis))) {
                if (entry.tag != null) {
                    if ((entry.flags & 1) != 0) {
                        return new DropBoxManager.Entry(entry.tag, entry.timestampMillis);
                    }
                    try {
                        return new DropBoxManager.Entry(entry.tag, entry.timestampMillis, entry.file, entry.flags);
                    } catch (IOException e) {
                        Slog.e(TAG, "Can't read: " + entry.file, e);
                    }
                }
            }
            return null;
        } catch (IOException e2) {
            Slog.e(TAG, "Can't init", e2);
            return null;
        }
    }

    public synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        DropBoxManager.Entry dbe;
        InputStreamReader isr;
        if (getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: Can't dump DropBoxManagerService");
            return;
        }
        try {
            init();
            StringBuilder out = new StringBuilder();
            boolean doPrint = false;
            boolean doFile = false;
            ArrayList<String> searchArgs = new ArrayList<>();
            for (int i = 0; args != null && i < args.length; i++) {
                if (args[i].equals("-p") || args[i].equals("--print")) {
                    doPrint = true;
                } else if (args[i].equals("-f") || args[i].equals("--file")) {
                    doFile = true;
                } else if (args[i].startsWith("-")) {
                    out.append("Unknown argument: ").append(args[i]).append("\n");
                } else {
                    searchArgs.add(args[i]);
                }
            }
            out.append("Drop box contents: ").append(this.mAllFiles.contents.size()).append(" entries\n");
            if (!searchArgs.isEmpty()) {
                out.append("Searching for:");
                for (String a : searchArgs) {
                    out.append(" ").append(a);
                }
                out.append("\n");
            }
            int numFound = 0;
            int numArgs = searchArgs.size();
            Time time = new Time();
            out.append("\n");
            for (EntryFile entry : this.mAllFiles.contents) {
                time.set(entry.timestampMillis);
                String date = time.format("%Y-%m-%d %H:%M:%S");
                boolean match = true;
                for (int i2 = 0; i2 < numArgs && match; i2++) {
                    String arg = searchArgs.get(i2);
                    match = !date.contains(arg) ? arg.equals(entry.tag) : true;
                }
                if (match) {
                    numFound++;
                    if (doPrint) {
                        out.append("========================================\n");
                    }
                    out.append(date).append(" ").append(entry.tag == null ? "(no tag)" : entry.tag);
                    if (entry.file == null) {
                        out.append(" (no file)\n");
                    } else if ((entry.flags & 1) != 0) {
                        out.append(" (contents lost)\n");
                    } else {
                        out.append(" (");
                        if ((entry.flags & 4) != 0) {
                            out.append("compressed ");
                        }
                        out.append((entry.flags & 2) != 0 ? "text" : "data");
                        out.append(", ").append(entry.file.length()).append(" bytes)\n");
                        if (doFile || (doPrint && (entry.flags & 2) == 0)) {
                            if (!doPrint) {
                                out.append("    ");
                            }
                            out.append(entry.file.getPath()).append("\n");
                        }
                        if ((entry.flags & 2) != 0 && (doPrint || !doFile)) {
                            InputStreamReader isr2 = null;
                            try {
                                dbe = new DropBoxManager.Entry(entry.tag, entry.timestampMillis, entry.file, entry.flags);
                                if (doPrint) {
                                    try {
                                        try {
                                            isr = new InputStreamReader(dbe.getInputStream());
                                        } catch (Throwable th) {
                                            th = th;
                                        }
                                    } catch (IOException e) {
                                        e = e;
                                    }
                                    try {
                                        char[] buf = new char[4096];
                                        boolean newline = false;
                                        while (true) {
                                            int n = isr.read(buf);
                                            if (n <= 0) {
                                                break;
                                            }
                                            out.append(buf, 0, n);
                                            newline = buf[n + (-1)] == '\n';
                                            if (out.length() > 65536) {
                                                pw.write(out.toString());
                                                out.setLength(0);
                                            }
                                        }
                                        if (newline) {
                                            isr2 = isr;
                                        } else {
                                            out.append("\n");
                                            isr2 = isr;
                                        }
                                    } catch (IOException e2) {
                                        e = e2;
                                        isr2 = isr;
                                        out.append("*** ").append(e.toString()).append("\n");
                                        Slog.e(TAG, "Can't read: " + entry.file, e);
                                        if (dbe != null) {
                                            dbe.close();
                                        }
                                        if (isr2 != null) {
                                            try {
                                                isr2.close();
                                            } catch (IOException e3) {
                                            }
                                        }
                                    } catch (Throwable th2) {
                                        th = th2;
                                        isr2 = isr;
                                        if (dbe != null) {
                                            dbe.close();
                                        }
                                        if (isr2 != null) {
                                            try {
                                                isr2.close();
                                            } catch (IOException e4) {
                                            }
                                        }
                                        throw th;
                                    }
                                } else {
                                    String text = dbe.getText(70);
                                    out.append("    ");
                                    if (text == null) {
                                        out.append("[null]");
                                    } else {
                                        boolean truncated = text.length() == 70;
                                        out.append(text.trim().replace('\n', '/'));
                                        if (truncated) {
                                            out.append(" ...");
                                        }
                                    }
                                    out.append("\n");
                                }
                                if (dbe != null) {
                                    dbe.close();
                                }
                                if (isr2 != null) {
                                    try {
                                        isr2.close();
                                    } catch (IOException e5) {
                                    }
                                }
                            } catch (IOException e6) {
                                e = e6;
                                dbe = null;
                            } catch (Throwable th3) {
                                th = th3;
                                dbe = null;
                            }
                        }
                        if (doPrint) {
                            out.append("\n");
                        }
                    }
                }
            }
            if (numFound == 0) {
                out.append("(No entries found.)\n");
            }
            if (args == null || args.length == 0) {
                if (!doPrint) {
                    out.append("\n");
                }
                out.append("Usage: dumpsys dropbox [--print|--file] [YYYY-mm-dd] [HH:MM:SS] [tag]\n");
            }
            pw.write(out.toString());
        } catch (IOException e7) {
            pw.println("Can't initialize: " + e7);
            Slog.e(TAG, "Can't init", e7);
        }
    }

    private static final class FileList implements Comparable<FileList> {
        public int blocks;
        public final TreeSet<EntryFile> contents;

        FileList(FileList fileList) {
            this();
        }

        private FileList() {
            this.blocks = 0;
            this.contents = new TreeSet<>();
        }

        @Override
        public final int compareTo(FileList o) {
            if (this.blocks != o.blocks) {
                return o.blocks - this.blocks;
            }
            if (this == o) {
                return 0;
            }
            if (hashCode() < o.hashCode()) {
                return -1;
            }
            return hashCode() > o.hashCode() ? 1 : 0;
        }
    }

    private static final class EntryFile implements Comparable<EntryFile> {
        public final int blocks;
        public final File file;
        public final int flags;
        public final String tag;
        public final long timestampMillis;

        @Override
        public final int compareTo(EntryFile o) {
            if (this.timestampMillis < o.timestampMillis) {
                return -1;
            }
            if (this.timestampMillis > o.timestampMillis) {
                return 1;
            }
            if (this.file != null && o.file != null) {
                return this.file.compareTo(o.file);
            }
            if (o.file != null) {
                return -1;
            }
            if (this.file != null) {
                return 1;
            }
            if (this == o) {
                return 0;
            }
            if (hashCode() < o.hashCode()) {
                return -1;
            }
            return hashCode() > o.hashCode() ? 1 : 0;
        }

        public EntryFile(File temp, File dir, String tag, long timestampMillis, int flags, int blockSize) throws IOException {
            if ((flags & 1) != 0) {
                throw new IllegalArgumentException();
            }
            this.tag = tag;
            this.timestampMillis = timestampMillis;
            this.flags = flags;
            this.file = new File(dir, Uri.encode(tag) + "@" + timestampMillis + ((flags & 2) != 0 ? ".txt" : ".dat") + ((flags & 4) != 0 ? ".gz" : ""));
            if (!temp.renameTo(this.file)) {
                throw new IOException("Can't rename " + temp + " to " + this.file);
            }
            this.blocks = (int) (((this.file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
        }

        public EntryFile(File dir, String tag, long timestampMillis) throws IOException {
            this.tag = tag;
            this.timestampMillis = timestampMillis;
            this.flags = 1;
            this.file = new File(dir, Uri.encode(tag) + "@" + timestampMillis + ".lost");
            this.blocks = 0;
            new FileOutputStream(this.file).close();
        }

        public EntryFile(File file, int blockSize) {
            String name;
            long millis;
            this.file = file;
            this.blocks = (int) (((this.file.length() + ((long) blockSize)) - 1) / ((long) blockSize));
            String name2 = file.getName();
            int at = name2.lastIndexOf(64);
            if (at < 0) {
                this.tag = null;
                this.timestampMillis = 0L;
                this.flags = 1;
                return;
            }
            int flags = 0;
            this.tag = Uri.decode(name2.substring(0, at));
            if (name2.endsWith(".gz")) {
                flags = 4;
                name2 = name2.substring(0, name2.length() - 3);
            }
            if (name2.endsWith(".lost")) {
                flags |= 1;
                name = name2.substring(at + 1, name2.length() - 5);
            } else if (name2.endsWith(".txt")) {
                flags |= 2;
                name = name2.substring(at + 1, name2.length() - 4);
            } else if (name2.endsWith(".dat")) {
                name = name2.substring(at + 1, name2.length() - 4);
            } else {
                this.flags = 1;
                this.timestampMillis = 0L;
                return;
            }
            this.flags = flags;
            try {
                millis = Long.valueOf(name).longValue();
            } catch (NumberFormatException e) {
                millis = 0;
            }
            this.timestampMillis = millis;
        }

        public EntryFile(long millis) {
            this.tag = null;
            this.timestampMillis = millis;
            this.flags = 1;
            this.file = null;
            this.blocks = 0;
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
            File[] files = this.mDropBoxDir.listFiles();
            if (files == null) {
                throw new IOException("Can't list files: " + this.mDropBoxDir);
            }
            this.mAllFiles = new FileList(null);
            this.mFilesByTag = new HashMap<>();
            for (File file : files) {
                if (file.getName().endsWith(".tmp")) {
                    Slog.i(TAG, "Cleaning temp file: " + file);
                    file.delete();
                } else {
                    EntryFile entry = new EntryFile(file, this.mBlockSize);
                    if (entry.tag == null) {
                        Slog.w(TAG, "Unrecognized file: " + file);
                    } else if (entry.timestampMillis == 0) {
                        Slog.w(TAG, "Invalid filename: " + file);
                        file.delete();
                    } else {
                        enrollEntry(entry);
                    }
                }
            }
        }
    }

    private synchronized void enrollEntry(EntryFile entry) {
        this.mAllFiles.contents.add(entry);
        this.mAllFiles.blocks += entry.blocks;
        if (entry.tag != null && entry.file != null && entry.blocks > 0) {
            FileList tagFiles = this.mFilesByTag.get(entry.tag);
            if (tagFiles == null) {
                tagFiles = new FileList(null);
                this.mFilesByTag.put(entry.tag, tagFiles);
            }
            tagFiles.contents.add(entry);
            tagFiles.blocks += entry.blocks;
        }
    }

    private synchronized long createEntry(File temp, String tag, int flags) throws IOException {
        long t;
        t = System.currentTimeMillis();
        SortedSet<EntryFile> tail = this.mAllFiles.contents.tailSet(new EntryFile(JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY + t));
        EntryFile[] future = null;
        if (!tail.isEmpty()) {
            future = (EntryFile[]) tail.toArray(new EntryFile[tail.size()]);
            tail.clear();
        }
        if (!this.mAllFiles.contents.isEmpty()) {
            t = Math.max(t, this.mAllFiles.contents.last().timestampMillis + 1);
        }
        if (future != null) {
            for (EntryFile late : future) {
                this.mAllFiles.blocks -= late.blocks;
                FileList tagFiles = this.mFilesByTag.get(late.tag);
                if (tagFiles != null && tagFiles.contents.remove(late)) {
                    tagFiles.blocks -= late.blocks;
                }
                if ((late.flags & 1) == 0) {
                    enrollEntry(new EntryFile(late.file, this.mDropBoxDir, late.tag, t, late.flags, this.mBlockSize));
                    t++;
                } else {
                    enrollEntry(new EntryFile(this.mDropBoxDir, late.tag, t));
                    t++;
                }
            }
        }
        if (temp == null) {
            enrollEntry(new EntryFile(this.mDropBoxDir, tag, t));
        } else {
            enrollEntry(new EntryFile(temp, this.mDropBoxDir, tag, t, flags, this.mBlockSize));
        }
        return t;
    }

    private synchronized long trimToFit() {
        int ageSeconds = Settings.Global.getInt(this.mContentResolver, "dropbox_age_seconds", DEFAULT_AGE_SECONDS);
        int maxFiles = Settings.Global.getInt(this.mContentResolver, "dropbox_max_files", 1000);
        long cutoffMillis = System.currentTimeMillis() - ((long) (ageSeconds * 1000));
        while (!this.mAllFiles.contents.isEmpty()) {
            EntryFile entry = this.mAllFiles.contents.first();
            if (entry.timestampMillis > cutoffMillis && this.mAllFiles.contents.size() < maxFiles) {
                break;
            }
            FileList tag = this.mFilesByTag.get(entry.tag);
            if (tag != null && tag.contents.remove(entry)) {
                tag.blocks -= entry.blocks;
            }
            if (this.mAllFiles.contents.remove(entry)) {
                this.mAllFiles.blocks -= entry.blocks;
            }
            if (entry.file != null) {
                entry.file.delete();
            }
        }
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis > this.mCachedQuotaUptimeMillis + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC) {
            int quotaPercent = Settings.Global.getInt(this.mContentResolver, "dropbox_quota_percent", 10);
            int reservePercent = Settings.Global.getInt(this.mContentResolver, "dropbox_reserve_percent", 10);
            int quotaKb = Settings.Global.getInt(this.mContentResolver, "dropbox_quota_kb", DEFAULT_QUOTA_KB);
            this.mStatFs.restat(this.mDropBoxDir.getPath());
            int available = this.mStatFs.getAvailableBlocks();
            int nonreserved = available - ((this.mStatFs.getBlockCount() * reservePercent) / 100);
            int maximum = (quotaKb * 1024) / this.mBlockSize;
            this.mCachedQuotaBlocks = Math.min(maximum, Math.max(0, (nonreserved * quotaPercent) / 100));
            this.mCachedQuotaUptimeMillis = uptimeMillis;
        }
        if (this.mAllFiles.blocks > this.mCachedQuotaBlocks) {
            int unsqueezed = this.mAllFiles.blocks;
            int squeezed = 0;
            TreeSet<FileList> tags = new TreeSet<>(this.mFilesByTag.values());
            for (FileList tag2 : tags) {
                if (squeezed > 0 && tag2.blocks <= (this.mCachedQuotaBlocks - unsqueezed) / squeezed) {
                    break;
                }
                unsqueezed -= tag2.blocks;
                squeezed++;
            }
            int tagQuota = (this.mCachedQuotaBlocks - unsqueezed) / squeezed;
            for (FileList tag3 : tags) {
                if (this.mAllFiles.blocks < this.mCachedQuotaBlocks) {
                    break;
                }
                while (tag3.blocks > tagQuota && !tag3.contents.isEmpty()) {
                    EntryFile entry2 = tag3.contents.first();
                    if (tag3.contents.remove(entry2)) {
                        tag3.blocks -= entry2.blocks;
                    }
                    if (this.mAllFiles.contents.remove(entry2)) {
                        this.mAllFiles.blocks -= entry2.blocks;
                    }
                    try {
                        if (entry2.file != null) {
                            entry2.file.delete();
                        }
                        enrollEntry(new EntryFile(this.mDropBoxDir, entry2.tag, entry2.timestampMillis));
                    } catch (IOException e) {
                        Slog.e(TAG, "Can't write tombstone file", e);
                    }
                }
            }
        }
        return this.mCachedQuotaBlocks * this.mBlockSize;
    }
}
