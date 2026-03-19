package com.mediatek.calendarimporter.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.calendarimporter.utils.StringUtils;
import com.mediatek.vcalendar.VCalParser;
import com.mediatek.vcalendar.VCalStatusChangeOperator;
import java.io.FileNotFoundException;

public class ImportProcessor extends BaseProcessor implements VCalStatusChangeOperator {
    static final String TAG = "ImportProcessor";
    private boolean mIsBytesMode;
    private VCalParser mParser;
    private final Handler mUiHandler;

    public ImportProcessor(Context context, String str, Handler handler, Uri uri) {
        this.mIsBytesMode = false;
        this.mUiHandler = handler;
        if (StringUtils.isNullOrEmpty(str)) {
            this.mParser = new VCalParser(uri, context, this);
            return;
        }
        LogUtils.d(TAG, "The dst accountName :" + str);
        this.mParser = new VCalParser(uri, str, context, this);
    }

    public ImportProcessor(Context context, String str, Handler handler) throws FileNotFoundException {
        this.mIsBytesMode = false;
        if (str.length() <= 0) {
            LogUtils.e(TAG, "Constructor: the given vcsContent is empty.");
            throw new FileNotFoundException();
        }
        this.mUiHandler = handler;
        this.mIsBytesMode = true;
        this.mParser = new VCalParser(str, context, this);
    }

    @Override
    public void run() {
        super.run();
        if (this.mIsBytesMode) {
            LogUtils.d(TAG, "run: startParseVcsContent()");
            this.mParser.startParseVcsContent();
        } else {
            LogUtils.d(TAG, "run: mParser.startParse()");
            this.mParser.startParse();
        }
    }

    @Override
    public boolean cancel(boolean z) {
        if (this.mParser != null) {
            this.mParser.cancelCurrentParse();
            this.mParser.close();
        }
        return super.cancel(z);
    }

    @Override
    public void vCalOperationCanceled(int i, int i2) {
        LogUtils.i(TAG, "vCalOperationCanceled,finishedCnt:" + i + ",totalCnt:" + i2);
        this.mParser.close();
    }

    @Override
    public void vCalOperationExceptionOccured(int i, int i2, int i3) {
        LogUtils.w(TAG, "vCalOperationExceptionOccured,finishedCnt:" + i + ",totalCnt:" + i2 + ",type:" + i3);
        this.mParser.close();
        Message messageObtain = Message.obtain();
        messageObtain.what = -1;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i3;
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
        LogUtils.d(TAG, "vCalProcessStatusUpdate: totalCnt: " + i);
    }

    @Override
    public void vCalOperationFinished(int i, int i2, Object obj) {
        LogUtils.i(TAG, "vCalOperationFinished: successCnt:" + i + ",totalCnt:" + i2);
        this.mParser.close();
        Message messageObtain = Message.obtain();
        messageObtain.what = 1;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i2;
        messageObtain.obj = obj;
        this.mUiHandler.sendMessage(messageObtain);
    }
}
