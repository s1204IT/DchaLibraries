package com.android.ex.camera2.portability;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.LinkedList;

class HistoryHandler extends Handler {
    private static final int MAX_HISTORY_SIZE = 400;
    final LinkedList<Integer> mMsgHistory;

    HistoryHandler(Looper looper) {
        super(looper);
        this.mMsgHistory = new LinkedList<>();
        this.mMsgHistory.offerLast(-1);
    }

    Integer getCurrentMessage() {
        return this.mMsgHistory.peekLast();
    }

    String generateHistoryString(int cameraId) {
        String info = new String("HIST");
        String info2 = info + "_ID" + cameraId;
        for (Integer msg : this.mMsgHistory) {
            info2 = info2 + '_' + msg.toString();
        }
        return info2 + "_HEND";
    }

    @Override
    public void handleMessage(Message msg) {
        this.mMsgHistory.offerLast(Integer.valueOf(msg.what));
        while (this.mMsgHistory.size() > 400) {
            this.mMsgHistory.pollFirst();
        }
    }
}
