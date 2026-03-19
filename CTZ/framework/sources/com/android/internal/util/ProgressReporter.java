package com.android.internal.util;

import android.content.Intent;
import android.os.Bundle;
import android.os.IProgressListener;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.MathUtils;
import com.android.internal.annotations.GuardedBy;

public class ProgressReporter {
    private static final int STATE_FINISHED = 2;
    private static final int STATE_INIT = 0;
    private static final int STATE_STARTED = 1;
    private final int mId;

    @GuardedBy("this")
    private final RemoteCallbackList<IProgressListener> mListeners = new RemoteCallbackList<>();

    @GuardedBy("this")
    private int mState = 0;

    @GuardedBy("this")
    private int mProgress = 0;

    @GuardedBy("this")
    private Bundle mExtras = new Bundle();

    @GuardedBy("this")
    private int[] mSegmentRange = {0, 100};

    public ProgressReporter(int i) {
        this.mId = i;
    }

    public void addListener(IProgressListener iProgressListener) {
        if (iProgressListener == null) {
            return;
        }
        synchronized (this) {
            this.mListeners.register(iProgressListener);
            switch (this.mState) {
                case 1:
                    try {
                        iProgressListener.onStarted(this.mId, null);
                        iProgressListener.onProgress(this.mId, this.mProgress, this.mExtras);
                    } catch (RemoteException e) {
                    }
                    break;
                case 2:
                    try {
                        iProgressListener.onFinished(this.mId, null);
                    } catch (RemoteException e2) {
                    }
                    break;
            }
        }
    }

    public void setProgress(int i) {
        setProgress(i, 100, null);
    }

    public void setProgress(int i, CharSequence charSequence) {
        setProgress(i, 100, charSequence);
    }

    public void setProgress(int i, int i2) {
        setProgress(i, i2, null);
    }

    public void setProgress(int i, int i2, CharSequence charSequence) {
        synchronized (this) {
            if (this.mState != 1) {
                throw new IllegalStateException("Must be started to change progress");
            }
            this.mProgress = this.mSegmentRange[0] + MathUtils.constrain((i * this.mSegmentRange[1]) / i2, 0, this.mSegmentRange[1]);
            if (charSequence != null) {
                this.mExtras.putCharSequence(Intent.EXTRA_TITLE, charSequence);
            }
            notifyProgress(this.mId, this.mProgress, this.mExtras);
        }
    }

    public int[] startSegment(int i) {
        int[] iArr;
        synchronized (this) {
            iArr = this.mSegmentRange;
            this.mSegmentRange = new int[]{this.mProgress, (i * this.mSegmentRange[1]) / 100};
        }
        return iArr;
    }

    public void endSegment(int[] iArr) {
        synchronized (this) {
            this.mProgress = this.mSegmentRange[0] + this.mSegmentRange[1];
            this.mSegmentRange = iArr;
        }
    }

    int getProgress() {
        return this.mProgress;
    }

    int[] getSegmentRange() {
        return this.mSegmentRange;
    }

    public void start() {
        synchronized (this) {
            this.mState = 1;
            notifyStarted(this.mId, null);
            notifyProgress(this.mId, this.mProgress, this.mExtras);
        }
    }

    public void finish() {
        synchronized (this) {
            this.mState = 2;
            notifyFinished(this.mId, null);
            this.mListeners.kill();
        }
    }

    private void notifyStarted(int i, Bundle bundle) {
        for (int iBeginBroadcast = this.mListeners.beginBroadcast() - 1; iBeginBroadcast >= 0; iBeginBroadcast--) {
            try {
                ((IProgressListener) this.mListeners.getBroadcastItem(iBeginBroadcast)).onStarted(i, bundle);
            } catch (RemoteException e) {
            }
        }
        this.mListeners.finishBroadcast();
    }

    private void notifyProgress(int i, int i2, Bundle bundle) {
        for (int iBeginBroadcast = this.mListeners.beginBroadcast() - 1; iBeginBroadcast >= 0; iBeginBroadcast--) {
            try {
                ((IProgressListener) this.mListeners.getBroadcastItem(iBeginBroadcast)).onProgress(i, i2, bundle);
            } catch (RemoteException e) {
            }
        }
        this.mListeners.finishBroadcast();
    }

    private void notifyFinished(int i, Bundle bundle) {
        for (int iBeginBroadcast = this.mListeners.beginBroadcast() - 1; iBeginBroadcast >= 0; iBeginBroadcast--) {
            try {
                ((IProgressListener) this.mListeners.getBroadcastItem(iBeginBroadcast)).onFinished(i, bundle);
            } catch (RemoteException e) {
            }
        }
        this.mListeners.finishBroadcast();
    }
}
