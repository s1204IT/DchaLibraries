package com.android.server.media;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.INotificationManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.IRemoteVolumeController;
import android.media.ISessionTokensListener;
import android.media.MediaController2;
import android.media.SessionToken2;
import android.media.session.IActiveSessionsListener;
import android.media.session.ICallback;
import android.media.session.IOnMediaKeyListener;
import android.media.session.IOnVolumeKeyLongPressListener;
import android.media.session.ISession;
import android.media.session.ISessionCallback;
import android.media.session.ISessionManager;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemService;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.media.AudioPlayerStateMonitor;
import com.android.server.media.MediaSessionStack;
import com.android.server.pm.Settings;
import com.android.server.wm.WindowManagerService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MediaSessionService extends SystemService implements Watchdog.Monitor {
    static final boolean DEBUG;
    private static final boolean DEBUG_KEY_EVENT = true;
    private static final int MEDIA_KEY_LISTENER_TIMEOUT = 1000;
    private static final String TAG = "MediaSessionService";
    static final boolean USE_MEDIA2_APIS = false;
    private static final int WAKELOCK_TIMEOUT = 5000;
    private AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    private IAudioService mAudioService;
    private ContentResolver mContentResolver;
    private FullUserRecord mCurrentFullUserRecord;
    private final SparseIntArray mFullUserIds;
    private MediaSessionRecord mGlobalPrioritySession;
    private final MessageHandler mHandler;
    private boolean mHasFeatureLeanback;
    private KeyguardManager mKeyguardManager;
    private final Object mLock;
    private final int mLongPressTimeout;
    private final PowerManager.WakeLock mMediaEventWakeLock;
    private final INotificationManager mNotificationManager;
    private final IPackageManager mPackageManager;
    private IRemoteVolumeController mRvc;
    private final SessionManagerImpl mSessionManagerImpl;
    private final Map<SessionToken2, MediaController2> mSessionRecords;
    private final List<SessionTokensListenerRecord> mSessionTokensListeners;
    private final ArrayList<SessionsListenerRecord> mSessionsListeners;
    private SettingsObserver mSettingsObserver;
    private final SparseArray<FullUserRecord> mUserRecords;

    static {
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
    }

    public MediaSessionService(Context context) {
        super(context);
        this.mFullUserIds = new SparseIntArray();
        this.mUserRecords = new SparseArray<>();
        this.mSessionsListeners = new ArrayList<>();
        this.mLock = new Object();
        this.mHandler = new MessageHandler();
        this.mSessionRecords = new ArrayMap();
        this.mSessionTokensListeners = new ArrayList();
        this.mSessionManagerImpl = new SessionManagerImpl();
        this.mMediaEventWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "handleMediaEvent");
        this.mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
        this.mNotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        this.mPackageManager = AppGlobals.getPackageManager();
    }

    @Override
    public void onStart() {
        publishBinderService("media_session", this.mSessionManagerImpl);
        Watchdog.getInstance().addMonitor(this);
        this.mKeyguardManager = (KeyguardManager) getContext().getSystemService("keyguard");
        this.mAudioService = getAudioService();
        this.mAudioPlayerStateMonitor = AudioPlayerStateMonitor.getInstance();
        this.mAudioPlayerStateMonitor.registerListener(new AudioPlayerStateMonitor.OnAudioPlayerActiveStateChangedListener() {
            @Override
            public final void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
                MediaSessionService.lambda$onStart$0(this.f$0, audioPlaybackConfiguration, z);
            }
        }, null);
        this.mAudioPlayerStateMonitor.registerSelfIntoAudioServiceIfNeeded(this.mAudioService);
        this.mContentResolver = getContext().getContentResolver();
        this.mSettingsObserver = new SettingsObserver();
        this.mSettingsObserver.observe();
        this.mHasFeatureLeanback = getContext().getPackageManager().hasSystemFeature("android.software.leanback");
        updateUser();
        registerPackageBroadcastReceivers();
        buildMediaSessionService2List();
    }

    public static void lambda$onStart$0(MediaSessionService mediaSessionService, AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
        if (z || !audioPlaybackConfiguration.isActive() || audioPlaybackConfiguration.getPlayerType() == 3) {
            return;
        }
        synchronized (mediaSessionService.mLock) {
            FullUserRecord fullUserRecordLocked = mediaSessionService.getFullUserRecordLocked(UserHandle.getUserId(audioPlaybackConfiguration.getClientUid()));
            if (fullUserRecordLocked != null) {
                fullUserRecordLocked.mPriorityStack.updateMediaButtonSessionIfNeeded();
            }
        }
    }

    private IAudioService getAudioService() {
        return IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
    }

    private boolean isGlobalPriorityActiveLocked() {
        return this.mGlobalPrioritySession != null && this.mGlobalPrioritySession.isActive();
    }

    public void updateSession(MediaSessionRecord mediaSessionRecord) {
        synchronized (this.mLock) {
            FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(mediaSessionRecord.getUserId());
            if (fullUserRecordLocked == null) {
                Log.w(TAG, "Unknown session updated. Ignoring.");
                return;
            }
            if ((mediaSessionRecord.getFlags() & 65536) != 0) {
                Log.d(TAG, "Global priority session is updated, active=" + mediaSessionRecord.isActive());
                fullUserRecordLocked.pushAddressedPlayerChangedLocked();
            } else {
                if (!fullUserRecordLocked.mPriorityStack.contains(mediaSessionRecord)) {
                    Log.w(TAG, "Unknown session updated. Ignoring.");
                    return;
                }
                fullUserRecordLocked.mPriorityStack.onSessionStateChange(mediaSessionRecord);
            }
            this.mHandler.postSessionsChanged(mediaSessionRecord.getUserId());
        }
    }

    public void setGlobalPrioritySession(MediaSessionRecord mediaSessionRecord) {
        synchronized (this.mLock) {
            FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(mediaSessionRecord.getUserId());
            if (this.mGlobalPrioritySession != mediaSessionRecord) {
                Log.d(TAG, "Global priority session is changed from " + this.mGlobalPrioritySession + " to " + mediaSessionRecord);
                this.mGlobalPrioritySession = mediaSessionRecord;
                if (fullUserRecordLocked != null && fullUserRecordLocked.mPriorityStack.contains(mediaSessionRecord)) {
                    fullUserRecordLocked.mPriorityStack.removeSession(mediaSessionRecord);
                }
            }
        }
    }

    private List<MediaSessionRecord> getActiveSessionsLocked(int i) {
        ArrayList arrayList = new ArrayList();
        if (i == -1) {
            int size = this.mUserRecords.size();
            for (int i2 = 0; i2 < size; i2++) {
                arrayList.addAll(this.mUserRecords.valueAt(i2).mPriorityStack.getActiveSessions(i));
            }
        } else {
            FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(i);
            if (fullUserRecordLocked == null) {
                Log.w(TAG, "getSessions failed. Unknown user " + i);
                return arrayList;
            }
            arrayList.addAll(fullUserRecordLocked.mPriorityStack.getActiveSessions(i));
        }
        if (isGlobalPriorityActiveLocked() && (i == -1 || i == this.mGlobalPrioritySession.getUserId())) {
            arrayList.add(0, this.mGlobalPrioritySession);
        }
        return arrayList;
    }

    public void notifyRemoteVolumeChanged(int i, MediaSessionRecord mediaSessionRecord) {
        if (this.mRvc == null || !mediaSessionRecord.isActive()) {
            return;
        }
        try {
            this.mRvc.remoteVolumeChanged(mediaSessionRecord.getControllerBinder(), i);
        } catch (Exception e) {
            Log.wtf(TAG, "Error sending volume change to system UI.", e);
        }
    }

    public void onSessionPlaystateChanged(MediaSessionRecord mediaSessionRecord, int i, int i2) {
        synchronized (this.mLock) {
            FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(mediaSessionRecord.getUserId());
            if (fullUserRecordLocked != null && fullUserRecordLocked.mPriorityStack.contains(mediaSessionRecord)) {
                fullUserRecordLocked.mPriorityStack.onPlaystateChanged(mediaSessionRecord, i, i2);
                return;
            }
            Log.d(TAG, "Unknown session changed playback state. Ignoring.");
        }
    }

    public void onSessionPlaybackTypeChanged(MediaSessionRecord mediaSessionRecord) {
        synchronized (this.mLock) {
            FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(mediaSessionRecord.getUserId());
            if (fullUserRecordLocked != null && fullUserRecordLocked.mPriorityStack.contains(mediaSessionRecord)) {
                pushRemoteVolumeUpdateLocked(mediaSessionRecord.getUserId());
                return;
            }
            Log.d(TAG, "Unknown session changed playback type. Ignoring.");
        }
    }

    @Override
    public void onStartUser(int i) {
        if (DEBUG) {
            Log.d(TAG, "onStartUser: " + i);
        }
        updateUser();
    }

    @Override
    public void onSwitchUser(int i) {
        if (DEBUG) {
            Log.d(TAG, "onSwitchUser: " + i);
        }
        updateUser();
    }

    @Override
    public void onStopUser(int i) {
        if (DEBUG) {
            Log.d(TAG, "onStopUser: " + i);
        }
        synchronized (this.mLock) {
            FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(i);
            if (fullUserRecordLocked != null) {
                if (fullUserRecordLocked.mFullUserId == i) {
                    fullUserRecordLocked.destroySessionsForUserLocked(-1);
                    this.mUserRecords.remove(i);
                } else {
                    fullUserRecordLocked.destroySessionsForUserLocked(i);
                }
            }
            updateUser();
        }
    }

    @Override
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    protected void enforcePhoneStatePermission(int i, int i2) {
        if (getContext().checkPermission("android.permission.MODIFY_PHONE_STATE", i, i2) != 0) {
            throw new SecurityException("Must hold the MODIFY_PHONE_STATE permission.");
        }
    }

    void sessionDied(MediaSessionRecord mediaSessionRecord) {
        synchronized (this.mLock) {
            destroySessionLocked(mediaSessionRecord);
        }
    }

    void destroySession(MediaSessionRecord mediaSessionRecord) {
        synchronized (this.mLock) {
            destroySessionLocked(mediaSessionRecord);
        }
    }

    private void updateUser() {
        synchronized (this.mLock) {
            UserManager userManager = (UserManager) getContext().getSystemService("user");
            this.mFullUserIds.clear();
            List<UserInfo> users = userManager.getUsers();
            if (users != null) {
                for (UserInfo userInfo : users) {
                    if (userInfo.isManagedProfile()) {
                        this.mFullUserIds.put(userInfo.id, userInfo.profileGroupId);
                    } else {
                        this.mFullUserIds.put(userInfo.id, userInfo.id);
                        if (this.mUserRecords.get(userInfo.id) == null) {
                            this.mUserRecords.put(userInfo.id, new FullUserRecord(userInfo.id));
                        }
                    }
                }
            }
            int currentUser = ActivityManager.getCurrentUser();
            this.mCurrentFullUserRecord = this.mUserRecords.get(currentUser);
            if (this.mCurrentFullUserRecord == null) {
                Log.w(TAG, "Cannot find FullUserInfo for the current user " + currentUser);
                this.mCurrentFullUserRecord = new FullUserRecord(currentUser);
                this.mUserRecords.put(currentUser, this.mCurrentFullUserRecord);
            }
            this.mFullUserIds.put(currentUser, currentUser);
        }
    }

    private void updateActiveSessionListeners() {
        synchronized (this.mLock) {
            for (int size = this.mSessionsListeners.size() - 1; size >= 0; size--) {
                SessionsListenerRecord sessionsListenerRecord = this.mSessionsListeners.get(size);
                try {
                    enforceMediaPermissions(sessionsListenerRecord.mComponentName, sessionsListenerRecord.mPid, sessionsListenerRecord.mUid, sessionsListenerRecord.mUserId);
                } catch (SecurityException e) {
                    Log.i(TAG, "ActiveSessionsListener " + sessionsListenerRecord.mComponentName + " is no longer authorized. Disconnecting.");
                    this.mSessionsListeners.remove(size);
                    try {
                        sessionsListenerRecord.mListener.onActiveSessionsChanged(new ArrayList());
                    } catch (Exception e2) {
                    }
                }
            }
        }
    }

    private void destroySessionLocked(MediaSessionRecord mediaSessionRecord) {
        if (DEBUG) {
            Log.d(TAG, "Destroying " + mediaSessionRecord);
        }
        FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(mediaSessionRecord.getUserId());
        if (this.mGlobalPrioritySession == mediaSessionRecord) {
            this.mGlobalPrioritySession = null;
            if (mediaSessionRecord.isActive() && fullUserRecordLocked != null) {
                fullUserRecordLocked.pushAddressedPlayerChangedLocked();
            }
        } else if (fullUserRecordLocked != null) {
            fullUserRecordLocked.mPriorityStack.removeSession(mediaSessionRecord);
        }
        try {
            mediaSessionRecord.getCallback().asBinder().unlinkToDeath(mediaSessionRecord, 0);
        } catch (Exception e) {
        }
        mediaSessionRecord.onDestroy();
        this.mHandler.postSessionsChanged(mediaSessionRecord.getUserId());
    }

    private void registerPackageBroadcastReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGES_SUSPENDED");
        intentFilter.addAction("android.intent.action.PACKAGES_UNSUSPENDED");
        intentFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
        intentFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        getContext().registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean booleanExtra;
                if (intent.getIntExtra("android.intent.extra.user_handle", -10000) == -10000) {
                    Log.w(MediaSessionService.TAG, "Intent broadcast does not contain user handle: " + intent);
                    return;
                }
                booleanExtra = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, "Received change in packages, intent=" + intent);
                }
                switch (intent.getAction()) {
                    case "android.intent.action.PACKAGE_ADDED":
                    case "android.intent.action.PACKAGE_REMOVED":
                    case "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE":
                    case "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE":
                        if (booleanExtra) {
                            return;
                        }
                        break;
                    case "android.intent.action.PACKAGE_CHANGED":
                    case "android.intent.action.PACKAGES_SUSPENDED":
                    case "android.intent.action.PACKAGES_UNSUSPENDED":
                    case "android.intent.action.PACKAGE_REPLACED":
                        break;
                    default:
                        return;
                }
                MediaSessionService.this.buildMediaSessionService2List();
            }
        }, UserHandle.ALL, intentFilter, null, BackgroundThread.getHandler());
    }

    private void buildMediaSessionService2List() {
    }

    private void enforcePackageName(String str, int i) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("packageName may not be empty");
        }
        for (String str2 : getContext().getPackageManager().getPackagesForUid(i)) {
            if (str.equals(str2)) {
                return;
            }
        }
        throw new IllegalArgumentException("packageName is not owned by the calling process");
    }

    private void enforceMediaPermissions(ComponentName componentName, int i, int i2, int i3) {
        if (!isCurrentVolumeController(i, i2) && getContext().checkPermission("android.permission.MEDIA_CONTENT_CONTROL", i, i2) != 0 && !isEnabledNotificationListener(componentName, UserHandle.getUserId(i2), i3)) {
            throw new SecurityException("Missing permission to control media.");
        }
    }

    private boolean isCurrentVolumeController(int i, int i2) {
        return getContext().checkPermission("android.permission.STATUS_BAR_SERVICE", i, i2) == 0;
    }

    private void enforceSystemUiPermission(String str, int i, int i2) {
        if (!isCurrentVolumeController(i, i2)) {
            throw new SecurityException("Only system ui may " + str);
        }
    }

    private boolean isEnabledNotificationListener(ComponentName componentName, int i, int i2) {
        if (i != i2) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "Checking if enabled notification listener " + componentName);
        }
        if (componentName != null) {
            try {
                return this.mNotificationManager.isNotificationListenerAccessGrantedForUser(componentName, i);
            } catch (RemoteException e) {
                Log.w(TAG, "Dead NotificationManager in isEnabledNotificationListener", e);
            }
        }
        return false;
    }

    private MediaSessionRecord createSessionInternal(int i, int i2, int i3, String str, ISessionCallback iSessionCallback, String str2) throws RemoteException {
        MediaSessionRecord mediaSessionRecordCreateSessionLocked;
        synchronized (this.mLock) {
            mediaSessionRecordCreateSessionLocked = createSessionLocked(i, i2, i3, str, iSessionCallback, str2);
        }
        return mediaSessionRecordCreateSessionLocked;
    }

    private MediaSessionRecord createSessionLocked(int i, int i2, int i3, String str, ISessionCallback iSessionCallback, String str2) {
        FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(i3);
        if (fullUserRecordLocked == null) {
            Log.wtf(TAG, "Request from invalid user: " + i3);
            throw new RuntimeException("Session request from invalid user.");
        }
        MediaSessionRecord mediaSessionRecord = new MediaSessionRecord(i, i2, i3, str, iSessionCallback, str2, this, this.mHandler.getLooper());
        try {
            iSessionCallback.asBinder().linkToDeath(mediaSessionRecord, 0);
            fullUserRecordLocked.mPriorityStack.addSession(mediaSessionRecord);
            this.mHandler.postSessionsChanged(i3);
            if (DEBUG) {
                Log.d(TAG, "Created session for " + str + " with tag " + str2);
            }
            return mediaSessionRecord;
        } catch (RemoteException e) {
            throw new RuntimeException("Media Session owner died prematurely.", e);
        }
    }

    private int findIndexOfSessionsListenerLocked(IActiveSessionsListener iActiveSessionsListener) {
        for (int size = this.mSessionsListeners.size() - 1; size >= 0; size--) {
            if (this.mSessionsListeners.get(size).mListener.asBinder() == iActiveSessionsListener.asBinder()) {
                return size;
            }
        }
        return -1;
    }

    private void pushSessionsChanged(int i) {
        synchronized (this.mLock) {
            if (getFullUserRecordLocked(i) == null) {
                Log.w(TAG, "pushSessionsChanged failed. No user with id=" + i);
                return;
            }
            List<MediaSessionRecord> activeSessionsLocked = getActiveSessionsLocked(i);
            int size = activeSessionsLocked.size();
            ArrayList arrayList = new ArrayList();
            for (int i2 = 0; i2 < size; i2++) {
                arrayList.add(new MediaSession.Token(activeSessionsLocked.get(i2).getControllerBinder()));
            }
            pushRemoteVolumeUpdateLocked(i);
            for (int size2 = this.mSessionsListeners.size() - 1; size2 >= 0; size2--) {
                SessionsListenerRecord sessionsListenerRecord = this.mSessionsListeners.get(size2);
                if (sessionsListenerRecord.mUserId == -1 || sessionsListenerRecord.mUserId == i) {
                    try {
                        sessionsListenerRecord.mListener.onActiveSessionsChanged(arrayList);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Dead ActiveSessionsListener in pushSessionsChanged, removing", e);
                        this.mSessionsListeners.remove(size2);
                    }
                }
            }
        }
    }

    private void pushRemoteVolumeUpdateLocked(int i) {
        if (this.mRvc != null) {
            try {
                FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(i);
                if (fullUserRecordLocked == null) {
                    Log.w(TAG, "pushRemoteVolumeUpdateLocked failed. No user with id=" + i);
                    return;
                }
                MediaSessionRecord defaultRemoteSession = fullUserRecordLocked.mPriorityStack.getDefaultRemoteSession(i);
                this.mRvc.updateRemoteController(defaultRemoteSession == null ? null : defaultRemoteSession.getControllerBinder());
            } catch (RemoteException e) {
                Log.wtf(TAG, "Error sending default remote volume to sys ui.", e);
            }
        }
    }

    public void onMediaButtonReceiverChanged(MediaSessionRecord mediaSessionRecord) {
        synchronized (this.mLock) {
            FullUserRecord fullUserRecordLocked = getFullUserRecordLocked(mediaSessionRecord.getUserId());
            MediaSessionRecord mediaButtonSession = fullUserRecordLocked.mPriorityStack.getMediaButtonSession();
            if (mediaSessionRecord == mediaButtonSession) {
                fullUserRecordLocked.rememberMediaButtonReceiverLocked(mediaButtonSession);
            }
        }
    }

    private String getCallingPackageName(int i) {
        String[] packagesForUid = getContext().getPackageManager().getPackagesForUid(i);
        if (packagesForUid != null && packagesForUid.length > 0) {
            return packagesForUid[0];
        }
        return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    }

    private void dispatchVolumeKeyLongPressLocked(KeyEvent keyEvent) {
        if (this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener == null) {
            return;
        }
        try {
            this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener.onVolumeKeyLongPress(keyEvent);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to send " + keyEvent + " to volume key long-press listener");
        }
    }

    private FullUserRecord getFullUserRecordLocked(int i) {
        int i2 = this.mFullUserIds.get(i, -1);
        if (i2 < 0) {
            return null;
        }
        return this.mUserRecords.get(i2);
    }

    void destroySession2Internal(SessionToken2 sessionToken2) {
        synchronized (this.mLock) {
            if (sessionToken2.getType() == 0 ? false | removeSessionRecordLocked(sessionToken2) : false | addSessionRecordLocked(sessionToken2)) {
                postSessionTokensUpdated(UserHandle.getUserId(sessionToken2.getUid()));
            }
        }
    }

    final class FullUserRecord implements MediaSessionStack.OnMediaButtonSessionChangedListener {
        private static final String COMPONENT_NAME_USER_ID_DELIM = ",";
        private ICallback mCallback;
        private final int mFullUserId;
        private boolean mInitialDownMusicOnly;
        private KeyEvent mInitialDownVolumeKeyEvent;
        private int mInitialDownVolumeStream;
        private PendingIntent mLastMediaButtonReceiver;
        private IOnMediaKeyListener mOnMediaKeyListener;
        private int mOnMediaKeyListenerUid;
        private IOnVolumeKeyLongPressListener mOnVolumeKeyLongPressListener;
        private int mOnVolumeKeyLongPressListenerUid;
        private final MediaSessionStack mPriorityStack;
        private ComponentName mRestoredMediaButtonReceiver;
        private int mRestoredMediaButtonReceiverUserId;

        public FullUserRecord(int i) {
            String[] strArrSplit;
            this.mFullUserId = i;
            this.mPriorityStack = new MediaSessionStack(MediaSessionService.this.mAudioPlayerStateMonitor, this);
            String stringForUser = Settings.Secure.getStringForUser(MediaSessionService.this.mContentResolver, "media_button_receiver", this.mFullUserId);
            if (stringForUser == null || (strArrSplit = stringForUser.split(COMPONENT_NAME_USER_ID_DELIM)) == null || strArrSplit.length != 2) {
                return;
            }
            this.mRestoredMediaButtonReceiver = ComponentName.unflattenFromString(strArrSplit[0]);
            this.mRestoredMediaButtonReceiverUserId = Integer.parseInt(strArrSplit[1]);
        }

        public void destroySessionsForUserLocked(int i) {
            Iterator<MediaSessionRecord> it = this.mPriorityStack.getPriorityList(false, i).iterator();
            while (it.hasNext()) {
                MediaSessionService.this.destroySessionLocked(it.next());
            }
        }

        public void dumpLocked(PrintWriter printWriter, String str) {
            printWriter.print(str + "Record for full_user=" + this.mFullUserId);
            int size = MediaSessionService.this.mFullUserIds.size();
            for (int i = 0; i < size; i++) {
                if (MediaSessionService.this.mFullUserIds.keyAt(i) != MediaSessionService.this.mFullUserIds.valueAt(i) && MediaSessionService.this.mFullUserIds.valueAt(i) == this.mFullUserId) {
                    printWriter.print(", profile_user=" + MediaSessionService.this.mFullUserIds.keyAt(i));
                }
            }
            printWriter.println();
            String str2 = str + "  ";
            printWriter.println(str2 + "Volume key long-press listener: " + this.mOnVolumeKeyLongPressListener);
            printWriter.println(str2 + "Volume key long-press listener package: " + MediaSessionService.this.getCallingPackageName(this.mOnVolumeKeyLongPressListenerUid));
            printWriter.println(str2 + "Media key listener: " + this.mOnMediaKeyListener);
            printWriter.println(str2 + "Media key listener package: " + MediaSessionService.this.getCallingPackageName(this.mOnMediaKeyListenerUid));
            printWriter.println(str2 + "Callback: " + this.mCallback);
            printWriter.println(str2 + "Last MediaButtonReceiver: " + this.mLastMediaButtonReceiver);
            printWriter.println(str2 + "Restored MediaButtonReceiver: " + this.mRestoredMediaButtonReceiver);
            this.mPriorityStack.dump(printWriter, str2);
        }

        @Override
        public void onMediaButtonSessionChanged(MediaSessionRecord mediaSessionRecord, MediaSessionRecord mediaSessionRecord2) {
            Log.d(MediaSessionService.TAG, "Media button session is changed to " + mediaSessionRecord2);
            synchronized (MediaSessionService.this.mLock) {
                if (mediaSessionRecord != null) {
                    try {
                        MediaSessionService.this.mHandler.postSessionsChanged(mediaSessionRecord.getUserId());
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                if (mediaSessionRecord2 != null) {
                    rememberMediaButtonReceiverLocked(mediaSessionRecord2);
                    MediaSessionService.this.mHandler.postSessionsChanged(mediaSessionRecord2.getUserId());
                }
                pushAddressedPlayerChangedLocked();
            }
        }

        public void rememberMediaButtonReceiverLocked(MediaSessionRecord mediaSessionRecord) {
            ComponentName component;
            PendingIntent mediaButtonReceiver = mediaSessionRecord.getMediaButtonReceiver();
            this.mLastMediaButtonReceiver = mediaButtonReceiver;
            this.mRestoredMediaButtonReceiver = null;
            String strFlattenToString = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (mediaButtonReceiver != null && (component = mediaButtonReceiver.getIntent().getComponent()) != null && mediaSessionRecord.getPackageName().equals(component.getPackageName())) {
                strFlattenToString = component.flattenToString();
            }
            Settings.Secure.putStringForUser(MediaSessionService.this.mContentResolver, "media_button_receiver", strFlattenToString + COMPONENT_NAME_USER_ID_DELIM + mediaSessionRecord.getUserId(), this.mFullUserId);
        }

        private void pushAddressedPlayerChangedLocked() {
            if (this.mCallback == null) {
                return;
            }
            try {
                MediaSessionRecord mediaButtonSessionLocked = getMediaButtonSessionLocked();
                if (mediaButtonSessionLocked == null) {
                    if (MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver == null) {
                        if (MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver != null) {
                            this.mCallback.onAddressedPlayerChangedToMediaButtonReceiver(MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver);
                        }
                    } else {
                        this.mCallback.onAddressedPlayerChangedToMediaButtonReceiver(MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver.getIntent().getComponent());
                    }
                } else {
                    this.mCallback.onAddressedPlayerChangedToMediaSession(new MediaSession.Token(mediaButtonSessionLocked.getControllerBinder()));
                }
            } catch (RemoteException e) {
                Log.w(MediaSessionService.TAG, "Failed to pushAddressedPlayerChangedLocked", e);
            }
        }

        private MediaSessionRecord getMediaButtonSessionLocked() {
            return MediaSessionService.this.isGlobalPriorityActiveLocked() ? MediaSessionService.this.mGlobalPrioritySession : this.mPriorityStack.getMediaButtonSession();
        }
    }

    final class SessionsListenerRecord implements IBinder.DeathRecipient {
        private final ComponentName mComponentName;
        private final IActiveSessionsListener mListener;
        private final int mPid;
        private final int mUid;
        private final int mUserId;

        public SessionsListenerRecord(IActiveSessionsListener iActiveSessionsListener, ComponentName componentName, int i, int i2, int i3) {
            this.mListener = iActiveSessionsListener;
            this.mComponentName = componentName;
            this.mUserId = i;
            this.mPid = i2;
            this.mUid = i3;
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
        public void onChange(boolean z, Uri uri) {
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

        public ISession createSession(String str, ISessionCallback iSessionCallback, String str2, int i) throws RemoteException {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforcePackageName(str, callingUid);
                int iHandleIncomingUser = ActivityManager.handleIncomingUser(callingPid, callingUid, i, false, true, "createSession", str);
                if (iSessionCallback == null) {
                    throw new IllegalArgumentException("Controller callback cannot be null");
                }
                return MediaSessionService.this.createSessionInternal(callingPid, callingUid, iHandleIncomingUser, str, iSessionCallback, str2).getSessionBinder();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public List<IBinder> getSessions(ComponentName componentName, int i) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                int iVerifySessionsRequest = verifySessionsRequest(componentName, i, callingPid, callingUid);
                ArrayList arrayList = new ArrayList();
                synchronized (MediaSessionService.this.mLock) {
                    Iterator it = MediaSessionService.this.getActiveSessionsLocked(iVerifySessionsRequest).iterator();
                    while (it.hasNext()) {
                        arrayList.add(((MediaSessionRecord) it.next()).getControllerBinder().asBinder());
                    }
                }
                return arrayList;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void addSessionsListener(IActiveSessionsListener iActiveSessionsListener, ComponentName componentName, int i) throws RemoteException {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                int iVerifySessionsRequest = verifySessionsRequest(componentName, i, callingPid, callingUid);
                synchronized (MediaSessionService.this.mLock) {
                    if (MediaSessionService.this.findIndexOfSessionsListenerLocked(iActiveSessionsListener) != -1) {
                        Log.w(MediaSessionService.TAG, "ActiveSessionsListener is already added, ignoring");
                        return;
                    }
                    SessionsListenerRecord sessionsListenerRecord = MediaSessionService.this.new SessionsListenerRecord(iActiveSessionsListener, componentName, iVerifySessionsRequest, callingPid, callingUid);
                    try {
                        iActiveSessionsListener.asBinder().linkToDeath(sessionsListenerRecord, 0);
                        MediaSessionService.this.mSessionsListeners.add(sessionsListenerRecord);
                    } catch (RemoteException e) {
                        Log.e(MediaSessionService.TAG, "ActiveSessionsListener is dead, ignoring it", e);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void removeSessionsListener(IActiveSessionsListener iActiveSessionsListener) throws RemoteException {
            synchronized (MediaSessionService.this.mLock) {
                int iFindIndexOfSessionsListenerLocked = MediaSessionService.this.findIndexOfSessionsListenerLocked(iActiveSessionsListener);
                if (iFindIndexOfSessionsListenerLocked != -1) {
                    SessionsListenerRecord sessionsListenerRecord = (SessionsListenerRecord) MediaSessionService.this.mSessionsListeners.remove(iFindIndexOfSessionsListenerLocked);
                    try {
                        sessionsListenerRecord.mListener.asBinder().unlinkToDeath(sessionsListenerRecord, 0);
                    } catch (Exception e) {
                    }
                }
            }
        }

        public void dispatchMediaKeyEvent(String str, boolean z, KeyEvent keyEvent, boolean z2) {
            String str2;
            boolean z3;
            if (keyEvent == null || !KeyEvent.isMediaKey(keyEvent.getKeyCode())) {
                Log.w(MediaSessionService.TAG, "Attempted to dispatch null or non-media key event.");
                return;
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.DEBUG) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("dispatchMediaKeyEvent, pkg=");
                    str2 = str;
                    sb.append(str2);
                    sb.append(" pid=");
                    sb.append(callingPid);
                    sb.append(", uid=");
                    sb.append(callingUid);
                    sb.append(", asSystem=");
                    z3 = z;
                    sb.append(z3);
                    sb.append(", event=");
                    sb.append(keyEvent);
                    Log.d(MediaSessionService.TAG, sb.toString());
                } else {
                    str2 = str;
                    z3 = z;
                }
                if (!isUserSetupComplete()) {
                    Slog.i(MediaSessionService.TAG, "Not dispatching media key event because user setup is in progress.");
                    return;
                }
                synchronized (MediaSessionService.this.mLock) {
                    boolean zIsGlobalPriorityActiveLocked = MediaSessionService.this.isGlobalPriorityActiveLocked();
                    if (zIsGlobalPriorityActiveLocked && callingUid != 1000) {
                        Slog.i(MediaSessionService.TAG, "Only the system can dispatch media key event to the global priority session.");
                        return;
                    }
                    if (!zIsGlobalPriorityActiveLocked && MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyListener != null) {
                        Log.d(MediaSessionService.TAG, "Send " + keyEvent + " to the media key listener");
                        try {
                            MediaSessionService.this.mCurrentFullUserRecord.mOnMediaKeyListener.onMediaKey(keyEvent, new MediaKeyListenerResultReceiver(str2, callingPid, callingUid, z3, keyEvent, z2));
                            return;
                        } catch (RemoteException e) {
                            Log.w(MediaSessionService.TAG, "Failed to send " + keyEvent + " to the media key listener");
                        }
                    }
                    if (zIsGlobalPriorityActiveLocked || !isVoiceKey(keyEvent.getKeyCode())) {
                        dispatchMediaKeyEventLocked(str, callingPid, callingUid, z, keyEvent, z2);
                    } else {
                        handleVoiceKeyEventLocked(str, callingPid, callingUid, z, keyEvent, z2);
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setCallback(ICallback iCallback) {
            Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (!UserHandle.isSameApp(callingUid, 1002)) {
                    throw new SecurityException("Only Bluetooth service processes can set Callback");
                }
                synchronized (MediaSessionService.this.mLock) {
                    int userId = UserHandle.getUserId(callingUid);
                    final FullUserRecord fullUserRecordLocked = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (fullUserRecordLocked != null && fullUserRecordLocked.mFullUserId == userId) {
                        fullUserRecordLocked.mCallback = iCallback;
                        Log.d(MediaSessionService.TAG, "The callback " + fullUserRecordLocked.mCallback + " is set by " + MediaSessionService.this.getCallingPackageName(callingUid));
                        if (fullUserRecordLocked.mCallback == null) {
                            return;
                        }
                        try {
                            fullUserRecordLocked.mCallback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                                @Override
                                public void binderDied() {
                                    synchronized (MediaSessionService.this.mLock) {
                                        fullUserRecordLocked.mCallback = null;
                                    }
                                }
                            }, 0);
                            fullUserRecordLocked.pushAddressedPlayerChangedLocked();
                        } catch (RemoteException e) {
                            Log.w(MediaSessionService.TAG, "Failed to set callback", e);
                            fullUserRecordLocked.mCallback = null;
                        }
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can set the callback, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setOnVolumeKeyLongPressListener(IOnVolumeKeyLongPressListener iOnVolumeKeyLongPressListener) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.this.getContext().checkPermission("android.permission.SET_VOLUME_KEY_LONG_PRESS_LISTENER", callingPid, callingUid) != 0) {
                    throw new SecurityException("Must hold the SET_VOLUME_KEY_LONG_PRESS_LISTENER permission.");
                }
                synchronized (MediaSessionService.this.mLock) {
                    int userId = UserHandle.getUserId(callingUid);
                    final FullUserRecord fullUserRecordLocked = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (fullUserRecordLocked != null && fullUserRecordLocked.mFullUserId == userId) {
                        if (fullUserRecordLocked.mOnVolumeKeyLongPressListener != null && fullUserRecordLocked.mOnVolumeKeyLongPressListenerUid != callingUid) {
                            Log.w(MediaSessionService.TAG, "The volume key long-press listener cannot be reset by another app , mOnVolumeKeyLongPressListener=" + fullUserRecordLocked.mOnVolumeKeyLongPressListenerUid + ", uid=" + callingUid);
                            return;
                        }
                        fullUserRecordLocked.mOnVolumeKeyLongPressListener = iOnVolumeKeyLongPressListener;
                        fullUserRecordLocked.mOnVolumeKeyLongPressListenerUid = callingUid;
                        Log.d(MediaSessionService.TAG, "The volume key long-press listener " + iOnVolumeKeyLongPressListener + " is set by " + MediaSessionService.this.getCallingPackageName(callingUid));
                        if (fullUserRecordLocked.mOnVolumeKeyLongPressListener != null) {
                            try {
                                fullUserRecordLocked.mOnVolumeKeyLongPressListener.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                                    @Override
                                    public void binderDied() {
                                        synchronized (MediaSessionService.this.mLock) {
                                            fullUserRecordLocked.mOnVolumeKeyLongPressListener = null;
                                        }
                                    }
                                }, 0);
                            } catch (RemoteException e) {
                                Log.w(MediaSessionService.TAG, "Failed to set death recipient " + fullUserRecordLocked.mOnVolumeKeyLongPressListener);
                                fullUserRecordLocked.mOnVolumeKeyLongPressListener = null;
                            }
                        }
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can set the volume key long-press listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setOnMediaKeyListener(IOnMediaKeyListener iOnMediaKeyListener) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.this.getContext().checkPermission("android.permission.SET_MEDIA_KEY_LISTENER", callingPid, callingUid) != 0) {
                    throw new SecurityException("Must hold the SET_MEDIA_KEY_LISTENER permission.");
                }
                synchronized (MediaSessionService.this.mLock) {
                    int userId = UserHandle.getUserId(callingUid);
                    final FullUserRecord fullUserRecordLocked = MediaSessionService.this.getFullUserRecordLocked(userId);
                    if (fullUserRecordLocked != null && fullUserRecordLocked.mFullUserId == userId) {
                        if (fullUserRecordLocked.mOnMediaKeyListener != null && fullUserRecordLocked.mOnMediaKeyListenerUid != callingUid) {
                            Log.w(MediaSessionService.TAG, "The media key listener cannot be reset by another app. , mOnMediaKeyListenerUid=" + fullUserRecordLocked.mOnMediaKeyListenerUid + ", uid=" + callingUid);
                            return;
                        }
                        fullUserRecordLocked.mOnMediaKeyListener = iOnMediaKeyListener;
                        fullUserRecordLocked.mOnMediaKeyListenerUid = callingUid;
                        Log.d(MediaSessionService.TAG, "The media key listener " + fullUserRecordLocked.mOnMediaKeyListener + " is set by " + MediaSessionService.this.getCallingPackageName(callingUid));
                        if (fullUserRecordLocked.mOnMediaKeyListener != null) {
                            try {
                                fullUserRecordLocked.mOnMediaKeyListener.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                                    @Override
                                    public void binderDied() {
                                        synchronized (MediaSessionService.this.mLock) {
                                            fullUserRecordLocked.mOnMediaKeyListener = null;
                                        }
                                    }
                                }, 0);
                            } catch (RemoteException e) {
                                Log.w(MediaSessionService.TAG, "Failed to set death recipient " + fullUserRecordLocked.mOnMediaKeyListener);
                                fullUserRecordLocked.mOnMediaKeyListener = null;
                            }
                        }
                        return;
                    }
                    Log.w(MediaSessionService.TAG, "Only the full user can set the media key listener, userId=" + userId);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void dispatchVolumeKeyEvent(String str, boolean z, KeyEvent keyEvent, int i, boolean z2) {
            if (keyEvent == null || (keyEvent.getKeyCode() != 24 && keyEvent.getKeyCode() != 25 && keyEvent.getKeyCode() != 164)) {
                Log.w(MediaSessionService.TAG, "Attempted to dispatch null or non-volume key event.");
                return;
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            Log.d(MediaSessionService.TAG, "dispatchVolumeKeyEvent, pkg=" + str + ", pid=" + callingPid + ", uid=" + callingUid + ", asSystem=" + z + ", event=" + keyEvent);
            try {
                synchronized (MediaSessionService.this.mLock) {
                    if (MediaSessionService.this.isGlobalPriorityActiveLocked() || MediaSessionService.this.mCurrentFullUserRecord.mOnVolumeKeyLongPressListener == null) {
                        dispatchVolumeKeyEventLocked(str, callingPid, callingUid, z, keyEvent, i, z2);
                    } else if (keyEvent.getAction() != 0) {
                        MediaSessionService.this.mHandler.removeMessages(2);
                        if (MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent == null || MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent.getDownTime() != keyEvent.getDownTime()) {
                            MediaSessionService.this.dispatchVolumeKeyLongPressLocked(keyEvent);
                        } else {
                            dispatchVolumeKeyEventLocked(str, callingPid, callingUid, z, MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent, MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeStream, MediaSessionService.this.mCurrentFullUserRecord.mInitialDownMusicOnly);
                            dispatchVolumeKeyEventLocked(str, callingPid, callingUid, z, keyEvent, i, z2);
                        }
                    } else {
                        if (keyEvent.getRepeatCount() == 0) {
                            MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent = KeyEvent.obtain(keyEvent);
                            MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeStream = i;
                            MediaSessionService.this.mCurrentFullUserRecord.mInitialDownMusicOnly = z2;
                            MediaSessionService.this.mHandler.sendMessageDelayed(MediaSessionService.this.mHandler.obtainMessage(2, MediaSessionService.this.mCurrentFullUserRecord.mFullUserId, 0), MediaSessionService.this.mLongPressTimeout);
                        }
                        if (keyEvent.getRepeatCount() > 0 || keyEvent.isLongPress()) {
                            MediaSessionService.this.mHandler.removeMessages(2);
                            if (MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent != null) {
                                MediaSessionService.this.dispatchVolumeKeyLongPressLocked(MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent);
                                MediaSessionService.this.mCurrentFullUserRecord.mInitialDownVolumeKeyEvent = null;
                            }
                            MediaSessionService.this.dispatchVolumeKeyLongPressLocked(keyEvent);
                        }
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void dispatchVolumeKeyEventLocked(String str, int i, int i2, boolean z, KeyEvent keyEvent, int i3, boolean z2) {
            boolean z3;
            int i4;
            int i5 = 1;
            boolean z4 = keyEvent.getAction() == 0;
            boolean z5 = keyEvent.getAction() == 1;
            int keyCode = keyEvent.getKeyCode();
            if (keyCode != 164) {
                switch (keyCode) {
                    case WindowManagerService.H.SHOW_STRICT_MODE_VIOLATION:
                        i5 = -1;
                    case 24:
                        z3 = false;
                        break;
                    default:
                        i5 = 0;
                        z3 = false;
                        break;
                }
            } else {
                z3 = true;
                i5 = 0;
            }
            if (z4 || z5) {
                if (z2) {
                    i4 = 4608;
                } else if (z5) {
                    i4 = 4116;
                } else {
                    i4 = 4113;
                }
                int i6 = i4;
                if (i5 != 0) {
                    dispatchAdjustVolumeLocked(str, i, i2, z, i3, z5 ? 0 : i5, i6);
                } else if (z3 && z4 && keyEvent.getRepeatCount() == 0) {
                    dispatchAdjustVolumeLocked(str, i, i2, z, i3, 101, i6);
                }
            }
        }

        public void dispatchAdjustVolume(String str, int i, int i2, int i3) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                synchronized (MediaSessionService.this.mLock) {
                    dispatchAdjustVolumeLocked(str, callingPid, callingUid, false, i, i2, i3);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void setRemoteVolumeController(IRemoteVolumeController iRemoteVolumeController) {
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                MediaSessionService.this.enforceSystemUiPermission("listen for volume changes", callingPid, callingUid);
                MediaSessionService.this.mRvc = iRemoteVolumeController;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean isGlobalPriorityActive() {
            boolean zIsGlobalPriorityActiveLocked;
            synchronized (MediaSessionService.this.mLock) {
                zIsGlobalPriorityActiveLocked = MediaSessionService.this.isGlobalPriorityActiveLocked();
            }
            return zIsGlobalPriorityActiveLocked;
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(MediaSessionService.this.getContext(), MediaSessionService.TAG, printWriter)) {
                printWriter.println("MEDIA SESSION SERVICE (dumpsys media_session)");
                printWriter.println();
                synchronized (MediaSessionService.this.mLock) {
                    printWriter.println(MediaSessionService.this.mSessionsListeners.size() + " sessions listeners.");
                    printWriter.println("Global priority session is " + MediaSessionService.this.mGlobalPrioritySession);
                    if (MediaSessionService.this.mGlobalPrioritySession != null) {
                        MediaSessionService.this.mGlobalPrioritySession.dump(printWriter, "  ");
                    }
                    printWriter.println("User Records:");
                    int size = MediaSessionService.this.mUserRecords.size();
                    for (int i = 0; i < size; i++) {
                        ((FullUserRecord) MediaSessionService.this.mUserRecords.valueAt(i)).dumpLocked(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    }
                    MediaSessionService.this.mAudioPlayerStateMonitor.dump(MediaSessionService.this.getContext(), printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
            }
        }

        public boolean isTrusted(String str, int i, int i2) throws RemoteException {
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (MediaSessionService.this.getContext().getPackageManager().getPackageUidAsUser(str, UserHandle.getUserId(i2)) == i2) {
                    return hasMediaControlPermission(UserHandle.getUserId(callingUid), str, i, i2);
                }
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, "Package name " + str + " doesn't match with the uid " + i2);
                }
                return false;
            } catch (PackageManager.NameNotFoundException e) {
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, "Package " + str + " doesn't exist");
                }
                return false;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean createSession2(Bundle bundle) {
            return false;
        }

        public void destroySession2(Bundle bundle) {
        }

        public List<Bundle> getSessionTokens(boolean z, boolean z2, String str) throws RemoteException {
            return null;
        }

        public void addSessionTokensListener(ISessionTokensListener iSessionTokensListener, int i, String str) throws RemoteException {
        }

        public void removeSessionTokensListener(ISessionTokensListener iSessionTokensListener, String str) throws RemoteException {
        }

        private int verifySessionsRequest(ComponentName componentName, int i, int i2, int i3) {
            String packageName;
            if (componentName != null) {
                packageName = componentName.getPackageName();
                MediaSessionService.this.enforcePackageName(packageName, i3);
            } else {
                packageName = null;
            }
            int iHandleIncomingUser = ActivityManager.handleIncomingUser(i2, i3, i, true, true, "getSessions", packageName);
            MediaSessionService.this.enforceMediaPermissions(componentName, i2, i3, iHandleIncomingUser);
            return iHandleIncomingUser;
        }

        private int verifySessionsRequest2(int i, String str, int i2, int i3) throws RemoteException {
            int iHandleIncomingUser = ActivityManager.handleIncomingUser(i2, i3, i, true, true, "getSessionTokens", str);
            if (!hasMediaControlPermission(iHandleIncomingUser, str, i2, i3)) {
                throw new SecurityException("Missing permission to control media.");
            }
            return iHandleIncomingUser;
        }

        private boolean hasMediaControlPermission(int i, String str, int i2, int i3) throws RemoteException {
            if (MediaSessionService.this.isCurrentVolumeController(i2, i3) || i3 == 1000 || MediaSessionService.this.getContext().checkPermission("android.permission.MEDIA_CONTENT_CONTROL", i2, i3) == 0) {
                return true;
            }
            if (MediaSessionService.DEBUG) {
                Log.d(MediaSessionService.TAG, str + " (uid=" + i3 + ") hasn't granted MEDIA_CONTENT_CONTROL");
            }
            int userId = UserHandle.getUserId(i3);
            if (i == userId) {
                List enabledNotificationListeners = MediaSessionService.this.mNotificationManager.getEnabledNotificationListeners(userId);
                if (enabledNotificationListeners != null) {
                    for (int i4 = 0; i4 < enabledNotificationListeners.size(); i4++) {
                        if (TextUtils.equals(str, ((ComponentName) enabledNotificationListeners.get(i4)).getPackageName())) {
                            return true;
                        }
                    }
                }
                if (MediaSessionService.DEBUG) {
                    Log.d(MediaSessionService.TAG, str + " (uid=" + i3 + ") doesn't have an enabled notification listener");
                }
                return false;
            }
            return false;
        }

        private void dispatchAdjustVolumeLocked(String str, int i, int i2, boolean z, final int i3, final int i4, final int i5) {
            MediaSessionRecord defaultVolumeSession;
            boolean z2;
            if (MediaSessionService.this.isGlobalPriorityActiveLocked()) {
                defaultVolumeSession = MediaSessionService.this.mGlobalPrioritySession;
            } else {
                defaultVolumeSession = MediaSessionService.this.mCurrentFullUserRecord.mPriorityStack.getDefaultVolumeSession();
            }
            if (isValidLocalStreamType(i3) && AudioSystem.isStreamActive(i3, 0)) {
                z2 = true;
            } else {
                z2 = false;
            }
            Log.d(MediaSessionService.TAG, "Adjusting " + defaultVolumeSession + " by " + i4 + ". flags=" + i5 + ", suggestedStream=" + i3 + ", preferSuggestedStream=" + z2);
            if (defaultVolumeSession != null && !z2) {
                defaultVolumeSession.adjustVolume(str, i, i2, null, z, i4, i5, true);
                return;
            }
            if ((i5 & 512) == 0 || AudioSystem.isStreamActive(3, 0)) {
                MediaSessionService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MediaSessionService.this.mAudioService.adjustSuggestedStreamVolume(i4, i3, i5, MediaSessionService.this.getContext().getOpPackageName(), MediaSessionService.TAG);
                        } catch (RemoteException e) {
                            Log.e(MediaSessionService.TAG, "Error adjusting default volume.", e);
                        } catch (IllegalArgumentException e2) {
                            Log.e(MediaSessionService.TAG, "Cannot adjust volume: direction=" + i4 + ", suggestedStream=" + i3 + ", flags=" + i5, e2);
                        }
                    }
                });
            } else if (MediaSessionService.DEBUG) {
                Log.d(MediaSessionService.TAG, "No active session to adjust, skipping media only volume event");
            }
        }

        private void handleVoiceKeyEventLocked(String str, int i, int i2, boolean z, KeyEvent keyEvent, boolean z2) {
            int action = keyEvent.getAction();
            boolean z3 = (keyEvent.getFlags() & 128) != 0;
            if (action == 0) {
                if (keyEvent.getRepeatCount() == 0) {
                    this.mVoiceButtonDown = true;
                    this.mVoiceButtonHandled = false;
                    return;
                } else {
                    if (this.mVoiceButtonDown && !this.mVoiceButtonHandled && z3) {
                        this.mVoiceButtonHandled = true;
                        startVoiceInput(z2);
                        return;
                    }
                    return;
                }
            }
            if (action == 1 && this.mVoiceButtonDown) {
                this.mVoiceButtonDown = false;
                if (!this.mVoiceButtonHandled && !keyEvent.isCanceled()) {
                    dispatchMediaKeyEventLocked(str, i, i2, z, KeyEvent.changeAction(keyEvent, 0), z2);
                    dispatchMediaKeyEventLocked(str, i, i2, z, keyEvent, z2);
                }
            }
        }

        private void dispatchMediaKeyEventLocked(String str, int i, int i2, boolean z, KeyEvent keyEvent, boolean z2) {
            ComponentName component;
            int i3;
            MediaSessionRecord mediaButtonSessionLocked = MediaSessionService.this.mCurrentFullUserRecord.getMediaButtonSessionLocked();
            int i4 = -1;
            if (mediaButtonSessionLocked == null) {
                if (MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver != null || MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver != null) {
                    if (z2) {
                        this.mKeyEventReceiver.aquireWakeLockLocked();
                    }
                    Intent intent = new Intent("android.intent.action.MEDIA_BUTTON");
                    intent.addFlags(268435456);
                    intent.putExtra("android.intent.extra.KEY_EVENT", keyEvent);
                    if (z) {
                        str = MediaSessionService.this.getContext().getPackageName();
                    }
                    intent.putExtra("android.intent.extra.PACKAGE_NAME", str);
                    try {
                        if (MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver != null) {
                            PendingIntent pendingIntent = MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver;
                            Log.d(MediaSessionService.TAG, "Sending " + keyEvent + " to the last known PendingIntent " + pendingIntent);
                            Context context = MediaSessionService.this.getContext();
                            if (z2) {
                                i4 = this.mKeyEventReceiver.mLastTimeoutId;
                            }
                            pendingIntent.send(context, i4, intent, this.mKeyEventReceiver, MediaSessionService.this.mHandler);
                            if (MediaSessionService.this.mCurrentFullUserRecord.mCallback != null && (component = MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver.getIntent().getComponent()) != null) {
                                MediaSessionService.this.mCurrentFullUserRecord.mCallback.onMediaKeyEventDispatchedToMediaButtonReceiver(keyEvent, component);
                            }
                            return;
                        }
                        ComponentName componentName = MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiver;
                        Log.d(MediaSessionService.TAG, "Sending " + keyEvent + " to the restored intent " + componentName);
                        intent.setComponent(componentName);
                        MediaSessionService.this.getContext().sendBroadcastAsUser(intent, UserHandle.of(MediaSessionService.this.mCurrentFullUserRecord.mRestoredMediaButtonReceiverUserId));
                        if (MediaSessionService.this.mCurrentFullUserRecord.mCallback != null) {
                            MediaSessionService.this.mCurrentFullUserRecord.mCallback.onMediaKeyEventDispatchedToMediaButtonReceiver(keyEvent, componentName);
                            return;
                        }
                        return;
                    } catch (PendingIntent.CanceledException e) {
                        Log.i(MediaSessionService.TAG, "Error sending key event to media button receiver " + MediaSessionService.this.mCurrentFullUserRecord.mLastMediaButtonReceiver, e);
                        return;
                    } catch (RemoteException e2) {
                        Log.w(MediaSessionService.TAG, "Failed to send callback", e2);
                        return;
                    }
                }
                return;
            }
            Log.d(MediaSessionService.TAG, "Sending " + keyEvent + " to " + mediaButtonSessionLocked);
            if (z2) {
                this.mKeyEventReceiver.aquireWakeLockLocked();
            }
            if (!z2) {
                i3 = -1;
            } else {
                i3 = this.mKeyEventReceiver.mLastTimeoutId;
            }
            mediaButtonSessionLocked.sendMediaButton(str, i, i2, z, keyEvent, i3, this.mKeyEventReceiver);
            if (MediaSessionService.this.mCurrentFullUserRecord.mCallback != null) {
                try {
                    MediaSessionService.this.mCurrentFullUserRecord.mCallback.onMediaKeyEventDispatchedToMediaSession(keyEvent, new MediaSession.Token(mediaButtonSessionLocked.getControllerBinder()));
                } catch (RemoteException e3) {
                    Log.w(MediaSessionService.TAG, "Failed to send callback", e3);
                }
            }
        }

        private void startVoiceInput(boolean z) {
            Intent intent;
            PowerManager powerManager = (PowerManager) MediaSessionService.this.getContext().getSystemService("power");
            boolean z2 = false;
            boolean z3 = MediaSessionService.this.mKeyguardManager != null && MediaSessionService.this.mKeyguardManager.isKeyguardLocked();
            if (!z3 && powerManager.isScreenOn()) {
                intent = new Intent("android.speech.action.WEB_SEARCH");
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_WEB_SEARCH");
            } else {
                intent = new Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE");
                if (z3 && MediaSessionService.this.mKeyguardManager.isKeyguardSecure()) {
                    z2 = true;
                }
                intent.putExtra("android.speech.extras.EXTRA_SECURE", z2);
                Log.i(MediaSessionService.TAG, "voice-based interactions: about to use ACTION_VOICE_SEARCH_HANDS_FREE");
            }
            if (z) {
                MediaSessionService.this.mMediaEventWakeLock.acquire();
            }
            try {
                try {
                    intent.setFlags(276824064);
                    if (MediaSessionService.DEBUG) {
                        Log.d(MediaSessionService.TAG, "voiceIntent: " + intent);
                    }
                    MediaSessionService.this.getContext().startActivityAsUser(intent, UserHandle.CURRENT);
                    if (!z) {
                        return;
                    }
                } catch (ActivityNotFoundException e) {
                    Log.w(MediaSessionService.TAG, "No activity for search: " + e);
                    if (!z) {
                        return;
                    }
                }
                MediaSessionService.this.mMediaEventWakeLock.release();
            } catch (Throwable th) {
                if (z) {
                    MediaSessionService.this.mMediaEventWakeLock.release();
                }
                throw th;
            }
        }

        private boolean isVoiceKey(int i) {
            return i == 79 || (!MediaSessionService.this.mHasFeatureLeanback && i == 85);
        }

        private boolean isUserSetupComplete() {
            return Settings.Secure.getIntForUser(MediaSessionService.this.getContext().getContentResolver(), "user_setup_complete", 0, -2) != 0;
        }

        private boolean isValidLocalStreamType(int i) {
            return i >= 0 && i <= 5;
        }

        private class MediaKeyListenerResultReceiver extends ResultReceiver implements Runnable {
            private final boolean mAsSystemService;
            private boolean mHandled;
            private final KeyEvent mKeyEvent;
            private final boolean mNeedWakeLock;
            private final String mPackageName;
            private final int mPid;
            private final int mUid;

            private MediaKeyListenerResultReceiver(String str, int i, int i2, boolean z, KeyEvent keyEvent, boolean z2) {
                super(MediaSessionService.this.mHandler);
                MediaSessionService.this.mHandler.postDelayed(this, 1000L);
                this.mPackageName = str;
                this.mPid = i;
                this.mUid = i2;
                this.mAsSystemService = z;
                this.mKeyEvent = keyEvent;
                this.mNeedWakeLock = z2;
            }

            @Override
            public void run() {
                Log.d(MediaSessionService.TAG, "The media key listener is timed-out for " + this.mKeyEvent);
                dispatchMediaKeyEvent();
            }

            @Override
            protected void onReceiveResult(int i, Bundle bundle) {
                if (i == 1) {
                    this.mHandled = true;
                    MediaSessionService.this.mHandler.removeCallbacks(this);
                } else {
                    dispatchMediaKeyEvent();
                }
            }

            private void dispatchMediaKeyEvent() {
                if (this.mHandled) {
                    return;
                }
                this.mHandled = true;
                MediaSessionService.this.mHandler.removeCallbacks(this);
                synchronized (MediaSessionService.this.mLock) {
                    if (MediaSessionService.this.isGlobalPriorityActiveLocked() || !SessionManagerImpl.this.isVoiceKey(this.mKeyEvent.getKeyCode())) {
                        SessionManagerImpl.this.dispatchMediaKeyEventLocked(this.mPackageName, this.mPid, this.mUid, this.mAsSystemService, this.mKeyEvent, this.mNeedWakeLock);
                    } else {
                        SessionManagerImpl.this.handleVoiceKeyEventLocked(this.mPackageName, this.mPid, this.mUid, this.mAsSystemService, this.mKeyEvent, this.mNeedWakeLock);
                    }
                }
            }
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
                    if (this.mRefCount == 0) {
                        return;
                    }
                    this.mLastTimeoutId++;
                    this.mRefCount = 0;
                    releaseWakeLockLocked();
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
            protected void onReceiveResult(int i, Bundle bundle) {
                if (i >= this.mLastTimeoutId) {
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
            public void onSendFinished(PendingIntent pendingIntent, Intent intent, int i, String str, Bundle bundle) {
                onReceiveResult(i, null);
            }
        }
    }

    final class MessageHandler extends Handler {
        private static final int MSG_SESSIONS_CHANGED = 1;
        private static final int MSG_SESSIONS_TOKENS_CHANGED = 3;
        private static final int MSG_VOLUME_INITIAL_DOWN = 2;
        private final SparseArray<Integer> mIntegerCache = new SparseArray<>();

        MessageHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    MediaSessionService.this.pushSessionsChanged(((Integer) message.obj).intValue());
                    return;
                case 2:
                    synchronized (MediaSessionService.this.mLock) {
                        FullUserRecord fullUserRecord = (FullUserRecord) MediaSessionService.this.mUserRecords.get(message.arg1);
                        if (fullUserRecord != null && fullUserRecord.mInitialDownVolumeKeyEvent != null) {
                            MediaSessionService.this.dispatchVolumeKeyLongPressLocked(fullUserRecord.mInitialDownVolumeKeyEvent);
                            fullUserRecord.mInitialDownVolumeKeyEvent = null;
                        }
                        break;
                    }
                    return;
                case 3:
                    MediaSessionService.this.pushSessionTokensChanged(((Integer) message.obj).intValue());
                    return;
                default:
                    return;
            }
        }

        public void postSessionsChanged(int i) {
            Integer numValueOf = this.mIntegerCache.get(i);
            if (numValueOf == null) {
                numValueOf = Integer.valueOf(i);
                this.mIntegerCache.put(i, numValueOf);
            }
            removeMessages(1, numValueOf);
            obtainMessage(1, numValueOf).sendToTarget();
        }
    }

    private class ControllerCallback extends MediaController2.ControllerCallback {
        private final SessionToken2 mToken;

        ControllerCallback(SessionToken2 sessionToken2) {
            this.mToken = sessionToken2;
        }

        @Override
        public void onDisconnected(MediaController2 mediaController2) {
            MediaSessionService.this.destroySession2Internal(this.mToken);
        }
    }

    private final class SessionTokensListenerRecord implements IBinder.DeathRecipient {
        private final ISessionTokensListener mListener;
        private final int mUserId;

        public SessionTokensListenerRecord(ISessionTokensListener iSessionTokensListener, int i) {
            this.mListener = iSessionTokensListener;
            this.mUserId = i;
        }

        @Override
        public void binderDied() {
            synchronized (MediaSessionService.this.mLock) {
                MediaSessionService.this.mSessionTokensListeners.remove(this);
            }
        }
    }

    private void postSessionTokensUpdated(int i) {
        this.mHandler.obtainMessage(3, Integer.valueOf(i)).sendToTarget();
    }

    private void pushSessionTokensChanged(int i) {
        synchronized (this.mLock) {
            ArrayList arrayList = new ArrayList();
            for (SessionToken2 sessionToken2 : this.mSessionRecords.keySet()) {
                if (UserHandle.getUserId(sessionToken2.getUid()) == i || -1 == i) {
                    arrayList.add(sessionToken2.toBundle());
                }
            }
            for (SessionTokensListenerRecord sessionTokensListenerRecord : this.mSessionTokensListeners) {
                if (sessionTokensListenerRecord.mUserId == i || sessionTokensListenerRecord.mUserId == -1) {
                    try {
                        sessionTokensListenerRecord.mListener.onSessionTokensChanged(arrayList);
                    } catch (RemoteException e) {
                        Log.w(TAG, "Failed to notify session tokens changed", e);
                    }
                }
            }
        }
    }

    private boolean addSessionRecordLocked(SessionToken2 sessionToken2) {
        return addSessionRecordLocked(sessionToken2, null);
    }

    private boolean addSessionRecordLocked(SessionToken2 sessionToken2, MediaController2 mediaController2) {
        if (this.mSessionRecords.containsKey(sessionToken2) && this.mSessionRecords.get(sessionToken2) == mediaController2) {
            return false;
        }
        this.mSessionRecords.put(sessionToken2, mediaController2);
        return true;
    }

    private boolean removeSessionRecordLocked(SessionToken2 sessionToken2) {
        if (!this.mSessionRecords.containsKey(sessionToken2)) {
            return false;
        }
        this.mSessionRecords.remove(sessionToken2);
        return true;
    }
}
