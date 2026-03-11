package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import com.android.systemui.Dependency;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import java.util.ArrayList;

public class DeviceProvisionedControllerImpl extends CurrentUserTracker implements DeviceProvisionedController {
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private final Uri mDeviceProvisionedUri;
    private final ArrayList<DeviceProvisionedController.DeviceProvisionedListener> mListeners;
    protected final ContentObserver mSettingsObserver;
    private final Uri mUserSetupUri;

    public DeviceProvisionedControllerImpl(Context context) {
        super(context);
        this.mListeners = new ArrayList<>();
        this.mSettingsObserver = new ContentObserver((Handler) Dependency.get(Dependency.MAIN_HANDLER)) {
            @Override
            public void onChange(boolean z, Uri uri, int i) {
                if (DeviceProvisionedControllerImpl.this.mUserSetupUri.equals(uri)) {
                    DeviceProvisionedControllerImpl.this.notifySetupChanged();
                } else {
                    DeviceProvisionedControllerImpl.this.notifyProvisionedChanged();
                }
            }
        };
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mDeviceProvisionedUri = Settings.Global.getUriFor("device_provisioned");
        this.mUserSetupUri = Settings.Secure.getUriFor("user_setup_complete");
    }

    @Override
    public boolean isDeviceProvisioned() {
        return Settings.Global.getInt(this.mContentResolver, "device_provisioned", 0) != 0;
    }

    @Override
    public boolean isUserSetup(int i) {
        return Settings.Secure.getIntForUser(this.mContentResolver, "user_setup_complete", 0, i) != 0;
    }

    @Override
    public int getCurrentUser() {
        return ActivityManager.getCurrentUser();
    }

    @Override
    public void addCallback(DeviceProvisionedController.DeviceProvisionedListener deviceProvisionedListener) {
        this.mListeners.add(deviceProvisionedListener);
        if (this.mListeners.size() == 1) {
            startListening(getCurrentUser());
        }
        deviceProvisionedListener.onUserSetupChanged();
        deviceProvisionedListener.onDeviceProvisionedChanged();
    }

    @Override
    public void removeCallback(DeviceProvisionedController.DeviceProvisionedListener deviceProvisionedListener) {
        this.mListeners.remove(deviceProvisionedListener);
        if (this.mListeners.size() == 0) {
            stopListening();
        }
    }

    private void startListening(int i) {
        this.mContentResolver.registerContentObserver(this.mDeviceProvisionedUri, true, this.mSettingsObserver, 0);
        this.mContentResolver.registerContentObserver(this.mUserSetupUri, true, this.mSettingsObserver, i);
        startTracking();
    }

    private void stopListening() {
        stopTracking();
        this.mContentResolver.unregisterContentObserver(this.mSettingsObserver);
    }

    @Override
    public void onUserSwitched(int i) {
        this.mContentResolver.unregisterContentObserver(this.mSettingsObserver);
        this.mContentResolver.registerContentObserver(this.mDeviceProvisionedUri, true, this.mSettingsObserver, 0);
        this.mContentResolver.registerContentObserver(this.mUserSetupUri, true, this.mSettingsObserver, i);
        notifyUserChanged();
    }

    private void notifyUserChanged() {
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onUserSwitched();
        }
    }

    public void notifySetupChanged() {
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onUserSetupChanged();
        }
    }

    public void notifyProvisionedChanged() {
        for (int size = this.mListeners.size() - 1; size >= 0; size--) {
            this.mListeners.get(size).onDeviceProvisionedChanged();
        }
    }
}
