package com.android.launcher2;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SearchManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.os.UserHandle;
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
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Advanceable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.launcher.R;
import com.android.launcher2.CellLayout;
import com.android.launcher2.DragLayer;
import com.android.launcher2.DropTarget;
import com.android.launcher2.LauncherModel;
import com.android.launcher2.SmoothPagedView;
import com.android.launcher2.Workspace;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class Launcher extends Activity implements View.OnClickListener, View.OnLongClickListener, View.OnTouchListener, LauncherModel.Callbacks {
    private View mAllAppsButton;
    private LauncherAppWidgetHost mAppWidgetHost;
    private AppWidgetManager mAppWidgetManager;
    private AppsCustomizePagedView mAppsCustomizeContent;
    private AppsCustomizeTabHost mAppsCustomizeTabHost;
    private long mAutoAdvanceSentTime;
    private AnimatorSet mDividerAnimator;
    private View mDockDivider;
    private DragController mDragController;
    private DragLayer mDragLayer;
    private Bitmap mFolderIconBitmap;
    private Canvas mFolderIconCanvas;
    private ImageView mFolderIconImageView;
    private FolderInfo mFolderInfo;
    private Hotseat mHotseat;
    private IconCache mIconCache;
    private LayoutInflater mInflater;
    private View mLauncherView;
    private LauncherModel mModel;
    private boolean mOnResumeNeedsLoad;
    private AppWidgetProviderInfo mPendingAddWidgetInfo;
    private View mQsbDivider;
    private boolean mRestoring;
    private Bundle mSavedInstanceState;
    private Bundle mSavedState;
    private SearchDropTargetBar mSearchDropTargetBar;
    private SharedPreferences mSharedPrefs;
    private AnimatorSet mStateAnimation;
    private boolean mWaitingForResult;
    private BubbleTextView mWaitingForResume;
    private ArrayList<Object> mWidgetsAndShortcuts;
    private Workspace mWorkspace;
    private Drawable mWorkspaceBackgroundDrawable;
    private static final Object sLock = new Object();
    private static int sScreen = 2;
    private static int NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS = 10;
    private static boolean sPausedFromUserAction = false;
    private static LocaleConfiguration sLocaleConfiguration = null;
    private static HashMap<Long, FolderInfo> sFolders = new HashMap<>();
    private static Drawable.ConstantState[] sGlobalSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sVoiceSearchIcon = new Drawable.ConstantState[2];
    private static Drawable.ConstantState[] sAppMarketIcon = new Drawable.ConstantState[2];
    static final ArrayList<String> sDumpLogs = new ArrayList<>();
    private static ArrayList<PendingAddArguments> sPendingAddList = new ArrayList<>();
    private static boolean sForceEnableRotation = isPropertyEnabled("launcher_force_rotate");
    private State mState = State.WORKSPACE;
    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();
    private ItemInfo mPendingAddInfo = new ItemInfo();
    private int mPendingAddWidgetId = -1;
    private int[] mTmpAddItemCellCoordinates = new int[2];
    private boolean mAutoAdvanceRunning = false;
    private State mOnResumeState = State.NONE;
    private SpannableStringBuilder mDefaultKeySsb = null;
    private boolean mWorkspaceLoading = true;
    private boolean mPaused = true;
    private ArrayList<Runnable> mOnResumeCallbacks = new ArrayList<>();
    private boolean mUserPresent = true;
    private boolean mVisible = false;
    private boolean mAttached = false;
    private Intent mAppMarketIntent = null;
    private final int ADVANCE_MSG = 1;
    private final int mAdvanceInterval = 20000;
    private final int mAdvanceStagger = 250;
    private long mAutoAdvanceTimeLeft = -1;
    private HashMap<View, AppWidgetProviderInfo> mWidgetsToAdvance = new HashMap<>();
    private final int mRestoreScreenOrientationDelay = 500;
    private final ArrayList<Integer> mSynchronouslyBoundPages = new ArrayList<>();
    private int mNewShortcutAnimatePage = -1;
    private ArrayList<View> mNewShortcutAnimateViews = new ArrayList<>();
    private Rect mRectForFolderAnimation = new Rect();
    private HideFromAccessibilityHelper mHideFromAccessibilityHelper = new HideFromAccessibilityHelper();
    private Runnable mBuildLayersRunnable = new Runnable() {
        @Override
        public void run() {
            if (Launcher.this.mWorkspace != null) {
                Launcher.this.mWorkspace.buildPageHardwareLayers();
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                Launcher.this.mUserPresent = false;
                Launcher.this.mDragLayer.clearAllResizeFrames();
                Launcher.this.updateRunning();
                if (Launcher.this.mAppsCustomizeTabHost != null && Launcher.this.mPendingAddInfo.container == -1) {
                    Launcher.this.mAppsCustomizeTabHost.reset();
                    Launcher.this.showWorkspace(false);
                    return;
                }
                return;
            }
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                Launcher.this.mUserPresent = true;
                Launcher.this.updateRunning();
            } else if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action) || "android.intent.action.MANAGED_PROFILE_REMOVED".equals(action)) {
                Launcher.this.getModel().forceReload();
            }
        }
    };
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                int i = 0;
                for (View key : Launcher.this.mWidgetsToAdvance.keySet()) {
                    final View v = key.findViewById(((AppWidgetProviderInfo) Launcher.this.mWidgetsToAdvance.get(key)).autoAdvanceViewId);
                    int delay = i * 250;
                    if (v instanceof Advanceable) {
                        postDelayed(new Runnable() {
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
        }
    };
    private Runnable mBindPackagesUpdatedRunnable = new Runnable() {
        @Override
        public void run() {
            Launcher.this.bindPackagesUpdated(Launcher.this.mWidgetsAndShortcuts);
            Launcher.this.mWidgetsAndShortcuts = null;
        }
    };

    private enum State {
        NONE,
        WORKSPACE,
        APPS_CUSTOMIZE,
        APPS_CUSTOMIZE_SPRING_LOADED
    }

    private static class PendingAddArguments {
        int cellX;
        int cellY;
        long container;
        Intent intent;
        int requestCode;
        int screen;

        private PendingAddArguments() {
        }
    }

    private static boolean isPropertyEnabled(String propertyName) {
        return Log.isLoggable(propertyName, 2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LauncherApplication app = (LauncherApplication) getApplication();
        this.mSharedPrefs = getSharedPreferences(LauncherApplication.getSharedPreferencesKey(), 0);
        this.mModel = app.setLauncher(this);
        this.mIconCache = app.getIconCache();
        this.mDragController = new DragController(this);
        this.mInflater = getLayoutInflater();
        this.mAppWidgetManager = AppWidgetManager.getInstance(this);
        this.mAppWidgetHost = new LauncherAppWidgetHost(this, 1024);
        this.mAppWidgetHost.startListening();
        this.mPaused = false;
        checkForLocaleChange();
        setContentView(R.layout.launcher);
        setupViews();
        showFirstRunWorkspaceCling();
        registerContentObservers();
        lockAllApps();
        this.mSavedState = savedInstanceState;
        restoreState(this.mSavedState);
        if (this.mAppsCustomizeContent != null) {
            this.mAppsCustomizeContent.onPackagesUpdated(LauncherModel.getSortedWidgetsAndShortcuts(this));
        }
        if (!this.mRestoring) {
            if (sPausedFromUserAction) {
                this.mModel.startLoader(true, -1);
            } else {
                this.mModel.startLoader(true, this.mWorkspace.getCurrentPage());
            }
        }
        if (!this.mModel.isAllAppsLoaded()) {
            ViewGroup appsCustomizeContentParent = (ViewGroup) this.mAppsCustomizeContent.getParent();
            this.mInflater.inflate(R.layout.apps_customize_progressbar, appsCustomizeContentParent);
        }
        this.mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(this.mDefaultKeySsb, 0);
        IntentFilter filter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        registerReceiver(this.mCloseSystemDialogsReceiver, filter);
        updateGlobalIcons();
        unlockScreenOrientation(true);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sPausedFromUserAction = true;
    }

    private void updateGlobalIcons() {
        boolean searchVisible = false;
        boolean voiceVisible = false;
        int coi = getCurrentOrientationIndexForGlobalIcons();
        if (sGlobalSearchIcon[coi] == null || sVoiceSearchIcon[coi] == null || sAppMarketIcon[coi] == null) {
            updateAppMarketIcon();
            searchVisible = updateGlobalSearchIcon();
            voiceVisible = updateVoiceSearchIcon(searchVisible);
        }
        if (sGlobalSearchIcon[coi] != null) {
            updateGlobalSearchIcon(sGlobalSearchIcon[coi]);
            searchVisible = true;
        }
        if (sVoiceSearchIcon[coi] != null) {
            updateVoiceSearchIcon(sVoiceSearchIcon[coi]);
            voiceVisible = true;
        }
        if (sAppMarketIcon[coi] != null) {
            updateAppMarketIcon(sAppMarketIcon[coi]);
        }
        if (this.mSearchDropTargetBar != null) {
            this.mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
        }
    }

    private void checkForLocaleChange() {
        if (sLocaleConfiguration == null) {
            new AsyncTask<Void, Void, LocaleConfiguration>() {
                @Override
                protected LocaleConfiguration doInBackground(Void... unused) throws Throwable {
                    LocaleConfiguration localeConfiguration = new LocaleConfiguration();
                    Launcher.readConfiguration(Launcher.this, localeConfiguration);
                    return localeConfiguration;
                }

                @Override
                protected void onPostExecute(LocaleConfiguration result) {
                    LocaleConfiguration unused = Launcher.sLocaleConfiguration = result;
                    Launcher.this.checkForLocaleChange();
                }
            }.execute(new Void[0]);
            return;
        }
        Configuration configuration = getResources().getConfiguration();
        String previousLocale = sLocaleConfiguration.locale;
        String locale = configuration.locale.toString();
        int previousMcc = sLocaleConfiguration.mcc;
        int mcc = configuration.mcc;
        int previousMnc = sLocaleConfiguration.mnc;
        int mnc = configuration.mnc;
        boolean localeChanged = (locale.equals(previousLocale) && mcc == previousMcc && mnc == previousMnc) ? false : true;
        if (localeChanged) {
            sLocaleConfiguration.locale = locale;
            sLocaleConfiguration.mcc = mcc;
            sLocaleConfiguration.mnc = mnc;
            this.mIconCache.flush();
            final LocaleConfiguration localeConfiguration = sLocaleConfiguration;
            new Thread("WriteLocaleConfiguration") {
                @Override
                public void run() throws Throwable {
                    Launcher.writeConfiguration(Launcher.this, localeConfiguration);
                }
            }.start();
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc;
        public int mnc;

        private LocaleConfiguration() {
            this.mcc = -1;
            this.mnc = -1;
        }
    }

    private static void readConfiguration(Context context, LocaleConfiguration configuration) throws Throwable {
        DataInputStream in = null;
        try {
            DataInputStream in2 = new DataInputStream(context.openFileInput("launcher.preferences"));
            try {
                configuration.locale = in2.readUTF();
                configuration.mcc = in2.readInt();
                configuration.mnc = in2.readInt();
                if (in2 != null) {
                    try {
                        in2.close();
                    } catch (IOException e) {
                    }
                }
            } catch (FileNotFoundException e2) {
                in = in2;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (IOException e4) {
                in = in2;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e5) {
                    }
                }
            } catch (Throwable th) {
                th = th;
                in = in2;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e6) {
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e7) {
        } catch (IOException e8) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static void writeConfiguration(Context context, LocaleConfiguration configuration) throws Throwable {
        DataOutputStream out;
        DataOutputStream out2 = null;
        try {
            try {
                out = new DataOutputStream(context.openFileOutput("launcher.preferences", 0));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e2) {
        }
        try {
            out.writeUTF(configuration.locale);
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
            if (out != null) {
                try {
                    out.close();
                    out2 = out;
                } catch (IOException e3) {
                    out2 = out;
                }
            } else {
                out2 = out;
            }
        } catch (FileNotFoundException e4) {
            out2 = out;
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e5) {
                }
            }
        } catch (IOException e6) {
            out2 = out;
            context.getFileStreamPath("launcher.preferences").delete();
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e7) {
                }
            }
        } catch (Throwable th2) {
            th = th2;
            out2 = out;
            if (out2 != null) {
                try {
                    out2.close();
                } catch (IOException e8) {
                }
            }
            throw th;
        }
    }

    public DragLayer getDragLayer() {
        return this.mDragLayer;
    }

    boolean isDraggingEnabled() {
        return !this.mModel.isLoadingWorkspace();
    }

    static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }

    private boolean completeAdd(PendingAddArguments args) {
        boolean result = false;
        switch (args.requestCode) {
            case 1:
                completeAddShortcut(args.intent, args.container, args.screen, args.cellX, args.cellY);
                result = true;
                break;
            case 5:
                int appWidgetId = args.intent.getIntExtra("appWidgetId", -1);
                completeAddAppWidget(appWidgetId, args.container, args.screen, null, null);
                result = true;
                break;
            case 6:
                completeAddApplication(args.intent, args.container, args.screen, args.cellX, args.cellY);
                break;
            case 7:
                processShortcut(args.intent);
                break;
        }
        resetAddInfo();
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int appWidgetId;
        int pendingAddWidgetId = this.mPendingAddWidgetId;
        this.mPendingAddWidgetId = -1;
        if (requestCode == 11) {
            int appWidgetId2 = data != null ? data.getIntExtra("appWidgetId", -1) : -1;
            if (resultCode == 0) {
                completeTwoStageWidgetDrop(0, appWidgetId2);
                return;
            } else {
                if (resultCode == -1) {
                    addAppWidgetImpl(appWidgetId2, this.mPendingAddInfo, null, this.mPendingAddWidgetInfo);
                    return;
                }
                return;
            }
        }
        boolean delayExitSpringLoadedMode = false;
        boolean isWidgetDrop = requestCode == 9 || requestCode == 5;
        this.mWaitingForResult = false;
        if (isWidgetDrop) {
            int widgetId = data != null ? data.getIntExtra("appWidgetId", -1) : -1;
            if (widgetId < 0) {
                appWidgetId = pendingAddWidgetId;
            } else {
                appWidgetId = widgetId;
            }
            if (appWidgetId < 0) {
                Log.e("Launcher", "Error: appWidgetId (EXTRA_APPWIDGET_ID) was not returned from the \\widget configuration activity.");
                completeTwoStageWidgetDrop(0, appWidgetId);
                return;
            } else {
                completeTwoStageWidgetDrop(resultCode, appWidgetId);
                return;
            }
        }
        if (resultCode == -1 && this.mPendingAddInfo.container != -1) {
            PendingAddArguments args = new PendingAddArguments();
            args.requestCode = requestCode;
            args.intent = data;
            args.container = this.mPendingAddInfo.container;
            args.screen = this.mPendingAddInfo.screen;
            args.cellX = this.mPendingAddInfo.cellX;
            args.cellY = this.mPendingAddInfo.cellY;
            if (isWorkspaceLocked()) {
                sPendingAddList.add(args);
            } else {
                delayExitSpringLoadedMode = completeAdd(args);
            }
        }
        this.mDragLayer.clearAnimatedView();
        exitSpringLoadedDragModeDelayed(resultCode != 0, delayExitSpringLoadedMode, null);
    }

    private void completeTwoStageWidgetDrop(final int resultCode, final int appWidgetId) {
        CellLayout cellLayout = (CellLayout) this.mWorkspace.getChildAt(this.mPendingAddInfo.screen);
        Runnable onCompleteRunnable = null;
        int animationType = 0;
        AppWidgetHostView boundWidget = null;
        if (resultCode == -1) {
            animationType = 3;
            final AppWidgetHostView layout = this.mAppWidgetHost.createView(this, appWidgetId, this.mPendingAddWidgetInfo);
            boundWidget = layout;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    Launcher.this.completeAddAppWidget(appWidgetId, Launcher.this.mPendingAddInfo.container, Launcher.this.mPendingAddInfo.screen, layout, null);
                    Launcher.this.exitSpringLoadedDragModeDelayed(resultCode != 0, false, null);
                }
            };
        } else if (resultCode == 0) {
            this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            animationType = 4;
            onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    Launcher.this.exitSpringLoadedDragModeDelayed(resultCode != 0, false, null);
                }
            };
        }
        if (this.mDragLayer.getAnimatedView() != null) {
            this.mWorkspace.animateWidgetDrop(this.mPendingAddInfo, cellLayout, (DragView) this.mDragLayer.getAnimatedView(), onCompleteRunnable, animationType, boundWidget, true);
        } else {
            onCompleteRunnable.run();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirstFrameAnimatorHelper.setIsVisible(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirstFrameAnimatorHelper.setIsVisible(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        switch (BenesseExtension.getDchaState()) {
            case 0:
            case 1:
                Settings.System.putInt(getContentResolver(), "dcha_state", 0);
                Settings.System.putInt(getContentResolver(), "hide_navigation_bar", 0);
                break;
        }
        if (this.mOnResumeState == State.WORKSPACE) {
            showWorkspace(false);
        } else if (this.mOnResumeState == State.APPS_CUSTOMIZE) {
            showAllApps(false);
        }
        this.mOnResumeState = State.NONE;
        setWorkspaceBackground(this.mState == State.WORKSPACE);
        InstallShortcutReceiver.flushInstallQueue(this);
        this.mPaused = false;
        sPausedFromUserAction = false;
        Log.d("Launcher", "Idle mode Screen is on ?");
        if (SystemProperties.getBoolean("ril.sim.idleScreenEvent", false)) {
            Intent intent = new Intent("android.intent.action.stk.event_list_action");
            intent.putExtra("STK EVENT", 5);
            intent.putExtra("EVENT LENGTH", 0);
            sendBroadcast(intent);
            Log.d("Launcher", "sendBroadcast STK Idle Screen Event intent !!!!!!!!!!!!!!! = " + intent);
        }
        if (this.mRestoring || this.mOnResumeNeedsLoad) {
            this.mWorkspaceLoading = true;
            this.mModel.startLoader(true, -1);
            this.mRestoring = false;
            this.mOnResumeNeedsLoad = false;
        }
        if (this.mOnResumeCallbacks.size() > 0) {
            if (this.mAppsCustomizeContent != null) {
                this.mAppsCustomizeContent.setBulkBind(true);
            }
            for (int i = 0; i < this.mOnResumeCallbacks.size(); i++) {
                this.mOnResumeCallbacks.get(i).run();
            }
            if (this.mAppsCustomizeContent != null) {
                this.mAppsCustomizeContent.setBulkBind(false);
            }
            this.mOnResumeCallbacks.clear();
        }
        if (this.mWaitingForResume != null) {
            this.mWaitingForResume.setStayPressed(false);
        }
        if (this.mAppsCustomizeContent != null) {
            this.mAppsCustomizeContent.resetDrawableState();
        }
        getWorkspace().reinflateWidgetsIfNecessary();
        updateGlobalIcons();
    }

    @Override
    protected void onPause() {
        updateWallpaperVisibility(true);
        super.onPause();
        this.mPaused = true;
        this.mDragController.cancelDrag();
        this.mDragController.resetLastGestureUpTime();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        this.mModel.stopLoader();
        if (this.mAppsCustomizeContent != null) {
            this.mAppsCustomizeContent.surrender();
        }
        return Boolean.TRUE;
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
        State[] stateValues = State.values();
        for (int i = 0; i < stateValues.length; i++) {
            if (stateValues[i].ordinal() == stateOrdinal) {
                State state2 = stateValues[i];
                return state2;
            }
        }
        return state;
    }

    private void restoreState(Bundle savedState) {
        if (savedState != null) {
            State state = intToState(savedState.getInt("launcher.state", State.WORKSPACE.ordinal()));
            if (state == State.APPS_CUSTOMIZE) {
                this.mOnResumeState = State.APPS_CUSTOMIZE;
            }
            int currentScreen = savedState.getInt("launcher.current_screen", -1);
            if (currentScreen > -1) {
                this.mWorkspace.setCurrentPage(currentScreen);
            }
            long pendingAddContainer = savedState.getLong("launcher.add_container", -1L);
            int pendingAddScreen = savedState.getInt("launcher.add_screen", -1);
            if (pendingAddContainer != -1 && pendingAddScreen > -1) {
                this.mPendingAddInfo.container = pendingAddContainer;
                this.mPendingAddInfo.screen = pendingAddScreen;
                this.mPendingAddInfo.cellX = savedState.getInt("launcher.add_cell_x");
                this.mPendingAddInfo.cellY = savedState.getInt("launcher.add_cell_y");
                this.mPendingAddInfo.spanX = savedState.getInt("launcher.add_span_x");
                this.mPendingAddInfo.spanY = savedState.getInt("launcher.add_span_y");
                this.mPendingAddWidgetInfo = (AppWidgetProviderInfo) savedState.getParcelable("launcher.add_widget_info");
                this.mPendingAddWidgetId = savedState.getInt("launcher.add_widget_id");
                this.mWaitingForResult = true;
                this.mRestoring = true;
            }
            boolean renameFolder = savedState.getBoolean("launcher.rename_folder", false);
            if (renameFolder) {
                long id = savedState.getLong("launcher.rename_folder_id");
                this.mFolderInfo = this.mModel.getFolderById(this, sFolders, id);
                this.mRestoring = true;
            }
            if (this.mAppsCustomizeTabHost != null) {
                String curTab = savedState.getString("apps_customize_currentTab");
                if (curTab != null) {
                    this.mAppsCustomizeTabHost.setContentTypeImmediate(this.mAppsCustomizeTabHost.getContentTypeForTabTag(curTab));
                    this.mAppsCustomizeContent.loadAssociatedPages(this.mAppsCustomizeContent.getCurrentPage());
                }
                int currentIndex = savedState.getInt("apps_customize_currentIndex");
                this.mAppsCustomizeContent.restorePageForIndex(currentIndex);
            }
        }
    }

    private void setupViews() {
        DragController dragController = this.mDragController;
        this.mLauncherView = findViewById(R.id.launcher);
        this.mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        this.mWorkspace = (Workspace) this.mDragLayer.findViewById(R.id.workspace);
        this.mQsbDivider = findViewById(R.id.qsb_divider);
        this.mDockDivider = findViewById(R.id.dock_divider);
        this.mLauncherView.setSystemUiVisibility(1024);
        this.mWorkspaceBackgroundDrawable = getResources().getDrawable(R.drawable.workspace_bg);
        this.mDragLayer.setup(this, dragController);
        this.mHotseat = (Hotseat) findViewById(R.id.hotseat);
        if (this.mHotseat != null) {
            this.mHotseat.setup(this);
        }
        this.mWorkspace.setHapticFeedbackEnabled(false);
        this.mWorkspace.setOnLongClickListener(this);
        this.mWorkspace.setup(dragController);
        dragController.addDragListener(this.mWorkspace);
        this.mSearchDropTargetBar = (SearchDropTargetBar) this.mDragLayer.findViewById(R.id.qsb_bar);
        this.mAppsCustomizeTabHost = (AppsCustomizeTabHost) findViewById(R.id.apps_customize_pane);
        this.mAppsCustomizeContent = (AppsCustomizePagedView) this.mAppsCustomizeTabHost.findViewById(R.id.apps_customize_pane_content);
        this.mAppsCustomizeContent.setup(this, dragController);
        dragController.setDragScoller(this.mWorkspace);
        dragController.setScrollView(this.mDragLayer);
        dragController.setMoveTarget(this.mWorkspace);
        dragController.addDropTarget(this.mWorkspace);
        if (this.mSearchDropTargetBar != null) {
            this.mSearchDropTargetBar.setup(this, dragController);
        }
    }

    View createShortcut(ShortcutInfo info) {
        return createShortcut(R.layout.application, (ViewGroup) this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()), info);
    }

    View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
        BubbleTextView favorite = (BubbleTextView) this.mInflater.inflate(layoutResId, parent, false);
        favorite.applyFromShortcutInfo(info, this.mIconCache);
        favorite.setOnClickListener(this);
        return favorite;
    }

    void completeAddApplication(Intent data, long container, int screen, int cellX, int cellY) {
        int[] cellXY = this.mTmpAddItemCellCoordinates;
        CellLayout layout = getCellLayout(container, screen);
        if (cellX >= 0 && cellY >= 0) {
            cellXY[0] = cellX;
            cellXY[1] = cellY;
        } else if (!layout.findCellForSpan(cellXY, 1, 1)) {
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }
        ShortcutInfo info = this.mModel.getShortcutInfo(getPackageManager(), data, Process.myUserHandle(), this);
        if (info != null) {
            info.setActivity(data);
            info.container = -1L;
            this.mWorkspace.addApplicationShortcut(info, layout, container, screen, cellXY[0], cellXY[1], isWorkspaceLocked(), cellX, cellY);
            return;
        }
        Log.e("Launcher", "Couldn't find ActivityInfo for selected application: " + data);
    }

    private void completeAddShortcut(Intent data, long container, int screen, int cellX, int cellY) {
        boolean foundCellSpan;
        int[] cellXY = this.mTmpAddItemCellCoordinates;
        int[] touchXY = this.mPendingAddInfo.dropPos;
        CellLayout layout = getCellLayout(container, screen);
        ShortcutInfo info = this.mModel.infoFromShortcutIntent(this, data, null);
        if (info != null) {
            View view = createShortcut(info);
            if (cellX >= 0 && cellY >= 0) {
                cellXY[0] = cellX;
                cellXY[1] = cellY;
                foundCellSpan = true;
                if (!this.mWorkspace.createUserFolderIfNecessary(view, container, layout, cellXY, 0.0f, true, null, null)) {
                    DropTarget.DragObject dragObject = new DropTarget.DragObject();
                    dragObject.dragInfo = info;
                    if (this.mWorkspace.addToExistingFolderIfNecessary(view, layout, cellXY, 0.0f, dragObject, true)) {
                        return;
                    }
                } else {
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
            LauncherModel.addItemToDatabase(this, info, container, screen, cellXY[0], cellXY[1], false);
            if (!this.mRestoring) {
                this.mWorkspace.addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1, isWorkspaceLocked());
            }
        }
    }

    static int[] getSpanForWidget(Context context, ComponentName component, int minWidth, int minHeight) {
        Rect padding = AppWidgetHostView.getDefaultPaddingForWidget(context, component, null);
        int requiredWidth = padding.left + minWidth + padding.right;
        int requiredHeight = padding.top + minHeight + padding.bottom;
        return CellLayout.rectToCell(context.getResources(), requiredWidth, requiredHeight, null);
    }

    static int[] getSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minWidth, info.minHeight);
    }

    static int[] getMinSpanForWidget(Context context, AppWidgetProviderInfo info) {
        return getSpanForWidget(context, info.provider, info.minResizeWidth, info.minResizeHeight);
    }

    private void completeAddAppWidget(final int appWidgetId, long container, int screen, AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo) {
        boolean foundCellSpan;
        if (appWidgetInfo == null) {
            appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        }
        CellLayout layout = getCellLayout(container, screen);
        int[] minSpanXY = getMinSpanForWidget(this, appWidgetInfo);
        int[] spanXY = getSpanForWidget(this, appWidgetInfo);
        int[] cellXY = this.mTmpAddItemCellCoordinates;
        int[] touchXY = this.mPendingAddInfo.dropPos;
        int[] finalSpan = new int[2];
        if (this.mPendingAddInfo.cellX >= 0 && this.mPendingAddInfo.cellY >= 0) {
            cellXY[0] = this.mPendingAddInfo.cellX;
            cellXY[1] = this.mPendingAddInfo.cellY;
            spanXY[0] = this.mPendingAddInfo.spanX;
            spanXY[1] = this.mPendingAddInfo.spanY;
            foundCellSpan = true;
        } else if (touchXY != null) {
            int[] result = layout.findNearestVacantArea(touchXY[0], touchXY[1], minSpanXY[0], minSpanXY[1], spanXY[0], spanXY[1], cellXY, finalSpan);
            spanXY[0] = finalSpan[0];
            spanXY[1] = finalSpan[1];
            foundCellSpan = result != null;
        } else {
            foundCellSpan = layout.findCellForSpan(cellXY, minSpanXY[0], minSpanXY[1]);
        }
        if (!foundCellSpan) {
            if (appWidgetId != -1) {
                new Thread("deleteAppWidgetId") {
                    @Override
                    public void run() {
                        Launcher.this.mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                    }
                }.start();
            }
            showOutOfSpaceMessage(isHotseatLayout(layout));
            return;
        }
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId, appWidgetInfo.provider);
        launcherInfo.spanX = spanXY[0];
        launcherInfo.spanY = spanXY[1];
        launcherInfo.minSpanX = this.mPendingAddInfo.minSpanX;
        launcherInfo.minSpanY = this.mPendingAddInfo.minSpanY;
        launcherInfo.user = appWidgetInfo.getProfile();
        LauncherModel.addItemToDatabase(this, launcherInfo, container, screen, cellXY[0], cellXY[1], false);
        if (!this.mRestoring) {
            if (hostView == null) {
                launcherInfo.hostView = this.mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            } else {
                launcherInfo.hostView = hostView;
            }
            launcherInfo.hostView.setTag(launcherInfo);
            launcherInfo.hostView.setVisibility(0);
            launcherInfo.notifyWidgetSizeChanged(this);
            this.mWorkspace.addInScreen(launcherInfo.hostView, container, screen, cellXY[0], cellXY[1], launcherInfo.spanX, launcherInfo.spanY, isWorkspaceLocked());
            addWidgetToAutoAdvanceIfNeeded(launcherInfo.hostView, appWidgetInfo);
        }
        resetAddInfo();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
        filter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
        registerReceiver(this.mReceiver, filter);
        FirstFrameAnimatorHelper.initializeDrawListener(getWindow().getDecorView());
        this.mAttached = true;
        this.mVisible = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mVisible = false;
        if (this.mAttached) {
            unregisterReceiver(this.mReceiver);
            this.mAttached = false;
        }
        updateRunning();
    }

    public void onWindowVisibilityChanged(int visibility) {
        this.mVisible = visibility == 0;
        updateRunning();
        if (this.mVisible) {
            this.mAppsCustomizeTabHost.onWindowVisible();
            if (!this.mWorkspaceLoading) {
                ViewTreeObserver observer = this.mWorkspace.getViewTreeObserver();
                observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
                    private boolean mStarted = false;

                    @Override
                    public void onDraw() {
                        if (!this.mStarted) {
                            this.mStarted = true;
                            Launcher.this.mWorkspace.postDelayed(Launcher.this.mBuildLayersRunnable, 500L);
                            Launcher.this.mWorkspace.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (Launcher.this.mWorkspace != null && Launcher.this.mWorkspace.getViewTreeObserver() != null) {
                                        Launcher.this.mWorkspace.getViewTreeObserver().removeOnDrawListener(this);
                                    }
                                }
                            });
                        }
                    }
                });
            }
            updateAppMarketIcon();
            clearTypedText();
        }
    }

    private void sendAdvanceMessage(long delay) {
        this.mHandler.removeMessages(1);
        Message msg = this.mHandler.obtainMessage(1);
        this.mHandler.sendMessageDelayed(msg, delay);
        this.mAutoAdvanceSentTime = System.currentTimeMillis();
    }

    private void updateRunning() {
        boolean autoAdvanceRunning = this.mVisible && this.mUserPresent && !this.mWidgetsToAdvance.isEmpty();
        if (autoAdvanceRunning != this.mAutoAdvanceRunning) {
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
    }

    void addWidgetToAutoAdvanceIfNeeded(View hostView, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo != null && appWidgetInfo.autoAdvanceViewId != -1) {
            KeyEvent.Callback callbackFindViewById = hostView.findViewById(appWidgetInfo.autoAdvanceViewId);
            if (callbackFindViewById instanceof Advanceable) {
                this.mWidgetsToAdvance.put(hostView, appWidgetInfo);
                ((Advanceable) callbackFindViewById).fyiWillBeAdvancedByHostKThx();
                updateRunning();
            }
        }
    }

    void removeWidgetToAutoAdvance(View hostView) {
        if (this.mWidgetsToAdvance.containsKey(hostView)) {
            this.mWidgetsToAdvance.remove(hostView);
            updateRunning();
        }
    }

    public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
        removeWidgetToAutoAdvance(launcherInfo.hostView);
        launcherInfo.hostView = null;
    }

    void showOutOfSpaceMessage(boolean isHotseatLayout) {
        int strId = isHotseatLayout ? R.string.hotseat_out_of_space : R.string.out_of_space;
        Toast.makeText(this, getString(strId), 0).show();
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return this.mAppWidgetHost;
    }

    public LauncherModel getModel() {
        return this.mModel;
    }

    void closeSystemDialogs() {
        getWindow().closeAllPanels();
        this.mWaitingForResult = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if ("android.intent.action.MAIN".equals(intent.getAction())) {
            closeSystemDialogs();
            final boolean alreadyOnHome = (intent.getFlags() & 4194304) != 4194304;
            Runnable processIntent = new Runnable() {
                @Override
                public void run() {
                    if (Launcher.this.mWorkspace != null) {
                        Folder openFolder = Launcher.this.mWorkspace.getOpenFolder();
                        Launcher.this.mWorkspace.exitWidgetResizeMode();
                        if (alreadyOnHome && Launcher.this.mState == State.WORKSPACE && !Launcher.this.mWorkspace.isTouchActive() && openFolder == null) {
                            Launcher.this.mWorkspace.moveToDefaultScreen(true);
                        }
                        Launcher.this.closeFolder();
                        Launcher.this.exitSpringLoadedDragMode();
                        if (alreadyOnHome) {
                            Launcher.this.showWorkspace(true);
                        } else {
                            Launcher.this.mOnResumeState = State.WORKSPACE;
                        }
                        View v = Launcher.this.getWindow().peekDecorView();
                        if (v != null && v.getWindowToken() != null) {
                            InputMethodManager imm = (InputMethodManager) Launcher.this.getSystemService("input_method");
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                        if (!alreadyOnHome && Launcher.this.mAppsCustomizeTabHost != null) {
                            Launcher.this.mAppsCustomizeTabHost.reset();
                        }
                    }
                }
            };
            if (alreadyOnHome && !this.mWorkspace.hasWindowFocus()) {
                this.mWorkspace.postDelayed(processIntent, 350L);
            } else {
                processIntent.run();
            }
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        Iterator<Integer> it = this.mSynchronouslyBoundPages.iterator();
        while (it.hasNext()) {
            int page = it.next().intValue();
            this.mWorkspace.restoreInstanceStateForChild(page);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("launcher.current_screen", this.mWorkspace.getNextPage());
        super.onSaveInstanceState(outState);
        outState.putInt("launcher.state", this.mState.ordinal());
        closeFolder();
        if (this.mPendingAddInfo.container != -1 && this.mPendingAddInfo.screen > -1 && this.mWaitingForResult) {
            outState.putLong("launcher.add_container", this.mPendingAddInfo.container);
            outState.putInt("launcher.add_screen", this.mPendingAddInfo.screen);
            outState.putInt("launcher.add_cell_x", this.mPendingAddInfo.cellX);
            outState.putInt("launcher.add_cell_y", this.mPendingAddInfo.cellY);
            outState.putInt("launcher.add_span_x", this.mPendingAddInfo.spanX);
            outState.putInt("launcher.add_span_y", this.mPendingAddInfo.spanY);
            outState.putParcelable("launcher.add_widget_info", this.mPendingAddWidgetInfo);
            outState.putInt("launcher.add_widget_id", this.mPendingAddWidgetId);
        }
        if (this.mFolderInfo != null && this.mWaitingForResult) {
            outState.putBoolean("launcher.rename_folder", true);
            outState.putLong("launcher.rename_folder_id", this.mFolderInfo.id);
        }
        if (this.mAppsCustomizeTabHost != null) {
            String currentTabTag = this.mAppsCustomizeTabHost.getCurrentTabTag();
            if (currentTabTag != null) {
                outState.putString("apps_customize_currentTab", currentTabTag);
            }
            int currentIndex = this.mAppsCustomizeContent.getSaveInstanceStateIndex();
            outState.putInt("apps_customize_currentIndex", currentIndex);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(0);
        this.mWorkspace.removeCallbacks(this.mBuildLayersRunnable);
        LauncherApplication app = (LauncherApplication) getApplication();
        this.mModel.stopLoader();
        app.setLauncher(null);
        try {
            this.mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            Log.w("Launcher", "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }
        this.mAppWidgetHost = null;
        this.mWidgetsToAdvance.clear();
        TextKeyListener.getInstance().release();
        if (this.mModel != null) {
            this.mModel.unbindItemInfosAndClearQueuedBindRunnables();
        }
        getContentResolver().unregisterContentObserver(this.mWidgetObserver);
        unregisterReceiver(this.mCloseSystemDialogsReceiver);
        this.mDragLayer.clearAllResizeFrames();
        ((ViewGroup) this.mWorkspace.getParent()).removeAllViews();
        this.mWorkspace.removeAllViews();
        this.mWorkspace = null;
        this.mDragController = null;
        LauncherAnimUtils.onDestroyActivity();
    }

    public DragController getDragController() {
        return this.mDragController;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0) {
            this.mWaitingForResult = true;
        }
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, boolean globalSearch) {
        showWorkspace(true);
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
        startGlobalSearch(initialQuery, selectInitialQuery, appSearchData, sourceBounds);
    }

    public void startGlobalSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData, Rect sourceBounds) {
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
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("Launcher", "Global search activity not found: " + globalSearchActivity);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isWorkspaceLocked()) {
            return false;
        }
        super.onCreateOptionsMenu(menu);
        Intent manageApps = new Intent("android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS");
        manageApps.setFlags(276824064);
        Intent settings = new Intent("android.settings.SETTINGS");
        settings.setFlags(270532608);
        String helpUrl = getString(R.string.help_url);
        Intent help = new Intent("android.intent.action.VIEW", Uri.parse(helpUrl));
        help.setFlags(276824064);
        menu.add(1, 2, 0, R.string.menu_wallpaper).setIcon(android.R.drawable.ic_menu_gallery).setAlphabeticShortcut('W');
        menu.add(0, 3, 0, R.string.menu_manage_apps).setIcon(android.R.drawable.ic_menu_manage).setIntent(manageApps).setAlphabeticShortcut('M');
        menu.add(0, 4, 0, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences).setIntent(settings).setAlphabeticShortcut('P');
        if (!helpUrl.isEmpty()) {
            menu.add(0, 5, 0, R.string.menu_help).setIcon(android.R.drawable.ic_menu_help).setIntent(help).setAlphabeticShortcut('H');
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (this.mAppsCustomizeTabHost.isTransitioning()) {
            return false;
        }
        boolean allAppsVisible = this.mAppsCustomizeTabHost.getVisibility() == 0;
        menu.setGroupVisible(1, allAppsVisible ? false : true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                startWallpaper();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        return true;
    }

    public boolean isWorkspaceLocked() {
        return this.mWorkspaceLoading || this.mWaitingForResult;
    }

    private void resetAddInfo() {
        this.mPendingAddInfo.container = -1L;
        this.mPendingAddInfo.screen = -1;
        ItemInfo itemInfo = this.mPendingAddInfo;
        this.mPendingAddInfo.cellY = -1;
        itemInfo.cellX = -1;
        ItemInfo itemInfo2 = this.mPendingAddInfo;
        this.mPendingAddInfo.spanY = -1;
        itemInfo2.spanX = -1;
        ItemInfo itemInfo3 = this.mPendingAddInfo;
        this.mPendingAddInfo.minSpanY = -1;
        itemInfo3.minSpanX = -1;
        this.mPendingAddInfo.dropPos = null;
    }

    void addAppWidgetImpl(int appWidgetId, ItemInfo info, AppWidgetHostView boundWidget, AppWidgetProviderInfo appWidgetInfo) {
        if (appWidgetInfo.configure != null) {
            this.mPendingAddWidgetInfo = appWidgetInfo;
            this.mPendingAddWidgetId = appWidgetId;
            startAppWidgetConfigureActivitySafely(appWidgetId);
        } else {
            completeAddAppWidget(appWidgetId, info.container, info.screen, boundWidget, appWidgetInfo);
            exitSpringLoadedDragModeDelayed(true, false, null);
        }
    }

    void processShortcutFromDrop(ComponentName componentName, long container, int screen, int[] cell, int[] loc) {
        resetAddInfo();
        this.mPendingAddInfo.container = container;
        this.mPendingAddInfo.screen = screen;
        this.mPendingAddInfo.dropPos = loc;
        if (cell != null) {
            this.mPendingAddInfo.cellX = cell[0];
            this.mPendingAddInfo.cellY = cell[1];
        }
        Intent createShortcutIntent = new Intent("android.intent.action.CREATE_SHORTCUT");
        createShortcutIntent.setComponent(componentName);
        processShortcut(createShortcutIntent);
    }

    void addAppWidgetFromDrop(PendingAddWidgetInfo info, long container, int screen, int[] cell, int[] span, int[] loc) {
        resetAddInfo();
        ItemInfo itemInfo = this.mPendingAddInfo;
        info.container = container;
        itemInfo.container = container;
        ItemInfo itemInfo2 = this.mPendingAddInfo;
        info.screen = screen;
        itemInfo2.screen = screen;
        this.mPendingAddInfo.dropPos = loc;
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
            addAppWidgetImpl(hostView.getAppWidgetId(), info, hostView, info.info);
            return;
        }
        int appWidgetId = getAppWidgetHost().allocateAppWidgetId();
        boolean success = this.mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.info.getProfile(), info.componentName, info.bindOptions);
        if (success) {
            addAppWidgetImpl(appWidgetId, info, null, info.info);
            return;
        }
        this.mPendingAddWidgetInfo = info.info;
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_BIND");
        intent.putExtra("appWidgetId", appWidgetId);
        intent.putExtra("appWidgetProvider", info.componentName);
        intent.putExtra("appWidgetProviderProfile", info.info.getProfile());
        startActivityForResult(intent, 11);
    }

    void processShortcut(Intent intent) {
        String applicationName = getResources().getString(R.string.group_applications);
        Object shortcutName = intent.getStringExtra("android.intent.extra.shortcut.NAME");
        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            Intent pickIntent = new Intent("android.intent.action.PICK_ACTIVITY");
            pickIntent.putExtra("android.intent.extra.INTENT", mainIntent);
            pickIntent.putExtra("android.intent.extra.TITLE", getText(R.string.title_select_application));
            startActivityForResultSafely(pickIntent, 6);
            return;
        }
        startActivityForResultSafely(intent, 1);
    }

    FolderIcon addFolder(CellLayout layout, long container, int screen, int cellX, int cellY) {
        FolderInfo folderInfo = new FolderInfo();
        folderInfo.title = getText(R.string.folder_name);
        LauncherModel.addItemToDatabase(this, folderInfo, container, screen, cellX, cellY, false);
        sFolders.put(Long.valueOf(folderInfo.id), folderInfo);
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, layout, folderInfo, this.mIconCache);
        this.mWorkspace.addInScreen(newFolder, container, screen, cellX, cellY, 1, 1, isWorkspaceLocked());
        return newFolder;
    }

    void removeFolder(FolderInfo folder) {
        sFolders.remove(Long.valueOf(folder.id));
    }

    private void startWallpaper() {
        showWorkspace(true);
        Intent pickWallpaper = new Intent("android.intent.action.SET_WALLPAPER");
        Intent chooser = Intent.createChooser(pickWallpaper, getText(R.string.chooser_wallpaper));
        startActivityForResult(chooser, 10);
    }

    private void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true, this.mWidgetObserver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == 0) {
            Log.d("Launcher", "Down key was pressed ");
            if (SystemProperties.getBoolean("ril.sim.userActivityEvent", false)) {
                Intent intent = new Intent("android.intent.action.stk.event_list_action");
                intent.putExtra("STK EVENT", 4);
                intent.putExtra("EVENT LENGTH", 0);
                sendBroadcast(intent);
                Log.d("Launcher", "sendBroadcast STK User Activity Event intent !!!!!!!!!!!!!!! = " + intent);
            }
            switch (event.getKeyCode()) {
                case 3:
                    return true;
                case 25:
                    if (isPropertyEnabled("launcher_dump_state")) {
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
        if (isAllAppsVisible()) {
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

    private void onAppWidgetReset() {
        if (this.mAppWidgetHost != null) {
            this.mAppWidgetHost.startListening();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getWindowToken() != null && this.mWorkspace.isFinishedSwitchingState()) {
            Object tag = v.getTag();
            if (tag instanceof ShortcutInfo) {
                Intent intent = ((ShortcutInfo) tag).intent;
                int[] pos = new int[2];
                v.getLocationOnScreen(pos);
                intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight()));
                boolean success = startActivitySafely(v, intent, tag);
                if (success && (v instanceof BubbleTextView)) {
                    this.mWaitingForResume = (BubbleTextView) v;
                    this.mWaitingForResume.setStayPressed(true);
                    return;
                }
                return;
            }
            if (tag instanceof FolderInfo) {
                if (v instanceof FolderIcon) {
                    FolderIcon fi = (FolderIcon) v;
                    handleFolderClick(fi);
                    return;
                }
                return;
            }
            if (v == this.mAllAppsButton) {
                if (isAllAppsVisible()) {
                    showWorkspace(true);
                } else {
                    onClickAllAppsButton(v);
                }
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        showWorkspace(true);
        return false;
    }

    public void onClickSearchButton(View v) {
        v.performHapticFeedback(1);
        onSearchRequested();
    }

    public void onClickVoiceButton(View v) {
        v.performHapticFeedback(1);
        try {
            SearchManager searchManager = (SearchManager) getSystemService("search");
            ComponentName activityName = searchManager.getGlobalSearchActivity();
            Intent intent = new Intent("android.speech.action.WEB_SEARCH");
            intent.setFlags(268435456);
            if (activityName != null) {
                intent.setPackage(activityName.getPackageName());
            }
            startActivity(null, intent, "onClickVoiceButton");
        } catch (ActivityNotFoundException e) {
            Intent intent2 = new Intent("android.speech.action.WEB_SEARCH");
            intent2.setFlags(268435456);
            startActivitySafely(null, intent2, "onClickVoiceButton");
        }
    }

    public void onClickAllAppsButton(View v) {
        showAllApps(true);
    }

    public void onTouchDownAllAppsButton(View v) {
        v.performHapticFeedback(1);
    }

    public void onClickAppMarketButton(View v) {
        if (this.mAppMarketIntent != null) {
            startActivitySafely(v, this.mAppMarketIntent, "app market");
        } else {
            Log.e("Launcher", "Invalid app market intent.");
        }
    }

    void startApplicationDetailsActivity(ComponentName componentName, UserHandle user) {
        LauncherApps launcherApps = (LauncherApps) getSystemService("launcherapps");
        try {
            launcherApps.startAppDetailsActivity(componentName, user, null, null);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Unable to launch settings");
        } catch (SecurityException e2) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Launcher does not have permission to launch settings");
        }
    }

    void startApplicationUninstallActivity(ApplicationInfo appInfo, UserHandle user) {
        if ((appInfo.flags & 1) == 0) {
            Toast.makeText(this, R.string.uninstall_system_app_text, 0).show();
            return;
        }
        String packageName = appInfo.componentName.getPackageName();
        String className = appInfo.componentName.getClassName();
        Intent intent = new Intent("android.intent.action.DELETE", Uri.fromParts("package", packageName, className));
        intent.setFlags(276824064);
        if (user != null) {
            intent.putExtra("android.intent.extra.USER", user);
        }
        startActivity(intent);
    }

    boolean startActivity(View v, Intent intent, Object tag) {
        boolean useLaunchAnimation;
        intent.addFlags(268435456);
        if (v != null) {
            try {
                useLaunchAnimation = !intent.hasExtra("com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION");
            } catch (SecurityException e) {
                Toast.makeText(this, R.string.activity_not_found, 0).show();
                Log.e("Launcher", "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity or use the exported attribute for this activity. tag=" + tag + " intent=" + intent, e);
                return false;
            }
        }
        UserHandle user = (UserHandle) intent.getParcelableExtra("profile");
        LauncherApps launcherApps = (LauncherApps) getSystemService("launcherapps");
        if (useLaunchAnimation) {
            ActivityOptions opts = ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            if (user == null || user.equals(Process.myUserHandle())) {
                startActivity(intent, opts.toBundle());
            } else {
                launcherApps.startMainActivity(intent.getComponent(), user, intent.getSourceBounds(), opts.toBundle());
            }
        } else if (user == null || user.equals(Process.myUserHandle())) {
            startActivity(intent);
        } else {
            launcherApps.startMainActivity(intent.getComponent(), user, intent.getSourceBounds(), null);
        }
        return true;
    }

    boolean startActivitySafely(View v, Intent intent, Object tag) {
        try {
            boolean success = startActivity(v, intent, tag);
            return success;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Unable to launch. tag=" + tag + " intent=" + intent, e);
            return false;
        }
    }

    void startAppWidgetConfigureActivitySafely(int appWidgetId) {
        try {
            this.mAppWidgetHost.startAppWidgetConfigureActivityForResult(this, appWidgetId, 0, 5, null);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
        }
    }

    void startActivityForResultSafely(Intent intent, int requestCode) {
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
        } catch (SecurityException e2) {
            Toast.makeText(this, R.string.activity_not_found, 0).show();
            Log.e("Launcher", "Launcher does not have the permission to launch " + intent + ". Make sure to create a MAIN intent-filter for the corresponding activity or use the exported attribute for this activity.", e2);
        }
    }

    private void handleFolderClick(FolderIcon folderIcon) {
        FolderInfo info = folderIcon.getFolderInfo();
        Folder openFolder = this.mWorkspace.getFolderForTag(info);
        if (info.opened && openFolder == null) {
            Log.d("Launcher", "Folder info marked as open, but associated folder is not open. Screen: " + info.screen + " (" + info.cellX + ", " + info.cellY + ")");
            info.opened = false;
        }
        if (!info.opened && !folderIcon.getFolder().isDestroyed()) {
            closeFolder();
            openFolder(folderIcon);
        } else if (openFolder != null) {
            int folderScreen = this.mWorkspace.getPageForView(openFolder);
            closeFolder(openFolder);
            if (folderScreen != this.mWorkspace.getCurrentPage()) {
                closeFolder();
                openFolder(folderIcon);
            }
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
        if (fi.getFolder() != null) {
            fi.getFolder().bringToFront();
        }
    }

    private void growAndFadeOutFolderIcon(FolderIcon fi) {
        if (fi != null) {
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
            oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
            oa.start();
        }
    }

    private void shrinkAndFadeInFolderIcon(final FolderIcon fi) {
        if (fi != null) {
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 1.0f);
            PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
            PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);
            final CellLayout cl = (CellLayout) fi.getParent().getParent();
            this.mDragLayer.removeView(this.mFolderIconImageView);
            copyFolderIconToImage(fi);
            ObjectAnimator oa = LauncherAnimUtils.ofPropertyValuesHolder(this.mFolderIconImageView, alpha, scaleX, scaleY);
            oa.setDuration(getResources().getInteger(R.integer.config_folderAnimDuration));
            oa.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (cl != null) {
                        cl.clearFolderLeaveBehind();
                        Launcher.this.mDragLayer.removeView(Launcher.this.mFolderIconImageView);
                        fi.setVisibility(0);
                    }
                }
            });
            oa.start();
        }
    }

    public void openFolder(FolderIcon folderIcon) {
        Folder folder = folderIcon.getFolder();
        FolderInfo info = folder.mInfo;
        info.opened = true;
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
        Folder folder = this.mWorkspace.getOpenFolder();
        if (folder != null) {
            if (folder.isEditingName()) {
                folder.dismissEditingName();
            }
            closeFolder(folder);
            dismissFolderCling(null);
        }
    }

    void closeFolder(Folder folder) {
        folder.getInfo().opened = false;
        ViewGroup parent = (ViewGroup) folder.getParent().getParent();
        if (parent != null) {
            FolderIcon fi = (FolderIcon) this.mWorkspace.getViewForTag(folder.mInfo);
            shrinkAndFadeInFolderIcon(fi);
        }
        folder.animateClosed();
        getDragLayer().sendAccessibilityEvent(32);
    }

    @Override
    public boolean onLongClick(View v) {
        if (!isDraggingEnabled() || isWorkspaceLocked() || this.mState != State.WORKSPACE) {
            return false;
        }
        if (!(v instanceof CellLayout)) {
            v = (View) v.getParent().getParent();
        }
        resetAddInfo();
        CellLayout.CellInfo longClickCellInfo = (CellLayout.CellInfo) v.getTag();
        if (longClickCellInfo == null) {
            return true;
        }
        View itemUnderLongClick = longClickCellInfo.cell;
        boolean allowLongPress = isHotseatLayout(v) || this.mWorkspace.allowLongPress();
        if (allowLongPress && !this.mDragController.isDragging() && itemUnderLongClick != null && !(itemUnderLongClick instanceof Folder)) {
            this.mWorkspace.startDrag(longClickCellInfo);
        }
        return true;
    }

    boolean isHotseatLayout(View layout) {
        return this.mHotseat != null && layout != null && (layout instanceof CellLayout) && layout == this.mHotseat.getLayout();
    }

    Hotseat getHotseat() {
        return this.mHotseat;
    }

    SearchDropTargetBar getSearchBar() {
        return this.mSearchDropTargetBar;
    }

    CellLayout getCellLayout(long container, int screen) {
        if (container == -101) {
            if (this.mHotseat != null) {
                return this.mHotseat.getLayout();
            }
            return null;
        }
        return (CellLayout) this.mWorkspace.getChildAt(screen);
    }

    Workspace getWorkspace() {
        return this.mWorkspace;
    }

    @Override
    public boolean isAllAppsVisible() {
        return this.mState == State.APPS_CUSTOMIZE || this.mOnResumeState == State.APPS_CUSTOMIZE;
    }

    @Override
    public boolean isAllAppsButtonRank(int rank) {
        return this.mHotseat.isAllAppsButtonRank(rank);
    }

    private void setPivotsForZoom(View view, float scaleFactor) {
        view.setPivotX(view.getWidth() / 2.0f);
        view.setPivotY(view.getHeight() / 2.0f);
    }

    void disableWallpaperIfInAllApps() {
        if (isAllAppsVisible() && this.mAppsCustomizeTabHost != null && !this.mAppsCustomizeTabHost.isTransitioning()) {
            updateWallpaperVisibility(false);
        }
    }

    private void setWorkspaceBackground(boolean workspace) {
        this.mLauncherView.setBackground(workspace ? this.mWorkspaceBackgroundDrawable : null);
    }

    void updateWallpaperVisibility(boolean visible) {
        int wpflags = visible ? 1048576 : 0;
        int curflags = getWindow().getAttributes().flags & 1048576;
        if (wpflags != curflags) {
            getWindow().setFlags(wpflags, 1048576);
        }
        setWorkspaceBackground(visible);
    }

    private void dispatchOnLauncherTransitionPrepare(View view, boolean animated, boolean toWorkspace) {
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionPrepare(this, animated, toWorkspace);
        }
    }

    private void dispatchOnLauncherTransitionStart(View view, boolean animated, boolean toWorkspace) {
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionStart(this, animated, toWorkspace);
        }
        dispatchOnLauncherTransitionStep(view, 0.0f);
    }

    private void dispatchOnLauncherTransitionStep(View view, float t) {
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionStep(this, t);
        }
    }

    private void dispatchOnLauncherTransitionEnd(View view, boolean animated, boolean toWorkspace) {
        if (view instanceof LauncherTransitionable) {
            ((LauncherTransitionable) view).onLauncherTransitionEnd(this, animated, toWorkspace);
        }
        dispatchOnLauncherTransitionStep(view, 1.0f);
    }

    private void showAppsCustomizeHelper(final boolean animated, final boolean springLoaded) {
        if (this.mStateAnimation != null) {
            this.mStateAnimation.setDuration(0L);
            this.mStateAnimation.cancel();
            this.mStateAnimation = null;
        }
        Resources res = getResources();
        int duration = res.getInteger(R.integer.config_appsCustomizeZoomInTime);
        int fadeDuration = res.getInteger(R.integer.config_appsCustomizeFadeInTime);
        final float scale = res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = this.mWorkspace;
        final AppsCustomizeTabHost toView = this.mAppsCustomizeTabHost;
        int startDelay = res.getInteger(R.integer.config_workspaceAppsCustomizeAnimationStagger);
        setPivotsForZoom(toView, scale);
        Animator workspaceAnim = this.mWorkspace.getChangeStateAnimation(Workspace.State.SMALL, animated);
        if (animated) {
            toView.setScaleX(scale);
            toView.setScaleY(scale);
            LauncherViewPropertyAnimator scaleAnim = new LauncherViewPropertyAnimator(toView);
            scaleAnim.scaleX(1.0f).scaleY(1.0f).setDuration(duration).setInterpolator(new Workspace.ZoomOutInterpolator());
            toView.setVisibility(0);
            toView.setAlpha(0.0f);
            ObjectAnimator alphaAnim = LauncherAnimUtils.ofFloat(toView, "alpha", 0.0f, 1.0f).setDuration(fadeDuration);
            alphaAnim.setInterpolator(new DecelerateInterpolator(1.5f));
            alphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (animation == null) {
                        throw new RuntimeException("animation is null");
                    }
                    float t = ((Float) animation.getAnimatedValue()).floatValue();
                    Launcher.this.dispatchOnLauncherTransitionStep(fromView, t);
                    Launcher.this.dispatchOnLauncherTransitionStep(toView, t);
                }
            });
            this.mStateAnimation = LauncherAnimUtils.createAnimatorSet();
            this.mStateAnimation.play(scaleAnim).after(startDelay);
            this.mStateAnimation.play(alphaAnim).after(startDelay);
            this.mStateAnimation.addListener(new AnimatorListenerAdapter() {
                boolean animationCancelled = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    Launcher.this.updateWallpaperVisibility(true);
                    toView.setTranslationX(0.0f);
                    toView.setTranslationY(0.0f);
                    toView.setVisibility(0);
                    toView.bringToFront();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    Launcher.this.dispatchOnLauncherTransitionEnd(fromView, animated, false);
                    Launcher.this.dispatchOnLauncherTransitionEnd(toView, animated, false);
                    if (Launcher.this.mWorkspace != null && !springLoaded && !LauncherApplication.isScreenLarge()) {
                        Launcher.this.mWorkspace.hideScrollingIndicator(true);
                        Launcher.this.hideDockDivider();
                    }
                    if (!this.animationCancelled) {
                        Launcher.this.updateWallpaperVisibility(false);
                    }
                    if (Launcher.this.mSearchDropTargetBar != null) {
                        Launcher.this.mSearchDropTargetBar.hideSearchBar(false);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    this.animationCancelled = true;
                }
            });
            if (workspaceAnim != null) {
                this.mStateAnimation.play(workspaceAnim);
            }
            boolean delayAnim = false;
            dispatchOnLauncherTransitionPrepare(fromView, animated, false);
            dispatchOnLauncherTransitionPrepare(toView, animated, false);
            if (toView.getContent().getMeasuredWidth() == 0 || this.mWorkspace.getMeasuredWidth() == 0 || toView.getMeasuredWidth() == 0) {
                delayAnim = true;
            }
            final AnimatorSet stateAnimation = this.mStateAnimation;
            final Runnable startAnimRunnable = new Runnable() {
                @Override
                public void run() {
                    if (Launcher.this.mStateAnimation == stateAnimation) {
                        Launcher.this.setPivotsForZoom(toView, scale);
                        Launcher.this.dispatchOnLauncherTransitionStart(fromView, animated, false);
                        Launcher.this.dispatchOnLauncherTransitionStart(toView, animated, false);
                        LauncherAnimUtils.startAnimationAfterNextDraw(Launcher.this.mStateAnimation, toView);
                    }
                }
            };
            if (delayAnim) {
                ViewTreeObserver observer = toView.getViewTreeObserver();
                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        startAnimRunnable.run();
                        toView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
                return;
            } else {
                startAnimRunnable.run();
                return;
            }
        }
        toView.setTranslationX(0.0f);
        toView.setTranslationY(0.0f);
        toView.setScaleX(1.0f);
        toView.setScaleY(1.0f);
        toView.setVisibility(0);
        toView.bringToFront();
        if (!springLoaded && !LauncherApplication.isScreenLarge()) {
            this.mWorkspace.hideScrollingIndicator(true);
            hideDockDivider();
            if (this.mSearchDropTargetBar != null) {
                this.mSearchDropTargetBar.hideSearchBar(false);
            }
        }
        dispatchOnLauncherTransitionPrepare(fromView, animated, false);
        dispatchOnLauncherTransitionStart(fromView, animated, false);
        dispatchOnLauncherTransitionEnd(fromView, animated, false);
        dispatchOnLauncherTransitionPrepare(toView, animated, false);
        dispatchOnLauncherTransitionStart(toView, animated, false);
        dispatchOnLauncherTransitionEnd(toView, animated, false);
        updateWallpaperVisibility(false);
    }

    private void hideAppsCustomizeHelper(State toState, final boolean animated, boolean springLoaded, final Runnable onCompleteRunnable) {
        if (this.mStateAnimation != null) {
            this.mStateAnimation.setDuration(0L);
            this.mStateAnimation.cancel();
            this.mStateAnimation = null;
        }
        Resources res = getResources();
        int duration = res.getInteger(R.integer.config_appsCustomizeZoomOutTime);
        int fadeOutDuration = res.getInteger(R.integer.config_appsCustomizeFadeOutTime);
        float scaleFactor = res.getInteger(R.integer.config_appsCustomizeZoomScaleFactor);
        final View fromView = this.mAppsCustomizeTabHost;
        final View toView = this.mWorkspace;
        Animator workspaceAnim = null;
        if (toState == State.WORKSPACE) {
            int stagger = res.getInteger(R.integer.config_appsCustomizeWorkspaceAnimationStagger);
            workspaceAnim = this.mWorkspace.getChangeStateAnimation(Workspace.State.NORMAL, animated, stagger);
        } else if (toState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            workspaceAnim = this.mWorkspace.getChangeStateAnimation(Workspace.State.SPRING_LOADED, animated);
        }
        setPivotsForZoom(fromView, scaleFactor);
        updateWallpaperVisibility(true);
        showHotseat(animated);
        if (animated) {
            LauncherViewPropertyAnimator scaleAnim = new LauncherViewPropertyAnimator(fromView);
            scaleAnim.scaleX(scaleFactor).scaleY(scaleFactor).setDuration(duration).setInterpolator(new Workspace.ZoomInInterpolator());
            ObjectAnimator alphaAnim = LauncherAnimUtils.ofFloat(fromView, "alpha", 1.0f, 0.0f).setDuration(fadeOutDuration);
            alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            alphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float t = 1.0f - ((Float) animation.getAnimatedValue()).floatValue();
                    Launcher.this.dispatchOnLauncherTransitionStep(fromView, t);
                    Launcher.this.dispatchOnLauncherTransitionStep(toView, t);
                }
            });
            this.mStateAnimation = LauncherAnimUtils.createAnimatorSet();
            dispatchOnLauncherTransitionPrepare(fromView, animated, true);
            dispatchOnLauncherTransitionPrepare(toView, animated, true);
            this.mAppsCustomizeContent.pauseScrolling();
            this.mStateAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    Launcher.this.updateWallpaperVisibility(true);
                    fromView.setVisibility(8);
                    Launcher.this.dispatchOnLauncherTransitionEnd(fromView, animated, true);
                    Launcher.this.dispatchOnLauncherTransitionEnd(toView, animated, true);
                    if (Launcher.this.mWorkspace != null) {
                        Launcher.this.mWorkspace.hideScrollingIndicator(false);
                    }
                    if (onCompleteRunnable != null) {
                        onCompleteRunnable.run();
                    }
                    Launcher.this.mAppsCustomizeContent.updateCurrentPageScroll();
                    Launcher.this.mAppsCustomizeContent.resumeScrolling();
                }
            });
            this.mStateAnimation.playTogether(scaleAnim, alphaAnim);
            if (workspaceAnim != null) {
                this.mStateAnimation.play(workspaceAnim);
            }
            dispatchOnLauncherTransitionStart(fromView, animated, true);
            dispatchOnLauncherTransitionStart(toView, animated, true);
            LauncherAnimUtils.startAnimationAfterNextDraw(this.mStateAnimation, toView);
            return;
        }
        fromView.setVisibility(8);
        dispatchOnLauncherTransitionPrepare(fromView, animated, true);
        dispatchOnLauncherTransitionStart(fromView, animated, true);
        dispatchOnLauncherTransitionEnd(fromView, animated, true);
        dispatchOnLauncherTransitionPrepare(toView, animated, true);
        dispatchOnLauncherTransitionStart(toView, animated, true);
        dispatchOnLauncherTransitionEnd(toView, animated, true);
        this.mWorkspace.hideScrollingIndicator(false);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= 60) {
            this.mAppsCustomizeTabHost.onTrimMemory();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus) {
            updateWallpaperVisibility(true);
        } else {
            this.mWorkspace.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Launcher.this.disableWallpaperIfInAllApps();
                }
            }, 500L);
        }
    }

    void showWorkspace(boolean animated) {
        showWorkspace(animated, null);
    }

    void showWorkspace(boolean animated, Runnable onCompleteRunnable) {
        boolean z = false;
        if (this.mState != State.WORKSPACE) {
            boolean wasInSpringLoadedMode = this.mState == State.APPS_CUSTOMIZE_SPRING_LOADED;
            this.mWorkspace.setVisibility(0);
            hideAppsCustomizeHelper(State.WORKSPACE, animated, false, onCompleteRunnable);
            if (this.mSearchDropTargetBar != null) {
                this.mSearchDropTargetBar.showSearchBar(wasInSpringLoadedMode);
            }
            if (animated && wasInSpringLoadedMode) {
                z = true;
            }
            showDockDivider(z);
            if (this.mAllAppsButton != null) {
                this.mAllAppsButton.requestFocus();
            }
        }
        this.mWorkspace.flashScrollingIndicator(animated);
        this.mState = State.WORKSPACE;
        this.mUserPresent = true;
        updateRunning();
        getWindow().getDecorView().sendAccessibilityEvent(32);
    }

    void showAllApps(boolean animated) {
        if (this.mState == State.WORKSPACE) {
            showAppsCustomizeHelper(animated, false);
            this.mAppsCustomizeTabHost.requestFocus();
            this.mState = State.APPS_CUSTOMIZE;
            this.mUserPresent = false;
            updateRunning();
            closeFolder();
            getWindow().getDecorView().sendAccessibilityEvent(32);
        }
    }

    void enterSpringLoadedDragMode() {
        if (isAllAppsVisible()) {
            hideAppsCustomizeHelper(State.APPS_CUSTOMIZE_SPRING_LOADED, true, true, null);
            hideDockDivider();
            this.mState = State.APPS_CUSTOMIZE_SPRING_LOADED;
        }
    }

    void exitSpringLoadedDragModeDelayed(final boolean successfulDrop, boolean extendedDelay, final Runnable onCompleteRunnable) {
        if (this.mState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (successfulDrop) {
                        Launcher.this.mAppsCustomizeTabHost.setVisibility(8);
                        Launcher.this.showWorkspace(true, onCompleteRunnable);
                    } else {
                        Launcher.this.exitSpringLoadedDragMode();
                    }
                }
            }, extendedDelay ? 600 : 300);
        }
    }

    void exitSpringLoadedDragMode() {
        if (this.mState == State.APPS_CUSTOMIZE_SPRING_LOADED) {
            showAppsCustomizeHelper(true, true);
            this.mState = State.APPS_CUSTOMIZE;
        }
    }

    void hideDockDivider() {
        if (this.mQsbDivider != null && this.mDockDivider != null) {
            this.mQsbDivider.setVisibility(4);
            this.mDockDivider.setVisibility(4);
        }
    }

    void showDockDivider(boolean animated) {
        if (this.mQsbDivider != null && this.mDockDivider != null) {
            this.mQsbDivider.setVisibility(0);
            this.mDockDivider.setVisibility(0);
            if (this.mDividerAnimator != null) {
                this.mDividerAnimator.cancel();
                this.mQsbDivider.setAlpha(1.0f);
                this.mDockDivider.setAlpha(1.0f);
                this.mDividerAnimator = null;
            }
            if (animated) {
                this.mDividerAnimator = LauncherAnimUtils.createAnimatorSet();
                this.mDividerAnimator.playTogether(LauncherAnimUtils.ofFloat(this.mQsbDivider, "alpha", 1.0f), LauncherAnimUtils.ofFloat(this.mDockDivider, "alpha", 1.0f));
                int duration = 0;
                if (this.mSearchDropTargetBar != null) {
                    duration = this.mSearchDropTargetBar.getTransitionInDuration();
                }
                this.mDividerAnimator.setDuration(duration);
                this.mDividerAnimator.start();
            }
        }
    }

    void lockAllApps() {
    }

    void showHotseat(boolean animated) {
        if (!LauncherApplication.isScreenLarge()) {
            if (!animated) {
                this.mHotseat.setAlpha(1.0f);
            } else if (this.mHotseat.getAlpha() != 1.0f) {
                int duration = 0;
                if (this.mSearchDropTargetBar != null) {
                    duration = this.mSearchDropTargetBar.getTransitionInDuration();
                }
                this.mHotseat.animate().alpha(1.0f).setDuration(duration);
            }
        }
    }

    private int getCurrentOrientationIndexForGlobalIcons() {
        switch (getResources().getConfiguration().orientation) {
            case 2:
                return 1;
            default:
                return 0;
        }
    }

    private Drawable getExternalPackageToolbarIcon(ComponentName activityName, String resourceName) {
        int iconResId;
        try {
            PackageManager packageManager = getPackageManager();
            Bundle metaData = packageManager.getActivityInfo(activityName, 128).metaData;
            if (metaData != null && (iconResId = metaData.getInt(resourceName)) != 0) {
                Resources res = packageManager.getResourcesForActivity(activityName);
                return res.getDrawable(iconResId);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Launcher", "Failed to load toolbar icon; " + activityName.flattenToShortString() + " not found", e);
        } catch (Resources.NotFoundException nfe) {
            Log.w("Launcher", "Failed to load toolbar icon from " + activityName.flattenToShortString(), nfe);
        }
        return null;
    }

    private Drawable.ConstantState updateTextButtonWithIconFromExternalActivity(int buttonId, ComponentName activityName, int fallbackDrawableId, String toolbarResourceName) {
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);
        Resources r = getResources();
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);
        TextView button = (TextView) findViewById(buttonId);
        if (toolbarIcon == null) {
            Drawable toolbarIcon2 = r.getDrawable(fallbackDrawableId);
            toolbarIcon2.setBounds(0, 0, w, h);
            if (button == null) {
                return null;
            }
            button.setCompoundDrawables(toolbarIcon2, null, null, null);
            return null;
        }
        toolbarIcon.setBounds(0, 0, w, h);
        if (button != null) {
            button.setCompoundDrawables(toolbarIcon, null, null, null);
        }
        return toolbarIcon.getConstantState();
    }

    private Drawable.ConstantState updateButtonWithIconFromExternalActivity(int buttonId, ComponentName activityName, int fallbackDrawableId, String toolbarResourceName) {
        ImageView button = (ImageView) findViewById(buttonId);
        Drawable toolbarIcon = getExternalPackageToolbarIcon(activityName, toolbarResourceName);
        if (button != null) {
            if (toolbarIcon == null) {
                button.setImageResource(fallbackDrawableId);
            } else {
                button.setImageDrawable(toolbarIcon);
            }
        }
        if (toolbarIcon != null) {
            return toolbarIcon.getConstantState();
        }
        return null;
    }

    private void updateTextButtonWithDrawable(int buttonId, Drawable d) {
        TextView button = (TextView) findViewById(buttonId);
        button.setCompoundDrawables(d, null, null, null);
    }

    private void updateButtonWithDrawable(int buttonId, Drawable.ConstantState d) {
        ImageView button = (ImageView) findViewById(buttonId);
        button.setImageDrawable(d.newDrawable(getResources()));
    }

    private void invalidatePressedFocusedStates(View container, View button) {
        if (container instanceof HolographicLinearLayout) {
            HolographicLinearLayout layout = (HolographicLinearLayout) container;
            layout.invalidatePressedFocusedStates();
        } else if (button instanceof HolographicImageView) {
            HolographicImageView view = (HolographicImageView) button;
            view.invalidatePressedFocusedStates();
        }
    }

    private boolean updateGlobalSearchIcon() {
        View searchButtonContainer = findViewById(R.id.search_button_container);
        ImageView searchButton = (ImageView) findViewById(R.id.search_button);
        View voiceButtonContainer = findViewById(R.id.voice_button_container);
        View voiceButton = findViewById(R.id.voice_button);
        View voiceButtonProxy = findViewById(R.id.voice_button_proxy);
        SearchManager searchManager = (SearchManager) getSystemService("search");
        ComponentName activityName = searchManager.getGlobalSearchActivity();
        if (activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            sGlobalSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.search_button, activityName, R.drawable.ic_home_search_normal_holo, "com.android.launcher.toolbar_search_icon");
            if (sGlobalSearchIcon[coi] == null) {
                sGlobalSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.search_button, activityName, R.drawable.ic_home_search_normal_holo, "com.android.launcher.toolbar_icon");
            }
            if (searchButtonContainer != null) {
                searchButtonContainer.setVisibility(0);
            }
            searchButton.setVisibility(0);
            invalidatePressedFocusedStates(searchButtonContainer, searchButton);
            return true;
        }
        if (searchButtonContainer != null) {
            searchButtonContainer.setVisibility(8);
        }
        if (voiceButtonContainer != null) {
            voiceButtonContainer.setVisibility(8);
        }
        searchButton.setVisibility(8);
        voiceButton.setVisibility(8);
        if (voiceButtonProxy == null) {
            return false;
        }
        voiceButtonProxy.setVisibility(8);
        return false;
    }

    private void updateGlobalSearchIcon(Drawable.ConstantState d) {
        View searchButtonContainer = findViewById(R.id.search_button_container);
        View searchButton = (ImageView) findViewById(R.id.search_button);
        updateButtonWithDrawable(R.id.search_button, d);
        invalidatePressedFocusedStates(searchButtonContainer, searchButton);
    }

    private boolean updateVoiceSearchIcon(boolean searchVisible) {
        View voiceButtonContainer = findViewById(R.id.voice_button_container);
        View voiceButton = findViewById(R.id.voice_button);
        View voiceButtonProxy = findViewById(R.id.voice_button_proxy);
        SearchManager searchManager = (SearchManager) getSystemService("search");
        ComponentName globalSearchActivity = searchManager.getGlobalSearchActivity();
        ComponentName activityName = null;
        if (globalSearchActivity != null) {
            Intent intent = new Intent("android.speech.action.WEB_SEARCH");
            intent.setPackage(globalSearchActivity.getPackageName());
            activityName = intent.resolveActivity(getPackageManager());
        }
        if (activityName == null) {
            activityName = new Intent("android.speech.action.WEB_SEARCH").resolveActivity(getPackageManager());
        }
        if (searchVisible && activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo, "com.android.launcher.toolbar_voice_search_icon");
            if (sVoiceSearchIcon[coi] == null) {
                sVoiceSearchIcon[coi] = updateButtonWithIconFromExternalActivity(R.id.voice_button, activityName, R.drawable.ic_home_voice_search_holo, "com.android.launcher.toolbar_icon");
            }
            if (voiceButtonContainer != null) {
                voiceButtonContainer.setVisibility(0);
            }
            voiceButton.setVisibility(0);
            if (voiceButtonProxy != null) {
                voiceButtonProxy.setVisibility(0);
            }
            invalidatePressedFocusedStates(voiceButtonContainer, voiceButton);
            return true;
        }
        if (voiceButtonContainer != null) {
            voiceButtonContainer.setVisibility(8);
        }
        voiceButton.setVisibility(8);
        if (voiceButtonProxy == null) {
            return false;
        }
        voiceButtonProxy.setVisibility(8);
        return false;
    }

    private void updateVoiceSearchIcon(Drawable.ConstantState d) {
        View voiceButtonContainer = findViewById(R.id.voice_button_container);
        View voiceButton = findViewById(R.id.voice_button);
        updateButtonWithDrawable(R.id.voice_button, d);
        invalidatePressedFocusedStates(voiceButtonContainer, voiceButton);
    }

    private void updateAppMarketIcon() {
        View marketButton = findViewById(R.id.market_button);
        Intent intent = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.APP_MARKET");
        ComponentName activityName = intent.resolveActivity(getPackageManager());
        if (activityName != null) {
            int coi = getCurrentOrientationIndexForGlobalIcons();
            this.mAppMarketIntent = intent;
            sAppMarketIcon[coi] = updateTextButtonWithIconFromExternalActivity(R.id.market_button, activityName, R.drawable.ic_launcher_market_holo, "com.android.launcher.toolbar_icon");
            marketButton.setVisibility(0);
            return;
        }
        marketButton.setVisibility(8);
        marketButton.setEnabled(false);
    }

    private void updateAppMarketIcon(Drawable.ConstantState d) {
        Resources r = getResources();
        Drawable marketIconDrawable = d.newDrawable(r);
        int w = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_width);
        int h = r.getDimensionPixelSize(R.dimen.toolbar_external_icon_height);
        marketIconDrawable.setBounds(0, 0, w, h);
        updateTextButtonWithDrawable(R.id.market_button, marketIconDrawable);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean result = super.dispatchPopulateAccessibilityEvent(event);
        List<CharSequence> text = event.getText();
        text.clear();
        if (this.mState == State.APPS_CUSTOMIZE) {
            text.add(getString(R.string.all_apps_button_label));
        } else {
            text.add(getString(R.string.all_apps_home_button_label));
        }
        return result;
    }

    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        private CloseSystemDialogsIntentReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Launcher.this.closeSystemDialogs();
        }
    }

    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            Launcher.this.onAppWidgetReset();
        }
    }

    private boolean waitUntilResume(Runnable run, boolean deletePreviousRunnables) {
        if (!this.mPaused) {
            return false;
        }
        Log.i("Launcher", "Deferring update until onResume");
        if (deletePreviousRunnables) {
            while (this.mOnResumeCallbacks.remove(run)) {
            }
        }
        this.mOnResumeCallbacks.add(run);
        return true;
    }

    private boolean waitUntilResume(Runnable run) {
        return waitUntilResume(run, false);
    }

    @Override
    public boolean setLoadOnResume() {
        if (!this.mPaused) {
            return false;
        }
        Log.i("Launcher", "setLoadOnResume");
        this.mOnResumeNeedsLoad = true;
        return true;
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
        this.mOnResumeCallbacks.clear();
        Workspace workspace = this.mWorkspace;
        this.mNewShortcutAnimatePage = -1;
        this.mNewShortcutAnimateViews.clear();
        this.mWorkspace.clearDropTargets();
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout layoutParent = (CellLayout) workspace.getChildAt(i);
            layoutParent.removeAllViewsInLayout();
        }
        this.mWidgetsToAdvance.clear();
        if (this.mHotseat != null) {
            this.mHotseat.resetLayout();
        }
    }

    @Override
    public void bindItems(final ArrayList<ItemInfo> shortcuts, final int start, final int end) {
        if (!waitUntilResume(new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindItems(shortcuts, start, end);
            }
        })) {
            Set<String> newApps = this.mSharedPrefs.getStringSet("apps.new.list", new HashSet<>());
            Workspace workspace = this.mWorkspace;
            for (int i = start; i < end; i++) {
                ItemInfo item = shortcuts.get(i);
                if (item.container != -101 || this.mHotseat != null) {
                    switch (item.itemType) {
                        case 0:
                        case 1:
                            ShortcutInfo info = (ShortcutInfo) item;
                            String uri = info.intent.toUri(0).toString();
                            View shortcut = createShortcut(info);
                            workspace.addInScreen(shortcut, item.container, item.screen, item.cellX, item.cellY, 1, 1, false);
                            boolean animateIconUp = false;
                            if (item.container != -101) {
                                synchronized (newApps) {
                                    if (newApps.contains(uri)) {
                                        animateIconUp = newApps.remove(uri);
                                    }
                                }
                                if (animateIconUp) {
                                    shortcut.setAlpha(0.0f);
                                    shortcut.setScaleX(0.0f);
                                    shortcut.setScaleY(0.0f);
                                    this.mNewShortcutAnimatePage = item.screen;
                                    if (!this.mNewShortcutAnimateViews.contains(shortcut)) {
                                        this.mNewShortcutAnimateViews.add(shortcut);
                                    }
                                }
                            } else {
                                continue;
                            }
                            break;
                        case 2:
                            FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this, (ViewGroup) workspace.getChildAt(workspace.getCurrentPage()), (FolderInfo) item, this.mIconCache);
                            workspace.addInScreen(newFolder, item.container, item.screen, item.cellX, item.cellY, 1, 1, false);
                            break;
                    }
                }
            }
            workspace.requestLayout();
        }
    }

    @Override
    public void bindFolders(final HashMap<Long, FolderInfo> folders) {
        if (!waitUntilResume(new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindFolders(folders);
            }
        })) {
            sFolders.clear();
            sFolders.putAll(folders);
        }
    }

    @Override
    public void bindAppWidget(final LauncherAppWidgetInfo item) {
        if (!waitUntilResume(new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindAppWidget(item);
            }
        })) {
            Workspace workspace = this.mWorkspace;
            int appWidgetId = item.appWidgetId;
            AppWidgetProviderInfo appWidgetInfo = this.mAppWidgetManager.getAppWidgetInfo(appWidgetId);
            item.hostView = this.mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);
            item.hostView.setTag(item);
            item.onBindAppWidget(this);
            workspace.addInScreen(item.hostView, item.container, item.screen, item.cellX, item.cellY, item.spanX, item.spanY, false);
            addWidgetToAutoAdvanceIfNeeded(item.hostView, appWidgetInfo);
            workspace.requestLayout();
        }
    }

    @Override
    public void onPageBoundSynchronously(int page) {
        this.mSynchronouslyBoundPages.add(Integer.valueOf(page));
    }

    @Override
    public void finishBindingItems() {
        if (!waitUntilResume(new Runnable() {
            @Override
            public void run() {
                Launcher.this.finishBindingItems();
            }
        })) {
            if (this.mSavedState != null) {
                if (!this.mWorkspace.hasFocus()) {
                    this.mWorkspace.getChildAt(this.mWorkspace.getCurrentPage()).requestFocus();
                }
                this.mSavedState = null;
            }
            this.mWorkspace.restoreInstanceStateForRemainingPages();
            for (int i = 0; i < sPendingAddList.size(); i++) {
                completeAdd(sPendingAddList.get(i));
            }
            sPendingAddList.clear();
            updateAppMarketIcon();
            if (this.mVisible || this.mWorkspaceLoading) {
                Runnable newAppsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Launcher.this.runNewAppsAnimation(false);
                    }
                };
                boolean willSnapPage = this.mNewShortcutAnimatePage > -1 && this.mNewShortcutAnimatePage != this.mWorkspace.getCurrentPage();
                if (canRunNewAppsAnimation()) {
                    if (willSnapPage) {
                        this.mWorkspace.snapToPage(this.mNewShortcutAnimatePage, newAppsRunnable);
                    } else {
                        runNewAppsAnimation(false);
                    }
                } else {
                    runNewAppsAnimation(willSnapPage);
                }
            }
            this.mWorkspaceLoading = false;
        }
    }

    private boolean canRunNewAppsAnimation() {
        long diff = System.currentTimeMillis() - this.mDragController.getLastGestureUpTime();
        return diff > ((long) (NEW_APPS_ANIMATION_INACTIVE_TIMEOUT_SECONDS * 1000));
    }

    private void runNewAppsAnimation(boolean immediate) {
        AnimatorSet anim = LauncherAnimUtils.createAnimatorSet();
        Collection<Animator> bounceAnims = new ArrayList<>();
        Collections.sort(this.mNewShortcutAnimateViews, new Comparator<View>() {
            @Override
            public int compare(View a, View b) {
                CellLayout.LayoutParams alp = (CellLayout.LayoutParams) a.getLayoutParams();
                CellLayout.LayoutParams blp = (CellLayout.LayoutParams) b.getLayoutParams();
                int cellCountX = LauncherModel.getCellCountX();
                return ((alp.cellY * cellCountX) + alp.cellX) - ((blp.cellY * cellCountX) + blp.cellX);
            }
        });
        if (immediate) {
            for (View v : this.mNewShortcutAnimateViews) {
                v.setAlpha(1.0f);
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
            }
        } else {
            for (int i = 0; i < this.mNewShortcutAnimateViews.size(); i++) {
                ValueAnimator bounceAnim = LauncherAnimUtils.ofPropertyValuesHolder(this.mNewShortcutAnimateViews.get(i), PropertyValuesHolder.ofFloat("alpha", 1.0f), PropertyValuesHolder.ofFloat("scaleX", 1.0f), PropertyValuesHolder.ofFloat("scaleY", 1.0f));
                bounceAnim.setDuration(450L);
                bounceAnim.setStartDelay(i * 75);
                bounceAnim.setInterpolator(new SmoothPagedView.OvershootInterpolator());
                bounceAnims.add(bounceAnim);
            }
            anim.playTogether(bounceAnims);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (Launcher.this.mWorkspace != null) {
                        Launcher.this.mWorkspace.postDelayed(Launcher.this.mBuildLayersRunnable, 500L);
                    }
                }
            });
            anim.start();
        }
        this.mNewShortcutAnimatePage = -1;
        this.mNewShortcutAnimateViews.clear();
        new Thread("clearNewAppsThread") {
            @Override
            public void run() {
                Launcher.this.mSharedPrefs.edit().putInt("apps.new.page", -1).putStringSet("apps.new.list", null).commit();
            }
        }.start();
    }

    @Override
    public void bindSearchablesChanged() {
        boolean searchVisible = updateGlobalSearchIcon();
        boolean voiceVisible = updateVoiceSearchIcon(searchVisible);
        if (this.mSearchDropTargetBar != null) {
            this.mSearchDropTargetBar.onSearchPackagesChanged(searchVisible, voiceVisible);
        }
    }

    @Override
    public void bindAllApplications(final ArrayList<ApplicationInfo> apps) {
        Runnable setAllAppsRunnable = new Runnable() {
            @Override
            public void run() {
                if (Launcher.this.mAppsCustomizeContent != null) {
                    Launcher.this.mAppsCustomizeContent.setApps(apps);
                }
            }
        };
        View progressBar = this.mAppsCustomizeTabHost.findViewById(R.id.apps_customize_progress_bar);
        if (progressBar != null) {
            ((ViewGroup) progressBar.getParent()).removeView(progressBar);
            this.mAppsCustomizeTabHost.post(setAllAppsRunnable);
        } else {
            setAllAppsRunnable.run();
        }
    }

    @Override
    public void bindAppsAdded(final ArrayList<ApplicationInfo> apps) {
        if (!waitUntilResume(new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindAppsAdded(apps);
            }
        }) && this.mAppsCustomizeContent != null) {
            this.mAppsCustomizeContent.addApps(apps);
        }
    }

    @Override
    public void bindAppsUpdated(final ArrayList<ApplicationInfo> apps) {
        if (!waitUntilResume(new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindAppsUpdated(apps);
            }
        })) {
            if (this.mWorkspace != null) {
                this.mWorkspace.updateShortcuts(apps);
            }
            if (this.mAppsCustomizeContent != null) {
                this.mAppsCustomizeContent.updateApps(apps);
            }
        }
    }

    @Override
    public void bindComponentsRemoved(final ArrayList<String> packageNames, final ArrayList<ApplicationInfo> appInfos, final boolean matchPackageNamesOnly, final UserHandle user) {
        if (!waitUntilResume(new Runnable() {
            @Override
            public void run() {
                Launcher.this.bindComponentsRemoved(packageNames, appInfos, matchPackageNamesOnly, user);
            }
        })) {
            if (matchPackageNamesOnly) {
                this.mWorkspace.removeItemsByPackageName(packageNames, user);
            } else {
                this.mWorkspace.removeItemsByApplicationInfo(appInfos, user);
            }
            if (this.mAppsCustomizeContent != null) {
                this.mAppsCustomizeContent.removeApps(appInfos);
            }
            this.mDragController.onAppsRemoved(appInfos, this);
        }
    }

    @Override
    public void bindPackagesUpdated(ArrayList<Object> widgetsAndShortcuts) {
        if (waitUntilResume(this.mBindPackagesUpdatedRunnable, true)) {
            this.mWidgetsAndShortcuts = widgetsAndShortcuts;
        } else if (this.mAppsCustomizeContent != null) {
            this.mAppsCustomizeContent.onPackagesUpdated(widgetsAndShortcuts);
        }
    }

    private int mapConfigurationOriActivityInfoOri(int configOri) {
        Display d = getWindowManager().getDefaultDisplay();
        int naturalOri = 2;
        switch (d.getRotation()) {
            case 0:
            case 2:
                naturalOri = configOri;
                break;
            case 1:
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

    public boolean isRotationEnabled() {
        return sForceEnableRotation || getResources().getBoolean(R.bool.allow_rotation);
    }

    public void lockScreenOrientation() {
        if (isRotationEnabled()) {
            setRequestedOrientation(mapConfigurationOriActivityInfoOri(getResources().getConfiguration().orientation));
        }
    }

    public void unlockScreenOrientation(boolean immediate) {
        if (isRotationEnabled()) {
            if (immediate) {
                setRequestedOrientation(-1);
            } else {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Launcher.this.setRequestedOrientation(-1);
                    }
                }, 500L);
            }
        }
    }

    private boolean isClingsEnabled() {
        return false;
    }

    private Cling initCling(int clingId, int[] positionData, boolean animate, int delay) {
        final Cling cling = (Cling) findViewById(clingId);
        if (cling != null) {
            cling.init(this, positionData);
            cling.setVisibility(0);
            cling.setLayerType(2, null);
            if (animate) {
                cling.buildLayer();
                cling.setAlpha(0.0f);
                cling.animate().alpha(1.0f).setInterpolator(new AccelerateInterpolator()).setDuration(550L).setStartDelay(delay).start();
            } else {
                cling.setAlpha(1.0f);
            }
            cling.setFocusableInTouchMode(true);
            cling.post(new Runnable() {
                @Override
                public void run() {
                    cling.setFocusable(true);
                    cling.requestFocus();
                }
            });
            this.mHideFromAccessibilityHelper.setImportantForAccessibilityToNo(this.mDragLayer, clingId == R.id.all_apps_cling);
        }
        return cling;
    }

    private void dismissCling(final Cling cling, final String flag, int duration) {
        if (cling != null && cling.getVisibility() != 8) {
            ObjectAnimator anim = LauncherAnimUtils.ofFloat(cling, "alpha", 0.0f);
            anim.setDuration(duration);
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    cling.setVisibility(8);
                    cling.cleanup();
                    new Thread("dismissClingThread") {
                        @Override
                        public void run() {
                            SharedPreferences.Editor editor = Launcher.this.mSharedPrefs.edit();
                            editor.putBoolean(flag, true);
                            editor.commit();
                        }
                    }.start();
                }
            });
            anim.start();
            this.mHideFromAccessibilityHelper.restoreImportantForAccessibility(this.mDragLayer);
        }
    }

    private void removeCling(int id) {
        final View cling = findViewById(id);
        if (cling != null) {
            final ViewGroup parent = (ViewGroup) cling.getParent();
            parent.post(new Runnable() {
                @Override
                public void run() {
                    parent.removeView(cling);
                }
            });
            this.mHideFromAccessibilityHelper.restoreImportantForAccessibility(this.mDragLayer);
        }
    }

    private boolean skipCustomClingIfNoAccounts() {
        Cling cling = (Cling) findViewById(R.id.workspace_cling);
        boolean customCling = cling.getDrawIdentifier().equals("workspace_custom");
        if (!customCling) {
            return false;
        }
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");
        return accounts.length == 0;
    }

    public void showFirstRunWorkspaceCling() {
        if (isClingsEnabled() && !this.mSharedPrefs.getBoolean("cling.workspace.dismissed", false) && !skipCustomClingIfNoAccounts()) {
            if (this.mSharedPrefs.getInt("DEFAULT_WORKSPACE_RESOURCE_ID", 0) != 0 && getResources().getBoolean(R.bool.config_useCustomClings)) {
                View cling = findViewById(R.id.workspace_cling);
                ViewGroup clingParent = (ViewGroup) cling.getParent();
                int clingIndex = clingParent.indexOfChild(cling);
                clingParent.removeViewAt(clingIndex);
                View customCling = this.mInflater.inflate(R.layout.custom_workspace_cling, clingParent, false);
                clingParent.addView(customCling, clingIndex);
                customCling.setId(R.id.workspace_cling);
            }
            initCling(R.id.workspace_cling, null, false, 0);
            return;
        }
        removeCling(R.id.workspace_cling);
    }

    public void showFirstRunAllAppsCling(int[] position) {
        if (isClingsEnabled() && !this.mSharedPrefs.getBoolean("cling.allapps.dismissed", false)) {
            initCling(R.id.all_apps_cling, position, true, 0);
        } else {
            removeCling(R.id.all_apps_cling);
        }
    }

    public Cling showFirstRunFoldersCling() {
        if (isClingsEnabled() && !this.mSharedPrefs.getBoolean("cling.folder.dismissed", false)) {
            return initCling(R.id.folder_cling, null, true, 0);
        }
        removeCling(R.id.folder_cling);
        return null;
    }

    public boolean isFolderClingVisible() {
        Cling cling = (Cling) findViewById(R.id.folder_cling);
        return cling != null && cling.getVisibility() == 0;
    }

    public void dismissWorkspaceCling(View v) {
        Cling cling = (Cling) findViewById(R.id.workspace_cling);
        dismissCling(cling, "cling.workspace.dismissed", 250);
    }

    public void dismissAllAppsCling(View v) {
        Cling cling = (Cling) findViewById(R.id.all_apps_cling);
        dismissCling(cling, "cling.allapps.dismissed", 250);
    }

    public void dismissFolderCling(View v) {
        Cling cling = (Cling) findViewById(R.id.folder_cling);
        dismissCling(cling, "cling.folder.dismissed", 250);
    }

    public void dumpState() {
        Log.d("Launcher", "BEGIN launcher2 dump state for launcher " + this);
        Log.d("Launcher", "mSavedState=" + this.mSavedState);
        Log.d("Launcher", "mWorkspaceLoading=" + this.mWorkspaceLoading);
        Log.d("Launcher", "mRestoring=" + this.mRestoring);
        Log.d("Launcher", "mWaitingForResult=" + this.mWaitingForResult);
        Log.d("Launcher", "mSavedInstanceState=" + this.mSavedInstanceState);
        Log.d("Launcher", "sFolders.size=" + sFolders.size());
        this.mModel.dumpState();
        if (this.mAppsCustomizeContent != null) {
            this.mAppsCustomizeContent.dumpState();
        }
        Log.d("Launcher", "END launcher2 dump state");
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.println(" ");
        writer.println("Debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            writer.println("  " + sDumpLogs.get(i));
        }
    }

    public static void dumpDebugLogsToConsole() {
        Log.d("Launcher", "");
        Log.d("Launcher", "*********************");
        Log.d("Launcher", "Launcher debug logs: ");
        for (int i = 0; i < sDumpLogs.size(); i++) {
            Log.d("Launcher", "  " + sDumpLogs.get(i));
        }
        Log.d("Launcher", "*********************");
        Log.d("Launcher", "");
    }
}
