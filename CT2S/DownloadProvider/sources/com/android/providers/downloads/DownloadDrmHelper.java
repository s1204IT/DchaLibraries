package com.android.providers.downloads;

import android.content.Context;
import android.drm.DrmManagerClient;
import java.io.File;

public class DownloadDrmHelper {
    public static boolean isDrmConvertNeeded(String mimetype) {
        return "application/vnd.oma.drm.message".equals(mimetype);
    }

    public static String modifyDrmFwLockFileExtension(String filename) {
        if (filename != null) {
            int extensionIndex = filename.lastIndexOf(".");
            if (extensionIndex != -1) {
                filename = filename.substring(0, extensionIndex);
            }
            return filename.concat(".fl");
        }
        return filename;
    }

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
