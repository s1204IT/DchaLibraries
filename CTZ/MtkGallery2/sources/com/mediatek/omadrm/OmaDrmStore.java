package com.mediatek.omadrm;

public class OmaDrmStore {

    public static class DrmFileExtension {
        public static final String EXTENSION_DRM_CONTENT = ".dcf";
        public static final String EXTENSION_DRM_MESSAGE = ".dm";
        public static final String EXTENSION_RIGHTS_WBXML = ".drc";
        public static final String EXTENSION_RIGHTS_XML = ".dr";
    }

    public static class DrmIntentExtra {
        public static final String EXTRA_DRM_LEVEL = "android.intent.extra.drm_level";
        public static final int LEVEL_ALL = 4;
        public static final int LEVEL_FL = 1;
        public static final int LEVEL_NONE = 0;
        public static final int LEVEL_SD = 2;
    }

    public static class DrmObjectMimeType {
        public static final String MIME_TYPE_CTA5_MESSAGE = "application/vnd.mtk.cta5.message";
        public static final String MIME_TYPE_DRM_CONTENT = "application/vnd.oma.drm.content";
        public static final String MIME_TYPE_DRM_MESSAGE = "application/vnd.oma.drm.message";
        public static final String MIME_TYPE_RIGHTS_WBXML = "application/vnd.oma.drm.rights+wbxml";
        public static final String MIME_TYPE_RIGHTS_XML = "application/vnd.oma.drm.rights+xml";
    }

    public interface MetadatasColumns {
        public static final String DRM_CONTENT_DESCRIPTION = "drm_content_description";
        public static final String DRM_CONTENT_NAME = "drm_content_name";
        public static final String DRM_CONTENT_URI = "drm_content_uri";
        public static final String DRM_CONTENT_VENDOR = "drm_content_vendor";
        public static final String DRM_DATALEN = "drm_dataLen";
        public static final String DRM_ICON_URI = "drm_icon_uri";
        public static final String DRM_METHOD = "drm_method";
        public static final String DRM_MIME_TYPE = "drm_mime_type";
        public static final String DRM_OFFSET = "drm_offset";
        public static final String DRM_RIGHTS_ISSUER = "drm_rights_issuer";
        public static final String IS_DRM = "isdrm";
    }

    public static class Method {
        public static final int CD = 2;
        public static final int FL = 1;
        public static final int FLSD = 8;
        public static final int SD = 4;
    }

    public static class MimePrefix {
        public static final String AUDIO = "audio/";
        public static final String IMAGE = "image/";
        public static final String VIDEO = "video/";
    }
}
