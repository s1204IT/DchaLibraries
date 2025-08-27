package com.android.settings.enterprise;

import android.content.Context;

/* loaded from: classes.dex */
public class AdminGrantedCameraPermissionPreferenceController extends AdminGrantedPermissionsPreferenceControllerBase {
    public AdminGrantedCameraPermissionPreferenceController(Context context, boolean z) {
        super(context, z, new String[]{"android.permission.CAMERA"});
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "enterprise_privacy_number_camera_access_packages";
    }
}
