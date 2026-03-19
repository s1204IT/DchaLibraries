package com.android.storagemanager.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.webkit.MimeTypeMap;
import java.io.File;

public class IconProvider {
    private Context mContext;

    public IconProvider(Context context) {
        this.mContext = context;
    }

    public Drawable loadMimeIcon(String str) {
        return this.mContext.getContentResolver().getTypeDrawable(str);
    }

    public static String getMimeType(File file) {
        String name = file.getName();
        int iLastIndexOf = name.lastIndexOf(46);
        if (iLastIndexOf >= 0) {
            String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(iLastIndexOf + 1).toLowerCase());
            if (mimeTypeFromExtension != null) {
                return mimeTypeFromExtension;
            }
            return "application/octet-stream";
        }
        return "application/octet-stream";
    }

    public static boolean isImageType(File file) {
        return getMimeType(file).startsWith("image/");
    }
}
