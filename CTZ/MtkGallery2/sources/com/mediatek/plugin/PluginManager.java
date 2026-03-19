package com.mediatek.plugin;

import android.content.Context;
import com.mediatek.plugin.element.PluginDescriptor;
import com.mediatek.plugin.parallel.Future;
import com.mediatek.plugin.parallel.FutureListener;
import com.mediatek.plugin.parallel.ThreadPool;
import com.mediatek.plugin.preload.Preloader;
import com.mediatek.plugin.utils.FileUtils;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.ReflectUtils;
import com.mediatek.plugin.utils.TraceHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PluginManager {
    private static final String DEX_DIR = "plugin_manager_dex";
    private static final String NATIVE_LIB_DIR = "plugin_manager_lib";
    private static final String TAG = "PluginManager/PluginManager";
    private static PluginManager sPluginManager;
    private Context mContext;
    private String mDexDir;
    private String mNativeLibDir;
    private ArrayList<String> mArchiveDir = new ArrayList<>();
    private PluginRegistry mRegistry = new PluginRegistry();

    public interface PreloaderListener {
        void onPreloadFinished();
    }

    public static PluginManager getInstance(Context context) {
        if (sPluginManager == null) {
            sPluginManager = new PluginManager(context);
        }
        return sPluginManager;
    }

    protected PluginManager(Context context) {
        this.mContext = context.getApplicationContext();
        this.mNativeLibDir = this.mContext.getDir(NATIVE_LIB_DIR, 0).getAbsolutePath();
        this.mDexDir = this.mContext.getDir(DEX_DIR, 0).getAbsolutePath();
    }

    public void addPluginDir(String str) {
        Log.d(TAG, "<addPluginDir> path = " + str);
        this.mArchiveDir.add(str);
    }

    public void preloadAllPlugins(PreloaderListener preloaderListener) {
        preloadAllPlugins(true, true, true, preloaderListener);
    }

    public void preloadAllPlugins(final boolean z, final boolean z2, final boolean z3, final PreloaderListener preloaderListener) {
        final ArrayList<String> allArchivePath = getAllArchivePath();
        if (allArchivePath == null || allArchivePath.size() == 0) {
            Log.d(TAG, "<preloadAllPlugins> archivePaths empty, call onPreloadFinished directly");
            preloaderListener.onPreloadFinished();
            return;
        }
        int size = allArchivePath.size();
        final CountDownLatch countDownLatch = new CountDownLatch(size);
        for (int i = 0; i < size; i++) {
            final int i2 = i;
            ThreadPool.getInstance().submit(new ThreadPool.Job<Void>() {
                @Override
                public Void run(ThreadPool.JobContext jobContext) {
                    Log.d(PluginManager.TAG, "<preloadAllPlugins> plugin path " + ((String) allArchivePath.get(i2)));
                    PluginDescriptor pluginDescriptorPreloadPlugin = Preloader.getInstance().preloadPlugin(PluginManager.this.mContext, (String) allArchivePath.get(i2), PluginManager.this.mNativeLibDir, z, z2);
                    Log.d(PluginManager.TAG, "<preloadAllPlugins> pluginDescriptor " + pluginDescriptorPreloadPlugin);
                    if (pluginDescriptorPreloadPlugin != null) {
                        PluginManager.this.mRegistry.addPluginDescriptor(pluginDescriptorPreloadPlugin);
                        return null;
                    }
                    return null;
                }
            }, new FutureListener<Void>() {
                @Override
                public synchronized void onFutureDone(Future<Void> future) {
                    countDownLatch.countDown();
                    Log.d(PluginManager.TAG, "<preloadAllPlugins.onFutureDone> latch count " + countDownLatch.getCount());
                    if (countDownLatch.getCount() != 0) {
                        return;
                    }
                    PluginManager.this.mRegistry.generateRelationship();
                    preloaderListener.onPreloadFinished();
                    Log.d(PluginManager.TAG, "<preloadAllPlugins.onFutureDone> onPreloadFinished done!");
                    if (z3) {
                        for (final String str : PluginManager.this.mRegistry.getAllPluginsId()) {
                            ThreadPool.getInstance().submit(new ThreadPool.Job<Void>() {
                                @Override
                                public Void run(ThreadPool.JobContext jobContext) {
                                    PluginManager.this.getPlugin(str);
                                    return null;
                                }
                            });
                        }
                    }
                }
            });
        }
    }

    public PluginRegistry getRegistry() {
        return this.mRegistry;
    }

    public Plugin getPlugin(String str) {
        TraceHelper.beginSection(">>>>PluginManager-getPlugin");
        Log.d(TAG, "<getPlugin> begin");
        PluginDescriptor pluginDescriptor = this.mRegistry.getPluginDescriptor(str);
        if (pluginDescriptor == null) {
            Log.d(TAG, "<getPlugin> pluginDescriptor is null");
            TraceHelper.endSection();
            return null;
        }
        synchronized (pluginDescriptor) {
            Plugin plugin = this.mRegistry.getPlugin(str);
            if (plugin != null) {
                Log.d(TAG, "<getPlugin> plugin != null");
                TraceHelper.endSection();
                return plugin;
            }
            Plugin pluginActivePlugin = activePlugin(str, pluginDescriptor);
            TraceHelper.endSection();
            return pluginActivePlugin;
        }
    }

    private Plugin activePlugin(String str, PluginDescriptor pluginDescriptor) {
        TraceHelper.beginSection(">>>>PluginManager-activePlugin");
        Log.d(TAG, "<activePlugin> begin, pluginId " + str);
        HashMap<String, PluginDescriptor> requirePluginDes = pluginDescriptor.getRequirePluginDes();
        if (requirePluginDes == null || requirePluginDes.isEmpty()) {
            Plugin pluginDoActivePlugin = doActivePlugin(str, pluginDescriptor);
            TraceHelper.endSection();
            Log.d(TAG, "<activePlugin> end, requirePluginDes size is 0, doactive itsself! plugin id " + str);
            return pluginDoActivePlugin;
        }
        for (Map.Entry<String, PluginDescriptor> entry : requirePluginDes.entrySet()) {
            if (entry.getValue() != null) {
                String str2 = entry.getValue().id;
                if (this.mRegistry.getPlugin(str2) == null) {
                    if (activePlugin(str2, entry.getValue()) == null) {
                        throw new IllegalStateException("Active required plugin failed!");
                    }
                    Plugin pluginDoActivePlugin2 = doActivePlugin(str, pluginDescriptor);
                    TraceHelper.endSection();
                    Log.d(TAG, "<activePlugin> end, doactive itsself! plugin id " + str);
                    return pluginDoActivePlugin2;
                }
            }
        }
        Log.d(TAG, "<activePlugin> end, retun null");
        TraceHelper.endSection();
        return null;
    }

    private Plugin doActivePlugin(String str, PluginDescriptor pluginDescriptor) {
        TraceHelper.beginSection(">>>>PluginManager-doActivePlugin");
        Log.d(TAG, "<doActivePlugin> begin, pluginId " + str);
        PluginClassLoader pluginClassLoader = new PluginClassLoader(pluginDescriptor.getArchivePath(), this.mDexDir, this.mNativeLibDir, getClass().getClassLoader());
        pluginClassLoader.setRequiredClassLoader(getRequiredClassLoader(pluginDescriptor));
        try {
            Log.d(TAG, "<doActivePlugin> pluginDescriptor.className " + pluginDescriptor.className);
            Plugin plugin = (Plugin) ReflectUtils.createInstance(ReflectUtils.getConstructor(pluginClassLoader.loadClass(pluginDescriptor.className), (Class<?>[]) new Class[]{PluginDescriptor.class, ClassLoader.class}), pluginDescriptor, pluginClassLoader);
            plugin.start();
            this.mRegistry.setPlugin(str, plugin);
            TraceHelper.endSection();
            return plugin;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "<doActivePlugin> ClassNotFoundException, pluginId " + str);
            e.printStackTrace();
            TraceHelper.endSection();
            return null;
        }
    }

    private ArrayList<ClassLoader> getRequiredClassLoader(PluginDescriptor pluginDescriptor) {
        TraceHelper.beginSection(">>>>PluginManager-getRequiredClassLoader");
        Log.d(TAG, "<getRequiredClassLoader> begin");
        ArrayList<ClassLoader> arrayList = new ArrayList<>();
        HashMap<String, PluginDescriptor> requirePluginDes = pluginDescriptor.getRequirePluginDes();
        if (requirePluginDes == null) {
            Log.d(TAG, "<getRequiredClassLoader> end, requirePluginDes == null");
            TraceHelper.endSection();
            return null;
        }
        Iterator<Map.Entry<String, PluginDescriptor>> it = requirePluginDes.entrySet().iterator();
        while (it.hasNext()) {
            Plugin plugin = this.mRegistry.getPlugin(it.next().getValue().id);
            if (plugin != null) {
                arrayList.add(plugin.getClassLoader());
            }
        }
        Log.d(TAG, "<getRequiredClassLoader> end");
        TraceHelper.endSection();
        return arrayList;
    }

    private ArrayList<String> getAllArchivePath() {
        Log.d(TAG, "<getAllArchivePath> begin");
        ArrayList<String> arrayList = new ArrayList<>();
        Iterator<String> it = this.mArchiveDir.iterator();
        while (it.hasNext()) {
            for (String str : FileUtils.getAllFile(it.next())) {
                if (str.endsWith(".jar") || str.endsWith(".apk")) {
                    arrayList.add(str);
                    Log.d(TAG, "<getAllArchivePath> " + str);
                }
            }
        }
        Log.d(TAG, "<getAllArchivePath> end");
        return arrayList;
    }
}
