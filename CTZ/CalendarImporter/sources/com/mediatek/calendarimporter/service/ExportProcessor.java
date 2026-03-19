package com.mediatek.calendarimporter.service;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.vcalendar.VCalComposer;
import com.mediatek.vcalendar.VCalStatusChangeOperator;

public class ExportProcessor extends BaseProcessor implements VCalStatusChangeOperator {
    public static final int FILE_MODE = 0;
    public static final int MEMORY_FILE_MODE = 1;
    private static final String TAG = "ExportProcessor";
    private final VCalComposer mComposer;
    private final int mMode;
    private final VCalService mService;
    private final Handler mUiHandler;

    public ExportProcessor(VCalService vCalService, Handler handler, Uri uri) throws IllegalArgumentException {
        this.mService = vCalService;
        long id = ContentUris.parseId(uri);
        if (id < 0) {
            LogUtils.e(TAG, "Constructor,The given eventId is inlegal or empty, eventId :" + id);
            throw new IllegalArgumentException(uri.toString());
        }
        String str = "_id=" + String.valueOf(id) + " AND deleted!=1";
        LogUtils.i(TAG, "Constructor: the going query selection = \"" + str + "\"");
        this.mUiHandler = handler;
        this.mComposer = new VCalComposer(this.mService, str, this);
        this.mMode = 1;
    }

    @Override
    public void run() {
        super.run();
        LogUtils.d(TAG, "ExportProcessor.run() has been called,mode=" + this.mMode);
        if (this.mMode == 1) {
            LogUtils.w(TAG, "ExportProcessor.run() MEMORY_FILE_MODE should not be called.");
        }
    }

    @Override
    public boolean cancel(boolean z) {
        LogUtils.i(TAG, "cancel,mayInterruptIfRunning:" + z);
        if (this.mComposer != null) {
            this.mComposer.cancelCurrentCompose();
        }
        return super.cancel(z);
    }

    @Override
    public void vCalOperationCanceled(int i, int i2) {
        LogUtils.i(TAG, "vCalOperationCanceled,finishedCnt:" + i + ",totalCnt:" + i2);
        Message messageObtain = Message.obtain();
        messageObtain.what = 2;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i2;
        this.mUiHandler.sendMessage(messageObtain);
    }

    @Override
    public void vCalOperationExceptionOccured(int i, int i2, int i3) {
        LogUtils.w(TAG, "vCalOperationExceptionOccured,finishedCnt:" + i + ",totalCnt:" + i2 + ",type:" + i3);
        Message messageObtain = Message.obtain();
        messageObtain.what = -1;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i2;
        this.mUiHandler.sendMessage(messageObtain);
    }

    @Override
    public void vCalProcessStatusUpdate(int i, int i2) {
        Message messageObtain = Message.obtain();
        messageObtain.what = 0;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i2;
        this.mUiHandler.sendMessage(messageObtain);
    }

    @Override
    public void vCalOperationStarted(int i) {
        LogUtils.d(TAG, "vCarOperationStarted: totalCnt: " + i);
    }

    @Override
    public void vCalOperationFinished(int i, int i2, Object obj) {
        LogUtils.i(TAG, "vCalOperationFinished: successCnt:" + i + ",totalCnt:" + i2);
        Message messageObtain = Message.obtain();
        messageObtain.what = 1;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i2;
        messageObtain.obj = obj;
        this.mUiHandler.sendMessage(messageObtain);
    }
}
