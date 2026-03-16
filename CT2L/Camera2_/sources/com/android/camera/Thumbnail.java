package com.android.camera;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import java.io.FileDescriptor;
import java.io.IOException;

public class Thumbnail {
    public static Bitmap createVideoThumbnailBitmap(FileDescriptor fd, int targetWidth) {
        return createVideoThumbnailBitmap(null, fd, targetWidth);
    }

    public static Bitmap createVideoThumbnailBitmap(String filePath, int targetWidth) {
        return createVideoThumbnailBitmap(filePath, null, targetWidth);
    }

    private static Bitmap createVideoThumbnailBitmap(String filePath, FileDescriptor fd, int targetWidth) throws IOException {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (filePath != null) {
                retriever.setDataSource(filePath);
            } else {
                retriever.setDataSource(fd);
            }
            bitmap = retriever.getFrameAtTime(-1L);
            try {
                retriever.release();
            } catch (RuntimeException e) {
            }
        } catch (IllegalArgumentException e2) {
            try {
                retriever.release();
            } catch (RuntimeException e3) {
            }
        } catch (RuntimeException e4) {
            try {
                retriever.release();
            } catch (RuntimeException e5) {
            }
        } catch (Throwable th) {
            try {
                retriever.release();
            } catch (RuntimeException e6) {
            }
            throw th;
        }
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > targetWidth) {
            float scale = targetWidth / width;
            int w = Math.round(width * scale);
            int h = Math.round(height * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        }
        return bitmap;
    }
}
