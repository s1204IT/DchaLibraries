package com.android.systemui.statusbar.phone;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.TimeUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.Interpolators;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class LightBarTransitionsController implements Dumpable, CommandQueue.Callbacks {
    private final DarkIntensityApplier mApplier;
    private float mDarkIntensity;
    private float mNextDarkIntensity;
    private float mPendingDarkIntensity;
    private ValueAnimator mTintAnimator;
    private boolean mTintChangePending;
    private boolean mTransitionDeferring;
    private long mTransitionDeferringDuration;
    private long mTransitionDeferringStartTime;
    private boolean mTransitionPending;
    private final Runnable mTransitionDeferringDoneRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.LightBarTransitionsController.1
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            LightBarTransitionsController.this.mTransitionDeferring = false;
        }
    };
    private final Handler mHandler = new Handler();
    private final KeyguardMonitor mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);

    public interface DarkIntensityApplier {
        void applyDarkIntensity(float f);
    }

    /* renamed from: com.android.systemui.statusbar.phone.LightBarTransitionsController$1 */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            LightBarTransitionsController.this.mTransitionDeferring = false;
        }
    }

    public LightBarTransitionsController(Context context, DarkIntensityApplier darkIntensityApplier) {
        this.mApplier = darkIntensityApplier;
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).addCallbacks(this);
    }

    public void destroy(Context context) {
        ((CommandQueue) SysUiServiceProvider.getComponent(context, CommandQueue.class)).removeCallbacks(this);
    }

    public void saveState(Bundle bundle) {
        bundle.putFloat("dark_intensity", (this.mTintAnimator == null || !this.mTintAnimator.isRunning()) ? this.mDarkIntensity : this.mNextDarkIntensity);
    }

    public void restoreState(Bundle bundle) {
        setIconTintInternal(bundle.getFloat("dark_intensity", 0.0f));
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionPending(boolean z) {
        if (this.mKeyguardMonitor.isKeyguardGoingAway() && !z) {
            return;
        }
        this.mTransitionPending = true;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionCancelled() {
        if (this.mTransitionPending && this.mTintChangePending) {
            this.mTintChangePending = false;
            animateIconTint(this.mPendingDarkIntensity, 0L, 120L);
        }
        this.mTransitionPending = false;
    }

    @Override // com.android.systemui.statusbar.CommandQueue.Callbacks
    public void appTransitionStarting(long j, long j2, boolean z) {
        if (this.mKeyguardMonitor.isKeyguardGoingAway() && !z) {
            return;
        }
        if (this.mTransitionPending && this.mTintChangePending) {
            this.mTintChangePending = false;
            animateIconTint(this.mPendingDarkIntensity, Math.max(0L, j - SystemClock.uptimeMillis()), j2);
        } else if (this.mTransitionPending) {
            this.mTransitionDeferring = true;
            this.mTransitionDeferringStartTime = j;
            this.mTransitionDeferringDuration = j2;
            this.mHandler.removeCallbacks(this.mTransitionDeferringDoneRunnable);
            this.mHandler.postAtTime(this.mTransitionDeferringDoneRunnable, j);
        }
        this.mTransitionPending = false;
    }

    public void setIconsDark(boolean z, boolean z2) {
        if (!z2) {
            setIconTintInternal(z ? 1.0f : 0.0f);
            this.mNextDarkIntensity = z ? 1.0f : 0.0f;
        } else if (this.mTransitionPending) {
            deferIconTintChange(z ? 1.0f : 0.0f);
        } else if (this.mTransitionDeferring) {
            animateIconTint(z ? 1.0f : 0.0f, Math.max(0L, this.mTransitionDeferringStartTime - SystemClock.uptimeMillis()), this.mTransitionDeferringDuration);
        } else {
            animateIconTint(z ? 1.0f : 0.0f, 0L, 120L);
        }
    }

    public float getCurrentDarkIntensity() {
        return this.mDarkIntensity;
    }

    private void deferIconTintChange(float f) {
        if (this.mTintChangePending && f == this.mPendingDarkIntensity) {
            return;
        }
        this.mTintChangePending = true;
        this.mPendingDarkIntensity = f;
    }

    private void animateIconTint(float f, long j, long j2) {
        if (this.mNextDarkIntensity == f) {
            return;
        }
        if (this.mTintAnimator != null) {
            this.mTintAnimator.cancel();
        }
        this.mNextDarkIntensity = f;
        this.mTintAnimator = ValueAnimator.ofFloat(this.mDarkIntensity, f);
        this.mTintAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.systemui.statusbar.phone.-$$Lambda$LightBarTransitionsController$PJRveQsGC7aANrqdSv3tRYb3x7c
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                this.f$0.setIconTintInternal(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        this.mTintAnimator.setDuration(j2);
        this.mTintAnimator.setStartDelay(j);
        this.mTintAnimator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        this.mTintAnimator.start();
    }

    private void setIconTintInternal(float f) {
        this.mDarkIntensity = f;
        this.mApplier.applyDarkIntensity(f);
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mTransitionDeferring=");
        printWriter.print(this.mTransitionDeferring);
        if (this.mTransitionDeferring) {
            printWriter.println();
            printWriter.print("   mTransitionDeferringStartTime=");
            printWriter.println(TimeUtils.formatUptime(this.mTransitionDeferringStartTime));
            printWriter.print("   mTransitionDeferringDuration=");
            TimeUtils.formatDuration(this.mTransitionDeferringDuration, printWriter);
            printWriter.println();
        }
        printWriter.print("  mTransitionPending=");
        printWriter.print(this.mTransitionPending);
        printWriter.print(" mTintChangePending=");
        printWriter.println(this.mTintChangePending);
        printWriter.print("  mPendingDarkIntensity=");
        printWriter.print(this.mPendingDarkIntensity);
        printWriter.print(" mDarkIntensity=");
        printWriter.print(this.mDarkIntensity);
        printWriter.print(" mNextDarkIntensity=");
        printWriter.println(this.mNextDarkIntensity);
    }
}
