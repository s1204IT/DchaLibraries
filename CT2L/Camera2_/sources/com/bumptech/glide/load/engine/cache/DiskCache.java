package com.bumptech.glide.load.engine.cache;

import com.bumptech.glide.load.Key;
import java.io.InputStream;
import java.io.OutputStream;

public interface DiskCache {

    public interface Writer {
        boolean write(OutputStream outputStream);
    }

    void delete(Key key);

    InputStream get(Key key);

    void put(Key key, Writer writer);
}
