package android.app;

import android.content.pm.PackageParser;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.content.res.CompatResources;
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
import android.util.LruCache;
import android.util.Pair;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayAdjustments;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Predicate;

public class ResourcesManager {
    private static final boolean DEBUG = false;
    static final String TAG = "ResourcesManager";
    private static final Predicate<WeakReference<Resources>> sEmptyReferencePredicate = new Predicate() {
        @Override
        public final boolean test(Object obj) {
            return ResourcesManager.lambda$static$0((WeakReference) obj);
        }
    };
    private static ResourcesManager sResourcesManager;
    private CompatibilityInfo mResCompatibilityInfo;
    private final Configuration mResConfiguration = new Configuration();
    private final ArrayMap<ResourcesKey, WeakReference<ResourcesImpl>> mResourceImpls = new ArrayMap<>();
    private final ArrayList<WeakReference<Resources>> mResourceReferences = new ArrayList<>();
    private final LruCache<ApkKey, ApkAssets> mLoadedApkAssets = new LruCache<>(3);
    private final ArrayMap<ApkKey, WeakReference<ApkAssets>> mCachedApkAssets = new ArrayMap<>();
    private final WeakHashMap<IBinder, ActivityResources> mActivityResourceReferences = new WeakHashMap<>();
    private final ArrayMap<Pair<Integer, DisplayAdjustments>, WeakReference<Display>> mAdjustedDisplays = new ArrayMap<>();

    static boolean lambda$static$0(WeakReference weakReference) {
        return weakReference == null || weakReference.get() == null;
    }

    private static class ApkKey {
        public final boolean overlay;
        public final String path;
        public final boolean sharedLib;

        ApkKey(String str, boolean z, boolean z2) {
            this.path = str;
            this.sharedLib = z;
            this.overlay = z2;
        }

        public int hashCode() {
            return (31 * (((this.path.hashCode() + 31) * 31) + Boolean.hashCode(this.sharedLib))) + Boolean.hashCode(this.overlay);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ApkKey)) {
                return false;
            }
            ApkKey apkKey = (ApkKey) obj;
            return this.path.equals(apkKey.path) && this.sharedLib == apkKey.sharedLib && this.overlay == apkKey.overlay;
        }
    }

    private static class ActivityResources {
        public final ArrayList<WeakReference<Resources>> activityResources;
        public final Configuration overrideConfig;

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

    public void invalidatePath(String str) {
        synchronized (this) {
            int i = 0;
            int i2 = 0;
            while (i < this.mResourceImpls.size()) {
                ResourcesKey resourcesKeyKeyAt = this.mResourceImpls.keyAt(i);
                if (resourcesKeyKeyAt.isPathReferenced(str)) {
                    cleanupResourceImpl(resourcesKeyKeyAt);
                    i2++;
                } else {
                    i++;
                }
            }
            Log.i(TAG, "Invalidated " + i2 + " asset managers that referenced " + str);
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

    @VisibleForTesting
    protected DisplayMetrics getDisplayMetrics(int i, DisplayAdjustments displayAdjustments) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        Display adjustedDisplay = getAdjustedDisplay(i, displayAdjustments);
        if (adjustedDisplay != null) {
            adjustedDisplay.getMetrics(displayMetrics);
        } else {
            displayMetrics.setToDefaults();
        }
        return displayMetrics;
    }

    private static void applyNonDefaultDisplayMetricsToConfiguration(DisplayMetrics displayMetrics, Configuration configuration) {
        configuration.touchscreen = 1;
        configuration.densityDpi = displayMetrics.densityDpi;
        configuration.screenWidthDp = (int) (displayMetrics.widthPixels / displayMetrics.density);
        configuration.screenHeightDp = (int) (displayMetrics.heightPixels / displayMetrics.density);
        int iResetScreenLayout = Configuration.resetScreenLayout(configuration.screenLayout);
        if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
            configuration.orientation = 2;
            configuration.screenLayout = Configuration.reduceScreenLayout(iResetScreenLayout, configuration.screenWidthDp, configuration.screenHeightDp);
        } else {
            configuration.orientation = 1;
            configuration.screenLayout = Configuration.reduceScreenLayout(iResetScreenLayout, configuration.screenHeightDp, configuration.screenWidthDp);
        }
        configuration.smallestScreenWidthDp = configuration.screenWidthDp;
        configuration.compatScreenWidthDp = configuration.screenWidthDp;
        configuration.compatScreenHeightDp = configuration.screenHeightDp;
        configuration.compatSmallestScreenWidthDp = configuration.smallestScreenWidthDp;
    }

    public boolean applyCompatConfigurationLocked(int i, Configuration configuration) {
        if (this.mResCompatibilityInfo != null && !this.mResCompatibilityInfo.supportsScreen()) {
            this.mResCompatibilityInfo.applyToConfiguration(i, configuration);
            return true;
        }
        return false;
    }

    private Display getAdjustedDisplay(int i, DisplayAdjustments displayAdjustments) {
        Display display;
        Pair<Integer, DisplayAdjustments> pairCreate = Pair.create(Integer.valueOf(i), displayAdjustments != null ? new DisplayAdjustments(displayAdjustments) : new DisplayAdjustments());
        synchronized (this) {
            WeakReference<Display> weakReference = this.mAdjustedDisplays.get(pairCreate);
            if (weakReference != null && (display = weakReference.get()) != null) {
                return display;
            }
            DisplayManagerGlobal displayManagerGlobal = DisplayManagerGlobal.getInstance();
            if (displayManagerGlobal == null) {
                return null;
            }
            Display compatibleDisplay = displayManagerGlobal.getCompatibleDisplay(i, pairCreate.second);
            if (compatibleDisplay != null) {
                this.mAdjustedDisplays.put(pairCreate, new WeakReference<>(compatibleDisplay));
            }
            return compatibleDisplay;
        }
    }

    public Display getAdjustedDisplay(int i, Resources resources) {
        synchronized (this) {
            DisplayManagerGlobal displayManagerGlobal = DisplayManagerGlobal.getInstance();
            if (displayManagerGlobal == null) {
                return null;
            }
            return displayManagerGlobal.getCompatibleDisplay(i, resources);
        }
    }

    private void cleanupResourceImpl(ResourcesKey resourcesKey) {
        ResourcesImpl resourcesImpl = this.mResourceImpls.remove(resourcesKey).get();
        if (resourcesImpl != null) {
            resourcesImpl.flushLayoutCache();
        }
    }

    private static String overlayPathToIdmapPath(String str) {
        return "/data/resource-cache/" + str.substring(1).replace('/', '@') + "@idmap";
    }

    private ApkAssets loadApkAssets(String str, boolean z, boolean z2) throws IOException {
        ApkAssets apkAssetsLoadFromPath;
        ApkKey apkKey = new ApkKey(str, z, z2);
        ApkAssets apkAssets = this.mLoadedApkAssets.get(apkKey);
        if (apkAssets != null) {
            return apkAssets;
        }
        WeakReference<ApkAssets> weakReference = this.mCachedApkAssets.get(apkKey);
        if (weakReference != null) {
            ApkAssets apkAssets2 = weakReference.get();
            if (apkAssets2 != null) {
                this.mLoadedApkAssets.put(apkKey, apkAssets2);
                return apkAssets2;
            }
            this.mCachedApkAssets.remove(apkKey);
        }
        if (z2) {
            apkAssetsLoadFromPath = ApkAssets.loadOverlayFromPath(overlayPathToIdmapPath(str), false);
        } else {
            apkAssetsLoadFromPath = ApkAssets.loadFromPath(str, false, z);
        }
        this.mLoadedApkAssets.put(apkKey, apkAssetsLoadFromPath);
        this.mCachedApkAssets.put(apkKey, new WeakReference<>(apkAssetsLoadFromPath));
        return apkAssetsLoadFromPath;
    }

    @VisibleForTesting
    protected AssetManager createAssetManager(ResourcesKey resourcesKey) {
        AssetManager.Builder builder = new AssetManager.Builder();
        if (resourcesKey.mResDir != null) {
            try {
                builder.addApkAssets(loadApkAssets(resourcesKey.mResDir, false, false));
            } catch (IOException e) {
                Log.e(TAG, "failed to add asset path " + resourcesKey.mResDir);
                return null;
            }
        }
        if (resourcesKey.mSplitResDirs != null) {
            for (String str : resourcesKey.mSplitResDirs) {
                try {
                    builder.addApkAssets(loadApkAssets(str, false, false));
                } catch (IOException e2) {
                    Log.e(TAG, "failed to add split asset path " + str);
                    return null;
                }
            }
        }
        if (resourcesKey.mOverlayDirs != null) {
            for (String str2 : resourcesKey.mOverlayDirs) {
                try {
                    builder.addApkAssets(loadApkAssets(str2, false, true));
                } catch (IOException e3) {
                    Log.w(TAG, "failed to add overlay path " + str2);
                }
            }
        }
        if (resourcesKey.mLibDirs != null) {
            for (String str3 : resourcesKey.mLibDirs) {
                if (str3.endsWith(PackageParser.APK_FILE_EXTENSION)) {
                    try {
                        builder.addApkAssets(loadApkAssets(str3, true, false));
                    } catch (IOException e4) {
                        Log.w(TAG, "Asset path '" + str3 + "' does not exist or contains no resources.");
                    }
                }
            }
        }
        return builder.build();
    }

    private static <T> int countLiveReferences(Collection<WeakReference<T>> collection) {
        Iterator<WeakReference<T>> it = collection.iterator();
        int i = 0;
        while (it.hasNext()) {
            WeakReference<T> next = it.next();
            if ((next != null ? next.get() : null) != null) {
                i++;
            }
        }
        return i;
    }

    public void dump(String str, PrintWriter printWriter) {
        synchronized (this) {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            for (int i = 0; i < str.length() / 2; i++) {
                indentingPrintWriter.increaseIndent();
            }
            indentingPrintWriter.println("ResourcesManager:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.print("cached apks: total=");
            indentingPrintWriter.print(this.mLoadedApkAssets.size());
            indentingPrintWriter.print(" created=");
            indentingPrintWriter.print(this.mLoadedApkAssets.createCount());
            indentingPrintWriter.print(" evicted=");
            indentingPrintWriter.print(this.mLoadedApkAssets.evictionCount());
            indentingPrintWriter.print(" hit=");
            indentingPrintWriter.print(this.mLoadedApkAssets.hitCount());
            indentingPrintWriter.print(" miss=");
            indentingPrintWriter.print(this.mLoadedApkAssets.missCount());
            indentingPrintWriter.print(" max=");
            indentingPrintWriter.print(this.mLoadedApkAssets.maxSize());
            indentingPrintWriter.println();
            indentingPrintWriter.print("total apks: ");
            indentingPrintWriter.println(countLiveReferences(this.mCachedApkAssets.values()));
            indentingPrintWriter.print("resources: ");
            int iCountLiveReferences = countLiveReferences(this.mResourceReferences);
            Iterator<ActivityResources> it = this.mActivityResourceReferences.values().iterator();
            while (it.hasNext()) {
                iCountLiveReferences += countLiveReferences(it.next().activityResources);
            }
            indentingPrintWriter.println(iCountLiveReferences);
            indentingPrintWriter.print("resource impls: ");
            indentingPrintWriter.println(countLiveReferences(this.mResourceImpls.values()));
        }
    }

    private Configuration generateConfig(ResourcesKey resourcesKey, DisplayMetrics displayMetrics) {
        boolean z = resourcesKey.mDisplayId == 0;
        boolean zHasOverrideConfiguration = resourcesKey.hasOverrideConfiguration();
        if (!z || zHasOverrideConfiguration) {
            Configuration configuration = new Configuration(getConfiguration());
            if (!z) {
                applyNonDefaultDisplayMetricsToConfiguration(displayMetrics, configuration);
            }
            if (zHasOverrideConfiguration) {
                configuration.updateFrom(resourcesKey.mOverrideConfiguration);
            }
            return configuration;
        }
        return getConfiguration();
    }

    private ResourcesImpl createResourcesImpl(ResourcesKey resourcesKey) {
        DisplayAdjustments displayAdjustments = new DisplayAdjustments(resourcesKey.mOverrideConfiguration);
        displayAdjustments.setCompatibilityInfo(resourcesKey.mCompatInfo);
        AssetManager assetManagerCreateAssetManager = createAssetManager(resourcesKey);
        if (assetManagerCreateAssetManager == null) {
            return null;
        }
        DisplayMetrics displayMetrics = getDisplayMetrics(resourcesKey.mDisplayId, displayAdjustments);
        return new ResourcesImpl(assetManagerCreateAssetManager, displayMetrics, generateConfig(resourcesKey, displayMetrics), displayAdjustments);
    }

    private ResourcesImpl findResourcesImplForKeyLocked(ResourcesKey resourcesKey) {
        WeakReference<ResourcesImpl> weakReference = this.mResourceImpls.get(resourcesKey);
        ResourcesImpl resourcesImpl = weakReference != null ? weakReference.get() : null;
        if (resourcesImpl == null || !resourcesImpl.getAssets().isUpToDate()) {
            return null;
        }
        return resourcesImpl;
    }

    private ResourcesImpl findOrCreateResourcesImplForKeyLocked(ResourcesKey resourcesKey) {
        ResourcesImpl resourcesImplFindResourcesImplForKeyLocked = findResourcesImplForKeyLocked(resourcesKey);
        if (resourcesImplFindResourcesImplForKeyLocked == null && (resourcesImplFindResourcesImplForKeyLocked = createResourcesImpl(resourcesKey)) != null) {
            this.mResourceImpls.put(resourcesKey, new WeakReference<>(resourcesImplFindResourcesImplForKeyLocked));
        }
        return resourcesImplFindResourcesImplForKeyLocked;
    }

    private ResourcesKey findKeyForResourceImplLocked(ResourcesImpl resourcesImpl) {
        int size = this.mResourceImpls.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                return null;
            }
            WeakReference<ResourcesImpl> weakReferenceValueAt = this.mResourceImpls.valueAt(i);
            ResourcesImpl resourcesImpl2 = weakReferenceValueAt != null ? weakReferenceValueAt.get() : null;
            if (resourcesImpl2 == null || resourcesImpl != resourcesImpl2) {
                i++;
            } else {
                return this.mResourceImpls.keyAt(i);
            }
        }
    }

    boolean isSameResourcesOverrideConfig(IBinder iBinder, Configuration configuration) {
        ActivityResources activityResources;
        synchronized (this) {
            if (iBinder == null) {
                activityResources = null;
            } else {
                try {
                    activityResources = this.mActivityResourceReferences.get(iBinder);
                } finally {
                }
            }
            if (activityResources == null) {
                return configuration == null;
            }
            if (Objects.equals(activityResources.overrideConfig, configuration) || (configuration != null && activityResources.overrideConfig != null && configuration.diffPublicOnly(activityResources.overrideConfig) == 0)) {
                z = true;
            }
            return z;
        }
    }

    private ActivityResources getOrCreateActivityResourcesStructLocked(IBinder iBinder) {
        ActivityResources activityResources = this.mActivityResourceReferences.get(iBinder);
        if (activityResources == null) {
            ActivityResources activityResources2 = new ActivityResources();
            this.mActivityResourceReferences.put(iBinder, activityResources2);
            return activityResources2;
        }
        return activityResources;
    }

    private Resources getOrCreateResourcesForActivityLocked(IBinder iBinder, ClassLoader classLoader, ResourcesImpl resourcesImpl, CompatibilityInfo compatibilityInfo) {
        ActivityResources orCreateActivityResourcesStructLocked = getOrCreateActivityResourcesStructLocked(iBinder);
        int size = orCreateActivityResourcesStructLocked.activityResources.size();
        for (int i = 0; i < size; i++) {
            Resources resources = orCreateActivityResourcesStructLocked.activityResources.get(i).get();
            if (resources != null && Objects.equals(resources.getClassLoader(), classLoader) && resources.getImpl() == resourcesImpl) {
                return resources;
            }
        }
        Resources compatResources = compatibilityInfo.needsCompatResources() ? new CompatResources(classLoader) : new Resources(classLoader);
        compatResources.setImpl(resourcesImpl);
        orCreateActivityResourcesStructLocked.activityResources.add(new WeakReference<>(compatResources));
        return compatResources;
    }

    private Resources getOrCreateResourcesLocked(ClassLoader classLoader, ResourcesImpl resourcesImpl, CompatibilityInfo compatibilityInfo) {
        int size = this.mResourceReferences.size();
        for (int i = 0; i < size; i++) {
            Resources resources = this.mResourceReferences.get(i).get();
            if (resources != null && Objects.equals(resources.getClassLoader(), classLoader) && resources.getImpl() == resourcesImpl) {
                return resources;
            }
        }
        Resources compatResources = compatibilityInfo.needsCompatResources() ? new CompatResources(classLoader) : new Resources(classLoader);
        compatResources.setImpl(resourcesImpl);
        this.mResourceReferences.add(new WeakReference<>(compatResources));
        return compatResources;
    }

    public Resources createBaseActivityResources(IBinder iBinder, String str, String[] strArr, String[] strArr2, String[] strArr3, int i, Configuration configuration, CompatibilityInfo compatibilityInfo, ClassLoader classLoader) {
        ClassLoader systemClassLoader;
        try {
            Trace.traceBegin(8192L, "ResourcesManager#createBaseActivityResources");
            ResourcesKey resourcesKey = new ResourcesKey(str, strArr, strArr2, strArr3, i, configuration != null ? new Configuration(configuration) : null, compatibilityInfo);
            if (classLoader == null) {
                systemClassLoader = ClassLoader.getSystemClassLoader();
            } else {
                systemClassLoader = classLoader;
            }
            synchronized (this) {
                getOrCreateActivityResourcesStructLocked(iBinder);
            }
            updateResourcesForActivity(iBinder, configuration, i, false);
            return getOrCreateResources(iBinder, resourcesKey, systemClassLoader);
        } finally {
            Trace.traceEnd(8192L);
        }
    }

    private Resources getOrCreateResources(IBinder iBinder, ResourcesKey resourcesKey, ClassLoader classLoader) {
        Resources orCreateResourcesLocked;
        synchronized (this) {
            try {
                if (iBinder != null) {
                    ActivityResources orCreateActivityResourcesStructLocked = getOrCreateActivityResourcesStructLocked(iBinder);
                    ArrayUtils.unstableRemoveIf(orCreateActivityResourcesStructLocked.activityResources, sEmptyReferencePredicate);
                    if (resourcesKey.hasOverrideConfiguration() && !orCreateActivityResourcesStructLocked.overrideConfig.equals(Configuration.EMPTY)) {
                        Configuration configuration = new Configuration(orCreateActivityResourcesStructLocked.overrideConfig);
                        configuration.updateFrom(resourcesKey.mOverrideConfiguration);
                        resourcesKey.mOverrideConfiguration.setTo(configuration);
                    }
                    ResourcesImpl resourcesImplFindResourcesImplForKeyLocked = findResourcesImplForKeyLocked(resourcesKey);
                    if (resourcesImplFindResourcesImplForKeyLocked != null) {
                        return getOrCreateResourcesForActivityLocked(iBinder, classLoader, resourcesImplFindResourcesImplForKeyLocked, resourcesKey.mCompatInfo);
                    }
                } else {
                    ArrayUtils.unstableRemoveIf(this.mResourceReferences, sEmptyReferencePredicate);
                    ResourcesImpl resourcesImplFindResourcesImplForKeyLocked2 = findResourcesImplForKeyLocked(resourcesKey);
                    if (resourcesImplFindResourcesImplForKeyLocked2 != null) {
                        return getOrCreateResourcesLocked(classLoader, resourcesImplFindResourcesImplForKeyLocked2, resourcesKey.mCompatInfo);
                    }
                }
                ResourcesImpl resourcesImplCreateResourcesImpl = createResourcesImpl(resourcesKey);
                if (resourcesImplCreateResourcesImpl == null) {
                    return null;
                }
                this.mResourceImpls.put(resourcesKey, new WeakReference<>(resourcesImplCreateResourcesImpl));
                if (iBinder != null) {
                    orCreateResourcesLocked = getOrCreateResourcesForActivityLocked(iBinder, classLoader, resourcesImplCreateResourcesImpl, resourcesKey.mCompatInfo);
                } else {
                    orCreateResourcesLocked = getOrCreateResourcesLocked(classLoader, resourcesImplCreateResourcesImpl, resourcesKey.mCompatInfo);
                }
                return orCreateResourcesLocked;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public Resources getResources(IBinder iBinder, String str, String[] strArr, String[] strArr2, String[] strArr3, int i, Configuration configuration, CompatibilityInfo compatibilityInfo, ClassLoader classLoader) {
        ClassLoader systemClassLoader;
        try {
            Trace.traceBegin(8192L, "ResourcesManager#getResources");
            ResourcesKey resourcesKey = new ResourcesKey(str, strArr, strArr2, strArr3, i, configuration != null ? new Configuration(configuration) : null, compatibilityInfo);
            if (classLoader == null) {
                systemClassLoader = ClassLoader.getSystemClassLoader();
            } else {
                systemClassLoader = classLoader;
            }
            return getOrCreateResources(iBinder, resourcesKey, systemClassLoader);
        } finally {
            Trace.traceEnd(8192L);
        }
    }

    public void updateResourcesForActivity(IBinder iBinder, Configuration configuration, int i, boolean z) {
        Configuration configuration2 = configuration;
        try {
            Trace.traceBegin(8192L, "ResourcesManager#updateResourcesForActivity");
            synchronized (this) {
                ActivityResources orCreateActivityResourcesStructLocked = getOrCreateActivityResourcesStructLocked(iBinder);
                if (!Objects.equals(orCreateActivityResourcesStructLocked.overrideConfig, configuration2) || z) {
                    Configuration configuration3 = new Configuration(orCreateActivityResourcesStructLocked.overrideConfig);
                    if (configuration2 != null) {
                        orCreateActivityResourcesStructLocked.overrideConfig.setTo(configuration2);
                    } else {
                        orCreateActivityResourcesStructLocked.overrideConfig.unset();
                    }
                    boolean z2 = !orCreateActivityResourcesStructLocked.overrideConfig.equals(Configuration.EMPTY);
                    int size = orCreateActivityResourcesStructLocked.activityResources.size();
                    int i2 = 0;
                    while (i2 < size) {
                        Resources resources = orCreateActivityResourcesStructLocked.activityResources.get(i2).get();
                        if (resources != null) {
                            ResourcesKey resourcesKeyFindKeyForResourceImplLocked = findKeyForResourceImplLocked(resources.getImpl());
                            if (resourcesKeyFindKeyForResourceImplLocked == null) {
                                Slog.e(TAG, "can't find ResourcesKey for resources impl=" + resources.getImpl());
                            } else {
                                Configuration configuration4 = new Configuration();
                                if (configuration2 != null) {
                                    configuration4.setTo(configuration2);
                                }
                                if (z2 && resourcesKeyFindKeyForResourceImplLocked.hasOverrideConfiguration()) {
                                    configuration4.updateFrom(Configuration.generateDelta(configuration3, resourcesKeyFindKeyForResourceImplLocked.mOverrideConfiguration));
                                }
                                ResourcesKey resourcesKey = new ResourcesKey(resourcesKeyFindKeyForResourceImplLocked.mResDir, resourcesKeyFindKeyForResourceImplLocked.mSplitResDirs, resourcesKeyFindKeyForResourceImplLocked.mOverlayDirs, resourcesKeyFindKeyForResourceImplLocked.mLibDirs, i, configuration4, resourcesKeyFindKeyForResourceImplLocked.mCompatInfo);
                                ResourcesImpl resourcesImplFindResourcesImplForKeyLocked = findResourcesImplForKeyLocked(resourcesKey);
                                if (resourcesImplFindResourcesImplForKeyLocked == null && (resourcesImplFindResourcesImplForKeyLocked = createResourcesImpl(resourcesKey)) != null) {
                                    this.mResourceImpls.put(resourcesKey, new WeakReference<>(resourcesImplFindResourcesImplForKeyLocked));
                                }
                                if (resourcesImplFindResourcesImplForKeyLocked != null && resourcesImplFindResourcesImplForKeyLocked != resources.getImpl()) {
                                    resources.setImpl(resourcesImplFindResourcesImplForKeyLocked);
                                }
                            }
                        }
                        i2++;
                        configuration2 = configuration;
                    }
                    return;
                }
                Trace.traceEnd(8192L);
            }
        } finally {
            Trace.traceEnd(8192L);
        }
    }

    public final boolean applyConfigurationToResourcesLocked(Configuration configuration, CompatibilityInfo compatibilityInfo) {
        try {
            Trace.traceBegin(8192L, "ResourcesManager#applyConfigurationToResourcesLocked");
            if (!this.mResConfiguration.isOtherSeqNewer(configuration) && compatibilityInfo == null) {
                if (ActivityThread.DEBUG_CONFIGURATION) {
                    Slog.v(TAG, "Skipping new config: curSeq=" + this.mResConfiguration.seq + ", newSeq=" + configuration.seq);
                }
                return false;
            }
            int iUpdateFrom = this.mResConfiguration.updateFrom(configuration);
            this.mAdjustedDisplays.clear();
            DisplayMetrics displayMetrics = getDisplayMetrics();
            if (compatibilityInfo != null && (this.mResCompatibilityInfo == null || !this.mResCompatibilityInfo.equals(compatibilityInfo))) {
                this.mResCompatibilityInfo = compatibilityInfo;
                iUpdateFrom |= 3328;
            }
            Resources.updateSystemConfiguration(configuration, displayMetrics, compatibilityInfo);
            ApplicationPackageManager.configurationChanged();
            boolean z = true;
            int size = this.mResourceImpls.size() - 1;
            Configuration configuration2 = null;
            while (size >= 0) {
                ResourcesKey resourcesKeyKeyAt = this.mResourceImpls.keyAt(size);
                WeakReference<ResourcesImpl> weakReferenceValueAt = this.mResourceImpls.valueAt(size);
                ResourcesImpl resourcesImpl = weakReferenceValueAt != null ? weakReferenceValueAt.get() : null;
                if (resourcesImpl != null) {
                    if (ActivityThread.DEBUG_CONFIGURATION) {
                        Slog.v(TAG, "Changing resources " + resourcesImpl + " config to: " + configuration);
                    }
                    int i = resourcesKeyKeyAt.mDisplayId;
                    boolean z2 = i == 0 ? z : false;
                    boolean zHasOverrideConfiguration = resourcesKeyKeyAt.hasOverrideConfiguration();
                    if (!z2 || zHasOverrideConfiguration) {
                        if (configuration2 == null) {
                            configuration2 = new Configuration();
                        }
                        configuration2.setTo(configuration);
                        DisplayAdjustments displayAdjustments = resourcesImpl.getDisplayAdjustments();
                        if (compatibilityInfo != null) {
                            DisplayAdjustments displayAdjustments2 = new DisplayAdjustments(displayAdjustments);
                            displayAdjustments2.setCompatibilityInfo(compatibilityInfo);
                            displayAdjustments = displayAdjustments2;
                        }
                        DisplayMetrics displayMetrics2 = getDisplayMetrics(i, displayAdjustments);
                        if (!z2) {
                            applyNonDefaultDisplayMetricsToConfiguration(displayMetrics2, configuration2);
                        }
                        if (zHasOverrideConfiguration) {
                            configuration2.updateFrom(resourcesKeyKeyAt.mOverrideConfiguration);
                        }
                        resourcesImpl.updateConfiguration(configuration2, displayMetrics2, compatibilityInfo);
                    } else {
                        resourcesImpl.updateConfiguration(configuration, displayMetrics, compatibilityInfo);
                    }
                } else {
                    this.mResourceImpls.removeAt(size);
                }
                size--;
                z = true;
            }
            return iUpdateFrom != 0;
        } finally {
            Trace.traceEnd(8192L);
        }
    }

    public void appendLibAssetForMainAssetPath(String str, String str2) {
        synchronized (this) {
            ArrayMap<ResourcesImpl, ResourcesKey> arrayMap = new ArrayMap<>();
            int size = this.mResourceImpls.size();
            for (int i = 0; i < size; i++) {
                ResourcesKey resourcesKeyKeyAt = this.mResourceImpls.keyAt(i);
                WeakReference<ResourcesImpl> weakReferenceValueAt = this.mResourceImpls.valueAt(i);
                ResourcesImpl resourcesImpl = weakReferenceValueAt != null ? weakReferenceValueAt.get() : null;
                if (resourcesImpl != null && Objects.equals(resourcesKeyKeyAt.mResDir, str) && !ArrayUtils.contains(resourcesKeyKeyAt.mLibDirs, str2)) {
                    int length = 1 + (resourcesKeyKeyAt.mLibDirs != null ? resourcesKeyKeyAt.mLibDirs.length : 0);
                    String[] strArr = new String[length];
                    if (resourcesKeyKeyAt.mLibDirs != null) {
                        System.arraycopy(resourcesKeyKeyAt.mLibDirs, 0, strArr, 0, resourcesKeyKeyAt.mLibDirs.length);
                    }
                    strArr[length - 1] = str2;
                    arrayMap.put(resourcesImpl, new ResourcesKey(resourcesKeyKeyAt.mResDir, resourcesKeyKeyAt.mSplitResDirs, resourcesKeyKeyAt.mOverlayDirs, strArr, resourcesKeyKeyAt.mDisplayId, resourcesKeyKeyAt.mOverrideConfiguration, resourcesKeyKeyAt.mCompatInfo));
                }
            }
            redirectResourcesToNewImplLocked(arrayMap);
        }
    }

    final void applyNewResourceDirsLocked(String str, String[] strArr) {
        try {
            Trace.traceBegin(8192L, "ResourcesManager#applyNewResourceDirsLocked");
            ArrayMap<ResourcesImpl, ResourcesKey> arrayMap = new ArrayMap<>();
            int size = this.mResourceImpls.size();
            for (int i = 0; i < size; i++) {
                ResourcesKey resourcesKeyKeyAt = this.mResourceImpls.keyAt(i);
                WeakReference<ResourcesImpl> weakReferenceValueAt = this.mResourceImpls.valueAt(i);
                ResourcesImpl resourcesImpl = weakReferenceValueAt != null ? weakReferenceValueAt.get() : null;
                if (resourcesImpl != null && (resourcesKeyKeyAt.mResDir == null || resourcesKeyKeyAt.mResDir.equals(str))) {
                    arrayMap.put(resourcesImpl, new ResourcesKey(resourcesKeyKeyAt.mResDir, resourcesKeyKeyAt.mSplitResDirs, strArr, resourcesKeyKeyAt.mLibDirs, resourcesKeyKeyAt.mDisplayId, resourcesKeyKeyAt.mOverrideConfiguration, resourcesKeyKeyAt.mCompatInfo));
                }
            }
            redirectResourcesToNewImplLocked(arrayMap);
        } finally {
            Trace.traceEnd(8192L);
        }
    }

    private void redirectResourcesToNewImplLocked(ArrayMap<ResourcesImpl, ResourcesKey> arrayMap) {
        ResourcesKey resourcesKey;
        ResourcesKey resourcesKey2;
        if (arrayMap.isEmpty()) {
            return;
        }
        int size = this.mResourceReferences.size();
        int i = 0;
        while (true) {
            if (i < size) {
                WeakReference<Resources> weakReference = this.mResourceReferences.get(i);
                Resources resources = weakReference != null ? weakReference.get() : null;
                if (resources != null && (resourcesKey2 = arrayMap.get(resources.getImpl())) != null) {
                    ResourcesImpl resourcesImplFindOrCreateResourcesImplForKeyLocked = findOrCreateResourcesImplForKeyLocked(resourcesKey2);
                    if (resourcesImplFindOrCreateResourcesImplForKeyLocked == null) {
                        throw new Resources.NotFoundException("failed to redirect ResourcesImpl");
                    }
                    resources.setImpl(resourcesImplFindOrCreateResourcesImplForKeyLocked);
                }
                i++;
            } else {
                for (ActivityResources activityResources : this.mActivityResourceReferences.values()) {
                    int size2 = activityResources.activityResources.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        WeakReference<Resources> weakReference2 = activityResources.activityResources.get(i2);
                        Resources resources2 = weakReference2 != null ? weakReference2.get() : null;
                        if (resources2 != null && (resourcesKey = arrayMap.get(resources2.getImpl())) != null) {
                            ResourcesImpl resourcesImplFindOrCreateResourcesImplForKeyLocked2 = findOrCreateResourcesImplForKeyLocked(resourcesKey);
                            if (resourcesImplFindOrCreateResourcesImplForKeyLocked2 == null) {
                                throw new Resources.NotFoundException("failed to redirect ResourcesImpl");
                            }
                            resources2.setImpl(resourcesImplFindOrCreateResourcesImplForKeyLocked2);
                        }
                    }
                }
                return;
            }
        }
    }
}
