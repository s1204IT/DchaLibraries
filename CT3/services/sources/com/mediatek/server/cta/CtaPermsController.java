package com.mediatek.server.cta;

import android.content.Context;
import android.content.pm.PackageParser;
import android.content.pm.PermissionRecords;
import android.os.Build;
import com.mediatek.server.cta.impl.CtaPermLinker;
import com.mediatek.server.cta.impl.PermErrorHelper;
import com.mediatek.server.cta.impl.PermRecordsController;
import com.mediatek.server.cta.impl.PermReviewFlagHelper;
import java.util.List;

public class CtaPermsController {
    public static boolean DEBUG = "eng".equals(Build.TYPE);
    private Context mContext;
    private PermRecordsController mPermRecordsController;

    public CtaPermsController(Context context) {
        this.mContext = context;
        this.mPermRecordsController = new PermRecordsController(context);
    }

    public void configDebugFlag(boolean z) {
        DEBUG = z;
    }

    public void systemReady() {
        this.mPermRecordsController.systemReady();
    }

    public boolean isPermissionReviewRequired(PackageParser.Package r2, int i, boolean z) {
        return PermReviewFlagHelper.getInstance(this.mContext).isPermissionReviewRequired(r2, i, z);
    }

    public List<String> getPermRecordPkgs() {
        return this.mPermRecordsController.getPermRecordPkgs();
    }

    public List<String> getPermRecordPerms(String str) {
        return this.mPermRecordsController.getPermRecordPerms(str);
    }

    public PermissionRecords getPermRecords(String str, String str2) {
        return this.mPermRecordsController.getPermRecords(str, str2);
    }

    public void reportPermRequestUsage(String str, int i) {
        this.mPermRecordsController.reportPermRequestUsage(str, i);
    }

    public void shutdown() {
        this.mPermRecordsController.shutdown();
    }

    public String parsePermName(int i, String str, String str2) {
        return PermErrorHelper.getInstance(this.mContext).parsePermName(i, str, str2);
    }

    public void linkCtaPermissions(PackageParser.Package r2) {
        CtaPermLinker.getInstance(this.mContext).link(r2);
    }
}
