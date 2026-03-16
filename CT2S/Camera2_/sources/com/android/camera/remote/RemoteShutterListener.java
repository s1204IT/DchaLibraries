package com.android.camera.remote;

public interface RemoteShutterListener {
    void onModuleExit();

    void onModuleReady(RemoteCameraModule remoteCameraModule);

    void onPictureTaken(byte[] bArr);
}
