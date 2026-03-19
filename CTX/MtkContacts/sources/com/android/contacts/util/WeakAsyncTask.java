package com.android.contacts.util;

import android.os.AsyncTask;
import java.lang.ref.WeakReference;

public abstract class WeakAsyncTask<Params, Progress, Result, WeakTarget> extends AsyncTask<Params, Progress, Result> {
    protected WeakReference<WeakTarget> mTarget;

    protected abstract Result doInBackground(WeakTarget weaktarget, Params... paramsArr);

    public WeakAsyncTask(WeakTarget weaktarget) {
        this.mTarget = new WeakReference<>(weaktarget);
    }

    @Override
    protected final void onPreExecute() {
        WeakTarget weaktarget = this.mTarget.get();
        if (weaktarget != null) {
            onPreExecute(weaktarget);
        }
    }

    @Override
    protected final Result doInBackground(Params... paramsArr) {
        WeakTarget weaktarget = this.mTarget.get();
        if (weaktarget != null) {
            return doInBackground(weaktarget, paramsArr);
        }
        return null;
    }

    @Override
    protected final void onPostExecute(Result result) {
        WeakTarget weaktarget = this.mTarget.get();
        if (weaktarget != null) {
            onPostExecute(weaktarget, result);
        }
    }

    protected void onPreExecute(WeakTarget weaktarget) {
    }

    protected void onPostExecute(WeakTarget weaktarget, Result result) {
    }
}
