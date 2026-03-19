package com.android.server.wifi.hotspot2;

import android.content.Context;
import android.net.Network;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.android.server.wifi.hotspot2.OsuNetworkConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PasspointProvisioner {
    private static final int PROVISIONING_FAILURE = 1;
    private static final int PROVISIONING_STATUS = 0;
    private static final String TAG = "PasspointProvisioner";
    private static final String TLS_VERSION = "TLSv1";
    private int mCallingUid;
    private final Context mContext;
    private final PasspointObjectFactory mObjectFactory;
    private final OsuNetworkConnection mOsuNetworkConnection;
    private final OsuServerConnection mOsuServerConnection;
    private final WfaKeyStore mWfaKeyStore;
    private int mCurrentSessionId = 0;
    private boolean mVerboseLoggingEnabled = false;
    private final ProvisioningStateMachine mProvisioningStateMachine = new ProvisioningStateMachine();
    private final OsuNetworkCallbacks mOsuNetworkCallbacks = new OsuNetworkCallbacks();

    static int access$404(PasspointProvisioner passpointProvisioner) {
        int i = passpointProvisioner.mCurrentSessionId + 1;
        passpointProvisioner.mCurrentSessionId = i;
        return i;
    }

    PasspointProvisioner(Context context, PasspointObjectFactory passpointObjectFactory) {
        this.mContext = context;
        this.mOsuNetworkConnection = passpointObjectFactory.makeOsuNetworkConnection(context);
        this.mOsuServerConnection = passpointObjectFactory.makeOsuServerConnection();
        this.mWfaKeyStore = passpointObjectFactory.makeWfaKeyStore();
        this.mObjectFactory = passpointObjectFactory;
    }

    public void init(Looper looper) {
        this.mProvisioningStateMachine.start(new Handler(looper));
        this.mOsuNetworkConnection.init(this.mProvisioningStateMachine.getHandler());
        this.mProvisioningStateMachine.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                PasspointProvisioner.lambda$init$0(this.f$0);
            }
        });
    }

    public static void lambda$init$0(PasspointProvisioner passpointProvisioner) {
        passpointProvisioner.mWfaKeyStore.load();
        passpointProvisioner.mOsuServerConnection.init(passpointProvisioner.mObjectFactory.getSSLContext(TLS_VERSION), passpointProvisioner.mObjectFactory.getTrustManagerImpl(passpointProvisioner.mWfaKeyStore.get()));
    }

    public void enableVerboseLogging(int i) {
        this.mVerboseLoggingEnabled = i > 0;
        this.mOsuNetworkConnection.enableVerboseLogging(i);
        this.mOsuServerConnection.enableVerboseLogging(i);
    }

    public boolean startSubscriptionProvisioning(int i, final OsuProvider osuProvider, final IProvisioningCallback iProvisioningCallback) {
        this.mCallingUid = i;
        Log.v(TAG, "Provisioning started with " + osuProvider.toString());
        this.mProvisioningStateMachine.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mProvisioningStateMachine.startProvisioning(osuProvider, iProvisioningCallback);
            }
        });
        return true;
    }

    class ProvisioningStateMachine {
        private static final int INITIAL_STATE = 1;
        private static final int OSU_AP_CONNECTED = 3;
        private static final int OSU_PROVIDER_VERIFIED = 6;
        private static final int OSU_SERVER_CONNECTED = 4;
        private static final int OSU_SERVER_VALIDATED = 5;
        private static final String TAG = "ProvisioningStateMachine";
        private static final int WAITING_TO_CONNECT = 2;
        private Handler mHandler;
        private OsuProvider mOsuProvider;
        private IProvisioningCallback mProvisioningCallback;
        private URL mServerUrl;
        private int mState = 1;

        ProvisioningStateMachine() {
        }

        public void start(Handler handler) {
            this.mHandler = handler;
        }

        public Handler getHandler() {
            return this.mHandler;
        }

        public void startProvisioning(OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "startProvisioning received in state=" + this.mState);
            }
            if (this.mState != 1) {
                if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "State Machine needs to be reset before starting provisioning");
                }
                resetStateMachine(6);
            }
            if (!PasspointProvisioner.this.mOsuServerConnection.canValidateServer()) {
                Log.w(TAG, "Provisioning is not possible");
                this.mProvisioningCallback = iProvisioningCallback;
                resetStateMachine(7);
                return;
            }
            try {
                this.mServerUrl = new URL(osuProvider.getServerUri().toString());
                this.mProvisioningCallback = iProvisioningCallback;
                this.mOsuProvider = osuProvider;
                PasspointProvisioner.this.mOsuNetworkConnection.setEventCallback(PasspointProvisioner.this.mOsuNetworkCallbacks);
                PasspointProvisioner.this.mOsuServerConnection.setEventCallback(PasspointProvisioner.this.new OsuServerCallbacks(PasspointProvisioner.access$404(PasspointProvisioner.this)));
                if (!PasspointProvisioner.this.mOsuNetworkConnection.connect(this.mOsuProvider.getOsuSsid(), this.mOsuProvider.getNetworkAccessIdentifier())) {
                    resetStateMachine(1);
                } else {
                    invokeProvisioningCallback(0, 1);
                    changeState(2);
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "Invalid Server URL");
                this.mProvisioningCallback = iProvisioningCallback;
                resetStateMachine(2);
            }
        }

        public void handleWifiDisabled() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Wifi Disabled in state=" + this.mState);
            }
            if (this.mState == 1) {
                Log.w(TAG, "Wifi Disable unhandled in state=" + this.mState);
                return;
            }
            resetStateMachine(1);
        }

        public void handleServerValidationFailure(int i) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Server Validation failure received in " + this.mState);
            }
            if (i != PasspointProvisioner.this.mCurrentSessionId) {
                Log.w(TAG, "Expected server validation callback for currentSessionId=" + PasspointProvisioner.this.mCurrentSessionId);
                return;
            }
            if (this.mState != 4) {
                Log.wtf(TAG, "Server Validation Failure unhandled in mState=" + this.mState);
                return;
            }
            resetStateMachine(4);
        }

        public void handleServerValidationSuccess(int i) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Server Validation Success received in " + this.mState);
            }
            if (i != PasspointProvisioner.this.mCurrentSessionId) {
                Log.w(TAG, "Expected server validation callback for currentSessionId=" + PasspointProvisioner.this.mCurrentSessionId);
                return;
            }
            if (this.mState != 4) {
                Log.wtf(TAG, "Server validation success event unhandled in state=" + this.mState);
                return;
            }
            changeState(5);
            invokeProvisioningCallback(0, 4);
            validateProvider();
        }

        private void validateProvider() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Validating provider in state=" + this.mState);
            }
            if (!PasspointProvisioner.this.mOsuServerConnection.validateProvider(this.mOsuProvider.getFriendlyName())) {
                resetStateMachine(5);
            } else {
                changeState(6);
                invokeProvisioningCallback(0, 5);
            }
        }

        public void handleConnectedEvent(Network network) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Connected event received in state=" + this.mState);
            }
            if (this.mState != 2) {
                Log.wtf(TAG, "Connection event unhandled in state=" + this.mState);
                return;
            }
            invokeProvisioningCallback(0, 2);
            changeState(3);
            initiateServerConnection(network);
        }

        private void initiateServerConnection(Network network) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Initiating server connection in state=" + this.mState);
            }
            if (this.mState == 3) {
                if (!PasspointProvisioner.this.mOsuServerConnection.connect(this.mServerUrl, network)) {
                    resetStateMachine(3);
                    return;
                } else {
                    changeState(4);
                    invokeProvisioningCallback(0, 3);
                    return;
                }
            }
            Log.wtf(TAG, "Initiating server connection aborted in invalid state=" + this.mState);
        }

        public void handleDisconnect() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Connection failed in state=" + this.mState);
            }
            if (this.mState == 1) {
                Log.w(TAG, "Disconnect event unhandled in state=" + this.mState);
                return;
            }
            resetStateMachine(1);
        }

        private void invokeProvisioningCallback(int i, int i2) {
            if (this.mProvisioningCallback == null) {
                Log.e(TAG, "Provisioning callback " + i + " with status " + i2 + " not invoked");
                return;
            }
            try {
                if (i == 0) {
                    this.mProvisioningCallback.onProvisioningStatus(i2);
                } else {
                    this.mProvisioningCallback.onProvisioningFailure(i2);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Remote Exception while posting callback type=" + i + " status=" + i2);
            }
        }

        private void changeState(int i) {
            if (i != this.mState) {
                if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                    Log.v(TAG, "Changing state from " + this.mState + " -> " + i);
                }
                this.mState = i;
            }
        }

        private void resetStateMachine(int i) {
            invokeProvisioningCallback(1, i);
            PasspointProvisioner.this.mOsuNetworkConnection.setEventCallback(null);
            PasspointProvisioner.this.mOsuNetworkConnection.disconnectIfNeeded();
            PasspointProvisioner.this.mOsuServerConnection.setEventCallback(null);
            PasspointProvisioner.this.mOsuServerConnection.cleanup();
            changeState(1);
        }
    }

    class OsuNetworkCallbacks implements OsuNetworkConnection.Callbacks {
        OsuNetworkCallbacks() {
        }

        @Override
        public void onConnected(Network network) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "onConnected to " + network);
            }
            if (network == null) {
                PasspointProvisioner.this.mProvisioningStateMachine.handleDisconnect();
            } else {
                PasspointProvisioner.this.mProvisioningStateMachine.handleConnectedEvent(network);
            }
        }

        @Override
        public void onDisconnected() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "onDisconnected");
            }
            PasspointProvisioner.this.mProvisioningStateMachine.handleDisconnect();
        }

        @Override
        public void onTimeOut() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "Timed out waiting for connection to OSU AP");
            }
            PasspointProvisioner.this.mProvisioningStateMachine.handleDisconnect();
        }

        @Override
        public void onWifiEnabled() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "onWifiEnabled");
            }
        }

        @Override
        public void onWifiDisabled() {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "onWifiDisabled");
            }
            PasspointProvisioner.this.mProvisioningStateMachine.handleWifiDisabled();
        }
    }

    public class OsuServerCallbacks {
        private final int mSessionId;

        OsuServerCallbacks(int i) {
            this.mSessionId = i;
        }

        public int getSessionId() {
            return this.mSessionId;
        }

        public void onServerValidationStatus(final int i, boolean z) {
            if (PasspointProvisioner.this.mVerboseLoggingEnabled) {
                Log.v(PasspointProvisioner.TAG, "OSU Server Validation status=" + z + " sessionId=" + i);
            }
            if (z) {
                PasspointProvisioner.this.mProvisioningStateMachine.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        PasspointProvisioner.this.mProvisioningStateMachine.handleServerValidationSuccess(i);
                    }
                });
            } else {
                PasspointProvisioner.this.mProvisioningStateMachine.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        PasspointProvisioner.this.mProvisioningStateMachine.handleServerValidationFailure(i);
                    }
                });
            }
        }
    }
}
