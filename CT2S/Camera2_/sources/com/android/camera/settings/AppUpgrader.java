package com.android.camera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.camera.CameraActivity;
import com.android.camera.app.AppController;
import com.android.camera.app.ModuleManager;
import com.android.camera.debug.Log;
import com.android.camera.module.ModuleController;
import com.android.camera.settings.SettingsUtil;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.Size;
import java.util.List;
import java.util.Map;

public class AppUpgrader extends SettingsUpgrader {
    public static final int APP_UPGRADE_VERSION = 6;
    private static final int CAMERA_MODULE_SETTINGS_FILES_RENAMED_VERSION = 6;
    private static final int CAMERA_SETTINGS_SELECTED_MODULE_INDEX = 5;
    private static final int CAMERA_SETTINGS_STRINGS_UPGRADE = 5;
    private static final int CAMERA_SIZE_SETTING_UPGRADE_VERSION = 3;
    private static final int FORCE_LOCATION_CHOICE_VERSION = 2;
    private static final String OLD_CAMERA_PREFERENCES_PREFIX = "_preferences_";
    private static final String OLD_GLOBAL_PREFERENCES_FILENAME = "_preferences_camera";
    private static final String OLD_KEY_UPGRADE_VERSION = "pref_strict_upgrade_version";
    private static final String OLD_MODULE_PREFERENCES_PREFIX = "_preferences_module_";
    private static final Log.Tag TAG = new Log.Tag("AppUpgrader");
    private final AppController mAppController;

    public AppUpgrader(AppController appController) {
        super(Keys.KEY_UPGRADE_VERSION, 6);
        this.mAppController = appController;
    }

    @Override
    protected int getLastVersion(SettingsManager settingsManager) {
        SharedPreferences defaultPreferences = settingsManager.getDefaultPreferences();
        if (defaultPreferences.contains(OLD_KEY_UPGRADE_VERSION)) {
            Map<String, ?> allPrefs = defaultPreferences.getAll();
            Object oldVersion = allPrefs.get(OLD_KEY_UPGRADE_VERSION);
            defaultPreferences.edit().remove(OLD_KEY_UPGRADE_VERSION).apply();
            if (oldVersion instanceof Integer) {
                return ((Integer) oldVersion).intValue();
            }
            if (oldVersion instanceof String) {
                return SettingsManager.convertToInt((String) oldVersion);
            }
        }
        return super.getLastVersion(settingsManager);
    }

    @Override
    public void upgrade(SettingsManager settingsManager, int lastVersion, int currentVersion) {
        Context context = this.mAppController.getAndroidContext();
        if (lastVersion < 5) {
            upgradeTypesToStrings(settingsManager);
        }
        if (lastVersion < 2) {
            forceLocationChoice(settingsManager);
        }
        if (lastVersion < 3) {
            CameraDeviceInfo infos = CameraAgentFactory.getAndroidCameraAgent(context, CameraAgentFactory.CameraApi.API_1).getCameraDeviceInfo();
            upgradeCameraSizeSetting(settingsManager, context, infos, SettingsUtil.CAMERA_FACING_FRONT);
            upgradeCameraSizeSetting(settingsManager, context, infos, SettingsUtil.CAMERA_FACING_BACK);
            settingsManager.remove(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX);
        }
        if (lastVersion < 6) {
            upgradeCameraSettingsFiles(settingsManager, context);
            upgradeModuleSettingsFiles(settingsManager, context, this.mAppController);
        }
        if (lastVersion < 5) {
            upgradeSelectedModeIndex(settingsManager, context);
        }
    }

    private void upgradeTypesToStrings(SettingsManager settingsManager) {
        boolean flashSupportedBackCamera;
        SharedPreferences defaultPreferences = settingsManager.getDefaultPreferences();
        SharedPreferences oldGlobalPreferences = settingsManager.openPreferences(OLD_GLOBAL_PREFERENCES_FILENAME);
        if (defaultPreferences.contains(Keys.KEY_RECORD_LOCATION)) {
            boolean location = removeBoolean(defaultPreferences, Keys.KEY_RECORD_LOCATION);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION, location);
        }
        if (defaultPreferences.contains(Keys.KEY_USER_SELECTED_ASPECT_RATIO)) {
            boolean userSelectedAspectRatio = removeBoolean(defaultPreferences, Keys.KEY_USER_SELECTED_ASPECT_RATIO);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO, userSelectedAspectRatio);
        }
        if (defaultPreferences.contains(Keys.KEY_EXPOSURE_COMPENSATION_ENABLED)) {
            boolean manualExposureCompensationEnabled = removeBoolean(defaultPreferences, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED, manualExposureCompensationEnabled);
        }
        if (defaultPreferences.contains(Keys.KEY_CAMERA_FIRST_USE_HINT_SHOWN)) {
            boolean hint = removeBoolean(defaultPreferences, Keys.KEY_CAMERA_FIRST_USE_HINT_SHOWN);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_FIRST_USE_HINT_SHOWN, hint);
        }
        if (defaultPreferences.contains(Keys.KEY_STARTUP_MODULE_INDEX)) {
            int startupModuleIndex = removeInteger(defaultPreferences, Keys.KEY_STARTUP_MODULE_INDEX);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, startupModuleIndex);
        }
        if (defaultPreferences.contains(Keys.KEY_CAMERA_MODULE_LAST_USED)) {
            int lastCameraUsedModuleIndex = removeInteger(defaultPreferences, Keys.KEY_CAMERA_MODULE_LAST_USED);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED, lastCameraUsedModuleIndex);
        }
        if (oldGlobalPreferences.contains(Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA) && (flashSupportedBackCamera = removeBoolean(oldGlobalPreferences, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA))) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA, flashSupportedBackCamera);
        }
        if (defaultPreferences.contains(Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING)) {
            boolean shouldShowRefocusViewer = removeBoolean(defaultPreferences, Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING, shouldShowRefocusViewer);
        }
        if (defaultPreferences.contains(Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING)) {
            boolean shouldShowSettingsButtonCling = removeBoolean(defaultPreferences, Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING, shouldShowSettingsButtonCling);
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_HDR_PLUS)) {
            String hdrPlus = removeString(oldGlobalPreferences, Keys.KEY_CAMERA_HDR_PLUS);
            if ("on".equals(hdrPlus)) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, true);
            }
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_HDR)) {
            String hdrPlus2 = removeString(oldGlobalPreferences, Keys.KEY_CAMERA_HDR);
            if ("on".equals(hdrPlus2)) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, true);
            }
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_GRID_LINES)) {
            String hdrPlus3 = removeString(oldGlobalPreferences, Keys.KEY_CAMERA_GRID_LINES);
            if ("on".equals(hdrPlus3)) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES, true);
            }
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_AUTO_FOCUS)) {
            String hdrPlus4 = removeString(oldGlobalPreferences, Keys.KEY_CAMERA_AUTO_FOCUS);
            if ("on".equals(hdrPlus4)) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_AUTO_FOCUS, true);
            }
        }
    }

    private void forceLocationChoice(SettingsManager settingsManager) {
        SharedPreferences oldGlobalPreferences = settingsManager.openPreferences(OLD_GLOBAL_PREFERENCES_FILENAME);
        if (settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION)) {
            if (!settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION)) {
                settingsManager.remove(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION);
            }
        } else if (oldGlobalPreferences.contains(Keys.KEY_RECORD_LOCATION)) {
            String location = removeString(oldGlobalPreferences, Keys.KEY_RECORD_LOCATION);
            if ("on".equals(location)) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION, true);
            }
        }
    }

    private void upgradeCameraSizeSetting(SettingsManager settingsManager, Context context, CameraDeviceInfo infos, SettingsUtil.CameraDeviceSelector facing) {
        String key;
        List<Size> supported;
        if (facing == SettingsUtil.CAMERA_FACING_FRONT) {
            key = Keys.KEY_PICTURE_SIZE_FRONT;
        } else if (facing == SettingsUtil.CAMERA_FACING_BACK) {
            key = Keys.KEY_PICTURE_SIZE_BACK;
        } else {
            Log.w(TAG, "Ignoring attempt to upgrade size of unhandled camera facing direction");
            return;
        }
        if (infos == null) {
            settingsManager.remove(SettingsManager.SCOPE_GLOBAL, key);
            return;
        }
        String pictureSize = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, key);
        int camera = SettingsUtil.getCameraId(infos, facing);
        if (camera != -1 && (supported = CameraPictureSizesCacher.getSizesForCamera(camera, context)) != null) {
            Size size = SettingsUtil.getPhotoSize(pictureSize, supported, camera);
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, key, SettingsUtil.sizeToSetting(size));
        }
    }

    private void copyPreferences(SharedPreferences oldPrefs, SharedPreferences newPrefs) {
        Map<String, ?> entries = oldPrefs.getAll();
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                Log.w(TAG, "skipped upgrade and removing entry for null key " + key);
                newPrefs.edit().remove(key).apply();
            } else if (value instanceof Boolean) {
                String boolValue = SettingsManager.convert(((Boolean) value).booleanValue());
                newPrefs.edit().putString(key, boolValue).apply();
            } else if (value instanceof Integer) {
                String intValue = SettingsManager.convert(((Integer) value).intValue());
                newPrefs.edit().putString(key, intValue).apply();
            } else if (value instanceof Long) {
                long longValue = ((Long) value).longValue();
                if (longValue <= 2147483647L && longValue >= -2147483648L) {
                    String intValue2 = SettingsManager.convert((int) longValue);
                    newPrefs.edit().putString(key, intValue2).apply();
                } else {
                    Log.w(TAG, "skipped upgrade for out of bounds long key " + key + " : " + longValue);
                }
            } else if (value instanceof String) {
                newPrefs.edit().putString(key, (String) value).apply();
            } else {
                Log.w(TAG, "skipped upgrade and removing entry for unrecognized key type " + key + " : " + value.getClass());
                newPrefs.edit().remove(key).apply();
            }
        }
    }

    private void upgradeCameraSettingsFiles(SettingsManager settingsManager, Context context) {
        String[] cameraIds = context.getResources().getStringArray(R.array.camera_id_entryvalues);
        for (int i = 0; i < cameraIds.length; i++) {
            SharedPreferences oldCameraPreferences = settingsManager.openPreferences(OLD_CAMERA_PREFERENCES_PREFIX + cameraIds[i]);
            SharedPreferences newCameraPreferences = settingsManager.openPreferences(CameraActivity.CAMERA_SCOPE_PREFIX + cameraIds[i]);
            copyPreferences(oldCameraPreferences, newCameraPreferences);
        }
    }

    private void upgradeModuleSettingsFiles(SettingsManager settingsManager, Context context, AppController app) {
        int[] moduleIds = context.getResources().getIntArray(R.array.camera_modes);
        for (int i = 0; i < moduleIds.length; i++) {
            String moduleId = Integer.toString(moduleIds[i]);
            SharedPreferences oldModulePreferences = settingsManager.openPreferences("_preferences_module_" + moduleId);
            ModuleManager.ModuleAgent agent = app.getModuleManager().getModuleAgent(moduleIds[i]);
            if (agent != null) {
                ModuleController module = agent.createModule(app);
                SharedPreferences newModulePreferences = settingsManager.openPreferences("_preferences_module_" + module.getModuleStringIdentifier());
                copyPreferences(oldModulePreferences, newModulePreferences);
            }
        }
    }

    private void upgradeSelectedModeIndex(SettingsManager settingsManager, Context context) {
        int gcamIndex = context.getResources().getInteger(R.integer.camera_mode_gcam);
        int lastUsedCameraIndex = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED).intValue();
        if (lastUsedCameraIndex == 6) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED, gcamIndex);
        }
        int startupModuleIndex = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX).intValue();
        if (startupModuleIndex == 6) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, gcamIndex);
        }
    }
}
