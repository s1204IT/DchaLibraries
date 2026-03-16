package com.android.server.updates;

import android.util.Base64;

public class TZInfoInstallReceiver extends ConfigUpdateInstallReceiver {
    public TZInfoInstallReceiver() {
        super("/data/misc/zoneinfo/", "tzdata", "metadata/", "version");
    }

    @Override
    protected void install(byte[] encodedContent, int version) throws Throwable {
        super.install(Base64.decode(encodedContent, 0), version);
    }
}
