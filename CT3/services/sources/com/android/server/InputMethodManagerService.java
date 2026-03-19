package com.android.server;

import android.R;
import android.annotation.IntDef;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.AppOpsManager;
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManagerInternal;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.style.SuggestionSpan;
import android.util.ArrayMap;
import android.util.ArraySet;
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
import android.view.WindowManagerInternal;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnectionInspector;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManagerInternal;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
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
import com.android.internal.view.InputMethodClient;
import com.android.server.notification.ZenModeHelper;
import com.android.server.pm.PackageManagerService;
import com.android.server.statusbar.StatusBarManagerService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class InputMethodManagerService extends IInputMethodManager.Stub implements ServiceConnection, Handler.Callback {
    static boolean DEBUG = false;
    static final boolean DEBUG_RESTORE;
    static final int MSG_ATTACH_TOKEN = 1040;
    static final int MSG_BIND_CLIENT = 3010;
    static final int MSG_BIND_INPUT = 1010;
    static final int MSG_CREATE_SESSION = 1050;
    static final int MSG_HARD_KEYBOARD_SWITCH_CHANGED = 4000;
    static final int MSG_HIDE_CURRENT_INPUT_METHOD = 1035;
    static final int MSG_HIDE_SOFT_INPUT = 1030;
    static final int MSG_RESTART_INPUT = 2010;
    static final int MSG_SET_ACTIVE = 3020;
    static final int MSG_SET_INTERACTIVE = 3030;
    static final int MSG_SET_USER_ACTION_NOTIFICATION_SEQUENCE_NUMBER = 3040;
    static final int MSG_SHOW_IM_CONFIG = 3;
    static final int MSG_SHOW_IM_SUBTYPE_ENABLER = 2;
    static final int MSG_SHOW_IM_SUBTYPE_PICKER = 1;
    static final int MSG_SHOW_SOFT_INPUT = 1020;
    static final int MSG_START_INPUT = 2000;
    static final int MSG_SWITCH_IME = 3050;
    static final int MSG_UNBIND_CLIENT = 3000;
    static final int MSG_UNBIND_INPUT = 1000;
    private static final int NOT_A_SUBTYPE_ID = -1;
    static final int SECURE_SUGGESTION_SPANS_MAX_SIZE = 20;
    static final String TAG = "InputMethodManagerService";
    private static final String TAG_TRY_SUPPRESSING_IME_SWITCHER = "TrySuppressingImeSwitcher";
    static final long TIME_TO_RECONNECT = 3000;
    private boolean mAccessibilityRequestingNoSoftKeyboard;
    private final AppOpsManager mAppOpsManager;
    private String[] mArgs;
    boolean mBoundToMethod;
    final HandlerCaller mCaller;
    final Context mContext;
    EditorInfo mCurAttribute;
    ClientState mCurClient;
    private boolean mCurClientInKeyguard;
    IBinder mCurFocusedWindow;
    ClientState mCurFocusedWindowClient;
    String mCurId;
    IInputContext mCurInputContext;
    int mCurInputContextMissingMethods;
    Intent mCurIntent;
    IInputMethod mCurMethod;
    String mCurMethodId;
    int mCurSeq;
    IBinder mCurToken;
    private InputMethodSubtype mCurrentSubtype;
    private AlertDialog.Builder mDialogBuilder;
    SessionState mEnabledSession;
    private InputMethodFileManager mFileManager;
    private final int mHardKeyboardBehavior;
    final boolean mHasFeature;
    boolean mHaveConnection;
    private final boolean mImeSelectedOnBoot;
    private PendingIntent mImeSwitchPendingIntent;
    private Notification.Builder mImeSwitcherNotification;
    int mImeWindowVis;
    private InputMethodInfo[] mIms;
    boolean mInputShown;
    private KeyguardManager mKeyguardManager;
    long mLastBindTime;
    private LocaleList mLastSystemLocales;
    private int mNextArg;
    private NotificationManager mNotificationManager;
    private boolean mNotificationShown;
    final Resources mRes;
    final InputMethodUtils.InputMethodSettings mSettings;
    boolean mShowExplicitlyRequested;
    boolean mShowForced;
    boolean mShowForcedFromKey;
    private boolean mShowImeWithHardKeyboard;
    private boolean mShowOngoingImeSwitcherForPhones;
    boolean mShowRequested;
    private final String mSlotIme;
    private StatusBarManagerService mStatusBar;
    private int[] mSubtypeIds;
    private Toast mSubtypeSwitchedByShortCutToast;
    private final InputMethodSubtypeSwitchingController mSwitchingController;
    private AlertDialog mSwitchingDialog;
    private View mSwitchingDialogTitleView;
    boolean mSystemReady;
    private Timer mTimer;
    private final UserManager mUserManager;
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
    boolean mVisibleBound = false;
    final HashMap<IBinder, ClientState> mClients = new HashMap<>();
    private final HashMap<InputMethodInfo, ArrayList<InputMethodSubtype>> mShortcutInputMethodsAndSubtypes = new HashMap<>();
    boolean mIsInteractive = true;
    int mCurUserActionNotificationSequenceNumber = 0;
    int mBackDisposition = 0;
    private final MyPackageMonitor mMyPackageMonitor = new MyPackageMonitor();
    private int index1 = 0;
    private int index2 = 0;
    private final IPackageManager mIPackageManager = AppGlobals.getPackageManager();
    final Handler mHandler = new Handler(this);
    final SettingsObserver mSettingsObserver = new SettingsObserver(this.mHandler);
    final IWindowManager mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    final WindowManagerInternal mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
    private final HardKeyboardListener mHardKeyboardListener = new HardKeyboardListener(this, null);

    @IntDef({0, ZenModeHelper.SUPPRESSED_EFFECT_NOTIFICATIONS})
    @Retention(RetentionPolicy.SOURCE)
    private @interface HardKeyboardBehavior {
        public static final int WIRED_AFFORDANCE = 1;
        public static final int WIRELESS_AFFORDANCE = 0;
    }

    static {
        DEBUG_RESTORE = DEBUG;
    }

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
        boolean mRegistered;
        int mUserId;

        SettingsObserver(Handler handler) {
            super(handler);
            this.mRegistered = false;
            this.mLastEnabled = "";
        }

        public void registerContentObserverLocked(int userId) {
            if (this.mRegistered && this.mUserId == userId) {
                return;
            }
            ContentResolver resolver = InputMethodManagerService.this.mContext.getContentResolver();
            if (this.mRegistered) {
                InputMethodManagerService.this.mContext.getContentResolver().unregisterContentObserver(this);
                this.mRegistered = false;
            }
            if (this.mUserId != userId) {
                this.mLastEnabled = "";
                this.mUserId = userId;
            }
            resolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("enabled_input_methods"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("selected_input_method_subtype"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("show_ime_with_hard_keyboard"), false, this, userId);
            resolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_soft_keyboard_mode"), false, this, userId);
            this.mRegistered = true;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Uri showImeUri = Settings.Secure.getUriFor("show_ime_with_hard_keyboard");
            Uri accessibilityRequestingNoImeUri = Settings.Secure.getUriFor("accessibility_soft_keyboard_mode");
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (showImeUri.equals(uri)) {
                    InputMethodManagerService.this.updateKeyboardFromSettingsLocked();
                } else if (accessibilityRequestingNoImeUri.equals(uri)) {
                    InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard = Settings.Secure.getIntForUser(InputMethodManagerService.this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, this.mUserId) == 1;
                    if (InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard) {
                        boolean showRequested = InputMethodManagerService.this.mShowRequested;
                        InputMethodManagerService.this.hideCurrentInputLocked(0, null);
                        InputMethodManagerService.this.mShowRequested = showRequested;
                    } else if (InputMethodManagerService.this.mShowRequested) {
                        InputMethodManagerService.this.showCurrentInputLocked(1, null);
                    }
                } else {
                    boolean enabledChanged = false;
                    String newEnabled = InputMethodManagerService.this.mSettings.getEnabledInputMethodsStr();
                    if (!this.mLastEnabled.equals(newEnabled)) {
                        this.mLastEnabled = newEnabled;
                        enabledChanged = true;
                    }
                    InputMethodManagerService.this.updateInputMethodsFromSettingsLocked(enabledChanged);
                }
            }
        }

        public String toString() {
            return "SettingsObserver{mUserId=" + this.mUserId + " mRegistered=" + this.mRegistered + " mLastEnabled=" + this.mLastEnabled + "}";
        }
    }

    class ImmsBroadcastReceiver extends BroadcastReceiver {
        ImmsBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                InputMethodManagerService.this.hideInputMethodMenu();
                return;
            }
            if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_REMOVED".equals(action)) {
                InputMethodManagerService.this.updateCurrentProfileIds();
                return;
            }
            if ("android.os.action.SETTING_RESTORED".equals(action)) {
                String name = intent.getStringExtra("setting_name");
                if (!"enabled_input_methods".equals(name)) {
                    return;
                }
                String prevValue = intent.getStringExtra("previous_value");
                String newValue = intent.getStringExtra("new_value");
                InputMethodManagerService.restoreEnabledInputMethods(InputMethodManagerService.this.mContext, prevValue, newValue);
                return;
            }
            if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                if (!InputMethodManagerService.this.mInputShown) {
                    return;
                }
                InputMethodManagerService.this.hideCurrentInputLocked(0, null);
                Slog.i(InputMethodManagerService.TAG, "IPO shutdown");
                return;
            }
            Slog.w(InputMethodManagerService.TAG, "Unexpected intent " + intent);
        }
    }

    static void restoreEnabledInputMethods(Context context, String prevValue, String newValue) {
        if (DEBUG_RESTORE) {
            Slog.i(TAG, "Restoring enabled input methods:");
            Slog.i(TAG, "prev=" + prevValue);
            Slog.i(TAG, " new=" + newValue);
        }
        ArrayMap<String, ArraySet<String>> prevMap = InputMethodUtils.parseInputMethodsAndSubtypesString(prevValue);
        ArrayMap<String, ArraySet<String>> newMap = InputMethodUtils.parseInputMethodsAndSubtypesString(newValue);
        for (Map.Entry<String, ArraySet<String>> entry : newMap.entrySet()) {
            String imeId = entry.getKey();
            ArraySet<String> prevSubtypes = prevMap.get(imeId);
            if (prevSubtypes == null) {
                prevSubtypes = new ArraySet<>(2);
                prevMap.put(imeId, prevSubtypes);
            }
            prevSubtypes.addAll((ArraySet<? extends String>) entry.getValue());
        }
        String mergedImesAndSubtypesString = InputMethodUtils.buildInputMethodsAndSubtypesString(prevMap);
        if (DEBUG_RESTORE) {
            Slog.i(TAG, "Merged IME string:");
            Slog.i(TAG, "     " + mergedImesAndSubtypesString);
        }
        Settings.Secure.putString(context.getContentResolver(), "enabled_input_methods", mergedImesAndSubtypesString);
    }

    class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
        }

        private boolean isChangingPackagesOfCurrentUser() {
            int userId = getChangingUserId();
            boolean retval = userId == InputMethodManagerService.this.mSettings.getCurrentUserId();
            if (InputMethodManagerService.DEBUG && !retval) {
                Slog.d(InputMethodManagerService.TAG, "--- ignore this call back from a background user: " + userId);
            }
            return retval;
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            if (!isChangingPackagesOfCurrentUser()) {
                return false;
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
                                    if (!doit) {
                                        return true;
                                    }
                                    InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked("");
                                    InputMethodManagerService.this.chooseNewDefaultIMELocked();
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }

        public void onSomePackagesChanged() {
            if (!isChangingPackagesOfCurrentUser()) {
                return;
            }
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
                            InputMethodManagerService.this.setInputMethodEnabledLocked(imi.getId(), false);
                        }
                    }
                }
                InputMethodManagerService.this.buildInputMethodListLocked(false);
                boolean changed = false;
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
                            InputMethodManagerService.this.updateSystemUiLocked(InputMethodManagerService.this.mCurToken, 0, InputMethodManagerService.this.mBackDisposition);
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
                } else if (!changed && isPackageModified(curIm.getPackageName())) {
                    changed = true;
                }
                if (changed) {
                    InputMethodManagerService.this.updateFromSettingsLocked(false);
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

    private class HardKeyboardListener implements WindowManagerInternal.OnHardKeyboardStatusChangeListener {
        HardKeyboardListener(InputMethodManagerService this$0, HardKeyboardListener hardKeyboardListener) {
            this();
        }

        private HardKeyboardListener() {
        }

        public void onHardKeyboardStatusChange(boolean available) {
            InputMethodManagerService.this.mHandler.sendMessage(InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_HARD_KEYBOARD_SWITCH_CHANGED, Integer.valueOf(available ? 1 : 0)));
        }

        public void handleHardKeyboardStatusChange(boolean available) {
            if (InputMethodManagerService.DEBUG) {
                Slog.w(InputMethodManagerService.TAG, "HardKeyboardStatusChanged: available=" + available);
            }
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (InputMethodManagerService.this.mSwitchingDialog != null && InputMethodManagerService.this.mSwitchingDialogTitleView != null && InputMethodManagerService.this.mSwitchingDialog.isShowing()) {
                    InputMethodManagerService.this.mSwitchingDialogTitleView.findViewById(R.id.input_method_nav_back).setVisibility(available ? 0 : 8);
                }
            }
        }
    }

    public static final class Lifecycle extends SystemService {
        private InputMethodManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new InputMethodManagerService(context);
        }

        @Override
        public void onStart() {
            LocalServices.addService(InputMethodManagerInternal.class, new LocalServiceImpl(this.mService.mHandler));
            publishBinderService("input_method", this.mService);
        }

        @Override
        public void onSwitchUser(int userHandle) {
            this.mService.onSwitchUser(userHandle);
        }

        @Override
        public void onBootPhase(int phase) {
            if (phase != 550) {
                return;
            }
            StatusBarManagerService statusBarService = (StatusBarManagerService) ServiceManager.getService("statusbar");
            this.mService.systemRunning(statusBarService);
        }

        @Override
        public void onUnlockUser(int userHandle) {
            this.mService.onUnlockUser(userHandle);
        }
    }

    void onUnlockUser(int userId) {
        String preInstalledImeName;
        synchronized (this.mMethodMap) {
            int currentUserId = this.mSettings.getCurrentUserId();
            if (DEBUG) {
                Slog.d(TAG, "onUnlockUser: userId=" + userId + " curUserId=" + currentUserId);
            }
            if (userId != currentUserId) {
                return;
            }
            this.mSettings.switchCurrentUser(currentUserId, this.mSystemReady ? false : true);
            buildInputMethodListLocked(false);
            updateInputMethodsFromSettingsLocked(true);
            if (!this.mImeSelectedOnBoot && (preInstalledImeName = SystemProperties.get("ro.mtk_default_ime")) != null) {
                InputMethodInfo preInstalledImi = null;
                Iterator imi$iterator = this.mMethodList.iterator();
                while (true) {
                    if (!imi$iterator.hasNext()) {
                        break;
                    }
                    InputMethodInfo imi = (InputMethodInfo) imi$iterator.next();
                    Slog.i(TAG, "mMethodList service info : " + imi.getServiceName());
                    if (preInstalledImeName.equals(imi.getServiceName())) {
                        preInstalledImi = imi;
                        break;
                    }
                }
                Slog.i(TAG, "preInstalledImi= " + preInstalledImi);
                if (preInstalledImi != null) {
                    setInputMethodEnabledLocked(preInstalledImi.getId(), true);
                    setInputMethodLocked(preInstalledImi.getId(), -1);
                } else {
                    Slog.w(TAG, "Set preinstall ime as default fail.");
                    resetDefaultImeLocked(this.mContext);
                }
            }
        }
    }

    void onSwitchUser(int userId) {
        synchronized (this.mMethodMap) {
            switchUserLocked(userId);
        }
    }

    public InputMethodManagerService(Context context) {
        this.mContext = context;
        this.mRes = context.getResources();
        this.mCaller = new HandlerCaller(context, (Looper) null, new HandlerCaller.Callback() {
            public void executeMessage(Message msg) {
                InputMethodManagerService.this.handleMessage(msg);
            }
        }, true);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mHasFeature = context.getPackageManager().hasSystemFeature("android.software.input_methods");
        this.mSlotIme = this.mContext.getString(R.string.config_helpIntentExtraKey);
        this.mHardKeyboardBehavior = this.mContext.getResources().getInteger(R.integer.config_flipToScreenOffMaxLatencyMicros);
        Bundle extras = new Bundle();
        extras.putBoolean("android.allowDuringSetup", true);
        this.mImeSwitcherNotification = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_collapse_bundle).setWhen(0L).setOngoing(true).addExtras(extras).setCategory("sys").setColor(R.color.system_accent3_600);
        Intent intent = new Intent("android.settings.SHOW_INPUT_METHOD_PICKER");
        this.mImeSwitchPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        this.mShowOngoingImeSwitcherForPhones = false;
        IntentFilter broadcastFilter = new IntentFilter();
        broadcastFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        broadcastFilter.addAction("android.intent.action.USER_ADDED");
        broadcastFilter.addAction("android.intent.action.USER_REMOVED");
        broadcastFilter.addAction("android.os.action.SETTING_RESTORED");
        broadcastFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        this.mContext.registerReceiver(new ImmsBroadcastReceiver(), broadcastFilter);
        this.mNotificationShown = false;
        int userId = 0;
        try {
            userId = ActivityManagerNative.getDefault().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mMyPackageMonitor.register(this.mContext, null, UserHandle.ALL, true);
        this.mSettings = new InputMethodUtils.InputMethodSettings(this.mRes, context.getContentResolver(), this.mMethodMap, this.mMethodList, userId, !this.mSystemReady);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, userId);
        synchronized (this.mMethodMap) {
            this.mSwitchingController = InputMethodSubtypeSwitchingController.createInstanceLocked(this.mSettings, context);
        }
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        if (DEBUG) {
            Slog.d(TAG, "Initial default ime = " + defaultImiId);
        }
        this.mImeSelectedOnBoot = !TextUtils.isEmpty(defaultImiId);
        synchronized (this.mMethodMap) {
            buildInputMethodListLocked(!this.mImeSelectedOnBoot);
        }
        this.mSettings.enableAllIMEsIfThereIsNoEnabledIME();
        if (!this.mImeSelectedOnBoot) {
            Slog.w(TAG, "No IME selected. Choose the most applicable IME.");
            synchronized (this.mMethodMap) {
                resetDefaultImeLocked(context);
            }
        }
        synchronized (this.mMethodMap) {
            this.mSettingsObserver.registerContentObserverLocked(userId);
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
        if (this.mCurMethodId != null && !InputMethodUtils.isSystemIme(this.mMethodMap.get(this.mCurMethodId))) {
            return;
        }
        List<InputMethodInfo> suitableImes = InputMethodUtils.getDefaultEnabledImes(context, this.mSystemReady, this.mSettings.getEnabledInputMethodListLocked());
        if (suitableImes.isEmpty()) {
            Slog.i(TAG, "No default found");
            return;
        }
        InputMethodInfo defIm = suitableImes.get(0);
        Slog.i(TAG, "Default found, using " + defIm.getId());
        setSelectedInputMethodAndSubtypeLocked(defIm, -1, false);
    }

    private void resetAllInternalStateLocked(boolean updateOnlyWhenLocaleChanged, boolean resetDefaultEnabledIme) {
        if (!this.mSystemReady) {
            return;
        }
        LocaleList newLocales = this.mRes.getConfiguration().getLocales();
        if (updateOnlyWhenLocaleChanged && (newLocales == null || newLocales.equals(this.mLastSystemLocales))) {
            return;
        }
        if (!updateOnlyWhenLocaleChanged) {
            hideCurrentInputLocked(0, null);
            resetCurrentMethodAndClient(6);
        }
        if (DEBUG) {
            Slog.i(TAG, "LocaleList has been changed to " + newLocales);
        }
        buildInputMethodListLocked(resetDefaultEnabledIme);
        if (!updateOnlyWhenLocaleChanged) {
            String selectedImiId = this.mSettings.getSelectedInputMethod();
            if (TextUtils.isEmpty(selectedImiId)) {
                resetDefaultImeLocked(this.mContext);
            }
        } else {
            resetDefaultImeLocked(this.mContext);
        }
        updateFromSettingsLocked(true);
        this.mLastSystemLocales = newLocales;
        if (updateOnlyWhenLocaleChanged) {
            return;
        }
        try {
            startInputInnerLocked();
        } catch (RuntimeException e) {
            Slog.w(TAG, "Unexpected exception", e);
        }
    }

    private void resetStateIfCurrentLocaleChangedLocked() {
        resetAllInternalStateLocked(true, true);
    }

    private void switchUserLocked(int newUserId) {
        if (DEBUG) {
            Slog.d(TAG, "Switching user stage 1/3. newUserId=" + newUserId + " currentUserId=" + this.mSettings.getCurrentUserId());
        }
        this.mSettingsObserver.registerContentObserverLocked(newUserId);
        boolean useCopyOnWriteSettings = (this.mSystemReady && this.mUserManager.isUserUnlockingOrUnlocked(newUserId)) ? false : true;
        this.mSettings.switchCurrentUser(newUserId, useCopyOnWriteSettings);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, newUserId);
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        if (DEBUG) {
            Slog.d(TAG, "Switching user stage 2/3. newUserId=" + newUserId + " defaultImiId=" + defaultImiId);
        }
        boolean initialUserSwitch = TextUtils.isEmpty(defaultImiId);
        resetAllInternalStateLocked(false, initialUserSwitch);
        if (initialUserSwitch) {
            InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), newUserId, this.mContext.getBasePackageName());
        }
        if (!DEBUG) {
            return;
        }
        Slog.d(TAG, "Switching user stage 3/3. newUserId=" + newUserId + " selectedIme=" + this.mSettings.getSelectedInputMethod());
    }

    void updateCurrentProfileIds() {
        this.mSettings.setCurrentProfileIds(this.mUserManager.getProfileIdsWithDisabled(this.mSettings.getCurrentUserId()));
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
            if (DEBUG) {
                Slog.d(TAG, "--- systemReady");
            }
            if (!this.mSystemReady) {
                this.mSystemReady = true;
                int currentUserId = this.mSettings.getCurrentUserId();
                this.mSettings.switchCurrentUser(currentUserId, !this.mUserManager.isUserUnlockingOrUnlocked(currentUserId));
                this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
                this.mStatusBar = statusBar;
                if (this.mStatusBar != null) {
                    this.mStatusBar.setIconVisibility(this.mSlotIme, false);
                }
                updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                this.mShowOngoingImeSwitcherForPhones = this.mRes.getBoolean(R.^attr-private.__removed6);
                if (this.mShowOngoingImeSwitcherForPhones) {
                    this.mWindowManagerInternal.setOnHardKeyboardStatusChangeListener(this.mHardKeyboardListener);
                }
                buildInputMethodListLocked(!this.mImeSelectedOnBoot);
                if (!this.mImeSelectedOnBoot) {
                    Slog.w(TAG, "Reset the default IME as \"Resource\" is ready here.");
                    String preInstalledImeName = SystemProperties.get("ro.mtk_default_ime");
                    if (preInstalledImeName != null) {
                        InputMethodInfo preInstalledImi = null;
                        Iterator imi$iterator = this.mMethodList.iterator();
                        while (true) {
                            if (!imi$iterator.hasNext()) {
                                break;
                            }
                            InputMethodInfo imi = (InputMethodInfo) imi$iterator.next();
                            Slog.i(TAG, "mMethodList service info : " + imi.getServiceName());
                            if (preInstalledImeName.equals(imi.getServiceName())) {
                                preInstalledImi = imi;
                                break;
                            }
                        }
                        if (preInstalledImi != null) {
                            setInputMethodLocked(preInstalledImi.getId(), -1);
                        } else {
                            Slog.w(TAG, "Set preinstall ime as default fail.");
                            resetDefaultImeLocked(this.mContext);
                        }
                    }
                    resetStateIfCurrentLocaleChangedLocked();
                    InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), this.mSettings.getCurrentUserId(), this.mContext.getBasePackageName());
                }
                this.mLastSystemLocales = this.mRes.getConfiguration().getLocales();
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

    public void refreshImeWindowVisibilityLocked() {
        Configuration conf = this.mRes.getConfiguration();
        boolean haveHardKeyboard = conf.keyboard != 1;
        boolean hardKeyShown = haveHardKeyboard && conf.hardKeyboardHidden != 2;
        boolean isScreenLocked = isKeyguardLocked();
        boolean inputActive = !isScreenLocked ? !this.mInputShown ? hardKeyShown : true : false;
        boolean inputVisible = inputActive && !hardKeyShown;
        this.mImeWindowVis = (inputVisible ? 2 : 0) | (inputActive ? 1 : 0);
        updateImeWindowStatusLocked();
    }

    private void updateImeWindowStatusLocked() {
        setImeWindowStatus(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
    }

    private boolean calledFromValidUser() {
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (DEBUG) {
            Slog.d(TAG, "--- calledFromForegroundUserOrSystemProcess ? calling uid = " + uid + " system uid = 1000 calling userId = " + userId + ", foreground user id = " + this.mSettings.getCurrentUserId() + ", calling pid = " + Binder.getCallingPid() + InputMethodUtils.getApiCallStack());
        }
        if (uid == 1000 || this.mSettings.isCurrentProfile(userId)) {
            return true;
        }
        if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            Slog.w(TAG, "--- IPC called from background users. Ignore. callers=" + Debug.getCallers(10));
            return false;
        }
        if (DEBUG) {
            Slog.d(TAG, "--- Access granted because the calling process has the INTERACT_ACROSS_USERS_FULL permission");
        }
        return true;
    }

    private boolean calledWithValidToken(IBinder token) {
        if (token == null || this.mCurToken != token) {
            return false;
        }
        return true;
    }

    private boolean bindCurrentInputMethodService(Intent service, ServiceConnection conn, int flags) {
        if (service == null || conn == null) {
            Slog.e(TAG, "--- bind failed: service = " + service + ", conn = " + conn);
            return false;
        }
        return this.mContext.bindServiceAsUser(service, conn, flags, new UserHandle(this.mSettings.getCurrentUserId()));
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
        ArrayList enabledInputMethodListLocked;
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
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            this.mClients.put(client.asBinder(), new ClientState(client, inputContext, uid, pid));
        }
    }

    public void removeClient(IInputMethodClient client) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            ClientState cs = this.mClients.remove(client.asBinder());
            if (cs != null) {
                clearClientSessionLocked(cs);
                if (this.mCurClient == cs) {
                    this.mCurClient = null;
                }
                if (this.mCurFocusedWindowClient == cs) {
                    this.mCurFocusedWindowClient = null;
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

    void unbindCurrentClientLocked(int unbindClientReason) {
        if (this.mCurClient == null) {
            return;
        }
        if (DEBUG) {
            Slog.v(TAG, "unbindCurrentInputLocked: client=" + this.mCurClient.client.asBinder());
        }
        if (this.mBoundToMethod) {
            this.mBoundToMethod = false;
            if (this.mCurMethod != null) {
                executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(1000, this.mCurMethod));
            }
        }
        executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_SET_ACTIVE, 0, this.mCurClient));
        executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_UNBIND_CLIENT, this.mCurSeq, unbindClientReason, this.mCurClient.client));
        this.mCurClient.sessionRequested = false;
        this.mCurClient = null;
        hideInputMethodMenuLocked();
    }

    private int getImeShowFlags() {
        if (this.mShowForcedFromKey) {
            return 5;
        }
        if (this.mShowForced) {
            return 3;
        }
        if (!this.mShowExplicitlyRequested) {
            return 0;
        }
        return 1;
    }

    private int getAppShowFlags() {
        if (this.mShowForced) {
            return 2;
        }
        if (this.mShowExplicitlyRequested) {
            return 0;
        }
        return 1;
    }

    InputBindResult attachNewInputLocked(boolean initial) {
        if (!this.mBoundToMethod) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_BIND_INPUT, this.mCurMethod, this.mCurClient.binding));
            this.mBoundToMethod = true;
        }
        SessionState session = this.mCurClient.curSession;
        if (initial) {
            executeOrSendMessage(session.method, this.mCaller.obtainMessageIOOO(MSG_START_INPUT, this.mCurInputContextMissingMethods, session, this.mCurInputContext, this.mCurAttribute));
        } else {
            executeOrSendMessage(session.method, this.mCaller.obtainMessageIOOO(MSG_RESTART_INPUT, this.mCurInputContextMissingMethods, session, this.mCurInputContext, this.mCurAttribute));
        }
        if (this.mShowRequested) {
            if (DEBUG) {
                Slog.v(TAG, "Attach new input asks to show input");
            }
            showCurrentInputLocked(getAppShowFlags(), null);
        }
        return new InputBindResult(session.session, session.channel != null ? session.channel.dup() : null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
    }

    InputBindResult startInputLocked(int startInputReason, IInputMethodClient client, IInputContext inputContext, int missingMethods, EditorInfo attribute, int controlFlags) {
        if (this.mCurMethodId == null) {
            return this.mNoBinding;
        }
        ClientState cs = this.mClients.get(client.asBinder());
        if (cs == null) {
            throw new IllegalArgumentException("unknown client " + client.asBinder());
        }
        if (attribute == null) {
            Slog.w(TAG, "Ignoring startInput with null EditorInfo. uid=" + cs.uid + " pid=" + cs.pid);
            return null;
        }
        try {
            if (!this.mIWindowManager.inputMethodClientHasFocus(cs.client)) {
                Slog.w(TAG, "Starting input on non-focused client " + cs.client + " (uid=" + cs.uid + " pid=" + cs.pid + ")");
                return null;
            }
        } catch (RemoteException e) {
        }
        return startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, controlFlags);
    }

    InputBindResult startInputUncheckedLocked(ClientState cs, IInputContext inputContext, int missingMethods, EditorInfo attribute, int controlFlags) {
        if (this.mCurMethodId == null) {
            return this.mNoBinding;
        }
        if (!InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, cs.uid, attribute.packageName)) {
            Slog.e(TAG, "Rejecting this client as it reported an invalid package name. uid=" + cs.uid + " package=" + attribute.packageName);
            return this.mNoBinding;
        }
        if (this.mCurClient != cs) {
            this.mCurClientInKeyguard = isKeyguardLocked();
            unbindCurrentClientLocked(1);
            if (DEBUG) {
                Slog.v(TAG, "switching to client: client=" + cs.client.asBinder() + " keyguard=" + this.mCurClientInKeyguard);
            }
            if (this.mIsInteractive) {
                executeOrSendMessage(cs.client, this.mCaller.obtainMessageIO(MSG_SET_ACTIVE, this.mIsInteractive ? 1 : 0, cs));
            }
        }
        this.mCurSeq++;
        if (this.mCurSeq <= 0) {
            this.mCurSeq = 1;
        }
        this.mCurClient = cs;
        this.mCurInputContext = inputContext;
        this.mCurInputContextMissingMethods = missingMethods;
        this.mCurAttribute = attribute;
        if (this.mCurId != null && this.mCurId.equals(this.mCurMethodId)) {
            if (cs.curSession != null) {
                return attachNewInputLocked((controlFlags & 256) != 0);
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
            Slog.w(TAG, "Unknown id: " + this.mCurMethodId + ", rebuild the method list and reset to the default Ime.");
            buildInputMethodListLocked(true);
            info = this.mMethodMap.get(this.mCurMethodId);
            if (info == null) {
                throw new IllegalArgumentException("Unknown id: " + this.mCurMethodId);
            }
        }
        unbindCurrentMethodLocked(true);
        this.mCurIntent = new Intent("android.view.InputMethod");
        this.mCurIntent.setComponent(info.getComponent());
        this.mCurIntent.putExtra("android.intent.extra.client_label", R.string.force_close);
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

    private InputBindResult startInput(int startInputReason, IInputMethodClient client, IInputContext inputContext, int missingMethods, EditorInfo attribute, int controlFlags) {
        InputBindResult inputBindResultStartInputLocked;
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mMethodMap) {
            if (DEBUG) {
                Slog.v(TAG, "startInput: reason=" + InputMethodClient.getStartInputReason(startInputReason) + " client = " + client.asBinder() + " inputContext=" + inputContext + " missingMethods=" + InputConnectionInspector.getMissingMethodFlagsAsString(missingMethods) + " attribute=" + attribute + " controlFlags=#" + Integer.toHexString(controlFlags));
            }
            long ident = Binder.clearCallingIdentity();
            try {
                inputBindResultStartInputLocked = startInputLocked(startInputReason, client, inputContext, missingMethods, attribute, controlFlags);
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
                    unbindCurrentMethodLocked(false);
                    return;
                }
                if (DEBUG) {
                    Slog.v(TAG, "Initiating attach with token: " + this.mCurToken);
                }
                executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_ATTACH_TOKEN, this.mCurMethod, this.mCurToken));
                if (this.mCurClient != null) {
                    clearClientSessionLocked(this.mCurClient);
                    requestClientSessionLocked(this.mCurClient);
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

    void unbindCurrentMethodLocked(boolean savePosition) {
        if (this.mVisibleBound) {
            this.mContext.unbindService(this.mVisibleConnection);
            this.mVisibleBound = false;
        }
        if (this.mHaveConnection) {
            this.mContext.unbindService(this);
            this.mHaveConnection = false;
        }
        if (this.mCurToken != null) {
            try {
                if (DEBUG) {
                    Slog.v(TAG, "Removing window token: " + this.mCurToken);
                }
                if ((this.mImeWindowVis & 1) != 0 && savePosition) {
                    this.mWindowManagerInternal.saveLastInputMethodWindowForTransition();
                }
                this.mIWindowManager.removeWindowToken(this.mCurToken);
            } catch (RemoteException e) {
            }
            this.mCurToken = null;
        }
        this.mCurId = null;
        clearCurMethodLocked();
    }

    void resetCurrentMethodAndClient(int unbindClientReason) {
        this.mCurMethodId = null;
        unbindCurrentMethodLocked(false);
        unbindCurrentClientLocked(unbindClientReason);
    }

    void requestClientSessionLocked(ClientState cs) {
        if (cs.sessionRequested) {
            return;
        }
        if (DEBUG) {
            Slog.v(TAG, "Creating new session for client " + cs);
        }
        InputChannel[] channels = InputChannel.openInputChannelPair(cs.toString());
        cs.sessionRequested = true;
        executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOOO(MSG_CREATE_SESSION, this.mCurMethod, channels[1], new MethodCallback(this, this.mCurMethod, channels[0])));
    }

    void clearClientSessionLocked(ClientState cs) {
        finishSessionLocked(cs.curSession);
        cs.curSession = null;
        cs.sessionRequested = false;
    }

    private void finishSessionLocked(SessionState sessionState) {
        if (sessionState == null) {
            return;
        }
        if (sessionState.session != null) {
            try {
                sessionState.session.finishSession();
            } catch (RemoteException e) {
                Slog.w(TAG, "Session failed to close due to remote exception", e);
                updateSystemUiLocked(this.mCurToken, 0, this.mBackDisposition);
            }
            sessionState.session = null;
        }
        if (sessionState.channel == null) {
            return;
        }
        sessionState.channel.dispose();
        sessionState.channel = null;
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
        if (this.mStatusBar == null) {
            return;
        }
        this.mStatusBar.setIconVisibility(this.mSlotIme, false);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        synchronized (this.mMethodMap) {
            if (DEBUG) {
                Slog.v(TAG, "Service disconnected: " + name + " mCurIntent=" + this.mCurIntent);
            }
            if (this.mCurMethod != null && this.mCurIntent != null && name.equals(this.mCurIntent.getComponent())) {
                clearCurMethodLocked();
                this.mLastBindTime = SystemClock.uptimeMillis();
                this.mShowRequested = this.mInputShown;
                this.mInputShown = false;
                if (this.mCurClient != null) {
                    executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_UNBIND_CLIENT, 3, this.mCurSeq, this.mCurClient.client));
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
                    if (DEBUG) {
                        Slog.d(TAG, "hide the small icon for the input method");
                    }
                    if (this.mStatusBar != null) {
                        this.mStatusBar.setIconVisibility(this.mSlotIme, false);
                    }
                } else if (packageName != null) {
                    if (DEBUG) {
                        Slog.d(TAG, "show a small icon for the input method");
                    }
                    CharSequence contentDescription = null;
                    try {
                        PackageManager packageManager = this.mContext.getPackageManager();
                        contentDescription = packageManager.getApplicationLabel(this.mIPackageManager.getApplicationInfo(packageName, 0, this.mSettings.getCurrentUserId()));
                    } catch (RemoteException e) {
                    }
                    if (this.mStatusBar != null) {
                        this.mStatusBar.setIcon(this.mSlotIme, packageName, iconId, 0, contentDescription != null ? contentDescription.toString() : null);
                        this.mStatusBar.setIconVisibility(this.mSlotIme, true);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private boolean shouldShowImeSwitcherLocked(int visibility) {
        if (!this.mShowOngoingImeSwitcherForPhones || this.mSwitchingDialog != null || isScreenLocked() || (visibility & 1) == 0) {
            return false;
        }
        if (this.mWindowManagerInternal.isHardKeyboardAvailable()) {
            if (this.mHardKeyboardBehavior == 0) {
                return true;
            }
        } else if ((visibility & 2) == 0) {
            return false;
        }
        List<InputMethodInfo> imis = this.mSettings.getEnabledInputMethodListLocked();
        int N = imis.size();
        if (N > 2) {
            return true;
        }
        if (N < 1) {
            return false;
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
            if (nonAuxSubtype != null && auxSubtype != null) {
                if ((nonAuxSubtype.getLocale().equals(auxSubtype.getLocale()) || auxSubtype.overridesImplicitlyEnabledSubtype() || nonAuxSubtype.overridesImplicitlyEnabledSubtype()) && nonAuxSubtype.containsExtraValueKey(TAG_TRY_SUPPRESSING_IME_SWITCHER)) {
                    return false;
                }
                return true;
            }
            return true;
        }
        return false;
    }

    private boolean isKeyguardLocked() {
        if (this.mKeyguardManager != null) {
            return this.mKeyguardManager.isKeyguardLocked();
        }
        return false;
    }

    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
        if (!calledWithValidToken(token)) {
            int uid = Binder.getCallingUid();
            Slog.e(TAG, "Ignoring setImeWindowStatus due to an invalid token. uid:" + uid + " token:" + token);
            return;
        }
        synchronized (this.mMethodMap) {
            this.mImeWindowVis = vis;
            this.mBackDisposition = backDisposition;
            updateSystemUiLocked(token, vis, backDisposition);
        }
    }

    private void updateSystemUi(IBinder token, int vis, int backDisposition) {
        synchronized (this.mMethodMap) {
            updateSystemUiLocked(token, vis, backDisposition);
        }
    }

    private void updateSystemUiLocked(IBinder token, int vis, int backDisposition) {
        if (!calledWithValidToken(token)) {
            int uid = Binder.getCallingUid();
            Slog.e(TAG, "Ignoring updateSystemUiLocked due to an invalid token. uid:" + uid + " token:" + token);
            return;
        }
        long ident = Binder.clearCallingIdentity();
        if (vis != 0) {
            try {
                if (isKeyguardLocked() && !this.mCurClientInKeyguard) {
                    vis = 0;
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        boolean needsToShowImeSwitcher = shouldShowImeSwitcherLocked(vis);
        if (this.mStatusBar != null) {
            this.mStatusBar.setImeWindowStatus(token, vis, backDisposition, needsToShowImeSwitcher);
        }
        InputMethodInfo imi = this.mMethodMap.get(this.mCurMethodId);
        if (imi != null && needsToShowImeSwitcher) {
            CharSequence title = this.mRes.getText(R.string.fallback_wallpaper_component);
            CharSequence summary = InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, imi, this.mCurrentSubtype);
            this.mImeSwitcherNotification.setContentTitle(title).setContentText(summary).setContentIntent(this.mImeSwitchPendingIntent);
            try {
                if (this.mNotificationManager != null && !this.mIWindowManager.hasNavigationBar()) {
                    if (DEBUG) {
                        Slog.d(TAG, "--- show notification: label =  " + summary);
                    }
                    this.mNotificationManager.notifyAsUser(null, R.string.fallback_wallpaper_component, this.mImeSwitcherNotification.build(), UserHandle.ALL);
                    this.mNotificationShown = true;
                }
            } catch (RemoteException e) {
            }
        } else if (this.mNotificationShown && this.mNotificationManager != null) {
            if (DEBUG) {
                Slog.d(TAG, "--- hide notification");
            }
            this.mNotificationManager.cancelAsUser(null, R.string.fallback_wallpaper_component, UserHandle.ALL);
            this.mNotificationShown = false;
        }
    }

    public void registerSuggestionSpansForNotification(SuggestionSpan[] spans) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            InputMethodInfo currentImi = this.mMethodMap.get(this.mCurMethodId);
            for (SuggestionSpan ss : spans) {
                if (!TextUtils.isEmpty(ss.getNotificationTargetClassName())) {
                    this.mSecureSuggestionSpans.put(ss, currentImi);
                }
            }
        }
    }

    public boolean notifySuggestionPicked(SuggestionSpan span, String originalString, int index) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            InputMethodInfo targetImi = this.mSecureSuggestionSpans.get(span);
            if (targetImi == null) {
                return false;
            }
            String[] suggestions = span.getSuggestions();
            if (index < 0 || index >= suggestions.length) {
                return false;
            }
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
                return true;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
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
                    ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(imm.getPackageName(), PackageManagerService.DumpState.DUMP_VERSION, this.mSettings.getCurrentUserId());
                    if (ai != null && ai.enabledSetting == 4) {
                        if (DEBUG) {
                            Slog.d(TAG, "Update state(" + imm.getId() + "): DISABLED_UNTIL_USED -> DEFAULT");
                        }
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
                resetCurrentMethodAndClient(5);
            }
            this.mShortcutInputMethodsAndSubtypes.clear();
        } else {
            resetCurrentMethodAndClient(4);
        }
        this.mSwitchingController.resetCircularListLocked(this.mContext);
    }

    public void updateKeyboardFromSettingsLocked() {
        this.mShowImeWithHardKeyboard = this.mSettings.isShowImeWithHardKeyboardEnabled();
        if (this.mSwitchingDialog == null || this.mSwitchingDialogTitleView == null || !this.mSwitchingDialog.isShowing()) {
            return;
        }
        Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(R.id.input_method_nav_buttons);
        hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
    }

    private void notifyInputMethodSubtypeChanged(int userId, InputMethodInfo inputMethodInfo, InputMethodSubtype subtype) {
        InputManagerInternal inputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        if (inputManagerInternal == null) {
            return;
        }
        inputManagerInternal.onInputMethodSubtypeChanged(userId, inputMethodInfo, subtype);
    }

    void setInputMethodLocked(String id, int subtypeId) {
        InputMethodSubtype newSubtype;
        InputMethodInfo info = this.mMethodMap.get(id);
        if (info == null) {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        if (id.equals(this.mCurMethodId)) {
            int subtypeCount = info.getSubtypeCount();
            if (subtypeCount <= 0) {
                return;
            }
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
                        updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                        this.mCurMethod.changeInputMethodSubtype(newSubtype);
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Failed to call changeInputMethodSubtype");
                        return;
                    }
                }
                notifyInputMethodSubtypeChanged(this.mSettings.getCurrentUserId(), info, newSubtype);
                return;
            }
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            setSelectedInputMethodAndSubtypeLocked(info, subtypeId, false);
            this.mCurMethodId = id;
            if (ActivityManagerNative.isSystemReady()) {
                Intent intent = new Intent("android.intent.action.INPUT_METHOD_CHANGED");
                intent.addFlags(536870912);
                intent.putExtra("input_method_id", id);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
            unbindCurrentClientLocked(2);
            Binder.restoreCallingIdentity(ident);
            notifyInputMethodSubtypeChanged(this.mSettings.getCurrentUserId(), info, getCurrentInputMethodSubtypeLocked());
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public boolean showSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        if (!calledFromValidUser()) {
            return false;
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                    if (!this.mIWindowManager.inputMethodClientHasFocus(client)) {
                        Slog.w(TAG, "Ignoring showSoftInput of uid " + uid + ": " + client);
                        return false;
                    }
                }
                if (DEBUG) {
                    Slog.v(TAG, "Client requesting input be shown");
                }
                return showCurrentInputLocked(flags, resultReceiver);
            }
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean showCurrentInputLocked(int flags, ResultReceiver resultReceiver) {
        this.mShowRequested = true;
        if (this.mAccessibilityRequestingNoSoftKeyboard) {
            return false;
        }
        if ((flags & 2) != 0) {
            this.mShowExplicitlyRequested = true;
            this.mShowForced = true;
        } else if ((flags & 1) == 0) {
            this.mShowExplicitlyRequested = true;
        }
        if ((flags & 4) != 0) {
            this.mShowExplicitlyRequested = true;
            this.mShowForcedFromKey = true;
        }
        if (!this.mSystemReady) {
            return false;
        }
        if (this.mCurMethod != null) {
            if (DEBUG) {
                Slog.d(TAG, "showCurrentInputLocked: mCurToken=" + this.mCurToken);
            }
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageIOO(MSG_SHOW_SOFT_INPUT, getImeShowFlags(), this.mCurMethod, resultReceiver));
            this.mInputShown = true;
            if (this.mHaveConnection && !this.mVisibleBound) {
                bindCurrentInputMethodService(this.mCurIntent, this.mVisibleConnection, 201326593);
                this.mVisibleBound = true;
            }
            return true;
        }
        if (this.mHaveConnection && SystemClock.uptimeMillis() >= this.mLastBindTime + TIME_TO_RECONNECT) {
            EventLog.writeEvent(EventLogTags.IMF_FORCE_RECONNECT_IME, this.mCurMethodId, Long.valueOf(SystemClock.uptimeMillis() - this.mLastBindTime), 1);
            Slog.w(TAG, "Force disconnect/connect to the IME in showCurrentInputLocked()");
            this.mContext.unbindService(this);
            bindCurrentInputMethodService(this.mCurIntent, this, 1073741825);
            return false;
        }
        if (!DEBUG) {
            return false;
        }
        Slog.d(TAG, "Can't show input: connection = " + this.mHaveConnection + ", time = " + ((this.mLastBindTime + TIME_TO_RECONNECT) - SystemClock.uptimeMillis()));
        return false;
    }

    public boolean hideSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        if (!calledFromValidUser()) {
            return false;
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                    if (!this.mIWindowManager.inputMethodClientHasFocus(client)) {
                        if (DEBUG) {
                            Slog.w(TAG, "Ignoring hideSoftInput of uid " + uid + ": " + client);
                        }
                        return false;
                    }
                }
                if (DEBUG) {
                    Slog.v(TAG, "Client requesting input be hidden");
                }
                return hideCurrentInputLocked(flags, resultReceiver);
            }
        } catch (RemoteException e) {
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean hideCurrentInputLocked(int flags, ResultReceiver resultReceiver) {
        boolean res;
        boolean shouldHideSoftInput = true;
        if ((flags & 1) != 0 && (this.mShowExplicitlyRequested || this.mShowForced)) {
            if (DEBUG) {
                Slog.v(TAG, "Not hiding: explicit show not cancelled by non-explicit hide");
            }
            return false;
        }
        if (this.mShowForced && (flags & 2) != 0) {
            if (DEBUG) {
                Slog.v(TAG, "Not hiding: forced show not cancelled by not-always hide");
            }
            return false;
        }
        if (this.mCurMethod == null) {
            shouldHideSoftInput = false;
        } else if (!this.mInputShown && (this.mImeWindowVis & 1) == 0) {
            shouldHideSoftInput = false;
        }
        if (shouldHideSoftInput) {
            hideInputMethodMenu();
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_HIDE_SOFT_INPUT, this.mCurMethod, resultReceiver));
            res = true;
        } else {
            res = false;
        }
        if (this.mHaveConnection && this.mVisibleBound) {
            try {
                this.mContext.unbindService(this.mVisibleConnection);
            } catch (IllegalArgumentException e) {
                if (DEBUG) {
                    Slog.v(TAG, e.toString());
                }
            }
            this.mVisibleBound = false;
        }
        this.mInputShown = false;
        this.mShowRequested = false;
        this.mShowExplicitlyRequested = false;
        this.mShowForced = false;
        this.mShowForcedFromKey = false;
        return res;
    }

    public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int controlFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, int missingMethods) {
        if (windowToken != null) {
            return windowGainedFocus(startInputReason, client, windowToken, controlFlags, softInputMode, windowFlags, attribute, inputContext, missingMethods);
        }
        return startInput(startInputReason, client, inputContext, missingMethods, attribute, controlFlags);
    }

    private InputBindResult windowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int controlFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, int missingMethods) {
        boolean calledFromValidUser = calledFromValidUser();
        InputBindResult res = null;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (DEBUG) {
                    Slog.v(TAG, "windowGainedFocus: reason=" + InputMethodClient.getStartInputReason(startInputReason) + " client=" + client.asBinder() + " inputContext=" + inputContext + " missingMethods=" + InputConnectionInspector.getMissingMethodFlagsAsString(missingMethods) + " attribute=" + attribute + " controlFlags=#" + Integer.toHexString(controlFlags) + " softInputMode=#" + Integer.toHexString(softInputMode) + " windowFlags=#" + Integer.toHexString(windowFlags));
                }
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
                        return startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, controlFlags);
                    }
                    return null;
                }
                this.mCurFocusedWindow = windowToken;
                this.mCurFocusedWindowClient = cs;
                boolean zIsLayoutSizeAtLeast = (softInputMode & 240) != 16 ? this.mRes.getConfiguration().isLayoutSizeAtLeast(3) : true;
                boolean isTextEditor = (controlFlags & 2) != 0;
                boolean didStart = false;
                switch (softInputMode & 15) {
                    case 0:
                        if (isTextEditor && zIsLayoutSizeAtLeast) {
                            if (isTextEditor && zIsLayoutSizeAtLeast && (softInputMode & 256) != 0) {
                                if (DEBUG) {
                                    Slog.v(TAG, "Unspecified window will show input");
                                }
                                if (attribute != null) {
                                    res = startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, controlFlags);
                                    didStart = true;
                                }
                                showCurrentInputLocked(1, null);
                            }
                        } else if (WindowManager.LayoutParams.mayUseInputMethod(windowFlags)) {
                            if (DEBUG) {
                                Slog.v(TAG, "Unspecified window will hide input");
                            }
                            hideCurrentInputLocked(2, null);
                        }
                        break;
                    case 2:
                        if ((softInputMode & 256) != 0) {
                            if (DEBUG) {
                                Slog.v(TAG, "Window asks to hide input going forward");
                            }
                            hideCurrentInputLocked(0, null);
                        }
                        break;
                    case 3:
                        if (DEBUG) {
                            Slog.v(TAG, "Window asks to hide input");
                        }
                        hideCurrentInputLocked(0, null);
                        break;
                    case 4:
                        if ((softInputMode & 256) != 0) {
                            if (DEBUG) {
                                Slog.v(TAG, "Window asks to show input going forward");
                            }
                            if (attribute != null) {
                                res = startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, controlFlags);
                                didStart = true;
                            }
                            showCurrentInputLocked(1, null);
                        }
                        break;
                    case 5:
                        if (DEBUG) {
                            Slog.v(TAG, "Window asks to always show input");
                        }
                        if (attribute != null) {
                            res = startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, controlFlags);
                            didStart = true;
                        }
                        showCurrentInputLocked(1, null);
                        break;
                }
                if (!didStart && attribute != null) {
                    res = startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, controlFlags);
                }
                return res;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void showInputMethodPickerFromClient(IInputMethodClient client, int auxiliarySubtypeMode) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                Slog.w(TAG, "Ignoring showInputMethodPickerFromClient of uid " + Binder.getCallingUid() + ": " + client);
            }
            this.mHandler.sendMessage(this.mCaller.obtainMessageI(1, auxiliarySubtypeMode));
        }
    }

    public void setInputMethod(IBinder token, String id) {
        if (!calledFromValidUser()) {
            return;
        }
        setInputMethodWithSubtypeId(token, id, -1);
    }

    public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (subtype != null) {
                setInputMethodWithSubtypeIdLocked(token, id, InputMethodUtils.getSubtypeIdFromHashCode(this.mMethodMap.get(id), subtype.hashCode()));
            } else {
                setInputMethod(token, id);
            }
        }
    }

    public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient client, String inputMethodId) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(2, inputMethodId));
        }
    }

    public boolean switchToLastInputMethod(IBinder token) {
        List<InputMethodInfo> enabled;
        InputMethodSubtype keyboardSubtype;
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
            InputMethodInfo inputMethodInfo = lastIme != null ? this.mMethodMap.get(lastIme.first) : null;
            String targetLastImiId = null;
            int subtypeId = -1;
            if (lastIme != null && inputMethodInfo != null) {
                boolean imiIdIsSame = inputMethodInfo.getId().equals(this.mCurMethodId);
                int lastSubtypeHash = Integer.parseInt((String) lastIme.second);
                int currentSubtypeHash = this.mCurrentSubtype == null ? -1 : this.mCurrentSubtype.hashCode();
                if (!imiIdIsSame || lastSubtypeHash != currentSubtypeHash) {
                    targetLastImiId = (String) lastIme.first;
                    subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(inputMethodInfo, lastSubtypeHash);
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
            if (TextUtils.isEmpty(targetLastImiId)) {
                return false;
            }
            if (DEBUG) {
                Slog.d(TAG, "Switch to: " + inputMethodInfo.getId() + ", " + ((String) lastIme.second) + ", from: " + this.mCurMethodId + ", " + subtypeId);
            }
            setInputMethodWithSubtypeIdLocked(token, targetLastImiId, subtypeId);
            return true;
        }
    }

    public boolean switchToNextInputMethod(IBinder token, boolean onlyCurrentIme) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (!calledWithValidToken(token)) {
                int uid = Binder.getCallingUid();
                Slog.e(TAG, "Ignoring switchToNextInputMethod due to an invalid token. uid:" + uid + " token:" + token);
                return false;
            }
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(onlyCurrentIme, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, true);
            if (nextSubtype == null) {
                return false;
            }
            setInputMethodWithSubtypeIdLocked(token, nextSubtype.mImi.getId(), nextSubtype.mSubtypeId);
            return true;
        }
    }

    public boolean shouldOfferSwitchingToNextInputMethod(IBinder token) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (!calledWithValidToken(token)) {
                int uid = Binder.getCallingUid();
                Slog.e(TAG, "Ignoring shouldOfferSwitchingToNextInputMethod due to an invalid token. uid:" + uid + " token:" + token);
                return false;
            }
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(false, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, true);
            return nextSubtype != null;
        }
    }

    public InputMethodSubtype getLastInputMethodSubtype() {
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mMethodMap) {
            Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
            if (lastIme == null || TextUtils.isEmpty((CharSequence) lastIme.first) || TextUtils.isEmpty((CharSequence) lastIme.second)) {
                return null;
            }
            InputMethodInfo lastImi = this.mMethodMap.get(lastIme.first);
            if (lastImi == null) {
                return null;
            }
            try {
                int lastSubtypeHash = Integer.parseInt((String) lastIme.second);
                int lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(lastImi, lastSubtypeHash);
                if (lastSubtypeId < 0 || lastSubtypeId >= lastImi.getSubtypeCount()) {
                    return null;
                }
                return lastImi.getSubtypeAt(lastSubtypeId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public void setAdditionalInputMethodSubtypes(String imiId, InputMethodSubtype[] subtypes) {
        if (!calledFromValidUser() || TextUtils.isEmpty(imiId) || subtypes == null) {
            return;
        }
        synchronized (this.mMethodMap) {
            InputMethodInfo imi = this.mMethodMap.get(imiId);
            if (imi == null) {
                return;
            }
            try {
                String[] packageInfos = this.mIPackageManager.getPackagesForUid(Binder.getCallingUid());
                if (packageInfos != null) {
                    for (String str : packageInfos) {
                        if (str.equals(imi.getPackageName())) {
                            this.mFileManager.addInputMethodSubtypes(imi, subtypes);
                            long ident = Binder.clearCallingIdentity();
                            try {
                                buildInputMethodListLocked(false);
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

    public int getInputMethodWindowVisibleHeight() {
        return this.mWindowManagerInternal.getInputMethodWindowVisibleHeight();
    }

    public void clearLastInputMethodWindowForTransition(IBinder token) {
        if (!calledFromValidUser()) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (!calledWithValidToken(token)) {
                    int uid = Binder.getCallingUid();
                    Slog.e(TAG, "Ignoring clearLastInputMethodWindowForTransition due to an invalid token. uid:" + uid + " token:" + token);
                } else {
                    this.mWindowManagerInternal.clearLastInputMethodWindowForTransition();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void notifyUserAction(int sequenceNumber) {
        if (DEBUG) {
            Slog.d(TAG, "Got the notification of a user action. sequenceNumber:" + sequenceNumber);
        }
        synchronized (this.mMethodMap) {
            if (this.mCurUserActionNotificationSequenceNumber != sequenceNumber) {
                if (DEBUG) {
                    Slog.d(TAG, "Ignoring the user action notification due to the sequence number mismatch. expected:" + this.mCurUserActionNotificationSequenceNumber + " actual: " + sequenceNumber);
                }
            } else {
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
        if (!calledFromValidUser()) {
            return;
        }
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

    public void showMySoftInput(IBinder token, int flags) {
        if (!calledFromValidUser()) {
            return;
        }
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

    void setEnabledSessionInMainThread(SessionState session) {
        if (this.mEnabledSession == session) {
            return;
        }
        if (this.mEnabledSession != null && this.mEnabledSession.session != null) {
            try {
                if (DEBUG) {
                    Slog.v(TAG, "Disabling: " + this.mEnabledSession);
                }
                this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, false);
            } catch (RemoteException e) {
            }
        }
        this.mEnabledSession = session;
        if (this.mEnabledSession == null || this.mEnabledSession.session == null) {
            return;
        }
        try {
            if (DEBUG) {
                Slog.v(TAG, "Enabling: " + this.mEnabledSession);
            }
            this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, true);
        } catch (RemoteException e2) {
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        boolean showAuxSubtypes;
        switch (msg.what) {
            case 1:
                switch (msg.arg1) {
                    case 0:
                        showAuxSubtypes = this.mInputShown;
                        break;
                    case 1:
                        showAuxSubtypes = true;
                        break;
                    case 2:
                        showAuxSubtypes = false;
                        break;
                    default:
                        Slog.e(TAG, "Unknown subtype picker mode = " + msg.arg1);
                        return false;
                }
                showInputMethodMenu(showAuxSubtypes);
                return true;
            case 2:
                showInputMethodAndSubtypeEnabler((String) msg.obj);
                return true;
            case 3:
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
                SomeArgs args = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args.arg1).bindInput((InputBinding) args.arg2);
                    break;
                } catch (RemoteException e2) {
                }
                args.recycle();
                return true;
            case MSG_SHOW_SOFT_INPUT:
                SomeArgs args2 = (SomeArgs) msg.obj;
                try {
                    if (DEBUG) {
                        Slog.v(TAG, "Calling " + args2.arg1 + ".showSoftInput(" + msg.arg1 + ", " + args2.arg2 + ")");
                    }
                    ((IInputMethod) args2.arg1).showSoftInput(msg.arg1, (ResultReceiver) args2.arg2);
                    break;
                } catch (RemoteException e3) {
                }
                args2.recycle();
                return true;
            case MSG_HIDE_SOFT_INPUT:
                SomeArgs args3 = (SomeArgs) msg.obj;
                try {
                    if (DEBUG) {
                        Slog.v(TAG, "Calling " + args3.arg1 + ".hideSoftInput(0, " + args3.arg2 + ")");
                    }
                    ((IInputMethod) args3.arg1).hideSoftInput(0, (ResultReceiver) args3.arg2);
                    break;
                } catch (RemoteException e4) {
                }
                args3.recycle();
                return true;
            case MSG_HIDE_CURRENT_INPUT_METHOD:
                synchronized (this.mMethodMap) {
                    hideCurrentInputLocked(0, null);
                }
                return true;
            case MSG_ATTACH_TOKEN:
                SomeArgs args4 = (SomeArgs) msg.obj;
                try {
                    if (DEBUG) {
                        Slog.v(TAG, "Sending attach of token: " + args4.arg2);
                    }
                    ((IInputMethod) args4.arg1).attachToken((IBinder) args4.arg2);
                    break;
                } catch (RemoteException e5) {
                }
                args4.recycle();
                return true;
            case MSG_CREATE_SESSION:
                SomeArgs args5 = (SomeArgs) msg.obj;
                IInputMethod method = (IInputMethod) args5.arg1;
                InputChannel channel = (InputChannel) args5.arg2;
                try {
                    method.createSession(channel, (IInputSessionCallback) args5.arg3);
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
                args5.recycle();
                return true;
            case MSG_START_INPUT:
                int missingMethods = msg.arg1;
                SomeArgs args6 = (SomeArgs) msg.obj;
                try {
                    SessionState session = (SessionState) args6.arg1;
                    setEnabledSessionInMainThread(session);
                    session.method.startInput((IInputContext) args6.arg2, missingMethods, (EditorInfo) args6.arg3);
                    break;
                } catch (RemoteException e7) {
                }
                args6.recycle();
                return true;
            case MSG_RESTART_INPUT:
                int missingMethods2 = msg.arg1;
                SomeArgs args7 = (SomeArgs) msg.obj;
                try {
                    SessionState session2 = (SessionState) args7.arg1;
                    setEnabledSessionInMainThread(session2);
                    session2.method.restartInput((IInputContext) args7.arg2, missingMethods2, (EditorInfo) args7.arg3);
                    break;
                } catch (RemoteException e8) {
                }
                args7.recycle();
                return true;
            case MSG_UNBIND_CLIENT:
                try {
                    ((IInputMethodClient) msg.obj).onUnbindMethod(msg.arg1, msg.arg2);
                    return true;
                } catch (RemoteException e9) {
                    return true;
                }
            case 3010:
                SomeArgs args8 = (SomeArgs) msg.obj;
                IInputMethodClient client = (IInputMethodClient) args8.arg1;
                InputBindResult res = (InputBindResult) args8.arg2;
                try {
                    try {
                        client.onBindMethod(res);
                    } catch (RemoteException e10) {
                        Slog.w(TAG, "Client died receiving input method " + args8.arg2);
                        if (res.channel != null && Binder.isProxy(client)) {
                            res.channel.dispose();
                        }
                    }
                    args8.recycle();
                    return true;
                } finally {
                    if (res.channel != null && Binder.isProxy(client)) {
                        res.channel.dispose();
                    }
                }
            case MSG_SET_ACTIVE:
                try {
                    ((ClientState) msg.obj).client.setActive(msg.arg1 != 0);
                    return true;
                } catch (RemoteException e11) {
                    Slog.w(TAG, "Got RemoteException sending setActive(false) notification to pid " + ((ClientState) msg.obj).pid + " uid " + ((ClientState) msg.obj).uid);
                    return true;
                }
            case MSG_SET_INTERACTIVE:
                handleSetInteractive(msg.arg1 != 0);
                return true;
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
            case 3050:
                handleSwitchInputMethod(msg.arg1 != 0);
                return true;
            case MSG_HARD_KEYBOARD_SWITCH_CHANGED:
                this.mHardKeyboardListener.handleHardKeyboardStatusChange(msg.arg1 == 1);
                return true;
            default:
                return false;
        }
    }

    private void handleSetInteractive(boolean interactive) {
        synchronized (this.mMethodMap) {
            this.mIsInteractive = interactive;
            updateSystemUiLocked(this.mCurToken, interactive ? this.mImeWindowVis : 0, this.mBackDisposition);
            if (this.mCurClient != null && this.mCurClient.client != null) {
                executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_SET_ACTIVE, this.mIsInteractive ? 1 : 0, this.mCurClient));
            }
        }
    }

    private void handleSwitchInputMethod(boolean forwardDirection) {
        synchronized (this.mMethodMap) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(false, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, forwardDirection);
            if (nextSubtype == null) {
                return;
            }
            setInputMethodLocked(nextSubtype.mImi.getId(), nextSubtype.mSubtypeId);
            InputMethodInfo newInputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
            if (newInputMethodInfo == null) {
                return;
            }
            CharSequence toastText = InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, newInputMethodInfo, this.mCurrentSubtype);
            if (!TextUtils.isEmpty(toastText)) {
                if (this.mSubtypeSwitchedByShortCutToast == null) {
                    this.mSubtypeSwitchedByShortCutToast = Toast.makeText(this.mContext, toastText, 0);
                } else {
                    this.mSubtypeSwitchedByShortCutToast.setText(toastText);
                }
                this.mSubtypeSwitchedByShortCutToast.show();
            }
        }
    }

    private boolean chooseNewDefaultIMELocked() {
        InputMethodInfo imi = InputMethodUtils.getMostApplicableDefaultIME(this.mSettings.getEnabledInputMethodListLocked());
        if (imi != null) {
            if (DEBUG) {
                Slog.d(TAG, "New default IME was selected: " + imi.getId());
            }
            resetSelectedInputMethodAndSubtypeLocked(imi.getId());
            return true;
        }
        return false;
    }

    void buildInputMethodListLocked(boolean resetDefaultEnabledIme) {
        if (DEBUG) {
            Slog.d(TAG, "--- re-buildInputMethodList reset = " + resetDefaultEnabledIme + " \n ------ caller=" + Debug.getCallers(10));
        }
        this.mMethodList.clear();
        this.mMethodMap.clear();
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.view.InputMethod"), 32896, this.mSettings.getCurrentUserId());
        HashMap<String, List<InputMethodSubtype>> additionalSubtypes = this.mFileManager.getAllAdditionalInputMethodSubtypes();
        for (int i = 0; i < services.size(); i++) {
            ResolveInfo ri = services.get(i);
            ServiceInfo si = ri.serviceInfo;
            ComponentName compName = new ComponentName(si.packageName, si.name);
            if (!"android.permission.BIND_INPUT_METHOD".equals(si.permission)) {
                Slog.w(TAG, "Skipping input method " + compName + ": it does not require the permission android.permission.BIND_INPUT_METHOD");
            } else {
                if (DEBUG) {
                    Slog.d(TAG, "Checking " + compName);
                }
                try {
                    InputMethodInfo p = new InputMethodInfo(this.mContext, ri, additionalSubtypes);
                    this.mMethodList.add(p);
                    String id = p.getId();
                    this.mMethodMap.put(id, p);
                    if (DEBUG) {
                        Slog.d(TAG, "Found an input method " + p);
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.w(TAG, "Unable to load input method " + compName, e);
                }
            }
        }
        if (!resetDefaultEnabledIme) {
            boolean enabledImeFound = false;
            List<InputMethodInfo> enabledImes = this.mSettings.getEnabledInputMethodListLocked();
            int N = enabledImes.size();
            int i2 = 0;
            while (true) {
                if (i2 >= N) {
                    break;
                }
                if (!this.mMethodList.contains(enabledImes.get(i2))) {
                    i2++;
                } else {
                    enabledImeFound = true;
                    break;
                }
            }
            if (!enabledImeFound) {
                Slog.i(TAG, "All the enabled IMEs are gone. Reset default enabled IMEs.");
                resetDefaultEnabledIme = true;
                resetSelectedInputMethodAndSubtypeLocked("");
            }
        }
        if (resetDefaultEnabledIme) {
            ArrayList<InputMethodInfo> defaultEnabledIme = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mSystemReady, this.mMethodList);
            int N2 = defaultEnabledIme.size();
            for (int i3 = 0; i3 < N2; i3++) {
                InputMethodInfo imi = defaultEnabledIme.get(i3);
                if (DEBUG) {
                    Slog.d(TAG, "--- enable ime = " + imi);
                }
                setInputMethodEnabledLocked(imi.getId(), true);
            }
            if (TextUtils.isEmpty(this.mSettings.getEnabledInputMethodsStr())) {
                int i4 = 0;
                while (true) {
                    if (i4 >= this.mMethodList.size()) {
                        break;
                    }
                    InputMethodInfo imi2 = this.mMethodList.get(i4);
                    if (!InputMethodUtils.isSystemIme(imi2)) {
                        i4++;
                    } else {
                        setInputMethodEnabledLocked(imi2.getId(), true);
                        break;
                    }
                }
            }
        }
        String defaultImiId = this.mSettings.getSelectedInputMethod();
        if (!TextUtils.isEmpty(defaultImiId)) {
            if (!this.mMethodMap.containsKey(defaultImiId)) {
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

    private void showInputMethodAndSubtypeEnabler(String inputMethodId) {
        int userId;
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
        intent.setFlags(337641472);
        if (!TextUtils.isEmpty(inputMethodId)) {
            intent.putExtra("input_method_id", inputMethodId);
        }
        synchronized (this.mMethodMap) {
            userId = this.mSettings.getCurrentUserId();
        }
        this.mContext.startActivityAsUser(intent, null, UserHandle.of(userId));
    }

    private void showConfigureInputMethods() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        hideInputMethodMenu();
        Intent intent = new Intent("android.settings.INPUT_METHOD_SETTINGS");
        intent.setFlags(337641472);
        this.mContext.startActivityAsUser(intent, null, UserHandle.CURRENT);
    }

    private boolean isScreenLocked() {
        if (this.mKeyguardManager == null || !this.mKeyguardManager.isKeyguardLocked()) {
            return false;
        }
        return this.mKeyguardManager.isKeyguardSecure();
    }

    private void showInputMethodMenu(boolean showAuxSubtypes) {
        InputMethodSubtype currentSubtype;
        if (DEBUG) {
            Slog.v(TAG, "Show switching menu. showAuxSubtypes=" + showAuxSubtypes);
        }
        Context context = this.mContext;
        boolean isScreenLocked = isScreenLocked();
        String lastInputMethodId = this.mSettings.getSelectedInputMethod();
        int lastInputMethodSubtypeId = this.mSettings.getSelectedInputMethodSubtypeId(lastInputMethodId);
        if (DEBUG) {
            Slog.v(TAG, "Current IME: " + lastInputMethodId);
        }
        synchronized (this.mMethodMap) {
            HashMap<InputMethodInfo, List<InputMethodSubtype>> immis = this.mSettings.getExplicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked(this.mContext);
            if (immis == null || immis.size() == 0) {
                return;
            }
            hideInputMethodMenuLocked();
            List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList = this.mSwitchingController.getSortedInputMethodAndSubtypeListLocked(showAuxSubtypes, isScreenLocked);
            if (lastInputMethodSubtypeId == -1 && (currentSubtype = getCurrentInputMethodSubtypeLocked()) != null) {
                InputMethodInfo currentImi = this.mMethodMap.get(this.mCurMethodId);
                lastInputMethodSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(currentImi, currentSubtype.hashCode());
            }
            int N = imList.size();
            this.mIms = new InputMethodInfo[N];
            this.mSubtypeIds = new int[N];
            int checkedItem = 0;
            boolean mNeedfindInputMethod = true;
            for (int i = 0; i < N; i++) {
                InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = imList.get(i);
                this.mIms[i] = item.mImi;
                this.mSubtypeIds[i] = item.mSubtypeId;
                if (this.mIms[i].getId().equals(lastInputMethodId)) {
                    if (mNeedfindInputMethod) {
                        checkedItem = i;
                    }
                    int subtypeId = this.mSubtypeIds[i];
                    if (subtypeId == -1 || ((lastInputMethodSubtypeId == -1 && subtypeId == 0) || subtypeId == lastInputMethodSubtypeId)) {
                        checkedItem = i;
                        mNeedfindInputMethod = false;
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
            LayoutInflater inflater = (LayoutInflater) dialogContext.getSystemService(LayoutInflater.class);
            View tv = inflater.inflate(R.layout.date_picker_legacy_holo, (ViewGroup) null);
            this.mDialogBuilder.setCustomTitle(tv);
            this.mSwitchingDialogTitleView = tv;
            this.mSwitchingDialogTitleView.findViewById(R.id.input_method_nav_back).setVisibility(this.mWindowManagerInternal.isHardKeyboardAvailable() ? 0 : 8);
            Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(R.id.input_method_nav_buttons);
            hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
            hardKeySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    InputMethodManagerService.this.mSettings.setShowImeWithHardKeyboard(isChecked);
                    InputMethodManagerService.this.hideInputMethodMenu();
                }
            });
            final ImeSubtypeListAdapter adapter = new ImeSubtypeListAdapter(dialogContext, R.layout.date_picker_material, imList, checkedItem);
            DialogInterface.OnClickListener choiceListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    synchronized (InputMethodManagerService.this.mMethodMap) {
                        if (InputMethodManagerService.this.mIms == null || InputMethodManagerService.this.mIms.length <= which || InputMethodManagerService.this.mSubtypeIds == null || InputMethodManagerService.this.mSubtypeIds.length <= which) {
                            return;
                        }
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
            };
            this.mDialogBuilder.setSingleChoiceItems(adapter, checkedItem, choiceListener);
            this.mSwitchingDialog = this.mDialogBuilder.create();
            this.mSwitchingDialog.setCanceledOnTouchOutside(true);
            this.mSwitchingDialog.getWindow().setType(2012);
            this.mSwitchingDialog.getWindow().getAttributes().privateFlags |= 16;
            this.mSwitchingDialog.getWindow().getAttributes().setTitle("Select input method");
            updateSystemUi(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
            this.mSwitchingDialog.show();
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
            this.mInflater = (LayoutInflater) context.getSystemService(LayoutInflater.class);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView : this.mInflater.inflate(this.mTextViewResourceId, (ViewGroup) null);
            if (position < 0 || position >= this.mItemsList.size()) {
                return view;
            }
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
            RadioButton radioButton = (RadioButton) view.findViewById(R.id.input_method_nav_center_group);
            radioButton.setChecked(position == this.mCheckedItem);
            return view;
        }
    }

    void hideInputMethodMenu() {
        synchronized (this.mMethodMap) {
            hideInputMethodMenuLocked();
        }
    }

    void hideInputMethodMenuLocked() {
        if (DEBUG) {
            Slog.v(TAG, "Hide switching menu");
        }
        if (this.mSwitchingDialog != null) {
            this.mSwitchingDialog.dismiss();
            this.mSwitchingDialog = null;
        }
        updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
        this.mDialogBuilder = null;
        this.mIms = null;
    }

    public boolean setInputMethodEnabled(String id, boolean enabled) {
        boolean inputMethodEnabledLocked;
        if (!calledFromValidUser()) {
            return false;
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
            this.mSettings.appendAndPutEnabledInputMethodLocked(id, false);
            return false;
        }
        StringBuilder builder = new StringBuilder();
        if (!this.mSettings.buildAndPutEnabledInputMethodsStrRemovingIdLocked(builder, enabledInputMethodsList, id)) {
            return false;
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
        if (DEBUG) {
            Slog.d(TAG, "Bump mCurUserActionNotificationSequenceNumber:" + this.mCurUserActionNotificationSequenceNumber);
        }
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
        if (setSubtypeOnly) {
            return;
        }
        this.mSettings.putSelectedInputMethod(imi != null ? imi.getId() : "");
    }

    private void resetSelectedInputMethodAndSubtypeLocked(String newDefaultIme) {
        String subtypeHashCode;
        InputMethodInfo imi = this.mMethodMap.get(newDefaultIme);
        int lastSubtypeId = -1;
        if (imi != null && !TextUtils.isEmpty(newDefaultIme) && (subtypeHashCode = this.mSettings.getLastSubtypeForInputMethodLocked(newDefaultIme)) != null) {
            try {
                lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, Integer.parseInt(subtypeHashCode));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "HashCode for subtype looks broken: " + subtypeHashCode, e);
            }
        }
        setSelectedInputMethodAndSubtypeLocked(imi, lastSubtypeId, false);
    }

    private Pair<InputMethodInfo, InputMethodSubtype> findLastResortApplicableShortcutInputMethodAndSubtypeLocked(String mode) {
        List<InputMethodInfo> imis = this.mSettings.getEnabledInputMethodListLocked();
        InputMethodInfo mostApplicableIMI = null;
        InputMethodSubtype mostApplicableSubtype = null;
        boolean foundInSystemIME = false;
        Iterator imi$iterator = imis.iterator();
        while (true) {
            if (!imi$iterator.hasNext()) {
                break;
            }
            InputMethodInfo imi = (InputMethodInfo) imi$iterator.next();
            String imiId = imi.getId();
            if (!foundInSystemIME || imiId.equals(this.mCurMethodId)) {
                List<InputMethodSubtype> enabledSubtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                InputMethodSubtype subtype = this.mCurrentSubtype != null ? InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledSubtypes, mode, this.mCurrentSubtype.getLocale(), false) : null;
                if (subtype == null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledSubtypes, mode, (String) null, true);
                }
                ArrayList<InputMethodSubtype> overridingImplicitlyEnabledSubtypes = InputMethodUtils.getOverridingImplicitlyEnabledSubtypes(imi, mode);
                ArrayList<InputMethodSubtype> subtypesForSearch = overridingImplicitlyEnabledSubtypes.isEmpty() ? InputMethodUtils.getSubtypes(imi) : overridingImplicitlyEnabledSubtypes;
                if (subtype == null && this.mCurrentSubtype != null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, subtypesForSearch, mode, this.mCurrentSubtype.getLocale(), false);
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
        if (DEBUG && mostApplicableIMI != null) {
            Slog.w(TAG, "Most applicable shortcut input method was:" + mostApplicableIMI.getId());
            if (mostApplicableSubtype != null) {
                Slog.w(TAG, "Most applicable shortcut input method subtype was:," + mostApplicableSubtype.getMode() + "," + mostApplicableSubtype.getLocale());
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

    public List getShortcutInputMethodsAndSubtypes() {
        synchronized (this.mMethodMap) {
            ArrayList<Object> ret = new ArrayList<>();
            if (this.mShortcutInputMethodsAndSubtypes.size() == 0) {
                Pair<InputMethodInfo, InputMethodSubtype> info = findLastResortApplicableShortcutInputMethodAndSubtypeLocked("voice");
                if (info != null) {
                    ret.add(info.first);
                    ret.add(info.second);
                }
                return ret;
            }
            for (InputMethodInfo imi : this.mShortcutInputMethodsAndSubtypes.keySet()) {
                ret.add(imi);
                for (InputMethodSubtype subtype : this.mShortcutInputMethodsAndSubtypes.get(imi)) {
                    ret.add(subtype);
                }
            }
            return ret;
        }
    }

    public boolean setCurrentInputMethodSubtype(InputMethodSubtype subtype) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (subtype != null) {
                if (this.mCurMethodId != null) {
                    InputMethodInfo imi = this.mMethodMap.get(this.mCurMethodId);
                    int subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, subtype.hashCode());
                    if (subtypeId != -1) {
                        setInputMethodLocked(this.mCurMethodId, subtypeId);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static class InputMethodFileManager {
        private static final String ADDITIONAL_SUBTYPES_FILE_NAME = "subtypes.xml";
        private static final String ATTR_ICON = "icon";
        private static final String ATTR_ID = "id";
        private static final String ATTR_IME_SUBTYPE_EXTRA_VALUE = "imeSubtypeExtraValue";
        private static final String ATTR_IME_SUBTYPE_ID = "subtypeId";
        private static final String ATTR_IME_SUBTYPE_LANGUAGE_TAG = "languageTag";
        private static final String ATTR_IME_SUBTYPE_LOCALE = "imeSubtypeLocale";
        private static final String ATTR_IME_SUBTYPE_MODE = "imeSubtypeMode";
        private static final String ATTR_IS_ASCII_CAPABLE = "isAsciiCapable";
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
            File systemDir;
            if (methodMap == null) {
                throw new NullPointerException("methodMap is null");
            }
            this.mMethodMap = methodMap;
            if (userId == 0) {
                systemDir = new File(Environment.getDataDirectory(), SYSTEM_PATH);
            } else {
                systemDir = Environment.getUserSystemDirectory(userId);
            }
            File inputMethodDir = new File(systemDir, INPUT_METHOD_PATH);
            if (!inputMethodDir.exists() && !inputMethodDir.mkdirs()) {
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
                    if (subtypes.contains(subtype)) {
                        Slog.w(InputMethodManagerService.TAG, "Duplicated subtype definition found: " + subtype.getLocale() + ", " + subtype.getMode());
                    } else {
                        subtypes.add(subtype);
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
            boolean isSetMethodMap = methodMap != null && methodMap.size() > 0;
            FileOutputStream fos = null;
            try {
                fos = subtypesFile.startWrite();
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
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
                            if (subtype.hasSubtypeId()) {
                                fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_ID, String.valueOf(subtype.getSubtypeId()));
                            }
                            fastXmlSerializer.attribute(null, ATTR_ICON, String.valueOf(subtype.getIconResId()));
                            fastXmlSerializer.attribute(null, ATTR_LABEL, String.valueOf(subtype.getNameResId()));
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_LOCALE, subtype.getLocale());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_LANGUAGE_TAG, subtype.getLanguageTag());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_MODE, subtype.getMode());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_EXTRA_VALUE, subtype.getExtraValue());
                            fastXmlSerializer.attribute(null, ATTR_IS_AUXILIARY, String.valueOf(subtype.isAuxiliary() ? 1 : 0));
                            fastXmlSerializer.attribute(null, ATTR_IS_ASCII_CAPABLE, String.valueOf(subtype.isAsciiCapable() ? 1 : 0));
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
                if (fos == null) {
                    return;
                }
                subtypesFile.failWrite(fos);
            }
        }

        private static void readAdditionalInputMethodSubtypes(HashMap<String, List<InputMethodSubtype>> allSubtypes, AtomicFile subtypesFile) {
            Throwable th;
            int type;
            if (allSubtypes == null || subtypesFile == null) {
                return;
            }
            allSubtypes.clear();
            Throwable th2 = null;
            FileInputStream fileInputStream = null;
            try {
                try {
                    FileInputStream fis = subtypesFile.openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(fis, StandardCharsets.UTF_8.name());
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
                                    int icon = Integer.parseInt(parser.getAttributeValue(null, ATTR_ICON));
                                    int label = Integer.parseInt(parser.getAttributeValue(null, ATTR_LABEL));
                                    String imeSubtypeLocale = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_LOCALE);
                                    String languageTag = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_LANGUAGE_TAG);
                                    String imeSubtypeMode = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_MODE);
                                    String imeSubtypeExtraValue = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_EXTRA_VALUE);
                                    boolean isAuxiliary = "1".equals(String.valueOf(parser.getAttributeValue(null, ATTR_IS_AUXILIARY)));
                                    boolean isAsciiCapable = "1".equals(String.valueOf(parser.getAttributeValue(null, ATTR_IS_ASCII_CAPABLE)));
                                    InputMethodSubtype.InputMethodSubtypeBuilder builder = new InputMethodSubtype.InputMethodSubtypeBuilder().setSubtypeNameResId(label).setSubtypeIconResId(icon).setSubtypeLocale(imeSubtypeLocale).setLanguageTag(languageTag).setSubtypeMode(imeSubtypeMode).setSubtypeExtraValue(imeSubtypeExtraValue).setIsAuxiliary(isAuxiliary).setIsAsciiCapable(isAsciiCapable);
                                    String subtypeIdString = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_ID);
                                    if (subtypeIdString != null) {
                                        builder.setSubtypeId(Integer.parseInt(subtypeIdString));
                                    }
                                    tempSubtypesArray.add(builder.build());
                                }
                            }
                        }
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        throw th2;
                    }
                } catch (IOException | NumberFormatException | XmlPullParserException e) {
                    Slog.w(InputMethodManagerService.TAG, "Error reading subtypes", e);
                }
            } catch (Throwable th4) {
                try {
                    throw th4;
                } catch (Throwable th5) {
                    th2 = th4;
                    th = th5;
                    if (0 != 0) {
                        try {
                            fileInputStream.close();
                        } catch (Throwable th6) {
                            if (th2 == null) {
                                th2 = th6;
                            } else if (th2 != th6) {
                                th2.addSuppressed(th6);
                            }
                        }
                    }
                    if (th2 != null) {
                        throw th;
                    }
                    throw th2;
                }
            }
        }
    }

    private static final class LocalServiceImpl implements InputMethodManagerInternal {
        private final Handler mHandler;

        LocalServiceImpl(Handler handler) {
            this.mHandler = handler;
        }

        public void setInteractive(boolean interactive) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(InputMethodManagerService.MSG_SET_INTERACTIVE, interactive ? 1 : 0, 0));
        }

        public void switchInputMethod(boolean forwardDirection) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3050, forwardDirection ? 1 : 0, 0));
        }

        public void hideCurrentInputMethod() {
            this.mHandler.removeMessages(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD);
            this.mHandler.sendEmptyMessage(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD);
        }
    }

    private static String imeWindowStatusToString(int imeWindowVis) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if ((imeWindowVis & 1) != 0) {
            sb.append("Active");
            first = false;
        }
        if ((imeWindowVis & 2) != 0) {
            if (!first) {
                sb.append("|");
            }
            sb.append("Visible");
        }
        return sb.toString();
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ClientState client;
        ClientState focusedWindowClient;
        IInputMethod method;
        String option;
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump InputMethodManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        this.mArgs = args;
        this.mNextArg = 1;
        if (args != null && args.length > 0 && (option = args[0]) != null && option.length() > 0 && option.charAt(0) == '-') {
            handleDebugCmd(fd, pw, option);
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
            focusedWindowClient = this.mCurFocusedWindowClient;
            p.println("  mCurFocusedWindowClient=" + focusedWindowClient);
            p.println("  mCurId=" + this.mCurId + " mHaveConnect=" + this.mHaveConnection + " mBoundToMethod=" + this.mBoundToMethod);
            p.println("  mCurToken=" + this.mCurToken);
            p.println("  mCurIntent=" + this.mCurIntent);
            method = this.mCurMethod;
            p.println("  mCurMethod=" + this.mCurMethod);
            p.println("  mEnabledSession=" + this.mEnabledSession);
            p.println("  mImeWindowVis=" + imeWindowStatusToString(this.mImeWindowVis));
            p.println("  mShowRequested=" + this.mShowRequested + " mShowExplicitlyRequested=" + this.mShowExplicitlyRequested + " mShowForced=" + this.mShowForced + " mInputShown=" + this.mInputShown);
            p.println("  mCurUserActionNotificationSequenceNumber=" + this.mCurUserActionNotificationSequenceNumber);
            p.println("  mSystemReady=" + this.mSystemReady + " mInteractive=" + this.mIsInteractive);
            p.println("  mSettingsObserver=" + this.mSettingsObserver);
            p.println("  mSwitchingController:");
            this.mSwitchingController.dump(p);
            p.println("  mSettings:");
            this.mSettings.dumpLocked(p, "    ");
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
        if (focusedWindowClient != null && client != focusedWindowClient) {
            p.println(" ");
            p.println("Warning: Current input method client doesn't match the last focused. window.");
            p.println("Dumping input method client in the last focused window just in case.");
            p.println(" ");
            pw.flush();
            try {
                focusedWindowClient.client.asBinder().dump(fd, args);
            } catch (RemoteException e2) {
                p.println("Input method client in focused window dead: " + e2);
            }
        }
        p.println(" ");
        if (method == null) {
            p.println("No input method service.");
            return;
        }
        pw.flush();
        try {
            method.asBinder().dump(fd, args);
        } catch (RemoteException e3) {
            p.println("Input method service dead: " + e3);
        }
    }

    private void printUsage(PrintWriter pw) {
        pw.println("Input method manager service dump options:");
        pw.println("  [-d] [-h] [cmd] [option] ...");
        pw.println("  -d enable <zone>          enable the debug zone");
        pw.println("  -d disable <zone>         disable the debug zone");
        pw.println("       zone list:");
        pw.println("         0 : InputMethodManagerService");
        pw.println("         1 : InputMethodService");
        pw.println("         2 : InputMethodManager");
        pw.println("  -h                        print the dump usage");
    }

    private void handleDebugCmd(FileDescriptor fd, PrintWriter pw, String option) {
        if ("-d".equals(option)) {
            String action = nextArg();
            if ("enable".equals(action)) {
                runDebug(fd, pw, true);
                return;
            } else if ("disable".equals(action)) {
                runDebug(fd, pw, false);
                return;
            } else {
                printUsage(pw);
                return;
            }
        }
        if ("-h".equals(option)) {
            printUsage(pw);
        } else {
            pw.println("Unknown argument: " + option + "; use -h for help");
        }
    }

    private void runDebug(FileDescriptor fd, PrintWriter pw, boolean enable) {
        String[] args = new String[1];
        while (true) {
            String type = nextArg();
            if (type == null) {
                return;
            }
            if ("0".equals(type)) {
                DEBUG = enable;
            } else if ("1".equals(type)) {
                args[0] = enable ? "enable" : "disable";
                runInputMethodServiceDebug(fd, pw, args);
            } else if ("2".equals(type)) {
                args[0] = enable ? "enable" : "disable";
                runInputMethodManagerDebug(fd, pw, args);
            } else {
                printUsage(pw);
                return;
            }
        }
    }

    private void runInputMethodServiceDebug(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mCurMethod == null) {
            return;
        }
        try {
            this.mCurMethod.asBinder().dump(fd, args);
        } catch (RemoteException e) {
            pw.println("Input method client dead: " + e);
        }
    }

    private void runInputMethodManagerDebug(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mCurClient == null) {
            return;
        }
        try {
            this.mCurClient.client.asBinder().dump(fd, args);
        } catch (RemoteException e) {
            pw.println("Input method client dead: " + e);
        }
    }

    private String nextArg() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String[] strArr = this.mArgs;
        int i = this.mNextArg;
        this.mNextArg = i + 1;
        return strArr[i];
    }

    public void switchInputMethodFromWindowManager(boolean isForward) {
        if (DEBUG) {
            Slog.d(TAG, "switch input method from WindowManager: " + isForward);
        }
        synchronized (this.mMethodMap) {
            if (isForward) {
                this.index1++;
            } else {
                this.index1--;
            }
            if (this.index1 == 1 || this.index1 == -1) {
                this.index2 = this.index1;
                if (this.mTimer != null) {
                    this.mTimer.purge();
                }
                this.mTimer = new Timer();
                this.mTimer.schedule(new SwitchImeTask(this, null), 500L);
            }
        }
    }

    private class SwitchImeTask extends TimerTask {
        SwitchImeTask(InputMethodManagerService this$0, SwitchImeTask switchImeTask) {
            this();
        }

        private SwitchImeTask() {
        }

        @Override
        public void run() {
            int currentSubtypeId;
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (InputMethodManagerService.this.index2 == InputMethodManagerService.this.index1) {
                    List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> imList = InputMethodManagerService.this.mSwitchingController.getSortedInputMethodAndSubtypeListLocked(false, false);
                    InputMethodInfo currentMethod = InputMethodManagerService.this.mMethodMap.get(InputMethodManagerService.this.mCurMethodId);
                    if (imList.size() <= 1) {
                        if (InputMethodManagerService.DEBUG) {
                            Slog.w(InputMethodManagerService.TAG, "Only one IME within list, ignored");
                        }
                        return;
                    }
                    int listSize = imList.size();
                    if (InputMethodManagerService.this.mCurrentSubtype != null) {
                        currentSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(currentMethod, InputMethodManagerService.this.mCurrentSubtype.hashCode());
                    } else {
                        currentSubtypeId = -1;
                    }
                    if (InputMethodManagerService.DEBUG) {
                        Slog.d(InputMethodManagerService.TAG, "ImeSubtypeListItem size : " + listSize);
                    }
                    int i = 0;
                    while (true) {
                        if (i >= listSize) {
                            break;
                        }
                        InputMethodSubtypeSwitchingController.ImeSubtypeListItem isli = imList.get(i);
                        if (!isli.mImi.equals(currentMethod) || isli.mSubtypeId != currentSubtypeId) {
                            i++;
                        } else {
                            if (InputMethodManagerService.DEBUG) {
                                Slog.d(InputMethodManagerService.TAG, "index2: " + InputMethodManagerService.this.index2 + ",i: " + i + ",listSize: " + listSize);
                            }
                            InputMethodManagerService.this.index2 += i;
                            InputMethodManagerService.this.index2 %= listSize;
                            if (InputMethodManagerService.this.index2 < 0) {
                                InputMethodManagerService.this.index2 += listSize;
                            }
                            InputMethodSubtypeSwitchingController.ImeSubtypeListItem item = imList.get(InputMethodManagerService.this.index2);
                            if (InputMethodManagerService.DEBUG) {
                                Slog.d(InputMethodManagerService.TAG, "set input method in runnable! index2: " + InputMethodManagerService.this.index2 + ",item: " + item.mImi.getId());
                            }
                            InputMethodManagerService.this.setInputMethodLocked(item.mImi.getId(), item.mSubtypeId);
                        }
                    }
                    InputMethodManagerService.this.index2 = 0;
                    InputMethodManagerService.this.index1 = 0;
                } else {
                    InputMethodManagerService.this.index2 = InputMethodManagerService.this.index1;
                    Slog.d(InputMethodManagerService.TAG, "schedule switch task after 500ms! index2: " + InputMethodManagerService.this.index2);
                    InputMethodManagerService.this.mTimer.purge();
                    InputMethodManagerService.this.mTimer = new Timer();
                    InputMethodManagerService.this.mTimer.schedule(InputMethodManagerService.this.new SwitchImeTask(), 500L);
                }
            }
        }
    }

    public void sendCharacterToCurClient(int unicode) {
        if (this.mCurClient == null || this.mCurClient.client == null) {
            return;
        }
        try {
            this.mCurClient.client.sendCharacter(unicode);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
