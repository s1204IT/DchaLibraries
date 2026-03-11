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
    private Runnable mCreateState = new Runnable(this) {
        final CrashRecoveryHandler this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void run() {
            try {
                Message.obtain(this.this$0.mBackgroundHandler, 1, this.this$0.mController.createSaveState()).sendToTarget();
                this.this$0.mForegroundHandler.removeCallbacks(this.this$0.mCreateState);
            } catch (Throwable th) {
                Log.w("BrowserCrashRecovery", "Failed to save state", th);
            }
        }
    };
    private Handler mForegroundHandler = new Handler();
    private Handler mBackgroundHandler = new Handler(this, BackgroundHandler.getLooper()) {
        final CrashRecoveryHandler this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.this$0.writeState((Bundle) message.obj);
                    return;
                case 2:
                    File file = new File(this.this$0.mContext.getCacheDir(), "browser_state.parcel");
                    if (file.exists()) {
                        file.delete();
                        return;
                    }
                    return;
                case 3:
                    this.this$0.mRecoveryState = this.this$0.loadCrashState();
                    synchronized (this.this$0) {
                        this.this$0.mIsPreloading = false;
                        this.this$0.mDidPreload = true;
                        this.this$0.notifyAll();
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    };

    private CrashRecoveryHandler(Controller controller) {
        this.mController = controller;
        this.mContext = this.mController.getActivity().getApplicationContext();
    }

    public static CrashRecoveryHandler initialize(Controller controller) {
        if (sInstance == null) {
            sInstance = new CrashRecoveryHandler(controller);
        } else {
            sInstance.mController = controller;
        }
        return sInstance;
    }

    public Bundle loadCrashState() {
        FileInputStream fileInputStream;
        Parcel parcelObtain;
        FileInputStream fileInputStream2;
        ByteArrayOutputStream byteArrayOutputStream;
        byte[] bArr;
        FileInputStream fileInputStream3 = null;
        synchronized (this) {
            if (!shouldRestore()) {
                return null;
            }
            try {
                BrowserSettings.getInstance().setLastRunPaused(false);
                parcelObtain = Parcel.obtain();
            } catch (Throwable th) {
                th = th;
                fileInputStream3 = fileInputStream;
            }
            try {
                fileInputStream2 = new FileInputStream(new File(this.mContext.getCacheDir(), "browser_state.parcel"));
                try {
                    byteArrayOutputStream = new ByteArrayOutputStream();
                    bArr = new byte[4096];
                } catch (FileNotFoundException e) {
                    parcelObtain.recycle();
                    if (fileInputStream2 != null) {
                    }
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    Log.w("BrowserCrashRecovery", "Failed to recover state!", th);
                    parcelObtain.recycle();
                    if (fileInputStream2 != null) {
                    }
                    return null;
                }
            } catch (FileNotFoundException e2) {
                fileInputStream2 = null;
            } catch (Throwable th3) {
                th = th3;
                fileInputStream2 = null;
            }
            while (true) {
                int i = fileInputStream2.read(bArr);
                if (i <= 0) {
                    break;
                }
                byteArrayOutputStream.write(bArr, 0, i);
                fileInputStream2.close();
                return null;
            }
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            parcelObtain.unmarshall(byteArray, 0, byteArray.length);
            parcelObtain.setDataPosition(0);
            Bundle bundle = parcelObtain.readBundle();
            if (bundle != null) {
                if (!bundle.isEmpty()) {
                    parcelObtain.recycle();
                    try {
                        fileInputStream2.close();
                    } catch (IOException e3) {
                    }
                    return bundle;
                }
            }
            parcelObtain.recycle();
            fileInputStream2.close();
            return null;
        }
    }

    private boolean shouldRestore() {
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        return System.currentTimeMillis() - browserSettings.getLastRecovered() > 300000 || browserSettings.wasLastRunPaused();
    }

    private void updateLastRecovered(long j) {
        BrowserSettings.getInstance().setLastRecovered(j);
    }

    public void backupState() {
        this.mForegroundHandler.postDelayed(this.mCreateState, 500L);
    }

    public void clearState() {
        this.mBackgroundHandler.sendEmptyMessage(2);
        updateLastRecovered(0L);
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

    void writeState(Bundle bundle) {
        synchronized (this) {
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
}
