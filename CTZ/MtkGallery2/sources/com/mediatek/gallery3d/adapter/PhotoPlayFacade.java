package com.mediatek.gallery3d.adapter;

import android.app.Activity;
import android.content.Context;
import com.android.gallery3d.app.GalleryAppImpl;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallerybasic.base.ExtFields;
import com.mediatek.gallerybasic.base.IFieldDefinition;
import com.mediatek.gallerybasic.base.IFilter;
import com.mediatek.gallerybasic.base.LayerManager;
import com.mediatek.gallerybasic.base.MediaCenter;
import com.mediatek.gallerybasic.base.MediaFilter;
import com.mediatek.gallerybasic.base.MediaMember;
import com.mediatek.gallerybasic.base.PlayEngine;
import com.mediatek.gallerybasic.base.ThumbType;
import com.mediatek.gallerybasic.dynamic.LayerManagerImpl;
import com.mediatek.gallerybasic.dynamic.PhotoPlayEngine;
import com.mediatek.gallerybasic.gl.GLIdleExecuter;
import com.mediatek.gallerybasic.platform.PlatformHelper;
import com.mediatek.gallerybasic.util.ExtFieldsUtils;
import com.mediatek.gallerybasic.util.Utils;
import java.util.ArrayList;

public class PhotoPlayFacade {
    private static boolean sHasIntialized = false;
    private static MediaCenter sMediaCenter;

    public static void initialize(GalleryAppImpl galleryAppImpl, int i, int i2, int i3) {
        if (sHasIntialized) {
            return;
        }
        Utils.initialize(galleryAppImpl);
        ThumbType.MICRO.setTargetSize(i);
        ThumbType.MIDDLE.setTargetSize(i2);
        ThumbType.HIGHQUALITY.setTargetSize(i3);
        PlatformHelper.setPlatform(new PlatformImpl(galleryAppImpl));
        registerFilters();
        registerFieldDefinitions(galleryAppImpl);
        sHasIntialized = true;
    }

    public static MediaCenter getMediaCenter() {
        if (sMediaCenter == null) {
            sMediaCenter = new MediaCenter();
        }
        return sMediaCenter;
    }

    public static int getFullScreenPlayCount() {
        return 1;
    }

    public static int getFullScreenTotalCount() {
        return 3;
    }

    private static void registerFilters() {
        for (IFilter iFilter : (IFilter[]) FeatureManager.getInstance().getImplement(IFilter.class, new Object[0])) {
            MediaFilter.registerFilter(iFilter);
        }
    }

    private static void registerFieldDefinitions(Context context) {
        IFieldDefinition[] iFieldDefinitionArr = (IFieldDefinition[]) FeatureManager.getInstance().getImplement(IFieldDefinition.class, new Object[0]);
        ExtFields.initColumns(context);
        ExtFields.addVideoFiled(ExtFieldsUtils.VIDEO_ROTATION_FIELD);
        ExtFields.registerFieldDefinition(iFieldDefinitionArr);
    }

    public static void registerMedias(Context context, GLIdleExecuter gLIdleExecuter) {
        MediaMember[] mediaMemberArr;
        Log.d("MtkGallery2/PhotoPlayFacade", "<registerMedias> Context = " + context + ", GLIdleExecuter = " + gLIdleExecuter + " Resources = " + context.getResources());
        MediaCenter mediaCenter = getMediaCenter();
        if (gLIdleExecuter != null) {
            mediaMemberArr = (MediaMember[]) FeatureManager.getInstance().getImplement(MediaMember.class, context, gLIdleExecuter, context.getResources());
        } else {
            mediaMemberArr = (MediaMember[]) FeatureManager.getInstance().getImplement(MediaMember.class, context, context.getResources());
        }
        ArrayList<MediaMember> arrayList = new ArrayList<>();
        for (MediaMember mediaMember : mediaMemberArr) {
            arrayList.add(mediaMember);
            mediaMember.setMediaCenter(getMediaCenter());
        }
        arrayList.add(new MediaMember(context));
        mediaCenter.registerMedias(arrayList);
    }

    public static void registerWidgetMedias(Context context) {
        Log.d("MtkGallery2/PhotoPlayFacade", "<registerWidgetMedias> context = " + context);
        if (getMediaCenter().getMemberCount() <= 0) {
            registerMedias(context, null);
        }
    }

    public static PlayEngine createPlayEngineForFullScreen() {
        return new PhotoPlayEngine(getMediaCenter(), 3, 1, 2, ThumbType.MIDDLE);
    }

    public static LayerManager createLayerMananger(Activity activity) {
        return new LayerManagerImpl(activity, getMediaCenter());
    }
}
