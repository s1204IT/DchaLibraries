package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Rect;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import com.android.systemui.ExpandHelper;
import com.android.systemui.R;
import com.android.systemui.SwipeHelper;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

public class HeadsUpNotificationView extends FrameLayout implements ViewTreeObserver.OnComputeInternalInsetsListener, ExpandHelper.Callback, SwipeHelper.Callback {
    private static final ViewOutlineProvider CONTENT_HOLDER_OUTLINE_PROVIDER = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            int outlineLeft = view.getPaddingLeft();
            int outlineTop = view.getPaddingTop();
            outline.setRect(outlineLeft, outlineTop, (view.getWidth() - outlineLeft) - view.getPaddingRight(), (view.getHeight() - outlineTop) - view.getPaddingBottom());
        }
    };
    private PhoneStatusBar mBar;
    private ViewGroup mContentHolder;
    private final int mDefaultSnoozeLengthMs;
    private EdgeSwipeHelper mEdgeSwipeHelper;
    private NotificationData.Entry mHeadsUp;
    private final float mMaxAlpha;
    private String mMostRecentPackageName;
    private ContentObserver mSettingsObserver;
    private int mSnoozeLengthMs;
    private final ArrayMap<String, Long> mSnoozedPackages;
    private long mStartTouchTime;
    private SwipeHelper mSwipeHelper;
    Rect mTmpRect;
    int[] mTmpTwoArray;
    private final int mTouchSensitivityDelay;
    private int mUser;

    public HeadsUpNotificationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeadsUpNotificationView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mTmpRect = new Rect();
        this.mTmpTwoArray = new int[2];
        this.mMaxAlpha = 1.0f;
        Resources resources = context.getResources();
        this.mTouchSensitivityDelay = resources.getInteger(R.integer.heads_up_sensitivity_delay);
        this.mSnoozedPackages = new ArrayMap<>();
        this.mDefaultSnoozeLengthMs = resources.getInteger(R.integer.heads_up_default_snooze_length_ms);
        this.mSnoozeLengthMs = this.mDefaultSnoozeLengthMs;
    }

    public void updateResources() {
        if (this.mContentHolder != null) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mContentHolder.getLayoutParams();
            lp.width = getResources().getDimensionPixelSize(R.dimen.notification_panel_width);
            lp.gravity = getResources().getInteger(R.integer.notification_panel_layout_gravity);
            this.mContentHolder.setLayoutParams(lp);
        }
    }

    public void setBar(PhoneStatusBar bar) {
        this.mBar = bar;
    }

    public ViewGroup getHolder() {
        return this.mContentHolder;
    }

    public boolean showNotification(NotificationData.Entry headsUp) {
        if (this.mHeadsUp != null && headsUp != null && !this.mHeadsUp.key.equals(headsUp.key)) {
            release();
        }
        this.mHeadsUp = headsUp;
        if (this.mContentHolder != null) {
            this.mContentHolder.removeAllViews();
        }
        if (this.mHeadsUp != null) {
            this.mMostRecentPackageName = this.mHeadsUp.notification.getPackageName();
            this.mHeadsUp.row.setSystemExpanded(true);
            this.mHeadsUp.row.setSensitive(false);
            this.mHeadsUp.row.setHeadsUp(true);
            this.mHeadsUp.row.setHideSensitive(false, false, 0L, 0L);
            if (this.mContentHolder == null) {
                return false;
            }
            this.mContentHolder.setX(0.0f);
            this.mContentHolder.setVisibility(0);
            this.mContentHolder.setAlpha(1.0f);
            this.mContentHolder.addView(this.mHeadsUp.row);
            sendAccessibilityEvent(2048);
            this.mSwipeHelper.snapChild(this.mContentHolder, 1.0f);
            this.mStartTouchTime = SystemClock.elapsedRealtime() + ((long) this.mTouchSensitivityDelay);
            this.mHeadsUp.setInterruption();
            this.mBar.scheduleHeadsUpOpen();
            this.mBar.resetHeadsUpDecayTimer();
        }
        return true;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView.getVisibility() == 0) {
            sendAccessibilityEvent(2048);
        }
    }

    public boolean isShowing(String key) {
        return this.mHeadsUp != null && this.mHeadsUp.key.equals(key);
    }

    public void clear() {
        this.mHeadsUp = null;
        this.mBar.scheduleHeadsUpClose();
    }

    public void dismiss() {
        if (this.mHeadsUp != null) {
            if (this.mHeadsUp.notification.isClearable()) {
                this.mBar.onNotificationClear(this.mHeadsUp.notification);
            } else {
                release();
            }
            this.mHeadsUp = null;
            this.mBar.scheduleHeadsUpClose();
        }
    }

    public void release() {
        if (this.mHeadsUp != null) {
            this.mBar.displayNotificationFromHeadsUp(this.mHeadsUp.notification);
        }
        this.mHeadsUp = null;
    }

    public boolean isSnoozed(String packageName) {
        String key = snoozeKey(packageName, this.mUser);
        Long snoozedUntil = this.mSnoozedPackages.get(key);
        if (snoozedUntil != null) {
            if (snoozedUntil.longValue() > SystemClock.elapsedRealtime()) {
                return true;
            }
            this.mSnoozedPackages.remove(packageName);
        }
        return false;
    }

    public void snooze() {
        if (this.mMostRecentPackageName != null) {
            this.mSnoozedPackages.put(snoozeKey(this.mMostRecentPackageName, this.mUser), Long.valueOf(SystemClock.elapsedRealtime() + ((long) this.mSnoozeLengthMs)));
        }
        releaseAndClose();
    }

    private static String snoozeKey(String packageName, int user) {
        return user + "," + packageName;
    }

    public void releaseAndClose() {
        release();
        this.mBar.scheduleHeadsUpClose();
    }

    public NotificationData.Entry getEntry() {
        return this.mHeadsUp;
    }

    public boolean isClearable() {
        return this.mHeadsUp == null || this.mHeadsUp.notification.isClearable();
    }

    @Override
    public void onAttachedToWindow() {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        float touchSlop = viewConfiguration.getScaledTouchSlop();
        this.mSwipeHelper = new SwipeHelper(0, this, getContext());
        this.mSwipeHelper.setMaxSwipeProgress(1.0f);
        this.mEdgeSwipeHelper = new EdgeSwipeHelper(touchSlop);
        getResources().getDimensionPixelSize(R.dimen.notification_min_height);
        getResources().getDimensionPixelSize(R.dimen.notification_max_height);
        this.mContentHolder = (ViewGroup) findViewById(R.id.content_holder);
        this.mContentHolder.setOutlineProvider(CONTENT_HOLDER_OUTLINE_PROVIDER);
        this.mSnoozeLengthMs = Settings.Global.getInt(this.mContext.getContentResolver(), "heads_up_snooze_length_ms", this.mDefaultSnoozeLengthMs);
        this.mSettingsObserver = new ContentObserver(getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                int packageSnoozeLengthMs = Settings.Global.getInt(HeadsUpNotificationView.this.mContext.getContentResolver(), "heads_up_snooze_length_ms", -1);
                if (packageSnoozeLengthMs > -1 && packageSnoozeLengthMs != HeadsUpNotificationView.this.mSnoozeLengthMs) {
                    HeadsUpNotificationView.this.mSnoozeLengthMs = packageSnoozeLengthMs;
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("heads_up_snooze_length_ms"), false, this.mSettingsObserver);
        if (this.mHeadsUp != null) {
            showNotification(this.mHeadsUp);
        }
        getViewTreeObserver().addOnComputeInternalInsetsListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return SystemClock.elapsedRealtime() < this.mStartTouchTime || this.mEdgeSwipeHelper.onInterceptTouchEvent(ev) || this.mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
    }

    @Override
    public void onDraw(Canvas c) {
        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (SystemClock.elapsedRealtime() < this.mStartTouchTime) {
            return false;
        }
        this.mBar.resetHeadsUpDecayTimer();
        return this.mEdgeSwipeHelper.onTouchEvent(ev) || this.mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        this.mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        this.mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    @Override
    public ExpandableView getChildAtRawPosition(float x, float y) {
        return getChildAtPosition(x, y);
    }

    @Override
    public ExpandableView getChildAtPosition(float x, float y) {
        if (this.mHeadsUp == null) {
            return null;
        }
        return this.mHeadsUp.row;
    }

    @Override
    public boolean canChildBeExpanded(View v) {
        return this.mHeadsUp != null && this.mHeadsUp.row == v && this.mHeadsUp.row.isExpandable();
    }

    @Override
    public void setUserExpandedChild(View v, boolean userExpanded) {
        if (this.mHeadsUp != null && this.mHeadsUp.row == v) {
            this.mHeadsUp.row.setUserExpanded(userExpanded);
        }
    }

    @Override
    public void setUserLockedChild(View v, boolean userLocked) {
        if (this.mHeadsUp != null && this.mHeadsUp.row == v) {
            this.mHeadsUp.row.setUserLocked(userLocked);
        }
    }

    @Override
    public void expansionStateChanged(boolean isExpanding) {
    }

    @Override
    public boolean canChildBeDismissed(View v) {
        return true;
    }

    @Override
    public boolean isAntiFalsingNeeded() {
        return false;
    }

    @Override
    public float getFalsingThresholdFactor() {
        return 1.0f;
    }

    @Override
    public void onChildDismissed(View v) {
        Log.v("HeadsUpNotificationView", "User swiped heads up to dismiss");
        this.mBar.onHeadsUpDismissed();
    }

    @Override
    public void onBeginDrag(View v) {
    }

    @Override
    public void onDragCancelled(View v) {
        this.mContentHolder.setAlpha(1.0f);
    }

    @Override
    public void onChildSnappedBack(View animView) {
    }

    @Override
    public boolean updateSwipeProgress(View animView, boolean dismissable, float swipeProgress) {
        getBackground().setAlpha((int) (255.0f * swipeProgress));
        return false;
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        return this.mContentHolder;
    }

    @Override
    public View getChildContentView(View v) {
        return this.mContentHolder;
    }

    public void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo info) {
        this.mContentHolder.getLocationOnScreen(this.mTmpTwoArray);
        info.setTouchableInsets(3);
        info.touchableRegion.set(this.mTmpTwoArray[0], this.mTmpTwoArray[1], this.mTmpTwoArray[0] + this.mContentHolder.getWidth(), this.mTmpTwoArray[1] + this.mContentHolder.getHeight());
    }

    public String getKey() {
        if (this.mHeadsUp == null) {
            return null;
        }
        return this.mHeadsUp.notification.getKey();
    }

    public void setUser(int user) {
        this.mUser = user;
    }

    private class EdgeSwipeHelper {
        private boolean mConsuming;
        private float mFirstX;
        private float mFirstY;
        private final float mTouchSlop;

        public EdgeSwipeHelper(float touchSlop) {
            this.mTouchSlop = touchSlop;
        }

        public boolean onInterceptTouchEvent(MotionEvent ev) {
            switch (ev.getActionMasked()) {
                case 0:
                    this.mFirstX = ev.getX();
                    this.mFirstY = ev.getY();
                    this.mConsuming = false;
                    break;
                case 1:
                case 3:
                    this.mConsuming = false;
                    break;
                case 2:
                    float dY = ev.getY() - this.mFirstY;
                    float daX = Math.abs(ev.getX() - this.mFirstX);
                    float daY = Math.abs(dY);
                    if (!this.mConsuming && daX < daY && daY > this.mTouchSlop) {
                        HeadsUpNotificationView.this.snooze();
                        if (dY > 0.0f) {
                            HeadsUpNotificationView.this.mBar.animateExpandNotificationsPanel();
                        }
                        this.mConsuming = true;
                    }
                    break;
            }
            return this.mConsuming;
        }

        public boolean onTouchEvent(MotionEvent ev) {
            return this.mConsuming;
        }
    }
}
