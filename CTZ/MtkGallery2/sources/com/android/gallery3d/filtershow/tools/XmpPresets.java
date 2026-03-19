package com.android.gallery3d.filtershow.tools;

import android.content.Context;
import android.net.Uri;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.util.XmpUtilHelper;
import com.mediatek.gallery3d.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class XmpPresets {

    public static class XMresults {
        public Uri originalimage;
        public ImagePreset preset;
        public String presetString;
    }

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace("http://ns.google.com/photos/1.0/filter/", "AFltr");
        } catch (XMPException e) {
            Log.e("XmpPresets", "Register XMP name space failed", e);
        }
    }

    public static void writeFilterXMP(Context context, Uri uri, File file, ImagePreset imagePreset) {
        InputStream inputStreamOpenInputStream;
        InputStream inputStream = null;
        XMPMeta xMPMetaCreate = null;
        try {
            inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            inputStreamOpenInputStream = null;
        } catch (Throwable th) {
            th = th;
        }
        try {
            XMPMeta xMPMetaExtractXMPMeta = XmpUtilHelper.extractXMPMeta(inputStreamOpenInputStream);
            Utils.closeSilently(inputStreamOpenInputStream);
            xMPMetaCreate = xMPMetaExtractXMPMeta;
        } catch (FileNotFoundException e2) {
            Utils.closeSilently(inputStreamOpenInputStream);
        } catch (Throwable th2) {
            th = th2;
            inputStream = inputStreamOpenInputStream;
            Utils.closeSilently(inputStream);
            throw th;
        }
        if (xMPMetaCreate == null) {
            xMPMetaCreate = XMPMetaFactory.create();
        }
        try {
            xMPMetaCreate.setProperty("http://ns.google.com/photos/1.0/filter/", "SourceFileUri", uri.toString());
            xMPMetaCreate.setProperty("http://ns.google.com/photos/1.0/filter/", "filterstack", imagePreset.getJsonString("Saved"));
            if (XmpUtilHelper.writeXMPMeta(file.getAbsolutePath(), xMPMetaCreate)) {
                return;
            }
            Log.v("XmpPresets", "Write XMP meta to file failed:" + file.getAbsolutePath());
        } catch (XMPException e3) {
            Log.v("XmpPresets", "Write XMP meta to file failed:" + file.getAbsolutePath());
        }
    }

    public static XMresults extractXMPData(Context context, MasterImage masterImage, Uri uri) {
        InputStream inputStreamOpenInputStream;
        XMPMeta xMPMetaExtractXMPMeta;
        InputStream inputStream = null;
        if (uri == null) {
            return null;
        }
        XMresults xMresults = new XMresults();
        try {
            inputStreamOpenInputStream = context.getContentResolver().openInputStream(uri);
            try {
                xMPMetaExtractXMPMeta = XmpUtilHelper.extractXMPMeta(inputStreamOpenInputStream);
                Utils.closeSilently(inputStreamOpenInputStream);
            } catch (FileNotFoundException e) {
                Utils.closeSilently(inputStreamOpenInputStream);
                xMPMetaExtractXMPMeta = null;
            } catch (Throwable th) {
                inputStream = inputStreamOpenInputStream;
                th = th;
                Utils.closeSilently(inputStream);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            inputStreamOpenInputStream = null;
        } catch (Throwable th2) {
            th = th2;
        }
        if (xMPMetaExtractXMPMeta == null) {
            return null;
        }
        try {
            String propertyString = xMPMetaExtractXMPMeta.getPropertyString("http://ns.google.com/photos/1.0/filter/", "SourceFileUri");
            if (propertyString != null) {
                String propertyString2 = xMPMetaExtractXMPMeta.getPropertyString("http://ns.google.com/photos/1.0/filter/", "filterstack");
                xMresults.originalimage = Uri.parse(propertyString);
                xMresults.preset = new ImagePreset();
                xMresults.presetString = propertyString2;
                if (xMresults.preset.readJsonFromString(propertyString2)) {
                    return xMresults;
                }
                return null;
            }
        } catch (XMPException e3) {
            e3.printStackTrace();
        }
        return null;
    }
}
