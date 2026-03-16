package com.android.keyguard;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.CameraWidgetFrame;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardWidgetPager;
import java.lang.ref.WeakReference;

public class KeyguardHostView extends KeyguardViewBase {
    public static boolean DEBUG = false;
    public static boolean DEBUGXPORT = true;
    private final int MAX_WIDGETS;
    private KeyguardWidgetPager mAppWidgetContainer;
    private AppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private int mAppWidgetToShow;
    private boolean mCameraDisabled;
    private final CameraWidgetFrame.Callbacks mCameraWidgetCallbacks;
    private int mDisabledFeatures;
    private final Rect mInsets;
    private KeyguardMultiUserSelectorView mKeyguardMultiUserSelectorView;
    private MultiPaneChallengeLayout mMultiPaneChallengeLayout;
    private MyOnClickHandler mOnClickHandler;
    private Runnable mPostBootCompletedRunnable;
    private boolean mSafeModeEnabled;
    protected boolean mShowSecurityWhenReturn;
    private SlidingChallengeLayout mSlidingChallengeLayout;
    private final Runnable mSwitchPageRunnable;
    private Rect mTempRect;
    private KeyguardTransportControlView mTransportControl;
    private int mTransportState;
    private KeyguardUpdateMonitorCallback mUpdateMonitorCallbacks;
    private final int mUserId;
    private boolean mUserSetupCompleted;
    private KeyguardViewStateManager mViewStateManager;
    private KeyguardWidgetPager.Callbacks mWidgetCallbacks;

    public interface OnDismissAction {
        boolean onDismiss();
    }

    interface TransportControlCallback {
        void userActivity();
    }

    interface UserSwitcherCallback {
        void hideSecurityView(int i);

        void showUnlockHint();

        void userActivity();
    }

    public KeyguardHostView(Context context) {
        this(context, null);
    }

    public KeyguardHostView(Context context, AttributeSet attrs) {
        Context userContext;
        super(context, attrs);
        this.mTransportState = 0;
        this.MAX_WIDGETS = 5;
        this.mTempRect = new Rect();
        this.mInsets = new Rect();
        this.mOnClickHandler = new MyOnClickHandler(this);
        this.mUpdateMonitorCallbacks = new KeyguardUpdateMonitorCallback() {
            @Override
            public void onBootCompleted() {
                if (KeyguardHostView.this.mPostBootCompletedRunnable != null) {
                    KeyguardHostView.this.mPostBootCompletedRunnable.run();
                    KeyguardHostView.this.mPostBootCompletedRunnable = null;
                }
            }

            @Override
            public void onUserSwitchComplete(int userId) {
                if (KeyguardHostView.this.mKeyguardMultiUserSelectorView != null) {
                    KeyguardHostView.this.mKeyguardMultiUserSelectorView.finalizeActiveUserView(true);
                }
            }
        };
        this.mWidgetCallbacks = new KeyguardWidgetPager.Callbacks() {
            @Override
            public void userActivity() {
                KeyguardHostView.this.userActivity();
            }

            @Override
            public void onUserActivityTimeoutChanged() {
                KeyguardHostView.this.onUserActivityTimeoutChanged();
            }

            @Override
            public void onAddView(View v) {
                if (!KeyguardHostView.this.shouldEnableAddWidget()) {
                    KeyguardHostView.this.mAppWidgetContainer.setAddWidgetEnabled(false);
                }
            }

            @Override
            public void onRemoveView(View v, boolean deletePermanently) {
                int appWidgetId;
                if (deletePermanently && (appWidgetId = ((KeyguardWidgetFrame) v).getContentAppWidgetId()) != 0 && appWidgetId != -2) {
                    KeyguardHostView.this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                }
            }

            @Override
            public void onRemoveViewAnimationCompleted() {
                if (KeyguardHostView.this.shouldEnableAddWidget()) {
                    KeyguardHostView.this.mAppWidgetContainer.setAddWidgetEnabled(true);
                }
            }
        };
        this.mCameraWidgetCallbacks = new CameraWidgetFrame.Callbacks() {
            @Override
            public void onLaunchingCamera() {
                setSliderHandleAlpha(0.0f);
            }

            @Override
            public void onCameraLaunchedSuccessfully() {
                if (KeyguardHostView.this.mAppWidgetContainer.isCameraPage(KeyguardHostView.this.mAppWidgetContainer.getCurrentPage())) {
                    KeyguardHostView.this.mAppWidgetContainer.scrollLeft();
                }
                setSliderHandleAlpha(1.0f);
                KeyguardHostView.this.mShowSecurityWhenReturn = true;
            }

            @Override
            public void onCameraLaunchedUnsuccessfully() {
                setSliderHandleAlpha(1.0f);
            }

            private void setSliderHandleAlpha(float alpha) {
                SlidingChallengeLayout slider = (SlidingChallengeLayout) KeyguardHostView.this.findViewById(R.id.sliding_layout);
                if (slider != null) {
                    slider.setHandleAlpha(alpha);
                }
            }
        };
        this.mSwitchPageRunnable = new Runnable() {
            @Override
            public void run() {
                KeyguardHostView.this.showAppropriateWidgetPage();
            }
        };
        if (DEBUG) {
            Log.e("KeyguardHostView", "KeyguardHostView()");
        }
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mUserId = this.mLockPatternUtils.getCurrentUser();
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (dpm != null) {
            this.mDisabledFeatures = getDisabledFeatures(dpm);
            this.mCameraDisabled = dpm.getCameraDisabled(null);
        }
        this.mSafeModeEnabled = LockPatternUtils.isSafeModeEnabled();
        try {
            userContext = this.mContext.createPackageContextAsUser("system", 0, new UserHandle(this.mUserId));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            userContext = context;
        }
        this.mAppWidgetHost = new AppWidgetHost(userContext, 1262836039, this.mOnClickHandler, Looper.myLooper());
        this.mAppWidgetManager = AppWidgetManager.getInstance(userContext);
        this.mViewStateManager = new KeyguardViewStateManager(this);
        this.mUserSetupCompleted = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
        getInitialTransportState();
        if (this.mSafeModeEnabled) {
            Log.v("KeyguardHostView", "Keyguard widgets disabled by safe mode");
        }
        if ((this.mDisabledFeatures & 1) != 0) {
            Log.v("KeyguardHostView", "Keyguard widgets disabled by DPM");
        }
        if ((this.mDisabledFeatures & 2) != 0) {
            Log.v("KeyguardHostView", "Keyguard secure camera disabled by DPM");
        }
    }

    private void getInitialTransportState() {
        int i;
        KeyguardUpdateMonitor.DisplayClientState dcs = KeyguardUpdateMonitor.getInstance(this.mContext).getCachedDisplayClientState();
        if (dcs.clearing) {
            i = 0;
        } else {
            i = isMusicPlaying(dcs.playbackState) ? 2 : 1;
        }
        this.mTransportState = i;
        if (DEBUGXPORT) {
            Log.v("KeyguardHostView", "Initial transport state: " + this.mTransportState + ", pbstate=" + dcs.playbackState);
        }
    }

    private void cleanupAppWidgetIds() {
        if (!this.mSafeModeEnabled && !widgetsDisabled()) {
            int[] appWidgetIdsInKeyguardSettings = this.mLockPatternUtils.getAppWidgets();
            int[] appWidgetIdsBoundToHost = this.mAppWidgetHost.getAppWidgetIds();
            for (int appWidgetId : appWidgetIdsBoundToHost) {
                if (!contains(appWidgetIdsInKeyguardSettings, appWidgetId)) {
                    Log.d("KeyguardHostView", "Found a appWidgetId that's not being used by keyguard, deleting id " + appWidgetId);
                    this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                }
            }
        }
    }

    private static boolean contains(int[] array, int target) {
        for (int value : array) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    private static final boolean isMusicPlaying(int playbackState) {
        switch (playbackState) {
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        this.mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(getSecurityContainer(), this.mTempRect);
        ev.offsetLocation(this.mTempRect.left, this.mTempRect.top);
        boolean result2 = getSecurityContainer().dispatchTouchEvent(ev) || result;
        ev.offsetLocation(-this.mTempRect.left, -this.mTempRect.top);
        return result2;
    }

    private int getWidgetPosition(int id) {
        KeyguardWidgetPager appWidgetContainer = this.mAppWidgetContainer;
        int children = appWidgetContainer.getChildCount();
        for (int i = 0; i < children; i++) {
            View content = appWidgetContainer.getWidgetPageAt(i).getContent();
            if (content == null || content.getId() != id) {
                if (content == null) {
                    Log.w("KeyguardHostView", "*** Null content at i=" + i + ",id=" + id + ",N=" + children);
                }
            } else {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View deleteDropTarget = findViewById(R.id.keyguard_widget_pager_delete_target);
        this.mAppWidgetContainer = (KeyguardWidgetPager) findViewById(R.id.app_widget_container);
        this.mAppWidgetContainer.setVisibility(0);
        this.mAppWidgetContainer.setCallbacks(this.mWidgetCallbacks);
        this.mAppWidgetContainer.setDeleteDropTarget(deleteDropTarget);
        this.mAppWidgetContainer.setMinScale(0.5f);
        this.mSlidingChallengeLayout = (SlidingChallengeLayout) findViewById(R.id.sliding_layout);
        if (this.mSlidingChallengeLayout != null) {
            this.mSlidingChallengeLayout.setOnChallengeScrolledListener(this.mViewStateManager);
        }
        this.mAppWidgetContainer.setViewStateManager(this.mViewStateManager);
        this.mAppWidgetContainer.setLockPatternUtils(this.mLockPatternUtils);
        this.mMultiPaneChallengeLayout = (MultiPaneChallengeLayout) findViewById(R.id.multi_pane_challenge);
        ChallengeLayout challenge = this.mSlidingChallengeLayout != null ? this.mSlidingChallengeLayout : this.mMultiPaneChallengeLayout;
        challenge.setOnBouncerStateChangedListener(this.mViewStateManager);
        this.mAppWidgetContainer.setBouncerAnimationDuration(challenge.getBouncerAnimationDuration());
        this.mViewStateManager.setPagedView(this.mAppWidgetContainer);
        this.mViewStateManager.setChallengeLayout(challenge);
        this.mViewStateManager.setSecurityViewContainer(getSecurityContainer());
        if (KeyguardUpdateMonitor.getInstance(this.mContext).hasBootCompleted()) {
            updateAndAddWidgets();
        } else {
            this.mPostBootCompletedRunnable = new Runnable() {
                @Override
                public void run() {
                    KeyguardHostView.this.updateAndAddWidgets();
                }
            };
        }
        getSecurityContainer().updateSecurityViews(this.mViewStateManager.isBouncing());
        enableUserSelectorIfNecessary();
    }

    private void updateAndAddWidgets() {
        cleanupAppWidgetIds();
        addDefaultWidgets();
        addWidgetsFromSettings();
        maybeEnableAddButton();
        checkAppWidgetConsistency();
        if (this.mSlidingChallengeLayout != null) {
            this.mSlidingChallengeLayout.setEnableChallengeDragging(!widgetsDisabled());
        }
        this.mSwitchPageRunnable.run();
        this.mViewStateManager.showUsabilityHints();
    }

    private void maybeEnableAddButton() {
        if (!shouldEnableAddWidget()) {
            this.mAppWidgetContainer.setAddWidgetEnabled(false);
        }
    }

    private boolean shouldEnableAddWidget() {
        return numWidgets() < 5 && this.mUserSetupCompleted;
    }

    @Override
    public boolean dismiss(boolean authenticated) {
        boolean finished = super.dismiss(authenticated);
        if (!finished) {
            this.mViewStateManager.showBouncer(true);
            KeyguardSecurityModel.SecurityMode securityMode = getSecurityContainer().getSecurityMode();
            boolean isFullScreen = getResources().getBoolean(R.bool.kg_sim_puk_account_full_screen);
            boolean isSimOrAccount = securityMode == KeyguardSecurityModel.SecurityMode.SimPin || securityMode == KeyguardSecurityModel.SecurityMode.SimPuk || securityMode == KeyguardSecurityModel.SecurityMode.Account;
            this.mAppWidgetContainer.setVisibility((isSimOrAccount && isFullScreen) ? 8 : 0);
            setSystemUiVisibility(isSimOrAccount ? getSystemUiVisibility() | 33554432 : getSystemUiVisibility() & (-33554433));
            if (this.mSlidingChallengeLayout != null) {
                this.mSlidingChallengeLayout.setChallengeInteractive(isFullScreen ? false : true);
            }
        }
        return finished;
    }

    private int getDisabledFeatures(DevicePolicyManager dpm) {
        if (dpm == null) {
            return 0;
        }
        int currentUser = this.mLockPatternUtils.getCurrentUser();
        int disabledFeatures = dpm.getKeyguardDisabledFeatures(null, currentUser);
        return disabledFeatures;
    }

    private boolean widgetsDisabled() {
        boolean disabledByLowRamDevice = ActivityManager.isLowRamDeviceStatic();
        boolean disabledByDpm = (this.mDisabledFeatures & 1) != 0;
        boolean disabledByUser = !this.mLockPatternUtils.getWidgetsEnabled();
        return disabledByLowRamDevice || disabledByDpm || disabledByUser;
    }

    private boolean cameraDisabledByDpm() {
        return this.mCameraDisabled || (this.mDisabledFeatures & 2) != 0;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        super.setLockPatternUtils(utils);
        getSecurityContainer().updateSecurityViews(this.mViewStateManager.isBouncing());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAppWidgetHost.startListening();
        KeyguardUpdateMonitor.getInstance(this.mContext).registerCallback(this.mUpdateMonitorCallbacks);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mAppWidgetHost.stopListening();
        KeyguardUpdateMonitor.getInstance(this.mContext).removeCallback(this.mUpdateMonitorCallbacks);
    }

    void addWidget(AppWidgetHostView view, int pageIndex) {
        this.mAppWidgetContainer.addWidget(view, pageIndex);
    }

    @Override
    public void userActivity() {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.userActivity();
        }
    }

    @Override
    public void onUserActivityTimeoutChanged() {
        if (this.mViewMediatorCallback != null) {
            this.mViewMediatorCallback.onUserActivityTimeoutChanged();
        }
    }

    @Override
    public long getUserActivityTimeout() {
        if (this.mAppWidgetContainer != null) {
            return this.mAppWidgetContainer.getUserActivityTimeout();
        }
        return -1L;
    }

    private static class MyOnClickHandler extends RemoteViews.OnClickHandler {
        WeakReference<KeyguardHostView> mKeyguardHostView;

        MyOnClickHandler(KeyguardHostView hostView) {
            this.mKeyguardHostView = new WeakReference<>(hostView);
        }

        public boolean onClickHandler(final View view, final PendingIntent pendingIntent, final Intent fillInIntent) {
            KeyguardHostView hostView = this.mKeyguardHostView.get();
            if (hostView == null) {
                return false;
            }
            if (pendingIntent.isActivity()) {
                hostView.setOnDismissAction(new OnDismissAction() {
                    @Override
                    public boolean onDismiss() {
                        try {
                            Context context = view.getContext();
                            ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                            context.startIntentSender(pendingIntent.getIntentSender(), fillInIntent, 268435456, 268435456, 0, opts.toBundle());
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("KeyguardHostView", "Cannot send pending intent: ", e);
                        } catch (Exception e2) {
                            Log.e("KeyguardHostView", "Cannot send pending intent due to unknown exception: ", e2);
                        }
                        return false;
                    }
                });
                if (hostView.mViewStateManager.isChallengeShowing()) {
                    hostView.mViewStateManager.showBouncer(true);
                    return true;
                }
                hostView.dismiss();
                return true;
            }
            return super.onClickHandler(view, pendingIntent, fillInIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mViewStateManager != null) {
            this.mViewStateManager.showUsabilityHints();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        clearAppWidgetToShow();
        if (KeyguardUpdateMonitor.getInstance(this.mContext).hasBootCompleted()) {
            checkAppWidgetConsistency();
        }
        CameraWidgetFrame cameraPage = findCameraPage();
        if (cameraPage != null) {
            cameraPage.onScreenTurnedOff();
        }
    }

    public void clearAppWidgetToShow() {
        this.mAppWidgetToShow = 0;
    }

    private boolean addWidget(int appId, int pageIndex, boolean updateDbIfFailed) {
        AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(appId);
        if (appWidgetInfo != null) {
            AppWidgetHostView view = this.mAppWidgetHost.createView(this.mContext, appId, appWidgetInfo);
            addWidget(view, pageIndex);
            return true;
        }
        if (updateDbIfFailed) {
            Log.w("KeyguardHostView", "*** AppWidgetInfo for app widget id " + appId + "  was null for user" + this.mUserId + ", deleting");
            this.mAppWidgetHost.deleteAppWidgetId(appId);
            this.mLockPatternUtils.removeAppWidget(appId);
        }
        return false;
    }

    private int numWidgets() {
        int childCount = this.mAppWidgetContainer.getChildCount();
        int widgetCount = 0;
        for (int i = 0; i < childCount; i++) {
            if (this.mAppWidgetContainer.isWidgetPage(i)) {
                widgetCount++;
            }
        }
        return widgetCount;
    }

    private void addDefaultWidgets() {
        View cameraWidget;
        if (!this.mSafeModeEnabled && !widgetsDisabled()) {
            LayoutInflater inflater = LayoutInflater.from(this.mContext);
            View addWidget = inflater.inflate(R.layout.keyguard_add_widget, (ViewGroup) this, false);
            this.mAppWidgetContainer.addWidget(addWidget, 0);
            View addWidgetButton = addWidget.findViewById(R.id.keyguard_add_widget_view);
            addWidgetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    KeyguardHostView.this.getActivityLauncher().launchWidgetPicker(0);
                }
            });
        }
        if (!this.mSafeModeEnabled && !cameraDisabledByDpm() && this.mUserSetupCompleted && this.mContext.getResources().getBoolean(R.bool.kg_enable_camera_default_widget) && (cameraWidget = CameraWidgetFrame.create(this.mContext, this.mCameraWidgetCallbacks, getActivityLauncher())) != null) {
            this.mAppWidgetContainer.addWidget(cameraWidget);
        }
    }

    private KeyguardTransportControlView getOrCreateTransportControl() {
        if (this.mTransportControl == null) {
            LayoutInflater inflater = LayoutInflater.from(this.mContext);
            this.mTransportControl = (KeyguardTransportControlView) inflater.inflate(R.layout.keyguard_transport_control_view, (ViewGroup) this, false);
            this.mTransportControl.setTransportControlCallback(new TransportControlCallback() {
                @Override
                public void userActivity() {
                    KeyguardHostView.this.mViewMediatorCallback.userActivity();
                }
            });
        }
        return this.mTransportControl;
    }

    private int getInsertPageIndex() {
        View addWidget = this.mAppWidgetContainer.findViewById(R.id.keyguard_add_widget);
        int insertionIndex = this.mAppWidgetContainer.indexOfChild(addWidget);
        if (insertionIndex < 0) {
            return 0;
        }
        return insertionIndex + 1;
    }

    private void addDefaultStatusWidget(int index) {
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        View statusWidget = inflater.inflate(R.layout.keyguard_status_view, (ViewGroup) null, true);
        this.mAppWidgetContainer.addWidget(statusWidget, index);
    }

    private void addWidgetsFromSettings() {
        if (this.mSafeModeEnabled || widgetsDisabled()) {
            addDefaultStatusWidget(0);
            return;
        }
        int insertionIndex = getInsertPageIndex();
        int[] widgets = this.mLockPatternUtils.getAppWidgets();
        if (widgets == null) {
            Log.d("KeyguardHostView", "Problem reading widgets");
            return;
        }
        for (int i = widgets.length - 1; i >= 0; i--) {
            if (widgets[i] == -2) {
                addDefaultStatusWidget(insertionIndex);
            } else {
                addWidget(widgets[i], insertionIndex, true);
            }
        }
    }

    private int allocateIdForDefaultAppWidget() {
        Resources res = getContext().getResources();
        ComponentName defaultAppWidget = new ComponentName(res.getString(R.string.widget_default_package_name), res.getString(R.string.widget_default_class_name));
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        try {
            this.mAppWidgetManager.bindAppWidgetId(appWidgetId, defaultAppWidget);
            return appWidgetId;
        } catch (IllegalArgumentException e) {
            Log.e("KeyguardHostView", "Error when trying to bind default AppWidget: " + e);
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            return 0;
        }
    }

    public void checkAppWidgetConsistency() {
        int childCount = this.mAppWidgetContainer.getChildCount();
        boolean widgetPageExists = false;
        int i = 0;
        while (true) {
            if (i >= childCount) {
                break;
            }
            if (!this.mAppWidgetContainer.isWidgetPage(i)) {
                i++;
            } else {
                widgetPageExists = true;
                break;
            }
        }
        if (!widgetPageExists) {
            int insertPageIndex = getInsertPageIndex();
            boolean userAddedWidgetsEnabled = !widgetsDisabled();
            boolean addedDefaultAppWidget = false;
            if (!this.mSafeModeEnabled) {
                if (userAddedWidgetsEnabled) {
                    int appWidgetId = allocateIdForDefaultAppWidget();
                    if (appWidgetId != 0) {
                        addedDefaultAppWidget = addWidget(appWidgetId, insertPageIndex, true);
                    }
                } else {
                    int appWidgetId2 = this.mLockPatternUtils.getFallbackAppWidgetId();
                    if (appWidgetId2 == 0 && (appWidgetId2 = allocateIdForDefaultAppWidget()) != 0) {
                        this.mLockPatternUtils.writeFallbackAppWidgetId(appWidgetId2);
                    }
                    if (appWidgetId2 != 0 && !(addedDefaultAppWidget = addWidget(appWidgetId2, insertPageIndex, false))) {
                        this.mAppWidgetHost.deleteAppWidgetId(appWidgetId2);
                        this.mLockPatternUtils.writeFallbackAppWidgetId(0);
                    }
                }
            }
            if (!addedDefaultAppWidget) {
                addDefaultStatusWidget(insertPageIndex);
            }
            if (!this.mSafeModeEnabled && userAddedWidgetsEnabled) {
                this.mAppWidgetContainer.onAddView(this.mAppWidgetContainer.getChildAt(insertPageIndex), insertPageIndex);
            }
        }
    }

    static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int appWidgetToShow;
        Rect insets;
        int transportState;

        SavedState(Parcelable superState) {
            super(superState);
            this.appWidgetToShow = 0;
            this.insets = new Rect();
        }

        private SavedState(Parcel in) {
            super(in);
            this.appWidgetToShow = 0;
            this.insets = new Rect();
            this.transportState = in.readInt();
            this.appWidgetToShow = in.readInt();
            this.insets = (Rect) in.readParcelable(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.transportState);
            out.writeInt(this.appWidgetToShow);
            out.writeParcelable(this.insets, 0);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        if (DEBUG) {
            Log.d("KeyguardHostView", "onSaveInstanceState, tstate=" + this.mTransportState);
        }
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        boolean showing = this.mTransportControl != null && this.mAppWidgetContainer.getWidgetPageIndex(this.mTransportControl) >= 0;
        ss.transportState = showing ? 2 : this.mTransportState;
        ss.appWidgetToShow = this.mAppWidgetToShow;
        ss.insets.set(this.mInsets);
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mTransportState = ss.transportState;
        this.mAppWidgetToShow = ss.appWidgetToShow;
        setInsets(ss.insets);
        if (DEBUG) {
            Log.d("KeyguardHostView", "onRestoreInstanceState, transport=" + this.mTransportState);
        }
        this.mSwitchPageRunnable.run();
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        setInsets(insets);
        return true;
    }

    private void setInsets(Rect insets) {
        this.mInsets.set(insets);
        if (this.mSlidingChallengeLayout != null) {
            this.mSlidingChallengeLayout.setInsets(this.mInsets);
        }
        if (this.mMultiPaneChallengeLayout != null) {
            this.mMultiPaneChallengeLayout.setInsets(this.mInsets);
        }
        CameraWidgetFrame cameraWidget = findCameraPage();
        if (cameraWidget != null) {
            cameraWidget.setInsets(this.mInsets);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (DEBUG) {
            Log.d("KeyguardHostView", "Window is " + (hasWindowFocus ? "focused" : "unfocused"));
        }
        if (hasWindowFocus && this.mShowSecurityWhenReturn) {
            SlidingChallengeLayout slider = (SlidingChallengeLayout) findViewById(R.id.sliding_layout);
            if (slider != null) {
                slider.setHandleAlpha(1.0f);
                slider.showChallenge(true);
            }
            this.mShowSecurityWhenReturn = false;
        }
    }

    private void showAppropriateWidgetPage() {
        int state = this.mTransportState;
        boolean transportAdded = ensureTransportPresentOrRemoved(state);
        final int pageToShow = getAppropriateWidgetPage(state);
        if (!transportAdded) {
            this.mAppWidgetContainer.setCurrentPage(pageToShow);
        } else if (state == 2) {
            post(new Runnable() {
                @Override
                public void run() {
                    KeyguardHostView.this.mAppWidgetContainer.setCurrentPage(pageToShow);
                }
            });
        }
    }

    private boolean ensureTransportPresentOrRemoved(int state) {
        boolean showing = getWidgetPosition(R.id.keyguard_transport_control) != -1;
        boolean visible = state == 2;
        boolean shouldBeVisible = state == 1 && isMusicPlaying(state);
        if (!showing && (visible || shouldBeVisible)) {
            int lastWidget = this.mAppWidgetContainer.getChildCount() - 1;
            int position = 0;
            if (lastWidget >= 0) {
                position = this.mAppWidgetContainer.isCameraPage(lastWidget) ? lastWidget : lastWidget + 1;
            }
            if (DEBUGXPORT) {
                Log.v("KeyguardHostView", "add transport at " + position);
            }
            this.mAppWidgetContainer.addWidget(getOrCreateTransportControl(), position);
            return true;
        }
        if (showing && state == 0) {
            if (DEBUGXPORT) {
                Log.v("KeyguardHostView", "remove transport");
            }
            this.mAppWidgetContainer.removeWidget(getOrCreateTransportControl());
            this.mTransportControl = null;
            KeyguardUpdateMonitor.getInstance(getContext()).dispatchSetBackground(null);
        }
        return false;
    }

    private CameraWidgetFrame findCameraPage() {
        for (int i = this.mAppWidgetContainer.getChildCount() - 1; i >= 0; i--) {
            if (this.mAppWidgetContainer.isCameraPage(i)) {
                return (CameraWidgetFrame) this.mAppWidgetContainer.getChildAt(i);
            }
        }
        return null;
    }

    private int getAppropriateWidgetPage(int musicTransportState) {
        if (this.mAppWidgetToShow != 0) {
            int childCount = this.mAppWidgetContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (this.mAppWidgetContainer.getWidgetPageAt(i).getContentAppWidgetId() == this.mAppWidgetToShow) {
                    return i;
                }
            }
            this.mAppWidgetToShow = 0;
        }
        if (musicTransportState == 2) {
            if (DEBUG) {
                Log.d("KeyguardHostView", "Music playing, show transport");
            }
            int i2 = this.mAppWidgetContainer.getWidgetPageIndex(getOrCreateTransportControl());
            return i2;
        }
        int rightMost = this.mAppWidgetContainer.getChildCount() - 1;
        if (this.mAppWidgetContainer.isCameraPage(rightMost)) {
            rightMost--;
        }
        if (DEBUG) {
            Log.d("KeyguardHostView", "Show right-most page " + rightMost);
        }
        int i3 = rightMost;
        return i3;
    }

    private void enableUserSelectorIfNecessary() {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        if (um == null) {
            Throwable t = new Throwable();
            t.fillInStackTrace();
            Log.e("KeyguardHostView", "user service is null.", t);
            return;
        }
        if (um.isUserSwitcherEnabled()) {
            View multiUserView = findViewById(R.id.keyguard_user_selector);
            if (multiUserView == null) {
                if (DEBUG) {
                    Log.d("KeyguardHostView", "can't find user_selector in layout.");
                }
            } else {
                if (multiUserView instanceof KeyguardMultiUserSelectorView) {
                    this.mKeyguardMultiUserSelectorView = (KeyguardMultiUserSelectorView) multiUserView;
                    this.mKeyguardMultiUserSelectorView.setVisibility(0);
                    this.mKeyguardMultiUserSelectorView.addUsers(um.getUsers(true));
                    UserSwitcherCallback callback = new UserSwitcherCallback() {
                        @Override
                        public void hideSecurityView(int duration) {
                            KeyguardHostView.this.getSecurityContainer().animate().alpha(0.0f).setDuration(duration);
                        }

                        @Override
                        public void showUnlockHint() {
                            if (KeyguardHostView.this.getSecurityContainer() != null) {
                                KeyguardHostView.this.getSecurityContainer().showUsabilityHint();
                            }
                        }

                        @Override
                        public void userActivity() {
                            if (KeyguardHostView.this.mViewMediatorCallback != null) {
                                KeyguardHostView.this.mViewMediatorCallback.userActivity();
                            }
                        }
                    };
                    this.mKeyguardMultiUserSelectorView.setCallback(callback);
                    return;
                }
                Throwable t2 = new Throwable();
                t2.fillInStackTrace();
                if (multiUserView == null) {
                    Log.e("KeyguardHostView", "could not find the user_selector.", t2);
                } else {
                    Log.e("KeyguardHostView", "user_selector is the wrong type.", t2);
                }
            }
        }
    }

    @Override
    public void cleanUp() {
        int count = this.mAppWidgetContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            KeyguardWidgetFrame frame = this.mAppWidgetContainer.getWidgetPageAt(i);
            frame.removeAllViews();
        }
        getSecurityContainer().onPause();
    }
}
