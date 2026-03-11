package com.android.systemui.volume;

import android.content.res.Configuration;
import android.os.Handler;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.qs.tiles.DndTile;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class VolumeUI extends SystemUI {
    private static boolean LOGD = Log.isLoggable("VolumeUI", 3);
    private boolean mEnabled;
    private final Handler mHandler = new Handler();
    private VolumeDialogComponent mVolumeComponent;

    @Override
    public void start() {
        boolean z = this.mContext.getResources().getBoolean(R.bool.enable_volume_ui);
        boolean z2 = this.mContext.getResources().getBoolean(R.bool.enable_safety_warning);
        this.mEnabled = z || z2;
        if (this.mEnabled) {
            this.mVolumeComponent = new VolumeDialogComponent(this, this.mContext, null);
            this.mVolumeComponent.setEnableDialogs(z, z2);
            putComponent(VolumeComponent.class, getVolumeComponent());
            setDefaultVolumeController();
        }
    }

    private VolumeComponent getVolumeComponent() {
        return this.mVolumeComponent;
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.mEnabled) {
            getVolumeComponent().onConfigurationChanged(configuration);
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("mEnabled=");
        printWriter.println(this.mEnabled);
        if (this.mEnabled) {
            getVolumeComponent().dump(fileDescriptor, printWriter, strArr);
        }
    }

    private void setDefaultVolumeController() {
        DndTile.setVisible(this.mContext, true);
        if (LOGD) {
            Log.d("VolumeUI", "Registering default volume controller");
        }
        getVolumeComponent().register();
    }
}
