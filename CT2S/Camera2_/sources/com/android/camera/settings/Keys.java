package com.android.camera.settings;

import android.content.Context;
import com.android.camera.app.LocationManager;
import com.android.camera.util.ApiHelper;
import com.android.camera2.R;

public class Keys {
    public static final String KEY_CAMERA_AUTO_FOCUS = "pref_camera_auto_focus";
    public static final String KEY_CAMERA_FIRST_USE_HINT_SHOWN = "pref_camera_first_use_hint_shown_key";
    public static final String KEY_CAMERA_GRID_LINES = "pref_camera_grid_lines";
    public static final String KEY_CAMERA_HDR = "pref_camera_hdr_key";
    public static final String KEY_CAMERA_HDR_PLUS = "pref_camera_hdr_plus_key";
    public static final String KEY_CAMERA_ID = "pref_camera_id_key";
    public static final String KEY_CAMERA_MODULE_LAST_USED = "pref_camera_module_last_used_index";
    public static final String KEY_CAMERA_PANO_ORIENTATION = "pref_camera_pano_orientation";
    public static final String KEY_COUNTDOWN_DURATION = "pref_camera_countdown_duration_key";
    public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
    public static final String KEY_EXPOSURE_COMPENSATION_ENABLED = "pref_camera_exposure_compensation_key";
    public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
    public static final String KEY_FLASH_SUPPORTED_BACK_CAMERA = "pref_flash_supported_back_camera";
    public static final String KEY_FOCUS_MODE = "pref_camera_focusmode_key";
    public static final String KEY_HDR_PLUS_FLASH_MODE = "pref_hdr_plus_flash_mode";
    public static final String KEY_JPEG_QUALITY = "pref_camera_jpegquality_key";
    public static final String KEY_PICTURE_SIZE_BACK = "pref_camera_picturesize_back_key";
    public static final String KEY_PICTURE_SIZE_FRONT = "pref_camera_picturesize_front_key";
    public static final String KEY_RECORD_LOCATION = "pref_camera_recordlocation_key";
    public static final String KEY_RELEASE_DIALOG_LAST_SHOWN_VERSION = "pref_release_dialog_last_shown_version";
    public static final String KEY_REQUEST_RETURN_HDR_PLUS = "pref_request_return_hdr_plus";
    public static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
    public static final String KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING = "pref_should_show_refocus_viewer_cling";
    public static final String KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING = "pref_should_show_settings_button_cling";
    public static final String KEY_STARTUP_MODULE_INDEX = "camera.startup_module";
    public static final String KEY_UPGRADE_VERSION = "pref_upgrade_version";
    public static final String KEY_USER_SELECTED_ASPECT_RATIO = "pref_user_selected_aspect_ratio";
    public static final String KEY_VIDEOCAMERA_FLASH_MODE = "pref_camera_video_flashmode_key";
    public static final String KEY_VIDEO_EFFECT = "pref_video_effect_key";
    public static final String KEY_VIDEO_FIRST_USE_HINT_SHOWN = "pref_video_first_use_hint_shown_key";
    public static final String KEY_VIDEO_QUALITY_BACK = "pref_video_quality_back_key";
    public static final String KEY_VIDEO_QUALITY_FRONT = "pref_video_quality_front_key";

    public static void setDefaults(SettingsManager settingsManager, Context context) {
        settingsManager.setDefaults(KEY_COUNTDOWN_DURATION, 0, context.getResources().getIntArray(R.array.pref_countdown_duration));
        settingsManager.setDefaults(KEY_CAMERA_ID, context.getString(R.string.pref_camera_id_default), context.getResources().getStringArray(R.array.camera_id_entryvalues));
        settingsManager.setDefaults(KEY_SCENE_MODE, context.getString(R.string.pref_camera_scenemode_default), context.getResources().getStringArray(R.array.pref_camera_scenemode_entryvalues));
        settingsManager.setDefaults(KEY_FLASH_MODE, context.getString(R.string.pref_camera_flashmode_default), context.getResources().getStringArray(R.array.pref_camera_flashmode_entryvalues));
        settingsManager.setDefaults(KEY_CAMERA_HDR, false);
        settingsManager.setDefaults(KEY_CAMERA_HDR_PLUS, false);
        settingsManager.setDefaults(KEY_CAMERA_FIRST_USE_HINT_SHOWN, true);
        settingsManager.setDefaults(KEY_FOCUS_MODE, context.getString(R.string.pref_camera_focusmode_default), context.getResources().getStringArray(R.array.pref_camera_focusmode_entryvalues));
        String videoQualityBackDefaultValue = context.getString(R.string.pref_video_quality_large);
        if (ApiHelper.IS_NEXUS_6) {
            videoQualityBackDefaultValue = context.getString(R.string.pref_video_quality_medium);
        }
        settingsManager.setDefaults(KEY_VIDEO_QUALITY_BACK, videoQualityBackDefaultValue, context.getResources().getStringArray(R.array.pref_video_quality_entryvalues));
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_VIDEO_QUALITY_BACK)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_VIDEO_QUALITY_BACK);
        }
        settingsManager.setDefaults(KEY_VIDEO_QUALITY_FRONT, context.getString(R.string.pref_video_quality_large), context.getResources().getStringArray(R.array.pref_video_quality_entryvalues));
        if (!settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, KEY_VIDEO_QUALITY_FRONT)) {
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, KEY_VIDEO_QUALITY_FRONT);
        }
        settingsManager.setDefaults(KEY_JPEG_QUALITY, context.getString(R.string.pref_camera_jpeg_quality_normal), context.getResources().getStringArray(R.array.pref_camera_jpeg_quality_entryvalues));
        settingsManager.setDefaults(KEY_VIDEOCAMERA_FLASH_MODE, context.getString(R.string.pref_camera_video_flashmode_default), context.getResources().getStringArray(R.array.pref_camera_video_flashmode_entryvalues));
        settingsManager.setDefaults(KEY_VIDEO_EFFECT, context.getString(R.string.pref_video_effect_default), context.getResources().getStringArray(R.array.pref_video_effect_entryvalues));
        settingsManager.setDefaults(KEY_VIDEO_FIRST_USE_HINT_SHOWN, true);
        settingsManager.setDefaults(KEY_STARTUP_MODULE_INDEX, 0, context.getResources().getIntArray(R.array.camera_modes));
        settingsManager.setDefaults(KEY_CAMERA_MODULE_LAST_USED, context.getResources().getInteger(R.integer.camera_mode_photo), context.getResources().getIntArray(R.array.camera_modes));
        settingsManager.setDefaults(KEY_CAMERA_PANO_ORIENTATION, context.getString(R.string.pano_orientation_horizontal), context.getResources().getStringArray(R.array.pref_camera_pano_orientation_entryvalues));
        settingsManager.setDefaults(KEY_CAMERA_GRID_LINES, false);
        settingsManager.setDefaults(KEY_CAMERA_AUTO_FOCUS, false);
        settingsManager.setDefaults(KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING, true);
        settingsManager.setDefaults(KEY_HDR_PLUS_FLASH_MODE, context.getString(R.string.pref_camera_hdr_plus_flashmode_default), context.getResources().getStringArray(R.array.pref_camera_hdr_plus_flashmode_entryvalues));
        settingsManager.setDefaults(KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING, true);
    }

    public static boolean isCameraBackFacing(SettingsManager settingsManager, String moduleScope) {
        return settingsManager.isDefault(moduleScope, KEY_CAMERA_ID);
    }

    public static boolean isHdrPlusOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_HDR_PLUS);
    }

    public static boolean isHdrOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_HDR);
    }

    public static boolean requestsReturnToHdrPlus(SettingsManager settingsManager, String moduleScope) {
        return settingsManager.getBoolean(moduleScope, KEY_REQUEST_RETURN_HDR_PLUS);
    }

    public static boolean areGridLinesOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_GRID_LINES);
    }

    public static boolean areAutoFocusOn(SettingsManager settingsManager) {
        return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_AUTO_FOCUS);
    }

    public static boolean isPanoOrientationHorizontal(SettingsManager settingsManager) {
        return settingsManager.isDefault(SettingsManager.SCOPE_GLOBAL, KEY_CAMERA_PANO_ORIENTATION);
    }

    public static void setLocation(SettingsManager settingsManager, boolean on, LocationManager locationManager) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_RECORD_LOCATION, on);
        locationManager.recordLocation(on);
    }

    public static void setAspectRatioSelected(SettingsManager settingsManager) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_USER_SELECTED_ASPECT_RATIO, true);
    }

    public static void setManualExposureCompensation(SettingsManager settingsManager, boolean on) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, KEY_EXPOSURE_COMPENSATION_ENABLED, on);
    }

    public static void syncLocationManager(SettingsManager settingsManager, LocationManager locationManager) {
        boolean value = settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, KEY_RECORD_LOCATION);
        locationManager.recordLocation(value);
    }
}
