package com.android.providers.media;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.IMediaScannerListener;
import android.media.IMediaScannerService;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.mediatek.media.mediascanner.MediaScannerExImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class MediaScannerService extends Service implements Runnable {
    private String[] mExternalStoragePaths;
    private MediaScannerInserter mMediaScannerInserter;
    private MediaScannerThreadPool mMediaScannerThreadPool;
    private long mPostScanFinishTime;
    private long mPreScanFinishTime;
    private long mScanFinishTime;
    private long mScanStartTime;
    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;
    private PowerManager.WakeLock mWakeLock;
    private final ArrayList<PrescanTask> mPrescanTaskList = new ArrayList<>();
    private final IMediaScannerService.Stub mBinder = new IMediaScannerService.Stub() {
        public void requestScanFile(String str, String str2, IMediaScannerListener iMediaScannerListener) {
            if (MediaUtils.LOG_SCAN) {
                Log.d("MediaScannerService", "IMediaScannerService.scanFile: " + str + " mimeType: " + str2);
            }
            Bundle bundle = new Bundle();
            bundle.putString("filepath", str);
            bundle.putString("mimetype", str2);
            if (iMediaScannerListener != null) {
                bundle.putIBinder("listener", iMediaScannerListener.asBinder());
            }
            MediaScannerService.this.startService(new Intent(MediaScannerService.this, (Class<?>) MediaScannerService.class).putExtras(bundle));
        }

        public void scanFile(String str, String str2) {
            requestScanFile(str, str2, null);
        }
    };
    private boolean mIsThreadPoolEnable = false;
    private int mStartId = -1;
    private BroadcastReceiver mUnmountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                Log.v("MediaScannerService", "onReceive()-" + intent.getAction());
                while (MediaScannerService.this.mServiceHandler == null) {
                    try {
                        wait(100L);
                    } catch (InterruptedException e) {
                        Log.e("MediaScannerService", "onStartCommand: InterruptedException!");
                    }
                }
                if (MediaScannerService.this.mMediaScannerThreadPool != null && MediaScannerService.this.mIsThreadPoolEnable) {
                    MediaScannerService.this.mMediaScannerThreadPool.stopScan();
                }
                int unused = MediaScannerService.this.mStartId;
                MediaScannerService.this.mStartId = -1;
                MediaScannerService.this.mServiceHandler.removeMessages(10);
            }
        }
    };

    class PrescanTask extends AsyncTask<Void, Void, Void> {
        private Bundle mBundle;
        private Handler mHandler;
        private int mStartId;
        private String mVolumn;

        public PrescanTask(Bundle bundle, Handler handler, int i) {
            this.mBundle = null;
            this.mHandler = null;
            this.mStartId = -1;
            this.mVolumn = null;
            this.mBundle = bundle;
            this.mHandler = handler;
            this.mStartId = i;
            this.mVolumn = ((Bundle) bundle.clone()).getString("volume");
        }

        protected String getPrescanVolume() {
            return this.mVolumn;
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            Log.d("MediaScannerService", "PrescanTask.doInBackground() mVolumn = " + this.mVolumn);
            if (this.mVolumn != null && this.mVolumn.equals("external")) {
                MediaScannerService.this.prescanSdCardRelated("external");
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void r6) {
            MediaScannerService.this.mServiceHandler.sendMessage(MediaScannerService.this.mServiceHandler.obtainMessage(10, this.mStartId, -1, this.mBundle));
            synchronized (MediaScannerService.this) {
                MediaScannerService.this.mPrescanTaskList.remove(this);
            }
            super.onPostExecute(r6);
        }
    }

    private void prescanSdCardRelated(String str) {
        MediaScannerExImpl mediaScannerExImpl;
        Throwable th;
        Throwable th2;
        try {
            try {
                getContentResolver().call(MediaStore.Files.getContentUri("external"), "action_prescan_started", (String) null, (Bundle) null);
                mediaScannerExImpl = new MediaScannerExImpl(this, str);
            } catch (IllegalArgumentException e) {
                Log.e("MediaScannerService", "IllegalArgumentException in prescanSdCardRelated", e);
                return;
            } catch (Exception e2) {
                Log.e("MediaScannerService", "Exception in prescanSdCardRelated", e2);
                getContentResolver().call(MediaStore.Files.getContentUri("external"), "action_prescan_done", (String) null, (Bundle) null);
                if (!MediaUtils.LOG_SCAN) {
                    return;
                }
            }
            try {
                mediaScannerExImpl.preScanAll(str);
                $closeResource(null, mediaScannerExImpl);
                getContentResolver().call(MediaStore.Files.getContentUri("external"), "action_prescan_done", (String) null, (Bundle) null);
                if (!MediaUtils.LOG_SCAN) {
                    return;
                }
                Log.d("MediaScannerService", "prescanSdCardRelated()");
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    $closeResource(th, mediaScannerExImpl);
                    throw th2;
                }
            }
        } catch (Throwable th5) {
            getContentResolver().call(MediaStore.Files.getContentUri("external"), "action_prescan_done", (String) null, (Bundle) null);
            if (MediaUtils.LOG_SCAN) {
                Log.d("MediaScannerService", "prescanSdCardRelated()");
            }
            throw th5;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private void openDatabase(String str) {
        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", str);
            getContentResolver().insert(Uri.parse("content://media/"), contentValues);
        } catch (IllegalArgumentException e) {
            Log.w("MediaScannerService", "failed to open media database");
        }
    }

    private void scan(String[] strArr, String str) {
        String str2;
        StringBuilder sb;
        Uri uriInsert;
        MediaScanner mediaScanner;
        Throwable th;
        Throwable th2;
        Uri uri = Uri.parse("file://" + strArr[0]);
        this.mWakeLock.acquire();
        try {
            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put("volume", str);
                uriInsert = getContentResolver().insert(MediaStore.getMediaScannerUri(), contentValues);
                sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_STARTED", uri));
                try {
                    if (str.equals("external")) {
                        openDatabase(str);
                    }
                    mediaScanner = new MediaScanner(this, str);
                } catch (Exception e) {
                    Log.e("MediaScannerService", "exception in MediaScanner.scan()", e);
                }
            } catch (Exception e2) {
                Log.e("MediaScannerService", "exception in MediaScanner.scan()", e2);
                sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri));
                this.mWakeLock.release();
                if (!MediaUtils.LOG_SCAN) {
                    return;
                }
                str2 = "MediaScannerService";
                sb = new StringBuilder();
            }
            try {
                mediaScanner.scanDirectories(strArr);
                $closeResource(null, mediaScanner);
                getContentResolver().delete(uriInsert, null, null);
                sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri));
                this.mWakeLock.release();
                if (MediaUtils.LOG_SCAN) {
                    str2 = "MediaScannerService";
                    sb = new StringBuilder();
                    sb.append("scan(): volumeName = ");
                    sb.append(str);
                    sb.append(", directories = ");
                    sb.append(Arrays.toString(strArr));
                    Log.d(str2, sb.toString());
                }
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    th = th3;
                    th2 = th4;
                    $closeResource(th, mediaScanner);
                    throw th2;
                }
            }
        } catch (Throwable th5) {
            sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", uri));
            this.mWakeLock.release();
            if (MediaUtils.LOG_SCAN) {
                Log.d("MediaScannerService", "scan(): volumeName = " + str + ", directories = " + Arrays.toString(strArr));
            }
            throw th5;
        }
    }

    private void scanFolder(String[] strArr, String str) {
        String str2;
        StringBuilder sb;
        if (this.mWakeLock != null && !this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
        try {
            try {
                if (str.equals("external")) {
                    openDatabase(str);
                }
                MediaScannerExImpl mediaScannerExImpl = new MediaScannerExImpl(this, str);
                Throwable th = null;
                try {
                    mediaScannerExImpl.scanFolders(strArr, str, false);
                    if (this.mWakeLock != null && this.mWakeLock.isHeld() && this.mMediaScannerThreadPool == null) {
                        this.mWakeLock.release();
                    }
                } finally {
                    $closeResource(th, mediaScannerExImpl);
                }
            } catch (Throwable th2) {
                if (this.mWakeLock != null && this.mWakeLock.isHeld() && this.mMediaScannerThreadPool == null) {
                    this.mWakeLock.release();
                }
                if (MediaUtils.LOG_SCAN) {
                    Log.d("MediaScannerService", "scanFolder(): volumeName = " + str + ", folders = " + Arrays.toString(strArr));
                }
                throw th2;
            }
        } catch (Exception e) {
            Log.e("MediaScannerService", "exception in scanFolder", e);
            if (this.mWakeLock != null && this.mWakeLock.isHeld() && this.mMediaScannerThreadPool == null) {
                this.mWakeLock.release();
            }
            if (!MediaUtils.LOG_SCAN) {
                return;
            }
            str2 = "MediaScannerService";
            sb = new StringBuilder();
        }
        if (MediaUtils.LOG_SCAN) {
            str2 = "MediaScannerService";
            sb = new StringBuilder();
            sb.append("scanFolder(): volumeName = ");
            sb.append(str);
            sb.append(", folders = ");
            sb.append(Arrays.toString(strArr));
            Log.d(str2, sb.toString());
        }
    }

    @Override
    public void onCreate() {
        this.mWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(1, "MediaScannerService");
        this.mExternalStoragePaths = ((StorageManager) getSystemService("storage")).getVolumePaths();
        new Thread(null, this, "MediaScannerService").start();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_EJECT");
        intentFilter.addDataScheme("file");
        intentFilter.setPriority(100);
        registerReceiver(this.mUnmountReceiver, intentFilter);
        this.mIsThreadPoolEnable = getCpuCoreNum() >= 4 && !isLowRamDevice();
        Log.d("MediaScannerService", "onCreate: CpuCoreNum = " + getCpuCoreNum() + ", isLowRamDevice = " + isLowRamDevice() + ", mIsThreadPoolEnable = " + this.mIsThreadPoolEnable);
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) {
        while (this.mServiceHandler == null) {
            synchronized (this) {
                try {
                    wait(100L);
                } catch (InterruptedException e) {
                    Log.e("MediaScannerService", "onStartCommand: InterruptedException!");
                }
            }
        }
        if (intent == null) {
            Log.e("MediaScannerService", "Intent is null in onStartCommand: ", new NullPointerException());
            return 2;
        }
        this.mExternalStoragePaths = ((StorageManager) getSystemService("storage")).getVolumePaths();
        Bundle extras = intent.getExtras();
        if (extras.getString("filepath") != null) {
            this.mServiceHandler.sendMessage(this.mServiceHandler.obtainMessage(11, i2, -1, extras));
            return 3;
        }
        synchronized (this) {
            String string = ((Bundle) extras.clone()).getString("volume");
            ArrayList arrayList = new ArrayList();
            for (PrescanTask prescanTask : this.mPrescanTaskList) {
                String prescanVolume = prescanTask.getPrescanVolume();
                if (prescanVolume != null && string != null && prescanVolume.equals(string)) {
                    prescanTask.cancel(true);
                    arrayList.add(prescanTask);
                }
            }
            this.mPrescanTaskList.remove(arrayList);
            PrescanTask prescanTask2 = new PrescanTask(extras, this.mServiceHandler, i2);
            this.mPrescanTaskList.add(prescanTask2);
            prescanTask2.execute(new Void[0]);
        }
        return 3;
    }

    @Override
    public void onDestroy() {
        Log.d("MediaScannerService", "onDestroy");
        while (this.mServiceLooper == null) {
            synchronized (this) {
                try {
                    wait(100L);
                } catch (InterruptedException e) {
                    Log.e("MediaScannerService", "onDestroy: InterruptedException!");
                }
            }
        }
        this.mServiceLooper.quit();
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        unregisterReceiver(this.mUnmountReceiver);
        synchronized (this) {
            Iterator<PrescanTask> it = this.mPrescanTaskList.iterator();
            while (it.hasNext()) {
                it.next().cancel(true);
            }
            this.mPrescanTaskList.clear();
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(11);
        Looper.prepare();
        this.mServiceLooper = Looper.myLooper();
        this.mServiceHandler = new ServiceHandler();
        Process.setThreadPriority(11);
        Looper.loop();
    }

    private Uri scanFile(String str, String str2) throws Throwable {
        Throwable th;
        try {
            MediaScanner mediaScanner = new MediaScanner(this, "external");
            try {
                Uri uriScanSingleFile = mediaScanner.scanSingleFile(new File(str).getCanonicalPath(), str2);
                $closeResource(null, mediaScanner);
                return uriScanSingleFile;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                $closeResource(th, mediaScanner);
                throw th;
            }
        } catch (Exception e) {
            Log.e("MediaScannerService", "bad path " + str + " in scanFile()", e);
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
        public void handleMessage(Message message) throws Throwable {
            Bundle bundle = (Bundle) message.obj;
            if (MediaUtils.LOG_SCAN) {
                Log.v("MediaScannerService", "handleMessage: what = " + message.what + ", startId = " + message.arg1 + ", arguments = " + message.obj);
            }
            if (bundle != null && bundle.getBoolean("update_titles")) {
                try {
                    MediaScannerService.this.getBaseContext().getContentResolver().acquireContentProviderClient("media").call("update_titles", null, null);
                } catch (Exception e) {
                    Log.e("MediaScannerService", "Exception in mediaProvider call", e);
                }
            }
            switch (message.what) {
                case 10:
                    MediaScannerService.this.handleScanDirectory(message);
                    break;
                case 11:
                    MediaScannerService.this.handleScanSingleFile(message);
                    break;
                case 12:
                    MediaScannerService.this.handleShutdownThreadpool();
                    break;
                case 13:
                    MediaScannerService.this.handleScanFinish();
                    break;
                default:
                    Log.w("MediaScannerService", "unsupport message " + message.what);
                    break;
            }
        }
    }

    private void handleScanSingleFile(Message message) throws Throwable {
        IMediaScannerListener iMediaScannerListenerAsInterface;
        Bundle bundle = (Bundle) message.obj;
        String string = bundle.getString("filepath");
        try {
            IBinder iBinder = bundle.getIBinder("listener");
            Uri uriScanFile = null;
            if (iBinder != null) {
                iMediaScannerListenerAsInterface = IMediaScannerListener.Stub.asInterface(iBinder);
            } else {
                iMediaScannerListenerAsInterface = null;
            }
            try {
                if (new File(string).isDirectory()) {
                    scanFolder(new String[]{string}, "external");
                } else {
                    uriScanFile = scanFile(string, bundle.getString("mimetype"));
                }
            } catch (Exception e) {
                Log.e("MediaScannerService", "Exception scanning single file " + string, e);
            }
            if (iMediaScannerListenerAsInterface != null) {
                iMediaScannerListenerAsInterface.scanCompleted(string, uriScanFile);
            }
        } catch (Exception e2) {
            Log.e("MediaScannerService", "Exception in handleScanSingleFile", e2);
        }
        if (this.mStartId != -1) {
            stopSelfResult(this.mStartId);
            this.mStartId = message.arg1;
        } else {
            stopSelf(message.arg1);
        }
    }

    private void handleScanDirectory(Message message) {
        try {
            String string = ((Bundle) message.obj).getString("volume");
            String[] strArr = null;
            if ("internal".equals(string)) {
                strArr = new String[]{Environment.getRootDirectory() + "/media", Environment.getOemDirectory() + "/media", Environment.getProductDirectory() + "/media"};
            } else if ("external".equals(string)) {
                if (((UserManager) getSystemService(UserManager.class)).isDemoUser()) {
                    strArr = (String[]) ArrayUtils.appendElement(String.class, this.mExternalStoragePaths, Environment.getDataPreloadsMediaDirectory().getAbsolutePath());
                } else {
                    strArr = this.mExternalStoragePaths;
                }
                if (this.mIsThreadPoolEnable) {
                    this.mStartId = message.arg1;
                    if (this.mMediaScannerThreadPool == null) {
                        scanWithThreadPool(strArr, string);
                        return;
                    }
                    return;
                }
            }
            if (strArr != null) {
                scan(strArr, string);
            }
        } catch (Exception e) {
            Log.e("MediaScannerService", "Exception in handleScanDirectory", e);
        }
        if (this.mStartId != -1) {
            stopSelfResult(this.mStartId);
            this.mStartId = message.arg1;
        } else {
            stopSelf(message.arg1);
        }
    }

    private void scanWithThreadPool(String[] strArr, String str) {
        this.mScanStartTime = System.currentTimeMillis();
        this.mServiceHandler.removeMessages(10);
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("volume", str);
        getContentResolver().insert(MediaStore.getMediaScannerUri(), contentValues);
        openDatabase(str);
        initializeThreadPool(strArr, str);
        this.mPreScanFinishTime = System.currentTimeMillis();
        sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_STARTED", Uri.parse("file://" + this.mExternalStoragePaths[0])));
        this.mMediaScannerThreadPool.parseScanTask();
        if (MediaUtils.LOG_SCAN) {
            Log.v("MediaScannerService", "scanWithThreadPool() " + Arrays.toString(strArr));
        }
    }

    private void initializeThreadPool(String[] strArr, String str) {
        if (this.mMediaScannerThreadPool == null) {
            if (MediaUtils.LOG_SCAN) {
                Log.v("MediaScannerService", "initializeThreadPool() with creating new one");
            }
            this.mMediaScannerInserter = new MediaScannerInserter(this, this.mServiceHandler);
            this.mMediaScannerThreadPool = new MediaScannerThreadPool(this, strArr, this.mServiceHandler, this.mMediaScannerInserter.getInsertHandler());
        }
    }

    private void releaseThreadPool() {
        synchronized (this) {
            this.mMediaScannerInserter.release();
            this.mMediaScannerThreadPool = null;
            this.mMediaScannerInserter = null;
            if (MediaUtils.LOG_SCAN) {
                Log.v("MediaScannerService", "releaseThreadPool()");
            }
        }
    }

    private void handleShutdownThreadpool() {
        if (this.mMediaScannerThreadPool != null && !this.mMediaScannerThreadPool.isShutdown()) {
            if (MediaUtils.LOG_SCAN) {
                Log.v("MediaScannerService", "handleShutdownThreadpool()");
            }
            this.mMediaScannerThreadPool.shutdown();
        }
    }

    private void handleScanFinish() {
        try {
            try {
                this.mScanFinishTime = System.currentTimeMillis();
                MediaScannerExImpl mediaScannerExImpl = new MediaScannerExImpl(this, "external");
                Throwable th = null;
                try {
                    mediaScannerExImpl.postScanAll(this.mMediaScannerThreadPool.getPlaylistFilePaths());
                    $closeResource(null, mediaScannerExImpl);
                    getContentResolver().delete(MediaStore.getMediaScannerUri(), null, null);
                } catch (Throwable th2) {
                    $closeResource(th, mediaScannerExImpl);
                    throw th2;
                }
            } catch (Throwable th3) {
                if (MediaUtils.LOG_SCAN) {
                    Log.v("MediaScannerService", "handleScanFinish()");
                }
                throw th3;
            }
        } catch (Exception e) {
            Log.e("MediaScannerService", "Exception in handleScanFinish", e);
            if (MediaUtils.LOG_SCAN) {
            }
        }
        if (MediaUtils.LOG_SCAN) {
            Log.v("MediaScannerService", "handleScanFinish()");
        }
        this.mPostScanFinishTime = System.currentTimeMillis();
        sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_FINISHED", Uri.parse("file://" + this.mExternalStoragePaths[0])));
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        releaseThreadPool();
        stopSelfResult(this.mStartId);
        this.mStartId = -1;
    }

    private int getCpuCoreNum() {
        return Runtime.getRuntime().availableProcessors();
    }

    private boolean isLowRamDevice() {
        return ((ActivityManager) getSystemService("activity")).isLowRamDevice();
    }
}
