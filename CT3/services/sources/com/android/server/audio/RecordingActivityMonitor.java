package com.android.server.audio;

import android.media.AudioFormat;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.IRecordingConfigDispatcher;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public final class RecordingActivityMonitor implements AudioSystem.AudioRecordingCallback {
    public static final String TAG = "AudioService.RecordingActivityMonitor";
    private ArrayList<RecMonitorClient> mClients = new ArrayList<>();
    private HashMap<Integer, AudioRecordingConfiguration> mRecordConfigs = new HashMap<>();

    RecordingActivityMonitor() {
        RecMonitorClient.sMonitor = this;
    }

    public void onRecordingConfigurationChanged(int event, int session, int source, int[] recordingInfo) {
        List<AudioRecordingConfiguration> configs;
        if (MediaRecorder.isSystemOnlyAudioSource(source) || (configs = updateSnapshot(event, session, source, recordingInfo)) == null) {
            return;
        }
        synchronized (this.mClients) {
            Iterator<RecMonitorClient> clientIterator = this.mClients.iterator();
            while (clientIterator.hasNext()) {
                try {
                    clientIterator.next().mDispatcherCb.dispatchRecordingConfigChange(configs);
                } catch (RemoteException e) {
                    Log.w(TAG, "Could not call dispatchRecordingConfigChange() on client", e);
                }
            }
        }
    }

    void initMonitor() {
        AudioSystem.setRecordingCallback(this);
    }

    void registerRecordingCallback(IRecordingConfigDispatcher rcdb) {
        if (rcdb == null) {
            return;
        }
        synchronized (this.mClients) {
            RecMonitorClient rmc = new RecMonitorClient(rcdb);
            if (rmc.init()) {
                this.mClients.add(rmc);
            }
        }
    }

    void unregisterRecordingCallback(IRecordingConfigDispatcher rcdb) {
        if (rcdb == null) {
            return;
        }
        synchronized (this.mClients) {
            Iterator<RecMonitorClient> clientIterator = this.mClients.iterator();
            while (true) {
                if (!clientIterator.hasNext()) {
                    break;
                }
                RecMonitorClient rmc = clientIterator.next();
                if (rcdb.equals(rmc.mDispatcherCb)) {
                    break;
                }
            }
        }
    }

    List<AudioRecordingConfiguration> getActiveRecordingConfigurations() {
        ArrayList arrayList;
        synchronized (this.mRecordConfigs) {
            arrayList = new ArrayList(this.mRecordConfigs.values());
        }
        return arrayList;
    }

    private List<AudioRecordingConfiguration> updateSnapshot(int event, int session, int source, int[] recordingInfo) {
        boolean configChanged;
        ArrayList arrayList;
        synchronized (this.mRecordConfigs) {
            switch (event) {
                case 0:
                    configChanged = this.mRecordConfigs.remove(new Integer(session)) != null;
                    break;
                case 1:
                    AudioFormat clientFormat = new AudioFormat.Builder().setEncoding(recordingInfo[0]).setChannelMask(recordingInfo[1]).setSampleRate(recordingInfo[2]).build();
                    AudioFormat deviceFormat = new AudioFormat.Builder().setEncoding(recordingInfo[3]).setChannelMask(recordingInfo[4]).setSampleRate(recordingInfo[5]).build();
                    int patchHandle = recordingInfo[6];
                    Integer sessionKey = new Integer(session);
                    if (!this.mRecordConfigs.containsKey(sessionKey)) {
                        this.mRecordConfigs.put(sessionKey, new AudioRecordingConfiguration(session, source, clientFormat, deviceFormat, patchHandle));
                        configChanged = true;
                    } else {
                        AudioRecordingConfiguration updatedConfig = new AudioRecordingConfiguration(session, source, clientFormat, deviceFormat, patchHandle);
                        if (!updatedConfig.equals(this.mRecordConfigs.get(sessionKey))) {
                            this.mRecordConfigs.remove(sessionKey);
                            this.mRecordConfigs.put(sessionKey, updatedConfig);
                            configChanged = true;
                        } else {
                            configChanged = false;
                        }
                    }
                    break;
                default:
                    Log.e(TAG, String.format("Unknown event %d for session %d, source %d", Integer.valueOf(event), Integer.valueOf(session), Integer.valueOf(source)));
                    configChanged = false;
                    break;
            }
            arrayList = configChanged ? new ArrayList(this.mRecordConfigs.values()) : null;
        }
        return arrayList;
    }

    private static final class RecMonitorClient implements IBinder.DeathRecipient {
        static RecordingActivityMonitor sMonitor;
        final IRecordingConfigDispatcher mDispatcherCb;

        RecMonitorClient(IRecordingConfigDispatcher rcdb) {
            this.mDispatcherCb = rcdb;
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
}
