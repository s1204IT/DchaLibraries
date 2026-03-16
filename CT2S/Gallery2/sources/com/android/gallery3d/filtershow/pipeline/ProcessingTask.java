package com.android.gallery3d.filtershow.pipeline;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

public abstract class ProcessingTask {
    private Handler mProcessingHandler;
    private Handler mResultHandler;
    private ProcessingTaskController mTaskController;
    private int mType;

    interface Request {
    }

    interface Result {
    }

    interface Update {
    }

    public abstract Result doInBackground(Request request);

    public abstract void onResult(Result result);

    public boolean postRequest(Request message) {
        Message msg = this.mProcessingHandler.obtainMessage(this.mType);
        msg.obj = message;
        if (isPriorityTask()) {
            if (this.mProcessingHandler.hasMessages(getType())) {
                return false;
            }
            this.mProcessingHandler.sendMessageAtFrontOfQueue(msg);
        } else if (isDelayedTask()) {
            if (this.mProcessingHandler.hasMessages(getType())) {
                this.mProcessingHandler.removeMessages(getType());
            }
            this.mProcessingHandler.sendMessageDelayed(msg, 300L);
        } else {
            this.mProcessingHandler.sendMessage(msg);
        }
        return true;
    }

    public void postUpdate(Update message) {
        Message msg = this.mResultHandler.obtainMessage(this.mType);
        msg.obj = message;
        msg.arg1 = 2;
        this.mResultHandler.sendMessage(msg);
    }

    public void processRequest(Request message) {
        Result result = doInBackground(message);
        Message msg = this.mResultHandler.obtainMessage(this.mType);
        msg.obj = result;
        msg.arg1 = 1;
        this.mResultHandler.sendMessage(msg);
    }

    public void added(ProcessingTaskController taskController) {
        this.mTaskController = taskController;
        this.mResultHandler = taskController.getResultHandler();
        this.mProcessingHandler = taskController.getProcessingHandler();
        this.mType = taskController.getReservedType();
    }

    public int getType() {
        return this.mType;
    }

    public Context getContext() {
        return this.mTaskController.getContext();
    }

    public void onUpdate(Update message) {
    }

    public boolean isPriorityTask() {
        return false;
    }

    public boolean isDelayedTask() {
        return false;
    }
}
