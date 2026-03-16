package com.android.internal.os;

import android.os.DropBoxManager;
import android.util.Log;

class AndroidPrintStream extends LoggingPrintStream {
    private final int priority;
    private final String tag;

    public AndroidPrintStream(int priority, String tag) {
        if (tag == null) {
            throw new NullPointerException(DropBoxManager.EXTRA_TAG);
        }
        this.priority = priority;
        this.tag = tag;
    }

    @Override
    protected void log(String line) {
        Log.println(this.priority, this.tag, line);
    }
}
