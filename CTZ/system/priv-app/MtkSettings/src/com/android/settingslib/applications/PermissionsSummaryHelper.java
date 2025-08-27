package com.android.settingslib.applications;

import android.content.Context;
import android.content.pm.permission.RuntimePermissionPresentationInfo;
import android.content.pm.permission.RuntimePermissionPresenter;
import android.os.Handler;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class PermissionsSummaryHelper {
    public static void getPermissionSummary(Context context, String str, final PermissionsResultCallback permissionsResultCallback) {
        RuntimePermissionPresenter.getInstance(context).getAppPermissions(str, new RuntimePermissionPresenter.OnResultCallback() { // from class: com.android.settingslib.applications.PermissionsSummaryHelper.1
            public void onGetAppPermissions(List<RuntimePermissionPresentationInfo> list) {
                int size = list.size();
                ArrayList arrayList = new ArrayList();
                int i = 0;
                int i2 = 0;
                int i3 = 0;
                for (int i4 = 0; i4 < size; i4++) {
                    RuntimePermissionPresentationInfo runtimePermissionPresentationInfo = list.get(i4);
                    i++;
                    if (runtimePermissionPresentationInfo.isGranted()) {
                        if (runtimePermissionPresentationInfo.isStandard()) {
                            arrayList.add(runtimePermissionPresentationInfo.getLabel());
                            i2++;
                        } else {
                            i3++;
                        }
                    }
                }
                Collator collator = Collator.getInstance();
                collator.setStrength(0);
                Collections.sort(arrayList, collator);
                permissionsResultCallback.onPermissionSummaryResult(i2, i, i3, arrayList);
            }
        }, (Handler) null);
    }

    public static abstract class PermissionsResultCallback {
        public void onPermissionSummaryResult(int i, int i2, int i3, List<CharSequence> list) {
        }
    }
}
