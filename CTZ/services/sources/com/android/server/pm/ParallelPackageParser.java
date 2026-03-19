package com.android.server.pm;

import android.content.pm.PackageParser;
import android.os.Trace;
import android.util.DisplayMetrics;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ConcurrentUtils;
import java.io.File;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

class ParallelPackageParser implements AutoCloseable {
    private static final int MAX_THREADS = 4;
    private static final int QUEUE_CAPACITY = 10;
    private final File mCacheDir;
    private volatile String mInterruptedInThread;
    private final DisplayMetrics mMetrics;
    private final boolean mOnlyCore;
    private final PackageParser.Callback mPackageParserCallback;
    private final String[] mSeparateProcesses;
    private final BlockingQueue<ParseResult> mQueue = new ArrayBlockingQueue(10);
    private final ExecutorService mService = ConcurrentUtils.newFixedThreadPool(4, "package-parsing-thread", -2);

    ParallelPackageParser(String[] strArr, boolean z, DisplayMetrics displayMetrics, File file, PackageParser.Callback callback) {
        this.mSeparateProcesses = strArr;
        this.mOnlyCore = z;
        this.mMetrics = displayMetrics;
        this.mCacheDir = file;
        this.mPackageParserCallback = callback;
    }

    static class ParseResult {
        PackageParser.Package pkg;
        File scanFile;
        Throwable throwable;

        ParseResult() {
        }

        public String toString() {
            return "ParseResult{pkg=" + this.pkg + ", scanFile=" + this.scanFile + ", throwable=" + this.throwable + '}';
        }
    }

    public ParseResult take() {
        try {
            if (this.mInterruptedInThread != null) {
                throw new InterruptedException("Interrupted in " + this.mInterruptedInThread);
            }
            return this.mQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public void submit(final File file, final int i) {
        this.mService.submit(new Runnable() {
            @Override
            public final void run() {
                ParallelPackageParser.lambda$submit$0(this.f$0, file, i);
            }
        });
    }

    public static void lambda$submit$0(ParallelPackageParser parallelPackageParser, File file, int i) {
        ParseResult parseResult = new ParseResult();
        Trace.traceBegin(262144L, "parallel parsePackage [" + file + "]");
        try {
            try {
                PackageParser packageParser = new PackageParser();
                packageParser.setSeparateProcesses(parallelPackageParser.mSeparateProcesses);
                packageParser.setOnlyCoreApps(parallelPackageParser.mOnlyCore);
                packageParser.setDisplayMetrics(parallelPackageParser.mMetrics);
                packageParser.setCacheDir(parallelPackageParser.mCacheDir);
                packageParser.setCallback(parallelPackageParser.mPackageParserCallback);
                parseResult.scanFile = file;
                parseResult.pkg = parallelPackageParser.parsePackage(packageParser, file, i);
            } catch (Throwable th) {
                parseResult.throwable = th;
            }
            try {
                parallelPackageParser.mQueue.put(parseResult);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                parallelPackageParser.mInterruptedInThread = Thread.currentThread().getName();
            }
        } finally {
            Trace.traceEnd(262144L);
        }
    }

    @VisibleForTesting
    protected PackageParser.Package parsePackage(PackageParser packageParser, File file, int i) throws PackageParser.PackageParserException {
        return packageParser.parsePackage(file, i, true);
    }

    @Override
    public void close() {
        List<Runnable> listShutdownNow = this.mService.shutdownNow();
        if (!listShutdownNow.isEmpty()) {
            throw new IllegalStateException("Not all tasks finished before calling close: " + listShutdownNow);
        }
    }
}
