package android.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IIpSecService;
import android.net.IpSecManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;

public final class IpSecTransform implements AutoCloseable {
    public static final int ENCAP_ESPINUDP = 2;
    public static final int ENCAP_ESPINUDP_NON_IKE = 1;
    public static final int ENCAP_NONE = 0;
    public static final int MODE_TRANSPORT = 0;
    public static final int MODE_TUNNEL = 1;
    private static final String TAG = "IpSecTransform";
    private Handler mCallbackHandler;
    private final IpSecConfig mConfig;
    private final Context mContext;
    private ConnectivityManager.PacketKeepalive mKeepalive;
    private NattKeepaliveCallback mUserKeepaliveCallback;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final ConnectivityManager.PacketKeepaliveCallback mKeepaliveCallback = new AnonymousClass1();
    private int mResourceId = -1;

    @Retention(RetentionPolicy.SOURCE)
    public @interface EncapType {
    }

    @VisibleForTesting
    public IpSecTransform(Context context, IpSecConfig ipSecConfig) {
        this.mContext = context;
        this.mConfig = new IpSecConfig(ipSecConfig);
    }

    private IIpSecService getIpSecService() {
        IBinder service = ServiceManager.getService("ipsec");
        if (service == null) {
            throw new RemoteException("Failed to connect to IpSecService").rethrowAsRuntimeException();
        }
        return IIpSecService.Stub.asInterface(service);
    }

    private void checkResultStatus(int i) throws IpSecManager.ResourceUnavailableException, IpSecManager.SpiUnavailableException, IOException {
        switch (i) {
            case 0:
                return;
            case 1:
                throw new IpSecManager.ResourceUnavailableException("Failed to allocate a new IpSecTransform");
            case 2:
                Log.wtf(TAG, "Attempting to use an SPI that was somehow not reserved");
                break;
        }
        throw new IllegalStateException("Failed to Create a Transform with status code " + i);
    }

    private IpSecTransform activate() throws IpSecManager.ResourceUnavailableException, IpSecManager.SpiUnavailableException, IOException {
        synchronized (this) {
            try {
                try {
                    try {
                        IpSecTransformResponse ipSecTransformResponseCreateTransform = getIpSecService().createTransform(this.mConfig, new Binder(), this.mContext.getOpPackageName());
                        checkResultStatus(ipSecTransformResponseCreateTransform.status);
                        this.mResourceId = ipSecTransformResponseCreateTransform.resourceId;
                        Log.d(TAG, "Added Transform with Id " + this.mResourceId);
                        this.mCloseGuard.open("build");
                    } catch (RemoteException e) {
                        throw e.rethrowAsRuntimeException();
                    }
                } catch (ServiceSpecificException e2) {
                    throw IpSecManager.rethrowUncheckedExceptionFromServiceSpecificException(e2);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return this;
    }

    @VisibleForTesting
    public static boolean equals(IpSecTransform ipSecTransform, IpSecTransform ipSecTransform2) {
        return (ipSecTransform == null || ipSecTransform2 == null) ? ipSecTransform == ipSecTransform2 : IpSecConfig.equals(ipSecTransform.getConfig(), ipSecTransform2.getConfig()) && ipSecTransform.mResourceId == ipSecTransform2.mResourceId;
    }

    @Override
    public void close() {
        Log.d(TAG, "Removing Transform with Id " + this.mResourceId);
        try {
            if (this.mResourceId == -1) {
                this.mCloseGuard.close();
                return;
            }
            try {
                getIpSecService().deleteTransform(this.mResourceId);
                stopNattKeepalive();
            } catch (RemoteException e) {
                throw e.rethrowAsRuntimeException();
            } catch (Exception e2) {
                Log.e(TAG, "Failed to close " + this + ", Exception=" + e2);
            }
        } finally {
            this.mResourceId = -1;
            this.mCloseGuard.close();
        }
    }

    protected void finalize() throws Throwable {
        if (this.mCloseGuard != null) {
            this.mCloseGuard.warnIfOpen();
        }
        close();
    }

    IpSecConfig getConfig() {
        return this.mConfig;
    }

    class AnonymousClass1 extends ConnectivityManager.PacketKeepaliveCallback {
        AnonymousClass1() {
        }

        @Override
        public void onStarted() {
            synchronized (this) {
                IpSecTransform.this.mCallbackHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        IpSecTransform.this.mUserKeepaliveCallback.onStarted();
                    }
                });
            }
        }

        @Override
        public void onStopped() {
            synchronized (this) {
                IpSecTransform.this.mKeepalive = null;
                IpSecTransform.this.mCallbackHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        IpSecTransform.this.mUserKeepaliveCallback.onStopped();
                    }
                });
            }
        }

        @Override
        public void onError(final int i) {
            synchronized (this) {
                IpSecTransform.this.mKeepalive = null;
                IpSecTransform.this.mCallbackHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        IpSecTransform.this.mUserKeepaliveCallback.onError(i);
                    }
                });
            }
        }
    }

    @VisibleForTesting
    public int getResourceId() {
        return this.mResourceId;
    }

    public static class NattKeepaliveCallback {
        public static final int ERROR_HARDWARE_ERROR = 3;
        public static final int ERROR_HARDWARE_UNSUPPORTED = 2;
        public static final int ERROR_INVALID_NETWORK = 1;

        public void onStarted() {
        }

        public void onStopped() {
        }

        public void onError(int i) {
        }
    }

    public void startNattKeepalive(NattKeepaliveCallback nattKeepaliveCallback, int i, Handler handler) throws IOException {
        Preconditions.checkNotNull(nattKeepaliveCallback);
        if (i < 20 || i > 3600) {
            throw new IllegalArgumentException("Invalid NAT-T keepalive interval");
        }
        Preconditions.checkNotNull(handler);
        if (this.mResourceId == -1) {
            throw new IllegalStateException("Packet keepalive cannot be started for an inactive transform");
        }
        synchronized (this.mKeepaliveCallback) {
            if (this.mKeepaliveCallback != null) {
                throw new IllegalStateException("Keepalive already active");
            }
            this.mUserKeepaliveCallback = nattKeepaliveCallback;
            this.mKeepalive = ((ConnectivityManager) this.mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).startNattKeepalive(this.mConfig.getNetwork(), i, this.mKeepaliveCallback, NetworkUtils.numericToInetAddress(this.mConfig.getSourceAddress()), ConnectivityManager.PacketKeepalive.NATT_PORT, NetworkUtils.numericToInetAddress(this.mConfig.getDestinationAddress()));
            this.mCallbackHandler = handler;
        }
    }

    public void stopNattKeepalive() {
        synchronized (this.mKeepaliveCallback) {
            if (this.mKeepalive == null) {
                Log.e(TAG, "No active keepalive to stop");
            } else {
                this.mKeepalive.stop();
            }
        }
    }

    public static class Builder {
        private IpSecConfig mConfig;
        private Context mContext;

        public Builder setEncryption(IpSecAlgorithm ipSecAlgorithm) {
            Preconditions.checkNotNull(ipSecAlgorithm);
            this.mConfig.setEncryption(ipSecAlgorithm);
            return this;
        }

        public Builder setAuthentication(IpSecAlgorithm ipSecAlgorithm) {
            Preconditions.checkNotNull(ipSecAlgorithm);
            this.mConfig.setAuthentication(ipSecAlgorithm);
            return this;
        }

        public Builder setAuthenticatedEncryption(IpSecAlgorithm ipSecAlgorithm) {
            Preconditions.checkNotNull(ipSecAlgorithm);
            this.mConfig.setAuthenticatedEncryption(ipSecAlgorithm);
            return this;
        }

        public Builder setIpv4Encapsulation(IpSecManager.UdpEncapsulationSocket udpEncapsulationSocket, int i) {
            Preconditions.checkNotNull(udpEncapsulationSocket);
            this.mConfig.setEncapType(2);
            if (udpEncapsulationSocket.getResourceId() == -1) {
                throw new IllegalArgumentException("Invalid UdpEncapsulationSocket");
            }
            this.mConfig.setEncapSocketResourceId(udpEncapsulationSocket.getResourceId());
            this.mConfig.setEncapRemotePort(i);
            return this;
        }

        public IpSecTransform buildTransportModeTransform(InetAddress inetAddress, IpSecManager.SecurityParameterIndex securityParameterIndex) throws IpSecManager.ResourceUnavailableException, IpSecManager.SpiUnavailableException, IOException {
            Preconditions.checkNotNull(inetAddress);
            Preconditions.checkNotNull(securityParameterIndex);
            if (securityParameterIndex.getResourceId() == -1) {
                throw new IllegalArgumentException("Invalid SecurityParameterIndex");
            }
            this.mConfig.setMode(0);
            this.mConfig.setSourceAddress(inetAddress.getHostAddress());
            this.mConfig.setSpiResourceId(securityParameterIndex.getResourceId());
            return new IpSecTransform(this.mContext, this.mConfig).activate();
        }

        public IpSecTransform buildTunnelModeTransform(InetAddress inetAddress, IpSecManager.SecurityParameterIndex securityParameterIndex) throws IpSecManager.ResourceUnavailableException, IpSecManager.SpiUnavailableException, IOException {
            Preconditions.checkNotNull(inetAddress);
            Preconditions.checkNotNull(securityParameterIndex);
            if (securityParameterIndex.getResourceId() == -1) {
                throw new IllegalArgumentException("Invalid SecurityParameterIndex");
            }
            this.mConfig.setMode(1);
            this.mConfig.setSourceAddress(inetAddress.getHostAddress());
            this.mConfig.setSpiResourceId(securityParameterIndex.getResourceId());
            return new IpSecTransform(this.mContext, this.mConfig).activate();
        }

        public Builder(Context context) {
            Preconditions.checkNotNull(context);
            this.mContext = context;
            this.mConfig = new IpSecConfig();
        }
    }

    public String toString() {
        return "IpSecTransform{resourceId=" + this.mResourceId + "}";
    }
}
