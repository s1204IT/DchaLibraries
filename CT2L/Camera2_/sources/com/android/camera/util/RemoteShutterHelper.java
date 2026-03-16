package com.android.camera.util;

import android.content.Context;
import com.android.camera.remote.RemoteCameraModule;
import com.android.camera.remote.RemoteShutterListener;

public class RemoteShutterHelper {
    public static RemoteShutterListener create(Context context) {
        return new RemoteShutterListener() {
            @Override
            public void onPictureTaken(byte[] photoData) {
            }

            @Override
            public void onModuleReady(RemoteCameraModule module) {
            }

            @Override
            public void onModuleExit() {
            }
        };
    }
}
