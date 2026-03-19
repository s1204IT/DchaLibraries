package com.mediatek.plugin.zip;

import android.content.Context;
import com.mediatek.plugin.res.IResource;
import com.mediatek.plugin.res.JarResource;
import com.mediatek.plugin.utils.TraceHelper;

public class JarFile extends ZipFile {
    private IResource mResource;

    public static String getSuffix() {
        return ".jar";
    }

    public JarFile(String str) {
        super(str);
    }

    @Override
    public String getXmlRelativePath() {
        return "res/raw/plugin.xml";
    }

    @Override
    public IResource getResource(Context context) {
        TraceHelper.beginSection(">>>>JarFile-getResource");
        if (this.mResource == null) {
            this.mResource = new JarResource(context, this.mFilPath);
        }
        TraceHelper.endSection();
        return this.mResource;
    }
}
