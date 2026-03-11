package com.android.browser;

import android.os.AsyncTask;
import android.util.Log;

public class OutputMemoryInfo extends AsyncTask<TabControl, Void, Void> {
    private String savedFileName;
    private TabControl tabController = null;
    private boolean logToFile = false;

    @Override
    public Void doInBackground(TabControl... params) {
        if (params.length != 2) {
            Log.d("browser", "Incorrect parameters to OutputMemoryInfo's doInBackground(): " + String.valueOf(params.length));
        } else {
            this.tabController = params[0];
            if (params.length == 2 && params[1] != null) {
                this.logToFile = true;
            }
            String flag = "BrowserMemory" + System.currentTimeMillis();
            this.savedFileName = Performance.printMemoryInfo(this.logToFile, flag);
        }
        return null;
    }
}
