package com.android.gallery3d.filtershow.tools;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.util.XmpUtilHelper;
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

    public static void writeFilterXMP(Context context, Uri srcUri, File dstFile, ImagePreset preset) {
        InputStream is = null;
        XMPMeta xmpMeta = null;
        try {
            is = context.getContentResolver().openInputStream(srcUri);
            xmpMeta = XmpUtilHelper.extractXMPMeta(is);
        } catch (FileNotFoundException e) {
        } finally {
            Utils.closeSilently(is);
        }
        if (xmpMeta == null) {
            xmpMeta = XMPMetaFactory.create();
        }
        try {
            xmpMeta.setProperty("http://ns.google.com/photos/1.0/filter/", "SourceFileUri", srcUri.toString());
            xmpMeta.setProperty("http://ns.google.com/photos/1.0/filter/", "filterstack", preset.getJsonString("Saved"));
            if (!XmpUtilHelper.writeXMPMeta(dstFile.getAbsolutePath(), xmpMeta)) {
                Log.v("XmpPresets", "Write XMP meta to file failed:" + dstFile.getAbsolutePath());
            }
        } catch (XMPException e2) {
            Log.v("XmpPresets", "Write XMP meta to file failed:" + dstFile.getAbsolutePath());
        }
    }

    public static XMresults extractXMPData(Context context, MasterImage mMasterImage, Uri uriToEdit) {
        XMresults ret = new XMresults();
        InputStream is = null;
        XMPMeta xmpMeta = null;
        try {
            is = context.getContentResolver().openInputStream(uriToEdit);
            xmpMeta = XmpUtilHelper.extractXMPMeta(is);
        } catch (FileNotFoundException e) {
        } finally {
            Utils.closeSilently(is);
        }
        if (xmpMeta == null) {
            return null;
        }
        try {
            String strSrcUri = xmpMeta.getPropertyString("http://ns.google.com/photos/1.0/filter/", "SourceFileUri");
            if (strSrcUri != null) {
                String filterString = xmpMeta.getPropertyString("http://ns.google.com/photos/1.0/filter/", "filterstack");
                Uri srcUri = Uri.parse(strSrcUri);
                ret.originalimage = srcUri;
                ret.preset = new ImagePreset();
                ret.presetString = filterString;
                boolean ok = ret.preset.readJsonFromString(filterString);
                if (ok) {
                    return ret;
                }
                return null;
            }
        } catch (XMPException e2) {
            e2.printStackTrace();
        }
        return null;
    }
}
