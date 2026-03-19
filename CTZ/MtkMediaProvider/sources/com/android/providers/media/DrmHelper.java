package com.android.providers.media;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;
import android.util.SparseIntArray;
import java.util.HashSet;
import java.util.Iterator;

public class DrmHelper {
    private static final SparseIntArray sPermitState = new SparseIntArray(32);
    private static final HashSet<String> sDrmPermitProcessList = new HashSet<>(16);

    static {
        initDrmPermistProcessList();
    }

    public static boolean isPermitedAccessDrm(Context context, int i) {
        return !MediaUtils.IS_SUPPORT_DRM || (getPermittedState(context, i) & 1) > 0;
    }

    private static synchronized int getPermittedState(Context context, int i) {
        int i2;
        i2 = sPermitState.get(i, -1);
        if (i2 < 0) {
            sPermitState.clear();
            Iterator<ActivityManager.RunningAppProcessInfo> it = ((ActivityManager) context.getSystemService("activity")).getRunningAppProcesses().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ActivityManager.RunningAppProcessInfo next = it.next();
                if (next.pid == Process.myPid()) {
                    sPermitState.put(next.pid, 1);
                } else {
                    sPermitState.put(next.pid, sDrmPermitProcessList.contains(next.processName) ? 1 : 0);
                }
            }
            i2 = sPermitState.get(i, 0);
        }
        return i2;
    }

    private static void initDrmPermistProcessList() {
        for (String str : new String[]{"com.android.music", "com.android.gallery", "com.android.gallery:CropImage", "com.cooliris.media", "com.mediatek.videoplayer", "com.mediatek.videoplayer2", "com.android.settings", "com.android.gallery3d", "com.android.gallery3d:crop", "com.android.gallery3d:widgetservice", "com.android.deskclock", "com.android.mms", "system"}) {
            sDrmPermitProcessList.add(str);
        }
    }
}
