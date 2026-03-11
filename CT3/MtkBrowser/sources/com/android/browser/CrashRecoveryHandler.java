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
                Bundle state = CrashRecoveryHandler.this.mController.createSaveState();
                Message.obtain(CrashRecoveryHandler.this.mBackgroundHandler, 1, state).sendToTarget();
                CrashRecoveryHandler.this.mForegroundHandler.removeCallbacks(CrashRecoveryHandler.this.mCreateState);
            } catch (Throwable t) {
                Log.w("BrowserCrashRecovery", "Failed to save state", t);
            }
        }
    };
    private Handler mForegroundHandler = new Handler();
    private Handler mBackgroundHandler = new Handler(BackgroundHandler.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Bundle saveState = (Bundle) msg.obj;
                    CrashRecoveryHandler.this.writeState(saveState);
                    return;
                case 2:
                    File state = new File(CrashRecoveryHandler.this.mContext.getCacheDir(), "browser_state.parcel");
                    if (!state.exists()) {
                        return;
                    }
                    state.delete();
                    return;
                case 3:
                    CrashRecoveryHandler.this.mRecoveryState = CrashRecoveryHandler.this.loadCrashState();
                    synchronized (CrashRecoveryHandler.this) {
                        CrashRecoveryHandler.this.mIsPreloading = false;
                        CrashRecoveryHandler.this.mDidPreload = true;
                        CrashRecoveryHandler.this.notifyAll();
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
        long lastRecovered = browserSettings.getLastRecovered();
        long timeSinceLastRecover = System.currentTimeMillis() - lastRecovered;
        if (timeSinceLastRecover <= 300000) {
            return browserSettings.wasLastRunPaused();
        }
        return true;
    }

    private void updateLastRecovered(long time) {
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        browserSettings.setLastRecovered(time);
    }

    public synchronized Bundle loadCrashState() {
        FileInputStream fin;
        if (!shouldRestore()) {
            return null;
        }
        BrowserSettings browserSettings = BrowserSettings.getInstance();
        browserSettings.setLastRunPaused(false);
        Parcel parcel = Parcel.obtain();
        FileInputStream fin2 = null;
        try {
            try {
                File stateFile = new File(this.mContext.getCacheDir(), "browser_state.parcel");
                fin = new FileInputStream(stateFile);
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
        } catch (Throwable th2) {
            e = th2;
        }
        try {
            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (true) {
                int read = fin.read(buffer);
                if (read <= 0) {
                    break;
                }
                dataStream.write(buffer, 0, read);
            }
            byte[] data = dataStream.toByteArray();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            Bundle state = parcel.readBundle();
            if (state != null) {
                if (!state.isEmpty()) {
                    parcel.recycle();
                    if (fin != null) {
                        try {
                            fin.close();
                        } catch (IOException e2) {
                        }
                    }
                    return state;
                }
            }
            parcel.recycle();
            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e3) {
                }
            }
            fin2 = fin;
        } catch (FileNotFoundException e4) {
            fin2 = fin;
            parcel.recycle();
            if (fin2 != null) {
                try {
                    fin2.close();
                } catch (IOException e5) {
                }
            }
            return null;
        } catch (Throwable th3) {
            th = th3;
            fin2 = fin;
            parcel.recycle();
            if (fin2 != null) {
                try {
                    fin2.close();
                } catch (IOException e6) {
                }
            }
            throw th;
        }
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

    synchronized void writeState(Bundle state) {
        Parcel p = Parcel.obtain();
        try {
            try {
                state.writeToParcel(p, 0);
                File stateJournal = new File(this.mContext.getCacheDir(), "browser_state.parcel.journal");
                FileOutputStream fout = new FileOutputStream(stateJournal);
                fout.write(p.marshall());
                fout.close();
                File stateFile = new File(this.mContext.getCacheDir(), "browser_state.parcel");
                if (!stateJournal.renameTo(stateFile)) {
                    stateFile.delete();
                    stateJournal.renameTo(stateFile);
                }
            } finally {
                p.recycle();
            }
        } catch (Throwable e) {
            Log.i("BrowserCrashRecovery", "Failed to save persistent state", e);
            p.recycle();
        }
    }
}
