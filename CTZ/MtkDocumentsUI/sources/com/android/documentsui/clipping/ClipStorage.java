package com.android.documentsui.clipping;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import com.android.documentsui.base.Files;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public final class ClipStorage implements ClipStore {
    static final boolean $assertionsDisabled = false;
    static final int NUM_OF_SLOTS = 20;
    private int mNextSlot;
    private final File mOutDir;
    private final SharedPreferences mPref;
    private final File[] mSlots = new File[NUM_OF_SLOTS];
    private static final long STALENESS_THRESHOLD = TimeUnit.DAYS.toMillis(2);
    private static final byte[] LINE_SEPARATOR = System.lineSeparator().getBytes();

    public ClipStorage(File file, SharedPreferences sharedPreferences) {
        this.mOutDir = file;
        this.mPref = sharedPreferences;
        this.mNextSlot = this.mPref.getInt("NextAvailableSlot", 0);
    }

    synchronized int claimStorageSlot() {
        int i;
        i = this.mNextSlot;
        int i2 = 0;
        while (i2 < NUM_OF_SLOTS) {
            createSlotFileObject(i);
            if (this.mSlots[i].exists() && this.mSlots[i].list().length > 1 && !checkStaleFiles(i)) {
                i2++;
                i = (i + 1) % NUM_OF_SLOTS;
            }
        }
        prepareSlot(i);
        this.mNextSlot = (i + 1) % NUM_OF_SLOTS;
        this.mPref.edit().putInt("NextAvailableSlot", this.mNextSlot).commit();
        return i;
    }

    private boolean checkStaleFiles(int i) {
        if (toSlotDataFile(i).lastModified() + STALENESS_THRESHOLD <= System.currentTimeMillis()) {
            return true;
        }
        return $assertionsDisabled;
    }

    private void prepareSlot(int i) {
        Files.deleteRecursively(this.mSlots[i]);
        this.mSlots[i].mkdir();
    }

    private Writer createWriter(int i) throws IOException {
        return new Writer(toSlotDataFile(i));
    }

    @Override
    public synchronized File getFile(int i) throws IOException {
        File file;
        createSlotFileObject(i);
        File slotDataFile = toSlotDataFile(i);
        file = new File(this.mSlots[i], Integer.toString(this.mSlots[i].list().length));
        try {
            Os.symlink(slotDataFile.getAbsolutePath(), file.getAbsolutePath());
        } catch (ErrnoException e) {
            e.rethrowAsIOException();
        }
        return file;
    }

    @Override
    public ClipStorageReader createReader(File file) throws IOException {
        return new ClipStorageReader(file);
    }

    private File toSlotDataFile(int i) {
        return new File(this.mSlots[i], "primary");
    }

    private void createSlotFileObject(int i) {
        if (this.mSlots[i] == null) {
            this.mSlots[i] = new File(this.mOutDir, Integer.toString(i));
        }
    }

    public static File prepareStorage(File file) {
        File clipDir = getClipDir(file);
        clipDir.mkdir();
        return clipDir;
    }

    private static File getClipDir(File file) {
        return new File(file, "clippings");
    }

    public static final class Writer implements Closeable {
        static final boolean $assertionsDisabled = false;
        private final FileLock mLock;
        private final FileOutputStream mOut;

        private Writer(File file) throws IOException {
            this.mOut = new FileOutputStream(file);
            this.mLock = this.mOut.getChannel().lock();
        }

        public void write(Uri uri) throws IOException {
            this.mOut.write(uri.toString().getBytes());
            this.mOut.write(ClipStorage.LINE_SEPARATOR);
        }

        @Override
        public void close() throws IOException {
            if (this.mLock != null) {
                this.mLock.release();
            }
            if (this.mOut != null) {
                this.mOut.close();
            }
        }
    }

    @Override
    public int persistUris(Iterable<Uri> iterable) {
        int iClaimStorageSlot = claimStorageSlot();
        persistUris(iterable, iClaimStorageSlot);
        return iClaimStorageSlot;
    }

    void persistUris(Iterable<Uri> iterable, int i) {
        new PersistTask(this, iterable, i).execute(new Void[0]);
    }

    private static final class PersistTask extends AsyncTask<Void, Void, Void> {
        static final boolean $assertionsDisabled = false;
        private final ClipStorage mClipStore;
        private final int mSlot;
        private final Iterable<Uri> mUris;

        PersistTask(ClipStorage clipStorage, Iterable<Uri> iterable, int i) {
            this.mClipStore = clipStorage;
            this.mUris = iterable;
            this.mSlot = i;
        }

        @Override
        protected Void doInBackground(Void... voidArr) throws Throwable {
            Writer writerCreateWriter;
            Throwable th;
            Throwable th2;
            try {
                writerCreateWriter = this.mClipStore.createWriter(this.mSlot);
            } catch (IOException e) {
                Log.e("ClipStorage", "Caught exception trying to write jumbo clip to disk.", e);
            }
            try {
                Iterator<Uri> it = this.mUris.iterator();
                while (it.hasNext()) {
                    writerCreateWriter.write(it.next());
                }
                if (writerCreateWriter != null) {
                    writerCreateWriter.close();
                }
                return null;
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    if (writerCreateWriter != null) {
                        throw th2;
                    }
                    if (th == null) {
                        writerCreateWriter.close();
                        throw th2;
                    }
                    try {
                        writerCreateWriter.close();
                        throw th2;
                    } catch (Throwable th5) {
                        th.addSuppressed(th5);
                        throw th2;
                    }
                    return null;
                }
            }
        }
    }
}
