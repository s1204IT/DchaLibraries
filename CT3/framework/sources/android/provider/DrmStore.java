package android.provider;

import android.net.Uri;

public final class DrmStore {
    private static final String ACCESS_DRM_PERMISSION = "android.permission.ACCESS_DRM";
    public static final String AUTHORITY = "drm";
    private static final String TAG = "DrmStore";

    public interface Audio extends Columns {
        public static final Uri CONTENT_URI = Uri.parse("content://drm/audio");
    }

    public interface Columns extends BaseColumns {
        public static final String DATA = "_data";
        public static final String MIME_TYPE = "mime_type";
        public static final String SIZE = "_size";
        public static final String TITLE = "title";
    }

    public interface Images extends Columns {
        public static final Uri CONTENT_URI = Uri.parse("content://drm/images");
    }
}
