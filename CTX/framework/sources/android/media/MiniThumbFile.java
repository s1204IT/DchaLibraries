package android.media;

import android.app.backup.FullBackup;
import android.app.job.JobInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import com.android.internal.content.NativeLibraryHelper;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Hashtable;
import java.util.Iterator;

public class MiniThumbFile {
    public static final int BYTES_PER_MINTHUMB = 10000;
    private static final int HEADER_SIZE = 13;
    private static final int MINI_THUMB_DATA_FILE_VERSION = 4;
    private static final String TAG = "MiniThumbFile";
    private static final Hashtable<String, MiniThumbFile> sThumbFiles = new Hashtable<>();
    private FileChannel mChannel;
    private RandomAccessFile mMiniThumbFile;
    private Uri mUri;
    private ByteBuffer mBuffer = ByteBuffer.allocateDirect(10000);
    private ByteBuffer mEmptyBuffer = ByteBuffer.allocateDirect(10000);

    public static synchronized void reset() {
        Iterator<MiniThumbFile> it = sThumbFiles.values().iterator();
        while (it.hasNext()) {
            it.next().deactivate();
        }
        sThumbFiles.clear();
    }

    public static synchronized MiniThumbFile instance(Uri uri) {
        MiniThumbFile miniThumbFile;
        String str = uri.getPathSegments().get(1);
        miniThumbFile = sThumbFiles.get(str);
        if (miniThumbFile == null) {
            miniThumbFile = new MiniThumbFile(Uri.parse("content://media/external/" + str + "/media"));
            sThumbFiles.put(str, miniThumbFile);
        }
        return miniThumbFile;
    }

    private String randomAccessFilePath(int i) {
        return (Environment.getExternalStorageDirectory().toString() + "/DCIM/.thumbnails") + "/.thumbdata" + i + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + this.mUri.hashCode();
    }

    private void removeOldFile() {
        File file = new File(randomAccessFilePath(3));
        if (file.exists()) {
            try {
                file.delete();
            } catch (SecurityException e) {
            }
        }
    }

    private RandomAccessFile miniThumbDataFile() {
        if (this.mMiniThumbFile == null) {
            removeOldFile();
            String strRandomAccessFilePath = randomAccessFilePath(4);
            File parentFile = new File(strRandomAccessFilePath).getParentFile();
            if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
                Log.e(TAG, "Unable to create .thumbnails directory " + parentFile.toString());
            }
            File file = new File(strRandomAccessFilePath);
            try {
                this.mMiniThumbFile = new RandomAccessFile(file, "rw");
            } catch (IOException e) {
                try {
                    this.mMiniThumbFile = new RandomAccessFile(file, FullBackup.ROOT_TREE_TOKEN);
                } catch (IOException e2) {
                }
            }
            if (this.mMiniThumbFile != null) {
                this.mChannel = this.mMiniThumbFile.getChannel();
            }
        }
        return this.mMiniThumbFile;
    }

    private MiniThumbFile(Uri uri) {
        this.mUri = uri;
    }

    public synchronized void deactivate() {
        if (this.mMiniThumbFile != null) {
            try {
                this.mMiniThumbFile.close();
                this.mMiniThumbFile = null;
            } catch (IOException e) {
            }
        }
    }

    public synchronized long getMagic(long j) {
        FileLock fileLockLock;
        if (miniThumbDataFile() != null) {
            long j2 = JobInfo.MIN_BACKOFF_MILLIS * j;
            FileLock fileLock = null;
            try {
                try {
                    try {
                        this.mBuffer.clear();
                        this.mBuffer.limit(9);
                        fileLockLock = this.mChannel.lock(j2, 9L, true);
                    } catch (IOException e) {
                    }
                } catch (IOException e2) {
                    e = e2;
                } catch (RuntimeException e3) {
                    e = e3;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (this.mChannel.read(this.mBuffer, j2) == 9) {
                    this.mBuffer.position(0);
                    if (this.mBuffer.get() == 1) {
                        long j3 = this.mBuffer.getLong();
                        if (fileLockLock != null) {
                            try {
                                fileLockLock.release();
                            } catch (IOException e4) {
                            }
                        }
                        return j3;
                    }
                }
            } catch (IOException e5) {
                e = e5;
                fileLock = fileLockLock;
                Log.v(TAG, "Got exception checking file magic: ", e);
                if (fileLock != null) {
                    fileLock.release();
                }
                return 0L;
            } catch (RuntimeException e6) {
                e = e6;
                fileLock = fileLockLock;
                Log.e(TAG, "Got exception when reading magic, id = " + j + ", disk full or mount read-only? " + e.getClass());
                if (fileLock != null) {
                    fileLock.release();
                }
                return 0L;
            } catch (Throwable th2) {
                th = th2;
                fileLock = fileLockLock;
                if (fileLock != null) {
                    try {
                        fileLock.release();
                    } catch (IOException e7) {
                    }
                }
                throw th;
            }
            if (fileLockLock != null) {
                fileLockLock.release();
            }
        }
        return 0L;
    }

    public synchronized void eraseMiniThumb(long j) {
        FileLock fileLockLock;
        if (miniThumbDataFile() != null) {
            long j2 = JobInfo.MIN_BACKOFF_MILLIS * j;
            FileLock fileLock = null;
            try {
                try {
                    try {
                        this.mBuffer.clear();
                        this.mBuffer.limit(9);
                        fileLockLock = this.mChannel.lock(j2, JobInfo.MIN_BACKOFF_MILLIS, false);
                    } catch (IOException e) {
                    }
                } catch (IOException e2) {
                    e = e2;
                } catch (RuntimeException e3) {
                    e = e3;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                if (this.mChannel.read(this.mBuffer, j2) == 9) {
                    this.mBuffer.position(0);
                    if (this.mBuffer.get() == 1) {
                        if (this.mBuffer.getLong() == 0) {
                            Log.i(TAG, "no thumbnail for id " + j);
                            if (fileLockLock != null) {
                                try {
                                    fileLockLock.release();
                                } catch (IOException e4) {
                                }
                            }
                            return;
                        }
                        this.mChannel.write(this.mEmptyBuffer, j2);
                    }
                }
            } catch (IOException e5) {
                e = e5;
                fileLock = fileLockLock;
                Log.v(TAG, "Got exception checking file magic: ", e);
                if (fileLock != null) {
                    fileLock.release();
                }
            } catch (RuntimeException e6) {
                e = e6;
                fileLock = fileLockLock;
                Log.e(TAG, "Got exception when reading magic, id = " + j + ", disk full or mount read-only? " + e.getClass());
                if (fileLock != null) {
                    fileLock.release();
                }
            } catch (Throwable th2) {
                th = th2;
                fileLock = fileLockLock;
                if (fileLock != null) {
                    try {
                        fileLock.release();
                    } catch (IOException e7) {
                    }
                }
                throw th;
            }
            if (fileLockLock != null) {
                fileLockLock.release();
            }
        }
    }

    public synchronized void saveMiniThumbToFile(byte[] bArr, long j, long j2) throws IOException {
        if (miniThumbDataFile() == null) {
            return;
        }
        long j3 = JobInfo.MIN_BACKOFF_MILLIS * j;
        FileLock fileLock = null;
        try {
            try {
                if (bArr != null) {
                    try {
                    } catch (IOException e) {
                        e = e;
                    } catch (RuntimeException e2) {
                        e = e2;
                    }
                    if (bArr.length > 9987) {
                        return;
                    }
                    this.mBuffer.clear();
                    this.mBuffer.put((byte) 1);
                    this.mBuffer.putLong(j2);
                    this.mBuffer.putInt(bArr.length);
                    this.mBuffer.put(bArr);
                    this.mBuffer.flip();
                    FileLock fileLockLock = this.mChannel.lock(j3, JobInfo.MIN_BACKOFF_MILLIS, false);
                    try {
                        this.mChannel.write(this.mBuffer, j3);
                        fileLock = fileLockLock;
                    } catch (IOException e3) {
                        e = e3;
                    } catch (RuntimeException e4) {
                        e = e4;
                        fileLock = fileLockLock;
                        Log.e(TAG, "couldn't save mini thumbnail data for " + j + "; disk full or mount read-only? " + e.getClass());
                        if (fileLock != null) {
                            fileLock.release();
                        }
                    } catch (Throwable th) {
                        th = th;
                        fileLock = fileLockLock;
                        if (fileLock != null) {
                            try {
                                fileLock.release();
                            } catch (IOException e5) {
                            }
                        }
                        throw th;
                    }
                    if (fileLock != null) {
                        fileLock.release();
                    }
                    Log.e(TAG, "couldn't save mini thumbnail data for " + j + "; ", e);
                    throw e;
                }
                if (fileLock != null) {
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e6) {
        }
    }

    public synchronized byte[] getMiniThumbFromFile(long j, byte[] bArr) {
        FileLock fileLockLock;
        FileLock fileLockMiniThumbDataFile = miniThumbDataFile();
        if (fileLockMiniThumbDataFile == 0) {
            return null;
        }
        long j2 = JobInfo.MIN_BACKOFF_MILLIS * j;
        try {
        } catch (Throwable th) {
            th = th;
        }
        try {
            try {
                this.mBuffer.clear();
                fileLockLock = this.mChannel.lock(j2, JobInfo.MIN_BACKOFF_MILLIS, true);
            } catch (IOException e) {
            }
        } catch (IOException e2) {
            e = e2;
            fileLockLock = null;
        } catch (RuntimeException e3) {
            e = e3;
            fileLockLock = null;
        } catch (Throwable th2) {
            th = th2;
            fileLockMiniThumbDataFile = 0;
            if (fileLockMiniThumbDataFile != 0) {
                try {
                    fileLockMiniThumbDataFile.release();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
        try {
            int i = this.mChannel.read(this.mBuffer, j2);
            if (i > 13) {
                this.mBuffer.position(0);
                byte b = this.mBuffer.get();
                long j3 = this.mBuffer.getLong();
                int i2 = this.mBuffer.getInt();
                if (i >= 13 + i2 && i2 != 0 && j3 != 0 && b == 1 && bArr.length >= i2) {
                    this.mBuffer.get(bArr, 0, i2);
                    if (fileLockLock != null) {
                        try {
                            fileLockLock.release();
                        } catch (IOException e5) {
                        }
                    }
                    return bArr;
                }
            }
        } catch (IOException e6) {
            e = e6;
            Log.w(TAG, "got exception when reading thumbnail id=" + j + ", exception: " + e);
            if (fileLockLock != null) {
                fileLockLock.release();
            }
            return null;
        } catch (RuntimeException e7) {
            e = e7;
            Log.e(TAG, "Got exception when reading thumbnail, id = " + j + ", disk full or mount read-only? " + e.getClass());
            if (fileLockLock != null) {
                fileLockLock.release();
            }
            return null;
        }
        if (fileLockLock != null) {
            fileLockLock.release();
        }
        return null;
    }
}
