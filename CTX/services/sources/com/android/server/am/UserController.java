package com.android.server.am;

import android.R;
import android.app.AppGlobals;
import android.app.Dialog;
import android.app.IStopUserCallback;
import android.app.IUserSwitchObserver;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IProgressListener;
import android.os.IRemoteCallback;
import android.os.IUserManager;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.IStorageManager;
import android.os.storage.StorageManager;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimingsTraceLog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemServiceManager;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import com.android.server.pm.UserManagerService;
import com.android.server.wm.WindowManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class UserController implements Handler.Callback {
    static final int CONTINUE_USER_SWITCH_MSG = 20;
    static final int FOREGROUND_PROFILE_CHANGED_MSG = 70;
    static final int REPORT_LOCKED_BOOT_COMPLETE_MSG = 110;
    static final int REPORT_USER_SWITCH_COMPLETE_MSG = 80;
    static final int REPORT_USER_SWITCH_MSG = 10;
    static final int START_PROFILES_MSG = 40;
    static final int START_USER_SWITCH_FG_MSG = 120;
    static final int START_USER_SWITCH_UI_MSG = 1000;
    static final int SYSTEM_USER_CURRENT_MSG = 60;
    static final int SYSTEM_USER_START_MSG = 50;
    static final int SYSTEM_USER_UNLOCK_MSG = 100;
    private static final String TAG = "ActivityManager";
    private static final int USER_SWITCH_CALLBACKS_TIMEOUT_MS = 5000;
    static final int USER_SWITCH_CALLBACKS_TIMEOUT_MSG = 90;
    static final int USER_SWITCH_TIMEOUT_MS = 3000;
    static final int USER_SWITCH_TIMEOUT_MSG = 30;

    @GuardedBy("mLock")
    private volatile ArraySet<String> mCurWaitingUserSwitchCallbacks;

    @GuardedBy("mLock")
    private int[] mCurrentProfileIds;

    @GuardedBy("mLock")
    private volatile int mCurrentUserId;
    private final Handler mHandler;
    private final Injector mInjector;
    private final Object mLock;
    private final LockPatternUtils mLockPatternUtils;
    int mMaxRunningUsers;

    @GuardedBy("mLock")
    private int[] mStartedUserArray;

    @GuardedBy("mLock")
    private final SparseArray<UserState> mStartedUsers;

    @GuardedBy("mLock")
    private String mSwitchingFromSystemUserMessage;

    @GuardedBy("mLock")
    private String mSwitchingToSystemUserMessage;

    @GuardedBy("mLock")
    private volatile int mTargetUserId;

    @GuardedBy("mLock")
    private ArraySet<String> mTimeoutUserSwitchCallbacks;
    private final Handler mUiHandler;

    @GuardedBy("mLock")
    private final ArrayList<Integer> mUserLru;

    @GuardedBy("mLock")
    private final SparseIntArray mUserProfileGroupIds;
    private final RemoteCallbackList<IUserSwitchObserver> mUserSwitchObservers;
    boolean mUserSwitchUiEnabled;

    UserController(ActivityManagerService activityManagerService) {
        this(new Injector(activityManagerService));
    }

    @VisibleForTesting
    UserController(Injector injector) {
        this.mLock = new Object();
        this.mCurrentUserId = 0;
        this.mTargetUserId = -10000;
        this.mStartedUsers = new SparseArray<>();
        this.mUserLru = new ArrayList<>();
        this.mStartedUserArray = new int[]{0};
        this.mCurrentProfileIds = new int[0];
        this.mUserProfileGroupIds = new SparseIntArray();
        this.mUserSwitchObservers = new RemoteCallbackList<>();
        this.mUserSwitchUiEnabled = true;
        this.mInjector = injector;
        this.mHandler = this.mInjector.getHandler(this);
        this.mUiHandler = this.mInjector.getUiHandler(this);
        UserState userState = new UserState(UserHandle.SYSTEM);
        userState.mUnlockProgress.addListener(new UserProgressListener());
        this.mStartedUsers.put(0, userState);
        this.mUserLru.add(0);
        this.mLockPatternUtils = this.mInjector.getLockPatternUtils();
        updateStartedUserArrayLU();
    }

    void finishUserSwitch(UserState userState) {
        finishUserBoot(userState);
        startProfiles();
        synchronized (this.mLock) {
            stopRunningUsersLU(this.mMaxRunningUsers);
        }
    }

    List<Integer> getRunningUsersLU() {
        ArrayList arrayList = new ArrayList();
        for (Integer num : this.mUserLru) {
            UserState userState = this.mStartedUsers.get(num.intValue());
            if (userState != null && userState.state != 4 && userState.state != 5 && (num.intValue() != 0 || !UserInfo.isSystemOnly(num.intValue()))) {
                arrayList.add(num);
            }
        }
        return arrayList;
    }

    void stopRunningUsersLU(int i) {
        List<Integer> runningUsersLU = getRunningUsersLU();
        Iterator<Integer> it = runningUsersLU.iterator();
        while (runningUsersLU.size() > i && it.hasNext()) {
            Integer next = it.next();
            if (next.intValue() != 0 && next.intValue() != this.mCurrentUserId && stopUsersLU(next.intValue(), false, null) == 0) {
                it.remove();
            }
        }
    }

    boolean canStartMoreUsers() {
        boolean z;
        synchronized (this.mLock) {
            z = getRunningUsersLU().size() < this.mMaxRunningUsers;
        }
        return z;
    }

    private void finishUserBoot(UserState userState) {
        finishUserBoot(userState, null);
    }

    private void finishUserBoot(UserState userState, IIntentReceiver iIntentReceiver) {
        int identifier = userState.mHandle.getIdentifier();
        Slog.d(TAG, "Finishing user boot " + identifier);
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(identifier) != userState) {
                return;
            }
            if (userState.setState(0, 1)) {
                this.mInjector.getUserManagerInternal().setUserState(identifier, userState.state);
                if (identifier == 0 && !this.mInjector.isRuntimeRestarted() && !this.mInjector.isFirstBootOrUpgrade()) {
                    int iElapsedRealtime = (int) (SystemClock.elapsedRealtime() / 1000);
                    MetricsLogger.histogram(this.mInjector.getContext(), "framework_locked_boot_completed", iElapsedRealtime);
                    if (iElapsedRealtime > START_USER_SWITCH_FG_MSG) {
                        if ("user".equals(Build.TYPE)) {
                            Slog.wtf("SystemServerTiming", "finishUserBoot took too long. uptimeSeconds=" + iElapsedRealtime);
                        } else {
                            Slog.w("SystemServerTiming", "finishUserBoot took too long. uptimeSeconds=" + iElapsedRealtime);
                        }
                    }
                }
                this.mHandler.sendMessage(this.mHandler.obtainMessage(110, identifier, 0));
                Intent intent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED", (Uri) null);
                intent.putExtra("android.intent.extra.user_handle", identifier);
                intent.addFlags(150994944);
                this.mInjector.broadcastIntent(intent, null, iIntentReceiver, 0, null, null, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, identifier);
            }
            if (this.mInjector.getUserManager().isManagedProfile(identifier)) {
                UserInfo profileParent = this.mInjector.getUserManager().getProfileParent(identifier);
                if (profileParent != null && isUserRunning(profileParent.id, 4)) {
                    Slog.d(TAG, "User " + identifier + " (parent " + profileParent.id + "): attempting unlock because parent is unlocked");
                    maybeUnlockUser(identifier);
                    return;
                }
                Slog.d(TAG, "User " + identifier + " (parent " + (profileParent == null ? "<null>" : String.valueOf(profileParent.id)) + "): delaying unlock because parent is locked");
                return;
            }
            maybeUnlockUser(identifier);
        }
    }

    private void finishUserUnlocking(final UserState userState) {
        final int identifier = userState.mHandle.getIdentifier();
        if (StorageManager.isUserKeyUnlocked(identifier)) {
            synchronized (this.mLock) {
                if (this.mStartedUsers.get(identifier) == userState && userState.state == 1) {
                    userState.mUnlockProgress.start();
                    userState.mUnlockProgress.setProgress(5, this.mInjector.getContext().getString(R.string.PERSOSUBSTATE_RUIM_HRPD_PUK_SUCCESS));
                    FgThread.getHandler().post(new Runnable() {
                        @Override
                        public final void run() {
                            UserController.lambda$finishUserUnlocking$0(this.f$0, identifier, userState);
                        }
                    });
                }
            }
        }
    }

    public static void lambda$finishUserUnlocking$0(UserController userController, int i, UserState userState) {
        if (!StorageManager.isUserKeyUnlocked(i)) {
            Slog.w(TAG, "User key got locked unexpectedly, leaving user locked.");
            return;
        }
        userController.mInjector.getUserManager().onBeforeUnlockUser(i);
        synchronized (userController.mLock) {
            if (userState.setState(1, 2)) {
                userController.mInjector.getUserManagerInternal().setUserState(i, userState.state);
                userState.mUnlockProgress.setProgress(20);
                userController.mHandler.obtainMessage(100, i, 0, userState).sendToTarget();
            }
        }
    }

    void finishUserUnlocked(final UserState userState) {
        UserInfo profileParent;
        int identifier = userState.mHandle.getIdentifier();
        if (StorageManager.isUserKeyUnlocked(identifier)) {
            synchronized (this.mLock) {
                if (this.mStartedUsers.get(userState.mHandle.getIdentifier()) != userState) {
                    return;
                }
                if (userState.setState(2, 3)) {
                    this.mInjector.getUserManagerInternal().setUserState(identifier, userState.state);
                    userState.mUnlockProgress.finish();
                    if (identifier == 0) {
                        this.mInjector.startPersistentApps(DumpState.DUMP_DOMAIN_PREFERRED);
                    }
                    this.mInjector.installEncryptionUnawareProviders(identifier);
                    Intent intent = new Intent("android.intent.action.USER_UNLOCKED");
                    intent.putExtra("android.intent.extra.user_handle", identifier);
                    intent.addFlags(1342177280);
                    this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, identifier);
                    if (getUserInfo(identifier).isManagedProfile() && (profileParent = this.mInjector.getUserManager().getProfileParent(identifier)) != null) {
                        Intent intent2 = new Intent("android.intent.action.MANAGED_PROFILE_UNLOCKED");
                        intent2.putExtra("android.intent.extra.USER", UserHandle.of(identifier));
                        intent2.addFlags(1342177280);
                        this.mInjector.broadcastIntent(intent2, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, profileParent.id);
                    }
                    UserInfo userInfo = getUserInfo(identifier);
                    if (!Objects.equals(userInfo.lastLoggedInFingerprint, Build.FINGERPRINT)) {
                        boolean z = false;
                        if (userInfo.isManagedProfile() && (!userState.tokenProvided || !this.mLockPatternUtils.isSeparateProfileChallengeEnabled(identifier))) {
                            z = true;
                        }
                        this.mInjector.sendPreBootBroadcast(identifier, z, new Runnable() {
                            @Override
                            public final void run() {
                                this.f$0.finishUserUnlockedCompleted(userState);
                            }
                        });
                        return;
                    }
                    finishUserUnlockedCompleted(userState);
                }
            }
        }
    }

    private void finishUserUnlockedCompleted(UserState userState) {
        final int identifier = userState.mHandle.getIdentifier();
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(userState.mHandle.getIdentifier()) != userState) {
                return;
            }
            final UserInfo userInfo = getUserInfo(identifier);
            if (userInfo != null && StorageManager.isUserKeyUnlocked(identifier)) {
                this.mInjector.getUserManager().onUserLoggedIn(identifier);
                if (!userInfo.isInitialized() && identifier != 0) {
                    Slog.d(TAG, "Initializing user #" + identifier);
                    Intent intent = new Intent("android.intent.action.USER_INITIALIZE");
                    intent.addFlags(285212672);
                    this.mInjector.broadcastIntent(intent, null, new IIntentReceiver.Stub() {
                        public void performReceive(Intent intent2, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
                            UserController.this.mInjector.getUserManager().makeInitialized(userInfo.id);
                        }
                    }, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, identifier);
                }
                Slog.i(TAG, "Sending BOOT_COMPLETE user #" + identifier);
                if (identifier == 0 && !this.mInjector.isRuntimeRestarted() && !this.mInjector.isFirstBootOrUpgrade()) {
                    MetricsLogger.histogram(this.mInjector.getContext(), "framework_boot_completed", (int) (SystemClock.elapsedRealtime() / 1000));
                }
                Intent intent2 = new Intent("android.intent.action.BOOT_COMPLETED", (Uri) null);
                intent2.putExtra("android.intent.extra.user_handle", identifier);
                intent2.addFlags(150994944);
                this.mInjector.broadcastIntent(intent2, null, new IIntentReceiver.Stub() {
                    public void performReceive(Intent intent3, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) throws RemoteException {
                        Slog.i(UserController.TAG, "Finished processing BOOT_COMPLETED for u" + identifier);
                    }
                }, 0, null, null, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, identifier);
            }
        }
    }

    class AnonymousClass3 extends IStopUserCallback.Stub {
        final boolean val$foreground;

        AnonymousClass3(boolean z) {
            this.val$foreground = z;
        }

        public void userStopped(final int i) {
            Handler handler = UserController.this.mHandler;
            final boolean z = this.val$foreground;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    UserController.this.startUser(i, z);
                }
            });
        }

        public void userStopAborted(int i) {
        }
    }

    int restartUser(int i, boolean z) {
        return stopUser(i, true, new AnonymousClass3(z));
    }

    int stopUser(int i, boolean z, IStopUserCallback iStopUserCallback) {
        int iStopUsersLU;
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String str = "Permission Denial: switchUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, str);
            throw new SecurityException(str);
        }
        if (i < 0 || i == 0) {
            throw new IllegalArgumentException("Can't stop system user " + i);
        }
        enforceShellRestriction("no_debugging_features", i);
        synchronized (this.mLock) {
            iStopUsersLU = stopUsersLU(i, z, iStopUserCallback);
        }
        return iStopUsersLU;
    }

    private int stopUsersLU(int i, boolean z, IStopUserCallback iStopUserCallback) {
        if (i == 0) {
            return -3;
        }
        if (isCurrentUserLU(i)) {
            return -2;
        }
        int[] usersToStopLU = getUsersToStopLU(i);
        for (int i2 : usersToStopLU) {
            if (i2 == 0 || isCurrentUserLU(i2)) {
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    Slog.i(TAG, "stopUsersLocked cannot stop related user " + i2);
                }
                if (z) {
                    Slog.i(TAG, "Force stop user " + i + ". Related users will not be stopped");
                    stopSingleUserLU(i, iStopUserCallback);
                    return 0;
                }
                return -4;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i(TAG, "stopUsersLocked usersToStop=" + Arrays.toString(usersToStopLU));
        }
        int length = usersToStopLU.length;
        for (int i3 = 0; i3 < length; i3++) {
            int i4 = usersToStopLU[i3];
            stopSingleUserLU(i4, i4 == i ? iStopUserCallback : null);
        }
        return 0;
    }

    private void stopSingleUserLU(final int i, final IStopUserCallback iStopUserCallback) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i(TAG, "stopSingleUserLocked userId=" + i);
        }
        final UserState userState = this.mStartedUsers.get(i);
        if (userState == null) {
            if (iStopUserCallback != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        iStopUserCallback.userStopped(i);
                    }
                });
                return;
            }
            return;
        }
        if (iStopUserCallback != null) {
            userState.mStopCallbacks.add(iStopUserCallback);
        }
        if (userState.state != 4 && userState.state != 5) {
            userState.setState(4);
            this.mInjector.getUserManagerInternal().setUserState(i, userState.state);
            updateStartedUserArrayLU();
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    UserController.lambda$stopSingleUserLU$3(this.f$0, i, userState);
                }
            });
        }
    }

    public static void lambda$stopSingleUserLU$3(UserController userController, int i, UserState userState) {
        Intent intent = new Intent("android.intent.action.USER_STOPPING");
        intent.addFlags(1073741824);
        intent.putExtra("android.intent.extra.user_handle", i);
        intent.putExtra("android.intent.extra.SHUTDOWN_USERSPACE_ONLY", true);
        IIntentReceiver anonymousClass4 = userController.new AnonymousClass4(i, userState);
        userController.mInjector.clearBroadcastQueueForUser(i);
        userController.mInjector.broadcastIntent(intent, null, anonymousClass4, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, -1);
    }

    class AnonymousClass4 extends IIntentReceiver.Stub {
        final int val$userId;
        final UserState val$uss;

        AnonymousClass4(int i, UserState userState) {
            this.val$userId = i;
            this.val$uss = userState;
        }

        public void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) {
            Handler handler = UserController.this.mHandler;
            final int i3 = this.val$userId;
            final UserState userState = this.val$uss;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    UserController.this.finishUserStopping(i3, userState);
                }
            });
        }
    }

    void finishUserStopping(int i, final UserState userState) {
        Intent intent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        IIntentReceiver iIntentReceiver = new IIntentReceiver.Stub() {
            public void performReceive(Intent intent2, int i2, String str, Bundle bundle, boolean z, boolean z2, int i3) {
                UserController.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        UserController.this.finishUserStopped(userState);
                    }
                });
            }
        };
        synchronized (this.mLock) {
            if (userState.state != 4) {
                return;
            }
            userState.setState(5);
            this.mInjector.getUserManagerInternal().setUserState(i, userState.state);
            this.mInjector.batteryStatsServiceNoteEvent(16391, Integer.toString(i), i);
            this.mInjector.getSystemServiceManager().stopUser(i);
            this.mInjector.broadcastIntent(intent, null, iIntentReceiver, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, i);
        }
    }

    void finishUserStopped(UserState userState) {
        ArrayList arrayList;
        int i;
        boolean z;
        final int identifier = userState.mHandle.getIdentifier();
        synchronized (this.mLock) {
            arrayList = new ArrayList(userState.mStopCallbacks);
            if (this.mStartedUsers.get(identifier) == userState && userState.state == 5) {
                this.mStartedUsers.remove(identifier);
                this.mUserLru.remove(Integer.valueOf(identifier));
                updateStartedUserArrayLU();
                z = true;
            } else {
                z = false;
            }
        }
        if (z) {
            this.mInjector.getUserManagerInternal().removeUserState(identifier);
            this.mInjector.activityManagerOnUserStopped(identifier);
            forceStopUser(identifier, "finish user");
        }
        for (i = 0; i < arrayList.size(); i++) {
            if (z) {
                try {
                    ((IStopUserCallback) arrayList.get(i)).userStopped(identifier);
                } catch (RemoteException e) {
                }
            } else {
                ((IStopUserCallback) arrayList.get(i)).userStopAborted(identifier);
            }
        }
        if (z) {
            this.mInjector.systemServiceManagerCleanupUser(identifier);
            this.mInjector.stackSupervisorRemoveUser(identifier);
            if (getUserInfo(identifier).isEphemeral()) {
                this.mInjector.getUserManager().removeUserEvenWhenDisallowed(identifier);
            }
            FgThread.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    UserController.lambda$finishUserStopped$4(this.f$0, identifier);
                }
            });
        }
    }

    public static void lambda$finishUserStopped$4(UserController userController, int i) {
        synchronized (userController.mLock) {
            if (userController.mStartedUsers.get(i) != null) {
                Slog.w(TAG, "User was restarted, skipping key eviction");
                return;
            }
            try {
                userController.getStorageManager().lockUserKey(i);
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            }
        }
    }

    private int[] getUsersToStopLU(int i) {
        int size = this.mStartedUsers.size();
        IntArray intArray = new IntArray();
        intArray.add(i);
        int i2 = this.mUserProfileGroupIds.get(i, -10000);
        for (int i3 = 0; i3 < size; i3++) {
            int identifier = this.mStartedUsers.valueAt(i3).mHandle.getIdentifier();
            boolean z = true;
            boolean z2 = i2 != -10000 && i2 == this.mUserProfileGroupIds.get(identifier, -10000);
            if (identifier != i) {
                z = false;
            }
            if (z2 && !z) {
                intArray.add(identifier);
            }
        }
        return intArray.toArray();
    }

    private void forceStopUser(int i, String str) {
        this.mInjector.activityManagerForceStopPackage(i, str);
        Intent intent = new Intent("android.intent.action.USER_STOPPED");
        intent.addFlags(1342177280);
        intent.putExtra("android.intent.extra.user_handle", i);
        this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, -1);
    }

    private void stopGuestOrEphemeralUserIfBackground(int i) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i(TAG, "Stop guest or ephemeral user if background: " + i);
        }
        synchronized (this.mLock) {
            UserState userState = this.mStartedUsers.get(i);
            if (i != 0 && i != this.mCurrentUserId && userState != null && userState.state != 4 && userState.state != 5) {
                UserInfo userInfo = getUserInfo(i);
                if (userInfo.isEphemeral()) {
                    ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).onEphemeralUserStop(i);
                }
                if (userInfo.isGuest() || userInfo.isEphemeral()) {
                    synchronized (this.mLock) {
                        stopUsersLU(i, true, null);
                    }
                }
            }
        }
    }

    void scheduleStartProfiles() {
        if (!this.mHandler.hasMessages(40)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(40), 1000L);
        }
    }

    void startProfiles() {
        int currentUserId = getCurrentUserId();
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i(TAG, "startProfilesLocked");
        }
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(currentUserId, false);
        ArrayList arrayList = new ArrayList(profiles.size());
        for (UserInfo userInfo : profiles) {
            if ((userInfo.flags & 16) == 16 && userInfo.id != currentUserId && !userInfo.isQuietModeEnabled()) {
                arrayList.add(userInfo);
            }
        }
        int size = arrayList.size();
        int i = 0;
        while (i < size && i < this.mMaxRunningUsers - 1) {
            startUser(((UserInfo) arrayList.get(i)).id, false);
            i++;
        }
        if (i < size) {
            Slog.w(TAG, "More profiles than MAX_RUNNING_USERS");
        }
    }

    private IStorageManager getStorageManager() {
        return IStorageManager.Stub.asInterface(ServiceManager.getService("mount"));
    }

    boolean startUser(int i, boolean z) {
        return startUser(i, z, null);
    }

    boolean startUser(final int i, final boolean z, final IProgressListener iProgressListener) throws Throwable {
        long currentProfileIds;
        Object[] objArr;
        UserState userState;
        Object[] objArr2;
        Object[] objArr3;
        UserState userState2;
        boolean z2;
        int i2;
        long j;
        boolean z3;
        int i3;
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String str = "Permission Denial: switchUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, str);
            throw new SecurityException(str);
        }
        Slog.i(TAG, "Starting userid:" + i + " fg:" + z);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int currentUserId = getCurrentUserId();
            if (currentUserId == i) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return true;
            }
            if (z) {
                this.mInjector.clearAllLockedTasks("startUser");
            }
            UserInfo userInfo = getUserInfo(i);
            currentProfileIds = 0;
            currentProfileIds = 0;
            if (userInfo == null) {
                Slog.w(TAG, "No user info for user #" + i);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return false;
            }
            if (z && userInfo.isManagedProfile()) {
                Slog.w(TAG, "Cannot switch to User #" + i + ": not a full user");
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                return false;
            }
            if (z && this.mUserSwitchUiEnabled) {
                this.mInjector.getWindowManager().startFreezingScreen(R.anim.rotation_animation_enter, R.anim.resolver_launch_anim);
            }
            try {
                synchronized (this.mLock) {
                    try {
                        UserState userState3 = this.mStartedUsers.get(i);
                        if (userState3 == null) {
                            UserState userState4 = new UserState(UserHandle.of(i));
                            userState4.mUnlockProgress.addListener(new UserProgressListener());
                            this.mStartedUsers.put(i, userState4);
                            updateStartedUserArrayLU();
                            userState = userState4;
                            objArr2 = true;
                            objArr = true;
                        } else {
                            if (userState3.state == 5 && !isCallingOnHandlerThread()) {
                                Slog.i(TAG, "User #" + i + " is shutting down - will start after full stop");
                                this.mHandler.post(new Runnable() {
                                    @Override
                                    public final void run() throws Throwable {
                                        this.f$0.startUser(i, z, iProgressListener);
                                    }
                                });
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                                return true;
                            }
                            objArr = false;
                            userState = userState3;
                            objArr2 = false;
                        }
                        Integer numValueOf = Integer.valueOf(i);
                        this.mUserLru.remove(numValueOf);
                        this.mUserLru.add(numValueOf);
                        if (iProgressListener != null) {
                            userState.mUnlockProgress.addListener(iProgressListener);
                        }
                        if (objArr2 != false) {
                            this.mInjector.getUserManagerInternal().setUserState(i, userState.state);
                        }
                        if (z) {
                            this.mInjector.reportGlobalUsageEventLocked(16);
                            synchronized (this.mLock) {
                                this.mCurrentUserId = i;
                                this.mTargetUserId = -10000;
                            }
                            this.mInjector.updateUserConfiguration();
                            updateCurrentProfileIds();
                            this.mInjector.getWindowManager().setCurrentUser(i, getCurrentProfileIds());
                            this.mInjector.reportCurWakefulnessUsageEvent();
                            if (this.mUserSwitchUiEnabled) {
                                this.mInjector.getWindowManager().setSwitchingUser(true);
                                this.mInjector.getWindowManager().lockNow(null);
                            }
                        } else {
                            Integer numValueOf2 = Integer.valueOf(this.mCurrentUserId);
                            updateCurrentProfileIds();
                            WindowManagerService windowManager = this.mInjector.getWindowManager();
                            currentProfileIds = getCurrentProfileIds();
                            windowManager.setCurrentProfileIds(currentProfileIds);
                            synchronized (this.mLock) {
                                try {
                                    this.mUserLru.remove(numValueOf2);
                                    currentProfileIds = this.mUserLru;
                                    currentProfileIds.add(numValueOf2);
                                } catch (Throwable th) {
                                    th = th;
                                    while (true) {
                                        try {
                                            throw th;
                                        } catch (Throwable th2) {
                                            th = th2;
                                        }
                                    }
                                }
                            }
                        }
                        if (userState.state == 4) {
                            userState.setState(userState.lastState);
                            this.mInjector.getUserManagerInternal().setUserState(i, userState.state);
                            synchronized (this.mLock) {
                                updateStartedUserArrayLU();
                            }
                        } else {
                            if (userState.state != 5) {
                                objArr3 = objArr;
                                if (userState.state == 0) {
                                    this.mInjector.getUserManager().onBeforeStartUser(i);
                                    this.mHandler.sendMessage(this.mHandler.obtainMessage(50, i, 0));
                                }
                                if (z) {
                                    this.mHandler.sendMessage(this.mHandler.obtainMessage(60, i, currentUserId));
                                    this.mHandler.removeMessages(10);
                                    this.mHandler.removeMessages(30);
                                    this.mHandler.sendMessage(this.mHandler.obtainMessage(10, currentUserId, i, userState));
                                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(30, currentUserId, i, userState), 3000L);
                                }
                                if (objArr3 == true) {
                                    userState2 = userState;
                                    z2 = true;
                                    i2 = currentUserId;
                                    j = jClearCallingIdentity;
                                    z3 = z;
                                } else {
                                    Intent intent = new Intent("android.intent.action.USER_STARTED");
                                    intent.addFlags(1342177280);
                                    intent.putExtra("android.intent.extra.user_handle", i);
                                    userState2 = userState;
                                    z2 = true;
                                    i2 = currentUserId;
                                    j = jClearCallingIdentity;
                                    z3 = z;
                                    try {
                                        this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, i);
                                    } catch (Throwable th3) {
                                        th = th3;
                                        currentProfileIds = j;
                                    }
                                }
                                if (z3) {
                                    i3 = i;
                                    finishUserBoot(userState2);
                                } else {
                                    i3 = i;
                                    moveUserToForeground(userState2, i2, i3);
                                }
                                if (objArr3 != false) {
                                    Intent intent2 = new Intent("android.intent.action.USER_STARTING");
                                    intent2.addFlags(1073741824);
                                    intent2.putExtra("android.intent.extra.user_handle", i3);
                                    this.mInjector.broadcastIntent(intent2, null, new IIntentReceiver.Stub() {
                                        public void performReceive(Intent intent3, int i4, String str2, Bundle bundle, boolean z4, boolean z5, int i5) throws RemoteException {
                                        }
                                    }, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, -1);
                                }
                                Binder.restoreCallingIdentity(j);
                                return z2;
                            }
                            userState.setState(0);
                            this.mInjector.getUserManagerInternal().setUserState(i, userState.state);
                            synchronized (this.mLock) {
                                updateStartedUserArrayLU();
                            }
                        }
                        objArr3 = true;
                        if (userState.state == 0) {
                        }
                        if (z) {
                        }
                        if (objArr3 == true) {
                        }
                        if (z3) {
                        }
                        if (objArr3 != false) {
                        }
                        Binder.restoreCallingIdentity(j);
                        return z2;
                    } catch (Throwable th4) {
                        th = th4;
                        while (true) {
                            try {
                                throw th;
                            } catch (Throwable th5) {
                                th = th5;
                            }
                        }
                    }
                }
            } catch (Throwable th6) {
                th = th6;
            }
        } catch (Throwable th7) {
            th = th7;
            currentProfileIds = jClearCallingIdentity;
        }
        Binder.restoreCallingIdentity(currentProfileIds);
        throw th;
    }

    private boolean isCallingOnHandlerThread() {
        return Looper.myLooper() == this.mHandler.getLooper();
    }

    void startUserInForeground(int i) {
        if (!startUser(i, true)) {
            this.mInjector.getWindowManager().setSwitchingUser(false);
        }
    }

    boolean unlockUser(int i, byte[] bArr, byte[] bArr2, IProgressListener iProgressListener) {
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String str = "Permission Denial: unlockUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, str);
            throw new SecurityException(str);
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return unlockUserCleared(i, bArr, bArr2, iProgressListener);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean maybeUnlockUser(int i) {
        return unlockUserCleared(i, null, null, null);
    }

    private static void notifyFinished(int i, IProgressListener iProgressListener) {
        if (iProgressListener == null) {
            return;
        }
        try {
            iProgressListener.onFinished(i, (Bundle) null);
        } catch (RemoteException e) {
        }
    }

    private boolean unlockUserCleared(int i, byte[] bArr, byte[] bArr2, IProgressListener iProgressListener) {
        UserState userState;
        int i2;
        int[] iArr;
        if (!StorageManager.isUserKeyUnlocked(i)) {
            try {
                getStorageManager().unlockUserKey(i, getUserInfo(i).serialNumber, bArr, bArr2);
            } catch (RemoteException | RuntimeException e) {
                Slog.w(TAG, "Failed to unlock: " + e.getMessage());
            }
        }
        synchronized (this.mLock) {
            userState = this.mStartedUsers.get(i);
            if (userState != null) {
                userState.mUnlockProgress.addListener(iProgressListener);
                userState.tokenProvided = bArr != null;
            }
        }
        if (userState == null) {
            notifyFinished(i, iProgressListener);
            return false;
        }
        finishUserUnlocking(userState);
        synchronized (this.mLock) {
            iArr = new int[this.mStartedUsers.size()];
            for (int i3 = 0; i3 < iArr.length; i3++) {
                iArr[i3] = this.mStartedUsers.keyAt(i3);
            }
        }
        for (int i4 : iArr) {
            UserInfo profileParent = this.mInjector.getUserManager().getProfileParent(i4);
            if (profileParent != null && profileParent.id == i && i4 != i) {
                Slog.d(TAG, "User " + i4 + " (parent " + profileParent.id + "): attempting unlock because parent was just unlocked");
                maybeUnlockUser(i4);
            }
        }
        return true;
    }

    boolean switchUser(int i) {
        enforceShellRestriction("no_debugging_features", i);
        int currentUserId = getCurrentUserId();
        UserInfo userInfo = getUserInfo(i);
        if (i == currentUserId) {
            Slog.i(TAG, "user #" + i + " is already the current user");
            return true;
        }
        if (userInfo == null) {
            Slog.w(TAG, "No user info for user #" + i);
            return false;
        }
        if (!userInfo.supportsSwitchTo()) {
            Slog.w(TAG, "Cannot switch to User #" + i + ": not supported");
            return false;
        }
        if (userInfo.isManagedProfile()) {
            Slog.w(TAG, "Cannot switch to User #" + i + ": not a full user");
            return false;
        }
        synchronized (this.mLock) {
            this.mTargetUserId = i;
        }
        if (this.mUserSwitchUiEnabled) {
            Pair pair = new Pair(getUserInfo(currentUserId), userInfo);
            this.mUiHandler.removeMessages(1000);
            this.mUiHandler.sendMessage(this.mHandler.obtainMessage(1000, pair));
        } else {
            this.mHandler.removeMessages(START_USER_SWITCH_FG_MSG);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(START_USER_SWITCH_FG_MSG, i, 0));
        }
        return true;
    }

    private void showUserSwitchDialog(Pair<UserInfo, UserInfo> pair) {
        this.mInjector.showUserSwitchingDialog((UserInfo) pair.first, (UserInfo) pair.second, getSwitchingFromSystemUserMessage(), getSwitchingToSystemUserMessage());
    }

    private void dispatchForegroundProfileChanged(int i) {
        int iBeginBroadcast = this.mUserSwitchObservers.beginBroadcast();
        for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i2).onForegroundProfileSwitch(i);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    void dispatchUserSwitchComplete(int i) {
        this.mInjector.getWindowManager().setSwitchingUser(false);
        int iBeginBroadcast = this.mUserSwitchObservers.beginBroadcast();
        for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i2).onUserSwitchComplete(i);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    private void dispatchLockedBootComplete(int i) {
        int iBeginBroadcast = this.mUserSwitchObservers.beginBroadcast();
        for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i2).onLockedBootComplete(i);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    private void stopBackgroundUsersIfEnforced(int i) {
        if (i == 0 || !hasUserRestriction("no_run_in_background", i)) {
            return;
        }
        synchronized (this.mLock) {
            if (ActivityManagerDebugConfig.DEBUG_MU) {
                Slog.i(TAG, "stopBackgroundUsersIfEnforced stopping " + i + " and related users");
            }
            stopUsersLU(i, false, null);
        }
    }

    private void timeoutUserSwitch(UserState userState, int i, int i2) {
        synchronized (this.mLock) {
            Slog.e(TAG, "User switch timeout: from " + i + " to " + i2);
            this.mTimeoutUserSwitchCallbacks = this.mCurWaitingUserSwitchCallbacks;
            this.mHandler.removeMessages(USER_SWITCH_CALLBACKS_TIMEOUT_MSG);
            sendContinueUserSwitchLU(userState, i, i2);
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(USER_SWITCH_CALLBACKS_TIMEOUT_MSG, i, i2), 5000L);
        }
    }

    private void timeoutUserSwitchCallbacks(int i, int i2) {
        synchronized (this.mLock) {
            if (this.mTimeoutUserSwitchCallbacks != null && !this.mTimeoutUserSwitchCallbacks.isEmpty()) {
                Slog.wtf(TAG, "User switch timeout: from " + i + " to " + i2 + ". Observers that didn't respond: " + this.mTimeoutUserSwitchCallbacks);
                this.mTimeoutUserSwitchCallbacks = null;
            }
        }
    }

    void dispatchUserSwitch(UserState userState, final int i, final int i2) {
        UserState userState2;
        AtomicInteger atomicInteger;
        int i3;
        Slog.d(TAG, "Dispatch onUserSwitching oldUser #" + i + " newUser #" + i2);
        int iBeginBroadcast = this.mUserSwitchObservers.beginBroadcast();
        if (iBeginBroadcast > 0) {
            final ArraySet<String> arraySet = new ArraySet<>();
            synchronized (this.mLock) {
                userState2 = userState;
                userState2.switching = true;
                this.mCurWaitingUserSwitchCallbacks = arraySet;
            }
            AtomicInteger atomicInteger2 = new AtomicInteger(iBeginBroadcast);
            final long jElapsedRealtime = SystemClock.elapsedRealtime();
            int i4 = 0;
            while (i4 < iBeginBroadcast) {
                try {
                    final String str = "#" + i4 + " " + this.mUserSwitchObservers.getBroadcastCookie(i4);
                    synchronized (this.mLock) {
                        try {
                            arraySet.add(str);
                        } finally {
                            th = th;
                            while (true) {
                                try {
                                } catch (Throwable th) {
                                    th = th;
                                }
                            }
                        }
                    }
                    final AtomicInteger atomicInteger3 = atomicInteger2;
                    i3 = iBeginBroadcast;
                    iBeginBroadcast = i4;
                    final UserState userState3 = userState2;
                    atomicInteger = atomicInteger2;
                    try {
                        this.mUserSwitchObservers.getBroadcastItem(iBeginBroadcast).onUserSwitching(i2, new IRemoteCallback.Stub() {
                            public void sendResult(Bundle bundle) throws RemoteException {
                                synchronized (UserController.this.mLock) {
                                    long jElapsedRealtime2 = SystemClock.elapsedRealtime() - jElapsedRealtime;
                                    if (jElapsedRealtime2 > 3000) {
                                        Slog.e(UserController.TAG, "User switch timeout: observer " + str + " sent result after " + jElapsedRealtime2 + " ms");
                                    }
                                    arraySet.remove(str);
                                    if (atomicInteger3.decrementAndGet() == 0 && arraySet == UserController.this.mCurWaitingUserSwitchCallbacks) {
                                        UserController.this.sendContinueUserSwitchLU(userState3, i, i2);
                                    }
                                }
                            }
                        });
                    } catch (RemoteException e) {
                    }
                } catch (RemoteException e2) {
                    atomicInteger = atomicInteger2;
                    i3 = iBeginBroadcast;
                    iBeginBroadcast = i4;
                }
                i4 = iBeginBroadcast + 1;
                userState2 = userState;
                iBeginBroadcast = i3;
                atomicInteger2 = atomicInteger;
            }
        } else {
            synchronized (this.mLock) {
                sendContinueUserSwitchLU(userState, i, i2);
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    void sendContinueUserSwitchLU(UserState userState, int i, int i2) {
        this.mCurWaitingUserSwitchCallbacks = null;
        this.mHandler.removeMessages(30);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(20, i, i2, userState));
    }

    void continueUserSwitch(UserState userState, int i, int i2) {
        Slog.d(TAG, "Continue user switch oldUser #" + i + ", newUser #" + i2);
        if (this.mUserSwitchUiEnabled) {
            this.mInjector.getWindowManager().stopFreezingScreen();
        }
        userState.switching = false;
        this.mHandler.removeMessages(80);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(80, i2, 0));
        stopGuestOrEphemeralUserIfBackground(i);
        stopBackgroundUsersIfEnforced(i);
    }

    private void moveUserToForeground(UserState userState, int i, int i2) {
        if (this.mInjector.stackSupervisorSwitchUser(i2, userState)) {
            this.mInjector.startHomeActivity(i2, "moveUserToForeground");
        } else {
            this.mInjector.stackSupervisorResumeFocusedStackTopActivity();
        }
        EventLogTags.writeAmSwitchUser(i2);
        sendUserSwitchBroadcasts(i, i2);
    }

    void sendUserSwitchBroadcasts(int i, int i2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        if (i >= 0) {
            try {
                List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(i, false);
                int size = profiles.size();
                for (int i3 = 0; i3 < size; i3++) {
                    int i4 = profiles.get(i3).id;
                    Intent intent = new Intent("android.intent.action.USER_BACKGROUND");
                    intent.addFlags(1342177280);
                    intent.putExtra("android.intent.extra.user_handle", i4);
                    this.mInjector.broadcastIntent(intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, i4);
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
        if (i2 >= 0) {
            List<UserInfo> profiles2 = this.mInjector.getUserManager().getProfiles(i2, false);
            int size2 = profiles2.size();
            for (int i5 = 0; i5 < size2; i5++) {
                int i6 = profiles2.get(i5).id;
                Intent intent2 = new Intent("android.intent.action.USER_FOREGROUND");
                intent2.addFlags(1342177280);
                intent2.putExtra("android.intent.extra.user_handle", i6);
                this.mInjector.broadcastIntent(intent2, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, i6);
            }
            Intent intent3 = new Intent("android.intent.action.USER_SWITCHED");
            intent3.addFlags(1342177280);
            intent3.putExtra("android.intent.extra.user_handle", i2);
            this.mInjector.broadcastIntent(intent3, null, null, 0, null, null, new String[]{"android.permission.MANAGE_USERS"}, -1, null, false, false, ActivityManagerService.MY_PID, 1000, -1);
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    int handleIncomingUser(int i, int i2, int i3, boolean z, int i4, String str, String str2) {
        int userId = UserHandle.getUserId(i2);
        if (userId == i3) {
            return i3;
        }
        int iUnsafeConvertIncomingUser = unsafeConvertIncomingUser(i3);
        if (i2 != 0 && i2 != 1000) {
            boolean zIsSameProfileGroup = false;
            if ((!this.mInjector.isCallerRecents(i2) || userId != getCurrentUserId() || !isSameProfileGroup(userId, iUnsafeConvertIncomingUser)) && this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", i, i2, -1, true) != 0) {
                if (i4 != 2 && this.mInjector.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", i, i2, -1, true) == 0) {
                    if (i4 != 0) {
                        if (i4 == 1) {
                            zIsSameProfileGroup = isSameProfileGroup(userId, iUnsafeConvertIncomingUser);
                        } else {
                            throw new IllegalArgumentException("Unknown mode: " + i4);
                        }
                    } else {
                        zIsSameProfileGroup = true;
                    }
                }
                if (!zIsSameProfileGroup) {
                    if (i3 != -3) {
                        StringBuilder sb = new StringBuilder(128);
                        sb.append("Permission Denial: ");
                        sb.append(str);
                        if (str2 != null) {
                            sb.append(" from ");
                            sb.append(str2);
                        }
                        sb.append(" asks to run as user ");
                        sb.append(i3);
                        sb.append(" but is calling from user ");
                        sb.append(UserHandle.getUserId(i2));
                        sb.append("; this requires ");
                        sb.append("android.permission.INTERACT_ACROSS_USERS_FULL");
                        if (i4 != 2) {
                            sb.append(" or ");
                            sb.append("android.permission.INTERACT_ACROSS_USERS");
                        }
                        String string = sb.toString();
                        Slog.w(TAG, string);
                        throw new SecurityException(string);
                    }
                }
            }
        } else {
            userId = iUnsafeConvertIncomingUser;
        }
        if (!z) {
            ensureNotSpecialUser(userId);
        }
        if (i2 == 2000 && userId >= 0 && hasUserRestriction("no_debugging_features", userId)) {
            throw new SecurityException("Shell does not have permission to access user " + userId + "\n " + Debug.getCallers(3));
        }
        return userId;
    }

    int unsafeConvertIncomingUser(int i) {
        if (i != -2 && i != -3) {
            return i;
        }
        return getCurrentUserId();
    }

    void ensureNotSpecialUser(int i) {
        if (i >= 0) {
            return;
        }
        throw new IllegalArgumentException("Call does not support special user #" + i);
    }

    void registerUserSwitchObserver(IUserSwitchObserver iUserSwitchObserver, String str) {
        Preconditions.checkNotNull(str, "Observer name cannot be null");
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String str2 = "Permission Denial: registerUserSwitchObserver() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, str2);
            throw new SecurityException(str2);
        }
        this.mUserSwitchObservers.register(iUserSwitchObserver, str);
    }

    void sendForegroundProfileChanged(int i) {
        this.mHandler.removeMessages(70);
        this.mHandler.obtainMessage(70, i, 0).sendToTarget();
    }

    void unregisterUserSwitchObserver(IUserSwitchObserver iUserSwitchObserver) {
        this.mUserSwitchObservers.unregister(iUserSwitchObserver);
    }

    UserState getStartedUserState(int i) {
        UserState userState;
        synchronized (this.mLock) {
            userState = this.mStartedUsers.get(i);
        }
        return userState;
    }

    boolean hasStartedUserState(int i) {
        return this.mStartedUsers.get(i) != null;
    }

    private void updateStartedUserArrayLU() {
        int i = 0;
        for (int i2 = 0; i2 < this.mStartedUsers.size(); i2++) {
            UserState userStateValueAt = this.mStartedUsers.valueAt(i2);
            if (userStateValueAt.state != 4 && userStateValueAt.state != 5) {
                i++;
            }
        }
        this.mStartedUserArray = new int[i];
        int i3 = 0;
        for (int i4 = 0; i4 < this.mStartedUsers.size(); i4++) {
            UserState userStateValueAt2 = this.mStartedUsers.valueAt(i4);
            if (userStateValueAt2.state != 4 && userStateValueAt2.state != 5) {
                this.mStartedUserArray[i3] = this.mStartedUsers.keyAt(i4);
                i3++;
            }
        }
    }

    void sendBootCompleted(IIntentReceiver iIntentReceiver) {
        SparseArray<UserState> sparseArrayClone;
        synchronized (this.mLock) {
            sparseArrayClone = this.mStartedUsers.clone();
        }
        for (int i = 0; i < sparseArrayClone.size(); i++) {
            finishUserBoot(sparseArrayClone.valueAt(i), iIntentReceiver);
        }
    }

    void onSystemReady() {
        updateCurrentProfileIds();
        this.mInjector.reportCurWakefulnessUsageEvent();
    }

    private void updateCurrentProfileIds() {
        List<UserInfo> profiles = this.mInjector.getUserManager().getProfiles(getCurrentUserId(), false);
        int[] iArr = new int[profiles.size()];
        for (int i = 0; i < iArr.length; i++) {
            iArr[i] = profiles.get(i).id;
        }
        List<UserInfo> users = this.mInjector.getUserManager().getUsers(false);
        synchronized (this.mLock) {
            this.mCurrentProfileIds = iArr;
            this.mUserProfileGroupIds.clear();
            for (int i2 = 0; i2 < users.size(); i2++) {
                UserInfo userInfo = users.get(i2);
                if (userInfo.profileGroupId != -10000) {
                    this.mUserProfileGroupIds.put(userInfo.id, userInfo.profileGroupId);
                }
            }
        }
    }

    int[] getStartedUserArray() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mStartedUserArray;
        }
        return iArr;
    }

    boolean isUserRunning(int i, int i2) {
        UserState startedUserState = getStartedUserState(i);
        if (startedUserState == null) {
            return false;
        }
        if ((i2 & 1) != 0) {
            return true;
        }
        if ((i2 & 2) != 0) {
            switch (startedUserState.state) {
                case 0:
                case 1:
                    return true;
                default:
                    return false;
            }
        }
        if ((i2 & 8) != 0) {
            switch (startedUserState.state) {
                case 2:
                case 3:
                    return true;
                case 4:
                case 5:
                    return StorageManager.isUserKeyUnlocked(i);
                default:
                    return false;
            }
        }
        if ((i2 & 4) == 0) {
            return (startedUserState.state == 4 || startedUserState.state == 5) ? false : true;
        }
        switch (startedUserState.state) {
            case 3:
                return true;
            case 4:
            case 5:
                return StorageManager.isUserKeyUnlocked(i);
            default:
                return false;
        }
    }

    UserInfo getCurrentUser() {
        UserInfo currentUserLU;
        if (this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") != 0 && this.mInjector.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String str = "Permission Denial: getCurrentUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS";
            Slog.w(TAG, str);
            throw new SecurityException(str);
        }
        if (this.mTargetUserId == -10000) {
            return getUserInfo(this.mCurrentUserId);
        }
        synchronized (this.mLock) {
            currentUserLU = getCurrentUserLU();
        }
        return currentUserLU;
    }

    UserInfo getCurrentUserLU() {
        return getUserInfo(this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId);
    }

    int getCurrentOrTargetUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
        }
        return i;
    }

    int getCurrentOrTargetUserIdLU() {
        return this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
    }

    int getCurrentUserIdLU() {
        return this.mCurrentUserId;
    }

    int getCurrentUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mCurrentUserId;
        }
        return i;
    }

    private boolean isCurrentUserLU(int i) {
        return i == getCurrentOrTargetUserIdLU();
    }

    int[] getUsers() {
        UserManagerService userManager = this.mInjector.getUserManager();
        return userManager != null ? userManager.getUserIds() : new int[]{0};
    }

    UserInfo getUserInfo(int i) {
        return this.mInjector.getUserManager().getUserInfo(i);
    }

    int[] getUserIds() {
        return this.mInjector.getUserManager().getUserIds();
    }

    int[] expandUserId(int i) {
        if (i != -1) {
            return new int[]{i};
        }
        return getUsers();
    }

    boolean exists(int i) {
        return this.mInjector.getUserManager().exists(i);
    }

    void enforceShellRestriction(String str, int i) {
        if (Binder.getCallingUid() == 2000) {
            if (i < 0 || hasUserRestriction(str, i)) {
                throw new SecurityException("Shell does not have permission to access user " + i);
            }
        }
    }

    boolean hasUserRestriction(String str, int i) {
        return this.mInjector.getUserManager().hasUserRestriction(str, i);
    }

    Set<Integer> getProfileIds(int i) {
        HashSet hashSet = new HashSet();
        Iterator<UserInfo> it = this.mInjector.getUserManager().getProfiles(i, false).iterator();
        while (it.hasNext()) {
            hashSet.add(Integer.valueOf(it.next().id));
        }
        return hashSet;
    }

    boolean isSameProfileGroup(int i, int i2) {
        boolean z = true;
        if (i == i2) {
            return true;
        }
        synchronized (this.mLock) {
            int i3 = this.mUserProfileGroupIds.get(i, -10000);
            int i4 = this.mUserProfileGroupIds.get(i2, -10000);
            if (i3 == -10000 || i3 != i4) {
                z = false;
            }
        }
        return z;
    }

    boolean isUserOrItsParentRunning(int i) {
        synchronized (this.mLock) {
            if (isUserRunning(i, 0)) {
                return true;
            }
            int i2 = this.mUserProfileGroupIds.get(i, -10000);
            if (i2 == -10000) {
                return false;
            }
            return isUserRunning(i2, 0);
        }
    }

    boolean isCurrentProfile(int i) {
        boolean zContains;
        synchronized (this.mLock) {
            zContains = ArrayUtils.contains(this.mCurrentProfileIds, i);
        }
        return zContains;
    }

    int[] getCurrentProfileIds() {
        int[] iArr;
        synchronized (this.mLock) {
            iArr = this.mCurrentProfileIds;
        }
        return iArr;
    }

    void onUserRemoved(int i) {
        synchronized (this.mLock) {
            for (int size = this.mUserProfileGroupIds.size() - 1; size >= 0; size--) {
                if (this.mUserProfileGroupIds.keyAt(size) == i || this.mUserProfileGroupIds.valueAt(size) == i) {
                    this.mUserProfileGroupIds.removeAt(size);
                }
            }
            this.mCurrentProfileIds = ArrayUtils.removeInt(this.mCurrentProfileIds, i);
        }
    }

    protected boolean shouldConfirmCredentials(int i) {
        synchronized (this.mLock) {
            if (this.mStartedUsers.get(i) == null) {
                return false;
            }
            if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i)) {
                return false;
            }
            KeyguardManager keyguardManager = this.mInjector.getKeyguardManager();
            return keyguardManager.isDeviceLocked(i) && keyguardManager.isDeviceSecure(i);
        }
    }

    boolean isLockScreenDisabled(int i) {
        return this.mLockPatternUtils.isLockScreenDisabled(i);
    }

    void setSwitchingFromSystemUserMessage(String str) {
        synchronized (this.mLock) {
            this.mSwitchingFromSystemUserMessage = str;
        }
    }

    void setSwitchingToSystemUserMessage(String str) {
        synchronized (this.mLock) {
            this.mSwitchingToSystemUserMessage = str;
        }
    }

    private String getSwitchingFromSystemUserMessage() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingFromSystemUserMessage;
        }
        return str;
    }

    private String getSwitchingToSystemUserMessage() {
        String str;
        synchronized (this.mLock) {
            str = this.mSwitchingToSystemUserMessage;
        }
        return str;
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        synchronized (this.mLock) {
            long jStart = protoOutputStream.start(j);
            for (int i = 0; i < this.mStartedUsers.size(); i++) {
                UserState userStateValueAt = this.mStartedUsers.valueAt(i);
                long jStart2 = protoOutputStream.start(2246267895809L);
                protoOutputStream.write(1120986464257L, userStateValueAt.mHandle.getIdentifier());
                userStateValueAt.writeToProto(protoOutputStream, 1146756268034L);
                protoOutputStream.end(jStart2);
            }
            for (int i2 = 0; i2 < this.mStartedUserArray.length; i2++) {
                protoOutputStream.write(2220498092034L, this.mStartedUserArray[i2]);
            }
            for (int i3 = 0; i3 < this.mUserLru.size(); i3++) {
                protoOutputStream.write(2220498092035L, this.mUserLru.get(i3).intValue());
            }
            if (this.mUserProfileGroupIds.size() > 0) {
                for (int i4 = 0; i4 < this.mUserProfileGroupIds.size(); i4++) {
                    long jStart3 = protoOutputStream.start(2246267895812L);
                    protoOutputStream.write(1120986464257L, this.mUserProfileGroupIds.keyAt(i4));
                    protoOutputStream.write(1120986464258L, this.mUserProfileGroupIds.valueAt(i4));
                    protoOutputStream.end(jStart3);
                }
            }
            protoOutputStream.end(jStart);
        }
    }

    void dump(PrintWriter printWriter, boolean z) {
        synchronized (this.mLock) {
            printWriter.println("  mStartedUsers:");
            for (int i = 0; i < this.mStartedUsers.size(); i++) {
                UserState userStateValueAt = this.mStartedUsers.valueAt(i);
                printWriter.print("    User #");
                printWriter.print(userStateValueAt.mHandle.getIdentifier());
                printWriter.print(": ");
                userStateValueAt.dump(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, printWriter);
            }
            printWriter.print("  mStartedUserArray: [");
            for (int i2 = 0; i2 < this.mStartedUserArray.length; i2++) {
                if (i2 > 0) {
                    printWriter.print(", ");
                }
                printWriter.print(this.mStartedUserArray[i2]);
            }
            printWriter.println("]");
            printWriter.print("  mUserLru: [");
            for (int i3 = 0; i3 < this.mUserLru.size(); i3++) {
                if (i3 > 0) {
                    printWriter.print(", ");
                }
                printWriter.print(this.mUserLru.get(i3));
            }
            printWriter.println("]");
            if (this.mUserProfileGroupIds.size() > 0) {
                printWriter.println("  mUserProfileGroupIds:");
                for (int i4 = 0; i4 < this.mUserProfileGroupIds.size(); i4++) {
                    printWriter.print("    User #");
                    printWriter.print(this.mUserProfileGroupIds.keyAt(i4));
                    printWriter.print(" -> profile #");
                    printWriter.println(this.mUserProfileGroupIds.valueAt(i4));
                }
            }
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case 10:
                dispatchUserSwitch((UserState) message.obj, message.arg1, message.arg2);
                break;
            case 20:
                continueUserSwitch((UserState) message.obj, message.arg1, message.arg2);
                break;
            case 30:
                timeoutUserSwitch((UserState) message.obj, message.arg1, message.arg2);
                break;
            case 40:
                startProfiles();
                break;
            case 50:
                this.mInjector.batteryStatsServiceNoteEvent(32775, Integer.toString(message.arg1), message.arg1);
                this.mInjector.getSystemServiceManager().startUser(message.arg1);
                break;
            case 60:
                this.mInjector.batteryStatsServiceNoteEvent(16392, Integer.toString(message.arg2), message.arg2);
                this.mInjector.batteryStatsServiceNoteEvent(32776, Integer.toString(message.arg1), message.arg1);
                this.mInjector.getSystemServiceManager().switchUser(message.arg1);
                break;
            case 70:
                dispatchForegroundProfileChanged(message.arg1);
                break;
            case 80:
                dispatchUserSwitchComplete(message.arg1);
                break;
            case USER_SWITCH_CALLBACKS_TIMEOUT_MSG:
                timeoutUserSwitchCallbacks(message.arg1, message.arg2);
                break;
            case 100:
                final int i = message.arg1;
                this.mInjector.getSystemServiceManager().unlockUser(i);
                FgThread.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mInjector.loadUserRecents(i);
                    }
                });
                finishUserUnlocked((UserState) message.obj);
                break;
            case 110:
                dispatchLockedBootComplete(message.arg1);
                break;
            case START_USER_SWITCH_FG_MSG:
                startUserInForeground(message.arg1);
                break;
            case 1000:
                showUserSwitchDialog((Pair) message.obj);
                break;
        }
        return false;
    }

    private static class UserProgressListener extends IProgressListener.Stub {
        private volatile long mUnlockStarted;

        private UserProgressListener() {
        }

        public void onStarted(int i, Bundle bundle) throws RemoteException {
            Slog.d(UserController.TAG, "Started unlocking user " + i);
            this.mUnlockStarted = SystemClock.uptimeMillis();
        }

        public void onProgress(int i, int i2, Bundle bundle) throws RemoteException {
            Slog.d(UserController.TAG, "Unlocking user " + i + " progress " + i2);
        }

        public void onFinished(int i, Bundle bundle) throws RemoteException {
            long jUptimeMillis = SystemClock.uptimeMillis() - this.mUnlockStarted;
            if (i == 0) {
                new TimingsTraceLog("SystemServerTiming", 524288L).logDuration("SystemUserUnlock", jUptimeMillis);
                return;
            }
            Slog.d(UserController.TAG, "Unlocking user " + i + " took " + jUptimeMillis + " ms");
        }
    }

    @VisibleForTesting
    static class Injector {
        private final ActivityManagerService mService;
        private UserManagerService mUserManager;
        private UserManagerInternal mUserManagerInternal;

        Injector(ActivityManagerService activityManagerService) {
            this.mService = activityManagerService;
        }

        protected Handler getHandler(Handler.Callback callback) {
            return new Handler(this.mService.mHandlerThread.getLooper(), callback);
        }

        protected Handler getUiHandler(Handler.Callback callback) {
            return new Handler(this.mService.mUiHandler.getLooper(), callback);
        }

        protected Context getContext() {
            return this.mService.mContext;
        }

        protected LockPatternUtils getLockPatternUtils() {
            return new LockPatternUtils(getContext());
        }

        protected int broadcastIntent(Intent intent, String str, IIntentReceiver iIntentReceiver, int i, String str2, Bundle bundle, String[] strArr, int i2, Bundle bundle2, boolean z, boolean z2, int i3, int i4, int i5) {
            int iBroadcastIntentLocked;
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    iBroadcastIntentLocked = this.mService.broadcastIntentLocked(null, null, intent, str, iIntentReceiver, i, str2, bundle, strArr, i2, bundle2, z, z2, i3, i4, i5);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return iBroadcastIntentLocked;
        }

        int checkCallingPermission(String str) {
            return this.mService.checkCallingPermission(str);
        }

        WindowManagerService getWindowManager() {
            return this.mService.mWindowManager;
        }

        void activityManagerOnUserStopped(int i) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.onUserStoppedLocked(i);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void systemServiceManagerCleanupUser(int i) {
            this.mService.mSystemServiceManager.cleanupUser(i);
        }

        protected UserManagerService getUserManager() {
            if (this.mUserManager == null) {
                this.mUserManager = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
            }
            return this.mUserManager;
        }

        UserManagerInternal getUserManagerInternal() {
            if (this.mUserManagerInternal == null) {
                this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
            }
            return this.mUserManagerInternal;
        }

        KeyguardManager getKeyguardManager() {
            return (KeyguardManager) this.mService.mContext.getSystemService(KeyguardManager.class);
        }

        void batteryStatsServiceNoteEvent(int i, String str, int i2) {
            this.mService.mBatteryStatsService.noteEvent(i, str, i2);
        }

        boolean isRuntimeRestarted() {
            return this.mService.mSystemServiceManager.isRuntimeRestarted();
        }

        SystemServiceManager getSystemServiceManager() {
            return this.mService.mSystemServiceManager;
        }

        boolean isFirstBootOrUpgrade() {
            IPackageManager packageManager = AppGlobals.getPackageManager();
            try {
                if (!packageManager.isFirstBoot()) {
                    if (!packageManager.isUpgrade()) {
                        return false;
                    }
                }
                return true;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        void sendPreBootBroadcast(int i, boolean z, final Runnable runnable) {
            new PreBootBroadcaster(this.mService, i, null, z) {
                @Override
                public void onFinished() {
                    runnable.run();
                }
            }.sendNext();
        }

        void activityManagerForceStopPackage(int i, String str) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.forceStopPackageLocked(null, -1, false, false, true, false, false, i, str);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        int checkComponentPermission(String str, int i, int i2, int i3, boolean z) {
            return this.mService.checkComponentPermission(str, i, i2, i3, z);
        }

        protected void startHomeActivity(int i, String str) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.startHomeActivityLocked(i, str);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void updateUserConfiguration() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.updateUserConfigurationLocked();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void clearBroadcastQueueForUser(int i) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.clearBroadcastQueueForUserLocked(i);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void loadUserRecents(int i) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.getRecentTasks().loadUserRecentsLocked(i);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void startPersistentApps(int i) {
            this.mService.startPersistentApps(i);
        }

        void installEncryptionUnawareProviders(int i) {
            this.mService.installEncryptionUnawareProviders(i);
        }

        void showUserSwitchingDialog(UserInfo userInfo, UserInfo userInfo2, String str, String str2) {
            Dialog carUserSwitchingDialog;
            if (!this.mService.mContext.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
                carUserSwitchingDialog = new UserSwitchingDialog(this.mService, this.mService.mContext, userInfo, userInfo2, true, str, str2);
            } else {
                carUserSwitchingDialog = new CarUserSwitchingDialog(this.mService, this.mService.mContext, userInfo, userInfo2, true, str, str2);
            }
            carUserSwitchingDialog.show();
        }

        void reportGlobalUsageEventLocked(int i) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.reportGlobalUsageEventLocked(i);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void reportCurWakefulnessUsageEvent() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.reportCurWakefulnessUsageEventLocked();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        void stackSupervisorRemoveUser(int i) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.mStackSupervisor.removeUserLocked(i);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        protected boolean stackSupervisorSwitchUser(int i, UserState userState) {
            boolean zSwitchUserLocked;
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    zSwitchUserLocked = this.mService.mStackSupervisor.switchUserLocked(i, userState);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return zSwitchUserLocked;
        }

        protected void stackSupervisorResumeFocusedStackTopActivity() {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        protected void clearAllLockedTasks(String str) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    this.mService.getLockTaskController().clearLockedTasks(str);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        protected boolean isCallerRecents(int i) {
            return this.mService.getRecentTasks().isCallerRecents(i);
        }
    }
}
