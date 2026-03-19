package com.mediatek.camera.common.storage;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.photo.HeifHelper;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MediaSaver {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MediaSaver.class.getSimpleName());
    private static final String TEMP_SUFFIX = ".tmp";
    private final ContentResolver mContentResolver;
    private SaveTask mSaveTask;
    private final List<Request> mSaveQueue = new LinkedList();
    private List<MediaSaverListener> mMediaSaverListeners = new ArrayList();

    public interface MediaSaverListener {
        void onFileSaved(Uri uri);
    }

    public void addMediaSaverListener(MediaSaverListener mediaSaverListener) {
        this.mMediaSaverListeners.add(mediaSaverListener);
    }

    public MediaSaver(Activity activity) {
        this.mContentResolver = activity.getContentResolver();
    }

    public void addSaveRequest(byte[] bArr, ContentValues contentValues, String str, MediaSaverListener mediaSaverListener) {
        addSaveRequest(bArr, contentValues, str, mediaSaverListener, 256);
    }

    public void addSaveRequest(byte[] bArr, ContentValues contentValues, String str, MediaSaverListener mediaSaverListener, int i) {
        if (bArr == null) {
            LogHelper.w(TAG, "[addSaveRequest] there is no valid data need to save.");
        } else {
            addRequest(new Request(bArr, contentValues, str, mediaSaverListener, null, i));
        }
    }

    public void addSaveRequest(ContentValues contentValues, String str, MediaSaverListener mediaSaverListener) {
        if (contentValues == null) {
            LogHelper.w(TAG, "[addSaveRequest] there is no valid data need to save.");
        } else {
            addRequest(new Request(null, contentValues, str, mediaSaverListener, null, 0));
        }
    }

    public void updateSaveRequest(byte[] bArr, ContentValues contentValues, String str, Uri uri) {
        if (contentValues == null) {
            LogHelper.w(TAG, "[updateSaveRequest] there is no valid data need to save.");
        } else {
            addRequest(new Request(bArr, contentValues, str, null, uri, 0));
        }
    }

    public long getBytesWaitingToSave() {
        long dataSize;
        synchronized (this.mSaveQueue) {
            Iterator<Request> it = this.mSaveQueue.iterator();
            dataSize = 0;
            while (it.hasNext()) {
                dataSize += (long) it.next().getDataSize();
            }
        }
        return dataSize;
    }

    public int getPendingRequestNumber() {
        int size;
        synchronized (this.mSaveQueue) {
            size = this.mSaveQueue.size();
        }
        return size;
    }

    private void saveDataToStorage(Request request) throws Throwable {
        FileOutputStream fileOutputStream;
        LogHelper.d(TAG, "[saveDataToStorage]+");
        if (request.mData == null) {
            LogHelper.w(TAG, "data is null,return!");
            return;
        }
        if (request.mFilePath == null && request.mValues != null) {
            LogHelper.d(TAG, "get filePath from contentValues.");
            request.mFilePath = request.mValues.getAsString("_data");
        }
        if (request.mFilePath == null) {
            LogHelper.w(TAG, "filePath is null, return");
            return;
        }
        String str = request.mFilePath + TEMP_SUFFIX;
        if (request.mType == HeifHelper.FORMAT_HEIF) {
            HeifHelper.saveData(request.mData, request.mValues.getAsInteger("width").intValue(), request.mValues.getAsInteger("height").intValue(), request.mValues.getAsInteger("orientation").intValue(), request.mFilePath);
            LogHelper.d(TAG, "[saveDataToStorage]-");
            return;
        }
        FileOutputStream fileOutputStream2 = null;
        try {
            try {
                try {
                    LogHelper.d(TAG, "save the data to SD Card");
                    fileOutputStream = new FileOutputStream(str);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
                fileOutputStream = fileOutputStream2;
            }
        } catch (IOException e2) {
            LogHelper.e(TAG, "IOException:", e2);
        }
        try {
            fileOutputStream.write(request.mData);
            fileOutputStream.close();
            new File(str).renameTo(new File(request.mFilePath));
            fileOutputStream.close();
        } catch (IOException e3) {
            e = e3;
            fileOutputStream2 = fileOutputStream;
            LogHelper.e(TAG, "Failed to write image,ex:", e);
            if (fileOutputStream2 != null) {
                fileOutputStream2.close();
            }
            LogHelper.d(TAG, "[saveDataToStorage]-");
        } catch (Throwable th2) {
            th = th2;
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e4) {
                    LogHelper.e(TAG, "IOException:", e4);
                }
            }
            throw th;
        }
        LogHelper.d(TAG, "[saveDataToStorage]-");
    }

    private void insertDb(Request request) {
        LogUtil.Tag tag;
        StringBuilder sb;
        LogUtil.Tag tag2;
        StringBuilder sb2;
        LogHelper.d(TAG, "[insertDb]");
        if (request.mValues == null) {
            LogHelper.w(TAG, "[insertDb] ContentValues is null, return");
            return;
        }
        try {
            if (request.mData != null) {
                try {
                    try {
                        try {
                            updateContentValues(request);
                            request.mUri = this.mContentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, request.mValues);
                            tag = TAG;
                            sb = new StringBuilder();
                        } catch (SQLiteConstraintException e) {
                            LogHelper.e(TAG, "failed to add image to media store,SQLiteConstraintException:", e);
                            tag = TAG;
                            sb = new StringBuilder();
                        }
                    } catch (UnsupportedOperationException e2) {
                        LogHelper.e(TAG, "failed to add image to media store, UnsupportedOperationException:", e2);
                        tag = TAG;
                        sb = new StringBuilder();
                    }
                } catch (IllegalArgumentException e3) {
                    LogHelper.e(TAG, "failed to add image to media store, IllegalArgumentException:", e3);
                    tag = TAG;
                    sb = new StringBuilder();
                }
                sb.append("Current image URI: ");
                sb.append(request.mUri);
                request = sb.toString();
                LogHelper.v(tag, request);
                return;
            }
            if (request.mFilePath == null) {
                LogHelper.w(TAG, "filePath is null when insert video DB");
                return;
            }
            new File(request.mFilePath).renameTo(new File(request.mValues.getAsString("_data")));
            try {
                try {
                    try {
                        try {
                            request.mUri = this.mContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, request.mValues);
                            tag2 = TAG;
                            sb2 = new StringBuilder();
                        } catch (SQLiteConstraintException e4) {
                            LogHelper.e(TAG, "failed to add video to media store,SQLiteConstraintException:", e4);
                            tag2 = TAG;
                            sb2 = new StringBuilder();
                        }
                    } catch (UnsupportedOperationException e5) {
                        LogHelper.e(TAG, "failed to add video to media store, UnsupportedOperationException:", e5);
                        tag2 = TAG;
                        sb2 = new StringBuilder();
                    }
                } catch (IllegalArgumentException e6) {
                    LogHelper.e(TAG, "failed to add video to media store, IllegalArgumentException:", e6);
                    tag2 = TAG;
                    sb2 = new StringBuilder();
                }
                sb2.append("Current video URI: ");
                sb2.append(request.mUri);
                request = sb2.toString();
                LogHelper.v(tag2, request);
            } catch (Throwable th) {
                LogHelper.v(TAG, "Current video URI: " + request.mUri);
                throw th;
            }
        } catch (Throwable th2) {
            LogHelper.v(TAG, "Current image URI: " + request.mUri);
            throw th2;
        }
    }

    private void updateDbAccordingUri(Request request) {
        LogUtil.Tag tag;
        StringBuilder sb;
        LogHelper.d(TAG, "[updateDbAccordingUri]");
        if (request.mValues == null) {
            LogHelper.w(TAG, "[updateDbAccordingUri] ContentValues is null, return");
            return;
        }
        try {
            if (request.mData != null) {
                try {
                    try {
                        updateContentValues(request);
                        this.mContentResolver.update(request.mUri, request.mValues, null, null);
                        tag = TAG;
                        sb = new StringBuilder();
                    } catch (IllegalArgumentException e) {
                        LogHelper.e(TAG, "failed to update image to media store, IllegalArgumentException:", e);
                        tag = TAG;
                        sb = new StringBuilder();
                    }
                } catch (SQLiteConstraintException e2) {
                    LogHelper.e(TAG, "failed to update image to media store,SQLiteConstraintException:", e2);
                    tag = TAG;
                    sb = new StringBuilder();
                } catch (UnsupportedOperationException e3) {
                    LogHelper.e(TAG, "failed to update image to media store, UnsupportedOperationException:", e3);
                    tag = TAG;
                    sb = new StringBuilder();
                }
                sb.append("Current image URI: ");
                sb.append(request.mUri);
                request = sb.toString();
                LogHelper.v(tag, request);
            }
        } catch (Throwable th) {
            LogHelper.v(TAG, "Current image URI: " + request.mUri);
            throw th;
        }
    }

    private void addRequest(Request request) {
        LogHelper.d(TAG, "[addSaveRequest]+, the queue number is = " + this.mSaveQueue.size() + "mSaveTask:" + this.mSaveTask);
        synchronized (this.mSaveQueue) {
            this.mSaveQueue.add(request);
        }
        if (this.mSaveTask == null) {
            this.mSaveTask = new SaveTask();
            this.mSaveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            LogHelper.d(TAG, "[addRequest]execute save AsyncTask.");
        }
        LogHelper.d(TAG, "[addRequest]-, the queue number is = " + this.mSaveQueue.size());
    }

    private void updateContentValues(Request request) {
        if (request.mFilePath == null) {
            return;
        }
        Integer asInteger = request.mValues.getAsInteger("width");
        Integer asInteger2 = request.mValues.getAsInteger("height");
        LogHelper.d(TAG, "[updateContentValues] size :" + asInteger + " X " + asInteger2);
        if (asInteger != null && asInteger2 != null) {
            if (asInteger.intValue() != 0 && asInteger2.intValue() != 0) {
                return;
            }
            Size sizeFromSdkExif = CameraUtil.getSizeFromSdkExif(request.mFilePath);
            request.mValues.put("width", Integer.valueOf(sizeFromSdkExif.getWidth()));
            request.mValues.put("height", Integer.valueOf(sizeFromSdkExif.getHeight()));
            LogHelper.d(TAG, "[updateContentValues] ,update width & height");
        }
    }

    private class Request {
        private byte[] mData;
        private String mFilePath;
        private MediaSaverListener mMediaSaverListener;
        private int mType;
        private Uri mUri;
        private ContentValues mValues;

        public Request(byte[] bArr, ContentValues contentValues, String str, MediaSaverListener mediaSaverListener, Uri uri, int i) {
            this.mData = bArr;
            this.mValues = contentValues;
            this.mFilePath = str;
            this.mMediaSaverListener = mediaSaverListener;
            this.mUri = uri;
            this.mType = i;
        }

        private int getDataSize() {
            if (this.mData == null) {
                return 0;
            }
            return this.mData.length;
        }

        private void saveRequest() throws Throwable {
            MediaSaver.this.saveDataToStorage(this);
            if (this.mUri == null) {
                MediaSaver.this.insertDb(this);
            } else {
                MediaSaver.this.updateDbAccordingUri(this);
            }
        }
    }

    private class SaveTask extends AsyncTask<Void, Void, Void> {
        Request mRequest;

        public SaveTask() {
        }

        @Override
        protected void onPreExecute() {
            LogHelper.d(MediaSaver.TAG, "[SaveTask]onPreExcute.");
        }

        @Override
        protected Void doInBackground(Void... voidArr) throws Throwable {
            LogHelper.d(MediaSaver.TAG, "[SaveTask]doInBackground+, queue is empty = " + MediaSaver.this.mSaveQueue.isEmpty());
            while (true) {
                if (!MediaSaver.this.mSaveQueue.isEmpty()) {
                    synchronized (MediaSaver.this.mSaveQueue) {
                        if (MediaSaver.this.mSaveQueue.isEmpty()) {
                            break;
                        }
                        this.mRequest = (Request) MediaSaver.this.mSaveQueue.get(0);
                        MediaSaver.this.mSaveQueue.remove(0);
                    }
                } else {
                    break;
                }
            }
            MediaSaver.this.mSaveTask = null;
            LogHelper.d(MediaSaver.TAG, "[SaveTask] doInBackground-");
            return null;
        }

        @Override
        protected void onPostExecute(Void r1) {
        }
    }
}
