package com.android.providers.downloads;

import android.content.Context;
import android.drm.DrmManagerClient;
import java.io.File;

public class DownloadDrmHelper {
    public static String getOriginalMimeType(Context context, File file, String str) {
        DrmManagerClient drmManagerClient = new DrmManagerClient(context);
        try {
            String string = file.toString();
            if (drmManagerClient.canHandle(string, (String) null)) {
                return drmManagerClient.getOriginalMimeType(string);
            }
            return str;
        } finally {
            drmManagerClient.release();
        }
    }
}
