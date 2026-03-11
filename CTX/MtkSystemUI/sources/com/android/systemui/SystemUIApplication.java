package com.android.systemui;

import android.app.ActivityThread;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.TimingsTraceLog;
import com.android.systemui.plugins.OverlayPlugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.util.NotificationChannels;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SystemUIApplication extends Application implements SysUiServiceProvider {
    private boolean mBootCompleted;
    private final Map<Class<?>, Object> mComponents = new HashMap();
    private SystemUI[] mServices;
    private boolean mServicesStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(2131886642);
        SystemUIFactory.createFromConfig(this);
        if (Process.myUserHandle().equals(UserHandle.SYSTEM)) {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            intentFilter.setPriority(1000);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (SystemUIApplication.this.mBootCompleted) {
                        return;
                    }
                    SystemUIApplication.this.unregisterReceiver(this);
                    SystemUIApplication.this.mBootCompleted = true;
                    if (SystemUIApplication.this.mServicesStarted) {
                        int length = SystemUIApplication.this.mServices.length;
                        for (int i = 0; i < length; i++) {
                            SystemUIApplication.this.mServices[i].onBootCompleted();
                        }
                    }
                }
            }, intentFilter);
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction()) && SystemUIApplication.this.mBootCompleted) {
                        NotificationChannels.createAll(context);
                    }
                }
            }, new IntentFilter("android.intent.action.LOCALE_CHANGED"));
            return;
        }
        String strCurrentProcessName = ActivityThread.currentProcessName();
        ApplicationInfo applicationInfo = getApplicationInfo();
        if (strCurrentProcessName != null) {
            if (strCurrentProcessName.startsWith(applicationInfo.processName + ":")) {
                return;
            }
        }
        startSecondaryUserServicesIfNeeded();
    }

    public void startServicesIfNeeded() {
        startServicesIfNeeded(getResources().getStringArray(R.array.config_systemUIServiceComponents));
    }

    void startSecondaryUserServicesIfNeeded() {
        startServicesIfNeeded(getResources().getStringArray(R.array.config_systemUIServiceComponentsPerUser));
    }

    private void startServicesIfNeeded(String[] strArr) {
        if (this.mServicesStarted) {
            return;
        }
        this.mServices = new SystemUI[strArr.length];
        if (!this.mBootCompleted && "1".equals(SystemProperties.get("sys.boot_completed"))) {
            this.mBootCompleted = true;
        }
        Log.v("SystemUIService", "Starting SystemUI services for user " + Process.myUserHandle().getIdentifier() + ".");
        TimingsTraceLog timingsTraceLog = new TimingsTraceLog("SystemUIBootTiming", 4096L);
        timingsTraceLog.traceBegin("StartServices");
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            String str = strArr[i];
            timingsTraceLog.traceBegin("StartServices" + str);
            long jCurrentTimeMillis = System.currentTimeMillis();
            try {
                Class<?> cls = Class.forName(str);
                this.mServices[i] = (SystemUI) cls.newInstance();
                this.mServices[i].mContext = this;
                this.mServices[i].mComponents = this.mComponents;
                this.mServices[i].start();
                timingsTraceLog.traceEnd();
                long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                if (jCurrentTimeMillis2 > 1000) {
                    Log.w("SystemUIService", "Initialization of " + cls.getName() + " took " + jCurrentTimeMillis2 + " ms");
                }
                if (this.mBootCompleted) {
                    this.mServices[i].onBootCompleted();
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e2) {
                throw new RuntimeException(e2);
            } catch (InstantiationException e3) {
                throw new RuntimeException(e3);
            }
        }
        timingsTraceLog.traceEnd();
        ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener((PluginListener) new AnonymousClass3(), OverlayPlugin.class, true);
        this.mServicesStarted = true;
    }

    class AnonymousClass3 implements PluginListener<OverlayPlugin> {
        private ArraySet<OverlayPlugin> mOverlays;

        AnonymousClass3() {
        }

        @Override
        public void onPluginConnected(OverlayPlugin overlayPlugin, Context context) {
            StatusBar statusBar = (StatusBar) SystemUIApplication.this.getComponent(StatusBar.class);
            if (statusBar != null) {
                overlayPlugin.setup(statusBar.getStatusBarWindow(), statusBar.getNavigationBarView());
            }
            if (this.mOverlays == null) {
                this.mOverlays = new ArraySet<>();
            }
            if (overlayPlugin.holdStatusBarOpen()) {
                this.mOverlays.add(overlayPlugin);
                ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setStateListener(new StatusBarWindowManager.OtherwisedCollapsedListener() {
                    @Override
                    public final void setWouldOtherwiseCollapse(boolean z) {
                        this.f$0.mOverlays.forEach(new Consumer() {
                            @Override
                            public final void accept(Object obj) {
                                ((OverlayPlugin) obj).setCollapseDesired(z);
                            }
                        });
                    }
                });
                ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setForcePluginOpen(this.mOverlays.size() != 0);
            }
        }

        @Override
        public void onPluginDisconnected(OverlayPlugin overlayPlugin) {
            this.mOverlays.remove(overlayPlugin);
            ((StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class)).setForcePluginOpen(this.mOverlays.size() != 0);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        if (this.mServicesStarted) {
            int length = this.mServices.length;
            for (int i = 0; i < length; i++) {
                if (this.mServices[i] != null) {
                    this.mServices[i].onConfigurationChanged(configuration);
                }
            }
        }
    }

    @Override
    public <T> T getComponent(Class<T> cls) {
        return (T) this.mComponents.get(cls);
    }

    public SystemUI[] getServices() {
        return this.mServices;
    }
}
