package com.android.settings.enterprise;

import android.content.Context;

/* loaded from: classes.dex */
public class AdminGrantedMicrophonePermissionPreferenceController extends AdminGrantedPermissionsPreferenceControllerBase {
    public AdminGrantedMicrophonePermissionPreferenceController(Context context, boolean z) {
        super(context, z, new String[]{"android.permission.RECORD_AUDIO"});
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "enterprise_privacy_number_microphone_access_packages";
    }
}
