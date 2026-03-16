package com.bumptech.glide.load.model.stream;

import com.bumptech.glide.load.data.ByteArrayFetcher;
import com.bumptech.glide.load.data.DataFetcher;
import java.io.InputStream;
import java.util.UUID;

public class StreamByteArrayLoader implements StreamModelLoader<byte[]> {
    private String id;

    public StreamByteArrayLoader() {
        this(UUID.randomUUID().toString());
    }

    public StreamByteArrayLoader(String id) {
        this.id = id;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(byte[] model, int width, int height) {
        return new ByteArrayFetcher(model, this.id);
    }
}
