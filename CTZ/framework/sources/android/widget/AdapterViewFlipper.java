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
public class AdapterViewFlipper extends AdapterViewAnimator {
    private static final int DEFAULT_INTERVAL = 10000;
    private static final boolean LOGD = false;
    private static final String TAG = "ViewFlipper";
    private boolean mAdvancedByHost;
    private boolean mAutoStart;
    private int mFlipInterval;
    private final Runnable mFlipRunnable;
    private final BroadcastReceiver mReceiver;
    private boolean mRunning;
    private boolean mStarted;
    private boolean mUserPresent;
    private boolean mVisible;

    public AdapterViewFlipper(Context context) {
        super(context);
        this.mFlipInterval = 10000;
        this.mAutoStart = false;
        this.mRunning = false;
        this.mStarted = false;
        this.mVisible = false;
        this.mUserPresent = true;
        this.mAdvancedByHost = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    AdapterViewFlipper.this.mUserPresent = false;
                    AdapterViewFlipper.this.updateRunning();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    AdapterViewFlipper.this.mUserPresent = true;
                    AdapterViewFlipper.this.updateRunning(false);
                }
            }
        };
        this.mFlipRunnable = new Runnable() {
            @Override
            public void run() {
                if (AdapterViewFlipper.this.mRunning) {
                    AdapterViewFlipper.this.showNext();
                }
            }
        };
    }

    public AdapterViewFlipper(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public AdapterViewFlipper(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public AdapterViewFlipper(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mFlipInterval = 10000;
        this.mAutoStart = false;
        this.mRunning = false;
        this.mStarted = false;
        this.mVisible = false;
        this.mUserPresent = true;
        this.mAdvancedByHost = false;
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    AdapterViewFlipper.this.mUserPresent = false;
                    AdapterViewFlipper.this.updateRunning();
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    AdapterViewFlipper.this.mUserPresent = true;
                    AdapterViewFlipper.this.updateRunning(false);
                }
            }
        };
        this.mFlipRunnable = new Runnable() {
            @Override
            public void run() {
                if (AdapterViewFlipper.this.mRunning) {
                    AdapterViewFlipper.this.showNext();
                }
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AdapterViewFlipper, i, i2);
        this.mFlipInterval = typedArrayObtainStyledAttributes.getInt(0, 10000);
        this.mAutoStart = typedArrayObtainStyledAttributes.getBoolean(1, false);
        this.mLoopViews = true;
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

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        updateRunning();
    }

    public int getFlipInterval() {
        return this.mFlipInterval;
    }

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
    @RemotableViewMethod
    public void showNext() {
        if (this.mRunning) {
            removeCallbacks(this.mFlipRunnable);
            postDelayed(this.mFlipRunnable, this.mFlipInterval);
        }
        super.showNext();
    }

    @Override
    @RemotableViewMethod
    public void showPrevious() {
        if (this.mRunning) {
            removeCallbacks(this.mFlipRunnable);
            postDelayed(this.mFlipRunnable, this.mFlipInterval);
        }
        super.showPrevious();
    }

    private void updateRunning() {
        updateRunning(true);
    }

    private void updateRunning(boolean z) {
        boolean z2 = !this.mAdvancedByHost && this.mVisible && this.mStarted && this.mUserPresent && this.mAdapter != null;
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

    @Override
    public void fyiWillBeAdvancedByHostKThx() {
        this.mAdvancedByHost = true;
        updateRunning(false);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return AdapterViewFlipper.class.getName();
    }
}
