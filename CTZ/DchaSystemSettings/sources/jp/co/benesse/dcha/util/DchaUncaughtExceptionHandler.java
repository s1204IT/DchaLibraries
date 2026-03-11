package jp.co.benesse.dcha.util;

import android.content.Context;
import android.os.Process;
import java.lang.Thread;

public class DchaUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Context mAppContext;

    public DchaUncaughtExceptionHandler(Context context) {
        this.mAppContext = context;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable th) {
        try {
            Logger.e(this.mAppContext, "uncaughtException", th);
        } finally {
            Process.killProcess(Process.myPid());
        }
    }
}
