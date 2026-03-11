package com.android.systemui.keyboard;

import android.R;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.Utils;
import com.android.systemui.SystemUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class KeyboardUI extends SystemUI implements InputManager.OnTabletModeChangedListener {
    private boolean mBootCompleted;
    private long mBootCompletedTime;
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    protected volatile Context mContext;
    private BluetoothDialog mDialog;
    private boolean mEnabled;
    private volatile KeyboardHandler mHandler;
    private String mKeyboardName;
    private LocalBluetoothAdapter mLocalBluetoothAdapter;
    private LocalBluetoothProfileManager mProfileManager;
    private ScanCallback mScanCallback;
    private int mState;
    private volatile KeyboardUIHandler mUIHandler;
    private int mInTabletMode = -1;
    private int mScanAttempt = 0;

    @Override
    public void start() {
        this.mContext = super.mContext;
        HandlerThread thread = new HandlerThread("Keyboard", 10);
        thread.start();
        this.mHandler = new KeyboardHandler(thread.getLooper());
        this.mHandler.sendEmptyMessage(0);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("KeyboardUI:");
        pw.println("  mEnabled=" + this.mEnabled);
        pw.println("  mBootCompleted=" + this.mEnabled);
        pw.println("  mBootCompletedTime=" + this.mBootCompletedTime);
        pw.println("  mKeyboardName=" + this.mKeyboardName);
        pw.println("  mInTabletMode=" + this.mInTabletMode);
        pw.println("  mState=" + stateToString(this.mState));
    }

    @Override
    protected void onBootCompleted() {
        this.mHandler.sendEmptyMessage(1);
    }

    public void onTabletModeChanged(long whenNanos, boolean inTabletMode) {
        if ((!inTabletMode || this.mInTabletMode == 1) && (inTabletMode || this.mInTabletMode == 0)) {
            return;
        }
        this.mInTabletMode = inTabletMode ? 1 : 0;
        processKeyboardState();
    }

    public void init() {
        LocalBluetoothManager localBluetoothManager;
        BluetoothCallbackHandler bluetoothCallbackHandler = null;
        Object[] objArr = 0;
        Context context = this.mContext;
        this.mKeyboardName = context.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_IN_PROGRESS);
        if (TextUtils.isEmpty(this.mKeyboardName) || (localBluetoothManager = LocalBluetoothManager.getInstance(context, null)) == null) {
            return;
        }
        this.mEnabled = true;
        this.mCachedDeviceManager = localBluetoothManager.getCachedDeviceManager();
        this.mLocalBluetoothAdapter = localBluetoothManager.getBluetoothAdapter();
        this.mProfileManager = localBluetoothManager.getProfileManager();
        localBluetoothManager.getEventManager().registerCallback(new BluetoothCallbackHandler(this, bluetoothCallbackHandler));
        Utils.setErrorListener(new BluetoothErrorListener(this, objArr == true ? 1 : 0));
        InputManager inputManager = (InputManager) context.getSystemService(InputManager.class);
        inputManager.registerOnTabletModeChangedListener(this, this.mHandler);
        this.mInTabletMode = inputManager.isInTabletMode();
        processKeyboardState();
        this.mUIHandler = new KeyboardUIHandler();
    }

    public void processKeyboardState() {
        this.mHandler.removeMessages(2);
        if (!this.mEnabled) {
            this.mState = -1;
            return;
        }
        if (!this.mBootCompleted) {
            this.mState = 1;
            return;
        }
        if (this.mInTabletMode != 0) {
            if (this.mState == 3) {
                stopScanning();
            } else if (this.mState == 4) {
                this.mUIHandler.sendEmptyMessage(9);
            }
            this.mState = 2;
            return;
        }
        int btState = this.mLocalBluetoothAdapter.getState();
        if ((btState == 11 || btState == 12) && this.mState == 4) {
            this.mUIHandler.sendEmptyMessage(9);
        }
        if (btState == 11) {
            this.mState = 4;
            return;
        }
        if (btState != 12) {
            this.mState = 4;
            showBluetoothDialog();
            return;
        }
        CachedBluetoothDevice device = getPairedKeyboard();
        if (this.mState == 2 || this.mState == 4) {
            if (device != null) {
                this.mState = 6;
                device.connect(false);
                return;
            }
            this.mCachedDeviceManager.clearNonBondedDevices();
        }
        CachedBluetoothDevice device2 = getDiscoveredKeyboard();
        if (device2 != null) {
            this.mState = 5;
            device2.startPairing();
        } else {
            this.mState = 3;
            startScanning();
        }
    }

    public void onBootCompletedInternal() {
        this.mBootCompleted = true;
        this.mBootCompletedTime = SystemClock.uptimeMillis();
        if (this.mState != 1) {
            return;
        }
        processKeyboardState();
    }

    private void showBluetoothDialog() {
        if (isUserSetupComplete()) {
            long now = SystemClock.uptimeMillis();
            long earliestDialogTime = this.mBootCompletedTime + 10000;
            if (earliestDialogTime < now) {
                this.mUIHandler.sendEmptyMessage(8);
                return;
            } else {
                this.mHandler.sendEmptyMessageAtTime(2, earliestDialogTime);
                return;
            }
        }
        this.mLocalBluetoothAdapter.enable();
    }

    private boolean isUserSetupComplete() {
        ContentResolver resolver = this.mContext.getContentResolver();
        return Settings.Secure.getIntForUser(resolver, "user_setup_complete", 0, -2) != 0;
    }

    private CachedBluetoothDevice getPairedKeyboard() {
        Set<BluetoothDevice> devices = this.mLocalBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice d : devices) {
            if (this.mKeyboardName.equals(d.getName())) {
                return getCachedBluetoothDevice(d);
            }
        }
        return null;
    }

    private CachedBluetoothDevice getDiscoveredKeyboard() {
        Collection<CachedBluetoothDevice> devices = this.mCachedDeviceManager.getCachedDevicesCopy();
        for (CachedBluetoothDevice d : devices) {
            if (d.getName().equals(this.mKeyboardName)) {
                return d;
            }
        }
        return null;
    }

    public CachedBluetoothDevice getCachedBluetoothDevice(BluetoothDevice d) {
        CachedBluetoothDevice cachedDevice = this.mCachedDeviceManager.findDevice(d);
        if (cachedDevice == null) {
            return this.mCachedDeviceManager.addDevice(this.mLocalBluetoothAdapter, this.mProfileManager, d);
        }
        return cachedDevice;
    }

    private void startScanning() {
        BluetoothLeScanner scanner = this.mLocalBluetoothAdapter.getBluetoothLeScanner();
        ScanFilter filter = new ScanFilter.Builder().setDeviceName(this.mKeyboardName).build();
        ScanSettings settings = new ScanSettings.Builder().setCallbackType(1).setNumOfMatches(1).setScanMode(2).setReportDelay(0L).build();
        this.mScanCallback = new KeyboardScanCallback(this, null);
        scanner.startScan(Arrays.asList(filter), settings, this.mScanCallback);
        KeyboardHandler keyboardHandler = this.mHandler;
        int i = this.mScanAttempt + 1;
        this.mScanAttempt = i;
        Message abortMsg = keyboardHandler.obtainMessage(10, i, 0);
        this.mHandler.sendMessageDelayed(abortMsg, 30000L);
    }

    private void stopScanning() {
        if (this.mScanCallback == null) {
            return;
        }
        BluetoothLeScanner scanner = this.mLocalBluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) {
            scanner.stopScan(this.mScanCallback);
        }
        this.mScanCallback = null;
    }

    public void bleAbortScanInternal(int scanAttempt) {
        if (this.mState != 3 || scanAttempt != this.mScanAttempt) {
            return;
        }
        stopScanning();
        this.mState = 9;
    }

    public void onDeviceAddedInternal(CachedBluetoothDevice d) {
        if (this.mState != 3 || !d.getName().equals(this.mKeyboardName)) {
            return;
        }
        stopScanning();
        d.startPairing();
        this.mState = 5;
    }

    public void onBluetoothStateChangedInternal(int bluetoothState) {
        if (bluetoothState != 12 || this.mState != 4) {
            return;
        }
        processKeyboardState();
    }

    public void onDeviceBondStateChangedInternal(CachedBluetoothDevice d, int bondState) {
        if (this.mState != 5 || !d.getName().equals(this.mKeyboardName)) {
            return;
        }
        if (bondState == 12) {
            this.mState = 6;
        } else {
            if (bondState != 10) {
                return;
            }
            this.mState = 7;
        }
    }

    public void onBleScanFailedInternal() {
        this.mScanCallback = null;
        if (this.mState != 3) {
            return;
        }
        this.mState = 9;
    }

    public void onShowErrorInternal(Context context, String name, int messageResId) {
        if ((this.mState != 5 && this.mState != 7) || !this.mKeyboardName.equals(name)) {
            return;
        }
        String message = context.getString(messageResId, name);
        Toast.makeText(context, message, 0).show();
    }

    private final class KeyboardUIHandler extends Handler {
        public KeyboardUIHandler() {
            super(Looper.getMainLooper(), null, true);
        }

        @Override
        public void handleMessage(Message message) {
            BluetoothDialogClickListener bluetoothDialogClickListener = null;
            Object[] objArr = 0;
            switch (message.what) {
                case 8:
                    if (KeyboardUI.this.mDialog == null) {
                        BluetoothDialogClickListener bluetoothDialogClickListener2 = new BluetoothDialogClickListener(KeyboardUI.this, bluetoothDialogClickListener);
                        BluetoothDialogDismissListener bluetoothDialogDismissListener = new BluetoothDialogDismissListener(KeyboardUI.this, objArr == true ? 1 : 0);
                        KeyboardUI.this.mDialog = new BluetoothDialog(KeyboardUI.this.mContext);
                        KeyboardUI.this.mDialog.setTitle(com.android.systemui.R.string.enable_bluetooth_title);
                        KeyboardUI.this.mDialog.setMessage(com.android.systemui.R.string.enable_bluetooth_message);
                        KeyboardUI.this.mDialog.setPositiveButton(com.android.systemui.R.string.enable_bluetooth_confirmation_ok, bluetoothDialogClickListener2);
                        KeyboardUI.this.mDialog.setNegativeButton(R.string.cancel, bluetoothDialogClickListener2);
                        KeyboardUI.this.mDialog.setOnDismissListener(bluetoothDialogDismissListener);
                        KeyboardUI.this.mDialog.show();
                    }
                    break;
                case 9:
                    if (KeyboardUI.this.mDialog != null) {
                        KeyboardUI.this.mDialog.dismiss();
                    }
                    break;
            }
        }
    }

    private final class KeyboardHandler extends Handler {
        public KeyboardHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    KeyboardUI.this.init();
                    break;
                case 1:
                    KeyboardUI.this.onBootCompletedInternal();
                    break;
                case 2:
                    KeyboardUI.this.processKeyboardState();
                    break;
                case 3:
                    boolean enable = msg.arg1 == 1;
                    if (enable) {
                        KeyboardUI.this.mLocalBluetoothAdapter.enable();
                    } else {
                        KeyboardUI.this.mState = 8;
                    }
                    break;
                case 4:
                    int bluetoothState = msg.arg1;
                    KeyboardUI.this.onBluetoothStateChangedInternal(bluetoothState);
                    break;
                case 5:
                    CachedBluetoothDevice d = (CachedBluetoothDevice) msg.obj;
                    int bondState = msg.arg1;
                    KeyboardUI.this.onDeviceBondStateChangedInternal(d, bondState);
                    break;
                case 6:
                    BluetoothDevice d2 = (BluetoothDevice) msg.obj;
                    CachedBluetoothDevice cachedDevice = KeyboardUI.this.getCachedBluetoothDevice(d2);
                    KeyboardUI.this.onDeviceAddedInternal(cachedDevice);
                    break;
                case 7:
                    KeyboardUI.this.onBleScanFailedInternal();
                    break;
                case 10:
                    int scanAttempt = msg.arg1;
                    KeyboardUI.this.bleAbortScanInternal(scanAttempt);
                    break;
                case 11:
                    Pair<Context, String> p = (Pair) msg.obj;
                    KeyboardUI.this.onShowErrorInternal((Context) p.first, (String) p.second, msg.arg1);
                    break;
            }
        }
    }

    private final class BluetoothDialogClickListener implements DialogInterface.OnClickListener {
        BluetoothDialogClickListener(KeyboardUI this$0, BluetoothDialogClickListener bluetoothDialogClickListener) {
            this();
        }

        private BluetoothDialogClickListener() {
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            int enable = -1 == which ? 1 : 0;
            KeyboardUI.this.mHandler.obtainMessage(3, enable, 0).sendToTarget();
            KeyboardUI.this.mDialog = null;
        }
    }

    private final class BluetoothDialogDismissListener implements DialogInterface.OnDismissListener {
        BluetoothDialogDismissListener(KeyboardUI this$0, BluetoothDialogDismissListener bluetoothDialogDismissListener) {
            this();
        }

        private BluetoothDialogDismissListener() {
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            KeyboardUI.this.mDialog = null;
        }
    }

    private final class KeyboardScanCallback extends ScanCallback {
        KeyboardScanCallback(KeyboardUI this$0, KeyboardScanCallback keyboardScanCallback) {
            this();
        }

        private KeyboardScanCallback() {
        }

        private boolean isDeviceDiscoverable(ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            int flags = scanRecord.getAdvertiseFlags();
            return (flags & 3) != 0;
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            BluetoothDevice bestDevice = null;
            int bestRssi = Integer.MIN_VALUE;
            for (ScanResult result : results) {
                if (isDeviceDiscoverable(result) && result.getRssi() > bestRssi) {
                    bestDevice = result.getDevice();
                    bestRssi = result.getRssi();
                }
            }
            if (bestDevice == null) {
                return;
            }
            KeyboardUI.this.mHandler.obtainMessage(6, bestDevice).sendToTarget();
        }

        @Override
        public void onScanFailed(int errorCode) {
            KeyboardUI.this.mHandler.obtainMessage(7).sendToTarget();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!isDeviceDiscoverable(result)) {
                return;
            }
            KeyboardUI.this.mHandler.obtainMessage(6, result.getDevice()).sendToTarget();
        }
    }

    private final class BluetoothCallbackHandler implements BluetoothCallback {
        BluetoothCallbackHandler(KeyboardUI this$0, BluetoothCallbackHandler bluetoothCallbackHandler) {
            this();
        }

        private BluetoothCallbackHandler() {
        }

        @Override
        public void onBluetoothStateChanged(int bluetoothState) {
            KeyboardUI.this.mHandler.obtainMessage(4, bluetoothState, 0).sendToTarget();
        }

        @Override
        public void onDeviceBondStateChanged(CachedBluetoothDevice cachedDevice, int bondState) {
            KeyboardUI.this.mHandler.obtainMessage(5, bondState, 0, cachedDevice).sendToTarget();
        }

        @Override
        public void onDeviceAdded(CachedBluetoothDevice cachedDevice) {
        }

        @Override
        public void onDeviceDeleted(CachedBluetoothDevice cachedDevice) {
        }

        @Override
        public void onScanningStateChanged(boolean started) {
        }

        @Override
        public void onConnectionStateChanged(CachedBluetoothDevice cachedDevice, int state) {
        }
    }

    private final class BluetoothErrorListener implements Utils.ErrorListener {
        BluetoothErrorListener(KeyboardUI this$0, BluetoothErrorListener bluetoothErrorListener) {
            this();
        }

        private BluetoothErrorListener() {
        }

        @Override
        public void onShowError(Context context, String name, int messageResId) {
            KeyboardUI.this.mHandler.obtainMessage(11, messageResId, 0, new Pair(context, name)).sendToTarget();
        }
    }

    private static String stateToString(int state) {
        switch (state) {
            case -1:
                return "STATE_NOT_ENABLED";
            case 0:
            default:
                return "STATE_UNKNOWN (" + state + ")";
            case 1:
                return "STATE_WAITING_FOR_BOOT_COMPLETED";
            case 2:
                return "STATE_WAITING_FOR_TABLET_MODE_EXIT";
            case 3:
                return "STATE_WAITING_FOR_DEVICE_DISCOVERY";
            case 4:
                return "STATE_WAITING_FOR_BLUETOOTH";
            case 5:
                return "STATE_PAIRING";
            case 6:
                return "STATE_PAIRED";
            case 7:
                return "STATE_PAIRING_FAILED";
            case 8:
                return "STATE_USER_CANCELLED";
            case 9:
                return "STATE_DEVICE_NOT_FOUND";
        }
    }
}
