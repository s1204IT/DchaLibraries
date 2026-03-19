package com.android.managedprovisioning.common;

import android.content.Context;
import android.content.SharedPreferences;

public class ManagedProvisioningSharedPreferences {
    static final String KEY_PROVISIONING_ID = "provisioning_id";
    static final String SHARED_PREFERENCE = "managed_profile_shared_preferences";
    private static final Object sWriteLock = new Object();
    private final SharedPreferences mSharedPreferences;

    public ManagedProvisioningSharedPreferences(Context context) {
        this.mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE, 0);
    }

    public long getProvisioningId() {
        return this.mSharedPreferences.getLong(KEY_PROVISIONING_ID, 0L);
    }

    public long incrementAndGetProvisioningId() {
        long provisioningId;
        synchronized (sWriteLock) {
            provisioningId = getProvisioningId() + 1;
            this.mSharedPreferences.edit().putLong(KEY_PROVISIONING_ID, provisioningId).commit();
        }
        return provisioningId;
    }
}
