package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IUserSwitchObserver;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.style.SuggestionSpan;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.LruCache;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InputBindResult;
import com.android.server.pm.PackageManagerService;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wm.WindowManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class InputMethodManagerService extends IInputMethodManager.Stub implements ServiceConnection, Handler.Callback {
    static final boolean DEBUG = false;
    static final int MSG_ATTACH_TOKEN = 1040;
    static final int MSG_BIND_INPUT = 1010;
    static final int MSG_BIND_METHOD = 3010;
    static final int MSG_CREATE_SESSION = 1050;
    static final int MSG_HARD_KEYBOARD_SWITCH_CHANGED = 4000;
    static final int MSG_HIDE_SOFT_INPUT = 1030;
    static final int MSG_RESTART_INPUT = 2010;
    static final int MSG_SET_ACTIVE = 3020;
    static final int MSG_SET_USER_ACTION_NOTIFICATION_SEQUENCE_NUMBER = 3040;
    static final int MSG_SHOW_IM_CONFIG = 4;
    static final int MSG_SHOW_IM_PICKER = 1;
    static final int MSG_SHOW_IM_SUBTYPE_ENABLER = 3;
    static final int MSG_SHOW_IM_SUBTYPE_PICKER = 2;
    static final int MSG_SHOW_SOFT_INPUT = 1020;
    static final int MSG_START_INPUT = 2000;
    static final int MSG_UNBIND_INPUT = 1000;
    static final int MSG_UNBIND_METHOD = 3000;
    private static final int NOT_A_SUBTYPE_ID = -1;
    static final int SECURE_SUGGESTION_SPANS_MAX_SIZE = 20;
    static final String TAG = "InputMethodManagerService";
    private static final String TAG_TRY_SUPPRESSING_IME_SWITCHER = "TrySuppressingImeSwitcher";
    static final long TIME_TO_RECONNECT = 3000;
    private final AppOpsManager mAppOpsManager;
    boolean mBoundToMethod;
    final HandlerCaller mCaller;
    final Context mContext;
    EditorInfo mCurAttribute;
    ClientState mCurClient;
    private boolean mCurClientInKeyguard;
    IBinder mCurFocusedWindow;
    String mCurId;
    IInputContext mCurInputContext;
    Intent mCurIntent;
    IInputMethod mCurMethod;
    String mCurMethodId;
    int mCurSeq;
    IBinder mCurToken;
    private InputMethodSubtype mCurrentSubtype;
    private AlertDialog.Builder mDialogBuilder;
    SessionState mEnabledSession;
    private InputMethodFileManager mFileManager;
    final boolean mHasFeature;
    boolean mHaveConnection;
    private final boolean mImeSelectedOnBoot;
    private PendingIntent mImeSwitchPendingIntent;
    int mImeWindowVis;
    private InputMethodInfo[] mIms;
    boolean mInputShown;
    private KeyguardManager mKeyguardManager;
    long mLastBindTime;
    private Locale mLastSystemLocale;
    private NotificationManager mNotificationManager;
    private boolean mNotificationShown;
    final Resources mRes;
    final InputMethodUtils.InputMethodSettings mSettings;
    final SettingsObserver mSettingsObserver;
    boolean mShowExplicitlyRequested;
    boolean mShowForced;
    private boolean mShowImeWithHardKeyboard;
    private boolean mShowOngoingImeSwitcherForPhones;
    boolean mShowRequested;
    private StatusBarManagerService mStatusBar;
    private int[] mSubtypeIds;
    private final InputMethodSubtypeSwitchingController mSwitchingController;
    private AlertDialog mSwitchingDialog;
    private View mSwitchingDialogTitleView;
    boolean mSystemReady;
    private final WindowManagerService mWindowManagerService;
    final InputBindResult mNoBinding = new InputBindResult((IInputMethodSession) null, (InputChannel) null, (String) null, -1, -1);
    final ArrayList<InputMethodInfo> mMethodList = new ArrayList<>();
    final HashMap<String, InputMethodInfo> mMethodMap = new HashMap<>();
    private final LruCache<SuggestionSpan, InputMethodInfo> mSecureSuggestionSpans = new LruCache<>(20);
    final ServiceConnection mVisibleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    boolean mVisibleBound = DEBUG;
    final HashMap<IBinder, ClientState> mClients = new HashMap<>();
    private final HashMap<InputMethodInfo, ArrayList<InputMethodSubtype>> mShortcutInputMethodsAndSubtypes = new HashMap<>();
    boolean mScreenOn = true;
    int mCurUserActionNotificationSequenceNumber = 0;
    int mBackDisposition = 0;
    private final MyPackageMonitor mMyPackageMonitor = new MyPackageMonitor();
    private final IPackageManager mIPackageManager = AppGlobals.getPackageManager();
    final Handler mHandler = new Handler(this);
    final IWindowManager mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    private final HardKeyboardListener mHardKeyboardListener = new HardKeyboardListener();
    private Notification mImeSwitcherNotification = new Notification();

    static class SessionState {
        InputChannel channel;
        final ClientState client;
        final IInputMethod method;
        IInputMethodSession session;

        public String toString() {
            return "SessionState{uid " + this.client.uid + " pid " + this.client.pid + " method " + Integer.toHexString(System.identityHashCode(this.method)) + " session " + Integer.toHexString(System.identityHashCode(this.session)) + " channel " + this.channel + "}";
        }

        SessionState(ClientState _client, IInputMethod _method, IInputMethodSession _session, InputChannel _channel) {
            this.client = _client;
            this.method = _method;
            this.session = _session;
            this.channel = _channel;
        }
    }

    static final class ClientState {
        final InputBinding binding;
        final IInputMethodClient client;
        SessionState curSession;
        final IInputContext inputContext;
        final int pid;
        boolean sessionRequested;
        final int uid;

        public String toString() {
            return "ClientState{" + Integer.toHexString(System.identityHashCode(this)) + " uid " + this.uid + " pid " + this.pid + "}";
        }

        ClientState(IInputMethodClient _client, IInputContext _inputContext, int _uid, int _pid) {
            this.client = _client;
            this.inputContext = _inputContext;
            this.uid = _uid;
            this.pid = _pid;
            this.binding = new InputBinding(null, this.inputContext.asBinder(), this.uid, this.pid);
        }
    }

    class SettingsObserver extends ContentObserver {
        String mLastEnabled;

        SettingsObserver(Handler handler) {
            super(handler);
            this.mLastEnabled = "";
            ContentResolver resolver = InputMethodManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), InputMethodManagerService.DEBUG, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor("enabled_input_methods"), InputMethodManagerService.DEBUG, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor("selected_input_method_subtype"), InputMethodManagerService.DEBUG, this);
            resolver.registerContentObserver(Settings.Secure.getUriFor("show_ime_with_hard_keyboard"), InputMethodManagerService.DEBUG, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Uri showImeUri = Settings.Secure.getUriFor("show_ime_with_hard_keyboard");
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (showImeUri.equals(uri)) {
                    InputMethodManagerService.this.updateKeyboardFromSettingsLocked();
                } else {
                    boolean enabledChanged = InputMethodManagerService.DEBUG;
                    String newEnabled = InputMethodManagerService.this.mSettings.getEnabledInputMethodsStr();
                    if (!this.mLastEnabled.equals(newEnabled)) {
                        this.mLastEnabled = newEnabled;
                        enabledChanged = true;
                    }
                    InputMethodManagerService.this.updateInputMethodsFromSettingsLocked(enabledChanged);
                }
            }
        }
    }

    class ImmsBroadcastReceiver extends BroadcastReceiver {
        ImmsBroadcastReceiver() {
        }

        private void updateActive() {
            if (InputMethodManagerService.this.mCurClient != null && InputMethodManagerService.this.mCurClient.client != null) {
                InputMethodManagerService.this.executeOrSendMessage(InputMethodManagerService.this.mCurClient.client, InputMethodManagerService.this.mCaller.obtainMessageIO(InputMethodManagerService.MSG_SET_ACTIVE, InputMethodManagerService.this.mScreenOn ? 1 : 0, InputMethodManagerService.this.mCurClient));
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                InputMethodManagerService.this.mScreenOn = true;
                InputMethodManagerService.this.refreshImeWindowVisibilityLocked();
                updateActive();
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                InputMethodManagerService.this.mScreenOn = InputMethodManagerService.DEBUG;
                InputMethodManagerService.this.setImeWindowVisibilityStatusHiddenLocked();
                updateActive();
            } else if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                InputMethodManagerService.this.hideInputMethodMenu();
            } else if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_REMOVED".equals(action)) {
                InputMethodManagerService.this.updateCurrentProfileIds();
            } else {
                Slog.w(InputMethodManagerService.TAG, "Unexpected intent " + intent);
            }
        }
    }

    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        private boolean isChangingPackagesOfCurrentUser() {
            int userId = getChangingUserId();
            if (userId == InputMethodManagerService.this.mSettings.getCurrentUserId()) {
                return true;
            }
            return InputMethodManagerService.DEBUG;
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            if (!isChangingPackagesOfCurrentUser()) {
                return InputMethodManagerService.DEBUG;
            }
            synchronized (InputMethodManagerService.this.mMethodMap) {
                String curInputMethodId = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                int N = InputMethodManagerService.this.mMethodList.size();
                if (curInputMethodId != null) {
                    for (int i = 0; i < N; i++) {
                        InputMethodInfo imi = InputMethodManagerService.this.mMethodList.get(i);
                        if (imi.getId().equals(curInputMethodId)) {
                            for (String pkg : packages) {
                                if (imi.getPackageName().equals(pkg)) {
                                    if (doit) {
                                        InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked("");
                                        InputMethodManagerService.this.chooseNewDefaultIMELocked();
                                        return true;
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
                return InputMethodManagerService.DEBUG;
            }
        }

        public void onSomePackagesChanged() {
            if (isChangingPackagesOfCurrentUser()) {
                synchronized (InputMethodManagerService.this.mMethodMap) {
                    InputMethodInfo curIm = null;
                    String curInputMethodId = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                    int N = InputMethodManagerService.this.mMethodList.size();
                    if (curInputMethodId != null) {
                        for (int i = 0; i < N; i++) {
                            InputMethodInfo imi = InputMethodManagerService.this.mMethodList.get(i);
                            String imiId = imi.getId();
                            if (imiId.equals(curInputMethodId)) {
                                curIm = imi;
                            }
                            int change = isPackageDisappearing(imi.getPackageName());
                            if (isPackageModified(imi.getPackageName())) {
                                InputMethodManagerService.this.mFileManager.deleteAllInputMethodSubtypes(imiId);
                            }
                            if (change == 2 || change == 3) {
                                Slog.i(InputMethodManagerService.TAG, "Input method uninstalled, disabling: " + imi.getComponent());
                                InputMethodManagerService.this.setInputMethodEnabledLocked(imi.getId(), InputMethodManagerService.DEBUG);
                            }
                        }
                    }
                    InputMethodManagerService.this.buildInputMethodListLocked(InputMethodManagerService.this.mMethodList, InputMethodManagerService.this.mMethodMap, InputMethodManagerService.DEBUG);
                    boolean changed = InputMethodManagerService.DEBUG;
                    if (curIm != null) {
                        int change2 = isPackageDisappearing(curIm.getPackageName());
                        if (change2 == 2 || change2 == 3) {
                            ServiceInfo si = null;
                            try {
                                si = InputMethodManagerService.this.mIPackageManager.getServiceInfo(curIm.getComponent(), 0, InputMethodManagerService.this.mSettings.getCurrentUserId());
                            } catch (RemoteException e) {
                            }
                            if (si == null) {
                                Slog.i(InputMethodManagerService.TAG, "Current input method removed: " + curInputMethodId);
                                InputMethodManagerService.this.setImeWindowVisibilityStatusHiddenLocked();
                                if (!InputMethodManagerService.this.chooseNewDefaultIMELocked()) {
                                    changed = true;
                                    curIm = null;
                                    Slog.i(InputMethodManagerService.TAG, "Unsetting current input method");
                                    InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked("");
                                }
                            }
                        }
                    }
                    if (curIm == null) {
                        changed = InputMethodManagerService.this.chooseNewDefaultIMELocked();
                    }
                    if (changed) {
                        InputMethodManagerService.this.updateFromSettingsLocked(InputMethodManagerService.DEBUG);
                    }
                }
            }
        }
    }

    private static final class MethodCallback extends IInputSessionCallback.Stub {
        private final InputChannel mChannel;
        private final IInputMethod mMethod;
        private final InputMethodManagerService mParentIMMS;

        MethodCallback(InputMethodManagerService imms, IInputMethod method, InputChannel channel) {
            this.mParentIMMS = imms;
            this.mMethod = method;
            this.mChannel = channel;
        }

        public void sessionCreated(IInputMethodSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mParentIMMS.onSessionCreated(this.mMethod, session, this.mChannel);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private class HardKeyboardListener implements WindowManagerService.OnHardKeyboardStatusChangeListener {
        private HardKeyboardListener() {
        }

        @Override
        public void onHardKeyboardStatusChange(boolean available) {
            InputMethodManagerService.this.mHandler.sendMessage(InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_HARD_KEYBOARD_SWITCH_CHANGED, Integer.valueOf(available ? 1 : 0)));
        }

        public void handleHardKeyboardStatusChange(boolean available) {
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (InputMethodManagerService.this.mSwitchingDialog != null && InputMethodManagerService.this.mSwitchingDialogTitleView != null && InputMethodManagerService.this.mSwitchingDialog.isShowing()) {
                    InputMethodManagerService.this.mSwitchingDialogTitleView.findViewById(R.id.firstStrongLtr).setVisibility(available ? 0 : 8);
                }
            }
        }
    }

    public InputMethodManagerService(Context context, WindowManagerService windowManager) {
        this.mContext = context;
        this.mRes = context.getResources();
        this.mCaller = new HandlerCaller(context, (Looper) null, new HandlerCaller.Callback() {
            public void executeMessage(Message msg) {
                InputMethodManagerService.this.handleMessage(msg);
            }
        }, true);
        this.mWindowManagerService = windowManager;
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mHasFeature = context.getPackageManager().hasSystemFeature("android.software.input_methods");
        this.mImeSwitcherNotification.icon = R.drawable.ic_corp_badge_off;
        this.mImeSwitcherNotification.when = 0L;
        this.mImeSwitcherNotification.flags = 2;
        this.mImeSwitcherNotification.tickerText = null;
        this.mImeSwitcherNotification.defaults = 0;
        this.mImeSwitcherNotification.sound = null;
        this.mImeSwitcherNotification.vibrate = null;
        this.mImeSwitcherNotification.extras.putBoolean("android.allowDuringSetup", true);
        this.mImeSwitcherNotification.category = "sys";
        Intent intent = new Intent("android.settings.SHOW_INPUT_METHOD_PICKER");
        this.mImeSwitchPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        this.mShowOngoingImeSwitcherForPhones = DEBUG;
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction("android.intent.action.SCREEN_ON");
        broadcastFilter.addAction("android.intent.action.SCREEN_OFF");
        broadcastFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        broadcastFilter.addAction("android.intent.action.USER_ADDED");
        broadcastFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiver(new ImmsBroadcastReceiver(), broadcastFilter);
        this.mNotificationShown = DEBUG;
        int userId = 0;
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    synchronized (InputMethodManagerService.this.mMethodMap) {
                        InputMethodManagerService.this.switchUserLocked(newUserId);
                    }
                    if (reply != null) {
                        try {
                            reply.sendResult((Bundle) null);
                        } catch (RemoteException e) {
                        }
                    }
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                }
            });
            userId = ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mMyPackageMonitor.register(this.mContext, null, UserHandle.ALL, true);
        this.mSettings = new InputMethodUtils.InputMethodSettings(this.mRes, context.getContentResolver(), this.mMethodMap, this.mMethodList, userId);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, userId);
        synchronized (this.mMethodMap) {
            this.mSwitchingController = InputMethodSubtypeSwitchingController.createInstanceLocked(this.mSettings, context);
        }
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        this.mImeSelectedOnBoot = !TextUtils.isEmpty(defaultImiId);
        synchronized (this.mMethodMap) {
            buildInputMethodListLocked(this.mMethodList, this.mMethodMap, this.mImeSelectedOnBoot ? false : true);
        }
        this.mSettings.enableAllIMEsIfThereIsNoEnabledIME();
        if (!this.mImeSelectedOnBoot) {
            Slog.w(TAG, "No IME selected. Choose the most applicable IME.");
            synchronized (this.mMethodMap) {
                resetDefaultImeLocked(context);
            }
        }
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        synchronized (this.mMethodMap) {
            updateFromSettingsLocked(true);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent2) {
                synchronized (InputMethodManagerService.this.mMethodMap) {
                    InputMethodManagerService.this.resetStateIfCurrentLocaleChangedLocked();
                }
            }
        }, filter);
    }

    private void resetDefaultImeLocked(Context context) {
        if (this.mCurMethodId == null || InputMethodUtils.isSystemIme(this.mMethodMap.get(this.mCurMethodId))) {
            InputMethodInfo defIm = null;
            for (InputMethodInfo imi : this.mMethodList) {
                if (defIm == null && InputMethodUtils.isValidSystemDefaultIme(this.mSystemReady, imi, context)) {
                    defIm = imi;
                    Slog.i(TAG, "Selected default: " + imi.getId());
                }
            }
            if (defIm == null && this.mMethodList.size() > 0) {
                defIm = InputMethodUtils.getMostApplicableDefaultIME(this.mSettings.getEnabledInputMethodListLocked());
                if (defIm != null) {
                    Slog.i(TAG, "Default found, using " + defIm.getId());
                } else {
                    Slog.i(TAG, "No default found");
                }
            }
            if (defIm != null) {
                setSelectedInputMethodAndSubtypeLocked(defIm, -1, DEBUG);
            }
        }
    }

    private void resetAllInternalStateLocked(boolean updateOnlyWhenLocaleChanged, boolean resetDefaultEnabledIme) {
        if (this.mSystemReady) {
            Locale newLocale = this.mRes.getConfiguration().locale;
            if (!updateOnlyWhenLocaleChanged || (newLocale != null && !newLocale.equals(this.mLastSystemLocale))) {
                if (!updateOnlyWhenLocaleChanged) {
                    hideCurrentInputLocked(0, null);
                    this.mCurMethodId = null;
                    unbindCurrentMethodLocked(true, DEBUG);
                }
                buildInputMethodListLocked(this.mMethodList, this.mMethodMap, resetDefaultEnabledIme);
                if (!updateOnlyWhenLocaleChanged) {
                    String selectedImiId = this.mSettings.getSelectedInputMethod();
                    if (TextUtils.isEmpty(selectedImiId)) {
                        resetDefaultImeLocked(this.mContext);
                    }
                } else {
                    resetDefaultImeLocked(this.mContext);
                }
                updateFromSettingsLocked(true);
                this.mLastSystemLocale = newLocale;
                if (!updateOnlyWhenLocaleChanged) {
                    try {
                        startInputInnerLocked();
                    } catch (RuntimeException e) {
                        Slog.w(TAG, "Unexpected exception", e);
                    }
                }
            }
        }
    }

    private void resetStateIfCurrentLocaleChangedLocked() {
        resetAllInternalStateLocked(DEBUG, DEBUG);
    }

    private void switchUserLocked(int newUserId) {
        this.mSettings.setCurrentUserId(newUserId);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, newUserId);
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        boolean initialUserSwitch = TextUtils.isEmpty(defaultImiId);
        resetAllInternalStateLocked(DEBUG, initialUserSwitch);
        if (initialUserSwitch) {
            InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mContext.getPackageManager(), this.mSettings.getEnabledInputMethodListLocked());
        }
    }

    void updateCurrentProfileIds() {
        List<UserInfo> profiles = UserManager.get(this.mContext).getProfiles(this.mSettings.getCurrentUserId());
        int[] currentProfileIds = new int[profiles.size()];
        for (int i = 0; i < currentProfileIds.length; i++) {
            currentProfileIds[i] = profiles.get(i).id;
        }
        this.mSettings.setCurrentProfileIds(currentProfileIds);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Input Method Manager Crash", e);
            }
            throw e;
        }
    }

    public void systemRunning(StatusBarManagerService statusBar) {
        synchronized (this.mMethodMap) {
            if (!this.mSystemReady) {
                this.mSystemReady = true;
                this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
                this.mStatusBar = statusBar;
                statusBar.setIconVisibility("ime", DEBUG);
                updateImeWindowStatusLocked();
                this.mShowOngoingImeSwitcherForPhones = this.mRes.getBoolean(R.^attr-private.accessibilityFocusedDrawable);
                if (this.mShowOngoingImeSwitcherForPhones) {
                    this.mWindowManagerService.setOnHardKeyboardStatusChangeListener(this.mHardKeyboardListener);
                }
                buildInputMethodListLocked(this.mMethodList, this.mMethodMap, !this.mImeSelectedOnBoot);
                if (!this.mImeSelectedOnBoot) {
                    Slog.w(TAG, "Reset the default IME as \"Resource\" is ready here.");
                    resetStateIfCurrentLocaleChangedLocked();
                    InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mContext.getPackageManager(), this.mSettings.getEnabledInputMethodListLocked());
                }
                this.mLastSystemLocale = this.mRes.getConfiguration().locale;
                try {
                    startInputInnerLocked();
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Unexpected exception", e);
                }
            }
        }
    }

    private void setImeWindowVisibilityStatusHiddenLocked() {
        this.mImeWindowVis = 0;
        updateImeWindowStatusLocked();
    }

    private void refreshImeWindowVisibilityLocked() {
        Configuration conf = this.mRes.getConfiguration();
        boolean haveHardKeyboard = conf.keyboard != 1;
        boolean hardKeyShown = haveHardKeyboard && conf.hardKeyboardHidden != 2;
        boolean isScreenLocked = isKeyguardLocked();
        boolean inputActive = !isScreenLocked && (this.mInputShown || hardKeyShown);
        boolean inputVisible = inputActive && !hardKeyShown;
        this.mImeWindowVis = (inputActive ? 1 : 0) | (inputVisible ? 2 : 0);
        updateImeWindowStatusLocked();
    }

    private void updateImeWindowStatusLocked() {
        setImeWindowStatus(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
    }

    private boolean calledFromValidUser() {
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (uid == 1000 || this.mSettings.isCurrentProfile(userId) || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            return true;
        }
        Slog.w(TAG, "--- IPC called from background users. Ignore. \n" + InputMethodUtils.getStackTrace());
        return DEBUG;
    }

    private boolean calledWithValidToken(IBinder token) {
        if (token == null || this.mCurToken != token) {
            return DEBUG;
        }
        return true;
    }

    private boolean bindCurrentInputMethodService(Intent service, ServiceConnection conn, int flags) {
        if (service != null && conn != null) {
            return this.mContext.bindServiceAsUser(service, conn, flags, new UserHandle(this.mSettings.getCurrentUserId()));
        }
        Slog.e(TAG, "--- bind failed: service = " + service + ", conn = " + conn);
        return DEBUG;
    }

    public List<InputMethodInfo> getInputMethodList() {
        ArrayList arrayList;
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        synchronized (this.mMethodMap) {
            arrayList = new ArrayList(this.mMethodList);
        }
        return arrayList;
    }

    public List<InputMethodInfo> getEnabledInputMethodList() {
        List<InputMethodInfo> enabledInputMethodListLocked;
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        synchronized (this.mMethodMap) {
            enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
        }
        return enabledInputMethodListLocked;
    }

    public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String imiId, boolean allowsImplicitlySelectedSubtypes) {
        InputMethodInfo imi;
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        synchronized (this.mMethodMap) {
            if (imiId == null) {
                if (this.mCurMethodId != null) {
                    imi = this.mMethodMap.get(this.mCurMethodId);
                } else {
                    imi = this.mMethodMap.get(imiId);
                }
            }
            if (imi == null) {
                return Collections.emptyList();
            }
            return this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, allowsImplicitlySelectedSubtypes);
        }
    }

    public void addClient(IInputMethodClient client, IInputContext inputContext, int uid, int pid) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                this.mClients.put(client.asBinder(), new ClientState(client, inputContext, uid, pid));
            }
        }
    }

    public void removeClient(IInputMethodClient client) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                ClientState cs = this.mClients.remove(client.asBinder());
                if (cs != null) {
                    clearClientSessionLocked(cs);
                }
            }
        }
    }

    void executeOrSendMessage(IInterface target, Message msg) {
        if (target.asBinder() instanceof Binder) {
            this.mCaller.sendMessage(msg);
        } else {
            handleMessage(msg);
            msg.recycle();
        }
    }

    void unbindCurrentClientLocked() {
        if (this.mCurClient != null) {
            if (this.mBoundToMethod) {
                this.mBoundToMethod = DEBUG;
                if (this.mCurMethod != null) {
                    executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(1000, this.mCurMethod));
                }
            }
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_SET_ACTIVE, 0, this.mCurClient));
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_UNBIND_METHOD, this.mCurSeq, this.mCurClient.client));
            this.mCurClient.sessionRequested = DEBUG;
            this.mCurClient = null;
            hideInputMethodMenuLocked();
        }
    }

    private int getImeShowFlags() {
        if (this.mShowForced) {
            int flags = 0 | 3;
            return flags;
        }
        if (!this.mShowExplicitlyRequested) {
            return 0;
        }
        int flags2 = 0 | 1;
        return flags2;
    }

    private int getAppShowFlags() {
        if (this.mShowForced) {
            int flags = 0 | 2;
            return flags;
        }
        if (this.mShowExplicitlyRequested) {
            return 0;
        }
        int flags2 = 0 | 1;
        return flags2;
    }

    InputBindResult attachNewInputLocked(boolean initial) {
        if (!this.mBoundToMethod) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_BIND_INPUT, this.mCurMethod, this.mCurClient.binding));
            this.mBoundToMethod = true;
        }
        SessionState session = this.mCurClient.curSession;
        if (initial) {
            executeOrSendMessage(session.method, this.mCaller.obtainMessageOOO(MSG_START_INPUT, session, this.mCurInputContext, this.mCurAttribute));
        } else {
            executeOrSendMessage(session.method, this.mCaller.obtainMessageOOO(MSG_RESTART_INPUT, session, this.mCurInputContext, this.mCurAttribute));
        }
        if (this.mShowRequested) {
            showCurrentInputLocked(getAppShowFlags(), null);
        }
        return new InputBindResult(session.session, session.channel != null ? session.channel.dup() : null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
    }

    InputBindResult startInputLocked(IInputMethodClient client, IInputContext inputContext, EditorInfo attribute, int controlFlags) {
        if (this.mCurMethodId == null) {
            return this.mNoBinding;
        }
        ClientState cs = this.mClients.get(client.asBinder());
        if (cs == null) {
            throw new IllegalArgumentException("unknown client " + client.asBinder());
        }
        try {
            if (!this.mIWindowManager.inputMethodClientHasFocus(cs.client)) {
                Slog.w(TAG, "Starting input on non-focused client " + cs.client + " (uid=" + cs.uid + " pid=" + cs.pid + ")");
                return null;
            }
        } catch (RemoteException e) {
        }
        return startInputUncheckedLocked(cs, inputContext, attribute, controlFlags);
    }

    InputBindResult startInputUncheckedLocked(ClientState cs, IInputContext inputContext, EditorInfo attribute, int controlFlags) {
        if (this.mCurMethodId == null) {
            return this.mNoBinding;
        }
        if (this.mCurClient != cs) {
            this.mCurClientInKeyguard = isKeyguardLocked();
            unbindCurrentClientLocked();
            if (this.mScreenOn) {
                executeOrSendMessage(cs.client, this.mCaller.obtainMessageIO(MSG_SET_ACTIVE, this.mScreenOn ? 1 : 0, cs));
            }
        }
        this.mCurSeq++;
        if (this.mCurSeq <= 0) {
            this.mCurSeq = 1;
        }
        this.mCurClient = cs;
        this.mCurInputContext = inputContext;
        this.mCurAttribute = attribute;
        if (this.mCurId != null && this.mCurId.equals(this.mCurMethodId)) {
            if (cs.curSession != null) {
                return attachNewInputLocked((controlFlags & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0);
            }
            if (this.mHaveConnection) {
                if (this.mCurMethod != null) {
                    requestClientSessionLocked(cs);
                    return new InputBindResult((IInputMethodSession) null, (InputChannel) null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
                }
                if (SystemClock.uptimeMillis() < this.mLastBindTime + TIME_TO_RECONNECT) {
                    return new InputBindResult((IInputMethodSession) null, (InputChannel) null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
                }
                EventLog.writeEvent(EventLogTags.IMF_FORCE_RECONNECT_IME, this.mCurMethodId, Long.valueOf(SystemClock.uptimeMillis() - this.mLastBindTime), 0);
            }
        }
        return startInputInnerLocked();
    }

    InputBindResult startInputInnerLocked() {
        if (this.mCurMethodId == null) {
            return this.mNoBinding;
        }
        if (!this.mSystemReady) {
            return new InputBindResult((IInputMethodSession) null, (InputChannel) null, this.mCurMethodId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
        }
        InputMethodInfo info = this.mMethodMap.get(this.mCurMethodId);
        if (info == null) {
            throw new IllegalArgumentException("Unknown id: " + this.mCurMethodId);
        }
        unbindCurrentMethodLocked(DEBUG, true);
        this.mCurIntent = new Intent("android.view.InputMethod");
        this.mCurIntent.setComponent(info.getComponent());
        this.mCurIntent.putExtra("android.intent.extra.client_label", R.string.keyguard_accessibility_status);
        if (BenesseExtension.getDchaState() == 0) {
            this.mCurIntent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.INPUT_METHOD_SETTINGS"), 0));
        }
        if (bindCurrentInputMethodService(this.mCurIntent, this, 1610612741)) {
            this.mLastBindTime = SystemClock.uptimeMillis();
            this.mHaveConnection = true;
            this.mCurId = info.getId();
            this.mCurToken = new Binder();
            try {
                Slog.v(TAG, "Adding window token: " + this.mCurToken);
                this.mIWindowManager.addWindowToken(this.mCurToken, 2011);
            } catch (RemoteException e) {
            }
            return new InputBindResult((IInputMethodSession) null, (InputChannel) null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
        }
        this.mCurIntent = null;
        Slog.w(TAG, "Failure connecting to input method service: " + this.mCurIntent);
        return null;
    }

    public InputBindResult startInput(IInputMethodClient client, IInputContext inputContext, EditorInfo attribute, int controlFlags) {
        InputBindResult inputBindResultStartInputLocked;
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mMethodMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                inputBindResultStartInputLocked = startInputLocked(client, inputContext, attribute, controlFlags);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return inputBindResultStartInputLocked;
    }

    public void finishInput(IInputMethodClient client) {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this.mMethodMap) {
            if (this.mCurIntent != null && name.equals(this.mCurIntent.getComponent())) {
                this.mCurMethod = IInputMethod.Stub.asInterface(service);
                if (this.mCurToken == null) {
                    Slog.w(TAG, "Service connected without a token!");
                    unbindCurrentMethodLocked(DEBUG, DEBUG);
                } else {
                    executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_ATTACH_TOKEN, this.mCurMethod, this.mCurToken));
                    if (this.mCurClient != null) {
                        clearClientSessionLocked(this.mCurClient);
                        requestClientSessionLocked(this.mCurClient);
                    }
                }
            }
        }
    }

    void onSessionCreated(IInputMethod method, IInputMethodSession session, InputChannel channel) {
        synchronized (this.mMethodMap) {
            if (this.mCurMethod != null && method != null && this.mCurMethod.asBinder() == method.asBinder() && this.mCurClient != null) {
                clearClientSessionLocked(this.mCurClient);
                this.mCurClient.curSession = new SessionState(this.mCurClient, method, session, channel);
                InputBindResult res = attachNewInputLocked(true);
                if (res.method != null) {
                    executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageOO(3010, this.mCurClient.client, res));
                }
                return;
            }
            channel.dispose();
        }
    }

    void unbindCurrentMethodLocked(boolean reportToClient, boolean savePosition) {
        if (this.mVisibleBound) {
            this.mContext.unbindService(this.mVisibleConnection);
            this.mVisibleBound = DEBUG;
        }
        if (this.mHaveConnection) {
            this.mContext.unbindService(this);
            this.mHaveConnection = DEBUG;
        }
        if (this.mCurToken != null) {
            try {
                if ((this.mImeWindowVis & 1) != 0 && savePosition) {
                    this.mWindowManagerService.saveLastInputMethodWindowForTransition();
                }
                this.mIWindowManager.removeWindowToken(this.mCurToken);
            } catch (RemoteException e) {
            }
            this.mCurToken = null;
        }
        this.mCurId = null;
        clearCurMethodLocked();
        if (reportToClient && this.mCurClient != null) {
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_UNBIND_METHOD, this.mCurSeq, this.mCurClient.client));
        }
    }

    void requestClientSessionLocked(ClientState cs) {
        if (!cs.sessionRequested) {
            InputChannel[] channels = InputChannel.openInputChannelPair(cs.toString());
            cs.sessionRequested = true;
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOOO(MSG_CREATE_SESSION, this.mCurMethod, channels[1], new MethodCallback(this, this.mCurMethod, channels[0])));
        }
    }

    void clearClientSessionLocked(ClientState cs) {
        finishSessionLocked(cs.curSession);
        cs.curSession = null;
        cs.sessionRequested = DEBUG;
    }

    private void finishSessionLocked(SessionState sessionState) {
        if (sessionState != null) {
            if (sessionState.session != null) {
                try {
                    sessionState.session.finishSession();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Session failed to close due to remote exception", e);
                    setImeWindowVisibilityStatusHiddenLocked();
                }
                sessionState.session = null;
            }
            if (sessionState.channel != null) {
                sessionState.channel.dispose();
                sessionState.channel = null;
            }
        }
    }

    void clearCurMethodLocked() {
        if (this.mCurMethod != null) {
            for (ClientState cs : this.mClients.values()) {
                clearClientSessionLocked(cs);
            }
            finishSessionLocked(this.mEnabledSession);
            this.mEnabledSession = null;
            this.mCurMethod = null;
        }
        if (this.mStatusBar != null) {
            this.mStatusBar.setIconVisibility("ime", DEBUG);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        synchronized (this.mMethodMap) {
            if (this.mCurMethod != null && this.mCurIntent != null && name.equals(this.mCurIntent.getComponent())) {
                clearCurMethodLocked();
                this.mLastBindTime = SystemClock.uptimeMillis();
                this.mShowRequested = this.mInputShown;
                this.mInputShown = DEBUG;
                if (this.mCurClient != null) {
                    executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_UNBIND_METHOD, this.mCurSeq, this.mCurClient.client));
                }
            }
        }
    }

    public void updateStatusIcon(IBinder token, String packageName, int iconId) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (!calledWithValidToken(token)) {
                    int uid = Binder.getCallingUid();
                    Slog.e(TAG, "Ignoring updateStatusIcon due to an invalid token. uid:" + uid + " token:" + token);
                    return;
                }
                if (iconId == 0) {
                    if (this.mStatusBar != null) {
                        this.mStatusBar.setIconVisibility("ime", DEBUG);
                    }
                } else if (packageName != null) {
                    CharSequence contentDescription = null;
                    try {
                        PackageManager packageManager = this.mContext.getPackageManager();
                        contentDescription = packageManager.getApplicationLabel(this.mIPackageManager.getApplicationInfo(packageName, 0, this.mSettings.getCurrentUserId()));
                    } catch (RemoteException e) {
                    }
                    if (this.mStatusBar != null) {
                        this.mStatusBar.setIcon("ime", packageName, iconId, 0, contentDescription != null ? contentDescription.toString() : null);
                        this.mStatusBar.setIconVisibility("ime", true);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean needsToShowImeSwitchOngoingNotification() {
        if (!this.mShowOngoingImeSwitcherForPhones || this.mSwitchingDialog != null || isScreenLocked()) {
            return DEBUG;
        }
        synchronized (this.mMethodMap) {
            List<InputMethodInfo> imis = this.mSettings.getEnabledInputMethodListLocked();
            int N = imis.size();
            if (N > 2) {
                return true;
            }
            if (N < 1) {
                return DEBUG;
            }
            int nonAuxCount = 0;
            int auxCount = 0;
            InputMethodSubtype nonAuxSubtype = null;
            InputMethodSubtype auxSubtype = null;
            for (int i = 0; i < N; i++) {
                InputMethodInfo imi = imis.get(i);
                List<InputMethodSubtype> subtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                int subtypeCount = subtypes.size();
                if (subtypeCount == 0) {
                    nonAuxCount++;
                } else {
                    for (int j = 0; j < subtypeCount; j++) {
                        InputMethodSubtype subtype = subtypes.get(j);
                        if (!subtype.isAuxiliary()) {
                            nonAuxCount++;
                            nonAuxSubtype = subtype;
                        } else {
                            auxCount++;
                            auxSubtype = subtype;
                        }
                    }
                }
            }
            if (nonAuxCount > 1 || auxCount > 1) {
                return true;
            }
            if (nonAuxCount == 1 && auxCount == 1) {
                if (nonAuxSubtype != null && auxSubtype != null && ((nonAuxSubtype.getLocale().equals(auxSubtype.getLocale()) || auxSubtype.overridesImplicitlyEnabledSubtype() || nonAuxSubtype.overridesImplicitlyEnabledSubtype()) && nonAuxSubtype.containsExtraValueKey(TAG_TRY_SUPPRESSING_IME_SWITCHER))) {
                    return DEBUG;
                }
                return true;
            }
            return DEBUG;
        }
    }

    private boolean isKeyguardLocked() {
        if (this.mKeyguardManager == null || !this.mKeyguardManager.isKeyguardLocked()) {
            return DEBUG;
        }
        return true;
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
        InputMethodInfo imi;
        long ident = Binder.clearCallingIdentity();
        try {
            if (!calledWithValidToken(token)) {
                int uid = Binder.getCallingUid();
                Slog.e(TAG, "Ignoring setImeWindowStatus due to an invalid token. uid:" + uid + " token:" + token);
                return;
            }
            synchronized (this.mMethodMap) {
                if (vis != 0) {
                    if (isKeyguardLocked() && !this.mCurClientInKeyguard) {
                        vis = 0;
                    }
                    this.mImeWindowVis = vis;
                    this.mBackDisposition = backDisposition;
                    boolean iconVisibility = ((vis & 1) != 0 || (!this.mWindowManagerService.isHardKeyboardAvailable() && (vis & 2) == 0)) ? DEBUG : true;
                    boolean needsToShowImeSwitcher = (iconVisibility || !needsToShowImeSwitchOngoingNotification()) ? DEBUG : true;
                    if (this.mStatusBar != null) {
                        this.mStatusBar.setImeWindowStatus(token, vis, backDisposition, needsToShowImeSwitcher);
                    }
                    imi = this.mMethodMap.get(this.mCurMethodId);
                    if (imi == null && needsToShowImeSwitcher) {
                        CharSequence title = this.mRes.getText(R.string.indeterminate_progress_55);
                        CharSequence summary = InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, imi, this.mCurrentSubtype);
                        this.mImeSwitcherNotification.color = this.mContext.getResources().getColor(R.color.system_accent3_600);
                        this.mImeSwitcherNotification.setLatestEventInfo(this.mContext, title, summary, this.mImeSwitchPendingIntent);
                        if (this.mNotificationManager != null && !this.mWindowManagerService.hasNavigationBar()) {
                            this.mNotificationManager.notifyAsUser(null, R.string.indeterminate_progress_55, this.mImeSwitcherNotification, UserHandle.ALL);
                            this.mNotificationShown = true;
                        }
                    } else if (this.mNotificationShown && this.mNotificationManager != null) {
                        this.mNotificationManager.cancelAsUser(null, R.string.indeterminate_progress_55, UserHandle.ALL);
                        this.mNotificationShown = DEBUG;
                    }
                } else {
                    this.mImeWindowVis = vis;
                    this.mBackDisposition = backDisposition;
                    if ((vis & 1) != 0) {
                        if (iconVisibility) {
                            if (this.mStatusBar != null) {
                            }
                            imi = this.mMethodMap.get(this.mCurMethodId);
                            if (imi == null) {
                                if (this.mNotificationShown) {
                                    this.mNotificationManager.cancelAsUser(null, R.string.indeterminate_progress_55, UserHandle.ALL);
                                    this.mNotificationShown = DEBUG;
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void registerSuggestionSpansForNotification(SuggestionSpan[] spans) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                InputMethodInfo currentImi = this.mMethodMap.get(this.mCurMethodId);
                for (SuggestionSpan ss : spans) {
                    if (!TextUtils.isEmpty(ss.getNotificationTargetClassName())) {
                        this.mSecureSuggestionSpans.put(ss, currentImi);
                    }
                }
            }
        }
    }

    public boolean notifySuggestionPicked(SuggestionSpan span, String originalString, int index) {
        boolean z = DEBUG;
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                InputMethodInfo targetImi = this.mSecureSuggestionSpans.get(span);
                if (targetImi != null) {
                    String[] suggestions = span.getSuggestions();
                    if (index >= 0 && index < suggestions.length) {
                        String className = span.getNotificationTargetClassName();
                        Intent intent = new Intent();
                        intent.setClassName(targetImi.getPackageName(), className);
                        intent.setAction("android.text.style.SUGGESTION_PICKED");
                        intent.putExtra("before", originalString);
                        intent.putExtra("after", suggestions[index]);
                        intent.putExtra("hashcode", span.hashCode());
                        long ident = Binder.clearCallingIdentity();
                        try {
                            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                            Binder.restoreCallingIdentity(ident);
                            z = true;
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(ident);
                            throw th;
                        }
                    }
                }
            }
        }
        return z;
    }

    void updateFromSettingsLocked(boolean enabledMayChange) {
        updateInputMethodsFromSettingsLocked(enabledMayChange);
        updateKeyboardFromSettingsLocked();
    }

    void updateInputMethodsFromSettingsLocked(boolean enabledMayChange) {
        if (enabledMayChange) {
            List<InputMethodInfo> enabled = this.mSettings.getEnabledInputMethodListLocked();
            for (int i = 0; i < enabled.size(); i++) {
                InputMethodInfo imm = enabled.get(i);
                try {
                    ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(imm.getPackageName(), 32768, this.mSettings.getCurrentUserId());
                    if (ai != null && ai.enabledSetting == 4) {
                        this.mIPackageManager.setApplicationEnabledSetting(imm.getPackageName(), 0, 1, this.mSettings.getCurrentUserId(), this.mContext.getBasePackageName());
                    }
                } catch (RemoteException e) {
                }
            }
        }
        String id = this.mSettings.getSelectedInputMethod();
        if (TextUtils.isEmpty(id) && chooseNewDefaultIMELocked()) {
            id = this.mSettings.getSelectedInputMethod();
        }
        if (!TextUtils.isEmpty(id)) {
            try {
                setInputMethodLocked(id, this.mSettings.getSelectedInputMethodSubtypeId(id));
            } catch (IllegalArgumentException e2) {
                Slog.w(TAG, "Unknown input method from prefs: " + id, e2);
                this.mCurMethodId = null;
                unbindCurrentMethodLocked(true, DEBUG);
            }
            this.mShortcutInputMethodsAndSubtypes.clear();
        } else {
            this.mCurMethodId = null;
            unbindCurrentMethodLocked(true, DEBUG);
        }
        this.mSwitchingController.resetCircularListLocked(this.mContext);
    }

    public void updateKeyboardFromSettingsLocked() {
        this.mShowImeWithHardKeyboard = this.mSettings.isShowImeWithHardKeyboardEnabled();
        if (this.mSwitchingDialog != null && this.mSwitchingDialogTitleView != null && this.mSwitchingDialog.isShowing()) {
            Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(R.id.firstStrongRtl);
            hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
        }
    }

    void setInputMethodLocked(String id, int subtypeId) {
        InputMethodSubtype newSubtype;
        InputMethodInfo info = this.mMethodMap.get(id);
        if (info == null) {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        if (this.mCurClient != null && this.mCurAttribute != null) {
            int uid = this.mCurClient.uid;
            String packageName = this.mCurAttribute.packageName;
            if (SystemConfig.getInstance().getFixedImeApps().contains(packageName)) {
                if (!InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, uid, packageName)) {
                    Slog.e(TAG, "Ignoring FixedImeApps due to the validation failure. uid=" + uid + " package=" + packageName);
                } else {
                    return;
                }
            }
        }
        if (id.equals(this.mCurMethodId)) {
            int subtypeCount = info.getSubtypeCount();
            if (subtypeCount > 0) {
                InputMethodSubtype oldSubtype = this.mCurrentSubtype;
                if (subtypeId >= 0 && subtypeId < subtypeCount) {
                    newSubtype = info.getSubtypeAt(subtypeId);
                } else {
                    newSubtype = getCurrentInputMethodSubtypeLocked();
                }
                if (newSubtype == null || oldSubtype == null) {
                    Slog.w(TAG, "Illegal subtype state: old subtype = " + oldSubtype + ", new subtype = " + newSubtype);
                    return;
                }
                if (newSubtype != oldSubtype) {
                    setSelectedInputMethodAndSubtypeLocked(info, subtypeId, true);
                    if (this.mCurMethod != null) {
                        try {
                            refreshImeWindowVisibilityLocked();
                            this.mCurMethod.changeInputMethodSubtype(newSubtype);
                            return;
                        } catch (RemoteException e) {
                            Slog.w(TAG, "Failed to call changeInputMethodSubtype");
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            setSelectedInputMethodAndSubtypeLocked(info, subtypeId, DEBUG);
            this.mCurMethodId = id;
            if (ActivityManagerNative.isSystemReady()) {
                Intent intent = new Intent("android.intent.action.INPUT_METHOD_CHANGED");
                intent.addFlags(536870912);
                intent.putExtra("input_method_id", id);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
            unbindCurrentClientLocked();
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean showSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        boolean zShowCurrentInputLocked = DEBUG;
        if (calledFromValidUser()) {
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mMethodMap) {
                    if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                        try {
                            if (this.mIWindowManager.inputMethodClientHasFocus(client)) {
                                zShowCurrentInputLocked = showCurrentInputLocked(flags, resultReceiver);
                            } else {
                                Slog.w(TAG, "Ignoring showSoftInput of uid " + uid + ": " + client);
                            }
                        } catch (RemoteException e) {
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return zShowCurrentInputLocked;
    }

    boolean showCurrentInputLocked(int flags, ResultReceiver resultReceiver) {
        this.mShowRequested = true;
        if ((flags & 1) == 0) {
            this.mShowExplicitlyRequested = true;
        }
        if ((flags & 2) != 0) {
            this.mShowExplicitlyRequested = true;
            this.mShowForced = true;
        }
        if (!this.mSystemReady) {
            return DEBUG;
        }
        if (this.mCurMethod != null) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageIOO(MSG_SHOW_SOFT_INPUT, getImeShowFlags(), this.mCurMethod, resultReceiver));
            this.mInputShown = true;
            if (this.mHaveConnection && !this.mVisibleBound) {
                bindCurrentInputMethodService(this.mCurIntent, this.mVisibleConnection, 134217729);
                this.mVisibleBound = true;
            }
            return true;
        }
        if (!this.mHaveConnection || SystemClock.uptimeMillis() < this.mLastBindTime + TIME_TO_RECONNECT) {
            return DEBUG;
        }
        EventLog.writeEvent(EventLogTags.IMF_FORCE_RECONNECT_IME, this.mCurMethodId, Long.valueOf(SystemClock.uptimeMillis() - this.mLastBindTime), 1);
        Slog.w(TAG, "Force disconnect/connect to the IME in showCurrentInputLocked()");
        this.mContext.unbindService(this);
        bindCurrentInputMethodService(this.mCurIntent, this, 1073741825);
        return DEBUG;
    }

    public boolean hideSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        boolean zHideCurrentInputLocked = DEBUG;
        if (calledFromValidUser()) {
            Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                synchronized (this.mMethodMap) {
                    if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                        try {
                            if (this.mIWindowManager.inputMethodClientHasFocus(client)) {
                                zHideCurrentInputLocked = hideCurrentInputLocked(flags, resultReceiver);
                            } else {
                                setImeWindowVisibilityStatusHiddenLocked();
                            }
                        } catch (RemoteException e) {
                            setImeWindowVisibilityStatusHiddenLocked();
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return zHideCurrentInputLocked;
    }

    boolean hideCurrentInputLocked(int flags, ResultReceiver resultReceiver) {
        boolean res;
        if ((flags & 1) != 0 && (this.mShowExplicitlyRequested || this.mShowForced)) {
            return DEBUG;
        }
        if (this.mShowForced && (flags & 2) != 0) {
            return DEBUG;
        }
        if (this.mInputShown && this.mCurMethod != null) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_HIDE_SOFT_INPUT, this.mCurMethod, resultReceiver));
            res = true;
        } else {
            res = DEBUG;
        }
        if (this.mHaveConnection && this.mVisibleBound) {
            this.mContext.unbindService(this.mVisibleConnection);
            this.mVisibleBound = DEBUG;
        }
        this.mInputShown = DEBUG;
        this.mShowRequested = DEBUG;
        this.mShowExplicitlyRequested = DEBUG;
        this.mShowForced = DEBUG;
        return res;
    }

    public InputBindResult windowGainedFocus(IInputMethodClient client, IBinder windowToken, int controlFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext) {
        boolean calledFromValidUser = calledFromValidUser();
        InputBindResult res = null;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                ClientState cs = this.mClients.get(client.asBinder());
                if (cs == null) {
                    throw new IllegalArgumentException("unknown client " + client.asBinder());
                }
                try {
                    if (!this.mIWindowManager.inputMethodClientHasFocus(cs.client)) {
                        Slog.w(TAG, "Focus gain on non-focused client " + cs.client + " (uid=" + cs.uid + " pid=" + cs.pid + ")");
                        return null;
                    }
                } catch (RemoteException e) {
                }
                if (!calledFromValidUser) {
                    Slog.w(TAG, "A background user is requesting window. Hiding IME.");
                    Slog.w(TAG, "If you want to interect with IME, you need android.permission.INTERACT_ACROSS_USERS_FULL");
                    hideCurrentInputLocked(0, null);
                    return null;
                }
                if (this.mCurFocusedWindow == windowToken) {
                    Slog.w(TAG, "Window already focused, ignoring focus gain of: " + client + " attribute=" + attribute + ", token = " + windowToken);
                    if (attribute != null) {
                        return startInputUncheckedLocked(cs, inputContext, attribute, controlFlags);
                    }
                    return null;
                }
                this.mCurFocusedWindow = windowToken;
                boolean doAutoShow = ((softInputMode & 240) == 16 || this.mRes.getConfiguration().isLayoutSizeAtLeast(3)) ? true : DEBUG;
                boolean isTextEditor = (controlFlags & 2) != 0 ? true : DEBUG;
                boolean didStart = DEBUG;
                switch (softInputMode & 15) {
                    case 0:
                        if (isTextEditor && doAutoShow) {
                            if (isTextEditor && doAutoShow && (softInputMode & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
                                if (attribute != null) {
                                    res = startInputUncheckedLocked(cs, inputContext, attribute, controlFlags);
                                    didStart = true;
                                }
                                showCurrentInputLocked(1, null);
                            }
                        } else if (WindowManager.LayoutParams.mayUseInputMethod(windowFlags)) {
                            hideCurrentInputLocked(2, null);
                        }
                        break;
                    case 2:
                        if ((softInputMode & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
                            hideCurrentInputLocked(0, null);
                        }
                        break;
                    case 3:
                        hideCurrentInputLocked(0, null);
                        break;
                    case 4:
                        if ((softInputMode & PackageManagerService.DumpState.DUMP_VERIFIERS) != 0) {
                            if (attribute != null) {
                                res = startInputUncheckedLocked(cs, inputContext, attribute, controlFlags);
                                didStart = true;
                            }
                            showCurrentInputLocked(1, null);
                        }
                        break;
                    case 5:
                        if (attribute != null) {
                            res = startInputUncheckedLocked(cs, inputContext, attribute, controlFlags);
                            didStart = true;
                        }
                        showCurrentInputLocked(1, null);
                        break;
                }
                if (!didStart && attribute != null) {
                    res = startInputUncheckedLocked(cs, inputContext, attribute, controlFlags);
                }
                Binder.restoreCallingIdentity(ident);
                return res;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void showInputMethodPickerFromClient(IInputMethodClient client) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                    Slog.w(TAG, "Ignoring showInputMethodPickerFromClient of uid " + Binder.getCallingUid() + ": " + client);
                }
                this.mHandler.sendEmptyMessage(2);
            }
        }
    }

    public void setInputMethod(IBinder token, String id) {
        if (calledFromValidUser()) {
            setInputMethodWithSubtypeId(token, id, -1);
        }
    }

    public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (subtype != null) {
                    setInputMethodWithSubtypeIdLocked(token, id, InputMethodUtils.getSubtypeIdFromHashCode(this.mMethodMap.get(id), subtype.hashCode()));
                } else {
                    setInputMethod(token, id);
                }
            }
        }
    }

    public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient client, String inputMethodId) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                    Slog.w(TAG, "Ignoring showInputMethodAndSubtypeEnablerFromClient of: " + client);
                }
                executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(3, inputMethodId));
            }
        }
    }

    public boolean switchToLastInputMethod(IBinder token) {
        InputMethodInfo lastImi;
        List<InputMethodInfo> enabled;
        InputMethodSubtype keyboardSubtype;
        if (!calledFromValidUser()) {
            return DEBUG;
        }
        synchronized (this.mMethodMap) {
            Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
            if (lastIme != null) {
                lastImi = this.mMethodMap.get(lastIme.first);
            } else {
                lastImi = null;
            }
            String targetLastImiId = null;
            int subtypeId = -1;
            if (lastIme != null && lastImi != null) {
                boolean imiIdIsSame = lastImi.getId().equals(this.mCurMethodId);
                int lastSubtypeHash = Integer.valueOf((String) lastIme.second).intValue();
                int currentSubtypeHash = this.mCurrentSubtype == null ? -1 : this.mCurrentSubtype.hashCode();
                if (!imiIdIsSame || lastSubtypeHash != currentSubtypeHash) {
                    targetLastImiId = (String) lastIme.first;
                    subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(lastImi, lastSubtypeHash);
                }
            }
            if (TextUtils.isEmpty(targetLastImiId) && !InputMethodUtils.canAddToLastInputMethod(this.mCurrentSubtype) && (enabled = this.mSettings.getEnabledInputMethodListLocked()) != null) {
                int N = enabled.size();
                String locale = this.mCurrentSubtype == null ? this.mRes.getConfiguration().locale.toString() : this.mCurrentSubtype.getLocale();
                for (int i = 0; i < N; i++) {
                    InputMethodInfo imi = enabled.get(i);
                    if (imi.getSubtypeCount() > 0 && InputMethodUtils.isSystemIme(imi) && (keyboardSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, InputMethodUtils.getSubtypes(imi), "keyboard", locale, true)) != null) {
                        targetLastImiId = imi.getId();
                        subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, keyboardSubtype.hashCode());
                        if (keyboardSubtype.getLocale().equals(locale)) {
                            break;
                        }
                    }
                }
            }
            if (!TextUtils.isEmpty(targetLastImiId)) {
                setInputMethodWithSubtypeIdLocked(token, targetLastImiId, subtypeId);
                return true;
            }
            return DEBUG;
        }
    }

    public boolean switchToNextInputMethod(IBinder token, boolean onlyCurrentIme) {
        boolean z;
        if (!calledFromValidUser()) {
            return DEBUG;
        }
        synchronized (this.mMethodMap) {
            if (!calledWithValidToken(token)) {
                int uid = Binder.getCallingUid();
                Slog.e(TAG, "Ignoring switchToNextInputMethod due to an invalid token. uid:" + uid + " token:" + token);
                z = false;
            } else {
                InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(onlyCurrentIme, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype);
                if (nextSubtype == null) {
                    z = false;
                } else {
                    setInputMethodWithSubtypeIdLocked(token, nextSubtype.mImi.getId(), nextSubtype.mSubtypeId);
                    z = true;
                }
            }
        }
        return z;
    }

    public boolean shouldOfferSwitchingToNextInputMethod(IBinder token) {
        boolean z;
        if (!calledFromValidUser()) {
            return DEBUG;
        }
        synchronized (this.mMethodMap) {
            if (!calledWithValidToken(token)) {
                int uid = Binder.getCallingUid();
                Slog.e(TAG, "Ignoring shouldOfferSwitchingToNextInputMethod due to an invalid token. uid:" + uid + " token:" + token);
                z = false;
            } else {
                InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(DEBUG, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype);
                z = nextSubtype != null;
            }
        }
        return z;
    }

    public InputMethodSubtype getLastInputMethodSubtype() {
        InputMethodSubtype subtypeAt;
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mMethodMap) {
            Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
            if (lastIme == null || TextUtils.isEmpty((CharSequence) lastIme.first) || TextUtils.isEmpty((CharSequence) lastIme.second)) {
                subtypeAt = null;
            } else {
                InputMethodInfo lastImi = this.mMethodMap.get(lastIme.first);
                if (lastImi == null) {
                    subtypeAt = null;
                } else {
                    try {
                        int lastSubtypeHash = Integer.valueOf((String) lastIme.second).intValue();
                        int lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(lastImi, lastSubtypeHash);
                        subtypeAt = (lastSubtypeId < 0 || lastSubtypeId >= lastImi.getSubtypeCount()) ? null : lastImi.getSubtypeAt(lastSubtypeId);
                    } catch (NumberFormatException e) {
                        subtypeAt = null;
                    }
                }
            }
        }
        return subtypeAt;
    }

    public void setAdditionalInputMethodSubtypes(String imiId, InputMethodSubtype[] subtypes) {
        if (calledFromValidUser() && !TextUtils.isEmpty(imiId) && subtypes != null && subtypes.length != 0) {
            synchronized (this.mMethodMap) {
                InputMethodInfo imi = this.mMethodMap.get(imiId);
                if (imi != null) {
                    try {
                        String[] packageInfos = this.mIPackageManager.getPackagesForUid(Binder.getCallingUid());
                        if (packageInfos != null) {
                            for (String str : packageInfos) {
                                if (str.equals(imi.getPackageName())) {
                                    this.mFileManager.addInputMethodSubtypes(imi, subtypes);
                                    long ident = Binder.clearCallingIdentity();
                                    try {
                                        buildInputMethodListLocked(this.mMethodList, this.mMethodMap, DEBUG);
                                        return;
                                    } finally {
                                        Binder.restoreCallingIdentity(ident);
                                    }
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to get package infos");
                    }
                }
            }
        }
    }

    public int getInputMethodWindowVisibleHeight() {
        return this.mWindowManagerService.getInputMethodWindowVisibleHeight();
    }

    public void notifyUserAction(int sequenceNumber) {
        synchronized (this.mMethodMap) {
            if (this.mCurUserActionNotificationSequenceNumber == sequenceNumber) {
                InputMethodInfo imi = this.mMethodMap.get(this.mCurMethodId);
                if (imi != null) {
                    this.mSwitchingController.onUserActionLocked(imi, this.mCurrentSubtype);
                }
            }
        }
    }

    private void setInputMethodWithSubtypeId(IBinder token, String id, int subtypeId) {
        synchronized (this.mMethodMap) {
            setInputMethodWithSubtypeIdLocked(token, id, subtypeId);
        }
    }

    private void setInputMethodWithSubtypeIdLocked(IBinder token, String id, int subtypeId) {
        if (token == null) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Using null token requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
        } else if (this.mCurToken != token) {
            Slog.w(TAG, "Ignoring setInputMethod of uid " + Binder.getCallingUid() + " token: " + token);
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            setInputMethodLocked(id, subtypeId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void hideMySoftInput(IBinder token, int flags) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (!calledWithValidToken(token)) {
                    int uid = Binder.getCallingUid();
                    Slog.e(TAG, "Ignoring hideInputMethod due to an invalid token. uid:" + uid + " token:" + token);
                } else {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        hideCurrentInputLocked(flags, null);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    public void showMySoftInput(IBinder token, int flags) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (!calledWithValidToken(token)) {
                    int uid = Binder.getCallingUid();
                    Slog.e(TAG, "Ignoring showMySoftInput due to an invalid token. uid:" + uid + " token:" + token);
                } else {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        showCurrentInputLocked(flags, null);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    void setEnabledSessionInMainThread(SessionState session) {
        if (this.mEnabledSession != session) {
            if (this.mEnabledSession != null && this.mEnabledSession.session != null) {
                try {
                    this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, DEBUG);
                } catch (RemoteException e) {
                }
            }
            this.mEnabledSession = session;
            if (this.mEnabledSession != null && this.mEnabledSession.session != null) {
                try {
                    this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, true);
                } catch (RemoteException e2) {
                }
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean z = DEBUG;
        switch (msg.what) {
            case 1:
                showInputMethodMenu();
                return true;
            case 2:
                showInputMethodSubtypeMenu();
                return true;
            case 3:
                SomeArgs args = (SomeArgs) msg.obj;
                showInputMethodAndSubtypeEnabler((String) args.arg1);
                args.recycle();
                return true;
            case 4:
                showConfigureInputMethods();
                return true;
            case 1000:
                try {
                    ((IInputMethod) msg.obj).unbindInput();
                    return true;
                } catch (RemoteException e) {
                    return true;
                }
            case MSG_BIND_INPUT:
                SomeArgs args2 = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args2.arg1).bindInput((InputBinding) args2.arg2);
                    break;
                } catch (RemoteException e2) {
                }
                args2.recycle();
                return true;
            case MSG_SHOW_SOFT_INPUT:
                SomeArgs args3 = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args3.arg1).showSoftInput(msg.arg1, (ResultReceiver) args3.arg2);
                    break;
                } catch (RemoteException e3) {
                }
                args3.recycle();
                return true;
            case MSG_HIDE_SOFT_INPUT:
                SomeArgs args4 = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args4.arg1).hideSoftInput(0, (ResultReceiver) args4.arg2);
                    break;
                } catch (RemoteException e4) {
                }
                args4.recycle();
                return true;
            case MSG_ATTACH_TOKEN:
                SomeArgs args5 = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args5.arg1).attachToken((IBinder) args5.arg2);
                    break;
                } catch (RemoteException e5) {
                }
                args5.recycle();
                return true;
            case MSG_CREATE_SESSION:
                SomeArgs args6 = (SomeArgs) msg.obj;
                IInputMethod method = (IInputMethod) args6.arg1;
                InputChannel channel = (InputChannel) args6.arg2;
                try {
                    method.createSession(channel, (IInputSessionCallback) args6.arg3);
                    if (channel != null && Binder.isProxy(method)) {
                        channel.dispose();
                    }
                } catch (RemoteException e6) {
                    if (channel != null && Binder.isProxy(method)) {
                        channel.dispose();
                    }
                } catch (Throwable th) {
                    if (channel != null && Binder.isProxy(method)) {
                        channel.dispose();
                    }
                    throw th;
                }
                args6.recycle();
                return true;
            case MSG_START_INPUT:
                SomeArgs args7 = (SomeArgs) msg.obj;
                try {
                    SessionState session = (SessionState) args7.arg1;
                    setEnabledSessionInMainThread(session);
                    session.method.startInput((IInputContext) args7.arg2, (EditorInfo) args7.arg3);
                    break;
                } catch (RemoteException e7) {
                }
                args7.recycle();
                return true;
            case MSG_RESTART_INPUT:
                SomeArgs args8 = (SomeArgs) msg.obj;
                try {
                    SessionState session2 = (SessionState) args8.arg1;
                    setEnabledSessionInMainThread(session2);
                    session2.method.restartInput((IInputContext) args8.arg2, (EditorInfo) args8.arg3);
                    break;
                } catch (RemoteException e8) {
                }
                args8.recycle();
                return true;
            case MSG_UNBIND_METHOD:
                try {
                    ((IInputMethodClient) msg.obj).onUnbindMethod(msg.arg1);
                    return true;
                } catch (RemoteException e9) {
                    return true;
                }
            case 3010:
                SomeArgs args9 = (SomeArgs) msg.obj;
                IInputMethodClient client = (IInputMethodClient) args9.arg1;
                InputBindResult res = (InputBindResult) args9.arg2;
                try {
                    try {
                        client.onBindMethod(res);
                    } catch (RemoteException e10) {
                        Slog.w(TAG, "Client died receiving input method " + args9.arg2);
                        if (res.channel != null && Binder.isProxy(client)) {
                            res.channel.dispose();
                        }
                    }
                    args9.recycle();
                    return true;
                } finally {
                    if (res.channel != null && Binder.isProxy(client)) {
                        res.channel.dispose();
                    }
                }
            case MSG_SET_ACTIVE:
                try {
                    IInputMethodClient iInputMethodClient = ((ClientState) msg.obj).client;
                    if (msg.arg1 != 0) {
                        z = true;
                    }
                    iInputMethodClient.setActive(z);
                    return true;
                } catch (RemoteException e11) {
                    Slog.w(TAG, "Got RemoteException sending setActive(false) notification to pid " + ((ClientState) msg.obj).pid + " uid " + ((ClientState) msg.obj).uid);
                    return true;
                }
            case 3040:
                int sequenceNumber = msg.arg1;
                ClientState clientState = (ClientState) msg.obj;
                try {
                    clientState.client.setUserActionNotificationSequenceNumber(sequenceNumber);
                    return true;
                } catch (RemoteException e12) {
                    Slog.w(TAG, "Got RemoteException sending setUserActionNotificationSequenceNumber(" + sequenceNumber + ") notification to pid " + clientState.pid + " uid " + clientState.uid);
                    return true;
                }
            case MSG_HARD_KEYBOARD_SWITCH_CHANGED:
                this.mHardKeyboardListener.handleHardKeyboardStatusChange(msg.arg1 == 1);
                return true;
            default:
                return DEBUG;
        }
    }

    private boolean chooseNewDefaultIMELocked() {
        InputMethodInfo imi = InputMethodUtils.getMostApplicableDefaultIME(this.mSettings.getEnabledInputMethodListLocked());
        if (imi == null) {
            return DEBUG;
        }
        resetSelectedInputMethodAndSubtypeLocked(imi.getId());
        return true;
    }

    void buildInputMethodListLocked(ArrayList<InputMethodInfo> list, HashMap<String, InputMethodInfo> map, boolean resetDefaultEnabledIme) {
        list.clear();
        map.clear();
        PackageManager pm = this.mContext.getPackageManager();
        String disabledSysImes = this.mSettings.getDisabledSystemInputMethods();
        if (disabledSysImes == null) {
        }
        List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.view.InputMethod"), 32896, this.mSettings.getCurrentUserId());
        HashMap<String, List<InputMethodSubtype>> additionalSubtypes = this.mFileManager.getAllAdditionalInputMethodSubtypes();
        for (int i = 0; i < services.size(); i++) {
            ResolveInfo ri = services.get(i);
            ServiceInfo si = ri.serviceInfo;
            ComponentName compName = new ComponentName(si.packageName, si.name);
            if (!"android.permission.BIND_INPUT_METHOD".equals(si.permission)) {
                Slog.w(TAG, "Skipping input method " + compName + ": it does not require the permission android.permission.BIND_INPUT_METHOD");
            } else {
                try {
                    InputMethodInfo p = new InputMethodInfo(this.mContext, ri, additionalSubtypes);
                    list.add(p);
                    String id = p.getId();
                    map.put(id, p);
                } catch (Exception e) {
                    Slog.wtf(TAG, "Unable to load input method " + compName, e);
                }
            }
        }
        if (resetDefaultEnabledIme) {
            ArrayList<InputMethodInfo> defaultEnabledIme = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mSystemReady, list);
            for (int i2 = 0; i2 < defaultEnabledIme.size(); i2++) {
                InputMethodInfo imi = defaultEnabledIme.get(i2);
                setInputMethodEnabledLocked(imi.getId(), true);
            }
        }
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        if (!TextUtils.isEmpty(defaultImiId)) {
            if (!map.containsKey(defaultImiId)) {
                Slog.w(TAG, "Default IME is uninstalled. Choose new default IME.");
                if (chooseNewDefaultIMELocked()) {
                    updateInputMethodsFromSettingsLocked(true);
                }
            } else {
                setInputMethodEnabledLocked(defaultImiId, true);
            }
        }
        this.mSwitchingController.resetCircularListLocked(this.mContext);
    }

    private void showInputMethodMenu() {
        showInputMethodMenuInternal(DEBUG);
    }

    private void showInputMethodSubtypeMenu() {
        showInputMethodMenuInternal(true);
    }

    private void showInputMethodAndSubtypeEnabler(String inputMethodId) {
        Intent intent = new Intent("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
        intent.setFlags(337641472);
        if (!TextUtils.isEmpty(inputMethodId)) {
            intent.putExtra("input_method_id", inputMethodId);
        }
        this.mContext.startActivityAsUser(intent, null, UserHandle.CURRENT);
    }

    private void showConfigureInputMethods() {
        if (BenesseExtension.getDchaState() == 0) {
            Intent intent = new Intent("android.settings.INPUT_METHOD_SETTINGS");
            intent.setFlags(337641472);
            this.mContext.startActivityAsUser(intent, null, UserHandle.CURRENT);
        }
    }

    private boolean isScreenLocked() {
        if (this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardLocked() && this.mKeyguardManager.isKeyguardSecure()) {
            return true;
        }
        return DEBUG;
    }

    private void showInputMethodMenuInternal(boolean showSubtypes) {
        InputMethodSubtype currentSubtype;
        Context context = this.mContext;
        boolean isScreenLocked = isScreenLocked();
        String lastInputMethodId = this.mSettings.getSelectedInputMethod();
        int lastInputMethodSubtypeId = this.mSettings.getSelectedInputMethodSubtypeId(lastInputMethodId);
        synchronized (this.mMethodMap) {
            HashMap<InputMethodInfo, List<InputMethodSubtype>> immis = this.mSettings.getExplicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked(this.mContext);
            if (immis != null && immis.size() != 0) {
                hideInputMethodMenuLocked();
                List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList = this.mSwitchingController.getSortedInputMethodAndSubtypeListLocked(showSubtypes, this.mInputShown, isScreenLocked);
                if (lastInputMethodSubtypeId == -1 && (currentSubtype = getCurrentInputMethodSubtypeLocked()) != null) {
                    InputMethodInfo currentImi = this.mMethodMap.get(this.mCurMethodId);
                    lastInputMethodSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(currentImi, currentSubtype.hashCode());
                }
                int N = imList.size();
                this.mIms = new InputMethodInfo[N];
                this.mSubtypeIds = new int[N];
                int checkedItem = 0;
                for (int i = 0; i < N; i++) {
                    InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = imList.get(i);
                    this.mIms[i] = item.mImi;
                    this.mSubtypeIds[i] = item.mSubtypeId;
                    if (this.mIms[i].getId().equals(lastInputMethodId)) {
                        int subtypeId = this.mSubtypeIds[i];
                        if (subtypeId == -1 || ((lastInputMethodSubtypeId == -1 && subtypeId == 0) || subtypeId == lastInputMethodSubtypeId)) {
                            checkedItem = i;
                        }
                    }
                }
                Context settingsContext = new ContextThemeWrapper(context, R.style.Theme.DeviceDefault.Settings);
                this.mDialogBuilder = new AlertDialog.Builder(settingsContext);
                this.mDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        InputMethodManagerService.this.hideInputMethodMenu();
                    }
                });
                Context dialogContext = this.mDialogBuilder.getContext();
                TypedArray a = dialogContext.obtainStyledAttributes(null, com.android.internal.R.styleable.DialogPreference, R.attr.alertDialogStyle, 0);
                Drawable dialogIcon = a.getDrawable(2);
                a.recycle();
                this.mDialogBuilder.setIcon(dialogIcon);
                LayoutInflater inflater = (LayoutInflater) dialogContext.getSystemService("layout_inflater");
                View tv = inflater.inflate(R.layout.chooser_action_button, (ViewGroup) null);
                this.mDialogBuilder.setCustomTitle(tv);
                this.mSwitchingDialogTitleView = tv;
                this.mSwitchingDialogTitleView.findViewById(R.id.firstStrongLtr).setVisibility(this.mWindowManagerService.isHardKeyboardAvailable() ? 0 : 8);
                Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(R.id.firstStrongRtl);
                hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
                hardKeySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        InputMethodManagerService.this.mSettings.setShowImeWithHardKeyboard(isChecked);
                        InputMethodManagerService.this.hideInputMethodMenu();
                    }
                });
                final ImeSubtypeListAdapter adapter = new ImeSubtypeListAdapter(dialogContext, R.layout.chooser_action_row, imList, checkedItem);
                DialogInterface.OnClickListener choiceListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        synchronized (InputMethodManagerService.this.mMethodMap) {
                            if (InputMethodManagerService.this.mIms != null && InputMethodManagerService.this.mIms.length > which && InputMethodManagerService.this.mSubtypeIds != null && InputMethodManagerService.this.mSubtypeIds.length > which) {
                                InputMethodInfo im = InputMethodManagerService.this.mIms[which];
                                int subtypeId2 = InputMethodManagerService.this.mSubtypeIds[which];
                                adapter.mCheckedItem = which;
                                adapter.notifyDataSetChanged();
                                InputMethodManagerService.this.hideInputMethodMenu();
                                if (im != null) {
                                    if (subtypeId2 < 0 || subtypeId2 >= im.getSubtypeCount()) {
                                        subtypeId2 = -1;
                                    }
                                    InputMethodManagerService.this.setInputMethodLocked(im.getId(), subtypeId2);
                                }
                            }
                        }
                    }
                };
                this.mDialogBuilder.setSingleChoiceItems(adapter, checkedItem, choiceListener);
                if (showSubtypes && !isScreenLocked) {
                    DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            InputMethodManagerService.this.showConfigureInputMethods();
                        }
                    };
                    this.mDialogBuilder.setPositiveButton(R.string.indeterminate_progress_56, positiveListener);
                }
                this.mSwitchingDialog = this.mDialogBuilder.create();
                this.mSwitchingDialog.setCanceledOnTouchOutside(true);
                this.mSwitchingDialog.getWindow().setType(2012);
                this.mSwitchingDialog.getWindow().getAttributes().privateFlags |= 16;
                this.mSwitchingDialog.getWindow().getAttributes().setTitle("Select input method");
                updateImeWindowStatusLocked();
                this.mSwitchingDialog.show();
            }
        }
    }

    private static class ImeSubtypeListAdapter extends ArrayAdapter<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> {
        public int mCheckedItem;
        private final LayoutInflater mInflater;
        private final List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> mItemsList;
        private final int mTextViewResourceId;

        public ImeSubtypeListAdapter(Context context, int textViewResourceId, List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> itemsList, int checkedItem) {
            super(context, textViewResourceId, itemsList);
            this.mTextViewResourceId = textViewResourceId;
            this.mItemsList = itemsList;
            this.mCheckedItem = checkedItem;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            boolean z = InputMethodManagerService.DEBUG;
            View view = convertView != null ? convertView : this.mInflater.inflate(this.mTextViewResourceId, (ViewGroup) null);
            if (position >= 0 && position < this.mItemsList.size()) {
                InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = this.mItemsList.get(position);
                CharSequence imeName = item.mImeName;
                CharSequence subtypeName = item.mSubtypeName;
                TextView firstTextView = (TextView) view.findViewById(R.id.text1);
                TextView secondTextView = (TextView) view.findViewById(R.id.text2);
                if (TextUtils.isEmpty(subtypeName)) {
                    firstTextView.setText(imeName);
                    secondTextView.setVisibility(8);
                } else {
                    firstTextView.setText(subtypeName);
                    secondTextView.setText(imeName);
                    secondTextView.setVisibility(0);
                }
                RadioButton radioButton = (RadioButton) view.findViewById(R.id.fitCenter);
                if (position == this.mCheckedItem) {
                    z = true;
                }
                radioButton.setChecked(z);
            }
            return view;
        }
    }

    void hideInputMethodMenu() {
        synchronized (this.mMethodMap) {
            hideInputMethodMenuLocked();
        }
    }

    void hideInputMethodMenuLocked() {
        if (this.mSwitchingDialog != null) {
            this.mSwitchingDialog.dismiss();
            this.mSwitchingDialog = null;
        }
        updateImeWindowStatusLocked();
        this.mDialogBuilder = null;
        this.mIms = null;
    }

    public boolean setInputMethodEnabled(String id, boolean enabled) {
        boolean inputMethodEnabledLocked;
        if (!calledFromValidUser()) {
            return DEBUG;
        }
        synchronized (this.mMethodMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
            long ident = Binder.clearCallingIdentity();
            try {
                inputMethodEnabledLocked = setInputMethodEnabledLocked(id, enabled);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return inputMethodEnabledLocked;
    }

    boolean setInputMethodEnabledLocked(String id, boolean enabled) {
        InputMethodInfo imm = this.mMethodMap.get(id);
        if (imm == null) {
            throw new IllegalArgumentException("Unknown id: " + this.mCurMethodId);
        }
        List<Pair<String, ArrayList<String>>> enabledInputMethodsList = this.mSettings.getEnabledInputMethodsAndSubtypeListLocked();
        if (enabled) {
            for (Pair<String, ArrayList<String>> pair : enabledInputMethodsList) {
                if (((String) pair.first).equals(id)) {
                    return true;
                }
            }
            this.mSettings.appendAndPutEnabledInputMethodLocked(id, DEBUG);
            return DEBUG;
        }
        StringBuilder builder = new StringBuilder();
        if (!this.mSettings.buildAndPutEnabledInputMethodsStrRemovingIdLocked(builder, enabledInputMethodsList, id)) {
            return DEBUG;
        }
        String selId = this.mSettings.getSelectedInputMethod();
        if (id.equals(selId) && !chooseNewDefaultIMELocked()) {
            Slog.i(TAG, "Can't find new IME, unsetting the current input method.");
            resetSelectedInputMethodAndSubtypeLocked("");
        }
        return true;
    }

    private void setSelectedInputMethodAndSubtypeLocked(InputMethodInfo imi, int subtypeId, boolean setSubtypeOnly) {
        this.mSettings.saveCurrentInputMethodAndSubtypeToHistory(this.mCurMethodId, this.mCurrentSubtype);
        this.mCurUserActionNotificationSequenceNumber = Math.max(this.mCurUserActionNotificationSequenceNumber + 1, 1);
        if (this.mCurClient != null && this.mCurClient.client != null) {
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(3040, this.mCurUserActionNotificationSequenceNumber, this.mCurClient));
        }
        if (imi == null || subtypeId < 0) {
            this.mSettings.putSelectedSubtype(-1);
            this.mCurrentSubtype = null;
        } else if (subtypeId < imi.getSubtypeCount()) {
            InputMethodSubtype subtype = imi.getSubtypeAt(subtypeId);
            this.mSettings.putSelectedSubtype(subtype.hashCode());
            this.mCurrentSubtype = subtype;
        } else {
            this.mSettings.putSelectedSubtype(-1);
            this.mCurrentSubtype = getCurrentInputMethodSubtypeLocked();
        }
        if (this.mSystemReady && !setSubtypeOnly) {
            this.mSettings.putSelectedInputMethod(imi != null ? imi.getId() : "");
        }
    }

    private void resetSelectedInputMethodAndSubtypeLocked(String newDefaultIme) {
        String subtypeHashCode;
        InputMethodInfo imi = this.mMethodMap.get(newDefaultIme);
        int lastSubtypeId = -1;
        if (imi != null && !TextUtils.isEmpty(newDefaultIme) && (subtypeHashCode = this.mSettings.getLastSubtypeForInputMethodLocked(newDefaultIme)) != null) {
            try {
                lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, Integer.valueOf(subtypeHashCode).intValue());
            } catch (NumberFormatException e) {
                Slog.w(TAG, "HashCode for subtype looks broken: " + subtypeHashCode, e);
            }
        }
        setSelectedInputMethodAndSubtypeLocked(imi, lastSubtypeId, DEBUG);
    }

    private Pair<InputMethodInfo, InputMethodSubtype> findLastResortApplicableShortcutInputMethodAndSubtypeLocked(String mode) {
        List<InputMethodInfo> imis = this.mSettings.getEnabledInputMethodListLocked();
        InputMethodInfo mostApplicableIMI = null;
        InputMethodSubtype mostApplicableSubtype = null;
        boolean foundInSystemIME = DEBUG;
        Iterator<InputMethodInfo> it = imis.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            InputMethodInfo imi = it.next();
            String imiId = imi.getId();
            if (!foundInSystemIME || imiId.equals(this.mCurMethodId)) {
                InputMethodSubtype subtype = null;
                List<InputMethodSubtype> enabledSubtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                if (this.mCurrentSubtype != null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledSubtypes, mode, this.mCurrentSubtype.getLocale(), DEBUG);
                }
                if (subtype == null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledSubtypes, mode, (String) null, true);
                }
                ArrayList<InputMethodSubtype> overridingImplicitlyEnabledSubtypes = InputMethodUtils.getOverridingImplicitlyEnabledSubtypes(imi, mode);
                ArrayList<InputMethodSubtype> subtypesForSearch = overridingImplicitlyEnabledSubtypes.isEmpty() ? InputMethodUtils.getSubtypes(imi) : overridingImplicitlyEnabledSubtypes;
                if (subtype == null && this.mCurrentSubtype != null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, subtypesForSearch, mode, this.mCurrentSubtype.getLocale(), DEBUG);
                }
                if (subtype == null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, subtypesForSearch, mode, (String) null, true);
                }
                if (subtype == null) {
                    continue;
                } else {
                    if (imiId.equals(this.mCurMethodId)) {
                        mostApplicableIMI = imi;
                        mostApplicableSubtype = subtype;
                        break;
                    }
                    if (!foundInSystemIME) {
                        mostApplicableIMI = imi;
                        mostApplicableSubtype = subtype;
                        if ((imi.getServiceInfo().applicationInfo.flags & 1) != 0) {
                            foundInSystemIME = true;
                        }
                    }
                }
            }
        }
        if (mostApplicableIMI != null) {
            return new Pair<>(mostApplicableIMI, mostApplicableSubtype);
        }
        return null;
    }

    public InputMethodSubtype getCurrentInputMethodSubtype() {
        InputMethodSubtype currentInputMethodSubtypeLocked;
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mMethodMap) {
            currentInputMethodSubtypeLocked = getCurrentInputMethodSubtypeLocked();
        }
        return currentInputMethodSubtypeLocked;
    }

    private InputMethodSubtype getCurrentInputMethodSubtypeLocked() {
        if (this.mCurMethodId == null) {
            return null;
        }
        boolean subtypeIsSelected = this.mSettings.isSubtypeSelected();
        InputMethodInfo imi = this.mMethodMap.get(this.mCurMethodId);
        if (imi == null || imi.getSubtypeCount() == 0) {
            return null;
        }
        if (!subtypeIsSelected || this.mCurrentSubtype == null || !InputMethodUtils.isValidSubtypeId(imi, this.mCurrentSubtype.hashCode())) {
            int subtypeId = this.mSettings.getSelectedInputMethodSubtypeId(this.mCurMethodId);
            if (subtypeId == -1) {
                List<InputMethodSubtype> explicitlyOrImplicitlyEnabledSubtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                if (explicitlyOrImplicitlyEnabledSubtypes.size() == 1) {
                    this.mCurrentSubtype = explicitlyOrImplicitlyEnabledSubtypes.get(0);
                } else if (explicitlyOrImplicitlyEnabledSubtypes.size() > 1) {
                    this.mCurrentSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, explicitlyOrImplicitlyEnabledSubtypes, "keyboard", (String) null, true);
                    if (this.mCurrentSubtype == null) {
                        this.mCurrentSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, explicitlyOrImplicitlyEnabledSubtypes, (String) null, (String) null, true);
                    }
                }
            } else {
                this.mCurrentSubtype = (InputMethodSubtype) InputMethodUtils.getSubtypes(imi).get(subtypeId);
            }
        }
        return this.mCurrentSubtype;
    }

    private void addShortcutInputMethodAndSubtypes(InputMethodInfo imi, InputMethodSubtype subtype) {
        if (this.mShortcutInputMethodsAndSubtypes.containsKey(imi)) {
            this.mShortcutInputMethodsAndSubtypes.get(imi).add(subtype);
            return;
        }
        ArrayList<InputMethodSubtype> subtypes = new ArrayList<>();
        subtypes.add(subtype);
        this.mShortcutInputMethodsAndSubtypes.put(imi, subtypes);
    }

    public List getShortcutInputMethodsAndSubtypes() {
        ArrayList<Object> ret;
        synchronized (this.mMethodMap) {
            ret = new ArrayList<>();
            if (this.mShortcutInputMethodsAndSubtypes.size() == 0) {
                Pair<InputMethodInfo, InputMethodSubtype> info = findLastResortApplicableShortcutInputMethodAndSubtypeLocked("voice");
                if (info != null) {
                    ret.add(info.first);
                    ret.add(info.second);
                }
            } else {
                for (InputMethodInfo imi : this.mShortcutInputMethodsAndSubtypes.keySet()) {
                    ret.add(imi);
                    for (InputMethodSubtype subtype : this.mShortcutInputMethodsAndSubtypes.get(imi)) {
                        ret.add(subtype);
                    }
                }
            }
        }
        return ret;
    }

    public boolean setCurrentInputMethodSubtype(InputMethodSubtype subtype) {
        boolean z = DEBUG;
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (subtype != null) {
                    if (this.mCurMethodId != null) {
                        InputMethodInfo imi = this.mMethodMap.get(this.mCurMethodId);
                        int subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, subtype.hashCode());
                        if (subtypeId != -1) {
                            setInputMethodLocked(this.mCurMethodId, subtypeId);
                            z = true;
                        }
                    }
                }
            }
        }
        return z;
    }

    private static class InputMethodFileManager {
        private static final String ADDITIONAL_SUBTYPES_FILE_NAME = "subtypes.xml";
        private static final String ATTR_ICON = "icon";
        private static final String ATTR_ID = "id";
        private static final String ATTR_IME_SUBTYPE_EXTRA_VALUE = "imeSubtypeExtraValue";
        private static final String ATTR_IME_SUBTYPE_LOCALE = "imeSubtypeLocale";
        private static final String ATTR_IME_SUBTYPE_MODE = "imeSubtypeMode";
        private static final String ATTR_IS_AUXILIARY = "isAuxiliary";
        private static final String ATTR_LABEL = "label";
        private static final String INPUT_METHOD_PATH = "inputmethod";
        private static final String NODE_IMI = "imi";
        private static final String NODE_SUBTYPE = "subtype";
        private static final String NODE_SUBTYPES = "subtypes";
        private static final String SYSTEM_PATH = "system";
        private final AtomicFile mAdditionalInputMethodSubtypeFile;
        private final HashMap<String, List<InputMethodSubtype>> mAdditionalSubtypesMap = new HashMap<>();
        private final HashMap<String, InputMethodInfo> mMethodMap;

        public InputMethodFileManager(HashMap<String, InputMethodInfo> methodMap, int userId) {
            if (methodMap == null) {
                throw new NullPointerException("methodMap is null");
            }
            this.mMethodMap = methodMap;
            File systemDir = userId == 0 ? new File(Environment.getDataDirectory(), SYSTEM_PATH) : Environment.getUserSystemDirectory(userId);
            File inputMethodDir = new File(systemDir, INPUT_METHOD_PATH);
            if (!inputMethodDir.mkdirs()) {
                Slog.w(InputMethodManagerService.TAG, "Couldn't create dir.: " + inputMethodDir.getAbsolutePath());
            }
            File subtypeFile = new File(inputMethodDir, ADDITIONAL_SUBTYPES_FILE_NAME);
            this.mAdditionalInputMethodSubtypeFile = new AtomicFile(subtypeFile);
            if (!subtypeFile.exists()) {
                writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, methodMap);
            } else {
                readAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile);
            }
        }

        private void deleteAllInputMethodSubtypes(String imiId) {
            synchronized (this.mMethodMap) {
                this.mAdditionalSubtypesMap.remove(imiId);
                writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, this.mMethodMap);
            }
        }

        public void addInputMethodSubtypes(InputMethodInfo imi, InputMethodSubtype[] additionalSubtypes) {
            synchronized (this.mMethodMap) {
                ArrayList<InputMethodSubtype> subtypes = new ArrayList<>();
                for (InputMethodSubtype subtype : additionalSubtypes) {
                    if (!subtypes.contains(subtype)) {
                        subtypes.add(subtype);
                    } else {
                        Slog.w(InputMethodManagerService.TAG, "Duplicated subtype definition found: " + subtype.getLocale() + ", " + subtype.getMode());
                    }
                }
                this.mAdditionalSubtypesMap.put(imi.getId(), subtypes);
                writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, this.mMethodMap);
            }
        }

        public HashMap<String, List<InputMethodSubtype>> getAllAdditionalInputMethodSubtypes() {
            HashMap<String, List<InputMethodSubtype>> map;
            synchronized (this.mMethodMap) {
                map = this.mAdditionalSubtypesMap;
            }
            return map;
        }

        private static void writeAdditionalInputMethodSubtypes(HashMap<String, List<InputMethodSubtype>> allSubtypes, AtomicFile subtypesFile, HashMap<String, InputMethodInfo> methodMap) {
            boolean isSetMethodMap = (methodMap == null || methodMap.size() <= 0) ? InputMethodManagerService.DEBUG : true;
            FileOutputStream fos = null;
            try {
                fos = subtypesFile.startWrite();
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fos, "utf-8");
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                fastXmlSerializer.startTag(null, NODE_SUBTYPES);
                for (String imiId : allSubtypes.keySet()) {
                    if (isSetMethodMap && !methodMap.containsKey(imiId)) {
                        Slog.w(InputMethodManagerService.TAG, "IME uninstalled or not valid.: " + imiId);
                    } else {
                        fastXmlSerializer.startTag(null, NODE_IMI);
                        fastXmlSerializer.attribute(null, ATTR_ID, imiId);
                        List<InputMethodSubtype> subtypesList = allSubtypes.get(imiId);
                        int N = subtypesList.size();
                        for (int i = 0; i < N; i++) {
                            InputMethodSubtype subtype = subtypesList.get(i);
                            fastXmlSerializer.startTag(null, NODE_SUBTYPE);
                            fastXmlSerializer.attribute(null, ATTR_ICON, String.valueOf(subtype.getIconResId()));
                            fastXmlSerializer.attribute(null, ATTR_LABEL, String.valueOf(subtype.getNameResId()));
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_LOCALE, subtype.getLocale());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_MODE, subtype.getMode());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_EXTRA_VALUE, subtype.getExtraValue());
                            fastXmlSerializer.attribute(null, ATTR_IS_AUXILIARY, String.valueOf(subtype.isAuxiliary() ? 1 : 0));
                            fastXmlSerializer.endTag(null, NODE_SUBTYPE);
                        }
                        fastXmlSerializer.endTag(null, NODE_IMI);
                    }
                }
                fastXmlSerializer.endTag(null, NODE_SUBTYPES);
                fastXmlSerializer.endDocument();
                subtypesFile.finishWrite(fos);
            } catch (IOException e) {
                Slog.w(InputMethodManagerService.TAG, "Error writing subtypes", e);
                if (fos != null) {
                    subtypesFile.failWrite(fos);
                }
            }
        }

        private static void readAdditionalInputMethodSubtypes(HashMap<String, List<InputMethodSubtype>> allSubtypes, AtomicFile subtypesFile) {
            int type;
            if (allSubtypes == null || subtypesFile == null) {
                return;
            }
            allSubtypes.clear();
            FileInputStream fis = null;
            try {
                try {
                    try {
                        FileInputStream fis2 = subtypesFile.openRead();
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(fis2, null);
                        parser.getEventType();
                        do {
                            type = parser.next();
                            if (type == 2) {
                                break;
                            }
                        } while (type != 1);
                        String firstNodeName = parser.getName();
                        if (!NODE_SUBTYPES.equals(firstNodeName)) {
                            throw new XmlPullParserException("Xml doesn't start with subtypes");
                        }
                        int depth = parser.getDepth();
                        String currentImiId = null;
                        ArrayList<InputMethodSubtype> tempSubtypesArray = null;
                        while (true) {
                            int type2 = parser.next();
                            if ((type2 == 3 && parser.getDepth() <= depth) || type2 == 1) {
                                break;
                            }
                            if (type2 == 2) {
                                String nodeName = parser.getName();
                                if (NODE_IMI.equals(nodeName)) {
                                    currentImiId = parser.getAttributeValue(null, ATTR_ID);
                                    if (TextUtils.isEmpty(currentImiId)) {
                                        Slog.w(InputMethodManagerService.TAG, "Invalid imi id found in subtypes.xml");
                                    } else {
                                        tempSubtypesArray = new ArrayList<>();
                                        allSubtypes.put(currentImiId, tempSubtypesArray);
                                    }
                                } else if (NODE_SUBTYPE.equals(nodeName)) {
                                    if (TextUtils.isEmpty(currentImiId) || tempSubtypesArray == null) {
                                        Slog.w(InputMethodManagerService.TAG, "IME uninstalled or not valid.: " + currentImiId);
                                    } else {
                                        int icon = Integer.valueOf(parser.getAttributeValue(null, ATTR_ICON)).intValue();
                                        int label = Integer.valueOf(parser.getAttributeValue(null, ATTR_LABEL)).intValue();
                                        String imeSubtypeLocale = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_LOCALE);
                                        String imeSubtypeMode = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_MODE);
                                        String imeSubtypeExtraValue = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_EXTRA_VALUE);
                                        boolean isAuxiliary = "1".equals(String.valueOf(parser.getAttributeValue(null, ATTR_IS_AUXILIARY)));
                                        InputMethodSubtype subtype = new InputMethodSubtype.InputMethodSubtypeBuilder().setSubtypeNameResId(label).setSubtypeIconResId(icon).setSubtypeLocale(imeSubtypeLocale).setSubtypeMode(imeSubtypeMode).setSubtypeExtraValue(imeSubtypeExtraValue).setIsAuxiliary(isAuxiliary).build();
                                        tempSubtypesArray.add(subtype);
                                    }
                                }
                            }
                        }
                        if (fis2 != null) {
                            try {
                                fis2.close();
                            } catch (IOException e) {
                                Slog.w(InputMethodManagerService.TAG, "Failed to close.");
                            }
                        }
                    } catch (Throwable th) {
                        if (0 != 0) {
                            try {
                                fis.close();
                            } catch (IOException e2) {
                                Slog.w(InputMethodManagerService.TAG, "Failed to close.");
                            }
                        }
                        throw th;
                    }
                } catch (XmlPullParserException e3) {
                    Slog.w(InputMethodManagerService.TAG, "Error reading subtypes: " + e3);
                    if (0 != 0) {
                        try {
                            fis.close();
                        } catch (IOException e4) {
                            Slog.w(InputMethodManagerService.TAG, "Failed to close.");
                        }
                    }
                }
            } catch (IOException e5) {
                Slog.w(InputMethodManagerService.TAG, "Error reading subtypes: " + e5);
                if (0 != 0) {
                    try {
                        fis.close();
                    } catch (IOException e6) {
                        Slog.w(InputMethodManagerService.TAG, "Failed to close.");
                    }
                }
            } catch (NumberFormatException e7) {
                Slog.w(InputMethodManagerService.TAG, "Error reading subtypes: " + e7);
                if (0 != 0) {
                    try {
                        fis.close();
                    } catch (IOException e8) {
                        Slog.w(InputMethodManagerService.TAG, "Failed to close.");
                    }
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ClientState client;
        IInputMethod method;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump InputMethodManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        Printer p = new PrintWriterPrinter(pw);
        synchronized (this.mMethodMap) {
            p.println("Current Input Method Manager state:");
            int N = this.mMethodList.size();
            p.println("  Input Methods:");
            for (int i = 0; i < N; i++) {
                InputMethodInfo info = this.mMethodList.get(i);
                p.println("  InputMethod #" + i + ":");
                info.dump(p, "    ");
            }
            p.println("  Clients:");
            for (ClientState ci : this.mClients.values()) {
                p.println("  Client " + ci + ":");
                p.println("    client=" + ci.client);
                p.println("    inputContext=" + ci.inputContext);
                p.println("    sessionRequested=" + ci.sessionRequested);
                p.println("    curSession=" + ci.curSession);
            }
            p.println("  mCurMethodId=" + this.mCurMethodId);
            client = this.mCurClient;
            p.println("  mCurClient=" + client + " mCurSeq=" + this.mCurSeq);
            p.println("  mCurFocusedWindow=" + this.mCurFocusedWindow);
            p.println("  mCurId=" + this.mCurId + " mHaveConnect=" + this.mHaveConnection + " mBoundToMethod=" + this.mBoundToMethod);
            p.println("  mCurToken=" + this.mCurToken);
            p.println("  mCurIntent=" + this.mCurIntent);
            method = this.mCurMethod;
            p.println("  mCurMethod=" + this.mCurMethod);
            p.println("  mEnabledSession=" + this.mEnabledSession);
            p.println("  mShowRequested=" + this.mShowRequested + " mShowExplicitlyRequested=" + this.mShowExplicitlyRequested + " mShowForced=" + this.mShowForced + " mInputShown=" + this.mInputShown);
            p.println("  mCurUserActionNotificationSequenceNumber=" + this.mCurUserActionNotificationSequenceNumber);
            p.println("  mSystemReady=" + this.mSystemReady + " mInteractive=" + this.mScreenOn);
        }
        p.println(" ");
        if (client != null) {
            pw.flush();
            try {
                client.client.asBinder().dump(fd, args);
            } catch (RemoteException e) {
                p.println("Input method client dead: " + e);
            }
        } else {
            p.println("No input method client.");
        }
        p.println(" ");
        if (method != null) {
            pw.flush();
            try {
                method.asBinder().dump(fd, args);
                return;
            } catch (RemoteException e2) {
                p.println("Input method service dead: " + e2);
                return;
            }
        }
        p.println("No input method service.");
    }
}
