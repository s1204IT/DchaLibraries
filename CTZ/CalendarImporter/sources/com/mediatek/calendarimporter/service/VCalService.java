package com.mediatek.calendarimporter.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import com.mediatek.calendarimporter.utils.LogUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class VCalService extends Service {
    private static final String TAG = "VCalService";
    private MyBinder mBinder = null;
    public final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    public class MyBinder extends Binder {
        public MyBinder() {
        }

        public VCalService getService() {
            return VCalService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBinder = new MyBinder();
        LogUtils.d(TAG, "VCalService onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.d(TAG, "VCalService onBind");
        return this.mBinder;
    }

    public void onFinish() {
        super.onDestroy();
    }

    public void disconnected(String str) {
        LogUtils.i(TAG, "disconnected, the context = " + str);
    }

    public void tryExecuteProcessor(BaseProcessor baseProcessor) {
        LogUtils.d(TAG, "VCalService tryExecuteProcessor");
        tryExecute(baseProcessor);
    }

    public void tryCancelProcessor(BaseProcessor baseProcessor) {
        LogUtils.d(TAG, "VCalService tryCancelProcessor");
        if (baseProcessor == null) {
            LogUtils.w(TAG, "The processor going to cancel is null");
        } else {
            baseProcessor.cancel(true);
        }
    }

    private synchronized boolean tryExecute(BaseProcessor baseProcessor) {
        try {
            this.mExecutorService.execute(baseProcessor);
        } catch (RejectedExecutionException e) {
            LogUtils.e(TAG, "tryExecute: RejectedExecutionException.");
            return false;
        }
        return true;
    }
}
