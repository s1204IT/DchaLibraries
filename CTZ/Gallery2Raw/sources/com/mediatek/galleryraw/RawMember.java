package com.mediatek.galleryraw;

import android.content.Context;
import android.content.res.Resources;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.MediaMember;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;

public class RawMember extends MediaMember {
    private static final int PRIORITY = 10;
    public static final String[] RAW_MIME_TYPE = {"image/x-adobe-dng", "image/x-canon-cr2", "image/x-nikon-nef", "image/x-nikon-nrw", "image/x-sony-arw", "image/x-panasonic-rw2", "image/x-olympus-orf", "image/x-fuji-raf", "image/x-pentax-pef", "image/x-samsung-srw"};
    private static final String TAG = "MtkGallery2/RawMember";
    public static int sType;
    private Layer mLayer;

    public RawMember(Context context) {
        super(context);
    }

    public RawMember(Context context, Resources resources) {
        super(context, null, resources);
    }

    public RawMember(Context context, GLIdleExecuter gLIdleExecuter, Resources resources) {
        super(context, gLIdleExecuter, resources);
    }

    @Override
    public boolean isMatching(MediaData mediaData) {
        if (mediaData == null || mediaData.mimeType == null) {
            return false;
        }
        for (String str : RAW_MIME_TYPE) {
            if (str.equals(mediaData.mimeType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ExtItem getItem(MediaData mediaData) {
        return new RawItem(mediaData);
    }

    @Override
    public Layer getLayer() {
        if (this.mLayer == null) {
            this.mLayer = new RawLayer(this.mResources);
        }
        return this.mLayer;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    protected void onTypeObtained(int i) {
        sType = i;
    }
}
