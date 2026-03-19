package com.android.server.backup.internal;

public interface OnTaskFinishedListener {
    public static final OnTaskFinishedListener NOP = new OnTaskFinishedListener() {
        @Override
        public final void onFinished(String str) {
            OnTaskFinishedListener.lambda$static$0(str);
        }
    };

    void onFinished(String str);

    static void lambda$static$0(String str) {
    }
}
