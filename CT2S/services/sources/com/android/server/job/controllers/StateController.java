package com.android.server.job.controllers;

import android.content.Context;
import com.android.server.job.StateChangedListener;
import java.io.PrintWriter;

public abstract class StateController {
    protected static final boolean DEBUG = false;
    protected Context mContext;
    protected StateChangedListener mStateChangedListener;

    public abstract void dumpControllerState(PrintWriter printWriter);

    public abstract void maybeStartTrackingJob(JobStatus jobStatus);

    public abstract void maybeStopTrackingJob(JobStatus jobStatus);

    public StateController(StateChangedListener stateChangedListener, Context context) {
        this.mStateChangedListener = stateChangedListener;
        this.mContext = context;
    }
}
