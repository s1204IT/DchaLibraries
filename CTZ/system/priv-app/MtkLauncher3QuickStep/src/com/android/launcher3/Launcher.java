package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.appwidget.AppWidgetHostView;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;
import com.android.launcher3.DropTarget;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Workspace;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.allapps.AllAppsContainerView;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompatVO;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.FolderIconPreviewVerifier;
import com.android.launcher3.keyboard.CustomActionsPopup;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.logging.UserEventDispatcher;
import com.android.launcher3.model.ModelWriter;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.popup.PopupDataProvider;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.states.InternalStateHandler;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.ActivityResultInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.PendingRequestArgs;
import com.android.launcher3.util.Themes;
import com.android.launcher3.util.TraceHelper;
import com.android.launcher3.util.UiThreadHelper;
import com.android.launcher3.util.ViewOnDrawExecutor;
import com.android.launcher3.views.OptionsPopupView;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PendingAppWidgetHostView;
import com.android.launcher3.widget.WidgetAddFlowHandler;
import com.android.launcher3.widget.WidgetHostViewLoader;
import com.android.launcher3.widget.WidgetListRowEntry;
import com.android.launcher3.widget.WidgetsFullSheet;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/* loaded from: classes.dex */
public class Launcher extends BaseDraggingActivity implements LauncherExterns, LauncherModel.Callbacks, LauncherProviderChangeListener, UserEventDispatcher.UserEventDelegate {
    private static final float BOUNCE_ANIMATION_TENSION = 1.3f;
    static final boolean DEBUG_STRICT_MODE = false;
    static final boolean LOGD = false;
    static final int NEW_APPS_ANIMATION_DELAY = 500;
    private static final int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 5;
    private static final int NEW_APPS_PAGE_MOVE_DELAY = 500;
    private static final int ON_ACTIVITY_RESULT_ANIMATION_DELAY = 500;
    private static final int REQUEST_BIND_APPWIDGET = 11;
    public static final int REQUEST_BIND_PENDING_APPWIDGET = 12;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_CREATE_SHORTCUT = 1;
    protected static final int REQUEST_LAST = 100;
    private static final int REQUEST_PERMISSION_CALL_PHONE = 14;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    public static final int REQUEST_RECONFIGURE_APPWIDGET = 13;
    private static final String RUNTIME_STATE = "launcher.state";
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    private static final String RUNTIME_STATE_PENDING_ACTIVITY_RESULT = "launcher.activity_result";
    private static final String RUNTIME_STATE_PENDING_REQUEST_ARGS = "launcher.request_args";
    private static final String RUNTIME_STATE_WIDGET_PANEL = "launcher.widget_panel";
    public static final String TAG = "Launcher";
    private LauncherAccessibilityDelegate mAccessibilityDelegate;
    AllAppsTransitionController mAllAppsController;
    private LauncherAppTransitionManager mAppTransitionManager;
    private LauncherAppWidgetHost mAppWidgetHost;
    private AppWidgetManagerCompat mAppWidgetManager;
    AllAppsContainerView mAppsView;
    private DragController mDragController;
    DragLayer mDragLayer;
    private DropTargetBar mDropTargetBar;
    public ViewGroupFocusHelper mFocusHandler;
    Hotseat mHotseat;

    @Nullable
    private View mHotseatSearchBox;
    private IconCache mIconCache;
    private LauncherCallbacks mLauncherCallbacks;
    private View mLauncherView;
    private LauncherModel mModel;
    private ModelWriter mModelWriter;
    private Configuration mOldConfig;
    private OnResumeCallback mOnResumeCallback;
    private View mOverviewPanel;
    private View mOverviewPanelContainer;
    private ActivityResultInfo mPendingActivityResult;
    private ViewOnDrawExecutor mPendingExecutor;
    private PendingRequestArgs mPendingRequestArgs;
    private PopupDataProvider mPopupDataProvider;
    private RotationHelper mRotationHelper;
    private SharedPreferences mSharedPrefs;
    private LauncherStateManager mStateManager;
    Workspace mWorkspace;
    private final int[] mTmpAddItemCellCoordinates = new int[2];
    boolean mWorkspaceLoading = true;
    private int mSynchronouslyBoundPage = -1;
    private final Handler mHandler = new Handler();
    private final Runnable mLogOnDelayedResume = new Runnable() { // from class: com.android.launcher3.-$$Lambda$Launcher$I8Rtg4HeLRqItE1F68HKY2QnKug
        @Override // java.lang.Runnable
        public final void run() {
            this.f$0.logOnDelayedResume();
        }
    };
    private final BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() { // from class: com.android.launcher3.Launcher.6
        AnonymousClass6() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (Launcher.this.mPendingRequestArgs == null) {
                Launcher.this.mStateManager.goToState(LauncherState.NORMAL);
            }
        }
    };

    public interface LauncherOverlay {
        void onScrollChange(float f, boolean z);

        void onScrollInteractionBegin();

        void onScrollInteractionEnd();

        void setOverlayCallbacks(LauncherOverlayCallbacks launcherOverlayCallbacks);
    }

    public interface LauncherOverlayCallbacks {
        void onScrollChanged(float f);
    }

    public interface OnResumeCallback {
        void onLauncherResume();
    }

    @Override // com.android.launcher3.BaseDraggingActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        TraceHelper.beginSection("Launcher-onCreate");
        super.onCreate(bundle);
        TraceHelper.partitionSection("Launcher-onCreate", "super call");
        LauncherAppState launcherAppState = LauncherAppState.getInstance(this);
        this.mOldConfig = new Configuration(getResources().getConfiguration());
        this.mModel = launcherAppState.setLauncher(this);
        initDeviceProfile(launcherAppState.getInvariantDeviceProfile());
        this.mSharedPrefs = Utilities.getPrefs(this);
        this.mIconCache = launcherAppState.getIconCache();
        this.mAccessibilityDelegate = new LauncherAccessibilityDelegate(this);
        this.mDragController = new DragController(this);
        this.mAllAppsController = new AllAppsTransitionController(this);
        this.mStateManager = new LauncherStateManager(this);
        UiFactory.onCreate(this);
        this.mAppWidgetManager = AppWidgetManagerCompat.getInstance(this);
        this.mAppWidgetHost = new LauncherAppWidgetHost(this);
        this.mAppWidgetHost.startListening();
        this.mLauncherView = LayoutInflater.from(this).inflate(R.layout.launcher, (ViewGroup) null);
        setupViews();
        this.mPopupDataProvider = new PopupDataProvider(this);
        this.mRotationHelper = new RotationHelper(this);
        this.mAppTransitionManager = LauncherAppTransitionManager.newInstance(this);
        boolean zHandleCreate = InternalStateHandler.handleCreate(this, getIntent());
        if (zHandleCreate && bundle != null) {
            bundle.remove(RUNTIME_STATE);
        }
        restoreState(bundle);
        int i = PagedView.INVALID_RESTORE_PAGE;
        if (bundle != null) {
            i = bundle.getInt(RUNTIME_STATE_CURRENT_SCREEN, PagedView.INVALID_RESTORE_PAGE);
        }
        if (!this.mModel.startLoader(i)) {
            if (!zHandleCreate) {
                this.mDragLayer.getAlphaProperty(1).setValue(0.0f);
            }
        } else {
            this.mWorkspace.setCurrentPage(i);
            setWorkspaceLoading(true);
        }
        setDefaultKeyMode(3);
        setContentView(this.mLauncherView);
        getRootView().dispatchInsets();
        registerReceiver(this.mScreenOffReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
        getSystemUiController().updateUiState(0, Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onCreate(bundle);
        }
        this.mRotationHelper.initialize();
        TraceHelper.endSection("Launcher-onCreate");
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks
    public void onConfigurationChanged(Configuration configuration) {
        if ((configuration.diff(this.mOldConfig) & 1152) != 0) {
            this.mUserEventDispatcher = null;
            initDeviceProfile(this.mDeviceProfile.inv);
            dispatchDeviceProfileChanged();
            reapplyUi();
            this.mDragLayer.recreateControllers();
            rebindModel();
        }
        this.mOldConfig.setTo(configuration);
        UiFactory.onLauncherStateOrResumeChanged(this);
        super.onConfigurationChanged(configuration);
    }

    @Override // com.android.launcher3.BaseDraggingActivity
    protected void reapplyUi() {
        getRootView().dispatchInsets();
        getStateManager().reapplyState(true);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void rebindModel() {
        int nextPage = this.mWorkspace.getNextPage();
        if (this.mModel.startLoader(nextPage)) {
            this.mWorkspace.setCurrentPage(nextPage);
            setWorkspaceLoading(true);
        }
    }

    private void initDeviceProfile(InvariantDeviceProfile invariantDeviceProfile) {
        this.mDeviceProfile = invariantDeviceProfile.getDeviceProfile(this);
        if (isInMultiWindowModeCompat()) {
            Display defaultDisplay = getWindowManager().getDefaultDisplay();
            Point point = new Point();
            defaultDisplay.getSize(point);
            this.mDeviceProfile = this.mDeviceProfile.getMultiWindowProfile(this, point);
        }
        onDeviceProfileInitiated();
        this.mModelWriter = this.mModel.getWriter(this.mDeviceProfile.isVerticalBarLayout(), true);
    }

    public RotationHelper getRotationHelper() {
        return this.mRotationHelper;
    }

    public LauncherStateManager getStateManager() {
        return this.mStateManager;
    }

    @Override // android.app.Activity
    public <T extends View> T findViewById(int i) {
        return (T) this.mLauncherView.findViewById(i);
    }

    @Override // com.android.launcher3.LauncherProviderChangeListener
    public void onAppWidgetHostReset() {
        if (this.mAppWidgetHost != null) {
            this.mAppWidgetHost.startListening();
        }
    }

    @Override // com.android.launcher3.LauncherExterns
    public void setLauncherOverlay(LauncherOverlay launcherOverlay) {
        if (launcherOverlay != null) {
            launcherOverlay.setOverlayCallbacks(new LauncherOverlayCallbacksImpl());
        }
        this.mWorkspace.setLauncherOverlay(launcherOverlay);
    }

    @Override // com.android.launcher3.LauncherExterns
    public boolean setLauncherCallbacks(LauncherCallbacks launcherCallbacks) {
        this.mLauncherCallbacks = launcherCallbacks;
        return true;
    }

    @Override // com.android.launcher3.LauncherProviderChangeListener
    public void onLauncherProviderChanged() {
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onLauncherProviderChange();
        }
    }

    public boolean isDraggingEnabled() {
        return !isWorkspaceLoading();
    }

    public int getViewIdForItem(ItemInfo itemInfo) {
        return (int) itemInfo.id;
    }

    public PopupDataProvider getPopupDataProvider() {
        return this.mPopupDataProvider;
    }

    @Override // com.android.launcher3.BaseDraggingActivity
    public BadgeInfo getBadgeInfoForItem(ItemInfo itemInfo) {
        return this.mPopupDataProvider.getBadgeInfoForItem(itemInfo);
    }

    @Override // com.android.launcher3.BaseDraggingActivity
    public void invalidateParent(ItemInfo itemInfo) {
        View homescreenIconByItemId;
        if (new FolderIconPreviewVerifier(getDeviceProfile().inv).isItemInPreview(itemInfo.rank) && itemInfo.container >= 0 && (homescreenIconByItemId = getWorkspace().getHomescreenIconByItemId(itemInfo.container)) != null) {
            homescreenIconByItemId.invalidate();
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private long completeAdd(int i, Intent intent, int i2, PendingRequestArgs pendingRequestArgs) {
        LauncherAppWidgetProviderInfo launcherAppWidgetInfo;
        long jEnsurePendingDropLayoutExists = pendingRequestArgs.screenId;
        if (pendingRequestArgs.container == -100) {
            jEnsurePendingDropLayoutExists = ensurePendingDropLayoutExists(pendingRequestArgs.screenId);
        }
        if (i == 1) {
            completeAddShortcut(intent, pendingRequestArgs.container, jEnsurePendingDropLayoutExists, pendingRequestArgs.cellX, pendingRequestArgs.cellY, pendingRequestArgs);
        } else if (i == 5) {
            completeAddAppWidget(i2, pendingRequestArgs, null, null);
        } else {
            switch (i) {
                case 12:
                    LauncherAppWidgetInfo launcherAppWidgetInfoCompleteRestoreAppWidget = completeRestoreAppWidget(i2, 4);
                    if (launcherAppWidgetInfoCompleteRestoreAppWidget != null && (launcherAppWidgetInfo = this.mAppWidgetManager.getLauncherAppWidgetInfo(i2)) != null) {
                        new WidgetAddFlowHandler(launcherAppWidgetInfo).startConfigActivity(this, launcherAppWidgetInfoCompleteRestoreAppWidget, 13);
                        break;
                    }
                    break;
                case 13:
                    completeRestoreAppWidget(i2, 0);
                    break;
            }
        }
        return jEnsurePendingDropLayoutExists;
    }

    private void handleActivityResult(int i, int i2, Intent intent) {
        int intExtra;
        if (isWorkspaceLoading()) {
            this.mPendingActivityResult = new ActivityResultInfo(i, i2, intent);
            return;
        }
        this.mPendingActivityResult = null;
        PendingRequestArgs pendingRequestArgs = this.mPendingRequestArgs;
        setWaitingForResult(null);
        if (pendingRequestArgs == null) {
            return;
        }
        int widgetId = pendingRequestArgs.getWidgetId();
        AnonymousClass1 anonymousClass1 = new Runnable() { // from class: com.android.launcher3.Launcher.1
            AnonymousClass1() {
            }

            @Override // java.lang.Runnable
            public void run() {
                Launcher.this.mStateManager.goToState(LauncherState.NORMAL, 500L);
            }
        };
        if (i == 11) {
            if (intent != null) {
                intExtra = intent.getIntExtra(LauncherSettings.Favorites.APPWIDGET_ID, -1);
            } else {
                intExtra = -1;
            }
            if (i2 == 0) {
                completeTwoStageWidgetDrop(0, intExtra, pendingRequestArgs);
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, anonymousClass1, 500, false);
                return;
            } else {
                if (i2 == -1) {
                    addAppWidgetImpl(intExtra, pendingRequestArgs, null, pendingRequestArgs.getWidgetHandler(), 500);
                    return;
                }
                return;
            }
        }
        if (i == 9 || i == 5) {
            int intExtra2 = intent != null ? intent.getIntExtra(LauncherSettings.Favorites.APPWIDGET_ID, -1) : -1;
            if (intExtra2 >= 0) {
                widgetId = intExtra2;
            }
            if (widgetId < 0 || i2 == 0) {
                Log.e(TAG, "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the widget configuration activity.");
                completeTwoStageWidgetDrop(0, widgetId, pendingRequestArgs);
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, new Runnable() { // from class: com.android.launcher3.Launcher.2
                    AnonymousClass2() {
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        Launcher.this.getStateManager().goToState(LauncherState.NORMAL);
                    }
                }, 500, false);
                return;
            } else {
                if (pendingRequestArgs.container == -100) {
                    pendingRequestArgs.screenId = ensurePendingDropLayoutExists(pendingRequestArgs.screenId);
                }
                CellLayout screenWithId = this.mWorkspace.getScreenWithId(pendingRequestArgs.screenId);
                screenWithId.setDropPending(true);
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, new Runnable() { // from class: com.android.launcher3.Launcher.3
                    final /* synthetic */ int val$appWidgetId;
                    final /* synthetic */ CellLayout val$dropLayout;
                    final /* synthetic */ PendingRequestArgs val$requestArgs;
                    final /* synthetic */ int val$resultCode;

                    AnonymousClass3(int i22, int widgetId2, PendingRequestArgs pendingRequestArgs2, CellLayout screenWithId2) {
                        i = i22;
                        i = widgetId2;
                        pendingRequestArgs = pendingRequestArgs2;
                        cellLayout = screenWithId2;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        Launcher.this.completeTwoStageWidgetDrop(i, i, pendingRequestArgs);
                        cellLayout.setDropPending(false);
                    }
                }, 500, false);
                return;
            }
        }
        if (i == 13 || i == 12) {
            if (i22 == -1) {
                completeAdd(i, intent, widgetId2, pendingRequestArgs2);
                return;
            }
            return;
        }
        if (i == 1) {
            if (i22 == -1 && pendingRequestArgs2.container != -1) {
                completeAdd(i, intent, -1, pendingRequestArgs2);
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, anonymousClass1, 500, false);
            } else if (i22 == 0) {
                this.mWorkspace.removeExtraEmptyScreenDelayed(true, anonymousClass1, 500, false);
            }
        }
        this.mDragLayer.clearAnimatedView();
    }

    /* renamed from: com.android.launcher3.Launcher$1 */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            Launcher.this.mStateManager.goToState(LauncherState.NORMAL, 500L);
        }
    }

    /* renamed from: com.android.launcher3.Launcher$2 */
    class AnonymousClass2 implements Runnable {
        AnonymousClass2() {
        }

        @Override // java.lang.Runnable
        public void run() {
            Launcher.this.getStateManager().goToState(LauncherState.NORMAL);
        }
    }

    /* renamed from: com.android.launcher3.Launcher$3 */
    class AnonymousClass3 implements Runnable {
        final /* synthetic */ int val$appWidgetId;
        final /* synthetic */ CellLayout val$dropLayout;
        final /* synthetic */ PendingRequestArgs val$requestArgs;
        final /* synthetic */ int val$resultCode;

        AnonymousClass3(int i22, int widgetId2, PendingRequestArgs pendingRequestArgs2, CellLayout screenWithId2) {
            i = i22;
            i = widgetId2;
            pendingRequestArgs = pendingRequestArgs2;
            cellLayout = screenWithId2;
        }

        @Override // java.lang.Runnable
        public void run() {
            Launcher.this.completeTwoStageWidgetDrop(i, i, pendingRequestArgs);
            cellLayout.setDropPending(false);
        }
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    public void onActivityResult(int i, int i2, Intent intent) {
        handleActivityResult(i, i2, intent);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onActivityResult(i, i2, intent);
        }
    }

    @Override // android.app.Activity
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        View childAt;
        PendingRequestArgs pendingRequestArgs = this.mPendingRequestArgs;
        if (i == 14 && pendingRequestArgs != null && pendingRequestArgs.getRequestCode() == 14) {
            setWaitingForResult(null);
            CellLayout cellLayout = getCellLayout(pendingRequestArgs.container, pendingRequestArgs.screenId);
            if (cellLayout != null) {
                childAt = cellLayout.getChildAt(pendingRequestArgs.cellX, pendingRequestArgs.cellY);
            } else {
                childAt = null;
            }
            Intent pendingIntent = pendingRequestArgs.getPendingIntent();
            if (iArr.length > 0 && iArr[0] == 0) {
                startActivitySafely(childAt, pendingIntent, null);
            } else {
                Toast.makeText(this, getString(R.string.msg_no_phone_permission, new Object[]{getString(R.string.derived_app_name)}), 0).show();
            }
        }
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onRequestPermissionsResult(i, strArr, iArr);
        }
    }

    private long ensurePendingDropLayoutExists(long j) {
        if (this.mWorkspace.getScreenWithId(j) == null) {
            this.mWorkspace.addExtraEmptyScreen();
            return this.mWorkspace.commitExtraEmptyScreen();
        }
        return j;
    }

    void completeTwoStageWidgetDrop(int i, int i2, PendingRequestArgs pendingRequestArgs) {
        int i3;
        int i4;
        AnonymousClass4 anonymousClass4;
        AppWidgetHostView appWidgetHostView;
        CellLayout screenWithId = this.mWorkspace.getScreenWithId(pendingRequestArgs.screenId);
        if (i == -1) {
            AppWidgetHostView appWidgetHostViewCreateView = this.mAppWidgetHost.createView((Context) this, i2, pendingRequestArgs.getWidgetHandler().getProviderInfo(this));
            i4 = 3;
            appWidgetHostView = appWidgetHostViewCreateView;
            anonymousClass4 = new Runnable() { // from class: com.android.launcher3.Launcher.4
                final /* synthetic */ int val$appWidgetId;
                final /* synthetic */ AppWidgetHostView val$layout;
                final /* synthetic */ PendingRequestArgs val$requestArgs;

                AnonymousClass4(int i22, PendingRequestArgs pendingRequestArgs2, AppWidgetHostView appWidgetHostViewCreateView2) {
                    i = i22;
                    pendingRequestArgs = pendingRequestArgs2;
                    appWidgetHostView = appWidgetHostViewCreateView2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    Launcher.this.completeAddAppWidget(i, pendingRequestArgs, appWidgetHostView, null);
                    Launcher.this.mStateManager.goToState(LauncherState.NORMAL, 500L);
                }
            };
        } else {
            if (i == 0) {
                this.mAppWidgetHost.deleteAppWidgetId(i22);
                i3 = 4;
            } else {
                i3 = 0;
            }
            i4 = i3;
            anonymousClass4 = null;
            appWidgetHostView = null;
        }
        if (this.mDragLayer.getAnimatedView() != null) {
            this.mWorkspace.animateWidgetDrop(pendingRequestArgs2, screenWithId, (DragView) this.mDragLayer.getAnimatedView(), anonymousClass4, i4, appWidgetHostView, true);
        } else if (anonymousClass4 != null) {
            anonymousClass4.run();
        }
    }

    /* renamed from: com.android.launcher3.Launcher$4 */
    class AnonymousClass4 implements Runnable {
        final /* synthetic */ int val$appWidgetId;
        final /* synthetic */ AppWidgetHostView val$layout;
        final /* synthetic */ PendingRequestArgs val$requestArgs;

        AnonymousClass4(int i22, PendingRequestArgs pendingRequestArgs2, AppWidgetHostView appWidgetHostViewCreateView2) {
            i = i22;
            pendingRequestArgs = pendingRequestArgs2;
            appWidgetHostView = appWidgetHostViewCreateView2;
        }

        @Override // java.lang.Runnable
        public void run() {
            Launcher.this.completeAddAppWidget(i, pendingRequestArgs, appWidgetHostView, null);
            Launcher.this.mStateManager.goToState(LauncherState.NORMAL, 500L);
        }
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onStop();
        }
        getUserEventDispatcher().logActionCommand(5, this.mStateManager.getState().containerType, -1);
        this.mAppWidgetHost.setListenIfResumed(false);
        NotificationListener.removeNotificationsChangedListener();
        getStateManager().moveToRestState();
        onTrimMemory(20);
    }

    @Override // com.android.launcher3.BaseDraggingActivity, com.android.launcher3.BaseActivity, android.app.Activity
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onStart();
        }
        this.mAppWidgetHost.setListenIfResumed(true);
        NotificationListener.setNotificationsChangedListener(this.mPopupDataProvider);
        UiFactory.onStart(this);
    }

    private void logOnDelayedResume() {
        if (hasBeenResumed()) {
            getUserEventDispatcher().logActionCommand(7, this.mStateManager.getState().containerType, -1);
            getUserEventDispatcher().startSession();
        }
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    protected void onResume() {
        TraceHelper.beginSection("ON_RESUME");
        super.onResume();
        TraceHelper.partitionSection("ON_RESUME", "superCall");
        switch (Settings.System.getInt(getContentResolver(), "dcha_state", 0)) {
            case 1:
                Settings.System.putInt(getContentResolver(), "dcha_state", 0);
            case 0:
                if (Settings.System.getInt(getContentResolver(), "hide_navigation_bar", 0) != 0) {
                    Settings.System.putInt(getContentResolver(), "hide_navigation_bar", 0);
                    break;
                }
                break;
        }
        this.mHandler.removeCallbacks(this.mLogOnDelayedResume);
        Utilities.postAsyncCallback(this.mHandler, this.mLogOnDelayedResume);
        setOnResumeCallback(null);
        InstallShortcutReceiver.disableAndFlushInstallQueue(1, this);
        this.mModel.refreshShortcutsIfRequired();
        DiscoveryBounce.showForHomeIfNeeded(this);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onResume();
        }
        UiFactory.onLauncherStateOrResumeChanged(this);
        TraceHelper.endSection("ON_RESUME");
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    protected void onPause() {
        InstallShortcutReceiver.enableInstallQueue(1);
        super.onPause();
        this.mDragController.cancelDrag();
        this.mDragController.resetLastGestureUpTime();
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onPause();
        }
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        UiFactory.onLauncherStateOrResumeChanged(this);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        this.mStateManager.onWindowFocusChanged();
    }

    class LauncherOverlayCallbacksImpl implements LauncherOverlayCallbacks {
        LauncherOverlayCallbacksImpl() {
        }

        @Override // com.android.launcher3.Launcher.LauncherOverlayCallbacks
        public void onScrollChanged(float f) {
            if (Launcher.this.mWorkspace != null) {
                Launcher.this.mWorkspace.onOverlayScrollChanged(f);
            }
        }
    }

    public boolean hasSettings() {
        if (this.mLauncherCallbacks != null) {
            return this.mLauncherCallbacks.hasSettings();
        }
        return Utilities.ATLEAST_OREO || !getResources().getBoolean(R.bool.allow_rotation);
    }

    public boolean isInState(LauncherState launcherState) {
        return this.mStateManager.getState() == launcherState;
    }

    private void restoreState(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        LauncherState launcherState = LauncherState.values()[bundle.getInt(RUNTIME_STATE, LauncherState.NORMAL.ordinal)];
        if (!launcherState.disableRestore) {
            this.mStateManager.goToState(launcherState, false);
        }
        PendingRequestArgs pendingRequestArgs = (PendingRequestArgs) bundle.getParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS);
        if (pendingRequestArgs != null) {
            setWaitingForResult(pendingRequestArgs);
        }
        this.mPendingActivityResult = (ActivityResultInfo) bundle.getParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT);
        SparseArray sparseParcelableArray = bundle.getSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL);
        if (sparseParcelableArray != null) {
            WidgetsFullSheet.show(this, false).restoreHierarchyState(sparseParcelableArray);
        }
    }

    private void setupViews() {
        this.mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        this.mFocusHandler = this.mDragLayer.getFocusIndicatorHelper();
        this.mWorkspace = (Workspace) this.mDragLayer.findViewById(R.id.workspace);
        this.mWorkspace.initParentViews(this.mDragLayer);
        this.mOverviewPanel = findViewById(R.id.overview_panel);
        this.mOverviewPanelContainer = findViewById(R.id.overview_panel_container);
        this.mHotseat = (Hotseat) findViewById(R.id.hotseat);
        this.mHotseatSearchBox = findViewById(R.id.search_container_hotseat);
        this.mLauncherView.setSystemUiVisibility(1792);
        this.mDragLayer.setup(this.mDragController, this.mWorkspace);
        final DragLayer dragLayer = this.mDragLayer;
        Objects.requireNonNull(dragLayer);
        UiFactory.setOnTouchControllersChangedListener(this, new Runnable() { // from class: com.android.launcher3.-$$Lambda$dv0eqKdGIQKwyAOrq_nygXTL2ec
            @Override // java.lang.Runnable
            public final void run() {
                dragLayer.recreateControllers();
            }
        });
        this.mWorkspace.setup(this.mDragController);
        this.mWorkspace.lockWallpaperToDefaultPage();
        this.mWorkspace.bindAndInitFirstWorkspaceScreen(null);
        this.mDragController.addDragListener(this.mWorkspace);
        this.mDropTargetBar = (DropTargetBar) this.mDragLayer.findViewById(R.id.drop_target_bar);
        this.mAppsView = (AllAppsContainerView) findViewById(R.id.apps_view);
        this.mDragController.setMoveTarget(this.mWorkspace);
        this.mDropTargetBar.setup(this.mDragController);
        this.mAllAppsController.setupViews(this.mAppsView);
    }

    View createShortcut(ShortcutInfo shortcutInfo) {
        return createShortcut((ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), shortcutInfo);
    }

    public View createShortcut(ViewGroup viewGroup, ShortcutInfo shortcutInfo) {
        BubbleTextView bubbleTextView = (BubbleTextView) LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.app_icon, viewGroup, false);
        bubbleTextView.applyFromShortcutInfo(shortcutInfo);
        bubbleTextView.setOnClickListener(ItemClickHandler.INSTANCE);
        bubbleTextView.setOnFocusChangeListener(this.mFocusHandler);
        return bubbleTextView;
    }

    private void completeAddShortcut(Intent intent, long j, long j2, int i, int i2, PendingRequestArgs pendingRequestArgs) {
        ShortcutInfo shortcutInfoCreateShortcutInfoFromPinItemRequest;
        ShortcutInfo shortcutInfo;
        View view;
        int[] iArr;
        CellLayout cellLayout;
        boolean zFindCellForSpan;
        ShortcutInfo shortcutInfoFromShortcutIntent;
        if (pendingRequestArgs.getRequestCode() != 1 || pendingRequestArgs.getPendingIntent().getComponent() == null) {
            return;
        }
        int[] iArr2 = this.mTmpAddItemCellCoordinates;
        CellLayout cellLayout2 = getCellLayout(j, j2);
        if (Utilities.ATLEAST_OREO) {
            shortcutInfoCreateShortcutInfoFromPinItemRequest = LauncherAppsCompatVO.createShortcutInfoFromPinItemRequest(this, LauncherAppsCompatVO.getPinItemRequest(intent), 0L);
        } else {
            shortcutInfoCreateShortcutInfoFromPinItemRequest = null;
        }
        if (shortcutInfoCreateShortcutInfoFromPinItemRequest == null) {
            if (Process.myUserHandle().equals(pendingRequestArgs.user)) {
                shortcutInfoFromShortcutIntent = InstallShortcutReceiver.fromShortcutIntent(this, intent);
            } else {
                shortcutInfoFromShortcutIntent = null;
            }
            if (shortcutInfoFromShortcutIntent == null) {
                Log.e(TAG, "Unable to parse a valid custom shortcut result");
                return;
            } else {
                if (!new PackageManagerHelper(this).hasPermissionForActivity(shortcutInfoFromShortcutIntent.intent, pendingRequestArgs.getPendingIntent().getComponent().getPackageName())) {
                    Log.e(TAG, "Ignoring malicious intent " + shortcutInfoFromShortcutIntent.intent.toUri(0));
                    return;
                }
                shortcutInfo = shortcutInfoFromShortcutIntent;
            }
        } else {
            shortcutInfo = shortcutInfoCreateShortcutInfoFromPinItemRequest;
        }
        if (j < 0) {
            View viewCreateShortcut = createShortcut(shortcutInfo);
            if (i >= 0 && i2 >= 0) {
                iArr2[0] = i;
                iArr2[1] = i2;
                view = viewCreateShortcut;
                if (this.mWorkspace.createUserFolderIfNecessary(viewCreateShortcut, j, cellLayout2, iArr2, 0.0f, true, null)) {
                    return;
                }
                DropTarget.DragObject dragObject = new DropTarget.DragObject();
                dragObject.dragInfo = shortcutInfo;
                iArr = iArr2;
                if (this.mWorkspace.addToExistingFolderIfNecessary(view, cellLayout2, iArr, 0.0f, dragObject, true)) {
                    return;
                }
                cellLayout = cellLayout2;
                zFindCellForSpan = true;
            } else {
                view = viewCreateShortcut;
                iArr = iArr2;
                cellLayout = cellLayout2;
                zFindCellForSpan = cellLayout.findCellForSpan(iArr, 1, 1);
            }
            if (!zFindCellForSpan) {
                this.mWorkspace.onNoCellFound(cellLayout);
                return;
            }
            getModelWriter().addItemToDatabase(shortcutInfo, j, j2, iArr[0], iArr[1]);
            this.mWorkspace.addInScreen(view, shortcutInfo);
            return;
        }
        FolderIcon folderIconFindFolderIcon = findFolderIcon(j);
        if (folderIconFindFolderIcon != null) {
            ((FolderInfo) folderIconFindFolderIcon.getTag()).add(shortcutInfo, pendingRequestArgs.rank, false);
            return;
        }
        Log.e(TAG, "Could not find folder with id " + j + " to add shortcut.");
    }

    /* renamed from: com.android.launcher3.Launcher$5 */
    class AnonymousClass5 implements Workspace.ItemOperator {
        final /* synthetic */ long val$folderIconId;

        AnonymousClass5(long j) {
            j = j;
        }

        @Override // com.android.launcher3.Workspace.ItemOperator
        public boolean evaluate(ItemInfo itemInfo, View view) {
            return itemInfo != null && itemInfo.id == j;
        }
    }

    public FolderIcon findFolderIcon(long j) {
        return (FolderIcon) this.mWorkspace.getFirstMatch(new Workspace.ItemOperator() { // from class: com.android.launcher3.Launcher.5
            final /* synthetic */ long val$folderIconId;

            AnonymousClass5(long j2) {
                j = j2;
            }

            @Override // com.android.launcher3.Workspace.ItemOperator
            public boolean evaluate(ItemInfo itemInfo, View view) {
                return itemInfo != null && itemInfo.id == j;
            }
        });
    }

    void completeAddAppWidget(int i, ItemInfo itemInfo, AppWidgetHostView appWidgetHostView, LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo) {
        if (launcherAppWidgetProviderInfo == null) {
            launcherAppWidgetProviderInfo = this.mAppWidgetManager.getLauncherAppWidgetInfo(i);
        }
        LauncherAppWidgetInfo launcherAppWidgetInfo = new LauncherAppWidgetInfo(i, launcherAppWidgetProviderInfo.provider);
        launcherAppWidgetInfo.spanX = itemInfo.spanX;
        launcherAppWidgetInfo.spanY = itemInfo.spanY;
        launcherAppWidgetInfo.minSpanX = itemInfo.minSpanX;
        launcherAppWidgetInfo.minSpanY = itemInfo.minSpanY;
        launcherAppWidgetInfo.user = launcherAppWidgetProviderInfo.getProfile();
        getModelWriter().addItemToDatabase(launcherAppWidgetInfo, itemInfo.container, itemInfo.screenId, itemInfo.cellX, itemInfo.cellY);
        if (appWidgetHostView == null) {
            appWidgetHostView = this.mAppWidgetHost.createView((Context) this, i, launcherAppWidgetProviderInfo);
        }
        appWidgetHostView.setVisibility(0);
        prepareAppWidget(appWidgetHostView, launcherAppWidgetInfo);
        this.mWorkspace.addInScreen(appWidgetHostView, launcherAppWidgetInfo);
    }

    private void prepareAppWidget(AppWidgetHostView appWidgetHostView, LauncherAppWidgetInfo launcherAppWidgetInfo) {
        appWidgetHostView.setTag(launcherAppWidgetInfo);
        launcherAppWidgetInfo.onBindAppWidget(this, appWidgetHostView);
        appWidgetHostView.setFocusable(true);
        appWidgetHostView.setOnFocusChangeListener(this.mFocusHandler);
    }

    /* renamed from: com.android.launcher3.Launcher$6 */
    class AnonymousClass6 extends BroadcastReceiver {
        AnonymousClass6() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (Launcher.this.mPendingRequestArgs == null) {
                Launcher.this.mStateManager.goToState(LauncherState.NORMAL);
            }
        }
    }

    public void updateIconBadges(Set<PackageUserKey> set) {
        this.mWorkspace.updateIconBadges(set);
        this.mAppsView.getAppsStore().updateIconBadges(set);
        PopupContainerWithArrow open = PopupContainerWithArrow.getOpen(this);
        if (open != null) {
            open.updateNotificationHeader(set);
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onAttachedToWindow();
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onDetachedFromWindow();
        }
    }

    public AllAppsTransitionController getAllAppsController() {
        return this.mAllAppsController;
    }

    /* JADX DEBUG: Method merged with bridge method: getRootView()Landroid/view/View; */
    @Override // com.android.launcher3.BaseDraggingActivity
    public LauncherRootView getRootView() {
        return (LauncherRootView) this.mLauncherView;
    }

    /* JADX DEBUG: Method merged with bridge method: getDragLayer()Lcom/android/launcher3/views/BaseDragLayer; */
    @Override // com.android.launcher3.BaseDraggingActivity
    public DragLayer getDragLayer() {
        return this.mDragLayer;
    }

    public AllAppsContainerView getAppsView() {
        return this.mAppsView;
    }

    public Workspace getWorkspace() {
        return this.mWorkspace;
    }

    public Hotseat getHotseat() {
        return this.mHotseat;
    }

    public View getHotseatSearchBox() {
        return this.mHotseatSearchBox;
    }

    @Override // com.android.launcher3.BaseDraggingActivity
    public <T extends View> T getOverviewPanel() {
        return (T) this.mOverviewPanel;
    }

    public <T extends View> T getOverviewPanelContainer() {
        return (T) this.mOverviewPanelContainer;
    }

    public DropTargetBar getDropTargetBar() {
        return this.mDropTargetBar;
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return this.mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return this.mModel;
    }

    public ModelWriter getModelWriter() {
        return this.mModelWriter;
    }

    @Override // com.android.launcher3.LauncherExterns
    public SharedPreferences getSharedPrefs() {
        return this.mSharedPrefs;
    }

    public int getOrientation() {
        return this.mOldConfig.orientation;
    }

    @Override // android.app.Activity
    protected void onNewIntent(Intent intent) {
        TraceHelper.beginSection("NEW_INTENT");
        super.onNewIntent(intent);
        boolean z = hasWindowFocus() && (intent.getFlags() & 4194304) != 4194304;
        boolean z2 = z && isInState(LauncherState.NORMAL) && AbstractFloatingView.getTopOpenView(this) == null;
        boolean zEquals = "android.intent.action.MAIN".equals(intent.getAction());
        boolean zHandleNewIntent = InternalStateHandler.handleNewIntent(this, intent, isStarted());
        if (zEquals) {
            if (!zHandleNewIntent) {
                UserEventDispatcher userEventDispatcher = getUserEventDispatcher();
                AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this);
                if (topOpenView != null) {
                    topOpenView.logActionCommand(0);
                } else if (z) {
                    LauncherLogProto.Target targetNewContainerTarget = LoggerUtils.newContainerTarget(this.mStateManager.getState().containerType);
                    targetNewContainerTarget.pageIndex = this.mWorkspace.getCurrentPage();
                    userEventDispatcher.logActionCommand(0, targetNewContainerTarget, LoggerUtils.newContainerTarget(1));
                }
                AbstractFloatingView.closeAllOpenViews(this, isStarted());
                if (!isInState(LauncherState.NORMAL)) {
                    this.mStateManager.goToState(LauncherState.NORMAL);
                }
                if (!z) {
                    this.mAppsView.reset(isStarted());
                }
                if (z2 && !this.mWorkspace.isTouchActive()) {
                    Workspace workspace = this.mWorkspace;
                    final Workspace workspace2 = this.mWorkspace;
                    Objects.requireNonNull(workspace2);
                    workspace.post(new Runnable() { // from class: com.android.launcher3.-$$Lambda$sYh4eC33epC-mbZ5fujk1nuROqs
                        @Override // java.lang.Runnable
                        public final void run() {
                            workspace2.moveToDefaultScreen();
                        }
                    });
                }
            }
            View viewPeekDecorView = getWindow().peekDecorView();
            if (viewPeekDecorView != null && viewPeekDecorView.getWindowToken() != null) {
                UiThreadHelper.hideKeyboardAsync(this, viewPeekDecorView.getWindowToken());
            }
            if (this.mLauncherCallbacks != null) {
                this.mLauncherCallbacks.onHomeIntent(zHandleNewIntent);
            }
        }
        TraceHelper.endSection("NEW_INTENT");
    }

    @Override // android.app.Activity
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mWorkspace.restoreInstanceStateForChild(this.mSynchronouslyBoundPage);
    }

    @Override // android.app.Activity
    protected void onSaveInstanceState(Bundle bundle) {
        if (this.mWorkspace.getChildCount() > 0) {
            bundle.putInt(RUNTIME_STATE_CURRENT_SCREEN, this.mWorkspace.getNextPage());
        }
        bundle.putInt(RUNTIME_STATE, this.mStateManager.getState().ordinal);
        AbstractFloatingView openView = AbstractFloatingView.getOpenView(this, 16);
        if (openView != null) {
            SparseArray<? extends Parcelable> sparseArray = new SparseArray<>();
            openView.saveHierarchyState(sparseArray);
            bundle.putSparseParcelableArray(RUNTIME_STATE_WIDGET_PANEL, sparseArray);
        } else {
            bundle.remove(RUNTIME_STATE_WIDGET_PANEL);
        }
        AbstractFloatingView.closeAllOpenViews(this, false);
        if (this.mPendingRequestArgs != null) {
            bundle.putParcelable(RUNTIME_STATE_PENDING_REQUEST_ARGS, this.mPendingRequestArgs);
        }
        if (this.mPendingActivityResult != null) {
            bundle.putParcelable(RUNTIME_STATE_PENDING_ACTIVITY_RESULT, this.mPendingActivityResult);
        }
        super.onSaveInstanceState(bundle);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onSaveInstanceState(bundle);
        }
    }

    @Override // com.android.launcher3.BaseDraggingActivity, android.app.Activity
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mScreenOffReceiver);
        this.mWorkspace.removeFolderListeners();
        UiFactory.setOnTouchControllersChangedListener(this, null);
        if (this.mModel.isCurrentCallbacks(this)) {
            this.mModel.stopLoader();
            LauncherAppState.getInstance(this).setLauncher(null);
        }
        this.mRotationHelper.destroy();
        try {
            this.mAppWidgetHost.stopListening();
        } catch (NullPointerException e) {
            Log.w(TAG, "problem while stopping AppWidgetHost during Launcher destruction", e);
        }
        TextKeyListener.getInstance().release();
        LauncherAnimUtils.onDestroyActivity();
        clearPendingBinds();
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onDestroy();
        }
    }

    /* JADX DEBUG: Method merged with bridge method: getAccessibilityDelegate()Landroid/view/View$AccessibilityDelegate; */
    @Override // com.android.launcher3.BaseActivity
    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return this.mAccessibilityDelegate;
    }

    public DragController getDragController() {
        return this.mDragController;
    }

    @Override // android.app.Activity
    public void startActivityForResult(Intent intent, int i, Bundle bundle) {
        super.startActivityForResult(intent, i, bundle);
    }

    @Override // android.app.Activity
    public void startIntentSenderForResult(IntentSender intentSender, int i, Intent intent, int i2, int i3, int i4, Bundle bundle) {
        try {
            super.startIntentSenderForResult(intentSender, i, intent, i2, i3, i4, bundle);
        } catch (IntentSender.SendIntentException e) {
            throw new ActivityNotFoundException();
        }
    }

    @Override // android.app.Activity
    public void startSearch(String str, boolean z, Bundle bundle, boolean z2) {
        if (bundle == null) {
            bundle = new Bundle();
            bundle.putString("source", "launcher-search");
        }
        if (this.mLauncherCallbacks == null || !this.mLauncherCallbacks.startSearch(str, z, bundle)) {
            super.startSearch(str, z, bundle, true);
        }
        this.mStateManager.goToState(LauncherState.NORMAL);
    }

    public boolean isWorkspaceLocked() {
        return this.mWorkspaceLoading || this.mPendingRequestArgs != null;
    }

    public boolean isWorkspaceLoading() {
        return this.mWorkspaceLoading;
    }

    private void setWorkspaceLoading(boolean z) {
        this.mWorkspaceLoading = z;
    }

    public void setWaitingForResult(PendingRequestArgs pendingRequestArgs) {
        this.mPendingRequestArgs = pendingRequestArgs;
    }

    void addAppWidgetFromDropImpl(int i, ItemInfo itemInfo, AppWidgetHostView appWidgetHostView, WidgetAddFlowHandler widgetAddFlowHandler) {
        addAppWidgetImpl(i, itemInfo, appWidgetHostView, widgetAddFlowHandler, 0);
    }

    void addAppWidgetImpl(int i, ItemInfo itemInfo, AppWidgetHostView appWidgetHostView, WidgetAddFlowHandler widgetAddFlowHandler, int i2) {
        if (!widgetAddFlowHandler.startConfigActivity(this, i, itemInfo, 5)) {
            AnonymousClass7 anonymousClass7 = new Runnable() { // from class: com.android.launcher3.Launcher.7
                AnonymousClass7() {
                }

                @Override // java.lang.Runnable
                public void run() {
                    Launcher.this.mStateManager.goToState(LauncherState.NORMAL, 500L);
                }
            };
            completeAddAppWidget(i, itemInfo, appWidgetHostView, widgetAddFlowHandler.getProviderInfo(this));
            this.mWorkspace.removeExtraEmptyScreenDelayed(true, anonymousClass7, i2, false);
        }
    }

    /* renamed from: com.android.launcher3.Launcher$7 */
    class AnonymousClass7 implements Runnable {
        AnonymousClass7() {
        }

        @Override // java.lang.Runnable
        public void run() {
            Launcher.this.mStateManager.goToState(LauncherState.NORMAL, 500L);
        }
    }

    public void addPendingItem(PendingAddItemInfo pendingAddItemInfo, long j, long j2, int[] iArr, int i, int i2) {
        pendingAddItemInfo.container = j;
        pendingAddItemInfo.screenId = j2;
        if (iArr != null) {
            pendingAddItemInfo.cellX = iArr[0];
            pendingAddItemInfo.cellY = iArr[1];
        }
        pendingAddItemInfo.spanX = i;
        pendingAddItemInfo.spanY = i2;
        int i3 = pendingAddItemInfo.itemType;
        if (i3 != 1) {
            switch (i3) {
                case 4:
                case 5:
                    addAppWidgetFromDrop((PendingAddWidgetInfo) pendingAddItemInfo);
                    return;
                default:
                    throw new IllegalStateException("Unknown item type: " + pendingAddItemInfo.itemType);
            }
        }
        processShortcutFromDrop((PendingAddShortcutInfo) pendingAddItemInfo);
    }

    private void processShortcutFromDrop(PendingAddShortcutInfo pendingAddShortcutInfo) {
        setWaitingForResult(PendingRequestArgs.forIntent(1, new Intent("android.intent.action.CREATE_SHORTCUT").setComponent(pendingAddShortcutInfo.componentName), pendingAddShortcutInfo));
        if (!pendingAddShortcutInfo.activityInfo.startConfigActivity(this, 1)) {
            handleActivityResult(1, 0, null);
        }
    }

    private void addAppWidgetFromDrop(PendingAddWidgetInfo pendingAddWidgetInfo) {
        AppWidgetHostView appWidgetHostView = pendingAddWidgetInfo.boundWidget;
        WidgetAddFlowHandler handler = pendingAddWidgetInfo.getHandler();
        if (appWidgetHostView != null) {
            getDragLayer().removeView(appWidgetHostView);
            addAppWidgetFromDropImpl(appWidgetHostView.getAppWidgetId(), pendingAddWidgetInfo, appWidgetHostView, handler);
            pendingAddWidgetInfo.boundWidget = null;
        } else {
            int iAllocateAppWidgetId = getAppWidgetHost().allocateAppWidgetId();
            if (this.mAppWidgetManager.bindAppWidgetIdIfAllowed(iAllocateAppWidgetId, pendingAddWidgetInfo.info, pendingAddWidgetInfo.bindOptions)) {
                addAppWidgetFromDropImpl(iAllocateAppWidgetId, pendingAddWidgetInfo, null, handler);
            } else {
                handler.startBindFlow(this, iAllocateAppWidgetId, pendingAddWidgetInfo, 11);
            }
        }
    }

    FolderIcon addFolder(CellLayout cellLayout, long j, long j2, int i, int i2) {
        FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);
        getModelWriter().addItemToDatabase(folderInfo, j, j2, i, i2);
        FolderIcon folderIconFromXml = FolderIcon.fromXml(R.layout.folder_icon, this, cellLayout, folderInfo);
        this.mWorkspace.addInScreen(folderIconFromXml, folderInfo);
        this.mWorkspace.getParentCellLayoutForView(folderIconFromXml).getShortcutsAndWidgets().measureChild(folderIconFromXml);
        return folderIconFromXml;
    }

    public boolean removeItem(View view, ItemInfo itemInfo, boolean z) {
        if (itemInfo instanceof ShortcutInfo) {
            View homescreenIconByItemId = this.mWorkspace.getHomescreenIconByItemId(itemInfo.container);
            if (homescreenIconByItemId instanceof FolderIcon) {
                ((FolderInfo) homescreenIconByItemId.getTag()).remove((ShortcutInfo) itemInfo, true);
            } else {
                this.mWorkspace.removeWorkspaceItem(view);
            }
            if (z) {
                getModelWriter().deleteItemFromDatabase(itemInfo);
            }
        } else if (itemInfo instanceof FolderInfo) {
            FolderInfo folderInfo = (FolderInfo) itemInfo;
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).removeListeners();
            }
            this.mWorkspace.removeWorkspaceItem(view);
            if (z) {
                getModelWriter().deleteFolderAndContentsFromDatabase(folderInfo);
            }
        } else if (itemInfo instanceof LauncherAppWidgetInfo) {
            LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) itemInfo;
            this.mWorkspace.removeWorkspaceItem(view);
            if (z) {
                deleteWidgetInfo(launcherAppWidgetInfo);
            }
        } else {
            return false;
        }
        return true;
    }

    private void deleteWidgetInfo(LauncherAppWidgetInfo launcherAppWidgetInfo) {
        LauncherAppWidgetHost appWidgetHost = getAppWidgetHost();
        if (appWidgetHost != null && !launcherAppWidgetInfo.isCustomWidget() && launcherAppWidgetInfo.isWidgetIdAllocated()) {
            new AsyncTask<Void, Void, Void>() { // from class: com.android.launcher3.Launcher.8
                final /* synthetic */ LauncherAppWidgetHost val$appWidgetHost;
                final /* synthetic */ LauncherAppWidgetInfo val$widgetInfo;

                AnonymousClass8(LauncherAppWidgetHost appWidgetHost2, LauncherAppWidgetInfo launcherAppWidgetInfo2) {
                    launcherAppWidgetHost = appWidgetHost2;
                    launcherAppWidgetInfo = launcherAppWidgetInfo2;
                }

                /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
                @Override // android.os.AsyncTask
                public Void doInBackground(Void... voidArr) {
                    launcherAppWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                    return null;
                }
            }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
        }
        getModelWriter().deleteItemFromDatabase(launcherAppWidgetInfo2);
    }

    /* renamed from: com.android.launcher3.Launcher$8 */
    class AnonymousClass8 extends AsyncTask<Void, Void, Void> {
        final /* synthetic */ LauncherAppWidgetHost val$appWidgetHost;
        final /* synthetic */ LauncherAppWidgetInfo val$widgetInfo;

        AnonymousClass8(LauncherAppWidgetHost appWidgetHost2, LauncherAppWidgetInfo launcherAppWidgetInfo2) {
            launcherAppWidgetHost = appWidgetHost2;
            launcherAppWidgetInfo = launcherAppWidgetInfo2;
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... voidArr) {
            launcherAppWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
            return null;
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return keyEvent.getKeyCode() == 3 || super.dispatchKeyEvent(keyEvent);
    }

    @Override // android.app.Activity
    public void onBackPressed() {
        if (finishAutoCancelActionMode()) {
            return;
        }
        if (this.mLauncherCallbacks != null && this.mLauncherCallbacks.handleBackPressed()) {
            return;
        }
        if (this.mDragController.isDragging()) {
            this.mDragController.cancelDrag();
            return;
        }
        UserEventDispatcher userEventDispatcher = getUserEventDispatcher();
        AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this);
        if (topOpenView == null || !topOpenView.onBackPressed()) {
            if (!isInState(LauncherState.NORMAL)) {
                LauncherState lastState = this.mStateManager.getLastState();
                userEventDispatcher.logActionCommand(1, this.mStateManager.getState().containerType, lastState.containerType);
                this.mStateManager.goToState(lastState);
                return;
            }
            this.mWorkspace.showOutlinesTemporarily();
        }
    }

    @Override // com.android.launcher3.BaseDraggingActivity
    @TargetApi(23)
    public ActivityOptions getActivityLaunchOptions(View view) {
        return this.mAppTransitionManager.getActivityLaunchOptions(this, view);
    }

    public LauncherAppTransitionManager getAppTransitionManager() {
        return this.mAppTransitionManager;
    }

    @Override // com.android.launcher3.BaseDraggingActivity
    @TargetApi(23)
    protected boolean onErrorStartingShortcut(Intent intent, ItemInfo itemInfo) {
        if (intent.getComponent() == null && "android.intent.action.CALL".equals(intent.getAction()) && checkSelfPermission("android.permission.CALL_PHONE") != 0) {
            setWaitingForResult(PendingRequestArgs.forIntent(14, intent, itemInfo));
            requestPermissions(new String[]{"android.permission.CALL_PHONE"}, 14);
            return true;
        }
        return false;
    }

    @Override // com.android.launcher3.BaseActivity, com.android.launcher3.logging.UserEventDispatcher.UserEventDelegate
    public void modifyUserEvent(LauncherLogProto.LauncherEvent launcherEvent) {
        if (launcherEvent.srcTarget != null && launcherEvent.srcTarget.length > 0 && launcherEvent.srcTarget[1].containerType == 7) {
            launcherEvent.srcTarget = new LauncherLogProto.Target[]{launcherEvent.srcTarget[0], launcherEvent.srcTarget[1], LoggerUtils.newTarget(3)};
            LauncherState state = this.mStateManager.getState();
            if (state == LauncherState.ALL_APPS) {
                launcherEvent.srcTarget[2].containerType = 4;
            } else if (state == LauncherState.OVERVIEW) {
                launcherEvent.srcTarget[2].containerType = 12;
            }
        }
    }

    @Override // com.android.launcher3.BaseDraggingActivity
    public boolean startActivitySafely(View view, Intent intent, ItemInfo itemInfo) {
        boolean zStartActivitySafely = super.startActivitySafely(view, intent, itemInfo);
        if (zStartActivitySafely && (view instanceof BubbleTextView)) {
            BubbleTextView bubbleTextView = (BubbleTextView) view;
            bubbleTextView.setStayPressed(true);
            setOnResumeCallback(bubbleTextView);
        }
        return zStartActivitySafely;
    }

    boolean isHotseatLayout(View view) {
        return this.mHotseat != null && view != null && (view instanceof CellLayout) && view == this.mHotseat.getLayout();
    }

    public CellLayout getCellLayout(long j, long j2) {
        if (j == -101) {
            if (this.mHotseat != null) {
                return this.mHotseat.getLayout();
            }
            return null;
        }
        return this.mWorkspace.getScreenWithId(j2);
    }

    @Override // android.app.Activity, android.content.ComponentCallbacks2
    public void onTrimMemory(int i) {
        super.onTrimMemory(i);
        if (i >= 20) {
            SQLiteDatabase.releaseMemory();
        }
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.onTrimMemory(i);
        }
        UiFactory.onTrimMemory(this, i);
    }

    @Override // android.app.Activity, android.view.Window.Callback
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        String description;
        boolean zDispatchPopulateAccessibilityEvent = super.dispatchPopulateAccessibilityEvent(accessibilityEvent);
        List<CharSequence> text = accessibilityEvent.getText();
        text.clear();
        if (this.mWorkspace == null) {
            description = getString(R.string.all_apps_home_button_label);
        } else {
            description = this.mStateManager.getState().getDescription(this);
        }
        text.add(description);
        return zDispatchPopulateAccessibilityEvent;
    }

    public void setOnResumeCallback(OnResumeCallback onResumeCallback) {
        if (this.mOnResumeCallback != null) {
            this.mOnResumeCallback.onLauncherResume();
        }
        this.mOnResumeCallback = onResumeCallback;
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public int getCurrentWorkspaceScreen() {
        if (this.mWorkspace != null) {
            return this.mWorkspace.getCurrentPage();
        }
        return 0;
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void clearPendingBinds() {
        if (this.mPendingExecutor != null) {
            this.mPendingExecutor.markCompleted();
            this.mPendingExecutor = null;
        }
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void startBinding() {
        TraceHelper.beginSection("startBinding");
        AbstractFloatingView.closeOpenViews(this, true, 399);
        setWorkspaceLoading(true);
        this.mWorkspace.clearDropTargets();
        this.mWorkspace.removeAllWorkspaceScreens();
        this.mAppWidgetHost.clearViews();
        if (this.mHotseat != null) {
            this.mHotseat.resetLayout(this.mDeviceProfile.isVerticalBarLayout());
        }
        TraceHelper.endSection("startBinding");
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindScreens(ArrayList<Long> arrayList) {
        if (arrayList.isEmpty()) {
            this.mWorkspace.addExtraEmptyScreen();
        }
        bindAddScreens(arrayList);
        this.mWorkspace.unlockWallpaperFromDefaultPageOnNextLayout();
    }

    private void bindAddScreens(ArrayList<Long> arrayList) {
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            this.mWorkspace.insertNewWorkspaceScreenBeforeEmptyScreen(arrayList.get(i).longValue());
        }
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindAppsAdded(ArrayList<Long> arrayList, ArrayList<ItemInfo> arrayList2, ArrayList<ItemInfo> arrayList3) {
        if (arrayList != null) {
            bindAddScreens(arrayList);
        }
        if (arrayList2 != null && !arrayList2.isEmpty()) {
            bindItems(arrayList2, false);
        }
        if (arrayList3 != null && !arrayList3.isEmpty()) {
            bindItems(arrayList3, true);
        }
        this.mWorkspace.removeExtraEmptyScreen(false, false);
    }

    /* JADX WARN: Removed duplicated region for block: B:77:0x0076  */
    /* JADX WARN: Removed duplicated region for block: B:82:0x00bc  */
    /* JADX WARN: Removed duplicated region for block: B:84:0x00c1  */
    /* JADX WARN: Removed duplicated region for block: B:99:0x00d5 A[SYNTHETIC] */
    @Override // com.android.launcher3.LauncherModel.Callbacks
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void bindItems(List<ItemInfo> list, boolean z) {
        View viewCreateShortcut;
        CellLayout screenWithId;
        AnimatorSet animatorSetCreateAnimatorSet = LauncherAnimUtils.createAnimatorSet();
        ArrayList arrayList = new ArrayList();
        boolean z2 = z && canRunNewAppsAnimation();
        Workspace workspace = this.mWorkspace;
        int size = list.size();
        long j = -1;
        for (int i = 0; i < size; i++) {
            ItemInfo itemInfo = list.get(i);
            if (itemInfo.container != -101 || this.mHotseat != null) {
                switch (itemInfo.itemType) {
                    case 0:
                    case 1:
                    case 6:
                        viewCreateShortcut = createShortcut((ShortcutInfo) itemInfo);
                        if (itemInfo.container == -100 && (screenWithId = this.mWorkspace.getScreenWithId(itemInfo.screenId)) != null && screenWithId.isOccupied(itemInfo.cellX, itemInfo.cellY)) {
                            Log.d(TAG, "Collision while binding workspace item: " + itemInfo + ". Collides with " + screenWithId.getChildAt(itemInfo.cellX, itemInfo.cellY).getTag());
                            getModelWriter().deleteItemFromDatabase(itemInfo);
                            break;
                        } else {
                            workspace.addInScreenFromBind(viewCreateShortcut, itemInfo);
                            if (z2) {
                                break;
                            } else {
                                viewCreateShortcut.setAlpha(0.0f);
                                viewCreateShortcut.setScaleX(0.0f);
                                viewCreateShortcut.setScaleY(0.0f);
                                arrayList.add(createNewAppBounceAnimation(viewCreateShortcut, i));
                                j = itemInfo.screenId;
                                break;
                            }
                        }
                    case 2:
                        viewCreateShortcut = FolderIcon.fromXml(R.layout.folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()), (FolderInfo) itemInfo);
                        if (itemInfo.container == -100) {
                            workspace.addInScreenFromBind(viewCreateShortcut, itemInfo);
                            if (z2) {
                            }
                            break;
                        }
                        break;
                    case 3:
                    default:
                        throw new RuntimeException("Invalid Item Type");
                    case 4:
                    case 5:
                        viewCreateShortcut = inflateAppWidget((LauncherAppWidgetInfo) itemInfo);
                        if (viewCreateShortcut == null) {
                            break;
                        } else if (itemInfo.container == -100) {
                        }
                        break;
                }
            }
        }
        if (z2 && j > -1) {
            long screenIdForPageIndex = this.mWorkspace.getScreenIdForPageIndex(this.mWorkspace.getNextPage());
            int pageIndexForScreenId = this.mWorkspace.getPageIndexForScreenId(j);
            AnonymousClass9 anonymousClass9 = new Runnable() { // from class: com.android.launcher3.Launcher.9
                final /* synthetic */ AnimatorSet val$anim;
                final /* synthetic */ Collection val$bounceAnims;

                AnonymousClass9(AnimatorSet animatorSetCreateAnimatorSet2, Collection arrayList2) {
                    animatorSet = animatorSetCreateAnimatorSet2;
                    collection = arrayList2;
                }

                @Override // java.lang.Runnable
                public void run() {
                    animatorSet.playTogether(collection);
                    animatorSet.start();
                }
            };
            if (j != screenIdForPageIndex) {
                this.mWorkspace.postDelayed(new Runnable() { // from class: com.android.launcher3.Launcher.10
                    final /* synthetic */ int val$newScreenIndex;
                    final /* synthetic */ Runnable val$startBounceAnimRunnable;

                    AnonymousClass10(int pageIndexForScreenId2, Runnable anonymousClass92) {
                        i = pageIndexForScreenId2;
                        runnable = anonymousClass92;
                    }

                    @Override // java.lang.Runnable
                    public void run() {
                        if (Launcher.this.mWorkspace != null) {
                            AbstractFloatingView.closeAllOpenViews(Launcher.this, false);
                            Launcher.this.mWorkspace.snapToPage(i);
                            Launcher.this.mWorkspace.postDelayed(runnable, 500L);
                        }
                    }
                }, 500L);
            } else {
                this.mWorkspace.postDelayed(anonymousClass92, 500L);
            }
        }
        workspace.requestLayout();
    }

    /* renamed from: com.android.launcher3.Launcher$9 */
    class AnonymousClass9 implements Runnable {
        final /* synthetic */ AnimatorSet val$anim;
        final /* synthetic */ Collection val$bounceAnims;

        AnonymousClass9(AnimatorSet animatorSetCreateAnimatorSet2, Collection arrayList2) {
            animatorSet = animatorSetCreateAnimatorSet2;
            collection = arrayList2;
        }

        @Override // java.lang.Runnable
        public void run() {
            animatorSet.playTogether(collection);
            animatorSet.start();
        }
    }

    /* renamed from: com.android.launcher3.Launcher$10 */
    class AnonymousClass10 implements Runnable {
        final /* synthetic */ int val$newScreenIndex;
        final /* synthetic */ Runnable val$startBounceAnimRunnable;

        AnonymousClass10(int pageIndexForScreenId2, Runnable anonymousClass92) {
            i = pageIndexForScreenId2;
            runnable = anonymousClass92;
        }

        @Override // java.lang.Runnable
        public void run() {
            if (Launcher.this.mWorkspace != null) {
                AbstractFloatingView.closeAllOpenViews(Launcher.this, false);
                Launcher.this.mWorkspace.snapToPage(i);
                Launcher.this.mWorkspace.postDelayed(runnable, 500L);
            }
        }
    }

    public void bindAppWidget(LauncherAppWidgetInfo launcherAppWidgetInfo) {
        View viewInflateAppWidget = inflateAppWidget(launcherAppWidgetInfo);
        if (viewInflateAppWidget != null) {
            this.mWorkspace.addInScreen(viewInflateAppWidget, launcherAppWidgetInfo);
            this.mWorkspace.requestLayout();
        }
    }

    private View inflateAppWidget(LauncherAppWidgetInfo launcherAppWidgetInfo) {
        LauncherAppWidgetProviderInfo launcherAppWidgetInfo2;
        AppWidgetHostView pendingAppWidgetHostView;
        if (this.mIsSafeModeEnabled) {
            PendingAppWidgetHostView pendingAppWidgetHostView2 = new PendingAppWidgetHostView(this, launcherAppWidgetInfo, this.mIconCache, true);
            prepareAppWidget(pendingAppWidgetHostView2, launcherAppWidgetInfo);
            return pendingAppWidgetHostView2;
        }
        TraceHelper.beginSection("BIND_WIDGET");
        if (!launcherAppWidgetInfo.hasRestoreFlag(2)) {
            if (launcherAppWidgetInfo.hasRestoreFlag(1)) {
                launcherAppWidgetInfo2 = this.mAppWidgetManager.findProvider(launcherAppWidgetInfo.providerName, launcherAppWidgetInfo.user);
            } else {
                launcherAppWidgetInfo2 = this.mAppWidgetManager.getLauncherAppWidgetInfo(launcherAppWidgetInfo.appWidgetId);
            }
        } else {
            launcherAppWidgetInfo2 = null;
        }
        if (!launcherAppWidgetInfo.hasRestoreFlag(2) && launcherAppWidgetInfo.restoreStatus != 0) {
            if (launcherAppWidgetInfo2 == null) {
                Log.d(TAG, "Removing restored widget: id=" + launcherAppWidgetInfo.appWidgetId + " belongs to component " + launcherAppWidgetInfo.providerName + ", as the provider is null");
                getModelWriter().deleteItemFromDatabase(launcherAppWidgetInfo);
                return null;
            }
            if (launcherAppWidgetInfo.hasRestoreFlag(1)) {
                if (!launcherAppWidgetInfo.hasRestoreFlag(16)) {
                    launcherAppWidgetInfo.appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                    launcherAppWidgetInfo.restoreStatus = 16 | launcherAppWidgetInfo.restoreStatus;
                    PendingAddWidgetInfo pendingAddWidgetInfo = new PendingAddWidgetInfo(launcherAppWidgetInfo2);
                    pendingAddWidgetInfo.spanX = launcherAppWidgetInfo.spanX;
                    pendingAddWidgetInfo.spanY = launcherAppWidgetInfo.spanY;
                    pendingAddWidgetInfo.minSpanX = launcherAppWidgetInfo.minSpanX;
                    pendingAddWidgetInfo.minSpanY = launcherAppWidgetInfo.minSpanY;
                    Bundle defaultOptionsForWidget = WidgetHostViewLoader.getDefaultOptionsForWidget(this, pendingAddWidgetInfo);
                    boolean zHasRestoreFlag = launcherAppWidgetInfo.hasRestoreFlag(32);
                    if (zHasRestoreFlag && launcherAppWidgetInfo.bindOptions != null) {
                        Bundle extras = launcherAppWidgetInfo.bindOptions.getExtras();
                        if (defaultOptionsForWidget != null) {
                            extras.putAll(defaultOptionsForWidget);
                        }
                        defaultOptionsForWidget = extras;
                    }
                    boolean zBindAppWidgetIdIfAllowed = this.mAppWidgetManager.bindAppWidgetIdIfAllowed(launcherAppWidgetInfo.appWidgetId, launcherAppWidgetInfo2, defaultOptionsForWidget);
                    launcherAppWidgetInfo.bindOptions = null;
                    launcherAppWidgetInfo.restoreStatus &= -33;
                    if (zBindAppWidgetIdIfAllowed) {
                        launcherAppWidgetInfo.restoreStatus = (launcherAppWidgetInfo2.configure == null || zHasRestoreFlag) ? 0 : 4;
                    }
                    getModelWriter().updateItemInDatabase(launcherAppWidgetInfo);
                }
            } else if (launcherAppWidgetInfo.hasRestoreFlag(4) && launcherAppWidgetInfo2.configure == null) {
                launcherAppWidgetInfo.restoreStatus = 0;
                getModelWriter().updateItemInDatabase(launcherAppWidgetInfo);
            }
        }
        if (launcherAppWidgetInfo.restoreStatus == 0) {
            if (launcherAppWidgetInfo2 == null) {
                FileLog.e(TAG, "Removing invalid widget: id=" + launcherAppWidgetInfo.appWidgetId);
                deleteWidgetInfo(launcherAppWidgetInfo);
                return null;
            }
            launcherAppWidgetInfo.minSpanX = launcherAppWidgetInfo2.minSpanX;
            launcherAppWidgetInfo.minSpanY = launcherAppWidgetInfo2.minSpanY;
            pendingAppWidgetHostView = this.mAppWidgetHost.createView((Context) this, launcherAppWidgetInfo.appWidgetId, launcherAppWidgetInfo2);
        } else {
            pendingAppWidgetHostView = new PendingAppWidgetHostView(this, launcherAppWidgetInfo, this.mIconCache, false);
        }
        prepareAppWidget(pendingAppWidgetHostView, launcherAppWidgetInfo);
        TraceHelper.endSection("BIND_WIDGET", "id=" + launcherAppWidgetInfo.appWidgetId);
        return pendingAppWidgetHostView;
    }

    private LauncherAppWidgetInfo completeRestoreAppWidget(int i, int i2) {
        LauncherAppWidgetHostView widgetForAppWidgetId = this.mWorkspace.getWidgetForAppWidgetId(i);
        if (widgetForAppWidgetId == null || !(widgetForAppWidgetId instanceof PendingAppWidgetHostView)) {
            Log.e(TAG, "Widget update called, when the widget no longer exists.");
            return null;
        }
        LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) widgetForAppWidgetId.getTag();
        launcherAppWidgetInfo.restoreStatus = i2;
        if (launcherAppWidgetInfo.restoreStatus == 0) {
            launcherAppWidgetInfo.pendingItemInfo = null;
        }
        if (((PendingAppWidgetHostView) widgetForAppWidgetId).isReinflateIfNeeded()) {
            widgetForAppWidgetId.reInflate();
        }
        getModelWriter().updateItemInDatabase(launcherAppWidgetInfo);
        return launcherAppWidgetInfo;
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void onPageBoundSynchronously(int i) {
        this.mSynchronouslyBoundPage = i;
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void executeOnNextDraw(ViewOnDrawExecutor viewOnDrawExecutor) {
        if (this.mPendingExecutor != null) {
            this.mPendingExecutor.markCompleted();
        }
        this.mPendingExecutor = viewOnDrawExecutor;
        if (!isInState(LauncherState.ALL_APPS)) {
            this.mAppsView.getAppsStore().setDeferUpdates(true);
            this.mPendingExecutor.execute(new Runnable() { // from class: com.android.launcher3.-$$Lambda$Launcher$AxCP77NtvN-TGPHnfypVCzOvaOg
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.mAppsView.getAppsStore().setDeferUpdates(false);
                }
            });
        }
        viewOnDrawExecutor.attachTo(this);
    }

    public void clearPendingExecutor(ViewOnDrawExecutor viewOnDrawExecutor) {
        if (this.mPendingExecutor == viewOnDrawExecutor) {
            this.mPendingExecutor = null;
        }
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void finishFirstPageBind(ViewOnDrawExecutor viewOnDrawExecutor) {
        MultiValueAlpha.AlphaProperty alphaProperty = this.mDragLayer.getAlphaProperty(1);
        if (alphaProperty.getValue() >= 1.0f) {
            if (viewOnDrawExecutor != null) {
                viewOnDrawExecutor.onLoadAnimationCompleted();
            }
        } else {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(alphaProperty, MultiValueAlpha.VALUE, 1.0f);
            if (viewOnDrawExecutor != null) {
                objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.Launcher.11
                    final /* synthetic */ ViewOnDrawExecutor val$executor;

                    AnonymousClass11(ViewOnDrawExecutor viewOnDrawExecutor2) {
                        viewOnDrawExecutor = viewOnDrawExecutor2;
                    }

                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        viewOnDrawExecutor.onLoadAnimationCompleted();
                    }
                });
            }
            objectAnimatorOfFloat.start();
        }
    }

    /* renamed from: com.android.launcher3.Launcher$11 */
    class AnonymousClass11 extends AnimatorListenerAdapter {
        final /* synthetic */ ViewOnDrawExecutor val$executor;

        AnonymousClass11(ViewOnDrawExecutor viewOnDrawExecutor2) {
            viewOnDrawExecutor = viewOnDrawExecutor2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            viewOnDrawExecutor.onLoadAnimationCompleted();
        }
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void finishBindingItems() {
        TraceHelper.beginSection("finishBindingItems");
        this.mWorkspace.restoreInstanceStateForRemainingPages();
        setWorkspaceLoading(false);
        if (this.mPendingActivityResult != null) {
            handleActivityResult(this.mPendingActivityResult.requestCode, this.mPendingActivityResult.resultCode, this.mPendingActivityResult.data);
            this.mPendingActivityResult = null;
        }
        InstallShortcutReceiver.disableAndFlushInstallQueue(2, this);
        TraceHelper.endSection("finishBindingItems");
    }

    private boolean canRunNewAppsAnimation() {
        return System.currentTimeMillis() - this.mDragController.getLastGestureUpTime() > 5000;
    }

    private ValueAnimator createNewAppBounceAnimation(View view, int i) {
        ObjectAnimator objectAnimatorOfViewAlphaAndScale = LauncherAnimUtils.ofViewAlphaAndScale(view, 1.0f, 1.0f, 1.0f);
        objectAnimatorOfViewAlphaAndScale.setDuration(450L);
        objectAnimatorOfViewAlphaAndScale.setStartDelay(i * 85);
        objectAnimatorOfViewAlphaAndScale.setInterpolator(new OvershootInterpolator(BOUNCE_ANIMATION_TENSION));
        return objectAnimatorOfViewAlphaAndScale;
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindAllApplications(ArrayList<AppInfo> arrayList) {
        this.mAppsView.getAppsStore().setApps(arrayList);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.bindAllApplications(arrayList);
        }
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> multiHashMap) {
        this.mPopupDataProvider.setDeepShortcutMap(multiHashMap);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindAppsAddedOrUpdated(ArrayList<AppInfo> arrayList) {
        this.mAppsView.getAppsStore().addOrUpdateApps(arrayList);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindPromiseAppProgressUpdated(PromiseAppInfo promiseAppInfo) {
        this.mAppsView.getAppsStore().updatePromiseAppProgress(promiseAppInfo);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindWidgetsRestored(ArrayList<LauncherAppWidgetInfo> arrayList) {
        this.mWorkspace.widgetsRestored(arrayList);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindShortcutsChanged(ArrayList<ShortcutInfo> arrayList, UserHandle userHandle) {
        if (!arrayList.isEmpty()) {
            this.mWorkspace.updateShortcuts(arrayList);
        }
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindRestoreItemsChange(HashSet<ItemInfo> hashSet) {
        this.mWorkspace.updateRestoreItems(hashSet);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindWorkspaceComponentsRemoved(ItemInfoMatcher itemInfoMatcher) {
        this.mWorkspace.removeItemsByMatcher(itemInfoMatcher);
        this.mDragController.onAppsRemoved(itemInfoMatcher);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindAppInfosRemoved(ArrayList<AppInfo> arrayList) {
        this.mAppsView.getAppsStore().removeApps(arrayList);
    }

    @Override // com.android.launcher3.LauncherModel.Callbacks
    public void bindAllWidgets(ArrayList<WidgetListRowEntry> arrayList) {
        this.mPopupDataProvider.setAllWidgets(arrayList);
        AbstractFloatingView topOpenView = AbstractFloatingView.getTopOpenView(this);
        if (topOpenView != null) {
            topOpenView.onWidgetsBound();
        }
    }

    public void refreshAndBindWidgetsForPackageUser(@Nullable PackageUserKey packageUserKey) {
        this.mModel.refreshAndBindWidgetsAndShortcuts(packageUserKey);
    }

    @Override // com.android.launcher3.BaseActivity, android.app.Activity
    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(str, fileDescriptor, printWriter, strArr);
        if (strArr.length > 0) {
            if (TextUtils.equals(strArr[0], "--all")) {
                printWriter.println(str + "Workspace Items");
                for (int i = 0; i < this.mWorkspace.getPageCount(); i++) {
                    printWriter.println(str + "  Homescreen " + i);
                    ShortcutAndWidgetContainer shortcutsAndWidgets = ((CellLayout) this.mWorkspace.getPageAt(i)).getShortcutsAndWidgets();
                    for (int i2 = 0; i2 < shortcutsAndWidgets.getChildCount(); i2++) {
                        Object tag = shortcutsAndWidgets.getChildAt(i2).getTag();
                        if (tag != null) {
                            printWriter.println(str + "    " + tag.toString());
                        }
                    }
                }
                printWriter.println(str + "  Hotseat");
                ShortcutAndWidgetContainer shortcutsAndWidgets2 = this.mHotseat.getLayout().getShortcutsAndWidgets();
                for (int i3 = 0; i3 < shortcutsAndWidgets2.getChildCount(); i3++) {
                    Object tag2 = shortcutsAndWidgets2.getChildAt(i3).getTag();
                    if (tag2 != null) {
                        printWriter.println(str + "    " + tag2.toString());
                    }
                }
            }
        }
        printWriter.println(str + "Misc:");
        printWriter.print(str + "\tmWorkspaceLoading=" + this.mWorkspaceLoading);
        StringBuilder sb = new StringBuilder();
        sb.append(" mPendingRequestArgs=");
        sb.append(this.mPendingRequestArgs);
        printWriter.print(sb.toString());
        printWriter.println(" mPendingActivityResult=" + this.mPendingActivityResult);
        printWriter.println(" mRotationHelper: " + this.mRotationHelper);
        dumpMisc(printWriter);
        try {
            FileLog.flushAll(printWriter);
        } catch (Exception e) {
        }
        this.mModel.dumpState(str, fileDescriptor, printWriter, strArr);
        if (this.mLauncherCallbacks != null) {
            this.mLauncherCallbacks.dump(str, fileDescriptor, printWriter, strArr);
        }
    }

    @Override // android.app.Activity, android.view.Window.Callback
    @TargetApi(24)
    public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> list, Menu menu, int i) {
        ArrayList arrayList = new ArrayList();
        if (isInState(LauncherState.NORMAL)) {
            arrayList.add(new KeyboardShortcutInfo(getString(R.string.all_apps_button_label), 29, 4096));
        }
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            if (new CustomActionsPopup(this, currentFocus).canShow()) {
                arrayList.add(new KeyboardShortcutInfo(getString(R.string.custom_actions), 43, 4096));
            }
            if ((currentFocus.getTag() instanceof ItemInfo) && DeepShortcutManager.supportsShortcuts((ItemInfo) currentFocus.getTag())) {
                arrayList.add(new KeyboardShortcutInfo(getString(R.string.shortcuts_menu_with_notifications_description), 47, 4096));
            }
        }
        if (!arrayList.isEmpty()) {
            list.add(new KeyboardShortcutGroup(getString(R.string.home_screen), arrayList));
        }
        super.onProvideKeyboardShortcuts(list, menu, i);
    }

    @Override // android.app.Activity
    public boolean onKeyShortcut(int i, KeyEvent keyEvent) {
        if (keyEvent.hasModifiers(4096)) {
            if (i != 29) {
                if (i != 43) {
                    if (i == 47) {
                        View currentFocus = getCurrentFocus();
                        if ((currentFocus instanceof BubbleTextView) && (currentFocus.getTag() instanceof ItemInfo) && this.mAccessibilityDelegate.performAction(currentFocus, (ItemInfo) currentFocus.getTag(), R.id.action_deep_shortcuts)) {
                            PopupContainerWithArrow.getOpen(this).requestFocus();
                            return true;
                        }
                    }
                } else if (new CustomActionsPopup(this, getCurrentFocus()).show()) {
                    return true;
                }
            } else if (isInState(LauncherState.NORMAL)) {
                getStateManager().goToState(LauncherState.ALL_APPS);
                return true;
            }
        }
        return super.onKeyShortcut(i, keyEvent);
    }

    @Override // android.app.Activity, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i == 82) {
            if (!this.mDragController.isDragging() && !this.mWorkspace.isSwitchingState() && isInState(LauncherState.NORMAL)) {
                AbstractFloatingView.closeAllOpenViews(this);
                OptionsPopupView.showDefaultOptions(this, -1.0f, -1.0f);
                return true;
            }
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    public static Launcher getLauncher(Context context) {
        if (context instanceof Launcher) {
            return (Launcher) context;
        }
        return (Launcher) ((ContextWrapper) context).getBaseContext();
    }
}
