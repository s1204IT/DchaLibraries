package android.content.pm.split;

import android.content.pm.PackageParser;
import android.content.res.ApkAssets;
import android.content.res.AssetManager;
import android.os.Build;
import com.android.internal.util.ArrayUtils;
import java.io.IOException;
import libcore.io.IoUtils;

public class DefaultSplitAssetLoader implements SplitAssetLoader {
    private final String mBaseCodePath;
    private AssetManager mCachedAssetManager;
    private final int mFlags;
    private final String[] mSplitCodePaths;

    public DefaultSplitAssetLoader(PackageParser.PackageLite packageLite, int i) {
        this.mBaseCodePath = packageLite.baseCodePath;
        this.mSplitCodePaths = packageLite.splitCodePaths;
        this.mFlags = i;
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

    @Override
    public AssetManager getBaseAssetManager() throws PackageParser.PackageParserException {
        if (this.mCachedAssetManager == null) {
            ApkAssets[] apkAssetsArr = new ApkAssets[(this.mSplitCodePaths != null ? this.mSplitCodePaths.length : 0) + 1];
            apkAssetsArr[0] = loadApkAssets(this.mBaseCodePath, this.mFlags);
            if (!ArrayUtils.isEmpty(this.mSplitCodePaths)) {
                String[] strArr = this.mSplitCodePaths;
                int length = strArr.length;
                int i = 1;
                int i2 = 0;
                while (i2 < length) {
                    apkAssetsArr[i] = loadApkAssets(strArr[i2], this.mFlags);
                    i2++;
                    i++;
                }
            }
            AssetManager assetManager = new AssetManager();
            assetManager.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
            assetManager.setApkAssets(apkAssetsArr, false);
            this.mCachedAssetManager = assetManager;
            return this.mCachedAssetManager;
        }
        return this.mCachedAssetManager;
    }

    @Override
    public AssetManager getSplitAssetManager(int i) throws PackageParser.PackageParserException {
        return getBaseAssetManager();
    }

    @Override
    public void close() throws Exception {
        IoUtils.closeQuietly(this.mCachedAssetManager);
    }
}
