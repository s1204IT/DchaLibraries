package android.net;

import android.content.Context;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.AndroidException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import dalvik.system.CloseGuard;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public final class IpSecManager {
    public static final int DIRECTION_IN = 0;
    public static final int DIRECTION_OUT = 1;
    public static final int INVALID_RESOURCE_ID = -1;
    public static final int INVALID_SECURITY_PARAMETER_INDEX = 0;
    private static final String TAG = "IpSecManager";
    private final Context mContext;
    private final IIpSecService mService;

    @Retention(RetentionPolicy.SOURCE)
    public @interface PolicyDirection {
    }

    public interface Status {
        public static final int OK = 0;
        public static final int RESOURCE_UNAVAILABLE = 1;
        public static final int SPI_UNAVAILABLE = 2;
    }

    public static final class SpiUnavailableException extends AndroidException {
        private final int mSpi;

        SpiUnavailableException(String str, int i) {
            super(str + " (spi: " + i + ")");
            this.mSpi = i;
        }

        public int getSpi() {
            return this.mSpi;
        }
    }

    public static final class ResourceUnavailableException extends AndroidException {
        ResourceUnavailableException(String str) {
            super(str);
        }
    }

    public static final class SecurityParameterIndex implements AutoCloseable {
        private final CloseGuard mCloseGuard;
        private final InetAddress mDestinationAddress;
        private int mResourceId;
        private final IIpSecService mService;
        private int mSpi;

        public int getSpi() {
            return this.mSpi;
        }

        @Override
        public void close() {
            try {
                try {
                    try {
                        this.mService.releaseSecurityParameterIndex(this.mResourceId);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                } catch (Exception e2) {
                    Log.e(IpSecManager.TAG, "Failed to close " + this + ", Exception=" + e2);
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

        private SecurityParameterIndex(IIpSecService iIpSecService, InetAddress inetAddress, int i) throws ResourceUnavailableException, SpiUnavailableException {
            this.mCloseGuard = CloseGuard.get();
            this.mSpi = 0;
            this.mResourceId = -1;
            this.mService = iIpSecService;
            this.mDestinationAddress = inetAddress;
            try {
                IpSecSpiResponse ipSecSpiResponseAllocateSecurityParameterIndex = this.mService.allocateSecurityParameterIndex(inetAddress.getHostAddress(), i, new Binder());
                if (ipSecSpiResponseAllocateSecurityParameterIndex == null) {
                    throw new NullPointerException("Received null response from IpSecService");
                }
                int i2 = ipSecSpiResponseAllocateSecurityParameterIndex.status;
                switch (i2) {
                    case 0:
                        this.mSpi = ipSecSpiResponseAllocateSecurityParameterIndex.spi;
                        this.mResourceId = ipSecSpiResponseAllocateSecurityParameterIndex.resourceId;
                        if (this.mSpi != 0) {
                            if (this.mResourceId == -1) {
                                throw new RuntimeException("Invalid Resource ID returned by IpSecService: " + i2);
                            }
                            this.mCloseGuard.open("open");
                            return;
                        }
                        throw new RuntimeException("Invalid SPI returned by IpSecService: " + i2);
                    case 1:
                        throw new ResourceUnavailableException("No more SPIs may be allocated by this requester.");
                    case 2:
                        throw new SpiUnavailableException("Requested SPI is unavailable", i);
                    default:
                        throw new RuntimeException("Unknown status returned by IpSecService: " + i2);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @VisibleForTesting
        public int getResourceId() {
            return this.mResourceId;
        }

        public String toString() {
            return "SecurityParameterIndex{spi=" + this.mSpi + ",resourceId=" + this.mResourceId + "}";
        }
    }

    public SecurityParameterIndex allocateSecurityParameterIndex(InetAddress inetAddress) throws ResourceUnavailableException {
        try {
            return new SecurityParameterIndex(this.mService, inetAddress, 0);
        } catch (SpiUnavailableException e) {
            throw new ResourceUnavailableException("No SPIs available");
        } catch (ServiceSpecificException e2) {
            throw rethrowUncheckedExceptionFromServiceSpecificException(e2);
        }
    }

    public SecurityParameterIndex allocateSecurityParameterIndex(InetAddress inetAddress, int i) throws ResourceUnavailableException, SpiUnavailableException {
        if (i == 0) {
            throw new IllegalArgumentException("Requested SPI must be a valid (non-zero) SPI");
        }
        try {
            return new SecurityParameterIndex(this.mService, inetAddress, i);
        } catch (ServiceSpecificException e) {
            throw rethrowUncheckedExceptionFromServiceSpecificException(e);
        }
    }

    public void applyTransportModeTransform(Socket socket, int i, IpSecTransform ipSecTransform) throws Exception {
        socket.getSoLinger();
        applyTransportModeTransform(socket.getFileDescriptor$(), i, ipSecTransform);
    }

    public void applyTransportModeTransform(DatagramSocket datagramSocket, int i, IpSecTransform ipSecTransform) throws Exception {
        applyTransportModeTransform(datagramSocket.getFileDescriptor$(), i, ipSecTransform);
    }

    public void applyTransportModeTransform(FileDescriptor fileDescriptor, int i, IpSecTransform ipSecTransform) throws Exception {
        try {
            ParcelFileDescriptor parcelFileDescriptorDup = ParcelFileDescriptor.dup(fileDescriptor);
            try {
                this.mService.applyTransportModeTransform(parcelFileDescriptorDup, i, ipSecTransform.getResourceId());
            } finally {
                if (parcelFileDescriptorDup != null) {
                    $closeResource(null, parcelFileDescriptorDup);
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e2);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public void removeTransportModeTransforms(Socket socket) throws Exception {
        socket.getSoLinger();
        removeTransportModeTransforms(socket.getFileDescriptor$());
    }

    public void removeTransportModeTransforms(DatagramSocket datagramSocket) throws Exception {
        removeTransportModeTransforms(datagramSocket.getFileDescriptor$());
    }

    public void removeTransportModeTransforms(FileDescriptor fileDescriptor) throws Exception {
        try {
            ParcelFileDescriptor parcelFileDescriptorDup = ParcelFileDescriptor.dup(fileDescriptor);
            try {
                this.mService.removeTransportModeTransforms(parcelFileDescriptorDup);
            } finally {
                if (parcelFileDescriptorDup != null) {
                    $closeResource(null, parcelFileDescriptorDup);
                }
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e2);
        }
    }

    public void removeTunnelModeTransform(Network network, IpSecTransform ipSecTransform) {
    }

    public static final class UdpEncapsulationSocket implements AutoCloseable {
        private final CloseGuard mCloseGuard;
        private final ParcelFileDescriptor mPfd;
        private final int mPort;
        private int mResourceId;
        private final IIpSecService mService;

        private UdpEncapsulationSocket(IIpSecService iIpSecService, int i) throws ResourceUnavailableException, IOException {
            this.mResourceId = -1;
            this.mCloseGuard = CloseGuard.get();
            this.mService = iIpSecService;
            try {
                IpSecUdpEncapResponse ipSecUdpEncapResponseOpenUdpEncapsulationSocket = this.mService.openUdpEncapsulationSocket(i, new Binder());
                switch (ipSecUdpEncapResponseOpenUdpEncapsulationSocket.status) {
                    case 0:
                        this.mResourceId = ipSecUdpEncapResponseOpenUdpEncapsulationSocket.resourceId;
                        this.mPort = ipSecUdpEncapResponseOpenUdpEncapsulationSocket.port;
                        this.mPfd = ipSecUdpEncapResponseOpenUdpEncapsulationSocket.fileDescriptor;
                        this.mCloseGuard.open("constructor");
                        return;
                    case 1:
                        throw new ResourceUnavailableException("No more Sockets may be allocated by this requester.");
                    default:
                        throw new RuntimeException("Unknown status returned by IpSecService: " + ipSecUdpEncapResponseOpenUdpEncapsulationSocket.status);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        public FileDescriptor getFileDescriptor() {
            if (this.mPfd == null) {
                return null;
            }
            return this.mPfd.getFileDescriptor();
        }

        public int getPort() {
            return this.mPort;
        }

        @Override
        public void close() throws IOException {
            try {
                try {
                    try {
                        this.mService.closeUdpEncapsulationSocket(this.mResourceId);
                        this.mResourceId = -1;
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                } catch (Exception e2) {
                    Log.e(IpSecManager.TAG, "Failed to close " + this + ", Exception=" + e2);
                }
                try {
                    this.mPfd.close();
                } catch (IOException e3) {
                    Log.e(IpSecManager.TAG, "Failed to close UDP Encapsulation Socket with Port= " + this.mPort);
                    throw e3;
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

        @VisibleForTesting
        public int getResourceId() {
            return this.mResourceId;
        }

        public String toString() {
            return "UdpEncapsulationSocket{port=" + this.mPort + ",resourceId=" + this.mResourceId + "}";
        }
    }

    public UdpEncapsulationSocket openUdpEncapsulationSocket(int i) throws ResourceUnavailableException, IOException {
        if (i == 0) {
            throw new IllegalArgumentException("Specified port must be a valid port number!");
        }
        try {
            return new UdpEncapsulationSocket(this.mService, i);
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        }
    }

    public UdpEncapsulationSocket openUdpEncapsulationSocket() throws ResourceUnavailableException, IOException {
        try {
            return new UdpEncapsulationSocket(this.mService, 0);
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        }
    }

    public static final class IpSecTunnelInterface implements AutoCloseable {
        private final CloseGuard mCloseGuard;
        private String mInterfaceName;
        private final InetAddress mLocalAddress;
        private final String mOpPackageName;
        private final InetAddress mRemoteAddress;
        private int mResourceId;
        private final IIpSecService mService;
        private final Network mUnderlyingNetwork;

        public String getInterfaceName() {
            return this.mInterfaceName;
        }

        public void addAddress(InetAddress inetAddress, int i) throws IOException {
            try {
                this.mService.addAddressToTunnelInterface(this.mResourceId, new LinkAddress(inetAddress, i), this.mOpPackageName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (ServiceSpecificException e2) {
                throw IpSecManager.rethrowCheckedExceptionFromServiceSpecificException(e2);
            }
        }

        public void removeAddress(InetAddress inetAddress, int i) throws IOException {
            try {
                this.mService.removeAddressFromTunnelInterface(this.mResourceId, new LinkAddress(inetAddress, i), this.mOpPackageName);
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            } catch (ServiceSpecificException e2) {
                throw IpSecManager.rethrowCheckedExceptionFromServiceSpecificException(e2);
            }
        }

        private IpSecTunnelInterface(Context context, IIpSecService iIpSecService, InetAddress inetAddress, InetAddress inetAddress2, Network network) throws ResourceUnavailableException, IOException {
            this.mCloseGuard = CloseGuard.get();
            this.mResourceId = -1;
            this.mOpPackageName = context.getOpPackageName();
            this.mService = iIpSecService;
            this.mLocalAddress = inetAddress;
            this.mRemoteAddress = inetAddress2;
            this.mUnderlyingNetwork = network;
            try {
                IpSecTunnelInterfaceResponse ipSecTunnelInterfaceResponseCreateTunnelInterface = this.mService.createTunnelInterface(inetAddress.getHostAddress(), inetAddress2.getHostAddress(), network, new Binder(), this.mOpPackageName);
                switch (ipSecTunnelInterfaceResponseCreateTunnelInterface.status) {
                    case 0:
                        this.mResourceId = ipSecTunnelInterfaceResponseCreateTunnelInterface.resourceId;
                        this.mInterfaceName = ipSecTunnelInterfaceResponseCreateTunnelInterface.interfaceName;
                        this.mCloseGuard.open("constructor");
                        return;
                    case 1:
                        throw new ResourceUnavailableException("No more tunnel interfaces may be allocated by this requester.");
                    default:
                        throw new RuntimeException("Unknown status returned by IpSecService: " + ipSecTunnelInterfaceResponseCreateTunnelInterface.status);
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }

        @Override
        public void close() {
            try {
                try {
                    try {
                        this.mService.deleteTunnelInterface(this.mResourceId, this.mOpPackageName);
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                } catch (Exception e2) {
                    Log.e(IpSecManager.TAG, "Failed to close " + this + ", Exception=" + e2);
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

        @VisibleForTesting
        public int getResourceId() {
            return this.mResourceId;
        }

        public String toString() {
            return "IpSecTunnelInterface{ifname=" + this.mInterfaceName + ",resourceId=" + this.mResourceId + "}";
        }
    }

    public IpSecTunnelInterface createIpSecTunnelInterface(InetAddress inetAddress, InetAddress inetAddress2, Network network) throws ResourceUnavailableException, IOException {
        try {
            return new IpSecTunnelInterface(this.mContext, this.mService, inetAddress, inetAddress2, network);
        } catch (ServiceSpecificException e) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e);
        }
    }

    public void applyTunnelModeTransform(IpSecTunnelInterface ipSecTunnelInterface, int i, IpSecTransform ipSecTransform) throws IOException {
        try {
            this.mService.applyTunnelModeTransform(ipSecTunnelInterface.getResourceId(), i, ipSecTransform.getResourceId(), this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw rethrowCheckedExceptionFromServiceSpecificException(e2);
        }
    }

    public IpSecManager(Context context, IIpSecService iIpSecService) {
        this.mContext = context;
        this.mService = (IIpSecService) Preconditions.checkNotNull(iIpSecService, "missing service");
    }

    private static void maybeHandleServiceSpecificException(ServiceSpecificException serviceSpecificException) {
        if (serviceSpecificException.errorCode == OsConstants.EINVAL) {
            throw new IllegalArgumentException(serviceSpecificException);
        }
        if (serviceSpecificException.errorCode == OsConstants.EAGAIN) {
            throw new IllegalStateException(serviceSpecificException);
        }
        if (serviceSpecificException.errorCode == OsConstants.EOPNOTSUPP) {
            throw new UnsupportedOperationException(serviceSpecificException);
        }
    }

    static RuntimeException rethrowUncheckedExceptionFromServiceSpecificException(ServiceSpecificException serviceSpecificException) {
        maybeHandleServiceSpecificException(serviceSpecificException);
        throw new RuntimeException(serviceSpecificException);
    }

    static IOException rethrowCheckedExceptionFromServiceSpecificException(ServiceSpecificException serviceSpecificException) throws IOException {
        maybeHandleServiceSpecificException(serviceSpecificException);
        throw new ErrnoException("IpSec encountered errno=" + serviceSpecificException.errorCode, serviceSpecificException.errorCode).rethrowAsIOException();
    }
}
