package com.android.server.input;

import android.R;
import android.app.IInputForwarder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayViewport;
import android.hardware.input.IInputDevicesChangedListener;
import android.hardware.input.IInputManager;
import android.hardware.input.ITabletModeChangedListener;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManagerInternal;
import android.hardware.input.KeyboardLayout;
import android.hardware.input.TouchCalibration;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.Display;
import android.view.IInputFilter;
import android.view.IInputFilterHost;
import android.view.IWindow;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.PointerIcon;
import android.view.ViewConfiguration;
import android.widget.Toast;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.Watchdog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import libcore.io.IoUtils;
import libcore.io.Streams;
import org.xmlpull.v1.XmlPullParser;

public class InputManagerService extends IInputManager.Stub implements Watchdog.Monitor {
    public static final int BTN_MOUSE = 272;
    static final boolean DEBUG = false;
    private static final String EXCLUDED_DEVICES_PATH = "etc/excluded-input-devices.xml";
    private static final int INJECTION_TIMEOUT_MILLIS = 30000;
    private static final int INPUT_EVENT_INJECTION_FAILED = 2;
    private static final int INPUT_EVENT_INJECTION_PERMISSION_DENIED = 1;
    private static final int INPUT_EVENT_INJECTION_SUCCEEDED = 0;
    private static final int INPUT_EVENT_INJECTION_TIMED_OUT = 3;
    public static final int KEY_STATE_DOWN = 1;
    public static final int KEY_STATE_UNKNOWN = -1;
    public static final int KEY_STATE_UP = 0;
    public static final int KEY_STATE_VIRTUAL = 2;
    private static final int MSG_DELIVER_INPUT_DEVICES_CHANGED = 1;
    private static final int MSG_DELIVER_TABLET_MODE_CHANGED = 6;
    private static final int MSG_RELOAD_DEVICE_ALIASES = 5;
    private static final int MSG_RELOAD_KEYBOARD_LAYOUTS = 3;
    private static final int MSG_SWITCH_KEYBOARD_LAYOUT = 2;
    private static final int MSG_UPDATE_KEYBOARD_LAYOUTS = 4;
    public static final int SW_CAMERA_LENS_COVER = 9;
    public static final int SW_CAMERA_LENS_COVER_BIT = 512;
    public static final int SW_HEADPHONE_INSERT = 2;
    public static final int SW_HEADPHONE_INSERT_BIT = 4;
    public static final int SW_JACK_BITS = 212;
    public static final int SW_JACK_PHYSICAL_INSERT = 7;
    public static final int SW_JACK_PHYSICAL_INSERT_BIT = 128;
    public static final int SW_KEYPAD_SLIDE = 10;
    public static final int SW_KEYPAD_SLIDE_BIT = 1024;
    public static final int SW_LID = 0;
    public static final int SW_LID_BIT = 1;
    public static final int SW_LINEOUT_INSERT = 6;
    public static final int SW_LINEOUT_INSERT_BIT = 64;
    public static final int SW_MICROPHONE_INSERT = 4;
    public static final int SW_MICROPHONE_INSERT_BIT = 16;
    public static final int SW_TABLET_MODE = 1;
    public static final int SW_TABLET_MODE_BIT = 2;
    static final String TAG = "InputManager";
    public static final int VIEWPORT_DEFAULT = 1;
    public static final int VIEWPORT_EXTERNAL = 2;
    public static final int VIEWPORT_VIRTUAL = 3;
    private final Context mContext;
    private final File mDoubleTouchGestureEnableFile;
    private IWindow mFocusedWindow;
    private boolean mFocusedWindowHasCapture;
    private boolean mInputDevicesChangedPending;
    IInputFilter mInputFilter;
    InputFilterHost mInputFilterHost;
    private PendingIntent mKeyboardLayoutIntent;
    private boolean mKeyboardLayoutNotificationShown;
    private int mNextVibratorTokenValue;
    private NotificationManager mNotificationManager;
    private final long mPtr;
    private Toast mSwitchedKeyboardLayoutToast;
    private boolean mSystemReady;
    final boolean mUseDevInputEventForAudioJack;
    private WindowManagerCallbacks mWindowManagerCallbacks;
    private WiredAccessoryCallbacks mWiredAccessoryCallbacks;
    private final Object mTabletModeLock = new Object();
    private final SparseArray<TabletModeChangedListenerRecord> mTabletModeChangedListeners = new SparseArray<>();
    private final List<TabletModeChangedListenerRecord> mTempTabletModeChangedListenersToNotify = new ArrayList();
    private final PersistentDataStore mDataStore = new PersistentDataStore();
    private Object mInputDevicesLock = new Object();
    private InputDevice[] mInputDevices = new InputDevice[0];
    private final SparseArray<InputDevicesChangedListenerRecord> mInputDevicesChangedListeners = new SparseArray<>();
    private final ArrayList<InputDevicesChangedListenerRecord> mTempInputDevicesChangedListenersToNotify = new ArrayList<>();
    private final ArrayList<InputDevice> mTempFullKeyboards = new ArrayList<>();
    private Object mVibratorLock = new Object();
    private HashMap<IBinder, VibratorToken> mVibratorTokens = new HashMap<>();
    final Object mInputFilterLock = new Object();
    private final InputManagerHandler mHandler = new InputManagerHandler(DisplayThread.get().getLooper());

    private interface KeyboardLayoutVisitor {
        void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout);
    }

    public interface WindowManagerCallbacks {
        KeyEvent dispatchUnhandledKey(InputWindowHandle inputWindowHandle, KeyEvent keyEvent, int i);

        int getPointerLayer();

        long interceptKeyBeforeDispatching(InputWindowHandle inputWindowHandle, KeyEvent keyEvent, int i);

        int interceptKeyBeforeQueueing(KeyEvent keyEvent, int i);

        int interceptMotionBeforeQueueingNonInteractive(long j, int i);

        long notifyANR(InputApplicationHandle inputApplicationHandle, InputWindowHandle inputWindowHandle, String str);

        void notifyCameraLensCoverSwitchChanged(long j, boolean z);

        void notifyConfigurationChanged();

        void notifyInputChannelBroken(InputWindowHandle inputWindowHandle);

        void notifyLidSwitchChanged(long j, boolean z);
    }

    public interface WiredAccessoryCallbacks {
        void notifyWiredAccessoryChanged(long j, int i, int i2);

        void systemReady();
    }

    private static native void nativeCancelVibrate(long j, int i, int i2);

    private static native void nativeDisableInputDevice(long j, int i);

    private static native String nativeDump(long j);

    private static native void nativeEnableInputDevice(long j, int i);

    private static native int nativeGetKeyCodeState(long j, int i, int i2, int i3);

    private static native int nativeGetScanCodeState(long j, int i, int i2, int i3);

    private static native int nativeGetSwitchState(long j, int i, int i2, int i3);

    private static native boolean nativeHasKeys(long j, int i, int i2, int[] iArr, boolean[] zArr);

    private static native long nativeInit(InputManagerService inputManagerService, Context context, MessageQueue messageQueue);

    private static native int nativeInjectInputEvent(long j, InputEvent inputEvent, int i, int i2, int i3, int i4, int i5, int i6);

    private static native boolean nativeIsInputDeviceEnabled(long j, int i);

    private static native void nativeMonitor(long j);

    private static native void nativeRegisterInputChannel(long j, InputChannel inputChannel, InputWindowHandle inputWindowHandle, boolean z);

    private static native void nativeReloadCalibration(long j);

    private static native void nativeReloadDeviceAliases(long j);

    private static native void nativeReloadKeyboardLayouts(long j);

    private static native void nativeReloadPointerIcons(long j);

    private static native void nativeSetCustomPointerIcon(long j, PointerIcon pointerIcon);

    private static native void nativeSetDisplayViewport(long j, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, String str);

    private static native void nativeSetFocusedApplication(long j, InputApplicationHandle inputApplicationHandle);

    private static native void nativeSetInputDispatchMode(long j, boolean z, boolean z2);

    private static native void nativeSetInputFilterEnabled(long j, boolean z);

    private static native void nativeSetInputWindows(long j, InputWindowHandle[] inputWindowHandleArr);

    private static native void nativeSetInteractive(long j, boolean z);

    private static native void nativeSetPointerCapture(long j, boolean z);

    private static native void nativeSetPointerIconType(long j, int i);

    private static native void nativeSetPointerSpeed(long j, int i);

    private static native void nativeSetShowTouches(long j, boolean z);

    private static native void nativeSetSystemUiVisibility(long j, int i);

    private static native void nativeSetVirtualDisplayViewports(long j, DisplayViewport[] displayViewportArr);

    private static native void nativeStart(long j);

    private static native void nativeToggleCapsLock(long j, int i);

    private static native boolean nativeTransferTouchFocus(long j, InputChannel inputChannel, InputChannel inputChannel2);

    private static native void nativeUnregisterInputChannel(long j, InputChannel inputChannel);

    private static native void nativeVibrate(long j, int i, long[] jArr, int i2, int i3);

    public InputManagerService(Context context) {
        this.mContext = context;
        this.mUseDevInputEventForAudioJack = context.getResources().getBoolean(R.^attr-private.pointerIconVectorFill);
        Slog.i(TAG, "Initializing input manager, mUseDevInputEventForAudioJack=" + this.mUseDevInputEventForAudioJack);
        this.mPtr = nativeInit(this, this.mContext, this.mHandler.getLooper().getQueue());
        String string = context.getResources().getString(R.string.activitychooserview_choose_application_error);
        this.mDoubleTouchGestureEnableFile = TextUtils.isEmpty(string) ? null : new File(string);
        LocalServices.addService(InputManagerInternal.class, new LocalService());
    }

    public void setWindowManagerCallbacks(WindowManagerCallbacks windowManagerCallbacks) {
        this.mWindowManagerCallbacks = windowManagerCallbacks;
    }

    public void setWiredAccessoryCallbacks(WiredAccessoryCallbacks wiredAccessoryCallbacks) {
        this.mWiredAccessoryCallbacks = wiredAccessoryCallbacks;
    }

    public void start() {
        Slog.i(TAG, "Starting input manager");
        nativeStart(this.mPtr);
        Watchdog.getInstance().addMonitor(this);
        registerPointerSpeedSettingObserver();
        registerShowTouchesSettingObserver();
        registerAccessibilityLargePointerSettingObserver();
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                InputManagerService.this.updatePointerSpeedFromSettings();
                InputManagerService.this.updateShowTouchesFromSettings();
                InputManagerService.this.updateAccessibilityLargePointerFromSettings();
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mHandler);
        updatePointerSpeedFromSettings();
        updateShowTouchesFromSettings();
        updateAccessibilityLargePointerFromSettings();
    }

    public void systemRunning() {
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mSystemReady = true;
        IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_REPLACED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                InputManagerService.this.updateKeyboardLayouts();
            }
        }, intentFilter, null, this.mHandler);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                InputManagerService.this.reloadDeviceAliases();
            }
        }, new IntentFilter("android.bluetooth.device.action.ALIAS_CHANGED"), null, this.mHandler);
        this.mHandler.sendEmptyMessage(5);
        this.mHandler.sendEmptyMessage(4);
        if (this.mWiredAccessoryCallbacks != null) {
            this.mWiredAccessoryCallbacks.systemReady();
        }
    }

    private void reloadKeyboardLayouts() {
        nativeReloadKeyboardLayouts(this.mPtr);
    }

    private void reloadDeviceAliases() {
        nativeReloadDeviceAliases(this.mPtr);
    }

    private void setDisplayViewportsInternal(DisplayViewport displayViewport, DisplayViewport displayViewport2, List<DisplayViewport> list) {
        if (displayViewport.valid) {
            setDisplayViewport(1, displayViewport);
        }
        if (displayViewport2.valid) {
            setDisplayViewport(2, displayViewport2);
        } else if (displayViewport.valid) {
            setDisplayViewport(2, displayViewport);
        }
        nativeSetVirtualDisplayViewports(this.mPtr, (DisplayViewport[]) list.toArray(new DisplayViewport[0]));
    }

    private void setDisplayViewport(int i, DisplayViewport displayViewport) {
        nativeSetDisplayViewport(this.mPtr, i, displayViewport.displayId, displayViewport.orientation, displayViewport.logicalFrame.left, displayViewport.logicalFrame.top, displayViewport.logicalFrame.right, displayViewport.logicalFrame.bottom, displayViewport.physicalFrame.left, displayViewport.physicalFrame.top, displayViewport.physicalFrame.right, displayViewport.physicalFrame.bottom, displayViewport.deviceWidth, displayViewport.deviceHeight, displayViewport.uniqueId);
    }

    public int getKeyCodeState(int i, int i2, int i3) {
        return nativeGetKeyCodeState(this.mPtr, i, i2, i3);
    }

    public int getScanCodeState(int i, int i2, int i3) {
        return nativeGetScanCodeState(this.mPtr, i, i2, i3);
    }

    public int getSwitchState(int i, int i2, int i3) {
        return nativeGetSwitchState(this.mPtr, i, i2, i3);
    }

    public boolean hasKeys(int i, int i2, int[] iArr, boolean[] zArr) {
        if (iArr == null) {
            throw new IllegalArgumentException("keyCodes must not be null.");
        }
        if (zArr == null || zArr.length < iArr.length) {
            throw new IllegalArgumentException("keyExists must not be null and must be at least as large as keyCodes.");
        }
        return nativeHasKeys(this.mPtr, i, i2, iArr, zArr);
    }

    public InputChannel monitorInput(String str) {
        if (str == null) {
            throw new IllegalArgumentException("inputChannelName must not be null.");
        }
        InputChannel[] inputChannelArrOpenInputChannelPair = InputChannel.openInputChannelPair(str);
        nativeRegisterInputChannel(this.mPtr, inputChannelArrOpenInputChannelPair[0], null, true);
        inputChannelArrOpenInputChannelPair[0].dispose();
        return inputChannelArrOpenInputChannelPair[1];
    }

    public void registerInputChannel(InputChannel inputChannel, InputWindowHandle inputWindowHandle) {
        if (inputChannel == null) {
            throw new IllegalArgumentException("inputChannel must not be null.");
        }
        nativeRegisterInputChannel(this.mPtr, inputChannel, inputWindowHandle, false);
    }

    public void unregisterInputChannel(InputChannel inputChannel) {
        if (inputChannel == null) {
            throw new IllegalArgumentException("inputChannel must not be null.");
        }
        nativeUnregisterInputChannel(this.mPtr, inputChannel);
    }

    public void setInputFilter(IInputFilter iInputFilter) {
        synchronized (this.mInputFilterLock) {
            IInputFilter iInputFilter2 = this.mInputFilter;
            if (iInputFilter2 == iInputFilter) {
                return;
            }
            if (iInputFilter2 != null) {
                this.mInputFilter = null;
                this.mInputFilterHost.disconnectLocked();
                this.mInputFilterHost = null;
                try {
                    iInputFilter2.uninstall();
                } catch (RemoteException e) {
                }
            }
            if (iInputFilter != null) {
                this.mInputFilter = iInputFilter;
                this.mInputFilterHost = new InputFilterHost();
                try {
                    iInputFilter.install(this.mInputFilterHost);
                } catch (RemoteException e2) {
                }
            }
            nativeSetInputFilterEnabled(this.mPtr, iInputFilter != null);
        }
    }

    public boolean injectInputEvent(InputEvent inputEvent, int i) {
        return injectInputEventInternal(inputEvent, 0, i);
    }

    private boolean injectInputEventInternal(InputEvent inputEvent, int i, int i2) {
        if (inputEvent != null) {
            if (i2 != 0 && i2 != 2 && i2 != 1) {
                throw new IllegalArgumentException("mode is invalid");
            }
            int callingPid = Binder.getCallingPid();
            int callingUid = Binder.getCallingUid();
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                int iNativeInjectInputEvent = nativeInjectInputEvent(this.mPtr, inputEvent, i, callingPid, callingUid, i2, INJECTION_TIMEOUT_MILLIS, 134217728);
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                if (iNativeInjectInputEvent != 3) {
                    switch (iNativeInjectInputEvent) {
                        case 0:
                            return true;
                        case 1:
                            Slog.w(TAG, "Input event injection from pid " + callingPid + " permission denied.");
                            throw new SecurityException("Injecting to another application requires INJECT_EVENTS permission");
                        default:
                            Slog.w(TAG, "Input event injection from pid " + callingPid + " failed.");
                            return false;
                    }
                }
                Slog.w(TAG, "Input event injection from pid " + callingPid + " timed out.");
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                throw th;
            }
        }
        throw new IllegalArgumentException("event must not be null");
    }

    public InputDevice getInputDevice(int i) {
        synchronized (this.mInputDevicesLock) {
            int length = this.mInputDevices.length;
            for (int i2 = 0; i2 < length; i2++) {
                InputDevice inputDevice = this.mInputDevices[i2];
                if (inputDevice.getId() == i) {
                    return inputDevice;
                }
            }
            return null;
        }
    }

    public boolean isInputDeviceEnabled(int i) {
        return nativeIsInputDeviceEnabled(this.mPtr, i);
    }

    public void enableInputDevice(int i) {
        if (!checkCallingPermission("android.permission.DISABLE_INPUT_DEVICE", "enableInputDevice()")) {
            throw new SecurityException("Requires DISABLE_INPUT_DEVICE permission");
        }
        nativeEnableInputDevice(this.mPtr, i);
    }

    public void disableInputDevice(int i) {
        if (!checkCallingPermission("android.permission.DISABLE_INPUT_DEVICE", "disableInputDevice()")) {
            throw new SecurityException("Requires DISABLE_INPUT_DEVICE permission");
        }
        nativeDisableInputDevice(this.mPtr, i);
    }

    public int[] getInputDeviceIds() {
        int[] iArr;
        synchronized (this.mInputDevicesLock) {
            int length = this.mInputDevices.length;
            iArr = new int[length];
            for (int i = 0; i < length; i++) {
                iArr[i] = this.mInputDevices[i].getId();
            }
        }
        return iArr;
    }

    public InputDevice[] getInputDevices() {
        InputDevice[] inputDeviceArr;
        synchronized (this.mInputDevicesLock) {
            inputDeviceArr = this.mInputDevices;
        }
        return inputDeviceArr;
    }

    public void registerInputDevicesChangedListener(IInputDevicesChangedListener iInputDevicesChangedListener) {
        if (iInputDevicesChangedListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mInputDevicesLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mInputDevicesChangedListeners.get(callingPid) != null) {
                throw new SecurityException("The calling process has already registered an InputDevicesChangedListener.");
            }
            InputDevicesChangedListenerRecord inputDevicesChangedListenerRecord = new InputDevicesChangedListenerRecord(callingPid, iInputDevicesChangedListener);
            try {
                iInputDevicesChangedListener.asBinder().linkToDeath(inputDevicesChangedListenerRecord, 0);
                this.mInputDevicesChangedListeners.put(callingPid, inputDevicesChangedListenerRecord);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onInputDevicesChangedListenerDied(int i) {
        synchronized (this.mInputDevicesLock) {
            this.mInputDevicesChangedListeners.remove(i);
        }
    }

    private void deliverInputDevicesChanged(InputDevice[] inputDeviceArr) {
        this.mTempInputDevicesChangedListenersToNotify.clear();
        this.mTempFullKeyboards.clear();
        synchronized (this.mInputDevicesLock) {
            if (this.mInputDevicesChangedPending) {
                this.mInputDevicesChangedPending = false;
                int size = this.mInputDevicesChangedListeners.size();
                for (int i = 0; i < size; i++) {
                    this.mTempInputDevicesChangedListenersToNotify.add(this.mInputDevicesChangedListeners.valueAt(i));
                }
                int length = this.mInputDevices.length;
                int[] iArr = new int[length * 2];
                int i2 = 0;
                for (int i3 = 0; i3 < length; i3++) {
                    InputDevice inputDevice = this.mInputDevices[i3];
                    int i4 = i3 * 2;
                    iArr[i4] = inputDevice.getId();
                    iArr[i4 + 1] = inputDevice.getGeneration();
                    if (!inputDevice.isVirtual() && inputDevice.isFullKeyboard()) {
                        if (!containsInputDeviceWithDescriptor(inputDeviceArr, inputDevice.getDescriptor())) {
                            this.mTempFullKeyboards.add(i2, inputDevice);
                            i2++;
                        } else {
                            this.mTempFullKeyboards.add(inputDevice);
                        }
                    }
                }
                for (int i5 = 0; i5 < size; i5++) {
                    this.mTempInputDevicesChangedListenersToNotify.get(i5).notifyInputDevicesChanged(iArr);
                }
                this.mTempInputDevicesChangedListenersToNotify.clear();
                ArrayList arrayList = new ArrayList();
                int size2 = this.mTempFullKeyboards.size();
                synchronized (this.mDataStore) {
                    for (int i6 = 0; i6 < size2; i6++) {
                        try {
                            InputDevice inputDevice2 = this.mTempFullKeyboards.get(i6);
                            String currentKeyboardLayoutForInputDevice = getCurrentKeyboardLayoutForInputDevice(inputDevice2.getIdentifier());
                            if (currentKeyboardLayoutForInputDevice == null && (currentKeyboardLayoutForInputDevice = getDefaultKeyboardLayout(inputDevice2)) != null) {
                                setCurrentKeyboardLayoutForInputDevice(inputDevice2.getIdentifier(), currentKeyboardLayoutForInputDevice);
                            }
                            if (currentKeyboardLayoutForInputDevice == null) {
                                setCurrentKeyboardLayoutForInputDevice(inputDevice2.getIdentifier(), "jp.co.omronsoft.iwnnime.ml/jp.co.omronsoft.iwnnime.ml.InputDeviceReceiver/japanese_layout");
                            }
                        } catch (Throwable th) {
                            throw th;
                        }
                    }
                }
                if (this.mNotificationManager != null) {
                    if (!arrayList.isEmpty()) {
                        if (arrayList.size() > 1) {
                            showMissingKeyboardLayoutNotification(null);
                        } else {
                            showMissingKeyboardLayoutNotification((InputDevice) arrayList.get(0));
                        }
                    } else if (this.mKeyboardLayoutNotificationShown) {
                        hideMissingKeyboardLayoutNotification();
                    }
                }
                this.mTempFullKeyboards.clear();
            }
        }
    }

    private String getDefaultKeyboardLayout(final InputDevice inputDevice) {
        final Locale locale = this.mContext.getResources().getConfiguration().locale;
        if (TextUtils.isEmpty(locale.getLanguage())) {
            return null;
        }
        final ArrayList arrayList = new ArrayList();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() {
            @Override
            public void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                if (keyboardLayout.getVendorId() != inputDevice.getVendorId() || keyboardLayout.getProductId() != inputDevice.getProductId()) {
                    return;
                }
                LocaleList locales = keyboardLayout.getLocales();
                int size = locales.size();
                for (int i2 = 0; i2 < size; i2++) {
                    if (InputManagerService.isCompatibleLocale(locale, locales.get(i2))) {
                        arrayList.add(keyboardLayout);
                        return;
                    }
                }
            }
        });
        if (arrayList.isEmpty()) {
            return null;
        }
        Collections.sort(arrayList);
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            KeyboardLayout keyboardLayout = (KeyboardLayout) arrayList.get(i);
            LocaleList locales = keyboardLayout.getLocales();
            int size2 = locales.size();
            for (int i2 = 0; i2 < size2; i2++) {
                Locale locale2 = locales.get(i2);
                if (locale2.getCountry().equals(locale.getCountry()) && locale2.getVariant().equals(locale.getVariant())) {
                    return keyboardLayout.getDescriptor();
                }
            }
        }
        for (int i3 = 0; i3 < size; i3++) {
            KeyboardLayout keyboardLayout2 = (KeyboardLayout) arrayList.get(i3);
            LocaleList locales2 = keyboardLayout2.getLocales();
            int size3 = locales2.size();
            for (int i4 = 0; i4 < size3; i4++) {
                if (locales2.get(i4).getCountry().equals(locale.getCountry())) {
                    return keyboardLayout2.getDescriptor();
                }
            }
        }
        return ((KeyboardLayout) arrayList.get(0)).getDescriptor();
    }

    private static boolean isCompatibleLocale(Locale locale, Locale locale2) {
        if (locale.getLanguage().equals(locale2.getLanguage())) {
            return TextUtils.isEmpty(locale.getCountry()) || TextUtils.isEmpty(locale2.getCountry()) || locale.getCountry().equals(locale2.getCountry());
        }
        return false;
    }

    public TouchCalibration getTouchCalibrationForInputDevice(String str, int i) {
        TouchCalibration touchCalibration;
        if (str == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        synchronized (this.mDataStore) {
            touchCalibration = this.mDataStore.getTouchCalibration(str, i);
        }
        return touchCalibration;
    }

    public void setTouchCalibrationForInputDevice(String str, int i, TouchCalibration touchCalibration) {
        if (!checkCallingPermission("android.permission.SET_INPUT_CALIBRATION", "setTouchCalibrationForInputDevice()")) {
            throw new SecurityException("Requires SET_INPUT_CALIBRATION permission");
        }
        if (str == null) {
            throw new IllegalArgumentException("inputDeviceDescriptor must not be null");
        }
        if (touchCalibration == null) {
            throw new IllegalArgumentException("calibration must not be null");
        }
        if (i < 0 || i > 3) {
            throw new IllegalArgumentException("surfaceRotation value out of bounds");
        }
        synchronized (this.mDataStore) {
            try {
                if (this.mDataStore.setTouchCalibration(str, i, touchCalibration)) {
                    nativeReloadCalibration(this.mPtr);
                }
            } finally {
                this.mDataStore.saveIfNeeded();
            }
        }
    }

    public int isInTabletMode() {
        if (!checkCallingPermission("android.permission.TABLET_MODE", "isInTabletMode()")) {
            throw new SecurityException("Requires TABLET_MODE permission");
        }
        return getSwitchState(-1, -256, 1);
    }

    public void registerTabletModeChangedListener(ITabletModeChangedListener iTabletModeChangedListener) {
        if (!checkCallingPermission("android.permission.TABLET_MODE", "registerTabletModeChangedListener()")) {
            throw new SecurityException("Requires TABLET_MODE_LISTENER permission");
        }
        if (iTabletModeChangedListener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mTabletModeLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mTabletModeChangedListeners.get(callingPid) != null) {
                throw new IllegalStateException("The calling process has already registered a TabletModeChangedListener.");
            }
            TabletModeChangedListenerRecord tabletModeChangedListenerRecord = new TabletModeChangedListenerRecord(callingPid, iTabletModeChangedListener);
            try {
                iTabletModeChangedListener.asBinder().linkToDeath(tabletModeChangedListenerRecord, 0);
                this.mTabletModeChangedListeners.put(callingPid, tabletModeChangedListenerRecord);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onTabletModeChangedListenerDied(int i) {
        synchronized (this.mTabletModeLock) {
            this.mTabletModeChangedListeners.remove(i);
        }
    }

    private void deliverTabletModeChanged(long j, boolean z) {
        int size;
        int i;
        this.mTempTabletModeChangedListenersToNotify.clear();
        synchronized (this.mTabletModeLock) {
            size = this.mTabletModeChangedListeners.size();
            for (int i2 = 0; i2 < size; i2++) {
                this.mTempTabletModeChangedListenersToNotify.add(this.mTabletModeChangedListeners.valueAt(i2));
            }
        }
        for (i = 0; i < size; i++) {
            this.mTempTabletModeChangedListenersToNotify.get(i).notifyTabletModeChanged(j, z);
        }
    }

    private void showMissingKeyboardLayoutNotification(InputDevice inputDevice) {
        if (!this.mKeyboardLayoutNotificationShown) {
            Intent intent = new Intent("android.settings.HARD_KEYBOARD_SETTINGS");
            if (inputDevice != null) {
                intent.putExtra("input_device_identifier", (Parcelable) inputDevice.getIdentifier());
            }
            intent.setFlags(337641472);
            PendingIntent activityAsUser = BenesseExtension.getDchaState() == 0 ? PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT) : null;
            Resources resources = this.mContext.getResources();
            this.mNotificationManager.notifyAsUser(null, 19, new Notification.Builder(this.mContext, SystemNotificationChannels.PHYSICAL_KEYBOARD).setContentTitle(resources.getString(R.string.lockscreen_pattern_instructions)).setContentText(resources.getString(R.string.lockscreen_pattern_correct)).setContentIntent(activityAsUser).setSmallIcon(R.drawable.ic_media_route_connected_light_18_mtrl).setColor(this.mContext.getColor(R.color.car_colorPrimary)).build(), UserHandle.ALL);
            this.mKeyboardLayoutNotificationShown = true;
        }
    }

    private void hideMissingKeyboardLayoutNotification() {
        if (this.mKeyboardLayoutNotificationShown) {
            this.mKeyboardLayoutNotificationShown = false;
            this.mNotificationManager.cancelAsUser(null, 19, UserHandle.ALL);
        }
    }

    private void updateKeyboardLayouts() {
        final HashSet hashSet = new HashSet();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() {
            @Override
            public void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                hashSet.add(keyboardLayout.getDescriptor());
            }
        });
        synchronized (this.mDataStore) {
            try {
                this.mDataStore.removeUninstalledKeyboardLayouts(hashSet);
            } finally {
                this.mDataStore.saveIfNeeded();
            }
        }
        reloadKeyboardLayouts();
    }

    private static boolean containsInputDeviceWithDescriptor(InputDevice[] inputDeviceArr, String str) {
        for (InputDevice inputDevice : inputDeviceArr) {
            if (inputDevice.getDescriptor().equals(str)) {
                return true;
            }
        }
        return false;
    }

    public KeyboardLayout[] getKeyboardLayouts() {
        final ArrayList arrayList = new ArrayList();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() {
            @Override
            public void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                arrayList.add(keyboardLayout);
            }
        });
        return (KeyboardLayout[]) arrayList.toArray(new KeyboardLayout[arrayList.size()]);
    }

    public KeyboardLayout[] getKeyboardLayoutsForInputDevice(final InputDeviceIdentifier inputDeviceIdentifier) {
        final String[] enabledKeyboardLayoutsForInputDevice = getEnabledKeyboardLayoutsForInputDevice(inputDeviceIdentifier);
        final ArrayList arrayList = new ArrayList(enabledKeyboardLayoutsForInputDevice.length);
        final ArrayList arrayList2 = new ArrayList();
        visitAllKeyboardLayouts(new KeyboardLayoutVisitor() {
            boolean mHasSeenDeviceSpecificLayout;

            @Override
            public void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                for (String str : enabledKeyboardLayoutsForInputDevice) {
                    if (str != null && str.equals(keyboardLayout.getDescriptor())) {
                        arrayList.add(keyboardLayout);
                        return;
                    }
                }
                if (keyboardLayout.getVendorId() == inputDeviceIdentifier.getVendorId() && keyboardLayout.getProductId() == inputDeviceIdentifier.getProductId()) {
                    if (!this.mHasSeenDeviceSpecificLayout) {
                        this.mHasSeenDeviceSpecificLayout = true;
                        arrayList2.clear();
                    }
                    arrayList2.add(keyboardLayout);
                    return;
                }
                if (keyboardLayout.getVendorId() == -1 && keyboardLayout.getProductId() == -1 && !this.mHasSeenDeviceSpecificLayout) {
                    arrayList2.add(keyboardLayout);
                }
            }
        });
        int size = arrayList.size();
        int size2 = arrayList2.size();
        KeyboardLayout[] keyboardLayoutArr = new KeyboardLayout[size + size2];
        arrayList.toArray(keyboardLayoutArr);
        for (int i = 0; i < size2; i++) {
            keyboardLayoutArr[size + i] = (KeyboardLayout) arrayList2.get(i);
        }
        return keyboardLayoutArr;
    }

    public KeyboardLayout getKeyboardLayout(String str) {
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        final KeyboardLayout[] keyboardLayoutArr = new KeyboardLayout[1];
        visitKeyboardLayout(str, new KeyboardLayoutVisitor() {
            @Override
            public void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                keyboardLayoutArr[0] = keyboardLayout;
            }
        });
        if (keyboardLayoutArr[0] == null) {
            Slog.w(TAG, "Could not get keyboard layout with descriptor '" + str + "'.");
        }
        return keyboardLayoutArr[0];
    }

    private void visitAllKeyboardLayouts(KeyboardLayoutVisitor keyboardLayoutVisitor) {
        PackageManager packageManager = this.mContext.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryBroadcastReceivers(new Intent("android.hardware.input.action.QUERY_KEYBOARD_LAYOUTS"), 786560)) {
            visitKeyboardLayoutsInPackage(packageManager, resolveInfo.activityInfo, null, resolveInfo.priority, keyboardLayoutVisitor);
        }
    }

    private void visitKeyboardLayout(String str, KeyboardLayoutVisitor keyboardLayoutVisitor) {
        KeyboardLayoutDescriptor keyboardLayoutDescriptor = KeyboardLayoutDescriptor.parse(str);
        if (keyboardLayoutDescriptor != null) {
            PackageManager packageManager = this.mContext.getPackageManager();
            try {
                visitKeyboardLayoutsInPackage(packageManager, packageManager.getReceiverInfo(new ComponentName(keyboardLayoutDescriptor.packageName, keyboardLayoutDescriptor.receiverName), 786560), keyboardLayoutDescriptor.keyboardLayoutName, 0, keyboardLayoutVisitor);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    private void visitKeyboardLayoutsInPackage(PackageManager packageManager, ActivityInfo activityInfo, String str, int i, KeyboardLayoutVisitor keyboardLayoutVisitor) {
        TypedArray typedArray;
        Object obj = str;
        Bundle bundle = activityInfo.metaData;
        if (bundle == null) {
            return;
        }
        int i2 = bundle.getInt("android.hardware.input.metadata.KEYBOARD_LAYOUTS");
        if (i2 == 0) {
            Slog.w(TAG, "Missing meta-data 'android.hardware.input.metadata.KEYBOARD_LAYOUTS' on receiver " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name);
            return;
        }
        CharSequence charSequenceLoadLabel = activityInfo.loadLabel(packageManager);
        String string = charSequenceLoadLabel != null ? charSequenceLoadLabel.toString() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        int i3 = 1;
        int i4 = 0;
        int i5 = (activityInfo.applicationInfo.flags & 1) != 0 ? i : 0;
        try {
            Resources resourcesForApplication = packageManager.getResourcesForApplication(activityInfo.applicationInfo);
            XmlResourceParser xml = resourcesForApplication.getXml(i2);
            try {
                XmlUtils.beginDocument(xml, "keyboard-layouts");
                while (true) {
                    XmlUtils.nextElement(xml);
                    String name = xml.getName();
                    if (name != null) {
                        if (name.equals("keyboard-layout")) {
                            TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(xml, com.android.internal.R.styleable.KeyboardLayout);
                            try {
                                String string2 = typedArrayObtainAttributes.getString(i3);
                                String string3 = typedArrayObtainAttributes.getString(i4);
                                int resourceId = typedArrayObtainAttributes.getResourceId(2, i4);
                                LocaleList localesFromLanguageTags = getLocalesFromLanguageTags(typedArrayObtainAttributes.getString(3));
                                int i6 = typedArrayObtainAttributes.getInt(5, -1);
                                int i7 = typedArrayObtainAttributes.getInt(4, -1);
                                if (string2 == null || string3 == null || resourceId == 0) {
                                    typedArray = typedArrayObtainAttributes;
                                    Slog.w(TAG, "Missing required 'name', 'label' or 'keyboardLayout' attributes in keyboard layout resource from receiver " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name);
                                } else {
                                    String str2 = KeyboardLayoutDescriptor.format(activityInfo.packageName, activityInfo.name, string2);
                                    if (obj == null || string2.equals(obj)) {
                                        typedArray = typedArrayObtainAttributes;
                                        try {
                                            keyboardLayoutVisitor.visitKeyboardLayout(resourcesForApplication, resourceId, new KeyboardLayout(str2, string3, string, i5, localesFromLanguageTags, i6, i7));
                                        } catch (Throwable th) {
                                            th = th;
                                            typedArray.recycle();
                                            throw th;
                                        }
                                    } else {
                                        typedArray = typedArrayObtainAttributes;
                                    }
                                }
                                typedArray.recycle();
                            } catch (Throwable th2) {
                                th = th2;
                                typedArray = typedArrayObtainAttributes;
                            }
                        } else {
                            Slog.w(TAG, "Skipping unrecognized element '" + name + "' in keyboard layout resource from receiver " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name);
                        }
                        obj = str;
                        i3 = 1;
                        i4 = 0;
                    } else {
                        return;
                    }
                }
            } finally {
                xml.close();
            }
        } catch (Exception e) {
            Slog.w(TAG, "Could not parse keyboard layout resource from receiver " + activityInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + activityInfo.name, e);
        }
    }

    private static LocaleList getLocalesFromLanguageTags(String str) {
        if (TextUtils.isEmpty(str)) {
            return LocaleList.getEmptyLocaleList();
        }
        return LocaleList.forLanguageTags(str.replace('|', ','));
    }

    private String getLayoutDescriptor(InputDeviceIdentifier inputDeviceIdentifier) {
        if (inputDeviceIdentifier == null || inputDeviceIdentifier.getDescriptor() == null) {
            throw new IllegalArgumentException("identifier and descriptor must not be null");
        }
        if (inputDeviceIdentifier.getVendorId() == 0 && inputDeviceIdentifier.getProductId() == 0) {
            return inputDeviceIdentifier.getDescriptor();
        }
        return "vendor:" + inputDeviceIdentifier.getVendorId() + ",product:" + inputDeviceIdentifier.getProductId();
    }

    public String getCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) {
        String currentKeyboardLayout;
        String layoutDescriptor = getLayoutDescriptor(inputDeviceIdentifier);
        synchronized (this.mDataStore) {
            currentKeyboardLayout = this.mDataStore.getCurrentKeyboardLayout(layoutDescriptor);
            if (currentKeyboardLayout == null && !layoutDescriptor.equals(inputDeviceIdentifier.getDescriptor())) {
                currentKeyboardLayout = this.mDataStore.getCurrentKeyboardLayout(inputDeviceIdentifier.getDescriptor());
            }
        }
        return currentKeyboardLayout;
    }

    public void setCurrentKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) {
        if (!checkCallingPermission("android.permission.SET_KEYBOARD_LAYOUT", "setCurrentKeyboardLayoutForInputDevice()")) {
            throw new SecurityException("Requires SET_KEYBOARD_LAYOUT permission");
        }
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        String layoutDescriptor = getLayoutDescriptor(inputDeviceIdentifier);
        synchronized (this.mDataStore) {
            try {
                if (this.mDataStore.setCurrentKeyboardLayout(layoutDescriptor, str)) {
                    this.mHandler.sendEmptyMessage(3);
                }
            } finally {
                this.mDataStore.saveIfNeeded();
            }
        }
    }

    public String[] getEnabledKeyboardLayoutsForInputDevice(InputDeviceIdentifier inputDeviceIdentifier) {
        String[] keyboardLayouts;
        String layoutDescriptor = getLayoutDescriptor(inputDeviceIdentifier);
        synchronized (this.mDataStore) {
            keyboardLayouts = this.mDataStore.getKeyboardLayouts(layoutDescriptor);
            if ((keyboardLayouts == null || keyboardLayouts.length == 0) && !layoutDescriptor.equals(inputDeviceIdentifier.getDescriptor())) {
                keyboardLayouts = this.mDataStore.getKeyboardLayouts(inputDeviceIdentifier.getDescriptor());
            }
        }
        return keyboardLayouts;
    }

    public void addKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) {
        if (!checkCallingPermission("android.permission.SET_KEYBOARD_LAYOUT", "addKeyboardLayoutForInputDevice()")) {
            throw new SecurityException("Requires SET_KEYBOARD_LAYOUT permission");
        }
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        String layoutDescriptor = getLayoutDescriptor(inputDeviceIdentifier);
        synchronized (this.mDataStore) {
            try {
                String currentKeyboardLayout = this.mDataStore.getCurrentKeyboardLayout(layoutDescriptor);
                if (currentKeyboardLayout == null && !layoutDescriptor.equals(inputDeviceIdentifier.getDescriptor())) {
                    currentKeyboardLayout = this.mDataStore.getCurrentKeyboardLayout(inputDeviceIdentifier.getDescriptor());
                }
                if (this.mDataStore.addKeyboardLayout(layoutDescriptor, str) && !Objects.equals(currentKeyboardLayout, this.mDataStore.getCurrentKeyboardLayout(layoutDescriptor))) {
                    this.mHandler.sendEmptyMessage(3);
                }
            } finally {
                this.mDataStore.saveIfNeeded();
            }
        }
    }

    public void removeKeyboardLayoutForInputDevice(InputDeviceIdentifier inputDeviceIdentifier, String str) {
        if (!checkCallingPermission("android.permission.SET_KEYBOARD_LAYOUT", "removeKeyboardLayoutForInputDevice()")) {
            throw new SecurityException("Requires SET_KEYBOARD_LAYOUT permission");
        }
        if (str == null) {
            throw new IllegalArgumentException("keyboardLayoutDescriptor must not be null");
        }
        String layoutDescriptor = getLayoutDescriptor(inputDeviceIdentifier);
        synchronized (this.mDataStore) {
            try {
                String currentKeyboardLayout = this.mDataStore.getCurrentKeyboardLayout(layoutDescriptor);
                if (currentKeyboardLayout == null && !layoutDescriptor.equals(inputDeviceIdentifier.getDescriptor())) {
                    currentKeyboardLayout = this.mDataStore.getCurrentKeyboardLayout(inputDeviceIdentifier.getDescriptor());
                }
                boolean zRemoveKeyboardLayout = this.mDataStore.removeKeyboardLayout(layoutDescriptor, str);
                if (!layoutDescriptor.equals(inputDeviceIdentifier.getDescriptor())) {
                    zRemoveKeyboardLayout |= this.mDataStore.removeKeyboardLayout(inputDeviceIdentifier.getDescriptor(), str);
                }
                if (zRemoveKeyboardLayout && !Objects.equals(currentKeyboardLayout, this.mDataStore.getCurrentKeyboardLayout(layoutDescriptor))) {
                    this.mHandler.sendEmptyMessage(3);
                }
            } finally {
                this.mDataStore.saveIfNeeded();
            }
        }
    }

    public void switchKeyboardLayout(int i, int i2) {
        this.mHandler.obtainMessage(2, i, i2).sendToTarget();
    }

    private void handleSwitchKeyboardLayout(int i, int i2) {
        boolean zSwitchKeyboardLayout;
        String currentKeyboardLayout;
        KeyboardLayout keyboardLayout;
        InputDevice inputDevice = getInputDevice(i);
        if (inputDevice != null) {
            String layoutDescriptor = getLayoutDescriptor(inputDevice.getIdentifier());
            synchronized (this.mDataStore) {
                try {
                    zSwitchKeyboardLayout = this.mDataStore.switchKeyboardLayout(layoutDescriptor, i2);
                    currentKeyboardLayout = this.mDataStore.getCurrentKeyboardLayout(layoutDescriptor);
                } finally {
                    this.mDataStore.saveIfNeeded();
                }
            }
            if (zSwitchKeyboardLayout) {
                if (this.mSwitchedKeyboardLayoutToast != null) {
                    this.mSwitchedKeyboardLayoutToast.cancel();
                    this.mSwitchedKeyboardLayoutToast = null;
                }
                if (currentKeyboardLayout != null && (keyboardLayout = getKeyboardLayout(currentKeyboardLayout)) != null) {
                    this.mSwitchedKeyboardLayoutToast = Toast.makeText(this.mContext, keyboardLayout.getLabel(), 0);
                    this.mSwitchedKeyboardLayoutToast.show();
                }
                reloadKeyboardLayouts();
            }
        }
    }

    public void setInputWindows(InputWindowHandle[] inputWindowHandleArr, InputWindowHandle inputWindowHandle) {
        IWindow iWindow = inputWindowHandle != null ? inputWindowHandle.clientWindow : null;
        if (this.mFocusedWindow != iWindow) {
            this.mFocusedWindow = iWindow;
            if (this.mFocusedWindowHasCapture) {
                setPointerCapture(false);
            }
        }
        nativeSetInputWindows(this.mPtr, inputWindowHandleArr);
    }

    public void setFocusedApplication(InputApplicationHandle inputApplicationHandle) {
        nativeSetFocusedApplication(this.mPtr, inputApplicationHandle);
    }

    public void requestPointerCapture(IBinder iBinder, boolean z) {
        if (this.mFocusedWindow == null || this.mFocusedWindow.asBinder() != iBinder) {
            Slog.e(TAG, "requestPointerCapture called for a window that has no focus: " + iBinder);
            return;
        }
        if (this.mFocusedWindowHasCapture == z) {
            StringBuilder sb = new StringBuilder();
            sb.append("requestPointerCapture: already ");
            sb.append(z ? "enabled" : "disabled");
            Slog.i(TAG, sb.toString());
            return;
        }
        setPointerCapture(z);
        try {
            this.mFocusedWindow.dispatchPointerCaptureChanged(z);
        } catch (RemoteException e) {
        }
    }

    private void setPointerCapture(boolean z) {
        this.mFocusedWindowHasCapture = z;
        nativeSetPointerCapture(this.mPtr, z);
    }

    public void setInputDispatchMode(boolean z, boolean z2) {
        nativeSetInputDispatchMode(this.mPtr, z, z2);
    }

    public void setSystemUiVisibility(int i) {
        nativeSetSystemUiVisibility(this.mPtr, i);
    }

    public boolean transferTouchFocus(InputChannel inputChannel, InputChannel inputChannel2) {
        if (inputChannel == null) {
            throw new IllegalArgumentException("fromChannel must not be null.");
        }
        if (inputChannel2 == null) {
            throw new IllegalArgumentException("toChannel must not be null.");
        }
        return nativeTransferTouchFocus(this.mPtr, inputChannel, inputChannel2);
    }

    public void tryPointerSpeed(int i) {
        if (!checkCallingPermission("android.permission.SET_POINTER_SPEED", "tryPointerSpeed()")) {
            throw new SecurityException("Requires SET_POINTER_SPEED permission");
        }
        if (i < -7 || i > 7) {
            throw new IllegalArgumentException("speed out of range");
        }
        setPointerSpeedUnchecked(i);
    }

    public void updatePointerSpeedFromSettings() {
        setPointerSpeedUnchecked(getPointerSpeedSetting());
    }

    private void setPointerSpeedUnchecked(int i) {
        nativeSetPointerSpeed(this.mPtr, Math.min(Math.max(i, -7), 7));
    }

    private void registerPointerSpeedSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("pointer_speed"), true, new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                InputManagerService.this.updatePointerSpeedFromSettings();
            }
        }, -1);
    }

    private int getPointerSpeedSetting() {
        try {
            return Settings.System.getIntForUser(this.mContext.getContentResolver(), "pointer_speed", -2);
        } catch (Settings.SettingNotFoundException e) {
            return 0;
        }
    }

    public void updateShowTouchesFromSettings() {
        nativeSetShowTouches(this.mPtr, getShowTouchesSetting(0) != 0);
    }

    private void registerShowTouchesSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("show_touches"), true, new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                InputManagerService.this.updateShowTouchesFromSettings();
            }
        }, -1);
    }

    public void updateAccessibilityLargePointerFromSettings() {
        boolean z = true;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_large_pointer_icon", 0, -2) != 1) {
            z = false;
        }
        PointerIcon.setUseLargeIcons(z);
        nativeReloadPointerIcons(this.mPtr);
    }

    private void registerAccessibilityLargePointerSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("accessibility_large_pointer_icon"), true, new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                InputManagerService.this.updateAccessibilityLargePointerFromSettings();
            }
        }, -1);
    }

    private int getShowTouchesSetting(int i) {
        try {
            return Settings.System.getIntForUser(this.mContext.getContentResolver(), "show_touches", -2);
        } catch (Settings.SettingNotFoundException e) {
            return i;
        }
    }

    public void vibrate(int i, long[] jArr, int i2, IBinder iBinder) {
        VibratorToken vibratorToken;
        if (i2 >= jArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        synchronized (this.mVibratorLock) {
            vibratorToken = this.mVibratorTokens.get(iBinder);
            if (vibratorToken == null) {
                int i3 = this.mNextVibratorTokenValue;
                this.mNextVibratorTokenValue = i3 + 1;
                vibratorToken = new VibratorToken(i, iBinder, i3);
                try {
                    iBinder.linkToDeath(vibratorToken, 0);
                    this.mVibratorTokens.put(iBinder, vibratorToken);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        synchronized (vibratorToken) {
            vibratorToken.mVibrating = true;
            nativeVibrate(this.mPtr, i, jArr, i2, vibratorToken.mTokenValue);
        }
    }

    public void cancelVibrate(int i, IBinder iBinder) {
        synchronized (this.mVibratorLock) {
            VibratorToken vibratorToken = this.mVibratorTokens.get(iBinder);
            if (vibratorToken != null && vibratorToken.mDeviceId == i) {
                cancelVibrateIfNeeded(vibratorToken);
            }
        }
    }

    void onVibratorTokenDied(VibratorToken vibratorToken) {
        synchronized (this.mVibratorLock) {
            this.mVibratorTokens.remove(vibratorToken.mToken);
        }
        cancelVibrateIfNeeded(vibratorToken);
    }

    private void cancelVibrateIfNeeded(VibratorToken vibratorToken) {
        synchronized (vibratorToken) {
            if (vibratorToken.mVibrating) {
                nativeCancelVibrate(this.mPtr, vibratorToken.mDeviceId, vibratorToken.mTokenValue);
                vibratorToken.mVibrating = false;
            }
        }
    }

    public void setPointerIconType(int i) {
        nativeSetPointerIconType(this.mPtr, i);
    }

    public void setCustomPointerIcon(PointerIcon pointerIcon) {
        Preconditions.checkNotNull(pointerIcon);
        nativeSetCustomPointerIcon(this.mPtr, pointerIcon);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.println("INPUT MANAGER (dumpsys input)\n");
            String strNativeDump = nativeDump(this.mPtr);
            if (strNativeDump != null) {
                printWriter.println(strNativeDump);
            }
        }
    }

    private boolean checkCallingPermission(String str, String str2) {
        if (Binder.getCallingPid() == Process.myPid() || this.mContext.checkCallingPermission(str) == 0) {
            return true;
        }
        Slog.w(TAG, "Permission Denial: " + str2 + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + str);
        return false;
    }

    @Override
    public void monitor() {
        synchronized (this.mInputFilterLock) {
        }
        nativeMonitor(this.mPtr);
    }

    public IInputForwarder createInputForwarder(int i) throws RemoteException {
        if (!checkCallingPermission("android.permission.INJECT_EVENTS", "createInputForwarder()")) {
            throw new SecurityException("Requires INJECT_EVENTS permission");
        }
        Display display = ((DisplayManager) this.mContext.getSystemService(DisplayManager.class)).getDisplay(i);
        if (display == null) {
            throw new IllegalArgumentException("Can't create input forwarder for non-existent displayId: " + i);
        }
        if (Binder.getCallingUid() != display.getOwnerUid()) {
            throw new SecurityException("Only owner of the display can forward input events to it.");
        }
        return new InputForwarder(i);
    }

    private void notifyConfigurationChanged(long j) {
        this.mWindowManagerCallbacks.notifyConfigurationChanged();
    }

    private void notifyInputDevicesChanged(InputDevice[] inputDeviceArr) {
        synchronized (this.mInputDevicesLock) {
            if (!this.mInputDevicesChangedPending) {
                this.mInputDevicesChangedPending = true;
                this.mHandler.obtainMessage(1, this.mInputDevices).sendToTarget();
            }
            this.mInputDevices = inputDeviceArr;
        }
    }

    private void notifySwitch(long j, int i, int i2) {
        if ((i2 & 1) != 0) {
            this.mWindowManagerCallbacks.notifyLidSwitchChanged(j, (i & 1) == 0);
        }
        if ((i2 & 512) != 0) {
            this.mWindowManagerCallbacks.notifyCameraLensCoverSwitchChanged(j, (i & 512) != 0);
        }
        if (this.mUseDevInputEventForAudioJack && (i2 & SW_JACK_BITS) != 0) {
            this.mWiredAccessoryCallbacks.notifyWiredAccessoryChanged(j, i, i2);
        }
        if ((i2 & 2) != 0) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.argi1 = (int) ((-1) & j);
            someArgsObtain.argi2 = (int) (j >> 32);
            someArgsObtain.arg1 = Boolean.valueOf((i & 2) != 0);
            this.mHandler.obtainMessage(6, someArgsObtain).sendToTarget();
        }
    }

    private void notifyInputChannelBroken(InputWindowHandle inputWindowHandle) {
        this.mWindowManagerCallbacks.notifyInputChannelBroken(inputWindowHandle);
    }

    private long notifyANR(InputApplicationHandle inputApplicationHandle, InputWindowHandle inputWindowHandle, String str) {
        return this.mWindowManagerCallbacks.notifyANR(inputApplicationHandle, inputWindowHandle, str);
    }

    final boolean filterInputEvent(InputEvent inputEvent, int i) {
        synchronized (this.mInputFilterLock) {
            if (this.mInputFilter != null) {
                try {
                    this.mInputFilter.filterInputEvent(inputEvent, i);
                } catch (RemoteException e) {
                }
                return false;
            }
            inputEvent.recycle();
            return true;
        }
    }

    private int interceptKeyBeforeQueueing(KeyEvent keyEvent, int i) {
        return this.mWindowManagerCallbacks.interceptKeyBeforeQueueing(keyEvent, i);
    }

    private int interceptMotionBeforeQueueingNonInteractive(long j, int i) {
        return this.mWindowManagerCallbacks.interceptMotionBeforeQueueingNonInteractive(j, i);
    }

    private long interceptKeyBeforeDispatching(InputWindowHandle inputWindowHandle, KeyEvent keyEvent, int i) {
        return this.mWindowManagerCallbacks.interceptKeyBeforeDispatching(inputWindowHandle, keyEvent, i);
    }

    private KeyEvent dispatchUnhandledKey(InputWindowHandle inputWindowHandle, KeyEvent keyEvent, int i) {
        return this.mWindowManagerCallbacks.dispatchUnhandledKey(inputWindowHandle, keyEvent, i);
    }

    private boolean checkInjectEventsPermission(int i, int i2) {
        return this.mContext.checkPermission("android.permission.INJECT_EVENTS", i, i2) == 0;
    }

    private int getVirtualKeyQuietTimeMillis() {
        return this.mContext.getResources().getInteger(R.integer.config_fixedRefreshRateInHighZone);
    }

    private String[] getExcludedDeviceNames() throws Throwable {
        FileReader fileReader;
        Exception e;
        ArrayList arrayList = new ArrayList();
        File rootDirectory = Environment.getRootDirectory();
        FileReader fileReader2 = EXCLUDED_DEVICES_PATH;
        File file = new File(rootDirectory, EXCLUDED_DEVICES_PATH);
        try {
        } catch (Throwable th) {
            th = th;
        }
        try {
            try {
                fileReader = new FileReader(file);
            } catch (IOException e2) {
            }
            try {
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileReader);
                XmlUtils.beginDocument(xmlPullParserNewPullParser, "devices");
                while (true) {
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    if (!"device".equals(xmlPullParserNewPullParser.getName())) {
                        break;
                    }
                    String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, com.android.server.pm.Settings.ATTR_NAME);
                    if (attributeValue != null) {
                        arrayList.add(attributeValue);
                    }
                }
                fileReader.close();
            } catch (FileNotFoundException e3) {
                if (fileReader != null) {
                    fileReader.close();
                }
                return (String[]) arrayList.toArray(new String[arrayList.size()]);
            } catch (Exception e4) {
                e = e4;
                Slog.e(TAG, "Exception while parsing '" + file.getAbsolutePath() + "'", e);
                if (fileReader != null) {
                    fileReader.close();
                }
                return (String[]) arrayList.toArray(new String[arrayList.size()]);
            }
        } catch (FileNotFoundException e5) {
            fileReader = null;
        } catch (Exception e6) {
            fileReader = null;
            e = e6;
        } catch (Throwable th2) {
            th = th2;
            fileReader2 = 0;
            if (fileReader2 != 0) {
                try {
                    fileReader2.close();
                } catch (IOException e7) {
                }
            }
            throw th;
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    private int getKeyRepeatTimeout() {
        return ViewConfiguration.getKeyRepeatTimeout();
    }

    private int getKeyRepeatDelay() {
        return ViewConfiguration.getKeyRepeatDelay();
    }

    private int getHoverTapTimeout() {
        return ViewConfiguration.getHoverTapTimeout();
    }

    private int getHoverTapSlop() {
        return ViewConfiguration.getHoverTapSlop();
    }

    private int getDoubleTapTimeout() {
        return ViewConfiguration.getDoubleTapTimeout();
    }

    private int getLongPressTimeout() {
        return ViewConfiguration.getLongPressTimeout();
    }

    private int getPointerLayer() {
        return this.mWindowManagerCallbacks.getPointerLayer();
    }

    private PointerIcon getPointerIcon() {
        return PointerIcon.getDefaultIcon(this.mContext);
    }

    private String[] getKeyboardLayoutOverlay(InputDeviceIdentifier inputDeviceIdentifier) {
        String currentKeyboardLayoutForInputDevice;
        if (!this.mSystemReady || (currentKeyboardLayoutForInputDevice = getCurrentKeyboardLayoutForInputDevice(inputDeviceIdentifier)) == null) {
            return null;
        }
        final String[] strArr = new String[2];
        visitKeyboardLayout(currentKeyboardLayoutForInputDevice, new KeyboardLayoutVisitor() {
            @Override
            public void visitKeyboardLayout(Resources resources, int i, KeyboardLayout keyboardLayout) {
                try {
                    strArr[0] = keyboardLayout.getDescriptor();
                    strArr[1] = Streams.readFully(new InputStreamReader(resources.openRawResource(i)));
                } catch (Resources.NotFoundException e) {
                } catch (IOException e2) {
                }
            }
        });
        if (strArr[0] == null) {
            Slog.w(TAG, "Could not get keyboard layout with descriptor '" + currentKeyboardLayoutForInputDevice + "'.");
            return null;
        }
        return strArr;
    }

    private String getDeviceAlias(String str) {
        return BluetoothAdapter.checkBluetoothAddress(str) ? null : null;
    }

    private final class InputManagerHandler extends Handler {
        public InputManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    InputManagerService.this.deliverInputDevicesChanged((InputDevice[]) message.obj);
                    break;
                case 2:
                    InputManagerService.this.handleSwitchKeyboardLayout(message.arg1, message.arg2);
                    break;
                case 3:
                    InputManagerService.this.reloadKeyboardLayouts();
                    break;
                case 4:
                    InputManagerService.this.updateKeyboardLayouts();
                    break;
                case 5:
                    InputManagerService.this.reloadDeviceAliases();
                    break;
                case 6:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    InputManagerService.this.deliverTabletModeChanged((((long) someArgs.argi1) & 4294967295L) | (((long) someArgs.argi2) << 32), ((Boolean) someArgs.arg1).booleanValue());
                    break;
            }
        }
    }

    private final class InputFilterHost extends IInputFilterHost.Stub {
        private boolean mDisconnected;

        private InputFilterHost() {
        }

        public void disconnectLocked() {
            this.mDisconnected = true;
        }

        public void sendInputEvent(InputEvent inputEvent, int i) {
            if (inputEvent == null) {
                throw new IllegalArgumentException("event must not be null");
            }
            synchronized (InputManagerService.this.mInputFilterLock) {
                if (!this.mDisconnected) {
                    InputManagerService.nativeInjectInputEvent(InputManagerService.this.mPtr, inputEvent, 0, 0, 0, 0, 0, i | 67108864);
                }
            }
        }
    }

    private static final class KeyboardLayoutDescriptor {
        public String keyboardLayoutName;
        public String packageName;
        public String receiverName;

        private KeyboardLayoutDescriptor() {
        }

        public static String format(String str, String str2, String str3) {
            return str + SliceClientPermissions.SliceAuthority.DELIMITER + str2 + SliceClientPermissions.SliceAuthority.DELIMITER + str3;
        }

        public static KeyboardLayoutDescriptor parse(String str) {
            int i;
            int iIndexOf;
            int i2;
            int iIndexOf2 = str.indexOf(47);
            if (iIndexOf2 < 0 || (i = iIndexOf2 + 1) == str.length() || (iIndexOf = str.indexOf(47, i)) < iIndexOf2 + 2 || (i2 = iIndexOf + 1) == str.length()) {
                return null;
            }
            KeyboardLayoutDescriptor keyboardLayoutDescriptor = new KeyboardLayoutDescriptor();
            keyboardLayoutDescriptor.packageName = str.substring(0, iIndexOf2);
            keyboardLayoutDescriptor.receiverName = str.substring(i, iIndexOf);
            keyboardLayoutDescriptor.keyboardLayoutName = str.substring(i2);
            return keyboardLayoutDescriptor;
        }
    }

    private final class InputDevicesChangedListenerRecord implements IBinder.DeathRecipient {
        private final IInputDevicesChangedListener mListener;
        private final int mPid;

        public InputDevicesChangedListenerRecord(int i, IInputDevicesChangedListener iInputDevicesChangedListener) {
            this.mPid = i;
            this.mListener = iInputDevicesChangedListener;
        }

        @Override
        public void binderDied() {
            InputManagerService.this.onInputDevicesChangedListenerDied(this.mPid);
        }

        public void notifyInputDevicesChanged(int[] iArr) {
            try {
                this.mListener.onInputDevicesChanged(iArr);
            } catch (RemoteException e) {
                Slog.w(InputManagerService.TAG, "Failed to notify process " + this.mPid + " that input devices changed, assuming it died.", e);
                binderDied();
            }
        }
    }

    private final class TabletModeChangedListenerRecord implements IBinder.DeathRecipient {
        private final ITabletModeChangedListener mListener;
        private final int mPid;

        public TabletModeChangedListenerRecord(int i, ITabletModeChangedListener iTabletModeChangedListener) {
            this.mPid = i;
            this.mListener = iTabletModeChangedListener;
        }

        @Override
        public void binderDied() {
            InputManagerService.this.onTabletModeChangedListenerDied(this.mPid);
        }

        public void notifyTabletModeChanged(long j, boolean z) {
            try {
                this.mListener.onTabletModeChanged(j, z);
            } catch (RemoteException e) {
                Slog.w(InputManagerService.TAG, "Failed to notify process " + this.mPid + " that tablet mode changed, assuming it died.", e);
                binderDied();
            }
        }
    }

    private final class VibratorToken implements IBinder.DeathRecipient {
        public final int mDeviceId;
        public final IBinder mToken;
        public final int mTokenValue;
        public boolean mVibrating;

        public VibratorToken(int i, IBinder iBinder, int i2) {
            this.mDeviceId = i;
            this.mToken = iBinder;
            this.mTokenValue = i2;
        }

        @Override
        public void binderDied() {
            InputManagerService.this.onVibratorTokenDied(this);
        }
    }

    private final class LocalService extends InputManagerInternal {
        private LocalService() {
        }

        public void setDisplayViewports(DisplayViewport displayViewport, DisplayViewport displayViewport2, List<DisplayViewport> list) {
            InputManagerService.this.setDisplayViewportsInternal(displayViewport, displayViewport2, list);
        }

        public boolean injectInputEvent(InputEvent inputEvent, int i, int i2) {
            return InputManagerService.this.injectInputEventInternal(inputEvent, i, i2);
        }

        public void setInteractive(boolean z) {
            InputManagerService.nativeSetInteractive(InputManagerService.this.mPtr, z);
        }

        public void toggleCapsLock(int i) {
            InputManagerService.nativeToggleCapsLock(InputManagerService.this.mPtr, i);
        }

        public void setPulseGestureEnabled(boolean z) throws Throwable {
            FileWriter fileWriter;
            if (InputManagerService.this.mDoubleTouchGestureEnableFile != null) {
                FileWriter fileWriter2 = null;
                try {
                    try {
                        fileWriter = new FileWriter(InputManagerService.this.mDoubleTouchGestureEnableFile);
                    } catch (IOException e) {
                        e = e;
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    fileWriter.write(z ? "1" : "0");
                    IoUtils.closeQuietly(fileWriter);
                } catch (IOException e2) {
                    e = e2;
                    fileWriter2 = fileWriter;
                    Log.wtf(InputManagerService.TAG, "Unable to setPulseGestureEnabled", e);
                    IoUtils.closeQuietly(fileWriter2);
                } catch (Throwable th2) {
                    th = th2;
                    fileWriter2 = fileWriter;
                    IoUtils.closeQuietly(fileWriter2);
                    throw th;
                }
            }
        }
    }
}
