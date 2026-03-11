package com.android.systemui.statusbar;

import android.content.res.Configuration;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.ServiceMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SystemBars extends SystemUI implements ServiceMonitor.Callbacks {
    private ServiceMonitor mServiceMonitor;
    private BaseStatusBar mStatusBar;

    @Override
    public void start() {
        this.mServiceMonitor = new ServiceMonitor("SystemBars", false, this.mContext, "bar_service_component", this);
        this.mServiceMonitor.start();
    }

    @Override
    public void onNoService() {
        createStatusBarFromConfig();
    }

    @Override
    public long onServiceStartAttempt() {
        if (this.mStatusBar != null) {
            this.mStatusBar.destroy();
            this.mStatusBar = null;
            return 500L;
        }
        return 0L;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (this.mStatusBar == null) {
            return;
        }
        this.mStatusBar.onConfigurationChanged(newConfig);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mStatusBar == null) {
            return;
        }
        this.mStatusBar.dump(fd, pw, args);
    }

    private void createStatusBarFromConfig() {
        String clsName = this.mContext.getString(R.string.config_statusBarComponent);
        if (clsName == null || clsName.length() == 0) {
            throw andLog("No status bar component configured", null);
        }
        try {
            Class<?> cls = this.mContext.getClassLoader().loadClass(clsName);
            try {
                this.mStatusBar = (BaseStatusBar) cls.newInstance();
                this.mStatusBar.mContext = this.mContext;
                this.mStatusBar.mComponents = this.mComponents;
                this.mStatusBar.start();
            } catch (Throwable t) {
                throw andLog("Error creating status bar component: " + clsName, t);
            }
        } catch (Throwable t2) {
            throw andLog("Error loading status bar component: " + clsName, t2);
        }
    }

    private RuntimeException andLog(String msg, Throwable t) {
        Log.w("SystemBars", msg, t);
        throw new RuntimeException(msg, t);
    }
}
