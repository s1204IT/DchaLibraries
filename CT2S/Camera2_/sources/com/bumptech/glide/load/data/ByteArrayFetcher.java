package com.bumptech.glide.load.data;

import com.bumptech.glide.Priority;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteArrayFetcher implements DataFetcher<InputStream> {
    private final byte[] bytes;
    private final String id;

    public ByteArrayFetcher(byte[] bytes, String id) {
        this.bytes = bytes;
        this.id = id;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        return new ByteArrayInputStream(this.bytes);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void cancel() {
    }
}
