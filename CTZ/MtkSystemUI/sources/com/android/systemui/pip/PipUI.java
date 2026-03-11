package com.android.systemui.pip;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import com.android.systemui.SystemUI;
import com.android.systemui.pip.tv.PipManager;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.CommandQueue;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class PipUI extends SystemUI implements CommandQueue.Callbacks {
    private BasePipManager mPipManager;
    private boolean mSupportsPip;

    @Override
    public void start() {
        BasePipManager pipManager;
        PackageManager packageManager = this.mContext.getPackageManager();
        this.mSupportsPip = packageManager.hasSystemFeature("android.software.picture_in_picture");
        if (!this.mSupportsPip) {
            return;
        }
        if (!SystemServicesProxy.getInstance(this.mContext).isSystemUser(SystemServicesProxy.getInstance(this.mContext).getProcessUser())) {
            throw new IllegalStateException("Non-primary Pip component not currently supported.");
        }
        if (packageManager.hasSystemFeature("android.software.leanback_only")) {
            pipManager = PipManager.getInstance();
        } else {
            pipManager = com.android.systemui.pip.phone.PipManager.getInstance();
        }
        this.mPipManager = pipManager;
        this.mPipManager.initialize(this.mContext);
        ((CommandQueue) getComponent(CommandQueue.class)).addCallbacks(this);
    }

    @Override
    public void showPictureInPictureMenu() {
        this.mPipManager.showPictureInPictureMenu();
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.mPipManager == null) {
            return;
        }
        this.mPipManager.onConfigurationChanged(configuration);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mPipManager == null) {
            return;
        }
        this.mPipManager.dump(printWriter);
    }
}
