package com.android.settingslib.license;

import android.content.Context;
import android.util.Log;
import com.android.settingslib.utils.AsyncLoader;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LicenseHtmlLoader extends AsyncLoader<File> {
    private static final String[] DEFAULT_LICENSE_XML_PATHS = {"/system/etc/NOTICE.xml.gz", "/vendor/etc/NOTICE.xml.gz", "/odm/etc/NOTICE.xml.gz", "/oem/etc/NOTICE.xml.gz"};
    private Context mContext;

    public LicenseHtmlLoader(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    public File loadInBackground() {
        return generateHtmlFromDefaultXmlFiles();
    }

    @Override
    public void onDiscardResult(File file) {
    }

    private File generateHtmlFromDefaultXmlFiles() {
        List<File> vaildXmlFiles = getVaildXmlFiles();
        if (vaildXmlFiles.isEmpty()) {
            Log.e("LicenseHtmlLoader", "No notice file exists.");
            return null;
        }
        File cachedHtmlFile = getCachedHtmlFile();
        if (!isCachedHtmlFileOutdated(vaildXmlFiles, cachedHtmlFile) || generateHtmlFile(vaildXmlFiles, cachedHtmlFile)) {
            return cachedHtmlFile;
        }
        return null;
    }

    List<File> getVaildXmlFiles() {
        ArrayList arrayList = new ArrayList();
        for (String str : DEFAULT_LICENSE_XML_PATHS) {
            File file = new File(str);
            if (file.exists() && file.length() != 0) {
                arrayList.add(file);
            }
        }
        return arrayList;
    }

    File getCachedHtmlFile() {
        return new File(this.mContext.getCacheDir(), "NOTICE.html");
    }

    boolean isCachedHtmlFileOutdated(List<File> list, File file) {
        if (!file.exists() || file.length() == 0) {
            return true;
        }
        Iterator<File> it = list.iterator();
        while (it.hasNext()) {
            if (file.lastModified() < it.next().lastModified()) {
                return true;
            }
        }
        return false;
    }

    boolean generateHtmlFile(List<File> list, File file) {
        return LicenseHtmlGeneratorFromXml.generateHtml(list, file);
    }
}
