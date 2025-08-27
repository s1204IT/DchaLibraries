package com.android.systemui;

import android.app.PendingIntent;
import android.content.Intent;
import com.android.systemui.plugins.ActivityStarter;

/* loaded from: classes.dex */
public class ActivityStarterDelegate implements ActivityStarter {
    private ActivityStarter mActualStarter;

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startPendingIntentDismissingKeyguard(pendingIntent);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startActivity(intent, z);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z, boolean z2) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startActivity(intent, z, z2);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void startActivity(Intent intent, boolean z, ActivityStarter.Callback callback) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startActivity(intent, z, callback);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postStartActivityDismissingKeyguard(Intent intent, int i) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.postStartActivityDismissingKeyguard(intent, i);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postStartActivityDismissingKeyguard(PendingIntent pendingIntent) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.postStartActivityDismissingKeyguard(pendingIntent);
    }

    @Override // com.android.systemui.plugins.ActivityStarter
    public void postQSRunnableDismissingKeyguard(Runnable runnable) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.postQSRunnableDismissingKeyguard(runnable);
    }

    public void setActivityStarterImpl(ActivityStarter activityStarter) {
        this.mActualStarter = activityStarter;
    }
}
