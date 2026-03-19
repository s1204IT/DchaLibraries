package com.android.internal.os;

import android.os.DropBoxManager;
import android.util.Log;

class AndroidPrintStream extends LoggingPrintStream {
    private final int priority;
    private final String tag;

    public AndroidPrintStream(int i, String str) {
        if (str == null) {
            throw new NullPointerException(DropBoxManager.EXTRA_TAG);
        }
        this.priority = i;
        this.tag = str;
    }

    @Override
    protected void log(String str) {
        Log.println(this.priority, this.tag, str);
    }
}
