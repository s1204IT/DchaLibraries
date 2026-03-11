package com.android.systemui.plugins;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.VersionInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PluginInstanceManager<T extends Plugin> {
    public static final String PLUGIN_PERMISSION = "com.android.systemui.permission.PLUGIN";
    private final boolean isDebuggable;
    private final String mAction;
    private final boolean mAllowMultiple;
    private final Context mContext;
    private final PluginListener<T> mListener;

    @VisibleForTesting
    final PluginInstanceManager<T>.MainHandler mMainHandler;
    private final PluginManagerImpl mManager;

    @VisibleForTesting
    final PluginInstanceManager<T>.PluginHandler mPluginHandler;
    private final PackageManager mPm;
    private final VersionInfo mVersion;

    PluginInstanceManager(Context context, String str, PluginListener<T> pluginListener, boolean z, Looper looper, VersionInfo versionInfo, PluginManagerImpl pluginManagerImpl) {
        this(context, context.getPackageManager(), str, pluginListener, z, looper, versionInfo, pluginManagerImpl, Build.IS_DEBUGGABLE);
    }

    @VisibleForTesting
    PluginInstanceManager(Context context, PackageManager packageManager, String str, PluginListener<T> pluginListener, boolean z, Looper looper, VersionInfo versionInfo, PluginManagerImpl pluginManagerImpl, boolean z2) {
        this.mMainHandler = new MainHandler(Looper.getMainLooper());
        this.mPluginHandler = new PluginHandler(looper);
        this.mManager = pluginManagerImpl;
        this.mContext = context;
        this.mPm = packageManager;
        this.mAction = str;
        this.mListener = pluginListener;
        this.mAllowMultiple = z;
        this.mVersion = versionInfo;
        this.isDebuggable = z2;
    }

    public PluginInfo<T> getPlugin() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new RuntimeException("Must be called from UI thread");
        }
        this.mPluginHandler.handleQueryPlugins(null);
        if (((PluginHandler) this.mPluginHandler).mPlugins.size() <= 0) {
            return null;
        }
        this.mMainHandler.removeMessages(1);
        PluginInfo<T> pluginInfo = (PluginInfo) ((PluginHandler) this.mPluginHandler).mPlugins.get(0);
        PluginPrefs.setHasPlugins(this.mContext);
        pluginInfo.mPlugin.onCreate(this.mContext, ((PluginInfo) pluginInfo).mPluginContext);
        return pluginInfo;
    }

    public void loadAll() {
        this.mPluginHandler.sendEmptyMessage(1);
    }

    public void destroy() {
        Iterator it = new ArrayList(((PluginHandler) this.mPluginHandler).mPlugins).iterator();
        while (it.hasNext()) {
            this.mMainHandler.obtainMessage(2, ((PluginInfo) it.next()).mPlugin).sendToTarget();
        }
    }

    public void onPackageRemoved(String str) {
        this.mPluginHandler.obtainMessage(3, str).sendToTarget();
    }

    public void onPackageChange(String str) {
        this.mPluginHandler.obtainMessage(3, str).sendToTarget();
        this.mPluginHandler.obtainMessage(2, str).sendToTarget();
    }

    public boolean checkAndDisable(String str) {
        boolean z = false;
        for (PluginInfo pluginInfo : new ArrayList(((PluginHandler) this.mPluginHandler).mPlugins)) {
            if (str.startsWith(pluginInfo.mPackage)) {
                disable(pluginInfo);
                z = true;
            }
        }
        return z;
    }

    public boolean disableAll() {
        ArrayList arrayList = new ArrayList(((PluginHandler) this.mPluginHandler).mPlugins);
        for (int i = 0; i < arrayList.size(); i++) {
            disable((PluginInfo) arrayList.get(i));
        }
        return arrayList.size() != 0;
    }

    private void disable(PluginInfo pluginInfo) {
        Log.w("PluginInstanceManager", "Disabling plugin " + pluginInfo.mPackage + "/" + pluginInfo.mClass);
        this.mPm.setComponentEnabledSetting(new ComponentName(pluginInfo.mPackage, pluginInfo.mClass), 2, 1);
    }

    public <T> boolean dependsOn(Plugin plugin, Class<T> cls) {
        for (PluginInfo pluginInfo : new ArrayList(((PluginHandler) this.mPluginHandler).mPlugins)) {
            if (pluginInfo.mPlugin.getClass().getName().equals(plugin.getClass().getName())) {
                return pluginInfo.mVersion != null && pluginInfo.mVersion.hasClass(cls);
            }
        }
        return false;
    }

    public String toString() {
        return String.format("%s@%s (action=%s)", getClass().getSimpleName(), Integer.valueOf(hashCode()), this.mAction);
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    PluginPrefs.setHasPlugins(PluginInstanceManager.this.mContext);
                    PluginInfo pluginInfo = (PluginInfo) message.obj;
                    PluginInstanceManager.this.mManager.handleWtfs();
                    if (!(message.obj instanceof PluginFragment)) {
                        ((Plugin) pluginInfo.mPlugin).onCreate(PluginInstanceManager.this.mContext, pluginInfo.mPluginContext);
                    }
                    PluginInstanceManager.this.mListener.onPluginConnected((Plugin) pluginInfo.mPlugin, pluginInfo.mPluginContext);
                    break;
                case 2:
                    PluginInstanceManager.this.mListener.onPluginDisconnected((Plugin) message.obj);
                    if (!(message.obj instanceof PluginFragment)) {
                        ((Plugin) message.obj).onDestroy();
                    }
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    }

    private class PluginHandler extends Handler {
        private final ArrayList<PluginInfo<T>> mPlugins;

        public PluginHandler(Looper looper) {
            super(looper);
            this.mPlugins = new ArrayList<>();
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    for (int size = this.mPlugins.size() - 1; size >= 0; size--) {
                        PluginInfo<T> pluginInfo = this.mPlugins.get(size);
                        PluginInstanceManager.this.mListener.onPluginDisconnected(pluginInfo.mPlugin);
                        if (!(pluginInfo.mPlugin instanceof PluginFragment)) {
                            pluginInfo.mPlugin.onDestroy();
                        }
                    }
                    this.mPlugins.clear();
                    handleQueryPlugins(null);
                    break;
                case 2:
                    String str = (String) message.obj;
                    if (PluginInstanceManager.this.mAllowMultiple || this.mPlugins.size() == 0) {
                        handleQueryPlugins(str);
                    }
                    break;
                case 3:
                    String str2 = (String) message.obj;
                    for (int size2 = this.mPlugins.size() - 1; size2 >= 0; size2--) {
                        PluginInfo<T> pluginInfo2 = this.mPlugins.get(size2);
                        if (pluginInfo2.mPackage.equals(str2)) {
                            PluginInstanceManager.this.mMainHandler.obtainMessage(2, pluginInfo2.mPlugin).sendToTarget();
                            this.mPlugins.remove(size2);
                        }
                    }
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }

        public void handleQueryPlugins(String str) {
            Intent intent = new Intent(PluginInstanceManager.this.mAction);
            if (str != null) {
                intent.setPackage(str);
            }
            List<ResolveInfo> listQueryIntentServices = PluginInstanceManager.this.mPm.queryIntentServices(intent, 0);
            if (listQueryIntentServices.size() > 1 && !PluginInstanceManager.this.mAllowMultiple) {
                Log.w("PluginInstanceManager", "Multiple plugins found for " + PluginInstanceManager.this.mAction);
                return;
            }
            for (ResolveInfo resolveInfo : listQueryIntentServices) {
                PluginInfo<T> pluginInfoHandleLoadPlugin = handleLoadPlugin(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
                if (pluginInfoHandleLoadPlugin != null) {
                    PluginInstanceManager.this.mMainHandler.obtainMessage(1, pluginInfoHandleLoadPlugin).sendToTarget();
                    this.mPlugins.add(pluginInfoHandleLoadPlugin);
                }
            }
        }

        protected PluginInfo<T> handleLoadPlugin(ComponentName componentName) {
            String string;
            if (!PluginInstanceManager.this.isDebuggable) {
                Log.d("PluginInstanceManager", "Somehow hit second debuggable check");
                return null;
            }
            String packageName = componentName.getPackageName();
            String className = componentName.getClassName();
            try {
                ApplicationInfo applicationInfo = PluginInstanceManager.this.mPm.getApplicationInfo(packageName, 0);
                if (PluginInstanceManager.this.mPm.checkPermission(PluginInstanceManager.PLUGIN_PERMISSION, packageName) == 0) {
                    ClassLoader classLoader = PluginInstanceManager.this.mManager.getClassLoader(applicationInfo.sourceDir, applicationInfo.packageName);
                    PluginContextWrapper pluginContextWrapper = new PluginContextWrapper(PluginInstanceManager.this.mContext.createApplicationContext(applicationInfo, 0), classLoader);
                    Class<?> cls = Class.forName(className, true, classLoader);
                    Plugin plugin = (Plugin) cls.newInstance();
                    try {
                        return new PluginInfo<>(packageName, className, plugin, pluginContextWrapper, checkVersion(cls, plugin, PluginInstanceManager.this.mVersion));
                    } catch (VersionInfo.InvalidVersionException e) {
                        Notification.Builder color = new Notification.Builder(PluginInstanceManager.this.mContext, PluginManager.NOTIFICATION_CHANNEL_ID).setStyle(new Notification.BigTextStyle()).setSmallIcon(PluginInstanceManager.this.mContext.getResources().getIdentifier("tuner", "drawable", PluginInstanceManager.this.mContext.getPackageName())).setWhen(0L).setShowWhen(false).setVisibility(1).setColor(PluginInstanceManager.this.mContext.getColor(Resources.getSystem().getIdentifier("system_notification_accent_color", "color", "android")));
                        try {
                            string = PluginInstanceManager.this.mPm.getServiceInfo(componentName, 0).loadLabel(PluginInstanceManager.this.mPm).toString();
                        } catch (PackageManager.NameNotFoundException e2) {
                            string = className;
                        }
                        if (!e.isTooNew()) {
                            Notification.Builder contentTitle = color.setContentTitle("Plugin \"" + string + "\" is too old");
                            StringBuilder sb = new StringBuilder();
                            sb.append("Contact plugin developer to get an updated version.\n");
                            sb.append(e.getMessage());
                            contentTitle.setContentText(sb.toString());
                        } else {
                            Notification.Builder contentTitle2 = color.setContentTitle("Plugin \"" + string + "\" is too new");
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Check to see if an OTA is available.\n");
                            sb2.append(e.getMessage());
                            contentTitle2.setContentText(sb2.toString());
                        }
                        color.addAction(new Notification.Action.Builder((Icon) null, "Disable plugin", PendingIntent.getBroadcast(PluginInstanceManager.this.mContext, 0, new Intent("com.android.systemui.action.DISABLE_PLUGIN").setData(Uri.parse("package://" + componentName.flattenToString())), 0)).build());
                        ((NotificationManager) PluginInstanceManager.this.mContext.getSystemService(NotificationManager.class)).notifyAsUser(className, 6, color.build(), UserHandle.ALL);
                        Log.w("PluginInstanceManager", "Plugin has invalid interface version " + plugin.getVersion() + ", expected " + PluginInstanceManager.this.mVersion);
                        return null;
                    }
                }
                Log.d("PluginInstanceManager", "Plugin doesn't have permission: " + packageName);
                return null;
            } catch (Throwable th) {
                Log.w("PluginInstanceManager", "Couldn't load plugin: " + packageName, th);
                return null;
            }
        }

        private VersionInfo checkVersion(Class<?> cls, T t, VersionInfo versionInfo) throws VersionInfo.InvalidVersionException {
            VersionInfo versionInfoAddClass = new VersionInfo().addClass(cls);
            if (versionInfoAddClass.hasVersionInfo()) {
                versionInfo.checkVersion(versionInfoAddClass);
                return versionInfoAddClass;
            }
            if (t.getVersion() != versionInfo.getDefaultVersion()) {
                throw new VersionInfo.InvalidVersionException("Invalid legacy version", false);
            }
            return null;
        }
    }

    public static class PluginContextWrapper extends ContextWrapper {
        private final ClassLoader mClassLoader;
        private LayoutInflater mInflater;

        public PluginContextWrapper(Context context, ClassLoader classLoader) {
            super(context);
            this.mClassLoader = classLoader;
        }

        @Override
        public ClassLoader getClassLoader() {
            return this.mClassLoader;
        }

        @Override
        public Object getSystemService(String str) {
            if ("layout_inflater".equals(str)) {
                if (this.mInflater == null) {
                    this.mInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
                }
                return this.mInflater;
            }
            return getBaseContext().getSystemService(str);
        }
    }

    static class PluginInfo<T> {
        private String mClass;
        String mPackage;
        T mPlugin;
        private final Context mPluginContext;
        private final VersionInfo mVersion;

        public PluginInfo(String str, String str2, T t, Context context, VersionInfo versionInfo) {
            this.mPlugin = t;
            this.mClass = str2;
            this.mPackage = str;
            this.mPluginContext = context;
            this.mVersion = versionInfo;
        }
    }
}
