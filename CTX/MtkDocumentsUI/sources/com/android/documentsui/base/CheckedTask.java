package com.android.documentsui.base;

import android.os.AsyncTask;

public abstract class CheckedTask<Input, Output> extends AsyncTask<Input, Void, Output> {
    private Check mCheck;

    @FunctionalInterface
    public interface Check {
        boolean stop();
    }

    protected abstract void finish(Output output);

    protected abstract Output run(Input... inputArr);

    public CheckedTask(Check check) {
        this.mCheck = check;
    }

    protected void prepare() {
    }

    @Override
    protected final void onPreExecute() {
        if (this.mCheck.stop()) {
            return;
        }
        prepare();
    }

    @Override
    protected final Output doInBackground(Input... inputArr) {
        if (this.mCheck.stop()) {
            return null;
        }
        return run(inputArr);
    }

    @Override
    protected final void onPostExecute(Output output) {
        if (this.mCheck.stop()) {
            return;
        }
        finish(output);
    }
}
