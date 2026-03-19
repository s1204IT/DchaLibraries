package com.android.server.storage;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.ArrayMap;
import android.util.DataUnit;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.utils.PriorityDump;
import dalvik.system.VMRuntime;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class DeviceStorageMonitorService extends SystemService {
    private static final long DEFAULT_CHECK_INTERVAL = 60000;
    public static final String EXTRA_SEQUENCE = "seq";
    private static final int MSG_CHECK = 1;
    static final int OPTION_FORCE_UPDATE = 1;
    static final String SERVICE = "devicestoragemonitor";
    private static final String TAG = "DeviceStorageMonitorService";
    private static final String TV_NOTIFICATION_CHANNEL_ID = "devicestoragemonitor.tv";
    private CacheFileDeletedObserver mCacheFileDeletedObserver;
    private volatile int mForceLevel;
    private final Handler mHandler;
    private final HandlerThread mHandlerThread;
    private final DeviceStorageMonitorInternal mLocalService;
    private NotificationManager mNotifManager;
    private final Binder mRemoteService;
    private final AtomicInteger mSeq;
    private final ArrayMap<UUID, State> mStates;
    private static final long DEFAULT_LOG_DELTA_BYTES = DataUnit.MEBIBYTES.toBytes(64);
    private static final long BOOT_IMAGE_STORAGE_REQUIREMENT = DataUnit.MEBIBYTES.toBytes(250);

    private static class State {
        private static final int LEVEL_FULL = 2;
        private static final int LEVEL_LOW = 1;
        private static final int LEVEL_NORMAL = 0;
        private static final int LEVEL_UNKNOWN = -1;
        public long lastUsableBytes;
        public int level;

        private State() {
            this.level = 0;
            this.lastUsableBytes = JobStatus.NO_LATEST_RUNTIME;
        }

        private static boolean isEntering(int i, int i2, int i3) {
            return i3 >= i && (i2 < i || i2 == -1);
        }

        private static boolean isLeaving(int i, int i2, int i3) {
            return i3 < i && (i2 >= i || i2 == -1);
        }

        private static String levelToString(int i) {
            switch (i) {
                case -1:
                    return "UNKNOWN";
                case 0:
                    return PriorityDump.PRIORITY_ARG_NORMAL;
                case 1:
                    return "LOW";
                case 2:
                    return "FULL";
                default:
                    return Integer.toString(i);
            }
        }
    }

    private State findOrCreateState(UUID uuid) {
        State state = this.mStates.get(uuid);
        if (state == null) {
            State state2 = new State();
            this.mStates.put(uuid, state2);
            return state2;
        }
        return state;
    }

    private void check() {
        int i;
        StorageManager storageManager = (StorageManager) getContext().getSystemService(StorageManager.class);
        int i2 = this.mSeq.get();
        if (storageManager != null) {
            for (VolumeInfo volumeInfo : storageManager.getWritablePrivateVolumes()) {
                File path = volumeInfo.getPath();
                long storageFullBytes = storageManager.getStorageFullBytes(path);
                long storageLowBytes = storageManager.getStorageLowBytes(path);
                if (path.getUsableSpace() < (3 * storageLowBytes) / 2) {
                    try {
                        ((PackageManagerService) ServiceManager.getService(Settings.ATTR_PACKAGE)).freeStorage(volumeInfo.getFsUuid(), storageLowBytes * 2, 0);
                    } catch (IOException e) {
                        Slog.w(TAG, e);
                    }
                }
                UUID uuidConvert = StorageManager.convert(volumeInfo.getFsUuid());
                State stateFindOrCreateState = findOrCreateState(uuidConvert);
                long totalSpace = path.getTotalSpace();
                long usableSpace = path.getUsableSpace();
                int i3 = stateFindOrCreateState.level;
                if (this.mForceLevel != -1) {
                    i3 = -1;
                    i = this.mForceLevel;
                } else if (usableSpace <= storageFullBytes) {
                    i = 2;
                } else {
                    i = (usableSpace > storageLowBytes && (!StorageManager.UUID_DEFAULT.equals(uuidConvert) || isBootImageOnDisk() || usableSpace >= BOOT_IMAGE_STORAGE_REQUIREMENT)) ? 0 : 1;
                }
                if (Math.abs(stateFindOrCreateState.lastUsableBytes - usableSpace) > DEFAULT_LOG_DELTA_BYTES || i3 != i) {
                    EventLogTags.writeStorageState(uuidConvert.toString(), i3, i, usableSpace, totalSpace);
                    stateFindOrCreateState.lastUsableBytes = usableSpace;
                }
                updateNotifications(volumeInfo, i3, i);
                updateBroadcasts(volumeInfo, i3, i, i2);
                stateFindOrCreateState.level = i;
            }
        } else {
            Slog.w(TAG, "StorageManager service not ready !!!");
        }
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 60000L);
        }
    }

    public DeviceStorageMonitorService(Context context) {
        super(context);
        this.mSeq = new AtomicInteger(1);
        this.mForceLevel = -1;
        this.mStates = new ArrayMap<>();
        this.mLocalService = new DeviceStorageMonitorInternal() {
            @Override
            public void checkMemory() {
                DeviceStorageMonitorService.this.mHandler.removeMessages(1);
                DeviceStorageMonitorService.this.mHandler.obtainMessage(1).sendToTarget();
            }

            @Override
            public boolean isMemoryLow() {
                return Environment.getDataDirectory().getUsableSpace() < getMemoryLowThreshold();
            }

            @Override
            public long getMemoryLowThreshold() {
                return ((StorageManager) DeviceStorageMonitorService.this.getContext().getSystemService(StorageManager.class)).getStorageLowBytes(Environment.getDataDirectory());
            }
        };
        this.mRemoteService = new Binder() {
            @Override
            protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
                if (DumpUtils.checkDumpPermission(DeviceStorageMonitorService.this.getContext(), DeviceStorageMonitorService.TAG, printWriter)) {
                    DeviceStorageMonitorService.this.dumpImpl(fileDescriptor, printWriter, strArr);
                }
            }

            public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
                DeviceStorageMonitorService.this.new Shell().exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
            }
        };
        this.mHandlerThread = new HandlerThread(TAG, 10);
        this.mHandlerThread.start();
        this.mHandler = new Handler(this.mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    DeviceStorageMonitorService.this.check();
                }
            }
        };
    }

    private static boolean isBootImageOnDisk() {
        for (String str : InstructionSets.getAllDexCodeInstructionSets()) {
            if (!VMRuntime.isBootClassPathOnDisk(str)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onStart() {
        Context context = getContext();
        this.mNotifManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        this.mCacheFileDeletedObserver = new CacheFileDeletedObserver();
        this.mCacheFileDeletedObserver.startWatching();
        if (context.getPackageManager().hasSystemFeature("android.software.leanback")) {
            this.mNotifManager.createNotificationChannel(new NotificationChannel(TV_NOTIFICATION_CHANNEL_ID, context.getString(R.string.biometric_face_not_recognized), 4));
        }
        publishBinderService(SERVICE, this.mRemoteService);
        publishLocalService(DeviceStorageMonitorInternal.class, this.mLocalService);
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1).sendToTarget();
    }

    class Shell extends ShellCommand {
        Shell() {
        }

        public int onCommand(String str) {
            return DeviceStorageMonitorService.this.onShellCommand(this, str);
        }

        public void onHelp() {
            DeviceStorageMonitorService.dumpHelp(getOutPrintWriter());
        }
    }

    int parseOptions(Shell shell) {
        int i = 0;
        while (true) {
            String nextOption = shell.getNextOption();
            if (nextOption != null) {
                if ("-f".equals(nextOption)) {
                    i |= 1;
                }
            } else {
                return i;
            }
        }
    }

    int onShellCommand(Shell shell, String str) {
        byte b;
        if (str == null) {
            return shell.handleDefaultCommands(str);
        }
        PrintWriter outPrintWriter = shell.getOutPrintWriter();
        int iHashCode = str.hashCode();
        if (iHashCode != 108404047) {
            if (iHashCode != 1526871410) {
                b = (iHashCode == 1692300408 && str.equals("force-not-low")) ? (byte) 1 : (byte) -1;
            } else if (str.equals("force-low")) {
                b = 0;
            }
        } else if (str.equals("reset")) {
            b = 2;
        }
        switch (b) {
            case 0:
                int options = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                this.mForceLevel = 1;
                int iIncrementAndGet = this.mSeq.incrementAndGet();
                if ((options & 1) != 0) {
                    this.mHandler.removeMessages(1);
                    this.mHandler.obtainMessage(1).sendToTarget();
                    outPrintWriter.println(iIncrementAndGet);
                }
                return 0;
            case 1:
                int options2 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                this.mForceLevel = 0;
                int iIncrementAndGet2 = this.mSeq.incrementAndGet();
                if ((options2 & 1) != 0) {
                    this.mHandler.removeMessages(1);
                    this.mHandler.obtainMessage(1).sendToTarget();
                    outPrintWriter.println(iIncrementAndGet2);
                }
                return 0;
            case 2:
                int options3 = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                this.mForceLevel = -1;
                int iIncrementAndGet3 = this.mSeq.incrementAndGet();
                if ((options3 & 1) != 0) {
                    this.mHandler.removeMessages(1);
                    this.mHandler.obtainMessage(1).sendToTarget();
                    outPrintWriter.println(iIncrementAndGet3);
                }
                return 0;
            default:
                return shell.handleDefaultCommands(str);
        }
    }

    static void dumpHelp(PrintWriter printWriter) {
        printWriter.println("Device storage monitor service (devicestoragemonitor) commands:");
        printWriter.println("  help");
        printWriter.println("    Print this help text.");
        printWriter.println("  force-low [-f]");
        printWriter.println("    Force storage to be low, freezing storage state.");
        printWriter.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        printWriter.println("  force-not-low [-f]");
        printWriter.println("    Force storage to not be low, freezing storage state.");
        printWriter.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        printWriter.println("  reset [-f]");
        printWriter.println("    Unfreeze storage state, returning to current real values.");
        printWriter.println("    -f: force a storage change broadcast be sent, prints new sequence.");
    }

    void dumpImpl(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        if (strArr == null || strArr.length == 0 || "-a".equals(strArr[0])) {
            indentingPrintWriter.println("Known volumes:");
            indentingPrintWriter.increaseIndent();
            for (int i = 0; i < this.mStates.size(); i++) {
                UUID uuidKeyAt = this.mStates.keyAt(i);
                State stateValueAt = this.mStates.valueAt(i);
                if (StorageManager.UUID_DEFAULT.equals(uuidKeyAt)) {
                    indentingPrintWriter.println("Default:");
                } else {
                    indentingPrintWriter.println(uuidKeyAt + ":");
                }
                indentingPrintWriter.increaseIndent();
                indentingPrintWriter.printPair("level", State.levelToString(stateValueAt.level));
                indentingPrintWriter.printPair("lastUsableBytes", Long.valueOf(stateValueAt.lastUsableBytes));
                indentingPrintWriter.println();
                indentingPrintWriter.decreaseIndent();
            }
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.println();
            indentingPrintWriter.printPair("mSeq", Integer.valueOf(this.mSeq.get()));
            indentingPrintWriter.printPair("mForceState", State.levelToString(this.mForceLevel));
            indentingPrintWriter.println();
            indentingPrintWriter.println();
            return;
        }
        new Shell().exec(this.mRemoteService, null, fileDescriptor, null, strArr, null, new ResultReceiver(null));
    }

    private void updateNotifications(VolumeInfo volumeInfo, int i, int i2) {
        CharSequence text;
        Context context = getContext();
        UUID uuidConvert = StorageManager.convert(volumeInfo.getFsUuid());
        if (!State.isEntering(1, i, i2)) {
            if (State.isLeaving(1, i, i2)) {
                this.mNotifManager.cancelAsUser(uuidConvert.toString(), 23, UserHandle.ALL);
                return;
            }
            return;
        }
        Intent intent = new Intent("android.os.storage.action.MANAGE_STORAGE");
        intent.putExtra("android.os.storage.extra.UUID", uuidConvert);
        intent.addFlags(268435456);
        CharSequence text2 = context.getText(R.string.db_default_sync_mode);
        boolean zEquals = StorageManager.UUID_DEFAULT.equals(uuidConvert);
        int i3 = R.string.days;
        if (zEquals) {
            if (!isBootImageOnDisk()) {
                i3 = R.string.db_default_journal_mode;
            }
            text = context.getText(i3);
        } else {
            text = context.getText(R.string.days);
        }
        Notification notificationBuild = new Notification.Builder(context, SystemNotificationChannels.ALERTS).setSmallIcon(R.drawable.pointer_grabbing).setTicker(text2).setColor(context.getColor(R.color.car_colorPrimary)).setContentTitle(text2).setContentText(text).setContentIntent(PendingIntent.getActivityAsUser(context, 0, intent, 0, null, UserHandle.CURRENT)).setStyle(new Notification.BigTextStyle().bigText(text)).setVisibility(1).setCategory("sys").extend(new Notification.TvExtender().setChannelId(TV_NOTIFICATION_CHANNEL_ID)).build();
        notificationBuild.flags |= 32;
        this.mNotifManager.notifyAsUser(uuidConvert.toString(), 23, notificationBuild, UserHandle.ALL);
    }

    private void updateBroadcasts(VolumeInfo volumeInfo, int i, int i2, int i3) {
        if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, volumeInfo.getFsUuid())) {
            return;
        }
        Intent intentPutExtra = new Intent("android.intent.action.DEVICE_STORAGE_LOW").addFlags(85983232).putExtra(EXTRA_SEQUENCE, i3);
        Intent intentPutExtra2 = new Intent("android.intent.action.DEVICE_STORAGE_OK").addFlags(85983232).putExtra(EXTRA_SEQUENCE, i3);
        if (!State.isEntering(1, i, i2)) {
            if (State.isLeaving(1, i, i2)) {
                getContext().removeStickyBroadcastAsUser(intentPutExtra, UserHandle.ALL);
                getContext().sendBroadcastAsUser(intentPutExtra2, UserHandle.ALL);
            }
        } else {
            getContext().sendStickyBroadcastAsUser(intentPutExtra, UserHandle.ALL);
        }
        Intent intentPutExtra3 = new Intent("android.intent.action.DEVICE_STORAGE_FULL").addFlags(67108864).putExtra(EXTRA_SEQUENCE, i3);
        Intent intentPutExtra4 = new Intent("android.intent.action.DEVICE_STORAGE_NOT_FULL").addFlags(67108864).putExtra(EXTRA_SEQUENCE, i3);
        if (!State.isEntering(2, i, i2)) {
            if (State.isLeaving(2, i, i2)) {
                getContext().removeStickyBroadcastAsUser(intentPutExtra3, UserHandle.ALL);
                getContext().sendBroadcastAsUser(intentPutExtra4, UserHandle.ALL);
                return;
            }
            return;
        }
        getContext().sendStickyBroadcastAsUser(intentPutExtra3, UserHandle.ALL);
    }

    private static class CacheFileDeletedObserver extends FileObserver {
        public CacheFileDeletedObserver() {
            super(Environment.getDownloadCacheDirectory().getAbsolutePath(), 512);
        }

        @Override
        public void onEvent(int i, String str) {
            EventLogTags.writeCacheFileDeleted(str);
        }
    }
}
