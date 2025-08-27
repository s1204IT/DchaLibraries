package com.android.systemui.statusbar.policy;

/* loaded from: classes.dex */
public interface DataSaverController extends CallbackController<Listener> {

    public interface Listener {
        void onDataSaverChanged(boolean z);
    }

    boolean isDataSaverEnabled();

    void setDataSaverEnabled(boolean z);
}
