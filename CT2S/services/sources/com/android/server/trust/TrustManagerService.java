package com.android.server.trust;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.trust.ITrustListener;
import android.app.trust.ITrustManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.Xml;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.SystemService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class TrustManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final int MSG_CLEANUP_USER = 8;
    private static final int MSG_DISPATCH_UNLOCK_ATTEMPT = 3;
    private static final int MSG_ENABLED_AGENTS_CHANGED = 4;
    private static final int MSG_KEYGUARD_SHOWING_CHANGED = 6;
    private static final int MSG_REGISTER_LISTENER = 1;
    private static final int MSG_REQUIRE_CREDENTIAL_ENTRY = 5;
    private static final int MSG_START_USER = 7;
    private static final int MSG_SWITCH_USER = 9;
    private static final int MSG_UNREGISTER_LISTENER = 2;
    private static final String PERMISSION_PROVIDE_AGENT = "android.permission.PROVIDE_TRUST_AGENT";
    private static final String TAG = "TrustManagerService";
    private static final Intent TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");
    private final ArraySet<AgentInfo> mActiveAgents;
    private final ActivityManager mActivityManager;
    final TrustArchive mArchive;
    private final Context mContext;
    private int mCurrentUser;

    @GuardedBy("mDeviceLockedForUser")
    private final SparseBooleanArray mDeviceLockedForUser;
    private final Handler mHandler;
    private final LockPatternUtils mLockPatternUtils;
    private final PackageMonitor mPackageMonitor;
    private final Receiver mReceiver;
    private final IBinder mService;
    private boolean mTrustAgentsCanRun;
    private final ArrayList<ITrustListener> mTrustListeners;
    private final SparseBooleanArray mUserHasAuthenticatedSinceBoot;

    @GuardedBy("mUserIsTrusted")
    private final SparseBooleanArray mUserIsTrusted;
    private final UserManager mUserManager;

    public TrustManagerService(Context context) {
        super(context);
        this.mActiveAgents = new ArraySet<>();
        this.mTrustListeners = new ArrayList<>();
        this.mReceiver = new Receiver();
        this.mUserHasAuthenticatedSinceBoot = new SparseBooleanArray();
        this.mArchive = new TrustArchive();
        this.mUserIsTrusted = new SparseBooleanArray();
        this.mDeviceLockedForUser = new SparseBooleanArray();
        this.mTrustAgentsCanRun = DEBUG;
        this.mCurrentUser = 0;
        this.mService = new ITrustManager.Stub() {
            public void reportUnlockAttempt(boolean authenticated, int userId) throws RemoteException {
                enforceReportPermission();
                TrustManagerService.this.mHandler.obtainMessage(3, authenticated ? 1 : 0, userId).sendToTarget();
            }

            public void reportEnabledTrustAgentsChanged(int userId) throws RemoteException {
                enforceReportPermission();
                TrustManagerService.this.mHandler.removeMessages(4);
                TrustManagerService.this.mHandler.sendEmptyMessage(4);
            }

            public void reportRequireCredentialEntry(int userId) throws RemoteException {
                enforceReportPermission();
                if (userId == -1 || userId >= 0) {
                    TrustManagerService.this.mHandler.obtainMessage(5, userId, 0).sendToTarget();
                    return;
                }
                throw new IllegalArgumentException("userId must be an explicit user id or USER_ALL");
            }

            public void reportKeyguardShowingChanged() throws RemoteException {
                enforceReportPermission();
                TrustManagerService.this.mHandler.removeMessages(6);
                TrustManagerService.this.mHandler.sendEmptyMessage(6);
            }

            public void registerTrustListener(ITrustListener trustListener) throws RemoteException {
                enforceListenerPermission();
                TrustManagerService.this.mHandler.obtainMessage(1, trustListener).sendToTarget();
            }

            public void unregisterTrustListener(ITrustListener trustListener) throws RemoteException {
                enforceListenerPermission();
                TrustManagerService.this.mHandler.obtainMessage(2, trustListener).sendToTarget();
            }

            public boolean isDeviceLocked(int userId) throws RemoteException {
                return TrustManagerService.this.isDeviceLockedInner(TrustManagerService.this.resolveProfileParent(ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), userId, TrustManagerService.DEBUG, true, "isDeviceLocked", null)));
            }

            private void enforceReportPermission() {
                TrustManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_KEYGUARD_SECURE_STORAGE", "reporting trust events");
            }

            private void enforceListenerPermission() {
                TrustManagerService.this.mContext.enforceCallingPermission("android.permission.TRUST_LISTENER", "register trust listener");
            }

            protected void dump(FileDescriptor fd, final PrintWriter fout, String[] args) {
                TrustManagerService.this.mContext.enforceCallingPermission("android.permission.DUMP", "dumping TrustManagerService");
                if (!TrustManagerService.this.isSafeMode()) {
                    if (TrustManagerService.this.mTrustAgentsCanRun) {
                        final List<UserInfo> userInfos = TrustManagerService.this.mUserManager.getUsers(true);
                        TrustManagerService.this.mHandler.runWithScissors(new Runnable() {
                            @Override
                            public void run() {
                                fout.println("Trust manager state:");
                                for (UserInfo user : userInfos) {
                                    dumpUser(fout, user, user.id == TrustManagerService.this.mCurrentUser ? true : TrustManagerService.DEBUG);
                                }
                            }
                        }, 1500L);
                        return;
                    } else {
                        fout.println("disabled because the third-party apps can't run yet.");
                        return;
                    }
                }
                fout.println("disabled because the system is in safe mode.");
            }

            private void dumpUser(PrintWriter fout, UserInfo user, boolean isCurrent) {
                fout.printf(" User \"%s\" (id=%d, flags=%#x)", user.name, Integer.valueOf(user.id), Integer.valueOf(user.flags));
                if (!user.supportsSwitchTo()) {
                    fout.println("(managed profile)");
                    fout.println("   disabled because switching to this user is not possible.");
                    return;
                }
                if (isCurrent) {
                    fout.print(" (current)");
                }
                fout.print(": trusted=" + dumpBool(TrustManagerService.this.aggregateIsTrusted(user.id)));
                fout.print(", trustManaged=" + dumpBool(TrustManagerService.this.aggregateIsTrustManaged(user.id)));
                fout.print(", deviceLocked=" + dumpBool(TrustManagerService.this.isDeviceLockedInner(user.id)));
                fout.println();
                fout.println("   Enabled agents:");
                boolean duplicateSimpleNames = TrustManagerService.DEBUG;
                ArraySet<String> simpleNames = new ArraySet<>();
                for (AgentInfo info : TrustManagerService.this.mActiveAgents) {
                    if (info.userId == user.id) {
                        boolean trusted = info.agent.isTrusted();
                        fout.print("    ");
                        fout.println(info.component.flattenToShortString());
                        fout.print("     bound=" + dumpBool(info.agent.isBound()));
                        fout.print(", connected=" + dumpBool(info.agent.isConnected()));
                        fout.print(", managingTrust=" + dumpBool(info.agent.isManagingTrust()));
                        fout.print(", trusted=" + dumpBool(trusted));
                        fout.println();
                        if (trusted) {
                            fout.println("      message=\"" + ((Object) info.agent.getMessage()) + "\"");
                        }
                        if (!info.agent.isConnected()) {
                            String restartTime = TrustArchive.formatDuration(info.agent.getScheduledRestartUptimeMillis() - SystemClock.uptimeMillis());
                            fout.println("      restartScheduledAt=" + restartTime);
                        }
                        if (!simpleNames.add(TrustArchive.getSimpleName(info.component))) {
                            duplicateSimpleNames = true;
                        }
                    }
                }
                fout.println("   Events:");
                TrustManagerService.this.mArchive.dump(fout, 50, user.id, "    ", duplicateSimpleNames);
                fout.println();
            }

            private String dumpBool(boolean b) {
                return b ? "1" : "0";
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        TrustManagerService.this.addListener((ITrustListener) msg.obj);
                        break;
                    case 2:
                        TrustManagerService.this.removeListener((ITrustListener) msg.obj);
                        break;
                    case 3:
                        TrustManagerService.this.dispatchUnlockAttempt(msg.arg1 != 0 ? true : TrustManagerService.DEBUG, msg.arg2);
                        break;
                    case 4:
                        TrustManagerService.this.refreshAgentList(-1);
                        TrustManagerService.this.refreshDeviceLockedForUser(-1);
                        break;
                    case 5:
                        TrustManagerService.this.requireCredentialEntry(msg.arg1);
                        break;
                    case 6:
                        TrustManagerService.this.refreshDeviceLockedForUser(TrustManagerService.this.mCurrentUser);
                        break;
                    case 7:
                    case 8:
                        TrustManagerService.this.refreshAgentList(msg.arg1);
                        break;
                    case 9:
                        TrustManagerService.this.mCurrentUser = msg.arg1;
                        TrustManagerService.this.refreshDeviceLockedForUser(-1);
                        break;
                }
            }
        };
        this.mPackageMonitor = new PackageMonitor() {
            public void onSomePackagesChanged() {
                TrustManagerService.this.refreshAgentList(-1);
            }

            public boolean onPackageChanged(String packageName, int uid, String[] components) {
                return true;
            }

            public void onPackageDisappeared(String packageName, int reason) {
                TrustManagerService.this.removeAgentsOfPackage(packageName);
            }
        };
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    @Override
    public void onStart() {
        publishBinderService("trust", this.mService);
    }

    @Override
    public void onBootPhase(int phase) {
        if (!isSafeMode()) {
            if (phase == 500) {
                this.mPackageMonitor.register(this.mContext, this.mHandler.getLooper(), UserHandle.ALL, true);
                this.mReceiver.register(this.mContext);
            } else if (phase == 600) {
                this.mTrustAgentsCanRun = true;
                refreshAgentList(-1);
            } else if (phase == 1000) {
                maybeEnableFactoryTrustAgents(this.mLockPatternUtils, 0);
            }
        }
    }

    private static final class AgentInfo {
        TrustAgentWrapper agent;
        ComponentName component;
        Drawable icon;
        CharSequence label;
        ComponentName settings;
        int userId;

        private AgentInfo() {
        }

        public boolean equals(Object other) {
            if (!(other instanceof AgentInfo)) {
                return TrustManagerService.DEBUG;
            }
            AgentInfo o = (AgentInfo) other;
            if (this.component.equals(o.component) && this.userId == o.userId) {
                return true;
            }
            return TrustManagerService.DEBUG;
        }

        public int hashCode() {
            return (this.component.hashCode() * 31) + this.userId;
        }
    }

    private void updateTrustAll() {
        List<UserInfo> userInfos = this.mUserManager.getUsers(true);
        for (UserInfo userInfo : userInfos) {
            updateTrust(userInfo.id, DEBUG);
        }
    }

    public void updateTrust(int userId, boolean initiatedByUser) {
        boolean changed;
        dispatchOnTrustManagedChanged(aggregateIsTrustManaged(userId), userId);
        boolean trusted = aggregateIsTrusted(userId);
        synchronized (this.mUserIsTrusted) {
            changed = this.mUserIsTrusted.get(userId) != trusted ? true : DEBUG;
            this.mUserIsTrusted.put(userId, trusted);
        }
        dispatchOnTrustChanged(trusted, userId, initiatedByUser);
        if (changed) {
            refreshDeviceLockedForUser(userId);
        }
    }

    void refreshAgentList(int userId) {
        List<UserInfo> userInfos;
        List<PersistableBundle> config;
        if (this.mTrustAgentsCanRun) {
            if (userId != -1 && userId < 0) {
                Log.e(TAG, "refreshAgentList(userId=" + userId + "): Invalid user handle, must be USER_ALL or a specific user.", new Throwable("here"));
                userId = -1;
            }
            PackageManager pm = this.mContext.getPackageManager();
            if (userId == -1) {
                userInfos = this.mUserManager.getUsers(true);
            } else {
                userInfos = new ArrayList<>();
                userInfos.add(this.mUserManager.getUserInfo(userId));
            }
            LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
            ArraySet<AgentInfo> obsoleteAgents = new ArraySet<>();
            obsoleteAgents.addAll(this.mActiveAgents);
            for (UserInfo userInfo : userInfos) {
                if (userInfo != null && !userInfo.partial && userInfo.isEnabled() && !userInfo.guestToRemove && userInfo.supportsSwitchTo() && this.mActivityManager.isUserRunning(userInfo.id) && lockPatternUtils.isSecure(userInfo.id) && this.mUserHasAuthenticatedSinceBoot.get(userInfo.id)) {
                    DevicePolicyManager dpm = lockPatternUtils.getDevicePolicyManager();
                    int disabledFeatures = dpm.getKeyguardDisabledFeatures(null, userInfo.id);
                    boolean disableTrustAgents = (disabledFeatures & 16) != 0 ? true : DEBUG;
                    List<ComponentName> enabledAgents = lockPatternUtils.getEnabledTrustAgents(userInfo.id);
                    if (enabledAgents != null) {
                        List<ResolveInfo> resolveInfos = resolveAllowedTrustAgents(pm, userInfo.id);
                        for (ResolveInfo resolveInfo : resolveInfos) {
                            ComponentName name = getComponentName(resolveInfo);
                            if (enabledAgents.contains(name) && (!disableTrustAgents || ((config = dpm.getTrustAgentConfiguration(null, name, userInfo.id)) != null && !config.isEmpty()))) {
                                AgentInfo agentInfo = new AgentInfo();
                                agentInfo.component = name;
                                agentInfo.userId = userInfo.id;
                                if (!this.mActiveAgents.contains(agentInfo)) {
                                    agentInfo.label = resolveInfo.loadLabel(pm);
                                    agentInfo.icon = resolveInfo.loadIcon(pm);
                                    agentInfo.settings = getSettingsComponentName(pm, resolveInfo);
                                    agentInfo.agent = new TrustAgentWrapper(this.mContext, this, new Intent().setComponent(name), userInfo.getUserHandle());
                                    this.mActiveAgents.add(agentInfo);
                                } else {
                                    obsoleteAgents.remove(agentInfo);
                                }
                            }
                        }
                    }
                }
            }
            boolean trustMayHaveChanged = DEBUG;
            for (int i = 0; i < obsoleteAgents.size(); i++) {
                AgentInfo info = obsoleteAgents.valueAt(i);
                if (userId == -1 || userId == info.userId) {
                    if (info.agent.isManagingTrust()) {
                        trustMayHaveChanged = true;
                    }
                    info.agent.destroy();
                    this.mActiveAgents.remove(info);
                }
            }
            if (trustMayHaveChanged) {
                if (userId == -1) {
                    updateTrustAll();
                } else {
                    updateTrust(userId, DEBUG);
                }
            }
        }
    }

    boolean isDeviceLockedInner(int userId) {
        boolean z;
        synchronized (this.mDeviceLockedForUser) {
            z = this.mDeviceLockedForUser.get(userId, true);
        }
        return z;
    }

    private void refreshDeviceLockedForUser(int userId) {
        List<UserInfo> userInfos;
        boolean changed;
        if (userId != -1 && userId < 0) {
            Log.e(TAG, "refreshDeviceLockedForUser(userId=" + userId + "): Invalid user handle, must be USER_ALL or a specific user.", new Throwable("here"));
            userId = -1;
        }
        if (userId == -1) {
            userInfos = this.mUserManager.getUsers(true);
        } else {
            userInfos = new ArrayList<>();
            userInfos.add(this.mUserManager.getUserInfo(userId));
        }
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        for (int i = 0; i < userInfos.size(); i++) {
            UserInfo info = userInfos.get(i);
            if (info != null && !info.partial && info.isEnabled() && !info.guestToRemove && info.supportsSwitchTo()) {
                int id = info.id;
                boolean secure = this.mLockPatternUtils.isSecure(id);
                boolean trusted = aggregateIsTrusted(id);
                boolean showingKeyguard = true;
                if (this.mCurrentUser == id) {
                    try {
                        showingKeyguard = wm.isKeyguardLocked();
                    } catch (RemoteException e) {
                    }
                }
                boolean deviceLocked = (secure && showingKeyguard && !trusted) ? true : DEBUG;
                synchronized (this.mDeviceLockedForUser) {
                    changed = isDeviceLockedInner(id) != deviceLocked ? true : DEBUG;
                    this.mDeviceLockedForUser.put(id, deviceLocked);
                }
                if (changed) {
                    dispatchDeviceLocked(id, deviceLocked);
                }
            }
        }
    }

    private void dispatchDeviceLocked(int userId, boolean isLocked) {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo agent = this.mActiveAgents.valueAt(i);
            if (agent.userId == userId) {
                if (isLocked) {
                    agent.agent.onDeviceLocked();
                } else {
                    agent.agent.onDeviceUnlocked();
                }
            }
        }
    }

    void updateDevicePolicyFeatures() {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.agent.isConnected()) {
                info.agent.updateDevicePolicyFeatures();
            }
        }
    }

    private void removeAgentsOfPackage(String packageName) {
        boolean trustMayHaveChanged = DEBUG;
        for (int i = this.mActiveAgents.size() - 1; i >= 0; i--) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (packageName.equals(info.component.getPackageName())) {
                Log.i(TAG, "Resetting agent " + info.component.flattenToShortString());
                if (info.agent.isManagingTrust()) {
                    trustMayHaveChanged = true;
                }
                info.agent.destroy();
                this.mActiveAgents.removeAt(i);
            }
        }
        if (trustMayHaveChanged) {
            updateTrustAll();
        }
    }

    public void resetAgent(ComponentName name, int userId) {
        boolean trustMayHaveChanged = DEBUG;
        for (int i = this.mActiveAgents.size() - 1; i >= 0; i--) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (name.equals(info.component) && userId == info.userId) {
                Log.i(TAG, "Resetting agent " + info.component.flattenToShortString());
                if (info.agent.isManagingTrust()) {
                    trustMayHaveChanged = true;
                }
                info.agent.destroy();
                this.mActiveAgents.removeAt(i);
            }
        }
        if (trustMayHaveChanged) {
            updateTrust(userId, DEBUG);
        }
        refreshAgentList(userId);
    }

    private ComponentName getSettingsComponentName(PackageManager pm, ResolveInfo resolveInfo) {
        XmlResourceParser parser;
        int type;
        if (resolveInfo == null || resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        XmlResourceParser parser2 = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, "android.service.trust.trustagent");
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
            if (0 != 0) {
                parser2.close();
            }
        } catch (IOException e2) {
            caughtException = e2;
            if (0 != 0) {
                parser2.close();
            }
        } catch (XmlPullParserException e3) {
            caughtException = e3;
            if (0 != 0) {
                parser2.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                parser2.close();
            }
            throw th;
        }
        if (parser == null) {
            Slog.w(TAG, "Can't find android.service.trust.trustagent meta-data");
            if (parser == null) {
                return null;
            }
            parser.close();
            return null;
        }
        Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        do {
            type = parser.next();
            if (type == 1) {
                break;
            }
        } while (type != 2);
        String nodeName = parser.getName();
        if (!"trust-agent".equals(nodeName)) {
            Slog.w(TAG, "Meta-data does not start with trust-agent tag");
            if (parser == null) {
                return null;
            }
            parser.close();
            return null;
        }
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.TrustAgent);
        cn = sa.getString(2);
        sa.recycle();
        if (parser != null) {
            parser.close();
        }
        if (caughtException != null) {
            Slog.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName, caughtException);
            return null;
        }
        if (cn == null) {
            return null;
        }
        if (cn.indexOf(47) < 0) {
            cn = resolveInfo.serviceInfo.packageName + "/" + cn;
        }
        return ComponentName.unflattenFromString(cn);
    }

    private ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private void maybeEnableFactoryTrustAgents(LockPatternUtils utils, int userId) {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 0, userId) == 0) {
            PackageManager pm = this.mContext.getPackageManager();
            List<ResolveInfo> resolveInfos = resolveAllowedTrustAgents(pm, userId);
            ArraySet<ComponentName> discoveredAgents = new ArraySet<>();
            for (ResolveInfo resolveInfo : resolveInfos) {
                ComponentName componentName = getComponentName(resolveInfo);
                int applicationInfoFlags = resolveInfo.serviceInfo.applicationInfo.flags;
                if ((applicationInfoFlags & 1) == 0) {
                    Log.i(TAG, "Leaving agent " + componentName + " disabled because package is not a system package.");
                } else {
                    discoveredAgents.add(componentName);
                }
            }
            List<ComponentName> previouslyEnabledAgents = utils.getEnabledTrustAgents(userId);
            if (previouslyEnabledAgents != null) {
                discoveredAgents.addAll(previouslyEnabledAgents);
            }
            utils.setEnabledTrustAgents(discoveredAgents, userId);
            Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 1, userId);
        }
    }

    private List<ResolveInfo> resolveAllowedTrustAgents(PackageManager pm, int userId) {
        List<ResolveInfo> resolveInfos = pm.queryIntentServicesAsUser(TRUST_AGENT_INTENT, 0, userId);
        ArrayList<ResolveInfo> allowedAgents = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo != null && resolveInfo.serviceInfo.applicationInfo != null) {
                String packageName = resolveInfo.serviceInfo.packageName;
                if (pm.checkPermission(PERMISSION_PROVIDE_AGENT, packageName) != 0) {
                    ComponentName name = getComponentName(resolveInfo);
                    Log.w(TAG, "Skipping agent " + name + " because package does not have permission " + PERMISSION_PROVIDE_AGENT + ".");
                } else {
                    allowedAgents.add(resolveInfo);
                }
            }
        }
        return allowedAgents;
    }

    private boolean aggregateIsTrusted(int userId) {
        if (!this.mUserHasAuthenticatedSinceBoot.get(userId)) {
            return DEBUG;
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId && info.agent.isTrusted()) {
                return true;
            }
        }
        return DEBUG;
    }

    private boolean aggregateIsTrustManaged(int userId) {
        if (!this.mUserHasAuthenticatedSinceBoot.get(userId)) {
            return DEBUG;
        }
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId && info.agent.isManagingTrust()) {
                return true;
            }
        }
        return DEBUG;
    }

    private void dispatchUnlockAttempt(boolean successful, int userId) {
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo info = this.mActiveAgents.valueAt(i);
            if (info.userId == userId) {
                info.agent.onUnlockAttempt(successful);
            }
        }
        if (successful) {
            updateUserHasAuthenticated(userId);
        }
    }

    private void updateUserHasAuthenticated(int userId) {
        if (!this.mUserHasAuthenticatedSinceBoot.get(userId)) {
            this.mUserHasAuthenticatedSinceBoot.put(userId, true);
            refreshAgentList(userId);
        }
    }

    private void requireCredentialEntry(int userId) {
        if (userId == -1) {
            this.mUserHasAuthenticatedSinceBoot.clear();
            refreshAgentList(-1);
        } else {
            this.mUserHasAuthenticatedSinceBoot.put(userId, DEBUG);
            refreshAgentList(userId);
        }
    }

    private void addListener(ITrustListener listener) {
        for (int i = 0; i < this.mTrustListeners.size(); i++) {
            if (this.mTrustListeners.get(i).asBinder() == listener.asBinder()) {
                return;
            }
        }
        this.mTrustListeners.add(listener);
        updateTrustAll();
    }

    private void removeListener(ITrustListener listener) {
        for (int i = 0; i < this.mTrustListeners.size(); i++) {
            if (this.mTrustListeners.get(i).asBinder() == listener.asBinder()) {
                this.mTrustListeners.remove(i);
                return;
            }
        }
    }

    private void dispatchOnTrustChanged(boolean enabled, int userId, boolean initiatedByUser) {
        if (!enabled) {
            initiatedByUser = DEBUG;
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i).onTrustChanged(enabled, userId, initiatedByUser);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    private void dispatchOnTrustManagedChanged(boolean managed, int userId) {
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i).onTrustManagedChanged(managed, userId);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i);
                i--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i++;
        }
    }

    @Override
    public void onStartUser(int userId) {
        this.mHandler.obtainMessage(7, userId, 0, null).sendToTarget();
    }

    @Override
    public void onCleanupUser(int userId) {
        this.mHandler.obtainMessage(8, userId, 0, null).sendToTarget();
    }

    @Override
    public void onSwitchUser(int userId) {
        this.mHandler.obtainMessage(9, userId, 0, null).sendToTarget();
    }

    private int resolveProfileParent(int userId) {
        long identity = Binder.clearCallingIdentity();
        try {
            UserInfo parent = this.mUserManager.getProfileParent(userId);
            if (parent != null) {
                userId = parent.getUserHandle().getIdentifier();
            }
            return userId;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            int userId;
            String action = intent.getAction();
            if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                TrustManagerService.this.refreshAgentList(getSendingUserId());
                TrustManagerService.this.updateDevicePolicyFeatures();
                return;
            }
            if ("android.intent.action.USER_PRESENT".equals(action)) {
                TrustManagerService.this.updateUserHasAuthenticated(getSendingUserId());
                return;
            }
            if ("android.intent.action.USER_ADDED".equals(action)) {
                int userId2 = getUserId(intent);
                if (userId2 > 0) {
                    TrustManagerService.this.maybeEnableFactoryTrustAgents(TrustManagerService.this.mLockPatternUtils, userId2);
                    return;
                }
                return;
            }
            if ("android.intent.action.USER_REMOVED".equals(action) && (userId = getUserId(intent)) > 0) {
                TrustManagerService.this.mUserHasAuthenticatedSinceBoot.delete(userId);
                synchronized (TrustManagerService.this.mUserIsTrusted) {
                    TrustManagerService.this.mUserIsTrusted.delete(userId);
                }
                synchronized (TrustManagerService.this.mDeviceLockedForUser) {
                    TrustManagerService.this.mDeviceLockedForUser.delete(userId);
                }
                TrustManagerService.this.refreshAgentList(userId);
                TrustManagerService.this.refreshDeviceLockedForUser(userId);
            }
        }

        private int getUserId(Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -100);
            if (userId <= 0) {
                Slog.wtf(TrustManagerService.TAG, "EXTRA_USER_HANDLE missing or invalid, value=" + userId);
                return -100;
            }
            return userId;
        }

        public void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
            filter.addAction("android.intent.action.USER_PRESENT");
            filter.addAction("android.intent.action.USER_ADDED");
            filter.addAction("android.intent.action.USER_REMOVED");
            context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, null);
        }
    }
}
