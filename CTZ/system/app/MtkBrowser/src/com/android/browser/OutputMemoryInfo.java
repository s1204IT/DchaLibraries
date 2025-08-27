package com.android.browser;

import android.os.AsyncTask;
import android.util.Log;

/* loaded from: classes.dex */
public class OutputMemoryInfo extends AsyncTask<TabControl, Void, Void> {
    private String savedFileName;
    private TabControl tabController = null;
    private boolean logToFile = false;

    /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
    @Override // android.os.AsyncTask
    protected Void doInBackground(TabControl... tabControlArr) {
        if (tabControlArr.length != 2) {
            Log.d("browser", "Incorrect parameters to OutputMemoryInfo's doInBackground(): " + String.valueOf(tabControlArr.length));
            return null;
        }
        this.tabController = tabControlArr[0];
        if (tabControlArr.length == 2 && tabControlArr[1] != null) {
            this.logToFile = true;
        }
        this.savedFileName = Performance.printMemoryInfo(this.logToFile, "BrowserMemory" + System.currentTimeMillis());
        return null;
    }
}
