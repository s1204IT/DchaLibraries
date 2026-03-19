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
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.MetricsLogger;
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
    private static HealthService sHealthService;
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
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHealthBinder(this);
    }

    @Override
    protected boolean start() {
        this.mHealthChannels = Collections.synchronizedList(new ArrayList());
        this.mApps = Collections.synchronizedMap(new HashMap());
        this.mHealthDevices = Collections.synchronizedMap(new HashMap());
        HandlerThread handlerThread = new HandlerThread("BluetoothHdpHandler");
        handlerThread.start();
        this.mHandler = new HealthServiceMessageHandler(handlerThread.getLooper());
        initializeNative();
        this.mNativeAvailable = true;
        setHealthService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        setHealthService(null);
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
                AppInfo value = it.next().getValue();
                if (value != null) {
                    value.cleanup();
                }
                it.remove();
            }
        }
    }

    @Override
    protected void cleanup() {
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
        }
    }

    @VisibleForTesting
    public static synchronized HealthService getHealthService() {
        if (sHealthService == null) {
            Log.w(TAG, "getHealthService(): service is null");
            return null;
        }
        if (!sHealthService.isAvailable()) {
            Log.w(TAG, "getHealthService(): service is not available");
            return null;
        }
        return sHealthService;
    }

    private static synchronized void setHealthService(HealthService healthService) {
        Log.d(TAG, "setHealthService(): set to: " + healthService);
        sHealthService = healthService;
    }

    private final class HealthServiceMessageHandler extends Handler {
        private HealthServiceMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) throws IOException {
            Log.d(HealthService.TAG, "HealthService Handler msg: " + message.what);
            int i = message.what;
            switch (i) {
                case 1:
                    BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration = (BluetoothHealthAppConfiguration) message.obj;
                    AppInfo appInfo = (AppInfo) HealthService.this.mApps.get(bluetoothHealthAppConfiguration);
                    if (appInfo != null) {
                        int iRegisterHealthAppNative = HealthService.this.registerHealthAppNative(bluetoothHealthAppConfiguration.getDataType(), HealthService.this.convertRoleToHal(bluetoothHealthAppConfiguration.getRole()), bluetoothHealthAppConfiguration.getName(), HealthService.this.convertChannelTypeToHal(bluetoothHealthAppConfiguration.getChannelType()));
                        if (iRegisterHealthAppNative == -1) {
                            HealthService.this.callStatusCallback(bluetoothHealthAppConfiguration, 1);
                            appInfo.cleanup();
                            HealthService.this.mApps.remove(bluetoothHealthAppConfiguration);
                        } else {
                            appInfo.mRcpObj = new BluetoothHealthDeathRecipient(HealthService.this, bluetoothHealthAppConfiguration);
                            try {
                                appInfo.mCallback.asBinder().linkToDeath(appInfo.mRcpObj, 0);
                            } catch (RemoteException e) {
                                Log.e(HealthService.TAG, "LinktoDeath Exception:" + e);
                            }
                            appInfo.mAppId = iRegisterHealthAppNative;
                            HealthService.this.callStatusCallback(bluetoothHealthAppConfiguration, 0);
                        }
                        break;
                    }
                    break;
                case 2:
                    BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration2 = (BluetoothHealthAppConfiguration) message.obj;
                    int i2 = ((AppInfo) HealthService.this.mApps.get(bluetoothHealthAppConfiguration2)).mAppId;
                    if (!HealthService.this.unregisterHealthAppNative(i2)) {
                        Log.e(HealthService.TAG, "Failed to unregister application: id: " + i2);
                        HealthService.this.callStatusCallback(bluetoothHealthAppConfiguration2, 3);
                    }
                    break;
                case 3:
                    HealthChannel healthChannel = (HealthChannel) message.obj;
                    healthChannel.mChannelId = HealthService.this.connectChannelNative(Utils.getByteAddress(healthChannel.mDevice), ((AppInfo) HealthService.this.mApps.get(healthChannel.mConfig)).mAppId);
                    if (healthChannel.mChannelId == -1) {
                        HealthService.this.callHealthChannelCallback(healthChannel.mConfig, healthChannel.mDevice, 3, 0, healthChannel.mChannelFd, healthChannel.mChannelId);
                        HealthService.this.callHealthChannelCallback(healthChannel.mConfig, healthChannel.mDevice, 0, 3, healthChannel.mChannelFd, healthChannel.mChannelId);
                    }
                    break;
                case 4:
                    HealthChannel healthChannel2 = (HealthChannel) message.obj;
                    if (!HealthService.this.disconnectChannelNative(healthChannel2.mChannelId)) {
                        HealthService.this.callHealthChannelCallback(healthChannel2.mConfig, healthChannel2.mDevice, 3, 2, healthChannel2.mChannelFd, healthChannel2.mChannelId);
                        HealthService.this.callHealthChannelCallback(healthChannel2.mConfig, healthChannel2.mDevice, 2, 3, healthChannel2.mChannelFd, healthChannel2.mChannelId);
                    }
                    break;
                default:
                    switch (i) {
                        case 11:
                            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationFindAppConfigByAppId = HealthService.this.findAppConfigByAppId(message.arg1);
                            if (bluetoothHealthAppConfigurationFindAppConfigByAppId != null) {
                                int iConvertHalRegStatus = HealthService.this.convertHalRegStatus(message.arg2);
                                HealthService.this.callStatusCallback(bluetoothHealthAppConfigurationFindAppConfigByAppId, iConvertHalRegStatus);
                                if (iConvertHalRegStatus == 1 || iConvertHalRegStatus == 2) {
                                    ((AppInfo) HealthService.this.mApps.get(bluetoothHealthAppConfigurationFindAppConfigByAppId)).cleanup();
                                    HealthService.this.mApps.remove(bluetoothHealthAppConfigurationFindAppConfigByAppId);
                                }
                                break;
                            }
                            break;
                        case 12:
                            ChannelStateEvent channelStateEvent = (ChannelStateEvent) message.obj;
                            HealthChannel healthChannelFindChannelById = HealthService.this.findChannelById(channelStateEvent.mChannelId);
                            BluetoothHealthAppConfiguration bluetoothHealthAppConfigurationFindAppConfigByAppId2 = HealthService.this.findAppConfigByAppId(channelStateEvent.mAppId);
                            if (HealthService.this.convertHalChannelState(channelStateEvent.mState) == 0 && bluetoothHealthAppConfigurationFindAppConfigByAppId2 == null) {
                                Log.e(HealthService.TAG, "Disconnected for non existing app");
                            } else {
                                if (healthChannelFindChannelById == null) {
                                    healthChannelFindChannelById = new HealthChannel(HealthService.this.getDevice(channelStateEvent.mAddr), bluetoothHealthAppConfigurationFindAppConfigByAppId2, bluetoothHealthAppConfigurationFindAppConfigByAppId2.getChannelType());
                                    healthChannelFindChannelById.mChannelId = channelStateEvent.mChannelId;
                                    HealthService.this.mHealthChannels.add(healthChannelFindChannelById);
                                }
                                int iConvertHalChannelState = HealthService.this.convertHalChannelState(channelStateEvent.mState);
                                if (iConvertHalChannelState == 2) {
                                    try {
                                        healthChannelFindChannelById.mChannelFd = ParcelFileDescriptor.dup(channelStateEvent.mFd);
                                    } catch (IOException e2) {
                                        Log.e(HealthService.TAG, "failed to dup ParcelFileDescriptor");
                                        return;
                                    }
                                } else {
                                    healthChannelFindChannelById.mChannelFd = null;
                                }
                                HealthService.this.callHealthChannelCallback(healthChannelFindChannelById.mConfig, healthChannelFindChannelById.mDevice, iConvertHalChannelState, healthChannelFindChannelById.mState, healthChannelFindChannelById.mChannelFd, healthChannelFindChannelById.mChannelId);
                                healthChannelFindChannelById.mState = iConvertHalChannelState;
                                if (channelStateEvent.mState == 4 || channelStateEvent.mState == 3) {
                                    HealthService.this.mHealthChannels.remove(healthChannelFindChannelById);
                                }
                            }
                            break;
                    }
                    break;
            }
        }
    }

    private static class BluetoothHealthDeathRecipient implements IBinder.DeathRecipient {
        private BluetoothHealthAppConfiguration mConfig;
        private HealthService mService;

        BluetoothHealthDeathRecipient(HealthService healthService, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) {
            this.mService = healthService;
            this.mConfig = bluetoothHealthAppConfiguration;
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

        BluetoothHealthBinder(HealthService healthService) {
            this.mService = healthService;
        }

        @Override
        public void cleanup() {
            this.mService = null;
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

        public boolean registerAppConfiguration(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, IBluetoothHealthCallback iBluetoothHealthCallback) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.registerAppConfiguration(bluetoothHealthAppConfiguration, iBluetoothHealthCallback);
        }

        public boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.unregisterAppConfiguration(bluetoothHealthAppConfiguration);
        }

        public boolean connectChannelToSource(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.connectChannelToSource(bluetoothDevice, bluetoothHealthAppConfiguration);
        }

        public boolean connectChannelToSink(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.connectChannelToSink(bluetoothDevice, bluetoothHealthAppConfiguration, i);
        }

        public boolean disconnectChannel(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) {
            HealthService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnectChannel(bluetoothDevice, bluetoothHealthAppConfiguration, i);
        }

        public ParcelFileDescriptor getMainChannelFd(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) {
            HealthService service = getService();
            if (service == null) {
                return null;
            }
            return service.getMainChannelFd(bluetoothDevice, bluetoothHealthAppConfiguration);
        }

        public int getHealthDeviceConnectionState(BluetoothDevice bluetoothDevice) {
            HealthService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getHealthDeviceConnectionState(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedHealthDevices() {
            HealthService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getConnectedHealthDevices();
        }

        public List<BluetoothDevice> getHealthDevicesMatchingConnectionStates(int[] iArr) {
            HealthService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getHealthDevicesMatchingConnectionStates(iArr);
        }
    }

    boolean registerAppConfiguration(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, IBluetoothHealthCallback iBluetoothHealthCallback) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (bluetoothHealthAppConfiguration == null) {
            Log.e(TAG, "Trying to use a null config for registration");
            return false;
        }
        if (this.mApps.get(bluetoothHealthAppConfiguration) != null) {
            Log.d(TAG, "Config has already been registered");
            return false;
        }
        this.mApps.put(bluetoothHealthAppConfiguration, new AppInfo(iBluetoothHealthCallback));
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, bluetoothHealthAppConfiguration));
        return true;
    }

    boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (this.mApps.get(bluetoothHealthAppConfiguration) == null) {
            Log.d(TAG, "unregisterAppConfiguration: no app found");
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, bluetoothHealthAppConfiguration));
        return true;
    }

    boolean connectChannelToSource(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return connectChannel(bluetoothDevice, bluetoothHealthAppConfiguration, 12);
    }

    boolean connectChannelToSink(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return connectChannel(bluetoothDevice, bluetoothHealthAppConfiguration, i);
    }

    boolean disconnectChannel(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HealthChannel healthChannelFindChannelById = findChannelById(i);
        if (healthChannelFindChannelById == null) {
            Log.d(TAG, "disconnectChannel: no channel found");
            return false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, healthChannelFindChannelById));
        return true;
    }

    ParcelFileDescriptor getMainChannelFd(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        HealthChannel healthChannel = null;
        for (HealthChannel healthChannel2 : this.mHealthChannels) {
            if (healthChannel2.mDevice.equals(bluetoothDevice) && healthChannel2.mConfig.equals(bluetoothHealthAppConfiguration)) {
                healthChannel = healthChannel2;
            }
        }
        if (healthChannel == null) {
            Log.e(TAG, "No channel found for device: " + bluetoothDevice + " config: " + bluetoothHealthAppConfiguration);
            return null;
        }
        return healthChannel.mChannelFd;
    }

    int getHealthDeviceConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return getConnectionState(bluetoothDevice);
    }

    List<BluetoothDevice> getConnectedHealthDevices() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return lookupHealthDevicesMatchingStates(new int[]{2});
    }

    List<BluetoothDevice> getHealthDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        return lookupHealthDevicesMatchingStates(iArr);
    }

    private void onAppRegistrationState(int i, int i2) {
        Message messageObtainMessage = this.mHandler.obtainMessage(11);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.arg2 = i2;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void onChannelStateChanged(int i, byte[] bArr, int i2, int i3, int i4, FileDescriptor fileDescriptor) {
        Message messageObtainMessage = this.mHandler.obtainMessage(12);
        messageObtainMessage.obj = new ChannelStateEvent(i, bArr, i2, i3, i4, fileDescriptor);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private String getStringChannelType(int i) {
        if (i == 10) {
            return "Reliable";
        }
        if (i == 11) {
            return "Streaming";
        }
        return "Any";
    }

    private void callStatusCallback(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) {
        IBluetoothHealthCallback iBluetoothHealthCallback = this.mApps.get(bluetoothHealthAppConfiguration).mCallback;
        if (iBluetoothHealthCallback == null) {
            Log.e(TAG, "Callback object null");
        }
        try {
            iBluetoothHealthCallback.onHealthAppConfigurationStatusChange(bluetoothHealthAppConfiguration, i);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote Exception:" + e);
        }
    }

    private BluetoothHealthAppConfiguration findAppConfigByAppId(int i) {
        BluetoothHealthAppConfiguration key;
        Iterator<Map.Entry<BluetoothHealthAppConfiguration, AppInfo>> it = this.mApps.entrySet().iterator();
        while (true) {
            if (!it.hasNext()) {
                key = null;
                break;
            }
            Map.Entry<BluetoothHealthAppConfiguration, AppInfo> next = it.next();
            if (i == next.getValue().mAppId) {
                key = next.getKey();
                break;
            }
        }
        if (key == null) {
            Log.e(TAG, "No appConfig found for " + i);
        }
        return key;
    }

    private int convertHalRegStatus(int i) {
        switch (i) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.e(TAG, "Unexpected App Registration state: " + i);
                break;
        }
        return 1;
    }

    private int convertHalChannelState(int i) {
        switch (i) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            default:
                Log.e(TAG, "Unexpected channel state: " + i);
                break;
        }
        return 0;
    }

    private boolean connectChannel(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) {
        if (this.mApps.get(bluetoothHealthAppConfiguration) == null) {
            Log.e(TAG, "connectChannel fail to get a app id from config");
            return false;
        }
        HealthChannel healthChannel = new HealthChannel(bluetoothDevice, bluetoothHealthAppConfiguration, i);
        Message messageObtainMessage = this.mHandler.obtainMessage(3);
        messageObtainMessage.obj = healthChannel;
        this.mHandler.sendMessage(messageObtainMessage);
        return true;
    }

    private void callHealthChannelCallback(BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, BluetoothDevice bluetoothDevice, int i, int i2, ParcelFileDescriptor parcelFileDescriptor, int i3) throws IOException {
        ParcelFileDescriptor parcelFileDescriptorDup;
        broadcastHealthDeviceStateChange(bluetoothDevice, i);
        Log.d(TAG, "Health Device Callback: " + bluetoothDevice + " State Change: " + i2 + "->" + i + ", config: " + bluetoothHealthAppConfiguration);
        IBluetoothHealthCallback iBluetoothHealthCallback = null;
        if (parcelFileDescriptor != null) {
            try {
                parcelFileDescriptorDup = parcelFileDescriptor.dup();
            } catch (IOException e) {
                Log.e(TAG, "Exception while duping: " + e);
                parcelFileDescriptorDup = null;
            }
        } else {
            parcelFileDescriptorDup = null;
        }
        if (this.mApps.get(bluetoothHealthAppConfiguration) == null) {
            Log.d(TAG, "Health Device Callback: mApps.get(config) is null");
        } else {
            iBluetoothHealthCallback = this.mApps.get(bluetoothHealthAppConfiguration).mCallback;
        }
        IBluetoothHealthCallback iBluetoothHealthCallback2 = iBluetoothHealthCallback;
        if (iBluetoothHealthCallback2 == null) {
            Log.e(TAG, "No callback found for config: " + bluetoothHealthAppConfiguration);
            return;
        }
        try {
            iBluetoothHealthCallback2.onHealthChannelStateChange(bluetoothHealthAppConfiguration, bluetoothDevice, i2, i, parcelFileDescriptorDup, i3);
        } catch (RemoteException e2) {
            Log.e(TAG, "Remote Exception:" + e2);
        }
    }

    private void broadcastHealthDeviceStateChange(BluetoothDevice bluetoothDevice, int i) {
        if (this.mHealthDevices.get(bluetoothDevice) == null) {
            this.mHealthDevices.put(bluetoothDevice, 0);
        }
        int iIntValue = this.mHealthDevices.get(bluetoothDevice).intValue();
        int iConvertState = convertState(i);
        if (iIntValue == iConvertState) {
            return;
        }
        boolean z = true;
        switch (iIntValue) {
            case 1:
                if (iConvertState != 2 && !findChannelByStates(bluetoothDevice, new int[]{1, 3}).isEmpty()) {
                    z = false;
                }
                break;
            case 2:
                if (!findChannelByStates(bluetoothDevice, new int[]{1, 0}).isEmpty()) {
                }
                break;
            case 3:
                if (findChannelByStates(bluetoothDevice, new int[]{1, 3}).isEmpty()) {
                    updateAndSendIntent(bluetoothDevice, iConvertState, iIntValue);
                }
                z = false;
                break;
        }
        if (z) {
            updateAndSendIntent(bluetoothDevice, iConvertState, iIntValue);
        }
    }

    private void updateAndSendIntent(BluetoothDevice bluetoothDevice, int i, int i2) {
        if (i == 0) {
            this.mHealthDevices.remove(bluetoothDevice);
        } else {
            this.mHealthDevices.put(bluetoothDevice, Integer.valueOf(i));
        }
        if (i != i2 && i == 2) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.HEALTH);
        }
    }

    private int convertState(int i) {
        switch (i) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.e(TAG, "Mismatch in Channel and Health Device State: " + i);
                break;
        }
        return 0;
    }

    private int convertRoleToHal(int i) {
        if (i == 1) {
            return 0;
        }
        if (i == 2) {
            return 1;
        }
        Log.e(TAG, "unkonw role: " + i);
        return 1;
    }

    private int convertChannelTypeToHal(int i) {
        if (i == 10) {
            return 0;
        }
        if (i == 11) {
            return 1;
        }
        if (i == 12) {
            return 2;
        }
        Log.e(TAG, "unkonw channel type: " + i);
        return 2;
    }

    private HealthChannel findChannelById(int i) {
        for (HealthChannel healthChannel : this.mHealthChannels) {
            if (healthChannel.mChannelId == i) {
                return healthChannel;
            }
        }
        Log.e(TAG, "No channel found by id: " + i);
        return null;
    }

    private List<HealthChannel> findChannelByStates(BluetoothDevice bluetoothDevice, int[] iArr) {
        ArrayList arrayList = new ArrayList();
        for (HealthChannel healthChannel : this.mHealthChannels) {
            if (healthChannel.mDevice.equals(bluetoothDevice)) {
                for (int i : iArr) {
                    if (healthChannel.mState == i) {
                        arrayList.add(healthChannel);
                    }
                }
            }
        }
        return arrayList;
    }

    private int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (this.mHealthDevices.get(bluetoothDevice) == null) {
            return 0;
        }
        return this.mHealthDevices.get(bluetoothDevice).intValue();
    }

    List<BluetoothDevice> lookupHealthDevicesMatchingStates(int[] iArr) {
        ArrayList arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mHealthDevices.keySet()) {
            int connectionState = getConnectionState(bluetoothDevice);
            int length = iArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (iArr[i] != connectionState) {
                    i++;
                } else {
                    arrayList.add(bluetoothDevice);
                    break;
                }
            }
        }
        return arrayList;
    }

    @Override
    public void dump(StringBuilder sb) {
        super.dump(sb);
        println(sb, "mHealthChannels:");
        Iterator<HealthChannel> it = this.mHealthChannels.iterator();
        while (it.hasNext()) {
            println(sb, "  " + it.next());
        }
        println(sb, "mApps:");
        for (BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration : this.mApps.keySet()) {
            println(sb, "  " + bluetoothHealthAppConfiguration + " : " + this.mApps.get(bluetoothHealthAppConfiguration));
        }
        println(sb, "mHealthDevices:");
        for (BluetoothDevice bluetoothDevice : this.mHealthDevices.keySet()) {
            println(sb, "  " + bluetoothDevice + " : " + this.mHealthDevices.get(bluetoothDevice));
        }
    }

    private static class AppInfo {
        private int mAppId;
        private IBluetoothHealthCallback mCallback;
        private BluetoothHealthDeathRecipient mRcpObj;

        private AppInfo(IBluetoothHealthCallback iBluetoothHealthCallback) {
            this.mCallback = iBluetoothHealthCallback;
            this.mRcpObj = null;
            this.mAppId = -1;
        }

        private void cleanup() {
            if (this.mCallback != null) {
                if (this.mRcpObj != null) {
                    try {
                        this.mCallback.asBinder().unlinkToDeath(this.mRcpObj, 0);
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

        private HealthChannel(BluetoothDevice bluetoothDevice, BluetoothHealthAppConfiguration bluetoothHealthAppConfiguration, int i) {
            this.mChannelFd = null;
            this.mDevice = bluetoothDevice;
            this.mConfig = bluetoothHealthAppConfiguration;
            this.mState = 0;
            this.mChannelType = i;
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

        private ChannelStateEvent(int i, byte[] bArr, int i2, int i3, int i4, FileDescriptor fileDescriptor) {
            this.mAppId = i;
            this.mAddr = bArr;
            this.mCfgIndex = i2;
            this.mState = i4;
            this.mChannelId = i3;
            this.mFd = fileDescriptor;
        }
    }
}
