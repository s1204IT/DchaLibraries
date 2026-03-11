package com.android.systemui;

import android.app.PendingIntent;
import android.content.Intent;
import com.android.systemui.plugins.ActivityStarter;

public class ActivityStarterDelegate implements ActivityStarter {
    private ActivityStarter mActualStarter;

    @Override
    public void startPendingIntentDismissingKeyguard(PendingIntent pendingIntent) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startPendingIntentDismissingKeyguard(pendingIntent);
    }

    @Override
    public void startActivity(Intent intent, boolean z) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startActivity(intent, z);
    }

    @Override
    public void startActivity(Intent intent, boolean z, boolean z2) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startActivity(intent, z, z2);
    }

    @Override
    public void startActivity(Intent intent, boolean z, ActivityStarter.Callback callback) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.startActivity(intent, z, callback);
    }

    @Override
    public void postStartActivityDismissingKeyguard(Intent intent, int i) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.postStartActivityDismissingKeyguard(intent, i);
    }

    @Override
    public void postStartActivityDismissingKeyguard(PendingIntent pendingIntent) {
        if (this.mActualStarter == null) {
            return;
        }
        this.mActualStarter.postStartActivityDismissingKeyguard(pendingIntent);
    }

    @Override
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
