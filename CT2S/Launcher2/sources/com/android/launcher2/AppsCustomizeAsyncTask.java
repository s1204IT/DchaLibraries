package com.android.launcher2;

import android.os.AsyncTask;
import android.os.Process;
import com.android.launcher2.AsyncTaskPageData;

class AppsCustomizeAsyncTask extends AsyncTask<AsyncTaskPageData, Void, AsyncTaskPageData> {
    AsyncTaskPageData.Type dataType;
    int page;
    int threadPriority = 0;

    AppsCustomizeAsyncTask(int p, AsyncTaskPageData.Type ty) {
        this.page = p;
        this.dataType = ty;
    }

    @Override
    public AsyncTaskPageData doInBackground(AsyncTaskPageData... params) {
        if (params.length != 1) {
            return null;
        }
        params[0].doInBackgroundCallback.run(this, params[0]);
        return params[0];
    }

    @Override
    public void onPostExecute(AsyncTaskPageData result) {
        result.postExecuteCallback.run(this, result);
    }

    void setThreadPriority(int p) {
        this.threadPriority = p;
    }

    void syncThreadPriority() {
        Process.setThreadPriority(this.threadPriority);
    }
}
