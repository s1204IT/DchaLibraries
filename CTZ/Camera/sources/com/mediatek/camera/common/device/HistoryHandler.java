package com.mediatek.camera.common.device;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import java.util.LinkedList;

public abstract class HistoryHandler extends Handler {
    private final LinkedList<Integer> mMsgHistory;
    protected long mMsgStartTime;
    protected long mMsgStopTime;

    protected abstract void doHandleMessage(Message message);

    protected HistoryHandler(Looper looper) {
        super(looper);
        this.mMsgHistory = new LinkedList<>();
        this.mMsgHistory.offerLast(-1);
    }

    @Override
    public void handleMessage(Message message) {
        this.mMsgHistory.offerLast(Integer.valueOf(message.what));
        while (this.mMsgHistory.size() > 400) {
            this.mMsgHistory.pollFirst();
        }
    }

    protected void printStartMsg(String str, String str2, long j) {
        Log.i(str, "[" + str2 + "]+, pending time = " + j + "ms.");
    }

    protected void printStopMsg(String str, String str2, long j) {
        Log.i(str, "[" + str2 + "]-, executing time = " + j + "ms.");
    }

    protected String generateHistoryString(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Begin is:");
        sb.append("camera id:");
        sb.append(i);
        for (Integer num : this.mMsgHistory) {
            sb.append("_");
            sb.append(num);
        }
        sb.append("End");
        return sb.toString();
    }
}
