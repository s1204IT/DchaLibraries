package com.android.browser;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CrashRecoveryHandler {
    private static CrashRecoveryHandler sInstance;
    private Context mContext;
    private Controller mController;
    private boolean mIsPreloading = false;
    private boolean mDidPreload = false;
    private Bundle mRecoveryState = null;
    private Runnable mCreateState = new Runnable() {
        @Override
        public void run() {
            try {
                Message.obtain(CrashRecoveryHandler.this.mBackgroundHandler, 1, CrashRecoveryHandler.this.mController.createSaveState()).sendToTarget();
                CrashRecoveryHandler.this.mForegroundHandler.removeCallbacks(CrashRecoveryHandler.this.mCreateState);
            } catch (Throwable th) {
                Log.w("BrowserCrashRecovery", "Failed to save state", th);
            }
        }
    };
    private Handler mForegroundHandler = new Handler();
    private Handler mBackgroundHandler = new Handler(BackgroundHandler.getLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    CrashRecoveryHandler.this.writeState((Bundle) message.obj);
                    return;
                case 2:
                    File file = new File(CrashRecoveryHandler.this.mContext.getCacheDir(), "browser_state.parcel");
                    if (file.exists()) {
                        file.delete();
                        return;
                    }
                    return;
                case 3:
                    CrashRecoveryHandler.this.mRecoveryState = CrashRecoveryHandler.this.loadCrashState();
                    synchronized (CrashRecoveryHandler.this) {
                        CrashRecoveryHandler.this.mIsPreloading = false;
                        CrashRecoveryHandler.this.mDidPreload = true;
                        CrashRecoveryHandler.this.notifyAll();
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    };

    public static CrashRecoveryHandler initialize(Controller controller) {
        if (sInstance == null) {
            sInstance = new CrashRecoveryHandler(controller);
        } else {
            sInstance.mController = controller;
        }
        return sInstance;
    }

    private CrashRecoveryHandler(Controller controller) {
        this.mController = controller;
        this.mContext = this.mController.getActivity().getApplicationContext();
    }

    public void backupState() {
        this.mForegroundHandler.postDelayed(this.mCreateState, 500L);
    }

    public void clearState() {
        this.mBackgroundHandler.sendEmptyMessage(2);
        updateLastRecovered(0L);
    }

    private boolean shouldRestore() {
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        return System.currentTimeMillis() - browserSettings.getLastRecovered() > 300000 || browserSettings.wasLastRunPaused();
    }

    private void updateLastRecovered(long j) {
        BrowserSettings.getInstance().setLastRecovered(j);
    }

    private synchronized Bundle loadCrashState() {
        FileInputStream fileInputStream;
        ByteArrayOutputStream byteArrayOutputStream;
        byte[] bArr;
        Parcel parcelShouldRestore = shouldRestore();
        FileInputStream fileInputStream2 = null;
        if (parcelShouldRestore == 0) {
            return null;
        }
        try {
            BrowserSettings.getInstance().setLastRunPaused(false);
            parcelShouldRestore = Parcel.obtain();
        } catch (Throwable th) {
            th = th;
        }
        try {
            fileInputStream = new FileInputStream(new File(this.mContext.getCacheDir(), "browser_state.parcel"));
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                bArr = new byte[4096];
            } catch (FileNotFoundException e) {
                parcelShouldRestore.recycle();
                if (fileInputStream != null) {
                }
                return null;
            } catch (Throwable th2) {
                th = th2;
                Log.w("BrowserCrashRecovery", "Failed to recover state!", th);
                parcelShouldRestore.recycle();
                if (fileInputStream != null) {
                }
                return null;
            }
        } catch (FileNotFoundException e2) {
            fileInputStream = null;
        } catch (Throwable th3) {
            th = th3;
            fileInputStream = null;
        }
        while (true) {
            int i = fileInputStream.read(bArr);
            if (i <= 0) {
                break;
            }
            byteArrayOutputStream.write(bArr, 0, i);
            fileInputStream.close();
            return null;
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        parcelShouldRestore.unmarshall(byteArray, 0, byteArray.length);
        parcelShouldRestore.setDataPosition(0);
        Bundle bundle = parcelShouldRestore.readBundle();
        if (bundle != null) {
            if (!bundle.isEmpty()) {
                parcelShouldRestore.recycle();
                try {
                    fileInputStream.close();
                } catch (IOException e3) {
                }
                return bundle;
            }
        }
        parcelShouldRestore.recycle();
        fileInputStream.close();
        return null;
    }

    public void startRecovery(Intent intent) {
        synchronized (this) {
            while (this.mIsPreloading) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
        if (!this.mDidPreload) {
            this.mRecoveryState = loadCrashState();
        }
        updateLastRecovered(this.mRecoveryState != null ? System.currentTimeMillis() : 0L);
        this.mController.doStart(this.mRecoveryState, intent);
        this.mRecoveryState = null;
    }

    public void preloadCrashState() {
        synchronized (this) {
            if (this.mIsPreloading) {
                return;
            }
            this.mIsPreloading = true;
            this.mBackgroundHandler.sendEmptyMessage(3);
        }
    }

    synchronized void writeState(Bundle bundle) {
        Parcel parcelObtain = Parcel.obtain();
        try {
            try {
                bundle.writeToParcel(parcelObtain, 0);
                File file = new File(this.mContext.getCacheDir(), "browser_state.parcel.journal");
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(parcelObtain.marshall());
                fileOutputStream.close();
                File file2 = new File(this.mContext.getCacheDir(), "browser_state.parcel");
                if (!file.renameTo(file2)) {
                    file2.delete();
                    file.renameTo(file2);
                }
            } catch (Throwable th) {
                Log.i("BrowserCrashRecovery", "Failed to save persistent state", th);
            }
        } finally {
            parcelObtain.recycle();
        }
    }
}
