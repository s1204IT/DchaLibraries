package com.android.statementservice.retriever;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AndroidPackageInfoFetcher {
    private Context mContext;

    public AndroidPackageInfoFetcher(Context context) {
        this.mContext = context;
    }

    public List<String> getCertFingerprints(String str) throws PackageManager.NameNotFoundException {
        return Utils.getCertFingerprintsFromPackageManager(str, this.mContext);
    }

    public List<String> getStatements(String str) throws PackageManager.NameNotFoundException {
        ApplicationInfo applicationInfo = this.mContext.getPackageManager().getPackageInfo(str, 128).applicationInfo;
        if (applicationInfo.metaData == null) {
            return Collections.emptyList();
        }
        int i = applicationInfo.metaData.getInt("associated_assets");
        if (i == 0) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(this.mContext.getPackageManager().getResourcesForApplication(str).getStringArray(i));
        } catch (Resources.NotFoundException e) {
            return Collections.emptyList();
        }
    }
}
