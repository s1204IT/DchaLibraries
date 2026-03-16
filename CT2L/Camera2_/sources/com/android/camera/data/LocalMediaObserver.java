package com.android.camera.data;

import android.database.ContentObserver;

public class LocalMediaObserver extends ContentObserver {
    private boolean mActivityPaused;
    private ChangeListener mChangeListener;
    private boolean mMediaDataChangedDuringPause;

    public interface ChangeListener {
        void onChange();
    }

    public LocalMediaObserver() {
        super(null);
        this.mActivityPaused = false;
        this.mMediaDataChangedDuringPause = false;
    }

    public void setForegroundChangeListener(ChangeListener changeListener) {
        this.mChangeListener = changeListener;
    }

    public void removeForegroundChangeListener() {
        this.mChangeListener = null;
    }

    @Override
    public void onChange(boolean selfChange) {
        if (this.mChangeListener != null) {
            this.mChangeListener.onChange();
        }
        if (this.mActivityPaused) {
            this.mMediaDataChangedDuringPause = true;
        }
    }

    public void setActivityPaused(boolean paused) {
        this.mActivityPaused = paused;
        if (!paused) {
            this.mMediaDataChangedDuringPause = false;
        }
    }

    public boolean isMediaDataChangedDuringPause() {
        return this.mMediaDataChangedDuringPause;
    }
}
