package com.android.providers.downloads;

import android.content.Context;
import android.drm.DrmManagerClient;
import java.io.File;

public class DownloadDrmHelper {
    public static String getOriginalMimeType(Context context, File file, String currentMime) {
        DrmManagerClient client = new DrmManagerClient(context);
        try {
            String rawFile = file.toString();
            if (client.canHandle(rawFile, (String) null)) {
                currentMime = client.getOriginalMimeType(rawFile);
            }
            return currentMime;
        } finally {
            client.release();
        }
    }
}
