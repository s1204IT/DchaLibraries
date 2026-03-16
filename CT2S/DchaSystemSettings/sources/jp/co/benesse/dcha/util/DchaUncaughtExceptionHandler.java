package jp.co.benesse.dcha.util;

import android.content.Context;
import android.os.Process;
import java.lang.Thread;

public class DchaUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Context mAppContext;

    public DchaUncaughtExceptionHandler(Context appContext) {
        this.mAppContext = appContext;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            Logger.e(this.mAppContext, "uncaughtException", ex);
        } finally {
            Process.killProcess(Process.myPid());
        }
    }
}
