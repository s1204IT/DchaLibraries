package com.android.server.pm;

import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.android.server.pm.ShortcutService;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class ShortcutBitmapSaver {
    private static final boolean ADD_DELAY_BEFORE_SAVE_FOR_TEST = false;
    private static final boolean DEBUG = false;
    private static final long SAVE_DELAY_MS_FOR_TEST = 1000;
    private static final String TAG = "ShortcutService";
    private final long SAVE_WAIT_TIMEOUT_MS = 30000;
    private final Executor mExecutor = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue());

    @GuardedBy("mPendingItems")
    private final Deque<PendingItem> mPendingItems = new LinkedBlockingDeque();
    private final Runnable mRunnable = new Runnable() {
        @Override
        public final void run() {
            ShortcutBitmapSaver.lambda$new$1(this.f$0);
        }
    };
    private final ShortcutService mService;

    private static class PendingItem {
        public final byte[] bytes;
        private final long mInstantiatedUptimeMillis;
        public final ShortcutInfo shortcut;

        private PendingItem(ShortcutInfo shortcutInfo, byte[] bArr) {
            this.shortcut = shortcutInfo;
            this.bytes = bArr;
            this.mInstantiatedUptimeMillis = SystemClock.uptimeMillis();
        }

        public String toString() {
            return "PendingItem{size=" + this.bytes.length + " age=" + (SystemClock.uptimeMillis() - this.mInstantiatedUptimeMillis) + "ms shortcut=" + this.shortcut.toInsecureString() + "}";
        }
    }

    public ShortcutBitmapSaver(ShortcutService shortcutService) {
        this.mService = shortcutService;
    }

    public boolean waitForAllSavesLocked() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        this.mExecutor.execute(new Runnable() {
            @Override
            public final void run() {
                countDownLatch.countDown();
            }
        });
        try {
            if (countDownLatch.await(30000L, TimeUnit.MILLISECONDS)) {
                return true;
            }
            this.mService.wtf("Timed out waiting on saving bitmaps.");
            return false;
        } catch (InterruptedException e) {
            Slog.w(TAG, "interrupted");
            return false;
        }
    }

    public String getBitmapPathMayWaitLocked(ShortcutInfo shortcutInfo) {
        if (waitForAllSavesLocked() && shortcutInfo.hasIconFile()) {
            return shortcutInfo.getBitmapPath();
        }
        return null;
    }

    public void removeIcon(ShortcutInfo shortcutInfo) {
        shortcutInfo.setIconResourceId(0);
        shortcutInfo.setIconResName(null);
        shortcutInfo.setBitmapPath(null);
        shortcutInfo.clearFlags(2572);
    }

    public void saveBitmapLocked(ShortcutInfo shortcutInfo, int i, Bitmap.CompressFormat compressFormat, int i2) {
        Icon icon = shortcutInfo.getIcon();
        Preconditions.checkNotNull(icon);
        Bitmap bitmap = icon.getBitmap();
        if (bitmap == null) {
            Log.e(TAG, "Missing icon: " + shortcutInfo);
            return;
        }
        StrictMode.ThreadPolicy threadPolicy = StrictMode.getThreadPolicy();
        try {
            try {
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(threadPolicy).permitCustomSlowCalls().build());
                ShortcutService shortcutService = this.mService;
                Bitmap bitmapShrinkBitmap = ShortcutService.shrinkBitmap(bitmap, i);
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(65536);
                    Object[] objArr = null;
                    Object[] objArr2 = 0;
                    Object[] objArr3 = 0;
                    try {
                        if (!bitmapShrinkBitmap.compress(compressFormat, i2, byteArrayOutputStream)) {
                            Slog.wtf(TAG, "Unable to compress bitmap");
                        }
                        byteArrayOutputStream.flush();
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        byteArrayOutputStream.close();
                        byteArrayOutputStream.close();
                        StrictMode.setThreadPolicy(threadPolicy);
                        shortcutInfo.addFlags(2056);
                        if (icon.getType() == 5) {
                            shortcutInfo.addFlags(512);
                        }
                        PendingItem pendingItem = new PendingItem(shortcutInfo, byteArray);
                        synchronized (this.mPendingItems) {
                            this.mPendingItems.add(pendingItem);
                        }
                        this.mExecutor.execute(this.mRunnable);
                    } finally {
                    }
                } finally {
                    if (bitmapShrinkBitmap != bitmap) {
                        bitmapShrinkBitmap.recycle();
                    }
                }
            } catch (IOException | OutOfMemoryError | RuntimeException e) {
                Slog.wtf(TAG, "Unable to write bitmap to file", e);
                StrictMode.setThreadPolicy(threadPolicy);
            }
        } catch (Throwable th) {
            StrictMode.setThreadPolicy(threadPolicy);
            throw th;
        }
    }

    public static void lambda$new$1(ShortcutBitmapSaver shortcutBitmapSaver) {
        while (shortcutBitmapSaver.processPendingItems()) {
        }
    }

    private boolean processPendingItems() throws Throwable {
        ShortcutInfo shortcutInfo;
        Throwable th;
        Throwable e;
        File file = null;
        try {
            synchronized (this.mPendingItems) {
                if (this.mPendingItems.size() == 0) {
                    return false;
                }
                PendingItem pendingItemPop = this.mPendingItems.pop();
                shortcutInfo = pendingItemPop.shortcut;
                try {
                    if (!shortcutInfo.isIconPendingSave()) {
                        if (shortcutInfo != null) {
                            if (shortcutInfo.getBitmapPath() == null) {
                                removeIcon(shortcutInfo);
                            }
                            shortcutInfo.clearFlags(2048);
                        }
                        return true;
                    }
                    try {
                        try {
                            ShortcutService.FileOutputStreamWithPath fileOutputStreamWithPathOpenIconFileForWrite = this.mService.openIconFileForWrite(shortcutInfo.getUserId(), shortcutInfo);
                            File file2 = fileOutputStreamWithPathOpenIconFileForWrite.getFile();
                            try {
                                fileOutputStreamWithPathOpenIconFileForWrite.write(pendingItemPop.bytes);
                                IoUtils.closeQuietly(fileOutputStreamWithPathOpenIconFileForWrite);
                                shortcutInfo.setBitmapPath(file2.getAbsolutePath());
                                if (shortcutInfo != null) {
                                    if (shortcutInfo.getBitmapPath() == null) {
                                        removeIcon(shortcutInfo);
                                    }
                                    shortcutInfo.clearFlags(2048);
                                }
                                return true;
                            } catch (Throwable th2) {
                                IoUtils.closeQuietly(fileOutputStreamWithPathOpenIconFileForWrite);
                                throw th2;
                            }
                        } catch (IOException | RuntimeException e2) {
                            e = e2;
                            Slog.e(TAG, "Unable to write bitmap to file", e);
                            if (0 != 0 && file.exists()) {
                                file.delete();
                            }
                            if (shortcutInfo != null) {
                                if (shortcutInfo.getBitmapPath() == null) {
                                    removeIcon(shortcutInfo);
                                }
                                shortcutInfo.clearFlags(2048);
                            }
                            return true;
                        }
                    } catch (IOException | RuntimeException e3) {
                        e = e3;
                        Slog.e(TAG, "Unable to write bitmap to file", e);
                        if (0 != 0) {
                            file.delete();
                        }
                        if (shortcutInfo != null) {
                        }
                        return true;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    if (shortcutInfo != null) {
                        if (shortcutInfo.getBitmapPath() == null) {
                            removeIcon(shortcutInfo);
                        }
                        shortcutInfo.clearFlags(2048);
                    }
                    throw th;
                }
            }
        } catch (Throwable th4) {
            shortcutInfo = null;
            th = th4;
        }
    }

    public void dumpLocked(PrintWriter printWriter, String str) {
        synchronized (this.mPendingItems) {
            int size = this.mPendingItems.size();
            printWriter.print(str);
            printWriter.println("Pending saves: Num=" + size + " Executor=" + this.mExecutor);
            for (PendingItem pendingItem : this.mPendingItems) {
                printWriter.print(str);
                printWriter.print("  ");
                printWriter.println(pendingItem);
            }
        }
    }
}
