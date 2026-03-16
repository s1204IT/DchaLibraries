package com.bumptech.glide.load.resource.gif;

import android.util.Log;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.engine.Resource;
import java.io.IOException;
import java.io.OutputStream;

public class GifResourceEncoder implements ResourceEncoder<GifData> {
    private static final String TAG = "GifEncoder";

    @Override
    public boolean encode(Resource<GifData> resource, OutputStream os) {
        try {
            os.write(resource.get().getData());
            return true;
        } catch (IOException e) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "Failed to encode gif", e);
            }
            return false;
        }
    }

    @Override
    public String getId() {
        return "GifResourceEncoder.com.bumptech.glide.load.resource.gif";
    }
}
