package com.android.settings.bluetooth;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothProfileManager;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class DockService extends Service implements LocalBluetoothProfileManager.ServiceListener {
    private CheckBox mAudioMediaCheckbox;
    private boolean[] mCheckedItems;
    private BluetoothDevice mDevice;
    private CachedBluetoothDeviceManager mDeviceManager;
    private AlertDialog mDialog;
    private LocalBluetoothAdapter mLocalAdapter;
    private BluetoothDevice mPendingDevice;
    private int mPendingStartId;
    private LocalBluetoothProfileManager mProfileManager;
    private LocalBluetoothProfile[] mProfiles;
    private Runnable mRunnable;
    private volatile ServiceHandler mServiceHandler;
    private volatile Looper mServiceLooper;
    private int mStartIdAssociatedWithDialog;
    private int mPendingTurnOnStartId = -100;
    private int mPendingTurnOffStartId = -100;
    private final DialogInterface.OnMultiChoiceClickListener mMultiClickListener = new DialogInterface.OnMultiChoiceClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            DockService.this.mCheckedItems[which] = isChecked;
        }
    };
    private final CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (DockService.this.mDevice != null) {
                LocalBluetoothPreferences.saveDockAutoConnectSetting(DockService.this, DockService.this.mDevice.getAddress(), isChecked);
            } else {
                Settings.Global.putInt(DockService.this.getContentResolver(), "dock_audio_media_enabled", isChecked ? 1 : 0);
            }
        }
    };
    private final DialogInterface.OnDismissListener mDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            if (DockService.this.mPendingDevice == null) {
                DockEventReceiver.finishStartingService(DockService.this, DockService.this.mStartIdAssociatedWithDialog);
            }
            DockService.this.stopForeground(true);
        }
    };
    private final DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                if (DockService.this.mDevice != null) {
                    if (!LocalBluetoothPreferences.hasDockAutoConnectSetting(DockService.this, DockService.this.mDevice.getAddress())) {
                        LocalBluetoothPreferences.saveDockAutoConnectSetting(DockService.this, DockService.this.mDevice.getAddress(), true);
                    }
                    DockService.this.applyBtSettings(DockService.this.mDevice, DockService.this.mStartIdAssociatedWithDialog);
                } else if (DockService.this.mAudioMediaCheckbox != null) {
                    Settings.Global.putInt(DockService.this.getContentResolver(), "dock_audio_media_enabled", DockService.this.mAudioMediaCheckbox.isChecked() ? 1 : 0);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        LocalBluetoothManager manager = LocalBluetoothManager.getInstance(this);
        if (manager == null) {
            Log.e("DockService", "Can't get LocalBluetoothManager: exiting");
            return;
        }
        this.mLocalAdapter = manager.getBluetoothAdapter();
        this.mDeviceManager = manager.getCachedDeviceManager();
        this.mProfileManager = manager.getProfileManager();
        if (this.mProfileManager == null) {
            Log.e("DockService", "Can't get LocalBluetoothProfileManager: exiting");
            return;
        }
        HandlerThread thread = new HandlerThread("DockService");
        thread.start();
        this.mServiceLooper = thread.getLooper();
        this.mServiceHandler = new ServiceHandler(this.mServiceLooper);
    }

    @Override
    public void onDestroy() {
        this.mRunnable = null;
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (this.mProfileManager != null) {
            this.mProfileManager.removeServiceListener(this);
        }
        if (this.mServiceLooper != null) {
            this.mServiceLooper.quit();
        }
        this.mLocalAdapter = null;
        this.mDeviceManager = null;
        this.mProfileManager = null;
        this.mServiceLooper = null;
        this.mServiceHandler = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("dock_settings", 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            DockEventReceiver.finishStartingService(this, startId);
        } else if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction())) {
            handleBtStateChange(intent, startId);
        } else {
            SharedPreferences prefs = getPrefs();
            if ("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction())) {
                BluetoothDevice disconnectedDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                int retryCount = prefs.getInt("connect_retry_count", 0);
                if (retryCount < 6) {
                    prefs.edit().putInt("connect_retry_count", retryCount + 1).apply();
                    handleUnexpectedDisconnect(disconnectedDevice, this.mProfileManager.getHeadsetProfile(), startId);
                }
            } else if ("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED".equals(intent.getAction())) {
                BluetoothDevice disconnectedDevice2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                int retryCount2 = prefs.getInt("connect_retry_count", 0);
                if (retryCount2 < 6) {
                    prefs.edit().putInt("connect_retry_count", retryCount2 + 1).apply();
                    handleUnexpectedDisconnect(disconnectedDevice2, this.mProfileManager.getA2dpProfile(), startId);
                }
            } else {
                Message msg = parseIntent(intent);
                if (msg == null) {
                    DockEventReceiver.finishStartingService(this, startId);
                } else {
                    if (msg.what == 222) {
                        prefs.edit().remove("connect_retry_count").apply();
                    }
                    msg.arg2 = startId;
                    processMessage(msg);
                }
            }
        }
        return 2;
    }

    private final class ServiceHandler extends Handler {
        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            DockService.this.processMessage(msg);
        }
    }

    public synchronized void processMessage(Message msg) {
        int msgType = msg.what;
        int state = msg.arg1;
        int startId = msg.arg2;
        BluetoothDevice device = null;
        if (msg.obj != null) {
            device = (BluetoothDevice) msg.obj;
        }
        boolean deferFinishCall = false;
        switch (msgType) {
            case 111:
                if (device != null) {
                    createDialog(device, state, startId);
                }
                break;
            case 222:
                deferFinishCall = msgTypeDocked(device, state, startId);
                break;
            case 333:
                msgTypeUndockedTemporary(device, state, startId);
                break;
            case 444:
                deferFinishCall = msgTypeUndockedPermanent(device, startId);
                break;
            case 555:
                deferFinishCall = msgTypeDisableBluetooth(startId);
                break;
        }
        if (this.mDialog == null && this.mPendingDevice == null && msgType != 333 && !deferFinishCall) {
            DockEventReceiver.finishStartingService(this, startId);
        }
    }

    private boolean msgTypeDisableBluetooth(int startId) {
        SharedPreferences prefs = getPrefs();
        if (this.mLocalAdapter.disable()) {
            prefs.edit().remove("disable_bt_when_undock").apply();
            return false;
        }
        prefs.edit().putBoolean("disable_bt", true).apply();
        this.mPendingTurnOffStartId = startId;
        return true;
    }

    private void msgTypeUndockedTemporary(BluetoothDevice device, int state, int startId) {
        Message newMsg = this.mServiceHandler.obtainMessage(444, state, startId, device);
        this.mServiceHandler.sendMessageDelayed(newMsg, 1000L);
    }

    private boolean msgTypeUndockedPermanent(BluetoothDevice device, int startId) {
        handleUndocked(device);
        if (device == null) {
            return false;
        }
        SharedPreferences prefs = getPrefs();
        if (!prefs.getBoolean("disable_bt_when_undock", false)) {
            return false;
        }
        if (hasOtherConnectedDevices(device)) {
            prefs.edit().remove("disable_bt_when_undock").apply();
            return false;
        }
        Message newMsg = this.mServiceHandler.obtainMessage(555, 0, startId, null);
        this.mServiceHandler.sendMessageDelayed(newMsg, 2000L);
        return true;
    }

    private boolean msgTypeDocked(final BluetoothDevice device, final int state, final int startId) {
        this.mServiceHandler.removeMessages(444);
        this.mServiceHandler.removeMessages(555);
        getPrefs().edit().remove("disable_bt").apply();
        if (device != null) {
            if (!device.equals(this.mDevice)) {
                if (this.mDevice != null) {
                    handleUndocked(this.mDevice);
                }
                this.mDevice = device;
                this.mProfileManager.addServiceListener(this);
                if (this.mProfileManager.isManagerReady()) {
                    handleDocked(device, state, startId);
                    this.mProfileManager.removeServiceListener(this);
                } else {
                    this.mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            DockService.this.handleDocked(device, state, startId);
                        }
                    };
                    return true;
                }
            }
        } else {
            int dockAudioMediaEnabled = Settings.Global.getInt(getContentResolver(), "dock_audio_media_enabled", -1);
            if (dockAudioMediaEnabled == -1 && state == 3) {
                handleDocked(null, state, startId);
                return true;
            }
        }
        return false;
    }

    synchronized boolean hasOtherConnectedDevices(BluetoothDevice dock) {
        boolean z = false;
        synchronized (this) {
            Collection<CachedBluetoothDevice> cachedDevices = this.mDeviceManager.getCachedDevicesCopy();
            Set<BluetoothDevice> btDevices = this.mLocalAdapter.getBondedDevices();
            if (btDevices != null && cachedDevices != null && !btDevices.isEmpty()) {
                Iterator<CachedBluetoothDevice> it = cachedDevices.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    CachedBluetoothDevice deviceUI = it.next();
                    BluetoothDevice btDevice = deviceUI.getDevice();
                    if (!btDevice.equals(dock) && btDevices.contains(btDevice) && deviceUI.isConnected()) {
                        z = true;
                        break;
                    }
                }
            }
        }
        return z;
    }

    private Message parseIntent(Intent intent) {
        int msgType;
        BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        int state = intent.getIntExtra("android.intent.extra.DOCK_STATE", -1234);
        switch (state) {
            case 0:
                msgType = 333;
                return this.mServiceHandler.obtainMessage(msgType, state, 0, device);
            case 1:
            case 2:
            case 4:
                if (device == null) {
                    Log.w("DockService", "device is null");
                    return null;
                }
            case 3:
                if ("com.android.settings.bluetooth.action.DOCK_SHOW_UI".equals(intent.getAction())) {
                    if (device == null) {
                        Log.w("DockService", "device is null");
                        return null;
                    }
                    msgType = 111;
                } else {
                    msgType = 222;
                }
                return this.mServiceHandler.obtainMessage(msgType, state, 0, device);
            default:
                return null;
        }
    }

    private void createDialog(BluetoothDevice device, int state, int startId) {
        View view;
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        this.mDevice = device;
        switch (state) {
            case 1:
            case 2:
            case 3:
            case 4:
                startForeground(0, new Notification());
                AlertDialog.Builder ab = new AlertDialog.Builder(this);
                LayoutInflater inflater = (LayoutInflater) getSystemService("layout_inflater");
                this.mAudioMediaCheckbox = null;
                if (device != null) {
                    boolean firstTime = !LocalBluetoothPreferences.hasDockAutoConnectSetting(this, device.getAddress());
                    CharSequence[] items = initBtSettings(device, state, firstTime);
                    ab.setTitle(getString(R.string.bluetooth_dock_settings_title));
                    ab.setMultiChoiceItems(items, this.mCheckedItems, this.mMultiClickListener);
                    view = inflater.inflate(R.layout.remember_dock_setting, (ViewGroup) null);
                    CheckBox rememberCheckbox = (CheckBox) view.findViewById(R.id.remember);
                    boolean checked = firstTime || LocalBluetoothPreferences.getDockAutoConnectSetting(this, device.getAddress());
                    rememberCheckbox.setChecked(checked);
                    rememberCheckbox.setOnCheckedChangeListener(this.mCheckedChangeListener);
                } else {
                    ab.setTitle(getString(R.string.bluetooth_dock_settings_title));
                    view = inflater.inflate(R.layout.dock_audio_media_enable_dialog, (ViewGroup) null);
                    this.mAudioMediaCheckbox = (CheckBox) view.findViewById(R.id.dock_audio_media_enable_cb);
                    boolean checked2 = Settings.Global.getInt(getContentResolver(), "dock_audio_media_enabled", 0) == 1;
                    this.mAudioMediaCheckbox.setChecked(checked2);
                    this.mAudioMediaCheckbox.setOnCheckedChangeListener(this.mCheckedChangeListener);
                }
                float pixelScaleFactor = getResources().getDisplayMetrics().density;
                int viewSpacingLeft = (int) (14.0f * pixelScaleFactor);
                int viewSpacingRight = (int) (14.0f * pixelScaleFactor);
                ab.setView(view, viewSpacingLeft, 0, viewSpacingRight, 0);
                ab.setPositiveButton(getString(android.R.string.ok), this.mClickListener);
                this.mStartIdAssociatedWithDialog = startId;
                this.mDialog = ab.create();
                this.mDialog.getWindow().setType(2009);
                this.mDialog.setOnDismissListener(this.mDismissListener);
                this.mDialog.show();
                break;
        }
    }

    private CharSequence[] initBtSettings(BluetoothDevice device, int state, boolean firstTime) {
        int numOfProfiles;
        switch (state) {
            case 1:
            case 3:
            case 4:
                numOfProfiles = 1;
                break;
            case 2:
                numOfProfiles = 2;
                break;
            default:
                return null;
        }
        this.mProfiles = new LocalBluetoothProfile[numOfProfiles];
        this.mCheckedItems = new boolean[numOfProfiles];
        CharSequence[] items = new CharSequence[numOfProfiles];
        switch (state) {
            case 1:
            case 3:
            case 4:
                items[0] = getString(R.string.bluetooth_dock_settings_a2dp);
                this.mProfiles[0] = this.mProfileManager.getA2dpProfile();
                if (firstTime) {
                    this.mCheckedItems[0] = false;
                } else {
                    this.mCheckedItems[0] = this.mProfiles[0].isPreferred(device);
                }
                break;
            case 2:
                items[0] = getString(R.string.bluetooth_dock_settings_headset);
                items[1] = getString(R.string.bluetooth_dock_settings_a2dp);
                this.mProfiles[0] = this.mProfileManager.getHeadsetProfile();
                this.mProfiles[1] = this.mProfileManager.getA2dpProfile();
                if (firstTime) {
                    this.mCheckedItems[0] = true;
                    this.mCheckedItems[1] = true;
                } else {
                    this.mCheckedItems[0] = this.mProfiles[0].isPreferred(device);
                    this.mCheckedItems[1] = this.mProfiles[1].isPreferred(device);
                }
                break;
        }
        return items;
    }

    private void handleBtStateChange(Intent intent, int startId) {
        int btState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
        synchronized (this) {
            if (btState == 12) {
                handleBluetoothStateOn(startId);
            } else if (btState == 13) {
                getPrefs().edit().remove("disable_bt_when_undock").apply();
                DockEventReceiver.finishStartingService(this, startId);
            } else if (btState == 10) {
                if (this.mPendingTurnOffStartId != -100) {
                    DockEventReceiver.finishStartingService(this, this.mPendingTurnOffStartId);
                    getPrefs().edit().remove("disable_bt").apply();
                    this.mPendingTurnOffStartId = -100;
                }
                if (this.mPendingDevice != null) {
                    this.mLocalAdapter.enable();
                    this.mPendingTurnOnStartId = startId;
                } else {
                    DockEventReceiver.finishStartingService(this, startId);
                }
            }
        }
    }

    private void handleBluetoothStateOn(int startId) {
        if (this.mPendingDevice != null) {
            if (this.mPendingDevice.equals(this.mDevice)) {
                applyBtSettings(this.mPendingDevice, this.mPendingStartId);
            }
            this.mPendingDevice = null;
            DockEventReceiver.finishStartingService(this, this.mPendingStartId);
        } else {
            SharedPreferences prefs = getPrefs();
            Intent i = registerReceiver(null, new IntentFilter("android.intent.action.DOCK_EVENT"));
            if (i != null) {
                int state = i.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                if (state != 0) {
                    BluetoothDevice device = (BluetoothDevice) i.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    if (device != null) {
                        connectIfEnabled(device);
                    }
                } else if (prefs.getBoolean("disable_bt", false) && this.mLocalAdapter.disable()) {
                    this.mPendingTurnOffStartId = startId;
                    prefs.edit().remove("disable_bt").apply();
                    return;
                }
            }
        }
        if (this.mPendingTurnOnStartId != -100) {
            DockEventReceiver.finishStartingService(this, this.mPendingTurnOnStartId);
            this.mPendingTurnOnStartId = -100;
        }
        DockEventReceiver.finishStartingService(this, startId);
    }

    private synchronized void handleUnexpectedDisconnect(BluetoothDevice disconnectedDevice, LocalBluetoothProfile profile, int startId) {
        BluetoothDevice dockedDevice;
        if (disconnectedDevice != null) {
            Intent intent = registerReceiver(null, new IntentFilter("android.intent.action.DOCK_EVENT"));
            if (intent != null) {
                int state = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                if (state != 0 && (dockedDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE")) != null && dockedDevice.equals(disconnectedDevice)) {
                    CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(dockedDevice);
                    cachedDevice.connectProfile(profile);
                }
            }
            DockEventReceiver.finishStartingService(this, startId);
        } else {
            DockEventReceiver.finishStartingService(this, startId);
        }
    }

    private synchronized void connectIfEnabled(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(device);
        List<LocalBluetoothProfile> profiles = cachedDevice.getConnectableProfiles();
        Iterator<LocalBluetoothProfile> it = profiles.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            LocalBluetoothProfile profile = it.next();
            if (profile.getPreferred(device) == 1000) {
                break;
            }
        }
    }

    public synchronized void applyBtSettings(BluetoothDevice device, int startId) {
        if (device != null) {
            if (this.mProfiles != null && this.mCheckedItems != null && this.mLocalAdapter != null) {
                boolean[] arr$ = this.mCheckedItems;
                int len$ = arr$.length;
                int i$ = 0;
                while (true) {
                    if (i$ < len$) {
                        boolean enable = arr$[i$];
                        if (enable) {
                            int btState = this.mLocalAdapter.getBluetoothState();
                            this.mLocalAdapter.enable();
                            if (btState != 12) {
                                if (this.mPendingDevice == null || !this.mPendingDevice.equals(this.mDevice)) {
                                    this.mPendingDevice = device;
                                    this.mPendingStartId = startId;
                                    if (btState != 11) {
                                        getPrefs().edit().putBoolean("disable_bt_when_undock", true).apply();
                                    }
                                }
                            }
                        }
                        i$++;
                    } else {
                        this.mPendingDevice = null;
                        boolean callConnect = false;
                        CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(device);
                        for (int i = 0; i < this.mProfiles.length; i++) {
                            LocalBluetoothProfile profile = this.mProfiles[i];
                            if (this.mCheckedItems[i]) {
                                callConnect = true;
                            } else if (!this.mCheckedItems[i]) {
                                int status = profile.getConnectionStatus(cachedDevice.getDevice());
                                if (status == 2) {
                                    cachedDevice.disconnect(this.mProfiles[i]);
                                }
                            }
                            profile.setPreferred(device, this.mCheckedItems[i]);
                        }
                        if (callConnect) {
                            cachedDevice.connect(false);
                        }
                    }
                }
            }
        }
    }

    public synchronized void handleDocked(BluetoothDevice device, int state, int startId) {
        if (device != null) {
            if (LocalBluetoothPreferences.getDockAutoConnectSetting(this, device.getAddress())) {
                initBtSettings(device, state, false);
                applyBtSettings(this.mDevice, startId);
            } else {
                createDialog(device, state, startId);
            }
        }
    }

    private synchronized void handleUndocked(BluetoothDevice device) {
        this.mRunnable = null;
        this.mProfileManager.removeServiceListener(this);
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        this.mDevice = null;
        this.mPendingDevice = null;
        if (device != null) {
            CachedBluetoothDevice cachedDevice = getCachedBluetoothDevice(device);
            cachedDevice.disconnect();
        }
    }

    private CachedBluetoothDevice getCachedBluetoothDevice(BluetoothDevice device) {
        CachedBluetoothDevice cachedDevice = this.mDeviceManager.findDevice(device);
        if (cachedDevice == null) {
            return this.mDeviceManager.addDevice(this.mLocalAdapter, this.mProfileManager, device);
        }
        return cachedDevice;
    }

    @Override
    public synchronized void onServiceConnected() {
        if (this.mRunnable != null) {
            this.mRunnable.run();
            this.mRunnable = null;
            this.mProfileManager.removeServiceListener(this);
        }
    }

    @Override
    public void onServiceDisconnected() {
    }
}
