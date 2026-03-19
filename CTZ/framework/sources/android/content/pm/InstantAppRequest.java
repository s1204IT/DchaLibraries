package android.content.pm;

import android.content.Intent;
import android.content.pm.InstantAppResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;

public final class InstantAppRequest {
    public final String callingPackage;
    public final InstantAppResolveInfo.InstantAppDigest digest;
    public final Intent origIntent;
    public final boolean resolveForStart;
    public final String resolvedType;
    public final AuxiliaryResolveInfo responseObj;
    public final int userId;
    public final Bundle verificationBundle;

    public InstantAppRequest(AuxiliaryResolveInfo auxiliaryResolveInfo, Intent intent, String str, String str2, int i, Bundle bundle, boolean z) {
        this.responseObj = auxiliaryResolveInfo;
        this.origIntent = intent;
        this.resolvedType = str;
        this.callingPackage = str2;
        this.userId = i;
        this.verificationBundle = bundle;
        this.resolveForStart = z;
        if (intent.getData() != null && !TextUtils.isEmpty(intent.getData().getHost())) {
            this.digest = new InstantAppResolveInfo.InstantAppDigest(intent.getData().getHost(), 5);
        } else {
            this.digest = InstantAppResolveInfo.InstantAppDigest.UNDEFINED;
        }
    }
}
