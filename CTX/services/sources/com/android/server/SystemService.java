package com.android.server;

import android.app.ActivityThread;
import android.content.Context;
import android.os.IBinder;
import android.os.ServiceManager;

public abstract class SystemService {
    public static final int PHASE_ACTIVITY_MANAGER_READY = 550;
    public static final int PHASE_BOOT_COMPLETED = 1000;
    public static final int PHASE_DEVICE_SPECIFIC_SERVICES_READY = 520;
    public static final int PHASE_LOCK_SETTINGS_READY = 480;
    public static final int PHASE_SYSTEM_SERVICES_READY = 500;
    public static final int PHASE_THIRD_PARTY_APPS_CAN_START = 600;
    public static final int PHASE_WAIT_FOR_DEFAULT_DISPLAY = 100;
    private final Context mContext;

    public abstract void onStart();

    public SystemService(Context context) {
        this.mContext = context;
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final Context getUiContext() {
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    public final boolean isSafeMode() {
        return getManager().isSafeMode();
    }

    public void onBootPhase(int i) {
    }

    public void onStartUser(int i) {
    }

    public void onUnlockUser(int i) {
    }

    public void onSwitchUser(int i) {
    }

    public void onStopUser(int i) {
    }

    public void onCleanupUser(int i) {
    }

    protected final void publishBinderService(String str, IBinder iBinder) {
        publishBinderService(str, iBinder, false);
    }

    protected final void publishBinderService(String str, IBinder iBinder, boolean z) {
        publishBinderService(str, iBinder, z, 8);
    }

    protected final void publishBinderService(String str, IBinder iBinder, boolean z, int i) {
        ServiceManager.addService(str, iBinder, z, i);
    }

    protected final IBinder getBinderService(String str) {
        return ServiceManager.getService(str);
    }

    protected final <T> void publishLocalService(Class<T> cls, T t) {
        LocalServices.addService(cls, t);
    }

    protected final <T> T getLocalService(Class<T> cls) {
        return (T) LocalServices.getService(cls);
    }

    private SystemServiceManager getManager() {
        return (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
    }
}
