package com.android.providers.media;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.media.IMediaScannerListener;
import android.media.IMediaScannerService;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.util.Locale;

public class MediaScannerService extends Service implements Runnable {
    private final IMediaScannerService.Stub mBinder = new IMediaScannerService.Stub() {
        public void requestScanFile(String path, String mimeType, IMediaScannerListener listener) {
            Bundle args = new Bundle();
            args.putString("filepath", path);
            args.putString("mimetype", mimeType);
            if (listener != null) {
                args.putIBinder("listener", listener.asBinder());
            }
            MediaScannerService.this.startService(new Intent(MediaScannerService.this, (Class<?>) MediaScannerService.class).putExtras(args));
        }

        public void scanFile(String path, String mimeType) {
            requestScanFile(path, mimeType, null);
        }
    };
    private String[] mExternalStoragePaths;
    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;
    private PowerManager.WakeLock mWakeLock;

    private void openDatabase(String volumeName) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", volumeName);
            getContentResolver().insert(Uri.parse("content://media/"), values);
        } catch (IllegalArgumentException e) {
            Log.w("MediaScannerService", "failed to open media database");
        }
    }

    private MediaScanner createMediaScanner() {
        MediaScanner scanner = new MediaScanner(this);
        Locale locale = getResources().getConfiguration().locale;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language != null) {
                if (country != null) {
                    scanner.setLocale(language + "_" + country);
                } else {
                    scanner.setLocale(language);
                }
            }
        }
        return scanner;
    }

    private void scan(String[] directories, String volumeName) {
        Uri uri = Uri.parse("file://" + directories[0]);
        this.mWakeLock.acquire();
        try {
            ContentValues values = new ContentValues();
            values.put("volume", volumeName);
            Uri scanUri = getContentResolver().insert(MediaStore.getMediaScannerUri(), values);
            sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_STARTED", uri));
            try {
                if (volumeName.equals("external")) {
                    openDatabase(volumeName);
                }
                MediaScanner scanner = createMediaScanner();
                scanner.scanDirectories(directories, volumeName);
            } catch (Exception e) {
                Log.e("MediaScannerService", "exception in MediaScanner.scan()", e);
            }
            getContentResolver().delete(scanUri, null, null);
        } finally {
            sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri));
            this.mWakeLock.release();
        }
    }

    @Override
    public void onCreate() {
        PowerManager pm = (PowerManager) getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, "MediaScannerService");
        StorageManager storageManager = (StorageManager) getSystemService("storage");
        this.mExternalStoragePaths = storageManager.getVolumePaths();
        Thread thr = new Thread(null, this, "MediaScannerService");
        thr.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        while (this.mServiceHandler == null) {
            synchronized (this) {
                try {
                    wait(100L);
                } catch (InterruptedException e) {
                }
            }
        }
        if (intent == null) {
            Log.e("MediaScannerService", "Intent is null in onStartCommand: ", new NullPointerException());
            return 2;
        }
        Message msg = this.mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent.getExtras();
        this.mServiceHandler.sendMessage(msg);
        return 3;
    }

    @Override
    public void onDestroy() {
        while (this.mServiceLooper == null) {
            synchronized (this) {
                try {
                    wait(100L);
                } catch (InterruptedException e) {
                }
            }
        }
        this.mServiceLooper.quit();
    }

    @Override
    public void run() {
        Process.setThreadPriority(11);
        Looper.prepare();
        this.mServiceLooper = Looper.myLooper();
        this.mServiceHandler = new ServiceHandler();
        Looper.loop();
    }

    private Uri scanFile(String path, String mimeType) {
        openDatabase("external");
        MediaScanner scanner = createMediaScanner();
        try {
            String canonicalPath = new File(path).getCanonicalPath();
            return scanner.scanSingleFile(canonicalPath, "external", mimeType);
        } catch (Exception e) {
            Log.e("MediaScannerService", "bad path " + path + " in scanFile()", e);
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    private final class ServiceHandler extends Handler {
        private ServiceHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle arguments = (Bundle) msg.obj;
            String filePath = arguments.getString("filepath");
            try {
                if (filePath != null) {
                    IBinder binder = arguments.getIBinder("listener");
                    IMediaScannerListener listener = binder == null ? null : IMediaScannerListener.Stub.asInterface(binder);
                    Uri uri = null;
                    try {
                        uri = MediaScannerService.this.scanFile(filePath, arguments.getString("mimetype"));
                    } catch (Exception e) {
                        Log.e("MediaScannerService", "Exception scanning file", e);
                    }
                    if (listener != null) {
                        listener.scanCompleted(filePath, uri);
                    }
                } else {
                    String volume = arguments.getString("volume");
                    String[] directories = null;
                    if ("internal".equals(volume)) {
                        directories = new String[]{Environment.getRootDirectory() + "/media", Environment.getOemDirectory() + "/media"};
                    } else if ("external".equals(volume)) {
                        directories = MediaScannerService.this.mExternalStoragePaths;
                    }
                    if (directories != null) {
                        MediaScannerService.this.scan(directories, volume);
                    }
                }
            } catch (Exception e2) {
                Log.e("MediaScannerService", "Exception in handleMessage", e2);
            }
            MediaScannerService.this.stopSelf(msg.arg1);
        }
    }
}
