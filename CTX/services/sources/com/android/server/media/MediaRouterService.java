package com.android.server.media;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRoutesInfo;
import android.media.IAudioRoutesObserver;
import android.media.IAudioService;
import android.media.IMediaRouterClient;
import android.media.IMediaRouterService;
import android.media.MediaRouterClientState;
import android.media.RemoteDisplayState;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.IntArray;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.media.AudioPlayerStateMonitor;
import com.android.server.media.RemoteDisplayProviderProxy;
import com.android.server.media.RemoteDisplayProviderWatcher;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

public final class MediaRouterService extends IMediaRouterService.Stub implements Watchdog.Monitor {
    static final long CONNECTED_TIMEOUT = 60000;
    static final long CONNECTING_TIMEOUT = 5000;
    private static final boolean DEBUG;
    private static final String TAG = "MediaRouterService";
    BluetoothDevice mActiveBluetoothDevice;
    private final AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    private final IAudioService mAudioService;
    private final Context mContext;
    private final Object mLock = new Object();
    private final SparseArray<UserRecord> mUserRecords = new SparseArray<>();
    private final ArrayMap<IBinder, ClientRecord> mAllClientRecords = new ArrayMap<>();
    private int mCurrentUserId = -1;
    private final Handler mHandler = new Handler();
    private final IntArray mActivePlayerMinPriorityQueue = new IntArray();
    private final IntArray mActivePlayerUidMinPriorityQueue = new IntArray();
    private final BroadcastReceiver mReceiver = new MediaRouterServiceBroadcastReceiver();
    int mAudioRouteMainType = 0;
    boolean mGlobalBluetoothA2dpOn = false;

    static {
        DEBUG = Log.isLoggable(TAG, 3) || !"user".equals(Build.TYPE);
    }

    public MediaRouterService(Context context) {
        this.mContext = context;
        Watchdog.getInstance().addMonitor(this);
        this.mAudioService = IAudioService.Stub.asInterface(ServiceManager.getService("audio"));
        this.mAudioPlayerStateMonitor = AudioPlayerStateMonitor.getInstance();
        this.mAudioPlayerStateMonitor.registerListener(new AudioPlayerStateMonitor.OnAudioPlayerActiveStateChangedListener() {
            static final long WAIT_MS = 500;
            final Runnable mRestoreBluetoothA2dpRunnable = new Runnable() {
                @Override
                public void run() {
                    MediaRouterService.this.restoreBluetoothA2dp();
                }
            };

            @Override
            public void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z) {
                boolean z2;
                int i;
                if (z || !audioPlaybackConfiguration.isActive()) {
                    z2 = false;
                } else {
                    z2 = true;
                }
                int playerInterfaceId = audioPlaybackConfiguration.getPlayerInterfaceId();
                int clientUid = audioPlaybackConfiguration.getClientUid();
                int iIndexOf = MediaRouterService.this.mActivePlayerMinPriorityQueue.indexOf(playerInterfaceId);
                if (iIndexOf >= 0) {
                    MediaRouterService.this.mActivePlayerMinPriorityQueue.remove(iIndexOf);
                    MediaRouterService.this.mActivePlayerUidMinPriorityQueue.remove(iIndexOf);
                }
                if (z2) {
                    MediaRouterService.this.mActivePlayerMinPriorityQueue.add(audioPlaybackConfiguration.getPlayerInterfaceId());
                    MediaRouterService.this.mActivePlayerUidMinPriorityQueue.add(clientUid);
                    i = clientUid;
                } else {
                    i = MediaRouterService.this.mActivePlayerUidMinPriorityQueue.size() > 0 ? MediaRouterService.this.mActivePlayerUidMinPriorityQueue.get(MediaRouterService.this.mActivePlayerUidMinPriorityQueue.size() - 1) : -1;
                }
                MediaRouterService.this.mHandler.removeCallbacks(this.mRestoreBluetoothA2dpRunnable);
                if (i < 0) {
                    MediaRouterService.this.mHandler.postDelayed(this.mRestoreBluetoothA2dpRunnable, 500L);
                    if (MediaRouterService.DEBUG) {
                        Slog.d(MediaRouterService.TAG, "onAudioPlayerActiveStateChanged: uid=" + clientUid + ", active=" + z2 + ", delaying");
                        return;
                    }
                    return;
                }
                MediaRouterService.this.restoreRoute(i);
                if (MediaRouterService.DEBUG) {
                    Slog.d(MediaRouterService.TAG, "onAudioPlayerActiveStateChanged: uid=" + clientUid + ", active=" + z2 + ", restoreUid=" + i);
                }
            }
        }, this.mHandler);
        this.mAudioPlayerStateMonitor.registerSelfIntoAudioServiceIfNeeded(this.mAudioService);
        try {
            this.mAudioService.startWatchingRoutes(new IAudioRoutesObserver.Stub() {
                public void dispatchAudioRoutesChanged(AudioRoutesInfo audioRoutesInfo) {
                    synchronized (MediaRouterService.this.mLock) {
                        if (audioRoutesInfo.mainType != MediaRouterService.this.mAudioRouteMainType) {
                            boolean z = false;
                            if ((audioRoutesInfo.mainType & 19) == 0) {
                                MediaRouterService mediaRouterService = MediaRouterService.this;
                                if (audioRoutesInfo.bluetoothName != null || MediaRouterService.this.mActiveBluetoothDevice != null) {
                                    z = true;
                                }
                                mediaRouterService.mGlobalBluetoothA2dpOn = z;
                            } else {
                                MediaRouterService.this.mGlobalBluetoothA2dpOn = false;
                            }
                            MediaRouterService.this.mAudioRouteMainType = audioRoutesInfo.mainType;
                        }
                    }
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException in the audio service.");
        }
        context.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, new IntentFilter("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED"), null, null);
    }

    public void systemRunning() {
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.USER_SWITCHED")) {
                    MediaRouterService.this.switchUser();
                }
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"));
        switchUser();
    }

    @Override
    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    public void registerClientAsUser(IMediaRouterClient iMediaRouterClient, String str, int i) {
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        int callingUid = Binder.getCallingUid();
        if (!validatePackageName(callingUid, str)) {
            throw new SecurityException("packageName must match the calling uid");
        }
        int callingPid = Binder.getCallingPid();
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(callingPid, callingUid, i, false, true, "registerClientAsUser", str);
        boolean z = this.mContext.checkCallingOrSelfPermission("android.permission.CONFIGURE_WIFI_DISPLAY") == 0;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                registerClientLocked(iMediaRouterClient, callingUid, callingPid, str, iHandleIncomingUser, z);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void unregisterClient(IMediaRouterClient iMediaRouterClient) {
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                unregisterClientLocked(iMediaRouterClient, false);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public MediaRouterClientState getState(IMediaRouterClient iMediaRouterClient) {
        MediaRouterClientState stateLocked;
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                stateLocked = getStateLocked(iMediaRouterClient);
            }
            return stateLocked;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean isPlaybackActive(IMediaRouterClient iMediaRouterClient) {
        ClientRecord clientRecord;
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                clientRecord = this.mAllClientRecords.get(iMediaRouterClient.asBinder());
            }
            if (clientRecord != null) {
                return this.mAudioPlayerStateMonitor.isPlaybackActive(clientRecord.mUid);
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setDiscoveryRequest(IMediaRouterClient iMediaRouterClient, int i, boolean z) {
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                setDiscoveryRequestLocked(iMediaRouterClient, i, z);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setSelectedRoute(IMediaRouterClient iMediaRouterClient, String str, boolean z) {
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                setSelectedRouteLocked(iMediaRouterClient, str, z);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void requestSetVolume(IMediaRouterClient iMediaRouterClient, String str, int i) {
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        if (str == null) {
            throw new IllegalArgumentException("routeId must not be null");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                requestSetVolumeLocked(iMediaRouterClient, str, i);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void requestUpdateVolume(IMediaRouterClient iMediaRouterClient, String str, int i) {
        if (iMediaRouterClient == null) {
            throw new IllegalArgumentException("client must not be null");
        }
        if (str == null) {
            throw new IllegalArgumentException("routeId must not be null");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                requestUpdateVolumeLocked(iMediaRouterClient, str, i);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.println("MEDIA ROUTER SERVICE (dumpsys media_router)");
            printWriter.println();
            printWriter.println("Global state");
            printWriter.println("  mCurrentUserId=" + this.mCurrentUserId);
            synchronized (this.mLock) {
                int size = this.mUserRecords.size();
                for (int i = 0; i < size; i++) {
                    UserRecord userRecordValueAt = this.mUserRecords.valueAt(i);
                    printWriter.println();
                    userRecordValueAt.dump(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
            }
        }
    }

    void restoreBluetoothA2dp() {
        boolean z;
        BluetoothDevice bluetoothDevice;
        try {
            synchronized (this.mLock) {
                z = this.mGlobalBluetoothA2dpOn;
                bluetoothDevice = this.mActiveBluetoothDevice;
            }
            if (bluetoothDevice != null) {
                Slog.v(TAG, "restoreBluetoothA2dp(" + z + ")");
                this.mAudioService.setBluetoothA2dpOn(z);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "RemoteException while calling setBluetoothA2dpOn.");
        }
    }

    void restoreRoute(int i) {
        ClientRecord next;
        synchronized (this.mLock) {
            UserRecord userRecord = this.mUserRecords.get(UserHandle.getUserId(i));
            if (userRecord != null && userRecord.mClientRecords != null) {
                Iterator<ClientRecord> it = userRecord.mClientRecords.iterator();
                while (it.hasNext()) {
                    next = it.next();
                    if (validatePackageName(i, next.mPackageName)) {
                        break;
                    }
                }
                next = null;
            } else {
                next = null;
            }
        }
        if (next != null) {
            try {
                next.mClient.onRestoreRoute();
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to call onRestoreRoute. Client probably died.");
                return;
            }
        }
        restoreBluetoothA2dp();
    }

    void switchUser() {
        synchronized (this.mLock) {
            int currentUser = ActivityManager.getCurrentUser();
            if (this.mCurrentUserId != currentUser) {
                int i = this.mCurrentUserId;
                this.mCurrentUserId = currentUser;
                UserRecord userRecord = this.mUserRecords.get(i);
                if (userRecord != null) {
                    userRecord.mHandler.sendEmptyMessage(2);
                    disposeUserIfNeededLocked(userRecord);
                }
                UserRecord userRecord2 = this.mUserRecords.get(currentUser);
                if (userRecord2 != null) {
                    userRecord2.mHandler.sendEmptyMessage(1);
                }
            }
        }
    }

    void clientDied(ClientRecord clientRecord) {
        synchronized (this.mLock) {
            unregisterClientLocked(clientRecord.mClient, true);
        }
    }

    private void registerClientLocked(IMediaRouterClient iMediaRouterClient, int i, int i2, String str, int i3, boolean z) {
        UserRecord userRecord;
        boolean z2;
        IBinder iBinderAsBinder = iMediaRouterClient.asBinder();
        if (this.mAllClientRecords.get(iBinderAsBinder) == null) {
            UserRecord userRecord2 = this.mUserRecords.get(i3);
            if (userRecord2 != null) {
                userRecord = userRecord2;
                z2 = false;
            } else {
                userRecord = new UserRecord(i3);
                z2 = true;
            }
            ClientRecord clientRecord = new ClientRecord(userRecord, iMediaRouterClient, i, i2, str, z);
            try {
                iBinderAsBinder.linkToDeath(clientRecord, 0);
                if (z2) {
                    this.mUserRecords.put(i3, userRecord);
                    initializeUserLocked(userRecord);
                }
                userRecord.mClientRecords.add(clientRecord);
                this.mAllClientRecords.put(iBinderAsBinder, clientRecord);
                initializeClientLocked(clientRecord);
            } catch (RemoteException e) {
                throw new RuntimeException("Media router client died prematurely.", e);
            }
        }
    }

    private void unregisterClientLocked(IMediaRouterClient iMediaRouterClient, boolean z) {
        ClientRecord clientRecordRemove = this.mAllClientRecords.remove(iMediaRouterClient.asBinder());
        if (clientRecordRemove != null) {
            UserRecord userRecord = clientRecordRemove.mUserRecord;
            userRecord.mClientRecords.remove(clientRecordRemove);
            disposeClientLocked(clientRecordRemove, z);
            disposeUserIfNeededLocked(userRecord);
        }
    }

    private MediaRouterClientState getStateLocked(IMediaRouterClient iMediaRouterClient) {
        ClientRecord clientRecord = this.mAllClientRecords.get(iMediaRouterClient.asBinder());
        if (clientRecord != null) {
            return clientRecord.getState();
        }
        return null;
    }

    private void setDiscoveryRequestLocked(IMediaRouterClient iMediaRouterClient, int i, boolean z) {
        ClientRecord clientRecord = this.mAllClientRecords.get(iMediaRouterClient.asBinder());
        if (clientRecord != null) {
            if (!clientRecord.mTrusted) {
                i &= -5;
            }
            if (clientRecord.mRouteTypes != i || clientRecord.mActiveScan != z) {
                if (DEBUG) {
                    Slog.d(TAG, clientRecord + ": Set discovery request, routeTypes=0x" + Integer.toHexString(i) + ", activeScan=" + z);
                }
                clientRecord.mRouteTypes = i;
                clientRecord.mActiveScan = z;
                clientRecord.mUserRecord.mHandler.sendEmptyMessage(3);
            }
        }
    }

    private void setSelectedRouteLocked(IMediaRouterClient iMediaRouterClient, String str, boolean z) {
        ClientRecord clientRecord = this.mAllClientRecords.get(iMediaRouterClient.asBinder());
        if (clientRecord != null) {
            String str2 = clientRecord.mSelectedRouteId;
            if (!Objects.equals(str, str2)) {
                if (DEBUG) {
                    Slog.d(TAG, clientRecord + ": Set selected route, routeId=" + str + ", oldRouteId=" + str2 + ", explicit=" + z);
                }
                clientRecord.mSelectedRouteId = str;
                if (z && clientRecord.mTrusted) {
                    if (str2 != null) {
                        clientRecord.mUserRecord.mHandler.obtainMessage(5, str2).sendToTarget();
                    }
                    if (str != null) {
                        clientRecord.mUserRecord.mHandler.obtainMessage(4, str).sendToTarget();
                    }
                }
            }
        }
    }

    private void requestSetVolumeLocked(IMediaRouterClient iMediaRouterClient, String str, int i) {
        ClientRecord clientRecord = this.mAllClientRecords.get(iMediaRouterClient.asBinder());
        if (clientRecord != null) {
            clientRecord.mUserRecord.mHandler.obtainMessage(6, i, 0, str).sendToTarget();
        }
    }

    private void requestUpdateVolumeLocked(IMediaRouterClient iMediaRouterClient, String str, int i) {
        ClientRecord clientRecord = this.mAllClientRecords.get(iMediaRouterClient.asBinder());
        if (clientRecord != null) {
            clientRecord.mUserRecord.mHandler.obtainMessage(7, i, 0, str).sendToTarget();
        }
    }

    private void initializeUserLocked(UserRecord userRecord) {
        if (DEBUG) {
            Slog.d(TAG, userRecord + ": Initialized");
        }
        if (userRecord.mUserId == this.mCurrentUserId) {
            userRecord.mHandler.sendEmptyMessage(1);
        }
    }

    private void disposeUserIfNeededLocked(UserRecord userRecord) {
        if (userRecord.mUserId != this.mCurrentUserId && userRecord.mClientRecords.isEmpty()) {
            if (DEBUG) {
                Slog.d(TAG, userRecord + ": Disposed");
            }
            this.mUserRecords.remove(userRecord.mUserId);
        }
    }

    private void initializeClientLocked(ClientRecord clientRecord) {
        if (DEBUG) {
            Slog.d(TAG, clientRecord + ": Registered");
        }
    }

    private void disposeClientLocked(ClientRecord clientRecord, boolean z) {
        if (DEBUG) {
            if (z) {
                Slog.d(TAG, clientRecord + ": Died!");
            } else {
                Slog.d(TAG, clientRecord + ": Unregistered");
            }
        }
        if (clientRecord.mRouteTypes != 0 || clientRecord.mActiveScan) {
            clientRecord.mUserRecord.mHandler.sendEmptyMessage(3);
        }
        clientRecord.dispose();
    }

    private boolean validatePackageName(int i, String str) {
        String[] packagesForUid;
        if (str != null && (packagesForUid = this.mContext.getPackageManager().getPackagesForUid(i)) != null) {
            for (String str2 : packagesForUid) {
                if (str2.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    final class MediaRouterServiceBroadcastReceiver extends BroadcastReceiver {
        MediaRouterServiceBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED")) {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                synchronized (MediaRouterService.this.mLock) {
                    MediaRouterService.this.mActiveBluetoothDevice = bluetoothDevice;
                    MediaRouterService.this.mGlobalBluetoothA2dpOn = bluetoothDevice != null;
                }
            }
        }
    }

    final class ClientRecord implements IBinder.DeathRecipient {
        public boolean mActiveScan;
        public final IMediaRouterClient mClient;
        public final String mPackageName;
        public final int mPid;
        public int mRouteTypes;
        public String mSelectedRouteId;
        public final boolean mTrusted;
        public final int mUid;
        public final UserRecord mUserRecord;

        public ClientRecord(UserRecord userRecord, IMediaRouterClient iMediaRouterClient, int i, int i2, String str, boolean z) {
            this.mUserRecord = userRecord;
            this.mClient = iMediaRouterClient;
            this.mUid = i;
            this.mPid = i2;
            this.mPackageName = str;
            this.mTrusted = z;
        }

        public void dispose() {
            this.mClient.asBinder().unlinkToDeath(this, 0);
        }

        @Override
        public void binderDied() {
            MediaRouterService.this.clientDied(this);
        }

        MediaRouterClientState getState() {
            if (this.mTrusted) {
                return this.mUserRecord.mRouterState;
            }
            return null;
        }

        public void dump(PrintWriter printWriter, String str) {
            printWriter.println(str + this);
            String str2 = str + "  ";
            printWriter.println(str2 + "mTrusted=" + this.mTrusted);
            printWriter.println(str2 + "mRouteTypes=0x" + Integer.toHexString(this.mRouteTypes));
            printWriter.println(str2 + "mActiveScan=" + this.mActiveScan);
            printWriter.println(str2 + "mSelectedRouteId=" + this.mSelectedRouteId);
        }

        public String toString() {
            return "Client " + this.mPackageName + " (pid " + this.mPid + ")";
        }
    }

    final class UserRecord {
        public final ArrayList<ClientRecord> mClientRecords = new ArrayList<>();
        public final UserHandler mHandler;
        public MediaRouterClientState mRouterState;
        public final int mUserId;

        public UserRecord(int i) {
            this.mUserId = i;
            this.mHandler = new UserHandler(MediaRouterService.this, this);
        }

        public void dump(final PrintWriter printWriter, String str) {
            printWriter.println(str + this);
            final String str2 = str + "  ";
            int size = this.mClientRecords.size();
            if (size != 0) {
                for (int i = 0; i < size; i++) {
                    this.mClientRecords.get(i).dump(printWriter, str2);
                }
            } else {
                printWriter.println(str2 + "<no clients>");
            }
            printWriter.println(str2 + "State");
            printWriter.println(str2 + "mRouterState=" + this.mRouterState);
            if (!this.mHandler.runWithScissors(new Runnable() {
                @Override
                public void run() {
                    UserRecord.this.mHandler.dump(printWriter, str2);
                }
            }, 1000L)) {
                printWriter.println(str2 + "<could not dump handler state>");
            }
        }

        public String toString() {
            return "User " + this.mUserId;
        }
    }

    static final class UserHandler extends Handler implements RemoteDisplayProviderWatcher.Callback, RemoteDisplayProviderProxy.Callback {
        private static final int MSG_CONNECTION_TIMED_OUT = 9;
        public static final int MSG_REQUEST_SET_VOLUME = 6;
        public static final int MSG_REQUEST_UPDATE_VOLUME = 7;
        public static final int MSG_SELECT_ROUTE = 4;
        public static final int MSG_START = 1;
        public static final int MSG_STOP = 2;
        public static final int MSG_UNSELECT_ROUTE = 5;
        private static final int MSG_UPDATE_CLIENT_STATE = 8;
        public static final int MSG_UPDATE_DISCOVERY_REQUEST = 3;
        private static final int PHASE_CONNECTED = 2;
        private static final int PHASE_CONNECTING = 1;
        private static final int PHASE_NOT_AVAILABLE = -1;
        private static final int PHASE_NOT_CONNECTED = 0;
        private static final int TIMEOUT_REASON_CONNECTION_LOST = 2;
        private static final int TIMEOUT_REASON_NOT_AVAILABLE = 1;
        private static final int TIMEOUT_REASON_WAITING_FOR_CONNECTED = 4;
        private static final int TIMEOUT_REASON_WAITING_FOR_CONNECTING = 3;
        private boolean mClientStateUpdateScheduled;
        private int mConnectionPhase;
        private int mConnectionTimeoutReason;
        private long mConnectionTimeoutStartTime;
        private int mDiscoveryMode;
        private final ArrayList<ProviderRecord> mProviderRecords;
        private boolean mRunning;
        private RouteRecord mSelectedRouteRecord;
        private final MediaRouterService mService;
        private final ArrayList<IMediaRouterClient> mTempClients;
        private final UserRecord mUserRecord;
        private final RemoteDisplayProviderWatcher mWatcher;

        public UserHandler(MediaRouterService mediaRouterService, UserRecord userRecord) {
            super(Looper.getMainLooper(), null, true);
            this.mProviderRecords = new ArrayList<>();
            this.mTempClients = new ArrayList<>();
            this.mDiscoveryMode = 0;
            this.mConnectionPhase = -1;
            this.mService = mediaRouterService;
            this.mUserRecord = userRecord;
            this.mWatcher = new RemoteDisplayProviderWatcher(mediaRouterService.mContext, this, this, this.mUserRecord.mUserId);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    start();
                    break;
                case 2:
                    stop();
                    break;
                case 3:
                    updateDiscoveryRequest();
                    break;
                case 4:
                    selectRoute((String) message.obj);
                    break;
                case 5:
                    unselectRoute((String) message.obj);
                    break;
                case 6:
                    requestSetVolume((String) message.obj, message.arg1);
                    break;
                case 7:
                    requestUpdateVolume((String) message.obj, message.arg1);
                    break;
                case 8:
                    updateClientState();
                    break;
                case 9:
                    connectionTimedOut();
                    break;
            }
        }

        public void dump(PrintWriter printWriter, String str) {
            printWriter.println(str + "Handler");
            String str2 = str + "  ";
            printWriter.println(str2 + "mRunning=" + this.mRunning);
            printWriter.println(str2 + "mDiscoveryMode=" + this.mDiscoveryMode);
            printWriter.println(str2 + "mSelectedRouteRecord=" + this.mSelectedRouteRecord);
            printWriter.println(str2 + "mConnectionPhase=" + this.mConnectionPhase);
            printWriter.println(str2 + "mConnectionTimeoutReason=" + this.mConnectionTimeoutReason);
            StringBuilder sb = new StringBuilder();
            sb.append(str2);
            sb.append("mConnectionTimeoutStartTime=");
            sb.append(this.mConnectionTimeoutReason != 0 ? TimeUtils.formatUptime(this.mConnectionTimeoutStartTime) : "<n/a>");
            printWriter.println(sb.toString());
            this.mWatcher.dump(printWriter, str);
            int size = this.mProviderRecords.size();
            if (size != 0) {
                for (int i = 0; i < size; i++) {
                    this.mProviderRecords.get(i).dump(printWriter, str);
                }
                return;
            }
            printWriter.println(str2 + "<no providers>");
        }

        private void start() {
            if (!this.mRunning) {
                this.mRunning = true;
                this.mWatcher.start();
            }
        }

        private void stop() {
            if (this.mRunning) {
                this.mRunning = false;
                unselectSelectedRoute();
                this.mWatcher.stop();
            }
        }

        private void updateDiscoveryRequest() {
            int i;
            int i2;
            boolean z;
            synchronized (this.mService.mLock) {
                int size = this.mUserRecord.mClientRecords.size();
                i2 = 0;
                z = false;
                for (int i3 = 0; i3 < size; i3++) {
                    ClientRecord clientRecord = this.mUserRecord.mClientRecords.get(i3);
                    i2 |= clientRecord.mRouteTypes;
                    z |= clientRecord.mActiveScan;
                }
            }
            int i4 = (i2 & 4) != 0 ? z ? 2 : 1 : 0;
            if (this.mDiscoveryMode != i4) {
                this.mDiscoveryMode = i4;
                int size2 = this.mProviderRecords.size();
                for (i = 0; i < size2; i++) {
                    this.mProviderRecords.get(i).getProvider().setDiscoveryMode(this.mDiscoveryMode);
                }
            }
        }

        private void selectRoute(String str) {
            RouteRecord routeRecordFindRouteRecord;
            if (str != null) {
                if ((this.mSelectedRouteRecord == null || !str.equals(this.mSelectedRouteRecord.getUniqueId())) && (routeRecordFindRouteRecord = findRouteRecord(str)) != null) {
                    unselectSelectedRoute();
                    Slog.i(MediaRouterService.TAG, "Selected route:" + routeRecordFindRouteRecord);
                    this.mSelectedRouteRecord = routeRecordFindRouteRecord;
                    checkSelectedRouteState();
                    routeRecordFindRouteRecord.getProvider().setSelectedDisplay(routeRecordFindRouteRecord.getDescriptorId());
                    scheduleUpdateClientState();
                }
            }
        }

        private void unselectRoute(String str) {
            if (str != null && this.mSelectedRouteRecord != null && str.equals(this.mSelectedRouteRecord.getUniqueId())) {
                unselectSelectedRoute();
            }
        }

        private void unselectSelectedRoute() {
            if (this.mSelectedRouteRecord != null) {
                Slog.i(MediaRouterService.TAG, "Unselected route:" + this.mSelectedRouteRecord);
                this.mSelectedRouteRecord.getProvider().setSelectedDisplay(null);
                this.mSelectedRouteRecord = null;
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        private void requestSetVolume(String str, int i) {
            if (this.mSelectedRouteRecord != null && str.equals(this.mSelectedRouteRecord.getUniqueId())) {
                this.mSelectedRouteRecord.getProvider().setDisplayVolume(i);
            }
        }

        private void requestUpdateVolume(String str, int i) {
            if (this.mSelectedRouteRecord != null && str.equals(this.mSelectedRouteRecord.getUniqueId())) {
                this.mSelectedRouteRecord.getProvider().adjustDisplayVolume(i);
            }
        }

        @Override
        public void addProvider(RemoteDisplayProviderProxy remoteDisplayProviderProxy) {
            remoteDisplayProviderProxy.setCallback(this);
            remoteDisplayProviderProxy.setDiscoveryMode(this.mDiscoveryMode);
            remoteDisplayProviderProxy.setSelectedDisplay(null);
            ProviderRecord providerRecord = new ProviderRecord(remoteDisplayProviderProxy);
            this.mProviderRecords.add(providerRecord);
            providerRecord.updateDescriptor(remoteDisplayProviderProxy.getDisplayState());
            scheduleUpdateClientState();
        }

        @Override
        public void removeProvider(RemoteDisplayProviderProxy remoteDisplayProviderProxy) {
            int iFindProviderRecord = findProviderRecord(remoteDisplayProviderProxy);
            if (iFindProviderRecord >= 0) {
                this.mProviderRecords.remove(iFindProviderRecord).updateDescriptor(null);
                remoteDisplayProviderProxy.setCallback(null);
                remoteDisplayProviderProxy.setDiscoveryMode(0);
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        @Override
        public void onDisplayStateChanged(RemoteDisplayProviderProxy remoteDisplayProviderProxy, RemoteDisplayState remoteDisplayState) {
            updateProvider(remoteDisplayProviderProxy, remoteDisplayState);
        }

        private void updateProvider(RemoteDisplayProviderProxy remoteDisplayProviderProxy, RemoteDisplayState remoteDisplayState) {
            int iFindProviderRecord = findProviderRecord(remoteDisplayProviderProxy);
            if (iFindProviderRecord >= 0 && this.mProviderRecords.get(iFindProviderRecord).updateDescriptor(remoteDisplayState)) {
                checkSelectedRouteState();
                scheduleUpdateClientState();
            }
        }

        private void checkSelectedRouteState() {
            if (this.mSelectedRouteRecord == null) {
                this.mConnectionPhase = -1;
                updateConnectionTimeout(0);
            }
            if (!this.mSelectedRouteRecord.isValid() || !this.mSelectedRouteRecord.isEnabled()) {
                updateConnectionTimeout(1);
                return;
            }
            int i = this.mConnectionPhase;
            this.mConnectionPhase = getConnectionPhase(this.mSelectedRouteRecord.getStatus());
            if (i >= 1 && this.mConnectionPhase < 1) {
                updateConnectionTimeout(2);
                return;
            }
            switch (this.mConnectionPhase) {
                case 0:
                    updateConnectionTimeout(3);
                    break;
                case 1:
                    if (i != 1) {
                        Slog.i(MediaRouterService.TAG, "Connecting to route: " + this.mSelectedRouteRecord);
                    }
                    updateConnectionTimeout(4);
                    break;
                case 2:
                    if (i != 2) {
                        Slog.i(MediaRouterService.TAG, "Connected to route: " + this.mSelectedRouteRecord);
                    }
                    updateConnectionTimeout(0);
                    break;
                default:
                    updateConnectionTimeout(1);
                    break;
            }
        }

        private void updateConnectionTimeout(int i) {
            if (i != this.mConnectionTimeoutReason) {
                if (this.mConnectionTimeoutReason != 0) {
                    removeMessages(9);
                }
                this.mConnectionTimeoutReason = i;
                this.mConnectionTimeoutStartTime = SystemClock.uptimeMillis();
                switch (i) {
                    case 1:
                    case 2:
                        sendEmptyMessage(9);
                        break;
                    case 3:
                        sendEmptyMessageDelayed(9, MediaRouterService.CONNECTING_TIMEOUT);
                        break;
                    case 4:
                        sendEmptyMessageDelayed(9, 60000L);
                        break;
                }
            }
        }

        private void connectionTimedOut() {
            if (this.mConnectionTimeoutReason == 0 || this.mSelectedRouteRecord == null) {
                Log.wtf(MediaRouterService.TAG, "Handled connection timeout for no reason.");
                return;
            }
            switch (this.mConnectionTimeoutReason) {
                case 1:
                    Slog.i(MediaRouterService.TAG, "Selected route no longer available: " + this.mSelectedRouteRecord);
                    break;
                case 2:
                    Slog.i(MediaRouterService.TAG, "Selected route connection lost: " + this.mSelectedRouteRecord);
                    break;
                case 3:
                    Slog.i(MediaRouterService.TAG, "Selected route timed out while waiting for connection attempt to begin after " + (SystemClock.uptimeMillis() - this.mConnectionTimeoutStartTime) + " ms: " + this.mSelectedRouteRecord);
                    break;
                case 4:
                    Slog.i(MediaRouterService.TAG, "Selected route timed out while connecting after " + (SystemClock.uptimeMillis() - this.mConnectionTimeoutStartTime) + " ms: " + this.mSelectedRouteRecord);
                    break;
            }
            this.mConnectionTimeoutReason = 0;
            unselectSelectedRoute();
        }

        private void scheduleUpdateClientState() {
            if (!this.mClientStateUpdateScheduled) {
                this.mClientStateUpdateScheduled = true;
                sendEmptyMessage(8);
            }
        }

        private void updateClientState() {
            this.mClientStateUpdateScheduled = false;
            MediaRouterClientState mediaRouterClientState = new MediaRouterClientState();
            int size = this.mProviderRecords.size();
            for (int i = 0; i < size; i++) {
                this.mProviderRecords.get(i).appendClientState(mediaRouterClientState);
            }
            try {
                synchronized (this.mService.mLock) {
                    this.mUserRecord.mRouterState = mediaRouterClientState;
                    int size2 = this.mUserRecord.mClientRecords.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        this.mTempClients.add(this.mUserRecord.mClientRecords.get(i2).mClient);
                    }
                }
                int size3 = this.mTempClients.size();
                for (int i3 = 0; i3 < size3; i3++) {
                    try {
                        this.mTempClients.get(i3).onStateChanged();
                    } catch (RemoteException e) {
                        Slog.w(MediaRouterService.TAG, "Failed to call onStateChanged. Client probably died.");
                    }
                }
            } finally {
                this.mTempClients.clear();
            }
        }

        private int findProviderRecord(RemoteDisplayProviderProxy remoteDisplayProviderProxy) {
            int size = this.mProviderRecords.size();
            for (int i = 0; i < size; i++) {
                if (this.mProviderRecords.get(i).getProvider() == remoteDisplayProviderProxy) {
                    return i;
                }
            }
            return -1;
        }

        private RouteRecord findRouteRecord(String str) {
            int size = this.mProviderRecords.size();
            for (int i = 0; i < size; i++) {
                RouteRecord routeRecordFindRouteByUniqueId = this.mProviderRecords.get(i).findRouteByUniqueId(str);
                if (routeRecordFindRouteByUniqueId != null) {
                    return routeRecordFindRouteByUniqueId;
                }
            }
            return null;
        }

        private static int getConnectionPhase(int i) {
            if (i != 6) {
                switch (i) {
                    case 0:
                        return 2;
                    case 1:
                    case 3:
                        return 0;
                    case 2:
                        return 1;
                    default:
                        return -1;
                }
            }
            return 2;
        }

        static final class ProviderRecord {
            private RemoteDisplayState mDescriptor;
            private final RemoteDisplayProviderProxy mProvider;
            private final ArrayList<RouteRecord> mRoutes = new ArrayList<>();
            private final String mUniquePrefix;

            public ProviderRecord(RemoteDisplayProviderProxy remoteDisplayProviderProxy) {
                this.mProvider = remoteDisplayProviderProxy;
                this.mUniquePrefix = remoteDisplayProviderProxy.getFlattenedComponentName() + ":";
            }

            public RemoteDisplayProviderProxy getProvider() {
                return this.mProvider;
            }

            public String getUniquePrefix() {
                return this.mUniquePrefix;
            }

            public boolean updateDescriptor(RemoteDisplayState remoteDisplayState) {
                int i;
                int size;
                boolean z = false;
                z = false;
                if (this.mDescriptor != remoteDisplayState) {
                    this.mDescriptor = remoteDisplayState;
                    if (remoteDisplayState != null) {
                        if (remoteDisplayState.isValid()) {
                            ArrayList arrayList = remoteDisplayState.displays;
                            int size2 = arrayList.size();
                            boolean zUpdateDescriptor = false;
                            i = 0;
                            for (int i2 = 0; i2 < size2; i2++) {
                                RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo = (RemoteDisplayState.RemoteDisplayInfo) arrayList.get(i2);
                                String str = remoteDisplayInfo.id;
                                int iFindRouteByDescriptorId = findRouteByDescriptorId(str);
                                if (iFindRouteByDescriptorId < 0) {
                                    RouteRecord routeRecord = new RouteRecord(this, str, assignRouteUniqueId(str));
                                    this.mRoutes.add(i, routeRecord);
                                    routeRecord.updateDescriptor(remoteDisplayInfo);
                                    zUpdateDescriptor = true;
                                    i++;
                                } else if (iFindRouteByDescriptorId < i) {
                                    Slog.w(MediaRouterService.TAG, "Ignoring route descriptor with duplicate id: " + remoteDisplayInfo);
                                } else {
                                    RouteRecord routeRecord2 = this.mRoutes.get(iFindRouteByDescriptorId);
                                    Collections.swap(this.mRoutes, iFindRouteByDescriptorId, i);
                                    zUpdateDescriptor |= routeRecord2.updateDescriptor(remoteDisplayInfo);
                                    i++;
                                }
                            }
                            z = zUpdateDescriptor;
                            size = this.mRoutes.size() - 1;
                            while (size >= i) {
                            }
                        } else {
                            Slog.w(MediaRouterService.TAG, "Ignoring invalid descriptor from media route provider: " + this.mProvider.getFlattenedComponentName());
                            i = 0;
                            size = this.mRoutes.size() - 1;
                            while (size >= i) {
                            }
                        }
                    } else {
                        i = 0;
                        size = this.mRoutes.size() - 1;
                        while (size >= i) {
                            this.mRoutes.remove(size).updateDescriptor(null);
                            size--;
                            z = true;
                        }
                    }
                }
                return z;
            }

            public void appendClientState(MediaRouterClientState mediaRouterClientState) {
                int size = this.mRoutes.size();
                for (int i = 0; i < size; i++) {
                    mediaRouterClientState.routes.add(this.mRoutes.get(i).getInfo());
                }
            }

            public RouteRecord findRouteByUniqueId(String str) {
                int size = this.mRoutes.size();
                for (int i = 0; i < size; i++) {
                    RouteRecord routeRecord = this.mRoutes.get(i);
                    if (routeRecord.getUniqueId().equals(str)) {
                        return routeRecord;
                    }
                }
                return null;
            }

            private int findRouteByDescriptorId(String str) {
                int size = this.mRoutes.size();
                for (int i = 0; i < size; i++) {
                    if (this.mRoutes.get(i).getDescriptorId().equals(str)) {
                        return i;
                    }
                }
                return -1;
            }

            public void dump(PrintWriter printWriter, String str) {
                printWriter.println(str + this);
                String str2 = str + "  ";
                this.mProvider.dump(printWriter, str2);
                int size = this.mRoutes.size();
                if (size != 0) {
                    for (int i = 0; i < size; i++) {
                        this.mRoutes.get(i).dump(printWriter, str2);
                    }
                    return;
                }
                printWriter.println(str2 + "<no routes>");
            }

            public String toString() {
                return "Provider " + this.mProvider.getFlattenedComponentName();
            }

            private String assignRouteUniqueId(String str) {
                return this.mUniquePrefix + str;
            }
        }

        static final class RouteRecord {
            private RemoteDisplayState.RemoteDisplayInfo mDescriptor;
            private final String mDescriptorId;
            private MediaRouterClientState.RouteInfo mImmutableInfo;
            private final MediaRouterClientState.RouteInfo mMutableInfo;
            private final ProviderRecord mProviderRecord;

            public RouteRecord(ProviderRecord providerRecord, String str, String str2) {
                this.mProviderRecord = providerRecord;
                this.mDescriptorId = str;
                this.mMutableInfo = new MediaRouterClientState.RouteInfo(str2);
            }

            public RemoteDisplayProviderProxy getProvider() {
                return this.mProviderRecord.getProvider();
            }

            public ProviderRecord getProviderRecord() {
                return this.mProviderRecord;
            }

            public String getDescriptorId() {
                return this.mDescriptorId;
            }

            public String getUniqueId() {
                return this.mMutableInfo.id;
            }

            public MediaRouterClientState.RouteInfo getInfo() {
                if (this.mImmutableInfo == null) {
                    this.mImmutableInfo = new MediaRouterClientState.RouteInfo(this.mMutableInfo);
                }
                return this.mImmutableInfo;
            }

            public boolean isValid() {
                return this.mDescriptor != null;
            }

            public boolean isEnabled() {
                return this.mMutableInfo.enabled;
            }

            public int getStatus() {
                return this.mMutableInfo.statusCode;
            }

            public boolean updateDescriptor(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                boolean z = false;
                if (this.mDescriptor != remoteDisplayInfo) {
                    this.mDescriptor = remoteDisplayInfo;
                    if (remoteDisplayInfo != null) {
                        String strComputeName = computeName(remoteDisplayInfo);
                        if (!Objects.equals(this.mMutableInfo.name, strComputeName)) {
                            this.mMutableInfo.name = strComputeName;
                            z = true;
                        }
                        String strComputeDescription = computeDescription(remoteDisplayInfo);
                        if (!Objects.equals(this.mMutableInfo.description, strComputeDescription)) {
                            this.mMutableInfo.description = strComputeDescription;
                            z = true;
                        }
                        int iComputeSupportedTypes = computeSupportedTypes(remoteDisplayInfo);
                        if (this.mMutableInfo.supportedTypes != iComputeSupportedTypes) {
                            this.mMutableInfo.supportedTypes = iComputeSupportedTypes;
                            z = true;
                        }
                        boolean zComputeEnabled = computeEnabled(remoteDisplayInfo);
                        if (this.mMutableInfo.enabled != zComputeEnabled) {
                            this.mMutableInfo.enabled = zComputeEnabled;
                            z = true;
                        }
                        int iComputeStatusCode = computeStatusCode(remoteDisplayInfo);
                        if (this.mMutableInfo.statusCode != iComputeStatusCode) {
                            this.mMutableInfo.statusCode = iComputeStatusCode;
                            z = true;
                        }
                        int iComputePlaybackType = computePlaybackType(remoteDisplayInfo);
                        if (this.mMutableInfo.playbackType != iComputePlaybackType) {
                            this.mMutableInfo.playbackType = iComputePlaybackType;
                            z = true;
                        }
                        int iComputePlaybackStream = computePlaybackStream(remoteDisplayInfo);
                        if (this.mMutableInfo.playbackStream != iComputePlaybackStream) {
                            this.mMutableInfo.playbackStream = iComputePlaybackStream;
                            z = true;
                        }
                        int iComputeVolume = computeVolume(remoteDisplayInfo);
                        if (this.mMutableInfo.volume != iComputeVolume) {
                            this.mMutableInfo.volume = iComputeVolume;
                            z = true;
                        }
                        int iComputeVolumeMax = computeVolumeMax(remoteDisplayInfo);
                        if (this.mMutableInfo.volumeMax != iComputeVolumeMax) {
                            this.mMutableInfo.volumeMax = iComputeVolumeMax;
                            z = true;
                        }
                        int iComputeVolumeHandling = computeVolumeHandling(remoteDisplayInfo);
                        if (this.mMutableInfo.volumeHandling != iComputeVolumeHandling) {
                            this.mMutableInfo.volumeHandling = iComputeVolumeHandling;
                            z = true;
                        }
                        int iComputePresentationDisplayId = computePresentationDisplayId(remoteDisplayInfo);
                        if (this.mMutableInfo.presentationDisplayId != iComputePresentationDisplayId) {
                            this.mMutableInfo.presentationDisplayId = iComputePresentationDisplayId;
                            z = true;
                        }
                    }
                }
                if (z) {
                    this.mImmutableInfo = null;
                }
                return z;
            }

            public void dump(PrintWriter printWriter, String str) {
                printWriter.println(str + this);
                String str2 = str + "  ";
                printWriter.println(str2 + "mMutableInfo=" + this.mMutableInfo);
                printWriter.println(str2 + "mDescriptorId=" + this.mDescriptorId);
                printWriter.println(str2 + "mDescriptor=" + this.mDescriptor);
            }

            public String toString() {
                return "Route " + this.mMutableInfo.name + " (" + this.mMutableInfo.id + ")";
            }

            private static String computeName(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                return remoteDisplayInfo.name;
            }

            private static String computeDescription(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                String str = remoteDisplayInfo.description;
                if (TextUtils.isEmpty(str)) {
                    return null;
                }
                return str;
            }

            private static int computeSupportedTypes(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                return 7;
            }

            private static boolean computeEnabled(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                switch (remoteDisplayInfo.status) {
                    case 2:
                    case 3:
                    case 4:
                        return true;
                    default:
                        return false;
                }
            }

            private static int computeStatusCode(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                switch (remoteDisplayInfo.status) {
                    case 0:
                        return 4;
                    case 1:
                        return 5;
                    case 2:
                        return 3;
                    case 3:
                        return 2;
                    case 4:
                        return 6;
                    default:
                        return 0;
                }
            }

            private static int computePlaybackType(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                return 1;
            }

            private static int computePlaybackStream(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                return 3;
            }

            private static int computeVolume(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                int i = remoteDisplayInfo.volume;
                int i2 = remoteDisplayInfo.volumeMax;
                if (i < 0) {
                    return 0;
                }
                if (i > i2) {
                    return i2;
                }
                return i;
            }

            private static int computeVolumeMax(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                int i = remoteDisplayInfo.volumeMax;
                if (i > 0) {
                    return i;
                }
                return 0;
            }

            private static int computeVolumeHandling(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                if (remoteDisplayInfo.volumeHandling == 1) {
                    return 1;
                }
                return 0;
            }

            private static int computePresentationDisplayId(RemoteDisplayState.RemoteDisplayInfo remoteDisplayInfo) {
                int i = remoteDisplayInfo.presentationDisplayId;
                if (i < 0) {
                    return -1;
                }
                return i;
            }
        }
    }
}
