package com.android.browser;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class CheckMemoryTask extends AsyncTask<Object, Void, Void> {
    private Handler mHandler;

    public CheckMemoryTask(Handler handler) {
        this.mHandler = handler;
    }

    @Override
    protected Void doInBackground(Object... objArr) {
        if (objArr.length != 6) {
            Log.d("browser", "Incorrect parameters to CheckMemoryTask doInBackground(): " + String.valueOf(objArr.length));
            return null;
        }
        if (Performance.checkShouldReleaseTabs(((Integer) objArr[0]).intValue(), (ArrayList) objArr[1], ((Boolean) objArr[2]).booleanValue(), (String) objArr[3], (CopyOnWriteArrayList) objArr[4], ((Boolean) objArr[5]).booleanValue()) && this.mHandler != null && !this.mHandler.hasMessages(1100)) {
            this.mHandler.sendEmptyMessage(1100);
            return null;
        }
        return null;
    }
}
