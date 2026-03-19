package com.android.bips.ipp;

import android.os.AsyncTask;

class CancelJobTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = CancelJobTask.class.getSimpleName();
    private final Backend mBackend;
    private final int mJobId;

    CancelJobTask(Backend backend, int i) {
        this.mBackend = backend;
        this.mJobId = i;
    }

    @Override
    protected Void doInBackground(Void... voidArr) {
        this.mBackend.nativeCancelJob(this.mJobId);
        return null;
    }
}
