package com.android.server.am;

import android.R;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.os.BatteryStatsImpl;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.notification.NotificationManagerInternal;
import com.android.server.pm.DumpState;
import com.android.server.pm.Settings;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ServiceRecord extends Binder implements ComponentName.WithComponentName {
    static final int MAX_DELIVERY_COUNT = 3;
    static final int MAX_DONE_EXECUTING_COUNT = 6;
    private static final String TAG = "ActivityManager";
    final ActivityManagerService ams;
    ProcessRecord app;
    ApplicationInfo appInfo;
    boolean callStart;
    int crashCount;
    boolean createdFromFg;
    boolean delayed;
    boolean delayedStop;
    long destroyTime;
    boolean destroying;
    boolean executeFg;
    int executeNesting;
    long executingStart;
    final boolean exported;
    boolean fgRequired;
    boolean fgWaiting;
    int foregroundId;
    Notification foregroundNoti;
    final Intent.FilterComparison intent;
    boolean isForeground;
    ProcessRecord isolatedProc;
    private int lastStartId;
    final ComponentName name;
    long nextRestartTime;
    final String packageName;
    final String permission;
    final String processName;
    int restartCount;
    long restartDelay;
    long restartTime;
    ServiceState restartTracker;
    final Runnable restarter;
    final ServiceInfo serviceInfo;
    final String shortName;
    boolean startRequested;
    long startingBgTimeout;
    final BatteryStatsImpl.Uid.Pkg.Serv stats;
    boolean stopIfKilled;
    String stringName;
    int totalRestartCount;
    ServiceState tracker;
    final int userId;
    boolean whitelistManager;
    final ArrayMap<Intent.FilterComparison, IntentBindRecord> bindings = new ArrayMap<>();
    final ArrayMap<IBinder, ArrayList<ConnectionRecord>> connections = new ArrayMap<>();
    final ArrayList<StartItem> deliveredStarts = new ArrayList<>();
    final ArrayList<StartItem> pendingStarts = new ArrayList<>();
    final long createRealTime = SystemClock.elapsedRealtime();
    long lastActivity = SystemClock.uptimeMillis();

    static class StartItem {
        final int callingId;
        long deliveredTime;
        int deliveryCount;
        int doneExecutingCount;
        final int id;
        final Intent intent;
        final ActivityManagerService.NeededUriGrants neededGrants;
        final ServiceRecord sr;
        String stringName;
        final boolean taskRemoved;
        UriPermissionOwner uriPermissions;

        StartItem(ServiceRecord serviceRecord, boolean z, int i, Intent intent, ActivityManagerService.NeededUriGrants neededUriGrants, int i2) {
            this.sr = serviceRecord;
            this.taskRemoved = z;
            this.id = i;
            this.intent = intent;
            this.neededGrants = neededUriGrants;
            this.callingId = i2;
        }

        UriPermissionOwner getUriPermissionsLocked() {
            if (this.uriPermissions == null) {
                this.uriPermissions = new UriPermissionOwner(this.sr.ams, this);
            }
            return this.uriPermissions;
        }

        void removeUriPermissionsLocked() {
            if (this.uriPermissions != null) {
                this.uriPermissions.removeUriPermissionsLocked();
                this.uriPermissions = null;
            }
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j, long j2) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1120986464257L, this.id);
            ProtoUtils.toDuration(protoOutputStream, 1146756268034L, this.deliveredTime, j2);
            protoOutputStream.write(1120986464259L, this.deliveryCount);
            protoOutputStream.write(1120986464260L, this.doneExecutingCount);
            if (this.intent != null) {
                this.intent.writeToProto(protoOutputStream, 1146756268037L, true, true, true, false);
            }
            if (this.neededGrants != null) {
                this.neededGrants.writeToProto(protoOutputStream, 1146756268038L);
            }
            if (this.uriPermissions != null) {
                this.uriPermissions.writeToProto(protoOutputStream, 1146756268039L);
            }
            protoOutputStream.end(jStart);
        }

        public String toString() {
            if (this.stringName != null) {
                return this.stringName;
            }
            StringBuilder sb = new StringBuilder(128);
            sb.append("ServiceRecord{");
            sb.append(Integer.toHexString(System.identityHashCode(this.sr)));
            sb.append(' ');
            sb.append(this.sr.shortName);
            sb.append(" StartItem ");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(" id=");
            sb.append(this.id);
            sb.append('}');
            String string = sb.toString();
            this.stringName = string;
            return string;
        }
    }

    void dumpStartList(PrintWriter printWriter, String str, List<StartItem> list, long j) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            StartItem startItem = list.get(i);
            printWriter.print(str);
            printWriter.print("#");
            printWriter.print(i);
            printWriter.print(" id=");
            printWriter.print(startItem.id);
            if (j != 0) {
                printWriter.print(" dur=");
                TimeUtils.formatDuration(startItem.deliveredTime, j, printWriter);
            }
            if (startItem.deliveryCount != 0) {
                printWriter.print(" dc=");
                printWriter.print(startItem.deliveryCount);
            }
            if (startItem.doneExecutingCount != 0) {
                printWriter.print(" dxc=");
                printWriter.print(startItem.doneExecutingCount);
            }
            printWriter.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printWriter.print(str);
            printWriter.print("  intent=");
            if (startItem.intent != null) {
                printWriter.println(startItem.intent.toString());
            } else {
                printWriter.println("null");
            }
            if (startItem.neededGrants != null) {
                printWriter.print(str);
                printWriter.print("  neededGrants=");
                printWriter.println(startItem.neededGrants);
            }
            if (startItem.uriPermissions != null) {
                startItem.uriPermissions.dump(printWriter, str);
            }
        }
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long j2;
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.shortName);
        protoOutputStream.write(1133871366146L, this.app != null);
        if (this.app != null) {
            protoOutputStream.write(1120986464259L, this.app.pid);
        }
        if (this.intent != null) {
            this.intent.getIntent().writeToProto(protoOutputStream, 1146756268036L, false, true, false, true);
        }
        protoOutputStream.write(1138166333445L, this.packageName);
        protoOutputStream.write(1138166333446L, this.processName);
        protoOutputStream.write(1138166333447L, this.permission);
        long jUptimeMillis = SystemClock.uptimeMillis();
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (this.appInfo != null) {
            long jStart2 = protoOutputStream.start(1146756268040L);
            protoOutputStream.write(1138166333441L, this.appInfo.sourceDir);
            if (!Objects.equals(this.appInfo.sourceDir, this.appInfo.publicSourceDir)) {
                protoOutputStream.write(1138166333442L, this.appInfo.publicSourceDir);
            }
            protoOutputStream.write(1138166333443L, this.appInfo.dataDir);
            protoOutputStream.end(jStart2);
        }
        if (this.app != null) {
            this.app.writeToProto(protoOutputStream, 1146756268041L);
        }
        if (this.isolatedProc != null) {
            this.isolatedProc.writeToProto(protoOutputStream, 1146756268042L);
        }
        protoOutputStream.write(1133871366155L, this.whitelistManager);
        protoOutputStream.write(1133871366156L, this.delayed);
        if (this.isForeground || this.foregroundId != 0) {
            long jStart3 = protoOutputStream.start(1146756268045L);
            protoOutputStream.write(1120986464257L, this.foregroundId);
            this.foregroundNoti.writeToProto(protoOutputStream, 1146756268034L);
            protoOutputStream.end(jStart3);
        }
        ProtoUtils.toDuration(protoOutputStream, 1146756268046L, this.createRealTime, jElapsedRealtime);
        ProtoUtils.toDuration(protoOutputStream, 1146756268047L, this.startingBgTimeout, jUptimeMillis);
        ProtoUtils.toDuration(protoOutputStream, 1146756268048L, this.lastActivity, jUptimeMillis);
        ProtoUtils.toDuration(protoOutputStream, 1146756268049L, this.restartTime, jUptimeMillis);
        protoOutputStream.write(1133871366162L, this.createdFromFg);
        if (this.startRequested || this.delayedStop || this.lastStartId != 0) {
            long jStart4 = protoOutputStream.start(1146756268051L);
            protoOutputStream.write(1133871366145L, this.startRequested);
            j2 = 1133871366146L;
            protoOutputStream.write(1133871366146L, this.delayedStop);
            protoOutputStream.write(1133871366147L, this.stopIfKilled);
            protoOutputStream.write(1120986464261L, this.lastStartId);
            protoOutputStream.end(jStart4);
        } else {
            j2 = 1133871366146L;
        }
        if (this.executeNesting != 0) {
            long jStart5 = protoOutputStream.start(1146756268052L);
            protoOutputStream.write(1120986464257L, this.executeNesting);
            protoOutputStream.write(j2, this.executeFg);
            ProtoUtils.toDuration(protoOutputStream, 1146756268035L, this.executingStart, jUptimeMillis);
            protoOutputStream.end(jStart5);
        }
        if (this.destroying || this.destroyTime != 0) {
            ProtoUtils.toDuration(protoOutputStream, 1146756268053L, this.destroyTime, jUptimeMillis);
        }
        if (this.crashCount != 0 || this.restartCount != 0 || this.restartDelay != 0 || this.nextRestartTime != 0) {
            long jStart6 = protoOutputStream.start(1146756268054L);
            protoOutputStream.write(1120986464257L, this.restartCount);
            ProtoUtils.toDuration(protoOutputStream, 1146756268034L, this.restartDelay, jUptimeMillis);
            ProtoUtils.toDuration(protoOutputStream, 1146756268035L, this.nextRestartTime, jUptimeMillis);
            protoOutputStream.write(1120986464260L, this.crashCount);
            protoOutputStream.end(jStart6);
        }
        if (this.deliveredStarts.size() > 0) {
            int size = this.deliveredStarts.size();
            for (int i = 0; i < size; i++) {
                this.deliveredStarts.get(i).writeToProto(protoOutputStream, 2246267895831L, jUptimeMillis);
            }
        }
        if (this.pendingStarts.size() > 0) {
            int size2 = this.pendingStarts.size();
            for (int i2 = 0; i2 < size2; i2++) {
                this.pendingStarts.get(i2).writeToProto(protoOutputStream, 2246267895832L, jUptimeMillis);
            }
        }
        if (this.bindings.size() > 0) {
            int size3 = this.bindings.size();
            for (int i3 = 0; i3 < size3; i3++) {
                this.bindings.valueAt(i3).writeToProto(protoOutputStream, 2246267895833L);
            }
        }
        if (this.connections.size() > 0) {
            int size4 = this.connections.size();
            for (int i4 = 0; i4 < size4; i4++) {
                ArrayList<ConnectionRecord> arrayListValueAt = this.connections.valueAt(i4);
                for (int i5 = 0; i5 < arrayListValueAt.size(); i5++) {
                    arrayListValueAt.get(i5).writeToProto(protoOutputStream, 2246267895834L);
                }
            }
        }
        protoOutputStream.end(jStart);
    }

    void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("intent={");
        printWriter.print(this.intent.getIntent().toShortString(false, true, false, true));
        printWriter.println('}');
        printWriter.print(str);
        printWriter.print("packageName=");
        printWriter.println(this.packageName);
        printWriter.print(str);
        printWriter.print("processName=");
        printWriter.println(this.processName);
        if (this.permission != null) {
            printWriter.print(str);
            printWriter.print("permission=");
            printWriter.println(this.permission);
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (this.appInfo != null) {
            printWriter.print(str);
            printWriter.print("baseDir=");
            printWriter.println(this.appInfo.sourceDir);
            if (!Objects.equals(this.appInfo.sourceDir, this.appInfo.publicSourceDir)) {
                printWriter.print(str);
                printWriter.print("resDir=");
                printWriter.println(this.appInfo.publicSourceDir);
            }
            printWriter.print(str);
            printWriter.print("dataDir=");
            printWriter.println(this.appInfo.dataDir);
        }
        printWriter.print(str);
        printWriter.print("app=");
        printWriter.println(this.app);
        if (this.isolatedProc != null) {
            printWriter.print(str);
            printWriter.print("isolatedProc=");
            printWriter.println(this.isolatedProc);
        }
        if (this.whitelistManager) {
            printWriter.print(str);
            printWriter.print("whitelistManager=");
            printWriter.println(this.whitelistManager);
        }
        if (this.delayed) {
            printWriter.print(str);
            printWriter.print("delayed=");
            printWriter.println(this.delayed);
        }
        if (this.isForeground || this.foregroundId != 0) {
            printWriter.print(str);
            printWriter.print("isForeground=");
            printWriter.print(this.isForeground);
            printWriter.print(" foregroundId=");
            printWriter.print(this.foregroundId);
            printWriter.print(" foregroundNoti=");
            printWriter.println(this.foregroundNoti);
        }
        printWriter.print(str);
        printWriter.print("createTime=");
        TimeUtils.formatDuration(this.createRealTime, jElapsedRealtime, printWriter);
        printWriter.print(" startingBgTimeout=");
        TimeUtils.formatDuration(this.startingBgTimeout, jUptimeMillis, printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("lastActivity=");
        TimeUtils.formatDuration(this.lastActivity, jUptimeMillis, printWriter);
        printWriter.print(" restartTime=");
        TimeUtils.formatDuration(this.restartTime, jUptimeMillis, printWriter);
        printWriter.print(" createdFromFg=");
        printWriter.println(this.createdFromFg);
        if (this.startRequested || this.delayedStop || this.lastStartId != 0) {
            printWriter.print(str);
            printWriter.print("startRequested=");
            printWriter.print(this.startRequested);
            printWriter.print(" delayedStop=");
            printWriter.print(this.delayedStop);
            printWriter.print(" stopIfKilled=");
            printWriter.print(this.stopIfKilled);
            printWriter.print(" callStart=");
            printWriter.print(this.callStart);
            printWriter.print(" lastStartId=");
            printWriter.println(this.lastStartId);
        }
        if (this.executeNesting != 0) {
            printWriter.print(str);
            printWriter.print("executeNesting=");
            printWriter.print(this.executeNesting);
            printWriter.print(" executeFg=");
            printWriter.print(this.executeFg);
            printWriter.print(" executingStart=");
            TimeUtils.formatDuration(this.executingStart, jUptimeMillis, printWriter);
            printWriter.println();
        }
        if (this.destroying || this.destroyTime != 0) {
            printWriter.print(str);
            printWriter.print("destroying=");
            printWriter.print(this.destroying);
            printWriter.print(" destroyTime=");
            TimeUtils.formatDuration(this.destroyTime, jUptimeMillis, printWriter);
            printWriter.println();
        }
        if (this.crashCount != 0 || this.restartCount != 0 || this.restartDelay != 0 || this.nextRestartTime != 0) {
            printWriter.print(str);
            printWriter.print("restartCount=");
            printWriter.print(this.restartCount);
            printWriter.print(" restartDelay=");
            TimeUtils.formatDuration(this.restartDelay, jUptimeMillis, printWriter);
            printWriter.print(" nextRestartTime=");
            TimeUtils.formatDuration(this.nextRestartTime, jUptimeMillis, printWriter);
            printWriter.print(" crashCount=");
            printWriter.println(this.crashCount);
        }
        if (this.deliveredStarts.size() > 0) {
            printWriter.print(str);
            printWriter.println("Delivered Starts:");
            dumpStartList(printWriter, str, this.deliveredStarts, jUptimeMillis);
        }
        if (this.pendingStarts.size() > 0) {
            printWriter.print(str);
            printWriter.println("Pending Starts:");
            dumpStartList(printWriter, str, this.pendingStarts, 0L);
        }
        if (this.bindings.size() > 0) {
            printWriter.print(str);
            printWriter.println("Bindings:");
            for (int i = 0; i < this.bindings.size(); i++) {
                IntentBindRecord intentBindRecordValueAt = this.bindings.valueAt(i);
                printWriter.print(str);
                printWriter.print("* IntentBindRecord{");
                printWriter.print(Integer.toHexString(System.identityHashCode(intentBindRecordValueAt)));
                if ((intentBindRecordValueAt.collectFlags() & 1) != 0) {
                    printWriter.append(" CREATE");
                }
                printWriter.println("}:");
                intentBindRecordValueAt.dumpInService(printWriter, str + "  ");
            }
        }
        if (this.connections.size() > 0) {
            printWriter.print(str);
            printWriter.println("All Connections:");
            for (int i2 = 0; i2 < this.connections.size(); i2++) {
                ArrayList<ConnectionRecord> arrayListValueAt = this.connections.valueAt(i2);
                for (int i3 = 0; i3 < arrayListValueAt.size(); i3++) {
                    printWriter.print(str);
                    printWriter.print("  ");
                    printWriter.println(arrayListValueAt.get(i3));
                }
            }
        }
    }

    ServiceRecord(ActivityManagerService activityManagerService, BatteryStatsImpl.Uid.Pkg.Serv serv, ComponentName componentName, Intent.FilterComparison filterComparison, ServiceInfo serviceInfo, boolean z, Runnable runnable) {
        this.ams = activityManagerService;
        this.stats = serv;
        this.name = componentName;
        this.shortName = componentName.flattenToShortString();
        this.intent = filterComparison;
        this.serviceInfo = serviceInfo;
        this.appInfo = serviceInfo.applicationInfo;
        this.packageName = serviceInfo.applicationInfo.packageName;
        this.processName = serviceInfo.processName;
        this.permission = serviceInfo.permission;
        this.exported = serviceInfo.exported;
        this.restarter = runnable;
        this.userId = UserHandle.getUserId(this.appInfo.uid);
        this.createdFromFg = z;
    }

    public ServiceState getTracker() {
        if (this.tracker != null) {
            return this.tracker;
        }
        if ((this.serviceInfo.applicationInfo.flags & 8) == 0) {
            this.tracker = this.ams.mProcessStats.getServiceStateLocked(this.serviceInfo.packageName, this.serviceInfo.applicationInfo.uid, this.serviceInfo.applicationInfo.versionCode, this.serviceInfo.processName, this.serviceInfo.name);
            this.tracker.applyNewOwner(this);
        }
        return this.tracker;
    }

    public void forceClearTracker() {
        if (this.tracker != null) {
            this.tracker.clearCurrentOwner(this, true);
            this.tracker = null;
        }
    }

    public void makeRestarting(int i, long j) {
        if (this.restartTracker == null) {
            if ((this.serviceInfo.applicationInfo.flags & 8) == 0) {
                this.restartTracker = this.ams.mProcessStats.getServiceStateLocked(this.serviceInfo.packageName, this.serviceInfo.applicationInfo.uid, this.serviceInfo.applicationInfo.versionCode, this.serviceInfo.processName, this.serviceInfo.name);
            }
            if (this.restartTracker == null) {
                return;
            }
        }
        this.restartTracker.setRestarting(true, i, j);
    }

    public AppBindRecord retrieveAppBindingLocked(Intent intent, ProcessRecord processRecord) {
        Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
        IntentBindRecord intentBindRecord = this.bindings.get(filterComparison);
        if (intentBindRecord == null) {
            intentBindRecord = new IntentBindRecord(this, filterComparison);
            this.bindings.put(filterComparison, intentBindRecord);
        }
        AppBindRecord appBindRecord = intentBindRecord.apps.get(processRecord);
        if (appBindRecord != null) {
            return appBindRecord;
        }
        AppBindRecord appBindRecord2 = new AppBindRecord(this, intentBindRecord, processRecord);
        intentBindRecord.apps.put(processRecord, appBindRecord2);
        return appBindRecord2;
    }

    public boolean hasAutoCreateConnections() {
        int size = this.connections.size() - 1;
        while (true) {
            if (size < 0) {
                return false;
            }
            ArrayList<ConnectionRecord> arrayListValueAt = this.connections.valueAt(size);
            for (int i = 0; i < arrayListValueAt.size(); i++) {
                if ((arrayListValueAt.get(i).flags & 1) != 0) {
                    return true;
                }
            }
            size--;
        }
    }

    public void updateWhitelistManager() {
        this.whitelistManager = false;
        for (int size = this.connections.size() - 1; size >= 0; size--) {
            ArrayList<ConnectionRecord> arrayListValueAt = this.connections.valueAt(size);
            for (int i = 0; i < arrayListValueAt.size(); i++) {
                if ((arrayListValueAt.get(i).flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0) {
                    this.whitelistManager = true;
                    return;
                }
            }
        }
    }

    public void resetRestartCounter() {
        this.restartCount = 0;
        this.restartDelay = 0L;
        this.restartTime = 0L;
    }

    public StartItem findDeliveredStart(int i, boolean z, boolean z2) {
        int size = this.deliveredStarts.size();
        for (int i2 = 0; i2 < size; i2++) {
            StartItem startItem = this.deliveredStarts.get(i2);
            if (startItem.id == i && startItem.taskRemoved == z) {
                if (z2) {
                    this.deliveredStarts.remove(i2);
                }
                return startItem;
            }
        }
        return null;
    }

    public int getLastStartId() {
        return this.lastStartId;
    }

    public int makeNextStartId() {
        this.lastStartId++;
        if (this.lastStartId < 1) {
            this.lastStartId = 1;
        }
        return this.lastStartId;
    }

    public void postNotification() {
        final int i = this.appInfo.uid;
        final int i2 = this.app.pid;
        if (this.foregroundId != 0 && this.foregroundNoti != null) {
            final String str = this.packageName;
            final int i3 = this.foregroundId;
            final Notification notification = this.foregroundNoti;
            this.ams.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int i4;
                    NotificationManagerInternal notificationManagerInternal = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
                    if (notificationManagerInternal == null) {
                        return;
                    }
                    Notification notificationBuild = notification;
                    try {
                        if (notificationBuild.getSmallIcon() == null) {
                            Slog.v(ServiceRecord.TAG, "Attempted to start a foreground service (" + ServiceRecord.this.name + ") with a broken notification (no icon: " + notificationBuild + ")");
                            CharSequence charSequenceLoadLabel = ServiceRecord.this.appInfo.loadLabel(ServiceRecord.this.ams.mContext.getPackageManager());
                            if (charSequenceLoadLabel == null) {
                                charSequenceLoadLabel = ServiceRecord.this.appInfo.packageName;
                            }
                            try {
                                Notification.Builder builder = new Notification.Builder(ServiceRecord.this.ams.mContext.createPackageContextAsUser(ServiceRecord.this.appInfo.packageName, 0, new UserHandle(ServiceRecord.this.userId)), notificationBuild.getChannelId());
                                builder.setSmallIcon(ServiceRecord.this.appInfo.icon);
                                builder.setFlag(64, true);
                                Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                intent.setData(Uri.fromParts(Settings.ATTR_PACKAGE, ServiceRecord.this.appInfo.packageName, null));
                                PendingIntent activityAsUser = PendingIntent.getActivityAsUser(ServiceRecord.this.ams.mContext, 0, intent, 134217728, null, UserHandle.of(ServiceRecord.this.userId));
                                builder.setColor(ServiceRecord.this.ams.mContext.getColor(R.color.car_colorPrimary));
                                builder.setContentTitle(ServiceRecord.this.ams.mContext.getString(R.string.PERSOSUBSTATE_RUIM_RUIM_PUK_SUCCESS, charSequenceLoadLabel));
                                builder.setContentText(ServiceRecord.this.ams.mContext.getString(R.string.PERSOSUBSTATE_RUIM_RUIM_PUK_IN_PROGRESS, charSequenceLoadLabel));
                                builder.setContentIntent(activityAsUser);
                                if (BenesseExtension.getDchaState() == 0) {
                                    builder.setContentIntent(activityAsUser);
                                }
                                notificationBuild = builder.build();
                            } catch (PackageManager.NameNotFoundException e) {
                            }
                        }
                        if (notificationManagerInternal.getNotificationChannel(str, i, notificationBuild.getChannelId()) == null) {
                            try {
                                i4 = ServiceRecord.this.ams.mContext.getPackageManager().getApplicationInfoAsUser(ServiceRecord.this.appInfo.packageName, 0, ServiceRecord.this.userId).targetSdkVersion;
                            } catch (PackageManager.NameNotFoundException e2) {
                                i4 = 27;
                            }
                            if (i4 >= 27) {
                                throw new RuntimeException("invalid channel for service notification: " + ServiceRecord.this.foregroundNoti);
                            }
                        }
                        if (notificationBuild.getSmallIcon() == null) {
                            throw new RuntimeException("invalid service notification: " + ServiceRecord.this.foregroundNoti);
                        }
                        notificationManagerInternal.enqueueNotification(str, str, i, i2, null, i3, notificationBuild, ServiceRecord.this.userId);
                        ServiceRecord.this.foregroundNoti = notificationBuild;
                    } catch (RuntimeException e3) {
                        Slog.w(ServiceRecord.TAG, "Error showing notification for service", e3);
                        ServiceRecord.this.ams.mServices.killMisbehavingService(this, i, i2, str);
                    }
                }
            });
        }
    }

    public void cancelNotification() {
        final String str = this.packageName;
        final int i = this.foregroundId;
        this.ams.mHandler.post(new Runnable() {
            @Override
            public void run() {
                INotificationManager service = NotificationManager.getService();
                if (service == null) {
                    return;
                }
                try {
                    service.cancelNotificationWithTag(str, (String) null, i, ServiceRecord.this.userId);
                } catch (RemoteException e) {
                } catch (RuntimeException e2) {
                    Slog.w(ServiceRecord.TAG, "Error canceling notification for service", e2);
                }
            }
        });
    }

    public void stripForegroundServiceFlagFromNotification() {
        if (this.foregroundId == 0) {
            return;
        }
        final int i = this.foregroundId;
        final int i2 = this.userId;
        final String str = this.packageName;
        this.ams.mHandler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManagerInternal notificationManagerInternal = (NotificationManagerInternal) LocalServices.getService(NotificationManagerInternal.class);
                if (notificationManagerInternal == null) {
                    return;
                }
                notificationManagerInternal.removeForegroundServiceFlagFromNotification(str, i, i2);
            }
        });
    }

    public void clearDeliveredStartsLocked() {
        for (int size = this.deliveredStarts.size() - 1; size >= 0; size--) {
            this.deliveredStarts.get(size).removeUriPermissionsLocked();
        }
        this.deliveredStarts.clear();
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ServiceRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" u");
        sb.append(this.userId);
        sb.append(' ');
        sb.append(this.shortName);
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }

    public ComponentName getComponentName() {
        return this.name;
    }
}
