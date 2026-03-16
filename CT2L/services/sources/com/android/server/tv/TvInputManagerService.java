package com.android.server.tv;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.media.tv.ITvInputClient;
import android.media.tv.ITvInputHardware;
import android.media.tv.ITvInputHardwareCallback;
import android.media.tv.ITvInputManager;
import android.media.tv.ITvInputManagerCallback;
import android.media.tv.ITvInputService;
import android.media.tv.ITvInputServiceCallback;
import android.media.tv.ITvInputSession;
import android.media.tv.ITvInputSessionCallback;
import android.media.tv.TvContentRating;
import android.media.tv.TvContentRatingSystemInfo;
import android.media.tv.TvContract;
import android.media.tv.TvInputHardwareInfo;
import android.media.tv.TvInputInfo;
import android.media.tv.TvStreamConfig;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputChannel;
import android.view.Surface;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.SystemService;
import com.android.server.tv.TvInputHardwareManager;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

public final class TvInputManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String TAG = "TvInputManagerService";
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mCurrentUserId;
    private final Object mLock;
    private final TvInputHardwareManager mTvInputHardwareManager;
    private final SparseArray<UserState> mUserStates;
    private final WatchLogHandler mWatchLogHandler;

    public TvInputManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mCurrentUserId = 0;
        this.mUserStates = new SparseArray<>();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mWatchLogHandler = new WatchLogHandler(this.mContentResolver, IoThread.get().getLooper());
        this.mTvInputHardwareManager = new TvInputHardwareManager(context, new HardwareListener());
        synchronized (this.mLock) {
            this.mUserStates.put(this.mCurrentUserId, new UserState(this.mContext, this.mCurrentUserId));
        }
    }

    @Override
    public void onStart() {
        publishBinderService("tv_input", new BinderService());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 500) {
            registerBroadcastReceivers();
        } else if (phase == 600) {
            synchronized (this.mLock) {
                buildTvInputListLocked(this.mCurrentUserId, null);
                buildTvContentRatingSystemListLocked(this.mCurrentUserId);
            }
        }
        this.mTvInputHardwareManager.onBootPhase(phase);
    }

    private void registerBroadcastReceivers() {
        PackageMonitor monitor = new PackageMonitor() {
            private void buildTvInputList(String[] packages) {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.buildTvInputListLocked(getChangingUserId(), packages);
                    TvInputManagerService.this.buildTvContentRatingSystemListLocked(getChangingUserId());
                }
            }

            public void onPackageUpdateFinished(String packageName, int uid) {
                buildTvInputList(new String[]{packageName});
            }

            public void onPackagesAvailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInputList(packages);
                }
            }

            public void onPackagesUnavailable(String[] packages) {
                if (isReplacing()) {
                    buildTvInputList(packages);
                }
            }

            public void onSomePackagesChanged() {
                if (!isReplacing()) {
                    buildTvInputList(null);
                }
            }

            public void onPackageRemoved(String packageName, int uid) {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(getChangingUserId());
                    if (userState.packageSet.contains(packageName)) {
                        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
                        String[] selectionArgs = {packageName};
                        operations.add(ContentProviderOperation.newDelete(TvContract.Channels.CONTENT_URI).withSelection("package_name=?", selectionArgs).build());
                        operations.add(ContentProviderOperation.newDelete(TvContract.Programs.CONTENT_URI).withSelection("package_name=?", selectionArgs).build());
                        operations.add(ContentProviderOperation.newDelete(TvContract.WatchedPrograms.CONTENT_URI).withSelection("package_name=?", selectionArgs).build());
                        try {
                            TvInputManagerService.this.mContentResolver.applyBatch("android.media.tv", operations);
                        } catch (OperationApplicationException | RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in applyBatch", e);
                        }
                    }
                }
            }
        };
        monitor.register(this.mContext, (Looper) null, UserHandle.ALL, true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    TvInputManagerService.this.switchUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    TvInputManagerService.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", 0));
                }
            }
        }, UserHandle.ALL, intentFilter, null, null);
    }

    private static boolean hasHardwarePermission(PackageManager pm, ComponentName component) {
        if (pm.checkPermission("android.permission.TV_INPUT_HARDWARE", component.getPackageName()) == 0) {
            return true;
        }
        return DEBUG;
    }

    private void buildTvInputListLocked(int userId, String[] updatedPackages) {
        UserState userState = getUserStateLocked(userId);
        userState.packageSet.clear();
        PackageManager pm = this.mContext.getPackageManager();
        List<ResolveInfo> services = pm.queryIntentServices(new Intent("android.media.tv.TvInputService"), 132);
        List<TvInputInfo> inputList = new ArrayList<>();
        for (ResolveInfo ri : services) {
            ServiceInfo si = ri.serviceInfo;
            if (!"android.permission.BIND_TV_INPUT".equals(si.permission)) {
                Slog.w(TAG, "Skipping TV input " + si.name + ": it does not require the permission android.permission.BIND_TV_INPUT");
            } else {
                ComponentName component = new ComponentName(si.packageName, si.name);
                if (!hasHardwarePermission(pm, component)) {
                    try {
                        inputList.add(TvInputInfo.createTvInputInfo(this.mContext, ri));
                    } catch (IOException | XmlPullParserException e) {
                        Slog.e(TAG, "failed to load TV input " + si.name, e);
                    }
                } else {
                    ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
                    if (serviceState == null) {
                        userState.serviceStateMap.put(component, new ServiceState(component, userId));
                        updateServiceConnectionLocked(component, userId);
                    } else {
                        inputList.addAll(serviceState.inputList);
                    }
                }
                userState.packageSet.add(si.packageName);
            }
        }
        Map<String, TvInputState> inputMap = new HashMap<>();
        for (TvInputInfo info : inputList) {
            TvInputState state = (TvInputState) userState.inputMap.get(info.getId());
            if (state == null) {
                state = new TvInputState();
            }
            state.info = info;
            inputMap.put(info.getId(), state);
        }
        for (String inputId : inputMap.keySet()) {
            if (!userState.inputMap.containsKey(inputId)) {
                notifyInputAddedLocked(userState, inputId);
            } else if (updatedPackages != null) {
                ComponentName component2 = inputMap.get(inputId).info.getComponent();
                int len$ = updatedPackages.length;
                int i$ = 0;
                while (true) {
                    if (i$ < len$) {
                        String updatedPackage = updatedPackages[i$];
                        if (!component2.getPackageName().equals(updatedPackage)) {
                            i$++;
                        } else {
                            updateServiceConnectionLocked(component2, userId);
                            notifyInputUpdatedLocked(userState, inputId);
                            break;
                        }
                    }
                }
            }
        }
        for (String inputId2 : userState.inputMap.keySet()) {
            if (!inputMap.containsKey(inputId2)) {
                ServiceState serviceState2 = (ServiceState) userState.serviceStateMap.get(((TvInputState) userState.inputMap.get(inputId2)).info.getComponent());
                if (serviceState2 != null) {
                    abortPendingCreateSessionRequestsLocked(serviceState2, inputId2, userId);
                }
                notifyInputRemovedLocked(userState, inputId2);
            }
        }
        userState.inputMap.clear();
        userState.inputMap = inputMap;
    }

    private void buildTvContentRatingSystemListLocked(int userId) {
        UserState userState = getUserStateLocked(userId);
        userState.contentRatingSystemList.clear();
        PackageManager pm = this.mContext.getPackageManager();
        Intent intent = new Intent("android.media.tv.action.QUERY_CONTENT_RATING_SYSTEMS");
        for (ResolveInfo resolveInfo : pm.queryBroadcastReceivers(intent, 128)) {
            ActivityInfo receiver = resolveInfo.activityInfo;
            Bundle metaData = receiver.metaData;
            if (metaData != null) {
                int xmlResId = metaData.getInt("android.media.tv.metadata.CONTENT_RATING_SYSTEMS");
                if (xmlResId == 0) {
                    Slog.w(TAG, "Missing meta-data 'android.media.tv.metadata.CONTENT_RATING_SYSTEMS' on receiver " + receiver.packageName + "/" + receiver.name);
                } else {
                    userState.contentRatingSystemList.add(TvContentRatingSystemInfo.createTvContentRatingSystemInfo(xmlResId, receiver.applicationInfo));
                }
            }
        }
    }

    private void switchUser(int userId) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId != userId) {
                this.mCurrentUserId = userId;
                UserState userState = this.mUserStates.get(userId);
                if (userState == null) {
                    userState = new UserState(this.mContext, userId);
                }
                this.mUserStates.put(userId, userState);
                buildTvInputListLocked(userId, null);
                buildTvContentRatingSystemListLocked(userId);
            }
        }
    }

    private void removeUser(int userId) {
        synchronized (this.mLock) {
            UserState userState = this.mUserStates.get(userId);
            if (userState == null) {
                return;
            }
            for (SessionState state : userState.sessionStateMap.values()) {
                if (state.session != null) {
                    try {
                        state.session.release();
                    } catch (RemoteException e) {
                        Slog.e(TAG, "error in release", e);
                    }
                }
            }
            userState.sessionStateMap.clear();
            for (ServiceState serviceState : userState.serviceStateMap.values()) {
                if (serviceState.callback != null) {
                    try {
                        serviceState.service.unregisterCallback(serviceState.callback);
                    } catch (RemoteException e2) {
                        Slog.e(TAG, "error in unregisterCallback", e2);
                    }
                }
                this.mContext.unbindService(serviceState.connection);
            }
            userState.serviceStateMap.clear();
            userState.inputMap.clear();
            userState.packageSet.clear();
            userState.contentRatingSystemList.clear();
            userState.clientStateMap.clear();
            userState.callbackSet.clear();
            userState.mainSessionToken = null;
            this.mUserStates.remove(userId);
        }
    }

    private UserState getUserStateLocked(int userId) {
        UserState userState = this.mUserStates.get(userId);
        if (userState == null) {
            throw new IllegalStateException("User state not found for user ID " + userId);
        }
        return userState;
    }

    private ServiceState getServiceStateLocked(ComponentName component, int userId) {
        UserState userState = getUserStateLocked(userId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
        if (serviceState == null) {
            throw new IllegalStateException("Service state not found for " + component + " (userId=" + userId + ")");
        }
        return serviceState;
    }

    private SessionState getSessionStateLocked(IBinder sessionToken, int callingUid, int userId) {
        UserState userState = getUserStateLocked(userId);
        SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
        if (sessionState == null) {
            throw new SessionNotFoundException("Session state not found for token " + sessionToken);
        }
        if (callingUid != 1000 && callingUid != sessionState.callingUid) {
            throw new SecurityException("Illegal access to the session with token " + sessionToken + " from uid " + callingUid);
        }
        return sessionState;
    }

    private ITvInputSession getSessionLocked(IBinder sessionToken, int callingUid, int userId) {
        return getSessionLocked(getSessionStateLocked(sessionToken, callingUid, userId));
    }

    private ITvInputSession getSessionLocked(SessionState sessionState) {
        ITvInputSession session = sessionState.session;
        if (session == null) {
            throw new IllegalStateException("Session not yet created for token " + sessionState.sessionToken);
        }
        return session;
    }

    private int resolveCallingUserId(int callingPid, int callingUid, int requestedUserId, String methodName) {
        return ActivityManager.handleIncomingUser(callingPid, callingUid, requestedUserId, DEBUG, DEBUG, methodName, null);
    }

    private static boolean shouldMaintainConnection(ServiceState serviceState) {
        if (!serviceState.sessionTokens.isEmpty() || serviceState.isHardware) {
            return true;
        }
        return DEBUG;
    }

    private void updateServiceConnectionLocked(ComponentName component, int userId) {
        UserState userState = getUserStateLocked(userId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(component);
        if (serviceState == null) {
            return;
        }
        if (serviceState.reconnecting) {
            if (!serviceState.sessionTokens.isEmpty()) {
                return;
            } else {
                serviceState.reconnecting = DEBUG;
            }
        }
        boolean maintainConnection = shouldMaintainConnection(serviceState);
        if (serviceState.service != null || !maintainConnection || userId != this.mCurrentUserId) {
            if (serviceState.service == null || maintainConnection) {
                return;
            }
            this.mContext.unbindService(serviceState.connection);
            userState.serviceStateMap.remove(component);
            return;
        }
        if (!serviceState.bound) {
            Intent i = new Intent("android.media.tv.TvInputService").setComponent(component);
            serviceState.bound = this.mContext.bindServiceAsUser(i, serviceState.connection, 1, new UserHandle(userId));
        }
    }

    private void abortPendingCreateSessionRequestsLocked(ServiceState serviceState, String inputId, int userId) {
        UserState userState = getUserStateLocked(userId);
        List<SessionState> sessionsToAbort = new ArrayList<>();
        for (IBinder sessionToken : serviceState.sessionTokens) {
            SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
            if (sessionState.session == null && (inputId == null || sessionState.info.getId().equals(inputId))) {
                sessionsToAbort.add(sessionState);
            }
        }
        for (SessionState sessionState2 : sessionsToAbort) {
            removeSessionStateLocked(sessionState2.sessionToken, sessionState2.userId);
            sendSessionTokenToClientLocked(sessionState2.client, sessionState2.info.getId(), null, null, sessionState2.seq);
        }
        updateServiceConnectionLocked(serviceState.component, userId);
    }

    private void createSessionInternalLocked(ITvInputService service, IBinder sessionToken, int userId) {
        UserState userState = getUserStateLocked(userId);
        SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
        InputChannel[] channels = InputChannel.openInputChannelPair(sessionToken.toString());
        try {
            service.createSession(channels[1], new SessionCallback(sessionState, channels), sessionState.info.getId());
        } catch (RemoteException e) {
            Slog.e(TAG, "error in createSession", e);
            removeSessionStateLocked(sessionToken, userId);
            sendSessionTokenToClientLocked(sessionState.client, sessionState.info.getId(), null, null, sessionState.seq);
        }
        channels[1].dispose();
    }

    private void sendSessionTokenToClientLocked(ITvInputClient client, String inputId, IBinder sessionToken, InputChannel channel, int seq) {
        try {
            client.onSessionCreated(inputId, sessionToken, channel, seq);
        } catch (RemoteException e) {
            Slog.e(TAG, "error in onSessionCreated", e);
        }
    }

    private void releaseSessionLocked(IBinder sessionToken, int callingUid, int userId) {
        Exception e;
        SessionState sessionState = null;
        try {
            try {
                sessionState = getSessionStateLocked(sessionToken, callingUid, userId);
                if (sessionState.session != null) {
                    UserState userState = getUserStateLocked(userId);
                    if (sessionToken == userState.mainSessionToken) {
                        setMainLocked(sessionToken, DEBUG, callingUid, userId);
                    }
                    sessionState.session.release();
                }
            } finally {
                if (0 != 0) {
                    null.session = null;
                }
            }
        } catch (RemoteException e2) {
            e = e2;
            Slog.e(TAG, "error in releaseSession", e);
            if (sessionState != null) {
                sessionState.session = null;
            }
        } catch (SessionNotFoundException e3) {
            e = e3;
            Slog.e(TAG, "error in releaseSession", e);
            if (sessionState != null) {
            }
        }
        removeSessionStateLocked(sessionToken, userId);
    }

    private void removeSessionStateLocked(IBinder sessionToken, int userId) {
        ServiceState serviceState;
        UserState userState = getUserStateLocked(userId);
        if (sessionToken == userState.mainSessionToken) {
            userState.mainSessionToken = null;
        }
        SessionState sessionState = (SessionState) userState.sessionStateMap.remove(sessionToken);
        if (sessionState == null) {
            return;
        }
        ClientState clientState = (ClientState) userState.clientStateMap.get(sessionState.client.asBinder());
        if (clientState != null) {
            clientState.sessionTokens.remove(sessionToken);
            if (clientState.isEmpty()) {
                userState.clientStateMap.remove(sessionState.client.asBinder());
            }
        }
        TvInputInfo info = sessionState.info;
        if (info != null && (serviceState = (ServiceState) userState.serviceStateMap.get(info.getComponent())) != null) {
            serviceState.sessionTokens.remove(sessionToken);
        }
        updateServiceConnectionLocked(sessionState.info.getComponent(), userId);
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = sessionToken;
        args.arg2 = Long.valueOf(System.currentTimeMillis());
        this.mWatchLogHandler.obtainMessage(2, args).sendToTarget();
    }

    private void setMainLocked(IBinder sessionToken, boolean isMain, int callingUid, int userId) {
        try {
            SessionState sessionState = getSessionStateLocked(sessionToken, callingUid, userId);
            if (sessionState.hardwareSessionToken != null) {
                sessionState = getSessionStateLocked(sessionState.hardwareSessionToken, 1000, userId);
            }
            ServiceState serviceState = getServiceStateLocked(sessionState.info.getComponent(), userId);
            if (serviceState.isHardware) {
                ITvInputSession session = getSessionLocked(sessionState);
                session.setMain(isMain);
            }
        } catch (RemoteException | SessionNotFoundException e) {
            Slog.e(TAG, "error in setMain", e);
        }
    }

    private void notifyInputAddedLocked(UserState userState, String inputId) {
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onInputAdded(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report added input to callback", e);
            }
        }
    }

    private void notifyInputRemovedLocked(UserState userState, String inputId) {
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onInputRemoved(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report removed input to callback", e);
            }
        }
    }

    private void notifyInputUpdatedLocked(UserState userState, String inputId) {
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onInputUpdated(inputId);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated input to callback", e);
            }
        }
    }

    private void notifyInputStateChangedLocked(UserState userState, String inputId, int state, ITvInputManagerCallback targetCallback) {
        if (targetCallback != null) {
            try {
                targetCallback.onInputStateChanged(inputId, state);
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report state change to callback", e);
                return;
            }
        }
        for (ITvInputManagerCallback callback : userState.callbackSet) {
            try {
                callback.onInputStateChanged(inputId, state);
            } catch (RemoteException e2) {
                Slog.e(TAG, "failed to report state change to callback", e2);
            }
        }
    }

    private void setStateLocked(String inputId, int state, int userId) {
        UserState userState = getUserStateLocked(userId);
        TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
        ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(inputState.info.getComponent());
        int oldState = inputState.state;
        inputState.state = state;
        if ((serviceState == null || serviceState.service != null || !shouldMaintainConnection(serviceState)) && oldState != state) {
            notifyInputStateChangedLocked(userState, inputId, state, null);
        }
    }

    private final class BinderService extends ITvInputManager.Stub {
        private BinderService() {
        }

        public List<TvInputInfo> getTvInputList(int userId) {
            List<TvInputInfo> inputList;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputList");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    inputList = new ArrayList<>();
                    for (TvInputState state : userState.inputMap.values()) {
                        inputList.add(state.info);
                    }
                }
                return inputList;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public TvInputInfo getTvInputInfo(String inputId, int userId) {
            TvInputInfo tvInputInfo;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputInfo");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    TvInputState state = (TvInputState) userState.inputMap.get(inputId);
                    tvInputInfo = state == null ? null : state.info;
                }
                return tvInputInfo;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public int getTvInputState(String inputId, int userId) {
            int i;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvInputState");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    TvInputState state = (TvInputState) userState.inputMap.get(inputId);
                    i = state == null ? -1 : state.state;
                }
                return i;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<TvContentRatingSystemInfo> getTvContentRatingSystemList(int userId) {
            List<TvContentRatingSystemInfo> list;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getTvContentRatingSystemList");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    list = userState.contentRatingSystemList;
                }
                return list;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void registerCallback(final ITvInputManagerCallback callback, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "registerCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    final UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    userState.callbackSet.add(callback);
                    try {
                        callback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                synchronized (TvInputManagerService.this.mLock) {
                                    if (userState.callbackSet != null) {
                                        userState.callbackSet.remove(callback);
                                    }
                                }
                            }
                        }, 0);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void unregisterCallback(ITvInputManagerCallback callback, int userId) {
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "unregisterCallback");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    userState.callbackSet.remove(callback);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isParentalControlsEnabled(int userId) {
            boolean zIsParentalControlsEnabled;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "isParentalControlsEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    zIsParentalControlsEnabled = userState.persistentDataStore.isParentalControlsEnabled();
                }
                return zIsParentalControlsEnabled;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setParentalControlsEnabled(boolean enabled, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "setParentalControlsEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    userState.persistentDataStore.setParentalControlsEnabled(enabled);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isRatingBlocked(String rating, int userId) {
            boolean zIsRatingBlocked;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "isRatingBlocked");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    zIsRatingBlocked = userState.persistentDataStore.isRatingBlocked(TvContentRating.unflattenFromString(rating));
                }
                return zIsRatingBlocked;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<String> getBlockedRatings(int userId) {
            List<String> ratings;
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "getBlockedRatings");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    ratings = new ArrayList<>();
                    TvContentRating[] arr$ = userState.persistentDataStore.getBlockedRatings();
                    for (TvContentRating rating : arr$) {
                        ratings.add(rating.flattenToString());
                    }
                }
                return ratings;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void addBlockedRating(String rating, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "addBlockedRating");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    userState.persistentDataStore.addBlockedRating(TvContentRating.unflattenFromString(rating));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeBlockedRating(String rating, int userId) {
            ensureParentalControlsPermission();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), userId, "removeBlockedRating");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    userState.persistentDataStore.removeBlockedRating(TvContentRating.unflattenFromString(rating));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private void ensureParentalControlsPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.MODIFY_PARENTAL_CONTROLS") != 0) {
                throw new SecurityException("The caller does not have parental controls permission");
            }
        }

        public void createSession(ITvInputClient client, String inputId, int seq, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "createSession");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    TvInputState inputState = (TvInputState) userState.inputMap.get(inputId);
                    if (inputState == null) {
                        Slog.w(TvInputManagerService.TAG, "Failed to find input state for inputId=" + inputId);
                        TvInputManagerService.this.sendSessionTokenToClientLocked(client, inputId, null, null, seq);
                        return;
                    }
                    TvInputInfo info = inputState.info;
                    ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(info.getComponent());
                    if (serviceState == null) {
                        serviceState = new ServiceState(info.getComponent(), resolvedUserId);
                        userState.serviceStateMap.put(info.getComponent(), serviceState);
                    }
                    if (serviceState.reconnecting) {
                        TvInputManagerService.this.sendSessionTokenToClientLocked(client, inputId, null, null, seq);
                        return;
                    }
                    Binder binder = new Binder();
                    SessionState sessionState = new SessionState(binder, info, client, seq, callingUid, resolvedUserId);
                    userState.sessionStateMap.put(binder, sessionState);
                    serviceState.sessionTokens.add(binder);
                    if (serviceState.service != null) {
                        TvInputManagerService.this.createSessionInternalLocked(serviceState.service, binder, resolvedUserId);
                    } else {
                        TvInputManagerService.this.updateServiceConnectionLocked(info.getComponent(), resolvedUserId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void releaseSession(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "releaseSession");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.releaseSessionLocked(sessionToken, callingUid, resolvedUserId);
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setMainSession(IBinder sessionToken, int userId) {
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setMainSession");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    if (userState.mainSessionToken == sessionToken) {
                        return;
                    }
                    IBinder oldMainSessionToken = userState.mainSessionToken;
                    userState.mainSessionToken = sessionToken;
                    if (sessionToken != null) {
                        TvInputManagerService.this.setMainLocked(sessionToken, true, callingUid, userId);
                    }
                    if (oldMainSessionToken != null) {
                        TvInputManagerService.this.setMainLocked(oldMainSessionToken, TvInputManagerService.DEBUG, 1000, userId);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setSurface(IBinder sessionToken, Surface surface, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setSurface");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        if (sessionState.hardwareSessionToken == null) {
                            TvInputManagerService.this.getSessionLocked(sessionState).setSurface(surface);
                        } else {
                            TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId).setSurface(surface);
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in setSurface", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in setSurface", e);
                    }
                }
            } finally {
                if (surface != null) {
                    surface.release();
                }
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void dispatchSurfaceChanged(IBinder sessionToken, int format, int width, int height, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "dispatchSurfaceChanged");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).dispatchSurfaceChanged(format, width, height);
                        if (sessionState.hardwareSessionToken != null) {
                            TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId).dispatchSurfaceChanged(format, width, height);
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in dispatchSurfaceChanged", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in dispatchSurfaceChanged", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setVolume(IBinder sessionToken, float volume, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setVolume");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionState = TvInputManagerService.this.getSessionStateLocked(sessionToken, callingUid, resolvedUserId);
                        TvInputManagerService.this.getSessionLocked(sessionState).setVolume(volume);
                        if (sessionState.hardwareSessionToken != null) {
                            TvInputManagerService.this.getSessionLocked(sessionState.hardwareSessionToken, 1000, resolvedUserId).setVolume(volume > 0.0f ? 1.0f : 0.0f);
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in setVolume", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in setVolume", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void tune(IBinder sessionToken, Uri channelUri, Bundle params, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "tune");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).tune(channelUri, params);
                        if (!TvContract.isChannelUriForPassthroughInput(channelUri)) {
                            UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                            SessionState sessionState = (SessionState) userState.sessionStateMap.get(sessionToken);
                            SomeArgs args = SomeArgs.obtain();
                            args.arg1 = sessionState.info.getComponent().getPackageName();
                            args.arg2 = Long.valueOf(System.currentTimeMillis());
                            args.arg3 = Long.valueOf(ContentUris.parseId(channelUri));
                            args.arg4 = params;
                            args.arg5 = sessionToken;
                            TvInputManagerService.this.mWatchLogHandler.obtainMessage(1, args).sendToTarget();
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in tune", e);
                        Binder.restoreCallingIdentity(identity);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in tune", e);
                        Binder.restoreCallingIdentity(identity);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void requestUnblockContent(IBinder sessionToken, String unblockedRating, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "unblockContent");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).requestUnblockContent(unblockedRating);
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in requestUnblockContent", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in requestUnblockContent", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void setCaptionEnabled(IBinder sessionToken, boolean enabled, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "setCaptionEnabled");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).setCaptionEnabled(enabled);
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in setCaptionEnabled", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in setCaptionEnabled", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void selectTrack(IBinder sessionToken, int type, String trackId, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "selectTrack");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).selectTrack(type, trackId);
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in selectTrack", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in selectTrack", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void sendAppPrivateCommand(IBinder sessionToken, String command, Bundle data, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "sendAppPrivateCommand");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).appPrivateCommand(command, data);
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in appPrivateCommand", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in appPrivateCommand", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void createOverlayView(IBinder sessionToken, IBinder windowToken, Rect frame, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "createOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).createOverlayView(windowToken, frame);
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in createOverlayView", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in createOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void relayoutOverlayView(IBinder sessionToken, Rect frame, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "relayoutOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).relayoutOverlayView(frame);
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in relayoutOverlayView", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in relayoutOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void removeOverlayView(IBinder sessionToken, int userId) {
            Exception e;
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "removeOverlayView");
            long identity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(sessionToken, callingUid, resolvedUserId).removeOverlayView();
                    } catch (RemoteException e2) {
                        e = e2;
                        Slog.e(TvInputManagerService.TAG, "error in removeOverlayView", e);
                    } catch (SessionNotFoundException e3) {
                        e = e3;
                        Slog.e(TvInputManagerService.TAG, "error in removeOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public List<TvInputHardwareInfo> getHardwareList() throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.getHardwareList();
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public ITvInputHardware acquireTvInputHardware(int deviceId, ITvInputHardwareCallback callback, TvInputInfo info, int userId) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "acquireTvInputHardware");
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.acquireHardware(deviceId, callback, info, callingUid, resolvedUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void releaseTvInputHardware(int deviceId, ITvInputHardware hardware, int userId) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") == 0) {
                long identity = Binder.clearCallingIdentity();
                int callingUid = Binder.getCallingUid();
                int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "releaseTvInputHardware");
                try {
                    TvInputManagerService.this.mTvInputHardwareManager.releaseHardware(deviceId, hardware, callingUid, resolvedUserId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public List<TvStreamConfig> getAvailableTvStreamConfigList(String inputId, int userId) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_TV_INPUT") != 0) {
                throw new SecurityException("Requires CAPTURE_TV_INPUT permission");
            }
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "getAvailableTvStreamConfigList");
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.getAvailableTvStreamConfigList(inputId, callingUid, resolvedUserId);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean captureFrame(String inputId, Surface surface, TvStreamConfig config, int userId) throws RemoteException {
            boolean zCaptureFrame;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_TV_INPUT") != 0) {
                throw new SecurityException("Requires CAPTURE_TV_INPUT permission");
            }
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "captureFrame");
            String hardwareInputId = null;
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    if (userState.inputMap.get(inputId) == null) {
                        Slog.e(TvInputManagerService.TAG, "input not found for " + inputId);
                        zCaptureFrame = TvInputManagerService.DEBUG;
                    } else {
                        Iterator i$ = userState.sessionStateMap.values().iterator();
                        while (true) {
                            if (!i$.hasNext()) {
                                break;
                            }
                            SessionState sessionState = (SessionState) i$.next();
                            if (sessionState.info.getId().equals(inputId) && sessionState.hardwareSessionToken != null) {
                                hardwareInputId = ((SessionState) userState.sessionStateMap.get(sessionState.hardwareSessionToken)).info.getId();
                                break;
                            }
                        }
                        zCaptureFrame = TvInputManagerService.this.mTvInputHardwareManager.captureFrame(hardwareInputId != null ? hardwareInputId : inputId, surface, config, callingUid, resolvedUserId);
                    }
                }
                return zCaptureFrame;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean isSingleSessionActive(int userId) throws RemoteException {
            long identity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int resolvedUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, userId, "isSingleSessionActive");
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(resolvedUserId);
                    if (userState.sessionStateMap.size() == 1) {
                        return true;
                    }
                    if (userState.sessionStateMap.size() == 2) {
                        SessionState[] sessionStates = (SessionState[]) userState.sessionStateMap.values().toArray(new SessionState[0]);
                        if (sessionStates[0].hardwareSessionToken != null || sessionStates[1].hardwareSessionToken != null) {
                            return true;
                        }
                    }
                    return TvInputManagerService.DEBUG;
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            if (TvInputManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") == 0) {
                synchronized (TvInputManagerService.this.mLock) {
                    pw.println("User Ids (Current user: " + TvInputManagerService.this.mCurrentUserId + "):");
                    pw.increaseIndent();
                    for (int i = 0; i < TvInputManagerService.this.mUserStates.size(); i++) {
                        pw.println(Integer.valueOf(TvInputManagerService.this.mUserStates.keyAt(i)));
                    }
                    pw.decreaseIndent();
                    for (int i2 = 0; i2 < TvInputManagerService.this.mUserStates.size(); i2++) {
                        int userId = TvInputManagerService.this.mUserStates.keyAt(i2);
                        UserState userState = TvInputManagerService.this.getUserStateLocked(userId);
                        pw.println("UserState (" + userId + "):");
                        pw.increaseIndent();
                        pw.println("inputMap: inputId -> TvInputState");
                        pw.increaseIndent();
                        for (Map.Entry<String, TvInputState> entry : userState.inputMap.entrySet()) {
                            pw.println(entry.getKey() + ": " + entry.getValue());
                        }
                        pw.decreaseIndent();
                        pw.println("packageSet:");
                        pw.increaseIndent();
                        for (String packageName : userState.packageSet) {
                            pw.println(packageName);
                        }
                        pw.decreaseIndent();
                        pw.println("clientStateMap: ITvInputClient -> ClientState");
                        pw.increaseIndent();
                        for (Map.Entry<IBinder, ClientState> entry2 : userState.clientStateMap.entrySet()) {
                            ClientState client = entry2.getValue();
                            pw.println(entry2.getKey() + ": " + client);
                            pw.increaseIndent();
                            pw.println("sessionTokens:");
                            pw.increaseIndent();
                            for (IBinder token : client.sessionTokens) {
                                pw.println("" + token);
                            }
                            pw.decreaseIndent();
                            pw.println("clientTokens: " + client.clientToken);
                            pw.println("userId: " + client.userId);
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("serviceStateMap: ComponentName -> ServiceState");
                        pw.increaseIndent();
                        for (Map.Entry<ComponentName, ServiceState> entry3 : userState.serviceStateMap.entrySet()) {
                            ServiceState service = entry3.getValue();
                            pw.println(entry3.getKey() + ": " + service);
                            pw.increaseIndent();
                            pw.println("sessionTokens:");
                            pw.increaseIndent();
                            for (IBinder token2 : service.sessionTokens) {
                                pw.println("" + token2);
                            }
                            pw.decreaseIndent();
                            pw.println("service: " + service.service);
                            pw.println("callback: " + service.callback);
                            pw.println("bound: " + service.bound);
                            pw.println("reconnecting: " + service.reconnecting);
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("sessionStateMap: ITvInputSession -> SessionState");
                        pw.increaseIndent();
                        for (Map.Entry<IBinder, SessionState> entry4 : userState.sessionStateMap.entrySet()) {
                            SessionState session = entry4.getValue();
                            pw.println(entry4.getKey() + ": " + session);
                            pw.increaseIndent();
                            pw.println("info: " + session.info);
                            pw.println("client: " + session.client);
                            pw.println("seq: " + session.seq);
                            pw.println("callingUid: " + session.callingUid);
                            pw.println("userId: " + session.userId);
                            pw.println("sessionToken: " + session.sessionToken);
                            pw.println("session: " + session.session);
                            pw.println("logUri: " + session.logUri);
                            pw.println("hardwareSessionToken: " + session.hardwareSessionToken);
                            pw.decreaseIndent();
                        }
                        pw.decreaseIndent();
                        pw.println("callbackSet:");
                        pw.increaseIndent();
                        for (ITvInputManagerCallback callback : userState.callbackSet) {
                            pw.println(callback.toString());
                        }
                        pw.decreaseIndent();
                        pw.println("mainSessionToken: " + userState.mainSessionToken);
                        pw.decreaseIndent();
                    }
                }
                return;
            }
            pw.println("Permission Denial: can't dump TvInputManager from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        }
    }

    private static final class UserState {
        private final Set<ITvInputManagerCallback> callbackSet;
        private final Map<IBinder, ClientState> clientStateMap;
        private final List<TvContentRatingSystemInfo> contentRatingSystemList;
        private Map<String, TvInputState> inputMap;
        private IBinder mainSessionToken;
        private final Set<String> packageSet;
        private final PersistentDataStore persistentDataStore;
        private final Map<ComponentName, ServiceState> serviceStateMap;
        private final Map<IBinder, SessionState> sessionStateMap;

        private UserState(Context context, int userId) {
            this.inputMap = new HashMap();
            this.packageSet = new HashSet();
            this.contentRatingSystemList = new ArrayList();
            this.clientStateMap = new HashMap();
            this.serviceStateMap = new HashMap();
            this.sessionStateMap = new HashMap();
            this.callbackSet = new HashSet();
            this.mainSessionToken = null;
            this.persistentDataStore = new PersistentDataStore(context, userId);
        }
    }

    private final class ClientState implements IBinder.DeathRecipient {
        private IBinder clientToken;
        private final List<IBinder> sessionTokens = new ArrayList();
        private final int userId;

        ClientState(IBinder clientToken, int userId) {
            this.clientToken = clientToken;
            this.userId = userId;
        }

        public boolean isEmpty() {
            return this.sessionTokens.isEmpty();
        }

        @Override
        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getUserStateLocked(this.userId);
                ClientState clientState = (ClientState) userState.clientStateMap.get(this.clientToken);
                if (clientState != null) {
                    while (clientState.sessionTokens.size() > 0) {
                        TvInputManagerService.this.releaseSessionLocked(clientState.sessionTokens.get(0), 1000, this.userId);
                    }
                }
                this.clientToken = null;
            }
        }
    }

    private final class ServiceState {
        private boolean bound;
        private ServiceCallback callback;
        private final ComponentName component;
        private final ServiceConnection connection;
        private final List<TvInputInfo> inputList;
        private final boolean isHardware;
        private boolean reconnecting;
        private ITvInputService service;
        private final List<IBinder> sessionTokens;

        private ServiceState(ComponentName component, int userId) {
            this.sessionTokens = new ArrayList();
            this.inputList = new ArrayList();
            this.component = component;
            this.connection = new InputServiceConnection(component, userId);
            this.isHardware = TvInputManagerService.hasHardwarePermission(TvInputManagerService.this.mContext.getPackageManager(), component);
        }
    }

    private static final class TvInputState {
        private TvInputInfo info;
        private int state;

        private TvInputState() {
            this.state = 0;
        }

        public String toString() {
            return "info: " + this.info + "; state: " + this.state;
        }
    }

    private final class SessionState implements IBinder.DeathRecipient {
        private final int callingUid;
        private final ITvInputClient client;
        private IBinder hardwareSessionToken;
        private final TvInputInfo info;
        private Uri logUri;
        private final int seq;
        private ITvInputSession session;
        private final IBinder sessionToken;
        private final int userId;

        private SessionState(IBinder sessionToken, TvInputInfo info, ITvInputClient client, int seq, int callingUid, int userId) {
            this.sessionToken = sessionToken;
            this.info = info;
            this.client = client;
            this.seq = seq;
            this.callingUid = callingUid;
            this.userId = userId;
        }

        @Override
        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                this.session = null;
                if (this.client == null) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(this.userId);
                    while (i$.hasNext()) {
                    }
                    TvInputManagerService.this.removeSessionStateLocked(this.sessionToken, this.userId);
                } else {
                    try {
                        this.client.onSessionReleased(this.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onSessionReleased", e);
                    }
                    UserState userState2 = TvInputManagerService.this.getUserStateLocked(this.userId);
                    for (SessionState sessionState : userState2.sessionStateMap.values()) {
                        if (this.sessionToken == sessionState.hardwareSessionToken) {
                            TvInputManagerService.this.releaseSessionLocked(sessionState.sessionToken, 1000, this.userId);
                            try {
                                sessionState.client.onSessionReleased(sessionState.seq);
                            } catch (RemoteException e2) {
                                Slog.e(TvInputManagerService.TAG, "error in onSessionReleased", e2);
                            }
                        }
                    }
                    TvInputManagerService.this.removeSessionStateLocked(this.sessionToken, this.userId);
                }
            }
        }
    }

    private final class InputServiceConnection implements ServiceConnection {
        private final ComponentName mComponent;
        private final int mUserId;

        private InputServiceConnection(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getUserStateLocked(this.mUserId);
                ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(this.mComponent);
                serviceState.service = ITvInputService.Stub.asInterface(service);
                if (!serviceState.isHardware || serviceState.callback != null) {
                    for (IBinder sessionToken : serviceState.sessionTokens) {
                        TvInputManagerService.this.createSessionInternalLocked(serviceState.service, sessionToken, this.mUserId);
                    }
                    for (TvInputState inputState : userState.inputMap.values()) {
                        if (inputState.info.getComponent().equals(component) && inputState.state != 2) {
                            TvInputManagerService.this.notifyInputStateChangedLocked(userState, inputState.info.getId(), inputState.state, null);
                        }
                    }
                    if (!serviceState.isHardware) {
                        List<TvInputHardwareInfo> hardwareInfoList = TvInputManagerService.this.mTvInputHardwareManager.getHardwareList();
                        for (TvInputHardwareInfo hardwareInfo : hardwareInfoList) {
                            try {
                                serviceState.service.notifyHardwareAdded(hardwareInfo);
                            } catch (RemoteException e) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e);
                            }
                        }
                        List<HdmiDeviceInfo> deviceInfoList = TvInputManagerService.this.mTvInputHardwareManager.getHdmiDeviceList();
                        for (HdmiDeviceInfo deviceInfo : deviceInfoList) {
                            try {
                                serviceState.service.notifyHdmiDeviceAdded(deviceInfo);
                            } catch (RemoteException e2) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e2);
                            }
                        }
                    }
                } else {
                    serviceState.callback = TvInputManagerService.this.new ServiceCallback(this.mComponent, this.mUserId);
                    try {
                        serviceState.service.registerCallback(serviceState.callback);
                    } catch (RemoteException e3) {
                        Slog.e(TvInputManagerService.TAG, "error in registerCallback", e3);
                    }
                    while (i$.hasNext()) {
                    }
                    while (i$.hasNext()) {
                    }
                    if (!serviceState.isHardware) {
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (this.mComponent.equals(component)) {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState userState = TvInputManagerService.this.getUserStateLocked(this.mUserId);
                    ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(this.mComponent);
                    if (serviceState != null) {
                        serviceState.reconnecting = true;
                        serviceState.bound = TvInputManagerService.DEBUG;
                        serviceState.service = null;
                        serviceState.callback = null;
                        TvInputManagerService.this.abortPendingCreateSessionRequestsLocked(serviceState, null, this.mUserId);
                        for (TvInputState inputState : userState.inputMap.values()) {
                            if (inputState.info.getComponent().equals(component)) {
                                TvInputManagerService.this.notifyInputStateChangedLocked(userState, inputState.info.getId(), 2, null);
                            }
                        }
                    }
                }
                return;
            }
            throw new IllegalArgumentException("Mismatched ComponentName: " + this.mComponent + " (expected), " + component + " (actual).");
        }
    }

    private final class ServiceCallback extends ITvInputServiceCallback.Stub {
        private final ComponentName mComponent;
        private final int mUserId;

        ServiceCallback(ComponentName component, int userId) {
            this.mComponent = component;
            this.mUserId = userId;
        }

        private void ensureHardwarePermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                throw new SecurityException("The caller does not have hardware permission");
            }
        }

        private void ensureValidInput(TvInputInfo inputInfo) {
            if (inputInfo.getId() == null || !this.mComponent.equals(inputInfo.getComponent())) {
                throw new IllegalArgumentException("Invalid TvInputInfo");
            }
        }

        private void addTvInputLocked(TvInputInfo inputInfo) {
            ServiceState serviceState = TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId);
            serviceState.inputList.add(inputInfo);
            TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
        }

        public void addHardwareTvInput(int deviceId, TvInputInfo inputInfo) {
            ensureHardwarePermission();
            ensureValidInput(inputInfo);
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.mTvInputHardwareManager.addHardwareTvInput(deviceId, inputInfo);
                addTvInputLocked(inputInfo);
            }
        }

        public void addHdmiTvInput(int id, TvInputInfo inputInfo) {
            ensureHardwarePermission();
            ensureValidInput(inputInfo);
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.mTvInputHardwareManager.addHdmiTvInput(id, inputInfo);
                addTvInputLocked(inputInfo);
            }
        }

        public void removeTvInput(String inputId) {
            ensureHardwarePermission();
            synchronized (TvInputManagerService.this.mLock) {
                ServiceState serviceState = TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId);
                boolean removed = TvInputManagerService.DEBUG;
                Iterator<TvInputInfo> it = serviceState.inputList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    if (it.next().getId().equals(inputId)) {
                        it.remove();
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
                    TvInputManagerService.this.mTvInputHardwareManager.removeTvInput(inputId);
                } else {
                    Slog.e(TvInputManagerService.TAG, "failed to remove input " + inputId);
                }
            }
        }
    }

    private final class SessionCallback extends ITvInputSessionCallback.Stub {
        private final InputChannel[] mChannels;
        private final SessionState mSessionState;

        SessionCallback(SessionState sessionState, InputChannel[] channels) {
            this.mSessionState = sessionState;
            this.mChannels = channels;
        }

        public void onSessionCreated(ITvInputSession session, IBinder harewareSessionToken) {
            synchronized (TvInputManagerService.this.mLock) {
                this.mSessionState.session = session;
                this.mSessionState.hardwareSessionToken = harewareSessionToken;
                if (session == null || !addSessionTokenToClientStateLocked(session)) {
                    TvInputManagerService.this.removeSessionStateLocked(this.mSessionState.sessionToken, this.mSessionState.userId);
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.info.getId(), null, null, this.mSessionState.seq);
                } else {
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.info.getId(), this.mSessionState.sessionToken, this.mChannels[0], this.mSessionState.seq);
                }
                this.mChannels[0].dispose();
            }
        }

        private boolean addSessionTokenToClientStateLocked(ITvInputSession session) {
            try {
                session.asBinder().linkToDeath(this.mSessionState, 0);
                IBinder clientToken = this.mSessionState.client.asBinder();
                UserState userState = TvInputManagerService.this.getUserStateLocked(this.mSessionState.userId);
                ClientState clientState = (ClientState) userState.clientStateMap.get(clientToken);
                if (clientState == null) {
                    clientState = TvInputManagerService.this.new ClientState(clientToken, this.mSessionState.userId);
                    try {
                        clientToken.linkToDeath(clientState, 0);
                        userState.clientStateMap.put(clientToken, clientState);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died", e);
                        return TvInputManagerService.DEBUG;
                    }
                }
                clientState.sessionTokens.add(this.mSessionState.sessionToken);
                return true;
            } catch (RemoteException e2) {
                Slog.e(TvInputManagerService.TAG, "session process has already died", e2);
                return TvInputManagerService.DEBUG;
            }
        }

        public void onChannelRetuned(Uri channelUri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onChannelRetuned(channelUri, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onChannelRetuned", e);
                    }
                }
            }
        }

        public void onTracksChanged(List<TvTrackInfo> tracks) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onTracksChanged(tracks, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTracksChanged", e);
                    }
                }
            }
        }

        public void onTrackSelected(int type, String trackId) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onTrackSelected(type, trackId, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onTrackSelected", e);
                    }
                }
            }
        }

        public void onVideoAvailable() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onVideoAvailable(this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onVideoAvailable", e);
                    }
                }
            }
        }

        public void onVideoUnavailable(int reason) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onVideoUnavailable(reason, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onVideoUnavailable", e);
                    }
                }
            }
        }

        public void onContentAllowed() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onContentAllowed(this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onContentAllowed", e);
                    }
                }
            }
        }

        public void onContentBlocked(String rating) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onContentBlocked(rating, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onContentBlocked", e);
                    }
                }
            }
        }

        public void onLayoutSurface(int left, int top, int right, int bottom) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onLayoutSurface(left, top, right, bottom, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onLayoutSurface", e);
                    }
                }
            }
        }

        public void onSessionEvent(String eventType, Bundle eventArgs) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session != null && this.mSessionState.client != null) {
                    try {
                        this.mSessionState.client.onSessionEvent(eventType, eventArgs, this.mSessionState.seq);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "error in onSessionEvent", e);
                    }
                }
            }
        }
    }

    private static final class WatchLogHandler extends Handler {
        private static final int MSG_LOG_WATCH_END = 2;
        private static final int MSG_LOG_WATCH_START = 1;
        private final ContentResolver mContentResolver;

        public WatchLogHandler(ContentResolver contentResolver, Looper looper) {
            super(looper);
            this.mContentResolver = contentResolver;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SomeArgs args = (SomeArgs) msg.obj;
                    String packageName = (String) args.arg1;
                    long watchStartTime = ((Long) args.arg2).longValue();
                    long channelId = ((Long) args.arg3).longValue();
                    Bundle tuneParams = (Bundle) args.arg4;
                    IBinder sessionToken = (IBinder) args.arg5;
                    ContentValues values = new ContentValues();
                    values.put("package_name", packageName);
                    values.put("watch_start_time_utc_millis", Long.valueOf(watchStartTime));
                    values.put("channel_id", Long.valueOf(channelId));
                    if (tuneParams != null) {
                        values.put("tune_params", encodeTuneParams(tuneParams));
                    }
                    values.put("session_token", sessionToken.toString());
                    this.mContentResolver.insert(TvContract.WatchedPrograms.CONTENT_URI, values);
                    args.recycle();
                    break;
                case 2:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    IBinder sessionToken2 = (IBinder) args2.arg1;
                    long watchEndTime = ((Long) args2.arg2).longValue();
                    ContentValues values2 = new ContentValues();
                    values2.put("watch_end_time_utc_millis", Long.valueOf(watchEndTime));
                    values2.put("session_token", sessionToken2.toString());
                    this.mContentResolver.insert(TvContract.WatchedPrograms.CONTENT_URI, values2);
                    args2.recycle();
                    break;
                default:
                    Slog.w(TvInputManagerService.TAG, "Unhandled message code: " + msg.what);
                    break;
            }
        }

        private String encodeTuneParams(Bundle tuneParams) {
            StringBuilder builder = new StringBuilder();
            Set<String> keySet = tuneParams.keySet();
            Iterator<String> it = keySet.iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = tuneParams.get(key);
                if (value != null) {
                    builder.append(replaceEscapeCharacters(key));
                    builder.append("=");
                    builder.append(replaceEscapeCharacters(value.toString()));
                    if (it.hasNext()) {
                        builder.append(", ");
                    }
                }
            }
            return builder.toString();
        }

        private String replaceEscapeCharacters(String src) {
            StringBuilder builder = new StringBuilder();
            char[] arr$ = src.toCharArray();
            for (char ch : arr$) {
                if ("%=,".indexOf(ch) >= 0) {
                    builder.append('%');
                }
                builder.append(ch);
            }
            return builder.toString();
        }
    }

    private final class HardwareListener implements TvInputHardwareManager.Listener {
        private HardwareListener() {
        }

        @Override
        public void onStateChanged(String inputId, int state) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.setStateLocked(inputId, state, TvInputManagerService.this.mCurrentUserId);
            }
        }

        @Override
        public void onHardwareDeviceAdded(TvInputHardwareInfo info) {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getUserStateLocked(TvInputManagerService.this.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHardwareAdded(info);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHardwareDeviceRemoved(TvInputHardwareInfo info) {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getUserStateLocked(TvInputManagerService.this.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHardwareRemoved(info);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareRemoved", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHdmiDeviceAdded(HdmiDeviceInfo deviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getUserStateLocked(TvInputManagerService.this.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHdmiDeviceAdded(deviceInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHdmiDeviceRemoved(HdmiDeviceInfo deviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = TvInputManagerService.this.getUserStateLocked(TvInputManagerService.this.mCurrentUserId);
                for (ServiceState serviceState : userState.serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHdmiDeviceRemoved(deviceInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceRemoved", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHdmiDeviceUpdated(String inputId, HdmiDeviceInfo deviceInfo) {
            Integer state;
            synchronized (TvInputManagerService.this.mLock) {
                switch (deviceInfo.getDevicePowerStatus()) {
                    case 0:
                        state = 0;
                        break;
                    case 1:
                    case 2:
                    case 3:
                        state = 1;
                        break;
                    default:
                        state = null;
                        break;
                }
                if (state != null) {
                    TvInputManagerService.this.setStateLocked(inputId, state.intValue(), TvInputManagerService.this.mCurrentUserId);
                }
            }
        }
    }

    private static class SessionNotFoundException extends IllegalArgumentException {
        public SessionNotFoundException() {
        }

        public SessionNotFoundException(String name) {
            super(name);
        }
    }
}
