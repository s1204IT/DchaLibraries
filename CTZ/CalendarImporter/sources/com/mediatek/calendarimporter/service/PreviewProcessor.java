package com.mediatek.calendarimporter.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import com.mediatek.calendarimporter.R;
import com.mediatek.calendarimporter.utils.LogUtils;
import com.mediatek.calendarimporter.utils.StringUtils;
import com.mediatek.vcalendar.ComponentPreviewInfo;
import com.mediatek.vcalendar.VCalParser;
import com.mediatek.vcalendar.VCalStatusChangeOperator;

public class PreviewProcessor extends BaseProcessor implements VCalStatusChangeOperator {
    private static final String TAG = "PreviewProcessor";
    private final Context mContext;
    private final VCalParser mParser;
    private final Handler mUiHandler;

    public PreviewProcessor(Context context, Uri uri, Handler handler) {
        this.mContext = context;
        this.mParser = new VCalParser(uri, context, this);
        this.mUiHandler = handler;
    }

    @Override
    public void run() {
        LogUtils.d(TAG, "PreviewProcessor.run()");
        super.run();
        this.mParser.startParsePreview();
    }

    @Override
    public boolean cancel(boolean z) {
        LogUtils.d(TAG, "cancel,mayInterruptIfRunning=" + z);
        this.mParser.close();
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
        this.mUiHandler.sendMessage(messageObtain);
    }

    @Override
    public void vCalProcessStatusUpdate(int i, int i2) {
    }

    @Override
    public void vCalOperationStarted(int i) {
        LogUtils.d(TAG, "vCarOperationStarted: totalCnt: " + i);
    }

    @Override
    public void vCalOperationFinished(int i, int i2, Object obj) {
        LogUtils.i(TAG, "vCarOperationStarted: successCnt: " + i + ",totalCnt:" + i2);
        this.mParser.close();
        ComponentPreviewInfo componentPreviewInfo = (ComponentPreviewInfo) obj;
        StringBuilder sb = new StringBuilder();
        switch (componentPreviewInfo.componentCount) {
            case ProcessorMsgType.PROCESSOR_EXCEPTION:
            case 0:
                LogUtils.w(TAG, "startParsePreview: No VEvent exsits in the file.");
                break;
            case 1:
                String str = componentPreviewInfo.eventSummary;
                String str2 = componentPreviewInfo.eventOrganizer;
                String str3 = componentPreviewInfo.eventDuration;
                String string = this.mContext.getResources().getString(R.string.null_name);
                if (StringUtils.isNullOrEmpty(str)) {
                    str = string;
                }
                if (StringUtils.isNullOrEmpty(str2)) {
                    str2 = string;
                }
                if (StringUtils.isNullOrEmpty(str3)) {
                    str3 = string;
                }
                sb.append(this.mContext.getResources().getString(R.string.title_lable) + str + "\n");
                sb.append(this.mContext.getResources().getString(R.string.calendar_lable) + str2 + "\n");
                StringBuilder sb2 = new StringBuilder();
                sb2.append(this.mContext.getResources().getString(R.string.date_lable));
                sb2.append(str3);
                sb.append(sb2.toString());
                break;
            default:
                sb.append("Events Count:");
                sb.append(componentPreviewInfo.componentCount);
                sb.append("\n");
                break;
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = 1;
        messageObtain.arg1 = i;
        messageObtain.arg2 = i2;
        messageObtain.obj = sb.toString();
        if (messageObtain.obj == null) {
            messageObtain.what = -1;
        }
        this.mUiHandler.sendMessage(messageObtain);
    }
}
