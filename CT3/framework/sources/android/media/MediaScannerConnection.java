package android.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.IMediaScannerListener;
import android.media.IMediaScannerService;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class MediaScannerConnection implements ServiceConnection {
    private static final boolean LOG;
    private static final boolean LOGD = "eng".equals(Build.TYPE);
    private static final String TAG = "MediaScannerConnection";
    private MediaScannerConnectionClient mClient;
    private boolean mConnected;
    private Context mContext;
    private final IMediaScannerListener.Stub mListener = new IMediaScannerListener.Stub() {
        @Override
        public void scanCompleted(String path, Uri uri) {
            MediaScannerConnectionClient client = MediaScannerConnection.this.mClient;
            if (client == null) {
                return;
            }
            client.onScanCompleted(path, uri);
        }
    };
    private IMediaScannerService mService;

    public interface MediaScannerConnectionClient extends OnScanCompletedListener {
        void onMediaScannerConnected();

        @Override
        void onScanCompleted(String str, Uri uri);
    }

    public interface OnScanCompletedListener {
        void onScanCompleted(String str, Uri uri);
    }

    static {
        LOG = !Log.isLoggable(TAG, 3) ? LOGD : true;
    }

    public MediaScannerConnection(Context context, MediaScannerConnectionClient client) {
        this.mContext = context;
        this.mClient = client;
    }

    public void connect() {
        synchronized (this) {
            if (!this.mConnected) {
                Intent intent = new Intent(IMediaScannerService.class.getName());
                intent.setComponent(new ComponentName("com.android.providers.media", "com.android.providers.media.MediaScannerService"));
                this.mContext.bindService(intent, this, 1);
                this.mConnected = true;
            }
        }
    }

    public void disconnect() {
        synchronized (this) {
            if (this.mConnected) {
                if (LOG) {
                    Log.v(TAG, "Disconnecting from Media Scanner");
                }
                try {
                    this.mContext.unbindService(this);
                } catch (IllegalArgumentException ex) {
                    if (LOG) {
                        Log.v(TAG, "disconnect failed: " + ex);
                    }
                }
                this.mConnected = false;
            }
        }
    }

    public synchronized boolean isConnected() {
        return this.mService != null ? this.mConnected : false;
    }

    public void scanFile(String path, String mimeType) {
        synchronized (this) {
            if (this.mService == null || !this.mConnected) {
                throw new IllegalStateException("not connected to MediaScannerService");
            }
            try {
                if (LOG) {
                    Log.v(TAG, "Scanning file " + path);
                }
                this.mService.requestScanFile(path, mimeType, this.mListener);
            } catch (RemoteException e) {
                if (LOG) {
                    Log.d(TAG, "Failed to scan file " + path);
                }
            }
        }
    }

    static class ClientProxy implements MediaScannerConnectionClient {
        final OnScanCompletedListener mClient;
        MediaScannerConnection mConnection;
        final String[] mMimeTypes;
        int mNextPath;
        final String[] mPaths;

        ClientProxy(String[] paths, String[] mimeTypes, OnScanCompletedListener client) {
            this.mPaths = paths;
            this.mMimeTypes = mimeTypes;
            this.mClient = client;
        }

        @Override
        public void onMediaScannerConnected() {
            scanNextPath();
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (this.mClient != null) {
                this.mClient.onScanCompleted(path, uri);
            }
            scanNextPath();
        }

        void scanNextPath() {
            if (this.mNextPath >= this.mPaths.length) {
                this.mConnection.disconnect();
            } else {
                this.mConnection.scanFile(this.mPaths[this.mNextPath], this.mMimeTypes != null ? this.mMimeTypes[this.mNextPath] : null);
                this.mNextPath++;
            }
        }
    }

    public static void scanFile(Context context, String[] paths, String[] mimeTypes, OnScanCompletedListener callback) {
        ClientProxy client = new ClientProxy(paths, mimeTypes, callback);
        MediaScannerConnection connection = new MediaScannerConnection(context, client);
        client.mConnection = connection;
        connection.connect();
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        if (LOG) {
            Log.v(TAG, "Connected to Media Scanner");
        }
        synchronized (this) {
            this.mService = IMediaScannerService.Stub.asInterface(service);
            if (this.mService != null && this.mClient != null) {
                this.mClient.onMediaScannerConnected();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        if (LOG) {
            Log.v(TAG, "Disconnected from Media Scanner");
        }
        synchronized (this) {
            this.mService = null;
        }
    }
}
