package com.android.providers.downloads;

import android.content.Context;
import android.drm.DrmManagerClient;
import java.io.File;

public class DownloadDrmHelper {
    public static boolean isDrmConvertNeeded(String str) {
        return "application/vnd.oma.drm.message".equals(str);
    }

    public static String modifyDrmFwLockFileExtension(String str) {
        if (str != null) {
            int iLastIndexOf = str.lastIndexOf(".");
            if (iLastIndexOf != -1) {
                str = str.substring(0, iLastIndexOf);
            }
            return str.concat(".fl");
        }
        return str;
    }

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
