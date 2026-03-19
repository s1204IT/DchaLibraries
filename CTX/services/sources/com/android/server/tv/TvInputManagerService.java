package com.android.server.tv;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.hardware.hdmi.HdmiDeviceInfo;
import android.media.PlaybackParams;
import android.media.tv.DvbDeviceInfo;
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
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputChannel;
import android.view.Surface;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.IoThread;
import com.android.server.SystemService;
import com.android.server.UiModeManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.tv.TvInputHardwareManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TvInputManagerService extends SystemService {
    private static final boolean DEBUG = false;
    private static final String DVB_DIRECTORY = "/dev/dvb";
    private static final String TAG = "TvInputManagerService";
    private final Context mContext;
    private int mCurrentUserId;
    private final Object mLock;
    private final TvInputHardwareManager mTvInputHardwareManager;
    private final SparseArray<UserState> mUserStates;
    private final WatchLogHandler mWatchLogHandler;
    private static final Pattern sFrontEndDevicePattern = Pattern.compile("^dvb([0-9]+)\\.frontend([0-9]+)$");
    private static final Pattern sAdapterDirPattern = Pattern.compile("^adapter([0-9]+)$");
    private static final Pattern sFrontEndInAdapterDirPattern = Pattern.compile("^frontend([0-9]+)$");

    public TvInputManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mCurrentUserId = 0;
        this.mUserStates = new SparseArray<>();
        this.mContext = context;
        this.mWatchLogHandler = new WatchLogHandler(this.mContext.getContentResolver(), IoThread.get().getLooper());
        this.mTvInputHardwareManager = new TvInputHardwareManager(context, new HardwareListener());
        synchronized (this.mLock) {
            getOrCreateUserStateLocked(this.mCurrentUserId);
        }
    }

    @Override
    public void onStart() {
        publishBinderService("tv_input", new BinderService());
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            registerBroadcastReceivers();
        } else if (i == 600) {
            synchronized (this.mLock) {
                buildTvInputListLocked(this.mCurrentUserId, null);
                buildTvContentRatingSystemListLocked(this.mCurrentUserId);
            }
        }
        this.mTvInputHardwareManager.onBootPhase(i);
    }

    @Override
    public void onUnlockUser(int i) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId != i) {
                return;
            }
            buildTvInputListLocked(this.mCurrentUserId, null);
            buildTvContentRatingSystemListLocked(this.mCurrentUserId);
        }
    }

    private void registerBroadcastReceivers() {
        new PackageMonitor() {
            private void buildTvInputList(String[] strArr) {
                synchronized (TvInputManagerService.this.mLock) {
                    if (TvInputManagerService.this.mCurrentUserId == getChangingUserId()) {
                        TvInputManagerService.this.buildTvInputListLocked(TvInputManagerService.this.mCurrentUserId, strArr);
                        TvInputManagerService.this.buildTvContentRatingSystemListLocked(TvInputManagerService.this.mCurrentUserId);
                    }
                }
            }

            public void onPackageUpdateFinished(String str, int i) {
                buildTvInputList(new String[]{str});
            }

            public void onPackagesAvailable(String[] strArr) {
                if (isReplacing()) {
                    buildTvInputList(strArr);
                }
            }

            public void onPackagesUnavailable(String[] strArr) {
                if (isReplacing()) {
                    buildTvInputList(strArr);
                }
            }

            public void onSomePackagesChanged() {
                if (isReplacing()) {
                    return;
                }
                buildTvInputList(null);
            }

            public boolean onPackageChanged(String str, int i, String[] strArr) {
                return true;
            }
        }.register(this.mContext, (Looper) null, UserHandle.ALL, true);
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

    private static boolean hasHardwarePermission(PackageManager packageManager, ComponentName componentName) {
        return packageManager.checkPermission("android.permission.TV_INPUT_HARDWARE", componentName.getPackageName()) == 0;
    }

    private void buildTvInputListLocked(int i, String[] strArr) {
        UserState orCreateUserStateLocked = getOrCreateUserStateLocked(i);
        orCreateUserStateLocked.packageSet.clear();
        PackageManager packageManager = this.mContext.getPackageManager();
        List listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(new Intent("android.media.tv.TvInputService"), 132, i);
        ArrayList<TvInputInfo> arrayList = new ArrayList();
        Iterator it = listQueryIntentServicesAsUser.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ResolveInfo resolveInfo = (ResolveInfo) it.next();
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            if (!"android.permission.BIND_TV_INPUT".equals(serviceInfo.permission)) {
                Slog.w(TAG, "Skipping TV input " + serviceInfo.name + ": it does not require the permission android.permission.BIND_TV_INPUT");
            } else {
                ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                if (!hasHardwarePermission(packageManager, componentName)) {
                    try {
                        arrayList.add(new TvInputInfo.Builder(this.mContext, resolveInfo).build());
                    } catch (Exception e) {
                        Slog.e(TAG, "failed to load TV input " + serviceInfo.name, e);
                    }
                } else {
                    ServiceState serviceState = (ServiceState) orCreateUserStateLocked.serviceStateMap.get(componentName);
                    if (serviceState == null) {
                        orCreateUserStateLocked.serviceStateMap.put(componentName, new ServiceState(componentName, i));
                        updateServiceConnectionLocked(componentName, i);
                    } else {
                        arrayList.addAll(serviceState.hardwareInputMap.values());
                    }
                }
                orCreateUserStateLocked.packageSet.add(serviceInfo.packageName);
            }
        }
        HashMap map = new HashMap();
        for (TvInputInfo tvInputInfo : arrayList) {
            TvInputState tvInputState = (TvInputState) orCreateUserStateLocked.inputMap.get(tvInputInfo.getId());
            if (tvInputState == null) {
                tvInputState = new TvInputState();
            }
            tvInputState.info = tvInputInfo;
            map.put(tvInputInfo.getId(), tvInputState);
        }
        for (String str : map.keySet()) {
            if (!orCreateUserStateLocked.inputMap.containsKey(str)) {
                notifyInputAddedLocked(orCreateUserStateLocked, str);
            } else if (strArr != null) {
                ComponentName component = ((TvInputState) map.get(str)).info.getComponent();
                int length = strArr.length;
                int i2 = 0;
                while (true) {
                    if (i2 < length) {
                        if (!component.getPackageName().equals(strArr[i2])) {
                            i2++;
                        } else {
                            updateServiceConnectionLocked(component, i);
                            notifyInputUpdatedLocked(orCreateUserStateLocked, str);
                            break;
                        }
                    }
                }
            }
        }
        for (String str2 : orCreateUserStateLocked.inputMap.keySet()) {
            if (!map.containsKey(str2)) {
                ServiceState serviceState2 = (ServiceState) orCreateUserStateLocked.serviceStateMap.get(((TvInputState) orCreateUserStateLocked.inputMap.get(str2)).info.getComponent());
                if (serviceState2 != null) {
                    abortPendingCreateSessionRequestsLocked(serviceState2, str2, i);
                }
                notifyInputRemovedLocked(orCreateUserStateLocked, str2);
            }
        }
        orCreateUserStateLocked.inputMap.clear();
        orCreateUserStateLocked.inputMap = map;
    }

    private void buildTvContentRatingSystemListLocked(int i) {
        UserState orCreateUserStateLocked = getOrCreateUserStateLocked(i);
        orCreateUserStateLocked.contentRatingSystemList.clear();
        Iterator<ResolveInfo> it = this.mContext.getPackageManager().queryBroadcastReceivers(new Intent("android.media.tv.action.QUERY_CONTENT_RATING_SYSTEMS"), 128).iterator();
        while (it.hasNext()) {
            ActivityInfo activityInfo = it.next().activityInfo;
            Bundle bundle = activityInfo.metaData;
            if (bundle != null) {
                int i2 = bundle.getInt("android.media.tv.metadata.CONTENT_RATING_SYSTEMS");
                if (i2 == 0) {
                    Slog.w(TAG, "Missing meta-data 'android.media.tv.metadata.CONTENT_RATING_SYSTEMS' on receiver " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name);
                } else {
                    orCreateUserStateLocked.contentRatingSystemList.add(TvContentRatingSystemInfo.createTvContentRatingSystemInfo(i2, activityInfo.applicationInfo));
                }
            }
        }
    }

    private void switchUser(int i) {
        synchronized (this.mLock) {
            if (this.mCurrentUserId == i) {
                return;
            }
            UserState userState = this.mUserStates.get(this.mCurrentUserId);
            ArrayList<SessionState> arrayList = new ArrayList();
            for (SessionState sessionState : userState.sessionStateMap.values()) {
                if (sessionState.session != null && !sessionState.isRecordingSession) {
                    arrayList.add(sessionState);
                }
            }
            for (SessionState sessionState2 : arrayList) {
                try {
                    sessionState2.session.release();
                } catch (RemoteException e) {
                    Slog.e(TAG, "error in release", e);
                }
                clearSessionAndNotifyClientLocked(sessionState2);
            }
            Iterator it = userState.serviceStateMap.keySet().iterator();
            while (it.hasNext()) {
                ServiceState serviceState = (ServiceState) userState.serviceStateMap.get((ComponentName) it.next());
                if (serviceState != null && serviceState.sessionTokens.isEmpty()) {
                    if (serviceState.callback != null) {
                        try {
                            serviceState.service.unregisterCallback(serviceState.callback);
                        } catch (RemoteException e2) {
                            Slog.e(TAG, "error in unregisterCallback", e2);
                        }
                    }
                    this.mContext.unbindService(serviceState.connection);
                    it.remove();
                }
            }
            this.mCurrentUserId = i;
            getOrCreateUserStateLocked(i);
            buildTvInputListLocked(i, null);
            buildTvContentRatingSystemListLocked(i);
            this.mWatchLogHandler.obtainMessage(3, getContentResolverForUser(i)).sendToTarget();
        }
    }

    private void clearSessionAndNotifyClientLocked(SessionState sessionState) throws Throwable {
        if (sessionState.client != null) {
            try {
                sessionState.client.onSessionReleased(sessionState.seq);
            } catch (RemoteException e) {
                Slog.e(TAG, "error in onSessionReleased", e);
            }
        }
        for (SessionState sessionState2 : getOrCreateUserStateLocked(sessionState.userId).sessionStateMap.values()) {
            if (sessionState.sessionToken == sessionState2.hardwareSessionToken) {
                releaseSessionLocked(sessionState2.sessionToken, 1000, sessionState.userId);
                try {
                    sessionState2.client.onSessionReleased(sessionState2.seq);
                } catch (RemoteException e2) {
                    Slog.e(TAG, "error in onSessionReleased", e2);
                }
            }
        }
        removeSessionStateLocked(sessionState.sessionToken, sessionState.userId);
    }

    private void removeUser(int i) {
        synchronized (this.mLock) {
            UserState userState = this.mUserStates.get(i);
            if (userState == null) {
                return;
            }
            for (SessionState sessionState : userState.sessionStateMap.values()) {
                if (sessionState.session != null) {
                    try {
                        sessionState.session.release();
                    } catch (RemoteException e) {
                        Slog.e(TAG, "error in release", e);
                    }
                }
            }
            userState.sessionStateMap.clear();
            for (ServiceState serviceState : userState.serviceStateMap.values()) {
                if (serviceState.service != null) {
                    if (serviceState.callback != null) {
                        try {
                            serviceState.service.unregisterCallback(serviceState.callback);
                        } catch (RemoteException e2) {
                            Slog.e(TAG, "error in unregisterCallback", e2);
                        }
                    }
                    this.mContext.unbindService(serviceState.connection);
                }
            }
            userState.serviceStateMap.clear();
            userState.inputMap.clear();
            userState.packageSet.clear();
            userState.contentRatingSystemList.clear();
            userState.clientStateMap.clear();
            userState.callbackSet.clear();
            userState.mainSessionToken = null;
            this.mUserStates.remove(i);
        }
    }

    private ContentResolver getContentResolverForUser(int i) {
        Context contextCreatePackageContextAsUser;
        UserHandle userHandle = new UserHandle(i);
        try {
            contextCreatePackageContextAsUser = this.mContext.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, userHandle);
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "failed to create package context as user " + userHandle);
            contextCreatePackageContextAsUser = this.mContext;
        }
        return contextCreatePackageContextAsUser.getContentResolver();
    }

    private UserState getOrCreateUserStateLocked(int i) {
        UserState userState = this.mUserStates.get(i);
        if (userState == null) {
            UserState userState2 = new UserState(this.mContext, i);
            this.mUserStates.put(i, userState2);
            return userState2;
        }
        return userState;
    }

    private ServiceState getServiceStateLocked(ComponentName componentName, int i) {
        ServiceState serviceState = (ServiceState) getOrCreateUserStateLocked(i).serviceStateMap.get(componentName);
        if (serviceState == null) {
            throw new IllegalStateException("Service state not found for " + componentName + " (userId=" + i + ")");
        }
        return serviceState;
    }

    private SessionState getSessionStateLocked(IBinder iBinder, int i, int i2) {
        SessionState sessionState = (SessionState) getOrCreateUserStateLocked(i2).sessionStateMap.get(iBinder);
        if (sessionState == null) {
            throw new SessionNotFoundException("Session state not found for token " + iBinder);
        }
        if (i != 1000 && i != sessionState.callingUid) {
            throw new SecurityException("Illegal access to the session with token " + iBinder + " from uid " + i);
        }
        return sessionState;
    }

    private ITvInputSession getSessionLocked(IBinder iBinder, int i, int i2) {
        return getSessionLocked(getSessionStateLocked(iBinder, i, i2));
    }

    private ITvInputSession getSessionLocked(SessionState sessionState) {
        ITvInputSession iTvInputSession = sessionState.session;
        if (iTvInputSession != null) {
            return iTvInputSession;
        }
        throw new IllegalStateException("Session not yet created for token " + sessionState.sessionToken);
    }

    private int resolveCallingUserId(int i, int i2, int i3, String str) {
        return ActivityManager.handleIncomingUser(i, i2, i3, false, false, str, null);
    }

    private void updateServiceConnectionLocked(ComponentName componentName, int i) {
        UserState orCreateUserStateLocked = getOrCreateUserStateLocked(i);
        ServiceState serviceState = (ServiceState) orCreateUserStateLocked.serviceStateMap.get(componentName);
        if (serviceState == null) {
            return;
        }
        boolean z = false;
        if (serviceState.reconnecting) {
            if (!serviceState.sessionTokens.isEmpty()) {
                return;
            } else {
                serviceState.reconnecting = false;
            }
        }
        if (i != this.mCurrentUserId) {
            z = !serviceState.sessionTokens.isEmpty();
        } else if (!serviceState.sessionTokens.isEmpty() || serviceState.isHardware) {
            z = true;
        }
        if (serviceState.service != null || !z) {
            if (serviceState.service == null || z) {
                return;
            }
            this.mContext.unbindService(serviceState.connection);
            orCreateUserStateLocked.serviceStateMap.remove(componentName);
            return;
        }
        if (serviceState.bound) {
            return;
        }
        serviceState.bound = this.mContext.bindServiceAsUser(new Intent("android.media.tv.TvInputService").setComponent(componentName), serviceState.connection, 33554433, new UserHandle(i));
    }

    private void abortPendingCreateSessionRequestsLocked(ServiceState serviceState, String str, int i) {
        UserState orCreateUserStateLocked = getOrCreateUserStateLocked(i);
        ArrayList<SessionState> arrayList = new ArrayList();
        Iterator it = serviceState.sessionTokens.iterator();
        while (it.hasNext()) {
            SessionState sessionState = (SessionState) orCreateUserStateLocked.sessionStateMap.get((IBinder) it.next());
            if (sessionState.session == null && (str == null || sessionState.inputId.equals(str))) {
                arrayList.add(sessionState);
            }
        }
        for (SessionState sessionState2 : arrayList) {
            removeSessionStateLocked(sessionState2.sessionToken, sessionState2.userId);
            sendSessionTokenToClientLocked(sessionState2.client, sessionState2.inputId, null, null, sessionState2.seq);
        }
        updateServiceConnectionLocked(serviceState.component, i);
    }

    private void createSessionInternalLocked(ITvInputService iTvInputService, IBinder iBinder, int i) {
        SessionState sessionState = (SessionState) getOrCreateUserStateLocked(i).sessionStateMap.get(iBinder);
        InputChannel[] inputChannelArrOpenInputChannelPair = InputChannel.openInputChannelPair(iBinder.toString());
        SessionCallback sessionCallback = new SessionCallback(sessionState, inputChannelArrOpenInputChannelPair);
        try {
            if (sessionState.isRecordingSession) {
                iTvInputService.createRecordingSession(sessionCallback, sessionState.inputId);
            } else {
                iTvInputService.createSession(inputChannelArrOpenInputChannelPair[1], sessionCallback, sessionState.inputId);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "error in createSession", e);
            removeSessionStateLocked(iBinder, i);
            sendSessionTokenToClientLocked(sessionState.client, sessionState.inputId, null, null, sessionState.seq);
        }
        inputChannelArrOpenInputChannelPair[1].dispose();
    }

    private void sendSessionTokenToClientLocked(ITvInputClient iTvInputClient, String str, IBinder iBinder, InputChannel inputChannel, int i) {
        try {
            iTvInputClient.onSessionCreated(str, iBinder, inputChannel, i);
        } catch (RemoteException e) {
            Slog.e(TAG, "error in onSessionCreated", e);
        }
    }

    private void releaseSessionLocked(IBinder iBinder, int i, int i2) throws Throwable {
        SessionState sessionStateLocked;
        try {
            try {
                sessionStateLocked = getSessionStateLocked(iBinder, i, i2);
                try {
                    if (sessionStateLocked.session != null) {
                        if (iBinder == getOrCreateUserStateLocked(i2).mainSessionToken) {
                            setMainLocked(iBinder, false, i, i2);
                        }
                        sessionStateLocked.session.release();
                    }
                } catch (RemoteException | SessionNotFoundException e) {
                    e = e;
                    Slog.e(TAG, "error in releaseSession", e);
                    if (sessionStateLocked != null) {
                    }
                    removeSessionStateLocked(iBinder, i2);
                }
            } catch (Throwable th) {
                th = th;
                if (0 != 0) {
                    null.session = null;
                }
                throw th;
            }
        } catch (RemoteException | SessionNotFoundException e2) {
            e = e2;
            sessionStateLocked = null;
        } catch (Throwable th2) {
            th = th2;
            if (0 != 0) {
            }
            throw th;
        }
        if (sessionStateLocked != null) {
            sessionStateLocked.session = null;
        }
        removeSessionStateLocked(iBinder, i2);
    }

    private void removeSessionStateLocked(IBinder iBinder, int i) {
        UserState orCreateUserStateLocked = getOrCreateUserStateLocked(i);
        if (iBinder == orCreateUserStateLocked.mainSessionToken) {
            orCreateUserStateLocked.mainSessionToken = null;
        }
        SessionState sessionState = (SessionState) orCreateUserStateLocked.sessionStateMap.remove(iBinder);
        if (sessionState == null) {
            return;
        }
        ClientState clientState = (ClientState) orCreateUserStateLocked.clientStateMap.get(sessionState.client.asBinder());
        if (clientState != null) {
            clientState.sessionTokens.remove(iBinder);
            if (clientState.isEmpty()) {
                orCreateUserStateLocked.clientStateMap.remove(sessionState.client.asBinder());
            }
        }
        ServiceState serviceState = (ServiceState) orCreateUserStateLocked.serviceStateMap.get(sessionState.componentName);
        if (serviceState != null) {
            serviceState.sessionTokens.remove(iBinder);
        }
        updateServiceConnectionLocked(sessionState.componentName, i);
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = iBinder;
        someArgsObtain.arg2 = Long.valueOf(System.currentTimeMillis());
        this.mWatchLogHandler.obtainMessage(2, someArgsObtain).sendToTarget();
    }

    private void setMainLocked(IBinder iBinder, boolean z, int i, int i2) {
        try {
            SessionState sessionStateLocked = getSessionStateLocked(iBinder, i, i2);
            if (sessionStateLocked.hardwareSessionToken != null) {
                sessionStateLocked = getSessionStateLocked(sessionStateLocked.hardwareSessionToken, 1000, i2);
            }
            if (!getServiceStateLocked(sessionStateLocked.componentName, i2).isHardware) {
                return;
            }
            getSessionLocked(sessionStateLocked).setMain(z);
        } catch (RemoteException | SessionNotFoundException e) {
            Slog.e(TAG, "error in setMain", e);
        }
    }

    private void notifyInputAddedLocked(UserState userState, String str) {
        Iterator it = userState.callbackSet.iterator();
        while (it.hasNext()) {
            try {
                ((ITvInputManagerCallback) it.next()).onInputAdded(str);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report added input to callback", e);
            }
        }
    }

    private void notifyInputRemovedLocked(UserState userState, String str) {
        Iterator it = userState.callbackSet.iterator();
        while (it.hasNext()) {
            try {
                ((ITvInputManagerCallback) it.next()).onInputRemoved(str);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report removed input to callback", e);
            }
        }
    }

    private void notifyInputUpdatedLocked(UserState userState, String str) {
        Iterator it = userState.callbackSet.iterator();
        while (it.hasNext()) {
            try {
                ((ITvInputManagerCallback) it.next()).onInputUpdated(str);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated input to callback", e);
            }
        }
    }

    private void notifyInputStateChangedLocked(UserState userState, String str, int i, ITvInputManagerCallback iTvInputManagerCallback) {
        if (iTvInputManagerCallback != null) {
            try {
                iTvInputManagerCallback.onInputStateChanged(str, i);
                return;
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report state change to callback", e);
                return;
            }
        }
        Iterator it = userState.callbackSet.iterator();
        while (it.hasNext()) {
            try {
                ((ITvInputManagerCallback) it.next()).onInputStateChanged(str, i);
            } catch (RemoteException e2) {
                Slog.e(TAG, "failed to report state change to callback", e2);
            }
        }
    }

    private void updateTvInputInfoLocked(UserState userState, TvInputInfo tvInputInfo) {
        String id = tvInputInfo.getId();
        TvInputState tvInputState = (TvInputState) userState.inputMap.get(id);
        if (tvInputState == null) {
            Slog.e(TAG, "failed to set input info - unknown input id " + id);
            return;
        }
        tvInputState.info = tvInputInfo;
        Iterator it = userState.callbackSet.iterator();
        while (it.hasNext()) {
            try {
                ((ITvInputManagerCallback) it.next()).onTvInputInfoUpdated(tvInputInfo);
            } catch (RemoteException e) {
                Slog.e(TAG, "failed to report updated input info to callback", e);
            }
        }
    }

    private void setStateLocked(String str, int i, int i2) {
        UserState orCreateUserStateLocked = getOrCreateUserStateLocked(i2);
        TvInputState tvInputState = (TvInputState) orCreateUserStateLocked.inputMap.get(str);
        ServiceState serviceState = (ServiceState) orCreateUserStateLocked.serviceStateMap.get(tvInputState.info.getComponent());
        int i3 = tvInputState.state;
        tvInputState.state = i;
        if ((serviceState == null || serviceState.service != null || (serviceState.sessionTokens.isEmpty() && !serviceState.isHardware)) && i3 != i) {
            notifyInputStateChangedLocked(orCreateUserStateLocked, str, i, null);
        }
    }

    private final class BinderService extends ITvInputManager.Stub {
        private BinderService() {
        }

        public List<TvInputInfo> getTvInputList(int i) {
            ArrayList arrayList;
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "getTvInputList");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId);
                    arrayList = new ArrayList();
                    Iterator it = orCreateUserStateLocked.inputMap.values().iterator();
                    while (it.hasNext()) {
                        arrayList.add(((TvInputState) it.next()).info);
                    }
                }
                return arrayList;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public TvInputInfo getTvInputInfo(String str, int i) {
            TvInputInfo tvInputInfo;
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "getTvInputInfo");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputState tvInputState = (TvInputState) TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).inputMap.get(str);
                    tvInputInfo = tvInputState == null ? null : tvInputState.info;
                }
                return tvInputInfo;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void updateTvInputInfo(TvInputInfo tvInputInfo, int i) {
            String str = tvInputInfo.getServiceInfo().packageName;
            String callingPackageName = getCallingPackageName();
            if (TextUtils.equals(str, callingPackageName) || TvInputManagerService.this.mContext.checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
                int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "updateTvInputInfo");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (TvInputManagerService.this.mLock) {
                        TvInputManagerService.this.updateTvInputInfoLocked(TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId), tvInputInfo);
                    }
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new IllegalArgumentException("calling package " + callingPackageName + " is not allowed to change TvInputInfo for " + str);
        }

        private String getCallingPackageName() {
            String[] packagesForUid = TvInputManagerService.this.mContext.getPackageManager().getPackagesForUid(Binder.getCallingUid());
            if (packagesForUid != null && packagesForUid.length > 0) {
                return packagesForUid[0];
            }
            return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }

        public int getTvInputState(String str, int i) {
            int i2;
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "getTvInputState");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputState tvInputState = (TvInputState) TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).inputMap.get(str);
                    i2 = tvInputState == null ? 0 : tvInputState.state;
                }
                return i2;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<TvContentRatingSystemInfo> getTvContentRatingSystemList(int i) {
            List<TvContentRatingSystemInfo> list;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.READ_CONTENT_RATING_SYSTEMS") == 0) {
                int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "getTvContentRatingSystemList");
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    synchronized (TvInputManagerService.this.mLock) {
                        list = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).contentRatingSystemList;
                    }
                    return list;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            throw new SecurityException("The caller does not have permission to read content rating systems");
        }

        public void sendTvInputNotifyIntent(Intent intent, int i) {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.NOTIFY_TV_INPUTS") != 0) {
                throw new SecurityException("The caller: " + getCallingPackageName() + " doesn't have permission: android.permission.NOTIFY_TV_INPUTS");
            }
            if (TextUtils.isEmpty(intent.getPackage())) {
                throw new IllegalArgumentException("Must specify package name to notify.");
            }
            String action = intent.getAction();
            byte b = -1;
            int iHashCode = action.hashCode();
            if (iHashCode != -160295064) {
                if (iHashCode != 1568780589) {
                    if (iHashCode == 2011523553 && action.equals("android.media.tv.action.PREVIEW_PROGRAM_ADDED_TO_WATCH_NEXT")) {
                        b = 2;
                    }
                } else if (action.equals("android.media.tv.action.PREVIEW_PROGRAM_BROWSABLE_DISABLED")) {
                    b = 0;
                }
            } else if (action.equals("android.media.tv.action.WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    if (intent.getLongExtra("android.media.tv.extra.PREVIEW_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid preview program ID.");
                    }
                    break;
                case 1:
                    if (intent.getLongExtra("android.media.tv.extra.WATCH_NEXT_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid watch next program ID.");
                    }
                    break;
                case 2:
                    if (intent.getLongExtra("android.media.tv.extra.PREVIEW_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid preview program ID.");
                    }
                    if (intent.getLongExtra("android.media.tv.extra.WATCH_NEXT_PROGRAM_ID", -1L) < 0) {
                        throw new IllegalArgumentException("Invalid watch next program ID.");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid TV input notifying action: " + intent.getAction());
            }
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "sendTvInputNotifyIntent");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                TvInputManagerService.this.getContext().sendBroadcastAsUser(intent, new UserHandle(iResolveCallingUserId));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void registerCallback(final ITvInputManagerCallback iTvInputManagerCallback, int i) {
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "registerCallback");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    final UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId);
                    orCreateUserStateLocked.callbackSet.add(iTvInputManagerCallback);
                    try {
                        iTvInputManagerCallback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                synchronized (TvInputManagerService.this.mLock) {
                                    if (orCreateUserStateLocked.callbackSet != null) {
                                        orCreateUserStateLocked.callbackSet.remove(iTvInputManagerCallback);
                                    }
                                }
                            }
                        }, 0);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void unregisterCallback(ITvInputManagerCallback iTvInputManagerCallback, int i) {
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "unregisterCallback");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).callbackSet.remove(iTvInputManagerCallback);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isParentalControlsEnabled(int i) {
            boolean zIsParentalControlsEnabled;
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "isParentalControlsEnabled");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    zIsParentalControlsEnabled = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).persistentDataStore.isParentalControlsEnabled();
                }
                return zIsParentalControlsEnabled;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setParentalControlsEnabled(boolean z, int i) {
            ensureParentalControlsPermission();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "setParentalControlsEnabled");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).persistentDataStore.setParentalControlsEnabled(z);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isRatingBlocked(String str, int i) {
            boolean zIsRatingBlocked;
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "isRatingBlocked");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    zIsRatingBlocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).persistentDataStore.isRatingBlocked(TvContentRating.unflattenFromString(str));
                }
                return zIsRatingBlocked;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<String> getBlockedRatings(int i) {
            ArrayList arrayList;
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "getBlockedRatings");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId);
                    arrayList = new ArrayList();
                    for (TvContentRating tvContentRating : orCreateUserStateLocked.persistentDataStore.getBlockedRatings()) {
                        arrayList.add(tvContentRating.flattenToString());
                    }
                }
                return arrayList;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void addBlockedRating(String str, int i) {
            ensureParentalControlsPermission();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "addBlockedRating");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).persistentDataStore.addBlockedRating(TvContentRating.unflattenFromString(str));
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void removeBlockedRating(String str, int i) {
            ensureParentalControlsPermission();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "removeBlockedRating");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).persistentDataStore.removeBlockedRating(TvContentRating.unflattenFromString(str));
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void ensureParentalControlsPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.MODIFY_PARENTAL_CONTROLS") != 0) {
                throw new SecurityException("The caller does not have parental controls permission");
            }
        }

        public void createSession(ITvInputClient iTvInputClient, String str, boolean z, int i, int i2) throws Throwable {
            long j;
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i2, "createSession");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        if (i2 != TvInputManagerService.this.mCurrentUserId && !z) {
                            TvInputManagerService.this.sendSessionTokenToClientLocked(iTvInputClient, str, null, null, i);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return;
                        }
                        UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId);
                        TvInputState tvInputState = (TvInputState) orCreateUserStateLocked.inputMap.get(str);
                        if (tvInputState == null) {
                            Slog.w(TvInputManagerService.TAG, "Failed to find input state for inputId=" + str);
                            TvInputManagerService.this.sendSessionTokenToClientLocked(iTvInputClient, str, null, null, i);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return;
                        }
                        TvInputInfo tvInputInfo = tvInputState.info;
                        ServiceState serviceState = (ServiceState) orCreateUserStateLocked.serviceStateMap.get(tvInputInfo.getComponent());
                        if (serviceState == null) {
                            serviceState = new ServiceState(tvInputInfo.getComponent(), iResolveCallingUserId);
                            orCreateUserStateLocked.serviceStateMap.put(tvInputInfo.getComponent(), serviceState);
                        }
                        ServiceState serviceState2 = serviceState;
                        if (serviceState2.reconnecting) {
                            TvInputManagerService.this.sendSessionTokenToClientLocked(iTvInputClient, str, null, null, i);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return;
                        }
                        Binder binder = new Binder();
                        try {
                            orCreateUserStateLocked.sessionStateMap.put(binder, new SessionState(binder, tvInputInfo.getId(), tvInputInfo.getComponent(), z, iTvInputClient, i, callingUid, iResolveCallingUserId));
                            serviceState2.sessionTokens.add(binder);
                            if (serviceState2.service != null) {
                                TvInputManagerService.this.createSessionInternalLocked(serviceState2.service, binder, iResolveCallingUserId);
                            } else {
                                TvInputManagerService.this.updateServiceConnectionLocked(tvInputInfo.getComponent(), iResolveCallingUserId);
                            }
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        } catch (Throwable th) {
                            th = th;
                            j = jClearCallingIdentity;
                            while (true) {
                                try {
                                    try {
                                        throw th;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        Binder.restoreCallingIdentity(j);
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                }
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        j = jClearCallingIdentity;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
                j = jClearCallingIdentity;
            }
        }

        public void releaseSession(IBinder iBinder, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "releaseSession");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    TvInputManagerService.this.releaseSessionLocked(iBinder, callingUid, iResolveCallingUserId);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setMainSession(IBinder iBinder, int i) {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CHANGE_HDMI_CEC_ACTIVE_SOURCE") != 0) {
                throw new SecurityException("The caller does not have CHANGE_HDMI_CEC_ACTIVE_SOURCE permission");
            }
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "setMainSession");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId);
                    if (orCreateUserStateLocked.mainSessionToken == iBinder) {
                        return;
                    }
                    IBinder iBinder2 = orCreateUserStateLocked.mainSessionToken;
                    orCreateUserStateLocked.mainSessionToken = iBinder;
                    if (iBinder != null) {
                        TvInputManagerService.this.setMainLocked(iBinder, true, callingUid, i);
                    }
                    if (iBinder2 != null) {
                        TvInputManagerService.this.setMainLocked(iBinder2, false, 1000, i);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setSurface(IBinder iBinder, Surface surface, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "setSurface");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        SessionState sessionStateLocked = TvInputManagerService.this.getSessionStateLocked(iBinder, callingUid, iResolveCallingUserId);
                        if (sessionStateLocked.hardwareSessionToken == null) {
                            TvInputManagerService.this.getSessionLocked(sessionStateLocked).setSurface(surface);
                        } else {
                            TvInputManagerService.this.getSessionLocked(sessionStateLocked.hardwareSessionToken, 1000, iResolveCallingUserId).setSurface(surface);
                        }
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in setSurface", e);
                    }
                }
            } finally {
                if (surface != null) {
                    surface.release();
                }
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void dispatchSurfaceChanged(IBinder iBinder, int i, int i2, int i3, int i4) {
            SessionState sessionStateLocked;
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i4, "dispatchSurfaceChanged");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        sessionStateLocked = TvInputManagerService.this.getSessionStateLocked(iBinder, callingUid, iResolveCallingUserId);
                        TvInputManagerService.this.getSessionLocked(sessionStateLocked).dispatchSurfaceChanged(i, i2, i3);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in dispatchSurfaceChanged", e);
                    }
                    if (sessionStateLocked.hardwareSessionToken != null) {
                        TvInputManagerService.this.getSessionLocked(sessionStateLocked.hardwareSessionToken, 1000, iResolveCallingUserId).dispatchSurfaceChanged(i, i2, i3);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setVolume(IBinder iBinder, float f, int i) {
            SessionState sessionStateLocked;
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "setVolume");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        sessionStateLocked = TvInputManagerService.this.getSessionStateLocked(iBinder, callingUid, iResolveCallingUserId);
                        TvInputManagerService.this.getSessionLocked(sessionStateLocked).setVolume(f);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in setVolume", e);
                    }
                    if (sessionStateLocked.hardwareSessionToken != null) {
                        ITvInputSession sessionLocked = TvInputManagerService.this.getSessionLocked(sessionStateLocked.hardwareSessionToken, 1000, iResolveCallingUserId);
                        float f2 = 0.0f;
                        if (f > 0.0f) {
                            f2 = 1.0f;
                        }
                        sessionLocked.setVolume(f2);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void tune(IBinder iBinder, Uri uri, Bundle bundle, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "tune");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).tune(uri, bundle);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in tune", e);
                    }
                    if (TvContract.isChannelUriForPassthroughInput(uri)) {
                        return;
                    }
                    SessionState sessionState = (SessionState) TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId).sessionStateMap.get(iBinder);
                    if (sessionState.isRecordingSession) {
                        return;
                    }
                    SomeArgs someArgsObtain = SomeArgs.obtain();
                    someArgsObtain.arg1 = sessionState.componentName.getPackageName();
                    someArgsObtain.arg2 = Long.valueOf(System.currentTimeMillis());
                    someArgsObtain.arg3 = Long.valueOf(ContentUris.parseId(uri));
                    someArgsObtain.arg4 = bundle;
                    someArgsObtain.arg5 = iBinder;
                    TvInputManagerService.this.mWatchLogHandler.obtainMessage(1, someArgsObtain).sendToTarget();
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void unblockContent(IBinder iBinder, String str, int i) {
            ensureParentalControlsPermission();
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "unblockContent");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).unblockContent(str);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in unblockContent", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setCaptionEnabled(IBinder iBinder, boolean z, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "setCaptionEnabled");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).setCaptionEnabled(z);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in setCaptionEnabled", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void selectTrack(IBinder iBinder, int i, String str, int i2) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i2, "selectTrack");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).selectTrack(i, str);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in selectTrack", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void sendAppPrivateCommand(IBinder iBinder, String str, Bundle bundle, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "sendAppPrivateCommand");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).appPrivateCommand(str, bundle);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in appPrivateCommand", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void createOverlayView(IBinder iBinder, IBinder iBinder2, Rect rect, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "createOverlayView");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).createOverlayView(iBinder2, rect);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in createOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void relayoutOverlayView(IBinder iBinder, Rect rect, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "relayoutOverlayView");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).relayoutOverlayView(rect);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in relayoutOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void removeOverlayView(IBinder iBinder, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "removeOverlayView");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).removeOverlayView();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in removeOverlayView", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void timeShiftPlay(IBinder iBinder, Uri uri, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "timeShiftPlay");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).timeShiftPlay(uri);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftPlay", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void timeShiftPause(IBinder iBinder, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "timeShiftPause");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).timeShiftPause();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftPause", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void timeShiftResume(IBinder iBinder, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "timeShiftResume");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).timeShiftResume();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftResume", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void timeShiftSeekTo(IBinder iBinder, long j, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "timeShiftSeekTo");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).timeShiftSeekTo(j);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftSeekTo", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void timeShiftSetPlaybackParams(IBinder iBinder, PlaybackParams playbackParams, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "timeShiftSetPlaybackParams");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).timeShiftSetPlaybackParams(playbackParams);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftSetPlaybackParams", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void timeShiftEnablePositionTracking(IBinder iBinder, boolean z, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "timeShiftEnablePositionTracking");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).timeShiftEnablePositionTracking(z);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in timeShiftEnablePositionTracking", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void startRecording(IBinder iBinder, Uri uri, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "startRecording");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).startRecording(uri);
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in startRecording", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void stopRecording(IBinder iBinder, int i) {
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "stopRecording");
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    try {
                        TvInputManagerService.this.getSessionLocked(iBinder, callingUid, iResolveCallingUserId).stopRecording();
                    } catch (RemoteException | SessionNotFoundException e) {
                        Slog.e(TvInputManagerService.TAG, "error in stopRecording", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<TvInputHardwareInfo> getHardwareList() throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.getHardwareList();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public ITvInputHardware acquireTvInputHardware(int i, ITvInputHardwareCallback iTvInputHardwareCallback, TvInputInfo tvInputInfo, int i2) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return null;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.acquireHardware(i, iTvInputHardwareCallback, tvInputInfo, callingUid, TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i2, "acquireTvInputHardware"));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void releaseTvInputHardware(int i, ITvInputHardware iTvInputHardware, int i2) throws RemoteException {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            try {
                TvInputManagerService.this.mTvInputHardwareManager.releaseHardware(i, iTvInputHardware, callingUid, TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i2, "releaseTvInputHardware"));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<DvbDeviceInfo> getDvbDeviceList() throws RemoteException {
            List<DvbDeviceInfo> listUnmodifiableList;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.DVB_DEVICE") != 0) {
                throw new SecurityException("Requires DVB_DEVICE permission");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                ArrayList arrayList = new ArrayList();
                boolean z = false;
                for (String str : new File("/dev").list()) {
                    Matcher matcher = TvInputManagerService.sFrontEndDevicePattern.matcher(str);
                    if (matcher.find()) {
                        arrayList.add(new DvbDeviceInfo(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
                    }
                    if (TextUtils.equals("dvb", str)) {
                        z = true;
                    }
                }
                if (!z) {
                    return Collections.unmodifiableList(arrayList);
                }
                File file = new File(TvInputManagerService.DVB_DIRECTORY);
                ArrayList arrayList2 = new ArrayList();
                for (String str2 : file.list()) {
                    Matcher matcher2 = TvInputManagerService.sAdapterDirPattern.matcher(str2);
                    if (matcher2.find()) {
                        int i = Integer.parseInt(matcher2.group(1));
                        String[] list = new File("/dev/dvb/" + str2).list();
                        int length = list.length;
                        for (int i2 = 0; i2 < length; i2++) {
                            Matcher matcher3 = TvInputManagerService.sFrontEndInAdapterDirPattern.matcher(list[i2]);
                            if (matcher3.find()) {
                                arrayList2.add(new DvbDeviceInfo(i, Integer.parseInt(matcher3.group(1))));
                            }
                        }
                    }
                }
                if (arrayList2.isEmpty()) {
                    listUnmodifiableList = Collections.unmodifiableList(arrayList);
                } else {
                    listUnmodifiableList = Collections.unmodifiableList(arrayList2);
                }
                return listUnmodifiableList;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public ParcelFileDescriptor openDvbDevice(DvbDeviceInfo dvbDeviceInfo, int i) throws RemoteException {
            String str;
            int i2;
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.DVB_DEVICE") != 0) {
                throw new SecurityException("Requires DVB_DEVICE permission");
            }
            boolean z = false;
            for (String str2 : new File("/dev").list()) {
                if (TextUtils.equals("dvb", str2)) {
                    boolean z2 = z;
                    for (String str3 : new File(TvInputManagerService.DVB_DIRECTORY).list()) {
                        if (TvInputManagerService.sAdapterDirPattern.matcher(str3).find()) {
                            String[] list = new File("/dev/dvb/" + str3).list();
                            int length = list.length;
                            int i3 = 0;
                            while (true) {
                                if (i3 >= length) {
                                    break;
                                }
                                if (!TvInputManagerService.sFrontEndInAdapterDirPattern.matcher(list[i3]).find()) {
                                    i3++;
                                } else {
                                    z2 = true;
                                    break;
                                }
                            }
                        }
                        if (z2) {
                            break;
                        }
                    }
                    z = z2;
                }
                if (z) {
                    break;
                }
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                switch (i) {
                    case 0:
                        str = String.format(z ? "/dev/dvb/adapter%d/demux%d" : "/dev/dvb%d.demux%d", Integer.valueOf(dvbDeviceInfo.getAdapterId()), Integer.valueOf(dvbDeviceInfo.getDeviceId()));
                        break;
                    case 1:
                        str = String.format(z ? "/dev/dvb/adapter%d/dvr%d" : "/dev/dvb%d.dvr%d", Integer.valueOf(dvbDeviceInfo.getAdapterId()), Integer.valueOf(dvbDeviceInfo.getDeviceId()));
                        break;
                    case 2:
                        str = String.format(z ? "/dev/dvb/adapter%d/frontend%d" : "/dev/dvb%d.frontend%d", Integer.valueOf(dvbDeviceInfo.getAdapterId()), Integer.valueOf(dvbDeviceInfo.getDeviceId()));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid DVB device: " + i);
                }
                try {
                    File file = new File(str);
                    if (2 == i) {
                        i2 = 805306368;
                    } else {
                        i2 = 268435456;
                    }
                    return ParcelFileDescriptor.open(file, i2);
                } catch (FileNotFoundException e) {
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<TvStreamConfig> getAvailableTvStreamConfigList(String str, int i) throws RemoteException {
            ensureCaptureTvInputPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            try {
                return TvInputManagerService.this.mTvInputHardwareManager.getAvailableTvStreamConfigList(str, callingUid, TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "getAvailableTvStreamConfigList"));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean captureFrame(String str, Surface surface, TvStreamConfig tvStreamConfig, int i) throws RemoteException {
            ensureCaptureTvInputPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int callingUid = Binder.getCallingUid();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), callingUid, i, "captureFrame");
            String str2 = null;
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId);
                    if (orCreateUserStateLocked.inputMap.get(str) == null) {
                        Slog.e(TvInputManagerService.TAG, "input not found for " + str);
                        return false;
                    }
                    Iterator it = orCreateUserStateLocked.sessionStateMap.values().iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        SessionState sessionState = (SessionState) it.next();
                        if (sessionState.inputId.equals(str) && sessionState.hardwareSessionToken != null) {
                            str2 = ((SessionState) orCreateUserStateLocked.sessionStateMap.get(sessionState.hardwareSessionToken)).inputId;
                            break;
                        }
                    }
                    return TvInputManagerService.this.mTvInputHardwareManager.captureFrame(str2 != null ? str2 : str, surface, tvStreamConfig, callingUid, iResolveCallingUserId);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isSingleSessionActive(int i) throws RemoteException {
            ensureCaptureTvInputPermission();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "isSingleSessionActive");
            try {
                synchronized (TvInputManagerService.this.mLock) {
                    UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iResolveCallingUserId);
                    if (orCreateUserStateLocked.sessionStateMap.size() == 1) {
                        return true;
                    }
                    if (orCreateUserStateLocked.sessionStateMap.size() == 2) {
                        SessionState[] sessionStateArr = (SessionState[]) orCreateUserStateLocked.sessionStateMap.values().toArray(new SessionState[2]);
                        if (sessionStateArr[0].hardwareSessionToken != null || sessionStateArr[1].hardwareSessionToken != null) {
                            return true;
                        }
                    }
                    return false;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void ensureCaptureTvInputPermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.CAPTURE_TV_INPUT") != 0) {
                throw new SecurityException("Requires CAPTURE_TV_INPUT permission");
            }
        }

        public void requestChannelBrowsable(Uri uri, int i) throws RemoteException {
            String callingPackageName = getCallingPackageName();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int iResolveCallingUserId = TvInputManagerService.this.resolveCallingUserId(Binder.getCallingPid(), Binder.getCallingUid(), i, "requestChannelBrowsable");
            try {
                Intent intent = new Intent("android.media.tv.action.CHANNEL_BROWSABLE_REQUESTED");
                List<ResolveInfo> listQueryBroadcastReceivers = TvInputManagerService.this.getContext().getPackageManager().queryBroadcastReceivers(intent, 0);
                if (listQueryBroadcastReceivers != null) {
                    Iterator<ResolveInfo> it = listQueryBroadcastReceivers.iterator();
                    while (it.hasNext()) {
                        String str = it.next().activityInfo.packageName;
                        intent.putExtra("android.media.tv.extra.CHANNEL_ID", ContentUris.parseId(uri));
                        intent.putExtra("android.media.tv.extra.PACKAGE_NAME", callingPackageName);
                        intent.setPackage(str);
                        TvInputManagerService.this.getContext().sendBroadcastAsUser(intent, new UserHandle(iResolveCallingUserId));
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            if (DumpUtils.checkDumpPermission(TvInputManagerService.this.mContext, TvInputManagerService.TAG, indentingPrintWriter)) {
                synchronized (TvInputManagerService.this.mLock) {
                    indentingPrintWriter.println("User Ids (Current user: " + TvInputManagerService.this.mCurrentUserId + "):");
                    indentingPrintWriter.increaseIndent();
                    for (int i = 0; i < TvInputManagerService.this.mUserStates.size(); i++) {
                        indentingPrintWriter.println(Integer.valueOf(TvInputManagerService.this.mUserStates.keyAt(i)));
                    }
                    indentingPrintWriter.decreaseIndent();
                    for (int i2 = 0; i2 < TvInputManagerService.this.mUserStates.size(); i2++) {
                        int iKeyAt = TvInputManagerService.this.mUserStates.keyAt(i2);
                        UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(iKeyAt);
                        indentingPrintWriter.println("UserState (" + iKeyAt + "):");
                        indentingPrintWriter.increaseIndent();
                        indentingPrintWriter.println("inputMap: inputId -> TvInputState");
                        indentingPrintWriter.increaseIndent();
                        for (Map.Entry entry : orCreateUserStateLocked.inputMap.entrySet()) {
                            indentingPrintWriter.println(((String) entry.getKey()) + ": " + entry.getValue());
                        }
                        indentingPrintWriter.decreaseIndent();
                        indentingPrintWriter.println("packageSet:");
                        indentingPrintWriter.increaseIndent();
                        Iterator it = orCreateUserStateLocked.packageSet.iterator();
                        while (it.hasNext()) {
                            indentingPrintWriter.println((String) it.next());
                        }
                        indentingPrintWriter.decreaseIndent();
                        indentingPrintWriter.println("clientStateMap: ITvInputClient -> ClientState");
                        indentingPrintWriter.increaseIndent();
                        for (Map.Entry entry2 : orCreateUserStateLocked.clientStateMap.entrySet()) {
                            ClientState clientState = (ClientState) entry2.getValue();
                            indentingPrintWriter.println(entry2.getKey() + ": " + clientState);
                            indentingPrintWriter.increaseIndent();
                            indentingPrintWriter.println("sessionTokens:");
                            indentingPrintWriter.increaseIndent();
                            Iterator it2 = clientState.sessionTokens.iterator();
                            while (it2.hasNext()) {
                                indentingPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((IBinder) it2.next()));
                            }
                            indentingPrintWriter.decreaseIndent();
                            indentingPrintWriter.println("clientTokens: " + clientState.clientToken);
                            indentingPrintWriter.println("userId: " + clientState.userId);
                            indentingPrintWriter.decreaseIndent();
                        }
                        indentingPrintWriter.decreaseIndent();
                        indentingPrintWriter.println("serviceStateMap: ComponentName -> ServiceState");
                        indentingPrintWriter.increaseIndent();
                        for (Map.Entry entry3 : orCreateUserStateLocked.serviceStateMap.entrySet()) {
                            ServiceState serviceState = (ServiceState) entry3.getValue();
                            indentingPrintWriter.println(entry3.getKey() + ": " + serviceState);
                            indentingPrintWriter.increaseIndent();
                            indentingPrintWriter.println("sessionTokens:");
                            indentingPrintWriter.increaseIndent();
                            Iterator it3 = serviceState.sessionTokens.iterator();
                            while (it3.hasNext()) {
                                indentingPrintWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + ((IBinder) it3.next()));
                            }
                            indentingPrintWriter.decreaseIndent();
                            indentingPrintWriter.println("service: " + serviceState.service);
                            indentingPrintWriter.println("callback: " + serviceState.callback);
                            indentingPrintWriter.println("bound: " + serviceState.bound);
                            indentingPrintWriter.println("reconnecting: " + serviceState.reconnecting);
                            indentingPrintWriter.decreaseIndent();
                        }
                        indentingPrintWriter.decreaseIndent();
                        indentingPrintWriter.println("sessionStateMap: ITvInputSession -> SessionState");
                        indentingPrintWriter.increaseIndent();
                        for (Map.Entry entry4 : orCreateUserStateLocked.sessionStateMap.entrySet()) {
                            SessionState sessionState = (SessionState) entry4.getValue();
                            indentingPrintWriter.println(entry4.getKey() + ": " + sessionState);
                            indentingPrintWriter.increaseIndent();
                            indentingPrintWriter.println("inputId: " + sessionState.inputId);
                            indentingPrintWriter.println("client: " + sessionState.client);
                            indentingPrintWriter.println("seq: " + sessionState.seq);
                            indentingPrintWriter.println("callingUid: " + sessionState.callingUid);
                            indentingPrintWriter.println("userId: " + sessionState.userId);
                            indentingPrintWriter.println("sessionToken: " + sessionState.sessionToken);
                            indentingPrintWriter.println("session: " + sessionState.session);
                            indentingPrintWriter.println("logUri: " + sessionState.logUri);
                            indentingPrintWriter.println("hardwareSessionToken: " + sessionState.hardwareSessionToken);
                            indentingPrintWriter.decreaseIndent();
                        }
                        indentingPrintWriter.decreaseIndent();
                        indentingPrintWriter.println("callbackSet:");
                        indentingPrintWriter.increaseIndent();
                        Iterator it4 = orCreateUserStateLocked.callbackSet.iterator();
                        while (it4.hasNext()) {
                            indentingPrintWriter.println(((ITvInputManagerCallback) it4.next()).toString());
                        }
                        indentingPrintWriter.decreaseIndent();
                        indentingPrintWriter.println("mainSessionToken: " + orCreateUserStateLocked.mainSessionToken);
                        indentingPrintWriter.decreaseIndent();
                    }
                }
                TvInputManagerService.this.mTvInputHardwareManager.dump(fileDescriptor, printWriter, strArr);
            }
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

        private UserState(Context context, int i) {
            this.inputMap = new HashMap();
            this.packageSet = new HashSet();
            this.contentRatingSystemList = new ArrayList();
            this.clientStateMap = new HashMap();
            this.serviceStateMap = new HashMap();
            this.sessionStateMap = new HashMap();
            this.callbackSet = new HashSet();
            this.mainSessionToken = null;
            this.persistentDataStore = new PersistentDataStore(context, i);
        }
    }

    private final class ClientState implements IBinder.DeathRecipient {
        private IBinder clientToken;
        private final List<IBinder> sessionTokens = new ArrayList();
        private final int userId;

        ClientState(IBinder iBinder, int i) {
            this.clientToken = iBinder;
            this.userId = i;
        }

        public boolean isEmpty() {
            return this.sessionTokens.isEmpty();
        }

        @Override
        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                ClientState clientState = (ClientState) TvInputManagerService.this.getOrCreateUserStateLocked(this.userId).clientStateMap.get(this.clientToken);
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
        private final Map<String, TvInputInfo> hardwareInputMap;
        private final boolean isHardware;
        private boolean reconnecting;
        private ITvInputService service;
        private final List<IBinder> sessionTokens;

        private ServiceState(ComponentName componentName, int i) {
            this.sessionTokens = new ArrayList();
            this.hardwareInputMap = new HashMap();
            this.component = componentName;
            this.connection = new InputServiceConnection(componentName, i);
            this.isHardware = TvInputManagerService.hasHardwarePermission(TvInputManagerService.this.mContext.getPackageManager(), componentName);
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
        private final ComponentName componentName;
        private IBinder hardwareSessionToken;
        private final String inputId;
        private final boolean isRecordingSession;
        private Uri logUri;
        private final int seq;
        private ITvInputSession session;
        private final IBinder sessionToken;
        private final int userId;

        private SessionState(IBinder iBinder, String str, ComponentName componentName, boolean z, ITvInputClient iTvInputClient, int i, int i2, int i3) {
            this.sessionToken = iBinder;
            this.inputId = str;
            this.componentName = componentName;
            this.isRecordingSession = z;
            this.client = iTvInputClient;
            this.seq = i;
            this.callingUid = i2;
            this.userId = i3;
        }

        @Override
        public void binderDied() {
            synchronized (TvInputManagerService.this.mLock) {
                this.session = null;
                TvInputManagerService.this.clearSessionAndNotifyClientLocked(this);
            }
        }
    }

    private final class InputServiceConnection implements ServiceConnection {
        private final ComponentName mComponent;
        private final int mUserId;

        private InputServiceConnection(ComponentName componentName, int i) {
            this.mComponent = componentName;
            this.mUserId = i;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Iterator it;
            synchronized (TvInputManagerService.this.mLock) {
                UserState userState = (UserState) TvInputManagerService.this.mUserStates.get(this.mUserId);
                if (userState == null) {
                    TvInputManagerService.this.mContext.unbindService(this);
                    return;
                }
                ServiceState serviceState = (ServiceState) userState.serviceStateMap.get(this.mComponent);
                serviceState.service = ITvInputService.Stub.asInterface(iBinder);
                if (!serviceState.isHardware || serviceState.callback != null) {
                    it = serviceState.sessionTokens.iterator();
                    while (it.hasNext()) {
                        TvInputManagerService.this.createSessionInternalLocked(serviceState.service, (IBinder) it.next(), this.mUserId);
                    }
                    for (TvInputState tvInputState : userState.inputMap.values()) {
                        if (tvInputState.info.getComponent().equals(componentName) && tvInputState.state != 0) {
                            TvInputManagerService.this.notifyInputStateChangedLocked(userState, tvInputState.info.getId(), tvInputState.state, null);
                        }
                    }
                    if (serviceState.isHardware) {
                        serviceState.hardwareInputMap.clear();
                        Iterator<TvInputHardwareInfo> it2 = TvInputManagerService.this.mTvInputHardwareManager.getHardwareList().iterator();
                        while (it2.hasNext()) {
                            try {
                                serviceState.service.notifyHardwareAdded(it2.next());
                            } catch (RemoteException e) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e);
                            }
                        }
                        Iterator<HdmiDeviceInfo> it3 = TvInputManagerService.this.mTvInputHardwareManager.getHdmiDeviceList().iterator();
                        while (it3.hasNext()) {
                            try {
                                serviceState.service.notifyHdmiDeviceAdded(it3.next());
                            } catch (RemoteException e2) {
                                Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e2);
                            }
                        }
                    }
                    return;
                }
                serviceState.callback = TvInputManagerService.this.new ServiceCallback(this.mComponent, this.mUserId);
                try {
                    serviceState.service.registerCallback(serviceState.callback);
                } catch (RemoteException e3) {
                    Slog.e(TvInputManagerService.TAG, "error in registerCallback", e3);
                }
                it = serviceState.sessionTokens.iterator();
                while (it.hasNext()) {
                }
                while (r9.hasNext()) {
                }
                if (serviceState.isHardware) {
                }
                return;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (this.mComponent.equals(componentName)) {
                synchronized (TvInputManagerService.this.mLock) {
                    ServiceState serviceState = (ServiceState) TvInputManagerService.this.getOrCreateUserStateLocked(this.mUserId).serviceStateMap.get(this.mComponent);
                    if (serviceState != null) {
                        serviceState.reconnecting = true;
                        serviceState.bound = false;
                        serviceState.service = null;
                        serviceState.callback = null;
                        TvInputManagerService.this.abortPendingCreateSessionRequestsLocked(serviceState, null, this.mUserId);
                    }
                }
                return;
            }
            throw new IllegalArgumentException("Mismatched ComponentName: " + this.mComponent + " (expected), " + componentName + " (actual).");
        }
    }

    private final class ServiceCallback extends ITvInputServiceCallback.Stub {
        private final ComponentName mComponent;
        private final int mUserId;

        ServiceCallback(ComponentName componentName, int i) {
            this.mComponent = componentName;
            this.mUserId = i;
        }

        private void ensureHardwarePermission() {
            if (TvInputManagerService.this.mContext.checkCallingPermission("android.permission.TV_INPUT_HARDWARE") != 0) {
                throw new SecurityException("The caller does not have hardware permission");
            }
        }

        private void ensureValidInput(TvInputInfo tvInputInfo) {
            if (tvInputInfo.getId() == null || !this.mComponent.equals(tvInputInfo.getComponent())) {
                throw new IllegalArgumentException("Invalid TvInputInfo");
            }
        }

        private void addHardwareInputLocked(TvInputInfo tvInputInfo) {
            TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId).hardwareInputMap.put(tvInputInfo.getId(), tvInputInfo);
            TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
        }

        public void addHardwareInput(int i, TvInputInfo tvInputInfo) {
            ensureHardwarePermission();
            ensureValidInput(tvInputInfo);
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.mTvInputHardwareManager.addHardwareInput(i, tvInputInfo);
                addHardwareInputLocked(tvInputInfo);
            }
        }

        public void addHdmiInput(int i, TvInputInfo tvInputInfo) {
            ensureHardwarePermission();
            ensureValidInput(tvInputInfo);
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.mTvInputHardwareManager.addHdmiInput(i, tvInputInfo);
                addHardwareInputLocked(tvInputInfo);
            }
        }

        public void removeHardwareInput(String str) {
            ensureHardwarePermission();
            synchronized (TvInputManagerService.this.mLock) {
                if (TvInputManagerService.this.getServiceStateLocked(this.mComponent, this.mUserId).hardwareInputMap.remove(str) != null) {
                    TvInputManagerService.this.buildTvInputListLocked(this.mUserId, null);
                    TvInputManagerService.this.mTvInputHardwareManager.removeHardwareInput(str);
                } else {
                    Slog.e(TvInputManagerService.TAG, "failed to remove input " + str);
                }
            }
        }
    }

    private final class SessionCallback extends ITvInputSessionCallback.Stub {
        private final InputChannel[] mChannels;
        private final SessionState mSessionState;

        SessionCallback(SessionState sessionState, InputChannel[] inputChannelArr) {
            this.mSessionState = sessionState;
            this.mChannels = inputChannelArr;
        }

        public void onSessionCreated(ITvInputSession iTvInputSession, IBinder iBinder) {
            synchronized (TvInputManagerService.this.mLock) {
                this.mSessionState.session = iTvInputSession;
                this.mSessionState.hardwareSessionToken = iBinder;
                if (iTvInputSession == null || !addSessionTokenToClientStateLocked(iTvInputSession)) {
                    TvInputManagerService.this.removeSessionStateLocked(this.mSessionState.sessionToken, this.mSessionState.userId);
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.inputId, null, null, this.mSessionState.seq);
                } else {
                    TvInputManagerService.this.sendSessionTokenToClientLocked(this.mSessionState.client, this.mSessionState.inputId, this.mSessionState.sessionToken, this.mChannels[0], this.mSessionState.seq);
                }
                this.mChannels[0].dispose();
            }
        }

        private boolean addSessionTokenToClientStateLocked(ITvInputSession iTvInputSession) {
            try {
                iTvInputSession.asBinder().linkToDeath(this.mSessionState, 0);
                IBinder iBinderAsBinder = this.mSessionState.client.asBinder();
                UserState orCreateUserStateLocked = TvInputManagerService.this.getOrCreateUserStateLocked(this.mSessionState.userId);
                ClientState clientState = (ClientState) orCreateUserStateLocked.clientStateMap.get(iBinderAsBinder);
                if (clientState == null) {
                    clientState = TvInputManagerService.this.new ClientState(iBinderAsBinder, this.mSessionState.userId);
                    try {
                        iBinderAsBinder.linkToDeath(clientState, 0);
                        orCreateUserStateLocked.clientStateMap.put(iBinderAsBinder, clientState);
                    } catch (RemoteException e) {
                        Slog.e(TvInputManagerService.TAG, "client process has already died", e);
                        return false;
                    }
                }
                clientState.sessionTokens.add(this.mSessionState.sessionToken);
                return true;
            } catch (RemoteException e2) {
                Slog.e(TvInputManagerService.TAG, "session process has already died", e2);
                return false;
            }
        }

        public void onChannelRetuned(Uri uri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onChannelRetuned(uri, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onChannelRetuned", e);
                }
            }
        }

        public void onTracksChanged(List<TvTrackInfo> list) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTracksChanged(list, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTracksChanged", e);
                }
            }
        }

        public void onTrackSelected(int i, String str) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTrackSelected(i, str, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTrackSelected", e);
                }
            }
        }

        public void onVideoAvailable() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onVideoAvailable(this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onVideoAvailable", e);
                }
            }
        }

        public void onVideoUnavailable(int i) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onVideoUnavailable(i, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onVideoUnavailable", e);
                }
            }
        }

        public void onContentAllowed() {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onContentAllowed(this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onContentAllowed", e);
                }
            }
        }

        public void onContentBlocked(String str) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onContentBlocked(str, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onContentBlocked", e);
                }
            }
        }

        public void onLayoutSurface(int i, int i2, int i3, int i4) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onLayoutSurface(i, i2, i3, i4, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onLayoutSurface", e);
                }
            }
        }

        public void onSessionEvent(String str, Bundle bundle) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onSessionEvent(str, bundle, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onSessionEvent", e);
                }
            }
        }

        public void onTimeShiftStatusChanged(int i) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTimeShiftStatusChanged(i, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTimeShiftStatusChanged", e);
                }
            }
        }

        public void onTimeShiftStartPositionChanged(long j) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTimeShiftStartPositionChanged(j, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTimeShiftStartPositionChanged", e);
                }
            }
        }

        public void onTimeShiftCurrentPositionChanged(long j) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTimeShiftCurrentPositionChanged(j, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTimeShiftCurrentPositionChanged", e);
                }
            }
        }

        public void onTuned(Uri uri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onTuned(this.mSessionState.seq, uri);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onTuned", e);
                }
            }
        }

        public void onRecordingStopped(Uri uri) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onRecordingStopped(uri, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onRecordingStopped", e);
                }
            }
        }

        public void onError(int i) {
            synchronized (TvInputManagerService.this.mLock) {
                if (this.mSessionState.session == null || this.mSessionState.client == null) {
                    return;
                }
                try {
                    this.mSessionState.client.onError(i, this.mSessionState.seq);
                } catch (RemoteException e) {
                    Slog.e(TvInputManagerService.TAG, "error in onError", e);
                }
            }
        }
    }

    private static final class WatchLogHandler extends Handler {
        static final int MSG_LOG_WATCH_END = 2;
        static final int MSG_LOG_WATCH_START = 1;
        static final int MSG_SWITCH_CONTENT_RESOLVER = 3;
        private ContentResolver mContentResolver;

        WatchLogHandler(ContentResolver contentResolver, Looper looper) {
            super(looper);
            this.mContentResolver = contentResolver;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    String str = (String) someArgs.arg1;
                    long jLongValue = ((Long) someArgs.arg2).longValue();
                    long jLongValue2 = ((Long) someArgs.arg3).longValue();
                    Bundle bundle = (Bundle) someArgs.arg4;
                    IBinder iBinder = (IBinder) someArgs.arg5;
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("package_name", str);
                    contentValues.put("watch_start_time_utc_millis", Long.valueOf(jLongValue));
                    contentValues.put("channel_id", Long.valueOf(jLongValue2));
                    if (bundle != null) {
                        contentValues.put("tune_params", encodeTuneParams(bundle));
                    }
                    contentValues.put("session_token", iBinder.toString());
                    this.mContentResolver.insert(TvContract.WatchedPrograms.CONTENT_URI, contentValues);
                    someArgs.recycle();
                    break;
                case 2:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    IBinder iBinder2 = (IBinder) someArgs2.arg1;
                    long jLongValue3 = ((Long) someArgs2.arg2).longValue();
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put("watch_end_time_utc_millis", Long.valueOf(jLongValue3));
                    contentValues2.put("session_token", iBinder2.toString());
                    this.mContentResolver.insert(TvContract.WatchedPrograms.CONTENT_URI, contentValues2);
                    someArgs2.recycle();
                    break;
                case 3:
                    this.mContentResolver = (ContentResolver) message.obj;
                    break;
                default:
                    Slog.w(TvInputManagerService.TAG, "unhandled message code: " + message.what);
                    break;
            }
        }

        private String encodeTuneParams(Bundle bundle) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = bundle.keySet().iterator();
            while (it.hasNext()) {
                String next = it.next();
                Object obj = bundle.get(next);
                if (obj != null) {
                    sb.append(replaceEscapeCharacters(next));
                    sb.append("=");
                    sb.append(replaceEscapeCharacters(obj.toString()));
                    if (it.hasNext()) {
                        sb.append(", ");
                    }
                }
            }
            return sb.toString();
        }

        private String replaceEscapeCharacters(String str) {
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                if ("%=,".indexOf(c) >= 0) {
                    sb.append('%');
                }
                sb.append(c);
            }
            return sb.toString();
        }
    }

    private final class HardwareListener implements TvInputHardwareManager.Listener {
        private HardwareListener() {
        }

        @Override
        public void onStateChanged(String str, int i) {
            synchronized (TvInputManagerService.this.mLock) {
                TvInputManagerService.this.setStateLocked(str, i, TvInputManagerService.this.mCurrentUserId);
            }
        }

        @Override
        public void onHardwareDeviceAdded(TvInputHardwareInfo tvInputHardwareInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHardwareAdded(tvInputHardwareInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareAdded", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHardwareDeviceRemoved(TvInputHardwareInfo tvInputHardwareInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHardwareRemoved(tvInputHardwareInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHardwareRemoved", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHdmiDeviceAdded(HdmiDeviceInfo hdmiDeviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHdmiDeviceAdded(hdmiDeviceInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceAdded", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHdmiDeviceRemoved(HdmiDeviceInfo hdmiDeviceInfo) {
            synchronized (TvInputManagerService.this.mLock) {
                for (ServiceState serviceState : TvInputManagerService.this.getOrCreateUserStateLocked(TvInputManagerService.this.mCurrentUserId).serviceStateMap.values()) {
                    if (serviceState.isHardware && serviceState.service != null) {
                        try {
                            serviceState.service.notifyHdmiDeviceRemoved(hdmiDeviceInfo);
                        } catch (RemoteException e) {
                            Slog.e(TvInputManagerService.TAG, "error in notifyHdmiDeviceRemoved", e);
                        }
                    }
                }
            }
        }

        @Override
        public void onHdmiDeviceUpdated(String str, HdmiDeviceInfo hdmiDeviceInfo) {
            Integer num;
            synchronized (TvInputManagerService.this.mLock) {
                switch (hdmiDeviceInfo.getDevicePowerStatus()) {
                    case 0:
                        num = 0;
                        break;
                    case 1:
                    case 2:
                    case 3:
                        num = 1;
                        break;
                    default:
                        num = null;
                        break;
                }
                if (num != null) {
                    TvInputManagerService.this.setStateLocked(str, num.intValue(), TvInputManagerService.this.mCurrentUserId);
                }
            }
        }
    }

    private static class SessionNotFoundException extends IllegalArgumentException {
        public SessionNotFoundException(String str) {
            super(str);
        }
    }
}
