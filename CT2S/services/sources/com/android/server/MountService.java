package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.ObbInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.os.storage.IMountServiceListener;
import android.os.storage.IMountShutdownObserver;
import android.os.storage.IObbActionListener;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IMediaContainerService;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.NativeDaemonConnector;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.xmlpull.v1.XmlPullParserException;

class MountService extends IMountService.Stub implements INativeDaemonConnectorCallbacks, Watchdog.Monitor {
    private static final int CRYPTO_ALGORITHM_KEY_SIZE = 128;
    private static final boolean DEBUG_EVENTS = false;
    private static final boolean DEBUG_OBB = false;
    private static final boolean DEBUG_UNMOUNT = false;
    private static final int H_FSTRIM = 5;
    private static final int H_SYSTEM_READY = 4;
    private static final int H_UNMOUNT_MS = 3;
    private static final int H_UNMOUNT_PM_DONE = 2;
    private static final int H_UNMOUNT_PM_UPDATE = 1;
    private static final String LAST_FSTRIM_FILE = "last-fstrim";
    private static final boolean LOCAL_LOGD = false;
    private static final int MAX_CONTAINERS = 250;
    private static final int MAX_UNMOUNT_RETRIES = 4;
    private static final int OBB_FLUSH_MOUNT_STATE = 5;
    private static final int OBB_MCS_BOUND = 2;
    private static final int OBB_MCS_RECONNECT = 4;
    private static final int OBB_MCS_UNBIND = 3;
    private static final int OBB_RUN_ACTION = 1;
    private static final int PBKDF2_HASH_ROUNDS = 1024;
    private static final int RETRY_UNMOUNT_DELAY = 30;
    private static final String TAG = "MountService";
    private static final String TAG_STORAGE = "storage";
    private static final String TAG_STORAGE_LIST = "StorageList";
    private static final String VOLD_TAG = "VoldConnector";
    private static final boolean WATCHDOG_ENABLE = false;
    private final NativeDaemonConnector mConnector;
    private final Context mContext;
    private StorageVolume mEmulatedTemplate;
    private final Handler mHandler;
    private long mLastMaintenance;
    private final File mLastMaintenanceFile;
    private final ObbActionHandler mObbActionHandler;
    private PackageManagerService mPms;
    private boolean mUmsEnabling;
    static MountService sSelf = null;
    public static final String[] CRYPTO_TYPES = {"password", "default", "pattern", "pin"};
    static final ComponentName DEFAULT_CONTAINER_COMPONENT = new ComponentName("com.android.defcontainer", "com.android.defcontainer.DefaultContainerService");
    private final Object mVolumesLock = new Object();

    @GuardedBy("mVolumesLock")
    private final ArrayList<StorageVolume> mVolumes = Lists.newArrayList();

    @GuardedBy("mVolumesLock")
    private final HashMap<String, StorageVolume> mVolumesByPath = Maps.newHashMap();

    @GuardedBy("mVolumesLock")
    private final HashMap<String, String> mVolumeStates = Maps.newHashMap();
    private volatile boolean mSystemReady = false;
    private boolean mUmsAvailable = false;
    private final ArrayList<MountServiceBinderListener> mListeners = new ArrayList<>();
    private final CountDownLatch mConnectedSignal = new CountDownLatch(1);
    private final CountDownLatch mAsecsScanned = new CountDownLatch(1);
    private boolean mSendUmsConnectedOnBoot = false;
    private final HashSet<String> mAsecMountSet = new HashSet<>();
    private final Map<IBinder, List<ObbState>> mObbMounts = new HashMap();
    private final Map<String, ObbState> mObbPathToStateMap = new HashMap();
    private final DefaultContainerConnection mDefContainerConn = new DefaultContainerConnection();
    private IMediaContainerService mContainerService = null;
    private final BroadcastReceiver mUserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int userId = intent.getIntExtra("android.intent.extra.user_handle", -1);
            if (userId != -1) {
                UserHandle user = new UserHandle(userId);
                String action = intent.getAction();
                if ("android.intent.action.USER_ADDED".equals(action)) {
                    synchronized (MountService.this.mVolumesLock) {
                        MountService.this.createEmulatedVolumeForUserLocked(user);
                    }
                    return;
                }
                if ("android.intent.action.USER_REMOVED".equals(action)) {
                    synchronized (MountService.this.mVolumesLock) {
                        List<StorageVolume> toRemove = Lists.newArrayList();
                        for (StorageVolume volume : MountService.this.mVolumes) {
                            if (user.equals(volume.getOwner())) {
                                toRemove.add(volume);
                            }
                        }
                        Iterator<StorageVolume> it = toRemove.iterator();
                        while (it.hasNext()) {
                            MountService.this.removeVolumeLocked(it.next());
                        }
                    }
                    return;
                }
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    for (StorageVolume volume2 : MountService.this.mVolumes) {
                        if (!volume2.isEmulated()) {
                            if (userId == 0) {
                                try {
                                    int rc = MountService.this.doMountVolume(volume2.getPath());
                                    if (rc != 0) {
                                        Slog.w(MountService.TAG, String.format("SwitchUser mount failed (%d)", Integer.valueOf(rc)));
                                    }
                                } catch (Exception ex) {
                                    Slog.w(MountService.TAG, "Failed to mount media on switch user", ex);
                                }
                            } else {
                                MountService.this.doUnmountVolume(volume2.getPath(), true, false);
                            }
                        }
                    }
                }
            }
        }
    };
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean available = false;
            if (intent.getBooleanExtra("connected", false) && intent.getBooleanExtra("mass_storage", false)) {
                available = true;
            }
            MountService.this.notifyShareAvailabilityChange(available);
        }
    };

    class VolumeState {
        public static final int Checking = 3;
        public static final int Formatting = 6;
        public static final int Idle = 1;
        public static final int Init = -1;
        public static final int Mounted = 4;
        public static final int NoMedia = 0;
        public static final int Pending = 2;
        public static final int Shared = 7;
        public static final int SharedMnt = 8;
        public static final int Unmounting = 5;

        VolumeState() {
        }
    }

    class VoldResponseCode {
        public static final int AsecListResult = 111;
        public static final int AsecPathResult = 211;
        public static final int CryptfsGetfieldResult = 113;
        public static final int FstrimCompleted = 700;
        public static final int OpFailedMediaBlank = 402;
        public static final int OpFailedMediaCorrupt = 403;
        public static final int OpFailedNoMedia = 401;
        public static final int OpFailedStorageBusy = 405;
        public static final int OpFailedStorageNotFound = 406;
        public static final int OpFailedVolNotMounted = 404;
        public static final int ShareEnabledResult = 212;
        public static final int ShareStatusResult = 210;
        public static final int StorageUsersListResult = 112;
        public static final int VolumeBadRemoval = 632;
        public static final int VolumeDiskInserted = 630;
        public static final int VolumeDiskRemoved = 631;
        public static final int VolumeListResult = 110;
        public static final int VolumeStateChange = 605;
        public static final int VolumeUserLabelChange = 614;
        public static final int VolumeUuidChange = 613;

        VoldResponseCode() {
        }
    }

    class ObbState implements IBinder.DeathRecipient {
        final String canonicalPath;
        final int nonce;
        final int ownerGid;
        final String ownerPath;
        final String rawPath;
        final IObbActionListener token;
        final String voldPath;

        public ObbState(String rawPath, String canonicalPath, int callingUid, IObbActionListener token, int nonce) {
            this.rawPath = rawPath;
            this.canonicalPath = canonicalPath.toString();
            int userId = UserHandle.getUserId(callingUid);
            this.ownerPath = MountService.buildObbPath(canonicalPath, userId, false);
            this.voldPath = MountService.buildObbPath(canonicalPath, userId, true);
            this.ownerGid = UserHandle.getSharedAppGid(callingUid);
            this.token = token;
            this.nonce = nonce;
        }

        public IBinder getBinder() {
            return this.token.asBinder();
        }

        @Override
        public void binderDied() {
            ObbAction action = MountService.this.new UnmountObbAction(this, true);
            MountService.this.mObbActionHandler.sendMessage(MountService.this.mObbActionHandler.obtainMessage(1, action));
        }

        public void link() throws RemoteException {
            getBinder().linkToDeath(this, 0);
        }

        public void unlink() {
            getBinder().unlinkToDeath(this, 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ObbState{");
            sb.append("rawPath=").append(this.rawPath);
            sb.append(",canonicalPath=").append(this.canonicalPath);
            sb.append(",ownerPath=").append(this.ownerPath);
            sb.append(",voldPath=").append(this.voldPath);
            sb.append(",ownerGid=").append(this.ownerGid);
            sb.append(",token=").append(this.token);
            sb.append(",binder=").append(getBinder());
            sb.append('}');
            return sb.toString();
        }
    }

    class DefaultContainerConnection implements ServiceConnection {
        DefaultContainerConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IMediaContainerService imcs = IMediaContainerService.Stub.asInterface(service);
            MountService.this.mObbActionHandler.sendMessage(MountService.this.mObbActionHandler.obtainMessage(2, imcs));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    class UnmountCallBack {
        final boolean force;
        final String path;
        final boolean removeEncryption;
        int retries = 0;

        UnmountCallBack(String path, boolean force, boolean removeEncryption) {
            this.path = path;
            this.force = force;
            this.removeEncryption = removeEncryption;
        }

        void handleFinished() {
            MountService.this.doUnmountVolume(this.path, true, this.removeEncryption);
        }
    }

    class UmsEnableCallBack extends UnmountCallBack {
        final String method;

        UmsEnableCallBack(String path, String method, boolean force) {
            super(path, force, false);
            this.method = method;
        }

        @Override
        void handleFinished() {
            super.handleFinished();
            MountService.this.doShareUnshareVolume(this.path, this.method, true);
        }
    }

    class ShutdownCallBack extends UnmountCallBack {
        MountShutdownLatch mMountShutdownLatch;

        ShutdownCallBack(String path, MountShutdownLatch mountShutdownLatch) {
            super(path, true, false);
            this.mMountShutdownLatch = mountShutdownLatch;
        }

        @Override
        void handleFinished() {
            int ret = MountService.this.doUnmountVolume(this.path, true, this.removeEncryption);
            Slog.i(MountService.TAG, "Unmount completed: " + this.path + ", result code: " + ret);
            this.mMountShutdownLatch.countDown();
        }
    }

    static class MountShutdownLatch {
        private AtomicInteger mCount;
        private IMountShutdownObserver mObserver;

        MountShutdownLatch(IMountShutdownObserver observer, int count) {
            this.mObserver = observer;
            this.mCount = new AtomicInteger(count);
        }

        void countDown() {
            boolean sendShutdown = false;
            if (this.mCount.decrementAndGet() == 0) {
                sendShutdown = true;
            }
            if (sendShutdown && this.mObserver != null) {
                try {
                    this.mObserver.onShutDownComplete(0);
                } catch (RemoteException e) {
                    Slog.w(MountService.TAG, "RemoteException when shutting down");
                }
            }
        }
    }

    class MountServiceHandler extends Handler {
        ArrayList<UnmountCallBack> mForceUnmounts;
        boolean mUpdatingStatus;

        MountServiceHandler(Looper l) {
            super(l);
            this.mForceUnmounts = new ArrayList<>();
            this.mUpdatingStatus = false;
        }

        @Override
        public void handleMessage(Message msg) {
            int[] pids;
            int sizeArrN;
            switch (msg.what) {
                case 1:
                    this.mForceUnmounts.add((UnmountCallBack) msg.obj);
                    if (!this.mUpdatingStatus) {
                        this.mUpdatingStatus = true;
                        MountService.this.mPms.updateExternalMediaStatus(false, true);
                    }
                    break;
                case 2:
                    this.mUpdatingStatus = false;
                    int size = this.mForceUnmounts.size();
                    int[] sizeArr = new int[size];
                    ActivityManagerService ams = (ActivityManagerService) ServiceManager.getService("activity");
                    int i = 0;
                    int sizeArrN2 = 0;
                    while (i < size) {
                        UnmountCallBack ucb = this.mForceUnmounts.get(i);
                        String path = ucb.path;
                        boolean done = false;
                        if (!ucb.force || (pids = MountService.this.getStorageUsers(path)) == null || pids.length == 0) {
                            done = true;
                        } else {
                            ams.killPids(pids, "unmount media", true);
                            int[] pids2 = MountService.this.getStorageUsers(path);
                            if (pids2 == null || pids2.length == 0) {
                                done = true;
                            }
                        }
                        if (!done && ucb.retries < 4) {
                            Slog.i(MountService.TAG, "Retrying to kill storage users again");
                            Handler handler = MountService.this.mHandler;
                            Handler handler2 = MountService.this.mHandler;
                            int i2 = ucb.retries;
                            ucb.retries = i2 + 1;
                            handler.sendMessageDelayed(handler2.obtainMessage(2, Integer.valueOf(i2)), 30L);
                            sizeArrN = sizeArrN2;
                        } else {
                            if (ucb.retries >= 4) {
                                Slog.i(MountService.TAG, "Failed to unmount media inspite of 4 retries. Forcibly killing processes now");
                            }
                            sizeArrN = sizeArrN2 + 1;
                            sizeArr[sizeArrN2] = i;
                            MountService.this.mHandler.sendMessage(MountService.this.mHandler.obtainMessage(3, ucb));
                        }
                        i++;
                        sizeArrN2 = sizeArrN;
                    }
                    for (int i3 = sizeArrN2 - 1; i3 >= 0; i3--) {
                        this.mForceUnmounts.remove(sizeArr[i3]);
                    }
                    break;
                case 3:
                    ((UnmountCallBack) msg.obj).handleFinished();
                    break;
                case 4:
                    try {
                        MountService.this.handleSystemReady();
                    } catch (Exception ex) {
                        Slog.e(MountService.TAG, "Boot-time mount exception", ex);
                        return;
                    }
                    break;
                case 5:
                    MountService.this.waitForReady();
                    Slog.i(MountService.TAG, "Running fstrim idle maintenance");
                    try {
                        MountService.this.mLastMaintenance = System.currentTimeMillis();
                        MountService.this.mLastMaintenanceFile.setLastModified(MountService.this.mLastMaintenance);
                    } catch (Exception e) {
                        Slog.e(MountService.TAG, "Unable to record last fstrim!");
                    }
                    try {
                        MountService.this.mConnector.execute("fstrim", "dotrim");
                        EventLogTags.writeFstrimStart(SystemClock.elapsedRealtime());
                    } catch (NativeDaemonConnectorException e2) {
                        Slog.e(MountService.TAG, "Failed to run fstrim!");
                    }
                    Runnable callback = (Runnable) msg.obj;
                    if (callback != null) {
                        callback.run();
                    }
                    break;
            }
        }
    }

    void waitForAsecScan() {
        waitForLatch(this.mAsecsScanned);
    }

    private void waitForReady() {
        waitForLatch(this.mConnectedSignal);
    }

    private void waitForLatch(CountDownLatch latch) {
        while (!latch.await(5000L, TimeUnit.MILLISECONDS)) {
            try {
                Slog.w(TAG, "Thread " + Thread.currentThread().getName() + " still waiting for MountService ready...");
            } catch (InterruptedException e) {
                Slog.w(TAG, "Interrupt while waiting for MountService to be ready.");
            }
        }
    }

    private boolean isReady() {
        try {
            return this.mConnectedSignal.await(0L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void handleSystemReady() {
        HashMap<String, String> snapshot;
        synchronized (this.mVolumesLock) {
            snapshot = new HashMap<>(this.mVolumeStates);
        }
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            String path = entry.getKey();
            String state = entry.getValue();
            StorageVolume volume = this.mVolumesByPath.get(path);
            if (volume != null) {
                try {
                } catch (Exception e) {
                    Slog.e(TAG, "Failed to mount media on boot-time", e);
                }
                if (volume.isEmulated() || ActivityManagerNative.getDefault().getCurrentUser().id == 0) {
                }
            }
            if (state.equals("unmounted")) {
                int rc = doMountVolume(path);
                if (rc != 0) {
                    Slog.e(TAG, String.format("Boot-time mount failed (%d)", Integer.valueOf(rc)));
                }
            } else if (state.equals("shared")) {
                notifyVolumeStateChange(null, path, 0, 7);
            }
        }
        synchronized (this.mVolumesLock) {
            for (StorageVolume volume2 : this.mVolumes) {
                if (volume2.isEmulated()) {
                    updatePublicVolumeState(volume2, "mounted");
                }
            }
        }
        if (this.mSendUmsConnectedOnBoot) {
            sendUmsIntent(true);
            this.mSendUmsConnectedOnBoot = false;
        }
        MountServiceIdler.scheduleIdlePass(this.mContext);
    }

    private final class MountServiceBinderListener implements IBinder.DeathRecipient {
        final IMountServiceListener mListener;

        MountServiceBinderListener(IMountServiceListener listener) {
            this.mListener = listener;
        }

        @Override
        public void binderDied() {
            synchronized (MountService.this.mListeners) {
                MountService.this.mListeners.remove(this);
                this.mListener.asBinder().unlinkToDeath(this, 0);
            }
        }
    }

    void runIdleMaintenance(Runnable callback) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, callback));
    }

    public void runMaintenance() {
        validatePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        runIdleMaintenance(null);
    }

    public long lastMaintenance() {
        return this.mLastMaintenance;
    }

    private void doShareUnshareVolume(String path, String method, boolean enable) {
        if (!method.equals("ums")) {
            throw new IllegalArgumentException(String.format("Method %s not supported", method));
        }
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[3];
            objArr[0] = enable ? "share" : "unshare";
            objArr[1] = path;
            objArr[2] = method;
            nativeDaemonConnector.execute("volume", objArr);
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to share/unshare", e);
        }
    }

    private void updatePublicVolumeState(StorageVolume volume, String state) {
        String oldState;
        String path = volume.getPath();
        synchronized (this.mVolumesLock) {
            oldState = this.mVolumeStates.put(path, state);
            volume.setState(state);
        }
        if (state.equals(oldState)) {
            Slog.w(TAG, String.format("Duplicate state transition (%s -> %s) for %s", state, state, path));
            return;
        }
        Slog.d(TAG, "volume state changed for " + path + " (" + oldState + " -> " + state + ")");
        if (volume.isPrimary() && !volume.isEmulated()) {
            if ("unmounted".equals(state)) {
                this.mPms.updateExternalMediaStatus(false, false);
                this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(5, path));
            } else if ("mounted".equals(state)) {
                this.mPms.updateExternalMediaStatus(true, false);
            }
        }
        synchronized (this.mListeners) {
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                MountServiceBinderListener bl = this.mListeners.get(i);
                try {
                    try {
                        bl.mListener.onStorageStateChanged(path, oldState, state);
                    } catch (Exception ex) {
                        Slog.e(TAG, "Listener failed", ex);
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Listener dead");
                    this.mListeners.remove(i);
                }
            }
        }
    }

    @Override
    public void onDaemonConnected() {
        new Thread("MountService#onDaemonConnected") {
            @Override
            public void run() throws Throwable {
                StorageVolume volume;
                String state;
                try {
                    String[] vols = NativeDaemonEvent.filterMessageList(MountService.this.mConnector.executeForList("volume", "list", "broadcast"), 110);
                    for (String volstr : vols) {
                        String[] tok = volstr.split(" ");
                        String path = tok[1];
                        synchronized (MountService.this.mVolumesLock) {
                            volume = (StorageVolume) MountService.this.mVolumesByPath.get(path);
                        }
                        int st = Integer.parseInt(tok[2]);
                        if (st == 0) {
                            state = "removed";
                        } else if (st == 1) {
                            state = "unmounted";
                        } else if (st == 4) {
                            state = "mounted";
                            Slog.i(MountService.TAG, "Media already mounted on daemon connection");
                        } else if (st == 7) {
                            state = "shared";
                            Slog.i(MountService.TAG, "Media shared on daemon connection");
                        } else {
                            throw new Exception(String.format("Unexpected state %d", Integer.valueOf(st)));
                        }
                        if (state != null) {
                            MountService.this.updatePublicVolumeState(volume, state);
                        }
                    }
                } catch (Exception e) {
                    Slog.e(MountService.TAG, "Error processing initial volume state", e);
                    StorageVolume primary = MountService.this.getPrimaryPhysicalVolume();
                    if (primary != null) {
                        MountService.this.updatePublicVolumeState(primary, "removed");
                    }
                }
                MountService.this.mConnectedSignal.countDown();
                if ("".equals(SystemProperties.get("vold.encrypt_progress"))) {
                    MountService.this.copyLocaleFromMountService();
                }
                MountService.this.mPms.scanAvailableAsecs();
                MountService.this.mAsecsScanned.countDown();
            }
        }.start();
    }

    private void copyLocaleFromMountService() {
        try {
            String systemLocale = getField("SystemLocale");
            if (!TextUtils.isEmpty(systemLocale)) {
                Slog.d(TAG, "Got locale " + systemLocale + " from mount service");
                Locale locale = Locale.forLanguageTag(systemLocale);
                Configuration config = new Configuration();
                config.setLocale(locale);
                try {
                    ActivityManagerNative.getDefault().updateConfiguration(config);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Error setting system locale from mount service", e);
                }
                Slog.d(TAG, "Setting system properties to " + systemLocale + " from mount service");
                SystemProperties.set("persist.sys.language", locale.getLanguage());
                SystemProperties.set("persist.sys.country", locale.getCountry());
            }
        } catch (RemoteException e2) {
        }
    }

    @Override
    public boolean onCheckHoldWakeLock(int code) {
        return false;
    }

    @Override
    public boolean onEvent(int code, String raw, String[] cooked) {
        StorageVolume volume;
        if (code == 605) {
            notifyVolumeStateChange(cooked[2], cooked[3], Integer.parseInt(cooked[7]), Integer.parseInt(cooked[10]));
        } else if (code == 613) {
            String path = cooked[2];
            String uuid = cooked.length > 3 ? cooked[3] : null;
            StorageVolume vol = this.mVolumesByPath.get(path);
            if (vol != null) {
                vol.setUuid(uuid);
            }
        } else if (code == 614) {
            String path2 = cooked[2];
            String userLabel = cooked.length > 3 ? cooked[3] : null;
            StorageVolume vol2 = this.mVolumesByPath.get(path2);
            if (vol2 != null) {
                vol2.setUserLabel(userLabel);
            }
        } else if (code == 630 || code == 631 || code == 632) {
            String action = null;
            String str = cooked[2];
            final String path3 = cooked[3];
            try {
                String devComp = cooked[6].substring(1, cooked[6].length() - 1);
                String[] devTok = devComp.split(":");
                Integer.parseInt(devTok[0]);
                Integer.parseInt(devTok[1]);
            } catch (Exception ex) {
                Slog.e(TAG, "Failed to parse major/minor", ex);
            }
            synchronized (this.mVolumesLock) {
                volume = this.mVolumesByPath.get(path3);
                this.mVolumeStates.get(path3);
            }
            if (code == 630) {
                try {
                    if (ActivityManagerNative.getDefault().getCurrentUser().id == 0) {
                        new Thread("MountService#VolumeDiskInserted") {
                            @Override
                            public void run() {
                                try {
                                    int rc = MountService.this.doMountVolume(path3);
                                    if (rc != 0) {
                                        Slog.w(MountService.TAG, String.format("Insertion mount failed (%d)", Integer.valueOf(rc)));
                                    }
                                } catch (Exception ex2) {
                                    Slog.w(MountService.TAG, "Failed to mount media on insertion", ex2);
                                }
                            }
                        }.start();
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to mount media on insertion", e);
                }
            } else if (code == 631) {
                if (getVolumeState(path3).equals("bad_removal")) {
                    return true;
                }
                updatePublicVolumeState(volume, "unmounted");
                sendStorageIntent("android.intent.action.MEDIA_UNMOUNTED", volume, UserHandle.ALL);
                updatePublicVolumeState(volume, "removed");
                action = "android.intent.action.MEDIA_REMOVED";
            } else if (code == 632) {
                updatePublicVolumeState(volume, "unmounted");
                sendStorageIntent("android.intent.action.MEDIA_UNMOUNTED", volume, UserHandle.ALL);
                updatePublicVolumeState(volume, "bad_removal");
                action = "android.intent.action.MEDIA_BAD_REMOVAL";
            } else if (code == 700) {
                EventLogTags.writeFstrimFinish(SystemClock.elapsedRealtime());
            } else {
                Slog.e(TAG, String.format("Unknown code {%d}", Integer.valueOf(code)));
            }
            if (action != null) {
                sendStorageIntent(action, volume, UserHandle.ALL);
            }
        } else {
            return false;
        }
        return true;
    }

    private void notifyVolumeStateChange(String label, String path, int oldState, int newState) {
        StorageVolume volume;
        String state;
        synchronized (this.mVolumesLock) {
            volume = this.mVolumesByPath.get(path);
            state = getVolumeState(path);
        }
        String action = null;
        if (oldState == 7 && newState != oldState) {
            sendStorageIntent("android.intent.action.MEDIA_UNSHARED", volume, UserHandle.ALL);
        }
        if (newState != -1 && newState != 0) {
            if (newState == 1) {
                if (!state.equals("bad_removal") && !state.equals("nofs") && !state.equals("unmountable") && !getUmsEnabling()) {
                    updatePublicVolumeState(volume, "unmounted");
                    action = "android.intent.action.MEDIA_UNMOUNTED";
                }
            } else if (newState != 2) {
                if (newState == 3) {
                    updatePublicVolumeState(volume, "checking");
                    action = "android.intent.action.MEDIA_CHECKING";
                } else if (newState == 4) {
                    updatePublicVolumeState(volume, "mounted");
                    action = "android.intent.action.MEDIA_MOUNTED";
                } else if (newState == 5) {
                    action = "android.intent.action.MEDIA_EJECT";
                } else if (newState != 6) {
                    if (newState == 7) {
                        updatePublicVolumeState(volume, "unmounted");
                        sendStorageIntent("android.intent.action.MEDIA_UNMOUNTED", volume, UserHandle.ALL);
                        updatePublicVolumeState(volume, "shared");
                        action = "android.intent.action.MEDIA_SHARED";
                    } else {
                        if (newState == 8) {
                            Slog.e(TAG, "Live shared mounts not supported yet!");
                            return;
                        }
                        Slog.e(TAG, "Unhandled VolumeState {" + newState + "}");
                    }
                }
            }
        }
        if (action != null) {
            sendStorageIntent(action, volume, UserHandle.ALL);
        }
    }

    private int doMountVolume(String path) {
        StorageVolume volume;
        int rc = 0;
        synchronized (this.mVolumesLock) {
            volume = this.mVolumesByPath.get(path);
        }
        if (!volume.isEmulated() && hasUserRestriction("no_physical_media")) {
            Slog.w(TAG, "User has restriction DISALLOW_MOUNT_PHYSICAL_MEDIA; cannot mount volume.");
            return -1;
        }
        try {
            this.mConnector.execute("volume", "mount", path);
        } catch (NativeDaemonConnectorException e) {
            String action = null;
            int code = e.getCode();
            if (code == 401) {
                rc = -2;
            } else if (code == 402) {
                updatePublicVolumeState(volume, "nofs");
                action = "android.intent.action.MEDIA_NOFS";
                rc = -3;
            } else if (code == 403) {
                updatePublicVolumeState(volume, "unmountable");
                action = "android.intent.action.MEDIA_UNMOUNTABLE";
                rc = -4;
            } else {
                rc = -1;
            }
            if (action != null) {
                sendStorageIntent(action, volume, UserHandle.ALL);
            }
        }
        return rc;
    }

    private int doUnmountVolume(String path, boolean force, boolean removeEncryption) {
        if (!getVolumeState(path).equals("mounted")) {
            return VoldResponseCode.OpFailedVolNotMounted;
        }
        Runtime.getRuntime().gc();
        this.mPms.updateExternalMediaStatus(false, false);
        try {
            NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("volume", "unmount", path);
            if (removeEncryption) {
                cmd.appendArg("force_and_revert");
            } else if (force) {
                cmd.appendArg("force");
            }
            this.mConnector.execute(cmd);
            synchronized (this.mAsecMountSet) {
                this.mAsecMountSet.clear();
            }
            return 0;
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 404) {
                return -5;
            }
            if (code == 405) {
                return -7;
            }
            return -1;
        }
    }

    private int doFormatVolume(String path) {
        try {
            this.mConnector.execute("volume", "format", path);
            return 0;
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 401) {
                return -2;
            }
            if (code == 403) {
                return -4;
            }
            return -1;
        }
    }

    private boolean doGetVolumeShared(String path, String method) {
        try {
            NativeDaemonEvent event = this.mConnector.execute("volume", "shared", path, method);
            if (event.getCode() == 212) {
                return event.getMessage().endsWith("enabled");
            }
            return false;
        } catch (NativeDaemonConnectorException e) {
            Slog.e(TAG, "Failed to read response to volume shared " + path + " " + method);
            return false;
        }
    }

    private void notifyShareAvailabilityChange(boolean avail) {
        synchronized (this.mListeners) {
            this.mUmsAvailable = avail;
            for (int i = this.mListeners.size() - 1; i >= 0; i--) {
                MountServiceBinderListener bl = this.mListeners.get(i);
                try {
                    bl.mListener.onUsbMassStorageConnectionChanged(avail);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Listener dead");
                    this.mListeners.remove(i);
                } catch (Exception ex) {
                    Slog.e(TAG, "Listener failed", ex);
                }
            }
        }
        if (this.mSystemReady) {
            sendUmsIntent(avail);
        } else {
            this.mSendUmsConnectedOnBoot = avail;
        }
        StorageVolume primary = getPrimaryPhysicalVolume();
        if (!avail && primary != null && "shared".equals(getVolumeState(primary.getPath()))) {
            final String path = primary.getPath();
            new Thread("MountService#AvailabilityChange") {
                @Override
                public void run() {
                    try {
                        Slog.w(MountService.TAG, "Disabling UMS after cable disconnect");
                        MountService.this.doShareUnshareVolume(path, "ums", false);
                        int rc = MountService.this.doMountVolume(path);
                        if (rc != 0) {
                            Slog.e(MountService.TAG, String.format("Failed to remount {%s} on UMS enabled-disconnect (%d)", path, Integer.valueOf(rc)));
                        }
                    } catch (Exception ex2) {
                        Slog.w(MountService.TAG, "Failed to mount media on UMS enabled-disconnect", ex2);
                    }
                }
            }.start();
        }
    }

    private void sendStorageIntent(String action, StorageVolume volume, UserHandle user) {
        Intent intent = new Intent(action, Uri.parse("file://" + volume.getPath()));
        intent.putExtra("storage_volume", volume);
        intent.addFlags(67108864);
        Slog.d(TAG, "sendStorageIntent " + intent + " to " + user);
        this.mContext.sendBroadcastAsUser(intent, user);
    }

    private void sendUmsIntent(boolean c) {
        this.mContext.sendBroadcastAsUser(new Intent(c ? "android.intent.action.UMS_CONNECTED" : "android.intent.action.UMS_DISCONNECTED"), UserHandle.ALL);
    }

    private void validatePermission(String perm) {
        if (this.mContext.checkCallingOrSelfPermission(perm) != 0) {
            throw new SecurityException(String.format("Requires %s permission", perm));
        }
    }

    private boolean hasUserRestriction(String restriction) {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        return um.hasUserRestriction(restriction, Binder.getCallingUserHandle());
    }

    private void validateUserRestriction(String restriction) {
        if (hasUserRestriction(restriction)) {
            throw new SecurityException("User has restriction " + restriction);
        }
    }

    private void readStorageListLocked() {
        this.mVolumes.clear();
        this.mVolumeStates.clear();
        Resources resources = this.mContext.getResources();
        XmlResourceParser parser = resources.getXml(R.bool.autofill_dialog_horizontal_space_included);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        try {
            try {
                try {
                    XmlUtils.beginDocument(parser, TAG_STORAGE_LIST);
                    while (true) {
                        XmlUtils.nextElement(parser);
                        String element = parser.getName();
                        if (element == null) {
                            break;
                        }
                        if (TAG_STORAGE.equals(element)) {
                            TypedArray a = resources.obtainAttributes(attrs, com.android.internal.R.styleable.Storage);
                            String path = a.getString(0);
                            int descriptionId = a.getResourceId(1, -1);
                            CharSequence description = a.getText(1);
                            boolean primary = a.getBoolean(2, false);
                            boolean removable = a.getBoolean(3, false);
                            boolean emulated = a.getBoolean(4, false);
                            int mtpReserve = a.getInt(5, 0);
                            boolean allowMassStorage = a.getBoolean(6, false);
                            long maxFileSize = ((long) a.getInt(7, 0)) * 1024 * 1024;
                            Slog.d(TAG, "got storage path: " + path + " description: " + ((Object) description) + " primary: " + primary + " removable: " + removable + " emulated: " + emulated + " mtpReserve: " + mtpReserve + " allowMassStorage: " + allowMassStorage + " maxFileSize: " + maxFileSize);
                            if (emulated) {
                                this.mEmulatedTemplate = new StorageVolume(null, descriptionId, true, false, true, mtpReserve, false, maxFileSize, null);
                                UserManagerService userManager = UserManagerService.getInstance();
                                for (UserInfo user : userManager.getUsers(false)) {
                                    createEmulatedVolumeForUserLocked(user.getUserHandle());
                                }
                            } else if (path == null || description == null) {
                                Slog.e(TAG, "Missing storage path or description in readStorageList");
                            } else {
                                StorageVolume volume = new StorageVolume(new File(path), descriptionId, primary, removable, emulated, mtpReserve, allowMassStorage, maxFileSize, null);
                                addVolumeLocked(volume);
                                this.mVolumeStates.put(volume.getPath(), "unmounted");
                                volume.setState("unmounted");
                            }
                            a.recycle();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (XmlPullParserException e2) {
                throw new RuntimeException(e2);
            }
        } finally {
            int index = isExternalStorageEmulated() ? 1 : 0;
            for (StorageVolume volume2 : this.mVolumes) {
                if (!volume2.isEmulated()) {
                    volume2.setStorageId(index);
                    index++;
                }
            }
            parser.close();
        }
    }

    private void createEmulatedVolumeForUserLocked(UserHandle user) {
        if (this.mEmulatedTemplate == null) {
            throw new IllegalStateException("Missing emulated volume multi-user template");
        }
        Environment.UserEnvironment userEnv = new Environment.UserEnvironment(user.getIdentifier());
        File path = userEnv.getExternalStorageDirectory();
        StorageVolume volume = StorageVolume.fromTemplate(this.mEmulatedTemplate, path, user);
        volume.setStorageId(0);
        addVolumeLocked(volume);
        if (this.mSystemReady) {
            updatePublicVolumeState(volume, "mounted");
        } else {
            this.mVolumeStates.put(volume.getPath(), "mounted");
            volume.setState("mounted");
        }
    }

    private void addVolumeLocked(StorageVolume volume) {
        Slog.d(TAG, "addVolumeLocked() " + volume);
        this.mVolumes.add(volume);
        StorageVolume existing = this.mVolumesByPath.put(volume.getPath(), volume);
        if (existing != null) {
            throw new IllegalStateException("Volume at " + volume.getPath() + " already exists: " + existing);
        }
    }

    private void removeVolumeLocked(StorageVolume volume) {
        Slog.d(TAG, "removeVolumeLocked() " + volume);
        this.mVolumes.remove(volume);
        this.mVolumesByPath.remove(volume.getPath());
        this.mVolumeStates.remove(volume.getPath());
    }

    private StorageVolume getPrimaryPhysicalVolume() {
        synchronized (this.mVolumesLock) {
            for (StorageVolume volume : this.mVolumes) {
                if (volume.isPrimary() && !volume.isEmulated()) {
                    return volume;
                }
            }
            return null;
        }
    }

    public MountService(Context context) {
        sSelf = this;
        this.mContext = context;
        synchronized (this.mVolumesLock) {
            readStorageListLocked();
        }
        this.mPms = (PackageManagerService) ServiceManager.getService("package");
        HandlerThread hthread = new HandlerThread(TAG);
        hthread.start();
        this.mHandler = new MountServiceHandler(hthread.getLooper());
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_ADDED");
        userFilter.addAction("android.intent.action.USER_REMOVED");
        userFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mUserReceiver, userFilter, null, this.mHandler);
        StorageVolume primary = getPrimaryPhysicalVolume();
        if (primary != null && primary.allowMassStorage()) {
            this.mContext.registerReceiver(this.mUsbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"), null, this.mHandler);
        }
        this.mObbActionHandler = new ObbActionHandler(IoThread.get().getLooper());
        File dataDir = Environment.getDataDirectory();
        File systemDir = new File(dataDir, "system");
        this.mLastMaintenanceFile = new File(systemDir, LAST_FSTRIM_FILE);
        if (!this.mLastMaintenanceFile.exists()) {
            try {
                new FileOutputStream(this.mLastMaintenanceFile).close();
            } catch (IOException e) {
                Slog.e(TAG, "Unable to create fstrim record " + this.mLastMaintenanceFile.getPath());
            }
        } else {
            this.mLastMaintenance = this.mLastMaintenanceFile.lastModified();
        }
        this.mConnector = new NativeDaemonConnector(this, "vold", SystemService.PHASE_SYSTEM_SERVICES_READY, VOLD_TAG, 25, null);
        Thread thread = new Thread(this.mConnector, VOLD_TAG);
        thread.start();
    }

    public void systemReady() {
        this.mSystemReady = true;
        this.mHandler.obtainMessage(4).sendToTarget();
    }

    public void registerListener(IMountServiceListener listener) {
        synchronized (this.mListeners) {
            MountServiceBinderListener bl = new MountServiceBinderListener(listener);
            try {
                listener.asBinder().linkToDeath(bl, 0);
                this.mListeners.add(bl);
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to link to listener death");
            }
        }
    }

    public void unregisterListener(IMountServiceListener listener) {
        synchronized (this.mListeners) {
            for (MountServiceBinderListener bl : this.mListeners) {
                if (bl.mListener.asBinder() == listener.asBinder()) {
                    this.mListeners.remove(this.mListeners.indexOf(bl));
                    listener.asBinder().unlinkToDeath(bl, 0);
                    return;
                }
            }
        }
    }

    public void shutdown(IMountShutdownObserver observer) {
        int retries;
        validatePermission("android.permission.SHUTDOWN");
        Slog.i(TAG, "Shutting down");
        synchronized (this.mVolumesLock) {
            MountShutdownLatch mountShutdownLatch = new MountShutdownLatch(observer, this.mVolumeStates.size());
            for (String path : this.mVolumeStates.keySet()) {
                String state = this.mVolumeStates.get(path);
                if (state.equals("shared")) {
                    setUsbMassStorageEnabled(false);
                } else if (state.equals("checking")) {
                    int retries2 = 30;
                    while (true) {
                        if (!state.equals("checking")) {
                            retries = retries2;
                            break;
                        }
                        retries = retries2 - 1;
                        if (retries2 < 0) {
                            break;
                        }
                        try {
                            Thread.sleep(1000L);
                            state = Environment.getExternalStorageState();
                            retries2 = retries;
                        } catch (InterruptedException iex) {
                            Slog.e(TAG, "Interrupted while waiting for media", iex);
                            if (retries == 0) {
                            }
                            if (!state.equals("mounted")) {
                            }
                        }
                    }
                    if (retries == 0) {
                        Slog.e(TAG, "Timed out waiting for media to check");
                    }
                }
                if (!state.equals("mounted")) {
                    ShutdownCallBack ucb = new ShutdownCallBack(path, mountShutdownLatch);
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(1, ucb));
                } else if (observer != null) {
                    mountShutdownLatch.countDown();
                    Slog.i(TAG, "Unmount completed: " + path + ", result code: 0");
                }
            }
        }
    }

    private boolean getUmsEnabling() {
        boolean z;
        synchronized (this.mListeners) {
            z = this.mUmsEnabling;
        }
        return z;
    }

    private void setUmsEnabling(boolean enable) {
        synchronized (this.mListeners) {
            this.mUmsEnabling = enable;
        }
    }

    public boolean isUsbMassStorageConnected() {
        boolean z;
        waitForReady();
        if (getUmsEnabling()) {
            return true;
        }
        synchronized (this.mListeners) {
            z = this.mUmsAvailable;
        }
        return z;
    }

    public void setUsbMassStorageEnabled(boolean enable) {
        waitForReady();
        validatePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        validateUserRestriction("no_usb_file_transfer");
        StorageVolume primary = getPrimaryPhysicalVolume();
        if (primary != null) {
            String path = primary.getPath();
            String vs = getVolumeState(path);
            if (enable && vs.equals("mounted")) {
                setUmsEnabling(enable);
                UmsEnableCallBack umscb = new UmsEnableCallBack(path, "ums", true);
                this.mHandler.sendMessage(this.mHandler.obtainMessage(1, umscb));
                setUmsEnabling(false);
            }
            if (!enable) {
                doShareUnshareVolume(path, "ums", enable);
                if (doMountVolume(path) != 0) {
                    Slog.e(TAG, "Failed to remount " + path + " after disabling share method ums");
                }
            }
        }
    }

    public boolean isUsbMassStorageEnabled() {
        waitForReady();
        StorageVolume primary = getPrimaryPhysicalVolume();
        if (primary != null) {
            return doGetVolumeShared(primary.getPath(), "ums");
        }
        return false;
    }

    public String getVolumeState(String mountPoint) {
        String state;
        synchronized (this.mVolumesLock) {
            state = this.mVolumeStates.get(mountPoint);
            if (state == null) {
                Slog.w(TAG, "getVolumeState(" + mountPoint + "): Unknown volume");
                if (SystemProperties.get("vold.encrypt_progress").length() != 0) {
                    state = "removed";
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        return state;
    }

    public boolean isExternalStorageEmulated() {
        return this.mEmulatedTemplate != null;
    }

    public int mountVolume(String path) {
        validatePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        return doMountVolume(path);
    }

    public void unmountVolume(String path, boolean force, boolean removeEncryption) {
        validatePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        String volState = getVolumeState(path);
        if (!"unmounted".equals(volState) && !"removed".equals(volState) && !"shared".equals(volState) && !"unmountable".equals(volState)) {
            UnmountCallBack ucb = new UnmountCallBack(path, force, removeEncryption);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1, ucb));
        }
    }

    public int formatVolume(String path) {
        validatePermission("android.permission.MOUNT_FORMAT_FILESYSTEMS");
        waitForReady();
        return doFormatVolume(path);
    }

    public int[] getStorageUsers(String path) {
        validatePermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS");
        waitForReady();
        try {
            String[] r = NativeDaemonEvent.filterMessageList(this.mConnector.executeForList(TAG_STORAGE, DatabaseHelper.SoundModelContract.KEY_USERS, path), 112);
            int[] data = new int[r.length];
            for (int i = 0; i < r.length; i++) {
                String[] tok = r[i].split(" ");
                try {
                    data[i] = Integer.parseInt(tok[0]);
                } catch (NumberFormatException e) {
                    Slog.e(TAG, String.format("Error parsing pid %s", tok[0]));
                    return new int[0];
                }
            }
            return data;
        } catch (NativeDaemonConnectorException e2) {
            Slog.e(TAG, "Failed to retrieve storage users list", e2);
            return new int[0];
        }
    }

    private void warnOnNotMounted() {
        StorageVolume primary = getPrimaryPhysicalVolume();
        if (primary != null) {
            boolean mounted = false;
            try {
                mounted = "mounted".equals(getVolumeState(primary.getPath()));
            } catch (IllegalArgumentException e) {
            }
            if (!mounted) {
                Slog.w(TAG, "getSecureContainerList() called when storage not mounted");
            }
        }
    }

    public String[] getSecureContainerList() {
        validatePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        try {
            return NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("asec", "list"), 111);
        } catch (NativeDaemonConnectorException e) {
            return new String[0];
        }
    }

    public int createSecureContainer(String id, int sizeMb, String fstype, String key, int ownerUid, boolean external) {
        validatePermission("android.permission.ASEC_CREATE");
        waitForReady();
        warnOnNotMounted();
        int rc = 0;
        try {
            NativeDaemonConnector nativeDaemonConnector = this.mConnector;
            Object[] objArr = new Object[7];
            objArr[0] = "create";
            objArr[1] = id;
            objArr[2] = Integer.valueOf(sizeMb);
            objArr[3] = fstype;
            objArr[4] = new NativeDaemonConnector.SensitiveArg(key);
            objArr[5] = Integer.valueOf(ownerUid);
            objArr[6] = external ? "1" : "0";
            nativeDaemonConnector.execute("asec", objArr);
        } catch (NativeDaemonConnectorException e) {
            rc = -1;
        }
        if (rc == 0) {
            synchronized (this.mAsecMountSet) {
                this.mAsecMountSet.add(id);
            }
        }
        return rc;
    }

    public int resizeSecureContainer(String id, int sizeMb, String key) {
        validatePermission("android.permission.ASEC_CREATE");
        waitForReady();
        warnOnNotMounted();
        try {
            this.mConnector.execute("asec", "resize", id, Integer.valueOf(sizeMb), new NativeDaemonConnector.SensitiveArg(key));
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return -1;
        }
    }

    public int finalizeSecureContainer(String id) {
        validatePermission("android.permission.ASEC_CREATE");
        warnOnNotMounted();
        try {
            this.mConnector.execute("asec", "finalize", id);
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return -1;
        }
    }

    public int fixPermissionsSecureContainer(String id, int gid, String filename) {
        validatePermission("android.permission.ASEC_CREATE");
        warnOnNotMounted();
        try {
            this.mConnector.execute("asec", "fixperms", id, Integer.valueOf(gid), filename);
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return -1;
        }
    }

    public int destroySecureContainer(String id, boolean force) {
        validatePermission("android.permission.ASEC_DESTROY");
        waitForReady();
        warnOnNotMounted();
        Runtime.getRuntime().gc();
        int rc = 0;
        try {
            NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("asec", "destroy", id);
            if (force) {
                cmd.appendArg("force");
            }
            this.mConnector.execute(cmd);
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 405) {
                rc = -7;
            } else {
                rc = -1;
            }
        }
        if (rc == 0) {
            synchronized (this.mAsecMountSet) {
                if (this.mAsecMountSet.contains(id)) {
                    this.mAsecMountSet.remove(id);
                }
            }
        }
        return rc;
    }

    public int mountSecureContainer(String id, String key, int ownerUid, boolean readOnly) {
        int rc;
        validatePermission("android.permission.ASEC_MOUNT_UNMOUNT");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            if (this.mAsecMountSet.contains(id)) {
                rc = -6;
            } else {
                rc = 0;
                try {
                    NativeDaemonConnector nativeDaemonConnector = this.mConnector;
                    Object[] objArr = new Object[5];
                    objArr[0] = "mount";
                    objArr[1] = id;
                    objArr[2] = new NativeDaemonConnector.SensitiveArg(key);
                    objArr[3] = Integer.valueOf(ownerUid);
                    objArr[4] = readOnly ? "ro" : "rw";
                    nativeDaemonConnector.execute("asec", objArr);
                } catch (NativeDaemonConnectorException e) {
                    int code = e.getCode();
                    if (code != 405) {
                        rc = -1;
                    }
                }
                if (rc == 0) {
                    synchronized (this.mAsecMountSet) {
                        this.mAsecMountSet.add(id);
                    }
                }
            }
        }
        return rc;
    }

    public int unmountSecureContainer(String id, boolean force) {
        int rc;
        validatePermission("android.permission.ASEC_MOUNT_UNMOUNT");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            if (!this.mAsecMountSet.contains(id)) {
                rc = -5;
            } else {
                Runtime.getRuntime().gc();
                rc = 0;
                try {
                    NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("asec", "unmount", id);
                    if (force) {
                        cmd.appendArg("force");
                    }
                    this.mConnector.execute(cmd);
                } catch (NativeDaemonConnectorException e) {
                    int code = e.getCode();
                    if (code == 405) {
                        rc = -7;
                    } else {
                        rc = -1;
                    }
                }
                if (rc == 0) {
                    synchronized (this.mAsecMountSet) {
                        this.mAsecMountSet.remove(id);
                    }
                }
            }
        }
        return rc;
    }

    public boolean isSecureContainerMounted(String id) {
        boolean zContains;
        validatePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            zContains = this.mAsecMountSet.contains(id);
        }
        return zContains;
    }

    public int renameSecureContainer(String oldId, String newId) {
        validatePermission("android.permission.ASEC_RENAME");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mAsecMountSet) {
            if (this.mAsecMountSet.contains(oldId) || this.mAsecMountSet.contains(newId)) {
                return -6;
            }
            try {
                this.mConnector.execute("asec", "rename", oldId, newId);
                return 0;
            } catch (NativeDaemonConnectorException e) {
                return -1;
            }
        }
    }

    public String getSecureContainerPath(String id) {
        validatePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        try {
            NativeDaemonEvent event = this.mConnector.execute("asec", "path", id);
            event.checkCode(211);
            return event.getMessage();
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 406) {
                Slog.i(TAG, String.format("Container '%s' not found", id));
                return null;
            }
            throw new IllegalStateException(String.format("Unexpected response code %d", Integer.valueOf(code)));
        }
    }

    public String getSecureContainerFilesystemPath(String id) {
        validatePermission("android.permission.ASEC_ACCESS");
        waitForReady();
        warnOnNotMounted();
        try {
            NativeDaemonEvent event = this.mConnector.execute("asec", "fspath", id);
            event.checkCode(211);
            return event.getMessage();
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code == 406) {
                Slog.i(TAG, String.format("Container '%s' not found", id));
                return null;
            }
            throw new IllegalStateException(String.format("Unexpected response code %d", Integer.valueOf(code)));
        }
    }

    public void finishMediaUpdate() {
        this.mHandler.sendEmptyMessage(2);
    }

    private boolean isUidOwnerOfPackageOrSystem(String packageName, int callerUid) {
        if (callerUid == 1000) {
            return true;
        }
        if (packageName == null) {
            return false;
        }
        int packageUid = this.mPms.getPackageUid(packageName, UserHandle.getUserId(callerUid));
        return callerUid == packageUid;
    }

    public String getMountedObbPath(String rawPath) {
        ObbState state;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        waitForReady();
        warnOnNotMounted();
        synchronized (this.mObbPathToStateMap) {
            state = this.mObbPathToStateMap.get(rawPath);
        }
        if (state == null) {
            Slog.w(TAG, "Failed to find OBB mounted at " + rawPath);
            return null;
        }
        try {
            NativeDaemonEvent event = this.mConnector.execute("obb", "path", state.voldPath);
            event.checkCode(211);
            return event.getMessage();
        } catch (NativeDaemonConnectorException e) {
            int code = e.getCode();
            if (code != 406) {
                throw new IllegalStateException(String.format("Unexpected response code %d", Integer.valueOf(code)));
            }
            return null;
        }
    }

    public boolean isObbMounted(String rawPath) {
        boolean zContainsKey;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbMounts) {
            zContainsKey = this.mObbPathToStateMap.containsKey(rawPath);
        }
        return zContainsKey;
    }

    public void mountObb(String rawPath, String canonicalPath, String key, IObbActionListener token, int nonce) {
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        Preconditions.checkNotNull(canonicalPath, "canonicalPath cannot be null");
        Preconditions.checkNotNull(token, "token cannot be null");
        int callingUid = Binder.getCallingUid();
        ObbState obbState = new ObbState(rawPath, canonicalPath, callingUid, token, nonce);
        ObbAction action = new MountObbAction(obbState, key, callingUid);
        this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, action));
    }

    public void unmountObb(String rawPath, boolean force, IObbActionListener token, int nonce) {
        ObbState existingState;
        Preconditions.checkNotNull(rawPath, "rawPath cannot be null");
        synchronized (this.mObbPathToStateMap) {
            existingState = this.mObbPathToStateMap.get(rawPath);
        }
        if (existingState != null) {
            int callingUid = Binder.getCallingUid();
            ObbState newState = new ObbState(rawPath, existingState.canonicalPath, callingUid, token, nonce);
            ObbAction action = new UnmountObbAction(newState, force);
            this.mObbActionHandler.sendMessage(this.mObbActionHandler.obtainMessage(1, action));
            return;
        }
        Slog.w(TAG, "Unknown OBB mount at " + rawPath);
    }

    public int getEncryptionState() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        try {
            NativeDaemonEvent event = this.mConnector.execute("cryptfs", "cryptocomplete");
            return Integer.parseInt(event.getMessage());
        } catch (NativeDaemonConnectorException e) {
            Slog.w(TAG, "Error in communicating with cryptfs in validating");
            return -1;
        } catch (NumberFormatException e2) {
            Slog.w(TAG, "Unable to parse result from cryptfs cryptocomplete");
            return -1;
        }
    }

    private String toHex(String password) {
        if (password == null) {
            return new String();
        }
        byte[] bytes = password.getBytes(StandardCharsets.UTF_8);
        return new String(Hex.encodeHex(bytes));
    }

    private String fromHex(String hexPassword) {
        if (hexPassword == null) {
            return null;
        }
        try {
            byte[] bytes = Hex.decodeHex(hexPassword.toCharArray());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (DecoderException e) {
            return null;
        }
    }

    public int decryptStorage(String password) {
        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        try {
            NativeDaemonEvent event = this.mConnector.execute("cryptfs", "checkpw", new NativeDaemonConnector.SensitiveArg(toHex(password)));
            int code = Integer.parseInt(event.getMessage());
            if (code == 0) {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MountService.this.mConnector.execute("cryptfs", "restart");
                        } catch (NativeDaemonConnectorException e) {
                            Slog.e(MountService.TAG, "problem executing in background", e);
                        }
                    }
                }, 1000L);
                return code;
            }
            return code;
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int encryptStorage(int type, String password) {
        if (TextUtils.isEmpty(password) && type != 1) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        try {
            this.mConnector.execute("cryptfs", "enablecrypto", "inplace", CRYPTO_TYPES[type], new NativeDaemonConnector.SensitiveArg(toHex(password)));
            return 0;
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int changeEncryptionPassword(int type, String password) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        waitForReady();
        try {
            NativeDaemonEvent event = this.mConnector.execute("cryptfs", "changepw", CRYPTO_TYPES[type], new NativeDaemonConnector.SensitiveArg(toHex(password)));
            return Integer.parseInt(event.getMessage());
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int verifyEncryptionPassword(String password) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("no permission to access the crypt keeper");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.CRYPT_KEEPER", "no permission to access the crypt keeper");
        if (TextUtils.isEmpty(password)) {
            throw new IllegalArgumentException("password cannot be empty");
        }
        waitForReady();
        try {
            NativeDaemonEvent event = this.mConnector.execute("cryptfs", "verifypw", new NativeDaemonConnector.SensitiveArg(toHex(password)));
            Slog.i(TAG, "cryptfs verifypw => " + event.getMessage());
            return Integer.parseInt(event.getMessage());
        } catch (NativeDaemonConnectorException e) {
            return e.getCode();
        }
    }

    public int getPasswordType() {
        waitForReady();
        try {
            NativeDaemonEvent event = this.mConnector.execute("cryptfs", "getpwtype");
            for (int i = 0; i < CRYPTO_TYPES.length; i++) {
                if (CRYPTO_TYPES[i].equals(event.getMessage())) {
                    return i;
                }
            }
            throw new IllegalStateException("unexpected return from cryptfs");
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void setField(String field, String contents) throws RemoteException {
        waitForReady();
        try {
            this.mConnector.execute("cryptfs", "setfield", field, contents);
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String getField(String field) throws RemoteException {
        waitForReady();
        try {
            String[] contents = NativeDaemonEvent.filterMessageList(this.mConnector.executeForList("cryptfs", "getfield", field), 113);
            String result = new String();
            for (String content : contents) {
                result = result + content;
            }
            return result;
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public String getPassword() throws RemoteException {
        if (!isReady()) {
            return new String();
        }
        try {
            NativeDaemonEvent event = this.mConnector.execute("cryptfs", "getpw");
            return fromHex(event.getMessage());
        } catch (NativeDaemonConnectorException e) {
            throw e.rethrowAsParcelableException();
        }
    }

    public void clearPassword() throws RemoteException {
        if (isReady()) {
            try {
                this.mConnector.execute("cryptfs", "clearpw");
            } catch (NativeDaemonConnectorException e) {
                throw e.rethrowAsParcelableException();
            }
        }
    }

    public int mkdirs(String callingPkg, String appPath) {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        Environment.UserEnvironment userEnv = new Environment.UserEnvironment(userId);
        AppOpsManager appOps = (AppOpsManager) this.mContext.getSystemService("appops");
        appOps.checkPackage(Binder.getCallingUid(), callingPkg);
        try {
            String appPath2 = new File(appPath).getCanonicalPath();
            if (!appPath2.endsWith("/")) {
                appPath2 = appPath2 + "/";
            }
            String voldPath = maybeTranslatePathForVold(appPath2, userEnv.buildExternalStorageAppDataDirs(callingPkg), userEnv.buildExternalStorageAppDataDirsForVold(callingPkg));
            if (voldPath != null) {
                try {
                    this.mConnector.execute("volume", "mkdirs", voldPath);
                    return 0;
                } catch (NativeDaemonConnectorException e) {
                    return e.getCode();
                }
            }
            String voldPath2 = maybeTranslatePathForVold(appPath2, userEnv.buildExternalStorageAppObbDirs(callingPkg), userEnv.buildExternalStorageAppObbDirsForVold(callingPkg));
            if (voldPath2 != null) {
                try {
                    this.mConnector.execute("volume", "mkdirs", voldPath2);
                    return 0;
                } catch (NativeDaemonConnectorException e2) {
                    return e2.getCode();
                }
            }
            String voldPath3 = maybeTranslatePathForVold(appPath2, userEnv.buildExternalStorageAppMediaDirs(callingPkg), userEnv.buildExternalStorageAppMediaDirsForVold(callingPkg));
            if (voldPath3 != null) {
                try {
                    this.mConnector.execute("volume", "mkdirs", voldPath3);
                    return 0;
                } catch (NativeDaemonConnectorException e3) {
                    return e3.getCode();
                }
            }
            throw new SecurityException("Invalid mkdirs path: " + appPath2);
        } catch (IOException e4) {
            Slog.e(TAG, "Failed to resolve " + appPath + ": " + e4);
            return -1;
        }
    }

    public static String maybeTranslatePathForVold(String path, File[] appPaths, File[] voldPaths) {
        if (appPaths.length != voldPaths.length) {
            throw new IllegalStateException("Paths must be 1:1 mapping");
        }
        for (int i = 0; i < appPaths.length; i++) {
            String appPath = appPaths[i].getAbsolutePath() + "/";
            if (path.startsWith(appPath)) {
                String path2 = new File(voldPaths[i], path.substring(appPath.length())).getAbsolutePath();
                if (!path2.endsWith("/")) {
                    return path2 + "/";
                }
                return path2;
            }
        }
        return null;
    }

    public StorageVolume[] getVolumeList() {
        StorageVolume[] res;
        int callingUserId = UserHandle.getCallingUserId();
        boolean accessAll = this.mContext.checkPermission("android.permission.ACCESS_ALL_EXTERNAL_STORAGE", Binder.getCallingPid(), Binder.getCallingUid()) == 0;
        synchronized (this.mVolumesLock) {
            ArrayList<StorageVolume> filtered = Lists.newArrayList();
            for (StorageVolume volume : this.mVolumes) {
                UserHandle owner = volume.getOwner();
                boolean ownerMatch = owner == null || owner.getIdentifier() == callingUserId;
                if (accessAll || ownerMatch) {
                    filtered.add(volume);
                }
            }
            res = (StorageVolume[]) filtered.toArray(new StorageVolume[filtered.size()]);
            int cnt = res.length;
            if (cnt > 1 && res[0] != null && !res[0].isPrimary()) {
                StorageVolume primary = null;
                int i = 1;
                while (true) {
                    if (i >= cnt) {
                        break;
                    }
                    if (res[i] == null || !res[i].isPrimary()) {
                        i++;
                    } else {
                        primary = res[i];
                        break;
                    }
                }
                if (primary != null) {
                    for (int k = i; k > 0; k--) {
                        res[k] = res[k - 1];
                    }
                    res[0] = primary;
                }
            }
        }
        return res;
    }

    private void addObbStateLocked(ObbState obbState) throws RemoteException {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = this.mObbMounts.get(binder);
        if (obbStates == null) {
            obbStates = new ArrayList<>();
            this.mObbMounts.put(binder, obbStates);
        } else {
            for (ObbState o : obbStates) {
                if (o.rawPath.equals(obbState.rawPath)) {
                    throw new IllegalStateException("Attempt to add ObbState twice. This indicates an error in the MountService logic.");
                }
            }
        }
        obbStates.add(obbState);
        try {
            obbState.link();
            this.mObbPathToStateMap.put(obbState.rawPath, obbState);
        } catch (RemoteException e) {
            obbStates.remove(obbState);
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
            throw e;
        }
    }

    private void removeObbStateLocked(ObbState obbState) {
        IBinder binder = obbState.getBinder();
        List<ObbState> obbStates = this.mObbMounts.get(binder);
        if (obbStates != null) {
            if (obbStates.remove(obbState)) {
                obbState.unlink();
            }
            if (obbStates.isEmpty()) {
                this.mObbMounts.remove(binder);
            }
        }
        this.mObbPathToStateMap.remove(obbState.rawPath);
    }

    private class ObbActionHandler extends Handler {
        private final List<ObbAction> mActions;
        private boolean mBound;

        ObbActionHandler(Looper l) {
            super(l);
            this.mBound = false;
            this.mActions = new LinkedList();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ObbAction action = (ObbAction) msg.obj;
                    if (!this.mBound && !connectToService()) {
                        Slog.e(MountService.TAG, "Failed to bind to media container service");
                        action.handleError();
                        return;
                    } else {
                        this.mActions.add(action);
                        return;
                    }
                case 2:
                    if (msg.obj != null) {
                        MountService.this.mContainerService = (IMediaContainerService) msg.obj;
                    }
                    if (MountService.this.mContainerService == null) {
                        Slog.e(MountService.TAG, "Cannot bind to media container service");
                        Iterator<ObbAction> it = this.mActions.iterator();
                        while (it.hasNext()) {
                            it.next().handleError();
                        }
                        this.mActions.clear();
                        return;
                    }
                    if (this.mActions.size() > 0) {
                        ObbAction action2 = this.mActions.get(0);
                        if (action2 != null) {
                            action2.execute(this);
                            return;
                        }
                        return;
                    }
                    Slog.w(MountService.TAG, "Empty queue");
                    return;
                case 3:
                    if (this.mActions.size() > 0) {
                        this.mActions.remove(0);
                    }
                    if (this.mActions.size() != 0) {
                        MountService.this.mObbActionHandler.sendEmptyMessage(2);
                        return;
                    } else {
                        if (this.mBound) {
                            disconnectService();
                            return;
                        }
                        return;
                    }
                case 4:
                    if (this.mActions.size() > 0) {
                        if (this.mBound) {
                            disconnectService();
                        }
                        if (!connectToService()) {
                            Slog.e(MountService.TAG, "Failed to bind to media container service");
                            Iterator<ObbAction> it2 = this.mActions.iterator();
                            while (it2.hasNext()) {
                                it2.next().handleError();
                            }
                            this.mActions.clear();
                            return;
                        }
                        return;
                    }
                    return;
                case 5:
                    String path = (String) msg.obj;
                    synchronized (MountService.this.mObbMounts) {
                        List<ObbState> obbStatesToRemove = new LinkedList<>();
                        for (ObbState state : MountService.this.mObbPathToStateMap.values()) {
                            if (state.canonicalPath.startsWith(path)) {
                                obbStatesToRemove.add(state);
                            }
                        }
                        for (ObbState obbState : obbStatesToRemove) {
                            MountService.this.removeObbStateLocked(obbState);
                            try {
                                obbState.token.onObbResult(obbState.rawPath, obbState.nonce, 2);
                            } catch (RemoteException e) {
                                Slog.i(MountService.TAG, "Couldn't send unmount notification for  OBB: " + obbState.rawPath);
                            }
                        }
                        break;
                    }
                    return;
                default:
                    return;
            }
        }

        private boolean connectToService() {
            Intent service = new Intent().setComponent(MountService.DEFAULT_CONTAINER_COMPONENT);
            if (!MountService.this.mContext.bindService(service, MountService.this.mDefContainerConn, 1)) {
                return false;
            }
            this.mBound = true;
            return true;
        }

        private void disconnectService() {
            MountService.this.mContainerService = null;
            this.mBound = false;
            MountService.this.mContext.unbindService(MountService.this.mDefContainerConn);
        }
    }

    abstract class ObbAction {
        private static final int MAX_RETRIES = 3;
        ObbState mObbState;
        private int mRetries;

        abstract void handleError();

        abstract void handleExecute() throws RemoteException, IOException;

        ObbAction(ObbState obbState) {
            this.mObbState = obbState;
        }

        public void execute(ObbActionHandler handler) {
            try {
                this.mRetries++;
                if (this.mRetries > 3) {
                    Slog.w(MountService.TAG, "Failed to invoke remote methods on default container service. Giving up");
                    MountService.this.mObbActionHandler.sendEmptyMessage(3);
                    handleError();
                } else {
                    handleExecute();
                    MountService.this.mObbActionHandler.sendEmptyMessage(3);
                }
            } catch (RemoteException e) {
                MountService.this.mObbActionHandler.sendEmptyMessage(4);
            } catch (Exception e2) {
                handleError();
                MountService.this.mObbActionHandler.sendEmptyMessage(3);
            }
        }

        protected ObbInfo getObbInfo() throws IOException {
            ObbInfo obbInfo;
            try {
                obbInfo = MountService.this.mContainerService.getObbInfo(this.mObbState.ownerPath);
            } catch (RemoteException e) {
                Slog.d(MountService.TAG, "Couldn't call DefaultContainerService to fetch OBB info for " + this.mObbState.ownerPath);
                obbInfo = null;
            }
            if (obbInfo == null) {
                throw new IOException("Couldn't read OBB file: " + this.mObbState.ownerPath);
            }
            return obbInfo;
        }

        protected void sendNewStatusOrIgnore(int status) {
            if (this.mObbState != null && this.mObbState.token != null) {
                try {
                    this.mObbState.token.onObbResult(this.mObbState.rawPath, this.mObbState.nonce, status);
                } catch (RemoteException e) {
                    Slog.w(MountService.TAG, "MountServiceListener went away while calling onObbStateChanged");
                }
            }
        }
    }

    class MountObbAction extends ObbAction {
        private final int mCallingUid;
        private final String mKey;

        MountObbAction(ObbState obbState, String key, int callingUid) {
            super(obbState);
            this.mKey = key;
            this.mCallingUid = callingUid;
        }

        @Override
        public void handleExecute() throws IOException, RemoteException {
            boolean isMounted;
            String hashedKey;
            MountService.this.waitForReady();
            MountService.this.warnOnNotMounted();
            ObbInfo obbInfo = getObbInfo();
            if (MountService.this.isUidOwnerOfPackageOrSystem(obbInfo.packageName, this.mCallingUid)) {
                synchronized (MountService.this.mObbMounts) {
                    isMounted = MountService.this.mObbPathToStateMap.containsKey(this.mObbState.rawPath);
                }
                if (isMounted) {
                    Slog.w(MountService.TAG, "Attempt to mount OBB which is already mounted: " + obbInfo.filename);
                    sendNewStatusOrIgnore(24);
                    return;
                }
                if (this.mKey == null) {
                    hashedKey = "none";
                } else {
                    try {
                        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                        KeySpec ks = new PBEKeySpec(this.mKey.toCharArray(), obbInfo.salt, 1024, 128);
                        SecretKey key = factory.generateSecret(ks);
                        BigInteger bi = new BigInteger(key.getEncoded());
                        hashedKey = bi.toString(16);
                    } catch (NoSuchAlgorithmException e) {
                        Slog.e(MountService.TAG, "Could not load PBKDF2 algorithm", e);
                        sendNewStatusOrIgnore(20);
                        return;
                    } catch (InvalidKeySpecException e2) {
                        Slog.e(MountService.TAG, "Invalid key spec when loading PBKDF2 algorithm", e2);
                        sendNewStatusOrIgnore(20);
                        return;
                    }
                }
                int rc = 0;
                try {
                    MountService.this.mConnector.execute("obb", "mount", this.mObbState.voldPath, new NativeDaemonConnector.SensitiveArg(hashedKey), Integer.valueOf(this.mObbState.ownerGid));
                } catch (NativeDaemonConnectorException e3) {
                    int code = e3.getCode();
                    if (code != 405) {
                        rc = -1;
                    }
                }
                if (rc == 0) {
                    synchronized (MountService.this.mObbMounts) {
                        MountService.this.addObbStateLocked(this.mObbState);
                    }
                    sendNewStatusOrIgnore(1);
                    return;
                }
                Slog.e(MountService.TAG, "Couldn't mount OBB file: " + rc);
                sendNewStatusOrIgnore(21);
                return;
            }
            Slog.w(MountService.TAG, "Denied attempt to mount OBB " + obbInfo.filename + " which is owned by " + obbInfo.packageName);
            sendNewStatusOrIgnore(25);
        }

        @Override
        public void handleError() {
            sendNewStatusOrIgnore(20);
        }

        public String toString() {
            return "MountObbAction{" + this.mObbState + '}';
        }
    }

    class UnmountObbAction extends ObbAction {
        private final boolean mForceUnmount;

        UnmountObbAction(ObbState obbState, boolean force) {
            super(obbState);
            this.mForceUnmount = force;
        }

        @Override
        public void handleExecute() throws IOException {
            ObbState existingState;
            MountService.this.waitForReady();
            MountService.this.warnOnNotMounted();
            getObbInfo();
            synchronized (MountService.this.mObbMounts) {
                existingState = (ObbState) MountService.this.mObbPathToStateMap.get(this.mObbState.rawPath);
            }
            if (existingState == null) {
                sendNewStatusOrIgnore(23);
                return;
            }
            if (existingState.ownerGid != this.mObbState.ownerGid) {
                Slog.w(MountService.TAG, "Permission denied attempting to unmount OBB " + existingState.rawPath + " (owned by GID " + existingState.ownerGid + ")");
                sendNewStatusOrIgnore(25);
                return;
            }
            int rc = 0;
            try {
                NativeDaemonConnector.Command cmd = new NativeDaemonConnector.Command("obb", "unmount", this.mObbState.voldPath);
                if (this.mForceUnmount) {
                    cmd.appendArg("force");
                }
                MountService.this.mConnector.execute(cmd);
            } catch (NativeDaemonConnectorException e) {
                int code = e.getCode();
                if (code == 405) {
                    rc = -7;
                } else if (code == 406) {
                    rc = 0;
                } else {
                    rc = -1;
                }
            }
            if (rc == 0) {
                synchronized (MountService.this.mObbMounts) {
                    MountService.this.removeObbStateLocked(existingState);
                }
                sendNewStatusOrIgnore(2);
                return;
            }
            Slog.w(MountService.TAG, "Could not unmount OBB: " + existingState);
            sendNewStatusOrIgnore(22);
        }

        @Override
        public void handleError() {
            sendNewStatusOrIgnore(20);
        }

        public String toString() {
            return "UnmountObbAction{" + this.mObbState + ",force=" + this.mForceUnmount + '}';
        }
    }

    public static String buildObbPath(String canonicalPath, int userId, boolean forVold) {
        String path;
        if (Environment.isExternalStorageEmulated()) {
            String path2 = canonicalPath.toString();
            Environment.UserEnvironment userEnv = new Environment.UserEnvironment(userId);
            String externalPath = userEnv.getExternalStorageDirectory().getAbsolutePath();
            String legacyExternalPath = Environment.getLegacyExternalStorageDirectory().getAbsolutePath();
            if (path2.startsWith(externalPath)) {
                path = path2.substring(externalPath.length() + 1);
            } else if (path2.startsWith(legacyExternalPath)) {
                path = path2.substring(legacyExternalPath.length() + 1);
            } else {
                return canonicalPath;
            }
            if (path.startsWith("Android/obb")) {
                String path3 = path.substring("Android/obb".length() + 1);
                if (forVold) {
                    return new File(Environment.getEmulatedStorageObbSource(), path3).getAbsolutePath();
                }
                Environment.UserEnvironment ownerEnv = new Environment.UserEnvironment(0);
                return new File(ownerEnv.buildExternalStorageAndroidObbDirs()[0], path3).getAbsolutePath();
            }
            if (forVold) {
                return new File(Environment.getEmulatedStorageSource(userId), path).getAbsolutePath();
            }
            return new File(userEnv.getExternalDirsForApp()[0], path).getAbsolutePath();
        }
        return canonicalPath;
    }

    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", TAG);
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(writer, "  ", 160);
        synchronized (this.mObbMounts) {
            indentingPrintWriter.println("mObbMounts:");
            indentingPrintWriter.increaseIndent();
            for (Map.Entry<IBinder, List<ObbState>> e : this.mObbMounts.entrySet()) {
                indentingPrintWriter.println(e.getKey() + ":");
                indentingPrintWriter.increaseIndent();
                List<ObbState> obbStates = e.getValue();
                for (ObbState obbState : obbStates) {
                    indentingPrintWriter.println(obbState);
                }
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.println("mObbPathToStateMap:");
            indentingPrintWriter.increaseIndent();
            for (Map.Entry<String, ObbState> e2 : this.mObbPathToStateMap.entrySet()) {
                indentingPrintWriter.print(e2.getKey());
                indentingPrintWriter.print(" -> ");
                indentingPrintWriter.println(e2.getValue());
            }
            indentingPrintWriter.decreaseIndent();
        }
        synchronized (this.mVolumesLock) {
            indentingPrintWriter.println();
            indentingPrintWriter.println("mVolumes:");
            indentingPrintWriter.increaseIndent();
            for (StorageVolume volume : this.mVolumes) {
                indentingPrintWriter.println(volume);
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.println("Current state: " + this.mVolumeStates.get(volume.getPath()));
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.println();
        indentingPrintWriter.println("mConnection:");
        indentingPrintWriter.increaseIndent();
        this.mConnector.dump(fd, indentingPrintWriter, args);
        indentingPrintWriter.decreaseIndent();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        indentingPrintWriter.println();
        indentingPrintWriter.print("Last maintenance: ");
        indentingPrintWriter.println(sdf.format(new Date(this.mLastMaintenance)));
    }

    @Override
    public void monitor() {
        if (this.mConnector != null) {
            this.mConnector.monitor();
        }
    }
}
