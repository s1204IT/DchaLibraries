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
    public Void doInBackground(Object... params) {
        if (params.length != 6) {
            Log.d("browser", "Incorrect parameters to CheckMemoryTask doInBackground(): " + String.valueOf(params.length));
        } else {
            int visibleWebviewNums = ((Integer) params[0]).intValue();
            ArrayList<Integer> tabIndex = (ArrayList) params[1];
            boolean isFreeMemory = ((Boolean) params[2]).booleanValue();
            String url = (String) params[3];
            CopyOnWriteArrayList<Integer> freeTabIndexs = (CopyOnWriteArrayList) params[4];
            boolean isRemoveTab = ((Boolean) params[5]).booleanValue();
            boolean shouldReleaseTab = Performance.checkShouldReleaseTabs(visibleWebviewNums, tabIndex, isFreeMemory, url, freeTabIndexs, isRemoveTab);
            if (shouldReleaseTab && this.mHandler != null && !this.mHandler.hasMessages(1100)) {
                this.mHandler.sendEmptyMessage(1100);
            }
        }
        return null;
    }
}
