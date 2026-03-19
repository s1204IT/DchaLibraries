package android.media;

import android.app.backup.FullBackup;
import android.net.Uri;
import android.os.BatteryStats;
import android.os.Environment;
import android.os.Trace;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.Adler32;
import libcore.io.IoUtils;

public class MiniThumbFile {
    private static final long AUTHOR = -1058094635;
    public static final int BYTES_PER_MINTHUMB = 16384;
    private static final int DATA_START_OFFSET = 524288;
    private static final int IH_DATA_CHECKSUM_OFFSET = 16;
    private static final int IH_LENGTH_OFFSET = 28;
    private static final int IH_MAGIC_OFFSET = 8;
    private static final int IH_ORIGINAL_ID_OFFSET = 0;
    private static final int IH_POSITION_OFFSET = 24;
    private static final int INDEX_HEADER_SIZE = 32;
    private static final long MAGIC_THUMB_FILE = 538182168;
    private static final int MAX_THUMB_COUNT_PER_FILE = 16383;
    private static final int MAX_THUMB_FILE_SIZE = 52428800;
    private static final int MINI_THUMB_DATA_FILE_VERSION = 7;
    private static final String TAG = "MiniThumbFile";
    private static final int VERSION_HEADER_SIZE = 32;
    private static final int VH_ACTIVECOUNT_OFFSET = 12;
    private static final int VH_AUTHOR_OFFSET = 16;
    private static final int VH_CHECKSUM_OFFSET = 24;
    private static final int VH_MAGIC_OFFSET = 4;
    private static final int VH_VERSION_OFFSET = 0;
    private Uri mUri;
    private static final HashMap<Long, MiniThumbDataFile> sMiniThumbDataFile = new HashMap<>();
    private static MiniThumbFile sMiniThumbFile = null;
    private static Object sLock = new Object();
    private byte[] mVersionHeader = new byte[32];
    private byte[] mIndexHeader = new byte[32];
    private Adler32 mChecker = new Adler32();
    private ByteBuffer mBuffer = ByteBuffer.allocateDirect(16384);
    private ByteBuffer mEmptyBuffer = ByteBuffer.allocateDirect(16384);
    private ByteBuffer mEmptyIndexBuffer = ByteBuffer.allocateDirect(32);

    public static synchronized void reset() {
        if (sMiniThumbFile != null) {
            sMiniThumbFile.deactivate();
        }
        sMiniThumbFile = null;
    }

    public static synchronized MiniThumbFile instance(Uri uri) {
        if (sMiniThumbFile == null) {
            sMiniThumbFile = new MiniThumbFile(null);
        }
        return sMiniThumbFile;
    }

    private static String randomAccessFilePath(long id) {
        String storagePath = Environment.getExternalStorageDirectory().getPath();
        String directoryName = getMiniThumbFileDirectoryPath();
        int fileIndex = ((int) id) / MAX_THUMB_COUNT_PER_FILE;
        String fileName = getMiniThumbFilePrefix() + fileIndex;
        return storagePath + "/" + directoryName + "/" + fileName;
    }

    private MiniThumbDataFile miniThumbDataFile(long id) {
        MiniThumbDataFile miniThumbDataFile;
        synchronized (sLock) {
            long fileIndex = id / 16383;
            miniThumbDataFile = sMiniThumbDataFile.get(Long.valueOf(fileIndex));
            if (miniThumbDataFile == null) {
                String path = randomAccessFilePath(id);
                File directory = new File(path).getParentFile();
                if (!directory.isDirectory() && !directory.mkdirs()) {
                    Log.e(TAG, "Unable to create .thumbnails directory " + directory.toString());
                }
                File file = new File(path);
                try {
                    miniThumbDataFile = new MiniThumbDataFile(new RandomAccessFile(file, "rw"), path);
                } catch (IOException ex) {
                    Log.e(TAG, "miniThumbDataFile: IOException(rw) for: " + path + ", try read only mode", ex);
                    try {
                        miniThumbDataFile = new MiniThumbDataFile(new RandomAccessFile(file, FullBackup.ROOT_TREE_TOKEN), path);
                    } catch (IOException ex2) {
                        Log.e(TAG, "miniThumbDataFile: IOException(r) for: " + path, ex2);
                    }
                }
                if (miniThumbDataFile != null) {
                    sMiniThumbDataFile.put(Long.valueOf(fileIndex), miniThumbDataFile);
                }
            }
        }
        return miniThumbDataFile;
    }

    private MiniThumbFile(Uri uri) {
        this.mUri = uri;
        Log.v(TAG, "activate MiniThumbFile " + this);
    }

    public synchronized void deactivate() {
        synchronized (sLock) {
            Iterator<Map.Entry<Long, MiniThumbDataFile>> iterator = sMiniThumbDataFile.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, MiniThumbDataFile> entry = iterator.next();
                MiniThumbDataFile miniThumbDataFile = entry.getValue();
                if (miniThumbDataFile != null) {
                    miniThumbDataFile.close();
                }
                iterator.remove();
            }
        }
        Log.v(TAG, "deactivate MiniThumbFile " + this);
    }

    public synchronized long getMagic(long id) {
        MiniThumbDataFile miniThumbDataFile = miniThumbDataFile(id);
        if (miniThumbDataFile != null) {
            try {
                try {
                    long magic = miniThumbDataFile.getMagic(id);
                    return magic;
                } catch (RuntimeException ex) {
                    Log.e(TAG, "Got exception when reading magic, id = " + id + ", disk full or mount read-only? " + ex.getClass());
                }
            } catch (IOException ex2) {
                Log.v(TAG, "Got exception checking file magic: ", ex2);
            }
        }
        return 0L;
    }

    public synchronized void eraseMiniThumb(long id) {
        MiniThumbDataFile miniThumbDataFile = miniThumbDataFile(id);
        if (miniThumbDataFile == null) {
            return;
        }
        try {
            Log.v(TAG, "eraseMiniThumb with : id = " + id);
            miniThumbDataFile.eraseMiniThumb(id);
        } catch (IOException ex) {
            Log.e(TAG, "couldn't erase mini thumbnail data for " + id + "; ", ex);
        } catch (RuntimeException ex2) {
            Log.e(TAG, "couldn't erase mini thumbnail data for " + id + "; disk full or mount read-only? " + ex2.getClass());
        }
    }

    public synchronized void saveMiniThumbToFile(byte[] data, long id, long magic) throws IOException {
        MiniThumbDataFile miniThumbDataFile = miniThumbDataFile(id);
        if (miniThumbDataFile == null) {
            return;
        }
        try {
            Log.v(TAG, "saveMiniThumbToFile with : id = " + id + ", magic = " + magic);
            miniThumbDataFile.updateDataToThumbFile(data, id, magic);
        } catch (IOException ex) {
            Log.e(TAG, "couldn't save mini thumbnail data for " + id + "; ", ex);
            throw ex;
        } catch (RuntimeException ex2) {
            Log.e(TAG, "couldn't save mini thumbnail data for " + id + "; disk full or mount read-only? " + ex2.getClass());
        }
    }

    public synchronized byte[] getMiniThumbFromFile(long id, byte[] data) {
        return getMiniThumbFromFile(id, data, null);
    }

    public synchronized byte[] getMiniThumbFromFile(long id, byte[] data, ThumbResult result) {
        MiniThumbDataFile miniThumbDataFile = miniThumbDataFile(id);
        if (miniThumbDataFile == null) {
            return null;
        }
        try {
            Log.v(TAG, "getMiniThumbFromFile for id " + id);
            return miniThumbDataFile.getDataFromThumbFile(data, id, result);
        } catch (IOException ex) {
            Log.w(TAG, "got exception when reading thumbnail id=" + id + ", exception: " + ex);
            return null;
        } catch (RuntimeException ex2) {
            Log.e(TAG, "Got exception when reading thumbnail, id = " + id + ", disk full or mount read-only? " + ex2.getClass());
            return null;
        }
    }

    public static String getMiniThumbFilePrefix() {
        return ".thumbdata-7.0_";
    }

    public static String getMiniThumbFileDirectoryPath() {
        return ".thumbnails";
    }

    public static class ThumbResult {
        public static final int SUCCESS = 2;
        public static final int UNSPECIFIED = 0;
        public static final int WRONG_CHECK_CODE = 1;
        private int mDetail = 0;

        void setDetail(int detail) {
            this.mDetail = detail;
        }

        public int getDetail() {
            return this.mDetail;
        }
    }

    private class MiniThumbDataFile {
        private int mActiveCount;
        private FileChannel mChannel;
        private MappedByteBuffer mIndexMappedBuffer;
        private String mPath;
        private RandomAccessFile mRandomAccessFile;

        public MiniThumbDataFile(RandomAccessFile miniThumbFile, String path) throws IOException {
            this.mRandomAccessFile = miniThumbFile;
            this.mPath = path;
            if (!load()) {
                reset();
            }
            this.mChannel = this.mRandomAccessFile.getChannel();
            try {
                this.mIndexMappedBuffer = this.mChannel.map(FileChannel.MapMode.READ_WRITE, 0L, Trace.TRACE_TAG_POWER);
            } catch (NonWritableChannelException ex1) {
                Log.w(MiniThumbFile.TAG, "map MiniThumbFile(READ_WRITE) with NonWritableChannelException, try READ_ONLY mode", ex1);
                try {
                    this.mIndexMappedBuffer = this.mChannel.map(FileChannel.MapMode.READ_ONLY, 0L, Trace.TRACE_TAG_POWER);
                } catch (NonReadableChannelException e) {
                    throw new IOException("try map as READ_ONLY mode with NonReadableChannelException");
                }
            }
            Log.v(MiniThumbFile.TAG, "Create MiniThumbDataFile with size " + (this.mRandomAccessFile.length() / 1024) + "KB");
        }

        private synchronized boolean load() throws IOException {
            this.mRandomAccessFile.seek(0L);
            if (this.mRandomAccessFile.read(MiniThumbFile.this.mVersionHeader) != 32) {
                Log.w(MiniThumbFile.TAG, "cannot read version header");
                return false;
            }
            if (MiniThumbFile.this.readInt(MiniThumbFile.this.mVersionHeader, 0) != 7) {
                Log.w(MiniThumbFile.TAG, "miss MiniThumbDataFile version");
                return false;
            }
            if (MiniThumbFile.this.readLong(MiniThumbFile.this.mVersionHeader, 4) != MiniThumbFile.MAGIC_THUMB_FILE) {
                Log.w(MiniThumbFile.TAG, "miss MiniThumbDataFile magic");
                return false;
            }
            int i = MiniThumbFile.this.readInt(MiniThumbFile.this.mVersionHeader, 12);
            this.mActiveCount = i;
            if (i >= MiniThumbFile.MAX_THUMB_COUNT_PER_FILE) {
                Log.w(MiniThumbFile.TAG, "active count big than limit, need reset");
                return false;
            }
            if (MiniThumbFile.this.readLong(MiniThumbFile.this.mVersionHeader, 24) != MiniThumbFile.this.checkSum(MiniThumbFile.this.mVersionHeader, 0, 24)) {
                Log.w(MiniThumbFile.TAG, "invalid version check sum, version header may be destoried");
                return false;
            }
            long size = this.mRandomAccessFile.length();
            if (size > 52428800) {
                Log.w(MiniThumbFile.TAG, "MiniThumbDataFile size is big than limit(current size = " + ((size / 1024) / 1024) + "M)");
                return false;
            }
            Log.d(MiniThumbFile.TAG, "load MiniThumbDataFile with active count is " + this.mActiveCount);
            return true;
        }

        private synchronized void reset() throws IOException {
            Log.d(MiniThumbFile.TAG, "reset MiniThumbDataFile " + this.mPath);
            this.mActiveCount = 0;
            this.mRandomAccessFile.setLength(0L);
            this.mRandomAccessFile.setLength(Trace.TRACE_TAG_POWER);
            this.mRandomAccessFile.seek(0L);
            MiniThumbFile.this.writeInt(MiniThumbFile.this.mVersionHeader, 0, 7);
            MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 4, MiniThumbFile.MAGIC_THUMB_FILE);
            MiniThumbFile.this.writeInt(MiniThumbFile.this.mVersionHeader, 12, this.mActiveCount);
            MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 16, MiniThumbFile.AUTHOR);
            MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 24, MiniThumbFile.this.checkSum(MiniThumbFile.this.mVersionHeader, 0, 24));
            this.mRandomAccessFile.write(MiniThumbFile.this.mVersionHeader);
        }

        public synchronized int updateActiveCount() throws IOException {
            int currentActionCount;
            int currentActionCount2 = getActiveCount();
            MiniThumbFile.this.writeInt(MiniThumbFile.this.mVersionHeader, 0, 7);
            MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 4, MiniThumbFile.MAGIC_THUMB_FILE);
            currentActionCount = currentActionCount2 + 1;
            MiniThumbFile.this.writeInt(MiniThumbFile.this.mVersionHeader, 12, currentActionCount);
            MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 16, MiniThumbFile.AUTHOR);
            MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 24, MiniThumbFile.this.checkSum(MiniThumbFile.this.mVersionHeader, 0, 24));
            this.mIndexMappedBuffer.position(0);
            this.mIndexMappedBuffer.put(MiniThumbFile.this.mVersionHeader);
            return currentActionCount;
        }

        public synchronized int getActiveCount() throws IOException {
            this.mIndexMappedBuffer.position(0);
            if (this.mIndexMappedBuffer.get(MiniThumbFile.this.mVersionHeader) != null && MiniThumbFile.this.readLong(MiniThumbFile.this.mVersionHeader, 24) == MiniThumbFile.this.checkSum(MiniThumbFile.this.mVersionHeader, 0, 24)) {
                this.mActiveCount = MiniThumbFile.this.readInt(MiniThumbFile.this.mVersionHeader, 12);
                Log.v(MiniThumbFile.TAG, "getActiveCount is " + this.mActiveCount);
                return this.mActiveCount;
            }
            Log.v(MiniThumbFile.TAG, "invalid version header, reset MiniThumbDataFile");
            reset();
            return this.mActiveCount;
        }

        public synchronized void updateIndexHeader(byte[] header, long id) throws IOException {
            int position = ((((int) id) % MiniThumbFile.MAX_THUMB_COUNT_PER_FILE) * 32) + 32;
            this.mIndexMappedBuffer.position(position);
            this.mIndexMappedBuffer.put(header, 0, 32);
        }

        public synchronized ByteBuffer getIndexHeader(byte[] header, long id) throws IOException {
            int position = ((((int) id) % MiniThumbFile.MAX_THUMB_COUNT_PER_FILE) * 32) + 32;
            this.mIndexMappedBuffer.position(position);
            return this.mIndexMappedBuffer.get(header, 0, 32);
        }

        public synchronized long getMagic(long id) throws IOException {
            getIndexHeader(MiniThumbFile.this.mIndexHeader, id);
            long storedId = MiniThumbFile.this.readLong(MiniThumbFile.this.mIndexHeader, 0);
            long magic = MiniThumbFile.this.readLong(MiniThumbFile.this.mIndexHeader, 8);
            if (storedId == id) {
                Log.v(MiniThumbFile.TAG, "getMagic succuss with: id = " + id + ", magic = " + magic);
                return magic;
            }
            Log.v(MiniThumbFile.TAG, "getMagic fail for id " + id + " with store id is " + storedId);
            return 0L;
        }

        public synchronized void updateDataToThumbFile(byte[] data, long id, long magic) throws IOException {
            int position;
            if (data != null) {
                if (data.length <= 16384) {
                    if (getIndexHeader(MiniThumbFile.this.mIndexHeader, id) != null && MiniThumbFile.this.readLong(MiniThumbFile.this.mIndexHeader, 0) == id) {
                        position = MiniThumbFile.this.readInt(MiniThumbFile.this.mIndexHeader, 24);
                    } else {
                        position = 524288 + (updateActiveCount() * 16384);
                    }
                    MiniThumbFile.this.writeLong(MiniThumbFile.this.mIndexHeader, 0, id);
                    MiniThumbFile.this.writeLong(MiniThumbFile.this.mIndexHeader, 8, magic);
                    MiniThumbFile.this.writeLong(MiniThumbFile.this.mIndexHeader, 16, MiniThumbFile.this.checkSum(data));
                    MiniThumbFile.this.writeInt(MiniThumbFile.this.mIndexHeader, 24, position);
                    MiniThumbFile.this.writeInt(MiniThumbFile.this.mIndexHeader, 28, data.length);
                    updateIndexHeader(MiniThumbFile.this.mIndexHeader, id);
                    MiniThumbFile.this.mBuffer.clear();
                    MiniThumbFile.this.mBuffer.put(data);
                    MiniThumbFile.this.mBuffer.flip();
                    FileLock lock = null;
                    try {
                        lock = this.mChannel.lock(position, 16384L, false);
                        this.mChannel.write(MiniThumbFile.this.mBuffer, position);
                        Log.v(MiniThumbFile.TAG, "updateDataToThumbFile succuss with " + bufferToString(MiniThumbFile.this.mIndexHeader));
                        return;
                    } finally {
                        if (lock != null) {
                            try {
                                lock.release();
                            } catch (IOException ex) {
                                Log.e(MiniThumbFile.TAG, "updateDataToThumbFile: can not release lock!", ex);
                            }
                        }
                    }
                }
            }
            Log.v(MiniThumbFile.TAG, "updateDataToThumbFile with invalid data");
        }

        public synchronized void eraseMiniThumb(long id) throws IOException {
            if (getIndexHeader(MiniThumbFile.this.mIndexHeader, id) != null && MiniThumbFile.this.readLong(MiniThumbFile.this.mIndexHeader, 0) == id) {
                int position = MiniThumbFile.this.readInt(MiniThumbFile.this.mIndexHeader, 24);
                FileLock lock = null;
                try {
                    lock = this.mChannel.lock(position, 16384L, false);
                    this.mChannel.write(MiniThumbFile.this.mEmptyBuffer, position);
                    updateIndexHeader(MiniThumbFile.this.mEmptyIndexBuffer.array(), id);
                    int currentActionCount = getActiveCount();
                    MiniThumbFile.this.writeInt(MiniThumbFile.this.mVersionHeader, 0, 7);
                    MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 4, MiniThumbFile.MAGIC_THUMB_FILE);
                    MiniThumbFile.this.writeInt(MiniThumbFile.this.mVersionHeader, 12, currentActionCount - 1);
                    MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 16, MiniThumbFile.AUTHOR);
                    MiniThumbFile.this.writeLong(MiniThumbFile.this.mVersionHeader, 24, MiniThumbFile.this.checkSum(MiniThumbFile.this.mVersionHeader, 0, 24));
                    this.mIndexMappedBuffer.position(0);
                    this.mIndexMappedBuffer.put(MiniThumbFile.this.mVersionHeader);
                    Log.v(MiniThumbFile.TAG, "updateDataToThumbFile succuss with " + bufferToString(MiniThumbFile.this.mIndexHeader));
                } finally {
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException ex) {
                            Log.e(MiniThumbFile.TAG, "eraseMiniThumb: can not release lock!", ex);
                        }
                    }
                }
            }
        }

        public synchronized byte[] getDataFromThumbFile(byte[] data, long id, ThumbResult result) throws IOException {
            if (getIndexHeader(MiniThumbFile.this.mIndexHeader, id) == null) {
                Log.w(MiniThumbFile.TAG, "can not get index header for id " + id);
                return null;
            }
            long oldId = MiniThumbFile.this.readLong(MiniThumbFile.this.mIndexHeader, 0);
            MiniThumbFile.this.readLong(MiniThumbFile.this.mIndexHeader, 8);
            long dataCheckSum = MiniThumbFile.this.readLong(MiniThumbFile.this.mIndexHeader, 16);
            int position = MiniThumbFile.this.readInt(MiniThumbFile.this.mIndexHeader, 24);
            int length = MiniThumbFile.this.readInt(MiniThumbFile.this.mIndexHeader, 28);
            if (oldId != id) {
                Log.w(MiniThumbFile.TAG, "invalid store original id : store id = " + oldId + ", given id = " + id);
                return null;
            }
            if (data.length < length) {
                Log.w(MiniThumbFile.TAG, "invalid store data length: store length = " + length + ", given length = " + data.length);
                return null;
            }
            try {
                MiniThumbFile.this.mBuffer.clear();
                FileLock lock = this.mChannel.lock(position, 16384L, false);
                if (this.mChannel.read(MiniThumbFile.this.mBuffer, position) >= length) {
                    MiniThumbFile.this.mBuffer.position(0);
                    MiniThumbFile.this.mBuffer.get(data, 0, length);
                    if (dataCheckSum == MiniThumbFile.this.checkSum(data, 0, length)) {
                        if (result != null) {
                            result.setDetail(2);
                        }
                        Log.v(MiniThumbFile.TAG, "getDataFromThumbFile success with " + bufferToString(MiniThumbFile.this.mIndexHeader));
                        if (lock != null) {
                            try {
                                lock.release();
                            } catch (IOException ex) {
                                Log.e(MiniThumbFile.TAG, "getDataFromThumbFile: can not release lock!", ex);
                            }
                        }
                        return data;
                    }
                    if (result != null) {
                        result.setDetail(1);
                    }
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException ex2) {
                            Log.e(MiniThumbFile.TAG, "getDataFromThumbFile: can not release lock!", ex2);
                        }
                    }
                    Log.v(MiniThumbFile.TAG, "getDataFromThumbFile fail with " + bufferToString(MiniThumbFile.this.mIndexHeader));
                    return null;
                }
                if (lock != null) {
                }
                Log.v(MiniThumbFile.TAG, "getDataFromThumbFile fail with " + bufferToString(MiniThumbFile.this.mIndexHeader));
                return null;
            } finally {
            }
        }

        public synchronized void close() {
            Log.v(MiniThumbFile.TAG, "close MiniThumbDataFile " + this.mPath);
            syncAll();
            IoUtils.closeQuietly(this.mRandomAccessFile);
            IoUtils.closeQuietly(this.mChannel);
            this.mRandomAccessFile = null;
            this.mChannel = null;
            this.mIndexMappedBuffer = null;
        }

        public void syncIndex() {
            try {
                this.mIndexMappedBuffer.force();
            } catch (Throwable t) {
                Log.w(MiniThumbFile.TAG, "sync MiniThumbDataFile index failed", t);
            }
        }

        public void syncAll() {
            syncIndex();
            try {
                this.mRandomAccessFile.getFD().sync();
            } catch (Throwable t) {
                Log.w(MiniThumbFile.TAG, "sync MiniThumbDataFile failed", t);
            }
        }

        public String bufferToString(byte[] buffer) {
            StringBuilder builder = new StringBuilder();
            builder.append("id = ").append(MiniThumbFile.this.readLong(buffer, 0));
            builder.append(", magic = ").append(MiniThumbFile.this.readLong(buffer, 8));
            builder.append(", data checksum = ").append(MiniThumbFile.this.readLong(buffer, 16));
            builder.append(", position = ").append(MiniThumbFile.this.readInt(buffer, 24));
            builder.append(", length = ").append(MiniThumbFile.this.readInt(buffer, 28));
            return builder.toString();
        }
    }

    private int readInt(byte[] buf, int offset) {
        return (buf[offset] & BatteryStats.HistoryItem.CMD_NULL) | ((buf[offset + 1] & BatteryStats.HistoryItem.CMD_NULL) << 8) | ((buf[offset + 2] & BatteryStats.HistoryItem.CMD_NULL) << 16) | ((buf[offset + 3] & BatteryStats.HistoryItem.CMD_NULL) << 24);
    }

    private long readLong(byte[] buf, int offset) {
        long result = buf[offset + 7] & BatteryStats.HistoryItem.CMD_NULL;
        for (int i = 6; i >= 0; i--) {
            result = (result << 8) | ((long) (buf[offset + i] & BatteryStats.HistoryItem.CMD_NULL));
        }
        return result;
    }

    private void writeInt(byte[] buf, int offset, int value) {
        for (int i = 0; i < 4; i++) {
            buf[offset + i] = (byte) (value & 255);
            value >>= 8;
        }
    }

    private void writeLong(byte[] buf, int offset, long value) {
        for (int i = 0; i < 8; i++) {
            buf[offset + i] = (byte) (255 & value);
            value >>= 8;
        }
    }

    private long checkSum(byte[] data) {
        this.mChecker.reset();
        this.mChecker.update(data);
        return this.mChecker.getValue();
    }

    private long checkSum(byte[] data, int offset, int length) {
        this.mChecker.reset();
        this.mChecker.update(data, offset, length);
        return this.mChecker.getValue();
    }
}
