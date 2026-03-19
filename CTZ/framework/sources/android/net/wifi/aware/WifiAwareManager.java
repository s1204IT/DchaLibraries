package android.net.wifi.aware;

import android.content.Context;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.net.wifi.aware.TlvBufferUtils;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.util.List;
import libcore.util.HexEncoding;

public class WifiAwareManager {
    public static final String ACTION_WIFI_AWARE_STATE_CHANGED = "android.net.wifi.aware.action.WIFI_AWARE_STATE_CHANGED";
    private static final boolean DBG = false;
    private static final String TAG = "WifiAwareManager";
    private static final boolean VDBG = false;
    public static final int WIFI_AWARE_DATA_PATH_ROLE_INITIATOR = 0;
    public static final int WIFI_AWARE_DATA_PATH_ROLE_RESPONDER = 1;
    private final Context mContext;
    private final Object mLock = new Object();
    private final IWifiAwareManager mService;

    @Retention(RetentionPolicy.SOURCE)
    public @interface DataPathRole {
    }

    public WifiAwareManager(Context context, IWifiAwareManager iWifiAwareManager) {
        this.mContext = context;
        this.mService = iWifiAwareManager;
    }

    public boolean isAvailable() {
        try {
            return this.mService.isUsageEnabled();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Characteristics getCharacteristics() {
        try {
            return this.mService.getCharacteristics();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void attach(AttachCallback attachCallback, Handler handler) {
        attach(handler, null, attachCallback, null);
    }

    public void attach(AttachCallback attachCallback, IdentityChangedListener identityChangedListener, Handler handler) {
        attach(handler, null, attachCallback, identityChangedListener);
    }

    public void attach(Handler handler, ConfigRequest configRequest, AttachCallback attachCallback, IdentityChangedListener identityChangedListener) {
        if (attachCallback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }
        synchronized (this.mLock) {
            try {
                Looper mainLooper = handler == null ? Looper.getMainLooper() : handler.getLooper();
                try {
                    Binder binder = new Binder();
                    this.mService.connect(binder, this.mContext.getOpPackageName(), new WifiAwareEventCallbackProxy(this, mainLooper, binder, attachCallback, identityChangedListener), configRequest, identityChangedListener != null);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void disconnect(int i, Binder binder) {
        try {
            this.mService.disconnect(i, binder);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void publish(int i, Looper looper, PublishConfig publishConfig, DiscoverySessionCallback discoverySessionCallback) {
        if (discoverySessionCallback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }
        try {
            this.mService.publish(this.mContext.getOpPackageName(), i, publishConfig, new WifiAwareDiscoverySessionCallbackProxy(this, looper, true, discoverySessionCallback, i));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updatePublish(int i, int i2, PublishConfig publishConfig) {
        try {
            this.mService.updatePublish(i, i2, publishConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void subscribe(int i, Looper looper, SubscribeConfig subscribeConfig, DiscoverySessionCallback discoverySessionCallback) {
        if (discoverySessionCallback == null) {
            throw new IllegalArgumentException("Null callback provided");
        }
        try {
            this.mService.subscribe(this.mContext.getOpPackageName(), i, subscribeConfig, new WifiAwareDiscoverySessionCallbackProxy(this, looper, false, discoverySessionCallback, i));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void updateSubscribe(int i, int i2, SubscribeConfig subscribeConfig) {
        try {
            this.mService.updateSubscribe(i, i2, subscribeConfig);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void terminateSession(int i, int i2) {
        try {
            this.mService.terminateSession(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void sendMessage(int i, int i2, PeerHandle peerHandle, byte[] bArr, int i3, int i4) {
        if (peerHandle == null) {
            throw new IllegalArgumentException("sendMessage: invalid peerHandle - must be non-null");
        }
        try {
            this.mService.sendMessage(i, i2, peerHandle.peerId, bArr, i3, i4);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkSpecifier createNetworkSpecifier(int i, int i2, int i3, PeerHandle peerHandle, byte[] bArr, String str) {
        int i4 = 1;
        if (i2 != 0 && i2 != 1) {
            throw new IllegalArgumentException("createNetworkSpecifier: Invalid 'role' argument when creating a network specifier");
        }
        if ((i2 == 0 || !WifiAwareUtils.isLegacyVersion(this.mContext, 28)) && peerHandle == null) {
            throw new IllegalArgumentException("createNetworkSpecifier: Invalid peer handle - cannot be null");
        }
        if (peerHandle != null) {
            i4 = 0;
        }
        return new WifiAwareNetworkSpecifier(i4, i2, i, i3, peerHandle != null ? peerHandle.peerId : 0, null, bArr, str, Process.myUid());
    }

    public NetworkSpecifier createNetworkSpecifier(int i, int i2, byte[] bArr, byte[] bArr2, String str) {
        if (i2 != 0 && i2 != 1) {
            throw new IllegalArgumentException("createNetworkSpecifier: Invalid 'role' argument when creating a network specifier");
        }
        if ((i2 == 0 || !WifiAwareUtils.isLegacyVersion(this.mContext, 28)) && bArr == null) {
            throw new IllegalArgumentException("createNetworkSpecifier: Invalid peer MAC - cannot be null");
        }
        if (bArr != null && bArr.length != 6) {
            throw new IllegalArgumentException("createNetworkSpecifier: Invalid peer MAC address");
        }
        return new WifiAwareNetworkSpecifier(bArr == null ? 3 : 2, i2, i, 0, 0, bArr, bArr2, str, Process.myUid());
    }

    private static class WifiAwareEventCallbackProxy extends IWifiAwareEventCallback.Stub {
        private static final int CALLBACK_CONNECT_FAIL = 1;
        private static final int CALLBACK_CONNECT_SUCCESS = 0;
        private static final int CALLBACK_IDENTITY_CHANGED = 2;
        private final WeakReference<WifiAwareManager> mAwareManager;
        private final Binder mBinder;
        private final Handler mHandler;
        private final Looper mLooper;

        WifiAwareEventCallbackProxy(WifiAwareManager wifiAwareManager, Looper looper, Binder binder, final AttachCallback attachCallback, final IdentityChangedListener identityChangedListener) {
            this.mAwareManager = new WeakReference<>(wifiAwareManager);
            this.mLooper = looper;
            this.mBinder = binder;
            this.mHandler = new Handler(looper) {
                @Override
                public void handleMessage(Message message) {
                    WifiAwareManager wifiAwareManager2 = (WifiAwareManager) WifiAwareEventCallbackProxy.this.mAwareManager.get();
                    if (wifiAwareManager2 == null) {
                        Log.w(WifiAwareManager.TAG, "WifiAwareEventCallbackProxy: handleMessage post GC");
                    }
                    switch (message.what) {
                        case 0:
                            attachCallback.onAttached(new WifiAwareSession(wifiAwareManager2, WifiAwareEventCallbackProxy.this.mBinder, message.arg1));
                            break;
                        case 1:
                            WifiAwareEventCallbackProxy.this.mAwareManager.clear();
                            attachCallback.onAttachFailed();
                            break;
                        case 2:
                            if (identityChangedListener == null) {
                                Log.e(WifiAwareManager.TAG, "CALLBACK_IDENTITY_CHANGED: null listener.");
                            } else {
                                identityChangedListener.onIdentityChanged((byte[]) message.obj);
                            }
                            break;
                    }
                }
            };
        }

        @Override
        public void onConnectSuccess(int i) {
            Message messageObtainMessage = this.mHandler.obtainMessage(0);
            messageObtainMessage.arg1 = i;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onConnectFail(int i) {
            Message messageObtainMessage = this.mHandler.obtainMessage(1);
            messageObtainMessage.arg1 = i;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onIdentityChanged(byte[] bArr) {
            Message messageObtainMessage = this.mHandler.obtainMessage(2);
            messageObtainMessage.obj = bArr;
            this.mHandler.sendMessage(messageObtainMessage);
        }
    }

    private static class WifiAwareDiscoverySessionCallbackProxy extends IWifiAwareDiscoverySessionCallback.Stub {
        private static final int CALLBACK_MATCH = 4;
        private static final int CALLBACK_MATCH_WITH_DISTANCE = 8;
        private static final int CALLBACK_MESSAGE_RECEIVED = 7;
        private static final int CALLBACK_MESSAGE_SEND_FAIL = 6;
        private static final int CALLBACK_MESSAGE_SEND_SUCCESS = 5;
        private static final int CALLBACK_SESSION_CONFIG_FAIL = 2;
        private static final int CALLBACK_SESSION_CONFIG_SUCCESS = 1;
        private static final int CALLBACK_SESSION_STARTED = 0;
        private static final int CALLBACK_SESSION_TERMINATED = 3;
        private static final String MESSAGE_BUNDLE_KEY_MESSAGE = "message";
        private static final String MESSAGE_BUNDLE_KEY_MESSAGE2 = "message2";
        private final WeakReference<WifiAwareManager> mAwareManager;
        private final int mClientId;
        private final Handler mHandler;
        private final boolean mIsPublish;
        private final DiscoverySessionCallback mOriginalCallback;
        private DiscoverySession mSession;

        WifiAwareDiscoverySessionCallbackProxy(WifiAwareManager wifiAwareManager, Looper looper, boolean z, DiscoverySessionCallback discoverySessionCallback, int i) {
            this.mAwareManager = new WeakReference<>(wifiAwareManager);
            this.mIsPublish = z;
            this.mOriginalCallback = discoverySessionCallback;
            this.mClientId = i;
            this.mHandler = new Handler(looper) {
                @Override
                public void handleMessage(Message message) {
                    List<byte[]> list;
                    if (WifiAwareDiscoverySessionCallbackProxy.this.mAwareManager.get() == null) {
                        Log.w(WifiAwareManager.TAG, "WifiAwareDiscoverySessionCallbackProxy: handleMessage post GC");
                    }
                    switch (message.what) {
                        case 0:
                            WifiAwareDiscoverySessionCallbackProxy.this.onProxySessionStarted(message.arg1);
                            break;
                        case 1:
                            WifiAwareDiscoverySessionCallbackProxy.this.mOriginalCallback.onSessionConfigUpdated();
                            break;
                        case 2:
                            WifiAwareDiscoverySessionCallbackProxy.this.mOriginalCallback.onSessionConfigFailed();
                            if (WifiAwareDiscoverySessionCallbackProxy.this.mSession == null) {
                                WifiAwareDiscoverySessionCallbackProxy.this.mAwareManager.clear();
                            }
                            break;
                        case 3:
                            WifiAwareDiscoverySessionCallbackProxy.this.onProxySessionTerminated(message.arg1);
                            break;
                        case 4:
                        case 8:
                            byte[] byteArray = message.getData().getByteArray(WifiAwareDiscoverySessionCallbackProxy.MESSAGE_BUNDLE_KEY_MESSAGE2);
                            try {
                                list = new TlvBufferUtils.TlvIterable(0, 1, byteArray).toList();
                            } catch (BufferOverflowException e) {
                                Log.e(WifiAwareManager.TAG, "onServiceDiscovered: invalid match filter byte array '" + new String(HexEncoding.encode(byteArray)) + "' - cannot be parsed: e=" + e);
                                list = null;
                            }
                            if (message.what == 4) {
                                WifiAwareDiscoverySessionCallbackProxy.this.mOriginalCallback.onServiceDiscovered(new PeerHandle(message.arg1), message.getData().getByteArray("message"), list);
                            } else {
                                WifiAwareDiscoverySessionCallbackProxy.this.mOriginalCallback.onServiceDiscoveredWithinRange(new PeerHandle(message.arg1), message.getData().getByteArray("message"), list, message.arg2);
                            }
                            break;
                        case 5:
                            WifiAwareDiscoverySessionCallbackProxy.this.mOriginalCallback.onMessageSendSucceeded(message.arg1);
                            break;
                        case 6:
                            WifiAwareDiscoverySessionCallbackProxy.this.mOriginalCallback.onMessageSendFailed(message.arg1);
                            break;
                        case 7:
                            WifiAwareDiscoverySessionCallbackProxy.this.mOriginalCallback.onMessageReceived(new PeerHandle(message.arg1), (byte[]) message.obj);
                            break;
                    }
                }
            };
        }

        @Override
        public void onSessionStarted(int i) {
            Message messageObtainMessage = this.mHandler.obtainMessage(0);
            messageObtainMessage.arg1 = i;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onSessionConfigSuccess() {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
        }

        @Override
        public void onSessionConfigFail(int i) {
            Message messageObtainMessage = this.mHandler.obtainMessage(2);
            messageObtainMessage.arg1 = i;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onSessionTerminated(int i) {
            Message messageObtainMessage = this.mHandler.obtainMessage(3);
            messageObtainMessage.arg1 = i;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        private void onMatchCommon(int i, int i2, byte[] bArr, byte[] bArr2, int i3) {
            Bundle bundle = new Bundle();
            bundle.putByteArray("message", bArr);
            bundle.putByteArray(MESSAGE_BUNDLE_KEY_MESSAGE2, bArr2);
            Message messageObtainMessage = this.mHandler.obtainMessage(i);
            messageObtainMessage.arg1 = i2;
            messageObtainMessage.arg2 = i3;
            messageObtainMessage.setData(bundle);
            this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onMatch(int i, byte[] bArr, byte[] bArr2) {
            onMatchCommon(4, i, bArr, bArr2, 0);
        }

        @Override
        public void onMatchWithDistance(int i, byte[] bArr, byte[] bArr2, int i2) {
            onMatchCommon(8, i, bArr, bArr2, i2);
        }

        @Override
        public void onMessageSendSuccess(int i) {
            Message messageObtainMessage = this.mHandler.obtainMessage(5);
            messageObtainMessage.arg1 = i;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onMessageSendFail(int i, int i2) {
            Message messageObtainMessage = this.mHandler.obtainMessage(6);
            messageObtainMessage.arg1 = i;
            messageObtainMessage.arg2 = i2;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        @Override
        public void onMessageReceived(int i, byte[] bArr) {
            Message messageObtainMessage = this.mHandler.obtainMessage(7);
            messageObtainMessage.arg1 = i;
            messageObtainMessage.obj = bArr;
            this.mHandler.sendMessage(messageObtainMessage);
        }

        public void onProxySessionStarted(int i) {
            if (this.mSession != null) {
                Log.e(WifiAwareManager.TAG, "onSessionStarted: sessionId=" + i + ": session already created!?");
                throw new IllegalStateException("onSessionStarted: sessionId=" + i + ": session already created!?");
            }
            WifiAwareManager wifiAwareManager = this.mAwareManager.get();
            if (wifiAwareManager == null) {
                Log.w(WifiAwareManager.TAG, "onProxySessionStarted: mgr GC'd");
                return;
            }
            if (this.mIsPublish) {
                PublishDiscoverySession publishDiscoverySession = new PublishDiscoverySession(wifiAwareManager, this.mClientId, i);
                this.mSession = publishDiscoverySession;
                this.mOriginalCallback.onPublishStarted(publishDiscoverySession);
            } else {
                SubscribeDiscoverySession subscribeDiscoverySession = new SubscribeDiscoverySession(wifiAwareManager, this.mClientId, i);
                this.mSession = subscribeDiscoverySession;
                this.mOriginalCallback.onSubscribeStarted(subscribeDiscoverySession);
            }
        }

        public void onProxySessionTerminated(int i) {
            if (this.mSession != null) {
                this.mSession.setTerminated();
                this.mSession = null;
            } else {
                Log.w(WifiAwareManager.TAG, "Proxy: onSessionTerminated called but mSession is null!?");
            }
            this.mAwareManager.clear();
            this.mOriginalCallback.onSessionTerminated();
        }
    }
}
