package com.android.gallery3d.app;

import android.os.Parcelable;
import com.android.gallery3d.ui.ScreenNail;

public abstract class AppBridge implements Parcelable {

    public interface Server {
    }

    public abstract ScreenNail attachScreenNail();

    public abstract void detachScreenNail();

    public abstract boolean isPanorama();

    public abstract boolean isStaticCamera();

    public abstract void onFullScreenChanged(boolean z);

    public abstract boolean onSingleTapUp(int i, int i2);

    public abstract void setServer(Server server);
}
