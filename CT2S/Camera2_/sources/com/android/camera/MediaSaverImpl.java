package com.android.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import java.io.File;

public class MediaSaverImpl implements MediaSaver {
    private static final int SAVE_TASK_MEMORY_LIMIT = 20971520;
    private static final Log.Tag TAG = new Log.Tag("MediaSaverImpl");
    private static final String VIDEO_BASE_URI = "content://media/external/video/media";
    private long mMemoryUse = 0;
    private MediaSaver.QueueListener mQueueListener;

    static long access$022(MediaSaverImpl x0, long x1) {
        long j = x0.mMemoryUse - x1;
        x0.mMemoryUse = j;
        return j;
    }

    @Override
    public boolean isQueueFull() {
        return this.mMemoryUse >= 20971520;
    }

    @Override
    public void addImage(byte[] data, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l, ContentResolver resolver) {
        if (isQueueFull()) {
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        ImageSaveTask t = new ImageSaveTask(data, title, date, loc == null ? null : new Location(loc), width, height, orientation, exif, resolver, l);
        this.mMemoryUse += (long) data.length;
        if (isQueueFull()) {
            onQueueFull();
        }
        t.execute(new Void[0]);
    }

    @Override
    public void addImage(byte[] data, String title, long date, Location loc, int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l, ContentResolver resolver) {
        addImage(data, title, date, loc, 0, 0, orientation, exif, l, resolver);
    }

    @Override
    public void addImage(byte[] data, String title, Location loc, int width, int height, int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener l, ContentResolver resolver) {
        addImage(data, title, System.currentTimeMillis(), loc, width, height, orientation, exif, l, resolver);
    }

    @Override
    public void addVideo(String path, ContentValues values, MediaSaver.OnMediaSavedListener l, ContentResolver resolver) {
        new VideoSaveTask(path, values, l, resolver).execute(new Void[0]);
    }

    @Override
    public void setQueueListener(MediaSaver.QueueListener l) {
        this.mQueueListener = l;
        if (l != null) {
            l.onQueueStatus(isQueueFull());
        }
    }

    private void onQueueFull() {
        if (this.mQueueListener != null) {
            this.mQueueListener.onQueueStatus(true);
        }
    }

    private void onQueueAvailable() {
        if (this.mQueueListener != null) {
            this.mQueueListener.onQueueStatus(false);
        }
    }

    private class ImageSaveTask extends AsyncTask<Void, Void, Uri> {
        private final byte[] data;
        private final long date;
        private final ExifInterface exif;
        private int height;
        private final MediaSaver.OnMediaSavedListener listener;
        private final Location loc;
        private final int orientation;
        private final ContentResolver resolver;
        private final String title;
        private int width;

        public ImageSaveTask(byte[] data, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, ContentResolver resolver, MediaSaver.OnMediaSavedListener listener) {
            this.data = data;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Uri doInBackground(Void... v) {
            if (this.width == 0 || this.height == 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(this.data, 0, this.data.length, options);
                this.width = options.outWidth;
                this.height = options.outHeight;
            }
            return Storage.addImage(this.resolver, this.title, this.date, this.loc, this.orientation, this.exif, this.data, this.width, this.height);
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (this.listener != null && uri != null) {
                this.listener.onMediaSaved(uri);
            }
            boolean previouslyFull = MediaSaverImpl.this.isQueueFull();
            MediaSaverImpl.access$022(MediaSaverImpl.this, this.data.length);
            if (MediaSaverImpl.this.isQueueFull() != previouslyFull) {
                MediaSaverImpl.this.onQueueAvailable();
            }
        }
    }

    private class VideoSaveTask extends AsyncTask<Void, Void, Uri> {
        private final MediaSaver.OnMediaSavedListener listener;
        private String path;
        private final ContentResolver resolver;
        private final ContentValues values;

        public VideoSaveTask(String path, ContentValues values, MediaSaver.OnMediaSavedListener l, ContentResolver r) {
            this.path = path;
            this.values = new ContentValues(values);
            this.listener = l;
            this.resolver = r;
        }

        @Override
        protected Uri doInBackground(Void... v) {
            Uri uri = null;
            try {
                Uri videoTable = Uri.parse(MediaSaverImpl.VIDEO_BASE_URI);
                uri = this.resolver.insert(videoTable, this.values);
                String finalName = this.values.getAsString("_data");
                File finalFile = new File(finalName);
                if (new File(this.path).renameTo(finalFile)) {
                    this.path = finalName;
                }
                this.resolver.update(uri, this.values, null, null);
                return uri;
            } catch (Exception e) {
                Log.e(MediaSaverImpl.TAG, "failed to add video to media store", e);
                uri = null;
                return uri;
            } finally {
                Log.v(MediaSaverImpl.TAG, "Current video URI: " + uri);
            }
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (this.listener != null) {
                this.listener.onMediaSaved(uri);
            }
        }
    }
}
