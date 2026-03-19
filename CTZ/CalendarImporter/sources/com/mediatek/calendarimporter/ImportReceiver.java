package com.mediatek.calendarimporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.android.common.speech.LoggingEvents;
import com.mediatek.calendarimporter.BindServiceHelper;
import com.mediatek.calendarimporter.service.ImportProcessor;
import com.mediatek.calendarimporter.service.VCalService;
import com.mediatek.calendarimporter.utils.LogUtils;
import java.io.FileNotFoundException;

public class ImportReceiver extends BroadcastReceiver implements BindServiceHelper.ServiceConnectedOperation {
    private static final String ACTION = "com.mtk.intent.action.RESTORE";
    private static final String ACTION_RESULT = "com.mtk.intent.action.RESTORE.RESULT";
    private static final String TAG = "ImportReceiver";
    private static final String VCS_CONTENT = "vcs_content";
    private Context mContext;
    private ImportProcessor mProcessor;
    private VCalService mService;
    private BindServiceHelper mServiceHelper;
    private String mVcsContent;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        this.mContext = context;
        Context applicationContext = this.mContext.getApplicationContext();
        LogUtils.i(TAG, "action = " + action);
        if (ACTION.equals(action)) {
            byte[] byteArrayExtra = intent.getByteArrayExtra(VCS_CONTENT);
            this.mVcsContent = byteArrayExtra != null ? new String(byteArrayExtra) : new String(LoggingEvents.EXTRA_CALLING_APP_NAME);
            LogUtils.d(TAG, "onReceive,file length: " + this.mVcsContent.length());
            this.mServiceHelper = new BindServiceHelper(applicationContext, this);
            LogUtils.d(TAG, "Context: " + applicationContext);
            this.mServiceHelper.onBindService();
        }
    }

    @Override
    public void serviceConnected(VCalService vCalService) {
        LogUtils.d(TAG, "Receiver: service:" + vCalService);
        this.mService = vCalService;
        try {
            this.mProcessor = new ImportProcessor(this.mService, this.mVcsContent, new Handler() {
                @Override
                public void handleMessage(Message message) {
                    Intent intent = new Intent(ImportReceiver.ACTION_RESULT);
                    int i = message.what;
                    if (i == -1) {
                        intent.putExtra("isSuccess", false);
                        ImportReceiver.this.mContext.sendBroadcast(intent);
                        return;
                    }
                    if (i == 1) {
                        intent.putExtra("event_title", ((Bundle) message.obj).getString("event_title", null));
                        if (message.arg1 == message.arg2) {
                            intent.putExtra("isSuccess", true);
                        } else {
                            intent.putExtra("isSuccess", false);
                        }
                        ImportReceiver.this.mContext.sendBroadcast(intent);
                        LogUtils.d(ImportReceiver.TAG, "sendBroadcast, action= " + intent.getAction());
                    }
                }
            });
        } catch (FileNotFoundException e) {
            LogUtils.e(TAG, "Can not create the Processor for a empty VcsContent.");
            e.printStackTrace();
        }
        this.mService.tryExecuteProcessor(this.mProcessor);
    }

    @Override
    public void serviceUnConnected() {
        this.mProcessor.cancel(false);
        this.mService = null;
    }
}
