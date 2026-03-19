package android.content.pm.split;

import android.content.pm.PackageParser;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.SparseArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import libcore.io.IoUtils;

public class SplitAssetDependencyLoader extends SplitDependencyLoader<PackageParser.PackageParserException> implements SplitAssetLoader {
    private final AssetManager[] mCachedAssetManagers;
    private final ApkAssets[][] mCachedSplitApks;
    private final int mFlags;
    private final String[] mSplitPaths;

    public SplitAssetDependencyLoader(PackageParser.PackageLite packageLite, SparseArray<int[]> sparseArray, int i) {
        super(sparseArray);
        this.mSplitPaths = new String[packageLite.splitCodePaths.length + 1];
        this.mSplitPaths[0] = packageLite.baseCodePath;
        System.arraycopy(packageLite.splitCodePaths, 0, this.mSplitPaths, 1, packageLite.splitCodePaths.length);
        this.mFlags = i;
        this.mCachedSplitApks = new ApkAssets[this.mSplitPaths.length][];
        this.mCachedAssetManagers = new AssetManager[this.mSplitPaths.length];
    }

    @Override
    protected boolean isSplitCached(int i) {
        return this.mCachedAssetManagers[i] != null;
    }

    private static ApkAssets loadApkAssets(String str, int i) throws PackageParser.PackageParserException {
        if ((i & 1) != 0 && !PackageParser.isApkPath(str)) {
            throw new PackageParser.PackageParserException(-100, "Invalid package file: " + str);
        }
        try {
            return ApkAssets.loadFromPath(str);
        } catch (IOException e) {
            throw new PackageParser.PackageParserException(-2, "Failed to load APK at path " + str, e);
        }
    }

    private static AssetManager createAssetManagerWithAssets(ApkAssets[] apkAssetsArr) {
        AssetManager assetManager = new AssetManager();
        assetManager.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
        assetManager.setApkAssets(apkAssetsArr, false);
        return assetManager;
    }

    @Override
    protected void constructSplit(int i, int[] iArr, int i2) throws PackageParser.PackageParserException {
        ArrayList arrayList = new ArrayList();
        if (i2 >= 0) {
            Collections.addAll(arrayList, this.mCachedSplitApks[i2]);
        }
        arrayList.add(loadApkAssets(this.mSplitPaths[i], this.mFlags));
        for (int i3 : iArr) {
            arrayList.add(loadApkAssets(this.mSplitPaths[i3], this.mFlags));
        }
        this.mCachedSplitApks[i] = (ApkAssets[]) arrayList.toArray(new ApkAssets[arrayList.size()]);
        this.mCachedAssetManagers[i] = createAssetManagerWithAssets(this.mCachedSplitApks[i]);
    }

    @Override
    public AssetManager getBaseAssetManager() throws PackageParser.PackageParserException {
        loadDependenciesForSplit(0);
        return this.mCachedAssetManagers[0];
    }

    @Override
    public AssetManager getSplitAssetManager(int i) throws PackageParser.PackageParserException {
        int i2 = i + 1;
        loadDependenciesForSplit(i2);
        return this.mCachedAssetManagers[i2];
    }

    @Override
    public void close() throws Exception {
        for (AssetManager assetManager : this.mCachedAssetManagers) {
            IoUtils.closeQuietly(assetManager);
        }
    }
}
