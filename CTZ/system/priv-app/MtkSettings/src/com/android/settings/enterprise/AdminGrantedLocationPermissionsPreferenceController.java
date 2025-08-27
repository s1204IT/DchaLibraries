package com.android.settings.enterprise;

import android.content.Context;

/* loaded from: classes.dex */
public class AdminGrantedLocationPermissionsPreferenceController extends AdminGrantedPermissionsPreferenceControllerBase {
    public AdminGrantedLocationPermissionsPreferenceController(Context context, boolean z) {
        super(context, z, new String[]{"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"});
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "enterprise_privacy_number_location_access_packages";
    }
}
