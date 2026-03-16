package android.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RemoteViews;
import com.android.internal.R;

@RemoteViews.RemoteView
public class ViewFlipper extends ViewAnimator {
    private static final int DEFAULT_INTERVAL = 3000;
    private static final boolean LOGD = false;
    private static final String TAG = "ViewFlipper";
    private final int FLIP_MSG;
    private boolean mAutoStart;
    private int mFlipInterval;
    private final Handler mHandler;
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
        this.FLIP_MSG = 1;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1 && ViewFlipper.this.mRunning) {
                    ViewFlipper.this.showNext();
                    Message msg2 = obtainMessage(1);
                    sendMessageDelayed(msg2, ViewFlipper.this.mFlipInterval);
                }
            }
        };
    }

    public ViewFlipper(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        this.FLIP_MSG = 1;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1 && ViewFlipper.this.mRunning) {
                    ViewFlipper.this.showNext();
                    Message msg2 = obtainMessage(1);
                    sendMessageDelayed(msg2, ViewFlipper.this.mFlipInterval);
                }
            }
        };
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewFlipper);
        this.mFlipInterval = a.getInt(0, DEFAULT_INTERVAL);
        this.mAutoStart = a.getBoolean(1, false);
        a.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        getContext().registerReceiverAsUser(this.mReceiver, Process.myUserHandle(), filter, null, this.mHandler);
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
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        this.mVisible = visibility == 0;
        updateRunning(false);
    }

    @RemotableViewMethod
    public void setFlipInterval(int milliseconds) {
        this.mFlipInterval = milliseconds;
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
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(ViewFlipper.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(ViewFlipper.class.getName());
    }

    private void updateRunning() {
        updateRunning(true);
    }

    private void updateRunning(boolean flipNow) {
        boolean running = this.mVisible && this.mStarted && this.mUserPresent;
        if (running != this.mRunning) {
            if (running) {
                showOnly(this.mWhichChild, flipNow);
                Message msg = this.mHandler.obtainMessage(1);
                this.mHandler.sendMessageDelayed(msg, this.mFlipInterval);
            } else {
                this.mHandler.removeMessages(1);
            }
            this.mRunning = running;
        }
    }

    public boolean isFlipping() {
        return this.mStarted;
    }

    public void setAutoStart(boolean autoStart) {
        this.mAutoStart = autoStart;
    }

    public boolean isAutoStart() {
        return this.mAutoStart;
    }
}
