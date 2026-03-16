package com.android.server.media;

import android.R;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.IRemoteVolumeController;
import android.media.session.IActiveSessionsListener;
import android.media.session.ISession;
import android.media.session.ISessionCallback;
import android.media.session.ISessionManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MediaSessionService extends SystemService implements Watchdog.Monitor {
    private static final int WAKELOCK_TIMEOUT = 5000;
    private final ArrayList<MediaSessionRecord> mAllSessions;
    private AudioManager mAudioManager;
    private IAudioService mAudioService;
    private ContentResolver mContentResolver;
    private int mCurrentUserId;
    private final MessageHandler mHandler;
    final IBinder mICallback;
    private KeyguardManager mKeyguardManager;
    private final Object mLock;
    private final PowerManager.WakeLock mMediaEventWakeLock;
    private final MediaSessionStack mPriorityStack;
    private IRemoteVolumeController mRvc;
    private final SessionManagerImpl mSessionManagerImpl;
    private final ArrayList<SessionsListenerRecord> mSessionsListeners;
    private SettingsObserver mSettingsObserver;
    private final boolean mUseMasterVolume;
    private final SparseArray<UserRecord> mUserRecords;
    private static final String TAG = "MediaSessionService";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public MediaSessionService(Context context) {
        super(context);
        this.mICallback = new Binder();
        this.mAllSessions = new ArrayList<>();
        this.mUserRecords = new SparseArray<>();
        this.mSessionsListeners = new ArrayList<>();
        this.mLock = new Object();
        this.mHandler = new MessageHandler();
        this.mCurrentUserId = -1;
        this.mSessionManagerImpl = new SessionManagerImpl();
        this.mPriorityStack = new MediaSessionStack();
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mMediaEventWakeLock = pm.newWakeLock(1, "handleMediaEvent");
        this.mUseMasterVolume = context.getResources().getBoolean(R.^attr-private.alertDialogCenterButtons);
    }

    @Override
    public void onStart() {
        publishBinderService("media_session", this.mSessionManagerImpl);
        Watchdog.getInstance().addMonitor(this);
        updateUser();
        this.mKeyguardManager = (KeyguardManager) getContext().getSystemService("keyguard");
        this.mAudioService = getAudioService();
        this.mAudioManager = (AudioManager) getContext().getSystemService("audio");
        this.mContentResolver = getContext().getContentResolver();
        this.mSettingsObserver = new SettingsObserver();
        this.mSettingsObserver.observe();
    }

    private IAudioService getAudioService() {
        IBinder b = ServiceManager.getService("audio");
        return IAudioService.Stub.asInterface(b);
    }

    public void updateSession(MediaSessionRecord record) {
        synchronized (this.mLock) {
            if (!this.mAllSessions.contains(record)) {
                Log.d(TAG, "Unknown session updated. Ignoring.");
            } else {
                this.mPriorityStack.onSessionStateChange(record);
                this.mHandler.post(1, record.getUserId(), 0);
            }
        }
    }

    public void notifyRemoteVolumeChanged(int flags, MediaSessionRecord session) {
        if (this.mRvc != null) {
            try {
                this.mRvc.remoteVolumeChanged(session.getControllerBinder(), flags);
            } catch (Exception e) {
                Log.wtf(TAG, "Error sending volume change to system UI.", e);
            }
        }
    }

    public void onSessionPlaystateChange(MediaSessionRecord record, int oldState, int newState) {
        synchronized (this.mLock) {
            if (!this.mAllSessions.contains(record)) {
                Log.d(TAG, "Unknown session changed playback state. Ignoring.");
                return;
            }
            boolean updateSessions = this.mPriorityStack.onPlaystateChange(record, oldState, newState);
            if (updateSessions) {
                this.mHandler.post(1, record.getUserId(), 0);
            }
        }
    }

    public void onSessionPlaybackTypeChanged(MediaSessionRecord record) {
        synchronized (this.mLock) {
            if (!this.mAllSessions.contains(record)) {
                Log.d(TAG, "Unknown session changed playback type. Ignoring.");
            } else {
                pushRemoteVolumeUpdateLocked(record.getUserId());
            }
        }
    }

    @Override
    public void onStartUser(int userHandle) {
        updateUser();
    }

    @Override
    public void onSwitchUser(int userHandle) {
        updateUser();
    }

    @Override
    public void onStopUser(int userHandle) {
        synchronized (this.mLock) {
            UserRecord user = this.mUserRecords.get(userHandle);
            if (user != null) {
                destroyUserLocked(user);
            }
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    protected void enforcePhoneStatePermission(int pid, int uid) {
        if (getContext().checkPermission("android.permission.MODIFY_PHONE_STATE", pid, uid) != 0) {
            throw new SecurityException("Must hold the MODIFY_PHONE_STATE permission.");
        }
    }

    void sessionDied(MediaSessionRecord session) {
        synchronized (this.mLock) {
            destroySessionLocked(session);
        }
    }

    void destroySession(MediaSessionRecord session) {
        synchronized (this.mLock) {
            destroySessionLocked(session);
        }
    }

    private void updateUser() {
        synchronized (this.mLock) {
            int userId = ActivityManager.getCurrentUser();
            if (this.mCurrentUserId != userId) {
                int oldUserId = this.mCurrentUserId;
                this.mCurrentUserId = userId;
                UserRecord oldUser = this.mUserRecords.get(oldUserId);
                if (oldUser != null) {
                    oldUser.stopLocked();
                }
                UserRecord newUser = getOrCreateUser(userId);
                newUser.startLocked();
            }
        }
    }

    private void updateActiveSessionListeners() {
        synchronized (this.mLock) {
            for (int i = this.mSessionsListeners.size() - 1; i >= 0; i--) {
                SessionsListenerRecord listener = this.mSessionsListeners.get(i);
                try {
                    enforceMediaPermissions(listener.mComponentName, listener.mPid, listener.mUid, listener.mUserId);
                } catch (SecurityException e) {
                    Log.i(TAG, "ActiveSessionsListener " + listener.mComponentName + " is no longer authorized. Disconnecting.");
                    this.mSessionsListeners.remove(i);
                    try {
                        listener.mListener.onActiveSessionsChanged(new ArrayList());
                    } catch (Exception e2) {
                    }
                }
            }
        }
    }

    private void destroyUserLocked(UserRecord user) {
        user.stopLocked();
        user.destroyLocked();
        this.mUserRecords.remove(user.mUserId);
    }

    private void destroySessionLocked(MediaSessionRecord session) {
        int userId = session.getUserId();
        UserRecord user = this.mUserRecords.get(userId);
        if (user != null) {
            user.removeSessionLocked(session);
        }
        this.mPriorityStack.removeSession(session);
        this.mAllSessions.remove(session);
        try {
            session.getCallback().asBinder().unlinkToDeath(session, 0);
        } catch (Exception e) {
        }
        session.onDestroy();
        this.mHandler.post(1, session.getUserId(), 0);
    }

    private void enforcePackageName(String packageName, int uid) {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName may not be empty");
        }
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        for (String str : packages) {
            if (packageName.equals(str)) {
                return;
            }
        }
        throw new IllegalArgumentException("packageName is not owned by the calling process");
    }

    private void enforceMediaPermissions(ComponentName compName, int pid, int uid, int resolvedUserId) {
        if (getContext().checkPermission("android.permission.MEDIA_CONTENT_CONTROL", pid, uid) != 0 && !isEnabledNotificationListener(compName, UserHandle.getUserId(uid), resolvedUserId)) {
            throw new SecurityException("Missing permission to control media.");
        }
    }

    private void enforceStatusBarPermission(String action, int pid, int uid) {
        if (getContext().checkPermission("android.permission.STATUS_BAR_SERVICE", pid, uid) != 0) {
            throw new SecurityException("Only system ui may " + action);
        }
    }

    private boolean isEnabledNotificationListener(ComponentName compName, int userId, int forUserId) {
        if (userId != forUserId) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "Checking if enabled notification listener " + compName);
        }
        if (compName == null) {
            return false;
        }
        String enabledNotifListeners = Settings.Secure.getStringForUser(this.mContentResolver, "enabled_notification_listeners", userId);
        if (enabledNotifListeners != null) {
            String[] components = enabledNotifListeners.split(":");
            for (String str : components) {
                ComponentName component = ComponentName.unflattenFromString(str);
                if (component != null && compName.equals(component)) {
                    if (DEBUG) {
                        Log.d(TAG, "ok to get sessions: " + component + " is authorized notification listener");
                    }
                    return true;
                }
            }
        }
        if (!DEBUG) {
            return false;
        }
        Log.d(TAG, "not ok to get sessions, " + compName + " is not in list of ENABLED_NOTIFICATION_LISTENERS for user " + userId);
        return false;
    }

    private MediaSessionRecord createSessionInternal(int callerPid, int callerUid, int userId, String callerPackageName, ISessionCallback cb, String tag) throws RemoteException {
        MediaSessionRecord mediaSessionRecordCreateSessionLocked;
        synchronized (this.mLock) {
            mediaSessionRecordCreateSessionLocked = createSessionLocked(callerPid, callerUid, userId, callerPackageName, cb, tag);
        }
        return mediaSessionRecordCreateSessionLocked;
    }

    private MediaSessionRecord createSessionLocked(int callerPid, int callerUid, int userId, String callerPackageName, ISessionCallback cb, String tag) {
        MediaSessionRecord session = new MediaSessionRecord(callerPid, callerUid, userId, callerPackageName, cb, tag, this, this.mHandler);
        try {
            cb.asBinder().linkToDeath(session, 0);
            this.mAllSessions.add(session);
            this.mPriorityStack.addSession(session);
            UserRecord user = getOrCreateUser(userId);
            user.addSessionLocked(session);
            this.mHandler.post(1, userId, 0);
            if (DEBUG) {
                Log.d(TAG, "Created session for package " + callerPackageName + " with tag " + tag);
            }
            return session;
        } catch (RemoteException e) {
            throw new RuntimeException("Media Session owner died prematurely.", e);
        }
    }

    private UserRecord getOrCreateUser(int userId) {
        UserRecord user = this.mUserRecords.get(userId);
        if (user == null) {
            UserRecord user2 = new UserRecord(getContext(), userId);
            this.mUserRecords.put(userId, user2);
            return user2;
        }
        return user;
    }

    private int findIndexOfSessionsListenerLocked(IActiveSessionsListener listener) {
        for (int i = this.mSessionsListeners.size() - 1; i >= 0; i--) {
            if (this.mSessionsListeners.get(i).mListener.asBinder() == listener.asBinder()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSessionDiscoverable(MediaSessionRecord record) {
        return record.isActive();
    }

    private void pushSessionsChanged(int userId) {
        synchronized (this.mLock) {
            List<MediaSessionRecord> records = this.mPriorityStack.getActiveSessions(userId);
            int size = records.size();
            if (size > 0 && records.get(0).isPlaybackActive(false)) {
                rememberMediaButtonReceiverLocked(records.get(0));
            }
            ArrayList<MediaSession.Token> tokens = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                tokens.add(new MediaSession.Token(records.get(i).getControllerBinder()));
            }
            pushRemoteVolumeUpdateLocked(userId);
            for (int i2 = this.mSessionsListeners.size() - 1; i2 >= 0; i2--) {
                SessionsListenerRecord record = this.mSessionsListeners.get(i2);
                if (record.mUserId == -1 || record.mUserId == userId) {
                    try {
                        record.mListener.onActiveSessionsChanged(tokens);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Dead ActiveSessionsListener in pushSessionsChanged, removing", e);
                        this.mSessionsListeners.remove(i2);
                    }
                }
            }
        }
    }

    private void pushRemoteVolumeUpdateLocked(int userId) {
        if (this.mRvc != null) {
            try {
                MediaSessionRecord record = this.mPriorityStack.getDefaultRemoteSession(userId);
                this.mRvc.updateRemoteController(record == null ? null : record.getControllerBinder());
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error sending default remote volume to sys ui.", e);
            }
        }
    }

    private void rememberMediaButtonReceiverLocked(MediaSessionRecord record) {
        PendingIntent receiver = record.getMediaButtonReceiver();
        UserRecord user = this.mUserRecords.get(record.getUserId());
        if (receiver == null || user == null) {
            return;
        }
        user.mLastMediaButtonReceiver = receiver;
    }

    final class UserRecord {
        private PendingIntent mLastMediaButtonReceiver;
        private final ArrayList<MediaSessionRecord> mSessions = new ArrayList<>();
        private final int mUserId;

        public UserRecord(Context context, int userId) {
            this.mUserId = userId;
        }

        public void startLocked() {
        }

        public void stopLocked() {
        }

        public void destroyLocked() {
            for (int i = this.mSessions.size() - 1; i >= 0; i--) {
                MediaSessionRecord session = this.mSessions.get(i);
                MediaSessionService.this.destroySessionLocked(session);
            }
        }

        public ArrayList<MediaSessionRecord> getSessionsLocked() {
            return this.mSessions;
        }

        public void addSessionLocked(MediaSessionRecord session) {
            this.mSessions.add(session);
        }

        public void removeSessionLocked(MediaSessionRecord session) {
            this.mSessions.remove(session);
        }

        public void dumpLocked(PrintWriter pw, String prefix) {
            pw.println(prefix + "Record for user " + this.mUserId);
            String indent = prefix + "  ";
            pw.println(indent + "MediaButtonReceiver:" + this.mLastMediaButtonReceiver);
            int size = this.mSessions.size();
            pw.println(indent + size + " Sessions:");
            for (int i = 0; i < size; i++) {
                pw.println(indent + this.mSessions.get(i).toString());
            }
        }
    }

    final class SessionsListenerRecord implements IBinder.DeathRecipient {
        private final ComponentName mComponentName;
        private final IActiveSessionsListener mListener;
        private final int mPid;
        private final int mUid;
        private final int mUserId;

        public SessionsListenerRecord(IActiveSessionsListener listener, ComponentName componentName, int userId, int pid, int uid) {
            this.mListener = listener;
            this.mComponentName = componentName;
            this.mUserId = userId;
            this.mPid = pid;
            this.mUid = uid;
        }

        @Override
        public void binderDied() {
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.mSessionsListeners.remove(this);
            }
        }
    }

    final class SettingsObserver extends ContentObserver {
        private final Uri mSecureSettingsUri;

        private SettingsObserver() {
            super(null);
            this.mSecureSettingsUri = Settings.Secure.getUriFor("enabled_notification_listeners");
        }

        private void observe() {
            MediaSessionService.this.mContentResolver.registerContentObserver(this.mSecureSettingsUri, false, this, -1);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            MediaSessionService.this.updateActiveSessionListeners();
        }
    }

    class SessionManagerImpl extends ISessionManager.Stub {
        private static final String EXTRA_WAKELOCK_ACQUIRED = "android.media.AudioService.WAKELOCK_ACQUIRED";
        private static final int WAKELOCK_RELEASE_ON_FINISHED = 1980;
        private KeyEventWakeLockReceiver mKeyEventReceiver;
        private boolean mVoiceButtonDown = false;
        private boolean mVoiceButtonHandled = false;
        BroadcastReceiver mKeyEventDone = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras;
                if (intent != null && (extras = intent.getExtras()) != null) {
                    synchronized (MediaSessionService.this.mLock) {
                        if (extras.containsKey(SessionManagerImpl.EXTRA_WAKELOCK_ACQUIRED) && MediaSessionService.this.mMediaEventWakeLock.isHeld()) {
                            MediaSessionService.this.mMediaEventWakeLock.release();
                        }
                    }
                }
            }
        };

        SessionManagerImpl() {
            this.mKeyEventReceiver = new KeyEventWakeLockReceiver(MediaSessionService.this.mHandler);
        }

        public ISession createSession(String packageName, ISessionCallback cb, String tag, int userId) throws RemoteException {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforcePackageName(packageName, uid);
                int resolvedUserId = ActivityManager.handleIncomingUser(pid, uid, userId, false, true, "createSession", packageName);
                if (cb != null) {
                    return MediaSessionService.this.createSessionInternal(pid, uid, resolvedUserId, packageName, cb, tag).getSessionBinder();
                }
                throw new IllegalArgumentException("Controller callback cannot be null");
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public List<IBinder> getSessions(ComponentName componentName, int userId) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                int resolvedUserId = verifySessionsRequest(componentName, userId, pid, uid);
                ArrayList<IBinder> binders = new ArrayList<>();
                synchronized (MediaSessionService.this.mLock) {
                    ArrayList<MediaSessionRecord> records = MediaSessionService.this.mPriorityStack.getActiveSessions(resolvedUserId);
                    int size = records.size();
                    for (int i = 0; i < size; i++) {
                        binders.add(records.get(i).getControllerBinder().asBinder());
                    }
                }
                return binders;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void addSessionsListener(IActiveSessionsListener listener, ComponentName componentName, int userId) throws RemoteException {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                int resolvedUserId = verifySessionsRequest(componentName, userId, pid, uid);
                synchronized (MediaSessionService.this.mLock) {
                    int index = MediaSessionService.this.findIndexOfSessionsListenerLocked(listener);
                    if (index != -1) {
                        Log.w(MediaSessionService.TAG, "ActiveSessionsListener is already added, ignoring");
                        return;
                    }
                    SessionsListenerRecord record = MediaSessionService.this.new SessionsListenerRecord(listener, componentName, resolvedUserId, pid, uid);
                    try {
                        listener.asBinder().linkToDeath(record, 0);
                        MediaSessionService.this.mSessionsListeners.add(record);
                    } catch (RemoteException e) {
                        Log.e(MediaSessionService.TAG, "ActiveSessionsListener is dead, ignoring it", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void removeSessionsListener(IActiveSessionsListener listener) throws RemoteException {
            synchronized (MediaSessionService.this.mLock) {
                int index = MediaSessionService.this.findIndexOfSessionsListenerLocked(listener);
                if (index != -1) {
                    SessionsListenerRecord record = (SessionsListenerRecord) MediaSessionService.this.mSessionsListeners.remove(index);
                    try {
                        record.mListener.asBinder().unlinkToDeath(record, 0);
                    } catch (Exception e) {
                    }
                }
            }
        }

        public void dispatchMediaKeyEvent(KeyEvent keyEvent, boolean needWakeLock) {
            if (keyEvent == null || !KeyEvent.isMediaKey(keyEvent.getKeyCode())) {
                Log.w(MediaSessionService.TAG, "Attempted to dispatch null or non-media key event.");
                return;
            }
            Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                if (!isUserSetupComplete()) {
                    Slog.i(MediaSessionService.TAG, "Not dispatching media key event because user setup is in progress.");
                    return;
                }
                if (isGlobalPriorityActive() && uid != 1000) {
                    Slog.i(MediaSessionService.TAG, "Only the system can dispatch media key event to the global priority session.");
                    return;
                }
                synchronized (MediaSessionService.this.mLock) {
                    boolean useNotPlayingSessions = ((UserRecord) MediaSessionService.this.mUserRecords.get(ActivityManager.getCurrentUser())).mLastMediaButtonReceiver == null;
                    MediaSessionRecord session = MediaSessionService.this.mPriorityStack.getDefaultMediaButtonSession(MediaSessionService.this.mCurrentUserId, useNotPlayingSessions);
                    if (isVoiceKey(keyEvent.getKeyCode())) {
                        handleVoiceKeyEventLocked(keyEvent, needWakeLock, session);
                    } else {
                        dispatchMediaKeyEventLocked(keyEvent, needWakeLock, session);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void dispatchAdjustVolume(int suggestedStream, int delta, int flags) {
            Binder.getCallingPid();
            Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    MediaSessionRecord session = MediaSessionService.this.mPriorityStack.getDefaultVolumeSession(MediaSessionService.this.mCurrentUserId);
                    dispatchAdjustVolumeLocked(suggestedStream, delta, flags, session);
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setRemoteVolumeController(IRemoteVolumeController rvc) {
            int pid = Binder.getCallingPid();
            int uid = Binder.getCallingUid();
            long token = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforceStatusBarPermission("listen for volume changes", pid, uid);
                MediaSessionService.this.mRvc = rvc;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public boolean isGlobalPriorityActive() {
            return MediaSessionService.this.mPriorityStack.isGlobalPriorityActive();
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (MediaSessionService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump MediaSessionService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            pw.println("MEDIA SESSION SERVICE (dumpsys media_session)");
            pw.println();
            synchronized (MediaSessionService.this.mLock) {
                pw.println(MediaSessionService.this.mSessionsListeners.size() + " sessions listeners.");
                int count = MediaSessionService.this.mAllSessions.size();
                pw.println(count + " Sessions:");
                for (int i = 0; i < count; i++) {
                    ((MediaSessionRecord) MediaSessionService.this.mAllSessions.get(i)).dump(pw, "");
                    pw.println();
                }
                MediaSessionService.this.mPriorityStack.dump(pw, "");
                pw.println("User Records:");
                int count2 = MediaSessionService.this.mUserRecords.size();
                for (int i2 = 0; i2 < count2; i2++) {
                    UserRecord user = (UserRecord) MediaSessionService.this.mUserRecords.get(i2);
                    user.dumpLocked(pw, "");
                }
            }
        }

        private int verifySessionsRequest(ComponentName componentName, int userId, int pid, int uid) {
            String packageName = null;
            if (componentName != null) {
                packageName = componentName.getPackageName();
                MediaSessionService.this.enforcePackageName(packageName, uid);
            }
            int resolvedUserId = ActivityManager.handleIncomingUser(pid, uid, userId, true, true, "getSessions", packageName);
            MediaSessionService.this.enforceMediaPermissions(componentName, pid, uid, resolvedUserId);
            return resolvedUserId;
        }

        private void dispatchAdjustVolumeLocked(int suggestedStream, int direction, int flags, MediaSessionRecord session) {
            if (MediaSessionService.DEBUG) {
                String description = session == null ? null : session.toString();
                Log.d(MediaSessionService.TAG, "Adjusting session " + description + " by " + direction + ". flags=" + flags + ", suggestedStream=" + suggestedStream);
            }
            boolean preferSuggestedStream = false;
            if (isValidLocalStreamType(suggestedStream) && AudioSystem.isStreamActive(suggestedStream, 0)) {
                preferSuggestedStream = true;
            }
            if (session == null || preferSuggestedStream) {
                if ((flags & 512) != 0 && !AudioSystem.isStreamActive(3, 0) && !AudioSystem.isStreamActive(0, 0) && !AudioSystem.isStreamActive(10, 0)) {
                    if (MediaSessionService.DEBUG) {
                        Log.d(MediaSessionService.TAG, "No active session to adjust, skipping media only volume event");
                        return;
                    }
                    return;
                }
                try {
                    String packageName = MediaSessionService.this.getContext().getOpPackageName();
                    if (MediaSessionService.this.mUseMasterVolume) {
                        boolean isMasterMute = MediaSessionService.this.mAudioService.isMasterMute();
                        if (direction != -99) {
                            MediaSessionService.this.mAudioService.adjustMasterVolume(direction, flags, packageName);
                            if (isMasterMute && direction != 0) {
                                MediaSessionService.this.mAudioService.setMasterMute(false, flags, packageName, MediaSessionService.this.mICallback);
                            }
                        } else {
                            MediaSessionService.this.mAudioService.setMasterMute(!isMasterMute, flags, packageName, MediaSessionService.this.mICallback);
                        }
                    } else {
                        boolean isStreamMute = MediaSessionService.this.mAudioService.isStreamMute(suggestedStream);
                        if (direction == -99) {
                            MediaSessionService.this.mAudioManager.setStreamMute(suggestedStream, !isStreamMute);
                        } else {
                            MediaSessionService.this.mAudioService.adjustSuggestedStreamVolume(direction, suggestedStream, flags, packageName);
                            if (isStreamMute && direction != 0) {
                                MediaSessionService.this.mAudioManager.setStreamMute(suggestedStream, false);
                            }
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(MediaSessionService.TAG, "Error adjusting default volume.", e);
                }
                return;
            }
            session.adjustVolume(direction, flags, MediaSessionService.this.getContext().getPackageName(), UserHandle.myUserId(), true);
        }

        private void handleVoiceKeyEventLocked(KeyEvent keyEvent, boolean needWakeLock, MediaSessionRecord session) {
            if (session != null && session.hasFlag(65536)) {
                dispatchMediaKeyEventLocked(keyEvent, needWakeLock, session);
                return;
            }
            int action = keyEvent.getAction();
            boolean isLongPress = (keyEvent.getFlags() & 128) != 0;
            if (action == 0) {
                if (keyEvent.getRepeatCount() == 0) {
                    this.mVoiceButtonDown = true;
                    this.mVoiceButtonHandled = false;
                    return;
                } else {
                    if (this.mVoiceButtonDown && !this.mVoiceButtonHandled && isLongPress) {
                        this.mVoiceButtonHandled = true;
                        startVoiceInput(needWakeLock);
                        return;
                    }
                    return;
                }
            }
            if (action == 1 && this.mVoiceButtonDown) {
                this.mVoiceButtonDown = false;
                if (!this.mVoiceButtonHandled && !keyEvent.isCanceled()) {
                    KeyEvent downEvent = KeyEvent.changeAction(keyEvent, 0);
                    dispatchMediaKeyEventLocked(downEvent, needWakeLock, session);
                    dispatchMediaKeyEventLocked(keyEvent, needWakeLock, session);
                }
            }
        }

        private void dispatchMediaKeyEventLocked(KeyEvent keyEvent, boolean needWakeLock, MediaSessionRecord session) {
            int i;
            int i2;
            if (session != null) {
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, "Sending media key to " + session.toString());
                }
                if (needWakeLock) {
                    this.mKeyEventReceiver.aquireWakeLockLocked();
                }
                if (!needWakeLock) {
                    i2 = -1;
                } else {
                    i2 = this.mKeyEventReceiver.mLastTimeoutId;
                }
                session.sendMediaButton(keyEvent, i2, this.mKeyEventReceiver);
                return;
            }
            int userId = ActivityManager.getCurrentUser();
            UserRecord user = (UserRecord) MediaSessionService.this.mUserRecords.get(userId);
            if (user.mLastMediaButtonReceiver != null) {
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, "Sending media key to last known PendingIntent");
                }
                if (needWakeLock) {
                    this.mKeyEventReceiver.aquireWakeLockLocked();
                }
                Intent mediaButtonIntent = new Intent("android.intent.action.MEDIA_BUTTON");
                mediaButtonIntent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
                try {
                    PendingIntent pendingIntent = user.mLastMediaButtonReceiver;
                    Context context = MediaSessionService.this.getContext();
                    if (!needWakeLock) {
                        i = -1;
                    } else {
                        i = this.mKeyEventReceiver.mLastTimeoutId;
                    }
                    pendingIntent.send(context, i, mediaButtonIntent, this.mKeyEventReceiver, null);
                    return;
                } catch (PendingIntent.CanceledException e) {
                    Log.i(MediaSessionService.TAG, "Error sending key event to media button receiver " + user.mLastMediaButtonReceiver, e);
                    return;
                }
            }
            if (MediaSessionService.DEBUG) {
                Log.d(MediaSessionService.TAG, "Sending media key ordered broadcast");
            }
            if (needWakeLock) {
                MediaSessionService.this.mMediaEventWakeLock.acquire();
            }
            Intent keyIntent = new Intent("android.intent.action.MEDIA_BUTTON", (Uri) null);
            keyIntent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
            if (needWakeLock) {
                keyIntent.putExtra(EXTRA_WAKELOCK_ACQUIRED, WAKELOCK_RELEASE_ON_FINISHED);
            }
            MediaSessionService.this.getContext().sendOrderedBroadcastAsUser(keyIntent, UserHandle.ALL, null, this.mKeyEventDone, MediaSessionService.this.mHandler, -1, null, null);
        }

        private void startVoiceInput(boolean needWakeLock) {
            Intent voiceIntent;
            PowerManager pm = (PowerManager) MediaSessionService.this.getContext().getSystemService("power");
            boolean isLocked = MediaSessionService.this.mKeyguardManager != null && MediaSessionService.this.mKeyguardManager.isKeyguardLocked();
            if (!isLocked && pm.isScreenOn()) {
                voiceIntent = new Intent("android.speech.action.WEB_SEARCH");
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_WEB_SEARCH");
            } else {
                voiceIntent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
                voiceIntent.putExtra("android.speech.extras.EXTRA_SECURE", isLocked && MediaSessionService.this.mKeyguardManager.isKeyguardSecure());
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_VOICE_SEARCH_HANDS_FREE");
            }
            if (needWakeLock) {
                MediaSessionService.this.mMediaEventWakeLock.acquire();
            }
            if (voiceIntent != null) {
                try {
                    try {
                        voiceIntent.setFlags(276824064);
                        MediaSessionService.this.getContext().startActivityAsUser(voiceIntent, UserHandle.CURRENT);
                    } catch (ActivityNotFoundException e) {
                        Log.w(MediaSessionService.TAG, "No activity for search: " + e);
                        if (needWakeLock) {
                            MediaSessionService.this.mMediaEventWakeLock.release();
                            return;
                        }
                        return;
                    }
                } catch (Throwable th) {
                    if (needWakeLock) {
                        MediaSessionService.this.mMediaEventWakeLock.release();
                    }
                    throw th;
                }
            }
            if (needWakeLock) {
                MediaSessionService.this.mMediaEventWakeLock.release();
            }
        }

        private boolean isVoiceKey(int keyCode) {
            return keyCode == 79;
        }

        private boolean isUserSetupComplete() {
            return Settings.Secure.getIntForUser(MediaSessionService.this.getContext().getContentResolver(), "user_setup_complete", 0, -2) != 0;
        }

        private boolean isValidLocalStreamType(int streamType) {
            return streamType >= 0 && streamType <= 5;
        }

        class KeyEventWakeLockReceiver extends ResultReceiver implements Runnable, PendingIntent.OnFinished {
            private final Handler mHandler;
            private int mLastTimeoutId;
            private int mRefCount;

            public KeyEventWakeLockReceiver(Handler handler) {
                super(handler);
                this.mRefCount = 0;
                this.mLastTimeoutId = 0;
                this.mHandler = handler;
            }

            public void onTimeout() {
                synchronized (MediaSessionService.this.mLock) {
                    if (this.mRefCount != 0) {
                        this.mLastTimeoutId++;
                        this.mRefCount = 0;
                        releaseWakeLockLocked();
                    }
                }
            }

            public void aquireWakeLockLocked() {
                if (this.mRefCount == 0) {
                    MediaSessionService.this.mMediaEventWakeLock.acquire();
                }
                this.mRefCount++;
                this.mHandler.removeCallbacks(this);
                this.mHandler.postDelayed(this, 5000L);
            }

            @Override
            public void run() {
                onTimeout();
            }

            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode >= this.mLastTimeoutId) {
                    synchronized (MediaSessionService.this.mLock) {
                        if (this.mRefCount > 0) {
                            this.mRefCount--;
                            if (this.mRefCount == 0) {
                                releaseWakeLockLocked();
                            }
                        }
                    }
                }
            }

            private void releaseWakeLockLocked() {
                MediaSessionService.this.mMediaEventWakeLock.release();
                this.mHandler.removeCallbacks(this);
            }

            @Override
            public void onSendFinished(PendingIntent pendingIntent, Intent intent, int resultCode, String resultData, Bundle resultExtras) {
                onReceiveResult(resultCode, null);
            }
        }
    }

    final class MessageHandler extends Handler {
        private static final int MSG_SESSIONS_CHANGED = 1;

        MessageHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MediaSessionService.this.pushSessionsChanged(msg.arg1);
                    break;
            }
        }

        public void post(int what, int arg1, int arg2) {
            obtainMessage(what, arg1, arg2).sendToTarget();
        }
    }
}
