package android.hardware.input;

import android.app.IInputForwarder;
import android.content.Context;
import android.hardware.input.IInputDevicesChangedListener;
import android.hardware.input.IInputManager;
import android.hardware.input.ITabletModeChangedListener;
import android.media.AudioAttributes;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.PointerIcon;
import com.android.internal.os.SomeArgs;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public final class InputManager {
    public static final String ACTION_QUERY_KEYBOARD_LAYOUTS = "android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS";
    private static final boolean DEBUG = false;
    public static final int DEFAULT_POINTER_SPEED = 0;
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;
    public static final int MAX_POINTER_SPEED = 7;
    public static final String META_DATA_KEYBOARD_LAYOUTS = "android.hardware.input.metadata.KEYBOARD_LAYOUTS";
    public static final int MIN_POINTER_SPEED = -7;
    private static final int MSG_DEVICE_ADDED = 1;
    private static final int MSG_DEVICE_CHANGED = 3;
    private static final int MSG_DEVICE_REMOVED = 2;
    public static final int SWITCH_STATE_OFF = 0;
    public static final int SWITCH_STATE_ON = 1;
    public static final int SWITCH_STATE_UNKNOWN = -1;
    private static final String TAG = "InputManager";
    private static InputManager sInstance;
    private final IInputManager mIm;
    private SparseArray<InputDevice> mInputDevices;
    private InputDevicesChangedListener mInputDevicesChangedListener;
    private List<OnTabletModeChangedListenerDelegate> mOnTabletModeChangedListeners;
    private TabletModeChangedListener mTabletModeChangedListener;
    private final Object mInputDevicesLock = new Object();
    private final ArrayList<InputDeviceListenerDelegate> mInputDeviceListeners = new ArrayList<>();
    private final Object mTabletModeLock = new Object();

    public interface InputDeviceListener {
        void onInputDeviceAdded(int i);

        void onInputDeviceChanged(int i);

        void onInputDeviceRemoved(int i);
    }

    public interface OnTabletModeChangedListener {
        void onTabletModeChanged(long j, boolean z);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SwitchState {
    }

    private InputManager(IInputManager iInputManager) {
        this.mIm = iInputManager;
    }

    public static InputManager getInstance() {
        InputManager inputManager;
        synchronized (InputManager.class) {
            if (sInstance == null) {
                try {
                    sInstance = new InputManager(IInputManager.Stub.asInterface(ServiceManager.getServiceOrThrow("input")));
                } catch (ServiceManager.ServiceNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
            inputManager = sInstance;
        }
        return inputManager;
    }

    public InputDevice getInputDevice(int i) {
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            int iIndexOfKey = this.mInputDevices.indexOfKey(i);
            if (iIndexOfKey < 0) {
                return null;
            }
            InputDevice inputDeviceValueAt = this.mInputDevices.valueAt(iIndexOfKey);
            if (inputDeviceValueAt == null) {
                try {
                    inputDeviceValueAt = this.mIm.getInputDevice(i);
                    if (inputDeviceValueAt != null) {
                        this.mInputDevices.setValueAt(iIndexOfKey, inputDeviceValueAt);
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            return inputDeviceValueAt;
        }
    }

    public InputDevice getInputDeviceByDescriptor(String str) {
        if (str == null) {
            throw new IllegalArgumentException("descriptor must not be null.");
        }
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            int size = this.mInputDevices.size();
            for (int i = 0; i < size; i++) {
                InputDevice inputDeviceValueAt = this.mInputDevices.valueAt(i);
                if (inputDeviceValueAt == null) {
                    try {
                        inputDeviceValueAt = this.mIm.getInputDevice(this.mInputDevices.keyAt(i));
                        if (inputDeviceValueAt == null) {
                            continue;
                        } else {
                            this.mInputDevices.setValueAt(i, inputDeviceValueAt);
                            if (!str.equals(inputDeviceValueAt.getDescriptor())) {
                                return inputDeviceValueAt;
                            }
                        }
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                } else if (!str.equals(inputDeviceValueAt.getDescriptor())) {
                }
            }
            return null;
        }
    }

    public int[] getInputDeviceIds() {
        int[] iArr;
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            int size = this.mInputDevices.size();
            iArr = new int[size];
            for (int i = 0; i < size; i++) {
                iArr[i] = this.mInputDevices.keyAt(i);
            }
        }
        return iArr;
    }

    public boolean isInputDeviceEnabled(int i) {
        try {
            return this.mIm.isInputDeviceEnabled(i);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not check enabled status of input device with id = " + i);
            throw e.rethrowFromSystemServer();
        }
    }

    public void enableInputDevice(int i) {
        try {
            this.mIm.enableInputDevice(i);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not enable input device with id = " + i);
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableInputDevice(int i) {
        try {
            this.mIm.disableInputDevice(i);
        } catch (RemoteException e) {
            Log.w(TAG, "Could not disable input device with id = " + i);
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerInputDeviceListener(InputDeviceListener inputDeviceListener, Handler handler) {
        if (inputDeviceListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mInputDevicesLock) {
            populateInputDevicesLocked();
            if (findInputDeviceListenerLocked(inputDeviceListener) < 0) {
                this.mInputDeviceListeners.add(new InputDeviceListenerDelegate(inputDeviceListener, handler));
            }
        }
    }

    public void unregisterInputDeviceListener(InputDeviceListener inputDeviceListener) {
        if (inputDeviceListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mInputDevicesLock) {
            int iFindInputDeviceListenerLocked = findInputDeviceListenerLocked(inputDeviceListener);
            if (iFindInputDeviceListenerLocked >= 0) {
                this.mInputDeviceListeners.get(iFindInputDeviceListenerLocked).removeCallbacksAndMessages(null);
                this.mInputDeviceListeners.remove(iFindInputDeviceListenerLocked);
            }
        }
    }

    private int findInputDeviceListenerLocked(InputDeviceListener inputDeviceListener) {
        int size = this.mInputDeviceListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mInputDeviceListeners.get(i).mListener == inputDeviceListener) {
                return i;
            }
        }
        return -1;
    }

    public int isInTabletMode() {
        try {
            return this.mIm.isInTabletMode();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerOnTabletModeChangedListener(OnTabletModeChangedListener onTabletModeChangedListener, Handler handler) {
        if (onTabletModeChangedListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mTabletModeLock) {
            if (this.mOnTabletModeChangedListeners == null) {
                initializeTabletModeListenerLocked();
            }
            if (findOnTabletModeChangedListenerLocked(onTabletModeChangedListener) < 0) {
                this.mOnTabletModeChangedListeners.add(new OnTabletModeChangedListenerDelegate(onTabletModeChangedListener, handler));
            }
        }
    }

    public void unregisterOnTabletModeChangedListener(OnTabletModeChangedListener onTabletModeChangedListener) {
        if (onTabletModeChangedListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mTabletModeLock) {
            int iFindOnTabletModeChangedListenerLocked = findOnTabletModeChangedListenerLocked(onTabletModeChangedListener);
            if (iFindOnTabletModeChangedListenerLocked >= 0) {
                this.mOnTabletModeChangedListeners.remove(iFindOnTabletModeChangedListenerLocked).removeCallbacksAndMessages(null);
            }
        }
    }

    private void initializeTabletModeListenerLocked() {
        TabletModeChangedListener tabletModeChangedListener = new TabletModeChangedListener();
        try {
            this.mIm.registerTabletModeChangedListener(tabletModeChangedListener);
            this.mTabletModeChangedListener = tabletModeChangedListener;
            this.mOnTabletModeChangedListeners = new ArrayList();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private int findOnTabletModeChangedListenerLocked(OnTabletModeChangedListener onTabletModeChangedListener) {
        int size = this.mOnTabletModeChangedListeners.size();
        for (int i = 0; i < size; i++) {
            if (this.mOnTabletModeChangedListeners.get(i).mListener == onTabletModeChangedListener) {
                return i;
            }
        }
        return -1;
    }

    public KeyboardLayout[] getKeyboardLayouts() {
        try {
            return this.mIm.getKeyboardLayouts();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public KeyboardLayout[] getKeyboardLayoutsForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) {
        try {
            return this.mIm.getKeyboardLayoutsForInputDevice(inputDeviceIdentifier);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public KeyboardLayout getKeyboardLayout(String str) {
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            return this.mIm.getKeyboardLayout(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) {
        try {
            return this.mIm.getCurrentKeyboardLayoutForInputDevice(inputDeviceIdentifier);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) {
        if (inputDeviceIdentifier == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            this.mIm.setCurrentKeyboardLayoutForInputDevice(inputDeviceIdentifier, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getEnabledKeyboardLayoutsForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) {
        if (inputDeviceIdentifier == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        try {
            return this.mIm.getEnabledKeyboardLayoutsForInputDevice(inputDeviceIdentifier);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) {
        if (inputDeviceIdentifier == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            this.mIm.addKeyboardLayoutForInputDevice(inputDeviceIdentifier, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) {
        if (inputDeviceIdentifier == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        try {
            this.mIm.removeKeyboardLayoutForInputDevice(inputDeviceIdentifier, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public TouchCalibration getTouchCalibration(String str, int i) {
        try {
            return this.mIm.getTouchCalibrationForInputDevice(str, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setTouchCalibration(String str, int i, TouchCalibration touchCalibration) {
        try {
            this.mIm.setTouchCalibrationForInputDevice(str, i, touchCalibration);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getPointerSpeed(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), Settings.System.POINTER_SPEED);
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }

    public void setPointerSpeed(Context context, int i) {
        if (i < -7 || i > 7) {
            throw new IllegalArgumentException("speed out of range");
        }
        Settings.System.putInt(context.getContentResolver(), Settings.System.POINTER_SPEED, i);
    }

    public void tryPointerSpeed(int i) {
        if (i < -7 || i > 7) {
            throw new IllegalArgumentException("speed out of range");
        }
        try {
            this.mIm.tryPointerSpeed(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean[] deviceHasKeys(int[] iArr) {
        return deviceHasKeys(-1, iArr);
    }

    public boolean[] deviceHasKeys(int i, int[] iArr) {
        boolean[] zArr = new boolean[iArr.length];
        try {
            this.mIm.hasKeys(i, -256, iArr, zArr);
            return zArr;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean injectInputEvent(InputEvent inputEvent, int i) {
        if (inputEvent == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        if (i != 0 && i != 2 && i != 1) {
            throw new IllegalArgumentException("mode is invalid");
        }
        try {
            return this.mIm.injectInputEvent(inputEvent, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPointerIconType(int i) {
        try {
            this.mIm.setPointerIconType(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setCustomPointerIcon(PointerIcon pointerIcon) {
        try {
            this.mIm.setCustomPointerIcon(pointerIcon);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestPointerCapture(IBinder iBinder, boolean z) {
        try {
            this.mIm.requestPointerCapture(iBinder, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IInputForwarder createInputForwarder(int i) {
        try {
            return this.mIm.createInputForwarder(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void populateInputDevicesLocked() {
        if (this.mInputDevicesChangedListener == null) {
            InputDevicesChangedListener inputDevicesChangedListener = new InputDevicesChangedListener();
            try {
                this.mIm.registerInputDevicesChangedListener(inputDevicesChangedListener);
                this.mInputDevicesChangedListener = inputDevicesChangedListener;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        if (this.mInputDevices == null) {
            try {
                int[] inputDeviceIds = this.mIm.getInputDeviceIds();
                this.mInputDevices = new SparseArray<>();
                for (int i : inputDeviceIds) {
                    this.mInputDevices.put(i, null);
                }
            } catch (RemoteException e2) {
                throw e2.rethrowFromSystemServer();
            }
        }
    }

    private void onInputDevicesChanged(int[] iArr) {
        synchronized (this.mInputDevicesLock) {
            int size = this.mInputDevices.size();
            while (true) {
                size--;
                if (size <= 0) {
                    break;
                }
                int iKeyAt = this.mInputDevices.keyAt(size);
                if (!containsDeviceId(iArr, iKeyAt)) {
                    this.mInputDevices.removeAt(size);
                    sendMessageToInputDeviceListenersLocked(2, iKeyAt);
                }
            }
            for (int i = 0; i < iArr.length; i += 2) {
                int i2 = iArr[i];
                int iIndexOfKey = this.mInputDevices.indexOfKey(i2);
                if (iIndexOfKey < 0) {
                    this.mInputDevices.put(i2, null);
                    sendMessageToInputDeviceListenersLocked(1, i2);
                } else {
                    InputDevice inputDeviceValueAt = this.mInputDevices.valueAt(iIndexOfKey);
                    if (inputDeviceValueAt != null) {
                        if (inputDeviceValueAt.getGeneration() != iArr[i + 1]) {
                            this.mInputDevices.setValueAt(iIndexOfKey, null);
                            sendMessageToInputDeviceListenersLocked(3, i2);
                        }
                    }
                }
            }
        }
    }

    private void sendMessageToInputDeviceListenersLocked(int i, int i2) {
        int size = this.mInputDeviceListeners.size();
        for (int i3 = 0; i3 < size; i3++) {
            InputDeviceListenerDelegate inputDeviceListenerDelegate = this.mInputDeviceListeners.get(i3);
            inputDeviceListenerDelegate.sendMessage(inputDeviceListenerDelegate.obtainMessage(i, i2, 0));
        }
    }

    private static boolean containsDeviceId(int[] iArr, int i) {
        for (int i2 = 0; i2 < iArr.length; i2 += 2) {
            if (iArr[i2] == i) {
                return true;
            }
        }
        return false;
    }

    private void onTabletModeChanged(long j, boolean z) {
        synchronized (this.mTabletModeLock) {
            int size = this.mOnTabletModeChangedListeners.size();
            for (int i = 0; i < size; i++) {
                this.mOnTabletModeChangedListeners.get(i).sendTabletModeChanged(j, z);
            }
        }
    }

    public Vibrator getInputDeviceVibrator(int i) {
        return new InputDeviceVibrator(i);
    }

    private final class InputDevicesChangedListener extends IInputDevicesChangedListener.Stub {
        private InputDevicesChangedListener() {
        }

        @Override
        public void onInputDevicesChanged(int[] iArr) throws RemoteException {
            InputManager.this.onInputDevicesChanged(iArr);
        }
    }

    private static final class InputDeviceListenerDelegate extends Handler {
        public final InputDeviceListener mListener;

        public InputDeviceListenerDelegate(InputDeviceListener inputDeviceListener, Handler handler) {
            super(handler != null ? handler.getLooper() : Looper.myLooper());
            this.mListener = inputDeviceListener;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    this.mListener.onInputDeviceAdded(message.arg1);
                    break;
                case 2:
                    this.mListener.onInputDeviceRemoved(message.arg1);
                    break;
                case 3:
                    this.mListener.onInputDeviceChanged(message.arg1);
                    break;
            }
        }
    }

    private final class TabletModeChangedListener extends ITabletModeChangedListener.Stub {
        private TabletModeChangedListener() {
        }

        @Override
        public void onTabletModeChanged(long j, boolean z) {
            InputManager.this.onTabletModeChanged(j, z);
        }
    }

    private static final class OnTabletModeChangedListenerDelegate extends Handler {
        private static final int MSG_TABLET_MODE_CHANGED = 0;
        public final OnTabletModeChangedListener mListener;

        public OnTabletModeChangedListenerDelegate(OnTabletModeChangedListener onTabletModeChangedListener, Handler handler) {
            super(handler != null ? handler.getLooper() : Looper.myLooper());
            this.mListener = onTabletModeChangedListener;
        }

        public void sendTabletModeChanged(long j, boolean z) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.argi1 = (int) ((-1) & j);
            someArgsObtain.argi2 = (int) (j >> 32);
            someArgsObtain.arg1 = Boolean.valueOf(z);
            obtainMessage(0, someArgsObtain).sendToTarget();
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                SomeArgs someArgs = (SomeArgs) message.obj;
                this.mListener.onTabletModeChanged((((long) someArgs.argi1) & 4294967295L) | (((long) someArgs.argi2) << 32), ((Boolean) someArgs.arg1).booleanValue());
            }
        }
    }

    private final class InputDeviceVibrator extends Vibrator {
        private final int mDeviceId;
        private final Binder mToken = new Binder();

        public InputDeviceVibrator(int i) {
            this.mDeviceId = i;
        }

        @Override
        public boolean hasVibrator() {
            return true;
        }

        @Override
        public boolean hasAmplitudeControl() {
            return false;
        }

        @Override
        public void vibrate(int i, String str, VibrationEffect vibrationEffect, AudioAttributes audioAttributes) {
            long[] timings;
            int repeatIndex;
            if (vibrationEffect instanceof VibrationEffect.OneShot) {
                timings = new long[]{0, ((VibrationEffect.OneShot) vibrationEffect).getDuration()};
                repeatIndex = -1;
            } else if (vibrationEffect instanceof VibrationEffect.Waveform) {
                VibrationEffect.Waveform waveform = (VibrationEffect.Waveform) vibrationEffect;
                timings = waveform.getTimings();
                repeatIndex = waveform.getRepeatIndex();
            } else {
                Log.w(InputManager.TAG, "Pre-baked effects aren't supported on input devices");
                return;
            }
            try {
                InputManager.this.mIm.vibrate(this.mDeviceId, timings, repeatIndex, this.mToken);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void cancel() {
            try {
                InputManager.this.mIm.cancelVibrate(this.mDeviceId, this.mToken);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }
}
