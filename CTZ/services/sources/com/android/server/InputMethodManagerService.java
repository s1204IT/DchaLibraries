package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
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
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.text.TextUtils;
import android.text.style.SuggestionSpan;
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
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManagerInternal;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.IInputContentUriToken;
import com.android.internal.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InputBindResult;
import com.android.internal.view.InputMethodClient;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.backup.BackupManagerService;
import com.android.server.pm.DumpState;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wm.WindowManagerInternal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class InputMethodManagerService extends IInputMethodManager.Stub implements ServiceConnection, Handler.Callback {
    private static final String ACTION_SHOW_INPUT_METHOD_PICKER = "com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER";
    static final boolean DEBUG = false;
    private static final int IME_CONNECTION_BIND_FLAGS = 1082130437;
    private static final int IME_VISIBLE_BIND_FLAGS = 738197505;
    static final int MSG_ATTACH_TOKEN = 1040;
    static final int MSG_BIND_CLIENT = 3010;
    static final int MSG_BIND_INPUT = 1010;
    static final int MSG_CREATE_SESSION = 1050;
    static final int MSG_HARD_KEYBOARD_SWITCH_CHANGED = 4000;
    static final int MSG_HIDE_CURRENT_INPUT_METHOD = 1035;
    static final int MSG_HIDE_SOFT_INPUT = 1030;
    static final int MSG_REPORT_FULLSCREEN_MODE = 3045;
    static final int MSG_SET_ACTIVE = 3020;
    static final int MSG_SET_INTERACTIVE = 3030;
    static final int MSG_SET_USER_ACTION_NOTIFICATION_SEQUENCE_NUMBER = 3040;
    static final int MSG_SHOW_IM_CONFIG = 3;
    static final int MSG_SHOW_IM_SUBTYPE_ENABLER = 2;
    static final int MSG_SHOW_IM_SUBTYPE_PICKER = 1;
    static final int MSG_SHOW_SOFT_INPUT = 1020;
    static final int MSG_START_INPUT = 2000;
    static final int MSG_START_VR_INPUT = 2010;
    static final int MSG_SWITCH_IME = 3050;
    static final int MSG_SYSTEM_UNLOCK_USER = 5000;
    static final int MSG_UNBIND_CLIENT = 3000;
    static final int MSG_UNBIND_INPUT = 1000;
    private static final int NOT_A_SUBTYPE_ID = -1;
    static final int SECURE_SUGGESTION_SPANS_MAX_SIZE = 20;
    static final String TAG = "InputMethodManagerService";
    private static final String TAG_TRY_SUPPRESSING_IME_SWITCHER = "TrySuppressingImeSwitcher";
    static final long TIME_TO_RECONNECT = 3000;
    private boolean mAccessibilityRequestingNoSoftKeyboard;
    private final AppOpsManager mAppOpsManager;
    boolean mBoundToMethod;
    final HandlerCaller mCaller;
    final Context mContext;
    EditorInfo mCurAttribute;
    ClientState mCurClient;
    private boolean mCurClientInKeyguard;
    IBinder mCurFocusedWindow;
    ClientState mCurFocusedWindowClient;
    int mCurFocusedWindowSoftInputMode;
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
    private final HardKeyboardListener mHardKeyboardListener;
    final boolean mHasFeature;
    boolean mHaveConnection;
    private PendingIntent mImeSwitchPendingIntent;
    private Notification.Builder mImeSwitcherNotification;
    int mImeWindowVis;
    private InputMethodInfo[] mIms;
    boolean mInFullscreenMode;
    boolean mInputShown;
    private KeyguardManager mKeyguardManager;
    long mLastBindTime;
    private LocaleList mLastSystemLocales;
    private NotificationManager mNotificationManager;
    private boolean mNotificationShown;
    final Resources mRes;
    final InputMethodUtils.InputMethodSettings mSettings;
    boolean mShowExplicitlyRequested;
    boolean mShowForced;
    private boolean mShowImeWithHardKeyboard;
    private boolean mShowOngoingImeSwitcherForPhones;
    boolean mShowRequested;
    private final String mSlotIme;

    @GuardedBy("mMethodMap")
    private final StartInputHistory mStartInputHistory;
    private StatusBarManagerService mStatusBar;
    private int[] mSubtypeIds;
    private Toast mSubtypeSwitchedByShortCutToast;
    private final InputMethodSubtypeSwitchingController mSwitchingController;
    private AlertDialog mSwitchingDialog;
    private View mSwitchingDialogTitleView;
    boolean mSystemReady;
    private final UserManager mUserManager;
    final ArrayList<InputMethodInfo> mMethodList = new ArrayList<>();
    final HashMap<String, InputMethodInfo> mMethodMap = new HashMap<>();
    private final LruCache<SuggestionSpan, InputMethodInfo> mSecureSuggestionSpans = new LruCache<>(20);

    @GuardedBy("mMethodMap")
    private int mMethodMapUpdateCount = 0;
    final ServiceConnection mVisibleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };
    boolean mVisibleBound = false;
    private final IVrStateCallbacks mVrStateCallbacks = new IVrStateCallbacks.Stub() {
        public void onVrStateChanged(boolean z) {
            if (!z) {
                InputMethodManagerService.this.restoreNonVrImeFromSettingsNoCheck();
            }
        }
    };
    final HashMap<IBinder, ClientState> mClients = new HashMap<>();
    private final HashMap<InputMethodInfo, ArrayList<InputMethodSubtype>> mShortcutInputMethodsAndSubtypes = new HashMap<>();
    boolean mIsInteractive = true;
    int mCurUserActionNotificationSequenceNumber = 0;
    int mBackDisposition = 0;
    private IBinder mSwitchingDialogToken = new Binder();
    private final MyPackageMonitor mMyPackageMonitor = new MyPackageMonitor();
    private boolean mBindInstantServiceAllowed = false;

    @GuardedBy("mMethodMap")
    private final WeakHashMap<IBinder, StartInputInfo> mStartInputMap = new WeakHashMap<>();
    private final IPackageManager mIPackageManager = AppGlobals.getPackageManager();
    final Handler mHandler = new Handler(this);
    final SettingsObserver mSettingsObserver = new SettingsObserver(this.mHandler);
    final IWindowManager mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    final WindowManagerInternal mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);

    @Retention(RetentionPolicy.SOURCE)
    private @interface HardKeyboardBehavior {
        public static final int WIRED_AFFORDANCE = 1;
        public static final int WIRELESS_AFFORDANCE = 0;
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ShellCommandResult {
        public static final int FAILURE = -1;
        public static final int SUCCESS = 0;
    }

    private static final class DebugFlag {
        private static final Object LOCK = new Object();
        private final boolean mDefaultValue;
        private final String mKey;

        @GuardedBy("LOCK")
        private boolean mValue;

        public DebugFlag(String str, boolean z) {
            this.mKey = str;
            this.mDefaultValue = z;
            this.mValue = SystemProperties.getBoolean(str, z);
        }

        void refresh() {
            synchronized (LOCK) {
                this.mValue = SystemProperties.getBoolean(this.mKey, this.mDefaultValue);
            }
        }

        boolean value() {
            boolean z;
            synchronized (LOCK) {
                z = this.mValue;
            }
            return z;
        }
    }

    private static final class DebugFlags {
        static final DebugFlag FLAG_OPTIMIZE_START_INPUT = new DebugFlag("debug.optimize_startinput", false);

        private DebugFlags() {
        }
    }

    static class SessionState {
        InputChannel channel;
        final ClientState client;
        final IInputMethod method;
        IInputMethodSession session;

        public String toString() {
            return "SessionState{uid " + this.client.uid + " pid " + this.client.pid + " method " + Integer.toHexString(System.identityHashCode(this.method)) + " session " + Integer.toHexString(System.identityHashCode(this.session)) + " channel " + this.channel + "}";
        }

        SessionState(ClientState clientState, IInputMethod iInputMethod, IInputMethodSession iInputMethodSession, InputChannel inputChannel) {
            this.client = clientState;
            this.method = iInputMethod;
            this.session = iInputMethodSession;
            this.channel = inputChannel;
        }
    }

    private void restoreNonVrImeFromSettingsNoCheck() {
        synchronized (this.mMethodMap) {
            String selectedInputMethod = this.mSettings.getSelectedInputMethod();
            setInputMethodLocked(selectedInputMethod, this.mSettings.getSelectedInputMethodSubtypeId(selectedInputMethod));
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

        ClientState(IInputMethodClient iInputMethodClient, IInputContext iInputContext, int i, int i2) {
            this.client = iInputMethodClient;
            this.inputContext = iInputContext;
            this.uid = i;
            this.pid = i2;
            this.binding = new InputBinding(null, this.inputContext.asBinder(), this.uid, this.pid);
        }
    }

    private static class StartInputInfo {
        private static final AtomicInteger sSequenceNumber = new AtomicInteger(0);
        final int mClientBindSequenceNumber;
        final EditorInfo mEditorInfo;
        final String mImeId;
        final IBinder mImeToken;
        final boolean mRestarting;
        final int mStartInputReason;
        final IBinder mTargetWindow;
        final int mTargetWindowSoftInputMode;
        final int mSequenceNumber = sSequenceNumber.getAndIncrement();
        final long mTimestamp = SystemClock.uptimeMillis();
        final long mWallTime = System.currentTimeMillis();

        StartInputInfo(IBinder iBinder, String str, int i, boolean z, IBinder iBinder2, EditorInfo editorInfo, int i2, int i3) {
            this.mImeToken = iBinder;
            this.mImeId = str;
            this.mStartInputReason = i;
            this.mRestarting = z;
            this.mTargetWindow = iBinder2;
            this.mEditorInfo = editorInfo;
            this.mTargetWindowSoftInputMode = i2;
            this.mClientBindSequenceNumber = i3;
        }
    }

    private static final class StartInputHistory {
        private static final int ENTRY_SIZE_FOR_HIGH_RAM_DEVICE = 16;
        private static final int ENTRY_SIZE_FOR_LOW_RAM_DEVICE = 5;
        private final Entry[] mEntries;
        private int mNextIndex;

        private StartInputHistory() {
            this.mEntries = new Entry[getEntrySize()];
            this.mNextIndex = 0;
        }

        private static int getEntrySize() {
            if (ActivityManager.isLowRamDeviceStatic()) {
                return 5;
            }
            return 16;
        }

        private static final class Entry {
            int mClientBindSequenceNumber;
            EditorInfo mEditorInfo;
            String mImeId;
            String mImeTokenString;
            boolean mRestarting;
            int mSequenceNumber;
            int mStartInputReason;
            int mTargetWindowSoftInputMode;
            String mTargetWindowString;
            long mTimestamp;
            long mWallTime;

            Entry(StartInputInfo startInputInfo) {
                set(startInputInfo);
            }

            void set(StartInputInfo startInputInfo) {
                this.mSequenceNumber = startInputInfo.mSequenceNumber;
                this.mTimestamp = startInputInfo.mTimestamp;
                this.mWallTime = startInputInfo.mWallTime;
                this.mImeTokenString = String.valueOf(startInputInfo.mImeToken);
                this.mImeId = startInputInfo.mImeId;
                this.mStartInputReason = startInputInfo.mStartInputReason;
                this.mRestarting = startInputInfo.mRestarting;
                this.mTargetWindowString = String.valueOf(startInputInfo.mTargetWindow);
                this.mEditorInfo = startInputInfo.mEditorInfo;
                this.mTargetWindowSoftInputMode = startInputInfo.mTargetWindowSoftInputMode;
                this.mClientBindSequenceNumber = startInputInfo.mClientBindSequenceNumber;
            }
        }

        void addEntry(StartInputInfo startInputInfo) {
            int i = this.mNextIndex;
            if (this.mEntries[i] == null) {
                this.mEntries[i] = new Entry(startInputInfo);
            } else {
                this.mEntries[i].set(startInputInfo);
            }
            this.mNextIndex = (this.mNextIndex + 1) % this.mEntries.length;
        }

        void dump(PrintWriter printWriter, String str) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            for (int i = 0; i < this.mEntries.length; i++) {
                Entry entry = this.mEntries[(this.mNextIndex + i) % this.mEntries.length];
                if (entry != null) {
                    printWriter.print(str);
                    printWriter.println("StartInput #" + entry.mSequenceNumber + ":");
                    printWriter.print(str);
                    printWriter.println(" time=" + simpleDateFormat.format(new Date(entry.mWallTime)) + " (timestamp=" + entry.mTimestamp + ") reason=" + InputMethodClient.getStartInputReason(entry.mStartInputReason) + " restarting=" + entry.mRestarting);
                    printWriter.print(str);
                    StringBuilder sb = new StringBuilder();
                    sb.append(" imeToken=");
                    sb.append(entry.mImeTokenString);
                    sb.append(" [");
                    sb.append(entry.mImeId);
                    sb.append("]");
                    printWriter.println(sb.toString());
                    printWriter.print(str);
                    printWriter.println(" targetWin=" + entry.mTargetWindowString + " [" + entry.mEditorInfo.packageName + "] clientBindSeq=" + entry.mClientBindSequenceNumber);
                    printWriter.print(str);
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(" softInputMode=");
                    sb2.append(InputMethodClient.softInputModeToString(entry.mTargetWindowSoftInputMode));
                    printWriter.println(sb2.toString());
                    printWriter.print(str);
                    printWriter.println(" inputType=0x" + Integer.toHexString(entry.mEditorInfo.inputType) + " imeOptions=0x" + Integer.toHexString(entry.mEditorInfo.imeOptions) + " fieldId=0x" + Integer.toHexString(entry.mEditorInfo.fieldId) + " fieldName=" + entry.mEditorInfo.fieldName + " actionId=" + entry.mEditorInfo.actionId + " actionLabel=" + ((Object) entry.mEditorInfo.actionLabel));
                }
            }
        }
    }

    class SettingsObserver extends ContentObserver {
        String mLastEnabled;
        boolean mRegistered;
        int mUserId;

        SettingsObserver(Handler handler) {
            super(handler);
            this.mRegistered = false;
            this.mLastEnabled = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }

        public void registerContentObserverLocked(int i) {
            if (this.mRegistered && this.mUserId == i) {
                return;
            }
            ContentResolver contentResolver = InputMethodManagerService.this.mContext.getContentResolver();
            if (this.mRegistered) {
                InputMethodManagerService.this.mContext.getContentResolver().unregisterContentObserver(this);
                this.mRegistered = false;
            }
            if (this.mUserId != i) {
                this.mLastEnabled = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                this.mUserId = i;
            }
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this, i);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("enabled_input_methods"), false, this, i);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("selected_input_method_subtype"), false, this, i);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("show_ime_with_hard_keyboard"), false, this, i);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_soft_keyboard_mode"), false, this, i);
            this.mRegistered = true;
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            Uri uriFor = Settings.Secure.getUriFor("show_ime_with_hard_keyboard");
            Uri uriFor2 = Settings.Secure.getUriFor("accessibility_soft_keyboard_mode");
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (uriFor.equals(uri)) {
                    InputMethodManagerService.this.updateKeyboardFromSettingsLocked();
                } else {
                    boolean zEquals = uriFor2.equals(uri);
                    boolean z2 = true;
                    if (zEquals) {
                        InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard = Settings.Secure.getIntForUser(InputMethodManagerService.this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, this.mUserId) == 1;
                        if (InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard) {
                            boolean z3 = InputMethodManagerService.this.mShowRequested;
                            InputMethodManagerService.this.hideCurrentInputLocked(0, null);
                            InputMethodManagerService.this.mShowRequested = z3;
                        } else if (InputMethodManagerService.this.mShowRequested) {
                            InputMethodManagerService.this.showCurrentInputLocked(1, null);
                        }
                    } else {
                        String enabledInputMethodsStr = InputMethodManagerService.this.mSettings.getEnabledInputMethodsStr();
                        if (!this.mLastEnabled.equals(enabledInputMethodsStr)) {
                            this.mLastEnabled = enabledInputMethodsStr;
                        } else {
                            z2 = false;
                        }
                        InputMethodManagerService.this.updateInputMethodsFromSettingsLocked(z2);
                    }
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
            if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
                InputMethodManagerService.this.onActionLocaleChanged();
                return;
            }
            if (InputMethodManagerService.ACTION_SHOW_INPUT_METHOD_PICKER.equals(action)) {
                InputMethodManagerService.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
                return;
            }
            Slog.w(InputMethodManagerService.TAG, "Unexpected intent " + intent);
        }
    }

    private void startVrInputMethodNoCheck(ComponentName componentName) {
        if (componentName == null) {
            restoreNonVrImeFromSettingsNoCheck();
            return;
        }
        synchronized (this.mMethodMap) {
            String packageName = componentName.getPackageName();
            Iterator<InputMethodInfo> it = this.mMethodList.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                InputMethodInfo next = it.next();
                if (TextUtils.equals(next.getPackageName(), packageName) && next.isVrOnly()) {
                    setInputMethodEnabledLocked(next.getId(), true);
                    setInputMethodLocked(next.getId(), -1);
                    break;
                }
            }
        }
    }

    void onActionLocaleChanged() {
        synchronized (this.mMethodMap) {
            LocaleList locales = this.mRes.getConfiguration().getLocales();
            if (locales == null || !locales.equals(this.mLastSystemLocales)) {
                buildInputMethodListLocked(true);
                resetDefaultImeLocked(this.mContext);
                updateFromSettingsLocked(true);
                this.mLastSystemLocales = locales;
            }
        }
    }

    final class MyPackageMonitor extends PackageMonitor {

        @GuardedBy("mMethodMap")
        private final ArraySet<String> mKnownImePackageNames = new ArraySet<>();
        private final ArrayList<String> mChangedPackages = new ArrayList<>();
        private boolean mImePackageAppeared = false;

        MyPackageMonitor() {
        }

        @GuardedBy("mMethodMap")
        void clearKnownImePackageNamesLocked() {
            this.mKnownImePackageNames.clear();
        }

        @GuardedBy("mMethodMap")
        final void addKnownImePackageNameLocked(String str) {
            this.mKnownImePackageNames.add(str);
        }

        @GuardedBy("mMethodMap")
        private boolean isChangingPackagesOfCurrentUserLocked() {
            return getChangingUserId() == InputMethodManagerService.this.mSettings.getCurrentUserId();
        }

        public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (!isChangingPackagesOfCurrentUserLocked()) {
                    return false;
                }
                String selectedInputMethod = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                int size = InputMethodManagerService.this.mMethodList.size();
                if (selectedInputMethod != null) {
                    for (int i2 = 0; i2 < size; i2++) {
                        InputMethodInfo inputMethodInfo = InputMethodManagerService.this.mMethodList.get(i2);
                        if (inputMethodInfo.getId().equals(selectedInputMethod)) {
                            for (String str : strArr) {
                                if (inputMethodInfo.getPackageName().equals(str)) {
                                    if (z) {
                                        InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                        InputMethodManagerService.this.chooseNewDefaultIMELocked();
                                        return true;
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            }
        }

        public void onBeginPackageChanges() {
            clearPackageChangeState();
        }

        public void onPackageAppeared(String str, int i) {
            if (!this.mImePackageAppeared && !InputMethodManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.view.InputMethod").setPackage(str), InputMethodManagerService.this.getComponentMatchingFlags(512), getChangingUserId()).isEmpty()) {
                this.mImePackageAppeared = true;
            }
            this.mChangedPackages.add(str);
        }

        public void onPackageDisappeared(String str, int i) {
            this.mChangedPackages.add(str);
        }

        public void onPackageModified(String str) {
            this.mChangedPackages.add(str);
        }

        public void onPackagesSuspended(String[] strArr) {
            for (String str : strArr) {
                this.mChangedPackages.add(str);
            }
        }

        public void onPackagesUnsuspended(String[] strArr) {
            for (String str : strArr) {
                this.mChangedPackages.add(str);
            }
        }

        public void onFinishPackageChanges() {
            onFinishPackageChangesInternal();
            clearPackageChangeState();
        }

        private void clearPackageChangeState() {
            this.mChangedPackages.clear();
            this.mImePackageAppeared = false;
        }

        @GuardedBy("mMethodMap")
        private boolean shouldRebuildInputMethodListLocked() {
            if (this.mImePackageAppeared) {
                return true;
            }
            int size = this.mChangedPackages.size();
            for (int i = 0; i < size; i++) {
                if (this.mKnownImePackageNames.contains(this.mChangedPackages.get(i))) {
                    return true;
                }
            }
            return false;
        }

        private void onFinishPackageChangesInternal() {
            InputMethodInfo inputMethodInfo;
            boolean z;
            int iIsPackageDisappearing;
            ServiceInfo serviceInfo;
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (isChangingPackagesOfCurrentUserLocked()) {
                    if (shouldRebuildInputMethodListLocked()) {
                        String selectedInputMethod = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                        int size = InputMethodManagerService.this.mMethodList.size();
                        InputMethodInfo inputMethodInfo2 = null;
                        if (selectedInputMethod != null) {
                            inputMethodInfo = null;
                            for (int i = 0; i < size; i++) {
                                InputMethodInfo inputMethodInfo3 = InputMethodManagerService.this.mMethodList.get(i);
                                String id = inputMethodInfo3.getId();
                                if (id.equals(selectedInputMethod)) {
                                    inputMethodInfo = inputMethodInfo3;
                                }
                                int iIsPackageDisappearing2 = isPackageDisappearing(inputMethodInfo3.getPackageName());
                                if (isPackageModified(inputMethodInfo3.getPackageName())) {
                                    InputMethodManagerService.this.mFileManager.deleteAllInputMethodSubtypes(id);
                                }
                                if (iIsPackageDisappearing2 == 2 || iIsPackageDisappearing2 == 3) {
                                    Slog.i(InputMethodManagerService.TAG, "Input method uninstalled, disabling: " + inputMethodInfo3.getComponent());
                                    InputMethodManagerService.this.setInputMethodEnabledLocked(inputMethodInfo3.getId(), false);
                                }
                            }
                        } else {
                            inputMethodInfo = null;
                        }
                        InputMethodManagerService.this.buildInputMethodListLocked(false);
                        boolean zChooseNewDefaultIMELocked = true;
                        if (inputMethodInfo == null || !((iIsPackageDisappearing = isPackageDisappearing(inputMethodInfo.getPackageName())) == 2 || iIsPackageDisappearing == 3)) {
                            z = false;
                            inputMethodInfo2 = inputMethodInfo;
                        } else {
                            try {
                                serviceInfo = InputMethodManagerService.this.mIPackageManager.getServiceInfo(inputMethodInfo.getComponent(), 0, InputMethodManagerService.this.mSettings.getCurrentUserId());
                            } catch (RemoteException e) {
                                serviceInfo = null;
                            }
                            if (serviceInfo == null) {
                                Slog.i(InputMethodManagerService.TAG, "Current input method removed: " + selectedInputMethod);
                                InputMethodManagerService.this.updateSystemUiLocked(InputMethodManagerService.this.mCurToken, 0, InputMethodManagerService.this.mBackDisposition);
                                if (!InputMethodManagerService.this.chooseNewDefaultIMELocked()) {
                                    Slog.i(InputMethodManagerService.TAG, "Unsetting current input method");
                                    InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                    z = true;
                                }
                            }
                        }
                        if (inputMethodInfo2 == null) {
                            zChooseNewDefaultIMELocked = InputMethodManagerService.this.chooseNewDefaultIMELocked();
                        } else if (z || !isPackageModified(inputMethodInfo2.getPackageName())) {
                            zChooseNewDefaultIMELocked = z;
                        }
                        if (zChooseNewDefaultIMELocked) {
                            InputMethodManagerService.this.updateFromSettingsLocked(false);
                        }
                    }
                }
            }
        }
    }

    private static final class MethodCallback extends IInputSessionCallback.Stub {
        private final InputChannel mChannel;
        private final IInputMethod mMethod;
        private final InputMethodManagerService mParentIMMS;

        MethodCallback(InputMethodManagerService inputMethodManagerService, IInputMethod iInputMethod, InputChannel inputChannel) {
            this.mParentIMMS = inputMethodManagerService;
            this.mMethod = iInputMethod;
            this.mChannel = inputChannel;
        }

        public void sessionCreated(IInputMethodSession iInputMethodSession) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mParentIMMS.onSessionCreated(this.mMethod, iInputMethodSession, this.mChannel);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private class HardKeyboardListener implements WindowManagerInternal.OnHardKeyboardStatusChangeListener {
        private HardKeyboardListener() {
        }

        @Override
        public void onHardKeyboardStatusChange(boolean z) {
            InputMethodManagerService.this.mHandler.sendMessage(InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_HARD_KEYBOARD_SWITCH_CHANGED, Integer.valueOf(z ? 1 : 0)));
        }

        public void handleHardKeyboardStatusChange(boolean z) {
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (InputMethodManagerService.this.mSwitchingDialog != null && InputMethodManagerService.this.mSwitchingDialogTitleView != null && InputMethodManagerService.this.mSwitchingDialog.isShowing()) {
                    InputMethodManagerService.this.mSwitchingDialogTitleView.findViewById(R.id.clock).setVisibility(z ? 0 : 8);
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
        public void onSwitchUser(int i) {
            this.mService.onSwitchUser(i);
        }

        @Override
        public void onBootPhase(int i) {
            if (i == 550) {
                this.mService.systemRunning((StatusBarManagerService) ServiceManager.getService("statusbar"));
            }
        }

        @Override
        public void onUnlockUser(int i) {
            this.mService.mHandler.sendMessage(this.mService.mHandler.obtainMessage(InputMethodManagerService.MSG_SYSTEM_UNLOCK_USER, i, 0));
        }
    }

    void onUnlockUser(int i) {
        synchronized (this.mMethodMap) {
            int currentUserId = this.mSettings.getCurrentUserId();
            if (i != currentUserId) {
                return;
            }
            this.mSettings.switchCurrentUser(currentUserId, !this.mSystemReady);
            if (this.mSystemReady) {
                buildInputMethodListLocked(false);
                updateInputMethodsFromSettingsLocked(true);
            }
        }
    }

    void onSwitchUser(int i) {
        synchronized (this.mMethodMap) {
            switchUserLocked(i);
        }
    }

    public InputMethodManagerService(Context context) {
        int i = 0;
        this.mStartInputHistory = new StartInputHistory();
        this.mContext = context;
        this.mRes = context.getResources();
        this.mCaller = new HandlerCaller(context, (Looper) null, new HandlerCaller.Callback() {
            public void executeMessage(Message message) {
                InputMethodManagerService.this.handleMessage(message);
            }
        }, true);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mHardKeyboardListener = new HardKeyboardListener();
        this.mHasFeature = context.getPackageManager().hasSystemFeature("android.software.input_methods");
        this.mSlotIme = this.mContext.getString(R.string.mediasize_iso_c5);
        this.mHardKeyboardBehavior = this.mContext.getResources().getInteger(R.integer.config_cameraPrivacyLightAlsAveragingIntervalMillis);
        Bundle bundle = new Bundle();
        bundle.putBoolean("android.allowDuringSetup", true);
        this.mImeSwitcherNotification = new Notification.Builder(this.mContext, SystemNotificationChannels.VIRTUAL_KEYBOARD).setSmallIcon(R.drawable.ic_media_route_connected_dark_01_mtrl).setWhen(0L).setOngoing(true).addExtras(bundle).setCategory("sys").setColor(this.mContext.getColor(R.color.car_colorPrimary));
        this.mImeSwitchPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_SHOW_INPUT_METHOD_PICKER).setPackage(this.mContext.getPackageName()), 0);
        this.mShowOngoingImeSwitcherForPhones = false;
        this.mNotificationShown = false;
        try {
            i = ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mSettings = new InputMethodUtils.InputMethodSettings(this.mRes, context.getContentResolver(), this.mMethodMap, this.mMethodList, i, !this.mSystemReady);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, i);
        this.mSwitchingController = InputMethodSubtypeSwitchingController.createInstanceLocked(this.mSettings, context);
        IVrManager service = ServiceManager.getService("vrmanager");
        if (service != null) {
            try {
                service.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e2) {
                Slog.e(TAG, "Failed to register VR mode state listener.");
            }
        }
    }

    private void resetDefaultImeLocked(Context context) {
        if (this.mCurMethodId != null && !InputMethodUtils.isSystemIme(this.mMethodMap.get(this.mCurMethodId))) {
            return;
        }
        ArrayList defaultEnabledImes = InputMethodUtils.getDefaultEnabledImes(context, this.mSettings.getEnabledInputMethodListLocked());
        if (defaultEnabledImes.isEmpty()) {
            Slog.i(TAG, "No default found");
        } else {
            setSelectedInputMethodAndSubtypeLocked((InputMethodInfo) defaultEnabledImes.get(0), -1, false);
        }
    }

    @GuardedBy("mMethodMap")
    private void switchUserLocked(int i) {
        this.mSettingsObserver.registerContentObserverLocked(i);
        this.mSettings.switchCurrentUser(i, (this.mSystemReady && this.mUserManager.isUserUnlockingOrUnlocked(i)) ? false : true);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, i);
        boolean zIsEmpty = TextUtils.isEmpty(this.mSettings.getSelectedInputMethod());
        this.mLastSystemLocales = this.mRes.getConfiguration().getLocales();
        if (this.mSystemReady) {
            hideCurrentInputLocked(0, null);
            resetCurrentMethodAndClient(6);
            buildInputMethodListLocked(zIsEmpty);
            if (TextUtils.isEmpty(this.mSettings.getSelectedInputMethod())) {
                resetDefaultImeLocked(this.mContext);
            }
            updateFromSettingsLocked(true);
            try {
                startInputInnerLocked();
            } catch (RuntimeException e) {
                Slog.w(TAG, "Unexpected exception", e);
            }
        }
        if (zIsEmpty) {
            InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), i, this.mContext.getBasePackageName());
        }
    }

    void updateCurrentProfileIds() {
        this.mSettings.setCurrentProfileIds(this.mUserManager.getProfileIdsWithDisabled(this.mSettings.getCurrentUserId()));
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            return super.onTransact(i, parcel, parcel2, i2);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Input Method Manager Crash", e);
            }
            throw e;
        }
    }

    public void systemRunning(StatusBarManagerService statusBarManagerService) {
        synchronized (this.mMethodMap) {
            if (!this.mSystemReady) {
                this.mSystemReady = true;
                this.mLastSystemLocales = this.mRes.getConfiguration().getLocales();
                int currentUserId = this.mSettings.getCurrentUserId();
                this.mSettings.switchCurrentUser(currentUserId, !this.mUserManager.isUserUnlockingOrUnlocked(currentUserId));
                this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
                this.mStatusBar = statusBarManagerService;
                if (this.mStatusBar != null) {
                    this.mStatusBar.setIconVisibility(this.mSlotIme, false);
                }
                updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                this.mShowOngoingImeSwitcherForPhones = this.mRes.getBoolean(R.^attr-private.selectionDividersDistance);
                if (this.mShowOngoingImeSwitcherForPhones) {
                    this.mWindowManagerInternal.setOnHardKeyboardStatusChangeListener(this.mHardKeyboardListener);
                }
                this.mMyPackageMonitor.register(this.mContext, null, UserHandle.ALL, true);
                this.mSettingsObserver.registerContentObserverLocked(currentUserId);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
                intentFilter.addAction("android.intent.action.USER_ADDED");
                intentFilter.addAction("android.intent.action.USER_REMOVED");
                intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
                intentFilter.addAction(ACTION_SHOW_INPUT_METHOD_PICKER);
                this.mContext.registerReceiver(new ImmsBroadcastReceiver(), intentFilter);
                buildInputMethodListLocked(TextUtils.isEmpty(this.mSettings.getSelectedInputMethod()) ^ true ? false : true);
                resetDefaultImeLocked(this.mContext);
                updateFromSettingsLocked(true);
                InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), currentUserId, this.mContext.getBasePackageName());
                try {
                    startInputInnerLocked();
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Unexpected exception", e);
                }
            }
        }
    }

    private boolean calledFromValidUser() {
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        if (callingUid == 1000 || this.mSettings.isCurrentProfile(userId) || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            return true;
        }
        Slog.w(TAG, "--- IPC called from background users. Ignore. callers=" + Debug.getCallers(10));
        return false;
    }

    private boolean calledWithValidToken(IBinder iBinder) {
        if (iBinder == null && Binder.getCallingPid() == Process.myPid()) {
            return false;
        }
        if (iBinder == null || iBinder != this.mCurToken) {
            Slog.e(TAG, "Ignoring " + Debug.getCaller() + " due to an invalid token. uid:" + Binder.getCallingUid() + " token:" + iBinder);
            return false;
        }
        return true;
    }

    @GuardedBy("mMethodMap")
    private boolean bindCurrentInputMethodServiceLocked(Intent intent, ServiceConnection serviceConnection, int i) {
        if (intent == null || serviceConnection == null) {
            Slog.e(TAG, "--- bind failed: service = " + intent + ", conn = " + serviceConnection);
            return false;
        }
        if (this.mBindInstantServiceAllowed) {
            i |= DumpState.DUMP_CHANGES;
        }
        return this.mContext.bindServiceAsUser(intent, serviceConnection, i, new UserHandle(this.mSettings.getCurrentUserId()));
    }

    public List<InputMethodInfo> getInputMethodList() {
        return getInputMethodList(false);
    }

    public List<InputMethodInfo> getVrInputMethodList() {
        return getInputMethodList(true);
    }

    private List<InputMethodInfo> getInputMethodList(boolean z) {
        ArrayList arrayList;
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        synchronized (this.mMethodMap) {
            arrayList = new ArrayList();
            for (InputMethodInfo inputMethodInfo : this.mMethodList) {
                if (inputMethodInfo.isVrOnly() == z) {
                    arrayList.add(inputMethodInfo);
                }
            }
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

    public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String str, boolean z) {
        InputMethodInfo inputMethodInfo;
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        synchronized (this.mMethodMap) {
            if (str == null) {
                try {
                    if (this.mCurMethodId != null) {
                        inputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
                    } else {
                        inputMethodInfo = this.mMethodMap.get(str);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (inputMethodInfo == null) {
                return Collections.emptyList();
            }
            return this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, inputMethodInfo, z);
        }
    }

    public void addClient(IInputMethodClient iInputMethodClient, IInputContext iInputContext, int i, int i2) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            this.mClients.put(iInputMethodClient.asBinder(), new ClientState(iInputMethodClient, iInputContext, i, i2));
        }
    }

    public void removeClient(IInputMethodClient iInputMethodClient) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            ClientState clientStateRemove = this.mClients.remove(iInputMethodClient.asBinder());
            if (clientStateRemove != null) {
                clearClientSessionLocked(clientStateRemove);
                if (this.mCurClient == clientStateRemove) {
                    if (this.mBoundToMethod) {
                        this.mBoundToMethod = false;
                        if (this.mCurMethod != null) {
                            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(1000, this.mCurMethod));
                        }
                    }
                    this.mCurClient = null;
                }
                if (this.mCurFocusedWindowClient == clientStateRemove) {
                    this.mCurFocusedWindowClient = null;
                }
            }
        }
    }

    void executeOrSendMessage(IInterface iInterface, Message message) {
        if (iInterface.asBinder() instanceof Binder) {
            this.mCaller.sendMessage(message);
        } else {
            handleMessage(message);
            message.recycle();
        }
    }

    void unbindCurrentClientLocked(int i) {
        if (this.mCurClient != null) {
            if (this.mBoundToMethod) {
                this.mBoundToMethod = false;
                if (this.mCurMethod != null) {
                    executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(1000, this.mCurMethod));
                }
            }
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_SET_ACTIVE, 0, 0, this.mCurClient));
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_UNBIND_CLIENT, this.mCurSeq, i, this.mCurClient.client));
            this.mCurClient.sessionRequested = false;
            this.mCurClient = null;
            hideInputMethodMenuLocked();
        }
    }

    private int getImeShowFlags() {
        if (this.mShowForced) {
            return 3;
        }
        if (this.mShowExplicitlyRequested) {
            return 1;
        }
        return 0;
    }

    private int getAppShowFlags() {
        if (this.mShowForced) {
            return 2;
        }
        if (!this.mShowExplicitlyRequested) {
            return 1;
        }
        return 0;
    }

    @GuardedBy("mMethodMap")
    InputBindResult attachNewInputLocked(int i, boolean z) {
        if (!this.mBoundToMethod) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_BIND_INPUT, this.mCurMethod, this.mCurClient.binding));
            this.mBoundToMethod = true;
        }
        Binder binder = new Binder();
        StartInputInfo startInputInfo = new StartInputInfo(this.mCurToken, this.mCurId, i, !z, this.mCurFocusedWindow, this.mCurAttribute, this.mCurFocusedWindowSoftInputMode, this.mCurSeq);
        this.mStartInputMap.put(binder, startInputInfo);
        this.mStartInputHistory.addEntry(startInputInfo);
        SessionState sessionState = this.mCurClient.curSession;
        executeOrSendMessage(sessionState.method, this.mCaller.obtainMessageIIOOOO(2000, this.mCurInputContextMissingMethods, !z ? 1 : 0, binder, sessionState, this.mCurInputContext, this.mCurAttribute));
        if (this.mShowRequested) {
            showCurrentInputLocked(getAppShowFlags(), null);
        }
        return new InputBindResult(0, sessionState.session, sessionState.channel != null ? sessionState.channel.dup() : null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
    }

    @GuardedBy("mMethodMap")
    InputBindResult startInputLocked(int i, IInputMethodClient iInputMethodClient, IInputContext iInputContext, int i2, EditorInfo editorInfo, int i3) {
        if (this.mCurMethodId == null) {
            return InputBindResult.NO_IME;
        }
        ClientState clientState = this.mClients.get(iInputMethodClient.asBinder());
        if (clientState == null) {
            throw new IllegalArgumentException("unknown client " + iInputMethodClient.asBinder());
        }
        if (editorInfo == null) {
            Slog.w(TAG, "Ignoring startInput with null EditorInfo. uid=" + clientState.uid + " pid=" + clientState.pid);
            return InputBindResult.NULL_EDITOR_INFO;
        }
        try {
            if (!this.mIWindowManager.inputMethodClientHasFocus(clientState.client)) {
                return InputBindResult.NOT_IME_TARGET_WINDOW;
            }
        } catch (RemoteException e) {
        }
        return startInputUncheckedLocked(clientState, iInputContext, i2, editorInfo, i3, i);
    }

    @GuardedBy("mMethodMap")
    InputBindResult startInputUncheckedLocked(ClientState clientState, IInputContext iInputContext, int i, EditorInfo editorInfo, int i2, int i3) {
        if (this.mCurMethodId == null) {
            return InputBindResult.NO_IME;
        }
        if (!InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, clientState.uid, editorInfo.packageName)) {
            Slog.e(TAG, "Rejecting this client as it reported an invalid package name. uid=" + clientState.uid + " package=" + editorInfo.packageName);
            return InputBindResult.INVALID_PACKAGE_NAME;
        }
        if (this.mCurClient != clientState) {
            this.mCurClientInKeyguard = isKeyguardLocked();
            unbindCurrentClientLocked(1);
            if (this.mIsInteractive) {
                executeOrSendMessage(clientState.client, this.mCaller.obtainMessageIO(MSG_SET_ACTIVE, this.mIsInteractive ? 1 : 0, clientState));
            }
        }
        this.mCurSeq++;
        if (this.mCurSeq <= 0) {
            this.mCurSeq = 1;
        }
        this.mCurClient = clientState;
        this.mCurInputContext = iInputContext;
        this.mCurInputContextMissingMethods = i;
        this.mCurAttribute = editorInfo;
        if (this.mCurId != null && this.mCurId.equals(this.mCurMethodId)) {
            if (clientState.curSession != null) {
                return attachNewInputLocked(i3, (i2 & 256) != 0);
            }
            if (this.mHaveConnection) {
                if (this.mCurMethod != null) {
                    requestClientSessionLocked(clientState);
                    return new InputBindResult(1, (IInputMethodSession) null, (InputChannel) null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
                }
                if (SystemClock.uptimeMillis() < this.mLastBindTime + TIME_TO_RECONNECT) {
                    return new InputBindResult(2, (IInputMethodSession) null, (InputChannel) null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
                }
                EventLog.writeEvent(EventLogTags.IMF_FORCE_RECONNECT_IME, this.mCurMethodId, Long.valueOf(SystemClock.uptimeMillis() - this.mLastBindTime), 0);
            }
        }
        return startInputInnerLocked();
    }

    InputBindResult startInputInnerLocked() {
        if (this.mCurMethodId == null) {
            return InputBindResult.NO_IME;
        }
        if (!this.mSystemReady) {
            return new InputBindResult(7, (IInputMethodSession) null, (InputChannel) null, this.mCurMethodId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
        }
        InputMethodInfo inputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
        if (inputMethodInfo == null) {
            throw new IllegalArgumentException("Unknown id: " + this.mCurMethodId);
        }
        unbindCurrentMethodLocked(true);
        this.mCurIntent = new Intent("android.view.InputMethod");
        this.mCurIntent.setComponent(inputMethodInfo.getComponent());
        this.mCurIntent.putExtra("android.intent.extra.client_label", R.string.config_mt_sms_polling_text);
        if (BenesseExtension.getDchaState() == 0) {
            this.mCurIntent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.INPUT_METHOD_SETTINGS"), 0));
        }
        if (bindCurrentInputMethodServiceLocked(this.mCurIntent, this, IME_CONNECTION_BIND_FLAGS)) {
            this.mLastBindTime = SystemClock.uptimeMillis();
            this.mHaveConnection = true;
            this.mCurId = inputMethodInfo.getId();
            this.mCurToken = new Binder();
            try {
                this.mIWindowManager.addWindowToken(this.mCurToken, 2011, 0);
            } catch (RemoteException e) {
            }
            return new InputBindResult(2, (IInputMethodSession) null, (InputChannel) null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
        }
        this.mCurIntent = null;
        Slog.w(TAG, "Failure connecting to input method service: " + this.mCurIntent);
        return InputBindResult.IME_NOT_CONNECTED;
    }

    private InputBindResult startInput(int i, IInputMethodClient iInputMethodClient, IInputContext iInputContext, int i2, EditorInfo editorInfo, int i3) {
        InputBindResult inputBindResultStartInputLocked;
        if (!calledFromValidUser()) {
            return InputBindResult.INVALID_USER;
        }
        synchronized (this.mMethodMap) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                inputBindResultStartInputLocked = startInputLocked(i, iInputMethodClient, iInputContext, i2, editorInfo, i3);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        return inputBindResultStartInputLocked;
    }

    public void finishInput(IInputMethodClient iInputMethodClient) {
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this.mMethodMap) {
            if (this.mCurIntent != null && componentName.equals(this.mCurIntent.getComponent())) {
                this.mCurMethod = IInputMethod.Stub.asInterface(iBinder);
                if (this.mCurToken == null) {
                    Slog.w(TAG, "Service connected without a token!");
                    unbindCurrentMethodLocked(false);
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

    void onSessionCreated(IInputMethod iInputMethod, IInputMethodSession iInputMethodSession, InputChannel inputChannel) {
        synchronized (this.mMethodMap) {
            if (this.mCurMethod != null && iInputMethod != null && this.mCurMethod.asBinder() == iInputMethod.asBinder() && this.mCurClient != null) {
                clearClientSessionLocked(this.mCurClient);
                this.mCurClient.curSession = new SessionState(this.mCurClient, iInputMethod, iInputMethodSession, inputChannel);
                InputBindResult inputBindResultAttachNewInputLocked = attachNewInputLocked(9, true);
                if (inputBindResultAttachNewInputLocked.method != null) {
                    executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageOO(3010, this.mCurClient.client, inputBindResultAttachNewInputLocked));
                }
                return;
            }
            inputChannel.dispose();
        }
    }

    void unbindCurrentMethodLocked(boolean z) {
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
                if ((this.mImeWindowVis & 1) != 0 && z) {
                    this.mWindowManagerInternal.saveLastInputMethodWindowForTransition();
                }
                this.mIWindowManager.removeWindowToken(this.mCurToken, 0);
            } catch (RemoteException e) {
            }
            this.mCurToken = null;
        }
        this.mCurId = null;
        clearCurMethodLocked();
    }

    void resetCurrentMethodAndClient(int i) {
        this.mCurMethodId = null;
        unbindCurrentMethodLocked(false);
        unbindCurrentClientLocked(i);
    }

    void requestClientSessionLocked(ClientState clientState) {
        if (!clientState.sessionRequested) {
            InputChannel[] inputChannelArrOpenInputChannelPair = InputChannel.openInputChannelPair(clientState.toString());
            clientState.sessionRequested = true;
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOOO(MSG_CREATE_SESSION, this.mCurMethod, inputChannelArrOpenInputChannelPair[1], new MethodCallback(this, this.mCurMethod, inputChannelArrOpenInputChannelPair[0])));
        }
    }

    void clearClientSessionLocked(ClientState clientState) {
        finishSessionLocked(clientState.curSession);
        clientState.curSession = null;
        clientState.sessionRequested = false;
    }

    private void finishSessionLocked(SessionState sessionState) {
        if (sessionState != null) {
            if (sessionState.session != null) {
                try {
                    sessionState.session.finishSession();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Session failed to close due to remote exception", e);
                    updateSystemUiLocked(this.mCurToken, 0, this.mBackDisposition);
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
            Iterator<ClientState> it = this.mClients.values().iterator();
            while (it.hasNext()) {
                clearClientSessionLocked(it.next());
            }
            finishSessionLocked(this.mEnabledSession);
            this.mEnabledSession = null;
            this.mCurMethod = null;
        }
        if (this.mStatusBar != null) {
            this.mStatusBar.setIconVisibility(this.mSlotIme, false);
        }
        this.mInFullscreenMode = false;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        synchronized (this.mMethodMap) {
            if (this.mCurMethod != null && this.mCurIntent != null && componentName.equals(this.mCurIntent.getComponent())) {
                clearCurMethodLocked();
                this.mLastBindTime = SystemClock.uptimeMillis();
                this.mShowRequested = this.mInputShown;
                this.mInputShown = false;
                unbindCurrentClientLocked(3);
            }
        }
    }

    public void updateStatusIcon(IBinder iBinder, String str, int i) {
        CharSequence applicationLabel;
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(iBinder)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (i == 0) {
                        if (this.mStatusBar != null) {
                            this.mStatusBar.setIconVisibility(this.mSlotIme, false);
                        }
                    } else if (str != null) {
                        String string = null;
                        try {
                            applicationLabel = this.mContext.getPackageManager().getApplicationLabel(this.mIPackageManager.getApplicationInfo(str, 0, this.mSettings.getCurrentUserId()));
                        } catch (RemoteException e) {
                            applicationLabel = null;
                        }
                        if (this.mStatusBar != null) {
                            StatusBarManagerService statusBarManagerService = this.mStatusBar;
                            String str2 = this.mSlotIme;
                            if (applicationLabel != null) {
                                string = applicationLabel.toString();
                            }
                            statusBarManagerService.setIcon(str2, str, i, 0, string);
                            this.mStatusBar.setIconVisibility(this.mSlotIme, true);
                        }
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    private boolean shouldShowImeSwitcherLocked(int i) {
        if (!this.mShowOngoingImeSwitcherForPhones || this.mSwitchingDialog != null) {
            return false;
        }
        if ((this.mWindowManagerInternal.isKeyguardShowingAndNotOccluded() && this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardSecure()) || (i & 1) == 0) {
            return false;
        }
        if (this.mWindowManagerInternal.isHardKeyboardAvailable()) {
            if (this.mHardKeyboardBehavior == 0) {
                return true;
            }
        } else if ((i & 2) == 0) {
            return false;
        }
        ArrayList enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
        int size = enabledInputMethodListLocked.size();
        if (size > 2) {
            return true;
        }
        if (size < 1) {
            return false;
        }
        int i2 = 0;
        int i3 = 0;
        InputMethodSubtype inputMethodSubtype = null;
        InputMethodSubtype inputMethodSubtype2 = null;
        for (int i4 = 0; i4 < size; i4++) {
            List enabledInputMethodSubtypeListLocked = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, (InputMethodInfo) enabledInputMethodListLocked.get(i4), true);
            int size2 = enabledInputMethodSubtypeListLocked.size();
            if (size2 == 0) {
                i2++;
            } else {
                InputMethodSubtype inputMethodSubtype3 = inputMethodSubtype2;
                InputMethodSubtype inputMethodSubtype4 = inputMethodSubtype;
                int i5 = i3;
                int i6 = i2;
                for (int i7 = 0; i7 < size2; i7++) {
                    InputMethodSubtype inputMethodSubtype5 = (InputMethodSubtype) enabledInputMethodSubtypeListLocked.get(i7);
                    if (!inputMethodSubtype5.isAuxiliary()) {
                        i6++;
                        inputMethodSubtype4 = inputMethodSubtype5;
                    } else {
                        i5++;
                        inputMethodSubtype3 = inputMethodSubtype5;
                    }
                }
                i2 = i6;
                i3 = i5;
                inputMethodSubtype = inputMethodSubtype4;
                inputMethodSubtype2 = inputMethodSubtype3;
            }
        }
        if (i2 > 1 || i3 > 1) {
            return true;
        }
        if (i2 == 1 && i3 == 1) {
            return inputMethodSubtype == null || inputMethodSubtype2 == null || !((inputMethodSubtype.getLocale().equals(inputMethodSubtype2.getLocale()) || inputMethodSubtype2.overridesImplicitlyEnabledSubtype() || inputMethodSubtype.overridesImplicitlyEnabledSubtype()) && inputMethodSubtype.containsExtraValueKey(TAG_TRY_SUPPRESSING_IME_SWITCHER));
        }
        return false;
    }

    private boolean isKeyguardLocked() {
        return this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardLocked();
    }

    public void setImeWindowStatus(IBinder iBinder, IBinder iBinder2, int i, int i2) {
        StartInputInfo startInputInfo;
        boolean z;
        if (calledWithValidToken(iBinder)) {
            synchronized (this.mMethodMap) {
                startInputInfo = this.mStartInputMap.get(iBinder2);
                this.mImeWindowVis = i;
                this.mBackDisposition = i2;
                updateSystemUiLocked(iBinder, i, i2);
            }
            switch (i2) {
                case 1:
                    z = false;
                    break;
                case 2:
                    z = true;
                    break;
                default:
                    if ((i & 2) != 0) {
                    }
                    break;
            }
            this.mWindowManagerInternal.updateInputMethodWindowStatus(iBinder, (i & 2) != 0, z, startInputInfo != null ? startInputInfo.mTargetWindow : null);
        }
    }

    private void updateSystemUi(IBinder iBinder, int i, int i2) {
        synchronized (this.mMethodMap) {
            updateSystemUiLocked(iBinder, i, i2);
        }
    }

    private void updateSystemUiLocked(IBinder iBinder, int i, int i2) {
        if (!calledWithValidToken(iBinder)) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        if (i != 0) {
            try {
                if (isKeyguardLocked() && !this.mCurClientInKeyguard) {
                    i = 0;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
        boolean zShouldShowImeSwitcherLocked = shouldShowImeSwitcherLocked(i);
        if (this.mStatusBar != null) {
            this.mStatusBar.setImeWindowStatus(iBinder, i, i2, zShouldShowImeSwitcherLocked);
        }
        InputMethodInfo inputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
        if (inputMethodInfo != null && zShouldShowImeSwitcherLocked) {
            this.mImeSwitcherNotification.setContentTitle(this.mRes.getText(R.string.lockscreen_pattern_correct)).setContentText(InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, inputMethodInfo, this.mCurrentSubtype)).setContentIntent(this.mImeSwitchPendingIntent);
            try {
                if (this.mNotificationManager != null && !this.mIWindowManager.hasNavigationBar()) {
                    this.mNotificationManager.notifyAsUser(null, 8, this.mImeSwitcherNotification.build(), UserHandle.ALL);
                    this.mNotificationShown = true;
                }
            } catch (RemoteException e) {
            }
        } else if (this.mNotificationShown && this.mNotificationManager != null) {
            this.mNotificationManager.cancelAsUser(null, 8, UserHandle.ALL);
            this.mNotificationShown = false;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    public void registerSuggestionSpansForNotification(SuggestionSpan[] suggestionSpanArr) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            InputMethodInfo inputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
            for (SuggestionSpan suggestionSpan : suggestionSpanArr) {
                if (!TextUtils.isEmpty(suggestionSpan.getNotificationTargetClassName())) {
                    this.mSecureSuggestionSpans.put(suggestionSpan, inputMethodInfo);
                }
            }
        }
    }

    public boolean notifySuggestionPicked(SuggestionSpan suggestionSpan, String str, int i) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            InputMethodInfo inputMethodInfo = this.mSecureSuggestionSpans.get(suggestionSpan);
            if (inputMethodInfo == null) {
                return false;
            }
            String[] suggestions = suggestionSpan.getSuggestions();
            if (i >= 0 && i < suggestions.length) {
                String notificationTargetClassName = suggestionSpan.getNotificationTargetClassName();
                Intent intent = new Intent();
                intent.setClassName(inputMethodInfo.getPackageName(), notificationTargetClassName);
                intent.setAction("android.text.style.SUGGESTION_PICKED");
                intent.putExtra("before", str);
                intent.putExtra("after", suggestions[i]);
                intent.putExtra("hashcode", suggestionSpan.hashCode());
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return true;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            return false;
        }
    }

    void updateFromSettingsLocked(boolean z) {
        updateInputMethodsFromSettingsLocked(z);
        updateKeyboardFromSettingsLocked();
    }

    void updateInputMethodsFromSettingsLocked(boolean z) {
        if (z) {
            ArrayList enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
            for (int i = 0; i < enabledInputMethodListLocked.size(); i++) {
                InputMethodInfo inputMethodInfo = (InputMethodInfo) enabledInputMethodListLocked.get(i);
                try {
                    ApplicationInfo applicationInfo = this.mIPackageManager.getApplicationInfo(inputMethodInfo.getPackageName(), 32768, this.mSettings.getCurrentUserId());
                    if (applicationInfo != null && applicationInfo.enabledSetting == 4) {
                        this.mIPackageManager.setApplicationEnabledSetting(inputMethodInfo.getPackageName(), 0, 1, this.mSettings.getCurrentUserId(), this.mContext.getBasePackageName());
                    }
                } catch (RemoteException e) {
                }
            }
        }
        String selectedInputMethod = this.mSettings.getSelectedInputMethod();
        if (TextUtils.isEmpty(selectedInputMethod) && chooseNewDefaultIMELocked()) {
            selectedInputMethod = this.mSettings.getSelectedInputMethod();
        }
        if (!TextUtils.isEmpty(selectedInputMethod)) {
            try {
                setInputMethodLocked(selectedInputMethod, this.mSettings.getSelectedInputMethodSubtypeId(selectedInputMethod));
            } catch (IllegalArgumentException e2) {
                Slog.w(TAG, "Unknown input method from prefs: " + selectedInputMethod, e2);
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
        if (this.mSwitchingDialog != null && this.mSwitchingDialogTitleView != null && this.mSwitchingDialog.isShowing()) {
            ((Switch) this.mSwitchingDialogTitleView.findViewById(R.id.close_button)).setChecked(this.mShowImeWithHardKeyboard);
        }
    }

    void setInputMethodLocked(String str, int i) {
        InputMethodSubtype currentInputMethodSubtypeLocked;
        InputMethodInfo inputMethodInfo = this.mMethodMap.get(str);
        if (inputMethodInfo == null) {
            throw new IllegalArgumentException("Unknown id: " + str);
        }
        if (str.equals(this.mCurMethodId)) {
            int subtypeCount = inputMethodInfo.getSubtypeCount();
            if (subtypeCount <= 0) {
                return;
            }
            InputMethodSubtype inputMethodSubtype = this.mCurrentSubtype;
            if (i >= 0 && i < subtypeCount) {
                currentInputMethodSubtypeLocked = inputMethodInfo.getSubtypeAt(i);
            } else {
                currentInputMethodSubtypeLocked = getCurrentInputMethodSubtypeLocked();
            }
            if (currentInputMethodSubtypeLocked == null || inputMethodSubtype == null) {
                Slog.w(TAG, "Illegal subtype state: old subtype = " + inputMethodSubtype + ", new subtype = " + currentInputMethodSubtypeLocked);
                return;
            }
            if (currentInputMethodSubtypeLocked != inputMethodSubtype) {
                setSelectedInputMethodAndSubtypeLocked(inputMethodInfo, i, true);
                if (this.mCurMethod != null) {
                    try {
                        updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                        this.mCurMethod.changeInputMethodSubtype(currentInputMethodSubtypeLocked);
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
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            setSelectedInputMethodAndSubtypeLocked(inputMethodInfo, i, false);
            this.mCurMethodId = str;
            if (((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).isSystemReady()) {
                Intent intent = new Intent("android.intent.action.INPUT_METHOD_CHANGED");
                intent.addFlags(536870912);
                intent.putExtra("input_method_id", str);
                this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            }
            unbindCurrentClientLocked(2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean showSoftInput(IInputMethodClient iInputMethodClient, int i, ResultReceiver resultReceiver) {
        if (!calledFromValidUser()) {
            return false;
        }
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || iInputMethodClient == null || this.mCurClient.client.asBinder() != iInputMethodClient.asBinder()) {
                    try {
                        if (!this.mIWindowManager.inputMethodClientHasFocus(iInputMethodClient)) {
                            Slog.w(TAG, "Ignoring showSoftInput of uid " + callingUid + ": " + iInputMethodClient);
                            return false;
                        }
                    } catch (RemoteException e) {
                        return false;
                    }
                }
                return showCurrentInputLocked(i, resultReceiver);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean showCurrentInputLocked(int i, ResultReceiver resultReceiver) {
        this.mShowRequested = true;
        if (this.mAccessibilityRequestingNoSoftKeyboard) {
            return false;
        }
        if ((i & 2) != 0) {
            this.mShowExplicitlyRequested = true;
            this.mShowForced = true;
        } else if ((i & 1) == 0) {
            this.mShowExplicitlyRequested = true;
        }
        if (!this.mSystemReady) {
            return false;
        }
        if (this.mCurMethod != null) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageIOO(MSG_SHOW_SOFT_INPUT, getImeShowFlags(), this.mCurMethod, resultReceiver));
            this.mInputShown = true;
            if (!this.mHaveConnection || this.mVisibleBound) {
                return true;
            }
            bindCurrentInputMethodServiceLocked(this.mCurIntent, this.mVisibleConnection, IME_VISIBLE_BIND_FLAGS);
            this.mVisibleBound = true;
            return true;
        }
        if (this.mHaveConnection && SystemClock.uptimeMillis() >= this.mLastBindTime + TIME_TO_RECONNECT) {
            EventLog.writeEvent(EventLogTags.IMF_FORCE_RECONNECT_IME, this.mCurMethodId, Long.valueOf(SystemClock.uptimeMillis() - this.mLastBindTime), 1);
            Slog.w(TAG, "Force disconnect/connect to the IME in showCurrentInputLocked()");
            this.mContext.unbindService(this);
            bindCurrentInputMethodServiceLocked(this.mCurIntent, this, IME_CONNECTION_BIND_FLAGS);
        }
        return false;
    }

    public boolean hideSoftInput(IInputMethodClient iInputMethodClient, int i, ResultReceiver resultReceiver) {
        if (!calledFromValidUser()) {
            return false;
        }
        Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || iInputMethodClient == null || this.mCurClient.client.asBinder() != iInputMethodClient.asBinder()) {
                    try {
                        if (!this.mIWindowManager.inputMethodClientHasFocus(iInputMethodClient)) {
                            return false;
                        }
                    } catch (RemoteException e) {
                        return false;
                    }
                }
                return hideCurrentInputLocked(i, resultReceiver);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean hideCurrentInputLocked(int i, ResultReceiver resultReceiver) {
        if ((i & 1) != 0 && (this.mShowExplicitlyRequested || this.mShowForced)) {
            return false;
        }
        if (this.mShowForced && (i & 2) != 0) {
            return false;
        }
        boolean z = true;
        if (this.mCurMethod != null && (this.mInputShown || (this.mImeWindowVis & 1) != 0)) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_HIDE_SOFT_INPUT, this.mCurMethod, resultReceiver));
        } else {
            z = false;
        }
        if (this.mHaveConnection && this.mVisibleBound) {
            this.mContext.unbindService(this.mVisibleConnection);
            this.mVisibleBound = false;
        }
        this.mInputShown = false;
        this.mShowRequested = false;
        this.mShowExplicitlyRequested = false;
        this.mShowForced = false;
        return z;
    }

    public InputBindResult startInputOrWindowGainedFocus(int i, IInputMethodClient iInputMethodClient, IBinder iBinder, int i2, int i3, int i4, EditorInfo editorInfo, IInputContext iInputContext, int i5, int i6) {
        InputBindResult inputBindResultStartInput;
        if (iBinder != null) {
            inputBindResultStartInput = windowGainedFocus(i, iInputMethodClient, iBinder, i2, i3, i4, editorInfo, iInputContext, i5, i6);
        } else {
            inputBindResultStartInput = startInput(i, iInputMethodClient, iInputContext, i5, editorInfo, i2);
        }
        if (inputBindResultStartInput == null) {
            Slog.wtf(TAG, "InputBindResult is @NonNull. startInputReason=" + InputMethodClient.getStartInputReason(i) + " windowFlags=#" + Integer.toHexString(i4) + " editorInfo=" + editorInfo);
            return InputBindResult.NULL;
        }
        return inputBindResultStartInput;
    }

    private InputBindResult windowGainedFocus(int i, IInputMethodClient iInputMethodClient, IBinder iBinder, int i2, int i3, int i4, EditorInfo editorInfo, IInputContext iInputContext, int i5, int i6) {
        int i7;
        InputBindResult inputBindResultStartInputUncheckedLocked;
        InputBindResult inputBindResultStartInputUncheckedLocked2;
        boolean zCalledFromValidUser = calledFromValidUser();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                ClientState clientState = this.mClients.get(iInputMethodClient.asBinder());
                if (clientState == null) {
                    throw new IllegalArgumentException("unknown client " + iInputMethodClient.asBinder());
                }
                try {
                    if (!this.mIWindowManager.inputMethodClientHasFocus(clientState.client)) {
                        return InputBindResult.NOT_IME_TARGET_WINDOW;
                    }
                } catch (RemoteException e) {
                }
                boolean z = false;
                if (!zCalledFromValidUser) {
                    Slog.w(TAG, "A background user is requesting window. Hiding IME.");
                    Slog.w(TAG, "If you want to interect with IME, you need android.permission.INTERACT_ACROSS_USERS_FULL");
                    hideCurrentInputLocked(0, null);
                    return InputBindResult.INVALID_USER;
                }
                if (this.mCurFocusedWindow == iBinder) {
                    if (editorInfo != null) {
                        return startInputUncheckedLocked(clientState, iInputContext, i5, editorInfo, i2, i);
                    }
                    return new InputBindResult(3, (IInputMethodSession) null, (InputChannel) null, (String) null, -1, -1);
                }
                this.mCurFocusedWindow = iBinder;
                this.mCurFocusedWindowSoftInputMode = i3;
                this.mCurFocusedWindowClient = clientState;
                int i8 = 1;
                boolean z2 = (i3 & 240) == 16 || this.mRes.getConfiguration().isLayoutSizeAtLeast(3);
                int i9 = i2 & 2;
                boolean z3 = i9 != 0;
                switch (i3 & 15) {
                    case 0:
                        if (z3 && z2) {
                            if (z3 && z2 && (i3 & 256) != 0) {
                                if (editorInfo != null) {
                                    z = true;
                                    inputBindResultStartInputUncheckedLocked = startInputUncheckedLocked(clientState, iInputContext, i5, editorInfo, i2, i);
                                    i7 = 1;
                                } else {
                                    i7 = 1;
                                    inputBindResultStartInputUncheckedLocked = null;
                                }
                                showCurrentInputLocked(i7, null);
                                inputBindResultStartInputUncheckedLocked2 = inputBindResultStartInputUncheckedLocked;
                                break;
                            }
                        } else if (WindowManager.LayoutParams.mayUseInputMethod(i4)) {
                            hideCurrentInputLocked(2, null);
                        }
                        inputBindResultStartInputUncheckedLocked2 = null;
                        break;
                    case 1:
                        inputBindResultStartInputUncheckedLocked2 = null;
                        break;
                    case 2:
                        if ((i3 & 256) != 0) {
                            hideCurrentInputLocked(0, null);
                        }
                        inputBindResultStartInputUncheckedLocked2 = null;
                        break;
                    case 3:
                        hideCurrentInputLocked(0, null);
                        inputBindResultStartInputUncheckedLocked2 = null;
                        break;
                    case 4:
                        if ((i3 & 256) == 0) {
                            inputBindResultStartInputUncheckedLocked2 = null;
                        } else if (!InputMethodUtils.isSoftInputModeStateVisibleAllowed(i6, i2)) {
                            Slog.e(TAG, "SOFT_INPUT_STATE_VISIBLE is ignored because there is no focused view that also returns true from View#onCheckIsTextEditor()");
                            inputBindResultStartInputUncheckedLocked2 = null;
                        } else {
                            if (editorInfo != null) {
                                z = true;
                                inputBindResultStartInputUncheckedLocked2 = startInputUncheckedLocked(clientState, iInputContext, i5, editorInfo, i2, i);
                                i8 = 1;
                            } else {
                                inputBindResultStartInputUncheckedLocked2 = null;
                            }
                            showCurrentInputLocked(i8, null);
                        }
                        break;
                    case 5:
                        if (!InputMethodUtils.isSoftInputModeStateVisibleAllowed(i6, i2)) {
                            Slog.e(TAG, "SOFT_INPUT_STATE_ALWAYS_VISIBLE is ignored because there is no focused view that also returns true from View#onCheckIsTextEditor()");
                            inputBindResultStartInputUncheckedLocked2 = null;
                        } else {
                            if (editorInfo != null) {
                                z = true;
                                inputBindResultStartInputUncheckedLocked2 = startInputUncheckedLocked(clientState, iInputContext, i5, editorInfo, i2, i);
                                i8 = 1;
                            } else {
                                inputBindResultStartInputUncheckedLocked2 = null;
                            }
                            showCurrentInputLocked(i8, null);
                        }
                        break;
                    default:
                        inputBindResultStartInputUncheckedLocked2 = null;
                        break;
                }
                if (!z) {
                    inputBindResultStartInputUncheckedLocked2 = editorInfo != null ? (DebugFlags.FLAG_OPTIMIZE_START_INPUT.value() && i9 == 0) ? InputBindResult.NO_EDITOR : startInputUncheckedLocked(clientState, iInputContext, i5, editorInfo, i2, i) : InputBindResult.NULL_EDITOR_INFO;
                }
                return inputBindResultStartInputUncheckedLocked2;
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean canShowInputMethodPickerLocked(IInputMethodClient iInputMethodClient) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) == 1000) {
            return true;
        }
        if (this.mCurFocusedWindowClient == null || iInputMethodClient == null || this.mCurFocusedWindowClient.client.asBinder() != iInputMethodClient.asBinder()) {
            return (this.mCurIntent != null && InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, callingUid, this.mCurIntent.getComponent().getPackageName())) || this.mContext.checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") == 0;
        }
        return true;
    }

    public void showInputMethodPickerFromClient(IInputMethodClient iInputMethodClient, int i) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (!canShowInputMethodPickerLocked(iInputMethodClient)) {
                Slog.w(TAG, "Ignoring showInputMethodPickerFromClient of uid " + Binder.getCallingUid() + ": " + iInputMethodClient);
                return;
            }
            this.mHandler.sendMessage(this.mCaller.obtainMessageI(1, i));
        }
    }

    public boolean isInputMethodPickerShownForTest() {
        synchronized (this.mMethodMap) {
            if (this.mSwitchingDialog == null) {
                return false;
            }
            return this.mSwitchingDialog.isShowing();
        }
    }

    public void setInputMethod(IBinder iBinder, String str) {
        if (!calledFromValidUser()) {
            return;
        }
        setInputMethodWithSubtypeId(iBinder, str, -1);
    }

    public void setInputMethodAndSubtype(IBinder iBinder, String str, InputMethodSubtype inputMethodSubtype) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            try {
                if (inputMethodSubtype != null) {
                    setInputMethodWithSubtypeIdLocked(iBinder, str, InputMethodUtils.getSubtypeIdFromHashCode(this.mMethodMap.get(str), inputMethodSubtype.hashCode()));
                } else {
                    setInputMethod(iBinder, str);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient iInputMethodClient, String str) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(2, str));
        }
    }

    public boolean switchToPreviousInputMethod(IBinder iBinder) {
        InputMethodInfo inputMethodInfo;
        ArrayList enabledInputMethodListLocked;
        String locale;
        InputMethodSubtype inputMethodSubtypeFindLastResortApplicableSubtypeLocked;
        int iHashCode;
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            Pair lastInputMethodAndSubtypeLocked = this.mSettings.getLastInputMethodAndSubtypeLocked();
            String str = null;
            if (lastInputMethodAndSubtypeLocked != null) {
                inputMethodInfo = this.mMethodMap.get(lastInputMethodAndSubtypeLocked.first);
            } else {
                inputMethodInfo = null;
            }
            int subtypeIdFromHashCode = -1;
            if (lastInputMethodAndSubtypeLocked != null && inputMethodInfo != null) {
                boolean zEquals = inputMethodInfo.getId().equals(this.mCurMethodId);
                int i = Integer.parseInt((String) lastInputMethodAndSubtypeLocked.second);
                if (this.mCurrentSubtype != null) {
                    iHashCode = this.mCurrentSubtype.hashCode();
                } else {
                    iHashCode = -1;
                }
                if (!zEquals || i != iHashCode) {
                    str = (String) lastInputMethodAndSubtypeLocked.first;
                    subtypeIdFromHashCode = InputMethodUtils.getSubtypeIdFromHashCode(inputMethodInfo, i);
                }
            }
            if (TextUtils.isEmpty(str) && !InputMethodUtils.canAddToLastInputMethod(this.mCurrentSubtype) && (enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked()) != null) {
                int size = enabledInputMethodListLocked.size();
                if (this.mCurrentSubtype == null) {
                    locale = this.mRes.getConfiguration().locale.toString();
                } else {
                    locale = this.mCurrentSubtype.getLocale();
                }
                String str2 = str;
                int i2 = 0;
                while (true) {
                    if (i2 < size) {
                        InputMethodInfo inputMethodInfo2 = (InputMethodInfo) enabledInputMethodListLocked.get(i2);
                        if (inputMethodInfo2.getSubtypeCount() > 0 && InputMethodUtils.isSystemIme(inputMethodInfo2) && (inputMethodSubtypeFindLastResortApplicableSubtypeLocked = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, InputMethodUtils.getSubtypes(inputMethodInfo2), "keyboard", locale, true)) != null) {
                            String id = inputMethodInfo2.getId();
                            int subtypeIdFromHashCode2 = InputMethodUtils.getSubtypeIdFromHashCode(inputMethodInfo2, inputMethodSubtypeFindLastResortApplicableSubtypeLocked.hashCode());
                            if (!inputMethodSubtypeFindLastResortApplicableSubtypeLocked.getLocale().equals(locale)) {
                                str2 = id;
                                subtypeIdFromHashCode = subtypeIdFromHashCode2;
                            } else {
                                str = id;
                                subtypeIdFromHashCode = subtypeIdFromHashCode2;
                                break;
                            }
                        }
                        i2++;
                    } else {
                        str = str2;
                        break;
                    }
                }
            }
            if (TextUtils.isEmpty(str)) {
                return false;
            }
            setInputMethodWithSubtypeIdLocked(iBinder, str, subtypeIdFromHashCode);
            return true;
        }
    }

    public boolean switchToNextInputMethod(IBinder iBinder, boolean z) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (!calledWithValidToken(iBinder)) {
                return false;
            }
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextInputMethodLocked = this.mSwitchingController.getNextInputMethodLocked(z, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, true);
            if (nextInputMethodLocked == null) {
                return false;
            }
            setInputMethodWithSubtypeIdLocked(iBinder, nextInputMethodLocked.mImi.getId(), nextInputMethodLocked.mSubtypeId);
            return true;
        }
    }

    public boolean shouldOfferSwitchingToNextInputMethod(IBinder iBinder) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(iBinder)) {
                return this.mSwitchingController.getNextInputMethodLocked(false, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, true) != null;
            }
            return false;
        }
    }

    public InputMethodSubtype getLastInputMethodSubtype() {
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mMethodMap) {
            Pair lastInputMethodAndSubtypeLocked = this.mSettings.getLastInputMethodAndSubtypeLocked();
            if (lastInputMethodAndSubtypeLocked != null && !TextUtils.isEmpty((CharSequence) lastInputMethodAndSubtypeLocked.first) && !TextUtils.isEmpty((CharSequence) lastInputMethodAndSubtypeLocked.second)) {
                InputMethodInfo inputMethodInfo = this.mMethodMap.get(lastInputMethodAndSubtypeLocked.first);
                if (inputMethodInfo == null) {
                    return null;
                }
                try {
                    int subtypeIdFromHashCode = InputMethodUtils.getSubtypeIdFromHashCode(inputMethodInfo, Integer.parseInt((String) lastInputMethodAndSubtypeLocked.second));
                    if (subtypeIdFromHashCode >= 0 && subtypeIdFromHashCode < inputMethodInfo.getSubtypeCount()) {
                        return inputMethodInfo.getSubtypeAt(subtypeIdFromHashCode);
                    }
                    return null;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }

    public void setAdditionalInputMethodSubtypes(String str, InputMethodSubtype[] inputMethodSubtypeArr) {
        if (!calledFromValidUser() || TextUtils.isEmpty(str) || inputMethodSubtypeArr == null) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (this.mSystemReady) {
                InputMethodInfo inputMethodInfo = this.mMethodMap.get(str);
                if (inputMethodInfo == null) {
                    return;
                }
                try {
                    String[] packagesForUid = this.mIPackageManager.getPackagesForUid(Binder.getCallingUid());
                    if (packagesForUid != null) {
                        for (String str2 : packagesForUid) {
                            if (str2.equals(inputMethodInfo.getPackageName())) {
                                this.mFileManager.addInputMethodSubtypes(inputMethodInfo, inputMethodSubtypeArr);
                                long jClearCallingIdentity = Binder.clearCallingIdentity();
                                try {
                                    buildInputMethodListLocked(false);
                                    return;
                                } finally {
                                    Binder.restoreCallingIdentity(jClearCallingIdentity);
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

    public int getInputMethodWindowVisibleHeight() {
        return this.mWindowManagerInternal.getInputMethodWindowVisibleHeight();
    }

    public void clearLastInputMethodWindowForTransition(IBinder iBinder) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(iBinder)) {
                this.mWindowManagerInternal.clearLastInputMethodWindowForTransition();
            }
        }
    }

    public void notifyUserAction(int i) {
        synchronized (this.mMethodMap) {
            if (this.mCurUserActionNotificationSequenceNumber != i) {
                return;
            }
            InputMethodInfo inputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
            if (inputMethodInfo != null) {
                this.mSwitchingController.onUserActionLocked(inputMethodInfo, this.mCurrentSubtype);
            }
        }
    }

    private void setInputMethodWithSubtypeId(IBinder iBinder, String str, int i) {
        synchronized (this.mMethodMap) {
            setInputMethodWithSubtypeIdLocked(iBinder, str, i);
        }
    }

    private void setInputMethodWithSubtypeIdLocked(IBinder iBinder, String str, int i) {
        if (iBinder == null) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Using null token requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
        } else if (this.mCurToken != iBinder) {
            Slog.w(TAG, "Ignoring setInputMethod of uid " + Binder.getCallingUid() + " token: " + iBinder);
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            setInputMethodLocked(str, i);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void hideMySoftInput(IBinder iBinder, int i) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(iBinder)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    hideCurrentInputLocked(i, null);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    public void showMySoftInput(IBinder iBinder, int i) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(iBinder)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    showCurrentInputLocked(i, null);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    void setEnabledSessionInMainThread(SessionState sessionState) {
        if (this.mEnabledSession != sessionState) {
            if (this.mEnabledSession != null && this.mEnabledSession.session != null) {
                try {
                    this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, false);
                } catch (RemoteException e) {
                }
            }
            this.mEnabledSession = sessionState;
            if (this.mEnabledSession != null && this.mEnabledSession.session != null) {
                try {
                    this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, true);
                } catch (RemoteException e2) {
                }
            }
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 1:
                switch (message.arg1) {
                    case 0:
                        z = this.mInputShown;
                        break;
                    case 1:
                        z = true;
                        break;
                    case 2:
                        break;
                    default:
                        Slog.e(TAG, "Unknown subtype picker mode = " + message.arg1);
                        return false;
                }
                showInputMethodMenu(z);
                return true;
            case 2:
                showInputMethodAndSubtypeEnabler((String) message.obj);
                return true;
            case 3:
                showConfigureInputMethods();
                return true;
            case 1000:
                try {
                    ((IInputMethod) message.obj).unbindInput();
                    break;
                } catch (RemoteException e) {
                }
                return true;
            case MSG_BIND_INPUT:
                SomeArgs someArgs = (SomeArgs) message.obj;
                try {
                    ((IInputMethod) someArgs.arg1).bindInput((InputBinding) someArgs.arg2);
                    break;
                } catch (RemoteException e2) {
                }
                someArgs.recycle();
                return true;
            case MSG_SHOW_SOFT_INPUT:
                SomeArgs someArgs2 = (SomeArgs) message.obj;
                try {
                    ((IInputMethod) someArgs2.arg1).showSoftInput(message.arg1, (ResultReceiver) someArgs2.arg2);
                    break;
                } catch (RemoteException e3) {
                }
                someArgs2.recycle();
                return true;
            case MSG_HIDE_SOFT_INPUT:
                SomeArgs someArgs3 = (SomeArgs) message.obj;
                try {
                    ((IInputMethod) someArgs3.arg1).hideSoftInput(0, (ResultReceiver) someArgs3.arg2);
                    break;
                } catch (RemoteException e4) {
                }
                someArgs3.recycle();
                return true;
            case MSG_HIDE_CURRENT_INPUT_METHOD:
                synchronized (this.mMethodMap) {
                    hideCurrentInputLocked(0, null);
                    break;
                }
                return true;
            case MSG_ATTACH_TOKEN:
                SomeArgs someArgs4 = (SomeArgs) message.obj;
                try {
                    ((IInputMethod) someArgs4.arg1).attachToken((IBinder) someArgs4.arg2);
                    break;
                } catch (RemoteException e5) {
                }
                someArgs4.recycle();
                return true;
            case MSG_CREATE_SESSION:
                SomeArgs someArgs5 = (SomeArgs) message.obj;
                IInputMethod iInputMethod = (IInputMethod) someArgs5.arg1;
                InputChannel inputChannel = (InputChannel) someArgs5.arg2;
                try {
                    iInputMethod.createSession(inputChannel, (IInputSessionCallback) someArgs5.arg3);
                    break;
                } catch (RemoteException e6) {
                    if (inputChannel != null && Binder.isProxy(iInputMethod)) {
                    }
                } catch (Throwable th) {
                    if (inputChannel != null && Binder.isProxy(iInputMethod)) {
                        inputChannel.dispose();
                    }
                    throw th;
                }
                if (inputChannel != null && Binder.isProxy(iInputMethod)) {
                    inputChannel.dispose();
                }
                someArgs5.recycle();
                return true;
            case 2000:
                int i = message.arg1;
                boolean z = message.arg2 != 0;
                SomeArgs someArgs6 = (SomeArgs) message.obj;
                IBinder iBinder = (IBinder) someArgs6.arg1;
                SessionState sessionState = (SessionState) someArgs6.arg2;
                IInputContext iInputContext = (IInputContext) someArgs6.arg3;
                EditorInfo editorInfo = (EditorInfo) someArgs6.arg4;
                try {
                    setEnabledSessionInMainThread(sessionState);
                    sessionState.method.startInput(iBinder, iInputContext, i, editorInfo, z);
                    break;
                } catch (RemoteException e7) {
                }
                someArgs6.recycle();
                return true;
            case MSG_START_VR_INPUT:
                startVrInputMethodNoCheck((ComponentName) message.obj);
                return true;
            case MSG_UNBIND_CLIENT:
                try {
                    ((IInputMethodClient) message.obj).onUnbindMethod(message.arg1, message.arg2);
                    break;
                } catch (RemoteException e8) {
                }
                return true;
            case 3010:
                SomeArgs someArgs7 = (SomeArgs) message.obj;
                ?? IsProxy = (IInputMethodClient) someArgs7.arg1;
                InputBindResult inputBindResult = (InputBindResult) someArgs7.arg2;
                try {
                    try {
                        IsProxy.onBindMethod(inputBindResult);
                    } finally {
                        if (inputBindResult.channel != null && Binder.isProxy(IsProxy)) {
                            inputBindResult.channel.dispose();
                        }
                    }
                } catch (RemoteException e9) {
                    Slog.w(TAG, "Client died receiving input method " + someArgs7.arg2);
                    IsProxy = IsProxy;
                    if (inputBindResult.channel != null) {
                        boolean zIsProxy = Binder.isProxy(IsProxy);
                        IsProxy = zIsProxy;
                        if (zIsProxy) {
                        }
                    }
                }
                someArgs7.recycle();
                return true;
            case MSG_SET_ACTIVE:
                try {
                    ((ClientState) message.obj).client.setActive(message.arg1 != 0, message.arg2 != 0);
                    break;
                } catch (RemoteException e10) {
                    Slog.w(TAG, "Got RemoteException sending setActive(false) notification to pid " + ((ClientState) message.obj).pid + " uid " + ((ClientState) message.obj).uid);
                }
                return true;
            case MSG_SET_INTERACTIVE:
                handleSetInteractive(message.arg1 != 0);
                return true;
            case 3040:
                int i2 = message.arg1;
                ClientState clientState = (ClientState) message.obj;
                try {
                    clientState.client.setUserActionNotificationSequenceNumber(i2);
                    break;
                } catch (RemoteException e11) {
                    Slog.w(TAG, "Got RemoteException sending setUserActionNotificationSequenceNumber(" + i2 + ") notification to pid " + clientState.pid + " uid " + clientState.uid);
                }
                return true;
            case MSG_REPORT_FULLSCREEN_MODE:
                z = message.arg1 != 0;
                ClientState clientState2 = (ClientState) message.obj;
                try {
                    clientState2.client.reportFullscreenMode(z);
                    break;
                } catch (RemoteException e12) {
                    Slog.w(TAG, "Got RemoteException sending reportFullscreen(" + z + ") notification to pid=" + clientState2.pid + " uid=" + clientState2.uid);
                }
                return true;
            case 3050:
                handleSwitchInputMethod(message.arg1 != 0);
                return true;
            case MSG_HARD_KEYBOARD_SWITCH_CHANGED:
                this.mHardKeyboardListener.handleHardKeyboardStatusChange(message.arg1 == 1);
                return true;
            case MSG_SYSTEM_UNLOCK_USER:
                onUnlockUser(message.arg1);
                return true;
            default:
                return false;
        }
    }

    private void handleSetInteractive(boolean z) {
        synchronized (this.mMethodMap) {
            this.mIsInteractive = z;
            updateSystemUiLocked(this.mCurToken, z ? this.mImeWindowVis : 0, this.mBackDisposition);
            if (this.mCurClient != null && this.mCurClient.client != null) {
                executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_SET_ACTIVE, this.mIsInteractive ? 1 : 0, this.mInFullscreenMode ? 1 : 0, this.mCurClient));
            }
        }
    }

    private void handleSwitchInputMethod(boolean z) {
        synchronized (this.mMethodMap) {
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem nextInputMethodLocked = this.mSwitchingController.getNextInputMethodLocked(false, this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, z);
            if (nextInputMethodLocked == null) {
                return;
            }
            setInputMethodLocked(nextInputMethodLocked.mImi.getId(), nextInputMethodLocked.mSubtypeId);
            InputMethodInfo inputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
            if (inputMethodInfo == null) {
                return;
            }
            CharSequence imeAndSubtypeDisplayName = InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, inputMethodInfo, this.mCurrentSubtype);
            if (!TextUtils.isEmpty(imeAndSubtypeDisplayName)) {
                if (this.mSubtypeSwitchedByShortCutToast == null) {
                    this.mSubtypeSwitchedByShortCutToast = Toast.makeText(this.mContext, imeAndSubtypeDisplayName, 0);
                } else {
                    this.mSubtypeSwitchedByShortCutToast.setText(imeAndSubtypeDisplayName);
                }
                this.mSubtypeSwitchedByShortCutToast.show();
            }
        }
    }

    private boolean chooseNewDefaultIMELocked() {
        InputMethodInfo mostApplicableDefaultIME = InputMethodUtils.getMostApplicableDefaultIME(this.mSettings.getEnabledInputMethodListLocked());
        if (mostApplicableDefaultIME != null) {
            resetSelectedInputMethodAndSubtypeLocked(mostApplicableDefaultIME.getId());
            return true;
        }
        return false;
    }

    private int getComponentMatchingFlags(int i) {
        synchronized (this.mMethodMap) {
            if (this.mBindInstantServiceAllowed) {
                i |= DumpState.DUMP_VOLUMES;
            }
        }
        return i;
    }

    @GuardedBy("mMethodMap")
    void buildInputMethodListLocked(boolean z) {
        boolean z2;
        boolean z3;
        if (!this.mSystemReady) {
            Slog.e(TAG, "buildInputMethodListLocked is not allowed until system is ready");
            return;
        }
        this.mMethodList.clear();
        this.mMethodMap.clear();
        this.mMethodMapUpdateCount++;
        this.mMyPackageMonitor.clearKnownImePackageNamesLocked();
        PackageManager packageManager = this.mContext.getPackageManager();
        List listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(new Intent("android.view.InputMethod"), getComponentMatchingFlags(32896), this.mSettings.getCurrentUserId());
        HashMap<String, List<InputMethodSubtype>> allAdditionalInputMethodSubtypes = this.mFileManager.getAllAdditionalInputMethodSubtypes();
        for (int i = 0; i < listQueryIntentServicesAsUser.size(); i++) {
            ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentServicesAsUser.get(i);
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            String strComputeId = InputMethodInfo.computeId(resolveInfo);
            if (!"android.permission.BIND_INPUT_METHOD".equals(serviceInfo.permission)) {
                Slog.w(TAG, "Skipping input method " + strComputeId + ": it does not require the permission android.permission.BIND_INPUT_METHOD");
            } else {
                try {
                    InputMethodInfo inputMethodInfo = new InputMethodInfo(this.mContext, resolveInfo, allAdditionalInputMethodSubtypes.get(strComputeId));
                    this.mMethodList.add(inputMethodInfo);
                    this.mMethodMap.put(inputMethodInfo.getId(), inputMethodInfo);
                } catch (Exception e) {
                    Slog.wtf(TAG, "Unable to load input method " + strComputeId, e);
                }
            }
        }
        List listQueryIntentServicesAsUser2 = packageManager.queryIntentServicesAsUser(new Intent("android.view.InputMethod"), getComponentMatchingFlags(512), this.mSettings.getCurrentUserId());
        int size = listQueryIntentServicesAsUser2.size();
        for (int i2 = 0; i2 < size; i2++) {
            ServiceInfo serviceInfo2 = ((ResolveInfo) listQueryIntentServicesAsUser2.get(i2)).serviceInfo;
            if ("android.permission.BIND_INPUT_METHOD".equals(serviceInfo2.permission)) {
                this.mMyPackageMonitor.addKnownImePackageNameLocked(serviceInfo2.packageName);
            }
        }
        if (z) {
            z2 = false;
        } else {
            ArrayList enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
            int size2 = enabledInputMethodListLocked.size();
            int i3 = 0;
            boolean z4 = false;
            while (true) {
                if (i3 < size2) {
                    InputMethodInfo inputMethodInfo2 = (InputMethodInfo) enabledInputMethodListLocked.get(i3);
                    if (this.mMethodList.contains(inputMethodInfo2)) {
                        if (inputMethodInfo2.isAuxiliaryIme()) {
                            z4 = true;
                        } else {
                            z3 = true;
                            z4 = true;
                            break;
                        }
                    }
                    i3++;
                } else {
                    z3 = false;
                    break;
                }
            }
            if (!z4) {
                resetSelectedInputMethodAndSubtypeLocked(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                z = true;
            } else if (!z3) {
                z2 = true;
            }
            z2 = false;
        }
        if (z || z2) {
            ArrayList defaultEnabledImes = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mMethodList, z2);
            int size3 = defaultEnabledImes.size();
            for (int i4 = 0; i4 < size3; i4++) {
                setInputMethodEnabledLocked(((InputMethodInfo) defaultEnabledImes.get(i4)).getId(), true);
            }
        }
        String selectedInputMethod = this.mSettings.getSelectedInputMethod();
        if (!TextUtils.isEmpty(selectedInputMethod)) {
            if (!this.mMethodMap.containsKey(selectedInputMethod)) {
                Slog.w(TAG, "Default IME is uninstalled. Choose new default IME.");
                if (chooseNewDefaultIMELocked()) {
                    updateInputMethodsFromSettingsLocked(true);
                }
            } else {
                setInputMethodEnabledLocked(selectedInputMethod, true);
            }
        }
        this.mSwitchingController.resetCircularListLocked(this.mContext);
    }

    private void showInputMethodAndSubtypeEnabler(String str) {
        int currentUserId;
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
        intent.setFlags(337641472);
        if (!TextUtils.isEmpty(str)) {
            intent.putExtra("input_method_id", str);
        }
        synchronized (this.mMethodMap) {
            currentUserId = this.mSettings.getCurrentUserId();
        }
        this.mContext.startActivityAsUser(intent, null, UserHandle.of(currentUserId));
    }

    private void showConfigureInputMethods() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent("android.settings.INPUT_METHOD_SETTINGS");
        intent.setFlags(337641472);
        this.mContext.startActivityAsUser(intent, null, UserHandle.CURRENT);
    }

    private boolean isScreenLocked() {
        return this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardLocked() && this.mKeyguardManager.isKeyguardSecure();
    }

    private void showInputMethodMenu(boolean z) {
        int i;
        InputMethodSubtype currentInputMethodSubtypeLocked;
        boolean zIsScreenLocked = isScreenLocked();
        String selectedInputMethod = this.mSettings.getSelectedInputMethod();
        int selectedInputMethodSubtypeId = this.mSettings.getSelectedInputMethodSubtypeId(selectedInputMethod);
        synchronized (this.mMethodMap) {
            HashMap explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked = this.mSettings.getExplicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked(this.mContext);
            if (explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked != null && explicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked.size() != 0) {
                hideInputMethodMenuLocked();
                List sortedInputMethodAndSubtypeListLocked = this.mSwitchingController.getSortedInputMethodAndSubtypeListLocked(z, zIsScreenLocked);
                if (selectedInputMethodSubtypeId == -1 && (currentInputMethodSubtypeLocked = getCurrentInputMethodSubtypeLocked()) != null) {
                    selectedInputMethodSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(this.mMethodMap.get(this.mCurMethodId), currentInputMethodSubtypeLocked.hashCode());
                }
                int size = sortedInputMethodAndSubtypeListLocked.size();
                this.mIms = new InputMethodInfo[size];
                this.mSubtypeIds = new int[size];
                int i2 = 0;
                int i3 = 0;
                for (int i4 = 0; i4 < size; i4++) {
                    InputMethodSubtypeSwitchingController.ImeSubtypeListItem imeSubtypeListItem = (InputMethodSubtypeSwitchingController.ImeSubtypeListItem) sortedInputMethodAndSubtypeListLocked.get(i4);
                    this.mIms[i4] = imeSubtypeListItem.mImi;
                    this.mSubtypeIds[i4] = imeSubtypeListItem.mSubtypeId;
                    if (this.mIms[i4].getId().equals(selectedInputMethod) && ((i = this.mSubtypeIds[i4]) == -1 || ((selectedInputMethodSubtypeId == -1 && i == 0) || i == selectedInputMethodSubtypeId))) {
                        i3 = i4;
                    }
                }
                this.mDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper((Context) ActivityThread.currentActivityThread().getSystemUiContext(), R.style.Theme.DeviceDefault.Settings));
                this.mDialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        InputMethodManagerService.this.hideInputMethodMenu();
                    }
                });
                Context context = this.mDialogBuilder.getContext();
                TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(null, com.android.internal.R.styleable.DialogPreference, R.attr.alertDialogStyle, 0);
                Drawable drawable = typedArrayObtainStyledAttributes.getDrawable(2);
                typedArrayObtainStyledAttributes.recycle();
                this.mDialogBuilder.setIcon(drawable);
                View viewInflate = ((LayoutInflater) context.getSystemService(LayoutInflater.class)).inflate(R.layout.dialog_custom_title, (ViewGroup) null);
                this.mDialogBuilder.setCustomTitle(viewInflate);
                this.mSwitchingDialogTitleView = viewInflate;
                View viewFindViewById = this.mSwitchingDialogTitleView.findViewById(R.id.clock);
                if (!this.mWindowManagerInternal.isHardKeyboardAvailable()) {
                    i2 = 8;
                }
                viewFindViewById.setVisibility(i2);
                Switch r1 = (Switch) this.mSwitchingDialogTitleView.findViewById(R.id.close_button);
                r1.setChecked(this.mShowImeWithHardKeyboard);
                r1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean z2) {
                        InputMethodManagerService.this.mSettings.setShowImeWithHardKeyboard(z2);
                        InputMethodManagerService.this.hideInputMethodMenu();
                    }
                });
                final ImeSubtypeListAdapter imeSubtypeListAdapter = new ImeSubtypeListAdapter(context, R.layout.dialog_custom_title_holo, sortedInputMethodAndSubtypeListLocked, i3);
                this.mDialogBuilder.setSingleChoiceItems(imeSubtypeListAdapter, i3, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i5) {
                        synchronized (InputMethodManagerService.this.mMethodMap) {
                            if (InputMethodManagerService.this.mIms != null && InputMethodManagerService.this.mIms.length > i5 && InputMethodManagerService.this.mSubtypeIds != null && InputMethodManagerService.this.mSubtypeIds.length > i5) {
                                InputMethodInfo inputMethodInfo = InputMethodManagerService.this.mIms[i5];
                                int i6 = InputMethodManagerService.this.mSubtypeIds[i5];
                                imeSubtypeListAdapter.mCheckedItem = i5;
                                imeSubtypeListAdapter.notifyDataSetChanged();
                                InputMethodManagerService.this.hideInputMethodMenu();
                                if (inputMethodInfo != null) {
                                    if (i6 < 0 || i6 >= inputMethodInfo.getSubtypeCount()) {
                                        i6 = -1;
                                    }
                                    InputMethodManagerService.this.setInputMethodLocked(inputMethodInfo.getId(), i6);
                                }
                            }
                        }
                    }
                });
                this.mSwitchingDialog = this.mDialogBuilder.create();
                this.mSwitchingDialog.setCanceledOnTouchOutside(true);
                Window window = this.mSwitchingDialog.getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                window.setType(2012);
                attributes.token = this.mSwitchingDialogToken;
                attributes.privateFlags |= 16;
                attributes.setTitle("Select input method");
                window.setAttributes(attributes);
                updateSystemUi(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                this.mSwitchingDialog.show();
            }
        }
    }

    private static class ImeSubtypeListAdapter extends ArrayAdapter<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> {
        public int mCheckedItem;
        private final LayoutInflater mInflater;
        private final List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> mItemsList;
        private final int mTextViewResourceId;

        public ImeSubtypeListAdapter(Context context, int i, List<InputMethodSubtypeSwitchingController.ImeSubtypeListItem> list, int i2) {
            super(context, i, list);
            this.mTextViewResourceId = i;
            this.mItemsList = list;
            this.mCheckedItem = i2;
            this.mInflater = (LayoutInflater) context.getSystemService(LayoutInflater.class);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = this.mInflater.inflate(this.mTextViewResourceId, (ViewGroup) null);
            }
            if (i < 0 || i >= this.mItemsList.size()) {
                return view;
            }
            InputMethodSubtypeSwitchingController.ImeSubtypeListItem imeSubtypeListItem = this.mItemsList.get(i);
            CharSequence charSequence = imeSubtypeListItem.mImeName;
            CharSequence charSequence2 = imeSubtypeListItem.mSubtypeName;
            TextView textView = (TextView) view.findViewById(R.id.text1);
            TextView textView2 = (TextView) view.findViewById(R.id.text2);
            if (TextUtils.isEmpty(charSequence2)) {
                textView.setText(charSequence);
                textView2.setVisibility(8);
            } else {
                textView.setText(charSequence2);
                textView2.setText(charSequence);
                textView2.setVisibility(0);
            }
            ((RadioButton) view.findViewById(R.id.intoExisting)).setChecked(i == this.mCheckedItem);
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
            this.mSwitchingDialogTitleView = null;
        }
        updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
        this.mDialogBuilder = null;
        this.mIms = null;
    }

    boolean setInputMethodEnabledLocked(String str, boolean z) {
        if (this.mMethodMap.get(str) == null) {
            throw new IllegalArgumentException("Unknown id: " + this.mCurMethodId);
        }
        List enabledInputMethodsAndSubtypeListLocked = this.mSettings.getEnabledInputMethodsAndSubtypeListLocked();
        if (z) {
            Iterator it = enabledInputMethodsAndSubtypeListLocked.iterator();
            while (it.hasNext()) {
                if (((String) ((Pair) it.next()).first).equals(str)) {
                    return true;
                }
            }
            this.mSettings.appendAndPutEnabledInputMethodLocked(str, false);
            return false;
        }
        if (!this.mSettings.buildAndPutEnabledInputMethodsStrRemovingIdLocked(new StringBuilder(), enabledInputMethodsAndSubtypeListLocked, str)) {
            return false;
        }
        if (str.equals(this.mSettings.getSelectedInputMethod()) && !chooseNewDefaultIMELocked()) {
            Slog.i(TAG, "Can't find new IME, unsetting the current input method.");
            resetSelectedInputMethodAndSubtypeLocked(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        return true;
    }

    private void setSelectedInputMethodAndSubtypeLocked(InputMethodInfo inputMethodInfo, int i, boolean z) {
        boolean z2;
        if (inputMethodInfo == null || !inputMethodInfo.isVrOnly()) {
            z2 = false;
        } else {
            z2 = true;
        }
        if (!z2) {
            this.mSettings.saveCurrentInputMethodAndSubtypeToHistory(this.mCurMethodId, this.mCurrentSubtype);
        }
        this.mCurUserActionNotificationSequenceNumber = Math.max(this.mCurUserActionNotificationSequenceNumber + 1, 1);
        if (this.mCurClient != null && this.mCurClient.client != null) {
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(3040, this.mCurUserActionNotificationSequenceNumber, this.mCurClient));
        }
        if (z2) {
            return;
        }
        if (inputMethodInfo == null || i < 0) {
            this.mSettings.putSelectedSubtype(-1);
            this.mCurrentSubtype = null;
        } else if (i >= inputMethodInfo.getSubtypeCount()) {
            this.mSettings.putSelectedSubtype(-1);
            this.mCurrentSubtype = getCurrentInputMethodSubtypeLocked();
        } else {
            InputMethodSubtype subtypeAt = inputMethodInfo.getSubtypeAt(i);
            this.mSettings.putSelectedSubtype(subtypeAt.hashCode());
            this.mCurrentSubtype = subtypeAt;
        }
        if (!z) {
            this.mSettings.putSelectedInputMethod(inputMethodInfo != null ? inputMethodInfo.getId() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
    }

    private void resetSelectedInputMethodAndSubtypeLocked(String str) {
        int subtypeIdFromHashCode;
        String lastSubtypeForInputMethodLocked;
        InputMethodInfo inputMethodInfo = this.mMethodMap.get(str);
        if (inputMethodInfo != null && !TextUtils.isEmpty(str) && (lastSubtypeForInputMethodLocked = this.mSettings.getLastSubtypeForInputMethodLocked(str)) != null) {
            try {
                subtypeIdFromHashCode = InputMethodUtils.getSubtypeIdFromHashCode(inputMethodInfo, Integer.parseInt(lastSubtypeForInputMethodLocked));
            } catch (NumberFormatException e) {
                Slog.w(TAG, "HashCode for subtype looks broken: " + lastSubtypeForInputMethodLocked, e);
                subtypeIdFromHashCode = -1;
            }
        } else {
            subtypeIdFromHashCode = -1;
        }
        setSelectedInputMethodAndSubtypeLocked(inputMethodInfo, subtypeIdFromHashCode, false);
    }

    private Pair<InputMethodInfo, InputMethodSubtype> findLastResortApplicableShortcutInputMethodAndSubtypeLocked(String str) {
        InputMethodSubtype inputMethodSubtypeFindLastResortApplicableSubtypeLocked;
        InputMethodSubtype inputMethodSubtypeFindLastResortApplicableSubtypeLocked2;
        Iterator it = this.mSettings.getEnabledInputMethodListLocked().iterator();
        boolean z = false;
        InputMethodInfo inputMethodInfo = null;
        InputMethodSubtype inputMethodSubtype = null;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            InputMethodInfo inputMethodInfo2 = (InputMethodInfo) it.next();
            String id = inputMethodInfo2.getId();
            if (!z || id.equals(this.mCurMethodId)) {
                List enabledInputMethodSubtypeListLocked = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, inputMethodInfo2, true);
                if (this.mCurrentSubtype != null) {
                    inputMethodSubtypeFindLastResortApplicableSubtypeLocked = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledInputMethodSubtypeListLocked, str, this.mCurrentSubtype.getLocale(), false);
                } else {
                    inputMethodSubtypeFindLastResortApplicableSubtypeLocked = null;
                }
                if (inputMethodSubtypeFindLastResortApplicableSubtypeLocked == null) {
                    inputMethodSubtypeFindLastResortApplicableSubtypeLocked = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledInputMethodSubtypeListLocked, str, (String) null, true);
                }
                ArrayList overridingImplicitlyEnabledSubtypes = InputMethodUtils.getOverridingImplicitlyEnabledSubtypes(inputMethodInfo2, str);
                if (overridingImplicitlyEnabledSubtypes.isEmpty()) {
                    overridingImplicitlyEnabledSubtypes = InputMethodUtils.getSubtypes(inputMethodInfo2);
                }
                if (inputMethodSubtypeFindLastResortApplicableSubtypeLocked == null && this.mCurrentSubtype != null) {
                    inputMethodSubtypeFindLastResortApplicableSubtypeLocked = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, overridingImplicitlyEnabledSubtypes, str, this.mCurrentSubtype.getLocale(), false);
                }
                if (inputMethodSubtypeFindLastResortApplicableSubtypeLocked == null) {
                    inputMethodSubtypeFindLastResortApplicableSubtypeLocked2 = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, overridingImplicitlyEnabledSubtypes, str, (String) null, true);
                } else {
                    inputMethodSubtypeFindLastResortApplicableSubtypeLocked2 = inputMethodSubtypeFindLastResortApplicableSubtypeLocked;
                }
                if (inputMethodSubtypeFindLastResortApplicableSubtypeLocked2 == null) {
                    continue;
                } else if (!id.equals(this.mCurMethodId)) {
                    if (!z) {
                        if ((inputMethodInfo2.getServiceInfo().applicationInfo.flags & 1) != 0) {
                            inputMethodInfo = inputMethodInfo2;
                            inputMethodSubtype = inputMethodSubtypeFindLastResortApplicableSubtypeLocked2;
                            z = true;
                        } else {
                            inputMethodInfo = inputMethodInfo2;
                            inputMethodSubtype = inputMethodSubtypeFindLastResortApplicableSubtypeLocked2;
                        }
                    }
                } else {
                    inputMethodInfo = inputMethodInfo2;
                    inputMethodSubtype = inputMethodSubtypeFindLastResortApplicableSubtypeLocked2;
                    break;
                }
            }
        }
        if (inputMethodInfo != null) {
            return new Pair<>(inputMethodInfo, inputMethodSubtype);
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
        boolean zIsSubtypeSelected = this.mSettings.isSubtypeSelected();
        InputMethodInfo inputMethodInfo = this.mMethodMap.get(this.mCurMethodId);
        if (inputMethodInfo == null || inputMethodInfo.getSubtypeCount() == 0) {
            return null;
        }
        if (!zIsSubtypeSelected || this.mCurrentSubtype == null || !InputMethodUtils.isValidSubtypeId(inputMethodInfo, this.mCurrentSubtype.hashCode())) {
            int selectedInputMethodSubtypeId = this.mSettings.getSelectedInputMethodSubtypeId(this.mCurMethodId);
            if (selectedInputMethodSubtypeId == -1) {
                List enabledInputMethodSubtypeListLocked = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, inputMethodInfo, true);
                if (enabledInputMethodSubtypeListLocked.size() != 1) {
                    if (enabledInputMethodSubtypeListLocked.size() > 1) {
                        this.mCurrentSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledInputMethodSubtypeListLocked, "keyboard", (String) null, true);
                        if (this.mCurrentSubtype == null) {
                            this.mCurrentSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledInputMethodSubtypeListLocked, (String) null, (String) null, true);
                        }
                    }
                } else {
                    this.mCurrentSubtype = (InputMethodSubtype) enabledInputMethodSubtypeListLocked.get(0);
                }
            } else {
                this.mCurrentSubtype = (InputMethodSubtype) InputMethodUtils.getSubtypes(inputMethodInfo).get(selectedInputMethodSubtypeId);
            }
        }
        return this.mCurrentSubtype;
    }

    public List getShortcutInputMethodsAndSubtypes() {
        synchronized (this.mMethodMap) {
            ArrayList arrayList = new ArrayList();
            if (this.mShortcutInputMethodsAndSubtypes.size() == 0) {
                Pair<InputMethodInfo, InputMethodSubtype> pairFindLastResortApplicableShortcutInputMethodAndSubtypeLocked = findLastResortApplicableShortcutInputMethodAndSubtypeLocked("voice");
                if (pairFindLastResortApplicableShortcutInputMethodAndSubtypeLocked != null) {
                    arrayList.add(pairFindLastResortApplicableShortcutInputMethodAndSubtypeLocked.first);
                    arrayList.add(pairFindLastResortApplicableShortcutInputMethodAndSubtypeLocked.second);
                }
                return arrayList;
            }
            for (InputMethodInfo inputMethodInfo : this.mShortcutInputMethodsAndSubtypes.keySet()) {
                arrayList.add(inputMethodInfo);
                Iterator<InputMethodSubtype> it = this.mShortcutInputMethodsAndSubtypes.get(inputMethodInfo).iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next());
                }
            }
            return arrayList;
        }
    }

    public boolean setCurrentInputMethodSubtype(InputMethodSubtype inputMethodSubtype) {
        int subtypeIdFromHashCode;
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (inputMethodSubtype != null) {
                try {
                    if (this.mCurMethodId != null && (subtypeIdFromHashCode = InputMethodUtils.getSubtypeIdFromHashCode(this.mMethodMap.get(this.mCurMethodId), inputMethodSubtype.hashCode())) != -1) {
                        setInputMethodLocked(this.mCurMethodId, subtypeIdFromHashCode);
                        return true;
                    }
                } catch (Throwable th) {
                    throw th;
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

        public InputMethodFileManager(HashMap<String, InputMethodInfo> map, int i) {
            File userSystemDirectory;
            if (map == null) {
                throw new NullPointerException("methodMap is null");
            }
            this.mMethodMap = map;
            if (i == 0) {
                userSystemDirectory = new File(Environment.getDataDirectory(), SYSTEM_PATH);
            } else {
                userSystemDirectory = Environment.getUserSystemDirectory(i);
            }
            File file = new File(userSystemDirectory, INPUT_METHOD_PATH);
            if (!file.exists() && !file.mkdirs()) {
                Slog.w(InputMethodManagerService.TAG, "Couldn't create dir.: " + file.getAbsolutePath());
            }
            File file2 = new File(file, ADDITIONAL_SUBTYPES_FILE_NAME);
            this.mAdditionalInputMethodSubtypeFile = new AtomicFile(file2, "input-subtypes");
            if (!file2.exists()) {
                writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, map);
            } else {
                readAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile);
            }
        }

        private void deleteAllInputMethodSubtypes(String str) {
            synchronized (this.mMethodMap) {
                this.mAdditionalSubtypesMap.remove(str);
                writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, this.mMethodMap);
            }
        }

        public void addInputMethodSubtypes(InputMethodInfo inputMethodInfo, InputMethodSubtype[] inputMethodSubtypeArr) {
            synchronized (this.mMethodMap) {
                ArrayList arrayList = new ArrayList();
                for (InputMethodSubtype inputMethodSubtype : inputMethodSubtypeArr) {
                    if (!arrayList.contains(inputMethodSubtype)) {
                        arrayList.add(inputMethodSubtype);
                    } else {
                        Slog.w(InputMethodManagerService.TAG, "Duplicated subtype definition found: " + inputMethodSubtype.getLocale() + ", " + inputMethodSubtype.getMode());
                    }
                }
                this.mAdditionalSubtypesMap.put(inputMethodInfo.getId(), arrayList);
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

        private static void writeAdditionalInputMethodSubtypes(HashMap<String, List<InputMethodSubtype>> map, AtomicFile atomicFile, HashMap<String, InputMethodInfo> map2) {
            FileOutputStream fileOutputStreamStartWrite;
            Object[] objArr = map2 != null && map2.size() > 0;
            try {
                fileOutputStreamStartWrite = atomicFile.startWrite();
            } catch (IOException e) {
                e = e;
                fileOutputStreamStartWrite = null;
            }
            try {
                FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                fastXmlSerializer.startTag(null, NODE_SUBTYPES);
                for (String str : map.keySet()) {
                    if (objArr == false || map2.containsKey(str)) {
                        fastXmlSerializer.startTag(null, NODE_IMI);
                        fastXmlSerializer.attribute(null, ATTR_ID, str);
                        List<InputMethodSubtype> list = map.get(str);
                        int size = list.size();
                        for (int i = 0; i < size; i++) {
                            InputMethodSubtype inputMethodSubtype = list.get(i);
                            fastXmlSerializer.startTag(null, NODE_SUBTYPE);
                            if (inputMethodSubtype.hasSubtypeId()) {
                                fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_ID, String.valueOf(inputMethodSubtype.getSubtypeId()));
                            }
                            fastXmlSerializer.attribute(null, ATTR_ICON, String.valueOf(inputMethodSubtype.getIconResId()));
                            fastXmlSerializer.attribute(null, ATTR_LABEL, String.valueOf(inputMethodSubtype.getNameResId()));
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_LOCALE, inputMethodSubtype.getLocale());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_LANGUAGE_TAG, inputMethodSubtype.getLanguageTag());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_MODE, inputMethodSubtype.getMode());
                            fastXmlSerializer.attribute(null, ATTR_IME_SUBTYPE_EXTRA_VALUE, inputMethodSubtype.getExtraValue());
                            fastXmlSerializer.attribute(null, ATTR_IS_AUXILIARY, String.valueOf(inputMethodSubtype.isAuxiliary() ? 1 : 0));
                            fastXmlSerializer.attribute(null, ATTR_IS_ASCII_CAPABLE, String.valueOf(inputMethodSubtype.isAsciiCapable() ? 1 : 0));
                            fastXmlSerializer.endTag(null, NODE_SUBTYPE);
                        }
                        fastXmlSerializer.endTag(null, NODE_IMI);
                    } else {
                        Slog.w(InputMethodManagerService.TAG, "IME uninstalled or not valid.: " + str);
                    }
                }
                fastXmlSerializer.endTag(null, NODE_SUBTYPES);
                fastXmlSerializer.endDocument();
                atomicFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e2) {
                e = e2;
                Slog.w(InputMethodManagerService.TAG, "Error writing subtypes", e);
                if (fileOutputStreamStartWrite != null) {
                    atomicFile.failWrite(fileOutputStreamStartWrite);
                }
            }
        }

        private static void readAdditionalInputMethodSubtypes(HashMap<String, List<InputMethodSubtype>> map, AtomicFile atomicFile) {
            int next;
            int i;
            int i2;
            if (map == null || atomicFile == null) {
                return;
            }
            map.clear();
            try {
                FileInputStream fileInputStreamOpenRead = atomicFile.openRead();
                Throwable th = null;
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    xmlPullParserNewPullParser.getEventType();
                    do {
                        next = xmlPullParserNewPullParser.next();
                        i = 1;
                        i2 = 2;
                        if (next == 2) {
                            break;
                        }
                    } while (next != 1);
                    if (!NODE_SUBTYPES.equals(xmlPullParserNewPullParser.getName())) {
                        throw new XmlPullParserException("Xml doesn't start with subtypes");
                    }
                    int depth = xmlPullParserNewPullParser.getDepth();
                    String attributeValue = null;
                    ArrayList arrayList = null;
                    while (true) {
                        int next2 = xmlPullParserNewPullParser.next();
                        if ((next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth) || next2 == i) {
                            break;
                        }
                        if (next2 == i2) {
                            String name = xmlPullParserNewPullParser.getName();
                            if (NODE_IMI.equals(name)) {
                                attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ID);
                                if (TextUtils.isEmpty(attributeValue)) {
                                    Slog.w(InputMethodManagerService.TAG, "Invalid imi id found in subtypes.xml");
                                } else {
                                    arrayList = new ArrayList();
                                    map.put(attributeValue, arrayList);
                                }
                            } else if (NODE_SUBTYPE.equals(name)) {
                                if (TextUtils.isEmpty(attributeValue) || arrayList == null) {
                                    Slog.w(InputMethodManagerService.TAG, "IME uninstalled or not valid.: " + attributeValue);
                                } else {
                                    int i3 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_ICON));
                                    int i4 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_LABEL));
                                    String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_IME_SUBTYPE_LOCALE);
                                    String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_IME_SUBTYPE_LANGUAGE_TAG);
                                    String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_IME_SUBTYPE_MODE);
                                    String attributeValue5 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_IME_SUBTYPE_EXTRA_VALUE);
                                    InputMethodSubtype.InputMethodSubtypeBuilder isAsciiCapable = new InputMethodSubtype.InputMethodSubtypeBuilder().setSubtypeNameResId(i4).setSubtypeIconResId(i3).setSubtypeLocale(attributeValue2).setLanguageTag(attributeValue3).setSubtypeMode(attributeValue4).setSubtypeExtraValue(attributeValue5).setIsAuxiliary("1".equals(String.valueOf(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_IS_AUXILIARY)))).setIsAsciiCapable("1".equals(String.valueOf(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_IS_ASCII_CAPABLE))));
                                    String attributeValue6 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_IME_SUBTYPE_ID);
                                    if (attributeValue6 != null) {
                                        isAsciiCapable.setSubtypeId(Integer.parseInt(attributeValue6));
                                    }
                                    arrayList.add(isAsciiCapable.build());
                                }
                            }
                        }
                        i = 1;
                        i2 = 2;
                    }
                    if (fileInputStreamOpenRead != null) {
                        fileInputStreamOpenRead.close();
                    }
                } catch (Throwable th2) {
                    if (fileInputStreamOpenRead == null) {
                        throw th2;
                    }
                    if (0 == 0) {
                        fileInputStreamOpenRead.close();
                        throw th2;
                    }
                    try {
                        fileInputStreamOpenRead.close();
                        throw th2;
                    } catch (Throwable th3) {
                        th.addSuppressed(th3);
                        throw th2;
                    }
                }
            } catch (IOException | NumberFormatException | XmlPullParserException e) {
                Slog.w(InputMethodManagerService.TAG, "Error reading subtypes", e);
            }
        }
    }

    private static final class LocalServiceImpl implements InputMethodManagerInternal {
        private final Handler mHandler;

        LocalServiceImpl(Handler handler) {
            this.mHandler = handler;
        }

        public void setInteractive(boolean z) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(InputMethodManagerService.MSG_SET_INTERACTIVE, z ? 1 : 0, 0));
        }

        public void switchInputMethod(boolean z) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3050, z ? 1 : 0, 0));
        }

        public void hideCurrentInputMethod() {
            this.mHandler.removeMessages(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD);
            this.mHandler.sendEmptyMessage(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD);
        }

        public void startVrInputMethodNoCheck(ComponentName componentName) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(InputMethodManagerService.MSG_START_VR_INPUT, componentName));
        }
    }

    private static String imeWindowStatusToString(int i) {
        boolean z;
        StringBuilder sb = new StringBuilder();
        if ((i & 1) != 0) {
            sb.append("Active");
            z = false;
        } else {
            z = true;
        }
        if ((i & 2) != 0) {
            if (!z) {
                sb.append("|");
            }
            sb.append("Visible");
        }
        return sb.toString();
    }

    public IInputContentUriToken createInputContentUriToken(IBinder iBinder, Uri uri, String str) {
        if (!calledFromValidUser()) {
            return null;
        }
        if (iBinder == null) {
            throw new NullPointerException("token");
        }
        if (str == null) {
            throw new NullPointerException(BackupManagerService.BACKUP_FINISHED_PACKAGE_EXTRA);
        }
        if (uri == null) {
            throw new NullPointerException("contentUri");
        }
        if (!"content".equals(uri.getScheme())) {
            throw new InvalidParameterException("contentUri must have content scheme");
        }
        synchronized (this.mMethodMap) {
            int callingUid = Binder.getCallingUid();
            if (this.mCurMethodId == null) {
                return null;
            }
            if (this.mCurToken != iBinder) {
                Slog.e(TAG, "Ignoring createInputContentUriToken mCurToken=" + this.mCurToken + " token=" + iBinder);
                return null;
            }
            if (!TextUtils.equals(this.mCurAttribute.packageName, str)) {
                Slog.e(TAG, "Ignoring createInputContentUriToken mCurAttribute.packageName=" + this.mCurAttribute.packageName + " packageName=" + str);
                return null;
            }
            int userId = UserHandle.getUserId(callingUid);
            return new InputContentUriTokenHandler(ContentProvider.getUriWithoutUserId(uri), callingUid, str, ContentProvider.getUserIdFromUri(uri, userId), UserHandle.getUserId(this.mCurClient.uid));
        }
    }

    public void reportFullscreenMode(IBinder iBinder, boolean z) {
        if (!calledFromValidUser()) {
            return;
        }
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(iBinder)) {
                if (this.mCurClient != null && this.mCurClient.client != null) {
                    this.mInFullscreenMode = z;
                    executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_REPORT_FULLSCREEN_MODE, z ? 1 : 0, this.mCurClient));
                }
            }
        }
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        ClientState clientState;
        ClientState clientState2;
        IInputMethod iInputMethod;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            PrintWriterPrinter printWriterPrinter = new PrintWriterPrinter(printWriter);
            synchronized (this.mMethodMap) {
                printWriterPrinter.println("Current Input Method Manager state:");
                int size = this.mMethodList.size();
                printWriterPrinter.println("  Input Methods: mMethodMapUpdateCount=" + this.mMethodMapUpdateCount + " mBindInstantServiceAllowed=" + this.mBindInstantServiceAllowed);
                for (int i = 0; i < size; i++) {
                    InputMethodInfo inputMethodInfo = this.mMethodList.get(i);
                    printWriterPrinter.println("  InputMethod #" + i + ":");
                    inputMethodInfo.dump(printWriterPrinter, "    ");
                }
                printWriterPrinter.println("  Clients:");
                for (ClientState clientState3 : this.mClients.values()) {
                    printWriterPrinter.println("  Client " + clientState3 + ":");
                    StringBuilder sb = new StringBuilder();
                    sb.append("    client=");
                    sb.append(clientState3.client);
                    printWriterPrinter.println(sb.toString());
                    printWriterPrinter.println("    inputContext=" + clientState3.inputContext);
                    printWriterPrinter.println("    sessionRequested=" + clientState3.sessionRequested);
                    printWriterPrinter.println("    curSession=" + clientState3.curSession);
                }
                printWriterPrinter.println("  mCurMethodId=" + this.mCurMethodId);
                clientState = this.mCurClient;
                printWriterPrinter.println("  mCurClient=" + clientState + " mCurSeq=" + this.mCurSeq);
                printWriterPrinter.println("  mCurFocusedWindow=" + this.mCurFocusedWindow + " softInputMode=" + InputMethodClient.softInputModeToString(this.mCurFocusedWindowSoftInputMode) + " client=" + this.mCurFocusedWindowClient);
                clientState2 = this.mCurFocusedWindowClient;
                printWriterPrinter.println("  mCurId=" + this.mCurId + " mHaveConnect=" + this.mHaveConnection + " mBoundToMethod=" + this.mBoundToMethod);
                StringBuilder sb2 = new StringBuilder();
                sb2.append("  mCurToken=");
                sb2.append(this.mCurToken);
                printWriterPrinter.println(sb2.toString());
                printWriterPrinter.println("  mCurIntent=" + this.mCurIntent);
                iInputMethod = this.mCurMethod;
                printWriterPrinter.println("  mCurMethod=" + this.mCurMethod);
                printWriterPrinter.println("  mEnabledSession=" + this.mEnabledSession);
                printWriterPrinter.println("  mImeWindowVis=" + imeWindowStatusToString(this.mImeWindowVis));
                printWriterPrinter.println("  mShowRequested=" + this.mShowRequested + " mShowExplicitlyRequested=" + this.mShowExplicitlyRequested + " mShowForced=" + this.mShowForced + " mInputShown=" + this.mInputShown);
                StringBuilder sb3 = new StringBuilder();
                sb3.append("  mInFullscreenMode=");
                sb3.append(this.mInFullscreenMode);
                printWriterPrinter.println(sb3.toString());
                StringBuilder sb4 = new StringBuilder();
                sb4.append("  mCurUserActionNotificationSequenceNumber=");
                sb4.append(this.mCurUserActionNotificationSequenceNumber);
                printWriterPrinter.println(sb4.toString());
                printWriterPrinter.println("  mSystemReady=" + this.mSystemReady + " mInteractive=" + this.mIsInteractive);
                StringBuilder sb5 = new StringBuilder();
                sb5.append("  mSettingsObserver=");
                sb5.append(this.mSettingsObserver);
                printWriterPrinter.println(sb5.toString());
                printWriterPrinter.println("  mSwitchingController:");
                this.mSwitchingController.dump(printWriterPrinter);
                printWriterPrinter.println("  mSettings:");
                this.mSettings.dumpLocked(printWriterPrinter, "    ");
                printWriterPrinter.println("  mStartInputHistory:");
                this.mStartInputHistory.dump(printWriter, "   ");
            }
            printWriterPrinter.println(" ");
            if (clientState != null) {
                printWriter.flush();
                try {
                    TransferPipe.dumpAsync(clientState.client.asBinder(), fileDescriptor, strArr);
                } catch (RemoteException | IOException e) {
                    printWriterPrinter.println("Failed to dump input method client: " + e);
                }
            } else {
                printWriterPrinter.println("No input method client.");
            }
            if (clientState2 != null && clientState != clientState2) {
                printWriterPrinter.println(" ");
                printWriterPrinter.println("Warning: Current input method client doesn't match the last focused. window.");
                printWriterPrinter.println("Dumping input method client in the last focused window just in case.");
                printWriterPrinter.println(" ");
                printWriter.flush();
                try {
                    TransferPipe.dumpAsync(clientState2.client.asBinder(), fileDescriptor, strArr);
                } catch (RemoteException | IOException e2) {
                    printWriterPrinter.println("Failed to dump input method client in focused window: " + e2);
                }
            }
            printWriterPrinter.println(" ");
            if (iInputMethod != null) {
                printWriter.flush();
                try {
                    TransferPipe.dumpAsync(iInputMethod.asBinder(), fileDescriptor, strArr);
                    return;
                } catch (RemoteException | IOException e3) {
                    printWriterPrinter.println("Failed to dump input method service: " + e3);
                    return;
                }
            }
            printWriterPrinter.println("No input method service.");
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) throws RemoteException {
        new ShellCommandImpl(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    private static final class ShellCommandImpl extends ShellCommand {
        final InputMethodManagerService mService;

        ShellCommandImpl(InputMethodManagerService inputMethodManagerService) {
            this.mService = inputMethodManagerService;
        }

        public int onCommand(String str) throws Exception {
            if ("refresh_debug_properties".equals(str)) {
                return refreshDebugProperties();
            }
            if ("set-bind-instant-service-allowed".equals(str)) {
                return setBindInstantServiceAllowed();
            }
            if ("ime".equals(str)) {
                String nextArg = getNextArg();
                if (nextArg != null && !"help".equals(nextArg) && !"-h".equals(nextArg)) {
                    switch (nextArg) {
                        case "list":
                            return this.mService.handleShellCommandListInputMethods(this);
                        case "enable":
                            return this.mService.handleShellCommandEnableDisableInputMethod(this, true);
                        case "disable":
                            return this.mService.handleShellCommandEnableDisableInputMethod(this, false);
                        case "set":
                            return this.mService.handleShellCommandSetInputMethod(this);
                        case "reset":
                            return this.mService.handleShellCommandResetInputMethod(this);
                        default:
                            getOutPrintWriter().println("Unknown command: " + nextArg);
                            return -1;
                    }
                }
                onImeCommandHelp();
                return 0;
            }
            return handleDefaultCommands(str);
        }

        private int setBindInstantServiceAllowed() {
            return this.mService.handleSetBindInstantServiceAllowed(this);
        }

        private int refreshDebugProperties() {
            DebugFlags.FLAG_OPTIMIZE_START_INPUT.refresh();
            return 0;
        }

        public void onHelp() throws Exception {
            PrintWriter outPrintWriter = getOutPrintWriter();
            Throwable th = null;
            try {
                outPrintWriter.println("InputMethodManagerService commands:");
                outPrintWriter.println("  help");
                outPrintWriter.println("    Prints this help text.");
                outPrintWriter.println("  dump [options]");
                outPrintWriter.println("    Synonym of dumpsys.");
                outPrintWriter.println("  ime <command> [options]");
                outPrintWriter.println("    Manipulate IMEs.  Run \"ime help\" for details.");
                outPrintWriter.println("  set-bind-instant-service-allowed true|false ");
                outPrintWriter.println("    Set whether binding to services provided by instant apps is allowed.");
            } finally {
                if (outPrintWriter != null) {
                    $closeResource(th, outPrintWriter);
                }
            }
        }

        private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
            if (th == null) {
                autoCloseable.close();
                return;
            }
            try {
                autoCloseable.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }

        private void onImeCommandHelp() throws Exception {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(getOutPrintWriter(), "  ", 100);
            try {
                indentingPrintWriter.println("ime <command>:");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("list [-a] [-s]");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("prints all enabled input methods.");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("-a: see all input methods");
                indentingPrintWriter.println("-s: only a single summary line of each");
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("enable <ID>");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("allows the given input method ID to be used.");
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("disable <ID>");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("disallows the given input method ID to be used.");
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("set <ID>");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("switches to the given input method ID.");
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.println("reset");
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("reset currently selected/enabled IMEs to the default ones as if the device is initially booted with the current locale.");
                indentingPrintWriter.decreaseIndent();
                indentingPrintWriter.decreaseIndent();
            } finally {
                $closeResource(null, indentingPrintWriter);
            }
        }
    }

    private int handleSetBindInstantServiceAllowed(ShellCommand shellCommand) {
        String nextArgRequired = shellCommand.getNextArgRequired();
        if (nextArgRequired == null) {
            shellCommand.getErrPrintWriter().println("Error: no true/false specified");
            return -1;
        }
        boolean z = Boolean.parseBoolean(nextArgRequired);
        synchronized (this.mMethodMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_BIND_INSTANT_SERVICE") != 0) {
                shellCommand.getErrPrintWriter().print("Caller must have MANAGE_BIND_INSTANT_SERVICE permission");
                return -1;
            }
            if (this.mBindInstantServiceAllowed == z) {
                return 0;
            }
            this.mBindInstantServiceAllowed = z;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                resetSelectedInputMethodAndSubtypeLocked(null);
                this.mSettings.putSelectedInputMethod((String) null);
                buildInputMethodListLocked(false);
                updateInputMethodsFromSettingsLocked(true);
                return 0;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private int handleShellCommandListInputMethods(ShellCommand shellCommand) {
        boolean z = false;
        boolean z2 = false;
        while (true) {
            String nextOption = shellCommand.getNextOption();
            if (nextOption != null) {
                byte b = -1;
                int iHashCode = nextOption.hashCode();
                if (iHashCode != 1492) {
                    if (iHashCode == 1510 && nextOption.equals("-s")) {
                        b = 1;
                    }
                } else if (nextOption.equals("-a")) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        z = true;
                        break;
                    case 1:
                        z2 = true;
                        break;
                }
            } else {
                List<InputMethodInfo> inputMethodList = z ? getInputMethodList() : getEnabledInputMethodList();
                final PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
                Printer printer = new Printer() {
                    @Override
                    public final void println(String str) {
                        outPrintWriter.println(str);
                    }
                };
                int size = inputMethodList.size();
                for (int i = 0; i < size; i++) {
                    if (z2) {
                        outPrintWriter.println(inputMethodList.get(i).getId());
                    } else {
                        outPrintWriter.print(inputMethodList.get(i).getId());
                        outPrintWriter.println(":");
                        inputMethodList.get(i).dump(printer, "  ");
                    }
                }
                return 0;
            }
        }
    }

    private int handleShellCommandEnableDisableInputMethod(ShellCommand shellCommand, boolean z) {
        boolean inputMethodEnabledLocked;
        if (!calledFromValidUser()) {
            shellCommand.getErrPrintWriter().print("Must be called from the foreground user or with INTERACT_ACROSS_USERS_FULL");
            return -1;
        }
        String nextArgRequired = shellCommand.getNextArgRequired();
        synchronized (this.mMethodMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                shellCommand.getErrPrintWriter().print("Caller must have WRITE_SECURE_SETTINGS permission");
                throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                inputMethodEnabledLocked = setInputMethodEnabledLocked(nextArgRequired, z);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        outPrintWriter.print("Input method ");
        outPrintWriter.print(nextArgRequired);
        outPrintWriter.print(": ");
        outPrintWriter.print(z == inputMethodEnabledLocked ? "already " : "now ");
        outPrintWriter.println(z ? "enabled" : "disabled");
        return 0;
    }

    private int handleShellCommandSetInputMethod(ShellCommand shellCommand) {
        String nextArgRequired = shellCommand.getNextArgRequired();
        setInputMethod(null, nextArgRequired);
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        outPrintWriter.print("Input method ");
        outPrintWriter.print(nextArgRequired);
        outPrintWriter.println("  selected");
        return 0;
    }

    private int handleShellCommandResetInputMethod(ShellCommand shellCommand) {
        String selectedInputMethod;
        List<InputMethodInfo> enabledInputMethodList;
        if (!calledFromValidUser()) {
            shellCommand.getErrPrintWriter().print("Must be called from the foreground user or with INTERACT_ACROSS_USERS_FULL");
            return -1;
        }
        synchronized (this.mMethodMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                shellCommand.getErrPrintWriter().print("Caller must have WRITE_SECURE_SETTINGS permission");
                throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (this.mMethodMap) {
                    hideCurrentInputLocked(0, null);
                    unbindCurrentMethodLocked(false);
                    resetSelectedInputMethodAndSubtypeLocked(null);
                    this.mSettings.putSelectedInputMethod((String) null);
                    ArrayList enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
                    int size = enabledInputMethodListLocked.size();
                    for (int i = 0; i < size; i++) {
                        setInputMethodEnabledLocked(((InputMethodInfo) enabledInputMethodListLocked.get(i)).getId(), false);
                    }
                    ArrayList defaultEnabledImes = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mMethodList);
                    int size2 = defaultEnabledImes.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        setInputMethodEnabledLocked(((InputMethodInfo) defaultEnabledImes.get(i2)).getId(), true);
                    }
                    updateInputMethodsFromSettingsLocked(true);
                    InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), this.mSettings.getCurrentUserId(), this.mContext.getBasePackageName());
                    selectedInputMethod = this.mSettings.getSelectedInputMethod();
                    enabledInputMethodList = getEnabledInputMethodList();
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
                outPrintWriter.println("Reset current and enabled IMEs");
                outPrintWriter.println("Newly selected IME:");
                outPrintWriter.print("  ");
                outPrintWriter.println(selectedInputMethod);
                outPrintWriter.println("Newly enabled IMEs:");
                int size3 = enabledInputMethodList.size();
                for (int i3 = 0; i3 < size3; i3++) {
                    outPrintWriter.print("  ");
                    outPrintWriter.println(enabledInputMethodList.get(i3).getId());
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
        return 0;
    }

    public void sendCharacterToCurClient(int i) {
        if (this.mCurClient != null && this.mCurClient.client != null) {
            try {
                this.mCurClient.client.sendCharacter(i);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
