package com.mediatek.server.telecom.ext;

public class DefaultGttUtilExt implements IGttUtilExt {
    @Override
    public boolean skipUpdateAudioTtyMode() {
        return false;
    }
}
