package com.mediatek.omadrm;

public class OmaDrmInfoRequest {
    public static final String ACTION_CHECK_RIGHTS_STATUS_BY_FD = "checkRightsStatusByFd";
    public static final String ACTION_CHECK_SECURE_TIME = "checkSecureTime";
    public static final String ACTION_CTA5_CHECKTOKEN = "CTA5Checktoken";
    public static final String ACTION_CTA5_CLEARTOKEN = "CTA5Cleartoken";
    public static final String ACTION_INSTALL_DRM_TO_DEVICE = "installDrmToDevice";
    public static final String ACTION_LOAD_DEVICE_ID = "loadDeviceId";
    public static final String ACTION_MARK_AS_CONSUME_IN_APP_CLIENT = "markAsConsumeInAppClient";
    public static final String ACTION_SAVE_DEVICE_ID = "saveDeviceId";
    public static final String ACTION_SAVE_SECURE_TIME = "saveSecureTime";
    public static final String ACTION_UPDATE_OFFSET = "updateOffset";
    public static final String KEY_ACTION = "action";
    public static final String KEY_CTA5_FILEPATH = "CTA5FilePath";
    public static final String KEY_CTA5_TOKEN = "CTA5Token";
    public static final String KEY_DATA = "data";
    public static final String KEY_DATA_1 = "data_1";
    public static final String KEY_DATA_1_EXTRA = "data_1_extra";
    public static final String KEY_DATA_2 = "data_2";
    public static final String KEY_DATA_2_EXTRA = "data_2_extra";
    public static final String KEY_FILEDESCRIPTOR = "FileDescriptorKey";

    public static class DrmRequestResult {
        public static final String RESULT_FAILURE = "failure";
        public static final String RESULT_SUCCESS = "success";
    }
}
