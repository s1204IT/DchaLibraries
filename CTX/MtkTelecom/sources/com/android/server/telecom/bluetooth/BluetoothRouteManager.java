package com.android.server.telecom.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Message;
import android.telecom.Log;
import android.telecom.Logging.Session;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.telecom.BluetoothHeadsetProxy;
import com.android.server.telecom.CallState;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.Timeouts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class BluetoothRouteManager extends StateMachine {
    private static final String LOG_TAG = BluetoothRouteManager.class.getSimpleName();
    private static final SparseArray<String> MESSAGE_CODE_TO_NAME = new SparseArray<String>() {
        {
            put(1, "NEW_DEVICE_CONNECTED");
            put(2, "LOST_DEVICE");
            put(100, "CONNECT_HFP");
            put(101, "DISCONNECT_HFP");
            put(102, "RETRY_HFP_CONNECTION");
            put(200, "HFP_IS_ON");
            put(201, "HFP_LOST");
            put(300, "CONNECTION_TIMEOUT");
            put(400, "GET_CURRENT_STATE");
            put(9001, "RUN_RUNNABLE");
        }
    };
    private BluetoothDevice mActiveDeviceCache;
    private final Map<String, AudioConnectedState> mAudioConnectedStates;
    private final Map<String, AudioConnectingState> mAudioConnectingStates;
    private final State mAudioOffState;
    private final Context mContext;
    private BluetoothDeviceManager mDeviceManager;
    private BluetoothStateListener mListener;
    private final TelecomSystem.SyncRoot mLock;
    private final LinkedHashSet<String> mMostRecentlyUsedDevices;
    private final Timeouts.Adapter mTimeoutsAdapter;
    private final Set<State> statesToCleanUp;

    public interface BluetoothStateListener {
        void onBluetoothActiveDeviceGone();

        void onBluetoothActiveDevicePresent();

        void onBluetoothAudioConnected();

        void onBluetoothAudioDisconnected();

        void onBluetoothDeviceListChanged();
    }

    private final class AudioOffState extends State {
        private AudioOffState() {
        }

        public String getName() {
            return "AudioOff";
        }

        public void enter() {
            BluetoothDevice bluetoothAudioConnectedDevice = BluetoothRouteManager.this.getBluetoothAudioConnectedDevice();
            if (bluetoothAudioConnectedDevice != null) {
                Log.w(BluetoothRouteManager.LOG_TAG, "Entering AudioOff state but device %s appears to be connected. Disconnecting.", new Object[]{bluetoothAudioConnectedDevice});
                BluetoothRouteManager.this.disconnectAudio();
            }
            BluetoothRouteManager.this.cleanupStatesForDisconnectedDevices();
            if (BluetoothRouteManager.this.mListener != null) {
                BluetoothRouteManager.this.mListener.onBluetoothAudioDisconnected();
            }
        }

        public boolean processMessage(Message message) {
            if (message.what == 9001) {
                ((Runnable) message.obj).run();
                return true;
            }
            SomeArgs someArgs = (SomeArgs) message.obj;
            try {
                switch (message.what) {
                    case 1:
                        BluetoothRouteManager.this.addDevice((String) someArgs.arg2);
                        break;
                    case CallState.SELECT_PHONE_ACCOUNT:
                        BluetoothRouteManager.this.removeDevice((String) someArgs.arg2);
                        break;
                    case 100:
                        String strConnectHfpAudio = BluetoothRouteManager.this.connectHfpAudio((String) someArgs.arg2);
                        if (strConnectHfpAudio != null) {
                            BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectingStateForAddress(strConnectHfpAudio, "AudioOff/CONNECT_HFP"));
                        } else {
                            Log.w(BluetoothRouteManager.LOG_TAG, "Tried to connect to %s but failed to connect to any HFP device.", new Object[]{(String) someArgs.arg2});
                        }
                        break;
                    case 101:
                        break;
                    case 102:
                        Log.i(BluetoothRouteManager.LOG_TAG, "Retrying HFP connection to %s", new Object[]{(String) someArgs.arg2});
                        String strConnectHfpAudio2 = BluetoothRouteManager.this.connectHfpAudio((String) someArgs.arg2, someArgs.argi1);
                        if (strConnectHfpAudio2 != null) {
                            BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectingStateForAddress(strConnectHfpAudio2, "AudioOff/RETRY_HFP_CONNECTION"));
                        } else {
                            Log.i(BluetoothRouteManager.LOG_TAG, "Retry failed.", new Object[0]);
                        }
                        break;
                    case 200:
                        String str = (String) someArgs.arg2;
                        Log.w(BluetoothRouteManager.LOG_TAG, "HFP audio unexpectedly turned on from device %s", new Object[]{str});
                        BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectedStateForAddress(str, "AudioOff/HFP_IS_ON"));
                        break;
                    case 201:
                        Log.i(BluetoothRouteManager.LOG_TAG, "Received HFP off for device %s while HFP off.", new Object[]{(String) someArgs.arg2});
                        break;
                    case 300:
                        break;
                    case 400:
                        ((BlockingQueue) someArgs.arg3).offer(this);
                        break;
                }
                return true;
            } finally {
                someArgs.recycle();
            }
        }
    }

    private final class AudioConnectingState extends State {
        private final String mDeviceAddress;

        AudioConnectingState(String str) {
            this.mDeviceAddress = str;
        }

        public String getName() {
            return "Connecting:" + this.mDeviceAddress;
        }

        public void enter() {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = Log.createSubsession();
            BluetoothRouteManager.this.sendMessageDelayed(300, someArgsObtain, BluetoothRouteManager.this.mTimeoutsAdapter.getBluetoothPendingTimeoutMillis(BluetoothRouteManager.this.mContext.getContentResolver()));
            BluetoothRouteManager.this.mListener.onBluetoothAudioConnected();
        }

        public void exit() {
            BluetoothRouteManager.this.removeMessages(300);
        }

        public boolean processMessage(Message message) {
            if (message.what == 9001) {
                ((Runnable) message.obj).run();
                return true;
            }
            SomeArgs someArgs = (SomeArgs) message.obj;
            String str = (String) someArgs.arg2;
            try {
                switch (message.what) {
                    case 1:
                        BluetoothRouteManager.this.addDevice(str);
                        break;
                    case CallState.SELECT_PHONE_ACCOUNT:
                        BluetoothRouteManager.this.removeDevice((String) someArgs.arg2);
                        if (Objects.equals(str, this.mDeviceAddress)) {
                            BluetoothRouteManager.this.transitionToActualState();
                        }
                        break;
                    case 100:
                        if (!Objects.equals(this.mDeviceAddress, str)) {
                            String strConnectHfpAudio = BluetoothRouteManager.this.connectHfpAudio(str);
                            if (strConnectHfpAudio != null) {
                                BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectingStateForAddress(strConnectHfpAudio, "AudioConnecting/CONNECT_HFP"));
                            } else {
                                Log.w(BluetoothRouteManager.LOG_TAG, "Tried to connect to %s but failed to connect to any HFP device.", new Object[]{(String) someArgs.arg2});
                            }
                        }
                        break;
                    case 101:
                        BluetoothRouteManager.this.disconnectAudio();
                        BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.mAudioOffState);
                        break;
                    case 102:
                        if (Objects.equals(str, this.mDeviceAddress)) {
                            Log.d(BluetoothRouteManager.LOG_TAG, "Retry message came through while connecting.", new Object[0]);
                        } else {
                            String strConnectHfpAudio2 = BluetoothRouteManager.this.connectHfpAudio(str, someArgs.argi1);
                            if (strConnectHfpAudio2 != null) {
                                BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectingStateForAddress(strConnectHfpAudio2, "AudioConnecting/RETRY_HFP_CONNECTION"));
                            } else {
                                Log.i(BluetoothRouteManager.LOG_TAG, "Retry failed.", new Object[0]);
                            }
                        }
                        break;
                    case 200:
                        if (Objects.equals(this.mDeviceAddress, str)) {
                            Log.i(BluetoothRouteManager.LOG_TAG, "HFP connection success for device %s.", new Object[]{this.mDeviceAddress});
                            BluetoothRouteManager.this.transitionTo((IState) BluetoothRouteManager.this.mAudioConnectedStates.get(this.mDeviceAddress));
                        } else {
                            Log.w(BluetoothRouteManager.LOG_TAG, "In connecting state for device %s but %s is now connected", new Object[]{this.mDeviceAddress, str});
                            BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectedStateForAddress(str, "AudioConnecting/HFP_IS_ON"));
                        }
                        break;
                    case 201:
                        if (Objects.equals(this.mDeviceAddress, str)) {
                            Log.i(BluetoothRouteManager.LOG_TAG, "Connection with device %s failed.", new Object[]{this.mDeviceAddress});
                            BluetoothRouteManager.this.transitionToActualState();
                        } else {
                            Log.w(BluetoothRouteManager.LOG_TAG, "Got HFP lost message for device %s while connecting to %s.", new Object[]{str, this.mDeviceAddress});
                        }
                        break;
                    case 300:
                        Log.i(BluetoothRouteManager.LOG_TAG, "Connection with device %s timed out.", new Object[]{this.mDeviceAddress});
                        BluetoothRouteManager.this.transitionToActualState();
                        break;
                    case 400:
                        ((BlockingQueue) someArgs.arg3).offer(this);
                        break;
                }
                return true;
            } finally {
                someArgs.recycle();
            }
        }
    }

    private final class AudioConnectedState extends State {
        private final String mDeviceAddress;

        AudioConnectedState(String str) {
            this.mDeviceAddress = str;
        }

        public String getName() {
            return "Connected:" + this.mDeviceAddress;
        }

        public void enter() {
            BluetoothRouteManager.this.removeMessages(102);
            BluetoothRouteManager.this.mMostRecentlyUsedDevices.remove(this.mDeviceAddress);
            BluetoothRouteManager.this.mMostRecentlyUsedDevices.add(this.mDeviceAddress);
            BluetoothRouteManager.this.mListener.onBluetoothAudioConnected();
        }

        public boolean processMessage(Message message) {
            if (message.what == 9001) {
                ((Runnable) message.obj).run();
                return true;
            }
            SomeArgs someArgs = (SomeArgs) message.obj;
            String str = (String) someArgs.arg2;
            try {
                switch (message.what) {
                    case 1:
                        BluetoothRouteManager.this.addDevice(str);
                        break;
                    case CallState.SELECT_PHONE_ACCOUNT:
                        BluetoothRouteManager.this.removeDevice((String) someArgs.arg2);
                        if (Objects.equals(str, this.mDeviceAddress)) {
                            BluetoothRouteManager.this.transitionToActualState();
                        }
                        break;
                    case 100:
                        if (!Objects.equals(this.mDeviceAddress, str)) {
                            if (BluetoothRouteManager.this.connectHfpAudio(str) != null) {
                                BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectingStateForAddress(str, "AudioConnected/CONNECT_HFP"));
                            } else {
                                Log.w(BluetoothRouteManager.LOG_TAG, "Tried to connect to %s but failed to connect to any HFP device.", new Object[]{(String) someArgs.arg2});
                            }
                        }
                        break;
                    case 101:
                        BluetoothRouteManager.this.disconnectAudio();
                        BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.mAudioOffState);
                        break;
                    case 102:
                        if (Objects.equals(str, this.mDeviceAddress)) {
                            Log.d(BluetoothRouteManager.LOG_TAG, "Retry message came through while connected.", new Object[0]);
                        } else {
                            String strConnectHfpAudio = BluetoothRouteManager.this.connectHfpAudio(str, someArgs.argi1);
                            if (strConnectHfpAudio != null) {
                                BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectingStateForAddress(strConnectHfpAudio, "AudioConnected/RETRY_HFP_CONNECTION"));
                            } else {
                                Log.i(BluetoothRouteManager.LOG_TAG, "Retry failed.", new Object[0]);
                            }
                        }
                        break;
                    case 200:
                        if (Objects.equals(this.mDeviceAddress, str)) {
                            Log.i(BluetoothRouteManager.LOG_TAG, "Received redundant HFP_IS_ON for %s", new Object[]{this.mDeviceAddress});
                        } else {
                            Log.w(BluetoothRouteManager.LOG_TAG, "In connected state for device %s but %s is now connected", new Object[]{this.mDeviceAddress, str});
                            BluetoothRouteManager.this.transitionTo(BluetoothRouteManager.this.getConnectedStateForAddress(str, "AudioConnected/HFP_IS_ON"));
                        }
                        break;
                    case 201:
                        if (Objects.equals(this.mDeviceAddress, str)) {
                            Log.i(BluetoothRouteManager.LOG_TAG, "HFP connection with device %s lost.", new Object[]{this.mDeviceAddress});
                            BluetoothRouteManager.this.transitionToActualState();
                        } else {
                            Log.w(BluetoothRouteManager.LOG_TAG, "Got HFP lost message for device %s while connected to %s.", new Object[]{str, this.mDeviceAddress});
                        }
                        break;
                    case 300:
                        Log.w(BluetoothRouteManager.LOG_TAG, "Received CONNECTION_TIMEOUT while connected.", new Object[0]);
                        break;
                    case 400:
                        ((BlockingQueue) someArgs.arg3).offer(this);
                        break;
                }
                return true;
            } finally {
                someArgs.recycle();
            }
        }
    }

    public BluetoothRouteManager(Context context, TelecomSystem.SyncRoot syncRoot, BluetoothDeviceManager bluetoothDeviceManager, Timeouts.Adapter adapter) {
        super(BluetoothRouteManager.class.getSimpleName());
        this.mAudioConnectingStates = new HashMap();
        this.mAudioConnectedStates = new HashMap();
        this.statesToCleanUp = new HashSet();
        this.mMostRecentlyUsedDevices = new LinkedHashSet<>();
        this.mActiveDeviceCache = null;
        this.mContext = context;
        this.mLock = syncRoot;
        this.mDeviceManager = bluetoothDeviceManager;
        this.mDeviceManager.setBluetoothRouteManager(this);
        this.mTimeoutsAdapter = adapter;
        this.mAudioOffState = new AudioOffState();
        addState(this.mAudioOffState);
        setInitialState(this.mAudioOffState);
        start();
    }

    protected void onPreHandleMessage(Message message) {
        if (message.obj != null && (message.obj instanceof SomeArgs)) {
            Log.continueSession((Session) ((SomeArgs) message.obj).arg1, "BRM.pM_" + message.what);
            Log.i(LOG_TAG, "Message received: %s.", new Object[]{MESSAGE_CODE_TO_NAME.get(message.what)});
            return;
        }
        if (message.what == 9001 && (message.obj instanceof Runnable)) {
            Log.i(LOG_TAG, "Running runnable for testing", new Object[0]);
            return;
        }
        String str = LOG_TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Message sent must be of type nonnull SomeArgs, but got ");
        sb.append(message.obj == null ? "null" : message.obj.getClass().getSimpleName());
        Log.w(str, sb.toString(), new Object[0]);
        Log.w(LOG_TAG, "The message was of code %d = %s", new Object[]{Integer.valueOf(message.what), MESSAGE_CODE_TO_NAME.get(message.what)});
    }

    protected void onPostHandleMessage(Message message) {
        Log.endSession();
    }

    public boolean isBluetoothAvailable() {
        return this.mDeviceManager.getNumConnectedDevices() > 0;
    }

    public boolean isBluetoothAudioConnectedOrPending() {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = Log.createSubsession();
        LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue();
        someArgsObtain.arg3 = linkedBlockingQueue;
        sendMessage(400, someArgsObtain);
        try {
            State state = (IState) linkedBlockingQueue.poll(1000L, TimeUnit.MILLISECONDS);
            if (state != null) {
                return state != this.mAudioOffState;
            }
            Log.w(LOG_TAG, "Failed to get a state from the state machine in time -- Handler stuck?", new Object[0]);
            return false;
        } catch (InterruptedException e) {
            Log.w(LOG_TAG, "isBluetoothAudioConnectedOrPending -- interrupted getting state", new Object[0]);
            return false;
        }
    }

    public void connectBluetoothAudio(String str) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = Log.createSubsession();
        someArgsObtain.arg2 = str;
        sendMessage(100, someArgsObtain);
    }

    public void disconnectBluetoothAudio() {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = Log.createSubsession();
        sendMessage(101, someArgsObtain);
    }

    public void setListener(BluetoothStateListener bluetoothStateListener) {
        this.mListener = bluetoothStateListener;
    }

    public void onDeviceAdded(String str) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = Log.createSubsession();
        someArgsObtain.arg2 = str;
        sendMessage(1, someArgsObtain);
        this.mListener.onBluetoothDeviceListChanged();
    }

    public void onDeviceLost(String str) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = Log.createSubsession();
        someArgsObtain.arg2 = str;
        sendMessage(2, someArgsObtain);
        this.mListener.onBluetoothDeviceListChanged();
    }

    public void onActiveDeviceChanged(BluetoothDevice bluetoothDevice) {
        BluetoothDevice bluetoothDevice2 = this.mActiveDeviceCache;
        this.mActiveDeviceCache = bluetoothDevice;
        if ((bluetoothDevice2 == null) ^ (bluetoothDevice == null)) {
            if (bluetoothDevice == null) {
                this.mListener.onBluetoothActiveDeviceGone();
            } else {
                this.mListener.onBluetoothActiveDevicePresent();
            }
        }
    }

    public Collection<BluetoothDevice> getConnectedDevices() {
        Collection<BluetoothDevice> collectionUnmodifiableCollection;
        synchronized (this.mLock) {
            collectionUnmodifiableCollection = Collections.unmodifiableCollection(new ArrayList(this.mDeviceManager.getConnectedDevices()));
        }
        return collectionUnmodifiableCollection;
    }

    private String connectHfpAudio(String str) {
        return connectHfpAudio(str, 0);
    }

    private String connectHfpAudio(final String str, int i) {
        String activeDeviceAddress;
        Optional<BluetoothDevice> optionalFindAny = getConnectedDevices().stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Objects.equals(((BluetoothDevice) obj).getAddress(), str);
            }
        }).findAny();
        if (!optionalFindAny.isPresent()) {
            activeDeviceAddress = getActiveDeviceAddress();
        } else {
            activeDeviceAddress = str;
        }
        if (!optionalFindAny.isPresent()) {
            Log.i(this, "No device with address %s available. Using %s instead.", new Object[]{str, activeDeviceAddress});
        }
        if (activeDeviceAddress == null) {
            Log.i(this, "No device specified and BT stack has no active device. Not connecting.", new Object[0]);
            return null;
        }
        if (!connectAudio(activeDeviceAddress)) {
            boolean z = i < 2;
            String str2 = LOG_TAG;
            Object[] objArr = new Object[2];
            objArr[0] = activeDeviceAddress;
            objArr[1] = z ? "retry" : "not retry";
            Log.w(str2, "Could not connect to %s. Will %s", objArr);
            if (z) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = Log.createSubsession();
                someArgsObtain.arg2 = activeDeviceAddress;
                someArgsObtain.argi1 = i + 1;
                sendMessageDelayed(102, someArgsObtain, this.mTimeoutsAdapter.getRetryBluetoothConnectAudioBackoffMillis(this.mContext.getContentResolver()));
            }
            return null;
        }
        return activeDeviceAddress;
    }

    public String getActiveDeviceAddress() {
        if (this.mActiveDeviceCache == null) {
            return null;
        }
        return this.mActiveDeviceCache.getAddress();
    }

    private void transitionToActualState() {
        BluetoothDevice bluetoothAudioConnectedDevice = getBluetoothAudioConnectedDevice();
        if (bluetoothAudioConnectedDevice != null) {
            Log.i(LOG_TAG, "Device %s is already connected; going to AudioConnected.", new Object[]{bluetoothAudioConnectedDevice});
            transitionTo(getConnectedStateForAddress(bluetoothAudioConnectedDevice.getAddress(), "transitionToActualState"));
        } else {
            transitionTo(this.mAudioOffState);
        }
    }

    @VisibleForTesting
    public BluetoothDevice getBluetoothAudioConnectedDevice() {
        BluetoothHeadsetProxy headsetService = this.mDeviceManager.getHeadsetService();
        if (headsetService == null) {
            Log.i(this, "getBluetoothAudioConnectedDevice: no headset service available.", new Object[0]);
            return null;
        }
        List<BluetoothDevice> connectedDevices = headsetService.getConnectedDevices();
        for (int i = 0; i < connectedDevices.size(); i++) {
            BluetoothDevice bluetoothDevice = connectedDevices.get(i);
            boolean z = headsetService.getAudioState(bluetoothDevice) != 10;
            Log.v(this, "isBluetoothAudioConnected: ==> isAudioOn = " + z + "for headset: " + bluetoothDevice, new Object[0]);
            if (z) {
                return bluetoothDevice;
            }
        }
        return null;
    }

    @VisibleForTesting
    public boolean isInbandRingingEnabled() {
        BluetoothHeadsetProxy headsetService = this.mDeviceManager.getHeadsetService();
        if (headsetService == null) {
            Log.i(this, "isInbandRingingEnabled: no headset service available.", new Object[0]);
            return false;
        }
        return headsetService.isInbandRingingEnabled();
    }

    private boolean connectAudio(String str) {
        BluetoothHeadsetProxy headsetService = this.mDeviceManager.getHeadsetService();
        if (headsetService == null) {
            Log.w(this, "Trying to connect audio but no headset service exists.", new Object[0]);
            return false;
        }
        BluetoothDevice deviceFromAddress = this.mDeviceManager.getDeviceFromAddress(str);
        if (deviceFromAddress == null) {
            Log.w(this, "Attempting to turn on audio for a disconnected device", new Object[0]);
            return false;
        }
        if (!headsetService.setActiveDevice(deviceFromAddress)) {
            Log.w(LOG_TAG, "Couldn't set active device to %s", new Object[]{str});
            return false;
        }
        if (headsetService.isAudioOn()) {
            return true;
        }
        return headsetService.connectAudio();
    }

    private void disconnectAudio() {
        BluetoothHeadsetProxy headsetService = this.mDeviceManager.getHeadsetService();
        if (headsetService == null) {
            Log.w(this, "Trying to disconnect audio but no headset service exists.", new Object[0]);
        } else {
            headsetService.disconnectAudio();
        }
    }

    private boolean addDevice(String str) {
        if (this.mAudioConnectingStates.containsKey(str)) {
            Log.i(this, "Attempting to add device %s twice.", new Object[]{str});
            return false;
        }
        AudioConnectedState audioConnectedState = new AudioConnectedState(str);
        AudioConnectingState audioConnectingState = new AudioConnectingState(str);
        this.mAudioConnectingStates.put(str, audioConnectingState);
        this.mAudioConnectedStates.put(str, audioConnectedState);
        addState(audioConnectedState);
        addState(audioConnectingState);
        return true;
    }

    private boolean removeDevice(String str) {
        if (!this.mAudioConnectingStates.containsKey(str)) {
            Log.i(this, "Attempting to remove already-removed device %s", new Object[]{str});
            return false;
        }
        this.statesToCleanUp.add(this.mAudioConnectingStates.remove(str));
        this.statesToCleanUp.add(this.mAudioConnectedStates.remove(str));
        this.mMostRecentlyUsedDevices.remove(str);
        return true;
    }

    private AudioConnectingState getConnectingStateForAddress(String str, String str2) {
        if (!this.mAudioConnectingStates.containsKey(str)) {
            Log.w(LOG_TAG, "Device being connected to does not have a corresponding state: %s", new Object[]{str2});
            addDevice(str);
        }
        return this.mAudioConnectingStates.get(str);
    }

    private AudioConnectedState getConnectedStateForAddress(String str, String str2) {
        if (!this.mAudioConnectedStates.containsKey(str)) {
            Log.w(LOG_TAG, "Device already connected to does not have a corresponding state: %s", new Object[]{str2});
            addDevice(str);
        }
        return this.mAudioConnectedStates.get(str);
    }

    private void cleanupStatesForDisconnectedDevices() {
        for (State state : this.statesToCleanUp) {
            if (state != null) {
                removeState(state);
            }
        }
        this.statesToCleanUp.clear();
    }

    @VisibleForTesting
    public void setInitialStateForTesting(String str, BluetoothDevice bluetoothDevice) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 1040541145) {
            if (iHashCode != 1217813208) {
                b = (iHashCode == 1424757481 && str.equals("Connected")) ? (byte) 2 : (byte) -1;
            } else if (str.equals("Connecting")) {
                b = 1;
            }
        } else if (str.equals("AudioOff")) {
            b = 0;
        }
        switch (b) {
            case CallState.NEW:
                transitionTo(this.mAudioOffState);
                break;
            case 1:
                transitionTo(getConnectingStateForAddress(bluetoothDevice.getAddress(), "setInitialStateForTesting"));
                break;
            case CallState.SELECT_PHONE_ACCOUNT:
                transitionTo(getConnectedStateForAddress(bluetoothDevice.getAddress(), "setInitialStateForTesting"));
                break;
        }
    }

    @VisibleForTesting
    public void setActiveDeviceCacheForTesting(BluetoothDevice bluetoothDevice) {
        this.mActiveDeviceCache = bluetoothDevice;
    }
}
