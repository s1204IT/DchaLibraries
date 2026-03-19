package com.mediatek.server.cta.impl;

import android.content.Context;
import android.content.pm.PackageParser;
import android.text.TextUtils;
import android.util.Slog;
import com.mediatek.Manifest;
import com.mediatek.cta.CtaPermissions;
import com.mediatek.internal.R;
import com.mediatek.server.cta.CtaPermsController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class CtaPermLinker {
    private static final String TAG = "CtaPermLinker";
    private static HashSet<String> sForceAddEmailPkgs;
    private static HashSet<String> sForceAddMmsPkgs;
    private static CtaPermLinker sInstance;

    private CtaPermLinker(Context context) {
        if (sForceAddEmailPkgs == null) {
            sForceAddEmailPkgs = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.force_add_send_email_pkgs)));
        }
        if (sForceAddMmsPkgs == null) {
            sForceAddMmsPkgs = new HashSet<>(Arrays.asList(context.getResources().getStringArray(R.array.force_add_send_mms_pkgs)));
        }
        if (CtaPermsController.DEBUG) {
            Iterator<String> it = sForceAddEmailPkgs.iterator();
            while (it.hasNext()) {
                Slog.d(TAG, "sForceAddEmailPkgs pkg = " + it.next());
            }
            Iterator<String> it2 = sForceAddMmsPkgs.iterator();
            while (it2.hasNext()) {
                Slog.d(TAG, "sForceAddMmsPkgs pkg = " + it2.next());
            }
        }
    }

    public static CtaPermLinker getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CtaPermLinker(context);
        }
        return sInstance;
    }

    public void link(PackageParser.Package r7) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        linkCtaPermissionsInternal(r7);
        Slog.i(TAG, "linkCtaPermissions takes " + (System.currentTimeMillis() - jCurrentTimeMillis) + " ms for pkg: " + (r7 == null ? "null" : r7.packageName));
    }

    private void linkCtaPermissionsInternal(PackageParser.Package r4) {
        int i = 0;
        if (r4 == null) {
            Slog.w(TAG, "linkCtaPermissionsInternal pkg is null");
            return;
        }
        while (true) {
            int i2 = i;
            if (i2 < r4.requestedPermissions.size()) {
                String str = (String) r4.requestedPermissions.get(i2);
                if (CtaPermissions.MAP.containsKey(str)) {
                    Iterator it = ((List) CtaPermissions.MAP.get(str)).iterator();
                    while (it.hasNext()) {
                        addCtaPermission(r4, (String) it.next());
                    }
                }
                i = i2 + 1;
            } else {
                boolean zContains = r4.requestedPermissions.contains("android.permission.INTERNET");
                handleCtaSendEmailPerm(r4, zContains);
                handleCtaSendMmsPerm(r4, zContains);
                return;
            }
        }
    }

    private void handleCtaSendEmailPerm(PackageParser.Package r9, boolean z) {
        if (!z) {
            return;
        }
        if (sForceAddEmailPkgs.contains(r9.packageName)) {
            addCtaPermission(r9, Manifest.permission.CTA_SEND_EMAIL);
        }
        if (!TextUtils.isEmpty(r9.packageName) && r9.packageName.contains("mail")) {
            addCtaPermission(r9, Manifest.permission.CTA_SEND_EMAIL);
        }
        if (r9.requestedPermissions.contains(Manifest.permission.CTA_SEND_EMAIL) || r9.activities == null) {
            return;
        }
        ArrayList arrayList = r9.activities;
        for (int i = 0; i < arrayList.size(); i++) {
            ArrayList arrayList2 = ((PackageParser.Activity) arrayList.get(i)).intents;
            if (arrayList2 != null) {
                int size = arrayList2.size();
                int i2 = 0;
                while (true) {
                    if (i2 >= size) {
                        break;
                    }
                    PackageParser.ActivityIntentInfo activityIntentInfo = (PackageParser.ActivityIntentInfo) arrayList2.get(i2);
                    if ((!activityIntentInfo.hasAction("android.intent.action.SEND") && !activityIntentInfo.hasAction("android.intent.action.SENDTO") && !activityIntentInfo.hasAction("android.intent.action.SEND_MULTIPLE")) || !activityIntentInfo.hasDataScheme("mailto")) {
                        i2++;
                    } else {
                        addCtaPermission(r9, Manifest.permission.CTA_SEND_EMAIL);
                        break;
                    }
                }
            }
        }
    }

    private void handleCtaSendMmsPerm(PackageParser.Package r9, boolean z) {
        if (!z) {
            return;
        }
        if (sForceAddMmsPkgs.contains(r9.packageName)) {
            addCtaPermission(r9, Manifest.permission.CTA_SEND_MMS);
        }
        if (r9.requestedPermissions.contains("android.permission.RECEIVE_MMS")) {
            addCtaPermission(r9, Manifest.permission.CTA_SEND_MMS);
        }
        if (r9.requestedPermissions.contains(Manifest.permission.CTA_SEND_MMS) || r9.activities == null) {
            return;
        }
        ArrayList arrayList = r9.activities;
        for (int i = 0; i < arrayList.size(); i++) {
            ArrayList arrayList2 = ((PackageParser.Activity) arrayList.get(i)).intents;
            if (arrayList2 != null) {
                int size = arrayList2.size();
                for (int i2 = 0; i2 < size; i2++) {
                    PackageParser.ActivityIntentInfo activityIntentInfo = (PackageParser.ActivityIntentInfo) arrayList2.get(i2);
                    if ((activityIntentInfo.hasAction("android.intent.action.SEND") || activityIntentInfo.hasAction("android.intent.action.SENDTO") || activityIntentInfo.hasAction("android.intent.action.SEND_MULTIPLE")) && (activityIntentInfo.hasDataScheme("mms") || activityIntentInfo.hasDataScheme("mmsto"))) {
                        addCtaPermission(r9, Manifest.permission.CTA_SEND_MMS);
                        break;
                    }
                }
            }
        }
    }

    private void addCtaPermission(PackageParser.Package r3, String str) {
        if (r3.requestedPermissions.indexOf(str) == -1) {
            r3.requestedPermissions.add(str);
        }
    }
}
