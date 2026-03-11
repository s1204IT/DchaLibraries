package com.android.systemui;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.keyboard.KeyboardUI;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.media.RingtonePlayer;
import com.android.systemui.power.PowerUI;
import com.android.systemui.recents.Recents;
import com.android.systemui.shortcut.ShortcutKeyDispatcher;
import com.android.systemui.stackdivider.Divider;
import com.android.systemui.statusbar.SystemBars;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tv.pip.PipUI;
import com.android.systemui.usb.StorageNotification;
import com.android.systemui.volume.VolumeUI;
import java.util.HashMap;
import java.util.Map;

public class SystemUIApplication extends Application {
    private boolean mBootCompleted;
    private boolean mServicesStarted;
    private final Class<?>[] SERVICES = {TunerService.class, KeyguardViewMediator.class, Recents.class, VolumeUI.class, Divider.class, SystemBars.class, StorageNotification.class, PowerUI.class, RingtonePlayer.class, KeyboardUI.class, PipUI.class, ShortcutKeyDispatcher.class};
    private final Class<?>[] SERVICES_PER_USER = {Recents.class, PipUI.class};
    private final SystemUI[] mServices = new SystemUI[this.SERVICES.length];
    private final Map<Class<?>, Object> mComponents = new HashMap();

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.systemui_theme);
        SystemUIFactory.createFromConfig(this);
        if (Process.myUserHandle().equals(UserHandle.SYSTEM)) {
            IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            filter.setPriority(1000);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (SystemUIApplication.this.mBootCompleted) {
                        return;
                    }
                    SystemUIApplication.this.unregisterReceiver(this);
                    SystemUIApplication.this.mBootCompleted = true;
                    if (!SystemUIApplication.this.mServicesStarted) {
                        return;
                    }
                    int N = SystemUIApplication.this.mServices.length;
                    for (int i = 0; i < N; i++) {
                        SystemUIApplication.this.mServices[i].onBootCompleted();
                    }
                }
            }, filter);
            return;
        }
        startServicesIfNeeded(this.SERVICES_PER_USER);
    }

    public void startServicesIfNeeded() {
        startServicesIfNeeded(this.SERVICES);
    }

    void startSecondaryUserServicesIfNeeded() {
        startServicesIfNeeded(this.SERVICES_PER_USER);
    }

    private void startServicesIfNeeded(Class<?>[] services) {
        if (this.mServicesStarted) {
            return;
        }
        if (!this.mBootCompleted && "1".equals(SystemProperties.get("sys.boot_completed"))) {
            this.mBootCompleted = true;
        }
        Log.v("SystemUIService", "Starting SystemUI services for user " + Process.myUserHandle().getIdentifier() + ".");
        int N = services.length;
        for (int i = 0; i < N; i++) {
            Class<?> cl = services[i];
            try {
                Object newService = SystemUIFactory.getInstance().createInstance(cl);
                this.mServices[i] = (SystemUI) (newService == null ? cl.newInstance() : newService);
                this.mServices[i].mContext = this;
                this.mServices[i].mComponents = this.mComponents;
                this.mServices[i].start();
                if (this.mBootCompleted) {
                    this.mServices[i].onBootCompleted();
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (InstantiationException ex2) {
                throw new RuntimeException(ex2);
            }
        }
        this.mServicesStarted = true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!this.mServicesStarted) {
            return;
        }
        int len = this.mServices.length;
        for (int i = 0; i < len; i++) {
            if (this.mServices[i] != null) {
                this.mServices[i].onConfigurationChanged(newConfig);
            }
        }
    }

    public <T> T getComponent(Class<T> cls) {
        return (T) this.mComponents.get(cls);
    }

    public SystemUI[] getServices() {
        return this.mServices;
    }
}
