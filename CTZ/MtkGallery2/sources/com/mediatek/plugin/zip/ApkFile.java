package com.mediatek.plugin.zip;

import android.content.Context;
import android.content.pm.PackageInfo;
import com.mediatek.plugin.res.ApkResource;
import com.mediatek.plugin.res.IResource;
import com.mediatek.plugin.utils.TraceHelper;

public class ApkFile extends ZipFile {
    protected PackageInfo mInfo;
    private IResource mResource;

    public static String getSuffix() {
        return ".apk";
    }

    public ApkFile(String str) {
        super(str);
    }

    @Override
    public String getXmlRelativePath() {
        return "res/raw/plugin.xml";
    }

    @Override
    public IResource getResource(Context context) {
        if (this.mResource == null) {
            initRes(context);
        }
        return this.mResource;
    }

    public PackageInfo getPackageInfo(Context context) {
        if (this.mInfo == null) {
            initRes(context);
        }
        return this.mInfo;
    }

    private void initRes(Context context) {
        TraceHelper.beginSection(">>>>ApkFile-initResource");
        this.mInfo = context.getPackageManager().getPackageArchiveInfo(this.mFilPath, 16384);
        this.mResource = new ApkResource(context, this.mFilPath, this.mInfo == null ? null : this.mInfo.packageName);
        TraceHelper.endSection();
    }
}
