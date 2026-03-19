package com.android.server;

import android.R;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.UEventObserver;
import android.util.Log;
import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.input.InputManagerService;
import com.android.server.pm.DumpState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class WiredAccessoryManager implements InputManagerService.WiredAccessoryCallbacks {
    private static final int BIT_HDMI_AUDIO = 16;
    private static final int BIT_HEADSET = 1;
    private static final int BIT_HEADSET_NO_MIC = 2;
    private static final int BIT_LINEOUT = 32;
    private static final int BIT_USB_HEADSET_ANLG = 4;
    private static final int BIT_USB_HEADSET_DGTL = 8;
    private static final boolean LOG = true;
    private static final int MSG_NEW_DEVICE_STATE = 1;
    private static final int MSG_SYSTEM_READY = 2;
    private static final String NAME_H2W = "h2w";
    private static final String NAME_HDMI = "hdmi";
    private static final String NAME_HDMI_AUDIO = "hdmi_audio";
    private static final String NAME_USB_AUDIO = "usb_audio";
    private static final int SUPPORTED_HEADSETS = 63;
    private static final String TAG = WiredAccessoryManager.class.getSimpleName();
    private final AudioManager mAudioManager;
    private int mHeadsetState;
    private final InputManagerService mInputManager;
    private final WiredAccessoryObserver mObserver;
    private int mSwitchValues;
    private final boolean mUseDevInputEventForAudioJack;
    private final PowerManager.WakeLock mWakeLock;
    private final Object mLock = new Object();
    private final Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    WiredAccessoryManager.this.setDevicesState(message.arg1, message.arg2, (String) message.obj);
                    WiredAccessoryManager.this.mWakeLock.release();
                    break;
                case 2:
                    WiredAccessoryManager.this.onSystemReady();
                    WiredAccessoryManager.this.mWakeLock.release();
                    break;
            }
        }
    };

    public WiredAccessoryManager(Context context, InputManagerService inputManagerService) {
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "WiredAccessoryManager");
        this.mWakeLock.setReferenceCounted(false);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mInputManager = inputManagerService;
        this.mUseDevInputEventForAudioJack = context.getResources().getBoolean(R.^attr-private.pointerIconVectorFill);
        this.mObserver = new WiredAccessoryObserver();
    }

    private void onSystemReady() {
        if (this.mUseDevInputEventForAudioJack) {
            int i = 0;
            if (this.mInputManager.getSwitchState(-1, -256, 2) == 1) {
                i = 4;
            }
            if (this.mInputManager.getSwitchState(-1, -256, 4) == 1) {
                i |= 16;
            }
            if (this.mInputManager.getSwitchState(-1, -256, 6) == 1) {
                i |= 64;
            }
            notifyWiredAccessoryChanged(0L, i, 84);
        }
        this.mObserver.init();
    }

    @Override
    public void notifyWiredAccessoryChanged(long j, int i, int i2) {
        Slog.v(TAG, "notifyWiredAccessoryChanged: when=" + j + " bits=" + switchCodeToString(i, i2) + " mask=" + Integer.toHexString(i2));
        synchronized (this.mLock) {
            this.mSwitchValues = (this.mSwitchValues & (~i2)) | i;
            int i3 = this.mSwitchValues & 84;
            int i4 = 1;
            if (i3 != 0) {
                if (i3 == 4) {
                    i4 = 2;
                } else if (i3 != 16 && i3 != 20) {
                    if (i3 == 64) {
                        i4 = 32;
                    }
                }
                updateLocked(NAME_H2W, i4 | (this.mHeadsetState & (-36)));
            }
            i4 = 0;
            updateLocked(NAME_H2W, i4 | (this.mHeadsetState & (-36)));
        }
    }

    @Override
    public void systemReady() {
        synchronized (this.mLock) {
            this.mWakeLock.acquire();
            this.mHandler.sendMessage(this.mHandler.obtainMessage(2, 0, 0, null));
        }
    }

    private void updateLocked(String str, int i) {
        boolean z;
        int i2 = i & SUPPORTED_HEADSETS;
        int i3 = i2 & 4;
        int i4 = i2 & 8;
        int i5 = i2 & 35;
        Slog.v(TAG, "newName=" + str + " newState=" + i + " headsetState=" + i2 + " prev headsetState=" + this.mHeadsetState);
        if (this.mHeadsetState == i2) {
            Log.e(TAG, "No state change.");
            return;
        }
        boolean z2 = false;
        if (i5 == 35) {
            Log.e(TAG, "Invalid combination, unsetting h2w flag");
            z = false;
        } else {
            z = true;
        }
        if (i3 == 4 && i4 == 8) {
            Log.e(TAG, "Invalid combination, unsetting usb flag");
        } else {
            z2 = true;
        }
        if (!z && !z2) {
            Log.e(TAG, "invalid transition, returning ...");
            return;
        }
        this.mWakeLock.acquire();
        Log.i(TAG, "MSG_NEW_DEVICE_STATE");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, i2, this.mHeadsetState, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
        this.mHeadsetState = i2;
    }

    private void setDevicesState(int i, int i2, String str) {
        synchronized (this.mLock) {
            int i3 = SUPPORTED_HEADSETS;
            int i4 = 1;
            while (i3 != 0) {
                if ((i4 & i3) != 0) {
                    setDeviceStateLocked(i4, i, i2, str);
                    i3 &= ~i4;
                }
                i4 <<= 1;
            }
        }
    }

    private void setDeviceStateLocked(int i, int i2, int i3, String str) {
        int i4 = i2 & i;
        if (i4 != (i3 & i)) {
            int i5 = 0;
            int i6 = i4 != 0 ? 1 : 0;
            int i7 = 8;
            if (i == 1) {
                i5 = -2147483632;
                i7 = 4;
            } else if (i != 2) {
                if (i == 32) {
                    i7 = DumpState.DUMP_INTENT_FILTER_VERIFIERS;
                } else if (i == 4) {
                    i7 = 2048;
                } else if (i == 8) {
                    i7 = 4096;
                } else if (i == 16) {
                    i7 = 1024;
                } else {
                    Slog.e(TAG, "setDeviceState() invalid headset type: " + i);
                    return;
                }
            }
            String str2 = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("headsetName: ");
            sb.append(str);
            sb.append(i6 == 1 ? " connected" : " disconnected");
            Slog.v(str2, sb.toString());
            if (i7 != 0) {
                this.mAudioManager.setWiredDeviceConnectionState(i7, i6, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str);
            }
            if (i5 != 0) {
                this.mAudioManager.setWiredDeviceConnectionState(i5, i6, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, str);
            }
        }
    }

    private String switchCodeToString(int i, int i2) {
        StringBuffer stringBuffer = new StringBuffer();
        if ((i2 & 4) != 0 && (i & 4) != 0) {
            stringBuffer.append("SW_HEADPHONE_INSERT ");
        }
        if ((i2 & 16) != 0 && (i & 16) != 0) {
            stringBuffer.append("SW_MICROPHONE_INSERT");
        }
        return stringBuffer.toString();
    }

    class WiredAccessoryObserver extends UEventObserver {
        private final List<UEventInfo> mUEventInfo = makeObservedUEventList();

        public WiredAccessoryObserver() {
        }

        void init() {
            int i;
            synchronized (WiredAccessoryManager.this.mLock) {
                Slog.v(WiredAccessoryManager.TAG, "init()");
                char[] cArr = new char[1024];
                for (int i2 = 0; i2 < this.mUEventInfo.size(); i2++) {
                    UEventInfo uEventInfo = this.mUEventInfo.get(i2);
                    try {
                        try {
                            FileReader fileReader = new FileReader(uEventInfo.getSwitchStatePath());
                            int i3 = fileReader.read(cArr, 0, 1024);
                            fileReader.close();
                            int i4 = Integer.parseInt(new String(cArr, 0, i3).trim());
                            if (i4 > 0) {
                                updateStateLocked(uEventInfo.getDevPath(), uEventInfo.getDevName(), i4);
                            }
                        } catch (FileNotFoundException e) {
                            Slog.w(WiredAccessoryManager.TAG, uEventInfo.getSwitchStatePath() + " not found while attempting to determine initial switch state");
                        }
                    } catch (Exception e2) {
                        Slog.e(WiredAccessoryManager.TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, e2);
                    }
                }
            }
            for (i = 0; i < this.mUEventInfo.size(); i++) {
                startObserving("DEVPATH=" + this.mUEventInfo.get(i).getDevPath());
            }
        }

        private List<UEventInfo> makeObservedUEventList() {
            ArrayList arrayList = new ArrayList();
            if (!WiredAccessoryManager.this.mUseDevInputEventForAudioJack) {
                UEventInfo uEventInfo = new UEventInfo(WiredAccessoryManager.NAME_H2W, 1, 2, 32);
                if (!uEventInfo.checkSwitchExists()) {
                    Slog.w(WiredAccessoryManager.TAG, "This kernel does not have wired headset support");
                } else {
                    arrayList.add(uEventInfo);
                }
            }
            UEventInfo uEventInfo2 = new UEventInfo(WiredAccessoryManager.NAME_USB_AUDIO, 4, 8, 0);
            if (!uEventInfo2.checkSwitchExists()) {
                Slog.w(WiredAccessoryManager.TAG, "This kernel does not have usb audio support");
            } else {
                arrayList.add(uEventInfo2);
            }
            UEventInfo uEventInfo3 = new UEventInfo(WiredAccessoryManager.NAME_HDMI_AUDIO, 16, 0, 0);
            if (uEventInfo3.checkSwitchExists()) {
                arrayList.add(uEventInfo3);
            } else {
                UEventInfo uEventInfo4 = new UEventInfo(WiredAccessoryManager.NAME_HDMI, 16, 0, 0);
                if (!uEventInfo4.checkSwitchExists()) {
                    Slog.w(WiredAccessoryManager.TAG, "This kernel does not have HDMI audio support");
                } else {
                    arrayList.add(uEventInfo4);
                }
            }
            return arrayList;
        }

        public void onUEvent(UEventObserver.UEvent uEvent) {
            Slog.v(WiredAccessoryManager.TAG, "Headset UEVENT: " + uEvent.toString());
            try {
                String str = uEvent.get("DEVPATH");
                String str2 = uEvent.get("SWITCH_NAME");
                int i = Integer.parseInt(uEvent.get("SWITCH_STATE"));
                synchronized (WiredAccessoryManager.this.mLock) {
                    updateStateLocked(str, str2, i);
                }
            } catch (NumberFormatException e) {
                Slog.e(WiredAccessoryManager.TAG, "Could not parse switch state from event " + uEvent);
            }
        }

        private void updateStateLocked(String str, String str2, int i) {
            for (int i2 = 0; i2 < this.mUEventInfo.size(); i2++) {
                UEventInfo uEventInfo = this.mUEventInfo.get(i2);
                if (str.equals(uEventInfo.getDevPath())) {
                    WiredAccessoryManager.this.updateLocked(str2, uEventInfo.computeNewHeadsetState(WiredAccessoryManager.this.mHeadsetState, i));
                    return;
                }
            }
        }

        private final class UEventInfo {
            private final String mDevName;
            private final int mState1Bits;
            private final int mState2Bits;
            private final int mStateNbits;

            public UEventInfo(String str, int i, int i2, int i3) {
                this.mDevName = str;
                this.mState1Bits = i;
                this.mState2Bits = i2;
                this.mStateNbits = i3;
            }

            public String getDevName() {
                return this.mDevName;
            }

            public String getDevPath() {
                return String.format(Locale.US, "/devices/virtual/switch/%s", this.mDevName);
            }

            public String getSwitchStatePath() {
                return String.format(Locale.US, "/sys/class/switch/%s/state", this.mDevName);
            }

            public boolean checkSwitchExists() {
                return new File(getSwitchStatePath()).exists();
            }

            public int computeNewHeadsetState(int i, int i2) {
                int i3;
                int i4 = ~(this.mState1Bits | this.mState2Bits | this.mStateNbits);
                if (i2 == 1) {
                    i3 = this.mState1Bits;
                } else if (i2 == 2) {
                    i3 = this.mState2Bits;
                } else {
                    i3 = i2 == this.mStateNbits ? this.mStateNbits : 0;
                }
                return (i & i4) | i3;
            }
        }
    }
}
