package com.bumptech.glide.load.model;

import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.util.ByteArrayPool;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamEncoder implements Encoder<InputStream> {
    @Override
    public boolean encode(InputStream data, OutputStream os) {
        byte[] buffer = ByteArrayPool.get().getBytes();
        while (true) {
            try {
                int read = data.read(buffer);
                if (read != -1) {
                    os.write(buffer, 0, read);
                } else {
                    ByteArrayPool.get().releaseBytes(buffer);
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public String getId() {
        return "";
    }
}
