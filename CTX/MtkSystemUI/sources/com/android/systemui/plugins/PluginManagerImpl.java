package com.android.systemui.plugins;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.PluginInstanceManager;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import dalvik.system.PathClassLoader;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.Thread;
import java.util.Iterator;
import java.util.Map;

public class PluginManagerImpl extends BroadcastReceiver implements PluginManager {
    private final boolean isDebuggable;
    private final Map<String, ClassLoader> mClassLoaders;
    private final Context mContext;
    private final PluginInstanceManagerFactory mFactory;
    private boolean mHasOneShot;
    private boolean mListening;
    private Looper mLooper;
    private final ArraySet<String> mOneShotPackages;
    private ClassLoaderFilter mParentClassLoader;
    private final ArrayMap<PluginListener<?>, PluginInstanceManager> mPluginMap;
    private final PluginPrefs mPluginPrefs;
    private boolean mWtfsSet;

    public PluginManagerImpl(Context context) {
        this(context, new PluginInstanceManagerFactory(), Build.IS_DEBUGGABLE, Thread.getUncaughtExceptionPreHandler());
    }

    @VisibleForTesting
    PluginManagerImpl(Context context, PluginInstanceManagerFactory pluginInstanceManagerFactory, boolean z, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.mPluginMap = new ArrayMap<>();
        this.mClassLoaders = new ArrayMap();
        this.mOneShotPackages = new ArraySet<>();
        this.mContext = context;
        this.mFactory = pluginInstanceManagerFactory;
        this.mLooper = (Looper) Dependency.get(Dependency.BG_LOOPER);
        this.isDebuggable = z;
        this.mPluginPrefs = new PluginPrefs(this.mContext);
        Thread.setUncaughtExceptionPreHandler(new PluginExceptionHandler(uncaughtExceptionHandler));
        if (this.isDebuggable) {
            new Handler(this.mLooper).post(new Runnable() {
                @Override
                public final void run() {
                    ((PluginDependencyProvider) Dependency.get(PluginDependencyProvider.class)).allowPluginDependency(ActivityStarter.class);
                }
            });
        }
    }

    @Override
    public <T extends Plugin> T getOneShotPlugin(Class<T> cls) {
        ProvidesInterface providesInterface = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
        if (providesInterface == null) {
            throw new RuntimeException(cls + " doesn't provide an interface");
        }
        if (TextUtils.isEmpty(providesInterface.action())) {
            throw new RuntimeException(cls + " doesn't provide an action");
        }
        return (T) getOneShotPlugin(providesInterface.action(), cls);
    }

    @Override
    public <T extends Plugin> T getOneShotPlugin(String str, Class<?> cls) {
        if (!this.isDebuggable) {
            return null;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("Must be called from UI thread");
        }
        PluginInstanceManager pluginInstanceManagerCreatePluginInstanceManager = this.mFactory.createPluginInstanceManager(this.mContext, str, null, false, this.mLooper, cls, this);
        this.mPluginPrefs.addAction(str);
        PluginInstanceManager.PluginInfo<T> plugin = pluginInstanceManagerCreatePluginInstanceManager.getPlugin();
        if (plugin == null) {
            return null;
        }
        this.mOneShotPackages.add(plugin.mPackage);
        this.mHasOneShot = true;
        startListening();
        return plugin.mPlugin;
    }

    @Override
    public <T extends Plugin> void addPluginListener(PluginListener<T> pluginListener, Class<?> cls) {
        addPluginListener((PluginListener) pluginListener, cls, false);
    }

    @Override
    public <T extends Plugin> void addPluginListener(PluginListener<T> pluginListener, Class<?> cls, boolean z) {
        addPluginListener(PluginManager.getAction(cls), pluginListener, cls, z);
    }

    @Override
    public <T extends Plugin> void addPluginListener(String str, PluginListener<T> pluginListener, Class<?> cls) {
        addPluginListener(str, pluginListener, cls, false);
    }

    @Override
    public <T extends Plugin> void addPluginListener(String str, PluginListener<T> pluginListener, Class cls, boolean z) {
        if (!this.isDebuggable) {
            return;
        }
        this.mPluginPrefs.addAction(str);
        PluginInstanceManager pluginInstanceManagerCreatePluginInstanceManager = this.mFactory.createPluginInstanceManager(this.mContext, str, pluginListener, z, this.mLooper, cls, this);
        pluginInstanceManagerCreatePluginInstanceManager.loadAll();
        this.mPluginMap.put(pluginListener, pluginInstanceManagerCreatePluginInstanceManager);
        startListening();
    }

    @Override
    public void removePluginListener(PluginListener<?> pluginListener) {
        if (this.isDebuggable && this.mPluginMap.containsKey(pluginListener)) {
            this.mPluginMap.remove(pluginListener).destroy();
            if (this.mPluginMap.size() == 0) {
                stopListening();
            }
        }
    }

    private void startListening() {
        if (this.mListening) {
            return;
        }
        this.mListening = true;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction(PluginManager.PLUGIN_CHANGED);
        intentFilter.addAction("com.android.systemui.action.DISABLE_PLUGIN");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(this, intentFilter);
        this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.USER_UNLOCKED"));
    }

    private void stopListening() {
        if (!this.mListening || this.mHasOneShot) {
            return;
        }
        this.mListening = false;
        this.mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String string;
        if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
            Iterator<PluginInstanceManager> it = this.mPluginMap.values().iterator();
            while (it.hasNext()) {
                it.next().loadAll();
            }
            return;
        }
        if ("com.android.systemui.action.DISABLE_PLUGIN".equals(intent.getAction())) {
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(intent.getData().toString().substring(10));
            this.mContext.getPackageManager().setComponentEnabledSetting(componentNameUnflattenFromString, 2, 1);
            ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).cancel(componentNameUnflattenFromString.getClassName(), 6);
            return;
        }
        String encodedSchemeSpecificPart = intent.getData().getEncodedSchemeSpecificPart();
        if (this.mOneShotPackages.contains(encodedSchemeSpecificPart)) {
            int identifier = this.mContext.getResources().getIdentifier("tuner", "drawable", this.mContext.getPackageName());
            int identifier2 = Resources.getSystem().getIdentifier("system_notification_accent_color", "color", "android");
            try {
                PackageManager packageManager = this.mContext.getPackageManager();
                string = packageManager.getApplicationInfo(encodedSchemeSpecificPart, 0).loadLabel(packageManager).toString();
            } catch (PackageManager.NameNotFoundException e) {
                string = encodedSchemeSpecificPart;
            }
            Notification.Builder contentText = new Notification.Builder(this.mContext, PluginManager.NOTIFICATION_CHANNEL_ID).setSmallIcon(identifier).setWhen(0L).setShowWhen(false).setPriority(2).setVisibility(1).setColor(this.mContext.getColor(identifier2)).setContentTitle("Plugin \"" + string + "\" has updated").setContentText("Restart SysUI for changes to take effect.");
            contentText.addAction(new Notification.Action.Builder((Icon) null, "Restart SysUI", PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.systemui.action.RESTART").setData(Uri.parse("package://" + encodedSchemeSpecificPart)), 0)).build());
            ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(encodedSchemeSpecificPart, 6, contentText.build(), UserHandle.ALL);
        }
        if (clearClassLoader(encodedSchemeSpecificPart)) {
            Toast.makeText(this.mContext, "Reloading " + encodedSchemeSpecificPart, 1).show();
        }
        if (!"android.intent.action.PACKAGE_REMOVED".equals(intent.getAction())) {
            Iterator<PluginInstanceManager> it2 = this.mPluginMap.values().iterator();
            while (it2.hasNext()) {
                it2.next().onPackageChange(encodedSchemeSpecificPart);
            }
        } else {
            Iterator<PluginInstanceManager> it3 = this.mPluginMap.values().iterator();
            while (it3.hasNext()) {
                it3.next().onPackageRemoved(encodedSchemeSpecificPart);
            }
        }
    }

    public ClassLoader getClassLoader(String str, String str2) {
        if (this.mClassLoaders.containsKey(str2)) {
            return this.mClassLoaders.get(str2);
        }
        PathClassLoader pathClassLoader = new PathClassLoader(str, getParentClassLoader());
        this.mClassLoaders.put(str2, pathClassLoader);
        return pathClassLoader;
    }

    private boolean clearClassLoader(String str) {
        return this.mClassLoaders.remove(str) != null;
    }

    ClassLoader getParentClassLoader() {
        if (this.mParentClassLoader == null) {
            this.mParentClassLoader = new ClassLoaderFilter(getClass().getClassLoader(), "com.android.systemui.plugin");
        }
        return this.mParentClassLoader;
    }

    public Context getContext(ApplicationInfo applicationInfo, String str) throws PackageManager.NameNotFoundException {
        return new PluginInstanceManager.PluginContextWrapper(this.mContext.createApplicationContext(applicationInfo, 0), getClassLoader(applicationInfo.sourceDir, str));
    }

    @Override
    public <T> boolean dependsOn(Plugin plugin, Class<T> cls) {
        for (int i = 0; i < this.mPluginMap.size(); i++) {
            if (this.mPluginMap.valueAt(i).dependsOn(plugin, cls)) {
                return true;
            }
        }
        return false;
    }

    public void handleWtfs() {
        if (!this.mWtfsSet) {
            this.mWtfsSet = true;
            Log.setWtfHandler(new Log.TerribleFailureHandler() {
                public final void onTerribleFailure(String str, Log.TerribleFailure terribleFailure, boolean z) {
                    PluginManagerImpl.lambda$handleWtfs$1(this.f$0, str, terribleFailure, z);
                }
            });
        }
    }

    public static void lambda$handleWtfs$1(PluginManagerImpl pluginManagerImpl, String str, Log.TerribleFailure terribleFailure, boolean z) {
        throw pluginManagerImpl.new CrashWhilePluginActiveException(terribleFailure);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(String.format("  plugin map (%d):", Integer.valueOf(this.mPluginMap.size())));
        for (PluginListener<?> pluginListener : this.mPluginMap.keySet()) {
            printWriter.println(String.format("    %s -> %s", pluginListener, this.mPluginMap.get(pluginListener)));
        }
    }

    @VisibleForTesting
    public static class PluginInstanceManagerFactory {
        public <T extends Plugin> PluginInstanceManager createPluginInstanceManager(Context context, String str, PluginListener<T> pluginListener, boolean z, Looper looper, Class<?> cls, PluginManagerImpl pluginManagerImpl) {
            return new PluginInstanceManager(context, str, pluginListener, z, looper, new VersionInfo().addClass(cls), pluginManagerImpl);
        }
    }

    private static class ClassLoaderFilter extends ClassLoader {
        private final ClassLoader mBase;
        private final String mPackage;

        public ClassLoaderFilter(ClassLoader classLoader, String str) {
            super(ClassLoader.getSystemClassLoader());
            this.mBase = classLoader;
            this.mPackage = str;
        }

        @Override
        protected Class<?> loadClass(String str, boolean z) throws ClassNotFoundException {
            if (!str.startsWith(this.mPackage)) {
                super.loadClass(str, z);
            }
            return this.mBase.loadClass(str);
        }
    }

    private class PluginExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler mHandler;

        private PluginExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
            this.mHandler = uncaughtExceptionHandler;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable th) {
            if (SystemProperties.getBoolean("plugin.debugging", false)) {
                this.mHandler.uncaughtException(thread, th);
                return;
            }
            boolean zCheckStack = checkStack(th);
            if (!zCheckStack) {
                Iterator it = PluginManagerImpl.this.mPluginMap.values().iterator();
                while (it.hasNext()) {
                    zCheckStack |= ((PluginInstanceManager) it.next()).disableAll();
                }
            }
            if (zCheckStack) {
                th = PluginManagerImpl.this.new CrashWhilePluginActiveException(th);
            }
            this.mHandler.uncaughtException(thread, th);
        }

        private boolean checkStack(Throwable th) {
            if (th == null) {
                return false;
            }
            boolean zCheckAndDisable = false;
            for (StackTraceElement stackTraceElement : th.getStackTrace()) {
                Iterator it = PluginManagerImpl.this.mPluginMap.values().iterator();
                while (it.hasNext()) {
                    zCheckAndDisable |= ((PluginInstanceManager) it.next()).checkAndDisable(stackTraceElement.getClassName());
                }
            }
            return checkStack(th.getCause()) | zCheckAndDisable;
        }
    }

    class CrashWhilePluginActiveException extends RuntimeException {
        public CrashWhilePluginActiveException(Throwable th) {
            super(th);
        }
    }
}
