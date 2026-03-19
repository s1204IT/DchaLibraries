package com.android.media.remotedisplay;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.IRemoteDisplayCallback;
import android.media.IRemoteDisplayProvider;
import android.media.RemoteDisplayState;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import java.util.Collection;

public abstract class RemoteDisplayProvider {
    public static final int DISCOVERY_MODE_ACTIVE = 2;
    public static final int DISCOVERY_MODE_NONE = 0;
    public static final int DISCOVERY_MODE_PASSIVE = 1;
    private static final int MSG_ADJUST_VOLUME = 6;
    private static final int MSG_CONNECT = 3;
    private static final int MSG_DISCONNECT = 4;
    private static final int MSG_SET_CALLBACK = 1;
    private static final int MSG_SET_DISCOVERY_MODE = 2;
    private static final int MSG_SET_VOLUME = 5;
    public static final String SERVICE_INTERFACE = "com.android.media.remotedisplay.RemoteDisplayProvider";
    private IRemoteDisplayCallback mCallback;
    private final Context mContext;
    private final ProviderHandler mHandler;
    private PendingIntent mSettingsPendingIntent;
    private final ArrayMap<String, RemoteDisplay> mDisplays = new ArrayMap<>();
    private int mDiscoveryMode = 0;
    private final ProviderStub mStub = new ProviderStub();

    public RemoteDisplayProvider(Context context) {
        this.mContext = context;
        this.mHandler = new ProviderHandler(context.getMainLooper());
    }

    public final Context getContext() {
        return this.mContext;
    }

    public IBinder getBinder() {
        return this.mStub;
    }

    public void onDiscoveryModeChanged(int i) {
    }

    public void onConnect(RemoteDisplay remoteDisplay) {
    }

    public void onDisconnect(RemoteDisplay remoteDisplay) {
    }

    public void onSetVolume(RemoteDisplay remoteDisplay, int i) {
    }

    public void onAdjustVolume(RemoteDisplay remoteDisplay, int i) {
    }

    public int getDiscoveryMode() {
        return this.mDiscoveryMode;
    }

    public Collection<RemoteDisplay> getDisplays() {
        return this.mDisplays.values();
    }

    public void addDisplay(RemoteDisplay remoteDisplay) {
        if (remoteDisplay == null || this.mDisplays.containsKey(remoteDisplay.getId())) {
            throw new IllegalArgumentException("display");
        }
        this.mDisplays.put(remoteDisplay.getId(), remoteDisplay);
        publishState();
    }

    public void updateDisplay(RemoteDisplay remoteDisplay) {
        if (remoteDisplay == null || this.mDisplays.get(remoteDisplay.getId()) != remoteDisplay) {
            throw new IllegalArgumentException("display");
        }
        publishState();
    }

    public void removeDisplay(RemoteDisplay remoteDisplay) {
        if (remoteDisplay == null || this.mDisplays.get(remoteDisplay.getId()) != remoteDisplay) {
            throw new IllegalArgumentException("display");
        }
        this.mDisplays.remove(remoteDisplay.getId());
        publishState();
    }

    public RemoteDisplay findRemoteDisplay(String str) {
        return this.mDisplays.get(str);
    }

    public PendingIntent getSettingsPendingIntent() {
        if (this.mSettingsPendingIntent == null) {
            Intent intent = new Intent("android.settings.CAST_SETTINGS");
            intent.setFlags(337641472);
            this.mSettingsPendingIntent = PendingIntent.getActivity(this.mContext, 0, intent, 0, null);
        }
        return this.mSettingsPendingIntent;
    }

    void setCallback(IRemoteDisplayCallback iRemoteDisplayCallback) {
        this.mCallback = iRemoteDisplayCallback;
        publishState();
    }

    void setDiscoveryMode(int i) {
        if (this.mDiscoveryMode != i) {
            this.mDiscoveryMode = i;
            onDiscoveryModeChanged(i);
        }
    }

    void publishState() {
        if (this.mCallback != null) {
            RemoteDisplayState remoteDisplayState = new RemoteDisplayState();
            int size = this.mDisplays.size();
            for (int i = 0; i < size; i++) {
                remoteDisplayState.displays.add(this.mDisplays.valueAt(i).getInfo());
            }
            try {
                this.mCallback.onStateChanged(remoteDisplayState);
            } catch (RemoteException e) {
            }
        }
    }

    final class ProviderStub extends IRemoteDisplayProvider.Stub {
        ProviderStub() {
        }

        public void setCallback(IRemoteDisplayCallback iRemoteDisplayCallback) {
            RemoteDisplayProvider.this.mHandler.obtainMessage(1, iRemoteDisplayCallback).sendToTarget();
        }

        public void setDiscoveryMode(int i) {
            RemoteDisplayProvider.this.mHandler.obtainMessage(2, i, 0).sendToTarget();
        }

        public void connect(String str) {
            RemoteDisplayProvider.this.mHandler.obtainMessage(3, str).sendToTarget();
        }

        public void disconnect(String str) {
            RemoteDisplayProvider.this.mHandler.obtainMessage(4, str).sendToTarget();
        }

        public void setVolume(String str, int i) {
            RemoteDisplayProvider.this.mHandler.obtainMessage(RemoteDisplayProvider.MSG_SET_VOLUME, i, 0, str).sendToTarget();
        }

        public void adjustVolume(String str, int i) {
            RemoteDisplayProvider.this.mHandler.obtainMessage(RemoteDisplayProvider.MSG_ADJUST_VOLUME, i, 0, str).sendToTarget();
        }
    }

    final class ProviderHandler extends Handler {
        public ProviderHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    RemoteDisplayProvider.this.setCallback((IRemoteDisplayCallback) message.obj);
                    break;
                case 2:
                    RemoteDisplayProvider.this.setDiscoveryMode(message.arg1);
                    break;
                case 3:
                    RemoteDisplay remoteDisplayFindRemoteDisplay = RemoteDisplayProvider.this.findRemoteDisplay((String) message.obj);
                    if (remoteDisplayFindRemoteDisplay != null) {
                        RemoteDisplayProvider.this.onConnect(remoteDisplayFindRemoteDisplay);
                    }
                    break;
                case 4:
                    RemoteDisplay remoteDisplayFindRemoteDisplay2 = RemoteDisplayProvider.this.findRemoteDisplay((String) message.obj);
                    if (remoteDisplayFindRemoteDisplay2 != null) {
                        RemoteDisplayProvider.this.onDisconnect(remoteDisplayFindRemoteDisplay2);
                    }
                    break;
                case RemoteDisplayProvider.MSG_SET_VOLUME:
                    RemoteDisplay remoteDisplayFindRemoteDisplay3 = RemoteDisplayProvider.this.findRemoteDisplay((String) message.obj);
                    if (remoteDisplayFindRemoteDisplay3 != null) {
                        RemoteDisplayProvider.this.onSetVolume(remoteDisplayFindRemoteDisplay3, message.arg1);
                    }
                    break;
                case RemoteDisplayProvider.MSG_ADJUST_VOLUME:
                    RemoteDisplay remoteDisplayFindRemoteDisplay4 = RemoteDisplayProvider.this.findRemoteDisplay((String) message.obj);
                    if (remoteDisplayFindRemoteDisplay4 != null) {
                        RemoteDisplayProvider.this.onAdjustVolume(remoteDisplayFindRemoteDisplay4, message.arg1);
                    }
                    break;
            }
        }
    }
}
