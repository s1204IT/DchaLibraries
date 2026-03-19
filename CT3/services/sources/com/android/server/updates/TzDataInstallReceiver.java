package com.android.server.updates;

import android.util.Slog;
import java.io.File;
import libcore.tzdata.update.TzDataBundleInstaller;

public class TzDataInstallReceiver extends ConfigUpdateInstallReceiver {
    private static final String TAG = "TZDataInstallReceiver";
    private static final String UPDATE_CONTENT_FILE_NAME = "tzdata_bundle.zip";
    private static final String UPDATE_METADATA_DIR_NAME = "metadata/";
    private static final String UPDATE_VERSION_FILE_NAME = "version";
    private final TzDataBundleInstaller installer;
    private static final File TZ_DATA_DIR = new File("/data/misc/zoneinfo");
    private static final String UPDATE_DIR_NAME = TZ_DATA_DIR.getPath() + "/updates/";

    public TzDataInstallReceiver() {
        super(UPDATE_DIR_NAME, UPDATE_CONTENT_FILE_NAME, UPDATE_METADATA_DIR_NAME, UPDATE_VERSION_FILE_NAME);
        this.installer = new TzDataBundleInstaller(TAG, TZ_DATA_DIR);
    }

    @Override
    protected void install(byte[] content, int version) throws Throwable {
        boolean valid = this.installer.install(content);
        Slog.i(TAG, "Timezone data install valid for this device: " + valid);
        super.install(content, version);
    }
}
