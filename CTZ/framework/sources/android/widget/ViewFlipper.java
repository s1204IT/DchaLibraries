package android.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.os.Process;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.widget.RemoteViews;
import com.android.internal.R;

@RemoteViews.RemoteView
public class ViewFlipper extends ViewAnimator {
    private static final int DEFAULT_INTERVAL = 3000;
    private static final boolean LOGD = false;
    private static final String TAG = "ViewFlipper";
    private boolean mAutoStart;
    private int mFlipInterval;
    private final Runnable mFlipRunnable;
    private final BroadcastReceiver mReceiver;
    private boolean mRunning;
    private boolean mStarted;
    private boolean mUserPresent;
    private boolean mVisible;

    public ViewFlipper(Context context) {
        super(context);
        this.mFlipInterval = DEFAULT_INTERVAL;
        this.mAutoStart = false;
        this.mRunning = false;
        this.mStarted = false;
        this.mVisible = false;
        this.mUserPresent = true;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    ViewFlipper.this.mUserPresent = false;
                    ViewFlipper.this.updateRunning();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    ViewFlipper.this.mUserPresent = true;
                    ViewFlipper.this.updateRunning(false);
                }
            }
        };
        this.mFlipRunnable = new Runnable() {
            @Override
            public void run() {
                if (ViewFlipper.this.mRunning) {
                    ViewFlipper.this.showNext();
                    ViewFlipper.this.postDelayed(ViewFlipper.this.mFlipRunnable, ViewFlipper.this.mFlipInterval);
                }
            }
        };
    }

    public ViewFlipper(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFlipInterval = DEFAULT_INTERVAL;
        this.mAutoStart = false;
        this.mRunning = false;
        this.mStarted = false;
        this.mVisible = false;
        this.mUserPresent = true;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    ViewFlipper.this.mUserPresent = false;
                    ViewFlipper.this.updateRunning();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    ViewFlipper.this.mUserPresent = true;
                    ViewFlipper.this.updateRunning(false);
                }
            }
        };
        this.mFlipRunnable = new Runnable() {
            @Override
            public void run() {
                if (ViewFlipper.this.mRunning) {
                    ViewFlipper.this.showNext();
                    ViewFlipper.this.postDelayed(ViewFlipper.this.mFlipRunnable, ViewFlipper.this.mFlipInterval);
                }
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ViewFlipper);
        this.mFlipInterval = typedArrayObtainStyledAttributes.getInt(0, DEFAULT_INTERVAL);
        this.mAutoStart = typedArrayObtainStyledAttributes.getBoolean(1, false);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);
        getContext().registerReceiverAsUser(this.mReceiver, Process.myUserHandle(), intentFilter, null, getHandler());
        if (this.mAutoStart) {
            startFlipping();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mVisible = false;
        getContext().unregisterReceiver(this.mReceiver);
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        this.mVisible = i == 0;
        updateRunning(false);
    }

    @RemotableViewMethod
    public void setFlipInterval(int i) {
        this.mFlipInterval = i;
    }

    public void startFlipping() {
        this.mStarted = true;
        updateRunning();
    }

    public void stopFlipping() {
        this.mStarted = false;
        updateRunning();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return ViewFlipper.class.getName();
    }

    private void updateRunning() {
        updateRunning(true);
    }

    private void updateRunning(boolean z) {
        boolean z2 = this.mVisible && this.mStarted && this.mUserPresent;
        if (z2 != this.mRunning) {
            if (z2) {
                showOnly(this.mWhichChild, z);
                postDelayed(this.mFlipRunnable, this.mFlipInterval);
            } else {
                removeCallbacks(this.mFlipRunnable);
            }
            this.mRunning = z2;
        }
    }

    public boolean isFlipping() {
        return this.mStarted;
    }

    public void setAutoStart(boolean z) {
        this.mAutoStart = z;
    }

    public boolean isAutoStart() {
        return this.mAutoStart;
    }
}
