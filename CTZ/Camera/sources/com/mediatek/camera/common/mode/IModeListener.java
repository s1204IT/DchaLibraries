package com.mediatek.camera.common.mode;

import com.mediatek.camera.common.app.IApp;

public interface IModeListener {
    void create(IApp iApp);

    void destroy();

    boolean onCameraSelected(String str);

    boolean onUserInteraction();

    void pause();

    void resume();
}
