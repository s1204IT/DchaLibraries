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

    public boolean postRequest(Request request) {
        Message messageObtainMessage = this.mProcessingHandler.obtainMessage(this.mType);
        messageObtainMessage.obj = request;
        if (isPriorityTask()) {
            if (this.mProcessingHandler.hasMessages(getType())) {
                return false;
            }
            this.mProcessingHandler.sendMessageAtFrontOfQueue(messageObtainMessage);
            return true;
        }
        if (isDelayedTask()) {
            if (this.mProcessingHandler.hasMessages(getType())) {
                this.mProcessingHandler.removeMessages(getType());
            }
            this.mProcessingHandler.sendMessageDelayed(messageObtainMessage, 150L);
            return true;
        }
        this.mProcessingHandler.sendMessage(messageObtainMessage);
        return true;
    }

    public void postUpdate(Update update) {
        Message messageObtainMessage = this.mResultHandler.obtainMessage(this.mType);
        messageObtainMessage.obj = update;
        messageObtainMessage.arg1 = 2;
        this.mResultHandler.sendMessage(messageObtainMessage);
    }

    public void processRequest(Request request) {
        Result resultDoInBackground = doInBackground(request);
        Message messageObtainMessage = this.mResultHandler.obtainMessage(this.mType);
        messageObtainMessage.obj = resultDoInBackground;
        messageObtainMessage.arg1 = 1;
        this.mResultHandler.sendMessage(messageObtainMessage);
    }

    public void added(ProcessingTaskController processingTaskController) {
        this.mTaskController = processingTaskController;
        this.mResultHandler = processingTaskController.getResultHandler();
        this.mProcessingHandler = processingTaskController.getProcessingHandler();
        this.mType = processingTaskController.getReservedType();
    }

    public int getType() {
        return this.mType;
    }

    public Context getContext() {
        return this.mTaskController.getContext();
    }

    public boolean isProcessingTaskBusy() {
        return this.mProcessingHandler.hasMessages(1) || this.mProcessingHandler.hasMessages(2) || this.mProcessingHandler.hasMessages(4);
    }

    public void onUpdate(Update update) {
    }

    public boolean isPriorityTask() {
        return false;
    }

    public boolean isDelayedTask() {
        return false;
    }
}
