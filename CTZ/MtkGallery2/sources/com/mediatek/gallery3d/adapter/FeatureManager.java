package com.mediatek.gallery3d.adapter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.android.gallery3d.common.Utils;
import com.mediatek.gallerybasic.base.IActivityCallback;
import com.mediatek.gallerybasic.base.IDecodeOptionsProcessor;
import com.mediatek.gallerybasic.base.IFieldDefinition;
import com.mediatek.gallerybasic.base.IFilter;
import com.mediatek.gallerybasic.base.MediaMember;
import com.mediatek.plugin.PluginManager;
import com.mediatek.plugin.PluginUtility;
import com.mediatek.plugin.element.Extension;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

public class FeatureManager {
    private static String PLUGIN_DIR_PATH2 = Environment.getExternalStorageDirectory().getPath() + "/gallery_plugin";
    private static FeatureManager sFeatureManager;
    private Context mContext;
    private PluginManager mPluginManager;
    private boolean mLoadingFinished = false;
    private Object mWaitLoading = new Object();

    private FeatureManager(Context context) {
        this.mContext = context;
    }

    public static synchronized void setup(Context context) {
        if (sFeatureManager == null) {
            Log.d("MtkGallery2/FeatureManager", "<setup> new FeatureManager, initialize");
            sFeatureManager = new FeatureManager(context);
            sFeatureManager.initialize();
        } else {
            Log.d("MtkGallery2/FeatureManager", "<setup> Has been set up, do nothing, return");
        }
    }

    public static FeatureManager getInstance() {
        return sFeatureManager;
    }

    private void initialize() {
        this.mPluginManager = PluginManager.getInstance(this.mContext);
        this.mPluginManager.addPluginDir("/system/app/MtkGallery2/galleryfeature");
        this.mPluginManager.addPluginDir(PLUGIN_DIR_PATH2);
        Log.d("MtkGallery2/FeatureManager", "<initialize> preloadAllPlugins");
        this.mPluginManager.preloadAllPlugins(true, false, true, new PluginManager.PreloaderListener() {
            @Override
            public void onPreloadFinished() {
                FeatureManager.this.mLoadingFinished = true;
                FeatureManager.this.preloadImplements();
                synchronized (FeatureManager.this.mWaitLoading) {
                    FeatureManager.this.mWaitLoading.notifyAll();
                }
            }
        });
    }

    private void preloadImplements() {
        getInstance().getImplement(IFilter.class, new Object[0]);
        getInstance().getImplement(IActivityCallback.class, new Object[0]);
        getInstance().getImplement(IFieldDefinition.class, new Object[0]);
        getInstance().getImplement(MediaMember.class, this.mContext, null, this.mContext.getResources());
        getInstance().getImplement(IDecodeOptionsProcessor.class, new Object[0]);
    }

    public <T> T[] getImplement(Class<?> cls, Object... objArr) {
        waitToPreloadFinished();
        Map<String, Extension> ext = PluginUtility.getExt(this.mPluginManager, cls);
        if (ext == null || ext.size() == 0) {
            return (T[]) ((Object[]) Array.newInstance(cls, 0));
        }
        T[] tArr = (T[]) ((Object[]) Array.newInstance(cls, ext.size()));
        Iterator<Map.Entry<String, Extension>> it = ext.entrySet().iterator();
        int i = 0;
        int i2 = 0;
        while (it.hasNext()) {
            Object objCreateInstance = PluginUtility.createInstance(this.mPluginManager, it.next().getValue(), objArr);
            int i3 = i + 1;
            tArr[i] = objCreateInstance;
            if (objCreateInstance == null) {
                i2++;
            }
            i = i3;
        }
        if (i2 == 0) {
            return tArr;
        }
        T[] tArr2 = (T[]) ((Object[]) Array.newInstance(cls, tArr.length - i2));
        int i4 = 0;
        for (Object[] objArr2 : tArr) {
            if (objArr2 != 0) {
                tArr2[i4] = objArr2;
                i4++;
            }
        }
        return tArr2;
    }

    private void waitToPreloadFinished() {
        while (!this.mLoadingFinished) {
            synchronized (this.mWaitLoading) {
                Log.d("MtkGallery2/FeatureManager", "<waitToPreloadFinished> wait...");
                Utils.waitWithoutInterrupt(this.mWaitLoading);
                Log.d("MtkGallery2/FeatureManager", "<waitToPreloadFinished> preload finished");
            }
        }
    }
}
