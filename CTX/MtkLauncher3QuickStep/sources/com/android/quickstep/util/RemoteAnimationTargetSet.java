package com.android.quickstep.util;

import com.android.systemui.shared.system.RemoteAnimationTargetCompat;
import java.util.ArrayList;

public class RemoteAnimationTargetSet {
    public final RemoteAnimationTargetCompat[] apps;
    public final RemoteAnimationTargetCompat[] unfilteredApps;

    public RemoteAnimationTargetSet(RemoteAnimationTargetCompat[] remoteAnimationTargetCompatArr, int i) {
        ArrayList arrayList = new ArrayList();
        if (remoteAnimationTargetCompatArr != null) {
            for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : remoteAnimationTargetCompatArr) {
                if (remoteAnimationTargetCompat.mode == i) {
                    arrayList.add(remoteAnimationTargetCompat);
                }
            }
        }
        this.unfilteredApps = remoteAnimationTargetCompatArr;
        this.apps = (RemoteAnimationTargetCompat[]) arrayList.toArray(new RemoteAnimationTargetCompat[arrayList.size()]);
    }

    public RemoteAnimationTargetCompat findTask(int i) {
        for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : this.apps) {
            if (remoteAnimationTargetCompat.taskId == i) {
                return remoteAnimationTargetCompat;
            }
        }
        return null;
    }

    public boolean isAnimatingHome() {
        for (RemoteAnimationTargetCompat remoteAnimationTargetCompat : this.apps) {
            if (remoteAnimationTargetCompat.activityType == 2) {
                return true;
            }
        }
        return false;
    }
}
