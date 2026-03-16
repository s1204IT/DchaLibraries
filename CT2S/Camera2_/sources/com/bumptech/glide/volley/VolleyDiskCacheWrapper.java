package com.bumptech.glide.volley;

import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import com.android.volley.Cache;
import com.android.volley.toolbox.ByteArrayPool;
import com.android.volley.toolbox.PoolingByteArrayOutputStream;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.StringKey;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class VolleyDiskCacheWrapper implements Cache {
    private static final int BYTE_POOL_SIZE = 2097152;
    private static final int CACHE_MAGIC = 538051844;
    private static final int DEFAULT_BYTE_ARRAY_SIZE = 8192;
    private static final String TAG = "VolleyDiskCacheWrapper";
    private final ByteArrayPool byteArrayPool = new ByteArrayPool(2097152);
    private final DiskCache diskCache;

    public VolleyDiskCacheWrapper(DiskCache diskCache) {
        this.diskCache = diskCache;
    }

    @Override
    public Cache.Entry get(String key) {
        Cache.Entry cacheEntry = null;
        InputStream result = this.diskCache.get(new StringKey(key));
        if (result != null) {
            try {
                try {
                    CacheHeader header = readHeader(result);
                    byte[] data = streamToBytes(result);
                    cacheEntry = header.toCacheEntry(data);
                } catch (IOException e) {
                    if (Log.isLoggable(TAG, 3)) {
                        e.printStackTrace();
                    }
                    this.diskCache.delete(new StringKey(key));
                    try {
                        result.close();
                    } catch (IOException e2) {
                    }
                }
            } finally {
                try {
                    result.close();
                } catch (IOException e3) {
                }
            }
        }
        return cacheEntry;
    }

    @Override
    public void put(final String key, final Cache.Entry entry) {
        this.diskCache.put(new StringKey(key), new DiskCache.Writer() {
            @Override
            public boolean write(OutputStream os) {
                CacheHeader header = VolleyDiskCacheWrapper.this.new CacheHeader(key, entry);
                boolean success = header.writeHeader(os);
                if (success) {
                    try {
                        os.write(entry.data);
                    } catch (IOException e) {
                        success = false;
                        if (Log.isLoggable(VolleyDiskCacheWrapper.TAG, 3)) {
                            Log.d(VolleyDiskCacheWrapper.TAG, "Unable to write data", e);
                        }
                    }
                }
                return success;
            }
        });
    }

    @Override
    public void initialize() {
    }

    @Override
    public void invalidate(String key, boolean fullExpire) {
        Cache.Entry entry = get(key);
        if (entry != null) {
            entry.softTtl = 0L;
            if (fullExpire) {
                entry.ttl = 0L;
            }
            put(key, entry);
        }
    }

    @Override
    public void remove(String key) {
        this.diskCache.delete(new StringKey(key));
    }

    @Override
    public void clear() {
    }

    public CacheHeader readHeader(InputStream is) throws IOException {
        CacheHeader entry = new CacheHeader();
        int magic = readInt(is);
        if (magic != CACHE_MAGIC) {
            throw new IOException();
        }
        entry.key = readString(is);
        entry.etag = readString(is);
        if (entry.etag.equals("")) {
            entry.etag = null;
        }
        entry.serverDate = readLong(is);
        entry.ttl = readLong(is);
        entry.softTtl = readLong(is);
        entry.responseHeaders = readStringStringMap(is);
        return entry;
    }

    class CacheHeader {
        public String etag;
        public String key;
        public Map<String, String> responseHeaders;
        public long serverDate;
        public long size;
        public long softTtl;
        public long ttl;

        private CacheHeader() {
        }

        public CacheHeader(String key, Cache.Entry entry) {
            this.key = key;
            this.size = entry.data.length;
            this.etag = entry.etag;
            this.serverDate = entry.serverDate;
            this.ttl = entry.ttl;
            this.softTtl = entry.softTtl;
            this.responseHeaders = entry.responseHeaders;
        }

        public Cache.Entry toCacheEntry(byte[] data) {
            Cache.Entry e = new Cache.Entry();
            e.data = data;
            e.etag = this.etag;
            e.serverDate = this.serverDate;
            e.ttl = this.ttl;
            e.softTtl = this.softTtl;
            e.responseHeaders = this.responseHeaders;
            return e;
        }

        public boolean writeHeader(OutputStream os) {
            try {
                VolleyDiskCacheWrapper.writeInt(os, VolleyDiskCacheWrapper.CACHE_MAGIC);
                VolleyDiskCacheWrapper.writeString(os, this.key);
                VolleyDiskCacheWrapper.writeString(os, this.etag == null ? "" : this.etag);
                VolleyDiskCacheWrapper.writeLong(os, this.serverDate);
                VolleyDiskCacheWrapper.writeLong(os, this.ttl);
                VolleyDiskCacheWrapper.writeLong(os, this.softTtl);
                VolleyDiskCacheWrapper.writeStringStringMap(this.responseHeaders, os);
                os.flush();
                return true;
            } catch (IOException e) {
                if (Log.isLoggable(VolleyDiskCacheWrapper.TAG, 3)) {
                    Log.d("%s", e.toString());
                }
                return false;
            }
        }
    }

    private static int read(InputStream is) throws IOException {
        int b = is.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    static void writeInt(OutputStream os, int n) throws IOException {
        os.write((n >> 0) & MotionEventCompat.ACTION_MASK);
        os.write((n >> 8) & MotionEventCompat.ACTION_MASK);
        os.write((n >> 16) & MotionEventCompat.ACTION_MASK);
        os.write((n >> 24) & MotionEventCompat.ACTION_MASK);
    }

    static int readInt(InputStream is) throws IOException {
        int n = 0 | (read(is) << 0);
        return n | (read(is) << 8) | (read(is) << 16) | (read(is) << 24);
    }

    static void writeLong(OutputStream os, long n) throws IOException {
        os.write((byte) (n >>> 0));
        os.write((byte) (n >>> 8));
        os.write((byte) (n >>> 16));
        os.write((byte) (n >>> 24));
        os.write((byte) (n >>> 32));
        os.write((byte) (n >>> 40));
        os.write((byte) (n >>> 48));
        os.write((byte) (n >>> 56));
    }

    static long readLong(InputStream is) throws IOException {
        long n = 0 | ((((long) read(is)) & 255) << 0);
        return n | ((((long) read(is)) & 255) << 8) | ((((long) read(is)) & 255) << 16) | ((((long) read(is)) & 255) << 24) | ((((long) read(is)) & 255) << 32) | ((((long) read(is)) & 255) << 40) | ((((long) read(is)) & 255) << 48) | ((((long) read(is)) & 255) << 56);
    }

    static void writeString(OutputStream os, String s) throws IOException {
        byte[] b = s.getBytes("UTF-8");
        writeLong(os, b.length);
        os.write(b, 0, b.length);
    }

    String readString(InputStream is) throws IOException {
        int n = (int) readLong(is);
        byte[] b = streamToBytes(is, n, this.byteArrayPool.getBuf(n));
        String result = new String(b, "UTF-8");
        this.byteArrayPool.returnBuf(b);
        return result;
    }

    static void writeStringStringMap(Map<String, String> map, OutputStream os) throws IOException {
        if (map != null) {
            writeInt(os, map.size());
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writeString(os, entry.getKey());
                writeString(os, entry.getValue());
            }
            return;
        }
        writeInt(os, 0);
    }

    Map<String, String> readStringStringMap(InputStream is) throws IOException {
        int size = readInt(is);
        Map<String, String> result = size == 0 ? Collections.emptyMap() : new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            String key = readString(is).intern();
            String value = readString(is).intern();
            result.put(key, value);
        }
        return result;
    }

    private static byte[] streamToBytes(InputStream in, int length, byte[] bytes) throws IOException {
        int pos = 0;
        while (pos < length) {
            int count = in.read(bytes, pos, length - pos);
            if (count == -1) {
                break;
            }
            pos += count;
        }
        if (pos != length) {
            throw new IOException("Expected " + length + " bytes, read " + pos + " bytes");
        }
        return bytes;
    }

    private byte[] streamToBytes(InputStream in) throws IOException {
        PoolingByteArrayOutputStream outputStream = new PoolingByteArrayOutputStream(this.byteArrayPool);
        byte[] bytes = this.byteArrayPool.getBuf(8192);
        while (in.read(bytes, 0, bytes.length - 0) != -1) {
            outputStream.write(bytes);
        }
        this.byteArrayPool.returnBuf(bytes);
        byte[] result = outputStream.toByteArray();
        outputStream.close();
        return result;
    }
}
