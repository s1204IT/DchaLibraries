package com.mediatek.galleryfeature.picturequality;

import android.graphics.BitmapFactory;
import com.mediatek.gallerybasic.base.IDecodeOptionsProcessor;
import com.mediatek.gallerybasic.util.Log;
import java.lang.reflect.Field;

public class PictureQuality implements IDecodeOptionsProcessor {
    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final boolean SUPPORT_PICTURE_QUALITY_ENHANCE = true;
    private static final String TAG = "MtkGallery/PictureQuality";
    public static Field mField = null;
    public static boolean mHasInitializedField;

    private boolean supportPQEnhance(String str) {
        return MIME_TYPE_JPEG.equals(str);
    }

    @Override
    public boolean processThumbDecodeOptions(String str, BitmapFactory.Options options) {
        initOptions(str, options);
        return true;
    }

    @Override
    public boolean processRegionDecodeOptions(String str, BitmapFactory.Options options) {
        initOptions(str, options);
        return true;
    }

    private void initOptions(BitmapFactory.Options options) {
        if (!mHasInitializedField) {
            try {
                mField = options.getClass().getField("inPostProc");
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "NoSuchFieldException: " + e);
            }
            mHasInitializedField = true;
        }
        if (mHasInitializedField && mField != null) {
            try {
                mField.set(options, true);
                Log.v(TAG, "<initOptions> inPostPro = " + mField.getBoolean(options));
            } catch (IllegalAccessException e2) {
                Log.e(TAG, "IllegalAccessException: " + e2);
            } catch (IllegalArgumentException e3) {
                Log.e(TAG, "IllegalArgumentException: " + e3);
            }
        }
    }

    private void initOptions(String str, BitmapFactory.Options options) {
        if (supportPQEnhance(str)) {
            initOptions(options);
        }
    }
}
