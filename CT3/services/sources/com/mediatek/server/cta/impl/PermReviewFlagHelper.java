package com.mediatek.server.cta.impl;

import android.content.Context;
import android.content.pm.PackageParser;
import android.os.UserHandle;
import com.mediatek.cta.CtaUtils;
import java.util.Iterator;

public class PermReviewFlagHelper {
    private static final String TAG = "PermReviewFlagHelper";
    private static PermReviewFlagHelper sInstance;
    private Context mContext;

    private PermReviewFlagHelper(Context context) {
        this.mContext = context;
    }

    public static PermReviewFlagHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PermReviewFlagHelper(context);
        }
        return sInstance;
    }

    public boolean isPermissionReviewRequired(PackageParser.Package r7, int i, boolean z) {
        if (!CtaUtils.isCtaSupported()) {
            return z;
        }
        if (!z) {
            return false;
        }
        if (r7.mSharedUserId == null) {
            return z;
        }
        if (r7.requestedPermissions.size() == 0) {
            return false;
        }
        UserHandle userHandleOf = UserHandle.of(i);
        Iterator it = r7.requestedPermissions.iterator();
        while (it.hasNext()) {
            if ((this.mContext.getPackageManager().getPermissionFlags((String) it.next(), r7.packageName, userHandleOf) & 64) != 0) {
                return true;
            }
        }
        return false;
    }
}
