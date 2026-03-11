package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DragLayer;
import com.android.launcher3.DropTarget;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.PagedView;
import com.android.launcher3.SearchDropTargetBar;
import com.android.launcher3.Workspace;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.DefaultAppSearchController;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.model.WidgetsModel;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.WidgetsContainerView;
import com.mediatek.launcher3.LauncherHelper;
import com.mediatek.launcher3.LauncherLog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Launcher extends Activity implements View.OnClickListener, View.OnLongClickListener, LauncherModel.Callbacks, View.OnTouchListener, PagedView.PageSwitchListener, LauncherProviderChangeListener {
    private static PendingAddArguments sPendingAddItem;
    private View mAllAppsButton;
    private LauncherAppWidgetHost mAppWidgetHost;
    private AppWidgetManagerCompat mAppWidgetManager;
    AllAppsContainerView mAppsView;
    private long mAutoAdvanceSentTime;
    private LauncherClings mClings;
    private DeviceProfile mDeviceProfile;
    private DragController mDragController;
    DragLayer mDragLayer;
    FocusIndicatorView mFocusHandler;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    ImageView mFolderIconImageView;
    private View.OnTouchListener mHapticFeedbackTouchListener;
    Hotseat mHotseat;
    private IconCache mIconCache;
    private LayoutInflater mInflater;
    private boolean mIsSafeModeEnabled;
    private LauncherCallbacks mLauncherCallbacks;
    private View mLauncherView;
    private LauncherModel mModel;
    private boolean mMoveToDefaultScreenFromNewIntent;
    private boolean mOnResumeNeedsLoad;
    private ViewGroup mOverviewPanel;
    private View mPageIndicators;
    private LauncherAppWidgetProviderInfo mPendingAddWidgetInfo;
    private AppWidgetHostView mQsb;
    private boolean mRestoring;
    private Bundle mSavedInstanceState;
    private Bundle mSavedState;
    private SearchDropTargetBar mSearchDropTargetBar;
    private SharedPreferences mSharedPrefs;
    LauncherStateTransitionAnimation mStateTransitionAnimation;
    private Stats mStats;
    ArrayList<AppInfo> mTmpAppsList;
    private boolean mWaitingForResult;
    private BubbleTextView mWaitingForResume;
    private View mWidgetsButton;
    WidgetsModel mWidgetsModel;
    WidgetsContainerView mWidgetsView;
    Workspace mWorkspace;
    Drawable mWorkspaceBackgroundDrawable;
    private static int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    static int NEW_APPS_ANIMATION_DELAY = 500;
    private static LongArrayMap<FolderInfo> sFolders = new LongArrayMap<>();
    static final ArrayList<String> sDumpLogs = new ArrayList<>();
    static Date sDateStamp = new Date();
    static DateFormat sDateFormat = DateFormat.getDateTimeInstance(3, 3);
    static long sRunStart = System.currentTimeMillis();
    protected static HashMap<String, CustomAppWidget> sCustomAppWidgets = new HashMap<>();
    State mState = State.WORKSPACE;
    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();
    PendingAddItemInfo mPendingAddInfo = new PendingAddItemInfo();
    private int mPendingAddWidgetId = -1;
    private int[] mTmpAddItemCellCoordinates = new int[2];
    private boolean mAutoAdvanceRunning = false;
    private State mOnResumeState = State.NONE;
    private SpannableStringBuilder mDefaultKeySsb = null;
    boolean mWorkspaceLoading = true;
    private boolean mPaused = true;
    private ArrayList<Runnable> mBindOnResumeCallbacks = new ArrayList<>();
    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<>();
    boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mHasFocus = false;
    private boolean mAttached = false;
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private long mAutoAdvanceTimeLeft = -1;
    HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new HashMap<>();
    private final int mRestoreScreenOrientationDelay = 500;
    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<>();
    private Rect mRectForFolderAnimation = new Rect();
    Runnable mBuildLayersRunnable = new Runnable() {
        @Override
        public void run() {
            if (Launcher.this.mWorkspace == null) {
                return;
            }
            Launcher.this.mWorkspace.buildPageHardwareLayers();
        }
    };
    private boolean mRotationEnabled = false;
    private Runnable mUpdateOrientationRunnable = new Runnable() {
        @Override
        public void run() {
            Launcher.this.setOrientation();
        }
    };
    private int mCurrentWorkSpaceScreen = -1001;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                LauncherLog.d("Launcher", "ACTION_SCREEN_OFF: mPendingAddInfo = " + Launcher.this.mPendingAddInfo + ", this = " + this);
                Launcher.this.mUserPresent = false;
                Launcher.this.mDragLayer.clearAllResizeFrames();
                Launcher.this.updateAutoAdvanceState();
                if (Launcher.this.mAppsView == null || Launcher.this.mWidgetsView == null || Launcher.this.mPendingAddInfo.container != -1 || Launcher.this.showWorkspace(false)) {
                    return;
                }
                Launcher.this.mAppsView.reset();
                return;
            }
            if (!"android.intent.action.USER_PRESENT".equals(action)) {
                return;
            }
            Launcher.this.mUserPresent = true;
            Launcher.this.updateAutoAdvanceState();
        }
    };
    final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                int i = 0;
                for (View key : Launcher.this.mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(Launcher.this.mWidgetsToAdvance.get(key).autoAdvanceViewId);
                    int delay = i * 250;
                    if (v instanceof Advanceable) {
                        Launcher.this.mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ((Advanceable) v).advance();
                            }
                        }, delay);
                    }
                    i++;
                }
                Launcher.this.sendAdvanceMessage(20000L);
            }
            return true;
        }
    });
    private Runnable mBindAllApplicationsRunnable = new Runnable() {
        @Override
        public void run() {
            Launcher.this.bindAllApplications(Launcher.this.mTmpAppsList);
            Launcher.this.mTmpAppsList = null;
        }
    };
    private Runnable mBindWidgetModelRunnable = new Runnable() {
        @Override
        public void run() {
            Launcher.this.bindWidgetsModel(Launcher.this.mWidgetsModel);
        }
    };

    public interface CustomContentCallbacks {
        boolean isScrollingAllowed();

        void onHide();

        void onScrollProgressChanged(float f);

        void onShow(boolean z);
    }

    public interface LauncherOverlay {
        void onScrollChange(float f, boolean z);

        void onScrollInteractionBegin();

        void onScrollInteractionEnd();
    }

    enum State {
        NONE,
        WORKSPACE,
        APPS,
        APPS_SPRING_LOADED,
        WIDGETS,
        WIDGETS_SPRING_LOADED;

        public static State[] valuesCustom() {
            return values();
        }
    }

    static class PendingAddArguments {
        int appWidgetId;
        int cellX;
        int cellY;
        long container;
        Intent intent;
        int requestCode;
        long screenId;

        PendingAddArguments() {
        }
    }

    void setOrientation() {
        if (this.mRotationEnabled) {
            unlockScreenOrientation(true);
        } else {
            setRequestedOrientation(5);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.preOnCreate();
        }
        super.onCreate(savedInstanceState);
        LauncherAppState app = LauncherAppState.getInstance();
        this.mDeviceProfile = getResources().getConfiguration().orientation == 2 ? app.getInvariantDeviceProfile().landscapeProfile : app.getInvariantDeviceProfile().portraitProfile;
        this.mSharedPrefs = Utilities.getPrefs(this);
        this.mIsSafeModeEnabled = getPackageManager().isSafeMode();
        this.mModel = app.setLauncher(this);
        this.mIconCache = app.getIconCache();
        this.mDragController = new DragController(this);
        this.mInflater = getLayoutInflater();
        this.mStateTransitionAnimation = new LauncherStateTransitionAnimation(this);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "(Launcher)onCreate: savedInstanceState = " + savedInstanceState + ", mModel = " + this.mModel + ", mIconCache = " + this.mIconCache + ", this = " + this);
        }
        this.mStats = new Stats(this);
        this.mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);
        this.mAppWidgetHost = new LauncherAppWidgetHost(this, 1024);
        this.mAppWidgetHost.startListening();
        this.mPaused = false;
        setContentView(R.layout.launcher);
        app.getInvariantDeviceProfile().landscapeProfile.setSearchBarHeight(getSearchBarHeight());
        app.getInvariantDeviceProfile().portraitProfile.setSearchBarHeight(getSearchBarHeight());
        setupViews();
        this.mDeviceProfile.layout(this);
        lockAllApps();
        this.mSavedState = savedInstanceState;
        restoreState(this.mSavedState);
        if (!this.mRestoring) {
            this.mModel.startLoader(this.mWorkspace.getRestorePage());
        }
        this.mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(this.mDefaultKeySsb, 0);
        IntentFilter filter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        registerReceiver(this.mCloseSystemDialogsReceiver, filter);
        this.mRotationEnabled = getResources().getBoolean(R.bool.allow_rotation);
        if (!this.mRotationEnabled) {
            this.mRotationEnabled = Utilities.isAllowRotationPrefEnabled(getApplicationContext());
        }
        setOrientation();
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onCreate(savedInstanceState);
        }
        if (shouldShowIntroScreen()) {
            showIntroScreen();
        } else {
            showFirstRunActivity();
            showFirstRunClings();
        }
    }

    @Override
    public void onSettingsChanged(String settings, boolean value) {
        if (!"pref_allowRotation".equals(settings)) {
            return;
        }
        this.mRotationEnabled = value;
        if (waitUntilResume(this.mUpdateOrientationRunnable, true)) {
            return;
        }
        this.mUpdateOrientationRunnable.run();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onPostCreate(savedInstanceState);
    }

    @Override
    public void onLauncherProviderChange() {
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onLauncherProviderChange();
    }

    protected boolean hasCustomContentToLeft() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.hasCustomContentToLeft();
        }
        return false;
    }

    protected void populateCustomContentContainer() {
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.populateCustomContentContainer();
    }

    public boolean isDraggingEnabled() {
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo info) {
        return (int) info.id;
    }

    private long completeAdd(PendingAddArguments args) {
        long screenId = args.screenId;
        if (args.container == -100) {
            screenId = ensurePendingDropLayoutExists(args.screenId);
        }
        switch (args.requestCode) {
            case PackageInstallerCompat.STATUS_INSTALLING:
                completeAddShortcut(args.intent, args.container, screenId, args.cellX, args.cellY);
                break;
            case 5:
                completeAddAppWidget(args.appWidgetId, args.container, screenId, null, null);
                break;
            case 12:
                completeRestoreAppWidget(args.appWidgetId);
                break;
        }
        resetAddInfo();
        return screenId;
    }

    private void handleActivityResult(int requestCode, final int resultCode, Intent data) {
        setWaitingForResult(false);
        int pendingAddWidgetId = this.mPendingAddWidgetId;
        this.mPendingAddWidgetId = -1;
        Runnable exitSpringLoaded = new Runnable() {
            @Override
            public void run() {
                Launcher.this.exitSpringLoadedDragModeDelayed(resultCode != 0, 300, null);
            }
        };
        if (requestCode == 11) {
            int appWidgetId = data != null ? data.getIntExtra("appWidgetId", -1) : -1;
            if (resultCode == 0) {
                completeTwoStageWidgetDrop(0, appWidgetId);
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded, 500, false);
                return;
            } else {
                if (resultCode == -1) {
                    addAppWidgetImpl(appWidgetId, this.mPendingAddInfo, null, this.mPendingAddWidgetInfo, 500);
                    getOrCreateQsbBar();
                    return;
                }
                return;
            }
        }
        if (requestCode == 10) {
            if (resultCode == -1 && this.mWorkspace.isInOverviewMode()) {
                this.mWorkspace.setCurrentPage(this.mWorkspace.getPageNearestToCenterOfScreen());
                showWorkspace(false);
                return;
            }
            return;
        }
        boolean isWidgetDrop = requestCode == 9 || requestCode == 5;
        boolean workspaceLocked = isWorkspaceLocked();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode + ", data = " + data + ", mPendingAddInfo = " + this.mPendingAddInfo);
        }
        if (!isWidgetDrop) {
            if (requestCode == 12) {
                if (resultCode == -1) {
                    PendingAddArguments args = preparePendingAddArgs(requestCode, data, pendingAddWidgetId, this.mPendingAddInfo);
                    if (workspaceLocked) {
                        sPendingAddItem = args;
                        return;
                    } else {
                        completeAdd(args);
                        return;
                    }
                }
                return;
            }
            if (resultCode == -1 && this.mPendingAddInfo.container != -1) {
                PendingAddArguments args2 = preparePendingAddArgs(requestCode, data, pendingAddWidgetId, this.mPendingAddInfo);
                if (isWorkspaceLocked()) {
                    sPendingAddItem = args2;
                } else {
                    completeAdd(args2);
                    this.mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded, 500, false);
                }
            } else if (resultCode == 0) {
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, exitSpringLoaded, 500, false);
            }
            this.mDragLayer.clearAnimatedView();
            return;
        }
        int widgetId = data != null ? data.getIntExtra("appWidgetId", -1) : -1;
        final int appWidgetId2 = widgetId < 0 ? pendingAddWidgetId : widgetId;
        if (appWidgetId2 < 0 || resultCode == 0) {
            Log.e("Launcher", "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the widget configuration activity.");
            completeTwoStageWidgetDrop(0, appWidgetId2);
            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    Launcher.this.exitSpringLoadedDragModeDelayed(false, 0, null);
                }
            };
            if (workspaceLocked) {
                this.mWorkspace.postDelayed(onComplete, 500L);
                return;
            } else {
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, 500, false);
                return;
            }
        }
        if (workspaceLocked) {
            sPendingAddItem = preparePendingAddArgs(requestCode, data, appWidgetId2, this.mPendingAddInfo);
            return;
        }
        if (this.mPendingAddInfo.container == -100) {
            this.mPendingAddInfo.screenId = ensurePendingDropLayoutExists(this.mPendingAddInfo.screenId);
        }
        final CellLayout dropLayout = this.mWorkspace.getScreenWithId(this.mPendingAddInfo.screenId);
        dropLayout.setDropPending(true);
        this.mWorkspace.removeExtraEmptyScreenDelayed(true, new Runnable() {
            @Override
            public void run() {
                Launcher.this.completeTwoStageWidgetDrop(resultCode, appWidgetId2);
                dropLayout.setDropPending(false);
            }
        }, 500, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        handleActivityResult(requestCode, resultCode, data);
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 13 && sPendingAddItem != null && sPendingAddItem.requestCode == 13) {
            View v = null;
            CellLayout layout = getCellLayout(sPendingAddItem.container, sPendingAddItem.screenId);
            if (layout != null) {
                v = layout.getChildAt(sPendingAddItem.cellX, sPendingAddItem.cellY);
            }
            Intent intent = sPendingAddItem.intent;
            sPendingAddItem = null;
            if (grantResults.length > 0 && grantResults[0] == 0) {
                startActivitySafely(v, intent, null);
            } else {
                Toast.makeText(this, getString(R.string.msg_no_phone_permission, new Object[]{getString(R.string.app_name)}), 0).show();
            }
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private PendingAddArguments preparePendingAddArgs(int requestCode, Intent data, int appWidgetId, ItemInfo info) {
        PendingAddArguments args = new PendingAddArguments();
        args.requestCode = requestCode;
        args.intent = data;
        args.container = info.container;
        args.screenId = info.screenId;
        args.cellX = info.cellX;
        args.cellY = info.cellY;
        args.appWidgetId = appWidgetId;
        return args;
    }

    private long ensurePendingDropLayoutExists(long screenId) {
        CellLayout dropLayout = this.mWorkspace.getScreenWithId(screenId);
        if (dropLayout == null) {
            this.mWorkspace.addExtraEmptyScreen();
            return this.mWorkspace.commitExtraEmptyScreen();
        }
        return screenId;
    }

    void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        if (this.mWorkspace == null) {
            LauncherLog.d("Launcher", "completeTwoStageWidgetDrop: mWorkspace = " + this.mWorkspace + ",mPendingAddInfo:" + this.mPendingAddInfo);
            return;
        }
        CellLayout cellLayout = this.mWorkspace.getScreenWithId(this.mPendingAddInfo.screenId);
        Runnable onCompleteRunnable = null;
        int animationType = 0;
        AppWidgetHostView boundWidget = null;
        if (resultCode == -1) {
            animationType = 3;
            final AppWidgetHostView layout = this.mAppWidgetHost.createView((Context) this, appWidgetId, this.mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    Launcher.this.completeAddAppWidget(appWidgetId, Launcher.this.mPendingAddInfo.container, Launcher.this.mPendingAddInfo.screenId, layout, null);
                    Launcher.this.exitSpringLoadedDragModeDelayed(resultCode != 0, 300, null);
                }
            };
        } else if (resultCode == 0) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = 4;
        }
        if (this.mDragLayer.getAnimatedView() != null) {
            this.mWorkspace.animateWidgetDrop(this.mPendingAddInfo, cellLayout, (DragView) this.mDragLayer.getAnimatedView(), onCompleteRunnable, animationType, boundWidget, true);
        } else {
            if (onCompleteRunnable == null) {
                return;
            }
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "(Launcher)onStop: this = " + this);
        }
        FirstFrameAnimatorHelper.setIsVisible(false);
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "(Launcher)onStart: this = " + this);
        }
        FirstFrameAnimatorHelper.setIsVisible(true);
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onStart();
    }

    @Override
    protected void onResume() {
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.preOnResume();
        }
        super.onResume();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "(Launcher)onResume: mRestoring = " + this.mRestoring + ", mOnResumeNeedsLoad = " + this.mOnResumeNeedsLoad + ",mPagesAreRecreated = , this = " + this);
        }
        switch (BenesseExtension.getDchaState()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
            case PackageInstallerCompat.STATUS_INSTALLING:
                Settings.System.putInt(getContentResolver(), "dcha_state", 0);
                Settings.System.putInt(getContentResolver(), "hide_navigation_bar", 0);
                break;
        }
        if (this.mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (this.mOnResumeState == State.APPS) {
            boolean launchedFromApp = this.mWaitingForResume != null;
            showAppsView(false, false, !launchedFromApp, false);
        } else if (this.mOnResumeState == State.WIDGETS) {
            showWidgetsView(false, false);
        }
        this.mOnResumeState = State.NONE;
        setWorkspaceBackground(this.mState == State.WORKSPACE ? 0 : 1);
        this.mPaused = false;
        if (this.mRestoring || this.mOnResumeNeedsLoad) {
            setWorkspaceLoading(true);
            this.mBindOnResumeCallbacks.clear();
            this.mModel.startLoader(-1001);
            this.mRestoring = false;
            this.mOnResumeNeedsLoad = false;
        }
        if (this.mBindOnResumeCallbacks.size() > 0) {
            for (int i = 0; i < this.mBindOnResumeCallbacks.size(); i++) {
                this.mBindOnResumeCallbacks.get(i).run();
            }
            this.mBindOnResumeCallbacks.clear();
        }
        if (this.mOnResumeCallbacks.size() > 0) {
            for (int i2 = 0; i2 < this.mOnResumeCallbacks.size(); i2++) {
                this.mOnResumeCallbacks.get(i2).run();
            }
            this.mOnResumeCallbacks.clear();
        }
        if (this.mWaitingForResume != null) {
            this.mWaitingForResume.setStayPressed(false);
        }
        if (!isWorkspaceLoading()) {
            getWorkspace().reinflateWidgetsIfNecessary();
        }
        reinflateQSBIfNecessary();
        if (this.mWorkspace.getCustomContentCallbacks() != null && !this.mMoveToDefaultScreenFromNewIntent && this.mWorkspace.isOnOrMovingToCustomContent()) {
            this.mWorkspace.getCustomContentCallbacks().onShow(true);
        }
        this.mMoveToDefaultScreenFromNewIntent = false;
        updateInteraction(Workspace.State.NORMAL, this.mWorkspace.getState());
        this.mWorkspace.onResume();
        if (!isWorkspaceLoading()) {
            InstallShortcutReceiver.disableAndFlushInstallQueue(this);
        }
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onResume();
        }
    }

    @Override
    protected void onPause() {
        InstallShortcutReceiver.enableInstallQueue();
        super.onPause();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "(Launcher)onPause: this = " + this);
        }
        this.mPaused = true;
        this.mDragController.cancelDrag();
        this.mDragController.resetLastGestureUpTime();
        if (this.mWorkspace.getCustomContentCallbacks() != null) {
            this.mWorkspace.getCustomContentCallbacks().onHide();
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onPause();
    }

    protected boolean hasSettings() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.hasSettings();
        }
        return !getResources().getBoolean(R.bool.allow_rotation);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onRetainNonConfigurationInstance: mSavedState = " + this.mSavedState + ", mSavedInstanceState = " + this.mSavedInstanceState);
        }
        if (this.mModel.isCurrentCallbacks(this)) {
            this.mModel.stopLoader();
        }
        return Boolean.TRUE;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.mHasFocus = hasFocus;
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onWindowFocusChanged(hasFocus);
    }

    private boolean acceptFilter() {
        InputMethodManager inputManager = (InputMethodManager) getSystemService("input_method");
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int uniChar = event.getUnicodeChar();
        boolean handled = super.onKeyDown(keyCode, event);
        boolean isKeyNotWhitespace = uniChar > 0 && !Character.isWhitespace(uniChar);
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d("Launcher", " onKeyDown: KeyCode = " + keyCode + ", KeyEvent = " + event + ", uniChar = " + uniChar + ", handled = " + handled + ", isKeyNotWhitespace = " + isKeyNotWhitespace);
        }
        if (!handled && acceptFilter() && isKeyNotWhitespace) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(this.mWorkspace, this.mDefaultKeySsb, keyCode, event);
            if (gotKey && this.mDefaultKeySsb != null && this.mDefaultKeySsb.length() > 0) {
                return onSearchRequested();
            }
        }
        if (keyCode == 82 && event.isLongPress()) {
            return true;
        }
        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 82) {
            if (!isOnCustomContent() && !this.mDragController.isDragging()) {
                closeFolder();
                this.mWorkspace.exitWidgetResizeMode();
                if (this.mState == State.WORKSPACE && !this.mWorkspace.isInOverviewMode() && !this.mWorkspace.isSwitchingState()) {
                    this.mOverviewPanel.requestFocus();
                    showOverviewMode(true, true);
                }
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private String getTypedText() {
        return this.mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        this.mDefaultKeySsb.clear();
        this.mDefaultKeySsb.clearSpans();
        Selection.setSelection(this.mDefaultKeySsb, 0);
    }

    private static State intToState(int stateOrdinal) {
        State state = State.WORKSPACE;
        State[] stateValues = State.valuesCustom();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                State state2 = stateValues[i];
                return state2;
            }
        }
        return state;
    }

    private void restoreState(Bundle savedState) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "restoreState: savedState = " + savedState);
        }
        if (savedState == null) {
            return;
        }
        State state = intToState(savedState.getInt("launcher.state", State.WORKSPACE.ordinal()));
        if (state == State.APPS || state == State.WIDGETS) {
            this.mOnResumeState = state;
        }
        int currentScreen = savedState.getInt("launcher.current_screen", -1001);
        if (currentScreen != -1001) {
            this.mWorkspace.setRestorePage(currentScreen);
        }
        this.mCurrentWorkSpaceScreen = currentScreen;
        long pendingAddContainer = savedState.getLong("launcher.add_container", -1L);
        long pendingAddScreen = savedState.getLong("launcher.add_screen", -1L);
        if (pendingAddContainer == -1 || pendingAddScreen <= -1) {
            return;
        }
        this.mPendingAddInfo.container = pendingAddContainer;
        this.mPendingAddInfo.screenId = pendingAddScreen;
        this.mPendingAddInfo.cellX = savedState.getInt("launcher.add_cell_x");
        this.mPendingAddInfo.cellY = savedState.getInt("launcher.add_cell_y");
        this.mPendingAddInfo.spanX = savedState.getInt("launcher.add_span_x");
        this.mPendingAddInfo.spanY = savedState.getInt("launcher.add_span_y");
        this.mPendingAddInfo.componentName = (ComponentName) savedState.getParcelable("launcher.add_component");
        AppWidgetProviderInfo info = (AppWidgetProviderInfo) savedState.getParcelable("launcher.add_widget_info");
        this.mPendingAddWidgetInfo = info == null ? null : LauncherAppWidgetProviderInfo.fromProviderInfo(this, info);
        this.mPendingAddWidgetId = savedState.getInt("launcher.add_widget_id");
        setWaitingForResult(true);
        this.mRestoring = true;
    }

    private void setupViews() {
        DragController dragController = this.mDragController;
        this.mLauncherView = findViewById(R.id.launcher);
        this.mFocusHandler = (FocusIndicatorView) findViewById(R.id.focus_indicator);
        this.mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        this.mWorkspace = (Workspace) this.mDragLayer.findViewById(R.id.workspace);
        this.mWorkspace.setPageSwitchListener(this);
        this.mPageIndicators = this.mDragLayer.findViewById(R.id.page_indicator);
        this.mLauncherView.setSystemUiVisibility(1792);
        this.mWorkspaceBackgroundDrawable = getResources().getDrawable(R.drawable.workspace_bg);
        this.mDragLayer.setup(this, dragController);
        this.mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (this.mHotseat != null) {
            this.mHotseat.setOnLongClickListener(this);
        }
        setupOverviewPanel();
        this.mWorkspace.setHapticFeedbackEnabled(false);
        this.mWorkspace.setOnLongClickListener(this);
        this.mWorkspace.setup(dragController);
        dragController.addDragListener(this.mWorkspace);
        this.mSearchDropTargetBar = (SearchDropTargetBar) this.mDragLayer.findViewById(R.id.search_drop_target_bar);
        this.mAppsView = (AllAppsContainerView) findViewById(R.id.apps_view);
        this.mWidgetsView = (WidgetsContainerView) findViewById(R.id.widgets_view);
        if (this.mLauncherCallbacks != null && this.mLauncherCallbacks.getAllAppsSearchBarController() != null) {
            this.mAppsView.setSearchBarController(this.mLauncherCallbacks.getAllAppsSearchBarController());
        } else {
            this.mAppsView.setSearchBarController(new DefaultAppSearchController());
        }
        dragController.setDragScoller(this.mWorkspace);
        dragController.setScrollView(this.mDragLayer);
        dragController.setMoveTarget(this.mWorkspace);
        dragController.addDropTarget(this.mWorkspace);
        if (this.mSearchDropTargetBar == null) {
            return;
        }
        this.mSearchDropTargetBar.setup(this, dragController);
        this.mSearchDropTargetBar.setQsbSearchBar(getOrCreateQsbBar());
    }

    private void setupOverviewPanel() {
        this.mOverviewPanel = (ViewGroup) findViewById(R.id.overview_panel);
        View.OnLongClickListener performClickOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return v.performClick();
            }
        };
        View wallpaperButton = findViewById(R.id.wallpaper_button);
        wallpaperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Launcher.this.mWorkspace.isSwitchingState()) {
                    return;
                }
                Launcher.this.onClickWallpaperPicker(view);
            }
        });
        wallpaperButton.setOnLongClickListener(performClickOnLongClick);
        wallpaperButton.setOnTouchListener(getHapticFeedbackTouchListener());
        this.mWidgetsButton = findViewById(R.id.widget_button);
        this.mWidgetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Launcher.this.mWorkspace.isSwitchingState()) {
                    return;
                }
                Launcher.this.onClickAddWidgetButton(view);
            }
        });
        this.mWidgetsButton.setOnLongClickListener(performClickOnLongClick);
        this.mWidgetsButton.setOnTouchListener(getHapticFeedbackTouchListener());
        View settingsButton = findViewById(R.id.settings_button);
        boolean hasSettings = hasSettings();
        if (hasSettings) {
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Launcher.this.mWorkspace.isSwitchingState()) {
                        return;
                    }
                    Launcher.this.onClickSettingsButton(view);
                }
            });
            settingsButton.setOnLongClickListener(performClickOnLongClick);
            settingsButton.setOnTouchListener(getHapticFeedbackTouchListener());
        } else {
            settingsButton.setVisibility(8);
        }
        this.mOverviewPanel.setAlpha(0.0f);
    }

    public void setAllAppsButton(View allAppsButton) {
        this.mAllAppsButton = allAppsButton;
    }

    public View getAllAppsButton() {
        return this.mAllAppsButton;
    }

    public View getWidgetsButton() {
        return this.mWidgetsButton;
    }

    View createShortcut(ShortcutInfo info) {
        return createShortcut((ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), info);
    }

    public View createShortcut(ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) this.mInflater.inflate(R.layout.app_icon, parent, false);
        favorite.applyFromShortcutInfo(info, this.mIconCache);
        favorite.setCompoundDrawablePadding(this.mDeviceProfile.iconDrawablePaddingPx);
        favorite.setOnClickListener(this);
        favorite.setOnFocusChangeListener(this.mFocusHandler);
        return favorite;
    }

    private void completeAddShortcut(Intent data, long container, long screenId, int cellX, int cellY) {
        boolean foundCellSpan;
        int[] cellXY = this.mTmpAddItemCellCoordinates;
        int[] touchXY = this.mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screenId);
        ShortcutInfo info = InstallShortcutReceiver.fromShortcutIntent(this, data);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "completeAddShortcut: info = " + info + ", data = " + data + ", container = " + container + ", screenId = " + screenId + ", cellX = " + cellX + ", cellY = " + cellY + ", layout = " + layout);
        }
        if (info == null || this.mPendingAddInfo.componentName == null) {
            return;
        }
        if (!PackageManagerHelper.hasPermissionForActivity(this, info.intent, this.mPendingAddInfo.componentName.getPackageName())) {
            Log.e("Launcher", "Ignoring malicious intent " + info.intent.toUri(0));
            return;
        }
        View view = createShortcut(info);
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
            foundCellSpan = true;
            if (this.mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0.0f, true, null, null)) {
                return;
            }
            DropTarget.DragObject dragObject = new DropTarget.DragObject();
            dragObject.dragInfo = info;
            if (this.mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0.0f, dragObject, true)) {
                return;
            }
        } else if (touchXY != null) {
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], 1, 1, cellXY);
            foundCellSpan = result != null;
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, 1, 1);
        }
        if (!foundCellSpan) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }
        LauncherModel.addItemToDatabase(this, info, container, screenId, cellXY[0], cellXY[1]);
        if (this.mRestoring) {
            return;
        }
        this.mWorkspace.addInScreen(view, container, screenId, cellXY[0], cellXY[1], 1, 1, isWorkspaceLocked());
    }

    void completeAddAppWidget(int appWidgetId, long container, long screenId, AppWidgetHostView hostView, LauncherAppWidgetProviderInfo appWidgetInfo) {
        ItemInfo info = this.mPendingAddInfo;
        if (appWidgetInfo == null) {
            appWidgetInfo = this.mAppWidgetManager.getLauncherAppWidgetInfo(appWidgetId);
        }
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "completeAddAppWidget: appWidgetId = " + appWidgetId + ", container = " + container + ", screenId = " + screenId);
        }
        if (appWidgetInfo.isCustomWidget) {
            appWidgetId = -100;
        }
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = info.spanX;
        launcherInfo.spanY = info.spanY;
        launcherInfo.minSpanX = info.minSpanX;
        launcherInfo.minSpanY = info.minSpanY;
        launcherInfo.user = this.mAppWidgetManager.getUser(appWidgetInfo);
        LauncherModel.addItemToDatabase(this, launcherInfo, container, screenId, info.cellX, info.cellY);
        if (!this.mRestoring) {
            if (hostView == null) {
                launcherInfo.hostView = this.mAppWidgetHost.createView((Context) this, appWidgetId, appWidgetInfo);
            } else {
                launcherInfo.hostView = hostView;
            }
            launcherInfo.hostView.setVisibility(0);
            addAppWidgetToWorkspace(launcherInfo, appWidgetInfo, isWorkspaceLocked());
        }
        resetAddInfo();
    }

    private void addAppWidgetToWorkspace(LauncherAppWidgetInfo item, LauncherAppWidgetProviderInfo appWidgetInfo, boolean insert) {
        item.hostView.setTag(item);
        item.onBindAppWidget(this);
        item.hostView.setFocusable(true);
        item.hostView.setOnFocusChangeListener(this.mFocusHandler);
        if (this.mWorkspace != null) {
            this.mWorkspace.addInScreen(item.hostView, item.container, item.screenId, item.cellX, item.cellY, item.spanX, item.spanY, insert);
        } else {
            LauncherLog.d("Launcher", "error , mWorkspace is null");
        }
        if (item.isCustomWidget()) {
            return;
        }
        addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onAttachedToWindow.");
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.USER_PRESENT");
        registerReceiver(this.mReceiver, filter);
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        this.mAttached = true;
        this.mVisible = true;
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onDetachedFromWindow.");
        }
        this.mVisible = false;
        if (this.mAttached) {
            unregisterReceiver(this.mReceiver);
            this.mAttached = false;
        }
        updateAutoAdvanceState();
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onDetachedFromWindow();
    }

    public void onWindowVisibilityChanged(int visibility) {
        this.mVisible = visibility == 0;
        updateAutoAdvanceState();
        if (!this.mVisible) {
            return;
        }
        if (!this.mWorkspaceLoading) {
            ViewTreeObserver observer = this.mWorkspace.getViewTreeObserver();
            observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                private boolean mStarted = false;

                @Override
                public void onDraw() {
                    if (this.mStarted) {
                        return;
                    }
                    this.mStarted = true;
                    Launcher.this.mWorkspace.postDelayed(Launcher.this.mBuildLayersRunnable, 500L);
                    Launcher.this.mWorkspace.post(new Runnable() {
                        @Override
                        public void run() {
                            if (Launcher.this.mWorkspace == null || Launcher.this.mWorkspace.getViewTreeObserver() == null) {
                                return;
                            }
                            Launcher.this.mWorkspace.getViewTreeObserver().removeOnDrawListener(this);
                        }
                    });
                }
            });
        }
        clearTypedText();
    }

    void sendAdvanceMessage(long delay) {
        this.mHandler.removeMessages(1);
        Message msg = this.mHandler.obtainMessage(1);
        this.mHandler.sendMessageDelayed(msg, delay);
        this.mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    void updateAutoAdvanceState() {
        boolean autoAdvanceRunning = this.mVisible && this.mUserPresent && !this.mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning == this.mAutoAdvanceRunning) {
            return;
        }
        this.mAutoAdvanceRunning = autoAdvanceRunning;
        if (autoAdvanceRunning) {
            long delay = this.mAutoAdvanceTimeLeft != -1 ? this.mAutoAdvanceTimeLeft : 20000L;
            sendAdvanceMessage(delay);
        } else {
            if (!this.mWidgetsToAdvance.isEmpty()) {
                this.mAutoAdvanceTimeLeft = Math.max(0L, 20000 - (System.currentTimeMillis() - this.mAutoAdvanceSentTime));
            }
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(0);
        }
    }

    private void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "addWidgetToAutoAdvanceIfNeeded hostView = " + hostView + ", appWidgetInfo = " + appWidgetInfo);
        }
        if (appWidgetInfo == null || appWidgetInfo.autoAdvanceViewId == -1) {
            return;
        }
        KeyEvent.Callback callbackFindViewById = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
        if (!(callbackFindViewById instanceof Advanceable)) {
            return;
        }
        this.mWidgetsToAdvance.put(hostView, appWidgetInfo);
        ((Advanceable) callbackFindViewById).fyiWillBeAdvancedByHostKThx();
        updateAutoAdvanceState();
    }

    private void removeWidgetToAutoAdvance(View hostView) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "removeWidgetToAutoAdvance hostView = " + hostView);
        }
        if (!this.mWidgetsToAdvance.containsKey(hostView)) {
            return;
        }
        this.mWidgetsToAdvance.remove(hostView);
        updateAutoAdvanceState();
    }

    public void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space;
        Toast.makeText(this, getString(strId), 0).show();
    }

    public DragLayer getDragLayer() {
        return this.mDragLayer;
    }

    public AllAppsContainerView getAppsView() {
        return this.mAppsView;
    }

    public WidgetsContainerView getWidgetsView() {
        return this.mWidgetsView;
    }

    public Workspace getWorkspace() {
        return this.mWorkspace;
    }

    public Hotseat getHotseat() {
        return this.mHotseat;
    }

    public ViewGroup getOverviewPanel() {
        return this.mOverviewPanel;
    }

    public SearchDropTargetBar getSearchDropTargetBar() {
        return this.mSearchDropTargetBar;
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return this.mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return this.mModel;
    }

    protected SharedPreferences getSharedPrefs() {
        return this.mSharedPrefs;
    }

    public DeviceProfile getDeviceProfile() {
        return this.mDeviceProfile;
    }

    public void closeSystemDialogs() {
        getWindow().closeAllPanels();
        setWaitingForResult(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onNewIntent: intent = " + intent);
        }
        Folder openFolder = this.mWorkspace.getOpenFolder();
        boolean alreadyOnHome = this.mHasFocus && (intent.getFlags() & 4194304) != 4194304;
        boolean isActionMain = "android.intent.action.MAIN".equals(intent.getAction());
        if (isActionMain) {
            closeSystemDialogs();
            if (this.mWorkspace == null) {
                return;
            }
            this.mWorkspace.exitWidgetResizeMode();
            closeFolder(alreadyOnHome);
            exitSpringLoadedDragMode();
            if (alreadyOnHome) {
                showWorkspace(true);
            } else {
                this.mOnResumeState = State.WORKSPACE;
            }
            View v = getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            if (!alreadyOnHome && this.mAppsView != null) {
                this.mAppsView.scrollToTop();
            }
            if (!alreadyOnHome && this.mWidgetsView != null) {
                this.mWidgetsView.scrollToTop();
            }
            if (this.mLauncherCallbacks != null) {
                this.mLauncherCallbacks.onHomeIntent();
            }
        }
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onNewIntent(intent);
        }
        if (!isActionMain) {
            return;
        }
        boolean zShouldMoveToDefaultScreenOnHomeIntent = this.mLauncherCallbacks != null ? this.mLauncherCallbacks.shouldMoveToDefaultScreenOnHomeIntent() : true;
        if (!alreadyOnHome || this.mState != State.WORKSPACE || this.mWorkspace.isTouchActive() || openFolder != null || !zShouldMoveToDefaultScreenOnHomeIntent) {
            return;
        }
        this.mMoveToDefaultScreenFromNewIntent = true;
        this.mWorkspace.post(new Runnable() {
            @Override
            public void run() {
                if (Launcher.this.mWorkspace == null) {
                    return;
                }
                Launcher.this.mWorkspace.moveToDefaultScreen(true);
            }
        });
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onRestoreInstanceState: state = " + state + ", mSavedInstanceState = " + this.mSavedInstanceState);
        }
        Iterator page$iterator = this.mSynchronouslyBoundPages.iterator();
        while (page$iterator.hasNext()) {
            int page = ((Integer) page$iterator.next()).intValue();
            this.mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (isWorkspaceLoading() && this.mSavedState != null) {
            outState.putAll(this.mSavedState);
            return;
        }
        if (this.mWorkspace.getChildCount() > 0) {
            outState.putInt("launcher.current_screen", this.mWorkspace.getCurrentPageOffsetFromCustomContent());
        } else {
            outState.putInt("launcher.current_screen", this.mCurrentWorkSpaceScreen);
        }
        super.onSaveInstanceState(outState);
        outState.putInt("launcher.state", this.mState.ordinal());
        closeFolder(false);
        if (this.mPendingAddInfo.container != -1 && this.mPendingAddInfo.screenId > -1 && this.mWaitingForResult) {
            outState.putLong("launcher.add_container", this.mPendingAddInfo.container);
            outState.putLong("launcher.add_screen", this.mPendingAddInfo.screenId);
            outState.putInt("launcher.add_cell_x", this.mPendingAddInfo.cellX);
            outState.putInt("launcher.add_cell_y", this.mPendingAddInfo.cellY);
            outState.putInt("launcher.add_span_x", this.mPendingAddInfo.spanX);
            outState.putInt("launcher.add_span_y", this.mPendingAddInfo.spanY);
            outState.putParcelable("launcher.add_component", this.mPendingAddInfo.componentName);
            outState.putParcelable("launcher.add_widget_info", this.mPendingAddWidgetInfo);
            outState.putInt("launcher.add_widget_id", this.mPendingAddWidgetId);
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "(Launcher)onDestroy: this = " + this);
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(0);
        this.mWorkspace.removeCallbacks(this.mBuildLayersRunnable);
        LauncherAppState app = LauncherAppState.getInstance();
        if (this.mModel.isCurrentCallbacks(this)) {
            this.mModel.stopLoader();
            app.setLauncher(null);
        }
        try {
            this.mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w("Launcher", "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        this.mAppWidgetHost = null;
        this.mWidgetsToAdvance.clear();
        TextKeyListener.getInstance().release();
        unregisterReceiver(this.mCloseSystemDialogsReceiver);
        this.mDragLayer.clearAllResizeFrames();
        ((ViewGroup) this.mWorkspace.getParent()).removeAllViews();
        this.mWorkspace.removeAllWorkspaceScreens();
        this.mWorkspace = null;
        this.mDragController = null;
        PackageInstallerCompat.getInstance(this).onStop();
        LauncherAnimUtils.onDestroyActivity();
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onDestroy();
    }

    public DragController getDragController() {
        return this.mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        onStartForResult(requestCode);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startIntentSenderForResult(IntentSender intent, int requestCode, Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, Bundle options) {
        onStartForResult(requestCode);
        try {
            super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    private void onStartForResult(int requestCode) {
        if (requestCode < 0) {
            return;
        }
        setWaitingForResult(true);
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "startSearch.");
        }
        if (initialQuery == null) {
            initialQuery = getTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }
        Rect sourceBounds = new Rect();
        if (this.mSearchDropTargetBar != null) {
            sourceBounds = this.mSearchDropTargetBar.getSearchBarBounds();
        }
        boolean clearTextImmediately = startSearch(initialQuery, selectInitialQuery, appSearchData, sourceBounds);
        if (clearTextImmediately) {
            clearTypedText();
        }
        showWorkspace(true);
    }

    public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        if (this.mLauncherCallbacks != null && this.mLauncherCallbacks.providesSearch()) {
            return this.mLauncherCallbacks.startSearch(initialQuery, selectInitialQuery, appSearchData, sourceBounds);
        }
        startGlobalSearch(initialQuery, selectInitialQuery, appSearchData, sourceBounds);
        return false;
    }

    private void startGlobalSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
        Bundle appSearchData2;
        SearchManager searchManager = (SearchManager) getSystemService("search");
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
        if (globalSearchActivity == null) {
            Log.w("Launcher", "No global search activity found.");
            return;
        }
        Intent intent = new Intent("android.search.action.GLOBAL_SEARCH");
        intent.addFlags(268435456);
        intent.setComponent(globalSearchActivity);
        if (appSearchData == null) {
            appSearchData2 = new Bundle();
        } else {
            appSearchData2 = new Bundle(appSearchData);
        }
        if (!appSearchData2.containsKey("source")) {
            appSearchData2.putString("source", getPackageName());
        }
        intent.putExtra("app_data", appSearchData2);
        if (!TextUtils.isEmpty(initialQuery)) {
            intent.putExtra("query", initialQuery);
        }
        if (selectInitialQuery) {
            intent.putExtra("select_query", selectInitialQuery);
        }
        intent.setSourceBounds(sourceBounds);
        try {
            int dcha_state = BenesseExtension.getDchaState();
            if (dcha_state != 0) {
                return;
            }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("Launcher", "Global search activity not found: " + globalSearchActivity);
        }
    }

    public boolean isOnCustomContent() {
        return this.mWorkspace.isOnOrMovingToCustomContent();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.onPrepareOptionsMenu(menu);
        }
        return false;
    }

    @Override
    public boolean onSearchRequested() {
        startSearch((String) null, false, (Bundle) null, true);
        return true;
    }

    public boolean isWorkspaceLocked() {
        if (this.mWorkspaceLoading) {
            return true;
        }
        return this.mWaitingForResult;
    }

    public boolean isWorkspaceLoading() {
        return this.mWorkspaceLoading;
    }

    private void setWorkspaceLoading(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        this.mWorkspaceLoading = value;
        if (isLocked == isWorkspaceLocked()) {
            return;
        }
        onWorkspaceLockedChanged();
    }

    private void setWaitingForResult(boolean value) {
        boolean isLocked = isWorkspaceLocked();
        this.mWaitingForResult = value;
        if (isLocked == isWorkspaceLocked()) {
            return;
        }
        onWorkspaceLockedChanged();
    }

    protected void onWorkspaceLockedChanged() {
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onWorkspaceLockedChanged();
    }

    private void resetAddInfo() {
        this.mPendingAddInfo.container = -1L;
        this.mPendingAddInfo.screenId = -1L;
        PendingAddItemInfo pendingAddItemInfo = this.mPendingAddInfo;
        this.mPendingAddInfo.cellY = -1;
        pendingAddItemInfo.cellX = -1;
        PendingAddItemInfo pendingAddItemInfo2 = this.mPendingAddInfo;
        this.mPendingAddInfo.spanY = -1;
        pendingAddItemInfo2.spanX = -1;
        PendingAddItemInfo pendingAddItemInfo3 = this.mPendingAddInfo;
        this.mPendingAddInfo.minSpanY = 1;
        pendingAddItemInfo3.minSpanX = 1;
        this.mPendingAddInfo.dropPos = null;
        this.mPendingAddInfo.componentName = null;
    }

    void addAppWidgetFromDropImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget, LauncherAppWidgetProviderInfo appWidgetInfo) {
        addAppWidgetImpl(appWidgetId, info, boundWidget, appWidgetInfo, 0);
    }

    void addAppWidgetImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget, LauncherAppWidgetProviderInfo appWidgetInfo, int delay) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "addAppWidgetImpl: appWidgetId = " + appWidgetId + ", info = " + info + ", boundWidget = " + boundWidget + ", appWidgetInfo = " + appWidgetInfo + ", delay = " + delay);
        }
        if (appWidgetInfo.configure == null) {
            Runnable onComplete = new Runnable() {
                @Override
                public void run() {
                    Launcher.this.exitSpringLoadedDragModeDelayed(true, 300, null);
                }
            };
            completeAddAppWidget(appWidgetId, info.container, info.screenId, boundWidget, appWidgetInfo);
            this.mWorkspace.removeExtraEmptyScreenDelayed(true, onComplete, delay, false);
        } else {
            this.mPendingAddWidgetInfo = appWidgetInfo;
            this.mPendingAddWidgetId = appWidgetId;
            setWaitingForResult(true);
            this.mAppWidgetManager.startConfigActivity(appWidgetInfo, appWidgetId, this, this.mAppWidgetHost, 5);
        }
    }

    public void addPendingItem(PendingAddItemInfo info, long container, long screenId, int[] cell, int spanX, int spanY) {
        switch (info.itemType) {
            case PackageInstallerCompat.STATUS_INSTALLING:
                processShortcutFromDrop(info.componentName, container, screenId, cell);
                return;
            case PackageInstallerCompat.STATUS_FAILED:
            case 3:
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
            case 4:
            case 5:
                int[] span = {spanX, spanY};
                addAppWidgetFromDrop((PendingAddWidgetInfo) info, container, screenId, cell, span);
                return;
        }
    }

    private void processShortcutFromDrop(ComponentName componentName, long container, long screenId, int[] cell) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "processShortcutFromDrop componentName = " + componentName + ", container = " + container + ", screenId = " + screenId);
        }
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        resetAddInfo();
        this.mPendingAddInfo.container = container;
        this.mPendingAddInfo.screenId = screenId;
        this.mPendingAddInfo.dropPos = null;
        this.mPendingAddInfo.componentName = componentName;
        if (cell != null) {
            this.mPendingAddInfo.cellX = cell[0];
            this.mPendingAddInfo.cellY = cell[1];
        }
        Intent createShortcutIntent = new Intent("android.intent.action.CREATE_SHORTCUT");
        createShortcutIntent.setComponent(componentName);
        Utilities.startActivityForResultSafely(this, createShortcutIntent, 1);
    }

    private void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, long screenId, int[] cell, int[] span) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "addAppWidgetFromDrop: info = " + info + ", container = " + container + ", screenId = " + screenId);
        }
        resetAddInfo();
        PendingAddItemInfo pendingAddItemInfo = this.mPendingAddInfo;
        info.container = container;
        pendingAddItemInfo.container = container;
        PendingAddItemInfo pendingAddItemInfo2 = this.mPendingAddInfo;
        info.screenId = screenId;
        pendingAddItemInfo2.screenId = screenId;
        this.mPendingAddInfo.dropPos = null;
        this.mPendingAddInfo.minSpanX = info.minSpanX;
        this.mPendingAddInfo.minSpanY = info.minSpanY;
        if (cell != null) {
            this.mPendingAddInfo.cellX = cell[0];
            this.mPendingAddInfo.cellY = cell[1];
        }
        if (span != null) {
            this.mPendingAddInfo.spanX = span[0];
            this.mPendingAddInfo.spanY = span[1];
        }
        AppWidgetHostView hostView = info.boundWidget;
        if (hostView != null) {
            getDragLayer().removeView(hostView);
            addAppWidgetFromDropImpl(hostView.getAppWidgetId(), info, hostView, info.info);
            info.boundWidget = null;
            return;
        }
        int appWidgetId = getAppWidgetHost().allocateAppWidgetId();
        Bundle options = info.bindOptions;
        boolean success = this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.info, options);
        if (success) {
            addAppWidgetFromDropImpl(appWidgetId, info, null, info.info);
            return;
        }
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        this.mPendingAddWidgetInfo = info.info;
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_BIND");
        intent.putExtra("appWidgetId", appWidgetId);
        intent.putExtra("appWidgetProvider", info.componentName);
        this.mAppWidgetManager.getUser(this.mPendingAddWidgetInfo).addToIntent(intent, "appWidgetProviderProfile");
        startActivityForResult(intent, 11);
    }

    FolderIcon addFolder(CellLayout layout, long container, long screenId, int cellX, int cellY) {
        FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);
        LauncherModel.addItemToDatabase(this, folderInfo, container, screenId, cellX, cellY);
        sFolders.put(folderInfo.id, folderInfo);
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo, this.mIconCache);
        this.mWorkspace.addInScreen(newFolder, container, screenId, cellX, cellY, 1, 1, isWorkspaceLocked());
        CellLayout parent = this.mWorkspace.getParentCellLayoutForView(newFolder);
        parent.getShortcutsAndWidgets().measureChild(newFolder);
        return newFolder;
    }

    public boolean removeItem(View v, ItemInfo itemInfo, boolean deleteFromDb) {
        if (itemInfo instanceof ShortcutInfo) {
            FolderInfo folderInfo = sFolders.get(itemInfo.container);
            if (folderInfo != null) {
                folderInfo.remove((ShortcutInfo) itemInfo);
            } else {
                this.mWorkspace.removeWorkspaceItem(v);
            }
            if (deleteFromDb) {
                LauncherModel.deleteItemFromDatabase(this, itemInfo);
                return true;
            }
            return true;
        }
        if (itemInfo instanceof FolderInfo) {
            FolderInfo folderInfo2 = (FolderInfo) itemInfo;
            unbindFolder(folderInfo2);
            this.mWorkspace.removeWorkspaceItem(v);
            if (deleteFromDb) {
                LauncherModel.deleteFolderAndContentsFromDatabase(this, folderInfo2);
                return true;
            }
            return true;
        }
        if (itemInfo instanceof LauncherAppWidgetInfo) {
            LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) itemInfo;
            this.mWorkspace.removeWorkspaceItem(v);
            removeWidgetToAutoAdvance(widgetInfo.hostView);
            widgetInfo.hostView = null;
            if (deleteFromDb) {
                deleteWidgetInfo(widgetInfo);
                return true;
            }
            return true;
        }
        return false;
    }

    private void unbindFolder(FolderInfo folder) {
        sFolders.remove(folder.id);
    }

    private void deleteWidgetInfo(final LauncherAppWidgetInfo widgetInfo) {
        final LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
        if (appWidgetHost != null && !widgetInfo.isCustomWidget() && widgetInfo.isWidgetIdValid()) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                public Void doInBackground(Void... args) {
                    appWidgetHost.deleteAppWidgetId(widgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        }
        LauncherModel.deleteItemFromDatabase(this, widgetInfo);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (LauncherLog.DEBUG_KEY) {
            LauncherLog.d("Launcher", "dispatchKeyEvent: keyEvent = " + event);
        }
        if (event.getAction() == 0) {
            switch (event.getKeyCode()) {
                case 3:
                    return true;
                case 25:
                    if (Utilities.isPropertyEnabled("launcher_dump_state")) {
                        dumpState();
                        return true;
                    }
                    break;
            }
        } else if (event.getAction() == 1) {
            switch (event.getKeyCode()) {
                case 3:
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "Back key pressed, mState = " + this.mState + ", mOnResumeState = " + this.mOnResumeState);
        }
        if (this.mLauncherCallbacks != null && this.mLauncherCallbacks.handleBackPressed()) {
            return;
        }
        if (this.mDragController.isDragging()) {
            this.mDragController.cancelDrag();
            return;
        }
        if (isAppsViewVisible()) {
            showWorkspace(true);
            return;
        }
        if (isWidgetsViewVisible()) {
            showOverviewMode(true);
            return;
        }
        if (this.mWorkspace.isInOverviewMode()) {
            showWorkspace(true);
            return;
        }
        if (this.mWorkspace.getOpenFolder() != null) {
            Folder openFolder = this.mWorkspace.getOpenFolder();
            if (openFolder.isEditingName()) {
                openFolder.dismissEditingName();
                return;
            } else {
                closeFolder();
                return;
            }
        }
        this.mWorkspace.exitWidgetResizeMode();
        this.mWorkspace.showOutlinesTemporarily();
    }

    @Override
    public void onAppWidgetHostReset() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onAppWidgetReset.");
        }
        if (this.mAppWidgetHost != null) {
            this.mAppWidgetHost.startListening();
        }
        bindSearchProviderChanged();
    }

    @Override
    public void onClick(View v) {
        LauncherHelper.beginSection("Launcher.onClick");
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "Click on view " + v);
        }
        if (v.getWindowToken() == null) {
            LauncherLog.d("Launcher", "Click on a view with no window token, directly return.");
            return;
        }
        if (!this.mWorkspace.isFinishedSwitchingState()) {
            LauncherLog.d("Launcher", "The workspace is in switching state when clicking on view, directly return.");
            return;
        }
        if (v instanceof Workspace) {
            if (this.mWorkspace.isInOverviewMode()) {
                showWorkspace(true);
                return;
            }
            return;
        }
        if ((v instanceof CellLayout) && this.mWorkspace.isInOverviewMode()) {
            showWorkspace(this.mWorkspace.indexOfChild(v), true);
        }
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            onClickAppShortcut(v);
        } else if (tag instanceof FolderInfo) {
            if (v instanceof FolderIcon) {
                onClickFolderIcon(v);
            }
        } else if (v == this.mAllAppsButton) {
            onClickAllAppsButton(v);
        } else if (tag instanceof AppInfo) {
            startAppShortcutOrInfoActivity(v);
        } else if ((tag instanceof LauncherAppWidgetInfo) && (v instanceof PendingAppWidgetHostView)) {
            onClickPendingWidget((PendingAppWidgetHostView) v);
        }
        LauncherHelper.endSection();
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    public void onClickPendingWidget(final PendingAppWidgetHostView v) {
        if (this.mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, 0).show();
            return;
        }
        final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
        if (v.isReadyForClickSetup()) {
            int widgetId = info.appWidgetId;
            LauncherAppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getLauncherAppWidgetInfo(widgetId);
            if (appWidgetInfo == null) {
                return;
            }
            this.mPendingAddWidgetInfo = appWidgetInfo;
            this.mPendingAddInfo.copyFrom(info);
            this.mPendingAddWidgetId = widgetId;
            AppWidgetManagerCompat.getInstance(this).startConfigActivity(appWidgetInfo, info.appWidgetId, this, this.mAppWidgetHost, 12);
            return;
        }
        if (info.installProgress < 0) {
            final String packageName = info.providerName.getPackageName();
            showBrokenAppInstallDialog(packageName, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Launcher.this.startActivitySafely(v, LauncherModel.getMarketIntent(packageName), info);
                }
            });
        } else {
            startActivitySafely(v, LauncherModel.getMarketIntent(info.providerName.getPackageName()), info);
        }
    }

    protected void onClickAllAppsButton(View v) {
        if (isAppsViewVisible()) {
            return;
        }
        showAppsView(true, false, true, false);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "[All apps launch time][Start] onClickAllAppsButton.");
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onClickAllAppsButton(v);
    }

    protected void onLongClickAllAppsButton(View v) {
        if (isAppsViewVisible()) {
            return;
        }
        showAppsView(true, false, true, false);
    }

    private void showBrokenAppInstallDialog(final String packageName, DialogInterface.OnClickListener onSearchClickListener) {
        new AlertDialog.Builder(this).setTitle(R.string.abandoned_promises_title).setMessage(R.string.abandoned_promise_explanation).setPositiveButton(R.string.abandoned_search, onSearchClickListener).setNeutralButton(R.string.abandoned_clean_this, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                UserHandleCompat user = UserHandleCompat.myUserHandle();
                Launcher.this.mWorkspace.removeAbandonedPromise(packageName, user);
            }
        }).create().show();
    }

    protected void onClickAppShortcut(final View v) {
        Object tag = v.getTag();
        if (!(tag instanceof ShortcutInfo)) {
            throw new IllegalArgumentException("Input must be a Shortcut");
        }
        ShortcutInfo shortcut = (ShortcutInfo) tag;
        if (shortcut.isDisabled != 0 && (shortcut.isDisabled & 4) == 0 && (shortcut.isDisabled & 8) == 0) {
            int error = R.string.activity_not_available;
            if ((shortcut.isDisabled & 1) != 0) {
                error = R.string.safemode_shortcut_error;
            }
            Toast.makeText(this, error, 0).show();
            return;
        }
        if ((v instanceof BubbleTextView) && shortcut.isPromise() && !shortcut.hasStatusFlag(4)) {
            showBrokenAppInstallDialog(shortcut.getTargetComponent().getPackageName(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Launcher.this.startAppShortcutOrInfoActivity(v);
                }
            });
            return;
        }
        startAppShortcutOrInfoActivity(v);
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onClickAppShortcut(v);
    }

    void startAppShortcutOrInfoActivity(View v) {
        ShortcutInfo shortcut;
        Intent intent;
        Object tag = v.getTag();
        if (tag instanceof ShortcutInfo) {
            shortcut = (ShortcutInfo) tag;
            intent = shortcut.intent;
            int[] pos = new int[2];
            v.getLocationOnScreen(pos);
            intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
        } else if (tag instanceof AppInfo) {
            shortcut = null;
            intent = ((AppInfo) tag).intent;
        } else {
            throw new IllegalArgumentException("Input must be a Shortcut or AppInfo");
        }
        boolean success = startActivitySafely(v, intent, tag);
        this.mStats.recordLaunch(v, intent, shortcut);
        if (!success || !(v instanceof BubbleTextView)) {
            return;
        }
        this.mWaitingForResume = (BubbleTextView) v;
        this.mWaitingForResume.setStayPressed(true);
    }

    protected void onClickFolderIcon(View v) {
        if (!(v instanceof FolderIcon)) {
            throw new IllegalArgumentException("Input must be a FolderIcon");
        }
        FolderIcon folderIcon = (FolderIcon) v;
        FolderInfo info = folderIcon.getFolderInfo();
        Folder openFolder = this.mWorkspace.getFolderForTag(info);
        if (info.opened && openFolder == null) {
            Log.d("Launcher", "Folder info marked as open, but associated folder is not open. Screen: " + info.screenId + " (" + info.cellX + ", " + info.cellY + ")");
            info.opened = false;
        }
        if (!info.opened && !folderIcon.getFolder().isDestroyed()) {
            closeFolder();
            openFolder(folderIcon);
        } else if (openFolder != null) {
            int folderScreen = this.mWorkspace.getPageForView(openFolder);
            closeFolder(openFolder, true);
            if (folderScreen != this.mWorkspace.getCurrentPage()) {
                closeFolder();
                openFolder(folderIcon);
            }
        }
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onClickFolderIcon(v);
        }
    }

    protected void onClickAddWidgetButton(View view) {
        if (this.mIsSafeModeEnabled) {
            Toast.makeText(this, R.string.safemode_widget_error, 0).show();
            return;
        }
        showWidgetsView(true, true);
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onClickAddWidgetButton(view);
    }

    protected void onClickWallpaperPicker(View v) {
        if (!Utilities.isWallapaperAllowed(this)) {
            Toast.makeText(this, R.string.msg_disabled_by_admin, 0).show();
            return;
        }
        int pageScroll = this.mWorkspace.getScrollForPage(this.mWorkspace.getPageNearestToCenterOfScreen());
        float offset = this.mWorkspace.mWallpaperOffset.wallpaperOffsetForScroll(pageScroll);
        startActivityForResult(new Intent("android.intent.action.SET_WALLPAPER").setPackage(getPackageName()).putExtra("com.android.launcher3.WALLPAPER_OFFSET", offset), 10);
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onClickWallpaperPicker(v);
    }

    protected void onClickSettingsButton(View v) {
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onClickSettingsButton(v);
        } else {
            startActivity(new Intent(this, (Class<?>) SettingsActivity.class));
        }
    }

    public View.OnTouchListener getHapticFeedbackTouchListener() {
        if (this.mHapticFeedbackTouchListener == null) {
            this.mHapticFeedbackTouchListener = new View.OnTouchListener() {
                @Override
                @SuppressLint({"ClickableViewAccessibility"})
                public boolean onTouch(View v, MotionEvent event) {
                    if ((event.getAction() & 255) == 0) {
                        v.performHapticFeedback(1);
                    }
                    return false;
                }
            };
        }
        return this.mHapticFeedbackTouchListener;
    }

    public void onDragStarted(View view) {
        if (isOnCustomContent()) {
            moveWorkspaceToDefaultScreen();
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onDragStarted(view);
    }

    protected void onInteractionEnd() {
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onInteractionEnd();
    }

    protected void onInteractionBegin() {
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onInteractionBegin();
    }

    public void updateInteraction(Workspace.State fromState, Workspace.State toState) {
        boolean fromStateWithOverlay = fromState != Workspace.State.NORMAL;
        boolean toStateWithOverlay = toState != Workspace.State.NORMAL;
        if (toStateWithOverlay) {
            onInteractionBegin();
        } else {
            if (!fromStateWithOverlay) {
                return;
            }
            onInteractionEnd();
        }
    }

    void startApplicationDetailsActivity(ComponentName componentName, UserHandleCompat user) {
        try {
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
            launcherApps.showAppDetailsForProfile(componentName, user);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Unable to launch settings");
        } catch (SecurityException e2) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Launcher does not have permission to launch settings");
        }
    }

    boolean startApplicationUninstallActivity(ComponentName componentName, int flags, UserHandleCompat user) {
        if ((flags & 1) == 0) {
            Toast.makeText(this, R.string.uninstall_system_app_text, 0).show();
            return false;
        }
        String packageName = componentName.getPackageName();
        String className = componentName.getClassName();
        Intent intent = new Intent("android.intent.action.DELETE", Uri.fromParts("package", packageName, className));
        intent.setFlags(276824064);
        if (user != null) {
            user.addToIntent(intent, "android.intent.extra.USER");
        }
        startActivity(intent);
        return true;
    }

    private boolean startActivity(View v, Intent intent, Object tag) {
        boolean useLaunchAnimation;
        Drawable icon;
        intent.addFlags(268435456);
        if (v != null) {
            try {
                useLaunchAnimation = !intent.hasExtra("com.android.launcher3.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION");
            } catch (SecurityException e) {
                if (Utilities.ATLEAST_MARSHMALLOW && (tag instanceof ItemInfo) && intent.getComponent() == null && "android.intent.action.CALL".equals(intent.getAction()) && checkSelfPermission("android.permission.CALL_PHONE") != 0) {
                    sPendingAddItem = preparePendingAddArgs(13, intent, 0, (ItemInfo) tag);
                    requestPermissions(new String[]{"android.permission.CALL_PHONE"}, 13);
                    return false;
                }
                Toast.makeText(this, R.string.activity_not_found, 0).show();
                Log.e("Launcher", "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity or use the exported attribute for this activity. tag=" + tag + " intent=" + intent, e);
                return false;
            }
        } else {
            useLaunchAnimation = false;
        }
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(this);
        UserManagerCompat userManager = UserManagerCompat.getInstance(this);
        UserHandleCompat user = null;
        if (intent.hasExtra("profile")) {
            long serialNumber = intent.getLongExtra("profile", -1L);
            user = userManager.getUserForSerialNumber(serialNumber);
        }
        Bundle optsBundle = null;
        if (useLaunchAnimation) {
            ActivityOptions opts = null;
            if (Utilities.ATLEAST_MARSHMALLOW) {
                int left = 0;
                int top = 0;
                int width = v.getMeasuredWidth();
                int height = v.getMeasuredHeight();
                if ((v instanceof TextView) && (icon = Workspace.getTextViewIcon((TextView) v)) != null) {
                    Rect bounds = icon.getBounds();
                    left = (width - bounds.width()) / 2;
                    top = v.getPaddingTop();
                    width = bounds.width();
                    height = bounds.height();
                }
                opts = ActivityOptions.makeClipRevealAnimation(v, left, top, width, height);
            } else if (!Utilities.ATLEAST_LOLLIPOP) {
                opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            } else if (Utilities.ATLEAST_LOLLIPOP_MR1) {
                opts = ActivityOptions.makeCustomAnimation(this, R.anim.task_open_enter, R.anim.no_anim);
            }
            optsBundle = opts != null ? opts.toBundle() : null;
        }
        if (user != null && !user.equals(UserHandleCompat.myUserHandle())) {
            launcherApps.startActivityForProfile(intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
            return true;
        }
        StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
        try {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
            startActivity(intent, optsBundle);
            return true;
        } finally {
            StrictMode.setVmPolicy(oldPolicy);
        }
    }

    public boolean startActivitySafely(View v, Intent intent, Object tag) {
        if (this.mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            Toast.makeText(this, R.string.safemode_shortcut_error, 0).show();
            return false;
        }
        try {
            boolean success = startActivity(v, intent, tag);
            return success;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Unable to launch. tag=" + tag + " intent=" + intent, e);
            return false;
        }
    }

    private void copyFolderIconToImage(FolderIcon fi) {
        DragLayer.LayoutParams lp;
        int width = fi.getMeasuredWidth();
        int height = fi.getMeasuredHeight();
        if (this.mFolderIconImageView == null) {
            this.mFolderIconImageView = new ImageView(this);
        }
        if (this.mFolderIconBitmap == null || this.mFolderIconBitmap.getWidth() != width || this.mFolderIconBitmap.getHeight() != height) {
            this.mFolderIconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            this.mFolderIconCanvas = new Canvas(this.mFolderIconBitmap);
        }
        if (this.mFolderIconImageView.getLayoutParams() instanceof DragLayer.LayoutParams) {
            lp = (DragLayer.LayoutParams) this.mFolderIconImageView.getLayoutParams();
        } else {
            lp = new DragLayer.LayoutParams(width, height);
        }
        float scale = this.mDragLayer.getDescendantRectRelativeToSelf(fi, this.mRectForFolderAnimation);
        lp.customPosition = true;
        lp.x = this.mRectForFolderAnimation.left;
        lp.y = this.mRectForFolderAnimation.top;
        lp.width = (int) (width * scale);
        lp.height = (int) (height * scale);
        this.mFolderIconCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        fi.draw(this.mFolderIconCanvas);
        this.mFolderIconImageView.setImageBitmap(this.mFolderIconBitmap);
        if (fi.getFolder() != null) {
            this.mFolderIconImageView.setPivotX(fi.getFolder().getPivotXForIconAnimation());
            this.mFolderIconImageView.setPivotY(fi.getFolder().getPivotYForIconAnimation());
        }
        if (this.mDragLayer.indexOfChild(this.mFolderIconImageView) != -1) {
            this.mDragLayer.removeView(this.mFolderIconImageView);
        }
        this.mDragLayer.addView(this.mFolderIconImageView, lp);
        if (fi.getFolder() == null) {
            return;
        }
        fi.getFolder().bringToFront();
    }

    private void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi == null) {
            return;
        }
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.5f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.5f);
        FolderInfo info = (FolderInfo) fi.getTag();
        if (info.container == -101) {
            CellLayout cl = (CellLayout) fi.getParent().getParent();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) fi.getLayoutParams();
            cl.setFolderLeaveBehindCell(lp.cellX, lp.cellY);
        }
        copyFolderIconToImage(fi);
        fi.setVisibility(4);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(this.mFolderIconImageView, alpha, scaleX, scaleY);
        if (Utilities.ATLEAST_LOLLIPOP) {
            oa.setInterpolator(new LogDecelerateInterpolator(100, 0));
        }
        oa.setDuration(getResources().getInteger(R.integer.config_folderExpandDuration));
        oa.start();
    }

    private void shrinkAndFadeInFolderIcon(final FolderIcon fi, boolean animate) {
        if (fi == null) {
            return;
        }
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);
        final CellLayout cl = (CellLayout) fi.getParent().getParent();
        this.mDragLayer.removeView(this.mFolderIconImageView);
        copyFolderIconToImage(fi);
        ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(this.mFolderIconImageView, alpha, scaleX, scaleY);
        oa.setDuration(getResources().getInteger(R.integer.config_folderExpandDuration));
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (cl == null) {
                    return;
                }
                cl.clearFolderLeaveBehind();
                Launcher.this.mDragLayer.removeView(Launcher.this.mFolderIconImageView);
                fi.setVisibility(0);
            }
        });
        oa.start();
        if (animate) {
            return;
        }
        oa.end();
    }

    public void openFolder(FolderIcon folderIcon) {
        Folder folder = folderIcon.getFolder();
        Folder openFolder = this.mWorkspace != null ? this.mWorkspace.getOpenFolder() : null;
        if (openFolder != null && openFolder != folder) {
            closeFolder();
        }
        FolderInfo info = folder.mInfo;
        info.opened = true;
        ((CellLayout.LayoutParams) folderIcon.getLayoutParams()).canReorder = false;
        if (folder.getParent() == null) {
            this.mDragLayer.addView(folder);
            this.mDragController.addDropTarget(folder);
        } else {
            Log.w("Launcher", "Opening folder (" + folder + ") which already has a parent (" + folder.getParent() + ").");
        }
        folder.animateOpen();
        growAndFadeOutFolderIcon(folderIcon);
        folder.sendAccessibilityEvent(32);
        getDragLayer().sendAccessibilityEvent(2048);
    }

    public void closeFolder() {
        closeFolder(true);
    }

    public void closeFolder(boolean animate) {
        Folder folder = this.mWorkspace != null ? this.mWorkspace.getOpenFolder() : null;
        if (folder == null) {
            return;
        }
        if (folder.isEditingName()) {
            folder.dismissEditingName();
        }
        closeFolder(folder, animate);
    }

    public void closeFolder(Folder folder, boolean animate) {
        folder.getInfo().opened = false;
        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            FolderIcon fi = (FolderIcon) this.mWorkspace.getViewForTag(folder.mInfo);
            LauncherLog.d("Launcher", "closeFolder: fi = " + fi);
            shrinkAndFadeInFolderIcon(fi, animate);
            if (fi != null) {
                ((CellLayout.LayoutParams) fi.getLayoutParams()).canReorder = true;
            }
        }
        if (animate) {
            folder.animateClosed();
        } else {
            folder.close(false);
        }
        getDragLayer().sendAccessibilityEvent(32);
    }

    @Override
    public boolean onLongClick(View v) {
        if (!isDraggingEnabled() || isWorkspaceLocked() || this.mState != State.WORKSPACE) {
            return false;
        }
        if (v == this.mAllAppsButton) {
            onLongClickAllAppsButton(v);
            return true;
        }
        if (v instanceof Workspace) {
            if (this.mWorkspace.isInOverviewMode() || this.mWorkspace.isTouchActive()) {
                return false;
            }
            showOverviewMode(true);
            this.mWorkspace.performHapticFeedback(0, 1);
            return true;
        }
        CellLayout.CellInfo longClickCellInfo = null;
        View itemUnderLongClick = null;
        if (v.getTag() instanceof ItemInfo) {
            ItemInfo info = (ItemInfo) v.getTag();
            longClickCellInfo = new CellLayout.CellInfo(v, info);
            itemUnderLongClick = longClickCellInfo.cell;
            resetAddInfo();
        }
        boolean inHotseat = isHotseatLayout(v);
        if (!this.mDragController.isDragging()) {
            if (itemUnderLongClick == null) {
                this.mWorkspace.performHapticFeedback(0, 1);
                if (this.mWorkspace.isInOverviewMode()) {
                    this.mWorkspace.startReordering(v);
                } else {
                    showOverviewMode(true);
                }
            } else {
                boolean isAllAppsButton = inHotseat ? isAllAppsButtonRank(this.mHotseat.getOrderInHotseat(longClickCellInfo.cellX, longClickCellInfo.cellY)) : false;
                if (itemUnderLongClick instanceof Folder) {
                    isAllAppsButton = true;
                }
                if (!isAllAppsButton) {
                    this.mWorkspace.startDrag(longClickCellInfo);
                }
            }
        }
        return true;
    }

    boolean isHotseatLayout(View layout) {
        return this.mHotseat != null && layout != null && (layout instanceof CellLayout) && layout == this.mHotseat.getLayout();
    }

    public CellLayout getCellLayout(long container, long screenId) {
        if (container == -101) {
            if (this.mHotseat != null) {
                return this.mHotseat.getLayout();
            }
            return null;
        }
        if (this.mWorkspace != null) {
            return this.mWorkspace.getScreenWithId(screenId);
        }
        return null;
    }

    public boolean isAppsViewVisible() {
        return this.mState == State.APPS || this.mOnResumeState == State.APPS;
    }

    public boolean isWidgetsViewVisible() {
        return this.mState == State.WIDGETS || this.mOnResumeState == State.WIDGETS;
    }

    private void setWorkspaceBackground(int background) {
        switch (background) {
            case PackageInstallerCompat.STATUS_INSTALLING:
                getWindow().setBackgroundDrawable(new ColorDrawable(0));
                break;
            case PackageInstallerCompat.STATUS_FAILED:
                getWindow().setBackgroundDrawable(null);
                break;
            default:
                getWindow().setBackgroundDrawable(this.mWorkspaceBackgroundDrawable);
                break;
        }
    }

    protected void changeWallpaperVisiblity(boolean visible) {
        int wpflags = visible ? 1048576 : 0;
        int curflags = getWindow().getAttributes().flags & 1048576;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, 1048576);
        }
        setWorkspaceBackground(visible ? 0 : 2);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "onTrimMemory: level = " + level);
        }
        if (level >= 20) {
            SQLiteDatabase.releaseMemory();
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onTrimMemory(level);
    }

    public boolean showWorkspace(boolean animated) {
        return showWorkspace(-1, animated, null);
    }

    public boolean showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        return showWorkspace(-1, animated, onCompleteRunnable);
    }

    protected boolean showWorkspace(int snapToPage, boolean animated) {
        return showWorkspace(snapToPage, animated, null);
    }

    boolean showWorkspace(int snapToPage, boolean animated, Runnable onCompleteRunnable) {
        LauncherHelper.beginSection("showWorkspace");
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "showWorkspace: animated = " + animated + ", mState = " + this.mState);
        }
        if (this.mWorkspace == null) {
            LauncherHelper.endSection();
            return false;
        }
        boolean changed = (this.mState == State.WORKSPACE && this.mWorkspace.getState() == Workspace.State.NORMAL) ? false : true;
        if (changed) {
            this.mWorkspace.setVisibility(0);
            this.mStateTransitionAnimation.startAnimationToWorkspace(this.mState, this.mWorkspace.getState(), Workspace.State.NORMAL, snapToPage, animated, onCompleteRunnable);
            if (this.mAllAppsButton != null) {
                this.mAllAppsButton.requestFocus();
            }
        }
        this.mState = State.WORKSPACE;
        this.mUserPresent = true;
        updateAutoAdvanceState();
        if (changed) {
            getWindow().getDecorView().sendAccessibilityEvent(32);
        }
        LauncherHelper.endSection();
        return changed;
    }

    void showOverviewMode(boolean animated) {
        showOverviewMode(animated, false);
    }

    void showOverviewMode(boolean animated, boolean requestButtonFocus) {
        Runnable postAnimRunnable = null;
        if (requestButtonFocus) {
            postAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    Launcher.this.mOverviewPanel.requestFocusFromTouch();
                }
            };
        }
        this.mWorkspace.setVisibility(0);
        this.mStateTransitionAnimation.startAnimationToWorkspace(this.mState, this.mWorkspace.getState(), Workspace.State.OVERVIEW, -1, animated, postAnimRunnable);
        this.mState = State.WORKSPACE;
    }

    void showAppsView(boolean animated, boolean resetListToTop, boolean updatePredictedApps, boolean focusSearchBar) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "showAppsView: animated = " + animated + ", mState = " + this.mState);
        }
        if (resetListToTop) {
            this.mAppsView.scrollToTop();
        }
        if (updatePredictedApps) {
            tryAndUpdatePredictedApps();
        }
        showAppsOrWidgets(State.APPS, animated, focusSearchBar);
    }

    void showWidgetsView(boolean animated, boolean resetPageToZero) {
        if (resetPageToZero) {
            this.mWidgetsView.scrollToTop();
        }
        showAppsOrWidgets(State.WIDGETS, animated, false);
        this.mWidgetsView.post(new Runnable() {
            @Override
            public void run() {
                Launcher.this.mWidgetsView.requestFocus();
            }
        });
    }

    private boolean showAppsOrWidgets(State toState, boolean animated, boolean focusSearchBar) {
        if (this.mState != State.WORKSPACE && this.mState != State.APPS_SPRING_LOADED && this.mState != State.WIDGETS_SPRING_LOADED) {
            return false;
        }
        if (toState != State.APPS && toState != State.WIDGETS) {
            return false;
        }
        if (toState == State.APPS) {
            this.mStateTransitionAnimation.startAnimationToAllApps(this.mWorkspace.getState(), animated, focusSearchBar);
        } else {
            this.mStateTransitionAnimation.startAnimationToWidgets(this.mWorkspace.getState(), animated);
        }
        this.mState = toState;
        this.mUserPresent = false;
        updateAutoAdvanceState();
        closeFolder();
        getWindow().getDecorView().sendAccessibilityEvent(32);
        return true;
    }

    public Animator startWorkspaceStateChangeAnimation(Workspace.State toState, int toPage, boolean animated, HashMap<View, Integer> layerViews) {
        Workspace.State fromState = this.mWorkspace.getState();
        Animator anim = this.mWorkspace.setStateWithAnimation(toState, toPage, animated, layerViews);
        updateInteraction(fromState, toState);
        return anim;
    }

    public void enterSpringLoadedDragMode() {
        if (this.mState == State.WORKSPACE || this.mState == State.APPS_SPRING_LOADED || this.mState == State.WIDGETS_SPRING_LOADED) {
            return;
        }
        this.mStateTransitionAnimation.startAnimationToWorkspace(this.mState, this.mWorkspace.getState(), Workspace.State.SPRING_LOADED, -1, true, null);
        this.mState = isAppsViewVisible() ? State.APPS_SPRING_LOADED : State.WIDGETS_SPRING_LOADED;
    }

    public void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, int delay, final Runnable onCompleteRunnable) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "exitSpringLoadedDragModeDelayed successfulDrop = " + successfulDrop + ", delay = " + delay + ", mState = " + this.mState);
        }
        if (this.mState == State.APPS_SPRING_LOADED || this.mState == State.WIDGETS_SPRING_LOADED) {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (successfulDrop) {
                        Launcher.this.mWidgetsView.setVisibility(8);
                        Launcher.this.showWorkspace(true, onCompleteRunnable);
                    } else {
                        Launcher.this.exitSpringLoadedDragMode();
                    }
                }
            }, delay);
        }
    }

    void exitSpringLoadedDragMode() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "exitSpringLoadedDragMode mState = " + this.mState);
        }
        if (this.mState == State.APPS_SPRING_LOADED) {
            showAppsView(true, false, false, false);
        } else {
            if (this.mState != State.WIDGETS_SPRING_LOADED) {
                return;
            }
            showWidgetsView(true, false);
        }
    }

    private void tryAndUpdatePredictedApps() {
        List<ComponentKey> apps;
        if (this.mLauncherCallbacks == null || (apps = this.mLauncherCallbacks.getPredictedApps()) == null) {
            return;
        }
        this.mAppsView.setPredictedApps(apps);
    }

    void lockAllApps() {
    }

    public boolean launcherCallbacksProvidesSearch() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.providesSearch();
        }
        return false;
    }

    public View getOrCreateQsbBar() {
        if (launcherCallbacksProvidesSearch()) {
            return this.mLauncherCallbacks.getQsbBar();
        }
        if (this.mQsb == null) {
            AppWidgetProviderInfo searchProvider = Utilities.getSearchWidgetProvider(this);
            if (searchProvider == null) {
                return null;
            }
            Bundle opts = new Bundle();
            opts.putInt("appWidgetCategory", 4);
            LauncherAppState app = LauncherAppState.getInstance();
            DeviceProfile portraitProfile = app.getInvariantDeviceProfile().portraitProfile;
            DeviceProfile landscapeProfile = app.getInvariantDeviceProfile().landscapeProfile;
            float density = getResources().getDisplayMetrics().density;
            Point searchDimens = portraitProfile.getSearchBarDimensForWidgetOpts(getResources());
            int maxHeight = (int) (searchDimens.y / density);
            int minHeight = maxHeight;
            int maxWidth = (int) (searchDimens.x / density);
            int minWidth = maxWidth;
            if (!landscapeProfile.isVerticalBarLayout()) {
                Point searchDimens2 = landscapeProfile.getSearchBarDimensForWidgetOpts(getResources());
                maxHeight = (int) Math.max(maxHeight, searchDimens2.y / density);
                minHeight = (int) Math.min(minHeight, searchDimens2.y / density);
                maxWidth = (int) Math.max(maxWidth, searchDimens2.x / density);
                minWidth = (int) Math.min(minWidth, searchDimens2.x / density);
            }
            opts.putInt("appWidgetMaxHeight", maxHeight);
            opts.putInt("appWidgetMinHeight", minHeight);
            opts.putInt("appWidgetMaxWidth", maxWidth);
            opts.putInt("appWidgetMinWidth", minWidth);
            if (this.mLauncherCallbacks != null) {
                opts.putAll(this.mLauncherCallbacks.getAdditionalSearchWidgetOptions());
            }
            int widgetId = this.mSharedPrefs.getInt("qsb_widget_id", -1);
            AppWidgetProviderInfo widgetInfo = this.mAppWidgetManager.getAppWidgetInfo(widgetId);
            if (!searchProvider.provider.flattenToString().equals(this.mSharedPrefs.getString("qsb_widget_provider", null)) || widgetInfo == null || !widgetInfo.provider.equals(searchProvider.provider)) {
                if (widgetId > -1) {
                    this.mAppWidgetHost.deleteAppWidgetId(widgetId);
                }
                widgetId = this.mAppWidgetHost.allocateAppWidgetId();
                if (!AppWidgetManagerCompat.getInstance(this).bindAppWidgetIdIfAllowed(widgetId, searchProvider, opts)) {
                    this.mAppWidgetHost.deleteAppWidgetId(widgetId);
                    widgetId = -1;
                }
                this.mSharedPrefs.edit().putInt("qsb_widget_id", widgetId).putString("qsb_widget_provider", searchProvider.provider.flattenToString()).apply();
            }
            this.mAppWidgetHost.setQsbWidgetId(widgetId);
            if (getResources().getBoolean(R.bool.enable_qsb) && widgetId != -1) {
                this.mQsb = this.mAppWidgetHost.createView(this, widgetId, searchProvider);
                this.mQsb.setId(R.id.qsb_widget);
                this.mQsb.updateAppWidgetOptions(opts);
                this.mQsb.setPadding(0, 0, 0, 0);
                this.mSearchDropTargetBar.addView(this.mQsb);
                this.mSearchDropTargetBar.setQsbSearchBar(this.mQsb);
            }
        }
        return this.mQsb;
    }

    private void reinflateQSBIfNecessary() {
        if (!(this.mQsb instanceof LauncherAppWidgetHostView) || !((LauncherAppWidgetHostView) this.mQsb).isReinflateRequired()) {
            return;
        }
        this.mSearchDropTargetBar.removeView(this.mQsb);
        this.mQsb = null;
        this.mSearchDropTargetBar.setQsbSearchBar(getOrCreateQsbBar());
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean result = super.dispatchPopulateAccessibilityEvent(event);
        List<CharSequence> text = event.getText();
        text.clear();
        if (this.mState == State.APPS) {
            text.add(getString(R.string.all_apps_button_label));
        } else if (this.mState == State.WIDGETS) {
            text.add(getString(R.string.widget_button_text));
        } else if (this.mWorkspace != null) {
            text.add(this.mWorkspace.getCurrentPageDescription());
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        CloseSystemDialogsIntentReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Object reason;
            Bundle bundle = intent.getExtras();
            if (bundle != null && (reason = bundle.get("reason")) != null) {
                String closeReason = reason.toString();
                LauncherLog.d("Launcher", "Close system dialogs: reason = " + closeReason);
                if ("lock".equals(closeReason)) {
                    return;
                }
            }
            Launcher.this.closeSystemDialogs();
        }
    }

    boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (this.mPaused) {
            if (deletePreviousRunnables) {
                while (this.mBindOnResumeCallbacks.remove(run)) {
                }
            }
            if (run instanceof AppsUpdateTask) {
                ArrayList<AppInfo> curApps = ((AppsUpdateTask) run).getApps();
                int curAppsSize = curApps.size();
                if (curAppsSize <= 0) {
                    Log.e("Launcher", "Error: curAppsSize is 0");
                } else {
                    ArrayList<Runnable> removeApps = new ArrayList<>();
                    for (Runnable oldRun : this.mBindOnResumeCallbacks) {
                        if (oldRun instanceof AppsUpdateTask) {
                            ArrayList<AppInfo> oldApps = ((AppsUpdateTask) oldRun).getApps();
                            int oldAppsSize = oldApps.size();
                            if (oldAppsSize <= 0) {
                                Log.e("Launcher", "Error: oldAppsSize is 0");
                            } else {
                                boolean hasSameItem = false;
                                for (AppInfo oldAppInfo : oldApps) {
                                    ComponentName oldAppComponent = oldAppInfo.componentName;
                                    for (AppInfo curAppInfo : curApps) {
                                        ComponentName curAppComponent = curAppInfo.componentName;
                                        if (oldAppComponent != null || curAppComponent != null || oldAppComponent.toString() == curAppComponent.toString()) {
                                            hasSameItem = true;
                                            break;
                                        }
                                    }
                                    if (!hasSameItem) {
                                        break;
                                    }
                                }
                                if (hasSameItem) {
                                    removeApps.add(oldRun);
                                }
                            }
                        }
                    }
                    for (Runnable rmRun : removeApps) {
                        Log.d("Launcher", "Debug: 1 pending task was removed");
                        this.mBindOnResumeCallbacks.remove(rmRun);
                    }
                    removeApps.clear();
                }
            }
            this.mBindOnResumeCallbacks.add(run);
            return true;
        }
        return false;
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    public void addOnResumeCallback(Runnable run) {
        this.mOnResumeCallbacks.add(run);
    }

    @Override
    public boolean setLoadOnResume() {
        if (this.mPaused) {
            this.mOnResumeNeedsLoad = true;
            return true;
        }
        return false;
    }

    @Override
    public int getCurrentWorkspaceScreen() {
        if (this.mWorkspace != null) {
            return this.mWorkspace.getCurrentPage();
        }
        return 2;
    }

    @Override
    public void startBinding() {
        setWorkspaceLoading(true);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "startBinding: this = " + this);
        }
        this.mBindOnResumeCallbacks.clear();
        this.mWorkspace.clearDropTargets();
        this.mWorkspace.removeAllWorkspaceScreens();
        this.mWidgetsToAdvance.clear();
        if (this.mHotseat == null) {
            return;
        }
        this.mHotseat.resetLayout();
    }

    @Override
    public void bindScreens(ArrayList<Long> orderedScreenIds) {
        bindAddScreens(orderedScreenIds);
        if (orderedScreenIds.size() == 0) {
            this.mWorkspace.addExtraEmptyScreen();
        }
        if (!hasCustomContentToLeft()) {
            return;
        }
        this.mWorkspace.createCustomContentContainer();
        populateCustomContentContainer();
    }

    public void bindAddScreens(ArrayList<Long> orderedScreenIds) {
        int count = orderedScreenIds.size();
        for (int i = 0; i < count; i++) {
            this.mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(orderedScreenIds.get(i).longValue());
        }
    }

    @Override
    public void bindAppsAdded(final ArrayList<Long> newScreens, final ArrayList<ItemInfo> addNotAnimated, final ArrayList<ItemInfo> addAnimated, final ArrayList<AppInfo> addedApps) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindAppsAdded(newScreens, addNotAnimated, addAnimated, addedApps);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (newScreens != null) {
            bindAddScreens(newScreens);
        }
        if (addNotAnimated != null && !addNotAnimated.isEmpty()) {
            bindItems(addNotAnimated, 0, addNotAnimated.size(), false);
        }
        if (addAnimated != null && !addAnimated.isEmpty()) {
            bindItems(addAnimated, 0, addAnimated.size(), true);
        }
        this.mWorkspace.removeExtraEmptyScreen(false, false);
        if (addedApps == null || this.mAppsView == null) {
            return;
        }
        this.mAppsView.addApps(addedApps);
    }

    @Override
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end, final boolean forceAnimateIcons) {
        View view;
        CellLayout cl;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindItems(shortcuts, start, end, forceAnimateIcons);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        final AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        final Collection<Animator> bounceAnims = new ArrayList<>();
        boolean zCanRunNewAppsAnimation = forceAnimateIcons ? canRunNewAppsAnimation() : false;
        Workspace workspace = this.mWorkspace;
        long newShortcutsScreenId = -1;
        for (int i = start; i < end; i++) {
            ItemInfo item = shortcuts.get(i);
            if (LauncherLog.DEBUG) {
                LauncherLog.d("Launcher", "bindItems: start = " + start + ", end = " + end + "item = " + item + ", this = " + this);
            }
            if (item.container != -101 || this.mHotseat != null) {
                switch (item.itemType) {
                    case PackageInstallerCompat.STATUS_INSTALLED:
                    case PackageInstallerCompat.STATUS_INSTALLING:
                        ShortcutInfo info = (ShortcutInfo) item;
                        view = createShortcut(info);
                        if (item.container == -100 && (cl = this.mWorkspace.getScreenWithId(item.screenId)) != null && cl.isOccupied(item.cellX, item.cellY)) {
                            View v = cl.getChildAt(item.cellX, item.cellY);
                            Object tag = v.getTag();
                            String desc = "Collision while binding workspace item: " + item + ". Collides with " + tag;
                            if (LauncherAppState.isDogfoodBuild()) {
                                throw new RuntimeException(desc);
                            }
                            Log.d("Launcher", desc);
                        }
                        break;
                    case PackageInstallerCompat.STATUS_FAILED:
                        view = FolderIcon.fromXml(R.layout.folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()), (FolderInfo) item, this.mIconCache);
                        break;
                    default:
                        throw new RuntimeException("Invalid Item Type");
                }
                workspace.addInScreenFromBind(view, item.container, item.screenId, item.cellX, item.cellY, 1, 1);
                if (zCanRunNewAppsAnimation) {
                    view.setAlpha(0.0f);
                    view.setScaleX(0.0f);
                    view.setScaleY(0.0f);
                    bounceAnims.add(createNewAppBounceAnimation(view, i));
                    newShortcutsScreenId = item.screenId;
                }
            }
        }
        if (zCanRunNewAppsAnimation && newShortcutsScreenId > -1) {
            long currentScreenId = this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getNextPage());
            final int newScreenIndex = this.mWorkspace.getPageIndexForScreenId(newShortcutsScreenId);
            final Runnable startBounceAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    anim.playTogether(bounceAnims);
                    anim.start();
                }
            };
            if (newShortcutsScreenId != currentScreenId) {
                this.mWorkspace.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Launcher.this.mWorkspace == null) {
                            return;
                        }
                        Launcher.this.mWorkspace.snapToPage(newScreenIndex);
                        Launcher.this.mWorkspace.postDelayed(startBounceAnimRunnable, Launcher.NEW_APPS_ANIMATION_DELAY);
                    }
                }, NEW_APPS_PAGE_MOVE_DELAY);
            } else {
                this.mWorkspace.postDelayed(startBounceAnimRunnable, NEW_APPS_ANIMATION_DELAY);
            }
        }
        workspace.requestLayout();
    }

    @Override
    public void bindFolders(final LongArrayMap<FolderInfo> folders) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "bindFolders: this = " + this);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindFolders(folders);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        sFolders = folders.clone();
    }

    private void bindSafeModeWidget(LauncherAppWidgetInfo item) {
        PendingAppWidgetHostView view = new PendingAppWidgetHostView(this, item, true);
        view.updateIcon(this.mIconCache);
        item.hostView = view;
        item.hostView.updateAppWidget(null);
        item.hostView.setOnClickListener(this);
        addAppWidgetToWorkspace(item, null, false);
        this.mWorkspace.requestLayout();
    }

    @Override
    public void bindAppWidget(final LauncherAppWidgetInfo item) {
        LauncherAppWidgetProviderInfo launcherAppWidgetInfo;
        int i;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindAppWidget(item);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (this.mIsSafeModeEnabled) {
            bindSafeModeWidget(item);
            return;
        }
        if (item.hasRestoreFlag(2)) {
            launcherAppWidgetInfo = null;
        } else if (item.hasRestoreFlag(1)) {
            launcherAppWidgetInfo = this.mAppWidgetManager.findProvider(item.providerName, item.user);
        } else {
            launcherAppWidgetInfo = this.mAppWidgetManager.getLauncherAppWidgetInfo(item.appWidgetId);
        }
        if (!item.hasRestoreFlag(2) && item.restoreStatus != 0) {
            if (launcherAppWidgetInfo == null) {
                LauncherModel.deleteItemFromDatabase(this, item);
                return;
            }
            if (item.hasRestoreFlag(1)) {
                PendingAddWidgetInfo pendingInfo = new PendingAddWidgetInfo(this, launcherAppWidgetInfo, null);
                pendingInfo.spanX = item.spanX;
                pendingInfo.spanY = item.spanY;
                pendingInfo.minSpanX = item.minSpanX;
                pendingInfo.minSpanY = item.minSpanY;
                Bundle options = WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingInfo);
                int newWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                boolean success = this.mAppWidgetManager.bindAppWidgetIdIfAllowed(newWidgetId, launcherAppWidgetInfo, options);
                if (!success) {
                    this.mAppWidgetHost.deleteAppWidgetId(newWidgetId);
                    LauncherModel.deleteItemFromDatabase(this, item);
                    return;
                }
                item.appWidgetId = newWidgetId;
                if (launcherAppWidgetInfo.configure == null) {
                    i = 0;
                } else {
                    i = 4;
                }
                item.restoreStatus = i;
                LauncherModel.updateItemInDatabase(this, item);
            } else if (item.hasRestoreFlag(4) && launcherAppWidgetInfo.configure == null) {
                item.restoreStatus = 0;
                LauncherModel.updateItemInDatabase(this, item);
            }
        }
        if (item.restoreStatus == 0) {
            if (launcherAppWidgetInfo == null) {
                Log.e("Launcher", "Removing invalid widget: id=" + item.appWidgetId);
                deleteWidgetInfo(item);
                return;
            } else {
                item.hostView = this.mAppWidgetHost.createView((Context) this, item.appWidgetId, launcherAppWidgetInfo);
                item.minSpanX = launcherAppWidgetInfo.minSpanX;
                item.minSpanY = launcherAppWidgetInfo.minSpanY;
                addAppWidgetToWorkspace(item, launcherAppWidgetInfo, false);
            }
        } else {
            PendingAppWidgetHostView view = new PendingAppWidgetHostView(this, item, this.mIsSafeModeEnabled);
            view.updateIcon(this.mIconCache);
            item.hostView = view;
            item.hostView.updateAppWidget(null);
            item.hostView.setOnClickListener(this);
            addAppWidgetToWorkspace(item, null, false);
        }
        this.mWorkspace.requestLayout();
    }

    private void completeRestoreAppWidget(int appWidgetId) {
        LauncherAppWidgetHostView view = this.mWorkspace.getWidgetForAppWidgetId(appWidgetId);
        if (view == null || !(view instanceof PendingAppWidgetHostView)) {
            Log.e("Launcher", "Widget update called, when the widget no longer exists.");
            return;
        }
        LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) view.getTag();
        info.restoreStatus = 0;
        this.mWorkspace.reinflateWidgetsIfNecessary();
        LauncherModel.updateItemInDatabase(this, info);
    }

    @Override
    public void onPageBoundSynchronously(int page) {
        this.mSynchronouslyBoundPages.add(Integer.valueOf(page));
    }

    @Override
    public void finishBindingItems() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "finishBindingItems: mSavedState = " + this.mSavedState + ", mSavedInstanceState = " + this.mSavedInstanceState + ", this = " + this);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.finishBindingItems();
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (this.mSavedState != null) {
            if (!this.mWorkspace.hasFocus()) {
                this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()).requestFocus();
            }
            this.mSavedState = null;
        }
        this.mWorkspace.restoreInstanceStateForRemainingPages();
        setWorkspaceLoading(false);
        sendLoadingCompleteBroadcastIfNecessary();
        if (sPendingAddItem != null) {
            final long screenId = completeAdd(sPendingAddItem);
            this.mWorkspace.post(new Runnable() {
                @Override
                public void run() {
                    Launcher.this.mWorkspace.snapToScreenId(screenId);
                }
            });
            sPendingAddItem = null;
        }
        InstallShortcutReceiver.disableAndFlushInstallQueue(this);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.finishBindingItems(false);
        }
        this.mWorkspace.removeExtraEmptyScreenDelayed(true, null, 10, false);
    }

    private void sendLoadingCompleteBroadcastIfNecessary() {
        if (this.mSharedPrefs.getBoolean("launcher.first_load_complete", false)) {
            return;
        }
        String permission = getResources().getString(R.string.receive_first_load_broadcast_permission);
        Intent intent = new Intent("com.android.launcher3.action.FIRST_LOAD_COMPLETE");
        sendBroadcast(intent, permission);
        SharedPreferences.Editor editor = this.mSharedPrefs.edit();
        editor.putBoolean("launcher.first_load_complete", true);
        editor.apply();
    }

    @Override
    public boolean isAllAppsButtonRank(int rank) {
        if (this.mHotseat != null) {
            return this.mHotseat.isAllAppsButtonRank(rank);
        }
        return false;
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - this.mDragController.getLastGestureUpTime();
        if (diff > NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000) {
            return this.mClings == null || !this.mClings.isVisible();
        }
        return false;
    }

    private ValueAnimator createNewAppBounceAnimation(View v, int i) {
        ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(v, PropertyValuesHolder.ofFloat("alpha", 1.0f), PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
        bounceAnim.setDuration(450L);
        bounceAnim.setStartDelay(i * 85);
        bounceAnim.setInterpolator(new OvershootInterpolator(1.3f));
        return bounceAnim;
    }

    public int getSearchBarHeight() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.getSearchBarHeight();
        }
        return 0;
    }

    @Override
    public void bindSearchProviderChanged() {
        if (this.mSearchDropTargetBar == null) {
            return;
        }
        if (this.mQsb != null) {
            this.mSearchDropTargetBar.removeView(this.mQsb);
            this.mQsb = null;
        }
        this.mSearchDropTargetBar.setQsbSearchBar(getOrCreateQsbBar());
    }

    @Override
    public void bindAllApplications(ArrayList<AppInfo> apps) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "bindAllApplications: apps start");
        }
        if (waitUntilResume(this.mBindAllApplicationsRunnable, true)) {
            this.mTmpAppsList = apps;
            return;
        }
        if (this.mAppsView != null) {
            this.mAppsView.setApps(apps);
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.bindAllApplications(apps);
    }

    private class AppsUpdateTask implements Runnable {
        private ArrayList<AppInfo> mApps;

        public AppsUpdateTask(ArrayList<AppInfo> apps) {
            this.mApps = null;
            this.mApps = apps;
        }

        @Override
        public void run() {
            Launcher.this.bindAppsUpdated(this.mApps);
        }

        public ArrayList<AppInfo> getApps() {
            return this.mApps;
        }
    }

    @Override
    public void bindAppsUpdated(ArrayList<AppInfo> apps) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "bindAppsUpdated: apps = " + apps);
        }
        AppsUpdateTask r = new AppsUpdateTask(apps);
        if (waitUntilResume(r) || this.mAppsView == null) {
            return;
        }
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "bindAppsUpdated()");
        }
        this.mAppsView.updateApps(apps);
    }

    @Override
    public void bindWidgetsRestored(final ArrayList<LauncherAppWidgetInfo> widgets) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindWidgetsRestored(widgets);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        this.mWorkspace.widgetsRestored(widgets);
    }

    @Override
    public void bindShortcutsChanged(final ArrayList<ShortcutInfo> updated, final ArrayList<ShortcutInfo> removed, final UserHandleCompat user) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindShortcutsChanged(updated, removed, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (!updated.isEmpty()) {
            this.mWorkspace.updateShortcuts(updated);
        }
        if (removed.isEmpty()) {
            return;
        }
        HashSet<ComponentName> removedComponents = new HashSet<>();
        for (ShortcutInfo si : removed) {
            removedComponents.add(si.getTargetComponent());
        }
        this.mWorkspace.removeItemsByComponentName(removedComponents, user);
        this.mDragController.onAppsRemoved(new HashSet<>(), removedComponents);
    }

    @Override
    public void bindRestoreItemsChange(final HashSet<ItemInfo> updates) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindRestoreItemsChange(updates);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        this.mWorkspace.updateRestoreItems(updates);
    }

    @Override
    public void bindWorkspaceComponentsRemoved(final HashSet<String> packageNames, final HashSet<ComponentName> components, final UserHandleCompat user) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindWorkspaceComponentsRemoved(packageNames, components, user);
            }
        };
        if (waitUntilResume(r)) {
            return;
        }
        if (!packageNames.isEmpty()) {
            this.mWorkspace.removeItemsByPackageName(packageNames, user);
        }
        if (!components.isEmpty()) {
            this.mWorkspace.removeItemsByComponentName(components, user);
        }
        this.mDragController.onAppsRemoved(packageNames, components);
    }

    @Override
    public void bindAppInfosRemoved(final ArrayList<AppInfo> appInfos) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindAppInfosRemoved(appInfos);
            }
        };
        if (waitUntilResume(r) || this.mAppsView == null) {
            return;
        }
        this.mAppsView.removeApps(appInfos);
    }

    @Override
    public void bindWidgetsModel(WidgetsModel model) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher", "bindAllPackages()");
        }
        if (waitUntilResume(this.mBindWidgetModelRunnable, true)) {
            this.mWidgetsModel = model;
        } else {
            if (this.mWidgetsView == null || model == null) {
                return;
            }
            this.mWidgetsView.addWidgets(model);
            this.mWidgetsModel = null;
        }
    }

    @Override
    public void notifyWidgetProvidersChanged() {
        if (this.mWorkspace == null || !this.mWorkspace.getState().shouldUpdateWidget) {
            return;
        }
        this.mModel.refreshAndBindWidgetsAndShortcuts(this, this.mWidgetsView.isEmpty());
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = 2;
        switch (d.getRotation()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
            case PackageInstallerCompat.STATUS_FAILED:
                naturalOri = configOri;
                break;
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 3:
                naturalOri = configOri != 2 ? 2 : 1;
                break;
        }
        int[] oriMap = {1, 0, 9, 8};
        int indexOffset = 0;
        if (naturalOri == 2) {
            indexOffset = 1;
        }
        return oriMap[(d.getRotation() + indexOffset) % 4];
    }

    @TargetApi(18)
    public void lockScreenOrientation() {
        if (!this.mRotationEnabled) {
            return;
        }
        if (Utilities.ATLEAST_JB_MR2) {
            setRequestedOrientation(14);
        } else {
            setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources().getConfiguration().orientation));
        }
    }

    public void unlockScreenOrientation(boolean immediate) {
        if (this.mRotationEnabled) {
            if (immediate) {
                setRequestedOrientation(-1);
                return;
            } else {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Launcher.this.setRequestedOrientation(-1);
                    }
                }, 500L);
                return;
            }
        }
        setRequestedOrientation(5);
    }

    protected boolean isLauncherPreinstalled() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.isLauncherPreinstalled();
        }
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(getComponentName().getPackageName(), 0);
            return (ai.flags & 1) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean overrideWallpaperDimensions() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.overrideWallpaperDimensions();
        }
        return true;
    }

    protected boolean hasFirstRunActivity() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.hasFirstRunActivity();
        }
        return false;
    }

    protected Intent getFirstRunActivity() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.getFirstRunActivity();
        }
        return null;
    }

    private boolean shouldRunFirstRunActivity() {
        return (ActivityManager.isRunningInTestHarness() || this.mSharedPrefs.getBoolean("launcher.first_run_activity_displayed", false)) ? false : true;
    }

    public boolean showFirstRunActivity() {
        Intent firstRunIntent;
        if (shouldRunFirstRunActivity() && hasFirstRunActivity() && (firstRunIntent = getFirstRunActivity()) != null) {
            startActivity(firstRunIntent);
            markFirstRunActivityShown();
            return true;
        }
        return false;
    }

    private void markFirstRunActivityShown() {
        SharedPreferences.Editor editor = this.mSharedPrefs.edit();
        editor.putBoolean("launcher.first_run_activity_displayed", true);
        editor.apply();
    }

    protected boolean hasDismissableIntroScreen() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.hasDismissableIntroScreen();
        }
        return false;
    }

    protected View getIntroScreen() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.getIntroScreen();
        }
        return null;
    }

    private boolean shouldShowIntroScreen() {
        return hasDismissableIntroScreen() && !this.mSharedPrefs.getBoolean("launcher.intro_screen_dismissed", false);
    }

    protected void showIntroScreen() {
        View introScreen = getIntroScreen();
        changeWallpaperVisiblity(false);
        if (introScreen == null) {
            return;
        }
        this.mDragLayer.showOverlayView(introScreen);
    }

    void showFirstRunClings() {
        LauncherClings launcherClings = new LauncherClings(this);
        if (!launcherClings.shouldShowFirstRunOrMigrationClings()) {
            return;
        }
        this.mClings = launcherClings;
        if (this.mModel.canMigrateFromOldLauncherDb(this)) {
            launcherClings.showMigrationCling();
        } else {
            launcherClings.showLongPressCling(true);
        }
    }

    void showWorkspaceSearchAndHotseat() {
        if (this.mWorkspace != null) {
            this.mWorkspace.setAlpha(1.0f);
        }
        if (this.mHotseat != null) {
            this.mHotseat.setAlpha(1.0f);
        }
        if (this.mPageIndicators != null) {
            this.mPageIndicators.setAlpha(1.0f);
        }
        if (this.mSearchDropTargetBar == null) {
            return;
        }
        this.mSearchDropTargetBar.animateToState(SearchDropTargetBar.State.SEARCH_BAR, 0);
    }

    void hideWorkspaceSearchAndHotseat() {
        if (this.mWorkspace != null) {
            this.mWorkspace.setAlpha(0.0f);
        }
        if (this.mHotseat != null) {
            this.mHotseat.setAlpha(0.0f);
        }
        if (this.mPageIndicators != null) {
            this.mPageIndicators.setAlpha(0.0f);
        }
        if (this.mSearchDropTargetBar == null) {
            return;
        }
        this.mSearchDropTargetBar.animateToState(SearchDropTargetBar.State.INVISIBLE, 0);
    }

    protected void moveWorkspaceToDefaultScreen() {
        this.mWorkspace.moveToDefaultScreen(false);
    }

    @Override
    public void onPageSwitch(View newPage, int newPageIndex) {
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.onPageSwitch(newPage, newPageIndex);
    }

    public FastBitmapDrawable createIconDrawable(Bitmap icon) {
        FastBitmapDrawable d = new FastBitmapDrawable(icon);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }

    public Drawable resizeIconDrawable(Drawable icon) {
        icon.setBounds(0, 0, this.mDeviceProfile.iconSizePx, this.mDeviceProfile.iconSizePx);
        return icon;
    }

    public void dumpState() {
        Log.d("Launcher", "BEGIN launcher3 dump state for launcher " + this);
        Log.d("Launcher", "mSavedState=" + this.mSavedState);
        Log.d("Launcher", "mWorkspaceLoading=" + this.mWorkspaceLoading);
        Log.d("Launcher", "mRestoring=" + this.mRestoring);
        Log.d("Launcher", "mWaitingForResult=" + this.mWaitingForResult);
        Log.d("Launcher", "mSavedInstanceState=" + this.mSavedInstanceState);
        Log.d("Launcher", "sFolders.size=" + sFolders.size());
        this.mModel.dumpState();
        Log.d("Launcher", "END launcher3 dump state");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        synchronized (sDumpLogs) {
            writer.println(" ");
            writer.println("Debug logs: ");
            for (int i = 0; i < sDumpLogs.size(); i++) {
                writer.println("  " + sDumpLogs.get(i));
            }
        }
        if (this.mLauncherCallbacks == null) {
            return;
        }
        this.mLauncherCallbacks.dump(prefix, fd, writer, args);
    }

    public static void addDumpLog(String tag, String log, boolean debugLog) {
        addDumpLog(tag, log, null, debugLog);
    }

    public static void addDumpLog(String tag, String log, Exception e, boolean debugLog) {
        if (!debugLog) {
            return;
        }
        if (e != null) {
            Log.d(tag, log, e);
        } else {
            Log.d(tag, log);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            LauncherHelper.beginSection("Launcher.dispatchTouchEvent:ACTION_DOWN");
        } else if (ev.getAction() == 1) {
            LauncherHelper.beginSection("Launcher.dispatchTouchEvent:ACTION_UP");
        }
        LauncherHelper.endSection();
        return super.dispatchTouchEvent(ev);
    }
}
