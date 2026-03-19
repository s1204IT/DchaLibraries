package com.mediatek.contacts.simservice;

import android.content.Intent;
import com.android.contacts.vcard.ProcessorBase;
import com.mediatek.contacts.simservice.SimProcessorManager;
import com.mediatek.contacts.util.Log;

public abstract class SimProcessorBase extends ProcessorBase {
    private volatile boolean mCanceled;
    private volatile boolean mDone;
    protected Intent mIntent;
    protected SimProcessorManager.ProcessorCompleteListener mListener;

    public abstract void doWork();

    public SimProcessorBase(Intent intent, SimProcessorManager.ProcessorCompleteListener processorCompleteListener) {
        this.mIntent = intent;
        this.mListener = processorCompleteListener;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public void run() {
        try {
            doWork();
        } finally {
            Log.d("SIMProcessorBase", "[run]finish: type = " + getType() + ",mDone = " + this.mDone + ",thread id = " + Thread.currentThread().getId());
            this.mDone = true;
            if (this.mListener != null && !this.mCanceled) {
                this.mListener.onProcessorCompleted(this.mIntent);
            }
        }
    }

    @Override
    public boolean cancel(boolean z) {
        if (this.mDone || this.mCanceled) {
            return false;
        }
        this.mCanceled = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return this.mCanceled;
    }

    @Override
    public boolean isDone() {
        return this.mDone;
    }

    public boolean identifyIntent(Intent intent) {
        return this.mIntent.equals(intent);
    }
}
