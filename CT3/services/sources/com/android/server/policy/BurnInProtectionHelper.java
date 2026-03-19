package com.android.server.policy;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.os.SystemClock;
import android.view.Display;
import android.view.animation.LinearInterpolator;
import com.android.server.LocalServices;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

public class BurnInProtectionHelper implements DisplayManager.DisplayListener, Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
    private static final String ACTION_BURN_IN_PROTECTION = "android.internal.policy.action.BURN_IN_PROTECTION";
    public static final int BURN_IN_MAX_RADIUS_DEFAULT = -1;
    private static final int BURN_IN_SHIFT_STEP = 2;
    private static final long CENTERING_ANIMATION_DURATION_MS = 100;
    private static final boolean DEBUG = false;
    private static final String TAG = "BurnInProtection";
    private final AlarmManager mAlarmManager;
    private boolean mBurnInProtectionActive;
    private final PendingIntent mBurnInProtectionIntent;
    private final int mBurnInRadiusMaxSquared;
    private final ValueAnimator mCenteringAnimator;
    private final Display mDisplay;
    private final DisplayManagerInternal mDisplayManagerInternal;
    private boolean mFirstUpdate;
    private final int mMaxHorizontalBurnInOffset;
    private final int mMaxVerticalBurnInOffset;
    private final int mMinHorizontalBurnInOffset;
    private final int mMinVerticalBurnInOffset;
    private static final long BURNIN_PROTECTION_WAKEUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long BURNIN_PROTECTION_MINIMAL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);
    private int mLastBurnInXOffset = 0;
    private int mXOffsetDirection = 1;
    private int mLastBurnInYOffset = 0;
    private int mYOffsetDirection = 1;
    private int mAppliedBurnInXOffset = 0;
    private int mAppliedBurnInYOffset = 0;
    private BroadcastReceiver mBurnInProtectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BurnInProtectionHelper.this.updateBurnInProtection();
        }
    };

    public BurnInProtectionHelper(Context context, int minHorizontalOffset, int maxHorizontalOffset, int minVerticalOffset, int maxVerticalOffset, int maxOffsetRadius) {
        this.mMinHorizontalBurnInOffset = minHorizontalOffset;
        this.mMaxHorizontalBurnInOffset = maxHorizontalOffset;
        this.mMinVerticalBurnInOffset = minVerticalOffset;
        this.mMaxVerticalBurnInOffset = maxVerticalOffset;
        if (maxOffsetRadius != -1) {
            this.mBurnInRadiusMaxSquared = maxOffsetRadius * maxOffsetRadius;
        } else {
            this.mBurnInRadiusMaxSquared = -1;
        }
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        context.registerReceiver(this.mBurnInProtectionReceiver, new IntentFilter(ACTION_BURN_IN_PROTECTION));
        Intent intent = new Intent(ACTION_BURN_IN_PROTECTION);
        intent.setPackage(context.getPackageName());
        intent.setFlags(1073741824);
        this.mBurnInProtectionIntent = PendingIntent.getBroadcast(context, 0, intent, 134217728);
        DisplayManager displayManager = (DisplayManager) context.getSystemService("display");
        this.mDisplay = displayManager.getDisplay(0);
        displayManager.registerDisplayListener(this, null);
        this.mCenteringAnimator = ValueAnimator.ofFloat(1.0f, 0.0f);
        this.mCenteringAnimator.setDuration(CENTERING_ANIMATION_DURATION_MS);
        this.mCenteringAnimator.setInterpolator(new LinearInterpolator());
        this.mCenteringAnimator.addListener(this);
        this.mCenteringAnimator.addUpdateListener(this);
    }

    public void startBurnInProtection() {
        if (this.mBurnInProtectionActive) {
            return;
        }
        this.mBurnInProtectionActive = true;
        this.mFirstUpdate = true;
        this.mCenteringAnimator.cancel();
        updateBurnInProtection();
    }

    private void updateBurnInProtection() {
        if (this.mBurnInProtectionActive) {
            if (this.mFirstUpdate) {
                this.mFirstUpdate = false;
            } else {
                adjustOffsets();
                this.mAppliedBurnInXOffset = this.mLastBurnInXOffset;
                this.mAppliedBurnInYOffset = this.mLastBurnInYOffset;
                this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), this.mLastBurnInXOffset, this.mLastBurnInYOffset);
            }
            long nowWall = System.currentTimeMillis();
            long nowElapsed = SystemClock.elapsedRealtime();
            long nextWall = nowWall + BURNIN_PROTECTION_MINIMAL_INTERVAL_MS;
            long nextElapsed = nowElapsed + (((nextWall - (nextWall % BURNIN_PROTECTION_WAKEUP_INTERVAL_MS)) + BURNIN_PROTECTION_WAKEUP_INTERVAL_MS) - nowWall);
            this.mAlarmManager.setExact(3, nextElapsed, this.mBurnInProtectionIntent);
            return;
        }
        this.mAlarmManager.cancel(this.mBurnInProtectionIntent);
        this.mCenteringAnimator.start();
    }

    public void cancelBurnInProtection() {
        if (!this.mBurnInProtectionActive) {
            return;
        }
        this.mBurnInProtectionActive = false;
        updateBurnInProtection();
    }

    private void adjustOffsets() {
        do {
            int xChange = this.mXOffsetDirection * 2;
            this.mLastBurnInXOffset += xChange;
            if (this.mLastBurnInXOffset > this.mMaxHorizontalBurnInOffset || this.mLastBurnInXOffset < this.mMinHorizontalBurnInOffset) {
                this.mLastBurnInXOffset -= xChange;
                this.mXOffsetDirection *= -1;
                int yChange = this.mYOffsetDirection * 2;
                this.mLastBurnInYOffset += yChange;
                if (this.mLastBurnInYOffset > this.mMaxVerticalBurnInOffset || this.mLastBurnInYOffset < this.mMinVerticalBurnInOffset) {
                    this.mLastBurnInYOffset -= yChange;
                    this.mYOffsetDirection *= -1;
                }
            }
            if (this.mBurnInRadiusMaxSquared == -1) {
                return;
            }
        } while ((this.mLastBurnInXOffset * this.mLastBurnInXOffset) + (this.mLastBurnInYOffset * this.mLastBurnInYOffset) > this.mBurnInRadiusMaxSquared);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mBurnInProtectionActive=" + this.mBurnInProtectionActive);
        pw.println(prefix2 + "mHorizontalBurnInOffsetsBounds=(" + this.mMinHorizontalBurnInOffset + ", " + this.mMaxHorizontalBurnInOffset + ")");
        pw.println(prefix2 + "mVerticalBurnInOffsetsBounds=(" + this.mMinVerticalBurnInOffset + ", " + this.mMaxVerticalBurnInOffset + ")");
        pw.println(prefix2 + "mBurnInRadiusMaxSquared=" + this.mBurnInRadiusMaxSquared);
        pw.println(prefix2 + "mLastBurnInOffset=(" + this.mLastBurnInXOffset + ", " + this.mLastBurnInYOffset + ")");
        pw.println(prefix2 + "mOfsetChangeDirections=(" + this.mXOffsetDirection + ", " + this.mYOffsetDirection + ")");
    }

    @Override
    public void onDisplayAdded(int i) {
    }

    @Override
    public void onDisplayRemoved(int i) {
    }

    @Override
    public void onDisplayChanged(int displayId) {
        if (displayId != this.mDisplay.getDisplayId()) {
            return;
        }
        if (this.mDisplay.getState() == 3 || this.mDisplay.getState() == 4) {
            startBurnInProtection();
        } else {
            cancelBurnInProtection();
        }
    }

    @Override
    public void onAnimationStart(Animator animator) {
    }

    @Override
    public void onAnimationEnd(Animator animator) {
        if (animator != this.mCenteringAnimator || this.mBurnInProtectionActive) {
            return;
        }
        this.mAppliedBurnInXOffset = 0;
        this.mAppliedBurnInYOffset = 0;
        this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), 0, 0);
    }

    @Override
    public void onAnimationCancel(Animator animator) {
    }

    @Override
    public void onAnimationRepeat(Animator animator) {
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        if (this.mBurnInProtectionActive) {
            return;
        }
        float value = ((Float) valueAnimator.getAnimatedValue()).floatValue();
        this.mDisplayManagerInternal.setDisplayOffsets(this.mDisplay.getDisplayId(), (int) (this.mAppliedBurnInXOffset * value), (int) (this.mAppliedBurnInYOffset * value));
    }
}
