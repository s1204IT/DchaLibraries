package com.android.server;

import android.R;
import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.UEventObserver;
import android.util.Log;
import android.util.Slog;
import android.widget.Toast;
import com.android.server.input.InputManagerService;
import com.android.server.pm.PackageManagerService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

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
    private final Context mContext;
    private int mHeadsetState;
    private final InputManagerService mInputManager;
    private final WiredAccessoryObserver mObserver;
    private int mSwitchValues;
    private final boolean mUseDevInputEventForAudioJack;
    private final PowerManager.WakeLock mWakeLock;
    private Toast toast;
    private final Object mLock = new Object();
    private String num_hs_pole = "NA";
    private int illegal_state = 0;
    private final Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    WiredAccessoryManager.this.setDevicesState(msg.arg1, msg.arg2, (String) msg.obj);
                    WiredAccessoryManager.this.mWakeLock.release();
                    break;
                case 2:
                    WiredAccessoryManager.this.onSystemReady();
                    WiredAccessoryManager.this.mWakeLock.release();
                    break;
            }
        }
    };

    public WiredAccessoryManager(Context context, InputManagerService inputManager) {
        this.mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, "WiredAccessoryManager");
        this.mWakeLock.setReferenceCounted(false);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mInputManager = inputManager;
        this.mUseDevInputEventForAudioJack = context.getResources().getBoolean(R.^attr-private.keyboardViewStyle);
        this.mObserver = new WiredAccessoryObserver();
        IntentFilter filter = new IntentFilter("android.intent.action.BOOT_COMPLETED");
        filter.addAction("android.intent.action.LAUNCH_POWEROFF_ALARM");
    }

    private void onSystemReady() {
        if (this.mUseDevInputEventForAudioJack) {
            int switchValues = 0;
            if (this.mInputManager.getSwitchState(-1, -256, 2) == 1) {
                switchValues = 4;
            }
            if (this.mInputManager.getSwitchState(-1, -256, 4) == 1) {
                switchValues |= 16;
            }
            if (this.mInputManager.getSwitchState(-1, -256, 6) == 1) {
                switchValues |= 64;
            }
            notifyWiredAccessoryChanged(0L, switchValues, 84);
        }
        this.mObserver.init();
    }

    @Override
    public void notifyWiredAccessoryChanged(long whenNanos, int switchValues, int switchMask) {
        int headset;
        Slog.v(TAG, "notifyWiredAccessoryChanged: when=" + whenNanos + " bits=" + switchCodeToString(switchValues, switchMask) + " mask=" + Integer.toHexString(switchMask));
        synchronized (this.mLock) {
            this.mSwitchValues = (this.mSwitchValues & (~switchMask)) | switchValues;
            switch (this.mSwitchValues & 84) {
                case 0:
                    headset = 0;
                    break;
                case 4:
                    headset = 2;
                    break;
                case 16:
                    headset = 1;
                    break;
                case 20:
                    headset = 1;
                    break;
                case 64:
                    headset = 32;
                    break;
                default:
                    headset = 0;
                    break;
            }
            updateLocked(NAME_H2W, (this.mHeadsetState & (-36)) | headset);
        }
    }

    private void showheadsetToast() {
        Slog.d(TAG, "come in showheadsetToast++++++++");
        if (this.mContext != null) {
            this.toast = Toast.makeText(this.mContext, com.mediatek.internal.R.string.headset_pin_recognition, 1);
            this.toast.show();
        }
        Timer headset_timer = new Timer();
        headset_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                WiredAccessoryManager.this.toast.show();
            }
        }, 500L);
    }

    private int getIllegalHeadset() {
        String pinStateFilePath = String.format("/sys/devices/platform/Accdet_Driver/driver/accdet_pin_recognition", new Object[0]);
        try {
            FileReader fw = new FileReader(pinStateFilePath);
            int state = fw.read();
            int pin_state = Integer.valueOf(state).intValue();
            fw.close();
            Log.d(TAG, "PIN state for Accdet is " + pin_state);
            return pin_state;
        } catch (Exception e) {
            Log.e(TAG, "", e);
            return 0;
        }
    }

    @Override
    public void systemReady() {
        synchronized (this.mLock) {
            this.mWakeLock.acquire();
            Message msg = this.mHandler.obtainMessage(2, 0, 0, null);
            this.mHandler.sendMessage(msg);
        }
    }

    private void updateLocked(String newName, int newState) {
        int headsetState = newState & SUPPORTED_HEADSETS;
        int usb_headset_anlg = headsetState & 4;
        int usb_headset_dgtl = headsetState & 8;
        int h2w_headset = headsetState & 35;
        boolean h2wStateChange = true;
        boolean usbStateChange = true;
        Slog.v(TAG, "newName=" + newName + " newState=" + newState + " headsetState=" + headsetState + " prev headsetState=" + this.mHeadsetState);
        if (this.mHeadsetState == headsetState) {
            Log.e(TAG, "No state change.");
            return;
        }
        if (h2w_headset == 35) {
            Log.e(TAG, "Invalid combination, unsetting h2w flag");
            h2wStateChange = false;
        }
        if (usb_headset_anlg == 4 && usb_headset_dgtl == 8) {
            Log.e(TAG, "Invalid combination, unsetting usb flag");
            usbStateChange = false;
        }
        if (!h2wStateChange && !usbStateChange) {
            Log.e(TAG, "invalid transition, returning ...");
            return;
        }
        this.mWakeLock.acquire();
        Log.i(TAG, "MSG_NEW_DEVICE_STATE");
        Message msg = this.mHandler.obtainMessage(1, headsetState, this.mHeadsetState, "");
        this.mHandler.sendMessage(msg);
        this.mHeadsetState = headsetState;
    }

    private void setDevicesState(int headsetState, int prevHeadsetState, String headsetName) {
        synchronized (this.mLock) {
            int allHeadsets = SUPPORTED_HEADSETS;
            int curHeadset = 1;
            while (allHeadsets != 0) {
                if ((curHeadset & allHeadsets) != 0) {
                    setDeviceStateLocked(curHeadset, headsetState, prevHeadsetState, headsetName);
                    allHeadsets &= ~curHeadset;
                }
                curHeadset <<= 1;
            }
        }
    }

    private void setDeviceStateLocked(int headset, int headsetState, int prevHeadsetState, String headsetName) {
        int state;
        int outDevice;
        if ((headsetState & headset) == (prevHeadsetState & headset)) {
            return;
        }
        int inDevice = 0;
        if ((headsetState & headset) != 0) {
            state = 1;
        } else {
            state = 0;
        }
        if (headset == 1) {
            outDevice = 4;
            inDevice = -2147483632;
        } else if (headset == 2) {
            outDevice = 8;
        } else if (headset == 32) {
            outDevice = PackageManagerService.DumpState.DUMP_INTENT_FILTER_VERIFIERS;
        } else if (headset == 4) {
            outDevice = PackageManagerService.DumpState.DUMP_VERIFIERS;
        } else if (headset == 8) {
            outDevice = 4096;
        } else if (headset == 16) {
            outDevice = 1024;
        } else {
            Slog.e(TAG, "setDeviceState() invalid headset type: " + headset);
            return;
        }
        Slog.v(TAG, "device " + headsetName + (state == 1 ? " connected" : " disconnected"));
        if (prevHeadsetState == 2 && headsetState == 1 && state == 0) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
            }
        }
        Log.d(TAG, "WiredHeadset num_hs_pole is " + this.num_hs_pole);
        if (state == 1 && this.num_hs_pole != "NA") {
            this.mAudioManager.setParameters(this.num_hs_pole);
        }
        if (outDevice != 0) {
            this.mAudioManager.setWiredDeviceConnectionState(outDevice, state, "", headsetName);
        }
        if (inDevice != 0) {
            this.mAudioManager.setWiredDeviceConnectionState(inDevice, state, "", headsetName);
        }
        this.illegal_state = getIllegalHeadset();
        if (49 != this.illegal_state) {
            return;
        }
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                WiredAccessoryManager.this.illegal_state = WiredAccessoryManager.this.getIllegalHeadset();
                if (49 == WiredAccessoryManager.this.illegal_state) {
                    Slog.d(WiredAccessoryManager.TAG, "show illegal Headset msg+++++++++++++");
                    WiredAccessoryManager.this.showheadsetToast();
                    WiredAccessoryManager.this.illegal_state = 0;
                } else {
                    Slog.d(WiredAccessoryManager.TAG, "don't show illegal Headset msg+++++++++++++");
                    WiredAccessoryManager.this.illegal_state = 0;
                }
            }
        }, 500L);
    }

    private String switchCodeToString(int switchValues, int switchMask) {
        StringBuffer sb = new StringBuffer();
        if ((switchMask & 4) != 0 && (switchValues & 4) != 0) {
            sb.append("SW_HEADPHONE_INSERT ");
        }
        if ((switchMask & 16) != 0 && (switchValues & 16) != 0) {
            sb.append("SW_MICROPHONE_INSERT");
        }
        return sb.toString();
    }

    class WiredAccessoryObserver extends UEventObserver {
        private final List<UEventInfo> mUEventInfo = makeObservedUEventList();

        public WiredAccessoryObserver() {
        }

        void init() {
            synchronized (WiredAccessoryManager.this.mLock) {
                Slog.v(WiredAccessoryManager.TAG, "init()");
                char[] buffer = new char[1024];
                for (int i = 0; i < this.mUEventInfo.size(); i++) {
                    UEventInfo uei = this.mUEventInfo.get(i);
                    try {
                        try {
                            FileReader file = new FileReader(uei.getSwitchStatePath());
                            int len = file.read(buffer, 0, 1024);
                            file.close();
                            int curState = Integer.parseInt(new String(buffer, 0, len).trim());
                            if (curState > 0) {
                                updateStateLocked(uei.getDevPath(), uei.getDevName(), curState);
                            }
                        } catch (Exception e) {
                            Slog.e(WiredAccessoryManager.TAG, "", e);
                        }
                    } catch (FileNotFoundException e2) {
                        Slog.w(WiredAccessoryManager.TAG, uei.getSwitchStatePath() + " not found while attempting to determine initial switch state");
                    }
                }
            }
            for (int i2 = 0; i2 < this.mUEventInfo.size(); i2++) {
                startObserving("DEVPATH=" + this.mUEventInfo.get(i2).getDevPath());
            }
        }

        private List<UEventInfo> makeObservedUEventList() {
            List<UEventInfo> retVal = new ArrayList<>();
            if (!WiredAccessoryManager.this.mUseDevInputEventForAudioJack) {
                UEventInfo uei = new UEventInfo(WiredAccessoryManager.NAME_H2W, 1, 2, 32);
                if (uei.checkSwitchExists()) {
                    retVal.add(uei);
                } else {
                    Slog.w(WiredAccessoryManager.TAG, "This kernel does not have wired headset support");
                }
            }
            UEventInfo uei2 = new UEventInfo(WiredAccessoryManager.NAME_USB_AUDIO, 4, 8, 0);
            if (uei2.checkSwitchExists()) {
                retVal.add(uei2);
            } else {
                Slog.w(WiredAccessoryManager.TAG, "This kernel does not have usb audio support");
            }
            UEventInfo uei3 = new UEventInfo(WiredAccessoryManager.NAME_HDMI_AUDIO, 16, 0, 0);
            if (uei3.checkSwitchExists()) {
                retVal.add(uei3);
            } else {
                UEventInfo uei4 = new UEventInfo(WiredAccessoryManager.NAME_HDMI, 16, 0, 0);
                if (uei4.checkSwitchExists()) {
                    retVal.add(uei4);
                } else {
                    Slog.w(WiredAccessoryManager.TAG, "This kernel does not have HDMI audio support");
                }
            }
            return retVal;
        }

        public void onUEvent(UEventObserver.UEvent event) {
            Slog.v(WiredAccessoryManager.TAG, "Headset UEVENT: " + event.toString());
            try {
                String devPath = event.get("DEVPATH");
                String name = event.get("SWITCH_NAME");
                int state = Integer.parseInt(event.get("SWITCH_STATE"));
                synchronized (WiredAccessoryManager.this.mLock) {
                    updateStateLocked(devPath, name, state);
                }
            } catch (NumberFormatException e) {
                Slog.e(WiredAccessoryManager.TAG, "Could not parse switch state from event " + event);
            }
        }

        private void updateStateLocked(String devPath, String name, int state) {
            for (int i = 0; i < this.mUEventInfo.size(); i++) {
                UEventInfo uei = this.mUEventInfo.get(i);
                if (devPath.equals(uei.getDevPath())) {
                    WiredAccessoryManager.this.updateLocked(name, uei.computeNewHeadsetState(WiredAccessoryManager.this.mHeadsetState, state));
                    return;
                }
            }
        }

        private final class UEventInfo {
            private final String mDevName;
            private final int mState1Bits;
            private final int mState2Bits;
            private final int mStateNbits;

            public UEventInfo(String devName, int state1Bits, int state2Bits, int stateNbits) {
                this.mDevName = devName;
                this.mState1Bits = state1Bits;
                this.mState2Bits = state2Bits;
                this.mStateNbits = stateNbits;
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
                File f = new File(getSwitchStatePath());
                return f.exists();
            }

            public int computeNewHeadsetState(int headsetState, int switchState) {
                int setBits;
                int preserveMask = ~(this.mState1Bits | this.mState2Bits | this.mStateNbits);
                if (switchState == 1) {
                    setBits = this.mState1Bits;
                } else if (switchState == 2) {
                    setBits = this.mState2Bits;
                } else {
                    setBits = switchState == this.mStateNbits ? this.mStateNbits : 0;
                }
                if (switchState == 3) {
                    WiredAccessoryManager.this.num_hs_pole = "num_hs_pole=5";
                    setBits = this.mState1Bits;
                } else if (switchState == 1) {
                    WiredAccessoryManager.this.num_hs_pole = "num_hs_pole=4";
                } else {
                    WiredAccessoryManager.this.num_hs_pole = "NA";
                }
                return (headsetState & preserveMask) | setBits;
            }
        }
    }
}
