package com.android.server.am;

import android.R;
import android.app.Dialog;
import android.app.IStopUserCallback;
import android.app.IUserSwitchObserver;
import android.app.KeyguardManager;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.IProgressListener;
import android.os.IRemoteCallback;
import android.os.IUserManager;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManagerInternal;
import android.os.storage.IMountService;
import android.os.storage.StorageManager;
import android.util.IntArray;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.pm.UserManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class UserController {
    static final int MAX_RUNNING_USERS = 3;
    private static final String TAG = "ActivityManager";
    static final int USER_SWITCH_TIMEOUT = 2000;
    Object mCurUserSwitchCallback;
    private final Handler mHandler;
    private final LockPatternUtils mLockPatternUtils;
    private final ActivityManagerService mService;
    private volatile UserManagerService mUserManager;
    private UserManagerInternal mUserManagerInternal;
    private int mCurrentUserId = 0;
    private int mTargetUserId = -10000;

    @GuardedBy("mService")
    private final SparseArray<UserState> mStartedUsers = new SparseArray<>();
    private final ArrayList<Integer> mUserLru = new ArrayList<>();
    private int[] mStartedUserArray = {0};
    private int[] mCurrentProfileIds = new int[0];
    private final SparseIntArray mUserProfileGroupIdsSelfLocked = new SparseIntArray();
    private final RemoteCallbackList<IUserSwitchObserver> mUserSwitchObservers = new RemoteCallbackList<>();

    UserController(ActivityManagerService service) {
        this.mService = service;
        this.mHandler = this.mService.mHandler;
        UserState uss = new UserState(UserHandle.SYSTEM);
        this.mStartedUsers.put(0, uss);
        this.mUserLru.add(0);
        this.mLockPatternUtils = new LockPatternUtils(this.mService.mContext);
        updateStartedUserArrayLocked();
    }

    void finishUserSwitch(UserState uss) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                finishUserBoot(uss);
                startProfilesLocked();
                stopRunningUsersLocked(3);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void stopRunningUsersLocked(int maxRunningUsers) {
        int num = this.mUserLru.size();
        int i = 0;
        while (num > maxRunningUsers && i < this.mUserLru.size()) {
            Integer oldUserId = this.mUserLru.get(i);
            UserState oldUss = this.mStartedUsers.get(oldUserId.intValue());
            if (oldUss == null) {
                this.mUserLru.remove(i);
                num--;
            } else if (oldUss.state == 4 || oldUss.state == 5) {
                num--;
                i++;
            } else if (oldUserId.intValue() == 0 || oldUserId.intValue() == this.mCurrentUserId) {
                if (UserInfo.isSystemOnly(oldUserId.intValue())) {
                    num--;
                }
                i++;
            } else {
                if (stopUsersLocked(oldUserId.intValue(), false, null) != 0) {
                    num--;
                }
                num--;
                i++;
            }
        }
    }

    private void finishUserBoot(UserState uss) {
        finishUserBoot(uss, null);
    }

    private void finishUserBoot(UserState uss, IIntentReceiver resultTo) {
        int userId = uss.mHandle.getIdentifier();
        Slog.d(TAG, "Finishing user boot " + userId);
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mStartedUsers.get(userId) != uss) {
                    return;
                }
                if (uss.setState(0, 1)) {
                    getUserManagerInternal().setUserState(userId, uss.state);
                    int uptimeSeconds = (int) (SystemClock.elapsedRealtime() / 1000);
                    MetricsLogger.histogram(this.mService.mContext, "framework_locked_boot_completed", uptimeSeconds);
                    Intent intent = new Intent("android.intent.action.LOCKED_BOOT_COMPLETED", (Uri) null);
                    intent.putExtra("android.intent.extra.user_handle", userId);
                    intent.addFlags(150994944);
                    this.mService.broadcastIntentLocked(null, null, intent, null, resultTo, 0, null, null, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, userId);
                }
                if (getUserManager().isManagedProfile(userId)) {
                    UserInfo parent = getUserManager().getProfileParent(userId);
                    if (parent == null || !isUserRunningLocked(parent.id, 4)) {
                        String parentId = parent == null ? "<null>" : String.valueOf(parent.id);
                        Slog.d(TAG, "User " + userId + " (parent " + parentId + "): delaying unlock because parent is locked");
                    } else {
                        Slog.d(TAG, "User " + userId + " (parent " + parent.id + "): attempting unlock because parent is unlocked");
                        maybeUnlockUser(userId);
                    }
                } else {
                    maybeUnlockUser(userId);
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void finishUserUnlocking(UserState uss) {
        int userId = uss.mHandle.getIdentifier();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mStartedUsers.get(uss.mHandle.getIdentifier()) != uss) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (!StorageManager.isUserKeyUnlocked(userId)) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (uss.setState(1, 2)) {
                    getUserManagerInternal().setUserState(userId, uss.state);
                    uss.mUnlockProgress.start();
                    uss.mUnlockProgress.setProgress(5, this.mService.mContext.getString(R.string.error_handwriting_unsupported));
                    this.mUserManager.onBeforeUnlockUser(userId);
                    uss.mUnlockProgress.setProgress(20);
                    this.mHandler.obtainMessage(61, userId, 0, uss).sendToTarget();
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void finishUserUnlocked(final UserState uss) {
        UserInfo parent;
        int userId = uss.mHandle.getIdentifier();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mStartedUsers.get(uss.mHandle.getIdentifier()) != uss) {
                    return;
                }
                if (StorageManager.isUserKeyUnlocked(userId)) {
                    if (uss.setState(2, 3)) {
                        getUserManagerInternal().setUserState(userId, uss.state);
                        uss.mUnlockProgress.finish();
                        Intent unlockedIntent = new Intent("android.intent.action.USER_UNLOCKED");
                        unlockedIntent.putExtra("android.intent.extra.user_handle", userId);
                        unlockedIntent.addFlags(1342177280);
                        this.mService.broadcastIntentLocked(null, null, unlockedIntent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, userId);
                        if (getUserInfo(userId).isManagedProfile() && (parent = getUserManager().getProfileParent(userId)) != null) {
                            Intent profileUnlockedIntent = new Intent("android.intent.action.MANAGED_PROFILE_UNLOCKED");
                            profileUnlockedIntent.putExtra("android.intent.extra.USER", UserHandle.of(userId));
                            profileUnlockedIntent.addFlags(1342177280);
                            this.mService.broadcastIntentLocked(null, null, profileUnlockedIntent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, parent.id);
                        }
                        UserInfo info = getUserInfo(userId);
                        if (Objects.equals(info.lastLoggedInFingerprint, Build.FINGERPRINT)) {
                            finishUserUnlockedCompleted(uss);
                        } else {
                            boolean quiet = info.isManagedProfile() ? (uss.tokenProvided && this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) ? false : true : false;
                            new PreBootBroadcaster(this.mService, userId, null, quiet) {
                                @Override
                                public void onFinished() {
                                    UserController.this.finishUserUnlockedCompleted(uss);
                                }
                            }.sendNext();
                        }
                    }
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void finishUserUnlockedCompleted(UserState uss) {
        int userId = uss.mHandle.getIdentifier();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mStartedUsers.get(uss.mHandle.getIdentifier()) != uss) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                final UserInfo userInfo = getUserInfo(userId);
                if (userInfo == null) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (!StorageManager.isUserKeyUnlocked(userId)) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                this.mUserManager.onUserLoggedIn(userId);
                if (!userInfo.isInitialized() && userId != 0) {
                    Slog.d(TAG, "Initializing user #" + userId);
                    Intent intent = new Intent("android.intent.action.USER_INITIALIZE");
                    intent.addFlags(268435456);
                    this.mService.broadcastIntentLocked(null, null, intent, null, new IIntentReceiver.Stub() {
                        public void performReceive(Intent intent2, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                            UserController.this.getUserManager().makeInitialized(userInfo.id);
                        }
                    }, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, userId);
                }
                Slog.d(TAG, "Sending BOOT_COMPLETE user #" + userId);
                int uptimeSeconds = (int) (SystemClock.elapsedRealtime() / 1000);
                MetricsLogger.histogram(this.mService.mContext, "framework_boot_completed", uptimeSeconds);
                Intent bootIntent = new Intent("android.intent.action.BOOT_COMPLETED", (Uri) null);
                bootIntent.putExtra("android.intent.extra.user_handle", userId);
                bootIntent.addFlags(150994944);
                this.mService.broadcastIntentLocked(null, null, bootIntent, null, null, 0, null, null, new String[]{"android.permission.RECEIVE_BOOT_COMPLETED"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, userId);
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    int stopUser(int userId, boolean force, IStopUserCallback callback) {
        int iStopUsersLocked;
        if (this.mService.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = "Permission Denial: switchUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        if (userId < 0 || userId == 0) {
            throw new IllegalArgumentException("Can't stop system user " + userId);
        }
        this.mService.enforceShellRestriction("no_debugging_features", userId);
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                iStopUsersLocked = stopUsersLocked(userId, force, callback);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return iStopUsersLocked;
    }

    private int stopUsersLocked(int userId, boolean force, IStopUserCallback callback) {
        if (userId == 0) {
            return -3;
        }
        if (isCurrentUserLocked(userId)) {
            return -2;
        }
        int[] usersToStop = getUsersToStopLocked(userId);
        for (int relatedUserId : usersToStop) {
            if (relatedUserId == 0 || isCurrentUserLocked(relatedUserId)) {
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    Slog.i(TAG, "stopUsersLocked cannot stop related user " + relatedUserId);
                }
                if (force) {
                    Slog.i(TAG, "Force stop user " + userId + ". Related users will not be stopped");
                    stopSingleUserLocked(userId, callback);
                    return 0;
                }
                return -4;
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i(TAG, "stopUsersLocked usersToStop=" + Arrays.toString(usersToStop));
        }
        int length = usersToStop.length;
        for (int i = 0; i < length; i++) {
            int userIdToStop = usersToStop[i];
            stopSingleUserLocked(userIdToStop, userIdToStop == userId ? callback : null);
        }
        return 0;
    }

    private void stopSingleUserLocked(final int userId, final IStopUserCallback callback) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i(TAG, "stopSingleUserLocked userId=" + userId);
        }
        final UserState uss = this.mStartedUsers.get(userId);
        if (uss == null) {
            if (callback != null) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.userStopped(userId);
                        } catch (RemoteException e) {
                        }
                    }
                });
                return;
            }
            return;
        }
        if (callback != null) {
            uss.mStopCallbacks.add(callback);
        }
        if (uss.state == 4 || uss.state == 5) {
            return;
        }
        uss.setState(4);
        getUserManagerInternal().setUserState(userId, uss.state);
        updateStartedUserArrayLocked();
        long ident = Binder.clearCallingIdentity();
        try {
            Intent stoppingIntent = new Intent("android.intent.action.USER_STOPPING");
            stoppingIntent.addFlags(1073741824);
            stoppingIntent.putExtra("android.intent.extra.user_handle", userId);
            stoppingIntent.putExtra("android.intent.extra.SHUTDOWN_USERSPACE_ONLY", true);
            IIntentReceiver stoppingReceiver = new IIntentReceiver.Stub() {
                public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                    Handler handler = UserController.this.mHandler;
                    final int i = userId;
                    final UserState userState = uss;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            UserController.this.finishUserStopping(i, userState);
                        }
                    });
                }
            };
            this.mService.clearBroadcastQueueForUserLocked(userId);
            this.mService.broadcastIntentLocked(null, null, stoppingIntent, null, stoppingReceiver, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, -1);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void finishUserStopping(int userId, final UserState uss) {
        Intent shutdownIntent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        IIntentReceiver shutdownReceiver = new IIntentReceiver.Stub() {
            public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) {
                Handler handler = UserController.this.mHandler;
                final UserState userState = uss;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        UserController.this.finishUserStopped(userState);
                    }
                });
            }
        };
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (uss.state != 4) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                uss.setState(5);
                ActivityManagerService.resetPriorityAfterLockedSection();
                getUserManagerInternal().setUserState(userId, uss.state);
                this.mService.mBatteryStatsService.noteEvent(16391, Integer.toString(userId), userId);
                this.mService.mSystemServiceManager.stopUser(userId);
                synchronized (this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        this.mService.broadcastIntentLocked(null, null, shutdownIntent, null, shutdownReceiver, 0, null, null, null, -1, null, true, false, ActivityManagerService.MY_PID, 1000, userId);
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th2) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th2;
            }
        }
    }

    void finishUserStopped(UserState uss) {
        ArrayList<IStopUserCallback> callbacks;
        boolean stopped;
        int userId = uss.mHandle.getIdentifier();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                callbacks = new ArrayList<>(uss.mStopCallbacks);
                if (this.mStartedUsers.get(userId) != uss || uss.state != 5) {
                    stopped = false;
                } else {
                    stopped = true;
                    this.mStartedUsers.remove(userId);
                    getUserManagerInternal().removeUserState(userId);
                    this.mUserLru.remove(Integer.valueOf(userId));
                    updateStartedUserArrayLocked();
                    this.mService.onUserStoppedLocked(userId);
                    forceStopUserLocked(userId, "finish user");
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        for (int i = 0; i < callbacks.size(); i++) {
            if (stopped) {
                try {
                    callbacks.get(i).userStopped(userId);
                } catch (RemoteException e) {
                }
            } else {
                callbacks.get(i).userStopAborted(userId);
            }
        }
        if (!stopped) {
            return;
        }
        this.mService.mSystemServiceManager.cleanupUser(userId);
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mStackSupervisor.removeUserLocked(userId);
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (!getUserInfo(userId).isEphemeral()) {
            return;
        }
        this.mUserManager.removeUser(userId);
    }

    private int[] getUsersToStopLocked(int userId) {
        int startedUsersSize = this.mStartedUsers.size();
        IntArray userIds = new IntArray();
        userIds.add(userId);
        synchronized (this.mUserProfileGroupIdsSelfLocked) {
            int userGroupId = this.mUserProfileGroupIdsSelfLocked.get(userId, -10000);
            for (int i = 0; i < startedUsersSize; i++) {
                UserState uss = this.mStartedUsers.valueAt(i);
                int startedUserId = uss.mHandle.getIdentifier();
                int startedUserGroupId = this.mUserProfileGroupIdsSelfLocked.get(startedUserId, -10000);
                boolean sameGroup = userGroupId != -10000 && userGroupId == startedUserGroupId;
                boolean sameUserId = startedUserId == userId;
                if (sameGroup && !sameUserId) {
                    userIds.add(startedUserId);
                }
            }
        }
        return userIds.toArray();
    }

    private void forceStopUserLocked(int userId, String reason) {
        this.mService.forceStopPackageLocked(null, -1, false, false, true, false, false, userId, reason);
        Intent intent = new Intent("android.intent.action.USER_STOPPED");
        intent.addFlags(1342177280);
        intent.putExtra("android.intent.extra.user_handle", userId);
        this.mService.broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, -1);
    }

    private void stopGuestOrEphemeralUserIfBackground() {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                int num = this.mUserLru.size();
                for (int i = 0; i < num; i++) {
                    Integer oldUserId = this.mUserLru.get(i);
                    UserState oldUss = this.mStartedUsers.get(oldUserId.intValue());
                    if (oldUserId.intValue() != 0 && oldUserId.intValue() != this.mCurrentUserId && oldUss.state != 4 && oldUss.state != 5) {
                        UserInfo userInfo = getUserInfo(oldUserId.intValue());
                        if (userInfo.isEphemeral()) {
                            ((UserManagerInternal) LocalServices.getService(UserManagerInternal.class)).onEphemeralUserStop(oldUserId.intValue());
                        }
                        if (userInfo.isGuest() || userInfo.isEphemeral()) {
                            stopUsersLocked(oldUserId.intValue(), true, null);
                            break;
                        }
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void startProfilesLocked() {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.i(TAG, "startProfilesLocked");
        }
        List<UserInfo> profiles = getUserManager().getProfiles(this.mCurrentUserId, false);
        List<UserInfo> profilesToStart = new ArrayList<>(profiles.size());
        for (UserInfo user : profiles) {
            if ((user.flags & 16) == 16 && user.id != this.mCurrentUserId && !user.isQuietModeEnabled()) {
                profilesToStart.add(user);
            }
        }
        int profilesToStartSize = profilesToStart.size();
        int i = 0;
        while (i < profilesToStartSize && i < 2) {
            startUser(profilesToStart.get(i).id, false);
            i++;
        }
        if (i >= profilesToStartSize) {
            return;
        }
        Slog.w(TAG, "More profiles than MAX_RUNNING_USERS");
    }

    private UserManagerService getUserManager() {
        UserManagerService userManager = this.mUserManager;
        if (userManager == null) {
            IBinder b = ServiceManager.getService("user");
            UserManagerService userManager2 = IUserManager.Stub.asInterface(b);
            this.mUserManager = userManager2;
            return userManager2;
        }
        return userManager;
    }

    private IMountService getMountService() {
        return IMountService.Stub.asInterface(ServiceManager.getService("mount"));
    }

    boolean startUser(int userId, boolean foreground) {
        if (this.mService.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = "Permission Denial: switchUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        Slog.i(TAG, "Starting userid:" + userId + " fg:" + foreground);
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    int oldUserId = this.mCurrentUserId;
                    if (oldUserId == userId) {
                        return true;
                    }
                    this.mService.mStackSupervisor.setLockTaskModeLocked(null, 0, "startUser", false);
                    UserInfo userInfo = getUserInfo(userId);
                    if (userInfo == null) {
                        Slog.w(TAG, "No user info for user #" + userId);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    if (foreground && userInfo.isManagedProfile()) {
                        Slog.w(TAG, "Cannot switch to User #" + userId + ": not a full user");
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return false;
                    }
                    if (foreground) {
                        this.mService.mWindowManager.startFreezingScreen(R.anim.recent_exit, R.anim.recent_enter);
                    }
                    boolean needStart = false;
                    if (this.mStartedUsers.get(userId) == null) {
                        UserState userState = new UserState(UserHandle.of(userId));
                        this.mStartedUsers.put(userId, userState);
                        getUserManagerInternal().setUserState(userId, userState.state);
                        updateStartedUserArrayLocked();
                        needStart = true;
                    }
                    UserState uss = this.mStartedUsers.get(userId);
                    Integer userIdInt = Integer.valueOf(userId);
                    this.mUserLru.remove(userIdInt);
                    this.mUserLru.add(userIdInt);
                    if (foreground) {
                        this.mCurrentUserId = userId;
                        this.mService.updateUserConfigurationLocked();
                        this.mTargetUserId = -10000;
                        updateCurrentProfileIdsLocked();
                        this.mService.mWindowManager.setCurrentUser(userId, this.mCurrentProfileIds);
                        this.mService.mWindowManager.lockNow(null);
                    } else {
                        Integer currentUserIdInt = Integer.valueOf(this.mCurrentUserId);
                        updateCurrentProfileIdsLocked();
                        this.mService.mWindowManager.setCurrentProfileIds(this.mCurrentProfileIds);
                        this.mUserLru.remove(currentUserIdInt);
                        this.mUserLru.add(currentUserIdInt);
                    }
                    if (uss.state == 4) {
                        uss.setState(uss.lastState);
                        getUserManagerInternal().setUserState(userId, uss.state);
                        updateStartedUserArrayLocked();
                        needStart = true;
                    } else if (uss.state == 5) {
                        uss.setState(0);
                        getUserManagerInternal().setUserState(userId, uss.state);
                        updateStartedUserArrayLocked();
                        needStart = true;
                    }
                    if (uss.state == 0) {
                        getUserManager().onBeforeStartUser(userId);
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(42, userId, 0));
                    }
                    if (foreground) {
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(43, userId, oldUserId));
                        this.mHandler.removeMessages(34);
                        this.mHandler.removeMessages(36);
                        this.mHandler.sendMessage(this.mHandler.obtainMessage(34, oldUserId, userId, uss));
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(36, oldUserId, userId, uss), 2000L);
                    }
                    if (needStart) {
                        Intent intent = new Intent("android.intent.action.USER_STARTED");
                        intent.addFlags(1342177280);
                        intent.putExtra("android.intent.extra.user_handle", userId);
                        this.mService.broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, userId);
                    }
                    if (foreground) {
                        moveUserToForegroundLocked(uss, oldUserId, userId);
                    } else {
                        this.mService.mUserController.finishUserBoot(uss);
                    }
                    if (needStart) {
                        Intent intent2 = new Intent("android.intent.action.USER_STARTING");
                        intent2.addFlags(1073741824);
                        intent2.putExtra("android.intent.extra.user_handle", userId);
                        this.mService.broadcastIntentLocked(null, null, intent2, null, new IIntentReceiver.Stub() {
                            public void performReceive(Intent intent3, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
                            }
                        }, 0, null, null, new String[]{"android.permission.INTERACT_ACROSS_USERS"}, -1, null, true, false, ActivityManagerService.MY_PID, 1000, -1);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    Binder.restoreCallingIdentity(ident);
                    return true;
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean startUserInForeground(int userId, Dialog dlg) {
        boolean result = startUser(userId, true);
        dlg.dismiss();
        return result;
    }

    boolean unlockUser(int userId, byte[] token, byte[] secret, IProgressListener listener) {
        if (this.mService.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = "Permission Denial: unlockUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        long binderToken = Binder.clearCallingIdentity();
        try {
            return unlockUserCleared(userId, token, secret, listener);
        } finally {
            Binder.restoreCallingIdentity(binderToken);
        }
    }

    boolean maybeUnlockUser(int userId) {
        return unlockUserCleared(userId, null, null, null);
    }

    private static void notifyFinished(int userId, IProgressListener listener) {
        if (listener == null) {
            return;
        }
        try {
            listener.onFinished(userId, (Bundle) null);
        } catch (RemoteException e) {
        }
    }

    boolean unlockUserCleared(int userId, byte[] token, byte[] secret, IProgressListener listener) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (!StorageManager.isUserKeyUnlocked(userId)) {
                    UserInfo userInfo = getUserInfo(userId);
                    IMountService mountService = getMountService();
                    try {
                        mountService.unlockUserKey(userId, userInfo.serialNumber, token, secret);
                    } catch (RemoteException | RuntimeException e) {
                        Slog.w(TAG, "Failed to unlock: " + e.getMessage());
                    }
                }
                UserState uss = this.mStartedUsers.get(userId);
                if (uss == null) {
                    notifyFinished(userId, listener);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                uss.mUnlockProgress.addListener(listener);
                uss.tokenProvided = token != null;
                finishUserUnlocking(uss);
                for (int i = 0; i < this.mStartedUsers.size(); i++) {
                    int testUserId = this.mStartedUsers.keyAt(i);
                    UserInfo parent = getUserManager().getProfileParent(testUserId);
                    if (parent != null && parent.id == userId && testUserId != userId) {
                        Slog.d(TAG, "User " + testUserId + " (parent " + parent.id + "): attempting unlock because parent was just unlocked");
                        maybeUnlockUser(testUserId);
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                return true;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void showUserSwitchDialog(Pair<UserInfo, UserInfo> fromToUserPair) {
        Dialog d = new UserSwitchingDialog(this.mService, this.mService.mContext, (UserInfo) fromToUserPair.first, (UserInfo) fromToUserPair.second, true);
        d.show();
    }

    void dispatchForegroundProfileChanged(int userId) {
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onForegroundProfileSwitch(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    void dispatchUserSwitchComplete(int userId) {
        int observerCount = this.mUserSwitchObservers.beginBroadcast();
        for (int i = 0; i < observerCount; i++) {
            try {
                this.mUserSwitchObservers.getBroadcastItem(i).onUserSwitchComplete(userId);
            } catch (RemoteException e) {
            }
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    private void stopBackgroundUsersIfEnforced(int oldUserId) {
        if (oldUserId == 0) {
            return;
        }
        boolean disallowRunInBg = hasUserRestriction("no_run_in_background", oldUserId);
        if (!disallowRunInBg) {
            return;
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (ActivityManagerDebugConfig.DEBUG_MU) {
                    Slog.i(TAG, "stopBackgroundUsersIfEnforced stopping " + oldUserId + " and related users");
                }
                stopUsersLocked(oldUserId, false, null);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void timeoutUserSwitch(UserState uss, int oldUserId, int newUserId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                Slog.w(TAG, "User switch timeout: from " + oldUserId + " to " + newUserId);
                sendContinueUserSwitchLocked(uss, oldUserId, newUserId);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void dispatchUserSwitch(final UserState uss, final int oldUserId, final int newUserId) {
        Slog.d(TAG, "Dispatch onUserSwitching oldUser #" + oldUserId + " newUser #" + newUserId);
        final int observerCount = this.mUserSwitchObservers.beginBroadcast();
        if (observerCount > 0) {
            IRemoteCallback.Stub stub = new IRemoteCallback.Stub() {
                int mCount = 0;

                public void sendResult(Bundle data) throws RemoteException {
                    synchronized (UserController.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (UserController.this.mCurUserSwitchCallback == this) {
                                this.mCount++;
                                if (this.mCount == observerCount) {
                                    UserController.this.sendContinueUserSwitchLocked(uss, oldUserId, newUserId);
                                }
                            }
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            };
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    uss.switching = true;
                    this.mCurUserSwitchCallback = stub;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            for (int i = 0; i < observerCount; i++) {
                try {
                    this.mUserSwitchObservers.getBroadcastItem(i).onUserSwitching(newUserId, stub);
                } catch (RemoteException e) {
                }
            }
        } else {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    sendContinueUserSwitchLocked(uss, oldUserId, newUserId);
                } catch (Throwable th2) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th2;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
        this.mUserSwitchObservers.finishBroadcast();
    }

    void sendContinueUserSwitchLocked(UserState uss, int oldUserId, int newUserId) {
        this.mCurUserSwitchCallback = null;
        this.mHandler.removeMessages(36);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(35, oldUserId, newUserId, uss));
    }

    void continueUserSwitch(UserState uss, int oldUserId, int newUserId) {
        Slog.d(TAG, "Continue user switch oldUser #" + oldUserId + ", newUser #" + newUserId);
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mWindowManager.stopFreezingScreen();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        uss.switching = false;
        this.mHandler.removeMessages(56);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(56, newUserId, 0));
        stopGuestOrEphemeralUserIfBackground();
        stopBackgroundUsersIfEnforced(oldUserId);
    }

    void moveUserToForegroundLocked(UserState uss, int oldUserId, int newUserId) {
        boolean homeInFront = this.mService.mStackSupervisor.switchUserLocked(newUserId, uss);
        if (homeInFront) {
            this.mService.startHomeActivityLocked(newUserId, "moveUserToForeground");
        } else {
            this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
        EventLogTags.writeAmSwitchUser(newUserId);
        sendUserSwitchBroadcastsLocked(oldUserId, newUserId);
    }

    void sendUserSwitchBroadcastsLocked(int oldUserId, int newUserId) {
        long ident = Binder.clearCallingIdentity();
        if (oldUserId >= 0) {
            try {
                List<UserInfo> profiles = getUserManager().getProfiles(oldUserId, false);
                int count = profiles.size();
                for (int i = 0; i < count; i++) {
                    int profileUserId = profiles.get(i).id;
                    Intent intent = new Intent("android.intent.action.USER_BACKGROUND");
                    intent.addFlags(1342177280);
                    intent.putExtra("android.intent.extra.user_handle", profileUserId);
                    this.mService.broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, profileUserId);
                }
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        if (newUserId >= 0) {
            List<UserInfo> profiles2 = getUserManager().getProfiles(newUserId, false);
            int count2 = profiles2.size();
            for (int i2 = 0; i2 < count2; i2++) {
                int profileUserId2 = profiles2.get(i2).id;
                Intent intent2 = new Intent("android.intent.action.USER_FOREGROUND");
                intent2.addFlags(1342177280);
                intent2.putExtra("android.intent.extra.user_handle", profileUserId2);
                this.mService.broadcastIntentLocked(null, null, intent2, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, profileUserId2);
            }
            Intent intent3 = new Intent("android.intent.action.USER_SWITCHED");
            intent3.addFlags(1342177280);
            intent3.putExtra("android.intent.extra.user_handle", newUserId);
            this.mService.broadcastIntentLocked(null, null, intent3, null, null, 0, null, null, new String[]{"android.permission.MANAGE_USERS"}, -1, null, false, false, ActivityManagerService.MY_PID, 1000, -1);
        }
    }

    int handleIncomingUser(int callingPid, int callingUid, int userId, boolean allowAll, int allowMode, String name, String callerPackage) {
        boolean zIsSameProfileGroup;
        int callingUserId = UserHandle.getUserId(callingUid);
        if (callingUserId == userId) {
            return userId;
        }
        int targetUserId = unsafeConvertIncomingUserLocked(userId);
        if (callingUid != 0 && callingUid != 1000) {
            if (this.mService.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS_FULL", callingPid, callingUid, -1, true) == 0) {
                zIsSameProfileGroup = true;
            } else if (allowMode == 2 || this.mService.checkComponentPermission("android.permission.INTERACT_ACROSS_USERS", callingPid, callingUid, -1, true) != 0) {
                zIsSameProfileGroup = false;
            } else if (allowMode == 0) {
                zIsSameProfileGroup = true;
            } else if (allowMode == 1) {
                zIsSameProfileGroup = isSameProfileGroup(callingUserId, targetUserId);
            } else {
                throw new IllegalArgumentException("Unknown mode: " + allowMode);
            }
            if (!zIsSameProfileGroup) {
                if (userId == -3) {
                    targetUserId = callingUserId;
                } else {
                    StringBuilder builder = new StringBuilder(128);
                    builder.append("Permission Denial: ");
                    builder.append(name);
                    if (callerPackage != null) {
                        builder.append(" from ");
                        builder.append(callerPackage);
                    }
                    builder.append(" asks to run as user ");
                    builder.append(userId);
                    builder.append(" but is calling from user ");
                    builder.append(UserHandle.getUserId(callingUid));
                    builder.append("; this requires ");
                    builder.append("android.permission.INTERACT_ACROSS_USERS_FULL");
                    if (allowMode != 2) {
                        builder.append(" or ");
                        builder.append("android.permission.INTERACT_ACROSS_USERS");
                    }
                    String msg = builder.toString();
                    Slog.w(TAG, msg);
                    throw new SecurityException(msg);
                }
            }
        }
        if (!allowAll && targetUserId < 0) {
            throw new IllegalArgumentException("Call does not support special user #" + targetUserId);
        }
        if (callingUid == USER_SWITCH_TIMEOUT && targetUserId >= 0 && hasUserRestriction("no_debugging_features", targetUserId)) {
            throw new SecurityException("Shell does not have permission to access user " + targetUserId + "\n " + Debug.getCallers(3));
        }
        return targetUserId;
    }

    int unsafeConvertIncomingUserLocked(int userId) {
        if (userId != -2 && userId != -3) {
            return userId;
        }
        return getCurrentUserIdLocked();
    }

    void registerUserSwitchObserver(IUserSwitchObserver observer) {
        if (this.mService.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            this.mUserSwitchObservers.register(observer);
        } else {
            String msg = "Permission Denial: registerUserSwitchObserver() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS_FULL";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
    }

    void unregisterUserSwitchObserver(IUserSwitchObserver observer) {
        this.mUserSwitchObservers.unregister(observer);
    }

    UserState getStartedUserStateLocked(int userId) {
        return this.mStartedUsers.get(userId);
    }

    boolean hasStartedUserState(int userId) {
        return this.mStartedUsers.get(userId) != null;
    }

    private void updateStartedUserArrayLocked() {
        int num = 0;
        for (int i = 0; i < this.mStartedUsers.size(); i++) {
            UserState uss = this.mStartedUsers.valueAt(i);
            if (uss.state != 4 && uss.state != 5) {
                num++;
            }
        }
        this.mStartedUserArray = new int[num];
        int num2 = 0;
        for (int i2 = 0; i2 < this.mStartedUsers.size(); i2++) {
            UserState uss2 = this.mStartedUsers.valueAt(i2);
            if (uss2.state != 4 && uss2.state != 5) {
                this.mStartedUserArray[num2] = this.mStartedUsers.keyAt(i2);
                num2++;
            }
        }
    }

    void sendBootCompletedLocked(IIntentReceiver resultTo) {
        for (int i = 0; i < this.mStartedUsers.size(); i++) {
            UserState uss = this.mStartedUsers.valueAt(i);
            finishUserBoot(uss, resultTo);
        }
    }

    void onSystemReady() {
        updateCurrentProfileIdsLocked();
    }

    private void updateCurrentProfileIdsLocked() {
        List<UserInfo> profiles = getUserManager().getProfiles(this.mCurrentUserId, false);
        int[] currentProfileIds = new int[profiles.size()];
        for (int i = 0; i < currentProfileIds.length; i++) {
            currentProfileIds[i] = profiles.get(i).id;
        }
        this.mCurrentProfileIds = currentProfileIds;
        synchronized (this.mUserProfileGroupIdsSelfLocked) {
            this.mUserProfileGroupIdsSelfLocked.clear();
            List<UserInfo> users = getUserManager().getUsers(false);
            for (int i2 = 0; i2 < users.size(); i2++) {
                UserInfo user = users.get(i2);
                if (user.profileGroupId != -10000) {
                    this.mUserProfileGroupIdsSelfLocked.put(user.id, user.profileGroupId);
                }
            }
        }
    }

    int[] getStartedUserArrayLocked() {
        return this.mStartedUserArray;
    }

    boolean isUserStoppingOrShuttingDownLocked(int userId) {
        UserState state = getStartedUserStateLocked(userId);
        if (state == null) {
            return false;
        }
        return state.state == 4 || state.state == 5;
    }

    boolean isUserRunningLocked(int userId, int flags) {
        UserState state = getStartedUserStateLocked(userId);
        if (state == null) {
            return false;
        }
        if ((flags & 1) != 0) {
            return true;
        }
        if ((flags & 2) != 0) {
            switch (state.state) {
                case 0:
                case 1:
                    return true;
                default:
                    return false;
            }
        }
        if ((flags & 8) != 0) {
            switch (state.state) {
                case 2:
                case 3:
                    return true;
                default:
                    return false;
            }
        }
        if ((flags & 4) == 0) {
            return true;
        }
        switch (state.state) {
            case 3:
                return true;
            default:
                return false;
        }
    }

    UserInfo getCurrentUser() {
        UserInfo currentUserLocked;
        if (this.mService.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS") != 0 && this.mService.checkCallingPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0) {
            String msg = "Permission Denial: getCurrentUser() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.INTERACT_ACROSS_USERS";
            Slog.w(TAG, msg);
            throw new SecurityException(msg);
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                currentUserLocked = getCurrentUserLocked();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return currentUserLocked;
    }

    UserInfo getCurrentUserLocked() {
        int userId = this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
        return getUserInfo(userId);
    }

    int getCurrentOrTargetUserIdLocked() {
        return this.mTargetUserId != -10000 ? this.mTargetUserId : this.mCurrentUserId;
    }

    int getCurrentUserIdLocked() {
        return this.mCurrentUserId;
    }

    private boolean isCurrentUserLocked(int userId) {
        return userId == getCurrentOrTargetUserIdLocked();
    }

    int setTargetUserIdLocked(int targetUserId) {
        this.mTargetUserId = targetUserId;
        return targetUserId;
    }

    int[] getUsers() {
        UserManagerService ums = getUserManager();
        return ums != null ? ums.getUserIds() : new int[]{0};
    }

    UserInfo getUserInfo(int userId) {
        return getUserManager().getUserInfo(userId);
    }

    int[] getUserIds() {
        return getUserManager().getUserIds();
    }

    boolean exists(int userId) {
        return getUserManager().exists(userId);
    }

    boolean hasUserRestriction(String restriction, int userId) {
        return getUserManager().hasUserRestriction(restriction, userId);
    }

    Set<Integer> getProfileIds(int userId) {
        Set<Integer> userIds = new HashSet<>();
        List<UserInfo> profiles = getUserManager().getProfiles(userId, false);
        for (UserInfo user : profiles) {
            userIds.add(Integer.valueOf(user.id));
        }
        return userIds;
    }

    boolean isSameProfileGroup(int callingUserId, int targetUserId) {
        boolean z = false;
        synchronized (this.mUserProfileGroupIdsSelfLocked) {
            int callingProfile = this.mUserProfileGroupIdsSelfLocked.get(callingUserId, -10000);
            int targetProfile = this.mUserProfileGroupIdsSelfLocked.get(targetUserId, -10000);
            if (callingProfile != -10000 && callingProfile == targetProfile) {
                z = true;
            }
        }
        return z;
    }

    boolean isCurrentProfileLocked(int userId) {
        return ArrayUtils.contains(this.mCurrentProfileIds, userId);
    }

    int[] getCurrentProfileIdsLocked() {
        return this.mCurrentProfileIds;
    }

    boolean shouldConfirmCredentials(int userId) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mStartedUsers.get(userId) == null) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return false;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (!this.mLockPatternUtils.isSeparateProfileChallengeEnabled(userId)) {
                    return false;
                }
                KeyguardManager km = (KeyguardManager) this.mService.mContext.getSystemService("keyguard");
                if (km.isDeviceLocked(userId)) {
                    return km.isDeviceSecure(userId);
                }
                return false;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    boolean isLockScreenDisabled(int userId) {
        return this.mLockPatternUtils.isLockScreenDisabled(userId);
    }

    private UserManagerInternal getUserManagerInternal() {
        if (this.mUserManagerInternal == null) {
            this.mUserManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        }
        return this.mUserManagerInternal;
    }

    void dump(PrintWriter pw, boolean dumpAll) {
        pw.println("  mStartedUsers:");
        for (int i = 0; i < this.mStartedUsers.size(); i++) {
            UserState uss = this.mStartedUsers.valueAt(i);
            pw.print("    User #");
            pw.print(uss.mHandle.getIdentifier());
            pw.print(": ");
            uss.dump("", pw);
        }
        pw.print("  mStartedUserArray: [");
        for (int i2 = 0; i2 < this.mStartedUserArray.length; i2++) {
            if (i2 > 0) {
                pw.print(", ");
            }
            pw.print(this.mStartedUserArray[i2]);
        }
        pw.println("]");
        pw.print("  mUserLru: [");
        for (int i3 = 0; i3 < this.mUserLru.size(); i3++) {
            if (i3 > 0) {
                pw.print(", ");
            }
            pw.print(this.mUserLru.get(i3));
        }
        pw.println("]");
        if (dumpAll) {
            pw.print("  mStartedUserArray: ");
            pw.println(Arrays.toString(this.mStartedUserArray));
        }
        synchronized (this.mUserProfileGroupIdsSelfLocked) {
            if (this.mUserProfileGroupIdsSelfLocked.size() > 0) {
                pw.println("  mUserProfileGroupIds:");
                for (int i4 = 0; i4 < this.mUserProfileGroupIdsSelfLocked.size(); i4++) {
                    pw.print("    User #");
                    pw.print(this.mUserProfileGroupIdsSelfLocked.keyAt(i4));
                    pw.print(" -> profile #");
                    pw.println(this.mUserProfileGroupIdsSelfLocked.valueAt(i4));
                }
            }
        }
    }
}
