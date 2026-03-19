package com.android.gallery3d.util;

import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.mediatek.gallery3d.adapter.FeatureHelper;
import java.util.Comparator;

public class MediaSetUtils {
    public static int STEREO_CLIPPINGS_BUCKET_ID;
    public static final Comparator<MediaSet> NAME_COMPARATOR = new NameComparator();
    public static int CAMERA_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/DCIM/Camera");
    public static int DOWNLOAD_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/download");
    public static int EDITED_ONLINE_PHOTOS_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/EditedOnlinePhotos");
    public static int IMPORTED_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/Imported");
    public static int SNAPSHOT_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/Pictures/Screenshots");
    private static Path[] sCameraPaths = {Path.fromString("/local/all/" + CAMERA_BUCKET_ID), Path.fromString("/local/image/" + CAMERA_BUCKET_ID), Path.fromString("/local/video/" + CAMERA_BUCKET_ID)};
    private static String[] sCameraPathStrings = {sCameraPaths[0].toString(), sCameraPaths[1].toString(), sCameraPaths[2].toString()};
    private static Path[] sTempCameraPaths = {sCameraPaths[0], sCameraPaths[1], sCameraPaths[2]};

    static {
        StringBuilder sb = new StringBuilder();
        sb.append(FeatureHelper.getDefaultPath().toString());
        sb.append("/");
        sb.append("Pictures/Clippings");
        STEREO_CLIPPINGS_BUCKET_ID = GalleryUtils.getBucketId(sb.toString());
    }

    public static boolean isCameraSource(Path path) {
        return sCameraPaths[0] == path || sCameraPaths[1] == path || sCameraPaths[2] == path;
    }

    public static class NameComparator implements Comparator<MediaSet> {
        @Override
        public int compare(MediaSet mediaSet, MediaSet mediaSet2) {
            int iCompareToIgnoreCase = mediaSet.getName().compareToIgnoreCase(mediaSet2.getName());
            return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : mediaSet.getPath().toString().compareTo(mediaSet2.getPath().toString());
        }
    }

    public static void refreshBucketId() {
        CAMERA_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/DCIM/Camera");
        DOWNLOAD_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/download");
        EDITED_ONLINE_PHOTOS_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/EditedOnlinePhotos");
        IMPORTED_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/Imported");
        StringBuilder sb = new StringBuilder();
        sb.append(FeatureHelper.getDefaultPath().toString());
        sb.append("/Pictures/Screenshots");
        SNAPSHOT_BUCKET_ID = GalleryUtils.getBucketId(sb.toString());
        STEREO_CLIPPINGS_BUCKET_ID = GalleryUtils.getBucketId(FeatureHelper.getDefaultPath().toString() + "/Pictures/Clippings");
        sCameraPaths[0] = Path.fromString("/local/all/" + CAMERA_BUCKET_ID);
        sCameraPaths[1] = Path.fromString("/local/image/" + CAMERA_BUCKET_ID);
        sCameraPaths[2] = Path.fromString("/local/video/" + CAMERA_BUCKET_ID);
        sCameraPathStrings[0] = sCameraPaths[0].toString();
        sCameraPathStrings[1] = sCameraPaths[1].toString();
        sCameraPathStrings[2] = sCameraPaths[2].toString();
        sTempCameraPaths[0] = sCameraPaths[0];
        sTempCameraPaths[1] = sCameraPaths[1];
        sTempCameraPaths[2] = sCameraPaths[2];
    }
}
