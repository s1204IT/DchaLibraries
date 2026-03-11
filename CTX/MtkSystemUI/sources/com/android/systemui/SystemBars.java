package com.android.systemui;

import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SystemBars extends SystemUI {
    private SystemUI mStatusBar;

    @Override
    public void start() {
        createStatusBarFromConfig();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mStatusBar != null) {
            this.mStatusBar.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private void createStatusBarFromConfig() {
        String string = this.mContext.getString(R.string.config_statusBarComponent);
        if (string == null || string.length() == 0) {
            throw andLog("No status bar component configured", null);
        }
        try {
            try {
                this.mStatusBar = (SystemUI) this.mContext.getClassLoader().loadClass(string).newInstance();
                this.mStatusBar.mContext = this.mContext;
                this.mStatusBar.mComponents = this.mComponents;
                this.mStatusBar.start();
            } catch (Throwable th) {
                throw andLog("Error creating status bar component: " + string, th);
            }
        } catch (Throwable th2) {
            throw andLog("Error loading status bar component: " + string, th2);
        }
    }

    private RuntimeException andLog(String str, Throwable th) {
        Log.w("SystemBars", str, th);
        throw new RuntimeException(str, th);
    }
}
