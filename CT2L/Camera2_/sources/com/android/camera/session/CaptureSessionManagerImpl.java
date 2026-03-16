package com.android.camera.session;

import android.content.ContentResolver;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import com.android.camera.Storage;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.session.CaptureSession;
import com.android.camera.session.CaptureSessionManager;
import com.android.camera.session.PlaceholderManager;
import com.android.camera.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class CaptureSessionManagerImpl implements CaptureSessionManager {
    private static final Log.Tag TAG = new Log.Tag("CaptureSessMgrImpl");
    public static final String TEMP_SESSIONS = "TEMP_SESSIONS";
    private final ContentResolver mContentResolver;
    private final MediaSaver mMediaSaver;
    private final PlaceholderManager mPlaceholderManager;
    private final SessionStorageManager mSessionStorageManager;
    private final HashMap<Uri, CharSequence> mFailedSessionMessages = new HashMap<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final LinkedList<CaptureSessionManager.SessionListener> mTaskListeners = new LinkedList<>();
    private final Map<String, CaptureSession> mSessions = new HashMap();

    private class CaptureSessionImpl implements CaptureSession {
        private Uri mContentUri;
        private Location mLocation;
        private boolean mNoPlaceHolderRequired;
        private PlaceholderManager.Session mPlaceHolderSession;
        private final HashSet<CaptureSession.ProgressListener> mProgressListeners;
        private CharSequence mProgressMessage;
        private int mProgressPercent;
        private final long mSessionStartMillis;
        private final String mTitle;
        private Uri mUri;

        private CaptureSessionImpl(String title, long sessionStartMillis, Location location) {
            this.mProgressPercent = 0;
            this.mNoPlaceHolderRequired = false;
            this.mProgressListeners = new HashSet<>();
            this.mTitle = title;
            this.mSessionStartMillis = sessionStartMillis;
            this.mLocation = location;
        }

        @Override
        public String getTitle() {
            return this.mTitle;
        }

        @Override
        public Location getLocation() {
            return this.mLocation;
        }

        @Override
        public void setLocation(Location location) {
            this.mLocation = location;
        }

        @Override
        public synchronized void setProgress(int percent) {
            this.mProgressPercent = percent;
            CaptureSessionManagerImpl.this.notifyTaskProgress(this.mUri, this.mProgressPercent);
            for (CaptureSession.ProgressListener listener : this.mProgressListeners) {
                listener.onProgressChanged(percent);
            }
        }

        @Override
        public synchronized int getProgress() {
            return this.mProgressPercent;
        }

        @Override
        public synchronized CharSequence getProgressMessage() {
            return this.mProgressMessage;
        }

        @Override
        public synchronized void setProgressMessage(CharSequence message) {
            this.mProgressMessage = message;
            CaptureSessionManagerImpl.this.notifyTaskProgressText(this.mUri, message);
            for (CaptureSession.ProgressListener listener : this.mProgressListeners) {
                listener.onStatusMessageChanged(message);
            }
        }

        @Override
        public void startEmpty() {
            this.mNoPlaceHolderRequired = true;
        }

        @Override
        public synchronized void startSession(byte[] placeholder, CharSequence progressMessage) {
            this.mProgressMessage = progressMessage;
            this.mPlaceHolderSession = CaptureSessionManagerImpl.this.mPlaceholderManager.insertPlaceholder(this.mTitle, placeholder, this.mSessionStartMillis);
            this.mUri = this.mPlaceHolderSession.outputUri;
            CaptureSessionManagerImpl.this.putSession(this.mUri, this);
            CaptureSessionManagerImpl.this.notifyTaskQueued(this.mUri);
        }

        @Override
        public synchronized void startSession(Uri uri, CharSequence progressMessage) {
            this.mUri = uri;
            this.mProgressMessage = progressMessage;
            this.mPlaceHolderSession = CaptureSessionManagerImpl.this.mPlaceholderManager.convertToPlaceholder(uri);
            CaptureSessionManagerImpl.this.mSessions.put(this.mUri.toString(), this);
            CaptureSessionManagerImpl.this.notifyTaskQueued(this.mUri);
        }

        @Override
        public synchronized void cancel() {
            if (this.mUri != null) {
                CaptureSessionManagerImpl.this.removeSession(this.mUri.toString());
            }
        }

        @Override
        public synchronized void saveAndFinish(byte[] data, int width, int height, int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener listener) {
            if (this.mNoPlaceHolderRequired) {
                CaptureSessionManagerImpl.this.mMediaSaver.addImage(data, this.mTitle, this.mSessionStartMillis, null, width, height, orientation, exif, listener, CaptureSessionManagerImpl.this.mContentResolver);
            } else if (this.mPlaceHolderSession != null) {
                this.mContentUri = CaptureSessionManagerImpl.this.mPlaceholderManager.finishPlaceholder(this.mPlaceHolderSession, this.mLocation, orientation, exif, data, width, height, "image/jpeg");
                CaptureSessionManagerImpl.this.removeSession(this.mUri.toString());
                CaptureSessionManagerImpl.this.notifyTaskDone(this.mPlaceHolderSession.outputUri);
            } else {
                throw new IllegalStateException("Cannot call saveAndFinish without calling startSession first.");
            }
        }

        @Override
        public void finish() {
            if (this.mPlaceHolderSession == null) {
                throw new IllegalStateException("Cannot call finish without calling startSession first.");
            }
            final String path = getPath();
            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    ExifInterface exif;
                    ExifInterface exif2;
                    try {
                        byte[] jpegDataTemp = FileUtil.readFileToByteArray(new File(path));
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(jpegDataTemp, 0, jpegDataTemp.length, options);
                        int width = options.outWidth;
                        int height = options.outHeight;
                        try {
                            exif2 = new ExifInterface();
                        } catch (IOException e) {
                            e = e;
                        }
                        try {
                            exif2.readExif(jpegDataTemp);
                            exif = exif2;
                        } catch (IOException e2) {
                            e = e2;
                            Log.w(CaptureSessionManagerImpl.TAG, "Could not read exif", e);
                            exif = null;
                        }
                        CaptureSessionImpl.this.saveAndFinish(jpegDataTemp, width, height, 0, exif, null);
                    } catch (IOException e3) {
                    }
                }
            });
        }

        @Override
        public String getPath() {
            if (this.mUri == null) {
                throw new IllegalStateException("Cannot retrieve URI of not started session.");
            }
            try {
                File tempDirectory = new File(CaptureSessionManagerImpl.this.getSessionDirectory(CaptureSessionManagerImpl.TEMP_SESSIONS), this.mTitle);
                tempDirectory.mkdirs();
                File tempFile = new File(tempDirectory, this.mTitle + Storage.JPEG_POSTFIX);
                try {
                    if (!tempFile.exists()) {
                        tempFile.createNewFile();
                    }
                    return tempFile.getPath();
                } catch (IOException e) {
                    Log.e(CaptureSessionManagerImpl.TAG, "Could not create temp session file", e);
                    throw new RuntimeException("Could not create temp session file", e);
                }
            } catch (IOException e2) {
                Log.e(CaptureSessionManagerImpl.TAG, "Could not get temp session directory", e2);
                throw new RuntimeException("Could not get temp session directory", e2);
            }
        }

        @Override
        public Uri getUri() {
            return this.mUri;
        }

        @Override
        public Uri getContentUri() {
            return this.mContentUri;
        }

        @Override
        public boolean hasPath() {
            return this.mUri != null;
        }

        @Override
        public void onPreviewAvailable() {
            CaptureSessionManagerImpl.this.notifySessionPreviewAvailable(this.mPlaceHolderSession.outputUri);
        }

        @Override
        public void updatePreview(String previewPath) {
            final String path = getPath();
            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        byte[] jpegDataTemp = FileUtil.readFileToByteArray(new File(path));
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(jpegDataTemp, 0, jpegDataTemp.length, options);
                        int width = options.outWidth;
                        int height = options.outHeight;
                        CaptureSessionManagerImpl.this.mPlaceholderManager.replacePlaceholder(CaptureSessionImpl.this.mPlaceHolderSession, jpegDataTemp, width, height);
                        CaptureSessionImpl.this.onPreviewAvailable();
                    } catch (IOException e) {
                    }
                }
            });
        }

        @Override
        public void finishWithFailure(CharSequence reason) {
            if (this.mPlaceHolderSession == null) {
                throw new IllegalStateException("Cannot call finish without calling startSession first.");
            }
            this.mProgressMessage = reason;
            CaptureSessionManagerImpl.this.removeSession(this.mUri.toString());
            CaptureSessionManagerImpl.this.mFailedSessionMessages.put(this.mPlaceHolderSession.outputUri, reason);
            CaptureSessionManagerImpl.this.notifyTaskFailed(this.mPlaceHolderSession.outputUri, reason);
        }

        @Override
        public void addProgressListener(CaptureSession.ProgressListener listener) {
            listener.onStatusMessageChanged(this.mProgressMessage);
            listener.onProgressChanged(this.mProgressPercent);
            this.mProgressListeners.add(listener);
        }

        @Override
        public void removeProgressListener(CaptureSession.ProgressListener listener) {
            this.mProgressListeners.remove(listener);
        }
    }

    public CaptureSessionManagerImpl(MediaSaver mediaSaver, ContentResolver contentResolver, PlaceholderManager placeholderManager, SessionStorageManager sessionStorageManager) {
        this.mMediaSaver = mediaSaver;
        this.mContentResolver = contentResolver;
        this.mPlaceholderManager = placeholderManager;
        this.mSessionStorageManager = sessionStorageManager;
    }

    @Override
    public CaptureSession createNewSession(String title, long sessionStartTime, Location location) {
        return new CaptureSessionImpl(title, sessionStartTime, location);
    }

    @Override
    public CaptureSession createSession() {
        return new CaptureSessionImpl(null, System.currentTimeMillis(), 0 == true ? 1 : 0);
    }

    @Override
    public void putSession(Uri sessionUri, CaptureSession session) {
        synchronized (this.mSessions) {
            this.mSessions.put(sessionUri.toString(), session);
        }
    }

    @Override
    public CaptureSession getSession(Uri sessionUri) {
        CaptureSession captureSession;
        synchronized (this.mSessions) {
            captureSession = this.mSessions.get(sessionUri.toString());
        }
        return captureSession;
    }

    @Override
    public void saveImage(byte[] data, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, MediaSaver.OnMediaSavedListener listener) {
        this.mMediaSaver.addImage(data, title, date, loc, width, height, orientation, exif, listener, this.mContentResolver);
    }

    @Override
    public void addSessionListener(CaptureSessionManager.SessionListener listener) {
        synchronized (this.mTaskListeners) {
            this.mTaskListeners.add(listener);
        }
    }

    @Override
    public void removeSessionListener(CaptureSessionManager.SessionListener listener) {
        synchronized (this.mTaskListeners) {
            this.mTaskListeners.remove(listener);
        }
    }

    @Override
    public File getSessionDirectory(String subDirectory) throws IOException {
        return this.mSessionStorageManager.getSessionDirectory(subDirectory);
    }

    private void removeSession(String sessionUri) {
        synchronized (this.mSessions) {
            this.mSessions.remove(sessionUri);
        }
    }

    private void notifyTaskQueued(final Uri uri) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    for (CaptureSessionManager.SessionListener listener : CaptureSessionManagerImpl.this.mTaskListeners) {
                        listener.onSessionQueued(uri);
                    }
                }
            }
        });
    }

    private void notifyTaskDone(final Uri uri) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    for (CaptureSessionManager.SessionListener listener : CaptureSessionManagerImpl.this.mTaskListeners) {
                        listener.onSessionDone(uri);
                    }
                }
            }
        });
    }

    private void notifyTaskFailed(final Uri uri, final CharSequence reason) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    for (CaptureSessionManager.SessionListener listener : CaptureSessionManagerImpl.this.mTaskListeners) {
                        listener.onSessionFailed(uri, reason);
                    }
                }
            }
        });
    }

    private void notifyTaskProgress(final Uri uri, final int progressPercent) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    for (CaptureSessionManager.SessionListener listener : CaptureSessionManagerImpl.this.mTaskListeners) {
                        listener.onSessionProgress(uri, progressPercent);
                    }
                }
            }
        });
    }

    private void notifyTaskProgressText(final Uri uri, final CharSequence message) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    for (CaptureSessionManager.SessionListener listener : CaptureSessionManagerImpl.this.mTaskListeners) {
                        listener.onSessionProgressText(uri, message);
                    }
                }
            }
        });
    }

    private void notifySessionPreviewAvailable(final Uri uri) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    for (CaptureSessionManager.SessionListener listener : CaptureSessionManagerImpl.this.mTaskListeners) {
                        listener.onSessionPreviewAvailable(uri);
                    }
                }
            }
        });
    }

    @Override
    public boolean hasErrorMessage(Uri uri) {
        return this.mFailedSessionMessages.containsKey(uri);
    }

    @Override
    public CharSequence getErrorMesage(Uri uri) {
        return this.mFailedSessionMessages.get(uri);
    }

    @Override
    public void removeErrorMessage(Uri uri) {
        this.mFailedSessionMessages.remove(uri);
    }

    @Override
    public void fillTemporarySession(final CaptureSessionManager.SessionListener listener) {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mSessions) {
                    for (String sessionUri : CaptureSessionManagerImpl.this.mSessions.keySet()) {
                        CaptureSession session = (CaptureSession) CaptureSessionManagerImpl.this.mSessions.get(sessionUri);
                        listener.onSessionQueued(session.getUri());
                        listener.onSessionProgress(session.getUri(), session.getProgress());
                        listener.onSessionProgressText(session.getUri(), session.getProgressMessage());
                    }
                }
            }
        });
    }
}
