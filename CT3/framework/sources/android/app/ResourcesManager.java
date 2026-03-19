package android.app;

import android.content.res.AssetManager;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.ResourcesImpl;
import android.content.res.ResourcesKey;
import android.hardware.display.DisplayManagerGlobal;
import android.os.IBinder;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayAdjustments;
import com.android.internal.util.ArrayUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class ResourcesManager {
    private static final boolean DEBUG = false;
    static final String TAG = "ResourcesManager";
    private static final Predicate<WeakReference<Resources>> sEmptyReferencePredicate = new Predicate<WeakReference<Resources>>() {
        @Override
        public boolean test(WeakReference<Resources> weakRef) {
            return weakRef == null || weakRef.get() == null;
        }
    };
    private static ResourcesManager sResourcesManager;
    private CompatibilityInfo mResCompatibilityInfo;
    private final Configuration mResConfiguration = new Configuration();
    private final ArrayMap<ResourcesKey, WeakReference<ResourcesImpl>> mResourceImpls = new ArrayMap<>();
    private final ArrayList<WeakReference<Resources>> mResourceReferences = new ArrayList<>();
    private final WeakHashMap<IBinder, ActivityResources> mActivityResourceReferences = new WeakHashMap<>();
    private final ArrayMap<Pair<Integer, DisplayAdjustments>, WeakReference<Display>> mDisplays = new ArrayMap<>();

    private static class ActivityResources {
        public final ArrayList<WeakReference<Resources>> activityResources;
        public final Configuration overrideConfig;

        ActivityResources(ActivityResources activityResources) {
            this();
        }

        private ActivityResources() {
            this.overrideConfig = new Configuration();
            this.activityResources = new ArrayList<>();
        }
    }

    public static ResourcesManager getInstance() {
        ResourcesManager resourcesManager;
        synchronized (ResourcesManager.class) {
            if (sResourcesManager == null) {
                sResourcesManager = new ResourcesManager();
            }
            resourcesManager = sResourcesManager;
        }
        return resourcesManager;
    }

    public void invalidatePath(String path) {
        synchronized (this) {
            int count = 0;
            int i = 0;
            while (i < this.mResourceImpls.size()) {
                ResourcesKey key = this.mResourceImpls.keyAt(i);
                if (key.isPathReferenced(path)) {
                    ResourcesImpl res = this.mResourceImpls.removeAt(i).get();
                    if (res != null) {
                        res.flushLayoutCache();
                    }
                    count++;
                } else {
                    i++;
                }
            }
            Log.i(TAG, "Invalidated " + count + " asset managers that referenced " + path);
        }
    }

    public Configuration getConfiguration() {
        Configuration configuration;
        synchronized (this) {
            configuration = this.mResConfiguration;
        }
        return configuration;
    }

    DisplayMetrics getDisplayMetrics() {
        return getDisplayMetrics(0, DisplayAdjustments.DEFAULT_DISPLAY_ADJUSTMENTS);
    }

    protected DisplayMetrics getDisplayMetrics(int displayId, DisplayAdjustments da) {
        DisplayMetrics dm = new DisplayMetrics();
        Display display = getAdjustedDisplay(displayId, da);
        if (display != null) {
            display.getMetrics(dm);
        } else {
            dm.setToDefaults();
        }
        return dm;
    }

    private static void applyNonDefaultDisplayMetricsToConfiguration(DisplayMetrics dm, Configuration config) {
        config.touchscreen = 1;
        config.densityDpi = dm.densityDpi;
        config.screenWidthDp = (int) (dm.widthPixels / dm.density);
        config.screenHeightDp = (int) (dm.heightPixels / dm.density);
        int sl = Configuration.resetScreenLayout(config.screenLayout);
        if (dm.widthPixels > dm.heightPixels) {
            config.orientation = 2;
            config.screenLayout = Configuration.reduceScreenLayout(sl, config.screenWidthDp, config.screenHeightDp);
        } else {
            config.orientation = 1;
            config.screenLayout = Configuration.reduceScreenLayout(sl, config.screenHeightDp, config.screenWidthDp);
        }
        config.smallestScreenWidthDp = config.screenWidthDp;
        config.compatScreenWidthDp = config.screenWidthDp;
        config.compatScreenHeightDp = config.screenHeightDp;
        config.compatSmallestScreenWidthDp = config.smallestScreenWidthDp;
    }

    public boolean applyCompatConfigurationLocked(int displayDensity, Configuration compatConfiguration) {
        if (this.mResCompatibilityInfo != null && !this.mResCompatibilityInfo.supportsScreen()) {
            this.mResCompatibilityInfo.applyToConfiguration(displayDensity, compatConfiguration);
            return true;
        }
        return false;
    }

    public Display getAdjustedDisplay(int displayId, DisplayAdjustments displayAdjustments) {
        Display display;
        DisplayAdjustments displayAdjustmentsCopy = displayAdjustments != null ? new DisplayAdjustments(displayAdjustments) : new DisplayAdjustments();
        Pair<Integer, DisplayAdjustments> key = Pair.create(Integer.valueOf(displayId), displayAdjustmentsCopy);
        synchronized (this) {
            WeakReference<Display> wd = this.mDisplays.get(key);
            if (wd != null && (display = wd.get()) != null) {
                return display;
            }
            DisplayManagerGlobal dm = DisplayManagerGlobal.getInstance();
            if (dm == null) {
                return null;
            }
            Display display2 = dm.getCompatibleDisplay(displayId, (DisplayAdjustments) key.second);
            if (display2 != null) {
                this.mDisplays.put(key, new WeakReference<>(display2));
            }
            return display2;
        }
    }

    protected AssetManager createAssetManager(ResourcesKey key) {
        AssetManager assets = new AssetManager();
        if (key.mResDir != null && assets.addAssetPath(key.mResDir) == 0) {
            throw new Resources.NotFoundException("failed to add asset path " + key.mResDir);
        }
        if (key.mSplitResDirs != null) {
            for (String splitResDir : key.mSplitResDirs) {
                if (assets.addAssetPath(splitResDir) == 0) {
                    throw new Resources.NotFoundException("failed to add split asset path " + splitResDir);
                }
            }
        }
        if (key.mOverlayDirs != null) {
            for (String idmapPath : key.mOverlayDirs) {
                assets.addOverlayPath(idmapPath);
            }
        }
        if (key.mLibDirs != null) {
            for (String libDir : key.mLibDirs) {
                if (libDir.endsWith(".apk") && assets.addAssetPathAsSharedLibrary(libDir) == 0) {
                    Log.w(TAG, "Asset path '" + libDir + "' does not exist or contains no resources.");
                }
            }
        }
        return assets;
    }

    private Configuration generateConfig(ResourcesKey key, DisplayMetrics dm) {
        boolean isDefaultDisplay = key.mDisplayId == 0;
        boolean hasOverrideConfig = key.hasOverrideConfiguration();
        if (!isDefaultDisplay || hasOverrideConfig) {
            Configuration config = new Configuration(getConfiguration());
            if (!isDefaultDisplay) {
                applyNonDefaultDisplayMetricsToConfiguration(dm, config);
            }
            if (hasOverrideConfig) {
                config.updateFrom(key.mOverrideConfiguration);
                return config;
            }
            return config;
        }
        return getConfiguration();
    }

    private ResourcesImpl createResourcesImpl(ResourcesKey key) {
        DisplayAdjustments daj = new DisplayAdjustments(key.mOverrideConfiguration);
        daj.setCompatibilityInfo(key.mCompatInfo);
        AssetManager assets = createAssetManager(key);
        DisplayMetrics dm = getDisplayMetrics(key.mDisplayId, daj);
        Configuration config = generateConfig(key, dm);
        ResourcesImpl impl = new ResourcesImpl(assets, dm, config, daj);
        return impl;
    }

    private ResourcesImpl findResourcesImplForKeyLocked(ResourcesKey key) {
        WeakReference<ResourcesImpl> weakImplRef = this.mResourceImpls.get(key);
        ResourcesImpl impl = weakImplRef != null ? weakImplRef.get() : null;
        if (impl == null || !impl.getAssets().isUpToDate()) {
            return null;
        }
        return impl;
    }

    private ResourcesImpl findOrCreateResourcesImplForKeyLocked(ResourcesKey key) {
        ResourcesImpl impl = findResourcesImplForKeyLocked(key);
        if (impl == null) {
            ResourcesImpl impl2 = createResourcesImpl(key);
            this.mResourceImpls.put(key, new WeakReference<>(impl2));
            return impl2;
        }
        return impl;
    }

    private ResourcesKey findKeyForResourceImplLocked(ResourcesImpl resourceImpl) {
        int refCount = this.mResourceImpls.size();
        for (int i = 0; i < refCount; i++) {
            WeakReference<ResourcesImpl> weakImplRef = this.mResourceImpls.valueAt(i);
            ResourcesImpl impl = weakImplRef != null ? weakImplRef.get() : null;
            if (impl != null && resourceImpl == impl) {
                return this.mResourceImpls.keyAt(i);
            }
        }
        return null;
    }

    private ActivityResources getOrCreateActivityResourcesStructLocked(IBinder activityToken) {
        ActivityResources activityResources = null;
        ActivityResources activityResources2 = this.mActivityResourceReferences.get(activityToken);
        if (activityResources2 == null) {
            ActivityResources activityResources3 = new ActivityResources(activityResources);
            this.mActivityResourceReferences.put(activityToken, activityResources3);
            return activityResources3;
        }
        return activityResources2;
    }

    private Resources getOrCreateResourcesForActivityLocked(IBinder activityToken, ClassLoader classLoader, ResourcesImpl impl) {
        ActivityResources activityResources = getOrCreateActivityResourcesStructLocked(activityToken);
        int refCount = activityResources.activityResources.size();
        for (int i = 0; i < refCount; i++) {
            WeakReference<Resources> weakResourceRef = activityResources.activityResources.get(i);
            Resources resources = weakResourceRef.get();
            if (resources != null && Objects.equals(resources.getClassLoader(), classLoader) && resources.getImpl() == impl) {
                return resources;
            }
        }
        Resources resources2 = new Resources(classLoader);
        resources2.setImpl(impl);
        activityResources.activityResources.add(new WeakReference<>(resources2));
        return resources2;
    }

    private Resources getOrCreateResourcesLocked(ClassLoader classLoader, ResourcesImpl impl) {
        int refCount = this.mResourceReferences.size();
        for (int i = 0; i < refCount; i++) {
            WeakReference<Resources> weakResourceRef = this.mResourceReferences.get(i);
            Resources resources = weakResourceRef.get();
            if (resources != null && Objects.equals(resources.getClassLoader(), classLoader) && resources.getImpl() == impl) {
                return resources;
            }
        }
        Resources resources2 = new Resources(classLoader);
        resources2.setImpl(impl);
        this.mResourceReferences.add(new WeakReference<>(resources2));
        return resources2;
    }

    public Resources createBaseActivityResources(IBinder activityToken, String resDir, String[] splitResDirs, String[] overlayDirs, String[] libDirs, int displayId, Configuration overrideConfig, CompatibilityInfo compatInfo, ClassLoader classLoader) {
        try {
            Trace.traceBegin(32768L, "ResourcesManager#createBaseActivityResources");
            ResourcesKey key = new ResourcesKey(resDir, splitResDirs, overlayDirs, libDirs, displayId, overrideConfig != null ? new Configuration(overrideConfig) : null, compatInfo);
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            synchronized (this) {
                getOrCreateActivityResourcesStructLocked(activityToken);
            }
            updateResourcesForActivity(activityToken, overrideConfig);
            return getOrCreateResources(activityToken, key, classLoader);
        } finally {
            Trace.traceEnd(32768L);
        }
    }

    private Resources getOrCreateResources(IBinder activityToken, ResourcesKey key, ClassLoader classLoader) {
        Resources resources;
        synchronized (this) {
            if (activityToken != null) {
                ActivityResources activityResources = getOrCreateActivityResourcesStructLocked(activityToken);
                ArrayUtils.unstableRemoveIf(activityResources.activityResources, sEmptyReferencePredicate);
                if (key.hasOverrideConfiguration() && !activityResources.overrideConfig.equals(Configuration.EMPTY)) {
                    Configuration temp = new Configuration(activityResources.overrideConfig);
                    temp.updateFrom(key.mOverrideConfiguration);
                    key.mOverrideConfiguration.setTo(temp);
                }
                ResourcesImpl resourcesImpl = findResourcesImplForKeyLocked(key);
                if (resourcesImpl != null) {
                    return getOrCreateResourcesForActivityLocked(activityToken, classLoader, resourcesImpl);
                }
            } else {
                ArrayUtils.unstableRemoveIf(this.mResourceReferences, sEmptyReferencePredicate);
                ResourcesImpl resourcesImpl2 = findResourcesImplForKeyLocked(key);
                if (resourcesImpl2 != null) {
                    return getOrCreateResourcesLocked(classLoader, resourcesImpl2);
                }
            }
            ResourcesImpl resourcesImpl3 = createResourcesImpl(key);
            synchronized (this) {
                ResourcesImpl existingResourcesImpl = findResourcesImplForKeyLocked(key);
                if (existingResourcesImpl != null) {
                    resourcesImpl3.getAssets().close();
                    resourcesImpl3 = existingResourcesImpl;
                } else {
                    this.mResourceImpls.put(key, new WeakReference<>(resourcesImpl3));
                }
                if (activityToken != null) {
                    resources = getOrCreateResourcesForActivityLocked(activityToken, classLoader, resourcesImpl3);
                } else {
                    resources = getOrCreateResourcesLocked(classLoader, resourcesImpl3);
                }
            }
            return resources;
        }
    }

    public Resources getResources(IBinder activityToken, String resDir, String[] splitResDirs, String[] overlayDirs, String[] libDirs, int displayId, Configuration overrideConfig, CompatibilityInfo compatInfo, ClassLoader classLoader) {
        try {
            Trace.traceBegin(32768L, "ResourcesManager#getResources");
            ResourcesKey key = new ResourcesKey(resDir, splitResDirs, overlayDirs, libDirs, displayId, overrideConfig != null ? new Configuration(overrideConfig) : null, compatInfo);
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            return getOrCreateResources(activityToken, key, classLoader);
        } finally {
            Trace.traceEnd(32768L);
        }
    }

    public void updateResourcesForActivity(IBinder activityToken, Configuration overrideConfig) {
        try {
            Trace.traceBegin(32768L, "ResourcesManager#updateResourcesForActivity");
            synchronized (this) {
                ActivityResources activityResources = getOrCreateActivityResourcesStructLocked(activityToken);
                if (Objects.equals(activityResources.overrideConfig, overrideConfig)) {
                    return;
                }
                Configuration oldConfig = new Configuration(activityResources.overrideConfig);
                if (overrideConfig != null) {
                    activityResources.overrideConfig.setTo(overrideConfig);
                } else {
                    activityResources.overrideConfig.setToDefaults();
                }
                boolean activityHasOverrideConfig = !activityResources.overrideConfig.equals(Configuration.EMPTY);
                int refCount = activityResources.activityResources.size();
                for (int i = 0; i < refCount; i++) {
                    WeakReference<Resources> weakResRef = activityResources.activityResources.get(i);
                    Resources resources = weakResRef.get();
                    if (resources != null) {
                        ResourcesKey oldKey = findKeyForResourceImplLocked(resources.getImpl());
                        if (oldKey == null) {
                            Slog.e(TAG, "can't find ResourcesKey for resources impl=" + resources.getImpl());
                        } else {
                            Configuration rebasedOverrideConfig = new Configuration();
                            if (overrideConfig != null) {
                                rebasedOverrideConfig.setTo(overrideConfig);
                            }
                            if (activityHasOverrideConfig && oldKey.hasOverrideConfiguration()) {
                                Configuration overrideOverrideConfig = Configuration.generateDelta(oldConfig, oldKey.mOverrideConfiguration);
                                rebasedOverrideConfig.updateFrom(overrideOverrideConfig);
                            }
                            ResourcesKey newKey = new ResourcesKey(oldKey.mResDir, oldKey.mSplitResDirs, oldKey.mOverlayDirs, oldKey.mLibDirs, oldKey.mDisplayId, rebasedOverrideConfig, oldKey.mCompatInfo);
                            ResourcesImpl resourcesImpl = findResourcesImplForKeyLocked(newKey);
                            if (resourcesImpl == null) {
                                resourcesImpl = createResourcesImpl(newKey);
                                this.mResourceImpls.put(newKey, new WeakReference<>(resourcesImpl));
                            }
                            if (resourcesImpl != resources.getImpl()) {
                                resources.setImpl(resourcesImpl);
                            }
                        }
                    }
                }
            }
        } finally {
            Trace.traceEnd(32768L);
        }
    }

    public final boolean applyConfigurationToResourcesLocked(Configuration config, CompatibilityInfo compat) {
        try {
            Trace.traceBegin(32768L, "ResourcesManager#applyConfigurationToResourcesLocked");
            if (!this.mResConfiguration.isOtherSeqNewer(config) && compat == null) {
                if (ActivityThread.DEBUG_CONFIGURATION) {
                    Slog.v(TAG, "Skipping new config: curSeq=" + this.mResConfiguration.seq + ", newSeq=" + config.seq);
                }
                return false;
            }
            int changes = this.mResConfiguration.updateFrom(config);
            this.mDisplays.clear();
            DisplayMetrics defaultDisplayMetrics = getDisplayMetrics();
            if (compat != null && (this.mResCompatibilityInfo == null || !this.mResCompatibilityInfo.equals(compat))) {
                this.mResCompatibilityInfo = compat;
                changes |= 3328;
            }
            Resources.updateSystemConfiguration(config, defaultDisplayMetrics, compat);
            ApplicationPackageManager.configurationChanged();
            Configuration tmpConfig = null;
            for (int i = this.mResourceImpls.size() - 1; i >= 0; i--) {
                ResourcesKey key = this.mResourceImpls.keyAt(i);
                ResourcesImpl r = this.mResourceImpls.valueAt(i).get();
                if (r != null) {
                    if (ActivityThread.DEBUG_CONFIGURATION) {
                        Slog.v(TAG, "Changing resources " + r + " config to: " + config);
                    }
                    int displayId = key.mDisplayId;
                    boolean isDefaultDisplay = displayId == 0;
                    boolean hasOverrideConfiguration = key.hasOverrideConfiguration();
                    if (!isDefaultDisplay || hasOverrideConfiguration) {
                        if (tmpConfig == null) {
                            tmpConfig = new Configuration();
                        }
                        tmpConfig.setTo(config);
                        DisplayAdjustments daj = r.getDisplayAdjustments();
                        if (compat != null) {
                            DisplayAdjustments daj2 = new DisplayAdjustments(daj);
                            daj2.setCompatibilityInfo(compat);
                            daj = daj2;
                        }
                        DisplayMetrics dm = getDisplayMetrics(displayId, daj);
                        if (!isDefaultDisplay) {
                            applyNonDefaultDisplayMetricsToConfiguration(dm, tmpConfig);
                        }
                        if (hasOverrideConfiguration) {
                            tmpConfig.updateFrom(key.mOverrideConfiguration);
                        }
                        r.updateConfiguration(tmpConfig, dm, compat);
                    } else {
                        r.updateConfiguration(config, defaultDisplayMetrics, compat);
                    }
                } else {
                    this.mResourceImpls.removeAt(i);
                }
            }
            return changes != 0;
        } finally {
            Trace.traceEnd(32768L);
        }
    }

    public void appendLibAssetForMainAssetPath(String assetPath, String libAsset) {
        ResourcesKey key;
        ResourcesKey key2;
        synchronized (this) {
            ArrayMap<ResourcesImpl, ResourcesKey> updatedResourceKeys = new ArrayMap<>();
            int implCount = this.mResourceImpls.size();
            for (int i = 0; i < implCount; i++) {
                ResourcesImpl impl = this.mResourceImpls.valueAt(i).get();
                ResourcesKey key3 = this.mResourceImpls.keyAt(i);
                if (impl != null && key3.mResDir.equals(assetPath) && !ArrayUtils.contains(key3.mLibDirs, libAsset)) {
                    int newLibAssetCount = (key3.mLibDirs != null ? key3.mLibDirs.length : 0) + 1;
                    String[] newLibAssets = new String[newLibAssetCount];
                    if (key3.mLibDirs != null) {
                        System.arraycopy(key3.mLibDirs, 0, newLibAssets, 0, key3.mLibDirs.length);
                    }
                    newLibAssets[newLibAssetCount - 1] = libAsset;
                    updatedResourceKeys.put(impl, new ResourcesKey(key3.mResDir, key3.mSplitResDirs, key3.mOverlayDirs, newLibAssets, key3.mDisplayId, key3.mOverrideConfiguration, key3.mCompatInfo));
                }
            }
            if (updatedResourceKeys.isEmpty()) {
                return;
            }
            int resourcesCount = this.mResourceReferences.size();
            for (int i2 = 0; i2 < resourcesCount; i2++) {
                Resources r = this.mResourceReferences.get(i2).get();
                if (r != null && (key2 = updatedResourceKeys.get(r.getImpl())) != null) {
                    r.setImpl(findOrCreateResourcesImplForKeyLocked(key2));
                }
            }
            for (ActivityResources activityResources : this.mActivityResourceReferences.values()) {
                int resCount = activityResources.activityResources.size();
                for (int i3 = 0; i3 < resCount; i3++) {
                    Resources r2 = activityResources.activityResources.get(i3).get();
                    if (r2 != null && (key = updatedResourceKeys.get(r2.getImpl())) != null) {
                        r2.setImpl(findOrCreateResourcesImplForKeyLocked(key));
                    }
                }
            }
        }
    }
}
