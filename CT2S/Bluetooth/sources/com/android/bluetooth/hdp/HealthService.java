package com.android.bluetooth.hdp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHealthAppConfiguration;
import android.bluetooth.IBluetoothHealth;
import android.bluetooth.IBluetoothHealthCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.ProfileService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class HealthService extends ProfileService {
    private static final int APP_REG_STATE_DEREG_FAILED = 3;
    private static final int APP_REG_STATE_DEREG_SUCCESS = 2;
    private static final int APP_REG_STATE_REG_FAILED = 1;
    private static final int APP_REG_STATE_REG_SUCCESS = 0;
    private static final int CHANNEL_TYPE_ANY = 2;
    private static final int CHANNEL_TYPE_RELIABLE = 0;
    private static final int CHANNEL_TYPE_STREAMING = 1;
    private static final int CONN_STATE_CONNECTED = 1;
    private static final int CONN_STATE_CONNECTING = 0;
    private static final int CONN_STATE_DESTROYED = 4;
    private static final int CONN_STATE_DISCONNECTED = 3;
    private static final int CONN_STATE_DISCONNECTING = 2;
    private static final boolean DBG = true;
    private static final int MDEP_ROLE_SINK = 1;
    private static final int MDEP_ROLE_SOURCE = 0;
    private static final int MESSAGE_APP_REGISTRATION_CALLBACK = 11;
    private static final int MESSAGE_CHANNEL_STATE_CALLBACK = 12;
    private static final int MESSAGE_CONNECT_CHANNEL = 3;
    private static final int MESSAGE_DISCONNECT_CHANNEL = 4;
    private static final int MESSAGE_REGISTER_APPLICATION = 1;
    private static final int MESSAGE_UNREGISTER_APPLICATION = 2;
    private static final String TAG = "HealthService";
    private static final boolean VDBG = false;
    private Map<BluetoothHealthAppConfiguration, AppInfo> mApps;
    private HealthServiceMessageHandler mHandler;
    private List<HealthChannel> mHealthChannels;
    private Map<BluetoothDevice, Integer> mHealthDevices;
    private boolean mNativeAvailable;

    private static native void classInitNative();

    private native void cleanupNative();

    private native int connectChannelNative(byte[] bArr, int i);

    private native boolean disconnectChannelNative(int i);

    private native void initializeNative();

    private native int registerHealthAppNative(int i, int i2, String str, int i3);

    private native boolean unregisterHealthAppNative(int i);

    static {
        classInitNative();
    }

    @Override
    protected String getName() {
        return TAG;
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHealthBinder(this);
    }

    @Override
    protected boolean start() {
        this.mHealthChannels = Collections.synchronizedList(new ArrayList());
        this.mApps = Collections.synchronizedMap(new HashMap());
        this.mHealthDevices = Collections.synchronizedMap(new HashMap());
        HandlerThread thread = new HandlerThread("BluetoothHdpHandler");
        thread.start();
        Looper looper = thread.getLooper();
        this.mHandler = new HealthServiceMessageHandler(looper);
        initializeNative();
        this.mNativeAvailable = true;
        return true;
    }

    @Override
    protected boolean stop() {
        if (this.mHandler != null) {
            this.mHandler.removeCallbacksAndMessages(null);
            Looper looper = this.mHandler.getLooper();
            if (looper != null) {
                looper.quit();
            }
        }
        cleanupApps();
        return true;
    }

    private void cleanupApps() {
        if (this.mApps != null) {
            Iterator<Map.Entry<BluetoothHealthAppConfiguration, AppInfo>> it = this.mApps.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BluetoothHealthAppConfiguration, AppInfo> entry = it.next();
                AppInfo appInfo = entry.getValue();
                if (appInfo != null) {
                    appInfo.cleanup();
                }
                it.remove();
            }
        }
    }

    @Override
    protected boolean cleanup() {
        this.mHandler = null;
        if (this.mNativeAvailable) {
            cleanupNative();
            this.mNativeAvailable = false;
        }
        if (this.mHealthChannels != null) {
            this.mHealthChannels.clear();
        }
        if (this.mHealthDevices != null) {
            this.mHealthDevices.clear();
        }
        if (this.mApps != null) {
            this.mApps.clear();
            return true;
        }
        return true;
    }

    private final class HealthServiceMessageHandler extends Handler {
        private HealthServiceMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            HealthService.this.log("HealthService Handler msg: " + msg.what);
            switch (msg.what) {
                case 1:
                    BluetoothHealthAppConfiguration appConfig = (BluetoothHealthAppConfiguration) msg.obj;
                    AppInfo appInfo = (AppInfo) HealthService.this.mApps.get(appConfig);
                    if (appInfo != null) {
                        int halRole = HealthService.this.convertRoleToHal(appConfig.getRole());
                        int halChannelType = HealthService.this.convertChannelTypeToHal(appConfig.getChannelType());
                        int appId = HealthService.this.registerHealthAppNative(appConfig.getDataType(), halRole, appConfig.getName(), halChannelType);
                        if (appId == -1) {
                            HealthService.this.callStatusCallback(appConfig, 1);
                            appInfo.cleanup();
                            HealthService.this.mApps.remove(appConfig);
                        } else {
                            appInfo.mRcpObj = new BluetoothHealthDeathRecipient(HealthService.this, appConfig);
                            IBinder binder = appInfo.mCallback.asBinder();
                            try {
                                binder.linkToDeath(appInfo.mRcpObj, 0);
                            } catch (RemoteException e) {
                                Log.e(HealthService.TAG, "LinktoDeath Exception:" + e);
                            }
                            appInfo.mAppId = appId;
                            HealthService.this.callStatusCallback(appConfig, 0);
                        }
                    }
                    break;
                case 2:
                    BluetoothHealthAppConfiguration appConfig2 = (BluetoothHealthAppConfiguration) msg.obj;
                    int appId2 = ((AppInfo) HealthService.this.mApps.get(appConfig2)).mAppId;
                    if (!HealthService.this.unregisterHealthAppNative(appId2)) {
                        Log.e(HealthService.TAG, "Failed to unregister application: id: " + appId2);
                        HealthService.this.callStatusCallback(appConfig2, 3);
                    }
                    break;
                case 3:
                    HealthChannel chan = (HealthChannel) msg.obj;
                    byte[] devAddr = Utils.getByteAddress(chan.mDevice);
                    chan.mChannelId = HealthService.this.connectChannelNative(devAddr, ((AppInfo) HealthService.this.mApps.get(chan.mConfig)).mAppId);
                    if (chan.mChannelId == -1) {
                        HealthService.this.callHealthChannelCallback(chan.mConfig, chan.mDevice, 3, 0, chan.mChannelFd, chan.mChannelId);
                        HealthService.this.callHealthChannelCallback(chan.mConfig, chan.mDevice, 0, 3, chan.mChannelFd, chan.mChannelId);
                    }
                    break;
                case 4:
                    HealthChannel chan2 = (HealthChannel) msg.obj;
                    if (!HealthService.this.disconnectChannelNative(chan2.mChannelId)) {
                        HealthService.this.callHealthChannelCallback(chan2.mConfig, chan2.mDevice, 3, 2, chan2.mChannelFd, chan2.mChannelId);
                        HealthService.this.callHealthChannelCallback(chan2.mConfig, chan2.mDevice, 2, 3, chan2.mChannelFd, chan2.mChannelId);
                    }
                    break;
                case 11:
                    BluetoothHealthAppConfiguration appConfig3 = HealthService.this.findAppConfigByAppId(msg.arg1);
                    if (appConfig3 != null) {
                        int regStatus = HealthService.this.convertHalRegStatus(msg.arg2);
                        HealthService.this.callStatusCallback(appConfig3, regStatus);
                        if (regStatus == 1 || regStatus == 2) {
                            ((AppInfo) HealthService.this.mApps.get(appConfig3)).cleanup();
                            HealthService.this.mApps.remove(appConfig3);
                        }
                    }
                    break;
                case 12:
                    ChannelStateEvent channelStateEvent = (ChannelStateEvent) msg.obj;
                    HealthChannel chan3 = HealthService.this.findChannelById(channelStateEvent.mChannelId);
                    BluetoothHealthAppConfiguration appConfig4 = HealthService.this.findAppConfigByAppId(channelStateEvent.mAppId);
                    if (HealthService.this.convertHalChannelState(channelStateEvent.mState) == 0 && appConfig4 == null) {
                        Log.e(HealthService.TAG, "Disconnected for non existing app");
                    } else {
                        if (chan3 == null) {
                            BluetoothDevice device = HealthService.this.getDevice(channelStateEvent.mAddr);
                            chan3 = new HealthChannel(device, appConfig4, appConfig4.getChannelType());
                            chan3.mChannelId = channelStateEvent.mChannelId;
                            HealthService.this.mHealthChannels.add(chan3);
                        }
                        int newState = HealthService.this.convertHalChannelState(channelStateEvent.mState);
                        if (newState == 2) {
                            try {
                                chan3.mChannelFd = ParcelFileDescriptor.dup(channelStateEvent.mFd);
                            } catch (IOException e2) {
                                Log.e(HealthService.TAG, "failed to dup ParcelFileDescriptor");
                                return;
                            }
                        } else {
                            chan3.mChannelFd = null;
                        }
                        HealthService.this.callHealthChannelCallback(chan3.mConfig, chan3.mDevice, newState, chan3.mState, chan3.mChannelFd, chan3.mChannelId);
                        chan3.mState = newState;
                        if (channelStateEvent.mState == 4) {
                            HealthService.this.mHealthChannels.remove(chan3);
                        }
                    }
                    break;
            }
        }
    }

    private static class BluetoothHealthDeathRecipient implements IBinder.DeathRecipient {
        private BluetoothHealthAppConfiguration mConfig;
        private HealthService mService;

        public BluetoothHealthDeathRecipient(HealthService service, BluetoothHealthAppConfiguration config) {
            this.mService = service;
            this.mConfig = config;
        }

        @Override
        public void binderDied() {
            Log.d(HealthService.TAG, "Binder is dead.");
            this.mService.unregisterAppConfiguration(this.mConfig);
        }

        public void cleanup() {
            this.mService = null;
            this.mConfig = null;
        }
    }

    private static class BluetoothHealthBinder extends IBluetoothHealth.Stub implements ProfileService.IProfileServiceBinder {
        private HealthService mService;

        public BluetoothHealthBinder(HealthService svc) {
            this.mService = svc;
        }

        @Override
        public boolean cleanup() {
            this.mService = null;
            return true;
        }

        private HealthService getService() {
            if (!Utils.checkCaller()) {
                Log.w(HealthService.TAG, "Health call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        public boolean registerAppConfiguration(BluetoothHealthAppConfiguration config, IBluetoothHealthCallback callback) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.registerAppConfiguration(config, callback);
        }

        public boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration config) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.unregisterAppConfiguration(config);
        }

        public boolean connectChannelToSource(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.connectChannelToSource(device, config);
        }

        public boolean connectChannelToSink(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelType) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.connectChannelToSink(device, config, channelType);
        }

        public boolean disconnectChannel(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelId) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnectChannel(device, config, channelId);
        }

        public ParcelFileDescriptor getMainChannelFd(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
            HealthService service = getService();
            if (service == null) {
                return null;
            }
            return service.getMainChannelFd(device, config);
        }

        public int getHealthDeviceConnectionState(BluetoothDevice device) {
            HealthService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getHealthDeviceConnectionState(device);
        }

        public List<BluetoothDevice> getConnectedHealthDevices() {
            HealthService service = getService();
            return service == null ? new ArrayList(0) : service.getConnectedHealthDevices();
        }

        public List<BluetoothDevice> getHealthDevicesMatchingConnectionStates(int[] states) {
            HealthService service = getService();
            return service == null ? new ArrayList(0) : service.getHealthDevicesMatchingConnectionStates(states);
        }
    }

    boolean registerAppConfiguration(BluetoothHealthAppConfiguration config, IBluetoothHealthCallback callback) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (this.mApps.get(config) != null) {
            Log.d(TAG, "Config has already been registered");
            return false;
        }
        this.mApps.put(config, new AppInfo(callback));
        Message msg = this.mHandler.obtainMessage(1, config);
        this.mHandler.sendMessage(msg);
        return true;
    }

    boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration config) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (this.mApps.get(config) == null) {
            Log.d(TAG, "unregisterAppConfiguration: no app found");
            return false;
        }
        Message msg = this.mHandler.obtainMessage(2, config);
        this.mHandler.sendMessage(msg);
        return true;
    }

    boolean connectChannelToSource(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return connectChannel(device, config, 12);
    }

    boolean connectChannelToSink(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelType) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return connectChannel(device, config, channelType);
    }

    boolean disconnectChannel(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelId) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HealthChannel chan = findChannelById(channelId);
        if (chan == null) {
            Log.d(TAG, "disconnectChannel: no channel found");
            return false;
        }
        Message msg = this.mHandler.obtainMessage(4, chan);
        this.mHandler.sendMessage(msg);
        return true;
    }

    ParcelFileDescriptor getMainChannelFd(BluetoothDevice device, BluetoothHealthAppConfiguration config) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HealthChannel healthChan = null;
        for (HealthChannel chan : this.mHealthChannels) {
            if (chan.mDevice.equals(device) && chan.mConfig.equals(config)) {
                healthChan = chan;
            }
        }
        if (healthChan == null) {
            Log.e(TAG, "No channel found for device: " + device + " config: " + config);
            return null;
        }
        return healthChan.mChannelFd;
    }

    int getHealthDeviceConnectionState(BluetoothDevice device) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return getConnectionState(device);
    }

    List<BluetoothDevice> getConnectedHealthDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        List<BluetoothDevice> devices = lookupHealthDevicesMatchingStates(new int[]{2});
        return devices;
    }

    List<BluetoothDevice> getHealthDevicesMatchingConnectionStates(int[] states) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        List<BluetoothDevice> devices = lookupHealthDevicesMatchingStates(states);
        return devices;
    }

    private void onAppRegistrationState(int appId, int state) {
        Message msg = this.mHandler.obtainMessage(11);
        msg.arg1 = appId;
        msg.arg2 = state;
        this.mHandler.sendMessage(msg);
    }

    private void onChannelStateChanged(int appId, byte[] addr, int cfgIndex, int channelId, int state, FileDescriptor pfd) {
        Message msg = this.mHandler.obtainMessage(12);
        ChannelStateEvent channelStateEvent = new ChannelStateEvent(appId, addr, cfgIndex, channelId, state, pfd);
        msg.obj = channelStateEvent;
        this.mHandler.sendMessage(msg);
    }

    private String getStringChannelType(int type) {
        if (type == 10) {
            return "Reliable";
        }
        if (type == 11) {
            return "Streaming";
        }
        return "Any";
    }

    private void callStatusCallback(BluetoothHealthAppConfiguration config, int status) {
        IBluetoothHealthCallback callback = this.mApps.get(config).mCallback;
        if (callback == null) {
            Log.e(TAG, "Callback object null");
        }
        try {
            callback.onHealthAppConfigurationStatusChange(config, status);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote Exception:" + e);
        }
    }

    private BluetoothHealthAppConfiguration findAppConfigByAppId(int appId) {
        BluetoothHealthAppConfiguration appConfig = null;
        Iterator<Map.Entry<BluetoothHealthAppConfiguration, AppInfo>> it = this.mApps.entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<BluetoothHealthAppConfiguration, AppInfo> e = it.next();
            if (appId == e.getValue().mAppId) {
                BluetoothHealthAppConfiguration appConfig2 = e.getKey();
                appConfig = appConfig2;
                break;
            }
        }
        if (appConfig == null) {
            Log.e(TAG, "No appConfig found for " + appId);
        }
        return appConfig;
    }

    private int convertHalRegStatus(int halRegStatus) {
        switch (halRegStatus) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.e(TAG, "Unexpected App Registration state: " + halRegStatus);
                break;
        }
        return 1;
    }

    private int convertHalChannelState(int halChannelState) {
        switch (halChannelState) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
            case 4:
                break;
            default:
                Log.e(TAG, "Unexpected channel state: " + halChannelState);
                break;
        }
        return 0;
    }

    private boolean connectChannel(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelType) {
        if (this.mApps.get(config) == null) {
            Log.e(TAG, "connectChannel fail to get a app id from config");
            return false;
        }
        HealthChannel chan = new HealthChannel(device, config, channelType);
        Message msg = this.mHandler.obtainMessage(3);
        msg.obj = chan;
        this.mHandler.sendMessage(msg);
        return true;
    }

    private void callHealthChannelCallback(BluetoothHealthAppConfiguration config, BluetoothDevice device, int state, int prevState, ParcelFileDescriptor fd, int id) {
        broadcastHealthDeviceStateChange(device, state);
        log("Health Device Callback: " + device + " State Change: " + prevState + "->" + state);
        ParcelFileDescriptor dupedFd = null;
        if (fd != null) {
            try {
                dupedFd = fd.dup();
            } catch (IOException e) {
                dupedFd = null;
                Log.e(TAG, "Exception while duping: " + e);
            }
        }
        IBluetoothHealthCallback callback = this.mApps.get(config).mCallback;
        if (callback == null) {
            Log.e(TAG, "No callback found for config: " + config);
            return;
        }
        try {
            callback.onHealthChannelStateChange(config, device, prevState, state, dupedFd, id);
        } catch (RemoteException e2) {
            Log.e(TAG, "Remote Exception:" + e2);
        }
    }

    private void broadcastHealthDeviceStateChange(BluetoothDevice device, int newChannelState) {
        if (this.mHealthDevices.get(device) == null) {
            this.mHealthDevices.put(device, 0);
        }
        int currDeviceState = this.mHealthDevices.get(device).intValue();
        int newDeviceState = convertState(newChannelState);
        if (currDeviceState != newDeviceState) {
            boolean sendIntent = false;
            switch (currDeviceState) {
                case 0:
                    sendIntent = true;
                    break;
                case 1:
                    if (newDeviceState == 2) {
                        sendIntent = true;
                    } else {
                        List<HealthChannel> chan = findChannelByStates(device, new int[]{1, 3});
                        if (chan.isEmpty()) {
                            sendIntent = true;
                        }
                    }
                    break;
                case 2:
                    List<HealthChannel> chan2 = findChannelByStates(device, new int[]{1, 2});
                    if (chan2.isEmpty()) {
                        sendIntent = true;
                    }
                    break;
                case 3:
                    List<HealthChannel> chan3 = findChannelByStates(device, new int[]{1, 3});
                    if (chan3.isEmpty()) {
                        updateAndSendIntent(device, newDeviceState, currDeviceState);
                    }
                    break;
            }
            if (sendIntent) {
                updateAndSendIntent(device, newDeviceState, currDeviceState);
            }
        }
    }

    private void updateAndSendIntent(BluetoothDevice device, int newDeviceState, int prevDeviceState) {
        if (newDeviceState == 0) {
            this.mHealthDevices.remove(device);
        } else {
            this.mHealthDevices.put(device, Integer.valueOf(newDeviceState));
        }
        notifyProfileConnectionStateChanged(device, 3, newDeviceState, prevDeviceState);
    }

    private int convertState(int state) {
        switch (state) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.e(TAG, "Mismatch in Channel and Health Device State: " + state);
                break;
        }
        return 0;
    }

    private int convertRoleToHal(int role) {
        if (role == 1) {
            return 0;
        }
        if (role == 2) {
            return 1;
        }
        Log.e(TAG, "unkonw role: " + role);
        return 1;
    }

    private int convertChannelTypeToHal(int channelType) {
        if (channelType == 10) {
            return 0;
        }
        if (channelType == 11) {
            return 1;
        }
        if (channelType == 12) {
            return 2;
        }
        Log.e(TAG, "unkonw channel type: " + channelType);
        return 2;
    }

    private HealthChannel findChannelById(int id) {
        for (HealthChannel chan : this.mHealthChannels) {
            if (chan.mChannelId == id) {
                return chan;
            }
        }
        Log.e(TAG, "No channel found by id: " + id);
        return null;
    }

    private List<HealthChannel> findChannelByStates(BluetoothDevice device, int[] states) {
        List<HealthChannel> channels = new ArrayList<>();
        for (HealthChannel chan : this.mHealthChannels) {
            if (chan.mDevice.equals(device)) {
                for (int state : states) {
                    if (chan.mState == state) {
                        channels.add(chan);
                    }
                }
            }
        }
        return channels;
    }

    private int getConnectionState(BluetoothDevice device) {
        if (this.mHealthDevices.get(device) == null) {
            return 0;
        }
        return this.mHealthDevices.get(device).intValue();
    }

    List<BluetoothDevice> lookupHealthDevicesMatchingStates(int[] states) {
        List<BluetoothDevice> healthDevices = new ArrayList<>();
        for (BluetoothDevice device : this.mHealthDevices.keySet()) {
            int healthDeviceState = getConnectionState(device);
            int len$ = states.length;
            int i$ = 0;
            while (true) {
                if (i$ < len$) {
                    int state = states[i$];
                    if (state != healthDeviceState) {
                        i$++;
                    } else {
                        healthDevices.add(device);
                        break;
                    }
                }
            }
        }
        return healthDevices;
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mHealthChannels:");
        for (HealthChannel channel : this.mHealthChannels) {
            println(sb, "  " + channel);
        }
        println(sb, "mApps:");
        for (BluetoothHealthAppConfiguration conf : this.mApps.keySet()) {
            println(sb, "  " + conf + " : " + this.mApps.get(conf));
        }
        println(sb, "mHealthDevices:");
        for (BluetoothDevice device : this.mHealthDevices.keySet()) {
            println(sb, "  " + device + " : " + this.mHealthDevices.get(device));
        }
    }

    private static class AppInfo {
        private int mAppId;
        private IBluetoothHealthCallback mCallback;
        private BluetoothHealthDeathRecipient mRcpObj;

        private AppInfo(IBluetoothHealthCallback callback) {
            this.mCallback = callback;
            this.mRcpObj = null;
            this.mAppId = -1;
        }

        private void cleanup() {
            if (this.mCallback != null) {
                if (this.mRcpObj != null) {
                    IBinder binder = this.mCallback.asBinder();
                    try {
                        binder.unlinkToDeath(this.mRcpObj, 0);
                    } catch (NoSuchElementException e) {
                        Log.e(HealthService.TAG, "No death recipient registered" + e);
                    }
                    this.mRcpObj.cleanup();
                    this.mRcpObj = null;
                }
                this.mCallback = null;
                return;
            }
            if (this.mRcpObj != null) {
                this.mRcpObj.cleanup();
                this.mRcpObj = null;
            }
        }
    }

    private class HealthChannel {
        private ParcelFileDescriptor mChannelFd;
        private int mChannelId;
        private int mChannelType;
        private BluetoothHealthAppConfiguration mConfig;
        private BluetoothDevice mDevice;
        private int mState;

        private HealthChannel(BluetoothDevice device, BluetoothHealthAppConfiguration config, int channelType) {
            this.mChannelFd = null;
            this.mDevice = device;
            this.mConfig = config;
            this.mState = 0;
            this.mChannelType = channelType;
            this.mChannelId = -1;
        }
    }

    private class ChannelStateEvent {
        byte[] mAddr;
        int mAppId;
        int mCfgIndex;
        int mChannelId;
        FileDescriptor mFd;
        int mState;

        private ChannelStateEvent(int appId, byte[] addr, int cfgIndex, int channelId, int state, FileDescriptor fileDescriptor) {
            this.mAppId = appId;
            this.mAddr = addr;
            this.mCfgIndex = cfgIndex;
            this.mState = state;
            this.mChannelId = channelId;
            this.mFd = fileDescriptor;
        }
    }
}
