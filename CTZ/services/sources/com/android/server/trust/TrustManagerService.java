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
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.text.TextUtils;
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
import com.android.internal.util.DumpUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.trust.TrustManagerService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class TrustManagerService extends SystemService {
    static final boolean DEBUG;
    private static final int MSG_CLEANUP_USER = 8;
    private static final int MSG_DISPATCH_UNLOCK_ATTEMPT = 3;
    private static final int MSG_DISPATCH_UNLOCK_LOCKOUT = 13;
    private static final int MSG_ENABLED_AGENTS_CHANGED = 4;
    private static final int MSG_FLUSH_TRUST_USUALLY_MANAGED = 10;
    private static final int MSG_KEYGUARD_SHOWING_CHANGED = 6;
    private static final int MSG_REFRESH_DEVICE_LOCKED_FOR_USER = 14;
    private static final int MSG_REGISTER_LISTENER = 1;
    private static final int MSG_START_USER = 7;
    private static final int MSG_STOP_USER = 12;
    private static final int MSG_SWITCH_USER = 9;
    private static final int MSG_UNLOCK_USER = 11;
    private static final int MSG_UNREGISTER_LISTENER = 2;
    private static final String PERMISSION_PROVIDE_AGENT = "android.permission.PROVIDE_TRUST_AGENT";
    private static final String TAG = "TrustManagerService";
    private static final Intent TRUST_AGENT_INTENT;
    private static final int TRUST_USUALLY_MANAGED_FLUSH_DELAY = 120000;
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
    private final StrongAuthTracker mStrongAuthTracker;
    private boolean mTrustAgentsCanRun;
    private final ArrayList<ITrustListener> mTrustListeners;

    @GuardedBy("mTrustUsuallyManagedForUser")
    private final SparseBooleanArray mTrustUsuallyManagedForUser;

    @GuardedBy("mUserIsTrusted")
    private final SparseBooleanArray mUserIsTrusted;
    private final UserManager mUserManager;

    @GuardedBy("mUsersUnlockedByFingerprint")
    private final SparseBooleanArray mUsersUnlockedByFingerprint;

    static {
        DEBUG = Build.IS_DEBUGGABLE && Log.isLoggable(TAG, 2);
        TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");
    }

    public TrustManagerService(Context context) {
        super(context);
        this.mActiveAgents = new ArraySet<>();
        this.mTrustListeners = new ArrayList<>();
        this.mReceiver = new Receiver(this, null);
        this.mArchive = new TrustArchive();
        this.mUserIsTrusted = new SparseBooleanArray();
        this.mDeviceLockedForUser = new SparseBooleanArray();
        this.mTrustUsuallyManagedForUser = new SparseBooleanArray();
        this.mUsersUnlockedByFingerprint = new SparseBooleanArray();
        this.mTrustAgentsCanRun = false;
        this.mCurrentUser = 0;
        this.mService = new AnonymousClass1();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                SparseBooleanArray sparseBooleanArrayClone;
                switch (message.what) {
                    case 1:
                        TrustManagerService.this.addListener((ITrustListener) message.obj);
                        return;
                    case 2:
                        TrustManagerService.this.removeListener((ITrustListener) message.obj);
                        return;
                    case 3:
                        TrustManagerService.this.dispatchUnlockAttempt(message.arg1 != 0, message.arg2);
                        return;
                    case 4:
                        TrustManagerService.this.refreshAgentList(-1);
                        TrustManagerService.this.refreshDeviceLockedForUser(-1);
                        return;
                    case 5:
                    default:
                        return;
                    case 6:
                        TrustManagerService.this.refreshDeviceLockedForUser(TrustManagerService.this.mCurrentUser);
                        return;
                    case 7:
                    case 8:
                    case 11:
                        TrustManagerService.this.refreshAgentList(message.arg1);
                        return;
                    case 9:
                        TrustManagerService.this.mCurrentUser = message.arg1;
                        TrustManagerService.this.refreshDeviceLockedForUser(-1);
                        return;
                    case 10:
                        synchronized (TrustManagerService.this.mTrustUsuallyManagedForUser) {
                            sparseBooleanArrayClone = TrustManagerService.this.mTrustUsuallyManagedForUser.clone();
                            break;
                        }
                        for (int i = 0; i < sparseBooleanArrayClone.size(); i++) {
                            int iKeyAt = sparseBooleanArrayClone.keyAt(i);
                            boolean zValueAt = sparseBooleanArrayClone.valueAt(i);
                            if (zValueAt != TrustManagerService.this.mLockPatternUtils.isTrustUsuallyManaged(iKeyAt)) {
                                TrustManagerService.this.mLockPatternUtils.setTrustUsuallyManaged(zValueAt, iKeyAt);
                            }
                        }
                        return;
                    case 12:
                        TrustManagerService.this.setDeviceLockedForUser(message.arg1, true);
                        return;
                    case 13:
                        TrustManagerService.this.dispatchUnlockLockout(message.arg1, message.arg2);
                        return;
                    case 14:
                        TrustManagerService.this.refreshDeviceLockedForUser(message.arg1);
                        return;
                }
            }
        };
        this.mPackageMonitor = new PackageMonitor() {
            public void onSomePackagesChanged() {
                TrustManagerService.this.refreshAgentList(-1);
            }

            public boolean onPackageChanged(String str, int i, String[] strArr) {
                return true;
            }

            public void onPackageDisappeared(String str, int i) {
                TrustManagerService.this.removeAgentsOfPackage(str);
            }
        };
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mStrongAuthTracker = new StrongAuthTracker(context);
    }

    @Override
    public void onStart() {
        publishBinderService("trust", this.mService);
    }

    @Override
    public void onBootPhase(int i) {
        if (isSafeMode()) {
            return;
        }
        if (i == 500) {
            this.mPackageMonitor.register(this.mContext, this.mHandler.getLooper(), UserHandle.ALL, true);
            this.mReceiver.register(this.mContext);
            this.mLockPatternUtils.registerStrongAuthTracker(this.mStrongAuthTracker);
        } else if (i == 600) {
            this.mTrustAgentsCanRun = true;
            refreshAgentList(-1);
            refreshDeviceLockedForUser(-1);
        } else if (i == 1000) {
            maybeEnableFactoryTrustAgents(this.mLockPatternUtils, 0);
        }
    }

    private static final class AgentInfo {
        TrustAgentWrapper agent;
        ComponentName component;
        Drawable icon;
        CharSequence label;
        SettingsAttrs settings;
        int userId;

        private AgentInfo() {
        }

        AgentInfo(AnonymousClass1 anonymousClass1) {
            this();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof AgentInfo)) {
                return false;
            }
            AgentInfo agentInfo = (AgentInfo) obj;
            return this.component.equals(agentInfo.component) && this.userId == agentInfo.userId;
        }

        public int hashCode() {
            return (this.component.hashCode() * 31) + this.userId;
        }
    }

    private void updateTrustAll() {
        Iterator it = this.mUserManager.getUsers(true).iterator();
        while (it.hasNext()) {
            updateTrust(((UserInfo) it.next()).id, 0);
        }
    }

    public void updateTrust(int i, int i2) {
        boolean z;
        boolean zAggregateIsTrustManaged = aggregateIsTrustManaged(i);
        dispatchOnTrustManagedChanged(zAggregateIsTrustManaged, i);
        if (this.mStrongAuthTracker.isTrustAllowedForUser(i) && isTrustUsuallyManagedInternal(i) != zAggregateIsTrustManaged) {
            updateTrustUsuallyManaged(i, zAggregateIsTrustManaged);
        }
        boolean zAggregateIsTrusted = aggregateIsTrusted(i);
        synchronized (this.mUserIsTrusted) {
            z = this.mUserIsTrusted.get(i) != zAggregateIsTrusted;
            this.mUserIsTrusted.put(i, zAggregateIsTrusted);
        }
        dispatchOnTrustChanged(zAggregateIsTrusted, i, i2);
        if (z) {
            refreshDeviceLockedForUser(i);
        }
    }

    private void updateTrustUsuallyManaged(int i, boolean z) {
        synchronized (this.mTrustUsuallyManagedForUser) {
            this.mTrustUsuallyManagedForUser.put(i, z);
        }
        this.mHandler.removeMessages(10);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(10), JobStatus.DEFAULT_TRIGGER_MAX_DELAY);
    }

    public long addEscrowToken(byte[] bArr, int i) {
        return this.mLockPatternUtils.addEscrowToken(bArr, i);
    }

    public boolean removeEscrowToken(long j, int i) {
        return this.mLockPatternUtils.removeEscrowToken(j, i);
    }

    public boolean isEscrowTokenActive(long j, int i) {
        return this.mLockPatternUtils.isEscrowTokenActive(j, i);
    }

    public void unlockUserWithToken(long j, byte[] bArr, int i) {
        this.mLockPatternUtils.unlockUserWithToken(j, bArr, i);
    }

    void showKeyguardErrorMessage(CharSequence charSequence) {
        dispatchOnTrustError(charSequence);
    }

    void refreshAgentList(int i) {
        List<UserInfo> arrayList;
        PackageManager packageManager;
        boolean z;
        int strongAuthForUser;
        List trustAgentConfiguration;
        int i2 = i;
        if (DEBUG) {
            Slog.d(TAG, "refreshAgentList(" + i2 + ")");
        }
        if (this.mTrustAgentsCanRun) {
            if (i2 != -1 && i2 < 0) {
                Log.e(TAG, "refreshAgentList(userId=" + i2 + "): Invalid user handle, must be USER_ALL or a specific user.", new Throwable("here"));
                i2 = -1;
            }
            PackageManager packageManager2 = this.mContext.getPackageManager();
            boolean z2 = true;
            if (i2 == -1) {
                arrayList = this.mUserManager.getUsers(true);
            } else {
                arrayList = new ArrayList();
                arrayList.add(this.mUserManager.getUserInfo(i2));
            }
            LockPatternUtils lockPatternUtils = this.mLockPatternUtils;
            ArraySet arraySet = new ArraySet();
            arraySet.addAll((ArraySet) this.mActiveAgents);
            for (UserInfo userInfo : arrayList) {
                if (userInfo != null && !userInfo.partial && userInfo.isEnabled() && !userInfo.guestToRemove) {
                    if (userInfo.supportsSwitchToByUser()) {
                        if (this.mActivityManager.isUserRunning(userInfo.id)) {
                            if (lockPatternUtils.isSecure(userInfo.id)) {
                                DevicePolicyManager devicePolicyManager = lockPatternUtils.getDevicePolicyManager();
                                ?? r12 = 0;
                                boolean z3 = (devicePolicyManager.getKeyguardDisabledFeatures(null, userInfo.id) & 16) != 0 ? z2 : false;
                                List enabledTrustAgents = lockPatternUtils.getEnabledTrustAgents(userInfo.id);
                                if (enabledTrustAgents != null) {
                                    for (ResolveInfo resolveInfo : resolveAllowedTrustAgents(packageManager2, userInfo.id)) {
                                        ComponentName componentName = getComponentName(resolveInfo);
                                        if (enabledTrustAgents.contains(componentName)) {
                                            if (z3 && ((trustAgentConfiguration = devicePolicyManager.getTrustAgentConfiguration(r12, componentName, userInfo.id)) == null || trustAgentConfiguration.isEmpty())) {
                                                if (DEBUG) {
                                                    Slog.d(TAG, "refreshAgentList: skipping " + componentName.flattenToShortString() + " u" + userInfo.id + ": not allowed by DPM");
                                                }
                                                z2 = true;
                                            } else {
                                                AgentInfo agentInfo = new AgentInfo(r12);
                                                agentInfo.component = componentName;
                                                agentInfo.userId = userInfo.id;
                                                if (this.mActiveAgents.contains(agentInfo)) {
                                                    agentInfo = this.mActiveAgents.valueAt(this.mActiveAgents.indexOf(agentInfo));
                                                } else {
                                                    agentInfo.label = resolveInfo.loadLabel(packageManager2);
                                                    agentInfo.icon = resolveInfo.loadIcon(packageManager2);
                                                    agentInfo.settings = getSettingsAttrs(packageManager2, resolveInfo);
                                                }
                                                boolean z4 = resolveInfo.serviceInfo.directBootAware && agentInfo.settings.canUnlockProfile;
                                                if (z4 && DEBUG) {
                                                    StringBuilder sb = new StringBuilder();
                                                    packageManager = packageManager2;
                                                    sb.append("refreshAgentList: trustagent ");
                                                    sb.append(componentName);
                                                    sb.append("of user ");
                                                    sb.append(userInfo.id);
                                                    sb.append("can unlock user profile.");
                                                    Slog.d(TAG, sb.toString());
                                                } else {
                                                    packageManager = packageManager2;
                                                }
                                                if (this.mUserManager.isUserUnlockingOrUnlocked(userInfo.id) || z4) {
                                                    if (this.mStrongAuthTracker.canAgentsRunForUser(userInfo.id) || (strongAuthForUser = this.mStrongAuthTracker.getStrongAuthForUser(userInfo.id)) == 8) {
                                                        z = true;
                                                    } else {
                                                        z = true;
                                                        if (strongAuthForUser != 1 || !z4) {
                                                            if (DEBUG) {
                                                                Slog.d(TAG, "refreshAgentList: skipping user " + userInfo.id + ": prevented by StrongAuthTracker = 0x" + Integer.toHexString(this.mStrongAuthTracker.getStrongAuthForUser(userInfo.id)));
                                                            }
                                                        }
                                                        z2 = z;
                                                        packageManager2 = packageManager;
                                                    }
                                                    if (agentInfo.agent == null) {
                                                        agentInfo.agent = new TrustAgentWrapper(this.mContext, this, new Intent().setComponent(componentName), userInfo.getUserHandle());
                                                    }
                                                    if (this.mActiveAgents.contains(agentInfo)) {
                                                        arraySet.remove(agentInfo);
                                                    } else {
                                                        this.mActiveAgents.add(agentInfo);
                                                    }
                                                    z2 = z;
                                                    packageManager2 = packageManager;
                                                } else {
                                                    if (DEBUG) {
                                                        Slog.d(TAG, "refreshAgentList: skipping user " + userInfo.id + "'s trust agent " + componentName + ": FBE still locked and  the agent cannot unlock user profile.");
                                                    }
                                                    packageManager2 = packageManager;
                                                    z2 = true;
                                                }
                                                r12 = 0;
                                            }
                                        } else if (DEBUG) {
                                            Slog.d(TAG, "refreshAgentList: skipping " + componentName.flattenToShortString() + " u" + userInfo.id + ": not enabled by user");
                                            z2 = true;
                                        }
                                    }
                                } else if (DEBUG) {
                                    Slog.d(TAG, "refreshAgentList: skipping user " + userInfo.id + ": no agents enabled by user");
                                }
                            } else if (DEBUG) {
                                Slog.d(TAG, "refreshAgentList: skipping user " + userInfo.id + ": no secure credential");
                            }
                        } else if (DEBUG) {
                            Slog.d(TAG, "refreshAgentList: skipping user " + userInfo.id + ": user not started");
                        }
                    } else if (DEBUG) {
                        Slog.d(TAG, "refreshAgentList: skipping user " + userInfo.id + ": switchToByUser=false");
                    }
                }
            }
            boolean z5 = z2;
            boolean z6 = false;
            for (int i3 = 0; i3 < arraySet.size(); i3++) {
                AgentInfo agentInfo2 = (AgentInfo) arraySet.valueAt(i3);
                if (i2 == -1 || i2 == agentInfo2.userId) {
                    if (agentInfo2.agent.isManagingTrust()) {
                        z6 = z5;
                    }
                    agentInfo2.agent.destroy();
                    this.mActiveAgents.remove(agentInfo2);
                }
            }
            if (z6) {
                if (i2 == -1) {
                    updateTrustAll();
                } else {
                    updateTrust(i2, 0);
                }
            }
        }
    }

    boolean isDeviceLockedInner(int i) {
        boolean z;
        synchronized (this.mDeviceLockedForUser) {
            z = this.mDeviceLockedForUser.get(i, true);
        }
        return z;
    }

    private void refreshDeviceLockedForUser(int i) {
        List users;
        boolean zIsKeyguardLocked;
        boolean z;
        if (i != -1 && i < 0) {
            Log.e(TAG, "refreshDeviceLockedForUser(userId=" + i + "): Invalid user handle, must be USER_ALL or a specific user.", new Throwable("here"));
            i = -1;
        }
        if (i == -1) {
            users = this.mUserManager.getUsers(true);
        } else {
            ArrayList arrayList = new ArrayList();
            arrayList.add(this.mUserManager.getUserInfo(i));
            users = arrayList;
        }
        IWindowManager windowManagerService = WindowManagerGlobal.getWindowManagerService();
        for (int i2 = 0; i2 < users.size(); i2++) {
            UserInfo userInfo = (UserInfo) users.get(i2);
            if (userInfo != null && !userInfo.partial && userInfo.isEnabled() && !userInfo.guestToRemove && userInfo.supportsSwitchToByUser()) {
                int i3 = userInfo.id;
                boolean zIsSecure = this.mLockPatternUtils.isSecure(i3);
                boolean zAggregateIsTrusted = aggregateIsTrusted(i3);
                if (this.mCurrentUser == i3) {
                    synchronized (this.mUsersUnlockedByFingerprint) {
                        z = this.mUsersUnlockedByFingerprint.get(i3, false);
                    }
                    try {
                        zIsKeyguardLocked = windowManagerService.isKeyguardLocked();
                    } catch (RemoteException e) {
                        zIsKeyguardLocked = true;
                    }
                } else {
                    zIsKeyguardLocked = true;
                    z = false;
                }
                setDeviceLockedForUser(i3, zIsSecure && zIsKeyguardLocked && !zAggregateIsTrusted && !z);
            }
        }
    }

    private void setDeviceLockedForUser(int i, boolean z) {
        int i2;
        boolean z2;
        synchronized (this.mDeviceLockedForUser) {
            z2 = isDeviceLockedInner(i) != z;
            this.mDeviceLockedForUser.put(i, z);
        }
        if (z2) {
            dispatchDeviceLocked(i, z);
            KeyStore.getInstance().onUserLockedStateChanged(i, z);
            for (int i3 : this.mUserManager.getEnabledProfileIds(i)) {
                if (this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(i3)) {
                    KeyStore.getInstance().onUserLockedStateChanged(i3, z);
                }
            }
        }
    }

    private void dispatchDeviceLocked(int i, boolean z) {
        for (int i2 = 0; i2 < this.mActiveAgents.size(); i2++) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(i2);
            if (agentInfoValueAt.userId == i) {
                if (z) {
                    agentInfoValueAt.agent.onDeviceLocked();
                } else {
                    agentInfoValueAt.agent.onDeviceUnlocked();
                }
            }
        }
    }

    void updateDevicePolicyFeatures() {
        boolean z = false;
        for (int i = 0; i < this.mActiveAgents.size(); i++) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(i);
            if (agentInfoValueAt.agent.isConnected()) {
                agentInfoValueAt.agent.updateDevicePolicyFeatures();
                z = true;
            }
        }
        if (z) {
            this.mArchive.logDevicePolicyChanged();
        }
    }

    private void removeAgentsOfPackage(String str) {
        boolean z = false;
        for (int size = this.mActiveAgents.size() - 1; size >= 0; size--) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(size);
            if (str.equals(agentInfoValueAt.component.getPackageName())) {
                Log.i(TAG, "Resetting agent " + agentInfoValueAt.component.flattenToShortString());
                if (agentInfoValueAt.agent.isManagingTrust()) {
                    z = true;
                }
                agentInfoValueAt.agent.destroy();
                this.mActiveAgents.removeAt(size);
            }
        }
        if (z) {
            updateTrustAll();
        }
    }

    public void resetAgent(ComponentName componentName, int i) {
        boolean z = false;
        for (int size = this.mActiveAgents.size() - 1; size >= 0; size--) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(size);
            if (componentName.equals(agentInfoValueAt.component) && i == agentInfoValueAt.userId) {
                Log.i(TAG, "Resetting agent " + agentInfoValueAt.component.flattenToShortString());
                if (agentInfoValueAt.agent.isManagingTrust()) {
                    z = true;
                }
                agentInfoValueAt.agent.destroy();
                this.mActiveAgents.removeAt(size);
            }
        }
        if (z) {
            updateTrust(i, 0);
        }
        refreshAgentList(i);
    }

    private SettingsAttrs getSettingsAttrs(PackageManager packageManager, ResolveInfo resolveInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        String string;
        boolean z;
        int next;
        if (resolveInfo == null || resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        try {
            xmlResourceParserLoadXmlMetaData = resolveInfo.serviceInfo.loadXmlMetaData(packageManager, "android.service.trust.trustagent");
            try {
                try {
                } catch (Throwable th) {
                    th = th;
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                    }
                    throw th;
                }
            } catch (PackageManager.NameNotFoundException e) {
                e = e;
                string = null;
            } catch (IOException e2) {
                e = e2;
                string = null;
            } catch (XmlPullParserException e3) {
                e = e3;
                string = null;
            }
        } catch (PackageManager.NameNotFoundException e4) {
            e = e4;
            xmlResourceParserLoadXmlMetaData = null;
            string = null;
        } catch (IOException e5) {
            e = e5;
            xmlResourceParserLoadXmlMetaData = null;
            string = null;
        } catch (XmlPullParserException e6) {
            e = e6;
            xmlResourceParserLoadXmlMetaData = null;
            string = null;
        } catch (Throwable th2) {
            th = th2;
            xmlResourceParserLoadXmlMetaData = null;
        }
        if (xmlResourceParserLoadXmlMetaData == null) {
            Slog.w(TAG, "Can't find android.service.trust.trustagent meta-data");
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            return null;
        }
        Resources resourcesForApplication = packageManager.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
        do {
            next = xmlResourceParserLoadXmlMetaData.next();
            if (next == 1) {
                break;
            }
        } while (next != 2);
        if (!"trust-agent".equals(xmlResourceParserLoadXmlMetaData.getName())) {
            Slog.w(TAG, "Meta-data does not start with trust-agent tag");
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            return null;
        }
        TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.TrustAgent);
        string = typedArrayObtainAttributes.getString(2);
        try {
            z = typedArrayObtainAttributes.getBoolean(3, false);
        } catch (PackageManager.NameNotFoundException e7) {
            e = e7;
            z = false;
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            if (e == null) {
            }
        } catch (IOException e8) {
            e = e8;
            z = false;
            if (xmlResourceParserLoadXmlMetaData != null) {
            }
            if (e == null) {
            }
        } catch (XmlPullParserException e9) {
            e = e9;
            z = false;
            if (xmlResourceParserLoadXmlMetaData != null) {
            }
            if (e == null) {
            }
        }
        try {
            typedArrayObtainAttributes.recycle();
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            e = null;
        } catch (PackageManager.NameNotFoundException e10) {
            e = e10;
            if (xmlResourceParserLoadXmlMetaData != null) {
            }
        } catch (IOException e11) {
            e = e11;
            if (xmlResourceParserLoadXmlMetaData != null) {
            }
        } catch (XmlPullParserException e12) {
            e = e12;
            if (xmlResourceParserLoadXmlMetaData != null) {
            }
        }
        if (e == null) {
            Slog.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName, e);
            return null;
        }
        if (string == null) {
            return null;
        }
        if (string.indexOf(47) < 0) {
            string = resolveInfo.serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + string;
        }
        return new SettingsAttrs(ComponentName.unflattenFromString(string), z);
    }

    private ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private void maybeEnableFactoryTrustAgents(LockPatternUtils lockPatternUtils, int i) {
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 0, i) != 0) {
            return;
        }
        List<ResolveInfo> listResolveAllowedTrustAgents = resolveAllowedTrustAgents(this.mContext.getPackageManager(), i);
        ComponentName defaultFactoryTrustAgent = getDefaultFactoryTrustAgent(this.mContext);
        boolean z = defaultFactoryTrustAgent != null;
        ArraySet arraySet = new ArraySet();
        if (z) {
            arraySet.add(defaultFactoryTrustAgent);
            Log.i(TAG, "Enabling " + defaultFactoryTrustAgent + " because it is a default agent.");
        } else {
            for (ResolveInfo resolveInfo : listResolveAllowedTrustAgents) {
                ComponentName componentName = getComponentName(resolveInfo);
                if ((resolveInfo.serviceInfo.applicationInfo.flags & 1) == 0) {
                    Log.i(TAG, "Leaving agent " + componentName + " disabled because package is not a system package.");
                } else {
                    arraySet.add(componentName);
                }
            }
        }
        List enabledTrustAgents = lockPatternUtils.getEnabledTrustAgents(i);
        if (enabledTrustAgents != null) {
            arraySet.addAll(enabledTrustAgents);
        }
        lockPatternUtils.setEnabledTrustAgents(arraySet, i);
        Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "trust_agents_initialized", 1, i);
    }

    private static ComponentName getDefaultFactoryTrustAgent(Context context) {
        String string = context.getResources().getString(android.R.string.activity_list_empty);
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        return ComponentName.unflattenFromString(string);
    }

    private List<ResolveInfo> resolveAllowedTrustAgents(PackageManager packageManager, int i) {
        List<ResolveInfo> listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(TRUST_AGENT_INTENT, 786560, i);
        ArrayList arrayList = new ArrayList(listQueryIntentServicesAsUser.size());
        for (ResolveInfo resolveInfo : listQueryIntentServicesAsUser) {
            if (resolveInfo.serviceInfo != null && resolveInfo.serviceInfo.applicationInfo != null) {
                if (packageManager.checkPermission(PERMISSION_PROVIDE_AGENT, resolveInfo.serviceInfo.packageName) != 0) {
                    Log.w(TAG, "Skipping agent " + getComponentName(resolveInfo) + " because package does not have permission " + PERMISSION_PROVIDE_AGENT + ".");
                } else {
                    arrayList.add(resolveInfo);
                }
            }
        }
        return arrayList;
    }

    private boolean aggregateIsTrusted(int i) {
        if (!this.mStrongAuthTracker.isTrustAllowedForUser(i)) {
            return false;
        }
        for (int i2 = 0; i2 < this.mActiveAgents.size(); i2++) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(i2);
            if (agentInfoValueAt.userId == i && agentInfoValueAt.agent.isTrusted()) {
                return true;
            }
        }
        return false;
    }

    private boolean aggregateIsTrustManaged(int i) {
        if (!this.mStrongAuthTracker.isTrustAllowedForUser(i)) {
            return false;
        }
        for (int i2 = 0; i2 < this.mActiveAgents.size(); i2++) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(i2);
            if (agentInfoValueAt.userId == i && agentInfoValueAt.agent.isManagingTrust()) {
                return true;
            }
        }
        return false;
    }

    private void dispatchUnlockAttempt(boolean z, int i) {
        if (z) {
            this.mStrongAuthTracker.allowTrustFromUnlock(i);
        }
        for (int i2 = 0; i2 < this.mActiveAgents.size(); i2++) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(i2);
            if (agentInfoValueAt.userId == i) {
                agentInfoValueAt.agent.onUnlockAttempt(z);
            }
        }
    }

    private void dispatchUnlockLockout(int i, int i2) {
        for (int i3 = 0; i3 < this.mActiveAgents.size(); i3++) {
            AgentInfo agentInfoValueAt = this.mActiveAgents.valueAt(i3);
            if (agentInfoValueAt.userId == i2) {
                agentInfoValueAt.agent.onUnlockLockout(i);
            }
        }
    }

    private void addListener(ITrustListener iTrustListener) {
        for (int i = 0; i < this.mTrustListeners.size(); i++) {
            if (this.mTrustListeners.get(i).asBinder() == iTrustListener.asBinder()) {
                return;
            }
        }
        this.mTrustListeners.add(iTrustListener);
        updateTrustAll();
    }

    private void removeListener(ITrustListener iTrustListener) {
        for (int i = 0; i < this.mTrustListeners.size(); i++) {
            if (this.mTrustListeners.get(i).asBinder() == iTrustListener.asBinder()) {
                this.mTrustListeners.remove(i);
                return;
            }
        }
    }

    private void dispatchOnTrustChanged(boolean z, int i, int i2) {
        if (DEBUG) {
            Log.i(TAG, "onTrustChanged(" + z + ", " + i + ", 0x" + Integer.toHexString(i2) + ")");
        }
        int i3 = 0;
        if (!z) {
            i2 = 0;
        }
        while (i3 < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i3).onTrustChanged(z, i, i2);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i3);
                i3--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i3++;
        }
    }

    private void dispatchOnTrustManagedChanged(boolean z, int i) {
        if (DEBUG) {
            Log.i(TAG, "onTrustManagedChanged(" + z + ", " + i + ")");
        }
        int i2 = 0;
        while (i2 < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i2).onTrustManagedChanged(z, i);
            } catch (DeadObjectException e) {
                Slog.d(TAG, "Removing dead TrustListener.");
                this.mTrustListeners.remove(i2);
                i2--;
            } catch (RemoteException e2) {
                Slog.e(TAG, "Exception while notifying TrustListener.", e2);
            }
            i2++;
        }
    }

    private void dispatchOnTrustError(CharSequence charSequence) {
        if (DEBUG) {
            Log.i(TAG, "onTrustError(" + ((Object) charSequence) + ")");
        }
        int i = 0;
        while (i < this.mTrustListeners.size()) {
            try {
                this.mTrustListeners.get(i).onTrustError(charSequence);
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
    public void onStartUser(int i) {
        this.mHandler.obtainMessage(7, i, 0, null).sendToTarget();
    }

    @Override
    public void onCleanupUser(int i) {
        this.mHandler.obtainMessage(8, i, 0, null).sendToTarget();
    }

    @Override
    public void onSwitchUser(int i) {
        this.mHandler.obtainMessage(9, i, 0, null).sendToTarget();
    }

    @Override
    public void onUnlockUser(int i) {
        this.mHandler.obtainMessage(11, i, 0, null).sendToTarget();
    }

    @Override
    public void onStopUser(int i) {
        this.mHandler.obtainMessage(12, i, 0, null).sendToTarget();
    }

    class AnonymousClass1 extends ITrustManager.Stub {
        AnonymousClass1() {
        }

        public void reportUnlockAttempt(boolean z, int i) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(3, z ? 1 : 0, i).sendToTarget();
        }

        public void reportUnlockLockout(int i, int i2) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.obtainMessage(13, i, i2).sendToTarget();
        }

        public void reportEnabledTrustAgentsChanged(int i) throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.removeMessages(4);
            TrustManagerService.this.mHandler.sendEmptyMessage(4);
        }

        public void reportKeyguardShowingChanged() throws RemoteException {
            enforceReportPermission();
            TrustManagerService.this.mHandler.removeMessages(6);
            TrustManagerService.this.mHandler.sendEmptyMessage(6);
            TrustManagerService.this.mHandler.runWithScissors(new Runnable() {
                @Override
                public final void run() {
                    TrustManagerService.AnonymousClass1.lambda$reportKeyguardShowingChanged$0();
                }
            }, 0L);
        }

        static void lambda$reportKeyguardShowingChanged$0() {
        }

        public void registerTrustListener(ITrustListener iTrustListener) throws RemoteException {
            enforceListenerPermission();
            TrustManagerService.this.mHandler.obtainMessage(1, iTrustListener).sendToTarget();
        }

        public void unregisterTrustListener(ITrustListener iTrustListener) throws RemoteException {
            enforceListenerPermission();
            TrustManagerService.this.mHandler.obtainMessage(2, iTrustListener).sendToTarget();
        }

        public boolean isDeviceLocked(int i) throws RemoteException {
            int iHandleIncomingUser = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), i, false, true, "isDeviceLocked", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (!TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(iHandleIncomingUser)) {
                    iHandleIncomingUser = TrustManagerService.this.resolveProfileParent(iHandleIncomingUser);
                }
                return TrustManagerService.this.isDeviceLockedInner(iHandleIncomingUser);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isDeviceSecure(int i) throws RemoteException {
            int iHandleIncomingUser = ActivityManager.handleIncomingUser(getCallingPid(), getCallingUid(), i, false, true, "isDeviceSecure", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (!TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(iHandleIncomingUser)) {
                    iHandleIncomingUser = TrustManagerService.this.resolveProfileParent(iHandleIncomingUser);
                }
                return TrustManagerService.this.mLockPatternUtils.isSecure(iHandleIncomingUser);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void enforceReportPermission() {
            TrustManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_KEYGUARD_SECURE_STORAGE", "reporting trust events");
        }

        private void enforceListenerPermission() {
            TrustManagerService.this.mContext.enforceCallingPermission("android.permission.TRUST_LISTENER", "register trust listener");
        }

        protected void dump(FileDescriptor fileDescriptor, final PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(TrustManagerService.this.mContext, TrustManagerService.TAG, printWriter)) {
                if (!TrustManagerService.this.isSafeMode()) {
                    if (TrustManagerService.this.mTrustAgentsCanRun) {
                        final List users = TrustManagerService.this.mUserManager.getUsers(true);
                        TrustManagerService.this.mHandler.runWithScissors(new Runnable() {
                            @Override
                            public void run() {
                                printWriter.println("Trust manager state:");
                                for (UserInfo userInfo : users) {
                                    AnonymousClass1.this.dumpUser(printWriter, userInfo, userInfo.id == TrustManagerService.this.mCurrentUser);
                                }
                            }
                        }, 1500L);
                        return;
                    } else {
                        printWriter.println("disabled because the third-party apps can't run yet.");
                        return;
                    }
                }
                printWriter.println("disabled because the system is in safe mode.");
            }
        }

        private void dumpUser(PrintWriter printWriter, UserInfo userInfo, boolean z) {
            printWriter.printf(" User \"%s\" (id=%d, flags=%#x)", userInfo.name, Integer.valueOf(userInfo.id), Integer.valueOf(userInfo.flags));
            if (!userInfo.supportsSwitchToByUser()) {
                printWriter.println("(managed profile)");
                printWriter.println("   disabled because switching to this user is not possible.");
                return;
            }
            if (z) {
                printWriter.print(" (current)");
            }
            printWriter.print(": trusted=" + dumpBool(TrustManagerService.this.aggregateIsTrusted(userInfo.id)));
            printWriter.print(", trustManaged=" + dumpBool(TrustManagerService.this.aggregateIsTrustManaged(userInfo.id)));
            printWriter.print(", deviceLocked=" + dumpBool(TrustManagerService.this.isDeviceLockedInner(userInfo.id)));
            printWriter.print(", strongAuthRequired=" + dumpHex(TrustManagerService.this.mStrongAuthTracker.getStrongAuthForUser(userInfo.id)));
            printWriter.println();
            printWriter.println("   Enabled agents:");
            ArraySet arraySet = new ArraySet();
            boolean z2 = false;
            for (AgentInfo agentInfo : TrustManagerService.this.mActiveAgents) {
                if (agentInfo.userId == userInfo.id) {
                    boolean zIsTrusted = agentInfo.agent.isTrusted();
                    printWriter.print("    ");
                    printWriter.println(agentInfo.component.flattenToShortString());
                    printWriter.print("     bound=" + dumpBool(agentInfo.agent.isBound()));
                    printWriter.print(", connected=" + dumpBool(agentInfo.agent.isConnected()));
                    printWriter.print(", managingTrust=" + dumpBool(agentInfo.agent.isManagingTrust()));
                    printWriter.print(", trusted=" + dumpBool(zIsTrusted));
                    printWriter.println();
                    if (zIsTrusted) {
                        printWriter.println("      message=\"" + ((Object) agentInfo.agent.getMessage()) + "\"");
                    }
                    if (!agentInfo.agent.isConnected()) {
                        printWriter.println("      restartScheduledAt=" + TrustArchive.formatDuration(agentInfo.agent.getScheduledRestartUptimeMillis() - SystemClock.uptimeMillis()));
                    }
                    if (!arraySet.add(TrustArchive.getSimpleName(agentInfo.component))) {
                        z2 = true;
                    }
                }
            }
            printWriter.println("   Events:");
            TrustManagerService.this.mArchive.dump(printWriter, 50, userInfo.id, "    ", z2);
            printWriter.println();
        }

        private String dumpBool(boolean z) {
            return z ? "1" : "0";
        }

        private String dumpHex(int i) {
            return "0x" + Integer.toHexString(i);
        }

        public void setDeviceLockedForUser(int i, boolean z) {
            enforceReportPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (TrustManagerService.this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i)) {
                    synchronized (TrustManagerService.this.mDeviceLockedForUser) {
                        TrustManagerService.this.mDeviceLockedForUser.put(i, z);
                    }
                    KeyStore.getInstance().onUserLockedStateChanged(i, z);
                    if (z) {
                        try {
                            ActivityManager.getService().notifyLockedProfile(i);
                        } catch (RemoteException e) {
                        }
                    }
                    Intent intent = new Intent("android.intent.action.DEVICE_LOCKED_CHANGED");
                    intent.addFlags(1073741824);
                    intent.putExtra("android.intent.extra.user_handle", i);
                    TrustManagerService.this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM, "android.permission.TRUST_LISTENER", null);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isTrustUsuallyManaged(int i) {
            TrustManagerService.this.mContext.enforceCallingPermission("android.permission.TRUST_LISTENER", "query trust state");
            return TrustManagerService.this.isTrustUsuallyManagedInternal(i);
        }

        public void unlockedByFingerprintForUser(int i) {
            enforceReportPermission();
            synchronized (TrustManagerService.this.mUsersUnlockedByFingerprint) {
                TrustManagerService.this.mUsersUnlockedByFingerprint.put(i, true);
            }
            TrustManagerService.this.mHandler.obtainMessage(14, i, 0).sendToTarget();
        }

        public void clearAllFingerprints() {
            enforceReportPermission();
            synchronized (TrustManagerService.this.mUsersUnlockedByFingerprint) {
                TrustManagerService.this.mUsersUnlockedByFingerprint.clear();
            }
            TrustManagerService.this.mHandler.obtainMessage(14, -1, 0).sendToTarget();
        }
    }

    private boolean isTrustUsuallyManagedInternal(int i) {
        synchronized (this.mTrustUsuallyManagedForUser) {
            int iIndexOfKey = this.mTrustUsuallyManagedForUser.indexOfKey(i);
            if (iIndexOfKey >= 0) {
                return this.mTrustUsuallyManagedForUser.valueAt(iIndexOfKey);
            }
            boolean zIsTrustUsuallyManaged = this.mLockPatternUtils.isTrustUsuallyManaged(i);
            synchronized (this.mTrustUsuallyManagedForUser) {
                int iIndexOfKey2 = this.mTrustUsuallyManagedForUser.indexOfKey(i);
                if (iIndexOfKey2 >= 0) {
                    return this.mTrustUsuallyManagedForUser.valueAt(iIndexOfKey2);
                }
                this.mTrustUsuallyManagedForUser.put(i, zIsTrustUsuallyManaged);
                return zIsTrustUsuallyManaged;
            }
        }
    }

    private int resolveProfileParent(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserInfo profileParent = this.mUserManager.getProfileParent(i);
            if (profileParent != null) {
                return profileParent.getUserHandle().getIdentifier();
            }
            return i;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private static class SettingsAttrs {
        public boolean canUnlockProfile;
        public ComponentName componentName;

        public SettingsAttrs(ComponentName componentName, boolean z) {
            this.componentName = componentName;
            this.canUnlockProfile = z;
        }
    }

    private class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        Receiver(TrustManagerService trustManagerService, AnonymousClass1 anonymousClass1) {
            this();
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
            if ("android.intent.action.USER_ADDED".equals(action)) {
                int userId2 = getUserId(intent);
                if (userId2 > 0) {
                    TrustManagerService.this.maybeEnableFactoryTrustAgents(TrustManagerService.this.mLockPatternUtils, userId2);
                    return;
                }
                return;
            }
            if ("android.intent.action.USER_REMOVED".equals(action) && (userId = getUserId(intent)) > 0) {
                synchronized (TrustManagerService.this.mUserIsTrusted) {
                    TrustManagerService.this.mUserIsTrusted.delete(userId);
                }
                synchronized (TrustManagerService.this.mDeviceLockedForUser) {
                    TrustManagerService.this.mDeviceLockedForUser.delete(userId);
                }
                synchronized (TrustManagerService.this.mTrustUsuallyManagedForUser) {
                    TrustManagerService.this.mTrustUsuallyManagedForUser.delete(userId);
                }
                synchronized (TrustManagerService.this.mUsersUnlockedByFingerprint) {
                    TrustManagerService.this.mUsersUnlockedByFingerprint.delete(userId);
                }
                TrustManagerService.this.refreshAgentList(userId);
                TrustManagerService.this.refreshDeviceLockedForUser(userId);
            }
        }

        private int getUserId(Intent intent) {
            int intExtra = intent.getIntExtra("android.intent.extra.user_handle", -100);
            if (intExtra > 0) {
                return intExtra;
            }
            Slog.wtf(TrustManagerService.TAG, "EXTRA_USER_HANDLE missing or invalid, value=" + intExtra);
            return -100;
        }

        public void register(Context context) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
            intentFilter.addAction("android.intent.action.USER_ADDED");
            intentFilter.addAction("android.intent.action.USER_REMOVED");
            context.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, null, null);
        }
    }

    private class StrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        SparseBooleanArray mStartFromSuccessfulUnlock;

        public StrongAuthTracker(Context context) {
            super(context);
            this.mStartFromSuccessfulUnlock = new SparseBooleanArray();
        }

        public void onStrongAuthRequiredChanged(int i) {
            this.mStartFromSuccessfulUnlock.delete(i);
            if (TrustManagerService.DEBUG) {
                Log.i(TrustManagerService.TAG, "onStrongAuthRequiredChanged(" + i + ") -> trustAllowed=" + isTrustAllowedForUser(i) + " agentsCanRun=" + canAgentsRunForUser(i));
            }
            TrustManagerService.this.refreshAgentList(i);
            TrustManagerService.this.updateTrust(i, 0);
        }

        boolean canAgentsRunForUser(int i) {
            return this.mStartFromSuccessfulUnlock.get(i) || super.isTrustAllowedForUser(i);
        }

        void allowTrustFromUnlock(int i) {
            if (i < 0) {
                throw new IllegalArgumentException("userId must be a valid user: " + i);
            }
            boolean zCanAgentsRunForUser = canAgentsRunForUser(i);
            this.mStartFromSuccessfulUnlock.put(i, true);
            if (TrustManagerService.DEBUG) {
                Log.i(TrustManagerService.TAG, "allowTrustFromUnlock(" + i + ") -> trustAllowed=" + isTrustAllowedForUser(i) + " agentsCanRun=" + canAgentsRunForUser(i));
            }
            if (canAgentsRunForUser(i) != zCanAgentsRunForUser) {
                TrustManagerService.this.refreshAgentList(i);
            }
        }
    }
}
