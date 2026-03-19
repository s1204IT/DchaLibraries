package com.mediatek.gallerygif;

import android.content.Context;
import android.content.res.Resources;
import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.Layer;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.MediaMember;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;
import com.mediatek.gallerybasic.platform.PlatformHelper;
import com.mediatek.gallerybasic.util.Log;

public class GifMember extends MediaMember {
    private static final int PRIORITY = 20;
    private static final String TAG = "MtkGallery2/GifMember";
    private GLIdleExecuter mGlIdleExecuter;

    public GifMember(Context context) {
        super(context);
    }

    public GifMember(Context context, GLIdleExecuter gLIdleExecuter) {
        super(context);
        this.mGlIdleExecuter = gLIdleExecuter;
    }

    public GifMember(Context context, Resources resources) {
        super(context, null, resources);
    }

    public GifMember(Context context, GLIdleExecuter gLIdleExecuter, Resources resources) {
        super(context, gLIdleExecuter, resources);
    }

    @Override
    public boolean isMatching(MediaData mediaData) {
        return mediaData != null && "image/gif".equals(mediaData.mimeType);
    }

    @Override
    public Player getPlayer(MediaData mediaData, ThumbType thumbType) {
        if (PlatformHelper.isOutOfDecodeSpec(mediaData.fileSize, mediaData.width, mediaData.height, mediaData.mimeType)) {
            Log.d(TAG, "<getPlayer>, outof decode spec, return null!");
            return null;
        }
        if (thumbType == ThumbType.FANCY || thumbType == ThumbType.MICRO) {
            return null;
        }
        return new GifPlayer(this.mContext, mediaData, Player.OutputType.TEXTURE, thumbType, this.mGlIdleExecuter);
    }

    @Override
    public ExtItem getItem(MediaData mediaData) {
        return new GifItem(mediaData, this.mContext);
    }

    @Override
    public Layer getLayer() {
        return null;
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
