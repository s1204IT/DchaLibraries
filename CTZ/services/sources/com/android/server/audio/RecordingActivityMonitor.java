package com.android.server.audio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.IRecordingConfigDispatcher;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.audio.AudioEventLogger;
import com.android.server.backup.BackupManagerConstants;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class RecordingActivityMonitor implements AudioSystem.AudioRecordingCallback {
    public static final String TAG = "AudioService.RecordingActivityMonitor";
    private static final AudioEventLogger sEventLogger = new AudioEventLogger(50, "recording activity as reported through AudioSystem.AudioRecordingCallback");
    private final PackageManager mPackMan;
    private ArrayList<RecMonitorClient> mClients = new ArrayList<>();
    private boolean mHasPublicClients = false;
    private HashMap<Integer, AudioRecordingConfiguration> mRecordConfigs = new HashMap<>();

    RecordingActivityMonitor(Context context) {
        RecMonitorClient.sMonitor = this;
        this.mPackMan = context.getPackageManager();
    }

    public void onRecordingConfigurationChanged(int i, int i2, int i3, int i4, int[] iArr, String str) {
        List<AudioRecordingConfiguration> listUpdateSnapshot;
        ArrayList<AudioRecordingConfiguration> arrayList;
        if (!MediaRecorder.isSystemOnlyAudioSource(i4) && (listUpdateSnapshot = updateSnapshot(i, i2, i3, i4, iArr)) != null) {
            synchronized (this.mClients) {
                if (this.mHasPublicClients) {
                    arrayList = anonymizeForPublicConsumption(listUpdateSnapshot);
                } else {
                    arrayList = new ArrayList<>();
                }
                for (RecMonitorClient recMonitorClient : this.mClients) {
                    try {
                        if (recMonitorClient.mIsPrivileged) {
                            recMonitorClient.mDispatcherCb.dispatchRecordingConfigChange(listUpdateSnapshot);
                        } else {
                            recMonitorClient.mDispatcherCb.dispatchRecordingConfigChange(arrayList);
                        }
                    } catch (RemoteException e) {
                        Log.w(TAG, "Could not call dispatchRecordingConfigChange() on client", e);
                    }
                }
            }
        }
    }

    protected void dump(PrintWriter printWriter) {
        printWriter.println("\nRecordActivityMonitor dump time: " + DateFormat.getTimeInstance().format(new Date()));
        synchronized (this.mRecordConfigs) {
            Iterator<AudioRecordingConfiguration> it = this.mRecordConfigs.values().iterator();
            while (it.hasNext()) {
                it.next().dump(printWriter);
            }
        }
        printWriter.println("\n");
        sEventLogger.dump(printWriter);
    }

    private ArrayList<AudioRecordingConfiguration> anonymizeForPublicConsumption(List<AudioRecordingConfiguration> list) {
        ArrayList<AudioRecordingConfiguration> arrayList = new ArrayList<>();
        Iterator<AudioRecordingConfiguration> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(AudioRecordingConfiguration.anonymizedCopy(it.next()));
        }
        return arrayList;
    }

    void initMonitor() {
        AudioSystem.setRecordingCallback(this);
    }

    void registerRecordingCallback(IRecordingConfigDispatcher iRecordingConfigDispatcher, boolean z) {
        if (iRecordingConfigDispatcher == null) {
            return;
        }
        synchronized (this.mClients) {
            RecMonitorClient recMonitorClient = new RecMonitorClient(iRecordingConfigDispatcher, z);
            if (recMonitorClient.init()) {
                if (!z) {
                    this.mHasPublicClients = true;
                }
                this.mClients.add(recMonitorClient);
            }
        }
    }

    void unregisterRecordingCallback(IRecordingConfigDispatcher iRecordingConfigDispatcher) {
        if (iRecordingConfigDispatcher == null) {
            return;
        }
        synchronized (this.mClients) {
            Iterator<RecMonitorClient> it = this.mClients.iterator();
            boolean z = false;
            while (it.hasNext()) {
                RecMonitorClient next = it.next();
                if (iRecordingConfigDispatcher.equals(next.mDispatcherCb)) {
                    next.release();
                    it.remove();
                } else if (!next.mIsPrivileged) {
                    z = true;
                }
            }
            this.mHasPublicClients = z;
        }
    }

    List<AudioRecordingConfiguration> getActiveRecordingConfigurations(boolean z) {
        synchronized (this.mRecordConfigs) {
            try {
                if (z) {
                    return new ArrayList(this.mRecordConfigs.values());
                }
                return anonymizeForPublicConsumption(new ArrayList(this.mRecordConfigs.values()));
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private List<AudioRecordingConfiguration> updateSnapshot(int i, int i2, int i3, int i4, int[] iArr) {
        String str;
        ArrayList arrayList;
        synchronized (this.mRecordConfigs) {
            switch (i) {
                case 0:
                    z = this.mRecordConfigs.remove(new Integer(i3)) != null;
                    if (z) {
                        sEventLogger.log(new RecordingEvent(i, i2, i3, i4, null));
                    }
                    break;
                case 1:
                    AudioFormat audioFormatBuild = new AudioFormat.Builder().setEncoding(iArr[0]).setChannelMask(iArr[1]).setSampleRate(iArr[2]).build();
                    AudioFormat audioFormatBuild2 = new AudioFormat.Builder().setEncoding(iArr[3]).setChannelMask(iArr[4]).setSampleRate(iArr[5]).build();
                    int i5 = iArr[6];
                    Integer num = new Integer(i3);
                    String[] packagesForUid = this.mPackMan.getPackagesForUid(i2);
                    if (packagesForUid != null && packagesForUid.length > 0) {
                        str = packagesForUid[0];
                    } else {
                        str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    }
                    String str2 = str;
                    AudioRecordingConfiguration audioRecordingConfiguration = new AudioRecordingConfiguration(i2, i3, i4, audioFormatBuild, audioFormatBuild2, i5, str2);
                    if (this.mRecordConfigs.containsKey(num)) {
                        if (!audioRecordingConfiguration.equals(this.mRecordConfigs.get(num))) {
                            this.mRecordConfigs.remove(num);
                            this.mRecordConfigs.put(num, audioRecordingConfiguration);
                        } else if (z) {
                            sEventLogger.log(new RecordingEvent(i, i2, i3, i4, str2));
                        }
                    } else {
                        this.mRecordConfigs.put(num, audioRecordingConfiguration);
                    }
                    z = true;
                    if (z) {
                    }
                    break;
                default:
                    Log.e(TAG, String.format("Unknown event %d for session %d, source %d", Integer.valueOf(i), Integer.valueOf(i3), Integer.valueOf(i4)));
                    break;
            }
            if (z) {
                arrayList = new ArrayList(this.mRecordConfigs.values());
            } else {
                arrayList = null;
            }
        }
        return arrayList;
    }

    private static final class RecMonitorClient implements IBinder.DeathRecipient {
        static RecordingActivityMonitor sMonitor;
        final IRecordingConfigDispatcher mDispatcherCb;
        final boolean mIsPrivileged;

        RecMonitorClient(IRecordingConfigDispatcher iRecordingConfigDispatcher, boolean z) {
            this.mDispatcherCb = iRecordingConfigDispatcher;
            this.mIsPrivileged = z;
        }

        @Override
        public void binderDied() {
            Log.w(RecordingActivityMonitor.TAG, "client died");
            sMonitor.unregisterRecordingCallback(this.mDispatcherCb);
        }

        boolean init() {
            try {
                this.mDispatcherCb.asBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException e) {
                Log.w(RecordingActivityMonitor.TAG, "Could not link to client death", e);
                return false;
            }
        }

        void release() {
            this.mDispatcherCb.asBinder().unlinkToDeath(this, 0);
        }
    }

    private static final class RecordingEvent extends AudioEventLogger.Event {
        private final int mClientUid;
        private final String mPackName;
        private final int mRecEvent;
        private final int mSession;
        private final int mSource;

        RecordingEvent(int i, int i2, int i3, int i4, String str) {
            this.mRecEvent = i;
            this.mClientUid = i2;
            this.mSession = i3;
            this.mSource = i4;
            this.mPackName = str;
        }

        @Override
        public String eventToString() {
            String str;
            StringBuilder sb = new StringBuilder("rec ");
            sb.append(this.mRecEvent == 1 ? "start" : "stop ");
            sb.append(" uid:");
            sb.append(this.mClientUid);
            sb.append(" session:");
            sb.append(this.mSession);
            sb.append(" src:");
            sb.append(MediaRecorder.toLogFriendlyAudioSource(this.mSource));
            if (this.mPackName == null) {
                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            } else {
                str = " pack:" + this.mPackName;
            }
            sb.append(str);
            return sb.toString();
        }
    }
}
